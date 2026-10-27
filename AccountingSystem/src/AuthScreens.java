import java.awt.*;
import javax.swing.*;

public class AuthScreens {

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("Accounting System");

            CardLayout cards = new CardLayout();
            JPanel root = new JPanel(cards);

            LoginPanel login = new LoginPanel();
            SignUpPanel signUp = new SignUpPanel();
            ForgotPasswordPanel forgot = new ForgotPasswordPanel();

            root.add(login, "login");
            root.add(signUp, "signup");
            root.add(forgot, "forgot");

            login.setOnSignUpClick(() -> cards.show(root, "signup"));
            login.setOnForgotClick(() -> cards.show(root, "forgot"));
            login.setOnLoginSuccess(user ->
                JOptionPane.showMessageDialog(frame, "Welcome, " + user + "!"));

            signUp.setOnBackToLogin(() -> cards.show(root, "login"));
            signUp.setOnSignUpSuccess(() -> cards.show(root, "login"));

            forgot.setOnBackToLogin(() -> cards.show(root, "login"));
            forgot.setOnResetDone(() -> {
                JOptionPane.showMessageDialog(frame,
                    "Password changed. You can log in now.");
                login.clearSensitive();
                cards.show(root, "login");
            });

            frame.setContentPane(root);
            frame.setSize(500, 740);
            frame.setLocationRelativeTo(null);
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setVisible(true);
        });
    }
}