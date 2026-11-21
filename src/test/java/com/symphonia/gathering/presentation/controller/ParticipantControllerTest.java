package com.symphonia.gathering.presentation.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.symphonia.IntegrationTest;
import com.symphonia.auth.domain.client.SocialClient;
import com.symphonia.auth.domain.exception.InvalidSocialTokenException;
import com.symphonia.auth.fixture.SocialIdentityFixture;
import com.symphonia.catalog.domain.entity.Anju;
import com.symphonia.catalog.domain.entity.Drink;
import com.symphonia.catalog.fixture.AnjuFixture;
import com.symphonia.catalog.fixture.DrinkFixture;
import com.symphonia.catalog.helper.CatalogHelper;
import com.symphonia.common.exception.error.CommonErrorCode;
import com.symphonia.gathering.domain.entity.Gathering;
import com.symphonia.gathering.domain.entity.Participant;
import com.symphonia.gathering.domain.error.GatheringErrorCode;
import com.symphonia.gathering.domain.policy.GatheringPolicy;
import com.symphonia.gathering.domain.vo.GatheringPairing;
import com.symphonia.gathering.fixture.GatheringFixture;
import com.symphonia.gathering.fixture.GatheringPairingFixture;
import com.symphonia.gathering.fixture.ParticipantFixture;
import com.symphonia.gathering.fixture.SettlementFixture;
import com.symphonia.gathering.helper.GatheringHelper;
import com.symphonia.gathering.helper.SettlementHelper;
import com.symphonia.gathering.presentation.dto.request.JoinGatheringRequest;
import com.symphonia.gathering.presentation.dto.request.SubmitParticipantPairingFeedbackRequest;
import com.symphonia.member.domain.entity.Member;
import com.symphonia.member.fixture.MemberFixture;
import com.symphonia.member.helper.MemberHelper;
import com.symphonia.pairing.domain.vo.PairingRating;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.jdbc.Sql;

@Sql("/sql/clear-catalog.sql")
class ParticipantControllerTest extends IntegrationTest {

    private static final String HYPHENATED_PHONE_NUMBER = "010-1234-5678";
    private static final String MASKED_PHONE_NUMBER = "010-****-5678";
    private static final String GOOGLE_ID_TOKEN = "google-id-token";

    @Autowired private GatheringHelper gatheringHelper;
    @Autowired private SettlementHelper settlementHelper;
    @Autowired private MemberHelper memberHelper;
    @Autowired private CatalogHelper catalogHelper;
    @Autowired private JdbcTemplate jdbcTemplate;

    @MockitoBean(name = "google")
    private SocialClient googleSocialClient;

    @BeforeEach
    void setUpGoogleAccount() {
        given(googleSocialClient.authenticate(GOOGLE_ID_TOKEN))
                .willReturn(SocialIdentityFixture.GOOGLE.create());
    }

    @Nested
    @DisplayName("POST /api/v1/participants는")
    class Join {

        @Nested
        @DisplayName("참여할 수 있는 요청인 경우")
        class WhenJoinable {

            @Test
            @DisplayName("인증 없이도 참여시키고 마스킹한 휴대폰과 개인 링크 토큰을 담아 201을 반환한다")
            void shouldJoinWithoutAuthentication() throws Exception {
                // given
                String joinToken = saveGatheringWithJoinToken();
                JoinGatheringRequest request =
                        joinRequest(
                                ParticipantFixture.KIM.getName(),
                                true,
                                GatheringPolicy.CONSENT_VERSION);

                // when & then
                mockMvc.perform(
                                post("/api/v1/participants")
                                        .header("X-Join-Token", joinToken)
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(objectMapper.writeValueAsString(request)))
                        .andExpect(status().isCreated())
                        .andExpect(jsonPath("$.data.id").isNumber())
                        .andExpect(jsonPath("$.data.name").value(ParticipantFixture.KIM.getName()))
                        .andExpect(jsonPath("$.data.maskedPhoneNumber").value(MASKED_PHONE_NUMBER))
                        .andExpect(jsonPath("$.data.linkToken").isNotEmpty());
            }
        }

        @Nested
        @DisplayName("참여자가 19명인 모임인 경우")
        class WhenParticipantCountJustBelowLimit {

