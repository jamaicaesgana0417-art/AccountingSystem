import java.awt.*;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.*;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import javax.swing.*;
import javax.swing.table.*;

/** Two-column balance sheet populated from posted journal entries. */
public class BalanceSheetPanel extends JPanel {
    private static final Color PAGE_BG = new Color(244, 246, 255);
    private static final Color BORDER = new Color(221, 228, 245);
    private static final Color TEXT = new Color(31, 50, 91);
    private static final Color MUTED = new Color(104, 120, 153);
    private static final Color BLUE = new Color(55, 91, 255);

    private final DefaultTableModel assets = reportModel("Assets");
    private final DefaultTableModel other = reportModel("Liabilities & Equity");
    private final JTable left = reportTable(assets);
    private final JTable right = reportTable(other);
    private final JLabel status = new JLabel("Open the report to load balances.");
    private final JButton options = new JButton("Options");
    private LocalDate reportDate = LocalDate.now();
    private LocalDate loadedDate;

    public BalanceSheetPanel() {
        setLayout(new BorderLayout(0, 18));
        setBackground(PAGE_BG);
        setBorder(BorderFactory.createEmptyBorder(30, 30, 24, 30));

        JLabel heading = new JLabel("BALANCE SHEET");
        heading.setFont(new Font("Inter", Font.BOLD, 24));
        heading.setForeground(TEXT);

        JLabel subtitle = new JLabel(
            "Review assets, liabilities, and equity as of a selected date."
        );
        subtitle.setFont(new Font("Inter", Font.PLAIN, 11));
        subtitle.setForeground(MUTED);

        JPanel titleBlock = new JPanel();
        titleBlock.setOpaque(false);
        titleBlock.setLayout(new BoxLayout(titleBlock, BoxLayout.Y_AXIS));
        titleBlock.add(heading);
        titleBlock.add(Box.createVerticalStrut(7));
        titleBlock.add(subtitle);

        options.setFocusPainted(false);
        options.setFont(new Font("Inter", Font.PLAIN, 11));
        options.setBackground(BLUE);
        options.setForeground(Color.WHITE);
        options.setBorder(BorderFactory.createEmptyBorder(8, 14, 8, 14));
        options.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        options.addActionListener(event -> showOptions());

        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);
        header.add(titleBlock, BorderLayout.WEST);
        header.add(options, BorderLayout.EAST);

        JPanel columns = new JPanel(new GridLayout(1, 2, 14, 0));
        columns.setOpaque(false);
        columns.add(section("Assets", left));
        columns.add(section("Liabilities & Equity", right));

