package cgs.modloader;

import cgs.modloader.api.*;
import ru.ius.david.cgs.MainMenuScreen;
import ru.ius.david.cgs.cgsGame;
import ru.ius.david.cgs.game.GameManager;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;

import java.awt.Desktop;
import java.io.*;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.*;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

public class ModLoader {
    private static final ModLoader INSTANCE = new ModLoader();
    public static final String MODS_DIR_NAME = "mods";
    public static final String CONFIG_FILE_NAME = "mods_config.json";

    private final File gameDir = new File(".");
    private final File modsDir = new File(MODS_DIR_NAME);
    private final List<ModContainer> loadedMods = new ArrayList<ModContainer>();
    private final Map<String, Boolean> modConfig = new HashMap<String, Boolean>();
    private cgsGame gameInstance;
    private boolean initialized = false;

    private float reloadToastLife = 0.0f;
    private String reloadToastMsg = "";

    public void reloadAll() {
        System.out.println("[ModLoader] F5 Pressed! Hot-reloading all mods, scripts and resources...");
        init();
        cgs.modloader.scripting.LuaModEngine.getInstance().reload();
        cgs.modloader.resources.ModResourceManager.getInstance().scanModAssets();
        reloadToastMsg = "⚡ [CGS ModLoader] Ресурсы, скрипты и моды перезагружены (F5)!";
        reloadToastLife = 4.0f;
        try {
            ru.ius.david.cgs.managers.SoundManager.getInstance().play(ru.ius.david.cgs.managers.SoundManager.SFX.BUTTON_CLICK);
        } catch(Throwable ignored) {}
    }


    public static ModLoader getInstance() {
        return INSTANCE;
    }

    private ModLoader() {}

    public synchronized void init() {
        if (initialized) return;
        initialized = true;

        System.out.println("[ModLoader] =======================================");
        System.out.println("[ModLoader] Initializing CGS ModLoader v1.0.0...");

        try {
            if (!modsDir.exists()) {
                modsDir.mkdirs();
            }
        } catch (Throwable ignored) {}

        try {
            loadConfig();
        } catch (Throwable ignored) {}

        try {
            discoverAndLoadMods();
        } catch (Throwable t) {
            System.err.println("[ModLoader] Error in discoverAndLoadMods: " + t.getMessage());
        }

        try {
            cgs.modloader.scripting.LuaModEngine.getInstance().reload();
        } catch (Throwable ignored) {}

        try {
            cgs.modloader.resources.ModResourceManager.getInstance().scanModAssets();
        } catch (Throwable ignored) {}

        System.out.println("[ModLoader] Total active mods loaded: " + loadedMods.size());
        System.out.println("[ModLoader] =======================================");
    }

