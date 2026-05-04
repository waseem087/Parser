# LL(1) Predictive Parser — CS4031 Compiler Construction Assignment 02

## Team Members
| Roll Number | Name   |
|-------------|--------|
| 22i-1226     | Waseem Akhtar |
| 22i-0889    | Naveed Ahmad |

**Section:** A  
**Language:** Java  
**Spring 2026**

---

## Project Structure

```
LL1Parser/
├── src/
│   ├── Main.java           - Entry point & pipeline orchestrator
│   ├── Grammar.java        - CFG storage, left factoring, left recursion removal
│   ├── FirstFollow.java    - FIRST and FOLLOW set computation
│   ├── ParsingTable.java   - LL(1) table construction
│   ├── ParserStack.java    - Generic stack data structure
│   ├── Parser.java         - Stack-based LL(1) parsing algorithm
│   ├── ParseResult.java    - Trace rows and result container
│   ├── ParseTreeNode.java  - Parse tree node
│   ├── ParseTree.java      - Parse tree wrapper & display
│   └── ErrorHandler.java   - Error detection & panic-mode recovery
├── input/
│   ├── grammar1.txt        - Simple grammar (epsilon)
│   ├── grammar1_valid.txt  - Valid strings for grammar 1
│   ├── grammar1_errors.txt - Error strings for grammar 1
│   ├── grammar1_edge.txt   - Edge cases for grammar 1
│   ├── grammar2.txt        - Expression grammar (left recursion)
│   ├── grammar2_valid.txt
│   ├── grammar2_errors.txt
│   ├── grammar2_edge.txt
│   ├── grammar3.txt        - Statement grammar (left factoring)
│   ├── grammar3_valid.txt
│   ├── grammar3_errors.txt
│   ├── grammar3_edge.txt
│   ├── grammar4.txt        - Indirect left recursion grammar
│   ├── grammar4_valid.txt
│   ├── grammar4_errors.txt
│   └── grammar4_edge.txt
├── output/                 - Generated output files (created at runtime)
├── build.sh                - Build & run script
└── README.md
```

---

## Compilation Instructions

### Prerequisites
- Java 11 or higher installed
- `javac` and `java` on your PATH

### Compile
```bash
chmod +x build.sh
./build.sh compile
```

Or manually:
```bash
mkdir -p out
javac -d out src/*.java
```

---

## Execution Instructions

### Run a specific grammar + input file pair
```bash
java -cp out Main <grammar_file> <input_file> [output_dir]
```

**Example:**
```bash
java -cp out Main input/grammar2.txt input/grammar2_valid.txt output/
```

### Run all grammars with all input files
```bash
./build.sh all
```

### Quick demo (default: grammar 2 valid strings)
```bash
./build.sh
```

---

## Input File Format Specification

### Grammar File (`grammarN.txt`)

```
# Lines starting with # are comments
NonTerminal -> production1 | production2 | ...
```

**Rules:**
- One production rule per line
- Arrow symbol: `->` (with spaces around it)
- Alternatives separated by `|`
- Symbols within an alternative separated by **spaces**
- Non-terminals: multi-character names starting with **uppercase** (e.g., `Expr`, `Term`, `Factor`)
- Terminals: lowercase letters, operators, keywords (e.g., `id`, `+`, `*`, `(`, `)`)
- Epsilon: `epsilon` or `@`
- First non-terminal listed is the **start symbol**

**Example (`grammar2.txt`):**
```
Expr -> Expr + Term | Term
Term -> Term * Factor | Factor
Factor -> ( Expr ) | id
```

### Input String File (`grammarN_valid.txt` / `grammarN_errors.txt`)

```
# Lines starting with # are comments
token1 token2 token3
another token string
```

**Rules:**
- One input string per line
- Tokens separated by **spaces**
- Do **not** include `$` — it is added automatically
- Each file should contain strings for **one grammar only**

---

## Sample Grammar and Input Files Explanation

### Grammar 1 — Simple Grammar

<img width="1016" height="250" alt="image" src="https://github.com/user-attachments/assets/0d65310c-b0e0-4653-b702-ec28a67b2174" />

```
Start -> First Second
First -> a | epsilon
Second -> b
```
- Tests epsilon productions and FOLLOW-based table entries
- Valid: `a b`, `b`
<img width="1137" height="429" alt="image" src="https://github.com/user-attachments/assets/5a10fafe-fe21-4ff2-9c1a-b0722411a116" />

### Grammar 2 — Expression Grammar (Left Recursion).

<img width="491" height="90" alt="image" src="https://github.com/user-attachments/assets/93be0a3c-a952-45d0-8581-0e3ed562e7bd" />

```
Expr -> Expr + Term | Term
Term -> Term * Factor | Factor
Factor -> ( Expr ) | id
```
- Has direct left recursion → automatically removed
- After transformation:
  ```
  Expr      -> Term ExprPrime
  ExprPrime -> + Term ExprPrime | epsilon
  Term      -> Factor TermPrime
  TermPrime -> * Factor TermPrime | epsilon
  Factor    -> ( Expr ) | id
  ```
- Valid: `id + id * id`, `( id + id ) * id`
<img width="421" height="64" alt="image" src="https://github.com/user-attachments/assets/23fbdf46-b5fe-4ead-853b-9f67fe0e639b" />
<img width="418" height="554" alt="image" src="https://github.com/user-attachments/assets/9a99ea6d-e46a-4b5f-9b4f-68e056300012" />


### Grammar 3 — Statement Grammar (Left Factoring)
<img width="1005" height="188" alt="image" src="https://github.com/user-attachments/assets/26458f57-7917-42b6-8d1a-9570571ba42e" />

```
Stmt -> if Cond then Stmt else Stmt | if Cond then Stmt | a
Cond -> b
```
- Has common prefix `if Cond then Stmt` → left factoring applied
- Valid: `if b then a`, `if b then a else a`
<img width="581" height="59" alt="image" src="https://github.com/user-attachments/assets/d5f082e8-4259-464c-b628-4bd1011a3282" />

<img width="521" height="458" alt="image" src="https://github.com/user-attachments/assets/31bdc50c-9bff-4b87-99e3-d9d11059bb8c" />

### Grammar 4 — Indirect Left Recursion
<img width="477" height="85" alt="image" src="https://github.com/user-attachments/assets/e35b1028-5436-4667-96a1-4a75e0567193" />

```
Start -> Alpha a | b
Alpha -> Alpha c | Start d | epsilon
```
- Demonstrates indirect left recursion removal algorithm
- Valid: `b`, `a`

---
<img width="1283" height="611" alt="image" src="https://github.com/user-attachments/assets/614adfba-98ce-47d7-8f98-974810f1ce71" />

## Output Files

After running, the following files are generated in `output/`:

| File | Contents |
|------|----------|
| `grammarN_transformed.txt` | Transformed grammar + FIRST/FOLLOW sets + parsing table |
| `grammarN_trace.txt` | Step-by-step parsing traces for all input strings |
| `grammarN_trees.txt` | Parse trees for accepted strings |

---

## Known Limitations

1. The dangling-else grammar (Grammar 3) produces an LL(1) conflict after left factoring because the `else` clause can associate with multiple `if` statements. The parser resolves this by taking the first entry (associates with innermost `if`).
2. Grammar 4 with indirect left recursion may produce epsilon-only non-terminals after substitution; these are handled gracefully.
3. Very deeply nested input strings may produce tall parse trees that are harder to read in ASCII format.
4. The parser does not support multi-line productions in the grammar file.
