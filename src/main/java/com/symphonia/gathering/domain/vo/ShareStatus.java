package com.symphonia.gathering.domain.vo;

// 분담금의 입금 상태다. 분담금은 미입금으로 시작하고, 낼 돈이 없는 0원 분담금만 확인 완료로 시작한다.
// 참여자가 "보냈어요"를 누르면 보냄이 되고, 결제자는 필요할 때만 확인 완료로 바꾸거나 미입금으로 되돌린다.
public enum ShareStatus {
    UNPAID,
    SENT,
    CONFIRMED
}
