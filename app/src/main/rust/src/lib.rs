use std::ffi::CString;
use std::os::raw::c_char;

/// Returns the status and version of the Rust core engine.
#[no_mangle]
pub extern "C" fn rust_core_status() -> *mut c_char {
    let msg = "Rust Core Engine v0.1.0 (Memory-Safe & High-Performance)";
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

/// A sample Rust function to process and calculate markdown statistics.
#[no_mangle]
pub extern "C" fn rust_calculate_reading_time_secs(word_count: u32) -> u32 {
    // Average reading speed: 200 words per minute -> 3.33 words per second
    if word_count == 0 {
        0
    } else {
        ((word_count as f64 / 200.0) * 60.0).ceil() as u32
    }
}
