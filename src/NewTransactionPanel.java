import java.awt.*;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.*;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

public class NewTransactionPanel extends JPanel {

    private static final Color NAVY = new Color(25, 47, 96);
    private static final Color PAGE_BG = new Color(244, 246, 255);
    private static final Color INPUT_BG = new Color(243, 246, 255);
    private static final Color BORDER = new Color(221, 228, 245);
    private static final Color TEXT = new Color(31, 50, 91);
    private static final Color MUTED = new Color(104, 120, 153);
    private static final Color BLUE = new Color(55, 91, 255);

    private final JTextField dateField =
        new JTextField(LocalDate.now().toString());

    private final JButton calendarButton = new JButton(new CalendarIcon());

    private final JTextField descriptionField = new JTextField();

    private final JComboBox<AccountOption> debitBox =
        new JComboBox<>();

    private final JComboBox<AccountOption> creditBox =
        new JComboBox<>();

    private final JTextField amountField = new JTextField();

    private final JButton postButton =
        new JButton("Confirm & Post");

    private final JLabel debitTotalLabel =
        new JLabel("₱ 0.00");

    private final JLabel creditTotalLabel =
        new JLabel("₱ 0.00");

    private final JLabel differenceLabel =
        new JLabel("₱ 0.00");


    public NewTransactionPanel() {
        setLayout(new BorderLayout(0, 24));
        setBackground(PAGE_BG);
        setBorder(BorderFactory.createEmptyBorder(30, 30, 28, 30));

        styleInput(dateField);
        styleInput(descriptionField);
        styleInput(amountField);
        styleInput(debitBox);
        styleInput(creditBox);

        stylePostButton();

        JLabel heading = new JLabel("NEW TRANSACTION");
        heading.setFont(new Font("Inter", Font.BOLD, 24));
        heading.setForeground(NAVY);

        JLabel subtitle = new JLabel(
            "Record a transaction and keep your books up to date."
        );
        subtitle.setFont(new Font("Inter", Font.PLAIN, 12));
        subtitle.setForeground(MUTED);

        JPanel titleBlock = new JPanel();
        titleBlock.setOpaque(false);
        titleBlock.setLayout(
            new BoxLayout(titleBlock, BoxLayout.Y_AXIS)
        );
        titleBlock.add(heading);
        titleBlock.add(Box.createVerticalStrut(7));
        titleBlock.add(subtitle);

        JPanel content = new JPanel(new GridBagLayout());
        content.setOpaque(false);

        JPanel detailsCard = buildDetailsCard();
        JPanel previewCard = buildPreviewCard();

        GridBagConstraints cardConstraints = new GridBagConstraints();
        cardConstraints.gridy = 0;
        cardConstraints.anchor = GridBagConstraints.NORTH;
        cardConstraints.weighty = 1;

        cardConstraints.gridx = 0;
        cardConstraints.weightx = 1;
        cardConstraints.fill = GridBagConstraints.BOTH;
        cardConstraints.insets = new Insets(0, 0, 0, 16);
        content.add(detailsCard, cardConstraints);

        cardConstraints.gridx = 1;
        cardConstraints.weightx = 0;
        cardConstraints.weighty = 0;
        cardConstraints.fill = GridBagConstraints.NONE;
        cardConstraints.insets = new Insets(0, 0, 0, 0);
        content.add(previewCard, cardConstraints);

        JLabel footer = new JLabel(
            "Posted entries appear in your General Journal and General Ledger."
        );
        footer.setFont(new Font("Inter", Font.PLAIN, 10));
        footer.setForeground(MUTED);

        JPanel center = new JPanel(new BorderLayout(0, 16));
        center.setOpaque(false);
        center.add(content, BorderLayout.CENTER);
        center.add(footer, BorderLayout.SOUTH);

        add(titleBlock, BorderLayout.NORTH);
        add(center, BorderLayout.CENTER);

        postButton.addActionListener(event -> postTransaction());

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
    }


