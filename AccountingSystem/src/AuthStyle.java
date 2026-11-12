import java.awt.*;
import java.awt.geom.Ellipse2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.text.JTextComponent;

/** Shared two-panel authentication design used by Login and Recovery screens. */
public final class AuthStyle {

    public static final Color NAVY = new Color(0x193B63);
    public static final Color NAVY_LIGHT = new Color(0x294D76);
    public static final Color PAGE = new Color(0xF1F5FB);
    public static final Color FIELD = Color.WHITE;
    public static final Color TEXT = new Color(0x17395F);
    public static final Color TEXT_MUTED = new Color(0x60758F);
    public static final Color BORDER = new Color(0xD7E0EA);
    public static final Color FOCUS = new Color(0x2878D0);
    public static final Color ERROR = new Color(0xC43D3D);

    private static final Font BASE = new Font("Segoe UI", Font.PLAIN, 14);
    private static final int RADIUS = 8;

    private AuthStyle() {}

    public static Font font(int style, int size) {
        return new Font("Segoe UI", style, size);
    }

    public static JLabel title(String text) {
        JLabel label = new JLabel(text);
        label.setFont(font(Font.BOLD, 30));
        label.setForeground(TEXT);
        return label;
    }

    public static JLabel subtitle(String text) {
        JLabel label = new JLabel(text);
        label.setFont(font(Font.PLAIN, 14));
        label.setForeground(TEXT_MUTED);
        return label;
    }

    public static JLabel fieldLabel(String text) {
        JLabel label = new JLabel(text);
        label.setFont(font(Font.BOLD, 12));
        label.setForeground(TEXT);
        return label;
    }

    public static JCheckBox checkBox(String text) {
        JCheckBox box = new JCheckBox(text);
        box.setFont(font(Font.PLAIN, 12));
        box.setForeground(TEXT_MUTED);
        box.setOpaque(false);
        box.setFocusPainted(false);
        box.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return box;
    }

    public static JButton linkButton(String text, int size, boolean bold) {
        String safe = text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
        String html = "<html><u>" + safe + "</u></html>";
        JButton button = new JButton(html);
        button.setFont(font(bold ? Font.BOLD : Font.PLAIN, size));
        button.setForeground(TEXT);
        button.setBorderPainted(false);
        button.setContentAreaFilled(false);
        button.setFocusPainted(false);
        button.setOpaque(false);
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        button.setMargin(new Insets(0, 0, 0, 0));
        return button;
    }

    public static class ArrowButton extends JButton {
        public ArrowButton(String text) {
            super(text + "  →");
            setFont(font(Font.BOLD, 14));
            setForeground(Color.WHITE);
            setPreferredSize(new Dimension(410, 44));
            setMinimumSize(new Dimension(100, 44));
            setFocusPainted(false);
            setBorderPainted(false);
            setContentAreaFilled(false);
            setOpaque(false);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            setHorizontalAlignment(SwingConstants.CENTER);
        }

