package com.example.medialibrary.controller;

import com.example.medialibrary.DTO.MediaItemRequest;
import com.example.medialibrary.DTO.MediaItemResponse;
import com.example.medialibrary.service.MediaItemService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
public class MediaItemController {
    private final MediaItemService service;

    public MediaItemController(MediaItemService service) {
        this.service = service;
    }

    @GetMapping("/api/media-items/")
    public ResponseEntity<List<MediaItemResponse>> getAllMediaItems(Authentication authentication) {
        return new ResponseEntity<>(this.service.getAllMediaItems(authentication.getName()), HttpStatus.OK);
    }

    @GetMapping("/api/media-items/{id}")
    public ResponseEntity<MediaItemResponse> getMediaItem(@PathVariable int id, Authentication authentication) {
        return new ResponseEntity<>(this.service.getMediaItem(id, authentication.getName()), HttpStatus.OK);
    }

    @PostMapping("/api/media-items")
    public ResponseEntity<MediaItemResponse> createMediaItem(@RequestBody @Valid MediaItemRequest mediaItem, Authentication authentication) {
        return new ResponseEntity<>(this.service.createMediaItem(mediaItem, authentication.getName()), HttpStatus.CREATED);
    }

    @PutMapping("/api/media-items/{id}")
    public ResponseEntity<MediaItemResponse> updateMediaItem(@RequestBody @Valid MediaItemRequest mediaItem, @PathVariable int id, Authentication authentication) {
        return new ResponseEntity<>(this.service.updateMediaItem(mediaItem, id, authentication.getName()), HttpStatus.OK);
    }

    @DeleteMapping("/api/media-items/{id}")
    public ResponseEntity<Void> deleteMediaItem(@PathVariable int id, Authentication authentication) {
        this.service.deleteMediaItem(id, authentication.getName());
        return ResponseEntity.noContent().build();
    }
}
