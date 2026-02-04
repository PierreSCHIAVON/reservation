# Authorization Documentation

## Overview

Ce projet utilise un système d'autorisation centralisé basé sur des annotations personnalisées et AOP (Aspect-Oriented Programming). Cette approche remplace les annotations `@PreAuthorize` avec des expressions SpEL complexes par des annotations plus simples et typées.

## Architecture

### Annotations Personnalisées

Quatre annotations personnalisées sont disponibles :

#### `@RequiresPropertyOwner`
Vérifie que l'utilisateur est propriétaire de la propriété.

```java
@RequiresPropertyOwner
public void updateProperty(@PathVariable UUID id, @RequestBody UpdateRequest request) {
    // Seul le propriétaire peut accéder ici
}
```

**Détection automatique des paramètres :**
- Cherche un paramètre nommé `id`
- Cherche un paramètre nommé `propertyId`
- Cherche une méthode `getPropertyId()` sur un objet request

**Utilisation avec un nom de paramètre spécifique :**
```java
@RequiresPropertyOwner("propertyId")
public void getAccessCodes(@PathVariable UUID propertyId) {
    // ...
}
```

#### `@RequiresReservationPropertyOwner`
Vérifie que l'utilisateur est propriétaire du bien lié à la réservation.

```java
@RequiresReservationPropertyOwner
public void confirmReservation(@PathVariable UUID id) {
    // Seul le propriétaire du bien peut accéder ici
}
```

#### `@RequiresReservationAccess`
Vérifie que l'utilisateur peut accéder à la réservation (locataire ou propriétaire).

```java
@RequiresReservationAccess
public ReservationResponse getReservation(@PathVariable UUID id) {
    // Le locataire ou le propriétaire peut accéder ici
}
```

#### `@RequiresAccessCodeCreator`
Vérifie que l'utilisateur est le créateur du code d'accès.

```java
@RequiresAccessCodeCreator
public void revokeAccessCode(@PathVariable UUID id) {
    // Seul le créateur du code peut accéder ici
}
```

### Aspect AOP

La classe `AuthorizationAspect` intercepte les méthodes annotées et effectue les vérifications d'autorisation :

1. **Extraction du JWT** : Récupère le subject (sub) depuis le SecurityContext
2. **Extraction des paramètres** : Trouve l'ID de la ressource depuis les paramètres de la méthode
3. **Vérification** : Appelle la méthode appropriée du `AuthorizationService`
4. **Rejection** : Lance `AccessDeniedException` si l'autorisation échoue

### Service d'Autorisation

Le `AuthorizationService` contient la logique métier d'autorisation :

```java
@Service("authz")
public class AuthorizationService {

    // Vérifie si l'utilisateur est propriétaire de la propriété
    boolean isPropertyOwner(UUID propertyId, String userSub);

    // Vérifie si l'utilisateur est locataire de la réservation
    boolean isReservationTenant(UUID reservationId, String userSub);

    // Vérifie si l'utilisateur est propriétaire du bien de la réservation
    boolean isReservationPropertyOwner(UUID reservationId, String userSub);

    // Vérifie si l'utilisateur peut accéder à la réservation
    boolean canAccessReservation(UUID reservationId, String userSub);

    // Vérifie si l'utilisateur est créateur du code d'accès
    boolean isAccessCodeCreator(UUID accessCodeId, String userSub);
}
```

## Matrice d'Autorisation

| Endpoint | Annotation | Règle |
|----------|-----------|-------|
| `PUT /api/properties/{id}` | `@RequiresPropertyOwner` | Propriétaire uniquement |
| `POST /api/properties/{id}/activate` | `@RequiresPropertyOwner` | Propriétaire uniquement |
| `POST /api/properties/{id}/deactivate` | `@RequiresPropertyOwner` | Propriétaire uniquement |
| `DELETE /api/properties/{id}` | `@RequiresPropertyOwner` | Propriétaire uniquement |
| `GET /api/reservations/{id}` | `@RequiresReservationAccess` | Locataire OU propriétaire |
| `POST /api/reservations/{id}/confirm` | `@RequiresReservationPropertyOwner` | Propriétaire uniquement |
| `POST /api/reservations/{id}/cancel` | `@RequiresReservationAccess` | Locataire OU propriétaire |
| `POST /api/reservations/{id}/complete` | `@RequiresReservationPropertyOwner` | Propriétaire uniquement |
| `POST /api/reservations/{id}/discount` | `@RequiresReservationPropertyOwner` | Propriétaire uniquement |
| `POST /api/reservations/{id}/free` | `@RequiresReservationPropertyOwner` | Propriétaire uniquement |
| `POST /api/access-codes` | `@RequiresPropertyOwner` | Propriétaire de la propriété |
| `GET /api/access-codes/property/{propertyId}` | `@RequiresPropertyOwner("propertyId")` | Propriétaire de la propriété |
| `POST /api/access-codes/{id}/revoke` | `@RequiresAccessCodeCreator` | Créateur du code |

## Avantages

1. **Lisibilité** : Les annotations sont claires et auto-documentées
2. **Type-safety** : Erreurs de compilation au lieu d'erreurs runtime
3. **Maintenabilité** : Logique centralisée dans l'aspect et le service
4. **Testabilité** : Facile à mocker et tester unitairement
5. **Évolutivité** : Ajout de nouvelles règles via de nouvelles annotations

## Logging

L'aspect log les vérifications d'autorisation :

```
DEBUG - Checking property owner: propertyId=..., userSub=...
WARN  - Access denied: User ... is not owner of property ...
```

## Gestion des Erreurs

En cas d'échec d'autorisation, une `AccessDeniedException` est lancée avec un message localisé en français :

- "Vous n'êtes pas autorisé à accéder à cette propriété"
- "Vous n'êtes pas autorisé à accéder à cette réservation"
- "Vous n'êtes pas autorisé à accéder à ce code d'accès"

Le `GlobalExceptionHandler` intercepte cette exception et retourne une réponse HTTP 403 Forbidden.

## Migration depuis @PreAuthorize

### Avant

```java
@PreAuthorize("@authz.isPropertyOwner(#id, authentication.principal.subject)")
public void updateProperty(@PathVariable UUID id, @RequestBody UpdateRequest request) {
    // ...
}
```

### Après

```java
@RequiresPropertyOwner
public void updateProperty(@PathVariable UUID id, @RequestBody UpdateRequest request) {
    // ...
}
```

## Ajout de Nouvelles Règles

Pour ajouter une nouvelle règle d'autorisation :

1. **Créer l'annotation** dans `security.annotation`
2. **Ajouter la méthode métier** dans `AuthorizationService`
3. **Ajouter le pointcut** dans `AuthorizationAspect`
4. **Documenter** dans cette page

### Exemple : Nouvelle annotation @RequiresTenantAccess

```java
// 1. Annotation
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface RequiresTenantAccess {
    String value() default "id";
}

// 2. Service
public boolean isTenant(UUID reservationId, String userSub) {
    return reservationRepository.existsByIdAndTenantSub(reservationId, userSub);
}

// 3. Aspect
@Before("@annotation(requiresTenantAccess)")
public void checkTenantAccess(JoinPoint joinPoint, RequiresTenantAccess requiresTenantAccess) {
    String userSub = extractUserSub();
    UUID reservationId = extractParameterValue(joinPoint, requiresTenantAccess.value(), UUID.class);

    if (!authorizationService.isTenant(reservationId, userSub)) {
        throw new AccessDeniedException("Vous n'êtes pas le locataire de cette réservation");
    }
}
```
