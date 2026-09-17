package com.symphonia.pairing.domain.vo;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public enum MoodType {
    FORMAL(MoodProfile.of(5, 0, 1, 1)),
    CASUAL(MoodProfile.of(1, 1, 2, 4)),
    ROMANTIC(MoodProfile.of(2, 5, 2, 3)),
    CELEBRATORY(MoodProfile.of(2, 1, 5, 2)),
    ;

    private final MoodProfile baseMoodProfile;

    public MoodProfile baseMoodProfile() {
        return baseMoodProfile;
    }
}
