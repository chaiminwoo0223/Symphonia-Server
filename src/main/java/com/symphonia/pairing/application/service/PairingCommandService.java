package com.symphonia.pairing.application.service;

import com.symphonia.catalog.domain.exception.AnjuNotFoundException;
import com.symphonia.catalog.domain.exception.DrinkNotFoundException;
import com.symphonia.catalog.domain.exception.MusicMoodNotFoundException;
import com.symphonia.catalog.domain.repository.AnjuRepository;
import com.symphonia.catalog.domain.repository.DrinkRepository;
import com.symphonia.catalog.domain.repository.MusicMoodRepository;
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
    private final DrinkRepository drinkRepository;
    private final AnjuRepository anjuRepository;
    private final MusicMoodRepository musicMoodRepository;
    private final PairingFeedbackRepository pairingFeedbackRepository;

    @Override
    public PairingFeedbackResult submit(SubmitPairingFeedbackCommand command) {
        drinkRepository.findById(command.drinkId()).orElseThrow(DrinkNotFoundException::new);
        anjuRepository.findById(command.anjuId()).orElseThrow(AnjuNotFoundException::new);
        musicMoodRepository
                .findById(command.musicMoodId())
                .orElseThrow(MusicMoodNotFoundException::new);

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
