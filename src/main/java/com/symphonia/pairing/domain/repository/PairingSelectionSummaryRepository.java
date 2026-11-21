package com.symphonia.pairing.domain.repository;

import com.symphonia.pairing.domain.vo.PairingSituation;

public interface PairingSelectionSummaryRepository {
    // 상황별 조합의 선택 횟수에 1을 더한다. 행이 없으면 1로 만든다.
    void accumulate(Long drinkId, Long anjuId, PairingSituation situation);
}
