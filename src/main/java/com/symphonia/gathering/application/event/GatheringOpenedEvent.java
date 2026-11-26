package com.symphonia.gathering.application.event;

// 모임을 새로 연 경우에만 발행한다. 지표는 건수만 세므로 식별자를 담지 않는다.
public record GatheringOpenedEvent() {}
