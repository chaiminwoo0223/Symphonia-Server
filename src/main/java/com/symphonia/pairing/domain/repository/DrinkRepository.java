package com.symphonia.pairing.domain.repository;

import com.symphonia.pairing.domain.entity.Drink;
import java.util.List;
import java.util.Optional;

public interface DrinkRepository {
    List<Drink> findAll();

    Optional<Drink> findById(Long drinkId);
}
