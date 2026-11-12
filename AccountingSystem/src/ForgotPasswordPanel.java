import java.awt.*;
import java.util.Arrays;
import java.util.function.Consumer;
import javax.swing.*;

/** Password recovery with a simple username-first screen. */
public class ForgotPasswordPanel extends JPanel {

    private static final String FIRST_HINT = " ";
    private static final String RESET_HINT =
        "Enter the recovery code saved when you created your account.";

    private final JTextField usernameField =
        new AuthStyle.RoundedTextField("Enter your username");
    private final JTextField codeField =
        new AuthStyle.RoundedTextField("e.g. XXXX-XXXX-XXXX");
    private final JPasswordField newPassField =
        new AuthStyle.RoundedPasswordField("New password (8+ characters)");
    private final JPasswordField confirmField =
        new AuthStyle.RoundedPasswordField("Re-enter new password");

    private final PasswordMeter meter = new PasswordMeter();
    private final JCheckBox showPassword = AuthStyle.checkBox("Show password");

    private final JButton continueButton = new AuthStyle.ArrowButton("Continue");
    private final JButton resetButton = new AuthStyle.ArrowButton("Reset Password");
    private final JButton changeUsernameButton =
        AuthStyle.linkButton("Change username", 13, true);
    private final JButton backButton =
        AuthStyle.linkButton("Back to log in", 15, true);

    private final JLabel firstMessage = new JLabel(FIRST_HINT);
    private final JLabel resetMessage = new JLabel(RESET_HINT);

    private final CardLayout stepLayout = new CardLayout();
    private final JPanel steps = new JPanel(stepLayout);

    private Runnable onBackToLogin = () -> {};
    private Runnable onResetDone = () -> {};
    private Consumer<Boolean> onStepChange = resetStep -> {};

    private String recoveryUsername = "";

    public ForgotPasswordPanel() {
        setLayout(new BorderLayout());

        lockField(usernameField, 42);
        lockField(codeField, 42);
        lockField(newPassField, 42);
        lockField(confirmField, 42);
        lockField(continueButton, 44);
        lockField(resetButton, 44);

        meter.setPreferredSize(new Dimension(410, 38));
        meter.setMinimumSize(new Dimension(100, 36));

        AuthStyle.bindShowPassword(showPassword, newPassField, confirmField);
        meter.attach(newPassField, usernameField);

        styleMessage(firstMessage);
        styleMessage(resetMessage);

        steps.setOpaque(false);
        steps.add(buildUsernameStep(), "username");
        steps.add(buildResetStep(), "reset");
        steps.setPreferredSize(new Dimension(410, 250));

        // Same navy brand panel and pale form side as the Login screen.
        add(AuthStyle.forgotPasswordBrandPanel(), BorderLayout.WEST);
        add(AuthStyle.formSide(steps, 16, 30), BorderLayout.CENTER);

        continueButton.addActionListener(e -> continueToReset());
        usernameField.addActionListener(e -> continueToReset());
        resetButton.addActionListener(e -> doReset());
        confirmField.addActionListener(e -> doReset());
        codeField.addActionListener(e -> newPassField.requestFocusInWindow());
        newPassField.addActionListener(e -> confirmField.requestFocusInWindow());

        changeUsernameButton.addActionListener(e -> showUsernameStep());
        backButton.addActionListener(e -> onBackToLogin.run());
    }

