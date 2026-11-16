package com.symphonia.gathering.application.usecase;

import com.symphonia.gathering.application.dto.command.AddRoundCommand;
import com.symphonia.gathering.application.dto.result.GatheringRoundResult;

public interface AddRoundUseCase {
    GatheringRoundResult add(AddRoundCommand command);
}
