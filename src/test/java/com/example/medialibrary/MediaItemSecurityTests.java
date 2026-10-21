package com.example.medialibrary;

import com.example.medialibrary.model.MediaItem;
import com.example.medialibrary.model.User;
import com.example.medialibrary.repository.MediaItemRepository;
import com.example.medialibrary.repository.UserRepository;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;


import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class MediaItemSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private MediaItemRepository mediaItemRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void getMediaItemWithoutAuthenticationReturns401() throws Exception {
        mockMvc.perform(get("/api/media-items/1"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @Transactional
    void userCannotRetrieveAnotherUsersMediaItem() throws Exception {
        User owner = userRepository.findByUsername("testuser").orElseThrow();

        MediaItem testItem = new MediaItem();
        testItem.setTitle("test");
        testItem.setOwner(owner);

        MediaItem savedItem = mediaItemRepository.save(testItem);

        mockMvc.perform(get("/api/media-items/" + savedItem.getId())
                        .with(httpBasic("testuser2", "test2")))
                .andExpect(status().isNotFound());
    }

    @Test
    @Transactional
    void ownerCanRetrieveTheirOwnMediaItem() throws Exception {
        User owner = userRepository.findByUsername("testuser").orElseThrow();

        MediaItem testItem = new MediaItem();
        testItem.setTitle("test");
        testItem.setOwner(owner);

        MediaItem savedItem = mediaItemRepository.save(testItem);

        mockMvc.perform(get("/api/media-items/" + savedItem.getId())
                        .with(httpBasic("testuser", "test")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(savedItem.getId()))
                .andExpect(jsonPath("$.title").value("test"));
    }

    @Test
    void createMediaItemWithoutAuthenticationReturns401() throws Exception {
        mockMvc.perform(post("/api/media-items")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"title\":\"Test Movie\",\"type\":\"MOVIE\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @Transactional
    void createMediaItemAssignsAuthenticatedUserAsOwner() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/media-items")
                        .with(httpBasic("testuser", "test"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
            {
                "title": "Test Movie",
                "type": "MOVIE"
            }
            """))
                .andExpect(status().isCreated())
                .andReturn();

        String responseJson = result.getResponse().getContentAsString();
        JsonNode responseNode = objectMapper.readTree(responseJson);
        int mediaItemId = responseNode.get("id").asInt();

        MediaItem savedItem = mediaItemRepository.findById(mediaItemId)
                .orElseThrow();

        assertEquals("testuser", savedItem.getOwner().getUsername());
    }

    @Test
    @Transactional
    void createMediaItemIgnoresClientSuppliedOwner() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/media-items")
                        .with(httpBasic("testuser", "test"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
            {
                "title": "Test Movie",
                "type": "MOVIE",
                  "owner": {
                    "username": "testuser2"
                  }
            }
            """))
                .andExpect(status().isCreated())
                .andReturn();

        String responseJson = result.getResponse().getContentAsString();
        JsonNode responseNode = objectMapper.readTree(responseJson);
        int mediaItemId = responseNode.get("id").asInt();

        MediaItem savedItem = mediaItemRepository.findById(mediaItemId)
                .orElseThrow();

        assertEquals("testuser", savedItem.getOwner().getUsername());
    }

    @Test
    void staleUpdateShouldReturn409Conflict() throws Exception {

        // Create a media item through the API.
        MvcResult createResult = mockMvc.perform(
                        post("/api/media-items")
                                .with(httpBasic("testuser", "test"))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                    {
                        "title": "Original Album",
                        "type": "ALBUM"
                    }
                    """))
                .andExpect(status().isCreated())
                .andReturn();

        JsonNode created = objectMapper.readTree(
                createResult.getResponse().getContentAsString()
        );

        int id = created.get("id").asInt();
        long originalVersion = created.get("version").asLong();

        try {
            // First client updates successfully.
            mockMvc.perform(
                            put("/api/media-items/" + id)
                                    .with(httpBasic("testuser", "test"))
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content("""
                        {
                            "title": "Updated by A",
                            "type": "ALBUM",
                            "version": %d
                        }
                        """.formatted(originalVersion)))
                    .andExpect(status().isOk());

            // Second client submits the original, stale version.
            mockMvc.perform(
                            put("/api/media-items/" + id)
                                    .with(httpBasic("testuser", "test"))
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content("""
                        {
                            "title": "Updated by B",
                            "type": "ALBUM",
                            "version": %d
                        }
                        """.formatted(originalVersion)))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.status").value(409))
                    .andExpect(jsonPath("$.title")
                            .value("Concurrent Modification"));

            // Verify the rejected update did not overwrite A's changes.
            mockMvc.perform(
                            get("/api/media-items/" + id)
                                    .with(httpBasic("testuser", "test")))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.title")
                            .value("Updated by A"))
                    .andExpect(jsonPath("$.version")
                            .value(originalVersion + 1));

        } finally {
            // Clean up test data.
            mockMvc.perform(
                            delete("/api/media-items/" + id)
                                    .with(httpBasic("testuser", "test")))
                    .andExpect(status().isNoContent());
        }
    }
}