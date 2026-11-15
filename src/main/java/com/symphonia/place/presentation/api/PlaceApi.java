package com.symphonia.place.presentation.api;

import com.symphonia.common.response.StandardResponse;
import com.symphonia.place.presentation.PlaceEndpoints;
import com.symphonia.place.presentation.dto.request.RecommendPlacesRequest;
import com.symphonia.place.presentation.dto.response.PlaceRecommendationResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;

@Tag(name = "Place API", description = "맛집 API")
public interface PlaceApi {
    @GetMapping(PlaceEndpoints.RECOMMEND)
    @Operation(
            summary = "맛집 추천 조회",
            description =
                    "지역명과 안주로 그 안주를 곁들일 수 있는 가게를 찾습니다. 로그인 없이 호출할 수 있습니다. "
                            + "결과는 네이버가 준 순서 그대로 최대 5건이고, 응답의 source와 각 가게의 link를 화면에 표시해야 합니다. "
                            + "지역명은 서버가 저장하지 않습니다. "
                            + "검색 결과도 서버에 저장하거나 캐시하지 않고 요청마다 네이버를 호출합니다. "
                            + "네이버 장애나 호출 한도 초과는 503으로 응답하고, 이때도 페어링 화면은 그대로 쓸 수 있습니다.")
    ResponseEntity<StandardResponse<PlaceRecommendationResponse>> recommend(
            @Valid @ModelAttribute RecommendPlacesRequest request);
}
