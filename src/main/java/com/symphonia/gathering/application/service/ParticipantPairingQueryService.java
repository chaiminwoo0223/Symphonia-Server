package com.symphonia.gathering.application.service;

import com.symphonia.catalog.application.dto.query.CatalogReferencesQuery;
import com.symphonia.catalog.application.dto.result.CatalogReferencesResult;
import com.symphonia.catalog.application.usecase.GetCatalogReferencesUseCase;
import com.symphonia.common.annotation.QueryService;
import com.symphonia.gathering.application.dto.result.ParticipantPairingResult;
import com.symphonia.gathering.application.usecase.GetLinkedParticipantUseCase;
import com.symphonia.gathering.application.usecase.GetParticipantPairingUseCase;
import com.symphonia.gathering.domain.exception.GatheringPairingNotFoundException;
import com.symphonia.gathering.domain.repository.GatheringPairingRepository;
import com.symphonia.gathering.domain.repository.ParticipantPairingFeedbackRepository;
import com.symphonia.gathering.domain.vo.GatheringPairing;
import lombok.RequiredArgsConstructor;

// 정산 페이지가 "보냈어요" 뒤에 피드백 카드를 띄울지 정하려고 부른다.
// 모임에서 고른 페어링이 없거나 카탈로그에서 대상이 사라졌으면 404로 응답해 카드를 띄우지 않게 한다.
@QueryService
@RequiredArgsConstructor
public class ParticipantPairingQueryService implements GetParticipantPairingUseCase {
    private final GetLinkedParticipantUseCase getLinkedParticipantUseCase;
    private final GatheringPairingRepository gatheringPairingRepository;
    private final ParticipantPairingFeedbackRepository participantPairingFeedbackRepository;
    private final GetCatalogReferencesUseCase getCatalogReferencesUseCase;

    @Override
    public ParticipantPairingResult getPairing(String linkToken) {
        Long participantId =
                getLinkedParticipantUseCase.getLinkedParticipant(linkToken).participantId();
        GatheringPairing gatheringPairing =
                gatheringPairingRepository
                        .findByParticipantId(participantId)
                        .orElseThrow(GatheringPairingNotFoundException::new);
        CatalogReferencesResult catalogReferences =
                getCatalogReferencesUseCase.getReferences(
                        CatalogReferencesQuery.of(
                                gatheringPairing.drinkId(), gatheringPairing.anjuId()));

        return ParticipantPairingResult.of(
                catalogReferences,
                participantPairingFeedbackRepository.existsByParticipantId(participantId));
    }
}
