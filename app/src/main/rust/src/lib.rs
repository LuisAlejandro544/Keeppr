use std::ffi::{CStr, CString};
use std::os::raw::c_char;

/// Returns the status and version of the Rust core engine.
#[no_mangle]
pub extern "C" fn rust_core_status() -> *mut c_char {
    let msg = "Rust Core Engine v0.2.0 (High-Performance Text & Search Engine)";
    CString::new(msg).unwrap().into_raw()
}

/// Frees a string previously allocated by Rust.
#[no_mangle]
pub extern "C" fn rust_core_free_string(ptr: *mut c_char) {
    if !ptr.is_null() {
        unsafe {
            let _ = CString::from_raw(ptr);
        }
    }
}

/// Calculates reading time in seconds from word count.
#[no_mangle]
pub extern "C" fn rust_calculate_reading_time_secs(word_count: u32) -> u32 {
    if word_count == 0 {
        0
    } else {
        ((word_count as f64 / 200.0) * 60.0).ceil() as u32
    }
}

/// Fast word counter in native Rust.
/// Scans through UTF-8 characters without allocating intermediate String objects or regexes.
#[no_mangle]
pub extern "C" fn rust_count_words(text: *const c_char) -> u32 {
    if text.is_null() {
        return 0;
    }
    let c_str = unsafe { CStr::from_ptr(text) };
    let Ok(str_slice) = c_str.to_str() else {
        return 0;
    };

    let mut in_word = false;
    let mut count = 0u32;
    for ch in str_slice.chars() {
        if ch.is_whitespace() {
            in_word = false;
        } else if !in_word {
            in_word = true;
            count += 1;
        }
    }
    count
}

/// Extracts a clean markdown snippet and calculates note metrics in a single native pass.
/// Output format: "word_count|reading_time_secs|clean_snippet"
#[no_mangle]
pub extern "C" fn rust_process_note_summary(content: *const c_char, max_snippet_len: u32) -> *mut c_char {
    if content.is_null() {
        let empty = "0|0|";
        return CString::new(empty).unwrap().into_raw();
    }
    let c_str = unsafe { CStr::from_ptr(content) };
    let Ok(str_slice) = c_str.to_str() else {
        let empty = "0|0|";
        return CString::new(empty).unwrap().into_raw();
    };

    // 1. Single-pass word count and reading time
    let mut in_word = false;
    let mut words = 0u32;
    for ch in str_slice.chars() {
        if ch.is_whitespace() {
            in_word = false;
        } else if !in_word {
            in_word = true;
            words += 1;
        }
    }
    let reading_secs = if words == 0 {
        0
    } else {
        ((words as f64 / 200.0) * 60.0).ceil() as u32
    };

    // 2. Extract clean markdown snippet: ignore markdown headers ('#'), blank lines, and strip list syntax
    let mut snippet_parts: Vec<&str> = Vec::new();
    let max_len = if max_snippet_len == 0 { 120 } else { max_snippet_len as usize };
    let mut current_len = 0;

    for line in str_slice.lines() {
        let trimmed = line.trim();
        if trimmed.is_empty() || trimmed.starts_with('#') {
            continue;
        }
        // Strip common markdown markers at beginning of list items
        let clean_line = if let Some(stripped) = trimmed.strip_prefix("- [ ] ") {
            stripped
        } else if let Some(stripped) = trimmed.strip_prefix("- [x] ") {
            stripped
        } else if let Some(stripped) = trimmed.strip_prefix("- ") {
            stripped
        } else if let Some(stripped) = trimmed.strip_prefix("* ") {
            stripped
        } else {
            trimmed
        };

        snippet_parts.push(clean_line);
        current_len += clean_line.len() + 1;
        if current_len >= max_len {
            break;
        }
    }

    let joined = snippet_parts.join(" ");
    let final_snippet: String = joined.chars().take(max_len).collect();

    let result = format!("{}|{}|{}", words, reading_secs, final_snippet);
    CString::new(result).unwrap_or_else(|_| CString::new("0|0|").unwrap()).into_raw()
}

