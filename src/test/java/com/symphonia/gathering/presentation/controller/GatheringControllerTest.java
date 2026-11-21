package com.symphonia.gathering.presentation.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import com.symphonia.IntegrationTest;
import com.symphonia.auth.helper.AuthHelper;
import com.symphonia.catalog.domain.entity.Anju;
import com.symphonia.catalog.domain.entity.Drink;
import com.symphonia.catalog.domain.error.CatalogErrorCode;
import com.symphonia.catalog.fixture.AnjuFixture;
import com.symphonia.catalog.fixture.DrinkFixture;
import com.symphonia.catalog.helper.CatalogHelper;
import com.symphonia.common.exception.error.CommonErrorCode;
import com.symphonia.gathering.domain.entity.Gathering;
import com.symphonia.gathering.domain.entity.Participant;
import com.symphonia.gathering.domain.entity.Settlement;
import com.symphonia.gathering.domain.error.GatheringErrorCode;
import com.symphonia.gathering.domain.policy.GatheringPolicy;
import com.symphonia.gathering.fixture.GatheringFixture;
import com.symphonia.gathering.fixture.GatheringPairingFixture;
import com.symphonia.gathering.fixture.ParticipantFixture;
import com.symphonia.gathering.fixture.SettlementFixture;
import com.symphonia.gathering.helper.GatheringHelper;
import com.symphonia.gathering.helper.SettlementHelper;
import com.symphonia.gathering.presentation.dto.request.AddRoundRequest;
import com.symphonia.gathering.presentation.dto.request.FixedAmountRequest;
import com.symphonia.gathering.presentation.dto.request.GatheringPairingRequest;
import com.symphonia.gathering.presentation.dto.request.JoinGatheringRequest;
import com.symphonia.gathering.presentation.dto.request.OpenGatheringRequest;
import com.symphonia.gathering.presentation.dto.request.RecordSettlementRequest;
import com.symphonia.gathering.presentation.dto.request.UpdateSettlementRequest;
import com.symphonia.member.domain.entity.Member;
import com.symphonia.member.fixture.MemberFixture;
import com.symphonia.member.helper.MemberHelper;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;

@Sql("/sql/clear-catalog.sql")
class GatheringControllerTest extends IntegrationTest {

    private static final String MASKED_KIM_PHONE_NUMBER = "010-****-5678";
    private static final UUID CLIENT_REQUEST_ID =
            UUID.fromString("0b6c1a52-6f4e-4d0a-9d52-7a1f3c1e8b11");

    @Autowired private GatheringHelper gatheringHelper;
    @Autowired private SettlementHelper settlementHelper;
    @Autowired private MemberHelper memberHelper;
    @Autowired private AuthHelper authHelper;
    @Autowired private CatalogHelper catalogHelper;
    @Autowired private JdbcTemplate jdbcTemplate;

    private Member host;
    private String hostToken;
    private Gathering gathering;
    private Participant payer;
    private Participant lee;
    private Participant park;

    @Nested
    @DisplayName("POST /api/v1/gatherings는")
    class Open {

        @Nested
        @DisplayName("인증된 요청인 경우")
        class WhenAuthenticated {

            @Test
            @DisplayName("모임을 열고 참여 토큰과 함께 201을 반환한다")
            void shouldOpenGathering() throws Exception {
                // given
                Member member = memberHelper.save(MemberFixture.KAKAO);
                String token = authHelper.bearerTokenFor(member);
                OpenGatheringRequest request =
                        new OpenGatheringRequest(GatheringFixture.FRIDAY_DINNER.getName(), null);

                // when & then
                mockMvc.perform(
                                post("/api/v1/gatherings")
                                        .header(HttpHeaders.AUTHORIZATION, token)
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(objectMapper.writeValueAsString(request)))
                        .andExpect(status().isCreated())
                        .andExpect(jsonPath("$.data.gatheringId").isNumber())
                        .andExpect(jsonPath("$.data.joinToken").isNotEmpty())
                        .andExpect(jsonPath("$.data.expiresAt").isNotEmpty());
            }
        }

        @Nested
        @DisplayName("추천 결과에서 고른 페어링과 함께 여는 경우")
        class WhenPairingGiven {

            @Test
            @DisplayName("모임을 열고 페어링을 함께 저장한 뒤 201을 반환한다")
            void shouldOpenGatheringWithPairing() throws Exception {
                // given
                Member member = memberHelper.save(MemberFixture.KAKAO);
                String token = authHelper.bearerTokenFor(member);
                GatheringPairingRequest pairing = seedPairingRequest();
                OpenGatheringRequest request =
                        new OpenGatheringRequest(GatheringFixture.FRIDAY_DINNER.getName(), pairing);

                // when & then
                mockMvc.perform(
                                post("/api/v1/gatherings")
                                        .header(HttpHeaders.AUTHORIZATION, token)
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(objectMapper.writeValueAsString(request)))
                        .andExpect(status().isCreated())
                        .andExpect(jsonPath("$.data.gatheringId").isNumber());
                assertThat(countPairingsOf(pairing)).isOne();
            }
        }

        @Nested
        @DisplayName("페어링의 필수 필드가 빠진 경우")
        class WhenPairingFieldMissing {

            @Test
            @DisplayName("pairing.drinkId 필드 검증 실패와 함께 400을 반환한다")
            void shouldReturnBadRequest() throws Exception {
                // given
                Member member = memberHelper.save(MemberFixture.KAKAO);
                String token = authHelper.bearerTokenFor(member);
                GatheringPairingFixture pairing = GatheringPairingFixture.FRIEND_CASUAL;
                OpenGatheringRequest request =
                        new OpenGatheringRequest(
                                GatheringFixture.FRIDAY_DINNER.getName(),
                                new GatheringPairingRequest(
                                        null,
                                        pairing.getAnjuId(),
                                        pairing.getRelationshipType(),
                                        pairing.getMoodType()));

                // when & then
                mockMvc.perform(
                                post("/api/v1/gatherings")
                                        .header(HttpHeaders.AUTHORIZATION, token)
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(objectMapper.writeValueAsString(request)))
                        .andExpect(status().isBadRequest())
                        .andExpect(
                                jsonPath("$.data.code")
                                        .value(CommonErrorCode.METHOD_ARGUMENT_NOT_VALID.getCode()))
                        .andExpect(jsonPath("$.data.violations[0].field").value("pairing.drinkId"));
            }
        }

        @Nested
        @DisplayName("인증 토큰이 없는 경우")
        class WhenUnauthenticated {

            @Test
            @DisplayName("401을 반환한다")
            void shouldReturnUnauthorized() throws Exception {
                // given
                OpenGatheringRequest request =
                        new OpenGatheringRequest(GatheringFixture.FRIDAY_DINNER.getName(), null);

                // when & then
                mockMvc.perform(
                                post("/api/v1/gatherings")
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(objectMapper.writeValueAsString(request)))
                        .andExpect(status().isUnauthorized());
            }
        }
    }

    @Nested
    @DisplayName("POST /api/v1/gatherings/{gatheringId}/join-token은")
    class Reissue {

        @Nested
        @DisplayName("모임을 만든 회원의 요청인 경우")
        class WhenMemberIsHost {

            @Test
            @DisplayName("새 참여 토큰과 함께 201을 반환한다")
            void shouldReissueJoinToken() throws Exception {
                // given
                Member member = memberHelper.save(MemberFixture.KAKAO);
                String token = authHelper.bearerTokenFor(member);
                Gathering gathering =
                        gatheringHelper.save(GatheringFixture.FRIDAY_DINNER, member.getId());
                String oldJoinToken = gatheringHelper.issueJoinToken(gathering);

                // when & then
                mockMvc.perform(
                                post(
                                                "/api/v1/gatherings/{gatheringId}/join-token",
                                                gathering.getId())
                                        .header(HttpHeaders.AUTHORIZATION, token))
                        .andExpect(status().isCreated())
                        .andExpect(jsonPath("$.data.gatheringId").value(gathering.getId()))
                        .andExpect(jsonPath("$.data.joinToken").isNotEmpty())
                        .andExpect(jsonPath("$.data.joinToken").value(not(oldJoinToken)))
                        .andExpect(jsonPath("$.data.expiresAt").isNotEmpty());
            }
        }

        @Nested
        @DisplayName("모임을 만들지 않은 다른 회원의 요청인 경우")
        class WhenMemberIsNotHost {

            @Test
            @DisplayName("NOT_GATHERING_HOST 코드와 함께 403을 반환한다")
            void shouldReturnForbidden() throws Exception {
                // given
                Member host = memberHelper.save(MemberFixture.KAKAO);
                Member otherMember = memberHelper.save(MemberFixture.GOOGLE);
                String token = authHelper.bearerTokenFor(otherMember);
                Gathering gathering =
                        gatheringHelper.save(GatheringFixture.FRIDAY_DINNER, host.getId());

                // when & then
                mockMvc.perform(
                                post(
                                                "/api/v1/gatherings/{gatheringId}/join-token",
                                                gathering.getId())
                                        .header(HttpHeaders.AUTHORIZATION, token))
                        .andExpect(status().isForbidden())
                        .andExpect(
                                jsonPath("$.data.code")
                                        .value(GatheringErrorCode.NOT_GATHERING_HOST.getCode()));
            }
        }

        @Nested
        @DisplayName("인증 토큰이 없는 경우")
        class WhenUnauthenticated {

            @Test
            @DisplayName("401을 반환한다")
            void shouldReturnUnauthorized() throws Exception {
                // given
                Member member = memberHelper.save(MemberFixture.KAKAO);
                Gathering gathering =
                        gatheringHelper.save(GatheringFixture.FRIDAY_DINNER, member.getId());

                // when & then
                mockMvc.perform(
                                post(
                                        "/api/v1/gatherings/{gatheringId}/join-token",
                                        gathering.getId()))
                        .andExpect(status().isUnauthorized());
            }
        }
    }

    @Nested
    @DisplayName("GET /api/v1/gatherings/{gatheringId}/participants는")
    class GetParticipants {

        @Nested
        @DisplayName("모임을 만든 회원의 요청인 경우")
        class WhenMemberIsHost {

            @Test
            @DisplayName("참여자의 이름과 마스킹한 휴대폰을 개인 링크 토큰 없이 200으로 반환한다")
            void shouldReturnParticipants() throws Exception {
                // given
                Member member = memberHelper.save(MemberFixture.KAKAO);
                String token = authHelper.bearerTokenFor(member);
                Gathering gathering =
                        gatheringHelper.join(
                                gatheringHelper.save(
                                        GatheringFixture.FRIDAY_DINNER, member.getId()),
                                ParticipantFixture.KIM,
                                ParticipantFixture.LEE);

                // when & then
                mockMvc.perform(
                                get(
                                                "/api/v1/gatherings/{gatheringId}/participants",
                                                gathering.getId())
                                        .header(HttpHeaders.AUTHORIZATION, token))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.data.length()").value(2))
                        .andExpect(
                                jsonPath("$.data[0].id")
                                        .value(gathering.getParticipants().get(0).getId()))
                        .andExpect(
                                jsonPath("$.data[0].name").value(ParticipantFixture.KIM.getName()))
                        .andExpect(jsonPath("$.data[0].maskedPhoneNumber").value("010-****-5678"))
                        .andExpect(jsonPath("$.data[0].linkToken").doesNotExist())
                        .andExpect(
                                jsonPath("$.data[1].name").value(ParticipantFixture.LEE.getName()));
            }
        }

