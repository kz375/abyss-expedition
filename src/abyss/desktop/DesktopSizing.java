package abyss.desktop;

import java.awt.*;

public final class DesktopSizing {
    private DesktopSizing() { }
    public static Dimension fit(Dimension desired, Rectangle usable) {
        return new Dimension(Math.max(1, Math.min(desired.width, usable.width - 24)),
                Math.max(1, Math.min(desired.height, usable.height - 24)));
    }
    public static void fit(Window window, Dimension desired) {
        GraphicsConfiguration config = window.getGraphicsConfiguration();
        Rectangle bounds = new Rectangle(config.getBounds());
        Insets insets = Toolkit.getDefaultToolkit().getScreenInsets(config);
        bounds.x += insets.left; bounds.y += insets.top;
        bounds.width -= insets.left + insets.right; bounds.height -= insets.top + insets.bottom;
        Dimension size = fit(desired, bounds);
        window.setMinimumSize(new Dimension(Math.min(480, size.width), Math.min(360, size.height)));
        window.setSize(size);
        window.setLocation(bounds.x + (bounds.width-size.width)/2, bounds.y + (bounds.height-size.height)/2);
    }
}
