package fr.clax.aurel.ui;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import static fr.clax.aurel.ui.MenuSettings.Flag.*;
import static fr.clax.aurel.ui.MenuSettings.Option.*;

/** The production menu, independent of the game renderer so interactions can be tested headlessly. */
public final class MenuUi {
    public static final int WIDTH = 900, HEIGHT = 560;
    private static final int TEXT = 0xffedf4fa, MUTED = 0xff8b9bab, QUIET = 0xff596b7c;
    private static final String[] PAGES = {"Apparence", "Couleurs", "Animations", "Compte", "Préréglages"};
    private static final String[] SUBTITLES = {
            "Des détails qui font toute la différence.", "Une signature qui n'appartient qu'à vous.",
            "Un mouvement discret. Une sensation naturelle.", "Votre identité, au cœur de l'interface.",
            "Quatre ambiances. Un point de départ."};
    private static final String[] PALETTES = {"Océan", "Améthyste", "Jade", "Crépuscule"};
    private final MenuSettings settings;
    private final Consumer<MenuSettings> save;
    private final Runnable close;
    private final List<Node> nodes = new ArrayList<>();
    private final Map<String, Double> motions = new HashMap<>();
    private final Map<String, Double> presses = new HashMap<>();
    private Node dragging;
    private int page, focus = -1;
    private double mouseX, mouseY, dt;
    private double notificationTime;
    private String notification = "", playerName = "Joueur";
    private boolean dirty, confirmReset;
    private double resetTime;

    private record Node(String id, String label, int x, int y, int w, int h,
                        Runnable action, MenuSettings.Option option, int trackX, int trackWidth) {
        boolean contains(double px, double py) { return px >= x && py >= y && px < x + w && py < y + h; }
    }

    public MenuUi(MenuSettings settings, Consumer<MenuSettings> save, Runnable close) {
        this.settings = settings; this.save = save; this.close = close;
    }

    public MenuSettings settings() { return settings; }
    public int page() { return page; }
    public boolean dirty() { return dirty; }
    public void notify(String message) { notification = message; notificationTime = 4; }

    public void paint(Canvas c, double mouseX, double mouseY, double seconds, String name) {
        this.mouseX = mouseX; this.mouseY = mouseY;
        dt = Colors.clamp(seconds, 0, .1);
        playerName = name == null || name.isBlank() ? "Joueur" : name;
        notificationTime = Math.max(0, notificationTime - Math.max(0, seconds));
        resetTime -= Math.max(0, seconds);
        if (resetTime <= 0) confirmReset = false;
        nodes.clear();
        int r = radius(), accent = settings.accent();

        for (int spread = 12; spread >= 2; spread -= 2) {
            c.round(-spread, -spread + 5, WIDTH + spread * 2, HEIGHT + spread * 2, r + spread,
                    Colors.alpha(0xff000000, settings.get(SHADOW) * .026));
        }
        c.round(-1, -1, WIDTH + 2, HEIGHT + 2, r + 1, Colors.alpha(accent, .12 + settings.get(BORDER) * .35));
        int top = Colors.alpha(0xff0b1019, settings.opacity());
        int bottom = settings.get(GRADIENT)
                ? Colors.alpha(Colors.mix(0xff0b1019, settings.secondary(), settings.get(GRADIENT_STRENGTH) * .25), settings.opacity()) : top;
        c.gradient(0, 0, WIDTH, HEIGHT, r, top, bottom, settings.get(HORIZONTAL));
        c.round(1, 1, 187, HEIGHT - 2, Math.max(0, r - 1), Colors.alpha(0xff131d2a, settings.opacity()));
        c.fill(188, 20, 1, HEIGHT - 40, 0xff22303e);

        c.round(22, 27, 29, 29, 9, Colors.alpha(accent, .16));
        c.text("A", 30, 32, 19, accent);
        c.text("AUREL", 62, 28, 23, TEXT);
        c.text("VOTRE ESPACE CLIENT", 23, 73, 9, QUIET);
        c.fill(22, 105, 143, 1, 0xff263343);
        c.text("PERSONNALISATION", 24, 124, 9, QUIET);
        for (int i = 0; i < PAGES.length; i++) {
            final int target = i;
            String id = "page-" + i;
            int y = 147 + i * 44;
            boolean selected = page == i;
            double hover = motion(id, hot(14, y, 160, 36) || selected);
            c.round(14, y, 160, 36, 7, Colors.alpha(accent, selected ? .16 : hover * .07));
            if (selected) c.round(14, y + 9, 3, 18, 1, accent);
            icon(c, i, 27, y + 11, selected ? accent : MUTED);
            c.text(PAGES[i], 52, y + 11, 12, selected ? TEXT : MUTED);
            add(new Node(id, PAGES[i], 14, y, 160, 36, () -> select(target), null, 0, 0));
            focusRing(c, id, 14, y, 160, 36);
        }
        c.text("FABRIC  /  26.2", 25, 407, 9, QUIET);
        c.round(14, 445, 160, 96, 9, 0xff0d1622);
        c.round(24, 458, 40, 58, 7, Colors.alpha(accent, .09));
        if (settings.get(SKIN)) c.avatar(24, 459, 40, 56);
        else c.text(initial(), 37, 477, 18, accent);
        c.text(shorten(c, playerName, 95, 12), 73, 467, 12, TEXT);
        c.round(73, 487, 5, 5, 2, 0xff51d69b);
        c.text("Compte actif", 83, 485, 9, MUTED);
        c.text("Préférences locales", 25, 525, 9, QUIET);

        c.text(PAGES[page], 214, 30, 23, TEXT);
        c.text(SUBTITLES[page], 214, 64, 11, MUTED);
        button(c, "save", dirty ? "Enregistrer *" : "Enregistrer", 726, 26, 112, 30, true, this::saveNow);
        button(c, "close", "×", 848, 26, 26, 30, false, close);
        c.fill(214, 94, 660, 1, 0xff22303e);

        switch (page) {
            case 0 -> appearance(c);
            case 1 -> colors(c);
            case 2 -> animations(c);
            case 3 -> account(c);
            case 4 -> presets(c);
            default -> throw new IllegalStateException("Invalid page");
        }
        c.text("TAB  Naviguer     ← →  Ajuster     ENTRÉE  Activer", 214, 534, 9, QUIET);
        String hint = "INSER / ÉCHAP  Fermer";
        c.text(hint, 874 - c.textWidth(hint, 9), 534, 9, QUIET);
        if (notificationTime > 0) {
            c.round(360, 490, 355, 31, 8, 0xff243349);
            c.text(shorten(c, notification, 327, 11), 374, 500, 11, TEXT);
        }
    }

