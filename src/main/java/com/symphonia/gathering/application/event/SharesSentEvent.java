package com.symphonia.gathering.application.event;

// "보냈어요" 요청 하나가 분담금을 하나라도 바꾼 경우에만 한 번 발행한다.
public record SharesSentEvent() {}
