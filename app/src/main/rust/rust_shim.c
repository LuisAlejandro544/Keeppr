#define _GNU_SOURCE
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <stdbool.h>
#include <stdint.h>
#include <ctype.h>
#include <math.h>
#include <time.h>

char* rust_core_status(void) {
    const char* msg = "Rust Core Engine v0.2.0 (High-Performance Text & Search Engine)";
    return strdup(msg);
}

void rust_core_free_string(char* ptr) {
    if (ptr != NULL) {
        free(ptr);
    }
}

uint32_t rust_calculate_reading_time_secs(uint32_t word_count) {
    if (word_count == 0) {
        return 0;
    }
    return (uint32_t)ceil(((double)word_count / 200.0) * 60.0);
}

uint32_t rust_count_words(const char* text) {
    if (text == NULL) {
        return 0;
    }
    bool in_word = false;
    uint32_t count = 0;
    for (const char* p = text; *p != '\0'; p++) {
        if (isspace((unsigned char)*p)) {
            in_word = false;
        } else if (!in_word) {
            in_word = true;
            count++;
        }
    }
    return count;
}

static bool starts_with(const char* str, const char* prefix) {
    return strncmp(str, prefix, strlen(prefix)) == 0;
}

static char* trim_inplace(char* str) {
    while (isspace((unsigned char)*str)) str++;
    if (*str == 0) return str;
    char* end = str + strlen(str) - 1;
    while (end > str && isspace((unsigned char)*end)) end--;
    end[1] = '\0';
    return str;
}

char* rust_process_note_summary(const char* content, uint32_t max_snippet_len) {
    if (content == NULL) {
        return strdup("0|0|");
    }

    uint32_t words = rust_count_words(content);
    uint32_t reading_secs = rust_calculate_reading_time_secs(words);

    size_t max_len = (max_snippet_len == 0) ? 120 : (size_t)max_snippet_len;
    char* content_copy = strdup(content);
    if (!content_copy) {
        return strdup("0|0|");
    }

    char* snippet = (char*)malloc(max_len + 4);
    if (!snippet) {
        free(content_copy);
        return strdup("0|0|");
    }
    snippet[0] = '\0';
    size_t current_len = 0;

    char* line = strtok(content_copy, "\r\n");
    while (line != NULL && current_len < max_len) {
        char* trimmed = trim_inplace(line);
        if (*trimmed == '\0' || trimmed[0] == '#') {
            line = strtok(NULL, "\r\n");
            continue;
        }

        const char* clean_line = trimmed;
        if (starts_with(trimmed, "- [ ] ")) {
            clean_line = trimmed + 6;
        } else if (starts_with(trimmed, "- [x] ")) {
            clean_line = trimmed + 6;
        } else if (starts_with(trimmed, "- ")) {
            clean_line = trimmed + 2;
        } else if (starts_with(trimmed, "* ")) {
            clean_line = trimmed + 2;
        }

        size_t part_len = strlen(clean_line);
        if (part_len > 0) {
            if (current_len > 0 && current_len < max_len) {
                strcat(snippet, " ");
                current_len++;
            }
            size_t to_copy = (max_len > current_len) ? (max_len - current_len) : 0;
            if (to_copy > part_len) to_copy = part_len;
            strncat(snippet, clean_line, to_copy);
            current_len += to_copy;
        }
        line = strtok(NULL, "\r\n");
    }

    free(content_copy);

    char result[256 + max_len];
    snprintf(result, sizeof(result), "%u|%u|%s", words, reading_secs, snippet);
    free(snippet);
    return strdup(result);
}

static bool contains_case_insensitive(const char* haystack, const char* needle_lower) {
    if (!haystack || !needle_lower) return false;
    size_t h_len = strlen(haystack);
    size_t n_len = strlen(needle_lower);
    if (n_len == 0) return true;
    if (h_len < n_len) return false;

    for (size_t i = 0; i <= h_len - n_len; i++) {
        bool match = true;
        for (size_t j = 0; j < n_len; j++) {
            if (tolower((unsigned char)haystack[i + j]) != (unsigned char)needle_lower[j]) {
                match = false;
                break;
            }
        }
        if (match) return true;
    }
    return false;
}

