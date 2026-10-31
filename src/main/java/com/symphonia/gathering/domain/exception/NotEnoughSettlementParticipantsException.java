package com.symphonia.gathering.domain.exception;

import com.symphonia.common.exception.BadRequestException;
import com.symphonia.gathering.domain.error.GatheringErrorCode;

public class NotEnoughSettlementParticipantsException extends BadRequestException {
    public NotEnoughSettlementParticipantsException() {
        super(GatheringErrorCode.NOT_ENOUGH_SETTLEMENT_PARTICIPANTS);
    }
}
