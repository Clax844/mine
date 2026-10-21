package fr.clax.aurel;

import fr.clax.aurel.ui.Colors;
import fr.clax.aurel.ui.MenuSettings;
import fr.clax.aurel.ui.MenuUi;
import fr.clax.aurel.ui.SettingsStore;
import fr.clax.aurel.ui.Transition;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

import java.io.IOException;

import static fr.clax.aurel.ui.MenuSettings.Flag.*;
import static fr.clax.aurel.ui.MenuSettings.Option.*;

public final class AurelScreen extends Screen {
    private final MenuSettings settings;
    private final SettingsStore store;
    private final KeyMapping openKey;
    private final MenuUi ui;
    private final Transition transition = new Transition();
    private long lastFrame = System.nanoTime();
    private float scale = 1, originX, originY;
    private boolean failedSave;

    AurelScreen(MenuSettings settings, SettingsStore store, KeyMapping openKey) {
        super(Component.literal("Aurel — Personnalisation"));
        this.settings = settings; this.store = store; this.openKey = openKey;
        ui = new MenuUi(settings, _ -> persist(), this::onClose);
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        // Aurel controls dimming itself, so transparent surfaces reveal the world.
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        long now = System.nanoTime();
        double seconds = Math.max(0, (now - lastFrame) / 1_000_000_000.0);
        lastFrame = now;
        double reveal = transition.update(seconds, settings.get(DURATION), settings.get(ANIMATIONS));
        if (transition.finished()) { minecraft.gui.setScreen(null); return; }
        graphics.fill(0, 0, width, height, Colors.alpha(0xff03070e, settings.get(DIM) * reveal));

        float fit = Math.min((width - 24f) / MenuUi.WIDTH, (height - 24f) / MenuUi.HEIGHT);
        // SCALE stays within the viewport. On small windows it can reduce the menu further.
        scale = Math.max(.05f, Math.min(fit, fit * .84f * (float) settings.get(SCALE))) * (float) (.975 + .025 * reveal);
        originX = (width - MenuUi.WIDTH * scale) / 2;
        originY = (height - MenuUi.HEIGHT * scale) / 2 + (settings.get(SLIDE) ? (float) ((1 - reveal) * 22 * scale) : 0);
        double logicalX = (mouseX - originX) / scale, logicalY = (mouseY - originY) / scale;
        graphics.pose().pushMatrix();
        graphics.pose().translate(originX, originY);
        graphics.pose().scale(scale, scale);
        ui.paint(new MinecraftCanvas(graphics, minecraft, logicalX, logicalY), logicalX, logicalY, seconds,
                minecraft.player == null ? "Joueur" : minecraft.player.getName().getString());
        graphics.pose().popMatrix();
        super.extractRenderState(graphics, mouseX, mouseY, delta);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (transition.closing()) return true;
        if (event.button() == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
            return ui.mouseDown((event.x() - originX) / scale, (event.y() - originY) / scale);
        }
        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double dx, double dy) {
        if (transition.closing()) return true;
        if (ui.dragging()) { ui.drag((event.x() - originX) / scale); return true; }
        return super.mouseDragged(event, dx, dy);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        if (ui.dragging()) { ui.release(); return true; }
        return super.mouseReleased(event);
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (transition.closing()) return true;
        if (event.key() == GLFW.GLFW_KEY_ESCAPE || openKey.matches(event)) { onClose(); return true; }
        if (event.key() == GLFW.GLFW_KEY_TAB) {
            ui.focusNext((event.modifiers() & GLFW.GLFW_MOD_SHIFT) != 0); return true;
        }
        if (event.key() == GLFW.GLFW_KEY_LEFT) return ui.adjust(-1);
        if (event.key() == GLFW.GLFW_KEY_RIGHT) return ui.adjust(1);
        if (event.key() == GLFW.GLFW_KEY_ENTER || event.key() == GLFW.GLFW_KEY_SPACE) return ui.activate();
        return super.keyPressed(event);
    }

    private boolean persist() {
        try {
            store.save(settings); failedSave = false; ui.saved(); return true;
        } catch (IOException e) {
            failedSave = true;
            ui.notify("Sauvegarde impossible. Voir les logs ; recliquez pour quitter.");
            AurelClient.LOG.error("Impossible d'enregistrer les préférences Aurel", e);
            return false;
        }
    }

    @Override
    public void onClose() {
        if (transition.closing()) return;
        ui.release();
        // A failed save stays visible. A second close lets the user leave without trapping them.
        if (!failedSave && !persist()) return;
        transition.close();
    }

    @Override
    public boolean isPauseScreen() { return false; }

    @Override
    public void removed() {
        // Preserve adjustments when another screen or a disconnect interrupts this screen.
        if (ui.dirty()) persist();
        super.removed();
    }
}
