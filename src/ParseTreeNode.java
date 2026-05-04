import java.util.*;

/**
 * A single node in the parse tree.
 */
public class ParseTreeNode {

    private final String label;                    // symbol this node represents
    private final List<ParseTreeNode> children = new ArrayList<>();
    private ParseTreeNode parent;

    public ParseTreeNode(String label) {
        this.label = label;
    }

    public void addChild(ParseTreeNode child) {
        child.parent = this;
        children.add(child);
    }

    public String getLabel()                   { return label; }
    public List<ParseTreeNode> getChildren()   { return children; }
    public ParseTreeNode getParent()           { return parent; }
    public boolean isLeaf()                    { return children.isEmpty(); }

    // ── Pretty Printing ─────────────────────────────────────────────────────

    /** Print the tree with indented ASCII art. */
    public void display() {
        display("", true);
    }

    private void display(String prefix, boolean isTail) {
        System.out.println(prefix + (isTail ? "└── " : "├── ") + label);
        for (int i = 0; i < children.size(); i++) {
            children.get(i).display(prefix + (isTail ? "    " : "│   "),
                    i == children.size() - 1);
        }
    }

    /** Return the tree as indented text. */
    public String toTextFormat() {
        StringBuilder sb = new StringBuilder();
        buildText(sb, "", true);
        return sb.toString();
    }

    private void buildText(StringBuilder sb, String prefix, boolean isTail) {
        sb.append(prefix).append(isTail ? "└── " : "├── ").append(label).append("\n");
        for (int i = 0; i < children.size(); i++) {
            children.get(i).buildText(sb, prefix + (isTail ? "    " : "│   "),
                    i == children.size() - 1);
        }
    }

    /** Pre-order traversal (yields labels). */
    public List<String> preorder() {
        List<String> result = new ArrayList<>();
        preorderHelper(result);
        return result;
    }

    private void preorderHelper(List<String> result) {
        result.add(label);
        for (ParseTreeNode child : children) child.preorderHelper(result);
    }
}
