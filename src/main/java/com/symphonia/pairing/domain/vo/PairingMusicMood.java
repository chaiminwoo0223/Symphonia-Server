package com.symphonia.pairing.domain.vo;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

// 추천이 점수를 매기는 음악 무드다. 카탈로그의 음악 무드 중 추천에 필요한 값만 담는다.
@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class PairingMusicMood {
    private final Long id;
    private final String title;
    private final String streamingUrl;
    private final MoodProfile moodProfile;

    public static PairingMusicMood of(
            Long id, String title, String streamingUrl, MoodProfile moodProfile) {
        return new PairingMusicMood(id, title, streamingUrl, moodProfile);
    }

    public double fitnessFor(MoodProfile occasionMoodProfile) {
        return moodProfile.fitness(occasionMoodProfile);
    }

    public boolean fitsFor(MoodProfile occasionMoodProfile) {
        return moodProfile.fits(occasionMoodProfile);
    }
}
