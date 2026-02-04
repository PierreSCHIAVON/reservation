# 🎮 Gestion de l'application - Guide rapide

## Scripts disponibles

Trois scripts ont été créés pour faciliter la gestion de l'application :

| Script | Description | Usage |
|--------|-------------|-------|
| `start.sh` | Démarre l'application | `./start.sh` |
| `stop.sh` | Arrête l'application | `./stop.sh` |
| `restart.sh` | Redémarre l'application | `./restart.sh` ou `./restart.sh --clean` |

## 🚀 Démarrer l'application

```bash
cd reservation
./start.sh
```

**Ce que fait le script** :
- ✅ Configure Java 21 automatiquement
- ✅ Vérifie que les services Docker (PostgreSQL, Keycloak) sont démarrés
- ✅ Détecte les conflits de ports (8080, 5432)
- ✅ Propose d'arrêter les processus en conflit
- ✅ Lance Spring Boot avec le profil `local`

**Résultat attendu** :
```
✨ Lancement de Spring Boot...
📍 URL: http://localhost:8080
📖 Swagger UI: http://localhost:8080/swagger-ui/index.html

Started ReservationApplication in 5.xxx seconds
```

## 🛑 Arrêter l'application

```bash
cd reservation
./stop.sh
```

**Ce que fait le script** :
- Arrête le processus Spring Boot
- Libère le port 8080
- Force l'arrêt si nécessaire

## 🔄 Redémarrer l'application

### Redémarrage simple
```bash
cd reservation
./restart.sh
```

### Redémarrage avec nettoyage complet
```bash
cd reservation
./restart.sh --clean
```

Le flag `--clean` effectue :
- Suppression des DTOs générés
- `mvn clean install -DskipTests`
- Rebuild complet avant redémarrage

**Quand utiliser `--clean` ?**
- Après modification du fichier `openapi.yml`
- Après modification du `pom.xml`
- En cas de problème de compilation étrange
- Pour repartir sur une base propre

## ⌨️ Commandes manuelles alternatives

Si tu préfères ne pas utiliser les scripts :

### Démarrage manuel
```bash
# 1. Configurer Java
export JAVA_HOME=/opt/homebrew/opt/openjdk@21/libexec/openjdk.jdk/Contents/Home

# 2. Démarrer les services Docker (si nécessaire)
docker compose -f ../docker-compose-dev.yml up -d

# 3. Lancer Spring Boot
cd reservation
mvn spring-boot:run -Dspring-boot.run.profiles=local
```

### Arrêt manuel
```bash
# Méthode 1 : Ctrl+C dans le terminal qui exécute l'app

# Méthode 2 : Kill le processus
pkill -f "spring-boot:run"

# Méthode 3 : Par numéro de PID
ps aux | grep "spring-boot:run" | grep -v grep
kill <PID>
```

## 🔍 Vérifier l'état de l'application

### L'app tourne-t-elle ?
```bash
# Vérifier le processus
ps aux | grep "spring-boot:run" | grep -v grep

# Vérifier le port
lsof -i :8080

# Test API
curl http://localhost:8080/actuator/health
```

### Voir les logs
Si lancée avec `./start.sh`, les logs s'affichent dans le terminal.

Si lancée en arrière-plan :
```bash
# Pas recommandé, mais si besoin :
mvn spring-boot:run -Dspring-boot.run.profiles=local > app.log 2>&1 &

# Voir les logs
tail -f app.log
```

## 🐳 Gestion des services Docker

### Démarrer les services (PostgreSQL + Keycloak)
```bash
cd /Users/pierreschiavon/IdeaProjects/reservation
docker compose -f docker-compose-dev.yml up -d
```

### Vérifier les services
```bash
docker compose -f docker-compose-dev.yml ps
```

### Arrêter les services
```bash
docker compose -f docker-compose-dev.yml down
```

### Réinitialiser complètement la base de données
```bash
# ⚠️ Cela supprime toutes les données !
docker compose -f docker-compose-dev.yml down -v
docker compose -f docker-compose-dev.yml up -d
```

## 🔧 Dépannage

### Erreur : "Port 8080 already in use"
```bash
# Trouver ce qui utilise le port
lsof -i :8080

# Arrêter le processus
./stop.sh
# ou
lsof -ti:8080 | xargs kill -9
```

### Erreur : "role 'reservation' does not exist"
PostgreSQL local est probablement en conflit.
```bash
# Arrêter PostgreSQL local
killall -9 postgres

# Vérifier que seul Docker écoute sur 5432
lsof -i :5432
```

### Erreur : "release version 21 not supported"
Java 21 n'est pas configuré.
```bash
# Vérifier la version
mvn -version

# Si ce n'est pas Java 21, recharger le terminal
source ~/.zshrc
```

### Les DTOs ne se régénèrent pas
```bash
# Forcer la régénération
rm -rf target/generated-sources/openapi
mvn clean install -DskipTests
```

## 📚 Références

- **Documentation complète** : `LANCER_EN_LOCAL.md`
- **Problèmes résolus** : `PROBLEMES_RESOLUS.md`
- **Guide rapide** : `DEMARRAGE_RAPIDE.md`
- **Keycloak admin** : http://localhost:8081 (admin/admin)
- **Swagger UI** : http://localhost:8080/swagger-ui/index.html
