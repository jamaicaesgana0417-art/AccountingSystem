import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.Arrays;
import java.util.function.Consumer;
import java.util.prefs.Preferences;
import javax.swing.*;

/** Login screen using the same split-panel style as password recovery. */
public class LoginPanel extends JPanel {

    private static final int MAX_FAILS = 5;
    private static final int LOCK_SECONDS = 30;
    private static final String PREF_USER = "lastUsername";

    private final Preferences prefs =
        Preferences.userNodeForPackage(LoginPanel.class);

    private final JTextField usernameField =
        new AuthStyle.RoundedTextField("Enter your username");
    private final JPasswordField passwordField =
        new AuthStyle.RoundedPasswordField("Enter your password");

    private final JCheckBox showPassword = AuthStyle.checkBox("Show password");
    private final JCheckBox rememberMe = AuthStyle.checkBox("Remember my username");
    private final JButton loginButton = new AuthStyle.ArrowButton("Log In");
    private final JButton forgotButton = AuthStyle.linkButton("Forgot password?", 12, true);
    private final JButton signUpButton = AuthStyle.linkButton("Create an account", 13, true);
    private final JLabel messageLabel = new JLabel(" ");
    private final JLabel capsLabel = new JLabel(" ");

    private Consumer<String> onLoginSuccess = username -> {};
    private Runnable onSignUpClick = () -> {};
    private Runnable onForgotClick = () -> {};

    private int failedAttempts = 0;

    public LoginPanel() {
        setLayout(new BorderLayout());
        AuthStyle.bindShowPassword(showPassword, passwordField);

        JPanel form = new JPanel(new GridBagLayout());
        form.setOpaque(false);

        JLabel title = AuthStyle.title("LOG IN");
        title.setHorizontalAlignment(SwingConstants.CENTER);
        JLabel subtitle = AuthStyle.subtitle(
            "Log in to open your accounting workspace."
        );
        subtitle.setHorizontalAlignment(SwingConstants.CENTER);

        messageLabel.setFont(AuthStyle.font(Font.PLAIN, 12));
        messageLabel.setForeground(AuthStyle.TEXT_MUTED);
        capsLabel.setFont(AuthStyle.font(Font.BOLD, 12));
        capsLabel.setForeground(new Color(0xB7791F));

        JPanel leftOptions = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        leftOptions.setOpaque(false);
        leftOptions.add(showPassword);
        leftOptions.add(rememberMe);

        JPanel optionsRow = new JPanel(new BorderLayout());
        optionsRow.setOpaque(false);
        optionsRow.add(leftOptions, BorderLayout.WEST);
        optionsRow.add(forgotButton, BorderLayout.EAST);

        JPanel statusRow = new JPanel(new BorderLayout(8, 0));
        statusRow.setOpaque(false);
        statusRow.add(capsLabel, BorderLayout.WEST);
        statusRow.add(messageLabel, BorderLayout.CENTER);

        JPanel createRow = new JPanel(new FlowLayout(FlowLayout.CENTER, 4, 0));
        createRow.setOpaque(false);
        JLabel newHere = new JLabel("New here?");
        newHere.setFont(AuthStyle.font(Font.PLAIN, 13));
        newHere.setForeground(AuthStyle.TEXT_MUTED);
        createRow.add(newHere);
        createRow.add(signUpButton);

        GridBagConstraints c = new GridBagConstraints();
        c.gridx = 0;
        c.weightx = 1;
        c.fill = GridBagConstraints.HORIZONTAL;

        c.gridy = 0; c.insets = new Insets(0, 0, 4, 0); form.add(title, c);
        c.gridy = 1; c.insets = new Insets(0, 0, 18, 0); form.add(subtitle, c);
        c.gridy = 2; c.insets = new Insets(0, 0, 12, 0); form.add(usernameField, c);
        c.gridy = 3; c.insets = new Insets(0, 0, 8, 0); form.add(passwordField, c);
        c.gridy = 4; c.insets = new Insets(0, 0, 8, 0); form.add(optionsRow, c);
        c.gridy = 5; c.insets = new Insets(0, 2, 10, 0); form.add(statusRow, c);
        c.gridy = 6; c.insets = new Insets(0, 0, 14, 0); form.add(loginButton, c);
        c.gridy = 7; c.insets = new Insets(0, 0, 0, 0); form.add(createRow, c);

        add(AuthStyle.loginBrandPanel(), BorderLayout.WEST);
        add(AuthStyle.formSide(form, 22, 42), BorderLayout.CENTER);

        String saved = prefs.get(PREF_USER, "");
        if (!saved.isEmpty()) {
            usernameField.setText(saved);
            rememberMe.setSelected(true);
        }

        passwordField.addKeyListener(new KeyAdapter() {
            @Override public void keyPressed(KeyEvent e) { updateCaps(); }
            @Override public void keyReleased(KeyEvent e) { updateCaps(); }
        });

        loginButton.addActionListener(e -> doLogin());
        passwordField.addActionListener(e -> doLogin());
        usernameField.addActionListener(e -> passwordField.requestFocusInWindow());
        forgotButton.addActionListener(e -> onForgotClick.run());
        signUpButton.addActionListener(e -> onSignUpClick.run());
    }

