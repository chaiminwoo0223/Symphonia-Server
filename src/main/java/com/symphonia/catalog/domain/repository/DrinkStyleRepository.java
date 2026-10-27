package com.symphonia.catalog.domain.repository;

import com.symphonia.catalog.domain.entity.DrinkStyle;
import com.symphonia.catalog.domain.vo.DrinkStyleSource;
import java.util.List;
import java.util.Optional;

public interface DrinkStyleRepository {
    DrinkStyle save(DrinkStyle drinkStyle);

    Optional<DrinkStyle> findById(Long drinkStyleId);

    List<DrinkStyle> findAllBySource(DrinkStyleSource source);
}
