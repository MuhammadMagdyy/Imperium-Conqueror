package views;

import java.awt.*;
import java.io.InputStream;

/**
 * Loads the three bundled OTF/TTF fonts from the JAR classpath and registers
 * them with the local GraphicsEnvironment so every Swing component can use them.
 *
 *  TITLE   — TeX Gyre Chorus  (calligraphy / drop-cap display)
 *  BODY    — TeX Gyre Bonum   (classical book serif, body & labels)
 *  ELEGANT — Lora             (refined variable serif, sub-headings)
 */
public class FontManager {

    // Public font family names after registration
    public static final String TITLE_FAMILY   = "TeX Gyre Chorus";
    public static final String BODY_FAMILY    = "TeX Gyre Bonum";
    public static final String ELEGANT_FAMILY = "Lora";

    // Pre-built ready-to-use instances
    public static Font FONT_TITLE_HUGE;   // 72 pt, Chorus
    public static Font FONT_TITLE_LARGE;  // 48 pt, Chorus
    public static Font FONT_TITLE;        // 28 pt, Chorus
    public static Font FONT_HEADING;      // 20 pt, Bonum Bold
    public static Font FONT_BODY_BOLD;    // 15 pt, Bonum Bold
    public static Font FONT_BODY;         // 14 pt, Bonum Regular
    public static Font FONT_SMALL;        // 12 pt, Bonum Regular
    public static Font FONT_ITALIC;       // 14 pt, Lora italic

    private static boolean loaded = false;

    public static synchronized void init() {
        if (loaded) return;
        loaded = true;

        GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();

        registerFont(ge, "fonts/Chorus.otf");
        registerFont(ge, "fonts/BonumBold.otf");
        registerFont(ge, "fonts/BonumRegular.otf");
        registerFont(ge, "fonts/Lora.ttf");

        // Build pre-sized instances (fall back to Serif if registration failed)
        FONT_TITLE_HUGE  = derive(TITLE_FAMILY,   Font.PLAIN,  72);
        FONT_TITLE_LARGE = derive(TITLE_FAMILY,   Font.PLAIN,  48);
        FONT_TITLE       = derive(TITLE_FAMILY,   Font.PLAIN,  28);
        FONT_HEADING     = derive(BODY_FAMILY,    Font.BOLD,   20);
        FONT_BODY_BOLD   = derive(BODY_FAMILY,    Font.BOLD,   15);
        FONT_BODY        = derive(BODY_FAMILY,    Font.PLAIN,  14);
        FONT_SMALL       = derive(BODY_FAMILY,    Font.PLAIN,  12);
        FONT_ITALIC      = derive(ELEGANT_FAMILY, Font.ITALIC, 14);
    }

    private static void registerFont(GraphicsEnvironment ge, String resource) {
        try (InputStream is = FontManager.class.getClassLoader()
                                               .getResourceAsStream(resource)) {
            if (is == null) return;
            int format = resource.endsWith(".otf") ? Font.TRUETYPE_FONT : Font.TRUETYPE_FONT;
            Font f = Font.createFont(format, is);
            ge.registerFont(f);
        } catch (Exception ignored) {}
    }

    private static Font derive(String family, int style, float size) {
        Font f = new Font(family, style, (int) size);
        // If family wasn't registered, Font constructor silently falls back to
        // the Dialog family — check and substitute our nice Serif fallback.
        if (f.getFamily().equals("Dialog")) {
            f = new Font("Serif", style, (int) size);
        }
        return f.deriveFont(size);
    }
}