    public void setOnLoginSuccess(Consumer<String> callback) {
        onLoginSuccess = callback != null ? callback : username -> {};
    }

    public void setOnSignUpClick(Runnable callback) {
        onSignUpClick = callback != null ? callback : () -> {};
    }

    public void setOnForgotClick(Runnable callback) {
        onForgotClick = callback != null ? callback : () -> {};
    }

    public void clearSensitive() {
        passwordField.setText("");
        if (showPassword.isSelected()) showPassword.doClick();
        messageLabel.setForeground(AuthStyle.TEXT_MUTED);
        messageLabel.setText(" ");
        capsLabel.setText(" ");
    }

    private void updateCaps() {
        boolean on = false;
        try {
            on = Toolkit.getDefaultToolkit().getLockingKeyState(KeyEvent.VK_CAPS_LOCK);
        } catch (UnsupportedOperationException ignored) {
            // Some systems cannot report the Caps Lock state.
        }
        capsLabel.setText(on ? "Caps Lock is on" : " ");
    }

    private void doLogin() {
        if (!loginButton.isEnabled()) return;

        String username = usernameField.getText().trim();
        char[] password = passwordField.getPassword();

        if (username.isEmpty() || password.length == 0) {
            Arrays.fill(password, '\0');
            showError("Please enter your username and password.");
            return;
        }

        loginButton.setEnabled(false);
        messageLabel.setForeground(AuthStyle.TEXT_MUTED);
        messageLabel.setText("Logging in...");

        new SwingWorker<String, Void>() {
            @Override
            protected String doInBackground() throws Exception {
                try {
                    return AuthService.login(username, password);
                } finally {
                    Arrays.fill(password, '\0');
                }
            }

            @Override
            protected void done() {
                loginButton.setEnabled(true);
                try {
                    String user = get();
                    if (user == null) {
                        failedAttempts++;
                        if (failedAttempts >= MAX_FAILS) {
                            startLock();
                        } else {
                            showError("Wrong username or password. "
                                + (MAX_FAILS - failedAttempts) + " tries left.");
                        }
                    } else {
                        failedAttempts = 0;
                        if (rememberMe.isSelected()) prefs.put(PREF_USER, user);
                        else prefs.remove(PREF_USER);
                        clearSensitive();
                        onLoginSuccess.accept(user);
                    }
                } catch (Exception ex) {
                    showError("Could not log in. Check your database connection.");
                    ex.printStackTrace();
                }
            }
        }.execute();
    }

    private void startLock() {
        failedAttempts = 0;
        loginButton.setEnabled(false);
        final int[] secondsLeft = { LOCK_SECONDS };
        showError("Too many attempts. Try again in " + secondsLeft[0] + "s.");

        Timer timer = new Timer(1000, null);
        timer.addActionListener(e -> {
            secondsLeft[0]--;
            if (secondsLeft[0] <= 0) {
                timer.stop();
                loginButton.setEnabled(true);
                messageLabel.setForeground(AuthStyle.TEXT_MUTED);
                messageLabel.setText(" ");
            } else {
                showError("Too many attempts. Try again in " + secondsLeft[0] + "s.");
            }
        });
        timer.start();
    }

    private void showError(String message) {
        messageLabel.setForeground(AuthStyle.ERROR);
        messageLabel.setText("<html><body style='width:340px;'>" + message + "</body></html>");
    }
}
