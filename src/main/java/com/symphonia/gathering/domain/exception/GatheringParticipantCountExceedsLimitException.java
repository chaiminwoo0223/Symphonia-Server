package com.symphonia.gathering.domain.exception;

import com.symphonia.common.exception.BadRequestException;
import com.symphonia.gathering.domain.error.GatheringErrorCode;

public class GatheringParticipantCountExceedsLimitException extends BadRequestException {
    public GatheringParticipantCountExceedsLimitException() {
        super(GatheringErrorCode.GATHERING_PARTICIPANT_COUNT_EXCEEDS_LIMIT);
    }
}
