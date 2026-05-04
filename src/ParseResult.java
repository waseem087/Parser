import java.util.*;

/**
 * Holds the complete result of parsing one input string:
 *  - step-by-step trace rows
 *  - whether the string was accepted
 *  - parse tree (if accepted)
 *  - error list
 */
public class ParseResult {

    /** One row in the parsing trace table. */
    public static class TraceRow {
        public final int    step;
        public final String stack;
        public final String input;
        public final String action;

        public TraceRow(int step, String stack, String input, String action) {
            this.step   = step;
            this.stack  = stack;
            this.input  = input;
            this.action = action;
        }
    }

    private final List<TraceRow>   trace      = new ArrayList<>();
    private boolean                accepted   = false;
    private ParseTree              parseTree  = null;
    private final ErrorHandler     errorHandler;

    public ParseResult(ErrorHandler errorHandler) {
        this.errorHandler = errorHandler;
    }

    public void addRow(int step, String stack, String input, String action) {
        trace.add(new TraceRow(step, stack, input, action));
    }

    public void setAccepted(boolean accepted) { this.accepted = accepted; }
    public void setParseTree(ParseTree tree)  { this.parseTree = tree; }

    public boolean   isAccepted()   { return accepted; }
    public ParseTree getParseTree() { return parseTree; }

    // ── Display ──────────────────────────────────────────────────────────────

    private static final int SW = 5, SKW = 35, IW = 30, AW = 45;

    public void printTrace() {
        System.out.println();
        System.out.printf("  %-" + SW + "s │ %-" + SKW + "s │ %-" + IW + "s │ %s%n",
                "Step", "Stack (bottom→top)", "Input", "Action");
        System.out.println("  " + "─".repeat(SW + SKW + IW + AW + 9));
        for (TraceRow row : trace) {
            // Break long lines
            String stack  = truncate(row.stack,  SKW);
            String input  = truncate(row.input,  IW);
            String action = truncate(row.action, AW);
            System.out.printf("  %-" + SW + "d │ %-" + SKW + "s │ %-" + IW + "s │ %s%n",
                    row.step, stack, input, action);
        }
        System.out.println();
        if (accepted) {
            System.out.println("  ✔ String ACCEPTED");
        } else {
            System.out.println("  ✘ String REJECTED  ("
                    + errorHandler.errorCount() + " error(s) found)");
        }
    }

    public String getTraceString() {
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("%-5s | %-35s | %-30s | %s%n",
                "Step", "Stack", "Input", "Action"));
        sb.append("-".repeat(120)).append("\n");
        for (TraceRow row : trace) {
            sb.append(String.format("%-5d | %-35s | %-30s | %s%n",
                    row.step, row.stack, row.input, row.action));
        }
        sb.append(accepted ? "ACCEPTED" : "REJECTED").append("\n");
        return sb.toString();
    }

    private String truncate(String s, int max) {
        if (s == null) return "";
        return s.length() <= max ? s : s.substring(0, max - 1) + "…";
    }
}
