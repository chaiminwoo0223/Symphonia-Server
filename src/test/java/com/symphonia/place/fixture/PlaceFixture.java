package com.symphonia.place.fixture;

import com.symphonia.place.domain.vo.Place;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum PlaceFixture {
    GANGNAM_HOF(
            "강남 호프",
            "술집>맥주,호프",
            "서울특별시 강남구 역삼동 1",
            "서울특별시 강남구 테헤란로 1",
            37.5034081,
            127.0278735,
            "https://example.com/hof"),
    NONHYEON_PUB(
            "논현 요리주점",
            "술집>요리주점",
            "서울특별시 강남구 논현동 2",
            "서울특별시 강남구 논현로 2",
            37.5240289,
            127.0384976,
            "https://example.com/pub");

    private final String name;
    private final String category;
    private final String address;
    private final String roadAddress;
    private final double latitude;
    private final double longitude;
    private final String link;

    public Place create() {
        return new Place(name, category, address, roadAddress, latitude, longitude, link);
    }
}
