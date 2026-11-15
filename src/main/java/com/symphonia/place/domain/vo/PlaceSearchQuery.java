package com.symphonia.place.domain.vo;

// 가게를 찾는 검색어다. 지역명과 안주의 맛집 검색어를 한 문장으로 이어 외부 검색에 넘긴다.
// 같은 지역을 공백만 다르게 보낸 요청이 캐시를 나누지 않도록 지역명의 공백을 정리한다.
public record PlaceSearchQuery(String region, String keyword) {
    public PlaceSearchQuery {
        region = region.strip().replaceAll("\\s+", " ");
    }

    public static PlaceSearchQuery of(String region, String keyword) {
        return new PlaceSearchQuery(region, keyword);
    }

    public String text() {
        return region + " " + keyword;
    }
}
