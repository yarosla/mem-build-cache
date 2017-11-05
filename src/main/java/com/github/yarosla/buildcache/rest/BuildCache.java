package com.github.yarosla.buildcache.rest;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.stats.CacheStats;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
public class BuildCache {
    private final static Logger logger = LoggerFactory.getLogger(BuildCache.class);

    private final Cache<String, CachedEntity> cache;

    public BuildCache(long memoryLimit) {
        logger.info("Creating LRU cache limited to {} bytes of content", memoryLimit);
        cache = Caffeine.newBuilder()
                .weigher((String key, CachedEntity entity) -> entity.content.length)
                .maximumWeight(memoryLimit)
                .recordStats()
                .build();
    }

    @RequestMapping(value = "/cache/**", method = RequestMethod.GET)
    public ResponseEntity<ByteArrayResource> get(HttpServletRequest request) throws UnsupportedEncodingException {
        String key = key(request);
        CachedEntity cachedEntity = cache.getIfPresent(key);
        if (cachedEntity != null) {
            logger.debug("get {} -> {} bytes", key, cachedEntity.content.length);
            return ResponseEntity.ok()
                    .contentType(cachedEntity.contentType)
                    .lastModified(cachedEntity.created)
                    .body(new ByteArrayResource(cachedEntity.content));
        } else {
            logger.debug("get {} -> not found", key);
            return ResponseEntity.notFound().build();
        }
    }

    @RequestMapping(value = "/cache/**", method = RequestMethod.PUT)
    public void put(HttpServletRequest request, HttpEntity<byte[]> requestEntity) throws UnsupportedEncodingException {
        String key = key(request);
        byte[] body = requestEntity.getBody();
        MediaType contentType = requestEntity.getHeaders().getContentType();
        logger.debug("put {} <- {} bytes", key, body.length);
        cache.put(key, new CachedEntity(contentType, body, System.currentTimeMillis()));
    }

    @RequestMapping(value = "/stats", method = RequestMethod.GET)
    public Statistics statistics() {
        logger.debug("collecting statistics");
        return new Statistics(cache);
    }

    private String key(HttpServletRequest request) throws UnsupportedEncodingException {
        return URLDecoder.decode(request.getPathInfo(), "UTF-8").substring(7); // 7 == "/cache/".length()
    }

    private static class CachedEntity {
        private MediaType contentType;
        private byte[] content;
        private long created;

        CachedEntity(MediaType contentType, byte[] content, long created) {
            this.contentType = contentType;
            this.content = content;
            this.created = created;
        }
    }

    @SuppressWarnings("unused")
    public static class Statistics {
        private long currentCount;
        private long currentWeight;
        private long evictionCount;
        private long evictionWeight;
        private long hitCount;
        private long missCount;
        private long requestCount;
        private List<String> hottest;

        public Statistics() {
        }

        Statistics(Cache<String, CachedEntity> cache) {
            CacheStats stats = cache.stats();
            currentCount = cache.estimatedSize();
            evictionCount = stats.evictionCount();
            evictionWeight = stats.evictionWeight();
            hitCount = stats.hitCount();
            missCount = stats.missCount();
            requestCount = stats.requestCount();
            cache.policy().eviction().ifPresent(eviction -> {
                currentWeight = eviction.weightedSize().orElse(0);
                hottest = eviction.hottest(10).entrySet().stream().map(Map.Entry::getKey).collect(Collectors.toList());
            });
        }

        public long getCurrentCount() {
            return currentCount;
        }

        public long getCurrentWeight() {
            return currentWeight;
        }

        public long getEvictionCount() {
            return evictionCount;
        }

        public long getEvictionWeight() {
            return evictionWeight;
        }

        public long getHitCount() {
            return hitCount;
        }

        public long getMissCount() {
            return missCount;
        }

        public long getRequestCount() {
            return requestCount;
        }

        public List<String> getHottest() {
            return hottest;
        }
    }
}
