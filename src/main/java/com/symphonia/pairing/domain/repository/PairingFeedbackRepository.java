package com.symphonia.pairing.domain.repository;

import com.symphonia.pairing.domain.entity.PairingFeedback;
import com.symphonia.pairing.domain.vo.CombinationId;
import java.util.Optional;
import java.util.Set;

public interface PairingFeedbackRepository {
    PairingFeedback save(PairingFeedback pairingFeedback);

    // 회원이 같은 조합에 낸 피드백이 없을 때만 저장하고, 저장했으면 true를 반환한다.
    boolean saveIfAbsent(PairingFeedback pairingFeedback);

    Optional<PairingFeedback> findByMemberIdAndCombinationForUpdate(
            Long memberId, Long drinkId, Long anjuId);

    // 회원이 싫어요를 남긴 음료×안주 조합이다. 추천에서 제외할 조합을 구할 때 쓴다.
    Set<CombinationId> findDislikedCombinationsByMemberId(Long memberId);
}
