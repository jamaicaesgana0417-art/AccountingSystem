import java.awt.*;
import java.util.Arrays;
import javax.swing.*;

public class SignUpPanel extends JPanel {

    private final JTextField usernameField = new JTextField(20);
    private final JPasswordField passwordField = new JPasswordField(20);
    private final JPasswordField confirmField = new JPasswordField(20);
    private final JButton signUpButton = new JButton("Create account");
    private final JLabel messageLabel = new JLabel(" ");

    // The main window can use these to switch screens later.
    private Runnable onSignUpSuccess = () -> {};

    public SignUpPanel() {
        setLayout(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(6, 6, 6, 6);
        c.anchor = GridBagConstraints.WEST;
        c.fill = GridBagConstraints.HORIZONTAL;

        JLabel title = new JLabel("Sign Up");
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

        c.gridx = 0; c.gridy = 3;
        add(new JLabel("Confirm password:"), c);
        c.gridx = 1;
        add(confirmField, c);

        c.gridx = 1; c.gridy = 4;
        add(signUpButton, c);

        c.gridx = 0; c.gridy = 5; c.gridwidth = 2;
        add(messageLabel, c);

        signUpButton.addActionListener(e -> doSignUp());
        confirmField.addActionListener(e -> doSignUp());
    }

    public void setOnSignUpSuccess(Runnable callback) {
        this.onSignUpSuccess = callback;
    }

    private void doSignUp() {
        String username = usernameField.getText().trim();
        char[] password = passwordField.getPassword();
        char[] confirm = confirmField.getPassword();

        if (username.isEmpty() || password.length == 0) {
            showError("Please fill in all the fields.");
            Arrays.fill(password, ' ');
            Arrays.fill(confirm, ' ');
            return;
        }

        if (!Arrays.equals(password, confirm)) {
            showError("Passwords do not match.");
            Arrays.fill(password, ' ');
            Arrays.fill(confirm, ' ');
            return;
        }
        Arrays.fill(confirm, ' ');

        signUpButton.setEnabled(false);
        messageLabel.setForeground(Color.DARK_GRAY);
        messageLabel.setText("Creating account...");

        new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() throws Exception {
                try {
                    AuthService.register(username, password);
                } finally {
                    Arrays.fill(password, ' ');
                }
                return null;
            }

            @Override
            protected void done() {
                signUpButton.setEnabled(true);
                try {
                    get();
                    usernameField.setText("");
                    passwordField.setText("");
                    confirmField.setText("");
                    messageLabel.setForeground(new Color(0, 128, 0));
                    messageLabel.setText("Account created! You can log in now.");
                    onSignUpSuccess.run();
                } catch (Exception ex) {
                    Throwable cause = (ex.getCause() != null) ? ex.getCause() : ex;
                    if (cause instanceof IllegalArgumentException) {
                        // Our own rules: bad username, short password, name taken
                        showError("<html>" + cause.getMessage() + "</html>");
                    } else {
                        showError("Could not create account. Check your connection.");
                        ex.printStackTrace();
                    }
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
            JFrame frame = new JFrame("Sign up test");
            frame.add(new SignUpPanel());
            frame.setSize(450, 320);
            frame.setLocationRelativeTo(null);
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setVisible(true);
        });
    }
}