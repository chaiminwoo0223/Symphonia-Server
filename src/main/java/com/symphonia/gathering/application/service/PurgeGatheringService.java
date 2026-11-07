package com.symphonia.gathering.application.service;

import com.symphonia.common.annotation.CommandService;
import com.symphonia.gathering.application.usecase.PurgeGatheringsUseCase;
import com.symphonia.gathering.domain.repository.GatheringJoinTokenRepository;
import com.symphonia.gathering.domain.repository.GatheringRepository;
import com.symphonia.gathering.domain.repository.GatheringRetentionRepository;
import com.symphonia.gathering.domain.repository.ParticipantBankAccountRepository;
import com.symphonia.gathering.domain.repository.ParticipantLinkTokenRepository;
import com.symphonia.gathering.domain.repository.SettlementRepository;
import com.symphonia.gathering.domain.vo.GatheringRetention;
import com.symphonia.notification.application.usecase.PurgeNotificationsUseCase;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Optional;
import lombok.RequiredArgsConstructor;

// 모임 개인정보의 파기 시각이 지나면 데이터 키를 먼저 지우고, 그다음 모임의 행을 모두 지운다(#128).
// 데이터 키를 지울 때는 모임과 정산 건만 잠근다. 정산 건을 바꾸는 흐름은 발송 예약을 잠근 뒤 정산 건을 잠그므로 서로 기다리는 순서가 생기지 않는다.
// 행을 지울 때는 발송 예약을 먼저 지우고 정산 건을 지워, 정산 건을 바꾸는 흐름과 같은 순서로 잠근다.
// 데이터 키를 지운 모임은 조회할 때 파기된 것으로 보므로, 그 뒤에 정산 건이 바뀌어도 파기는 되돌리지 않는다.
@CommandService
@RequiredArgsConstructor
public class PurgeGatheringService implements PurgeGatheringsUseCase {
    private final GatheringRetentionRepository gatheringRetentionRepository;
    private final GatheringRepository gatheringRepository;
    private final GatheringJoinTokenRepository gatheringJoinTokenRepository;
    private final ParticipantLinkTokenRepository participantLinkTokenRepository;
    private final ParticipantBankAccountRepository participantBankAccountRepository;
    private final SettlementRepository settlementRepository;
    private final PurgeNotificationsUseCase purgeNotificationsUseCase;
    private final Clock clock;

    // 고른 뒤 분담금이 미입금으로 되돌아갔을 수 있으므로, 잠근 뒤 파기 시각을 다시 판단한다.
    @Override
    public boolean discardDataKeyIfExpired(Long gatheringId) {
        LocalDateTime now = LocalDateTime.now(clock);
        Optional<GatheringRetention> expired =
                gatheringRetentionRepository
                        .findByGatheringIdForUpdate(gatheringId)
                        .filter(retention -> retention.isExpired(now));
        expired.ifPresent(_ -> gatheringRepository.discardDataKey(gatheringId));

        return expired.isPresent();
    }

    // 참조하는 쪽부터 테이블마다 한 번씩 지운다. 참여자 수와 관계없이 쿼리 수가 같다.
    @Override
    public void purge(Long gatheringId) {
        purgeNotificationsUseCase.purge(gatheringId);
        participantLinkTokenRepository.deleteAllByGatheringId(gatheringId);
        participantBankAccountRepository.deleteAllByGatheringId(gatheringId);
        settlementRepository.deleteAllByGatheringId(gatheringId);
        gatheringJoinTokenRepository.deleteAllByGatheringId(gatheringId);
        gatheringRepository.delete(gatheringId);
    }
}
