package com.symphonia.gathering.fixture;

import com.symphonia.gathering.domain.entity.ParticipantBankAccount;
import com.symphonia.gathering.domain.policy.SettlementPolicy;
import com.symphonia.gathering.domain.vo.BankAccount;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ParticipantBankAccountFixture {
    KOOKMIN("국민은행", "123456789012");

    private static final LocalDateTime CONSENTED_AT = LocalDateTime.of(2026, 10, 27, 20, 0);

    private final String bankName;
    private final String accountNumber;

    public ParticipantBankAccount create(Long participantId) {
        return ParticipantBankAccount.register(
                participantId,
                BankAccount.of(bankName, accountNumber),
                SettlementPolicy.BANK_ACCOUNT_CONSENT_VERSION,
                CONSENTED_AT);
    }
}
