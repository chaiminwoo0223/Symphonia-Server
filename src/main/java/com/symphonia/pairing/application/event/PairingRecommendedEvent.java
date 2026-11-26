package com.symphonia.pairing.application.event;

// 추천 요청 하나가 성공할 때마다 발행한다. 지표는 건수만 세므로 요청 내용과 식별자를 담지 않는다.
public record PairingRecommendedEvent() {}
