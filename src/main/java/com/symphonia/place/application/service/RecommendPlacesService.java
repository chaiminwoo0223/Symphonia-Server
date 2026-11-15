package com.symphonia.place.application.service;

import com.symphonia.catalog.application.usecase.GetAnjuPlaceKeywordUseCase;
import com.symphonia.place.application.dto.query.RecommendPlacesQuery;
import com.symphonia.place.application.dto.result.PlaceResult;
import com.symphonia.place.application.usecase.RecommendPlacesUseCase;
import com.symphonia.place.domain.client.PlaceSearchClient;
import com.symphonia.place.domain.vo.PlaceSearchQuery;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class RecommendPlacesService implements RecommendPlacesUseCase {
    private final GetAnjuPlaceKeywordUseCase getAnjuPlaceKeywordUseCase;
    private final PlaceSearchClient placeSearchClient;

    // 검색 결과는 외부 검색이 돌려준 순서 그대로 내려준다. 약관이 가공과 재정렬을 금지하기 때문이다.
    // 약관이 서버의 검색 결과 보관을 이력 조회와 개인화 목적으로만 허용하므로 캐시하지 않는다.
    // 그래서 요청마다 외부 검색을 호출한다.
    @Override
    public List<PlaceResult> recommend(RecommendPlacesQuery query) {
        PlaceSearchQuery searchQuery =
                PlaceSearchQuery.of(
                        query.region(),
                        getAnjuPlaceKeywordUseCase.getPlaceKeyword(query.anjuId()).placeKeyword());

        return placeSearchClient.search(searchQuery).stream().map(PlaceResult::from).toList();
    }
}
