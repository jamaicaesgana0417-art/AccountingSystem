import javax.swing.*;
import javax.swing.border.EmptyBorder;

import java.awt.*;
import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Consumer;

public class App {

    private static final Color NAVY =
        new Color(22, 52, 91);

    private static final Color BACKGROUND =
        new Color(242, 246, 251);

    private static final Color ACCENT =
        new Color(62, 148, 204);

    private static final Color DARK_TEXT =
        new Color(25, 52, 86);

    private static final Color MUTED_TEXT =
        new Color(100, 120, 145);

    public static void main(String[] args) {
        try {
            Database.initialize();
            AuthService.initialize();

            SwingUtilities.invokeLater(() ->
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

    // Called by LoginFrame after a successful login.
    public static void openAccountingWindow(String username) {
        // Every user gets the fixed chart of accounts.
        try {
            ChartOfAccounts.ensureForUser(username);
        } catch (SQLException exception) {
            exception.printStackTrace();
            JOptionPane.showMessageDialog(
                null,
                "Could not set up your accounts:\n"
                    + exception.getMessage(),
                "Database Error",
                JOptionPane.ERROR_MESSAGE
            );
        }

        JFrame window = new JFrame(
            "Accounting System - " + username
        );

        window.setDefaultCloseOperation(
            JFrame.EXIT_ON_CLOSE
        );

        window.setSize(1200, 750);

        window.setMinimumSize(
            new Dimension(850, 600)
        );

        window.setLocationRelativeTo(null);

        // Existing application screens.
        AccountsPanel accountsPanel =
            new AccountsPanel();

        NewTransactionPanel newTransactionPanel =
            new NewTransactionPanel();

        TransactionsPanel transactionsPanel =
            new TransactionsPanel();

        JournalPanel journalPanel =
            new JournalPanel();

        LedgerPanel ledgerPanel =
            new LedgerPanel();

        BalanceSheetPanel balanceSheetPanel =
            new BalanceSheetPanel();

        // CardLayout allows us to switch screens.
        CardLayout cardLayout = new CardLayout();

        JPanel content = new JPanel(cardLayout);

        content.add(
            newTransactionPanel,
            "New Transaction"
        );

        content.add(
            transactionsPanel,
            "Transactions"
        );

        content.add(
            accountsPanel,
            "Accounts"
        );

        content.add(
            journalPanel,
            "General Journal"
        );

        content.add(
            ledgerPanel,
            "General Ledger"
        );

        content.add(
            balanceSheetPanel,
            "Balance Sheet"
        );

        JPanel sideMenu = new JPanel();

sideMenu.setLayout(
    new BoxLayout(
        sideMenu,
        BoxLayout.Y_AXIS
    )
);

sideMenu.setBackground(Color.WHITE);

sideMenu.setPreferredSize(
    new Dimension(260, 0)
);

sideMenu.setBorder(
    new EmptyBorder(18, 18, 18, 18)
);

// Transparent overlay that darkens the screen.
JPanel overlay = new JPanel(new BorderLayout()) {

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        g.setColor(
            new Color(22, 52, 91, 65)
        );

        g.fillRect(
            0,
            0,
            getWidth(),
            getHeight()
        );
    }
};

overlay.setOpaque(false);

// Clicking the dimmed area closes the menu.
JPanel backdrop = new JPanel();

backdrop.setOpaque(false);

backdrop.addMouseListener(
    new java.awt.event.MouseAdapter() {

        @Override
        public void mouseClicked(
            java.awt.event.MouseEvent event
        ) {
            overlay.setVisible(false);
        }
    }
);

overlay.add(
    backdrop,
    BorderLayout.CENTER
);

overlay.add(
    sideMenu,
    BorderLayout.EAST
);

window.setGlassPane(overlay);
overlay.setVisible(false);

// Username and close button.
JPanel userHeader = new JPanel(
    new BorderLayout(10, 0)
);

userHeader.setOpaque(false);

userHeader.setAlignmentX(
    Component.LEFT_ALIGNMENT
);

userHeader.setMaximumSize(
    new Dimension(Integer.MAX_VALUE, 60)
);

JLabel avatar = new JLabel(
    "◌",
    SwingConstants.CENTER
);

avatar.setForeground(NAVY);
avatar.setFont(
    new Font(
        "SansSerif",
        Font.PLAIN,
        24
    )
);

avatar.setPreferredSize(
    new Dimension(32, 32)
);

JPanel userText = new JPanel();

userText.setOpaque(false);

userText.setLayout(
    new BoxLayout(
        userText,
        BoxLayout.Y_AXIS
    )
);

JLabel usernameLabel =
    new JLabel(username.toUpperCase());

usernameLabel.setFont(
    new Font(
        "SansSerif",
        Font.BOLD,
        13
    )
);

usernameLabel.setForeground(DARK_TEXT);

JLabel workspaceLabel =
    new JLabel("YOUR ACCOUNTING");

workspaceLabel.setForeground(MUTED_TEXT);

workspaceLabel.setFont(
    new Font(
        "SansSerif",
        Font.PLAIN,
        10
    )
);

userText.add(usernameLabel);
userText.add(workspaceLabel);

JButton closeMenu = new JButton("×");

closeMenu.setFocusable(false);
closeMenu.setBorderPainted(false);
closeMenu.setContentAreaFilled(false);
closeMenu.setForeground(MUTED_TEXT);
closeMenu.setFont(
    new Font(
        "SansSerif",
        Font.PLAIN,
        18
    )
);

closeMenu.addActionListener(
    event -> overlay.setVisible(false)
);

userHeader.add(
    avatar,
    BorderLayout.WEST
);

userHeader.add(
    userText,
    BorderLayout.CENTER
);

userHeader.add(
    closeMenu,
    BorderLayout.EAST
);

sideMenu.add(userHeader);
sideMenu.add(Box.createVerticalStrut(20));

// Navigation actions.
Consumer<String> navigate = screen -> {
    switch (screen) {
        case "New Transaction":
            newTransactionPanel.refreshAccounts();
            break;

        case "Transactions":
            transactionsPanel.refreshTransactions();
            break;

        case "Accounts":
            accountsPanel.refreshAccounts();
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

    cardLayout.show(content, screen);

    overlay.setVisible(false);

    window.revalidate();
    window.repaint();
};

// Blue New Transaction button.
JButton newEntry =
    createMenuButton("+   New Transaction");

newEntry.setHorizontalAlignment(
    SwingConstants.CENTER
);

newEntry.setBackground(NAVY);
newEntry.setForeground(Color.WHITE);
newEntry.setOpaque(true);
newEntry.setContentAreaFilled(true);
newEntry.setBorder(
    new EmptyBorder(12, 12, 12, 12)
);
newEntry.setFocusPainted(false);

newEntry.addActionListener(
    event -> navigate.accept("New Transaction")
);

sideMenu.add(newEntry);
sideMenu.add(Box.createVerticalStrut(20));

// Menu links.
Map<String, String> menuItems =
    new LinkedHashMap<>();

menuItems.put(
    "Home",
    "⌂   Home"
);

menuItems.put(
    "Transactions",
    "⇄   Transactions"
);

menuItems.put(
    "Accounts",
    "▦   Accounts"
);

menuItems.put(
    "General Journal",
    "▤   General Journal"
);

menuItems.put(
    "General Ledger",
    "▥   General Ledger"
);

menuItems.put(
    "Balance Sheet",
    "▤   Balance Sheet"
);

for (
    Map.Entry<String, String> item
        : menuItems.entrySet()
) {
    JButton menuButton =
        createMenuButton(item.getValue());

    String screenName = item.getKey();

    menuButton.addActionListener(
        event -> navigate.accept(screenName)
    );

    sideMenu.add(menuButton);
    sideMenu.add(Box.createVerticalStrut(5));
}

// Place utility actions at the bottom.
sideMenu.add(Box.createVerticalGlue());

JSeparator separator = new JSeparator();

separator.setMaximumSize(
    new Dimension(
        Integer.MAX_VALUE,
        1
    )
);

sideMenu.add(separator);
sideMenu.add(Box.createVerticalStrut(10));

JButton accountSettings =
    createMenuButton("⚙   Account settings");

accountSettings.addActionListener(event -> {
    overlay.setVisible(false);
    // Placeholder for future account-management screen.
});

sideMenu.add(accountSettings);
sideMenu.add(Box.createVerticalStrut(6));

JButton logoutButton =
    createMenuButton("↪   Log out");

logoutButton.addActionListener(event -> {
    int answer = JOptionPane.showConfirmDialog(
        window,
        "Log out of the application?",
        "Confirm Logout",
        JOptionPane.YES_NO_OPTION
    );

    if (answer == JOptionPane.YES_OPTION) {
        overlay.setVisible(false);

        Session.logout();

        new LoginFrame().setVisible(true);

        window.dispose();
    }
});

sideMenu.add(logoutButton);

        // Home screen.
        JPanel homePanel = createHomePanel(navigate);

        content.add(homePanel, "Home");

        // Top header.
        JPanel header = new JPanel(
            new BorderLayout()
        );

        header.setBackground(NAVY);

        header.setBorder(
            new EmptyBorder(12, 22, 12, 22)
        );

        JLabel brand = new JLabel(
            "▣  ACCOUNTING SYSTEM"
        );

        brand.setForeground(Color.WHITE);

        brand.setFont(
            new Font(
                "SansSerif",
                Font.BOLD,
                15
            )
        );

        JButton toggleMenu = new JButton("☰");

        toggleMenu.setForeground(Color.WHITE);
        toggleMenu.setBackground(NAVY);

        toggleMenu.setFont(
            new Font(
                "SansSerif",
                Font.PLAIN,
                22
            )
        );

        toggleMenu.setFocusPainted(false);
        toggleMenu.setBorderPainted(false);

        toggleMenu.setCursor(
            Cursor.getPredefinedCursor(
                Cursor.HAND_CURSOR
            )
        );

        toggleMenu.addActionListener(event -> {
    overlay.setVisible(true);
    overlay.requestFocusInWindow();
});

        header.add(brand, BorderLayout.WEST);
        header.add(toggleMenu, BorderLayout.EAST);

        window.add(header, BorderLayout.NORTH);
        window.add(content, BorderLayout.CENTER);

        cardLayout.show(content, "Home");

        window.setVisible(true);
    }

    private static JPanel createHomePanel(
        Consumer<String> navigate
    ) {
        JPanel home = new JPanel(
            new BorderLayout()
        );

        home.setBackground(
            new Color(242, 246, 251)
        );

        home.setBorder(
            new EmptyBorder(20, 30, 30, 30)
        );

        JPanel content = new JPanel();

        content.setLayout(
            new BoxLayout(
                content,
                BoxLayout.Y_AXIS
            )
        );

        content.setOpaque(false);

        JLabel breadcrumb = new JLabel(
            "Home / Enhanced"
        );

        breadcrumb.setForeground(MUTED_TEXT);

        breadcrumb.setFont(
            new Font(
                "SansSerif",
                Font.PLAIN,
                12
            )
        );

        breadcrumb.setAlignmentX(
            Component.LEFT_ALIGNMENT
        );

        JLabel heading = new JLabel(
            "WELCOME"
        );

        heading.setForeground(DARK_TEXT);

        heading.setFont(
            new Font(
                "SansSerif",
                Font.BOLD,
                26
            )
        );

        heading.setAlignmentX(
            Component.LEFT_ALIGNMENT
        );

        content.add(heading);
        content.add(Box.createVerticalStrut(8));

        JLabel subtitle = new JLabel(
            "Start a transaction or choose a "
                + "section to manage your books."
        );

        subtitle.setForeground(MUTED_TEXT);

        subtitle.setFont(
            new Font(
                "SansSerif",
                Font.PLAIN,
                13
            )
        );

        subtitle.setAlignmentX(
            Component.LEFT_ALIGNMENT
        );

        content.add(subtitle);
        content.add(Box.createVerticalStrut(28));

        JPanel banner = new JPanel(
            new BorderLayout(20, 0)
        );

        banner.setBackground(NAVY);

        banner.setBorder(
            new EmptyBorder(20, 25, 20, 25)
        );

        banner.setMaximumSize(
            new Dimension(
                Integer.MAX_VALUE,
                110
            )
        );

        banner.setAlignmentX(
            Component.LEFT_ALIGNMENT
        );

        JPanel bannerText = new JPanel();

        bannerText.setLayout(
            new BoxLayout(
                bannerText,
                BoxLayout.Y_AXIS
            )
        );

        bannerText.setOpaque(false);

        JLabel bannerTitle = new JLabel(
            "Ready to record something new?"
        );

        bannerTitle.setForeground(Color.WHITE);

        bannerTitle.setFont(
            new Font(
                "SansSerif",
                Font.BOLD,
                16
            )
        );

        JLabel bannerDescription = new JLabel(
            "Create a balanced entry and keep "
                + "your records up to date."
        );

        bannerDescription.setForeground(
            new Color(220, 230, 242)
        );

        bannerDescription.setFont(
            new Font(
                "SansSerif",
                Font.PLAIN,
                12
            )
        );

        bannerText.add(bannerTitle);
        bannerText.add(Box.createVerticalStrut(6));
        bannerText.add(bannerDescription);

        JButton newTransactionButton =
            new JButton("+  New Transaction");

        newTransactionButton.setBackground(Color.WHITE);
        newTransactionButton.setForeground(NAVY);
        newTransactionButton.setOpaque(true);
        newTransactionButton.setContentAreaFilled(true);
        newTransactionButton.setFocusPainted(false);
        newTransactionButton.setBorder(
            new EmptyBorder(10, 15, 10, 15)
        );
        newTransactionButton.addActionListener(
            event -> navigate.accept("New Transaction")
        );

        banner.add(bannerText, BorderLayout.CENTER);
        banner.add(newTransactionButton, BorderLayout.EAST);

        content.add(banner);
        content.add(Box.createVerticalStrut(26));

        JLabel toolsLabel = new JLabel(
            "Your accounting tools"
        );

        toolsLabel.setForeground(DARK_TEXT);

        toolsLabel.setFont(
            new Font(
                "SansSerif",
                Font.BOLD,
                15
            )
        );

        toolsLabel.setAlignmentX(
            Component.LEFT_ALIGNMENT
        );

        content.add(toolsLabel);
        content.add(Box.createVerticalStrut(15));

        JPanel tools = new JPanel(
            new GridLayout(2, 3, 18, 18)
        );

        tools.setOpaque(false);
        tools.setMaximumSize(
            new Dimension(
                Integer.MAX_VALUE,
                420
            )
        );
        tools.setPreferredSize(
            new Dimension(800, 360)
        );

        tools.setAlignmentX(
            Component.LEFT_ALIGNMENT
        );

        tools.add(createToolButton(
            "New Transaction",
            "Record a debit and credit entry.",
            navigate
        ));

        tools.add(createToolButton(
            "Transactions",
            "Search and export posted transactions.",
            navigate
        ));

        tools.add(createToolButton(
            "Accounts",
            "View your chart of accounts.",
            navigate
        ));

        tools.add(createToolButton(
            "General Journal",
            "Review entries in chronological order.",
            navigate
        ));

        tools.add(createToolButton(
            "General Ledger",
            "View activity and running balances.",
            navigate
        ));

        tools.add(createToolButton(
            "Balance Sheet",
            "Review assets, liabilities, and equity.",
            navigate
        ));

        content.add(tools);
        content.add(Box.createVerticalStrut(20));

        JLabel footer = new JLabel(
            "Use the menu in the upper-right corner "
                + "to switch sections at any time."
        );

        footer.setForeground(MUTED_TEXT);

        footer.setFont(
            new Font(
                "SansSerif",
                Font.PLAIN,
                11
            )
        );

        footer.setAlignmentX(
            Component.LEFT_ALIGNMENT
        );

        content.add(footer);

        home.add(content, BorderLayout.CENTER);

        return home;
    }

    private static JButton createToolButton(
        String screenName,
        String description,
        Consumer<String> navigate
    ) {
        boolean featured = false;

        String title =
            "Accounts".equals(screenName)
                ? "Chart of Accounts"
                : screenName;

String iconText;

    switch (screenName) {
        case "New Transaction":
            iconText = "+";
            break;

        case "Transactions":
        case "General Journal":
            iconText = "≡";
            break;

        case "Accounts":
        case "General Ledger":
        case "Balance Sheet":
            iconText = "▤";
            break;

        default:
            iconText = "•";
            break;
    }

    Color cardColor =
        featured ? NAVY : Color.WHITE;

    Color mainTextColor =
        featured ? Color.WHITE : DARK_TEXT;

    Color descriptionColor =
        featured
            ? new Color(220, 232, 246)
            : new Color(100, 116, 139);

    Color iconBackground =
        featured
            ? Color.WHITE
            : new Color(232, 242, 251);

    // Draw the rounded card background.
    JButton button = new JButton() {
        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D graphics =
                (Graphics2D) g.create();

            try {
                graphics.setRenderingHint(
                    RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON
                );

                graphics.setColor(cardColor);

                graphics.fillRoundRect(
                    0,
                    0,
                    getWidth() - 1,
                    getHeight() - 1,
                    20,
                    20
                );

                if (!featured) {
                    graphics.setColor(
                        new Color(220, 229, 239)
                    );

                    graphics.drawRoundRect(
                        0,
                        0,
                        getWidth() - 1,
                        getHeight() - 1,
                        20,
                        20
                    );
                }
            } finally {
                graphics.dispose();
            }

            super.paintComponent(g);
        }
    };

    button.setLayout(
        new BorderLayout(0, 10)
    );

    button.setOpaque(false);
    button.setContentAreaFilled(false);
    button.setBorderPainted(false);

    // Keep a visible focus indicator for keyboard navigation.
    button.setFocusPainted(true);

    button.setBorder(
        new EmptyBorder(16, 16, 16, 16)
    );

    button.setCursor(
        Cursor.getPredefinedCursor(
            Cursor.HAND_CURSOR
        )
    );

    // Top row: icon on the left, arrow on the right.
    JPanel topRow = new JPanel(
        new BorderLayout()
    );

    topRow.setOpaque(false);

    JLabel icon = new JLabel(
        iconText,
        SwingConstants.CENTER
    );

    icon.setFont(
        new Font(
            "SansSerif",
            Font.BOLD,
            18
        )
    );

    icon.setForeground(NAVY);
    icon.setBackground(iconBackground);
    icon.setOpaque(true);

    icon.setPreferredSize(
        new Dimension(30, 30)
    );

    JLabel arrow = new JLabel(
        "→",
        SwingConstants.RIGHT
    );

    arrow.setForeground(mainTextColor);

    arrow.setFont(
        new Font(
            "SansSerif",
            Font.PLAIN,
            18
        )
    );

    topRow.add(
        icon,
        BorderLayout.WEST
    );

    topRow.add(
        arrow,
        BorderLayout.EAST
    );

    // Title and description below the icon.
    JPanel textPanel = new JPanel();

    textPanel.setOpaque(false);

    textPanel.setLayout(
        new BoxLayout(
            textPanel,
            BoxLayout.Y_AXIS
        )
    );

    JLabel titleLabel = new JLabel(title);

    titleLabel.setFont(
        new Font(
            "SansSerif",
            Font.BOLD,
            14
        )
    );

    titleLabel.setForeground(mainTextColor);

    titleLabel.setAlignmentX(
        Component.LEFT_ALIGNMENT
    );

    JLabel descriptionLabel =
        new JLabel(description);

    descriptionLabel.setFont(
        new Font(
            "SansSerif",
            Font.PLAIN,
            11
        )
    );

    descriptionLabel.setForeground(
        descriptionColor
    );

    descriptionLabel.setAlignmentX(
        Component.LEFT_ALIGNMENT
    );

    textPanel.add(titleLabel);
    textPanel.add(Box.createVerticalStrut(6));
    textPanel.add(descriptionLabel);

    button.add(
        topRow,
        BorderLayout.NORTH
    );

    button.add(
        textPanel,
        BorderLayout.CENTER
    );

    button.addActionListener(
        event -> navigate.accept(screenName)
    );

    return button;
}

    private static JButton createMenuButton(
        String text
    ) {
        JButton button = new JButton(text);

        button.setHorizontalAlignment(
            SwingConstants.LEFT
        );

        button.setAlignmentX(
            Component.LEFT_ALIGNMENT
        );

        button.setMaximumSize(
            new Dimension(
                Integer.MAX_VALUE,
                38
            )
        );

        button.setBackground(Color.WHITE);
        button.setForeground(DARK_TEXT);

        button.setFont(
            new Font(
                "SansSerif",
                Font.PLAIN,
                13
            )
        );

        button.setFocusPainted(false);
        button.setBorderPainted(false);
        button.setOpaque(true);

        button.setBorder(
            new EmptyBorder(8, 8, 8, 8)
        );

        button.setCursor(
            Cursor.getPredefinedCursor(
                Cursor.HAND_CURSOR
            )
        );

        return button;
    }
}