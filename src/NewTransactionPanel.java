
import javax.swing.*;
import java.awt.*;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.*;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;

public class NewTransactionPanel extends JPanel {

    private final JTextField dateField =
        new JTextField(LocalDate.now().toString());

    private final JTextArea descriptionField =
        new JTextArea(4, 20);

    private final JComboBox<AccountOption> debitBox =
        new JComboBox<>();

    private final JComboBox<AccountOption> creditBox =
        new JComboBox<>();

    private final JTextField amountField = new JTextField();

    private final JButton postButton =
        new JButton("Confirm and Post");

    public NewTransactionPanel() {
        setLayout(new BorderLayout(0, 25));
        setBackground(Color.WHITE);
        setBorder(
            BorderFactory.createEmptyBorder(30, 30, 30, 30)
        );

        JLabel heading = new JLabel("New Transaction");
        heading.setFont(new Font("Arial", Font.BOLD, 24));
        heading.setForeground(new Color(15, 70, 125));

        descriptionField.setLineWrap(true);
        descriptionField.setWrapStyleWord(true);

        JPanel form = new JPanel(new GridBagLayout());
        form.setOpaque(false);

        addField(form, 0, "Date (YYYY-MM-DD):", dateField);
        addField(
            form, 1, "Description:",
            new JScrollPane(descriptionField)
        );
        addField(form, 2, "Debit Account:", debitBox);
        addField(form, 3, "Credit Account:", creditBox);
        addField(form, 4, "Amount (PHP):", amountField);

        JPanel formArea = new JPanel(new BorderLayout());
        formArea.setOpaque(false);
        formArea.add(form, BorderLayout.NORTH);

        JButton clearButton = new JButton("Clear Form");

        JPanel buttons = new JPanel(
            new FlowLayout(FlowLayout.RIGHT)
        );
        buttons.setOpaque(false);
        buttons.add(clearButton);
        buttons.add(postButton);

        add(heading, BorderLayout.NORTH);
        add(formArea, BorderLayout.CENTER);
        add(buttons, BorderLayout.SOUTH);

        clearButton.addActionListener(event -> clearForm());
        postButton.addActionListener(event -> postTransaction());
    }

    private void addField(
        JPanel form,
        int row,
        String label,
        JComponent input
    ) {
        GridBagConstraints constraints = new GridBagConstraints();

        constraints.gridy = row;
        constraints.insets = new Insets(8, 8, 8, 8);
        constraints.anchor = GridBagConstraints.NORTHWEST;

        constraints.gridx = 0;
        constraints.weightx = 0;
        constraints.fill = GridBagConstraints.NONE;
        form.add(new JLabel(label), constraints);

        constraints.gridx = 1;
        constraints.weightx = 1;
        constraints.fill = GridBagConstraints.HORIZONTAL;
        form.add(input, constraints);
    }

    // Called whenever the user opens this screen.
    public void refreshAccounts() {
        List<AccountOption> accounts = new ArrayList<>();

        String sql = """
            SELECT id, code, name
            FROM accounts
            WHERE active = 1
            ORDER BY code
            """;

        try (
            Connection connection = Database.connect();
            PreparedStatement statement =
                connection.prepareStatement(sql);
            ResultSet result = statement.executeQuery()
        ) {
            while (result.next()) {
                accounts.add(new AccountOption(
                    result.getInt("id"),
                    result.getString("code"),
                    result.getString("name")
                ));
            }
        } catch (SQLException exception) {
            postButton.setEnabled(false);
            showError("Could not load accounts.", exception);
            return;
        }

        debitBox.removeAllItems();
        creditBox.removeAllItems();

        for (AccountOption account : accounts) {
            debitBox.addItem(account);
            creditBox.addItem(account);
        }

        debitBox.setSelectedIndex(-1);
        creditBox.setSelectedIndex(-1);
        postButton.setEnabled(accounts.size() >= 2);
    }