        @Nested
        @DisplayName("모임을 만들지 않은 다른 회원의 요청인 경우")
        class WhenMemberIsNotHost {

            @Test
            @DisplayName("NOT_GATHERING_HOST 코드와 함께 403을 반환한다")
            void shouldReturnForbidden() throws Exception {
                // given
                Member host = memberHelper.save(MemberFixture.KAKAO);
                Member otherMember = memberHelper.save(MemberFixture.GOOGLE);
                String token = authHelper.bearerTokenFor(otherMember);
                Gathering gathering =
                        gatheringHelper.save(GatheringFixture.FRIDAY_DINNER, host.getId());

                // when & then
                mockMvc.perform(
                                get(
                                                "/api/v1/gatherings/{gatheringId}/participants",
                                                gathering.getId())
                                        .header(HttpHeaders.AUTHORIZATION, token))
                        .andExpect(status().isForbidden())
                        .andExpect(
                                jsonPath("$.data.code")
                                        .value(GatheringErrorCode.NOT_GATHERING_HOST.getCode()));
            }
        }

        @Nested
        @DisplayName("인증 토큰이 없는 경우")
        class WhenUnauthenticated {

            @Test
            @DisplayName("401을 반환한다")
            void shouldReturnUnauthorized() throws Exception {
                // given
                Member member = memberHelper.save(MemberFixture.KAKAO);
                Gathering gathering =
                        gatheringHelper.save(GatheringFixture.FRIDAY_DINNER, member.getId());

                // when & then
                mockMvc.perform(
                                get(
                                        "/api/v1/gatherings/{gatheringId}/participants",
                                        gathering.getId()))
                        .andExpect(status().isUnauthorized());
            }
        }
    }

    @Nested
    @DisplayName("POST /api/v1/gatherings/{gatheringId}/participants는")
    class JoinAsHost {

        @Nested
        @DisplayName("모임을 만든 회원이 동의하고 요청하는 경우")
        class WhenMemberIsHost {

            @Test
            @DisplayName("참여자로 등록하고 마스킹한 휴대폰과 개인 링크 토큰과 함께 201을 반환한다")
            void shouldJoinAsHost() throws Exception {
                // given
                Member member = memberHelper.save(MemberFixture.KAKAO);
                String token = authHelper.bearerTokenFor(member);
                Gathering gathering =
                        gatheringHelper.save(GatheringFixture.FRIDAY_DINNER, member.getId());

                // when & then
                performJoinAsHost(gathering.getId(), token, hostJoinRequest(true))
                        .andExpect(status().isCreated())
                        .andExpect(jsonPath("$.data.id").isNumber())
                        .andExpect(jsonPath("$.data.name").value(ParticipantFixture.KIM.getName()))
                        .andExpect(jsonPath("$.data.maskedPhoneNumber").value("010-****-5678"))
                        .andExpect(jsonPath("$.data.linkToken").isNotEmpty());
            }

            @Test
            @DisplayName("참여한 모임장이 참여자 목록에 나타난다")
            void shouldListHostAmongParticipants() throws Exception {
                // given
                Member member = memberHelper.save(MemberFixture.KAKAO);
                String token = authHelper.bearerTokenFor(member);
                Gathering gathering =
                        gatheringHelper.save(GatheringFixture.FRIDAY_DINNER, member.getId());

                // when
                MvcResult result =
                        performJoinAsHost(gathering.getId(), token, hostJoinRequest(true))
                                .andReturn();

                // then
                Number participantId =
                        JsonPath.read(result.getResponse().getContentAsString(), "$.data.id");
                mockMvc.perform(
                                get(
                                                "/api/v1/gatherings/{gatheringId}/participants",
                                                gathering.getId())
                                        .header(HttpHeaders.AUTHORIZATION, token))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.data[0].id").value(participantId.intValue()))
                        .andExpect(
                                jsonPath("$.data[0].name").value(ParticipantFixture.KIM.getName()));
            }
        }

        @Nested
        @DisplayName("동의하지 않은 경우")
        class WhenConsentNotAgreed {

            @Test
            @DisplayName("CONSENT_REQUIRED 코드와 함께 400을 반환한다")
            void shouldReturnBadRequest() throws Exception {
                // given
                Member member = memberHelper.save(MemberFixture.KAKAO);
                String token = authHelper.bearerTokenFor(member);
                Gathering gathering =
                        gatheringHelper.save(GatheringFixture.FRIDAY_DINNER, member.getId());

                // when & then
                performJoinAsHost(gathering.getId(), token, hostJoinRequest(false))
                        .andExpect(status().isBadRequest())
                        .andExpect(
                                jsonPath("$.data.code")
                                        .value(GatheringErrorCode.CONSENT_REQUIRED.getCode()));
            }
        }

        @Nested
        @DisplayName("이미 참여한 휴대폰인 경우")
        class WhenPhoneNumberAlreadyJoined {

            @Test
            @DisplayName("PARTICIPANT_ALREADY_JOINED 코드와 함께 409를 반환한다")
            void shouldReturnConflict() throws Exception {
                // given
                Member member = memberHelper.save(MemberFixture.KAKAO);
                String token = authHelper.bearerTokenFor(member);
                Gathering gathering =
                        gatheringHelper.join(
                                gatheringHelper.save(
                                        GatheringFixture.FRIDAY_DINNER, member.getId()),
                                ParticipantFixture.KIM);

                // when & then
                performJoinAsHost(gathering.getId(), token, hostJoinRequest(true))
                        .andExpect(status().isConflict())
                        .andExpect(
                                jsonPath("$.data.code")
                                        .value(
                                                GatheringErrorCode.PARTICIPANT_ALREADY_JOINED
                                                        .getCode()));
            }
        }

        @Nested
        @DisplayName("모임을 만든 지 48시간이 지난 경우")
        class WhenRecordingPeriodClosed {

            @Test
            @DisplayName("GATHERING_JOIN_CLOSED 코드와 함께 400을 반환한다")
            void shouldReturnBadRequest() throws Exception {
                // given
                Member member = memberHelper.save(MemberFixture.KAKAO);
                String token = authHelper.bearerTokenFor(member);
                Gathering gathering =
                        gatheringHelper.save(
                                GatheringFixture.FRIDAY_DINNER,
                                member.getId(),
                                LocalDateTime.now().minusHours(48));

                // when & then
                performJoinAsHost(gathering.getId(), token, hostJoinRequest(true))
                        .andExpect(status().isBadRequest())
                        .andExpect(
                                jsonPath("$.data.code")
                                        .value(GatheringErrorCode.GATHERING_JOIN_CLOSED.getCode()));
            }
        }

        @Nested
        @DisplayName("모임을 만들지 않은 다른 회원의 요청인 경우")
        class WhenMemberIsNotHost {

            @Test
            @DisplayName("NOT_GATHERING_HOST 코드와 함께 403을 반환한다")
            void shouldReturnForbidden() throws Exception {
                // given
                Member host = memberHelper.save(MemberFixture.KAKAO);
                Member otherMember = memberHelper.save(MemberFixture.GOOGLE);
                String token = authHelper.bearerTokenFor(otherMember);
                Gathering gathering =
                        gatheringHelper.save(GatheringFixture.FRIDAY_DINNER, host.getId());

                // when & then
                performJoinAsHost(gathering.getId(), token, hostJoinRequest(true))
                        .andExpect(status().isForbidden())
                        .andExpect(
                                jsonPath("$.data.code")
                                        .value(GatheringErrorCode.NOT_GATHERING_HOST.getCode()));
            }
        }

        @Nested
        @DisplayName("인증 토큰이 없는 경우")
        class WhenUnauthenticated {

            @Test
            @DisplayName("401을 반환한다")
            void shouldReturnUnauthorized() throws Exception {
                // given
                Member member = memberHelper.save(MemberFixture.KAKAO);
                Gathering gathering =
                        gatheringHelper.save(GatheringFixture.FRIDAY_DINNER, member.getId());

                // when & then
                mockMvc.perform(
                                post(
                                                "/api/v1/gatherings/{gatheringId}/participants",
                                                gathering.getId())
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(
                                                objectMapper.writeValueAsString(
                                                        hostJoinRequest(true))))
                        .andExpect(status().isUnauthorized());
            }
        }

        private JoinGatheringRequest hostJoinRequest(boolean consentAgreed) {
            return new JoinGatheringRequest(
                    ParticipantFixture.KIM.getName(),
                    ParticipantFixture.KIM.getPhoneNumber(),
                    consentAgreed,
                    GatheringPolicy.CONSENT_VERSION);
        }

