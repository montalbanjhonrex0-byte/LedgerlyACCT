
import java.io.*;
import java.net.HttpURLConnection;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class JournalTests {

    static int passed = 0, failed = 0;

    static void check(String name, boolean condition) {
        if (condition) {
            passed++;
            System.out.println("  PASS  " + name);
        } else {
            failed++;
            System.out.println("  FAIL  " + name);
        }
    }

    static void same(String name, Object expected, Object actual) {
        boolean ok = Objects.equals(expected, actual);

        if (ok) {
            passed++;
            System.out.println("  PASS  " + name);
        } else {
            failed++;
            System.out.println("  FAIL  " + name
                    + "   expected=" + expected
                    + "  actual=" + actual);
        }
    }

    static boolean anyContains(List<String> list, String part) {
        for (String s : list) {
            if (s.contains(part)) {
                return true;
            }
        }
        return false;
    }

    static JournalEntry cashInvestment(double amount) {
        return new JournalEntry("2026-11-04", "Owner invested cash")
                .addLine("Cash", "Assets", "Debit", amount)
                .addLine("Owner's Capital", "Equity", "Credit", amount);
    }

    public static void main(String[] args) throws Exception {

        System.out.println("== Unit tests: AccountCatalog (Task 3) ==");
        catalogTests();

        System.out.println("== Unit tests: JournalIdGenerator (Task 4) ==");
        idTests();

        System.out.println("== Unit tests: JournalValidator (Task 2) ==");
        validatorTests();

        System.out.println("== Unit tests: JournalService ==");
        serviceTests();

        System.out.println("== Integration tests: journal routes (Task 1) ==");
        apiTests();

        System.out.println();
        System.out.println("================================");
        System.out.println("Passed: " + passed + "   Failed: " + failed);
        System.out.println("================================");

        if (failed > 0) {
            System.exit(1);
        }
    }

    static void catalogTests() {

        same("5 standard categories",
                5, AccountCatalog.CATEGORIES.size());

        same("'Revenues' becomes 'Revenue'",
                "Revenue", AccountCatalog.normalizeCategory("Revenues"));

        same("'assets' becomes 'Assets'",
                "Assets", AccountCatalog.normalizeCategory("assets"));

        same("'Expense' becomes 'Expenses'",
                "Expenses", AccountCatalog.normalizeCategory(" Expense "));

        check("unknown category gives null",
                AccountCatalog.normalizeCategory("Savings") == null);

        check("null category gives null",
                AccountCatalog.normalizeCategory(null) == null);

        same("Cash is an Assets account",
                "Assets", AccountCatalog.categoryOfAccount("cash"));

        same("Rent Expense is an Expenses account",
                "Expenses", AccountCatalog.categoryOfAccount("Rent Expense"));

        check("unknown account title gives null",
                AccountCatalog.categoryOfAccount("Bitcoin") == null);

        same("standard spelling of cash",
                "Cash", AccountCatalog.standardTitle("cash"));

        same("Assets normal balance is Debit",
                "Debit", AccountCatalog.normalBalance("Assets"));

        same("Expenses normal balance is Debit",
                "Debit", AccountCatalog.normalBalance("Expenses"));

        same("Liabilities normal balance is Credit",
                "Credit", AccountCatalog.normalBalance("Liabilities"));

        same("Equity normal balance is Credit",
                "Credit", AccountCatalog.normalBalance("Equity"));

        same("Revenue normal balance is Credit",
                "Credit", AccountCatalog.normalBalance("Revenue"));

        same("resolve uses given category first",
                "Equity", AccountCatalog.resolve("Anything", "equity"));

        same("resolve falls back to the catalog",
                "Assets", AccountCatalog.resolve("Cash", ""));
    }

    static void idTests() {

        JournalIdGenerator g = new JournalIdGenerator();

        same("first ID", "JE-001", g.next());
        same("second ID", "JE-002", g.next());
        same("third ID", "JE-003", g.next());

        same("format 10", "JE-010", JournalIdGenerator.format(10));
        same("format 999", "JE-999", JournalIdGenerator.format(999));
        same("format 1000", "JE-1000", JournalIdGenerator.format(1000));

        same("parse JE-007", 7, JournalIdGenerator.parse("JE-007"));
        same("parse bad text", -1, JournalIdGenerator.parse("ABC"));
        same("parse null", -1, JournalIdGenerator.parse(null));

        JournalIdGenerator resumed = new JournalIdGenerator();

        resumed.resumeFrom(
                Arrays.asList("JE-001", "JE-005", "JE-003")
        );

        same("resumeFrom continues after biggest ID",
                "JE-006", resumed.next());

        final JournalIdGenerator shared = new JournalIdGenerator();

        final Set<String> seen =
                Collections.synchronizedSet(new HashSet<String>());

        Thread[] threads = new Thread[8];

        for (int t = 0; t < threads.length; t++) {

            threads[t] = new Thread(() -> {
                for (int i = 0; i < 100; i++) {
                    seen.add(shared.next());
                }
            });

            threads[t].start();
        }

        for (Thread t : threads) {
            try {
                t.join();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException(e);
            }
        }

        same("800 IDs from 8 threads are all different",
                800, seen.size());
    }

    static void validatorTests() {

        check("balanced entry is valid",
                JournalValidator.validate(cashInvestment(5000)).isEmpty());

        List<String> bad = JournalValidator.validate(
                new JournalEntry("2026-11-04", "Unbalanced")
                        .addLine("Cash", "Assets", "Debit", 1000)
                        .addLine("Owner's Capital", "Equity", "Credit", 900)
        );

        check("debits 1000 vs credits 900 is rejected",
                !bad.isEmpty());

        check("error says Not balanced",
                anyContains(bad, "Not balanced"));

        check("error shows both totals",
                anyContains(bad, "1,000.00")
                        && anyContains(bad, "900.00"));

        check("compound balanced entry is valid",
                JournalValidator.validate(
                        new JournalEntry("2026-11-04", "Bought equipment, part cash")
                                .addLine("Equipment", "Assets", "Debit", 3000)
                                .addLine("Cash", "Assets", "Credit", 1000)
                                .addLine("Accounts Payable", "Liabilities", "Credit", 2000)
                ).isEmpty());

        check("0.1 + 0.2 equals 0.3",
                JournalValidator.validate(
                        new JournalEntry("2026-11-04", "Small amounts")
                                .addLine("Cash", "Assets", "Debit", 0.1)
                                .addLine("Supplies", "Assets", "Debit", 0.2)
                                .addLine("Owner's Capital", "Equity", "Credit", 0.3)
                ).isEmpty());

        check("one line only is rejected",
                !JournalValidator.validate(
                        new JournalEntry("2026-11-04", "One line")
                                .addLine("Cash", "Assets", "Debit", 100)
                ).isEmpty());

        check("all debits without credit are rejected",
                !JournalValidator.validate(
                        new JournalEntry("2026-11-04", "No credit")
                                .addLine("Cash", "Assets", "Debit", 100)
                                .addLine("Supplies", "Assets", "Debit", 100)
                ).isEmpty());

        check("zero amount is rejected",
                !JournalValidator.validate(cashInvestment(0)).isEmpty());

        check("negative amount is rejected",
                !JournalValidator.validate(cashInvestment(-50)).isEmpty());

        check("NaN amount is rejected",
                !JournalValidator.validate(cashInvestment(Double.NaN)).isEmpty());

        check("3 decimal places is rejected",
                anyContains(
                        JournalValidator.validate(cashInvestment(10.005)),
                        "2 decimal places"
                ));

        JournalEntry badDate = cashInvestment(100);
        badDate.date = "2026-13-45";

        check("fake date is rejected",
                anyContains(JournalValidator.validate(badDate), "Date"));

        JournalEntry noDate = cashInvestment(100);
        noDate.date = "  ";

        check("empty date is rejected",
                anyContains(JournalValidator.validate(noDate), "Date"));

        JournalEntry noDesc = cashInvestment(100);
        noDesc.description = "";

        check("empty description is rejected",
                anyContains(JournalValidator.validate(noDesc), "Description"));

        check("empty account title is rejected",
                anyContains(
                        JournalValidator.validate(
                                new JournalEntry("2026-11-04", "x")
                                        .addLine("", "Assets", "Debit", 10)
                                        .addLine("Owner's Capital", "Equity", "Credit", 10)
                        ),
                        "Account Title"
                ));

        check("unknown category is rejected",
                anyContains(
                        JournalValidator.validate(
                                new JournalEntry("2026-11-04", "x")
                                        .addLine("Cash", "Savings", "Debit", 10)
                                        .addLine("Owner's Capital", "Equity", "Credit", 10)
                        ),
                        "not valid"
                ));

        check("Cash in wrong category is rejected",
                anyContains(
                        JournalValidator.validate(
                                new JournalEntry("2026-11-04", "x")
                                        .addLine("Cash", "Equity", "Debit", 10)
                                        .addLine("Owner's Capital", "Equity", "Credit", 10)
                        ),
                        "is a Assets account"
                ));

        check("custom account without category is rejected",
                anyContains(
                        JournalValidator.validate(
                                new JournalEntry("2026-11-04", "x")
                                        .addLine("Petty Cash Fund", "", "Debit", 10)
                                        .addLine("Owner's Capital", "Equity", "Credit", 10)
                        ),
                        "Choose a category"
                ));

        check("known account with empty category is accepted",
                JournalValidator.validate(
                        new JournalEntry("2026-11-04", "x")
                                .addLine("Cash", "", "Debit", 10)
                                .addLine("Owner's Capital", "", "Credit", 10)
                ).isEmpty());

        check("side must be Debit or Credit",
                anyContains(
                        JournalValidator.validate(
                                new JournalEntry("2026-11-04", "x")
                                        .addLine("Cash", "Assets", "Left", 10)
                                        .addLine("Owner's Capital", "Equity", "Credit", 10)
                        ),
                        "Debit or Credit"
                ));

        check("'Revenues' is accepted as a category",
                JournalValidator.validate(
                        new JournalEntry("2026-11-04", "Service income")
                                .addLine("Cash", "Assets", "Debit", 500)
                                .addLine("Consulting", "Revenues", "Credit", 500)
                ).isEmpty());

        check("null entry is rejected",
                !JournalValidator.validate(null).isEmpty());

        check("isBalanced() true for balanced entry",
                JournalValidator.isBalanced(cashInvestment(1)));
    }

    static void serviceTests() {

        JournalService svc = new JournalService();
        String user = "a@test.com";

        JournalService.Result r1 =
                svc.create(user, cashInvestment(5000));

        check("first create works", r1.ok());

        same("first entry gets JE-001",
                "JE-001", r1.entry.id);

        same("second entry gets JE-002",
                "JE-002", svc.create(user, cashInvestment(100)).entry.id);

        JournalService.Result rejected =
                svc.create(user,
                        new JournalEntry("2026-11-04", "Bad")
                                .addLine("Cash", "Assets", "Debit", 10)
                                .addLine("Owner's Capital", "Equity", "Credit", 9));

        check("unbalanced create is rejected", !rejected.ok());

        check("rejected entry is not saved",
                svc.list(user).size() == 2);

        same("rejected entry does not use up an ID",
                "JE-003",
                svc.create(user, cashInvestment(1)).entry.id);

        JournalEntry messy =
                new JournalEntry("  2026-11-05 ", "  Paid rent  ")
                        .addLine("  cash ", "assets", "credit", 800)
                        .addLine("rent expense", "Expense", "DEBIT", 800);

        JournalEntry saved = svc.create(user, messy).entry;

        same("description is trimmed",
                "Paid rent", saved.description);

        same("account spelling uses catalog",
                "Cash", saved.lines.get(0).account);

        same("category is standard name",
                "Assets", saved.lines.get(0).type);

        same("side is standard word",
                "Credit", saved.lines.get(0).entrySide);

        same("Expense becomes Expenses",
                "Expenses", saved.lines.get(1).type);

        JournalService.Result up =
                svc.update(user, "JE-001", cashInvestment(7000));

        check("update works", up.ok());

        same("update keeps the same ID",
                "JE-001", up.entry.id);

        same("update changes the amount",
                7000.0, svc.find(user, "JE-001").totalDebits());

        JournalService.Result badUp =
                svc.update(user, "JE-001",
                        new JournalEntry("2026-11-04", "Bad")
                                .addLine("Cash", "Assets", "Debit", 10)
                                .addLine("Owner's Capital", "Equity", "Credit", 5));

        check("unbalanced update is rejected",
                !badUp.ok() && !badUp.notFound);

        same("old version stays safe after bad update",
                7000.0, svc.find(user, "JE-001").totalDebits());

        check("update of missing ID says not found",
                svc.update(user, "JE-999", cashInvestment(1)).notFound);

        check("ID lookup ignores upper/lower case",
                svc.find(user, "je-001") != null);

        check("find of missing ID gives null",
                svc.find(user, "JE-999") == null);

        JournalService.Result other =
                svc.create("b@test.com", cashInvestment(1));

        same("another user starts again at JE-001",
                "JE-001", other.entry.id);

        same("user A still has 4 entries",
                4, svc.list(user).size());

        same("user B has 1 entry",
                1, svc.list("b@test.com").size());

        same("email is not case-sensitive",
                4, svc.list("A@Test.com").size());

        same("lines() gives every line",
                8, svc.lines(user).size());

        same("empty user has no lines",
                0, svc.lines("nobody@test.com").size());
    }

    static class Reply {
        int status;
        String body;
        String location;
    }

    static Reply call(
            String method,
            String url,
            String email,
            String body
    ) throws IOException {

        HttpURLConnection c =
                (HttpURLConnection) java.net.URI.create(url)
                        .toURL().openConnection();

        c.setRequestMethod(method);

        if (email != null) {
            c.setRequestProperty("X-User-Email", email);
        }

        if (body != null) {
            c.setDoOutput(true);
            c.setRequestProperty("Content-Type", "application/json");

            try (OutputStream out = c.getOutputStream()) {
                out.write(body.getBytes(StandardCharsets.UTF_8));
            }
        }

        Reply r = new Reply();

        try {
            r.status = c.getResponseCode();
            r.location = c.getHeaderField("Location");

            InputStream in = r.status >= 400
                    ? c.getErrorStream()
                    : c.getInputStream();

            ByteArrayOutputStream buf = new ByteArrayOutputStream();

            if (in != null) {
                byte[] b = new byte[2048];
                int n;

                while ((n = in.read(b)) > 0) {
                    buf.write(b, 0, n);
                }

                in.close();
            }

            r.body = new String(
                    buf.toByteArray(),
                    StandardCharsets.UTF_8
            );

            return r;

        } finally {
            c.disconnect();
        }
    }

    static final String GOOD =
            "{\"date\":\"2026-11-04\",\"description\":\"Owner invested cash\",\"lines\":["
                    + "{\"account\":\"Cash\",\"category\":\"Assets\",\"side\":\"Debit\",\"amount\":5000},"
                    + "{\"account\":\"Owner's Capital\",\"category\":\"Equity\",\"side\":\"Credit\",\"amount\":5000}]}";

    static final String UNBALANCED =
            "{\"date\":\"2026-11-04\",\"description\":\"Oops\",\"lines\":["
                    + "{\"account\":\"Cash\",\"category\":\"Assets\",\"side\":\"Debit\",\"amount\":5000},"
                    + "{\"account\":\"Owner's Capital\",\"category\":\"Equity\",\"side\":\"Credit\",\"amount\":4000}]}";

    @SuppressWarnings("unchecked")
    static void apiTests() throws Exception {

        JournalService svc = new JournalService();

        Set<String> users = new HashSet<>(
                Arrays.asList("jhon@test.com", "other@test.com")
        );

        JournalApiServer api =
                new JournalApiServer(svc, users::contains);

        int port = api.start(0);

        String base = "http://localhost:" + port + "/api";
        String me = "jhon@test.com";

        try {

            Reply post = call("POST", base + "/journal", me, GOOD);

            same("POST valid entry -> 201", 201, post.status);

            Map<String, Object> posted =
                    (Map<String, Object>) MiniJson.parse(post.body);

            same("POST answer has ID JE-001",
                    "JE-001", posted.get("id"));

            same("POST answer shows total debit",
                    5000.0, posted.get("totalDebit"));

            same("POST sends Location header",
                    "/api/journal/JE-001", post.location);

            Reply unbalanced =
                    call("POST", base + "/journal", me, UNBALANCED);

            same("POST unbalanced entry -> 400",
                    400, unbalanced.status);

            check("400 answer explains it is not balanced",
                    unbalanced.body.contains("Not balanced"));

            same("rejected POST was not saved",
                    1, svc.list(me).size());

            same("POST broken JSON -> 400",
                    400,
                    call("POST", base + "/journal", me, "{not json").status);

            same("POST JSON list instead of object -> 400",
                    400,
                    call("POST", base + "/journal", me, "[1,2]").status);

            same("POST without lines -> 400",
                    400,
                    call("POST", base + "/journal", me,
                            "{\"date\":\"2026-11-04\",\"description\":\"x\"}").status);

            same("POST with text amount -> 400",
                    400,
                    call("POST", base + "/journal", me,
                            "{\"date\":\"2026-11-04\",\"description\":\"x\",\"lines\":[{\"account\":\"Cash\",\"side\":\"Debit\",\"amount\":\"abc\"},"
                                    + "{\"account\":\"Owner's Capital\",\"side\":\"Credit\",\"amount\":1}]}").status);

            Reply second =
                    call("POST", base + "/journal", me, GOOD);

            Map<String, Object> second2 =
                    (Map<String, Object>) MiniJson.parse(second.body);

            same("second POST gets JE-002",
                    "JE-002", second2.get("id"));

            Reply list = call("GET", base + "/journal", me, null);

            same("GET list -> 200", 200, list.status);

            same("GET list has 2 entries",
                    2,
                    ((List<Object>) MiniJson.parse(list.body)).size());

            Reply one =
                    call("GET", base + "/journal/JE-001", me, null);

            same("GET one -> 200", 200, one.status);

            Map<String, Object> oneMap =
                    (Map<String, Object>) MiniJson.parse(one.body);

            same("GET one has 2 lines",
                    2,
                    ((List<Object>) oneMap.get("lines")).size());

            same("GET missing ID -> 404",
                    404,
                    call("GET", base + "/journal/JE-999", me, null).status);

            String changed = GOOD
                    .replace("5000", "7500")
                    .replace("Owner invested cash", "Owner invested more cash");

            Reply put =
                    call("PUT", base + "/journal/JE-001", me, changed);

            same("PUT valid change -> 200", 200, put.status);

            Map<String, Object> putMap =
                    (Map<String, Object>) MiniJson.parse(put.body);

            same("PUT keeps the ID",
                    "JE-001", putMap.get("id"));

            same("PUT changes the amount",
                    7500.0, putMap.get("totalDebit"));

            same("GET shows updated entry",
                    7500.0,
                    ((Map<String, Object>) MiniJson.parse(
                            call("GET", base + "/journal/JE-001", me, null).body
                    )).get("totalDebit"));

            same("PUT unbalanced change -> 400",
                    400,
                    call("PUT", base + "/journal/JE-001", me, UNBALANCED).status);

            same("old data is safe after bad PUT",
                    7500.0, svc.find(me, "JE-001").totalDebits());

            same("PUT missing ID -> 404",
                    404,
                    call("PUT", base + "/journal/JE-999", me, GOOD).status);

            same("no X-User-Email header -> 401",
                    401,
                    call("GET", base + "/journal", null, null).status);

            same("unknown email -> 401",
                    401,
                    call("GET", base + "/journal", "stranger@test.com", null).status);

            same("other user cannot see my entry -> 404",
                    404,
                    call("GET", base + "/journal/JE-001", "other@test.com", null).status);

            same("other user list is empty",
                    0,
                    ((List<Object>) MiniJson.parse(
                            call("GET", base + "/journal", "other@test.com", null).body
                    )).size());

            same("DELETE is not allowed -> 405",
                    405,
                    call("DELETE", base + "/journal/JE-001", me, null).status);

            same("PUT on list route -> 405",
                    405,
                    call("PUT", base + "/journal", me, GOOD).status);

            same("unknown route -> 404",
                    404,
                    call("GET", base + "/nothing", me, null).status);

            Reply acc = call("GET", base + "/accounts", null, null);

            same("GET /accounts -> 200", 200, acc.status);

            Map<String, Object> accMap =
                    (Map<String, Object>) MiniJson.parse(acc.body);

            same("catalog has 5 categories",
                    5,
                    ((List<Object>) accMap.get("categories")).size());

            check("catalog lists standard accounts",
                    ((List<Object>) accMap.get("accounts")).size() > 0);

            check("entries posted by route are visible to screen",
                    svc.lines(me).size() == 4);

        } finally {
            api.stop();
        }
    }
}
