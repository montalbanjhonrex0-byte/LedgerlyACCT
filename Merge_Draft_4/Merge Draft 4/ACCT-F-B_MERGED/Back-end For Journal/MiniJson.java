import java.math.BigDecimal;
import java.util.*;

/**
 * A very small JSON reader/writer, so the project needs no extra libraries.
 * parse() gives: Map (object), List (array), String, Double, Boolean or null.
 */
public final class MiniJson {
    private final String s;
    private int i = 0;

    private MiniJson(String s) { this.s = s; }

    public static Object parse(String text) {
        if (text == null) throw new IllegalArgumentException("Empty JSON");
        MiniJson p = new MiniJson(text);
        p.skipSpaces();
        Object v = p.readValue();
        p.skipSpaces();
        if (p.i != p.s.length()) throw p.fail("Extra text after the JSON");
        return v;
    }

    private IllegalArgumentException fail(String message) {
        return new IllegalArgumentException("Invalid JSON: " + message + " (position " + i + ")");
    }
    private void skipSpaces() { while (i < s.length() && Character.isWhitespace(s.charAt(i))) i++; }
    private boolean at(char c) { return i < s.length() && s.charAt(i) == c; }
    private void expect(char c) {
        if (!at(c)) throw fail("Expected '" + c + "'");
        i++;
    }

    private Object readValue() {
        if (i >= s.length()) throw fail("Unexpected end");
        char c = s.charAt(i);
        if (c == '{') return readObject();
        if (c == '[') return readArray();
        if (c == '"') return readString();
        if (s.startsWith("true", i)) { i += 4; return Boolean.TRUE; }
        if (s.startsWith("false", i)) { i += 5; return Boolean.FALSE; }
        if (s.startsWith("null", i)) { i += 4; return null; }
        return readNumber();
    }

    private Map<String, Object> readObject() {
        Map<String, Object> map = new LinkedHashMap<>();
        expect('{'); skipSpaces();
        if (at('}')) { i++; return map; }
        while (true) {
            skipSpaces();
            if (!at('"')) throw fail("Expected a text key");
            String k = readString();
            skipSpaces(); expect(':'); skipSpaces();
            map.put(k, readValue());
            skipSpaces();
            if (at(',')) { i++; continue; }
            expect('}');
            return map;
        }
    }

    private List<Object> readArray() {
        List<Object> list = new ArrayList<>();
        expect('['); skipSpaces();
        if (at(']')) { i++; return list; }
        while (true) {
            skipSpaces();
            list.add(readValue());
            skipSpaces();
            if (at(',')) { i++; continue; }
            expect(']');
            return list;
        }
    }

    private String readString() {
        expect('"');
        StringBuilder sb = new StringBuilder();
        while (true) {
            if (i >= s.length()) throw fail("Text is not closed");
            char c = s.charAt(i++);
            if (c == '"') return sb.toString();
            if (c != '\\') { sb.append(c); continue; }
            if (i >= s.length()) throw fail("Bad escape");
            char e = s.charAt(i++);
            switch (e) {
                case '"': sb.append('"'); break;
                case '\\': sb.append('\\'); break;
                case '/': sb.append('/'); break;
                case 'b': sb.append('\b'); break;
                case 'f': sb.append('\f'); break;
                case 'n': sb.append('\n'); break;
                case 'r': sb.append('\r'); break;
                case 't': sb.append('\t'); break;
                case 'u':
                    if (i + 4 > s.length()) throw fail("Bad \\u escape");
                    try { sb.append((char) Integer.parseInt(s.substring(i, i + 4), 16)); }
                    catch (NumberFormatException ex) { throw fail("Bad \\u escape"); }
                    i += 4;
                    break;
                default: throw fail("Bad escape");
            }
        }
    }

    private Double readNumber() {
        int start = i;
        while (i < s.length() && "+-0123456789.eE".indexOf(s.charAt(i)) >= 0) i++;
        if (start == i) throw fail("Unexpected character '" + s.charAt(i) + "'");
        try { return Double.valueOf(s.substring(start, i)); }
        catch (NumberFormatException ex) { throw fail("Bad number"); }
    }

    // ---------- writer ----------
    public static String toJson(Object value) {
        StringBuilder sb = new StringBuilder();
        write(sb, value);
        return sb.toString();
    }

    private static void write(StringBuilder sb, Object v) {
        if (v == null) { sb.append("null"); }
        else if (v instanceof String) { quote(sb, (String) v); }
        else if (v instanceof Boolean) { sb.append(v); }
        else if (v instanceof Integer || v instanceof Long) { sb.append(v); }
        else if (v instanceof Number) { sb.append(BigDecimal.valueOf(((Number) v).doubleValue()).toPlainString()); }
        else if (v instanceof Map) {
            sb.append('{');
            boolean first = true;
            for (Map.Entry<?, ?> e : ((Map<?, ?>) v).entrySet()) {
                if (!first) sb.append(',');
                first = false;
                quote(sb, String.valueOf(e.getKey())); sb.append(':'); write(sb, e.getValue());
            }
            sb.append('}');
        } else if (v instanceof Collection) {
            sb.append('[');
            boolean first = true;
            for (Object o : (Collection<?>) v) {
                if (!first) sb.append(',');
                first = false;
                write(sb, o);
            }
            sb.append(']');
        } else { quote(sb, String.valueOf(v)); }
    }

    private static void quote(StringBuilder sb, String text) {
        sb.append('"');
        for (int k = 0; k < text.length(); k++) {
            char c = text.charAt(k);
            switch (c) {
                case '"': sb.append("\\\""); break;
                case '\\': sb.append("\\\\"); break;
                case '\n': sb.append("\\n"); break;
                case '\r': sb.append("\\r"); break;
                case '\t': sb.append("\\t"); break;
                default:
                    if (c < 0x20) sb.append(String.format("\\u%04x", (int) c));
                    else sb.append(c);
            }
        }
        sb.append('"');
    }
}
