package com.symphonia.pairing.domain.repository;

import com.symphonia.pairing.domain.entity.PairingFeedback;
import com.symphonia.pairing.domain.vo.CombinationId;
import java.util.List;
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

    // 회원이 낸 피드백을 최근에 낸 순서로 읽는다. beforeId가 있으면 그 id보다 먼저 낸 피드백만 읽고, null이면 가장 최근 것부터 읽는다.
    List<PairingFeedback> findAllByMemberIdBeforeId(Long memberId, Long beforeId, int limit);

    // 회원이 낸 모든 피드백의 회원 id를 비워 익명 피드백으로 만든다.
    void anonymizeByMemberId(Long memberId);
}
