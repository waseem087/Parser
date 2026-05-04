import java.io.*;
import java.util.*;

/**
 * Computes FIRST and FOLLOW sets for a given grammar.
 * CS4031 - Compiler Construction, Assignment 02
 */
public class FirstFollow {

    private final Grammar grammar;
    private final Map<String, Set<String>> firstSets  = new LinkedHashMap<>();
    private final Map<String, Set<String>> followSets = new LinkedHashMap<>();

    public FirstFollow(Grammar grammar) {
        this.grammar = grammar;
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  Public API
    // ══════════════════════════════════════════════════════════════════════════

    public void compute() {
        computeFirst();
        computeFollow();
    }

    public Set<String> getFirst(String symbol) {
        return firstSets.getOrDefault(symbol, new LinkedHashSet<>());
    }

    public Map<String, Set<String>> getFirstSets()  { return firstSets; }
    public Map<String, Set<String>> getFollowSets() { return followSets; }

    public Set<String> getFollow(String nonTerminal) {
        return followSets.getOrDefault(nonTerminal, new LinkedHashSet<>());
    }

    /**
     * Compute FIRST of a sequence of symbols.
     * Used during parsing table construction.
     */
    public Set<String> firstOfSequence(List<String> symbols) {
        Set<String> result = new LinkedHashSet<>();

        if (symbols == null || symbols.isEmpty()) {
            result.add(Grammar.EPSILON);
            return result;
        }

        // Single epsilon symbol
        if (symbols.size() == 1 && symbols.get(0).equals(Grammar.EPSILON)) {
            result.add(Grammar.EPSILON);
            return result;
        }

        boolean allEpsilon = true;

        for (String sym : symbols) {
            if (sym.equals(Grammar.EPSILON)) {
                // epsilon in the middle: contributes nothing, but doesn't stop the chain
                continue;
            }

            Set<String> f = firstOf(sym);

            // Add everything except epsilon to result
            for (String s : f) {
                if (!s.equals(Grammar.EPSILON)) result.add(s);
            }

            if (!f.contains(Grammar.EPSILON)) {
                allEpsilon = false;
                break;
            }
        }

        if (allEpsilon) result.add(Grammar.EPSILON);
        return result;
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  FIRST Set Computation
    // ══════════════════════════════════════════════════════════════════════════

    private void computeFirst() {
        // Initialise empty sets for every non-terminal
        for (String nt : grammar.getNonTerminals()) {
            firstSets.put(nt, new LinkedHashSet<>());
        }

        // Fixed-point iteration
        boolean changed = true;
        while (changed) {
            changed = false;
            for (String nt : grammar.getNonTerminals()) {
                for (List<String> alt : grammar.getProductions(nt)) {
                    Set<String> before = new LinkedHashSet<>(firstSets.get(nt));
                    addFirstOfProduction(firstSets.get(nt), alt);
                    if (!firstSets.get(nt).equals(before)) changed = true;
                }
            }
        }
    }

    /**
     * Add FIRST(production) into target set.
     */
    private void addFirstOfProduction(Set<String> target, List<String> production) {
        if (production == null || production.isEmpty()) {
            target.add(Grammar.EPSILON);
            return;
        }

        // Production is just "epsilon"
        if (production.size() == 1 && production.get(0).equals(Grammar.EPSILON)) {
            target.add(Grammar.EPSILON);
            return;
        }

        boolean allCanBeEpsilon = true;
        for (String sym : production) {
            if (sym.equals(Grammar.EPSILON)) continue;

            Set<String> f = firstOf(sym);

            for (String s : f) {
                if (!s.equals(Grammar.EPSILON)) target.add(s);
            }

            if (!f.contains(Grammar.EPSILON)) {
                allCanBeEpsilon = false;
                break;
            }
        }

        if (allCanBeEpsilon) target.add(Grammar.EPSILON);
    }

    /**
     * FIRST of a single symbol.
     */
    private Set<String> firstOf(String sym) {
        if (sym == null) return new LinkedHashSet<>();

        if (sym.equals(Grammar.EPSILON)) {
            Set<String> s = new LinkedHashSet<>();
            s.add(Grammar.EPSILON);
            return s;
        }

        if (grammar.isTerminal(sym)) {
            Set<String> s = new LinkedHashSet<>();
            s.add(sym);
            return s;
        }

        // Non-terminal: return current computed set (may be partial during iteration)
        return firstSets.getOrDefault(sym, new LinkedHashSet<>());
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  FOLLOW Set Computation
    // ══════════════════════════════════════════════════════════════════════════

    private void computeFollow() {
        // Initialise empty sets
        for (String nt : grammar.getNonTerminals()) {
            followSets.put(nt, new LinkedHashSet<>());
        }

        // Rule 1: $ in FOLLOW(start symbol)
        followSets.get(grammar.getStartSymbol()).add(Grammar.END_MARKER);

        // Fixed-point iteration
        boolean changed = true;
        while (changed) {
            changed = false;
            for (String lhs : grammar.getNonTerminals()) {
                for (List<String> alt : grammar.getProductions(lhs)) {
                    for (int i = 0; i < alt.size(); i++) {
                        String sym = alt.get(i);
                        if (!grammar.isNonTerminal(sym)) continue;

                        Set<String> followSym = followSets.get(sym);
                        int prevSize = followSym.size();

                        // beta = remainder of production after position i
                        List<String> beta = new ArrayList<>();
                        for (int k = i + 1; k < alt.size(); k++) {
                            beta.add(alt.get(k));
                        }

                        // Compute FIRST(beta)
                        Set<String> firstBeta = firstOfSequence(beta);

                        // Add FIRST(beta) − {epsilon} to FOLLOW(sym)
                        for (String t : firstBeta) {
                            if (!t.equals(Grammar.EPSILON)) followSym.add(t);
                        }

                        // If epsilon in FIRST(beta), add FOLLOW(lhs) to FOLLOW(sym)
                        if (firstBeta.contains(Grammar.EPSILON)) {
                            Set<String> followLhs = followSets.get(lhs);
                            if (followLhs != null) {
                                followSym.addAll(followLhs);
                            }
                        }

                        if (followSym.size() != prevSize) changed = true;
                    }
                }
            }
        }
    }

    /**
     * Returns true if the sequence of symbols can derive epsilon.
     */
    public boolean canDeriveEpsilon(List<String> symbols) {
        if (symbols == null || symbols.isEmpty()) return true;
        for (String sym : symbols) {
            if (sym.equals(Grammar.EPSILON)) continue;
            if (grammar.isTerminal(sym)) return false;
            if (!firstSets.getOrDefault(sym, Collections.emptySet())
                    .contains(Grammar.EPSILON)) return false;
        }
        return true;
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  Display / Output
    // ══════════════════════════════════════════════════════════════════════════

    public void display() {
        System.out.println();
        System.out.println("┌─ FIRST Sets ──────────────────────────────────────────┐");
        System.out.printf("  %-20s │ %s%n", "Non-Terminal", "FIRST");
        System.out.println("  " + "─".repeat(20) + "┼" + "─".repeat(32));
        for (String nt : grammar.getNonTerminals()) {
            Set<String> first = firstSets.getOrDefault(nt, new LinkedHashSet<>());
            System.out.printf("  %-20s │ { %s }%n", nt, String.join(", ", first));
        }
        System.out.println("└" + "─".repeat(55) + "┘");

        System.out.println();
        System.out.println("┌─ FOLLOW Sets ─────────────────────────────────────────┐");
        System.out.printf("  %-20s │ %s%n", "Non-Terminal", "FOLLOW");
        System.out.println("  " + "─".repeat(20) + "┼" + "─".repeat(32));
        for (String nt : grammar.getNonTerminals()) {
            Set<String> follow = followSets.getOrDefault(nt, new LinkedHashSet<>());
            System.out.printf("  %-20s │ { %s }%n", nt, String.join(", ", follow));
        }
        System.out.println("└" + "─".repeat(55) + "┘");
    }

    public void writeToFile(PrintWriter pw) {
        pw.println("=== FIRST Sets ===");
        for (String nt : grammar.getNonTerminals()) {
            pw.printf("FIRST(%-15s) = { %s }%n", nt,
                    String.join(", ", firstSets.getOrDefault(nt, new LinkedHashSet<>())));
        }
        pw.println();
        pw.println("=== FOLLOW Sets ===");
        for (String nt : grammar.getNonTerminals()) {
            pw.printf("FOLLOW(%-14s) = { %s }%n", nt,
                    String.join(", ", followSets.getOrDefault(nt, new LinkedHashSet<>())));
        }
    }
}
