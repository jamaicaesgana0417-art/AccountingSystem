import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.*;

/** Read-only chart of accounts (the account list is fixed for everyone). */
public class AccountsPanel extends JPanel {

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

    public AccountsPanel() {
        setLayout(new BorderLayout(0, 20));
        setBackground(Color.WHITE);
        setBorder(
            BorderFactory.createEmptyBorder(30, 30, 30, 30)
        );

        JLabel heading = new JLabel("Chart of Accounts");
        heading.setFont(new Font("Inter", Font.BOLD, 24));
        heading.setForeground(new Color(15, 70, 125));

        JTable table = new JTable(tableModel);
        table.setRowHeight(26);
        table.setFillsViewportHeight(true);
        table.setAutoCreateRowSorter(true);
        table.getTableHeader().setReorderingAllowed(false);

        JButton refreshButton = new JButton("Refresh Accounts");

        JPanel bottom = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        bottom.setOpaque(false);
        bottom.add(refreshButton);

        add(heading, BorderLayout.NORTH);
        add(new JScrollPane(table), BorderLayout.CENTER);
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
    }
}
