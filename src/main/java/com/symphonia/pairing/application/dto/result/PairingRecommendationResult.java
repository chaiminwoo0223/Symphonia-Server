package com.symphonia.pairing.application.dto.result;

import com.symphonia.pairing.domain.vo.Pairing;
import com.symphonia.pairing.domain.vo.PairingNotice;
import java.util.List;

public record PairingRecommendationResult(
        List<PairingResult> pairings, List<PairingNoticeResult> notices) {
    public static PairingRecommendationResult of(
            List<Pairing> pairings, List<PairingNotice> notices) {
        return new PairingRecommendationResult(
                pairings.stream().map(PairingResult::from).toList(),
                notices.stream().map(PairingNoticeResult::from).toList());
    }
}
