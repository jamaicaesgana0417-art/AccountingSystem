import java.awt.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import javax.swing.*;
import javax.swing.table.*;

/** Two-column balance sheet populated from posted journal entries. */
public class BalanceSheetPanel extends JPanel {
    private static final Color LINE = new Color(190, 194, 199);
    private static final Color STRIPE = new Color(246, 248, 250);

    private final DefaultTableModel assets = reportModel("Asset");
    private final DefaultTableModel other = reportModel("Liabilities & Equity");
    private final JTable left = reportTable(assets);
    private final JTable right = reportTable(other);
    private final JLabel status = new JLabel("Open the report to load balances.");
    private final JButton options = new JButton("Options \u25be");
    private LocalDate reportDate = LocalDate.now();
    private LocalDate loadedDate;

    public BalanceSheetPanel() {
        setLayout(new BorderLayout(0, 12));
        setBackground(Color.WHITE);
        setBorder(BorderFactory.createEmptyBorder(16, 16, 12, 16));

        JLabel heading = new JLabel("Balance Sheet");
        heading.setFont(new Font("Arial", Font.BOLD, 18));
        heading.setForeground(new Color(35, 38, 43));
        options.setFocusPainted(false);
        options.setFont(new Font("Arial", Font.PLAIN, 12));
        options.setBackground(new Color(37, 91, 184));
        options.setForeground(Color.WHITE);
        options.addActionListener(event -> showOptions());

        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);
        header.add(heading, BorderLayout.WEST);
        header.add(options, BorderLayout.EAST);

