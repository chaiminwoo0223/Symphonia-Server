package com.symphonia.gathering.application.usecase;

import com.symphonia.gathering.application.dto.command.ConfirmShareCommand;
import com.symphonia.gathering.application.dto.result.SettlementResult;

public interface ConfirmShareUseCase {
    SettlementResult confirm(ConfirmShareCommand command);
}
