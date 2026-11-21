package com.symphonia.gathering.application.service;

import com.symphonia.common.annotation.CommandService;
import com.symphonia.gathering.application.usecase.PurgeGatheringUseCase;
import com.symphonia.gathering.domain.entity.Settlement;
import com.symphonia.gathering.domain.policy.GatheringPolicy;
import com.symphonia.gathering.domain.repository.GatheringJoinTokenRepository;
import com.symphonia.gathering.domain.repository.GatheringPairingRepository;
import com.symphonia.gathering.domain.repository.GatheringRepository;
import com.symphonia.gathering.domain.repository.GatheringRetentionRepository;
import com.symphonia.gathering.domain.repository.ParticipantLinkTokenRepository;
import com.symphonia.gathering.domain.repository.ParticipantPairingFeedbackRepository;
import com.symphonia.gathering.domain.repository.SettlementRepository;
import com.symphonia.gathering.domain.vo.GatheringPairing;
import com.symphonia.gathering.domain.vo.GatheringRetention;
import com.symphonia.pairing.application.dto.command.RecordPairingSelectionCommand;
import com.symphonia.pairing.application.usecase.RecordPairingSelectionUseCase;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Optional;
import lombok.RequiredArgsConstructor;

// 모임 개인정보의 파기 시각이 지나면 데이터 키를 먼저 지우고, 그다음 모임의 행을 모두 지운다(#128).
// 데이터 키를 지울 때는 모임과 정산 건만 잠근다.
// 데이터 키를 지운 모임은 조회할 때 파기된 것으로 보므로, 그 뒤에 정산 건이 바뀌어도 파기는 되돌리지 않는다.
@CommandService
@RequiredArgsConstructor
public class PurgeGatheringService implements PurgeGatheringUseCase {
    private final GatheringRetentionRepository gatheringRetentionRepository;
    private final GatheringRepository gatheringRepository;
    private final GatheringJoinTokenRepository gatheringJoinTokenRepository;
    private final ParticipantLinkTokenRepository participantLinkTokenRepository;
    private final ParticipantPairingFeedbackRepository participantPairingFeedbackRepository;
    private final GatheringPairingRepository gatheringPairingRepository;
    private final SettlementRepository settlementRepository;
    private final RecordPairingSelectionUseCase recordPairingSelectionUseCase;
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

    // 모임장이 지울 때의 1단계다. 모임을 먼저 잠그고 정산 건을 잠가, 자동 파기와 잠그는 순서가 같다.
    // 모임이 없으면 이미 지운 것이므로 false를 돌려주고, 데이터 키만 지운 모임은 지우던 중이므로 이어서 true를 돌려준다.
    @Override
    public boolean discardDataKeyOfHostedGathering(Long hostMemberId, Long gatheringId) {
        Optional<Long> actualHostMemberId =
                gatheringRepository.findHostMemberIdByIdForUpdate(gatheringId);
        actualHostMemberId.ifPresent(
                actualHost -> {
                    GatheringPolicy.validateHost(actualHost, hostMemberId);
                    GatheringPolicy.validateDeletable(
                            settlementRepository.findAllByGatheringIdForUpdate(gatheringId).stream()
                                    .anyMatch(Settlement::hasSentShares));
                    gatheringRepository.discardDataKey(gatheringId);
                });

        return actualHostMemberId.isPresent();
    }

    // 모임 페어링은 마지막으로 고른 조합 하나를 읽어 두고, 이 호출이 페어링을 지웠을 때만 선택 신호로 남긴다.
    // 같은 모임의 파기가 겹쳐도 먼저 지운 쪽만 세므로 선택 횟수가 두 번 더해지지 않는다.
    // 같은 트랜잭션이라 파기가 롤백되면 선택 횟수도 롤백된다.
    @Override
    public void purge(Long gatheringId) {
        Optional<GatheringPairing> chosen =
                gatheringPairingRepository.findByGatheringId(gatheringId);
        boolean pairingDeleted = deleteRows(gatheringId);
        chosen.filter(_ -> pairingDeleted).ifPresent(this::recordSelection);
    }

    @Override
    public void purgeWithoutSelectionSignal(Long gatheringId) {
        deleteRows(gatheringId);
    }

    // 참조하는 쪽부터 테이블마다 한 번씩 지운다. 참여자 수와 관계없이 쿼리 수가 같다. 모임 페어링을 지웠으면 true다.
    private boolean deleteRows(Long gatheringId) {
        participantLinkTokenRepository.deleteAllByGatheringId(gatheringId);
        participantPairingFeedbackRepository.deleteAllByGatheringId(gatheringId);
        settlementRepository.deleteAllByGatheringId(gatheringId);
        gatheringJoinTokenRepository.deleteAllByGatheringId(gatheringId);
        boolean pairingDeleted = gatheringPairingRepository.deleteByGatheringId(gatheringId);
        gatheringRepository.delete(gatheringId);

        return pairingDeleted;
    }

    private void recordSelection(GatheringPairing gatheringPairing) {
        recordPairingSelectionUseCase.record(
                RecordPairingSelectionCommand.of(
                        gatheringPairing.drinkId(),
                        gatheringPairing.anjuId(),
                        gatheringPairing.relationshipType(),
                        gatheringPairing.moodType()));
    }
}
