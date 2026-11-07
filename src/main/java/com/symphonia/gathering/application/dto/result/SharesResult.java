package com.symphonia.gathering.application.dto.result;

import java.time.LocalDateTime;
import java.util.List;

// 개인 링크에서 보는 분담금이다. payers는 결제자별 묶음이고 보낼 분담금이 남은 결제자가 앞에 온다.
// deletesAt은 모임의 개인정보를 파기할 시각이다. 계좌와 금액을 언제까지 볼 수 있는지 안내하는 데 쓴다.
public record SharesResult(LocalDateTime deletesAt, List<PayerSharesResult> payers) {
    public static SharesResult of(LocalDateTime deletesAt, List<PayerSharesResult> payers) {
        return new SharesResult(deletesAt, payers);
    }
}
