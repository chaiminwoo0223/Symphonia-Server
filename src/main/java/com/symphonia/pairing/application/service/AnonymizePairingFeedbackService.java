package com.symphonia.pairing.application.service;

import com.symphonia.common.annotation.CommandService;
import com.symphonia.pairing.application.usecase.AnonymizePairingFeedbackUseCase;
import com.symphonia.pairing.domain.repository.PairingFeedbackRepository;
import lombok.RequiredArgsConstructor;

// 탈퇴한 회원의 피드백은 지우지 않고 회원 id만 비운다.
// 이미 평점 집계에 반영된 값이어서 지우면 집계와 어긋나고, 회원 id가 없으면 익명 피드백과 같아진다.
@CommandService
@RequiredArgsConstructor
public class AnonymizePairingFeedbackService implements AnonymizePairingFeedbackUseCase {
    private final PairingFeedbackRepository pairingFeedbackRepository;

    @Override
    public void anonymize(Long memberId) {
        pairingFeedbackRepository.anonymizeByMemberId(memberId);
    }
}