    private void appearance(Canvas c) {
        card(c, 214, 114, 322, 302, "Surface & profondeur", "L'équilibre entre légèreté et contraste.");
        toggle(c, TRANSPARENT, 230, 165, 290);
        toggle(c, GRADIENT, 230, 204, 290);
        slider(c, OPACITY, 230, 251, 290);
        slider(c, RADIUS, 230, 304, 290);
        slider(c, SHADOW, 230, 357, 290);
        card(c, 552, 114, 322, 302, "Aperçu en direct", "Chaque réglage est appliqué immédiatement.");
        preview(c, 569, 170, 288, 114);
        slider(c, DIM, 569, 305, 288);
        slider(c, BORDER, 569, 358, 288);
        banner(c, "Votre interface, votre signature.", "Affinez votre palette dans l'onglet Couleurs.", "Choisir ma palette", () -> select(1));
    }

    private void colors(Canvas c) {
        card(c, 214, 114, 322, 302, "Palette personnalisée", "Deux teintes pour composer votre ambiance.");
        slider(c, ACCENT_HUE, 230, 171, 290);
        slider(c, SECONDARY_HUE, 230, 226, 290);
        slider(c, SATURATION, 230, 281, 290);
        slider(c, BRIGHTNESS, 230, 336, 290);
        card(c, 552, 114, 322, 302, "Matière & dégradé", "Une touche de couleur, sans surcharge.");
        preview(c, 569, 170, 288, 85);
        toggle(c, GRADIENT, 569, 270, 288);
        toggle(c, HORIZONTAL, 569, 309, 288);
        slider(c, GRADIENT_STRENGTH, 569, 359, 288);
        banner(c, "Un peu d'inspiration ?", "Océan, Améthyste, Jade ou Crépuscule.", "Voir les préréglages", () -> select(4));
    }

