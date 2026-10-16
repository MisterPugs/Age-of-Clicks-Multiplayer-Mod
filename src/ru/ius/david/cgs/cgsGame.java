/*
 * Decompiled with CFR 0.152.
 */
package ru.ius.david.cgs;

import com.badlogic.gdx.Application;
import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Graphics;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.badlogic.gdx.math.Matrix4;
import com.codedisaster.steamworks.SteamID;
import java.io.FileWriter;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.text.SimpleDateFormat;
import java.util.Date;
import ru.ius.david.cgs.AchievementsScreen;
import ru.ius.david.cgs.AgeConfirmationScreen;
import ru.ius.david.cgs.BackgroundEditorScreen;
import ru.ius.david.cgs.CFG;
import ru.ius.david.cgs.CGSSaveLoadScreen;
import ru.ius.david.cgs.CrashReport;
import ru.ius.david.cgs.CrashScreen;
import ru.ius.david.cgs.CursorManager;
import ru.ius.david.cgs.EditorSelectScreen;
import ru.ius.david.cgs.EventLayoutEditorScreen;
import ru.ius.david.cgs.EventsEditorScreen;
import ru.ius.david.cgs.FormablesEditorScreen;
import ru.ius.david.cgs.GovernmentsEditorScreen;
import ru.ius.david.cgs.HelpScreen;
import ru.ius.david.cgs.LanguageSelectScreen;
import ru.ius.david.cgs.LoadingScreen;
import ru.ius.david.cgs.MainMenuScreen;
import ru.ius.david.cgs.MapEditorScreen;
import ru.ius.david.cgs.MapMsaa;
import ru.ius.david.cgs.MapScreen;
import ru.ius.david.cgs.MultiplayerLobbyScreen;
import ru.ius.david.cgs.RenderWatchdog;
import ru.ius.david.cgs.ScenarioData;
import ru.ius.david.cgs.ScenarioSelectScreen;
import ru.ius.david.cgs.SettingsScreen;
import ru.ius.david.cgs.SplashScreen;
import ru.ius.david.cgs.StatisticsRecapScreen;
import ru.ius.david.cgs.StatisticsScreen;
import ru.ius.david.cgs.TextureEditorScreen;
import ru.ius.david.cgs.UpgradesEditorScreen;
import ru.ius.david.cgs.WarManager;
import ru.ius.david.cgs.effects.BatchFade;
import ru.ius.david.cgs.effects.PanelLayer;
import ru.ius.david.cgs.effects.UiFx;
import ru.ius.david.cgs.events.ScenarioEvents;
import ru.ius.david.cgs.game.GameManager;
import ru.ius.david.cgs.game.LanguageManager;
import ru.ius.david.cgs.game.SaveData;
import ru.ius.david.cgs.game.SaveManager;
import ru.ius.david.cgs.game.SettingsManager;
import ru.ius.david.cgs.game.TutorialManager;
import ru.ius.david.cgs.images.BackgroundManager;
import ru.ius.david.cgs.images.CountryFlagManager;
import ru.ius.david.cgs.images.ImageManager;
import ru.ius.david.cgs.images.Images;
import ru.ius.david.cgs.managers.AchievementManager;
import ru.ius.david.cgs.managers.AchievementNotification;
import ru.ius.david.cgs.managers.MusicManager;
import ru.ius.david.cgs.managers.SoundManager;
import ru.ius.david.cgs.mcp.MCPServerManager;
import ru.ius.david.cgs.menus.CharmArt;
import ru.ius.david.cgs.menus.ModManagerScreen;
import ru.ius.david.cgs.multiplayer.MultiplayerManager;
import ru.ius.david.cgs.platform.AdService;
import ru.ius.david.cgs.platform.NoOpAdService;
import ru.ius.david.cgs.platform.NoOpSecurityService;
import ru.ius.david.cgs.platform.PlatformFileChooser;
import ru.ius.david.cgs.platform.PlatformFrameRateService;
import ru.ius.david.cgs.platform.PlatformOrientationService;
import ru.ius.david.cgs.platform.PlatformSecurityService;
import ru.ius.david.cgs.platform.PlatformTiltService;
import ru.ius.david.cgs.steam.SteamManager;
import ru.ius.david.cgs.ui.FullVersionShowcase;
import ru.ius.david.cgs.utils.FrameStats;
import ru.ius.david.cgs.utils.GLStateMirror;
import ru.ius.david.cgs.utils.PrefsWriter;

