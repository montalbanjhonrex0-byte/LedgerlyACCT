public class AccountingLogic {
    public static boolean isIncomeStatementAccount(String type) {
        return type.equalsIgnoreCase("Revenue") || type.equalsIgnoreCase("Expenses");
    }

    public static boolean isBalanceSheetAccount(String type) {
        return type.equalsIgnoreCase("Assets") || type.equalsIgnoreCase("Liabilities") || type.equalsIgnoreCase("Equity");
    }
}