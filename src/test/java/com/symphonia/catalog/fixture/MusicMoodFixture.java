package com.symphonia.catalog.fixture;

import com.symphonia.catalog.domain.entity.MusicMood;
import com.symphonia.catalog.domain.vo.MoodProfile;
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
            "https://open.spotify.com/playlist/celebratory-dance"),
    CASUAL_LOFI(
            "로파이 힙합", MoodProfile.of(1, 0, 1, 5), "https://open.spotify.com/playlist/casual-lofi");

    private final String title;
    private final MoodProfile moodProfile;
    private final String streamingUrl;

    public MusicMood create() {
        return build(null, moodProfile);
    }

    public MusicMood createWithId() {
        return createWithId(ordinal() + 1L);
    }

    public MusicMood createWithId(Long id) {
        return build(id, moodProfile);
    }

    public MusicMood createWithMoodProfile(MoodProfile profile) {
        return build(ordinal() + 1L, profile);
    }

    // 축마다 값이 다른 분위기를 넣어 결과로 옮긴 축의 순서를 검증할 때 쓴다.
    public MusicMood createWithIdAndMoodProfile(Long id, MoodProfile profile) {
        return build(id, profile);
    }

    private MusicMood build(Long id, MoodProfile profile) {
        return MusicMood.builder()
                .id(id)
                .title(title)
                .moodProfile(profile)
                .streamingUrl(streamingUrl)
                .build();
    }
}
