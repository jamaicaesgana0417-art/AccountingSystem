import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.*;

/** Read-only chart of accounts (the account list is fixed for everyone). */
public class AccountsPanel extends JPanel {

    private static final Color PAGE_BG = new Color(244, 246, 255);
    private static final Color BORDER = new Color(221, 228, 245);
    private static final Color TEXT = new Color(31, 50, 91);
    private static final Color MUTED = new Color(104, 120, 153);
    private static final Color BLUE = new Color(55, 91, 255);

    private final DefaultTableModel tableModel =
        new DefaultTableModel(
            new String[] {"Code", "Account Name", "Type"},
            0
        ) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

    private final JTable table = new JTable(tableModel);
    private final JLabel countLabel = new JLabel("0 accounts");

    public AccountsPanel() {
        setLayout(new BorderLayout(0, 24));
        setBackground(PAGE_BG);
        setBorder(BorderFactory.createEmptyBorder(30, 30, 24, 30));

        JLabel heading = new JLabel("CHART OF ACCOUNTS");
        heading.setFont(new Font("Inter", Font.BOLD, 24));
        heading.setForeground(TEXT);

        JLabel subtitle = new JLabel(
            "Browse your active accounts by code, name, and category."
        );
        subtitle.setFont(new Font("Inter", Font.PLAIN, 11));
        subtitle.setForeground(MUTED);

        JPanel titleBlock = new JPanel();
        titleBlock.setOpaque(false);
        titleBlock.setLayout(new BoxLayout(titleBlock, BoxLayout.Y_AXIS));
        titleBlock.add(heading);
        titleBlock.add(Box.createVerticalStrut(7));
        titleBlock.add(subtitle);

        table.setRowHeight(34);
        table.setFillsViewportHeight(true);
        table.setAutoCreateRowSorter(true);
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
        table.getColumnModel().getColumn(0).setPreferredWidth(110);
        table.getColumnModel().getColumn(1).setPreferredWidth(420);
        table.getColumnModel().getColumn(2).setPreferredWidth(200);

        JButton refreshButton = new JButton("Refresh Accounts");
        refreshButton.setFont(new Font("Inter", Font.PLAIN, 11));
        refreshButton.setForeground(Color.WHITE);
        refreshButton.setBackground(BLUE);
        refreshButton.setFocusPainted(false);
        refreshButton.setBorder(
            BorderFactory.createEmptyBorder(8, 14, 8, 14)
        );
        refreshButton.setCursor(
            Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)
        );

        JPanel bottom = new JPanel(new BorderLayout());
        bottom.setOpaque(false);
        countLabel.setFont(new Font("Inter", Font.PLAIN, 10));
        countLabel.setForeground(MUTED);
        bottom.add(countLabel, BorderLayout.WEST);
        bottom.add(refreshButton, BorderLayout.EAST);

        JScrollPane scroll = new JScrollPane(table);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.getViewport().setBackground(Color.WHITE);

        JPanel tableCard = new JPanel(new BorderLayout());
        tableCard.setBackground(Color.WHITE);
        tableCard.setBorder(BorderFactory.createLineBorder(BORDER, 1, true));
        tableCard.add(table.getTableHeader(), BorderLayout.NORTH);
        tableCard.add(scroll, BorderLayout.CENTER);

        add(titleBlock, BorderLayout.NORTH);
        add(tableCard, BorderLayout.CENTER);
        add(bottom, BorderLayout.SOUTH);

        refreshButton.addActionListener(event -> refreshAccounts());

        refreshAccounts();
    }

    public void refreshAccounts() {
        String sql = """
            SELECT code, name, type
            FROM accounts
            WHERE owner_username = ? AND active = 1
            ORDER BY code
            """;

        java.util.List<Object[]> rows = new java.util.ArrayList<>();

        try (
            Connection connection = Database.connect();
            PreparedStatement statement =
                connection.prepareStatement(sql)
        ) {
            statement.setString(1, Session.getCurrentUser());

            try (ResultSet result = statement.executeQuery()) {
                while (result.next()) {
                    rows.add(new Object[] {
                        result.getString("code"),
                        result.getString("name"),
                        result.getString("type")
                    });
                }
            }
        } catch (SQLException exception) {
            exception.printStackTrace();
            JOptionPane.showMessageDialog(
                this,
                "Could not load accounts.\n" + exception.getMessage(),
                "Database Error",
                JOptionPane.ERROR_MESSAGE
            );
            return;
        }

        tableModel.setRowCount(0);

        for (Object[] row : rows) {
            tableModel.addRow(row);
        }

        countLabel.setText(rows.size() + " active accounts");
    }
}
