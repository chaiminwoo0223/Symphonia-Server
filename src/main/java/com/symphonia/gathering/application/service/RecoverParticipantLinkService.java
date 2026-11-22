package com.symphonia.gathering.application.service;

import com.symphonia.auth.application.usecase.VerifySocialTokenUseCase;
import com.symphonia.gathering.application.dto.command.RecoverParticipantLinkCommand;
import com.symphonia.gathering.application.dto.result.ParticipantResult;
import com.symphonia.gathering.application.usecase.RecoverParticipantLinkUseCase;
import com.symphonia.gathering.application.usecase.ReissueParticipantLinkTokenByGoogleSubjectUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

// 구글 ID 토큰 검증은 외부 호출이라 트랜잭션 밖에서 하고, 재발급은 트랜잭션을 여는 UseCase에 맡긴다.
// 검증 동안 DB 커넥션을 쥐지 않기 위해서다.
@Service
@RequiredArgsConstructor
public class RecoverParticipantLinkService implements RecoverParticipantLinkUseCase {
    // auth의 SocialClient 구현체 이름이다. 참여자의 본인 확인에는 구글만 쓴다.
    private static final String GOOGLE_PROVIDER = "google";

    private final VerifySocialTokenUseCase verifySocialTokenUseCase;
    private final ReissueParticipantLinkTokenByGoogleSubjectUseCase
            reissueParticipantLinkTokenByGoogleSubjectUseCase;

    // 구글 ID 토큰은 본인 확인에만 쓰고 sub 외의 값은 읽지 않는다.
    @Override
    public ParticipantResult recover(RecoverParticipantLinkCommand command) {
        String googleSubject =
                verifySocialTokenUseCase
                        .verify(GOOGLE_PROVIDER, command.googleIdToken())
                        .socialId();

        return reissueParticipantLinkTokenByGoogleSubjectUseCase.reissueByGoogleSubject(
                command.toReissueCommand(googleSubject));
    }
}
