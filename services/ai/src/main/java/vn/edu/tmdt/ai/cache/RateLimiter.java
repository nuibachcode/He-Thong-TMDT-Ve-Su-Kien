package vn.edu.tmdt.ai.cache;

import java.time.Duration;
import java.time.Instant;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import vn.edu.tmdt.ai.common.ApiException;
import vn.edu.tmdt.ai.config.AiProperties;

/**
 * Gioi han so tin nhan AI theo phut (fixed window) de bao ve quota free tier.
 */
@Component
public class RateLimiter {

    private final ReactiveStringRedisTemplate redis;
    private final AiProperties properties;

    public RateLimiter(ReactiveStringRedisTemplate redis, AiProperties properties) {
        this.redis = redis;
        this.properties = properties;
    }

    public Mono<Void> check(String identity) {
        if (!properties.rateLimit().enabled()) {
            return Mono.empty();
        }
        String key = "ai:rate:" + identity + ":" + Instant.now().getEpochSecond() / 60;
        return redis.opsForValue().increment(key)
                .flatMap(count -> {
                    Mono<Boolean> expire = count == 1
                            ? redis.expire(key, Duration.ofSeconds(70))
                            : Mono.just(Boolean.TRUE);
                    return expire.then(Mono.defer(() -> count > properties.rateLimit().perMinute()
                            ? Mono.error(ApiException.tooManyRequests(
                                    "Ban gui qua nhieu tin nhan, vui long thu lai sau 1 phut"))
                            : Mono.empty()));
                })
                .then()
                .onErrorResume(error -> error instanceof ApiException api
                        ? Mono.error(api)
                        : Mono.empty());
    }
}
