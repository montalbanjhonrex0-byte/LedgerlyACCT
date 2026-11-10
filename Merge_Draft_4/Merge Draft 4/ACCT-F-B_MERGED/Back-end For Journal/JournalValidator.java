import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.*;

/**
 * TASK 2 - Double-Entry Validation Logic Engine
 * Checks a journal entry before it is saved.
 * Main rule: Total Debits must equal Total Credits, or the entry is rejected.
 * Money is compared in cents (whole numbers) so 0.1 + 0.2 equals 0.3.
 */
public final class JournalValidator {
    private JournalValidator() {}

    public static long toCents(double amount) {
        return Math.round(amount * 100);
    }

    /** Returns a list of problems. An empty list means the entry is valid. */
    public static List<String> validate(JournalEntry entry) {
        List<String> errors = new ArrayList<>();
        if (entry == null) { errors.add("Journal entry is missing."); return errors; }

        String date = entry.date == null ? "" : entry.date.trim();
        if (date.isEmpty()) {
            errors.add("Date is required (YYYY-MM-DD).");
        } else {
            try { LocalDate.parse(date); }
            catch (DateTimeParseException ex) { errors.add("Date must be a real date in YYYY-MM-DD format."); }
        }
        if (entry.description == null || entry.description.trim().isEmpty()) {
            errors.add("Description is required.");
        }
        if (entry.lines == null || entry.lines.size() < 2) {
            errors.add("A journal entry needs at least 2 lines (one Debit and one Credit).");
            return errors;
        }

        long debitCents = 0, creditCents = 0;
        boolean hasDebit = false, hasCredit = false, linesOk = true;

        for (int i = 0; i < entry.lines.size(); i++) {
            Entry line = entry.lines.get(i);
            String where = "Line " + (i + 1) + ": ";
            if (line == null) { errors.add(where + "is empty."); linesOk = false; continue; }

            // account title
            String title = line.account == null ? "" : line.account.trim();
            if (title.isEmpty()) { errors.add(where + "Account Title is required."); linesOk = false; }

            // category (uses the catalog)
            String given = line.type == null ? "" : line.type.trim();
            String known = AccountCatalog.categoryOfAccount(title);
            if (!given.isEmpty()) {
                String category = AccountCatalog.normalizeCategory(given);
                if (category == null) {
                    errors.add(where + "Category \"" + given + "\" is not valid. Use Assets, Liabilities, Equity, Revenue or Expenses.");
                    linesOk = false;
                } else if (known != null && !known.equals(category)) {
                    errors.add(where + "\"" + AccountCatalog.standardTitle(title) + "\" is a " + known + " account, not " + category + ".");
                    linesOk = false;
                }
            } else if (known == null) {
                errors.add(where + "Choose a category for \"" + title + "\" (Assets, Liabilities, Equity, Revenue or Expenses).");
                linesOk = false;
            }

            // side
            String side = line.entrySide == null ? "" : line.entrySide.trim();
            boolean isDebit = side.equalsIgnoreCase("Debit"), isCredit = side.equalsIgnoreCase("Credit");
            if (!isDebit && !isCredit) { errors.add(where + "Side must be Debit or Credit."); linesOk = false; }

            // amount
            double a = line.amount;
            if (Double.isNaN(a) || Double.isInfinite(a) || a <= 0) {
                errors.add(where + "Amount must be a number greater than zero.");
                linesOk = false;
            } else if (Math.abs(a * 100 - Math.round(a * 100)) > 1e-6) {
                errors.add(where + "Amount can have at most 2 decimal places.");
                linesOk = false;
            } else if (isDebit) { debitCents += toCents(a); hasDebit = true; }
            else if (isCredit) { creditCents += toCents(a); hasCredit = true; }
        }

        // The double-entry rule. Only checked when every line is readable.
        if (linesOk) {
            if (!hasDebit) errors.add("The entry needs at least one Debit line.");
            if (!hasCredit) errors.add("The entry needs at least one Credit line.");
            if (debitCents != creditCents) {
                errors.add("Not balanced: Total Debits " + money(debitCents)
                        + " do not equal Total Credits " + money(creditCents) + ".");
            }
        }
        return errors;
    }

    public static boolean isBalanced(JournalEntry entry) {
        return validate(entry).isEmpty();
    }

    static String money(long cents) {
        return String.format("%,.2f", cents / 100.0);
    }
}
