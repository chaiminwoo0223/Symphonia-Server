package com.symphonia.pairing.domain.vo;

// 피드백을 낸 경로다. 낸 시점의 경로라서 회원이 탈퇴해 회원 id가 비어도 바뀌지 않는다.
public enum FeedbackSource {
    MEMBER,
    PARTICIPANT,
}
