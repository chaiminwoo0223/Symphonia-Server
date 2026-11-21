package com.symphonia.gathering.infrastructure.jpa;

import com.symphonia.gathering.domain.entity.Settlement;
import com.symphonia.gathering.domain.repository.GatheringRetentionRepository;
import com.symphonia.gathering.domain.repository.SettlementRepository;
import com.symphonia.gathering.domain.vo.GatheringRetention;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class GatheringRetentionRepositoryImpl implements GatheringRetentionRepository {
    private final GatheringJpaRepository gatheringJpaRepository;
    private final ParticipantJpaRepository participantJpaRepository;
    private final GatheringDataKeyJpaRepository gatheringDataKeyJpaRepository;
    private final SettlementRepository settlementRepository;

    @Override
    public Optional<GatheringRetention> findByGatheringId(Long gatheringId) {
        return gatheringJpaRepository
                .findById(gatheringId)
                .map(
                        gatheringEntity ->
                                toRetention(
                                        gatheringEntity,
                                        settlementRepository.findAllByGatheringId(gatheringId)));
    }

    @Override
    public Optional<GatheringRetention> findByParticipantId(Long participantId) {
        return participantJpaRepository
                .findById(participantId)
                .flatMap(participant -> findByGatheringId(participant.getGatheringId()));
    }

    @Override
    public List<GatheringRetention> findAllOpenedUntil(LocalDateTime openedAt) {
        return toRetentions(
                gatheringJpaRepository.findAllByOpenedAtLessThanEqualOrderById(openedAt));
    }

    @Override
    public List<GatheringRetention> findAllByHostMemberId(Long hostMemberId) {
        return toRetentions(gatheringJpaRepository.findAllByHostMemberIdOrderById(hostMemberId));
    }

    @Override
    public Optional<GatheringRetention> findByGatheringIdForUpdate(Long gatheringId) {
        return gatheringJpaRepository
                .findByIdForUpdateSkipLocked(gatheringId)
                .map(
                        gatheringEntity ->
                                toRetention(
                                        gatheringEntity,
                                        settlementRepository.findAllByGatheringIdForUpdate(
                                                gatheringId)));
    }

    private GatheringRetention toRetention(
            GatheringJpaEntity gatheringEntity, List<Settlement> settlements) {
        return GatheringRetention.of(
                gatheringEntity.getId(),
                gatheringEntity.getOpenedAt(),
                gatheringEntity.isRecordingExtended(),
                settlements,
                !gatheringDataKeyJpaRepository.existsById(gatheringEntity.getId()));
    }

    // 모임마다 따로 읽지 않도록 정산 건과 데이터 키를 모임 id 전체로 한 번씩 읽는다.
    private List<GatheringRetention> toRetentions(List<GatheringJpaEntity> gatheringEntities) {
        List<Long> gatheringIds =
                gatheringEntities.stream().map(GatheringJpaEntity::getId).toList();
        Map<Long, List<Settlement>> settlementsByGatheringId =
                settlementRepository.findAllByGatheringIds(gatheringIds).stream()
                        .collect(Collectors.groupingBy(Settlement::getGatheringId));
        Set<Long> dataKeyGatheringIds =
                gatheringDataKeyJpaRepository.findAllById(gatheringIds).stream()
                        .map(GatheringDataKeyJpaEntity::getGatheringId)
                        .collect(Collectors.toSet());

        return gatheringEntities.stream()
                .map(
                        gatheringEntity ->
                                GatheringRetention.of(
                                        gatheringEntity.getId(),
                                        gatheringEntity.getOpenedAt(),
                                        gatheringEntity.isRecordingExtended(),
                                        settlementsByGatheringId.getOrDefault(
                                                gatheringEntity.getId(), List.of()),
                                        !dataKeyGatheringIds.contains(gatheringEntity.getId())))
                .toList();
    }
}
