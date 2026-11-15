package com.symphonia.place.application.service;

import com.symphonia.catalog.application.usecase.GetAnjuPlaceKeywordUseCase;
import com.symphonia.common.annotation.QueryService;
import com.symphonia.place.application.cache.PlaceSearchCache;
import com.symphonia.place.application.dto.query.RecommendPlacesQuery;
import com.symphonia.place.application.dto.result.PlaceResult;
import com.symphonia.place.application.usecase.RecommendPlacesUseCase;
import com.symphonia.place.domain.client.PlaceSearchClient;
import com.symphonia.place.domain.vo.Place;
import com.symphonia.place.domain.vo.PlaceSearchQuery;
import java.util.List;
import lombok.RequiredArgsConstructor;

@QueryService
@RequiredArgsConstructor
public class PlaceQueryService implements RecommendPlacesUseCase {
    private final GetAnjuPlaceKeywordUseCase getAnjuPlaceKeywordUseCase;
    private final PlaceSearchCache placeSearchCache;
    private final PlaceSearchClient placeSearchClient;

    // 검색 결과는 외부 검색이 돌려준 순서 그대로 내려준다. 약관이 가공과 재정렬을 금지하기 때문이다.
    @Override
    public List<PlaceResult> recommend(RecommendPlacesQuery query) {
        PlaceSearchQuery searchQuery =
                PlaceSearchQuery.of(
                        query.region(),
                        getAnjuPlaceKeywordUseCase.getPlaceKeyword(query.anjuId()).placeKeyword());

        return placeSearchCache.find(searchQuery).orElseGet(() -> search(searchQuery)).stream()
                .map(PlaceResult::from)
                .toList();
    }

    // 캐시에 없을 때만 외부 검색을 부른다. 하루 호출 한도를 아끼기 위해 빈 결과도 캐시에 담는다.
    private List<Place> search(PlaceSearchQuery searchQuery) {
        List<Place> places = placeSearchClient.search(searchQuery);
        placeSearchCache.put(searchQuery, places);

        return places;
    }
}
