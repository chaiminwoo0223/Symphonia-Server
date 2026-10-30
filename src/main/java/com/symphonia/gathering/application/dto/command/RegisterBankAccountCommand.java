package com.symphonia.gathering.application.dto.command;

public record RegisterBankAccountCommand(
        String linkToken,
        String bankName,
        String accountNumber,
        boolean consentAgreed,
        String consentVersion) {}
