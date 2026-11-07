package com.symphonia.pairing.application.usecase;

import com.symphonia.pairing.application.dto.command.SubmitAnonymousPairingFeedbackCommand;
import com.symphonia.pairing.application.dto.result.PairingFeedbackResult;

// 회원이 아닌 사람의 피드백을 받는다. 누가 냈는지 남기지 않으므로 다시 내도 바꾸지 않고 새로 쌓는다.
// 같은 사람이 여러 번 내지 않게 막는 일은 부르는 쪽이 맡는다.
public interface SubmitAnonymousPairingFeedbackUseCase {
    PairingFeedbackResult submitAnonymous(SubmitAnonymousPairingFeedbackCommand command);
}
