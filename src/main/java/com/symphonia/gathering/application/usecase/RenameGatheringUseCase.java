package com.symphonia.gathering.application.usecase;

import com.symphonia.gathering.application.dto.command.RenameGatheringCommand;

public interface RenameGatheringUseCase {
    void rename(RenameGatheringCommand command);
}
