package fr.clax.aurel.ui;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static fr.clax.aurel.ui.MenuSettings.Flag.*;
import static fr.clax.aurel.ui.MenuSettings.Option.*;

/** Standalone integration checks; run with scripts/check-core.sh, without Minecraft stubs. */
public final class CoreChecks {
    private static int count;
    public static void main(String[] args) throws Exception {
        run("settings persistence and bounds", CoreChecks::persistence);
        run("invalid configuration and file preservation", CoreChecks::damagedConfiguration);
        run("mouse switches, slider capture and keyboard adjustment", CoreChecks::interactions);
        run("navigation, palettes, reset confirmation and save", CoreChecks::navigation);
        run("all pages render and expose expected controls", CoreChecks::pages);
        run("reversible transitions and reduced motion", CoreChecks::transitions);
        System.out.println("PASS: " + count + " checks; 0 failed, 0 skipped");
    }

    private static void persistence() throws Exception {
        Path dir = Files.createTempDirectory("aurel-check-");
        try {
            SettingsStore store = new SettingsStore(dir.resolve("nested/aurel.properties"));
            MenuSettings s = store.load();
            check(s.get(ANIMATIONS), "default animations");
            for (MenuSettings.Option k : MenuSettings.Option.values()) s.set(k, k.min + (k.max - k.min) * .37);
            for (MenuSettings.Flag k : MenuSettings.Flag.values()) s.set(k, !k.initial);
            store.save(s);
            MenuSettings loaded = store.load();
            for (MenuSettings.Option k : MenuSettings.Option.values()) check(s.get(k) == loaded.get(k), "roundtrip " + k);
            for (MenuSettings.Flag k : MenuSettings.Flag.values()) check(s.get(k) == loaded.get(k), "roundtrip " + k);
            s.set(OPACITY, Double.NaN); check(s.get(OPACITY) == OPACITY.initial, "NaN default");
            s.set(RADIUS, 99999); check(s.get(RADIUS) == RADIUS.max, "upper bound");
            s.set(RADIUS, -100); check(s.get(RADIUS) == 0, "lower bound");
            s.set(TRANSPARENT, false); check(s.opacity() == 1, "opaque background override");
            store.save(s); check(store.load().get(RADIUS) == 0, "repeat save");
            try (var files = Files.list(dir.resolve("nested"))) { check(files.count() == 1, "no abandoned temp files"); }
        } finally { remove(dir); }
    }

    private static void damagedConfiguration() throws Exception {
        Path dir = Files.createTempDirectory("aurel-damaged-");
        try {
            Path file = dir.resolve("aurel.properties");
            Files.writeString(file, "OPACITY=Infinity\nRADIUS=-40\nDURATION=oops\nANIMATIONS=maybe\nSKIN=false\n");
            MenuSettings s = new SettingsStore(file).load();
            check(s.get(OPACITY) == OPACITY.initial, "infinite opacity default");
            check(s.get(RADIUS) == 0, "invalid radius clamped");
            check(s.get(DURATION) == DURATION.initial, "malformed number default");
            check(s.get(ANIMATIONS) && !s.get(SKIN), "invalid flag preserves default; valid flag loads");
            Files.writeString(file, "broken=\\uZZZZ\n");
            String before = Files.readString(file);
            try { new SettingsStore(file).load(); throw new AssertionError("expected read failure"); }
            catch (IOException expected) { check(before.equals(Files.readString(file)), "damaged original preserved"); }
            Path target = dir.resolve("directory"); Files.createDirectories(target); Files.writeString(target.resolve("keep"), "original");
            try { new SettingsStore(target).save(new MenuSettings()); throw new AssertionError("expected write failure"); }
            catch (IOException expected) { check(Files.readString(target.resolve("keep")).equals("original"), "failed write preserves data"); }
            try (var files = Files.list(dir)) { check(files.noneMatch(p -> p.toString().endsWith(".tmp")), "failed save temp cleanup"); }
        } finally { remove(dir); }
    }

