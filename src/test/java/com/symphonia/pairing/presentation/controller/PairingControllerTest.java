package com.symphonia.pairing.presentation.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.symphonia.IntegrationTest;
import com.symphonia.auth.helper.AuthHelper;
import com.symphonia.catalog.domain.entity.Anju;
import com.symphonia.catalog.domain.entity.Drink;
import com.symphonia.catalog.fixture.AnjuFixture;
import com.symphonia.catalog.fixture.DrinkFixture;
import com.symphonia.catalog.helper.CatalogHelper;
import com.symphonia.common.exception.error.CommonErrorCode;
import com.symphonia.member.domain.entity.Member;
import com.symphonia.member.fixture.MemberFixture;
import com.symphonia.member.helper.MemberHelper;
import com.symphonia.pairing.domain.entity.PairingFeedback;
import com.symphonia.pairing.domain.repository.PairingFeedbackRepository;
import com.symphonia.pairing.domain.repository.PairingRatingSummaryRepository;
import com.symphonia.pairing.domain.vo.MoodType;
import com.symphonia.pairing.domain.vo.PairingRating;
import com.symphonia.pairing.domain.vo.PairingRatingChange;
import com.symphonia.pairing.domain.vo.PairingRatingSummary;
import com.symphonia.pairing.domain.vo.RelationshipType;
import com.symphonia.pairing.fixture.PairingFeedbackFixture;
import com.symphonia.pairing.presentation.dto.request.SubmitPairingFeedbackRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.jdbc.Sql;

@Sql("/sql/clear-catalog.sql")
class PairingControllerTest extends IntegrationTest {

    @Autowired private CatalogHelper catalogHelper;
    @Autowired private MemberHelper memberHelper;
    @Autowired private AuthHelper authHelper;
    @Autowired private PairingFeedbackRepository pairingFeedbackRepository;
    @Autowired private PairingRatingSummaryRepository pairingRatingSummaryRepository;

    @Nested
    @DisplayName("GET /api/v1/pairings/recommendations는")
    class Recommend {

