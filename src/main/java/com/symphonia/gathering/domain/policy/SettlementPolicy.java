package com.symphonia.gathering.domain.policy;

import com.symphonia.gathering.domain.exception.BankAccountAlreadyRegisteredException;
import com.symphonia.gathering.domain.exception.BankAccountConsentRequiredException;
import com.symphonia.gathering.domain.exception.BankAccountRequiredException;
import com.symphonia.gathering.domain.exception.InvalidSettlementParticipantsException;
import com.symphonia.gathering.domain.exception.NotSettlementPayerException;
import com.symphonia.gathering.domain.exception.OutdatedBankAccountConsentVersionException;
import com.symphonia.gathering.domain.exception.SettlementRecordingClosedException;
import java.time.LocalDateTime;

// 정산 건과 모임, 계좌를 함께 봐야 하는 검증이다. Service가 Settlement의 상태를 바꾸기 전에 호출한다.
public enum SettlementPolicy {
    ;

    // 결제자가 계좌를 입력할 때 안내하는 문구의 버전이다. 문구를 바꾸면 이 값도 바꾼다.
    public static final String BANK_ACCOUNT_CONSENT_VERSION = "2026-10-30";

    public static void validateRecordable(LocalDateTime gatheringCreatedAt, LocalDateTime now) {
        if (!GatheringPolicy.isWithinRecordingPeriod(gatheringCreatedAt, now)) {
            throw new SettlementRecordingClosedException();
        }
    }

    public static void validateGatheringParticipants(boolean allJoined) {
        if (!allJoined) {
            throw new InvalidSettlementParticipantsException();
        }
    }

    public static void validateBankAccountRegistered(boolean registered) {
        if (!registered) {
            throw new BankAccountRequiredException();
        }
    }

    public static void validateBankAccountNotRegistered(boolean registered) {
        if (registered) {
            throw new BankAccountAlreadyRegisteredException();
        }
    }

    public static void validateBankAccountConsent(boolean consentAgreed, String consentVersion) {
        if (!consentAgreed) {
            throw new BankAccountConsentRequiredException();
        }
        if (!BANK_ACCOUNT_CONSENT_VERSION.equals(consentVersion)) {
            throw new OutdatedBankAccountConsentVersionException();
        }
    }

    public static void validatePayer(Long payerParticipantId, Long participantId) {
        if (!payerParticipantId.equals(participantId)) {
            throw new NotSettlementPayerException();
        }
    }
}
