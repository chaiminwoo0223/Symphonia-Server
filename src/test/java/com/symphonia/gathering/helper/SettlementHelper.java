package com.symphonia.gathering.helper;

import com.symphonia.gathering.domain.entity.Gathering;
import com.symphonia.gathering.domain.entity.Participant;
import com.symphonia.gathering.domain.entity.Settlement;
import com.symphonia.gathering.domain.repository.ParticipantBankAccountRepository;
import com.symphonia.gathering.domain.repository.SettlementRepository;
import com.symphonia.gathering.fixture.ParticipantBankAccountFixture;
import com.symphonia.gathering.fixture.SettlementFixture;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SettlementHelper {

    private final ParticipantBankAccountRepository participantBankAccountRepository;
    private final SettlementRepository settlementRepository;

    public void registerBankAccount(Gathering gathering, Participant payer) {
        participantBankAccountRepository.save(
                ParticipantBankAccountFixture.KOOKMIN.create(payer.getId()), gathering.getId());
    }

    // 모임 참여자 전원을 인원으로 정산 건을 기록한다.
    public Settlement record(
            SettlementFixture settlementFixture, Gathering gathering, Participant payer) {
        return settlementRepository.save(
                settlementFixture.create(
                        gathering.getId(),
                        payer.getId(),
                        gathering.getParticipants().stream().map(Participant::getId).toList()));
    }

    // 참여자가 "보냈어요"를 누른 상태를 만든다.
    public Settlement markSent(Settlement settlement, Participant participant) {
        settlement.markShareSent(participant.getId());

        return settlementRepository.save(settlement);
    }

    public Settlement confirm(Settlement settlement, Participant participant) {
        settlement.confirmShare(participant.getId());

        return settlementRepository.save(settlement);
    }

    public Settlement cancel(Settlement settlement) {
        settlement.cancel(LocalDateTime.now());

        return settlementRepository.save(settlement);
    }
}
