package com.symphonia.pairing.domain.entity;

import com.symphonia.pairing.domain.vo.MoodProfile;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class MusicMood {
    private Long id;
    private String title;
    private MoodProfile moodProfile;
    private String streamingUrl;

    public static MusicMood of(String title, MoodProfile moodProfile, String streamingUrl) {
        return MusicMood.builder()
                .title(title)
                .moodProfile(moodProfile)
                .streamingUrl(streamingUrl)
                .build();
    }

    public double fitnessFor(MoodProfile occasionMoodProfile) {
        return moodProfile.fitness(occasionMoodProfile);
    }

    public boolean fitsFor(MoodProfile occasionMoodProfile) {
        return moodProfile.fits(occasionMoodProfile);
    }
}
