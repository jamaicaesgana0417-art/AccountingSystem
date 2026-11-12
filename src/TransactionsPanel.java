import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableRowSorter;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.io.BufferedWriter;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.*;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;

public class TransactionsPanel extends JPanel {

    private static final Color PAGE_BG = new Color(244, 246, 255);
    private static final Color INPUT_BG = new Color(243, 246, 255);
    private static final Color BORDER = new Color(221, 228, 245);
    private static final Color TEXT = new Color(31, 50, 91);
    private static final Color MUTED = new Color(104, 120, 153);
    private static final Color BLUE = new Color(55, 91, 255);

    private final JTextField searchField = new JTextField(16);
    private final JTextField startDateField = new JTextField(10);
    private final JTextField endDateField = new JTextField(10);

    private final JComboBox<AccountOption> accountBox =
        new JComboBox<>();

    private final JLabel countLabel =
        new JLabel("No transactions loaded.");

    private final DefaultTableModel model =
        new DefaultTableModel(
            new String[] {
                "Date",
                "Description",
                "Debit Account",
                "Credit Account",
                "Amount (₱)"
            },
            0
        ) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }

            @Override
            public Class<?> getColumnClass(int column) {
                if (column == 4) {
                    return BigDecimal.class;
                }

                return String.class;
            }
        };

    private final JTable table = new JTable(model);
    private final CardLayout resultsCards = new CardLayout();
    private final JPanel resultsBody = new JPanel(resultsCards);
    private final JLabel emptyStateTitle = new JLabel("No transactions yet");
    private final JLabel emptyStateMessage = new JLabel(
        "Post your first transaction or adjust your filters to see results."
    );

    public TransactionsPanel() {
        setLayout(new BorderLayout(0, 16));
        setBackground(PAGE_BG);
        setBorder(
            BorderFactory.createEmptyBorder(30, 30, 24, 30)
        );

        JLabel heading = new JLabel("TRANSACTION");
        heading.setFont(new Font("Inter", Font.BOLD, 24));
        heading.setForeground(TEXT);

        JLabel subtitle = new JLabel(
            "Search, review, and export your posted transactions."
        );
        subtitle.setFont(new Font("Inter", Font.PLAIN, 11));
        subtitle.setForeground(MUTED);

        JPanel titleBlock = new JPanel();
        titleBlock.setOpaque(false);
        titleBlock.setLayout(new BoxLayout(titleBlock, BoxLayout.Y_AXIS));
        titleBlock.add(heading);
        titleBlock.add(Box.createVerticalStrut(7));
        titleBlock.add(subtitle);

        styleFilterInput(searchField);
        styleFilterInput(startDateField);
        styleFilterInput(endDateField);
        styleFilterInput(accountBox);

        JPanel filterFields = new JPanel(new GridLayout(1, 4, 12, 0));
        filterFields.setOpaque(false);
        filterFields.add(filterField("Search", searchField));
        filterFields.add(filterField(
            "From date (optional)",
            dateFilter(startDateField)
        ));
        filterFields.add(filterField(
            "To date (optional)",
            dateFilter(endDateField)
        ));
        filterFields.add(filterField("Debit or credit account", accountBox));

        JButton applyButton = new JButton("Apply Filters");
        JButton clearButton = new JButton("Clear Filters");
        JButton refreshButton = new JButton("Refresh");
        JButton exportButton = new JButton("Export CSV");
        styleActionButton(applyButton, true);
        styleActionButton(clearButton, true);
        styleActionButton(refreshButton, true);
        styleActionButton(exportButton, true);

        JPanel actions = new JPanel(new BorderLayout());
        actions.setOpaque(false);

        JPanel leftActions = new JPanel(
            new FlowLayout(FlowLayout.LEFT, 8, 0)
        );
        leftActions.setOpaque(false);
        leftActions.add(applyButton);
        leftActions.add(clearButton);
        leftActions.add(exportButton);

        actions.add(leftActions, BorderLayout.WEST);
        actions.add(refreshButton, BorderLayout.EAST);

        JPanel filters = roundedCard(new BorderLayout(0, 12), Color.WHITE);
        filters.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(BORDER, 1, true),
            BorderFactory.createEmptyBorder(14, 14, 12, 14)
        ));
        filters.add(filterFields, BorderLayout.CENTER);
        filters.add(actions, BorderLayout.SOUTH);

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

        TableRowSorter<DefaultTableModel> sorter =
            new TableRowSorter<>(model);

        // Keep updated results in their default SQL order.
        sorter.setSortsOnUpdates(true);
        table.setRowSorter(sorter);

        int[] widths = {155, 291, 205, 205, 160};

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

        table.getColumnModel()
            .getColumn(4)
            .setCellRenderer(moneyRenderer);

        add(top, BorderLayout.NORTH);
        JScrollPane tableScrollPane = new JScrollPane(table);
        tableScrollPane.setBorder(BorderFactory.createEmptyBorder());
        tableScrollPane.getViewport().setBackground(Color.WHITE);
        tableScrollPane.setColumnHeaderView(null);

        JPanel emptyState = createEmptyState();
        resultsBody.setBackground(Color.WHITE);
        resultsBody.add(tableScrollPane, "table");
        resultsBody.add(emptyState, "empty");
        resultsCards.show(resultsBody, "empty");

        JPanel resultsCard = roundedCard(new BorderLayout(), Color.WHITE);
        resultsCard.setBorder(BorderFactory.createLineBorder(BORDER, 1, true));
        resultsCard.add(table.getTableHeader(), BorderLayout.NORTH);
        resultsCard.add(resultsBody, BorderLayout.CENTER);
        add(resultsCard, BorderLayout.CENTER);
        resultsCard.setMinimumSize(new Dimension(0, 240));

        countLabel.setFont(new Font("Inter", Font.PLAIN, 10));
        countLabel.setForeground(MUTED);
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

    private JComponent dateFilter(JTextField dateField) {
        JPanel dateInput = new JPanel(new BorderLayout(6, 0));
        dateInput.setOpaque(false);

        JButton calendarButton = new JButton(new CalendarIcon());
        calendarButton.setToolTipText("Choose date");
        calendarButton.setFont(new Font("Inter", Font.PLAIN, 10));
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

    private JPanel roundedCard(LayoutManager layout, Color background) {
        JPanel card = new JPanel(layout);
        card.setBackground(background);
        card.setBorder(BorderFactory.createLineBorder(BORDER, 1, true));
        return card;
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

    private void showEmptyResults(String title, String message) {
        emptyStateTitle.setText(title);
        emptyStateMessage.setText(message);
        resultsCards.show(resultsBody, "empty");
    }

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
            WHERE active = 1
            AND owner_username = ?
            ORDER BY code
            """;

        try (
            Connection connection = Database.connect();
            PreparedStatement statement =
                connection.prepareStatement(sql)
        ) {
            statement.setString(1, Session.getCurrentUser());

            try (ResultSet result = statement.executeQuery()) {
            while (result.next()) {
                accounts.add(new AccountOption(
                    result.getInt("id"),
                    result.getString("name")
                ));
            }
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
                da.name AS debit_account,
                ca.name AS credit_account,
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
            WHERE t.owner_username = ?
            """);

        List<Object> parameters = new ArrayList<>();
        parameters.add(Session.getCurrentUser());

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

        if (rows.isEmpty()) {
            showEmptyResults(
                "No transactions yet",
                "Post your first transaction or adjust your filters to see results."
            );
        } else {
            resultsCards.show(resultsBody, "table");
        }

        countLabel.setForeground(MUTED);
        countLabel.setText(
            "Displayed transactions: " + rows.size()
                + " | Total amount: ₱ " + total.toPlainString()
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
        showEmptyResults("Results unavailable", message);
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
}