/*
 * Decompiled with CFR 0.152.
 */
package ru.ius.david.cgs;

import com.badlogic.gdx.Application;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.InputAdapter;
import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.utils.TimeUtils;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import ru.ius.david.cgs.CFG;
import ru.ius.david.cgs.Touch;
import ru.ius.david.cgs.cgsGame;
import ru.ius.david.cgs.effects.BatchFade;
import ru.ius.david.cgs.effects.Ease;
import ru.ius.david.cgs.effects.Glide;
import ru.ius.david.cgs.effects.SlidingIndicator;
import ru.ius.david.cgs.effects.StaggerReveal;
import ru.ius.david.cgs.effects.ThemeReveal;
import ru.ius.david.cgs.effects.UiFx;
import ru.ius.david.cgs.elements.ButtonElement;
import ru.ius.david.cgs.elements.MenuElement;
import ru.ius.david.cgs.game.CharmCollection;
import ru.ius.david.cgs.game.LanguageManager;
import ru.ius.david.cgs.game.SaveManager;
import ru.ius.david.cgs.game.SettingsManager;
import ru.ius.david.cgs.images.BackgroundManager;
import ru.ius.david.cgs.images.Image;
import ru.ius.david.cgs.images.ImageManager;
import ru.ius.david.cgs.images.Images;
import ru.ius.david.cgs.images.MenuWorldMap;
import ru.ius.david.cgs.managers.AchievementManager;
import ru.ius.david.cgs.managers.HapticManager;
import ru.ius.david.cgs.managers.MusicManager;
import ru.ius.david.cgs.managers.SoundManager;
import ru.ius.david.cgs.menus.CGSInGame;
import ru.ius.david.cgs.menus.CharmBoard;
import ru.ius.david.cgs.menus.CharmCheat;
import ru.ius.david.cgs.menus.SliderMenu;
import ru.ius.david.cgs.menus.ThemeToggle;
import ru.ius.david.cgs.menus.TopBarRenderer;
import ru.ius.david.cgs.menus.WhatsNewBlock;
import ru.ius.david.cgs.ui.CGSConfirmDialog;
import ru.ius.david.cgs.utils.DotGrid;

