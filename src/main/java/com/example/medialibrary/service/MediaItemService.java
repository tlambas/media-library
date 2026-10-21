package com.example.medialibrary.service;

import com.example.medialibrary.DTO.MediaItemRequest;
import com.example.medialibrary.DTO.MediaItemResponse;
import com.example.medialibrary.exception.MediaItemNotFoundException;
import com.example.medialibrary.model.MediaItem;
import com.example.medialibrary.model.User;
import com.example.medialibrary.repository.MediaItemRepository;
import com.example.medialibrary.repository.UserRepository;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;


@Service
public class MediaItemService {
    private final MediaItemRepository mediaItemRepository;
    private final UserRepository userRepository;

    public MediaItemService(MediaItemRepository mediaItemRepository, UserRepository userRepository) {
        this.mediaItemRepository = mediaItemRepository;
        this.userRepository = userRepository;
    }

    public MediaItemResponse createMediaItem(MediaItemRequest req, String username) {
        User owner = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException(
                        "Authenticated user not found"));
        MediaItem newMediaItem = new MediaItem(req);
        newMediaItem.setOwner(owner);

        return new MediaItemResponse(this.mediaItemRepository.save(newMediaItem));
    }

    public List<MediaItemResponse> getAllMediaItems(String username) {
        List<MediaItemResponse> mediaItems = new ArrayList<>();
        this.mediaItemRepository.findByOwnerUsername(username)
                .forEach((mediaItem) -> mediaItems.add(new MediaItemResponse(mediaItem)));

        return mediaItems;
    }

    public MediaItemResponse getMediaItem(int id, String username) {
        return new MediaItemResponse(this.mediaItemRepository.findByIdAndOwnerUsername(id, username)
                .orElseThrow(() -> new MediaItemNotFoundException("Media item with id " + id + " not found.")));
    }

    @Transactional
    public MediaItemResponse updateMediaItem(MediaItemRequest req, int id, String username) {
        MediaItem item = mediaItemRepository
                .findByIdAndOwnerUsername(id, username)
                .orElseThrow(() -> new MediaItemNotFoundException(
                        "Media item with id " + id + " not found."
                ));

        if (req.getVersion() == null || !req.getVersion().equals(item.getVersion())) {
            throw new ObjectOptimisticLockingFailureException(MediaItem.class, id);
        }

        item.setTitle(req.getTitle());
        item.setDescription(req.getDescription());
        item.setType(req.getType());

        mediaItemRepository.flush();

        return new MediaItemResponse(item);
    }

    public void deleteMediaItem(int id, String username) {
        MediaItem deletedMediaItem = this.mediaItemRepository.findByIdAndOwnerUsername(id, username)
                .orElseThrow(() -> new MediaItemNotFoundException("Media item with id " + id + " not found."));
        this.mediaItemRepository.delete(deletedMediaItem);
    }
}