bool rust_match_note(
    const char* query,
    const char* title,
    const char* content,
    const char* tags
) {
    if (query == NULL) return true;

    while (isspace((unsigned char)*query)) query++;
    if (*query == '\0') return true;

    size_t q_len = strlen(query);
    char* q_lower = (char*)malloc(q_len + 1);
    if (!q_lower) return true;
    for (size_t i = 0; i < q_len; i++) {
        q_lower[i] = (char)tolower((unsigned char)query[i]);
    }
    q_lower[q_len] = '\0';
    while (q_len > 0 && isspace((unsigned char)q_lower[q_len - 1])) {
        q_lower[--q_len] = '\0';
    }
    if (q_len == 0) {
        free(q_lower);
        return true;
    }

    bool matched = false;
    if (title && contains_case_insensitive(title, q_lower)) {
        matched = true;
    } else if (tags && contains_case_insensitive(tags, q_lower)) {
        matched = true;
    } else if (content && contains_case_insensitive(content, q_lower)) {
        matched = true;
    }

    free(q_lower);
    return matched;
}

/* ========================================================================= */
/* --- Motor Criptográfico Nativo de Cifrado y Descifrado (AES-256 + HMAC) -- */
/* ========================================================================= */

static const uint32_t K256[64] = {
    0x428a2f98, 0x71374491, 0xb5c0fbcf, 0xe9b5dba5, 0x3956c25b, 0x59f111f1, 0x923f82a4, 0xab1c5ed5,
    0xd807aa98, 0x12835b01, 0x243185be, 0x550c7dc3, 0x72be5d74, 0x80deb1fe, 0x9bdc06a7, 0xc19bf174,
    0xe49b69c1, 0xefbe4786, 0x0fc19dc6, 0x240ca1cc, 0x2de92c6f, 0x4a7484aa, 0x5cb0a9dc, 0x76f988da,
    0x983e5152, 0xa831c66d, 0xb00327c8, 0xbf597fc7, 0xc6e00bf3, 0xd5a79147, 0x06ca6351, 0x14292967,
    0x27b70a85, 0x2e1b2138, 0x4d2c6dfc, 0x53380d13, 0x650a7354, 0x766a0abb, 0x81c2c92e, 0x92722c85,
    0xa2bfe8a1, 0xa81a664b, 0xc24b8b70, 0xc76c51a3, 0xd192e819, 0xd6990624, 0xf40e3585, 0x106aa070,
    0x19a4c116, 0x1e376c08, 0x2748774c, 0x34b0bcb5, 0x391c0cb3, 0x4ed8aa4a, 0x5b9cca4f, 0x682e6ff3,
    0x748f82ee, 0x78a5636f, 0x84c87814, 0x8cc70208, 0x90befffa, 0xa4506ceb, 0xbef9a3f7, 0xc67178f2
};

static inline uint32_t rotr32(uint32_t x, uint32_t n) {
    return (x >> n) | (x << (32 - n));
}

