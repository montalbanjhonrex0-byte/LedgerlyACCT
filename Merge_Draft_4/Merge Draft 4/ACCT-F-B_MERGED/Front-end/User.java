public class User {
    public String name, email, hash, country, code, symbol, currencyName;
    public User(String n, String e, String h, String c, CurrencyInfo i) {
        name = n; email = e; hash = h; country = c; 
        code = i.code; symbol = i.symbol; currencyName = i.name;
    }
}