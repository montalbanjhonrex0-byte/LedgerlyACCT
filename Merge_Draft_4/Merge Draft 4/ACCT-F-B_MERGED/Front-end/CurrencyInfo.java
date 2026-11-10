public class CurrencyInfo {
    public String code, symbol, name;
    public CurrencyInfo(String c, String s, String n) {
        code = c; symbol = s; name = n;
    }
    public String toString() {
        return code + " (" + symbol + ") — " + name;
    }
}