static void sha256_buffer(const uint8_t* data, size_t len, uint8_t out[32]) {
    uint32_t h0 = 0x6a09e667, h1 = 0xbb67ae85, h2 = 0x3c6ef372, h3 = 0xa54ff53a;
    uint32_t h4 = 0x510e527f, h5 = 0x9b05688c, h6 = 0x1f83d9ab, h7 = 0x5be0cd19;

    size_t new_len = len + 1 + 8;
    while (new_len % 64 != 0) new_len++;

    uint8_t* msg = (uint8_t*)calloc(new_len, 1);
    if (!msg) return;
    if (len > 0) memcpy(msg, data, len);
    msg[len] = 0x80;

    uint64_t bits = (uint64_t)len * 8;
    for (int i = 0; i < 8; ++i) {
        msg[new_len - 1 - i] = (uint8_t)((bits >> (i * 8)) & 0xff);
    }

    for (size_t chunk = 0; chunk < new_len; chunk += 64) {
        uint32_t w[64];
        for (int i = 0; i < 16; ++i) {
            w[i] = ((uint32_t)msg[chunk + i * 4] << 24) |
                   ((uint32_t)msg[chunk + i * 4 + 1] << 16) |
                   ((uint32_t)msg[chunk + i * 4 + 2] << 8) |
                   ((uint32_t)msg[chunk + i * 4 + 3]);
        }
        for (int i = 16; i < 64; ++i) {
            uint32_t s0 = rotr32(w[i - 15], 7) ^ rotr32(w[i - 15], 18) ^ (w[i - 15] >> 3);
            uint32_t s1 = rotr32(w[i - 2], 17) ^ rotr32(w[i - 2], 19) ^ (w[i - 2] >> 10);
            w[i] = w[i - 16] + s0 + w[i - 7] + s1;
        }

        uint32_t a = h0, b = h1, c = h2, d = h3;
        uint32_t e = h4, f = h5, g = h6, h = h7;

        for (int i = 0; i < 64; ++i) {
            uint32_t S1 = rotr32(e, 6) ^ rotr32(e, 11) ^ rotr32(e, 25);
            uint32_t ch = (e & f) ^ ((~e) & g);
            uint32_t temp1 = h + S1 + ch + K256[i] + w[i];
            uint32_t S0 = rotr32(a, 2) ^ rotr32(a, 13) ^ rotr32(a, 22);
            uint32_t maj = (a & b) ^ (a & c) ^ (b & c);
            uint32_t temp2 = S0 + maj;

            h = g; g = f; f = e; e = d + temp1;
            d = c; c = b; b = a; a = temp1 + temp2;
        }

        h0 += a; h1 += b; h2 += c; h3 += d;
        h4 += e; h5 += f; h6 += g; h7 += h;
    }
    free(msg);

    uint32_t digests[8] = {h0, h1, h2, h3, h4, h5, h6, h7};
    for (int i = 0; i < 8; ++i) {
        out[i * 4]     = (uint8_t)((digests[i] >> 24) & 0xff);
        out[i * 4 + 1] = (uint8_t)((digests[i] >> 16) & 0xff);
        out[i * 4 + 2] = (uint8_t)((digests[i] >> 8) & 0xff);
        out[i * 4 + 3] = (uint8_t)(digests[i] & 0xff);
    }
}

static void hmac_sha256(const uint8_t* key, size_t key_len, const uint8_t* data, size_t data_len, uint8_t out[32]) {
    uint8_t k[64] = {0};
    if (key_len > 64) {
        sha256_buffer(key, key_len, k);
    } else {
        memcpy(k, key, key_len);
    }

    uint8_t o_key_pad[64];
    uint8_t i_key_pad[64];
    for (int i = 0; i < 64; ++i) {
        o_key_pad[i] = k[i] ^ 0x5c;
        i_key_pad[i] = k[i] ^ 0x36;
    }

    uint8_t* inner = (uint8_t*)malloc(64 + data_len);
    if (!inner) return;
    memcpy(inner, i_key_pad, 64);
    if (data_len > 0) memcpy(inner + 64, data, data_len);

    uint8_t inner_hash[32];
    sha256_buffer(inner, 64 + data_len, inner_hash);
    free(inner);

    uint8_t outer[64 + 32];
    memcpy(outer, o_key_pad, 64);
    memcpy(outer + 64, inner_hash, 32);
    sha256_buffer(outer, 64 + 32, out);
}

static void pbkdf2_hmac_sha256(const char* password, const uint8_t* salt, size_t salt_len, uint32_t iterations, uint8_t* out, size_t out_len) {
    size_t pass_len = strlen(password);
    uint32_t num_blocks = (out_len + 31) / 32;
    uint8_t asalt[salt_len + 4];
    memcpy(asalt, salt, salt_len);

    for (uint32_t block = 1; block <= num_blocks; block++) {
        asalt[salt_len]     = (uint8_t)((block >> 24) & 0xff);
        asalt[salt_len + 1] = (uint8_t)((block >> 16) & 0xff);
        asalt[salt_len + 2] = (uint8_t)((block >> 8) & 0xff);
        asalt[salt_len + 3] = (uint8_t)(block & 0xff);

        uint8_t u[32];
        uint8_t t[32];
        hmac_sha256((const uint8_t*)password, pass_len, asalt, salt_len + 4, u);
        memcpy(t, u, 32);

        for (uint32_t iter = 1; iter < iterations; iter++) {
            hmac_sha256((const uint8_t*)password, pass_len, u, 32, u);
            for (int i = 0; i < 32; i++) {
                t[i] ^= u[i];
            }
        }

        size_t offset = (block - 1) * 32;
        size_t copy_len = (out_len - offset < 32) ? (out_len - offset) : 32;
        memcpy(out + offset, t, copy_len);
    }
}

