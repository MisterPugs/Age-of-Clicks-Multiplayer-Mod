/*
 * Decompiled with CFR 0.152.
 */
package ru.ius.david.cgs;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.InputAdapter;
import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.utils.ScreenUtils;
import com.codedisaster.steamworks.SteamAuth;
import com.codedisaster.steamworks.SteamAuthTicket;
import com.codedisaster.steamworks.SteamID;
import com.codedisaster.steamworks.SteamResult;
import com.codedisaster.steamworks.SteamUser;
import com.codedisaster.steamworks.SteamUserCallback;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import ru.ius.david.cgs.CFG;
import ru.ius.david.cgs.MapScreen;
import ru.ius.david.cgs.ScenarioData;
import ru.ius.david.cgs.ScenarioFileResolver;
import ru.ius.david.cgs.cgsGame;
import ru.ius.david.cgs.elements.ButtonElement;
import ru.ius.david.cgs.elements.MenuElement;
import ru.ius.david.cgs.game.LanguageManager;
import ru.ius.david.cgs.game.SettingsManager;
import ru.ius.david.cgs.images.BackgroundManager;
import ru.ius.david.cgs.images.Image;
import ru.ius.david.cgs.images.ImageManager;
import ru.ius.david.cgs.images.Images;
import ru.ius.david.cgs.menus.SliderMenu;
import ru.ius.david.cgs.multiplayer.MultiplayerManager;
import ru.ius.david.cgs.multiplayer.NetworkProtocol;
import ru.ius.david.cgs.multiplayer.steam.SteamLobbyManager;
import ru.ius.david.cgs.steam.SteamManager;
import ru.ius.david.cgs.steam.SteamWorkshopManager;
import ru.ius.david.cgs.ui.CGSTextInputDialog;

