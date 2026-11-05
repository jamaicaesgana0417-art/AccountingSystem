import javax.swing.*;
import javax.swing.plaf.basic.BasicComboBoxUI;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.JTableHeader;
import java.awt.*;
import java.awt.geom.RoundRectangle2D;
import java.math.BigDecimal;
import java.sql.*;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;

public class LedgerPanel extends JPanel {

    private static final Color PAGE_BG = new Color(240, 245, 252);
    private static final Color NAVY = new Color(22, 56, 102);
    private static final Color FIELD_BG = new Color(239, 243, 250);
    private static final Color FIELD_BORDER = new Color(224, 231, 242);
    private static final Color CARD_BORDER = new Color(220, 228, 240);
    private static final Color HEADER_BG = new Color(226, 234, 250);
    private static final Color ROW_LINE = new Color(235, 240, 248);
    private static final Color MUTED = new Color(110, 125, 145);
    private static final Color PLACEHOLDER = new Color(140, 152, 170);
    private static final Color TEXT = new Color(45, 60, 85);
    private static final Color BLUE_BUTTON = new Color(60, 143, 200);

    private static final Font INPUT_FONT =
        new Font("Inter", Font.PLAIN, 12);

    private final JComboBox<AccountOption> accountBox =
        new JComboBox<>();

    private final JTextField startField = new HintTextField("YYYY-MM-DD");
    private final JTextField endField = new HintTextField("YYYY-MM-DD");

    private final JLabel summaryLabel =
        new JLabel(
            "Running balances update automatically from posted journal entries."
        );

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

    // Also draws the "Choose an account" message when it is empty.
    private final JTable table = new JTable(model) {
        @Override
        protected void paintComponent(Graphics graphics) {
            super.paintComponent(graphics);

            if (getRowCount() == 0) {
                paintEmptyState(graphics, getVisibleRect());
            }
        }
    };

    public LedgerPanel() {
        setLayout(new BorderLayout(0, 16));
        setBackground(PAGE_BG);
        setBorder(
            BorderFactory.createEmptyBorder(28, 30, 20, 30)
        );

        JLabel heading = new JLabel("General Ledger");
        heading.setFont(new Font("Inter", Font.BOLD, 22)); //I change the font
        heading.setForeground(NAVY);

        JLabel subtitle = new JLabel(
            "Track each account's activity and running balance."
        );
        subtitle.setFont(new Font("Inter", Font.PLAIN, 12));
        subtitle.setForeground(MUTED);

        styleText(startField);
        styleText(endField);
        styleCombo(accountBox, "Select an account");

        // ----- Filter card -----
        RoundedPanel filterCard = new RoundedPanel(
            new GridBagLayout(), Color.WHITE, CARD_BORDER, 18
        );
        filterCard.setBorder(
            BorderFactory.createEmptyBorder(16, 20, 14, 20)
        );

        cell(filterCard, fieldLabel("Account"), 0, 0, 2.5,
            new Insets(0, 0, 6, 12));
        cell(filterCard, fieldLabel("From date (optional)"), 1, 0, 1,
            new Insets(0, 0, 6, 12));
        cell(filterCard, fieldLabel("To date (optional)"), 2, 0, 1,
            new Insets(0, 0, 6, 0));

        cell(filterCard, inputBox(accountBox), 0, 1, 2.5,
            new Insets(0, 0, 12, 12));
        cell(filterCard, inputBox(startField), 1, 1, 1,
            new Insets(0, 0, 12, 12));
        cell(filterCard, inputBox(endField), 2, 1, 1,
            new Insets(0, 0, 12, 0));

        JButton applyButton = new RoundedButton("Apply Filters");
        JButton clearButton = textButton("Clear Dates");
        JButton refreshButton = textButton("Refresh");
        JButton exportButton = textButton("Export CSV");

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
        buttons.setOpaque(false);
        buttons.add(applyButton);
        buttons.add(clearButton);
        buttons.add(refreshButton);
        buttons.add(exportButton);

        GridBagConstraints buttonsConstraints = new GridBagConstraints();
        buttonsConstraints.gridx = 0;
        buttonsConstraints.gridy = 2;
        buttonsConstraints.gridwidth = 3;
        buttonsConstraints.weightx = 1;
        buttonsConstraints.fill = GridBagConstraints.HORIZONTAL;
        buttonsConstraints.anchor = GridBagConstraints.WEST;
        filterCard.add(buttons, buttonsConstraints);

        // ----- Top: heading, subtitle, filters -----
        JPanel top = new JPanel(new GridBagLayout());
        top.setOpaque(false);

        cell(top, heading, 0, 0, 1, new Insets(0, 0, 0, 0));
        cell(top, subtitle, 0, 1, 1, new Insets(4, 0, 0, 0));
        cell(top, filterCard, 0, 2, 1, new Insets(18, 0, 0, 0));

        // ----- Table -----
        table.setRowHeight(32);
        table.setFillsViewportHeight(true);
        table.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS); //ge change nako para mo fit siya sa content sa table
        table.getTableHeader().setReorderingAllowed(false);
        table.setShowGrid(false);
        table.setShowHorizontalLines(true);
        table.setGridColor(ROW_LINE);
        table.setIntercellSpacing(new Dimension(0, 1));
        table.setBackground(Color.WHITE);
        table.setForeground(TEXT);
        table.setFont(INPUT_FONT);
        table.setSelectionBackground(new Color(232, 240, 252));
        table.setSelectionForeground(TEXT);

