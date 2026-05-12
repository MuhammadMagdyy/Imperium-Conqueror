package views;

import engine.Game;
import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.awt.image.BufferedImage;
import java.io.File;
import javax.imageio.ImageIO;
import java.io.IOException;

public class MainMenuFrame extends JFrame {

    private JTextField nameField;
    private JComboBox<String> cityCombo;
    private BufferedImage bgImage;
    private BufferedImage logoImg;

    public MainMenuFrame() {
        loadAssets();
        setupFrame();
        buildUI();
        setVisible(true);
    }

    private void loadAssets() {
        try {
            bgImage = ImageIO.read(new File("background/FantasyWorldMap.png"));
        } catch (IOException e) {
            bgImage = null;
        }
        try {
            logoImg = ImageIO.read(new File("icons/castle.png"));
        } catch (IOException e) {
            logoImg = null;
        }
    }

    private void setupFrame() {
        setTitle("Conqueror - The Age of Empires");
        setSize(900, 650);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setResizable(false);
    }

    private void buildUI() {
        JPanel root = new JPanel(new BorderLayout()) {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g;
                if (bgImage != null) {
                    g2.drawImage(bgImage, 0, 0, getWidth(), getHeight(), null);
                    // Dark overlay
                    g2.setColor(new Color(0, 0, 0, 150));
                    g2.fillRect(0, 0, getWidth(), getHeight());
                } else {
                    g2.setPaint(new GradientPaint(0, 0,
                            new Color(20, 10, 5), 0, getHeight(),
                            new Color(60, 30, 10)));
                    g2.fillRect(0, 0, getWidth(), getHeight());
                }
            }
        };
        root.setOpaque(false);

        // === TITLE PANEL ===
        JPanel titlePanel = new JPanel();
        titlePanel.setOpaque(false);
        titlePanel.setLayout(new BoxLayout(titlePanel, BoxLayout.Y_AXIS));
        titlePanel.setBorder(BorderFactory.createEmptyBorder(40, 0, 20, 0));

        if (logoImg != null) {
            Image scaled = logoImg.getScaledInstance(90, 90, Image.SCALE_SMOOTH);
            JLabel logoLabel = new JLabel(new ImageIcon(scaled));
            logoLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
            titlePanel.add(logoLabel);
            titlePanel.add(Box.createVerticalStrut(12));
        }

        JLabel title = new JLabel("CONQUEROR");
        title.setFont(new Font("Serif", Font.BOLD, 64));
        title.setForeground(new Color(220, 170, 50));
        title.setAlignmentX(Component.CENTER_ALIGNMENT);
        // Gold glow effect via drop shadow border
        title.setBorder(new EmptyBorder(0, 0, 0, 0));
        titlePanel.add(title);

        JLabel subtitle = new JLabel("The Age of Empires");
        subtitle.setFont(new Font("Serif", Font.ITALIC, 22));
        subtitle.setForeground(new Color(200, 200, 180));
        subtitle.setAlignmentX(Component.CENTER_ALIGNMENT);
        titlePanel.add(subtitle);

        root.add(titlePanel, BorderLayout.NORTH);

        // === CENTER FORM ===
        JPanel formOuter = new JPanel(new GridBagLayout());
        formOuter.setOpaque(false);

