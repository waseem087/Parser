# CS4031 — Compiler Construction  
## Assignment 02: LL(1) Parser Design & Implementation  
### Spring 2026

---

## 1. Introduction

An **LL(1) predictive parser** is a top-down, table-driven parser that reads input Left-to-right, produces a Leftmost derivation, and uses 1 token of lookahead. It is one of the most practical parsing strategies because it runs in linear time and the table construction is mechanical.

The full pipeline implemented in this assignment:

```
Grammar File → Left Factoring → Left Recursion Removal
             → FIRST Sets → FOLLOW Sets
             → LL(1) Parsing Table
             → Stack-Based Parser → Parse Tree
```

---

## 2. Approach

### 2.1 Data Structures

| Structure | Class | Purpose |
|-----------|-------|---------|
| Grammar | `Grammar.java` | `LinkedHashMap<String, List<List<String>>>` maps each non-terminal to its list of alternative productions. Insertion order preserved for display. |
| FIRST/FOLLOW | `FirstFollow.java` | `Map<String, Set<String>>` for both sets. `LinkedHashSet` preserves insertion order. |
| Parsing Table | `ParsingTable.java` | `Map<String, Map<String, List<String>>>`: outer key = non-terminal, inner key = terminal, value = production body. |
| Parser Stack | `ParserStack.java` | Wraps `ArrayDeque<T>` with explicit `push`, `pop`, `peek` and a bottom-up display method. |
| Parse Tree | `ParseTreeNode.java` | N-ary tree: each node holds a label and a list of child nodes. A parallel `nodeStack` mirrors the symbol stack during parsing. |
| Error Records | `ErrorHandler.java` | `List<ParseError>` accumulates all errors found in a single parse run. |

### 2.2 Algorithm Implementation Details

#### Left Factoring
1. For each non-terminal, group all alternatives by their **first symbol**.
2. If any group has more than one alternative, the group shares a common prefix.
3. Find the **longest common prefix** (LCP) within the group.
4. Replace: `A -> α β1 | α β2` with `A -> α A'` and `A' -> β1 | β2`.
5. Repeat until no group has more than one alternative.

New non-terminal names append `Prime` suffixes (e.g., `StmtPrime`, `StmtPrimePrime`) to avoid collisions.

#### Left Recursion Removal
The standard algorithm (Dragon Book §4.3.3):

1. **Order** non-terminals: A₁, A₂, ..., Aₙ.
2. For each Aᵢ (i = 1..n):
   a. For each Aⱼ (j < i): substitute all `Aᵢ -> Aⱼ γ` with `Aᵢ -> δ₁ γ | δ₂ γ | ...` where `Aⱼ -> δ₁ | δ₂ | ...`
   b. Eliminate **direct** left recursion from Aᵢ.

**Direct left recursion elimination:**  
`A -> A α₁ | A α₂ | β₁ | β₂`  
⟹  
`A -> β₁ A' | β₂ A'`  
`A' -> α₁ A' | α₂ A' | ε`

Edge case: if all alternatives of A are recursive (no β), we create `A -> A'` so the grammar remains consistent.

#### FIRST Set Computation
- Iterative fixed-point: compute until stable.
- For each production `A -> X₁ X₂ ... Xₙ`:
  - Add FIRST(X₁) − {ε}
  - If ε ∈ FIRST(X₁), add FIRST(X₂) − {ε}, etc.
  - If every Xᵢ can derive ε, add ε to FIRST(A).

#### FOLLOW Set Computation
- Initialize FOLLOW(start) = {$}.
- For each production `A -> α B β`:
  - Add FIRST(β) − {ε} to FOLLOW(B).
  - If ε ∈ FIRST(β) (or β is empty), add FOLLOW(A) to FOLLOW(B).
- Iterate until stable.

#### Parsing Table Construction
For each production `A -> α`:
1. For each terminal `a` in FIRST(α): add `A -> α` to M[A, a].
2. If ε ∈ FIRST(α): for each `b` in FOLLOW(A): add `A -> α` to M[A, b].

A grammar is LL(1) iff no cell has more than one production.

#### Parsing Algorithm
```
Stack: [$ , StartSymbol]    ($ at bottom)
Input: [token₁, token₂, ..., tokenₙ, $]

Loop:
  X = top of stack
  a = current input token
  if X == $ and a == $  → ACCEPT
  if X is terminal:
      if X == a  → pop X, advance input  (match)
      else       → ERROR (terminal mismatch)
  if X is non-terminal:
      look up M[X, a]
      if M[X, a] = {X -> Y₁ Y₂ ... Yₖ}:
          pop X
          push Yₖ, Yₖ₋₁, ..., Y₁   (reverse order)
      else → ERROR (empty table entry)
```

