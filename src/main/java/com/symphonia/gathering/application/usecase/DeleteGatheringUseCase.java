package com.symphonia.gathering.application.usecase;

import com.symphonia.gathering.application.dto.command.DeleteGatheringCommand;

public interface DeleteGatheringUseCase {
    void delete(DeleteGatheringCommand command);
}
