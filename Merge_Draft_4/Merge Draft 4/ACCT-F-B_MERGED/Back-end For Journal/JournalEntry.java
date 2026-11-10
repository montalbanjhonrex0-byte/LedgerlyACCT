import java.util.*;

public class JournalEntry {
    public String id, date, description;
    public final List<Entry> lines = new ArrayList<>();

    public JournalEntry(String date, String description) {
        this(null, date, description);
    }

    public JournalEntry(String id, String date, String description) {
        this.id = id; this.date = date; this.description = description;
    }

    public JournalEntry addLine(String account, String category, String side, double amount) {
        lines.add(new Entry(date, account, description, category, side, amount));
        return this;
    }

    public double totalDebits() { return sideTotalCents("Debit") / 100.0; }
    public double totalCredits() { return sideTotalCents("Credit") / 100.0; }

    private long sideTotalCents(String side) {
        long cents = 0;
        for (Entry e : lines) {
            if (e != null && e.entrySide != null && e.entrySide.trim().equalsIgnoreCase(side)) {
                cents += JournalValidator.toCents(e.amount);
            }
        }
        return cents;
    }
}