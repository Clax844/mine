package fr.clax.aurel.ui;

import java.util.LinkedHashMap;
import java.util.Map;

/** All cosmetic settings are bounded here, including values read from disk. */
public final class MenuSettings {
    public enum Option {
        OPACITY("Opacité du menu", .55, 1, .94, "%"),
        RADIUS("Arrondi des angles", 0, 22, 12, "px"),
        SHADOW("Profondeur de l'ombre", 0, 1, .45, "%"),
        DIM("Assombrir le jeu", 0, .8, .32, "%"),
        ACCENT_HUE("Couleur principale", 0, 359, 194, "°"),
        SECONDARY_HUE("Couleur secondaire", 0, 359, 250, "°"),
        SATURATION("Saturation", .2, 1, .78, "%"),
        BRIGHTNESS("Luminosité de l'accent", .45, 1, 1, "%"),
        GRADIENT_STRENGTH("Intensité du dégradé", 0, 1, .2, "%"),
        BORDER("Éclat des bordures", 0, 1, .24, "%"),
        DURATION("Durée d'ouverture", 120, 650, 280, "ms"),
        HOVER("Réactivité au survol", 0, 1, .65, "%"),
        SCALE("Échelle de l'interface", .75, 1.25, 1, "%");

        public final String label, unit;
        public final double min, max, initial;
        Option(String label, double min, double max, double initial, String unit) {
            this.label = label; this.min = min; this.max = max; this.initial = initial; this.unit = unit;
        }
        public String format(double value) {
            return Math.round(value * (unit.equals("%") ? 100 : 1)) + " " + unit;
        }
    }

    public enum Flag {
        TRANSPARENT("Fond transparent", true), GRADIENT("Dégradé du fond", true),
        HORIZONTAL("Dégradé horizontal", false), ANIMATIONS("Animations", true),
        SLIDE("Glissement à l'ouverture", true), SKIN("Afficher mon skin", true);
        public final String label;
        public final boolean initial;
        Flag(String label, boolean initial) { this.label = label; this.initial = initial; }
    }

    private final Map<Option, Double> numbers = new LinkedHashMap<>();
    private final Map<Flag, Boolean> flags = new LinkedHashMap<>();

    public MenuSettings() { reset(); }
    public double get(Option key) { return numbers.get(key); }
    public void set(Option key, double value) {
        numbers.put(key, Double.isFinite(value) ? Colors.clamp(value, key.min, key.max) : key.initial);
    }
    public boolean get(Flag key) { return flags.get(key); }
    public void set(Flag key, boolean value) { flags.put(key, value); }
    public void toggle(Flag key) { set(key, !get(key)); }
    public int accent() { return Colors.hsv(get(Option.ACCENT_HUE), get(Option.SATURATION), get(Option.BRIGHTNESS)); }
    public int secondary() { return Colors.hsv(get(Option.SECONDARY_HUE), get(Option.SATURATION), get(Option.BRIGHTNESS)); }
    public double opacity() { return get(Flag.TRANSPARENT) ? get(Option.OPACITY) : 1; }

    public void reset() {
        for (Option key : Option.values()) numbers.put(key, key.initial);
        for (Flag key : Flag.values()) flags.put(key, key.initial);
    }

    public void palette(int index) {
        double[][] palettes = {{194, 250}, {266, 319}, {158, 193}, {17, 342}};
        if (index < 0 || index >= palettes.length) throw new IllegalArgumentException("Unknown palette");
        set(Option.ACCENT_HUE, palettes[index][0]);
        set(Option.SECONDARY_HUE, palettes[index][1]);
    }
}
