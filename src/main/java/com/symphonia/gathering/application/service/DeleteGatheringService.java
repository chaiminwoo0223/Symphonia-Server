package com.symphonia.gathering.application.service;

import com.symphonia.gathering.application.dto.command.DeleteGatheringCommand;
import com.symphonia.gathering.application.usecase.DeleteGatheringUseCase;
import com.symphonia.gathering.application.usecase.PurgeGatheringUseCase;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

// 모임장이 모임을 파기 시각 전에 지운다. 자동 파기와 같은 두 단계 절차를 쓰므로 각 단계를 따로 커밋해야 하고, 이 클래스는 트랜잭션을 열지 않는다.
// 데이터 키를 지운 뒤 2단계가 실패해도 모임은 파기된 것으로 보이고, 다음 파기 스윕이 남은 행을 지운다.
@Service
@RequiredArgsConstructor
public class DeleteGatheringService implements DeleteGatheringUseCase {
    private final PurgeGatheringUseCase purgeGatheringUseCase;

    @Override
    public void delete(DeleteGatheringCommand command) {
        Optional.of(command)
                .filter(
                        deletion ->
                                purgeGatheringUseCase.discardDataKeyOfHostedGathering(
                                        deletion.memberId(), deletion.gatheringId()))
                .ifPresent(
                        deletion ->
                                purgeGatheringUseCase.purgeWithoutSelectionSignal(
                                        deletion.gatheringId()));
    }
}
