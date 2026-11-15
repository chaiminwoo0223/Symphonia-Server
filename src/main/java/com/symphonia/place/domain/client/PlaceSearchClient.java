package com.symphonia.place.domain.client;

import com.symphonia.place.domain.vo.Place;
import com.symphonia.place.domain.vo.PlaceSearchQuery;
import java.util.List;

// 외부 검색이 돌려준 순서를 그대로 반환한다. 중복 제거나 재정렬은 하지 않는다.
// 외부 검색을 쓸 수 없으면 PlaceSearchUnavailableException을 던진다.
// 호출 한도를 넘으면 PlaceSearchLimitExceededException을 던진다.
public interface PlaceSearchClient {
    List<Place> search(PlaceSearchQuery query);
}