            @Test
            @DisplayName("20번째 참여를 받아 201을 반환한다")
            void shouldJoinTwentiethParticipant() throws Exception {
                // given
                Gathering gathering =
                        gatheringHelper.join(
                                saveGathering(), GatheringPolicy.MAX_PARTICIPANT_COUNT - 1);
                String joinToken = gatheringHelper.issueJoinToken(gathering);
                JoinGatheringRequest request =
                        joinRequest(
                                ParticipantFixture.KIM.getName(),
                                true,
                                GatheringPolicy.CONSENT_VERSION);

                // when & then
                mockMvc.perform(
                                post("/api/v1/participants")
                                        .header("X-Join-Token", joinToken)
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(objectMapper.writeValueAsString(request)))
                        .andExpect(status().isCreated());
            }
        }

        @Nested
        @DisplayName("참여자가 20명인 모임인 경우")
        class WhenParticipantCountAtLimit {

            @Test
            @DisplayName("GATHERING_PARTICIPANT_COUNT_EXCEEDS_LIMIT 코드와 함께 400을 반환한다")
            void shouldReturnBadRequest() throws Exception {
                // given
                Gathering gathering =
                        gatheringHelper.join(
                                saveGathering(), GatheringPolicy.MAX_PARTICIPANT_COUNT);
                String joinToken = gatheringHelper.issueJoinToken(gathering);
                JoinGatheringRequest request =
                        joinRequest(
                                ParticipantFixture.KIM.getName(),
                                true,
                                GatheringPolicy.CONSENT_VERSION);

                // when & then
                mockMvc.perform(
                                post("/api/v1/participants")
                                        .header("X-Join-Token", joinToken)
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(objectMapper.writeValueAsString(request)))
                        .andExpect(status().isBadRequest())
                        .andExpect(
                                jsonPath("$.data.code")
                                        .value(
                                                GatheringErrorCode
                                                        .GATHERING_PARTICIPANT_COUNT_EXCEEDS_LIMIT
                                                        .getCode()));
            }
        }

        @Nested
        @DisplayName("개인정보 수집 및 이용에 동의하지 않은 경우")
        class WhenConsentNotAgreed {

            @Test
            @DisplayName("CONSENT_REQUIRED 코드와 함께 400을 반환한다")
            void shouldReturnBadRequest() throws Exception {
                // given
                String joinToken = saveGatheringWithJoinToken();
                JoinGatheringRequest request =
                        joinRequest(
                                ParticipantFixture.KIM.getName(),
                                false,
                                GatheringPolicy.CONSENT_VERSION);

                // when & then
                mockMvc.perform(
                                post("/api/v1/participants")
                                        .header("X-Join-Token", joinToken)
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(objectMapper.writeValueAsString(request)))
                        .andExpect(status().isBadRequest())
                        .andExpect(
                                jsonPath("$.data.code")
                                        .value(GatheringErrorCode.CONSENT_REQUIRED.getCode()));
            }
        }

        @Nested
        @DisplayName("구글 ID 토큰이 공백인 경우")
        class WhenGoogleIdTokenBlank {

            @Test
            @DisplayName("googleIdToken 필드 검증 실패와 함께 400을 반환한다")
            void shouldReturnBadRequest() throws Exception {
                // given
                String joinToken = saveGatheringWithJoinToken();
                JoinGatheringRequest request =
                        new JoinGatheringRequest(
                                " ",
                                ParticipantFixture.KIM.getName(),
                                HYPHENATED_PHONE_NUMBER,
                                true,
                                GatheringPolicy.CONSENT_VERSION);

                // when & then
                mockMvc.perform(
                                post("/api/v1/participants")
                                        .header("X-Join-Token", joinToken)
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(objectMapper.writeValueAsString(request)))
                        .andExpect(status().isBadRequest())
                        .andExpect(
                                jsonPath("$.data.code")
                                        .value(CommonErrorCode.METHOD_ARGUMENT_NOT_VALID.getCode()))
                        .andExpect(jsonPath("$.data.violations[0].field").value("googleIdToken"));
            }
        }

        @Nested
        @DisplayName("구글 ID 토큰이 유효하지 않은 경우")
        class WhenGoogleIdTokenInvalid {

