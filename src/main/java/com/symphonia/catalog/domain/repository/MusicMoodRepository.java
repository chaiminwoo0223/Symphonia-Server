package com.symphonia.catalog.domain.repository;

import com.symphonia.catalog.domain.entity.MusicMood;
import java.util.List;
import java.util.Optional;

public interface MusicMoodRepository {
    List<MusicMood> findAll();

    Optional<MusicMood> findById(Long musicMoodId);
}