        @Override protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            Color fill = NAVY;
            if (!isEnabled()) fill = new Color(0x9AAABD);
            else if (getModel().isPressed()) fill = NAVY.darker();
            else if (getModel().isRollover()) fill = NAVY_LIGHT;
            g2.setColor(fill);
            g2.fillRoundRect(0, 0, getWidth(), getHeight(), RADIUS, RADIUS);
            g2.dispose();
            super.paintComponent(g);
        }
    }

    public static class RoundedTextField extends JTextField {
        private final String hint;
        public RoundedTextField(String hint) {
            this.hint = hint;
            styleField(this);
        }
        @Override protected void paintComponent(Graphics g) {
            paintFieldBackground(this, g);
            super.paintComponent(g);
            if (getDocument().getLength() == 0) paintHint(this, g, hint);
        }
        @Override protected void paintBorder(Graphics g) { paintFieldBorder(this, g); }
    }

    public static class RoundedPasswordField extends JPasswordField {
        private final String hint;
        public RoundedPasswordField(String hint) {
            this.hint = hint;
            styleField(this);
        }
        @Override protected void paintComponent(Graphics g) {
            paintFieldBackground(this, g);
            super.paintComponent(g);
            if (getDocument().getLength() == 0) paintHint(this, g, hint);
        }
        @Override protected void paintBorder(Graphics g) { paintFieldBorder(this, g); }
    }

    private static void styleField(JTextComponent field) {
        field.setOpaque(false);
        field.setFont(BASE);
        field.setForeground(TEXT);
        field.setCaretColor(FOCUS);
        field.setSelectionColor(new Color(0xCFE2F8));
        field.setSelectedTextColor(TEXT);
        field.setBorder(new EmptyBorder(0, 12, 0, 12));
        field.setPreferredSize(new Dimension(410, 42));
        field.setMinimumSize(new Dimension(100, 42));
    }

    private static void paintFieldBackground(JComponent component, Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setColor(FIELD);
        g2.fillRoundRect(0, 0, component.getWidth() - 1, component.getHeight() - 1, RADIUS, RADIUS);
        g2.dispose();
    }

    private static void paintFieldBorder(JComponent component, Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setColor(component.isFocusOwner() ? FOCUS : BORDER);
        g2.setStroke(new BasicStroke(component.isFocusOwner() ? 1.7f : 1f));
        g2.drawRoundRect(1, 1, component.getWidth() - 3, component.getHeight() - 3, RADIUS, RADIUS);
        g2.dispose();
    }

    private static void paintHint(JComponent component, Graphics g, String hint) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g2.setFont(BASE);
        g2.setColor(TEXT_MUTED);
        FontMetrics fm = g2.getFontMetrics();
        Insets insets = component.getInsets();
        int y = (component.getHeight() - fm.getHeight()) / 2 + fm.getAscent();
        // Keep the hint visible on focus; hide it as soon as the user types.
        // This avoids the hint flashing away when the field is clicked.
        int x = insets.left;
        g2.drawString(hint, x, y);
        g2.dispose();
    }

    public static void bindShowPassword(JCheckBox toggle, JPasswordField... fields) {
        char[] echoChars = new char[fields.length];
        for (int i = 0; i < fields.length; i++) echoChars[i] = fields[i].getEchoChar();
        toggle.addActionListener(event -> {
            for (int i = 0; i < fields.length; i++) {
                fields[i].setEchoChar(toggle.isSelected() ? (char) 0 : echoChars[i]);
                fields[i].repaint();
            }
        });
    }

    /** Login brand area: the original accounting-workspace photo and message. */
    public static JPanel loginBrandPanel() {
        return createBrandPanel(
            "accounting.png",
            "Your books.<br>All in one<br>place.",
            "Record transactions, organize accounts,<br>and understand your finances with confidence.",
            new String[] {
                "Simple double-entry recording",
                "Journal and ledger in sync",
                "Clear financial reports"
            },
            40, 470, 26, 38
        );
    }

    /** Compact recovery brand area, sized to match the shorter recovery screen. */
    public static JPanel forgotPasswordBrandPanel() {
        return createBrandPanel(
            "recovery.png",
            "Locked out?<br>Let's get you back in.",
            "A few quick steps to recover<br>access to your accounting workspace.",
            null,
            32, 380, 24, 24
        );
    }

    /** Compact registration brand area with its own growth-themed image. */
    public static JPanel signUpBrandPanel() {
        return createBrandPanel(
            "signup.png",
            "Create<br>your account.",
            "Start building better<br>financial habits.",
            null,
            34, 380, 24, 24
        );
    }

    /** Kept as a compatibility alias for older screens. */
    public static JPanel brandPanel() {
        return forgotPasswordBrandPanel();
    }

    private static JPanel createBrandPanel(
        String imageName,
        String headlineText,
        String descriptionText,
        String[] features,
        int headlineSize,
        int brandWidth,
        int topGap,
        int sidePadding
    ) {
        final BufferedImage brandImage = loadBrandImage(imageName);

        JPanel panel = new JPanel(new BorderLayout()) {
            @Override protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                if (brandImage != null) {
                    double scale = Math.max(
                        (double) getWidth() / brandImage.getWidth(),
                        (double) getHeight() / brandImage.getHeight()
                    );
                    int imageWidth = (int) Math.ceil(brandImage.getWidth() * scale);
                    int imageHeight = (int) Math.ceil(brandImage.getHeight() * scale);
                    int imageX = (getWidth() - imageWidth) / 2;
                    int imageY = (getHeight() - imageHeight) / 2;

                    g2.setRenderingHint(
                        RenderingHints.KEY_INTERPOLATION,
                        RenderingHints.VALUE_INTERPOLATION_BICUBIC
                    );
                    g2.drawImage(brandImage, imageX, imageY, imageWidth, imageHeight, null);
                    g2.setColor(new Color(NAVY.getRed(), NAVY.getGreen(), NAVY.getBlue(), 178));
                    g2.fillRect(0, 0, getWidth(), getHeight());
                } else {
                    g2.setColor(NAVY);
                    g2.fillRect(0, 0, getWidth(), getHeight());
                }

                // Subtle dotted detail shared by all three authentication screens.
                g2.setColor(new Color(255, 255, 255, 24));
                for (int row = 0; row < 7; row++) {
                    for (int col = 0; col < 12; col++) {
                        double x = getWidth() - 105 + col * 13;
                        double y = getHeight() - 185 + row * 13;
                        g2.fill(new Ellipse2D.Double(x, y, 3, 3));
                    }
                }
                g2.dispose();
            }
        };

        panel.setPreferredSize(new Dimension(brandWidth, 480));
        panel.setMinimumSize(new Dimension(brandWidth, 0));
        panel.setBorder(new EmptyBorder(30, sidePadding, 22, 24));

        JPanel content = new JPanel();
        content.setOpaque(false);
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));

        JLabel brand = new JLabel("▣  ACCOUNTING SYSTEM");
        brand.setFont(font(Font.BOLD, 16));
        brand.setForeground(Color.WHITE);
        brand.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel headline = new JLabel("<html>" + headlineText + "</html>");
        headline.setFont(font(Font.BOLD, headlineSize));
        headline.setForeground(Color.WHITE);
        headline.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel description = new JLabel("<html>" + descriptionText + "</html>");
        description.setFont(font(Font.PLAIN, 14));
        description.setForeground(new Color(0xE3ECF6));
        description.setAlignmentX(Component.LEFT_ALIGNMENT);

        content.add(brand);
        content.add(Box.createVerticalStrut(topGap));
        content.add(headline);
        content.add(Box.createVerticalStrut(18));
        content.add(description);
        content.add(Box.createVerticalStrut(20));

        if (features != null) {
            for (int i = 0; i < features.length; i++) {
                content.add(feature(features[i]));
                if (i < features.length - 1) {
                    content.add(Box.createVerticalStrut(9));
                }
            }
        }

        panel.add(content, BorderLayout.NORTH);
        return panel;
    }

    private static BufferedImage loadBrandImage(String imageName) {
        String[] classpathPaths = {
            "/PICTURES/" + imageName,
            "/pictures/" + imageName,
            "/" + imageName
        };

        try {
            for (String path : classpathPaths) {
                java.net.URL resource = AuthStyle.class.getResource(path);
                if (resource != null) {
                    BufferedImage image = ImageIO.read(resource);
                    if (image != null) return image;
                }
            }

            // Support both the recommended src/PICTURES folder and older layouts.
            File[] candidates = {
                new File("src/PICTURES", imageName),
                new File("src/pictures", imageName),
                new File("src", imageName),
                new File("AccountingSystem/src/PICTURES", imageName),
                new File("AccountingSystem/src/pictures", imageName),
                new File("AccountingSystem/src", imageName),
                new File("PICTURES", imageName),
                new File("pictures", imageName)
            };

            for (File candidate : candidates) {
                if (candidate.isFile()) {
                    BufferedImage image = ImageIO.read(candidate);
                    if (image != null) return image;
                }
            }
        } catch (IOException exception) {
            System.err.println("Could not load " + imageName + ": " + exception.getMessage());
        }

        System.err.println("Could not find " + imageName + " in the classpath or PICTURES folder.");
        return null;
    }

    private static int brandFeatureWidth() {
        return 340;
    }

    private static JComponent feature(String text) {
        JPanel row = new JPanel(new BorderLayout(10, 0));
        row.setOpaque(true);
        row.setBackground(new Color(0x294B72));
        row.setBorder(new EmptyBorder(10, 12, 10, 12));
        row.setMaximumSize(new Dimension(brandFeatureWidth(), 42));
        row.setAlignmentX(Component.LEFT_ALIGNMENT);
        JLabel check = new JLabel("✓");
        check.setFont(font(Font.BOLD, 17));
        check.setForeground(Color.WHITE);
        JLabel label = new JLabel(text);
        label.setFont(font(Font.PLAIN, 13));
        label.setForeground(Color.WHITE);
        row.add(check, BorderLayout.WEST);
        row.add(label, BorderLayout.CENTER);
        return row;
    }

    /** Places a form in the pale right-hand half and keeps it centered. */
    public static JPanel formSide(JComponent form) {
        return formSide(form, 24, 42);
    }

    /** Compact form-side layout for the shorter sign-up and recovery screens. */
    public static JPanel formSide(JComponent form, int verticalPadding, int horizontalPadding) {
        JPanel side = new JPanel(new GridBagLayout());
        side.setBackground(PAGE);
        GridBagConstraints c = new GridBagConstraints();
        c.gridx = 0;
        c.gridy = 0;
        c.weightx = 1;
        c.weighty = 1;
        c.fill = GridBagConstraints.NONE;
        c.anchor = GridBagConstraints.CENTER;
        c.insets = new Insets(verticalPadding, horizontalPadding,
                              verticalPadding, horizontalPadding);
        form.setOpaque(false);
        side.add(form, c);
        return side;
    }

    public static void showRecoveryCode(Component parent, String code) {
        JTextField codeField = new JTextField(code, 18);
        codeField.setEditable(false);
        codeField.setFont(font(Font.BOLD, 18));
        codeField.setHorizontalAlignment(JTextField.CENTER);
        codeField.selectAll();
        JPanel content = new JPanel(new BorderLayout(0, 10));
        content.add(new JLabel("Save this new recovery code somewhere safe."), BorderLayout.NORTH);
        content.add(codeField, BorderLayout.CENTER);
        JOptionPane.showMessageDialog(parent, content, "Your new recovery code", JOptionPane.INFORMATION_MESSAGE);
    }
}
