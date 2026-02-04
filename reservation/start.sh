#!/bin/bash

# Script de démarrage de l'application en local
# Usage: ./start.sh

set -e

echo "🚀 Démarrage de l'application Reservation..."
echo ""

# Configuration Java 21
# Essayer de détecter JAVA_HOME automatiquement
if [ -z "$JAVA_HOME" ]; then
    echo "⚠️  JAVA_HOME n'est pas défini."

    # Essayer de trouver Java 21 avec différentes méthodes
    if [ -x "/opt/homebrew/opt/openjdk@21/libexec/openjdk.jdk/Contents/Home/bin/java" ]; then
        export JAVA_HOME="/opt/homebrew/opt/openjdk@21/libexec/openjdk.jdk/Contents/Home"
        echo "✅ Java 21 trouvé via Homebrew (Apple Silicon)"
    elif [ -x "/usr/local/opt/openjdk@21/libexec/openjdk.jdk/Contents/Home/bin/java" ]; then
        export JAVA_HOME="/usr/local/opt/openjdk@21/libexec/openjdk.jdk/Contents/Home"
        echo "✅ Java 21 trouvé via Homebrew (Intel)"
    elif command -v java >/dev/null 2>&1; then
        JAVA_VERSION=$(java -version 2>&1 | head -1 | cut -d'"' -f2 | cut -d'.' -f1)
        if [ "$JAVA_VERSION" = "21" ]; then
            echo "✅ Java 21 trouvé dans PATH"
        else
            echo "❌ Java $JAVA_VERSION trouvé, mais Java 21 est requis"
            echo "   Installez Java 21 ou définissez JAVA_HOME dans ~/.zshrc"
            exit 1
        fi
    else
        echo "❌ Java non trouvé"
        echo "   Installez Java 21 ou définissez JAVA_HOME dans ~/.zshrc"
        echo ""
        echo "   Exemple pour ~/.zshrc :"
        echo "   export JAVA_HOME=/chemin/vers/jdk-21"
        echo "   export PATH=\"\$JAVA_HOME/bin:\$PATH\""
        exit 1
    fi
else
    echo "✅ JAVA_HOME déjà configuré: $JAVA_HOME"
fi

# Vérifier la version de Java
JAVA_VERSION=$("$JAVA_HOME/bin/java" -version 2>&1 | head -1 | cut -d'"' -f2 | cut -d'.' -f1)
if [ "$JAVA_VERSION" != "21" ]; then
    echo "❌ Java $JAVA_VERSION détecté, mais Java 21 est requis"
    exit 1
fi

# Vérifier que les services Docker sont lancés
echo ""
echo "📦 Vérification des services Docker..."
if ! docker compose -f ../docker-compose-dev.yml ps | grep -q "Up"; then
    echo "⚠️  Services Docker non démarrés. Démarrage..."
    docker compose -f ../docker-compose-dev.yml up -d
    echo "⏳ Attente de la santé des services (30s)..."
    sleep 30
else
    echo "✅ Services Docker OK"
fi

# Vérifier que le port 8080 est libre
if lsof -Pi :8080 -sTCP:LISTEN -t >/dev/null 2>&1 ; then
    echo ""
    echo "⚠️  Le port 8080 est déjà utilisé."
    echo "Voulez-vous arrêter le processus existant ? (y/n)"
    read -r response
    if [[ "$response" =~ ^([yY][eE][sS]|[yY])$ ]]; then
        echo "🛑 Arrêt du processus sur le port 8080..."
        lsof -ti:8080 | xargs kill -9 2>/dev/null || true
        sleep 2
    else
        echo "❌ Abandon du démarrage."
        exit 1
    fi
fi

# Vérifier que PostgreSQL local n'est pas en conflit
if lsof -Pi :5432 -sTCP:LISTEN 2>/dev/null | grep -v docker | grep -v grep >/dev/null 2>&1; then
    echo ""
    echo "⚠️  PostgreSQL local détecté sur le port 5432 (conflit potentiel)."
    echo "Voulez-vous l'arrêter ? (y/n)"
    read -r response
    if [[ "$response" =~ ^([yY][eE][sS]|[yY])$ ]]; then
        echo "🛑 Arrêt de PostgreSQL local..."
        killall -9 postgres 2>/dev/null || true
        sleep 2
    fi
fi

echo ""
echo "✨ Lancement de Spring Boot..."
echo "📍 URL: http://localhost:8080"
echo "📖 Swagger UI: http://localhost:8080/swagger-ui/index.html"
echo ""
echo "Pour arrêter: Ctrl+C ou ./stop.sh"
echo ""

mvn spring-boot:run -Dspring-boot.run.profiles=local
