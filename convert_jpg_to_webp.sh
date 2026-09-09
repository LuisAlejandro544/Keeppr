#!/usr/bin/env bash
# ==============================================================================
# convert_jpg_to_webp.sh
# Script de conversión de imágenes JPG a WebP con máxima compresión y calidad.
# Compatible con entornos móviles, servidores CI/CD y estaciones de desarrollo.
# Soporta: cwebp, ffmpeg (libwebp) o ImageMagick (convert).
# ==============================================================================

set -euo pipefail

DEFAULT_INPUT="app/src/main/res/drawable/ic_keeppr_logo.jpg"
DEFAULT_OUTPUT="app/src/main/res/drawable/ic_keeppr_logo.webp"

INPUT_FILE="${1:-$DEFAULT_INPUT}"
OUTPUT_FILE="${2:-}"
MODE="${3:-max_quality}" # "max_quality" (q:100 sin pérdida visible) o "lossless"

# Si no se especificó archivo de salida, deducir con extensión .webp
if [ -z "$OUTPUT_FILE" ]; then
    if [ "$INPUT_FILE" = "$DEFAULT_INPUT" ]; then
        OUTPUT_FILE="$DEFAULT_OUTPUT"
    else
        OUTPUT_FILE="${INPUT_FILE%.*}.webp"
    fi
fi

echo "=================================================="
echo "🔄 Conversor JPG a WebP (Keeppr Native Tools)"
echo "=================================================="
echo "📁 Archivo de entrada: $INPUT_FILE"
echo "🎯 Archivo de salida:  $OUTPUT_FILE"
echo "⚙️  Modo seleccionado:   $MODE"

if [ ! -f "$INPUT_FILE" ]; then
    echo "❌ Error: El archivo de entrada '$INPUT_FILE' no existe."
    exit 1
fi

# Crear directorio de salida si no existe
mkdir -p "$(dirname "$OUTPUT_FILE")"

# Detectar herramienta disponible en el sistema
TOOL=""
if command -v cwebp >/dev/null 2>&1; then
    TOOL="cwebp"
elif command -v ffmpeg >/dev/null 2>&1; then
    TOOL="ffmpeg"
elif command -v convert >/dev/null 2>&1; then
    TOOL="convert"
else
    echo "❌ Error: No se encontró ninguna herramienta compatible (cwebp, ffmpeg o ImageMagick convert)."
    echo "💡 Instala una de ellas en tu entorno (ej: apt install webp o apt install ffmpeg)."
    exit 1
fi

echo "🛠️  Herramienta detectada: $TOOL"
echo "⏳ Procesando compresión al máximo nivel..."

if [ "$TOOL" = "cwebp" ]; then
    if [ "$MODE" = "lossless" ]; then
        cwebp -lossless -z 9 "$INPUT_FILE" -o "$OUTPUT_FILE"
    else
        cwebp -q 100 -m 6 "$INPUT_FILE" -o "$OUTPUT_FILE"
    fi
elif [ "$TOOL" = "ffmpeg" ]; then
    if [ "$MODE" = "lossless" ]; then
        ffmpeg -y -i "$INPUT_FILE" -c:v libwebp -lossless 1 -compression_level 6 "$OUTPUT_FILE" >/dev/null 2>&1
    else
        ffmpeg -y -i "$INPUT_FILE" -c:v libwebp -quality 100 -compression_level 6 "$OUTPUT_FILE" >/dev/null 2>&1
    fi
elif [ "$TOOL" = "convert" ]; then
    if [ "$MODE" = "lossless" ]; then
        convert "$INPUT_FILE" -quality 100 -define webp:lossless=true -define webp:method=6 "$OUTPUT_FILE"
    else
        convert "$INPUT_FILE" -quality 100 -define webp:method=6 "$OUTPUT_FILE"
    fi
fi

if [ ! -f "$OUTPUT_FILE" ]; then
    echo "❌ Error: La conversión falló y no se generó el archivo '$OUTPUT_FILE'."
    exit 1
fi

# Cálculo de estadísticas de tamaño
ORIG_SIZE=$(stat -c%s "$INPUT_FILE" 2>/dev/null || wc -c < "$INPUT_FILE")
NEW_SIZE=$(stat -c%s "$OUTPUT_FILE" 2>/dev/null || wc -c < "$OUTPUT_FILE")

ORIG_KB=$(awk "BEGIN {printf \"%.2f\", $ORIG_SIZE / 1024}")
NEW_KB=$(awk "BEGIN {printf \"%.2f\", $NEW_SIZE / 1024}")

if [ "$ORIG_SIZE" -gt 0 ]; then
    DIFF_PCT=$(awk "BEGIN {printf \"%.1f\", (1 - ($NEW_SIZE / $ORIG_SIZE)) * 100}")
else
    DIFF_PCT="0"
fi

echo ""
echo "✅ ¡Conversión completada con éxito!"
echo "--------------------------------------------------"
echo "📦 Tamaño original (JPG): ${ORIG_KB} KB"
echo "✨ Nuevo tamaño (WebP):   ${NEW_KB} KB"
echo "📉 Reducción de peso:     ${DIFF_PCT}%"
echo "🎉 Archivo WebP listo en: $OUTPUT_FILE"
echo "=================================================="
