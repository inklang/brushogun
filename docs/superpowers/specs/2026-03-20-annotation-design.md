# Annotation System Design for Ink

## Overview

Ink will support a compile-time annotation system for attaching metadata to declarations. Annotations are processed by the compiler to enable warnings, optimizations, and validation. This is a compile-time-only system — annotations are not stored in bytecode.

## Syntax

```ink
// Annotation declaration
annotation Deprecated {
    reason: string
}

annotation Inline {
    level: int
}

// Usage — annotations before declarations
@deprecated(reason="Use calculate() instead")
fn oldFunc() { ... }

@inline(level=2)
fn hotPath() { ... }

// Class annotation
@marked
class Config { ... }

// Field annotation
@validate @notNull
name: string

// Parameter annotation
fn process(@notNull input: string) { ... }
```

## Annotatable Targets

- **Functions** — `@inline myFunc() {...}`
- **Classes** — `@marked class Foo {...}`
- **Fields/Properties** — `@validate @notNull name: string`
- **Parameters** — `fn foo(@notNull arg: int)`

Multiple annotations can be stacked on a single declaration.

## Annotation Arguments

Arguments are named, similar to Python keyword arguments:
- `@deprecated(reason="...")`
- `@inline(level=2)`
- `@annotation(field=value, another=123)`

Positional arguments not supported — all arguments must be named.

## Annotation Declaration

Users can define custom annotations via `annotation` declarations:

```ink
annotation Deprecated {
    reason: string
}

annotation Inline {
    level: int  // defaults handled at use site
}
```

Annotation fields have types: `string`, `int`, `bool`, `float`, `double`.

## Built-in Annotations

| Annotation | Fields | Purpose |
|------------|--------|---------|
| `@deprecated` | `reason: string` | Warn when deprecated declaration is used |
| `@inline` | `level: int` (default=1) | Suggest inlining to optimizer |
| `@pure` | — | Function has no side effects |

## Implementation

### Lexer Changes

- Add `AT` token type
- Tokenize `@identifier` as a single token
- Handle `@identifier(args)` as AT + identifier + paren group

### Parser Changes

- Add `AnnotationDecl` statement type: `annotation Name { fields }`
- Add `AnnotationExpr` — holds annotation name + map of named args
- Parse annotations before declarations
- Attach annotation list to: `ClassStmt`, `FuncStmt`, `VarStmt`, `FuncParam`

### AST Changes

```kotlin
// New Expr type
sealed class Expr {
    data class Annotation(
        val name: String,
        val args: Map<String, Expr>
    ) : Expr()
}

// Attach to statements
data class ClassStmt(
    val annotations: List<Expr.Annotation>,
    val name: String,
    ...
)

// Attach to FuncParam
data class FuncParam(
    val annotations: List<Expr.Annotation>,
    val name: String,
    val type: Type?,
    val default: Expr?
)
```

### Compiler Pipeline

```
Source Code
    |
    v
Lexer (tokenize) --> Token stream
    |
    v
Parser (parse) --> AST (with annotation attachments)
    |
    v
ConstantFolder (existing)
    |
    v
[NEW] AnnotationChecker (processes @deprecated, @inline, @pure)
    |
    v
AstLowerer
    ...
```

### Annotation Processing (Compile-Time)

1. **DeprecatedChecker** — When resolving a call/reference to a `@deprecated` declaration, emit a compiler warning with the reason.
2. **InlineProcessor** — Attach inline hints to the IR for the optimizer.
3. **PureValidator** — For `@pure` functions, verify no side-effecting operations (reads globals, calls non-pure, I/O).
4. **Unknown annotations** — Ignored (extensible design).

### Error Handling

| Error | Behavior |
|-------|----------|
| Unknown annotation | Ignored (optional warning via flag) |
| `@inline` on non-function | Compiler error |
| Missing required annotation field | Compiler error |
| Unknown annotation field | Compiler error |
| `@deprecated` usage | Compiler warning with reason |

## Testing

```ink
// Test deprecation warning
@deprecated(reason="Old API")
fn oldApi() { true }

// Test inline annotation
@inline
fn smallFunc() { 1 + 1 }

// Test annotation on class
@marked
class Foo {}

// Test parameter annotations
fn check(@notNull value: string) { value }
```

## Files to Modify

1. `Token.kt` — Add `AT` token type
2. `Lexer.kt` — Tokenize `@identifier` and `@identifier(args)`
3. `Parser.kt` — Parse annotation declarations and usage
4. `AST.kt` — Add `AnnotationDecl` Stmt, `Annotation` Expr, attach to existing types
5. `InkCompiler.kt` — Add `AnnotationChecker` pass to pipeline
6. New file: `AnnotationChecker.kt` — Built-in annotation processors
7. Tests in `lang/src/test/kotlin/org/inklang/`