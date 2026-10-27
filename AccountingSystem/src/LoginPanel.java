import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.Arrays;
import java.util.function.Consumer;
import java.util.prefs.Preferences;
import javax.swing.*;

public class LoginPanel extends JPanel {

    private static final int MAX_FAILS = 5;
    private static final int LOCK_SECONDS = 30;
    private static final String PREF_USER = "lastUsername";

    private final Preferences prefs =
        Preferences.userNodeForPackage(LoginPanel.class);

    private final JTextField usernameField = Theme.textField("Username");
    private final JPasswordField passwordField = Theme.passwordField("Password");
    private final JCheckBox showPassword = Ui.checkBox("Show password");
    private final JCheckBox rememberMe = Ui.checkBox("Remember my username");
    private final JButton loginButton = Theme.primaryButton("Log in");
    private final JButton signUpButton = Theme.secondaryButton("Create new account");
    private final JLabel messageLabel = new JLabel(" ");
    private final JLabel capsLabel = new JLabel(" ");

    private Consumer<String> onLoginSuccess = username -> {};
    private Runnable onSignUpClick = () -> {};
    private Runnable onForgotClick = () -> {};

    private int failedAttempts = 0;

    public LoginPanel() {
        setLayout(new GridBagLayout());
        setBackground(Theme.BG);

        JPanel card = Theme.card();
        card.setLayout(new GridBagLayout());
        card.setBorder(BorderFactory.createEmptyBorder(32, 32, 32, 32));

        JLabel title = new JLabel("Welcome back");
        title.setFont(Theme.HEADING);
        title.setForeground(Theme.TEXT);

        JLabel subtitle = new JLabel("Log in to your accounting system");
        subtitle.setFont(Theme.BODY);
        subtitle.setForeground(Theme.MUTED);

        messageLabel.setFont(Theme.BODY.deriveFont(13f));
        messageLabel.setForeground(Theme.ERROR);

        capsLabel.setFont(Theme.BODY.deriveFont(Font.BOLD, 12f));
        capsLabel.setForeground(new Color(0xE0A800));

        Ui.bindShowPassword(showPassword, passwordField);

        JPanel optionsRow = new JPanel(new BorderLayout());
        optionsRow.setOpaque(false);
        optionsRow.add(showPassword, BorderLayout.WEST);
        optionsRow.add(Ui.link("Forgot password?", () -> onForgotClick.run()),
                BorderLayout.EAST);

        GridBagConstraints c = new GridBagConstraints();
        c.gridx = 0;
        c.weightx = 1;
        c.fill = GridBagConstraints.HORIZONTAL;

        c.gridy = 0; c.insets = new Insets(0, 0, 4, 0);  card.add(title, c);
        c.gridy = 1; c.insets = new Insets(0, 0, 22, 0); card.add(subtitle, c);
        c.gridy = 2; c.insets = new Insets(0, 0, 12, 0); card.add(usernameField, c);
        c.gridy = 3; c.insets = new Insets(0, 0, 6, 0);  card.add(passwordField, c);
        c.gridy = 4; c.insets = new Insets(0, 0, 2, 0);  card.add(optionsRow, c);
        c.gridy = 5; c.insets = new Insets(0, 0, 6, 0);  card.add(rememberMe, c);
        c.gridy = 6; c.insets = new Insets(0, 2, 0, 0);  card.add(capsLabel, c);
        c.gridy = 7; c.insets = new Insets(0, 2, 12, 0); card.add(messageLabel, c);
        c.gridy = 8; c.insets = new Insets(0, 0, 18, 0); card.add(loginButton, c);
        c.gridy = 9; c.insets = new Insets(0, 0, 0, 0);  card.add(signUpButton, c);

        add(card);

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
        signUpButton.addActionListener(e -> onSignUpClick.run());
    }

    public void setOnLoginSuccess(Consumer<String> callback) { this.onLoginSuccess = callback; }
    public void setOnSignUpClick(Runnable callback) { this.onSignUpClick = callback; }
    public void setOnForgotClick(Runnable callback) { this.onForgotClick = callback; }

    public void clearSensitive() {
        passwordField.setText("");
        messageLabel.setText(" ");
    }

    private void updateCaps() {
        boolean on = false;
        try {
            on = Toolkit.getDefaultToolkit()
                    .getLockingKeyState(KeyEvent.VK_CAPS_LOCK);
        } catch (UnsupportedOperationException ex) {
            // some systems can't tell; just hide the warning
        }
        capsLabel.setText(on ? "Caps Lock is on" : " ");
    }

    private void doLogin() {
        if (!loginButton.isEnabled()) return;

        String username = usernameField.getText().trim();
        char[] password = passwordField.getPassword();

        if (username.isEmpty() || password.length == 0) {
            showError("Please enter your username and password.");
            Arrays.fill(password, ' ');
            return;
        }

        loginButton.setEnabled(false);
        messageLabel.setForeground(Theme.MUTED);
        messageLabel.setText("Logging in...");

        new SwingWorker<String, Void>() {
            @Override
            protected String doInBackground() throws Exception {
                try {
                    return AuthService.login(username, password);
                } finally {
                    Arrays.fill(password, ' ');
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
                        if (rememberMe.isSelected()) {
                            prefs.put(PREF_USER, user);
                        } else {
                            prefs.remove(PREF_USER);
                        }
                        passwordField.setText("");
                        messageLabel.setText(" ");
                        onLoginSuccess.accept(user);
                    }
                } catch (Exception ex) {
                    showError("Could not log in. Check your connection.");
                    ex.printStackTrace();
                }
            }
        }.execute();
    }

    private void startLock() {
        failedAttempts = 0;
        loginButton.setEnabled(false);
        final int[] left = { LOCK_SECONDS };
        showError("Too many attempts. Try again in " + left[0] + "s.");

        javax.swing.Timer timer = new javax.swing.Timer(1000, null);
        timer.addActionListener(e -> {
            left[0]--;
            if (left[0] <= 0) {
                timer.stop();
                loginButton.setEnabled(true);
                messageLabel.setText(" ");
            } else {
                showError("Too many attempts. Try again in " + left[0] + "s.");
            }
        });
        timer.start();
    }

    private void showError(String message) {
        messageLabel.setForeground(Theme.ERROR);
        messageLabel.setText(message);
    }
}