/* --- AES-256 Core Engine --- */
static const uint8_t AES_SBOX[256] = {
    0x63, 0x7c, 0x77, 0x7b, 0xf2, 0x6b, 0x6f, 0xc5, 0x30, 0x01, 0x67, 0x2b, 0xfe, 0xd7, 0xab, 0x76,
    0xca, 0x82, 0xc9, 0x7d, 0xfa, 0x59, 0x47, 0xf0, 0xad, 0xd4, 0xa2, 0xaf, 0x9c, 0xa4, 0x72, 0xc0,
    0xb7, 0xfd, 0x93, 0x26, 0x36, 0x3f, 0xf7, 0xcc, 0x34, 0xa5, 0xe5, 0xf1, 0x71, 0xd8, 0x31, 0x15,
    0x04, 0xc7, 0x23, 0xc3, 0x18, 0x96, 0x05, 0x9a, 0x07, 0x12, 0x80, 0xe2, 0xeb, 0x27, 0xb2, 0x75,
    0x09, 0x83, 0x2c, 0x1a, 0x1b, 0x6e, 0x5a, 0xa0, 0x52, 0x3b, 0xd6, 0xb3, 0x29, 0xe3, 0x2f, 0x84,
    0x53, 0xd1, 0x00, 0xed, 0x20, 0xfc, 0xb1, 0x5b, 0x6a, 0xcb, 0xbe, 0x39, 0x4a, 0x4c, 0x58, 0xcf,
    0xd0, 0xef, 0xaa, 0xfb, 0x43, 0x4d, 0x33, 0x85, 0x45, 0xf9, 0x02, 0x7f, 0x50, 0x3c, 0x9f, 0xa8,
    0x51, 0xa3, 0x40, 0x8f, 0x92, 0x9d, 0x38, 0xf5, 0xbc, 0xb6, 0xda, 0x21, 0x10, 0xff, 0xf3, 0xd2,
    0xcd, 0x0c, 0x13, 0xec, 0x5f, 0x97, 0x44, 0x17, 0xc4, 0xa7, 0x7e, 0x3d, 0x64, 0x5d, 0x19, 0x73,
    0x60, 0x81, 0x4f, 0xdc, 0x22, 0x2a, 0x90, 0x88, 0x46, 0xee, 0xb8, 0x14, 0xde, 0x5e, 0x0b, 0xdb,
    0xe0, 0x32, 0x3a, 0x0a, 0x49, 0x06, 0x24, 0x5c, 0xc2, 0xd3, 0xac, 0x62, 0x91, 0x95, 0xe4, 0x79,
    0xe7, 0xc8, 0x37, 0x6d, 0x8d, 0xd5, 0x4e, 0xa9, 0x6c, 0x56, 0xf4, 0xea, 0x65, 0x7a, 0xae, 0x08,
    0xba, 0x78, 0x25, 0x2e, 0x1c, 0xa6, 0xb4, 0xc6, 0xe8, 0xdd, 0x74, 0x1f, 0x4b, 0xbd, 0x8b, 0x8a,
    0x70, 0x3e, 0xb5, 0x66, 0x48, 0x03, 0xf6, 0x0e, 0x61, 0x35, 0x57, 0xb9, 0x86, 0xc1, 0x1d, 0x9e,
    0xe1, 0xf8, 0x98, 0x11, 0x69, 0xd9, 0x8e, 0x94, 0x9b, 0x1e, 0x87, 0xe9, 0xce, 0x55, 0x28, 0xdf,
    0x8c, 0xa1, 0x89, 0x0d, 0xbf, 0xe6, 0x42, 0x68, 0x41, 0x99, 0x2d, 0x0f, 0xb0, 0x54, 0xbb, 0x16
};

