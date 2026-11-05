import javax.swing.*;
import javax.swing.plaf.basic.BasicComboBoxUI;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableRowSorter;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.awt.geom.RoundRectangle2D;
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

    private final JTextField searchField =
        new HintTextField("Search reference or description");
    private final JTextField startDateField = new HintTextField("YYYY-MM-DD");
    private final JTextField endDateField = new HintTextField("YYYY-MM-DD");

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

    // Also draws the "No transactions yet" message when it is empty.
    private final JTable table = new JTable(model) {
        @Override
        protected void paintComponent(Graphics graphics) {
            super.paintComponent(graphics);

            if (getRowCount() == 0) {
                paintEmptyState(graphics, getVisibleRect());
            }
        }
    };

    public TransactionsPanel() {
        setLayout(new BorderLayout(0, 16));
        setBackground(PAGE_BG);
        setBorder(
            BorderFactory.createEmptyBorder(28, 30, 20, 30)
        );

        JLabel heading = new JLabel("TRANSACTIONS");
        heading.setFont(new Font("Inter", Font.BOLD, 22)); //ge change nako font
        heading.setForeground(NAVY);

        JLabel subtitle = new JLabel(
            "Search, review, and export your posted transactions."
        );
        subtitle.setFont(new Font("Inter", Font.PLAIN, 12));
        subtitle.setForeground(MUTED);

        styleText(searchField);
        styleText(startDateField);
        styleText(endDateField);
        styleCombo(accountBox);

        // ----- Filter card -----
        RoundedPanel filterCard = new RoundedPanel(
            new GridBagLayout(), Color.WHITE, CARD_BORDER, 18
        );
        filterCard.setBorder(
            BorderFactory.createEmptyBorder(16, 20, 14, 20)
        );

        cell(filterCard, fieldLabel("Search"), 0, 0, 1.6,
            new Insets(0, 0, 6, 12));
        cell(filterCard, fieldLabel("From date (optional)"), 1, 0, 1,
            new Insets(0, 0, 6, 12));
        cell(filterCard, fieldLabel("To date (optional)"), 2, 0, 1,
            new Insets(0, 0, 6, 12));
        cell(filterCard, fieldLabel("Debit or credit account"), 3, 0, 1.6,
            new Insets(0, 0, 6, 0));

        cell(filterCard, inputBox(searchField), 0, 1, 1.6,
            new Insets(0, 0, 12, 12));
        cell(filterCard, inputBox(startDateField), 1, 1, 1,
            new Insets(0, 0, 12, 12));
        cell(filterCard, inputBox(endDateField), 2, 1, 1,
            new Insets(0, 0, 12, 12));
        cell(filterCard, inputBox(accountBox), 3, 1, 1.6,
            new Insets(0, 0, 12, 0));

        JButton applyButton = new RoundedButton("Apply Filters");
        JButton clearButton = textButton("Clear Filters");
        JButton refreshButton = textButton("Refresh");
        JButton exportButton = textButton("Export CSV");

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
        actions.setOpaque(false);
        actions.add(applyButton);
        actions.add(clearButton);
        actions.add(refreshButton);
        actions.add(exportButton);

        GridBagConstraints actionsConstraints = new GridBagConstraints();
        actionsConstraints.gridx = 0;
        actionsConstraints.gridy = 2;
        actionsConstraints.gridwidth = 4;
        actionsConstraints.weightx = 1;
        actionsConstraints.fill = GridBagConstraints.HORIZONTAL;
        actionsConstraints.anchor = GridBagConstraints.WEST;
        filterCard.add(actions, actionsConstraints);

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

        TableRowSorter<DefaultTableModel> sorter =
            new TableRowSorter<>(model);

        // Keep updated results in their default SQL order.
        sorter.setSortsOnUpdates(true);
        table.setRowSorter(sorter);

        int[] widths = {105, 155, 291, 205, 205, 160}; //ge change nako size sa width niya

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

        table.getColumnModel()
            .getColumn(5)
            .setCellRenderer(moneyRenderer);

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

                    String text = String.valueOf(value);

                    if (table.getRowSorter() != null) {
                        int modelColumn =
                            table.convertColumnIndexToModel(column);

                        for (RowSorter.SortKey key
                                : table.getRowSorter().getSortKeys()) {
                            if (key.getColumn() == modelColumn
                                    && key.getSortOrder()
                                        != SortOrder.UNSORTED) {
                                text += key.getSortOrder()
                                    == SortOrder.ASCENDING
                                    ? "  \u25B2" : "  \u25BC";
                            }
                        }
                    }

                    setText(text);
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

        countLabel.setFont(new Font("Inter", Font.PLAIN, 11));
        countLabel.setForeground(MUTED);
        countLabel.setBorder(
            BorderFactory.createEmptyBorder(0, 4, 0, 0)
        );

        add(top, BorderLayout.NORTH);
        add(tableCard, BorderLayout.CENTER);
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

    private static void styleCombo(JComboBox<AccountOption> combo) {
        combo.setUI(new FlatComboBoxUI());
        combo.setOpaque(false);
        combo.setBackground(FIELD_BG);
        combo.setForeground(TEXT);
        combo.setBorder(BorderFactory.createEmptyBorder());
        combo.setFont(INPUT_FONT);
        combo.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        combo.setRenderer(new DefaultListCellRenderer() {
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
                setBorder(
                    BorderFactory.createEmptyBorder(4, 6, 4, 6)
                );
                return this;
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
        String title = "No transactions yet";
        g.drawString(
            title,
            cx - g.getFontMetrics().stringWidth(title) / 2,
            cy - 6
        );

        g.setFont(new Font("Inter", Font.PLAIN, 10));
        g.setColor(MUTED);
        String text =
            "Post your first transaction or adjust your filters to see results.";
        g.drawString(
            text,
            cx - g.getFontMetrics().stringWidth(text) / 2,
            cy + 12
        );

        g.dispose();
    }

    // ---------------------------------------------------------------
    // Logic (unchanged)
    // ---------------------------------------------------------------

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
            "<html><font color='#6E7D91'>Displayed transactions: "
                + rows.size()
                + "</font> &nbsp;&nbsp;&nbsp;&nbsp; "
                + "<b><font color='#163866'>Total amount: \u20B1 "
                + total.toPlainString()
                + "</font></b></html>"
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