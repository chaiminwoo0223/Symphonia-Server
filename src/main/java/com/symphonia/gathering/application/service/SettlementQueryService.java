package com.symphonia.gathering.application.service;

import com.symphonia.common.annotation.QueryService;
import com.symphonia.gathering.application.dto.result.SettlementFormResult;
import com.symphonia.gathering.application.dto.result.SettlementParticipantResult;
import com.symphonia.gathering.application.dto.result.SettlementResult;
import com.symphonia.gathering.application.usecase.GetSettlementFormUseCase;
import com.symphonia.gathering.domain.entity.Gathering;
import com.symphonia.gathering.domain.entity.Participant;
import com.symphonia.gathering.domain.entity.Settlement;
import com.symphonia.gathering.domain.exception.GatheringNotFoundException;
import com.symphonia.gathering.domain.exception.ParticipantNotFoundException;
import com.symphonia.gathering.domain.policy.GatheringPolicy;
import com.symphonia.gathering.domain.policy.SettlementPolicy;
import com.symphonia.gathering.domain.repository.GatheringRepository;
import com.symphonia.gathering.domain.repository.ParticipantBankAccountRepository;
import com.symphonia.gathering.domain.repository.ParticipantLinkTokenRepository;
import com.symphonia.gathering.domain.repository.SettlementRepository;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;

@QueryService
@RequiredArgsConstructor
public class SettlementQueryService implements GetSettlementFormUseCase {
    private final ParticipantLinkTokenRepository participantLinkTokenRepository;
    private final GatheringRepository gatheringRepository;
    private final ParticipantBankAccountRepository participantBankAccountRepository;
    private final SettlementRepository settlementRepository;

    @Override
    public SettlementFormResult getForm(String linkToken) {
        Long participantId =
                participantLinkTokenRepository
                        .findParticipantIdByValue(linkToken)
                        .orElseThrow(ParticipantNotFoundException::new);
        Gathering gathering =
                gatheringRepository
                        .findByParticipantId(participantId)
                        .orElseThrow(GatheringNotFoundException::new);
        boolean recordable =
                GatheringPolicy.isWithinRecordingPeriod(
                        gathering.getCreatedAt(), LocalDateTime.now());
        List<Long> defaultParticipantIds =
                settlementRepository
                        .findLatestByGatheringId(gathering.getId())
                        .map(Settlement::getParticipantIds)
                        .orElseGet(
                                () ->
                                        gathering.getParticipants().stream()
                                                .map(Participant::getId)
                                                .toList());

        return SettlementFormResult.of(
                recordable,
                participantBankAccountRepository.existsByParticipantId(participantId),
                SettlementPolicy.BANK_ACCOUNT_CONSENT_VERSION,
                gathering.getParticipants().stream()
                        .map(SettlementParticipantResult::from)
                        .toList(),
                defaultParticipantIds,
                settlementRepository.findAllByPayerParticipantId(participantId).stream()
                        .map(SettlementResult::from)
                        .toList());
    }
}
