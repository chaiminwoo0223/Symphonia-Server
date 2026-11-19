package com.symphonia.gathering.application.usecase;

import com.symphonia.gathering.application.dto.command.ReissueParticipantLinkTokenCommand;
import com.symphonia.gathering.application.dto.result.ParticipantResult;

public interface ReissueParticipantLinkTokenUseCase {
    ParticipantResult reissue(ReissueParticipantLinkTokenCommand command);
}
