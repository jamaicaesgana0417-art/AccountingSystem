import javax.swing.*;
import javax.swing.border.EmptyBorder;

import java.awt.*;
import java.util.Arrays;

public class LoginFrame extends JFrame {

    // Colors used in the reference design
    private static final Color NAVY =
        new Color(22, 52, 91);

    private static final Color DARK_TEXT =
        new Color(25, 52, 86);

    private static final Color LIGHT_TEXT =
        new Color(105, 123, 145);

    private static final Color FIELD_BACKGROUND =
        new Color(242, 245, 249);

    private static final Color ACCENT_BLUE =
        new Color(62, 148, 204);

    private final CardLayout cardLayout = new CardLayout();

    private final JPanel cardPanel = new JPanel(cardLayout);

    // Login fields
    private final JTextField loginUsername =
        new JTextField();

    private final JPasswordField loginPassword =
        new JPasswordField();

    private final JButton loginButton =
        new JButton("Log In  →");

    private final JButton openSignUpButton =
        new JButton("New here? Create an account");

    // Sign-up fields
    private final JTextField signUpUsername =
        new JTextField();

    private final JPasswordField signUpPassword =
        new JPasswordField();

    private final JPasswordField confirmPassword =
        new JPasswordField();

    private final JButton createAccountButton =
        new JButton("Create Account  →");

    private final JButton openLoginButton =
        new JButton(
            "<html>Already have an account? <u>Log in</u></html>"
        );

    public LoginFrame() {
        super("Accounting System");

        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        cardPanel.add(createLoginScreen(), "LOGIN");
        cardPanel.add(createSignUpScreen(), "SIGN_UP");

        setContentPane(cardPanel);

        loginButton.addActionListener(event -> login());

        openSignUpButton.addActionListener(event -> {
            cardLayout.show(cardPanel, "SIGN_UP");
            getRootPane().setDefaultButton(createAccountButton);
            signUpUsername.requestFocusInWindow();
        });

        openLoginButton.addActionListener(event -> showLogin());

        createAccountButton.addActionListener(event -> signUp());

        cardLayout.show(cardPanel, "LOGIN");
        getRootPane().setDefaultButton(loginButton);

        setSize(1000, 650);
        setLocationRelativeTo(null);
    }

    private JPanel createLoginScreen() {
        JPanel screen = createAuthenticationScreen();
        JPanel content = new JPanel(new BorderLayout());
        content.setBackground(Color.WHITE);

        content.add(
            createInformationPanel(
                "Your books.",
                "All in one place.",
                true
            ),
            BorderLayout.WEST
        );

        JPanel rightPanel = createFormBackground();

        JPanel form = createForm();

        JLabel eyebrow = createEyebrow("WELCOME BACK");

        JLabel title = createTitle("LOG IN");

        JLabel subtitle = createSubtitle(
            "Log in to open your accounting workspace."
        );

        eyebrow.setHorizontalAlignment(SwingConstants.CENTER);
        eyebrow.setMaximumSize(new Dimension(370, 20));

        title.setHorizontalAlignment(SwingConstants.CENTER);
        title.setMaximumSize(new Dimension(370, 40));

        subtitle.setHorizontalAlignment(SwingConstants.CENTER);
        subtitle.setMaximumSize(new Dimension(370, 25));

        styleTextField(loginUsername);
        styleTextField(loginPassword);

        stylePrimaryButton(loginButton, NAVY);
        styleLinkButton(openSignUpButton);

        openSignUpButton.setHorizontalAlignment(SwingConstants.CENTER);
        openSignUpButton.setMaximumSize(new Dimension(370, 30));    

        loginButton.setPreferredSize(new Dimension(370, 38));
        loginButton.setMaximumSize(new Dimension(370, 38));

        form.add(eyebrow);
        form.add(Box.createVerticalStrut(10));

        form.add(title);
        form.add(Box.createVerticalStrut(10));

        form.add(subtitle);
        form.add(Box.createVerticalStrut(30));

        addField(
            form,
            "Username",
            loginUsername
        );

        addField(
            form,
            "Password",
            loginPassword
        );

        JLabel helper = createHelper(
            "Use your registered username and password."
        );

        form.add(helper);
        form.add(Box.createVerticalStrut(15));

        form.add(loginButton);
        form.add(Box.createVerticalStrut(18));

        form.add(openSignUpButton);

        rightPanel.add(form, new GridBagConstraints());

        content.add(rightPanel, BorderLayout.CENTER);
        screen.add(content, BorderLayout.CENTER);

        return screen;
    }

