package com.symphonia.pairing.application.listener;

import static org.springframework.transaction.event.TransactionPhase.BEFORE_COMMIT;

import com.symphonia.member.application.event.MemberDeletedEvent;
import com.symphonia.pairing.application.usecase.AnonymizePairingFeedbackUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;

// 피드백 익명화는 회원 삭제와 같은 DB를 바꾸므로, 커밋 직전에 같은 트랜잭션에서 처리한다.
// 커밋 뒤(AFTER_COMMIT)에는 트랜잭션이 이미 끝나 있어 UseCase의 쓰기가 반영되지 않는다.
// 이 처리가 실패하면 회원 삭제도 함께 롤백되어, 회원 id가 남은 피드백이 생기지 않는다.
@Component
@RequiredArgsConstructor
public class PairingMemberDeletedEventListener {
    private final AnonymizePairingFeedbackUseCase anonymizePairingFeedbackUseCase;

    @TransactionalEventListener(phase = BEFORE_COMMIT)
    public void handle(MemberDeletedEvent event) {
        anonymizePairingFeedbackUseCase.anonymize(event.memberId());
    }
}
