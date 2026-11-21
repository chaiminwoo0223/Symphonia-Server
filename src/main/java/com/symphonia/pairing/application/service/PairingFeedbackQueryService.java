package com.symphonia.pairing.application.service;

import com.symphonia.catalog.application.dto.query.CatalogReferencesQuery;
import com.symphonia.catalog.application.usecase.GetCatalogReferencesUseCase;
import com.symphonia.common.annotation.QueryService;
import com.symphonia.pairing.application.dto.query.GetMemberPairingFeedbacksQuery;
import com.symphonia.pairing.application.dto.result.MemberPairingFeedbackResult;
import com.symphonia.pairing.application.dto.result.MemberPairingFeedbacksResult;
import com.symphonia.pairing.application.usecase.GetMemberPairingFeedbacksUseCase;
import com.symphonia.pairing.domain.entity.PairingFeedback;
import com.symphonia.pairing.domain.repository.PairingFeedbackRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;

@QueryService
@RequiredArgsConstructor
public class PairingFeedbackQueryService implements GetMemberPairingFeedbacksUseCase {
    private final PairingFeedbackRepository pairingFeedbackRepository;
    private final GetCatalogReferencesUseCase getCatalogReferencesUseCase;

    // 다음 페이지가 있는지 알려고 한 건을 더 읽는다. 익명 피드백은 회원 id가 없어 읽히지 않는다.
    // 이름은 피드백에 복사해 두지 않고 읽을 때마다 catalog에서 가져온다. 피드백은 술과 안주를 FK로 참조하므로 대상이 사라지지 않는다.
    @Override
    public MemberPairingFeedbacksResult getFeedbacks(GetMemberPairingFeedbacksQuery query) {
        List<PairingFeedback> fetched =
                pairingFeedbackRepository.findAllByMemberIdBeforeId(
                        query.memberId(), query.cursor(), query.size() + 1);
        List<PairingFeedback> page = fetched.stream().limit(query.size()).toList();
        Long nextCursor = fetched.size() > query.size() ? page.getLast().getId() : null;

        return MemberPairingFeedbacksResult.of(
                page.stream().map(this::toResult).toList(), nextCursor);
    }

    private MemberPairingFeedbackResult toResult(PairingFeedback pairingFeedback) {
        return MemberPairingFeedbackResult.of(
                pairingFeedback,
                getCatalogReferencesUseCase.getReferences(
                        CatalogReferencesQuery.of(
                                pairingFeedback.getDrinkId(), pairingFeedback.getAnjuId())));
    }
}
