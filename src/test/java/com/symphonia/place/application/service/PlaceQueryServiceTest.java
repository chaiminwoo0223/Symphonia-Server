package com.symphonia.place.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

import com.symphonia.UnitTest;
import com.symphonia.catalog.application.dto.result.AnjuPlaceKeywordResult;
import com.symphonia.catalog.application.usecase.GetAnjuPlaceKeywordUseCase;
import com.symphonia.catalog.domain.exception.AnjuNotFoundException;
import com.symphonia.place.application.dto.query.RecommendPlacesQuery;
import com.symphonia.place.application.dto.result.PlaceResult;
import com.symphonia.place.domain.client.PlaceSearchClient;
import com.symphonia.place.domain.exception.PlaceSearchLimitExceededException;
import com.symphonia.place.domain.vo.PlaceSearchQuery;
import com.symphonia.place.fixture.PlaceFixture;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

@DisplayName("PlaceQueryService 단위 테스트")
class PlaceQueryServiceTest extends UnitTest {

    private static final String REGION = "강남구";
    private static final Long ANJU_ID = 7L;
    private static final String PLACE_KEYWORD = "호프";
    private static final PlaceSearchQuery SEARCH_QUERY = PlaceSearchQuery.of(REGION, PLACE_KEYWORD);

    @InjectMocks private PlaceQueryService placeQueryService;

    @Mock private GetAnjuPlaceKeywordUseCase getAnjuPlaceKeywordUseCase;
    @Mock private PlaceSearchClient placeSearchClient;

    @Nested
    @DisplayName("recommend 메서드는")
    class Recommend {

        @Nested
        @DisplayName("외부 검색이 가게를 돌려준 경우")
        class WhenSearchSucceeds {

            @Test
            @DisplayName("안주의 검색어와 지역명으로 검색해 결과를 순서 그대로 반환한다")
            void shouldReturnSearchedPlacesInOrder() {
                // given
                givenPlaceKeyword();
                given(placeSearchClient.search(SEARCH_QUERY))
                        .willReturn(
                                List.of(
                                        PlaceFixture.NONHYEON_PUB.create(),
                                        PlaceFixture.GANGNAM_HOF.create()));

                // when
                List<PlaceResult> results = placeQueryService.recommend(query());

                // then
                assertThat(results)
                        .containsExactly(
                                PlaceResult.from(PlaceFixture.NONHYEON_PUB.create()),
                                PlaceResult.from(PlaceFixture.GANGNAM_HOF.create()));
            }
        }

        @Nested
        @DisplayName("지역명에 공백이 여러 개인 경우")
        class WhenRegionHasExtraSpaces {

            @Test
            @DisplayName("공백을 하나로 정리한 검색어로 검색한다")
            void shouldSearchWithNormalizedRegion() {
                // given
                givenPlaceKeyword();
                PlaceSearchQuery normalized = PlaceSearchQuery.of("강남구 역삼동", PLACE_KEYWORD);
                given(placeSearchClient.search(normalized)).willReturn(List.of());

                // when
                placeQueryService.recommend(RecommendPlacesQuery.of("  강남구   역삼동 ", ANJU_ID));

                // then
                then(placeSearchClient).should().search(normalized);
            }
        }

        @Nested
        @DisplayName("외부 검색 결과가 없는 경우")
        class WhenNothingFound {

            @Test
            @DisplayName("빈 목록을 반환한다")
            void shouldReturnEmptyPlaces() {
                // given
                givenPlaceKeyword();
                given(placeSearchClient.search(SEARCH_QUERY)).willReturn(List.of());

                // when
                List<PlaceResult> results = placeQueryService.recommend(query());

                // then
                assertThat(results).isEmpty();
            }
        }

        @Nested
        @DisplayName("외부 검색이 실패한 경우")
        class WhenSearchFails {

            @Test
            @DisplayName("PlaceSearchLimitExceededException을 그대로 던진다")
            void shouldThrowPlaceSearchLimitExceededException() {
                // given
                givenPlaceKeyword();
                given(placeSearchClient.search(SEARCH_QUERY))
                        .willThrow(new PlaceSearchLimitExceededException());

                // when & then
                assertThatThrownBy(() -> placeQueryService.recommend(query()))
                        .isInstanceOf(PlaceSearchLimitExceededException.class);
            }
        }

        @Nested
        @DisplayName("안주가 존재하지 않는 경우")
        class WhenAnjuNotFound {

            @Test
            @DisplayName("AnjuNotFoundException을 던지고 외부 검색을 부르지 않는다")
            void shouldThrowAnjuNotFoundException() {
                // given
                given(getAnjuPlaceKeywordUseCase.getPlaceKeyword(ANJU_ID))
                        .willThrow(new AnjuNotFoundException());

                // when & then
                assertThatThrownBy(() -> placeQueryService.recommend(query()))
                        .isInstanceOf(AnjuNotFoundException.class);
                then(placeSearchClient).shouldHaveNoInteractions();
            }
        }

        private void givenPlaceKeyword() {
            given(getAnjuPlaceKeywordUseCase.getPlaceKeyword(ANJU_ID))
                    .willReturn(new AnjuPlaceKeywordResult(PLACE_KEYWORD));
        }

        private RecommendPlacesQuery query() {
            return RecommendPlacesQuery.of(REGION, ANJU_ID);
        }
    }
}
