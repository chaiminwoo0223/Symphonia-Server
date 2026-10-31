package com.symphonia.gathering.infrastructure.jpa;

import com.symphonia.gathering.domain.entity.ParticipantBankAccount;
import com.symphonia.gathering.domain.policy.SettlementPolicy;
import com.symphonia.gathering.domain.repository.ParticipantBankAccountRepository;
import com.symphonia.gathering.infrastructure.crypto.GatheringDataKey;
import java.util.Collection;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class ParticipantBankAccountRepositoryImpl implements ParticipantBankAccountRepository {
    private final ParticipantBankAccountJpaRepository participantBankAccountJpaRepository;
    private final GatheringDataKeyLoader gatheringDataKeyLoader;

    // 같은 참여자가 동시에 등록하면 Service의 중복 검사를 둘 다 통과하므로 기본 키 위반으로 구분한다.
    @Override
    public void save(ParticipantBankAccount participantBankAccount, Long gatheringId) {
        ParticipantBankAccountJpaEntity entity =
                ParticipantBankAccountJpaEntity.from(
                        participantBankAccount, gatheringDataKeyLoader.load(gatheringId));

        try {
            participantBankAccountJpaRepository.saveAndFlush(entity);
        } catch (DataIntegrityViolationException e) {
            SettlementPolicy.validateBankAccountNotRegistered(isPrimaryKeyViolation(e));
            throw e;
        }
    }

    @Override
    public boolean existsByParticipantId(Long participantId) {
        return participantBankAccountJpaRepository.existsById(participantId);
    }

    @Override
    public List<ParticipantBankAccount> findAllByParticipantIds(
            Collection<Long> participantIds, Long gatheringId) {
        GatheringDataKey dataKey = gatheringDataKeyLoader.load(gatheringId);

        return participantBankAccountJpaRepository.findAllById(participantIds).stream()
                .map(entity -> entity.toDomain(dataKey))
                .toList();
    }

    private boolean isPrimaryKeyViolation(DataIntegrityViolationException e) {
        return e.getCause() instanceof ConstraintViolationException violation
                && "pk_participant_bank_account".equalsIgnoreCase(violation.getConstraintName());
    }
}
