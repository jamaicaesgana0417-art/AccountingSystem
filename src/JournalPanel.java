import javax.swing.*;
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

public class JournalPanel extends JPanel {

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
        new HintTextField("Search journal entries");
    private final JTextField startField = new HintTextField("YYYY-MM-DD");
    private final JTextField endField = new HintTextField("YYYY-MM-DD");

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

    // Also draws the "No matching journal entries" message when empty.
    private final JTable table = new JTable(model) {
        @Override
        protected void paintComponent(Graphics graphics) {
            super.paintComponent(graphics);

            if (getRowCount() == 0) {
                paintEmptyState(graphics, getVisibleRect());
            }
        }
    };

    public JournalPanel() {
        setLayout(new BorderLayout(0, 16));
        setBackground(PAGE_BG);
        setBorder(
            BorderFactory.createEmptyBorder(28, 30, 20, 30)
        );

        JLabel heading = new JLabel("GENERAL JOURNAL");
        heading.setFont(new Font("Inter", Font.BOLD, 22)); //ge change nako font
        heading.setForeground(NAVY);

        JLabel subtitle = new JLabel(
            "Review posted entries in chronological order."
        );
        subtitle.setFont(new Font("Inter", Font.PLAIN, 12));
        subtitle.setForeground(MUTED);

        styleText(searchField);
        styleText(startField);
        styleText(endField);

        // ----- Filter card -----
        RoundedPanel filterCard = new RoundedPanel(
            new GridBagLayout(), Color.WHITE, CARD_BORDER, 18
        );
        filterCard.setBorder(
            BorderFactory.createEmptyBorder(16, 20, 14, 20)
        );

        cell(filterCard, fieldLabel("Search"), 0, 0, 2.5,
            new Insets(0, 0, 6, 12));
        cell(filterCard, fieldLabel("From date (optional)"), 1, 0, 1,
            new Insets(0, 0, 6, 12));
        cell(filterCard, fieldLabel("To date (optional)"), 2, 0, 1,
            new Insets(0, 0, 6, 0));

        cell(filterCard, inputBox(searchField), 0, 1, 2.5,
            new Insets(0, 0, 12, 12));
        cell(filterCard, inputBox(startField), 1, 1, 1,
            new Insets(0, 0, 12, 12));
        cell(filterCard, inputBox(endField), 2, 1, 1,
            new Insets(0, 0, 12, 0));

        JButton applyButton = new RoundedButton("Apply Filters");
        JButton clearButton = textButton("Clear Filters");
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

        // Keep chronological order and matching journal lines together.
        int[] widths = {100, 125, 245, 215, 145, 145};

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

        table.getColumnModel().getColumn(4)
            .setCellRenderer(moneyRenderer);

        table.getColumnModel().getColumn(5)
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

        totalsLabel.setFont(new Font("Inter", Font.PLAIN, 11));
        totalsLabel.setForeground(MUTED);
        totalsLabel.setBorder(
            BorderFactory.createEmptyBorder(0, 4, 0, 0)
        );

        add(top, BorderLayout.NORTH);
        add(tableCard, BorderLayout.CENTER);
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
        String title = "No matching journal entries";
        g.drawString(
            title,
            cx - g.getFontMetrics().stringWidth(title) / 2,
            cy - 6
        );

        g.setFont(new Font("Inter", Font.PLAIN, 10));
        g.setColor(MUTED);
        String text =
            "Posted transactions will appear here as debit and credit lines.";
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

        // One footer line, like the mockup. The warning still appears
        // when the displayed debits and credits are not equal.
        totalsLabel.setText(
            "<html><font color='#6E7D91'>Displayed lines: "
                + rows.size()
                + "</font> &nbsp;&nbsp;&nbsp;&nbsp; "
                + "<b><font color='#163866'>Total debit: \u20B1 "
                + totalDebit.toPlainString()
                + "</font></b> &nbsp;&nbsp;&nbsp;&nbsp; "
                + "<b><font color='#163866'>Total credit: \u20B1 "
                + totalCredit.toPlainString()
                + "</font></b>"
                + (balanced
                    ? ""
                    : " &nbsp;&nbsp;&nbsp;&nbsp; <b><font color='#D32F2F'>"
                        + "Warning: Displayed debits and credits differ."
                        + "</font></b>")
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