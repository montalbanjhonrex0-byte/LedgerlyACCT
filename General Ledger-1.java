
import javax.swing.*;
import javax.swing.table.*;
import java.awt.*;
import java.time.LocalDate;
import java.util.*;
import java.text.DecimalFormat;

public class GeneralLedger extends JFrame {

    private JTable journalTable;
    private JTable ledgerTable;

    private DefaultTableModel journalModel;
    private DefaultTableModel ledgerModel;

    private TableRowSorter<DefaultTableModel> sorter;

    private JComboBox<String> accountFilter;

    private JLabel totalDebitLabel;
    private JLabel totalCreditLabel;

    private final DecimalFormat df =
            new DecimalFormat("#,##0.00");

    // JOURNAL ENTRY CLASS
    static class JournalEntry {
        String date;
        String reference;
        String account;
        String description;
        double debit;
        double credit;

        JournalEntry(String date, String reference,
                     String account, String description,
                     double debit, double credit) {

            this.date = date;
            this.reference = reference;
            this.account = account;
            this.description = description;
            this.debit = debit;
            this.credit = credit;
        }
    }

    private ArrayList<JournalEntry> journalEntries =
            new ArrayList<>();

    public GeneralLedger() {

        setTitle(" General Ledger");
        setSize(1350, 750);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        setLayout(new BorderLayout());

        // MAIN PANEL
        JPanel main = new JPanel(new BorderLayout(15, 15));
        main.setBorder(BorderFactory.createEmptyBorder(
                20, 20, 20, 20));

        main.setBackground(new Color(244, 246, 250));

        // TITLE
        JLabel title = new JLabel("GENERAL LEDGER");
        title.setFont(new Font("Arial", Font.BOLD, 26));

        main.add(title, BorderLayout.NORTH);

        // CONTENT PANEL
        JPanel content = new JPanel(new BorderLayout(10, 15));
        content.setBackground(Color.WHITE);

        // SUMMARY PANEL
        JPanel summary = new JPanel(
                new GridLayout(1, 2, 15, 0));

        summary.setBackground(Color.WHITE);

        totalDebitLabel = new JLabel("₱0.00");
        totalCreditLabel = new JLabel("₱0.00");

        summary.add(summaryCard(
                "TOTAL DEBITS", totalDebitLabel));

        summary.add(summaryCard(
                "TOTAL CREDITS", totalCreditLabel));

        // ACCOUNT FILTER
        JPanel filterPanel = new JPanel(
                new FlowLayout(FlowLayout.LEFT));

        filterPanel.setBackground(Color.WHITE);

        accountFilter = new JComboBox<>();
        accountFilter.addItem("All Accounts");

        JButton refreshButton = new JButton("Refresh Ledger");
        JButton addButton = new JButton("Add Journal Entry");

        filterPanel.add(new JLabel("Account:"));
        filterPanel.add(accountFilter);
        filterPanel.add(refreshButton);
        filterPanel.add(addButton);

        JPanel topPanel = new JPanel(new BorderLayout(10, 10));
        topPanel.setBackground(Color.WHITE);

        topPanel.add(summary, BorderLayout.NORTH);
        topPanel.add(filterPanel, BorderLayout.SOUTH);

        content.add(topPanel, BorderLayout.NORTH);

        // JOURNAL TABLE
        String[] journalColumns = {
            "Date",
            "Reference No.",
            "Account Title",
            "Description",
            "Debit",
            "Credit"
        };

        journalModel = new DefaultTableModel(
                journalColumns, 0) {

            @Override
            public boolean isCellEditable(
                    int row, int column) {
                return false;
            }
        };

        journalTable = new JTable(journalModel);

        configureTable(journalTable,
                new int[]{110, 130, 170, 230, 120, 120});

        // LEDGER TABLE
        String[] ledgerColumns = {
            "Date",
            "Reference No.",
            "Account Title",
            "Description",
            "Debit",
            "Credit",
            "Running Balance"
        };

        ledgerModel = new DefaultTableModel(
                ledgerColumns, 0) {

            @Override
            public boolean isCellEditable(
                    int row, int column) {
                return false;
            }
        };

        ledgerTable = new JTable(ledgerModel);

        configureTable(ledgerTable,
                new int[]{110, 130, 170, 230, 120, 120, 160});

        sorter = new TableRowSorter<>(ledgerModel);
        ledgerTable.setRowSorter(sorter);

        JScrollPane ledgerScroll =
                new JScrollPane(ledgerTable);

        // JOURNAL AND LEDGER TABS
        JTabbedPane tabs = new JTabbedPane();

        tabs.addTab("Journal Entries",
                new JScrollPane(journalTable));

        tabs.addTab("General Ledger",
                ledgerScroll);

        content.add(tabs, BorderLayout.CENTER);

        main.add(content, BorderLayout.CENTER);

        add(main, BorderLayout.CENTER);

        // BUTTON ACTIONS
        addButton.addActionListener(
                e -> addJournalEntry());

        refreshButton.addActionListener(
                e -> postJournalToLedger());

        accountFilter.addActionListener(
                e -> filterLedger());

        setVisible(true);
    }

