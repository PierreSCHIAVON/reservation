package com.example.reservation.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;

/**
 * Configuration du système de cache de l'application.
 *
 * <p>Utilise Caffeine comme implémentation de cache in-memory pour des performances optimales.
 *
 * <p>Caches configurés:
 * <ul>
 *   <li><b>properties</b>: Cache des propriétés (TTL: 5 min, max 1000 entrées)</li>
 *   <li><b>reservations</b>: Cache des réservations (TTL: 2 min, max 500 entrées)</li>
 *   <li><b>accessCodes</b>: Cache des codes d'accès (TTL: 1 min, max 200 entrées)</li>
 *   <li><b>activeProperties</b>: Cache de la liste des propriétés actives (TTL: 1 min)</li>
 * </ul>
 *
 * <p>Note: Solution in-memory adaptée pour instance unique.
 * Pour déploiement multi-instances, migrer vers Redis avec Spring Data Redis.
 */
@Configuration
@EnableCaching
public class CacheConfig {

    /**
     * Cache principal pour les propriétés individuelles.
     * TTL de 5 minutes car les propriétés changent rarement.
     */
    @Bean
    public Caffeine<Object, Object> caffeineConfig() {
        return Caffeine.newBuilder()
                .expireAfterWrite(5, TimeUnit.MINUTES)
                .maximumSize(1000)
                .recordStats(); // Pour monitoring via actuator
    }

    @Bean
    public CacheManager cacheManager(Caffeine<Object, Object> caffeine) {
        CaffeineCacheManager cacheManager = new CaffeineCacheManager(
                "properties",
                "reservations",
                "accessCodes",
                "activeProperties"
        );
        cacheManager.setCaffeine(caffeine);
        return cacheManager;
    }

    /**
     * Configuration spécifique pour le cache des réservations.
     * TTL plus court (2 min) car les réservations changent plus fréquemment.
     */
    @Bean
    public Caffeine<Object, Object> reservationCaffeineConfig() {
        return Caffeine.newBuilder()
                .expireAfterWrite(2, TimeUnit.MINUTES)
                .maximumSize(500)
                .recordStats();
    }

    /**
     * Configuration spécifique pour le cache des codes d'accès.
     * TTL très court (1 min) car les codes peuvent être révoqués ou utilisés.
     */
    @Bean
    public Caffeine<Object, Object> accessCodeCaffeineConfig() {
        return Caffeine.newBuilder()
                .expireAfterWrite(1, TimeUnit.MINUTES)
                .maximumSize(200)
                .recordStats();
    }

    /**
     * Configuration pour le cache de la liste des propriétés actives.
     * TTL court (1 min) car c'est une liste qui peut changer souvent.
     */
    @Bean
    public Caffeine<Object, Object> activePropertiesCaffeineConfig() {
        return Caffeine.newBuilder()
                .expireAfterWrite(1, TimeUnit.MINUTES)
                .maximumSize(10) // Peu d'entrées (une par page)
                .recordStats();
    }
}
