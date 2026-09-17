package com.symphonia.pairing.domain.repository;

import com.symphonia.pairing.domain.entity.MusicMood;
import java.util.List;
import java.util.Optional;

public interface MusicMoodRepository {
    List<MusicMood> findAll();

    Optional<MusicMood> findById(Long musicMoodId);
}