    private static void interactions() {
        MenuSettings s = new MenuSettings(); MenuUi ui = menu(s); paint(ui);
        check(ui.mouseDown(260, 180), "switch click consumed");
        check(!s.get(TRANSPARENT) && ui.dirty(), "switch applies live");
        ui.mouseDown(350, 278); check(ui.dragging(), "slider captures mouse");
        ui.drag(-100); check(s.get(OPACITY) == OPACITY.min, "drag outside left clamps");
        ui.drag(2000); check(s.get(OPACITY) == OPACITY.max, "drag outside right clamps");
        ui.release(); ui.drag(-100); check(s.get(OPACITY) == OPACITY.max, "released slider stops tracking");
        ui.adjust(-1); check(s.get(OPACITY) < OPACITY.max, "keyboard adjustment");
        check(!ui.mouseDown(5000, -100), "outside click ignored");
        check(!ui.adjust(1), "outside click clears keyboard focus");
        s.set(ANIMATIONS, false); paint(ui);
    }

    private static void navigation() {
        MenuSettings s = new MenuSettings(); AtomicInteger saves = new AtomicInteger();
        MenuUi ui = new MenuUi(s, _ -> saves.incrementAndGet(), () -> {});
        paint(ui); ui.mouseDown(80, 338); check(ui.page() == 4, "preset navigation"); paint(ui);
        ui.mouseDown(440, 343); check(s.get(ACCENT_HUE) == 158, "Jade preset applies"); paint(ui);
        ui.mouseDown(750, 462); check(s.get(ACCENT_HUE) == 158, "reset requires second click"); paint(ui);
        ui.mouseDown(750, 462); check(s.get(ACCENT_HUE) == ACCENT_HUE.initial, "confirmed reset");
        paint(ui); ui.mouseDown(760, 40); check(saves.get() == 1, "save callback");
        ui.saved(); check(!ui.dirty(), "successful save clears dirty state");
        ui.mouseDown(80, 163); paint(ui); ui.focusNext(false); check(ui.activate(), "keyboard page activation");
        check(ui.page() == 0, "keyboard selected first page");
    }

    private static void pages() {
        MenuSettings s = new MenuSettings(); MenuUi ui = menu(s);
        String[] expected = {"Surface & profondeur", "Palette personnalisée", "Mouvements", "Votre compte", "Océan"};
        for (int page = 0; page < 5; page++) {
            paint(ui); ui.mouseDown(80, 160 + page * 44);
            SpyCanvas canvas = new SpyCanvas(); ui.paint(canvas, 0, 0, .016, "UnPseudoTrèsLongPourVérifier");
            check(ui.page() == page && canvas.texts.contains(expected[page]), "page " + page + " rendered");
            check(canvas.avatars == (page == 3 ? 2 : 1), "real skin slot on page " + page);
        }
        check(Colors.hsv(0, 1, 1) == 0xffff0000, "red HSV conversion");
        check(Colors.alpha(0xffffffff, 0) == 0xffffff, "zero alpha");
    }

    private static void transitions() {
        Transition t = new Transition(); double previous = 0;
        for (int i = 0; i < 20; i++) {
            double value = t.update(.016, 280, true); check(value >= previous && value <= 1, "monotone opening"); previous = value;
        }
        t.close(); check(t.update(.016, 280, true) < previous, "closing reverses opening");
        check(t.update(10, 280, true) == 0 && t.finished(), "long frame closes cleanly");
        Transition reduced = new Transition(); check(reduced.update(0, 280, false) == 1, "reduced motion opens instantly");
        reduced.close(); check(reduced.update(0, 280, false) == 0 && reduced.finished(), "reduced motion closes instantly");
    }

    private static MenuUi menu(MenuSettings s) { return new MenuUi(s, _ -> {}, () -> {}); }
    private static void paint(MenuUi ui) { ui.paint(new SpyCanvas(), 0, 0, .016, "Clax844"); }
    private static void check(boolean value, String message) { if (!value) throw new AssertionError(message); }
    private static void run(String name, Check test) throws Exception { test.run(); count++; System.out.println("PASS " + name); }
    private static void remove(Path dir) throws IOException {
        try (var paths = Files.walk(dir)) { for (Path p : paths.sorted(java.util.Comparator.reverseOrder()).toList()) Files.delete(p); }
    }
    @FunctionalInterface private interface Check { void run() throws Exception; }
    private static final class SpyCanvas implements Canvas {
        private final List<String> texts = new ArrayList<>(); private int avatars;
        public void fill(int x, int y, int w, int h, int color) { check(w >= 0 && h >= 0, "valid geometry"); }
        public void text(String text, int x, int y, int size, int color) { texts.add(text); }
        public int textWidth(String text, int size) { return (int) (text.length() * size * .52); }
        public void avatar(int x, int y, int w, int h) { avatars++; }
    }
}
