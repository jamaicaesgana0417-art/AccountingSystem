import javax.swing.*;
import javax.swing.border.EmptyBorder;

import java.awt.*;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
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
        HomeDashboard[] homeDashboard = new HomeDashboard[1];

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
    new Dimension(200, 0)
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
    new JLabel("STUDENT");

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


sideMenu.add(userHeader);
sideMenu.add(Box.createVerticalStrut(20));

// Navigation actions.
Consumer<String> navigate = screen -> {
    switch (screen) {
        case "Home":
            if (homeDashboard[0] != null) {
                homeDashboard[0].refreshStats();
            }
            break;

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
    sideMenu.setVisible(!"Home".equals(screen));

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

accountSettings.setText("⚙   Settings");
accountSettings.setEnabled(true);

accountSettings.addActionListener(
    event -> navigate.accept("Account Settings")
);

accountSettings.setToolTipText(
    "The settings screen has not been connected yet."
);

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

        content.add(
    createAccountSettingsPanel(
        username
    ),

    "Account Settings"
);

        // Home screen.
        HomeDashboard homePanel =
            new HomeDashboard(username, navigate);
        homeDashboard[0] = homePanel;

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

       JButton profileButton = new JButton("●");

profileButton.setToolTipText("Account menu");

profileButton.setForeground(NAVY);

profileButton.setBackground(
    new Color(230, 236, 255)
);

profileButton.setFont(
    new Font(
        "SansSerif",
        Font.PLAIN,
        18
    )
);

profileButton.setFocusPainted(false);

profileButton.setCursor(
    Cursor.getPredefinedCursor(
        Cursor.HAND_CURSOR
    )
);

// Menu shown when the profile button is clicked.
JPopupMenu profileMenu = new JPopupMenu();

JMenuItem userMenuItem = new JMenuItem(
    "Signed in as: " + username
);

userMenuItem.setEnabled(false);

JMenuItem logoutMenuItem = new JMenuItem("Log out");

// Reuses the existing logout confirmation and action.
logoutMenuItem.addActionListener(
    event -> logoutButton.doClick()
);

profileMenu.add(userMenuItem);
profileMenu.addSeparator();
profileMenu.add(logoutMenuItem);

        header.add(brand, BorderLayout.WEST);
        window.add(header, BorderLayout.NORTH);

        JPanel workspace = new JPanel(new BorderLayout());
        workspace.add(sideMenu, BorderLayout.WEST);
        workspace.add(content, BorderLayout.CENTER);
        window.add(workspace, BorderLayout.CENTER);

        cardLayout.show(content, "Home");
        sideMenu.setVisible(false);

        window.setVisible(true);
    }

    private static JPanel createAccountSettingsPanel(
        String username
    ) {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(BACKGROUND);

        JPanel content = new JPanel();
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.setBorder(new EmptyBorder(28, 30, 30, 30));
        content.setOpaque(false);

        JLabel eyebrow = new JLabel("YOUR WORKSPACE");
        eyebrow.setForeground(ACCENT);
        eyebrow.setFont(new Font("SansSerif", Font.BOLD, 11));
        eyebrow.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel heading = new JLabel("Account settings");
        heading.setFont(new Font("SansSerif", Font.BOLD, 28));
        heading.setForeground(DARK_TEXT);
        heading.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel subtitle = new JLabel(
            "Review your profile, workspace activity, and account access."
        );
        subtitle.setForeground(MUTED_TEXT);
        subtitle.setFont(new Font("SansSerif", Font.PLAIN, 13));
        subtitle.setAlignmentX(Component.LEFT_ALIGNMENT);

        JPanel profileCard = new JPanel(new BorderLayout(16, 0));
        profileCard.setBackground(Color.WHITE);
        profileCard.setBorder(new EmptyBorder(20, 22, 20, 22));
        profileCard.setAlignmentX(Component.LEFT_ALIGNMENT);
        profileCard.setMaximumSize(new Dimension(Integer.MAX_VALUE, 110));

        JLabel avatar = new JLabel(
            username.substring(0, 1).toUpperCase(),
            SwingConstants.CENTER
        );
        avatar.setOpaque(true);
        avatar.setBackground(new Color(230, 239, 249));
        avatar.setForeground(NAVY);
        avatar.setFont(new Font("SansSerif", Font.BOLD, 22));
        avatar.setPreferredSize(new Dimension(56, 56));

        JPanel identity = new JPanel();
        identity.setOpaque(false);
        identity.setLayout(new BoxLayout(identity, BoxLayout.Y_AXIS));

        JLabel profileName = new JLabel(username);
        profileName.setForeground(DARK_TEXT);
        profileName.setFont(new Font("SansSerif", Font.BOLD, 18));

        JLabel profileDescription = new JLabel("Personal accounting workspace");
        profileDescription.setForeground(MUTED_TEXT);
        profileDescription.setFont(new Font("SansSerif", Font.PLAIN, 12));

        JLabel sessionStatus = new JLabel("ACTIVE SESSION");
        sessionStatus.setForeground(new Color(34, 139, 93));
        sessionStatus.setFont(new Font("SansSerif", Font.BOLD, 10));

        identity.add(profileName);
        identity.add(Box.createVerticalStrut(5));
        identity.add(profileDescription);
        identity.add(Box.createVerticalStrut(7));
        identity.add(sessionStatus);

        profileCard.add(avatar, BorderLayout.WEST);
        profileCard.add(identity, BorderLayout.CENTER);

        JPanel summaryCards = new JPanel(new GridLayout(1, 2, 14, 0));
        summaryCards.setOpaque(false);
        summaryCards.setAlignmentX(Component.LEFT_ALIGNMENT);
        summaryCards.setMaximumSize(new Dimension(Integer.MAX_VALUE, 112));

        JLabel accountsValue = new JLabel("Loading...");
        JLabel transactionsValue = new JLabel("Loading...");
        summaryCards.add(createSettingsMetric("ACTIVE ACCOUNTS", accountsValue));
        summaryCards.add(createSettingsMetric("POSTED TRANSACTIONS", transactionsValue));

        JLabel summaryError = new JLabel(" ");
        summaryError.setForeground(new Color(176, 55, 55));
        summaryError.setFont(new Font("SansSerif", Font.PLAIN, 11));
        summaryError.setAlignmentX(Component.LEFT_ALIGNMENT);

        JPanel securityCard = new JPanel();
        securityCard.setLayout(new BoxLayout(securityCard, BoxLayout.Y_AXIS));
        securityCard.setBackground(Color.WHITE);
        securityCard.setBorder(new EmptyBorder(18, 22, 18, 22));
        securityCard.setAlignmentX(Component.LEFT_ALIGNMENT);
        securityCard.setMaximumSize(
            new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE)
        );

        JLabel securityTitle = new JLabel("Sign-in and security");
        securityTitle.setForeground(DARK_TEXT);
        securityTitle.setFont(new Font("SansSerif", Font.BOLD, 15));

        JLabel securityDescription = new JLabel(
            "<html>Your password is not displayed in settings. "
                + "Verify your current password before setting a new one. "
                + "Log out when you finish using a shared device.</html>"
        );
        securityDescription.setForeground(MUTED_TEXT);
        securityDescription.setFont(new Font("SansSerif", Font.PLAIN, 12));
        securityDescription.setAlignmentX(Component.LEFT_ALIGNMENT);

        JPasswordField currentPasswordField = new JPasswordField(24);
        JPasswordField newPasswordField = new JPasswordField(24);
        JPasswordField confirmPasswordField = new JPasswordField(24);
        styleSettingsPasswordField(currentPasswordField);
        styleSettingsPasswordField(newPasswordField);
        styleSettingsPasswordField(confirmPasswordField);

        JLabel passwordStatus = new JLabel(" ");
        passwordStatus.setFont(new Font("SansSerif", Font.PLAIN, 11));
        passwordStatus.setAlignmentX(Component.LEFT_ALIGNMENT);

        JButton changePasswordButton = new JButton("Update password");
        styleSettingsButton(changePasswordButton, true);
        changePasswordButton.setAlignmentX(Component.LEFT_ALIGNMENT);
        changePasswordButton.addActionListener(event ->
            changePassword(
                username,
                currentPasswordField,
                newPasswordField,
                confirmPasswordField,
                changePasswordButton,
                passwordStatus
            )
        );

        JSeparator securitySeparator = new JSeparator();
        securitySeparator.setMaximumSize(
            new Dimension(Integer.MAX_VALUE, 1)
        );

        JLabel eraseDescription = new JLabel(
            "<html>Permanently delete this user's transactions and their "
                + "journal entries. This cannot be undone.</html>"
        );
        eraseDescription.setForeground(MUTED_TEXT);
        eraseDescription.setFont(new Font("SansSerif", Font.PLAIN, 12));
        eraseDescription.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel eraseStatus = new JLabel(" ");
        eraseStatus.setForeground(new Color(176, 55, 55));
        eraseStatus.setFont(new Font("SansSerif", Font.PLAIN, 11));
        eraseStatus.setAlignmentX(Component.LEFT_ALIGNMENT);

        JButton eraseTransactionsButton =
            new JButton("Erase all my transactions");
        styleSettingsButton(eraseTransactionsButton, true);
        eraseTransactionsButton.setBackground(new Color(169, 48, 48));
        eraseTransactionsButton.setAlignmentX(Component.LEFT_ALIGNMENT);
        eraseTransactionsButton.addActionListener(event ->
            eraseTransactions(
                username,
                panel,
                transactionsValue,
                eraseTransactionsButton,
                eraseStatus
            )
        );

        securityCard.add(securityTitle);
        securityCard.add(Box.createVerticalStrut(8));
        securityCard.add(securityDescription);
        securityCard.add(Box.createVerticalStrut(14));
        securityCard.add(settingsFieldLabel("Current password"));
        securityCard.add(Box.createVerticalStrut(5));
        securityCard.add(currentPasswordField);
        securityCard.add(Box.createVerticalStrut(10));
        securityCard.add(settingsFieldLabel("New password"));
        securityCard.add(Box.createVerticalStrut(5));
        securityCard.add(newPasswordField);
        securityCard.add(Box.createVerticalStrut(10));
        securityCard.add(settingsFieldLabel("Confirm new password"));
        securityCard.add(Box.createVerticalStrut(5));
        securityCard.add(confirmPasswordField);
        securityCard.add(Box.createVerticalStrut(8));
        securityCard.add(changePasswordButton);
        securityCard.add(Box.createVerticalStrut(3));
        securityCard.add(passwordStatus);
        securityCard.add(Box.createVerticalStrut(12));
        securityCard.add(securitySeparator);
        securityCard.add(Box.createVerticalStrut(12));
        securityCard.add(eraseDescription);
        securityCard.add(Box.createVerticalStrut(8));
        securityCard.add(eraseTransactionsButton);
        securityCard.add(Box.createVerticalStrut(3));
        securityCard.add(eraseStatus);

        content.add(eyebrow);
        content.add(Box.createVerticalStrut(7));
        content.add(heading);
        content.add(Box.createVerticalStrut(7));
        content.add(subtitle);
        content.add(Box.createVerticalStrut(22));
        content.add(profileCard);
        content.add(Box.createVerticalStrut(16));
        content.add(summaryCards);
        content.add(Box.createVerticalStrut(5));
        content.add(summaryError);
        content.add(Box.createVerticalStrut(12));
        content.add(securityCard);

        JScrollPane scrollPane = new JScrollPane(content);
        scrollPane.setBorder(null);
        scrollPane.setOpaque(false);
        scrollPane.getViewport().setOpaque(false);
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);
        panel.add(scrollPane, BorderLayout.CENTER);

        loadWorkspaceSummary(
            username,
            accountsValue,
            transactionsValue,
            summaryError
        );
        return panel;
    }

    private static JPanel createSettingsMetric(String title, JLabel value) {
        JPanel card = new JPanel();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBackground(Color.WHITE);
        card.setBorder(new EmptyBorder(16, 18, 16, 18));
        card.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel label = new JLabel(title);
        label.setForeground(MUTED_TEXT);
        label.setFont(new Font("SansSerif", Font.BOLD, 10));

        value.setForeground(DARK_TEXT);
        value.setFont(new Font("SansSerif", Font.BOLD, 24));

        card.add(label);
        card.add(Box.createVerticalStrut(12));
        card.add(value);
        return card;
    }

    private static void styleSettingsButton(JButton button, boolean primary) {
        button.setFont(new Font("SansSerif", Font.BOLD, 12));
        button.setFocusPainted(false);
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        button.setBorder(new EmptyBorder(10, 15, 10, 15));
        button.setBackground(primary ? NAVY : Color.WHITE);
        button.setForeground(primary ? Color.WHITE : DARK_TEXT);
    }

    private static JLabel settingsFieldLabel(String text) {
        JLabel label = new JLabel(text);
        label.setForeground(DARK_TEXT);
        label.setFont(new Font("SansSerif", Font.BOLD, 11));
        label.setAlignmentX(Component.LEFT_ALIGNMENT);
        return label;
    }

    private static void styleSettingsPasswordField(
        JPasswordField field
    ) {
        field.setFont(new Font("SansSerif", Font.PLAIN, 13));
        field.setMaximumSize(
            new Dimension(Integer.MAX_VALUE, 38)
        );
        field.setAlignmentX(Component.LEFT_ALIGNMENT);
    }

    private static void changePassword(
        String username,
        JPasswordField currentField,
        JPasswordField newField,
        JPasswordField confirmationField,
        JButton submitButton,
        JLabel status
    ) {
        char[] currentPassword = currentField.getPassword();
        char[] newPassword = newField.getPassword();
        char[] confirmation = confirmationField.getPassword();

        if (currentPassword.length == 0
                || newPassword.length == 0
                || confirmation.length == 0) {
            java.util.Arrays.fill(currentPassword, '\0');
            java.util.Arrays.fill(newPassword, '\0');
            java.util.Arrays.fill(confirmation, '\0');
            status.setForeground(new Color(176, 55, 55));
            status.setText("Complete all password fields.");
            return;
        }

        if (!java.util.Arrays.equals(newPassword, confirmation)) {
            java.util.Arrays.fill(currentPassword, '\0');
            java.util.Arrays.fill(newPassword, '\0');
            java.util.Arrays.fill(confirmation, '\0');
            status.setForeground(new Color(176, 55, 55));
            status.setText("The new passwords do not match.");
            return;
        }

        if (newPassword.length < 8) {
            java.util.Arrays.fill(currentPassword, '\0');
            java.util.Arrays.fill(newPassword, '\0');
            java.util.Arrays.fill(confirmation, '\0');
            status.setForeground(new Color(176, 55, 55));
            status.setText("Use at least 8 characters for the new password.");
            return;
        }

        submitButton.setEnabled(false);
        status.setForeground(MUTED_TEXT);
        status.setText("Updating password...");

        new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() throws Exception {
                try {
                    AuthService.changePassword(
                        username,
                        currentPassword,
                        newPassword
                    );
                    return null;
                } finally {
                    java.util.Arrays.fill(currentPassword, '\0');
                    java.util.Arrays.fill(newPassword, '\0');
                    java.util.Arrays.fill(confirmation, '\0');
                }
            }

            @Override
            protected void done() {
                currentField.setText("");
                newField.setText("");
                confirmationField.setText("");
                submitButton.setEnabled(true);

                try {
                    get();
                    status.setForeground(new Color(34, 139, 93));
                    status.setText("Password updated successfully.");
                } catch (InterruptedException exception) {
                    Thread.currentThread().interrupt();
                    status.setForeground(new Color(176, 55, 55));
                    status.setText("Password update was interrupted.");
                } catch (java.util.concurrent.ExecutionException exception) {
                    Throwable cause = exception.getCause();
                    if (!(cause instanceof IllegalArgumentException)) {
                        cause.printStackTrace();
                    }
                    status.setForeground(new Color(176, 55, 55));
                    status.setText(cause.getMessage());
                }
            }
        }.execute();
    }

    private static void eraseTransactions(
        String username,
        Component parent,
        JLabel transactionsValue,
        JButton eraseButton,
        JLabel status
    ) {
        int answer = JOptionPane.showConfirmDialog(
            parent,
            "This permanently deletes all transactions and journal entries "
                + "for " + username + ". This cannot be undone.\n\n"
                + "Continue?",
            "Erase My Transactions",
            JOptionPane.YES_NO_OPTION,
            JOptionPane.WARNING_MESSAGE
        );

        if (answer != JOptionPane.YES_OPTION) {
            return;
        }

        String confirmation = JOptionPane.showInputDialog(
            parent,
            "Type DELETE to permanently erase your transactions:",
            "Confirm Permanent Deletion",
            JOptionPane.WARNING_MESSAGE
        );
        if (!"DELETE".equals(confirmation)) {
            status.setText("Deletion cancelled; confirmation did not match.");
            return;
        }

        eraseButton.setEnabled(false);
        status.setText("Deleting transactions...");

        new SwingWorker<Integer, Void>() {
            @Override
            protected Integer doInBackground() throws SQLException {
                return deleteTransactionsForUser(username);
            }

            @Override
            protected void done() {
                eraseButton.setEnabled(true);
                try {
                    int deletedCount = get();
                    transactionsValue.setText("0");
                    status.setText(
                        deletedCount + " transaction(s) erased."
                    );
                } catch (InterruptedException exception) {
                    Thread.currentThread().interrupt();
                    status.setText("Deletion was interrupted.");
                } catch (java.util.concurrent.ExecutionException exception) {
                    Throwable cause = exception.getCause();
                    cause.printStackTrace();
                    status.setText(
                        "Could not erase transactions: "
                            + cause.getMessage()
                    );
                }
            }
        }.execute();
    }

    private static int deleteTransactionsForUser(
        String username
    ) throws SQLException {
        try (Connection connection = Database.connect()) {
            connection.setAutoCommit(false);

            try {
                String deleteLinesSql = """
                    DELETE FROM journal_lines
                    WHERE transaction_id IN (
                        SELECT id FROM transactions
                        WHERE owner_username = ?
                    )
                    """;
                try (PreparedStatement statement =
                         connection.prepareStatement(deleteLinesSql)) {
                    statement.setString(1, username);
                    statement.executeUpdate();
                }

                String deleteTransactionsSql = """
                    DELETE FROM transactions
                    WHERE owner_username = ?
                    """;
                int deletedCount;
                try (PreparedStatement statement =
                         connection.prepareStatement(deleteTransactionsSql)) {
                    statement.setString(1, username);
                    deletedCount = statement.executeUpdate();
                }

                connection.commit();
                return deletedCount;
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

    private static void loadWorkspaceSummary(
        String username,
        JLabel accountsValue,
        JLabel transactionsValue,
        JLabel errorLabel
    ) {
        new SwingWorker<int[], Void>() {
            @Override
            protected int[] doInBackground() throws SQLException {
                String sql = """
                    SELECT
                        (SELECT COUNT(*) FROM accounts
                         WHERE owner_username = ? AND active = 1) AS account_count,
                        (SELECT COUNT(*) FROM transactions
                         WHERE owner_username = ?) AS transaction_count
                    """;

                try (
                    Connection connection = Database.connect();
                    PreparedStatement statement =
                        connection.prepareStatement(sql)
                ) {
                    statement.setString(1, username);
                    statement.setString(2, username);
                    try (ResultSet result = statement.executeQuery()) {
                        if (!result.next()) {
                            throw new SQLException(
                                "Workspace summary query returned no result."
                            );
                        }
                        return new int[] {
                            result.getInt("account_count"),
                            result.getInt("transaction_count")
                        };
                    }
                }
            }

            @Override
            protected void done() {
                try {
                    int[] summary = get();
                    accountsValue.setText(Integer.toString(summary[0]));
                    transactionsValue.setText(Integer.toString(summary[1]));
                } catch (InterruptedException exception) {
                    Thread.currentThread().interrupt();
                    errorLabel.setText("Could not load workspace summary.");
                } catch (java.util.concurrent.ExecutionException exception) {
                    Throwable cause = exception.getCause();
                    cause.printStackTrace();
                    errorLabel.setText(
                        "Could not load workspace summary: "
                            + cause.getMessage()
                    );
                }
            }
        }.execute();
    }

    private static final class HomeDashboard extends JPanel {
        private final String username;
        private final JLabel entriesValue = new JLabel("Loading...");
        private final JLabel accountsValue = new JLabel("Loading...");
        private final JLabel balancedValue = new JLabel("Loading...");
        private final JLabel statsError = new JLabel(" ");

        HomeDashboard(String username, Consumer<String> navigate) {
            this.username = username;
            HomeFonts.initialize();
            setLayout(new BorderLayout());
            setBackground(BACKGROUND);
            setOpaque(true);

            JLabel dateLabel = new JLabel(
                java.time.LocalDate.now().format(
                    java.time.format.DateTimeFormatter.ofPattern(
                        "EEEE, MMMM d"
                    )
                ).toUpperCase()
            );
            dateLabel.setForeground(ACCENT);
            dateLabel.setFont(new Font("Poppins", Font.BOLD, 10));
            dateLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

            JPanel content = new JPanel();
            content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
            content.setBorder(new EmptyBorder(28, 36, 28, 36));
            content.setOpaque(false);

            JLabel heading = new JLabel("Hey, Student!");
            heading.setForeground(DARK_TEXT);
            heading.setFont(HomeFonts.poppinsSemiBold(30));
            heading.setAlignmentX(Component.LEFT_ALIGNMENT);

            JLabel subtitle = new JLabel(
                "Welcome back — let’s make today count."
            );
            subtitle.setForeground(MUTED_TEXT);
            subtitle.setFont(HomeFonts.nunitoSansRegular(14));
            subtitle.setAlignmentX(Component.LEFT_ALIGNMENT);

            JPanel hero = new JPanel(new BorderLayout(20, 0)) {
                @Override
                protected void paintComponent(Graphics graphics) {
                    super.paintComponent(graphics);
                    Graphics2D g2 = (Graphics2D) graphics.create();
                    g2.setRenderingHint(
                        RenderingHints.KEY_ANTIALIASING,
                        RenderingHints.VALUE_ANTIALIAS_ON
                    );
                    g2.setPaint(new GradientPaint(
                        0, 0, new Color(22, 52, 91),
                        getWidth(), getHeight(), new Color(42, 105, 161)
                    ));
                    g2.fillRoundRect(
                        0, 0, getWidth(), getHeight(), 24, 24
                    );
                    g2.setColor(new Color(255, 255, 255, 18));
                    g2.fillOval(getWidth() - 190, -90, 240, 240);
                    g2.fillOval(getWidth() - 125, 45, 115, 115);
                    g2.dispose();
                }
            };
            hero.setOpaque(false);
            hero.setBorder(new EmptyBorder(23, 26, 23, 26));
            hero.setAlignmentX(Component.LEFT_ALIGNMENT);
            hero.setMaximumSize(new Dimension(Integer.MAX_VALUE, 138));

            JPanel heroText = new JPanel();
            heroText.setOpaque(false);
            heroText.setLayout(new BoxLayout(heroText, BoxLayout.Y_AXIS));

            JLabel heroTitle = new JLabel(
                "YOUR ACCOUNTING JOURNEY STARTS HERE"
            );
            heroTitle.setForeground(Color.WHITE);
            heroTitle.setFont(HomeFonts.poppinsSemiBold(24));

            JLabel heroSubtitle = new JLabel(
                "One clear entry at a time, you’re building stronger books."
            );
            heroSubtitle.setForeground(new Color(220, 232, 246));
            heroSubtitle.setFont(HomeFonts.nunitoSansRegular(13));

            heroText.add(heroTitle);
            heroText.add(Box.createVerticalStrut(5));
            heroText.add(heroSubtitle);

            JButton newTransactionButton =
                new JButton("+  New transaction");
            newTransactionButton.setBackground(Color.WHITE);
            newTransactionButton.setForeground(NAVY);
            newTransactionButton.setOpaque(true);
            newTransactionButton.setContentAreaFilled(true);
            newTransactionButton.setFocusPainted(false);
            newTransactionButton.setFont(
                new Font("Poppins", Font.BOLD, 12)
            );
            newTransactionButton.setBorder(
                new EmptyBorder(12, 18, 12, 18)
            );
            newTransactionButton.setCursor(
                Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)
            );
            newTransactionButton.addActionListener(
                event -> navigate.accept("New Transaction")
            );

            hero.add(heroText, BorderLayout.CENTER);
            hero.add(newTransactionButton, BorderLayout.EAST);

            JPanel stats = new JPanel(new GridLayout(1, 3, 14, 0));
            stats.setOpaque(false);
            stats.setAlignmentX(Component.LEFT_ALIGNMENT);
            stats.setMaximumSize(new Dimension(Integer.MAX_VALUE, 108));
            stats.add(createHomeStatCard(
                "Entries this week",
                entriesValue,
                "Your weekly activity",
                "↗",
                ACCENT
            ));
            stats.add(createHomeStatCard(
                "Accounts set up",
                accountsValue,
                "Ready to organize",
                "▤",
                new Color(75, 145, 117)
            ));
            stats.add(createHomeStatCard(
                "Books balanced",
                balancedValue,
                "Debit and credit check",
                "✓",
                new Color(113, 104, 190)
            ));

            JLabel statsErrorLabel = statsError;
            statsErrorLabel.setForeground(new Color(176, 55, 55));
            statsErrorLabel.setFont(new Font("Poppins", Font.PLAIN, 11));
            statsErrorLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

            JPanel sectionHeading = new JPanel(new BorderLayout());
            sectionHeading.setOpaque(false);
            sectionHeading.setAlignmentX(Component.LEFT_ALIGNMENT);

            JLabel quickActionsTitle = new JLabel("Quick actions");
            quickActionsTitle.setForeground(DARK_TEXT);
            quickActionsTitle.setFont(new Font("Poppins", Font.BOLD, 16));

            JLabel quickActionsHint = new JLabel("Pick up where you left off");
            quickActionsHint.setForeground(MUTED_TEXT);
            quickActionsHint.setFont(new Font("Poppins", Font.PLAIN, 11));
            sectionHeading.add(quickActionsTitle, BorderLayout.WEST);
            sectionHeading.add(quickActionsHint, BorderLayout.EAST);

            JPanel quickActions = new JPanel(new GridLayout(2, 3, 14, 14));
            quickActions.setOpaque(false);
            quickActions.setAlignmentX(Component.LEFT_ALIGNMENT);
            quickActions.setMaximumSize(new Dimension(Integer.MAX_VALUE, 300));
            quickActions.add(createToolButton(
                "Transactions",
                "Browse, filter, and export entries.",
                navigate
            ));
            quickActions.add(createToolButton(
                "Accounts",
                "Keep your accounts organized.",
                navigate
            ));
            quickActions.add(createToolButton(
                "General Journal",
                "See every entry in order.",
                navigate
            ));
            quickActions.add(createToolButton(
                "General Ledger",
                "Explore balances and account activity.",
                navigate
            ));
            quickActions.add(createToolButton(
                "Balance Sheet",
                "Review assets, liabilities, and equity.",
                navigate
            ));
            quickActions.add(createToolButton(
                "New Transaction",
                "Record a debit and credit entry.",
                navigate
            ));
            content.add(dateLabel);
            content.add(Box.createVerticalStrut(5));
            content.add(heading);
            content.add(Box.createVerticalStrut(6));
            content.add(subtitle);
            content.add(Box.createVerticalStrut(20));
            content.add(hero);
            content.add(Box.createVerticalStrut(16));
            content.add(stats);
            content.add(Box.createVerticalStrut(2));
            content.add(statsErrorLabel);
            content.add(Box.createVerticalStrut(9));
            content.add(sectionHeading);
            content.add(Box.createVerticalStrut(10));
            content.add(quickActions);

            JScrollPane scrollPane = new JScrollPane(content);
            scrollPane.setBorder(null);
            scrollPane.setOpaque(false);
            scrollPane.getViewport().setOpaque(false);
            scrollPane.getVerticalScrollBar().setUnitIncrement(16);
            add(scrollPane, BorderLayout.CENTER);

            refreshStats();
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            super.paintComponent(graphics);
            Graphics2D g2 = (Graphics2D) graphics.create();
            g2.setRenderingHint(
                RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_ON
            );
            g2.setPaint(new GradientPaint(
                0, 0, new Color(249, 252, 255),
                0, getHeight(), BACKGROUND
            ));
            g2.fillRect(0, 0, getWidth(), getHeight());
            g2.setColor(new Color(62, 148, 204, 14));
            g2.fillOval(getWidth() - 230, -145, 300, 300);
            g2.dispose();
        }

        void refreshStats() {
            new SwingWorker<HomeStats, Void>() {
                @Override
                protected HomeStats doInBackground() throws SQLException {
                    String sql = """
                        SELECT
                            (SELECT COUNT(*) FROM transactions
                             WHERE owner_username = ?
                               AND transaction_date >= ?
                               AND transaction_date <= ?) AS entries_this_week,
                            (SELECT COUNT(*) FROM accounts
                             WHERE owner_username = ? AND active = 1) AS active_accounts,
                            (SELECT COALESCE(SUM(jl.debit_centavos), 0)
                               FROM journal_lines jl
                               JOIN transactions t ON t.id = jl.transaction_id
                              WHERE t.owner_username = ?) AS total_debits,
                            (SELECT COALESCE(SUM(jl.credit_centavos), 0)
                               FROM journal_lines jl
                               JOIN transactions t ON t.id = jl.transaction_id
                              WHERE t.owner_username = ?) AS total_credits
                        """;

                    java.time.LocalDate today = java.time.LocalDate.now();
                    java.time.LocalDate weekStart = today.with(
                        java.time.DayOfWeek.MONDAY
                    );

                    try (
                        Connection connection = Database.connect();
                        PreparedStatement statement =
                            connection.prepareStatement(sql)
                    ) {
                        statement.setString(1, username);
                        statement.setString(2, weekStart.toString());
                        statement.setString(3, today.toString());
                        statement.setString(4, username);
                        statement.setString(5, username);
                        statement.setString(6, username);

                        try (ResultSet result = statement.executeQuery()) {
                            if (!result.next()) {
                                throw new SQLException(
                                    "Home dashboard summary returned no result."
                                );
                            }
                            return new HomeStats(
                                result.getInt("entries_this_week"),
                                result.getInt("active_accounts"),
                                result.getLong("total_debits"),
                                result.getLong("total_credits")
                            );
                        }
                    }
                }

                @Override
                protected void done() {
                    try {
                        HomeStats stats = get();
                        entriesValue.setText(
                            Integer.toString(stats.entriesThisWeek)
                        );
                        accountsValue.setText(
                            stats.activeAccounts + " / "
                                + ChartOfAccounts.ACCOUNTS.size()
                        );
                        balancedValue.setText(
                            stats.totalDebits == stats.totalCredits
                                ? "Yes"
                                : "Review"
                        );
                        statsError.setText(" ");
                    } catch (InterruptedException exception) {
                        Thread.currentThread().interrupt();
                        showStatsError("Could not load Home statistics.");
                    } catch (java.util.concurrent.ExecutionException exception) {
                        Throwable cause = exception.getCause();
                        cause.printStackTrace();
                        showStatsError(
                            "Could not load Home statistics: "
                                + cause.getMessage()
                        );
                    }
                }
            }.execute();
        }

        private void showStatsError(String message) {
            entriesValue.setText("—");
            accountsValue.setText("—");
            balancedValue.setText("—");
            statsError.setText(message);
        }
    }

    private static final class HomeStats {
        final int entriesThisWeek;
        final int activeAccounts;
        final long totalDebits;
        final long totalCredits;

        HomeStats(
            int entriesThisWeek,
            int activeAccounts,
            long totalDebits,
            long totalCredits
        ) {
            this.entriesThisWeek = entriesThisWeek;
            this.activeAccounts = activeAccounts;
            this.totalDebits = totalDebits;
            this.totalCredits = totalCredits;
        }
    }

    private static JPanel createHomeStatCard(
        String title,
        JLabel value,
        String description,
        String iconText,
        Color accent
    ) {
        JPanel card = new JPanel() {
            @Override
            protected void paintComponent(Graphics graphics) {
                Graphics2D g2 = (Graphics2D) graphics.create();
                g2.setRenderingHint(
                    RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON
                );
                g2.setColor(Color.WHITE);
                g2.fillRoundRect(
                    0, 0, getWidth() - 1, getHeight() - 1, 18, 18
                );
                g2.setColor(new Color(225, 233, 242));
                g2.drawRoundRect(
                    0, 0, getWidth() - 1, getHeight() - 1, 18, 18
                );
                g2.setColor(accent);
                g2.fillRoundRect(0, 15, 4, getHeight() - 30, 4, 4);
                g2.dispose();
                super.paintComponent(graphics);
            }
        };
        card.setLayout(new BorderLayout(10, 0));
        card.setOpaque(false);
        card.setBorder(new EmptyBorder(13, 17, 12, 15));

        JLabel icon = new JLabel(iconText, SwingConstants.CENTER) {
            @Override
            protected void paintComponent(Graphics graphics) {
                Graphics2D g2 = (Graphics2D) graphics.create();
                g2.setRenderingHint(
                    RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON
                );
                g2.setColor(new Color(
                    Math.min(255, accent.getRed() + 105),
                    Math.min(255, accent.getGreen() + 105),
                    Math.min(255, accent.getBlue() + 105)
                ));
                g2.fillOval(0, 0, getWidth(), getHeight());
                g2.dispose();
                super.paintComponent(graphics);
            }
        };
        icon.setForeground(accent);
        icon.setFont(new Font("Poppins", Font.BOLD, 16));
        icon.setOpaque(false);
        icon.setPreferredSize(new Dimension(34, 34));

        JLabel titleLabel = new JLabel(title);
        titleLabel.setForeground(MUTED_TEXT);
        titleLabel.setFont(new Font("Poppins", Font.PLAIN, 10));

        value.setForeground(DARK_TEXT);
        value.setFont(new Font("Poppins", Font.BOLD, 20));

        JLabel descriptionLabel = new JLabel(description);
        descriptionLabel.setForeground(MUTED_TEXT);
        descriptionLabel.setFont(new Font("Poppins", Font.PLAIN, 9));

        JPanel labels = new JPanel();
        labels.setOpaque(false);
        labels.setLayout(new BoxLayout(labels, BoxLayout.Y_AXIS));
        labels.add(titleLabel);
        labels.add(Box.createVerticalStrut(3));
        labels.add(value);
        labels.add(Box.createVerticalStrut(1));
        labels.add(descriptionLabel);

        card.add(icon, BorderLayout.WEST);
        card.add(labels, BorderLayout.CENTER);
        return card;
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

    // Draw square card and hover states for the Home quick actions.
    JButton button = new JButton() {
        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);

            Graphics2D graphics =
                (Graphics2D) g.create();

            try {
                graphics.setRenderingHint(
                    RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON
                );
                graphics.setStroke(new BasicStroke(1f));

                graphics.setColor(
                    !featured && getModel().isRollover()
                        ? new Color(226, 239, 251)
                        : cardColor
                );

                graphics.fillRect(
                    0,
                    0,
                    getWidth(),
                    getHeight()
                );

                if (!featured) {
                    graphics.setColor(new Color(220, 229, 239));
                    graphics.drawRect(
                        0,
                        0,
                        getWidth() - 1,
                        getHeight() - 1
                    );
                }
            } finally {
                graphics.dispose();
            }
        }
    };

    button.setLayout(
        new BorderLayout(0, 10)
    );

    button.setOpaque(false);
    button.setContentAreaFilled(false);
    button.setBorderPainted(false);
    button.setRolloverEnabled(true);
    button.setToolTipText(description);

    // The square border above is also the keyboard focus indicator.
    button.setFocusPainted(false);

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

    JLabel icon = new JLabel(iconText, SwingConstants.CENTER) {
        @Override
        protected void paintComponent(Graphics graphics) {
            Graphics2D g2 = (Graphics2D) graphics.create();
            g2.setRenderingHint(
                RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_ON
            );
            g2.setColor(iconBackground);
            g2.fillRoundRect(0, 0, getWidth(), getHeight(), 10, 10);
            g2.dispose();
            super.paintComponent(graphics);
        }
    };

    icon.setFont(
        new Font(
            "SansSerif",
            Font.BOLD,
            18
        )
    );

    icon.setForeground(NAVY);
    icon.setOpaque(false);

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