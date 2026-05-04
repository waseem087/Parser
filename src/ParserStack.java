import java.util.*;

/**
 * Generic stack used by the LL(1) parser.
 * Wraps a Deque internally.
 */
public class ParserStack<T> {

    private final Deque<T> deque = new ArrayDeque<>();

    public void push(T item) { deque.push(item); }

    public T pop() {
        if (deque.isEmpty()) throw new EmptyStackException();
        return deque.pop();
    }

    public T peek() {
        if (deque.isEmpty()) throw new EmptyStackException();
        return deque.peek();
    }

    public boolean isEmpty() { return deque.isEmpty(); }

    public int size() { return deque.size(); }

    /** Returns the stack as a list from bottom to top (for display). */
    public List<T> toListBottomUp() {
        List<T> list = new ArrayList<>(deque);
        Collections.reverse(list);
        return list;
    }

    /** Display string: $ Sym1 Sym2 (bottom to top) */
    public String toDisplayString() {
        List<T> list = toListBottomUp();
        StringBuilder sb = new StringBuilder();
        for (T item : list) {
            if (sb.length() > 0) sb.append(" ");
            sb.append(item);
        }
        return sb.toString();
    }

    @Override
    public String toString() { return toDisplayString(); }
}
