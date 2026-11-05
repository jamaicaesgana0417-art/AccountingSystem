import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.sql.SQLException;
import java.util.Locale;
import java.util.function.Consumer;

public class App {

    private static final Color NAVY = new Color(22, 56, 102);
    private static final Color NAVY_HOVER = new Color(32, 72, 128);
    private static final Color PAGE_BG = new Color(240, 245, 252);
    private static final Color CARD_BORDER = new Color(220, 228, 240);
    private static final Color CARD_HOVER = new Color(245, 249, 255);
    private static final Color ICON_BOX = new Color(226, 236, 250);
    private static final Color MUTED = new Color(110, 125, 145);
    private static final Color TEXT = new Color(45, 60, 85);
    private static final Color SCRIM = new Color(225, 232, 243, 175);

    private static final int ICON_PLUS = 0;
    private static final int ICON_LIST = 1;
    private static final int ICON_TABLE = 2;
    private static final int ICON_USER = 3;

    private static final int NAV_HOME = 0;
    private static final int NAV_SWAP = 1;
    private static final int NAV_BANK = 2;
    private static final int NAV_JOURNAL = 3;
    private static final int NAV_LEDGER = 4;
    private static final int NAV_SHEET = 5;
    private static final int NAV_LOGOUT = 7;

    private static final int HEADER_HEIGHT = 46;
    private static final int DRAWER_WIDTH = 300;

    public static void main(String[] args) {
        try {
            Database.initialize();
            AuthService.initialize();

            SwingUtilities.invokeLater(() -> //mo use kog lamda for short syntax and it's use para sa pag avoid sa thread issues kay ang swing kay single thread lang siya
                new LoginFrame().setVisible(true)
            );

        } catch (SQLException exception) {
            exception.printStackTrace();

            SwingUtilities.invokeLater(() ->
                JOptionPane.showMessageDialog(
                    null,
                    "Cannot initialize the database:\n"
                        + exception.getMessage(),
                    "Database Error",
                    JOptionPane.ERROR_MESSAGE
                )
            );
        }
    }

