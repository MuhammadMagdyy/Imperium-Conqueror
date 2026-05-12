package views;

import engine.*;
import units.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.util.*;
import java.util.List;
import java.util.function.Consumer;
import javax.swing.*;

/**
 * Interactive visual map of the three cities.
 * Cities are shown as medallions; roads show distances.
 * Clicking a city fires the onCitySelected callback.
 * Armies are shown as coloured flags along roads or at cities.
 */
public class CityMapPanel extends JPanel {

    // Fixed pixel positions for each city on the 700×360 canvas
    private static final Map<String, Point> POS = Map.of(
        "Cairo",  new Point(145, 200),
        "Sparta", new Point(375, 280),
        "Rome",   new Point(540, 120)
    );
    private static final int R = 38; // city circle radius

    private final Game game;
    private Consumer<City> onCitySelected;
    private String hovered = null;

    public CityMapPanel(Game game, Consumer<City> onCitySelected) {
        this.game = game;
        this.onCitySelected = onCitySelected;
        setOpaque(false);
        setPreferredSize(new Dimension(700, 360));

        addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) {
                City c = cityAt(e.getPoint());
                if (c != null && onCitySelected != null) {
                    AudioManager.get().playClick();
                    onCitySelected.accept(c);
                }
            }
        });
        addMouseMotionListener(new MouseMotionAdapter() {
            @Override public void mouseMoved(MouseEvent e) {
                City c = cityAt(e.getPoint());
                String name = c != null ? c.getName() : null;
                if (!Objects.equals(name, hovered)) {
                    hovered = name;
                    repaint();
                }
            }
        });
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        // Parchment background
        g2.setColor(new Color(18, 11, 4, 210));
        g2.fillRoundRect(0, 0, getWidth(), getHeight(), 12, 12);
        g2.setColor(new Color(160, 118, 38, 80));
        g2.setStroke(new BasicStroke(1.5f));
        g2.drawRoundRect(1, 1, getWidth()-3, getHeight()-3, 12, 12);

        // Title
        g2.setFont(FontManager.FONT_BODY_BOLD.deriveFont(Font.ITALIC, 13f));
        g2.setColor(new Color(160, 130, 55, 130));
        g2.drawString("ANCIENT WORLD — 60 BC", 14, 22);

        drawRoads(g2);
        drawArmies(g2);
        drawCities(g2);
        drawLegend(g2);
    }

    // ── ROADS ────────────────────────────────────────────────────
    private void drawRoads(Graphics2D g2) {
        List<Distance> dists = game.getDistances();
        for (Distance d : dists) {
            Point a = POS.get(d.getFrom());
            Point b = POS.get(d.getTo());
            if (a == null || b == null) continue;

            // Dashed road line
            g2.setColor(new Color(140, 100, 35, 110));
            g2.setStroke(new BasicStroke(2.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND,
                1, new float[]{8, 5}, 0));
            g2.drawLine(a.x, a.y, b.x, b.y);

            // Distance badge in the middle of the road
            int mx = (a.x + b.x) / 2;
            int my = (a.y + b.y) / 2;
            String label = d.getDistance() + " turns";
            g2.setFont(FontManager.FONT_SMALL.deriveFont(Font.BOLD, 11f));
            FontMetrics fm = g2.getFontMetrics();
            int lw = fm.stringWidth(label);
            int lh = fm.getHeight();
            g2.setColor(new Color(12, 7, 2, 200));
            g2.fillRoundRect(mx - lw/2 - 5, my - lh/2 - 1, lw + 10, lh + 2, 6, 6);
            g2.setColor(new Color(160, 120, 40, 120));
            g2.setStroke(new BasicStroke(1f));
            g2.drawRoundRect(mx - lw/2 - 5, my - lh/2 - 1, lw + 10, lh + 2, 6, 6);
            g2.setColor(new Color(200, 170, 85));
            g2.drawString(label, mx - lw/2, my + fm.getAscent()/2);
        }
    }

    // ── ARMIES ───────────────────────────────────────────────────
    private void drawArmies(Graphics2D g2) {
        List<Army> armies = game.getPlayer().getControlledArmies();
        // Group by location to avoid overlap
        Map<String, List<Army>> byLoc = new LinkedHashMap<>();
        for (Army a : armies) {
            String loc = a.getCurrentStatus() == Status.MARCHING ? "→" + a.getTarget() : a.getCurrentLocation();
            byLoc.computeIfAbsent(loc, k -> new ArrayList<>()).add(a);
        }

        for (Map.Entry<String, List<Army>> entry : byLoc.entrySet()) {
            String loc = entry.getKey();
            List<Army> group = entry.getValue();
            Point base = null;

            if (loc.startsWith("→")) {
                // Marching — show along road toward target
                String target = loc.substring(1);
                Point tp = POS.get(target);
                if (tp == null) continue;
                // Find a nearby origin city to position the army on the road
                // Since we don't store origin in Army, place at 40% from target
                // Check all cities and find which one is connected by road
                Point best = null; double bestDist = Double.MAX_VALUE;
                for (Map.Entry<String, Point> pe : POS.entrySet()) {
                    if (pe.getKey().equals(target)) continue;
                    // Check if road exists between pe.getKey() and target
                    for (Distance d : game.getDistances()) {
                        if ((d.getFrom().equals(pe.getKey()) && d.getTo().equals(target)) ||
                            (d.getTo().equals(pe.getKey()) && d.getFrom().equals(target))) {
                            double dist = pe.getValue().distance(tp);
                            if (dist < bestDist) { bestDist = dist; best = pe.getValue(); }
                        }
                    }
                }
                if (best != null) {
                    // Position army 35% of the way from origin toward target
                    int ax = (int)(best.x + (tp.x - best.x) * 0.35);
                    int ay = (int)(best.y + (tp.y - best.y) * 0.35);
                    base = new Point(ax, ay);
                }
            } else {
                base = POS.get(loc);
                if (base != null) base = new Point(base.x + R + 5, base.y - R - 5);
            }

            if (base == null) continue;

            for (int i = 0; i < group.size(); i++) {
                Army army = group.get(i);
                int ax = base.x + i * 20;
                int ay = base.y;
                drawArmyFlag(g2, ax, ay, army);
            }
        }
    }

    private void drawArmyFlag(Graphics2D g2, int x, int y, Army army) {
        String statusEmoji = switch (army.getCurrentStatus()) {
            case IDLE      -> "⚓";
            case MARCHING  -> "🏃";
            case BESIEGING -> "⚙";
        };
        // Flag pole
        g2.setColor(new Color(200, 165, 45));
        g2.setStroke(new BasicStroke(2f));
        g2.drawLine(x, y, x, y + 16);
        // Flag
        g2.setColor(new Color(200, 80, 40, 220));
        int[] fx = {x, x+12, x};
        int[] fy = {y, y+5, y+10};
        g2.fillPolygon(fx, fy, 3);
        g2.setFont(new Font("Serif", Font.PLAIN, 11));
        g2.setColor(new Color(240, 220, 140));
        g2.drawString(statusEmoji + army.getUnits().size(), x - 4, y + 28);
    }

    // ── CITIES ───────────────────────────────────────────────────
    private void drawCities(Graphics2D g2) {
        Player p = game.getPlayer();
        g2.setStroke(new BasicStroke(1f));

        for (City city : game.getAvailableCities()) {
            Point pos = POS.get(city.getName());
            if (pos == null) continue;

            boolean owned  = p.getControlledCities().contains(city);
            boolean siege  = city.isUnderSiege();
            boolean hover  = city.getName().equals(hovered);

            // Glow / pulse for hovered or siege
            if (hover || siege) {
                Color glow = siege ? new Color(255, 140, 0, 55) :
                             owned ? new Color(80,  200, 80,  40) :
                                     new Color(200, 80,  80,  40);
                g2.setColor(glow);
                g2.fillOval(pos.x - R - 10, pos.y - R - 10, (R+10)*2, (R+10)*2);
            }

            // Shadow
            g2.setColor(new Color(0, 0, 0, 90));
            g2.fillOval(pos.x - R + 4, pos.y - R + 4, R*2, R*2);

            // Main circle — gradient fill
            Color inner = owned ? new Color(35, 90, 35) : siege ? new Color(90, 55, 10) : new Color(90, 20, 20);
            Color outer = owned ? new Color(15, 50, 15) : siege ? new Color(55, 30, 5)  : new Color(55, 10, 10);
            RadialGradientPaint rgp = new RadialGradientPaint(
                new Point2D.Float(pos.x - R/3f, pos.y - R/3f),
                R * 1.2f, new float[]{0f, 1f}, new Color[]{inner, outer});
            g2.setPaint(rgp);
            g2.fillOval(pos.x - R, pos.y - R, R*2, R*2);

            // Border ring
            Color ring = owned ? new Color(80, 200, 80)
                        : siege ? new Color(255, 165, 30)
                        : new Color(200, 60, 60);
            g2.setColor(ring);
            g2.setStroke(new BasicStroke(hover ? 3.5f : 2.5f));
            g2.drawOval(pos.x - R, pos.y - R, R*2, R*2);

            // Inner decorative ring
            g2.setColor(new Color(ring.getRed(), ring.getGreen(), ring.getBlue(), 70));
            g2.setStroke(new BasicStroke(1f));
            g2.drawOval(pos.x - R + 5, pos.y - R + 5, (R-5)*2, (R-5)*2);

            // City emoji
            String emoji = owned ? "🏰" : siege ? "⚙" : "⚔";
            g2.setFont(new Font("Serif", Font.PLAIN, 24));
            FontMetrics efm = g2.getFontMetrics();
            g2.drawString(emoji, pos.x - efm.stringWidth(emoji)/2, pos.y + 8);

            // City name below circle
            g2.setFont(FontManager.FONT_BODY_BOLD.deriveFont(14f));
            FontMetrics nfm = g2.getFontMetrics();
            String name = city.getName();
            int nx = pos.x - nfm.stringWidth(name)/2;
            // Name shadow
            g2.setColor(new Color(0,0,0,160));
            g2.drawString(name, nx+1, pos.y + R + 17);
            // Name
            g2.setColor(ring);
            g2.drawString(name, nx, pos.y + R + 16);

            // Status tag
            String tag = owned ? "Yours" : siege ? "Besieged" : "Enemy";
            g2.setFont(FontManager.FONT_SMALL.deriveFont(Font.ITALIC, 11f));
            FontMetrics tfm = g2.getFontMetrics();
            g2.setColor(new Color(ring.getRed(), ring.getGreen(), ring.getBlue(), 180));
            g2.drawString(tag, pos.x - tfm.stringWidth(tag)/2, pos.y + R + 30);

            // Defender count badge
            int defenders = city.getDefendingArmy() != null ? city.getDefendingArmy().getUnits().size() : 0;
            g2.setColor(new Color(180, 140, 30));
            g2.setFont(FontManager.FONT_SMALL.deriveFont(Font.BOLD, 11f));
            String badge = "🛡" + defenders;
            g2.drawString(badge, pos.x - 10, pos.y - R - 6);

            // Hover tooltip
            if (hover) {
                String tip = city.getName() + " — click to inspect";
                g2.setFont(FontManager.FONT_SMALL.deriveFont(11f));
                FontMetrics tfm2 = g2.getFontMetrics();
                int tw = tfm2.stringWidth(tip) + 12;
                g2.setColor(new Color(12, 7, 2, 220));
                g2.fillRoundRect(pos.x - tw/2, pos.y - R - 28, tw, 18, 6, 6);
                g2.setColor(new Color(200, 165, 55, 150));
                g2.drawRoundRect(pos.x - tw/2, pos.y - R - 28, tw, 18, 6, 6);
                g2.setColor(new Color(230, 210, 140));
                g2.drawString(tip, pos.x - tw/2 + 6, pos.y - R - 15);
            }
        }
    }

    // ── LEGEND ───────────────────────────────────────────────────
    private void drawLegend(Graphics2D g2) {
        int x = getWidth() - 165, y = getHeight() - 100;
        g2.setColor(new Color(10, 6, 2, 190));
        g2.fillRoundRect(x-8, y-8, 160, 94, 8, 8);
        g2.setColor(new Color(140, 100, 30, 100));
        g2.setStroke(new BasicStroke(1f));
        g2.drawRoundRect(x-8, y-8, 160, 94, 8, 8);
        g2.setFont(FontManager.FONT_SMALL.deriveFont(Font.BOLD, 11f));
        int[] colors = {new Color(80,200,80).getRGB(), new Color(200,60,60).getRGB(),
                        new Color(255,165,30).getRGB(), new Color(200,165,45).getRGB()};
        String[] labels = {"Your City", "Enemy City", "Under Siege", "Your Army"};
        for (int i = 0; i < labels.length; i++) {
            g2.setColor(new Color(colors[i]));
            g2.fillOval(x, y + i*20, 10, 10);
            g2.setColor(new Color(215, 195, 135));
            g2.drawString(labels[i], x + 16, y + i*20 + 10);
        }
    }

    // ── HIT TEST ─────────────────────────────────────────────────
    private City cityAt(Point p) {
        for (City city : game.getAvailableCities()) {
            Point cp = POS.get(city.getName());
            if (cp != null && p.distance(cp) <= R + 6) return city;
        }
        return null;
    }
}
