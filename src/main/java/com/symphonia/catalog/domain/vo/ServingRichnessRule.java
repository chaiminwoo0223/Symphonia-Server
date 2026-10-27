package com.symphonia.catalog.domain.vo;

// 지방 구간으로 정한 무게감을 안주 형태에 따라 보정한다.
// 집어 먹는 안주는 100g당 지방이 높아도 한 번에 먹는 양이 적어 입안에 남는 기름기가 적기 때문이다.
// 튀긴 안주는 튀김 기름이 겉에 남아 형태가 같아도 따로 본다.
public record ServingRichnessRule(ServingStyle serving, boolean fried, int richnessOffset) {
    // 지방 구간의 가장 낮은 무게감이다. 보정해도 이보다 낮아지지 않는다.
    private static final int MIN_RICHNESS = 1;

    public boolean matches(CuratedAnju curatedAnju) {
        return serving == curatedAnju.serving() && fried == curatedAnju.fried();
    }

    public int apply(int richness) {
        return Math.max(MIN_RICHNESS, richness + richnessOffset);
    }
}
