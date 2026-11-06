package com.symphonia.pairing.domain.vo;

// 음료, 안주, 음악 조합을 id로 가리킨다. 피드백 집계를 조합에 붙일 때 FeedbackScores와 Pairing이 함께 쓴다.
public record CombinationId(Long drinkId, Long anjuId, Long musicMoodId) {}
