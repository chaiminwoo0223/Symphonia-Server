package com.symphonia.gathering.presentation.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.symphonia.IntegrationTest;
import com.symphonia.auth.domain.client.SocialClient;
import com.symphonia.auth.domain.identity.SocialIdentity;
import com.symphonia.auth.helper.AuthHelper;
import com.symphonia.gathering.application.usecase.GetExpiredGatheringsUseCase;
import com.symphonia.gathering.application.usecase.PurgeGatheringUseCase;
import com.symphonia.gathering.domain.policy.GatheringPolicy;
import com.symphonia.gathering.fixture.GatheringPairingFixture;
import com.symphonia.gathering.fixture.ParticipantFixture;
import com.symphonia.gathering.helper.GatheringHelper;
import com.symphonia.gathering.presentation.GatheringHeaders;
import com.symphonia.gathering.presentation.dto.request.FixedAmountRequest;
import com.symphonia.gathering.presentation.dto.request.GatheringPairingRequest;
import com.symphonia.gathering.presentation.dto.request.JoinGatheringRequest;
import com.symphonia.gathering.presentation.dto.request.MarkSharesSentRequest;
import com.symphonia.gathering.presentation.dto.request.OpenGatheringRequest;
import com.symphonia.gathering.presentation.dto.request.RecordSettlementRequest;
import com.symphonia.gathering.presentation.dto.request.SeenShareRequest;
import com.symphonia.gathering.presentation.dto.request.SubmitParticipantPairingFeedbackRequest;
import com.symphonia.gathering.presentation.scheduler.GatheringPurgeScheduler;
import com.symphonia.member.domain.entity.Member;
import com.symphonia.member.domain.repository.MemberRepository;
import com.symphonia.member.fixture.MemberFixture;
import com.symphonia.member.helper.MemberHelper;
import com.symphonia.pairing.domain.vo.PairingRating;
import com.symphonia.pairing.helper.PairingHelper;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.JsonNode;

// 모임을 열고 참여하고 정산하고 피드백을 내는 핵심 여정을 컨트롤러부터 실제 DB까지 잇는다(#129).
// 외부 호출이 없어 스텁은 없고, Postgres와 Redis 컨테이너만 쓴다. 트랜잭션 롤백 없이 커밋해야 요청마다 실제 트랜잭션과 커밋 뒤 리스너가 돈다.
@DisplayName("모임 핵심 여정 통합 테스트")
class GatheringJourneyIntegrationTest extends IntegrationTest {

    private static final long TOTAL_AMOUNT = 30_000L;
    // 파기 시각은 모임을 연 지 29일 뒤다.
    private static final int DAYS_UNTIL_PURGE = 29;

    @Autowired private MemberHelper memberHelper;
    @Autowired private GatheringHelper gatheringHelper;
    @Autowired private AuthHelper authHelper;
    @Autowired private MemberRepository memberRepository;
    @Autowired private GetExpiredGatheringsUseCase getExpiredGatheringsUseCase;
    @Autowired private PurgeGatheringUseCase purgeGatheringUseCase;
    @Autowired private PairingHelper pairingHelper;
    @Autowired private JdbcTemplate jdbcTemplate;
    @Autowired private MeterRegistry meterRegistry;

    @MockitoBean(name = "google")
    private SocialClient googleSocialClient;

    private Member host;
    private Long gatheringId;
    private Long drinkId;
    private Long anjuId;

    // 피드백과 집계는 시드 카탈로그 조합에 쌓이므로 그 조합의 행만 지운다. 파기가 끝까지 가지 못한 경우를 위해 모임의 행도 지운다.
    @AfterEach
    void deleteCommittedRows() {
        pairingHelper.deleteRowsOf(drinkId, anjuId);
        gatheringHelper.deleteRows(gatheringId);
        memberRepository.delete(host);
    }

    @Nested
    @DisplayName("모임장이 모임을 열고 정산한 뒤 탈퇴하는 여정은")
    class HostJourney {

