package com.symphonia.gathering.infrastructure.jpa;

import com.symphonia.gathering.domain.entity.Settlement;
import com.symphonia.gathering.domain.exception.SettlementConflictException;
import com.symphonia.gathering.domain.repository.SettlementRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import jakarta.persistence.OptimisticLockException;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class SettlementRepositoryImpl implements SettlementRepository {
    private final SettlementJpaRepository settlementJpaRepository;
    private final SettlementShareJpaRepository settlementShareJpaRepository;
    private final EntityManager entityManager;

    // 저장할 때마다 기존 분담금을 지우고 도메인의 분담금을 다시 넣는다. 새 정산 건은 지울 분담금이 없어 0행을 지운다.
    // 분담금 상태 변경도 같은 방식으로 저장한다. 저장 경로를 하나로 두어 분담금 행만 고치는 경로가 따로 생기지 않게 한다.
    @Override
    public Settlement save(Settlement settlement) {
        SettlementJpaEntity savedSettlement = saveWithVersionCheck(settlement);
        Long settlementId = savedSettlement.getId();
        settlementShareJpaRepository.deleteAllBySettlementId(settlementId);
        List<SettlementShareJpaEntity> savedShares =
                settlementShareJpaRepository.saveAll(
                        settlement.getShares().stream()
                                .map(share -> SettlementShareJpaEntity.from(share, settlementId))
                                .toList());

        return savedSettlement.toDomain(
                savedShares.stream().map(SettlementShareJpaEntity::toDomain).toList());
    }

    @Override
    public Optional<Settlement> findById(Long settlementId) {
        return settlementJpaRepository.findById(settlementId).map(this::toDomain);
    }

    @Override
    public Optional<Settlement> findLatestByGatheringId(Long gatheringId) {
        return settlementJpaRepository
                .findFirstByGatheringIdAndCancelledAtIsNullOrderByIdDesc(gatheringId)
                .map(this::toDomain);
    }

    @Override
    public List<Settlement> findAllByPayerParticipantId(Long payerParticipantId) {
        return toDomains(
                settlementJpaRepository.findAllByPayerParticipantIdAndCancelledAtIsNullOrderById(
                        payerParticipantId));
    }

    @Override
    public List<Settlement> findAllByShareParticipantId(Long participantId) {
        return toDomains(settlementJpaRepository.findAllByShareParticipantId(participantId));
    }

    @Override
    public List<Settlement> findAllByShareParticipantIdAndPayerForUpdate(
            Long participantId, Long payerParticipantId) {
        return toDomains(
                settlementJpaRepository.findAllByShareParticipantIdAndPayerForUpdate(
                        participantId, payerParticipantId));
    }

    // 다른 요청이 먼저 바꾼 정산 건이면 버전이 달라 저장이 실패한다.
    // 커밋 시점에 실패하면 이 메서드가 예외를 바꿀 수 없으므로 바로 flush한다.
    // 분담금 행만 바뀌면 정산 건 행이 그대로라 Hibernate가 버전을 올리지 않으므로, 저장할 때마다 버전을 올린다.
    // 새 정산 건도 같은 경로로 버전을 올린다. 저장 경로를 새 정산 건과 기존 정산 건으로 나누지 않기 위해서다.
    // OPTIMISTIC_FORCE_INCREMENT는 커밋할 때 버전을 올리므로, 바로 올리는 PESSIMISTIC_FORCE_INCREMENT를 쓴다.
    private SettlementJpaEntity saveWithVersionCheck(Settlement settlement) {
        try {
            SettlementJpaEntity savedSettlement =
                    settlementJpaRepository.saveAndFlush(SettlementJpaEntity.from(settlement));
            entityManager.lock(savedSettlement, LockModeType.PESSIMISTIC_FORCE_INCREMENT);
            return savedSettlement;
        } catch (ObjectOptimisticLockingFailureException | OptimisticLockException e) {
            throw new SettlementConflictException();
        }
    }

    private Settlement toDomain(SettlementJpaEntity settlementEntity) {
        return settlementEntity.toDomain(
                settlementShareJpaRepository
                        .findAllBySettlementIdOrderById(settlementEntity.getId())
                        .stream()
                        .map(SettlementShareJpaEntity::toDomain)
                        .toList());
    }

    // 정산 건마다 분담금을 따로 조회하지 않도록 분담금을 한 번에 읽어 정산 건별로 나눈다.
    private List<Settlement> toDomains(List<SettlementJpaEntity> settlementEntities) {
        Map<Long, List<SettlementShareJpaEntity>> sharesBySettlementId =
                settlementShareJpaRepository
                        .findAllBySettlementIdInOrderById(
                                settlementEntities.stream()
                                        .map(SettlementJpaEntity::getId)
                                        .toList())
                        .stream()
                        .collect(Collectors.groupingBy(SettlementShareJpaEntity::getSettlementId));

        return settlementEntities.stream()
                .map(
                        settlementEntity ->
                                settlementEntity.toDomain(
                                        sharesBySettlementId
                                                .getOrDefault(settlementEntity.getId(), List.of())
                                                .stream()
                                                .map(SettlementShareJpaEntity::toDomain)
                                                .toList()))
                .toList();
    }
}
