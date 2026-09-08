#!/usr/bin/env bash
# ==============================================================================
# setup_debug_keystore.sh
# Genera desde cero un debug.keystore 100% funcional para compilación Debug.
# Diseñado para entornos CI (GitHub Actions) y compilación local en Android.
# ==============================================================================

set -euo pipefail

# Determinar el directorio raíz del proyecto
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
KEYSTORE_PATH="${SCRIPT_DIR}/debug.keystore"
ANDROID_HOME_DIR="${HOME}/.android"
SYSTEM_KEYSTORE_PATH="${ANDROID_HOME_DIR}/debug.keystore"

echo "========================================================"
echo " [Keystore Setup] Iniciando generación limpia de Keystore"
echo "========================================================"

# 1. Forzar eliminación de cualquier archivo o enlace previo para no reutilizar residuos
if [ -f "$KEYSTORE_PATH" ] || [ -L "$KEYSTORE_PATH" ]; then
    echo "-> Eliminando keystore previo en ${KEYSTORE_PATH} para obligar generación desde 0..."
    rm -f "$KEYSTORE_PATH"
fi

# 2. Verificar disponibilidad de keytool (JDK)
if ! command -v keytool &> /dev/null; then
    echo "ERROR: 'keytool' no está disponible en el PATH. Asegúrate de tener JDK instalado." >&2
    exit 1
fi

# 3. Generar el keystore debug oficial con los parámetros estándar de Android
echo "-> Generando ${KEYSTORE_PATH} desde cero..."
keytool -genkeypair \
    -v \
    -keystore "$KEYSTORE_PATH" \
    -alias "androiddebugkey" \
    -keyalg RSA \
    -keysize 2048 \
    -validity 10000 \
    -storepass "android" \
    -keypass "android" \
    -dname "CN=Android Debug,O=Android,C=US" \
    -storetype PKCS12

chmod 644 "$KEYSTORE_PATH"

# 4. Asegurar compatibilidad adicional en ~/.android/debug.keystore si es necesario
mkdir -p "$ANDROID_HOME_DIR"
cp -f "$KEYSTORE_PATH" "$SYSTEM_KEYSTORE_PATH"
chmod 644 "$SYSTEM_KEYSTORE_PATH"

# 5. Validación de integridad
echo "-> Verificando integridad del keystore generado..."
keytool -list \
    -keystore "$KEYSTORE_PATH" \
    -alias "androiddebugkey" \
    -storepass "android" > /dev/null

echo "========================================================"
echo " [OK] debug.keystore generado exitosamente (100% funcional)"
echo "      Ubicación principal: ${KEYSTORE_PATH}"
echo "      Ubicación fallback:  ${SYSTEM_KEYSTORE_PATH}"
echo "      Alias:               androiddebugkey"
echo "========================================================"
