package cgs.modloader.resources;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import java.io.File;
import java.util.HashMap;
import java.util.Map;
import cgs.modloader.ModLoader;

public class ModResourceManager {
    private static final ModResourceManager INSTANCE = new ModResourceManager();
    public static ModResourceManager getInstance() { return INSTANCE; }

    private final Map<String, File> assetOverrides = new HashMap<String, File>();

    private ModResourceManager() {
        scanModAssets();
    }

    public void scanModAssets() {
        assetOverrides.clear();
        File modsDir = new File(ModLoader.MODS_DIR_NAME);
        if (!modsDir.exists()) return;

        File[] subdirs = modsDir.listFiles();
        if (subdirs == null) return;

        for (File dir : subdirs) {
            if (dir.isDirectory()) {
                File assetsDir = new File(dir, "assets");
                if (assetsDir.exists() && assetsDir.isDirectory()) {
                    scanRecursive(assetsDir, "");
                }
            }
        }
        if (!assetOverrides.isEmpty()) {
            System.out.println("[ModResourceManager] Total modded asset overrides active: " + assetOverrides.size());
        }
    }

    private void scanRecursive(File dir, String relPath) {
        File[] files = dir.listFiles();
        if (files == null) return;
        for (File f : files) {
            String subRel = relPath.isEmpty() ? f.getName() : (relPath + "/" + f.getName());
            if (f.isDirectory()) {
                scanRecursive(f, subRel);
            } else {
                assetOverrides.put(subRel.toLowerCase(), f);
            }
        }
    }

    public FileHandle resolveOverride(String originalPath) {
        if (originalPath == null) return null;
        String key = originalPath.replace('\\', '/').toLowerCase();
        File override = assetOverrides.get(key);
        if (override != null && override.exists()) {
            return Gdx.files.absolute(override.getAbsolutePath());
        }
        return null;
    }
}