    // TABLE DESIGN
    private void configureTable(
            JTable table, int[] widths) {

        table.setAutoResizeMode(
                JTable.AUTO_RESIZE_OFF);

        table.setRowHeight(38);

        table.setShowGrid(true);
        table.setShowVerticalLines(true);
        table.setShowHorizontalLines(true);

        table.setGridColor(
                new Color(190, 200, 215));

        table.setIntercellSpacing(
                new Dimension(1, 1));

        table.setFont(
                new Font("Arial", Font.PLAIN, 13));

        JTableHeader header = table.getTableHeader();

        header.setPreferredSize(
                new Dimension(0, 42));

        header.setBackground(
                new Color(23, 43, 77));

        header.setForeground(Color.WHITE);

        header.setFont(
                new Font("Arial", Font.BOLD, 13));

        header.setReorderingAllowed(false);

        // HEADER CELL RENDERER
        DefaultTableCellRenderer headerRenderer =
                new DefaultTableCellRenderer();

        headerRenderer.setBackground(
                new Color(23, 43, 77));

        headerRenderer.setForeground(Color.WHITE);

        headerRenderer.setHorizontalAlignment(
                SwingConstants.CENTER);

        for (int i = 0; i < widths.length; i++) {

            table.getColumnModel()
                    .getColumn(i)
                    .setPreferredWidth(widths[i]);

            table.getColumnModel()
                    .getColumn(i)
                    .setHeaderRenderer(headerRenderer);
        }

        // NUMERIC ALIGNMENT
        DefaultTableCellRenderer right =
                new DefaultTableCellRenderer();

        right.setHorizontalAlignment(
                SwingConstants.RIGHT);

        for (int i = 4; i < table.getColumnCount(); i++) {

            table.getColumnModel()
                    .getColumn(i)
                    .setCellRenderer(right);
        }
    }

    // SUMMARY CARD
    private JPanel summaryCard(
            String title, JLabel value) {

        JPanel panel = new JPanel(
                new BorderLayout(10, 10));

        panel.setBackground(Color.WHITE);

        panel.setBorder(
                BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(
                                new Color(220, 225, 235)),
                        BorderFactory.createEmptyBorder(
                                15, 15, 15, 15)));

        JLabel heading = new JLabel(title);

        heading.setForeground(Color.GRAY);

        value.setFont(
                new Font("Arial", Font.BOLD, 23));

        panel.add(heading, BorderLayout.NORTH);
        panel.add(value, BorderLayout.CENTER);

