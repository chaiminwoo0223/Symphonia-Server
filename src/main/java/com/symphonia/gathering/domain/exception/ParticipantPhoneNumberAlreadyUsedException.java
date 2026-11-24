package com.symphonia.gathering.domain.exception;

import com.symphonia.common.exception.ConflictException;
import com.symphonia.gathering.domain.error.GatheringErrorCode;

public class ParticipantPhoneNumberAlreadyUsedException extends ConflictException {
    public ParticipantPhoneNumberAlreadyUsedException() {
        super(GatheringErrorCode.PARTICIPANT_PHONE_NUMBER_ALREADY_USED);
    }
}