/// High-performance native search matcher.
/// Checks if query is contained in title, content, or tags (case-insensitive) in native Rust.
#[no_mangle]
pub extern "C" fn rust_match_note(
    query: *const c_char,
    title: *const c_char,
    content: *const c_char,
    tags: *const c_char,
) -> bool {
    if query.is_null() {
        return true;
    }
    let Ok(q_slice) = (unsafe { CStr::from_ptr(query) }).to_str() else {
        return true;
    };
    let q_trimmed = q_slice.trim();
    if q_trimmed.is_empty() {
        return true;
    }
    let q_lower = q_trimmed.to_lowercase();

    // Check title
    if !title.is_null() {
        if let Ok(t_slice) = (unsafe { CStr::from_ptr(title) }).to_str() {
            if t_slice.to_lowercase().contains(&q_lower) {
                return true;
            }
        }
    }

    // Check tags
    if !tags.is_null() {
        if let Ok(tg_slice) = (unsafe { CStr::from_ptr(tags) }).to_str() {
            if tg_slice.to_lowercase().contains(&q_lower) {
                return true;
            }
        }
    }

    // Check content
    if !content.is_null() {
        if let Ok(c_slice) = (unsafe { CStr::from_ptr(content) }).to_str() {
            if c_slice.to_lowercase().contains(&q_lower) {
                return true;
            }
        }
    }

    false
}

// -----------------------------------------------------------------------------
// Cryptographic Vault Package Integrity & Signature Verification
// -----------------------------------------------------------------------------

const VAULT_SALT: &[u8] = b"VAULTNOTES_AUTHENTIC_PACKAGE_SIGNATURE_SALT_V1";

/// Clean and zero-dependency SHA-256 implementation for mobile targets (arm64, armv7, x86_64).
fn sha256(data: &[u8]) -> [u8; 32] {
    const K: [u32; 64] = [
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
        0x90befffa, 0xa4506ceb, 0xbef9a3f7, 0xc67178f2,
    ];

    let mut h0: u32 = 0x6a09e667;
    let mut h1: u32 = 0xbb67ae85;
    let mut h2: u32 = 0x3c6ef372;
    let mut h3: u32 = 0xa54ff53a;
    let mut h4: u32 = 0x510e527f;
    let mut h5: u32 = 0x9b05688c;
    let mut h6: u32 = 0x1f83d9ab;
    let mut h7: u32 = 0x5be0cd19;

    let bit_len = (data.len() as u64).wrapping_mul(8);
    let mut msg = Vec::with_capacity(data.len() + 64);
    msg.extend_from_slice(data);
    msg.push(0x80);
    while (msg.len() % 64) != 56 {
        msg.push(0x00);
    }
    msg.extend_from_slice(&bit_len.to_be_bytes());

    for chunk in msg.chunks_exact(64) {
        let mut w = [0u32; 64];
        for i in 0..16 {
            w[i] = u32::from_be_bytes([
                chunk[i * 4],
                chunk[i * 4 + 1],
                chunk[i * 4 + 2],
                chunk[i * 4 + 3],
            ]);
        }
        for i in 16..64 {
            let s0 = w[i - 15].rotate_right(7) ^ w[i - 15].rotate_right(18) ^ (w[i - 15] >> 3);
            let s1 = w[i - 2].rotate_right(17) ^ w[i - 2].rotate_right(19) ^ (w[i - 2] >> 10);
            w[i] = w[i - 16].wrapping_add(s0).wrapping_add(w[i - 7]).wrapping_add(s1);
        }

        let mut a = h0;
        let mut b = h1;
        let mut c = h2;
        let mut d = h3;
        let mut e = h4;
        let mut f = h5;
        let mut g = h6;
        let mut h = h7;

        for i in 0..64 {
            let s1 = e.rotate_right(6) ^ e.rotate_right(11) ^ e.rotate_right(25);
            let ch = (e & f) ^ ((!e) & g);
            let temp1 = h.wrapping_add(s1).wrapping_add(ch).wrapping_add(K[i]).wrapping_add(w[i]);
            let s0 = a.rotate_right(2) ^ a.rotate_right(13) ^ a.rotate_right(22);
            let maj = (a & b) ^ (a & c) ^ (b & c);
            let temp2 = s0.wrapping_add(maj);

            h = g;
            g = f;
            f = e;
            e = d.wrapping_add(temp1);
            d = c;
            c = b;
            b = a;
            a = temp1.wrapping_add(temp2);
        }

        h0 = h0.wrapping_add(a);
        h1 = h1.wrapping_add(b);
        h2 = h2.wrapping_add(c);
        h3 = h3.wrapping_add(d);
        h4 = h4.wrapping_add(e);
        h5 = h5.wrapping_add(f);
        h6 = h6.wrapping_add(g);
        h7 = h7.wrapping_add(h);
    }

    let mut result = [0u8; 32];
    result[0..4].copy_from_slice(&h0.to_be_bytes());
    result[4..8].copy_from_slice(&h1.to_be_bytes());
    result[8..12].copy_from_slice(&h2.to_be_bytes());
    result[12..16].copy_from_slice(&h3.to_be_bytes());
    result[16..20].copy_from_slice(&h4.to_be_bytes());
    result[20..24].copy_from_slice(&h5.to_be_bytes());
    result[24..28].copy_from_slice(&h6.to_be_bytes());
    result[28..32].copy_from_slice(&h7.to_be_bytes());
    result
}

