import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.*;

public class AccountsPanel extends JPanel {

    private final JTextField codeField = new JTextField();
    private final JTextField nameField = new JTextField();

    private final JComboBox<String> typeBox = new JComboBox<>(
        new String[] {
            "ASSET",
            "LIABILITY",
            "EQUITY",
            "REVENUE",
            "EXPENSE"
        }
    );

    private final DefaultTableModel tableModel =
        new DefaultTableModel(
            new String[] {"ID", "Code", "Account Name", "Type"},
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
        heading.setFont(new Font("Arial", Font.BOLD, 24));
        heading.setForeground(new Color(15, 70, 125));

        // Account input form
        JPanel form = new JPanel(new GridLayout(0, 2, 12, 12));
        form.setOpaque(false);

        form.add(new JLabel("Account Code:"));
        form.add(codeField);

        form.add(new JLabel("Account Name:"));
        form.add(nameField);

        form.add(new JLabel("Account Type:"));
        form.add(typeBox);

        JButton addButton = new JButton("Add Account");
        JButton clearButton = new JButton("Clear Form");

        form.add(addButton);
        form.add(clearButton);

        JPanel top = new JPanel(new BorderLayout(0, 20));
        top.setOpaque(false);
        top.add(heading, BorderLayout.NORTH);
        top.add(form, BorderLayout.CENTER);

        // Saved accounts table
        JTable table = new JTable(tableModel);
        table.setRowHeight(26);
        table.setFillsViewportHeight(true);
        table.setAutoCreateRowSorter(true);
        table.getTableHeader().setReorderingAllowed(false);

        JButton refreshButton = new JButton("Refresh Accounts");

        JPanel bottom = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        bottom.setOpaque(false);
        bottom.add(refreshButton);

        add(top, BorderLayout.NORTH);
        add(new JScrollPane(table), BorderLayout.CENTER);
        add(bottom, BorderLayout.SOUTH);

        addButton.addActionListener(event -> addAccount());
        clearButton.addActionListener(event -> clearForm());
        refreshButton.addActionListener(event -> loadAccounts());

        loadAccounts();
    }

    private void addAccount() {
        String code = codeField.getText().trim();
        String name = nameField.getText().trim();
        String type = (String) typeBox.getSelectedItem();

        if (code.isEmpty() || name.isEmpty()) {
            JOptionPane.showMessageDialog(
                this,
                "Enter an account code and account name.",
                "Missing Information",
                JOptionPane.WARNING_MESSAGE
            );
            return;
        }

        String sql =
            "INSERT INTO accounts (code, name, type) VALUES (?, ?, ?)";

        try (
            Connection connection = Database.connect()
        ) {
            // Check whether the account code is already used.
            String checkSql = "SELECT 1 FROM accounts WHERE code = ?";

            try (
                PreparedStatement check =
                    connection.prepareStatement(checkSql)
            ) {
                check.setString(1, code);

                try (ResultSet result = check.executeQuery()) {
                    if (result.next()) {
                        JOptionPane.showMessageDialog(
                            this,
                            "That account code already exists.",
                            "Duplicate Account Code",
                            JOptionPane.WARNING_MESSAGE
                        );
                        return;
                    }
                }
            }

            try (
                PreparedStatement statement =
                    connection.prepareStatement(sql)
            ) {
                statement.setString(1, code);
                statement.setString(2, name);
                statement.setString(3, type);
                statement.executeUpdate();
            }

        } catch (SQLException exception) {
            showDatabaseError("Could not save the account.", exception);
            return;
        }

        clearForm();
        loadAccounts();

        JOptionPane.showMessageDialog(
            this,
            "Account saved successfully."
        );
    }

    private void loadAccounts() {
        String sql =
            "SELECT id, code, name, type FROM accounts ORDER BY code";

        // Read records first so a failed refresh preserves the table.
        java.util.List<Object[]> rows = new java.util.ArrayList<>();

        try (
            Connection connection = Database.connect();
            PreparedStatement statement =
                connection.prepareStatement(sql);
            ResultSet result = statement.executeQuery()
        ) {
            while (result.next()) {
                rows.add(new Object[] {
                    result.getInt("id"),
                    result.getString("code"),
                    result.getString("name"),
                    result.getString("type")
                });
            }

        } catch (SQLException exception) {
            showDatabaseError("Could not load accounts.", exception);
            return;
        }

        tableModel.setRowCount(0);

        for (Object[] row : rows) {
            tableModel.addRow(row);
        }
    }

    private void clearForm() {
        codeField.setText("");
        nameField.setText("");
        typeBox.setSelectedIndex(0);
        codeField.requestFocusInWindow();
    }

    private void showDatabaseError(
        String message,
        SQLException exception
    ) {
        exception.printStackTrace();

        JOptionPane.showMessageDialog(
            this,
            message + "\n" + exception.getMessage(),
            "Database Error",
            JOptionPane.ERROR_MESSAGE
        );
    }
}