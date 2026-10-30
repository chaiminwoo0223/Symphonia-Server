package com.symphonia.gathering.infrastructure.jpa;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SettlementJpaRepository extends JpaRepository<SettlementJpaEntity, Long> {
    Optional<SettlementJpaEntity> findFirstByGatheringIdAndCancelledAtIsNullOrderByIdDesc(
            Long gatheringId);

    List<SettlementJpaEntity> findAllByPayerParticipantIdAndCancelledAtIsNullOrderById(
            Long payerParticipantId);
}