    private JPanel createSignUpScreen() {
        JPanel screen = createAuthenticationScreen();
        JPanel content = new JPanel(new BorderLayout());
        content.setBackground(Color.WHITE);

        content.add(
            createInformationPanel(
                "A fresh start",
                "for your books.",
                false
            ),
            BorderLayout.WEST
        );

        JPanel rightPanel = createFormBackground();

        JPanel form = createForm();

        JLabel eyebrow = createEyebrow(
            "CREATE YOUR ACCOUNT"
        );

        JLabel title = createTitle("SIGN UP");

        JLabel subtitle = createSubtitle(
            "Set up your account to get started."
        );

        styleTextField(signUpUsername);
        styleTextField(signUpPassword);
        styleTextField(confirmPassword);

        stylePrimaryButton(
            createAccountButton,
            ACCENT_BLUE
        );

        createAccountButton.setPreferredSize(new Dimension(150, 38));
        createAccountButton.setMaximumSize(new Dimension(150, 38));

        styleLinkButton(openLoginButton);
        openLoginButton.setToolTipText("Return to the log in screen");

        form.add(eyebrow);
        form.add(Box.createVerticalStrut(10));

        form.add(title);
        form.add(Box.createVerticalStrut(10));

        form.add(subtitle);
        form.add(Box.createVerticalStrut(25));

        addField(
            form,
            "Username",
            signUpUsername
        );

        addField(
            form,
            "Password",
            signUpPassword
        );

        addField(
            form,
            "Confirm password",
            confirmPassword
        );

        JLabel helper = createHelper(
            "Use 3–30 letters, numbers or underscores for your "
                + "username and at least 8 characters for your password."
        );

        form.add(helper);
        form.add(Box.createVerticalStrut(15));

        form.add(createAccountButton);
        form.add(Box.createVerticalStrut(18));

        form.add(openLoginButton);

        rightPanel.add(form, new GridBagConstraints());

        content.add(rightPanel, BorderLayout.CENTER);
        screen.add(content, BorderLayout.CENTER);

        return screen;
    }

    private JPanel createAuthenticationScreen() {
        JPanel screen = new JPanel(new BorderLayout());

        return screen;
    }

