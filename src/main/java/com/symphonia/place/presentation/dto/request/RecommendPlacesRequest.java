package com.symphonia.place.presentation.dto.request;

import com.symphonia.place.application.dto.query.RecommendPlacesQuery;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

// 지역명은 외부 검색의 검색어에 그대로 들어가므로 한글, 숫자, 공백만 받는다. 임의의 문자열로 검색 한도를 쓰지 못하게 하기 위해서다.
public record RecommendPlacesRequest(
        @NotBlank
                @Pattern(
                        regexp = "^[가-힣0-9 ]{1,30}$",
                        message = "지역명은 한글, 숫자, 공백만 30자까지 입력할 수 있습니다.")
                @Schema(description = "지역명. 기기 위치를 구나 동 단위로 바꾼 값이나 사용자가 입력한 값", example = "강남구")
                String region,
        @NotNull @Schema(description = "맛집을 찾을 안주 ID") Long anjuId) {
    public RecommendPlacesQuery toQuery() {
        return RecommendPlacesQuery.of(region, anjuId);
    }
}
