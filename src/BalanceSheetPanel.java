import java.awt.*;
import java.awt.geom.RoundRectangle2D;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import javax.swing.*;
import javax.swing.table.*;

/** Two-column balance sheet populated from posted journal entries. */
public class BalanceSheetPanel extends JPanel {
    private static final Color PAGE_BG = new Color(240, 245, 252);
    private static final Color NAVY = new Color(22, 56, 102);
    private static final Color CARD_BORDER = new Color(220, 228, 240);
    private static final Color HEADER_BG = new Color(226, 234, 250);
    private static final Color ROW_LINE = new Color(235, 240, 248);
    private static final Color MUTED = new Color(110, 125, 145);
    private static final Color PLACEHOLDER = new Color(140, 152, 170);
    private static final Color TEXT = new Color(45, 60, 85);

    private static final String ZERO_TEXT = "\u20B1 0.00";

    private final DefaultTableModel assets = reportModel();
    private final DefaultTableModel other = reportModel();
    private final JTable left = reportTable(assets);
    private final JTable right = reportTable(other);
    private final JLabel status = new JLabel("Open the report to load balances.");
    private final JButton options = new OptionsButton("Options ▾");
    private final JLabel asOfLabel = new JLabel();

    // Summary cards at the top.
    private final JLabel totalAssetsCard = new JLabel(ZERO_TEXT);
    private final JLabel totalLiabilitiesCard = new JLabel(ZERO_TEXT);
    private final JLabel totalEquityCard = new JLabel(ZERO_TEXT);

    // Total bands at the bottom of each table card.
    private final JLabel assetsTotalValue = new JLabel(ZERO_TEXT);
    private final JLabel otherTotalValue = new JLabel(ZERO_TEXT);

    private LocalDate reportDate = LocalDate.now();
    private LocalDate loadedDate;
    private BigDecimal loadedTotalAssets = BigDecimal.ZERO;
    private BigDecimal loadedTotalOther = BigDecimal.ZERO;

    public BalanceSheetPanel() {
        setLayout(new BorderLayout(0, 16));
        setBackground(PAGE_BG);
        setBorder(BorderFactory.createEmptyBorder(28, 30, 20, 30));

        JLabel heading = new JLabel("BALANCE SHEET");
        heading.setFont(new Font("Inter", Font.BOLD, 22));
        heading.setForeground(NAVY);

        JLabel subtitle = new JLabel(
            "A clear view of your assets, liabilities, and equity.");
        subtitle.setFont(new Font("Inter", Font.PLAIN, 12));
        subtitle.setForeground(MUTED);

        asOfLabel.setFont(new Font("Inter", Font.PLAIN, 11));
        asOfLabel.setForeground(MUTED);
        updateAsOfLabel();

        options.setFocusPainted(false);
        options.setContentAreaFilled(false);
        options.setBorderPainted(false);
        options.setFont(new Font("Inter", Font.PLAIN, 12));
        options.setForeground(TEXT);
        options.setBorder(BorderFactory.createEmptyBorder(8, 16, 8, 16));
        options.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        options.addActionListener(event -> showOptions());

        JPanel infoRow = new JPanel(new BorderLayout());
        infoRow.setOpaque(false);
        infoRow.add(asOfLabel, BorderLayout.WEST);
        infoRow.add(options, BorderLayout.EAST);

        // ----- Summary cards -----
        JPanel summary = new JPanel(new GridLayout(1, 3, 16, 0));
        summary.setOpaque(false);
        summary.add(summaryCard("Total assets", totalAssetsCard));
        summary.add(summaryCard("Total liabilities", totalLiabilitiesCard));
        summary.add(summaryCard("Total equity", totalEquityCard));

        // ----- Top: heading, subtitle, as-of row, summary -----
        JPanel top = new JPanel(new GridBagLayout());
        top.setOpaque(false);
        place(top, heading, 0, new Insets(0, 0, 0, 0));
        place(top, subtitle, 1, new Insets(4, 0, 0, 0));
        place(top, infoRow, 2, new Insets(10, 0, 0, 0));
        place(top, summary, 3, new Insets(8, 0, 0, 0));

        // ----- The two statement cards -----
        JPanel columns = new JPanel(new GridLayout(1, 2, 16, 0));
        columns.setOpaque(false);
        columns.add(section("Assets", left, "Total Assets", assetsTotalValue));
        columns.add(section("Liabilities & Equity", right,
            "Total Liabilities & Equity", otherTotalValue));

        // ----- Status band -----
        status.setFont(new Font("Inter", Font.PLAIN, 11));
        status.setForeground(MUTED);

        RoundedPanel statusBand = new RoundedPanel(
            new BorderLayout(), HEADER_BG, null, 10);
        statusBand.setBorder(BorderFactory.createEmptyBorder(10, 16, 10, 16));
        statusBand.add(status, BorderLayout.CENTER);

        add(top, BorderLayout.NORTH);
        add(columns, BorderLayout.CENTER);
        add(statusBand, BorderLayout.SOUTH);
    }

