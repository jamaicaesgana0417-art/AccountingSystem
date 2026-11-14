import java.awt.*;
import javax.swing.Icon;

/** Small vector icons for the four accounting work areas. */
public final class AccountingIcons {
    private AccountingIcons() {}

    public static Icon journal() { return new GlyphIcon(0); }
    public static Icon transactions() { return new GlyphIcon(1); }
    public static Icon balanceSheet() { return new GlyphIcon(2); }
    public static Icon ledger() { return new GlyphIcon(3); }

    private static final class GlyphIcon implements Icon {
        private final int type;
        GlyphIcon(int type) { this.type = type; }
        public int getIconWidth() { return 30; }
        public int getIconHeight() { return 30; }

        public void paintIcon(Component component, Graphics graphics, int x, int y) {
            Graphics2D g = (Graphics2D) graphics.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setColor(new Color(232, 238, 255));
            g.fillRoundRect(x, y, 30, 30, 9, 9);
            g.setColor(new Color(55, 91, 255));
            g.setStroke(new BasicStroke(2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            switch (type) {
                case 0: // journal / notebook
                    g.drawRoundRect(x + 8, y + 5, 15, 20, 2, 2);
                    g.drawLine(x + 11, y + 10, x + 20, y + 10);
                    g.drawLine(x + 11, y + 14, x + 20, y + 14);
                    g.drawLine(x + 11, y + 18, x + 18, y + 18);
                    g.drawLine(x + 5, y + 8, x + 5, y + 22);
                    break;
                case 1: // transactions / two-way arrows
                    g.drawLine(x + 6, y + 10, x + 22, y + 10);
                    g.drawLine(x + 18, y + 6, x + 22, y + 10);
                    g.drawLine(x + 18, y + 14, x + 22, y + 10);
                    g.drawLine(x + 24, y + 20, x + 8, y + 20);
                    g.drawLine(x + 12, y + 16, x + 8, y + 20);
                    g.drawLine(x + 12, y + 24, x + 8, y + 20);
                    break;
                case 2: // balance sheet / balanced columns
                    g.drawLine(x + 15, y + 5, x + 15, y + 24);
                    g.drawLine(x + 6, y + 10, x + 24, y + 10);
                    g.drawLine(x + 8, y + 10, x + 5, y + 19);
                    g.drawLine(x + 5, y + 19, x + 11, y + 19);
                    g.drawLine(x + 22, y + 10, x + 19, y + 19);
                    g.drawLine(x + 19, y + 19, x + 25, y + 19);
                    g.drawLine(x + 10, y + 24, x + 20, y + 24);
                    break;
                default: // ledger / ruled book
                    g.drawRoundRect(x + 6, y + 5, 18, 20, 2, 2);
                    g.drawLine(x + 11, y + 5, x + 11, y + 25);
                    g.drawLine(x + 14, y + 10, x + 21, y + 10);
                    g.drawLine(x + 14, y + 15, x + 21, y + 15);
                    g.drawLine(x + 14, y + 20, x + 21, y + 20);
            }
            g.dispose();
        }
    }
}
