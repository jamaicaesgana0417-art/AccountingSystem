import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.DecimalFormat;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.LineBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.JTextComponent;

public class NewTransactionPanel extends JPanel {

    private JTextField dateField;
    private JTextArea descriptionField;
    private JComboBox<NewTransactionPanelAccountOption> debitBox;
    private JComboBox<NewTransactionPanelAccountOption> creditBox;
    private JTextField amountField;
    private JButton postButton;
    private JButton clearButton;

    private JLabel dateErrorLabel;
    private JLabel descriptionErrorLabel;
    private JLabel accountsErrorLabel;
    private JLabel amountErrorLabel;

    private final List<NewTransactionPanelAccountOption> allAccounts = new ArrayList<>();
    private final Border defaultBorder = UIManager.getBorder("TextField.border");
    private final Border errorBorder = new LineBorder(Color.RED, 1);
    private boolean formattingAmount = false;

    public NewTransactionPanel() {
        setLayout(new BorderLayout());
        initUI();
        setupShortcuts();
    }

    private void initUI() {
        JPanel formPanel = new JPanel(new GridBagLayout());
        formPanel.setBorder(BorderFactory.createEmptyBorder(40, 60, 40, 60));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 15, 6, 15);
        gbc.anchor = GridBagConstraints.WEST;

        // Row 0: Date
        gbc.gridx = 0; gbc.gridy = 0; gbc.weightx = 0.0; gbc.fill = GridBagConstraints.NONE;
        formPanel.add(new JLabel("Date (YYYY-MM-DD):"), gbc);
        dateField = new JTextField();
        dateField.setText(LocalDate.now().toString());
        gbc.gridx = 1; gbc.weightx = 1.0; gbc.fill = GridBagConstraints.HORIZONTAL;
        formPanel.add(dateField, gbc);

        // Row 1: Date Error
        dateErrorLabel = createErrorLabel();
        gbc.gridx = 1; gbc.gridy = 1; gbc.weightx = 1.0; gbc.fill = GridBagConstraints.HORIZONTAL;
        formPanel.add(dateErrorLabel, gbc);

        // Row 2: Description
        gbc.gridx = 0; gbc.gridy = 2; gbc.weightx = 0.0; gbc.fill = GridBagConstraints.NONE;
        formPanel.add(new JLabel("Description:"), gbc);
        descriptionField = new JTextArea(4, 20);
        descriptionField.setLineWrap(true);
        descriptionField.setWrapStyleWord(true);
        JScrollPane descScroll = new JScrollPane(descriptionField);
        gbc.gridx = 1; gbc.weightx = 1.0; gbc.fill = GridBagConstraints.HORIZONTAL;
        formPanel.add(descScroll, gbc);

        // Row 3: Description Error
        descriptionErrorLabel = createErrorLabel();
        gbc.gridx = 1; gbc.gridy = 3; gbc.weightx = 1.0; gbc.fill = GridBagConstraints.HORIZONTAL;
        formPanel.add(descriptionErrorLabel, gbc);

        // Row 4: Debit Account
        gbc.gridx = 0; gbc.gridy = 4; gbc.weightx = 0.0; gbc.fill = GridBagConstraints.NONE;
        formPanel.add(new JLabel("Debit Account:"), gbc);
        debitBox = new JComboBox<>();
        makeComboBoxSearchable(debitBox);
        gbc.gridx = 1; gbc.weightx = 1.0; gbc.fill = GridBagConstraints.HORIZONTAL;
        formPanel.add(debitBox, gbc);

        // Row 5: Credit Account
        gbc.gridx = 0; gbc.gridy = 5; gbc.weightx = 0.0; gbc.fill = GridBagConstraints.NONE;
        formPanel.add(new JLabel("Credit Account:"), gbc);
        creditBox = new JComboBox<>();
        makeComboBoxSearchable(creditBox);
        gbc.gridx = 1; gbc.weightx = 1.0; gbc.fill = GridBagConstraints.HORIZONTAL;
        formPanel.add(creditBox, gbc);

        // Row 6: Accounts Error
        accountsErrorLabel = createErrorLabel();
        gbc.gridx = 1; gbc.gridy = 6; gbc.weightx = 1.0; gbc.fill = GridBagConstraints.HORIZONTAL;
        formPanel.add(accountsErrorLabel, gbc);

        // Row 7: Amount
        gbc.gridx = 0; gbc.gridy = 7; gbc.weightx = 0.0; gbc.fill = GridBagConstraints.NONE;
        formPanel.add(new JLabel("Amount (PHP):"), gbc);
        amountField = new JTextField();
        setupCurrencyFormatting(amountField);
        gbc.gridx = 1; gbc.weightx = 1.0; gbc.fill = GridBagConstraints.HORIZONTAL;
        formPanel.add(amountField, gbc);

