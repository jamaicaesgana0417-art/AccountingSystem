import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.*;
import java.math.BigDecimal;
import java.sql.*;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;

public class LedgerPanel extends JPanel {

    private final JComboBox<AccountOption> accountBox =
        new JComboBox<>();

    private final JTextField startField = new JTextField(10);
    private final JTextField endField = new JTextField(10);

    private final JLabel summaryLabel =
        new JLabel("Select an account.");

    private boolean loadingAccounts = false;

    private final DefaultTableModel model =
        new DefaultTableModel(
            new String[] {
                "Account",
                "Reference",
                "Date",
                "Description",
                "Debit (PHP)",
                "Credit (PHP)",
                "Balance (PHP)",
                "Side"
            },
            0
        ) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }

            @Override
            public Class<?> getColumnClass(int column) {
                if (column == 1) {
                    return Long.class;
                }

                if (column >= 4 && column <= 6) {
                    return BigDecimal.class;
                }

                return String.class;
            }
        };

    private final JTable table = new JTable(model);

    public LedgerPanel() {
        setLayout(new BorderLayout(0, 20));
        setBackground(Color.WHITE);
        setBorder(
            BorderFactory.createEmptyBorder(30, 30, 30, 30)
        );

        JLabel heading = new JLabel("General Ledger");
        heading.setFont(new Font("Arial", Font.BOLD, 24));
        heading.setForeground(new Color(15, 70, 125));

        JPanel fields = new JPanel(new GridLayout(0, 2, 10, 8));
        fields.setOpaque(false);

        fields.add(new JLabel("Account:"));
        fields.add(accountBox);

        fields.add(new JLabel("From (YYYY-MM-DD, optional):"));
        fields.add(startField);

        fields.add(new JLabel("To (YYYY-MM-DD, optional):"));
        fields.add(endField);

        JButton applyButton = new JButton("Apply Filters");
        JButton clearButton = new JButton("Clear Dates");
        JButton refreshButton = new JButton("Refresh");
        JButton exportButton = new JButton("Export CSV");

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.LEFT));
        buttons.setOpaque(false);
        buttons.add(applyButton);
        buttons.add(clearButton);
        buttons.add(refreshButton);
        buttons.add(exportButton);

        JPanel filters = new JPanel(new BorderLayout(0, 8));
        filters.setOpaque(false);
        filters.add(fields, BorderLayout.CENTER);
        filters.add(buttons, BorderLayout.SOUTH);

        JPanel top = new JPanel(new BorderLayout(0, 15));
        top.setOpaque(false);
        top.add(heading, BorderLayout.NORTH);
        top.add(filters, BorderLayout.CENTER);

        table.setRowHeight(28);
        table.setFillsViewportHeight(true);
        table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        table.getTableHeader().setReorderingAllowed(false);

        // Keep chronological order so running balances remain meaningful.
        int[] widths = {200, 85, 110, 240, 120, 120, 130, 60};

        for (int i = 0; i < widths.length; i++) {
            table.getColumnModel()
                .getColumn(i)
                .setPreferredWidth(widths[i]);
        }

        DefaultTableCellRenderer moneyRenderer =
            new DefaultTableCellRenderer() {
                @Override
                protected void setValue(Object value) {
                    if (value instanceof BigDecimal) {
                        setText(
                            ((BigDecimal) value)
                                .setScale(2)
                                .toPlainString()
                        );
                    } else {
                        super.setValue(value);
                    }
                }
            };

        moneyRenderer.setHorizontalAlignment(SwingConstants.RIGHT);

        for (int column = 4; column <= 6; column++) {
            table.getColumnModel()
                .getColumn(column)
                .setCellRenderer(moneyRenderer);
        }

        add(top, BorderLayout.NORTH);
        add(new JScrollPane(table), BorderLayout.CENTER);
        add(summaryLabel, BorderLayout.SOUTH);

        accountBox.addActionListener(event -> {
            if (!loadingAccounts) {
                loadLedger();
            }
        });

        applyButton.addActionListener(event -> loadLedger());
        refreshButton.addActionListener(event -> refreshAccounts());

        startField.addActionListener(event -> loadLedger());
        endField.addActionListener(event -> loadLedger());

        clearButton.addActionListener(event -> {
            startField.setText("");
            endField.setText("");
            loadLedger();
        });

        exportButton.addActionListener(event ->
            CsvExporter.export(this, table, "general_ledger.csv")
        );
    }

    // App.java already calls this when opening General Ledger.
    public void refreshAccounts() {
        AccountOption previous =
            (AccountOption) accountBox.getSelectedItem();

        List<AccountOption> accounts = new ArrayList<>();

        String sql = """
            SELECT id, code, name, type
            FROM accounts
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
                    result.getString("name"),
                    result.getString("type")
                ));
            }

        } catch (SQLException exception) {
            showDatabaseError("Could not load accounts.", exception);
            return;
        }

        loadingAccounts = true;

        try {
            accountBox.removeAllItems();

            AccountOption selected = null;

            for (AccountOption account : accounts) {
                accountBox.addItem(account);

                if (previous != null && previous.id == account.id) {
                    selected = account;
                }
            }

            if (selected != null) {
                accountBox.setSelectedItem(selected);
            }

        } finally {
            loadingAccounts = false;
        }

        loadLedger();
    }

    private void loadLedger() {
        AccountOption account =
            (AccountOption) accountBox.getSelectedItem();

        if (account == null) {
            invalidateResults("No accounts available.");
            return;
        }

        LocalDate start;
        LocalDate end;

        try {
            start = optionalDate(startField.getText());
            end = optionalDate(endField.getText());

        } catch (DateTimeParseException exception) {
            invalidateResults("Invalid date. Use YYYY-MM-DD.");
            showWarning("Enter valid dates using YYYY-MM-DD.");
            return;
        }

        if (start != null && end != null && start.isAfter(end)) {
            invalidateResults("Invalid date range.");
            showWarning("Start date must not be after end date.");
            return;
        }

        /*
         * Read entries through the end date.
         * Entries before the start date form the opening balance.
         */
        String sql = """
            SELECT
                t.id AS transaction_id,
                t.transaction_date,
                t.description,
                j.debit_centavos,
                j.credit_centavos
            FROM journal_lines j
            JOIN transactions t ON t.id = j.transaction_id
            WHERE j.account_id = ?
            """;

        if (end != null) {
            sql += " AND t.transaction_date <= ? ";
        }

        sql += " ORDER BY t.transaction_date, t.id, j.id ";

        List<Object[]> entries = new ArrayList<>();

        BigDecimal opening = zero();
        BigDecimal periodDebit = zero();
        BigDecimal periodCredit = zero();

        // Positive = debit balance. Negative = credit balance.
        BigDecimal runningBalance = zero();

        try (
            Connection connection = Database.connect();
            PreparedStatement statement =
                connection.prepareStatement(sql)
        ) {
            statement.setInt(1, account.id);

            if (end != null) {
                statement.setString(2, end.toString());
            }

            try (ResultSet result = statement.executeQuery()) {
                while (result.next()) {
                    LocalDate entryDate = LocalDate.parse(
                        result.getString("transaction_date")
                    );

                    BigDecimal debit = BigDecimal.valueOf(
                        result.getLong("debit_centavos"), 2
                    );

                    BigDecimal credit = BigDecimal.valueOf(
                        result.getLong("credit_centavos"), 2
                    );

                    runningBalance = runningBalance
                        .add(debit)
                        .subtract(credit);

                    if (start != null && entryDate.isBefore(start)) {
                        opening = runningBalance;
                        continue;
                    }

                    periodDebit = periodDebit.add(debit);
                    periodCredit = periodCredit.add(credit);

                    entries.add(new Object[] {
                        account.label(),
                        result.getLong("transaction_id"),
                        entryDate.toString(),
                        result.getString("description"),
                        debit,
                        credit,
                        runningBalance.abs(),
                        side(runningBalance)
                    });
                }
            }

        } catch (SQLException exception) {
            showDatabaseError("Could not load ledger entries.", exception);
            return;

        } catch (DateTimeParseException exception) {
            invalidateResults("A saved transaction has an invalid date.");
            showWarning(
                "A saved transaction date is invalid."
                    + "\nStored dates must use YYYY-MM-DD."
            );
            return;
        }

        model.setRowCount(0);

        // Include opening balance in the table and CSV.
        model.addRow(new Object[] {
            account.label(),
            null,
            start == null ? "" : start.toString(),
            start == null
                ? "Opening balance before first recorded entry"
                : "Opening balance before " + start,
            zero(),
            zero(),
            opening.abs(),
            side(opening)
        });

        for (Object[] row : entries) {
            model.addRow(row);
        }

        // Include closing balance in the table and CSV.
        model.addRow(new Object[] {
            account.label(),
            null,
            end == null ? "" : end.toString(),
            end == null
                ? "Closing balance through latest recorded entry"
                : "Closing balance through " + end,
            null,
            null,
            runningBalance.abs(),
            side(runningBalance)
        });

        String normalSide =
            account.type.equals("ASSET")
                || account.type.equals("EXPENSE")
                ? "Debit"
                : "Credit";

        summaryLabel.setForeground(Color.DARK_GRAY);
        summaryLabel.setText(
            "<html>Account: " + account.code
                + " | Type: " + account.type
                + " | Normal side: " + normalSide
                + "<br>Period: "
                + (start == null ? "Beginning" : start.toString())
                + " to "
                + (end == null ? "Latest entry" : end.toString())
                + "<br>Opening: PHP " + opening.abs().toPlainString()
                + " " + side(opening)
                + "<br>Period Debit: PHP " + periodDebit.toPlainString()
                + " | Period Credit: PHP " + periodCredit.toPlainString()
                + "<br>Closing: PHP "
                + runningBalance.abs().toPlainString()
                + " " + side(runningBalance)
                + " | Entries: " + entries.size()
                + "</html>"
        );
    }

    private LocalDate optionalDate(String text) {
        String value = text.trim();
        return value.isEmpty() ? null : LocalDate.parse(value);
    }

    private static BigDecimal zero() {
        return new BigDecimal("0.00");
    }

    private static String side(BigDecimal balance) {
        if (balance.signum() > 0) {
            return "Dr";
        }

        if (balance.signum() < 0) {
            return "Cr";
        }

        return "";
    }

    private void invalidateResults(String message) {
        model.setRowCount(0);
        summaryLabel.setText(message);
        summaryLabel.setForeground(Color.RED);
    }

    private void showWarning(String message) {
        JOptionPane.showMessageDialog(
            this,
            message,
            "Check Ledger",
            JOptionPane.WARNING_MESSAGE
        );
    }

    private void showDatabaseError(
        String message,
        SQLException exception
    ) {
        exception.printStackTrace();
        invalidateResults("Ledger unavailable. Please refresh.");

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
        private final String type;

        AccountOption(int id, String code, String name, String type) {
            this.id = id;
            this.code = code;
            this.name = name;
            this.type = type;
        }

        String label() {
            return code + " - " + name;
        }

        @Override
        public String toString() {
            return label() + " [" + type + "]";
        }
    }
}