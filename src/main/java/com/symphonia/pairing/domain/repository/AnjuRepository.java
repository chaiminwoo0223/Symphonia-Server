package com.symphonia.pairing.domain.repository;

import com.symphonia.pairing.domain.entity.Anju;
import com.symphonia.pairing.domain.vo.AnjuSource;
import java.util.List;
import java.util.Optional;

public interface AnjuRepository {
    Anju save(Anju anju);

    List<Anju> findAll();

    Optional<Anju> findById(Long anjuId);

    Optional<Anju> findBySourceAndExternalId(AnjuSource source, String externalId);
}
