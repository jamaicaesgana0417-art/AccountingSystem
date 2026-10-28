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

public class JournalPanel extends JPanel {

    private final JTextField searchField = new JTextField(16);
    private final JTextField startField = new JTextField(10);
    private final JTextField endField = new JTextField(10);

    private final JComboBox<AccountOption> accountBox =
        new JComboBox<>();

    private final JComboBox<String> typeBox =
        new JComboBox<>(FilterSupport.typeChoices());

    private final JLabel totalsLabel =
        new JLabel("No entries loaded.");

    private final DefaultTableModel model =
        new DefaultTableModel(
            new String[] {
                "Reference", "Date", "Description",
                "Account", "Debit (PHP)", "Credit (PHP)"
            },
            0
        ) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }

            @Override
            public Class<?> getColumnClass(int column) {
                if (column == 0) {
                    return Long.class;
                }

                if (column == 4 || column == 5) {
                    return BigDecimal.class;
                }

                return String.class;
            }
        };

    private final JTable table = new JTable(model);

    public JournalPanel() {
        setLayout(new BorderLayout(0, 20));
        setBackground(Color.WHITE);
        setBorder(
            BorderFactory.createEmptyBorder(30, 30, 30, 30)
        );

        JLabel heading = new JLabel("General Journal");
        heading.setFont(new Font("Arial", Font.BOLD, 24));
        heading.setForeground(new Color(15, 70, 125));

        JPanel fields = new JPanel(new GridLayout(0, 2, 10, 8));
        fields.setOpaque(false);

        fields.add(new JLabel("Search:"));
        fields.add(searchField);

        fields.add(new JLabel("From (YYYY-MM-DD, optional):"));
        fields.add(startField);

        fields.add(new JLabel("To (YYYY-MM-DD, optional):"));
        fields.add(endField);

        fields.add(new JLabel("Account (entries that use it):"));
        fields.add(accountBox);

        fields.add(new JLabel("Account Type:"));
        fields.add(typeBox);

        JButton applyButton = new JButton("Apply Filters");
        JButton clearButton = new JButton("Clear Filters");
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

        // Keep chronological order and matching journal lines together.
        int[] widths = {85, 110, 230, 200, 130, 130};

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
                        BigDecimal amount = (BigDecimal) value;

                        setText(
                            amount.signum() == 0
                                ? ""
                                : amount.setScale(2).toPlainString()
                        );
                    } else {
                        super.setValue(value);
                    }
                }
            };

        moneyRenderer.setHorizontalAlignment(SwingConstants.RIGHT);

        table.getColumnModel().getColumn(4)
            .setCellRenderer(moneyRenderer);

        table.getColumnModel().getColumn(5)
            .setCellRenderer(moneyRenderer);

        add(top, BorderLayout.NORTH);
        add(new JScrollPane(table), BorderLayout.CENTER);
        add(totalsLabel, BorderLayout.SOUTH);

        accountBox.addItem(new AccountOption(null, "All Accounts"));

        applyButton.addActionListener(event -> loadJournal());
        refreshButton.addActionListener(event -> refreshJournal());

        searchField.addActionListener(event -> loadJournal());
        startField.addActionListener(event -> loadJournal());
        endField.addActionListener(event -> loadJournal());

        clearButton.addActionListener(event -> {
            searchField.setText("");
            startField.setText("");
            endField.setText("");
            accountBox.setSelectedIndex(0);
            typeBox.setSelectedIndex(0);
            loadJournal();
        });

        exportButton.addActionListener(event ->
            CsvExporter.export(this, table, "general_journal.csv")
        );
    }

    // App.java calls this when opening General Journal.
    public void refreshJournal() {
        if (loadAccounts()) {
            loadJournal();
        }
    }

    private boolean loadAccounts() {
        AccountOption previous =
            (AccountOption) accountBox.getSelectedItem();

        List<AccountOption> accounts = new ArrayList<>();

        String sql = """
            SELECT id, code, name
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
                    result.getString("code")
                        + " - " + result.getString("name")
                ));
            }

        } catch (SQLException exception) {
            exception.printStackTrace();
            invalidateResults("Journal unavailable. Please refresh.");

            JOptionPane.showMessageDialog(
                this,
                "Could not load accounts:\n" + exception.getMessage(),
                "Database Error",
                JOptionPane.ERROR_MESSAGE
            );
            return false;
        }

        accountBox.removeAllItems();

        AccountOption allAccounts =
            new AccountOption(null, "All Accounts");

        accountBox.addItem(allAccounts);

        AccountOption selection = allAccounts;

        for (AccountOption account : accounts) {
            accountBox.addItem(account);

            if (previous != null
                    && previous.id != null
                    && previous.id.equals(account.id)) {
                selection = account;
            }
        }

        accountBox.setSelectedItem(selection);
        return true;
    }

    private void loadJournal() {
        LocalDate start;
        LocalDate end;

        try {
            start = FilterSupport.parseOptionalDate(startField.getText());
            end = FilterSupport.parseOptionalDate(endField.getText());

        } catch (DateTimeParseException exception) {
            invalidateResults("Invalid date. Use YYYY-MM-DD.");
            showWarning("Enter valid dates using YYYY-MM-DD.");
            return;
        }

        String rangeError = FilterSupport.validateRange(start, end);

        if (rangeError != null) {
            invalidateResults("Invalid date range.");
            showWarning(rangeError);
            return;
        }

        AccountOption account =
            (AccountOption) accountBox.getSelectedItem();

        FilterSupport.Query query = buildQuery(
            start,
            end,
            searchField.getText(),
            account == null ? null : account.id,
            FilterSupport.typeOrNull(typeBox.getSelectedItem())
        );

        List<Object[]> rows = new ArrayList<>();
        BigDecimal totalDebit = new BigDecimal("0.00");
        BigDecimal totalCredit = new BigDecimal("0.00");

        try (
            Connection connection = Database.connect();
            PreparedStatement statement =
                connection.prepareStatement(query.sql)
        ) {
            FilterSupport.bind(statement, query.params);

            try (ResultSet result = statement.executeQuery()) {
                while (result.next()) {
                    BigDecimal debit = BigDecimal.valueOf(
                        result.getLong("debit_centavos"), 2
                    );

                    BigDecimal credit = BigDecimal.valueOf(
                        result.getLong("credit_centavos"), 2
                    );

                    rows.add(new Object[] {
                        result.getLong("transaction_id"),
                        result.getString("transaction_date"),
                        result.getString("description"),
                        result.getString("account_name"),
                        debit,
                        credit
                    });

                    totalDebit = totalDebit.add(debit);
                    totalCredit = totalCredit.add(credit);
                }
            }

        } catch (SQLException exception) {
            exception.printStackTrace();
            invalidateResults("Journal unavailable. Please refresh.");

            JOptionPane.showMessageDialog(
                this,
                "Could not load journal entries:\n"
                    + exception.getMessage(),
                "Database Error",
                JOptionPane.ERROR_MESSAGE
            );
            return;
        }

        model.setRowCount(0);

        for (Object[] row : rows) {
            model.addRow(row);
        }

        boolean balanced = totalDebit.compareTo(totalCredit) == 0;

        totalsLabel.setForeground(
            balanced ? Color.DARK_GRAY : Color.RED
        );

        totalsLabel.setText(
            "<html>Displayed journal lines: " + rows.size()
                + "<br>Total Debit: PHP " + totalDebit.toPlainString()
                + " | Total Credit: PHP " + totalCredit.toPlainString()
                + "<br>"
                + (rows.isEmpty()
                    ? "No matching entries."
                    : balanced
                        ? "Displayed debits and credits are equal."
                        : "Warning: Displayed debits and credits differ.")
                + "</html>"
        );
    }

    /**
     * Builds the General Journal query (package-private + static so it can
     * be tested without a window or database).
     *
     * Filters are combined with AND:
     *  - dates are inclusive on both ends
     *  - account / account type keep every line of any entry that uses a
     *    matching account, so each entry still shows both its debit and
     *    credit lines and the debit/credit totals stay balanced
     *  - keyword matches reference, description, or any account on the entry
     */
    static FilterSupport.Query buildQuery(
        LocalDate start,
        LocalDate end,
        String keyword,
        Integer accountId,
        String accountType
    ) {
        StringBuilder sql = new StringBuilder("""
            SELECT
                t.id AS transaction_id,
                CAST(t.transaction_date AS DATE) AS transaction_date,
                t.description,
                a.code || ' - ' || a.name AS account_name,
                j.debit_centavos,
                j.credit_centavos
            FROM journal_lines j
            JOIN transactions t ON t.id = j.transaction_id
            JOIN accounts a ON a.id = j.account_id
            WHERE 1 = 1
            """);

        List<Object> params = FilterSupport.newParams();

        FilterSupport.appendDateRange(
            sql, params, "t.transaction_date", start, end
        );

        if (accountId != null || accountType != null) {
            sql.append("""
                 AND EXISTS (
                    SELECT 1
                    FROM journal_lines f
                    JOIN accounts fa ON fa.id = f.account_id
                    WHERE f.transaction_id = t.id
                """);

            FilterSupport.appendEquals(sql, params, "fa.id", accountId);
            FilterSupport.appendEquals(sql, params, "fa.type", accountType);

            sql.append(" ) ");
        }

        String trimmed = FilterSupport.normalizeKeyword(keyword);

        if (!trimmed.isEmpty()) {
            sql.append(" AND ( ")
                .append(FilterSupport.keywordTest("t.id"))
                .append(" OR ")
                .append(FilterSupport.keywordTest("t.description"))
                .append(" OR EXISTS ( ")
                .append("SELECT 1 FROM journal_lines k ")
                .append("JOIN accounts ka ON ka.id = k.account_id ")
                .append("WHERE k.transaction_id = t.id AND ")
                .append(FilterSupport.keywordTest(
                    "ka.code || ' ' || ka.name || ' ' || ka.type"
                ))
                .append(" ) ) ");

            params.add(trimmed);
            params.add(trimmed);
            params.add(trimmed);
        }

        sql.append("""
             ORDER BY
                t.transaction_date,
                t.id,
                CASE WHEN j.debit_centavos > 0 THEN 0 ELSE 1 END,
                j.id
            """);

        return new FilterSupport.Query(sql.toString(), params);
    }

    private void invalidateResults(String message) {
        model.setRowCount(0);
        totalsLabel.setText(message);
        totalsLabel.setForeground(Color.RED);
    }

    private void showWarning(String message) {
        JOptionPane.showMessageDialog(
            this,
            message,
            "Check Filters",
            JOptionPane.WARNING_MESSAGE
        );
    }

    private static class AccountOption {
        private final Integer id;
        private final String label;

        AccountOption(Integer id, String label) {
            this.id = id;
            this.label = label;
        }

        @Override
        public String toString() {
            return label;
        }
    }
}
