package com.symphonia.place.presentation.controller;

import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.symphonia.IntegrationTest;
import com.symphonia.catalog.domain.entity.Anju;
import com.symphonia.catalog.domain.error.CatalogErrorCode;
import com.symphonia.catalog.fixture.AnjuFixture;
import com.symphonia.catalog.helper.CatalogHelper;
import com.symphonia.common.exception.error.CommonErrorCode;
import com.symphonia.place.domain.client.PlaceSearchClient;
import com.symphonia.place.domain.error.PlaceErrorCode;
import com.symphonia.place.domain.exception.PlaceSearchLimitExceededException;
import com.symphonia.place.domain.exception.PlaceSearchUnavailableException;
import com.symphonia.place.domain.vo.PlaceSearchQuery;
import com.symphonia.place.fixture.PlaceFixture;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.jdbc.Sql;

@Sql("/sql/clear-catalog.sql")
class PlaceControllerTest extends IntegrationTest {

    private static final String RECOMMEND_PATH = "/api/v1/places/recommendations";
    private static final String REGION = "강남구";

    @Autowired private CatalogHelper catalogHelper;

    @MockitoBean private PlaceSearchClient placeSearchClient;

    @Nested
    @DisplayName("GET /api/v1/places/recommendations는")
    class Recommend {

        @Test
        @DisplayName("인증 없이도 외부 검색이 돌려준 순서 그대로 출처와 함께 가게 목록을 반환한다")
        void shouldReturnPlacesInOrderWithSourceWithoutAuthentication() throws Exception {
            // given
            Anju anju = catalogHelper.saveAnju(AnjuFixture.GOLBAENGI_MUCHIM);
            given(placeSearchClient.search(searchQueryOf(anju)))
                    .willReturn(
                            List.of(
                                    PlaceFixture.NONHYEON_PUB.create(),
                                    PlaceFixture.GANGNAM_HOF.create()));

            // when & then
            mockMvc.perform(
                            get(RECOMMEND_PATH)
                                    .param("region", REGION)
                                    .param("anjuId", anju.getId().toString()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.source").value("네이버 검색결과"))
                    .andExpect(jsonPath("$.data.places.length()").value(2))
                    .andExpect(jsonPath("$.data.places[0].name").value("논현 요리주점"))
                    .andExpect(jsonPath("$.data.places[0].latitude").value(37.5240289))
                    .andExpect(jsonPath("$.data.places[0].longitude").value(127.0384976))
                    .andExpect(jsonPath("$.data.places[0].link").value("https://example.com/pub"))
                    .andExpect(jsonPath("$.data.places[1].name").value("강남 호프"));
        }

        @Test
        @DisplayName("검색 결과가 없으면 출처와 빈 목록을 반환한다")
        void shouldReturnEmptyPlacesWhenNothingFound() throws Exception {
            // given
            Anju anju = catalogHelper.saveAnju(AnjuFixture.GOLBAENGI_MUCHIM);
            given(placeSearchClient.search(searchQueryOf(anju))).willReturn(List.of());

            // when & then
            mockMvc.perform(
                            get(RECOMMEND_PATH)
                                    .param("region", REGION)
                                    .param("anjuId", anju.getId().toString()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.source").value("네이버 검색결과"))
                    .andExpect(jsonPath("$.data.places").isEmpty());
        }

        @Test
        @DisplayName("존재하지 않는 안주면 404를 반환한다")
        void shouldReturnNotFoundWhenAnjuNotExists() throws Exception {
            // when & then
            mockMvc.perform(get(RECOMMEND_PATH).param("region", REGION).param("anjuId", "-1"))
                    .andExpect(status().isNotFound())
                    .andExpect(
                            jsonPath("$.data.code").value(CatalogErrorCode.ANJU_NOT_FOUND.name()));
        }

        @Test
        @DisplayName("지역명이 없으면 400을 반환한다")
        void shouldReturnBadRequestWhenRegionMissing() throws Exception {
            // when & then
            mockMvc.perform(get(RECOMMEND_PATH).param("anjuId", "1"))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("지역명에 한글, 숫자, 공백 외의 문자가 있으면 400을 반환한다")
        void shouldReturnBadRequestWhenRegionHasInvalidCharacters() throws Exception {
            // when & then
            mockMvc.perform(get(RECOMMEND_PATH).param("region", "강남구<script>").param("anjuId", "1"))
                    .andExpect(status().isBadRequest())
                    .andExpect(
                            jsonPath("$.data.code")
                                    .value(CommonErrorCode.METHOD_ARGUMENT_NOT_VALID.name()));
        }

        @Test
        @DisplayName("지역명이 30자를 넘으면 400을 반환한다")
        void shouldReturnBadRequestWhenRegionTooLong() throws Exception {
            // when & then
            mockMvc.perform(
                            get(RECOMMEND_PATH)
                                    .param("region", "가".repeat(31))
                                    .param("anjuId", "1"))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("안주 ID가 없으면 400을 반환한다")
        void shouldReturnBadRequestWhenAnjuIdMissing() throws Exception {
            // when & then
            mockMvc.perform(get(RECOMMEND_PATH).param("region", REGION))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("외부 검색의 호출 한도를 넘으면 503과 한도 초과 코드를 반환한다")
        void shouldReturnServiceUnavailableWhenLimitExceeded() throws Exception {
            // given
            Anju anju = catalogHelper.saveAnju(AnjuFixture.GOLBAENGI_MUCHIM);
            given(placeSearchClient.search(searchQueryOf(anju)))
                    .willThrow(new PlaceSearchLimitExceededException());

            // when & then
            mockMvc.perform(
                            get(RECOMMEND_PATH)
                                    .param("region", REGION)
                                    .param("anjuId", anju.getId().toString()))
                    .andExpect(status().isServiceUnavailable())
                    .andExpect(
                            jsonPath("$.data.code")
                                    .value(PlaceErrorCode.PLACE_SEARCH_LIMIT_EXCEEDED.name()));
        }

        @Test
        @DisplayName("외부 검색을 쓸 수 없으면 503과 검색 불가 코드를 반환한다")
        void shouldReturnServiceUnavailableWhenSearchUnavailable() throws Exception {
            // given
            Anju anju = catalogHelper.saveAnju(AnjuFixture.GOLBAENGI_MUCHIM);
            given(placeSearchClient.search(searchQueryOf(anju)))
                    .willThrow(new PlaceSearchUnavailableException());

            // when & then
            mockMvc.perform(
                            get(RECOMMEND_PATH)
                                    .param("region", REGION)
                                    .param("anjuId", anju.getId().toString()))
                    .andExpect(status().isServiceUnavailable())
                    .andExpect(
                            jsonPath("$.data.code")
                                    .value(PlaceErrorCode.PLACE_SEARCH_UNAVAILABLE.name()));
        }

        private PlaceSearchQuery searchQueryOf(Anju anju) {
            return PlaceSearchQuery.of(REGION, anju.getPlaceKeyword());
        }
    }
}
