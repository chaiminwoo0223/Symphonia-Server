package com.symphonia.pairing.domain.repository;

import com.symphonia.pairing.domain.entity.DrinkStyle;
import com.symphonia.pairing.domain.vo.DrinkStyleSource;
import java.util.Optional;

public interface DrinkStyleRepository {
    DrinkStyle save(DrinkStyle drinkStyle);

    Optional<DrinkStyle> findById(Long drinkStyleId);

    Optional<DrinkStyle> findBySourceAndExternalId(DrinkStyleSource source, String externalId);
}
