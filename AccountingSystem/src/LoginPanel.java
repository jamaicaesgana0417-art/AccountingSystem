import java.awt.*;
import java.util.Arrays;
import java.util.function.Consumer;
import javax.swing.*;

public class LoginPanel extends JPanel {

    private final JTextField usernameField = Theme.textField("Username");
    private final JPasswordField passwordField = Theme.passwordField("Password");
    private final JButton loginButton = Theme.primaryButton("Log in");
    private final JButton signUpButton = Theme.secondaryButton("Create new account");
    private final JLabel messageLabel = new JLabel(" ");

    private Consumer<String> onLoginSuccess = username -> {};
    private Runnable onSignUpClick = () -> {};

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

        GridBagConstraints c = new GridBagConstraints();
        c.gridx = 0;
        c.weightx = 1;
        c.fill = GridBagConstraints.HORIZONTAL;

        c.gridy = 0; c.insets = new Insets(0, 0, 4, 0);  card.add(title, c);
        c.gridy = 1; c.insets = new Insets(0, 0, 22, 0); card.add(subtitle, c);
        c.gridy = 2; c.insets = new Insets(0, 0, 12, 0); card.add(usernameField, c);
        c.gridy = 3; c.insets = new Insets(0, 0, 8, 0);  card.add(passwordField, c);
        c.gridy = 4; c.insets = new Insets(0, 2, 12, 0); card.add(messageLabel, c);
        c.gridy = 5; c.insets = new Insets(0, 0, 18, 0); card.add(loginButton, c);
        c.gridy = 6; c.insets = new Insets(0, 0, 0, 0);  card.add(signUpButton, c);

        add(card);

        loginButton.addActionListener(e -> doLogin());
        passwordField.addActionListener(e -> doLogin());
        usernameField.addActionListener(e -> passwordField.requestFocusInWindow());
        signUpButton.addActionListener(e -> onSignUpClick.run());
    }

    public void setOnLoginSuccess(Consumer<String> callback) {
        this.onLoginSuccess = callback;
    }

    public void setOnSignUpClick(Runnable callback) {
        this.onSignUpClick = callback;
    }

    private void doLogin() {
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
                        showError("Wrong username or password.");
                    } else {
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

    private void showError(String message) {
        messageLabel.setForeground(Theme.ERROR);
        messageLabel.setText(message);
    }

    // TEMPORARY: lets you test this screen by itself.
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("Login test");
            LoginPanel panel = new LoginPanel();
            panel.setOnLoginSuccess(user ->
                JOptionPane.showMessageDialog(frame, "Welcome, " + user + "!"));
            frame.setContentPane(panel);
            frame.setSize(480, 560);
            frame.setLocationRelativeTo(null);
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setVisible(true);
        });
    }
}