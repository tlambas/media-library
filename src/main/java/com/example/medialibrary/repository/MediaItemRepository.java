package com.example.medialibrary.repository;

import com.example.medialibrary.model.MediaItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface MediaItemRepository extends JpaRepository<MediaItem, Integer> {
    Optional<MediaItem> findByIdAndOwnerUsername(Integer id, String username);
    List<MediaItem> findByOwnerUsername(String username);
}
