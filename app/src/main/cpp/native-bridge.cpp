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
char* rust_encrypt_note(const char* content, const char* password);
char* rust_decrypt_note(const char* payload, const char* password);
bool rust_is_encrypted_payload(const char* payload);
char* rust_generate_vault_signature(const uint8_t* data, size_t len);
bool rust_verify_vault_signature(const uint8_t* data, size_t len, const char* sig);
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

// Hook de seguridad de Lua para prevenir bucles infinitos y bloqueos en el hilo principal (ANR).
// Interrumpe la ejecución de forma segura mediante luaL_error si el script excede 100,000 instrucciones.
static void lua_timeout_instruction_hook(lua_State* L, lua_Debug* /* ar */) {
    luaL_error(L, "Tiempo de ejecución excedido (límite de 100,000 instrucciones alcanzado para prevenir bucles infinitos)");
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

    // Protección nativa contra bucles infinitos: hook de conteo que aborta de forma segura
    lua_sethook(L, lua_timeout_instruction_hook, LUA_MASKCOUNT, 100000);

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

extern "C" JNIEXPORT jstring JNICALL
Java_com_example_native_NativeEngine_executeLuaWithContext(
        JNIEnv* env,
        jobject /* this */,
        jstring script,
        jstring content,
        jstring title) {

    if (script == nullptr) {
        return env->NewStringUTF("Error: Script nulo");
    }

    const char* nativeScript = env->GetStringUTFChars(script, nullptr);

    // Inicializar estado oficial de Lua en C
    lua_State* L = luaL_newstate();
    if (L == nullptr) {
        env->ReleaseStringUTFChars(script, nativeScript);
        return env->NewStringUTF("Error: No se pudo inicializar el estado de Lua en C");
    }

    luaL_openlibs(L);

    // Protección nativa contra bucles infinitos: hook de conteo que aborta de forma segura
    lua_sethook(L, lua_timeout_instruction_hook, LUA_MASKCOUNT, 100000);

    // Inyectar de forma segura las variables globales 'content' y 'title' para el script
    if (content != nullptr) {
        const char* nativeContent = env->GetStringUTFChars(content, nullptr);
        lua_pushstring(L, nativeContent);
        lua_setglobal(L, "content");
        env->ReleaseStringUTFChars(content, nativeContent);
    } else {
        lua_pushstring(L, "");
        lua_setglobal(L, "content");
    }

    if (title != nullptr) {
        const char* nativeTitle = env->GetStringUTFChars(title, nullptr);
        lua_pushstring(L, nativeTitle);
        lua_setglobal(L, "title");
        env->ReleaseStringUTFChars(title, nativeTitle);
    } else {
        lua_pushstring(L, "");
        lua_setglobal(L, "title");
    }

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

// -----------------------------------------------------------------------------
// High-Performance Native Vault Crypto & Signature Engine (Rust Core Integration)
// -----------------------------------------------------------------------------

extern "C" JNIEXPORT jstring JNICALL
Java_com_example_native_NativeEngine_generateVaultSignatureInRust(
        JNIEnv* env,
        jobject /* this */,
        jbyteArray data) {
    if (data == nullptr) {
        return env->NewStringUTF("");
    }
    jsize len = env->GetArrayLength(data);
    jbyte* bytes = env->GetByteArrayElements(data, nullptr);
    if (bytes == nullptr) {
        return env->NewStringUTF("");
    }

    char* rustSig = rust_generate_vault_signature(reinterpret_cast<const uint8_t*>(bytes), static_cast<size_t>(len));
    env->ReleaseByteArrayElements(data, bytes, JNI_ABORT);

    if (rustSig == nullptr) {
        return env->NewStringUTF("");
    }

    jstring result = env->NewStringUTF(rustSig);
    rust_core_free_string(rustSig);
    return result;
}

extern "C" JNIEXPORT jboolean JNICALL
Java_com_example_native_NativeEngine_verifyVaultSignatureInRust(
        JNIEnv* env,
        jobject /* this */,
        jbyteArray data,
        jstring signature) {
    if (data == nullptr || signature == nullptr) {
        return JNI_FALSE;
    }
    jsize len = env->GetArrayLength(data);
    jbyte* bytes = env->GetByteArrayElements(data, nullptr);
    if (bytes == nullptr) {
        return JNI_FALSE;
    }

    const char* nativeSig = env->GetStringUTFChars(signature, nullptr);
    bool verified = false;
    if (nativeSig != nullptr) {
        verified = rust_verify_vault_signature(
                reinterpret_cast<const uint8_t*>(bytes),
                static_cast<size_t>(len),
                nativeSig
        );
        env->ReleaseStringUTFChars(signature, nativeSig);
    }

    env->ReleaseByteArrayElements(data, bytes, JNI_ABORT);
    return verified ? JNI_TRUE : JNI_FALSE;
}

extern "C" JNIEXPORT jstring JNICALL
Java_com_example_native_NativeEngine_encryptNoteInRust(
        JNIEnv* env,
        jobject /* this */,
        jstring plaintext,
        jstring password) {
    if (plaintext == nullptr || password == nullptr) {
        return env->NewStringUTF("");
    }

    const char* nativePlaintext = env->GetStringUTFChars(plaintext, nullptr);
    const char* nativePassword = env->GetStringUTFChars(password, nullptr);

    char* encrypted = rust_encrypt_note(nativePlaintext, nativePassword);

    env->ReleaseStringUTFChars(plaintext, nativePlaintext);
    env->ReleaseStringUTFChars(password, nativePassword);

    if (encrypted == nullptr) {
        return env->NewStringUTF("");
    }

    jstring result = env->NewStringUTF(encrypted);
    rust_core_free_string(encrypted);
    return result;
}

extern "C" JNIEXPORT jstring JNICALL
Java_com_example_native_NativeEngine_decryptNoteInRust(
        JNIEnv* env,
        jobject /* this */,
        jstring payload,
        jstring password) {
    if (payload == nullptr || password == nullptr) {
        return nullptr;
    }

    const char* nativePayload = env->GetStringUTFChars(payload, nullptr);
    const char* nativePassword = env->GetStringUTFChars(password, nullptr);

    char* decrypted = rust_decrypt_note(nativePayload, nativePassword);

    env->ReleaseStringUTFChars(payload, nativePayload);
    env->ReleaseStringUTFChars(password, nativePassword);

    if (decrypted == nullptr) {
        return nullptr;
    }

    jstring result = env->NewStringUTF(decrypted);
    rust_core_free_string(decrypted);
    return result;
}

extern "C" JNIEXPORT jboolean JNICALL
Java_com_example_native_NativeEngine_isEncryptedPayloadInRust(
        JNIEnv* env,
        jobject /* this */,
        jstring payload) {
    if (payload == nullptr) {
        return JNI_FALSE;
    }

    const char* nativePayload = env->GetStringUTFChars(payload, nullptr);
    bool isEncrypted = rust_is_encrypted_payload(nativePayload);
    env->ReleaseStringUTFChars(payload, nativePayload);

    return isEncrypted ? JNI_TRUE : JNI_FALSE;
}

