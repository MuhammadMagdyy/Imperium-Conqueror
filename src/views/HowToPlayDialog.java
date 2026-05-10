package views;

import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.awt.image.BufferedImage;
import java.io.File;
import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.border.*;

/**
 * Full "How to Play" reference card.
 * Opens on game start AND whenever the user presses Escape in the main frame.
 */
public class HowToPlayDialog extends JDialog {

    private static final Color GOLD   = new Color(210, 165, 40);
    private static final Color GOLD_L = new Color(240, 205, 90);
    private static final Color TXT    = new Color(225, 205, 150);
    private static final Color TXT_DIM= new Color(165, 148, 95);
    private static final Color BG     = new Color(14, 9, 3, 255);
    private static final Color PANEL  = new Color(22, 14, 5, 245);

    private final JTabbedPane tabs;

    public HowToPlayDialog(Frame owner) {
        super(owner, "How to Play — Conqueror", true);
        setSize(820, 640);
        setLocationRelativeTo(owner);
        setResizable(false);
        setUndecorated(true);          // custom chrome

        JPanel root = buildRoot();
        setContentPane(root);

        // Close on Escape
        getRootPane().registerKeyboardAction(
            e -> { AudioManager.get().playClick(); dispose(); },
            KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0),
            JComponent.WHEN_IN_FOCUSED_WINDOW
        );