### 2.3 Indirect Left Recursion Handling

Grammar 4 demonstrates this:
```
Start -> Alpha a | b
Alpha -> Alpha c | Start d | epsilon
```

Ordering: Start (A₁), Alpha (A₂).

Processing A₂=Alpha with j=1 (A₁=Start):
- `Alpha -> Start d` is expanded by substituting Start's productions:
  - `Start -> Alpha a` ⟹ `Alpha -> Alpha a d`
  - `Start -> b` ⟹ `Alpha -> b d`

After substitution: `Alpha -> Alpha c | Alpha a d | b d | epsilon`

Now eliminate direct left recursion:
- Recursive: `c`, `a d`
- Non-recursive: `b d`, `epsilon`

Result:
```
Alpha      -> b d AlphaPrime | AlphaPrime
AlphaPrime -> c AlphaPrime | a d AlphaPrime | epsilon
```

### 2.4 Error Recovery Strategy: Panic Mode

When M[X, a] is empty (non-terminal X has no rule for lookahead a):

1. **Report** the error with step number, stack top, and actual token.
2. Build the **synchronisation set** = FOLLOW(X) ∪ all grammar terminals ∪ {$}.
3. If `a` is already in the sync set: **pop X** from the stack (treat as epsilon) and continue.
4. Otherwise: **skip input tokens** until one in the sync set is found, then pop X and continue.

When a terminal X on the stack doesn't match input token a:
- Pop X from the stack (deletion recovery) and continue.

This allows the parser to find **multiple errors** in a single pass.

---

## 3. Challenges

### 3.1 Left Factoring Termination
Naïvely applying left factoring can loop if the new non-terminal itself has a common prefix. We solved this by repeating the outer loop until no change occurs, and by checking that the generated `PrimePrime...` name isn't already taken.

### 3.2 Epsilon in FIRST/FOLLOW
Epsilon interacts with both sets in subtle ways. A key insight: when computing FIRST of a sequence, epsilon only propagates if **every** preceding symbol can derive epsilon. We handle this with the `canDeriveEpsilon(sequence)` helper.

### 3.3 Indirect Left Recursion — Order Matters
The substitution step is sensitive to the ordering of non-terminals. We use the order they appear in the grammar file (LinkedHashMap preserves insertion order), which matches the standard textbook algorithm.

### 3.4 Parse Tree and Epsilon Nodes
When a production is `A -> epsilon`, we add an `epsilon` leaf child to the tree node for A, but push **nothing** onto the parser stack (epsilon means "match empty string"). This correctly represents the derivation while keeping the stack clean.

### 3.5 Dangling-Else Ambiguity (Grammar 3)
Grammar 3 is inherently ambiguous at the `else` clause. After left factoring we detect an LL(1) conflict at `M[StmtPrime, else]`. We keep the **first-entry** (the `else Stmt` production), which implements the standard "else associates with nearest if" convention.

---

## 4. Test Cases

### 4.1 Grammar 1 — Simple Grammar
```
Start -> First Second
First -> a | epsilon
Second -> b
```

| String | Expected | Reason |
|--------|----------|--------|
| `a b` | ACCEPT | First=a, Second=b |
| `b` | ACCEPT | First=epsilon, Second=b |
| `a a b` | REJECT | 'a' after First=a leaves unexpected 'a' before Second |
| `a` | REJECT | Second=b missing |
| `b b` | REJECT | Only one 'b' expected |
| `c` | REJECT | 'c' not in grammar |

### 4.2 Grammar 2 — Expression Grammar
```
Expr -> Term ExprPrime
ExprPrime -> + Term ExprPrime | epsilon
Term -> Factor TermPrime
TermPrime -> * Factor TermPrime | epsilon
Factor -> ( Expr ) | id
```

| String | Expected | Reason |
|--------|----------|--------|
| `id` | ACCEPT | Single identifier |
| `id + id` | ACCEPT | Addition |
| `id * id` | ACCEPT | Multiplication |
| `id + id * id` | ACCEPT | Mixed, * binds tighter |
| `( id + id ) * id` | ACCEPT | Parenthesized sub-expression |
| `( id + id + id )` | ACCEPT | Multiple additions |
| `id + * id` | REJECT | Operator follows operator |
| `( id + id` | REJECT | Missing closing parenthesis |
| `id id` | REJECT | Missing operator between ids |
| `+ id` | REJECT | Leading operator |

### 4.3 Grammar 3 — Statement Grammar
```
Stmt -> if Cond then Stmt StmtPrime | a
StmtPrime -> else Stmt | epsilon
Cond -> b
```

