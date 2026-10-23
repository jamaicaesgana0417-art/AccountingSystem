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

public class BalanceSheetPanel extends JPanel {

    private final JTextField dateField =
        new JTextField(LocalDate.now().toString(), 12);

    private final JLabel summaryLabel =
        new JLabel("Select a date and refresh.");

    private final DefaultTableModel model =
        new DefaultTableModel(
            new String[] {"Section", "Account", "Amount (PHP)"},
            0
        ) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }

            @Override
            public Class<?> getColumnClass(int column) {
                return column == 2 ? BigDecimal.class : String.class;
            }
        };

    public BalanceSheetPanel() {
        setLayout(new BorderLayout(0, 20));
        setBackground(Color.WHITE);
        setBorder(
            BorderFactory.createEmptyBorder(30, 30, 30, 30)
        );

        JLabel heading = new JLabel("Balance Sheet");
        heading.setFont(new Font("Arial", Font.BOLD, 24));
        heading.setForeground(new Color(15, 70, 125));

        JButton refreshButton = new JButton("Refresh");

        JPanel controls = new JPanel(new FlowLayout(FlowLayout.LEFT));
        controls.setOpaque(false);
        controls.add(new JLabel("As of (YYYY-MM-DD):"));
        controls.add(dateField);
        controls.add(refreshButton);

        JPanel top = new JPanel(new BorderLayout(0, 15));
        top.setOpaque(false);
        top.add(heading, BorderLayout.NORTH);
        top.add(controls, BorderLayout.CENTER);

        JTable table = new JTable(model);
        table.setRowHeight(28);
        table.setFillsViewportHeight(true);
        table.getTableHeader().setReorderingAllowed(false);

        table.getColumnModel().getColumn(0).setPreferredWidth(150);
        table.getColumnModel().getColumn(1).setPreferredWidth(400);
        table.getColumnModel().getColumn(2).setPreferredWidth(170);

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
            .getColumn(2)
            .setCellRenderer(moneyRenderer);

        add(top, BorderLayout.NORTH);
        add(new JScrollPane(table), BorderLayout.CENTER);
        add(summaryLabel, BorderLayout.SOUTH);

        refreshButton.addActionListener(event -> refreshBalanceSheet());
        dateField.addActionListener(event -> refreshBalanceSheet());
    }

    public void refreshBalanceSheet() {
        LocalDate asOf;

        try {
            asOf = LocalDate.parse(dateField.getText().trim());

        } catch (DateTimeParseException exception) {
            model.setRowCount(0);
            summaryLabel.setText("Enter a valid report date.");
            summaryLabel.setForeground(Color.RED);

            JOptionPane.showMessageDialog(
                this,
                "Enter a valid date using YYYY-MM-DD.",
                "Invalid Date",
                JOptionPane.WARNING_MESSAGE
            );
            return;
        }

        /*
         * Read individual lines and sum using BigDecimal in Java.
         * Earlier dates are included in the cumulative balances.
         */
        String sql = """
            SELECT
                a.id,
                a.code,
                a.name,
                a.type,
                j.debit_centavos,
                j.credit_centavos
            FROM accounts a
            LEFT JOIN (
                SELECT
                    jl.id,
                    jl.account_id,
                    jl.debit_centavos,
                    jl.credit_centavos,
                    t.transaction_date,
                    t.id AS transaction_id
                FROM journal_lines jl
                JOIN transactions t ON t.id = jl.transaction_id
                WHERE t.transaction_date <= ?
            ) j ON j.account_id = a.id
            ORDER BY
                a.code, a.id,
                j.transaction_date, j.transaction_id, j.id
            """;

        List<AccountBalance> accounts = new ArrayList<>();

        try (
            Connection connection = Database.connect();
            PreparedStatement statement =
                connection.prepareStatement(sql)
        ) {
            statement.setString(1, asOf.toString());

            try (ResultSet result = statement.executeQuery()) {
                AccountBalance current = null;

                while (result.next()) {
                    int accountId = result.getInt("id");

                    if (current == null || current.id != accountId) {
                        current = new AccountBalance(
                            accountId,
                            result.getString("code"),
                            result.getString("name"),
                            result.getString("type")
                        );

                        accounts.add(current);
                    }

                    BigDecimal debit = BigDecimal.valueOf(
                        result.getLong("debit_centavos"), 2
                    );

                    BigDecimal credit = BigDecimal.valueOf(
                        result.getLong("credit_centavos"), 2
                    );

                    // Debit-positive balance.
                    current.net = current.net.add(debit).subtract(credit);
                }
            }

        } catch (SQLException exception) {
            exception.printStackTrace();

            model.setRowCount(0);
            summaryLabel.setText("Balance Sheet unavailable. Please refresh.");
            summaryLabel.setForeground(Color.RED);

            JOptionPane.showMessageDialog(
                this,
                "Could not load the Balance Sheet:\n"
                    + exception.getMessage(),
                "Database Error",
                JOptionPane.ERROR_MESSAGE
            );
            return;
        }

        BigDecimal assets = zero();
        BigDecimal liabilities = zero();
        BigDecimal equity = zero();
        BigDecimal earnings = zero();

        for (AccountBalance account : accounts) {
            switch (account.type) {
                case "ASSET":
                    assets = assets.add(account.net);
                    break;

                case "LIABILITY":
                    liabilities = liabilities.subtract(account.net);
                    break;

                case "EQUITY":
                    equity = equity.subtract(account.net);
                    break;

                case "REVENUE":
                    earnings = earnings.subtract(account.net);
                    break;

                case "EXPENSE":
                    earnings = earnings.subtract(account.net);
                    break;

                default:
                    throw new IllegalStateException(
                        "Unsupported account type: " + account.type
                    );
            }
        }

        BigDecimal totalEquity = equity.add(earnings);
        BigDecimal liabilitiesAndEquity =
            liabilities.add(totalEquity);

        BigDecimal difference = assets.subtract(liabilitiesAndEquity);

        model.setRowCount(0);

        addAccountRows(accounts, "ASSET", "Assets");
        model.addRow(new Object[] {
            "Assets", "TOTAL ASSETS", assets
        });

        addAccountRows(accounts, "LIABILITY", "Liabilities");
        model.addRow(new Object[] {
            "Liabilities", "TOTAL LIABILITIES", liabilities
        });

        addAccountRows(accounts, "EQUITY", "Equity");

        model.addRow(new Object[] {
            "Equity",
            "Unclosed earnings / (loss)",
            earnings
        });

        model.addRow(new Object[] {
            "Equity", "TOTAL EQUITY", totalEquity
        });

        model.addRow(new Object[] {
            "Total",
            "TOTAL LIABILITIES AND EQUITY",
            liabilitiesAndEquity
        });

        boolean balanced = difference.signum() == 0;

        summaryLabel.setForeground(
            balanced ? Color.DARK_GRAY : Color.RED
        );

        summaryLabel.setText(
            "<html>As of: " + asOf
                + "<br>Assets: PHP " + assets.toPlainString()
                + "<br>Liabilities + Equity: PHP "
                + liabilitiesAndEquity.toPlainString()
                + "<br>"
                + (balanced
                    ? "Assets equal Liabilities plus Equity."
                    : "Difference: PHP " + difference.toPlainString())
                + "</html>"
        );
    }

    private void addAccountRows(
        List<AccountBalance> accounts,
        String type,
        String section
    ) {
        for (AccountBalance account : accounts) {
            if (!account.type.equals(type)) {
                continue;
            }

            BigDecimal amount = type.equals("ASSET")
                ? account.net
                : account.net.negate();

            model.addRow(new Object[] {
                section,
                account.code + " - " + account.name,
                amount
            });
        }
    }

    private static BigDecimal zero() {
        return new BigDecimal("0.00");
    }

    private static class AccountBalance {
        private final int id;
        private final String code;
        private final String name;
        private final String type;
        private BigDecimal net = zero();

        AccountBalance(
            int id,
            String code,
            String name,
            String type
        ) {
            this.id = id;
            this.code = code;
            this.name = name;
            this.type = type;
        }
    }
}