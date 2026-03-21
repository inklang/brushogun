# Unicode Escape Sequences Implementation Plan

**Goal:** Add `\uXXXX` Unicode escape sequence support to string literals.

**Architecture:** One new `when` case in the existing `unescape()` function in Parser.kt. The Lexer already passes raw characters through; Parser's `unescape()` converts escape sequences. No new opcodes, IR nodes, or VM changes needed.

**Scope:** Single file, single function. No test file changes needed — existing tests cover `unescape()` via string literal parsing.

---

## Chunk 1: Add Unicode Escape to Parser

**Files:**
- Modify: `ink/src/main/kotlin/org/inklang/lang/Parser.kt:3-21`

- [ ] **Step 1: Read the current `unescape()` function**

Read `ink/src/main/kotlin/org/inklang/lang/Parser.kt` lines 1-30 to see the current `unescape()` implementation.

- [ ] **Step 2: Add the `'u'` case to `unescape()`**

In the `when (s[i + 1])` block in `unescape()`, add the new case:

```kotlin
'u'  -> {
    if (i + 6 <= s.length) {
        val hex = s.substring(i + 2, i + 6)
        if (hex.all { it in "0123456789abcdefABCDEF" }) {
            append(hex.toInt(16).toChar())
            i += 6
        } else {
            append(s[i]); i++
        }
    } else {
        append(s[i]); i++
    }
}
```

Insert it after the `'\\'` case (line ~12).

- [ ] **Step 3: Verify the change compiles**

```bash
./gradlew :ink:compileKotlin --console=plain 2>&1 | tail -20
```

Expected: BUILD SUCCESSFUL (or the existing snakeyaml error — unrelated to this change).

- [ ] **Step 4: Commit**

```bash
git add ink/src/main/kotlin/org/inklang/lang/Parser.kt
git commit -m "feat: add \\uXXXX unicode escape support in string literals

Co-Authored-By: Claude Opus 4.6 <noreply@anthropic.com>"
```

---

## Verification

After implementation, test manually:

```ink
print("hello\u0020world")     // should print: hello world
print("\u4e2d\u6587")           // should print: 中文
print("emoji:\u1F600")          // should print: emoji 😀
print("invalid:\uGGGG")         // should print: invalid:\uGGGG
```

All tests pass: `./gradlew :ink:test --console=plain 2>&1 | tail -20`
