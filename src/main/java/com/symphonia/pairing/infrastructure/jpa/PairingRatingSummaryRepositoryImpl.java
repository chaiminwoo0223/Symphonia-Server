package com.symphonia.pairing.infrastructure.jpa;

import com.symphonia.pairing.domain.repository.PairingRatingSummaryRepository;
import com.symphonia.pairing.domain.vo.PairingRatingChange;
import com.symphonia.pairing.domain.vo.PairingRatingSummary;
import com.symphonia.pairing.domain.vo.PairingSituation;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class PairingRatingSummaryRepositoryImpl implements PairingRatingSummaryRepository {
    private final PairingRatingSummaryJpaRepository pairingRatingSummaryJpaRepository;
    private final PairingSituationRatingSummaryJpaRepository
            pairingSituationRatingSummaryJpaRepository;

    @Override
    public void accumulate(Long drinkId, Long anjuId, PairingRatingChange change) {
        pairingRatingSummaryJpaRepository.accumulate(
                drinkId, anjuId, change.likeDelta(), change.dislikeDelta());
    }

    @Override
    public void accumulateInSituation(
            Long drinkId, Long anjuId, PairingSituation situation, PairingRatingChange change) {
        pairingSituationRatingSummaryJpaRepository.accumulate(
                situation.relationshipType().name(),
                situation.moodType() == null ? null : situation.moodType().name(),
                drinkId,
                anjuId,
                change.likeDelta(),
                change.dislikeDelta());
    }

    // 상황 집계만 있고 전역 표본이 모자란 조합은 담지 않는다.
    @Override
    public List<PairingRatingSummary> findAllWithSampleAtLeast(
            int minimumSample, PairingSituation situation) {
        return situation.moodType() == null
                ? pairingRatingSummaryJpaRepository.findAllWithSampleAtLeastInSituationWithoutMood(
                        minimumSample, situation.relationshipType())
                : pairingRatingSummaryJpaRepository.findAllWithSampleAtLeastInSituation(
                        minimumSample, situation.relationshipType(), situation.moodType());
    }
}
