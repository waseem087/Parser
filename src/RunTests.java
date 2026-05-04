/**
 * RunTests.java
 * Convenience wrapper to run all four grammars in sequence.
 * Usage: java -cp out RunTests
 */
public class RunTests {

    public static void main(String[] args) throws Exception {

        String[][] grammars = {
            { "input/grammar1.txt", "input/grammar1_valid.txt",  "output/" },
            { "input/grammar1.txt", "input/grammar1_errors.txt", "output/" },
            { "input/grammar2.txt", "input/grammar2_valid.txt",  "output/" },
            { "input/grammar2.txt", "input/grammar2_errors.txt", "output/" },
            { "input/grammar2.txt", "input/grammar2_edge.txt",   "output/" },
            { "input/grammar3.txt", "input/grammar3_valid.txt",  "output/" },
            { "input/grammar3.txt", "input/grammar3_errors.txt", "output/" },
            { "input/grammar4.txt", "input/grammar4_valid.txt",  "output/" },
            { "input/grammar4.txt", "input/grammar4_errors.txt", "output/" },
        };

        for (String[] run : grammars) {
            System.out.println("\n" + "═".repeat(70));
            System.out.println("GRAMMAR: " + run[0] + "   INPUT: " + run[1]);
            System.out.println("═".repeat(70));
            try {
                Main.runParser(run[0], run[1], run[2]);
            } catch (Exception e) {
                System.err.println("ERROR during run: " + e.getMessage());
                e.printStackTrace();
            }
        }

        System.out.println("\n" + "═".repeat(70));
        System.out.println("All test runs complete. Check output/ for generated files.");
        System.out.println("═".repeat(70));
    }
}
