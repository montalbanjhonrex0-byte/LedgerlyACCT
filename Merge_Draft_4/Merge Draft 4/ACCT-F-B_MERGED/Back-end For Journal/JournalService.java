import java.util.*;

public class JournalService {
    public static class Result {
        public final JournalEntry entry;
        public final List<String> errors;
        public final boolean notFound;

        private Result(JournalEntry entry, List<String> errors, boolean notFound) {
            this.entry = entry; this.errors = errors; this.notFound = notFound;
        }
        public boolean ok() { return errors.isEmpty(); }
        static Result saved(JournalEntry e) { return new Result(e, Collections.<String>emptyList(), false); }
        static Result failed(List<String> errors) { return new Result(null, errors, false); }
        static Result missing(String id) {
            return new Result(null, Collections.singletonList("Journal entry " + id + " was not found."), true);
        }
    }

    private final Map<String, List<JournalEntry>> store = new HashMap<>();
    private final Map<String, JournalIdGenerator> idGenerators = new HashMap<>();

    private static String key(String user) {
        return user == null ? "" : user.trim().toLowerCase(Locale.ROOT);
    }
    private List<JournalEntry> entriesOf(String user) {
        return store.computeIfAbsent(key(user), k -> new ArrayList<>());
    }
    private JournalIdGenerator idsOf(String user) {
        return idGenerators.computeIfAbsent(key(user), k -> new JournalIdGenerator());
    }

    public synchronized Result create(String user, JournalEntry draft) {
        List<String> errors = JournalValidator.validate(draft);
        if (!errors.isEmpty()) return Result.failed(errors);
        JournalEntry saved = build(idsOf(user).next(), draft);
        entriesOf(user).add(saved);
        return Result.saved(saved);
    }

    public synchronized Result update(String user, String id, JournalEntry draft) {
        List<JournalEntry> list = entriesOf(user);
        int index = indexOf(list, id);
        if (index < 0) return Result.missing(id);
        List<String> errors = JournalValidator.validate(draft);
        if (!errors.isEmpty()) return Result.failed(errors);
        JournalEntry saved = build(list.get(index).id, draft);
        list.set(index, saved);
        return Result.saved(saved);
    }

    public synchronized List<JournalEntry> list(String user) {
        return new ArrayList<>(entriesOf(user));
    }

    public synchronized JournalEntry find(String user, String id) {
        List<JournalEntry> list = entriesOf(user);
        int index = indexOf(list, id);
        return index < 0 ? null : list.get(index);
    }

    public synchronized List<Entry> lines(String user) {
        List<Entry> all = new ArrayList<>();
        for (JournalEntry je : entriesOf(user)) all.addAll(je.lines);
        return all;
    }

    // ===== START MERGED CODE (NEW ADDITION) =====
    /**
     * Loads a journal entry that was saved earlier (for example in a workbook file).
     * It keeps its own ID, and new IDs continue after the biggest JE-### number.
     * Nothing is changed if an entry with the same ID is already loaded.
     */
    public synchronized void importSaved(String user, JournalEntry saved) {
        if (saved == null || saved.id == null) return;
        if (find(user, saved.id) != null) return;
        entriesOf(user).add(saved);
        idsOf(user).resumeFrom(Collections.singletonList(saved.id));
    }
    // ===== END MERGED CODE (NEW ADDITION) =====

    // ===== START MERGED CODE (NEW ADDITION) =====
    /** Removes one journal entry. Returns true if it existed. (Its JE number is not reused.) */
    public synchronized boolean delete(String user, String id) {
        List<JournalEntry> list = entriesOf(user);
        int index = indexOf(list, id);
        if (index < 0) return false;
        list.remove(index);
        return true;
    }

    private static int indexOf(List<JournalEntry> list, String id) {
        if (id == null) return -1;
        for (int i = 0; i < list.size(); i++) {
            if (list.get(i).id.equalsIgnoreCase(id.trim())) return i;
        }
        return -1;
    }

    private static JournalEntry build(String id, JournalEntry draft) {
        JournalEntry saved = new JournalEntry(id, draft.date.trim(), draft.description.trim());
        for (Entry l : draft.lines) {
            String title = l.account.trim();
            String standard = AccountCatalog.standardTitle(title);
            String side = l.entrySide.trim().equalsIgnoreCase("Debit") ? "Debit" : "Credit";
            double amount = JournalValidator.toCents(l.amount) / 100.0;

            // Uses JournalEntry's addLine method or instantiates Entry using 
            // the constructor signature matching your project's Entry class.
            saved.addLine(
                    standard != null ? standard : title,
                    AccountCatalog.resolve(title, l.type),
                    side,
                    amount
            );
        }
        return saved;
    }
}