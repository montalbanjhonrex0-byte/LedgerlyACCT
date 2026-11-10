import java.util.*;

/**
 * TASK 3 - Journal Account Catalog Mapping
 * Keeps the standard account categories on the backend:
 * Assets, Liabilities, Equity, Revenue, Expenses.
 * Also knows some common account titles and which category each one belongs to.
 */
public final class AccountCatalog {
    public static final String ASSETS = "Assets";
    public static final String LIABILITIES = "Liabilities";
    public static final String EQUITY = "Equity";
    public static final String REVENUE = "Revenue";      // same word the rest of the app already uses
    public static final String EXPENSES = "Expenses";

    public static final List<String> CATEGORIES = Collections.unmodifiableList(
            Arrays.asList(ASSETS, LIABILITIES, EQUITY, REVENUE, EXPENSES));

    // common account title -> category
    private static final Map<String, String> STANDARD_ACCOUNTS = new LinkedHashMap<>();
    static {
        STANDARD_ACCOUNTS.put("Cash", ASSETS);
        STANDARD_ACCOUNTS.put("Accounts Receivable", ASSETS);
        STANDARD_ACCOUNTS.put("Supplies", ASSETS);
        STANDARD_ACCOUNTS.put("Equipment", ASSETS);
        STANDARD_ACCOUNTS.put("Accounts Payable", LIABILITIES);
        STANDARD_ACCOUNTS.put("Notes Payable", LIABILITIES);
        STANDARD_ACCOUNTS.put("Owner's Capital", EQUITY);
        STANDARD_ACCOUNTS.put("Service Revenue", REVENUE);
        STANDARD_ACCOUNTS.put("Salaries Expense", EXPENSES);
        STANDARD_ACCOUNTS.put("Rent Expense", EXPENSES);
        STANDARD_ACCOUNTS.put("Utilities Expense", EXPENSES);
        STANDARD_ACCOUNTS.put("Supplies Expense", EXPENSES);
    }

    private AccountCatalog() {}

    /** Turns text like "assets", "Revenues", "expense" into the standard name. Returns null if unknown. */
    public static String normalizeCategory(String raw) {
        if (raw == null) return null;
        switch (raw.trim().toLowerCase(Locale.ROOT)) {
            case "asset": case "assets": return ASSETS;
            case "liability": case "liabilities": return LIABILITIES;
            case "equity": return EQUITY;
            case "revenue": case "revenues": return REVENUE;
            case "expense": case "expenses": return EXPENSES;
            default: return null;
        }
    }

    /** Category of a standard account title (ignores upper/lower case). Returns null if the title is not in the catalog. */
    public static String categoryOfAccount(String title) {
        String key = standardTitle(title);
        return key == null ? null : STANDARD_ACCOUNTS.get(key);
    }

    /** Returns the standard spelling of a title (e.g. "cash" -> "Cash"), or null if it is not a standard account. */
    public static String standardTitle(String title) {
        if (title == null) return null;
        String t = title.trim();
        for (String known : STANDARD_ACCOUNTS.keySet()) {
            if (known.equalsIgnoreCase(t)) return known;
        }
        return null;
    }

    /** "Debit" for Assets and Expenses, "Credit" for the others. */
    public static String normalBalance(String category) {
        String c = normalizeCategory(category);
        if (c == null) return null;
        return (c.equals(ASSETS) || c.equals(EXPENSES)) ? "Debit" : "Credit";
    }

    public static Map<String, String> standardAccounts() {
        return Collections.unmodifiableMap(STANDARD_ACCOUNTS);
    }

    /** Uses the category the user gave; if none was given, uses the catalog. Null if it cannot be found. */
    public static String resolve(String title, String givenCategory) {
        String c = normalizeCategory(givenCategory);
        return c != null ? c : categoryOfAccount(title);
    }
}
