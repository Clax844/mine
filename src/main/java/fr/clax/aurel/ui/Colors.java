package fr.clax.aurel.ui;

public final class Colors {
    private Colors() {}

    public static int alpha(int color, double opacity) {
        return ((int) Math.round(clamp(opacity, 0, 1) * 255) << 24) | (color & 0xffffff);
    }

    public static int mix(int a, int b, double t) {
        t = clamp(t, 0, 1);
        int out = 0;
        for (int shift : new int[]{0, 8, 16, 24}) {
            int channel = (int) Math.round(((a >>> shift) & 255) * (1 - t) + ((b >>> shift) & 255) * t);
            out |= channel << shift;
        }
        return out;
    }

    public static int hsv(double hue, double saturation, double value) {
        double h = ((hue % 360) + 360) % 360 / 60;
        double c = value * saturation, x = c * (1 - Math.abs(h % 2 - 1)), m = value - c;
        double r = 0, g = 0, b = 0;
        switch ((int) h) {
            case 0 -> { r = c; g = x; }
            case 1 -> { r = x; g = c; }
            case 2 -> { g = c; b = x; }
            case 3 -> { g = x; b = c; }
            case 4 -> { r = x; b = c; }
            default -> { r = c; b = x; }
        }
        return 0xff000000 | ((int) Math.round((r + m) * 255) << 16)
                | ((int) Math.round((g + m) * 255) << 8) | (int) Math.round((b + m) * 255);
    }

    public static double clamp(double n, double min, double max) {
        return Double.isFinite(n) ? Math.max(min, Math.min(max, n)) : min;
    }
}
