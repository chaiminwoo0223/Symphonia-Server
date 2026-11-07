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
        List<GatheringJpaEntity> gatheringEntities =
                gatheringJpaRepository.findAllByOpenedAtLessThanEqualOrderById(openedAt);
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
                                        settlementsByGatheringId.getOrDefault(
                                                gatheringEntity.getId(), List.of()),
                                        !dataKeyGatheringIds.contains(gatheringEntity.getId())))
                .toList();
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
                settlements,
                !gatheringDataKeyJpaRepository.existsById(gatheringEntity.getId()));
    }
}
