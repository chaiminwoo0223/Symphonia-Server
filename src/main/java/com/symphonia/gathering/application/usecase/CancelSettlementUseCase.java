package com.symphonia.gathering.application.usecase;

import com.symphonia.gathering.application.dto.command.CancelSettlementCommand;

public interface CancelSettlementUseCase {
    void cancel(CancelSettlementCommand command);
}