    private void animations(Canvas c) {
        card(c, 214, 114, 322, 302, "Mouvements", "Des transitions douces, jamais envahissantes.");
        toggle(c, ANIMATIONS, 230, 168, 290);
        toggle(c, SLIDE, 230, 209, 290);
        slider(c, DURATION, 230, 267, 290);
        slider(c, HOVER, 230, 327, 290);
        card(c, 552, 114, 322, 302, "À votre rythme", "Testez les interactions directement ici.");
        c.text("Survolez, cliquez, ajustez.", 574, 176, 15, TEXT);
        c.text("Les boutons réagissent à vos gestes.", 574, 204, 11, MUTED);
        button(c, "test-motion", "Essayer l'animation", 575, 236, 274, 38, true, () -> notify("Une touche de mouvement. Juste ce qu'il faut."));
        slider(c, SCALE, 574, 307, 276);
        c.text("L'échelle s'adapte aussi à votre écran.", 574, 369, 10, QUIET);
        banner(c, "Le confort passe en premier.", "Désactivez les animations pour des changements instantanés.", "Retour à l'apparence", () -> select(0));
    }

    private void account(Canvas c) {
        card(c, 214, 114, 322, 302, "Votre compte", "Le profil actuellement connecté à Minecraft.");
        c.round(238, 173, 274, 138, 10, Colors.alpha(settings.accent(), .07));
        if (settings.get(SKIN)) c.avatar(252, 179, 73, 124);
        else c.text(initial(), 270, 229, 32, settings.accent());
        c.text(shorten(c, playerName, 169, 18), 335, 218, 18, TEXT);
        c.text("Minecraft Java", 335, 249, 11, MUTED);
        c.text("Session active", 335, 271, 10, 0xff51d69b);
        toggle(c, SKIN, 238, 339, 274);
        card(c, 552, 114, 322, 302, "Simplement personnel", "Vos réglages restent sur cet appareil.");
        c.text("Une interface à votre image.", 574, 177, 15, TEXT);
        c.text("Votre skin vient du joueur en jeu.", 574, 212, 11, MUTED);
        c.text("Aucune clé ni connexion supplémentaire.", 574, 238, 11, MUTED);
        c.text("La touche d'ouverture se reconfigure", 574, 283, 11, MUTED);
        c.text("dans Options > Contrôles > Aurel.", 574, 305, 11, MUTED);
        button(c, "account-save", "Sauvegarder mes préférences", 574, 352, 277, 34, true, this::saveNow);
        banner(c, "Prêt à retrouver votre monde ?", "Vos préférences sont aussi sauvegardées à la fermeture.", "Revenir au jeu", close);
    }

    private void presets(Canvas c) {
        double[][] hue = {{194, 250}, {266, 319}, {158, 193}, {17, 342}};
        for (int i = 0; i < 4; i++) {
            int x = 214 + i % 2 * 338, y = 114 + i / 2 * 155;
            int a = Colors.hsv(hue[i][0], .78, 1), b = Colors.hsv(hue[i][1], .78, 1);
            card(c, x, y, 322, 141, PALETTES[i], i == 0 ? "Clair, précis et électrique." : i == 1 ? "Un éclat doux et créatif." : i == 2 ? "Une fraîcheur naturellement calme." : "Chaleureux, vibrant, singulier.");
            c.gradient(x + 17, y + 59, 150, 60, 9, Colors.alpha(a, .2), Colors.alpha(b, .4), true);
            c.round(x + 32, y + 77, 61, 5, 2, a);
            c.round(x + 32, y + 91, 104, 4, 2, Colors.alpha(0xffffffff, .25));
            final int palette = i;
            button(c, "preset-" + i, "Appliquer", x + 188, y + 74, 116, 31, false, () -> {
                settings.palette(palette); dirty = true; notify("Palette « " + PALETTES[palette] + " » appliquée");
            });
        }
        banner(c, "Repartir d'une page blanche.", "Rétablir les couleurs, les surfaces et les animations.",
                confirmReset ? "Confirmer le reset" : "Réinitialiser", () -> {
                    if (confirmReset) { settings.reset(); dirty = true; confirmReset = false; notify("Préférences par défaut restaurées"); }
                    else { confirmReset = true; resetTime = 4; }
                });
    }

    private void preview(Canvas c, int x, int y, int w, int h) {
        c.gradient(x, y, w, h, radius(), Colors.mix(0xff182536, settings.accent(), .09),
                settings.get(GRADIENT) ? Colors.mix(0xff182536, settings.secondary(), settings.get(GRADIENT_STRENGTH)) : 0xff182536,
                settings.get(HORIZONTAL));
        c.round(x + 14, y + 15, 4, h - 30, 2, settings.accent());
        c.text("Un espace qui vous ressemble.", x + 30, y + 20, 12, TEXT);
        c.text("Élégant jusque dans les détails.", x + 30, y + 43, 10, MUTED);
        if (h > 90) {
            c.round(x + 30, y + 72, 85, 21, 6, Colors.alpha(settings.accent(), .2));
            c.text("Votre palette", x + 38, y + 78, 9, settings.accent());
            c.round(x + w - 44, y + 76, 26, 14, 7, settings.accent());
            c.round(x + w - 32, y + 78, 10, 10, 5, TEXT);
        }
    }

