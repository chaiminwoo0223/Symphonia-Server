package com.symphonia.gathering.application.usecase;

import com.symphonia.gathering.application.dto.command.JoinWithVerifiedSubjectCommand;
import com.symphonia.gathering.application.dto.result.ParticipantResult;

// 구글 ID 토큰 검증을 마친 요청을 받아 참여자를 저장한다. 검증은 외부 호출이라 이 UseCase 밖에서 끝낸다.
public interface JoinWithVerifiedSubjectUseCase {
    ParticipantResult join(JoinWithVerifiedSubjectCommand command);
}
