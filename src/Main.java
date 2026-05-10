import views.AudioManager;
import views.MainMenuFrame;
import views.FontManager; // Added import
import javax.swing.*;
import java.awt.Color;

public class Main {
    public static void main(String[] args) {
        // 1. Initialize custom fonts BEFORE anything else
        FontManager.init();

        // 2. BOOT THE AUDIO ENGINE
        // This triggers buildAndPlayMusic() immediately
        AudioManager.get();

        System.setProperty("awt.useSystemAAFontSettings", "on");
        System.setProperty("swing.aatext", "true");

        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName());
            } catch (Exception ignored) {}

            // Dark medieval theme overrides
            UIManager.put("OptionPane.background",         new Color(18, 12, 5));
            UIManager.put("Panel.background",              new Color(18, 12, 5));
            UIManager.put("OptionPane.messageForeground",  new Color(220, 200, 140));

            // Use custom fonts in the UIManager so dialogs and popups match the theme
            UIManager.put("OptionPane.messageFont",        FontManager.FONT_BODY);
            UIManager.put("Button.font",                  FontManager.FONT_BODY_BOLD);

            UIManager.put("Button.background",             new Color(60, 42, 12));
            UIManager.put("Button.foreground",             new Color(220, 195, 120));
            UIManager.put("Button.focus",                  new Color(0, 0, 0, 0));
            UIManager.put("ComboBox.background",           new Color(30, 20, 8));
            UIManager.put("ComboBox.foreground",           new Color(220, 200, 140));
            UIManager.put("TextField.background",          new Color(30, 20, 8));
            UIManager.put("TextField.foreground",          new Color(220, 200, 140));
            UIManager.put("TextField.caretForeground",     new Color(220, 175, 50));
            UIManager.put("ScrollBar.thumb",               new Color(90, 65, 20));
            UIManager.put("ScrollBar.track",               new Color(30, 20, 8));
            UIManager.put("List.background",               new Color(18, 12, 5));
            UIManager.put("List.foreground",               new Color(220, 200, 140));
            UIManager.put("List.selectionBackground",      new Color(120, 85, 20));
            UIManager.put("List.selectionForeground",      new Color(255, 240, 180));

            new MainMenuFrame();
        });
    }
}