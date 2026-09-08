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