        return panel;
    }

    // ADD JOURNAL ENTRY
    private void addJournalEntry() {

        JTextField date = new JTextField();
        JTextField reference = new JTextField();
        JTextField account = new JTextField();
        JTextField description = new JTextField();
        JTextField debit = new JTextField();
        JTextField credit = new JTextField();

        Object[] fields = {
            "Date (YYYY-MM-DD):", date,
            "Reference Number:", reference,
            "Account Title:", account,
            "Description:", description,
            "Debit:", debit,
            "Credit:", credit
        };

        int result = JOptionPane.showConfirmDialog(
                this, fields,
                "New Journal Entry",
                JOptionPane.OK_CANCEL_OPTION);

        if (result != JOptionPane.OK_OPTION) {
            return;
        }

        try {

            String d = date.getText().trim();
            String ref = reference.getText().trim();
            String acc = account.getText().trim();
            String desc = description.getText().trim();

            double dr = Double.parseDouble(
                    debit.getText().trim());

            double cr = Double.parseDouble(
                    credit.getText().trim());

            LocalDate.parse(d);

            if (ref.isEmpty() || acc.isEmpty()
                    || desc.isEmpty()
                    || !Double.isFinite(dr)
                    || !Double.isFinite(cr)
                    || dr < 0 || cr < 0
                    || (dr > 0 && cr > 0)
                    || (dr == 0 && cr == 0)) {

                throw new IllegalArgumentException();
            }

            journalEntries.add(
                    new JournalEntry(
                            d, ref, acc, desc, dr, cr));

            updateJournalTable();
            updateAccountFilter();
            postJournalToLedger();

        } catch (Exception ex) {

            JOptionPane.showMessageDialog(
                    this,
                    "Invalid journal entry. Check all fields.",
                    "Input Error",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    // DISPLAY JOURNAL
    private void updateJournalTable() {

        journalModel.setRowCount(0);

        for (JournalEntry entry : journalEntries) {

            journalModel.addRow(new Object[]{
                entry.date,
                entry.reference,
                entry.account,
                entry.description,
                entry.debit == 0 ? "-" :
                    "₱" + df.format(entry.debit),
                entry.credit == 0 ? "-" :
                    "₱" + df.format(entry.credit)
            });
        }
    }

    // ACCOUNT FILTER OPTIONS
    private void updateAccountFilter() {

        String selected =
                (String) accountFilter.getSelectedItem();

        Set<String> accounts = new TreeSet<>();

        for (JournalEntry entry : journalEntries) {
            accounts.add(entry.account);
        }

        accountFilter.removeAllItems();
        accountFilter.addItem("All Accounts");

        for (String account : accounts) {
            accountFilter.addItem(account);
        }

        if (selected != null &&
                accounts.contains(selected)) {
            accountFilter.setSelectedItem(selected);
        }
    }

    // POST JOURNAL ENTRIES TO LEDGER
    private void postJournalToLedger() {

        ledgerModel.setRowCount(0);

        ArrayList<JournalEntry> sorted =
                new ArrayList<>(journalEntries);

        sorted.sort(
                Comparator.comparing(
                        (JournalEntry e) ->
                                LocalDate.parse(e.date))
                        .thenComparing(e -> e.reference)
        );

        Map<String, Double> balances =
                new HashMap<>();

        double totalDebit = 0;
        double totalCredit = 0;

        for (JournalEntry entry : sorted) {

            double previous =
                    balances.getOrDefault(
                            entry.account, 0.0);

            double running =
                    previous + entry.debit - entry.credit;

            balances.put(entry.account, running);

            totalDebit += entry.debit;
            totalCredit += entry.credit;

            ledgerModel.addRow(new Object[]{
                entry.date,
                entry.reference,
                entry.account,
                entry.description,
                entry.debit == 0 ? "-" :
                    "₱" + df.format(entry.debit),
                entry.credit == 0 ? "-" :
                    "₱" + df.format(entry.credit),
                "₱" + df.format(running)
            });
        }

        totalDebitLabel.setText(
                "₱" + df.format(totalDebit));

        totalCreditLabel.setText(
                "₱" + df.format(totalCredit));

        filterLedger();
    }

    // FILTER LEDGER BY ACCOUNT
    private void filterLedger() {

        String selected =
                (String) accountFilter.getSelectedItem();

        if (selected == null ||
                selected.equals("All Accounts")) {

            sorter.setRowFilter(null);

        } else {

            sorter.setRowFilter(
                    RowFilter.regexFilter(
                            "^" +
                            java.util.regex.Pattern.quote(selected)
                            + "$", 2));
        }
    }

    public static void main(String[] args) {

        SwingUtilities.invokeLater(() -> {
            new GeneralLedger();
        });
    }
}
