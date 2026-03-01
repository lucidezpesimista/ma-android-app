#!/bin/bash

# Script de configuración inicial para el proyecto Ma
# Este script descarga el Gradle Wrapper necesario

echo "Configurando proyecto Ma..."
echo ""

# Verificar que Java está instalado
if ! command -v java &> /dev/null; then
    echo "ERROR: Java no está instalado. Por favor instala JDK 17 o superior."
    exit 1
fi

# Verificar versión de Java
JAVA_VERSION=$(java -version 2>&1 | head -n 1 | cut -d'"' -f2)
echo "Java detectado: $JAVA_VERSION"

# Descargar Gradle Wrapper
echo ""
echo "Descargando Gradle Wrapper..."
if command -v curl &> /dev/null; then
    curl -L -o gradle/wrapper/gradle-wrapper.jar \
        https://raw.githubusercontent.com/gradle/gradle/v8.2.0/gradle/wrapper/gradle-wrapper.jar
elif command -v wget &> /dev/null; then
    wget -O gradle/wrapper/gradle-wrapper.jar \
        https://raw.githubusercontent.com/gradle/gradle/v8.2.0/gradle/wrapper/gradle-wrapper.jar
else
    echo "ERROR: Necesitas curl o wget para descargar el Gradle Wrapper."
    echo "Por favor descárgalo manualmente desde:"
    echo "https://services.gradle.org/distributions/gradle-8.2-bin.zip"
    exit 1
fi

# Hacer gradlew ejecutable
chmod +x gradlew

echo ""
echo "✅ Configuración completada!"
echo ""
echo "Para abrir el proyecto:"
echo "  1. Abre Android Studio"
echo "  2. Selecciona 'Open an existing project'"
echo "  3. Navega a esta carpeta y selecciónala"
echo ""
echo "O ejecuta desde línea de comandos:"
echo "  ./gradlew assembleDebug"
echo ""
