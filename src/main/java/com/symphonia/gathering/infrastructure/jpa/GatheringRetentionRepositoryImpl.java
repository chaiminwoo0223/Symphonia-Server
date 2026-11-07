package com.symphonia.gathering.infrastructure.jpa;

import com.symphonia.gathering.domain.entity.Settlement;
import com.symphonia.gathering.domain.repository.GatheringRetentionRepository;
import com.symphonia.gathering.domain.repository.SettlementRepository;
import com.symphonia.gathering.domain.vo.GatheringRetention;
import java.time.LocalDateTime;
import java.util.Collection;
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
                                GatheringRetention.of(
                                        gatheringId,
                                        gatheringEntity.getOpenedAt(),
                                        settlementRepository.findAllByGatheringId(gatheringId),
                                        !gatheringDataKeyJpaRepository.existsById(gatheringId)));
    }

    @Override
    public Optional<GatheringRetention> findByParticipantId(Long participantId) {
        return participantJpaRepository
                .findById(participantId)
                .flatMap(participant -> findByGatheringId(participant.getGatheringId()));
    }

    @Override
    public List<Long> findGatheringIdsOpenedUntil(LocalDateTime openedAt) {
        return gatheringJpaRepository.findIdsOpenedUntil(openedAt);
    }

    // 모임마다 따로 조회하지 않도록 정산 건과 데이터 키를 한 번에 읽어 모임별로 나눈다.
    @Override
    public List<GatheringRetention> findAllByGatheringIdsForUpdate(Collection<Long> gatheringIds) {
        List<GatheringJpaEntity> gatheringEntities =
                gatheringJpaRepository.findAllByIdsForUpdateSkipLocked(gatheringIds);
        List<Long> lockedGatheringIds =
                gatheringEntities.stream().map(GatheringJpaEntity::getId).toList();
        if (lockedGatheringIds.isEmpty()) {
            return List.of();
        }
        Map<Long, List<Settlement>> settlementsByGatheringId =
                settlementRepository.findAllByGatheringIdsForUpdate(lockedGatheringIds).stream()
                        .collect(Collectors.groupingBy(Settlement::getGatheringId));
        Set<Long> dataKeyGatheringIds =
                gatheringDataKeyJpaRepository.findAllById(lockedGatheringIds).stream()
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
}
