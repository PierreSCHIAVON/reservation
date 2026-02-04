package com.example.reservation.security;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Filtre de limitation de taux pour prévenir les abus et attaques DoS.
 *
 * <p>Limitation: 100 requêtes par minute par adresse IP.
 *
 * <p>Note: Solution in-memory adaptée pour instance unique.
 * Pour déploiement multi-instances, utiliser Redis avec Bucket4j.
 */
@Slf4j
@Component
public class RateLimitFilter implements Filter {

    // 100 requêtes par minute par IP
    private static final int MAX_REQUESTS_PER_MINUTE = 100;
    private static final long TIME_WINDOW_MS = 60_000; // 1 minute en millisecondes

    // Map: IP -> Liste des timestamps des requêtes
    private final Map<String, List<Long>> requestTimestamps = new ConcurrentHashMap<>();

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;

        // Exclure les endpoints de santé du rate limiting
        String requestURI = httpRequest.getRequestURI();
        if (requestURI.startsWith("/actuator/") || requestURI.equals("/v3/api-docs")) {
            chain.doFilter(request, response);
            return;
        }

        String clientIp = getClientIP(httpRequest);
        long now = System.currentTimeMillis();

        // Obtenir ou créer la liste des timestamps pour cette IP
        List<Long> timestamps = requestTimestamps.computeIfAbsent(
            clientIp,
            k -> new CopyOnWriteArrayList<>()
        );

        // Nettoyer les timestamps hors de la fenêtre de temps
        timestamps.removeIf(timestamp -> now - timestamp > TIME_WINDOW_MS);

        // Vérifier la limite
        if (timestamps.size() >= MAX_REQUESTS_PER_MINUTE) {
            log.warn("Rate limit exceeded for IP: {} ({} requests in last minute)",
                clientIp, timestamps.size());

            httpResponse.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            httpResponse.setContentType("application/json");
            httpResponse.setHeader("X-RateLimit-Limit", String.valueOf(MAX_REQUESTS_PER_MINUTE));
            httpResponse.setHeader("X-RateLimit-Remaining", "0");
            httpResponse.setHeader("X-RateLimit-Reset", String.valueOf((timestamps.get(0) + TIME_WINDOW_MS) / 1000));
            httpResponse.setHeader("Retry-After", "60");

            httpResponse.getWriter().write(
                "{\"error\":\"Too Many Requests\"," +
                "\"message\":\"Vous avez dépassé la limite de " + MAX_REQUESTS_PER_MINUTE + " requêtes par minute. Veuillez réessayer plus tard.\"," +
                "\"status\":429}"
            );
            return;
        }

        // Ajouter le timestamp actuel
        timestamps.add(now);

        // Ajouter les headers de rate limit dans la réponse
        httpResponse.setHeader("X-RateLimit-Limit", String.valueOf(MAX_REQUESTS_PER_MINUTE));
        httpResponse.setHeader("X-RateLimit-Remaining", String.valueOf(MAX_REQUESTS_PER_MINUTE - timestamps.size()));

        // Continuer la chaîne de filtres
        chain.doFilter(request, response);
    }

    /**
     * Extrait l'adresse IP réelle du client en tenant compte des proxies.
     */
    private String getClientIP(HttpServletRequest request) {
        // Vérifier les headers de proxy courants
        String ip = request.getHeader("X-Forwarded-For");
        if (ip != null && !ip.isEmpty() && !"unknown".equalsIgnoreCase(ip)) {
            // X-Forwarded-For peut contenir plusieurs IPs, prendre la première
            return ip.split(",")[0].trim();
        }

        ip = request.getHeader("X-Real-IP");
        if (ip != null && !ip.isEmpty() && !"unknown".equalsIgnoreCase(ip)) {
            return ip;
        }

        return request.getRemoteAddr();
    }

    /**
     * Nettoyage périodique des IPs inactives pour éviter les fuites mémoire.
     * Exécuté toutes les 5 minutes.
     */
    @Scheduled(fixedRate = 300_000) // 5 minutes
    public void cleanupInactiveEntries() {
        long now = System.currentTimeMillis();
        int beforeSize = requestTimestamps.size();

        requestTimestamps.entrySet().removeIf(entry -> {
            List<Long> timestamps = entry.getValue();
            timestamps.removeIf(timestamp -> now - timestamp > TIME_WINDOW_MS);
            return timestamps.isEmpty();
        });

        int afterSize = requestTimestamps.size();
        if (beforeSize > afterSize) {
            log.debug("Rate limit cleanup: removed {} inactive IP entries", beforeSize - afterSize);
        }
    }
}
