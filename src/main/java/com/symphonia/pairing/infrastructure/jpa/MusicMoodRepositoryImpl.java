package com.symphonia.pairing.infrastructure.jpa;

import com.symphonia.pairing.domain.entity.MusicMood;
import com.symphonia.pairing.domain.repository.MusicMoodRepository;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class MusicMoodRepositoryImpl implements MusicMoodRepository {
    private final MusicMoodJpaRepository musicMoodJpaRepository;

    @Override
    public List<MusicMood> findAll() {
        return musicMoodJpaRepository.findAll().stream().map(MusicMoodJpaEntity::toDomain).toList();
    }

    @Override
    public Optional<MusicMood> findById(Long musicMoodId) {
        return musicMoodJpaRepository.findById(musicMoodId).map(MusicMoodJpaEntity::toDomain);
    }
}
