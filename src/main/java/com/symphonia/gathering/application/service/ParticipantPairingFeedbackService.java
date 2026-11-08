package com.symphonia.gathering.application.service;

import com.symphonia.common.annotation.CommandService;
import com.symphonia.gathering.application.dto.command.SubmitParticipantPairingFeedbackCommand;
import com.symphonia.gathering.application.usecase.GetLinkedParticipantUseCase;
import com.symphonia.gathering.application.usecase.SubmitParticipantPairingFeedbackUseCase;
import com.symphonia.gathering.domain.exception.GatheringPairingNotFoundException;
import com.symphonia.gathering.domain.policy.GatheringPolicy;
import com.symphonia.gathering.domain.repository.GatheringPairingRepository;
import com.symphonia.gathering.domain.repository.ParticipantPairingFeedbackRepository;
import com.symphonia.gathering.domain.vo.GatheringPairing;
import com.symphonia.pairing.application.usecase.SubmitAnonymousPairingFeedbackUseCase;
import java.time.Clock;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;

// 참여자가 정산 페이지에서 모임 페어링에 남기는 익명 피드백을 받는다(#131).
// 피드백은 파기하지 않으므로 참여자를 가리키지 않고, 중복 제출은 참여자 쪽 제출 표시로 막는다.
// 제출 표시와 피드백은 한 트랜잭션이다. 카탈로그에서 대상이 사라져 피드백이 404로 실패하면 표시도 함께 롤백돼 다시 낼 수 있다.
@CommandService
@RequiredArgsConstructor
public class ParticipantPairingFeedbackService implements SubmitParticipantPairingFeedbackUseCase {
    private final GetLinkedParticipantUseCase getLinkedParticipantUseCase;
    private final GatheringPairingRepository gatheringPairingRepository;
    private final ParticipantPairingFeedbackRepository participantPairingFeedbackRepository;
    private final SubmitAnonymousPairingFeedbackUseCase submitAnonymousPairingFeedbackUseCase;
    private final Clock clock;

    @Override
    public void submit(SubmitParticipantPairingFeedbackCommand command) {
        Long participantId =
                getLinkedParticipantUseCase
                        .getLinkedParticipant(command.linkToken())
                        .participantId();
        GatheringPairing gatheringPairing =
                gatheringPairingRepository
                        .findByParticipantId(participantId)
                        .orElseThrow(GatheringPairingNotFoundException::new);
        // 다시 내는 요청과 동시에 내는 요청 모두 표시를 넣지 못하므로, 표시를 넣은 결과 하나로 중복 제출을 막는다.
        boolean marked =
                participantPairingFeedbackRepository.markSubmittedIfAbsent(
                        participantId, LocalDateTime.now(clock));
        GatheringPolicy.validatePairingFeedbackNotSubmitted(!marked);

        submitAnonymousPairingFeedbackUseCase.submitAnonymous(
                command.toSubmitAnonymousPairingFeedbackCommand(gatheringPairing));
    }
}
