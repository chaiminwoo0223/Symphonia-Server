package com.symphonia.gathering.domain.vo;

// 전이 규칙은 #125에서 Settlement에 둔다. 정산 건은 모든 분담을 미입금으로 시작한다.
public enum PaymentStatus {
    UNPAID,
    SENT,
    CONFIRMED
}
