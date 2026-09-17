package com.symphonia.pairing.domain.exception;

import com.symphonia.common.exception.NotFoundException;
import com.symphonia.pairing.domain.error.PairingErrorCode;

public class MusicMoodNotFoundException extends NotFoundException {
    public MusicMoodNotFoundException() {
        super(PairingErrorCode.MUSIC_MOOD_NOT_FOUND);
    }
}
