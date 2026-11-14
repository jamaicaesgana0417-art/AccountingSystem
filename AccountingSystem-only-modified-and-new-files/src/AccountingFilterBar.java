import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.time.*;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Supplier;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.*;

/**
 * Shared filter panel for the accounting tables:
 *
 *   [ Filter Transactions                                          X ]
 *   Search          | Account        | Date Range     | Amount Range
 *   (the Search column and its suggestions are optional)
 *   [Enter keyword] | [All Accounts] | [Select Date]  | [P0 - P-inf]
 *   Reset Filters                                      [Apply Filters]
 *
 * Filters are applied to the displayed table rows only; the underlying
 * records remain unchanged.
 */
public final class AccountingFilterBar extends JPanel {

    private static final String ALL_ACCOUNTS = "All Accounts";
    private static final int ACCOUNT_ROW_HEIGHT = 54;
    private static final String SELECT_DATE = "Select Date";
    private static final int MAX_SUGGESTIONS = 8;
    private static final int SUGGESTION_ROW_HEIGHT = 30;
    private static final DecimalFormat MONEY = new DecimalFormat("#,##0.##");

    // ---- Palette ----
    private static Color cCard()   { return Color.WHITE; }
    private static Color cField()  { return Color.WHITE; }
    private static Color cBorder() { return new Color(222, 229, 241); }
    private static Color cText()   { return new Color(24, 38, 79); }
    private static Color cMuted()  { return new Color(100, 116, 145); }
    private static Color cBlue()   { return new Color(37, 99, 235); }
    private static Color cTint()   { return new Color(238, 243, 255); }
    private static Color cError()  { return new Color(200, 40, 40); }

    private static final Font LABEL_FONT = new Font("Segoe UI", Font.BOLD, 12);
    private static final Font VALUE_FONT = new Font("Segoe UI", Font.PLAIN, 13);

    // ---- Table configuration ----
    private final JTable[] tables;
    private final int dateColumn;
    private final int[] descriptionColumns;
    private final int[] accountColumns;
    private final int[] amountColumns;

    // ---- Filter state ----
    private String selectedAccount = null;      // null = all accounts
    private String datePreset = null;           // null = no date filter
    private LocalDate customFrom = null;
    private LocalDate customTo = null;
    private BigDecimal minAmount = null;
    private BigDecimal maxAmount = null;

    // ---- Widgets ----
    private final HintField searchField = new HintField("Enter keyword...");
    private final Picker accountPicker;
    private final Picker datePicker;
    private final Picker amountPicker;
    private final JLabel titleLabel = new JLabel();
    private final JLabel resultLabel = new JLabel(" ");
    private final JButton closeButton = new JButton();
    private final JPanel body = new JPanel(new BorderLayout(0, 16));
    private final JPanel actionsPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
    private final JButton resetButton = new JButton("Reset Filters");
    private final JButton applyButton = new JButton("Apply Filters");
    private final List<JLabel> fieldLabels = new ArrayList<>();
    private final List<JButton> pillButtons = new ArrayList<>();
    private boolean collapsed = false;

    // ---- Search suggestions ----
    private final DefaultListModel<String> suggestionModel = new DefaultListModel<>();
    private final JList<String> suggestionList = new JList<>(suggestionModel);
    private final JPopupMenu suggestionPopup = new JPopupMenu();
    private boolean suppressSuggestions = false;

    public AccountingFilterBar(
            JTable[] tables,
            int dateColumn,
            int[] descriptionColumns,
            int[] accountColumns,
            int[] amountColumns,
            boolean includePeriod,
            String title) {
        this(tables, dateColumn, descriptionColumns, accountColumns,
             amountColumns, includePeriod, true, title);
    }

