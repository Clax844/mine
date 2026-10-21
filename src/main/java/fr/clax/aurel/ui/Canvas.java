package fr.clax.aurel.ui;

/** Logical coordinates. The Minecraft adapter handles resolution and GUI scale. */
public interface Canvas {
    void fill(int x, int y, int width, int height, int argb);
    void text(String text, int x, int y, int size, int argb);
    int textWidth(String text, int size);
    void avatar(int x, int y, int width, int height);

    default void round(int x, int y, int width, int height, int radius, int color) {
        gradient(x, y, width, height, radius, color, color, false);
    }

    default void gradient(int x, int y, int width, int height, int radius, int top, int bottom, boolean horizontal) {
        int r = Math.max(0, Math.min(radius, Math.min(width, height) / 2));
        // One band per pixel, bounded by the rounded rectangle. No shaders or mixins required.
        int length = horizontal ? width : height;
        for (int i = 0; i < length; i++) {
            double distance = Math.max(0, Math.max(r - i - .5, i + .5 - (length - r)));
            int inset = distance == 0 ? 0 : (int) Math.ceil(r - Math.sqrt(Math.max(0, r * r - distance * distance)));
            int color = Colors.mix(top, bottom, length <= 1 ? 0 : (double) i / (length - 1));
            if (horizontal) fill(x + i, y + inset, 1, height - 2 * inset, color);
            else fill(x + inset, y + i, width - 2 * inset, 1, color);
        }
    }
}
