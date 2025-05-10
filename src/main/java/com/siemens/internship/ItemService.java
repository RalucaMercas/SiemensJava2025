package com.siemens.internship;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

@Service
public class ItemService {
    @Autowired
    private ItemRepository itemRepository;
    private final List<Item> processedItems = new CopyOnWriteArrayList<>();
    private final AtomicInteger processedCount = new AtomicInteger(0);

    public List<Item> findAll() {
        return itemRepository.findAll();
    }

    public Optional<Item> findById(Long id) {
        return itemRepository.findById(id);
    }

    public Item save(Item item) {
        return itemRepository.save(item);
    }

    public void deleteById(Long id) {
        itemRepository.deleteById(id);
    }


    /**
     * Your Tasks
     * Identify all concurrency and asynchronous programming issues in the code
     * Fix the implementation to ensure:
     * All items are properly processed before the CompletableFuture completes
     * Thread safety for all shared state
     * Proper error handling and propagation
     * Efficient use of system resources
     * Correct use of Spring's @Async annotation
     * Add appropriate comments explaining your changes and why they fix the issues
     * Write a brief explanation of what was wrong with the original implementation
     *
     * Hints
     * Consider how CompletableFuture composition can help coordinate multiple async operations
     * Think about appropriate thread-safe collections
     * Examine how errors are handled and propagated
     * Consider the interaction between Spring's @Async and CompletableFuture
     */

    // 1. the original method returned "processedItems" right after the asynchronous processing started,
    // without waiting for them to finish, so the result was an empty list
    // 2. "processedItems" and "processedCount" were not thread-safe, so for example the operation
    // processedCount++ was not atomic
    // 3. Potential errors were just printed in console
    // 4. The method was annotated with @Async, which means it would run in a separate thread,
    // but using CompletableFuture with a custom ExecutorService interferes with Spring's processing
    // 5. Executor Service was never shut down, which can lead to memory leaks


    // Solution:
    // 1. Use a CompletableFuture list and wait for all threads to finish with "allOf"
    // 2. Changed "processedItems" type to be CopyOnWriteArrayList and "processedCount" to be an AtomicInteger,
    // which are thread-safe collections
    // 3. Let Spring manage the async processing with @Async, so we don't need to use a custom ExecutorService
    // 4. Catch exceptions and propagate them properly

    @Async
    public CompletableFuture<List<Item>> processItemsAsync() {
        processedItems.clear();
        processedCount.set(0);

        List<Long> itemIds = itemRepository.findAllIds();

        List<CompletableFuture<Void>> futures = itemIds.stream()
                .map(id -> CompletableFuture.runAsync(() -> {
                    try {
                        Thread.sleep(100);

                        Item item = itemRepository.findById(id).orElse(null);
                        if (item == null) {
                            return;
                        }

                        processedCount.incrementAndGet();

                        item.setStatus("PROCESSED");
                        itemRepository.save(item);
                        processedItems.add(item);

                    } catch (Exception e) {
                        throw new RuntimeException("Failed to process item with id: " + id, e);
                    }
                }))
                .collect(Collectors.toList());

        return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).handle((result, ex) -> {
                    if (ex != null) {
                        throw new RuntimeException("Failed to process items", ex);
                    }
                    return new ArrayList<>(processedItems);
                });
    }
}

