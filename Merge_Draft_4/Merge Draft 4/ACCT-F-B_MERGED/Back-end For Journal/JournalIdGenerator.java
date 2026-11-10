import java.util.Collection;

/**
 * TASK 4 - Transaction Numbering & ID Generation
 * Gives every saved journal entry a unique ID in order: JE-001, JE-002, JE-003 ...
 * After JE-999 it keeps going (JE-1000, JE-1001 ...).
 */
public class JournalIdGenerator {
    private int last = 0;

    /** Gives the next ID. Safe to call from many threads. */
    public synchronized String next() {
        last++;
        return format(last);
    }

    /** Builds an ID from a number: 1 -> "JE-001". */
    public static String format(int number) {
        return String.format("JE-%03d", number);
    }

    /** Reads the number from an ID: "JE-007" -> 7. Returns -1 if the ID is not in this format. */
    public static int parse(String id) {
        if (id == null || !id.matches("(?i)JE-\\d+")) return -1;
        try { return Integer.parseInt(id.substring(3)); } catch (NumberFormatException e) { return -1; }
    }

    /** Continue after the biggest ID already used (useful when old entries are loaded). */
    public synchronized void resumeFrom(Collection<String> existingIds) {
        for (String id : existingIds) last = Math.max(last, parse(id));
    }
}
