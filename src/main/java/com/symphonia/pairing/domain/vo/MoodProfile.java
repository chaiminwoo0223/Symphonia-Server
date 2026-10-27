package com.symphonia.pairing.domain.vo;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class MoodProfile {
    private static final int MAX_AXIS_VALUE = 5;
    private static final double FIT_THRESHOLD = 0.7;

    private final int formality;
    private final int romance;
    private final int celebration;
    private final int comfort;

    public static MoodProfile of(int formality, int romance, int celebration, int comfort) {
        return new MoodProfile(formality, romance, celebration, comfort);
    }

    public MoodProfile withFormality(int delta) {
        return MoodProfile.of(
                clamp(this.formality + delta), this.romance, this.celebration, this.comfort);
    }

    public MoodProfile withRomance(int delta) {
        return MoodProfile.of(
                this.formality, clamp(this.romance + delta), this.celebration, this.comfort);
    }

    public double fitness(MoodProfile other) {
        return AxisDistance.similarity(
                new int[] {formality, romance, celebration, comfort},
                new int[] {other.formality, other.romance, other.celebration, other.comfort});
    }

    public boolean fits(MoodProfile other) {
        return fitness(other) >= FIT_THRESHOLD;
    }

    private static int clamp(int value) {
        return Math.clamp(value, 0, MAX_AXIS_VALUE);
    }
}