public class MainMenuScreen
extends ScreenAdapter {
    public cgsGame getGame() { return this.game; }

    private cgsGame game;
    private SpriteBatch batch;
    private Matrix4 uiProjection;
    private SliderMenu menuPanel;
    private static final Color COLOR_BG_DARK = new Color(0.03f, 0.03f, 0.08f, 1.0f);
    private static final Color COLOR_SUBTITLE = new Color(0.74f, 0.73f, 0.8f, 0.95f);
    private static final Color TUTORIAL_GOLD = new Color(Color.valueOf("d9a72eff"));
    private static final float LOGO_WIDTH_OF_PANEL = 0.76f;
    private float fadeInTimer = 0.0f;
    private static final float FADE_IN_DURATION = 0.8f;
    private boolean introPending = false;
    private long introStartNanos = 0L;
    private static final float INTRO_TIME = 0.7f;
    private static final float INTRO_LOGO_TIME = 0.42f;
    private static final int INTRO_LOGO_DROP = 18;
    private static final float INTRO_RULE_START = 0.06f;
    private static final float INTRO_RULE_TIME = 0.34f;
    private final StaggerReveal rowReveal = new StaggerReveal();
    private static final int INTRO_ROW_RISE = 16;
    private static final float INTRO_SIDE_START = 0.16f;
    private static final float INTRO_SIDE_TIME = 0.3f;
    private static final float INTRO_POP_STEP = 0.035f;
    private final SlidingIndicator rowMarker = new SlidingIndicator();
    private final Glide rowMarkerShown = new Glide();
    private static final int PARTICLE_COUNT = 50;
    private ArrayList<Particle> particles;
    private ArrayList<CGSInGame.Spark> achSparks = new ArrayList();
    private float parallaxX = 0.0f;
    private float parallaxY = 0.0f;
    private float targetParallaxX = 0.0f;
    private float targetParallaxY = 0.0f;
    private CharmBoard charmBoard;
    private final CGSConfirmDialog charmConfirm = new CGSConfirmDialog();
    private ButtonElement playRow;
    private ButtonElement continueRow;
    private final ArrayList<ButtonElement> charmRows = new ArrayList();
    private final ArrayList<String> charmRowIds = new ArrayList();
    private float prevParallaxX = 0.0f;
    private final CharmCheat charmCheat = new CharmCheat();
    private String charmToast;
    private float charmToastLife;
    private static final float CHARM_TOAST_TIME = 4.5f;
    private final Color tmpFpsColor = new Color();
    private final ArrayList<IconButton> socialButtons = new ArrayList();
    private final ArrayList<IconButton> cornerButtons = new ArrayList();
    private IconButton achButton;
    private IconButton statButton;
    private IconButton pressedCornerButton = null;
    private boolean socialDialogVisible = false;
    private IconButton activeSocialButton = null;
    private float socialDialogTimer = 0.0f;
    private int socialDialogPressedIndex = -1;
    private int socialDialogHoverIndex = -1;
    private int socialDialogX;
    private int socialDialogY;
    private int socialDialogW;
    private int socialDialogH;
    private int socialBtnW;
    private int socialBtnH;
    private int socialBtnConfirmX;
    private int socialBtnConfirmY;
    private int socialBtnCancelX;
    private int socialBtnCancelY;
    private ArrayList<DialogParticle> dialogParticles = new ArrayList();
    private float dialogParticleTimer = 0.0f;
    private boolean gratitudeDialogVisible = false;
    private float gratitudeDialogTimer = 0.0f;
    private int gratitudeDialogPressedIndex = -1;
    private int gratitudeDialogHoverIndex = -1;
    private int gratitudeDialogX;
    private int gratitudeDialogY;
    private int gratitudeDialogW;
    private int gratitudeDialogH;
    private int gratitudeBtnW;
    private int gratitudeBtnH;
    private int gratitudeBtnConfirmX;
    private int gratitudeBtnConfirmY;
    private static final Color EMBER_RED = new Color(1.0f, 0.2f, 0.05f, 1.0f);
    private static final Color EMBER_ORANGE = new Color(1.0f, 0.5f, 0.0f, 1.0f);
    private static final Color EMBER_YELLOW = new Color(1.0f, 0.8f, 0.1f, 1.0f);
    private float inkTimer = 0.0f;
    private int btnW;
    private int btnH;
    private int gap;
    private int pad;
    private int separatorDrawY;
    private int separatorDrawY2;
    private int separatorDrawY3;
    private int panelX;
    private int panelY;
    private int panelW;
    private int panelH;
    private float scrollOffset = 0.0f;
    private float maxScrollOffset = 0.0f;
    private boolean isDraggingScroll = false;
    private float dragStartY = 0.0f;
    private float dragStartScrollOffset = 0.0f;
    private float scrollVelocity = 0.0f;
    private boolean hasSaves = false;
    private int headerY;
    private int logoW;
    private int logoH;
    private boolean pressedOnMap = false;
    private long lastMapStrokeMs = 0L;
    private static final long PAINT_SPREE_MS = 1500L;
    private final WhatsNewBlock whatsNew = new WhatsNewBlock();
    private final ThemeToggle themeToggle = new ThemeToggle();
    private final ThemeReveal themeReveal = new ThemeReveal();
    private final Runnable switchTheme = new Runnable(){

        @Override
        public void run() {
            SettingsManager settings = SettingsManager.getInstance();
            settings.setUiTheme(CFG.isDarkTheme() ? 0 : 1);
            try {
                settings.save();
            }
            catch (Exception e) {
                Gdx.app.log("MainMenuScreen", "Could not store the theme");
            }
            MainMenuScreen.this.themeToggle.playSwitch();
        }
    };
    private int focusedIndex = -1;
    private boolean keyboardFocusVisible = false;
    private TopBarRenderer topBarRenderer;
    private ShaderProgram inkShader;
    private boolean inkShaderDisposed;
    private int cornerTopY;
    private int cornerLeftX;
    private int pairLeftX;
    private int linksLeftX;
    private int linksTopY;
    private int linksBottomY;
    private boolean clusterInCorner;
    private boolean bannerOn;
    private int bannerX;
    private int bannerY;
    private int bannerW;
    private int bannerH;
    private int bannerTextW;
    private int bannerNoteH;
    private int bannerPadX;
    private static final Color LOGO_GLOW = new Color(1.0f, 0.74f, 0.22f, 1.0f);
    private final Color TMP_INK = new Color();

    public MainMenuScreen(cgsGame game) {
        this.game = game;
        this.batch = game.getSharedBatch();
        this.topBarRenderer = TopBarRenderer.forMenuScreens();
        this.initBackgrounds();
        this.initParticles();
        this.initInkShader();
        this.refreshSaveState();
        this.buildCornerButtons();
        this.themeToggle.setOnClick(new Runnable(){

            @Override
            public void run() {
                MainMenuScreen.this.themeReveal.request(MainMenuScreen.this.themeToggle.centerX(), MainMenuScreen.this.themeToggle.centerY());
            }
        });
        this.buildMenu();
        this.updateUIProjection();
    }

    private void buildCornerButtons() {
        Color gold = new Color(0.95f, 0.75f, 0.2f, 1.0f);
        this.statButton = new IconButton(Images.ic_nav_upgrades_white, LanguageManager.get("menu_statistics"), gold, null, new Runnable(){

            @Override
            public void run() {
                MainMenuScreen.this.game.showStatistics();
            }
        }, 12, true);
        this.achButton = new IconButton(Images.icon_achievement, LanguageManager.get("ach_screen_title"), gold, null, new Runnable(){

            @Override
            public void run() {
                MainMenuScreen.this.game.showAchievements();
            }
        }, 12, true);
        Color tgBlue = new Color(0.12f, 0.6f, 0.88f, 1.0f);
        this.socialButtons.clear();
        this.socialButtons.add(new IconButton(Images.icon_social_steam, "Steam", new Color(0.4f, 0.72f, 0.95f, 1.0f), "https://store.steampowered.com/app/4804480/Age_of_Clicks/", null, 10, false, "menu_social_steam"));
        this.socialButtons.add(new IconButton(Images.icon_social_yt, "YouTube", new Color(0.9f, 0.12f, 0.12f, 1.0f), "https://www.youtube.com/@RedBorderStudio", null, 10, false, "menu_social_youtube"));
        this.socialButtons.add(new IconButton(Images.icon_social_tg_chat, LanguageManager.get("menu_social_telegram_chat_title"), tgBlue, "https://t.me/AoHIIB", null, 10, false, "menu_social_telegram_chat"));
        this.socialButtons.add(new IconButton(Images.icon_social_tg, "Telegram", tgBlue, "https://t.me/RedBorderStudio", null, 10, false, "menu_social_telegram"));
        this.socialButtons.add(new IconButton(Images.icon_social_discord, "Discord", new Color(0.345f, 0.396f, 0.949f, 1.0f), "https://discord.gg/uXpGZX4pgC", null, 10, false, "menu_social_discord"));
        this.cornerButtons.clear();
        this.cornerButtons.add(this.statButton);
        this.cornerButtons.add(this.achButton);
        this.cornerButtons.addAll(this.socialButtons);
    }

    private void initInkShader() {
        if (this.inkShader != null) {
            return;
        }
        try {
            ShaderProgram.pedantic = false;
            String vert = Gdx.files.internal("shaders/ink_banner.vert").readString();
            String frag = Gdx.files.internal("shaders/dark_cloud_plate.frag").readString();
            this.inkShader = new ShaderProgram(vert, frag);
            if (!this.inkShader.isCompiled()) {
                Gdx.app.error("MainMenuScreen", "inkShader compilation error: " + this.inkShader.getLog());
            }
        }
        catch (Exception e) {
            Gdx.app.error("MainMenuScreen", "Failed to load inkShader", e);
        }
    }

    private void refreshSaveState() {
        this.hasSaves = SaveManager.hasAnySave();
    }

    private void initBackgrounds() {
    }

    private void initParticles() {
        this.particles = new ArrayList();
        int W = Gdx.graphics.getWidth();
        int H = Gdx.graphics.getHeight();
        for (int i = 0; i < 50; ++i) {
            this.particles.add(new Particle(W, H));
        }
    }

    private void updateUIProjection() {
        int w = Math.max(1, CFG.GAME_WIDTH);
        int h = Math.max(1, CFG.GAME_HEIGHT);
        this.uiProjection = new Matrix4();
        this.uiProjection.setToOrtho2D(0.0f, 0.0f, w, h);
        this.uiProjection.val[5] = -2.0f / (float)h;
        this.uiProjection.val[13] = 1.0f;
    }

    private int lineHMain() {
        return CFG.fontMain != null ? (int)Math.ceil(CFG.fontMain.getLineHeight()) : Math.round(17.0f * CFG.UI_SCALE * CFG.FONT_SCALE);
    }

    private int lineHSmall() {
        return CFG.fontSmall != null ? (int)Math.ceil(CFG.fontSmall.getLineHeight()) : Math.round(13.0f * CFG.UI_SCALE * CFG.FONT_SCALE);
    }

    private boolean isNarrow() {
        return CFG.GAME_WIDTH < CFG.s(780);
    }

    private int logoGap() {
        return CFG.s(10);
    }

    private int headerHeight() {
        return this.logoH + this.logoGap();
    }

    private float logoAspect() {
        Image img = ImageManager.getImage(Images.logo_game);
        if (img != null && img.getTexture() != null && img.getTexture().getHeight() > 0) {
            return (float)img.getTexture().getWidth() / (float)img.getTexture().getHeight();
        }
        return 2.8008752f;
    }

    private void buildMenu() {
        int W = CFG.GAME_WIDTH;
        int H = CFG.GAME_HEIGHT;
        this.btnW = Math.min(CFG.s(330), W - CFG.s(60));
        this.btnH = Math.max(CFG.s(54), this.lineHMain() + CFG.s(26));
        this.gap = CFG.s(12);
        this.pad = CFG.s(26);
        int separatorH = CFG.s(18);
        ArrayList<MenuElement> elements = new ArrayList<MenuElement>();
        this.panelW = this.btnW + this.pad * 2;
        this.panelX = (W - this.panelW) / 2;
        int btnX = this.panelX + this.pad;
        cgs.modloader.api.ModMenuHelper modMenuHelper = new cgs.modloader.api.ModMenuHelper();
        try {
            cgs.modloader.ModLoader.getInstance().onMainMenuBuild(this, modMenuHelper);
        } catch(Throwable t) {
            System.err.println("[ModLoader] Error in onMainMenuBuild: " + t.getMessage());
        }
        java.util.List<cgs.modloader.api.ModMenuHelper.ModMenuItem> modItems = modMenuHelper.getCustomButtons();
        int rows = (this.hasSaves ? 8 : 7) + modItems.size();
        int contentH = rows * this.btnH + (rows - 1) * this.gap + separatorH * 3;
        this.layoutCornerSlots(W, H);
        this.whatsNew.layout(this.panelX, 0, this.panelW, 0, W, H, true);
        boolean stripUnder = this.whatsNew.companionInRow();
        this.layoutBanner(W, H, stripUnder);
        int stripBand = stripUnder ? this.whatsNew.stripHeight() + CFG.s(18) : 0;
        int bottomReserve = 0;
        int stripRoom = 0;
        int minPanelH = 3 * this.btnH + 2 * this.gap + this.pad * 2;
        int minHeader = Math.min(Math.round((float)this.panelW * 0.76f / this.logoAspect()), CFG.s(56)) + this.logoGap();
        for (int pass = 0; pass < 2; ++pass) {
            boolean cramped;
            int noteBand;
            int n = noteBand = this.bannerOn ? H - this.bannerY + CFG.s(10) : 0;
            if (!this.clusterInCorner) {
                bottomReserve = H - (this.bannerOn ? this.bannerY : this.cornerTopY) + CFG.s(8);
                stripRoom = stripBand;
            } else {
                bottomReserve = 0;
                stripRoom = stripUnder ? stripBand + noteBand : Math.max(CFG.s(60), noteBand);
            }
            boolean bl = cramped = H - minHeader - bottomReserve - stripRoom - CFG.s(24) < minPanelH;
            if (!this.bannerOn || !cramped) break;
            this.bannerOn = false;
        }
        float logoAspect = this.logoAspect();
        int wantLogoH = Math.round((float)this.panelW * 0.76f / logoAspect);
        int roomLogoH = H - bottomReserve - stripRoom - CFG.s(24) - (contentH + this.pad * 2) - this.logoGap();
        this.logoH = Math.max(Math.min(wantLogoH, roomLogoH), Math.min(wantLogoH, CFG.s(56)));
        this.logoW = Math.round((float)this.logoH * logoAspect);
        int header = this.headerHeight();
        this.panelH = Math.min(contentH + this.pad * 2, H - header - bottomReserve - stripRoom - CFG.s(24));
        int blockH = header + this.panelH + stripRoom;
        this.headerY = Math.max(CFG.s(12), (H - bottomReserve - blockH) / 2);
        this.panelY = this.headerY + header;
        int clipH = this.panelH - this.pad * 2;
        this.maxScrollOffset = Math.max(0, contentH - clipH);
        this.scrollOffset = Math.max(0.0f, Math.min(this.maxScrollOffset, this.scrollOffset));
        int y = this.panelY + this.pad;
        this.continueRow = null;
        this.charmRows.clear();
        this.charmRowIds.clear();
        if (this.hasSaves) {
            ButtonElement btnContinue = this.row(btnX, y, this.btnW, this.btnH, LanguageManager.get("menu_load_game").toUpperCase(), Images.ic_folder, this.game.isGenuine() ? CFG.COLOR_RED_BTN : Color.GRAY, this.game.isGenuine() ? Color.WHITE : Color.LIGHT_GRAY, new Runnable(){

                @Override
                public void run() {
                    if (!MainMenuScreen.this.game.isGenuine()) {
                        return;
                    }
                    MainMenuScreen.this.game.showSaveLoadScreen();
                }
            });
            if (!this.game.isGenuine()) {
                btnContinue.setClickable(false);
                btnContinue.setTooltip(LanguageManager.get("menu_locked_not_genuine"));
            }
            elements.add(btnContinue);
            this.continueRow = btnContinue;
            y += this.btnH + this.gap;
        }
        boolean newIsPrimary = !this.hasSaves;
        ButtonElement btnNew = this.row(btnX, y, this.btnW, this.btnH, LanguageManager.get("menu_new_game").toUpperCase(), Images.ic_menu_play, !this.game.isGenuine() ? Color.GRAY : (newIsPrimary ? CFG.COLOR_RED_BTN : CFG.COLOR_PARCHMENT_BTN_BG), !this.game.isGenuine() ? Color.LIGHT_GRAY : (newIsPrimary ? Color.WHITE : CFG.COLOR_TEXT_DARK), new Runnable(){

            @Override
            public void run() {
                if (!MainMenuScreen.this.game.isGenuine()) {
                    return;
                }
                MainMenuScreen.this.game.showScenarioSelect();
            }
        });
        if (!this.game.isGenuine()) {
            btnNew.setClickable(false);
            btnNew.setTooltip(LanguageManager.get("menu_locked_not_genuine"));
        }
        elements.add(btnNew);
        this.playRow = btnNew;
        this.separatorDrawY = (y += this.btnH + this.gap) + separatorH / 2;
        boolean tutorialPassed = SettingsManager.getInstance().isTutorialPassed();
        Color tutBg = tutorialPassed ? CFG.COLOR_PARCHMENT_BTN_BG : TUTORIAL_GOLD;
        Color tutInk = tutorialPassed ? CFG.COLOR_TEXT_DARK : CFG.COLOR_TEXT_ON_LIGHT_ACCENT;
        ButtonElement btnTutorial = this.row(btnX, y += separatorH, this.btnW, this.btnH, LanguageManager.get("menu_tutorial"), Images.ic_menu_tutorial, this.game.isGenuine() ? tutBg : Color.GRAY, this.game.isGenuine() ? tutInk : Color.LIGHT_GRAY, new Runnable(){

            @Override
            public void run() {
                if (!MainMenuScreen.this.game.isGenuine()) {
                    return;
                }
                MainMenuScreen.this.game.startTutorialScenario();
            }
        });
        if (!this.game.isGenuine()) {
            btnTutorial.setClickable(false);
            btnTutorial.setTooltip(LanguageManager.get("menu_locked_not_genuine"));
        }
        elements.add(this.charmRow(btnTutorial, "tutorial"));

        for (cgs.modloader.api.ModMenuHelper.ModMenuItem item : modItems) {
            ButtonElement b = this.row(btnX, y += this.btnH + this.gap, this.btnW, this.btnH, item.text, item.iconIndex, item.bgColor, item.textColor, item.onClick);
            if (item.tooltip != null) b.setTooltip(item.tooltip);
            elements.add(this.charmRow(b, "mod_" + item.text));
        }

        elements.add(this.charmRow(this.row(btnX, y += this.btnH + this.gap, this.btnW, this.btnH, LanguageManager.get("menu_editor"), Images.ic_menu_brush, CFG.COLOR_PARCHMENT_BTN_BG, CFG.COLOR_TEXT_DARK, new Runnable(){

            @Override
            public void run() {
                MainMenuScreen.this.game.showEditorSelect();
            }
        }), "editor"));
        elements.add(this.charmRow(this.row(btnX, y += this.btnH + this.gap, this.btnW, this.btnH, LanguageManager.get("menu_mods").toUpperCase(), Images.ic_nav_map, CFG.COLOR_PARCHMENT_BTN_BG, CFG.COLOR_TEXT_DARK, new Runnable(){

            @Override
            public void run() {
                MainMenuScreen.this.game.showEnhancedModManager();
            }
        }), "mods"));
        this.separatorDrawY2 = (y += this.btnH + this.gap) + separatorH / 2;
        elements.add(this.charmRow(this.row(btnX, y += separatorH, this.btnW, this.btnH, LanguageManager.get("menu_settings"), Images.ic_menu_settings, CFG.COLOR_PARCHMENT_BTN_BG, CFG.COLOR_TEXT_DARK, new Runnable(){

            @Override
            public void run() {
                MainMenuScreen.this.game.showSettings();
            }
        }), "settings"));
        elements.add(this.charmRow(this.row(btnX, y += this.btnH + this.gap, this.btnW, this.btnH, LanguageManager.get("menu_help"), Images.ic_menu_help, CFG.COLOR_PARCHMENT_BTN_BG, CFG.COLOR_TEXT_DARK, new Runnable(){

            @Override
            public void run() {
                MainMenuScreen.this.game.showHelpScreen();
            }
        }), "help"));
        this.separatorDrawY3 = (y += this.btnH + this.gap) + separatorH / 2;
        elements.add(this.charmRow(this.row(btnX, y += separatorH, this.btnW, this.btnH, LanguageManager.get("menu_exit"), Images.ic_menu_exit, CFG.COLOR_TERTIARY_BTN, Color.WHITE, new Runnable(){

            @Override
            public void run() {
                Gdx.app.exit();
            }
        }), "exit"));
        this.menuPanel = new SliderMenu();
        this.menuPanel.initMenu(null, 0, 0, W, H, elements, true, false);
        this.menuPanel.setVisible(true);
        this.focusedIndex = -1;
        this.keyboardFocusVisible = false;
        this.whatsNew.layout(this.panelX, this.panelY, this.panelW, this.panelH, W, H, true);
        this.themeToggle.place(this.whatsNew.companionX(), this.whatsNew.companionY(), this.whatsNew.companionSize(), this.whatsNew.companionInRow());
        MenuWorldMap.setBlockedRect(this.panelX - CFG.s(8), this.headerY, this.panelW + CFG.s(16), this.panelY + this.panelH + CFG.s(8) - this.headerY);
    }

    private ButtonElement charmRow(ButtonElement b, String id) {
        this.charmRows.add(b);
        this.charmRowIds.add(id);
        return b;
    }

    private ButtonElement row(int x, int y, int w, int h, String label, int icon, Color bg, Color ink, Runnable action) {
        ButtonElement b = new ButtonElement(x, y, w, h, label, bg, ink, action);
        b.setIcon(icon);
        b.setAlignLeft(true);
        b.setFitText(true);
        return b;
    }

    private boolean overMenuChrome(int x, int y) {
        if (x >= this.panelX - CFG.s(8) && x <= this.panelX + this.panelW + CFG.s(8) && y >= this.panelY - CFG.s(8) && y <= this.panelY + this.panelH + CFG.s(8)) {
            return true;
        }
        if (y >= this.headerY - CFG.s(8) && y <= this.headerY + this.headerHeight() && x >= this.panelX - CFG.s(40) && x <= this.panelX + this.panelW + CFG.s(40)) {
            return true;
        }
        if (this.whatsNew.contains(x, y)) {
            return true;
        }
        if (this.themeToggle.contains(x, y)) {
            return true;
        }
        for (IconButton ib : this.cornerButtons) {
            if (!ib.contains(x, y)) continue;
            return true;
        }
        return false;
    }

    private boolean isPaintingSpree() {
        return MenuWorldMap.hasActor() && TimeUtils.timeSinceMillis(this.lastMapStrokeMs) < 1500L;
    }

    private void beginMapPress(int x, int y) {
        boolean bl = this.pressedOnMap = !this.overMenuChrome(x, y);
        if (this.pressedOnMap) {
            MenuWorldMap.endStroke();
            MenuWorldMap.press(x, y);
        }
    }

    private void declareButtonAnchor(CharmCollection.Spot spot, ButtonElement row, String id, int clipY, int clipH) {
        if (row == null || !row.getVisible()) {
            return;
        }
        int btnTop = row.getPosY() - (int)this.scrollOffset;
        int btnBottom = btnTop + row.getHeight();
        if (btnTop >= clipY && btnBottom <= clipY + clipH) {
            this.charmBoard.anchor(spot, id, row.getPosX(), btnBottom, row.getWidth());
        }
    }

    private void updateCharms(float delta) {
        if (this.charmBoard == null) {
            return;
        }
        int W = CFG.GAME_WIDTH;
        this.charmBoard.beginFrame();
        this.charmBoard.anchor(CharmCollection.Spot.SCREEN_TOP, 0, 0, W);
        if (this.menuPanel != null && this.menuPanel.getVisible()) {
            this.charmBoard.anchor(CharmCollection.Spot.PANEL, this.panelX, this.panelY, this.panelW);
            int clipY = this.panelY + this.pad;
            int clipH = this.panelH - this.pad * 2;
            this.declareButtonAnchor(CharmCollection.Spot.PLAY, this.continueRow, "continue", clipY, clipH);
            this.declareButtonAnchor(CharmCollection.Spot.PLAY, this.playRow, null, clipY, clipH);
            for (int i = 0; i < this.charmRows.size(); ++i) {
                this.declareButtonAnchor(CharmCollection.Spot.BUTTON, this.charmRows.get(i), this.charmRowIds.get(i), clipY, clipH);
            }
        }
        float drive = delta > 0.0f ? (this.parallaxX - this.prevParallaxX) / delta : 0.0f;
        this.prevParallaxX = this.parallaxX;
        this.charmBoard.update(delta, drive);
        if (this.charmToastLife > 0.0f) {
            this.charmToastLife -= delta;
        }
        this.charmConfirm.update(delta);
    }

    private void awardSweepIfComplete() {
        boolean complete = MenuWorldMap.recordConquest();
        if (complete) {
            AchievementManager.getInstance().unlockOutsideRun("ach_time_breach");
            CharmCollection.Charm got = CharmCollection.getInstance().grantFromMinigame();
            this.charmToast = LanguageManager.format("charm_granted", MainMenuScreen.charmName(got));
            SoundManager.getInstance().play(SoundManager.SFX.UPGRADE_LEVELUP);
        } else {
            int total = MenuWorldMap.poolSize();
            if (total <= 0) {
                return;
            }
            this.charmToast = LanguageManager.format("menu_map_sweep", String.valueOf(MenuWorldMap.conqueredCount()), String.valueOf(total));
        }
        this.charmToastLife = 4.5f;
    }

    static String charmName(CharmCollection.Charm c) {
        if (c == null) {
            return "";
        }
        switch (c.kind) {
            case HEART: {
                return LanguageManager.get("charm_kind_heart");
            }
            case HAND: {
                return LanguageManager.get("charm_kind_hand");
            }
            case LANG_FLAG: {
                return LanguageManager.get("charm_kind_lang_flag");
            }
            case COUNTRY_FLAG: {
                return LanguageManager.get("charm_kind_country_flag");
            }
            case SILHOUETTE: {
                return LanguageManager.get("charm_kind_silhouette");
            }
        }
        return "";
    }

    private void drawCharmToast(float alpha) {
        if (this.charmToast == null || this.charmToastLife <= 0.0f || CFG.fontSmall == null) {
            return;
        }
        float fade = Math.min(1.0f, this.charmToastLife / 0.8f) * alpha;
        int x = CFG.s(16);
        int y = CFG.s(14);
        CFG.glyphLayout.setText(CFG.fontSmall, this.charmToast);
        int w = (int)CFG.glyphLayout.width;
        this.TMP_INK.set(0.0f, 0.0f, 0.02f, 0.55f * fade);
        this.batch.setColor(this.TMP_INK);
        ImageManager.getImage(Images.pix255_255_255).draw(this.batch, x - CFG.s(8), y - CFG.s(5), w + CFG.s(16), this.lineHSmall() + CFG.s(10));
        this.batch.setColor(Color.WHITE);
        this.TMP_INK.set(1.0f, 0.86f, 0.45f, fade);
        CFG.drawTextSmallBold(this.batch, this.charmToast, x, y, this.TMP_INK);
    }

    public void openWhatsNewForProbe() {
        this.whatsNew.touchDown(this.stripProbePoint()[0], this.stripProbePoint()[1]);
        this.whatsNew.touchUp(this.stripProbePoint()[0], this.stripProbePoint()[1]);
    }

    private int[] stripProbePoint() {
        return new int[]{this.panelX + this.panelW / 2, this.panelY + this.panelH + CFG.s(28)};
    }

    public void toggleThemeForProbe() {
        this.themeToggle.mouseMoved(this.themeToggle.centerX(), this.themeToggle.centerY(), true);
        this.themeReveal.request(this.themeToggle.centerX(), this.themeToggle.centerY());
    }

    private boolean handleGratitudeDialogTouchDown(int screenX, int screenY) {
        if (this.gratitudeDialogTimer < 0.4f) {
            return true;
        }
        if (screenX >= this.gratitudeBtnConfirmX && screenX <= this.gratitudeBtnConfirmX + this.gratitudeBtnW && screenY >= this.gratitudeBtnConfirmY && screenY <= this.gratitudeBtnConfirmY + this.gratitudeBtnH) {
            this.gratitudeDialogPressedIndex = 0;
        }
        return true;
    }

    private boolean handleGratitudeDialogTouchUp(int screenX, int screenY) {
        if (this.gratitudeDialogTimer < 0.4f) {
            return true;
        }
        if (this.gratitudeDialogPressedIndex == 0) {
            boolean inside;
            boolean bl = inside = screenX >= this.gratitudeBtnConfirmX && screenX <= this.gratitudeBtnConfirmX + this.gratitudeBtnW && screenY >= this.gratitudeBtnConfirmY && screenY <= this.gratitudeBtnConfirmY + this.gratitudeBtnH;
            if (inside) {
                SoundManager.getInstance().play(SoundManager.SFX.BUTTON_CLICK);
                this.gratitudeDialogVisible = false;
                SettingsManager.getInstance().set1939(true);
            }
        }
        this.gratitudeDialogPressedIndex = -1;
        return true;
    }

    private void handleGratitudeDialogMouseMoved(int screenX, int screenY) {
        this.gratitudeDialogHoverIndex = -1;
        if (screenX >= this.gratitudeBtnConfirmX && screenX <= this.gratitudeBtnConfirmX + this.gratitudeBtnW && screenY >= this.gratitudeBtnConfirmY && screenY <= this.gratitudeBtnConfirmY + this.gratitudeBtnH) {
            this.gratitudeDialogHoverIndex = 0;
        }
    }

    private boolean handleSocialDialogTouchDown(int screenX, int screenY) {
        if (this.socialDialogTimer < 0.4f) {
            return true;
        }
        if (screenX >= this.socialBtnConfirmX && screenX <= this.socialBtnConfirmX + this.socialBtnW && screenY >= this.socialBtnConfirmY && screenY <= this.socialBtnConfirmY + this.socialBtnH) {
            this.socialDialogPressedIndex = 0;
        } else if (screenX >= this.socialBtnCancelX && screenX <= this.socialBtnCancelX + this.socialBtnW && screenY >= this.socialBtnCancelY && screenY <= this.socialBtnCancelY + this.socialBtnH) {
            this.socialDialogPressedIndex = 1;
        } else if (!this.isInsideSocialDialog(screenX, screenY)) {
            this.socialDialogPressedIndex = 2;
        }
        return true;
    }

    private boolean isInsideSocialDialog(int screenX, int screenY) {
        return screenX >= this.socialDialogX && screenX <= this.socialDialogX + this.socialDialogW && screenY >= this.socialDialogY && screenY <= this.socialDialogY + this.socialDialogH;
    }

    private boolean handleSocialDialogTouchUp(int screenX, int screenY) {
        if (this.socialDialogTimer < 0.4f) {
            return true;
        }
        if (this.socialDialogPressedIndex != -1) {
            int releaseIndex = -1;
            if (screenX >= this.socialBtnConfirmX && screenX <= this.socialBtnConfirmX + this.socialBtnW && screenY >= this.socialBtnConfirmY && screenY <= this.socialBtnConfirmY + this.socialBtnH) {
                releaseIndex = 0;
            } else if (screenX >= this.socialBtnCancelX && screenX <= this.socialBtnCancelX + this.socialBtnW && screenY >= this.socialBtnCancelY && screenY <= this.socialBtnCancelY + this.socialBtnH) {
                releaseIndex = 1;
            } else if (!this.isInsideSocialDialog(screenX, screenY)) {
                releaseIndex = 2;
            }
            if (releaseIndex == this.socialDialogPressedIndex) {
                SoundManager.getInstance().play(SoundManager.SFX.BUTTON_CLICK);
                if (releaseIndex == 0 && this.activeSocialButton != null) {
                    this.openSocialUrl(this.activeSocialButton.url);
                }
                this.socialDialogVisible = false;
                this.activeSocialButton = null;
            }
        }
        this.socialDialogPressedIndex = -1;
        return true;
    }

    private void handleSocialDialogMouseMoved(int screenX, int screenY) {
        this.socialDialogHoverIndex = -1;
        if (screenX >= this.socialBtnConfirmX && screenX <= this.socialBtnConfirmX + this.socialBtnW && screenY >= this.socialBtnConfirmY && screenY <= this.socialBtnConfirmY + this.socialBtnH) {
            this.socialDialogHoverIndex = 0;
        } else if (screenX >= this.socialBtnCancelX && screenX <= this.socialBtnCancelX + this.socialBtnW && screenY >= this.socialBtnCancelY && screenY <= this.socialBtnCancelY + this.socialBtnH) {
            this.socialDialogHoverIndex = 1;
        }
    }

    private void activateCornerButton(IconButton ib) {
        if (ib.url != null) {
            this.activeSocialButton = ib;
            this.socialDialogVisible = true;
            this.socialDialogTimer = 0.0f;
            this.socialDialogPressedIndex = -1;
            this.socialDialogHoverIndex = -1;
            SoundManager.getInstance().play(SoundManager.SFX.SCREEN_TRANSITION);
            this.dialogParticles.clear();
            return;
        }
        SoundManager.getInstance().play(SoundManager.SFX.BUTTON_CLICK);
        HapticManager.getInstance().vibrate(HapticManager.HapticType.BUTTON_CLICK);
        if (ib.action != null) {
            ib.action.run();
        }
    }

    private boolean handleKeyDown(int keycode) {
        boolean isConfirm;
        boolean isBack = keycode == 111 || keycode == 4;
        boolean bl = isConfirm = keycode == 66 || keycode == 160 || keycode == 62;
        if (this.gratitudeDialogVisible) {
            if ((isConfirm || isBack) && this.gratitudeDialogTimer >= 0.4f) {
                SoundManager.getInstance().play(SoundManager.SFX.BUTTON_CLICK);
                this.gratitudeDialogVisible = false;
                SettingsManager.getInstance().set1939(true);
            }
            return true;
        }
        if (this.socialDialogVisible) {
            if (this.socialDialogTimer < 0.4f) {
                return true;
            }
            if (isBack) {
                SoundManager.getInstance().play(SoundManager.SFX.BUTTON_CLICK);
                this.socialDialogVisible = false;
                this.activeSocialButton = null;
            } else if (isConfirm && this.activeSocialButton != null) {
                SoundManager.getInstance().play(SoundManager.SFX.BUTTON_CLICK);
                this.openSocialUrl(this.activeSocialButton.url);
                this.socialDialogVisible = false;
                this.activeSocialButton = null;
            }
            return true;
        }
        if (isBack && this.whatsNew.onBackKey()) {
            SoundManager.getInstance().play(SoundManager.SFX.BUTTON_CLICK);
            return true;
        }
        if (isBack) {
            return false;
        }
        if (keycode == 19) {
            this.moveFocus(-1);
            return true;
        }
        if (keycode == 20) {
            this.moveFocus(1);
            return true;
        }
        if (isConfirm) {
            MenuElement focused = this.getFocusedElement();
            if (focused != null && (this.keyboardFocusVisible || focused.getIsHovered())) {
                focused.actionElement();
            } else if (focused != null) {
                this.keyboardFocusVisible = true;
                focused.setIsHovered(true);
            } else {
                this.moveFocus(1);
            }
            return true;
        }
        return false;
    }

    private boolean hitLockedRow(int x, int y) {
        if (this.menuPanel == null || this.menuPanel.getMenuElements() == null) {
            return false;
        }
        for (MenuElement el : this.menuPanel.getMenuElements()) {
            if (el == null || !el.getVisible() || el.getClickable() || x < el.getPosX() || x >= el.getPosX() + el.getWidth() || y < el.getPosY() || y >= el.getPosY() + el.getHeight()) continue;
            return true;
        }
        return false;
    }

    private MenuElement getFocusedElement() {
        if (this.menuPanel == null || this.menuPanel.getMenuElements() == null) {
            return null;
        }
        List<MenuElement> els = this.menuPanel.getMenuElements();
        if (this.focusedIndex < 0 || this.focusedIndex >= els.size()) {
            return null;
        }
        MenuElement el = els.get(this.focusedIndex);
        return el != null && el.getVisible() && el.getClickable() ? el : null;
    }

    private void moveFocus(int step) {
        if (this.menuPanel == null || this.menuPanel.getMenuElements() == null) {
            return;
        }
        List<MenuElement> els = this.menuPanel.getMenuElements();
        int n = els.size();
        if (n == 0) {
            return;
        }
        int idx = this.focusedIndex;
        for (int tries = 0; tries < n; ++tries) {
            MenuElement el = els.get(idx = (idx + step + n) % n);
            if (el == null || !el.getVisible() || !el.getClickable()) continue;
            this.focusedIndex = idx;
            this.keyboardFocusVisible = true;
            for (int i = 0; i < n; ++i) {
                MenuElement other = els.get(i);
                if (other == null) continue;
                other.setIsHovered(i == idx);
            }
            this.scrollFocusIntoView(el);
            SoundManager.getInstance().play(SoundManager.SFX.SELECT);
            return;
        }
    }

    private void scrollFocusIntoView(MenuElement el) {
        if (this.maxScrollOffset <= 0.0f) {
            return;
        }
        int clipY = this.panelY + this.pad;
        int clipH = this.panelH - this.pad * 2;
        float top = (float)el.getPosY() - this.scrollOffset;
        float bottom = top + (float)el.getHeight();
        if (top < (float)clipY) {
            this.scrollOffset = el.getPosY() - clipY;
        } else if (bottom > (float)(clipY + clipH)) {
            this.scrollOffset = el.getPosY() + el.getHeight() - clipY - clipH;
        }
        this.scrollOffset = Math.max(0.0f, Math.min(this.maxScrollOffset, this.scrollOffset));
        this.scrollVelocity = 0.0f;
    }

    private void openSocialUrl(String url) {
        if (url == null) {
            return;
        }
        try {
            Gdx.net.openURI(url);
        }
        catch (Exception e) {
            Gdx.app.error("MainMenuScreen", "Failed to open URL: " + url, e);
        }
    }

    private void setupInput() {
        Gdx.input.setInputProcessor(new InputAdapter(){
            private float touchDownX;
            private float touchDownY;
            private boolean isClickCandidate = false;

            @Override
            public boolean touchDown(int screenX, int screenY, int pointer, int button) {
                if (cgs.modloader.update.ModUpdateChecker.getInstance().handleClick(screenX, CFG.GAME_HEIGHT - screenY, CFG.GAME_WIDTH, CFG.GAME_HEIGHT)) {
                    return true;
                }
                boolean painting;
                if (MainMenuScreen.this.gratitudeDialogVisible) {
                    return MainMenuScreen.this.handleGratitudeDialogTouchDown(screenX, screenY);
                }
                if (MainMenuScreen.this.socialDialogVisible) {
                    return MainMenuScreen.this.handleSocialDialogTouchDown(screenX, screenY);
                }
                boolean bl = painting = MainMenuScreen.this.isPaintingSpree() && !MainMenuScreen.this.overMenuChrome(screenX, screenY);
                if (!painting && MainMenuScreen.this.charmBoard != null && MainMenuScreen.this.charmBoard.touchDown(screenX, screenY)) {
                    return true;
                }
                if (MainMenuScreen.this.whatsNew.touchDown(screenX, screenY)) {
                    return true;
                }
                if (MainMenuScreen.this.themeToggle.touchDown(screenX, screenY)) {
                    return true;
                }
                MainMenuScreen.this.pressedCornerButton = null;
                for (IconButton ib : MainMenuScreen.this.cornerButtons) {
                    if (!ib.contains(screenX, screenY)) continue;
                    ib.pressed = true;
                    MainMenuScreen.this.pressedCornerButton = ib;
                    return true;
                }
                MainMenuScreen.this.beginMapPress(screenX, screenY);
                MainMenuScreen.this.keyboardFocusVisible = false;
                this.touchDownX = screenX;
                this.touchDownY = screenY;
                this.isClickCandidate = true;
                MainMenuScreen.this.isDraggingScroll = false;
                boolean handled = false;
                if (MainMenuScreen.this.menuPanel != null && MainMenuScreen.this.menuPanel.getVisible() && !(handled = MainMenuScreen.this.menuPanel.actionPressDown(screenX, screenY + (int)MainMenuScreen.this.scrollOffset)) && MainMenuScreen.this.hitLockedRow(screenX, screenY + (int)MainMenuScreen.this.scrollOffset)) {
                    SoundManager.getInstance().play(SoundManager.SFX.DENIED);
                }
                MainMenuScreen.this.dragStartY = screenY;
                MainMenuScreen.this.dragStartScrollOffset = MainMenuScreen.this.scrollOffset;
                MainMenuScreen.this.scrollVelocity = 0.0f;
                return handled || MainMenuScreen.this.maxScrollOffset > 0.0f && screenX >= MainMenuScreen.this.panelX && screenX <= MainMenuScreen.this.panelX + MainMenuScreen.this.panelW && screenY >= MainMenuScreen.this.panelY && screenY <= MainMenuScreen.this.panelY + MainMenuScreen.this.panelH;
            }

            @Override
            public boolean touchUp(int screenX, int screenY, int pointer, int button) {
                if (MainMenuScreen.this.gratitudeDialogVisible) {
                    return MainMenuScreen.this.handleGratitudeDialogTouchUp(screenX, screenY);
                }
                if (MainMenuScreen.this.socialDialogVisible) {
                    return MainMenuScreen.this.handleSocialDialogTouchUp(screenX, screenY);
                }
                if (MainMenuScreen.this.charmBoard != null && MainMenuScreen.this.charmBoard.touchUp(screenX, screenY)) {
                    return true;
                }
                if (MainMenuScreen.this.whatsNew.touchUp(screenX, screenY)) {
                    return true;
                }
                if (MainMenuScreen.this.themeToggle.touchUp(screenX, screenY)) {
                    return true;
                }
                if (MainMenuScreen.this.pressedCornerButton != null) {
                    IconButton armed = MainMenuScreen.this.pressedCornerButton;
                    armed.pressed = false;
                    MainMenuScreen.this.pressedCornerButton = null;
                    if (armed.contains(screenX, screenY)) {
                        MainMenuScreen.this.activateCornerButton(armed);
                    }
                    return true;
                }
                if (MainMenuScreen.this.pressedOnMap) {
                    MainMenuScreen.this.pressedOnMap = false;
                    MainMenuScreen.this.lastMapStrokeMs = TimeUtils.millis();
                    MenuWorldMap.endStroke();
                    if (MenuWorldMap.click(screenX, screenY)) {
                        SoundManager.getInstance().play(SoundManager.SFX.CLICK);
                        if (MenuWorldMap.isConquered()) {
                            SoundManager.getInstance().play(SoundManager.SFX.SCREEN_TRANSITION);
                            MenuWorldMap.nextMap(screenX, screenY);
                        }
                        return true;
                    }
                }
                MainMenuScreen.this.isDraggingScroll = false;
                if (this.isClickCandidate) {
                    this.isClickCandidate = false;
                    if (MainMenuScreen.this.menuPanel != null && MainMenuScreen.this.menuPanel.getVisible()) {
                        return MainMenuScreen.this.menuPanel.actionPressUp(screenX, screenY + (int)MainMenuScreen.this.scrollOffset);
                    }
                } else if (MainMenuScreen.this.menuPanel != null) {
                    MainMenuScreen.this.menuPanel.actionPressUp(-999, -999);
                }
                return false;
            }

            @Override
            public boolean touchDragged(int screenX, int screenY, int pointer) {
                if (MainMenuScreen.this.charmBoard != null && MainMenuScreen.this.charmBoard.touchDragged(screenX, screenY)) {
                    return true;
                }
                if (MainMenuScreen.this.charmBoard != null && MainMenuScreen.this.charmBoard.takeSwipeThrough()) {
                    int sx = MainMenuScreen.this.charmBoard.swipeStartX();
                    int sy = MainMenuScreen.this.charmBoard.swipeStartY();
                    MainMenuScreen.this.beginMapPress(sx, sy);
                    if (MainMenuScreen.this.pressedOnMap) {
                        MenuWorldMap.drag(sx, sy);
                    }
                    this.touchDownX = sx;
                    this.touchDownY = sy;
                    this.isClickCandidate = true;
                    MainMenuScreen.this.isDraggingScroll = false;
                    MainMenuScreen.this.dragStartY = sy;
                    MainMenuScreen.this.dragStartScrollOffset = MainMenuScreen.this.scrollOffset;
                    MainMenuScreen.this.scrollVelocity = 0.0f;
                }
                this.mouseMoved(screenX, screenY);
                if (MainMenuScreen.this.whatsNew.touchDragged(screenX, screenY)) {
                    return true;
                }
                if (MainMenuScreen.this.themeToggle.touchDragged(screenX, screenY)) {
                    return true;
                }
                if (MainMenuScreen.this.pressedOnMap) {
                    MenuWorldMap.hover(screenX, screenY);
                    MenuWorldMap.drag(screenX, screenY);
                    return true;
                }
                if (MainMenuScreen.this.pressedCornerButton != null) {
                    ((MainMenuScreen)MainMenuScreen.this).pressedCornerButton.pressed = MainMenuScreen.this.pressedCornerButton.contains(screenX, screenY);
                    return true;
                }
                if (MainMenuScreen.this.maxScrollOffset > 0.0f) {
                    float dy = this.touchDownY - (float)screenY;
                    if (this.isClickCandidate && Math.abs(dy) > (float)CFG.s(8)) {
                        this.isClickCandidate = false;
                        if (MainMenuScreen.this.menuPanel != null) {
                            MainMenuScreen.this.menuPanel.actionPressUp(-999, -999);
                        }
                        MainMenuScreen.this.isDraggingScroll = true;
                    }
                    if (MainMenuScreen.this.isDraggingScroll) {
                        float prevScroll = MainMenuScreen.this.scrollOffset;
                        MainMenuScreen.this.scrollOffset = MainMenuScreen.this.dragStartScrollOffset + dy;
                        MainMenuScreen.this.scrollOffset = Math.max(0.0f, Math.min(MainMenuScreen.this.maxScrollOffset, MainMenuScreen.this.scrollOffset));
                        MainMenuScreen.this.scrollVelocity = (MainMenuScreen.this.scrollOffset - prevScroll) / Gdx.graphics.getDeltaTime();
                        return true;
                    }
                }
                return false;
            }

            @Override
            public boolean mouseMoved(int screenX, int screenY) {
                if (MainMenuScreen.this.gratitudeDialogVisible) {
                    MainMenuScreen.this.handleGratitudeDialogMouseMoved(screenX, screenY);
                    return true;
                }
                if (MainMenuScreen.this.socialDialogVisible) {
                    MainMenuScreen.this.handleSocialDialogMouseMoved(screenX, screenY);
                    return true;
                }
                MainMenuScreen.this.themeToggle.mouseMoved(screenX, screenY, !MainMenuScreen.this.whatsNew.isExpanded());
                if (MainMenuScreen.this.charmBoard != null && MainMenuScreen.this.charmBoard.mouseMoved(screenX, screenY)) {
                    MenuWorldMap.clearHover();
                    return true;
                }
                if (MainMenuScreen.this.whatsNew.mouseMoved(screenX, screenY)) {
                    MenuWorldMap.clearHover();
                    return true;
                }
                if (MainMenuScreen.this.overMenuChrome(screenX, screenY)) {
                    MenuWorldMap.clearHover();
                } else {
                    MenuWorldMap.hover(screenX, screenY);
                }
                MainMenuScreen.this.targetParallaxX = -((float)screenX / (float)Gdx.graphics.getWidth() - 0.5f) * 80.0f;
                MainMenuScreen.this.targetParallaxY = ((float)screenY / (float)Gdx.graphics.getHeight() - 0.5f) * 40.0f;
                for (IconButton ib : MainMenuScreen.this.cornerButtons) {
                    ib.hovered = ib.contains(screenX, screenY);
                }
                if (MainMenuScreen.this.menuPanel != null && MainMenuScreen.this.menuPanel.getMenuElements() != null) {
                    List<MenuElement> els = MainMenuScreen.this.menuPanel.getMenuElements();
                    for (int i = 0; i < els.size(); ++i) {
                        MenuElement element = els.get(i);
                        if (element == null || !element.getVisible()) continue;
                        boolean isInside = screenX >= element.getPosX() && screenX < element.getPosX() + element.getWidth() && screenY + (int)MainMenuScreen.this.scrollOffset >= element.getPosY() && screenY + (int)MainMenuScreen.this.scrollOffset < element.getPosY() + element.getHeight();
                        element.setIsHovered(isInside);
                        if (!isInside || MainMenuScreen.this.keyboardFocusVisible) continue;
                        MainMenuScreen.this.focusedIndex = i;
                    }
                }
                return false;
            }

            @Override
            public boolean keyDown(int keycode) {
                return MainMenuScreen.this.handleKeyDown(keycode);
            }

            @Override
            public boolean keyTyped(char character) {
                if (MainMenuScreen.this.gratitudeDialogVisible || MainMenuScreen.this.socialDialogVisible) {
                    return false;
                }
                if (!MainMenuScreen.this.charmCheat.feed(character)) {
                    return false;
                }
                int added = CharmCheat.unlockAll();
                MainMenuScreen.this.charmToast = added > 0 ? LanguageManager.get("charm_cheat_unlocked") : LanguageManager.get("charm_cheat_already");
                MainMenuScreen.this.charmToastLife = 4.5f;
                SoundManager.getInstance().play(SoundManager.SFX.UPGRADE_LEVELUP);
                return true;
            }

            @Override
            public boolean scrolled(float amountX, float amountY) {
                if (MainMenuScreen.this.whatsNew.scrolled(Touch.getMousePosX(), Touch.getMousePosY(), amountY)) {
                    return true;
                }
                if (MainMenuScreen.this.maxScrollOffset > 0.0f) {
                    MainMenuScreen.this.scrollOffset += amountY * 30.0f;
                    MainMenuScreen.this.scrollOffset = Math.max(0.0f, Math.min(MainMenuScreen.this.maxScrollOffset, MainMenuScreen.this.scrollOffset));
                    MainMenuScreen.this.scrollVelocity = 0.0f;
                    return true;
                }
                return false;
            }
        });
    }

    public void skipIntroFade() {
        this.fadeInTimer = 0.8f;
        this.introPending = false;
        this.introStartNanos = 0L;
        this.rowReveal.finish();
    }

    @Override
    public void show() {
        this.setupInput();
        cgs.modloader.update.ModUpdateChecker.getInstance().checkForUpdatesAsync();
        if (this.charmBoard == null) {
            this.charmBoard = new CharmBoard(new CharmBoard.DeletePrompt(){

                @Override
                public void ask(String title, String message, String deleteLabel, String keepLabel, final Runnable onDelete, final Runnable onKeep) {
                    MainMenuScreen.this.charmConfirm.show(title, message, deleteLabel, keepLabel, new CGSConfirmDialog.OnConfirmResult(){

                        @Override
                        public void onOk() {
                            onDelete.run();
                        }

                        @Override
                        public void onCancel() {
                            onKeep.run();
                        }
                    });
                }
            });
            this.charmBoard.setDriveGain(3.0f);
            this.charmBoard.setOccluder(new CharmBoard.Occluder(){

                @Override
                public boolean covers(int layer, int x, int y) {
                    return MainMenuScreen.this.whatsNew.isExpanded() && MainMenuScreen.this.whatsNew.contains(x, y);
                }
            });
            this.charmBoard.setHoldToGrab(Gdx.app.getType() == Application.ApplicationType.Android || Gdx.app.getType() == Application.ApplicationType.iOS);
        }
        MusicManager.getInstance().playMenuMusic();
        this.game.setSkipAutoVignette(true);
        this.game.setIdleVignetteScale(1.0f);
        this.game.setEpochVignetteActive(false);
        this.buildCornerButtons();
        this.pressedCornerButton = null;
        this.socialDialogVisible = false;
        this.activeSocialButton = null;
        this.refreshSaveState();
        this.whatsNew.collapse();
        this.themeToggle.reset();
        this.themeReveal.cancel();
        this.buildMenu();
        this.introPending = true;
        this.introStartNanos = 0L;
        this.rowMarkerShown.unseed();
        if (!SettingsManager.getInstance().get1939()) {
            this.gratitudeDialogVisible = true;
            this.gratitudeDialogTimer = 0.0f;
            this.gratitudeDialogPressedIndex = -1;
            this.gratitudeDialogHoverIndex = -1;
            this.dialogParticles.clear();
        }
    }

    @Override
    public void pause() {
        this.themeReveal.cancel();
    }

    @Override
    public void hide() {
        this.themeReveal.cancel();
        this.game.setSkipAutoVignette(false);
        this.game.setIdleVignetteScale(1.0f);
        this.game.setEpochVignetteActive(false);
    }

    private void drawBgCover(Image bgImage, float alpha) {
        if (bgImage == null) {
            return;
        }
        int W = CFG.GAME_WIDTH;
        int H = CFG.GAME_HEIGHT;
        this.batch.setColor(1.0f, 1.0f, 1.0f, alpha);
        float overscanX = 110.0f;
        float overscanY = 60.0f;
        bgImage.draw(this.batch, (int)(-overscanX + this.parallaxX), (int)(-overscanY + this.parallaxY), (int)((float)W + overscanX * 2.0f), (int)((float)H + overscanY * 2.0f));
    }

    private void drawGlobalDotGrid(float pX, float pY, float alpha) {
        DotGrid.draw(this.batch, CFG.GAME_WIDTH, CFG.GAME_HEIGHT, pX, pY, alpha);
    }

    private void layoutCornerSlots(int W, int H) {
        int cols = 0;
        boolean narrow = this.isNarrow();
        int n = this.socialButtons.size();
        int margin = CFG.s(14);
        int big = narrow ? CFG.s(50) : CFG.s(66);
        int link = narrow ? CFG.s(40) : CFG.s(46);
        int linkGap = CFG.s(10);
        int pairY = H - big - margin;
        int achX = W - big - margin;
        int statX = achX - big - margin;
        int sideLeft = this.panelX + this.panelW + CFG.s(6) + CFG.s(16);
        int colBottom = pairY - CFG.s(14);
        int perColMax = Math.max(0, (colBottom - CFG.s(16) + linkGap) / (link + linkGap));
        int pitch = big + margin;
        int n2 = n == 0 ? 0 : (cols = perColMax == 0 ? Integer.MAX_VALUE : (n + perColMax - 1) / perColMax);
        if (n == 0) {
            this.clusterInCorner = statX >= sideLeft;
        } else {
            boolean bl = this.clusterInCorner = cols <= 3 && Math.min(statX, achX + big / 2 - (cols - 1) * pitch - link / 2) >= sideLeft;
        }
        if (this.clusterInCorner) {
            this.setSlot(this.achButton, achX, pairY, big, false);
            this.setSlot(this.statButton, statX, pairY, big, false);
            this.pairLeftX = statX;
            int perCol = n > 0 ? (n + cols - 1) / cols : 0;
            this.linksLeftX = W;
            this.linksTopY = colBottom;
            for (int i = 0; i < n; ++i) {
                int col = i / perCol;
                int row = i % perCol;
                int x = achX + big / 2 - col * pitch - link / 2;
                int y = colBottom - link - row * (link + linkGap);
                this.setSlot(this.socialButtons.get(i), x, y, link, false);
                this.linksLeftX = Math.min(this.linksLeftX, x);
                this.linksTopY = Math.min(this.linksTopY, y);
            }
            if (n == 0) {
                this.linksLeftX = this.pairLeftX;
            }
            this.linksBottomY = n > 0 ? colBottom : pairY;
        } else {
            int room = W - margin * 2;
            int groupGap = CFG.s(16);
            int pairGap = margin;
            boolean oneBand = false;
            if (n > 0) {
                int want = n * link + (n - 1) * linkGap + groupGap + big * 2 + linkGap;
                float k = Math.min(1.0f, (float)room / (float)want);
                if ((float)link * k >= (float)CFG.s(30)) {
                    oneBand = true;
                    link = (int)((float)link * k);
                    big = (int)((float)big * k);
                    linkGap = (int)((float)linkGap * k);
                    groupGap = (int)((float)groupGap * k);
                    pairGap = linkGap;
                } else {
                    link = Math.min(link, (room - (n - 1) * linkGap) / n);
                }
            }
            pairY = H - big - margin;
            achX = W - big - margin;
            statX = achX - big - pairGap;
            this.setSlot(this.achButton, achX, pairY, big, oneBand);
            this.setSlot(this.statButton, statX, pairY, big, oneBand);
            this.pairLeftX = statX;
            int rowRight = oneBand ? statX - groupGap : W - margin;
            int rowY = oneBand ? pairY + (big - link) / 2 : pairY - CFG.s(14) - link;
            for (int i = 0; i < n; ++i) {
                this.setSlot(this.socialButtons.get(i), rowRight - (i + 1) * link - i * linkGap, rowY, link, true);
            }
            this.linksLeftX = n > 0 ? rowRight - n * link - (n - 1) * linkGap : this.pairLeftX;
            this.linksTopY = n > 0 ? rowY : pairY;
            this.linksBottomY = n > 0 ? rowY + link : pairY;
        }
        this.cornerTopY = Math.min(pairY, this.linksTopY);
        this.cornerLeftX = Math.min(this.pairLeftX, this.linksLeftX);
        this.applyCornerHover();
    }

    private void setSlot(IconButton ib, int x, int y, int size, boolean hintAbove) {
        ib.slotX = x;
        ib.slotY = y;
        ib.baseSize = size;
        ib.hintAbove = hintAbove;
    }

    private void applyCornerHover() {
        int hoverExtra = CFG.s(6);
        for (IconButton ib : this.cornerButtons) {
            ib.size = ib.baseSize + (int)((float)hoverExtra * ib.hoverTransition);
            ib.x = ib.slotX - (ib.size - ib.baseSize) / 2;
            ib.y = ib.slotY - (ib.size - ib.baseSize) / 2;
        }
    }

    private void layoutBanner(int W, int H, boolean stripUnder) {
        this.bannerOn = false;
        if (CFG.fontSmall == null || !LanguageManager.has("menu_early_access_note")) {
            return;
        }
        String note = LanguageManager.get("menu_early_access_note");
        if (!this.clusterInCorner) {
            this.bannerPadX = CFG.s(16);
            this.bannerW = W - CFG.s(24);
            this.measureBanner(note);
            this.bannerH = Math.round((float)this.bannerNoteH * 1.2f) + CFG.s(26);
            this.bannerX = (W - this.bannerW) / 2;
            this.bannerY = this.cornerTopY - CFG.s(10) - this.bannerH;
            this.bannerOn = this.bannerTextW > CFG.s(120);
            return;
        }
        this.bannerPadX = CFG.s(28);
        int left = stripUnder ? CFG.s(20) : this.whatsNew.stripRight() + CFG.s(16);
        int right = this.pairLeftX - CFG.s(16);
        for (int pass = 0; pass < 2; ++pass) {
            int half = Math.min(W / 2 - left, right - W / 2);
            this.bannerW = Math.min(Math.min(W - CFG.s(40), CFG.s(750)), half * 2);
            if (this.bannerW - this.bannerPadX * 2 < CFG.s(120)) {
                return;
            }
            this.measureBanner(note);
            this.bannerH = (int)((float)(this.bannerNoteH + CFG.s(32)) * 1.2f);
            this.bannerX = (W - this.bannerW) / 2;
            this.bannerY = H - this.bannerH - CFG.s(12);
            if (this.bannerY >= this.linksBottomY + CFG.s(6) || this.linksLeftX >= right) break;
            right = Math.min(right, this.linksLeftX - CFG.s(16));
        }
        this.bannerOn = true;
    }

    private void measureBanner(String note) {
        this.bannerTextW = this.bannerW - this.bannerPadX * 2;
        CFG.glyphLayout.setText(CFG.fontSmall, note, COLOR_SUBTITLE, this.bannerTextW, 1, true);
        this.bannerNoteH = (int)CFG.glyphLayout.height;
    }

    private void drawIconButton(IconButton ib, float alpha, float scale) {
        float glyphAlpha;
        if (alpha <= 0.01f) {
            return;
        }
        float glowPulse = 0.5f + 0.5f * (float)Math.sin((double)ib.pulse * 2.0);
        float a = alpha * (0.85f + 0.15f * ib.hoverTransition);
        int sink = ib.pressed ? CFG.s(2) : 0;
        int size = Math.max(1, Math.round((float)ib.size * scale));
        int x = ib.x + (ib.size - size) / 2;
        int y = ib.y + (ib.size - size) / 2 + sink;
        int glow = Math.max(CFG.s(5), size / 11);
        if (ib.hovered) {
            this.batch.setColor(ib.accent.r, ib.accent.g, ib.accent.b, (0.32f + 0.1f * glowPulse) * a);
        } else {
            this.batch.setColor(0.85f, 0.65f, 0.15f, (0.09f + 0.045f * glowPulse) * a);
        }
        ImageManager.getImage(Images.obj_dot).draw(this.batch, x - glow, y - glow, size + glow * 2, size + glow * 2);
        int ring = Math.max(CFG.s(2), size / 22);
        this.batch.setColor(CFG.COLOR_IRON_BORDER.r, CFG.COLOR_IRON_BORDER.g, CFG.COLOR_IRON_BORDER.b, 0.8f * a);
        ImageManager.getImage(Images.obj_dot).draw(this.batch, x - ring, y - ring, size + ring * 2, size + ring * 2);
        this.batch.setColor(0.12f, 0.1f, 0.06f, 0.85f * a);
        ImageManager.getImage(Images.obj_dot).draw(this.batch, x, y, size, size);
        int inset = CFG.s(ib.glyphInset);
        float f = glyphAlpha = ib.hovered ? 1.0f : 0.82f;
        if (ib.tintGlyph) {
            this.batch.setColor(0.95f, 0.78f, 0.25f, glyphAlpha * a);
        } else {
            this.batch.setColor(1.0f, 1.0f, 1.0f, glyphAlpha * a);
        }
        ImageManager.getImage(ib.icon).draw(this.batch, x + inset, y + inset, size - inset * 2, size - inset * 2);
        this.batch.setColor(Color.WHITE);
    }

    private void drawEdgeVignette(float alpha) {
        int W = CFG.GAME_WIDTH;
        int H = CFG.GAME_HEIGHT;
        Image px = ImageManager.getImage(Images.pix255_255_255);
        if (px == null) {
            return;
        }
        int steps = 28;
        int bandW = Math.max(1, (int)((float)W * 0.26f) / 28);
        int bandH = Math.max(1, (int)((float)H * 0.3f) / 28);
        for (int i = 0; i < 28; ++i) {
            float t = 1.0f - (float)i / 28.0f;
            float a = (float)Math.pow(t, 1.6) * 0.5f * alpha;
            this.batch.setColor(0.0f, 0.0f, 0.015f, a);
            px.draw(this.batch, i * bandW, 0, bandW, H);
            px.draw(this.batch, W - (i + 1) * bandW, 0, bandW, H);
            px.draw(this.batch, 0, i * bandH, W, bandH);
            px.draw(this.batch, 0, H - (i + 1) * bandH, W, bandH);
        }
        this.batch.setColor(Color.WHITE);
    }

    private void drawWordmark(float alpha) {
        Image logo = ImageManager.getImage(Images.logo_game);
        if (logo == null || this.logoH <= 0) {
            return;
        }
        float in = this.introPart(0.0f, 0.42f);
        if (in <= 0.01f) {
            return;
        }
        float scale = 1.04f - 0.04f * in;
        int w = Math.round((float)this.logoW * scale);
        int h = Math.round((float)this.logoH * scale);
        int centreX = this.panelX + this.panelW / 2;
        int top = this.headerY + (this.logoH - h) / 2 - Math.round((float)CFG.s(18) * (1.0f - in));
        UiFx.radialGlowBox(this.batch, (float)centreX - (float)w * 0.42f, (float)top + (float)h * 0.18f, (float)w * 0.84f, (float)h * 0.7f, LOGO_GLOW, 0.22f * alpha * in, CFG.sf(26.0f));
        this.batch.setColor(1.0f, 1.0f, 1.0f, alpha * in);
        logo.draw(this.batch, centreX - w / 2, top, w, h);
        this.batch.setColor(Color.WHITE);
    }

    private float introClock() {
        if (this.introStartNanos == 0L) {
            return 2.8f;
        }
        float t = (float)(TimeUtils.nanoTime() - this.introStartNanos) / 1.0E9f;
        if (t >= 0.7f) {
            this.introStartNanos = 0L;
            return 2.8f;
        }
        return t;
    }

    private float introRaw(float start, float length) {
        return Ease.clamp01((this.introClock() - start) / length);
    }

    private float introPart(float start, float length) {
        return Ease.outCubic(this.introRaw(start, length));
    }

    private float cornerPop(IconButton ib) {
        int order = ib == this.achButton ? 0 : (ib == this.statButton ? 1 : 2 + this.socialButtons.indexOf(ib));
        return this.introRaw(0.16f + (float)order * 0.035f, 0.3f);
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    private void drawPanelFrame(float a) {
        if (a <= 0.01f) {
            return;
        }
        int fx = this.panelX - CFG.s(6);
        int fy = this.panelY - CFG.s(6);
        int fw = this.panelW + CFG.s(12);
        int fh = this.panelH + CFG.s(12);
        UiFx.boxGlowSoft(this.batch, fx, fy + CFG.s(4), fw, fh, 0.0f, 0.0f, 0.02f, 0.5f * a, CFG.sf(16.0f));
        boolean pushed = BatchFade.push(this.batch, a);
        try {
            CFG.drawIronMenuPanel(this.batch, fx, fy, fw, fh);
        }
        finally {
            if (pushed) {
                BatchFade.pop(this.batch);
            }
        }
        float drawn = this.introPart(0.06f, 0.34f);
        if (drawn <= 0.01f) {
            return;
        }
        int border = CFG.chromeBorder(4);
        int inset = CFG.isMinimalChrome() ? border : CFG.s(14);
        int ruleW = Math.round((float)(fw - inset * 2) * drawn);
        int ruleH = Math.max(2, CFG.s(3));
        int ruleX = fx + fw / 2 - ruleW / 2;
        if (CFG.isDarkTheme()) {
            this.batch.setColor(CFG.COLOR_RED_BTN.r, CFG.COLOR_RED_BTN.g, CFG.COLOR_RED_BTN.b, 0.16f * a);
            ImageManager.getImage(Images.gradientVertical).draw(this.batch, ruleX, fy + border + ruleH, ruleW, CFG.s(44));
        }
        this.batch.setColor(CFG.COLOR_RED_BTN.r, CFG.COLOR_RED_BTN.g, CFG.COLOR_RED_BTN.b, a);
        ImageManager.getImage(Images.pix255_255_255).draw(this.batch, ruleX, fy + border, ruleW, ruleH);
        this.batch.setColor(Color.WHITE);
    }

    private void drawGroupRule(int y, float a) {
        if (a <= 0.01f) {
            return;
        }
        Image fade = ImageManager.getImage(Images.gradient_full);
        Image px = ImageManager.getImage(Images.pix255_255_255);
        int x0 = this.panelX + this.pad;
        int cx = x0 + this.btnW / 2;
        int line = Math.max(1, CFG.s(1));
        int d = Math.max(4, CFG.s(6));
        int gapHalf = d + CFG.s(6);
        int half = fade.getWidth() / 2;
        Color c = CFG.COLOR_SEPARATOR_LINE;
        this.batch.setColor(c.r, c.g, c.b, c.a * a);
        fade.drawRegion(this.batch, x0, (float)y - (float)line / 2.0f, cx - gapHalf - x0, line, 0, 0, half, 1, false, false);
        fade.drawRegion(this.batch, cx + gapHalf, (float)y - (float)line / 2.0f, x0 + this.btnW - cx - gapHalf, line, half, 0, half, 1, false, false);
        px.draw(this.batch, cx - d / 2, y - d / 2, d / 2, d / 2, d, d, 1.0f, 1.0f, 45.0f, 0, 0, 1, 1, false, true);
        this.batch.setColor(Color.WHITE);
    }

    private int rowIndexBelow(int y) {
        if (this.menuPanel == null || this.menuPanel.getMenuElements() == null) {
            return 0;
        }
        List<MenuElement> els = this.menuPanel.getMenuElements();
        for (int i = 0; i < els.size(); ++i) {
            MenuElement el = els.get(i);
            if (el == null || el.getPosY() < y) continue;
            return i;
        }
        return Math.max(0, els.size() - 1);
    }

    private void drawRowMarker(float a) {
        float shown;
        MenuElement target = null;
        boolean touch;
        boolean bl = touch = Gdx.app.getType() == Application.ApplicationType.Android || Gdx.app.getType() == Application.ApplicationType.iOS;
        MenuElement menuElement = this.keyboardFocusVisible ? this.getFocusedElement() : (target = touch ? null : this.hoveredRow());
        if (target != null) {
            if (this.rowMarkerShown.value() < 0.05f) {
                this.rowMarker.snapTo(target.getPosY(), target.getHeight());
            } else {
                this.rowMarker.setTarget(target.getPosY(), target.getHeight());
            }
        }
        if ((shown = this.rowMarkerShown.toward(target != null ? 1.0f : 0.0f, 0.04f) * a) <= 0.01f) {
            return;
        }
        int inset = CFG.s(8);
        int barW = Math.max(2, CFG.s(4));
        int barX = this.panelX + this.pad / 2 - barW / 2;
        int barY = Math.round(this.rowMarker.x()) - (int)this.scrollOffset + inset;
        int barH = Math.round(this.rowMarker.w()) - inset * 2;
        if (barH <= 0) {
            return;
        }
        UiFx.boxGlowSoft(this.batch, barX, barY, barW, barH, CFG.COLOR_RED_BTN.r, CFG.COLOR_RED_BTN.g, CFG.COLOR_RED_BTN.b, 0.45f * shown, CFG.sf(5.0f));
        this.batch.setColor(CFG.COLOR_RED_BTN.r, CFG.COLOR_RED_BTN.g, CFG.COLOR_RED_BTN.b, shown);
        ImageManager.getImage(Images.pix255_255_255).draw(this.batch, barX, barY, barW, barH);
        this.batch.setColor(Color.WHITE);
    }

    private MenuElement hoveredRow() {
        if (this.menuPanel == null || this.menuPanel.getMenuElements() == null) {
            return null;
        }
        for (MenuElement el : this.menuPanel.getMenuElements()) {
            if (el == null || !el.getVisible() || !el.getClickable() || !el.getIsHovered()) continue;
            return el;
        }
        return null;
    }

    private void drawEmbers(float alpha) {
        Image dot = ImageManager.getImage(Images.obj_dot);
        for (Particle p : this.particles) {
            float px = p.x + this.parallaxX * 1.2f;
            float py = p.y + this.parallaxY * 1.2f;
            float flicker = 0.75f + 0.25f * (float)Math.sin(p.time * 2.3f + p.alphaSpeed * 40.0f);
            this.batch.setColor(p.color.r, p.color.g, p.color.b, p.alpha * 0.45f * flicker * alpha);
            this.topBarRenderer.drawRadialGlow(this.batch, (int)px, (int)py, (int)(p.size * 2.6f));
            this.batch.setColor(p.color.r, p.color.g, p.color.b, p.alpha * alpha);
            int ps = Math.max(2, (int)p.size);
            dot.draw(this.batch, (int)(px - (float)ps / 2.0f), (int)(py - (float)ps / 2.0f), ps, ps);
        }
        this.batch.setColor(Color.WHITE);
    }

    private void drawDisabledHint(float alpha) {
        if (this.menuPanel == null || this.menuPanel.getMenuElements() == null || CFG.fontSmall == null) {
            return;
        }
        MenuElement target = null;
        for (MenuElement el : this.menuPanel.getMenuElements()) {
            if (el == null || !el.getVisible() || el.getClickable() || !el.getIsHovered() || el.getTooltip() == null) continue;
            target = el;
            break;
        }
        if (target == null) {
            return;
        }
        String hint = target.getTooltip();
        CFG.glyphLayout.setText(CFG.fontSmall, hint);
        int textW = (int)CFG.glyphLayout.width;
        int textH = (int)CFG.glyphLayout.height;
        int plateH = CFG.s(28);
        int plateW = textW + CFG.s(16);
        int plateX = this.panelX + this.panelW + CFG.s(14);
        if (plateX + plateW > CFG.GAME_WIDTH - CFG.s(8)) {
            plateX = this.panelX - plateW - CFG.s(14);
        }
        int rowCenterY = target.getPosY() + target.getHeight() / 2 - (int)this.scrollOffset;
        int plateY = rowCenterY - plateH / 2;
        this.batch.setColor(0.0f, 0.0f, 0.0f, 0.35f * alpha);
        ImageManager.getImage(Images.pix255_255_255).draw(this.batch, plateX + 3, plateY + 3, plateW, plateH);
        this.batch.setColor(CFG.COLOR_PARCHMENT_PANEL_BG.r, CFG.COLOR_PARCHMENT_PANEL_BG.g, CFG.COLOR_PARCHMENT_PANEL_BG.b, 0.95f * alpha);
        ImageManager.getImage(Images.pix255_255_255).draw(this.batch, plateX, plateY, plateW, plateH);
        this.batch.setColor(CFG.COLOR_IRON_BORDER.r, CFG.COLOR_IRON_BORDER.g, CFG.COLOR_IRON_BORDER.b, 0.85f * alpha);
        ImageManager.getImage(Images.pix255_255_255).draw(this.batch, plateX, plateY, plateW, 1);
        ImageManager.getImage(Images.pix255_255_255).draw(this.batch, plateX, plateY + plateH - 1, plateW, 1);
        ImageManager.getImage(Images.pix255_255_255).draw(this.batch, plateX, plateY, 1, plateH);
        ImageManager.getImage(Images.pix255_255_255).draw(this.batch, plateX + plateW - 1, plateY, 1, plateH);
        CFG.drawTextSmall(this.batch, hint, plateX + CFG.s(8), plateY + plateH / 2 - textH / 2, new Color(CFG.COLOR_TEXT_DARK.r, CFG.COLOR_TEXT_DARK.g, CFG.COLOR_TEXT_DARK.b, alpha));
        this.batch.setColor(Color.WHITE);
    }

    private void drawIconButtonHint(IconButton ib, float alpha) {
        if (ib.hoverTransition <= 0.01f || CFG.fontSmall == null || ib.label == null) {
            return;
        }
        CFG.glyphLayout.setText(CFG.fontSmall, ib.label);
        float textW = CFG.glyphLayout.width;
        float textH = CFG.glyphLayout.height;
        int plateH = CFG.s(28);
        int plateW = (int)(textW + (float)CFG.s(16));
        int plateX = ib.x - plateW - CFG.s(12);
        int plateY = ib.y + ib.size / 2 - plateH / 2;
        if (ib.hintAbove) {
            plateX = Math.max(CFG.s(8), Math.min(CFG.GAME_WIDTH - CFG.s(8) - plateW, ib.x + ib.size / 2 - plateW / 2));
            plateY = ib.y - plateH - CFG.s(10);
        }
        float hintAlpha = ib.hoverTransition * alpha;
        this.batch.setColor(0.0f, 0.0f, 0.0f, 0.35f * hintAlpha);
        ImageManager.getImage(Images.pix255_255_255).draw(this.batch, plateX + 3, plateY + 3, plateW, plateH);
        this.batch.setColor(CFG.COLOR_PARCHMENT_PANEL_BG.r, CFG.COLOR_PARCHMENT_PANEL_BG.g, CFG.COLOR_PARCHMENT_PANEL_BG.b, 0.9f * hintAlpha);
        ImageManager.getImage(Images.pix255_255_255).draw(this.batch, plateX, plateY, plateW, plateH);
        this.batch.setColor(CFG.COLOR_IRON_BORDER.r, CFG.COLOR_IRON_BORDER.g, CFG.COLOR_IRON_BORDER.b, 0.8f * hintAlpha);
        int bSize = 1;
        ImageManager.getImage(Images.pix255_255_255).draw(this.batch, plateX, plateY, plateW, bSize);
        ImageManager.getImage(Images.pix255_255_255).draw(this.batch, plateX, plateY + plateH - bSize, plateW, bSize);
        ImageManager.getImage(Images.pix255_255_255).draw(this.batch, plateX, plateY, bSize, plateH);
        ImageManager.getImage(Images.pix255_255_255).draw(this.batch, plateX + plateW - bSize, plateY, bSize, plateH);
        CFG.drawTextSmall(this.batch, ib.label, plateX + CFG.s(8), plateY + plateH / 2 - (int)textH / 2, new Color(CFG.COLOR_TEXT_DARK.r, CFG.COLOR_TEXT_DARK.g, CFG.COLOR_TEXT_DARK.b, hintAlpha));
    }

    private void drawAchievementBadge(float alpha) {
        if (CFG.fontSmall == null) {
            return;
        }
        int unlocked = AchievementManager.getInstance().getUnlockedCount();
        int total = AchievementManager.getInstance().getTotalCount();
        if (unlocked <= 0) {
            return;
        }
        String badge = total > 0 ? unlocked + "/" + total : String.valueOf(unlocked);
        CFG.glyphLayout.setText(CFG.fontSmall, badge);
        int badgeW = Math.max(CFG.s(20), (int)CFG.glyphLayout.width + CFG.s(8));
        int badgeH = CFG.s(18);
        int badgeX = this.achButton.x + this.achButton.size - badgeW + CFG.s(6);
        int badgeY = this.achButton.y - CFG.s(2);
        this.batch.setColor(CFG.COLOR_RED_BTN.r, CFG.COLOR_RED_BTN.g, CFG.COLOR_RED_BTN.b, alpha);
        ImageManager.getImage(Images.pix255_255_255).draw(this.batch, badgeX, badgeY, badgeW, badgeH);
        CFG.drawTextSmall(this.batch, badge, badgeX + badgeW / 2 - (int)(CFG.glyphLayout.width / 2.0f), CFG.textTopCentered(CFG.fontSmall, badgeY, badgeH), new Color(1.0f, 1.0f, 1.0f, alpha));
        this.batch.setColor(Color.WHITE);
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    @Override
    public void render(float delta) {
        int fy;
        MenuElement focusedRow;
        int[] rules;
        float pSize;
        float vy;
        float vx;
        float speed;
        if (this.socialDialogVisible && this.activeSocialButton != null) {
            this.game.setEpochVignetteColor(this.activeSocialButton.accent.r, this.activeSocialButton.accent.g, this.activeSocialButton.accent.b);
            this.game.setEpochVignetteActive(true);
        } else if (this.gratitudeDialogVisible) {
            this.game.setEpochVignetteColor(0.9f, 0.12f, 0.12f);
            this.game.setEpochVignetteActive(true);
        } else {
            this.game.setEpochVignetteActive(false);
        }
        this.fadeInTimer = Math.min(this.fadeInTimer + delta, 0.8f);
        float alpha = this.fadeInTimer / 0.8f;
        if (this.introPending) {
            this.introPending = false;
            this.introStartNanos = TimeUtils.nanoTime();
            this.rowReveal.restart();
        }
        if (Gdx.app.getType() == Application.ApplicationType.Android) {
            float accelX = Gdx.input.getAccelerometerX();
            float accelY = Gdx.input.getAccelerometerY();
            this.targetParallaxX = -accelX * 8.0f;
            this.targetParallaxY = (accelY - 5.0f) * 4.0f;
        }
        this.parallaxX += (this.targetParallaxX - this.parallaxX) * 2.5f * delta;
        this.parallaxY += (this.targetParallaxY - this.parallaxY) * 2.5f * delta;
        this.updateCharms(delta);
        if (this.menuPanel != null) {
            this.menuPanel.updateElements(delta);
        }
        this.whatsNew.update(delta);
        this.themeToggle.update(delta);
        if (MenuWorldMap.consumeConquest()) {
            this.awardSweepIfComplete();
        }
        if (!this.isDraggingScroll && Math.abs(this.scrollVelocity) > 1.0f) {
            this.scrollOffset += this.scrollVelocity * delta;
            this.scrollVelocity = (float)((double)this.scrollVelocity * Math.pow(0.9f, delta * 60.0f));
            this.scrollOffset = Math.max(0.0f, Math.min(this.maxScrollOffset, this.scrollOffset));
        } else if (!this.isDraggingScroll) {
            this.scrollVelocity = 0.0f;
        }
        for (IconButton ib : this.cornerButtons) {
            ib.pulse += delta;
            if (ib.hovered) {
                ib.hoverTransition = Math.min(1.0f, ib.hoverTransition + delta * 8.0f);
                continue;
            }
            ib.hoverTransition = Math.max(0.0f, ib.hoverTransition - delta * 8.0f);
        }
        this.applyCornerHover();
        BackgroundManager.getInstance().update(delta);
        int W = CFG.GAME_WIDTH;
        int H = CFG.GAME_HEIGHT;
        for (Particle p : this.particles) {
            p.update(delta, W, H);
        }
        this.inkTimer += delta;
        if (Math.random() < 0.08) {
            float sx = W;
            float sy = H;
            float angle = (float)Math.atan2((float)H / 2.0f - (float)H, (float)W / 2.0f - 0.0f);
            float speed2 = (float)CFG.s(350) + (float)Math.random() * (float)CFG.s(250);
            float vx2 = -((float)Math.cos(angle += (float)(Math.random() * 0.4 - 0.2)) * speed2);
            float vy2 = (float)Math.sin(angle) * speed2;
            float life = 0.7f + (float)Math.random() * 0.9f;
            float size = (float)CFG.s(3) + (float)Math.random() * (float)CFG.s(4);
            Color color = new Color(1.0f, 0.8f, 0.2f, 1.0f);
            this.achSparks.add(new CGSInGame.Spark(sx, sy, vx2, vy2, life, size, color));
        }
        Iterator<CGSInGame.Spark> sit = this.achSparks.iterator();
        while (sit.hasNext()) {
            CGSInGame.Spark s = sit.next();
            s.life -= delta;
            if (s.life <= 0.0f) {
                sit.remove();
                continue;
            }
            s.x += s.vx * delta;
            s.y += s.vy * delta;
            float jScale = delta * 60.0f;
            s.x += (float)(Math.random() * (double)CFG.s(4) - (double)CFG.s(2)) * jScale * 0.3f;
            s.y += (float)(Math.random() * (double)CFG.s(4) - (double)CFG.s(2)) * jScale * 0.3f;
            float dragFactor = (float)Math.pow(0.92, (double)delta * 60.0);
            s.vx *= dragFactor;
            s.vy *= dragFactor;
        }
        if (this.socialDialogVisible && this.activeSocialButton != null) {
            this.dialogParticleTimer += delta;
            if (this.dialogParticleTimer > 0.3f) {
                this.dialogParticleTimer = 0.3f;
            }
            float spawnInterval = 0.04f;
            while (this.dialogParticleTimer >= spawnInterval) {
                this.dialogParticleTimer -= spawnInterval;
                double angle = Math.random() * 2.0 * Math.PI;
                speed = (float)CFG.s(90) + (float)Math.random() * (float)CFG.s(60);
                vx = (float)Math.cos(angle) * speed;
                vy = (float)Math.sin(angle) * speed;
                pSize = (float)CFG.s(20) + (float)Math.random() * (float)CFG.s(16);
                float startX = (float)W / 2.0f + (float)(Math.random() * (double)CFG.s(24) - (double)CFG.s(12));
                float startY = (float)H / 2.0f + (float)(Math.random() * (double)CFG.s(24) - (double)CFG.s(12));
                this.dialogParticles.add(new DialogParticle(startX, startY, vx, vy, pSize, this.activeSocialButton.icon, this.activeSocialButton.accent));
            }
        } else if (this.gratitudeDialogVisible) {
            this.dialogParticleTimer += delta;
            if (this.dialogParticleTimer > 0.3f) {
                this.dialogParticleTimer = 0.3f;
            }
            float spawnInterval = 0.04f;
            while (this.dialogParticleTimer >= spawnInterval) {
                this.dialogParticleTimer -= spawnInterval;
                double angle = Math.random() * 2.0 * Math.PI;
                speed = (float)CFG.s(90) + (float)Math.random() * (float)CFG.s(60);
                vx = (float)Math.cos(angle) * speed;
                vy = (float)Math.sin(angle) * speed;
                pSize = (float)CFG.s(20) + (float)Math.random() * (float)CFG.s(16);
                Color heartColor = new Color(0.9f, 0.12f, 0.12f, 1.0f);
                float startX = (float)W / 2.0f + (float)(Math.random() * (double)CFG.s(24) - (double)CFG.s(12));
                float startY = (float)H / 2.0f + (float)(Math.random() * (double)CFG.s(24) - (double)CFG.s(12));
                this.dialogParticles.add(new DialogParticle(startX, startY, vx, vy, pSize, Images.diplo_heart, heartColor));
            }
        } else {
            this.dialogParticleTimer = 0.0f;
        }
        Iterator<DialogParticle> dpit = this.dialogParticles.iterator();
        while (dpit.hasNext()) {
            DialogParticle dp = dpit.next();
            dp.update(delta);
            if (!(dp.alpha <= 0.0f)) continue;
            dpit.remove();
        }
        ScreenUtils.clear(COLOR_BG_DARK);
        this.batch.setProjectionMatrix(this.uiProjection);
        this.batch.begin();
        BackgroundManager bgm = BackgroundManager.getInstance();
        bgm.drawBackdrop(this.batch, alpha, this.parallaxX, this.parallaxY, 110.0f, 60.0f);
        float bgDim = bgm.isAtlas() ? 0.14f : 1.0f;
        this.batch.setColor(0.0f, 0.0f, 0.02f, 0.7f * bgDim * alpha);
        ImageManager.getImage(Images.gradient_vertical_full).draw(this.batch, 0, H / 3, W, H * 2 / 3);
        this.batch.setColor(0.0f, 0.0f, 0.03f, 0.3f * bgDim * alpha);
        ImageManager.getImage(Images.gradientVertical).draw(this.batch, 0, 0, W, H / 3);
        this.batch.setColor(0.0f, 0.0f, 0.0f, 0.15f * bgDim * alpha);
        ImageManager.getImage(Images.pix255_255_255).draw(this.batch, 0, 0, W, H);
        this.drawGlobalDotGrid(this.parallaxX, this.parallaxY, alpha);
        this.drawEmbers(alpha);
        if (!(this.gratitudeDialogVisible || this.socialDialogVisible && this.activeSocialButton != null)) {
            this.batch.end();
            this.game.drawVignetteNow();
            this.batch.begin();
        }
        this.drawEdgeVignette(alpha);
        this.drawWordmark(alpha);
        float frameIn = this.introPart(0.0f, 0.22f);
        this.drawPanelFrame(frameIn);
        this.batch.setColor(1.0f, 1.0f, 1.0f, alpha);
        int clipX = this.panelX;
        int clipY = this.panelY + this.pad;
        int clipW = this.panelW;
        int clipH = this.panelH - this.pad * 2;
        float scaleX = (float)Gdx.graphics.getWidth() / (float)CFG.GAME_WIDTH;
        float scaleY = (float)Gdx.graphics.getHeight() / (float)CFG.GAME_HEIGHT;
        int screenClipY = CFG.GAME_HEIGHT - (clipY + clipH);
        this.batch.flush();
        Gdx.gl.glEnable(3089);
        Gdx.gl.glScissor((int)((float)clipX * scaleX), (int)((float)screenClipY * scaleY), (int)((float)clipW * scaleX), (int)((float)clipH * scaleY));
        for (int ruleY : rules = new int[]{this.separatorDrawY, this.separatorDrawY2, this.separatorDrawY3}) {
            if (ruleY <= 0) continue;
            this.drawGroupRule(ruleY - (int)this.scrollOffset, this.rowReveal.rowProgress(this.rowIndexBelow(ruleY)));
        }
        if (this.menuPanel != null && this.menuPanel.getVisible()) {
            List<MenuElement> els = this.menuPanel.getMenuElements();
            int rise = CFG.s(16);
            for (int i = 0; i < els.size(); ++i) {
                float in;
                MenuElement element = els.get(i);
                if (element == null || !element.getVisible() || (in = this.rowReveal.rowProgress(i)) <= 0.01f) continue;
                boolean pushed = BatchFade.push(this.batch, in);
                try {
                    element.draw(this.batch, 0, -((int)this.scrollOffset) + (int)this.rowReveal.rowOffset(i, rise));
                    continue;
                }
                finally {
                    if (pushed) {
                        BatchFade.pop(this.batch);
                    }
                }
            }
            this.drawRowMarker(frameIn);
        }
        this.batch.flush();
        Gdx.gl.glDisable(3089);
        if (this.keyboardFocusVisible && this.menuPanel != null && this.menuPanel.getVisible() && (focusedRow = this.getFocusedElement()) != null && (fy = focusedRow.getPosY() - (int)this.scrollOffset) + focusedRow.getHeight() > clipY && fy < clipY + clipH) {
            int ringGap = CFG.s(3);
            CFG.drawPixelBorder(this.batch, focusedRow.getPosX() - ringGap, fy - ringGap, focusedRow.getWidth() + ringGap * 2, focusedRow.getHeight() + ringGap * 2, CFG.COLOR_ICON_GOLD, Math.max(2, CFG.s(2)));
            this.batch.setColor(Color.WHITE);
        }
        if (this.maxScrollOffset > 0.0f) {
            int scrollbarX = this.panelX + this.panelW - CFG.s(12);
            float scrollRatio = this.scrollOffset / this.maxScrollOffset;
            int totalContentH = clipH + (int)this.maxScrollOffset;
            float thumbRatio = (float)clipH / (float)totalContentH;
            int thumbH = Math.max(CFG.s(20), (int)((float)clipH * thumbRatio));
            int thumbY = clipY + clipH - thumbH - (int)((float)(clipH - thumbH) * scrollRatio);
            this.batch.setColor(CFG.COLOR_IRON_BORDER.r, CFG.COLOR_IRON_BORDER.g, CFG.COLOR_IRON_BORDER.b, frameIn);
            ImageManager.getImage(Images.pix255_255_255).draw(this.batch, scrollbarX - CFG.s(1), clipY, CFG.s(3), clipH);
            this.batch.setColor(CFG.COLOR_RED_BTN.r, CFG.COLOR_RED_BTN.g, CFG.COLOR_RED_BTN.b, frameIn);
            ImageManager.getImage(Images.pix255_255_255).draw(this.batch, scrollbarX - CFG.s(2), thumbY, CFG.s(5), thumbH);
            this.batch.setColor(Color.WHITE);
        }
        if (alpha < 1.0f) {
            this.batch.setColor(MainMenuScreen.COLOR_BG_DARK.r, MainMenuScreen.COLOR_BG_DARK.g, MainMenuScreen.COLOR_BG_DARK.b, 1.0f - alpha);
            ImageManager.getImage(Images.pix255_255_255).draw(this.batch, this.panelX - CFG.s(8), this.panelY - CFG.s(8), this.panelW + CFG.s(16), this.panelH + CFG.s(16));
            this.batch.setColor(Color.WHITE);
        }
        float sideIn = this.introPart(0.16f, 0.3f);
        this.whatsNew.drawStrip(this.batch, alpha * sideIn);
        this.themeToggle.draw(this.batch, alpha * sideIn);
        this.drawDisabledHint(alpha);
        if (this.bannerOn && CFG.fontSmall != null) {
            String note = LanguageManager.get("menu_early_access_note");
            float reveal = Math.min(1.0f, this.fadeInTimer / 0.6f) * this.introPart(0.16f, 0.3f);
            if (this.inkShaderDisposed) {
                this.inkShaderDisposed = false;
                this.initInkShader();
            }
            if (this.inkShader != null && this.inkShader.isCompiled()) {
                this.batch.end();
                this.inkShader.bind();
                this.inkShader.setUniformf("u_time", this.inkTimer);
                this.inkShader.setUniformf("u_resolution", (float)this.bannerW, (float)this.bannerH);
                this.batch.setShader(this.inkShader);
                this.batch.begin();
                this.batch.setColor(1.0f, 1.0f, 1.0f, alpha * reveal);
                ImageManager.getImage(Images.pix255_255_255).draw(this.batch, this.bannerX, this.bannerY, this.bannerW, this.bannerH);
                this.batch.end();
                this.batch.setShader(null);
                this.batch.begin();
            } else {
                this.batch.setColor(0.02f, 0.03f, 0.06f, 0.85f * alpha * reveal);
                ImageManager.getImage(Images.pix255_255_255).draw(this.batch, this.bannerX, this.bannerY, this.bannerW, this.bannerH);
            }
            if (reveal > 0.15f) {
                float textAlpha = Math.min(1.0f, (reveal - 0.15f) / 0.85f) * alpha;
                float oldR = CFG.fontSmall.getColor().r;
                float oldG = CFG.fontSmall.getColor().g;
                float oldB = CFG.fontSmall.getColor().b;
                float oldA = CFG.fontSmall.getColor().a;
                int drawX = this.bannerX + this.bannerPadX;
                int drawY = this.bannerY + (this.bannerH - this.bannerNoteH) / 2;
                try {
                    CFG.fontSmall.setColor(0.0f, 0.0f, 0.02f, 0.9f * textAlpha);
                    CFG.fontSmall.draw(this.batch, note, drawX + 1, drawY + 1, this.bannerTextW, 1, true);
                    CFG.fontSmall.setColor(MainMenuScreen.COLOR_SUBTITLE.r, MainMenuScreen.COLOR_SUBTITLE.g, MainMenuScreen.COLOR_SUBTITLE.b, MainMenuScreen.COLOR_SUBTITLE.a * textAlpha);
                    CFG.fontSmall.draw(this.batch, note, drawX, drawY, this.bannerTextW, 1, true);
                }
                finally {
                    CFG.fontSmall.setColor(oldR, oldG, oldB, oldA);
                }
            }
            this.batch.setColor(Color.WHITE);
        }
        this.batch.setColor(0.0f, 0.0f, 0.0f, 0.75f * alpha);
        this.topBarRenderer.drawRadialGlow(this.batch, W, H, CFG.s(320));
        this.batch.setColor(Color.WHITE);
        for (CGSInGame.Spark s : this.achSparks) {
            float sparkAlpha = s.life / s.maxLife;
            if (sparkAlpha > 0.8f) {
                sparkAlpha = (1.0f - sparkAlpha) / 0.2f;
            }
            this.batch.setColor(s.color.r, s.color.g, s.color.b, sparkAlpha * 0.9f * alpha);
            this.topBarRenderer.drawRadialGlow(this.batch, (int)s.x, (int)s.y, (int)(s.size * 3.0f));
            ImageManager.getImage(Images.obj_dot).draw(this.batch, (int)(s.x - s.size / 2.0f), (int)(s.y - s.size / 2.0f), (int)s.size, (int)s.size);
            this.batch.setColor(Color.WHITE);
        }
        for (IconButton ib : this.cornerButtons) {
            float pop = this.cornerPop(ib);
            this.drawIconButton(ib, alpha * Ease.clamp01(pop * 3.0f), 0.6f + 0.4f * Ease.outBack(pop));
        }
        this.drawAchievementBadge(alpha * Ease.clamp01(this.cornerPop(this.achButton) * 3.0f));
        for (IconButton ib : this.cornerButtons) {
            this.drawIconButtonHint(ib, alpha);
        }
        this.themeToggle.drawHint(this.batch, alpha);
        if (this.charmBoard != null) {
            this.charmBoard.draw(this.batch, alpha);
        }
        this.drawCharmToast(alpha);

        if (CFG.fontSmall != null) {
            String badge = "⚡ CGS ModLoader v1.0.0 [Модов: " + cgs.modloader.ModLoader.getInstance().getLoadedMods().size() + "]";
            CFG.drawTextSmall(this.batch, badge, CFG.s(16), CFG.s(22), new Color(0.2f, 0.75f, 0.95f, 0.85f * alpha));
        }

        if (SettingsManager.getInstance().isShowFPS() && CFG.fontSmall != null) {
            int fps = Gdx.graphics.getFramesPerSecond();
            if (fps >= 55) {
                this.tmpFpsColor.set(0.3f, 1.0f, 0.5f, alpha);
            } else if (fps >= 30) {
                this.tmpFpsColor.set(1.0f, 0.85f, 0.2f, alpha);
            } else {
                this.tmpFpsColor.set(1.0f, 0.3f, 0.3f, alpha);
            }
            String fpsText = "FPS: " + fps;
            CFG.glyphLayout.setText(CFG.fontSmall, fpsText);
            CFG.drawTextSmall(this.batch, fpsText, W - CFG.s(10) - (int)CFG.glyphLayout.width, CFG.s(10), this.tmpFpsColor);
        }
        if (this.gratitudeDialogVisible || this.socialDialogVisible && this.activeSocialButton != null) {
            float dialogTimer = this.gratitudeDialogVisible ? this.gratitudeDialogTimer : this.socialDialogTimer;
            float dialogAlpha = Math.min(1.0f, dialogTimer / 0.4f);
            this.batch.setColor(0.0f, 0.0f, 0.0f, 0.7f * dialogAlpha);
            ImageManager.getImage(Images.pix255_255_255).draw(this.batch, 0, 0, W, H);
            for (DialogParticle dp : this.dialogParticles) {
                this.batch.setColor(dp.color.r, dp.color.g, dp.color.b, Math.min(1.0f, dp.alpha * 1.3f) * alpha);
                Image img = ImageManager.getImage(dp.icon);
                img.draw(this.batch, (int)(dp.x - dp.size / 2.0f), (int)(dp.y - dp.size / 2.0f), (int)(dp.size / 2.0f), (int)(dp.size / 2.0f), (int)dp.size, (int)dp.size, 1.0f, 1.0f, dp.rotation, 0, 0, img.getWidth(), img.getHeight(), false, true);
            }
            this.batch.setColor(Color.WHITE);
            this.batch.end();
            this.game.drawVignetteNow();
            this.batch.begin();
        }
        this.whatsNew.drawExpanded(this.batch, alpha);
        if (this.gratitudeDialogVisible) {
            this.drawGratitudeDialog(delta);
        } else if (this.socialDialogVisible && this.activeSocialButton != null) {
            this.drawSocialDialog(delta);
        }
        this.batch.setColor(Color.WHITE);
        this.charmConfirm.draw(this.batch);
        this.themeReveal.draw(this.batch);
        this.batch.setColor(Color.WHITE);
        cgs.modloader.update.ModUpdateChecker.getInstance().renderBanner(this.batch, null, CFG.fontMain, delta, W, H);
        this.batch.end();
        this.themeReveal.captureThenSwitch(this.switchTheme);
    }

    private void drawSocialDialogButton(SpriteBatch sb, int x, int y, int w, int h, int index, String text, float alpha) {
        boolean hovered = this.socialDialogHoverIndex == index;
        int pressOffset = this.socialDialogPressedIndex == index ? 2 : 0;
        Color btnCol = new Color();
        if (index == 0) {
            btnCol.set(this.activeSocialButton.accent.r, this.activeSocialButton.accent.g, this.activeSocialButton.accent.b, alpha);
        } else {
            btnCol.set(0.5f, 0.5f, 0.5f, alpha);
        }
        if (hovered && this.socialDialogPressedIndex == -1) {
            btnCol.lerp(Color.WHITE, 0.15f);
        }
        CFG.drawIronButtonBg(sb, x, y, w, h, btnCol, pressOffset);
        CFG.fontMain.getData().setScale(1.0f);
        CFG.glyphLayout.setText(CFG.fontMain, text);
        int textW = (int)CFG.glyphLayout.width;
        int textH = (int)CFG.glyphLayout.height;
        CFG.drawTextWithShadow(sb, text, x + (w - textW) / 2 + pressOffset, y + (h - textH) / 2 + pressOffset, Color.WHITE);
    }

    private void drawSocialDialog(float delta) {
        this.socialDialogTimer += delta;
        float alpha = Math.min(1.0f, this.socialDialogTimer / 0.4f);
        int W = CFG.GAME_WIDTH;
        int H = CFG.GAME_HEIGHT;
        int baseW = Math.min(CFG.s(420), W - CFG.s(40));
        String descText = "";
        String titleText = this.activeSocialButton.label;
        String confirmBtnText = LanguageManager.get("menu_social_follow");
        String cancelBtnText = LanguageManager.get("menu_social_cancel");
        descText = LanguageManager.get(this.activeSocialButton.descKey != null ? this.activeSocialButton.descKey : "menu_social_website");
        int contentW = baseW - CFG.s(40);
        CFG.fontMain.getData().setScale(1.0f);
        CFG.glyphLayout.setText(CFG.fontMain, descText, Color.WHITE, contentW, 1, true);
        float descH = CFG.glyphLayout.height;
        int baseH = CFG.s(176) + (int)descH;
        float scale = 0.85f + 0.15f * Math.min(1.0f, this.socialDialogTimer / 0.4f);
        this.socialDialogW = (int)((float)baseW * scale);
        this.socialDialogH = (int)((float)baseH * scale);
        this.socialDialogX = (W - this.socialDialogW) / 2;
        this.socialDialogY = (H - this.socialDialogH) / 2;
        this.batch.setColor(1.0f, 1.0f, 1.0f, alpha);
        CFG.drawIronMenuPanel(this.batch, this.socialDialogX, this.socialDialogY, this.socialDialogW, this.socialDialogH);
        int accentH = CFG.s(6);
        this.batch.setColor(this.activeSocialButton.accent.r, this.activeSocialButton.accent.g, this.activeSocialButton.accent.b, alpha);
        ImageManager.getImage(Images.pix255_255_255).draw(this.batch, this.socialDialogX + CFG.s(4), this.socialDialogY + CFG.s(4), this.socialDialogW - CFG.s(8), accentH);
        this.batch.setColor(Color.WHITE);
        float iconScale = Math.min(1.0f, this.socialDialogTimer / 0.25f);
        int baseIconSize = CFG.s(64);
        int animIconSize = (int)((float)baseIconSize * iconScale);
        int iconX = this.socialDialogX + this.socialDialogW / 2 - animIconSize / 2;
        int iconY = this.socialDialogY - animIconSize / 2;
        if (animIconSize > 0) {
            float glowPulse = 0.5f + 0.5f * (float)Math.sin((double)this.socialDialogTimer * 4.0);
            this.batch.setColor(this.activeSocialButton.accent.r, this.activeSocialButton.accent.g, this.activeSocialButton.accent.b, (0.4f + 0.1f * glowPulse) * alpha);
            ImageManager.getImage(Images.obj_dot).draw(this.batch, iconX - CFG.s(6), iconY - CFG.s(6), animIconSize + CFG.s(12), animIconSize + CFG.s(12));
            this.batch.setColor(CFG.COLOR_TEXT_DARK.r, CFG.COLOR_TEXT_DARK.g, CFG.COLOR_TEXT_DARK.b, alpha);
            int borderW = CFG.s(3);
            ImageManager.getImage(Images.obj_dot).draw(this.batch, iconX - borderW, iconY - borderW, animIconSize + borderW * 2, animIconSize + borderW * 2);
            this.batch.setColor(0.12f, 0.1f, 0.06f, 0.95f * alpha);
            ImageManager.getImage(Images.obj_dot).draw(this.batch, iconX, iconY, animIconSize, animIconSize);
            int pad = CFG.s(12);
            this.batch.setColor(1.0f, 1.0f, 1.0f, alpha);
            ImageManager.getImage(this.activeSocialButton.icon).draw(this.batch, iconX + pad, iconY + pad, animIconSize - pad * 2, animIconSize - pad * 2);
            this.batch.setColor(Color.WHITE);
        }
        int contentY = this.socialDialogY + CFG.s(36);
        CFG.fontLarge.getData().setScale(1.0f);
        CFG.glyphLayout.setText(CFG.fontLarge, titleText);
        int titleW = (int)CFG.glyphLayout.width;
        int titleX = this.socialDialogX + (this.socialDialogW - titleW) / 2;
        Color textColor = new Color(CFG.COLOR_TEXT_DARK.r, CFG.COLOR_TEXT_DARK.g, CFG.COLOR_TEXT_DARK.b, alpha);
        CFG.drawTextLarge(this.batch, titleText, titleX, contentY, textColor);
        this.batch.setColor(CFG.COLOR_SEPARATOR_LINE.r, CFG.COLOR_SEPARATOR_LINE.g, CFG.COLOR_SEPARATOR_LINE.b, alpha);
        ImageManager.getImage(Images.pix255_255_255).draw(this.batch, this.socialDialogX + CFG.s(20), contentY += CFG.s(28), this.socialDialogW - CFG.s(40), CFG.s(1));
        this.batch.setColor(Color.WHITE);
        CFG.fontMain.getData().setScale(1.0f);
        CFG.glyphLayout.setText(CFG.fontMain, descText, textColor, contentW, 1, true);
        CFG.fontMain.draw((Batch)this.batch, CFG.glyphLayout, (float)(this.socialDialogX + CFG.s(20)), (float)(contentY += CFG.s(20)));
        CFG.fontMain.setColor(Color.WHITE);
        int spacing = CFG.s(20);
        this.socialBtnW = (this.socialDialogW - CFG.s(40) - spacing) / 2;
        this.socialBtnH = CFG.s(44);
        this.socialBtnCancelX = this.socialDialogX + CFG.s(20);
        this.socialBtnCancelY = this.socialDialogY + this.socialDialogH - this.socialBtnH - CFG.s(20);
        this.socialBtnConfirmX = this.socialBtnCancelX + this.socialBtnW + spacing;
        this.socialBtnConfirmY = this.socialBtnCancelY;
        this.drawSocialDialogButton(this.batch, this.socialBtnCancelX, this.socialBtnCancelY, this.socialBtnW, this.socialBtnH, 1, cancelBtnText, alpha);
        this.drawSocialDialogButton(this.batch, this.socialBtnConfirmX, this.socialBtnConfirmY, this.socialBtnW, this.socialBtnH, 0, confirmBtnText, alpha);
    }

    @Override
    public void resize(int width, int height) {
        CFG.GAME_WIDTH = width;
        CFG.GAME_HEIGHT = height;
        this.themeReveal.cancel();
        this.updateUIProjection();
        this.buildMenu();
    }

    @Override
    public void dispose() {
        this.themeReveal.dispose();
        if (this.inkShader != null) {
            this.inkShader.dispose();
            this.inkShader = null;
            this.inkShaderDisposed = true;
        }
    }

    private void drawGratitudeDialogButton(SpriteBatch sb, int x, int y, int w, int h, int index, String text, float alpha) {
        boolean hovered = this.gratitudeDialogHoverIndex == index;
        int pressOffset = this.gratitudeDialogPressedIndex == index ? 2 : 0;
        Color btnCol = new Color(0.9f, 0.12f, 0.12f, alpha);
        if (hovered && this.gratitudeDialogPressedIndex == -1) {
            btnCol.lerp(Color.WHITE, 0.15f);
        }
        CFG.drawIronButtonBg(sb, x, y, w, h, btnCol, pressOffset);
        CFG.fontMain.getData().setScale(1.0f);
        CFG.glyphLayout.setText(CFG.fontMain, text);
        int textW = (int)CFG.glyphLayout.width;
        int textH = (int)CFG.glyphLayout.height;
        CFG.drawTextWithShadow(sb, text, x + (w - textW) / 2 + pressOffset, y + (h - textH) / 2 + pressOffset, Color.WHITE);
    }

    private void drawGratitudeDialog(float delta) {
        this.gratitudeDialogTimer += delta;
        float alpha = Math.min(1.0f, this.gratitudeDialogTimer / 0.4f);
        int W = CFG.GAME_WIDTH;
        int H = CFG.GAME_HEIGHT;
        int baseW = Math.min(CFG.s(460), W - CFG.s(40));
        String descText = LanguageManager.get("menu_gratitude_desc");
        String titleText = LanguageManager.get("menu_gratitude_title");
        String confirmBtnText = LanguageManager.get("menu_gratitude_confirm");
        int contentW = baseW - CFG.s(40);
        CFG.fontMain.getData().setScale(1.0f);
        CFG.glyphLayout.setText(CFG.fontMain, descText, Color.WHITE, contentW, 1, true);
        float descH = CFG.glyphLayout.height;
        int baseH = CFG.s(176) + (int)descH;
        float scale = 0.85f + 0.15f * Math.min(1.0f, this.gratitudeDialogTimer / 0.4f);
        this.gratitudeDialogW = (int)((float)baseW * scale);
        this.gratitudeDialogH = (int)((float)baseH * scale);
        this.gratitudeDialogX = (W - this.gratitudeDialogW) / 2;
        this.gratitudeDialogY = (H - this.gratitudeDialogH) / 2;
        this.batch.setColor(1.0f, 1.0f, 1.0f, alpha);
        CFG.drawIronMenuPanel(this.batch, this.gratitudeDialogX, this.gratitudeDialogY, this.gratitudeDialogW, this.gratitudeDialogH);
        int accentH = CFG.s(6);
        this.batch.setColor(0.9f, 0.12f, 0.12f, alpha);
        ImageManager.getImage(Images.pix255_255_255).draw(this.batch, this.gratitudeDialogX + CFG.s(4), this.gratitudeDialogY + CFG.s(4), this.gratitudeDialogW - CFG.s(8), accentH);
        this.batch.setColor(Color.WHITE);
        float iconScale = Math.min(1.0f, this.gratitudeDialogTimer / 0.25f);
        int baseIconSize = CFG.s(64);
        int animIconSize = (int)((float)baseIconSize * iconScale);
        int iconX = this.gratitudeDialogX + this.gratitudeDialogW / 2 - animIconSize / 2;
        int iconY = this.gratitudeDialogY - animIconSize / 2;
        if (animIconSize > 0) {
            float glowPulse = 0.5f + 0.5f * (float)Math.sin((double)this.gratitudeDialogTimer * 4.0);
            this.batch.setColor(0.9f, 0.12f, 0.12f, (0.4f + 0.1f * glowPulse) * alpha);
            ImageManager.getImage(Images.obj_dot).draw(this.batch, iconX - CFG.s(6), iconY - CFG.s(6), animIconSize + CFG.s(12), animIconSize + CFG.s(12));
            this.batch.setColor(CFG.COLOR_TEXT_DARK.r, CFG.COLOR_TEXT_DARK.g, CFG.COLOR_TEXT_DARK.b, alpha);
            int borderW = CFG.s(3);
            ImageManager.getImage(Images.obj_dot).draw(this.batch, iconX - borderW, iconY - borderW, animIconSize + borderW * 2, animIconSize + borderW * 2);
            this.batch.setColor(0.12f, 0.1f, 0.06f, 0.95f * alpha);
            ImageManager.getImage(Images.obj_dot).draw(this.batch, iconX, iconY, animIconSize, animIconSize);
            int pad = CFG.s(12);
            this.batch.setColor(1.0f, 1.0f, 1.0f, alpha);
            ImageManager.getImage(Images.ic_mat_emoji_events).draw(this.batch, iconX + pad, iconY + pad, animIconSize - pad * 2, animIconSize - pad * 2);
            this.batch.setColor(Color.WHITE);
        }
        int contentY = this.gratitudeDialogY + CFG.s(36);
        CFG.fontLarge.getData().setScale(1.0f);
        CFG.glyphLayout.setText(CFG.fontLarge, titleText);
        int titleW = (int)CFG.glyphLayout.width;
        int titleX = this.gratitudeDialogX + (this.gratitudeDialogW - titleW) / 2;
        Color textColor = new Color(CFG.COLOR_TEXT_DARK.r, CFG.COLOR_TEXT_DARK.g, CFG.COLOR_TEXT_DARK.b, alpha);
        CFG.drawTextLarge(this.batch, titleText, titleX, contentY, textColor);
        this.batch.setColor(CFG.COLOR_SEPARATOR_LINE.r, CFG.COLOR_SEPARATOR_LINE.g, CFG.COLOR_SEPARATOR_LINE.b, alpha);
        ImageManager.getImage(Images.pix255_255_255).draw(this.batch, this.gratitudeDialogX + CFG.s(20), contentY += CFG.s(28), this.gratitudeDialogW - CFG.s(40), CFG.s(1));
        this.batch.setColor(Color.WHITE);
        CFG.fontMain.getData().setScale(1.0f);
        CFG.glyphLayout.setText(CFG.fontMain, descText, textColor, contentW, 1, true);
        CFG.fontMain.draw((Batch)this.batch, CFG.glyphLayout, (float)(this.gratitudeDialogX + CFG.s(20)), (float)(contentY += CFG.s(20)));
        CFG.fontMain.setColor(Color.WHITE);
        this.gratitudeBtnW = this.gratitudeDialogW - CFG.s(80);
        this.gratitudeBtnH = CFG.s(44);
        this.gratitudeBtnConfirmX = this.gratitudeDialogX + (this.gratitudeDialogW - this.gratitudeBtnW) / 2;
        this.gratitudeBtnConfirmY = this.gratitudeDialogY + this.gratitudeDialogH - this.gratitudeBtnH - CFG.s(20);
        this.drawGratitudeDialogButton(this.batch, this.gratitudeBtnConfirmX, this.gratitudeBtnConfirmY, this.gratitudeBtnW, this.gratitudeBtnH, 0, confirmBtnText, alpha);
    }

    private static class IconButton {
        final int icon;
        final String label;
        final Color accent;
        final String url;
        final Runnable action;
        final int glyphInset;
        final boolean tintGlyph;
        final String descKey;
        boolean hovered = false;
        boolean pressed = false;
        float pulse = 0.0f;
        float hoverTransition = 0.0f;
        int x;
        int y;
        int size;
        int baseSize;
        int slotX;
        int slotY;
        boolean hintAbove;

        IconButton(int icon, String label, Color accent, String url, Runnable action, int glyphInset, boolean tintGlyph) {
            this(icon, label, accent, url, action, glyphInset, tintGlyph, null);
        }

        IconButton(int icon, String label, Color accent, String url, Runnable action, int glyphInset, boolean tintGlyph, String descKey) {
            this.icon = icon;
            this.label = label;
            this.accent = accent;
            this.url = url;
            this.action = action;
            this.glyphInset = glyphInset;
            this.tintGlyph = tintGlyph;
            this.descKey = descKey;
        }

        boolean contains(int px, int py) {
            return px >= this.x && px < this.x + this.size && py >= this.y && py < this.y + this.size;
        }
    }

    private static class Particle {
        float x;
        float y;
        float vx;
        float vy;
        float alpha;
        float size;
        float alphaSpeed;
        float time;
        Color color;

        Particle(int screenW, int screenH) {
            this.reset(screenW, screenH, true);
        }

        void reset(int screenW, int screenH, boolean randomY) {
            this.x = (float)(Math.random() * (double)screenW);
            this.y = randomY ? (float)(Math.random() * (double)screenH) : (float)screenH + (float)(Math.random() * 40.0);
            this.vx = (float)(Math.random() * 0.4 - 0.2);
            this.vy = (float)(-(Math.random() * 0.4 + 0.15));
            this.size = (float)(2.0 + Math.random() * 4.0);
            this.alpha = (float)(Math.random() * 0.4 + 0.1);
            this.alphaSpeed = (float)(Math.random() * 0.3 + 0.1);
            this.time = (float)(Math.random() * 6.28);
            double rand = Math.random();
            this.color = rand < 0.4 ? EMBER_RED : (rand < 0.8 ? EMBER_ORANGE : EMBER_YELLOW);
        }

        void update(float delta, int screenW, int screenH) {
            this.time += delta;
            float k = delta * 60.0f;
            this.x += (this.vx + (float)Math.sin((double)this.time * 0.5) * 0.15f) * k;
            this.y += this.vy * k;
            this.alpha += (float)Math.sin(this.time * this.alphaSpeed) * 0.002f * k;
            this.alpha = Math.max(0.05f, Math.min(0.6f, this.alpha));
            if (this.y < -40.0f || this.x < -40.0f || this.x > (float)(screenW + 40)) {
                this.reset(screenW, screenH, false);
                this.y = (float)screenH + (float)(Math.random() * 20.0);
            }
        }
    }

    private static class DialogParticle {
        float x;
        float y;
        float vx;
        float vy;
        float size;
        float alpha;
        float rotation;
        float rotationSpeed;
        int icon;
        Color color;

        DialogParticle(float x, float y, float vx, float vy, float size, int icon, Color color) {
            this.x = x;
            this.y = y;
            this.vx = vx;
            this.vy = vy;
            this.size = size;
            this.icon = icon;
            this.color = color;
            this.alpha = 1.0f;
            this.rotation = (float)(Math.random() * 360.0);
            this.rotationSpeed = (float)(Math.random() * 200.0 - 100.0);
        }

        void update(float delta) {
            this.x += this.vx * delta;
            this.y += this.vy * delta;
            this.vx *= (float)Math.pow(0.995f, delta * 60.0f);
            this.vy *= (float)Math.pow(0.995f, delta * 60.0f);
            this.rotation += this.rotationSpeed * delta;
            this.alpha -= delta * 0.35f;
        }
    }
}

