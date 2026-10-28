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

    private final JTextField searchField = new JTextField(16);

    private final JComboBox<String> filterTypeBox =
        new JComboBox<>(FilterSupport.typeChoices());

    private final JLabel countLabel = new JLabel(" ");

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

    private final JTable table = new JTable(tableModel);

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

        // Search and filter the saved accounts list
        JButton applyButton = new JButton("Apply Filters");
        JButton clearFilterButton = new JButton("Clear Filters");

        JPanel filterBar = new JPanel(new FlowLayout(FlowLayout.LEFT));
        filterBar.setOpaque(false);
        filterBar.add(new JLabel("Search (code, name, type):"));
        filterBar.add(searchField);
        filterBar.add(new JLabel("Type:"));
        filterBar.add(filterTypeBox);
        filterBar.add(applyButton);
        filterBar.add(clearFilterButton);

        JPanel formAndFilters = new JPanel(new BorderLayout(0, 10));
        formAndFilters.setOpaque(false);
        formAndFilters.add(form, BorderLayout.NORTH);
        formAndFilters.add(filterBar, BorderLayout.SOUTH);

        JPanel top = new JPanel(new BorderLayout(0, 20));
        top.setOpaque(false);
        top.add(heading, BorderLayout.NORTH);
        top.add(formAndFilters, BorderLayout.CENTER);

        // Saved accounts table
        table.setRowHeight(26);
        table.setFillsViewportHeight(true);
        table.setAutoCreateRowSorter(true);
        table.getTableHeader().setReorderingAllowed(false);

        JButton refreshButton = new JButton("Refresh Accounts");
        JButton exportButton = new JButton("Export CSV");

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttons.setOpaque(false);
        buttons.add(exportButton);
        buttons.add(refreshButton);

        JPanel bottom = new JPanel(new BorderLayout());
        bottom.setOpaque(false);
        bottom.add(countLabel, BorderLayout.WEST);
        bottom.add(buttons, BorderLayout.EAST);

        add(top, BorderLayout.NORTH);
        add(new JScrollPane(table), BorderLayout.CENTER);
        add(bottom, BorderLayout.SOUTH);

        addButton.addActionListener(event -> addAccount());
        clearButton.addActionListener(event -> clearForm());
        refreshButton.addActionListener(event -> loadAccounts());

        applyButton.addActionListener(event -> loadAccounts());
        searchField.addActionListener(event -> loadAccounts());

        clearFilterButton.addActionListener(event -> {
            searchField.setText("");
            filterTypeBox.setSelectedIndex(0);
            loadAccounts();
        });

        exportButton.addActionListener(event ->
            CsvExporter.export(this, table, "chart_of_accounts.csv")
        );

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
        FilterSupport.Query query = buildQuery(
            searchField.getText(),
            FilterSupport.typeOrNull(filterTypeBox.getSelectedItem())
        );

        // Read records first so a failed refresh preserves the table.
        java.util.List<Object[]> rows = new java.util.ArrayList<>();

        try (
            Connection connection = Database.connect();
            PreparedStatement statement =
                connection.prepareStatement(query.sql)
        ) {
            FilterSupport.bind(statement, query.params);

            try (ResultSet result = statement.executeQuery()) {
                while (result.next()) {
                    rows.add(new Object[] {
                        result.getInt("id"),
                        result.getString("code"),
                        result.getString("name"),
                        result.getString("type")
                    });
                }
            }

        } catch (SQLException exception) {
            showDatabaseError("Could not load accounts.", exception);
            return;
        }

        tableModel.setRowCount(0);

        for (Object[] row : rows) {
            tableModel.addRow(row);
        }

        countLabel.setText("Displayed accounts: " + rows.size());
    }

    /**
     * Builds the Chart of Accounts query (package-private + static so it can
     * be tested without a window or database). Keyword matches code, name,
     * or type; the type filter is exact; both are combined with AND.
     */
    static FilterSupport.Query buildQuery(
        String keyword,
        String accountType
    ) {
        StringBuilder sql = new StringBuilder(
            "SELECT id, code, name, type FROM accounts WHERE 1 = 1 "
        );

        java.util.List<Object> params = FilterSupport.newParams();

        FilterSupport.appendEquals(sql, params, "type", accountType);

        FilterSupport.appendKeyword(
            sql, params, keyword, "code", "name", "type"
        );

        sql.append(" ORDER BY code ");

        return new FilterSupport.Query(sql.toString(), params);
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