    private JPanel createInformationPanel(
        String firstLine,
        String secondLine,
        boolean showLedgerIllustration
    ) {
        JPanel panel = new JPanel() {
            @Override
            protected void paintComponent(Graphics graphics) {
                super.paintComponent(graphics);

                Graphics2D g2 = (Graphics2D) graphics.create();
                g2.setRenderingHint(
                    RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON
                );
                g2.setColor(new Color(62, 111, 157, 85));
                g2.fillOval(getWidth() - 150, -105, 290, 290);
                g2.setColor(new Color(116, 161, 197, 45));
                g2.drawOval(getWidth() - 175, -130, 340, 340);
                g2.drawOval(getWidth() - 195, -150, 380, 380);
                g2.setColor(new Color(62, 111, 157, 75));
                g2.fillOval(35, getHeight() - 210, 220, 220);
                g2.dispose();
            }
        };

        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setOpaque(true);

        panel.setBackground(NAVY);
        panel.setPreferredSize(new Dimension(385, 0));
        panel.setBorder(new EmptyBorder(35, 35, 30, 35));

        JLabel brand = whiteLabel(
            "▣  ACCOUNTING SYSTEM",
            Font.BOLD,
            13
        );

        JLabel heading1 = whiteLabel(
            firstLine,
            Font.BOLD,
            30
        );

        JLabel heading2 = whiteLabel(
            secondLine,
            Font.BOLD,
            30
        );
        JSeparator headingRule = new JSeparator();
        headingRule.setBackground(new Color(220, 234, 248));
        headingRule.setForeground(new Color(220, 234, 248));
        headingRule.setMaximumSize(new Dimension(22, 3));
        headingRule.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel description = new JLabel(
            "<html>Record transactions, organize accounts, "
                + "and understand your finances with confidence.</html>"
        );

        description.setForeground(
            new Color(205, 219, 236)
        );

        description.setFont(
            new Font("SansSerif", Font.PLAIN, 13)
        );

        description.setAlignmentX(Component.LEFT_ALIGNMENT);

        panel.add(brand);
        if (showLedgerIllustration) {
            panel.add(Box.createVerticalStrut(22));
            panel.add(headingRule);
            panel.add(Box.createVerticalStrut(10));
        } else {
            panel.add(Box.createVerticalStrut(150));
        }

        panel.add(heading1);
        panel.add(Box.createVerticalStrut(4));
        panel.add(heading2);

        panel.add(Box.createVerticalStrut(14));
        panel.add(description);

        panel.add(Box.createVerticalStrut(18));

        panel.add(featureCard("✓  Simple double-entry recording"));

        panel.add(Box.createVerticalStrut(7));

        panel.add(featureCard("✓  Journal and ledger in sync"));

        panel.add(Box.createVerticalStrut(7));

        panel.add(featureCard("✓  Clear financial reports"));

        if (showLedgerIllustration) {
            panel.add(Box.createVerticalStrut(12));
            panel.add(new LedgerIllustration());
        }

        panel.add(Box.createVerticalGlue());

        JSeparator separator = new JSeparator();

        separator.setForeground(
            new Color(135, 157, 183)
        );

        separator.setMaximumSize(
            new Dimension(Integer.MAX_VALUE, 1)
        );

        separator.setAlignmentX(Component.LEFT_ALIGNMENT);

        panel.add(separator);
        panel.add(Box.createVerticalStrut(18));

        JLabel footer = whiteLabel(
            "Built for organized, confident accounting.",
            Font.PLAIN,
            11
        );

        footer.setForeground(
            new Color(193, 208, 226)
        );

        panel.add(footer);

        return panel;
    }

    private JPanel featureCard(String text) {
        JPanel card = new JPanel(new BorderLayout());
        card.setOpaque(true);
        card.setBackground(new Color(255, 255, 255, 22));
        card.setBorder(new EmptyBorder(7, 10, 7, 10));
        card.setMaximumSize(new Dimension(Integer.MAX_VALUE, 34));
        card.setAlignmentX(Component.LEFT_ALIGNMENT);
        card.add(featureLabel(text), BorderLayout.CENTER);
        return card;
    }

    private static class LedgerIllustration extends JPanel {
        LedgerIllustration() {
            setOpaque(false);
            setPreferredSize(new Dimension(230, 150));
            setMaximumSize(new Dimension(Integer.MAX_VALUE, 150));
            setAlignmentX(Component.LEFT_ALIGNMENT);
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            super.paintComponent(graphics);

            Graphics2D g2 = (Graphics2D) graphics.create();
            g2.setRenderingHint(
                RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_ON
            );

            int cardWidth = Math.min(getWidth() - 24, 230);
            int cardX = 12;
            int cardY = 18;

            g2.setColor(new Color(42, 116, 173, 100));
            g2.fillOval(35, 18, 165, 135);

            g2.setColor(Color.WHITE);
            g2.fillRoundRect(cardX, cardY, cardWidth, 112, 12, 12);
            g2.setColor(new Color(232, 241, 249));
            g2.fillRoundRect(cardX, cardY, cardWidth, 16, 12, 12);
            g2.fillRect(cardX, cardY + 9, cardWidth, 7);

            g2.setColor(new Color(22, 52, 91));
            g2.fillRoundRect(cardX + 14, cardY + 28, 57, 5, 3, 3);
            g2.setColor(new Color(153, 171, 192));
            g2.fillRoundRect(cardX + 14, cardY + 38, 37, 3, 2, 2);

            g2.setColor(new Color(225, 233, 241));
            g2.drawLine(cardX + 12, cardY + 54, cardX + cardWidth - 12, cardY + 54);

            for (int row = 0; row < 3; row++) {
                int y = cardY + 66 + row * 14;
                g2.setColor(ACCENT_BLUE);
                g2.fillRoundRect(cardX + 14, y, 5, 5, 2, 2);
                g2.setColor(new Color(112, 134, 160));
                g2.fillRoundRect(cardX + 26, y + 1, 58, 3, 2, 2);
                g2.fillRoundRect(cardX + 108, y + 1, 22, 3, 2, 2);
                g2.fillRoundRect(cardX + 143, y + 1, 40, 3, 2, 2);
            }

            g2.setColor(ACCENT_BLUE);
            g2.fillRoundRect(cardX + cardWidth - 27, cardY + 93, 44, 38, 10, 10);
            g2.setColor(Color.WHITE);
            g2.fillRoundRect(cardX + cardWidth - 16, cardY + 105, 22, 3, 2, 2);
            g2.fillRoundRect(cardX + cardWidth - 16, cardY + 113, 22, 3, 2, 2);
            g2.dispose();
        }
    }

