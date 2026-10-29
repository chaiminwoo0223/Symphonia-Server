package com.symphonia.gathering.application.usecase;

import com.symphonia.gathering.application.dto.command.ReissueJoinTokenCommand;
import com.symphonia.gathering.application.dto.result.GatheringJoinTokenResult;

public interface ReissueJoinTokenUseCase {
    GatheringJoinTokenResult reissue(ReissueJoinTokenCommand command);
}
