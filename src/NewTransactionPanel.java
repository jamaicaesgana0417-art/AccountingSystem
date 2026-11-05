import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.plaf.basic.BasicComboBoxUI;
import javax.swing.text.JTextComponent;
import java.awt.*;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.*;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;

public class NewTransactionPanel extends JPanel {

    private static final Color PAGE_BG = new Color(240, 245, 252);
    private static final Color NAVY = new Color(22, 56, 102);
    private static final Color FIELD_BG = new Color(239, 243, 250);
    private static final Color FIELD_BORDER = new Color(224, 231, 242);
    private static final Color CARD_BORDER = new Color(220, 228, 240);
    private static final Color MUTED = new Color(110, 125, 145);
    private static final Color PLACEHOLDER = new Color(140, 152, 170);
    private static final Color TEXT = new Color(45, 60, 85);
    private static final Color BLUE_BUTTON = new Color(60, 143, 200);
    private static final Color LIGHT_ON_NAVY = new Color(205, 218, 238);

    private static final Font INPUT_FONT =
        new Font("Inter", Font.PLAIN, 12);

    private final JTextField dateField =
        new JTextField(LocalDate.now().toString());

    private final JTextArea descriptionField =
        new HintTextArea("Enter the purpose of this transaction", 4, 20);

    private final JComboBox<AccountOption> debitBox =
        new JComboBox<>();

    private final JComboBox<AccountOption> creditBox =
        new JComboBox<>();

    private final JTextField amountField = new HintTextField("0.00");

    private final JButton postButton =
        new RoundedButton("Confirm & Post");

    // Live "Entry preview" card (display only).
    private final JLabel totalDebitLabel = new JLabel("\u20B1 0.00");
    private final JLabel totalCreditLabel = new JLabel("\u20B1 0.00");
    private final JLabel differenceLabel = new JLabel("\u20B1 0.00");
    private final JLabel previewNote =
        new JLabel("Select accounts to preview your entry.");

    public NewTransactionPanel() {
        setLayout(new BorderLayout());
        setBackground(PAGE_BG);

        JLabel heading = new JLabel("NEW TRANSACTION");
        heading.setFont(new Font("Inter", Font.BOLD, 22)); //ako ge change ang font
        heading.setForeground(NAVY);

        JLabel subtitle = new JLabel(
            "Record a transaction and keep your books up to date."
        );
        subtitle.setFont(new Font("Inter", Font.PLAIN, 12));
        subtitle.setForeground(MUTED);

        descriptionField.setLineWrap(true);
        descriptionField.setWrapStyleWord(true);

        styleText(dateField);
        styleText(amountField);
        styleText(descriptionField);

        styleCombo(debitBox, "Select debit account");
        styleCombo(creditBox, "Select credit account");

        JScrollPane descriptionScroll = new JScrollPane(descriptionField);
        descriptionScroll.setBorder(null);
        descriptionScroll.setOpaque(false);
        descriptionScroll.getViewport().setOpaque(false);

        JLabel referenceText =
            new JLabel("Auto-generated after posting");
        referenceText.setFont(INPUT_FONT);
        referenceText.setForeground(PLACEHOLDER);

        JLabel pesoSign = new JLabel("\u20B1");
        pesoSign.setFont(INPUT_FONT);
        pesoSign.setForeground(MUTED);

        // ----- Left card: transaction details -----
        RoundedPanel card = new RoundedPanel(
            new GridBagLayout(), Color.WHITE, CARD_BORDER, 18
        );
        card.setBorder(
            BorderFactory.createEmptyBorder(22, 24, 22, 24)
        );

        JLabel cardTitle = new JLabel("Transaction details");
        cardTitle.setFont(new Font("Inter", Font.BOLD, 14));
        cardTitle.setForeground(NAVY);

        place(card, cardTitle, 0, 0, 2, new Insets(0, 0, 16, 0));

        place(card, fieldLabel("Date"), 0, 1, 1,
            new Insets(0, 0, 6, 7));
        place(card, fieldLabel("Reference number"), 1, 1, 1,
            new Insets(0, 7, 6, 0));

        place(card, inputBox(dateField, null, new CalendarIcon(), 36),
            0, 2, 1, new Insets(0, 0, 14, 7));
        place(card, inputBox(referenceText, null, null, 36),
            1, 2, 1, new Insets(0, 7, 14, 0));

        place(card, fieldLabel("Description"), 0, 3, 2,
            new Insets(0, 0, 6, 0));
        place(card, inputBox(descriptionScroll, null, null, 78),
            0, 4, 2, new Insets(0, 0, 14, 0));

        place(card, fieldLabel("Debit account"), 0, 5, 1,
            new Insets(0, 0, 6, 7));
        place(card, fieldLabel("Credit account"), 1, 5, 1,
            new Insets(0, 7, 6, 0));

        place(card, inputBox(debitBox, null, null, 36),
            0, 6, 1, new Insets(0, 0, 14, 7));
        place(card, inputBox(creditBox, null, null, 36),
            1, 6, 1, new Insets(0, 7, 14, 0));

        place(card, fieldLabel("Amount (PHP)"), 0, 7, 2,
            new Insets(0, 0, 6, 0));
        place(card, inputBox(amountField, pesoSign, null, 36),
            0, 8, 2, new Insets(0, 0, 18, 0));

        JButton clearButton = new JButton("Clear form");
        clearButton.setFont(new Font("Inter", Font.PLAIN, 12));
        clearButton.setForeground(NAVY);
        clearButton.setContentAreaFilled(false);
        clearButton.setBorderPainted(false);
        clearButton.setFocusPainted(false);
        clearButton.setBorder(
            BorderFactory.createEmptyBorder(8, 4, 8, 12)
        );
        clearButton.setCursor(
            Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)
        );