        tabs = buildTabs();
        root.add(buildHeader(), BorderLayout.NORTH);
        root.add(tabs,          BorderLayout.CENTER);
        root.add(buildFooter(), BorderLayout.SOUTH);
    }

    /** Factory — plays a click and shows the dialog */
    public static void show(Frame owner) {
        new HowToPlayDialog(owner).setVisible(true);
    }

    // ─────────────────────────────────────────────────────────────────
    //  STRUCTURE
    // ─────────────────────────────────────────────────────────────────

    private JPanel buildRoot() {
        JPanel p = new JPanel(new BorderLayout(0, 0)) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g;
                g2.setColor(BG);
                g2.fillRect(0, 0, getWidth(), getHeight());
                // gold border
                g2.setColor(GOLD);
                g2.setStroke(new BasicStroke(2.5f));
                g2.drawRect(1, 1, getWidth()-3, getHeight()-3);
                g2.setColor(new Color(140, 100, 20, 80));
                g2.setStroke(new BasicStroke(1f));
                g2.drawRect(5, 5, getWidth()-11, getHeight()-11);
            }
        };
        p.setOpaque(false);
        p.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        return p;
    }

    private JPanel buildHeader() {
        JPanel h = new JPanel(new BorderLayout());
        h.setOpaque(false);
        h.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(0, 0, 1, 0, GOLD),
            BorderFactory.createEmptyBorder(10, 16, 10, 16)
        ));

        JLabel title = new JLabel("⚔   CONQUEROR — FIELD MANUAL   ⚔", SwingConstants.CENTER);
        title.setFont(FontManager.FONT_TITLE.deriveFont(26f));
        title.setForeground(GOLD);
        h.add(title, BorderLayout.CENTER);

        JLabel sub = new JLabel("Press  ESC  at any time to return to the game", SwingConstants.CENTER);
        sub.setFont(FontManager.FONT_ITALIC);
        sub.setForeground(TXT_DIM);
        h.add(sub, BorderLayout.SOUTH);

        return h;
    }

    private JTabbedPane buildTabs() {
        JTabbedPane tp = new JTabbedPane(JTabbedPane.LEFT);
        tp.setFont(FontManager.FONT_BODY_BOLD.deriveFont(14f));
        tp.setBackground(PANEL);
        tp.setForeground(TXT);
        tp.setOpaque(false);

        tp.addTab("🎯  Objective",    scrollWrap(pageObjective()));
        tp.addTab("🏙  Cities",       scrollWrap(pageCities()));
        tp.addTab("🏛  Buildings",    scrollWrap(pageBuildings()));
        tp.addTab("⚔  Armies",       scrollWrap(pageArmies()));
        tp.addTab("⚙  Siege",        scrollWrap(pageSiege()));
        tp.addTab("💰  Economy",      scrollWrap(pageEconomy()));
        tp.addTab("📋  Turn Order",   scrollWrap(pageTurnOrder()));
        tp.addTab("🏆  Win & Lose",   scrollWrap(pageWinLose()));

        // Style tab colours
        tp.setUI(new javax.swing.plaf.basic.BasicTabbedPaneUI() {
            @Override protected void paintTabBackground(Graphics g, int tp, int tab,
                    int x, int y, int w, int h, boolean sel) {
                g.setColor(sel ? new Color(70, 50, 10) : new Color(25, 16, 5));
                g.fillRect(x, y, w, h);
            }
            @Override protected void paintTabBorder(Graphics g, int tp, int tab,
                    int x, int y, int w, int h, boolean sel) {
                g.setColor(sel ? GOLD : new Color(90, 65, 15));
                g.drawRect(x, y, w-1, h-1);
            }
        });

        return tp;
    }

    private JPanel buildFooter() {
        JPanel f = new JPanel(new FlowLayout(FlowLayout.CENTER, 20, 8));
        f.setOpaque(false);
        f.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, GOLD));

        JButton close = goldButton("  ✔  Got It — Let Me Conquer!  ");
        close.addActionListener(e -> { AudioManager.get().playClick(); dispose(); });
        f.add(close);

        return f;
    }

    // ─────────────────────────────────────────────────────────────────
    //  TAB CONTENT PAGES
    // ─────────────────────────────────────────────────────────────────

    private JPanel pageObjective() {
        JPanel p = page();
        h2(p, "🎯  Your Goal");
        para(p, "You are a commander rising to power in the ancient world. Three great city-states " +
                "compete for dominance: Cairo, Rome, and Sparta.");
        para(p, "To WIN, you must conquer ALL cities before 30 turns expire. " +
                "You control one city at the start; take the other two by building armies and marching on enemy territory.");
        spacer(p);
        h2(p, "⏰  The Clock");
        rule(p, "Turn limit",    "30 turns. The counter is shown in the top bar.");
        rule(p, "Losing early",  "If all your soldiers are wiped out with no way to recover, you cannot win.");
        rule(p, "Winning early", "Occupy all 3 cities before turn 30 and the game ends immediately.");
        return p;
    }

    private JPanel pageCities() {
        JPanel p = page();
        h2(p, "🏙  The Cities");
        para(p, "The map has exactly three cities — Cairo, Rome, and Sparta. You start owning one. " +
                "Distances between them:");
        spacer(p);
        rule(p, "Cairo  →  Rome",   "6 turns marching");
        rule(p, "Cairo  →  Sparta", "5 turns marching");
        rule(p, "Sparta →  Rome",   "9 turns marching");
        spacer(p);
        h2(p, "🏰  City Panel (click a city on the left)");
        rule(p, "Green name",  "City you control — you can build, recruit, and form armies here.");
        rule(p, "Red name",    "Enemy city — march an army there to attack it.");
        rule(p, "🛡 number",   "How many defender units are garrisoned in that city.");
        rule(p, "Under siege", "Orange label — the city is being besieged (see Siege tab).");
        return p;
    }

    private JPanel pageBuildings() {
        JPanel p = page();
        h2(p, "🏛  Building Types");
        para(p, "Select one of YOUR cities, then press \"🏗 Build\" on the left panel. " +
                "Each building costs gold and appears immediately, but starts in cool-down " +
                "(⏳) — wait one turn before using it.");
        spacer(p);
        h2(p, "Economic Buildings  (harvest each turn)");
        rule(p, "🌾 Farm",   "Cost 1 000 💰  →  produces +500 Food/turn (Lvl 1), scaling to +1 000 at Lvl 3.");
        rule(p, "💰 Market", "Cost 1 500 💰  →  produces +1 000 Gold/turn (Lvl 1), scaling to +2 000 at Lvl 3.");
        spacer(p);
        h2(p, "Military Buildings  (recruit units)");
        rule(p, "⚔ Barracks",      "Cost 2 000 💰  →  recruits Infantry. Recruit cost 500 💰/unit.");
        rule(p, "🐴 Stable",        "Cost 2 500 💰  →  recruits Cavalry.  Recruit cost 600 💰/unit.");
        rule(p, "🏹 Archery Range", "Cost 1 500 💰  →  recruits Archers.  Recruit cost 400 💰/unit.");
        spacer(p);
        h2(p, "⬆  Upgrading");
        para(p, "Select an owned city → press \"⬆ Upgrade\" → pick the building. " +
                "Max level is 3. Higher level = stronger/more productive. Buildings must NOT be in cool-down to upgrade.");
        rule(p, "Max recruits/turn", "Each military building can recruit at most 3 units per turn.");
        return p;
    }

    private JPanel pageArmies() {
        JPanel p = page();
        h2(p, "⚔  Unit Types & Combat");
        para(p, "Three unit types exist, each with three levels (1–3). Higher level units " +
                "deal more damage and have more max soldiers.");
        spacer(p);
        rule(p, "🏹 Archer   Lvl 1/2", "60 soldiers. Excellent vs Cavalry, decent vs Archers.");
        rule(p, "🏹 Archer   Lvl 3",   "70 soldiers. Strongest ranged unit.");
        rule(p, "⚔ Infantry Lvl 1/2",  "50 soldiers. Balanced fighter. Good vs Archers.");
        rule(p, "⚔ Infantry Lvl 3",    "60 soldiers. Toughest foot soldier.");
        rule(p, "🐴 Cavalry  Lvl 1/2", "40 soldiers. Fast and powerful vs Archers.");
        rule(p, "🐴 Cavalry  Lvl 3",   "60 soldiers. Most powerful single unit type.");
        spacer(p);
        h2(p, "Forming an Army");
        rule(p, "1. Recruit", "Use a military building to add units to the city garrison.");
        rule(p, "2. Initiate", "Press \"⚔ Form Army\" — pick one unit as the seed. That unit leaves the garrison and forms an independent marching army.");
        rule(p, "3. Max size", "An army holds at most 10 units.");
        spacer(p);
        h2(p, "Marching");
        rule(p, "Select army", "Click it in the right-hand army list.");
        rule(p, "March To...", "Choose destination city. The army turns MARCHING and arrives after distance turns.");
        rule(p, "Arrival",     "When distance reaches 0 the army automatically stops and turns IDLE at the target city.");
        return p;
    }

    private JPanel pageSiege() {
        JPanel p = page();
        h2(p, "⚙  Siege Warfare");
        para(p, "When your army arrives at an enemy city you have two options:");
        spacer(p);
        h2(p, "Option A — Lay Siege (slow)");
        rule(p, "Lay Siege button", "Army must be IDLE at the target. Status changes to BESIEGING.");
        rule(p, "Turn 0",  "Siege begins. Enemy garrison loses 10% of each unit's soldiers per turn.");
        rule(p, "Turn 3",  "After 3 siege turns the city automatically surrenders and you occupy it FREE — no battle needed.");
        rule(p, "Risk",    "Your army pays extra food upkeep while besieging.");
        spacer(p);
        h2(p, "Option B — Auto-Resolve Battle (fast)");
        rule(p, "Auto Resolve button", "Triggers immediate combat. Your army vs. the city garrison fight round-by-round.");
        rule(p, "Win",  "If your army survives, you occupy the city immediately.");
        rule(p, "Lose", "If your army is destroyed, the city stays enemy-controlled.");
        spacer(p);
        h2(p, "After Occupation");
        rule(p, "Garrison", "Your attacking army becomes the new defending garrison.");
        rule(p, "Buildings", "All existing buildings in the captured city are yours to keep.");
        return p;
    }

    private JPanel pageEconomy() {
        JPanel p = page();
        h2(p, "💰  Gold & Food");
        rule(p, "Starting treasury", "5 000 gold. No starting food.");
        rule(p, "Gold income",  "Earned each turn from Markets (+1 000 → +2 000 per market per turn).");
        rule(p, "Food income",  "Earned each turn from Farms (+500 → +1 000 per farm per turn).");
        spacer(p);
        h2(p, "🌾  Upkeep — the #1 cause of defeat");
        para(p, "Every soldier in every unit in every army (and garrison) eats food each turn. " +
                "If you can't cover upkeep:");
        rule(p, "Outcome", "Food drops to 0 and ALL your units lose 10% of their current soldiers immediately.");
        rule(p, "Spiral",  "Weak armies lose sieges → fewer cities → less farm income → worse starvation.");
        spacer(p);
        h2(p, "Upkeep rates (food per soldier per turn)");
        rule(p, "Idle (garrison)",   "Archer 0.4 · Infantry 0.5 · Cavalry 0.6");
        rule(p, "Marching",          "Archer 0.5 · Infantry 0.6 · Cavalry 0.7");
        rule(p, "Besieging",         "Archer 0.6 · Infantry 0.7 · Cavalry 0.75 (level 3 higher)");
        spacer(p);
        h2(p, "💡  Key Tips");
        rule(p, "Build a Farm first",  "Food security before military expansion.");
        rule(p, "Then a Market",       "Gold funds all future buildings and recruits.");
        rule(p, "Don't over-recruit",  "More units = more upkeep. Quality beats quantity.");
        return p;
    }

    private JPanel pageTurnOrder() {
        JPanel p = page();
        h2(p, "📋  What Happens Each Turn (End Turn button)");
        para(p, "When you press END TURN the engine processes in this exact order:");
        spacer(p);
        numbered(p, "1", "All military building cool-downs reset to OFF. Recruit counters clear to 0.");
        numbered(p, "2", "All economic buildings (Markets, Farms) harvest and add to your treasury / food.");
        numbered(p, "3", "Marching armies advance 1 turn toward their target.");
        numbered(p, "4", "Armies that reach their target become IDLE at that city.");
        numbered(p, "5", "Total food upkeep is calculated across ALL units in ALL armies and garrisons.");
        numbered(p, "6", "If you have enough food, it is deducted. If not, food → 0 and units starve 10%.");
        numbered(p, "7", "Besieged cities: defenders lose 10% soldiers. After 3 siege turns → city falls.");
        spacer(p);
        h2(p, "🖱  During your turn (before End Turn) you may:");
        rule(p, "Build",    "Construct a new building in any city you control.");
        rule(p, "Upgrade",  "Upgrade any existing building (if not in cool-down, enough gold).");
        rule(p, "Recruit",  "Add a unit to a city garrison (max 3 per building per turn).");
        rule(p, "Form Army","Pull a unit out of a garrison to create a marching army.");
        rule(p, "March",    "Set a destination for an army.");
        rule(p, "Siege",    "Lay siege to or auto-resolve a battle at an enemy city.");
        return p;
    }

    private JPanel pageWinLose() {
        JPanel p = page();
        h2(p, "🏆  Victory Condition");
        para(p, "Occupy all THREE cities (Cairo, Rome, Sparta). " +
                "The game detects this the moment you occupy the last city and shows the victory screen.");
        spacer(p);
        h2(p, "💀  Defeat Conditions");
        rule(p, "Turn 30 expires",  "If any city remains under enemy control when turn 30 ends, you lose.");
        rule(p, "No recovery path", "If all your armies are destroyed and you have no gold left to recruit, you effectively cannot win.");
        spacer(p);
        h2(p, "💡  Winning Strategy");
        rule(p, "Turn 1–3",   "Build Farm + Market. Recruit 2–3 Infantry. Form an army.");
        rule(p, "Turn 3–6",   "March on the nearest city (Sparta from Cairo is 5 turns). " +
                               "While marching, keep recruiting.");
        rule(p, "Siege first","Lay siege rather than auto-resolving when the enemy garrison is large. " +
                               "3 siege turns costs you only food; auto-resolve risks your army.");
        rule(p, "Turn 10+",   "With 2 cities producing income, snowball into the last city.");
        rule(p, "Always",     "Watch the food bar. Never let it drop to zero mid-campaign.");
        return p;
    }

    // ─────────────────────────────────────────────────────────────────
    //  COMPONENT HELPERS
    // ─────────────────────────────────────────────────────────────────

    private JPanel page() {
        JPanel p = new JPanel();
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        p.setBackground(PANEL);
        p.setBorder(BorderFactory.createEmptyBorder(18, 22, 18, 22));
        return p;
    }

    private JScrollPane scrollWrap(JPanel content) {
        JScrollPane sp = new JScrollPane(content);
        sp.setOpaque(false);
        sp.getViewport().setOpaque(false);
        sp.setBorder(BorderFactory.createLineBorder(new Color(80, 60, 15, 100), 1));
        sp.getVerticalScrollBar().setUnitIncrement(14);
        return sp;
    }

    private void h2(JPanel p, String text) {
        JLabel l = new JLabel(text);
        l.setFont(FontManager.FONT_HEADING.deriveFont(17f));
        l.setForeground(GOLD);
        l.setAlignmentX(Component.LEFT_ALIGNMENT);
        l.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(GOLD.getRed(), GOLD.getGreen(), GOLD.getBlue(), 60)),
            BorderFactory.createEmptyBorder(6, 0, 4, 0)
        ));
        p.add(l);
    }

    private void para(JPanel p, String text) {
        JTextArea ta = new JTextArea(text);
        ta.setEditable(false);
        ta.setLineWrap(true);
        ta.setWrapStyleWord(true);
        ta.setOpaque(false);
        ta.setFont(FontManager.FONT_BODY);
        ta.setForeground(TXT);
        ta.setAlignmentX(Component.LEFT_ALIGNMENT);
        ta.setBorder(BorderFactory.createEmptyBorder(4, 0, 6, 0));
        ta.setMaximumSize(new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE));
        p.add(ta);
    }

    private void rule(JPanel p, String key, String value) {
        JPanel row = new JPanel(new BorderLayout(12, 0));
        row.setOpaque(false);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 32));
        row.setBorder(BorderFactory.createEmptyBorder(3, 8, 3, 0));
        row.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel k = new JLabel(key);
        k.setFont(FontManager.FONT_BODY_BOLD);
        k.setForeground(GOLD_L);
        k.setPreferredSize(new Dimension(170, 24));

        JLabel v = new JLabel(value);
        v.setFont(FontManager.FONT_BODY);
        v.setForeground(TXT);

        row.add(k, BorderLayout.WEST);
        row.add(v, BorderLayout.CENTER);
        p.add(row);
    }

    private void numbered(JPanel p, String num, String text) {
        JPanel row = new JPanel(new BorderLayout(10, 0));
        row.setOpaque(false);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 36));
        row.setBorder(BorderFactory.createEmptyBorder(4, 8, 4, 0));
        row.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel n = new JLabel(num + ".");
        n.setFont(FontManager.FONT_BODY_BOLD.deriveFont(Font.BOLD, 16f));
        n.setForeground(GOLD);
        n.setPreferredSize(new Dimension(28, 24));

        JLabel v = new JLabel(text);
        v.setFont(FontManager.FONT_BODY);
        v.setForeground(TXT);

        row.add(n, BorderLayout.WEST);
        row.add(v, BorderLayout.CENTER);
        p.add(row);
    }

    private void spacer(JPanel p) {
        p.add(Box.createVerticalStrut(10));
    }

    private JButton goldButton(String text) {
        JButton b = new JButton(text) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g;
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                Color top = getModel().isPressed()  ? new Color(80,  55,  8)  :
                            getModel().isRollover() ? new Color(180, 135, 22) : new Color(140, 100, 15);
                Color bot = getModel().isPressed()  ? new Color(50,  35,  4)  :
                            getModel().isRollover() ? new Color(120, 90,  12) : new Color(90,  65,  10);
                g2.setPaint(new GradientPaint(0,0,top,0,getHeight(),bot));
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 10, 10);
                g2.setColor(new Color(220, 170, 50, 160));
                g2.setStroke(new BasicStroke(1.5f));
                g2.drawRoundRect(1, 1, getWidth()-2, getHeight()-2, 10, 10);
                super.paintComponent(g);
            }
        };
        b.setContentAreaFilled(false);
        b.setBorderPainted(false);
        b.setFocusPainted(false);
        b.setFont(FontManager.FONT_BODY_BOLD.deriveFont(15f));
        b.setForeground(new Color(255, 240, 180));
        b.setPreferredSize(new Dimension(320, 42));
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return b;
    }
}