    private JLabel whiteLabel(
        String text,
        int style,
        int size
    ) {
        JLabel label = new JLabel(text);

        label.setForeground(Color.WHITE);

        label.setFont(
            new Font("SansSerif", style, size)
        );

        label.setAlignmentX(Component.LEFT_ALIGNMENT);

        return label;
    }

    private JLabel featureLabel(String text) {
        return whiteLabel(text, Font.PLAIN, 13);
    }

    private JPanel createFormBackground() {
        JPanel panel = new JPanel(new GridBagLayout());

        panel.setBackground(Color.WHITE);

        panel.setBorder(
            new EmptyBorder(35, 45, 35, 45)
        );

        return panel;
    }

    private JPanel createForm() {
       JPanel panel = new JPanel() {
        @Override
        public Dimension getPreferredSize() {
            Dimension size = super.getPreferredSize();
            return new Dimension(370, size.height);
        }
    };

    panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
    panel.setOpaque(false);

        return panel;
    }

    private JLabel createEyebrow(String text) {
        JLabel label = new JLabel(text);

        label.setForeground(ACCENT_BLUE);

        label.setFont(
            new Font("SansSerif", Font.BOLD, 10)
        );

        label.setAlignmentX(Component.LEFT_ALIGNMENT);

        return label;
    }

    private JLabel createTitle(String text) {
        JLabel label = new JLabel(text);

        label.setForeground(DARK_TEXT);

        label.setFont(
            new Font("SansSerif", Font.BOLD, 27)
        );

        label.setAlignmentX(Component.LEFT_ALIGNMENT);

        return label;
    }

    private JLabel createSubtitle(String text) {
        JLabel label = new JLabel(text);

        label.setForeground(LIGHT_TEXT);

        label.setFont(
            new Font("SansSerif", Font.PLAIN, 13)
        );

        label.setAlignmentX(Component.LEFT_ALIGNMENT);

        return label;
    }

    private JLabel createHelper(String text) {
        JLabel label = new JLabel(
            "<html>" + text + "</html>"
        );

        label.setForeground(LIGHT_TEXT);

        label.setFont(
            new Font("SansSerif", Font.PLAIN, 11)
        );

        label.setAlignmentX(Component.LEFT_ALIGNMENT);

        return label;
    }

    private void addField(
        JPanel form,
        String title,
        JTextField field
    ) {
        JLabel label = new JLabel(title);

        label.setForeground(DARK_TEXT);

        label.setFont(
            new Font("SansSerif", Font.BOLD, 12)
        );

        label.setAlignmentX(Component.LEFT_ALIGNMENT);

        form.add(label);
        form.add(Box.createVerticalStrut(7));

        form.add(field);
        form.add(Box.createVerticalStrut(18));
    }