        JPanel buttons = new JPanel(
            new FlowLayout(FlowLayout.LEFT, 0, 0)
        );
        buttons.setOpaque(false);
        buttons.add(clearButton);
        buttons.add(postButton);

        place(card, buttons, 0, 9, 2, new Insets(0, 0, 0, 0));

        // ----- Right card: entry preview -----
        RoundedPanel preview = new RoundedPanel(
            new GridBagLayout(), NAVY, null, 18
        );
        preview.setBorder(
            BorderFactory.createEmptyBorder(20, 22, 20, 22)
        );

        JLabel previewTag = new JLabel("ENTRY PREVIEW");
        previewTag.setFont(new Font("Inter", Font.BOLD, 9));
        previewTag.setForeground(LIGHT_ON_NAVY);

        JLabel previewTitle = new JLabel("Every entry, balanced.");
        previewTitle.setFont(new Font("Inter", Font.BOLD, 16));
        previewTitle.setForeground(Color.WHITE);

        JLabel previewText = new JLabel(
            "Your debit and credit must be equal."
        );
        previewText.setFont(new Font("Inter", Font.PLAIN, 11));
        previewText.setForeground(LIGHT_ON_NAVY);

        previewNote.setFont(new Font("Inter", Font.PLAIN, 10));
        previewNote.setForeground(LIGHT_ON_NAVY);

        place(preview, previewTag, 0, 0, 1, new Insets(0, 0, 10, 0));
        place(preview, previewTitle, 0, 1, 1, new Insets(0, 0, 6, 0));
        place(preview, previewText, 0, 2, 1, new Insets(0, 0, 18, 0));
        place(preview, previewRow("Total debit", totalDebitLabel),
            0, 3, 1, new Insets(0, 0, 8, 0));
        place(preview, previewRow("Total credit", totalCreditLabel),
            0, 4, 1, new Insets(0, 0, 8, 0));
        place(preview, previewRow("Difference", differenceLabel),
            0, 5, 1, new Insets(0, 0, 0, 0));
        place(preview, previewNote, 0, 6, 1, new Insets(18, 0, 0, 0));

        // ----- Card + preview side by side -----
        JPanel split = new JPanel(new GridBagLayout());
        split.setOpaque(false);

        card.setPreferredSize(new Dimension(440, card.getPreferredSize().height));
        preview.setPreferredSize(new Dimension(220, preview.getPreferredSize().height));

        GridBagConstraints left = new GridBagConstraints();
        left.gridx = 0;
        left.weightx = 2;
        left.fill = GridBagConstraints.HORIZONTAL;
        left.anchor = GridBagConstraints.NORTH;
        split.add(card, left);

        GridBagConstraints right = new GridBagConstraints();
        right.gridx = 1;
        right.weightx = 1;
        right.fill = GridBagConstraints.HORIZONTAL;
        right.anchor = GridBagConstraints.NORTH;
        right.insets = new Insets(0, 16, 0, 0);
        split.add(preview, right);

        JLabel footer = new JLabel(
            "Posted entries appear in your General Journal and General Ledger."
        );
        footer.setFont(new Font("Inter", Font.PLAIN, 11));
        footer.setForeground(MUTED);

        // ----- Whole page -----
        ScrollPage page = new ScrollPage();
        page.setBackground(PAGE_BG);
        page.setLayout(new GridBagLayout());
        page.setBorder(
            BorderFactory.createEmptyBorder(28, 30, 20, 30)
        );

