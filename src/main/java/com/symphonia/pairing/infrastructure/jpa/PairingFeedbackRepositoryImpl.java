package com.symphonia.pairing.infrastructure.jpa;

import com.symphonia.pairing.domain.entity.PairingFeedback;
import com.symphonia.pairing.domain.repository.PairingFeedbackRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class PairingFeedbackRepositoryImpl implements PairingFeedbackRepository {
    private final PairingFeedbackJpaRepository pairingFeedbackJpaRepository;

    @Override
    public PairingFeedback save(PairingFeedback pairingFeedback) {
        PairingFeedbackJpaEntity savedEntity =
                pairingFeedbackJpaRepository.save(PairingFeedbackJpaEntity.from(pairingFeedback));

        return savedEntity.toDomain();
    }
}
