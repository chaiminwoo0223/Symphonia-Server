package com.symphonia.pairing.domain.repository;

import com.symphonia.pairing.domain.entity.DrinkStyle;
import com.symphonia.pairing.domain.vo.DrinkStyleSource;
import java.util.List;
import java.util.Optional;

public interface DrinkStyleRepository {
    DrinkStyle save(DrinkStyle drinkStyle);

    Optional<DrinkStyle> findById(Long drinkStyleId);

    List<DrinkStyle> findAllBySource(DrinkStyleSource source);
}
