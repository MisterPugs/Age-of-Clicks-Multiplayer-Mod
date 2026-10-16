package ru.ius.david.cgs;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Graphics;
import com.badlogic.gdx.backends.lwjgl3.DefaultLwjgl3Input;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Input;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Window;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3WindowListener;
import com.badlogic.gdx.backends.lwjgl3.audio.Lwjgl3Audio;
import com.badlogic.gdx.utils.GdxRuntimeException;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Optional;
import java.util.Properties;
import java.util.concurrent.TimeUnit;
import org.lwjgl.glfw.GLFW;
import ru.ius.david.cgs.DesktopFileChooser;
import ru.ius.david.cgs.DesktopGpuTimer;
import ru.ius.david.cgs.DesktopHeapChoice;
import ru.ius.david.cgs.DesktopWindowService;
import ru.ius.david.cgs.ThreadedOpenALAudio;
import ru.ius.david.cgs.cgsGame;
import ru.ius.david.cgs.game.SettingsManager;
import ru.ius.david.cgs.managers.MusicManager;
import ru.ius.david.cgs.platform.NoOpSecurityService;
import ru.ius.david.cgs.steam.SteamManager;
import ru.ius.david.cgs.utils.FrameStats;
import ru.ius.david.cgs.utils.PrefsWriter;

public class DesktopLauncher {
    private static final int DEFAULT_STEAM_APP_ID = 4804480;
    private static final long LEGACY_PREFS_END = LocalDate.of(2026, 6, 17).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli();
    private static final int LOG_BUFFER_BYTES = 65536;
    private static final int AUDIO_SOURCES = 16;
    private static final int AUDIO_BUFFER_SIZE = 512;
    private static final int AUDIO_BUFFER_COUNT = 9;
    private static final long LOG_FLUSH_INTERVAL_MS = 200L;
    private static volatile PrintStream logSink;
    private static final int[][] STANDARD_RESOLUTIONS = new int[][]{
        {3840, 2160}, {2560, 1440}, {2560, 1080}, {1920, 1200}, {1920, 1080},
        {1680, 1050}, {1600, 900}, {1440, 900}, {1366, 768}, {1280, 1024},
        {1280, 800}, {1280, 720}, {1152, 864}, {1024, 768}, {800, 600}
    };

    static void flushLog() {
        PrintStream sink = logSink;
        if (sink != null) {
            sink.flush();
        }
    }

    static void reportLoopFailure(Throwable t) {
        Thread.UncaughtExceptionHandler handler = Thread.getDefaultUncaughtExceptionHandler();
        if (handler == null) {
            if (t instanceof RuntimeException) {
                throw (RuntimeException)t;
            }
            if (t instanceof Error) {
                throw (Error)t;
            }
            throw new GdxRuntimeException(t);
        }
        handler.uncaughtException(Thread.currentThread(), t);
    }

    private static void startLogFlusher(final PrintStream sink) {
        Thread flusher = new Thread(new Runnable(){
            @Override
            public void run() {
                while (true) {
                    try {
                        Thread.sleep(200L);
                    }
                    catch (InterruptedException e) {
                        sink.flush();
                        return;
                    }
                    sink.flush();
                }
            }
        }, "LogFlusher");
        flusher.setDaemon(true);
        flusher.start();
    }

    private static int resolveSteamAppId() {
        String[] candidates = new String[]{System.getenv("CGS_STEAM_APPID"), System.getProperty("cgs.steam.appid")};
        for (String c : candidates) {
            if (c == null || c.trim().isEmpty()) continue;
            try {
                int id = Integer.parseInt(c.trim());
                System.out.println("[Launcher] Steam AppID overridden to " + id + " via config.");
                return id;
            }
            catch (NumberFormatException ignore) {
                System.err.println("[Launcher] Ignoring invalid Steam AppID override: " + c);
            }
        }
        String launchedAs = System.getenv("SteamAppId");
        if (launchedAs != null && !launchedAs.trim().isEmpty()) {
            try {
                int id = Integer.parseInt(launchedAs.trim());
                if (id > 0) {
                    System.out.println("[Launcher] Launched by Steam as AppID " + id + ".");
                    return id;
                }
            }
            catch (NumberFormatException ignore) {
                System.err.println("[Launcher] Ignoring invalid SteamAppId from Steam: " + launchedAs);
            }
        }
        try {
            File f = new File("steam_appid.txt");
            if (f.exists()) {
                String txt = new String(Files.readAllBytes(f.toPath())).trim();
                if (!txt.isEmpty()) {
                    return Integer.parseInt(txt);
                }
            }
        }
        catch (Exception exception) {}
        return 4804480;
    }

