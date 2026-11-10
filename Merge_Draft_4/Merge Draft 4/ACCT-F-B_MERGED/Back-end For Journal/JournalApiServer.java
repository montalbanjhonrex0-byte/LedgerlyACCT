import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.function.Predicate;

/**
 * TASK 1 - Journal Transaction Endpoint Suite
 * Background web routes that receive, update and retrieve posted journal entries.
 * Uses the HTTP server that is already inside Java (no extra libraries).
 * It only listens on this computer (localhost).
 *
 *   POST /api/journal         receive (save) a new journal entry
 *   PUT  /api/journal/{id}    update a saved journal entry
 *   GET  /api/journal         retrieve all journal entries
 *   GET  /api/journal/{id}    retrieve one journal entry
 *   GET  /api/accounts        retrieve the account categories and standard accounts
 *
 * Every /api/journal call must send the header:  X-User-Email: your@email.com
 * (the email of an account that was signed up in the app).
 */
public class JournalApiServer {
    private static final int MAX_BODY_BYTES = 1_000_000;

    private final JournalService service;
    private final Predicate<String> userExists;
    private HttpServer server;

    public JournalApiServer(JournalService service, Predicate<String> userExists) {
        this.service = service;
        this.userExists = userExists;
    }

    /** Starts the routes. Use port 0 to pick any free port. Returns the port that is really used. */
    public int start(int port) throws IOException {
        server = HttpServer.create(new InetSocketAddress(InetAddress.getLoopbackAddress(), port), 0);
        server.createContext("/api/", this::handle);
        server.setExecutor(Executors.newFixedThreadPool(4, r -> {
            Thread t = new Thread(r, "journal-api");
            t.setDaemon(true);                 // does not keep the app open after the window closes
            return t;
        }));
        server.start();
        return server.getAddress().getPort();
    }

    public void stop() {
        if (server != null) server.stop(0);
    }

    // ---------------------------------------------------------------- routing
    private void handle(HttpExchange ex) throws IOException {
        try {
            String method = ex.getRequestMethod();
            String path = ex.getRequestURI().getPath().replaceAll("^/+|/+$", "");
            String[] parts = path.split("/");              // ["api", "journal", "JE-001"]

            if (parts.length == 2 && parts[1].equals("accounts")) {
                if (!method.equals("GET")) { methodNotAllowed(ex, "GET"); return; }
                send(ex, 200, accountsJson());
                return;
            }
            if (parts.length < 2 || parts.length > 3 || !parts[1].equals("journal")) {
                send(ex, 404, errors("Route not found."));
                return;
            }

            String user = ex.getRequestHeaders().getFirst("X-User-Email");
            user = user == null ? "" : user.trim().toLowerCase(Locale.ROOT);
            if (user.isEmpty() || !userExists.test(user)) {
                send(ex, 401, errors("Send the email of a signed-up account in the X-User-Email header."));
                return;
            }

            if (parts.length == 2) {                       // /api/journal
                if (method.equals("GET")) {
                    List<Object> all = new ArrayList<>();
                    for (JournalEntry je : service.list(user)) all.add(toJson(je));
                    send(ex, 200, all);
                } else if (method.equals("POST")) {
                    JournalService.Result r = service.create(user, parseDraft(readBody(ex)));
                    if (r.ok()) {
                        ex.getResponseHeaders().set("Location", "/api/journal/" + r.entry.id);
                        send(ex, 201, toJson(r.entry));
                    } else {
                        send(ex, 400, errors(r.errors));
                    }
                } else {
                    methodNotAllowed(ex, "GET, POST");
                }
                return;
            }

            String id = parts[2];                          // /api/journal/{id}
            if (method.equals("GET")) {
                JournalEntry je = service.find(user, id);
                if (je == null) send(ex, 404, errors("Journal entry " + id + " was not found."));
                else send(ex, 200, toJson(je));
            } else if (method.equals("PUT")) {
                JournalService.Result r = service.update(user, id, parseDraft(readBody(ex)));
                if (r.ok()) send(ex, 200, toJson(r.entry));
                else send(ex, r.notFound ? 404 : 400, errors(r.errors));
            } else {
                methodNotAllowed(ex, "GET, PUT");
            }
        } catch (IllegalArgumentException e) {             // bad JSON or wrong shape
            send(ex, 400, errors(e.getMessage()));
        } catch (Exception e) {
            send(ex, 500, errors("Server error: " + e.getMessage()));
        } finally {
            ex.close();
        }
    }

