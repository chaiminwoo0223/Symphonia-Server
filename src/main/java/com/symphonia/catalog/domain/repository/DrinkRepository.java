package com.symphonia.catalog.domain.repository;

import com.symphonia.catalog.domain.entity.Drink;
import com.symphonia.catalog.domain.vo.DrinkSource;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface DrinkRepository {
    Drink save(Drink drink);

    List<Drink> findAll();

    Optional<Drink> findById(Long drinkId);

    // 주어진 id의 음료를 한 번에 읽는다. 없는 id는 건너뛰고 순서는 정하지 않는다.
    List<Drink> findAllByIds(Collection<Long> drinkIds);

    List<Drink> findAllBySource(DrinkSource source);
}
