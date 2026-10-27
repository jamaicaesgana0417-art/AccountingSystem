import javax.swing.*;
import java.awt.*;
import java.util.Arrays;

public class LoginFrame extends JFrame {

    private final JTextField usernameField = new JTextField(20);
    private final JPasswordField passwordField =
        new JPasswordField(20);

    private final JButton loginButton = new JButton("Login");
    private final JButton signupButton = new JButton("Sign Up");

    public LoginFrame() {
        super("Accounting System - Login");

        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBackground(Color.WHITE);
        panel.setBorder(
            BorderFactory.createEmptyBorder(30, 35, 30, 35)
        );

        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(8, 8, 8, 8);
        c.fill = GridBagConstraints.HORIZONTAL;

        JLabel heading = new JLabel(
            "Accounting System",
            SwingConstants.CENTER
        );

        heading.setFont(new Font("Arial", Font.BOLD, 24));
        heading.setForeground(new Color(15, 70, 125));

        c.gridx = 0;
        c.gridy = 0;
        c.gridwidth = 2;
        panel.add(heading, c);

        c.gridwidth = 1;
        c.gridy = 1;
        panel.add(new JLabel("Username:"), c);

        c.gridx = 1;
        panel.add(usernameField, c);

        c.gridx = 0;
        c.gridy = 2;
        panel.add(new JLabel("Password:"), c);

        c.gridx = 1;
        panel.add(passwordField, c);

        c.gridx = 0;
        c.gridy = 3;
        panel.add(signupButton, c);

        c.gridx = 1;
        panel.add(loginButton, c);

        setContentPane(panel);
        getRootPane().setDefaultButton(loginButton);

        loginButton.addActionListener(event -> login());
        signupButton.addActionListener(event -> signUp());

        pack();
        setResizable(false);
        setLocationRelativeTo(null);
    }

    private void login() {
        String username = usernameField.getText().trim();
        char[] password = passwordField.getPassword();

        if (username.isEmpty() || password.length == 0) {
            Arrays.fill(password, '\0');
            showMessage("Enter your username and password.");
            return;
        }

        setBusy(true);

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
                try {
                    String authenticatedUser = get();
passwordField.setText("");

if (authenticatedUser == null) {
    showMessage("Incorrect username or password.");
    return;
}

Session.login(authenticatedUser);
App.openAccountingWindow(authenticatedUser);
dispose();

                } catch (Exception exception) {
                    showFailure(exception);
                } finally {
                    setBusy(false);
                }
            }
        }.execute();
    }

    private void signUp() {
        JTextField newUsername = new JTextField(20);
        JPasswordField newPassword = new JPasswordField(20);
        JPasswordField confirmPassword = new JPasswordField(20);

        JPanel form = new JPanel(new GridLayout(0, 1, 5, 5));

        form.add(new JLabel("Username (3–30 letters/numbers/underscores):"));
        form.add(newUsername);
        form.add(new JLabel("Password (at least 8 characters):"));
        form.add(newPassword);
        form.add(new JLabel("Confirm Password:"));
        form.add(confirmPassword);

        int answer = JOptionPane.showConfirmDialog(
            this,
            form,
            "Create Account",
            JOptionPane.OK_CANCEL_OPTION,
            JOptionPane.PLAIN_MESSAGE
        );

        char[] password = newPassword.getPassword();
        char[] confirmation = confirmPassword.getPassword();

        newPassword.setText("");
        confirmPassword.setText("");

        if (answer != JOptionPane.OK_OPTION) {
            Arrays.fill(password, '\0');
            Arrays.fill(confirmation, '\0');
            return;
        }

        boolean matches = Arrays.equals(password, confirmation);
        Arrays.fill(confirmation, '\0');

        if (!matches) {
            Arrays.fill(password, '\0');
            showMessage("Passwords do not match.");
            return;
        }

        String username = newUsername.getText().trim();
        setBusy(true);

        new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() throws Exception {
                try {
                    AuthService.register(username, password);
                    return null;
                } finally {
                    Arrays.fill(password, '\0');
                }
            }

            @Override
            protected void done() {
                try {
                    get();

                    usernameField.setText(username);
                    passwordField.setText("");

                    showMessage(
                        "Account created successfully. You can now log in."
                    );

                } catch (Exception exception) {
                    showFailure(exception);
                } finally {
                    setBusy(false);
                }
            }
        }.execute();
    }

    private void setBusy(boolean busy) {
        loginButton.setEnabled(!busy);
        signupButton.setEnabled(!busy);
        usernameField.setEnabled(!busy);
        passwordField.setEnabled(!busy);

        setCursor(Cursor.getPredefinedCursor(
            busy ? Cursor.WAIT_CURSOR : Cursor.DEFAULT_CURSOR
        ));
    }

    private void showMessage(String message) {
        JOptionPane.showMessageDialog(this, message);
    }

    private void showFailure(Exception exception) {
        Throwable cause = exception.getCause() == null
            ? exception
            : exception.getCause();

        cause.printStackTrace();

        JOptionPane.showMessageDialog(
            this,
            cause.getMessage(),
            "Authentication Error",
            JOptionPane.ERROR_MESSAGE
        );
    }
}