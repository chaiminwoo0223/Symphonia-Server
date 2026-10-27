package com.symphonia.catalog.domain.vo;

public enum AnjuSource {
    SEED,
    K_FIND;

    // 시드 행은 마이그레이션이 소유하므로, 매핑표에서 빠져도 적재가 지우지 않는다.
    public boolean isManagedByImport() {
        return this == K_FIND;
    }
}
