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
    private static final int AXIS_COUNT = 4;
    private static final double MAX_DISTANCE_SQUARED =
            AXIS_COUNT * (double) (MAX_AXIS_VALUE * MAX_AXIS_VALUE);

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
        int formalityDiff = this.formality - other.formality;
        int romanceDiff = this.romance - other.romance;
        int celebrationDiff = this.celebration - other.celebration;
        int comfortDiff = this.comfort - other.comfort;

        double distanceSquared =
                (double) formalityDiff * formalityDiff
                        + (double) romanceDiff * romanceDiff
                        + (double) celebrationDiff * celebrationDiff
                        + (double) comfortDiff * comfortDiff;

        return 1 - Math.sqrt(distanceSquared / MAX_DISTANCE_SQUARED);
    }

    private static int clamp(int value) {
        return Math.clamp(value, 0, MAX_AXIS_VALUE);
    }
}
