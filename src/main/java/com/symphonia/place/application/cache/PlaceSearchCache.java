package com.symphonia.place.application.cache;

import com.symphonia.place.domain.vo.Place;
import com.symphonia.place.domain.vo.PlaceSearchQuery;
import java.util.List;
import java.util.Optional;

// 외부 검색 결과를 하루 호출 한도를 아끼려고 짧게만 담아 둔다. 결과가 없던 검색도 빈 목록으로 담는다.
// 캐시 장애는 구현체가 삼키고, find는 캐시에 없을 때와 같은 빈 값을 돌려준다.
public interface PlaceSearchCache {
    Optional<List<Place>> find(PlaceSearchQuery query);

    void put(PlaceSearchQuery query, List<Place> places);
}
