package com.github.yarosla.buildcache;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;

class BuildCache {
    private final static Logger logger = LoggerFactory.getLogger(BuildCache.class);

    static final String PATH_VAR_NAME = "path";

    private final Cache<String, CachedEntity> cache;

    BuildCache(long memoryLimit) {
        logger.info("Creating LRU cache limited to {} bytes of content", memoryLimit);
        cache = Caffeine.newBuilder()
                .weigher((String key, CachedEntity entity) -> entity.content.length)
                .maximumWeight(memoryLimit)
                .recordStats()
                .build();
    }

    Mono<ServerResponse> get(ServerRequest request) {
        String key = key(request);
        CachedEntity cachedEntity = cache.getIfPresent(key);
        if (cachedEntity != null) {
            logger.debug("get {} -> {} bytes", key, cachedEntity.content.length);
            return ServerResponse.ok()
                    .contentType(cachedEntity.contentType)
                    .contentLength(cachedEntity.content.length)
                    .lastModified(ZonedDateTime.ofInstant(Instant.ofEpochMilli(cachedEntity.timestamp), ZoneOffset.UTC))
                    .body(BodyInserters.fromObject(cachedEntity.content));
        } else {
            logger.debug("get {} -> not found", key);
            return ServerResponse.notFound().build();
        }
    }

    Mono<ServerResponse> put(ServerRequest request) {
        String key = key(request);
        MediaType contentType = request.headers().contentType().orElse(MediaType.APPLICATION_OCTET_STREAM);
        long timestamp = System.currentTimeMillis();
        return request.bodyToMono(ByteArrayResource.class)
                .map(ByteArrayResource::getByteArray)
                .flatMap(bytes -> {
                    logger.debug("put {} <- {} bytes", key, bytes.length);
                    cache.put(key, new CachedEntity(contentType, bytes, timestamp));
                    return ServerResponse.ok().build();
                });
    }

    Mono<ServerResponse> statistics(@SuppressWarnings("unused") ServerRequest request) {
        logger.debug("collecting statistics");
        return ServerResponse.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .body(BodyInserters.fromObject(new CacheStatistics(cache)));
    }

    private String key(ServerRequest request) {
        return request.pathVariable(PATH_VAR_NAME);
    }

    private static class CachedEntity {
        private MediaType contentType;
        private byte[] content;
        private long timestamp;

        CachedEntity(MediaType contentType, byte[] content, long timestamp) {
            this.contentType = contentType;
            this.content = content;
            this.timestamp = timestamp;
        }
    }
}
