package com.symphonia.gathering.presentation.dto.request;

import com.symphonia.gathering.application.dto.command.RegisterBankAccountCommand;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterBankAccountRequest(
        @NotBlank @Size(max = 20) @Schema(description = "은행 이름") String bankName,
        @NotBlank @Schema(description = "계좌번호 (하이픈 포함 가능)") String accountNumber,
        @Schema(description = "계좌를 정산 건 참여자에게 보여 주는 데 동의 여부") boolean consentAgreed,
        @NotBlank @Schema(description = "기록 화면에서 받은 계좌 동의 안내 버전") String consentVersion) {
    public RegisterBankAccountCommand toCommand(String linkToken) {
        return new RegisterBankAccountCommand(
                linkToken, bankName, accountNumber, consentAgreed, consentVersion);
    }
}
