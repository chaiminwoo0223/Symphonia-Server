package com.symphonia.pairing.application.usecase;

import com.symphonia.pairing.application.dto.query.RecommendPairingQuery;
import com.symphonia.pairing.application.dto.result.PairingResult;
import java.util.List;

public interface RecommendPairingUseCase {
    List<PairingResult> recommend(RecommendPairingQuery query);
}
