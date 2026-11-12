import java.awt.*;
import javax.swing.*;

/**
 * One window for Login, Sign Up and Reset Password.
 * Replaces LoginFrame. After a good login it opens the accounting window.
 */
public class AuthFrame extends JFrame {

    private final CardLayout cards = new CardLayout();
    private final JPanel root = new JPanel(cards);

    private final LoginPanel login = new LoginPanel();
    private final SignUpPanel signUp = new SignUpPanel();
    private final ForgotPasswordPanel forgot = new ForgotPasswordPanel();

    public AuthFrame() {
        super("Accounting System");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        root.add(login, "login");
        root.add(signUp, "signup");
        root.add(forgot, "forgot");

        login.setOnSignUpClick(() -> cards.show(root, "signup"));
        login.setOnForgotClick(() -> cards.show(root, "forgot"));
        login.setOnLoginSuccess(user -> {
            Session.login(user);
            App.openAccountingWindow(user);
            dispose();
        });

        signUp.setOnBackToLogin(() -> cards.show(root, "login"));
        signUp.setOnSignUpSuccess(() -> cards.show(root, "login"));

        forgot.setOnBackToLogin(() -> cards.show(root, "login"));
        forgot.setOnResetDone(() -> {
            login.clearSensitive();
            cards.show(root, "login");
        });

        setContentPane(root);
        setSize(1180, 760);
        setMinimumSize(new Dimension(900, 620));
        setLocationRelativeTo(null);
    }
}