package com.symphonia.auth.application.listener;

import static org.springframework.transaction.event.TransactionPhase.AFTER_COMMIT;

import com.symphonia.auth.application.dto.command.LogoutCommand;
import com.symphonia.auth.application.usecase.BlockWithdrawnMemberUseCase;
import com.symphonia.auth.application.usecase.ForceRevokeUseCase;
import com.symphonia.auth.application.usecase.LogoutUseCase;
import com.symphonia.member.application.event.MemberDeletedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;

// 탈퇴는 이 기기만 끊는 로그아웃과 달리 회원의 모든 기기 세션을 끊는다.
// 그래서 모든 리프레시 토큰을 지우고, 탈퇴를 요청한 액세스 토큰은 로그아웃으로 블랙리스트에 올린다.
// 다른 기기의 액세스 토큰은 알 수 없으므로 탈퇴한 회원 id를 남겨 필터가 막게 한다.
@Component
@RequiredArgsConstructor
public class MemberDeletedEventListener {
    private final ForceRevokeUseCase forceRevokeUseCase;
    private final LogoutUseCase logoutUseCase;
    private final BlockWithdrawnMemberUseCase blockWithdrawnMemberUseCase;

    @TransactionalEventListener(phase = AFTER_COMMIT)
    public void handle(MemberDeletedEvent event) {
        String memberId = String.valueOf(event.memberId());

        blockWithdrawnMemberUseCase.block(memberId);
        forceRevokeUseCase.forceRevoke(memberId);
        logoutUseCase.logout(LogoutCommand.of(null, event.accessToken(), event.ip()));
    }
}
