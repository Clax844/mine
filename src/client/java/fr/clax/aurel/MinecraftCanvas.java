package fr.clax.aurel;

import fr.clax.aurel.ui.Canvas;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;

final class MinecraftCanvas implements Canvas {
    private final GuiGraphicsExtractor graphics;
    private final Minecraft client;
    private final double mouseX, mouseY;

    MinecraftCanvas(GuiGraphicsExtractor graphics, Minecraft client, double mouseX, double mouseY) {
        this.graphics = graphics; this.client = client; this.mouseX = mouseX; this.mouseY = mouseY;
    }

    @Override
    public void fill(int x, int y, int width, int height, int argb) {
        if (width > 0 && height > 0) graphics.fill(x, y, x + width, y + height, argb);
    }

    @Override
    public void text(String text, int x, int y, int size, int argb) {
        float scale = size / 9f;
        graphics.pose().pushMatrix();
        graphics.pose().translate((float) x, (float) y);
        graphics.pose().scale(scale, scale);
        graphics.text(client.font, text, 0, 0, argb, false);
        graphics.pose().popMatrix();
    }

    @Override
    public int textWidth(String text, int size) { return Math.round(client.font.width(text) * size / 9f); }

    @Override
    public void avatar(int x, int y, int width, int height) {
        if (client.player == null) return;
        // Vanilla renders the actual player's skin, including its current skin layers.
        InventoryScreen.extractEntityInInventoryFollowsMouse(graphics, x, y, x + width, y + height,
                (int) (height * .43), .0625f, (float) mouseX, (float) mouseY, client.player);
    }
}
