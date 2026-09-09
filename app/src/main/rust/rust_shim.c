#define _GNU_SOURCE
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <stdbool.h>
#include <stdint.h>
#include <ctype.h>
#include <math.h>

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