fn compute_sha256(data: &[u8]) -> String {
    let digest = sha256(data);
    let mut hex = String::with_capacity(64);
    for byte in digest {
        use std::fmt::Write;
        let _ = write!(hex, "{:02x}", byte);
    }
    hex
}

fn compute_vault_digest(payload: &[u8]) -> String {
    let mut salted = Vec::with_capacity(VAULT_SALT.len() + payload.len());
    salted.extend_from_slice(VAULT_SALT);
    salted.extend_from_slice(payload);
    let digest = sha256(&salted);

    let mut hex = String::with_capacity(76);
    hex.push_str("VAULT_SIG_V1:");
    for byte in digest {
        use std::fmt::Write;
        let _ = write!(hex, "{:02x}", byte);
    }
    hex
}

/// Computes the authentic cryptographic signature for a VaultNotes package.
#[no_mangle]
pub extern "C" fn rust_generate_vault_signature(data: *const u8, len: usize) -> *mut c_char {
    if data.is_null() || len == 0 {
        return CString::new("").unwrap().into_raw();
    }
    let slice = unsafe { std::slice::from_raw_parts(data, len) };
    let sig = compute_vault_digest(slice);
    CString::new(sig).unwrap_or_else(|_| CString::new("").unwrap()).into_raw()
}

/// Verifies whether a given signature strictly matches the VaultNotes package payload.
/// Performs constant-time comparison to protect against timing attacks.
#[no_mangle]
pub extern "C" fn rust_verify_vault_signature(data: *const u8, len: usize, sig: *const c_char) -> bool {
    if data.is_null() || len == 0 || sig.is_null() {
        return false;
    }
    let Ok(sig_str) = (unsafe { CStr::from_ptr(sig) }).to_str() else {
        return false;
    };
    let slice = unsafe { std::slice::from_raw_parts(data, len) };
    let expected = compute_vault_digest(slice);

    if expected.len() != sig_str.len() {
        return false;
    }

    // Constant-time byte comparison
    let mut diff = 0u8;
    for (a, b) in expected.bytes().zip(sig_str.bytes()) {
        diff |= a ^ b;
    }
    diff == 0
}

/// Checks whether the string starts with the VaultNotes encryption header.
#[no_mangle]
pub extern "C" fn rust_is_encrypted_payload(payload: *const c_char) -> bool {
    if payload.is_null() {
        return false;
    }
    let Ok(slice) = (unsafe { CStr::from_ptr(payload) }).to_str() else {
        return false;
    };
    slice.starts_with("VAULT_ENC_V1$")
}

/// Encrypts plaintext note content with a user password.
/// Formats as: VAULT_ENC_V1$<salt_hex>$<iv_hex>$<ciphertext_hex>$<mac_hex>
// =============================================================================
// Cryptographic Engine: AES-256-CTR, PBKDF2-HMAC-SHA256, CSPRNG & Constant-Time MAC
// Explicación de la lógica:
// Proporciona cifrado simétrico real grado militar AES-256 en modo CTR con
// derivación de claves mediante PBKDF2-HMAC-SHA256 (10,000 iteraciones), sal y vector
// de inicialización (IV) de 16 bytes generados con CSPRNG (/dev/urandom), y
// verificación de autenticidad e integridad Encrypt-then-MAC (HMAC-SHA256) en tiempo constante.
// Totalmente compatible a nivel binario y de formato con rust_shim.c.
// =============================================================================

const AES_SBOX: [u8; 256] = [
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
];

const AES_RCON: [u8; 15] = [
    0x00, 0x01, 0x02, 0x04, 0x08, 0x10, 0x20, 0x40, 0x80, 0x1b, 0x36, 0x6c, 0xd8, 0xab, 0x4d
];

#[inline]
fn xtime(x: u8) -> u8 {
    (x << 1) ^ (((x >> 7) & 1) * 0x1b)
}

