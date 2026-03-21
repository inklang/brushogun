# Deque Implementation Plan

> **For agentic workers:** REQUIRED: Use superpowers:subagent-driven-development (if subagents available) or superpowers:executing-plans to implement this plan. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add `Deque<T>` (double-ended queue) standard library type with O(1) push/pop at both ends.

**Architecture:** Follows existing collection patterns — `InternalDeque` wrapper in `Value.kt`, `DequeClass` descriptor, registered as global in `VM.kt`. Uses Kotlin's `ArrayDeque` internally. No new IR instructions; construction via existing `NewInstance`.

**Tech Stack:** Kotlin, Inklang bytecode VM, `ArrayDeque<Value>`

---

## Chunk 1: Add InternalDeque Wrapper

**Files:**
- Modify: `ink/src/main/kotlin/org/inklang/lang/Value.kt:74-77` (after InternalTuple)

- [ ] **Step 1: Add InternalDeque data class after InternalTuple**

In `Value.kt`, after the `InternalTuple` class (line 77), add:

```kotlin
    /** Wrapper for internal ArrayDeque storage (used by Deque class) */
    data class InternalDeque(val items: ArrayDeque<Value> = ArrayDeque()) : Value() {
        override fun toString() = items.joinToString(", ", "Deque(", ")")
    }
```

- [ ] **Step 2: Verify build compiles**

Run: `./gradlew :ink:compileKotlin 2>&1 | tail -5`
Expected: BUILD SUCCESSFUL

- [ ] **Step 3: Commit**

```bash
git add ink/src/main/kotlin/org/inklang/lang/Value.kt
git commit -m "feat(deque): add InternalDeque wrapper class

Co-Authored-By: Claude Opus 4.6 <noreply@anthropic.com>"
```

---

## Chunk 2: Add DequeIteratorClass

**Files:**
- Modify: `ink/src/main/kotlin/org/inklang/lang/Value.kt` (after SetIteratorClass, around line 386)

- [ ] **Step 1: Add DequeIteratorClass after ArrayIteratorClass**

In `Value.kt`, after the `ArrayIteratorClass` definition (around line 386), add:

```kotlin
    val DequeIteratorClass = ClassDescriptor(
        name = "DequeIterator",
        superClass = null,
        methods = mapOf(
            "hasNext" to Value.NativeFunction { args ->
                val self = args[0] as Value.Instance
                val items = (self.fields["__items"] as Value.InternalList).items
                val current = (self.fields["current"] as Value.Int).value
                if (current < items.size) Value.Boolean.TRUE else Value.Boolean.FALSE
            },
            "next" to Value.NativeFunction { args ->
                val self = args[0] as Value.Instance
                val items = (self.fields["__items"] as Value.InternalList).items
                val current = (self.fields["current"] as Value.Int).value
                self.fields["current"] = Value.Int(current + 1)
                items.getOrElse(current) { Value.Null }
            }
        )
    )
```

- [ ] **Step 2: Verify build compiles**

Run: `./gradlew :ink:compileKotlin 2>&1 | tail -5`
Expected: BUILD SUCCESSFUL

- [ ] **Step 3: Commit**

```bash
git add ink/src/main/kotlin/org/inklang/lang/Value.kt
git commit -m "feat(deque): add DequeIteratorClass

Co-Authored-By: Claude Opus 4.6 <noreply@anthropic.com>"
```

---

## Chunk 3: Add DequeClass

**Files:**
- Modify: `ink/src/main/kotlin/org/inklang/lang/Value.kt` (after TupleClass, around line 506)

- [ ] **Step 1: Add DequeClass after TupleClass**

In `Value.kt`, after the `newTuple` function (around line 506), add:

