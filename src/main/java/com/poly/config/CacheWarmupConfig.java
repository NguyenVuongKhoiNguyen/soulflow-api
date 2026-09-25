package com.poly.config;

import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.cache.CacheManager;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.EventListener;
import org.springframework.cache.Cache;

import com.poly.models.services.CommentService;
import com.poly.models.services.ProductService;
import com.poly.models.services.ReplyService;
import com.poly.models.enums.SortOrder;

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
            // Invoke cacheable methods through their Spring proxies. Calling them
            // from warmup methods on the same service bypasses cache interception.
            for (int pageNumber = 0; pageNumber < 10; pageNumber++) {
                productService.filterAndPaginateProducts(
                    null, null, null, null, null, null, null, null, null,
                    SortOrder.DESC, pageNumber, 5
                );
                commentService.filterAndPaginateComments(
                    null, null, null, null, null, null, null,
                    SortOrder.DESC, pageNumber, 5
                );
                replyService.filterAndPaginateReply(
                    null, null, null, null, null, null, null, null,
                    SortOrder.DESC, pageNumber, 5
                );
            }
            System.out.println("Cache Warmup Completed");
        } catch (Exception e) {
            // TODO: handle exception
            e.printStackTrace();
        }
    }
}
