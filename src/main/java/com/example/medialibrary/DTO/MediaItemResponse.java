package com.example.medialibrary.DTO;

import com.example.medialibrary.model.MediaItem;
import com.example.medialibrary.model.MediaType;
import com.example.medialibrary.model.User;

import java.util.Date;

public class MediaItemResponse {
    private Integer id;
    private String title;
    private String description;
    private MediaType type;
    private Date createdAt;
    private Date updatedAt;
    private Long version;

    public MediaItemResponse(MediaItem mediaItem) {
        this.id = mediaItem.getId();
        this.title = mediaItem.getTitle();
        this.description = mediaItem.getDescription();
        this.type = mediaItem.getType();
        this.createdAt = mediaItem.getCreatedAt();
        this.updatedAt = mediaItem.getUpdatedAt();
        this.version = mediaItem.getVersion();
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public MediaType getType() {
        return type;
    }

    public void setType(MediaType type) {
        this.type = type;
    }

    public Date getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
    }

    public Date getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Date updatedAt) {
        this.updatedAt = updatedAt;
    }

    public Long getVersion() { return version; }

    public void setVersion(Long version) { this.version = version; }
}
