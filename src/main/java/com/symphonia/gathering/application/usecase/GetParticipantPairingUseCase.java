package com.symphonia.gathering.application.usecase;

import com.symphonia.gathering.application.dto.result.ParticipantPairingResult;

public interface GetParticipantPairingUseCase {
    ParticipantPairingResult getPairing(String linkToken);
}
