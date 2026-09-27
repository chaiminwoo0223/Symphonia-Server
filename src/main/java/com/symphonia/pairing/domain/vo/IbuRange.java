package com.symphonia.pairing.domain.vo;

public record IbuRange(double min, double max) {
    public double midpoint() {
        return (min + max) / 2;
    }
}
