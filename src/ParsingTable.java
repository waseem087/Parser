import java.io.*;
import java.util.*;

/**
 * Constructs and stores the LL(1) parsing table.
 * M[NonTerminal][Terminal] = production (or null if empty / error)
 */
public class ParsingTable {

    private final Grammar    grammar;
    private final FirstFollow ff;

    // table[nt][terminal] -> production body (list of symbols)
    private final Map<String, Map<String, List<String>>> table = new LinkedHashMap<>();
    // Conflicts: stored for reporting
    private final List<String> conflicts = new ArrayList<>();
    private boolean isLL1 = true;

    public ParsingTable(Grammar grammar, FirstFollow ff) {
        this.grammar = grammar;
        this.ff      = ff;
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  Construction
    // ══════════════════════════════════════════════════════════════════════════

    public void construct() {
        // Initialize empty table
        for (String nt : grammar.getNonTerminals()) {
            table.put(nt, new LinkedHashMap<>());
        }

        for (String nt : grammar.getNonTerminals()) {
            for (List<String> production : grammar.getProductions(nt)) {

                // Compute FIRST(production)
                Set<String> firstAlpha = ff.firstOfSequence(production);

                // Rule 1: For each terminal a in FIRST(α), add M[A,a] = A -> α
                for (String terminal : firstAlpha) {
                    if (!terminal.equals(Grammar.EPSILON)) {
                        addEntry(nt, terminal, production);
                    }
                }

                // Rule 2: If ε ∈ FIRST(α), for each b in FOLLOW(A), add M[A,b] = A -> α
                if (firstAlpha.contains(Grammar.EPSILON)) {
                    for (String b : ff.getFollow(nt)) {
                        addEntry(nt, b, production);
                    }
                }
            }
        }
    }

    private void addEntry(String nt, String terminal, List<String> production) {
        Map<String, List<String>> row = table.get(nt);
        if (row.containsKey(terminal)) {
            // Conflict!
            String conflict = "CONFLICT at M[" + nt + ", " + terminal + "]: "
                    + nt + " -> " + String.join(" ", row.get(terminal))
                    + "  AND  " + nt + " -> " + String.join(" ", production);
            conflicts.add(conflict);
            isLL1 = false;
            // Keep the first entry (first-entry wins for panic recovery)
        } else {
            row.put(terminal, new ArrayList<>(production));
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  Query
    // ══════════════════════════════════════════════════════════════════════════

    /** Returns the production body for M[nt][terminal], or null if no entry. */
    public List<String> lookup(String nt, String terminal) {
        Map<String, List<String>> row = table.get(nt);
        if (row == null) return null;
        return row.get(terminal);
    }

    public boolean isLL1() { return isLL1; }

    // ══════════════════════════════════════════════════════════════════════════
    //  Display / Output
    // ══════════════════════════════════════════════════════════════════════════

    public void display() {
        List<String> terminals = new ArrayList<>(grammar.getTerminals());
        Collections.sort(terminals);

        // Column widths
        int ntWidth = grammar.getNonTerminals().stream()
                .mapToInt(String::length).max().orElse(10) + 2;
        int colWidth = 22;

        // Header
        System.out.println("\n┌─ LL(1) Parsing Table " + "─".repeat(30) + "┐");
        StringBuilder header = new StringBuilder(String.format("  %-" + ntWidth + "s", "NT"));
        for (String t : terminals) header.append(String.format(" │ %-" + colWidth + "s", t));
        System.out.println(header);
        System.out.println("  " + "─".repeat(ntWidth + terminals.size() * (colWidth + 3)));

        for (String nt : grammar.getNonTerminals()) {
            StringBuilder row = new StringBuilder(String.format("  %-" + ntWidth + "s", nt));
            Map<String, List<String>> ntRow = table.get(nt);
            for (String t : terminals) {
                String cell = "";
                if (ntRow != null && ntRow.containsKey(t)) {
                    cell = nt + "->" + String.join(" ", ntRow.get(t));
                    if (cell.length() > colWidth) cell = cell.substring(0, colWidth - 1) + "…";
                }
                row.append(String.format(" │ %-" + colWidth + "s", cell));
            }
            System.out.println(row);
        }
        System.out.println("└" + "─".repeat(53) + "┘");

        if (isLL1) {
            System.out.println("✔ Grammar IS LL(1)");
        } else {
            System.out.println("✘ Grammar is NOT LL(1). Conflicts found:");
            for (String c : conflicts) System.out.println("  " + c);
        }
    }

    public void writeToFile(PrintWriter pw) {
        pw.println("=== LL(1) Parsing Table ===");
        List<String> terminals = new ArrayList<>(grammar.getTerminals());
        Collections.sort(terminals);

        pw.printf("%-20s", "NT");
        for (String t : terminals) pw.printf(" | %-20s", t);
        pw.println();
        pw.println("-".repeat(20 + terminals.size() * 23));

        for (String nt : grammar.getNonTerminals()) {
            pw.printf("%-20s", nt);
            Map<String, List<String>> ntRow = table.get(nt);
            for (String t : terminals) {
                String cell = "";
                if (ntRow != null && ntRow.containsKey(t)) {
                    cell = nt + "->" + String.join(" ", ntRow.get(t));
                    if (cell.length() > 20) cell = cell.substring(0, 19) + "…";
                }
                pw.printf(" | %-20s", cell);
            }
            pw.println();
        }
        pw.println();
        pw.println("Grammar is " + (isLL1 ? "" : "NOT ") + "LL(1)");
        if (!isLL1) {
            for (String c : conflicts) pw.println("  " + c);
        }
    }
}
