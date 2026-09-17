package com.symphonia.pairing.infrastructure.jpa;

import com.symphonia.pairing.domain.entity.MusicMood;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Entity
@Getter
@SuperBuilder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MusicMoodJpaEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    @Embedded private MoodProfileEmbeddable moodProfile;

    @Column(nullable = false)
    private String streamingUrl;

    public static MusicMoodJpaEntity from(MusicMood musicMood) {
        return MusicMoodJpaEntity.builder()
                .id(musicMood.getId())
                .title(musicMood.getTitle())
                .moodProfile(MoodProfileEmbeddable.from(musicMood.getMoodProfile()))
                .streamingUrl(musicMood.getStreamingUrl())
                .build();
    }

    public MusicMood toDomain() {
        return MusicMood.builder()
                .id(id)
                .title(title)
                .moodProfile(moodProfile.toDomain())
                .streamingUrl(streamingUrl)
                .build();
    }
}
