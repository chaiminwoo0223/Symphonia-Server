package com.symphonia.pairing.domain.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MoodProfile {
    private static final int MAX_AXIS_VALUE = 5;
    private static final double FIT_THRESHOLD = 0.7;

    private int formality;
    private int romance;
    private int celebration;
    private int comfort;

    public static MoodProfile of(int formality, int romance, int celebration, int comfort) {
        return MoodProfile.builder()
                .formality(formality)
                .romance(romance)
                .celebration(celebration)
                .comfort(comfort)
                .build();
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
