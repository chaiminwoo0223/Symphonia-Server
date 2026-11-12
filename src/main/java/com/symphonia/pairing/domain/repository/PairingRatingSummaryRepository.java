package com.symphonia.pairing.domain.repository;

import com.symphonia.pairing.domain.vo.PairingRatingChange;
import com.symphonia.pairing.domain.vo.PairingRatingSummary;
import com.symphonia.pairing.domain.vo.PairingSituation;
import java.util.List;

public interface PairingRatingSummaryRepository {
    void accumulate(Long drinkId, Long anjuId, PairingRatingChange change);

    void accumulateInSituation(
            Long drinkId, Long anjuId, PairingSituation situation, PairingRatingChange change);

    // 전역 표본이 기준 이상인 조합의 집계를 읽고, 요청한 상황에서 받은 수를 함께 담는다.
    List<PairingRatingSummary> findAllWithSampleAtLeast(
            int minimumSample, PairingSituation situation);
}
