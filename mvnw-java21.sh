#!/bin/bash
# Script pour forcer Maven à utiliser Java 21

export JAVA_HOME=/opt/homebrew/opt/openjdk@21/libexec/openjdk.jdk/Contents/Home
export PATH="$JAVA_HOME/bin:$PATH"

echo "🔧 Utilisation de Java 21 pour Maven"
java -version
echo ""

cd reservation
mvn "$@"
