package views;

import engine.City;
import engine.Game;
import javax.swing.*;
import java.awt.*;
import java.util.HashMap;

public class StrategicMapDialog extends JDialog {
    private final Game game;
    private final Image mapImage;
    // Define coordinates for your cities on the map (Adjust these to match your PNG!)
    private static final HashMap<String, Point> CITY_COORDS = new HashMap<>() {{
        put("Cairo", new Point(600, 500));
        put("Rome",  new Point(400, 200));
        put("Sparta", new Point(550, 300));
    }};

    public StrategicMapDialog(Frame parent, Game game, Image bg) {
        super(parent, "Empire Strategic Map", true);
        this.game = game;
        this.mapImage = bg;

        setSize(800, 600);
        setLocationRelativeTo(parent);

        JPanel mapPanel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g;
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                // Draw Background
                g2.drawImage(mapImage, 0, 0, getWidth(), getHeight(), null);

                // Draw Cities
                for (City city : game.getAvailableCities()) {
                    Point p = CITY_COORDS.getOrDefault(city.getName(), new Point(100, 100));
                    // Scale coordinates to current window size
                    int x = (int)(p.x * (getWidth() / 1280.0));
                    int y = (int)(p.y * (getHeight() / 780.0));

                    boolean owned = game.getPlayer().getControlledCities().contains(city);

                    // Pulse effect for Siege
                    if (city.isUnderSiege() && (System.currentTimeMillis() / 500) % 2 == 0) {
                        g2.setColor(new Color(255, 255, 255, 150));
                        g2.drawOval(x-15, y-15, 30, 30);
                    }

                    // Draw City Glow
                    g2.setColor(owned ? new Color(0, 255, 100, 180) : new Color(255, 50, 50, 180));
                    g2.fillOval(x-10, y-10, 20, 20);

                    // Label
                    g2.setColor(Color.WHITE);
                    g2.setFont(new Font("Serif", Font.BOLD, 14));
                    g2.drawString(city.getName().toUpperCase(), x - 20, y - 15);
                }
            }
        };

        // Refresh timer for the "pulse" animation
        Timer t = new Timer(500, e -> mapPanel.repaint());
        t.start();

        add(mapPanel);

        // Close on ESC
        mapPanel.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent e) { dispose(); }
        });
    }
}