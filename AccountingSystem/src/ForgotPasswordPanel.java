import java.awt.*;
import java.util.Arrays;
import javax.swing.*;

public class ForgotPasswordPanel extends JPanel {

    private final JTextField usernameField = Theme.textField("Username");
    private final JTextField codeField = Theme.textField("Recovery code (XXXX-XXXX-XXXX)");
    private final JPasswordField newPassField = Theme.passwordField("New password (8+ characters)");
    private final JPasswordField confirmField = Theme.passwordField("Confirm new password");
    private final JCheckBox showPassword = Ui.checkBox("Show password");
    private final JButton resetButton = Theme.primaryButton("Reset password");
    private final JButton backButton = Theme.secondaryButton("Back to login");
    private final JLabel messageLabel = new JLabel(" ");

    private Runnable onBackToLogin = () -> {};
    private Runnable onResetDone = () -> {};

    public ForgotPasswordPanel() {
        setLayout(new GridBagLayout());
        setBackground(Theme.BG);

        JPanel card = Theme.card();
        card.setLayout(new GridBagLayout());
        card.setBorder(BorderFactory.createEmptyBorder(32, 32, 32, 32));

        JLabel title = new JLabel("Reset password");
        title.setFont(Theme.HEADING);
        title.setForeground(Theme.TEXT);

        JLabel subtitle = new JLabel(
            "<html><div style='width:290px'>Use the recovery code you got "
            + "when you created your account.</div></html>"
        );
        subtitle.setFont(Theme.BODY);
        subtitle.setForeground(Theme.MUTED);

        messageLabel.setFont(Theme.BODY.deriveFont(13f));
        messageLabel.setForeground(Theme.ERROR);

        Ui.bindShowPassword(showPassword, newPassField, confirmField);

        GridBagConstraints c = new GridBagConstraints();
        c.gridx = 0;
        c.weightx = 1;
        c.fill = GridBagConstraints.HORIZONTAL;

        c.gridy = 0; c.insets = new Insets(0, 0, 4, 0);  card.add(title, c);
        c.gridy = 1; c.insets = new Insets(0, 0, 20, 0); card.add(subtitle, c);
        c.gridy = 2; c.insets = new Insets(0, 0, 12, 0); card.add(usernameField, c);
        c.gridy = 3; c.insets = new Insets(0, 0, 12, 0); card.add(codeField, c);
        c.gridy = 4; c.insets = new Insets(0, 0, 12, 0); card.add(newPassField, c);
        c.gridy = 5; c.insets = new Insets(0, 0, 6, 0);  card.add(confirmField, c);
        c.gridy = 6; c.insets = new Insets(0, 0, 6, 0);  card.add(showPassword, c);
        c.gridy = 7; c.insets = new Insets(0, 2, 12, 0); card.add(messageLabel, c);
        c.gridy = 8; c.insets = new Insets(0, 0, 18, 0); card.add(resetButton, c);
        c.gridy = 9; c.insets = new Insets(0, 0, 0, 0);  card.add(backButton, c);

        add(card);

        resetButton.addActionListener(e -> doReset());
        confirmField.addActionListener(e -> doReset());
        usernameField.addActionListener(e -> codeField.requestFocusInWindow());
        codeField.addActionListener(e -> newPassField.requestFocusInWindow());
        newPassField.addActionListener(e -> confirmField.requestFocusInWindow());
        backButton.addActionListener(e -> {
            clearAll();
            onBackToLogin.run();
        });
    }

    public void setOnBackToLogin(Runnable callback) { this.onBackToLogin = callback; }
    public void setOnResetDone(Runnable callback) { this.onResetDone = callback; }

    private void clearAll() {
        usernameField.setText("");
        codeField.setText("");
        newPassField.setText("");
        confirmField.setText("");
        messageLabel.setText(" ");
    }

    private void doReset() {
        String username = usernameField.getText().trim();
        String code = codeField.getText().trim();
        char[] password = newPassField.getPassword();
        char[] confirm = confirmField.getPassword();

        String problem = null;
        if (username.isEmpty() || code.isEmpty() || password.length == 0) {
            problem = "Please fill in all the fields.";
        } else if (password.length < 8) {
            problem = "New password needs at least 8 characters.";
        } else if (!Arrays.equals(password, confirm)) {
            problem = "Passwords do not match.";
        }

        Arrays.fill(confirm, ' ');

        if (problem != null) {
            Arrays.fill(password, ' ');
            showError(problem);
            return;
        }

        resetButton.setEnabled(false);
        messageLabel.setForeground(Theme.MUTED);
        messageLabel.setText("Resetting...");

        new SwingWorker<String, Void>() {
            @Override
            protected String doInBackground() throws Exception {
                try {
                    return RecoveryService.resetPassword(username, code, password);
                } finally {
                    Arrays.fill(password, ' ');
                }
            }

            @Override
            protected void done() {
                resetButton.setEnabled(true);
                try {
                    String newCode = get();
                    if (newCode == null) {
                        showError("Username or recovery code is incorrect.");
                        return;
                    }
                    clearAll();
                    Ui.showRecoveryCode(ForgotPasswordPanel.this, newCode);
                    onResetDone.run();
                } catch (Exception ex) {
                    Throwable cause = (ex.getCause() != null) ? ex.getCause() : ex;
                    if (cause instanceof IllegalArgumentException) {
                        showError(cause.getMessage());
                    } else {
                        showError("Could not reset password. Check your connection.");
                        ex.printStackTrace();
                    }
                }
            }
        }.execute();
    }

    private void showError(String message) {
        messageLabel.setForeground(Theme.ERROR);
        messageLabel.setText("<html><div style='width:280px'>" + message + "</div></html>");
    }
}