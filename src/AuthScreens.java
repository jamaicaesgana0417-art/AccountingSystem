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

            root.add(login, "login");
            root.add(signUp, "signup");

            login.setOnSignUpClick(() -> cards.show(root, "signup"));
            login.setOnLoginSuccess(user ->
                JOptionPane.showMessageDialog(frame, "Welcome, " + user + "!"));

            signUp.setOnBackToLogin(() -> cards.show(root, "login"));
            signUp.setOnSignUpSuccess(() -> {
                JOptionPane.showMessageDialog(frame,
                    "Account created! You can log in now.");
                cards.show(root, "login");
            });

            frame.setContentPane(root);
            frame.setSize(500, 660);
            frame.setLocationRelativeTo(null);
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setVisible(true);
        });
    }
}