    public static void main(String[] arg) {
        if (DesktopHeapChoice.relaunchWithRightHeap(arg)) {
            return;
        }
        DesktopLauncher.killStaleInstances();
        FrameStats.gpuTimer = new DesktopGpuTimer();
        
        // Only redirect stdout/stderr if cgs.dontRedirectLog is not true
        if (!Boolean.getBoolean("cgs.dontRedirectLog")) {
            try {
                PrintStream logOut = new PrintStream(new BufferedOutputStream(new FileOutputStream("cgs_log.txt", false), 65536), false);
                logSink = logOut;
                DesktopLauncher.startLogFlusher(logOut);
                System.setOut(logOut);
                System.setErr(logOut);
            }
            catch (Exception e) {
                e.printStackTrace();
            }
        }

        long machineMemory = DesktopHeapChoice.physicalMemoryBytes();
        long autoHeap = DesktopHeapChoice.autoHeapBytes();
        if (DesktopHeapChoice.note != null) {
            System.out.println(DesktopHeapChoice.note);
        }
        System.out.println(DesktopHeapChoice.describe(machineMemory, autoHeap));
        boolean steamAvailable = SteamManager.init(DesktopLauncher.resolveSteamAppId());
        if (steamAvailable) {
            System.out.println("[Launcher] Steam initialized. Player: " + SteamManager.getLocalSteamName());
        } else {
            System.out.println("[Launcher] Steam not available — LAN only.");
        }
        Lwjgl3ApplicationConfiguration config = new Lwjgl3ApplicationConfiguration();
        config.setPreferencesConfig(".", com.badlogic.gdx.Files.FileType.Local);
        config.setAutoIconify(false);
        config.setTitle("Age of Clicks");
        config.setInitialVisible(true);
        config.setAudioConfig(16, 512, 9);
        config.setWindowIcon("icon_128x128.png", "icon_32x32.png", "icon_16x16.png");
        int windowMode = 1;
        int resolutionIndex = 0;
        int orientationMode = 0;
        int monitorIndex = 0;
        boolean vsyncEnabled = true;
        boolean mapMsaa = false;
        boolean hasMonitorPref = false;
        try {
            File prefsFile = new File("CGS_Settings");
            if (!prefsFile.exists()) {
                String oldPrefsPath = System.getProperty("user.home") + File.separator + ".prefs" + File.separator + "CGS_Settings";
                File oldPrefsFile = new File(oldPrefsPath);
                if (oldPrefsFile.exists() && oldPrefsFile.lastModified() >= LEGACY_PREFS_END) {
                    System.out.println("[Launcher] Not copying " + oldPrefsPath + ": written after settings left user.home, so not by an install of the game.");
                } else if (oldPrefsFile.exists()) {
                    try {
                        Files.copy(oldPrefsFile.toPath(), prefsFile.toPath());
                        System.out.println("[Launcher] Copied old settings from user.home to local directory.");
                    }
                    catch (Exception exception) {}
                }
            }
            if (prefsFile.exists()) {
                Properties props = new Properties();
                FileInputStream fis = new FileInputStream(prefsFile);
                props.loadFromXML(fis);
                fis.close();
                String wmStr = props.getProperty("windowMode");
                if (wmStr != null) {
                    windowMode = Integer.parseInt(wmStr);
                }
                String riStr = props.getProperty("resolutionIndex");
                if (riStr != null) {
                    resolutionIndex = Integer.parseInt(riStr);
                }
                String omStr = props.getProperty("orientationMode");
                if (omStr != null) {
                    orientationMode = Integer.parseInt(omStr);
                } else {
                    String pmStr = props.getProperty("portraitMode");
                    if (pmStr != null) {
                        orientationMode = Boolean.parseBoolean(pmStr) ? 1 : 0;
                    }
                }
                String miStr = props.getProperty("monitorIndex");
                if (miStr != null) {
                    monitorIndex = Integer.parseInt(miStr);
                    hasMonitorPref = true;
                }
                String vsyncStr = props.getProperty("vsync");
                if (vsyncStr != null) {
                    vsyncEnabled = Boolean.parseBoolean(vsyncStr);
                }
                String msaaStr = props.getProperty("mapMsaa");
                if (msaaStr != null) {
                    mapMsaa = Boolean.parseBoolean(msaaStr.trim());
                }
            }
        }
        catch (Exception prefsFile) {}
        
        config.useVsync(vsyncEnabled);
        if (windowMode < 0 || windowMode > 2) {
            windowMode = 1;
        }
        Graphics.Monitor[] monitors = Lwjgl3ApplicationConfiguration.getMonitors();
        Graphics.Monitor targetMonitor = null;
        if (hasMonitorPref && monitorIndex >= 0 && monitorIndex < monitors.length) {
            targetMonitor = monitors[monitorIndex];
        } else {
            for (Graphics.Monitor m : monitors) {
                if (m.virtualX != 0 || m.virtualY != 0) continue;
                targetMonitor = m;
                break;
            }
            if (targetMonitor == null && monitors.length > 0) {
                targetMonitor = monitors[0];
            }
        }
        int nativeW = 1920;
        int nativeH = 1080;
        Graphics.DisplayMode dm;
        if (targetMonitor != null && (dm = Lwjgl3ApplicationConfiguration.getDisplayMode(targetMonitor)) != null) {
            nativeW = dm.width;
            nativeH = dm.height;
        }
        ArrayList<int[]> availableResolutions = new ArrayList<int[]>();
        availableResolutions.add(new int[]{nativeW, nativeH});
        LinkedHashSet<String> addedKeys = new LinkedHashSet<String>();
        addedKeys.add(nativeW + "x" + nativeH);
        boolean isMonitorPortrait = nativeW < nativeH;
        for (int[] res : STANDARD_RESOLUTIONS) {
            int w = isMonitorPortrait ? Math.min(res[0], res[1]) : res[0];
            int h = isMonitorPortrait ? Math.max(res[0], res[1]) : res[1];
            String key = w + "x" + h;
            if (w > nativeW || h > nativeH || addedKeys.contains(key)) continue;
            addedKeys.add(key);
            availableResolutions.add(new int[]{w, h});
        }
        int targetW;
        int targetH;
        if (resolutionIndex >= 0 && resolutionIndex < availableResolutions.size()) {
            targetW = availableResolutions.get(resolutionIndex)[0];
            targetH = availableResolutions.get(resolutionIndex)[1];
        } else {
            targetW = nativeW;
            targetH = nativeH;
        }
        if (orientationMode == 1) {
            int minDim = Math.min(targetW, targetH);
            int maxDim = Math.max(targetW, targetH);
            targetW = minDim;
            targetH = maxDim;
        }
        Graphics.Monitor windowedMonitor = windowMode == 0 ? targetMonitor : null;
        int windowedW = targetW;
        int windowedH = targetH;
        boolean windowedAuto = resolutionIndex <= 0 || resolutionIndex >= availableResolutions.size();
        if ((windowMode == 1 || windowMode == 2) && targetMonitor != null) {
            config.setDecorated(false);
            int coverH = SettingsManager.coverWindowHeight(windowMode, nativeH, isMonitorPortrait);
            config.setWindowedMode(nativeW, coverH);
            config.setWindowPosition(targetMonitor.virtualX, targetMonitor.virtualY);
            System.out.println("[Launcher] " + (windowMode == 1 ? "Fullscreen" : "Borderless") + " window: " + nativeW + "x" + coverH + " over a " + nativeW + "x" + nativeH + " monitor");
        } else {
            config.setDecorated(true);
            config.setWindowedMode(windowedW, windowedH);
        }
        final cgsGame gameInstance = new cgsGame(new DesktopFileChooser());
        gameInstance.setSecurityService(new NoOpSecurityService());
        SettingsManager.setWindowService(new DesktopWindowService());
        SettingsManager.setMachineMemory(machineMemory, autoHeap);
        Runtime.getRuntime().addShutdownHook(new Thread(new Runnable(){
            @Override
            public void run() {
                try {
                    MusicManager.getInstance().stopAll();
                }
                catch (Throwable throwable) {}
                try {
                    PrefsWriter.drain();
                }
                catch (Throwable throwable) {}
                DesktopLauncher.flushLog();
                SteamManager.shutdown();
                DesktopLauncher.flushLog();
                Runtime.getRuntime().halt(0);
            }
        }, "Shutdown-Failsafe"));
        
        final Graphics.Monitor winMonitor = windowedMonitor;
        final int winW = windowedW;
        final int winH = windowedH;
        final boolean winAuto = windowedAuto;
        config.setWindowListener(new Lwjgl3WindowListener(){
            @Override
            public void iconified(boolean isIconified) {
                if (isIconified) {
                    gameInstance.pause();
                } else {
                    Gdx.app.postRunnable(new Runnable(){
                        @Override
                        public void run() {
                            gameInstance.resume();
                        }
                    });
                }
            }

            @Override
            public void focusLost() {
                cgsGame.setWindowFocused(false);
            }

            @Override
            public void focusGained() {
                cgsGame.setWindowFocused(true);
            }

            @Override
            public void created(Lwjgl3Window window) {
                if (window != null) {
                    if (winMonitor != null) {
                        try {
                            DesktopWindowService.place(window, winMonitor, winW, winH, winAuto);
                        }
                        catch (Throwable t) {
                            System.out.println("[Launcher] Could not fit the window into the work area: " + t);
                        }
                    }
                    try {
                        long handle = window.getWindowHandle();
                        GLFW.glfwShowWindow(handle);
                        GLFW.glfwFocusWindow(handle);
                        GLFW.glfwRequestWindowAttention(handle);
                    }
                    catch (Throwable throwable) {}
                }
            }

            @Override
            public void maximized(boolean isMaximized) {}

            @Override
            public void refreshRequested() {}

            @Override
            public boolean closeRequested() {
                try {
                    MusicManager.getInstance().stopAll();
                }
                catch (Throwable throwable) {}
                try {
                    SettingsManager.getInstance().save();
                }
                catch (Throwable throwable) {}
                try {
                    SteamManager.shutdown();
                }
                catch (Throwable throwable) {}
                try {
                    gameInstance.dispose();
                }
                catch (Throwable throwable) {}
                try {
                    Gdx.audio.dispose();
                }
                catch (Throwable throwable) {}
                try {
                    new File("cgs.pid").delete();
                }
                catch (Throwable throwable) {}
                DesktopLauncher.flushLog();
                Runtime.getRuntime().halt(0);
                return true;
            }

            @Override
            public void filesDropped(String[] files) {}
        });

        boolean launched = false;
        int attempt = mapMsaa ? -1 : 0;
        while (attempt < 3 && !launched) {
            if (attempt == -1) {
                config.setBackBufferConfig(8, 8, 8, 8, 16, 8, 4);
                System.out.println("[Launcher] Launching with stencil=8 depth=16 msaa=4 (edge smoothing)...");
            } else if (attempt == 0) {
                config.setBackBufferConfig(8, 8, 8, 8, 16, 8, 0);
                System.out.println("[Launcher] Launching with stencil=8 depth=16...");
            } else if (attempt == 1) {
                config.setBackBufferConfig(8, 8, 8, 8, 16, 0, 0);
                System.out.println("[Launcher] Retrying without stencil buffer (stencil=0 depth=16)...");
            } else {
                config.setBackBufferConfig(8, 8, 8, 8, 0, 0, 0);
                System.out.println("[Launcher] Retrying with minimal backbuffer config...");
            }
            try {
                new Lwjgl3Application(gameInstance, config){
                    @Override
                    public Lwjgl3Audio createAudio(Lwjgl3ApplicationConfiguration cfg) {
                        if (!ThreadedOpenALAudio.enabled()) {
                            return super.createAudio(cfg);
                        }
                        return new ThreadedOpenALAudio(16, 9, 512);
                    }

                    @Override
                    public void postRunnable(final Runnable runnable) {
                        super.postRunnable(new Runnable(){
                            @Override
                            public void run() {
                                try {
                                    runnable.run();
                                }
                                catch (Throwable t) {
                                    DesktopLauncher.reportLoopFailure(t);
                                }
                            }
                        });
                    }

                    @Override
                    public Lwjgl3Input createInput(Lwjgl3Window window) {
                        return new DefaultLwjgl3Input(window){
                            @Override
                            public void update() {
                                try {
                                    super.update();
                                }
                                catch (Throwable t) {
                                    DesktopLauncher.reportLoopFailure(t);
                                }
                            }
                        };
                    }
                };
                launched = true;
                SteamManager.shutdown();
                try {
                    new File("cgs.pid").delete();
                }
                catch (Throwable throwable) {}
                DesktopLauncher.flushLog();
                Runtime.getRuntime().halt(0);
            }
            catch (GdxRuntimeException e) {
                if (attempt < 2) {
                    System.err.println("[Launcher] Window creation failed (attempt " + (attempt + 1) + "), will retry: " + e.getMessage());
                }
                System.err.println("[Launcher] All launch attempts failed. GPU may not support OpenGL 3.2.");
                throw e;
            }
            ++attempt;
        }
    }

