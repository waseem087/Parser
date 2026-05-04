import java.util.*;

/**
 * LL(1) predictive parser using a stack.
 * Implements:
 *  - Stack-based parsing algorithm
 *  - Panic-mode error recovery
 *  - Parse tree construction
 */
public class Parser {

    private final Grammar      grammar;
    private final ParsingTable table;
    private final FirstFollow  ff;

    public Parser(Grammar grammar, ParsingTable table) {
        this.grammar = grammar;
        this.table   = table;
        // We need ff for sync sets; re-compute from the same grammar is not ideal,
        // so we store it. The ParsingTable holds a reference; we'll reconstruct ff lazily.
        this.ff = null; // ff will be passed per-parse if needed
    }

    // Allow passing pre-built ff
    private FirstFollow storedFF;

    public Parser(Grammar grammar, ParsingTable table, FirstFollow ff) {
        this.grammar  = grammar;
        this.table    = table;
        this.ff       = ff;
        this.storedFF = ff;
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  Main Parse Method
    // ══════════════════════════════════════════════════════════════════════════

    /**
     * Parse a single input string.
     * @param inputLine space-separated token string (without $)
     */
    public ParseResult parse(String inputLine) {
        ErrorHandler eh     = new ErrorHandler();
        ParseResult  result = new ParseResult(eh);

        // Tokenize
        List<String> tokens = new ArrayList<>(Arrays.asList(inputLine.trim().split("\\s+")));
        tokens.add(Grammar.END_MARKER); // append $

        // Stack: bottom = $, top = start symbol
        ParserStack<String> stack = new ParserStack<>();
        stack.push(Grammar.END_MARKER);
        stack.push(grammar.getStartSymbol());

        // Parse tree: parallel stack of tree nodes mirrors the symbol stack
        ParserStack<ParseTreeNode> nodeStack = new ParserStack<>();
        ParseTreeNode dummyBottom = new ParseTreeNode(Grammar.END_MARKER);
        ParseTreeNode rootNode    = new ParseTreeNode(grammar.getStartSymbol());
        nodeStack.push(dummyBottom);
        nodeStack.push(rootNode);

        int step    = 1;
        int pos     = 0;  // pointer into tokens

        while (true) {
            String x = stack.peek();
            String a = (pos < tokens.size()) ? tokens.get(pos) : Grammar.END_MARKER;

            String stackDisplay = stack.toDisplayString();
            String inputDisplay = remainingInput(tokens, pos);

            // ── Accept ──────────────────────────────────────────────────────
            if (x.equals(Grammar.END_MARKER) && a.equals(Grammar.END_MARKER)) {
                result.addRow(step, stackDisplay, inputDisplay, "ACCEPT");
                result.setAccepted(true);
                result.setParseTree(new ParseTree(rootNode));
                break;
            }

            // ── Terminal match ───────────────────────────────────────────────
            if (grammar.isTerminal(x)) {
                if (x.equals(a)) {
                    result.addRow(step++, stackDisplay, inputDisplay, "Match '" + a + "'");
                    stack.pop();
                    nodeStack.pop(); // matched terminal stays as leaf
                    pos++;
                } else {
                    // ERROR: terminal mismatch
                    eh.reportError(ErrorHandler.ErrorType.UNEXPECTED_TOKEN,
                            "Expected '" + x + "' but found '" + a + "'",
                            step, x, a);
                    result.addRow(step++, stackDisplay, inputDisplay,
                            "ERROR: expected '" + x + "', got '" + a + "'");

                    // Recovery: pop the mismatched terminal from stack and continue
                    stack.pop();
                    nodeStack.pop();
                }
                continue;
            }

            // ── Non-terminal: consult table ──────────────────────────────────
            if (grammar.isNonTerminal(x)) {
                List<String> production = table.lookup(x, a);

                if (production != null) {
                    // Expand
                    String prodStr = x + " -> " + String.join(" ", production);
                    result.addRow(step++, stackDisplay, inputDisplay, prodStr);

                    stack.pop();
                    ParseTreeNode parentNode = nodeStack.pop();

                    // Create child nodes
                    List<ParseTreeNode> childNodes = new ArrayList<>();
                    for (String sym : production) {
                        ParseTreeNode child = new ParseTreeNode(sym);
                        parentNode.addChild(child);
                        childNodes.add(child);
                    }

                    // Push children in REVERSE order (so leftmost is on top)
                    if (!(production.size() == 1 && production.get(0).equals(Grammar.EPSILON))) {
                        for (int i = childNodes.size() - 1; i >= 0; i--) {
                            stack.push(production.get(i));
                            nodeStack.push(childNodes.get(i));
                        }
                    }
                    // If epsilon production, nothing is pushed (epsilon = empty derivation)

                } else {
                    // ERROR: empty table entry
                    String expected = expectedTokens(x);
                    eh.reportError(ErrorHandler.ErrorType.EMPTY_TABLE_ENTRY,
                            "No production for [" + x + ", " + a + "]. Expected: " + expected,
                            step, x, a);
                    result.addRow(step++, stackDisplay, inputDisplay,
                            "ERROR: no rule for [" + x + ", " + a + "]");

                    // Panic-mode recovery: synchronize on FOLLOW(x)
                    Set<String> syncSet = buildSyncSet(x);
                    if (syncSet.contains(a)) {
                        // Pop the non-terminal (treat as epsilon)
                        stack.pop();
                        nodeStack.pop();
                    } else {
                        // Skip input tokens until we find a sync token
                        while (pos < tokens.size() && !syncSet.contains(tokens.get(pos))) {
                            pos++;
                        }
                        // Also pop the current non-terminal
                        stack.pop();
                        nodeStack.pop();
                    }
                }
                continue;
            }

            // ── End-of-input but stack non-empty ────────────────────────────
            if (a.equals(Grammar.END_MARKER) && !x.equals(Grammar.END_MARKER)) {
                eh.reportError(ErrorHandler.ErrorType.PREMATURE_END,
                        "Unexpected end of input. Stack still has: " + stackDisplay,
                        step, x, a);
                result.addRow(step++, stackDisplay, inputDisplay,
                        "ERROR: premature end of input");
                break;
            }

            // Fallback break to avoid infinite loop
            result.addRow(step++, stackDisplay, inputDisplay, "ERROR: unknown state");
            break;
        }

        return result;
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  Helpers
    // ══════════════════════════════════════════════════════════════════════════

    /** Build the remaining input display string. */
    private String remainingInput(List<String> tokens, int pos) {
        StringBuilder sb = new StringBuilder();
        for (int i = pos; i < tokens.size(); i++) {
            if (sb.length() > 0) sb.append(" ");
            sb.append(tokens.get(i));
        }
        return sb.toString();
    }

    /** Build synchronisation set for panic-mode (FOLLOW + FIRST of next NT). */
    private Set<String> buildSyncSet(String nt) {
        Set<String> sync = new LinkedHashSet<>();
        // Use FOLLOW set if ff is available
        if (storedFF != null) {
            sync.addAll(storedFF.getFollow(nt));
        }
        // Always include $ as sync
        sync.add(Grammar.END_MARKER);
        // Add all terminals from grammar as a broad safety net
        sync.addAll(grammar.getTerminals());
        return sync;
    }

    /** List of terminals for which a rule exists in M[nt, *]. */
    private String expectedTokens(String nt) {
        StringBuilder sb = new StringBuilder();
        for (String t : grammar.getTerminals()) {
            if (table.lookup(nt, t) != null) {
                if (sb.length() > 0) sb.append(", ");
                sb.append(t);
            }
        }
        return sb.length() > 0 ? sb.toString() : "(none)";
    }
}