    private JPanel buildDetailsCard() {
        JPanel card = new JPanel(new BorderLayout(0, 8));
        card.setBackground(Color.WHITE);
        card.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(BORDER, 1, true),
            BorderFactory.createEmptyBorder(18, 18, 18, 18)
        ));

        JLabel cardTitle = new JLabel("TRANSACTION DETAILS");
        cardTitle.setFont(new Font("Inter", Font.BOLD, 12));
        cardTitle.setForeground(TEXT);

        JPanel fields = new JPanel(new GridBagLayout());
        fields.setOpaque(false);

        addFieldBlock(fields, 0, 0, 2, "Date", createDateInput());
        addFieldBlock(fields, 0, 1, 2, "Description", descriptionField);
        addFieldBlock(fields, 0, 2, 1, "Debit account", debitBox);
        addFieldBlock(fields, 1, 2, 1, "Credit account", creditBox);
        addFieldBlock(fields, 0, 3, 2, "Amount (₱)", amountField);

        JButton clearButton = new JButton("Clear form");
        clearButton.setFont(new Font("Inter", Font.PLAIN, 12));
        clearButton.setForeground(TEXT);
        clearButton.setBackground(Color.WHITE);
        clearButton.setFocusPainted(false);
        clearButton.setBorder(
            BorderFactory.createEmptyBorder(9, 16, 9, 16)
        );
        clearButton.setCursor(
            Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)
        );
        clearButton.addActionListener(event -> {
            clearForm();
            updatePreview();
        });

        JPanel buttonRow = new JPanel(
            new FlowLayout(FlowLayout.RIGHT, 10, 0)
        );
        buttonRow.setOpaque(false);
        buttonRow.add(clearButton);
        buttonRow.add(postButton);

        card.add(cardTitle, BorderLayout.NORTH);
        card.add(fields, BorderLayout.CENTER);
        card.add(buttonRow, BorderLayout.SOUTH);

        card.setPreferredSize(new Dimension(545, card.getPreferredSize().height));
        return card;
    }


    private JPanel buildPreviewCard() {
        JPanel card = new JPanel();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBackground(Color.WHITE);
        card.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(BORDER, 1, true),
            BorderFactory.createEmptyBorder(18, 18, 18, 18)
        ));

        JLabel eyebrow = new JLabel("ENTRY PREVIEW");
        eyebrow.setFont(new Font("Inter", Font.BOLD, 9));
        eyebrow.setForeground(MUTED);
        eyebrow.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel title = new JLabel("Every entry, balanced.");
        title.setFont(new Font("Inter", Font.BOLD, 17));
        title.setForeground(TEXT);
        title.setBorder(
            BorderFactory.createEmptyBorder(10, 0, 0, 0)
        );
        title.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel helper = new JLabel(
            "Your debit and credit must be equal."
        );
        helper.setFont(new Font("Inter", Font.PLAIN, 10));
        helper.setForeground(MUTED);
        helper.setBorder(
            BorderFactory.createEmptyBorder(10, 0, 0, 0)
        );
        helper.setAlignmentX(Component.LEFT_ALIGNMENT);

        card.add(eyebrow);
        card.add(title);
        card.add(helper);
        card.add(Box.createVerticalStrut(10));
        card.add(previewRow("Total debit", debitTotalLabel));
        card.add(Box.createVerticalStrut(6));
        card.add(previewRow("Total credit", creditTotalLabel));
        card.add(Box.createVerticalStrut(6));
        card.add(previewRow("Difference", differenceLabel));

        JLabel hint = new JLabel(
            "Select accounts to preview your entry."
        );
        hint.setFont(new Font("Inter", Font.PLAIN, 9));
        hint.setForeground(MUTED);
        hint.setBorder(
            BorderFactory.createEmptyBorder(10, 0, 0, 0)
        );
        hint.setAlignmentX(Component.LEFT_ALIGNMENT);
        card.add(hint);

        card.setPreferredSize(new Dimension(258, card.getPreferredSize().height));
        return card;
    }


    private JPanel previewRow(String labelText, JLabel amountLabel) {
        JPanel row = new JPanel(new BorderLayout());
        row.setOpaque(false);
        row.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel label = new JLabel(labelText);
        label.setFont(new Font("Inter", Font.PLAIN, 11));
        label.setForeground(MUTED);

        amountLabel.setFont(new Font("Inter", Font.PLAIN, 11));
        amountLabel.setForeground(NAVY);

        row.add(label, BorderLayout.WEST);
        row.add(amountLabel, BorderLayout.EAST);
        return row;
    }


    private JComponent createDateInput() {
        JPanel input = new JPanel(new BorderLayout(6, 0));
        input.setOpaque(false);

        calendarButton.setToolTipText("Choose date");
        calendarButton.setBackground(INPUT_BG);
        calendarButton.setFocusPainted(false);
        calendarButton.setCursor(
            Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)
        );
        calendarButton.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(BORDER, 1, true),
            BorderFactory.createEmptyBorder(6, 8, 6, 8)
        ));
        calendarButton.setPreferredSize(new Dimension(36, 36));
        calendarButton.addActionListener(event -> showDatePicker());

        input.add(dateField, BorderLayout.CENTER);
        input.add(calendarButton, BorderLayout.EAST);
        return input;
    }


    private void showDatePicker() {
        LocalDate selectedDate;
        try {
            selectedDate = LocalDate.parse(dateField.getText().trim());
        } catch (DateTimeParseException exception) {
            selectedDate = LocalDate.now();
        }

        LocalDate[] activeDate = {selectedDate};
        YearMonth[] displayedMonth = {YearMonth.from(selectedDate)};

        JPopupMenu popup = new JPopupMenu();
        JPanel calendar = new JPanel(new BorderLayout(0, 8));
        calendar.setBackground(Color.WHITE);
        calendar.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JLabel monthLabel = new JLabel("", SwingConstants.CENTER);
        monthLabel.setFont(new Font("Inter", Font.BOLD, 12));
        monthLabel.setForeground(TEXT);

        JButton previousButton = new JButton("<");
        JButton nextButton = new JButton("> ");
        styleCalendarNavigationButton(previousButton);
        styleCalendarNavigationButton(nextButton);

        JPanel monthHeader = new JPanel(new BorderLayout());
        monthHeader.setOpaque(false);
        monthHeader.add(previousButton, BorderLayout.WEST);
        monthHeader.add(monthLabel, BorderLayout.CENTER);
        monthHeader.add(nextButton, BorderLayout.EAST);

        JPanel daysGrid = new JPanel(new GridLayout(7, 7, 2, 2));
        daysGrid.setOpaque(false);
        calendar.add(monthHeader, BorderLayout.NORTH);
        calendar.add(daysGrid, BorderLayout.CENTER);

        Runnable[] refreshCalendar = new Runnable[1];
        refreshCalendar[0] = () -> {
            monthLabel.setText(displayedMonth[0].format(
                DateTimeFormatter.ofPattern("MMMM yyyy")
            ));
            daysGrid.removeAll();

            String[] weekdays = {"S", "M", "T", "W", "T", "F", "S"};
            for (String weekday : weekdays) {
                JLabel weekdayLabel = new JLabel(
                    weekday,
                    SwingConstants.CENTER
                );
                weekdayLabel.setFont(new Font("Inter", Font.BOLD, 9));
                weekdayLabel.setForeground(MUTED);
                daysGrid.add(weekdayLabel);
            }

            int firstDay = displayedMonth[0].atDay(1)
                .getDayOfWeek().getValue() % 7;
            int daysInMonth = displayedMonth[0].lengthOfMonth();

            for (int cell = 0; cell < 42; cell++) {
                int day = cell - firstDay + 1;
                if (day < 1 || day > daysInMonth) {
                    daysGrid.add(new JLabel(""));
                    continue;
                }

                LocalDate date = displayedMonth[0].atDay(day);
                JButton dayButton = new JButton(Integer.toString(day));
                dayButton.setFont(new Font("Inter", Font.PLAIN, 10));
                dayButton.setForeground(TEXT);
                dayButton.setBackground(
                    date.equals(activeDate[0]) ? INPUT_BG : Color.WHITE
                );
                dayButton.setFocusPainted(false);
                dayButton.setBorder(BorderFactory.createLineBorder(
                    date.equals(activeDate[0]) ? BLUE : BORDER
                ));
                dayButton.setCursor(
                    Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)
                );
                dayButton.addActionListener(event -> {
                    dateField.setText(date.toString());
                    popup.setVisible(false);
                });
                daysGrid.add(dayButton);
            }

            daysGrid.revalidate();
            daysGrid.repaint();
        };

        previousButton.addActionListener(event -> {
            displayedMonth[0] = displayedMonth[0].minusMonths(1);
            refreshCalendar[0].run();
        });
        nextButton.addActionListener(event -> {
            displayedMonth[0] = displayedMonth[0].plusMonths(1);
            refreshCalendar[0].run();
        });

        refreshCalendar[0].run();
        popup.add(calendar);
        popup.setPopupSize(280, 280);
        popup.show(calendarButton, 0, calendarButton.getHeight());
    }


    private void styleCalendarNavigationButton(JButton button) {
        button.setFont(new Font("Inter", Font.PLAIN, 12));
        button.setForeground(TEXT);
        button.setBackground(Color.WHITE);
        button.setFocusPainted(false);
        button.setBorder(BorderFactory.createEmptyBorder(4, 8, 4, 8));
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
    }


    private void addFieldBlock(
        JPanel panel,
        int column,
        int row,
        int columnSpan,
        String labelText,
        JComponent input
    ) {
        JPanel block = new JPanel(new BorderLayout(0, 7));
        block.setOpaque(false);

        JLabel label = new JLabel(labelText);
        label.setFont(new Font("Inter", Font.PLAIN, 10));
        label.setForeground(TEXT);

        block.add(label, BorderLayout.NORTH);
        block.add(input, BorderLayout.CENTER);

        GridBagConstraints constraints = new GridBagConstraints();
        constraints.gridx = column;
        constraints.gridy = row;
        constraints.gridwidth = columnSpan;
        constraints.weightx = columnSpan == 2 ? 1 : 0.5;
        constraints.weighty = 1;
        constraints.fill = GridBagConstraints.HORIZONTAL;
        constraints.anchor = GridBagConstraints.NORTH;
        constraints.insets = new Insets(
            0,
            column == 0 ? 0 : 8,
            6,
            columnSpan == 2 ? 0 : (column == 0 ? 8 : 0)
        );

        panel.add(block, constraints);
    }


    private void styleInput(JComponent input) {
        input.setFont(new Font("Inter", Font.PLAIN, 11));
        input.setForeground(TEXT);
        input.setBackground(INPUT_BG);
        input.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(BORDER, 1, true),
            BorderFactory.createEmptyBorder(6, 10, 6, 10)
        ));
        input.setPreferredSize(new Dimension(200, 36));
    }


    private void stylePostButton() {
        postButton.setFont(new Font("Inter", Font.BOLD, 13));
        postButton.setForeground(Color.WHITE);
        postButton.setBackground(BLUE);
        postButton.setFocusPainted(false);
        postButton.setBorder(
            BorderFactory.createEmptyBorder(11, 20, 11, 20)
        );
        postButton.setCursor(
            Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)
        );
    }


    private void updatePreview() {
        String text = amountField.getText().trim();

        try {
            BigDecimal amount = new BigDecimal(text);

            if (amount.signum() < 0) {
                amount = BigDecimal.ZERO;
            }

            String formatted = "₱ " + amount.setScale(
                2,
                RoundingMode.HALF_UP
            );

            debitTotalLabel.setText(formatted);
            creditTotalLabel.setText(formatted);
            differenceLabel.setText("₱ 0.00");

        } catch (NumberFormatException exception) {
            debitTotalLabel.setText("₱ 0.00");
            creditTotalLabel.setText("₱ 0.00");
            differenceLabel.setText("₱ 0.00");
        }
    }


    // Call this whenever the user opens this screen.
    public void refreshAccounts() {
        List<AccountOption> accounts = new ArrayList<>();

        List<String> allowedNames = new ArrayList<>();
        for (ChartOfAccounts.Entry entry : ChartOfAccounts.ACCOUNTS) {
            allowedNames.add(entry.name);
        }

        String username = Session.getCurrentUser();

        if (username == null) {
            debitBox.removeAllItems();
            creditBox.removeAllItems();
            postButton.setEnabled(false);
            showWarning("Please log in before creating a transaction.");
            return;
        }

        String sql = """
            SELECT id, code, name, type
            FROM accounts
            WHERE active = 1
              AND owner_username = ?
            ORDER BY Code
            """;

        try (
            Connection connection = Database.connect();
            PreparedStatement statement =
                connection.prepareStatement(sql)
        ) {
            statement.setString(1, username);

            try (ResultSet result = statement.executeQuery()) {
                while (result.next()) {
                    String name = result.getString("name");

                    if (allowedNames.contains(name)) {
                        String type = result.getString("type");

                        accounts.add(new AccountOption(
                            result.getInt("id"),
                            result.getString("code"),
                            name + " [" + type + "]"
                        ));
                    }
                }
            }

        } catch (SQLException exception) {
            debitBox.removeAllItems();
            creditBox.removeAllItems();
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
                + "\nAmount: ₱ " + amount.toPlainString()
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
                date,
                description,
                debit.id,
                credit.id,
                centavos
            );

            clearForm();
            updatePreview();

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
                String checkSql = """
                    SELECT COUNT(*)
                    FROM accounts
                    WHERE active = 1
                      AND id IN (?, ?)
                      AND owner_username = ?
                    """;

                try (
                    PreparedStatement check =
                        connection.prepareStatement(checkSql)
                ) {
                    check.setInt(1, debitId);
                    check.setInt(2, creditId);
                    check.setString(3, Session.getCurrentUser());

                    try (ResultSet result = check.executeQuery()) {
                        if (!result.next() || result.getInt(1) != 2) {
                            throw new SQLException(
                                "Select two different active accounts."
                            );
                        }
                    }
                }

                String transactionSql = """
                    INSERT INTO transactions (
                        transaction_date,
                        description,
                        owner_username
                    ) VALUES (?, ?, ?)
                    RETURNING id
                    """;

                long transactionId;

                try (
                    PreparedStatement statement =
                        connection.prepareStatement(transactionSql)
                ) {
                    statement.setString(1, date.toString());
                    statement.setString(2, description);
                    statement.setString(
                        3,
                        Session.getCurrentUser()
                    );

                    try (ResultSet result = statement.executeQuery()) {
                        if (!result.next()) {
                            throw new SQLException(
                                "Could not retrieve the transaction ID."
                            );
                        }

                        transactionId = result.getLong("id");
                    }
                }

                String lineSql = """
                    INSERT INTO journal_lines (
                        transaction_id,
                        account_id,
                        debit_centavos,
                        credit_centavos
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
        private final String name;

        AccountOption(int id, String code, String name) {
            this.id = id;
            this.name = name;
        }

        @Override
        public String toString() {
            return name;
        }
    }


    private static class CalendarIcon implements Icon {
        @Override
        public int getIconWidth() {
            return 20;
        }

        @Override
        public int getIconHeight() {
            return 20;
        }

        @Override
        public void paintIcon(
            Component component,
            Graphics graphics,
            int x,
            int y
        ) {
            Graphics2D graphics2D = (Graphics2D) graphics.create();
            graphics2D.setRenderingHint(
                RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_ON
            );
            graphics2D.setColor(TEXT);
            graphics2D.drawRoundRect(x + 2, y + 4, 16, 14, 2, 2);
            graphics2D.drawLine(x + 2, y + 8, x + 18, y + 8);
            graphics2D.drawLine(x + 6, y + 2, x + 6, y + 6);
            graphics2D.drawLine(x + 14, y + 2, x + 14, y + 6);
            graphics2D.dispose();
        }
    }
}