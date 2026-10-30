package com.symphonia.gathering.domain.repository;

import com.symphonia.gathering.domain.entity.ParticipantBankAccount;

public interface ParticipantBankAccountRepository {
    // 계좌는 모임별 데이터 키로 암호화하므로 참여자가 속한 모임을 함께 받는다.
    void save(ParticipantBankAccount participantBankAccount, Long gatheringId);

    boolean existsByParticipantId(Long participantId);
}
