package cgs.modloader.spectator;

import ru.ius.david.cgs.CFG;
import ru.ius.david.cgs.images.Images;
import ru.ius.david.cgs.images.ImageManager;
import ru.ius.david.cgs.game.LanguageManager;
import ru.ius.david.cgs.MapModeManager;
import ru.ius.david.cgs.MapScreen;
import ru.ius.david.cgs.cgsGame;
import ru.ius.david.cgs.game.GameManager;
import ru.ius.david.cgs.menus.CGSCountrySelectMenu;
import ru.ius.david.cgs.multiplayer.MultiplayerManager;
import ru.ius.david.cgs.multiplayer.NetworkProtocol;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;

import java.lang.reflect.Field;
import java.util.List;

public class SpectatorManager {
    private static final SpectatorManager INSTANCE = new SpectatorManager();
    public static SpectatorManager getInstance() { return INSTANCE; }

    public static final int SPECTATOR_KEY_BASE = 0x70000000;

    private boolean spectatorMode = false;
    private float pulseTimer = 0.0f;
    private boolean roundStarted = false;
    private MapScreen lastMapScreen = null;

    private Field fCountrySelectMode;
    private Field fObserverMode;
    private Field fCountrySelectMenu;
    private Field fPlayerColorKey;
    private Field fSelectedCountryColorKey;
    private Field fMenuObserverMode;

    private SpectatorManager() {
        try {
            fCountrySelectMode = MapScreen.class.getDeclaredField("countrySelectMode");
            fCountrySelectMode.setAccessible(true);
            fObserverMode = MapScreen.class.getDeclaredField("observerMode");
            fObserverMode.setAccessible(true);
            fCountrySelectMenu = MapScreen.class.getDeclaredField("countrySelectMenu");
            fCountrySelectMenu.setAccessible(true);
            fPlayerColorKey = MapScreen.class.getDeclaredField("playerColorKey");
            fPlayerColorKey.setAccessible(true);
            fSelectedCountryColorKey = MapScreen.class.getDeclaredField("selectedCountryColorKey");
            fSelectedCountryColorKey.setAccessible(true);
            
            fMenuObserverMode = CGSCountrySelectMenu.class.getDeclaredField("observerMode");
            fMenuObserverMode.setAccessible(true);
        } catch (Throwable t) {
            System.err.println("[SpectatorManager] Reflection setup error: " + t.getMessage());
        }
    }

    public void setSpectatorMode(boolean active) {
        this.spectatorMode = active;
        this.roundStarted = false;
        System.out.println("[SpectatorManager] Spectator mode set to: " + active);
    }

    public boolean isSpectatorMode() {
        return this.spectatorMode;
    }

    public void onMapScreenFrame(MapScreen ms, cgsGame game, SpriteBatch batch, float delta) {
        if (!this.spectatorMode) {
            return;
        }

        if (this.lastMapScreen != ms) {
            this.lastMapScreen = ms;
            this.roundStarted = false;
            System.out.println("[SpectatorManager] Initializing spectator for MapScreen session");
        }

        this.pulseTimer += delta;

        // If the round has already started and MapScreen is running normally
        if (this.roundStarted) {
            return;
        }

        CGSCountrySelectMenu menu = null;
        try {
            if (fCountrySelectMenu != null) {
                menu = (CGSCountrySelectMenu) fCountrySelectMenu.get(ms);
                if (menu != null) {
                    if (menu.getVisible()) {
                        menu.setVisible(false);
                    }
                    if (fMenuObserverMode != null) {
                        fMenuObserverMode.setBoolean(menu, true);
                    }
                }
            }
            if (fObserverMode != null) {
                fObserverMode.setBoolean(ms, true);
            }
            if (fPlayerColorKey != null) {
                fPlayerColorKey.setInt(ms, -1);
            }
            if (fSelectedCountryColorKey != null) {
                fSelectedCountryColorKey.setInt(ms, -1);
            }
        } catch (Throwable ignored) {}

        // Ensure engine observer mode is active
        GameManager.getInstance().setObserverMode(true);

        MultiplayerManager mpMgr = game.getActiveMultiplayerManager();
        if (mpMgr != null && mpMgr.isMultiplayer()) {
            int dummyKey = SPECTATOR_KEY_BASE + Math.max(1, mpMgr.getLocalPlayerId());
            mpMgr.selectCountry(dummyKey);
            mpMgr.setReady(true);

            // Check if all combatants (non-spectators) have chosen a country and are ready
            NetworkProtocol.LobbyState state = mpMgr.getLatestLobbyState();
            if (state != null && state.players != null && !state.players.isEmpty()) {
                boolean allCombatantsReady = true;
                int combatantCount = 0;
                for (NetworkProtocol.PlayerInfo pi : state.players) {
                    if (pi.playerId == mpMgr.getLocalPlayerId()) continue;
                    if (pi.selectedColorKey >= SPECTATOR_KEY_BASE) continue; // spectator

                    combatantCount++;
                    if (!pi.ready || pi.selectedColorKey == -1) {
                        allCombatantsReady = false;
                        break;
                    }
                }

                // If all combatants are ready, start the round!
                if (allCombatantsReady && combatantCount > 0) {
                    startObserverMatch(ms, menu);
                    return;
                }
            }
        }

        // 3. Draw sleek spectator waiting banner on screen while waiting
        if (ms.isCountrySelectMode()) {
            drawSpectatorWaitingBanner(batch);
        }
    }