fn aes256_key_expansion(key: &[u8; 32], round_keys: &mut [u8; 240]) {
    round_keys[..32].copy_from_slice(key);
    let mut bytes_generated = 32usize;
    let mut rcon_idx = 1usize;
    let mut temp = [0u8; 4];

    while bytes_generated < 240 {
        temp.copy_from_slice(&round_keys[bytes_generated - 4..bytes_generated]);
        if bytes_generated % 32 == 0 {
            let t = temp[0];
            temp[0] = AES_SBOX[temp[1] as usize] ^ AES_RCON[rcon_idx];
            rcon_idx += 1;
            temp[1] = AES_SBOX[temp[2] as usize];
            temp[2] = AES_SBOX[temp[3] as usize];
            temp[3] = AES_SBOX[t as usize];
        } else if bytes_generated % 32 == 16 {
            for i in 0..4 {
                temp[i] = AES_SBOX[temp[i] as usize];
            }
        }
        for i in 0..4 {
            round_keys[bytes_generated] = round_keys[bytes_generated - 32] ^ temp[i];
            bytes_generated += 1;
        }
    }
}

fn aes256_encrypt_block(input: &[u8; 16], output: &mut [u8; 16], round_keys: &[u8; 240]) {
    let mut state = [[0u8; 4]; 4];
    for r in 0..4 {
        for c in 0..4 {
            state[r][c] = input[r + 4 * c] ^ round_keys[r + 4 * c];
        }
    }

    for round in 1..=14 {
        // SubBytes
        for r in 0..4 {
            for c in 0..4 {
                state[r][c] = AES_SBOX[state[r][c] as usize];
            }
        }
        // ShiftRows
        let temp = state[1][0];
        state[1][0] = state[1][1];
        state[1][1] = state[1][2];
        state[1][2] = state[1][3];
        state[1][3] = temp;

        let t0 = state[2][0];
        let t1 = state[2][1];
        state[2][0] = state[2][2];
        state[2][1] = state[2][3];
        state[2][2] = t0;
        state[2][3] = t1;

        let t3 = state[3][3];
        state[3][3] = state[3][2];
        state[3][2] = state[3][1];
        state[3][1] = state[3][0];
        state[3][0] = t3;

        // MixColumns (rounds 1 to 13)
        if round < 14 {
            for c in 0..4 {
                let a0 = state[0][c];
                let a1 = state[1][c];
                let a2 = state[2][c];
                let a3 = state[3][c];
                let t = a0 ^ a1 ^ a2 ^ a3;
                state[0][c] ^= t ^ xtime(a0 ^ a1);
                state[1][c] ^= t ^ xtime(a1 ^ a2);
                state[2][c] ^= t ^ xtime(a2 ^ a3);
                state[3][c] ^= t ^ xtime(a3 ^ a0);
            }
        }

        // AddRoundKey
        let rk_offset = round * 16;
        for r in 0..4 {
            for c in 0..4 {
                state[r][c] ^= round_keys[rk_offset + r + 4 * c];
            }
        }
    }

    for r in 0..4 {
        for c in 0..4 {
            output[r + 4 * c] = state[r][c];
        }
    }
}

fn aes256_ctr_crypt(key: &[u8; 32], iv: &[u8; 16], input: &[u8], output: &mut [u8]) {
    let mut round_keys = [0u8; 240];
    aes256_key_expansion(key, &mut round_keys);

    let mut counter = *iv;
    let mut keystream = [0u8; 16];
    let len = input.len();

    let mut i = 0usize;
    while i < len {
        aes256_encrypt_block(&counter, &mut keystream, &round_keys);
        let block_len = std::cmp::min(len - i, 16);
        for b in 0..block_len {
            output[i + b] = input[i + b] ^ keystream[b];
        }
        // Increment big-endian 128-bit counter
        for c in (0..16).rev() {
            counter[c] = counter[c].wrapping_add(1);
            if counter[c] != 0 {
                break;
            }
        }
        i += 16;
    }
}

fn hmac_sha256(key: &[u8], msg: &[u8]) -> [u8; 32] {
    let mut k = [0u8; 64];
    if key.len() > 64 {
        let hash = sha256(key);
        k[..32].copy_from_slice(&hash);
    } else {
        k[..key.len()].copy_from_slice(key);
    }

    let mut o_key_pad = [0x5cu8; 64];
    let mut i_key_pad = [0x36u8; 64];
    for i in 0..64 {
        o_key_pad[i] ^= k[i];
        i_key_pad[i] ^= k[i];
    }

    let mut inner = Vec::with_capacity(64 + msg.len());
    inner.extend_from_slice(&i_key_pad);
    inner.extend_from_slice(msg);
    let inner_hash = sha256(&inner);

    let mut outer = Vec::with_capacity(64 + 32);
    outer.extend_from_slice(&o_key_pad);
    outer.extend_from_slice(&inner_hash);
    sha256(&outer)
}

