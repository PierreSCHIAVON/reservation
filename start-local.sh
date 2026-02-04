#!/bin/bash

# Script de démarrage de l'application en local
# PostgreSQL et Keycloak tournent dans Docker
# Spring Boot tourne en local

set -e

echo "🚀 Démarrage de l'application Reservation en mode local"
echo ""

# Couleurs pour les messages
GREEN='\033[0;32m'
BLUE='\033[0;34m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Vérifier Java 21
echo -e "${BLUE}📌 Vérification de Java...${NC}"
JAVA_VERSION=$(java -version 2>&1 | head -1 | cut -d'"' -f2 | cut -d'.' -f1)

if [ "$JAVA_VERSION" != "21" ]; then
    echo -e "${RED}❌ Java 21 requis, version actuelle : $JAVA_VERSION${NC}"
    echo ""
    echo "Pour configurer Java 21 :"
    echo ""
    echo "  export JAVA_HOME=/opt/homebrew/opt/openjdk@21/libexec/openjdk.jdk/Contents/Home"
    echo "  export PATH=\"\$JAVA_HOME/bin:\$PATH\""
    echo ""
    echo "Ou ajoutez ces lignes à votre ~/.zshrc"
    exit 1
fi

echo -e "${GREEN}✅ Java 21 détecté${NC}"
echo ""

# Vérifier si Docker est en cours d'exécution
echo -e "${BLUE}📌 Vérification de Docker...${NC}"
if ! docker info > /dev/null 2>&1; then
    echo -e "${RED}❌ Docker n'est pas en cours d'exécution${NC}"
    echo "Veuillez démarrer Docker Desktop"
    exit 1
fi
echo -e "${GREEN}✅ Docker en cours d'exécution${NC}"
echo ""

# Démarrer les services Docker (PostgreSQL + Keycloak)
echo -e "${BLUE}📌 Démarrage des services (PostgreSQL + Keycloak)...${NC}"
docker compose -f docker-compose-dev.yml up -d

# Attendre que PostgreSQL soit prêt
echo -e "${BLUE}📌 Attente de PostgreSQL...${NC}"
for i in {1..30}; do
    if docker compose -f docker-compose-dev.yml exec -T postgres pg_isready -U reservation > /dev/null 2>&1; then
        echo -e "${GREEN}✅ PostgreSQL est prêt${NC}"
        break
    fi
    if [ $i -eq 30 ]; then
        echo -e "${RED}❌ Timeout en attendant PostgreSQL${NC}"
        exit 1
    fi
    sleep 1
done

# Attendre que Keycloak soit prêt
echo -e "${BLUE}📌 Attente de Keycloak...${NC}"
for i in {1..60}; do
    if curl -s http://localhost:8081/health/ready > /dev/null 2>&1; then
        echo -e "${GREEN}✅ Keycloak est prêt${NC}"
        break
    fi
    if [ $i -eq 60 ]; then
        echo -e "${RED}❌ Timeout en attendant Keycloak${NC}"
        echo "Keycloak peut prendre du temps à démarrer la première fois..."
        echo "Vous pouvez continuer, mais l'authentification pourrait ne pas fonctionner immédiatement"
    fi
    sleep 2
done

echo ""

# Générer les DTOs
echo -e "${BLUE}📌 Génération des DTOs depuis openapi.yml...${NC}"
cd reservation
mvn clean generate-sources -q

if [ $? -eq 0 ]; then
    echo -e "${GREEN}✅ DTOs générés avec succès${NC}"
else
    echo -e "${YELLOW}⚠️  Erreur lors de la génération des DTOs${NC}"
fi

echo ""
echo -e "${GREEN}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
echo -e "${GREEN}✅ Services démarrés !${NC}"
echo -e "${GREEN}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
echo ""
echo -e "${BLUE}Services disponibles :${NC}"
echo "  • PostgreSQL : localhost:5432"
echo "  • Keycloak   : http://localhost:8081"
echo ""
echo -e "${BLUE}Pour démarrer Spring Boot :${NC}"
echo "  mvn spring-boot:run -Dspring-boot.run.profiles=local"
echo ""
echo -e "${BLUE}Ou dans votre IDE :${NC}"
echo "  Main class: com.example.reservation.ReservationApplication"
echo "  VM options: -Dspring.profiles.active=local"
echo ""
echo -e "${BLUE}Une fois Spring Boot démarré :${NC}"
echo "  • API         : http://localhost:8080"
echo "  • Swagger UI  : http://localhost:8080/swagger-ui.html"
echo "  • Health      : http://localhost:8080/actuator/health"
echo ""
echo -e "${YELLOW}Pour arrêter les services :${NC}"
echo "  docker compose -f docker-compose-dev.yml down"
echo ""

# Demander si on veut lancer Spring Boot automatiquement
read -p "Voulez-vous démarrer Spring Boot maintenant ? (y/N) " -n 1 -r
echo ""
if [[ $REPLY =~ ^[Yy]$ ]]; then
    echo ""
    echo -e "${BLUE}📌 Démarrage de Spring Boot...${NC}"
    mvn spring-boot:run -Dspring-boot.run.profiles=local
fi
