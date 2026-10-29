package com.symphonia.gathering.infrastructure.jpa;

import com.symphonia.gathering.domain.entity.Gathering;
import com.symphonia.gathering.domain.exception.GatheringDataKeyNotFoundException;
import com.symphonia.gathering.domain.policy.GatheringPolicy;
import com.symphonia.gathering.domain.repository.GatheringRepository;
import com.symphonia.gathering.infrastructure.crypto.GatheringDataKey;
import com.symphonia.gathering.infrastructure.crypto.GatheringMasterKey;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class GatheringRepositoryImpl implements GatheringRepository {
    private final GatheringJpaRepository gatheringJpaRepository;
    private final ParticipantJpaRepository participantJpaRepository;
    private final GatheringDataKeyJpaRepository gatheringDataKeyJpaRepository;
    private final GatheringMasterKey gatheringMasterKey;

    @Override
    public Gathering save(Gathering gathering) {
        GatheringJpaEntity savedGathering =
                gatheringJpaRepository.save(GatheringJpaEntity.from(gathering));
        Long gatheringId = savedGathering.getId();
        GatheringDataKey dataKey =
                gathering.getId() == null ? createDataKey(gatheringId) : findDataKey(gatheringId);
        List<ParticipantJpaEntity> participantEntities =
                gathering.getParticipants().stream()
                        .map(
                                participant ->
                                        ParticipantJpaEntity.from(
                                                participant, gatheringId, dataKey))
                        .toList();

        try {
            List<ParticipantJpaEntity> savedParticipants =
                    participantJpaRepository.saveAll(participantEntities);

            return toDomain(savedGathering, savedParticipants, dataKey);
        } catch (DataIntegrityViolationException e) {
            GatheringPolicy.validateNotJoined(isPhoneNumberViolation(e));
            throw e;
        }
    }

    @Override
    public Optional<Gathering> findById(Long gatheringId) {
        return gatheringJpaRepository.findById(gatheringId).map(this::toDomain);
    }

    private GatheringDataKey createDataKey(Long gatheringId) {
        GatheringDataKey dataKey = GatheringDataKey.generate();
        gatheringDataKeyJpaRepository.save(
                GatheringDataKeyJpaEntity.of(gatheringId, gatheringMasterKey.wrap(dataKey)));

        return dataKey;
    }

    private GatheringDataKey findDataKey(Long gatheringId) {
        GatheringDataKeyJpaEntity dataKeyEntity =
                gatheringDataKeyJpaRepository
                        .findById(gatheringId)
                        .orElseThrow(GatheringDataKeyNotFoundException::new);

        return gatheringMasterKey.unwrap(dataKeyEntity.toWrappedDataKey());
    }

    private Gathering toDomain(
            GatheringJpaEntity gatheringEntity,
            List<ParticipantJpaEntity> participantEntities,
            GatheringDataKey dataKey) {
        return gatheringEntity.toDomain(
                participantEntities.stream()
                        .map(participantEntity -> participantEntity.toDomain(dataKey))
                        .toList());
    }

    // 같은 휴대폰으로 동시에 참여하면 Service의 중복 검사를 둘 다 통과하므로 unique 제약 위반으로 구분한다.
    private boolean isPhoneNumberViolation(DataIntegrityViolationException e) {
        return e.getCause() instanceof ConstraintViolationException violation
                && "uk_participant_phone_number".equalsIgnoreCase(violation.getConstraintName());
    }

    private Gathering toDomain(GatheringJpaEntity gatheringEntity) {
        Long gatheringId = gatheringEntity.getId();

        return toDomain(
                gatheringEntity,
                participantJpaRepository.findAllByGatheringIdOrderById(gatheringId),
                findDataKey(gatheringId));
    }
}
