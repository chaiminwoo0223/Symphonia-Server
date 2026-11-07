package com.symphonia.gathering.presentation.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.symphonia.IntegrationTest;
import com.symphonia.auth.helper.AuthHelper;
import com.symphonia.catalog.domain.entity.Anju;
import com.symphonia.catalog.domain.entity.Drink;
import com.symphonia.catalog.domain.entity.MusicMood;
import com.symphonia.catalog.domain.error.CatalogErrorCode;
import com.symphonia.catalog.fixture.AnjuFixture;
import com.symphonia.catalog.fixture.DrinkFixture;
import com.symphonia.catalog.fixture.MusicMoodFixture;
import com.symphonia.catalog.helper.CatalogHelper;
import com.symphonia.common.exception.error.CommonErrorCode;
import com.symphonia.gathering.domain.entity.Gathering;
import com.symphonia.gathering.domain.error.GatheringErrorCode;
import com.symphonia.gathering.domain.policy.GatheringPolicy;
import com.symphonia.gathering.fixture.GatheringFixture;
import com.symphonia.gathering.fixture.GatheringPairingFixture;
import com.symphonia.gathering.helper.GatheringHelper;
import com.symphonia.gathering.presentation.dto.request.GatheringPairingRequest;
import com.symphonia.gathering.presentation.dto.request.OpenGatheringRequest;
import com.symphonia.member.domain.entity.Member;
import com.symphonia.member.fixture.MemberFixture;
import com.symphonia.member.helper.MemberHelper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.jdbc.Sql;

@Sql("/sql/clear-catalog.sql")
class GatheringControllerTest extends IntegrationTest {

    @Autowired private GatheringHelper gatheringHelper;
    @Autowired private MemberHelper memberHelper;
    @Autowired private AuthHelper authHelper;
    @Autowired private CatalogHelper catalogHelper;
    @Autowired private JdbcTemplate jdbcTemplate;

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
                        .andExpect(jsonPath("$.data.joinToken").isNotEmpty());
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
                                        pairing.getMusicMoodId(),
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
                        .andExpect(jsonPath("$.data.joinToken").value(not(oldJoinToken)));
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
                                seeded.musicMoodId(),
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
                    .andExpect(
                            jsonPath("$.data.consentVersion")
                                    .value(GatheringPolicy.CONSENT_VERSION))
                    .andExpect(jsonPath("$.data.deletesAt").isNotEmpty());
        }
    }

    private String saveGatheringWithJoinToken() {
        Member member = memberHelper.save(MemberFixture.KAKAO);
        Gathering gathering = gatheringHelper.save(GatheringFixture.FRIDAY_DINNER, member.getId());

        return gatheringHelper.issueJoinToken(gathering);
    }

    private GatheringPairingRequest seedPairingRequest() {
        Drink drink = catalogHelper.saveDrink(DrinkFixture.SOJU);
        Anju anju = catalogHelper.saveAnju(AnjuFixture.GOLBAENGI_MUCHIM);
        MusicMood musicMood = catalogHelper.saveMusicMood(MusicMoodFixture.FORMAL_JAZZ);
        GatheringPairingFixture pairing = GatheringPairingFixture.FRIEND_CASUAL;

        return new GatheringPairingRequest(
                drink.getId(),
                anju.getId(),
                musicMood.getId(),
                pairing.getRelationshipType(),
                pairing.getMoodType());
    }

    private long countPairingsOf(GatheringPairingRequest pairing) {
        return jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM gathering_pairing WHERE drink_id = ? AND anju_id = ? AND music_mood_id = ?",
                Long.class,
                pairing.drinkId(),
                pairing.anjuId(),
                pairing.musicMoodId());
    }
}
