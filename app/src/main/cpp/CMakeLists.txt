cmake_minimum_required(VERSION 3.22.1)

project("vaultnotes_native" C CXX)

set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED ON)
set(CMAKE_C_STANDARD 11)

# Lua C Source Files (Original official Lua 5.4.6 C implementation)
set(LUA_SOURCES
    lua/lapi.c
    lua/lauxlib.c
    lua/lbaselib.c
    lua/lcode.c
    lua/lcorolib.c
    lua/lctype.c
    lua/ldblib.c
    lua/ldebug.c
    lua/ldo.c
    lua/ldump.c
    lua/lfunc.c
    lua/lgc.c
    lua/linit.c
    lua/liolib.c
    lua/llex.c
    lua/lmathlib.c
    lua/lmem.c
    lua/loadlib.c
    lua/lobject.c
    lua/lopcodes.c
    lua/loslib.c
    lua/lparser.c
    lua/lstate.c
    lua/lstring.c
    lua/lstrlib.c
    lua/ltable.c
    lua/ltablib.c
    lua/ltm.c
    lua/lundump.c
    lua/lutf8lib.c
    lua/lvm.c
    lua/lzio.c
)

# Compile official Lua as a static library
add_library(lua_static STATIC ${LUA_SOURCES})
target_include_directories(lua_static PUBLIC "${CMAKE_CURRENT_SOURCE_DIR}/lua")

# Map Android ABI to Rust Target
if(CMAKE_ANDROID_ARCH_ABI STREQUAL "arm64-v8a")
    set(RUST_TARGET "aarch64-linux-android")
elseif(CMAKE_ANDROID_ARCH_ABI STREQUAL "x86_64")
    set(RUST_TARGET "x86_64-linux-android")
elseif(CMAKE_ANDROID_ARCH_ABI STREQUAL "armeabi-v7a")
    set(RUST_TARGET "armv7-linux-androideabi")
elseif(CMAKE_ANDROID_ARCH_ABI STREQUAL "x86")
    set(RUST_TARGET "i686-linux-android")
else()
    set(RUST_TARGET "aarch64-linux-android")
endif()

# Locate the Rust static library compiled by Cargo
set(RUST_LIB_PATH "${CMAKE_CURRENT_SOURCE_DIR}/../rust/target/${RUST_TARGET}/release/libvaultnotes_rust.a")

add_library(vaultnotes_rust STATIC IMPORTED)
set_target_properties(vaultnotes_rust PROPERTIES
    IMPORTED_LOCATION "${RUST_LIB_PATH}"
)

# C++ Native Bridge Library
add_library(vaultnotes_native SHARED
    native-bridge.cpp
)

target_include_directories(vaultnotes_native PRIVATE
    "${CMAKE_CURRENT_SOURCE_DIR}/lua"
)

# Link C++ Bridge with Lua, Rust, and Android System Log
target_link_libraries(vaultnotes_native
    PRIVATE
    lua_static
    vaultnotes_rust
    log
)