| String | Expected | Reason |
|--------|----------|--------|
| `a` | ACCEPT | Simple statement |
| `if b then a` | ACCEPT | If-then |
| `if b then a else a` | ACCEPT | If-then-else |
| `if b then if b then a else a` | ACCEPT | Nested, else binds inner if |
| `if b` | REJECT | Missing then-part |
| `then a` | REJECT | No if |
| `if b else a` | REJECT | Missing then |

### 4.4 Grammar 4 — Indirect Left Recursion
```
Start -> Alpha a | b
Alpha -> Alpha c | Start d | epsilon
```
After transformation:
```
Start      -> Alpha a | b
Alpha      -> b d AlphaPrime | AlphaPrime
AlphaPrime -> c AlphaPrime | a d AlphaPrime | epsilon
```

| String | Expected | Reason |
|--------|----------|--------|
| `b` | ACCEPT | Start -> b |
| `a` | ACCEPT | Start -> Alpha a, Alpha -> AlphaPrime -> ε |
| `c` | REJECT | No rule produces leading 'c' from Start |
| `d` | REJECT | No rule produces leading 'd' from Start |
| `a b` | REJECT | Extra token after accepted 'a' |

---

## 5. Verification

1. **FIRST/FOLLOW manual check**: We hand-computed FIRST and FOLLOW for Grammar 2 against the Dragon Book (p.221) and verified they match.
2. **Parsing trace check**: Traced `id + id * id` step by step against the assignment's example in §5.3 — all 13 steps match exactly.
3. **Grammar 1 epsilon test**: Verified that `b` (without leading `a`) is accepted, confirming epsilon handling works.
4. **Error recovery check**: `id + * id` for Grammar 2 produces exactly one error and recovers to continue parsing.
5. **Indirect left recursion check**: Traced Grammar 4's transformation by hand and verified the resulting grammar generates the same language.

---

## 6. Sample Outputs

### 6.1 Grammar 2 Parse Tree for `( id + id ) * id`

```
└── Expr
    ├── Term
    │   ├── Factor
    │   │   ├── (
    │   │   ├── Expr
    │   │   │   ├── Term
    │   │   │   │   ├── Factor
    │   │   │   │   │   └── id
    │   │   │   │   └── TermPrime
    │   │   │   │       └── epsilon
    │   │   │   └── ExprPrime
    │   │   │       ├── +
    │   │   │       ├── Term
    │   │   │       │   ├── Factor
    │   │   │       │   │   └── id
    │   │   │       │   └── TermPrime
    │   │   │       │       └── epsilon
    │   │   │       └── ExprPrime
    │   │   │           └── epsilon
    │   │   └── )
    │   └── TermPrime
    │       ├── *
    │       ├── Factor
    │       │   └── id
    │       └── TermPrime
    │           └── epsilon
    └── ExprPrime
        └── epsilon
```

### 6.2 Error Handling for `id + * id`

```
Step | Stack               | Input        | Action
-----|---------------------|--------------|------------------------------------------
1    | $ Expr              | id + * id $  | Expr -> Term ExprPrime
2    | $ ExprPrime Term    | id + * id $  | Term -> Factor TermPrime
3    | $ EP TP Factor      | id + * id $  | Factor -> id
4    | $ EP TP id          | id + * id $  | Match 'id'
5    | $ EP TP             | + * id $     | TermPrime -> epsilon
6    | $ EP                | + * id $     | ExprPrime -> + Term ExprPrime
7    | $ EP Term +         | + * id $     | Match '+'
8    | $ EP Term           | * id $       | ERROR: no rule for [Term, *]
                                            Expected: id, (
     [RECOVERY] panic-mode: skipping '*'
9    | $ EP Term           | id $         | Term -> Factor TermPrime
10   | $ EP TP Factor      | id $         | Factor -> id
11   | $ EP TP id          | id $         | Match 'id'
12   | $ EP TP             | $            | TermPrime -> epsilon
13   | $ EP                | $            | ExprPrime -> epsilon
14   | $                   | $            | ACCEPT (with 1 error)

Result: Parsing completed with 1 error.
```

---

## 7. Conclusion

This assignment deepened our understanding of:

- **Grammar transformations**: Left factoring and recursion removal are algorithmic and can introduce many new non-terminals for complex grammars.
- **Fixed-point iteration**: Both FIRST and FOLLOW require iterating until stable; a single pass is insufficient.
- **LL(1) limitations**: Not all grammars can be parsed with LL(1). The dangling-else grammar is a classic example of an inherently ambiguous grammar that requires special handling.
- **Error recovery**: Panic-mode recovery is simple to implement and effective for finding multiple errors, but can sometimes skip valid tokens.
- **Parse trees**: Building the tree in parallel with the stack (using a mirrored node stack) is an elegant approach that requires no post-processing.

The modular design (one Java class per concern) made testing and debugging each component independently much easier.
