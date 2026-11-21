package com.symphonia.pairing.application.usecase;

import com.symphonia.pairing.application.dto.query.GetMemberPairingFeedbacksQuery;
import com.symphonia.pairing.application.dto.result.MemberPairingFeedbacksResult;

public interface GetMemberPairingFeedbacksUseCase {
    MemberPairingFeedbacksResult getFeedbacks(GetMemberPairingFeedbacksQuery query);
}
