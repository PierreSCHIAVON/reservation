package com.example.reservation.security.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation pour vérifier que l'utilisateur est propriétaire de la propriété.
 *
 * <p>Paramètres requis sur la méthode:
 * <ul>
 *   <li>UUID id - L'ID de la propriété (paramètre de path ou body)</li>
 *   <li>OU UUID propertyId - L'ID de la propriété</li>
 *   <li>OU Request avec getPropertyId() - Objet request contenant l'ID de la propriété</li>
 * </ul>
 *
 * <p>Exemple d'utilisation:
 * <pre>
 * {@code
 * @RequiresPropertyOwner
 * public void updateProperty(@PathVariable UUID id, @RequestBody UpdateRequest request) {
 *     // Seul le propriétaire peut accéder ici
 * }
 * }
 * </pre>
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface RequiresPropertyOwner {

    /**
     * Nom du paramètre contenant l'ID de la propriété.
     * Par défaut: "id", "propertyId", ou détecté depuis le request body.
     */
    String value() default "";
}
