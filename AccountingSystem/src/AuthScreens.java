import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.RoundRectangle2D;
import javax.swing.*;
import javax.swing.border.EmptyBorder;

/** Owns navigation and the compact, responsive sizing of authentication screens. */
public final class AuthScreens {

    private static final Dimension LOGIN_SIZE = new Dimension(1100, 520);
    private static final Dimension SIGNUP_SIZE = new Dimension(950, 470);
    private static final Dimension FORGOT_SIZE = new Dimension(950, 440);
    private static final Dimension RESET_SIZE = new Dimension(1000, 520);

    private AuthScreens() {}

    public static void show() {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("Accounting System");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setMinimumSize(new Dimension(820, 380));

            CardLayout cards = new CardLayout();
            JPanel cardDeck = new JPanel(cards);
            cardDeck.setOpaque(false);

            LoginPanel login = new LoginPanel();
            SignUpPanel signUp = new SignUpPanel();
            ForgotPasswordPanel forgot = new ForgotPasswordPanel();

            cardDeck.add(framed(login), "login");
            cardDeck.add(framed(signUp), "signup");
            cardDeck.add(framed(forgot), "forgot");

            JPanel root = new JPanel(new BorderLayout());
            root.setBackground(new Color(0x173B66));
            root.setBorder(new EmptyBorder(12, 12, 12, 12));
            root.add(cardDeck, BorderLayout.CENTER);

            Runnable showLogin = () -> {
                cards.show(cardDeck, "login");
                resizeFrame(frame, LOGIN_SIZE);
            };

            login.setOnSignUpClick(() -> {
                cards.show(cardDeck, "signup");
                resizeFrame(frame, SIGNUP_SIZE);
            });

            login.setOnForgotClick(() -> {
                forgot.prepareForDisplay();
                cards.show(cardDeck, "forgot");
                resizeFrame(frame, FORGOT_SIZE);
            });

            login.setOnLoginSuccess(user -> {
                Session.login(user);
                frame.dispose();
                App.openAccountingWindow(user);
            });

            signUp.setOnBackToLogin(showLogin);
            signUp.setOnSignUpSuccess(() -> {
                login.clearSensitive();
                showLogin.run();
            });

            forgot.setOnBackToLogin(showLogin);
            forgot.setOnResetDone(() -> {
                login.clearSensitive();
                showLogin.run();
            });
            forgot.setOnStepChange(resetStep ->
                resizeFrame(frame, resetStep ? RESET_SIZE : FORGOT_SIZE)
            );

            frame.setContentPane(root);
            frame.setSize(LOGIN_SIZE);
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
        });
    }

    private static void resizeFrame(JFrame frame, Dimension size) {
        frame.setSize(size);
        frame.setLocationRelativeTo(null);
        frame.revalidate();
    }

    /** Clips each screen to a subtle rounded card, like the supplied reference. */
    private static JPanel framed(JPanel screen) {
        JPanel card = new JPanel(new BorderLayout()) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                                    RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(Color.WHITE);
                g2.fill(new RoundRectangle2D.Float(
                    0, 0, getWidth() - 1, getHeight() - 1, 20, 20
                ));
                g2.dispose();
            }

            @Override
            protected void paintChildren(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.clip(new RoundRectangle2D.Float(
                    0, 0, getWidth(), getHeight(), 20, 20
                ));
                super.paintChildren(g2);
                g2.dispose();
            }

            @Override
            protected void paintBorder(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                                    RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(0xC6D9F3));
                g2.draw(new RoundRectangle2D.Float(
                    0.5f, 0.5f, getWidth() - 1, getHeight() - 1, 20, 20
                ));
                g2.dispose();
            }
        };
        card.setOpaque(false);
        card.add(screen, BorderLayout.CENTER);
        return card;
    }

    public static void main(String[] args) {
        show();
    }
}
