package com.symphonia.gathering.infrastructure.jpa;

import com.symphonia.common.crypto.Sha256;
import com.symphonia.gathering.domain.repository.ParticipantLinkTokenRepository;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class ParticipantLinkTokenRepositoryImpl implements ParticipantLinkTokenRepository {
    private final ParticipantLinkTokenJpaRepository participantLinkTokenJpaRepository;

    @Override
    public void save(String value, Long participantId) {
        participantLinkTokenJpaRepository.save(
                ParticipantLinkTokenJpaEntity.of(participantId, Sha256.hash(value)));
    }

    @Override
    public Optional<Long> findParticipantIdByValue(String value) {
        return participantLinkTokenJpaRepository
                .findByTokenHash(Sha256.hash(value))
                .map(ParticipantLinkTokenJpaEntity::getParticipantId);
    }
}
