package abyss.puzzle;

import java.awt.*;
import java.awt.geom.*;
import java.util.Set;

/** Code-drawn artwork; rendering never consumes the expedition's random stream. */
final class PuzzleArtwork {
    static final Color GOLD = new Color(235, 191, 102), CYAN = new Color(101, 221, 231);
    static final Color TEXT = new Color(226, 232, 239), MUTED = new Color(146, 161, 181);

    static void paint(Graphics2D source, int width, int height, Point player, Point monster,
                      Set<Point> boxes, Set<Point> targets, Point burst, int moves, int cores) {
        Graphics2D g = (Graphics2D) source.create();
        double scale = Math.min(width / 1000.0, height / 780.0);
        g.translate((width - 1000 * scale) / 2, (height - 780 * scale) / 2);
        g.scale(scale, scale);
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setPaint(new GradientPaint(0, 0, new Color(22, 30, 46), 1000, 780, new Color(8, 12, 21)));
        g.fillRect(0, 0, 1000, 780);
        text(g, "A B Y S S   /   M E C H A N I S M   T R I A L", 30, 32, 12, GOLD);
        text(g, "Trial of the Twin Cores", 30, 72, 30, TEXT);
        text(g, "Outmaneuver the guardian. Shatter both cores.", 32, 100, 14, MUTED);
        panel(g, 646, 24, 154, 84);
        panel(g, 814, 24, 156, 84);
        text(g, "MOVES LEFT", 664, 48, 12, MUTED);
        text(g, String.format("%02d", Math.max(0, PuzzleBoard.MOVE_LIMIT - moves)), 663, 84, 30,
                moves >= PuzzleBoard.MOVE_LIMIT - 10 ? new Color(255, 128, 104) : CYAN);
        text(g, "/ " + PuzzleBoard.MOVE_LIMIT, 717, 83, 14, MUTED);
        text(g, "CORES SHATTERED", 832, 48, 12, MUTED);
        text(g, (2 - cores) + " / 2", 832, 84, 30, GOLD);

        panel(g, 26, 125, 608, 608);
        for (int y = 0; y < 8; y++) for (int x = 0; x < 8; x++) {
            int px = 42 + x * 72, py = 141 + y * 72;
            boolean edge = x == 0 || y == 0 || x == 7 || y == 7;
            g.setColor(edge ? new Color(29, 35, 49) : new Color(39 + (x+y)%2*3, 49, 64));
            g.fillRoundRect(px, py, 68, 68, 8, 8);
            g.setColor(new Color(63, 75, 92));
            g.drawLine(px + 9, py + 1, px + 59, py + 1);
            if (edge) {
                g.setColor(new Color(80, 67, 55));
                g.drawLine(px + 6, py + 61, px + 16, py + 51);
            }
            Point p = new Point(x, y);
            if (targets.contains(p)) core(g, px + 34, py + 34);
            if (boxes.contains(p)) box(g, px + 34, py + 34);
            if (player.equals(p)) player(g, px + 34, py + 34);
            if (monster.equals(p)) monster(g, px + 34, py + 34);
        }
        if (burst != null) {
            int cx = 76 + burst.x * 72, cy = 175 + burst.y * 72;
            g.setColor(new Color(255, 195, 93, 100)); g.fillOval(cx-37, cy-37, 74, 74);
            g.setStroke(new BasicStroke(3)); g.setColor(GOLD);
            g.drawOval(cx-30, cy-30, 60, 60);
            for (int i = 0; i < 12; i++) {
                double a = i * Math.PI / 6;
                g.drawLine(cx+(int)(22*Math.cos(a)), cy+(int)(22*Math.sin(a)),
                        cx+(int)(42*Math.cos(a)), cy+(int)(42*Math.sin(a)));
            }
            text(g, "SHATTERED", cx-36, cy-43, 17, GOLD);
        }

        panel(g, 650, 125, 320, 608);
        text(g, "TRIAL GUIDE", 672, 159, 20, TEXT);
        text(g, "01  Push crates into the guardian.", 672, 190, 15, TEXT);
        text(g, "02  Push the guardian onto cores.", 672, 217, 15, TEXT);
        text(g, "03  Shatter both cores to win.", 672, 244, 15, TEXT);
        g.setColor(new Color(53, 65, 83)); g.drawLine(672, 268, 948, 268);
        player(g, 700, 311); text(g, "YOU / Adventurer", 745, 316, 15, CYAN);
        box(g, 700, 374); text(g, "CRATE / Push to move", 745, 379, 15, TEXT);
        monster(g, 700, 437); text(g, "GUARDIAN / Use crates", 745, 442, 15, TEXT);
        core(g, 700, 500); text(g, "CORE / Breaks on impact", 745, 505, 14, GOLD);
        text(g, "W A S D  /  Arrow keys", 672, 558, 19, TEXT);
        text(g, "Valid moves count. ESC to abandon.", 672, 586, 13, MUTED);
        text(g, "EDGE RULES", 672, 630, 16, GOLD);
        text(g, "Crates return to a free central tile.", 672, 657, 14, MUTED);
        text(g, "Guardian respawns in the central 5×5.", 672, 683, 14, MUTED);
        text(g, burst == null ? "Learn the mechanisms. Make every move count." : "CORE SHATTERED! The golden mechanism is gone.", 32, 760, 15, burst == null ? MUTED : GOLD);
        g.dispose();
    }

