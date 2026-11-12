import java.awt.*;
import java.util.Arrays;
import javax.swing.*;

/** Account registration screen matching the Login and Forgot Password design. */
public class SignUpPanel extends JPanel {

    private final JTextField usernameField =
        new AuthStyle.RoundedTextField("Choose a username");
    private final JPasswordField passwordField =
        new AuthStyle.RoundedPasswordField("Create a password (8+ characters)");
    private final JPasswordField confirmField =
        new AuthStyle.RoundedPasswordField("Re-enter your password");

    private final PasswordMeter meter = new PasswordMeter();
    private final JCheckBox showPassword = AuthStyle.checkBox("Show password");
    private final JButton signUpButton = new AuthStyle.ArrowButton("Create Account");
    private final JButton backButton =
        AuthStyle.linkButton("Log in", 14, true);
    private final JLabel messageLabel =
        new JLabel(" ");

    private Runnable onSignUpSuccess = () -> {};
    private Runnable onBackToLogin = () -> {};

    public SignUpPanel() {
        setLayout(new BorderLayout());

        lockField(usernameField, 42);
        lockField(passwordField, 42);
        lockField(confirmField, 42);
        lockField(signUpButton, 44);

        meter.setPreferredSize(new Dimension(410, 38));
        meter.setMinimumSize(new Dimension(100, 36));

        AuthStyle.bindShowPassword(showPassword, passwordField, confirmField);
        meter.attach(passwordField, usernameField);

        messageLabel.setFont(AuthStyle.font(Font.PLAIN, 12));
        messageLabel.setForeground(AuthStyle.TEXT_MUTED);

        JPanel form = new JPanel(new GridBagLayout());
        form.setOpaque(false);

        JLabel title = AuthStyle.title("CREATE ACCOUNT");
        title.setHorizontalAlignment(SwingConstants.CENTER);

        JLabel subtitle = AuthStyle.subtitle(
            "Create your workspace and get started."
        );
        subtitle.setHorizontalAlignment(SwingConstants.CENTER);

        JPanel loginRow = new JPanel(new FlowLayout(FlowLayout.CENTER, 4, 0));
        loginRow.setOpaque(false);
        JLabel alreadyHaveAccount = new JLabel("Already have an account?");
        alreadyHaveAccount.setFont(AuthStyle.font(Font.PLAIN, 13));
        alreadyHaveAccount.setForeground(AuthStyle.TEXT_MUTED);
        loginRow.add(alreadyHaveAccount);
        loginRow.add(backButton);

        GridBagConstraints c = new GridBagConstraints();
        c.gridx = 0;
        c.weightx = 1;
        c.fill = GridBagConstraints.HORIZONTAL;

        c.gridy = 0; c.insets = new Insets(0, 0, 4, 0); form.add(title, c);
        c.gridy = 1; c.insets = new Insets(0, 0, 16, 0); form.add(subtitle, c);
        c.gridy = 2; c.insets = new Insets(0, 0, 10, 0); form.add(usernameField, c);
        c.gridy = 3; c.insets = new Insets(0, 0, 3, 0); form.add(passwordField, c);
        c.gridy = 4; c.insets = new Insets(0, 2, 5, 0); form.add(meter, c);
        c.gridy = 5; c.insets = new Insets(0, 0, 8, 0); form.add(confirmField, c);
        c.gridy = 6; c.insets = new Insets(0, 0, 5, 0); form.add(showPassword, c);
        c.gridy = 7; c.insets = new Insets(0, 2, 8, 0); form.add(messageLabel, c);
        c.gridy = 8; c.insets = new Insets(0, 0, 12, 0); form.add(signUpButton, c);
        c.gridy = 9; c.insets = new Insets(0, 0, 0, 0); form.add(loginRow, c);

        add(AuthStyle.signUpBrandPanel(), BorderLayout.WEST);
        add(AuthStyle.formSide(form, 16, 30), BorderLayout.CENTER);

        signUpButton.addActionListener(e -> doSignUp());
        confirmField.addActionListener(e -> doSignUp());
        usernameField.addActionListener(e -> passwordField.requestFocusInWindow());
        passwordField.addActionListener(e -> confirmField.requestFocusInWindow());
        backButton.addActionListener(e -> {
            clearAll();
            onBackToLogin.run();
        });
    }

    public void setOnSignUpSuccess(Runnable callback) {
        onSignUpSuccess = callback != null ? callback : () -> {};
    }

    public void setOnBackToLogin(Runnable callback) {
        onBackToLogin = callback != null ? callback : () -> {};
    }

    private void lockField(JComponent component, int height) {
        component.setPreferredSize(new Dimension(410, height));
        component.setMinimumSize(new Dimension(100, height));
    }

    private void clearAll() {
        usernameField.setText("");
        passwordField.setText("");
        confirmField.setText("");
        if (showPassword.isSelected()) showPassword.doClick();
        signUpButton.setEnabled(true);
        messageLabel.setForeground(AuthStyle.TEXT_MUTED);
        messageLabel.setText(" ");
    }

    private void doSignUp() {
        if (!signUpButton.isEnabled()) return;

        String username = usernameField.getText().trim();
        char[] password = passwordField.getPassword();
        char[] confirm = confirmField.getPassword();

        String problem = null;
        if (username.isEmpty() || password.length == 0 || confirm.length == 0) {
            problem = "Please complete all fields.";
        } else if (!username.matches("[A-Za-z0-9_]{3,30}")) {
            problem = "Username must be 3–30 characters using letters, numbers, or underscores.";
        } else {
            problem = PasswordPolicy.validate(password, username);
            if (problem == null && !Arrays.equals(password, confirm)) {
                problem = "Passwords do not match.";
            }
        }

        Arrays.fill(confirm, '\0');

        if (problem != null) {
            Arrays.fill(password, '\0');
            showError(problem);
            return;
        }

        signUpButton.setEnabled(false);
        messageLabel.setForeground(AuthStyle.TEXT_MUTED);
        messageLabel.setText("Creating your account...");

        new SwingWorker<String, Void>() {
            @Override
            protected String doInBackground() throws Exception {
                try {
                    AuthService.register(username, password);
                } finally {
                    Arrays.fill(password, '\0');
                }

                try {
                    return RecoveryService.issueCode(username);
                } catch (Exception recoveryException) {
                    recoveryException.printStackTrace();
                    return null;
                }
            }

            @Override
            protected void done() {
                signUpButton.setEnabled(true);
                try {
                    String recoveryCode = get();
                    clearAll();

                    if (recoveryCode != null) {
                        AuthStyle.showRecoveryCode(SignUpPanel.this, recoveryCode);
                    } else {
                        JOptionPane.showMessageDialog(
                            SignUpPanel.this,
                            "Your account was created, but its recovery code "
                                + "could not be generated. Please contact your administrator.",
                            "Recovery Code",
                            JOptionPane.WARNING_MESSAGE
                        );
                    }

                    onSignUpSuccess.run();
                } catch (Exception ex) {
                    Throwable cause = ex.getCause() != null ? ex.getCause() : ex;
                    if (cause instanceof IllegalArgumentException) {
                        showError(cause.getMessage());
                    } else {
                        showError("Could not create your account. Check your database connection.");
                        ex.printStackTrace();
                    }
                }
            }
        }.execute();
    }

    private void showError(String message) {
        messageLabel.setForeground(AuthStyle.ERROR);
        messageLabel.setText(
            "<html><body style='width:340px;'>" + message + "</body></html>"
        );
    }
}
