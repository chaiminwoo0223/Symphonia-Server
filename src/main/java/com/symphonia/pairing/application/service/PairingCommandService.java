package com.symphonia.pairing.application.service;

import com.symphonia.catalog.application.usecase.VerifyCatalogReferencesUseCase;
import com.symphonia.common.annotation.CommandService;
import com.symphonia.pairing.application.dto.command.SubmitPairingFeedbackCommand;
import com.symphonia.pairing.application.dto.result.PairingFeedbackResult;
import com.symphonia.pairing.application.usecase.SubmitPairingFeedbackUseCase;
import com.symphonia.pairing.domain.entity.PairingFeedback;
import com.symphonia.pairing.domain.repository.PairingFeedbackRepository;
import lombok.RequiredArgsConstructor;

@CommandService
@RequiredArgsConstructor
public class PairingCommandService implements SubmitPairingFeedbackUseCase {
    private final VerifyCatalogReferencesUseCase verifyCatalogReferencesUseCase;
    private final PairingFeedbackRepository pairingFeedbackRepository;

    @Override
    public PairingFeedbackResult submit(SubmitPairingFeedbackCommand command) {
        verifyCatalogReferencesUseCase.verify(command.toVerifyCatalogReferencesQuery());

        PairingFeedback pairingFeedback =
                PairingFeedback.of(
                        command.memberId(),
                        command.drinkId(),
                        command.anjuId(),
                        command.musicMoodId(),
                        command.relationshipType(),
                        command.moodType(),
                        command.rating());
        PairingFeedback savedPairingFeedback = pairingFeedbackRepository.save(pairingFeedback);

        return PairingFeedbackResult.from(savedPairingFeedback);
    }
}
