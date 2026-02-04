# Scripts de gestion de l'application locale

Ce dossier contient des scripts shell pour faciliter le développement local de l'application.

## 📋 Prérequis

- **Java 21** installé (via Homebrew, SDKMAN, ou autre)
- **Docker** et **Docker Compose** installés
- **Maven** 3.9+ installé

## 🚀 Scripts disponibles

### `./start.sh`
Démarre l'application en mode développement local.

**Ce que fait le script** :
- ✅ Détecte automatiquement Java 21 (Homebrew, PATH, ou JAVA_HOME)
- ✅ Vérifie et démarre les services Docker (PostgreSQL, Keycloak)
- ✅ Détecte les conflits de ports (8080, 5432)
- ✅ Lance Spring Boot avec le profil `local`

**Usage** :
```bash
cd reservation
./start.sh
```

### `./stop.sh`
Arrête l'application en cours d'exécution.

**Usage** :
```bash
cd reservation
./stop.sh
```

### `./restart.sh`
Redémarre l'application.

**Usage** :
```bash
cd reservation

# Redémarrage simple
./restart.sh

# Redémarrage avec rebuild complet
./restart.sh --clean
```

Le flag `--clean` effectue :
- Suppression des DTOs générés
- `mvn clean install -DskipTests`
- Rebuild complet avant redémarrage

## 🔧 Configuration Java

Les scripts détectent automatiquement Java 21 dans cet ordre :

1. **Variable d'environnement `JAVA_HOME`** (si déjà définie)
2. **Homebrew Apple Silicon** : `/opt/homebrew/opt/openjdk@21/...`
3. **Homebrew Intel** : `/usr/local/opt/openjdk@21/...`
4. **Java dans le PATH** (vérifie que c'est version 21)

### Si Java 21 n'est pas détecté

**Option 1 : Définir JAVA_HOME dans votre shell**

Ajoutez à `~/.zshrc` (ou `~/.bashrc` pour Bash) :
```bash
export JAVA_HOME=/chemin/vers/votre/jdk-21
export PATH="$JAVA_HOME/bin:$PATH"
```

Puis rechargez :
```bash
source ~/.zshrc
```

**Option 2 : Installer Java 21 via Homebrew (macOS)**
```bash
brew install openjdk@21
```

**Option 3 : Utiliser SDKMAN (Linux/macOS)**
```bash
sdk install java 21.0.1-open
sdk use java 21.0.1-open
```

## 🐳 Services Docker

Les scripts utilisent `docker-compose-dev.yml` qui lance :
- **PostgreSQL** (port 5432) - Base de données de l'application
- **Keycloak** (port 8081) - Serveur d'authentification
- **PostgreSQL Keycloak** - Base de données de Keycloak

Pour gérer les services manuellement :
```bash
# Démarrer
docker compose -f docker-compose-dev.yml up -d

# Arrêter
docker compose -f docker-compose-dev.yml down

# Voir les logs
docker compose -f docker-compose-dev.yml logs -f

# Réinitialiser (⚠️ supprime les données)
docker compose -f docker-compose-dev.yml down -v
```

## 🐛 Dépannage

### Erreur : "Port 8080 already in use"
Le script le détectera et proposera d'arrêter le processus. Sinon :
```bash
./stop.sh
# ou
lsof -ti:8080 | xargs kill -9
```

### Erreur : "role 'reservation' does not exist"
Un PostgreSQL local interfère avec Docker. Le script le détectera. Sinon :
```bash
# Arrêter PostgreSQL local
killall -9 postgres

# Vérifier que seul Docker écoute sur 5432
lsof -i :5432
```

### Erreur : "release version 21 not supported"
Java 21 n'est pas configuré correctement :
```bash
# Vérifier la version
java -version
mvn -version

# Si différent de 21, configurer JAVA_HOME (voir section ci-dessus)
```

### Les scripts ne sont pas exécutables
```bash
chmod +x *.sh
```

## 📚 Documentation connexe

- **GESTION_APP.md** - Guide complet de gestion de l'application
- **LANCER_EN_LOCAL.md** - Guide détaillé pour lancer l'app en local
- **PROBLEMES_RESOLUS.md** - Problèmes courants et solutions

## 🔒 Compatibilité

**Systèmes d'exploitation** :
- ✅ macOS (Apple Silicon et Intel)
- ✅ Linux (avec adaptations mineures pour les chemins)
- ❌ Windows (utiliser WSL2 ou Git Bash)

**Shells supportés** :
- ✅ Bash
- ✅ Zsh
- ✅ Autres shells compatibles POSIX

## 💡 Personnalisation

Ces scripts sont conçus pour fonctionner "out of the box" pour la plupart des configurations. Si vous avez une installation Java personnalisée, vous pouvez :

1. Définir `JAVA_HOME` avant d'exécuter les scripts
2. Modifier les scripts localement (ils ne sont pas critiques)
3. Exécuter les commandes Maven manuellement

## 🤝 Contribution

Si vous trouvez un bug ou avez une amélioration à proposer pour ces scripts, n'hésitez pas à ouvrir une issue ou une PR !
