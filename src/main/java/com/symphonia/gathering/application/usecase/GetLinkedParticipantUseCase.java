package com.symphonia.gathering.application.usecase;

import com.symphonia.gathering.application.dto.result.LinkedParticipantResult;

// 개인 링크 토큰으로 참여자를 찾는 유일한 경로다. 개인 링크로 모임 데이터를 읽거나 바꾸는 Service는 Repository 대신 이 UseCase로 참여자를 찾는다.
// 파기 시각이 지난 모임의 토큰은 찾지 못한 것으로 보고 LinkTokenNotFoundException을 던진다.
public interface GetLinkedParticipantUseCase {
    LinkedParticipantResult getLinkedParticipant(String linkToken);
}