    private void card(Canvas c, int x, int y, int w, int h, String title, String subtitle) {
        c.round(x - 1, y - 1, w + 2, h + 2, radius() + 1,
                Colors.alpha(Colors.mix(0xff202d3e, settings.accent(), settings.get(BORDER) * .2), .1 + settings.get(BORDER) * .15));
        c.round(x, y, w, h, radius(), Colors.alpha(0xff101925, settings.opacity()));
        c.text(title, x + 17, y + 16, 14, TEXT);
        c.text(subtitle, x + 17, y + 37, 10, MUTED);
    }

    private void banner(Canvas c, String title, String subtitle, String action, Runnable callback) {
        c.round(214, 433, 660, 82, radius(), Colors.alpha(settings.accent(), .06));
        c.text(title, 231, 451, 14, TEXT);
        c.text(subtitle, 231, 477, 10, MUTED);
        button(c, "banner", action, 712, 452, 146, 32, false, callback);
    }

    private void toggle(Canvas c, MenuSettings.Flag flag, int x, int y, int w) {
        String id = flag.name();
        boolean on = settings.get(flag);
        double t = motion("switch-" + id, on);
        double hover = motion("hover-" + id, hot(x, y, w, 30));
        c.text(flag.label, x, y + 10, 12, Colors.mix(MUTED, TEXT, hover));
        c.round(x + w - 34, y + 7, 34, 18, 9, Colors.mix(0xff293746, settings.accent(), t));
        c.round(x + w - 31 + (int) Math.round(16 * t), y + 10, 12, 12, 6, TEXT);
        add(new Node(id, flag.label, x, y, w, 30, () -> { settings.toggle(flag); dirty = true; }, null, 0, 0));
        focusRing(c, id, x - 4, y, w + 8, 30);
    }

    private void slider(Canvas c, MenuSettings.Option option, int x, int y, int w) {
        String id = option.name();
        double value = settings.get(option), target = (value - option.min) / (option.max - option.min);
        double t = smooth("value-" + id, target);
        double hover = motion("hover-" + id, hot(x, y, w, 43) || dragging != null && dragging.id.equals(id));
        c.text(option.label, x, y, 11, MUTED);
        String formatted = option.format(value);
        c.text(formatted, x + w - c.textWidth(formatted, 10), y, 10, TEXT);
        int trackY = y + 27;
        if (option == ACCENT_HUE || option == SECONDARY_HUE) {
            for (int i = 0; i < w; i++) c.fill(x + i, trackY, 1, 4, Colors.hsv(359.0 * i / (w - 1), .8, .9));
        } else {
            c.round(x, trackY, w, 4, 2, 0xff2b3a4c);
            c.round(x, trackY, Math.max(4, (int) (t * w)), 4, 2, settings.accent());
        }
        int knob = (int) Math.round(x + t * (w - 10));
        if (hover > .01) c.round(knob - 4, trackY - 7, 18, 18, 9, Colors.alpha(settings.accent(), hover * .15));
        c.round(knob, trackY - 3, 10, 10, 5, TEXT);
        add(new Node(id, option.label, x, y, w, 43, null, option, x + 5, w - 10));
        focusRing(c, id, x - 4, y - 4, w + 8, 45);
    }

    private void button(Canvas c, String id, String label, int x, int y, int w, int h, boolean primary, Runnable callback) {
        double hover = motion(id, hot(x, y, w, h));
        double press = smooth("press-" + id, presses.getOrDefault(id, 0.0));
        presses.computeIfPresent(id, (_, value) -> Math.max(0, value - dt * 6));
        int inset = (int) Math.round(press * 2), yy = y + inset;
        int bg = primary ? Colors.mix(settings.accent(), TEXT, hover * .12) : Colors.mix(0xff182637, settings.accent(), hover * .13);
        c.round(x, yy, w, h - inset, Math.min(radius(), 7), primary ? bg : 0xff2a3b4d);
        if (!primary) c.round(x + 1, yy + 1, w - 2, h - inset - 2, Math.max(0, Math.min(radius(), 7) - 1), bg);
        int size = label.length() > 21 ? 10 : 11;
        c.text(label, x + (w - c.textWidth(label, size)) / 2, yy + (h - size) / 2, size, primary ? 0xff081923 : TEXT);
        add(new Node(id, label, x, y, w, h, callback, null, 0, 0));
        focusRing(c, id, x - 2, y - 2, w + 4, h + 4);
    }

