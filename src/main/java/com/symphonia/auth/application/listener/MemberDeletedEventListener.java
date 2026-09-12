package com.symphonia.auth.application.listener;

import static org.springframework.transaction.event.TransactionPhase.AFTER_COMMIT;

import com.symphonia.auth.application.dto.command.LogoutCommand;
import com.symphonia.auth.application.usecase.LogoutUseCase;
import com.symphonia.member.application.event.MemberDeletedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
public class MemberDeletedEventListener {
    private final LogoutUseCase logoutUseCase;

    @TransactionalEventListener(phase = AFTER_COMMIT)
    public void handle(MemberDeletedEvent event) {
        logoutUseCase.logout(LogoutCommand.of(event.accessToken(), event.ip()));
    }
}