static const uint8_t AES_RCON[15] = {
    0x00, 0x01, 0x02, 0x04, 0x08, 0x10, 0x20, 0x40, 0x80, 0x1b, 0x36, 0x6c, 0xd8, 0xab, 0x4d
};

static inline uint8_t xtime(uint8_t x) {
    return (uint8_t)((x << 1) ^ (((x >> 7) & 1) * 0x1b));
}

static void aes256_key_expansion(const uint8_t key[32], uint8_t round_keys[240]) {
    memcpy(round_keys, key, 32);
    int bytes_generated = 32;
    int rcon_idx = 1;
    uint8_t temp[4];

    while (bytes_generated < 240) {
        for (int i = 0; i < 4; i++) temp[i] = round_keys[bytes_generated - 4 + i];
        if (bytes_generated % 32 == 0) {
            uint8_t t = temp[0];
            temp[0] = AES_SBOX[temp[1]] ^ AES_RCON[rcon_idx++];
            temp[1] = AES_SBOX[temp[2]];
            temp[2] = AES_SBOX[temp[3]];
            temp[3] = AES_SBOX[t];
        } else if (bytes_generated % 32 == 16) {
            for (int i = 0; i < 4; i++) temp[i] = AES_SBOX[temp[i]];
        }
        for (int i = 0; i < 4; i++) {
            round_keys[bytes_generated] = round_keys[bytes_generated - 32] ^ temp[i];
            bytes_generated++;
        }
    }
}

static void aes256_encrypt_block(const uint8_t in[16], uint8_t out[16], const uint8_t round_keys[240]) {
    uint8_t state[4][4];
    for (int r = 0; r < 4; r++) {
        for (int c = 0; c < 4; c++) {
            state[r][c] = in[r + 4 * c] ^ round_keys[r + 4 * c];
        }
    }

    for (int round = 1; round <= 14; round++) {
        // SubBytes
        for (int r = 0; r < 4; r++) {
            for (int c = 0; c < 4; c++) {
                state[r][c] = AES_SBOX[state[r][c]];
            }
        }
        // ShiftRows
        uint8_t temp = state[1][0];
        state[1][0] = state[1][1]; state[1][1] = state[1][2]; state[1][2] = state[1][3]; state[1][3] = temp;

        temp = state[2][0]; uint8_t temp2 = state[2][1];
        state[2][0] = state[2][2]; state[2][1] = state[2][3]; state[2][2] = temp; state[2][3] = temp2;

        temp = state[3][3];
        state[3][3] = state[3][2]; state[3][2] = state[3][1]; state[3][1] = state[3][0]; state[3][0] = temp;

        // MixColumns (rounds 1 to 13)
        if (round < 14) {
            for (int c = 0; c < 4; c++) {
                uint8_t a0 = state[0][c], a1 = state[1][c], a2 = state[2][c], a3 = state[3][c];
                uint8_t t = a0 ^ a1 ^ a2 ^ a3;
                state[0][c] ^= t ^ xtime(a0 ^ a1);
                state[1][c] ^= t ^ xtime(a1 ^ a2);
                state[2][c] ^= t ^ xtime(a2 ^ a3);
                state[3][c] ^= t ^ xtime(a3 ^ a0);
            }
        }

        // AddRoundKey
        int rk_offset = round * 16;
        for (int r = 0; r < 4; r++) {
            for (int c = 0; c < 4; c++) {
                state[r][c] ^= round_keys[rk_offset + r + 4 * c];
            }
        }
    }

    for (int r = 0; r < 4; r++) {
        for (int c = 0; c < 4; c++) {
            out[r + 4 * c] = state[r][c];
        }
    }
}

static void aes256_ctr_crypt(const uint8_t key[32], const uint8_t iv[16], const uint8_t* in, uint8_t* out, size_t len) {
    uint8_t round_keys[240];
    aes256_key_expansion(key, round_keys);

    uint8_t counter[16];
    memcpy(counter, iv, 16);

    uint8_t keystream[16];
    for (size_t i = 0; i < len; i += 16) {
        aes256_encrypt_block(counter, keystream, round_keys);
        size_t block_len = (len - i < 16) ? (len - i) : 16;
        for (size_t b = 0; b < block_len; b++) {
            out[i + b] = in[i + b] ^ keystream[b];
        }
        // Increment 128-bit counter
        for (int c = 15; c >= 0; c--) {
            if (++counter[c] != 0) break;
        }
    }
}