        JPanel form = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g;
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(15, 10, 5, 210));
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 20, 20);
                g2.setColor(new Color(180, 130, 40, 150));
                g2.setStroke(new BasicStroke(2));
                g2.drawRoundRect(1, 1, getWidth()-2, getHeight()-2, 20, 20);
            }
        };
        form.setOpaque(false);
        form.setLayout(new GridBagLayout());
        form.setPreferredSize(new Dimension(440, 300));
        form.setBorder(BorderFactory.createEmptyBorder(30, 40, 30, 40));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 8, 10, 8);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        // Player name
        JLabel nameLabel = makeFormLabel("⚔  Commander Name:");
        gbc.gridx = 0; gbc.gridy = 0; gbc.gridwidth = 1;
        form.add(nameLabel, gbc);

        nameField = new JTextField("General");
        styleTextField(nameField);
        gbc.gridx = 0; gbc.gridy = 1; gbc.gridwidth = 1;
        form.add(nameField, gbc);

        // City selection
        JLabel cityLabel = makeFormLabel("🏰  Choose Your Capital:");
        gbc.gridx = 0; gbc.gridy = 2;
        form.add(cityLabel, gbc);

        cityCombo = new JComboBox<>(new String[]{"Cairo", "Rome", "Sparta"});
        styleCityCombo(cityCombo);
        gbc.gridx = 0; gbc.gridy = 3;
        form.add(cityCombo, gbc);

        // Start button
        gbc.gridx = 0; gbc.gridy = 4;
        gbc.insets = new Insets(20, 8, 4, 8);
        JButton startBtn = makeGoldButton("⚔  CONQUER THE WORLD  ⚔");
        startBtn.addActionListener(e -> startGame());
        form.add(startBtn, gbc);

        formOuter.add(form);
        root.add(formOuter, BorderLayout.CENTER);

        // === BOTTOM ===
        JLabel footer = new JLabel("Expand • Recruit • Conquer", SwingConstants.CENTER);
        footer.setFont(new Font("Serif", Font.ITALIC, 14));
        footer.setForeground(new Color(150, 130, 80));
        footer.setBorder(BorderFactory.createEmptyBorder(0, 0, 20, 0));
        root.add(footer, BorderLayout.SOUTH);

        setContentPane(root);
    }

    private JLabel makeFormLabel(String text) {
        JLabel l = new JLabel(text);
        l.setFont(new Font("Serif", Font.BOLD, 15));
        l.setForeground(new Color(210, 170, 70));
        return l;
    }

    private void styleTextField(JTextField field) {
        field.setFont(new Font("Serif", Font.PLAIN, 16));
        field.setBackground(new Color(30, 20, 10, 200));
        field.setForeground(new Color(240, 220, 160));
        field.setCaretColor(new Color(220, 180, 60));
        field.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(160, 120, 40), 1),
            BorderFactory.createEmptyBorder(6, 10, 6, 10)
        ));
        field.setPreferredSize(new Dimension(300, 38));
    }

    private void styleCityCombo(JComboBox<String> combo) {
        combo.setFont(new Font("Serif", Font.BOLD, 16));
        combo.setBackground(new Color(30, 20, 10));
        combo.setForeground(new Color(240, 220, 160));
        combo.setBorder(BorderFactory.createLineBorder(new Color(160, 120, 40), 1));
        combo.setPreferredSize(new Dimension(300, 38));
        combo.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> l, Object v,
                    int i, boolean sel, boolean foc) {
                super.getListCellRendererComponent(l, v, i, sel, foc);
                setBackground(sel ? new Color(80, 55, 20) : new Color(30, 20, 10));
                setForeground(new Color(240, 220, 160));
                setFont(new Font("Serif", Font.BOLD, 15));
                setBorder(BorderFactory.createEmptyBorder(4, 10, 4, 10));
                return this;
            }
        });
    }

    private JButton makeGoldButton(String text) {
        JButton btn = new JButton(text) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g;
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                Color top = getModel().isPressed() ? new Color(100, 70, 10) :
                            getModel().isRollover() ? new Color(200, 150, 30) : new Color(170, 120, 20);
                Color bot = getModel().isPressed() ? new Color(60, 40, 5) :
                            getModel().isRollover() ? new Color(140, 100, 10) : new Color(110, 75, 10);
                g2.setPaint(new GradientPaint(0, 0, top, 0, getHeight(), bot));
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 10, 10);
                g2.setColor(new Color(220, 170, 50, 180));
                g2.setStroke(new BasicStroke(2));
                g2.drawRoundRect(1, 1, getWidth()-2, getHeight()-2, 10, 10);
                super.paintComponent(g);
            }
        };
        btn.setContentAreaFilled(false);
        btn.setBorderPainted(false);
        btn.setFocusPainted(false);
        btn.setFont(new Font("Serif", Font.BOLD, 16));
        btn.setForeground(new Color(255, 240, 180));
        btn.setPreferredSize(new Dimension(300, 46));
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return btn;
    }

    private void startGame() {
        String name = nameField.getText().trim();
        if (name.isEmpty()) name = "Commander";
        String city = (String) cityCombo.getSelectedItem();
        try {
            Game game = new Game(name, city);
            new GameFrame(game, name, city);
            dispose();
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(this,
                "Failed to load game data:\n" + ex.getMessage(),
                "Error", JOptionPane.ERROR_MESSAGE);
        }
    }
}
