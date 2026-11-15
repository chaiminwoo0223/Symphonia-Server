package com.symphonia.place.domain.exception;

import com.symphonia.common.exception.ServiceUnavailableException;
import com.symphonia.place.domain.error.PlaceErrorCode;

public class PlaceSearchLimitExceededException extends ServiceUnavailableException {
    public PlaceSearchLimitExceededException() {
        super(PlaceErrorCode.PLACE_SEARCH_LIMIT_EXCEEDED);
    }
}
