import java.io.*;
import java.util.*;

/**
 * Main entry point for the LL(1) Parser.
 * CS4031 - Compiler Construction, Assignment 02
 */
public class Main {

    public static void main(String[] args) {
        System.out.println("╔══════════════════════════════════════════════════════╗");
        System.out.println("║         LL(1) Predictive Parser - CS4031             ║");
        System.out.println("╚══════════════════════════════════════════════════════╝");
        System.out.println();

        // Default file paths (can be overridden via args)
        String grammarFile  = "input/grammar1.txt";
        String inputFile    = "input/grammar1_valid.txt";
        String outputDir    = "output/";

        if (args.length >= 2) {
            grammarFile = args[0];
            inputFile   = args[1];
        }
        if (args.length >= 3) {
            outputDir = args[2];
        }

        // Ensure output directory exists
        new File(outputDir).mkdirs();

        try {
            runParser(grammarFile, inputFile, outputDir);
        } catch (IOException e) {
            System.err.println("[ERROR] File I/O error: " + e.getMessage());
            System.err.println("  Working directory: " + new File(".").getAbsolutePath());
            System.err.println("  Ensure input/ folder exists inside your Eclipse project root.");
            e.printStackTrace();
        } catch (Exception e) {
            System.err.println("[ERROR] Unexpected error: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Orchestrates the full LL(1) pipeline for one grammar + input file pair.
     */
    public static void runParser(String grammarFile, String inputFile, String outputDir)
            throws IOException {

        String grammarBase = new File(grammarFile).getName().replaceFirst("[.][^.]+$", "");

        // ── Step 1: Read grammar ──────────────────────────────────────────────
        System.out.println("━━━ Step 1: Reading Grammar from: " + grammarFile + " ━━━");
        Grammar grammar = new Grammar();
        grammar.readFromFile(grammarFile);
        grammar.display("Original Grammar");

        // ── Step 2: Left Factoring ────────────────────────────────────────────
        System.out.println("\n━━━ Step 2: Left Factoring ━━━");
        grammar.applyLeftFactoring();
        grammar.display("After Left Factoring");

        // ── Step 3: Left Recursion Removal ────────────────────────────────────
        System.out.println("\n━━━ Step 3: Left Recursion Removal ━━━");
        grammar.removeLeftRecursion();
        grammar.display("After Left Recursion Removal");

        // ── Step 4 & 5: FIRST and FOLLOW sets ────────────────────────────────
        System.out.println("\n━━━ Step 4 & 5: Computing FIRST and FOLLOW Sets ━━━");
        FirstFollow ff = new FirstFollow(grammar);
        ff.compute();
        ff.display();

        // ── Step 6: LL(1) Parsing Table ───────────────────────────────────────
        System.out.println("\n━━━ Step 6: Constructing LL(1) Parsing Table ━━━");
        ParsingTable table = new ParsingTable(grammar, ff);
        table.construct();
        table.display();

        // ── Write grammar/table output files ─────────────────────────────────
        try (PrintWriter pw = new PrintWriter(outputDir + grammarBase + "_transformed.txt")) {
            grammar.writeToFile(pw, ff, table);
        }

        // ── Step 7: Parse input strings ───────────────────────────────────────
        System.out.println("\n━━━ Step 7: Parsing Input Strings from: " + inputFile + " ━━━");
        List<String> inputStrings = readInputStrings(inputFile);

        StringBuilder traceOutput = new StringBuilder();
        StringBuilder treeOutput  = new StringBuilder();

        for (int i = 0; i < inputStrings.size(); i++) {
            String input = inputStrings.get(i).trim();
            if (input.isEmpty()) continue;

            System.out.println("\n─── Input String " + (i + 1) + ": " + input + " ───");
            Parser parser = new Parser(grammar, table, ff);
            ParseResult result = parser.parse(input);

            result.printTrace();
            traceOutput.append("=== Input ").append(i + 1).append(": ").append(input).append(" ===\n");
            traceOutput.append(result.getTraceString()).append("\n\n");

            if (result.isAccepted()) {
                System.out.println("\n── Parse Tree ──");
                result.getParseTree().display();
                treeOutput.append("=== Parse Tree for: ").append(input).append(" ===\n");
                treeOutput.append(result.getParseTree().toTextFormat()).append("\n\n");
            }
        }

        // ── Write parsing output files ────────────────────────────────────────
        try (PrintWriter pw = new PrintWriter(outputDir + grammarBase + "_trace.txt")) {
            pw.print(traceOutput);
        }
        try (PrintWriter pw = new PrintWriter(outputDir + grammarBase + "_trees.txt")) {
            pw.print(treeOutput);
        }

        System.out.println("\n✔ Output written to: " + outputDir);
    }

    /** Read non-empty lines from an input file. */
    private static List<String> readInputStrings(String filename) throws IOException {
        List<String> lines = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(new FileReader(filename))) {
            String line;
            while ((line = br.readLine()) != null) {
                if (!line.trim().isEmpty() && !line.trim().startsWith("#")) {
                    lines.add(line.trim());
                }
            }
        }
        return lines;
    }
}
