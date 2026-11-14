import java.awt.*;
import java.awt.geom.Arc2D;
import java.awt.geom.Line2D;
import javax.swing.*;
import javax.swing.plaf.basic.BasicButtonUI;

/**
 * Refresh button for the accounting screens. Idle, it shows a refresh arrow.
 * While the screen reloads its data it switches to a spinning icon, the
 * "Refreshing..." label, and a disabled state so the user can see the screen
 * is busy.
 */
final class RefreshButton extends JButton {
    private static final String IDLE_TEXT = "Refresh";
    private static final String BUSY_TEXT = "Refreshing...";
    private static final String IDLE_TIP = "Reload this screen's data";
    private static final String BUSY_TIP = "Refreshing this screen...";

    private static final Color BLUE = new Color(37, 99, 235);
    private static final Color TINT = new Color(238, 243, 255);
    private static final Color TINT_HOVER = new Color(226, 235, 255);
    private static final Color BORDER = new Color(222, 229, 241);
    // Shown while the screen is reloading.
    private static final Color GREEN = new Color(22, 163, 74);
    private static final Color GREEN_TINT = new Color(220, 252, 231);
    private static final Color GREEN_BORDER = new Color(134, 239, 172);

    private static final int ICON_SIZE = 16;
    private static final int GAP = 8;
    private static final int PAD_X = 16;
    private static final int HEIGHT = 36;

    private final ArrowIcon icon = new ArrowIcon();
    private final Timer spinner = new Timer(40, event -> {
        icon.angle = (icon.angle + 30) % 360;
        repaint();
    });
    private boolean refreshing = false;

    RefreshButton() {
        super(IDLE_TEXT);
        setIcon(icon);
        setIconTextGap(GAP);
        setFont(new Font("Inter", Font.BOLD, 12));
        setFocusPainted(false);
        setContentAreaFilled(false);
        setBorderPainted(false);
        setOpaque(false);
        setRolloverEnabled(true);
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        setToolTipText(IDLE_TIP);
        setUI(new RefreshUI());
    }

    /** Puts the button into or out of the refreshing state. */
    void setRefreshing(boolean busy) {
        if (busy == refreshing) {
            return;
        }

        refreshing = busy;
        setEnabled(!busy);
        setText(busy ? BUSY_TEXT : IDLE_TEXT);
        setToolTipText(busy ? BUSY_TIP : IDLE_TIP);
        setCursor(Cursor.getPredefinedCursor(
            busy ? Cursor.WAIT_CURSOR : Cursor.HAND_CURSOR));
        icon.spinning = busy;
        icon.angle = 0;

        if (busy) {
            spinner.start();
        } else {
            spinner.stop();
        }
        repaint();
    }

    @Override
    public Dimension getPreferredSize() {
        // Wide enough for both labels so the button does not resize when it
        // switches between them.
        FontMetrics metrics = getFontMetrics(getFont());
        int text = Math.max(
            metrics.stringWidth(IDLE_TEXT), metrics.stringWidth(BUSY_TEXT));
        return new Dimension(PAD_X * 2 + ICON_SIZE + GAP + text, HEIGHT);
    }

    /** Title on the left and the given actions grouped on the right. */
    static JPanel headerRow(Component title, Component... actions) {
        JPanel actionPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        actionPanel.setOpaque(false);
        for (Component action : actions) {
            actionPanel.add(action);
        }

        JPanel row = new JPanel(new BorderLayout(12, 0));
        row.setOpaque(false);
        row.add(title, BorderLayout.WEST);
        row.add(actionPanel, BorderLayout.EAST);
        return row;
    }

    private final class RefreshUI extends BasicButtonUI {
        @Override
        public void paint(Graphics graphics, JComponent component) {
            AbstractButton button = (AbstractButton) component;
            Graphics2D g = (Graphics2D) graphics.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

            int w = button.getWidth() - 1;
            int h = button.getHeight() - 1;
            boolean hover = button.isEnabled() && button.getModel().isRollover();
            g.setColor(refreshing ? GREEN_TINT : hover ? TINT_HOVER : TINT);
            g.fillRoundRect(0, 0, w, h, 14, 14);
            g.setColor(refreshing ? GREEN_BORDER : BORDER);
            g.drawRoundRect(0, 0, w, h, 14, 14);

            Font font = button.getFont();
            FontMetrics metrics = g.getFontMetrics(font);
            String text = button.getText();
            int contentWidth = ICON_SIZE + GAP + metrics.stringWidth(text);
            int x = (button.getWidth() - contentWidth) / 2;

            icon.paintIcon(button, g, x, (button.getHeight() - ICON_SIZE) / 2);

            g.setFont(font);
            g.setColor(refreshing ? GREEN : BLUE);
            g.drawString(text, x + ICON_SIZE + GAP,
                (button.getHeight() - metrics.getHeight()) / 2 + metrics.getAscent());
            g.dispose();
        }
    }

    /** Refresh arrow when idle, three-quarter ring while spinning. */
    private static final class ArrowIcon implements Icon {
        private boolean spinning;
        private int angle;

        @Override
        public int getIconWidth() {
            return ICON_SIZE;
        }

        @Override
        public int getIconHeight() {
            return ICON_SIZE;
        }

        @Override
        public void paintIcon(Component component, Graphics graphics, int x, int y) {
            Graphics2D g = (Graphics2D) graphics.create();
            g.translate(x, y);
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_ON);
            g.setColor(spinning ? GREEN : BLUE);
            g.setStroke(new BasicStroke(1.8f,
                BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));

            double center = ICON_SIZE / 2.0;
            double radius = 5.5;
            double box = 2 * radius;

            if (spinning) {
                g.draw(new Arc2D.Double(center - radius, center - radius,
                    box, box, angle, -270, Arc2D.OPEN));
            } else {
                // Clockwise arc that ends at 60 degrees, with an arrowhead there.
                g.draw(new Arc2D.Double(center - radius, center - radius,
                    box, box, 0, -300, Arc2D.OPEN));

                double end = Math.toRadians(60);
                double tipX = center + radius * Math.cos(end);
                double tipY = center - radius * Math.sin(end);

                // Direction of travel at the tip (screen y points down).
                double dirX = Math.sin(end);
                double dirY = Math.cos(end);
                double backX = -dirX;
                double backY = -dirY;

                double headLength = 4.0;
                double[] left = rotate(backX, backY, 40);
                double[] right = rotate(backX, backY, -40);
                g.draw(new Line2D.Double(tipX + left[0] * headLength,
                    tipY + left[1] * headLength, tipX, tipY));
                g.draw(new Line2D.Double(tipX + right[0] * headLength,
                    tipY + right[1] * headLength, tipX, tipY));
            }
            g.dispose();
        }

        private static double[] rotate(double x, double y, double degrees) {
            double radians = Math.toRadians(degrees);
            double cos = Math.cos(radians);
            double sin = Math.sin(radians);
            return new double[] {x * cos - y * sin, x * sin + y * cos};
        }
    }
}
