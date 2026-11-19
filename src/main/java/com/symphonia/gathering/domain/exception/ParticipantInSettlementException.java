package com.symphonia.gathering.domain.exception;

import com.symphonia.common.exception.ConflictException;
import com.symphonia.gathering.domain.error.GatheringErrorCode;

public class ParticipantInSettlementException extends ConflictException {
    public ParticipantInSettlementException() {
        super(GatheringErrorCode.PARTICIPANT_IN_SETTLEMENT);
    }
}
