package com.symphonia.pairing.infrastructure.jpa;

import com.symphonia.pairing.domain.vo.MoodProfile;
import jakarta.persistence.Embeddable;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Embeddable
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class MoodProfileEmbeddable {
    private int formality;
    private int romance;
    private int celebration;
    private int comfort;

    public static MoodProfileEmbeddable from(MoodProfile moodProfile) {
        return new MoodProfileEmbeddable(
                moodProfile.getFormality(),
                moodProfile.getRomance(),
                moodProfile.getCelebration(),
                moodProfile.getComfort());
    }

    public MoodProfile toDomain() {
        return MoodProfile.of(formality, romance, celebration, comfort);
    }
}
