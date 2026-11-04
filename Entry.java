public class Entry {
    public String date, account, description, type, entrySide; // entrySide: "Debit" or "Credit"
    public double amount;

    public Entry(String date, String account, String description, String type, String entrySide, double amount) {
        this.date = date;
        this.account = account;
        this.description = description;
        this.type = type;
        this.entrySide = entrySide;
        this.amount = amount;
    }
}