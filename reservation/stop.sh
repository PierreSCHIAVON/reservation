#!/bin/bash

# Script d'arrêt de l'application
# Usage: ./stop.sh

echo "🛑 Arrêt de l'application Reservation..."

# Arrêter Spring Boot
if pkill -f "spring-boot:run"; then
    echo "✅ Application Spring Boot arrêtée"
else
    echo "ℹ️  Aucune application Spring Boot en cours"
fi

# Vérifier que le port 8080 est bien libéré
sleep 2
if lsof -Pi :8080 -sTCP:LISTEN -t >/dev/null 2>&1 ; then
    echo "⚠️  Le port 8080 est toujours utilisé. Force l'arrêt..."
    lsof -ti:8080 | xargs kill -9 2>/dev/null || true
    echo "✅ Port 8080 libéré"
else
    echo "✅ Port 8080 libre"
fi

echo ""
echo "🏁 Application arrêtée avec succès"
