package com.symphonia.place.presentation.dto.response;

import com.symphonia.place.application.dto.result.PlaceResult;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

public record PlaceRecommendationResponse(
        @Schema(description = "검색 결과의 출처. 화면에 함께 표시해야 합니다.") String source,
        @Schema(description = "검색 결과 순서 그대로의 가게 목록. 결과가 없으면 빈 목록입니다.") List<PlaceResponse> places) {
    private static final String SOURCE = "네이버 검색결과";

    public static PlaceRecommendationResponse from(List<PlaceResult> results) {
        return new PlaceRecommendationResponse(
                SOURCE, results.stream().map(PlaceResponse::from).toList());
    }
}
