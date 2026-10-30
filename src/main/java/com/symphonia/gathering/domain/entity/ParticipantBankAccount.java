package com.symphonia.gathering.domain.entity;

import com.symphonia.gathering.domain.vo.BankAccount;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

// 참여자가 처음 정산 건의 결제자가 될 때 한 번만 등록하고 바꾸지 않는 계좌다.
// 계좌를 정산 건 참여자에게 보여 준다는 안내에 따로 동의한 버전과 시각을 함께 남긴다.
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class ParticipantBankAccount {
    private Long participantId;
    private BankAccount bankAccount;
    private String consentVersion;
    private LocalDateTime consentedAt;

    public static ParticipantBankAccount register(
            Long participantId,
            BankAccount bankAccount,
            String consentVersion,
            LocalDateTime consentedAt) {
        return ParticipantBankAccount.builder()
                .participantId(participantId)
                .bankAccount(bankAccount)
                .consentVersion(consentVersion)
                .consentedAt(consentedAt)
                .build();
    }
}