    private static void panel(Graphics2D g, int x, int y, int w, int h) {
        g.setColor(new Color(17, 24, 36)); g.fillRoundRect(x, y, w, h, 18, 18);
        g.setColor(new Color(60, 73, 92)); g.setStroke(new BasicStroke(1)); g.drawRoundRect(x, y, w, h, 18, 18);
    }
    private static void text(Graphics2D g, String value, int x, int y, int size, Color color) {
        g.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, size)); g.setColor(color); g.drawString(value, x, y);
    }
    private static void core(Graphics2D g, int x, int y) {
        g.setColor(new Color(235, 191, 102, 35)); g.fillOval(x-29,y-29,58,58);
        g.setColor(GOLD); g.setStroke(new BasicStroke(2)); g.drawOval(x-25,y-25,50,50);
        g.drawPolygon(new int[]{x,x+15,x,x-15},new int[]{y-21,y,y+21,y},4);
        g.setColor(new Color(255,231,174)); g.fillPolygon(new int[]{x,x+9,x,x-9},new int[]{y-15,y,y+15,y},4);
        g.setColor(new Color(116,80,45)); g.drawLine(x,y-12,x-4,y); g.drawLine(x-4,y,x+4,y+6);
    }
    private static void box(Graphics2D g, int x, int y) {
        g.setColor(new Color(9,13,20,110)); g.fillRoundRect(x-23,y-18,49,47,8,8);
        g.setPaint(new GradientPaint(x-22,y-22,new Color(184,134,77),x+22,y+22,new Color(107,70,42)));
        g.fillRoundRect(x-23,y-23,46,46,6,6);
        g.setColor(new Color(229,183,115)); g.setStroke(new BasicStroke(3)); g.drawRect(x-19,y-19,38,38);
        g.drawLine(x-17,y-17,x+17,y+17); g.drawLine(x+17,y-17,x-17,y+17);
        g.setColor(new Color(65,62,58)); for(int a : new int[]{-17,17}) for(int b : new int[]{-17,17}) g.fillOval(x+a-2,y+b-2,4,4);
    }
    private static void player(Graphics2D g, int x, int y) {
        g.setColor(new Color(101,221,231,35)); g.fillOval(x-28,y-28,56,56);
        g.setColor(new Color(37,106,128)); g.fillPolygon(new int[]{x,x+22,x-22},new int[]{y-17,y+24,y+24},3);
        g.setColor(CYAN); g.setStroke(new BasicStroke(2)); g.drawOval(x-14,y-24,28,29);
        g.setColor(new Color(190,234,233)); g.fillOval(x-10,y-20,20,21);
        g.setColor(new Color(27,52,72)); g.fillRoundRect(x-11,y-13,22,7,4,4);
        g.setColor(CYAN); g.drawLine(x-14,y+19,x+14,y+19);
    }
    private static void monster(Graphics2D g, int x, int y) {
        g.setColor(new Color(223,80,114,35)); g.fillOval(x-29,y-29,58,58);
        g.setColor(new Color(141,50,77)); g.fillRoundRect(x-22,y-17,44,40,10,10);
        g.setColor(new Color(229,116,133));
        g.fillPolygon(new int[]{x-21,x-28,x-8},new int[]{y-9,y-29,y-17},3);
        g.fillPolygon(new int[]{x+21,x+28,x+8},new int[]{y-9,y-29,y-17},3);
        g.setColor(new Color(255,222,161)); g.fillRect(x-14,y-6,9,5); g.fillRect(x+5,y-6,9,5);
        g.setColor(new Color(60,26,45)); g.fillRect(x-10,y+10,20,5);
    }
}
