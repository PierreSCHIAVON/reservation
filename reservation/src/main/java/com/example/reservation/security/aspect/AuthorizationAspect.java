package com.example.reservation.security.aspect;

import com.example.reservation.security.AuthorizationService;
import com.example.reservation.security.annotation.RequiresAccessCodeCreator;
import com.example.reservation.security.annotation.RequiresPropertyOwner;
import com.example.reservation.security.annotation.RequiresReservationAccess;
import com.example.reservation.security.annotation.RequiresReservationPropertyOwner;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.UUID;

/**
 * Aspect AOP pour gérer les annotations d'autorisation personnalisées.
 *
 * <p>Intercepte les méthodes annotées avec les annotations d'autorisation
 * et délègue les vérifications au {@link AuthorizationService}.
 */
@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class AuthorizationAspect {

    private final AuthorizationService authorizationService;

    @Before("@annotation(requiresPropertyOwner)")
    public void checkPropertyOwner(JoinPoint joinPoint, RequiresPropertyOwner requiresPropertyOwner) {
        String userSub = extractUserSub();
        UUID propertyId = extractPropertyId(joinPoint, requiresPropertyOwner.value());

        log.debug("Checking property owner: propertyId={}, userSub={}", propertyId, userSub);

        if (!authorizationService.isPropertyOwner(propertyId, userSub)) {
            log.warn("Access denied: User {} is not owner of property {}", userSub, propertyId);
            throw new AccessDeniedException("Vous n'êtes pas autorisé à accéder à cette propriété");
        }
    }

    @Before("@annotation(requiresReservationPropertyOwner)")
    public void checkReservationPropertyOwner(JoinPoint joinPoint, RequiresReservationPropertyOwner requiresReservationPropertyOwner) {
        String userSub = extractUserSub();
        UUID reservationId = extractParameterValue(joinPoint, requiresReservationPropertyOwner.value(), UUID.class);

        log.debug("Checking reservation property owner: reservationId={}, userSub={}", reservationId, userSub);

        if (!authorizationService.isReservationPropertyOwner(reservationId, userSub)) {
            log.warn("Access denied: User {} is not owner of property for reservation {}", userSub, reservationId);
            throw new AccessDeniedException("Vous n'êtes pas autorisé à accéder à cette réservation");
        }
    }

    @Before("@annotation(requiresReservationAccess)")
    public void checkReservationAccess(JoinPoint joinPoint, RequiresReservationAccess requiresReservationAccess) {
        String userSub = extractUserSub();
        UUID reservationId = extractParameterValue(joinPoint, requiresReservationAccess.value(), UUID.class);

        log.debug("Checking reservation access: reservationId={}, userSub={}", reservationId, userSub);

        if (!authorizationService.canAccessReservation(reservationId, userSub)) {
            log.warn("Access denied: User {} cannot access reservation {}", userSub, reservationId);
            throw new AccessDeniedException("Vous n'êtes pas autorisé à accéder à cette réservation");
        }
    }

    @Before("@annotation(requiresAccessCodeCreator)")
    public void checkAccessCodeCreator(JoinPoint joinPoint, RequiresAccessCodeCreator requiresAccessCodeCreator) {
        String userSub = extractUserSub();
        UUID accessCodeId = extractParameterValue(joinPoint, requiresAccessCodeCreator.value(), UUID.class);

        log.debug("Checking access code creator: accessCodeId={}, userSub={}", accessCodeId, userSub);

        if (!authorizationService.isAccessCodeCreator(accessCodeId, userSub)) {
            log.warn("Access denied: User {} is not creator of access code {}", userSub, accessCodeId);
            throw new AccessDeniedException("Vous n'êtes pas autorisé à accéder à ce code d'accès");
        }
    }

    /**
     * Extrait le subject (sub) de l'utilisateur depuis le JWT.
     */
    private String extractUserSub() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof Jwt jwt)) {
            throw new AccessDeniedException("Authentification requise");
        }
        return jwt.getSubject();
    }

    /**
     * Extrait l'ID de la propriété depuis les paramètres de la méthode.
     * Gère plusieurs cas:
     * - Paramètre nommé "id"
     * - Paramètre nommé "propertyId"
     * - Méthode getPropertyId() sur un objet request
     */
    private UUID extractPropertyId(JoinPoint joinPoint, String paramName) {
        // Si un nom de paramètre est spécifié, l'utiliser
        if (paramName != null && !paramName.isEmpty()) {
            return extractParameterValue(joinPoint, paramName, UUID.class);
        }

        // Essayer "id" en premier
        UUID result = tryExtractParameter(joinPoint, "id", UUID.class);
        if (result != null) {
            return result;
        }

        // Essayer "propertyId"
        result = tryExtractParameter(joinPoint, "propertyId", UUID.class);
        if (result != null) {
            return result;
        }

        // Essayer de trouver un objet request avec getPropertyId()
        Object[] args = joinPoint.getArgs();
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Parameter[] parameters = signature.getMethod().getParameters();

        for (int i = 0; i < parameters.length; i++) {
            Object arg = args[i];
            if (arg != null) {
                try {
                    Method getter = arg.getClass().getMethod("getPropertyId");
                    Object value = getter.invoke(arg);
                    if (value instanceof UUID) {
                        return (UUID) value;
                    }
                } catch (Exception e) {
                    // Continue searching
                }
            }
        }

        throw new IllegalStateException("Impossible de trouver l'ID de la propriété dans les paramètres de la méthode");
    }

    /**
     * Extrait la valeur d'un paramètre par son nom.
     */
    private <T> T extractParameterValue(JoinPoint joinPoint, String paramName, Class<T> expectedType) {
        T result = tryExtractParameter(joinPoint, paramName, expectedType);
        if (result == null) {
            throw new IllegalStateException("Paramètre '" + paramName + "' non trouvé ou type incorrect");
        }
        return result;
    }

    /**
     * Tente d'extraire un paramètre, retourne null si non trouvé.
     */
    private <T> T tryExtractParameter(JoinPoint joinPoint, String paramName, Class<T> expectedType) {
        Object[] args = joinPoint.getArgs();
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Parameter[] parameters = signature.getMethod().getParameters();

        for (int i = 0; i < parameters.length; i++) {
            if (parameters[i].getName().equals(paramName)) {
                Object value = args[i];
                if (value != null && expectedType.isInstance(value)) {
                    return expectedType.cast(value);
                }
            }
        }
        return null;
    }
}
