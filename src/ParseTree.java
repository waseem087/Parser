/**
 * Wrapper around the root ParseTreeNode.
 */
public class ParseTree {

    private final ParseTreeNode root;

    public ParseTree(ParseTreeNode root) {
        this.root = root;
    }

    public ParseTreeNode getRoot() { return root; }

    public void display() {
        if (root == null) {
            System.out.println("  (no tree)");
            return;
        }
        root.display();
    }

    public String toTextFormat() {
        if (root == null) return "(no tree)";
        return root.toTextFormat();
    }
}
