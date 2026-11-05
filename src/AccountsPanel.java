import javax.swing.*;
import javax.swing.plaf.basic.BasicComboBoxUI;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.JTableHeader;
import java.awt.*;
import java.awt.geom.RoundRectangle2D;
import java.sql.*;
import java.util.Locale;

public class AccountsPanel extends JPanel {

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

    private final JTextField codeField = new HintTextField("e.g. 101");
    private final JTextField nameField = new HintTextField("e.g. Cash");

    private final JComboBox<String> typeBox = new JComboBox<>(
        new String[] {
            "ASSET",
            "LIABILITY",
            "EQUITY",
            "REVENUE",
            "EXPENSE"
        }
    );

    private final DefaultTableModel tableModel =
        new DefaultTableModel(
            new String[] {"ID", "Code", "Account Name", "Type"},
            0
        ) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

    public AccountsPanel() {
        setLayout(new BorderLayout(0, 16));
        setBackground(PAGE_BG);
        setBorder(
            BorderFactory.createEmptyBorder(28, 30, 20, 30)
        );

        JLabel heading = new JLabel("CHART OF ACCOUNTS");
        heading.setFont(new Font("Inter", Font.BOLD, 22)); //ge change nako font
        heading.setForeground(NAVY);

        JLabel subtitle = new JLabel(
            "Organize the accounts used across your books."
        );
        subtitle.setFont(new Font("Inter", Font.PLAIN, 12));
        subtitle.setForeground(MUTED);

        styleText(codeField);
        styleText(nameField);
        styleCombo(typeBox);

        // Account input form
        RoundedPanel formCard = new RoundedPanel(
            new GridBagLayout(), Color.WHITE, CARD_BORDER, 18
        );
        formCard.setBorder(
            BorderFactory.createEmptyBorder(16, 20, 14, 20)
        );

        cell(formCard, fieldLabel("Account code"), 0, 0, 1,
            new Insets(0, 0, 6, 12));
        cell(formCard, fieldLabel("Account name"), 1, 0, 2,
            new Insets(0, 0, 6, 12));
        cell(formCard, fieldLabel("Account type"), 2, 0, 1,
            new Insets(0, 0, 6, 0));

        cell(formCard, inputBox(codeField), 0, 1, 1,
            new Insets(0, 0, 12, 12));
        cell(formCard, inputBox(nameField), 1, 1, 2,
            new Insets(0, 0, 12, 12));
        cell(formCard, inputBox(typeBox), 2, 1, 1,
            new Insets(0, 0, 12, 0));

        JButton addButton = new RoundedButton("Add Account");
        JButton clearButton = textButton("Clear Form");
        JButton refreshButton = textButton("Refresh Accounts");

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
        actions.setOpaque(false);
        actions.add(addButton);
        actions.add(clearButton);
        actions.add(refreshButton);

        GridBagConstraints actionsConstraints = new GridBagConstraints();
        actionsConstraints.gridx = 0;
        actionsConstraints.gridy = 2;
        actionsConstraints.gridwidth = 3;
        actionsConstraints.weightx = 1;
        actionsConstraints.fill = GridBagConstraints.HORIZONTAL;
        actionsConstraints.anchor = GridBagConstraints.WEST;
        formCard.add(actions, actionsConstraints);

        JPanel top = new JPanel(new GridBagLayout());
        top.setOpaque(false);

        cell(top, heading, 0, 0, 1, new Insets(0, 0, 0, 0));
        cell(top, subtitle, 0, 1, 1, new Insets(4, 0, 0, 0));
        cell(top, formCard, 0, 2, 1, new Insets(18, 0, 0, 0));

        // Saved accounts table (also draws the empty message).
        JTable table = new JTable(tableModel) {
            @Override
            protected void paintComponent(Graphics graphics) {
                super.paintComponent(graphics);

                if (getRowCount() == 0) {
                    paintEmptyState(graphics, getVisibleRect());
                }
            }
        };

        table.setRowHeight(32);
        table.setFillsViewportHeight(true);
        table.setAutoCreateRowSorter(true);
        table.getTableHeader().setReorderingAllowed(false);
        table.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
        table.setShowGrid(false);
        table.setShowHorizontalLines(true);
        table.setGridColor(ROW_LINE);
        table.setIntercellSpacing(new Dimension(0, 1));
        table.setBackground(Color.WHITE);
        table.setForeground(TEXT);
        table.setFont(INPUT_FONT);
        table.setSelectionBackground(new Color(232, 240, 252));
        table.setSelectionForeground(TEXT);

        int[] widths = {70, 120, 420, 160};

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
        table.setDefaultRenderer(Integer.class, cellRenderer);
        table.setDefaultRenderer(Object.class, cellRenderer);

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

        add(top, BorderLayout.NORTH);
        add(tableCard, BorderLayout.CENTER);

        addButton.addActionListener(event -> addAccount());
        clearButton.addActionListener(event -> clearForm());
        refreshButton.addActionListener(event -> loadAccounts());

        loadAccounts();
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

    private static void styleCombo(JComboBox<String> combo) {
        combo.setUI(new FlatComboBoxUI());
        combo.setOpaque(false);
        combo.setBackground(FIELD_BG);
        combo.setForeground(TEXT);
        combo.setBorder(BorderFactory.createEmptyBorder());
        combo.setFont(INPUT_FONT);
        combo.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        // Shows "Asset" instead of "ASSET"; the saved value is unchanged.
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

                if (value != null) {
                    String text = value.toString();

                    if (!text.isEmpty()) {
                        setText(
                            text.substring(0, 1).toUpperCase(Locale.ROOT)
                                + text.substring(1).toLowerCase(Locale.ROOT)
                        );
                    }
                }

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
        String title = "No accounts added";
        g.drawString(
            title,
            cx - g.getFontMetrics().stringWidth(title) / 2,
            cy - 6
        );

        g.setFont(new Font("Inter", Font.PLAIN, 10));
        g.setColor(MUTED);
        String text =
            "Add an account above to start building your chart of accounts.";
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

    private void addAccount() {
        String code = codeField.getText().trim();
        String name = nameField.getText().trim();
        String type = (String) typeBox.getSelectedItem();

        if (code.isEmpty() || name.isEmpty()) {
            JOptionPane.showMessageDialog(
                this,
                "Enter an account code and account name.",
                "Missing Information",
                JOptionPane.WARNING_MESSAGE
            );
            return;
        }

        String sql =
            "INSERT INTO accounts (code, name, type) VALUES (?, ?, ?)";

        try (
            Connection connection = Database.connect()
        ) {
            // Check whether the account code is already used.
            String checkSql = "SELECT 1 FROM accounts WHERE code = ?";

            try (
                PreparedStatement check =
                    connection.prepareStatement(checkSql)
            ) {
                check.setString(1, code);

                try (ResultSet result = check.executeQuery()) {
                    if (result.next()) {
                        JOptionPane.showMessageDialog(
                            this,
                            "That account code already exists.",
                            "Duplicate Account Code",
                            JOptionPane.WARNING_MESSAGE
                        );
                        return;
                    }
                }
            }

            try (
                PreparedStatement statement =
                    connection.prepareStatement(sql)
            ) {
                statement.setString(1, code);
                statement.setString(2, name);
                statement.setString(3, type);
                statement.executeUpdate();
            }

        } catch (SQLException exception) {
            showDatabaseError("Could not save the account.", exception);
            return;
        }

        clearForm();
        loadAccounts();

        JOptionPane.showMessageDialog(
            this,
            "Account saved successfully."
        );
    }

    private void loadAccounts() {
        String sql =
            "SELECT id, code, name, type FROM accounts ORDER BY code";

        // Read records first so a failed refresh preserves the table.
        java.util.List<Object[]> rows = new java.util.ArrayList<>();

        try (
            Connection connection = Database.connect();
            PreparedStatement statement =
                connection.prepareStatement(sql);
            ResultSet result = statement.executeQuery()
        ) {
            while (result.next()) {
                rows.add(new Object[] {
                    result.getInt("id"),
                    result.getString("code"),
                    result.getString("name"),
                    result.getString("type")
                });
            }

        } catch (SQLException exception) {
            showDatabaseError("Could not load accounts.", exception);
            return;
        }

        tableModel.setRowCount(0);

        for (Object[] row : rows) {
            tableModel.addRow(row);
        }
    }

    private void clearForm() {
        codeField.setText("");
        nameField.setText("");
        typeBox.setSelectedIndex(0);
        codeField.requestFocusInWindow();
    }

    private void showDatabaseError(
        String message,
        SQLException exception
    ) {
        exception.printStackTrace();

        JOptionPane.showMessageDialog(
            this,
            message + "\n" + exception.getMessage(),
            "Database Error",
            JOptionPane.ERROR_MESSAGE
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