public class MultiplayerLobbyScreen
extends ScreenAdapter {

    private boolean localReady = false;
    private boolean isSpectator = false;

    private String getLocalHostIp() {
        try {
            return InetAddress.getLocalHost().getHostAddress();
        } catch (Exception e) {
            return "127.0.0.1";
        }
    }

    private void promptDirectIp() {
        if (this.textInputDialog == null) {
            this.textInputDialog = new CGSTextInputDialog();
        }
        this.textInputDialog.show("ПОДКЛЮЧЕНИЕ ПО IP", "Введите IP адрес сервера (например 127.0.0.1):", "127.0.0.1", new CGSTextInputDialog.OnInputResult() {
            @Override
            public void onResult(String text) {
                if (text != null && !text.trim().isEmpty()) {
                    try {
                        String host = text.trim();
                        MultiplayerLobbyScreen.this.onJoinRoom(InetAddress.getByName(host));
                    } catch (Exception e) {
                        MultiplayerLobbyScreen.this.statusText = "Ошибка IP: " + e.getMessage();
                    }
                }
            }
            @Override
            public void onCancel() {}
        });
    }

    private cgsGame game;
    private SpriteBatch batch;
    private Matrix4 uiProjection;
    private MultiplayerManager mpManager;
    private final Color tmpFpsColor = new Color();
    private SliderMenu activePanel;
    private CGSTextInputDialog textInputDialog;
    private LobbyMode mode = LobbyMode.MENU;
    private String statusText = "";
    private List<NetworkProtocol.PlayerInfo> playerList = new ArrayList<NetworkProtocol.PlayerInfo>();
    private String localPlayerName = "Player";
    private List<ScenarioData> allScenarios;
    private int selectedScenarioIndex = 0;
    private InetAddress selectedRoomAddress = null;
    private boolean isSearching = false;
    private boolean steamMode = false;
    private List<SteamLobbyManager.SteamLobbyInfo> steamLobbies = new ArrayList<SteamLobbyManager.SteamLobbyInfo>();
    private SteamLobbyManager.SteamLobbyInfo selectedSteamLobby = null;
    private boolean isSteamSearching = false;
    private SteamLobbyManager discoveryLobbyManager = null;
    private int panelX;
    private int panelY;
    private int panelW;
    private int panelH;
    private int nameFieldY;
    private float bgTimer = 0.0f;
    private static final int PARTICLE_COUNT = 40;
    private ArrayList<Particle> particles;
    private static final Color COLOR_BG = new Color(0.03f, 0.03f, 0.08f, 1.0f);
    private static final Color COLOR_BTN_HOST = CFG.COLOR_RED_BTN;
    private static final Color COLOR_BTN_JOIN = CFG.COLOR_PARCHMENT_BTN_BG;
    private static final Color COLOR_BTN_START = CFG.COLOR_RED_BTN;
    private static final Color COLOR_BTN_BACK = CFG.COLOR_TERTIARY_BTN;
    private static final Color COLOR_BTN_READY = new Color(0.2f, 0.55f, 0.25f, 1.0f);
    private static final Color COLOR_BTN_NOT_READY = new Color(0.65f, 0.3f, 0.2f, 1.0f);
    private static final Color COLOR_TEXT_DARK = CFG.COLOR_TEXT_DARK;
    private static final Color COLOR_TEXT_DIM = CFG.COLOR_TEXT_MUTED_WARM;
    private static final Color COLOR_PLAYER_ROW = CFG.themed("d9ccb8cc", "4a4a4fcc");
    private static final Color COLOR_READY_YES = new Color(0.2f, 0.7f, 0.3f, 1.0f);
    private static final Color COLOR_READY_NO = new Color(0.75f, 0.25f, 0.2f, 1.0f);
    private static final Color COLOR_HOST_BADGE = new Color(0.85f, 0.65f, 0.15f, 1.0f);
    private static final Color COLOR_COUNTRY_NAME = new Color(0.25f, 0.4f, 0.65f, 1.0f);
    private float fadeInTimer = 0.0f;
    private static final float FADE_IN_DURATION = 0.6f;
    private Map<Integer, String> countryNameLookup = new HashMap<Integer, String>();

    private void loadScenarios() {
        this.allScenarios = ScenarioData.loadAll();
        if (this.allScenarios == null || this.allScenarios.isEmpty()) {
            this.allScenarios = new ArrayList<ScenarioData>();
            this.allScenarios.add(new ScenarioData("1936", "Europe 1936", "", 1936, "scenarios/1936/"));
        }
        for (int i = 0; i < this.allScenarios.size(); ++i) {
            if (!"1936".equals(this.allScenarios.get((int)i).id)) continue;
            this.selectedScenarioIndex = i;
            break;
        }
    }

    public MultiplayerLobbyScreen(cgsGame game) {
        this.game = game;
        this.batch = game.getSharedBatch();
        try {
            this.mpManager = new MultiplayerManager();
        }
        catch (Throwable t) {
            Gdx.app.log("MP", "Failed to initialize MultiplayerManager: " + t.getMessage(), t);
            throw new RuntimeException("KryoNet failed to load: " + t.getMessage(), t);
        }
        this.localPlayerName = SettingsManager.getInstance().getPlayerName();
        if (this.localPlayerName == null || this.localPlayerName.isEmpty()) {
            this.localPlayerName = "Player";
        }
        this.textInputDialog = new CGSTextInputDialog();
        this.loadScenarios();
        this.initBackgrounds();
        this.initParticles();
        this.updateUIProjection();
        this.buildMainMenu();
    }

    private void initBackgrounds() {
    }

    private void initParticles() {
        this.particles = new ArrayList();
        int W = Gdx.graphics.getWidth();
        int H = Gdx.graphics.getHeight();
        for (int i = 0; i < 40; ++i) {
            this.particles.add(new Particle(W, H));
        }
    }

    private void updateUIProjection() {
        int w = Gdx.graphics.getWidth();
        int h = Gdx.graphics.getHeight();
        this.uiProjection = new Matrix4();
        this.uiProjection.setToOrtho2D(0.0f, 0.0f, w, h);
        this.uiProjection.val[5] = -2.0f / (float)h;
        this.uiProjection.val[13] = 1.0f;
    }

    private void buildMainMenu() {
        int W = CFG.GAME_WIDTH;
        int H = CFG.GAME_HEIGHT;
        int btnW = Math.min(CFG.s(360), W - CFG.s(80));
        int btnH = CFG.s(56);
        int gap = CFG.s(16);
        int pad = CFG.s(32);
        boolean steamAvailable = SteamManager.isAvailable();
        int totalButtons = this.steamMode ? 3 : 4;
        this.panelW = btnW + pad * 2;
        int currentYOffset = pad;
        int tabY = 0;
        int tabH = CFG.s(36);
        if (steamAvailable) {
            tabY = currentYOffset;
            currentYOffset += tabH + gap;
        }
        this.nameFieldY = currentYOffset;
        currentYOffset += CFG.s(44) + gap;
        int roleRowY = currentYOffset;
        int roleRowH = CFG.s(36);
        currentYOffset += roleRowH + gap;
        int btnY = currentYOffset;
        this.panelH = (currentYOffset += btnH * totalButtons + gap * (totalButtons - 1)) + pad;
        this.panelX = (W - this.panelW) / 2;
        this.panelY = (H - this.panelH) / 2;
        if (steamAvailable) {
            tabY += this.panelY;
        }
        this.nameFieldY += this.panelY;
        roleRowY += this.panelY;
        btnY += this.panelY;
        int btnX = this.panelX + pad;
        ArrayList<MenuElement> elements = new ArrayList<MenuElement>();
        if (steamAvailable) {
            int tabW = (btnW - gap) / 2;
            Color lanBg = this.steamMode ? CFG.COLOR_TERTIARY_BTN : CFG.COLOR_PARCHMENT_BTN_BG;
            Color lanTxt = this.steamMode ? COLOR_TEXT_DIM : COLOR_TEXT_DARK;
            elements.add(new ButtonElement(btnX, tabY, tabW, tabH, "LAN", lanBg, lanTxt, new Runnable(){

                @Override
                public void run() {
                    MultiplayerLobbyScreen.this.steamMode = false;
                    MultiplayerLobbyScreen.this.buildMainMenu();
                    MultiplayerLobbyScreen.this.setupInput();
                }
            }));
            Color steamBg = this.steamMode ? new Color(0.15f, 0.35f, 0.65f, 1.0f) : CFG.COLOR_TERTIARY_BTN;
            Color steamTxt = this.steamMode ? Color.WHITE : COLOR_TEXT_DIM;
            elements.add(new ButtonElement(btnX + tabW + gap, tabY, tabW, tabH, LanguageManager.get("mp_steam_tab"), steamBg, steamTxt, new Runnable(){

                @Override
                public void run() {
                    MultiplayerLobbyScreen.this.steamMode = true;
                    MultiplayerLobbyScreen.this.buildMainMenu();
                    MultiplayerLobbyScreen.this.setupInput();
                }
            }));
        }

        int roleW = (btnW - gap) / 2;
        Color playerBg = !this.isSpectator ? new Color(0.18f, 0.5f, 0.25f, 1.0f) : CFG.COLOR_TERTIARY_BTN;
        Color playerTxt = !this.isSpectator ? Color.WHITE : COLOR_TEXT_DIM;
        elements.add(new ButtonElement(btnX, roleRowY, roleW, roleRowH, "ИГРОК", playerBg, playerTxt, new Runnable() {
            @Override
            public void run() {
                MultiplayerLobbyScreen.this.isSpectator = false;
                cgs.modloader.spectator.SpectatorManager.getInstance().setSpectatorMode(false);
                MultiplayerLobbyScreen.this.buildMainMenu();
                MultiplayerLobbyScreen.this.setupInput();
            }
        }));

        Color specBg = this.isSpectator ? new Color(0.15f, 0.45f, 0.75f, 1.0f) : CFG.COLOR_TERTIARY_BTN;
        Color specTxt = this.isSpectator ? Color.WHITE : COLOR_TEXT_DIM;
        elements.add(new ButtonElement(btnX + roleW + gap, roleRowY, roleW, roleRowH, "ЗРИТЕЛЬ", specBg, specTxt, new Runnable() {
            @Override
            public void run() {
                MultiplayerLobbyScreen.this.isSpectator = true;
                cgs.modloader.spectator.SpectatorManager.getInstance().setSpectatorMode(true);
                MultiplayerLobbyScreen.this.buildMainMenu();
                MultiplayerLobbyScreen.this.setupInput();
            }
        }));

        String hostLabel = this.steamMode ? "Steam: Host" : LanguageManager.get("mp_host");
        String joinLabel = this.steamMode ? "Steam: Join" : LanguageManager.get("mp_join");
        elements.add(new ButtonElement(btnX, btnY, btnW, btnH, hostLabel, COLOR_BTN_HOST, Color.WHITE, new Runnable(){

            @Override
            public void run() {
                MultiplayerLobbyScreen.this.onHostClick();
            }
        }));
        elements.add(new ButtonElement(btnX, btnY + btnH + gap, btnW, btnH, joinLabel, COLOR_BTN_JOIN, COLOR_TEXT_DARK, new Runnable(){

            @Override
            public void run() {
                MultiplayerLobbyScreen.this.onJoinClick();
            }
        }));
        if (!this.steamMode) {
            elements.add(new ButtonElement(btnX, btnY + (btnH + gap) * 2, btnW, btnH, "ПРЯМОЕ ПОДКЛЮЧЕНИЕ (IP)", new Color(0.12f, 0.42f, 0.7f, 1.0f), Color.WHITE, new Runnable(){
                @Override
                public void run() {
                    MultiplayerLobbyScreen.this.promptDirectIp();
                }
            }));
        }
        elements.add(new ButtonElement(btnX, btnY + (btnH + gap) * (this.steamMode ? 2 : 3), btnW, btnH, LanguageManager.get("mp_back"), COLOR_BTN_BACK, Color.WHITE, new Runnable(){

            @Override
            public void run() {
                MultiplayerLobbyScreen.this.onBackClick();
            }
        }));

        this.activePanel = new SliderMenu();
        this.activePanel.initMenu(null, 0, 0, W, H, elements, true, false);
        this.activePanel.setVisible(true);
    }

    private void buildConnectingPanel() {
        int W = CFG.GAME_WIDTH;
        int H = CFG.GAME_HEIGHT;
        int btnW = Math.min(CFG.s(360), W - CFG.s(80));
        int btnH = CFG.s(48);
        int pad = CFG.s(28);
        this.panelW = btnW + pad * 2;
        this.panelH = CFG.s(120) + btnH + pad * 2;
        this.panelX = (W - this.panelW) / 2;
        this.panelY = (H - this.panelH) / 2;
        ArrayList<MenuElement> elements = new ArrayList<MenuElement>();
        int btnX = this.panelX + pad;
        int cancelY = this.panelY + this.panelH - pad - btnH;
        elements.add(new ButtonElement(btnX, cancelY, btnW, btnH, LanguageManager.get("mp_disconnect"), COLOR_BTN_BACK, Color.WHITE, new Runnable(){

            @Override
            public void run() {
                MultiplayerLobbyScreen.this.onBackFromJoin();
            }
        }));

        this.activePanel = new SliderMenu();
        this.activePanel.initMenu(null, 0, 0, W, H, elements, true, false);
        this.activePanel.setVisible(true);
    }

    private void buildLobbyPanel() {
        int W = CFG.GAME_WIDTH;
        int H = CFG.GAME_HEIGHT;
        int btnW = Math.min(CFG.s(360), W - CFG.s(80));
        int btnH = CFG.s(48);
        int gap = CFG.s(12);
        int pad = CFG.s(28);
        int playerAreaH = CFG.s(38) * 8 + CFG.s(44);
        int roleRowH = CFG.s(36);
        int extraH = this.mpManager.isHost() ? btnH + gap : roleRowH + gap;
        this.panelW = btnW + pad * 2;
        this.panelH = playerAreaH + btnH + gap + extraH + pad * 2;
        this.panelX = (W - this.panelW) / 2;
        this.panelY = (H - this.panelH) / 2;
        ArrayList<MenuElement> elements = new ArrayList<MenuElement>();
        int btnX = this.panelX + pad;
        int bottomRow1Y = this.panelY + this.panelH - pad - btnH;
        if (this.mpManager.isHost()) {
            int halfW = btnW / 2 - gap / 2;
            elements.add(new ButtonElement(btnX, bottomRow1Y, halfW, btnH, LanguageManager.get("mp_start"), COLOR_BTN_START, Color.WHITE, new Runnable(){

                @Override
                public void run() {
                    MultiplayerLobbyScreen.this.onStartClick();
                }
            }));
            elements.add(new ButtonElement(btnX + halfW + gap, bottomRow1Y, halfW, btnH, LanguageManager.get("mp_disconnect"), COLOR_BTN_BACK, Color.WHITE, new Runnable(){

                @Override
                public void run() {
                    MultiplayerLobbyScreen.this.onDisconnectClick();
                }
            }));
            int scenarioY = bottomRow1Y - btnH - gap;
            String scenarioName = this.allScenarios != null && this.selectedScenarioIndex < this.allScenarios.size() ? this.allScenarios.get((int)this.selectedScenarioIndex).name : "?";
            elements.add(new ButtonElement(btnX, scenarioY, btnW, btnH, LanguageManager.format("mp_scenario", scenarioName), CFG.COLOR_PARCHMENT_BTN_BG, COLOR_TEXT_DARK, new Runnable(){

                @Override
                public void run() {
                    MultiplayerLobbyScreen.this.cycleScenario();
                }
            }));
            if (this.steamMode && SteamManager.isAvailable()) {
                int inviteY = scenarioY - btnH - gap;
                elements.add(new ButtonElement(btnX, inviteY, btnW, btnH, LanguageManager.get("mp_steam_invite"), new Color(0.1f, 0.35f, 0.6f, 1.0f), Color.WHITE, new Runnable(){

                    @Override
                    public void run() {
                        MultiplayerLobbyScreen.this.mpManager.openSteamInvite();
                    }
                }));
            }
        } else {
            int halfW = btnW / 2 - gap / 2;
            int roleY = bottomRow1Y - roleRowH - gap;

            Color playerBg = !this.isSpectator ? new Color(0.18f, 0.5f, 0.25f, 1.0f) : CFG.COLOR_TERTIARY_BTN;
            Color playerTxt = !this.isSpectator ? Color.WHITE : COLOR_TEXT_DIM;
            elements.add(new ButtonElement(btnX, roleY, halfW, roleRowH, "ИГРОК", playerBg, playerTxt, new Runnable() {
                @Override
                public void run() {
                    MultiplayerLobbyScreen.this.isSpectator = false;
                    cgs.modloader.spectator.SpectatorManager.getInstance().setSpectatorMode(false);
                    MultiplayerLobbyScreen.this.buildLobbyPanel();
                    MultiplayerLobbyScreen.this.setupInput();
                }
            }));

            Color specBg = this.isSpectator ? new Color(0.15f, 0.45f, 0.75f, 1.0f) : CFG.COLOR_TERTIARY_BTN;
            Color specTxt = this.isSpectator ? Color.WHITE : COLOR_TEXT_DIM;
            elements.add(new ButtonElement(btnX + halfW + gap, roleY, halfW, roleRowH, "ЗРИТЕЛЬ", specBg, specTxt, new Runnable() {
                @Override
                public void run() {
                    MultiplayerLobbyScreen.this.isSpectator = true;
                    cgs.modloader.spectator.SpectatorManager.getInstance().setSpectatorMode(true);
                    MultiplayerLobbyScreen.this.buildLobbyPanel();
                    MultiplayerLobbyScreen.this.setupInput();
                }
            }));

            String readyText = this.localReady ? "НЕ ГОТОВ" : "ГОТОВ";
            Color readyBg = this.localReady ? COLOR_BTN_NOT_READY : COLOR_BTN_READY;
            elements.add(new ButtonElement(btnX, bottomRow1Y, halfW, btnH, readyText, readyBg, Color.WHITE, new Runnable(){
                @Override
                public void run() {
                    MultiplayerLobbyScreen.this.localReady = !MultiplayerLobbyScreen.this.localReady;
                    if (MultiplayerLobbyScreen.this.mpManager != null) {
                        MultiplayerLobbyScreen.this.mpManager.setReady(MultiplayerLobbyScreen.this.localReady);
                    }
                    MultiplayerLobbyScreen.this.buildLobbyPanel();
                    MultiplayerLobbyScreen.this.setupInput();
                }
            }));
            elements.add(new ButtonElement(btnX + halfW + gap, bottomRow1Y, halfW, btnH, LanguageManager.get("mp_disconnect"), COLOR_BTN_BACK, Color.WHITE, new Runnable(){
                @Override
                public void run() {
                    MultiplayerLobbyScreen.this.onDisconnectClick();
                }
            }));
        }

        this.activePanel = new SliderMenu();
        this.activePanel.initMenu(null, 0, 0, W, H, elements, true, false);
        this.activePanel.setVisible(true);
    }

    private void cycleScenario() {
        if (this.allScenarios == null || this.allScenarios.isEmpty()) {
            return;
        }
        this.selectedScenarioIndex = (this.selectedScenarioIndex + 1) % this.allScenarios.size();
        ScenarioData selected = this.allScenarios.get(this.selectedScenarioIndex);
        this.loadCountryNames(selected.folderPath);
        if (this.mpManager != null && this.mpManager.isHost()) {
            NetworkProtocol.GameSettings settings = this.mpManager.getGameSettings();
            if (settings == null) {
                settings = new NetworkProtocol.GameSettings();
            }
            settings.scenarioPath = selected.folderPath;
            this.mpManager.updateGameSettings(settings);
        }
        this.buildLobbyPanel();
        this.setupInput();
    }

    private void loadCountryNames(String scenarioPath) {
        this.countryNameLookup.clear();
        scenarioPath = ScenarioFileResolver.localizeWorkshopPath(scenarioPath);
        try {
            FileHandle tagsFile;
            HashMap<String, String> tagToName = new HashMap<String, String>();
            String langSuffix = LanguageManager.getLangSuffix();
            String scenarioId = "";
            if (scenarioPath.endsWith("/")) {
                String trimmed = scenarioPath.substring(0, scenarioPath.length() - 1);
                scenarioId = trimmed.substring(trimmed.lastIndexOf(47) + 1);
            } else if (!scenarioPath.isEmpty()) {
                scenarioId = scenarioPath.substring(scenarioPath.lastIndexOf(47) + 1);
            }
            String[] potentialNames = new String[]{scenarioId.isEmpty() ? "" : scenarioId + "_country_names_" + langSuffix + ".txt", "country_names_" + langSuffix + ".txt", scenarioId.isEmpty() ? "" : scenarioId + "_country_names.txt", "country_names.txt"};
            FileHandle namesFile = null;
            for (String name : potentialNames) {
                if (name.isEmpty()) continue;
                String p1 = scenarioPath + name;
                String p2 = "assets/" + scenarioPath + name;
                String p3 = "map/" + scenarioPath + name;
                if (ScenarioFileResolver.resolve(p1).exists()) {
                    namesFile = ScenarioFileResolver.resolve(p1);
                    break;
                }
                if (ScenarioFileResolver.resolve(p2).exists()) {
                    namesFile = ScenarioFileResolver.resolve(p2);
                    break;
                }
                if (!ScenarioFileResolver.resolve(p3).exists()) continue;
                namesFile = ScenarioFileResolver.resolve(p3);
                break;
            }
            if (namesFile != null && namesFile.exists()) {
                String[] lines;
                String text = ScenarioFileResolver.readText(namesFile);
                for (String line : lines = text.split("\\r?\\n")) {
                    int eqIdx;
                    if ((line = line.trim()).isEmpty() || line.startsWith("#") || (eqIdx = line.indexOf(61)) <= 0) continue;
                    String tag = line.substring(0, eqIdx).trim();
                    String name = line.substring(eqIdx + 1).trim();
                    tagToName.put(tag, name);
                }
            }
            if ((tagsFile = ScenarioFileResolver.resolve(scenarioPath + "country_tags.txt")).exists()) {
                String[] lines;
                String text = ScenarioFileResolver.readText(tagsFile);
                for (String line : lines = text.split("\\r?\\n")) {
                    int eqIdx;
                    if ((line = line.trim()).isEmpty() || line.startsWith("//") || (eqIdx = line.indexOf(61)) <= 0) continue;
                    String hexPart = line.substring(0, eqIdx).trim().replace("#", "").replace("0x", "");
                    String tag = line.substring(eqIdx + 1).trim();
                    try {
                        int colorKey = Integer.parseInt(hexPart, 16);
                        String name = tagToName.getOrDefault(tag, tag);
                        this.countryNameLookup.put(colorKey, name);
                    }
                    catch (NumberFormatException numberFormatException) {
                        // empty catch block
                    }
                }
            }
            Gdx.app.log("LobbyScreen", "Loaded " + this.countryNameLookup.size() + " countries from " + scenarioPath);
            if (this.mpManager.isHost()) {
                if (this.mpManager.getServer() != null) {
                    this.mpManager.getServer().setCountryNameLookup(this.countryNameLookup);
                }
                if (this.mpManager.getSteamServer() != null) {
                    this.mpManager.getSteamServer().setCountryNameLookup(this.countryNameLookup);
                }
            }
        }
        catch (Exception e) {
            Gdx.app.log("LobbyScreen", "Error loading country names: " + e.getMessage());
        }
    }

    private void onHostClick() {
        this.mode = LobbyMode.HOSTING;
        this.statusText = LanguageManager.get("mp_status_server");
        SettingsManager.getInstance().setPlayerName(this.localPlayerName);
        String scenPath = this.allScenarios != null && this.selectedScenarioIndex < this.allScenarios.size() ? this.allScenarios.get((int)this.selectedScenarioIndex).folderPath : "scenarios/1936/";
        this.mpManager.setPlayerName(this.localPlayerName);
        this.mpManager.setUiListener(this.createLobbyListener());
        if (this.steamMode && SteamManager.isAvailable()) {
            try {
                SteamUser user = new SteamUser(new SteamUserCallback(){

                    @Override
                    public void onAuthSessionTicket(SteamAuthTicket authTicket, SteamResult result) {
                    }

                    @Override
                    public void onValidateAuthTicket(SteamID steamID, SteamAuth.AuthSessionResponse authSessionResponse, SteamID ownerSteamID) {
                    }

                    @Override
                    public void onMicroTxnAuthorization(int appID, long orderID, boolean authorized) {
                    }

                    @Override
                    public void onEncryptedAppTicket(SteamResult result) {
                    }
                });
                SteamID localId = user.getSteamID();
                user.dispose();
                this.mpManager.hostGameSteam(scenPath, this.localPlayerName, localId);
            }
            catch (Exception e) {
                Gdx.app.log("LobbyScreen", "Steam host failed: " + e.getMessage());
                this.steamMode = false;
                this.mpManager.hostGame(scenPath, this.localPlayerName);
            }
        } else {
            this.mpManager.hostGame(scenPath, this.localPlayerName);
        }
        this.loadCountryNames(scenPath);
        this.statusText = LanguageManager.get("mp_status_wait");
        this.mode = LobbyMode.IN_LOBBY;
        this.buildLobbyPanel();
        this.setupInput();
    }

    private void onJoinClick() {
        this.mode = LobbyMode.JOINING;
        this.statusText = "";
        this.selectedRoomAddress = null;
        this.selectedSteamLobby = null;
        SettingsManager.getInstance().setPlayerName(this.localPlayerName);
        this.mpManager.setPlayerName(this.localPlayerName);
        if (this.steamMode && SteamManager.isAvailable()) {
            this.startSteamDiscovery();
        } else {
            this.startScanning();
        }
    }

    private void startSteamDiscovery() {
        if (this.isSteamSearching) {
            return;
        }
        this.isSteamSearching = true;
        this.steamLobbies.clear();
        this.selectedSteamLobby = null;
        this.statusText = LanguageManager.get("mp_steam_searching");
        this.buildSteamJoinPanel();
        this.setupInput();
        this.mpManager.discoverSteamLobbies(new SteamLobbyManager.LobbyListener(){

            @Override
            public void onLobbyCreated(SteamID id, boolean ok) {
            }

            @Override
            public void onLobbyList(final List<SteamLobbyManager.SteamLobbyInfo> lobbies) {
                Gdx.app.postRunnable(new Runnable(){

                    @Override
                    public void run() {
                        MultiplayerLobbyScreen.this.isSteamSearching = false;
                        MultiplayerLobbyScreen.this.steamLobbies.clear();
                        MultiplayerLobbyScreen.this.steamLobbies.addAll(lobbies);
                        MultiplayerLobbyScreen.this.statusText = MultiplayerLobbyScreen.this.steamLobbies.isEmpty() ? LanguageManager.get("mp_steam_none") : "";
                        if (MultiplayerLobbyScreen.this.mode == LobbyMode.JOINING) {
                            MultiplayerLobbyScreen.this.buildSteamJoinPanel();
                            MultiplayerLobbyScreen.this.setupInput();
                        }
                    }
                });
            }

            @Override
            public void onLobbyJoined(SteamID id, SteamID host) {
            }

            @Override
            public void onLobbyJoinFailed(String reason) {
                Gdx.app.postRunnable(new Runnable(){

                    @Override
                    public void run() {
                        MultiplayerLobbyScreen.this.isSteamSearching = false;
                        MultiplayerLobbyScreen.this.statusText = LanguageManager.get("mp_steam_join_failed");
                    }
                });
            }

            @Override
            public void onLobbyMemberJoined(SteamID id) {
            }

            @Override
            public void onLobbyMemberLeft(SteamID id) {
            }
        });
    }

    private void buildSteamJoinPanel() {
        int W = CFG.GAME_WIDTH;
        int H = CFG.GAME_HEIGHT;
        int btnW = Math.min(CFG.s(360), W - CFG.s(80));
        int btnH = CFG.s(48);
        int gap = CFG.s(12);
        int pad = CFG.s(28);
        int roomAreaH = CFG.s(38) * 6 + CFG.s(44);
        this.panelW = btnW + pad * 2;
        this.panelH = roomAreaH + btnH + gap + btnH + gap + pad * 2;
        this.panelX = (W - this.panelW) / 2;
        this.panelY = (H - this.panelH) / 2;
        ArrayList<MenuElement> elements = new ArrayList<MenuElement>();
        int btnX = this.panelX + pad;
        int bottomRow2Y = this.panelY + this.panelH - pad - btnH;
        int bottomRow1Y = bottomRow2Y - btnH - gap;
        boolean hasSelection = this.selectedSteamLobby != null;
        Color joinBg = hasSelection ? COLOR_BTN_HOST : CFG.COLOR_TERTIARY_BTN;
        Color joinTxt = hasSelection ? Color.WHITE : COLOR_TEXT_DIM;
        elements.add(new ButtonElement(btnX, bottomRow1Y, btnW, btnH, LanguageManager.get("mp_join_selected"), joinBg, joinTxt, new Runnable(){

            @Override
            public void run() {
                if (MultiplayerLobbyScreen.this.selectedSteamLobby != null) {
                    MultiplayerLobbyScreen.this.onJoinSteamLobby(MultiplayerLobbyScreen.this.selectedSteamLobby);
                }
            }
        }));
        int halfW = btnW / 2 - gap / 2;
        Color refreshBg = this.isSteamSearching ? CFG.COLOR_TERTIARY_BTN : COLOR_BTN_JOIN;
        Color refreshTxt = this.isSteamSearching ? COLOR_TEXT_DIM : COLOR_TEXT_DARK;
        elements.add(new ButtonElement(btnX, bottomRow2Y, halfW, btnH, LanguageManager.get("mp_refresh"), refreshBg, refreshTxt, new Runnable(){

            @Override
            public void run() {
                if (!MultiplayerLobbyScreen.this.isSteamSearching) {
                    MultiplayerLobbyScreen.this.startSteamDiscovery();
                }
            }
        }));
        elements.add(new ButtonElement(btnX + halfW + gap, bottomRow2Y, halfW, btnH, LanguageManager.get("mp_back"), COLOR_BTN_BACK, Color.WHITE, new Runnable(){

            @Override
            public void run() {
                MultiplayerLobbyScreen.this.onBackFromJoin();
            }
        }));

        this.activePanel = new SliderMenu();
        this.activePanel.initMenu(null, 0, 0, W, H, elements, true, false);
        this.activePanel.setVisible(true);
    }

    private void onJoinSteamLobby(SteamLobbyManager.SteamLobbyInfo lobbyInfo) {
        this.statusText = LanguageManager.format("mp_steam_connecting", String.valueOf(lobbyInfo.hostName));
        this.mode = LobbyMode.IN_LOBBY;
        this.mpManager.setUiListener(this.createLobbyListener());
        this.mpManager.joinGameSteam(lobbyInfo, this.localPlayerName);
        this.buildConnectingPanel();
        this.setupInput();
    }

    public void joinLobbyDirectly(SteamID lobbyId) {
        this.steamMode = true;
        this.statusText = LanguageManager.get("mp_steam_connecting_invite");
        if (this.mpManager != null) {
            this.mpManager.shutdown();
        }
        try {
            this.mpManager = new MultiplayerManager();
        }
        catch (Throwable t) {
            Gdx.app.log("MP", "Failed to recreate MultiplayerManager for invite: " + t.getMessage());
            return;
        }
        SteamLobbyManager.SteamLobbyInfo dummy = new SteamLobbyManager.SteamLobbyInfo();
        dummy.lobbyId = lobbyId;
        dummy.hostName = "Friend";
        dummy.scenario = "Invited Scenario";
        dummy.playerCount = 1;
        dummy.maxPlayers = 8;
        this.onJoinSteamLobby(dummy);
    }

    private void startScanning() {
        if (this.isSearching) {
            return;
        }
        this.isSearching = true;
        this.statusText = LanguageManager.get("mp_searching");
        this.selectedRoomAddress = null;
        this.buildJoinPanel();
        this.setupInput();
        Thread scanThread = new Thread(new Runnable(){

            @Override
            public void run() {
                try {
                    MultiplayerLobbyScreen.this.mpManager.discoverHosts();
                    Gdx.app.postRunnable(new Runnable(){

                        @Override
                        public void run() {
                            MultiplayerLobbyScreen.this.isSearching = false;
                            MultiplayerLobbyScreen.this.statusText = "";
                            if (MultiplayerLobbyScreen.this.mode == LobbyMode.JOINING) {
                                MultiplayerLobbyScreen.this.buildJoinPanel();
                                MultiplayerLobbyScreen.this.setupInput();
                            }
                        }
                    });
                }
                catch (Exception e) {
                    try {
                        Gdx.app.log("MP", "Scan failed: " + e.getMessage());
                        Gdx.app.postRunnable(new Runnable() {
                        @Override
                        public void run() {
                            MultiplayerLobbyScreen.this.isSearching = false;
                            MultiplayerLobbyScreen.this.statusText = "";
                            if (MultiplayerLobbyScreen.this.mode == LobbyMode.JOINING) {
                                MultiplayerLobbyScreen.this.buildJoinPanel();
                                MultiplayerLobbyScreen.this.setupInput();
                            }
                        }
                    });
                    }
                    catch (Throwable throwable) {
                        Gdx.app.postRunnable(new Runnable() {
                        @Override
                        public void run() {
                            MultiplayerLobbyScreen.this.isSearching = false;
                            MultiplayerLobbyScreen.this.statusText = "";
                            if (MultiplayerLobbyScreen.this.mode == LobbyMode.JOINING) {
                                MultiplayerLobbyScreen.this.buildJoinPanel();
                                MultiplayerLobbyScreen.this.setupInput();
                            }
                        }
                    });
                        throw throwable;
                    }
                }
            }
        }, "LAN-Scan");
        scanThread.setDaemon(true);
        scanThread.start();
    }

    private void buildJoinPanel() {
        int W = CFG.GAME_WIDTH;
        int H = CFG.GAME_HEIGHT;
        int btnW = Math.min(CFG.s(360), W - CFG.s(80));
        int btnH = CFG.s(48);
        int gap = CFG.s(12);
        int pad = CFG.s(28);
        int roomAreaH = CFG.s(38) * 6 + CFG.s(44);
        this.panelW = btnW + pad * 2;
        this.panelH = roomAreaH + btnH + gap + btnH + gap + pad * 2;
        this.panelX = (W - this.panelW) / 2;
        this.panelY = (H - this.panelH) / 2;
        ArrayList<MenuElement> elements = new ArrayList<MenuElement>();
        int btnX = this.panelX + pad;
        int bottomRow2Y = this.panelY + this.panelH - pad - btnH;
        int bottomRow1Y = bottomRow2Y - btnH - gap;
        boolean hasSelection = this.selectedRoomAddress != null;
        Color joinBg = hasSelection ? COLOR_BTN_HOST : CFG.COLOR_TERTIARY_BTN;
        Color joinText = hasSelection ? Color.WHITE : COLOR_TEXT_DIM;
        elements.add(new ButtonElement(btnX, bottomRow1Y, btnW, btnH, LanguageManager.get("mp_join_room"), joinBg, joinText, new Runnable(){

            @Override
            public void run() {
                if (MultiplayerLobbyScreen.this.selectedRoomAddress != null) {
                    MultiplayerLobbyScreen.this.onJoinRoom(MultiplayerLobbyScreen.this.selectedRoomAddress);
                }
            }
        }));
        int halfW = btnW / 2 - gap / 2;
        Color refreshBg = this.isSearching ? CFG.COLOR_TERTIARY_BTN : COLOR_BTN_JOIN;
        Color refreshText = this.isSearching ? COLOR_TEXT_DIM : COLOR_TEXT_DARK;
        elements.add(new ButtonElement(btnX, bottomRow2Y, halfW, btnH, LanguageManager.get("mp_refresh"), refreshBg, refreshText, new Runnable(){

            @Override
            public void run() {
                if (!MultiplayerLobbyScreen.this.isSearching) {
                    MultiplayerLobbyScreen.this.startScanning();
                }
            }
        }));
        elements.add(new ButtonElement(btnX + halfW + gap, bottomRow2Y, halfW, btnH, LanguageManager.get("mp_back"), COLOR_BTN_BACK, Color.WHITE, new Runnable(){

            @Override
            public void run() {
                MultiplayerLobbyScreen.this.onBackFromJoin();
            }
        }));

        this.activePanel = new SliderMenu();
        this.activePanel.initMenu(null, 0, 0, W, H, elements, true, false);
        this.activePanel.setVisible(true);
    }

    private void onJoinRoom(InetAddress address) {
        if (address == null) {
            return;
        }
        String hostIp = address.getHostAddress();
        this.statusText = LanguageManager.format("mp_status_conn", hostIp);
        this.mpManager.setUiListener(this.createLobbyListener());
        this.mpManager.joinGame(hostIp, this.localPlayerName);
    }

    private void onBackFromJoin() {
        this.mpManager.shutdown();
        this.mode = LobbyMode.MENU;
        this.statusText = "";
        this.selectedRoomAddress = null;
        this.selectedSteamLobby = null;
        this.isSearching = false;
        this.isSteamSearching = false;
        this.steamLobbies.clear();
        this.buildMainMenu();
        this.setupInput();
    }

    private String getLocalizedScenarioName(String path) {
        int lastSlash;
        String trimmed;
        if (path == null) {
            return "?";
        }
        if (this.allScenarios != null) {
            for (ScenarioData sd : this.allScenarios) {
                if (!path.equals(sd.folderPath) && !path.replace("/", "").equals(sd.folderPath.replace("/", ""))) continue;
                return sd.name;
            }
        }
        if ((trimmed = path).endsWith("/")) {
            trimmed = trimmed.substring(0, trimmed.length() - 1);
        }
        if ((lastSlash = trimmed.lastIndexOf(47)) >= 0) {
            return trimmed.substring(lastSlash + 1);
        }
        return trimmed;
    }

    private void drawRoomBrowser(float alpha) {
        int pad = CFG.s(28);
        int headerH = CFG.s(34);
        int rowH = CFG.s(38);
        int rowGap = CFG.s(4);
        int x = this.panelX + pad;
        int rowW = this.panelW - pad * 2;
        this.batch.setColor(Color.WHITE);
        CFG.drawText(this.batch, LanguageManager.get("mp_lan_rooms"), x, this.panelY + pad + CFG.s(4), COLOR_TEXT_DARK);
        int headerSmallY = MultiplayerLobbyScreen.smallOnMain(this.panelY + pad + CFG.s(4));
        if (this.isSearching && CFG.fontSmall != null) {
            String searchMsg = LanguageManager.get("mp_searching");
            CFG.glyphLayout.setText(CFG.fontSmall, searchMsg);
            int tw = (int)CFG.glyphLayout.width;
            CFG.drawTextSmall(this.batch, searchMsg, x + rowW - tw, headerSmallY, COLOR_TEXT_DIM);
        }
        this.batch.setColor(CFG.COLOR_IRON_BORDER.r, CFG.COLOR_IRON_BORDER.g, CFG.COLOR_IRON_BORDER.b, 0.4f * alpha);
        ImageManager.getImage(Images.pix255_255_255).draw(this.batch, x, this.panelY + pad + headerH - CFG.s(6), rowW, 1);
        int startY = this.panelY + pad + headerH;
        Map<InetAddress, NetworkProtocol.DiscoveryResponse> rooms = this.mpManager.getDiscoveredRooms();
        if (rooms.isEmpty()) {
            if (!this.isSearching) {
                this.batch.setColor(Color.WHITE);
                CFG.drawTextCentered(this.batch, LanguageManager.get("mp_no_rooms_found"), this.panelX + this.panelW / 2, startY + CFG.s(40), COLOR_TEXT_DIM);
            }
            return;
        }
        int index = 0;
        for (Map.Entry<InetAddress, NetworkProtocol.DiscoveryResponse> entry : rooms.entrySet()) {
            InetAddress addr = entry.getKey();
            NetworkProtocol.DiscoveryResponse info = entry.getValue();
            int rowY = startY + index * (rowH + rowGap);
            boolean isSelected = addr.equals(this.selectedRoomAddress);
            if (isSelected) {
                this.batch.setColor(0.85f, 0.75f, 0.6f, 0.95f * alpha);
            } else {
                this.batch.setColor(MultiplayerLobbyScreen.COLOR_PLAYER_ROW.r, MultiplayerLobbyScreen.COLOR_PLAYER_ROW.g, MultiplayerLobbyScreen.COLOR_PLAYER_ROW.b, MultiplayerLobbyScreen.COLOR_PLAYER_ROW.a * alpha);
            }
            ImageManager.getImage(Images.pix255_255_255).draw(this.batch, x, rowY, rowW, rowH);
            if (isSelected) {
                this.batch.setColor(CFG.COLOR_RED_BTN.r, CFG.COLOR_RED_BTN.g, CFG.COLOR_RED_BTN.b, 0.9f * alpha);
            } else {
                this.batch.setColor(MultiplayerLobbyScreen.COLOR_COUNTRY_NAME.r, MultiplayerLobbyScreen.COLOR_COUNTRY_NAME.g, MultiplayerLobbyScreen.COLOR_COUNTRY_NAME.b, 0.8f * alpha);
            }
            ImageManager.getImage(Images.pix255_255_255).draw(this.batch, x, rowY, CFG.s(4), rowH);
            this.batch.setColor(Color.WHITE);
            int textY = MultiplayerLobbyScreen.roomTextTop(rowY, rowH);
            int textX = x + CFG.s(14);
            String hostLabel = info.hostName != null ? info.hostName : LanguageManager.get("mp_host_default");
            CFG.drawText(this.batch, hostLabel, textX, textY, COLOR_TEXT_DARK);
            if (CFG.fontSmall != null) {
                String scenarioLabel = this.getLocalizedScenarioName(info.scenarioName);
                CFG.drawTextSmall(this.batch, scenarioLabel, textX, textY + CFG.s(16), COLOR_TEXT_DIM);
                String playersCountStr = LanguageManager.format("mp_players_count", info.playerCount, info.maxPlayers);
                CFG.glyphLayout.setText(CFG.fontSmall, playersCountStr);
                int countW = (int)CFG.glyphLayout.width;
                CFG.drawTextSmall(this.batch, playersCountStr, x + rowW - countW - CFG.s(12), CFG.textTopCentered(CFG.fontSmall, rowY, rowH), COLOR_TEXT_DARK);
            }
            if (++index < 6) continue;
            break;
        }
        this.batch.setColor(Color.WHITE);
    }

    private void drawSteamLobbyBrowser(float alpha) {
        int pad = CFG.s(28);
        int headerH = CFG.s(34);
        int rowH = CFG.s(38);
        int rowGap = CFG.s(4);
        int x = this.panelX + pad;
        int rowW = this.panelW - pad * 2;
        Color steamBlue = new Color(0.15f, 0.45f, 0.8f, 1.0f);
        this.batch.setColor(Color.WHITE);
        CFG.drawText(this.batch, LanguageManager.get("mp_steam_lobbies_title"), x, this.panelY + pad + CFG.s(4), steamBlue);
        int headerSmallY = MultiplayerLobbyScreen.smallOnMain(this.panelY + pad + CFG.s(4));
        if (this.isSteamSearching && CFG.fontSmall != null) {
            String msg = LanguageManager.get("mp_searching_short");
            CFG.glyphLayout.setText(CFG.fontSmall, msg);
            int tw = (int)CFG.glyphLayout.width;
            CFG.drawTextSmall(this.batch, msg, x + rowW - tw, headerSmallY, COLOR_TEXT_DIM);
        }
        this.batch.setColor(0.15f, 0.45f, 0.8f, 0.4f * alpha);
        ImageManager.getImage(Images.pix255_255_255).draw(this.batch, x, this.panelY + pad + headerH - CFG.s(6), rowW, 1);
        int startY = this.panelY + pad + headerH;
        if (this.steamLobbies.isEmpty()) {
            if (!this.isSteamSearching) {
                this.batch.setColor(Color.WHITE);
                CFG.drawTextCentered(this.batch, LanguageManager.get("mp_steam_none"), this.panelX + this.panelW / 2, startY + CFG.s(40), COLOR_TEXT_DIM);
            }
            return;
        }
        int index = 0;
        for (SteamLobbyManager.SteamLobbyInfo info : this.steamLobbies) {
            boolean isSel;
            int rowY = startY + index * (rowH + rowGap);
            boolean bl = isSel = info == this.selectedSteamLobby;
            if (isSel) {
                this.batch.setColor(0.78f, 0.9f, 1.0f, 0.95f * alpha);
            } else {
                this.batch.setColor(MultiplayerLobbyScreen.COLOR_PLAYER_ROW.r, MultiplayerLobbyScreen.COLOR_PLAYER_ROW.g, MultiplayerLobbyScreen.COLOR_PLAYER_ROW.b, MultiplayerLobbyScreen.COLOR_PLAYER_ROW.a * alpha);
            }
            ImageManager.getImage(Images.pix255_255_255).draw(this.batch, x, rowY, rowW, rowH);
            if (isSel) {
                this.batch.setColor(0.1f, 0.4f, 0.8f, 0.9f * alpha);
            } else {
                this.batch.setColor(0.3f, 0.5f, 0.75f, 0.55f * alpha);
            }
            ImageManager.getImage(Images.pix255_255_255).draw(this.batch, x, rowY, CFG.s(4), rowH);
            this.batch.setColor(Color.WHITE);
            int textY = MultiplayerLobbyScreen.roomTextTop(rowY, rowH);
            int textX = x + CFG.s(14);
            String hostName = info.hostName != null ? info.hostName : LanguageManager.get("mp_host_default");
            CFG.drawText(this.batch, hostName, textX, textY, COLOR_TEXT_DARK);
            if (CFG.fontSmall != null) {
                String scen = this.getLocalizedScenarioName(info.scenario);
                CFG.drawTextSmall(this.batch, scen, textX, textY + CFG.s(16), COLOR_TEXT_DIM);
                String cnt = info.playerCount + "/" + info.maxPlayers + " players";
                CFG.glyphLayout.setText(CFG.fontSmall, cnt);
                int cntW = (int)CFG.glyphLayout.width;
                CFG.drawTextSmall(this.batch, cnt, x + rowW - cntW - CFG.s(12), CFG.textTopCentered(CFG.fontSmall, rowY, rowH), COLOR_TEXT_DARK);
            }
            if (++index < 6) continue;
            break;
        }
        this.batch.setColor(Color.WHITE);
    }

    private void onBackClick() {
        this.mpManager.shutdown();
        this.game.showMainMenu();
    }

    private void onStartClick() {
        if (this.mpManager.isHost()) {
            this.mpManager.startGame();
        }
    }

    private void onDisconnectClick() {
        this.mpManager.shutdown();
        this.mode = LobbyMode.MENU;
        this.playerList.clear();
        this.buildMainMenu();
        this.setupInput();
    }

    private void leaveForMissingScenario(String scenarioPath) {
        String name = ScenarioData.idFromPath(scenarioPath);
        boolean workshop = ScenarioFileResolver.isWorkshopPath(scenarioPath);
        this.onDisconnectClick();
        this.statusText = LanguageManager.format(workshop ? "mp_err_workshop_missing" : "mp_err_scenario_missing", name);
        if (workshop && SteamManager.isAvailable()) {
            String item = ScenarioFileResolver.workshopItemId(scenarioPath);
            SteamManager.activateGameOverlayToWebPage(SteamWorkshopManager.itemPageUrl(item != null ? item : name));
        }
    }

    private MultiplayerManager.MultiplayerListener createLobbyListener() {
        return new MultiplayerManager.MultiplayerListener(){

            @Override
            public void onConnected(int playerId) {
                MultiplayerLobbyScreen.this.statusText = LanguageManager.format("mp_status_connected", playerId);
                MultiplayerLobbyScreen.this.mode = LobbyMode.IN_LOBBY;
                String scenPath = MultiplayerLobbyScreen.this.mpManager.getScenarioPath();
                if (scenPath != null && !scenPath.isEmpty()) {
                    MultiplayerLobbyScreen.this.loadCountryNames(scenPath);
                }
                MultiplayerLobbyScreen.this.buildLobbyPanel();
                MultiplayerLobbyScreen.this.setupInput();
            }

            @Override
            public void onDisconnected() {
                MultiplayerLobbyScreen.this.statusText = LanguageManager.get("mp_status_disconnected");
                MultiplayerLobbyScreen.this.mode = LobbyMode.MENU;
                MultiplayerLobbyScreen.this.playerList.clear();
                MultiplayerLobbyScreen.this.buildMainMenu();
                MultiplayerLobbyScreen.this.setupInput();
            }

            @Override
            public void onLobbyUpdated(NetworkProtocol.LobbyState state) {
                MultiplayerLobbyScreen.this.playerList.clear();
                if (state.players != null) {
                    MultiplayerLobbyScreen.this.playerList.addAll(state.players);
                }
            }

            @Override
            public void onGameStarting(String scenarioPath, long seed, List<NetworkProtocol.PlayerInfo> players) {
                String localPath = ScenarioFileResolver.localizeWorkshopPath(scenarioPath);
                if (!ScenarioFileResolver.hasScenarioAt(localPath)) {
                    MultiplayerLobbyScreen.this.leaveForMissingScenario(localPath);
                    return;
                }
                MultiplayerLobbyScreen.this.statusText = LanguageManager.get("mp_status_started");
                MultiplayerLobbyScreen.this.game.startMultiplayerGame(MultiplayerLobbyScreen.this.mpManager, localPath, seed);
            }

            @Override
            public void onPlayerDisconnected(String playerName) {
                MultiplayerLobbyScreen.this.statusText = LanguageManager.format("mp_status_player_left", playerName);
            }

            @Override
            public void onCountrySelectRejected(int colorKey, String reason) {
                MultiplayerLobbyScreen.this.statusText = LanguageManager.format("mp_status_rejected", reason);
            }

            @Override
            public void onHostDisconnectedDuringGame() {
                Gdx.app.log("MP", "Host disconnected during active game session");
                MultiplayerLobbyScreen.this.statusText = LanguageManager.get("mp_status_disconnected");
                MapScreen ms = MapScreen.getActiveInstance();
                if (ms != null) {
                    ms.showToast(LanguageManager.get("mp_status_disconnected"));
                }
            }
        };
    }

    private void setupInput() {
        Gdx.input.setInputProcessor(new InputAdapter(){

            @Override
            public boolean touchDown(int screenX, int screenY, int pointer, int button) {
                int pad;
                if (MultiplayerLobbyScreen.this.mode == LobbyMode.MENU) {
                    pad = CFG.s(32);
                    int fieldX = MultiplayerLobbyScreen.this.panelX + pad;
                    int fieldY = MultiplayerLobbyScreen.this.nameFieldY + CFG.s(10);
                    int fieldW = MultiplayerLobbyScreen.this.panelW - pad * 2;
                    int fieldH = CFG.s(34);
                    if (screenX >= fieldX && screenX <= fieldX + fieldW && screenY >= fieldY && screenY <= fieldY + fieldH) {
                        MultiplayerLobbyScreen.this.textInputDialog.show(LanguageManager.get("mp_your_name"), MultiplayerLobbyScreen.this.localPlayerName, "", new CGSTextInputDialog.OnInputResult(){

                            @Override
                            public void onResult(String text) {
                                MultiplayerLobbyScreen.this.setLocalPlayerName(text);
                            }

                            @Override
                            public void onCancel() {
                            }
                        });
                        return true;
                    }
                }
                if (MultiplayerLobbyScreen.this.mode == LobbyMode.JOINING) {
                    pad = CFG.s(28);
                    int headerH = CFG.s(34);
                    int rowH = CFG.s(38);
                    int rowGap = CFG.s(4);
                    int x = MultiplayerLobbyScreen.this.panelX + pad;
                    int rowW = MultiplayerLobbyScreen.this.panelW - pad * 2;
                    int startY = MultiplayerLobbyScreen.this.panelY + pad + headerH;
                    Map<InetAddress, NetworkProtocol.DiscoveryResponse> rooms = MultiplayerLobbyScreen.this.mpManager.getDiscoveredRooms();
                    if (MultiplayerLobbyScreen.this.steamMode && SteamManager.isAvailable()) {
                        int idx = 0;
                        for (SteamLobbyManager.SteamLobbyInfo info : MultiplayerLobbyScreen.this.steamLobbies) {
                            int rowY = startY + idx * (rowH + rowGap);
                            if (screenX >= x && screenX <= x + rowW && screenY >= rowY && screenY <= rowY + rowH) {
                                MultiplayerLobbyScreen.this.selectedSteamLobby = info;
                                MultiplayerLobbyScreen.this.buildSteamJoinPanel();
                                MultiplayerLobbyScreen.this.setupInput();
                                return true;
                            }
                            if (++idx < 6) continue;
                            break;
                        }
                    } else if (!rooms.isEmpty()) {
                        int index = 0;
                        for (Map.Entry<InetAddress, NetworkProtocol.DiscoveryResponse> entry : rooms.entrySet()) {
                            int rowY = startY + index * (rowH + rowGap);
                            if (screenX >= x && screenX <= x + rowW && screenY >= rowY && screenY <= rowY + rowH) {
                                MultiplayerLobbyScreen.this.selectedRoomAddress = entry.getKey();
                                MultiplayerLobbyScreen.this.buildJoinPanel();
                                MultiplayerLobbyScreen.this.setupInput();
                                return true;
                            }
                            if (++index < 6) continue;
                            break;
                        }
                    }
                }
                if (MultiplayerLobbyScreen.this.activePanel != null && MultiplayerLobbyScreen.this.activePanel.getVisible()) {
                    return MultiplayerLobbyScreen.this.activePanel.actionPressDown(screenX, screenY);
                }
                return false;
            }

            @Override
            public boolean touchUp(int screenX, int screenY, int pointer, int button) {
                if (MultiplayerLobbyScreen.this.activePanel != null && MultiplayerLobbyScreen.this.activePanel.getVisible()) {
                    return MultiplayerLobbyScreen.this.activePanel.actionPressUp(screenX, screenY);
                }
                return false;
            }

            @Override
            public boolean mouseMoved(int screenX, int screenY) {
                if (MultiplayerLobbyScreen.this.activePanel != null && MultiplayerLobbyScreen.this.activePanel.getMenuElements() != null) {
                    for (MenuElement element : MultiplayerLobbyScreen.this.activePanel.getMenuElements()) {
                        if (element == null || !element.getVisible()) continue;
                        boolean isInside = screenX >= element.getPosX() && screenX < element.getPosX() + element.getWidth() && screenY >= element.getPosY() && screenY < element.getPosY() + element.getHeight();
                        element.setIsHovered(isInside);
                    }
                }
                return false;
            }
        });
    }

    @Override
    public void show() {
        this.mode = LobbyMode.MENU;
        this.playerList.clear();
        this.localPlayerName = SettingsManager.getInstance().getPlayerName();
        if (this.localPlayerName == null || this.localPlayerName.isEmpty()) {
            this.localPlayerName = "Player";
        }
        this.buildMainMenu();
        this.setupInput();
        this.fadeInTimer = 0.0f;
    }

    @Override
    public void render(float delta) {
        if (this.textInputDialog != null) {
            this.textInputDialog.update(delta);
        }
        if (!this.mpManager.isGameStarted()) {
            this.mpManager.update(delta);
        }
        if (this.activePanel != null) {
            this.activePanel.updateElements(delta);
        }
        this.fadeInTimer = Math.min(this.fadeInTimer + delta, 0.6f);
        float alpha = this.fadeInTimer / 0.6f;
        BackgroundManager.getInstance().update(delta);
        this.bgTimer += delta;
        int W = CFG.GAME_WIDTH;
        int H = CFG.GAME_HEIGHT;
        for (Particle p : this.particles) {
            p.update(delta, W, H);
        }
        ScreenUtils.clear(COLOR_BG);
        this.batch.setProjectionMatrix(this.uiProjection);
        this.batch.begin();
        BackgroundManager bgm = BackgroundManager.getInstance();
        bgm.drawSideBackdrop(this.batch, alpha, 0.0f, 0.0f, 0.0f, 0.0f);
        float bgDim = bgm.isAtlas() ? 0.14f : 1.0f;
        this.batch.setColor(0.0f, 0.0f, 0.02f, 0.7f * bgDim * alpha);
        ImageManager.getImage(Images.gradient_vertical_full).draw(this.batch, 0, H / 3, W, H * 2 / 3);
        this.batch.setColor(0.0f, 0.0f, 0.03f, 0.3f * alpha);
        ImageManager.getImage(Images.gradientVertical).draw(this.batch, 0, 0, W, H / 3);
        this.batch.setColor(0.0f, 0.0f, 0.0f, 0.2f * alpha);
        ImageManager.getImage(Images.pix255_255_255).draw(this.batch, 0, 0, W, H);
        for (Particle p : this.particles) {
            this.batch.setColor(p.color.r, p.color.g, p.color.b, p.alpha * alpha);
            int px = (int)(p.x - p.size / 2.0f);
            int py = (int)(p.y - p.size / 2.0f);
            int ps = Math.max(2, (int)p.size);
            ImageManager.getImage(Images.obj_dot).draw(this.batch, px, py, ps, ps);
        }
        this.batch.setColor(Color.WHITE);
        if (CFG.fontLarge != null) {
            CFG.drawTextCenteredLarge(this.batch, LanguageManager.get("mp_title"), W / 2, CFG.s(40), Color.WHITE);
        } else {
            CFG.drawTextCentered(this.batch, LanguageManager.get("mp_title"), W / 2, CFG.s(40), Color.WHITE);
        }
        CFG.drawIronMenuPanel(this.batch, this.panelX, this.panelY, this.panelW, this.panelH);
        if (this.mode == LobbyMode.MENU) {
            this.drawPlayerNameField(alpha);
        }
        if (this.mode == LobbyMode.IN_LOBBY) {
            this.drawPlayerList(alpha);
        }
        if (this.mode == LobbyMode.JOINING) {
            if (this.steamMode && SteamManager.isAvailable()) {
                this.drawSteamLobbyBrowser(alpha);
            } else {
                this.drawRoomBrowser(alpha);
            }
        }
        this.batch.setColor(1.0f, 1.0f, 1.0f, alpha);
        if (this.activePanel != null && this.activePanel.getVisible()) {
            for (MenuElement el : this.activePanel.getMenuElements()) {
                if (el == null || !el.getVisible()) continue;
                el.draw(this.batch, 0, 0);
            }
        }
        if (!this.statusText.isEmpty()) {
            CFG.drawTextCentered(this.batch, this.statusText, W / 2, H - CFG.s(30), COLOR_TEXT_DIM);
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
            CFG.drawTextSmall(this.batch, "FPS: " + fps, W - 70, 14, this.tmpFpsColor);
        }
        if (this.textInputDialog != null && this.textInputDialog.isVisible()) {
            this.textInputDialog.draw(this.batch);
        }
        this.batch.setColor(Color.WHITE);
        this.batch.end();
    }

    private void drawBgCover(Image bgImage, float alpha) {
        if (bgImage == null) {
            return;
        }
        int W = CFG.GAME_WIDTH;
        int H = CFG.GAME_HEIGHT;
        this.batch.setColor(1.0f, 1.0f, 1.0f, alpha);
        bgImage.draw(this.batch, 0, 0, W, H);
    }

    private void drawPlayerNameField(float alpha) {
        int pad = CFG.s(32);
        int x = this.panelX + pad;
        int y = this.nameFieldY;
        int fieldW = this.panelW - pad * 2;
        int fieldH = CFG.s(34);
        this.batch.setColor(Color.WHITE);
        if (CFG.fontSmall != null) {
            CFG.drawTextSmall(this.batch, LanguageManager.get("mp_your_name"), x, y - CFG.s(10), COLOR_TEXT_DIM);
        }
        this.batch.setColor(0.9f, 0.87f, 0.8f, 0.85f * alpha);
        ImageManager.getImage(Images.pix255_255_255).draw(this.batch, x, y + CFG.s(10), fieldW, fieldH);
        this.batch.setColor(CFG.COLOR_IRON_BORDER.r, CFG.COLOR_IRON_BORDER.g, CFG.COLOR_IRON_BORDER.b, 0.6f * alpha);
        ImageManager.getImage(Images.pix255_255_255).draw(this.batch, x, y + CFG.s(10), fieldW, 1);
        ImageManager.getImage(Images.pix255_255_255).draw(this.batch, x, y + CFG.s(10) + fieldH - 1, fieldW, 1);
        ImageManager.getImage(Images.pix255_255_255).draw(this.batch, x, y + CFG.s(10), 1, fieldH);
        ImageManager.getImage(Images.pix255_255_255).draw(this.batch, x + fieldW - 1, y + CFG.s(10), 1, fieldH);
        this.batch.setColor(Color.WHITE);
        CFG.drawText(this.batch, this.localPlayerName, x + CFG.s(8), CFG.textTopCentered(CFG.fontMain, y + CFG.s(10), fieldH), COLOR_TEXT_DARK);
    }

    private static int smallOnMain(int mainY) {
        if (CFG.fontMain == null || CFG.fontSmall == null) {
            return mainY;
        }
        return mainY + Math.round(CFG.fontMain.getCapHeight() - CFG.fontSmall.getCapHeight());
    }

    private static int roomTextTop(int rowY, int rowH) {
        float cap = CFG.fontSmall != null ? CFG.fontSmall.getCapHeight() : (float)CFG.s(8);
        return rowY + Math.round(((float)rowH - ((float)CFG.s(16) + cap)) / 2.0f);
    }

    private void drawPlayerList(float alpha) {
        int pad = CFG.s(28);
        int headerH = CFG.s(34);
        int rowH = CFG.s(34);
        int rowGap = CFG.s(4);
        int x = this.panelX + pad;
        int rowW = this.panelW - pad * 2;
        this.batch.setColor(Color.WHITE);
        String headerText = LanguageManager.format("mp_players", this.playerList.size());
        CFG.drawText(this.batch, headerText, x, this.panelY + pad + CFG.s(4), COLOR_TEXT_DARK);
        int headerSmallY = MultiplayerLobbyScreen.smallOnMain(this.panelY + pad + CFG.s(4));
        if (CFG.fontSmall != null) {
            CFG.glyphLayout.setText(CFG.fontMain, headerText);
            int headerW = (int)CFG.glyphLayout.width;
            String modeStr = this.steamMode ? "Steam P2P" : "LAN";
            Color modeColor = this.steamMode ? new Color(0.25f, 0.55f, 0.9f, 1.0f) : new Color(0.6f, 0.55f, 0.5f, 1.0f);
            CFG.drawTextSmall(this.batch, " \u2014 " + (String)modeStr, x + headerW, headerSmallY, modeColor);
        }
        boolean allReady = !this.playerList.isEmpty();
        for (NetworkProtocol.PlayerInfo pi : this.playerList) {
            if (pi.ready) continue;
            allReady = false;
            break;
        }
        if (allReady && this.playerList.size() > 0 && CFG.fontSmall != null) {
            String readyMsg = LanguageManager.get("mp_all_ready");
            CFG.glyphLayout.setText(CFG.fontSmall, readyMsg);
            int tw = (int)CFG.glyphLayout.width;
            CFG.drawTextSmall(this.batch, readyMsg, x + rowW - tw, headerSmallY, COLOR_READY_YES);
        }
        this.batch.setColor(CFG.COLOR_IRON_BORDER.r, CFG.COLOR_IRON_BORDER.g, CFG.COLOR_IRON_BORDER.b, 0.4f * alpha);
        ImageManager.getImage(Images.pix255_255_255).draw(this.batch, x, this.panelY + pad + headerH - CFG.s(6), rowW, 1);
        int y = this.panelY + pad + headerH;
        if (this.playerList.isEmpty() && !this.mpManager.isHost() && CFG.fontSmall != null) {
            int dots = (int)(this.bgTimer * 2.0f) % 4;
            String waitMsg = this.statusText.isEmpty() ? LanguageManager.get("mp_connecting") : this.statusText;
            StringBuilder sb = new StringBuilder(waitMsg);
            for (int d = 0; d < dots; ++d) {
                sb.append('.');
            }
            CFG.drawTextSmall(this.batch, sb.toString(), x, y + CFG.s(10), COLOR_TEXT_DIM);
        }
        for (int i = 0; i < this.playerList.size(); ++i) {
            NetworkProtocol.PlayerInfo pi = this.playerList.get(i);
            int rowY = y + i * (rowH + rowGap);
            this.batch.setColor(MultiplayerLobbyScreen.COLOR_PLAYER_ROW.r, MultiplayerLobbyScreen.COLOR_PLAYER_ROW.g, MultiplayerLobbyScreen.COLOR_PLAYER_ROW.b, MultiplayerLobbyScreen.COLOR_PLAYER_ROW.a * alpha);
            ImageManager.getImage(Images.pix255_255_255).draw(this.batch, x, rowY, rowW, rowH);
            float hue = (float)pi.playerId * 0.3f % 1.0f;
            this.batch.setColor(0.6f + hue * 0.4f, 0.2f + (1.0f - hue) * 0.3f, 0.3f, 0.8f * alpha);
            ImageManager.getImage(Images.pix255_255_255).draw(this.batch, x, rowY, CFG.s(4), rowH);
            this.batch.setColor(Color.WHITE);
            int textY = CFG.textTopCentered(CFG.fontMain, rowY, rowH);
            int textX = x + CFG.s(14);
            CFG.drawText(this.batch, pi.playerName, textX, textY, COLOR_TEXT_DARK);
            if (CFG.fontSmall != null) {
                String nameMeasure = pi.playerName;
                CFG.glyphLayout.setText(CFG.fontMain, nameMeasure);
                int nameW = (int)CFG.glyphLayout.width;
                if (pi.isHost) {
                    CFG.drawTextSmall(this.batch, LanguageManager.get("mp_host_label"), textX + nameW + CFG.s(8), MultiplayerLobbyScreen.smallOnMain(textY), COLOR_HOST_BADGE);
                }
                if (pi.selectedColorKey >= cgs.modloader.spectator.SpectatorManager.SPECTATOR_KEY_BASE || (this.mpManager != null && pi.playerId == this.mpManager.getLocalPlayerId() && this.isSpectator)) {
                    int offset = pi.isHost ? CFG.s(55) : CFG.s(8);
                    CFG.drawTextSmall(this.batch, "[ЗРИТЕЛЬ]", textX + nameW + offset, MultiplayerLobbyScreen.smallOnMain(textY), new Color(0.15f, 0.65f, 0.95f, 1.0f));
                }
                String readyBadge = pi.ready ? "[ГОТОВ]" : "[ОЖИДАНИЕ]";
                Color badgeCol = pi.ready ? COLOR_READY_YES : new Color(0.85f, 0.45f, 0.2f, 1.0f);
                CFG.glyphLayout.setText(CFG.fontSmall, readyBadge);
                int bw = (int)CFG.glyphLayout.width;
                CFG.drawTextSmall(this.batch, readyBadge, x + rowW - bw - CFG.s(10), MultiplayerLobbyScreen.smallOnMain(textY), badgeCol);
            }
        }
        this.batch.setColor(Color.WHITE);
    }

    public MultiplayerManager getMultiplayerManager() {
        return this.mpManager;
    }

    public void setLocalPlayerName(String name) {
        if (name != null && !name.trim().isEmpty()) {
            this.localPlayerName = name.trim();
            SettingsManager.getInstance().setPlayerName(this.localPlayerName);
        }
    }

    @Override
    public void resize(int width, int height) {
        CFG.GAME_WIDTH = width;
        CFG.GAME_HEIGHT = height;
        this.updateUIProjection();
        if (this.mode == LobbyMode.IN_LOBBY) {
            this.buildLobbyPanel();
        } else if (this.mode == LobbyMode.JOINING) {
            this.buildJoinPanel();
        } else {
            this.buildMainMenu();
        }
    }

    @Override
    public void dispose() {
        if (this.mpManager != null) {
            this.mpManager.shutdown();
        }
    }

    private static enum LobbyMode {
        MENU,
        HOSTING,
        JOINING,
        IN_LOBBY;

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
            this.color = Math.random() < 0.3 ? new Color(0.4f, 0.55f, 1.0f, 1.0f) : new Color(0.8f, 0.82f, 1.0f, 1.0f);
        }

        void update(float delta, int screenW, int screenH) {
            this.time += delta;
            this.x += this.vx + (float)Math.sin((double)this.time * 0.5) * 0.15f;
            this.y += this.vy;
            this.alpha += (float)Math.sin(this.time * this.alphaSpeed) * 0.002f;
            this.alpha = Math.max(0.05f, Math.min(0.6f, this.alpha));
            if (this.y < -40.0f || this.x < -40.0f || this.x > (float)(screenW + 40)) {
                this.reset(screenW, screenH, false);
                this.y = (float)screenH + (float)(Math.random() * 20.0);
            }
        }
    }
}

