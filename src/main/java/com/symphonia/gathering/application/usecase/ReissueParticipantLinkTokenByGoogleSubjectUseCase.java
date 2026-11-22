package com.symphonia.gathering.application.usecase;

import com.symphonia.gathering.application.dto.command.ReissueParticipantLinkTokenByGoogleSubjectCommand;
import com.symphonia.gathering.application.dto.result.ParticipantResult;

public interface ReissueParticipantLinkTokenByGoogleSubjectUseCase {
    ParticipantResult reissueByGoogleSubject(
            ReissueParticipantLinkTokenByGoogleSubjectCommand command);
}
