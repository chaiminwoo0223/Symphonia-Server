package com.symphonia.pairing.application.usecase;

import com.symphonia.pairing.application.dto.query.RecommendPairingQuery;
import com.symphonia.pairing.application.dto.result.PairingRecommendationResult;

public interface RecommendPairingUseCase {
    PairingRecommendationResult recommend(RecommendPairingQuery query);
}
