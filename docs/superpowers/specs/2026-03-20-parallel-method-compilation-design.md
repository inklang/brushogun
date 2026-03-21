# Design: Parallel Class Method Compilation

## Context

When compiling a class with many methods, each method's compilation pipeline (SSA round-trip → liveness analysis → register allocation → spill insertion → bytecode emission) runs sequentially, even though methods are completely independent. This is low-hanging fruit for parallelization since:

1. Methods within a class have no compile-time interdependencies
2. Each method compilation is pure — no shared mutable state
3. Class method compilation is a significant fraction of total compilation time for class-heavy code

## Approach

Use `ForkJoinPool` to compile independent methods in parallel inside each class's `LoadClass` handler.

## Changes

### `IrCompiler.kt`

**New data class** to hold compilation results:
```kotlin
private data class CompiledMethod(
    val chunk: Chunk,
    val spillSlotCount: Int
)
```

**New helper method** — pure function, no shared state:
```kotlin
private fun compileMethod(methodInfo: MethodInfo): CompiledMethod {
    val ssa = SsaBuilder.build(methodInfo.instrs, methodInfo.constants, methodInfo.arity)
    val deconstructed = SsaDeconstructor.deconstruct(ssa)
    val ranges = LivenessAnalyzer().analyze(deconstructed)
    val alloc = RegisterAllocator().allocate(ranges, methodInfo.arity)
    val resolved = SpillInserter().insert(deconstructed, alloc, ranges)
    val result = AstLowerer.LoweredResult(resolved, methodInfo.constants)
    val chunk = IrCompiler().compile(result)
    return CompiledMethod(chunk, alloc.spillSlotCount)
}
```

**Refactor `LoadClass` handling** to use `ForkJoinPool`:

1. Pre-allocate `chunk.functions` slots (so indices are pre-determined and unique)
2. `ForkJoinPool.invokeAll()` over all methods
3. Write compiled chunks into pre-allocated slots in order
4. Sequential fallback on error

### Error Handling

- If ForkJoinPool fails (e.g., single-threaded env), fall back to sequential compilation
- Wrap in try-catch at the pool level, not per-method

## Thread Safety

- Each method compilation uses fresh instances: `SsaBuilder`, `LivenessAnalyzer`, `RegisterAllocator`, `SpillInserter`
- `Chunk` is built sequentially before/after parallel section
- Only shared write is `chunk.functions[idx] = compiledChunk` at pre-determined unique indices
- No cross-method state sharing during compilation

## Testing

1. **Correctness tests** — existing test suite must pass (bytecode equivalence)
2. **Timing test** — compile class with N methods in parallel, verify speedup
3. **Thread safety** — compile multiple scripts concurrently from different `InkCompiler` instances

## Risk

- **Low** — concurrent execution of existing code paths, no structural changes
- Fallback to sequential on pool failure

## File Changes

- `lang/src/main/kotlin/org/inklang/lang/IrCompiler.kt` — main implementation
- `lang/src/test/kotlin/org/inklang/ParallelMethodCompilationTest.kt` — new test file
