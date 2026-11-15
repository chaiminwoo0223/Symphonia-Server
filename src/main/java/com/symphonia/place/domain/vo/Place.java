package com.symphonia.place.domain.vo;

// 검색 결과로 받은 가게다. 네이버 지역 검색은 가게 고유 id를 주지 않아 이름, 분류, 주소, 좌표, 링크만 갖는다.
// 약관상 DB에 저장하지 않고 짧게 캐시하기만 하므로 식별자를 두지 않는다.
public record Place(
        String name,
        String category,
        String address,
        String roadAddress,
        double latitude,
        double longitude,
        String link) {}
