package com.symphonia.gathering.application.usecase;

import com.symphonia.gathering.application.dto.command.JoinGatheringAsHostCommand;
import com.symphonia.gathering.application.dto.result.ParticipantResult;

public interface JoinGatheringAsHostUseCase {
    ParticipantResult joinAsHost(JoinGatheringAsHostCommand command);
}