        place(page, heading, 0, 0, 1, new Insets(0, 0, 0, 0));
        place(page, subtitle, 0, 1, 1, new Insets(4, 0, 0, 0));
        place(page, split, 0, 2, 1, new Insets(20, 0, 0, 0));
        place(page, footer, 0, 3, 1, new Insets(22, 0, 0, 0));

        GridBagConstraints filler = new GridBagConstraints();
        filler.gridx = 0;
        filler.gridy = 4;
        filler.weighty = 1;
        filler.fill = GridBagConstraints.BOTH;
        JPanel fillerPanel = new JPanel();
        fillerPanel.setOpaque(false);
        page.add(fillerPanel, filler);

        JScrollPane scroll = new JScrollPane(page);
        scroll.setBorder(null);
        scroll.getViewport().setBackground(PAGE_BG);
        scroll.getVerticalScrollBar().setUnitIncrement(16);

        add(scroll, BorderLayout.CENTER);

        // ----- Same actions as before -----
        clearButton.addActionListener(event -> clearForm());
        postButton.addActionListener(event -> postTransaction());

        // ----- Keep the preview card up to date -----
        amountField.getDocument().addDocumentListener(
            new DocumentListener() {
                @Override
                public void insertUpdate(DocumentEvent event) {
                    updatePreview();
                }

                @Override
                public void removeUpdate(DocumentEvent event) {
                    updatePreview();
                }

                @Override
                public void changedUpdate(DocumentEvent event) {
                    updatePreview();
                }
            }
        );

        debitBox.addActionListener(event -> updatePreview());
        creditBox.addActionListener(event -> updatePreview());

