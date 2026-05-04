import java.io.*;
import java.util.*;

/**
 * Represents a Context-Free Grammar.
 * Handles reading, storing, left factoring, and left recursion removal.
 */
public class Grammar {

    // Ordered list of non-terminals (insertion order preserved)
    private List<String> nonTerminals = new ArrayList<>();
    // Productions: NonTerminal -> list of alternatives (each alternative = list of symbols)
    private LinkedHashMap<String, List<List<String>>> productions = new LinkedHashMap<>();
    // Start symbol
    private String startSymbol;
    // Counter for generating fresh non-terminal names
    private int primeCounter = 0;

    // ── Constants ──────────────────────────────────────────────────────────────
    public static final String EPSILON = "epsilon";
    public static final String END_MARKER = "$";

    // ══════════════════════════════════════════════════════════════════════════
    //  Reading
    // ══════════════════════════════════════════════════════════════════════════

    /**
     * Read grammar from file.
     * Format: NonTerminal -> alt1 | alt2 | ...
     * Symbols within an alternative are separated by spaces.
     */
    public void readFromFile(String filename) throws IOException {
        try (BufferedReader br = new BufferedReader(new FileReader(filename))) {
            String line;
            boolean first = true;
            while ((line = br.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("#")) continue;

                // Split on "->"
                int arrowIdx = line.indexOf("->");
                if (arrowIdx < 0) continue;

                String lhs = line.substring(0, arrowIdx).trim();
                String rhs = line.substring(arrowIdx + 2).trim();

                // Validate: non-terminal must start with uppercase and be multi-char or single uppercase
                // Assignment says multi-character starting with uppercase; we accept both.
                if (!Character.isUpperCase(lhs.charAt(0))) {
                    throw new IllegalArgumentException("Non-terminal must start with uppercase: " + lhs);
                }

                if (first) {
                    startSymbol = lhs;
                    first = false;
                }

                if (!nonTerminals.contains(lhs)) {
                    nonTerminals.add(lhs);
                }

                List<List<String>> alts = productions.computeIfAbsent(lhs, k -> new ArrayList<>());

                // Split alternatives by '|'
                String[] alternatives = rhs.split("\\|");
                for (String alt : alternatives) {
                    alt = alt.trim();
                    List<String> symbols = new ArrayList<>();
                    if (alt.equals(EPSILON) || alt.equals("@")) {
                        symbols.add(EPSILON);
                    } else {
                        for (String sym : alt.split("\\s+")) {
                            if (!sym.isEmpty()) symbols.add(sym);
                        }
                    }
                    alts.add(symbols);
                }
            }
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  Left Factoring
    // ══════════════════════════════════════════════════════════════════════════

    /**
     * Apply left factoring repeatedly until no common prefixes remain.
     */
    public void applyLeftFactoring() {
        boolean changed = true;
        while (changed) {
            changed = false;
            // Iterate over a snapshot of current non-terminals
            List<String> nts = new ArrayList<>(nonTerminals);
            for (String nt : nts) {
                if (factorNonTerminal(nt)) {
                    changed = true;
                }
            }
        }
    }

    /**
     * Factor a single non-terminal. Returns true if any factoring was done.
     */
    private boolean factorNonTerminal(String nt) {
        List<List<String>> alts = productions.get(nt);
        if (alts == null || alts.size() <= 1) return false;

        // Find longest common prefix among all subsets
        // Group alternatives by first symbol
        Map<String, List<List<String>>> groups = new LinkedHashMap<>();
        for (List<String> alt : alts) {
            String first = alt.isEmpty() ? EPSILON : alt.get(0);
            groups.computeIfAbsent(first, k -> new ArrayList<>()).add(alt);
        }

        // Check if any group has more than one alternative (= common prefix exists)
        boolean needsFactoring = groups.values().stream().anyMatch(g -> g.size() > 1);
        if (!needsFactoring) return false;

        List<List<String>> newAlts = new ArrayList<>();

        for (Map.Entry<String, List<List<String>>> entry : groups.entrySet()) {
            List<List<String>> group = entry.getValue();
            if (group.size() == 1) {
                newAlts.add(group.get(0));
            } else {
                // Find longest common prefix in this group
                List<String> lcp = longestCommonPrefix(group);

                // Create new non-terminal
                String newNT = generateNewNT(nt);

                // New production: nt -> lcp newNT
                List<String> newAlt = new ArrayList<>(lcp);
                newAlt.add(newNT);
                newAlts.add(newAlt);

                // Build productions for newNT
                List<List<String>> newNTAlts = new ArrayList<>();
                for (List<String> alt : group) {
                    List<String> suffix = alt.subList(lcp.size(), alt.size());
                    if (suffix.isEmpty()) {
                        newNTAlts.add(new ArrayList<>(Collections.singletonList(EPSILON)));
                    } else {
                        newNTAlts.add(new ArrayList<>(suffix));
                    }
                }
                nonTerminals.add(newNT);
                productions.put(newNT, newNTAlts);
            }
        }

        productions.put(nt, newAlts);
        return true;
    }

    private List<String> longestCommonPrefix(List<List<String>> alts) {
        if (alts.isEmpty()) return new ArrayList<>();
        List<String> prefix = new ArrayList<>(alts.get(0));
        for (int i = 1; i < alts.size(); i++) {
            List<String> alt = alts.get(i);
            int len = Math.min(prefix.size(), alt.size());
            int k = 0;
            while (k < len && prefix.get(k).equals(alt.get(k))) k++;
            prefix = prefix.subList(0, k);
        }
        return new ArrayList<>(prefix);
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  Left Recursion Removal
    // ══════════════════════════════════════════════════════════════════════════

    /**
     * Remove both direct and indirect left recursion.
     * Uses the standard algorithm ordering non-terminals A1..An.
     */
    public void removeLeftRecursion() {
        List<String> orderedNTs = new ArrayList<>(nonTerminals);
        int n = orderedNTs.size();

        for (int i = 0; i < n; i++) {
            String ai = orderedNTs.get(i);

            // Step 1: Substitute indirect left recursion from Aj (j < i) into Ai
            for (int j = 0; j < i; j++) {
                String aj = orderedNTs.get(j);
                substituteIndirect(ai, aj);
            }

            // Step 2: Eliminate direct left recursion in Ai
            eliminateDirectLR(ai);
        }

        // Clean up any non-terminals that may have been added during substitution
        // (They won't have direct left recursion since they were freshly created)
    }

    /**
     * Substitute all occurrences of Aj at the head of Ai's productions with Aj's productions.
     */
    private void substituteIndirect(String ai, String aj) {
        List<List<String>> aiAlts = productions.get(ai);
        if (aiAlts == null) return;
        List<List<String>> ajAlts = productions.get(aj);
        if (ajAlts == null) return;

        List<List<String>> newAiAlts = new ArrayList<>();
        for (List<String> alt : aiAlts) {
            if (!alt.isEmpty() && alt.get(0).equals(aj)) {
                // Expand: Ai -> Aj rest  =>  Ai -> ajAlt rest  for each ajAlt
                List<String> rest = alt.subList(1, alt.size());
                for (List<String> ajAlt : ajAlts) {
                    List<String> newAlt = new ArrayList<>();
                    if (!(ajAlt.size() == 1 && ajAlt.get(0).equals(EPSILON))) {
                        newAlt.addAll(ajAlt);
                    }
                    newAlt.addAll(rest);
                    if (newAlt.isEmpty()) newAlt.add(EPSILON);
                    newAiAlts.add(newAlt);
                }
            } else {
                newAiAlts.add(alt);
            }
        }
        productions.put(ai, newAiAlts);
    }

    /**
     * Eliminate direct left recursion for a single non-terminal.
     * A -> A α1 | A α2 | β1 | β2
     * becomes:
     * A  -> β1 A' | β2 A'
     * A' -> α1 A' | α2 A' | ε
     */
    private void eliminateDirectLR(String nt) {
        List<List<String>> alts = productions.get(nt);
        if (alts == null) return;

        List<List<String>> recursive    = new ArrayList<>();
        List<List<String>> nonRecursive = new ArrayList<>();

        for (List<String> alt : alts) {
            if (!alt.isEmpty() && alt.get(0).equals(nt)) {
                recursive.add(alt.subList(1, alt.size())); // strip the leading nt
            } else {
                nonRecursive.add(alt);
            }
        }

        if (recursive.isEmpty()) return; // No direct left recursion

        String newNT = generateNewNT(nt);

        // A -> β1 A' | β2 A'
        List<List<String>> newAlts = new ArrayList<>();
        if (nonRecursive.isEmpty()) {
            // All alternatives were left-recursive; add epsilon A' as only non-recursive form
            List<String> epsilonAlt = new ArrayList<>();
            epsilonAlt.add(newNT);
            newAlts.add(epsilonAlt);
        }
        for (List<String> beta : nonRecursive) {
            List<String> newAlt = new ArrayList<>(beta);
            if (newAlt.size() == 1 && newAlt.get(0).equals(EPSILON)) newAlt.clear();
            newAlt.add(newNT);
            newAlts.add(newAlt);
        }

        // A' -> α1 A' | α2 A' | ε
        List<List<String>> newNTAlts = new ArrayList<>();
        for (List<String> alpha : recursive) {
            List<String> newAlt = new ArrayList<>(alpha);
            newAlt.add(newNT);
            newNTAlts.add(newAlt);
        }
        newNTAlts.add(new ArrayList<>(Collections.singletonList(EPSILON)));

        productions.put(nt, newAlts);
        nonTerminals.add(newNT);
        productions.put(newNT, newNTAlts);
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  Helpers
    // ══════════════════════════════════════════════════════════════════════════

    /** Generate a unique new non-terminal name by appending Prime suffixes. */
    private String generateNewNT(String base) {
        // Strip trailing digits to get clean base
        String candidate = base + "Prime";
        while (productions.containsKey(candidate) || nonTerminals.contains(candidate)) {
            candidate = candidate + "Prime";
        }
        return candidate;
    }

    /** Check whether a symbol is a non-terminal in this grammar. */
    public boolean isNonTerminal(String sym) {
        return productions.containsKey(sym);
    }

    /** Check whether a symbol is a terminal (including $). */
    public boolean isTerminal(String sym) {
        return !isNonTerminal(sym) && !sym.equals(EPSILON);
    }

    /** Return all terminals used in the grammar. */
    public Set<String> getTerminals() {
        Set<String> terminals = new LinkedHashSet<>();
        for (List<List<String>> alts : productions.values()) {
            for (List<String> alt : alts) {
                for (String sym : alt) {
                    if (!sym.equals(EPSILON) && !isNonTerminal(sym)) {
                        terminals.add(sym);
                    }
                }
            }
        }
        terminals.add(END_MARKER);
        return terminals;
    }

    // ── Getters ────────────────────────────────────────────────────────────────

    public List<String> getNonTerminals() { return nonTerminals; }
    public Map<String, List<List<String>>> getProductions() { return productions; }
    public List<List<String>> getProductions(String nt) {
        return productions.getOrDefault(nt, new ArrayList<>());
    }
    public String getStartSymbol() { return startSymbol; }

    // ══════════════════════════════════════════════════════════════════════════
    //  Display / Output
    // ══════════════════════════════════════════════════════════════════════════

    public void display(String title) {
        System.out.println("\n┌─ " + title + " " + "─".repeat(Math.max(0, 50 - title.length())) + "┐");
        for (String nt : nonTerminals) {
            List<List<String>> alts = productions.get(nt);
            if (alts == null) continue;
            StringBuilder sb = new StringBuilder("  " + nt + " -> ");
            for (int i = 0; i < alts.size(); i++) {
                if (i > 0) sb.append(" | ");
                sb.append(String.join(" ", alts.get(i)));
            }
            System.out.println(sb);
        }
        System.out.println("└" + "─".repeat(53) + "┘");
    }

    /** Write transformed grammar + FIRST/FOLLOW + table to a file. */
    public void writeToFile(PrintWriter pw, FirstFollow ff, ParsingTable table) {
        pw.println("=== Transformed Grammar ===");
        for (String nt : nonTerminals) {
            List<List<String>> alts = productions.get(nt);
            if (alts == null) continue;
            StringBuilder sb = new StringBuilder(nt + " -> ");
            for (int i = 0; i < alts.size(); i++) {
                if (i > 0) sb.append(" | ");
                sb.append(String.join(" ", alts.get(i)));
            }
            pw.println(sb);
        }
        pw.println();
        ff.writeToFile(pw);
        pw.println();
        table.writeToFile(pw);
    }
}