    // ---------------------------------------------------------------
    // Design helpers
    // ---------------------------------------------------------------

    private static void place(JPanel panel, JComponent component, int row,
                              Insets insets) {
        GridBagConstraints c = new GridBagConstraints();
        c.gridx = 0;
        c.gridy = row;
        c.weightx = 1;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.anchor = GridBagConstraints.NORTHWEST;
        c.insets = insets;
        panel.add(component, c);
    }

    private static JPanel summaryCard(String title, JLabel valueLabel) {
        RoundedPanel card = new RoundedPanel(
            new BorderLayout(0, 6), Color.WHITE, CARD_BORDER, 16);
        card.setBorder(BorderFactory.createEmptyBorder(14, 18, 14, 18));

        JLabel label = new JLabel(title);
        label.setFont(new Font("Inter", Font.PLAIN, 10));
        label.setForeground(MUTED);

        valueLabel.setFont(new Font("Inter", Font.BOLD, 20));
        valueLabel.setForeground(NAVY);

        card.add(label, BorderLayout.NORTH);
        card.add(valueLabel, BorderLayout.CENTER);
        return card;
    }

    private static JPanel section(String title, JTable table,
                                  String totalName, JLabel totalValue) {
        RoundedPanel card = new RoundedPanel(
            new BorderLayout(0, 12), Color.WHITE, CARD_BORDER, 18);
        card.setBorder(BorderFactory.createEmptyBorder(16, 18, 16, 18));

        JLabel label = new JLabel(title);
        label.setFont(new Font("Inter", Font.BOLD, 15));
        label.setForeground(NAVY);

        JScrollPane scroll = new JScrollPane(table);
        scroll.setBorder(null);
        scroll.getViewport().setBackground(Color.WHITE);

        // Rounds the corners of the header band.
        RoundedPanel tableWrap = new RoundedPanel(
            new BorderLayout(), Color.WHITE, null, 8);
        tableWrap.add(scroll, BorderLayout.CENTER);

        JLabel totalLabel = new JLabel(totalName);
        totalLabel.setFont(new Font("Inter", Font.BOLD, 11));
        totalLabel.setForeground(NAVY);

        totalValue.setFont(new Font("Inter", Font.BOLD, 12));
        totalValue.setForeground(NAVY);

        RoundedPanel band = new RoundedPanel(
            new BorderLayout(), HEADER_BG, null, 8);
        band.setBorder(BorderFactory.createEmptyBorder(10, 12, 10, 12));
        band.add(totalLabel, BorderLayout.WEST);
        band.add(totalValue, BorderLayout.EAST);

        card.add(label, BorderLayout.NORTH);
        card.add(tableWrap, BorderLayout.CENTER);
        card.add(band, BorderLayout.SOUTH);
        return card;
    }

