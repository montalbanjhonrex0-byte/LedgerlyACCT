import javax.swing.table.AbstractTableModel;
import java.text.DecimalFormat;
import java.util.ArrayList;

public class WorksheetBackend {

    public static final int MIN_ROWS = 15, FOOT = 4, BASE_SECTIONS = 6;
    public static final int FIRST = 3;
    public static final int INPUT = 4;

    public static final String[][] BASE_NAMES = {
        {"TRIAL", "BALANCE"}, {"ADJUSTMENTS"}, {"ADJUSTED", "TRIAL BALANCE"},
        {"INCOME", "STATEMENT"}, {"OWNER\u2019S", "EQUITY"}, {"BALANCE", "SHEET"}
    };
    public static final String[] FOOT_LABEL = {"TOTAL", "NET INCOME", "ENDING OWNER\u2019S EQUITY", "GRAND TOTAL"};
    public static final DecimalFormat MONEY = new DecimalFormat("#,##0.00");
    public static final double TOLERANCE = 0.005;

    public enum AccountType {
        NONE("", -1),
        ASSET("Asset", 10), LIABILITY("Liability", 10),
        CAPITAL("Owner\u2019s Capital", 8), DRAWINGS("Owner\u2019s Drawings", 8),
        REVENUE("Revenue", 6), EXPENSE("Expense", 6);

        public final String label;
        public final int firstCol;
        AccountType(String label, int firstCol) { this.label = label; this.firstCol = firstCol; }
        public int section() { return firstCol / 2; }
        @Override public String toString() { return label; }
    }

    public static class WorksheetModel extends AbstractTableModel {
        public final ArrayList<String> acct = new ArrayList<>();
        public final ArrayList<double[]> a = new ArrayList<>();
        public final ArrayList<AccountType> types = new ArrayList<>();
        public final ArrayList<String[]> sections = new ArrayList<>();
        public int rows = 0;
        public boolean finalized = false;

        public WorksheetModel() { reset(); }
        public int amt() { return sections.size() * 2; }

        public void reset() {
            rows = 0; acct.clear(); a.clear(); types.clear(); sections.clear();
            finalized = false;
            for (String[] n : BASE_NAMES) sections.add(n);
            for (int i = 0; i < MIN_ROWS; i++) push();
        }

        private void push() {
            acct.add("");
            a.add(new double[amt()]);
            types.add(AccountType.NONE);
            rows++;
        }

        public void addRow() {
            if (finalized) return;
            push();
            fireTableRowsInserted(rows - 1, rows - 1);
            fireTableRowsUpdated(0, getRowCount() - 1);
        }

        public int getRowCount() { return rows + FOOT; }
        public int getColumnCount() { return FIRST + amt(); }

        @Override public Class<?> getColumnClass(int c) { return c == 2 ? AccountType.class : Object.class; }

        public double netDebit(int r) {
            double[] v = a.get(r);
            return v[0] - v[1] + v[2] - v[3];
        }

        public double cell(int r, int k) {
            double[] v = a.get(r);
            if (k < INPUT) return v[k];
            double net = netDebit(r);
            if (k == 4 || k == 5)
                return k == 4 ? Math.max(net, 0) : Math.max(-net, 0);

            AccountType t = types.get(r);
            if (t == AccountType.NONE || k < t.firstCol || k > t.firstCol + 1) return 0;
            return k == t.firstCol ? Math.max(net, 0) : Math.max(-net, 0);
        }

        public double netProfit() {
            double revenues = 0, expenses = 0;
            for (int r = 0; r < rows; r++) { revenues += cell(r, 7); expenses += cell(r, 6); }
            return revenues - expenses;
        }

        public double[][] footer() {
            int n = amt();
            double[][] f = new double[FOOT][n];
            for (int k = 0; k < n; k++) for (int r = 0; r < rows; r++) f[0][k] += cell(r, k);
            double ni = netProfit();
            if (ni >= 0) { f[1][6] = ni; f[1][9] = ni; } else { f[1][7] = -ni; f[1][8] = -ni; }
            double end = (f[0][9] + f[1][9]) - (f[0][8] + f[1][8]);
            if (end >= 0) { f[2][8] = end; f[2][11] = end; } else { f[2][9] = -end; f[2][10] = -end; }
            for (int k = 0; k < n; k++) f[3][k] = f[0][k] + f[1][k] + f[2][k];
            return f;
        }

        public boolean empty() { for (double v : footer()[0]) if (v != 0) return false; return true; }

        public boolean balanced() {
            double[] g = footer()[3];
            for (int s = 0; s < BASE_SECTIONS; s++) if (Math.abs(g[2 * s] - g[2 * s + 1]) > TOLERANCE) return false;
            return true;
        }

        private boolean hasInput(int r) {
            double[] v = a.get(r);
            for (int i = 0; i < INPUT; i++) if (v[i] != 0) return true;
            return false;
        }

        public String verify() {
            if (empty()) return "Nothing to finalize. Enter some amounts first.";
            for (int r = 0; r < rows; r++)
                if (hasInput(r) && types.get(r) == AccountType.NONE)
                    return "Row " + (r + 1) + " has amounts but no account type.";

            double[] g = footer()[3];
            for (int s = 0; s < BASE_SECTIONS; s++)
                if (Math.abs(g[2 * s] - g[2 * s + 1]) > TOLERANCE)
                    return "Debits do not equal credits in " + String.join(" ", sections.get(s))
                            + " (Dr " + MONEY.format(g[2 * s]) + " vs Cr " + MONEY.format(g[2 * s + 1]) + ").";
            return null;
        }

        public boolean finalizeReport() {
            if (verify() != null) return false;
            finalized = true;
            fireTableDataChanged();
            return true;
        }

        public Object getValueAt(int r, int c) {
            if (r < rows) {
                if (c == 0) return r + 1;
                if (c == 1) return acct.get(r);
                if (c == 2) return types.get(r);
                return cell(r, c - FIRST);
            }
            if (c == 1) return FOOT_LABEL[r - rows];
            return c < FIRST ? "" : footer()[r - rows][c - FIRST];
        }

        @Override public boolean isCellEditable(int r, int c) {
            return !finalized && r < rows && c > 0 && c <= FIRST + INPUT - 1;
        }

        @Override public void setValueAt(Object v, int r, int c) {
            if (finalized || r >= rows) return;
            String s = v == null ? "" : v.toString().trim();
            if (c == 1) acct.set(r, s);
            else if (c == 2) {
                if (v instanceof AccountType) types.set(r, (AccountType) v);
            } else {
                try {
                    double x = s.isEmpty() ? 0 : Double.parseDouble(s.replace(",", ""));
                    if (Double.isNaN(x) || Double.isInfinite(x)) throw new NumberFormatException();
                    a.get(r)[c - FIRST] = Math.round(x * 100) / 100.0;
                } catch (NumberFormatException ex) { java.awt.Toolkit.getDefaultToolkit().beep(); }
            }
            fireTableRowsUpdated(0, getRowCount() - 1);
            if (r == rows - 1 && !s.isEmpty()) addRow();
        }

        public void clear() { reset(); fireTableStructureChanged(); }
    }
}