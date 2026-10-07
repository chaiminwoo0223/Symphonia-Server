package com.symphonia.pairing.domain.repository;

import com.symphonia.pairing.domain.entity.Drink;
import com.symphonia.pairing.domain.vo.DrinkSource;
import java.util.List;
import java.util.Optional;

public interface DrinkRepository {
    Drink save(Drink drink);

    List<Drink> findAll();

    Optional<Drink> findById(Long drinkId);

    Optional<Drink> findBySourceAndExternalId(DrinkSource source, String externalId);
}
