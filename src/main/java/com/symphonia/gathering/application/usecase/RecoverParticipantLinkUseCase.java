package com.symphonia.gathering.application.usecase;

import com.symphonia.gathering.application.dto.command.RecoverParticipantLinkCommand;
import com.symphonia.gathering.application.dto.result.ParticipantResult;

public interface RecoverParticipantLinkUseCase {
    ParticipantResult recover(RecoverParticipantLinkCommand command);
}
