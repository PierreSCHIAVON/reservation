# Guide de Démarrage (Local)

Ce guide regroupe tout ce qu'il faut pour lancer l'application en local, obtenir un JWT, et résoudre les problèmes courants.

## Prérequis

- Java 21
- Docker + Docker Compose

## Démarrage rapide (recommandé)

### 1) Configurer Java 21 (une seule fois)

```bash
export JAVA_HOME=/opt/homebrew/opt/openjdk@21/libexec/openjdk.jdk/Contents/Home
export PATH="$JAVA_HOME/bin:$PATH"
```

Pour rendre permanent :

```bash
echo 'export JAVA_HOME=/opt/homebrew/opt/openjdk@21/libexec/openjdk.jdk/Contents/Home' >> ~/.zshrc
echo 'export PATH="$JAVA_HOME/bin:$PATH"' >> ~/.zshrc
source ~/.zshrc
```

Vérifier :

```bash
java -version
# Doit afficher : openjdk version "21.x"
```

### 2) Lancer via le script

```bash
cd /Users/pierreschiavon/IdeaProjects/reservation
./start-local.sh
```

Ce script :
- Vérifie Java 21
- Démarre PostgreSQL + Keycloak (Docker)
- Attend les services
- Génère les DTOs
- Propose de lancer Spring Boot

## Démarrage manuel (sans script)

### 1) Démarrer les services (PostgreSQL + Keycloak)

```bash
cd /Users/pierreschiavon/IdeaProjects/reservation

docker compose -f docker-compose-dev.yml up -d
sleep 30
```

### 2) Lancer Spring Boot

```bash
cd reservation
mvn clean generate-sources
mvn spring-boot:run -Dspring-boot.run.profiles=local
```

## URLs utiles

| Service      | URL                                   |
|--------------|----------------------------------------|
| API          | http://localhost:8080                  |
| Swagger UI   | http://localhost:8080/swagger-ui.html  |
| Health Check | http://localhost:8080/actuator/health  |
| Keycloak     | http://localhost:8081                  |

## Obtenir un token JWT

```bash
export TOKEN=$(curl -X POST 'http://localhost:8081/realms/reservation/protocol/openid-connect/token' \
  -H 'Content-Type: application/x-www-form-urlencoded' \
  -d 'grant_type=password&client_id=reservation-test&username=testuser&password=testuser' \
  -s | jq -r .access_token)
```

Tester l'API :

```bash
curl -H "Authorization: Bearer $TOKEN" http://localhost:8080/api/reservations/mine
```

## Développement avec Hot Reload

### IntelliJ IDEA

1. Preferences → Build, Execution, Deployment → Compiler → cocher "Build project automatically"
2. Help → Find Action → Registry → cocher "compiler.automake.allow.when.app.running"
3. Lancer en Debug

### VS Code

1. Installer "Spring Boot Extension Pack"
2. Ouvrir `ReservationApplication.java`
3. F5 (Debug)

## Problèmes courants

### Port 8080 déjà utilisé

```bash
lsof -i :8080
kill -9 <PID>
```

### PostgreSQL ne démarre pas

```bash
docker compose -f docker-compose-dev.yml restart postgres
docker compose -f docker-compose-dev.yml logs -f postgres
```

### Conflit Postgres local (role 'reservation' absent)

```bash
killall -9 postgres
lsof -nP -iTCP:5432
# Doit montrer uniquement Docker (com.docker)
```

### Java version incorrecte

```bash
export JAVA_HOME=/opt/homebrew/opt/openjdk@21/libexec/openjdk.jdk/Contents/Home
export PATH="$JAVA_HOME/bin:$PATH"
java -version
```

## Commandes utiles

### Arrêter les services Docker

```bash
docker compose -f docker-compose-dev.yml down
```

### Regénérer les DTOs

```bash
cd reservation
mvn clean generate-sources
```

### Logs services

```bash
# PostgreSQL
docker compose -f docker-compose-dev.yml logs -f postgres

# Keycloak
docker compose -f docker-compose-dev.yml logs -f keycloak
```

## Checklist avant de lancer

- [ ] Java 21 installé et `JAVA_HOME` configuré
- [ ] Services Docker démarrés
- [ ] Pas de conflit sur 5432/8080
- [ ] Profil `local` utilisé