    // Called by LoginFrame after successful authentication.
    public static void openAccountingWindow(String username) {
        JFrame window = new JFrame(
            "Accounting System - " + username
        );

        window.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        window.setSize(1200, 700);
        window.setMinimumSize(new Dimension(800, 500));
        window.setLocationRelativeTo(null);

        // ----- Content (same panels as before) -----
        CardLayout cardLayout = new CardLayout();
        JPanel content = new JPanel(cardLayout);

        AccountsPanel accountsPanel = new AccountsPanel();
        NewTransactionPanel transactionPanel =
            new NewTransactionPanel();
        TransactionsPanel transactionsPanel =
            new TransactionsPanel();
        JournalPanel journalPanel = new JournalPanel();
        LedgerPanel ledgerPanel = new LedgerPanel();
        BalanceSheetPanel balanceSheetPanel =
            new BalanceSheetPanel();

        // ----- Navigation: same refresh logic, shared by menu and cards -----
        Consumer<String> showScreen = screenName -> {
            switch (screenName) {
                case "New Transaction":
                    transactionPanel.refreshAccounts();
                    break;

                case "Transactions":
                    transactionsPanel.refreshTransactions();
                    break;

                case "General Journal":
                    journalPanel.refreshJournal();
                    break;

                case "General Ledger":
                    ledgerPanel.refreshAccounts();
                    break;

                case "Balance Sheet":
                    balanceSheetPanel.refreshBalanceSheet();
                    break;

                default:
                    break;
            }

            cardLayout.show(content, screenName);
        };

        // Same as showScreen, but also understands "Home".
        Consumer<String> navigate = screenName -> {
            if ("Home".equals(screenName)) {
                cardLayout.show(content, "Home");
            } else {
                showScreen.accept(screenName);
            }
        };

        content.add(buildHomePage(showScreen), "Home");
        content.add(transactionPanel, "New Transaction");
        content.add(transactionsPanel, "Transactions");
        content.add(accountsPanel, "Accounts");
        content.add(journalPanel, "General Journal");
        content.add(ledgerPanel, "General Ledger");
        content.add(balanceSheetPanel, "Balance Sheet");

        // ----- Header: logo + title on the left, menu on the right -----
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(NAVY);
        header.setPreferredSize(new Dimension(0, HEADER_HEIGHT));
        header.setBorder(
            BorderFactory.createEmptyBorder(0, 16, 0, 16)
        );

        JLabel title = new JLabel("ACCOUNTING SYSTEM");
        title.setForeground(Color.WHITE);
        title.setFont(new Font("Inter", Font.BOLD, 13));

        JPanel brand = new JPanel(new GridBagLayout());
        brand.setOpaque(false);

        GridBagConstraints gap = new GridBagConstraints();
        gap.insets = new Insets(0, 0, 0, 10);
        brand.add(new LogoMark(), gap);
        brand.add(title, new GridBagConstraints());

        JButton menuButton = new HamburgerButton();

        JPanel menuArea = new JPanel(new GridBagLayout());
        menuArea.setOpaque(false);
        menuArea.add(menuButton);

        header.add(brand, BorderLayout.WEST);
        header.add(menuArea, BorderLayout.EAST);

        // ----- Side menu (slides over the page from the right) -----
        DrawerOverlay overlay = new DrawerOverlay();

        Runnable logout = () -> {
            int answer = JOptionPane.showConfirmDialog(
                window,
                "Log out of the application?",
                "Confirm Logout",
                JOptionPane.YES_NO_OPTION
            );

            if (answer == JOptionPane.YES_OPTION) {
                Session.logout();
                new LoginFrame().setVisible(true);
                window.dispose();
            }
        };

        overlay.setDrawer(
            buildDrawer(username, navigate, overlay, window, logout)
        );

        window.setGlassPane(overlay);

        menuButton.addActionListener(event -> overlay.setVisible(true));

        // Esc closes the side menu.
        window.getRootPane()
            .getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
            .put(KeyStroke.getKeyStroke("ESCAPE"), "closeDrawer");

        window.getRootPane()
            .getActionMap()
            .put("closeDrawer", new AbstractAction() {
                @Override
                public void actionPerformed(ActionEvent event) {
                    overlay.setVisible(false);
                }
            });

        window.add(header, BorderLayout.NORTH);
        window.add(content, BorderLayout.CENTER);

        cardLayout.show(content, "Home");
        window.setVisible(true);
    }

    // ---------------------------------------------------------------
    // Side menu
    // ---------------------------------------------------------------

    private static JComponent buildDrawer(
        String username,
        Consumer<String> navigate,
        DrawerOverlay overlay,
        JFrame window,
        Runnable logout
    ) {
        JPanel drawer = new JPanel(new BorderLayout());
        drawer.setBackground(Color.WHITE);
        drawer.setBorder(
            BorderFactory.createMatteBorder(0, 1, 0, 0, CARD_BORDER)
        );

        // ----- Top: profile, New Transaction button, navigation -----
        JPanel top = new JPanel(new GridBagLayout());
        top.setOpaque(false);
        top.setBorder(
            BorderFactory.createEmptyBorder(18, 18, 12, 18)
        );

        JLabel nameLabel = new JLabel(
            username.toUpperCase(Locale.ROOT)
        );
        nameLabel.setFont(new Font("Inter", Font.BOLD, 11));
        nameLabel.setForeground(NAVY);

        JLabel roleLabel = new JLabel("ACCOUNTING SYSTEM");
        roleLabel.setFont(new Font("Inter", Font.PLAIN, 9));
        roleLabel.setForeground(MUTED);

        JPanel names = new JPanel();
        names.setOpaque(false);
        names.setLayout(new BoxLayout(names, BoxLayout.Y_AXIS));
        names.add(nameLabel);
        names.add(Box.createVerticalStrut(2));
        names.add(roleLabel);

        JPanel namesArea = new JPanel(new GridBagLayout());
        namesArea.setOpaque(false);
        namesArea.add(names);

        CloseButton closeButton = new CloseButton();
        closeButton.addActionListener(event -> overlay.setVisible(false));

        JPanel closeArea = new JPanel(new GridBagLayout());
        closeArea.setOpaque(false);
        closeArea.add(closeButton);

        JPanel profile = new JPanel(new BorderLayout(10, 0));
        profile.setOpaque(false);
        profile.add(new IconBox(ICON_USER, ICON_BOX, NAVY), BorderLayout.WEST);
        profile.add(namesArea, BorderLayout.CENTER);
        profile.add(closeArea, BorderLayout.EAST);

        gridRow(top, profile, 0, new Insets(0, 0, 18, 0));

        // "+ New Transaction" button
        RoundedPanel newButton = new RoundedPanel(
            new GridBagLayout(), NAVY, null, 12
        );
        newButton.setPreferredSize(new Dimension(100, 42));
        newButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        JLabel newButtonText = new JLabel("+   New Transaction");
        newButtonText.setFont(new Font("Inter", Font.BOLD, 12));
        newButtonText.setForeground(Color.WHITE);
        newButton.add(newButtonText);

        newButton.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent event) {
                overlay.setVisible(false);
                navigate.accept("New Transaction");
            }

