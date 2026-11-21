package com.symphonia.member.application.usecase;

import com.symphonia.member.application.dto.result.WithdrawalCheckResult;

public interface CheckWithdrawalUseCase {
    WithdrawalCheckResult check(Long memberId);
}
