import java.util.*;

/**
 * Handles parse errors with panic-mode recovery.
 * Records all errors encountered during a parse.
 */
public class ErrorHandler {

    public enum ErrorType {
        UNEXPECTED_TOKEN,    // terminal on stack doesn't match input
        EMPTY_TABLE_ENTRY,   // M[A, a] is empty
        PREMATURE_END,       // input ends while stack not empty
        UNEXPECTED_END       // stack not empty, no more input
    }

    /** Represents a single parse error. */
    public static class ParseError {
        public final ErrorType type;
        public final String    message;
        public final int       step;
        public final String    stackTop;
        public final String    inputToken;

        public ParseError(ErrorType type, String message, int step,
                          String stackTop, String inputToken) {
            this.type       = type;
            this.message    = message;
            this.step       = step;
            this.stackTop   = stackTop;
            this.inputToken = inputToken;
        }

        @Override
        public String toString() {
            return String.format("[ERROR at step %d] %s  (stack-top='%s', input='%s')",
                    step, message, stackTop, inputToken);
        }
    }

    private final List<ParseError> errors = new ArrayList<>();

    public void reportError(ErrorType type, String message, int step,
                            String stackTop, String inputToken) {
        ParseError e = new ParseError(type, message, step, stackTop, inputToken);
        errors.add(e);
        System.out.printf("  %-70s%n", e.toString());
    }

    public boolean hasErrors()        { return !errors.isEmpty(); }
    public List<ParseError> getErrors() { return errors; }
    public int errorCount()           { return errors.size(); }

    /**
     * Panic-mode recovery:
     * Skip input tokens until one is found in the synchronisation set.
     * Returns the new input position after recovery.
     *
     * @param tokens      full token list
     * @param pos         current position in tokens
     * @param syncSet     set of terminals to synchronise on (FOLLOW of current NT)
     * @return new position (pointing to a sync token, or past end)
     */
    public int panicModeSkipInput(List<String> tokens, int pos, Set<String> syncSet) {
        System.out.println("  [RECOVERY] Panic-mode: skipping tokens until sync set "
                + syncSet + " ...");
        while (pos < tokens.size() && !syncSet.contains(tokens.get(pos))) {
            System.out.println("  [RECOVERY]   skipping token: " + tokens.get(pos));
            pos++;
        }
        if (pos < tokens.size()) {
            System.out.println("  [RECOVERY]   resuming at token: " + tokens.get(pos));
        } else {
            System.out.println("  [RECOVERY]   reached end of input.");
        }
        return pos;
    }
}