    public AccountingFilterBar(
            JTable[] tables,
            int dateColumn,
            int[] descriptionColumns,
            int[] accountColumns,
            int[] amountColumns,
            boolean includePeriod,
            boolean includeSearch,
            String title) {
        this.tables = tables;
        this.dateColumn = dateColumn;
        this.descriptionColumns = descriptionColumns;
        this.accountColumns = accountColumns;
        this.amountColumns = amountColumns;

        setOpaque(false);
        setLayout(new BorderLayout(0, 16));
        setBorder(new EmptyBorder(18, 22, 18, 22));

        accountPicker = new Picker(null, ALL_ACCOUNTS, this::buildAccountPopup);
        datePicker = new Picker(Glyph.Kind.CALENDAR, SELECT_DATE, this::buildDatePopup);
        amountPicker = new Picker(null, amountText(), this::buildAmountPopup);

        // Header: funnel icon, title, result count, close button
        JPanel header = new JPanel(new BorderLayout(10, 0));
        header.setOpaque(false);
        JPanel heading = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        heading.setOpaque(false);
        titleLabel.setText(title);
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 16));
        titleLabel.setIcon(new Glyph(Glyph.Kind.FUNNEL, 20, AccountingFilterBar::cBlue));
        titleLabel.setIconTextGap(10);
        resultLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        resultLabel.setBorder(new EmptyBorder(0, 14, 0, 0));
        heading.add(titleLabel);
        heading.add(resultLabel);
        header.add(heading, BorderLayout.CENTER);

        closeButton.setIcon(new Glyph(Glyph.Kind.CLOSE, 18, AccountingFilterBar::cText));
        closeButton.setToolTipText("Hide filters");
        closeButton.setContentAreaFilled(false);
        closeButton.setBorderPainted(false);
        closeButton.setFocusPainted(false);
        closeButton.setOpaque(false);
        closeButton.setPreferredSize(new Dimension(32, 32));
        closeButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        closeButton.addActionListener(e -> setCollapsed(!collapsed));
        header.add(closeButton, BorderLayout.EAST);
        add(header, BorderLayout.NORTH);

        // Four labelled fields separated by thin dividers
        JPanel fields = new JPanel(new GridBagLayout());
        fields.setOpaque(false);
        GridBagConstraints c = new GridBagConstraints();
        c.gridy = 0;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.anchor = GridBagConstraints.NORTHWEST;

        int x = 0;
        if (includeSearch) {
            c.weightx = 0.28;
            addColumn(fields, c, x++, "Search", searchField);
            addDivider(fields, x++);
        }
        c.weightx = 0.24;
        addColumn(fields, c, x++, "Account", accountPicker);
        if (includePeriod) {
            addDivider(fields, x++);
            c.weightx = 0.24;
            addColumn(fields, c, x++, "Date Range", datePicker);
        }
        addDivider(fields, x++);
        c.weightx = 0.24;
        addColumn(fields, c, x, "Amount Range", amountPicker);

        // Footer: Reset on the left, extra actions + Apply on the right
        JPanel footer = new JPanel(new BorderLayout());
        footer.setOpaque(false);

        resetButton.setIcon(new Glyph(Glyph.Kind.RESET, 18, AccountingFilterBar::cBlue));
        resetButton.setIconTextGap(8);
        resetButton.setFont(new Font("Segoe UI", Font.BOLD, 13));
        resetButton.setContentAreaFilled(false);
        resetButton.setBorderPainted(false);
        resetButton.setFocusPainted(false);
        resetButton.setOpaque(false);
        resetButton.setBorder(new EmptyBorder(6, 2, 6, 6));
        resetButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        resetButton.addActionListener(e -> resetFilters());

        applyButton.setIcon(new Glyph(Glyph.Kind.SEARCH, 18, () -> Color.WHITE));
        applyButton.setIconTextGap(9);
        stylePill(applyButton, true);
        applyButton.addActionListener(e -> applyFilters());

        actionsPanel.setOpaque(false);
        actionsPanel.add(applyButton);

        footer.add(resetButton, BorderLayout.WEST);
        footer.add(actionsPanel, BorderLayout.EAST);

        body.setOpaque(false);
        body.add(fields, BorderLayout.CENTER);
        body.add(footer, BorderLayout.SOUTH);
        add(body, BorderLayout.CENTER);

        searchField.addActionListener(e -> applyFilters());
        applyColors();
        setupSuggestions();
    }

    public AccountingFilterBar(
            JTable table, int dateColumn, int[] descriptionColumns,
            int[] accountColumns, int[] amountColumns, String title) {
        this(new JTable[]{table}, dateColumn, descriptionColumns,
             accountColumns, amountColumns, true, title);
    }

    // ---- Layout helpers ----

    private void addColumn(JPanel panel, GridBagConstraints c, int x,
                           String labelText, JComponent control) {
        JPanel column = new JPanel(new BorderLayout(0, 8));
        column.setOpaque(false);
        JLabel label = new JLabel(labelText);
        label.setFont(LABEL_FONT);
        fieldLabels.add(label);
        column.add(label, BorderLayout.NORTH);
        control.setPreferredSize(new Dimension(120, 42));
        column.add(control, BorderLayout.CENTER);
        c.gridx = x;
        c.insets = new Insets(0, 0, 0, 0);
        panel.add(column, c);
    }

    private void addDivider(JPanel panel, int x) {
        JPanel divider = new JPanel() {
            @Override protected void paintComponent(Graphics g) {
                g.setColor(cBorder());
                g.fillRect(getWidth() / 2, 0, 1, getHeight());
            }
        };
        divider.setOpaque(false);
        divider.setPreferredSize(new Dimension(21, 42));
        GridBagConstraints c = new GridBagConstraints();
        c.gridx = x;
        c.gridy = 0;
        c.weightx = 0;
        c.fill = GridBagConstraints.VERTICAL;
        c.anchor = GridBagConstraints.SOUTH;
        c.insets = new Insets(26, 0, 0, 0);
        panel.add(divider, c);
    }

    /** Lets the screens add their own buttons (e.g. Export CSV) next to Apply. */
    public void addActionButton(JButton button) {
        stylePill(button, false);
        actionsPanel.add(button, actionsPanel.getComponentCount() - 1);
        actionsPanel.revalidate();
    }

    private void stylePill(JButton button, boolean primary) {
        button.setFont(new Font("Segoe UI", Font.BOLD, 13));
        button.setFocusPainted(false);
        button.setContentAreaFilled(false);
        button.setOpaque(false);
        button.setBorder(new EmptyBorder(0, 20, 0, 20));
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        button.setRolloverEnabled(true);
        button.setPreferredSize(new Dimension(
            Math.max(primary ? 150 : 110, button.getPreferredSize().width), 44));
        button.setUI(new javax.swing.plaf.basic.BasicButtonUI() {
            @Override public void paint(Graphics g, JComponent comp) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON);
                ButtonModel model = ((AbstractButton) comp).getModel();
                int w = comp.getWidth() - 1;
                int h = comp.getHeight() - 1;
                if (primary) {
                    Color base = cBlue();
                    g2.setColor(model.isPressed() ? base.darker().darker()
                        : model.isRollover() ? base.darker() : base);
                    g2.fillRoundRect(0, 0, w, h, 14, 14);
                } else {
                    g2.setColor(model.isRollover() ? cTint() : cField());
                    g2.fillRoundRect(0, 0, w, h, 14, 14);
                    g2.setColor(cBorder());
                    g2.drawRoundRect(0, 0, w, h, 14, 14);
                }
                g2.dispose();
                super.paint(g, comp);
            }
        });
        if (!pillButtons.contains(button)) pillButtons.add(button);
        button.setForeground(primary ? Color.WHITE : cText());
    }

    /** Applies the palette colors to the child components. */
    void applyColors() {
        titleLabel.setForeground(cText());
        resultLabel.setForeground(cMuted());
        resetButton.setForeground(cBlue());
        for (JLabel label : fieldLabels) label.setForeground(cText());
        for (JButton b : pillButtons) {
            if (b != applyButton) b.setForeground(cText());
        }
        applyButton.setForeground(Color.WHITE);
        searchField.applyColors();
        accountPicker.applyColors();
        datePicker.applyColors();
        amountPicker.applyColors();
        repaint();
    }

    private void setCollapsed(boolean value) {
        collapsed = value;
        body.setVisible(!value);
        closeButton.setIcon(new Glyph(
            value ? Glyph.Kind.CHEVRON : Glyph.Kind.CLOSE, 18, AccountingFilterBar::cText));
        closeButton.setToolTipText(value ? "Show filters" : "Hide filters");
        revalidate();
        repaint();
    }

    @Override
    protected void paintComponent(Graphics graphics) {
        Graphics2D g2 = (Graphics2D) graphics.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setColor(cCard());
        g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 24, 24);
        g2.setColor(cBorder());
        g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 24, 24);
        g2.dispose();
        super.paintComponent(graphics);
    }

    // ---- Popups ----

    private JPopupMenu newPopup() {
        JPopupMenu popup = new JPopupMenu();
        popup.setBorder(BorderFactory.createLineBorder(cBorder(), 1, true));
        popup.setBackground(cCard());
        popup.setOpaque(true);
        return popup;
    }

    private JComponent buildAccountPopup(JPopupMenu popup, int width) {
        List<String> items = new ArrayList<>();
        items.add(ALL_ACCOUNTS);
        items.addAll(collectAccounts());

        JList<String> list = new JList<>(items.toArray(new String[0]));
        list.setFixedCellHeight(ACCOUNT_ROW_HEIGHT);
        list.setBackground(cCard());
        list.setForeground(cText());
        list.setFont(VALUE_FONT);
        list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        list.setCellRenderer(new ListCellRenderer<String>() {
            private final JPanel row = new JPanel(new GridLayout(2, 1, 0, 1));
            private final JLabel name = new JLabel();
            private final JLabel description = new JLabel();
            {
                row.setBorder(new EmptyBorder(6, 14, 6, 14));
                row.setOpaque(true);
                description.setFont(new Font("Segoe UI", Font.PLAIN, 11));
                row.add(name);
                row.add(description);
            }
            @Override public Component getListCellRendererComponent(
                    JList<? extends String> l, String value, int index,
                    boolean sel, boolean focus) {
                boolean all = ALL_ACCOUNTS.equals(value);
                boolean current = selectedAccount == null
                    ? all : value.equalsIgnoreCase(selectedAccount);
                String text = all ? "Show every account"
                    : ChartOfAccounts.describe(value);
                name.setText(value);
                name.setForeground(current ? cBlue() : cText());
                name.setFont(current ? VALUE_FONT.deriveFont(Font.BOLD) : VALUE_FONT);
                description.setText(text == null ? " " : text);
                description.setForeground(cMuted());
                row.setBackground(sel ? cTint() : cCard());
                return row;
            }
        });
        list.addMouseMotionListener(new MouseMotionAdapter() {
            @Override public void mouseMoved(MouseEvent e) {
                list.setSelectedIndex(list.locationToIndex(e.getPoint()));
            }
        });
        list.addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) {
                int index = list.locationToIndex(e.getPoint());
                if (index < 0) return;
                String value = items.get(index);
                selectedAccount = ALL_ACCOUNTS.equals(value) ? null : value;
                accountPicker.setText(value);
                popup.setVisible(false);
            }
        });

        JScrollPane scroll = new JScrollPane(list);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.getViewport().setBackground(cCard());
        scroll.setPreferredSize(new Dimension(
            Math.max(width, 300), Math.min(items.size() * ACCOUNT_ROW_HEIGHT + 4, 320)));
        return scroll;
    }

    private JComponent buildDatePopup(JPopupMenu popup, int width) {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBackground(cCard());
        panel.setBorder(new EmptyBorder(6, 6, 10, 6));

        String[] presets = {"Any date", "Today", "This Week", "This Month",
                            "Last 7 Days", "Last 30 Days"};
        for (String preset : presets) {
            boolean current = "Any date".equals(preset)
                ? datePreset == null : preset.equals(datePreset);
            panel.add(menuRow(preset, current, () -> {
                datePreset = "Any date".equals(preset) ? null : preset;
                customFrom = null;
                customTo = null;
                datePicker.setText(dateText());
                popup.setVisible(false);
            }));
        }

        panel.add(Box.createVerticalStrut(6));
        JPanel rule = new JPanel();
        rule.setBackground(cBorder());
        rule.setMaximumSize(new Dimension(Integer.MAX_VALUE, 1));
        rule.setPreferredSize(new Dimension(1, 1));
        panel.add(rule);
        panel.add(Box.createVerticalStrut(10));

        JLabel caption = new JLabel("CUSTOM RANGE  (YYYY-MM-DD)");
        caption.setFont(new Font("Segoe UI", Font.BOLD, 10));
        caption.setForeground(cMuted());
        caption.setBorder(new EmptyBorder(0, 8, 6, 0));
        caption.setAlignmentX(Component.LEFT_ALIGNMENT);
        panel.add(caption);

        HintField from = new HintField("From");
        HintField to = new HintField("To");
        if (customFrom != null) from.setText(customFrom.toString());
        if (customTo != null) to.setText(customTo.toString());
        JPanel inputs = new JPanel(new GridLayout(1, 2, 8, 0));
        inputs.setOpaque(false);
        inputs.setBorder(new EmptyBorder(0, 8, 0, 8));
        inputs.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        inputs.setAlignmentX(Component.LEFT_ALIGNMENT);
        inputs.add(from);
        inputs.add(to);
        panel.add(inputs);

        JLabel error = new JLabel(" ");
        error.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        error.setForeground(cError());
        error.setBorder(new EmptyBorder(6, 8, 4, 0));
        error.setAlignmentX(Component.LEFT_ALIGNMENT);
        panel.add(error);

        JButton set = popupButton("Set date range");
        set.addActionListener(e -> {
            try {
                LocalDate start = from.getText().trim().isEmpty()
                    ? null : LocalDate.parse(from.getText().trim());
                LocalDate end = to.getText().trim().isEmpty()
                    ? null : LocalDate.parse(to.getText().trim());
                if (start == null && end == null) {
                    error.setText("Enter at least one date.");
                    return;
                }
                if (start != null && end != null && start.isAfter(end)) {
                    error.setText("The start date is after the end date.");
                    return;
                }
                datePreset = "Custom Range";
                customFrom = start;
                customTo = end;
                datePicker.setText(dateText());
                popup.setVisible(false);
            } catch (DateTimeParseException ex) {
                error.setText("Use the format YYYY-MM-DD.");
            }
        });
        JPanel setRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        setRow.setOpaque(false);
        setRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        setRow.add(set);
        panel.add(setRow);

        panel.setPreferredSize(new Dimension(Math.max(width, 290), 330));
        return panel;
    }

    private JComponent buildAmountPopup(JPopupMenu popup, int width) {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBackground(cCard());
        panel.setBorder(new EmptyBorder(14, 14, 14, 14));

        JLabel caption = new JLabel("AMOUNT RANGE (\u20B1)");
        caption.setFont(new Font("Segoe UI", Font.BOLD, 10));
        caption.setForeground(cMuted());
        caption.setAlignmentX(Component.LEFT_ALIGNMENT);
        panel.add(caption);
        panel.add(Box.createVerticalStrut(8));

        HintField min = new HintField("Minimum");
        HintField max = new HintField("Maximum");
        if (minAmount != null) min.setText(minAmount.toPlainString());
        if (maxAmount != null) max.setText(maxAmount.toPlainString());
        JPanel inputs = new JPanel(new GridLayout(1, 2, 8, 0));
        inputs.setOpaque(false);
        inputs.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        inputs.setAlignmentX(Component.LEFT_ALIGNMENT);
        inputs.add(min);
        inputs.add(max);
        panel.add(inputs);

        JLabel error = new JLabel(" ");
        error.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        error.setForeground(cError());
        error.setBorder(new EmptyBorder(6, 0, 6, 0));
        error.setAlignmentX(Component.LEFT_ALIGNMENT);
        panel.add(error);

        JButton set = popupButton("Set amount range");
        set.addActionListener(e -> {
            try {
                BigDecimal lo = parseAmount(min.getText());
                BigDecimal hi = parseAmount(max.getText());
                if (lo != null && hi != null && lo.compareTo(hi) > 0) {
                    error.setText("Minimum cannot exceed maximum.");
                    return;
                }
                minAmount = lo;
                maxAmount = hi;
                amountPicker.setText(amountText());
                popup.setVisible(false);
            } catch (NumberFormatException ex) {
                error.setText("Use numbers only, e.g. 1500.50");
            }
        });
        JButton clear = new JButton("Clear");
        clear.setFont(new Font("Segoe UI", Font.BOLD, 12));
        clear.setForeground(cBlue());
        clear.setContentAreaFilled(false);
        clear.setBorderPainted(false);
        clear.setFocusPainted(false);
        clear.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        clear.addActionListener(e -> {
            minAmount = null;
            maxAmount = null;
            amountPicker.setText(amountText());
            popup.setVisible(false);
        });
        JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        row.setOpaque(false);
        row.setAlignmentX(Component.LEFT_ALIGNMENT);
        row.add(set);
        row.add(clear);
        panel.add(row);

        panel.setPreferredSize(new Dimension(Math.max(width, 300), 160));
        return panel;
    }

    private JButton menuRow(String text, boolean current, Runnable action) {
        JButton row = new JButton(text) {
            @Override protected void paintComponent(Graphics g) {
                if (getModel().isRollover()) {
                    Graphics2D g2 = (Graphics2D) g.create();
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                        RenderingHints.VALUE_ANTIALIAS_ON);
                    g2.setColor(cTint());
                    g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 10, 10);
                    g2.dispose();
                }
                super.paintComponent(g);
            }
        };
        row.setHorizontalAlignment(SwingConstants.LEFT);
        row.setFont(current ? VALUE_FONT.deriveFont(Font.BOLD) : VALUE_FONT);
        row.setForeground(current ? cBlue() : cText());
        row.setContentAreaFilled(false);
        row.setOpaque(false);
        row.setBorderPainted(false);
        row.setFocusPainted(false);
        row.setRolloverEnabled(true);
        row.setBorder(new EmptyBorder(0, 10, 0, 10));
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 34));
        row.setPreferredSize(new Dimension(100, 34));
        row.setAlignmentX(Component.LEFT_ALIGNMENT);
        row.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        row.addActionListener(e -> action.run());
        return row;
    }

    private JButton popupButton(String text) {
        JButton button = new JButton(text);
        stylePill(button, true);
        button.setPreferredSize(new Dimension(160, 36));
        return button;
    }

    // ---- Filter state -> display text ----

    private String dateText() {
        if (datePreset == null) return SELECT_DATE;
        if (!"Custom Range".equals(datePreset)) return datePreset;
        if (customFrom != null && customTo != null) return customFrom + " \u2013 " + customTo;
        if (customFrom != null) return "From " + customFrom;
        return "Until " + customTo;
    }

    private String amountText() {
        String lo = minAmount == null ? "0" : MONEY.format(minAmount);
        String hi = maxAmount == null ? "\u221E" : MONEY.format(maxAmount);
        return "\u20B1" + lo + " \u2013 \u20B1" + hi;
    }

    private LocalDate[] dateRange() {
        if (datePreset == null) return null;
        LocalDate today = LocalDate.now();
        switch (datePreset) {
            case "This Month":   return new LocalDate[]{today.withDayOfMonth(1), today};
            case "This Week":    return new LocalDate[]{today.with(DayOfWeek.MONDAY), today};
            case "Today":        return new LocalDate[]{today, today};
            case "Last 7 Days":  return new LocalDate[]{today.minusDays(6), today};
            case "Last 30 Days": return new LocalDate[]{today.minusDays(29), today};
            case "Custom Range": return new LocalDate[]{customFrom, customTo};
            default:             return null;
        }
    }

    private BigDecimal parseAmount(String value) {
        String cleaned = value.trim().replace("\u20B1", "").replace(",", "");
        if (cleaned.isEmpty()) return null;
        return new BigDecimal(cleaned);
    }

    /** Distinct account names currently shown in the tables, sorted. */
    private List<String> collectAccounts() {
        return collectNames(accountColumns);
    }

    /** Distinct non-empty values from the given columns, sorted, case-insensitive. */
    private List<String> collectNames(int[]... columnSets) {
        TreeMap<String, String> names = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        for (JTable table : tables) {
            TableModel model = table.getModel();
            for (int row = 0; row < model.getRowCount(); row++) {
                for (int[] columns : columnSets) {
                    for (int col : columns) {
                        if (col < 0 || col >= model.getColumnCount()) continue;
                        Object value = model.getValueAt(row, col);
                        if (value == null) continue;
                        String name = value.toString().trim();
                        if (name.isEmpty() || name.startsWith("Total ")) continue;
                        names.putIfAbsent(name, name);
                    }
                }
            }
        }
        return new ArrayList<>(names.values());
    }

    // ---- Filtering ----

    private boolean textMatches(TableModel model, int row, String query) {
        int[] all = new int[descriptionColumns.length + accountColumns.length + amountColumns.length];
        int p = 0;
        for (int col : descriptionColumns) all[p++] = col;
        for (int col : accountColumns) all[p++] = col;
        for (int col : amountColumns) all[p++] = col;

        for (int col : all) {
            if (col < 0 || col >= model.getColumnCount()) continue;
            Object value = model.getValueAt(row, col);
            String text = value == null ? "" : value.toString().trim();
            if (text.toLowerCase(Locale.ROOT).contains(query)) return true;

            // General Journal continuation rows leave the description blank:
            // they belong to the nearest entry above.
            if (text.isEmpty() && dateColumn >= 0 && isDescriptionColumn(col)) {
                for (int prior = row - 1; prior >= 0; prior--) {
                    Object previous = model.getValueAt(prior, col);
                    if (previous != null && !previous.toString().trim().isEmpty()) {
                        if (previous.toString().toLowerCase(Locale.ROOT).contains(query)) return true;
                        break;
                    }
                }
            }
        }
        return false;
    }

    private boolean isDescriptionColumn(int col) {
        for (int d : descriptionColumns) if (d == col) return true;
        return false;
    }

    private boolean accountMatches(TableModel model, int row, String account) {
        for (int col : accountColumns) {
            if (col < 0 || col >= model.getColumnCount()) continue;
            Object value = model.getValueAt(row, col);
            if (value != null && value.toString().trim().equalsIgnoreCase(account)) return true;
        }
        return false;
    }

    private boolean amountMatches(TableModel model, int row, BigDecimal min, BigDecimal max) {
        for (int col : amountColumns) {
            if (col < 0 || col >= model.getColumnCount()) continue;
            Object value = model.getValueAt(row, col);
            if (value instanceof Number) {
                BigDecimal amount = new BigDecimal(value.toString()).abs();
                if ((min == null || amount.compareTo(min) >= 0)
                        && (max == null || amount.compareTo(max) <= 0)) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean dateMatches(TableModel model, int row, LocalDate[] range) {
        Object value = model.getValueAt(row, dateColumn);
        String dateText = value == null ? "" : value.toString().trim();
        if (dateText.isEmpty()) {
            // Continuation lines inherit the nearest preceding entry date.
            for (int prior = row - 1; prior >= 0; prior--) {
                Object previous = model.getValueAt(prior, dateColumn);
                if (previous != null && !previous.toString().trim().isEmpty()) {
                    dateText = previous.toString().trim();
                    break;
                }
            }
        }
        if (dateText.isEmpty()) return true;
        try {
            LocalDate date = LocalDate.parse(dateText);
            if (range[0] != null && date.isBefore(range[0])) return false;
            if (range[1] != null && date.isAfter(range[1])) return false;
            return true;
        } catch (DateTimeParseException ex) {
            return false;
        }
    }

    @SuppressWarnings("unchecked")
    private void applyFilters() {
        final String query = searchField.getText().trim().toLowerCase(Locale.ROOT);
        final String account = selectedAccount;
        final BigDecimal min = minAmount;
        final BigDecimal max = maxAmount;
        final LocalDate[] dates = dateColumn >= 0 ? dateRange() : null;

        int visible = 0;
        for (JTable table : tables) {
            TableRowSorter<TableModel> sorter;
            if (table.getRowSorter() instanceof TableRowSorter) {
                sorter = (TableRowSorter<TableModel>) table.getRowSorter();
            } else {
                sorter = new TableRowSorter<>(table.getModel());
                table.setRowSorter(sorter);
            }

            sorter.setRowFilter(new RowFilter<TableModel, Integer>() {
                @Override
                public boolean include(Entry<? extends TableModel, ? extends Integer> entry) {
                    TableModel model = entry.getModel();
                    int row = entry.getIdentifier();
                    if (!query.isEmpty() && !textMatches(model, row, query)) return false;
                    if (account != null && !accountMatches(model, row, account)) return false;
                    if ((min != null || max != null) && !amountMatches(model, row, min, max)) return false;
                    if (dates != null && !dateMatches(model, row, dates)) return false;
                    return true;
                }
            });
            visible += table.getRowCount();
        }
        resultLabel.setText("\u2022  Matching rows: " + visible);
    }

    private void resetFilters() {
        searchField.setText("");
        selectedAccount = null;
        datePreset = null;
        customFrom = null;
        customTo = null;
        minAmount = null;
        maxAmount = null;
        accountPicker.setText(ALL_ACCOUNTS);
        datePicker.setText(SELECT_DATE);
        amountPicker.setText(amountText());
        applyFilters();
        resultLabel.setText(" ");
    }

    /** Re-applies the current filters (useful after a screen reloads data). */
    public void refreshSuggestionsAndApply() {
        applyFilters();
    }

    // ---- Search suggestions ----

    /** Builds the dropdown: suggestions appear while typing in the Search field. */
    private void setupSuggestions() {
        suggestionPopup.setFocusable(false);   // keep typing focus in the Search field
        suggestionPopup.setBorder(BorderFactory.createLineBorder(cBorder(), 1, true));
        suggestionPopup.setBackground(cCard());
        suggestionPopup.setOpaque(true);

        suggestionList.setFixedCellHeight(SUGGESTION_ROW_HEIGHT);
        suggestionList.setFont(VALUE_FONT);
        suggestionList.setBackground(cCard());
        suggestionList.setForeground(cText());
        suggestionList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        suggestionList.setCellRenderer(new DefaultListCellRenderer() {
            @Override public Component getListCellRendererComponent(
                    JList<?> list, Object value, int index,
                    boolean selected, boolean focus) {
                super.getListCellRendererComponent(list, value, index, false, false);
                setBorder(new EmptyBorder(0, 14, 0, 14));
                setOpaque(true);
                setBackground(selected ? cTint() : cCard());
                setForeground(cText());
                return this;
            }
        });
        suggestionList.addMouseMotionListener(new MouseMotionAdapter() {
            @Override public void mouseMoved(MouseEvent e) {
                suggestionList.setSelectedIndex(suggestionList.locationToIndex(e.getPoint()));
            }
        });
        suggestionList.addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) {
                int index = suggestionList.locationToIndex(e.getPoint());
                if (index >= 0) acceptSuggestion(suggestionModel.get(index));
            }
        });

        JScrollPane scroll = new JScrollPane(suggestionList);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.getViewport().setBackground(cCard());
        suggestionPopup.add(scroll);

        searchField.getDocument().addDocumentListener(new DocumentListener() {
            @Override public void insertUpdate(DocumentEvent e) { updateSuggestions(); }
            @Override public void removeUpdate(DocumentEvent e) { updateSuggestions(); }
            @Override public void changedUpdate(DocumentEvent e) { updateSuggestions(); }
        });
        searchField.addKeyListener(new KeyAdapter() {
            @Override public void keyPressed(KeyEvent e) { handleSuggestionKey(e); }
        });
    }

    /** Refreshes the list for the text typed so far. */
    private void updateSuggestions() {
        if (suppressSuggestions) return;

        String typed = searchField.getText().trim();
        suggestionModel.clear();
        if (!typed.isEmpty()) {
            for (String name : suggestionsFor(typed)) {
                suggestionModel.addElement(name);
            }
        }

        if (suggestionModel.isEmpty() || !searchField.isShowing()) {
            hideSuggestions();
            return;
        }

        int rows = suggestionModel.size();
        suggestionList.clearSelection();
        suggestionList.setVisibleRowCount(rows);
        suggestionPopup.setPopupSize(Math.max(searchField.getWidth(), 240),
            rows * SUGGESTION_ROW_HEIGHT + 2);
        suggestionPopup.show(searchField, 0, searchField.getHeight() + 4);
    }

    /** Account names and descriptions shown on screen that match the typed text. */
    private List<String> suggestionsFor(String typed) {
        String needle = typed.toLowerCase(Locale.ROOT);
        List<String> prefixMatches = new ArrayList<>();
        List<String> innerMatches = new ArrayList<>();

        for (String name : collectNames(descriptionColumns, accountColumns)) {
            String lower = name.toLowerCase(Locale.ROOT);
            if (lower.equals(needle)) continue;      // already typed in full
            if (lower.startsWith(needle)) {
                prefixMatches.add(name);
            } else if (lower.contains(needle)) {
                innerMatches.add(name);
            }
        }

        prefixMatches.addAll(innerMatches);
        return prefixMatches.size() > MAX_SUGGESTIONS
            ? prefixMatches.subList(0, MAX_SUGGESTIONS)
            : prefixMatches;
    }

    /** Arrow keys move through the list, Enter picks, Escape closes. */
    private void handleSuggestionKey(KeyEvent event) {
        if (!suggestionPopup.isVisible()) return;

        int key = event.getKeyCode();
        if (key == KeyEvent.VK_DOWN) {
            moveSuggestion(1);
        } else if (key == KeyEvent.VK_UP) {
            moveSuggestion(-1);
        } else if (key == KeyEvent.VK_ESCAPE) {
            hideSuggestions();
        } else if (key == KeyEvent.VK_ENTER) {
            String chosen = suggestionList.getSelectedValue();
            if (chosen == null) {
                hideSuggestions();   // plain Enter applies the typed text
                return;
            }
            acceptSuggestion(chosen);
        } else {
            return;
        }
        event.consume();
    }

    private void moveSuggestion(int step) {
        int size = suggestionModel.size();
        if (size == 0) return;

        int current = suggestionList.getSelectedIndex();
        int next = current < 0
            ? (step > 0 ? 0 : size - 1)
            : Math.floorMod(current + step, size);
        suggestionList.setSelectedIndex(next);
        suggestionList.ensureIndexIsVisible(next);
    }

    private void acceptSuggestion(String value) {
        suppressSuggestions = true;
        try {
            searchField.setText(value);
        } finally {
            suppressSuggestions = false;
        }
        hideSuggestions();
        applyFilters();
    }

    private void hideSuggestions() {
        suggestionPopup.setVisible(false);
    }

    // ---- Rounded text field with a placeholder ----

    private static final class HintField extends JTextField {
        private final String hint;

        HintField(String hint) {
            this.hint = hint;
            setOpaque(false);
            setFont(VALUE_FONT);
            setBorder(new EmptyBorder(0, 14, 0, 14));
            setPreferredSize(new Dimension(120, 42));
            addFocusListener(new FocusAdapter() {
                @Override public void focusGained(FocusEvent e) { repaint(); }
                @Override public void focusLost(FocusEvent e) { repaint(); }
            });
            applyColors();
        }

        void applyColors() {
            setForeground(cText());
            setCaretColor(cText());
            setSelectionColor(cBlue());
            setSelectedTextColor(Color.WHITE);
            repaint();
        }

        @Override protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(cField());
            g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 14, 14);
            g2.setColor(isFocusOwner() ? cBlue() : cBorder());
            g2.setStroke(new BasicStroke(isFocusOwner() ? 1.6f : 1f));
            g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 14, 14);
            g2.dispose();
            setForeground(cText());
            setCaretColor(cText());
            super.paintComponent(g);
            if (getText().isEmpty()) {
                Graphics2D h = (Graphics2D) g.create();
                h.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                    RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
                h.setColor(cMuted());
                h.setFont(getFont());
                FontMetrics fm = h.getFontMetrics();
                h.drawString(hint, getInsets().left,
                    (getHeight() - fm.getHeight()) / 2 + fm.getAscent());
                h.dispose();
            }
        }
    }

    // ---- Dropdown-style field that opens a popup ----

    private final class Picker extends JPanel {
        private final JLabel text = new JLabel();
        private final BiFunction<JPopupMenu, Integer, JComponent> content;
        private boolean hover;

        Picker(Glyph.Kind leftIcon, String initial,
               BiFunction<JPopupMenu, Integer, JComponent> content) {
            this.content = content;
            setOpaque(false);
            setLayout(new BorderLayout(10, 0));
            setBorder(new EmptyBorder(0, 14, 0, 12));
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

            if (leftIcon != null) {
                add(new JLabel(new Glyph(leftIcon, 18, AccountingFilterBar::cText)),
                    BorderLayout.WEST);
            }
            text.setFont(VALUE_FONT);
            text.setText(initial);
            add(text, BorderLayout.CENTER);
            add(new JLabel(new Glyph(Glyph.Kind.CHEVRON, 16, AccountingFilterBar::cText)),
                BorderLayout.EAST);

            addMouseListener(new MouseAdapter() {
                @Override public void mouseEntered(MouseEvent e) { hover = true; repaint(); }
                @Override public void mouseExited(MouseEvent e) { hover = false; repaint(); }
                @Override public void mouseClicked(MouseEvent e) { showPopup(); }
            });
            applyColors();
        }

        void setText(String value) {
            text.setText(value);
        }

        void applyColors() {
            text.setForeground(cText());
            repaint();
        }

        private void showPopup() {
            JPopupMenu popup = newPopup();
            popup.add(content.apply(popup, getWidth()));
            popup.show(this, 0, getHeight() + 4);
        }

        @Override protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(cField());
            g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 14, 14);
            g2.setColor(hover ? cBlue() : cBorder());
            g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 14, 14);
            g2.dispose();
            text.setForeground(cText());
            super.paintComponent(g);
        }
    }

    // ---- Painted icons (no icon font required) ----

    private static final class Glyph implements Icon {
        enum Kind { FUNNEL, CALENDAR, SEARCH, RESET, CHEVRON, CLOSE }

        private final Kind kind;
        private final int size;
        private final Supplier<Color> color;

        Glyph(Kind kind, int size, Supplier<Color> color) {
            this.kind = kind;
            this.size = size;
            this.color = color;
        }

        public int getIconWidth() { return size; }
        public int getIconHeight() { return size; }

        public void paintIcon(Component c, Graphics graphics, int x, int y) {
            Graphics2D g = (Graphics2D) graphics.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
            g.translate(x, y);
            double s = size / 18.0;
            g.scale(s, s);
            g.setColor(color.get());
            g.setStroke(new BasicStroke(1.7f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            switch (kind) {
                case FUNNEL: {
                    Path2D p = new Path2D.Double();
                    p.moveTo(2.5, 3.5);
                    p.lineTo(15.5, 3.5);
                    p.lineTo(10.5, 9.5);
                    p.lineTo(10.5, 15);
                    p.lineTo(7.5, 13.5);
                    p.lineTo(7.5, 9.5);
                    p.closePath();
                    g.draw(p);
                    break;
                }
                case CALENDAR:
                    g.draw(new RoundRectangle2D.Double(2, 3.5, 14, 12.5, 3, 3));
                    g.draw(new Line2D.Double(2, 7.5, 16, 7.5));
                    g.draw(new Line2D.Double(6, 1.5, 6, 5));
                    g.draw(new Line2D.Double(12, 1.5, 12, 5));
                    break;
                case SEARCH:
                    g.draw(new Ellipse2D.Double(2.5, 2.5, 10, 10));
                    g.draw(new Line2D.Double(10.5, 10.5, 15.5, 15.5));
                    break;
                case RESET: {
                    g.draw(new Arc2D.Double(3, 3, 12, 12, 130, 280, Arc2D.OPEN));
                    double theta = Math.toRadians(410);
                    double px = 9 + 6 * Math.cos(theta);
                    double py = 9 - 6 * Math.sin(theta);
                    double tx = -Math.sin(theta);
                    double ty = -Math.cos(theta);
                    double nx = -ty;
                    double ny = tx;
                    Path2D head = new Path2D.Double();
                    head.moveTo(px + tx * 3.2, py + ty * 3.2);
                    head.lineTo(px + nx * 3, py + ny * 3);
                    head.lineTo(px - nx * 3, py - ny * 3);
                    head.closePath();
                    g.fill(head);
                    break;
                }
                case CHEVRON: {
                    Path2D p = new Path2D.Double();
                    p.moveTo(4.5, 7);
                    p.lineTo(9, 11.5);
                    p.lineTo(13.5, 7);
                    g.draw(p);
                    break;
                }
                case CLOSE:
                    g.draw(new Line2D.Double(4, 4, 14, 14));
                    g.draw(new Line2D.Double(14, 4, 4, 14));
                    break;
            }
            g.dispose();
        }
    }
}
