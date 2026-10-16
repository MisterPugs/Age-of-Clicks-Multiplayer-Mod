package cgs.modloader;

public class ModLauncher {
    public static void main(String[] args) {
        System.out.println("==================================================");
        System.out.println("  ⚡ AGE OF CLICKS - CGS MODLOADER v1.0.0");
        System.out.println("==================================================");
        try {
            ModLoader.getInstance().init();
        } catch (Throwable t) {
            System.err.println("[ModLauncher] ModLoader pre-init failed: " + t.getMessage());
            t.printStackTrace();
        }
        ru.ius.david.cgs.DesktopLauncher.main(args);
    }
}
