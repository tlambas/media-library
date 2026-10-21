package com.example.medialibrary.DTO;

import com.example.medialibrary.model.MediaItem;
import com.example.medialibrary.model.MediaType;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.validation.constraints.NotBlank;

public class MediaItemRequest {
    @NotBlank
    private String title;
    private String description;
    @Enumerated(EnumType.STRING)
    private MediaType type;
    private Long version;

    public MediaItemRequest() {

    }

    public MediaItemRequest(MediaItem mediaItem) {
        this.title = mediaItem.getTitle();
        this.description = mediaItem.getDescription();
        this.type = mediaItem.getType();
    }

    public String getTitle() {
        return this.title;
    }

    public String getDescription() {
        return this.description;
    }

    public MediaType getType() {
        return this.type;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setType(MediaType type) {
        this.type = type;
    }

    public Long getVersion() { return version; }

    public void setVersion(Long version) { this.version = version; }
}