    private static DefaultTableModel reportModel() {
        return new DefaultTableModel(new String[]{"Account", "Amount (PHP)"}, 0) {
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
        // Also draws "No balances to display" when there are no rows.
        JTable table = new JTable(model) {
            @Override
            protected void paintComponent(Graphics graphics) {
                super.paintComponent(graphics);

                if (getRowCount() == 0) {
                    paintEmptyState(graphics, getVisibleRect());
                }
            }
        };

        table.setFont(new Font("Inter", Font.PLAIN, 12)); //I change the font
        table.setRowHeight(32);
        table.setFillsViewportHeight(true);
        table.setShowVerticalLines(false);
        table.setShowHorizontalLines(true);
        table.setGridColor(ROW_LINE);
        table.setIntercellSpacing(new Dimension(0, 1));
        table.setBackground(Color.WHITE);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setSelectionBackground(new Color(232, 240, 252));
        table.setSelectionForeground(TEXT);
        table.getTableHeader().setReorderingAllowed(false);
        table.getColumnModel().getColumn(0).setPreferredWidth(270);
        table.getColumnModel().getColumn(1).setPreferredWidth(160);

        DefaultTableCellRenderer renderer = new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(
                    JTable source, Object value, boolean selected,
                    boolean focused, int row, int column) {
                super.getTableCellRendererComponent(
                    source, value, selected, false, row, column);
                setHorizontalAlignment(column == 1 ? SwingConstants.RIGHT : SwingConstants.LEFT);
                setBorder(BorderFactory.createEmptyBorder(0, 12, 0, 12));
                setForeground(TEXT);
                return this;
            }
        };

        table.getColumnModel().getColumn(0).setCellRenderer(renderer);
        table.getColumnModel().getColumn(1).setCellRenderer(renderer);

        // Light blue header band, like the mockup.
        DefaultTableCellRenderer headerRenderer = new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(
                    JTable source, Object value, boolean selected,
                    boolean focused, int row, int column) {
                super.getTableCellRendererComponent(
                    source, value, false, false, row, column);
                setBackground(HEADER_BG);
                setForeground(NAVY);
                setFont(new Font("Inter", Font.BOLD, 10));
                setHorizontalAlignment(column == 1 ? SwingConstants.RIGHT : SwingConstants.LEFT);
                setBorder(BorderFactory.createEmptyBorder(0, 12, 0, 12));
                return this;
            }
        };

