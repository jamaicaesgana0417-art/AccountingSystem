import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableRowSorter;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.io.BufferedWriter;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.*;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;

public class TransactionsPanel extends JPanel {

    private final JTextField searchField = new JTextField(16);
    private final JTextField startDateField = new JTextField(10);
    private final JTextField endDateField = new JTextField(10);

    private final JComboBox<AccountOption> accountBox =
        new JComboBox<>();

    private final JLabel countLabel =
        new JLabel("No transactions loaded.");

    private final DefaultTableModel model =
        new DefaultTableModel(
            new String[] {
                "Reference",
                "Date",
                "Description",
                "Debit Account",
                "Credit Account",
                "Amount (PHP)"
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

                if (column == 5) {
                    return BigDecimal.class;
                }

                return String.class;
            }
        };

    private final JTable table = new JTable(model);

    public TransactionsPanel() {
        setLayout(new BorderLayout(0, 20));
        setBackground(Color.WHITE);
        setBorder(
            BorderFactory.createEmptyBorder(30, 30, 30, 30)
        );

        JLabel heading = new JLabel("Transactions");
        heading.setFont(new Font("Arial", Font.BOLD, 24));
        heading.setForeground(new Color(15, 70, 125));

        JPanel filterFields = new JPanel(new GridLayout(0, 2, 12, 8));
        filterFields.setOpaque(false);

        filterFields.add(new JLabel("Search:"));
        filterFields.add(searchField);

        filterFields.add(new JLabel("From (YYYY-MM-DD, optional):"));
        filterFields.add(startDateField);

        filterFields.add(new JLabel("To (YYYY-MM-DD, optional):"));
        filterFields.add(endDateField);

        filterFields.add(new JLabel("Debit or Credit Account:"));
        filterFields.add(accountBox);

        JButton applyButton = new JButton("Apply Filters");
        JButton clearButton = new JButton("Clear Filters");
        JButton refreshButton = new JButton("Refresh");
        JButton exportButton = new JButton("Export CSV");

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.LEFT));
        actions.setOpaque(false);
        actions.add(applyButton);
        actions.add(clearButton);
        actions.add(refreshButton);
        actions.add(exportButton);

        JPanel filters = new JPanel(new BorderLayout(0, 8));
        filters.setOpaque(false);
        filters.add(filterFields, BorderLayout.CENTER);
        filters.add(actions, BorderLayout.SOUTH);

        JPanel top = new JPanel(new BorderLayout(0, 15));
        top.setOpaque(false);
        top.add(heading, BorderLayout.NORTH);
        top.add(filters, BorderLayout.CENTER);

        table.setRowHeight(28);
        table.setFillsViewportHeight(true);
        table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        table.getTableHeader().setReorderingAllowed(false);

        TableRowSorter<DefaultTableModel> sorter =
            new TableRowSorter<>(model);

        // Keep updated results in their default SQL order.
        sorter.setSortsOnUpdates(true);
        table.setRowSorter(sorter);

        int[] widths = {85, 110, 250, 180, 180, 130};

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

        table.getColumnModel()
            .getColumn(5)
            .setCellRenderer(moneyRenderer);

        add(top, BorderLayout.NORTH);
        add(new JScrollPane(table), BorderLayout.CENTER);
        add(countLabel, BorderLayout.SOUTH);

        accountBox.addItem(new AccountOption(null, "All Accounts"));

        applyButton.addActionListener(event -> loadTransactions());
        searchField.addActionListener(event -> loadTransactions());
        startDateField.addActionListener(event -> loadTransactions());
        endDateField.addActionListener(event -> loadTransactions());

        refreshButton.addActionListener(event -> refreshTransactions());

        clearButton.addActionListener(event -> {
            searchField.setText("");
            startDateField.setText("");
            endDateField.setText("");
            accountBox.setSelectedIndex(0);
            loadTransactions();
        });

        exportButton.addActionListener(event -> exportCsv());
    }

    // App.java already calls this when opening Transactions.
    public void refreshTransactions() {
        if (loadAccounts()) {
            loadTransactions();
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
            showDatabaseError("Could not load accounts.", exception);
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

    private void loadTransactions() {
        LocalDate start;
        LocalDate end;

        try {
            start = parseOptionalDate(startDateField.getText());
            end = parseOptionalDate(endDateField.getText());

        } catch (DateTimeParseException exception) {
            invalidateResults("Enter valid dates using YYYY-MM-DD.");

            JOptionPane.showMessageDialog(
                this,
                "Enter valid dates using YYYY-MM-DD."
                    + "\nLeave a date blank if no limit is needed.",
                "Invalid Date",
                JOptionPane.WARNING_MESSAGE
            );
            return;
        }

        if (start != null && end != null && start.isAfter(end)) {
            invalidateResults("Start date must not be after end date.");

            JOptionPane.showMessageDialog(
                this,
                "Start date must not be after end date.",
                "Invalid Date Range",
                JOptionPane.WARNING_MESSAGE
            );
            return;
        }

        AccountOption account =
            (AccountOption) accountBox.getSelectedItem();

        String keyword = searchField.getText().trim();

        StringBuilder sql = new StringBuilder("""
            SELECT
                t.id,
                t.transaction_date,
                t.description,
                da.code || ' - ' || da.name AS debit_account,
                ca.code || ' - ' || ca.name AS credit_account,
                d.debit_centavos AS amount_centavos
            FROM transactions t
            JOIN journal_lines d
                ON d.transaction_id = t.id
                AND d.debit_centavos > 0
            JOIN accounts da ON da.id = d.account_id
            JOIN journal_lines c
                ON c.transaction_id = t.id
                AND c.credit_centavos > 0
            JOIN accounts ca ON ca.id = c.account_id
            WHERE 1 = 1
            """);

        List<Object> parameters = new ArrayList<>();

        if (start != null) {
            sql.append(" AND t.transaction_date >= ? ");
            parameters.add(start.toString());
        }

        if (end != null) {
            sql.append(" AND t.transaction_date <= ? ");
            parameters.add(end.toString());
        }

        if (account != null && account.id != null) {
            sql.append(" AND (da.id = ? OR ca.id = ?) ");
            parameters.add(account.id);
            parameters.add(account.id);
        }

        if (!keyword.isEmpty()) {
            sql.append("""
                 AND (
                    instr(lower(CAST(t.id AS TEXT)), lower(?)) > 0
                    OR instr(lower(t.description), lower(?)) > 0
                    OR instr(lower(da.code || ' ' || da.name), lower(?)) > 0
                    OR instr(lower(ca.code || ' ' || ca.name), lower(?)) > 0
                 )
                """);

            for (int i = 0; i < 4; i++) {
                parameters.add(keyword);
            }
        }

        sql.append(" ORDER BY t.transaction_date DESC, t.id DESC ");

        List<Object[]> rows = new ArrayList<>();
        BigDecimal total = new BigDecimal("0.00");

        try (
            Connection connection = Database.connect();
            PreparedStatement statement =
                connection.prepareStatement(sql.toString())
        ) {
            for (int i = 0; i < parameters.size(); i++) {
                Object parameter = parameters.get(i);

                if (parameter instanceof Integer) {
                    statement.setInt(i + 1, (Integer) parameter);
                } else {
                    statement.setString(i + 1, parameter.toString());
                }
            }

            try (ResultSet result = statement.executeQuery()) {
                while (result.next()) {
                    BigDecimal amount = BigDecimal.valueOf(
                        result.getLong("amount_centavos"), 2
                    );

                    rows.add(new Object[] {
                        result.getLong("id"),
                        result.getString("transaction_date"),
                        result.getString("description"),
                        result.getString("debit_account"),
                        result.getString("credit_account"),
                        amount
                    });

                    total = total.add(amount);
                }
            }

        } catch (SQLException exception) {
            showDatabaseError("Could not load transactions.", exception);
            return;
        }

        model.setRowCount(0);

        for (Object[] row : rows) {
            model.addRow(row);
        }

        countLabel.setForeground(Color.DARK_GRAY);
        countLabel.setText(
            "Displayed transactions: " + rows.size()
                + " | Total amount: PHP " + total.toPlainString()
        );
    }

    private LocalDate parseOptionalDate(String text) {
        String value = text.trim();

        return value.isEmpty() ? null : LocalDate.parse(value);
    }

    private void exportCsv() {
        if (table.getRowCount() == 0) {
            JOptionPane.showMessageDialog(
                this,
                "There are no displayed transactions to export."
            );
            return;
        }

        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Export Displayed Transactions");
        chooser.setFileFilter(
            new FileNameExtensionFilter("CSV files (*.csv)", "csv")
        );
        chooser.setSelectedFile(
            new java.io.File("transactions.csv")
        );

        if (chooser.showSaveDialog(this)
                != JFileChooser.APPROVE_OPTION) {
            return;
        }

        Path path = chooser.getSelectedFile().toPath();

        if (!path.getFileName().toString()
                .toLowerCase(java.util.Locale.ROOT)
                .endsWith(".csv")) {
            path = path.resolveSibling(
                path.getFileName().toString() + ".csv"
            );
        }

        if (Files.exists(path)) {
            int answer = JOptionPane.showConfirmDialog(
                this,
                "This file already exists. Replace it?",
                "Confirm Replacement",
                JOptionPane.YES_NO_OPTION
            );

            if (answer != JOptionPane.YES_OPTION) {
                return;
            }
        }

        try (
            BufferedWriter writer = Files.newBufferedWriter(
                path, StandardCharsets.UTF_8
            )
        ) {
            // Helps Excel recognize UTF-8 characters.
            writer.write('\uFEFF');

            for (int column = 0; column < table.getColumnCount(); column++) {
                if (column > 0) {
                    writer.write(",");
                }

                writer.write(csvCell(table.getColumnName(column)));
            }

            writer.write("\r\n");

            // JTable uses the visible row order, including user sorting.
            for (int row = 0; row < table.getRowCount(); row++) {
                for (
                    int column = 0;
                    column < table.getColumnCount();
                    column++
                ) {
                    if (column > 0) {
                        writer.write(",");
                    }

                    Object value = table.getValueAt(row, column);
                    String text;

                    if (value instanceof BigDecimal) {
                        text = ((BigDecimal) value)
                            .setScale(2)
                            .toPlainString();
                    } else {
                        text = value == null ? "" : value.toString();
                    }

                    writer.write(csvCell(text));
                }

                writer.write("\r\n");
            }

        } catch (IOException exception) {
            exception.printStackTrace();

            JOptionPane.showMessageDialog(
                this,
                "Could not export the file:\n"
                    + exception.getMessage(),
                "Export Error",
                JOptionPane.ERROR_MESSAGE
            );
            return;
        }

        JOptionPane.showMessageDialog(
            this,
            "Exported " + table.getRowCount()
                + " transactions to:\n" + path.toAbsolutePath()
        );
    }

    private String csvCell(String text) {
        // Prevent text from being interpreted as a spreadsheet formula.
        String leadingTrimmed = text.stripLeading();

        if (!leadingTrimmed.isEmpty()
                && "=+-@".indexOf(leadingTrimmed.charAt(0)) >= 0) {
            text = "'" + text;
        }

        return "\"" + text.replace("\"", "\"\"") + "\"";
    }

    private void invalidateResults(String message) {
        model.setRowCount(0);
        countLabel.setForeground(Color.RED);
        countLabel.setText(message);
    }

    private void showDatabaseError(
        String message,
        SQLException exception
    ) {
        exception.printStackTrace();
        invalidateResults("Results unavailable. Please refresh.");

        JOptionPane.showMessageDialog(
            this,
            message + "\n" + exception.getMessage(),
            "Database Error",
            JOptionPane.ERROR_MESSAGE
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