            @Test
            @DisplayName("INVALID_SOCIAL_TOKEN 코드와 함께 400을 반환한다")
            void shouldReturnBadRequest() throws Exception {
                // given
                String joinToken = saveGatheringWithJoinToken();
                given(googleSocialClient.authenticate(GOOGLE_ID_TOKEN))
                        .willThrow(new InvalidSocialTokenException());
                JoinGatheringRequest request =
                        joinRequest(
                                ParticipantFixture.KIM.getName(),
                                true,
                                GatheringPolicy.CONSENT_VERSION);

                // when & then
                mockMvc.perform(
                                post("/api/v1/participants")
                                        .header("X-Join-Token", joinToken)
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(objectMapper.writeValueAsString(request)))
                        .andExpect(status().isBadRequest())
                        .andExpect(jsonPath("$.data.code").value("INVALID_SOCIAL_TOKEN"));
            }
        }

        @Nested
        @DisplayName("같은 구글 계정으로 이미 참여한 경우")
        class WhenGoogleAccountAlreadyJoined {

            @Test
            @DisplayName("다른 휴대폰이어도 PARTICIPANT_ALREADY_JOINED 코드와 함께 409를 반환한다")
            void shouldReturnConflict() throws Exception {
                // given
                String joinToken = saveGatheringWithJoinToken();
                mockMvc.perform(
                                post("/api/v1/participants")
                                        .header("X-Join-Token", joinToken)
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(
                                                objectMapper.writeValueAsString(
                                                        joinRequest(
                                                                ParticipantFixture.KIM.getName(),
                                                                true,
                                                                GatheringPolicy.CONSENT_VERSION))))
                        .andExpect(status().isCreated());
                JoinGatheringRequest sameAccountRequest =
                        new JoinGatheringRequest(
                                GOOGLE_ID_TOKEN,
                                ParticipantFixture.LEE.getName(),
                                ParticipantFixture.LEE.getPhoneNumber(),
                                true,
                                GatheringPolicy.CONSENT_VERSION);

                // when & then
                mockMvc.perform(
                                post("/api/v1/participants")
                                        .header("X-Join-Token", joinToken)
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(
                                                objectMapper.writeValueAsString(
                                                        sameAccountRequest)))
                        .andExpect(status().isConflict())
                        .andExpect(
                                jsonPath("$.data.code")
                                        .value(
                                                GatheringErrorCode.PARTICIPANT_ALREADY_JOINED
                                                        .getCode()));
            }
        }

        @Nested
        @DisplayName("이름이 공백인 경우")
        class WhenNameBlank {

            @Test
            @DisplayName("name 필드 검증 실패와 함께 400을 반환한다")
            void shouldReturnBadRequest() throws Exception {
                // given
                String joinToken = saveGatheringWithJoinToken();
                JoinGatheringRequest request =
                        joinRequest(" ", true, GatheringPolicy.CONSENT_VERSION);

                // when & then
                mockMvc.perform(
                                post("/api/v1/participants")
                                        .header("X-Join-Token", joinToken)
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(objectMapper.writeValueAsString(request)))
                        .andExpect(status().isBadRequest())
                        .andExpect(
                                jsonPath("$.data.code")
                                        .value(CommonErrorCode.METHOD_ARGUMENT_NOT_VALID.getCode()))
                        .andExpect(jsonPath("$.data.violations[0].field").value("name"));
            }
        }

        @Nested
        @DisplayName("동의 안내 버전이 공백인 경우")
        class WhenConsentVersionBlank {

            @Test
            @DisplayName("consentVersion 필드 검증 실패와 함께 400을 반환한다")
            void shouldReturnBadRequest() throws Exception {
                // given
                String joinToken = saveGatheringWithJoinToken();
                JoinGatheringRequest request =
                        joinRequest(ParticipantFixture.KIM.getName(), true, " ");

                // when & then
                mockMvc.perform(
                                post("/api/v1/participants")
                                        .header("X-Join-Token", joinToken)
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(objectMapper.writeValueAsString(request)))
                        .andExpect(status().isBadRequest())
                        .andExpect(
                                jsonPath("$.data.code")
                                        .value(CommonErrorCode.METHOD_ARGUMENT_NOT_VALID.getCode()))
                        .andExpect(jsonPath("$.data.violations[0].field").value("consentVersion"));
            }
        }

