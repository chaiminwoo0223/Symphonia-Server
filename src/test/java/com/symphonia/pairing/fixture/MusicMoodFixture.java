package com.symphonia.pairing.fixture;

import com.symphonia.pairing.domain.entity.MusicMood;
import com.symphonia.pairing.domain.vo.MoodProfile;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum MusicMoodFixture {
    FORMAL_JAZZ(
            "잔잔한 재즈", MoodProfile.of(5, 1, 1, 2), "https://open.spotify.com/playlist/formal-jazz"),
    CASUAL_ACOUSTIC(
            "편안한 어쿠스틱",
            MoodProfile.of(1, 1, 2, 5),
            "https://open.spotify.com/playlist/casual-acoustic"),
    CELEBRATORY_DANCE(
            "신나는 파티 댄스",
            MoodProfile.of(1, 1, 5, 2),
            "https://open.spotify.com/playlist/celebratory-dance");

    private final String title;
    private final MoodProfile moodProfile;
    private final String streamingUrl;

    public MusicMood create() {
        return create(moodProfile);
    }

    public MusicMood create(MoodProfile moodProfile) {
        return MusicMood.builder()
                .title(title)
                .moodProfile(moodProfile)
                .streamingUrl(streamingUrl)
                .build();
    }
}