    public void startObserverMatch(MapScreen ms, CGSCountrySelectMenu menu) {
        if (this.roundStarted) return;
        this.roundStarted = true;
        System.out.println("[SpectatorManager] All players ready! Starting observer match for spectator...");

        try {
            if (menu != null) {
                if (fMenuObserverMode != null) {
                    fMenuObserverMode.setBoolean(menu, true);
                }
                menu.triggerPlayAction();
            }
        } catch (Throwable t) {
            System.err.println("[SpectatorManager] menu.triggerPlayAction error: " + t.getMessage());
        }

        try {
            if (fCountrySelectMode != null) {
                fCountrySelectMode.setBoolean(ms, false);
            }
            if (fObserverMode != null) {
                fObserverMode.setBoolean(ms, true);
            }
            if (fPlayerColorKey != null) {
                fPlayerColorKey.setInt(ms, -1);
            }
            if (fSelectedCountryColorKey != null) {
                fSelectedCountryColorKey.setInt(ms, -1);
            }
            if (fCountrySelectMenu != null) {
                CGSCountrySelectMenu m = (CGSCountrySelectMenu) fCountrySelectMenu.get(ms);
                if (m != null) {
                    m.setVisible(false);
                }
            }
            MapModeManager.getInstance().setCountrySelectMode(false);
            MapModeManager.getInstance().setPaused(false);
            if (ms.getInGameHud() != null) {
                ms.getInGameHud().setVisible(true);
                ms.getInGameHud().updateCountryName(LanguageManager.get("ui_observer"));
            }
        } catch (Throwable t) {
            System.err.println("[SpectatorManager] Error starting observer match: " + t.getMessage());
        }
    }

    private void drawSpectatorWaitingBanner(SpriteBatch batch) {
        int W = CFG.GAME_WIDTH;
        int H = CFG.GAME_HEIGHT;
        int boxW = CFG.s(540);
        int boxH = CFG.s(96);
        int boxX = (W - boxW) / 2;
        int boxY = (H - boxH) / 2;

        float pulse = (float) Math.sin(this.pulseTimer * 3.5f) * 0.25f + 0.75f;

        // Background dark card
        batch.setColor(0.04f, 0.06f, 0.12f, 0.94f);
        ImageManager.getImage(Images.pix255_255_255).draw(batch, boxX, boxY, boxW, boxH);

        // Cyber glowing cyan border
        batch.setColor(0.15f, 0.70f, 0.98f, pulse);
        ImageManager.getImage(Images.pix255_255_255).draw(batch, boxX, boxY, boxW, 2);
        ImageManager.getImage(Images.pix255_255_255).draw(batch, boxX, boxY + boxH - 2, boxW, 2);
        ImageManager.getImage(Images.pix255_255_255).draw(batch, boxX, boxY, 2, boxH);
        ImageManager.getImage(Images.pix255_255_255).draw(batch, boxX + boxW - 2, boxY, 2, boxH);

        batch.setColor(Color.WHITE);
        String title = "РЕЖИМ НАБЛЮДАТЕЛЯ (SPECTATOR)";
        int dots = ((int)(this.pulseTimer * 2.5f)) % 4;
        StringBuilder dotStr = new StringBuilder();
        for (int i = 0; i < dots; i++) dotStr.append(".");
        String sub = "Ожидание выбора стран игроками и старта раунда" + dotStr.toString();

        CFG.drawTextCentered(batch, title, W / 2, boxY + CFG.s(22), new Color(0.2f, 0.78f, 1.0f, 1.0f));
        if (CFG.fontSmall != null) {
            CFG.drawTextCentered(batch, sub, W / 2, boxY + CFG.s(58), new Color(0.85f, 0.88f, 0.95f, 0.92f));
        }
    }
}
