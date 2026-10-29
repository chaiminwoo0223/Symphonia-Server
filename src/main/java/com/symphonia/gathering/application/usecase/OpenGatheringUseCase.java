package com.symphonia.gathering.application.usecase;

import com.symphonia.gathering.application.dto.command.OpenGatheringCommand;
import com.symphonia.gathering.application.dto.result.GatheringJoinTokenResult;

public interface OpenGatheringUseCase {
    GatheringJoinTokenResult open(OpenGatheringCommand command);
}