    private void postTransaction() {
        LocalDate date;
        BigDecimal amount;
        long centavos;

        String description = descriptionField.getText().trim();

        AccountOption debit =
            (AccountOption) debitBox.getSelectedItem();

        AccountOption credit =
            (AccountOption) creditBox.getSelectedItem();

        if (description.isEmpty() || debit == null || credit == null) {
            showWarning(
                "Enter a description and select debit and credit accounts."
            );
            return;
        }

        if (debit.id == credit.id) {
            showWarning(
                "Debit and credit accounts must be different."
            );
            return;
        }

        try {
            date = LocalDate.parse(dateField.getText().trim());
        } catch (DateTimeParseException exception) {
            showWarning("Enter a valid date using YYYY-MM-DD.");
            return;
        }

        try {
            amount = new BigDecimal(amountField.getText().trim())
                .setScale(2, RoundingMode.UNNECESSARY);

            centavos = amount.movePointRight(2).longValueExact();

            if (centavos <= 0) {
                showWarning("Amount must be greater than zero.");
                return;
            }
        } catch (
            NumberFormatException | ArithmeticException exception
        ) {
            showWarning(
                "Enter a valid amount with at most two decimal places."
                + "\nExample: 1000.00"
            );
            return;
        }

        int answer = JOptionPane.showConfirmDialog(
            this,
            "Date: " + date
                + "\nDescription: " + description
                + "\nDebit: " + debit
                + "\nCredit: " + credit
                + "\nAmount: PHP " + amount.toPlainString()
                + "\n\nPost this transaction?",
            "Confirm Transaction",
            JOptionPane.YES_NO_OPTION
        );

        if (answer != JOptionPane.YES_OPTION) {
            return;
        }

        postButton.setEnabled(false);

        try {
            long transactionId = saveTransaction(
                date, description, debit.id, credit.id, centavos
            );

            clearForm();

            JOptionPane.showMessageDialog(
                this,
                "Transaction #" + transactionId
                    + " posted successfully."
            );
        } catch (SQLException exception) {
            showError("Could not post the transaction.", exception);
        } finally {
            postButton.setEnabled(debitBox.getItemCount() >= 2);
        }
    }

    private long saveTransaction(
        LocalDate date,
        String description,
        int debitId,
        int creditId,
        long centavos
    ) throws SQLException {

        try (Connection connection = Database.connect()) {
            connection.setAutoCommit(false);

            try {
                // Recheck that both accounts are still active.
                String checkSql = """
                    SELECT COUNT(*)
                    FROM accounts
                    WHERE active = 1 AND id IN (?, ?)
                    """;

                try (
                    PreparedStatement check =
                        connection.prepareStatement(checkSql)
                ) {
                    check.setInt(1, debitId);
                    check.setInt(2, creditId);

                    try (ResultSet result = check.executeQuery()) {
                        if (!result.next() || result.getInt(1) != 2) {
                            throw new SQLException(
                                "Select two different active accounts."
                            );
                        }
                    }
                }

                // Insert the transaction and retrieve its PostgreSQL ID.
                String transactionSql = """
                    INSERT INTO transactions (
                        transaction_date, description
                    ) VALUES (?, ?)
                    RETURNING id
                    """;

                long transactionId;

                try (
                    PreparedStatement statement =
                        connection.prepareStatement(transactionSql)
                ) {
                    statement.setString(1, date.toString());
                    statement.setString(2, description);

                    try (ResultSet result = statement.executeQuery()) {
                        if (!result.next()) {
                            throw new SQLException(
                                "Could not retrieve the transaction ID."
                            );
                        }

                        transactionId = result.getLong("id");
                    }
                }

                // Insert the debit and credit journal lines.
                String lineSql = """
                    INSERT INTO journal_lines (
                        transaction_id, account_id,
                        debit_centavos, credit_centavos
                    ) VALUES (?, ?, ?, ?)
                    """;

                try (
                    PreparedStatement statement =
                        connection.prepareStatement(lineSql)
                ) {
                    // Debit entry
                    statement.setLong(1, transactionId);
                    statement.setInt(2, debitId);
                    statement.setLong(3, centavos);
                    statement.setLong(4, 0);
                    statement.executeUpdate();

                    // Credit entry
                    statement.setLong(1, transactionId);
                    statement.setInt(2, creditId);
                    statement.setLong(3, 0);
                    statement.setLong(4, centavos);
                    statement.executeUpdate();
                }

                connection.commit();
                return transactionId;

            } catch (SQLException exception) {
                try {
                    connection.rollback();
                } catch (SQLException rollbackException) {
                    exception.addSuppressed(rollbackException);
                }

                throw exception;
            }
        }
    }

    private void clearForm() {
        dateField.setText(LocalDate.now().toString());
        descriptionField.setText("");
        amountField.setText("");
        debitBox.setSelectedIndex(-1);
        creditBox.setSelectedIndex(-1);
    }

    private void showWarning(String message) {
        JOptionPane.showMessageDialog(
            this,
            message,
            "Check Transaction",
            JOptionPane.WARNING_MESSAGE
        );
    }

    private void showError(
        String message,
        SQLException exception
    ) {
        exception.printStackTrace();

        JOptionPane.showMessageDialog(
            this,
            message + "\n" + exception.getMessage(),
            "Database Error",
            JOptionPane.ERROR_MESSAGE
        );
    }

    private static class AccountOption {
        private final int id;
        private final String code;
        private final String name;

        AccountOption(int id, String code, String name) {
            this.id = id;
            this.code = code;
            this.name = name;
        }

        @Override
        public String toString() {
            return code + " - " + name;
        }
    }
}
