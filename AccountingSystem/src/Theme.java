import java.awt.*;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import javax.swing.*;
import javax.swing.text.JTextComponent;

public class Theme {

    // Mixed style: TikTok (dark + red + cyan), Facebook (clean + rounded),
    // Claude (warm tones + serif heading)
    public static final Color BG     = new Color(0x121110);
    public static final Color CARD   = new Color(0x1D1B18);
    public static final Color FIELD  = new Color(0x2A2724);
    public static final Color TEXT   = new Color(0xF5F1EA);
    public static final Color MUTED  = new Color(0xA39E94);
    public static final Color BORDER = new Color(0x3A3630);
    public static final Color ACCENT = new Color(0xFE2C55); // buttons
    public static final Color FOCUS  = new Color(0x25F4EE); // focused field glow
    public static final Color ERROR  = new Color(0xFF7A7A);

    public static final Font HEADING = new Font("Georgia", Font.BOLD, 26);
    public static final Font BODY    = new Font("Segoe UI", Font.PLAIN, 14);
    public static final Font BUTTON  = new Font("Segoe UI", Font.BOLD, 15);

    private static final int RADIUS = 14;

    private static Graphics2D smooth(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_ON);
        return g2;
    }

    // ---------- Card ----------
    public static JPanel card() {
        JPanel p = new JPanel() {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = smooth(g);
                g2.setColor(CARD);
                g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1,
                        RADIUS * 2, RADIUS * 2);
                g2.setColor(BORDER);
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1,
                        RADIUS * 2, RADIUS * 2);
                g2.dispose();
            }
        };
        p.setOpaque(false);
        return p;
    }

    // ---------- Text fields ----------
    public static JTextField textField(String hint) {
        JTextField f = new JTextField() {
            @Override protected void paintComponent(Graphics g) {
                paintFieldBackground(this, g);
                super.paintComponent(g);
                if (getText().isEmpty() && !isFocusOwner()) paintHint(this, g, hint);
            }
            @Override protected void paintBorder(Graphics g) {
                paintFieldOutline(this, g);
            }
        };
        styleField(f);
        return f;
    }

    public static JPasswordField passwordField(String hint) {
        JPasswordField f = new JPasswordField() {
            @Override protected void paintComponent(Graphics g) {
                paintFieldBackground(this, g);
                super.paintComponent(g);
                if (getPassword().length == 0 && !isFocusOwner()) paintHint(this, g, hint);
            }
            @Override protected void paintBorder(Graphics g) {
                paintFieldOutline(this, g);
            }
        };
        styleField(f);
        return f;
    }

    private static void styleField(JTextComponent f) {
        f.setOpaque(false);
        f.setFont(BODY);
        f.setForeground(TEXT);
        f.setCaretColor(FOCUS);
        f.setSelectionColor(FOCUS);
        f.setSelectedTextColor(Color.BLACK);
        f.setBorder(BorderFactory.createEmptyBorder(0, 14, 0, 14));
        f.setPreferredSize(new Dimension(300, 44));
        f.addFocusListener(new FocusAdapter() {
            @Override public void focusGained(FocusEvent e) { f.repaint(); }
            @Override public void focusLost(FocusEvent e) { f.repaint(); }
        });
    }

    private static void paintFieldBackground(JComponent c, Graphics g) {
        Graphics2D g2 = smooth(g);
        g2.setColor(FIELD);
        g2.fillRoundRect(0, 0, c.getWidth() - 1, c.getHeight() - 1, RADIUS, RADIUS);
        g2.dispose();
    }

    private static void paintFieldOutline(JComponent c, Graphics g) {
        Graphics2D g2 = smooth(g);
        boolean focused = c.isFocusOwner();
        g2.setColor(focused ? FOCUS : BORDER);
        g2.setStroke(new BasicStroke(focused ? 2f : 1f));
        g2.drawRoundRect(1, 1, c.getWidth() - 3, c.getHeight() - 3, RADIUS, RADIUS);
        g2.dispose();
    }

    private static void paintHint(JComponent c, Graphics g, String hint) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g2.setColor(MUTED);
        g2.setFont(BODY);
        FontMetrics fm = g2.getFontMetrics();
        int x = c.getInsets().left;
        int y = (c.getHeight() - fm.getHeight()) / 2 + fm.getAscent();
        g2.drawString(hint, x, y);
        g2.dispose();
    }

    // ---------- Buttons ----------
    public static JButton primaryButton(String text) { return button(text, true); }
    public static JButton secondaryButton(String text) { return button(text, false); }

    private static JButton button(String text, boolean primary) {
        JButton b = new JButton(text) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = smooth(g);
                Color fill;
                if (primary) {
                    fill = getModel().isPressed() ? ACCENT.darker().darker()
                         : getModel().isRollover() ? ACCENT.darker()
                         : ACCENT;
                } else {
                    fill = getModel().isRollover() ? BORDER : FIELD;
                }
                if (!isEnabled()) fill = BORDER;
                g2.setColor(fill);
                g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, RADIUS, RADIUS);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        b.setFont(BUTTON);
        b.setForeground(primary ? Color.WHITE : TEXT);
        b.setOpaque(false);
        b.setContentAreaFilled(false);
        b.setBorderPainted(false);
        b.setFocusPainted(false);
        b.setRolloverEnabled(true);
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        b.setPreferredSize(new Dimension(300, 44));
        return b;
    }
}