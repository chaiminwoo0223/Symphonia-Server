package com.symphonia.gathering.application.usecase;

import com.symphonia.gathering.application.dto.command.MarkSharesSentCommand;
import com.symphonia.gathering.application.dto.result.PayerSharesResult;

public interface MarkSharesSentUseCase {
    PayerSharesResult markSent(MarkSharesSentCommand command);
}
