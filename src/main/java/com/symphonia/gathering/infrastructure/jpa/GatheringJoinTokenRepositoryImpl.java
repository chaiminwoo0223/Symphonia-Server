package com.symphonia.gathering.infrastructure.jpa;

import com.symphonia.common.crypto.Sha256;
import com.symphonia.gathering.domain.repository.GatheringJoinTokenRepository;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class GatheringJoinTokenRepositoryImpl implements GatheringJoinTokenRepository {
    private final GatheringJoinTokenJpaRepository gatheringJoinTokenJpaRepository;

    @Override
    public void save(String value, Long gatheringId) {
        gatheringJoinTokenJpaRepository.save(
                GatheringJoinTokenJpaEntity.of(gatheringId, Sha256.hash(value)));
    }

    @Override
    public Optional<Long> findGatheringIdByValue(String value) {
        return gatheringJoinTokenJpaRepository
                .findByTokenHash(Sha256.hash(value))
                .map(GatheringJoinTokenJpaEntity::getGatheringId);
    }
}