        JTableHeader header = table.getTableHeader();
        header.setDefaultRenderer(headerRenderer);
        header.setBackground(HEADER_BG);
        header.setOpaque(true);
        header.setPreferredSize(new Dimension(0, 32));
        return table;
    }

    private static void paintEmptyState(Graphics graphics, Rectangle area) {
        Graphics2D g = (Graphics2D) graphics.create();
        g.setRenderingHint(
            RenderingHints.KEY_TEXT_ANTIALIASING,
            RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        int cx = area.x + area.width / 2;
        int cy = area.y + area.height / 2;

        g.setFont(new Font("Inter", Font.PLAIN, 12));
        g.setColor(MUTED);
        String title = "No balances to display";
        g.drawString(title,
            cx - g.getFontMetrics().stringWidth(title) / 2, cy - 2);

        g.setFont(new Font("Inter", Font.PLAIN, 10));
        g.setColor(PLACEHOLDER);
        String text = "Balances appear after transactions are posted.";
        g.drawString(text,
            cx - g.getFontMetrics().stringWidth(text) / 2, cy + 16);

        g.dispose();
    }

    private void updateAsOfLabel() {
        asOfLabel.setText("As of " + reportDate.format(
            DateTimeFormatter.ofPattern("MMMM d, yyyy", Locale.ENGLISH)));
    }

    private void resetTotals() {
        loadedTotalAssets = BigDecimal.ZERO;
        loadedTotalOther = BigDecimal.ZERO;
        totalAssetsCard.setText(ZERO_TEXT);
        totalLiabilitiesCard.setText(ZERO_TEXT);
        totalEquityCard.setText(ZERO_TEXT);
        assetsTotalValue.setText(ZERO_TEXT);
        otherTotalValue.setText(ZERO_TEXT);
    }

    // ---------------------------------------------------------------
    // Logic (unchanged, except where the totals are displayed)
    // ---------------------------------------------------------------

    private void showOptions() {
        JPopupMenu menu = new JPopupMenu();
        menu.setBackground(Color.WHITE);
        menu.setBorder(
            BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(CARD_BORDER),
                BorderFactory.createEmptyBorder(6, 6, 6, 6)
            )
        );

        // Same actions as before, now as icon rows like the side menu.
        menu.add(new OptionRow(OPTION_REFRESH, "Refresh", () -> {
            menu.setVisible(false);
            refreshBalanceSheet();
        }));

        menu.add(new OptionRow(OPTION_DATE, "Change report date...", () -> {
            menu.setVisible(false);
            changeDate();
        }));

        JPanel line = new JPanel();
        line.setBackground(CARD_BORDER);
        line.setPreferredSize(new Dimension(10, 1));

        JPanel separator = new JPanel(new BorderLayout());
        separator.setOpaque(false);
        separator.setBorder(BorderFactory.createEmptyBorder(4, 6, 4, 6));
        separator.add(line, BorderLayout.CENTER);
        menu.add(separator);

        menu.add(new OptionRow(OPTION_EXPORT, "Export CSV...", () -> {
            menu.setVisible(false);
            exportCsv();
        }));

        // Open under the button, lined up with its right edge.
        menu.show(
            options,
            options.getWidth() - menu.getPreferredSize().width,
            options.getHeight() + 4
        );
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
        resetTotals();
        updateAsOfLabel();

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
                        if (net.signum() != 0) {
                            assets.addRow(new Object[]{account.name, net});
                        }
                        totalAssets = totalAssets.add(net);
                        break;

                    case "LIABILITY":
                        if (net.signum() != 0) {
                            other.addRow(new Object[]{account.name, net.negate()});
                        }
                        liabilities = liabilities.add(net.negate());
                        break;

                    case "EQUITY":
                        if (net.signum() != 0) {
                            other.addRow(new Object[]{account.name, net.negate()});
                        }
                        equity = equity.add(net.negate());
                        break;

                    case "REVENUE":
                        if (net.signum() != 0) {
                            other.addRow(new Object[]{account.name, net.negate()});
                        }
                        earnings = earnings.add(net.negate());
                        break;

                    case "EXPENSE":
                        if (net.signum() != 0) {
                            assets.addRow(new Object[]{account.name, net});
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

            // The totals now live in the cards and bands, not in the tables.
            loadedTotalAssets = totalAssets;
            loadedTotalOther = totalOther;
            totalAssetsCard.setText("\u20B1 " + money(totalAssets));
            totalLiabilitiesCard.setText("\u20B1 " + money(liabilities));
            totalEquityCard.setText("\u20B1 " + money(equity.add(earnings)));
            assetsTotalValue.setText("\u20B1 " + money(totalAssets));
            otherTotalValue.setText("\u20B1 " + money(totalOther));

            loadedDate = reportDate;
            BigDecimal difference = totalAssets.subtract(totalOther);
            status.setForeground(MUTED);
            status.setText(difference.signum() == 0
                ? "<html><b><font color='#2E7D4F'>\u2713 Balanced</font></b>"
                    + " &nbsp;&nbsp; Assets = Liabilities + Equity"
                    + " &nbsp;\u2022&nbsp; Difference: \u20B1 0.00</html>"
                : "<html><b><font color='#AA1E1E'>Not balanced</font></b>"
                    + " &nbsp;&nbsp; Assets = Liabilities + Equity"
                    + " &nbsp;\u2022&nbsp; <font color='#AA1E1E'>Difference: \u20B1 "
                    + money(difference) + "</font></html>");

        } catch (Exception exception) {
            assets.setRowCount(0);
            other.setRowCount(0);
            resetTotals();
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
            new String[]{"As of", "Section", "Account", "Amount (PHP)"},
            0
        );
        copyRows(output, assets, "Assets");
        output.addRow(new Object[]{
            loadedDate.toString(), "Assets", "Total Assets", loadedTotalAssets
        });
        copyRows(output, other, "Liabilities & Equity");
        output.addRow(new Object[]{
            loadedDate.toString(), "Liabilities & Equity",
            "Total Liabilities & Equity", loadedTotalOther
        });
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
                source.getValueAt(row, 1)
            });
        }
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
             )
            WHERE a.active = TRUE
              AND a.type IN ('ASSET', 'LIABILITY', 'EQUITY', 'REVENUE', 'EXPENSE')
            GROUP BY a.id, a.name, a.type
            ORDER BY a.type, a.name
            """;

        List<AccountSummary> rows = new ArrayList<>();

        try (
            Connection connection = Database.connect();
            PreparedStatement statement = connection.prepareStatement(sql)
        ) {
            statement.setString(1, asOf.toString());

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

    // ---------------------------------------------------------------
    // Small custom-painted component
    // ---------------------------------------------------------------

    private static final int OPTION_REFRESH = 0;
    private static final int OPTION_DATE = 1;
    private static final int OPTION_EXPORT = 2;

    // Rounded outline button for "Options".
    private static class OptionsButton extends JButton {
        OptionsButton(String text) {
            super(text);
            setContentAreaFilled(false);
            setBorderPainted(false);
            setFocusPainted(false);
            setOpaque(false);
            setRolloverEnabled(true);
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            Graphics2D g = (Graphics2D) graphics.create();
            g.setRenderingHint(
                RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_ON);

            g.setColor(getModel().isRollover()
                ? new Color(245, 249, 255) : Color.WHITE);
            g.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 12, 12);

            g.setColor(CARD_BORDER);
            g.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 12, 12);

            g.dispose();
            super.paintComponent(graphics);
        }
    }

    // One clickable row in the options menu.
    private static class OptionRow extends JPanel {
        private boolean hover = false;

        OptionRow(int iconType, String text, Runnable action) {
            super(new BorderLayout(12, 0));
            setOpaque(false);
            setBorder(BorderFactory.createEmptyBorder(0, 10, 0, 16));
            setPreferredSize(new Dimension(210, 36));
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

            JLabel label = new JLabel(text);
            label.setFont(new Font("Inter", Font.PLAIN, 12));
            label.setForeground(TEXT);

            add(new OptionIcon(iconType), BorderLayout.WEST);
            add(label, BorderLayout.CENTER);

            addMouseListener(new java.awt.event.MouseAdapter() {
                @Override
                public void mouseEntered(java.awt.event.MouseEvent event) {
                    hover = true;
                    repaint();
                }

                @Override
                public void mouseExited(java.awt.event.MouseEvent event) {
                    hover = false;
                    repaint();
                }

                @Override
                public void mouseClicked(java.awt.event.MouseEvent event) {
                    hover = false;
                    action.run();
                }
            });
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            if (hover) {
                Graphics2D g = (Graphics2D) graphics.create();
                g.setRenderingHint(
                    RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON);
                g.setColor(PAGE_BG);
                g.fillRoundRect(0, 0, getWidth(), getHeight(), 10, 10);
                g.dispose();
            }

            super.paintComponent(graphics);
        }
    }

    private static class OptionIcon extends JComponent {
        private final int type;

        OptionIcon(int type) {
            this.type = type;
            setPreferredSize(new Dimension(18, 18));
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            Graphics2D g = (Graphics2D) graphics.create();
            g.setRenderingHint(
                RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_ON);

            g.setColor(TEXT);
            g.setStroke(new BasicStroke(
                1.4f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));

            // Draw on a 16x16 grid centered in the component.
            g.translate((getWidth() - 16) / 2, (getHeight() - 16) / 2);

            if (type == OPTION_REFRESH) {
                g.drawArc(2, 2, 12, 12, 40, 280);
                g.drawLine(13, 4, 13, 0);
                g.drawLine(13, 4, 9, 4);

            } else if (type == OPTION_DATE) {
                g.drawRoundRect(1, 3, 13, 11, 3, 3);
                g.drawLine(1, 7, 14, 7);
                g.drawLine(5, 1, 5, 4);
                g.drawLine(10, 1, 10, 4);

            } else {
                g.drawPolyline(
                    new int[]{2, 2, 14, 14},
                    new int[]{10, 14, 14, 10},
                    4);
                g.drawLine(8, 2, 8, 10);
                g.drawLine(5, 7, 8, 10);
                g.drawLine(11, 7, 8, 10);
            }

            g.dispose();
        }
    }

    private static class RoundedPanel extends JPanel {
        private final Color fill;
        private final Color borderColor;
        private final int arc;

        RoundedPanel(LayoutManager layout, Color fill, Color borderColor,
                     int arc) {
            super(layout);
            this.fill = fill;
            this.borderColor = borderColor;
            this.arc = arc;
            setOpaque(false);
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            Graphics2D g = (Graphics2D) graphics.create();
            g.setRenderingHint(
                RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_ON);

            g.setColor(fill);
            g.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, arc, arc);

            g.dispose();
            super.paintComponent(graphics);
        }

        // Keeps children (like the table header) inside the rounded corners.
        @Override
        protected void paintChildren(Graphics graphics) {
            Graphics2D g = (Graphics2D) graphics.create();
            g.clip(new RoundRectangle2D.Float(
                0, 0, getWidth(), getHeight(), arc, arc));
            super.paintChildren(g);
            g.dispose();
        }

        // Draw the outline after the children so they never cover it.
        @Override
        public void paint(Graphics graphics) {
            super.paint(graphics);

            if (borderColor != null) {
                Graphics2D g = (Graphics2D) graphics.create();
                g.setRenderingHint(
                    RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON);
                g.setColor(borderColor);
                g.drawRoundRect(
                    0, 0, getWidth() - 1, getHeight() - 1, arc, arc);
                g.dispose();
            }
        }
    }
}