    // ---------------------------------------------------------------- reading the request
    @SuppressWarnings("unchecked")
    private static JournalEntry parseDraft(String body) {
        Object root = MiniJson.parse(body);
        if (!(root instanceof Map)) throw new IllegalArgumentException("The body must be a JSON object.");
        Map<String, Object> m = (Map<String, Object>) root;

        JournalEntry draft = new JournalEntry(text(m.get("date")), text(m.get("description")));
        Object lines = m.get("lines");
        if (!(lines instanceof List)) throw new IllegalArgumentException("\"lines\" must be a list of journal lines.");
        for (Object o : (List<Object>) lines) {
            if (!(o instanceof Map)) throw new IllegalArgumentException("Each line must be a JSON object.");
            Map<String, Object> line = (Map<String, Object>) o;
            Object amount = line.get("amount");
            draft.addLine(
                    text(line.get("account")),
                    text(line.get("category")),
                    text(line.get("side")),
                    amount instanceof Number ? ((Number) amount).doubleValue() : Double.NaN);
        }
        return draft;
    }

    private static String text(Object o) { return o == null ? null : String.valueOf(o); }

    private static String readBody(HttpExchange ex) throws IOException {
        try (InputStream in = ex.getRequestBody()) {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            byte[] buf = new byte[4096];
            int n;
            while ((n = in.read(buf)) > 0) {
                out.write(buf, 0, n);
                if (out.size() > MAX_BODY_BYTES) throw new IllegalArgumentException("The request is too large.");
            }
            return new String(out.toByteArray(), StandardCharsets.UTF_8);
        }
    }

    // ---------------------------------------------------------------- writing the answer
    private static Map<String, Object> toJson(JournalEntry je) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", je.id);
        m.put("date", je.date);
        m.put("description", je.description);
        m.put("totalDebit", je.totalDebits());
        m.put("totalCredit", je.totalCredits());
        List<Object> lines = new ArrayList<>();
        for (Entry e : je.lines) {
            Map<String, Object> l = new LinkedHashMap<>();
            l.put("account", e.account);
            l.put("category", e.type);
            l.put("side", e.entrySide);
            l.put("amount", e.amount);
            lines.add(l);
        }
        m.put("lines", lines);
        return m;
    }

    private static Map<String, Object> accountsJson() {
        Map<String, Object> m = new LinkedHashMap<>();
        List<Object> cats = new ArrayList<>();
        for (String c : AccountCatalog.CATEGORIES) {
            Map<String, Object> one = new LinkedHashMap<>();
            one.put("category", c);
            one.put("normalBalance", AccountCatalog.normalBalance(c));
            cats.add(one);
        }
        m.put("categories", cats);
        List<Object> accounts = new ArrayList<>();
        for (Map.Entry<String, String> a : AccountCatalog.standardAccounts().entrySet()) {
            Map<String, Object> one = new LinkedHashMap<>();
            one.put("account", a.getKey());
            one.put("category", a.getValue());
            accounts.add(one);
        }
        m.put("accounts", accounts);
        return m;
    }

    private static Map<String, Object> errors(String message) { return errors(Collections.singletonList(message)); }
    private static Map<String, Object> errors(List<String> messages) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("errors", messages);
        return m;
    }

    private static void methodNotAllowed(HttpExchange ex, String allowed) throws IOException {
        ex.getResponseHeaders().set("Allow", allowed);
        send(ex, 405, errors("Method not allowed. Use: " + allowed));
    }

    private static void send(HttpExchange ex, int status, Object body) throws IOException {
        byte[] bytes = MiniJson.toJson(body).getBytes(StandardCharsets.UTF_8);
        ex.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
        ex.sendResponseHeaders(status, bytes.length);
        ex.getResponseBody().write(bytes);
    }
}
