package com.symphonia.pairing.presentation.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.symphonia.IntegrationTest;
import com.symphonia.auth.helper.AuthHelper;
import com.symphonia.member.domain.entity.Member;
import com.symphonia.member.fixture.MemberFixture;
import com.symphonia.member.helper.MemberHelper;
import com.symphonia.pairing.domain.entity.Anju;
import com.symphonia.pairing.domain.entity.Drink;
import com.symphonia.pairing.domain.entity.MusicMood;
import com.symphonia.pairing.domain.vo.MoodType;
import com.symphonia.pairing.domain.vo.RelationshipType;
import com.symphonia.pairing.fixture.AnjuFixture;
import com.symphonia.pairing.fixture.DrinkFixture;
import com.symphonia.pairing.fixture.MusicMoodFixture;
import com.symphonia.pairing.infrastructure.jpa.AnjuJpaEntity;
import com.symphonia.pairing.infrastructure.jpa.AnjuJpaRepository;
import com.symphonia.pairing.infrastructure.jpa.DrinkJpaEntity;
import com.symphonia.pairing.infrastructure.jpa.DrinkJpaRepository;
import com.symphonia.pairing.infrastructure.jpa.MusicMoodJpaEntity;
import com.symphonia.pairing.infrastructure.jpa.MusicMoodJpaRepository;
import com.symphonia.pairing.presentation.dto.request.SubmitPairingFeedbackRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

class PairingControllerTest extends IntegrationTest {

    @Autowired private DrinkJpaRepository drinkJpaRepository;
    @Autowired private AnjuJpaRepository anjuJpaRepository;
    @Autowired private MusicMoodJpaRepository musicMoodJpaRepository;
    @Autowired private MemberHelper memberHelper;
    @Autowired private AuthHelper authHelper;

    @Nested
    @DisplayName("GET /api/v1/pairings/recommend는")
    class Recommend {

        @Test
        @DisplayName("인증 없이도 추천 목록을 반환한다")
        void shouldReturnRecommendationsWithoutAuthentication() throws Exception {
            // given
            seedCatalog();

            // when & then
            mockMvc.perform(
                            get("/api/v1/pairings/recommend")
                                    .param("relationshipType", "FRIEND")
                                    .param("moodType", "CASUAL"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data").isArray())
                    .andExpect(jsonPath("$.data[0].score").exists());
        }

        @Test
        @DisplayName("필수 파라미터가 없으면 400을 반환한다")
        void shouldReturnBadRequestWhenParameterMissing() throws Exception {
            // when & then
            mockMvc.perform(get("/api/v1/pairings/recommend").param("relationshipType", "FRIEND"))
                    .andExpect(status().isBadRequest());
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
                        .andExpect(jsonPath("$.data.musicMoodId").value(request.musicMoodId()));
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

    private void seedCatalog() {
        seedDrink();
        seedAnju();
        seedMusicMood();
    }

    private Drink seedDrink() {
        return drinkJpaRepository.save(DrinkJpaEntity.from(DrinkFixture.SOJU.create())).toDomain();
    }

    private Anju seedAnju() {
        return anjuJpaRepository
                .save(AnjuJpaEntity.from(AnjuFixture.GOLBAENGI_MUCHIM.create()))
                .toDomain();
    }

    private MusicMood seedMusicMood() {
        return musicMoodJpaRepository
                .save(MusicMoodJpaEntity.from(MusicMoodFixture.FORMAL_JAZZ.create()))
                .toDomain();
    }

    private SubmitPairingFeedbackRequest seedFeedbackRequest() {
        Drink drink = seedDrink();
        Anju anju = seedAnju();
        MusicMood musicMood = seedMusicMood();
        return new SubmitPairingFeedbackRequest(
                drink.getId(),
                anju.getId(),
                musicMood.getId(),
                RelationshipType.FRIEND,
                MoodType.CASUAL);
    }
}