    private void icon(Canvas c, int index, int x, int y, int color) {
        switch (index) {
            case 0 -> { c.round(x, y, 13, 13, 3, color); c.round(x + 3, y + 3, 7, 7, 2, 0xff172838); }
            case 1 -> { c.round(x, y, 7, 7, 3, color); c.round(x + 7, y + 7, 6, 6, 3, color); c.round(x, y + 8, 5, 5, 2, color); }
            case 2 -> { c.fill(x, y + 2, 12, 2, color); c.fill(x + 3, y + 6, 9, 2, color); c.fill(x + 6, y + 10, 6, 2, color); }
            case 3 -> { c.round(x + 4, y, 6, 6, 3, color); c.round(x + 1, y + 8, 12, 5, 2, color); }
            default -> { c.round(x, y, 5, 5, 1, color); c.round(x + 8, y, 5, 5, 1, color); c.round(x, y + 8, 5, 5, 1, color); c.round(x + 8, y + 8, 5, 5, 1, color); }
        }
    }

    public boolean mouseDown(double x, double y) {
        for (int i = nodes.size() - 1; i >= 0; i--) {
            Node node = nodes.get(i);
            if (!node.contains(x, y)) continue;
            focus = i;
            if (node.option != null) { dragging = node; drag(x); }
            else { presses.put(node.id, 1.0); node.action.run(); }
            return true;
        }
        focus = -1;
        return false;
    }

    public void drag(double x) {
        if (dragging == null) return;
        double t = Colors.clamp((x - dragging.trackX) / dragging.trackWidth, 0, 1);
        settings.set(dragging.option, dragging.option.min + t * (dragging.option.max - dragging.option.min));
        dirty = true;
    }
    public boolean dragging() { return dragging != null; }
    public void release() { dragging = null; }

    public void focusNext(boolean backwards) {
        if (nodes.isEmpty()) return;
        focus = focus < 0 ? (backwards ? nodes.size() - 1 : 0) : Math.floorMod(focus + (backwards ? -1 : 1), nodes.size());
    }

    public boolean activate() {
        if (focus < 0 || focus >= nodes.size()) return false;
        Node node = nodes.get(focus);
        if (node.action == null) return false;
        presses.put(node.id, 1.0); node.action.run(); return true;
    }

    public boolean adjust(int direction) {
        if (focus < 0 || focus >= nodes.size()) return false;
        Node node = nodes.get(focus);
        if (node.option == null) return false;
        MenuSettings.Option key = node.option;
        settings.set(key, settings.get(key) + direction * (key.max - key.min) / 100);
        dirty = true; return true;
    }

    public void saveNow() { save.accept(settings); }
    public void saved() { dirty = false; notify("Préférences enregistrées"); }
    private void select(int target) { page = target; focus = -1; dragging = null; confirmReset = false; }
    private void add(Node node) { nodes.add(node); }
    private int radius() { return (int) Math.round(settings.get(RADIUS)); }
    private String initial() { return playerName.substring(0, 1).toUpperCase(java.util.Locale.ROOT); }
    private boolean hot(int x, int y, int w, int h) { return mouseX >= x && mouseX < x + w && mouseY >= y && mouseY < y + h; }
    private double motion(String id, boolean on) { return smooth(id, on ? 1 : 0); }
    private double smooth(String id, double target) {
        double previous = motions.getOrDefault(id, target);
        double value = settings.get(ANIMATIONS)
                ? previous + (target - previous) * (1 - Math.exp(-dt * (10 + 24 * settings.get(HOVER)))) : target;
        motions.put(id, value); return value;
    }
    private void focusRing(Canvas c, String id, int x, int y, int w, int h) {
        if (focus >= 0 && focus < nodes.size() && nodes.get(focus).id.equals(id)) {
            c.fill(x, y, w, 1, settings.accent()); c.fill(x, y + h - 1, w, 1, settings.accent());
            c.fill(x, y, 1, h, settings.accent()); c.fill(x + w - 1, y, 1, h, settings.accent());
        }
    }
    private static String shorten(Canvas c, String text, int maxWidth, int size) {
        if (c.textWidth(text, size) <= maxWidth) return text;
        int end = text.length();
        while (end > 0 && c.textWidth(text.substring(0, end) + "…", size) > maxWidth) end--;
        return text.substring(0, end) + "…";
    }
}
