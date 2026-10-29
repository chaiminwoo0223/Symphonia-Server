package com.symphonia.gathering.infrastructure.jpa;

import com.symphonia.gathering.domain.entity.Gathering;
import com.symphonia.gathering.domain.entity.Participant;
import com.symphonia.gathering.domain.exception.GatheringDataKeyNotFoundException;
import com.symphonia.gathering.domain.policy.GatheringPolicy;
import com.symphonia.gathering.domain.repository.GatheringRepository;
import com.symphonia.gathering.infrastructure.crypto.GatheringDataKey;
import com.symphonia.gathering.infrastructure.crypto.GatheringMasterKey;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
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
        // 참여자는 참여할 때 더해지기만 하므로 새 참여자만 INSERT한다.
        // 기존 참여자까지 다시 저장하면 동시에 들어온 요청이 메모리에 있던 옛 값으로 덮어쓸 수 있다.
        List<ParticipantJpaEntity> participantEntities =
                gathering.getParticipants().stream()
                        .filter(participant -> participant.getId() == null)
                        .map(
                                participant ->
                                        ParticipantJpaEntity.from(
                                                participant, gatheringId, dataKey))
                        .toList();

        try {
            List<ParticipantJpaEntity> savedParticipants =
                    participantJpaRepository.saveAll(participantEntities);
            List<Participant> participants =
                    Stream.concat(
                                    gathering.getParticipants().stream()
                                            .filter(participant -> participant.getId() != null),
                                    savedParticipants.stream()
                                            .map(
                                                    participantEntity ->
                                                            participantEntity.toDomain(dataKey)))
                            .toList();

            return savedGathering.toDomain(participants);
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

    private Gathering toDomain(
            GatheringJpaEntity gatheringEntity,
            List<ParticipantJpaEntity> participantEntities,
            GatheringDataKey dataKey) {
        return gatheringEntity.toDomain(
                participantEntities.stream()
                        .map(participantEntity -> participantEntity.toDomain(dataKey))
                        .toList());
    }
}
