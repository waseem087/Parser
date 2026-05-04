#!/bin/bash
# build.sh - Compile and run the LL(1) Parser

SRC_DIR="src"
OUT_DIR="out"
MAIN_CLASS="Main"

# ── Compile ──────────────────────────────────────────────────────────────────
compile() {
    echo "Compiling Java sources..."
    mkdir -p "$OUT_DIR"
    javac -d "$OUT_DIR" "$SRC_DIR"/*.java
    if [ $? -eq 0 ]; then
        echo "Compilation successful."
    else
        echo "Compilation FAILED."
        exit 1
    fi
}

# ── Run ───────────────────────────────────────────────────────────────────────
run() {
    GRAMMAR="${1:-input/grammar2.txt}"
    INPUT="${2:-input/grammar2_valid.txt}"
    OUTPUT="${3:-output/}"
    echo "Running parser: grammar=$GRAMMAR  input=$INPUT  output=$OUTPUT"
    java -cp "$OUT_DIR" "$MAIN_CLASS" "$GRAMMAR" "$INPUT" "$OUTPUT"
}

# ── Run all grammars ───────────────────────────────────────────────────────────
run_all() {
    mkdir -p output
    for g in 1 2 3 4; do
        for suffix in valid errors edge; do
            IFILE="input/grammar${g}_${suffix}.txt"
            if [ -f "$IFILE" ]; then
                echo "=== Grammar $g | $suffix ==="
                java -cp "$OUT_DIR" "$MAIN_CLASS" \
                    "input/grammar${g}.txt" \
                    "$IFILE" \
                    "output/"
                echo ""
            fi
        done
    done
}

# ── Clean ─────────────────────────────────────────────────────────────────────
clean() {
    rm -rf "$OUT_DIR"
    rm -f output/*.txt
    echo "Cleaned."
}

# ── Main ──────────────────────────────────────────────────────────────────────
case "${1}" in
    compile) compile ;;
    run)     compile && run "$2" "$3" "$4" ;;
    all)     compile && run_all ;;
    clean)   clean ;;
    *)
        compile
        echo ""
        echo "Usage:"
        echo "  ./build.sh compile              - compile only"
        echo "  ./build.sh run <grammar> <input> [outdir] - run one parse"
        echo "  ./build.sh all                  - run all grammars"
        echo "  ./build.sh clean                - remove build artifacts"
        echo ""
        echo "Quick demo (Grammar 2 - Expression Grammar):"
        run "input/grammar2.txt" "input/grammar2_valid.txt" "output/"
        ;;
esac
