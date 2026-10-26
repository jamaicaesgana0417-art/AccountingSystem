import java.awt.*;
import java.util.Arrays;
import java.util.function.Consumer;
import javax.swing.*;

public class LoginPanel extends JPanel {

    private final JTextField usernameField = new JTextField(20);
    private final JPasswordField passwordField = new JPasswordField(20);
    private final JButton loginButton = new JButton("Login");
    private final JLabel messageLabel = new JLabel(" ");

    // The main window will set this later, so it knows when login worked.
    private Consumer<String> onLoginSuccess = username -> {};

    public LoginPanel() {
        setLayout(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(6, 6, 6, 6);
        c.anchor = GridBagConstraints.WEST;

        JLabel title = new JLabel("Login");
        title.setFont(title.getFont().deriveFont(Font.BOLD, 22f));
        c.gridx = 0; c.gridy = 0; c.gridwidth = 2;
        add(title, c);

        c.gridwidth = 1;
        c.gridx = 0; c.gridy = 1;
        add(new JLabel("Username:"), c);
        c.gridx = 1;
        add(usernameField, c);

        c.gridx = 0; c.gridy = 2;
        add(new JLabel("Password:"), c);
        c.gridx = 1;
        add(passwordField, c);

        c.gridx = 1; c.gridy = 3;
        add(loginButton, c);

        c.gridx = 0; c.gridy = 4; c.gridwidth = 2;
        add(messageLabel, c);

        loginButton.addActionListener(e -> doLogin());
        passwordField.addActionListener(e -> doLogin()); // Enter key works too
    }

    public void setOnLoginSuccess(Consumer<String> callback) {
        this.onLoginSuccess = callback;
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
        messageLabel.setForeground(Color.DARK_GRAY);
        messageLabel.setText("Logging in...");

        // Runs in the background so the window doesn't freeze.
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
        messageLabel.setForeground(Color.RED);
        messageLabel.setText(message);
    }

    // TEMPORARY: lets you test this screen by itself.
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("Login test");
            LoginPanel panel = new LoginPanel();
            panel.setOnLoginSuccess(user ->
                JOptionPane.showMessageDialog(frame, "Welcome, " + user + "!"));
            frame.add(panel);
            frame.setSize(400, 280);
            frame.setLocationRelativeTo(null);
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setVisible(true);
        });
    }
}