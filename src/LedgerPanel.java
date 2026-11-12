import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.*;
import java.math.BigDecimal;
import java.sql.*;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;

public class LedgerPanel extends JPanel {

    private static final Color PAGE_BG = new Color(244, 246, 255);
    private static final Color INPUT_BG = new Color(243, 246, 255);
    private static final Color BORDER = new Color(221, 228, 245);
    private static final Color TEXT = new Color(31, 50, 91);
    private static final Color MUTED = new Color(104, 120, 153);
    private static final Color BLUE = new Color(55, 91, 255);

    private final JComboBox<AccountOption> accountBox =
        new JComboBox<>();

    private final JTextField startField = new JTextField(10);
    private final JTextField endField = new JTextField(10);

    private final JLabel summaryLabel =
        new JLabel("Running balances update automatically from posted journal entries.");

    private boolean loadingAccounts = false;

    private final DefaultTableModel model =
        new DefaultTableModel(
            new String[] {
                "Date",
                "Description",
                "Debit Account",
                "Credit Account",
                "Amount",
                "Balance",
            },
            0
        ) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }

            @Override
            public Class<?> getColumnClass(int column) {
                if (column >= 4) {
                    return BigDecimal.class;
                }

                return String.class;
            }
        };

    private final JTable table = new JTable(model);
    private final CardLayout resultsCards = new CardLayout();
    private final JPanel resultsBody = new JPanel(resultsCards);
    private final JLabel emptyStateTitle =
        new JLabel("Choose an account to view its ledger");
    private final JLabel emptyStateMessage = new JLabel(
        "No accounts available. Add an account in Chart of Accounts to begin."
    );

    public LedgerPanel() {
        setLayout(new BorderLayout(0, 16));
        setBackground(PAGE_BG);
        setBorder(
            BorderFactory.createEmptyBorder(30, 30, 24, 30)
        );

        JLabel heading = new JLabel("GENERAL LEDGER");
        heading.setFont(new Font("Inter", Font.BOLD, 24));
        heading.setForeground(TEXT);

        JLabel subtitle = new JLabel(
            "Track each account's activity and running balance."
        );
        subtitle.setFont(new Font("Inter", Font.PLAIN, 11));
        subtitle.setForeground(MUTED);

        JPanel titleBlock = new JPanel();
        titleBlock.setOpaque(false);
        titleBlock.setLayout(new BoxLayout(titleBlock, BoxLayout.Y_AXIS));
        titleBlock.add(heading);
        titleBlock.add(Box.createVerticalStrut(7));
        titleBlock.add(subtitle);

        styleFilterInput(accountBox);
        styleFilterInput(startField);
        styleFilterInput(endField);

        JPanel fields = new JPanel(new GridLayout(1, 3, 12, 0));
        fields.setOpaque(false);
        fields.add(filterField("Account", accountBox));
        fields.add(filterField("From date (optional)", dateFilter(startField)));
        fields.add(filterField("To date (optional)", dateFilter(endField)));

        JButton applyButton = new JButton("Apply Filters");
        JButton clearButton = new JButton("Clear Filters");
        JButton refreshButton = new JButton("Refresh");
        JButton exportButton = new JButton("Export CSV");
        styleActionButton(applyButton, true);
        styleActionButton(clearButton, true);
        styleActionButton(refreshButton, true);
        styleActionButton(exportButton, true);

        JPanel filterButtons = new JPanel(
            new FlowLayout(FlowLayout.LEFT, 8, 0)
        );
        filterButtons.setOpaque(false);
        filterButtons.add(applyButton);
        filterButtons.add(clearButton);
        filterButtons.add(exportButton);

        JPanel buttons = new JPanel(new BorderLayout());
        buttons.setOpaque(false);
        buttons.add(filterButtons, BorderLayout.WEST);
        buttons.add(refreshButton, BorderLayout.EAST);

        JPanel filters = new JPanel(new BorderLayout(0, 12));
        filters.setBackground(Color.WHITE);
        filters.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(BORDER, 1, true),
            BorderFactory.createEmptyBorder(14, 14, 12, 14)
        ));
        filters.add(fields, BorderLayout.CENTER);
        filters.add(buttons, BorderLayout.SOUTH);

        JPanel top = new JPanel(new BorderLayout(0, 26));
        top.setOpaque(false);
        top.add(titleBlock, BorderLayout.NORTH);
        top.add(filters, BorderLayout.CENTER);

        table.setRowHeight(34);
        table.setFillsViewportHeight(true);
        table.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
        table.setFont(new Font("Inter", Font.PLAIN, 10));
        table.setForeground(TEXT);
        table.setBackground(Color.WHITE);
        table.setSelectionBackground(new Color(232, 238, 255));
        table.setSelectionForeground(TEXT);
        table.setShowHorizontalLines(true);
        table.setShowVerticalLines(true);
        table.setGridColor(BORDER);
        table.setIntercellSpacing(new Dimension(1, 1));
        table.getTableHeader().setReorderingAllowed(false);
        table.getTableHeader().setPreferredSize(new Dimension(0, 38));
        table.getTableHeader().setBackground(new Color(232, 238, 255));
        table.getTableHeader().setForeground(TEXT);
        table.getTableHeader().setFont(new Font("Inter", Font.BOLD, 9));
        table.getTableHeader().setBorder(BorderFactory.createEmptyBorder());

        // Keep chronological order so running balances remain meaningful.
        int[] widths = {110, 240, 180, 180, 130, 130};

        for (int i = 0; i < widths.length; i++) {
            table.getColumnModel()
                .getColumn(i)
                .setPreferredWidth(widths[i]);
        }

        DefaultTableCellRenderer moneyRenderer =
            new DefaultTableCellRenderer() {
                @Override
                protected void setValue(Object value) {
                    if (value instanceof BigDecimal) {
                        setText(
                            "₱ " + ((BigDecimal) value)
                                .setScale(2)
                                .toPlainString()
                        );
                    } else {
                        super.setValue(value);
                    }
                }
            };

        moneyRenderer.setHorizontalAlignment(SwingConstants.RIGHT);

        for (int column = 4; column <= 5; column++) {
            table.getColumnModel()
                .getColumn(column)
                .setCellRenderer(moneyRenderer);
        }

        add(top, BorderLayout.NORTH);

        JScrollPane tableScrollPane = new JScrollPane(table);
        tableScrollPane.setBorder(BorderFactory.createEmptyBorder());
        tableScrollPane.getViewport().setBackground(Color.WHITE);
        tableScrollPane.setColumnHeaderView(null);

        resultsBody.setBackground(Color.WHITE);
        resultsBody.add(tableScrollPane, "table");
        resultsBody.add(createEmptyState(), "empty");
        resultsCards.show(resultsBody, "empty");

        JPanel resultsCard = new JPanel(new BorderLayout());
        resultsCard.setBackground(Color.WHITE);
        resultsCard.setBorder(BorderFactory.createLineBorder(BORDER, 1, true));
        resultsCard.add(table.getTableHeader(), BorderLayout.NORTH);
        resultsCard.add(resultsBody, BorderLayout.CENTER);
        resultsCard.setMinimumSize(new Dimension(0, 240));
        add(resultsCard, BorderLayout.CENTER);

        summaryLabel.setFont(new Font("Inter", Font.PLAIN, 10));
        summaryLabel.setForeground(MUTED);
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

    private JPanel filterField(String labelText, JComponent input) {
        JPanel field = new JPanel(new BorderLayout(0, 6));
        field.setOpaque(false);

        JLabel label = new JLabel(labelText);
        label.setFont(new Font("Inter", Font.PLAIN, 9));
        label.setForeground(TEXT);
        field.add(label, BorderLayout.NORTH);
        field.add(input, BorderLayout.CENTER);
        return field;
    }

    private void styleFilterInput(JComponent input) {
        input.setFont(new Font("Inter", Font.PLAIN, 10));
        input.setForeground(TEXT);
        input.setBackground(INPUT_BG);
        input.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(BORDER, 1, true),
            BorderFactory.createEmptyBorder(0, 10, 0, 10)
        ));
        input.setPreferredSize(new Dimension(200, 36));
    }

    private void styleActionButton(JButton button, boolean primary) {
        button.setFont(new Font("Inter", Font.PLAIN, 11));
        button.setForeground(primary ? Color.WHITE : TEXT);
        button.setBackground(primary ? BLUE : Color.WHITE);
        button.setFocusPainted(false);
        button.setBorder(BorderFactory.createEmptyBorder(8, 12, 8, 12));
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        if (!primary) {
            button.setContentAreaFilled(false);
        }
    }

    private JComponent dateFilter(JTextField dateField) {
        JPanel dateInput = new JPanel(new BorderLayout(6, 0));
        dateInput.setOpaque(false);

        JButton calendarButton = new JButton(new CalendarIcon());
        calendarButton.setToolTipText("Choose date");
        calendarButton.setForeground(TEXT);
        calendarButton.setBackground(INPUT_BG);
        calendarButton.setFocusPainted(false);
        calendarButton.setCursor(
            Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)
        );
        calendarButton.setPreferredSize(new Dimension(36, 36));

        Border buttonBorder = BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(BORDER, 1, true),
            BorderFactory.createEmptyBorder(5, 7, 5, 7)
        );
        calendarButton.setBorder(buttonBorder);
        addCalendarHover(calendarButton, INPUT_BG, buttonBorder);
        calendarButton.addActionListener(
            event -> showDatePicker(dateField, calendarButton)
        );

        dateInput.add(dateField, BorderLayout.CENTER);
        dateInput.add(calendarButton, BorderLayout.EAST);
        return dateInput;
    }

    private void showDatePicker(JTextField dateField, JButton calendarButton) {
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
        JButton nextButton = new JButton(">");
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
                Color dayBackground = date.equals(activeDate[0])
                    ? INPUT_BG : Color.WHITE;
                dayButton.setBackground(dayBackground);
                dayButton.setFocusPainted(false);
                dayButton.setCursor(
                    Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)
                );

                Border dayBorder = BorderFactory.createLineBorder(
                    date.equals(activeDate[0]) ? BLUE : BORDER
                );
                dayButton.setBorder(dayBorder);
                addCalendarHover(dayButton, dayBackground, dayBorder);
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
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        Border buttonBorder = BorderFactory.createEmptyBorder(4, 8, 4, 8);
        button.setBorder(buttonBorder);
        addCalendarHover(button, Color.WHITE, buttonBorder);
    }

    private void addCalendarHover(
        JButton button,
        Color normalBackground,
        Border normalBorder
    ) {
        Color hoverBackground = new Color(232, 238, 255);
        button.setRolloverEnabled(true);
        button.setOpaque(true);
        button.setContentAreaFilled(true);
        button.getModel().addChangeListener(event -> {
            boolean hovered = button.getModel().isRollover();
            button.setBackground(hovered ? hoverBackground : normalBackground);
            button.setBorder(hovered
                ? BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(BLUE, 1, true),
                    BorderFactory.createEmptyBorder(4, 6, 4, 6)
                )
                : normalBorder
            );
        });
    }

    private JPanel createEmptyState() {
        JPanel emptyState = new JPanel();
        emptyState.setBackground(Color.WHITE);
        emptyState.setLayout(new BoxLayout(emptyState, BoxLayout.Y_AXIS));

        JLabel icon = new JLabel("\u25A4");
        icon.setFont(new Font("Inter", Font.PLAIN, 24));
        icon.setForeground(BLUE);
        icon.setAlignmentX(Component.CENTER_ALIGNMENT);

        emptyStateTitle.setFont(new Font("Inter", Font.BOLD, 14));
        emptyStateTitle.setForeground(TEXT);
        emptyStateTitle.setAlignmentX(Component.CENTER_ALIGNMENT);
        emptyStateMessage.setFont(new Font("Inter", Font.PLAIN, 9));
        emptyStateMessage.setForeground(MUTED);
        emptyStateMessage.setAlignmentX(Component.CENTER_ALIGNMENT);

        emptyState.add(Box.createVerticalGlue());
        emptyState.add(icon);
        emptyState.add(Box.createVerticalStrut(12));
        emptyState.add(emptyStateTitle);
        emptyState.add(Box.createVerticalStrut(8));
        emptyState.add(emptyStateMessage);
        emptyState.add(Box.createVerticalGlue());
        return emptyState;
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

    // App.java already calls this when opening General Ledger.
    public void refreshAccounts() {
        AccountOption previous =
            (AccountOption) accountBox.getSelectedItem();

        List<AccountOption> accounts = new ArrayList<>();

        String sql = """
            SELECT id, code, name, type
            FROM accounts
            WHERE owner_username = ?
            ORDER BY code
            """;

 try (
    Connection connection = Database.connect();
    PreparedStatement statement =
        connection.prepareStatement(sql)
) {
    // Load only accounts belonging to the logged-in user.
    statement.setString(1, Session.getCurrentUser());

    try (ResultSet result = statement.executeQuery()) {
        while (result.next()) {
            accounts.add(new AccountOption(
                result.getInt("id"),
                result.getString("code"),
                result.getString("name"),
                result.getString("type")
            ));
        }
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
            model.setRowCount(0);
            showEmptyResults(
                "Choose an account to view its ledger",
                "No accounts available. Add an account in Chart of Accounts to begin."
            );
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
                j.account_id,
                a.name AS account_name,
                j.debit_centavos,
                j.credit_centavos
            FROM transactions t
            JOIN journal_lines j ON j.transaction_id = t.id
            JOIN accounts a
                ON a.id = j.account_id
                AND a.owner_username = t.owner_username
            WHERE t.owner_username = ?
                AND EXISTS (
                    SELECT 1
                    FROM journal_lines selected_line
                    WHERE selected_line.transaction_id = t.id
                        AND selected_line.account_id = ?
                )
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
            statement.setString(1, Session.getCurrentUser());
            statement.setInt(2, account.id);

            if (end != null) {
                statement.setString(3, end.toString());
            }

            try (ResultSet result = statement.executeQuery()) {
                LedgerTransaction transaction = null;

                while (result.next()) {
                    long transactionId = result.getLong("transaction_id");
                    if (transaction == null
                        || transaction.id != transactionId) {
                        if (transaction != null) {
                            runningBalance = appendLedgerTransaction(
                                transaction,
                                start,
                                runningBalance,
                                entries
                            );
                            if (start == null
                                || !transaction.date.isBefore(start)) {
                                periodDebit = periodDebit.add(
                                    transaction.selectedDebit
                                );
                                periodCredit = periodCredit.add(
                                    transaction.selectedCredit
                                );
                            } else {
                                opening = runningBalance;
                            }
                        }
                        transaction = new LedgerTransaction(
                            transactionId,
                            LocalDate.parse(
                                result.getString("transaction_date")
                            ),
                            result.getString("description")
                        );
                    }

                    String accountName = result.getString("account_name");
                    BigDecimal debit = BigDecimal.valueOf(
                        result.getLong("debit_centavos"), 2
                    );

                    BigDecimal credit = BigDecimal.valueOf(
                        result.getLong("credit_centavos"), 2
                    );

                    if (debit.signum() > 0
                        && !transaction.debitAccounts.contains(accountName)) {
                        transaction.debitAccounts.add(accountName);
                    }
                    if (credit.signum() > 0
                        && !transaction.creditAccounts.contains(accountName)) {
                        transaction.creditAccounts.add(accountName);
                    }
                    if (result.getInt("account_id") == account.id) {
                        transaction.selectedDebit =
                            transaction.selectedDebit.add(debit);
                        transaction.selectedCredit =
                            transaction.selectedCredit.add(credit);
                    }
                }

                if (transaction != null) {
                    runningBalance = appendLedgerTransaction(
                        transaction,
                        start,
                        runningBalance,
                        entries
                    );
                    if (start == null || !transaction.date.isBefore(start)) {
                        periodDebit = periodDebit.add(
                            transaction.selectedDebit
                        );
                        periodCredit = periodCredit.add(
                            transaction.selectedCredit
                        );
                    } else {
                        opening = runningBalance;
                    }
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

        for (Object[] row : entries) {
            model.addRow(row);
        }

        resultsCards.show(resultsBody, "table");

        String normalSide =
            ChartOfAccounts.isDebitNormal(account.type)
                ? "Debit"
                : "Credit";

        summaryLabel.setForeground(MUTED);
        summaryLabel.setText(
            "Running balances update automatically from posted journal entries."
        );
        summaryLabel.setToolTipText(
            "Account: " + account.name
                + " | Type: " + account.type
                + " | Normal side: " + normalSide
                + " | Period: "
                + (start == null ? "Beginning" : start.toString())
                + " to "
                + (end == null ? "Latest entry" : end.toString())
                + " | Opening: ₱ " + opening.abs().toPlainString()
                + " " + side(opening)
                + " | Period debit: ₱ " + periodDebit.toPlainString()
                + " | Period credit: ₱ " + periodCredit.toPlainString()
                + " | Closing: ₱ " + runningBalance.abs().toPlainString()
                + " " + side(runningBalance)
                + " | Entries: " + entries.size()
        );
    }

    private BigDecimal appendLedgerTransaction(
        LedgerTransaction transaction,
        LocalDate start,
        BigDecimal runningBalance,
        List<Object[]> entries
    ) {
        BigDecimal movement = transaction.selectedDebit
            .subtract(transaction.selectedCredit);
        runningBalance = runningBalance.add(movement);

        if (start == null || !transaction.date.isBefore(start)) {
            entries.add(new Object[] {
                transaction.date.toString(),
                transaction.description,
                String.join(", ", transaction.debitAccounts),
                String.join(", ", transaction.creditAccounts),
                movement.abs(),
                runningBalance
            });
        }

        return runningBalance;
    }

    private static class LedgerTransaction {
        private final long id;
        private final LocalDate date;
        private final String description;
        private final List<String> debitAccounts = new ArrayList<>();
        private final List<String> creditAccounts = new ArrayList<>();
        private BigDecimal selectedDebit = zero();
        private BigDecimal selectedCredit = zero();

        LedgerTransaction(long id, LocalDate date, String description) {
            this.id = id;
            this.date = date;
            this.description = description;
        }
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
        summaryLabel.setText(
            "Running balances update automatically from posted journal entries."
        );
        summaryLabel.setToolTipText(message);
        summaryLabel.setForeground(MUTED);
        showEmptyResults("Results unavailable", message);
    }

    private void showEmptyResults(String title, String message) {
        emptyStateTitle.setText(title);
        emptyStateMessage.setText(message);
        resultsCards.show(resultsBody, "empty");
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
            return name;
        }

        @Override
        public String toString() {
            return label() + " [" + type + "]";
        }
    }
}