static void get_random_bytes(uint8_t* buf, size_t len) {
    FILE* f = fopen("/dev/urandom", "rb");
    if (f) {
        size_t r = fread(buf, 1, len, f);
        fclose(f);
        if (r == len) return;
    }
    // Fallback con reloj del sistema
    uint64_t seed = (uint64_t)time(NULL) ^ (uintptr_t)buf;
    for (size_t i = 0; i < len; i++) {
        seed = seed * 6364136223846793005ULL + 1;
        buf[i] = (uint8_t)(seed >> 33);
    }
}

static void to_hex(const uint8_t* in, size_t len, char* out) {
    static const char hex_chars[] = "0123456789abcdef";
    for (size_t i = 0; i < len; i++) {
        out[i * 2]     = hex_chars[(in[i] >> 4) & 0x0f];
        out[i * 2 + 1] = hex_chars[in[i] & 0x0f];
    }
    out[len * 2] = '\0';
}

static int from_hex(const char* in, size_t in_len, uint8_t* out) {
    if (in_len % 2 != 0) return -1;
    for (size_t i = 0; i < in_len / 2; i++) {
        char c1 = in[i * 2];
        char c2 = in[i * 2 + 1];
        int v1 = (c1 >= '0' && c1 <= '9') ? (c1 - '0') :
                 (c1 >= 'a' && c1 <= 'f') ? (c1 - 'a' + 10) :
                 (c1 >= 'A' && c1 <= 'F') ? (c1 - 'A' + 10) : -1;
        int v2 = (c2 >= '0' && c2 <= '9') ? (c2 - '0') :
                 (c2 >= 'a' && c2 <= 'f') ? (c2 - 'a' + 10) :
                 (c2 >= 'A' && c2 <= 'F') ? (c2 - 'A' + 10) : -1;
        if (v1 == -1 || v2 == -1) return -1;
        out[i] = (uint8_t)((v1 << 4) | v2);
    }
    return 0;
}

bool rust_is_encrypted_payload(const char* payload) {
    if (!payload) return false;
    return strncmp(payload, "VAULT_ENC_V1$", 13) == 0;
}

char* rust_encrypt_note(const char* content, const char* password) {
    if (!content || !password || strlen(password) == 0) {
        return strdup("");
    }

    uint8_t salt[16];
    uint8_t iv[16];
    get_random_bytes(salt, sizeof(salt));
    get_random_bytes(iv, sizeof(iv));

    // Derivar 64 bytes: 32 para clave AES, 32 para clave HMAC
    uint8_t derived_keys[64];
    pbkdf2_hmac_sha256(password, salt, sizeof(salt), 10000, derived_keys, 64);
    uint8_t aes_key[32];
    uint8_t mac_key[32];
    memcpy(aes_key, derived_keys, 32);
    memcpy(mac_key, derived_keys + 32, 32);

    size_t content_len = strlen(content);
    uint8_t* ciphertext = (uint8_t*)malloc(content_len);
    if (!ciphertext) return strdup("");
    aes256_ctr_crypt(aes_key, iv, (const uint8_t*)content, ciphertext, content_len);

    // Calcular MAC sobre salt || iv || ciphertext (Encrypt-then-MAC)
    size_t mac_input_len = 16 + 16 + content_len;
    uint8_t* mac_input = (uint8_t*)malloc(mac_input_len);
    if (!mac_input) {
        free(ciphertext);
        return strdup("");
    }
    memcpy(mac_input, salt, 16);
    memcpy(mac_input + 16, iv, 16);
    if (content_len > 0) memcpy(mac_input + 32, ciphertext, content_len);

    uint8_t mac[32];
    hmac_sha256(mac_key, 32, mac_input, mac_input_len, mac);
    free(mac_input);

    char salt_hex[33], iv_hex[33], mac_hex[65];
    to_hex(salt, 16, salt_hex);
    to_hex(iv, 16, iv_hex);
    to_hex(mac, 32, mac_hex);

    char* cipher_hex = (char*)malloc(content_len * 2 + 1);
    if (!cipher_hex) {
        free(ciphertext);
        return strdup("");
    }
    to_hex(ciphertext, content_len, cipher_hex);
    free(ciphertext);

    // Formato: VAULT_ENC_V1$<salt>$<iv>$<ciphertext>$<mac>
    size_t result_len = 13 + 32 + 1 + 32 + 1 + (content_len * 2) + 1 + 64 + 1;
    char* result = (char*)malloc(result_len);
    if (!result) {
        free(cipher_hex);
        return strdup("");
    }
    snprintf(result, result_len, "VAULT_ENC_V1$%s$%s$%s$%s", salt_hex, iv_hex, cipher_hex, mac_hex);
    free(cipher_hex);
    return result;
}

