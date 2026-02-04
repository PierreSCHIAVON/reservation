package com.example.reservation.security.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation pour vérifier que l'utilisateur est le créateur du code d'accès.
 *
 * <p>Paramètres requis sur la méthode:
 * <ul>
 *   <li>UUID id - L'ID du code d'accès</li>
 * </ul>
 *
 * <p>Exemple d'utilisation:
 * <pre>
 * {@code
 * @RequiresAccessCodeCreator
 * public void revokeAccessCode(@PathVariable UUID id) {
 *     // Seul le créateur du code peut accéder ici
 * }
 * }
 * </pre>
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface RequiresAccessCodeCreator {

    /**
     * Nom du paramètre contenant l'ID du code d'accès.
     * Par défaut: "id".
     */
    String value() default "id";
}
