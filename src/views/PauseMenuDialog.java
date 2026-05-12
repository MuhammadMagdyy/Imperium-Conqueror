package views;

import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.awt.event.*;

public class PauseMenuDialog extends JDialog {

    public enum Choice { CONTINUE, HOW_TO_PLAY, RESTART, EXIT }
    private Choice result = Choice.CONTINUE;

    public PauseMenuDialog(Frame owner) {
        super(owner, true);
        setUndecorated(true);
        setSize(380, 420);
        setLocationRelativeTo(owner);

        JPanel root = new JPanel(new BorderLayout()) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g;
                g2.setColor(new Color(10, 6, 2, 245));
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 18, 18);
                g2.setColor(new Color(210, 165, 40));
                g2.setStroke(new BasicStroke(2.5f));
                g2.drawRoundRect(1, 1, getWidth()-3, getHeight()-3, 18, 18);
                g2.setColor(new Color(140, 100, 20, 70));
                g2.setStroke(new BasicStroke(1f));
                g2.drawRoundRect(6, 6, getWidth()-13, getHeight()-13, 14, 14);
            }
        };
        root.setOpaque(false);
        root.setBorder(BorderFactory.createEmptyBorder(28, 36, 28, 36));

        // Header
        JPanel header = new JPanel(new GridLayout(3, 1, 0, 4));
        header.setOpaque(false);
        JLabel icon = new JLabel("⚔", SwingConstants.CENTER);
        icon.setFont(new Font("Serif", Font.PLAIN, 42));
        icon.setForeground(new Color(210, 165, 40));
        JLabel title = new JLabel("GAME PAUSED", SwingConstants.CENTER);
        title.setFont(FontManager.FONT_TITLE.deriveFont(30f));
        title.setForeground(new Color(210, 165, 40));
        JLabel sep = new JLabel("─────────────────", SwingConstants.CENTER);
        sep.setFont(new Font("Serif", Font.PLAIN, 14));
        sep.setForeground(new Color(140, 100, 30, 160));
        header.add(icon); header.add(title); header.add(sep);
        root.add(header, BorderLayout.NORTH);

        // Buttons
        JPanel btns = new JPanel(new GridLayout(4, 1, 0, 12));
        btns.setOpaque(false);
        btns.setBorder(BorderFactory.createEmptyBorder(22, 0, 0, 0));

        JButton cont    = menuBtn("▶   Continue",       new Color(75, 190, 90));
        JButton howTo   = menuBtn("?   How To Play",    new Color(210, 165, 40));
        JButton restart = menuBtn("🔄   Restart Game",  new Color(80, 140, 210));
        JButton exit    = menuBtn("✕   Exit Game",      new Color(210, 70, 55));

        cont.addActionListener(e -> close(Choice.CONTINUE));
        howTo.addActionListener(e -> close(Choice.HOW_TO_PLAY));
        restart.addActionListener(e -> {
            int ok = JOptionPane.showConfirmDialog(this,
                "Restart from the beginning?", "Restart",
                JOptionPane.YES_NO_OPTION);
            if (ok == JOptionPane.YES_OPTION) close(Choice.RESTART);
        });
        exit.addActionListener(e -> {
            int ok = JOptionPane.showConfirmDialog(this,
                "Exit the game?", "Exit",
                JOptionPane.YES_NO_OPTION);
            if (ok == JOptionPane.YES_OPTION) close(Choice.EXIT);
        });

        btns.add(cont); btns.add(howTo); btns.add(restart); btns.add(exit);
        root.add(btns, BorderLayout.CENTER);

        // Hint footer
        JLabel footer = new JLabel("Press ESC to continue playing", SwingConstants.CENTER);
        footer.setFont(FontManager.FONT_ITALIC.deriveFont(12f));
        footer.setForeground(new Color(130, 115, 68));
        footer.setBorder(BorderFactory.createEmptyBorder(14, 0, 0, 0));
        root.add(footer, BorderLayout.SOUTH);

        setContentPane(root);
        setBackground(new Color(0, 0, 0, 0));

        getRootPane().registerKeyboardAction(
            e -> close(Choice.CONTINUE),
            KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0),
            JComponent.WHEN_IN_FOCUSED_WINDOW);
    }

    private void close(Choice c) {
        result = c;
        AudioManager.get().playClick();
        dispose();
    }

    public Choice getResult() { return result; }

    /** Show the dialog and return what the player chose. */
    public static Choice show(Frame owner) {
        PauseMenuDialog d = new PauseMenuDialog(owner);
        d.setVisible(true);
        return d.getResult();
    }

    private JButton menuBtn(String text, Color accent) {
        JButton b = new JButton(text) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g;
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                Color base = getModel().isPressed()
                    ? accent.darker().darker()
                    : getModel().isRollover()
                        ? new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), 55)
                        : new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), 28);
                g2.setColor(base);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 10, 10);
                g2.setColor(new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), 180));
                g2.setStroke(new BasicStroke(1.6f));
                g2.drawRoundRect(1, 1, getWidth()-2, getHeight()-2, 10, 10);
                super.paintComponent(g);
            }
        };
        b.setContentAreaFilled(false); b.setBorderPainted(false); b.setFocusPainted(false);
        b.setFont(FontManager.FONT_BODY_BOLD.deriveFont(15f));
        b.setForeground(new Color(235, 220, 165));
        b.setHorizontalAlignment(SwingConstants.LEFT);
        b.setBorder(BorderFactory.createEmptyBorder(0, 18, 0, 0));
        b.setPreferredSize(new Dimension(300, 48));
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return b;
    }
}