        // Row 8: Amount Error
        amountErrorLabel = createErrorLabel();
        gbc.gridx = 1; gbc.gridy = 8; gbc.weightx = 1.0; gbc.fill = GridBagConstraints.HORIZONTAL;
        formPanel.add(amountErrorLabel, gbc);

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 20, 20));
        buttonPanel.setBorder(BorderFactory.createEmptyBorder(10, 40, 20, 40));
        clearButton = new JButton("Clear Form");
        postButton = new JButton("Confirm and Post");

        clearButton.addActionListener(e -> clearForm());
        postButton.addActionListener(e -> postTransaction());

        buttonPanel.add(clearButton);
        buttonPanel.add(postButton);

        add(formPanel, BorderLayout.CENTER);
        add(buttonPanel, BorderLayout.SOUTH);
    }

    private void makeComboBoxSearchable(JComboBox<NewTransactionPanelAccountOption> comboBox) {
        comboBox.setEditable(true);
        JTextComponent editor = (JTextComponent) comboBox.getEditor().getEditorComponent();

        editor.addFocusListener(new FocusAdapter() {
            @Override
            public void focusGained(FocusEvent e) {
                SwingUtilities.invokeLater(() -> {
                    if (!comboBox.isPopupVisible()) {
                        comboBox.showPopup();
                    }
                });
            }
        });

        editor.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                SwingUtilities.invokeLater(() -> {
                    if (!comboBox.isPopupVisible()) {
                        comboBox.showPopup();
                    }
                });
            }
        });
        
        editor.addKeyListener(new KeyAdapter() {
            @Override
            public void keyReleased(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER || e.getKeyCode() == KeyEvent.VK_UP || e.getKeyCode() == KeyEvent.VK_DOWN) {
                    return;
                }
                String text = editor.getText();
                SwingUtilities.invokeLater(() -> {
                    comboBox.removeAllItems();
                    for (NewTransactionPanelAccountOption opt : allAccounts) {
                        if (opt.toString().toLowerCase().contains(text.toLowerCase())) {
                            comboBox.addItem(opt);
                        }
                    }
                    editor.setText(text);
                    if (!comboBox.isPopupVisible()) {
                        comboBox.showPopup();
                    }
                });
            }
        });
    }

    private void setupCurrencyFormatting(JTextField field) {
        field.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) { formatLive(); }
            @Override
            public void removeUpdate(DocumentEvent e) { formatLive(); }
            @Override
            public void changedUpdate(DocumentEvent e) { formatLive(); }

            private void formatLive() {
                if (formattingAmount) return;
                SwingUtilities.invokeLater(() -> {
                    if (formattingAmount) return;
                    String text = field.getText();
                    String cleanText = text.replaceAll("[^0-9]", "");
                    if (cleanText.isEmpty()) {
                        if (!text.isEmpty()) {
                            formattingAmount = true;
                            field.setText("");
                            formattingAmount = false;
                        }
                        return;
                    }

                    try {
                        long parsed = Long.parseLong(cleanText);
                        DecimalFormat df = new DecimalFormat("#,##0");
                        String formatted = df.format(parsed);

                        if (!formatted.equals(text)) {
                            formattingAmount = true;
                            field.setText(formatted);
                            field.setCaretPosition(field.getText().length());
                            formattingAmount = false;
                        }
                    } catch (Exception ignored) {
                        formattingAmount = false;
                    }
                });
            }
        });
    }

    private void setupShortcuts() {
        InputMap inputMap = getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
        ActionMap actionMap = getActionMap();

        KeyStroke ctrlEnter = KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, ActionEvent.CTRL_MASK);
        inputMap.put(ctrlEnter, "postTransaction");
        actionMap.put("postTransaction", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (postButton.isEnabled()) {
                    postTransaction();
                }
            }
        });
    }

    private JLabel createErrorLabel() {
        JLabel label = new JLabel();
        label.setForeground(Color.RED);
        label.setFont(new Font("SansSerif", Font.PLAIN, 11));
        label.setVisible(false);
        return label;
    }

    private void clearInlineErrors() {
        resetFieldError(dateField, dateErrorLabel);
        resetFieldError(descriptionField, descriptionErrorLabel);
        resetFieldError(debitBox, accountsErrorLabel);
        resetFieldError(creditBox, accountsErrorLabel);
        resetFieldError(amountField, amountErrorLabel);
    }

    private void setFieldError(JComponent comp, JLabel errorLabel, String message) {
        comp.setBorder(errorBorder);
        errorLabel.setText(message);
        errorLabel.setVisible(true);
    }

    private void resetFieldError(JComponent comp, JLabel errorLabel) {
        comp.setBorder(defaultBorder);
        errorLabel.setVisible(false);
    }

    public void refreshAccounts() {
        allAccounts.clear();
        debitBox.removeAllItems();
        creditBox.removeAllItems();
        try (Connection conn = Database.connect();
             PreparedStatement stmt = conn.prepareStatement("SELECT id, name FROM accounts WHERE active = 1");
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                NewTransactionPanelAccountOption opt = new NewTransactionPanelAccountOption(rs.getInt("id"), rs.getString("name"));
                allAccounts.add(opt);
                debitBox.addItem(opt);
                creditBox.addItem(opt);
            }
        } catch (SQLException e) {
            // Handled silently
        }
    }

    private void postTransaction() {
        clearInlineErrors();
        boolean hasError = false;

        LocalDate date = null;
        BigDecimal amount = null;
        long centavos = 0;

        String description = descriptionField.getText().trim();
        NewTransactionPanelAccountOption debit = (NewTransactionPanelAccountOption) debitBox.getSelectedItem();
        NewTransactionPanelAccountOption credit = (NewTransactionPanelAccountOption) creditBox.getSelectedItem();

        try {
            date = LocalDate.parse(dateField.getText().trim());
        } catch (DateTimeParseException ex) {
            setFieldError(dateField, dateErrorLabel, "Invalid date format. Use YYYY-MM-DD.");
            hasError = true;
        }

        if (description.isEmpty()) {
            setFieldError(descriptionField, descriptionErrorLabel, "Description cannot be empty.");
            hasError = true;
        }

        if (debit == null || credit == null) {
            setFieldError(debitBox, accountsErrorLabel, "Select both debit and credit accounts.");
            hasError = true;
        } else if (debit.id == credit.id) {
            setFieldError(creditBox, accountsErrorLabel, "Debit and credit accounts must be different.");
            hasError = true;
        }

        String rawAmount = amountField.getText().trim().replaceAll(",", "");
        try {
            amount = new BigDecimal(rawAmount).setScale(2, RoundingMode.UNNECESSARY);
            centavos = amount.movePointRight(2).longValueExact();
            if (centavos <= 0) {
                setFieldError(amountField, amountErrorLabel, "Amount must be greater than zero.");
                hasError = true;
            }
        } catch (NumberFormatException | ArithmeticException ex) {
            setFieldError(amountField, amountErrorLabel, "Enter a valid amount (e.g. 1,500.00).");
            hasError = true;
        }

        if (hasError) return;

        int answer = JOptionPane.showConfirmDialog(
            this,
            "Date: " + date +
            "\nDescription: " + description +
            "\nDebit: " + debit +
            "\nCredit: " + credit +
            "\nAmount: PHP " + amount.toPlainString() +
            "\n\nPost this transaction?",
            "Confirm Transaction",
            JOptionPane.YES_NO_OPTION
        );

        if (answer != JOptionPane.YES_OPTION) {
            return;
        }

        postButton.setEnabled(false);
        try {
            long transactionId = saveTransaction(date, description, debit.id, credit.id, centavos);
            clearForm();
            JOptionPane.showMessageDialog(this, "Transaction #" + transactionId + " posted successfully.");
        } catch (SQLException ex) {
            showError("Could not post the transaction.", ex);
        } finally {
            postButton.setEnabled(debitBox.getItemCount() >= 2);
        }
    }

    private long saveTransaction(LocalDate date, String description, int debitId, int creditId, long centavos) throws SQLException {
        try (Connection connection = Database.connect()) {
            connection.setAutoCommit(false);
            try {
                String checkSql = "SELECT COUNT(*) FROM accounts WHERE active = 1 AND id IN (?, ?)";
                try (PreparedStatement check = connection.prepareStatement(checkSql)) {
                    check.setInt(1, debitId);
                    check.setInt(2, creditId);
                    try (ResultSet result = check.executeQuery()) {
                        if (!result.next() || result.getInt(1) != 2) {
                            throw new SQLException("Select two different active accounts.");
                        }
                    }
                }

                String transactionSql = "INSERT INTO transactions (transaction_date, description) VALUES (?, ?) RETURNING id";
                long transactionId;
                try (PreparedStatement statement = connection.prepareStatement(transactionSql)) {
                    statement.setString(1, date.toString());
                    statement.setString(2, description);
                    try (ResultSet result = statement.executeQuery()) {
                        if (!result.next()) {
                            throw new SQLException("Could not retrieve transaction ID.");
                        }
                        transactionId = result.getLong("id");
                    }
                }

                String lineSql = "INSERT INTO journal_lines (transaction_id, account_id, debit_centavos, credit_centavos) VALUES (?, ?, ?, ?)";
                try (PreparedStatement statement = connection.prepareStatement(lineSql)) {
                    statement.setLong(1, transactionId);
                    statement.setInt(2, debitId);
                    statement.setLong(3, centavos);
                    statement.setLong(4, 0);
                    statement.executeUpdate();

                    statement.setLong(1, transactionId);
                    statement.setInt(2, creditId);
                    statement.setLong(3, 0);
                    statement.setLong(4, centavos);
                    statement.executeUpdate();
                }

                connection.commit();
                return transactionId;
            } catch (SQLException ex) {
                try { connection.rollback(); } catch (SQLException rollbackEx) { ex.addSuppressed(rollbackEx); }
                throw ex;
            }
        }
    }

    private void clearForm() {
        dateField.setText(LocalDate.now().toString());
        descriptionField.setText("");
        amountField.setText("");
        debitBox.setSelectedIndex(-1);
        creditBox.setSelectedIndex(-1);
        clearInlineErrors();
    }

    private void showError(String message, Exception ex) {
        JOptionPane.showMessageDialog(this, message + "\n" + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
    }
}