package com.symphonia.pairing.domain.repository;

import com.symphonia.pairing.domain.entity.PairingFeedback;
import java.util.Collection;
import java.util.Set;

public interface PairingFeedbackRepository {
    PairingFeedback save(PairingFeedback pairingFeedback);

    Set<Long> findReferencedAnjuIds(Collection<Long> anjuIds);
}
