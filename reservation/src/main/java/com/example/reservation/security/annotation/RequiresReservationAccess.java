package com.example.reservation.security.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation pour vérifier que l'utilisateur peut accéder à la réservation.
 * L'utilisateur doit être soit le locataire, soit le propriétaire du bien.
 *
 * <p>Paramètres requis sur la méthode:
 * <ul>
 *   <li>UUID id - L'ID de la réservation</li>
 * </ul>
 *
 * <p>Exemple d'utilisation:
 * <pre>
 * {@code
 * @RequiresReservationAccess
 * public ReservationResponse getReservation(@PathVariable UUID id) {
 *     // Le locataire ou le propriétaire peut accéder ici
 * }
 * }
 * </pre>
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface RequiresReservationAccess {

    /**
     * Nom du paramètre contenant l'ID de la réservation.
     * Par défaut: "id".
     */
    String value() default "id";
}
