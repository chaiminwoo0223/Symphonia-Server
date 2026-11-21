package com.symphonia.pairing.infrastructure.jpa;

import com.symphonia.pairing.domain.repository.PairingSelectionSummaryRepository;
import com.symphonia.pairing.domain.vo.PairingSituation;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class PairingSelectionSummaryRepositoryImpl implements PairingSelectionSummaryRepository {
    private final PairingSelectionSummaryJpaRepository pairingSelectionSummaryJpaRepository;

    @Override
    public void accumulate(Long drinkId, Long anjuId, PairingSituation situation) {
        pairingSelectionSummaryJpaRepository.accumulate(
                situation.relationshipType().name(),
                situation.moodType() == null ? null : situation.moodType().name(),
                drinkId,
                anjuId);
    }
}
