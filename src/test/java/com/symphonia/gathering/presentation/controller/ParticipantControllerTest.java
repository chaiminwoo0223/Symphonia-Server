package com.symphonia.gathering.presentation.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.symphonia.IntegrationTest;
import com.symphonia.common.exception.error.CommonErrorCode;
import com.symphonia.gathering.domain.entity.Gathering;
import com.symphonia.gathering.domain.error.GatheringErrorCode;
import com.symphonia.gathering.domain.policy.GatheringPolicy;
import com.symphonia.gathering.domain.policy.SettlementPolicy;
import com.symphonia.gathering.fixture.GatheringFixture;
import com.symphonia.gathering.fixture.ParticipantBankAccountFixture;
import com.symphonia.gathering.fixture.ParticipantFixture;
import com.symphonia.gathering.helper.GatheringHelper;
import com.symphonia.gathering.presentation.dto.request.JoinGatheringRequest;
import com.symphonia.gathering.presentation.dto.request.RegisterBankAccountRequest;
import com.symphonia.member.domain.entity.Member;
import com.symphonia.member.fixture.MemberFixture;
import com.symphonia.member.helper.MemberHelper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

class ParticipantControllerTest extends IntegrationTest {

    private static final String HYPHENATED_PHONE_NUMBER = "010-1234-5678";
    private static final String MASKED_PHONE_NUMBER = "010-****-5678";
    private static final String HYPHENATED_ACCOUNT_NUMBER = "1234-5678-9012";

    @Autowired private GatheringHelper gatheringHelper;
    @Autowired private MemberHelper memberHelper;

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
                    name, HYPHENATED_PHONE_NUMBER, consentAgreed, consentVersion);
        }
    }

    @Nested
    @DisplayName("POST /api/v1/participants/bank-account는")
    class RegisterBankAccount {

        private String payerLinkToken;

        @BeforeEach
        void setUp() {
            Gathering gathering = gatheringHelper.join(saveGathering(), ParticipantFixture.KIM);
            payerLinkToken = gatheringHelper.issueLinkToken(gathering.getParticipants().get(0));
        }

        @Nested
        @DisplayName("등록할 수 있는 요청인 경우")
        class WhenRegistrable {

            @Test
            @DisplayName("인증 없이도 계좌를 등록하고 201을 반환한다")
            void shouldRegisterBankAccount() throws Exception {
                // given
                RegisterBankAccountRequest request =
                        bankAccountRequest(ParticipantBankAccountFixture.KOOKMIN.getBankName());

                // when & then
                mockMvc.perform(
                                post("/api/v1/participants/bank-account")
                                        .header("X-Link-Token", payerLinkToken)
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(objectMapper.writeValueAsString(request)))
                        .andExpect(status().isCreated())
                        .andExpect(jsonPath("$.ok").value(true));
            }
        }

        @Nested
        @DisplayName("은행 이름이 공백인 경우")
        class WhenBankNameBlank {

            @Test
            @DisplayName("bankName 필드 검증 실패와 함께 400을 반환한다")
            void shouldReturnBadRequest() throws Exception {
                // given
                RegisterBankAccountRequest request = bankAccountRequest(" ");

                // when & then
                mockMvc.perform(
                                post("/api/v1/participants/bank-account")
                                        .header("X-Link-Token", payerLinkToken)
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(objectMapper.writeValueAsString(request)))
                        .andExpect(status().isBadRequest())
                        .andExpect(
                                jsonPath("$.data.code")
                                        .value(CommonErrorCode.METHOD_ARGUMENT_NOT_VALID.getCode()))
                        .andExpect(jsonPath("$.data.violations[0].field").value("bankName"));
            }
        }

        private RegisterBankAccountRequest bankAccountRequest(String bankName) {
            return new RegisterBankAccountRequest(
                    bankName,
                    HYPHENATED_ACCOUNT_NUMBER,
                    true,
                    SettlementPolicy.BANK_ACCOUNT_CONSENT_VERSION);
        }
    }

    private Gathering saveGathering() {
        Member member = memberHelper.save(MemberFixture.KAKAO);

        return gatheringHelper.save(GatheringFixture.FRIDAY_DINNER, member.getId());
    }

    private String saveGatheringWithJoinToken() {
        return gatheringHelper.issueJoinToken(saveGathering());
    }
}
