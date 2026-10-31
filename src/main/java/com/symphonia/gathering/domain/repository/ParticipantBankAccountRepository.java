package com.symphonia.gathering.domain.repository;

import com.symphonia.gathering.domain.entity.ParticipantBankAccount;
import java.util.Collection;
import java.util.List;

public interface ParticipantBankAccountRepository {
    // 계좌는 모임별 데이터 키로 암호화하므로 참여자가 속한 모임을 함께 받는다.
    void save(ParticipantBankAccount participantBankAccount, Long gatheringId);

    boolean existsByParticipantId(Long participantId);

    // 참여자가 보낼 분담의 결제자 계좌를 한 번에 읽는다. 복호화할 모임별 데이터 키를 찾도록 모임을 함께 받는다.
    List<ParticipantBankAccount> findAllByParticipantIds(
            Collection<Long> participantIds, Long gatheringId);
}
