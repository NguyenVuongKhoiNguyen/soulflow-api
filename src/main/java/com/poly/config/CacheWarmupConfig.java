package com.poly.config;

import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.cache.CacheManager;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.EventListener;
import org.springframework.cache.Cache;

import com.poly.models.services.CommentService;
import com.poly.models.services.ProductService;
import com.poly.models.services.ReplyService;

import lombok.RequiredArgsConstructor;

@Configuration
@RequiredArgsConstructor
public class CacheWarmupConfig {

    private final CacheManager cacheManager;
    
    private final ProductService productService;

    private final CommentService commentService;

    private final ReplyService replyService;

    @EventListener(ApplicationReadyEvent.class)
    public void warmupCache() {

        try {
            //clear all cache
            cacheManager.getCacheNames().forEach(
                (name) -> {
                    Cache cache = cacheManager.getCache(name);
                    if (cache != null)
                        cache.clear();
                }
            );
            //warm up
            productService.warmupCache(10);
            commentService.warmupCache(10);
            replyService.warmupCache(10);
            System.out.println("Cache Warmup Completed");
        } catch (Exception e) {
            // TODO: handle exception
            e.printStackTrace();
        }
    }
}
