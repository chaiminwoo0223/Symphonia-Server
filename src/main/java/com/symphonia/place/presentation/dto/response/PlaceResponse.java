package com.symphonia.place.presentation.dto.response;

import com.symphonia.place.application.dto.result.PlaceResult;
import io.swagger.v3.oas.annotations.media.Schema;

public record PlaceResponse(
        @Schema(description = "가게 이름") String name,
        @Schema(description = "가게 분류", example = "술집>맥주,호프") String category,
        @Schema(description = "지번 주소") String address,
        @Schema(description = "도로명 주소") String roadAddress,
        @Schema(description = "위도") double latitude,
        @Schema(description = "경도") double longitude,
        @Schema(description = "원본 링크") String link) {
    public static PlaceResponse from(PlaceResult result) {
        return new PlaceResponse(
                result.name(),
                result.category(),
                result.address(),
                result.roadAddress(),
                result.latitude(),
                result.longitude(),
                result.link());
    }
}
