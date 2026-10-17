package cgs.mods.multiplayer;

import cgs.modloader.api.IMod;
import cgs.modloader.api.ModContext;
import cgs.modloader.api.ModMenuHelper;
import ru.ius.david.cgs.CFG;
import ru.ius.david.cgs.MainMenuScreen;
import ru.ius.david.cgs.cgsGame;
import ru.ius.david.cgs.game.GameManager;
import ru.ius.david.cgs.game.LanguageManager;
import ru.ius.david.cgs.images.Images;
import ru.ius.david.cgs.multiplayer.MultiplayerManager;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.Gdx;

public class MultiplayerMod implements IMod {
    private ModContext context;
    private cgsGame game;
    private float pulseTimer = 0.0f;
    private final Color badgeColor = new Color(0.12f, 0.42f, 0.72f, 1.0f);
    private final Color hudBgColor = new Color(0.04f, 0.06f, 0.12f, 0.85f);
    private final Color hudBorderColor = new Color(0.2f, 0.6f, 0.95f, 0.9f);

    @Override
    public void onInitialize(ModContext context) {
        this.context = context;
        context.log("Multiplayer Mod v1.2.0 initialized! Ready for high-performance LAN, Direct IP and Steam battles.");
    }

    @Override
    public void onGameCreated(cgsGame game) {
        this.game = game;
    }

    @Override
    public void onMainMenuBuild(final MainMenuScreen screen, ModMenuHelper helper) {
        boolean isRus = LanguageManager.getLangSuffix().equalsIgnoreCase("Rus");
        String label = LanguageManager.get("menu_multiplayer");
        if (label == null || label.trim().isEmpty()) {
            label = isRus ? "СЕТЕВАЯ ИГРА" : "MULTIPLAYER";
        }
        label = label.toUpperCase();

        String tooltip = isRus ? "Сетевые баталии: игра по сети до 8 игроков (LAN, Direct IP, Steam)"
                               : "Multiplayer battles: up to 8 players online (LAN, Direct IP, Steam)";

        // Styled cyber-blue button matching Age of Clicks aesthetic
        Color btnBg = new Color(0.12f, 0.38f, 0.65f, 1.0f);
        Color btnText = Color.WHITE;

        helper.addMenuButton(
            label,
            Images.ic_menu_multiplayer,
            btnBg,
            btnText,
            new Runnable() {
                @Override
                public void run() {
                    cgsGame g = screen.getGame();
                    if (g != null) {
                        g.showMultiplayerLobby();
                    }
                }
            },
            tooltip
        );
    }

    @Override
    public void onRenderOverlay(SpriteBatch batch, float delta) {
        // Live chat, tactical pings, scoreboard, HUD, pause, diplomacy during gameplay
        com.badlogic.gdx.Screen curScreen = game != null ? game.getScreen() : null;
        if (curScreen instanceof ru.ius.david.cgs.MapScreen) {
            ru.ius.david.cgs.MapScreen ms = (ru.ius.david.cgs.MapScreen) curScreen;
            if (ms.isCountrySelectMode()) {
                cgs.mods.multiplayer.chat.MultiplayerChat.getInstance().render(batch, delta, game);
                return;
            }
            cgs.mods.multiplayer.chat.MultiplayerChat.getInstance().render(batch, delta, game);
            cgs.mods.multiplayer.hud.TacticalHud.getInstance().render(batch, delta, CFG.GAME_WIDTH, CFG.GAME_HEIGHT, game);
            cgs.mods.multiplayer.pause.TacticalPauseManager.getInstance().renderBanner(batch, delta, CFG.GAME_WIDTH, CFG.GAME_HEIGHT, game);
            cgs.mods.multiplayer.diplomacy.MultiplayerDiplomacyDialog.getInstance().render(batch, CFG.fontMain, CFG.GAME_WIDTH, CFG.GAME_HEIGHT, GameManager.getInstance(), game);
            cgs.modloader.trade.DirectAidDialog.getInstance().render(batch, null, CFG.fontMain, CFG.GAME_WIDTH, CFG.GAME_HEIGHT, GameManager.getInstance());
        }

        GameManager gm = GameManager.getInstance();
        if (gm == null || !gm.isNetworkMode()) {
            return;
        }

        pulseTimer += delta;
        float pulse = 0.75f + (float)Math.sin(pulseTimer * 3.0f) * 0.25f;

        // Render sleek in-game Multiplayer HUD badge in top right corner
        if (CFG.fontSmall != null && batch.isDrawing()) {
            MultiplayerManager mpMgr = game != null ? game.getActiveMultiplayerManager() : null;
            int ping = (mpMgr != null) ? (int)mpMgr.getDisplayPingMs() : 0;
            String pingStr = ping > 0 ? (ping + " ms") : "OK";
            boolean isRus = LanguageManager.getLangSuffix().equalsIgnoreCase("Rus");
            String roleStr = gm.isHostInMultiplayer() ? (isRus ? "ХОСТ" : "HOST") : (isRus ? "КЛИЕНТ" : "CLIENT");
            String text = "🌐 " + (isRus ? "СЕТЬ: " : "NET: ") + roleStr + (isRus ? " | ПИНГ: " : " | PING: ") + pingStr;

            int textW = (int)CFG.glyphLayout.width;
            CFG.glyphLayout.setText(CFG.fontSmall, text);
            textW = (int)CFG.glyphLayout.width;

            int pad = CFG.s(8);
            int boxW = textW + pad * 2 + CFG.s(16);
            int boxH = CFG.s(24);
            int x = CFG.GAME_WIDTH - boxW - CFG.s(12);
            int y = CFG.s(8);

            // Draw status indicator
            Color dotColor = (ping < 100) ? new Color(0.2f, 0.9f, 0.3f, pulse) :
                             (ping < 250) ? new Color(0.95f, 0.8f, 0.2f, pulse) :
                                            new Color(0.95f, 0.25f, 0.2f, pulse);

            CFG.drawTextSmall(batch, text, x + pad + CFG.s(12), y + CFG.s(16), Color.WHITE);
        }
    }

    @Override
    public void onGameTick(GameManager gm) {
        cgs.mods.multiplayer.pause.TacticalPauseManager.getInstance().update(Gdx.graphics.getDeltaTime(), gm);
    }
}
