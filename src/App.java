import javax.swing.*;
import javax.swing.border.MatteBorder;
import java.awt.*;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class App {

    private static final Color BLUE = new Color(22, 56, 102);
    private static final Color TAB_IDLE = new Color(160, 180, 205);
    private static final Color TAB_ACTIVE = new Color(60, 143, 200);
    private static final Color TAB_TEXT = new Color(20, 40, 70);

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

        // ----- Header (replaces the sidebar title) -----
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(BLUE);
        header.setBorder(
            BorderFactory.createEmptyBorder(8, 15, 8, 15)
        );

        JLabel title = new JLabel(
            "Accounting System",
            SwingConstants.CENTER
        );
        title.setForeground(Color.WHITE);
        title.setFont(new Font("Inter", Font.BOLD, 28));

        // ----- User area (same user label + logout as before) -----
        JPanel userArea = new JPanel(
            new FlowLayout(FlowLayout.RIGHT, 8, 0)
        );
        userArea.setOpaque(false);

        JLabel userLabel = new JLabel("User: " + username);
        userLabel.setForeground(Color.WHITE);

        JButton logoutButton = new JButton("Logout");

        logoutButton.addActionListener(event -> {
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
        });

        userArea.add(userLabel);
        userArea.add(logoutButton);

        JPanel spacer = new JPanel();
        spacer.setOpaque(false);
        spacer.setPreferredSize(userArea.getPreferredSize());

        header.add(spacer, BorderLayout.WEST);
        header.add(title, BorderLayout.CENTER);
        header.add(userArea, BorderLayout.EAST);

        // ----- Content (unchanged) -----
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

        content.add(transactionPanel, "New Transaction");
        content.add(transactionsPanel, "Transactions");
        content.add(accountsPanel, "Accounts");
        content.add(journalPanel, "General Journal");
        content.add(ledgerPanel, "General Ledger");
        content.add(balanceSheetPanel, "Balance Sheet");

        // ----- Navigation: same screens and logic, now a top tab bar -----
        // Order follows the mockup (left to right).
        String[] screens = {
            "Balance Sheet",
            "General Ledger",
            "General Journal",
            "Accounts",
            "Transactions",
            "New Transaction"
        };

        JPanel tabBar = new JPanel(new GridLayout(1, 0, 0, 0));
        tabBar.setPreferredSize(new Dimension(0, 42));

        List<JButton> tabs = new ArrayList<>();

        for (String screenName : screens) {
            JButton button = new JButton(screenName);
            button.setFont(new Font("Inter", Font.PLAIN, 13));
            button.setFocusPainted(false);
            button.setOpaque(true);
            button.setContentAreaFilled(true);
            button.setBackground(TAB_IDLE);
            button.setForeground(TAB_TEXT);
            button.setBorder(new MatteBorder(0, 0, 0, 1, BLUE));
            button.setName(screenName);

            button.addActionListener(event -> {
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
                }

                cardLayout.show(content, screenName);
                highlight(tabs, button);
            });

            tabs.add(button);
            tabBar.add(button);
        }

        JPanel top = new JPanel(new BorderLayout());
        top.add(header, BorderLayout.NORTH);
        top.add(tabBar, BorderLayout.CENTER);

        window.add(top, BorderLayout.NORTH);
        window.add(content, BorderLayout.CENTER);

        cardLayout.show(content, "Accounts");
        for (JButton tab : tabs) {
            if ("Accounts".equals(tab.getName())) {
                highlight(tabs, tab);
            }
        }

        window.setVisible(true);
    }

    private static void highlight(List<JButton> tabs, JButton active) {
        for (JButton tab : tabs) {
            boolean isActive = (tab == active);
            tab.setBackground(isActive ? TAB_ACTIVE : TAB_IDLE);
            tab.setForeground(isActive ? Color.WHITE : TAB_TEXT);
        }
    }
}