package com.symphonia.gathering.infrastructure.jpa;

import com.symphonia.gathering.domain.entity.ParticipantBankAccount;
import com.symphonia.gathering.domain.vo.BankAccount;
import com.symphonia.gathering.infrastructure.crypto.GatheringDataKey;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.domain.Persistable;

@Entity
@Table(name = "participant_bank_account")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class ParticipantBankAccountJpaEntity implements Persistable<Long> {
    @Id private Long participantId;

    @Column(nullable = false)
    private byte[] encryptedBankName;

    @Column(nullable = false)
    private byte[] encryptedAccountNumber;

    @Column(nullable = false, length = 20)
    private String consentVersion;

    @Column(nullable = false)
    private LocalDateTime consentedAt;

    // 은행 이름과 계좌번호는 모임별 데이터 키로 암호화한 값만 저장한다.
    public static ParticipantBankAccountJpaEntity from(
            ParticipantBankAccount participantBankAccount, GatheringDataKey dataKey) {
        return new ParticipantBankAccountJpaEntity(
                participantBankAccount.getParticipantId(),
                dataKey.encrypt(participantBankAccount.getBankAccount().bankName()),
                dataKey.encrypt(participantBankAccount.getBankAccount().accountNumber()),
                participantBankAccount.getConsentVersion(),
                participantBankAccount.getConsentedAt());
    }

    public ParticipantBankAccount toDomain(GatheringDataKey dataKey) {
        return ParticipantBankAccount.builder()
                .participantId(participantId)
                .bankAccount(
                        new BankAccount(
                                dataKey.decrypt(encryptedBankName),
                                dataKey.decrypt(encryptedAccountNumber)))
                .consentVersion(consentVersion)
                .consentedAt(consentedAt)
                .build();
    }

    @Override
    public Long getId() {
        return participantId;
    }

    // 계좌는 한 번만 등록하므로 항상 새 행으로 보고 INSERT한다. 같은 참여자의 행이 있으면 기본 키 위반이 난다.
    @Override
    public boolean isNew() {
        return true;
    }
}
