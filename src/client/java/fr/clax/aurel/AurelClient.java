package fr.clax.aurel;

import com.mojang.blaze3d.platform.InputConstants;
import fr.clax.aurel.ui.MenuSettings;
import fr.clax.aurel.ui.SettingsStore;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.KeyMapping;
import net.minecraft.resources.Identifier;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

public final class AurelClient implements ClientModInitializer {
    public static final Logger LOG = LoggerFactory.getLogger("aurel");
    private final SettingsStore store = new SettingsStore(FabricLoader.getInstance().getConfigDir().resolve("aurel.properties"));
    private MenuSettings settings;

    @Override
    public void onInitializeClient() {
        try { settings = store.load(); }
        catch (IOException e) {
            // Preserve a damaged file for recovery instead of replacing it during startup.
            LOG.warn("Impossible de lire les préférences Aurel ; valeurs par défaut utilisées", e);
            settings = new MenuSettings();
        }
        KeyMapping.Category category = KeyMapping.Category.register(Identifier.fromNamespaceAndPath("aurel", "menu"));
        KeyMapping open = KeyMappingHelper.registerKeyMapping(new KeyMapping(
                "key.aurel.open", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_INSERT, category));
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (open.consumeClick()) {
                if (client.gui.screen() instanceof AurelScreen screen) screen.onClose();
                else if (client.player != null && client.gui.screen() == null) {
                    client.gui.setScreen(new AurelScreen(settings, store, open));
                }
            }
        });
    }
}
