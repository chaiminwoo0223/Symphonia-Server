package com.symphonia.place.domain.vo;

// 가게를 찾는 검색어다. 지역명과 안주의 맛집 검색어를 한 문장으로 이어 외부 검색에 넘긴다.
// 지역명의 연속 공백은 하나로 정리해 같은 검색어로 외부 검색을 호출한다.
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