public class cgsGame
extends Game {
    private SpriteBatch sharedBatch;
    private MainMenuScreen mainMenuScreen;
    private ScenarioSelectScreen scenarioSelectScreen;
    private SettingsScreen settingsScreen;
    private MultiplayerLobbyScreen multiplayerLobbyScreen;
    private CGSSaveLoadScreen saveLoadScreen;
    private MCPServerManager mcpServer;
    private MultiplayerManager activeMultiplayerManager;
    private PlatformFileChooser fileChooser;
    private AdService adService;
    private PlatformOrientationService orientationService;
    private PlatformFrameRateService frameRateService;
    private PlatformSecurityService securityService = new NoOpSecurityService();
    private boolean isGenuine = true;
    private SplashScreen splashScreen;
    private SplashScreen splashCurtain;
    private ModManagerScreen modManagerScreen;
    private PlatformTiltService tiltService;
    private boolean tiltWantedSent = true;
    private SpriteBatch vignetteBatch;
    private final Matrix4 vignetteProjection = new Matrix4();
    private ShaderProgram vignetteShader;
    private Texture white1x1;
    private TransitionState transitionState = TransitionState.IDLE;
    private float transitionTimer = 0.0f;
    private static final float CLOSE_DURATION = 0.15f;
    private static final float OPEN_DURATION = 0.15f;
    private Screen pendingScreen = null;
    private Screen screenToDispose = null;
    private static final float MAX_TRANSITION_INTENSITY = 0.55f;
    private static final float IDLE_INTENSITY = 0.35f;
    private static final float IDLE_RADIUS = 0.5f;
    private boolean warVignetteActive = false;
    private float warVignetteBlend = 0.0f;
    private static final float WAR_BLEND_SPEED = 3.0f;
    private static final float WAR_PULSE_SPEED = 4.0f;
    private static final float WAR_INTENSITY_MIN = 0.2f;
    private static final float WAR_INTENSITY_MAX = 0.4f;
    private static final float WAR_RADIUS = 0.4f;
    private float warPulseTimer = 0.0f;
    private boolean epochVignetteActive = false;
    private float epochVignetteBlend = 0.0f;
    private float epochColorR = 0.0f;
    private float epochColorG = 0.0f;
    private float epochColorB = 0.0f;
    private static final float EPOCH_BLEND_SPEED = 0.75f;
    private static final float EPOCH_PULSE_SPEED = 2.5f;
    private static final float EPOCH_INTENSITY_MIN = 0.4f;
    private static final float EPOCH_INTENSITY_MAX = 0.7f;
    private static final float EPOCH_RADIUS = 0.45f;
    private float epochPulseTimer = 0.0f;
    private boolean skipAutoVignette = false;
    private float idleVignetteScale = 1.0f;
    private boolean isPaused = false;
    private volatile boolean crashScreenActive = false;
    private volatile Throwable pendingCrashError = null;
    private int consecutiveRenderFailures = 0;
    private static final int RENDER_FAILURE_LIMIT = 10;
    private String firstRenderFailureReport = null;
    private String lastLoggedRenderFailure = null;
    private int repeatedRenderFailures = 0;
    private long lastRepeatLogMs = 0L;
    private volatile String pendingCrashText = null;
    private boolean scenariosDirty = false;
    private EventsEditorScreen eventsEditorAwaitingLayout;
    private boolean screenBuildPending = false;
    private static volatile boolean windowFocused = true;
    private static final int FRAMES_LOGGED_AFTER_FOCUS = 10;
    private static volatile int logFramesAfterFocus = 0;
    private static final long UNFOCUSED_FRAME_MS = 50L;
    private static final long SLOW_FRAME_MS = 25L;
    private static final SlowLineGate SLOW_FRAME_LINES = new SlowLineGate();
    static final SlowLineGate SLOW_WAR_UPDATE_LINES = new SlowLineGate();
    private static long paceDeadlineNanos = 0L;
    private static int paceRefreshHz = 0;
    private static long paceRefreshReadNanos = 0L;

    public cgsGame(PlatformFileChooser fileChooser, AdService adService) {
        this.fileChooser = fileChooser;
        this.adService = adService != null ? adService : new NoOpAdService();
    }

    public cgsGame(PlatformFileChooser fileChooser) {
        this(fileChooser, null);
    }

    public cgsGame() {
        this(null, null);
    }

    public PlatformFileChooser getFileChooser() {
        return this.fileChooser;
    }

    public AdService getAdService() {
        return this.adService;
    }

    public void setOrientationService(PlatformOrientationService service) {
        this.orientationService = service;
    }

    public PlatformOrientationService getOrientationService() {
        return this.orientationService;
    }

    public void setFrameRateService(PlatformFrameRateService service) {
        this.frameRateService = service;
    }

    public PlatformFrameRateService getFrameRateService() {
        return this.frameRateService;
    }

    public void setTiltService(PlatformTiltService service) {
        this.tiltService = service;
    }

    private void syncTiltSensor() {
        boolean want;
        PlatformTiltService tilt = this.tiltService;
        if (tilt == null) {
            return;
        }
        Screen pending = this.pendingScreen;
        boolean bl = want = !(this.getScreen() instanceof MapScreen) || pending != null && !(pending instanceof MapScreen);
        if (want == this.tiltWantedSent) {
            return;
        }
        this.tiltWantedSent = want;
        tilt.setTiltWanted(want);
    }

    public void applyFrameRateHint() {
        if (this.frameRateService == null) {
            return;
        }
        try {
            this.frameRateService.applyPreferredFrameRate(SettingsManager.getInstance().getTargetFPS());
        }
        catch (Exception exception) {
            // empty catch block
        }
    }

    public void setSecurityService(PlatformSecurityService service) {
        this.securityService = service != null ? service : new NoOpSecurityService();
    }

    public PlatformSecurityService getSecurityService() {
        return this.securityService;
    }

    public boolean isGenuine() {
        return this.isGenuine;
    }

    public void setPendingCrashText(String text) {
        this.pendingCrashText = text;
    }

    /*
     * WARNING - void declaration
     */
    @Override
    public void create() {
        ScreenAdapter nextScreen;
        GLStateMirror.install();
        CrashReport.arm();
        CrashReport.setContextProvider(this::describeCrashContext);
        RenderWatchdog.start();
        RenderWatchdog.startHeapWatch();
        MapMsaa.init();
        try {
            SettingsManager.getInstance().offerUltraOnce();
            SettingsManager.getInstance().stepDownUltraOnWeakGpuOnce();
        }
        catch (Throwable t) {
            Gdx.app.error("cgsGame", "Ultra offer failed", t);
        }
        if (Gdx.app.getType() != Application.ApplicationType.Android && Gdx.app.getType() != Application.ApplicationType.iOS) {
            Thread.setDefaultUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler(){

                @Override
                public void uncaughtException(Thread t, final Throwable e) {
                    try {
                        String stackTrace = CrashReport.format(e, t);
                        String time = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss").format(new Date());
                        String fileName = "crash_log_" + time + ".txt";
                        if (Gdx.files != null) {
                            Gdx.files.local(fileName).writeString(stackTrace, false);
                        } else {
                            FileWriter fw = new FileWriter(fileName);
                            fw.write(stackTrace);
                            fw.close();
                        }
                    }
                    catch (Exception ex) {
                        ex.printStackTrace();
                    }
                    e.printStackTrace();
                    System.err.flush();
                    if (!cgsGame.this.crashScreenActive) {
                        cgsGame.this.pendingCrashError = e;
                        try {
                            Gdx.app.postRunnable(new Runnable(){

                                @Override
                                public void run() {
                                    try {
                                        cgsGame.this.showCrashScreen(e);
                                    }
                                    catch (Exception ex3) {
                                        ex3.printStackTrace();
                                        System.exit(1);
                                    }
                                }
                            });
                            return;
                        }
                        catch (Exception ex4) {
                            ex4.printStackTrace();
                        }
                    }
                    System.exit(1);
                }
            });
        }
        long t0 = System.currentTimeMillis();
        this.sharedBatch = new SpriteBatch();
        long t = System.currentTimeMillis();
        CFG.init();
        try {
            this.isGenuine = this.securityService.isGenuine();
        }
        catch (Exception e) {
            this.isGenuine = false;
        }
        CFG.GAME_WIDTH = Gdx.graphics.getWidth();
        CFG.GAME_HEIGHT = Gdx.graphics.getHeight();
        if (CFG.DEBUG_LOG) {
            Gdx.app.setLogLevel(3);
        } else {
            Gdx.app.setLogLevel(0);
        }
        Gdx.graphics.setContinuousRendering(true);
        SettingsManager.getInstance().applyFPS();
        this.applyFrameRateHint();
        SettingsManager.getInstance().applyVSync();
        Gdx.app.log("STARTUP", "CFG.init: " + (System.currentTimeMillis() - t) + "ms");
        t = System.currentTimeMillis();
        CFG.loadFontMain();
        Gdx.app.log("STARTUP", "loadFontMain: " + (System.currentTimeMillis() - t) + "ms");
        t = System.currentTimeMillis();
        Images.init();
        Gdx.app.log("STARTUP", "Images.init: " + (System.currentTimeMillis() - t) + "ms");
        t = System.currentTimeMillis();
        LanguageManager.getInstance().init();
        Gdx.app.log("STARTUP", "LanguageManager: " + (System.currentTimeMillis() - t) + "ms");
        t = System.currentTimeMillis();
        SoundManager.getInstance();
        Gdx.app.log("STARTUP", "SoundManager: " + (System.currentTimeMillis() - t) + "ms");
        t = System.currentTimeMillis();
        MusicManager.getInstance();
        Gdx.app.log("STARTUP", "MusicManager: " + (System.currentTimeMillis() - t) + "ms");
        t = System.currentTimeMillis();
        this.initVignetteShader();
        Gdx.app.log("STARTUP", "VignetteShader: " + (System.currentTimeMillis() - t) + "ms");
        t = System.currentTimeMillis();
        AchievementNotification.getInstance().init();
        AchievementManager.getInstance();
        Gdx.app.log("STARTUP", "Achievements: " + (System.currentTimeMillis() - t) + "ms");
        t = System.currentTimeMillis();
        this.createMenuScreens();
        Gdx.app.log("STARTUP", "createMenuScreens: " + (System.currentTimeMillis() - t) + "ms");
        t = System.currentTimeMillis();
        if (CFG.isMcpServerEnabled()) {
            this.mcpServer = new MCPServerManager();
            this.mcpServer.start();
        }
        Gdx.app.log("STARTUP", "MCPServer: " + (System.currentTimeMillis() - t) + "ms");
        t = System.currentTimeMillis();
        CursorManager.init();
        Gdx.app.log("STARTUP", "CursorManager: " + (System.currentTimeMillis() - t) + "ms");
        Gdx.app.log("STARTUP", "=== TOTAL create(): " + (System.currentTimeMillis() - t0) + "ms ===");
        if (this.pendingCrashText != null) {
            this.showCrashScreen("CRASH / ERROR", this.pendingCrashText);
            return;
        }
        if (!SettingsManager.getInstance().isLanguageSet()) {
            nextScreen = new LanguageSelectScreen(this);
        } else if (!SettingsManager.getInstance().isAgeSet()) {
            nextScreen = new AgeConfirmationScreen(this);
        } else {
            if (this.adService != null) {
                this.adService.initializeAdsPrivacy(SettingsManager.getInstance().getPlayerAge());
            }
            nextScreen = this.mainMenuScreen;
        }
        try {
            Class<?> glfwClass;
            StringBuilder logObj = new StringBuilder();
            logObj.append("=== GRAPHICS DIAGNOSTIC ===\n");
            logObj.append("Date: ").append(new Date().toString()).append("\n");
            logObj.append("Gdx.graphics.getWidth/Height: ").append(Gdx.graphics.getWidth()).append("x").append(Gdx.graphics.getHeight()).append("\n");
            logObj.append("Gdx.graphics.getBackBufferWidth/Height: ").append(Gdx.graphics.getBackBufferWidth()).append("x").append(Gdx.graphics.getBackBufferHeight()).append("\n");
            Graphics.Monitor[] monitors = Gdx.graphics.getMonitors();
            logObj.append("Monitors Count: ").append(monitors.length).append("\n");
            for (int i = 0; i < monitors.length; ++i) {
                Graphics.Monitor m = monitors[i];
                Graphics.DisplayMode dm = Gdx.graphics.getDisplayMode(m);
                logObj.append("  Monitor ").append(i).append(":\n");
                logObj.append("    Name: ").append(m.name).append("\n");
                logObj.append("    Virtual Pos: ").append(m.virtualX).append(", ").append(m.virtualY).append("\n");
                if (dm != null) {
                    logObj.append("    DisplayMode Res: ").append(dm.width).append("x").append(dm.height).append(" @").append(dm.refreshRate).append("Hz\n");
                }
                try {
                    Field field = m.getClass().getDeclaredField("monitorHandle");
                    field.setAccessible(true);
                    long handle = field.getLong(m);
                    logObj.append("    Handle: ").append(handle).append("\n");
                    if (handle == 0L) continue;
                    glfwClass = Class.forName("org.lwjgl.glfw.GLFW");
                    Method getScaleMethod = glfwClass.getMethod("glfwGetMonitorContentScale", Long.TYPE, float[].class, float[].class);
                    float[] fArray = new float[1];
                    float[] sy = new float[1];
                    getScaleMethod.invoke(null, handle, fArray, sy);
                    logObj.append("    Content Scale: ").append(fArray[0]).append(", ").append(sy[0]).append("\n");
                    Method getVideoModeMethod = glfwClass.getMethod("glfwGetVideoMode", Long.TYPE);
                    Object videoMode = getVideoModeMethod.invoke(null, handle);
                    if (videoMode == null) continue;
                    Method wMethod = videoMode.getClass().getMethod("width", new Class[0]);
                    Method hMethod = videoMode.getClass().getMethod("height", new Class[0]);
                    logObj.append("    GLFW VidMode size: ").append(wMethod.invoke(videoMode, new Object[0])).append("x").append(hMethod.invoke(videoMode, new Object[0])).append("\n");
                    continue;
                }
                catch (Exception ex) {
                    logObj.append("    Failed to get extra info: ").append(ex.getMessage()).append("\n");
                }
            }
            try {
                Graphics graphics = Gdx.graphics;
                Method getWindowMethod = graphics.getClass().getMethod("getWindow", new Class[0]);
                Object window = getWindowMethod.invoke(graphics, new Object[0]);
                if (window != null) {
                    Method getHandleMethod = window.getClass().getMethod("getWindowHandle", new Class[0]);
                    long winHandle = (Long)getHandleMethod.invoke(window, new Object[0]);
                    logObj.append("Window Handle: ").append(winHandle).append("\n");
                    glfwClass = Class.forName("org.lwjgl.glfw.GLFW");
                    int[] wx = new int[1];
                    int[] nArray = new int[1];
                    Method getWindowPosMethod = glfwClass.getMethod("glfwGetWindowPos", Long.TYPE, int[].class, int[].class);
                    getWindowPosMethod.invoke(null, winHandle, wx, nArray);
                    logObj.append("Window Position (GLFW): ").append(wx[0]).append(", ").append(nArray[0]).append("\n");
                    int[] ww = new int[1];
                    int[] wh = new int[1];
                    Method getWindowSizeMethod = glfwClass.getMethod("glfwGetWindowSize", Long.TYPE, int[].class, int[].class);
                    getWindowSizeMethod.invoke(null, winHandle, ww, wh);
                    logObj.append("Window Size (GLFW): ").append(ww[0]).append("x").append(wh[0]).append("\n");
                    int[] fw = new int[1];
                    int[] fh = new int[1];
                    Method getFramebufferSizeMethod = glfwClass.getMethod("glfwGetFramebufferSize", Long.TYPE, int[].class, int[].class);
                    getFramebufferSizeMethod.invoke(null, winHandle, fw, fh);
                    logObj.append("Framebuffer Size (GLFW): ").append(fw[0]).append("x").append(fh[0]).append("\n");
                }
            }
            catch (Exception ex) {
                logObj.append("Failed to get window details: ").append(ex.getMessage()).append("\n");
            }
            int logW2 = Gdx.graphics.getWidth();
            int logH2 = Gdx.graphics.getHeight();
            int physW2 = Gdx.graphics.getBackBufferWidth();
            int physH2 = Gdx.graphics.getBackBufferHeight();
            logObj.append("DPI ratio (BackBuf/Window): ").append(logW2 > 0 ? String.format("%.3f", Float.valueOf((float)physW2 / (float)logW2)) : "?").append(" x ").append(logH2 > 0 ? String.format("%.3f", Float.valueOf((float)physH2 / (float)logH2)) : "?").append("\n");
            logObj.append("CFG.GAME_WIDTH/HEIGHT: ").append(CFG.GAME_WIDTH).append("x").append(CFG.GAME_HEIGHT).append("\n");
            logObj.append("WindowMode (settings): ").append(SettingsManager.getInstance().getWindowMode()).append(" (0=windowed,1=fullscreen,2=borderless)\n");
            logObj.append("ResolutionIndex: ").append(SettingsManager.getInstance().getResolutionIndex()).append("\n");
            logObj.append("isPaused at create() end: ").append(this.isPaused).append("\n");
            try {
                FileHandle localDir = Gdx.files.local(".");
                FileHandle[] localFiles = localDir.list();
                FileHandle newestCrash = null;
                int crashCount = 0;
                if (localFiles != null) {
                    for (FileHandle f : localFiles) {
                        if (!f.name().startsWith("crash_log_") || !f.name().endsWith(".txt")) continue;
                        ++crashCount;
                        if (newestCrash != null && f.name().compareTo(newestCrash.name()) <= 0) continue;
                        newestCrash = f;
                    }
                }
                if (newestCrash != null) {
                    logObj.append("Previous crash logs found: ").append(crashCount).append("\n");
                    String string = newestCrash.readString();
                    String crashText = string.length() > 3000 ? string.substring(0, 3000) + "...[truncated]" : string;
                    logObj.append("Latest crash (").append(newestCrash.name()).append("):\n").append(crashText).append("\n");
                } else {
                    logObj.append("No previous crash logs.\n");
                }
            }
            catch (Exception ex2) {
                logObj.append("Could not scan for crash logs: ").append(ex2.getMessage()).append("\n");
            }
            Gdx.files.local("cgs_graphics_log.txt").writeString(logObj.toString(), false);
            Gdx.app.log("DIAGNOSTIC", "Diagnostic log written to cgs_graphics_log.txt");
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        this.setScreen(new SplashScreen(this, nextScreen));
    }

    private void createMenuScreens() {
        this.mainMenuScreen = new MainMenuScreen(this);
        try { cgs.modloader.ModLoader.getInstance().onGameCreated(this); } catch(Throwable t) {}
        this.settingsScreen = new SettingsScreen(this);
        this.saveLoadScreen = new CGSSaveLoadScreen(this);
        this.scenarioSelectScreen = null;
        this.multiplayerLobbyScreen = null;
    }

    private ScenarioSelectScreen ensureScenarioSelectScreen() {
        if (this.scenarioSelectScreen == null) {
            this.scenarioSelectScreen = new ScenarioSelectScreen(this);
            this.scenariosDirty = false;
        }
        return this.scenarioSelectScreen;
    }

    private MultiplayerLobbyScreen ensureMultiplayerLobbyScreen() {
        if (this.multiplayerLobbyScreen == null) {
            this.multiplayerLobbyScreen = new MultiplayerLobbyScreen(this);
        }
        return this.multiplayerLobbyScreen;
    }

    public void recreateScreens() {
        if (this.mainMenuScreen != null) {
            this.mainMenuScreen.dispose();
        }
        if (this.scenarioSelectScreen != null) {
            this.scenarioSelectScreen.dispose();
        }
        if (this.settingsScreen != null) {
            this.settingsScreen.dispose();
        }
        if (this.multiplayerLobbyScreen != null) {
            this.multiplayerLobbyScreen.dispose();
        }
        if (this.saveLoadScreen != null) {
            this.saveLoadScreen.dispose();
        }
        this.createMenuScreens();
        if (this.getScreen() instanceof MainMenuScreen) {
            this.setScreen(this.mainMenuScreen);
        } else if (this.getScreen() instanceof SettingsScreen) {
            this.setScreen(this.settingsScreen);
        } else if (this.getScreen() instanceof ScenarioSelectScreen) {
            this.setScreen(this.ensureScenarioSelectScreen());
        } else if (this.getScreen() instanceof MultiplayerLobbyScreen) {
            this.setScreen(this.ensureMultiplayerLobbyScreen());
        } else if (this.getScreen() instanceof CGSSaveLoadScreen) {
            this.setScreen(this.saveLoadScreen);
        }
    }

    private void initVignetteShader() {
        String vertSrc = Gdx.files.internal("shaders/vignette.vert").readString();
        String fragSrc = Gdx.files.internal("shaders/vignette.frag").readString();
        ShaderProgram.pedantic = false;
        this.vignetteShader = new ShaderProgram(vertSrc, fragSrc);
        if (!this.vignetteShader.isCompiled()) {
            Gdx.app.error("VignetteShader", "Compilation failed:\n" + this.vignetteShader.getLog());
        }
        this.vignetteBatch = new SpriteBatch();
        Pixmap pix = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        pix.setColor(1.0f, 1.0f, 1.0f, 1.0f);
        pix.fill();
        this.white1x1 = new Texture(pix);
        pix.dispose();
    }

    public SpriteBatch getSharedBatch() {
        return this.sharedBatch;
    }

    public void setWarVignetteActive(boolean active) {
        this.warVignetteActive = active;
    }

    public void setEpochVignetteActive(boolean active) {
        this.epochVignetteActive = active;
    }

    public void setEpochVignetteColor(float r, float g, float b) {
        this.epochColorR = r;
        this.epochColorG = g;
        this.epochColorB = b;
    }

    public void setSkipAutoVignette(boolean skip) {
        this.skipAutoVignette = skip;
    }

    public void setIdleVignetteScale(float scale) {
        this.idleVignetteScale = scale;
    }

    public void showToast(String msg) {
        if (this.getScreen() instanceof MapScreen) {
            ((MapScreen)this.getScreen()).showToast(msg);
        } else {
            Gdx.app.log("TOAST", msg);
        }
    }

    public void drawVignetteNow() {
        this.drawVignetteOverlay();
    }

    void showSplashCurtain(SplashScreen splash) {
        this.splashCurtain = splash;
    }

    private void dropSplashCurtain() {
        if (this.splashCurtain == null) {
            return;
        }
        try {
            this.splashCurtain.dispose();
        }
        catch (Throwable throwable) {
            // empty catch block
        }
        this.splashCurtain = null;
    }

    
    public void showEnhancedModManager() {
        this.startTransitionDeferred(() -> {
            return new cgs.modloader.ui.EnhancedModManagerScreen(this);
        });
    }

    public void showModManager() {
        this.showEnhancedModManager();
        if (true) return;

        this.startTransitionDeferred(() -> {
            if (this.modManagerScreen != null) {
                this.modManagerScreen.dispose();
            }
            this.markScenariosDirty();
            this.modManagerScreen = new ModManagerScreen(this);
            return this.modManagerScreen;
        });
    }

    public void showMainMenu() {
        this.disconnectMultiplayer();
        this.startTransition(this.mainMenuScreen);
    }

    public void markScenariosDirty() {
        this.scenariosDirty = true;
    }

    public void showScenarioSelect() {
        this.startTransitionDeferred(() -> {
            if (this.scenariosDirty && this.scenarioSelectScreen != null) {
                this.scenarioSelectScreen.dispose();
                this.scenarioSelectScreen = null;
            }
            return this.ensureScenarioSelectScreen();
        });
    }

    public void showEditorSelect() {
        this.markScenariosDirty();
        this.startTransitionDeferred(() -> new EditorSelectScreen(this));
    }

    public void showBackgroundEditor() {
        this.startTransitionDeferred(() -> new BackgroundEditorScreen(this));
    }

    public void showTextureEditor() {
        this.startTransitionDeferred(() -> new TextureEditorScreen(this));
    }

    public void showModManagerFromEditor() {
        if (this.modManagerScreen != null) {
            this.modManagerScreen.dispose();
        }
        this.markScenariosDirty();
        this.modManagerScreen = new ModManagerScreen(this, true);
        this.startTransition(this.modManagerScreen);
    }

    public void showUpgradesEditor(ScenarioData scenario) {
        this.markScenariosDirty();
        this.startTransitionDeferred(() -> new UpgradesEditorScreen(this, scenario));
    }

    public void showGovernmentsEditor(ScenarioData scenario) {
        this.markScenariosDirty();
        this.startTransitionDeferred(() -> new GovernmentsEditorScreen(this, scenario));
    }

    public void showEventsEditor(ScenarioData scenario) {
        this.markScenariosDirty();
        if (this.eventsEditorAwaitingLayout != null) {
            this.eventsEditorAwaitingLayout.dispose();
            this.eventsEditorAwaitingLayout = null;
        }
        this.startTransition(new EventsEditorScreen(this, scenario));
    }

    public void showEventsEditor(ScenarioData scenario, ScenarioEvents doc, int selectIndex, boolean dirty) {
        this.markScenariosDirty();
        EventsEditorScreen kept = this.eventsEditorAwaitingLayout;
        this.eventsEditorAwaitingLayout = null;
        if (kept != null && kept.isEditing(scenario)) {
            kept.returnFromLayoutEditor(doc, selectIndex, dirty);
            this.startTransition(kept);
            return;
        }
        if (kept != null) {
            kept.dispose();
        }
        this.startTransition(new EventsEditorScreen(this, scenario, doc, selectIndex, dirty));
    }

    public void showEventLayoutEditor(ScenarioData scenario, ScenarioEvents doc, String layoutId, String sampleEventId, int returnIndex, boolean dirty) {
        this.markScenariosDirty();
        Screen leaving = this.getScreen();
        this.startTransition(new EventLayoutEditorScreen(this, scenario, doc, layoutId, sampleEventId, returnIndex, dirty));
        if (leaving instanceof EventsEditorScreen && this.screenToDispose == leaving) {
            this.screenToDispose = null;
            if (this.eventsEditorAwaitingLayout != null && this.eventsEditorAwaitingLayout != leaving) {
                this.eventsEditorAwaitingLayout.dispose();
            }
            this.eventsEditorAwaitingLayout = (EventsEditorScreen)leaving;
        }
    }

    public void showMapEditor(ScenarioData scenario) {
        this.markScenariosDirty();
        this.startTransitionDeferred(() -> new MapEditorScreen(this, scenario));
    }

    public void showFormablesEditor(ScenarioData scenario) {
        this.markScenariosDirty();
        this.startTransitionDeferred(() -> new FormablesEditorScreen(this, scenario));
    }

    public void showSaveLoadScreen() {
        this.startTransition(this.saveLoadScreen);
    }

    public void showSettings() {
        this.startTransition(this.settingsScreen);
    }

    public void showStatistics() {
        this.startTransitionDeferred(() -> new StatisticsScreen(this));
    }

    public void showStatisticsRecap() {
        this.startTransitionDeferred(() -> new StatisticsRecapScreen(this));
    }

    public void showAchievements() {
        this.startTransitionDeferred(() -> new AchievementsScreen(this));
    }

    public void showHelpScreen() {
        this.startTransitionDeferred(() -> new HelpScreen(this));
    }

    public void showMultiplayerLobby() {
        this.startTransitionDeferred(this::ensureMultiplayerLobbyScreen);
    }

    public void joinSteamLobbyDirectly(final SteamID lobbyId) {
        Gdx.app.postRunnable(new Runnable(){

            @Override
            public void run() {
                cgsGame.this.disconnectMultiplayer();
                cgsGame.this.startTransition(cgsGame.this.ensureMultiplayerLobbyScreen());
                cgsGame.this.multiplayerLobbyScreen.joinLobbyDirectly(lobbyId);
            }
        });
    }

    public void showLanguageSelect() {
        this.startTransitionDeferred(() -> new LanguageSelectScreen(this));
    }

    public void showAgeConfirmation() {
        this.startTransitionDeferred(() -> new AgeConfirmationScreen(this));
    }

    public void startMultiplayerGame(final MultiplayerManager mpManager, final String scenarioPath, final long seed) {
        Gdx.app.postRunnable(new Runnable() {
            @Override
            public void run() {
                cgsGame.this.activeMultiplayerManager = mpManager;
                GameManager.reset();
                GameManager gm = GameManager.getInstance();
                gm.setRandomSeed(seed);
                gm.setNetworkMode(true);
                mpManager.setGameManager(gm);
                if (cgsGame.this.scenarioSelectScreen != null) {
                    cgsGame.this.scenarioSelectScreen.dispose();
                    cgsGame.this.scenarioSelectScreen = null;
                }
                LoadingScreen loadingScreen = new LoadingScreen(cgsGame.this, scenarioPath);
                cgsGame.this.startTransition(loadingScreen);
            }
        });
    }

    public void startScenario(String scenarioPath) {
        this.disconnectMultiplayer();
        Screen current = this.getScreen();
        if (current instanceof MapScreen) {
            this.warVignetteActive = false;
            this.epochVignetteActive = false;
            this.warVignetteBlend = 0.0f;
            try {
                current.dispose();
            }
            catch (Exception e) {
                Gdx.app.error("cgsGame", "Error disposing old MapScreen", e);
            }
            this.screenToDispose = null;
            super.setScreen(null);
        }
        if (this.scenarioSelectScreen != null) {
            this.scenarioSelectScreen.dispose();
            this.scenarioSelectScreen = null;
        }
        GameManager.reset();
        System.gc();
        LoadingScreen loadingScreen = new LoadingScreen(this, scenarioPath);
        this.startTransition(loadingScreen);
    }

    public void startTutorialScenario() {
        this.disconnectMultiplayer();
        Screen currentTut = this.getScreen();
        if (currentTut instanceof MapScreen) {
            this.warVignetteActive = false;
            this.epochVignetteActive = false;
            this.warVignetteBlend = 0.0f;
            try {
                currentTut.dispose();
            }
            catch (Exception e) {
                Gdx.app.error("cgsGame", "Error disposing old MapScreen (tutorial)", e);
            }
            this.screenToDispose = null;
            super.setScreen(null);
        }
        if (this.scenarioSelectScreen != null) {
            this.scenarioSelectScreen.dispose();
            this.scenarioSelectScreen = null;
        }
        GameManager.reset();
        GameManager.getInstance().setTutorialMode(true);
        TutorialManager.getInstance().resetAllTutorials();
        String tutorialScenarioPath = "scenarios/1936/";
        LoadingScreen loadingScreen = new LoadingScreen(this, tutorialScenarioPath);
        this.startTransition(loadingScreen);
    }

    public void loadSaveGame(String saveSlotName) {
        this.disconnectMultiplayer();
        Screen currentSave = this.getScreen();
        if (currentSave instanceof MapScreen) {
            this.warVignetteActive = false;
            this.epochVignetteActive = false;
            this.warVignetteBlend = 0.0f;
            try {
                currentSave.dispose();
            }
            catch (Exception e) {
                Gdx.app.error("cgsGame", "Error disposing old MapScreen (loadSave)", e);
            }
            this.screenToDispose = null;
            super.setScreen(null);
        }
        if (this.scenarioSelectScreen != null) {
            this.scenarioSelectScreen.dispose();
            this.scenarioSelectScreen = null;
        }
        GameManager.reset();
        SaveData saveData = SaveManager.tryLoadSaveData(saveSlotName);
        if (saveData != null) {
            String scenarioPath = saveData.scenarioPath;
            if (scenarioPath == null || scenarioPath.isEmpty()) {
                scenarioPath = "map/scenario_main/";
            }
            LoadingScreen loadingScreen = new LoadingScreen(this, scenarioPath, saveSlotName, saveData);
            this.startTransition(loadingScreen);
        } else {
            Gdx.app.error("cgsGame", "Could not load save data metadata for " + saveSlotName);
        }
    }

    private void disconnectMultiplayer() {
        if (this.activeMultiplayerManager != null) {
            this.activeMultiplayerManager.shutdown();
            this.activeMultiplayerManager = null;
        }
        GameManager.getInstance().setNetworkMode(false);
    }

    public void showScenarioSelectFromGame() {
        this.warVignetteActive = false;
        this.warVignetteBlend = 0.0f;
        if (this.activeMultiplayerManager != null) {
            this.activeMultiplayerManager.shutdown();
            this.activeMultiplayerManager = null;
        }
        GameManager.getInstance().setNetworkMode(false);
        Screen current = this.getScreen();
        this.startTransition(this.ensureScenarioSelectScreen());
        if (current != null && current != this.mainMenuScreen && current != this.scenarioSelectScreen && current != this.settingsScreen) {
            this.screenToDispose = current;
        }
        Thread gcThread = new Thread(new Runnable(){

            @Override
            public void run() {
                try {
                    Thread.sleep(500L);
                }
                catch (InterruptedException interruptedException) {
                    // empty catch block
                }
                System.gc();
                Gdx.app.log("cgsGame", "Post-game GC hint fired");
            }
        }, "GC-Hint");
        gcThread.setDaemon(true);
        gcThread.start();
    }

    private void startTransitionDeferred(final ScreenBuilder builder) {
        if (this.screenBuildPending) {
            return;
        }
        this.screenBuildPending = true;
        Gdx.app.postRunnable(new Runnable(){

            @Override
            public void run() {
                try {
                    cgsGame.this.startTransition(builder.build());
                }
                finally {
                    cgsGame.this.screenBuildPending = false;
                }
            }
        });
    }

    private void startTransition(Screen target) {
        if (target == this.getScreen()) {
            return;
        }
        if (this.transitionState != TransitionState.IDLE) {
            this.pendingScreen = target;
            return;
        }
        Screen current = this.getScreen();
        if (current != null && current != this.mainMenuScreen && current != this.scenarioSelectScreen && current != this.settingsScreen && current != this.multiplayerLobbyScreen && current != this.saveLoadScreen) {
            this.screenToDispose = current;
        }
        this.pendingScreen = target;
        this.transitionState = TransitionState.CLOSING;
        this.transitionTimer = 0.0f;
        SoundManager.getInstance().play(SoundManager.SFX.SCREEN_TRANSITION);
    }

    public void showCrashScreen(Throwable error) {
        if (this.crashScreenActive) {
            return;
        }
        this.crashScreenActive = true;
        if (this.sharedBatch != null && this.sharedBatch.isDrawing()) {
            try {
                this.sharedBatch.end();
            }
            catch (Exception exception) {
                // empty catch block
            }
        }
        if (this.vignetteBatch != null && this.vignetteBatch.isDrawing()) {
            try {
                this.vignetteBatch.end();
            }
            catch (Exception exception) {
                // empty catch block
            }
        }
        this.skipAutoVignette = true;
        this.warVignetteActive = false;
        this.epochVignetteActive = false;
        this.transitionState = TransitionState.IDLE;
        this.dropSplashCurtain();
        try {
            CrashScreen crashScreen = new CrashScreen(this, error);
            this.setScreen(crashScreen);
        }
        catch (Throwable e) {
            e.printStackTrace();
            System.exit(1);
        }
    }

    public void showCrashScreen(String title, String text) {
        if (this.crashScreenActive) {
            return;
        }
        this.crashScreenActive = true;
        if (this.sharedBatch != null && this.sharedBatch.isDrawing()) {
            try {
                this.sharedBatch.end();
            }
            catch (Exception exception) {
                // empty catch block
            }
        }
        if (this.vignetteBatch != null && this.vignetteBatch.isDrawing()) {
            try {
                this.vignetteBatch.end();
            }
            catch (Exception exception) {
                // empty catch block
            }
        }
        this.skipAutoVignette = true;
        this.warVignetteActive = false;
        this.epochVignetteActive = false;
        this.transitionState = TransitionState.IDLE;
        this.dropSplashCurtain();
        try {
            CrashScreen crashScreen = new CrashScreen(this, title, text);
            this.setScreen(crashScreen);
        }
        catch (Throwable e) {
            e.printStackTrace();
            System.exit(1);
        }
    }

    public void clearCrashState() {
        this.crashScreenActive = false;
        this.pendingCrashError = null;
        this.skipAutoVignette = false;
    }

    @Override
    public void resize(int width, int height) {
        if (width > 0 && height > 0) {
            CFG.GAME_WIDTH = width;
            CFG.GAME_HEIGHT = height;
            try {
                int backW = Gdx.graphics.getBackBufferWidth();
                int backH = Gdx.graphics.getBackBufferHeight();
                if (backW <= 0) {
                    backW = width;
                }
                if (backH <= 0) {
                    backH = height;
                }
                Gdx.gl.glViewport(0, 0, backW, backH);
            }
            catch (Exception exception) {
                // empty catch block
            }
            super.resize(width, height);
        }
    }

    @Override
    public void render() {
        RenderWatchdog.heartbeat();
        long frameStartNanos = System.nanoTime();
        FrameStats.frameStart();
        try {
            this.renderDispatch();
        }
        finally {
            FrameStats.frameEnd();
            this.logSlowFrame(frameStartNanos);
            cgsGame.paceAndroidFrame(frameStartNanos);
            cgsGame.idleWhileMinimized();
            this.paceUnfocusedFrame(frameStartNanos);
        }
    }

    public static void setWindowFocused(boolean focused) {
        if (windowFocused == focused) {
            return;
        }
        windowFocused = focused;
        if (focused) {
            logFramesAfterFocus = 10;
        }
        if (CFG.DEBUG_LOG && Gdx.app != null) {
            Gdx.app.log("cgsGame", (focused ? "focus GAINED" : "focus LOST") + " at " + System.currentTimeMillis());
        }
    }

    public static boolean isWindowFocused() {
        return windowFocused;
    }

    private void paceUnfocusedFrame(long frameStartNanos) {
        if (windowFocused) {
            return;
        }
        if (Gdx.app == null || Gdx.app.getType() != Application.ApplicationType.Desktop) {
            return;
        }
        try {
            if (GameManager.getInstance() != null && GameManager.getInstance().isNetworkMode()) {
                return;
            }
        }
        catch (Throwable throwable) {
            // empty catch block
        }
        long spentMs = (System.nanoTime() - frameStartNanos) / 1000000L;
        long left = 50L - spentMs;
        if (left <= 0L) {
            return;
        }
        try {
            Thread.sleep(left);
        }
        catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private void logSlowFrame(long frameStartNanos) {
        String more;
        if (!CFG.DEBUG_LOG) {
            return;
        }
        long ms = (System.nanoTime() - frameStartNanos) / 1000000L;
        if (logFramesAfterFocus > 0) {
            int n = 10 - logFramesAfterFocus + 1;
            --logFramesAfterFocus;
            Gdx.app.log("cgsGame", "frame " + n + " after focus: " + ms + "ms at " + System.currentTimeMillis());
            return;
        }
        if (ms >= 25L && (more = cgsGame.slowLineSuffix(SLOW_FRAME_LINES, ms)) != null) {
            Gdx.app.log("cgsGame", "SLOW frame " + ms + "ms at " + System.currentTimeMillis() + more + (FrameStats.lastFrameWatched() ? " [counter]" : ""));
        }
    }

    static String slowLineSuffix(SlowLineGate gate, long ms) {
        Application.ApplicationType type;
        Application.ApplicationType applicationType = type = Gdx.app != null ? Gdx.app.getType() : null;
        if (type != Application.ApplicationType.Android && type != Application.ApplicationType.iOS) {
            return "";
        }
        return gate.admit(ms);
    }

    private static void idleWhileMinimized() {
        if (Gdx.app == null || Gdx.app.getType() != Application.ApplicationType.Desktop) {
            return;
        }
        if (Gdx.graphics.getWidth() > 0 && Gdx.graphics.getHeight() > 0) {
            return;
        }
        try {
            Thread.sleep(16L);
        }
        catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private void renderDispatch() {
        SteamManager.runCallbacks();
        BatchFade.reset();
        try {
            int backW = Gdx.graphics.getBackBufferWidth();
            int backH = Gdx.graphics.getBackBufferHeight();
            if (backW > 0 && backH > 0) {
                Gdx.gl.glViewport(0, 0, backW, backH);
            }
        }
        catch (Exception backW) {
            // empty catch block
        }
        if (this.pendingCrashError != null && !this.crashScreenActive) {
            Throwable err = this.pendingCrashError;
            this.pendingCrashError = null;
            this.showCrashScreen(err);
            return;
        }
        if (this.crashScreenActive) {
            super.render();
            return;
        }
        try {
            this.renderInternal();
            this.consecutiveRenderFailures = 0;
            this.firstRenderFailureReport = null;
            CrashReport.restoreReserve();
        }
        catch (Throwable e) {
            ++this.consecutiveRenderFailures;
            if (e instanceof OutOfMemoryError) {
                CrashReport.releaseReserve();
            }
            if (this.consecutiveRenderFailures == 1) {
                try {
                    this.firstRenderFailureReport = CrashReport.format(e, Thread.currentThread());
                    this.logRenderFailure(e, this.firstRenderFailureReport);
                }
                catch (Throwable reportFailed) {
                    this.firstRenderFailureReport = null;
                }
            }
            if (this.consecutiveRenderFailures < 10) {
                return;
            }
            try {
                Gdx.app.error("cgsGame", "Exception caught in render loop (persisted " + this.consecutiveRenderFailures + " frames)", e);
            }
            catch (Throwable reportFailed) {
                // empty catch block
            }
            String report = CrashReport.format(e, Thread.currentThread());
            try {
                if (this.firstRenderFailureReport != null && !this.firstRenderFailureReport.equals(report)) {
                    report = report + "\n\n=== FIRST occurrence (captured before fast-throw) ===\n" + this.firstRenderFailureReport;
                }
            }
            catch (Throwable throwable) {
                // empty catch block
            }
            try {
                System.err.flush();
                String time = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss").format(new Date());
                String fileName = "crash_log_" + time + ".txt";
                if (Gdx.files != null) {
                    Gdx.files.local(fileName).writeString(report, false);
                }
            }
            catch (Throwable ex) {
                ex.printStackTrace();
            }
            if (CrashReport.isStackless(e) && this.firstRenderFailureReport != null) {
                this.showCrashScreen("CRASH / ERROR", report);
            }
            this.showCrashScreen(e);
        }
    }

    private void logRenderFailure(Throwable e, String report) {
        String signature = CrashReport.signature(e);
        if (!signature.equals(this.lastLoggedRenderFailure)) {
            if (this.repeatedRenderFailures > 0) {
                Gdx.app.error("cgsGame", "Previous render-loop failure repeated " + this.repeatedRenderFailures + " more time(s) after its last log line");
            }
            this.lastLoggedRenderFailure = signature;
            this.repeatedRenderFailures = 0;
            this.lastRepeatLogMs = System.currentTimeMillis();
            Gdx.app.error("cgsGame", "First render-loop failure (real stack likely intact):\n" + report);
            return;
        }
        ++this.repeatedRenderFailures;
        long now = System.currentTimeMillis();
        if (now - this.lastRepeatLogMs >= 1000L) {
            Gdx.app.error("cgsGame", "Render-loop failure repeated " + this.repeatedRenderFailures + " time(s) since the last line, same as the report logged earlier: " + signature);
            this.repeatedRenderFailures = 0;
            this.lastRepeatLogMs = now;
        }
    }

    private String describeCrashContext() {
        StringBuilder sb = new StringBuilder(128);
        try {
            sb.append("screen=").append(cgsGame.screenName(this.getScreen()));
        }
        catch (Throwable throwable) {
            // empty catch block
        }
        try {
            GameManager gm = GameManager.peekInstance();
            if (gm == null) {
                sb.append(", game=none");
            } else {
                WarManager wm = gm.getWarManager();
                if (wm == null) {
                    sb.append(", war=none");
                } else {
                    long w = wm.getMapWidth();
                    long h = wm.getMapHeight();
                    sb.append(", map=").append(w).append('x').append(h).append(" (").append(w * h / 1000000L).append("Mpx)");
                }
            }
        }
        catch (Throwable gm) {
            // empty catch block
        }
        try {
            SettingsManager st = SettingsManager.getInstance();
            sb.append(", quality=").append(st.getGraphicsQuality()).append(", mapDetail=").append(st.isMapDetailHigh() ? "high" : "lite");
        }
        catch (Throwable throwable) {
            // empty catch block
        }
        return sb.toString();
    }

    private static String screenName(Screen s) {
        if (s == null) {
            return "none";
        }
        if (s instanceof MapScreen) {
            return "MapScreen";
        }
        if (s instanceof LoadingScreen) {
            return "LoadingScreen";
        }
        if (s instanceof MainMenuScreen) {
            return "MainMenuScreen";
        }
        if (s instanceof ScenarioSelectScreen) {
            return "ScenarioSelectScreen";
        }
        if (s instanceof CrashScreen) {
            return "CrashScreen";
        }
        return s.getClass().getSimpleName();
    }

    private static void paceAndroidFrame(long frameStartNanos) {
        if (Gdx.app == null || Gdx.app.getType() != Application.ApplicationType.Android) {
            return;
        }
        int targetFPS = SettingsManager.getInstance().getTargetFPS();
        if (targetFPS <= 0) {
            paceDeadlineNanos = 0L;
            return;
        }
        int refreshHz = cgsGame.displayRefreshHz();
        if (refreshHz > 0 && targetFPS >= refreshHz - 1) {
            paceDeadlineNanos = 0L;
            return;
        }
        long frameTimeNanos = 1000000000L / (long)targetFPS;
        long now0 = System.nanoTime();
        long deadline = paceDeadlineNanos + frameTimeNanos;
        if (paceDeadlineNanos == 0L || now0 - deadline > frameTimeNanos) {
            deadline = Math.max(now0, frameStartNanos + frameTimeNanos);
        }
        paceDeadlineNanos = deadline;
        long busyWaitNanos = 1000000L;
        long sleepUntilNanos = deadline - busyWaitNanos;
        long now = System.nanoTime();
        if (sleepUntilNanos > now) {
            long sleepNanos = sleepUntilNanos - now;
            try {
                Thread.sleep(sleepNanos / 1000000L, (int)(sleepNanos % 1000000L));
            }
            catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }
        }
        while (System.nanoTime() < deadline) {
        }
    }

    private static int displayRefreshHz() {
        long now = System.nanoTime();
        if (paceRefreshReadNanos == 0L || now - paceRefreshReadNanos > 2000000000L) {
            paceRefreshReadNanos = now;
            try {
                Graphics.DisplayMode mode = Gdx.graphics.getDisplayMode();
                paceRefreshHz = mode != null ? mode.refreshRate : 0;
            }
            catch (Throwable t) {
                paceRefreshHz = 0;
            }
        }
        return paceRefreshHz;
    }

    private void renderInternal() {
        if (this.sharedBatch != null && this.sharedBatch.isDrawing()) {
            try {
                this.sharedBatch.end();
                Gdx.app.error("cgsGame", "Shared SpriteBatch was left begun by the previous frame (screen: " + (this.getScreen() != null ? this.getScreen().getClass().getSimpleName() : "none") + ") \u2014 closed it so this frame can draw.");
            }
            catch (Throwable throwable) {
                // empty catch block
            }
        }
        float delta = Gdx.graphics.getDeltaTime();
        this.syncTiltSensor();

        if (this.sharedBatch != null) {
            try {
                cgs.modloader.ModLoader.getInstance().onRender(this.sharedBatch, delta);
            } catch(Throwable t) {}
        }

        if (this.activeMultiplayerManager != null) {
            this.activeMultiplayerManager.update(delta);
        }
        if (Gdx.graphics.getWidth() <= 0 || Gdx.graphics.getHeight() <= 0) {
            return;
        }
        MusicManager.getInstance().update(delta);
        if (this.warVignetteActive) {
            this.warVignetteBlend = Math.min(1.0f, this.warVignetteBlend + delta * 3.0f);
            this.warPulseTimer += delta;
        } else {
            this.warVignetteBlend = Math.max(0.0f, this.warVignetteBlend - delta * 3.0f);
            if (this.warVignetteBlend <= 0.0f) {
                this.warPulseTimer = 0.0f;
            }
        }
        if (this.epochVignetteActive) {
            this.epochVignetteBlend = Math.min(1.0f, this.epochVignetteBlend + delta * 0.75f);
            this.epochPulseTimer += delta;
        } else {
            this.epochVignetteBlend = Math.max(0.0f, this.epochVignetteBlend - delta * 0.75f);
            if (this.epochVignetteBlend <= 0.0f) {
                this.epochPulseTimer = 0.0f;
            }
        }
        if (this.transitionState != TransitionState.IDLE) {
            this.transitionTimer += delta;
            if (this.transitionState == TransitionState.CLOSING) {
                if (this.transitionTimer >= 0.15f) {
                    if (this.pendingScreen != null) {
                        this.setScreen(this.pendingScreen);
                        this.pendingScreen = null;
                    }
                    this.transitionState = TransitionState.OPENING;
                    this.transitionTimer = 0.0f;
                }
            } else if (this.transitionState == TransitionState.OPENING && this.transitionTimer >= 0.15f) {
                this.transitionState = TransitionState.IDLE;
                this.transitionTimer = 0.0f;
                if (this.screenToDispose != null) {
                    this.screenToDispose.dispose();
                    this.screenToDispose = null;
                }
            }
        }
        AchievementNotification.getInstance().update(delta);
        if (this.sharedBatch != null && this.sharedBatch.isDrawing()) {
            Gdx.app.error("cgsGame", "Recovered stuck shared batch (previous frame threw mid-draw)");
            try {
                this.sharedBatch.end();
            }
            catch (Throwable throwable) {
                // empty catch block
            }
        }
        if (this.vignetteBatch != null && this.vignetteBatch.isDrawing()) {
            Gdx.app.error("cgsGame", "Recovered stuck vignette batch (previous frame threw mid-draw)");
            try {
                this.vignetteBatch.end();
            }
            catch (Throwable throwable) {
                // empty catch block
            }
        }
        super.render();
        if (!this.skipAutoVignette) {
            this.drawVignetteOverlay();
        }
        if (this.skipAutoVignette && this.transitionState != TransitionState.IDLE) {
            this.drawTransitionOnly();
        }
        if (this.splashCurtain != null) {
            boolean stillUp;
            try {
                stillUp = this.splashCurtain.drawCurtain(delta);
            }
            catch (Throwable t) {
                Gdx.app.error("cgsGame", "Splash curtain failed; showing the screen without it", t);
                stillUp = false;
            }
            if (!stillUp) {
                this.dropSplashCurtain();
            }
        }
        FullVersionShowcase.renderOverlay(delta);
        AchievementNotification.getInstance().draw();
        if (this.sharedBatch != null) {
            boolean wasDrawing = this.sharedBatch.isDrawing();
            if (!wasDrawing) {
                this.sharedBatch.begin();
            }
            try {
                cgs.modloader.ModLoader.getInstance().onPostRender(this.sharedBatch, delta);
            } catch (Throwable t) {}
            if (!wasDrawing && this.sharedBatch.isDrawing()) {
                this.sharedBatch.end();
            }
        }
    }

    private void drawVignetteOverlay() {
        float invert;
        float colorB;
        float colorG;
        float colorR;
        float baseRadius;
        float baseIntensity;
        float pulseNorm;
        float pulse;
        if (this.vignetteShader == null || !this.vignetteShader.isCompiled()) {
            return;
        }
        if (this.warVignetteBlend > 0.001f) {
            pulse = (float)Math.sin(this.warPulseTimer * 4.0f);
            pulseNorm = pulse * 0.5f + 0.5f;
            float warIntensity = 0.2f + 0.2f * pulseNorm;
            baseIntensity = cgsGame.lerp(0.35f, warIntensity, this.warVignetteBlend);
            baseRadius = cgsGame.lerp(0.5f, 0.4f, this.warVignetteBlend);
            colorR = cgsGame.lerp(0.0f, 0.6f, this.warVignetteBlend);
            colorG = 0.0f;
            colorB = 0.0f;
        } else if (this.epochVignetteBlend > 0.001f) {
            pulse = (float)Math.sin(this.epochPulseTimer * 2.5f);
            pulseNorm = pulse * 0.5f + 0.5f;
            float epochIntensity = 0.4f + 0.29999998f * pulseNorm;
            baseIntensity = cgsGame.lerp(0.35f, epochIntensity, this.epochVignetteBlend);
            baseRadius = cgsGame.lerp(0.5f, 0.45f, this.epochVignetteBlend);
            colorR = cgsGame.lerp(0.0f, this.epochColorR, this.epochVignetteBlend);
            colorG = cgsGame.lerp(0.0f, this.epochColorG, this.epochVignetteBlend);
            colorB = cgsGame.lerp(0.0f, this.epochColorB, this.epochVignetteBlend);
        } else {
            baseIntensity = Math.min(1.0f, 0.35f * this.idleVignetteScale);
            baseRadius = 0.5f;
            colorR = 0.0f;
            colorG = 0.0f;
            colorB = 0.0f;
        }
        float transIntensity = 0.0f;
        float transRadius = baseRadius;
        if (this.transitionState != TransitionState.IDLE) {
            switch (this.transitionState) {
                case CLOSING: {
                    float t = Math.min(this.transitionTimer / 0.15f, 1.0f);
                    t = t * t * (3.0f - 2.0f * t);
                    transIntensity = 0.55f * t;
                    transRadius = 0.6f * (1.0f - t);
                    break;
                }
                case OPENING: {
                    float t = Math.min(this.transitionTimer / 0.15f, 1.0f);
                    t = t * t * (3.0f - 2.0f * t);
                    transIntensity = 0.55f * (1.0f - t);
                    transRadius = 0.6f * t;
                    break;
                }
            }
        }
        float finalIntensity = Math.max(baseIntensity, transIntensity);
        float finalRadius = Math.min(baseRadius, transRadius);
        if (transIntensity > baseIntensity) {
            colorR = 0.0f;
            colorG = 0.0f;
            colorB = 0.0f;
        }
        int w = Gdx.graphics.getWidth();
        int h = Gdx.graphics.getHeight();
        this.vignetteProjection.setToOrtho2D(0.0f, 0.0f, w, h);
        this.vignetteBatch.setProjectionMatrix(this.vignetteProjection);
        this.vignetteBatch.setShader(this.vignetteShader);
        this.vignetteBatch.begin();
        this.vignetteShader.setUniformf("u_intensity", finalIntensity);
        this.vignetteShader.setUniformf("u_radius", finalRadius);
        this.vignetteShader.setUniformf("u_color", colorR, colorG, colorB);
        float f = invert = this.warVignetteBlend < 0.001f ? this.epochVignetteBlend : 0.0f;
        if (this.transitionState != TransitionState.IDLE) {
            invert = 0.0f;
        }
        this.vignetteShader.setUniformf("u_invert", invert);
        this.vignetteBatch.setColor(1.0f, 1.0f, 1.0f, 1.0f);
        this.vignetteBatch.draw(this.white1x1, 0.0f, 0.0f, (float)w, (float)h);
        this.vignetteBatch.end();
        this.vignetteBatch.setShader(null);
    }

    private void drawTransitionOnly() {
        float radius;
        float intensity;
        if (this.vignetteShader == null || !this.vignetteShader.isCompiled()) {
            return;
        }
        if (this.transitionState == TransitionState.IDLE) {
            return;
        }
        switch (this.transitionState) {
            case CLOSING: {
                float t = Math.min(this.transitionTimer / 0.15f, 1.0f);
                t = t * t * (3.0f - 2.0f * t);
                intensity = 0.55f * t;
                radius = 0.6f * (1.0f - t);
                break;
            }
            case OPENING: {
                float t = Math.min(this.transitionTimer / 0.15f, 1.0f);
                t = t * t * (3.0f - 2.0f * t);
                intensity = 0.55f * (1.0f - t);
                radius = 0.6f * t;
                break;
            }
            default: {
                return;
            }
        }
        int w = Gdx.graphics.getWidth();
        int h = Gdx.graphics.getHeight();
        this.vignetteProjection.setToOrtho2D(0.0f, 0.0f, w, h);
        this.vignetteBatch.setProjectionMatrix(this.vignetteProjection);
        this.vignetteBatch.setShader(this.vignetteShader);
        this.vignetteBatch.begin();
        this.vignetteShader.setUniformf("u_intensity", intensity);
        this.vignetteShader.setUniformf("u_radius", radius);
        this.vignetteShader.setUniformf("u_color", 0.0f, 0.0f, 0.0f);
        this.vignetteShader.setUniformf("u_invert", 0.0f);
        this.vignetteBatch.setColor(1.0f, 1.0f, 1.0f, 1.0f);
        this.vignetteBatch.draw(this.white1x1, 0.0f, 0.0f, (float)w, (float)h);
        this.vignetteBatch.end();
        this.vignetteBatch.setShader(null);
    }

    public MultiplayerManager getActiveMultiplayerManager() {
        return this.activeMultiplayerManager;
    }

    private static float lerp(float a, float b, float t) {
        return a + (b - a) * t;
    }

    @Override
    public void pause() {
        RenderWatchdog.suspend();
        super.pause();
        this.isPaused = true;
        Gdx.app.log("cgsGame", "pause() \u2014 window minimized/hidden");
        SettingsManager.getInstance().markGles3SessionSurvived();
        MusicManager.getInstance().onAppPause();
        if (this.sharedBatch != null && this.sharedBatch.isDrawing()) {
            try {
                this.sharedBatch.end();
            }
            catch (Exception exception) {
                // empty catch block
            }
        }
        if (this.vignetteBatch != null && this.vignetteBatch.isDrawing()) {
            try {
                this.vignetteBatch.end();
            }
            catch (Exception exception) {
                // empty catch block
            }
        }
    }

    @Override
    public void resume() {
        Screen currentScreen;
        boolean isDesktop;
        RenderWatchdog.unsuspend();
        super.resume();
        Gdx.app.log("cgsGame", "resume() \u2014 window restored");
        int w = Gdx.graphics.getWidth();
        int h = Gdx.graphics.getHeight();
        if (w > 0 && h > 0) {
            CFG.GAME_WIDTH = w;
            CFG.GAME_HEIGHT = h;
        }
        boolean bl = isDesktop = Gdx.app.getType() == Application.ApplicationType.Desktop;
        if (!isDesktop) {
            if (this.white1x1 != null) {
                try {
                    this.white1x1.dispose();
                }
                catch (Exception exception) {
                    // empty catch block
                }
            }
            Pixmap pix = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
            pix.setColor(1.0f, 1.0f, 1.0f, 1.0f);
            pix.fill();
            this.white1x1 = new Texture(pix);
            pix.dispose();
            if (this.vignetteShader != null && !this.vignetteShader.isCompiled()) {
                String vertSrc = Gdx.files.internal("shaders/vignette.vert").readString();
                String fragSrc = Gdx.files.internal("shaders/vignette.frag").readString();
                ShaderProgram.pedantic = false;
                this.vignetteShader.dispose();
                this.vignetteShader = new ShaderProgram(vertSrc, fragSrc);
                if (!this.vignetteShader.isCompiled()) {
                    Gdx.app.error("VignetteShader", "Compilation failed:\n" + this.vignetteShader.getLog());
                }
            }
            CharmArt.dispose();
        }
        if (this.sharedBatch != null && this.sharedBatch.isDrawing()) {
            try {
                this.sharedBatch.end();
            }
            catch (Exception pix) {
                // empty catch block
            }
        }
        if (this.vignetteBatch != null && this.vignetteBatch.isDrawing()) {
            try {
                this.vignetteBatch.end();
            }
            catch (Exception pix) {
                // empty catch block
            }
        }
        if ((currentScreen = this.getScreen()) != null && w > 0 && h > 0) {
            currentScreen.resize(w, h);
        }
        MusicManager.getInstance().onAppResume();
        this.isPaused = false;
    }

    @Override
    public void dispose() {
        try {
            MusicManager.getInstance().stopAll();
        }
        catch (Throwable throwable) {
            // empty catch block
        }
        Thread exitThread = new Thread(new Runnable(){

            @Override
            public void run() {
                try {
                    Thread.sleep(800L);
                }
                catch (InterruptedException interruptedException) {
                    // empty catch block
                }
                System.out.flush();
                System.err.flush();
                Runtime.getRuntime().halt(0);
            }
        }, "JVM-Exit-Safety");
        exitThread.setDaemon(true);
        exitThread.start();
        try {
            SettingsManager.getInstance().save();
        }
        catch (Exception e) {
            Gdx.app.error("cgsGame", "Error saving settings: " + e.getMessage());
        }
        PrefsWriter.drain();
        try {
            BackgroundManager.getInstance().dispose();
        }
        catch (Exception e) {
            Gdx.app.error("cgsGame", "Error disposing backgrounds: " + e.getMessage());
        }
        try {
            this.disconnectMultiplayer();
        }
        catch (Exception e) {
            Gdx.app.error("cgsGame", "Error disconnecting multiplayer: " + e.getMessage());
        }
        Screen currentScreen = null;
        try {
            currentScreen = this.getScreen();
            if (currentScreen != null) {
                currentScreen.dispose();
            }
        }
        catch (Exception e) {
            Gdx.app.error("cgsGame", "Error disposing current screen: " + e.getMessage());
        }
        try {
            if (this.mainMenuScreen != null && this.mainMenuScreen != currentScreen) {
                this.mainMenuScreen.dispose();
            }
        }
        catch (Exception e) {
            Gdx.app.error("cgsGame", "Error disposing mainMenuScreen: " + e.getMessage());
        }
        try {
            if (this.scenarioSelectScreen != null && this.scenarioSelectScreen != currentScreen) {
                this.scenarioSelectScreen.dispose();
            }
        }
        catch (Exception e) {
            Gdx.app.error("cgsGame", "Error disposing scenarioSelectScreen: " + e.getMessage());
        }
        try {
            if (this.settingsScreen != null && this.settingsScreen != currentScreen) {
                this.settingsScreen.dispose();
            }
        }
        catch (Exception e) {
            Gdx.app.error("cgsGame", "Error disposing settingsScreen: " + e.getMessage());
        }
        try {
            if (this.multiplayerLobbyScreen != null && this.multiplayerLobbyScreen != currentScreen) {
                this.multiplayerLobbyScreen.dispose();
            }
        }
        catch (Exception e) {
            Gdx.app.error("cgsGame", "Error disposing multiplayerLobbyScreen: " + e.getMessage());
        }
        try {
            if (this.saveLoadScreen != null && this.saveLoadScreen != currentScreen) {
                this.saveLoadScreen.dispose();
            }
        }
        catch (Exception e) {
            Gdx.app.error("cgsGame", "Error disposing saveLoadScreen: " + e.getMessage());
        }
        try {
            BatchFade.dispose();
            UiFx.dispose();
            PanelLayer.dispose();
        }
        catch (Exception e) {
            Gdx.app.error("cgsGame", "Error disposing UI effect shaders: " + e.getMessage());
        }
        try {
            if (this.sharedBatch != null) {
                this.sharedBatch.dispose();
            }
        }
        catch (Exception e) {
            Gdx.app.error("cgsGame", "Error disposing sharedBatch: " + e.getMessage());
        }
        try {
            if (this.vignetteBatch != null) {
                this.vignetteBatch.dispose();
            }
        }
        catch (Exception e) {
            Gdx.app.error("cgsGame", "Error disposing vignetteBatch: " + e.getMessage());
        }
        try {
            if (this.vignetteShader != null) {
                this.vignetteShader.dispose();
            }
        }
        catch (Exception e) {
            Gdx.app.error("cgsGame", "Error disposing vignetteShader: " + e.getMessage());
        }
        try {
            if (this.white1x1 != null) {
                this.white1x1.dispose();
            }
        }
        catch (Exception e) {
            Gdx.app.error("cgsGame", "Error disposing white1x1: " + e.getMessage());
        }
        try {
            if (this.mcpServer != null) {
                this.mcpServer.stop();
            }
        }
        catch (Exception e) {
            Gdx.app.error("cgsGame", "Error stopping mcpServer: " + e.getMessage());
        }
        try {
            if (this.modManagerScreen != null && this.modManagerScreen != currentScreen) {
                this.modManagerScreen.dispose();
            }
        }
        catch (Exception e) {
            Gdx.app.error("cgsGame", "Error disposing modManagerScreen: " + e.getMessage());
        }
        try {
            MusicManager.getInstance().dispose();
        }
        catch (Exception e) {
            Gdx.app.error("cgsGame", "Error disposing MusicManager: " + e.getMessage());
        }
        try {
            SoundManager.getInstance().dispose();
        }
        catch (Exception e) {
            Gdx.app.error("cgsGame", "Error disposing SoundManager: " + e.getMessage());
        }
        try {
            CountryFlagManager.getInstance().dispose();
        }
        catch (Exception e) {
            Gdx.app.error("cgsGame", "Error disposing CountryFlagManager: " + e.getMessage());
        }
        try {
            ImageManager.dispose();
        }
        catch (Exception e) {
            Gdx.app.error("cgsGame", "Error disposing ImageManager: " + e.getMessage());
        }
        try {
            CursorManager.dispose();
        }
        catch (Exception e) {
            Gdx.app.error("cgsGame", "Error disposing CursorManager: " + e.getMessage());
        }
    }

    private static enum TransitionState {
        IDLE,
        CLOSING,
        OPENING;

    }

    private static interface ScreenBuilder {
        public Screen build();
    }

    static final class SlowLineGate {
        private boolean printed;
        private long lastLineNanos;
        private int held;
        private long heldMaxMs;

        SlowLineGate() {
        }

        String admit(long ms) {
            long now = System.nanoTime();
            if (this.printed && now - this.lastLineNanos < 1000000000L) {
                ++this.held;
                if (ms > this.heldMaxMs) {
                    this.heldMaxMs = ms;
                }
                return null;
            }
            this.printed = true;
            this.lastLineNanos = now;
            if (this.held == 0) {
                return "";
            }
            String more = " (+" + this.held + " more, max " + this.heldMaxMs + " ms)";
            this.held = 0;
            this.heldMaxMs = 0L;
            return more;
        }
    }
}

