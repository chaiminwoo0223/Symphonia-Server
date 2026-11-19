package com.symphonia.gathering.domain.exception;

import com.symphonia.common.exception.ConflictException;
import com.symphonia.gathering.domain.error.GatheringErrorCode;

public class ParticipantUnremovableException extends ConflictException {
    public ParticipantUnremovableException() {
        super(GatheringErrorCode.PARTICIPANT_UNREMOVABLE);
    }
}