    private void styleTextField(JTextField field) {
        field.setFont(
            new Font("SansSerif", Font.PLAIN, 14)
        );

        field.setForeground(DARK_TEXT);

        field.setBackground(FIELD_BACKGROUND);

        field.setBorder(
            BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(
                    new Color(218, 228, 240)
                ),
                new EmptyBorder(10, 12, 10, 12)
            )
        );
    field.setPreferredSize(new Dimension(370, 44));
    field.setMaximumSize(new Dimension(370, 44));
    field.setAlignmentX(Component.LEFT_ALIGNMENT);
    }

    private void stylePrimaryButton(
        JButton button,
        Color background
    ) {
        button.setBackground(background);
        button.setForeground(Color.WHITE);

        button.setFont(
            new Font("SansSerif", Font.BOLD, 13)
        );

        button.setFocusPainted(false);

        button.setBorder(
            new EmptyBorder(12, 15, 12, 15)
        );

        button.setCursor(
            Cursor.getPredefinedCursor(
                Cursor.HAND_CURSOR
            )
        );

        button.setMaximumSize(
            new Dimension(Integer.MAX_VALUE, 45)
        );

        button.setAlignmentX(Component.LEFT_ALIGNMENT);
    }

    private void styleLinkButton(JButton button) {
        button.setForeground(ACCENT_BLUE);

        button.setFont(
            new Font("SansSerif", Font.PLAIN, 12)
        );

        button.setBorderPainted(false);
        button.setContentAreaFilled(false);
        button.setFocusPainted(false);

        button.setBorder(
            new EmptyBorder(0, 0, 0, 0)
        );

        button.setCursor(
            Cursor.getPredefinedCursor(
                Cursor.HAND_CURSOR
            )
        );

        button.setAlignmentX(Component.LEFT_ALIGNMENT);
    }

    private void showLogin() {
        signUpPassword.setText("");
        confirmPassword.setText("");

        cardLayout.show(cardPanel, "LOGIN");

        getRootPane().setDefaultButton(loginButton);

        loginUsername.requestFocusInWindow();
    }

    private void login() {
        String username = loginUsername.getText().trim();

        char[] password = loginPassword.getPassword();

        if (username.isEmpty() || password.length == 0) {
            Arrays.fill(password, '\0');

            showMessage(
                "Enter your username and password."
            );

            return;
        }

        setBusy(true);

        new SwingWorker<String, Void>() {

            @Override
            protected String doInBackground()
                throws Exception {

                try {
                    return AuthService.login(
                        username,
                        password
                    );

                } finally {
                    Arrays.fill(password, '\0');
                }
            }

            @Override
            protected void done() {
                try {
                    String authenticatedUser = get();

                    loginPassword.setText("");

                    if (authenticatedUser == null) {
                        showMessage(
                            "Incorrect username or password."
                        );

                        return;
                    }

                    Session.login(authenticatedUser);

                    App.openAccountingWindow(
                        authenticatedUser
                    );

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
        String username =
            signUpUsername.getText().trim();

        char[] password =
            signUpPassword.getPassword();

        char[] confirmation =
            confirmPassword.getPassword();

        boolean passwordsMatch =
            Arrays.equals(password, confirmation);

        Arrays.fill(confirmation, '\0');

        if (!passwordsMatch) {
            Arrays.fill(password, '\0');

            showMessage(
                "Passwords do not match."
            );

            return;
        }

        setBusy(true);

        new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() throws Exception {
                try {
                    AuthService.register(
                        username,
                        password
                    );

                    return null;

                } finally {
                    Arrays.fill(password, '\0');
                }
            }


            @Override
            protected void done() {
                try {
                    get();

                    signUpPassword.setText("");
                    confirmPassword.setText("");

                    loginUsername.setText(username);
                    loginPassword.setText("");

                    showMessage(
                        "Account created successfully. "
                            + "You can now log in."
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
        openSignUpButton.setEnabled(!busy);

        createAccountButton.setEnabled(!busy);
        openLoginButton.setEnabled(!busy);

        loginUsername.setEnabled(!busy);
        loginPassword.setEnabled(!busy);

        signUpUsername.setEnabled(!busy);
        signUpPassword.setEnabled(!busy);
        confirmPassword.setEnabled(!busy);

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