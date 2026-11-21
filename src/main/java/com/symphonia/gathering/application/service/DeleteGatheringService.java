package com.symphonia.gathering.application.service;

import com.symphonia.common.annotation.CommandService;
import com.symphonia.gathering.application.dto.command.DeleteGatheringCommand;
import com.symphonia.gathering.application.usecase.DeleteGatheringUseCase;
import com.symphonia.gathering.application.usecase.PurgeGatheringUseCase;
import lombok.RequiredArgsConstructor;

// 모임장이 모임을 파기 시각 전에 지운다. 데이터 키를 지우는 것과 나머지 행을 지우는 것을 한 트랜잭션에 묶는다.
// 자동 파기와 달리 둘을 따로 커밋하지 않는다. 중간에 실패하면 모두 되돌아가 모임이 그대로 남고, 모임장이 다시 요청하면 된다.
// 따로 커밋하면 2단계가 실패했을 때 다음 파기 스윕이 연 지 9일이 지난 모임만 읽어서 행이 한동안 남는다.
@CommandService
@RequiredArgsConstructor
public class DeleteGatheringService implements DeleteGatheringUseCase {
    private final PurgeGatheringUseCase purgeGatheringUseCase;

    @Override
    public void delete(DeleteGatheringCommand command) {
        if (purgeGatheringUseCase.discardDataKeyOfHostedGathering(
                command.memberId(), command.gatheringId())) {
            purgeGatheringUseCase.purgeWithoutSelectionSignal(command.gatheringId());
        }
    }
}
