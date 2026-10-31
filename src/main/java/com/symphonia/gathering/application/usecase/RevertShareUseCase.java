package com.symphonia.gathering.application.usecase;

import com.symphonia.gathering.application.dto.command.RevertShareCommand;
import com.symphonia.gathering.application.dto.result.SettlementResult;

public interface RevertShareUseCase {
    SettlementResult revert(RevertShareCommand command);
}
