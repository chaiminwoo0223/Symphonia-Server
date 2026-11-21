package com.symphonia.pairing.application.service;

import com.symphonia.common.annotation.CommandService;
import com.symphonia.pairing.application.dto.command.RecordPairingSelectionCommand;
import com.symphonia.pairing.application.usecase.RecordPairingSelectionUseCase;
import com.symphonia.pairing.domain.repository.PairingSelectionSummaryRepository;
import lombok.RequiredArgsConstructor;

// 모임장이 고른 조합은 모임을 파기할 때 함께 지워진다. 지우기 전에 상황별 선택 횟수에 1을 더한다.
// 카탈로그 참조를 확인하지 않는다. 대상이 사라졌어도 파기가 멈추면 안 되기 때문이다.
@CommandService
@RequiredArgsConstructor
public class RecordPairingSelectionService implements RecordPairingSelectionUseCase {
    private final PairingSelectionSummaryRepository pairingSelectionSummaryRepository;

    @Override
    public void record(RecordPairingSelectionCommand command) {
        pairingSelectionSummaryRepository.accumulate(
                command.drinkId(), command.anjuId(), command.toSituation());
    }
}