        status.setFont(new Font("Inter", Font.PLAIN, 10));
        status.setForeground(MUTED);
        add(header, BorderLayout.NORTH);
        add(columns, BorderLayout.CENTER);
        add(status, BorderLayout.SOUTH);
    }

    private static JPanel section(String title, JTable table) {
        JPanel panel = new JPanel(new BorderLayout(0, 8));
        panel.setBackground(Color.WHITE);
        panel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(BORDER, 1, true),
            BorderFactory.createEmptyBorder(14, 14, 14, 14)
        ));

        JLabel label = new JLabel(title);
        label.setFont(new Font("Inter", Font.BOLD, 12));
        label.setForeground(TEXT);
        panel.add(label, BorderLayout.NORTH);

        JScrollPane scroll = new JScrollPane(table);
        scroll.setBorder(BorderFactory.createLineBorder(BORDER, 1));
        scroll.getViewport().setBackground(Color.WHITE);
        scroll.getViewport().setOpaque(true);
        table.getTableHeader().setBorder(
            BorderFactory.createMatteBorder(0, 0, 1, 0, BORDER)
        );
        panel.add(scroll, BorderLayout.CENTER);
        return panel;
    }

    private static DefaultTableModel reportModel(String name) {
        return new DefaultTableModel(
            new String[]{name, "Debit (₱)", "Credit (₱)"},
            0
        ) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }

            @Override
            public Class<?> getColumnClass(int column) {
                return column == 1 || column == 2
                    ? BigDecimal.class : String.class;
            }
        };
    }

    private static JTable reportTable(DefaultTableModel model) {
        JTable table = new JTable(model);
        table.setFont(new Font("Inter", Font.PLAIN, 10));
        table.setForeground(TEXT);
        table.setBackground(Color.WHITE);
        table.setRowHeight(32);
        table.setFillsViewportHeight(true);
        table.setShowHorizontalLines(true);
        table.setShowVerticalLines(true);
        table.setGridColor(BORDER);
        table.setIntercellSpacing(new Dimension(1, 1));
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setSelectionBackground(new Color(232, 238, 255));
        table.setSelectionForeground(TEXT);
        table.getTableHeader().setReorderingAllowed(false);
        table.getTableHeader().setFont(new Font("Inter", Font.BOLD, 9));
        table.getTableHeader().setBackground(new Color(232, 238, 255));
        table.getTableHeader().setForeground(TEXT);
        table.getTableHeader().setPreferredSize(new Dimension(0, 36));
        table.getTableHeader().setBorder(BorderFactory.createEmptyBorder());
        table.getColumnModel().getColumn(0).setPreferredWidth(230);
        table.getColumnModel().getColumn(1).setPreferredWidth(110);
        table.getColumnModel().getColumn(2).setPreferredWidth(110);

        DefaultTableCellRenderer renderer = new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(
                    JTable source, Object value, boolean selected,
                    boolean focused, int row, int column) {
                super.getTableCellRendererComponent(
                    source, value, selected, focused, row, column);
                String label = source.getValueAt(row, 0) == null
                    ? "" : String.valueOf(source.getValueAt(row, 0));
                boolean total = label.startsWith("Total ");
                setFont(source.getFont().deriveFont(total ? Font.BOLD : Font.PLAIN));
                setHorizontalAlignment(
                    column == 0 ? SwingConstants.LEFT : SwingConstants.RIGHT
                );
                if (column > 0 && value instanceof BigDecimal) {
                    setText("₱ " + money((BigDecimal) value));
                }
                if (selected) {
                    setForeground(source.getSelectionForeground());
                } else {
                    setForeground(TEXT);
                }
                return this;
            }
        };

        for (int column = 0; column < 3; column++) {
            table.getColumnModel().getColumn(column).setCellRenderer(renderer);
        }
        return table;
    }

    private void showOptions() {
        JPopupMenu menu = new JPopupMenu();
        JMenuItem refresh = new JMenuItem("Refresh");
        refresh.addActionListener(event -> refreshBalanceSheet());

        JMenuItem date = new JMenuItem("Change report date...");
        date.addActionListener(event -> changeDate());

        JMenuItem export = new JMenuItem("Export CSV...");
        export.addActionListener(event -> exportCsv());

        menu.add(refresh);
        menu.add(date);
        menu.addSeparator();
        menu.add(export);
        menu.show(options, 0, options.getHeight());
    }

    private void changeDate() {
        String input = JOptionPane.showInputDialog(
            this, "As-of date (YYYY-MM-DD):", reportDate.toString());
        if (input == null) {
            return;
        }

        try {
            reportDate = LocalDate.parse(input.trim());
            refreshBalanceSheet();
        } catch (DateTimeParseException exception) {
            JOptionPane.showMessageDialog(
                this,
                "Enter a valid date using YYYY-MM-DD.",
                "Invalid Date",
                JOptionPane.WARNING_MESSAGE
            );
        }
    }

    // App.java calls this when the Balance Sheet screen is opened.
    public void refreshBalanceSheet() {
        loadedDate = null;
        assets.setRowCount(0);
        other.setRowCount(0);

        try {
            List<AccountSummary> accounts = loadAccountSummaries(reportDate);
            BigDecimal totalAssets = BigDecimal.ZERO;
            BigDecimal liabilities = BigDecimal.ZERO;
            BigDecimal equity = BigDecimal.ZERO;
            BigDecimal earnings = BigDecimal.ZERO;

            for (AccountSummary account : accounts) {
                BigDecimal net = account.net();
                switch (account.type) {
                    case "ASSET":
                    case "CONTRA-ASSET":
                        if (net.signum() != 0) {
                            addAmountRow(assets, account.name, net, true);
                        }
                        totalAssets = totalAssets.add(net);
                        break;

                    case "LIABILITY":
                        if (net.signum() != 0) {
                            addAmountRow(
                                other,
                                account.name,
                                net.negate(),
                                false
                            );
                        }
                        liabilities = liabilities.add(net.negate());
                        break;

                    case "EQUITY":
                    case "CONTRA-EQUITY":
                        if (net.signum() != 0) {
                            addAmountRow(
                                other,
                                account.name,
                                net.negate(),
                                false
                            );
                        }
                        equity = equity.add(net.negate());
                        break;

                    case "REVENUE":
                        if (net.signum() != 0) {
                            addAmountRow(
                                other,
                                account.name,
                                net.negate(),
                                false
                            );
                        }
                        earnings = earnings.add(net.negate());
                        break;

                    case "EXPENSE":
                        if (net.signum() != 0) {
                            addAmountRow(assets, account.name, net, true);
                        }
                        earnings = earnings.add(net);
                        break;

                    default:
                        throw new IllegalArgumentException(
                            "Unknown account type: " + account.type
                        );
                }
            }

            BigDecimal totalOther = liabilities.add(equity).add(earnings);
            assets.addRow(new Object[]{"", null, null});
            addAmountRow(assets, "Total Assets", totalAssets, true);
            other.addRow(new Object[]{"", null, null});
            addAmountRow(
                other,
                "Total Liabilities & Equity",
                totalOther,
                false
            );

            loadedDate = reportDate;
            BigDecimal difference = totalAssets.subtract(totalOther);
            status.setForeground(difference.signum() == 0
                ? new Color(40, 80, 55)
                : new Color(170, 30, 30));
            status.setText("As of " + reportDate + "  |  "
                + (difference.signum() == 0
                    ? "Balanced"
                    : "Difference: ₱ " + money(difference)));

        } catch (Exception exception) {
            assets.setRowCount(0);
            other.setRowCount(0);
            status.setForeground(Color.RED);
            status.setText("Report unavailable. Use Options > Refresh to retry.");
            exception.printStackTrace();
            JOptionPane.showMessageDialog(
                this,
                "Could not load the balance sheet.\n" + exception.getMessage(),
                "Balance Sheet Error",
                JOptionPane.ERROR_MESSAGE
            );
        }
    }

    private void exportCsv() {
        refreshBalanceSheet();
        if (loadedDate == null) {
            return;
        }

        DefaultTableModel output = new DefaultTableModel(
            new String[]{
                "As of", "Section", "Account", "Debit (₱)", "Credit (₱)"
            },
            0
        );
        copyRows(output, assets, "Assets");
        copyRows(output, other, "Liabilities & Equity");
        CsvExporter.export(this, new JTable(output),
            "balance_sheet_" + loadedDate + ".csv");
    }

    private void copyRows(DefaultTableModel output,
                          DefaultTableModel source, String section) {
        for (int row = 0; row < source.getRowCount(); row++) {
            Object labelValue = source.getValueAt(row, 0);
            if (labelValue == null) {
                continue;
            }

            String label = labelValue.toString();
            if (label.isEmpty()) {
                continue;
            }

            output.addRow(new Object[]{
                loadedDate.toString(),
                section,
                label,
                source.getValueAt(row, 1),
                source.getValueAt(row, 2)
            });
        }
    }

    private void addAmountRow(
        DefaultTableModel model,
        String label,
        BigDecimal amount,
        boolean positiveIsDebit
    ) {
        boolean debitSide = (amount.signum() > 0) == positiveIsDebit;
        BigDecimal debit = amount.signum() == 0 || !debitSide
            ? null : amount.abs();
        BigDecimal credit = amount.signum() == 0 || debitSide
            ? null : amount.abs();
        model.addRow(new Object[]{label, debit, credit});
    }

    private List<AccountSummary> loadAccountSummaries(LocalDate asOf) throws SQLException {
        String sql = """
            SELECT a.id,
                   a.name,
                   a.type,
                   COALESCE(SUM(CASE WHEN jl.debit_centavos > 0 THEN jl.debit_centavos ELSE 0 END), 0) AS debit_total,
                   COALESCE(SUM(CASE WHEN jl.credit_centavos > 0 THEN jl.credit_centavos ELSE 0 END), 0) AS credit_total
            FROM accounts a
            LEFT JOIN journal_lines jl
              ON jl.account_id = a.id
             AND jl.transaction_id IN (
                   SELECT id
                   FROM transactions
                   WHERE transaction_date <= ?
                        AND owner_username = ?
             )
            WHERE a.active = 1
                AND a.owner_username = ?
              AND a.type IN ('ASSET', 'CONTRA-ASSET', 'LIABILITY', 'EQUITY', 'CONTRA-EQUITY', 'REVENUE', 'EXPENSE')
            GROUP BY a.id, a.name, a.type
            ORDER BY a.type, a.name
            """;

        List<AccountSummary> rows = new ArrayList<>();

        try (
            Connection connection = Database.connect();
            PreparedStatement statement = connection.prepareStatement(sql)
        ) {
            statement.setString(1, asOf.toString());
            statement.setString(2, Session.getCurrentUser());
            statement.setString(3, Session.getCurrentUser());

            try (ResultSet result = statement.executeQuery()) {
                while (result.next()) {
                    rows.add(new AccountSummary(
                        result.getString("name"),
                        result.getString("type"),
                        result.getLong("debit_total"),
                        result.getLong("credit_total")
                    ));
                }
            }
        }

        return rows;
    }

    private static String money(BigDecimal value) {
        return value.setScale(2, RoundingMode.HALF_UP).toPlainString();
    }

    private static final class AccountSummary {
        private final String name;
        private final String type;
        private final long debitCentavos;
        private final long creditCentavos;

        private AccountSummary(String name, String type,
                               long debitCentavos, long creditCentavos) {
            this.name = name;
            this.type = type;
            this.debitCentavos = debitCentavos;
            this.creditCentavos = creditCentavos;
        }

        private BigDecimal net() {
            long difference = debitCentavos - creditCentavos;
            return BigDecimal.valueOf(difference, 2);
        }
    }
}
