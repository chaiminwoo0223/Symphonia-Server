package com.symphonia.gathering.domain.exception;

import com.symphonia.common.exception.BadRequestException;
import com.symphonia.gathering.domain.error.GatheringErrorCode;

public class InvalidSettlementParticipantsException extends BadRequestException {
    public InvalidSettlementParticipantsException() {
        super(GatheringErrorCode.INVALID_SETTLEMENT_PARTICIPANTS);
    }
}