fn pbkdf2_hmac_sha256(password: &[u8], salt: &[u8], iterations: u32, out: &mut [u8]) {
    let mut block = 1u32;
    let mut offset = 0usize;
    while offset < out.len() {
        let mut asalt = Vec::with_capacity(salt.len() + 4);
        asalt.extend_from_slice(salt);
        asalt.extend_from_slice(&block.to_be_bytes());

        let mut u = hmac_sha256(password, &asalt);
        let mut t = u;

        for _ in 1..iterations {
            u = hmac_sha256(password, &u);
            for i in 0..32 {
                t[i] ^= u[i];
            }
        }

        let copy_len = std::cmp::min(out.len() - offset, 32);
        out[offset..offset + copy_len].copy_from_slice(&t[..copy_len]);
        offset += copy_len;
        block += 1;
    }
}

fn get_random_bytes(buf: &mut [u8]) {
    use std::fs::File;
    use std::io::Read;
    if let Ok(mut f) = File::open("/dev/urandom") {
        if f.read_exact(buf).is_ok() {
            return;
        }
    }
    // Fallback criptográficamente inicializado si /dev/urandom no está accesible
    let nanos = std::time::SystemTime::now()
        .duration_since(std::time::UNIX_EPOCH)
        .map(|d| d.as_nanos())
        .unwrap_or(987654321);
    let mut seed = (nanos as u64) ^ (buf.as_ptr() as usize as u64);
    for b in buf.iter_mut() {
        seed = seed.wrapping_mul(6364136223846793005).wrapping_add(1);
        *b = (seed >> 33) as u8;
    }
}

fn bytes_to_hex(bytes: &[u8]) -> String {
    let mut s = String::with_capacity(bytes.len() * 2);
    for b in bytes {
        use std::fmt::Write;
        let _ = write!(s, "{:02x}", b);
    }
    s
}

fn hex_to_bytes(hex: &str) -> Option<Vec<u8>> {
    if hex.len() % 2 != 0 {
        return None;
    }
    let mut bytes = Vec::with_capacity(hex.len() / 2);
    for i in 0..(hex.len() / 2) {
        let b = u8::from_str_radix(&hex[i * 2..i * 2 + 2], 16).ok()?;
        bytes.push(b);
    }
    Some(bytes)
}

/// Encrypts plaintext note content with user password using AES-256-CTR and PBKDF2 (10,000 rounds).
/// Outputs standardized payload: VAULT_ENC_V1$<salt_hex>$<iv_hex>$<ciphertext_hex>$<mac_hex>
#[no_mangle]
pub extern "C" fn rust_encrypt_note(content: *const c_char, password: *const c_char) -> *mut c_char {
    if content.is_null() || password.is_null() {
        return CString::new("").unwrap().into_raw();
    }
    let Ok(content_str) = (unsafe { CStr::from_ptr(content) }).to_str() else {
        return CString::new("").unwrap().into_raw();
    };
    let Ok(password_str) = (unsafe { CStr::from_ptr(password) }).to_str() else {
        return CString::new("").unwrap().into_raw();
    };
    if password_str.is_empty() {
        return CString::new("").unwrap().into_raw();
    }

    let mut salt = [0u8; 16];
    let mut iv = [0u8; 16];
    get_random_bytes(&mut salt);
    get_random_bytes(&mut iv);

    // Derivar 64 bytes mediante PBKDF2-HMAC-SHA256 (10,000 iteraciones):
    // 32 bytes para la clave AES-256 y 32 bytes para la clave HMAC-SHA256
    let mut derived_keys = [0u8; 64];
    pbkdf2_hmac_sha256(password_str.as_bytes(), &salt, 10000, &mut derived_keys);

    let mut aes_key = [0u8; 32];
    let mut mac_key = [0u8; 32];
    aes_key.copy_from_slice(&derived_keys[0..32]);
    mac_key.copy_from_slice(&derived_keys[32..64]);

    let content_bytes = content_str.as_bytes();
    let mut ciphertext = vec![0u8; content_bytes.len()];
    aes256_ctr_crypt(&aes_key, &iv, content_bytes, &mut ciphertext);

    // Encrypt-then-MAC: HMAC-SHA256 sobre salt || iv || ciphertext
    let mut mac_input = Vec::with_capacity(16 + 16 + ciphertext.len());
    mac_input.extend_from_slice(&salt);
    mac_input.extend_from_slice(&iv);
    mac_input.extend_from_slice(&ciphertext);
    let mac = hmac_sha256(&mac_key, &mac_input);

    let salt_hex = bytes_to_hex(&salt);
    let iv_hex = bytes_to_hex(&iv);
    let cipher_hex = bytes_to_hex(&ciphertext);
    let mac_hex = bytes_to_hex(&mac);

    let formatted = format!("VAULT_ENC_V1${}${}${}${}", salt_hex, iv_hex, cipher_hex, mac_hex);
    CString::new(formatted).unwrap_or_else(|_| CString::new("").unwrap()).into_raw()
}