        updatePreview();
    }

    // ---------------------------------------------------------------
    // Design helpers
    // ---------------------------------------------------------------

    private static void place(
        JPanel panel,
        JComponent component,
        int x,
        int y,
        int span,
        Insets insets
    ) {
        GridBagConstraints c = new GridBagConstraints();
        c.gridx = x;
        c.gridy = y;
        c.gridwidth = span;
        c.weightx = 1;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.anchor = GridBagConstraints.NORTHWEST;
        c.insets = insets;
        panel.add(component, c);
    }

    private static JLabel fieldLabel(String text) {
        JLabel label = new JLabel(text);
        label.setFont(new Font("Inter", Font.PLAIN, 11));
        label.setForeground(TEXT);
        return label;
    }

    private static void styleText(JTextComponent component) {
        component.setOpaque(false);
        component.setBorder(BorderFactory.createEmptyBorder());
        component.setFont(INPUT_FONT);
        component.setForeground(TEXT);
    }

    private static RoundedPanel inputBox(
        JComponent center,
        JComponent west,
        JComponent east,
        int height
    ) {
        RoundedPanel box = new RoundedPanel(
            new BorderLayout(8, 0), FIELD_BG, FIELD_BORDER, 10
        );
        box.setBorder(BorderFactory.createEmptyBorder(0, 10, 0, 10));
        box.setPreferredSize(new Dimension(120, height));

        if (west != null) {
            box.add(west, BorderLayout.WEST);
        }

        box.add(center, BorderLayout.CENTER);

        if (east != null) {
            box.add(east, BorderLayout.EAST);
        }

        return box;
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

    private static JPanel previewRow(String name, JLabel value) {
        JLabel label = new JLabel(name);
        label.setFont(new Font("Inter", Font.PLAIN, 11));
        label.setForeground(Color.WHITE);

        value.setFont(new Font("Inter", Font.BOLD, 11));
        value.setForeground(Color.WHITE);

        JPanel row = new JPanel(new BorderLayout());
        row.setOpaque(false);
        row.add(label, BorderLayout.WEST);
        row.add(value, BorderLayout.EAST);
        return row;
    }

    private static String money(BigDecimal value) {
        return "\u20B1 "
            + value.setScale(2, RoundingMode.HALF_UP).toPlainString();
    }

    // Display only: shows the totals the entry will have.
    private void updatePreview() {
        BigDecimal amount = null;

        try {
            BigDecimal parsed = new BigDecimal(amountField.getText().trim());

            if (parsed.signum() > 0) {
                amount = parsed;
            }
        } catch (NumberFormatException exception) {
            amount = null;
        }

        AccountOption debit = (AccountOption) debitBox.getSelectedItem();
        AccountOption credit = (AccountOption) creditBox.getSelectedItem();

        BigDecimal debitTotal =
            (amount != null && debit != null) ? amount : BigDecimal.ZERO;
        BigDecimal creditTotal =
            (amount != null && credit != null) ? amount : BigDecimal.ZERO;

        totalDebitLabel.setText(money(debitTotal));
        totalCreditLabel.setText(money(creditTotal));
        differenceLabel.setText(
            money(debitTotal.subtract(creditTotal).abs())
        );

        if (debit != null && credit != null && debit.id == credit.id) {
            previewNote.setText(
                "Debit and credit accounts must be different."
            );
        } else if (amount != null && debit != null && credit != null) {
            previewNote.setText("This entry is balanced and ready to post.");
        } else {
            previewNote.setText("Select accounts to preview your entry.");
        }
    }

    // ---------------------------------------------------------------
    // Logic (unchanged)
    // ---------------------------------------------------------------

    // Called whenever the user opens this screen.
    public void refreshAccounts() {
        List<AccountOption> accounts = new ArrayList<>();

        String sql = """
            SELECT id, code, name
            FROM accounts
            WHERE active = TRUE
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
                    result.getString("name")
                ));
            }
        } catch (SQLException exception) {
            postButton.setEnabled(false);
            showError("Could not load accounts.", exception);
            return;
        }

        debitBox.removeAllItems();
        creditBox.removeAllItems();

        for (AccountOption account : accounts) {
            debitBox.addItem(account);
            creditBox.addItem(account);
        }

        debitBox.setSelectedIndex(-1);
        creditBox.setSelectedIndex(-1);
        postButton.setEnabled(accounts.size() >= 2);
    }

    private void postTransaction() {
        LocalDate date;
        BigDecimal amount;
        long centavos;

        String description = descriptionField.getText().trim();

        AccountOption debit =
            (AccountOption) debitBox.getSelectedItem();

        AccountOption credit =
            (AccountOption) creditBox.getSelectedItem();

        if (description.isEmpty() || debit == null || credit == null) {
            showWarning(
                "Enter a description and select debit and credit accounts."
            );
            return;
        }

        if (debit.id == credit.id) {
            showWarning(
                "Debit and credit accounts must be different."
            );
            return;
        }

        try {
            date = LocalDate.parse(dateField.getText().trim());
        } catch (DateTimeParseException exception) {
            showWarning("Enter a valid date using YYYY-MM-DD.");
            return;
        }

        try {
            amount = new BigDecimal(amountField.getText().trim())
                .setScale(2, RoundingMode.UNNECESSARY);

            centavos = amount.movePointRight(2).longValueExact();

            if (centavos <= 0) {
                showWarning("Amount must be greater than zero.");
                return;
            }
        } catch (
            NumberFormatException | ArithmeticException exception
        ) {
            showWarning(
                "Enter a valid amount with at most two decimal places."
                + "\nExample: 1000.00"
            );
            return;
        }

        int answer = JOptionPane.showConfirmDialog(
            this,
            "Date: " + date
                + "\nDescription: " + description
                + "\nDebit: " + debit
                + "\nCredit: " + credit
                + "\nAmount: PHP " + amount.toPlainString()
                + "\n\nPost this transaction?",
            "Confirm Transaction",
            JOptionPane.YES_NO_OPTION
        );

        if (answer != JOptionPane.YES_OPTION) {
            return;
        }

        postButton.setEnabled(false);

        try {
            long transactionId = saveTransaction(
                date, description, debit.id, credit.id, centavos
            );

            clearForm();

            JOptionPane.showMessageDialog(
                this,
                "Transaction #" + transactionId
                    + " posted successfully."
            );
        } catch (SQLException exception) {
            showError("Could not post the transaction.", exception);
        } finally {
            postButton.setEnabled(debitBox.getItemCount() >= 2);
        }
    }

    private long saveTransaction(
        LocalDate date,
        String description,
        int debitId,
        int creditId,
        long centavos
    ) throws SQLException {

        try (Connection connection = Database.connect()) {
            connection.setAutoCommit(false);

            try {
                // Recheck that both accounts are still active.
                String checkSql = """
                    SELECT COUNT(*)
                    FROM accounts
                    WHERE active = TRUE AND id IN (?, ?)
                    """;

                try (
                    PreparedStatement check =
                        connection.prepareStatement(checkSql)
                ) {
                    check.setInt(1, debitId);
                    check.setInt(2, creditId);

                    try (ResultSet result = check.executeQuery()) {
                        if (!result.next() || result.getInt(1) != 2) {
                            throw new SQLException(
                                "Select two different active accounts."
                            );
                        }
                    }
                }

                // Insert the transaction and retrieve its PostgreSQL ID.
                String transactionSql = """
                    INSERT INTO transactions (
                        transaction_date, description
                    ) VALUES (?, ?)
                    RETURNING id
                    """;

                long transactionId;

                try (
                    PreparedStatement statement =
                        connection.prepareStatement(transactionSql)
                ) {
                    statement.setString(1, date.toString());
                    statement.setString(2, description);

                    try (ResultSet result = statement.executeQuery()) {
                        if (!result.next()) {
                            throw new SQLException(
                                "Could not retrieve the transaction ID."
                            );
                        }

                        transactionId = result.getLong("id");
                    }
                }

                // Insert the debit and credit journal lines.
                String lineSql = """
                    INSERT INTO journal_lines (
                        transaction_id, account_id,
                        debit_centavos, credit_centavos
                    ) VALUES (?, ?, ?, ?)
                    """;

                try (
                    PreparedStatement statement =
                        connection.prepareStatement(lineSql)
                ) {
                    // Debit entry
                    statement.setLong(1, transactionId);
                    statement.setInt(2, debitId);
                    statement.setLong(3, centavos);
                    statement.setLong(4, 0);
                    statement.executeUpdate();

                    // Credit entry
                    statement.setLong(1, transactionId);
                    statement.setInt(2, creditId);
                    statement.setLong(3, 0);
                    statement.setLong(4, centavos);
                    statement.executeUpdate();
                }

                connection.commit();
                return transactionId;

            } catch (SQLException exception) {
                try {
                    connection.rollback();
                } catch (SQLException rollbackException) {
                    exception.addSuppressed(rollbackException);
                }

                throw exception;
            }
        }
    }

    private void clearForm() {
        dateField.setText(LocalDate.now().toString());
        descriptionField.setText("");
        amountField.setText("");
        debitBox.setSelectedIndex(-1);
        creditBox.setSelectedIndex(-1);
    }

    private void showWarning(String message) {
        JOptionPane.showMessageDialog(
            this,
            message,
            "Check Transaction",
            JOptionPane.WARNING_MESSAGE
        );
    }

    private void showError(
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

    private static class AccountOption {
        private final int id;
        private final String code;
        private final String name;

        AccountOption(int id, String code, String name) {
            this.id = id;
            this.code = code;
            this.name = name;
        }

        @Override
        public String toString() {
            return code + " - " + name;
        }
    }

    // ---------------------------------------------------------------
    // Small custom-painted components
    // ---------------------------------------------------------------

    // Page that stretches to the window width inside the scroll pane.
    private static class ScrollPage extends JPanel implements Scrollable {

        @Override
        public Dimension getPreferredScrollableViewportSize() {
            return getPreferredSize();
        }

        @Override
        public int getScrollableUnitIncrement(
            Rectangle visible, int orientation, int direction
        ) {
            return 16;
        }

        @Override
        public int getScrollableBlockIncrement(
            Rectangle visible, int orientation, int direction
        ) {
            return visible.height;
        }

        @Override
        public boolean getScrollableTracksViewportWidth() {
            return true;
        }

        @Override
        public boolean getScrollableTracksViewportHeight() {
            return getParent() instanceof JViewport
                && getParent().getHeight() > getPreferredSize().height;
        }
    }

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

            if (borderColor != null) {
                g.setColor(borderColor);
                g.drawRoundRect(
                    0, 0, getWidth() - 1, getHeight() - 1, arc, arc
                );
            }

            g.dispose();
            super.paintComponent(graphics);
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
            setBorder(BorderFactory.createEmptyBorder(10, 24, 10, 24));
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

    private static class CalendarIcon extends JComponent {
        CalendarIcon() {
            setPreferredSize(new Dimension(16, 16));
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            Graphics2D g = (Graphics2D) graphics.create();
            g.setRenderingHint(
                RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_ON
            );

            g.setColor(MUTED);
            g.setStroke(new BasicStroke(1.4f));

            int top = (getHeight() - 16) / 2;

            g.drawRoundRect(1, top + 3, 13, 11, 3, 3);
            g.drawLine(1, top + 7, 14, top + 7);
            g.drawLine(5, top + 1, 5, top + 4);
            g.drawLine(10, top + 1, 10, top + 4);

            g.dispose();
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

    private static class HintTextArea extends JTextArea {
        private final String hint;

        HintTextArea(String hint, int rows, int columns) {
            super(rows, columns);
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

                Insets insets = getInsets();
                g.drawString(
                    hint,
                    insets.left,
                    insets.top + g.getFontMetrics().getAscent() + 8
                );
                g.dispose();
            }
        }
    }
}