#!/usr/bin/env bash
# ==============================================================================
# Script para limpiar artefactos y archivos temporales de C++, Rust y Lua
# ==============================================================================

set -e

# Ubicarse en el directorio raíz del proyecto
PROJECT_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$PROJECT_ROOT"

echo "🧹 [Keeppr] Iniciando limpieza de artefactos nativos (C++, Rust, Lua)..."

# 1. Limpieza de artefactos de compilación de Rust (Cargo target)
RUST_TARGET_DIR="app/src/main/rust/target"
if [ -d "$RUST_TARGET_DIR" ]; then
    echo "  -> Eliminando directorio de compilación de Rust: $RUST_TARGET_DIR"
    rm -rf "$RUST_TARGET_DIR"
fi

# Eliminar cualquier carpeta 'target' residual en cualquier parte del proyecto
find . -type d -name "target" -not -path "*/.git/*" -exec rm -rf {} + 2>/dev/null || true

RUST_LOCK_FILE="app/src/main/rust/Cargo.lock"
if [ -f "$RUST_LOCK_FILE" ]; then
    echo "  -> Eliminando archivo de bloqueo de Rust: $RUST_LOCK_FILE"
    rm -f "$RUST_LOCK_FILE"
fi

# Eliminar posibles respaldos de Rust
find app/src/main/rust -type f -name "*.rs.bk" -exec rm -vf {} + 2>/dev/null || true

# 2. Limpieza de artefactos de C++ y CMake
echo "  -> Limpiando directorios .cxx y .externalNativeBuild..."
rm -rf .cxx app/.cxx .externalNativeBuild app/.externalNativeBuild

echo "  -> Limpiando archivos de caché y configuración temporal de CMake..."
find . -type f \( -name "CMakeCache.txt" -o -name "cmake_install.cmake" \) -exec rm -vf {} + 2>/dev/null || true
find . -type d -name "CMakeFiles" -exec rm -rf {} + 2>/dev/null || true

echo "  -> Limpiando bibliotecas compiladas (.a, .so, .o, .obj) en el árbol de código fuente..."
find app/src -type f \( -name "*.o" -o -name "*.obj" -o -name "*.a" -o -name "*.so" -o -name "*.dylib" \) -exec rm -vf {} + 2>/dev/null || true

# 3. Limpieza de bytecode y temporales de Lua
echo "  -> Limpiando bytecode compilado de Lua (*.luac)..."
find . -type f -name "*.luac" -exec rm -vf {} + 2>/dev/null || true

# Limpiar temporales de descarga en /tmp si existen
rm -rf /tmp/lua* /tmp/rust_test 2>/dev/null || true

echo "✅ [VaultNotes] Limpieza de artefactos nativos completada exitosamente al 100%."
