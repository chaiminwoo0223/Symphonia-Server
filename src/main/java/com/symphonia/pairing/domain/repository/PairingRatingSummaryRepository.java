package com.symphonia.pairing.domain.repository;

import com.symphonia.pairing.domain.vo.PairingRatingChange;
import com.symphonia.pairing.domain.vo.PairingRatingSummary;
import java.util.List;

public interface PairingRatingSummaryRepository {
    void accumulate(Long drinkId, Long anjuId, Long musicMoodId, PairingRatingChange change);

    List<PairingRatingSummary> findAllWithSampleAtLeast(int minimumSample);
}