            @Override
            public void mouseEntered(MouseEvent event) {
                newButton.setFill(NAVY_HOVER);
            }

            @Override
            public void mouseExited(MouseEvent event) {
                newButton.setFill(NAVY);
            }
        });

        gridRow(top, newButton, 1, new Insets(0, 0, 14, 0));

        String[][] entries = {
            {"Home", "Home"},
            {"Transactions", "Transactions"},
            {"Accounts", "Accounts"},
            {"General Journal", "General Journal"},
            {"General Ledger", "General Ledger"},
            {"Balance Sheet", "Balance Sheet"}
        };

        int[] icons = {
            NAV_HOME, NAV_SWAP, NAV_BANK,
            NAV_JOURNAL, NAV_LEDGER, NAV_SHEET
        };

        for (int i = 0; i < entries.length; i++) {
            String screenName = entries[i][1];

            NavRow row = new NavRow(icons[i], entries[i][0], () -> {
                overlay.setVisible(false);
                navigate.accept(screenName);
            });

            gridRow(top, row, 2 + i, new Insets(0, 0, 2, 0));
        }

        // ----- Bottom: account settings and log out -----
        JPanel bottom = new JPanel(new GridBagLayout());
        bottom.setOpaque(false);
        bottom.setBorder(
            BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(
                    1, 0, 0, 0, CARD_BORDER
                ),
                BorderFactory.createEmptyBorder(10, 18, 14, 18)
            )
        );

        NavRow logoutRow = new NavRow(NAV_LOGOUT, "Log out", () -> {
            overlay.setVisible(false);
            logout.run();
        });

        gridRow(bottom, logoutRow, 0, new Insets(0, 0, 0, 0));

        drawer.add(top, BorderLayout.NORTH);
        drawer.add(bottom, BorderLayout.SOUTH);

        return drawer;
    }

    private static void gridRow(
        JPanel panel, JComponent component, int row, Insets insets
    ) {
        GridBagConstraints c = new GridBagConstraints();
        c.gridx = 0;
        c.gridy = row;
        c.weightx = 1;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.anchor = GridBagConstraints.NORTHWEST;
        c.insets = insets;
        panel.add(component, c);
    }

    // ---------------------------------------------------------------
    // Home page
    // ---------------------------------------------------------------

    private static JComponent buildHomePage(Consumer<String> showScreen) {
        HomePage page = new HomePage();
        page.setBackground(PAGE_BG);
        page.setLayout(new GridBagLayout());
        page.setBorder(
            BorderFactory.createEmptyBorder(28, 30, 20, 30)
        );

        GridBagConstraints c = new GridBagConstraints();
        c.gridx = 0;
        c.weightx = 1;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.anchor = GridBagConstraints.NORTHWEST;

        JLabel welcome = new JLabel(
            "WELCOME TO YOUR ACCOUNTING WORKSPACE"
        );
        welcome.setFont(new Font("Inter", Font.BOLD, 20));
        welcome.setForeground(NAVY);

        JLabel subtitle = new JLabel(
            "Start a transaction or choose a section to manage your books."
        );
        subtitle.setFont(new Font("Inter", Font.PLAIN, 12));
        subtitle.setForeground(MUTED);

        c.gridy = 0;
        page.add(welcome, c);

        c.gridy = 1;
        c.insets = new Insets(4, 0, 0, 0);
        page.add(subtitle, c);

        c.gridy = 2;
        c.insets = new Insets(22, 0, 0, 0);
        page.add(buildHero(showScreen), c);

        JLabel toolsHeading = new JLabel("Your accounting tools");
        toolsHeading.setFont(new Font("Inter", Font.BOLD, 14));
        toolsHeading.setForeground(NAVY);

        c.gridy = 3;
        c.insets = new Insets(26, 0, 10, 0);
        page.add(toolsHeading, c);

        JPanel grid = new JPanel(new GridLayout(2, 3, 16, 16));
        grid.setOpaque(false);

        grid.add(buildCard(
            "New Transaction",
            "Record a debit and credit entry.",
            "New Transaction", ICON_PLUS, true, showScreen
        ));
        grid.add(buildCard(
            "Transactions",
            "Search and export posted transactions.",
            "Transactions", ICON_LIST, false, showScreen
        ));
        grid.add(buildCard(
            "Chart of Accounts",
            "Create and organize your accounts.",
            "Accounts", ICON_TABLE, false, showScreen
        ));
        grid.add(buildCard(
            "General Journal",
            "Review entries in chronological order.",
            "General Journal", ICON_LIST, false, showScreen
        ));
        grid.add(buildCard(
            "General Ledger",
            "View activity and running balances.",
            "General Ledger", ICON_TABLE, false, showScreen
        ));
        grid.add(buildCard(
            "Balance Sheet",
            "Review assets, liabilities, and equity.",
            "Balance Sheet", ICON_TABLE, false, showScreen
        ));

        c.gridy = 4;
        c.insets = new Insets(0, 0, 0, 0);
        page.add(grid, c);

        JLabel footer = new JLabel(
            "Use the menu in the upper-right corner to switch sections at any time."
        );
        footer.setFont(new Font("Inter", Font.PLAIN, 11));
        footer.setForeground(MUTED);

        c.gridy = 5;
        c.insets = new Insets(18, 0, 0, 0);
        page.add(footer, c);

        // Pushes everything to the top when the window is tall.
        c.gridy = 6;
        c.weighty = 1;
        c.insets = new Insets(0, 0, 0, 0);
        c.fill = GridBagConstraints.BOTH;
        JPanel filler = new JPanel();
        filler.setOpaque(false);
        page.add(filler, c);

        JScrollPane scroll = new JScrollPane(page);
        scroll.setBorder(null);
        scroll.getViewport().setBackground(PAGE_BG);
        scroll.getVerticalScrollBar().setUnitIncrement(16);

        return scroll;
    }

    private static JComponent buildHero(Consumer<String> showScreen) {
        RoundedPanel hero = new RoundedPanel(
            new BorderLayout(), NAVY, null, 20
        );
        hero.setBorder(
            BorderFactory.createEmptyBorder(24, 28, 24, 28)
        );

        JLabel heading = new JLabel("Ready to record something new?");
        heading.setFont(new Font("Inter", Font.BOLD, 18));
        heading.setForeground(Color.WHITE);

        JLabel text = new JLabel(
            "Create a balanced entry and keep your records up to date."
        );
        text.setFont(new Font("Inter", Font.PLAIN, 12));
        text.setForeground(new Color(205, 218, 238));

        JPanel texts = new JPanel();
        texts.setOpaque(false);
        texts.setLayout(new BoxLayout(texts, BoxLayout.Y_AXIS));
        texts.add(heading);
        texts.add(Box.createVerticalStrut(6));
        texts.add(text);

        RoundedPanel button = new RoundedPanel(
            new GridBagLayout(), Color.WHITE, null, 12
        );
        button.setBorder(
            BorderFactory.createEmptyBorder(11, 24, 11, 24)
        );
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        JLabel buttonText = new JLabel("+   New Transaction");
        buttonText.setFont(new Font("Inter", Font.BOLD, 12));
        buttonText.setForeground(NAVY);
        button.add(buttonText);

        button.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent event) {
                showScreen.accept("New Transaction");
            }

            @Override
            public void mouseEntered(MouseEvent event) {
                button.setFill(CARD_HOVER);
            }

            @Override
            public void mouseExited(MouseEvent event) {
                button.setFill(Color.WHITE);
            }
        });

        JPanel buttonArea = new JPanel(new GridBagLayout());
        buttonArea.setOpaque(false);
        buttonArea.add(button);

        hero.add(texts, BorderLayout.CENTER);
        hero.add(buttonArea, BorderLayout.EAST);

        return hero;
    }

    private static JComponent buildCard(
        String titleText,
        String descriptionText,
        String screenName,
        int iconType,
        boolean dark,
        Consumer<String> showScreen
    ) {
        Color normal = dark ? NAVY : Color.WHITE;
        Color hover = dark ? NAVY_HOVER : CARD_HOVER;

        RoundedPanel card = new RoundedPanel(
            new BorderLayout(), normal, dark ? null : CARD_BORDER, 16
        );
        card.setBorder(
            BorderFactory.createEmptyBorder(14, 16, 14, 16)
        );
        card.setPreferredSize(new Dimension(200, 104));
        card.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        IconBox icon = new IconBox(
            iconType,
            dark ? Color.WHITE : ICON_BOX,
            NAVY
        );

        JLabel arrow = new JLabel("\u2192");
        arrow.setFont(new Font("Inter", Font.PLAIN, 15));
        arrow.setForeground(dark ? Color.WHITE : NAVY);

        JPanel topRow = new JPanel(new BorderLayout());
        topRow.setOpaque(false);
        topRow.add(icon, BorderLayout.WEST);
        topRow.add(arrow, BorderLayout.EAST);

        JLabel title = new JLabel(titleText);
        title.setFont(new Font("Inter", Font.BOLD, 13));
        title.setForeground(dark ? Color.WHITE : NAVY);

        JLabel description = new JLabel(descriptionText);
        description.setFont(new Font("Inter", Font.PLAIN, 10));
        description.setForeground(
            dark ? new Color(205, 218, 238) : MUTED
        );

        JPanel texts = new JPanel();
        texts.setOpaque(false);
        texts.setLayout(new BoxLayout(texts, BoxLayout.Y_AXIS));
        texts.add(title);
        texts.add(Box.createVerticalStrut(3));
        texts.add(description);

        card.add(topRow, BorderLayout.NORTH);
        card.add(texts, BorderLayout.SOUTH);

        card.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent event) {
                showScreen.accept(screenName);
            }

            @Override
            public void mouseEntered(MouseEvent event) {
                card.setFill(hover);
            }

            @Override
            public void mouseExited(MouseEvent event) {
                card.setFill(normal);
            }
        });

        return card;
    }

    // ---------------------------------------------------------------
    // Small custom-painted components
    // ---------------------------------------------------------------

    // Covers the page below the header and holds the side menu.
    private static class DrawerOverlay extends JPanel {
        private JComponent drawer;

        DrawerOverlay() {
            super(null);
            setOpaque(false);

            addMouseListener(new MouseAdapter() {
                @Override
                public void mousePressed(MouseEvent event) {
                    // Clicking outside the side menu closes it.
                    if (drawer == null
                            || !drawer.getBounds().contains(event.getPoint())) {
                        setVisible(false);
                    }
                }
            });

            // Keeps mouse movement from reaching the page underneath.
            addMouseMotionListener(new MouseAdapter() { });
        }

        void setDrawer(JComponent drawer) {
            this.drawer = drawer;
            add(drawer);
        }

        @Override
        public void doLayout() {
            if (drawer != null) {
                drawer.setBounds(
                    getWidth() - DRAWER_WIDTH,
                    HEADER_HEIGHT,
                    DRAWER_WIDTH,
                    Math.max(0, getHeight() - HEADER_HEIGHT)
                );
            }
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            graphics.setColor(SCRIM);
            graphics.fillRect(
                0,
                HEADER_HEIGHT,
                getWidth(),
                Math.max(0, getHeight() - HEADER_HEIGHT)
            );
        }
    }

    // One clickable row in the side menu.
    private static class NavRow extends JPanel {
        private boolean hover = false;

        NavRow(int iconType, String text, Runnable action) {
            super(new BorderLayout(12, 0));
            setOpaque(false);
            setBorder(BorderFactory.createEmptyBorder(0, 10, 0, 10));
            setPreferredSize(new Dimension(100, 36));
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

            JLabel label = new JLabel(text);
            label.setFont(new Font("Inter", Font.PLAIN, 12));
            label.setForeground(TEXT);

            add(new NavIcon(iconType), BorderLayout.WEST);
            add(label, BorderLayout.CENTER);

            addMouseListener(new MouseAdapter() {
                @Override
                public void mouseEntered(MouseEvent event) {
                    hover = true;
                    repaint();
                }

                @Override
                public void mouseExited(MouseEvent event) {
                    hover = false;
                    repaint();
                }

                @Override
                public void mouseClicked(MouseEvent event) {
                    hover = false;
                    action.run();
                }
            });
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            if (hover) {
                Graphics2D g = (Graphics2D) graphics.create();
                g.setRenderingHint(
                    RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON
                );
                g.setColor(PAGE_BG);
                g.fillRoundRect(0, 0, getWidth(), getHeight(), 10, 10);
                g.dispose();
            }

            super.paintComponent(graphics);
        }
    }

    private static class NavIcon extends JComponent {
        private final int type;

        NavIcon(int type) {
            this.type = type;
            setPreferredSize(new Dimension(18, 18));
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
                    1.4f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND
                )
            );

            // Draw on a 16x16 grid centered in the component.
            g.translate(
                (getWidth() - 16) / 2,
                (getHeight() - 16) / 2
            );

            switch (type) {
                case NAV_HOME:
                    g.drawPolyline(
                        new int[] {1, 8, 15},
                        new int[] {8, 2, 8},
                        3
                    );
                    g.drawRect(3, 8, 10, 6);
                    break;

                case NAV_SWAP:
                    g.drawLine(2, 5, 14, 5);
                    g.drawLine(11, 2, 14, 5);
                    g.drawLine(11, 8, 14, 5);
                    g.drawLine(2, 11, 14, 11);
                    g.drawLine(5, 8, 2, 11);
                    g.drawLine(5, 14, 2, 11);
                    break;

                case NAV_BANK:
                    g.drawPolyline(
                        new int[] {1, 8, 15, 1},
                        new int[] {6, 1, 6, 6},
                        4
                    );
                    g.drawLine(4, 8, 4, 12);
                    g.drawLine(8, 8, 8, 12);
                    g.drawLine(12, 8, 12, 12);
                    g.drawLine(1, 14, 15, 14);
                    break;

                case NAV_JOURNAL:
                    g.drawRoundRect(3, 1, 10, 14, 2, 2);
                    g.drawLine(6, 5, 10, 5);
                    g.drawLine(6, 8, 10, 8);
                    g.drawLine(6, 11, 10, 11);
                    break;

                case NAV_LEDGER:
                    g.drawRoundRect(1, 2, 14, 12, 2, 2);
                    g.drawLine(8, 2, 8, 14);
                    break;

                case NAV_SHEET:
                    g.drawRoundRect(3, 1, 10, 14, 2, 2);
                    g.drawRect(5, 4, 3, 3);
                    g.drawLine(5, 10, 11, 10);
                    g.drawLine(5, 12, 11, 12);
                    break;

                case NAV_LOGOUT:
                    g.drawPolyline(
                        new int[] {6, 2, 2, 6},
                        new int[] {2, 2, 14, 14},
                        4
                    );
                    g.drawLine(6, 8, 14, 8);
                    g.drawLine(11, 5, 14, 8);
                    g.drawLine(11, 11, 14, 8);
                    break;

                default:
                    break;
            }

            g.dispose();
        }
    }

    private static class CloseButton extends JButton {
        CloseButton() {
            setPreferredSize(new Dimension(24, 24));
            setContentAreaFilled(false);
            setBorderPainted(false);
            setFocusPainted(false);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            Graphics2D g = (Graphics2D) graphics.create();
            g.setRenderingHint(
                RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_ON
            );

            g.setColor(getModel().isRollover() ? NAVY : MUTED);
            g.setStroke(
                new BasicStroke(
                    1.6f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND
                )
            );

            int cx = getWidth() / 2;
            int cy = getHeight() / 2;

            g.drawLine(cx - 4, cy - 4, cx + 4, cy + 4);
            g.drawLine(cx - 4, cy + 4, cx + 4, cy - 4);

            g.dispose();
        }
    }

    // Home page that stretches to the window width inside the scroll pane.
    private static class HomePage extends JPanel implements Scrollable {

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
        private Color fill;
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

        void setFill(Color fill) {
            this.fill = fill;
            repaint();
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

    private static class IconBox extends JComponent {
        private final int type;
        private final Color boxColor;
        private final Color glyphColor;

        IconBox(int type, Color boxColor, Color glyphColor) {
            this.type = type;
            this.boxColor = boxColor;
            this.glyphColor = glyphColor;
            setPreferredSize(new Dimension(30, 30));
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            Graphics2D g = (Graphics2D) graphics.create();
            g.setRenderingHint(
                RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_ON
            );

            g.setColor(boxColor);
            g.fillRoundRect(0, 0, getWidth(), getHeight(), 9, 9);

            g.setColor(glyphColor);
            int cx = getWidth() / 2;
            int cy = getHeight() / 2;

            if (type == ICON_PLUS) {
                g.setStroke(new BasicStroke(2f));
                g.drawLine(cx - 6, cy, cx + 6, cy);
                g.drawLine(cx, cy - 6, cx, cy + 6);

            } else if (type == ICON_LIST) {
                g.setStroke(new BasicStroke(1.5f));

                for (int i = -1; i <= 1; i++) {
                    int y = cy + i * 5;
                    g.fillOval(cx - 8, y - 1, 3, 3);
                    g.drawLine(cx - 2, y, cx + 7, y);
                }

            } else if (type == ICON_USER) {
                g.setStroke(new BasicStroke(1.5f));
                g.drawOval(cx - 3, cy - 8, 6, 6);
                g.drawArc(cx - 7, cy - 1, 14, 14, 0, 180);

            } else {
                g.setStroke(new BasicStroke(1.5f));
                g.drawRoundRect(cx - 7, cy - 7, 14, 14, 3, 3);
                g.drawLine(cx - 7, cy, cx + 7, cy);
                g.drawLine(cx, cy - 7, cx, cy + 7);
            }

            g.dispose();
        }
    }

    private static class LogoMark extends JComponent {
        LogoMark() {
            setPreferredSize(new Dimension(18, 18));
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            Graphics2D g = (Graphics2D) graphics.create();
            g.setRenderingHint(
                RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_ON
            );

            g.setColor(Color.WHITE);
            g.setStroke(new BasicStroke(2f));
            g.drawRoundRect(1, 1, 15, 15, 4, 4);
            g.fillRect(6, 6, 6, 6);

            g.dispose();
        }
    }

    private static class HamburgerButton extends JButton {
        HamburgerButton() {
            setPreferredSize(new Dimension(34, 28));
            setContentAreaFilled(false);
            setBorderPainted(false);
            setFocusPainted(false);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            Graphics2D g = (Graphics2D) graphics.create();
            g.setRenderingHint(
                RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_ON
            );

            g.setColor(Color.WHITE);
            g.setStroke(
                new BasicStroke(
                    2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND
                )
            );

            int cy = getHeight() / 2;

            for (int i = -1; i <= 1; i++) {
                int y = cy + i * 6;
                g.drawLine(8, y, getWidth() - 8, y);
            }

            g.dispose();
        }
    }
}