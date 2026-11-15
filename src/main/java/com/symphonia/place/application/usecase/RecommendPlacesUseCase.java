package com.symphonia.place.application.usecase;

import com.symphonia.place.application.dto.query.RecommendPlacesQuery;
import com.symphonia.place.application.dto.result.PlaceResult;
import java.util.List;

public interface RecommendPlacesUseCase {
    List<PlaceResult> recommend(RecommendPlacesQuery query);
}
