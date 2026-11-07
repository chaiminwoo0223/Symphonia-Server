package com.symphonia.gathering.application.service;

import com.symphonia.common.annotation.QueryService;
import com.symphonia.gathering.application.dto.result.LinkedParticipantResult;
import com.symphonia.gathering.application.usecase.GetLinkedParticipantUseCase;
import com.symphonia.gathering.domain.exception.LinkTokenNotFoundException;
import com.symphonia.gathering.domain.repository.GatheringRetentionRepository;
import com.symphonia.gathering.domain.repository.ParticipantLinkTokenRepository;
import java.time.Clock;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;

// 파기 시각이 지난 모임의 링크는 물리 삭제 전이라도 찾지 못한 것으로 본다. 삭제 뒤의 응답과 같게 하기 위해서다.
// 파기 시각은 참여자 정보를 복호화하지 않고 판단하므로, 부르는 쪽이 모임을 읽기 전에 부르면 데이터 키를 지운 모임을 복호화하지 않는다.
@QueryService
@RequiredArgsConstructor
public class ParticipantQueryService implements GetLinkedParticipantUseCase {
    private final ParticipantLinkTokenRepository participantLinkTokenRepository;
    private final GatheringRetentionRepository gatheringRetentionRepository;
    private final Clock clock;

    @Override
    public LinkedParticipantResult getLinkedParticipant(String linkToken) {
        LocalDateTime now = LocalDateTime.now(clock);

        return participantLinkTokenRepository
                .findParticipantIdByValue(linkToken)
                .flatMap(
                        participantId ->
                                gatheringRetentionRepository
                                        .findByParticipantId(participantId)
                                        .filter(retention -> !retention.isExpired(now))
                                        .map(
                                                retention ->
                                                        LinkedParticipantResult.of(
                                                                participantId, retention)))
                .orElseThrow(LinkTokenNotFoundException::new);
    }
}
