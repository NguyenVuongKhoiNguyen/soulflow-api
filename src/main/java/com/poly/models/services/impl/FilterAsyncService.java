package com.poly.models.services.impl;

import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class FilterAsyncService {

    private static final Logger log = LoggerFactory.getLogger(FilterAsyncService.class);

    @Async("filterTaskExecutor")
    public <T> CompletableFuture<T> run(Supplier<T> work) {
        log.info("Running filter request on thread {}", Thread.currentThread().getName());
        return CompletableFuture.completedFuture(work.get());
    }
}
