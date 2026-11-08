package com.symphonia.gathering.infrastructure.jpa;

import com.symphonia.gathering.domain.repository.GatheringPairingRepository;
import com.symphonia.gathering.domain.vo.GatheringPairing;
import com.symphonia.pairing.domain.vo.MoodType;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class GatheringPairingRepositoryImpl implements GatheringPairingRepository {
    private final GatheringPairingJpaRepository gatheringPairingJpaRepository;

    @Override
    public void save(Long gatheringId, GatheringPairing gatheringPairing) {
        gatheringPairingJpaRepository.upsert(
                gatheringId,
                gatheringPairing.drinkId(),
                gatheringPairing.anjuId(),
                gatheringPairing.relationshipType().name(),
                Optional.ofNullable(gatheringPairing.moodType()).map(MoodType::name).orElse(null));
    }

    @Override
    public Optional<GatheringPairing> findByParticipantId(Long participantId) {
        return gatheringPairingJpaRepository
                .findByParticipantId(participantId)
                .map(GatheringPairingJpaEntity::toDomain);
    }

    @Override
    public void deleteByGatheringId(Long gatheringId) {
        gatheringPairingJpaRepository.deleteByGatheringId(gatheringId);
    }
}
