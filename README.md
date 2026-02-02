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
