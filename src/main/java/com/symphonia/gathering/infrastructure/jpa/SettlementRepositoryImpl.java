package com.symphonia.gathering.infrastructure.jpa;

import com.symphonia.gathering.domain.entity.Settlement;
import com.symphonia.gathering.domain.exception.SettlementConflictException;
import com.symphonia.gathering.domain.repository.SettlementRepository;
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

    // 저장할 때마다 기존 분담을 지우고 도메인의 분담을 다시 넣는다. 새 정산 건은 지울 분담이 없어 0행을 지운다.
    // 분담 행만 바꾸는 저장 방식은 입금 상태 전이와 함께 #125에서 정한다.
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

    // 정산 건마다 분담을 따로 조회하지 않도록 분담을 한 번에 읽어 정산 건별로 나눈다.
    @Override
    public List<Settlement> findAllByPayerParticipantId(Long payerParticipantId) {
        List<SettlementJpaEntity> settlementEntities =
                settlementJpaRepository.findAllByPayerParticipantIdAndCancelledAtIsNullOrderById(
                        payerParticipantId);
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

    // 다른 요청이 먼저 바꾼 정산 건이면 버전이 달라 저장이 실패한다.
    // 커밋 시점에 실패하면 이 메서드가 예외를 바꿀 수 없으므로 바로 flush한다.
    private SettlementJpaEntity saveWithVersionCheck(Settlement settlement) {
        try {
            return settlementJpaRepository.saveAndFlush(SettlementJpaEntity.from(settlement));
        } catch (ObjectOptimisticLockingFailureException e) {
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
}
