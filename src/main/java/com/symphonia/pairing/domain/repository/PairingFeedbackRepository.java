package com.symphonia.pairing.domain.repository;

import com.symphonia.pairing.domain.entity.PairingFeedback;
import java.util.Optional;

public interface PairingFeedbackRepository {
    PairingFeedback save(PairingFeedback pairingFeedback);

    // 회원이 같은 조합에 낸 피드백이 없을 때만 저장하고, 저장했으면 true를 반환한다.
    boolean saveIfAbsent(PairingFeedback pairingFeedback);

    Optional<PairingFeedback> findByMemberIdAndCombinationForUpdate(
            Long memberId, Long drinkId, Long anjuId, Long musicMoodId);
}
