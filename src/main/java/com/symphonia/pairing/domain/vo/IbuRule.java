package com.symphonia.pairing.domain.vo;

// IBU 구간 [minIbu, maxIbu)를 쓴맛 값으로 바꾼다.
public record IbuRule(double minIbu, double maxIbu, int bitterness) {
    public boolean contains(double ibu) {
        return ibu >= minIbu && ibu < maxIbu;
    }
}
