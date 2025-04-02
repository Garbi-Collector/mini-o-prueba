package com.example.minioprueba.repositories;

import com.example.minioprueba.entities.AudioFileEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface AudioFileRepository extends JpaRepository<AudioFileEntity,Long> {
    Optional<AudioFileEntity> findById(Long id);
    Page<AudioFileEntity> findAll(Pageable pageable);

}