        // Keep chronological order so running balances remain meaningful.
        int[] widths = {200, 85, 110, 240, 120, 120, 130, 60};

        for (int i = 0; i < widths.length; i++) {
            table.getColumnModel()
                .getColumn(i)
                .setPreferredWidth(widths[i]);
        }

        // Left-aligned cells with a little side padding.
        DefaultTableCellRenderer cellRenderer =
            new DefaultTableCellRenderer() {
                @Override
                public Component getTableCellRendererComponent(
                    JTable table, Object value, boolean isSelected,
                    boolean hasFocus, int row, int column
                ) {
                    super.getTableCellRendererComponent(
                        table, value, isSelected, false, row, column
                    );
                    setHorizontalAlignment(SwingConstants.LEFT);
                    setBorder(
                        BorderFactory.createEmptyBorder(0, 12, 0, 12)
                    );
                    return this;
                }
            };

        table.setDefaultRenderer(String.class, cellRenderer);
        table.setDefaultRenderer(Long.class, cellRenderer);

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

                @Override
                public Component getTableCellRendererComponent(
                    JTable table, Object value, boolean isSelected,
                    boolean hasFocus, int row, int column
                ) {
                    super.getTableCellRendererComponent(
                        table, value, isSelected, false, row, column
                    );
                    setBorder(
                        BorderFactory.createEmptyBorder(0, 12, 0, 12)
                    );
                    return this;
                }
            };

        moneyRenderer.setHorizontalAlignment(SwingConstants.RIGHT);

        for (int column = 4; column <= 6; column++) {
            table.getColumnModel()
                .getColumn(column)
                .setCellRenderer(moneyRenderer);
        }

        // Light blue header band, like the mockup.
        DefaultTableCellRenderer headerRenderer =
            new DefaultTableCellRenderer() {
                @Override
                public Component getTableCellRendererComponent(
                    JTable table, Object value, boolean isSelected,
                    boolean hasFocus, int row, int column
                ) {
                    super.getTableCellRendererComponent(
                        table, value, false, false, row, column
                    );
                    setBackground(HEADER_BG);
                    setForeground(NAVY);
                    setFont(new Font("Inter", Font.BOLD, 10));
                    setHorizontalAlignment(SwingConstants.LEFT);
                    setBorder(
                        BorderFactory.createEmptyBorder(0, 12, 0, 12)
                    );
                    return this;
                }
            };

        JTableHeader tableHeader = table.getTableHeader();
        tableHeader.setDefaultRenderer(headerRenderer);
        tableHeader.setBackground(HEADER_BG);
        tableHeader.setOpaque(true);
        tableHeader.setPreferredSize(
            new Dimension(tableHeader.getPreferredSize().width, 36)
        );

        JScrollPane scroll = new JScrollPane(table);
        scroll.setBorder(null);
        scroll.getViewport().setBackground(Color.WHITE);

        RoundedPanel tableCard = new RoundedPanel(
            new BorderLayout(), Color.WHITE, CARD_BORDER, 18
        );
        tableCard.add(scroll, BorderLayout.CENTER);

        summaryLabel.setFont(new Font("Inter", Font.PLAIN, 11));
        summaryLabel.setForeground(MUTED);
        summaryLabel.setBorder(
            BorderFactory.createEmptyBorder(0, 4, 0, 0)
        );

        add(top, BorderLayout.NORTH);
        add(tableCard, BorderLayout.CENTER);
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

    // ---------------------------------------------------------------
    // Design helpers
    // ---------------------------------------------------------------

    private static void cell(
        JPanel panel,
        JComponent component,
        int x,
        int y,
        double weightx,
        Insets insets
    ) {
        GridBagConstraints c = new GridBagConstraints();
        c.gridx = x;
        c.gridy = y;
        c.weightx = weightx;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.anchor = GridBagConstraints.NORTHWEST;
        c.insets = insets;
        panel.add(component, c);
    }

    private static JLabel fieldLabel(String text) {
        JLabel label = new JLabel(text);
        label.setFont(new Font("Inter", Font.PLAIN, 10));
        label.setForeground(TEXT);
        return label;
    }

    private static void styleText(JTextField field) {
        field.setOpaque(false);
        field.setBorder(BorderFactory.createEmptyBorder());
        field.setFont(INPUT_FONT);
        field.setForeground(TEXT);
    }

    private static void styleCombo(
        JComboBox<AccountOption> combo,
        String placeholder
    ) {
        combo.setUI(new FlatComboBoxUI());
        combo.setOpaque(false);
        combo.setBackground(FIELD_BG);
        combo.setForeground(TEXT);
        combo.setBorder(BorderFactory.createEmptyBorder());
        combo.setFont(INPUT_FONT);
        combo.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        combo.setRenderer(new DefaultListCellRenderer() {
            private boolean showingPlaceholder = false;

            @Override
            public Component getListCellRendererComponent(
                JList<?> list,
                Object value,
                int index,
                boolean isSelected,
                boolean cellHasFocus
            ) {
                super.getListCellRendererComponent(
                    list, value, index, isSelected, cellHasFocus
                );

                showingPlaceholder = (value == null);

                if (showingPlaceholder) {
                    setText(placeholder);
                }

                setBorder(
                    BorderFactory.createEmptyBorder(4, 6, 4, 6)
                );
                return this;
            }

            @Override
            public Color getForeground() {
                return showingPlaceholder
                    ? PLACEHOLDER
                    : super.getForeground();
            }
        });
    }

    private static RoundedPanel inputBox(JComponent inner) {
        RoundedPanel box = new RoundedPanel(
            new BorderLayout(), FIELD_BG, FIELD_BORDER, 10
        );
        box.setBorder(BorderFactory.createEmptyBorder(0, 10, 0, 10));
        box.setPreferredSize(new Dimension(90, 36));
        box.add(inner, BorderLayout.CENTER);
        return box;
    }

    private static JButton textButton(String text) {
        JButton button = new JButton(text);
        button.setFont(new Font("Inter", Font.PLAIN, 12));
        button.setForeground(TEXT);
        button.setContentAreaFilled(false);
        button.setBorderPainted(false);
        button.setFocusPainted(false);
        button.setBorder(BorderFactory.createEmptyBorder(9, 14, 9, 14));
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return button;
    }

    // Icon + message shown in the table area when there are no rows.
    private void paintEmptyState(Graphics graphics, Rectangle area) {
        Graphics2D g = (Graphics2D) graphics.create();
        g.setRenderingHint(
            RenderingHints.KEY_ANTIALIASING,
            RenderingHints.VALUE_ANTIALIAS_ON
        );
        g.setRenderingHint(
            RenderingHints.KEY_TEXT_ANTIALIASING,
            RenderingHints.VALUE_TEXT_ANTIALIAS_ON
        );

        int cx = area.x + area.width / 2;
        int cy = area.y + area.height / 2;

        // Small table icon
        g.setColor(BLUE_BUTTON);
        g.setStroke(new BasicStroke(1.5f));
        g.drawRoundRect(cx - 9, cy - 44, 18, 18, 3, 3);
        g.drawLine(cx - 9, cy - 38, cx + 9, cy - 38);
        g.drawLine(cx - 9, cy - 32, cx + 9, cy - 32);
        g.drawLine(cx, cy - 44, cx, cy - 26);

        g.setFont(new Font("Inter", Font.BOLD, 13));
        g.setColor(NAVY);
        String title = "Choose an account to view its ledger";
        g.drawString(
            title,
            cx - g.getFontMetrics().stringWidth(title) / 2,
            cy - 6
        );

        g.setFont(new Font("Inter", Font.PLAIN, 10));
        g.setColor(MUTED);
        String text =
            "No accounts available. Add an account in Chart of Accounts to begin.";
        g.drawString(
            text,
            cx - g.getFontMetrics().stringWidth(text) / 2,
            cy + 12
        );

        g.dispose();
    }

    // ---------------------------------------------------------------
    // Logic (unchanged, except the footer text layout)
    // ---------------------------------------------------------------

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

        // Same information as before, laid out in a compact footer.
        summaryLabel.setForeground(Color.DARK_GRAY);
        summaryLabel.setText(
            "<html><font color='#6E7D91'>Account: " + account.code
                + " &nbsp;|&nbsp; Type: " + account.type
                + " &nbsp;|&nbsp; Normal side: " + normalSide
                + " &nbsp;|&nbsp; Period: "
                + (start == null ? "Beginning" : start.toString())
                + " to "
                + (end == null ? "Latest entry" : end.toString())
                + "</font><br><b><font color='#163866'>Opening: \u20B1 "
                + opening.abs().toPlainString()
                + " " + side(opening)
                + " &nbsp;|&nbsp; Period Debit: \u20B1 "
                + periodDebit.toPlainString()
                + " &nbsp;|&nbsp; Period Credit: \u20B1 "
                + periodCredit.toPlainString()
                + " &nbsp;|&nbsp; Closing: \u20B1 "
                + runningBalance.abs().toPlainString()
                + " " + side(runningBalance)
                + "</font></b> &nbsp;&nbsp; <font color='#6E7D91'>Entries: "
                + entries.size()
                + "<br>Running balances update automatically from posted "
                + "journal entries.</font></html>"
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

    // ---------------------------------------------------------------
    // Small custom-painted components
    // ---------------------------------------------------------------

    private static class RoundedPanel extends JPanel {
        private final Color fill;
        private final Color borderColor;
        private final int arc;

        RoundedPanel(
            LayoutManager layout, Color fill, Color borderColor, int arc
        ) {
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
                RenderingHints.VALUE_ANTIALIAS_ON
            );

            g.setColor(fill);
            g.fillRoundRect(
                0, 0, getWidth() - 1, getHeight() - 1, arc, arc
            );

            g.dispose();
            super.paintComponent(graphics);
        }

        // Keeps children (like the table header) inside the rounded corners.
        @Override
        protected void paintChildren(Graphics graphics) {
            Graphics2D g = (Graphics2D) graphics.create();
            g.clip(
                new RoundRectangle2D.Float(
                    0, 0, getWidth(), getHeight(), arc, arc
                )
            );
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
                    RenderingHints.VALUE_ANTIALIAS_ON
                );
                g.setColor(borderColor);
                g.drawRoundRect(
                    0, 0, getWidth() - 1, getHeight() - 1, arc, arc
                );
                g.dispose();
            }
        }
    }

    private static class RoundedButton extends JButton {
        RoundedButton(String text) {
            super(text);
            setContentAreaFilled(false);
            setBorderPainted(false);
            setFocusPainted(false);
            setOpaque(false);
            setRolloverEnabled(true);
            setForeground(Color.WHITE);
            setFont(new Font("Inter", Font.BOLD, 12));
            setBorder(BorderFactory.createEmptyBorder(9, 20, 9, 20));
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            Graphics2D g = (Graphics2D) graphics.create();
            g.setRenderingHint(
                RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_ON
            );

            if (!isEnabled()) {
                g.setColor(new Color(170, 190, 210));
            } else if (getModel().isRollover()) {
                g.setColor(new Color(45, 125, 185));
            } else {
                g.setColor(BLUE_BUTTON);
            }

            g.fillRoundRect(0, 0, getWidth(), getHeight(), 10, 10);
            g.dispose();

            super.paintComponent(graphics);
        }
    }

    private static class FlatComboBoxUI extends BasicComboBoxUI {

        @Override
        protected JButton createArrowButton() {
            return new ChevronButton();
        }

        // The rounded box behind the combo already supplies the fill.
        @Override
        public void paintCurrentValueBackground(
            Graphics g, Rectangle bounds, boolean hasFocus
        ) {
        }

        @Override
        public void paintCurrentValue(
            Graphics g, Rectangle bounds, boolean hasFocus
        ) {
            // Never draw the blue "selected" look on the current value.
            super.paintCurrentValue(g, bounds, false);
        }
    }

    private static class ChevronButton extends JButton {
        ChevronButton() {
            setBorder(BorderFactory.createEmptyBorder());
            setContentAreaFilled(false);
            setFocusPainted(false);
            setOpaque(false);
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            Graphics2D g = (Graphics2D) graphics.create();
            g.setRenderingHint(
                RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_ON
            );

            g.setColor(TEXT);
            g.setStroke(
                new BasicStroke(
                    1.8f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND
                )
            );

            int cx = getWidth() / 2;
            int cy = getHeight() / 2;

            g.drawLine(cx - 4, cy - 2, cx, cy + 2);
            g.drawLine(cx, cy + 2, cx + 4, cy - 2);

            g.dispose();
        }
    }

    private static class HintTextField extends JTextField {
        private final String hint;

        HintTextField(String hint) {
            this.hint = hint;
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            super.paintComponent(graphics);

            if (getText().isEmpty()) {
                Graphics2D g = (Graphics2D) graphics.create();
                g.setRenderingHint(
                    RenderingHints.KEY_TEXT_ANTIALIASING,
                    RenderingHints.VALUE_TEXT_ANTIALIAS_ON
                );
                g.setColor(PLACEHOLDER);
                g.setFont(getFont());

                FontMetrics metrics = g.getFontMetrics();
                Insets insets = getInsets();
                int y = (getHeight() - metrics.getHeight()) / 2
                    + metrics.getAscent();

                g.drawString(hint, insets.left, y);
                g.dispose();
            }
        }
    }
}