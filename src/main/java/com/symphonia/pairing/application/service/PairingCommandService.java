package com.symphonia.pairing.application.service;

import com.symphonia.catalog.application.usecase.VerifyCatalogReferencesUseCase;
import com.symphonia.common.annotation.CommandService;
import com.symphonia.pairing.application.dto.command.SubmitAnonymousPairingFeedbackCommand;
import com.symphonia.pairing.application.dto.command.SubmitPairingFeedbackCommand;
import com.symphonia.pairing.application.dto.result.PairingFeedbackResult;
import com.symphonia.pairing.application.event.PairingFeedbackSubmittedEvent;
import com.symphonia.pairing.application.usecase.SubmitAnonymousPairingFeedbackUseCase;
import com.symphonia.pairing.application.usecase.SubmitPairingFeedbackUseCase;
import com.symphonia.pairing.domain.entity.PairingFeedback;
import com.symphonia.pairing.domain.repository.PairingFeedbackRepository;
import com.symphonia.pairing.domain.repository.PairingRatingSummaryRepository;
import com.symphonia.pairing.domain.vo.PairingFeedbackChange;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;

@CommandService
@RequiredArgsConstructor
public class PairingCommandService
        implements SubmitPairingFeedbackUseCase, SubmitAnonymousPairingFeedbackUseCase {
    private final VerifyCatalogReferencesUseCase verifyCatalogReferencesUseCase;
    private final PairingFeedbackRepository pairingFeedbackRepository;
    private final PairingRatingSummaryRepository pairingRatingSummaryRepository;
    private final ApplicationEventPublisher eventPublisher;

    // 회원은 같은 조합에 피드백을 하나만 남긴다. 없을 때만 넣은 뒤 잠가 읽으므로, 첫 피드백을 동시에 내도 나중 요청은 다시 낸 피드백으로 처리한다.
    // 잠가 읽기 때문에 동시에 다시 낸 평점이 집계에 두 번 반영되지도 않는다.
    @Override
    public PairingFeedbackResult submit(SubmitPairingFeedbackCommand command) {
        verifyCatalogReferencesUseCase.verify(command.toCatalogReferencesQuery());

        boolean created = pairingFeedbackRepository.saveIfAbsent(feedbackOf(command));
        PairingFeedback pairingFeedback =
                pairingFeedbackRepository
                        .findByMemberIdAndCombinationForUpdate(
                                command.memberId(), command.drinkId(), command.anjuId())
                        .orElseThrow();

        return created
                ? submitFirst(pairingFeedback, PairingFeedbackSubmittedEvent.byMember())
                : resubmit(pairingFeedback, command);
    }

    // 익명 피드백은 찾아서 바꿀 회원이 없으므로 늘 새로 저장한다.
    @Override
    public PairingFeedbackResult submitAnonymous(SubmitAnonymousPairingFeedbackCommand command) {
        verifyCatalogReferencesUseCase.verify(command.toCatalogReferencesQuery());

        PairingFeedback pairingFeedback =
                pairingFeedbackRepository.save(
                        PairingFeedback.anonymous(
                                command.drinkId(),
                                command.anjuId(),
                                command.relationshipType(),
                                command.moodType(),
                                command.rating()));

        return submitFirst(pairingFeedback, PairingFeedbackSubmittedEvent.byParticipant());
    }

    private PairingFeedback feedbackOf(SubmitPairingFeedbackCommand command) {
        return PairingFeedback.of(
                command.memberId(),
                command.drinkId(),
                command.anjuId(),
                command.relationshipType(),
                command.moodType(),
                command.rating());
    }

    private PairingFeedbackResult submitFirst(
            PairingFeedback pairingFeedback, PairingFeedbackSubmittedEvent event) {
        accumulate(pairingFeedback, pairingFeedback.addedChange());
        eventPublisher.publishEvent(event);

        return PairingFeedbackResult.submitted(pairingFeedback);
    }

    // 전역 집계를 먼저 갱신하고, 상황별 집계는 변화가 담긴 순서(상황 순서)대로 갱신한다.
    private void accumulate(PairingFeedback pairingFeedback, PairingFeedbackChange change) {
        change.globalChange()
                .ifPresent(
                        ratingChange ->
                                pairingRatingSummaryRepository.accumulate(
                                        pairingFeedback.getDrinkId(),
                                        pairingFeedback.getAnjuId(),
                                        ratingChange));
        change.situationChanges()
                .forEach(
                        situationChange ->
                                pairingRatingSummaryRepository.accumulateInSituation(
                                        pairingFeedback.getDrinkId(),
                                        pairingFeedback.getAnjuId(),
                                        situationChange.situation(),
                                        situationChange.change()));
    }

    private PairingFeedbackResult resubmit(
            PairingFeedback pairingFeedback, SubmitPairingFeedbackCommand command) {
        PairingFeedbackChange change =
                pairingFeedback.resubmit(
                        command.relationshipType(), command.moodType(), command.rating());
        PairingFeedback savedPairingFeedback = pairingFeedbackRepository.save(pairingFeedback);
        accumulate(savedPairingFeedback, change);

        return PairingFeedbackResult.resubmitted(savedPairingFeedback);
    }
}