        private ResultActions performJoinAsHost(
                Long gatheringId, String token, JoinGatheringRequest request) throws Exception {
            return mockMvc.perform(
                    post("/api/v1/gatherings/{gatheringId}/participants", gatheringId)
                            .header(HttpHeaders.AUTHORIZATION, token)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)));
        }
    }

    @Nested
    @DisplayName("DELETE /api/v1/gatherings/{gatheringId}/participants/{participantId}는")
    class RemoveParticipant {

        @Nested
        @DisplayName("정산 건에 없는 참여자인 경우")
        class WhenParticipantRemovable {

            @Test
            @DisplayName("참여자와 개인 링크를 지우고 204를 반환한다")
            void shouldRemoveParticipant() throws Exception {
                // given
                Member member = memberHelper.save(MemberFixture.KAKAO);
                String token = authHelper.bearerTokenFor(member);
                Gathering gathering =
                        gatheringHelper.join(
                                gatheringHelper.save(
                                        GatheringFixture.FRIDAY_DINNER, member.getId()),
                                ParticipantFixture.KIM,
                                ParticipantFixture.LEE);
                Participant kim = gathering.getParticipants().get(0);
                String kimLinkToken = gatheringHelper.issueLinkToken(kim);

                // when
                mockMvc.perform(
                                delete(
                                                "/api/v1/gatherings/{gatheringId}/participants/{participantId}",
                                                gathering.getId(),
                                                kim.getId())
                                        .header(HttpHeaders.AUTHORIZATION, token))
                        .andExpect(status().isNoContent());

                // then
                mockMvc.perform(
                                get(
                                                "/api/v1/gatherings/{gatheringId}/participants",
                                                gathering.getId())
                                        .header(HttpHeaders.AUTHORIZATION, token))
                        .andExpect(jsonPath("$.data.length()").value(1))
                        .andExpect(
                                jsonPath("$.data[0].name").value(ParticipantFixture.LEE.getName()));
                mockMvc.perform(get("/api/v1/shares").header("X-Link-Token", kimLinkToken))
                        .andExpect(status().isNotFound())
                        .andExpect(
                                jsonPath("$.data.code")
                                        .value(GatheringErrorCode.LINK_TOKEN_NOT_FOUND.getCode()));
            }
        }

        @Nested
        @DisplayName("취소한 정산 건의 인원인 참여자인 경우")
        class WhenParticipantInCancelledSettlement {

            @Test
            @DisplayName("PARTICIPANT_UNREMOVABLE 코드와 함께 409를 반환한다")
            void shouldReturnConflict() throws Exception {
                // given
                Member member = memberHelper.save(MemberFixture.KAKAO);
                String token = authHelper.bearerTokenFor(member);
                Gathering gathering =
                        gatheringHelper.join(
                                gatheringHelper.save(
                                        GatheringFixture.FRIDAY_DINNER, member.getId()),
                                ParticipantFixture.KIM,
                                ParticipantFixture.LEE);
                Participant kim = gathering.getParticipants().get(0);
                Participant lee = gathering.getParticipants().get(1);
                settlementHelper.cancel(
                        settlementHelper.record(SettlementFixture.DINNER, gathering, kim));

                // when & then
                mockMvc.perform(
                                delete(
                                                "/api/v1/gatherings/{gatheringId}/participants/{participantId}",
                                                gathering.getId(),
                                                lee.getId())
                                        .header(HttpHeaders.AUTHORIZATION, token))
                        .andExpect(status().isConflict())
                        .andExpect(
                                jsonPath("$.data.code")
                                        .value(
                                                GatheringErrorCode.PARTICIPANT_UNREMOVABLE
                                                        .getCode()));
            }
        }

        @Nested
        @DisplayName("모임에 없는 참여자인 경우")
        class WhenParticipantNotFound {

            @Test
            @DisplayName("PARTICIPANT_NOT_FOUND 코드와 함께 404를 반환한다")
            void shouldReturnNotFound() throws Exception {
                // given
                Member member = memberHelper.save(MemberFixture.KAKAO);
                String token = authHelper.bearerTokenFor(member);
                Gathering gathering =
                        gatheringHelper.save(GatheringFixture.FRIDAY_DINNER, member.getId());

                // when & then
                mockMvc.perform(
                                delete(
                                                "/api/v1/gatherings/{gatheringId}/participants/{participantId}",
                                                gathering.getId(),
                                                Long.MAX_VALUE)
                                        .header(HttpHeaders.AUTHORIZATION, token))
                        .andExpect(status().isNotFound())
                        .andExpect(
                                jsonPath("$.data.code")
                                        .value(GatheringErrorCode.PARTICIPANT_NOT_FOUND.getCode()));
            }
        }

        @Nested
        @DisplayName("모임을 만들지 않은 다른 회원의 요청인 경우")
        class WhenMemberIsNotHost {

            @Test
            @DisplayName("NOT_GATHERING_HOST 코드와 함께 403을 반환한다")
            void shouldReturnForbidden() throws Exception {
                // given
                Member host = memberHelper.save(MemberFixture.KAKAO);
                Member otherMember = memberHelper.save(MemberFixture.GOOGLE);
                String token = authHelper.bearerTokenFor(otherMember);
                Gathering gathering =
                        gatheringHelper.join(
                                gatheringHelper.save(GatheringFixture.FRIDAY_DINNER, host.getId()),
                                ParticipantFixture.KIM);

                // when & then
                mockMvc.perform(
                                delete(
                                                "/api/v1/gatherings/{gatheringId}/participants/{participantId}",
                                                gathering.getId(),
                                                gathering.getParticipants().getFirst().getId())
                                        .header(HttpHeaders.AUTHORIZATION, token))
                        .andExpect(status().isForbidden())
                        .andExpect(
                                jsonPath("$.data.code")
                                        .value(GatheringErrorCode.NOT_GATHERING_HOST.getCode()));
            }
        }
    }

    @Nested
    @DisplayName("POST /api/v1/gatherings/{gatheringId}/participants/{participantId}/link-token은")
    class ReissueLinkToken {

        @Nested
        @DisplayName("모임을 만든 회원의 요청인 경우")
        class WhenMemberIsHost {

            @Test
            @DisplayName("새 개인 링크 토큰과 함께 201을 반환하고 이전 링크는 무효가 된다")
            void shouldReissueLinkToken() throws Exception {
                // given
                Member member = memberHelper.save(MemberFixture.KAKAO);
                String token = authHelper.bearerTokenFor(member);
                Gathering gathering =
                        gatheringHelper.join(
                                gatheringHelper.save(
                                        GatheringFixture.FRIDAY_DINNER, member.getId()),
                                ParticipantFixture.KIM);
                Participant kim = gathering.getParticipants().getFirst();
                String oldLinkToken = gatheringHelper.issueLinkToken(kim);

                // when
                ResultActions actions =
                        mockMvc.perform(
                                post(
                                                "/api/v1/gatherings/{gatheringId}/participants/{participantId}/link-token",
                                                gathering.getId(),
                                                kim.getId())
                                        .header(HttpHeaders.AUTHORIZATION, token));

                // then
                actions.andExpect(status().isCreated())
                        .andExpect(jsonPath("$.data.id").value(kim.getId()))
                        .andExpect(jsonPath("$.data.maskedPhoneNumber").value("010-****-5678"));
                String newLinkToken =
                        objectMapper
                                .readTree(actions.andReturn().getResponse().getContentAsString())
                                .at("/data/linkToken")
                                .asText();
                mockMvc.perform(get("/api/v1/shares").header("X-Link-Token", newLinkToken))
                        .andExpect(status().isOk());
                mockMvc.perform(get("/api/v1/shares").header("X-Link-Token", oldLinkToken))
                        .andExpect(status().isNotFound())
                        .andExpect(
                                jsonPath("$.data.code")
                                        .value(GatheringErrorCode.LINK_TOKEN_NOT_FOUND.getCode()));
            }
        }

        @Nested
        @DisplayName("모임에 없는 참여자인 경우")
        class WhenParticipantNotFound {

            @Test
            @DisplayName("PARTICIPANT_NOT_FOUND 코드와 함께 404를 반환한다")
            void shouldReturnNotFound() throws Exception {
                // given
                Member member = memberHelper.save(MemberFixture.KAKAO);
                String token = authHelper.bearerTokenFor(member);
                Gathering gathering =
                        gatheringHelper.save(GatheringFixture.FRIDAY_DINNER, member.getId());

                // when & then
                mockMvc.perform(
                                post(
                                                "/api/v1/gatherings/{gatheringId}/participants/{participantId}/link-token",
                                                gathering.getId(),
                                                Long.MAX_VALUE)
                                        .header(HttpHeaders.AUTHORIZATION, token))
                        .andExpect(status().isNotFound())
                        .andExpect(
                                jsonPath("$.data.code")
                                        .value(GatheringErrorCode.PARTICIPANT_NOT_FOUND.getCode()));
            }
        }

        @Nested
        @DisplayName("모임을 만들지 않은 다른 회원의 요청인 경우")
        class WhenMemberIsNotHost {

            @Test
            @DisplayName("NOT_GATHERING_HOST 코드와 함께 403을 반환한다")
            void shouldReturnForbidden() throws Exception {
                // given
                Member host = memberHelper.save(MemberFixture.KAKAO);
                Member otherMember = memberHelper.save(MemberFixture.GOOGLE);
                String token = authHelper.bearerTokenFor(otherMember);
                Gathering gathering =
                        gatheringHelper.join(
                                gatheringHelper.save(GatheringFixture.FRIDAY_DINNER, host.getId()),
                                ParticipantFixture.KIM);

                // when & then
                mockMvc.perform(
                                post(
                                                "/api/v1/gatherings/{gatheringId}/participants/{participantId}/link-token",
                                                gathering.getId(),
                                                gathering.getParticipants().getFirst().getId())
                                        .header(HttpHeaders.AUTHORIZATION, token))
                        .andExpect(status().isForbidden())
                        .andExpect(
                                jsonPath("$.data.code")
                                        .value(GatheringErrorCode.NOT_GATHERING_HOST.getCode()));
            }
        }
    }

    @Nested
    @DisplayName("PUT /api/v1/gatherings/{gatheringId}/pairing은")
    class ChoosePairing {

        @Nested
        @DisplayName("모임을 만든 회원이 기록 기간 안에 고르는 경우")
        class WhenMemberIsHost {

            @Test
            @DisplayName("모임의 페어링을 저장하고 200을 반환한다")
            void shouldChoosePairing() throws Exception {
                // given
                Member member = memberHelper.save(MemberFixture.KAKAO);
                String token = authHelper.bearerTokenFor(member);
                Gathering gathering =
                        gatheringHelper.save(GatheringFixture.FRIDAY_DINNER, member.getId());
                GatheringPairingRequest request = seedPairingRequest();

                // when & then
                mockMvc.perform(
                                put("/api/v1/gatherings/{gatheringId}/pairing", gathering.getId())
                                        .header(HttpHeaders.AUTHORIZATION, token)
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(objectMapper.writeValueAsString(request)))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.ok").value(true));
                assertThat(countPairingsOf(request)).isOne();
            }

            @Test
            @DisplayName("분위기 없이 받은 추천의 페어링도 저장하고 200을 반환한다")
            void shouldChoosePairingWhenMoodTypeMissing() throws Exception {
                // given
                Member member = memberHelper.save(MemberFixture.KAKAO);
                String token = authHelper.bearerTokenFor(member);
                Gathering gathering =
                        gatheringHelper.save(GatheringFixture.FRIDAY_DINNER, member.getId());
                GatheringPairingRequest seeded = seedPairingRequest();
                GatheringPairingRequest request =
                        new GatheringPairingRequest(
                                seeded.drinkId(), seeded.anjuId(), seeded.relationshipType(), null);

                // when & then
                mockMvc.perform(
                                put("/api/v1/gatherings/{gatheringId}/pairing", gathering.getId())
                                        .header(HttpHeaders.AUTHORIZATION, token)
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(objectMapper.writeValueAsString(request)))
                        .andExpect(status().isOk());
                assertThat(countPairingsOf(request)).isOne();
            }
        }

        @Nested
        @DisplayName("모임을 만들지 않은 다른 회원의 요청인 경우")
        class WhenMemberIsNotHost {

            @Test
            @DisplayName("NOT_GATHERING_HOST 코드와 함께 403을 반환한다")
            void shouldReturnForbidden() throws Exception {
                // given
                Member host = memberHelper.save(MemberFixture.KAKAO);
                Member otherMember = memberHelper.save(MemberFixture.GOOGLE);
                String token = authHelper.bearerTokenFor(otherMember);
                Gathering gathering =
                        gatheringHelper.save(GatheringFixture.FRIDAY_DINNER, host.getId());
                GatheringPairingRequest request = seedPairingRequest();

                // when & then
                mockMvc.perform(
                                put("/api/v1/gatherings/{gatheringId}/pairing", gathering.getId())
                                        .header(HttpHeaders.AUTHORIZATION, token)
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(objectMapper.writeValueAsString(request)))
                        .andExpect(status().isForbidden())
                        .andExpect(
                                jsonPath("$.data.code")
                                        .value(GatheringErrorCode.NOT_GATHERING_HOST.getCode()));
            }
        }

        @Nested
        @DisplayName("페어링이 가리키는 술이 카탈로그에 없는 경우")
        class WhenDrinkNotFound {

            @Test
            @DisplayName("DRINK_NOT_FOUND 코드와 함께 404를 반환한다")
            void shouldReturnNotFound() throws Exception {
                // given
                Member member = memberHelper.save(MemberFixture.KAKAO);
                String token = authHelper.bearerTokenFor(member);
                Gathering gathering =
                        gatheringHelper.save(GatheringFixture.FRIDAY_DINNER, member.getId());
                GatheringPairingRequest seeded = seedPairingRequest();
                GatheringPairingRequest request =
                        new GatheringPairingRequest(
                                Long.MAX_VALUE,
                                seeded.anjuId(),
                                seeded.relationshipType(),
                                seeded.moodType());

                // when & then
                mockMvc.perform(
                                put("/api/v1/gatherings/{gatheringId}/pairing", gathering.getId())
                                        .header(HttpHeaders.AUTHORIZATION, token)
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(objectMapper.writeValueAsString(request)))
                        .andExpect(status().isNotFound())
                        .andExpect(
                                jsonPath("$.data.code")
                                        .value(CatalogErrorCode.DRINK_NOT_FOUND.getCode()));
            }
        }

        @Nested
        @DisplayName("인증 토큰이 없는 경우")
        class WhenUnauthenticated {

            @Test
            @DisplayName("401을 반환한다")
            void shouldReturnUnauthorized() throws Exception {
                // given
                Member member = memberHelper.save(MemberFixture.KAKAO);
                Gathering gathering =
                        gatheringHelper.save(GatheringFixture.FRIDAY_DINNER, member.getId());
                GatheringPairingRequest request = seedPairingRequest();

                // when & then
                mockMvc.perform(
                                put("/api/v1/gatherings/{gatheringId}/pairing", gathering.getId())
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(objectMapper.writeValueAsString(request)))
                        .andExpect(status().isUnauthorized());
            }
        }
    }

    @Nested
    @DisplayName("PUT /api/v1/gatherings/{gatheringId}/recording-extension은")
    class ExtendRecording {

        @Nested
        @DisplayName("기록 기간이 끝나기 전에 모임을 만든 회원이 요청하는 경우")
        class WhenMemberIsHost {

            @Test
            @DisplayName("기록 기간을 연장하고 연장한 기간이 끝나는 시각과 함께 200을 반환한다")
            void shouldExtendRecording() throws Exception {
                // given
                Member member = memberHelper.save(MemberFixture.KAKAO);
                String token = authHelper.bearerTokenFor(member);
                Gathering gathering =
                        gatheringHelper.save(
                                GatheringFixture.FRIDAY_DINNER,
                                member.getId(),
                                LocalDateTime.now().minusHours(40));

                // when & then
                performExtendRecording(gathering.getId(), token)
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.data.recordingClosesAt").isNotEmpty());
                assertThat(isRecordingExtended(gathering.getId())).isTrue();
            }

            @Test
            @DisplayName("이미 연장한 모임에 요청해도 연장한 기간이 끝나는 시각과 함께 200을 반환한다")
            void shouldReturnExtendedClosesAtWhenAlreadyExtended() throws Exception {
                // given
                Member member = memberHelper.save(MemberFixture.KAKAO);
                String token = authHelper.bearerTokenFor(member);
                LocalDateTime openedAt =
                        LocalDateTime.now().minusHours(40).truncatedTo(ChronoUnit.SECONDS);
                Gathering gathering =
                        gatheringHelper.save(
                                GatheringFixture.FRIDAY_DINNER, member.getId(), openedAt);
                jdbcTemplate.update(
                        "UPDATE gathering SET recording_extended = TRUE WHERE id = ?",
                        gathering.getId());

                // when & then
                performExtendRecording(gathering.getId(), token)
                        .andExpect(status().isOk())
                        .andExpect(
                                jsonPath("$.data.recordingClosesAt")
                                        .value(
                                                DateTimeFormatter.ISO_LOCAL_DATE_TIME.format(
                                                        openedAt.plusHours(96))));
            }
        }

        @Nested
        @DisplayName("모임을 만든 지 48시간이 지난 경우")
        class WhenRecordingPeriodClosed {

            @Test
            @DisplayName("GATHERING_RECORDING_EXTENSION_CLOSED 코드와 함께 400을 반환하고 연장하지 않는다")
            void shouldReturnBadRequest() throws Exception {
                // given
                Member member = memberHelper.save(MemberFixture.KAKAO);
                String token = authHelper.bearerTokenFor(member);
                Gathering gathering =
                        gatheringHelper.save(
                                GatheringFixture.FRIDAY_DINNER,
                                member.getId(),
                                LocalDateTime.now().minusHours(48));

                // when & then
                performExtendRecording(gathering.getId(), token)
                        .andExpect(status().isBadRequest())
                        .andExpect(
                                jsonPath("$.data.code")
                                        .value(
                                                GatheringErrorCode
                                                        .GATHERING_RECORDING_EXTENSION_CLOSED
                                                        .getCode()));
                assertThat(isRecordingExtended(gathering.getId())).isFalse();
            }
        }

        @Nested
        @DisplayName("모임을 만들지 않은 다른 회원의 요청인 경우")
        class WhenMemberIsNotHost {

            @Test
            @DisplayName("NOT_GATHERING_HOST 코드와 함께 403을 반환하고 연장하지 않는다")
            void shouldReturnForbidden() throws Exception {
                // given
                Member host = memberHelper.save(MemberFixture.KAKAO);
                Member otherMember = memberHelper.save(MemberFixture.GOOGLE);
                String token = authHelper.bearerTokenFor(otherMember);
                Gathering gathering =
                        gatheringHelper.save(GatheringFixture.FRIDAY_DINNER, host.getId());

                // when & then
                performExtendRecording(gathering.getId(), token)
                        .andExpect(status().isForbidden())
                        .andExpect(
                                jsonPath("$.data.code")
                                        .value(GatheringErrorCode.NOT_GATHERING_HOST.getCode()));
                assertThat(isRecordingExtended(gathering.getId())).isFalse();
            }
        }

        @Nested
        @DisplayName("인증 토큰이 없는 경우")
        class WhenUnauthenticated {

            @Test
            @DisplayName("401을 반환한다")
            void shouldReturnUnauthorized() throws Exception {
                // given
                Member member = memberHelper.save(MemberFixture.KAKAO);
                Gathering gathering =
                        gatheringHelper.save(GatheringFixture.FRIDAY_DINNER, member.getId());

                // when & then
                mockMvc.perform(
                                put(
                                        "/api/v1/gatherings/{gatheringId}/recording-extension",
                                        gathering.getId()))
                        .andExpect(status().isUnauthorized());
            }
        }

        private ResultActions performExtendRecording(Long gatheringId, String token)
                throws Exception {
            return mockMvc.perform(
                    put("/api/v1/gatherings/{gatheringId}/recording-extension", gatheringId)
                            .header(HttpHeaders.AUTHORIZATION, token));
        }

        private boolean isRecordingExtended(Long gatheringId) {
            return Boolean.TRUE.equals(
                    jdbcTemplate.queryForObject(
                            "SELECT recording_extended FROM gathering WHERE id = ?",
                            Boolean.class,
                            gatheringId));
        }
    }

    @Nested
    @DisplayName("POST /api/v1/gatherings/{gatheringId}/rounds는")
    class AddRound {

        @Nested
        @DisplayName("모임을 만든 회원이 마지막 차수의 다음 번호를 요청한 경우")
        class WhenNextRoundNumber {

            @Test
            @DisplayName("차수를 추가하고 그 차수의 id와 번호를 담아 201을 반환한다")
            void shouldAddRound() throws Exception {
                // given
                Member member = memberHelper.save(MemberFixture.KAKAO);
                String token = authHelper.bearerTokenFor(member);
                Gathering gathering =
                        gatheringHelper.save(GatheringFixture.FRIDAY_DINNER, member.getId());

                // when & then
                performAddRound(gathering.getId(), token, new AddRoundRequest(2))
                        .andExpect(status().isCreated())
                        .andExpect(jsonPath("$.data.id").isNumber())
                        .andExpect(jsonPath("$.data.number").value(2));
            }
        }

        @Nested
        @DisplayName("이미 있는 번호를 요청한 경우")
        class WhenRoundAlreadyExists {

            @Test
            @DisplayName("새로 만들지 않고 그 차수의 id와 번호를 담아 201을 반환한다")
            void shouldReturnExistingRound() throws Exception {
                // given
                Member member = memberHelper.save(MemberFixture.KAKAO);
                String token = authHelper.bearerTokenFor(member);
                Gathering gathering =
                        gatheringHelper.save(GatheringFixture.FRIDAY_DINNER, member.getId());

                // when & then
                performAddRound(gathering.getId(), token, new AddRoundRequest(1))
                        .andExpect(status().isCreated())
                        .andExpect(
                                jsonPath("$.data.id")
                                        .value(gathering.getRounds().getFirst().getId()))
                        .andExpect(jsonPath("$.data.number").value(1));
            }
        }

        @Nested
        @DisplayName("차수 번호가 0인 경우")
        class WhenRoundNumberNotPositive {

            @Test
            @DisplayName("roundNumber 필드 검증 실패와 함께 400을 반환한다")
            void shouldReturnBadRequest() throws Exception {
                // given
                Member member = memberHelper.save(MemberFixture.KAKAO);
                String token = authHelper.bearerTokenFor(member);
                Gathering gathering =
                        gatheringHelper.save(GatheringFixture.FRIDAY_DINNER, member.getId());

                // when & then
                performAddRound(gathering.getId(), token, new AddRoundRequest(0))
                        .andExpect(status().isBadRequest())
                        .andExpect(
                                jsonPath("$.data.code")
                                        .value(CommonErrorCode.METHOD_ARGUMENT_NOT_VALID.getCode()))
                        .andExpect(jsonPath("$.data.violations[0].field").value("roundNumber"));
            }
        }

        @Nested
        @DisplayName("마지막 차수의 다음 번호보다 큰 번호를 요청한 경우")
        class WhenRoundNumberSkipsRound {

            @Test
            @DisplayName("INVALID_GATHERING_ROUND_NUMBER 코드와 함께 400을 반환한다")
            void shouldReturnBadRequest() throws Exception {
                // given
                Member member = memberHelper.save(MemberFixture.KAKAO);
                String token = authHelper.bearerTokenFor(member);
                Gathering gathering =
                        gatheringHelper.save(GatheringFixture.FRIDAY_DINNER, member.getId());

                // when & then
                performAddRound(gathering.getId(), token, new AddRoundRequest(3))
                        .andExpect(status().isBadRequest())
                        .andExpect(
                                jsonPath("$.data.code")
                                        .value(
                                                GatheringErrorCode.INVALID_GATHERING_ROUND_NUMBER
                                                        .getCode()));
            }
        }

        @Nested
        @DisplayName("차수가 이미 상한까지 있는 경우")
        class WhenRoundCountAtLimit {

            @Test
            @DisplayName("GATHERING_ROUND_COUNT_EXCEEDS_LIMIT 코드와 함께 400을 반환한다")
            void shouldReturnBadRequest() throws Exception {
                // given
                Member member = memberHelper.save(MemberFixture.KAKAO);
                String token = authHelper.bearerTokenFor(member);
                Gathering gathering =
                        gatheringHelper.addRoundsUpTo(
                                gatheringHelper.save(
                                        GatheringFixture.FRIDAY_DINNER, member.getId()),
                                GatheringPolicy.MAX_ROUND_COUNT);
                AddRoundRequest request = new AddRoundRequest(GatheringPolicy.MAX_ROUND_COUNT + 1);

                // when & then
                performAddRound(gathering.getId(), token, request)
                        .andExpect(status().isBadRequest())
                        .andExpect(
                                jsonPath("$.data.code")
                                        .value(
                                                GatheringErrorCode
                                                        .GATHERING_ROUND_COUNT_EXCEEDS_LIMIT
                                                        .getCode()));
            }
        }

        @Nested
        @DisplayName("모임을 만든 지 48시간이 지난 경우")
        class WhenRecordingPeriodClosed {

            @Test
            @DisplayName("GATHERING_ROUND_CLOSED 코드와 함께 400을 반환한다")
            void shouldReturnBadRequest() throws Exception {
                // given
                Member member = memberHelper.save(MemberFixture.KAKAO);
                String token = authHelper.bearerTokenFor(member);
                Gathering gathering =
                        gatheringHelper.save(
                                GatheringFixture.FRIDAY_DINNER,
                                member.getId(),
                                LocalDateTime.now().minusHours(48));

                // when & then
                performAddRound(gathering.getId(), token, new AddRoundRequest(2))
                        .andExpect(status().isBadRequest())
                        .andExpect(
                                jsonPath("$.data.code")
                                        .value(
                                                GatheringErrorCode.GATHERING_ROUND_CLOSED
                                                        .getCode()));
            }
        }

        @Nested
        @DisplayName("모임을 만들지 않은 다른 회원의 요청인 경우")
        class WhenMemberIsNotHost {

            @Test
            @DisplayName("NOT_GATHERING_HOST 코드와 함께 403을 반환한다")
            void shouldReturnForbidden() throws Exception {
                // given
                Member host = memberHelper.save(MemberFixture.KAKAO);
                Member otherMember = memberHelper.save(MemberFixture.GOOGLE);
                String token = authHelper.bearerTokenFor(otherMember);
                Gathering gathering =
                        gatheringHelper.save(GatheringFixture.FRIDAY_DINNER, host.getId());

                // when & then
                performAddRound(gathering.getId(), token, new AddRoundRequest(2))
                        .andExpect(status().isForbidden())
                        .andExpect(
                                jsonPath("$.data.code")
                                        .value(GatheringErrorCode.NOT_GATHERING_HOST.getCode()));
            }
        }

        @Nested
        @DisplayName("존재하지 않는 모임인 경우")
        class WhenGatheringNotFound {

            @Test
            @DisplayName("GATHERING_NOT_FOUND 코드와 함께 404를 반환한다")
            void shouldReturnNotFound() throws Exception {
                // given
                Member member = memberHelper.save(MemberFixture.KAKAO);
                String token = authHelper.bearerTokenFor(member);

                // when & then
                performAddRound(Long.MAX_VALUE, token, new AddRoundRequest(2))
                        .andExpect(status().isNotFound())
                        .andExpect(
                                jsonPath("$.data.code")
                                        .value(GatheringErrorCode.GATHERING_NOT_FOUND.getCode()));
            }
        }

        @Nested
        @DisplayName("인증 토큰이 없는 경우")
        class WhenUnauthenticated {

            @Test
            @DisplayName("401을 반환한다")
            void shouldReturnUnauthorized() throws Exception {
                // given
                Member member = memberHelper.save(MemberFixture.KAKAO);
                Gathering gathering =
                        gatheringHelper.save(GatheringFixture.FRIDAY_DINNER, member.getId());

                // when & then
                mockMvc.perform(
                                post("/api/v1/gatherings/{gatheringId}/rounds", gathering.getId())
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(
                                                objectMapper.writeValueAsString(
                                                        new AddRoundRequest(2))))
                        .andExpect(status().isUnauthorized());
            }
        }

        private ResultActions performAddRound(
                Long gatheringId, String token, AddRoundRequest request) throws Exception {
            return mockMvc.perform(
                    post("/api/v1/gatherings/{gatheringId}/rounds", gatheringId)
                            .header(HttpHeaders.AUTHORIZATION, token)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)));
        }
    }

    @Nested
    @DisplayName("GET /api/v1/gatherings/{gatheringId}/settlements/form은")
    class GetSettlementForm {

        @BeforeEach
        void setUp() {
            givenSettlementGathering();
        }

        @Nested
        @DisplayName("모임을 만든 회원의 요청인 경우")
        class WhenMemberIsHost {

            @Test
            @DisplayName("마스킹한 휴대폰을 담은 참여자, 인원 기본값, 결제자로 기록한 정산 건을 담아 200을 반환한다")
            void shouldReturnSettlementForm() throws Exception {
                // given
                Settlement settlement =
                        settlementHelper.record(SettlementFixture.DINNER, gathering, payer);

                // when & then
                performGetForm(gathering.getId(), hostToken)
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.data.recordable").value(true))
                        .andExpect(jsonPath("$.data.deletesAt").isNotEmpty())
                        .andExpect(jsonPath("$.data.participants.length()").value(3))
                        .andExpect(
                                jsonPath("$.data.participants[0].name")
                                        .value(ParticipantFixture.KIM.getName()))
                        .andExpect(
                                jsonPath("$.data.participants[0].maskedPhoneNumber")
                                        .value(MASKED_KIM_PHONE_NUMBER))
                        .andExpect(jsonPath("$.data.participants[0].phoneNumber").doesNotExist())
                        .andExpect(
                                jsonPath("$.data.defaultParticipantIds")
                                        .value(
                                                contains(
                                                        payer.getId().intValue(),
                                                        lee.getId().intValue(),
                                                        park.getId().intValue())))
                        .andExpect(jsonPath("$.data.rounds.length()").value(1))
                        .andExpect(jsonPath("$.data.rounds[0].id").value(roundId()))
                        .andExpect(jsonPath("$.data.rounds[0].number").value(1))
                        .andExpect(jsonPath("$.data.settlements[0].id").value(settlement.getId()))
                        .andExpect(jsonPath("$.data.settlements[0].roundId").value(roundId()))
                        .andExpect(jsonPath("$.data.settlements[0].roundNumber").value(1))
                        .andExpect(jsonPath("$.data.settlements[0].payerAmount").value(3_334))
                        .andExpect(jsonPath("$.data.settlements[0].receivableAmount").value(6_666))
                        .andExpect(jsonPath("$.data.totalAmountSum").value(10_000))
                        .andExpect(jsonPath("$.data.receivableAmountSum").value(6_666));
            }

            @Test
            @DisplayName("결제자가 다른 취소하지 않은 정산 건을 모두 담고 합계를 더해 200을 반환한다")
            void shouldReturnAllUncancelledSettlementsOfGathering() throws Exception {
                // given
                Settlement dinner =
                        settlementHelper.record(SettlementFixture.DINNER, gathering, payer);
                Settlement secondRound =
                        settlementHelper.record(SettlementFixture.SECOND_ROUND, gathering, lee);
                settlementHelper.cancel(
                        settlementHelper.record(SettlementFixture.DINNER, gathering, park));

                // when & then
                performGetForm(gathering.getId(), hostToken)
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.data.settlements.length()").value(2))
                        .andExpect(jsonPath("$.data.settlements[0].id").value(dinner.getId()))
                        .andExpect(
                                jsonPath("$.data.settlements[0].payerParticipantId")
                                        .value(payer.getId()))
                        .andExpect(jsonPath("$.data.settlements[1].id").value(secondRound.getId()))
                        .andExpect(
                                jsonPath("$.data.settlements[1].payerParticipantId")
                                        .value(lee.getId()))
                        .andExpect(jsonPath("$.data.totalAmountSum").value(30_000))
                        .andExpect(jsonPath("$.data.receivableAmountSum").value(19_998));
            }
        }

        @Nested
        @DisplayName("모임을 만들지 않은 다른 회원의 요청인 경우")
        class WhenMemberIsNotHost {

            @Test
            @DisplayName("NOT_GATHERING_HOST 코드와 함께 403을 반환한다")
            void shouldReturnForbidden() throws Exception {
                // given
                String otherToken = otherMemberToken();

                // when & then
                performGetForm(gathering.getId(), otherToken)
                        .andExpect(status().isForbidden())
                        .andExpect(
                                jsonPath("$.data.code")
                                        .value(GatheringErrorCode.NOT_GATHERING_HOST.getCode()));
            }
        }

        @Nested
        @DisplayName("존재하지 않는 모임인 경우")
        class WhenGatheringNotFound {

            @Test
            @DisplayName("GATHERING_NOT_FOUND 코드와 함께 404를 반환한다")
            void shouldReturnNotFound() throws Exception {
                // when & then
                performGetForm(Long.MAX_VALUE, hostToken)
                        .andExpect(status().isNotFound())
                        .andExpect(
                                jsonPath("$.data.code")
                                        .value(GatheringErrorCode.GATHERING_NOT_FOUND.getCode()));
            }
        }

        @Nested
        @DisplayName("인증 토큰이 없는 경우")
        class WhenUnauthenticated {

            @Test
            @DisplayName("401을 반환한다")
            void shouldReturnUnauthorized() throws Exception {
                // when & then
                mockMvc.perform(
                                get(
                                        "/api/v1/gatherings/{gatheringId}/settlements/form",
                                        gathering.getId()))
                        .andExpect(status().isUnauthorized());
            }
        }

        private ResultActions performGetForm(Long gatheringId, String token) throws Exception {
            return mockMvc.perform(
                    get("/api/v1/gatherings/{gatheringId}/settlements/form", gatheringId)
                            .header(HttpHeaders.AUTHORIZATION, token));
        }
    }

    @Nested
    @DisplayName("POST /api/v1/gatherings/{gatheringId}/settlements는")
    class RecordSettlement {

        @BeforeEach
        void setUp() {
            givenSettlementGathering();
        }

        @Nested
        @DisplayName("기록할 수 있는 요청인 경우")
        class WhenRecordable {

            @Test
            @DisplayName("지정한 결제자의 정산 건을 기록하고 분담금을 담아 201을 반환한다")
            void shouldRecordSettlement() throws Exception {
                // given
                RecordSettlementRequest request =
                        new RecordSettlementRequest(
                                CLIENT_REQUEST_ID,
                                payer.getId(),
                                roundId(),
                                10_000L,
                                participantIds(),
                                List.of(new FixedAmountRequest(lee.getId(), 1_050L)));

                // when & then
                performRecord(request)
                        .andExpect(status().isCreated())
                        .andExpect(jsonPath("$.data.id").isNumber())
                        .andExpect(jsonPath("$.data.roundId").value(roundId()))
                        .andExpect(jsonPath("$.data.roundNumber").value(1))
                        .andExpect(jsonPath("$.data.payerParticipantId").value(payer.getId()))
                        .andExpect(jsonPath("$.data.totalAmount").value(10_000))
                        .andExpect(jsonPath("$.data.payerAmount").value(4_475))
                        .andExpect(jsonPath("$.data.receivableAmount").value(5_525))
                        .andExpect(jsonPath("$.data.shares[0].participantId").value(lee.getId()))
                        .andExpect(jsonPath("$.data.shares[0].amount").value(1_050))
                        .andExpect(jsonPath("$.data.shares[0].fixed").value(true))
                        .andExpect(jsonPath("$.data.shares[0].status").value("UNPAID"))
                        .andExpect(jsonPath("$.data.shares[1].participantId").value(park.getId()))
                        .andExpect(jsonPath("$.data.shares[1].amount").value(4_475))
                        .andExpect(jsonPath("$.data.shares[1].fixed").value(false));
            }

            @Test
            @DisplayName("모임장이 참여자 목록에 없어도 다른 참여자를 결제자로 기록하고 201을 반환한다")
            void shouldRecordSettlementWhenHostIsNotParticipant() throws Exception {
                // given
                RecordSettlementRequest request =
                        new RecordSettlementRequest(
                                CLIENT_REQUEST_ID,
                                lee.getId(),
                                roundId(),
                                10_000L,
                                List.of(lee.getId(), park.getId()),
                                List.of());

                // when & then
                performRecord(request)
                        .andExpect(status().isCreated())
                        .andExpect(jsonPath("$.data.payerParticipantId").value(lee.getId()))
                        .andExpect(jsonPath("$.data.payerAmount").value(5_000))
                        .andExpect(jsonPath("$.data.shares.length()").value(1))
                        .andExpect(jsonPath("$.data.shares[0].participantId").value(park.getId()))
                        .andExpect(jsonPath("$.data.shares[0].amount").value(5_000));
            }
        }

        @Nested
        @DisplayName("요청 id가 없는 경우")
        class WhenClientRequestIdMissing {

            @Test
            @DisplayName("clientRequestId 필드 검증 실패와 함께 400을 반환한다")
            void shouldReturnBadRequest() throws Exception {
                // given
                RecordSettlementRequest request =
                        new RecordSettlementRequest(
                                null,
                                payer.getId(),
                                roundId(),
                                10_000L,
                                participantIds(),
                                List.of());

                // when & then
                performRecord(request)
                        .andExpect(status().isBadRequest())
                        .andExpect(
                                jsonPath("$.data.code")
                                        .value(CommonErrorCode.METHOD_ARGUMENT_NOT_VALID.getCode()))
                        .andExpect(jsonPath("$.data.violations[0].field").value("clientRequestId"));
            }
        }

        @Nested
        @DisplayName("결제자 id가 없는 경우")
        class WhenPayerParticipantIdMissing {

            @Test
            @DisplayName("payerParticipantId 필드 검증 실패와 함께 400을 반환한다")
            void shouldReturnBadRequest() throws Exception {
                // given
                RecordSettlementRequest request =
                        new RecordSettlementRequest(
                                CLIENT_REQUEST_ID,
                                null,
                                roundId(),
                                10_000L,
                                participantIds(),
                                List.of());

                // when & then
                performRecord(request)
                        .andExpect(status().isBadRequest())
                        .andExpect(
                                jsonPath("$.data.code")
                                        .value(CommonErrorCode.METHOD_ARGUMENT_NOT_VALID.getCode()))
                        .andExpect(
                                jsonPath("$.data.violations[0].field").value("payerParticipantId"));
            }
        }

        @Nested
        @DisplayName("같은 요청 id로 이미 기록한 경우")
        class WhenSameClientRequestIdRecorded {

            @Test
            @DisplayName("정산 건을 새로 만들지 않고 처음 기록한 정산 건을 담아 201을 반환한다")
            void shouldReturnRecordedSettlement() throws Exception {
                // given
                Settlement recorded =
                        settlementHelper.record(SettlementFixture.DINNER, gathering, payer);
                RecordSettlementRequest request =
                        new RecordSettlementRequest(
                                recorded.getClientRequestId(),
                                payer.getId(),
                                roundId(),
                                20_000L,
                                participantIds(),
                                List.of());

                // when & then
                performRecord(request)
                        .andExpect(status().isCreated())
                        .andExpect(jsonPath("$.data.id").value(recorded.getId()))
                        .andExpect(
                                jsonPath("$.data.totalAmount")
                                        .value(SettlementFixture.DINNER.getTotalAmount()));
            }
        }

        @Nested
        @DisplayName("차수 id가 없는 경우")
        class WhenRoundIdMissing {

            @Test
            @DisplayName("roundId 필드 검증 실패와 함께 400을 반환한다")
            void shouldReturnBadRequest() throws Exception {
                // given
                RecordSettlementRequest request =
                        new RecordSettlementRequest(
                                CLIENT_REQUEST_ID,
                                payer.getId(),
                                null,
                                10_000L,
                                participantIds(),
                                List.of());

                // when & then
                performRecord(request)
                        .andExpect(status().isBadRequest())
                        .andExpect(
                                jsonPath("$.data.code")
                                        .value(CommonErrorCode.METHOD_ARGUMENT_NOT_VALID.getCode()))
                        .andExpect(jsonPath("$.data.violations[0].field").value("roundId"));
            }
        }

        @Nested
        @DisplayName("다른 모임의 차수 id인 경우")
        class WhenRoundOfOtherGathering {

            @Test
            @DisplayName("GATHERING_ROUND_NOT_FOUND 코드와 함께 404를 반환한다")
            void shouldReturnNotFound() throws Exception {
                // given
                RecordSettlementRequest request =
                        new RecordSettlementRequest(
                                CLIENT_REQUEST_ID,
                                payer.getId(),
                                otherGatheringRoundId(),
                                10_000L,
                                participantIds(),
                                List.of());

                // when & then
                performRecord(request)
                        .andExpect(status().isNotFound())
                        .andExpect(
                                jsonPath("$.data.code")
                                        .value(
                                                GatheringErrorCode.GATHERING_ROUND_NOT_FOUND
                                                        .getCode()));
            }
        }

        @Nested
        @DisplayName("결제자가 모임 참여자가 아닌 경우")
        class WhenPayerNotParticipantOfGathering {

            @Test
            @DisplayName("PARTICIPANT_NOT_FOUND 코드와 함께 404를 반환한다")
            void shouldReturnNotFound() throws Exception {
                // given
                Participant otherGatheringParticipant =
                        saveOtherGathering().getParticipants().getFirst();
                RecordSettlementRequest request =
                        new RecordSettlementRequest(
                                CLIENT_REQUEST_ID,
                                otherGatheringParticipant.getId(),
                                roundId(),
                                10_000L,
                                participantIds(),
                                List.of());

                // when & then
                performRecord(request)
                        .andExpect(status().isNotFound())
                        .andExpect(
                                jsonPath("$.data.code")
                                        .value(GatheringErrorCode.PARTICIPANT_NOT_FOUND.getCode()));
            }
        }

        @Nested
        @DisplayName("총액이 0인 경우")
        class WhenTotalAmountNotPositive {

            @Test
            @DisplayName("totalAmount 필드 검증 실패와 함께 400을 반환한다")
            void shouldReturnBadRequest() throws Exception {
                // given
                RecordSettlementRequest request =
                        new RecordSettlementRequest(
                                CLIENT_REQUEST_ID,
                                payer.getId(),
                                roundId(),
                                0L,
                                participantIds(),
                                List.of());

                // when & then
                performRecord(request)
                        .andExpect(status().isBadRequest())
                        .andExpect(
                                jsonPath("$.data.code")
                                        .value(CommonErrorCode.METHOD_ARGUMENT_NOT_VALID.getCode()))
                        .andExpect(jsonPath("$.data.violations[0].field").value("totalAmount"));
            }
        }

        @Nested
        @DisplayName("총액이 1억 원을 넘는 경우")
        class WhenTotalAmountExceedsLimit {

            @Test
            @DisplayName("totalAmount 필드 검증 실패와 함께 400을 반환한다")
            void shouldReturnBadRequest() throws Exception {
                // given
                RecordSettlementRequest request =
                        new RecordSettlementRequest(
                                CLIENT_REQUEST_ID,
                                payer.getId(),
                                roundId(),
                                100_000_001L,
                                participantIds(),
                                List.of());

                // when & then
                performRecord(request)
                        .andExpect(status().isBadRequest())
                        .andExpect(
                                jsonPath("$.data.code")
                                        .value(CommonErrorCode.METHOD_ARGUMENT_NOT_VALID.getCode()))
                        .andExpect(jsonPath("$.data.violations[0].field").value("totalAmount"));
            }
        }

        @Nested
        @DisplayName("지정 금액이 음수인 경우")
        class WhenFixedAmountNegative {

            @Test
            @DisplayName("지정 금액 필드 검증 실패와 함께 400을 반환한다")
            void shouldReturnBadRequest() throws Exception {
                // given
                RecordSettlementRequest request =
                        new RecordSettlementRequest(
                                CLIENT_REQUEST_ID,
                                payer.getId(),
                                roundId(),
                                10_000L,
                                participantIds(),
                                List.of(new FixedAmountRequest(lee.getId(), -1L)));

                // when & then
                performRecord(request)
                        .andExpect(status().isBadRequest())
                        .andExpect(
                                jsonPath("$.data.code")
                                        .value(CommonErrorCode.METHOD_ARGUMENT_NOT_VALID.getCode()))
                        .andExpect(
                                jsonPath("$.data.violations[0].field")
                                        .value("fixedAmounts[0].amount"));
            }
        }

        @Nested
        @DisplayName("지정 금액이 1억 원을 넘는 경우")
        class WhenFixedAmountExceedsLimit {

            @Test
            @DisplayName("지정 금액 필드 검증 실패와 함께 400을 반환한다")
            void shouldReturnBadRequest() throws Exception {
                // given
                RecordSettlementRequest request =
                        new RecordSettlementRequest(
                                CLIENT_REQUEST_ID,
                                payer.getId(),
                                roundId(),
                                10_000L,
                                participantIds(),
                                List.of(new FixedAmountRequest(lee.getId(), 100_000_001L)));

                // when & then
                performRecord(request)
                        .andExpect(status().isBadRequest())
                        .andExpect(
                                jsonPath("$.data.code")
                                        .value(CommonErrorCode.METHOD_ARGUMENT_NOT_VALID.getCode()))
                        .andExpect(
                                jsonPath("$.data.violations[0].field")
                                        .value("fixedAmounts[0].amount"));
            }
        }

        @Nested
        @DisplayName("같은 참여자의 금액을 두 번 지정한 경우")
        class WhenFixedAmountParticipantDuplicated {

            @Test
            @DisplayName("지정 대상 중복 검증 실패와 함께 400을 반환한다")
            void shouldReturnBadRequest() throws Exception {
                // given
                RecordSettlementRequest request =
                        new RecordSettlementRequest(
                                CLIENT_REQUEST_ID,
                                payer.getId(),
                                roundId(),
                                10_000L,
                                participantIds(),
                                List.of(
                                        new FixedAmountRequest(lee.getId(), 1_000L),
                                        new FixedAmountRequest(lee.getId(), 2_000L)));

                // when & then
                performRecord(request)
                        .andExpect(status().isBadRequest())
                        .andExpect(
                                jsonPath("$.data.code")
                                        .value(CommonErrorCode.METHOD_ARGUMENT_NOT_VALID.getCode()))
                        .andExpect(
                                jsonPath("$.data.violations[0].field")
                                        .value("fixedAmountParticipantsDistinct"));
            }
        }

        @Nested
        @DisplayName("인원이 비어 있는 경우")
        class WhenParticipantIdsEmpty {

            @Test
            @DisplayName("participantIds 필드 검증 실패와 함께 400을 반환한다")
            void shouldReturnBadRequest() throws Exception {
                // given
                RecordSettlementRequest request =
                        new RecordSettlementRequest(
                                CLIENT_REQUEST_ID,
                                payer.getId(),
                                roundId(),
                                10_000L,
                                List.of(),
                                List.of());

                // when & then
                performRecord(request)
                        .andExpect(status().isBadRequest())
                        .andExpect(
                                jsonPath("$.data.code")
                                        .value(CommonErrorCode.METHOD_ARGUMENT_NOT_VALID.getCode()))
                        .andExpect(jsonPath("$.data.violations[0].field").value("participantIds"));
            }
        }

        @Nested
        @DisplayName("모임을 만들지 않은 다른 회원의 요청인 경우")
        class WhenMemberIsNotHost {

            @Test
            @DisplayName("NOT_GATHERING_HOST 코드와 함께 403을 반환한다")
            void shouldReturnForbidden() throws Exception {
                // given
                String otherToken = otherMemberToken();

                // when & then
                performRecord(gathering.getId(), otherToken, validRecordRequest())
                        .andExpect(status().isForbidden())
                        .andExpect(
                                jsonPath("$.data.code")
                                        .value(GatheringErrorCode.NOT_GATHERING_HOST.getCode()));
            }
        }

        @Nested
        @DisplayName("존재하지 않는 모임인 경우")
        class WhenGatheringNotFound {

            @Test
            @DisplayName("GATHERING_NOT_FOUND 코드와 함께 404를 반환한다")
            void shouldReturnNotFound() throws Exception {
                // when & then
                performRecord(Long.MAX_VALUE, hostToken, validRecordRequest())
                        .andExpect(status().isNotFound())
                        .andExpect(
                                jsonPath("$.data.code")
                                        .value(GatheringErrorCode.GATHERING_NOT_FOUND.getCode()));
            }
        }

        @Nested
        @DisplayName("인증 토큰이 없는 경우")
        class WhenUnauthenticated {

            @Test
            @DisplayName("401을 반환한다")
            void shouldReturnUnauthorized() throws Exception {
                // when & then
                mockMvc.perform(
                                post(
                                                "/api/v1/gatherings/{gatheringId}/settlements",
                                                gathering.getId())
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(
                                                objectMapper.writeValueAsString(
                                                        validRecordRequest())))
                        .andExpect(status().isUnauthorized());
            }
        }

        private RecordSettlementRequest validRecordRequest() {
            return new RecordSettlementRequest(
                    CLIENT_REQUEST_ID,
                    payer.getId(),
                    roundId(),
                    10_000L,
                    participantIds(),
                    List.of());
        }

        private ResultActions performRecord(RecordSettlementRequest request) throws Exception {
            return performRecord(gathering.getId(), hostToken, request);
        }

        private ResultActions performRecord(
                Long gatheringId, String token, RecordSettlementRequest request) throws Exception {
            return mockMvc.perform(
                    post("/api/v1/gatherings/{gatheringId}/settlements", gatheringId)
                            .header(HttpHeaders.AUTHORIZATION, token)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)));
        }
    }

    @Nested
    @DisplayName("PUT /api/v1/gatherings/{gatheringId}/settlements/{settlementId}는")
    class UpdateSettlement {

        @BeforeEach
        void setUp() {
            givenSettlementGathering();
        }

        @Nested
        @DisplayName("모임장이 수정할 수 있는 정산 건인 경우")
        class WhenUpdatable {

            @Test
            @DisplayName("결제자는 그대로 두고 분담금을 다시 계산한 정산 건을 담아 200을 반환한다")
            void shouldUpdateSettlement() throws Exception {
                // given
                Settlement settlement =
                        settlementHelper.record(SettlementFixture.DINNER, gathering, payer);
                UpdateSettlementRequest request = updateRequest(20_000L);

                // when & then
                performUpdate(settlement, request)
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.data.id").value(settlement.getId()))
                        .andExpect(jsonPath("$.data.roundId").value(roundId()))
                        .andExpect(jsonPath("$.data.roundNumber").value(1))
                        .andExpect(jsonPath("$.data.payerParticipantId").value(payer.getId()))
                        .andExpect(jsonPath("$.data.totalAmount").value(20_000))
                        .andExpect(jsonPath("$.data.payerAmount").value(10_000))
                        .andExpect(jsonPath("$.data.shares.length()").value(1))
                        .andExpect(jsonPath("$.data.shares[0].participantId").value(lee.getId()))
                        .andExpect(jsonPath("$.data.shares[0].amount").value(10_000));
            }
        }

        @Nested
        @DisplayName("다른 차수로 바꾸는 요청인 경우")
        class WhenRoundChanged {

            @Test
            @DisplayName("정산 건을 새 차수로 옮기고 그 차수의 id와 번호를 담아 200을 반환한다")
            void shouldMoveSettlementToRound() throws Exception {
                // given
                Settlement settlement =
                        settlementHelper.record(SettlementFixture.DINNER, gathering, payer);
                Gathering withSecondRound = gatheringHelper.addRoundsUpTo(gathering, 2);
                Long secondRoundId = withSecondRound.getRounds().getLast().getId();
                UpdateSettlementRequest request = updateRequest(secondRoundId, 20_000L);

                // when & then
                performUpdate(settlement, request)
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.data.roundId").value(secondRoundId))
                        .andExpect(jsonPath("$.data.roundNumber").value(2));
            }
        }

        @Nested
        @DisplayName("차수 id가 없는 경우")
        class WhenRoundIdMissing {

            @Test
            @DisplayName("roundId 필드 검증 실패와 함께 400을 반환한다")
            void shouldReturnBadRequest() throws Exception {
                // given
                Settlement settlement =
                        settlementHelper.record(SettlementFixture.DINNER, gathering, payer);
                UpdateSettlementRequest request = updateRequest(null, 20_000L);

                // when & then
                performUpdate(settlement, request)
                        .andExpect(status().isBadRequest())
                        .andExpect(
                                jsonPath("$.data.code")
                                        .value(CommonErrorCode.METHOD_ARGUMENT_NOT_VALID.getCode()))
                        .andExpect(jsonPath("$.data.violations[0].field").value("roundId"));
            }
        }

        @Nested
        @DisplayName("다른 모임의 차수 id인 경우")
        class WhenRoundOfOtherGathering {

            @Test
            @DisplayName("GATHERING_ROUND_NOT_FOUND 코드와 함께 404를 반환한다")
            void shouldReturnNotFound() throws Exception {
                // given
                Settlement settlement =
                        settlementHelper.record(SettlementFixture.DINNER, gathering, payer);
                UpdateSettlementRequest request = updateRequest(otherGatheringRoundId(), 20_000L);

                // when & then
                performUpdate(settlement, request)
                        .andExpect(status().isNotFound())
                        .andExpect(
                                jsonPath("$.data.code")
                                        .value(
                                                GatheringErrorCode.GATHERING_ROUND_NOT_FOUND
                                                        .getCode()));
            }
        }

        @Nested
        @DisplayName("총액이 0인 경우")
        class WhenTotalAmountNotPositive {

            @Test
            @DisplayName("totalAmount 필드 검증 실패와 함께 400을 반환한다")
            void shouldReturnBadRequest() throws Exception {
                // given
                Settlement settlement =
                        settlementHelper.record(SettlementFixture.DINNER, gathering, payer);
                UpdateSettlementRequest request = updateRequest(0L);

                // when & then
                performUpdate(settlement, request)
                        .andExpect(status().isBadRequest())
                        .andExpect(
                                jsonPath("$.data.code")
                                        .value(CommonErrorCode.METHOD_ARGUMENT_NOT_VALID.getCode()))
                        .andExpect(jsonPath("$.data.violations[0].field").value("totalAmount"));
            }
        }

        @Nested
        @DisplayName("보낸 분담금이 있는 정산 건인 경우")
        class WhenShareSent {

            @Test
            @DisplayName("SETTLEMENT_SHARES_SENT 코드와 함께 409를 반환한다")
            void shouldReturnConflict() throws Exception {
                // given
                Settlement settlement =
                        settlementHelper.markSent(
                                settlementHelper.record(SettlementFixture.DINNER, gathering, payer),
                                lee);
                UpdateSettlementRequest request = updateRequest(20_000L);

                // when & then
                performUpdate(settlement, request)
                        .andExpect(status().isConflict())
                        .andExpect(
                                jsonPath("$.data.code")
                                        .value(
                                                GatheringErrorCode.SETTLEMENT_SHARES_SENT
                                                        .getCode()));
            }
        }

        @Nested
        @DisplayName("다른 모임의 정산 건인 경우")
        class WhenSettlementOfOtherGathering {

            @Test
            @DisplayName("SETTLEMENT_NOT_FOUND 코드와 함께 404를 반환한다")
            void shouldReturnNotFound() throws Exception {
                // given
                Gathering otherGathering = saveOtherGathering();
                Settlement otherSettlement =
                        settlementHelper.record(
                                SettlementFixture.DINNER,
                                otherGathering,
                                otherGathering.getParticipants().getFirst());

                // when & then
                performUpdate(otherSettlement, updateRequest(20_000L))
                        .andExpect(status().isNotFound())
                        .andExpect(
                                jsonPath("$.data.code")
                                        .value(GatheringErrorCode.SETTLEMENT_NOT_FOUND.getCode()));
            }
        }

        @Nested
        @DisplayName("모임을 만들지 않은 다른 회원의 요청인 경우")
        class WhenMemberIsNotHost {

            @Test
            @DisplayName("NOT_GATHERING_HOST 코드와 함께 403을 반환한다")
            void shouldReturnForbidden() throws Exception {
                // given
                Settlement settlement =
                        settlementHelper.record(SettlementFixture.DINNER, gathering, payer);
                String otherToken = otherMemberToken();

                // when & then
                performUpdate(
                                gathering.getId(),
                                settlement.getId(),
                                otherToken,
                                updateRequest(20_000L))
                        .andExpect(status().isForbidden())
                        .andExpect(
                                jsonPath("$.data.code")
                                        .value(GatheringErrorCode.NOT_GATHERING_HOST.getCode()));
            }
        }

        @Nested
        @DisplayName("존재하지 않는 모임인 경우")
        class WhenGatheringNotFound {

            @Test
            @DisplayName("GATHERING_NOT_FOUND 코드와 함께 404를 반환한다")
            void shouldReturnNotFound() throws Exception {
                // given
                Settlement settlement =
                        settlementHelper.record(SettlementFixture.DINNER, gathering, payer);

                // when & then
                performUpdate(Long.MAX_VALUE, settlement.getId(), hostToken, updateRequest(20_000L))
                        .andExpect(status().isNotFound())
                        .andExpect(
                                jsonPath("$.data.code")
                                        .value(GatheringErrorCode.GATHERING_NOT_FOUND.getCode()));
            }
        }

        @Nested
        @DisplayName("인증 토큰이 없는 경우")
        class WhenUnauthenticated {

            @Test
            @DisplayName("401을 반환한다")
            void shouldReturnUnauthorized() throws Exception {
                // given
                Settlement settlement =
                        settlementHelper.record(SettlementFixture.DINNER, gathering, payer);

                // when & then
                mockMvc.perform(
                                put(
                                                "/api/v1/gatherings/{gatheringId}/settlements/{settlementId}",
                                                gathering.getId(),
                                                settlement.getId())
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(
                                                objectMapper.writeValueAsString(
                                                        updateRequest(20_000L))))
                        .andExpect(status().isUnauthorized());
            }
        }

        private UpdateSettlementRequest updateRequest(long totalAmount) {
            return updateRequest(roundId(), totalAmount);
        }

        private UpdateSettlementRequest updateRequest(Long roundId, long totalAmount) {
            return new UpdateSettlementRequest(
                    roundId, totalAmount, List.of(payer.getId(), lee.getId()), List.of());
        }

        private ResultActions performUpdate(Settlement settlement, UpdateSettlementRequest request)
                throws Exception {
            return performUpdate(gathering.getId(), settlement.getId(), hostToken, request);
        }

        private ResultActions performUpdate(
                Long gatheringId, Long settlementId, String token, UpdateSettlementRequest request)
                throws Exception {
            return mockMvc.perform(
                    put(
                                    "/api/v1/gatherings/{gatheringId}/settlements/{settlementId}",
                                    gatheringId,
                                    settlementId)
                            .header(HttpHeaders.AUTHORIZATION, token)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)));
        }
    }

    @Nested
    @DisplayName("POST /api/v1/gatherings/{gatheringId}/settlements/{settlementId}/cancel은")
    class CancelSettlement {

        @BeforeEach
        void setUp() {
            givenSettlementGathering();
        }

        @Nested
        @DisplayName("모임장이 취소할 수 있는 정산 건인 경우")
        class WhenCancellable {

            @Test
            @DisplayName("정산 건을 취소하고 200을 반환한다")
            void shouldCancelSettlement() throws Exception {
                // given
                Settlement settlement =
                        settlementHelper.record(SettlementFixture.DINNER, gathering, payer);

                // when & then
                performCancel(gathering.getId(), settlement.getId(), hostToken)
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.ok").value(true));
            }
        }

        @Nested
        @DisplayName("이미 취소한 정산 건인 경우")
        class WhenSettlementCancelled {

            @Test
            @DisplayName("같은 요청을 다시 보낸 것으로 보고 200을 반환한다")
            void shouldReturnOk() throws Exception {
                // given
                Settlement settlement =
                        settlementHelper.cancel(
                                settlementHelper.record(
                                        SettlementFixture.DINNER, gathering, payer));

                // when & then
                performCancel(gathering.getId(), settlement.getId(), hostToken)
                        .andExpect(status().isOk());
            }
        }

        @Nested
        @DisplayName("보낸 분담금이 있는 정산 건인 경우")
        class WhenShareSent {

            @Test
            @DisplayName("SETTLEMENT_SHARES_SENT 코드와 함께 409를 반환한다")
            void shouldReturnConflict() throws Exception {
                // given
                Settlement settlement =
                        settlementHelper.markSent(
                                settlementHelper.record(SettlementFixture.DINNER, gathering, payer),
                                lee);

                // when & then
                performCancel(gathering.getId(), settlement.getId(), hostToken)
                        .andExpect(status().isConflict())
                        .andExpect(
                                jsonPath("$.data.code")
                                        .value(
                                                GatheringErrorCode.SETTLEMENT_SHARES_SENT
                                                        .getCode()));
            }
        }

        @Nested
        @DisplayName("다른 모임의 정산 건인 경우")
        class WhenSettlementOfOtherGathering {

            @Test
            @DisplayName("SETTLEMENT_NOT_FOUND 코드와 함께 404를 반환한다")
            void shouldReturnNotFound() throws Exception {
                // given
                Gathering otherGathering = saveOtherGathering();
                Settlement otherSettlement =
                        settlementHelper.record(
                                SettlementFixture.DINNER,
                                otherGathering,
                                otherGathering.getParticipants().getFirst());

                // when & then
                performCancel(gathering.getId(), otherSettlement.getId(), hostToken)
                        .andExpect(status().isNotFound())
                        .andExpect(
                                jsonPath("$.data.code")
                                        .value(GatheringErrorCode.SETTLEMENT_NOT_FOUND.getCode()));
            }
        }

        @Nested
        @DisplayName("모임을 만들지 않은 다른 회원의 요청인 경우")
        class WhenMemberIsNotHost {

            @Test
            @DisplayName("NOT_GATHERING_HOST 코드와 함께 403을 반환한다")
            void shouldReturnForbidden() throws Exception {
                // given
                Settlement settlement =
                        settlementHelper.record(SettlementFixture.DINNER, gathering, payer);
                String otherToken = otherMemberToken();

                // when & then
                performCancel(gathering.getId(), settlement.getId(), otherToken)
                        .andExpect(status().isForbidden())
                        .andExpect(
                                jsonPath("$.data.code")
                                        .value(GatheringErrorCode.NOT_GATHERING_HOST.getCode()));
            }
        }

        @Nested
        @DisplayName("존재하지 않는 모임인 경우")
        class WhenGatheringNotFound {

            @Test
            @DisplayName("GATHERING_NOT_FOUND 코드와 함께 404를 반환한다")
            void shouldReturnNotFound() throws Exception {
                // given
                Settlement settlement =
                        settlementHelper.record(SettlementFixture.DINNER, gathering, payer);

                // when & then
                performCancel(Long.MAX_VALUE, settlement.getId(), hostToken)
                        .andExpect(status().isNotFound())
                        .andExpect(
                                jsonPath("$.data.code")
                                        .value(GatheringErrorCode.GATHERING_NOT_FOUND.getCode()));
            }
        }

        @Nested
        @DisplayName("인증 토큰이 없는 경우")
        class WhenUnauthenticated {

            @Test
            @DisplayName("401을 반환한다")
            void shouldReturnUnauthorized() throws Exception {
                // given
                Settlement settlement =
                        settlementHelper.record(SettlementFixture.DINNER, gathering, payer);

                // when & then
                mockMvc.perform(
                                post(
                                        "/api/v1/gatherings/{gatheringId}/settlements/{settlementId}/cancel",
                                        gathering.getId(),
                                        settlement.getId()))
                        .andExpect(status().isUnauthorized());
            }
        }

        private ResultActions performCancel(Long gatheringId, Long settlementId, String token)
                throws Exception {
            return mockMvc.perform(
                    post(
                                    "/api/v1/gatherings/{gatheringId}/settlements/{settlementId}/cancel",
                                    gatheringId,
                                    settlementId)
                            .header(HttpHeaders.AUTHORIZATION, token));
        }
    }

    @Nested
    @DisplayName("GET /api/v1/gatherings/preview는")
    class Preview {

        @Test
        @DisplayName("인증 없이도 모임 이름, 참여 가능 여부, 동의 안내 버전, 파기 예정 시각을 반환한다")
        void shouldReturnGatheringWithoutAuthentication() throws Exception {
            // given
            String joinToken = saveGatheringWithJoinToken();

            // when & then
            mockMvc.perform(get("/api/v1/gatherings/preview").header("X-Join-Token", joinToken))
                    .andExpect(status().isOk())
                    .andExpect(
                            jsonPath("$.data.name").value(GatheringFixture.FRIDAY_DINNER.getName()))
                    .andExpect(jsonPath("$.data.joinable").value(true))
                    .andExpect(jsonPath("$.data.joinClosedReason").value(nullValue()))
                    .andExpect(
                            jsonPath("$.data.consentVersion")
                                    .value(GatheringPolicy.CONSENT_VERSION))
                    .andExpect(jsonPath("$.data.deletesAt").isNotEmpty());
        }
    }

    // 정산 건 테스트의 공통 준비다. 모임장은 참여자가 아니고, 모임에는 참여자 3명이 있다.
    private void givenSettlementGathering() {
        host = memberHelper.save(MemberFixture.KAKAO);
        hostToken = authHelper.bearerTokenFor(host);
        gathering =
                gatheringHelper.join(
                        gatheringHelper.save(GatheringFixture.FRIDAY_DINNER, host.getId()),
                        ParticipantFixture.KIM,
                        ParticipantFixture.LEE,
                        ParticipantFixture.PARK);
        payer = gathering.getParticipants().get(0);
        lee = gathering.getParticipants().get(1);
        park = gathering.getParticipants().get(2);
    }

    private String otherMemberToken() {
        return authHelper.bearerTokenFor(memberHelper.save(MemberFixture.GOOGLE));
    }

    // 같은 모임장이 연 다른 모임이다. 정산 건을 기록할 수 있도록 참여자 2명이 있다.
    private Gathering saveOtherGathering() {
        return gatheringHelper.join(
                gatheringHelper.join(
                        gatheringHelper.save(GatheringFixture.FRIDAY_DINNER, host.getId()),
                        ParticipantFixture.KIM),
                ParticipantFixture.LEE);
    }

    private Long roundId() {
        return gathering.getRounds().getFirst().getId();
    }

    private Long otherGatheringRoundId() {
        return saveOtherGathering().getRounds().getFirst().getId();
    }

    private List<Long> participantIds() {
        return List.of(payer.getId(), lee.getId(), park.getId());
    }

    private String saveGatheringWithJoinToken() {
        Member member = memberHelper.save(MemberFixture.KAKAO);
        Gathering gathering = gatheringHelper.save(GatheringFixture.FRIDAY_DINNER, member.getId());

        return gatheringHelper.issueJoinToken(gathering);
    }

    private GatheringPairingRequest seedPairingRequest() {
        Drink drink = catalogHelper.saveDrink(DrinkFixture.SOJU);
        Anju anju = catalogHelper.saveAnju(AnjuFixture.GOLBAENGI_MUCHIM);
        GatheringPairingFixture pairing = GatheringPairingFixture.FRIEND_CASUAL;

        return new GatheringPairingRequest(
                drink.getId(), anju.getId(), pairing.getRelationshipType(), pairing.getMoodType());
    }

    private long countPairingsOf(GatheringPairingRequest pairing) {
        return jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM gathering_pairing WHERE drink_id = ? AND anju_id = ?",
                Long.class,
                pairing.drinkId(),
                pairing.anjuId());
    }
}
