#!/bin/bash

# Script de redémarrage de l'application
# Usage: ./restart.sh [--clean]

set -e

CLEAN_BUILD=false
if [[ "$1" == "--clean" ]]; then
    CLEAN_BUILD=true
    echo "🧹 Mode nettoyage activé"
fi

echo "🔄 Redémarrage de l'application..."
echo ""

# Arrêter l'application
./stop.sh

echo ""

# Nettoyage optionnel
if [ "$CLEAN_BUILD" = true ]; then
    echo "🧹 Nettoyage du projet..."

    # Configuration Java 21 (si pas déjà défini)
    if [ -z "$JAVA_HOME" ]; then
        if [ -x "/opt/homebrew/opt/openjdk@21/libexec/openjdk.jdk/Contents/Home/bin/java" ]; then
            export JAVA_HOME="/opt/homebrew/opt/openjdk@21/libexec/openjdk.jdk/Contents/Home"
        elif [ -x "/usr/local/opt/openjdk@21/libexec/openjdk.jdk/Contents/Home/bin/java" ]; then
            export JAVA_HOME="/usr/local/opt/openjdk@21/libexec/openjdk.jdk/Contents/Home"
        fi
    fi

    # Nettoyer les DTOs générés
    rm -rf target/generated-sources/openapi

    # Rebuild complet
    mvn clean install -DskipTests

    echo "✅ Nettoyage et rebuild terminés"
    echo ""
fi

# Attendre un peu
sleep 2

# Redémarrer
./start.sh
