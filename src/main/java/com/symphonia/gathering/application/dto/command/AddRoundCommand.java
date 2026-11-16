package com.symphonia.gathering.application.dto.command;

// roundNumber는 추가할 차수의 번호다. 그 번호의 차수가 이미 있으면 새로 만들지 않고 돌려준다.
public record AddRoundCommand(String linkToken, int roundNumber) {}
