package com.symphonia.pairing.domain.repository;

import com.symphonia.pairing.domain.entity.Anju;
import java.util.List;
import java.util.Optional;

public interface AnjuRepository {
    List<Anju> findAll();

    Optional<Anju> findById(Long anjuId);
}
