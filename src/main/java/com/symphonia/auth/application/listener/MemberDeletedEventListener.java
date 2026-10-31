package com.symphonia.auth.application.listener;

import static org.springframework.transaction.event.TransactionPhase.AFTER_COMMIT;

import com.symphonia.auth.application.dto.command.LogoutCommand;
import com.symphonia.auth.application.usecase.ForceRevokeUseCase;
import com.symphonia.auth.application.usecase.LogoutUseCase;
import com.symphonia.member.application.event.MemberDeletedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;

// 탈퇴는 이 기기만 끊는 로그아웃과 달리 회원의 모든 기기 세션을 끊는다.
// 그래서 모든 리프레시 토큰을 지우고, 탈퇴를 요청한 액세스 토큰은 로그아웃으로 블랙리스트에 올린다.
@Component
@RequiredArgsConstructor
public class MemberDeletedEventListener {
    private final ForceRevokeUseCase forceRevokeUseCase;
    private final LogoutUseCase logoutUseCase;

    @TransactionalEventListener(phase = AFTER_COMMIT)
    public void handle(MemberDeletedEvent event) {
        forceRevokeUseCase.forceRevoke(String.valueOf(event.memberId()));
        logoutUseCase.logout(LogoutCommand.of(null, event.accessToken(), event.ip()));
    }
}