        @Test
        @Transactional(propagation = Propagation.NOT_SUPPORTED)
        @DisplayName("피드백을 남기고, 탈퇴한 모임장의 액세스 토큰을 거부하고, 보존 기간이 지나면 모임을 파기한다")
        void shouldKeepFeedbackAndPurgeGatheringAfterHostWithdrawal() throws Exception {
            // given
            host = memberHelper.save(MemberFixture.KAKAO);
            String hostToken = authHelper.bearerTokenFor(host);
            String otherDeviceToken = bearerTokenIssuedInNextSecond();
            drinkId = jdbcTemplate.queryForObject("SELECT MIN(id) FROM drink", Long.class);
            anjuId = jdbcTemplate.queryForObject("SELECT MIN(id) FROM anju", Long.class);
            givenGoogleAccounts(ParticipantFixture.KIM, ParticipantFixture.LEE);
            Map<String, Double> countsBefore = flowCounts();

            // when & then: 모임을 열고 두 사람이 참여 토큰으로 참여한다
            JsonNode opened = openGathering(hostToken);
            gatheringId = opened.at("/gatheringId").asLong();
            String joinToken = opened.at("/joinToken").asString();
            JsonNode kim = join(joinToken, ParticipantFixture.KIM);
            JsonNode lee = join(joinToken, ParticipantFixture.LEE);

            // 그다음 모임장이 이를 결제자로 정산 건을 기록하고, 김이 몫을 보냈다고 알린다
            long roundId =
                    json(
                                    get(settlementsPath() + "/form")
                                            .header(HttpHeaders.AUTHORIZATION, hostToken),
                                    200)
                            .at("/rounds/0/id")
                            .asLong();
            json(
                    post(settlementsPath())
                            .header(HttpHeaders.AUTHORIZATION, hostToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(
                                    objectMapper.writeValueAsString(
                                            recordRequest(
                                                    lee.at("/id").asLong(),
                                                    kim.at("/id").asLong(),
                                                    roundId))),
                    201);
            String kimLinkToken = kim.at("/linkToken").asString();
            JsonNode seen =
                    json(
                                    get("/api/v1/shares")
                                            .header(GatheringHeaders.LINK_TOKEN, kimLinkToken),
                                    200)
                            .at("/payers/0/shares/0");
            SeenShareRequest seenShare =
                    new SeenShareRequest(
                            seen.at("/settlementId").asLong(), seen.at("/amount").asLong());
            JsonNode shares =
                    json(
                            post("/api/v1/shares/sent")
                                    .header(GatheringHeaders.LINK_TOKEN, kimLinkToken)
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(
                                            objectMapper.writeValueAsString(
                                                    new MarkSharesSentRequest(
                                                            lee.at("/id").asLong(),
                                                            List.of(seenShare)))),
                            200);
            assertThat(shares.at("/shares/0/status").asString()).isEqualTo("SENT");
            assertThat(shares.at("/hasUnpaidShares").asBoolean()).isFalse();

            // 그다음 몫을 보낸 김이 피드백을 남긴다
            json(
                    post("/api/v1/participants/pairing/feedback")
                            .header(GatheringHeaders.LINK_TOKEN, kimLinkToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(
                                    objectMapper.writeValueAsString(
                                            new SubmitParticipantPairingFeedbackRequest(
                                                    drinkId, anjuId, PairingRating.LIKE))),
                    201);
            assertThat(countPairingRows("pairing_feedback")).isOne();

            // 그다음 커밋된 흐름마다 전환 지표가 올라 있다. 지표는 컨텍스트를 같이 쓰는 테스트와 누적되므로 늘어난 만큼만 본다
            assertThat(increasesSince(countsBefore))
                    .containsExactlyInAnyOrderEntriesOf(
                            Map.of(
                                    "symphonia.gathering.opened", 1.0,
                                    "symphonia.gathering.participant.joined", 2.0,
                                    "symphonia.gathering.settlement.recorded", 1.0,
                                    "symphonia.gathering.share.sent.requests", 1.0,
                                    "symphonia.pairing.feedback.submitted", 1.0));

            // 그다음 모임장이 탈퇴하면 다른 기기에서 발급한 액세스 토큰도 거부되지만 참여자의 개인 링크는 그대로 쓸 수 있다
            mockMvc.perform(
                            get(participantsPath())
                                    .header(HttpHeaders.AUTHORIZATION, otherDeviceToken))
                    .andExpect(status().isOk());
            mockMvc.perform(
                            delete("/api/v1/members/me")
                                    .header(HttpHeaders.AUTHORIZATION, hostToken))
                    .andExpect(status().isNoContent());
            mockMvc.perform(
                            get(participantsPath())
                                    .header(HttpHeaders.AUTHORIZATION, otherDeviceToken))
                    .andExpect(status().isUnauthorized());
            mockMvc.perform(get(participantsPath()).header(HttpHeaders.AUTHORIZATION, hostToken))
                    .andExpect(status().isUnauthorized());
            mockMvc.perform(get("/api/v1/shares").header(GatheringHeaders.LINK_TOKEN, kimLinkToken))
                    .andExpect(status().isOk());

            // 그다음 보존 기간이 지나면 스케줄러가 모임의 행을 모두 지우고, 모임장이 고른 조합의 선택 신호와 익명 피드백은 남긴다
            jdbcTemplate.update(
                    "UPDATE gathering SET opened_at = ? WHERE id = ?",
                    LocalDateTime.now().minusDays(DAYS_UNTIL_PURGE),
                    gatheringId);
            new GatheringPurgeScheduler(getExpiredGatheringsUseCase, purgeGatheringUseCase)
                    .purgeExpiredGatherings();
            assertThat(countGatheringRows("gathering", "id")).isZero();
            assertThat(countGatheringRows("participant", "gathering_id")).isZero();
            assertThat(countGatheringRows("settlement", "gathering_id")).isZero();
            assertThat(countPairingRows("pairing_selection_summary")).isOne();
            assertThat(countPairingRows("pairing_feedback")).isOne();
        }
    }

    // 액세스 토큰의 발급 시각은 초 단위라 같은 초에 만든 토큰은 문자열이 같고, 탈퇴를 요청한 토큰은 로그아웃으로 막힌다.
    // 탈퇴한 회원 id로 막히는지 보려면 다른 기기가 나중에 발급한 토큰처럼 문자열이 다른 토큰이 필요하므로 초가 바뀐 뒤에 발급한다.
    private String bearerTokenIssuedInNextSecond() throws InterruptedException {
        Thread.sleep(1_100);

        return authHelper.bearerTokenFor(host);
    }

    private JsonNode openGathering(String hostToken) throws Exception {
        GatheringPairingFixture pairing = GatheringPairingFixture.FRIEND_CASUAL;

        return json(
                post("/api/v1/gatherings")
                        .header(HttpHeaders.AUTHORIZATION, hostToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(
                                objectMapper.writeValueAsString(
                                        new OpenGatheringRequest(
                                                UUID.randomUUID(),
                                                "여정 모임",
                                                new GatheringPairingRequest(
                                                        drinkId,
                                                        anjuId,
                                                        pairing.getRelationshipType(),
                                                        pairing.getMoodType())))),
                201);
    }

    // 요청을 보내 기대한 상태 코드를 확인하고 응답의 data를 돌려준다.
    // 참여자마다 다른 구글 계정으로 본인을 확인한 것으로 스텁한다. 구글 ID 토큰을 sub로 쓴다.
    private void givenGoogleAccounts(ParticipantFixture... participants) {
        for (ParticipantFixture participant : participants) {
            String googleIdToken = googleIdTokenOf(participant);
            given(googleSocialClient.authenticate(googleIdToken))
                    .willReturn(
                            new SocialIdentity(
                                    googleIdToken,
                                    participant.getName(),
                                    "symphonia@google.com",
                                    null,
                                    "GOOGLE"));
        }
    }

    private JsonNode json(MockHttpServletRequestBuilder request, int expectedStatus)
            throws Exception {
        String body =
                mockMvc.perform(request)
                        .andExpect(status().is(expectedStatus))
                        .andReturn()
                        .getResponse()
                        .getContentAsString();

        return objectMapper.readTree(body).at("/data");
    }

    private JsonNode join(String joinToken, ParticipantFixture participant) throws Exception {
        return json(
                post("/api/v1/participants")
                        .header(GatheringHeaders.JOIN_TOKEN, joinToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(
                                objectMapper.writeValueAsString(
                                        new JoinGatheringRequest(
                                                googleIdTokenOf(participant),
                                                participant.getName(),
                                                participant.getPhoneNumber(),
                                                true,
                                                GatheringPolicy.CONSENT_VERSION))),
                201);
    }

    private String googleIdTokenOf(ParticipantFixture participant) {
        return "google-id-token-" + participant.name();
    }

    private String settlementsPath() {
        return "/api/v1/gatherings/" + gatheringId + "/settlements";
    }

    private RecordSettlementRequest recordRequest(long payerId, long memberId, long roundId) {
        return new RecordSettlementRequest(
                UUID.randomUUID(),
                payerId,
                roundId,
                TOTAL_AMOUNT,
                List.of(payerId, memberId),
                List.<FixedAmountRequest>of());
    }

    private Long countPairingRows(String table) {
        return jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM " + table + " WHERE drink_id = ? AND anju_id = ?",
                Long.class,
                drinkId,
                anjuId);
    }

    // 지표 이름마다 type 라벨의 값을 합한 건수다.
    private Map<String, Double> flowCounts() {
        return Stream.of(
                        "symphonia.gathering.opened",
                        "symphonia.gathering.participant.joined",
                        "symphonia.gathering.settlement.recorded",
                        "symphonia.gathering.share.sent.requests",
                        "symphonia.pairing.feedback.submitted")
                .collect(
                        Collectors.toMap(
                                Function.identity(),
                                name ->
                                        meterRegistry.get(name).counters().stream()
                                                .mapToDouble(Counter::count)
                                                .sum()));
    }

    private Map<String, Double> increasesSince(Map<String, Double> countsBefore) {
        return flowCounts().entrySet().stream()
                .collect(
                        Collectors.toMap(
                                Map.Entry::getKey,
                                count -> count.getValue() - countsBefore.get(count.getKey())));
    }

    private String participantsPath() {
        return "/api/v1/gatherings/" + gatheringId + "/participants";
    }

    private Long countGatheringRows(String table, String gatheringColumn) {
        return jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM " + table + " WHERE " + gatheringColumn + " = ?",
                Long.class,
                gatheringId);
    }
}
