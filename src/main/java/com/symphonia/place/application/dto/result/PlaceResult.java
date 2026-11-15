package com.symphonia.place.application.dto.result;

import com.symphonia.place.domain.vo.Place;

public record PlaceResult(
        String name,
        String category,
        String address,
        String roadAddress,
        double latitude,
        double longitude,
        String link) {
    public static PlaceResult from(Place place) {
        return new PlaceResult(
                place.name(),
                place.category(),
                place.address(),
                place.roadAddress(),
                place.latitude(),
                place.longitude(),
                place.link());
    }
}