        private JoinGatheringRequest joinRequest(
                String name, boolean consentAgreed, String consentVersion) {
            return new JoinGatheringRequest(
                    GOOGLE_ID_TOKEN, name, HYPHENATED_PHONE_NUMBER, consentAgreed, consentVersion);
        }
    }

    @Nested
    @DisplayName("GET /api/v1/participants/pairing은")
    class GetPairing {

        private Gathering gathering;
        private Participant participant;
        private String linkToken;

        @BeforeEach
        void setUp() {
            gathering = gatheringHelper.join(saveGathering(), ParticipantFixture.KIM);
            participant = gathering.getParticipants().get(0);
            linkToken = gatheringHelper.issueLinkToken(participant);
        }

        @Nested
        @DisplayName("모임에서 고른 페어링이 있는 경우")
        class WhenPairingChosen {

            @Test
            @DisplayName("인증 없이도 술과 안주의 ID와 이름, 피드백을 내지 않았고 아직 낼 수 없다는 값을 담아 200을 반환한다")
            void shouldReturnPairing() throws Exception {
                // given
                GatheringPairing chosen = choosePairing(gathering);

                // when & then
                mockMvc.perform(
                                get("/api/v1/participants/pairing")
                                        .header("X-Link-Token", linkToken))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.data.drinkId").value(chosen.drinkId().intValue()))
                        .andExpect(jsonPath("$.data.drinkName").value(DrinkFixture.SOJU.getName()))
                        .andExpect(jsonPath("$.data.anjuId").value(chosen.anjuId().intValue()))
                        .andExpect(
                                jsonPath("$.data.anjuName")
                                        .value(AnjuFixture.GOLBAENGI_MUCHIM.getName()))
                        .andExpect(jsonPath("$.data.feedbackSubmitted").value(false))
                        .andExpect(jsonPath("$.data.feedbackSubmittable").value(false));
            }

            @Test
            @DisplayName("본인 분담금을 보낸 참여자면 지금 낼 수 있다는 값을 담아 200을 반환한다")
            void shouldReturnFeedbackSubmittableWhenShareSent() throws Exception {
                // given
                Gathering joined = gatheringHelper.join(gathering, ParticipantFixture.LEE);
                choosePairing(joined);
                Participant payer = joined.getParticipants().get(1);
                settlementHelper.markSent(
                        settlementHelper.record(SettlementFixture.DINNER, joined, payer),
                        participant);

                // when & then
                mockMvc.perform(
                                get("/api/v1/participants/pairing")
                                        .header("X-Link-Token", linkToken))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.data.feedbackSubmitted").value(false))
                        .andExpect(jsonPath("$.data.feedbackSubmittable").value(true));
            }

            @Test
            @DisplayName("이미 피드백을 낸 참여자면 피드백을 냈다는 값을 담아 200을 반환한다")
            void shouldReturnFeedbackSubmittedWhenAlreadySubmitted() throws Exception {
                // given
                choosePairing(gathering);
                gatheringHelper.markPairingFeedbackSubmitted(participant);

                // when & then
                mockMvc.perform(
                                get("/api/v1/participants/pairing")
                                        .header("X-Link-Token", linkToken))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.data.feedbackSubmitted").value(true));
            }
        }

        @Nested
        @DisplayName("모임에서 고른 페어링이 없는 경우")
        class WhenPairingNotChosen {

            @Test
            @DisplayName("GATHERING_PAIRING_NOT_FOUND 코드와 함께 404를 반환한다")
            void shouldReturnNotFound() throws Exception {
                // when & then
                mockMvc.perform(
                                get("/api/v1/participants/pairing")
                                        .header("X-Link-Token", linkToken))
                        .andExpect(status().isNotFound())
                        .andExpect(
                                jsonPath("$.data.code")
                                        .value(
                                                GatheringErrorCode.GATHERING_PAIRING_NOT_FOUND
                                                        .getCode()));
            }
        }
    }

    @Nested
    @DisplayName("POST /api/v1/participants/pairing/feedback은")
    class SubmitPairingFeedback {

