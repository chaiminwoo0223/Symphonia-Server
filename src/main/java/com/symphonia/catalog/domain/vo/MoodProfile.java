package com.symphonia.catalog.domain.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MoodProfile {
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
}
