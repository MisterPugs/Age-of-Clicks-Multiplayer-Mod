package cgs.modloader.api;

import java.io.File;

public class ModContext {
    private final ModMetadata metadata;
    private final File modDirectory;
    private final File gameDirectory;

    public ModContext(ModMetadata metadata, File modDirectory, File gameDirectory) {
        this.metadata = metadata;
        this.modDirectory = modDirectory;
        this.gameDirectory = gameDirectory;
    }

    public ModMetadata getMetadata() {
        return metadata;
    }

    public File getModDirectory() {
        return modDirectory;
    }

    public File getGameDirectory() {
        return gameDirectory;
    }

    public void log(String msg) {
        System.out.println("[Mod: " + (metadata != null ? metadata.name : "Unknown") + "] " + msg);
    }

    public void logError(String msg, Throwable t) {
        System.err.println("[Mod: " + (metadata != null ? metadata.name : "Unknown") + "] ERROR: " + msg);
        if (t != null) {
            t.printStackTrace();
        }
    }
}
