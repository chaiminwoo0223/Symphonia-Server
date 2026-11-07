package com.symphonia.gathering.infrastructure.jpa;

import com.symphonia.gathering.domain.repository.GatheringPairingRepository;
import com.symphonia.gathering.domain.vo.GatheringPairing;
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
                gatheringPairing.musicMoodId(),
                gatheringPairing.relationshipType().name(),
                gatheringPairing.moodType().name());
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
