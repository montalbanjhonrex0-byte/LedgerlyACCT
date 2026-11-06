
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.event.TableModelEvent;
import java.awt.*;
import java.math.BigDecimal;

public class DynamicRunningBalance extends JFrame {

    private final String[] columns = {
        "Date", "Description", "Debit", "Credit", "Running Balance"
    };

    private final DefaultTableModel model;
    private final JTable table;
    private final JLabel balanceLabel;

    private BigDecimal openingBalance = BigDecimal.ZERO;
    private boolean updating = false;

    public DynamicRunningBalance() {

        setTitle("Dynamic Running Balance View");
        setSize(1000, 600);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        model = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return column != 4;
            }
        };

        table = new JTable(model);
        table.setRowHeight(30);
        table.setFillsViewportHeight(true);

        JScrollPane scrollPane = new JScrollPane(table);

        JButton addButton = new JButton("Add Entry");
        JButton deleteButton = new JButton("Delete Entry");

        balanceLabel = new JLabel("Current Balance: 0.00");
        balanceLabel.setFont(new Font("Arial", Font.BOLD, 16));

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.LEFT));
        buttons.add(addButton);
        buttons.add(deleteButton);

        JPanel footer = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        footer.add(balanceLabel);

        JPanel bottom = new JPanel(new BorderLayout());
        bottom.add(buttons, BorderLayout.WEST);
        bottom.add(footer, BorderLayout.EAST);

        add(scrollPane, BorderLayout.CENTER);
        add(bottom, BorderLayout.SOUTH);

        addButton.addActionListener(e -> {
            model.addRow(new Object[]{
                "", "", "", "", ""
            });
            recalculateBalances();
        });

        deleteButton.addActionListener(e -> {
            int row = table.getSelectedRow();

            if (row >= 0) {
                model.removeRow(table.convertRowIndexToModel(row));
                recalculateBalances();
            }
        });

        model.addTableModelListener(e -> {
            if (!updating &&
                e.getType() == TableModelEvent.UPDATE) {
                recalculateBalances();
            }
        });

        setVisible(true);
    }

    private BigDecimal getAmount(Object value) {
        if (value == null || value.toString().trim().isEmpty()) {
            return BigDecimal.ZERO;
        }

        return new BigDecimal(
            value.toString().replace(",", "").trim()
        );
    }

    private void recalculateBalances() {

        if (updating) return;

        updating = true;

        try {
            BigDecimal balance = openingBalance;

            for (int row = 0; row < model.getRowCount(); row++) {

                BigDecimal debit = getAmount(model.getValueAt(row, 2));
                BigDecimal credit = getAmount(model.getValueAt(row, 3));

                balance = balance.subtract(debit).add(credit);

                model.setValueAt(
                    balance.setScale(2,
                        java.math.RoundingMode.HALF_UP).toPlainString(),
                    row, 4
                );
            }

            balanceLabel.setText(
                "Current Balance: " +
                balance.setScale(2,
                    java.math.RoundingMode.HALF_UP).toPlainString()
            );

        } catch (NumberFormatException ex) {
            balanceLabel.setText("Check invalid transaction amount");
        } finally {
            updating = false;
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(DynamicRunningBalance::new);
    }
}
