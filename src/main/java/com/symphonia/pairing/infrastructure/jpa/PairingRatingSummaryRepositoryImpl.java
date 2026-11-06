package com.symphonia.pairing.infrastructure.jpa;

import com.symphonia.pairing.domain.repository.PairingRatingSummaryRepository;
import com.symphonia.pairing.domain.vo.PairingRatingChange;
import com.symphonia.pairing.domain.vo.PairingRatingSummary;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class PairingRatingSummaryRepositoryImpl implements PairingRatingSummaryRepository {
    private final PairingRatingSummaryJpaRepository pairingRatingSummaryJpaRepository;

    @Override
    public void accumulate(
            Long drinkId, Long anjuId, Long musicMoodId, PairingRatingChange change) {
        pairingRatingSummaryJpaRepository.accumulate(
                drinkId, anjuId, musicMoodId, change.likeDelta(), change.dislikeDelta());
    }

    @Override
    public List<PairingRatingSummary> findAllWithSampleAtLeast(int minimumSample) {
        return pairingRatingSummaryJpaRepository.findAllWithSampleAtLeast(minimumSample).stream()
                .map(PairingRatingSummaryJpaEntity::toDomain)
                .toList();
    }
}
