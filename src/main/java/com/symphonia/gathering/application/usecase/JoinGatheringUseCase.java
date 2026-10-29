package com.symphonia.gathering.application.usecase;

import com.symphonia.gathering.application.dto.command.JoinGatheringCommand;
import com.symphonia.gathering.application.dto.result.ParticipantResult;

public interface JoinGatheringUseCase {
    ParticipantResult join(JoinGatheringCommand command);
}
