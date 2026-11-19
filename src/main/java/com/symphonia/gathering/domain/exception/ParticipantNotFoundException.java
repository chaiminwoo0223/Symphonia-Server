package com.symphonia.gathering.domain.exception;

import com.symphonia.common.exception.NotFoundException;
import com.symphonia.gathering.domain.error.GatheringErrorCode;

public class ParticipantNotFoundException extends NotFoundException {
    public ParticipantNotFoundException() {
        super(GatheringErrorCode.PARTICIPANT_NOT_FOUND);
    }
}
