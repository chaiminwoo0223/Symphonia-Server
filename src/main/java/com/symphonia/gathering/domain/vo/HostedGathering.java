package com.symphonia.gathering.domain.vo;

// 모임장이 자기 모임 목록에서 보는 값 중 파기 시각 판단과 관계없는 값이다. 참여자를 복호화하지 않고 읽으므로 참여자는 수만 담는다.
public record HostedGathering(Long gatheringId, String name, long participantCount) {}