        JPanel columns = new JPanel(new GridLayout(1, 2, 14, 0));
        columns.setOpaque(false);
        columns.add(section("Assets", left));
        columns.add(section("Liabilities & Equity", right));
        status.setFont(new Font("Arial", Font.PLAIN, 12));
        add(header, BorderLayout.NORTH);
        add(columns, BorderLayout.CENTER);
        add(status, BorderLayout.SOUTH);
    }

    private static DefaultTableModel reportModel(String name) {
        return new DefaultTableModel(new String[]{name, "Amount"}, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }

            @Override
            public Class<?> getColumnClass(int column) {
                return column == 1 ? BigDecimal.class : String.class;
            }
        };
    }

    private static JTable reportTable(DefaultTableModel model) {
        JTable table = new JTable(model);
        table.setFont(new Font("Arial", Font.PLAIN, 13));
        table.setRowHeight(28);
        table.setFillsViewportHeight(true);
        table.setShowVerticalLines(true);
        table.setShowHorizontalLines(true);
        table.setGridColor(new Color(232, 235, 238));
        table.setIntercellSpacing(new Dimension(1, 1));
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setSelectionBackground(new Color(220, 232, 247));
        table.setSelectionForeground(Color.BLACK);
        table.getTableHeader().setReorderingAllowed(false);
        table.getTableHeader().setFont(new Font("Arial", Font.BOLD, 13));
        table.getTableHeader().setBackground(new Color(222, 225, 230));
        table.getTableHeader().setForeground(Color.DARK_GRAY);
        table.getTableHeader().setPreferredSize(new Dimension(0, 29));
        table.getColumnModel().getColumn(0).setPreferredWidth(270);
        table.getColumnModel().getColumn(1).setPreferredWidth(160);

        DefaultTableCellRenderer renderer = new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(
                    JTable source, Object value, boolean selected,
                    boolean focused, int row, int column) {
                super.getTableCellRendererComponent(
                    source, value, selected, focused, row, column);
                String label = String.valueOf(source.getValueAt(row, 0));
                boolean total = label.startsWith("Total ");
                setFont(source.getFont().deriveFont(
                    total ? Font.BOLD : Font.PLAIN));
                setHorizontalAlignment(column == 1
                            panel.add(scroll, BorderLayout.CENTER);
        return panel;
    }

    private void showOptions() {
        JPopupMenu menu = new JPopupMenu();
        JMenuItem refresh = new JMenuItem("Refresh");
        refresh.addActionListener(event -> refreshBalanceSheet());
        JMenuItem date = new JMenuItem("Change report date...");
        date.addActionListener(event -> changeDate());
        JMenuItem export = new JMenuItem("Export CSV...");
        export.addActionListener(event -> export());
        menu.add(refresh);
        menu.add(date);
        menu.addSeparator();
        menu.add(export);
        menu.show(options, 0, options.getHeight());
    }

    private void changeDate() {
        String input = JOptionPane.showInputDialog(
            this, "As-of date (YYYY-MM-DD):", reportDate.toString());
        if (input == null) return;
        try {
            reportDate = AccountingService.date(input);
            refreshBalanceSheet();
        } catch (Exception exception) {
            JOptionPane.showMessageDialog(this,
                "Enter a valid date using YYYY-MM-DD.",
                "Invalid Date", JOptionPane.WARNING_MESSAGE);
        }
    }

    // App.java calls this when the Balance Sheet screen is opened.
    public void refreshBalanceSheet() {
        loadedDate = null;
        assets.setRowCount(0);
        other.setRowCount(0);
        try {
            List<AccountingService.Account> accounts =
                AccountingService.accounts(reportDate);
            BigDecimal totalAssets = AccountingService.ZERO;
            BigDecimal liabilities = AccountingService.ZERO;
            BigDecimal equity = AccountingService.ZERO;
            BigDecimal earnings = AccountingService.ZERO;

            for (AccountingService.Account account : accounts) {
                BigDecimal net = account.net();
                switch (account.type) {
                    case "ASSET":
                        if (net.signum() != 0)
                            assets.addRow(new Object[]{account.name, net});
                        totalAssets = totalAssets.add(net);
                        break;
                    case "LIABILITY":
                        if (net.signum() != 0)
                            other.addRow(new Object[]{account.name, net.negate()});
                        liabilities = liabilities.subtract(net);
                        break;
                    case "EQUITY":
                        break;
                    case "REVENUE":
                    case "EXPENSE":
                        earnings = earnings.subtract(net);
                        break;
                    default:
                        throw new IllegalArgumentException(
                            "Unknown account type: " + account.type);
                }
            }

            for (AccountingService.Account account : accounts) {
                if (account.type.equals("EQUITY")) {
                    BigDecimal amount = account.net().negate();
                    if (amount.signum() != 0)
                        other.addRow(new Object[]{account.name, amount});
                    equity = equity.add(amount);
                }
            }
            if (earnings.signum() != 0) {
                other.addRow(new Object[]{
                    earnings.signum() < 0 ? "Net Loss" : "Net Income",
                    earnings
                });
            }

            BigDecimal totalOther = liabilities.add(equity).add(earnings);
            assets.addRow(new Object[]{"", null});
            assets.addRow(new Object[]{"Total Assets", totalAssets});
            other.addRow(new Object[]{"", null});
            other.addRow(new Object[]{"Total Liabilities & Equity", totalOther});
            loadedDate = reportDate;
            BigDecimal difference = totalAssets.subtract(totalOther);
            status.setForeground(difference.signum() == 0
                ? new Color(40, 80, 55) : new Color(170, 30, 30));
            status.setText("As of " + reportDate + "  |  "
                + (difference.signum() == 0 ? "Balanced"
                    : "Difference: PHP " + AccountingService.money(difference)));

        } catch (Exception exception) {
            assets.setRowCount(0);
            other.setRowCount(0);
            status.setForeground(Color.RED);
            status.setText("Report unavailable. Use Options > Refresh to retry.");
            UITheme.error(this, exception);
        }
    }

    private void export() {
        refreshBalanceSheet();
        if (loadedDate == null) return;
        DefaultTableModel output = UITheme.model(
            "As of", "Section", "Account", "Amount (PHP)");
        copyRows(output, assets, "Assets");
        copyRows(output, other, "Liabilities & Equity");
        CsvExporter.export(this, new JTable(output),
            "balance_sheet_" + loadedDate + ".csv");
    }

    private void copyRows(DefaultTableModel output,
                          DefaultTableModel source, String section) {
        for (int row = 0; row < source.getRowCount(); row++) {
            String label = source.getValueAt(row, 0).toString();
            if (label.isEmpty()) continue;
            output.addRow(new Object[]{loadedDate.toString(), section,
                label, source.getValueAt(row, 1)});
        }
    }
