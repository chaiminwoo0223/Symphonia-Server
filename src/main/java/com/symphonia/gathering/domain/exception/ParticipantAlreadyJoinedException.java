package com.symphonia.gathering.domain.exception;

import com.symphonia.common.exception.ConflictException;
import com.symphonia.gathering.domain.error.GatheringErrorCode;

public class ParticipantAlreadyJoinedException extends ConflictException {
    public ParticipantAlreadyJoinedException() {
        super(GatheringErrorCode.PARTICIPANT_ALREADY_JOINED);
    }
}
