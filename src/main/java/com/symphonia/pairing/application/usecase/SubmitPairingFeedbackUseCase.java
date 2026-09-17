package com.symphonia.pairing.application.usecase;

import com.symphonia.pairing.application.dto.command.SubmitPairingFeedbackCommand;
import com.symphonia.pairing.application.dto.result.PairingFeedbackResult;

public interface SubmitPairingFeedbackUseCase {
    PairingFeedbackResult submit(SubmitPairingFeedbackCommand command);
}
