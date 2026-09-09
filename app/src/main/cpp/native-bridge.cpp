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

// -----------------------------------------------------------------------------
// High-Performance Native Vault Crypto & Signature Engine
// -----------------------------------------------------------------------------

namespace vault_crypto {
    static const uint32_t K[64] = {
        0x428a2f98, 0x71374491, 0xb5c0fbcf, 0xe9b5dba5,
        0x3956c25b, 0x59f111f1, 0x923f82a4, 0xab1c5ed5,
        0xd807aa98, 0x12835b01, 0x243185be, 0x550c7dc3,
        0x72be5d74, 0x80deb1fe, 0x9bdc06a7, 0xc19bf174,
        0xe49b69c1, 0xefbe4786, 0x0fc19dc6, 0x240ca1cc,
        0x2de92c6f, 0x4a7484aa, 0x5cb0a9dc, 0x76f988da,
        0x983e5152, 0xa831c66d, 0xb00327c8, 0xbf597fc7,
        0xc6e00bf3, 0xd5a79147, 0x06ca6351, 0x14292967,
        0x27b70a85, 0x2e1b2138, 0x4d2c6dfc, 0x53380d13,
        0x650a7354, 0x766a0abb, 0x81c2c92e, 0x92722c85,
        0xa2bfe8a1, 0xa81a664b, 0xc24b8b70, 0xc76c51a3,
        0xd192e819, 0xd6990624, 0xf40e3585, 0x106aa070,
        0x19a4c116, 0x1e376c08, 0x2748774c, 0x34b0bcb5,
        0x391c0cb3, 0x4ed8aa4a, 0x5b9cca4f, 0x682e6ff3,
        0x748f82ee, 0x78a5636f, 0x84c87814, 0x8cc70208,
        0x90befffa, 0xa4506ceb, 0xbef9a3f7, 0xc67178f2
    };

    static inline uint32_t rotr(uint32_t x, uint32_t n) {
        return (x >> n) | (x << (32 - n));
    }

    static std::string sha256_hex(const uint8_t* data, size_t len) {
        uint32_t h0 = 0x6a09e667;
        uint32_t h1 = 0xbb67ae85;
        uint32_t h2 = 0x3c6ef372;
        uint32_t h3 = 0xa54ff53a;
        uint32_t h4 = 0x510e527f;
        uint32_t h5 = 0x9b05688c;
        uint32_t h6 = 0x1f83d9ab;
        uint32_t h7 = 0x5be0cd19;

        uint64_t bit_len = static_cast<uint64_t>(len) * 8ULL;
        std::vector<uint8_t> msg;
        msg.reserve(len + 64);
        msg.insert(msg.end(), data, data + len);
        msg.push_back(0x80);
        while ((msg.size() % 64) != 56) {
            msg.push_back(0x00);
        }
        for (int i = 7; i >= 0; --i) {
            msg.push_back(static_cast<uint8_t>((bit_len >> (i * 8)) & 0xFF));
        }

        for (size_t offset = 0; offset < msg.size(); offset += 64) {
            uint32_t w[64];
            for (size_t i = 0; i < 16; ++i) {
                w[i] = (static_cast<uint32_t>(msg[offset + i * 4]) << 24) |
                       (static_cast<uint32_t>(msg[offset + i * 4 + 1]) << 16) |
                       (static_cast<uint32_t>(msg[offset + i * 4 + 2]) << 8) |
                       (static_cast<uint32_t>(msg[offset + i * 4 + 3]));
            }
            for (size_t i = 16; i < 64; ++i) {
                uint32_t s0 = rotr(w[i - 15], 7) ^ rotr(w[i - 15], 18) ^ (w[i - 15] >> 3);
                uint32_t s1 = rotr(w[i - 2], 17) ^ rotr(w[i - 2], 19) ^ (w[i - 2] >> 10);
                w[i] = w[i - 16] + s0 + w[i - 7] + s1;
            }

            uint32_t a = h0, b = h1, c = h2, d = h3, e = h4, f = h5, g = h6, h = h7;
            for (size_t i = 0; i < 64; ++i) {
                uint32_t s1 = rotr(e, 6) ^ rotr(e, 11) ^ rotr(e, 25);
                uint32_t ch = (e & f) ^ ((~e) & g);
                uint32_t temp1 = h + s1 + ch + K[i] + w[i];
                uint32_t s0 = rotr(a, 2) ^ rotr(a, 13) ^ rotr(a, 22);
                uint32_t maj = (a & b) ^ (a & c) ^ (b & c);
                uint32_t temp2 = s0 + maj;

                h = g;
                g = f;
                f = e;
                e = d + temp1;
                d = c;
                c = b;
                b = a;
                a = temp1 + temp2;
            }

            h0 += a; h1 += b; h2 += c; h3 += d;
            h4 += e; h5 += f; h6 += g; h7 += h;
        }

        uint32_t digest[8] = {h0, h1, h2, h3, h4, h5, h6, h7};
        char hex[65];
        for (int i = 0; i < 8; ++i) {
            snprintf(hex + (i * 8), 9, "%08x", digest[i]);
        }
        hex[64] = '\0';
        return std::string(hex);
    }

    static const char* VAULT_SALT = "VAULTNOTES_AUTHENTIC_PACKAGE_SIGNATURE_SALT_V1";

    static std::string generate_signature(const uint8_t* data, size_t len) {
        std::vector<uint8_t> salted;
        size_t salt_len = strlen(VAULT_SALT);
        salted.reserve(salt_len + len);
        salted.insert(salted.end(), VAULT_SALT, VAULT_SALT + salt_len);
        salted.insert(salted.end(), data, data + len);

        return "VAULT_SIG_V1:" + sha256_hex(salted.data(), salted.size());
    }

    static bool verify_signature(const uint8_t* data, size_t len, const std::string& signature) {
        std::string expected = generate_signature(data, len);
        if (expected.length() != signature.length()) {
            return false;
        }
        uint8_t diff = 0;
        for (size_t i = 0; i < expected.length(); ++i) {
            diff |= static_cast<uint8_t>(expected[i] ^ signature[i]);
        }
        return diff == 0;
    }
}

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

    std::string sig = vault_crypto::generate_signature(reinterpret_cast<const uint8_t*>(bytes), static_cast<size_t>(len));
    env->ReleaseByteArrayElements(data, bytes, JNI_ABORT);

    return env->NewStringUTF(sig.c_str());
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
    std::string sigStr = nativeSig != nullptr ? nativeSig : "";
    if (nativeSig != nullptr) {
        env->ReleaseStringUTFChars(signature, nativeSig);
    }

    bool verified = vault_crypto::verify_signature(
            reinterpret_cast<const uint8_t*>(bytes),
            static_cast<size_t>(len),
            sigStr
    );

    env->ReleaseByteArrayElements(data, bytes, JNI_ABORT);
    return static_cast<jboolean>(verified);
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
    free(encrypted);
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
    free(decrypted);
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