        @Test
        @DisplayName("인증 없이도 추천 목록을 반환한다")
        void shouldReturnRecommendationsWithoutAuthentication() throws Exception {
            // given
            seedCatalog();

            // when & then
            mockMvc.perform(
                            get("/api/v1/pairings/recommendations")
                                    .param("relationshipType", "FRIEND")
                                    .param("moodType", "CASUAL")
                                    .param("isAdultConfirmed", "true"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data").isArray())
                    .andExpect(jsonPath("$.data[0].score").exists())
                    .andExpect(jsonPath("$.data[0].reasons").isArray())
                    .andExpect(jsonPath("$.data[0].reasons").isNotEmpty());
        }

        @Test
        @DisplayName("필수 파라미터가 없으면 400을 반환한다")
        void shouldReturnBadRequestWhenParameterMissing() throws Exception {
            // when & then
            mockMvc.perform(
                            get("/api/v1/pairings/recommendations")
                                    .param("relationshipType", "FRIEND")
                                    .param("isAdultConfirmed", "true"))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("attendeeAllergies와 겹치는 안주는 추천 목록에서 제외한다")
        void shouldExcludeAnjuConflictingWithAttendeeAllergies() throws Exception {
            // given
            catalogHelper.saveDrink(DrinkFixture.SOJU);
            catalogHelper.saveAnju(AnjuFixture.GOLBAENGI_MUCHIM);
            catalogHelper.saveAnju(AnjuFixture.DRIED_SNACK);

            // when & then
            mockMvc.perform(
                            get("/api/v1/pairings/recommendations")
                                    .param("relationshipType", "FRIEND")
                                    .param("moodType", "CASUAL")
                                    .param("isAdultConfirmed", "true")
                                    .param("attendeeAllergies", "PEANUT"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data").isNotEmpty())
                    .andExpect(
                            jsonPath(
                                            "$.data[?(@.anjuName=='%s')]",
                                            AnjuFixture.DRIED_SNACK.getName())
                                    .isEmpty());
        }

        @Test
        @DisplayName("attendeeConstraints를 반영해 무알코올 Drink를 응답에 포함한다")
        void shouldIncludeNonAlcoholicDrinkWhenAttendeeConstraintsGiven() throws Exception {
            // given
            catalogHelper.saveDrink(DrinkFixture.SOJU);
            catalogHelper.saveDrink(DrinkFixture.SODA);
            catalogHelper.saveAnju(AnjuFixture.GOLBAENGI_MUCHIM);

            // when & then
            mockMvc.perform(
                            get("/api/v1/pairings/recommendations")
                                    .param("relationshipType", "FRIEND")
                                    .param("moodType", "CASUAL")
                                    .param("isAdultConfirmed", "true")
                                    .param("attendeeConstraints", "DRIVER", "PREGNANT"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data[?(@.drinkNonAlcoholic==true)]").isNotEmpty());
        }

        @Test
        @DisplayName("정의되지 않은 attendeeAllergies 값이면 400을 반환한다")
        void shouldReturnBadRequestWhenAttendeeAllergyInvalid() throws Exception {
            // when & then
            mockMvc.perform(
                            get("/api/v1/pairings/recommendations")
                                    .param("relationshipType", "FRIEND")
                                    .param("moodType", "CASUAL")
                                    .param("isAdultConfirmed", "true")
                                    .param("attendeeAllergies", "NOT_AN_ALLERGY"))
                    .andExpect(status().isBadRequest());
        }

        @Nested
        @DisplayName("성인 인증이 되지 않은 경우")
        class WhenAdultNotConfirmed {

            @Test
            @DisplayName("isAdultConfirmed 파라미터가 없으면 400을 반환한다")
            void shouldReturnBadRequestWhenIsAdultConfirmedMissing() throws Exception {
                // when & then
                mockMvc.perform(
                                get("/api/v1/pairings/recommendations")
                                        .param("relationshipType", "FRIEND")
                                        .param("moodType", "CASUAL"))
                        .andExpect(status().isBadRequest());
            }

            @Test
            @DisplayName("isAdultConfirmed가 false면 400을 반환한다")
            void shouldReturnBadRequestWhenIsAdultConfirmedFalse() throws Exception {
                // when & then
                mockMvc.perform(
                                get("/api/v1/pairings/recommendations")
                                        .param("relationshipType", "FRIEND")
                                        .param("moodType", "CASUAL")
                                        .param("isAdultConfirmed", "false"))
                        .andExpect(status().isBadRequest());
            }
        }
    }

    @Nested
    @DisplayName("POST /api/v1/pairings/feedback는")
    class SubmitFeedback {

        @Nested
        @DisplayName("인증된 요청인 경우")
        class WhenAuthenticated {

            @Test
            @DisplayName("피드백을 저장하고 201을 반환한다")
            void shouldSaveFeedback() throws Exception {
                // given
                Member member = memberHelper.save(MemberFixture.KAKAO);
                String token = authHelper.bearerTokenFor(member);
                SubmitPairingFeedbackRequest request = seedFeedbackRequest();

                // when & then
                mockMvc.perform(
                                post("/api/v1/pairings/feedback")
                                        .header(HttpHeaders.AUTHORIZATION, token)
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(objectMapper.writeValueAsString(request)))
                        .andExpect(status().isCreated())
                        .andExpect(jsonPath("$.data.drinkId").value(request.drinkId()))
                        .andExpect(jsonPath("$.data.anjuId").value(request.anjuId()))
                        .andExpect(jsonPath("$.data.rating").value(request.rating().name()));
            }

            @Test
            @DisplayName("같은 조합에 다시 내면 기존 피드백을 바꾸고 집계를 고친 뒤 200을 반환한다")
            void shouldUpdateFeedbackWhenResubmitted() throws Exception {
                // given
                Member member = memberHelper.save(MemberFixture.KAKAO);
                String token = authHelper.bearerTokenFor(member);
                SubmitPairingFeedbackRequest request = seedFeedbackRequest(PairingRating.DISLIKE);
                PairingFeedback submitted =
                        pairingFeedbackRepository.save(
                                PairingFeedbackFixture.FRIEND_FORMAL.createWithMemberAndReferences(
                                        member.getId(), request.drinkId(), request.anjuId()));
                pairingRatingSummaryRepository.accumulate(
                        request.drinkId(),
                        request.anjuId(),
                        PairingRatingChange.added(
                                PairingFeedbackFixture.FRIEND_FORMAL.getRating()));

                // when & then
                mockMvc.perform(
                                post("/api/v1/pairings/feedback")
                                        .header(HttpHeaders.AUTHORIZATION, token)
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(objectMapper.writeValueAsString(request)))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.data.id").value(submitted.getId()))
                        .andExpect(jsonPath("$.data.rating").value(PairingRating.DISLIKE.name()));
                assertThat(pairingRatingSummaryRepository.findAllWithSampleAtLeast(1))
                        .containsExactly(
                                PairingRatingSummary.of(request.drinkId(), request.anjuId(), 0, 1));
            }

            @Test
            @DisplayName("rating이 없으면 400을 반환한다")
            void shouldReturnBadRequestWhenRatingMissing() throws Exception {
                // given
                Member member = memberHelper.save(MemberFixture.KAKAO);
                String token = authHelper.bearerTokenFor(member);
                SubmitPairingFeedbackRequest request = seedFeedbackRequest(null);

                // when & then
                mockMvc.perform(
                                post("/api/v1/pairings/feedback")
                                        .header(HttpHeaders.AUTHORIZATION, token)
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(objectMapper.writeValueAsString(request)))
                        .andExpect(status().isBadRequest())
                        .andExpect(jsonPath("$.ok").value(false))
                        .andExpect(
                                jsonPath("$.data.code")
                                        .value(CommonErrorCode.METHOD_ARGUMENT_NOT_VALID.getCode()))
                        .andExpect(jsonPath("$.data.violations[0].field").value("rating"));
            }
        }

        @Nested
        @DisplayName("인증 토큰이 없는 경우")
        class WhenUnauthenticated {

            @Test
            @DisplayName("401을 반환한다")
            void shouldReturnUnauthorized() throws Exception {
                // given
                SubmitPairingFeedbackRequest request = seedFeedbackRequest();

                // when & then
                mockMvc.perform(
                                post("/api/v1/pairings/feedback")
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(objectMapper.writeValueAsString(request)))
                        .andExpect(status().isUnauthorized());
            }
        }
    }

    // 소주와 마른안주는 맛이 가까워 추천 사유가 하나 이상 붙는다.
    private void seedCatalog() {
        catalogHelper.saveDrink(DrinkFixture.SOJU);
        catalogHelper.saveAnju(AnjuFixture.DRIED_SNACK);
    }

    private SubmitPairingFeedbackRequest seedFeedbackRequest() {
        return seedFeedbackRequest(PairingRating.LIKE);
    }

    private SubmitPairingFeedbackRequest seedFeedbackRequest(PairingRating rating) {
        Drink drink = catalogHelper.saveDrink(DrinkFixture.SOJU);
        Anju anju = catalogHelper.saveAnju(AnjuFixture.GOLBAENGI_MUCHIM);
        return new SubmitPairingFeedbackRequest(
                drink.getId(), anju.getId(), RelationshipType.FRIEND, MoodType.CASUAL, rating);
    }
}