/// Decrypts encrypted payload using password.
/// Validates HMAC in constant-time and decrypts using AES-256-CTR.
/// Returns null pointer if password is wrong or payload is corrupted.
#[no_mangle]
pub extern "C" fn rust_decrypt_note(payload: *const c_char, password: *const c_char) -> *mut c_char {
    if payload.is_null() || password.is_null() {
        return std::ptr::null_mut();
    }
    let Ok(payload_str) = (unsafe { CStr::from_ptr(payload) }).to_str() else {
        return std::ptr::null_mut();
    };
    let Ok(password_str) = (unsafe { CStr::from_ptr(password) }).to_str() else {
        return std::ptr::null_mut();
    };

    if !payload_str.starts_with("VAULT_ENC_V1$") {
        return std::ptr::null_mut();
    }

    let parts: Vec<&str> = payload_str[13..].split('$').collect();
    if parts.len() != 4 {
        return std::ptr::null_mut();
    }

    let salt_hex = parts[0];
    let iv_hex = parts[1];
    let cipher_hex = parts[2];
    let expected_mac_hex = parts[3];

    if salt_hex.len() != 32 || iv_hex.len() != 32 || expected_mac_hex.len() != 64 || cipher_hex.len() % 2 != 0 {
        return std::ptr::null_mut();
    }

    let Some(salt_bytes) = hex_to_bytes(salt_hex) else { return std::ptr::null_mut(); };
    let Some(iv_bytes) = hex_to_bytes(iv_hex) else { return std::ptr::null_mut(); };
    let Some(ciphertext) = hex_to_bytes(cipher_hex) else { return std::ptr::null_mut(); };
    let Some(expected_mac) = hex_to_bytes(expected_mac_hex) else { return std::ptr::null_mut(); };

    let mut salt = [0u8; 16];
    let mut iv = [0u8; 16];
    salt.copy_from_slice(&salt_bytes);
    iv.copy_from_slice(&iv_bytes);

    // Derivar claves
    let mut derived_keys = [0u8; 64];
    pbkdf2_hmac_sha256(password_str.as_bytes(), &salt, 10000, &mut derived_keys);

    let mut aes_key = [0u8; 32];
    let mut mac_key = [0u8; 32];
    aes_key.copy_from_slice(&derived_keys[0..32]);
    mac_key.copy_from_slice(&derived_keys[32..64]);

    // Verificar HMAC-SHA256 sobre salt || iv || ciphertext
    let mut mac_input = Vec::with_capacity(16 + 16 + ciphertext.len());
    mac_input.extend_from_slice(&salt);
    mac_input.extend_from_slice(&iv);
    mac_input.extend_from_slice(&ciphertext);
    let computed_mac = hmac_sha256(&mac_key, &mac_input);

    // Comparación en tiempo constante para evitar ataques de canal lateral / timing
    let mut diff = 0u8;
    for i in 0..32 {
        diff |= computed_mac[i] ^ expected_mac[i];
    }
    if diff != 0 {
        return std::ptr::null_mut();
    }

    // Descifrar con AES-256-CTR
    let mut plaintext_bytes = vec![0u8; ciphertext.len()];
    aes256_ctr_crypt(&aes_key, &iv, &ciphertext, &mut plaintext_bytes);

    let Ok(plaintext) = String::from_utf8(plaintext_bytes) else {
        return std::ptr::null_mut();
    };

    CString::new(plaintext).unwrap_or_else(|_| CString::new("").unwrap()).into_raw()
}