char* rust_decrypt_note(const char* payload, const char* password) {
    if (!payload || !password || !rust_is_encrypted_payload(payload)) {
        return NULL;
    }

    // Parsear los 4 componentes separados por $
    const char* p = payload + 13; // Omitir "VAULT_ENC_V1$"
    const char* d1 = strchr(p, '$');
    if (!d1) return NULL;
    const char* d2 = strchr(d1 + 1, '$');
    if (!d2) return NULL;
    const char* d3 = strchr(d2 + 1, '$');
    if (!d3) return NULL;

    size_t salt_len = d1 - p;
    size_t iv_len = d2 - (d1 + 1);
    size_t cipher_len = d3 - (d2 + 1);
    size_t mac_len = strlen(d3 + 1);

    if (salt_len != 32 || iv_len != 32 || mac_len != 64 || cipher_len % 2 != 0) {
        return NULL;
    }

    uint8_t salt[16], iv[16], expected_mac[32];
    if (from_hex(p, 32, salt) != 0 ||
        from_hex(d1 + 1, 32, iv) != 0 ||
        from_hex(d3 + 1, 64, expected_mac) != 0) {
        return NULL;
    }

    size_t ct_bytes_len = cipher_len / 2;
    uint8_t* ciphertext = (uint8_t*)malloc(ct_bytes_len);
    if (!ciphertext) return NULL;
    if (from_hex(d2 + 1, cipher_len, ciphertext) != 0) {
        free(ciphertext);
        return NULL;
    }

    // Derivar claves
    uint8_t derived_keys[64];
    pbkdf2_hmac_sha256(password, salt, 16, 10000, derived_keys, 64);
    uint8_t aes_key[32];
    uint8_t mac_key[32];
    memcpy(aes_key, derived_keys, 32);
    memcpy(mac_key, derived_keys + 32, 32);

    // Validar MAC en tiempo constante
    size_t mac_input_len = 16 + 16 + ct_bytes_len;
    uint8_t* mac_input = (uint8_t*)malloc(mac_input_len);
    if (!mac_input) {
        free(ciphertext);
        return NULL;
    }
    memcpy(mac_input, salt, 16);
    memcpy(mac_input + 16, iv, 16);
    if (ct_bytes_len > 0) memcpy(mac_input + 32, ciphertext, ct_bytes_len);

    uint8_t computed_mac[32];
    hmac_sha256(mac_key, 32, mac_input, mac_input_len, computed_mac);
    free(mac_input);

    uint8_t diff = 0;
    for (int i = 0; i < 32; i++) {
        diff |= (computed_mac[i] ^ expected_mac[i]);
    }
    if (diff != 0) {
        // Contraseña incorrecta o datos manipulados
        free(ciphertext);
        return NULL;
    }

    // Descifrado AES-256-CTR
    char* plaintext = (char*)malloc(ct_bytes_len + 1);
    if (!plaintext) {
        free(ciphertext);
        return NULL;
    }
    aes256_ctr_crypt(aes_key, iv, ciphertext, (uint8_t*)plaintext, ct_bytes_len);
    plaintext[ct_bytes_len] = '\0';
    free(ciphertext);
    return plaintext;
}
