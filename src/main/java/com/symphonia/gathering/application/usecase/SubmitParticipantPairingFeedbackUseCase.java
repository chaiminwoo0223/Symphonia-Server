package com.symphonia.gathering.application.usecase;

import com.symphonia.gathering.application.dto.command.SubmitParticipantPairingFeedbackCommand;

public interface SubmitParticipantPairingFeedbackUseCase {
    void submit(SubmitParticipantPairingFeedbackCommand command);
}
