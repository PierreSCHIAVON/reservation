package com.example.reservation.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration des noms de claims JWT.
 *
 * <p>Centralise les noms de claims utilisés dans l'application pour faciliter
 * la maintenance et permettre la configuration via application.properties.
 *
 * <p>Préfixe de configuration: {@code jwt.claims}
 *
 * <p>Exemple de configuration:
 * <pre>
 * jwt.claims.email=email
 * jwt.claims.subject=sub
 * jwt.claims.preferred-username=preferred_username
 * </pre>
 */
@Configuration
@ConfigurationProperties(prefix = "jwt.claims")
@Getter
@Setter
public class JwtClaimNames {

    /**
     * Nom du claim contenant l'adresse email de l'utilisateur.
     * Valeur par défaut: "email"
     */
    private String email = "email";

    /**
     * Nom du claim contenant le subject (identifiant unique) de l'utilisateur.
     * Valeur par défaut: "sub"
     */
    private String subject = "sub";

    /**
     * Nom du claim contenant le nom d'utilisateur préféré.
     * Valeur par défaut: "preferred_username"
     */
    private String preferredUsername = "preferred_username";
}
