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

public class JournalPanel extends JPanel {

    private static final Color PAGE_BG = new Color(244, 246, 255);
    private static final Color INPUT_BG = new Color(243, 246, 255);
    private static final Color BORDER = new Color(221, 228, 245);
    private static final Color TEXT = new Color(31, 50, 91);
    private static final Color MUTED = new Color(104, 120, 153);
    private static final Color BLUE = new Color(55, 91, 255);

    private final JTextField searchField = new JTextField(16);
    private final JTextField startField = new JTextField(10);
    private final JTextField endField = new JTextField(10);

    private final JLabel linesLabel = new JLabel("Displayed lines: 0");
    private final JLabel debitTotalLabel = new JLabel("Total debit: PHP 0.00");
    private final JLabel creditTotalLabel = new JLabel("Total credit: PHP 0.00");

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

    private final JTable table = new JTable(model);
    private final CardLayout resultsCards = new CardLayout();
    private final JPanel resultsBody = new JPanel(resultsCards);
    private final JLabel emptyStateTitle =
        new JLabel("No matching journal entries");
    private final JLabel emptyStateMessage = new JLabel(
        "Posted transactions will appear here as debit and credit lines."
    );

    public JournalPanel() {
        setLayout(new BorderLayout(0, 16));
        setBackground(PAGE_BG);
        setBorder(
            BorderFactory.createEmptyBorder(30, 30, 24, 30)
        );

        JLabel heading = new JLabel("GENERAL JOURNAL");
        heading.setFont(new Font("Inter", Font.BOLD, 24));
        heading.setForeground(TEXT);

        JLabel subtitle = new JLabel(
            "Review posted entries in chronological order."
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
        styleFilterInput(startField);
        styleFilterInput(endField);

        JPanel fields = new JPanel(new GridLayout(1, 3, 12, 0));
        fields.setOpaque(false);
        fields.add(filterField("Search", searchField));
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

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        buttons.setOpaque(false);
        buttons.add(applyButton);
        buttons.add(clearButton);
        buttons.add(refreshButton);
        buttons.add(exportButton);

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
        table.setShowGrid(false);
        table.setIntercellSpacing(new Dimension(0, 0));
        table.getTableHeader().setReorderingAllowed(false);
        table.getTableHeader().setPreferredSize(new Dimension(0, 38));
        table.getTableHeader().setBackground(new Color(232, 238, 255));
        table.getTableHeader().setForeground(TEXT);
        table.getTableHeader().setFont(new Font("Inter", Font.BOLD, 9));
        table.getTableHeader().setBorder(BorderFactory.createEmptyBorder());

        // Keep chronological order and matching journal lines together.
        int[] widths = {100, 125, 245, 215, 145, 145};

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
            };

        moneyRenderer.setHorizontalAlignment(SwingConstants.RIGHT);

        table.getColumnModel().getColumn(4)
            .setCellRenderer(moneyRenderer);

        table.getColumnModel().getColumn(5)
            .setCellRenderer(moneyRenderer);

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

        JPanel totalsPanel = new JPanel(
            new FlowLayout(FlowLayout.LEFT, 28, 0)
        );
        totalsPanel.setOpaque(false);
        styleSummaryLabel(linesLabel, false);
        styleSummaryLabel(debitTotalLabel, true);
        styleSummaryLabel(creditTotalLabel, true);
        totalsPanel.add(linesLabel);
        totalsPanel.add(debitTotalLabel);
        totalsPanel.add(creditTotalLabel);
        add(totalsPanel, BorderLayout.SOUTH);

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

    private void styleSummaryLabel(JLabel label, boolean emphasized) {
        label.setFont(new Font(
            "Inter",
            emphasized ? Font.BOLD : Font.PLAIN,
            10
        ));
        label.setForeground(emphasized ? TEXT : MUTED);
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
            WHERE t.owner_username = ?
            """);

        List<String> parameters = new ArrayList<>();
        parameters.add(Session.getCurrentUser());

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
        linesLabel.setText("Displayed lines: " + rows.size());
        debitTotalLabel.setText(
            "Total debit: PHP " + totalDebit.toPlainString()
        );
        creditTotalLabel.setText(
            "Total credit: PHP " + totalCredit.toPlainString()
        );

        Color totalsColor = balanced ? TEXT : Color.RED;
        debitTotalLabel.setForeground(totalsColor);
        creditTotalLabel.setForeground(totalsColor);
        String balanceStatus = balanced
            ? "Displayed debits and credits are equal."
            : "Warning: Displayed debits and credits differ.";
        debitTotalLabel.setToolTipText(balanceStatus);
        creditTotalLabel.setToolTipText(balanceStatus);

        if (rows.isEmpty()) {
            emptyStateTitle.setText("No matching journal entries");
            emptyStateMessage.setText(
                "Posted transactions will appear here as debit and credit lines."
            );
            resultsCards.show(resultsBody, "empty");
        } else {
            resultsCards.show(resultsBody, "table");
        }
    }

    private LocalDate optionalDate(String text) {
        String value = text.trim();

        return value.isEmpty() ? null : LocalDate.parse(value);
    }

    private void invalidateResults(String message) {
        model.setRowCount(0);
        linesLabel.setText(message);
        linesLabel.setForeground(Color.RED);
        debitTotalLabel.setText("");
        creditTotalLabel.setText("");
        emptyStateTitle.setText("Results unavailable");
        emptyStateMessage.setText(message);
        resultsCards.show(resultsBody, "empty");
    }

    private void showWarning(String message) {
        JOptionPane.showMessageDialog(
            this,
            message,
            "Check Filters",
            JOptionPane.WARNING_MESSAGE
        );
    }
}