    private JPanel buildUsernameStep() {
        JPanel form = formPanel();
        GridBagConstraints c = baseConstraints();

        JLabel title = AuthStyle.title("FORGOT PASSWORD");
        JLabel subtitle = AuthStyle.subtitle(
            "Enter your username to continue."
        );
        title.setHorizontalAlignment(SwingConstants.CENTER);
        subtitle.setHorizontalAlignment(SwingConstants.CENTER);

        c.gridy = 0; c.insets = new Insets(0, 0, 4, 0); form.add(title, c);
        c.gridy = 1; c.insets = new Insets(0, 0, 16, 0); form.add(subtitle, c);
        c.gridy = 2; c.insets = new Insets(0, 0, 8, 0); form.add(usernameField, c);
        c.gridy = 3; c.insets = new Insets(0, 2, 8, 0); form.add(firstMessage, c);
        // Place Back to Log In beside Continue on the same row.
        JPanel actionRow = new JPanel(new FlowLayout(FlowLayout.CENTER, 14, 0));
        actionRow.setOpaque(false);
        continueButton.setPreferredSize(new Dimension(220, 44));
        continueButton.setMinimumSize(new Dimension(150, 44));
        backButton.setPreferredSize(new Dimension(145, 44));
        actionRow.add(continueButton);
        actionRow.add(backButton);
        c.gridy = 4; c.insets = new Insets(0, 0, 12, 0); form.add(actionRow, c);

        return form;
    }

    private JPanel buildResetStep() {
        JPanel form = formPanel();
        GridBagConstraints c = baseConstraints();

        JLabel title = AuthStyle.title("RESET PASSWORD");
        JLabel subtitle = AuthStyle.subtitle(
            "Verify your recovery code and choose a new password."
        );
        title.setHorizontalAlignment(SwingConstants.CENTER);
        subtitle.setHorizontalAlignment(SwingConstants.CENTER);

        c.gridy = 0; c.insets = new Insets(0, 0, 4, 0); form.add(title, c);
        c.gridy = 1; c.insets = new Insets(0, 0, 12, 0); form.add(subtitle, c);

        c.gridy = 2; c.insets = new Insets(0, 0, 7, 0); form.add(codeField, c);
        c.gridy = 3; c.insets = new Insets(0, 0, 3, 0); form.add(newPassField, c);
        c.gridy = 4; c.insets = new Insets(0, 2, 5, 0); form.add(meter, c);
        c.gridy = 5; c.insets = new Insets(0, 0, 7, 0); form.add(confirmField, c);
        c.gridy = 6; c.insets = new Insets(0, 0, 3, 0); form.add(showPassword, c);
        c.gridy = 7; c.insets = new Insets(0, 2, 7, 0); form.add(resetMessage, c);
        c.gridy = 8; c.insets = new Insets(0, 0, 10, 0); form.add(resetButton, c);

        JPanel links = new JPanel(new FlowLayout(FlowLayout.CENTER, 12, 0));
        links.setOpaque(false);
        links.add(changeUsernameButton);
        c.gridy = 9; c.insets = new Insets(0, 0, 0, 0); form.add(links, c);
        return form;
    }

