# reservation
Site de réservation

## Prérequis

- Docker et Docker Compose

## Lancement

### Lancer tous les containers

```bash
docker compose up -d
```

### Lancer et reconstruire le backend (après modification du code)

```bash
docker compose up -d --build
```

### Voir l'état des containers

```bash
docker compose ps
```

### Voir les logs

```bash
docker compose logs -f           # tous les services
docker compose logs -f keycloak  # keycloak uniquement
docker compose logs -f backend   # backend uniquement
```

### Arrêter tous les containers

```bash
docker compose down
```

### Arrêter et supprimer les volumes (reset complet)

```bash
docker compose down -v
```

## Développement local

Pour développer en local avec hot-reload tout en utilisant les services Docker (PostgreSQL, Keycloak).

### 1. Lancer les services (sans le backend)

```bash
docker compose -f docker-compose-dev.yml up -d
```

### 2. Lancer le backend en local

```bash
cd reservation
./mvnw spring-boot:run -Dspring-boot.run.profiles=local
```

Ou depuis votre IDE avec le profil `local`.

### 3. Accès

| Service | URL |
|---------|-----|
| Backend | http://localhost:8080 |
| Keycloak | http://localhost:8081 |
| Keycloak Admin | http://localhost:8081/admin (admin/admin) |

### 4. Arrêter les services

```bash
docker compose -f docker-compose-dev.yml down
```
