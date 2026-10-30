package com.symphonia.gathering.application.usecase;

import com.symphonia.gathering.application.dto.command.UpdateSettlementCommand;
import com.symphonia.gathering.application.dto.result.SettlementResult;

public interface UpdateSettlementUseCase {
    SettlementResult update(UpdateSettlementCommand command);
}
