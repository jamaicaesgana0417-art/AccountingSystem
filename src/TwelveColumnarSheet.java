import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.io.BufferedWriter;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.*;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class TwelveColumnarSheet extends JPanel {
    private static final Color PAGE_BG = new Color(239, 243, 251);
    private static final Color CARD_BG = new Color(255, 255, 255);
    private static final Color BORDER = new Color(216, 223, 238);
    private static final Color TEXT = new Color(20, 47, 82);
    private static final Color MUTED = new Color(102, 118, 142);
    private static final Color BLUE = new Color(57, 103, 255);
    private static final Color NAVY = new Color(22, 52, 91);
    private static final Color HEADER_BG = new Color(18, 42, 74);
    private static final Color HEADER_SUB_BG = new Color(230, 236, 247);
    private static final Color INCOME_SUB_BG = new Color(219, 246, 236);
    private static final Color EQUITY_SUB_BG = new Color(255, 240, 218);
    private static final Color BALANCE_SUB_BG = new Color(226, 238, 250);
    private static final int ACCOUNT_COLUMN_WIDTH = 166;
    private static final int AMOUNT_COLUMN_WIDTH = 60;
    private static final int MIN_AMOUNT_COLUMN_WIDTH = 48;
    private static final int GROUP_TITLE_HEIGHT = 34;
    private static final int SUBHEADER_HEIGHT = 28;
    private static final int GROUP_HEADER_HEIGHT = GROUP_TITLE_HEIGHT + SUBHEADER_HEIGHT;
    private static final List<String> GROUP_NAMES = List.of(
        "Unadjusted Trial Balance",
        "Adjusting Journal Entries",
        "Adjusted Trial Balance",
        "Income Statement",
        "Owner’s Equity",
        "Balance Sheet"
    );

    private final DefaultTableModel model = new DefaultTableModel(
        new Object[] {
            "Account",
            "UTB Dr",
            "UTB Cr",
            "AJE Dr",
            "AJE Cr",
            "ATB Dr",
            "ATB Cr",
            "IS Dr",
            "IS Cr",
            "OE Dr",
            "OE Cr",
            "BS Dr",
            "BS Cr"
        },
        0
    ) {
        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }

        @Override
        public Class<?> getColumnClass(int column) {
            if (column > 0) {
                return BigDecimal.class;
            }
            return String.class;
        }
    };

    private final JTable table = new JTable(model);
    private final JTable frozenAccountTable = new JTable(model);
    private final JComboBox<PeriodOption> periodBox = new JComboBox<>();
    private final JTextField searchField = new JTextField(14);
    private final JLabel activeCountLabel = new JLabel("0 active accounts");
    private final JLabel shownCountLabel = new JLabel("0 accounts shown");
    private final JLabel statusLabel = new JLabel("Loading worksheet...");
    private final List<WorksheetRow> worksheetRows = new ArrayList<>();
    private int activeAccountCount;
    public TwelveColumnarSheet() {
        setLayout(new BorderLayout(0, 12));
        setBackground(PAGE_BG);
        setBorder(BorderFactory.createEmptyBorder(20, 20, 16, 20));

        JLabel heading = new JLabel("12-Column Worksheet");
        heading.setFont(new Font("SansSerif", Font.BOLD, 22));
        heading.setForeground(NAVY);

        JLabel subtitle = new JLabel(
            "Review your trial balance, adjustments, and financial statement columns."
        );
        subtitle.setFont(new Font("SansSerif", Font.PLAIN, 10));
        subtitle.setForeground(MUTED);

        JPanel titleBlock = new JPanel();
        titleBlock.setOpaque(false);
        titleBlock.setLayout(new BoxLayout(titleBlock, BoxLayout.Y_AXIS));
        titleBlock.add(heading);
        titleBlock.add(Box.createVerticalStrut(6));
        titleBlock.add(subtitle);

        JPanel toolbar = new JPanel(new BorderLayout(8, 0));
        toolbar.setOpaque(true);
        toolbar.setBackground(CARD_BG);
        toolbar.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(BORDER, 1, true),
            BorderFactory.createEmptyBorder(7, 9, 7, 9)
        ));

        periodBox.setFont(new Font("SansSerif", Font.PLAIN, 9));
        periodBox.setBackground(new Color(241, 245, 255));
        periodBox.setForeground(TEXT);
        periodBox.setBorder(BorderFactory.createEmptyBorder(3, 5, 3, 5));
        searchField.setFont(new Font("SansSerif", Font.PLAIN, 9));
        searchField.setForeground(TEXT);
        searchField.setToolTipText("Search account names");
        searchField.setBackground(new Color(241, 245, 255));
        searchField.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(231, 236, 249), 1, true),
            BorderFactory.createEmptyBorder(6, 8, 6, 8)
        ));

        activeCountLabel.setFont(new Font("SansSerif", Font.PLAIN, 9));
        activeCountLabel.setForeground(MUTED);

        JPanel filterPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        filterPanel.setOpaque(false);
        JLabel periodLabel = new JLabel("Period");
        periodLabel.setFont(new Font("SansSerif", Font.BOLD, 9));
        periodLabel.setForeground(MUTED);
        filterPanel.add(periodLabel);
        filterPanel.add(periodBox);
        filterPanel.add(searchField);
        filterPanel.add(activeCountLabel);

        JPanel actionPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        actionPanel.setOpaque(false);

        JButton refreshButton = new JButton("Refresh");
        refreshButton.setFont(new Font("SansSerif", Font.BOLD, 9));
        refreshButton.setForeground(Color.WHITE);
        refreshButton.setBackground(BLUE);
        refreshButton.setFocusPainted(false);
        refreshButton.setBorder(BorderFactory.createEmptyBorder(7, 12, 7, 12));
        refreshButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        refreshButton.addActionListener(event -> refreshWorksheet());

        JButton exportButton = new JButton("Export CSV");
        exportButton.setFont(new Font("SansSerif", Font.BOLD, 9));
        exportButton.setForeground(NAVY);
        exportButton.setBackground(HEADER_SUB_BG);
        exportButton.setFocusPainted(false);
        exportButton.setBorder(BorderFactory.createEmptyBorder(7, 12, 7, 12));
        exportButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        exportButton.addActionListener(event -> exportCsv());

        actionPanel.add(refreshButton);
        actionPanel.add(exportButton);

        toolbar.add(filterPanel, BorderLayout.CENTER);
        toolbar.add(actionPanel, BorderLayout.EAST);

        table.setRowHeight(21);
        table.setFillsViewportHeight(true);
        table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        table.setFont(new Font("SansSerif", Font.PLAIN, 9));
        table.setForeground(TEXT);
        table.setBackground(CARD_BG);
        table.setSelectionBackground(new Color(232, 238, 255));
        table.setSelectionForeground(TEXT);
        table.setShowHorizontalLines(true);
        table.setShowVerticalLines(true);
        table.setGridColor(BORDER);
        table.setIntercellSpacing(new Dimension(1, 1));
        table.setTableHeader(null);
        table.setRowSelectionAllowed(true);
        table.setColumnSelectionAllowed(false);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        frozenAccountTable.setRowHeight(table.getRowHeight());
        frozenAccountTable.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        frozenAccountTable.setFillsViewportHeight(true);
        frozenAccountTable.setShowHorizontalLines(true);
        frozenAccountTable.setShowVerticalLines(true);
        frozenAccountTable.setGridColor(BORDER);
        frozenAccountTable.setIntercellSpacing(new Dimension(1, 1));
        frozenAccountTable.setFont(table.getFont());
        frozenAccountTable.setBackground(CARD_BG);
        frozenAccountTable.setSelectionModel(table.getSelectionModel());
        frozenAccountTable.setRowSelectionAllowed(true);
        frozenAccountTable.setColumnSelectionAllowed(false);
        frozenAccountTable.setTableHeader(null);
        frozenAccountTable.setPreferredScrollableViewportSize(
            new Dimension(ACCOUNT_COLUMN_WIDTH, table.getRowHeight() * 8)
        );

        for (int index = frozenAccountTable.getColumnCount() - 1; index > 0; index--) {
            frozenAccountTable.removeColumn(frozenAccountTable.getColumnModel().getColumn(index));
        }
        for (int index = table.getColumnCount() - 1; index >= 0; index--) {
            if (table.getColumnModel().getColumn(index).getModelIndex() == 0) {
                table.removeColumn(table.getColumnModel().getColumn(index));
                break;
            }
        }

        frozenAccountTable.getColumnModel().getColumn(0).setPreferredWidth(ACCOUNT_COLUMN_WIDTH);
        frozenAccountTable.getColumnModel().getColumn(0).setMinWidth(ACCOUNT_COLUMN_WIDTH);
        frozenAccountTable.getColumnModel().getColumn(0).setMaxWidth(ACCOUNT_COLUMN_WIDTH);
        for (int index = 0; index < table.getColumnCount(); index++) {
            table.getColumnModel().getColumn(index).setPreferredWidth(AMOUNT_COLUMN_WIDTH);
            table.getColumnModel().getColumn(index).setMinWidth(MIN_AMOUNT_COLUMN_WIDTH);
        }

        DefaultTableCellRenderer renderer = new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(
                JTable table,
                Object value,
                boolean isSelected,
                boolean hasFocus,
                int row,
                int column
            ) {
                super.getTableCellRendererComponent(
                    table,
                    value,
                    isSelected,
                    hasFocus,
                    row,
                    column
                );

                int modelColumn = table.convertColumnIndexToModel(column);
                boolean summaryRow = isSummaryRow(table, row);
                boolean selected = isSelected;
                if (modelColumn == 0) {
                    setHorizontalAlignment(SwingConstants.LEFT);
                    setForeground(TEXT);
                    setText(value == null ? "" : value.toString());
                } else {
                    setHorizontalAlignment(SwingConstants.RIGHT);
                    if (value instanceof BigDecimal) {
                        setText(formatMoney((BigDecimal) value));
                    } else {
                        setText("-");
                    }
                    setForeground(TEXT);
                }
                setFont(getFont().deriveFont(summaryRow ? Font.BOLD : Font.PLAIN));
                setBackground(
                    selected ? new Color(220, 231, 255)
                        : summaryRow ? new Color(230, 236, 247)
                        : row % 2 == 0 ? CARD_BG : new Color(239, 245, 255)
                );
                setBorder(BorderFactory.createMatteBorder(0, 0, 1, 1, BORDER));
                return this;
            }
        };

        for (int column = 0; column < table.getColumnCount(); column++) {
            table.getColumnModel().getColumn(column).setCellRenderer(renderer);
        }
        frozenAccountTable.getColumnModel().getColumn(0).setCellRenderer(renderer);

        JPanel sheetContainer = new JPanel(new BorderLayout());
        sheetContainer.setBackground(CARD_BG);
        sheetContainer.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(BORDER, 1, true),
            BorderFactory.createEmptyBorder(0, 0, 0, 0)
        ));
        JScrollPane tableScrollPane = new JScrollPane(table);
        tableScrollPane.setBorder(BorderFactory.createEmptyBorder());
        tableScrollPane.setHorizontalScrollBarPolicy(
            ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED
        );
        tableScrollPane.setVerticalScrollBarPolicy(
            ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED
        );
        JPanel groupHeader = buildGroupHeader();
        tableScrollPane.setColumnHeaderView(null);
        tableScrollPane.setRowHeaderView(frozenAccountTable);
        tableScrollPane.getViewport().setBackground(CARD_BG);
        tableScrollPane.getRowHeader().setBackground(CARD_BG);
        tableScrollPane.getHorizontalScrollBar().setUnitIncrement(24);
        tableScrollPane.getHorizontalScrollBar().setBlockIncrement(120);

        JViewport groupHeaderViewport = new JViewport();
        groupHeaderViewport.setView(groupHeader);
        groupHeaderViewport.setPreferredSize(
            new Dimension(AMOUNT_COLUMN_WIDTH * 12, GROUP_HEADER_HEIGHT)
        );
        JPanel headerStrip = new JPanel(new BorderLayout());
        headerStrip.setBackground(CARD_BG);
        headerStrip.add(buildAccountHeader(), BorderLayout.WEST);
        headerStrip.add(groupHeaderViewport, BorderLayout.CENTER);

        JPanel verticalScrollbarGutter = new JPanel();
        verticalScrollbarGutter.setBackground(CARD_BG);
        verticalScrollbarGutter.setPreferredSize(new Dimension(
            tableScrollPane.getVerticalScrollBar().getPreferredSize().width,
            GROUP_HEADER_HEIGHT
        ));
        verticalScrollbarGutter.setVisible(
            tableScrollPane.getVerticalScrollBar().isVisible()
        );
        headerStrip.add(verticalScrollbarGutter, BorderLayout.EAST);

        tableScrollPane.getHorizontalScrollBar().addAdjustmentListener(event -> {
            Point position = groupHeaderViewport.getViewPosition();
            position.x = Math.min(
                event.getValue(),
                Math.max(0, groupHeader.getWidth() - groupHeaderViewport.getWidth())
            );
            groupHeaderViewport.setViewPosition(position);
        });
        tableScrollPane.getVerticalScrollBar().addComponentListener(
            new java.awt.event.ComponentAdapter() {
                @Override
                public void componentShown(java.awt.event.ComponentEvent event) {
                    verticalScrollbarGutter.setVisible(true);
                    headerStrip.revalidate();
                }

                @Override
                public void componentHidden(java.awt.event.ComponentEvent event) {
                    verticalScrollbarGutter.setVisible(false);
                    headerStrip.revalidate();
                }
            }
        );
        tableScrollPane.getViewport().addComponentListener(new java.awt.event.ComponentAdapter() {
            @Override
            public void componentResized(java.awt.event.ComponentEvent event) {
                resizeAmountColumns(tableScrollPane, groupHeader, groupHeaderViewport);
            }
        });
        SwingUtilities.invokeLater(() ->
            resizeAmountColumns(tableScrollPane, groupHeader, groupHeaderViewport)
        );
        sheetContainer.add(headerStrip, BorderLayout.NORTH);
        sheetContainer.add(tableScrollPane, BorderLayout.CENTER);

        statusLabel.setFont(new Font("SansSerif", Font.PLAIN, 9));
        statusLabel.setForeground(MUTED);
        shownCountLabel.setFont(new Font("SansSerif", Font.PLAIN, 10));
        shownCountLabel.setForeground(MUTED);

        JPanel top = new JPanel(new BorderLayout(0, 10));
        top.setOpaque(false);
        top.add(titleBlock, BorderLayout.NORTH);
        top.add(toolbar, BorderLayout.CENTER);

        JLabel currencyNote = new JLabel(
            "Amounts are in Philippine pesos (PHP). A dash indicates zero or no amount."
        );
        currencyNote.setFont(new Font("SansSerif", Font.PLAIN, 10));
        currencyNote.setForeground(MUTED);

        JPanel footer = new JPanel(new BorderLayout(8, 0));
        footer.setOpaque(false);
        footer.add(statusLabel, BorderLayout.WEST);
        footer.add(currencyNote, BorderLayout.CENTER);
        footer.add(shownCountLabel, BorderLayout.EAST);

        add(top, BorderLayout.NORTH);
        add(sheetContainer, BorderLayout.CENTER);
        add(footer, BorderLayout.SOUTH);

        searchField.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent event) {
                applyAccountFilter();
            }

            @Override
            public void removeUpdate(DocumentEvent event) {
                applyAccountFilter();
            }

            @Override
            public void changedUpdate(DocumentEvent event) {
                applyAccountFilter();
            }
        });

        refreshWorksheet();
    }

    private JPanel buildGroupHeader() {
        JPanel header = new JPanel(new GridLayout(1, 6));
        header.setBackground(HEADER_BG);
        header.setPreferredSize(new Dimension(AMOUNT_COLUMN_WIDTH * 12, GROUP_HEADER_HEIGHT));

        for (String groupName : GROUP_NAMES) {
            JPanel group = new JPanel(new GridLayout(2, 1));
            group.setBackground(HEADER_BG);
            group.setPreferredSize(new Dimension(AMOUNT_COLUMN_WIDTH * 2, GROUP_HEADER_HEIGHT));
            group.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 1, BORDER));

            JLabel title = new JLabel(groupTitleHtml(groupName), SwingConstants.CENTER);
            title.setOpaque(true);
            title.setBackground(HEADER_BG);
            title.setFont(new Font("SansSerif", Font.BOLD, 8));
            title.setForeground(Color.WHITE);
            title.setPreferredSize(new Dimension(AMOUNT_COLUMN_WIDTH * 2, GROUP_TITLE_HEIGHT));
            group.add(title);

            JPanel subheaders = new JPanel(new GridLayout(1, 2));
            Color subheaderColor = subheaderColor(groupName);
            subheaders.setBackground(subheaderColor);
            JLabel debit = createSubheader("Debit", subheaderColor);
            JLabel credit = createSubheader("Credit", subheaderColor);
            subheaders.add(debit);
            subheaders.add(credit);
            group.add(subheaders);
            header.add(group);
        }
        return header;
    }

    private String groupTitleHtml(String groupName) {
        switch (groupName) {
            case "Unadjusted Trial Balance":
                return "<html><center>Unadjusted Trial<br>Balance</center></html>";
            case "Adjusting Journal Entries":
                return "<html><center>Adjusting Journal<br>Entries</center></html>";
            case "Adjusted Trial Balance":
                return "<html><center>Adjusted Trial<br>Balance</center></html>";
            case "Income Statement":
                return "<html><center>Income Statement</center></html>";
            case "Owner’s Equity":
                return "<html><center>Owner’s Equity</center></html>";
            case "Balance Sheet":
                return "<html><center>Balance Sheet</center></html>";
            default:
                return "<html><center>" + groupName + "</center></html>";
        }
    }

    private Color subheaderColor(String groupName) {
        if ("Income Statement".equals(groupName)) {
            return INCOME_SUB_BG;
        }
        if ("Owner’s Equity".equals(groupName)) {
            return EQUITY_SUB_BG;
        }
        if ("Balance Sheet".equals(groupName)) {
            return BALANCE_SUB_BG;
        }
        return HEADER_SUB_BG;
    }

    private void resizeAmountColumns(
        JScrollPane scrollPane,
        JPanel groupHeader,
        JViewport groupHeaderViewport
    ) {
        int viewportWidth = scrollPane.getViewport().getWidth();
        if (viewportWidth <= 0) {
            return;
        }
        int columnWidth = Math.max(MIN_AMOUNT_COLUMN_WIDTH, viewportWidth / table.getColumnCount());
        for (int index = 0; index < table.getColumnCount(); index++) {
            table.getColumnModel().getColumn(index).setPreferredWidth(columnWidth);
        }

        groupHeader.setPreferredSize(new Dimension(columnWidth * 12, GROUP_HEADER_HEIGHT));
        groupHeader.setSize(columnWidth * 12, GROUP_HEADER_HEIGHT);
        groupHeaderViewport.setViewSize(new Dimension(columnWidth * 12, GROUP_HEADER_HEIGHT));
        for (Component component : groupHeader.getComponents()) {
            if (!(component instanceof JPanel)) {
                continue;
            }
            JPanel group = (JPanel) component;
            int groupIndex = groupHeader.getComponentZOrder(group);
            int firstColumn = groupIndex * 2;
            int groupWidth =
                table.getColumnModel().getColumn(firstColumn).getPreferredWidth()
                    + table.getColumnModel().getColumn(firstColumn + 1).getPreferredWidth();
            group.setPreferredSize(new Dimension(groupWidth, GROUP_HEADER_HEIGHT));
            group.setSize(groupWidth, GROUP_HEADER_HEIGHT);
            Component title = group.getComponent(0);
            title.setPreferredSize(new Dimension(groupWidth, GROUP_TITLE_HEIGHT));
        }
        table.revalidate();
        groupHeader.revalidate();
        groupHeaderViewport.revalidate();
        scrollPane.revalidate();
    }

    private JLabel createSubheader(String text, Color background) {
        JLabel label = new JLabel(text, SwingConstants.CENTER);
        label.setOpaque(true);
        label.setBackground(background);
        label.setFont(new Font("SansSerif", Font.BOLD, 8));
        label.setForeground(TEXT);
        label.setPreferredSize(new Dimension(AMOUNT_COLUMN_WIDTH, SUBHEADER_HEIGHT));
        label.setBorder(BorderFactory.createMatteBorder(1, 0, 1, 1, BORDER));
        return label;
    }

    private JComponent buildAccountHeader() {
        JPanel header = new JPanel(new BorderLayout());
        header.setPreferredSize(new Dimension(ACCOUNT_COLUMN_WIDTH, GROUP_HEADER_HEIGHT));

        JLabel title = new JLabel("Account name", SwingConstants.LEFT);
        title.setOpaque(true);
        title.setBackground(HEADER_BG);
        title.setForeground(Color.WHITE);
        title.setFont(new Font("SansSerif", Font.BOLD, 9));
        title.setPreferredSize(new Dimension(ACCOUNT_COLUMN_WIDTH, GROUP_TITLE_HEIGHT));
        title.setBorder(BorderFactory.createEmptyBorder(0, 8, 0, 8));

        JLabel subheader = new JLabel("ACCOUNT", SwingConstants.LEFT);
        subheader.setOpaque(true);
        subheader.setBackground(HEADER_SUB_BG);
        subheader.setForeground(TEXT);
        subheader.setFont(new Font("SansSerif", Font.BOLD, 8));
        subheader.setPreferredSize(new Dimension(ACCOUNT_COLUMN_WIDTH, SUBHEADER_HEIGHT));
        subheader.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(1, 0, 1, 1, BORDER),
            BorderFactory.createEmptyBorder(0, 8, 0, 8)
        ));

        header.add(title, BorderLayout.NORTH);
        header.add(subheader, BorderLayout.CENTER);
        return header;
    }

    public void refreshWorksheet() {
        model.setRowCount(0);
        worksheetRows.clear();

        String username = Session.getCurrentUser();
        if (username == null || username.isBlank()) {
            statusLabel.setText("No active user. Log in to load the worksheet.");
            activeAccountCount = 0;
            updateAccountCounts(0);
            return;
        }

        try {
            loadPeriods(username);
            Map<Integer, AccountInfo> accounts = loadAccounts(username);
            Map<Integer, long[]> unadjusted = loadBalances(username, selectedPeriod());
            Map<Integer, long[]> adjustments = loadAdjustingBalances(username);
            activeAccountCount = accounts.size();

            for (AccountInfo account : accounts.values()) {
                long unadjustedBalance = signedBalance(unadjusted.getOrDefault(account.id, new long[] {0L, 0L}));
                long adjustmentBalance = signedBalance(adjustments.getOrDefault(account.id, new long[] {0L, 0L}));
                long adjustedBalance = unadjustedBalance + adjustmentBalance;

                long[] unadjustedPair = splitSigned(unadjustedBalance);
                long[] adjustingPair = splitSigned(adjustmentBalance);
                long[] adjustedPair = splitSigned(adjustedBalance);
                long[] incomePair = classifyIncome(account.type, adjustedBalance);
                long[] ownerEquityPair = classifyOwnerEquity(account.type, adjustedBalance);
                long[] balanceSheetPair = classifyBalanceSheet(account.type, adjustedBalance);

                worksheetRows.add(new WorksheetRow(
                    account.name,
                    new long[] {
                        unadjustedPair[0], unadjustedPair[1],
                        adjustingPair[0], adjustingPair[1],
                        adjustedPair[0], adjustedPair[1],
                        incomePair[0], incomePair[1],
                        ownerEquityPair[0], ownerEquityPair[1],
                        balanceSheetPair[0], balanceSheetPair[1]
                    }
                ));
            }

            applyAccountFilter();
            statusLabel.setText(
                "Loaded " + activeAccountCount + " active accounts for " + username + "."
            );
        } catch (SQLException exception) {
            statusLabel.setText("Could not load the worksheet for the current user.");
            activeAccountCount = 0;
            updateAccountCounts(0);
            JOptionPane.showMessageDialog(
                this,
                "Could not load worksheet data.\n" + exception.getMessage(),
                "Database Error",
                JOptionPane.ERROR_MESSAGE
            );
        }
    }

    private void applyAccountFilter() {
        model.setRowCount(0);
        String query = searchField.getText().trim().toLowerCase();
        List<WorksheetRow> visibleRows = new ArrayList<>();
        long[] totals = new long[12];

        for (WorksheetRow row : worksheetRows) {
            if (!query.isEmpty() && !row.accountName.toLowerCase().contains(query)) {
                continue;
            }
            visibleRows.add(row);
            addAmounts(totals, row.amounts);

            Object[] tableRow = new Object[13];
            tableRow[0] = row.accountName;
            for (int index = 0; index < row.amounts.length; index++) {
                tableRow[index + 1] = money(row.amounts[index]);
            }
            model.addRow(tableRow);
        }

        long incomeDifference = totals[7] - totals[6];
        if (incomeDifference != 0) {
            long netIncome = incomeDifference > 0 ? incomeDifference : 0L;
            long netLoss = incomeDifference < 0 ? -incomeDifference : 0L;
            long[] netAmounts = new long[12];

            if (netIncome > 0) {
                netAmounts[6] = netIncome;
                netAmounts[9] = netIncome;
                netAmounts[11] = netIncome;
            } else {
                netAmounts[7] = netLoss;
                netAmounts[8] = netLoss;
                netAmounts[10] = netLoss;
            }

            model.addRow(toTableRow(
                netIncome > 0 ? "Net Income" : "Net Loss",
                netAmounts
            ));
            addAmounts(totals, netAmounts);
        }

        Object[] totalsRow = toTableRow("TOTALS (PHP)", totals);
        model.addRow(totalsRow);
        updateAccountCounts(visibleRows.size());
    }

    private Object[] toTableRow(String label, long[] amounts) {
        Object[] row = new Object[13];
        row[0] = label;
        for (int index = 0; index < amounts.length; index++) {
            row[index + 1] = money(amounts[index]);
        }
        return row;
    }

    private void updateAccountCounts(int shownAccounts) {
        activeCountLabel.setText(activeAccountCount + " active accounts");
        shownCountLabel.setText(shownAccounts + " accounts shown");
    }

    private void loadPeriods(String username) throws SQLException {
        PeriodOption previous = (PeriodOption) periodBox.getSelectedItem();
        String selectedKey = previous == null ? "" : previous.key;
        List<PeriodOption> periods = new ArrayList<>();
        periods.add(PeriodOption.allPeriods());

        String sql = """
            SELECT DISTINCT transaction_date
            FROM transactions
            WHERE owner_username = ?
              AND transaction_date IS NOT NULL
            ORDER BY transaction_date DESC
            """;

        try (
            Connection connection = Database.connect();
            PreparedStatement statement = connection.prepareStatement(sql)
        ) {
            statement.setString(1, username);
            try (ResultSet rs = statement.executeQuery()) {
                while (rs.next()) {
                    String transactionDate = rs.getString("transaction_date");
                    if (transactionDate == null || transactionDate.length() < 7) {
                        continue;
                    }
                    try {
                        YearMonth month = YearMonth.parse(transactionDate.substring(0, 7));
                        PeriodOption option = PeriodOption.forMonth(month);
                        if (!periods.contains(option)) {
                            periods.add(option);
                        }
                    } catch (DateTimeParseException ignored) {
                        // Ignore a malformed date when building the period selector.
                    }
                }
            }
        }

        try {
            periodBox.removeAllItems();
            PeriodOption selection = periods.get(0);
            for (PeriodOption option : periods) {
                periodBox.addItem(option);
                if (option.key.equals(selectedKey)) {
                    selection = option;
                }
            }
            periodBox.setSelectedItem(selection);
        } finally {
        }
    }

    private YearMonth selectedPeriod() {
        PeriodOption option = (PeriodOption) periodBox.getSelectedItem();
        return option == null ? null : option.month;
    }

    private boolean isSummaryRow(JTable sourceTable, int viewRow) {
        int modelRow = sourceTable.convertRowIndexToModel(viewRow);
        Object value = model.getValueAt(modelRow, 0);
        return "TOTALS (PHP)".equals(value)
            || "Net Income".equals(value)
            || "Net Loss".equals(value);
    }

    private void exportCsv() {
        if (model.getRowCount() == 0) {
            JOptionPane.showMessageDialog(
                this,
                "There are no worksheet rows to export.",
                "Export CSV",
                JOptionPane.INFORMATION_MESSAGE
            );
            return;
        }

        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Export worksheet as CSV");
        chooser.setFileFilter(new FileNameExtensionFilter("CSV files (*.csv)", "csv"));
        chooser.setSelectedFile(new java.io.File("twelve-column-worksheet.csv"));
        if (chooser.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) {
            return;
        }

        Path destination = chooser.getSelectedFile().toPath();
        if (!destination.getFileName().toString().toLowerCase().endsWith(".csv")) {
            destination = destination.resolveSibling(destination.getFileName() + ".csv");
        }

        String[] groups = new String[13];
        groups[0] = "Account";
        for (int groupIndex = 0; groupIndex < GROUP_NAMES.size(); groupIndex++) {
            groups[groupIndex * 2 + 1] = GROUP_NAMES.get(groupIndex);
            groups[groupIndex * 2 + 2] = GROUP_NAMES.get(groupIndex);
        }
        String[] subheaders = {
            "", "Debit", "Credit", "Debit", "Credit", "Debit", "Credit",
            "Debit", "Credit", "Debit", "Credit", "Debit", "Credit"
        };

        try (BufferedWriter writer = Files.newBufferedWriter(destination, StandardCharsets.UTF_8)) {
            writeCsvRow(writer, groups);
            writeCsvRow(writer, subheaders);
            for (int row = 0; row < model.getRowCount(); row++) {
                String[] values = new String[model.getColumnCount()];
                for (int column = 0; column < model.getColumnCount(); column++) {
                    Object value = model.getValueAt(row, column);
                    values[column] = value instanceof BigDecimal
                        ? ((BigDecimal) value).toPlainString()
                        : value == null ? "" : value.toString();
                }
                writeCsvRow(writer, values);
            }
        } catch (IOException exception) {
            JOptionPane.showMessageDialog(
                this,
                "Could not export the worksheet.\n" + exception.getMessage(),
                "Export Error",
                JOptionPane.ERROR_MESSAGE
            );
            return;
        }

        JOptionPane.showMessageDialog(
            this,
            "Worksheet exported to:\n" + destination.toAbsolutePath(),
            "Export CSV",
            JOptionPane.INFORMATION_MESSAGE
        );
    }

    private void writeCsvRow(BufferedWriter writer, String[] values) throws IOException {
        for (int index = 0; index < values.length; index++) {
            if (index > 0) {
                writer.write(',');
            }
            String value = values[index] == null ? "" : values[index];
            writer.write('"');
            writer.write(value.replace("\"", "\"\""));
            writer.write('"');
        }
        writer.newLine();
    }

    private Map<Integer, AccountInfo> loadAccounts(String username) throws SQLException {
        String sql = """
            SELECT id, name, type
            FROM accounts
            WHERE owner_username = ?
              AND active = 1
            ORDER BY CAST(code AS INTEGER)
            """;

        Map<Integer, AccountInfo> result = new LinkedHashMap<>();

        try (
            Connection connection = Database.connect();
            PreparedStatement statement = connection.prepareStatement(sql)
        ) {
            statement.setString(1, username);

            try (ResultSet rs = statement.executeQuery()) {
                while (rs.next()) {
                    result.put(
                        rs.getInt("id"),
                        new AccountInfo(
                            rs.getInt("id"),
                            rs.getString("name"),
                            rs.getString("type")
                        )
                    );
                }
            }
        }

        return result;
    }

    private Map<Integer, long[]> loadBalances(String username, YearMonth period) throws SQLException {
        String sql = """
            SELECT jl.account_id,
                   COALESCE(SUM(jl.debit_centavos), 0) AS debit_total,
                   COALESCE(SUM(jl.credit_centavos), 0) AS credit_total
            FROM journal_lines jl
            JOIN transactions t ON t.id = jl.transaction_id
            WHERE t.owner_username = ?
            """ + (period == null ? "" : """
              AND t.transaction_date >= ?
              AND t.transaction_date < ?
            """) + """
            GROUP BY jl.account_id
            """;

        Map<Integer, long[]> balances = new HashMap<>();

        try (
            Connection connection = Database.connect();
            PreparedStatement statement = connection.prepareStatement(sql)
        ) {
            statement.setString(1, username);
            if (period != null) {
                statement.setDate(2, Date.valueOf(period.atDay(1)));
                statement.setDate(3, Date.valueOf(period.plusMonths(1).atDay(1)));
            }

            try (ResultSet rs = statement.executeQuery()) {
                while (rs.next()) {
                    int accountId = rs.getInt("account_id");
                    long debit = rs.getLong("debit_total");
                    long credit = rs.getLong("credit_total");
                    balances.put(accountId, new long[] {debit, credit});
                }
            }
        }

        return balances;
    }

    private Map<Integer, long[]> loadAdjustingBalances(String username) throws SQLException {
        Map<Integer, long[]> balances = new HashMap<>();
        List<String> candidateSql = List.of(
            """
            SELECT account_id,
                   COALESCE(SUM(debit_centavos), 0) AS debit_total,
                   COALESCE(SUM(credit_centavos), 0) AS credit_total
            FROM adjusting_entries
            WHERE owner_username = ?
            GROUP BY account_id
            """,
            """
            SELECT account_id,
                   COALESCE(SUM(debit_centavos), 0) AS debit_total,
                   COALESCE(SUM(credit_centavos), 0) AS credit_total
            FROM adjusting_journal_lines
            WHERE owner_username = ?
            GROUP BY account_id
            """,
            """
            SELECT account_id,
                   COALESCE(SUM(debit_centavos), 0) AS debit_total,
                   COALESCE(SUM(credit_centavos), 0) AS credit_total
            FROM adjustments
            WHERE owner_username = ?
            GROUP BY account_id
            """
        );

        for (String sql : candidateSql) {
            try (
                Connection connection = Database.connect();
                PreparedStatement statement = connection.prepareStatement(sql)
            ) {
                statement.setString(1, username);
                try (ResultSet rs = statement.executeQuery()) {
                    while (rs.next()) {
                        int accountId = rs.getInt("account_id");
                        balances.put(
                            accountId,
                            new long[] {
                                rs.getLong("debit_total"),
                                rs.getLong("credit_total")
                            }
                        );
                    }
                }
                if (!balances.isEmpty()) {
                    return balances;
                }
            } catch (SQLException ignored) {
                // The table is not present or schema differs; keep trying the next candidate.
            }
        }

        return balances;
    }

    private static long signedBalance(long[] pair) {
        return pair[0] - pair[1];
    }

    private static long[] splitSigned(long signedCentavos) {
        if (signedCentavos >= 0) {
            return new long[] {signedCentavos, 0L};
        }
        return new long[] {0L, -signedCentavos};
    }

    private static long[] classifyIncome(String type, long adjustedBalance) {
        long[] sides = splitSigned(adjustedBalance);
        if ("REVENUE".equals(type)) {
            return sides[1] > 0
                ? new long[] {0L, sides[1]}
                : new long[] {sides[0], 0L};
        }
        if ("EXPENSE".equals(type)) {
            return sides[0] > 0
                ? new long[] {sides[0], 0L}
                : new long[] {0L, sides[1]};
        }
        return new long[] {0L, 0L};
    }

    private static long[] classifyOwnerEquity(String type, long adjustedBalance) {
        if ("EQUITY".equals(type) || "CONTRA-EQUITY".equals(type)) {
            return splitSigned(adjustedBalance);
        }
        return new long[] {0L, 0L};
    }

    private static long[] classifyBalanceSheet(String type, long adjustedBalance) {
        if ("ASSET".equals(type)
                || "CONTRA-ASSET".equals(type)
                || "LIABILITY".equals(type)
                || "EQUITY".equals(type)
                || "CONTRA-EQUITY".equals(type)) {
            return splitSigned(adjustedBalance);
        }
        return new long[] {0L, 0L};
    }

    private static void addAmounts(long[] totals, long[] values) {
        for (int index = 0; index < values.length; index++) {
            totals[index] += values[index];
        }
    }

    private static BigDecimal money(long cents) {
        return BigDecimal.valueOf(cents, 2);
    }

    private static String formatMoney(BigDecimal amount) {
        return amount.signum() == 0
            ? "-"
            : "₱ " + amount.setScale(2, java.math.RoundingMode.HALF_UP).toPlainString();
    }

    private static final class PeriodOption {
        private final YearMonth month;
        private final String key;
        private final String label;

        private PeriodOption(YearMonth month, String key, String label) {
            this.month = month;
            this.key = key;
            this.label = label;
        }

        private static PeriodOption allPeriods() {
            return new PeriodOption(null, "all", "All periods");
        }

        private static PeriodOption forMonth(YearMonth month) {
            return new PeriodOption(
                month,
                month.toString(),
                month.format(DateTimeFormatter.ofPattern("MMMM yyyy"))
            );
        }

        @Override
        public String toString() {
            return label;
        }

        @Override
        public boolean equals(Object other) {
            if (!(other instanceof PeriodOption)) {
                return false;
            }
            return key.equals(((PeriodOption) other).key);
        }

        @Override
        public int hashCode() {
            return key.hashCode();
        }
    }

    private static final class WorksheetRow {
        private final String accountName;
        private final long[] amounts;

        private WorksheetRow(String accountName, long[] amounts) {
            this.accountName = accountName;
            this.amounts = amounts;
        }
    }

    private static final class AccountInfo {
        private final int id;
        private final String name;
        private final String type;

        private AccountInfo(int id, String name, String type) {
            this.id = id;
            this.name = name;
            this.type = type;
        }
    }
}