    private JPanel formPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setOpaque(false);
        return panel;
    }

    private GridBagConstraints baseConstraints() {
        GridBagConstraints c = new GridBagConstraints();
        c.gridx = 0;
        c.weightx = 1;
        c.fill = GridBagConstraints.HORIZONTAL;
        return c;
    }

    private void lockField(JComponent component, int height) {
        component.setPreferredSize(new Dimension(410, height));
        component.setMinimumSize(new Dimension(100, height));
    }

    private void styleMessage(JLabel label) {
        label.setFont(AuthStyle.font(Font.PLAIN, 12));
        label.setForeground(AuthStyle.TEXT_MUTED);
    }

    private void continueToReset() {
        if (!continueButton.isEnabled()) return;

        String username = usernameField.getText().trim();
        if (!username.matches("[A-Za-z0-9_]{3,30}")) {
            showError(firstMessage,
                "Enter a valid username (3–30 letters, numbers, or underscores).", true);
            usernameField.requestFocusInWindow();
            return;
        }

        recoveryUsername = username;
        resetMessage.setForeground(AuthStyle.TEXT_MUTED);
        resetMessage.setText(RESET_HINT);
        stepLayout.show(steps, "reset");
        steps.setPreferredSize(new Dimension(410, 400));
        onStepChange.accept(true);
        revalidate();
        repaint();
        codeField.requestFocusInWindow();
    }

    private void showUsernameStep() {
        codeField.setText("");
        newPassField.setText("");
        confirmField.setText("");
        if (showPassword.isSelected()) showPassword.doClick();
        resetButton.setEnabled(true);
        resetMessage.setForeground(AuthStyle.TEXT_MUTED);
        resetMessage.setText(RESET_HINT);
        stepLayout.show(steps, "username");
        steps.setPreferredSize(new Dimension(410, 250));
        onStepChange.accept(false);
        revalidate();
        repaint();
        usernameField.requestFocusInWindow();
    }

    private void doReset() {
        if (!resetButton.isEnabled()) return;

        String code = codeField.getText().trim();
        char[] password = newPassField.getPassword();
        char[] confirm = confirmField.getPassword();

        String problem = null;
        if (code.isEmpty() || password.length == 0 || confirm.length == 0) {
            problem = "Please complete all fields.";
        } else {
            problem = PasswordPolicy.validate(password, recoveryUsername);
            if (problem == null && !Arrays.equals(password, confirm)) {
                problem = "Passwords do not match.";
            }
        }
        Arrays.fill(confirm, '\0');

        if (problem != null) {
            Arrays.fill(password, '\0');
            showError(resetMessage, problem, false);
            return;
        }

        resetButton.setEnabled(false);
        resetMessage.setForeground(AuthStyle.TEXT_MUTED);
        resetMessage.setText("Resetting password...");

        new SwingWorker<String, Void>() {
            @Override
            protected String doInBackground() throws Exception {
                try {
                    return RecoveryService.resetPassword(
                        recoveryUsername, code, password
                    );
                } finally {
                    Arrays.fill(password, '\0');
                }
            }

            @Override
            protected void done() {
                resetButton.setEnabled(true);
                try {
                    String replacementCode = get();
                    if (replacementCode == null) {
                        showError(resetMessage,
                            "Username or recovery code is incorrect.", false);
                        return;
                    }

                    clearAll();
                    AuthStyle.showRecoveryCode(ForgotPasswordPanel.this, replacementCode);
                    onResetDone.run();

                } catch (Exception ex) {
                    Throwable cause = ex.getCause() != null ? ex.getCause() : ex;
                    if (cause instanceof IllegalArgumentException) {
                        showError(resetMessage, cause.getMessage(), false);
                    } else {
                        showError(resetMessage,
                            "Could not reset password. Check your database connection.", false);
                        ex.printStackTrace();
                    }
                }
            }
        }.execute();
    }

    private void showError(JLabel label, String message, boolean firstStep) {
        label.setForeground(AuthStyle.ERROR);
        label.setText("<html><body style='width: 340px;'>" + message + "</body></html>");
    }

    private void clearAll() {
        usernameField.setText("");
        codeField.setText("");
        newPassField.setText("");
        confirmField.setText("");
        if (showPassword.isSelected()) showPassword.doClick();
        recoveryUsername = "";
        continueButton.setEnabled(true);
        resetButton.setEnabled(true);
        firstMessage.setForeground(AuthStyle.TEXT_MUTED);
        firstMessage.setText(FIRST_HINT);
        resetMessage.setForeground(AuthStyle.TEXT_MUTED);
        resetMessage.setText(RESET_HINT);
        stepLayout.show(steps, "username");
        steps.setPreferredSize(new Dimension(410, 250));
        onStepChange.accept(false);
        revalidate();
        repaint();
    }

    public void prepareForDisplay() {
        clearAll();
    }

    public void setOnBackToLogin(Runnable callback) {
        onBackToLogin = callback != null ? callback : () -> {};
    }

    public void setOnResetDone(Runnable callback) {
        onResetDone = callback != null ? callback : () -> {};
    }

    public void setOnStepChange(Consumer<Boolean> callback) {
        onStepChange = callback != null ? callback : resetStep -> {};
    }
}