        private Participant participant;
        private String linkToken;
        private String unpaidParticipantLinkToken;
        private GatheringPairing pairing;

        // 김이 낼 몫을 보냈고 박은 아직 보내지 않은 정산 건을 이가 결제자로 기록한 모임이다.
        @BeforeEach
        void setUp() {
            Gathering gathering =
                    gatheringHelper.join(
                            saveGathering(),
                            ParticipantFixture.KIM,
                            ParticipantFixture.LEE,
                            ParticipantFixture.PARK);
            participant = gathering.getParticipants().get(0);
            Participant payer = gathering.getParticipants().get(1);
            linkToken = gatheringHelper.issueLinkToken(participant);
            unpaidParticipantLinkToken =
                    gatheringHelper.issueLinkToken(gathering.getParticipants().get(2));
            pairing = choosePairing(gathering);
            settlementHelper.markSent(
                    settlementHelper.record(SettlementFixture.DINNER, gathering, payer),
                    participant);
        }

        @Nested
        @DisplayName("피드백을 낼 수 있는 요청인 경우")
        class WhenSubmittable {

            @Test
            @DisplayName("인증 없이도 회원 id 없는 피드백과 제출 표시를 남기고 201을 반환한다")
            void shouldSubmitPairingFeedback() throws Exception {
                // given
                SubmitParticipantPairingFeedbackRequest request =
                        new SubmitParticipantPairingFeedbackRequest(
                                pairing.drinkId(), pairing.anjuId(), PairingRating.LIKE);

                // when & then
                mockMvc.perform(
                                post("/api/v1/participants/pairing/feedback")
                                        .header("X-Link-Token", linkToken)
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(objectMapper.writeValueAsString(request)))
                        .andExpect(status().isCreated())
                        .andExpect(jsonPath("$.ok").value(true));
                assertThat(
                                jdbcTemplate.queryForObject(
                                        "SELECT COUNT(*) FROM pairing_feedback WHERE member_id IS NULL"
                                                + " AND drink_id = ? AND anju_id = ?"
                                                + " AND relationship_type = ? AND mood_type = ? AND rating = ?",
                                        Long.class,
                                        pairing.drinkId(),
                                        pairing.anjuId(),
                                        pairing.relationshipType().name(),
                                        pairing.moodType().name(),
                                        PairingRating.LIKE.name()))
                        .isOne();
                assertThat(
                                jdbcTemplate.queryForObject(
                                        "SELECT COUNT(*) FROM participant_pairing_feedback WHERE participant_id = ?",
                                        Long.class,
                                        participant.getId()))
                        .isOne();
            }
        }

        @Nested
        @DisplayName("이미 피드백을 낸 참여자인 경우")
        class WhenAlreadySubmitted {

            @Test
            @DisplayName("PAIRING_FEEDBACK_ALREADY_SUBMITTED 코드와 함께 409를 반환한다")
            void shouldReturnConflict() throws Exception {
                // given
                gatheringHelper.markPairingFeedbackSubmitted(participant);
                SubmitParticipantPairingFeedbackRequest request =
                        new SubmitParticipantPairingFeedbackRequest(
                                pairing.drinkId(), pairing.anjuId(), PairingRating.LIKE);

                // when & then
                mockMvc.perform(
                                post("/api/v1/participants/pairing/feedback")
                                        .header("X-Link-Token", linkToken)
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(objectMapper.writeValueAsString(request)))
                        .andExpect(status().isConflict())
                        .andExpect(
                                jsonPath("$.data.code")
                                        .value(
                                                GatheringErrorCode
                                                        .PAIRING_FEEDBACK_ALREADY_SUBMITTED
                                                        .getCode()));
            }
        }

        @Nested
        @DisplayName("참여자가 본 조합이 모임의 페어링과 다른 경우")
        class WhenPairingChanged {

