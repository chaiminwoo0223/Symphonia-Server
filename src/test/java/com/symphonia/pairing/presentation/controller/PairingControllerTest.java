package com.symphonia.pairing.presentation.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.symphonia.IntegrationTest;
import com.symphonia.auth.helper.AuthHelper;
import com.symphonia.catalog.domain.entity.Anju;
import com.symphonia.catalog.domain.entity.Drink;
import com.symphonia.catalog.domain.vo.AllergyType;
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
import com.symphonia.pairing.domain.vo.PairingSituation;
import com.symphonia.pairing.domain.vo.RelationshipType;
import com.symphonia.pairing.fixture.PairingFeedbackFixture;
import com.symphonia.pairing.presentation.dto.request.SubmitPairingFeedbackRequest;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

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
                    .andExpect(jsonPath("$.data.pairings").isArray())
                    .andExpect(jsonPath("$.data.pairings[0].score").exists())
                    .andExpect(jsonPath("$.data.pairings[0].reasons").isArray())
                    .andExpect(jsonPath("$.data.pairings[0].reasons").isNotEmpty())
                    .andExpect(jsonPath("$.data.notices").isArray())
                    .andExpect(jsonPath("$.data.notices").isEmpty());
        }

        @Test
        @DisplayName("필수 파라미터가 없으면 400을 반환한다")
        void shouldReturnBadRequestWhenParameterMissing() throws Exception {
            // when & then
            mockMvc.perform(
                            get("/api/v1/pairings/recommendations")
                                    .param("moodType", "CASUAL")
                                    .param("isAdultConfirmed", "true"))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("moodType 없이 요청해도 추천 목록을 반환한다")
        void shouldReturnRecommendationsWhenMoodTypeMissing() throws Exception {
            // given
            seedCatalog();

            // when & then
            mockMvc.perform(
                            get("/api/v1/pairings/recommendations")
                                    .param("relationshipType", "FRIEND")
                                    .param("isAdultConfirmed", "true"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.pairings").isNotEmpty());
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
                    .andExpect(jsonPath("$.data.pairings").isNotEmpty())
                    .andExpect(
                            jsonPath(
                                            "$.data.pairings[?(@.anjuName=='%s')]",
                                            AnjuFixture.DRIED_SNACK.getName())
                                    .isEmpty());
        }

        @Test
        @DisplayName("술의 알레르기를 drinkAllergyTypes로 응답한다")
        void shouldRespondDrinkAllergyTypes() throws Exception {
            // given
            catalogHelper.saveDrink(DrinkFixture.WINE, Set.of(AllergyType.SULFITE));
            catalogHelper.saveAnju(AnjuFixture.DRIED_SNACK);

            // when & then
            mockMvc.perform(
                            get("/api/v1/pairings/recommendations")
                                    .param("relationshipType", "FRIEND")
                                    .param("isAdultConfirmed", "true")
                                    .param("attendeeAllergies", "WHEAT"))
                    .andExpect(status().isOk())
                    .andExpect(
                            jsonPath(
                                            "$.data.pairings[?(@.drinkName=='%s')].drinkAllergyTypes[0]",
                                            DrinkFixture.WINE.getName())
                                    .value("SULFITE"));
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
                    .andExpect(
                            jsonPath("$.data.pairings[?(@.drinkNonAlcoholic==true)]").isNotEmpty());
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
        @DisplayName("싫어요를 남긴 회원이 로그인해 요청한 경우")
        class WhenRequestedByMemberWhoDisliked {

            @Test
            @DisplayName("싫어요를 남긴 조합을 제외한다")
            void shouldExcludeDislikedCombination() throws Exception {
                // given
                Member member = memberHelper.save(MemberFixture.KAKAO);
                String token = authHelper.bearerTokenFor(member);
                Anju disliked = seedCatalogWithDislikeOf(member.getId());

                // when & then
                mockMvc.perform(recommendRequest(token))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.data.pairings").isNotEmpty())
                        .andExpect(
                                jsonPath("$.data.pairings[?(@.anjuName=='%s')]", disliked.getName())
                                        .isEmpty());
            }
        }

        @Nested
        @DisplayName("다른 회원이 로그인해 요청한 경우")
        class WhenRequestedByOtherMember {

            @Test
            @DisplayName("싫어요를 남긴 조합을 제외하지 않는다")
            void shouldKeepDislikedCombination() throws Exception {
                // given
                Member disliker = memberHelper.save(MemberFixture.KAKAO);
                Member other = memberHelper.save(MemberFixture.GOOGLE);
                String token = authHelper.bearerTokenFor(other);
                Anju disliked = seedCatalogWithDislikeOf(disliker.getId());

                // when & then
                mockMvc.perform(recommendRequest(token))
                        .andExpect(status().isOk())
                        .andExpect(
                                jsonPath("$.data.pairings[?(@.anjuName=='%s')]", disliked.getName())
                                        .isNotEmpty());
            }
        }

        @Nested
        @DisplayName("유효한 액세스 토큰 없이 요청한 경우")
        class WhenRequestedWithoutValidToken {

            @Test
            @DisplayName("토큰이 없으면 제외하지 않고 그대로 추천한다")
            void shouldKeepDislikedCombinationWhenTokenMissing() throws Exception {
                // given
                Member member = memberHelper.save(MemberFixture.KAKAO);
                Anju disliked = seedCatalogWithDislikeOf(member.getId());

                // when & then
                mockMvc.perform(recommendRequest(null))
                        .andExpect(status().isOk())
                        .andExpect(
                                jsonPath("$.data.pairings[?(@.anjuName=='%s')]", disliked.getName())
                                        .isNotEmpty());
            }

            @Test
            @DisplayName("토큰이 유효하지 않으면 401이 아니라 비로그인 요청으로 추천한다")
            void shouldKeepDislikedCombinationWhenTokenInvalid() throws Exception {
                // given
                Member member = memberHelper.save(MemberFixture.KAKAO);
                Anju disliked = seedCatalogWithDislikeOf(member.getId());

                // when & then
                mockMvc.perform(recommendRequest("Bearer invalid-token"))
                        .andExpect(status().isOk())
                        .andExpect(
                                jsonPath("$.data.pairings[?(@.anjuName=='%s')]", disliked.getName())
                                        .isNotEmpty());
            }
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
    @DisplayName("GET /api/v1/pairings/feedback는")
    class GetFeedbacks {

        @Test
        @DisplayName("회원이 낸 피드백을 최근에 낸 순서로 술과 안주의 이름과 함께 200으로 반환한다")
        void shouldReturnMemberFeedbacks() throws Exception {
            // given
            Member member = memberHelper.save(MemberFixture.KAKAO);
            String token = authHelper.bearerTokenFor(member);
            Drink drink = catalogHelper.saveDrink(DrinkFixture.SOJU);
            Anju first = catalogHelper.saveAnju(AnjuFixture.GOLBAENGI_MUCHIM);
            Anju second = catalogHelper.saveAnju(AnjuFixture.DRIED_SNACK);
            saveFeedback(member.getId(), drink.getId(), first.getId());
            saveFeedback(member.getId(), drink.getId(), second.getId());

            // when & then
            mockMvc.perform(
                            get("/api/v1/pairings/feedback")
                                    .header(HttpHeaders.AUTHORIZATION, token))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.feedbacks.length()").value(2))
                    .andExpect(jsonPath("$.data.feedbacks[0].anjuId").value(second.getId()))
                    .andExpect(jsonPath("$.data.feedbacks[0].anjuName").value(second.getName()))
                    .andExpect(jsonPath("$.data.feedbacks[0].drinkId").value(drink.getId()))
                    .andExpect(jsonPath("$.data.feedbacks[0].drinkName").value(drink.getName()))
                    .andExpect(jsonPath("$.data.feedbacks[0].rating").value("LIKE"))
                    .andExpect(jsonPath("$.data.feedbacks[0].relationshipType").value("FRIEND"))
                    .andExpect(jsonPath("$.data.feedbacks[0].submittedAt").isNotEmpty())
                    .andExpect(jsonPath("$.data.feedbacks[1].anjuId").value(first.getId()))
                    .andExpect(jsonPath("$.data.nextCursor").doesNotExist());
        }

        @Test
        @DisplayName("다른 회원이 낸 피드백과 익명 피드백은 담지 않는다")
        void shouldExcludeOtherMembersAndAnonymousFeedbacks() throws Exception {
            // given
            Member member = memberHelper.save(MemberFixture.KAKAO);
            Member otherMember = memberHelper.save(MemberFixture.GOOGLE);
            String token = authHelper.bearerTokenFor(member);
            Drink drink = catalogHelper.saveDrink(DrinkFixture.SOJU);
            Anju anju = catalogHelper.saveAnju(AnjuFixture.GOLBAENGI_MUCHIM);
            saveFeedback(otherMember.getId(), drink.getId(), anju.getId());
            pairingFeedbackRepository.save(
                    PairingFeedbackFixture.FRIEND_FORMAL.createAnonymousWithReferences(
                            drink.getId(), anju.getId()));

            // when & then
            mockMvc.perform(
                            get("/api/v1/pairings/feedback")
                                    .header(HttpHeaders.AUTHORIZATION, token))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.feedbacks.length()").value(0));
        }

        @Test
        @DisplayName("size보다 피드백이 많으면 마지막으로 읽은 피드백 id를 nextCursor로 반환한다")
        void shouldReturnNextCursorWhenMoreFeedbacksRemain() throws Exception {
            // given
            Member member = memberHelper.save(MemberFixture.KAKAO);
            String token = authHelper.bearerTokenFor(member);
            Drink drink = catalogHelper.saveDrink(DrinkFixture.SOJU);
            Anju first = catalogHelper.saveAnju(AnjuFixture.GOLBAENGI_MUCHIM);
            Anju second = catalogHelper.saveAnju(AnjuFixture.DRIED_SNACK);
            Anju third = catalogHelper.saveAnju(AnjuFixture.FRIED_CHICKEN);
            saveFeedback(member.getId(), drink.getId(), first.getId());
            Long secondFeedbackId = saveFeedback(member.getId(), drink.getId(), second.getId());
            saveFeedback(member.getId(), drink.getId(), third.getId());

            // when & then
            mockMvc.perform(
                            get("/api/v1/pairings/feedback")
                                    .param("size", "2")
                                    .header(HttpHeaders.AUTHORIZATION, token))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.feedbacks.length()").value(2))
                    .andExpect(jsonPath("$.data.feedbacks[1].anjuId").value(second.getId()))
                    .andExpect(jsonPath("$.data.nextCursor").value(secondFeedbackId));
        }

        @Test
        @DisplayName("cursor로 준 id보다 먼저 낸 피드백만 이어서 반환하고 마지막이면 nextCursor를 주지 않는다")
        void shouldReturnFeedbacksBeforeCursor() throws Exception {
            // given
            Member member = memberHelper.save(MemberFixture.KAKAO);
            String token = authHelper.bearerTokenFor(member);
            Drink drink = catalogHelper.saveDrink(DrinkFixture.SOJU);
            Anju first = catalogHelper.saveAnju(AnjuFixture.GOLBAENGI_MUCHIM);
            Anju second = catalogHelper.saveAnju(AnjuFixture.DRIED_SNACK);
            Anju third = catalogHelper.saveAnju(AnjuFixture.FRIED_CHICKEN);
            saveFeedback(member.getId(), drink.getId(), first.getId());
            Long secondFeedbackId = saveFeedback(member.getId(), drink.getId(), second.getId());
            saveFeedback(member.getId(), drink.getId(), third.getId());

            // when & then
            mockMvc.perform(
                            get("/api/v1/pairings/feedback")
                                    .param("size", "2")
                                    .param("cursor", String.valueOf(secondFeedbackId))
                                    .header(HttpHeaders.AUTHORIZATION, token))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.feedbacks.length()").value(1))
                    .andExpect(jsonPath("$.data.feedbacks[0].anjuId").value(first.getId()))
                    .andExpect(jsonPath("$.data.nextCursor").doesNotExist());
        }

        @Test
        @DisplayName("size가 50을 넘으면 400을 반환한다")
        void shouldReturnBadRequestWhenSizeExceedsLimit() throws Exception {
            // given
            Member member = memberHelper.save(MemberFixture.KAKAO);
            String token = authHelper.bearerTokenFor(member);

            // when & then
            mockMvc.perform(
                            get("/api/v1/pairings/feedback")
                                    .param("size", "51")
                                    .header(HttpHeaders.AUTHORIZATION, token))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("인증 토큰이 없으면 401을 반환한다")
        void shouldReturnUnauthorizedWhenUnauthenticated() throws Exception {
            // when & then
            mockMvc.perform(get("/api/v1/pairings/feedback")).andExpect(status().isUnauthorized());
        }

        private Long saveFeedback(Long memberId, Long drinkId, Long anjuId) {
            return pairingFeedbackRepository
                    .save(
                            PairingFeedbackFixture.FRIEND_FORMAL.createWithMemberAndReferences(
                                    memberId, drinkId, anjuId))
                    .getId();
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
            @DisplayName("분위기 없이 받은 추천의 피드백도 저장하고 201을 반환한다")
            void shouldSaveFeedbackWhenMoodTypeMissing() throws Exception {
                // given
                Member member = memberHelper.save(MemberFixture.KAKAO);
                String token = authHelper.bearerTokenFor(member);
                SubmitPairingFeedbackRequest seeded = seedFeedbackRequest();
                SubmitPairingFeedbackRequest request =
                        new SubmitPairingFeedbackRequest(
                                seeded.drinkId(),
                                seeded.anjuId(),
                                seeded.relationshipType(),
                                null,
                                seeded.rating());

                // when & then
                mockMvc.perform(
                                post("/api/v1/pairings/feedback")
                                        .header(HttpHeaders.AUTHORIZATION, token)
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(objectMapper.writeValueAsString(request)))
                        .andExpect(status().isCreated())
                        .andExpect(jsonPath("$.data.moodType").value(nullValue()));
            }

            @Test
            @DisplayName("같은 조합에 다시 내면 기존 피드백을 바꾸고 전역 집계와 상황별 집계를 고친 뒤 200을 반환한다")
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
                pairingRatingSummaryRepository.accumulateInSituation(
                        request.drinkId(),
                        request.anjuId(),
                        submitted.situation(),
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
                assertThat(
                                pairingRatingSummaryRepository.findAllWithSampleAtLeast(
                                        1,
                                        PairingSituation.of(
                                                request.relationshipType(), request.moodType())))
                        .containsExactly(
                                PairingRatingSummary.of(
                                        request.drinkId(), request.anjuId(), 0, 1, 0, 1));
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

    // 소주에 마른안주와 골뱅이무침을 두고, 회원이 마른안주 조합에 싫어요를 남긴 상태로 만든다. 싫어요를 남긴 안주를 반환한다.
    private Anju seedCatalogWithDislikeOf(Long memberId) {
        Drink drink = catalogHelper.saveDrink(DrinkFixture.SOJU);
        Anju disliked = catalogHelper.saveAnju(AnjuFixture.DRIED_SNACK);
        catalogHelper.saveAnju(AnjuFixture.GOLBAENGI_MUCHIM);
        pairingFeedbackRepository.save(
                PairingFeedback.of(
                        memberId,
                        drink.getId(),
                        disliked.getId(),
                        RelationshipType.FRIEND,
                        null,
                        PairingRating.DISLIKE));

        return disliked;
    }

    private MockHttpServletRequestBuilder recommendRequest(String authorization) {
        MockHttpServletRequestBuilder request =
                get("/api/v1/pairings/recommendations")
                        .param("relationshipType", "FRIEND")
                        .param("isAdultConfirmed", "true");

        return authorization == null
                ? request
                : request.header(HttpHeaders.AUTHORIZATION, authorization);
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