```kotlin
    val DequeClass = ClassDescriptor(
        name = "Deque",
        superClass = null,
        methods = mapOf(
            "init" to Value.NativeFunction { args ->
                val self = args[0] as Value.Instance
                self.fields["__deque"] = Value.InternalDeque()
                val deque = (self.fields["__deque"] as Value.InternalDeque).items
                for (i in 1 until args.size) {
                    deque.addLast(args[i])
                }
                Value.Null
            },
            "push_left" to Value.NativeFunction { args ->
                val self = args[0] as Value.Instance
                val deque = (self.fields["__deque"] as Value.InternalDeque).items
                deque.addFirst(args[1])
                Value.Null
            },
            "push_right" to Value.NativeFunction { args ->
                val self = args[0] as Value.Instance
                val deque = (self.fields["__deque"] as Value.InternalDeque).items
                deque.addLast(args[1])
                Value.Null
            },
            "pop_left" to Value.NativeFunction { args ->
                val self = args[0] as Value.Instance
                val deque = (self.fields["__deque"] as Value.InternalDeque).items
                if (deque.isEmpty()) Value.Null else deque.removeFirst()
            },
            "pop_right" to Value.NativeFunction { args ->
                val self = args[0] as Value.Instance
                val deque = (self.fields["__deque"] as Value.InternalDeque).items
                if (deque.isEmpty()) Value.Null else deque.removeLast()
            },
            "peek_left" to Value.NativeFunction { args ->
                val self = args[0] as Value.Instance
                val deque = (self.fields["__deque"] as Value.InternalDeque).items
                if (deque.isEmpty()) Value.Null else deque.first()
            },
            "peek_right" to Value.NativeFunction { args ->
                val self = args[0] as Value.Instance
                val deque = (self.fields["__deque"] as Value.InternalDeque).items
                if (deque.isEmpty()) Value.Null else deque.last()
            },
            "size" to Value.NativeFunction { args ->
                val self = args[0] as Value.Instance
                val deque = (self.fields["__deque"] as Value.InternalDeque).items
                Value.Int(deque.size)
            },
            "is_empty" to Value.NativeFunction { args ->
                val self = args[0] as Value.Instance
                val deque = (self.fields["__deque"] as Value.InternalDeque).items
                if (deque.isEmpty()) Value.Boolean.TRUE else Value.Boolean.FALSE
            },
            "has" to Value.NativeFunction { args ->
                val self = args[0] as Value.Instance
                val deque = (self.fields["__deque"] as Value.InternalDeque).items
                if (deque.contains(args[1])) Value.Boolean.TRUE else Value.Boolean.FALSE
            },
            "clear" to Value.NativeFunction { args ->
                val self = args[0] as Value.Instance
                val deque = (self.fields["__deque"] as Value.InternalDeque).items
                deque.clear()
                Value.Null
            },
            "iter" to Value.NativeFunction { args ->
                val self = args[0] as Value.Instance
                val internalDeque = self.fields["__deque"] as Value.InternalDeque
                Value.Instance(
                    DequeIteratorClass,
                    mutableMapOf(
                        "__items" to Value.InternalList(internalDeque.items.toMutableList()),
                        "current" to Value.Int(0)
                    )
                )
            }
        )
    )

    fun newDeque(items: ArrayDeque<Value> = ArrayDeque()): Value.Instance =
        Value.Instance(
            DequeClass,
            mutableMapOf("__deque" to Value.InternalDeque(items))
        )
```

- [ ] **Step 2: Verify build compiles**

Run: `./gradlew :ink:compileKotlin 2>&1 | tail -5`
Expected: BUILD SUCCESSFUL

- [ ] **Step 3: Commit**

```bash
git add ink/src/main/kotlin/org/inklang/lang/Value.kt
git commit -m "feat(deque): add DequeClass with full API

push_left, push_right, pop_left, pop_right, peek_left,
peek_right, size, is_empty, has, clear, iter.

Co-Authored-By: Claude Opus 4.6 <noreply@anthropic.com>"
```

---

## Chunk 4: Register Deque in VM Globals

**Files:**
- Modify: `ink/src/main/kotlin/org/inklang/ast/VM.kt:28-32`

- [ ] **Step 1: Add Deque to VM globals**

In `VM.kt`, add `"Deque" to Value.Class(Builtins.DequeClass)` after the Tuple line:

```kotlin
        "Array" to Value.Class(Builtins.ArrayClass),
        "Map" to Value.Class(Builtins.MapClass),
        "Set" to Value.Class(Builtins.SetClass),
        "Tuple" to Value.Class(Builtins.TupleClass),
        "Deque" to Value.Class(Builtins.DequeClass),  // NEW
```

- [ ] **Step 2: Verify build compiles**

Run: `./gradlew :ink:compileKotlin 2>&1 | tail -5`
Expected: BUILD SUCCESSFUL

- [ ] **Step 3: Commit**

```bash
git add ink/src/main/kotlin/org/inklang/ast/VM.kt
git commit -m "feat(deque): register Deque in VM globals

Co-Authored-By: Claude Opus 4.6 <noreply@anthropic.com>"
```

---

## Chunk 5: Write Tests

**Files:**
- Create: `ink/src/test/kotlin/org/inklang/DequeTest.kt`

- [ ] **Step 1: Create DequeTest.kt with full test coverage**

