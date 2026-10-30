package com.symphonia.gathering.application.usecase;

import com.symphonia.gathering.application.dto.command.RecordSettlementCommand;
import com.symphonia.gathering.application.dto.result.SettlementResult;

public interface RecordSettlementUseCase {
    SettlementResult record(RecordSettlementCommand command);
}