            @Test
            @DisplayName("GATHERING_PAIRING_CONFLICT 코드와 함께 409를 반환한다")
            void shouldReturnConflict() throws Exception {
                // given
                SubmitParticipantPairingFeedbackRequest request =
                        new SubmitParticipantPairingFeedbackRequest(
                                pairing.drinkId(), pairing.anjuId() + 1, PairingRating.LIKE);

                // when & then
                mockMvc.perform(
                                post("/api/v1/participants/pairing/feedback")
                                        .header("X-Link-Token", linkToken)
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(objectMapper.writeValueAsString(request)))
                        .andExpect(status().isConflict())
                        .andExpect(
                                jsonPath("$.data.code")
                                        .value(
                                                GatheringErrorCode.GATHERING_PAIRING_CONFLICT
                                                        .getCode()));
            }
        }

        @Nested
        @DisplayName("낼 몫을 아직 보내지 않은 참여자인 경우")
        class WhenShareNotSent {

            @Test
            @DisplayName("SENT_SHARE_REQUIRED 코드와 함께 400을 반환한다")
            void shouldReturnBadRequest() throws Exception {
                // given
                SubmitParticipantPairingFeedbackRequest request =
                        new SubmitParticipantPairingFeedbackRequest(
                                pairing.drinkId(), pairing.anjuId(), PairingRating.LIKE);

                // when & then
                mockMvc.perform(
                                post("/api/v1/participants/pairing/feedback")
                                        .header("X-Link-Token", unpaidParticipantLinkToken)
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(objectMapper.writeValueAsString(request)))
                        .andExpect(status().isBadRequest())
                        .andExpect(
                                jsonPath("$.data.code")
                                        .value(GatheringErrorCode.SENT_SHARE_REQUIRED.getCode()));
            }
        }

        @Nested
        @DisplayName("술이나 안주 ID가 빠진 경우")
        class WhenCombinationMissing {

            @Test
            @DisplayName("drinkId 필드 검증 실패와 함께 400을 반환한다")
            void shouldReturnBadRequest() throws Exception {
                // given
                SubmitParticipantPairingFeedbackRequest request =
                        new SubmitParticipantPairingFeedbackRequest(
                                null, pairing.anjuId(), PairingRating.LIKE);

                // when & then
                mockMvc.perform(
                                post("/api/v1/participants/pairing/feedback")
                                        .header("X-Link-Token", linkToken)
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(objectMapper.writeValueAsString(request)))
                        .andExpect(status().isBadRequest())
                        .andExpect(
                                jsonPath("$.data.code")
                                        .value(CommonErrorCode.METHOD_ARGUMENT_NOT_VALID.getCode()))
                        .andExpect(jsonPath("$.data.violations[0].field").value("drinkId"));
            }
        }

        @Nested
        @DisplayName("평가값이 빠진 경우")
        class WhenRatingMissing {

            @Test
            @DisplayName("rating 필드 검증 실패와 함께 400을 반환한다")
            void shouldReturnBadRequest() throws Exception {
                // given
                SubmitParticipantPairingFeedbackRequest request =
                        new SubmitParticipantPairingFeedbackRequest(
                                pairing.drinkId(), pairing.anjuId(), null);

                // when & then
                mockMvc.perform(
                                post("/api/v1/participants/pairing/feedback")
                                        .header("X-Link-Token", linkToken)
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(objectMapper.writeValueAsString(request)))
                        .andExpect(status().isBadRequest())
                        .andExpect(
                                jsonPath("$.data.code")
                                        .value(CommonErrorCode.METHOD_ARGUMENT_NOT_VALID.getCode()))
                        .andExpect(jsonPath("$.data.violations[0].field").value("rating"));
            }
        }
    }

    private Gathering saveGathering() {
        Member member = memberHelper.save(MemberFixture.KAKAO);

        return gatheringHelper.save(GatheringFixture.FRIDAY_DINNER, member.getId());
    }

    private String saveGatheringWithJoinToken() {
        return gatheringHelper.issueJoinToken(saveGathering());
    }

    private GatheringPairing choosePairing(Gathering gathering) {
        Drink drink = catalogHelper.saveDrink(DrinkFixture.SOJU);
        Anju anju = catalogHelper.saveAnju(AnjuFixture.GOLBAENGI_MUCHIM);
        GatheringPairing pairing =
                GatheringPairingFixture.FRIEND_CASUAL.createWithReferences(
                        drink.getId(), anju.getId());
        gatheringHelper.choosePairing(gathering, pairing);

        return pairing;
    }
}
