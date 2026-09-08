#include <jni.h>
#include <string>
#include <sstream>
#include <android/log.h>

#define TAG "VaultNotesNative"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, TAG, __VA_ARGS__)

extern "C" {
#include "lua/lua.h"
#include "lua/lualib.h"
#include "lua/lauxlib.h"

// Rust Core Declarations
char* rust_core_status();
void rust_core_free_string(char* ptr);
uint32_t rust_calculate_reading_time_secs(uint32_t word_count);
uint32_t rust_count_words(const char* text);
char* rust_process_note_summary(const char* content, uint32_t max_snippet_len);
bool rust_match_note(const char* query, const char* title, const char* content, const char* tags);
}

extern "C" JNIEXPORT jstring JNICALL
Java_com_example_native_NativeEngine_getNativeInfo(
        JNIEnv* env,
        jobject /* this */) {

    std::ostringstream oss;

    // 1. C++ Compiler Information
    oss << "⚙️ C++ Standard: " << __cplusplus << "\n";

    // 2. Official C Lua Version
    oss << "🌙 " << LUA_RELEASE << " (" << LUA_COPYRIGHT << ")\n";

    // 3. Rust Engine Status via FFI
    char* rustStatus = rust_core_status();
    if (rustStatus != nullptr) {
        oss << "🦀 " << rustStatus << "\n";
        rust_core_free_string(rustStatus);
    } else {
        oss << "🦀 Rust Core: Inactive\n";
    }

    return env->NewStringUTF(oss.str().c_str());
}

extern "C" JNIEXPORT jstring JNICALL
Java_com_example_native_NativeEngine_executeLua(
        JNIEnv* env,
        jobject /* this */,
        jstring script) {

    if (script == nullptr) {
        return env->NewStringUTF("Error: Script nulo");
    }

    const char* nativeScript = env->GetStringUTFChars(script, nullptr);

    // Initialize official C Lua state
    lua_State* L = luaL_newstate();
    if (L == nullptr) {
        env->ReleaseStringUTFChars(script, nativeScript);
        return env->NewStringUTF("Error: No se pudo inicializar el estado de Lua en C");
    }

    luaL_openlibs(L);

    std::string result;
    int status = luaL_dostring(L, nativeScript);
    if (status != LUA_OK) {
        const char* err = lua_tostring(L, -1);
        result = std::string("Error Lua: ") + (err ? err : "desconocido");
    } else {
        if (lua_isstring(L, -1)) {
            result = lua_tostring(L, -1);
        } else if (lua_isnumber(L, -1)) {
            result = std::to_string(lua_tonumber(L, -1));
        } else if (lua_isboolean(L, -1)) {
            result = lua_toboolean(L, -1) ? "true" : "false";
        } else {
            result = "OK (Ejecución completada sin valor de retorno)";
        }
    }

    lua_close(L);
    env->ReleaseStringUTFChars(script, nativeScript);

    return env->NewStringUTF(result.c_str());
}

extern "C" JNIEXPORT jint JNICALL
Java_com_example_native_NativeEngine_calculateReadingTimeInRust(
        JNIEnv* /* env */,
        jobject /* this */,
        jint word_count) {
    return static_cast<jint>(rust_calculate_reading_time_secs(static_cast<uint32_t>(word_count)));
}

extern "C" JNIEXPORT jstring JNICALL
Java_com_example_native_NativeEngine_processNoteSummaryInRust(
        JNIEnv* env,
        jobject /* this */,
        jstring content,
        jint max_snippet_len) {
    const char* nativeContent = content != nullptr ? env->GetStringUTFChars(content, nullptr) : "";
    char* result = rust_process_note_summary(nativeContent, static_cast<uint32_t>(max_snippet_len));
    if (content != nullptr) {
        env->ReleaseStringUTFChars(content, nativeContent);
    }
    jstring jResult = env->NewStringUTF(result != nullptr ? result : "0|0|");
    if (result != nullptr) {
        rust_core_free_string(result);
    }
    return jResult;
}

extern "C" JNIEXPORT jboolean JNICALL
Java_com_example_native_NativeEngine_matchNoteInRust(
        JNIEnv* env,
        jobject /* this */,
        jstring query,
        jstring title,
        jstring content,
        jstring tags) {
    const char* nativeQuery = query != nullptr ? env->GetStringUTFChars(query, nullptr) : "";
    const char* nativeTitle = title != nullptr ? env->GetStringUTFChars(title, nullptr) : "";
    const char* nativeContent = content != nullptr ? env->GetStringUTFChars(content, nullptr) : "";
    const char* nativeTags = tags != nullptr ? env->GetStringUTFChars(tags, nullptr) : "";

    bool matched = rust_match_note(nativeQuery, nativeTitle, nativeContent, nativeTags);

    if (query != nullptr) env->ReleaseStringUTFChars(query, nativeQuery);
    if (title != nullptr) env->ReleaseStringUTFChars(title, nativeTitle);
    if (content != nullptr) env->ReleaseStringUTFChars(content, nativeContent);
    if (tags != nullptr) env->ReleaseStringUTFChars(tags, nativeTags);

    return static_cast<jboolean>(matched);
}

