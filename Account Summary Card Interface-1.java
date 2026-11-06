
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.text.NumberFormat;
import java.util.Locale;

public class AccountSummaryDashboard extends JFrame {

    private JComboBox<String> accountSelector;

    private JLabel debitValue;
    private JLabel creditValue;
    private JLabel balanceValue;

    private final NumberFormat currency =
        NumberFormat.getCurrencyInstance(
            Locale.forLanguageTag("en-PH")
        );

    private final String[] accounts = {
        "Select Account",
        "Savings Account",
        "Checking Account",
        "Business Account"
    };

    // No example transaction amounts
    private final double[][] accountData = {
        {0, 0, 0},
        {0, 0, 0},
        {0, 0, 0},
        {0, 0, 0}
    };

    private final Color darkGreen =
        new Color(0, 65, 45);

    private final Color lightGreen =
        new Color(30, 120, 85);

    public AccountSummaryDashboard() {

        setTitle("Account Summary Dashboard");
        setSize(900, 500);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        JPanel mainPanel = new JPanel(
            new BorderLayout(20, 25)
        );

        mainPanel.setBackground(darkGreen);
        mainPanel.setBorder(
            new EmptyBorder(30, 30, 30, 30)
        );

        // HEADER
        JLabel title = new JLabel(
            "ACCOUNT SUMMARY DASHBOARD"
        );

        title.setFont(
            new Font("Arial", Font.BOLD, 26)
        );

        title.setForeground(Color.WHITE);

        JLabel subtitle = new JLabel(
            "Monitor your account financial activity"
        );

        subtitle.setFont(
            new Font("Arial", Font.PLAIN, 14)
        );

        subtitle.setForeground(
            new Color(200, 225, 210)
        );

        JPanel heading = new JPanel(
            new GridLayout(2, 1, 0, 8)
        );

        heading.setOpaque(false);
        heading.add(title);
        heading.add(subtitle);

        // ACCOUNT SELECTOR
        accountSelector = new JComboBox<>(accounts);

        accountSelector.setFont(
            new Font("Arial", Font.PLAIN, 14)
        );

        accountSelector.setPreferredSize(
            new Dimension(200, 35)
        );

        JPanel selectionPanel = new JPanel(
            new FlowLayout(FlowLayout.LEFT)
        );

        selectionPanel.setOpaque(false);

        JLabel selectLabel =
            new JLabel("Select Account:");

        selectLabel.setForeground(Color.WHITE);

        selectionPanel.add(selectLabel);
        selectionPanel.add(accountSelector);

        JPanel header = new JPanel(
            new BorderLayout(10, 15)
        );

        header.setOpaque(false);
        header.add(heading, BorderLayout.NORTH);
        header.add(selectionPanel, BorderLayout.SOUTH);

        mainPanel.add(header, BorderLayout.NORTH);

        // SUMMARY CARDS
        JPanel cardsPanel = new JPanel(
            new GridLayout(1, 3, 20, 20)
        );

        cardsPanel.setOpaque(false);

        debitValue = new JLabel("");
        creditValue = new JLabel("");
        balanceValue = new JLabel("");

        cardsPanel.add(createCard(
            "TOTAL DEBITS",
            debitValue
        ));

        cardsPanel.add(createCard(
            "TOTAL CREDITS",
            creditValue
        ));

        cardsPanel.add(createCard(
            "ENDING BALANCE",
            balanceValue
        ));

        mainPanel.add(cardsPanel, BorderLayout.CENTER);

        // FOOTER
        JLabel footer = new JLabel(
            "ACCOUNT FINANCIAL OVERVIEW"
        );

        footer.setForeground(
            new Color(200, 225, 210)
        );

        footer.setHorizontalAlignment(
            SwingConstants.CENTER
        );

        mainPanel.add(footer, BorderLayout.SOUTH);

        add(mainPanel);

        accountSelector.addActionListener(
            e -> updateDashboard()
        );

        updateDashboard();
    }

    private JPanel createCard(
        String title,
        JLabel valueLabel
    ) {

        JPanel card = new JPanel(
            new BorderLayout(10, 20)
        );

        card.setBackground(Color.WHITE);

        card.setBorder(
            new EmptyBorder(30, 20, 30, 20)
        );

        JLabel titleLabel = new JLabel(title);

        titleLabel.setFont(
            new Font("Arial", Font.BOLD, 14)
        );

        titleLabel.setForeground(darkGreen);

        valueLabel.setFont(
            new Font("Arial", Font.BOLD, 25)
        );

        valueLabel.setForeground(lightGreen);

        card.add(titleLabel, BorderLayout.NORTH);
        card.add(valueLabel, BorderLayout.CENTER);

        return card;
    }

    private void updateDashboard() {

        int index = accountSelector.getSelectedIndex();

        if (index <= 0) {
            debitValue.setText("");
            creditValue.setText("");
            balanceValue.setText("");
            return;
        }

        debitValue.setText(
            currency.format(accountData[index][0])
        );

        creditValue.setText(
            currency.format(accountData[index][1])
        );

        balanceValue.setText(
            currency.format(accountData[index][2])
        );
    }

    public static void main(String[] args) {

        SwingUtilities.invokeLater(() -> {

            AccountSummaryDashboard dashboard =
                new AccountSummaryDashboard();

            dashboard.setVisible(true);
        });
    }
}
