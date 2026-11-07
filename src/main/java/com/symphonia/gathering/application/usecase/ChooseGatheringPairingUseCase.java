package com.symphonia.gathering.application.usecase;

import com.symphonia.gathering.application.dto.command.ChooseGatheringPairingCommand;

public interface ChooseGatheringPairingUseCase {
    void choose(ChooseGatheringPairingCommand command);
}
