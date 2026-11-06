
import javax.swing.*;
import javax.swing.table.*;
import java.awt.*;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class DynamicLedger extends JFrame {

    private final String[] columns = {
        "Date",
        "Voucher Reference",
        "Description",
        "Debit",
        "Credit",
        "Running Balance"
    };

    private final DefaultTableModel model;
    private final JTable table;

    private final List<Object[]> ledger = new ArrayList<>();

    private final JTextField startDate = new JTextField(10);
    private final JTextField endDate = new JTextField(10);
    private final JTextField voucherSearch = new JTextField(12);

    private final JComboBox<String> sortOption =
        new JComboBox<>(new String[]{
            "Date: Oldest to Newest",
            "Date: Newest to Oldest",
            "Voucher: A to Z",
            "Voucher: Z to A"
        });

    private final JLabel balanceLabel =
        new JLabel("Current Balance: 0.00");

    private BigDecimal openingBalance = BigDecimal.ZERO;

    private final DateTimeFormatter formatter =
        DateTimeFormatter.ISO_LOCAL_DATE;

    public DynamicLedger() {

        setTitle("Dynamic Ledger Management System");
        setSize(1200, 650);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        model = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int col) {
                return col != 5;
            }
        };

        table = new JTable(model);
        table.setRowHeight(27);

        // Filter and sorting panel
        JPanel filterPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));

        JButton filterButton = new JButton("Filter Date Range");
        JButton voucherButton = new JButton("Search Voucher");
        JButton sortButton = new JButton("Apply Sorting");
        JButton clearButton = new JButton("Clear Filters");

        filterPanel.add(new JLabel("Start Date (yyyy-MM-dd):"));
        filterPanel.add(startDate);

        filterPanel.add(new JLabel("End Date:"));
        filterPanel.add(endDate);

        filterPanel.add(filterButton);
        filterPanel.add(new JLabel("Voucher Reference:"));
        filterPanel.add(voucherSearch);
        filterPanel.add(voucherButton);

        filterPanel.add(sortOption);
        filterPanel.add(sortButton);
        filterPanel.add(clearButton);

        // Transaction buttons
        JButton addButton = new JButton("Add Entry");
        JButton deleteButton = new JButton("Delete Entry");

        JPanel actionPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        actionPanel.add(addButton);
        actionPanel.add(deleteButton);

        balanceLabel.setFont(new Font("Arial", Font.BOLD, 16));

        JPanel bottomPanel = new JPanel(new BorderLayout());
        bottomPanel.add(actionPanel, BorderLayout.WEST);
        bottomPanel.add(balanceLabel, BorderLayout.EAST);

        add(filterPanel, BorderLayout.NORTH);
        add(new JScrollPane(table), BorderLayout.CENTER);
        add(bottomPanel, BorderLayout.SOUTH);

        // Add transaction
        addButton.addActionListener(e -> addEntry());

        // Delete transaction
        deleteButton.addActionListener(e -> deleteEntry());

        // Filter by date range
        filterButton.addActionListener(e -> filterByDate());

        // Search voucher
        voucherButton.addActionListener(e -> filterByVoucher());

        // Sort entries
        sortButton.addActionListener(e -> sortLedger());

        // Clear filters
        clearButton.addActionListener(e -> {
            startDate.setText("");
            endDate.setText("");
            voucherSearch.setText("");
            sortOption.setSelectedIndex(0);
            refreshLedger(ledger);
        });

        setVisible(true);
    }

    // Add new ledger entry
    private void addEntry() {

        JTextField date = new JTextField();
        JTextField voucher = new JTextField();
        JTextField description = new JTextField();
        JTextField debit = new JTextField();
        JTextField credit = new JTextField();

        Object[] fields = {
            "Date (yyyy-MM-dd):", date,
            "Voucher Reference:", voucher,
            "Description:", description,
            "Debit:", debit,
            "Credit:", credit
        };

        int result = JOptionPane.showConfirmDialog(
            this, fields, "New Ledger Entry",
            JOptionPane.OK_CANCEL_OPTION
        );

        if (result != JOptionPane.OK_OPTION) return;

        try {
            LocalDate.parse(date.getText().trim(), formatter);

            BigDecimal d = parseAmount(debit.getText());
            BigDecimal c = parseAmount(credit.getText());

            if (d.signum() < 0 || c.signum() < 0 ||
                (d.signum() > 0 && c.signum() > 0)) {
                throw new IllegalArgumentException(
                    "Enter either a debit or a credit, not both."
                );
            }

            if (voucher.getText().trim().isEmpty()) {
                throw new IllegalArgumentException(
                    "Voucher reference is required."
                );
            }

            Object[] entry = {
                date.getText().trim(),
                voucher.getText().trim(),
                description.getText().trim(),
                d,
                c
            };

            ledger.add(entry);
            refreshLedger(ledger);

        } catch (Exception ex) {
            JOptionPane.showMessageDialog(
                this,
                "Invalid entry: " + ex.getMessage(),
                "Validation Error",
                JOptionPane.ERROR_MESSAGE
            );
        }
    }

    // Date range filtering
    private void filterByDate() {

        try {
            LocalDate from = LocalDate.parse(
                startDate.getText().trim(), formatter
            );

            LocalDate to = LocalDate.parse(
                endDate.getText().trim(), formatter
            );

            if (from.isAfter(to)) {
                throw new IllegalArgumentException(
                    "Start date must not be after end date."
                );
            }

            List<Object[]> filtered = new ArrayList<>();

            for (Object[] entry : ledger) {

                LocalDate date = LocalDate.parse(
                    entry[0].toString(), formatter
                );

                if (!date.isBefore(from) && !date.isAfter(to)) {
                    filtered.add(entry);
                }
            }

            refreshLedger(filtered);

        } catch (Exception ex) {
            showError("Enter valid date ranges using yyyy-MM-dd.");
        }
    }

    // Voucher reference search
    private void filterByVoucher() {

        String search = voucherSearch.getText()
            .trim().toLowerCase();

        List<Object[]> filtered = new ArrayList<>();

        for (Object[] entry : ledger) {

            String voucher = entry[1].toString().toLowerCase();

            if (voucher.contains(search)) {
                filtered.add(entry);
            }
        }

        refreshLedger(filtered);
    }

    // Sorting
    private void sortLedger() {

        List<Object[]> sorted = new ArrayList<>(ledger);

        switch (sortOption.getSelectedIndex()) {

            case 0:
                sorted.sort(Comparator.comparing(
                    e -> LocalDate.parse(
                        e[0].toString(), formatter
                    )
                ));
                break;

            case 1:
                sorted.sort(Comparator.comparing(
                    (Object[] e) -> LocalDate.parse(
                        e[0].toString(), formatter
                    )
                ).reversed());
                break;

            case 2:
                sorted.sort(Comparator.comparing(
                    e -> e[1].toString(),
                    String.CASE_INSENSITIVE_ORDER
                ));
                break;

            case 3:
                sorted.sort(Comparator.comparing(
                    (Object[] e) -> e[1].toString(),
                    String.CASE_INSENSITIVE_ORDER
                ).reversed());
                break;
        }

        refreshLedger(sorted);
    }

    // Display and calculate running balance
    private void refreshLedger(List<Object[]> entries) {

        model.setRowCount(0);

        BigDecimal balance = openingBalance;

        for (Object[] entry : entries) {

            BigDecimal debit = (BigDecimal) entry[3];
            BigDecimal credit = (BigDecimal) entry[4];

            balance = balance.subtract(debit).add(credit);

            model.addRow(new Object[]{
                entry[0],
                entry[1],
                entry[2],
                debit.toPlainString(),
                credit.toPlainString(),
                balance.setScale(2, RoundingMode.HALF_UP)
                       .toPlainString()
            });
        }

        balanceLabel.setText(
            "Displayed Balance: " +
            balance.setScale(2, RoundingMode.HALF_UP)
                   .toPlainString()
        );
    }

    private void deleteEntry() {

        int row = table.getSelectedRow();

        if (row < 0) return;

        String voucher = model.getValueAt(row, 1).toString();
        String date = model.getValueAt(row, 0).toString();

        int confirm = JOptionPane.showConfirmDialog(
            this,
            "Delete selected transaction?",
            "Confirm Delete",
            JOptionPane.YES_NO_OPTION
        );

        if (confirm != JOptionPane.YES_OPTION) return;

        // Locate the original transaction.
        for (int i = 0; i < ledger.size(); i++) {
            Object[] entry = ledger.get(i);

            if (entry[0].equals(date) &&
                entry[1].equals(voucher)) {
                ledger.remove(i);
                break;
            }
        }

        refreshLedger(ledger);
    }

    private BigDecimal parseAmount(String value) {

        if (value == null || value.trim().isEmpty()) {
            return BigDecimal.ZERO;
        }

        return new BigDecimal(
            value.trim().replace(",", "")
        );
    }

    private void showError(String message) {
        JOptionPane.showMessageDialog(
            this, message,
            "Input Error",
            JOptionPane.WARNING_MESSAGE
        );
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(DynamicLedger::new);
    }
}
