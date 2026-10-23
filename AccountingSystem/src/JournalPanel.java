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

        applyButton.addActionListener(event -> refreshJournal());
        refreshButton.addActionListener(event -> refreshJournal());

        searchField.addActionListener(event -> refreshJournal());
        startField.addActionListener(event -> refreshJournal());
        endField.addActionListener(event -> refreshJournal());

        clearButton.addActionListener(event -> {
            searchField.setText("");
            startField.setText("");
            endField.setText("");
            refreshJournal();
        });

        exportButton.addActionListener(event ->
            CsvExporter.export(this, table, "general_journal.csv")
        );
    }

    public void refreshJournal() {
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

        String keyword = searchField.getText().trim();

        StringBuilder sql = new StringBuilder("""
            SELECT
                t.id AS transaction_id,
                t.transaction_date,
                t.description,
                a.code || ' - ' || a.name AS account_name,
                j.debit_centavos,
                j.credit_centavos
            FROM journal_lines j
            JOIN transactions t ON t.id = j.transaction_id
            JOIN accounts a ON a.id = j.account_id
            WHERE 1 = 1
            """);

        List<String> parameters = new ArrayList<>();

        if (start != null) {
            sql.append(" AND t.transaction_date >= ? ");
            parameters.add(start.toString());
        }

        if (end != null) {
            sql.append(" AND t.transaction_date <= ? ");
            parameters.add(end.toString());
        }

        if (!keyword.isEmpty()) {
            sql.append("""
                 AND (
                    instr(lower(CAST(t.id AS TEXT)), lower(?)) > 0
                    OR instr(lower(t.description), lower(?)) > 0
                    OR EXISTS (
                        SELECT 1
                        FROM journal_lines matching_line
                        JOIN accounts matching_account
                            ON matching_account.id =
                               matching_line.account_id
                        WHERE matching_line.transaction_id = t.id
                        AND instr(
                            lower(
                                matching_account.code || ' ' ||
                                matching_account.name || ' ' ||
                                matching_account.type
                            ),
                            lower(?)
                        ) > 0
                    )
                 )
                """);

            parameters.add(keyword);
            parameters.add(keyword);
            parameters.add(keyword);
        }

        sql.append("""
             ORDER BY
                t.transaction_date,
                t.id,
                CASE WHEN j.debit_centavos > 0 THEN 0 ELSE 1 END,
                j.id
            """);

        List<Object[]> rows = new ArrayList<>();
        BigDecimal totalDebit = new BigDecimal("0.00");
        BigDecimal totalCredit = new BigDecimal("0.00");

        try (
            Connection connection = Database.connect();
            PreparedStatement statement =
                connection.prepareStatement(sql.toString())
        ) {
            for (int i = 0; i < parameters.size(); i++) {
                statement.setString(i + 1, parameters.get(i));
            }

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

    private LocalDate optionalDate(String text) {
        String value = text.trim();

        return value.isEmpty() ? null : LocalDate.parse(value);
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
}