package com.symphonia.catalog.domain.repository;

import com.symphonia.catalog.domain.entity.Anju;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface AnjuRepository {
    Anju save(Anju anju);

    List<Anju> findAll();

    Optional<Anju> findById(Long anjuId);

    void deleteAllById(Collection<Long> anjuIds);
}
