package com.example.medialibrary;

import com.example.medialibrary.DTO.MediaItemRequest;
import com.example.medialibrary.DTO.MediaItemResponse;
import com.example.medialibrary.model.MediaItem;
import com.example.medialibrary.model.MediaType;
import com.example.medialibrary.repository.MediaItemRepository;

import com.example.medialibrary.service.MediaItemService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.concurrent.*;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
class MediaItemConcurrencyTest {

    @Autowired
    private MediaItemRepository repository;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @Autowired
    private MediaItemService mediaItemService;

    @Test
    void concurrentUpdatesShouldRejectStaleVersion() throws Exception {

        TransactionTemplate transaction =
                new TransactionTemplate(transactionManager);

        // Create an item before starting concurrent transactions.
        Integer id = transaction.execute(status -> {
            MediaItem item = new MediaItem();
            item.setTitle("Original Album");
            item.setType(MediaType.ALBUM);

            return repository.save(item).getId();
        });

        CountDownLatch bothLoaded = new CountDownLatch(2);
        CountDownLatch allowFirstCommit = new CountDownLatch(1);
        CountDownLatch firstCommitted = new CountDownLatch(1);

        ExecutorService executor = Executors.newFixedThreadPool(2);

        try {
            Future<?> first = executor.submit(() -> {
                try {
                    transaction.executeWithoutResult(status -> {
                        MediaItem item = repository.findById(id)
                                .orElseThrow();

                        bothLoaded.countDown();
                        await(bothLoaded);

                        item.setTitle("Updated by A");
                    });
                } finally {
                    firstCommitted.countDown();
                }
            });

            Future<?> second = executor.submit(() -> {
                transaction.executeWithoutResult(status -> {
                    MediaItem item = repository.findById(id)
                            .orElseThrow();

                    bothLoaded.countDown();
                    await(bothLoaded);

                    await(allowFirstCommit);
                    await(firstCommitted);

                    item.setTitle("Updated by B");
                });
            });

            assertTrue(bothLoaded.await(10, TimeUnit.SECONDS));

            // Allow transaction A to finish before B.
            allowFirstCommit.countDown();

            first.get(10, TimeUnit.SECONDS);

            ExecutionException exception = assertThrows(
                    ExecutionException.class,
                    () -> second.get(10, TimeUnit.SECONDS)
            );

            assertTrue(
                    exception.getCause() instanceof
                            org.springframework.orm.ObjectOptimisticLockingFailureException
            );

            MediaItem saved = repository.findById(id)
                    .orElseThrow();

            assertEquals("Updated by A", saved.getTitle());
            assertEquals(1L, saved.getVersion());

        } finally {
            executor.shutdownNow();

            transaction.executeWithoutResult(status ->
                    repository.deleteById(id)
            );
        }
    }

    @Test
    void staleClientUpdateShouldBeRejected() {

        String username = "testuser";

        MediaItemRequest createRequest = new MediaItemRequest();
        createRequest.setTitle("Original Album");
        createRequest.setType(MediaType.ALBUM);

        MediaItemResponse created =
                mediaItemService.createMediaItem(
                        createRequest, username
                );

        Integer id = created.getId();
        Long originalVersion = created.getVersion();

        try {
            // Client A and B both originally saw version 0.

            MediaItemRequest updateA = new MediaItemRequest();
            updateA.setTitle("Updated by A");
            updateA.setType(MediaType.ALBUM);
            updateA.setVersion(originalVersion);

            mediaItemService.updateMediaItem(
                    updateA, id, username
            );

            // Client B submits an update using the old version.
            MediaItemRequest updateB = new MediaItemRequest();
            updateB.setTitle("Updated by B");
            updateB.setType(MediaType.ALBUM);
            updateB.setVersion(originalVersion);

            assertThrows(
                    ObjectOptimisticLockingFailureException.class,
                    () -> mediaItemService.updateMediaItem(
                            updateB, id, username
                    )
            );

            MediaItem saved = repository.findById(id)
                    .orElseThrow();

            assertEquals("Updated by A", saved.getTitle());
            assertEquals(
                    originalVersion + 1,
                    saved.getVersion()
            );

        } finally {
            mediaItemService.deleteMediaItem(id, username);
        }
    }

    private static void await(CountDownLatch latch) {
        try {
            if (!latch.await(10, TimeUnit.SECONDS)) {
                throw new IllegalStateException(
                        "Timed out waiting for other transaction"
                );
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(
                    "Thread interrupted", e
            );
        }
    }
}