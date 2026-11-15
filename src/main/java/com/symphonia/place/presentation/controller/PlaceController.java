package com.symphonia.place.presentation.controller;

import com.symphonia.common.response.StandardResponse;
import com.symphonia.place.application.dto.result.PlaceResult;
import com.symphonia.place.application.usecase.RecommendPlacesUseCase;
import com.symphonia.place.presentation.api.PlaceApi;
import com.symphonia.place.presentation.dto.request.RecommendPlacesRequest;
import com.symphonia.place.presentation.dto.response.PlaceRecommendationResponse;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class PlaceController implements PlaceApi {
    private final RecommendPlacesUseCase recommendPlacesUseCase;

    @Override
    public ResponseEntity<StandardResponse<PlaceRecommendationResponse>> recommend(
            RecommendPlacesRequest request) {
        List<PlaceResult> results = recommendPlacesUseCase.recommend(request.toQuery());
        PlaceRecommendationResponse response = PlaceRecommendationResponse.from(results);

        return ResponseEntity.status(HttpStatus.OK)
                .body(StandardResponse.success(HttpStatus.OK, response));
    }
}
