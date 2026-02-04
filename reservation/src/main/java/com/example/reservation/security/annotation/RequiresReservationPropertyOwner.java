package com.example.reservation.security.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation pour vérifier que l'utilisateur est propriétaire du bien lié à la réservation.
 *
 * <p>Paramètres requis sur la méthode:
 * <ul>
 *   <li>UUID id - L'ID de la réservation</li>
 * </ul>
 *
 * <p>Exemple d'utilisation:
 * <pre>
 * {@code
 * @RequiresReservationPropertyOwner
 * public void confirmReservation(@PathVariable UUID id) {
 *     // Seul le propriétaire du bien peut accéder ici
 * }
 * }
 * </pre>
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface RequiresReservationPropertyOwner {

    /**
     * Nom du paramètre contenant l'ID de la réservation.
     * Par défaut: "id".
     */
    String value() default "id";
}
