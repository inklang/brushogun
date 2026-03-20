# Standard Library: Set and Tuple — Design

## Context

Lectern currently has `Map` (hash-based, mutable) and `Array` (list, mutable) as built-in collection types. The HashMap-backed Map is fully implemented. This design adds:

- **Set** — unordered collection of unique values, mutable
- **Tuple** — ordered, immutable sequence

## Syntax

| Type | Literal Syntax | Factory Syntax |
|------|---------------|----------------|
| Set  | `{1, 2, 3}`   | `Set(1, 2, 3)` |
| Tuple | `(1, 2, 3)`  | `Tuple(1, 2, 3)` |

### Parsing Notes

- `{...}` set literals: requires lexer/parser support (distinct from block statements — ambiguity resolved by requiring at least one element or disallowing empty braces as blocks)
- `(...)` tuple literals: requires parser support; ambiguous with grouping parentheses — resolved by requiring 2+ elements or a trailing comma `(1,)` at parse-time
- Both desugar to factory constructor calls at the AST level (consistent with how `Map()` and array literals work)

## Implementation

### 1. Storage Wrappers (`Value.kt`)

```kotlin
/** Wrapper for internal MutableSet storage (used by Set class) */
data class InternalSet(val entries: MutableSet<Value> = mutableSetOf()) : Value() {
    override fun toString() = entries.joinToString(", ", "{", "}")
}

/** Wrapper for internal List storage (used by Tuple class) — immutable */
data class InternalTuple(val items: List<Value>) : Value() {
    override fun toString() = items.joinToString(", ", "(", ")")
}
```

Both parallel existing `InternalList` / `InternalMap` patterns.

### 2. SetClass (`Value.kt`, `Builtins` object)

```
methods:
  init      → self.fields["__entries"] = InternalSet(); return Null
  add(item) → entries.add(item); return Null
  has(item) → return Boolean(entries.contains(item))
  remove(item) → entries.remove(item); return Null
  size      → return Int(entries.size)
  clear     → entries.clear(); return Null
  delete(item) → alias for remove(item)
  iter      → return ArrayIterator-backed iterator over entries (can reuse ArrayIteratorClass with InternalSet wrapped as InternalList, or new dedicated SetIteratorClass)
```

**Iterator**: Reuse `ArrayIteratorClass` but with a `SetIterator` class instead to avoid boxing `InternalSet` into `InternalList`.

```
SetIteratorClass:
  fields: __entries (InternalSet), __items (List<Value> derived once at iter() call for consistent iteration), current (Int)
  hasNext → current < items.size
  next    → items[current]; current++
```

Note: set iteration order is undefined but must be deterministic within a single `iter()` call.

### 3. TupleClass (`Value.kt`, `Builtins` object)

```
methods:
  size → return Int(items.size)
  get(index) → return items[index] (0-based)
  has(item) → return Boolean(items.contains(item))
  iter → return ArrayIterator over items (reuse ArrayIteratorClass)
```

Tuple is immutable — **no `set`, `add`, `remove`, `clear` methods**.

```
factory constructor: Tuple(arg1, arg2, ...)
  → LoadClass("Tuple")
  → NewInstance with args
  → init() stores args as InternalTuple(items.toList()) in self.fields["__items"]
```

### 4. VM Globals (`VM.kt`)

```kotlin
globals = mutableMapOf(
    "Set" to Value.Class(Builtins.SetClass),
    "Tuple" to Value.Class(Builtins.TupleClass),
    ...
)
```

### 5. Parser Changes

**Set literal `{expr, expr, ...}`:**
- New `SetExpr` AST node: `data class SetExpr(val elements: List<Expr>)`
- Lowered as factory call: `Set(expr1, expr2, ...)` — no new IR instruction needed
- Disallow empty `{}` as set literal (parsed as block statement instead)

**Tuple literal `(expr, expr, ...)`:**
- New `TupleExpr` AST node: `data class TupleExpr(val elements: List<Expr>)`
- Enforce immutability at construction — `InternalTuple` is immutable
- Single-element tuple syntax `(expr,)` supported via trailing comma
- `(expr)` without comma remains grouping parentheses

### 6. Lowering (`AstLowerer.kt`)

```kotlin
is Expr.SetExpr -> {
    // Desugar to Set(element1, element2, ...)
    val elementRegs = expr.elements.map { lowerExpr(it, freshReg()) }
    val setClassReg = freshReg()
    emit(IrInstr.LoadGlobal(setClassReg, "Set"))
    val dstReg = freshReg()
    emit(IrInstr.NewInstance(dstReg, setClassReg, elementRegs))
    dstReg
}
is Expr.TupleExpr -> {
    // Desugar to Tuple(element1, element2, ...)
    val elementRegs = expr.elements.map { lowerExpr(it, freshReg()) }
    val tupleClassReg = freshReg()
    emit(IrInstr.LoadGlobal(tupleClassReg, "Tuple"))
    val dstReg = freshReg()
    emit(IrInstr.NewInstance(dstReg, tupleClassReg, elementRegs))
    dstReg
}
```

Both reuse existing `IrInstr.NewInstance` — no new IR instructions needed.

### 7. Tuple Indexing

Support `tuple[0]`, `tuple[1]`, etc. via existing `IndexExpr` lowering:

```kotlin
is Expr.IndexExpr -> {
    // existing Array/Map indexing...
    // extend to Tuple: get from InternalTuple.items
}
```

The `IndexExpr` lowering already handles `GetIndex` IR instruction; extend to check if object is `Instance` with `TupleClass` and use `InternalTuple.items.get(index)`.

### 8. Iterator Protocol

Both Set and Tuple use the iterator protocol (`iter()` → `hasNext()` → `next()`) already established by Array. Set needs its own `SetIteratorClass` because iteration over a set requires snapshotting entries into a list at iteration start (to avoid ConcurrentModificationException and provide stable iteration order).

## What Is Not In Scope

- Set operations `union`, `intersection`, `difference` as built-in methods — can be added later as Lectern stdlib functions
- Ordered sets or linked sets
- Nested tuples / heterogeneous tuples (all types in a tuple use the same `Value` representation)
- `for x in set` with undefined order acknowledged (iteration order is JVM's hashmap order)

## Testing

- `SetClass` unit tests: add, has, remove, size, clear, duplicate deduplication, iteration
- `TupleClass` unit tests: get, size, has, iteration, immutability (no set/add/remove/clear)
- Integration tests: `{1, 2, 3}.has(2)`, `for x in (1, 2, 3)`, `let t = (1, 2); t[0]`
- Parser tests: `{a, b}`, `(a, b)`, `(a,)`, empty `{}` as block, `(a)` as grouping