    private void loadConfig() {
        File cfgFile = new File(modsDir, CONFIG_FILE_NAME);
        if (!cfgFile.exists()) return;
        try {
            String json = new String(Files.readAllBytes(cfgFile.toPath()), StandardCharsets.UTF_8);
            // simple JSON parser for key-value boolean pairs
            String[] lines = json.split("[\r\n]+");
            for (String l : lines) {
                l = l.trim();
                if (l.contains(":") && !l.startsWith("{") && !l.startsWith("}")) {
                    String[] parts = l.split(":", 2);
                    String key = parts[0].replace("\"", "").replace(" ", "").replace(",", "").trim();
                    String val = parts[1].replace("\"", "").replace(" ", "").replace(",", "").trim();
                    if (!key.isEmpty()) {
                        modConfig.put(key, Boolean.parseBoolean(val));
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("[ModLoader] Failed to read " + CONFIG_FILE_NAME + ": " + e.getMessage());
        }
    }

    public void saveConfig() {
        File cfgFile = new File(modsDir, CONFIG_FILE_NAME);
        try {
            StringBuilder sb = new StringBuilder("{\n");
            int i = 0;
            for (Map.Entry<String, Boolean> entry : modConfig.entrySet()) {
                sb.append("  \"").append(entry.getKey()).append("\": ").append(entry.getValue());
                if (++i < modConfig.size()) sb.append(",");
                sb.append("\n");
            }
            sb.append("}\n");
            Files.write(cfgFile.toPath(), sb.toString().getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            System.err.println("[ModLoader] Failed to write " + CONFIG_FILE_NAME + ": " + e.getMessage());
        }
    }

    private void discoverAndLoadMods() {
        loadedMods.clear();

        // 1. Builtin core mods (Multiplayer Mod!)
        loadBuiltinMod(
            new ModMetadata(
                "cgs.multiplayer",
                "Сетевая Игра (Multiplayer Mod)",
                "1.2.0",
                "Galileo AI & AoC Devs",
                "Полноценный сетевой мультиплеер: LAN, Direct IP подключение, Steam P2P лобби до 8 игроков, синхронизация карты и действий в реальном времени.",
                "cgs.mods.multiplayer.MultiplayerMod"
            )
        );

        // 2. Discover external mods in mods/ folder
        File[] files = modsDir.listFiles();
        if (files == null) return;

        for (File f : files) {
            if (f.getName().equalsIgnoreCase(CONFIG_FILE_NAME)) continue;

            if (f.isDirectory()) {
                File manifest = new File(f, "mod.json");
                if (manifest.exists()) {
                    loadDirectoryMod(f, manifest);
                }
            } else if (f.getName().toLowerCase().endsWith(".jar")) {
                loadJarMod(f);
            }
        }
    }

    private void loadBuiltinMod(ModMetadata meta) {
        meta.builtin = true;
        boolean enabled = modConfig.getOrDefault(meta.id, true);
        meta.enabled = enabled;
        modConfig.put(meta.id, enabled);

        if (!enabled) {
            System.out.println("[ModLoader] Built-in mod disabled in config: " + meta.id);
            return;
        }

        try {
            Class<?> clazz = Class.forName(meta.entrypoint);
            IMod instance = (IMod) clazz.getDeclaredConstructor().newInstance();
            ModContext ctx = new ModContext(meta, new File(modsDir, "multiplayer"), gameDir);
            instance.onInitialize(ctx);
            loadedMods.add(new ModContainer(meta, new File(modsDir, "multiplayer"), instance, ctx));
            System.out.println("[ModLoader] Successfully initialized built-in mod: " + meta.name + " v" + meta.version);
        } catch (Throwable t) {
            System.err.println("[ModLoader] Failed to initialize built-in mod " + meta.id + ": " + t.getMessage());
            t.printStackTrace();
        }
    }

    private void loadDirectoryMod(File dir, File manifest) {
        try {
            ModMetadata meta = parseManifest(new String(Files.readAllBytes(manifest.toPath()), StandardCharsets.UTF_8));
            if (meta == null || meta.id == null || meta.id.isEmpty()) return;
            if (isModLoaded(meta.id)) return;

            boolean enabled = modConfig.getOrDefault(meta.id, meta.enabled);
            meta.enabled = enabled;
            modConfig.put(meta.id, enabled);

            if (!enabled) {
                System.out.println("[ModLoader] Mod disabled: " + meta.id);
                return;
            }

            IMod instance = null;
            ModContext ctx = new ModContext(meta, dir, gameDir);
            if (meta.entrypoint != null && !meta.entrypoint.trim().isEmpty()) {
                List<URL> urls = new ArrayList<URL>();
                urls.add(dir.toURI().toURL());
                File[] jars = dir.listFiles(new FilenameFilter() {
                    public boolean accept(File d, String name) {
                        return name.toLowerCase().endsWith(".jar");
                    }
                });
                if (jars != null) {
                    for (File jf : jars) {
                        urls.add(jf.toURI().toURL());
                    }
                }
                URLClassLoader loader = new URLClassLoader(urls.toArray(new URL[0]), getClass().getClassLoader());
                Class<?> clazz = Class.forName(meta.entrypoint, true, loader);
                instance = (IMod) clazz.getDeclaredConstructor().newInstance();
                instance.onInitialize(ctx);
            }
            loadedMods.add(new ModContainer(meta, dir, instance, ctx));
            System.out.println("[ModLoader] Loaded directory mod: " + meta.name + " (" + meta.id + ")");
        } catch (Throwable t) {
            System.err.println("[ModLoader] Failed to load directory mod from " + dir.getName() + ": " + t.getMessage());
            t.printStackTrace();
        }
    }

    private void loadJarMod(File jarFile) {
        try (JarFile jar = new JarFile(jarFile)) {
            JarEntry entry = jar.getJarEntry("mod.json");
            if (entry == null) entry = jar.getJarEntry("cgs-mod.json");
            if (entry == null) return;

            InputStream is = jar.getInputStream(entry);
            byte[] bytes = is.readAllBytes();
            ModMetadata meta = parseManifest(new String(bytes, StandardCharsets.UTF_8));
            if (meta == null || meta.id == null || meta.id.isEmpty()) return;
            if (isModLoaded(meta.id)) return;

            boolean enabled = modConfig.getOrDefault(meta.id, meta.enabled);
            meta.enabled = enabled;
            modConfig.put(meta.id, enabled);

            if (!enabled) {
                System.out.println("[ModLoader] Mod disabled: " + meta.id);
                return;
            }

            IMod instance = null;
            ModContext ctx = new ModContext(meta, jarFile, gameDir);
            if (meta.entrypoint != null && !meta.entrypoint.trim().isEmpty()) {
                URL url = jarFile.toURI().toURL();
                URLClassLoader loader = new URLClassLoader(new URL[]{url}, getClass().getClassLoader());
                Class<?> clazz = Class.forName(meta.entrypoint, true, loader);
                instance = (IMod) clazz.getDeclaredConstructor().newInstance();
                instance.onInitialize(ctx);
            }
            loadedMods.add(new ModContainer(meta, jarFile, instance, ctx));
            System.out.println("[ModLoader] Loaded jar mod: " + meta.name + " (" + meta.id + ")");
        } catch (Throwable t) {
            System.err.println("[ModLoader] Failed to load jar mod " + jarFile.getName() + ": " + t.getMessage());
            t.printStackTrace();
        }
    }

    private ModMetadata parseManifest(String json) {
        ModMetadata meta = new ModMetadata();
        String[] lines = json.split("[\r\n]+");
        for (String line : lines) {
            line = line.trim();
            if (line.contains(":") && !line.startsWith("{") && !line.startsWith("}")) {
                String[] parts = line.split(":", 2);
                String k = parts[0].replace("\"", "").replace(" ", "").replace(",", "").trim().toLowerCase();
                String v = parts[1].trim();
                if (v.endsWith(",")) v = v.substring(0, v.length() - 1).trim();
                if (v.startsWith("\"") && v.endsWith("\"") && v.length() >= 2) {
                    v = v.substring(1, v.length() - 1);
                }
                if (k.equals("id")) meta.id = v;
                else if (k.equals("name")) meta.name = v;
                else if (k.equals("version")) meta.version = v;
                else if (k.equals("author")) meta.author = v;
                else if (k.equals("description")) meta.description = v;
                else if (k.equals("entrypoint")) meta.entrypoint = v;
                else if (k.equals("icon")) meta.icon = v;
                else if (k.equals("enabled")) meta.enabled = Boolean.parseBoolean(v);
            }
        }
        return meta;
    }

    public boolean isModLoaded(String id) {
        if (id == null) return false;
        for (ModContainer mc : loadedMods) {
            if (id.equals(mc.metadata.id)) return true;
        }
        return false;
    }

    public void setModEnabled(String modId, boolean enabled) {
        modConfig.put(modId, enabled);
        saveConfig();
        for (ModContainer mc : loadedMods) {
            if (mc.metadata.id.equals(modId)) {
                mc.metadata.enabled = enabled;
            }
        }
    }

    public boolean isModEnabled(String modId) {
        return modConfig.getOrDefault(modId, true);
    }

    public List<ModContainer> getLoadedMods() {
        return Collections.unmodifiableList(loadedMods);
    }

    public void openModsFolder() {
        try {
            if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.OPEN)) {
                Desktop.getDesktop().open(modsDir);
            } else {
                new ProcessBuilder("explorer.exe", modsDir.getAbsolutePath()).start();
            }
        } catch (Exception e) {
            System.err.println("[ModLoader] Cannot open mods folder: " + e.getMessage());
        }
    }

    public void onGameCreated(cgsGame game) {
        this.gameInstance = game;
        init();
        for (ModContainer mc : loadedMods) {
            if (mc.metadata.enabled && mc.instance != null) {
                try {
                    mc.instance.onGameCreated(game);
                } catch (Throwable t) {
                    System.err.println("[ModLoader] Error in onGameCreated for " + mc.metadata.id + ": " + t.getMessage());
                }
            }
        }
    }

    public void onMainMenuBuild(MainMenuScreen screen, ModMenuHelper helper) {
        init();
        for (ModContainer mc : loadedMods) {
            if (mc.metadata.enabled && mc.instance != null) {
                try {
                    mc.instance.onMainMenuBuild(screen, helper);
                } catch (Throwable t) {
                    System.err.println("[ModLoader] Error in onMainMenuBuild for " + mc.metadata.id + ": " + t.getMessage());
                }
            }
        }
    }

    public void onGameTick(GameManager gm) {

        cgs.modloader.scripting.LuaModEngine.getInstance().onGameTick(gm);
        cgs.modloader.timeline.TimelineRecorder.getInstance().update(com.badlogic.gdx.Gdx.graphics.getDeltaTime(), gm);

        for (ModContainer mc : loadedMods) {
            if (mc.metadata.enabled && mc.instance != null) {
                try {
                    mc.instance.onGameTick(gm);
                } catch (Throwable t) {
                    // silently avoid console spam
                }
            }
        }
    }

    public void onRender(SpriteBatch batch, float delta) {

        if (com.badlogic.gdx.Gdx.input.isKeyJustPressed(com.badlogic.gdx.Input.Keys.F5)) {
            reloadAll();
        }
        if (reloadToastLife > 0.0f) {
            reloadToastLife -= delta;
            if (ru.ius.david.cgs.CFG.fontSmall != null && batch.isDrawing()) {
                int toastW = ru.ius.david.cgs.CFG.s(450);
                int toastH = ru.ius.david.cgs.CFG.s(32);
                int tx = (ru.ius.david.cgs.CFG.GAME_WIDTH - toastW) / 2;
                int ty = ru.ius.david.cgs.CFG.s(16);
                batch.setColor(0.05f, 0.1f, 0.2f, 0.95f);
                ru.ius.david.cgs.images.ImageManager.getImage(ru.ius.david.cgs.images.Images.pix255_255_255).draw(batch, tx, ty, toastW, toastH);
                batch.setColor(0.2f, 0.8f, 1.0f, 1.0f);
                ru.ius.david.cgs.images.ImageManager.getImage(ru.ius.david.cgs.images.Images.pix255_255_255).draw(batch, tx, ty, toastW, 2);
                ru.ius.david.cgs.CFG.drawTextSmall(batch, reloadToastMsg, tx + ru.ius.david.cgs.CFG.s(12), ty + ru.ius.david.cgs.CFG.s(22), com.badlogic.gdx.graphics.Color.WHITE);
                batch.setColor(com.badlogic.gdx.graphics.Color.WHITE);
            }
        }

        cgs.modloader.MultiplayerTestDriver.getInstance().update(this.gameInstance, delta);
    }

    public void onPostRender(SpriteBatch batch, float delta) {
        ru.ius.david.cgs.MapScreen mapScreen = (this.gameInstance != null && this.gameInstance.getScreen() instanceof ru.ius.david.cgs.MapScreen) ? (ru.ius.david.cgs.MapScreen) this.gameInstance.getScreen() : null;

        // 1. Spectator HUD & controls
        if (mapScreen != null) {
            cgs.modloader.spectator.SpectatorManager.getInstance().onMapScreenFrame(mapScreen, this.gameInstance, batch, delta);
        }

        // 2. Nuclear Warning System DEFCON 1
        if (mapScreen != null) {
            cgs.modloader.nuclear.NuclearAlertOverlay.getInstance().updateAndRender(
                batch, null, ru.ius.david.cgs.CFG.fontMain, delta, ru.ius.david.cgs.CFG.GAME_WIDTH, ru.ius.david.cgs.CFG.GAME_HEIGHT, mapScreen
            );
        }

        // 3. F6 Quick Match Save
        if (com.badlogic.gdx.Gdx.input.isKeyJustPressed(com.badlogic.gdx.Input.Keys.F6)) {
            ru.ius.david.cgs.multiplayer.MultiplayerManager mpMgr = this.gameInstance != null ? this.gameInstance.getActiveMultiplayerManager() : null;
            cgs.modloader.save.MultiplayerSaveManager.getInstance().saveMatch(this.gameInstance, mpMgr);
        }

        // 4. F7 Direct Aid / Lend-Lease Dialog toggle (gameplay only)
        if (mapScreen != null && com.badlogic.gdx.Gdx.input.isKeyJustPressed(com.badlogic.gdx.Input.Keys.F7)) {
            cgs.modloader.trade.DirectAidDialog.getInstance().toggle();
        }

        // 5. Direct Aid Dialog render & click handling (gameplay only)
        if (mapScreen != null && !mapScreen.isCountrySelectMode() && cgs.modloader.trade.DirectAidDialog.getInstance().isVisible()) {
            ru.ius.david.cgs.game.GameManager gm = ru.ius.david.cgs.game.GameManager.getInstance();
            ru.ius.david.cgs.multiplayer.MultiplayerManager mpMgr = this.gameInstance != null ? this.gameInstance.getActiveMultiplayerManager() : null;
            if (com.badlogic.gdx.Gdx.input.isButtonJustPressed(com.badlogic.gdx.Input.Buttons.LEFT)) {
                float mx = com.badlogic.gdx.Gdx.input.getX();
                float my = com.badlogic.gdx.Gdx.input.getY();
                cgs.modloader.trade.DirectAidDialog.getInstance().handleClick(mx, my, ru.ius.david.cgs.CFG.GAME_WIDTH, ru.ius.david.cgs.CFG.GAME_HEIGHT, gm, mpMgr);
            }
            cgs.modloader.trade.DirectAidDialog.getInstance().render(
                batch, null, ru.ius.david.cgs.CFG.fontMain, ru.ius.david.cgs.CFG.GAME_WIDTH, ru.ius.david.cgs.CFG.GAME_HEIGHT, gm
            );
        }

        // 6. Overlay rendering from all loaded mods (chat, network badge, etc.)
        for (ModContainer mc : loadedMods) {
            if (mc.metadata.enabled && mc.instance != null) {
                try {
                    mc.instance.onRenderOverlay(batch, delta);
                } catch (Throwable t) {
                    // silently avoid console spam
                }
            }
        }
    }

    public cgsGame getGame() {
        return gameInstance;
    }
}
