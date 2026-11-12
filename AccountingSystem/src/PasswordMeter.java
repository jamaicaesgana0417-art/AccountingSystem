import java.awt.*;
import java.util.Arrays;
import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

public class PasswordMeter extends JPanel {

    private static final Color[] COLORS = {
        AuthStyle.BORDER, AuthStyle.ERROR, new Color(0xE0A800),
        new Color(0x7CB342), new Color(0x2E9E5B)
    };
    private static final String[] NAMES = { " ", "Weak", "Fair", "Good", "Strong" };

    private int score = 0;
    private final JLabel nameLabel = new JLabel(" ");
    private final JTextArea hintArea = new JTextArea();
    private final JPanel bars;

    public PasswordMeter() {
        setLayout(new BorderLayout(0, 4));
        setOpaque(false);

        bars = new JPanel() {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                        RenderingHints.VALUE_ANTIALIAS_ON);
                int gap = 4;
                int w = (getWidth() - gap * 3) / 4;
                int h = 6;
                int y = (getHeight() - h) / 2;
                for (int i = 0; i < 4; i++) {
                    g2.setColor(i < score ? COLORS[score] : AuthStyle.BORDER);
                    g2.fillRoundRect(i * (w + gap), y, w, h, h, h);
                }
                g2.dispose();
            }
        };
        bars.setOpaque(false);
        bars.setPreferredSize(new Dimension(10, 18));

        nameLabel.setFont(AuthStyle.font(Font.BOLD, 12));
        nameLabel.setPreferredSize(new Dimension(54, 18));
        nameLabel.setHorizontalAlignment(SwingConstants.RIGHT);

        // Hint area configured to wrap and only show when there is text (removes the gap)
        hintArea.setEditable(false);
        hintArea.setFocusable(false);
        hintArea.setOpaque(false);
        hintArea.setLineWrap(true);
        hintArea.setWrapStyleWord(true);
        hintArea.setBorder(null);
        hintArea.setFont(AuthStyle.font(Font.BOLD, 11));
        hintArea.setForeground(AuthStyle.ERROR);
        hintArea.setVisible(false); // HIDE BY DEFAULT TO REMOVE GAP

        JPanel top = new JPanel(new BorderLayout(10, 0));
        top.setOpaque(false);
        top.add(bars, BorderLayout.CENTER);
        top.add(nameLabel, BorderLayout.EAST);

        add(top, BorderLayout.NORTH);
        add(hintArea, BorderLayout.CENTER);
    }

    public void update(char[] password, String username) {
        score = PasswordPolicy.score(password, username);
        nameLabel.setText(NAMES[score]);
        nameLabel.setForeground(COLORS[score]);

        String problem = password.length == 0
                ? null : PasswordPolicy.validate(password, username);

        if (problem != null && !problem.isEmpty()) {
            hintArea.setText(problem);
            hintArea.setVisible(true);
        } else {
            hintArea.setText("");
            hintArea.setVisible(false);
        }

        bars.repaint();
        revalidate();
    }

    // Updates the meter live while the user types.
    public void attach(JPasswordField passwordField, JTextField usernameField) {
        DocumentListener listener = new DocumentListener() {
            @Override public void insertUpdate(DocumentEvent e) { refresh(); }
            @Override public void removeUpdate(DocumentEvent e) { refresh(); }
            @Override public void changedUpdate(DocumentEvent e) { refresh(); }

            private void refresh() {
                char[] p = passwordField.getPassword();
                update(p, usernameField.getText());
                Arrays.fill(p, '\0');
            }
        };
        passwordField.getDocument().addDocumentListener(listener);
        usernameField.getDocument().addDocumentListener(listener);
    }
}