    private static void killStaleInstances() {
        if (Boolean.getBoolean("cgs.allowMultiple") || System.getenv("CGS_ALLOW_MULTIPLE") != null) {
            System.out.println("[Launcher] cgs.allowMultiple is enabled. Skipping stale instance killer.");
            return;
        }
        try {
            long myPid = ProcessHandle.current().pid();
            File pidFile = new File("cgs.pid");
            if (pidFile.exists()) {
                try {
                    String content = new String(Files.readAllBytes(pidFile.toPath()), StandardCharsets.UTF_8).trim();
                    if (!content.isEmpty()) {
                        long oldPid = Long.parseLong(content);
                        if (oldPid != myPid) {
                            Optional<ProcessHandle> opt = ProcessHandle.of(oldPid);
                            Optional<Instant> started = opt.isPresent() ? opt.get().info().startInstant() : Optional.empty();
                            boolean wroteIt = started.isPresent() && started.get().toEpochMilli() <= pidFile.lastModified() + 2000L;
                            if (opt.isPresent() && opt.get().isAlive() && !wroteIt) {
                                System.out.println("[Launcher] PID " + oldPid + " from cgs.pid is another process now - left alone.");
                            } else if (opt.isPresent() && opt.get().isAlive()) {
                                System.out.println("[Launcher] Terminating previous stale CGS process (PID " + oldPid + ") from cgs.pid...");
                                opt.get().destroyForcibly();
                                try {
                                    opt.get().onExit().get(2L, TimeUnit.SECONDS);
                                }
                                catch (Exception exception) {}
                            }
                        }
                    }
                }
                catch (Exception e) {
                    System.err.println("[Launcher] Warning reading cgs.pid: " + e.getMessage());
                }
            }
            long parentPid = ProcessHandle.current().parent().map(ProcessHandle::pid).orElse(-1L);
            try {
                ProcessHandle.allProcesses().forEach(ph -> {
                    if (ph.pid() == myPid || ph.pid() == parentPid) {
                        return;
                    }
                    try {
                        ph.info().commandLine().ifPresent(cmd -> {
                            if (!cmd.contains("ru.ius.david.cgs.DesktopLauncher") && !cmd.contains("ru.ius.david.cgs.desktop.DesktopLauncher")) {
                                return;
                            }
                            if (DesktopLauncher.isBuildToolCommandLine(cmd)) {
                                return;
                            }
                            System.out.println("[Launcher] Terminating stale DesktopLauncher process (PID " + ph.pid() + ")...");
                            ph.destroy();
                            try {
                                ph.onExit().get(2L, TimeUnit.SECONDS);
                            }
                            catch (Exception exception) {}
                            if (ph.isAlive()) {
                                ph.destroyForcibly();
                                try {
                                    ph.onExit().get(2L, TimeUnit.SECONDS);
                                }
                                catch (Exception exception) {}
                            }
                        });
                    }
                    catch (Exception exception) {}
                });
            }
            catch (Exception exception) {}
            try {
                Files.write(pidFile.toPath(), String.valueOf(myPid).getBytes(StandardCharsets.UTF_8));
                pidFile.deleteOnExit();
            }
            catch (Exception e) {
                System.err.println("[Launcher] Warning writing cgs.pid: " + e.getMessage());
            }
        }
        catch (Throwable t) {
            System.err.println("[Launcher] Failed to clean up stale instances: " + t.getMessage());
        }
    }

    private static boolean isBuildToolCommandLine(String cmd) {
        String c = cmd.toLowerCase(Locale.ROOT);
        return c.contains("gradledaemon") || c.contains("gradle-daemon") || c.contains("gradle-launcher") || c.contains("gradle-wrapper") || c.contains("-pmainclass") || c.contains("kotlin-daemon") || c.contains("idea_rt.jar") || c.contains("jetbrains");
    }
}
