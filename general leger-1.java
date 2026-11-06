
import javax.swing.*;
import javax.swing.table.*;
import java.awt.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class LedgerManagement extends JFrame {

    private JTable accountTable;
    private JTable transactionTable;

    private DefaultTableModel accountModel;
    private DefaultTableModel transactionModel;

    private JLabel debitLabel;
    private JLabel creditLabel;

    private final List<Object[]> transactions =
        new ArrayList<>();

    // GREEN COLOR THEME
    private final Color DARK_GREEN =
        new Color(20, 90, 50);

    private final Color GREEN =
        new Color(34, 139, 87);

    private final Color LIGHT_GREEN =
        new Color(190, 235, 200);

    private final Color PALE_GREEN =
        new Color(235, 248, 238);

    private final Color WHITE =
        Color.WHITE;

    public LedgerManagement() {

        setTitle("Ledger Management System");
        setSize(1150, 720);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        getContentPane().setBackground(DARK_GREEN);

        setLayout(new BorderLayout(12, 12));

        add(createHeader(), BorderLayout.NORTH);
        add(createMainContent(), BorderLayout.CENTER);
        add(createFooter(), BorderLayout.SOUTH);

        updateTotals();
    }

    // HEADER
    private JPanel createHeader() {

        JPanel panel = new JPanel(new BorderLayout());

        panel.setBackground(DARK_GREEN);

        panel.setBorder(BorderFactory.createEmptyBorder(
            15, 20, 10, 20));

        JButton addTransaction =
            createGreenButton("+ Add Transaction");

        addTransaction.addActionListener(e ->
            showTransactionDialog());

        panel.add(addTransaction, BorderLayout.EAST);

        return panel;
    }

    // MAIN TABLE CONTENT
    private JPanel createMainContent() {

        JPanel panel = new JPanel(new BorderLayout(10, 10));

        panel.setBackground(DARK_GREEN);

        panel.setBorder(BorderFactory.createEmptyBorder(
            5, 20, 10, 20));

        // ACCOUNT TABLE

        String[] accountColumns = {
            "Account Code",
            "Account Name",
            "Account Type",
            "Normal Balance",
            "Current Balance"
        };

        accountModel = new DefaultTableModel(
            accountColumns, 0) {

            public boolean isCellEditable(
                int row, int column) {
                return false;
            }
        };

        accountTable = createTable(accountModel);

        accountTable.getSelectionModel()
            .addListSelectionListener(e -> {

                if (!e.getValueIsAdjusting()) {
                    filterTransactions();
                }
            });

        JPanel accountPanel = createTablePanel(
            "ACCOUNT RECORDS",
            accountTable
        );

        // TRANSACTION TABLE

        String[] transactionColumns = {
            "Reference No.",
            "Date",
            "Account Code",
            "Description",
            "Debit",
            "Credit"
        };

        transactionModel = new DefaultTableModel(
            transactionColumns, 0) {

            public boolean isCellEditable(
                int row, int column) {
                return false;
            }
        };

        transactionTable = createTable(
            transactionModel);

        JPanel transactionPanel = createTablePanel(
            "TRANSACTION RECORDS",
            transactionTable
        );

        // TABLE GRID SPLIT

        JSplitPane split = new JSplitPane(
            JSplitPane.VERTICAL_SPLIT,
            accountPanel,
            transactionPanel
        );

        split.setResizeWeight(0.45);
        split.setDividerSize(8);
        split.setBackground(DARK_GREEN);

        panel.add(split, BorderLayout.CENTER);

        return panel;
    }

    // TABLE PANEL
    private JPanel createTablePanel(
        String title,
        JTable table) {

        JPanel panel = new JPanel(new BorderLayout(5, 8));

        panel.setBackground(PALE_GREEN);

        panel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(GREEN, 2),
            BorderFactory.createEmptyBorder(10, 10, 10, 10)
        ));

        JLabel label = new JLabel(title);

        label.setFont(new Font(
            "Arial", Font.BOLD, 15));

        label.setForeground(DARK_GREEN);

        JScrollPane scrollPane =
            new JScrollPane(table);

        scrollPane.getViewport().setBackground(WHITE);

        scrollPane.setBorder(
            BorderFactory.createLineBorder(GREEN));

        panel.add(label, BorderLayout.NORTH);
        panel.add(scrollPane, BorderLayout.CENTER);

        return panel;
    }

    // TABLE DESIGN
    private JTable createTable(
        DefaultTableModel model) {

        JTable table = new JTable(model);

        table.setRowHeight(35);

        table.setFont(new Font(
            "Arial", Font.PLAIN, 13));

        table.setSelectionMode(
            ListSelectionModel.SINGLE_SELECTION);

        table.setBackground(WHITE);

        table.setForeground(Color.BLACK);

        table.setGridColor(
            new Color(170, 210, 180));

        table.setShowGrid(true);

        table.setSelectionBackground(LIGHT_GREEN);

        table.setSelectionForeground(DARK_GREEN);

        // GREEN TABLE HEADER

        JTableHeader header = table.getTableHeader();

        header.setFont(new Font(
            "Arial", Font.BOLD, 13));

        header.setBackground(GREEN);

        header.setForeground(WHITE);

        header.setPreferredSize(
            new Dimension(0, 40));

        // ALTERNATING ROW COLORS

        table.setDefaultRenderer(
            Object.class,
            new DefaultTableCellRenderer() {

                @Override
                public Component getTableCellRendererComponent(
                    JTable table,
                    Object value,
                    boolean isSelected,
                    boolean hasFocus,
                    int row,
                    int column) {

                    Component component =
                        super.getTableCellRendererComponent(
                            table, value,
                            isSelected, hasFocus,
                            row, column
                        );

                    if (isSelected) {

                        component.setBackground(
                            LIGHT_GREEN);

                        component.setForeground(
                            DARK_GREEN);

                    } else if (row % 2 == 0) {

                        component.setBackground(
                            PALE_GREEN);

                        component.setForeground(
                            Color.BLACK);

                    } else {

                        component.setBackground(
                            WHITE);

                        component.setForeground(
                            Color.BLACK);
                    }

                    return component;
                }
            }
        );

        table.setAutoCreateRowSorter(true);

        return table;
    }

    // GREEN BUTTON DESIGN
    private JButton createGreenButton(String text) {

        JButton button = new JButton(text);

        button.setBackground(GREEN);
        button.setForeground(WHITE);

        button.setFont(new Font(
            "Arial", Font.BOLD, 13));

        button.setFocusPainted(false);
        button.setBorderPainted(false);
        button.setOpaque(true);

        button.setCursor(
            new Cursor(Cursor.HAND_CURSOR));

        return button;
    }

    // FOOTER
    private JPanel createFooter() {

        JPanel footer = new JPanel(
            new FlowLayout(FlowLayout.RIGHT, 20, 10));

        footer.setBackground(DARK_GREEN);

        debitLabel = new JLabel("Total Debit: ₱0.00");
        creditLabel = new JLabel("Total Credit: ₱0.00");

        debitLabel.setForeground(WHITE);
        creditLabel.setForeground(WHITE);

        debitLabel.setFont(new Font(
            "Arial", Font.BOLD, 14));

        creditLabel.setFont(new Font(
            "Arial", Font.BOLD, 14));

        JButton refresh = createGreenButton("Refresh");

        refresh.addActionListener(e -> {
            filterTransactions();
            updateTotals();
        });

        footer.add(debitLabel);
        footer.add(creditLabel);
        footer.add(refresh);

        return footer;
    }

    // ADD TRANSACTION
    private void addTransaction(
        String reference,
        String date,
        String accountCode,
        String description,
        double debit,
        double credit) {

        Object[] record = {
            reference,
            date,
            accountCode,
            description,
            debit,
            credit
        };

        transactions.add(record);

        transactionModel.addRow(record);
    }

    // FILTER TRANSACTIONS
    private void filterTransactions() {

        int selected = accountTable.getSelectedRow();

        transactionModel.setRowCount(0);

        String accountCode = null;

        if (selected >= 0) {

            int modelRow =
                accountTable.convertRowIndexToModel(
                    selected);

            accountCode = accountModel.getValueAt(
                modelRow, 0).toString();
        }

        for (Object[] transaction : transactions) {

            if (accountCode == null ||
                transaction[2].equals(accountCode)) {

                transactionModel.addRow(transaction);
            }
        }

        updateTotals();
    }

    // COMPUTE TOTALS
    private void updateTotals() {

        double debit = 0;
        double credit = 0;

        for (int i = 0;
             i < transactionModel.getRowCount(); i++) {

            debit += Double.parseDouble(
                transactionModel.getValueAt(i, 4)
                    .toString());

            credit += Double.parseDouble(
                transactionModel.getValueAt(i, 5)
                    .toString());
        }

        debitLabel.setText(String.format(
            "Total Debit: ₱%,.2f", debit));

        creditLabel.setText(String.format(
            "Total Credit: ₱%,.2f", credit));
    }

    // TRANSACTION FORM
    private void showTransactionDialog() {

        JTextField reference = new JTextField();
        JTextField account = new JTextField();
        JTextField description = new JTextField();
        JTextField debit = new JTextField("0");
        JTextField credit = new JTextField("0");

        JPanel form = new JPanel(
            new GridLayout(5, 2, 8, 8));

        form.setBackground(PALE_GREEN);

        form.add(new JLabel("Reference:"));
        form.add(reference);

        form.add(new JLabel("Account Code:"));
        form.add(account);

        form.add(new JLabel("Description:"));
        form.add(description);

        form.add(new JLabel("Debit:"));
        form.add(debit);

        form.add(new JLabel("Credit:"));
        form.add(credit);

        int result = JOptionPane.showConfirmDialog(
            this,
            form,
            "New Transaction",
            JOptionPane.OK_CANCEL_OPTION
        );

        if (result == JOptionPane.OK_OPTION) {

            try {

                double d = Double.parseDouble(
                    debit.getText());

                double c = Double.parseDouble(
                    credit.getText());

                if (d < 0 || c < 0 ||
                    (d == 0 && c == 0) ||
                    (d > 0 && c > 0)) {

                    throw new IllegalArgumentException(
                        "Enter either a debit or credit amount.");
                }

                String code = account.getText().trim();

                boolean exists = false;

                for (int i = 0;
                     i < accountModel.getRowCount(); i++) {

                    if (accountModel.getValueAt(i, 0)
                        .toString().equals(code)) {

                        exists = true;
                        break;
                    }
                }

                if (!exists) {

                    throw new IllegalArgumentException(
                        "Account code does not exist.");
                }

                addTransaction(
                    reference.getText(),
                    LocalDate.now().toString(),
                    code,
                    description.getText(),
                    d,
                    c
                );

                filterTransactions();

            } catch (NumberFormatException ex) {

                JOptionPane.showMessageDialog(
                    this,
                    "Invalid transaction amount."
                );

            } catch (IllegalArgumentException ex) {

                JOptionPane.showMessageDialog(
                    this,
                    ex.getMessage()
                );
            }
        }
    }

    // MAIN METHOD
    public static void main(String[] args) {

        SwingUtilities.invokeLater(() -> {

            new LedgerManagement().setVisible(true);

        });
    }
}
