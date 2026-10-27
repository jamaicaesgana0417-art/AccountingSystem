import java.awt.*;
import java.util.Arrays;
import javax.swing.*;

public class SignUpPanel extends JPanel {

    private final JTextField usernameField = Theme.textField("Username");
    private final JPasswordField passwordField = Theme.passwordField("Password (8+ characters)");
    private final JPasswordField confirmField = Theme.passwordField("Confirm password");
    private final JCheckBox showPassword = Ui.checkBox("Show password");
    private final JButton signUpButton = Theme.primaryButton("Create account");
    private final JButton backButton = Theme.secondaryButton("Back to login");
    private final JLabel messageLabel = new JLabel(" ");

    private Runnable onSignUpSuccess = () -> {};
    private Runnable onBackToLogin = () -> {};

    public SignUpPanel() {
        setLayout(new GridBagLayout());
        setBackground(Theme.BG);

        JPanel card = Theme.card();
        card.setLayout(new GridBagLayout());
        card.setBorder(BorderFactory.createEmptyBorder(32, 32, 32, 32));

        JLabel title = new JLabel("Create account");
        title.setFont(Theme.HEADING);
        title.setForeground(Theme.TEXT);

        JLabel subtitle = new JLabel("Join the accounting system");
        subtitle.setFont(Theme.BODY);
        subtitle.setForeground(Theme.MUTED);

        messageLabel.setFont(Theme.BODY.deriveFont(13f));
        messageLabel.setForeground(Theme.ERROR);

        Ui.bindShowPassword(showPassword, passwordField, confirmField);

        GridBagConstraints c = new GridBagConstraints();
        c.gridx = 0;
        c.weightx = 1;
        c.fill = GridBagConstraints.HORIZONTAL;

        c.gridy = 0; c.insets = new Insets(0, 0, 4, 0);  card.add(title, c);
        c.gridy = 1; c.insets = new Insets(0, 0, 22, 0); card.add(subtitle, c);
        c.gridy = 2; c.insets = new Insets(0, 0, 12, 0); card.add(usernameField, c);
        c.gridy = 3; c.insets = new Insets(0, 0, 12, 0); card.add(passwordField, c);
        c.gridy = 4; c.insets = new Insets(0, 0, 6, 0);  card.add(confirmField, c);
        c.gridy = 5; c.insets = new Insets(0, 0, 6, 0);  card.add(showPassword, c);
        c.gridy = 6; c.insets = new Insets(0, 2, 12, 0); card.add(messageLabel, c);
        c.gridy = 7; c.insets = new Insets(0, 0, 18, 0); card.add(signUpButton, c);
        c.gridy = 8; c.insets = new Insets(0, 0, 0, 0);  card.add(backButton, c);

        add(card);

        signUpButton.addActionListener(e -> doSignUp());
        confirmField.addActionListener(e -> doSignUp());
        usernameField.addActionListener(e -> passwordField.requestFocusInWindow());
        passwordField.addActionListener(e -> confirmField.requestFocusInWindow());
        backButton.addActionListener(e -> {
            clearAll();
            onBackToLogin.run();
        });
    }

    public void setOnSignUpSuccess(Runnable callback) { this.onSignUpSuccess = callback; }
    public void setOnBackToLogin(Runnable callback) { this.onBackToLogin = callback; }

    private void clearAll() {
        usernameField.setText("");
        passwordField.setText("");
        confirmField.setText("");
        messageLabel.setText(" ");
    }

    private void doSignUp() {
        String username = usernameField.getText().trim();
        char[] password = passwordField.getPassword();
        char[] confirm = confirmField.getPassword();

        String problem = null;
        if (username.isEmpty() || password.length == 0) {
            problem = "Please fill in all the fields.";
        } else if (!username.matches("[A-Za-z0-9_]{3,30}")) {
            problem = "Username: 3–30 letters, numbers, or _ only.";
        } else if (password.length < 8) {
            problem = "Password needs at least 8 characters.";
        } else if (!Arrays.equals(password, confirm)) {
            problem = "Passwords do not match.";
        }

        Arrays.fill(confirm, ' ');

        if (problem != null) {
            Arrays.fill(password, ' ');
            showError(problem);
            return;
        }

        signUpButton.setEnabled(false);
        messageLabel.setForeground(Theme.MUTED);
        messageLabel.setText("Creating account...");

        new SwingWorker<String, Void>() {
            @Override
            protected String doInBackground() throws Exception {
                try {
                    AuthService.register(username, password);
                } finally {
                    Arrays.fill(password, ' ');
                }
                // Account exists now. Make its recovery code.
                try {
                    return RecoveryService.issueCode(username);
                } catch (Exception e) {
                    e.printStackTrace();
                    return null;
                }
            }

            @Override
            protected void done() {
                signUpButton.setEnabled(true);
                try {
                    String code = get();
                    clearAll();
                    if (code != null) {
                        Ui.showRecoveryCode(SignUpPanel.this, code);
                    } else {
                        JOptionPane.showMessageDialog(
                            SignUpPanel.this,
                            "Account created, but the recovery code could not "
                            + "be set up.\nAsk your leader to check the database "
                            + "permissions.",
                            "Recovery code",
                            JOptionPane.WARNING_MESSAGE
                        );
                    }
                    onSignUpSuccess.run();
                } catch (Exception ex) {
                    Throwable cause = (ex.getCause() != null) ? ex.getCause() : ex;
                    if (cause instanceof IllegalArgumentException) {
                        showError(cause.getMessage());
                    } else {
                        showError("Could not create account. Check your connection.");
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