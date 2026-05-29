package io.snortexware.sisflow.config;

import org.springframework.cache.CacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;

import java.time.Duration;

@Configuration
public class RedisCacheConfig {

    @Bean
    RedisCacheConfiguration redisCacheConfiguration() {
        return baseConfiguration(Duration.ofMinutes(10));
    }

    @Bean
    CacheManager cacheManager(RedisConnectionFactory redisConnectionFactory) {
        return RedisCacheManager.builder(redisConnectionFactory)
                .cacheDefaults(baseConfiguration(Duration.ofMinutes(10)))
                .withCacheConfiguration("permissions", baseConfiguration(Duration.ofMinutes(15)))
                .withCacheConfiguration("roles", baseConfiguration(Duration.ofMinutes(15)))
                .withCacheConfiguration("rolePermissions", baseConfiguration(Duration.ofMinutes(15)))
                .withCacheConfiguration("userPermissions", baseConfiguration(Duration.ofMinutes(5)))
                .withCacheConfiguration("userRoles", baseConfiguration(Duration.ofMinutes(5)))
                .withCacheConfiguration("ticketConfigs", baseConfiguration(Duration.ofMinutes(30)))
                .withCacheConfiguration("agentGroups", baseConfiguration(Duration.ofMinutes(10)))
                .withCacheConfiguration("knowledgeBase", baseConfiguration(Duration.ofMinutes(10)))
                .withCacheConfiguration("ticketKnowledgeBase", baseConfiguration(Duration.ofMinutes(10)))
                .withCacheConfiguration("customers", baseConfiguration(Duration.ofMinutes(10)))
                .withCacheConfiguration("projects", baseConfiguration(Duration.ofMinutes(10)))
                .withCacheConfiguration("systems", baseConfiguration(Duration.ofMinutes(10)))
                .withCacheConfiguration("githubConfigurations", baseConfiguration(Duration.ofMinutes(10)))
                .withCacheConfiguration("slas", baseConfiguration(Duration.ofMinutes(30)))
                .withCacheConfiguration("customerAccesses", baseConfiguration(Duration.ofMinutes(5)))
                .build();
    }

    private RedisCacheConfiguration baseConfiguration(Duration ttl) {
        GenericJackson2JsonRedisSerializer serializer = new GenericJackson2JsonRedisSerializer();

        return RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(ttl)
                .disableCachingNullValues()
                .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(serializer));
    }
}
