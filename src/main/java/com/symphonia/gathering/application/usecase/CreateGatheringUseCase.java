package com.symphonia.gathering.application.usecase;

import com.symphonia.gathering.application.dto.command.CreateGatheringCommand;
import com.symphonia.gathering.application.dto.result.GatheringJoinTokenResult;

public interface CreateGatheringUseCase {
    GatheringJoinTokenResult create(CreateGatheringCommand command);
}
