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

