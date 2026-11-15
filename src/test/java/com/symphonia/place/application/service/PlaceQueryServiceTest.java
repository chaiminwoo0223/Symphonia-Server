package com.symphonia.place.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

import com.symphonia.UnitTest;
import com.symphonia.catalog.application.dto.result.AnjuPlaceKeywordResult;
import com.symphonia.catalog.application.usecase.GetAnjuPlaceKeywordUseCase;
import com.symphonia.catalog.domain.exception.AnjuNotFoundException;
import com.symphonia.place.application.cache.PlaceSearchCache;
import com.symphonia.place.application.dto.query.RecommendPlacesQuery;
import com.symphonia.place.application.dto.result.PlaceResult;
import com.symphonia.place.domain.client.PlaceSearchClient;
import com.symphonia.place.domain.exception.PlaceSearchLimitExceededException;
import com.symphonia.place.domain.vo.Place;
import com.symphonia.place.domain.vo.PlaceSearchQuery;
import com.symphonia.place.fixture.PlaceFixture;
import java.util.List;
import java.util.Optional;
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
    @Mock private PlaceSearchCache placeSearchCache;
    @Mock private PlaceSearchClient placeSearchClient;

    @Nested
    @DisplayName("recommend 메서드는")
    class Recommend {

        @Nested
        @DisplayName("캐시에 검색 결과가 있는 경우")
        class WhenCacheHit {

            @Test
            @DisplayName("외부 검색을 부르지 않고 캐시의 가게를 순서 그대로 반환한다")
            void shouldReturnCachedPlacesWithoutSearching() {
                // given
                givenPlaceKeyword();
                List<Place> cached =
                        List.of(
                                PlaceFixture.NONHYEON_PUB.create(),
                                PlaceFixture.GANGNAM_HOF.create());
                given(placeSearchCache.find(SEARCH_QUERY)).willReturn(Optional.of(cached));

                // when
                List<PlaceResult> results = placeQueryService.recommend(query());

                // then
                assertThat(results)
                        .containsExactly(
                                PlaceResult.from(PlaceFixture.NONHYEON_PUB.create()),
                                PlaceResult.from(PlaceFixture.GANGNAM_HOF.create()));
                then(placeSearchClient).shouldHaveNoInteractions();
            }

            @Test
            @DisplayName("결과가 없던 검색의 빈 목록도 외부 검색 없이 빈 목록으로 반환한다")
            void shouldReturnEmptyWithoutSearchingWhenCachedEmpty() {
                // given
                givenPlaceKeyword();
                given(placeSearchCache.find(SEARCH_QUERY)).willReturn(Optional.of(List.of()));

                // when
                List<PlaceResult> results = placeQueryService.recommend(query());

                // then
                assertThat(results).isEmpty();
                then(placeSearchClient).shouldHaveNoInteractions();
            }
        }

        @Nested
        @DisplayName("캐시에 검색 결과가 없는 경우")
        class WhenCacheMissed {

            @Test
            @DisplayName("외부 검색 결과를 순서 그대로 반환하고 캐시에 담는다")
            void shouldSearchAndPutPlacesInOrder() {
                // given
                givenPlaceKeyword();
                given(placeSearchCache.find(SEARCH_QUERY)).willReturn(Optional.empty());
                List<Place> searched =
                        List.of(
                                PlaceFixture.NONHYEON_PUB.create(),
                                PlaceFixture.GANGNAM_HOF.create());
                given(placeSearchClient.search(SEARCH_QUERY)).willReturn(searched);

                // when
                List<PlaceResult> results = placeQueryService.recommend(query());

                // then
                assertThat(results)
                        .containsExactly(
                                PlaceResult.from(PlaceFixture.NONHYEON_PUB.create()),
                                PlaceResult.from(PlaceFixture.GANGNAM_HOF.create()));
                then(placeSearchCache).should().put(SEARCH_QUERY, searched);
            }

            @Test
            @DisplayName("검색 결과가 없으면 빈 목록도 캐시에 담는다")
            void shouldPutEmptyPlacesWhenNothingFound() {
                // given
                givenPlaceKeyword();
                given(placeSearchCache.find(SEARCH_QUERY)).willReturn(Optional.empty());
                given(placeSearchClient.search(SEARCH_QUERY)).willReturn(List.of());

                // when
                List<PlaceResult> results = placeQueryService.recommend(query());

                // then
                assertThat(results).isEmpty();
                then(placeSearchCache).should().put(SEARCH_QUERY, List.of());
            }

            @Test
            @DisplayName("공백이 여러 개인 지역명은 하나로 정리해 검색한다")
            void shouldSearchWithNormalizedRegion() {
                // given
                givenPlaceKeyword();
                PlaceSearchQuery normalized = PlaceSearchQuery.of("강남구 역삼동", PLACE_KEYWORD);
                given(placeSearchCache.find(normalized)).willReturn(Optional.empty());
                given(placeSearchClient.search(normalized)).willReturn(List.of());

                // when
                placeQueryService.recommend(RecommendPlacesQuery.of("  강남구   역삼동 ", ANJU_ID));

                // then
                then(placeSearchClient).should().search(normalized);
            }

            @Test
            @DisplayName("외부 검색이 실패하면 예외를 그대로 던지고 캐시에 담지 않는다")
            void shouldThrowPlaceSearchLimitExceededException() {
                // given
                givenPlaceKeyword();
                given(placeSearchCache.find(SEARCH_QUERY)).willReturn(Optional.empty());
                given(placeSearchClient.search(SEARCH_QUERY))
                        .willThrow(new PlaceSearchLimitExceededException());

                // when & then
                assertThatThrownBy(() -> placeQueryService.recommend(query()))
                        .isInstanceOf(PlaceSearchLimitExceededException.class);
                then(placeSearchCache).should().find(SEARCH_QUERY);
                then(placeSearchCache).shouldHaveNoMoreInteractions();
            }
        }

        @Nested
        @DisplayName("안주가 존재하지 않는 경우")
        class WhenAnjuNotFound {

            @Test
            @DisplayName("AnjuNotFoundException을 던지고 캐시와 외부 검색을 부르지 않는다")
            void shouldThrowAnjuNotFoundException() {
                // given
                given(getAnjuPlaceKeywordUseCase.getPlaceKeyword(ANJU_ID))
                        .willThrow(new AnjuNotFoundException());

                // when & then
                assertThatThrownBy(() -> placeQueryService.recommend(query()))
                        .isInstanceOf(AnjuNotFoundException.class);
                then(placeSearchCache).shouldHaveNoInteractions();
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