```kotlin
package org.inklang

import org.inklang.ast.*
import org.inklang.lang.*
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.assertFalse

private fun compileAndRun(source: String): List<String> {
    val output = mutableListOf<String>()
    val tokens = tokenize(source)
    val stmts = Parser(tokens).parse()
    val folder = ConstantFolder()
    val folded = stmts.map { folder.foldStmt(it) }
    val result = AstLowerer().lower(folded)

    val (ssaDeconstructed, ssaOptConstants) = IrCompiler.optimizedSsaRoundTrip(result.instrs, result.constants)
    val ssaResult = AstLowerer.LoweredResult(ssaDeconstructed, ssaOptConstants)

    val ranges = LivenessAnalyzer().analyze(ssaResult.instrs)
    val allocResult = RegisterAllocator().allocate(ranges)
    val resolved = SpillInserter().insert(ssaResult.instrs, allocResult, ranges)
    val chunk = IrCompiler().compile(AstLowerer.LoweredResult(resolved, ssaResult.constants))
    chunk.spillSlotCount = allocResult.spillSlotCount

    val vm = VM()
    vm.globals["print"] = Value.NativeFunction { args ->
        output.add(args.joinToString(" ") { valueToString(it) })
        Value.Null
    }
    vm.execute(chunk)
    return output
}

class DequeTest {

    @Test
    fun `push_right and pop_right`() {
        val output = compileAndRun(
            """
            let d = Deque()
            d.push_right(1)
            d.push_right(2)
            d.push_right(3)
            print(d.pop_right())
            print(d.pop_right())
            print(d.pop_right())
            """.trimIndent()
        )
        assertEquals(listOf("3", "2", "1"), output)
    }

    @Test
    fun `push_left and pop_left`() {
        val output = compileAndRun(
            """
            let d = Deque()
            d.push_left(1)
            d.push_left(2)
            d.push_left(3)
            print(d.pop_left())
            print(d.pop_left())
            print(d.pop_left())
            """.trimIndent()
        )
        assertEquals(listOf("3", "2", "1"), output)
    }

    @Test
    fun `push_left and pop_right interleaved`() {
        val output = compileAndRun(
            """
            let d = Deque()
            d.push_left(1)
            d.push_right(2)
            d.push_left(3)
            d.push_right(4)
            print(d.pop_left())
            print(d.pop_right())
            print(d.pop_left())
            print(d.pop_right())
            """.trimIndent()
        )
        assertEquals(listOf("3", "4", "1", "2"), output)
    }

    @Test
    fun `peek_left and peek_right`() {
        val output = compileAndRun(
            """
            let d = Deque()
            d.push_right(10)
            d.push_right(20)
            d.push_left(5)
            print(d.peek_left())
            print(d.peek_right())
            print(d.size())
            """.trimIndent()
        )
        assertEquals(listOf("5", "20", "3"), output)
    }

    @Test
    fun `is_empty on empty and non-empty`() {
        val output = compileAndRun(
            """
            let d = Deque()
            print(d.is_empty())
            d.push_right(1)
            print(d.is_empty())
            d.pop_right()
            print(d.is_empty())
            """.trimIndent()
        )
        assertEquals(listOf("true", "false", "true"), output)
    }

    @Test
    fun `has returns true and false`() {
        val output = compileAndRun(
            """
            let d = Deque()
            d.push_right("hello")
            d.push_right("world")
            print(d.has("hello"))
            print(d.has("nope"))
            """.trimIndent()
        )
        assertEquals(listOf("true", "false"), output)
    }

    @Test
    fun `clear empties the deque`() {
        val output = compileAndRun(
            """
            let d = Deque()
            d.push_right(1)
            d.push_right(2)
            d.clear()
            print(d.is_empty())
            print(d.size())
            """.trimIndent()
        )
        assertEquals(listOf("true", "0"), output)
    }

    @Test
    fun `pop on empty returns null`() {
        val output = compileAndRun(
            """
            let d = Deque()
            print(d.pop_left())
            print(d.pop_right())
            print(d.peek_left())
            print(d.peek_right())
            """.trimIndent()
        )
        assertEquals(listOf("null", "null", "null", "null"), output)
    }

    @Test
    fun `iter works correctly`() {
        val output = compileAndRun(
            """
            let d = Deque()
            d.push_right(1)
            d.push_right(2)
            d.push_right(3)
            for x in d {
                print(x)
            }
            """.trimIndent()
        )
        assertEquals(listOf("1", "2", "3"), output)
    }

    @Test
    fun `size reports correct count`() {
        val output = compileAndRun(
            """
            let d = Deque()
            print(d.size())
            d.push_right(42)
            d.push_left(99)
            print(d.size())
            d.pop_right()
            print(d.size())
            """.trimIndent()
        )
        assertEquals(listOf("0", "2", "1"), output)
    }

    @Test
    fun `Deque with initial items`() {
        val output = compileAndRun(
            """
            let d = Deque(10, 20, 30)
            print(d.pop_left())
            print(d.pop_right())
            print(d.size())
            """.trimIndent()
        )
        assertEquals(listOf("10", "30", "1"), output)
    }
}
```

- [ ] **Step 2: Run tests to verify they pass**

Run: `./gradlew :ink:test --tests "org.inklang.DequeTest" 2>&1 | tail -20`
Expected: All tests pass

- [ ] **Step 3: Commit**

```bash
git add ink/src/test/kotlin/org/inklang/DequeTest.kt
git commit -m "test(deque): add DequeTest with full coverage

push/pop/peek at both ends, is_empty, has, clear, iter, size,
initial items, empty deque edge cases.

Co-Authored-By: Claude Opus 4.6 <noreply@anthropic.com>"
```

---

## Summary

| Chunk | Files | What |
|-------|-------|------|
| 1 | Value.kt | `InternalDeque` wrapper |
| 2 | Value.kt | `DequeIteratorClass` |
| 3 | Value.kt | `DequeClass` + `newDeque()` |
| 4 | VM.kt | Register `Deque` global |
| 5 | DequeTest.kt | Full test suite |
