package com.symphonia.member.application.service;

import com.symphonia.gathering.application.usecase.GetOngoingGatheringsUseCase;
import com.symphonia.member.application.dto.result.WithdrawalCheckResult;
import com.symphonia.member.application.usecase.CheckWithdrawalUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

// 모임장이 탈퇴해도 모임은 파기 시각까지 남지만 모임장은 새 기록과 참여자 관리를 할 수 없다. 탈퇴를 막지 않고 미리 알린다.
// 회원 DB를 읽지 않고 gathering의 UseCase만 부르므로 트랜잭션은 그 UseCase가 연다.
@Service
@RequiredArgsConstructor
public class WithdrawalCheckService implements CheckWithdrawalUseCase {
    private final GetOngoingGatheringsUseCase getOngoingGatheringsUseCase;

    @Override
    public WithdrawalCheckResult check(Long memberId) {
        return WithdrawalCheckResult.from(getOngoingGatheringsUseCase.getOngoing(memberId));
    }
}
