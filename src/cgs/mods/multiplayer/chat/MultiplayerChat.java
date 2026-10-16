package cgs.mods.multiplayer.chat;

import cgs.mods.multiplayer.actions.ChatMessageAction;
import cgs.mods.multiplayer.actions.TacticalPingAction;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import ru.ius.david.cgs.CFG;
import ru.ius.david.cgs.CountryData;
import ru.ius.david.cgs.cgsGame;
import ru.ius.david.cgs.game.CountryState;
import ru.ius.david.cgs.game.GameManager;
import ru.ius.david.cgs.images.ImageManager;
import ru.ius.david.cgs.images.Images;
import ru.ius.david.cgs.managers.SoundManager;
import ru.ius.david.cgs.multiplayer.MultiplayerManager;
import ru.ius.david.cgs.multiplayer.NetworkProtocol;

public class MultiplayerChat {
    private static final MultiplayerChat INSTANCE = new MultiplayerChat();
    public static MultiplayerChat getInstance() { return INSTANCE; }

    public static class ChatEntry {
        public final String sender;
        public final String text;
        public final boolean teamOnly;
        public final long timestamp;
        public float life = 10.0f; // visible for 10 seconds

        public ChatEntry(String sender, String text, boolean teamOnly) {
            this.sender = sender;
            this.text = text;
            this.teamOnly = teamOnly;
            this.timestamp = System.currentTimeMillis();
        }
    }

    public static class MapPing {
        public final String sender;
        public final float worldX, worldY;
        public final int type;
        public float life = 6.0f;
        public float ringScale = 0.0f;

        public MapPing(String sender, float x, float y, int type) {
            this.sender = sender;
            this.worldX = x;
            this.worldY = y;
            this.type = type;
        }
    }

    private final List<ChatEntry> messages = Collections.synchronizedList(new ArrayList<ChatEntry>());
    private final List<MapPing> pings = Collections.synchronizedList(new ArrayList<MapPing>());

    private boolean isChatActive = false;
    private StringBuilder inputBuffer = new StringBuilder();
    private boolean teamChatMode = false;
    private float keyRepeatTimer = 0.0f;

    private static final Color CHAT_BG = new Color(0.02f, 0.03f, 0.07f, 0.88f);
    private static final Color CHAT_INPUT_BG = new Color(0.06f, 0.09f, 0.16f, 0.95f);
    private static final Color SCOREBOARD_BG = new Color(0.03f, 0.05f, 0.1f, 0.92f);
    private static final Color COLOR_PING_ATTACK = new Color(0.95f, 0.2f, 0.2f, 0.9f);
    private static final Color COLOR_PING_DEFENSE = new Color(0.2f, 0.5f, 0.95f, 0.9f);
    private static final Color COLOR_PING_ALERT = new Color(0.95f, 0.8f, 0.15f, 0.9f);

    private MultiplayerChat() {
        addSystemMessage("Сетевой чат и тактические пинги активированы (Enter / T для чата, Tab для счета)");
    }

    public void addSystemMessage(String text) {
        messages.add(new ChatEntry("СИСТЕМА", text, false));
    }

    public void onReceiveMessage(String sender, String text, boolean teamOnly) {
        messages.add(new ChatEntry(sender, text, teamOnly));
        try {
            SoundManager.getInstance().play(SoundManager.SFX.BUTTON_CLICK);
        } catch (Throwable ignored) {}
    }

    public void onReceivePing(String sender, float x, float y, int type) {
        pings.add(new MapPing(sender, x, y, type));
        String typeStr = (type == 0) ? "АТАКА" : (type == 1) ? "ОБОРОНА" : "ВНИМАНИЕ";
        messages.add(new ChatEntry("📍 " + sender, "Тактическая метка: " + typeStr, false));
        try {
            SoundManager.getInstance().play(SoundManager.SFX.BUTTON_CLICK);
        } catch (Throwable ignored) {}
    }

    public boolean isTyping() {
        return isChatActive;
    }

    public void setTyping(boolean typing) {
        this.isChatActive = typing;
        if (!typing) {
            inputBuffer.setLength(0);
        }
    }

    public void update(float delta, cgsGame game) {
        // Update ping life
        synchronized (pings) {
            for (int i = pings.size() - 1; i >= 0; --i) {
                MapPing p = pings.get(i);
                p.life -= delta;
                p.ringScale += delta * 2.5f;
                if (p.life <= 0) pings.remove(i);
            }
        }

        // Update message life
        synchronized (messages) {
            for (ChatEntry m : messages) {
                if (!isChatActive) {
                    m.life -= delta;
                }
            }
        }

        // Handle Keyboard Shortcuts
        if (Gdx.input.isKeyJustPressed(Input.Keys.ENTER) || (!isChatActive && Gdx.input.isKeyJustPressed(Input.Keys.T))) {
            if (isChatActive) {
                // Send message
                String text = inputBuffer.toString().trim();
                if (!text.isEmpty()) {
                    sendMessage(text, game);
                }
                isChatActive = false;
                inputBuffer.setLength(0);
            } else {
                isChatActive = true;
                inputBuffer.setLength(0);
            }
        } else if (isChatActive && Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) {
            isChatActive = false;
            inputBuffer.setLength(0);
        }

        // Handle text typing while chat is active
        if (isChatActive) {
            handleChatTyping(delta);
        }

        // Handle tactical map ping: Alt/Shift + Left Click
        if (!isChatActive && (Gdx.input.isKeyPressed(Input.Keys.ALT_LEFT) || Gdx.input.isKeyPressed(Input.Keys.SHIFT_LEFT))) {
            if (Gdx.input.isButtonJustPressed(Input.Buttons.LEFT)) {
                sendTacticalPing(game);
            }
        }
    }

    private void handleChatTyping(float delta) {
        if (Gdx.input.isKeyJustPressed(Input.Keys.BACKSPACE)) {
            if (inputBuffer.length() > 0) {
                inputBuffer.setLength(inputBuffer.length() - 1);
            }
        } else if (Gdx.input.isKeyPressed(Input.Keys.BACKSPACE)) {
            keyRepeatTimer += delta;
            if (keyRepeatTimer > 0.4f && inputBuffer.length() > 0) {
                inputBuffer.setLength(inputBuffer.length() - 1);
                keyRepeatTimer = 0.35f;
            }
        } else {
            keyRepeatTimer = 0.0f;
        }

        if (Gdx.input.isKeyJustPressed(Input.Keys.TAB)) {
            teamChatMode = !teamChatMode;
        }
    }

    public void handleCharacterTyped(char character) {
        if (!isChatActive) return;
        if (character >= 32 && character != 127 && inputBuffer.length() < 120) {
            inputBuffer.append(character);
        }
    }

    public void sendMessage(String text, cgsGame game) {
        if (text.startsWith("/clear")) {
            messages.clear();
            addSystemMessage("Чат очищен.");
            return;
        }
        if (text.startsWith("/help")) {
            addSystemMessage("Команды: /clear - очистить, Tab - переключить канал (Все / Союзники)");
            return;
        }

        MultiplayerManager mpMgr = game != null ? game.getActiveMultiplayerManager() : null;
        String myName = (mpMgr != null && mpMgr.getPlayerName() != null) ? mpMgr.getPlayerName() : "Игрок";
        int myId = (mpMgr != null) ? mpMgr.getLocalPlayerId() : 0;

        if (mpMgr != null && mpMgr.isMultiplayer()) {
            mpMgr.enqueueAction(new ChatMessageAction(myId, myName, text, teamChatMode));
        } else {
            onReceiveMessage(myName, text, teamChatMode);
        }
    }

    private void sendTacticalPing(cgsGame game) {
        MultiplayerManager mpMgr = game != null ? game.getActiveMultiplayerManager() : null;
        String myName = (mpMgr != null && mpMgr.getPlayerName() != null) ? mpMgr.getPlayerName() : "Игрок";
        int myId = (mpMgr != null) ? mpMgr.getLocalPlayerId() : 0;

        float clickX = Gdx.input.getX();
        float clickY = Gdx.input.getY();

        int type = Gdx.input.isKeyPressed(Input.Keys.ALT_LEFT) ? 0 : 1; // Alt = Attack, Shift = Defense

        if (mpMgr != null && mpMgr.isMultiplayer()) {
            mpMgr.enqueueAction(new TacticalPingAction(myId, myName, clickX, clickY, type));
        } else {
            onReceivePing(myName, clickX, clickY, type);
        }
    }

    public void render(SpriteBatch batch, float delta, cgsGame game) {
        update(delta, game);

        // 1. Render Tactical Map Pings
        renderPings(batch);

        // 2. Render In-Game Chat Box
        renderChat(batch);

        // 3. Render Tab Scoreboard if TAB is held
        if (Gdx.input.isKeyPressed(Input.Keys.TAB)) {
            renderScoreboard(batch, game);
        }
    }

    private void renderPings(SpriteBatch batch) {
        if (pings.isEmpty()) return;
        synchronized (pings) {
            for (MapPing p : pings) {
                float alpha = Math.min(1.0f, p.life);
                Color baseColor = (p.type == 0) ? COLOR_PING_ATTACK : (p.type == 1) ? COLOR_PING_DEFENSE : COLOR_PING_ALERT;
                batch.setColor(baseColor.r, baseColor.g, baseColor.b, baseColor.a * alpha);

                int size = (int)(CFG.s(28) + (p.ringScale % 3.0f) * CFG.s(16));
                ImageManager.getImage(Images.pix255_255_255).draw(batch, (int)p.worldX - size / 2, (int)p.worldY - size / 2, size, 2);
                ImageManager.getImage(Images.pix255_255_255).draw(batch, (int)p.worldX - size / 2, (int)p.worldY - size / 2, 2, size);
                ImageManager.getImage(Images.pix255_255_255).draw(batch, (int)p.worldX + size / 2, (int)p.worldY - size / 2, 2, size);
                ImageManager.getImage(Images.pix255_255_255).draw(batch, (int)p.worldX - size / 2, (int)p.worldY + size / 2, size, 2);

                if (CFG.fontSmall != null) {
                    CFG.drawTextSmall(batch, "📍 " + p.sender, (int)p.worldX + CFG.s(10), (int)p.worldY + CFG.s(10), Color.WHITE);
                }
            }
        }
        batch.setColor(Color.WHITE);
    }

    private void renderChat(SpriteBatch batch) {
        int x = CFG.s(14);
        int chatW = Math.min(CFG.s(420), CFG.GAME_WIDTH - CFG.s(28));
        int lineH = CFG.s(20);
        int maxVisible = 8;

        List<ChatEntry> visibleList = new ArrayList<ChatEntry>();
        synchronized (messages) {
            for (int i = messages.size() - 1; i >= 0 && visibleList.size() < maxVisible; --i) {
                ChatEntry ce = messages.get(i);
                if (isChatActive || ce.life > 0) {
                    visibleList.add(ce);
                }
            }
        }
        Collections.reverse(visibleList);

        int totalH = visibleList.size() * lineH + (isChatActive ? CFG.s(32) : 0) + CFG.s(8);
        int y = CFG.GAME_HEIGHT - CFG.s(180) - totalH;

        if (!visibleList.isEmpty() || isChatActive) {
            // Draw background plate
            batch.setColor(CHAT_BG.r, CHAT_BG.g, CHAT_BG.b, isChatActive ? 0.9f : 0.65f);
            ImageManager.getImage(Images.pix255_255_255).draw(batch, x, y, chatW, totalH);
            batch.setColor(Color.WHITE);

            int textY = y + CFG.s(8);
            for (ChatEntry ce : visibleList) {
                if (CFG.fontSmall != null) {
                    float alpha = isChatActive ? 1.0f : Math.min(1.0f, ce.life);
                    Color senderCol = ce.sender.startsWith("📍") ? COLOR_PING_ALERT :
                                      ce.sender.equals("СИСТЕМА") ? new Color(0.9f, 0.75f, 0.2f, alpha) :
                                      new Color(0.3f, 0.75f, 1.0f, alpha);

                    String prefix = (ce.teamOnly ? "[СОЮЗ] " : "") + ce.sender + ": ";
                    CFG.drawTextSmall(batch, prefix, x + CFG.s(8), textY + CFG.s(14), senderCol);

                    CFG.glyphLayout.setText(CFG.fontSmall, prefix);
                    int pw = (int)CFG.glyphLayout.width;

                    Color msgCol = new Color(0.9f, 0.9f, 0.95f, alpha);
                    CFG.drawTextSmall(batch, ce.text, x + CFG.s(8) + pw, textY + CFG.s(14), msgCol);
                }
                textY += lineH;
            }

            // Draw input box if typing
            if (isChatActive) {
                int inputY = textY + CFG.s(4);
                int inputH = CFG.s(26);

                batch.setColor(CHAT_INPUT_BG);
                ImageManager.getImage(Images.pix255_255_255).draw(batch, x + CFG.s(4), inputY, chatW - CFG.s(8), inputH);

                batch.setColor(teamChatMode ? COLOR_PING_DEFENSE : new Color(0.2f, 0.65f, 0.95f, 0.8f));
                ImageManager.getImage(Images.pix255_255_255).draw(batch, x + CFG.s(4), inputY, chatW - CFG.s(8), 1);

                String channelPrompt = teamChatMode ? "[СОЮЗ] " : "[ОБЩИЙ] ";
                CFG.drawTextSmall(batch, channelPrompt, x + CFG.s(10), inputY + CFG.s(18), teamChatMode ? Color.CYAN : Color.WHITE);

                CFG.glyphLayout.setText(CFG.fontSmall, channelPrompt);
                int cpw = (int)CFG.glyphLayout.width;

                String displayInput = inputBuffer.toString() + ((System.currentTimeMillis() / 400) % 2 == 0 ? "_" : "");
                CFG.drawTextSmall(batch, displayInput, x + CFG.s(10) + cpw, inputY + CFG.s(18), Color.WHITE);
            }
        }
    }

    private void renderScoreboard(SpriteBatch batch, cgsGame game) {
        int W = CFG.GAME_WIDTH;
        int H = CFG.GAME_HEIGHT;
        int boardW = Math.min(CFG.s(720), W - CFG.s(40));
        int boardH = Math.min(CFG.s(460), H - CFG.s(80));
        int x = (W - boardW) / 2;
        int y = (H - boardH) / 2;

        batch.setColor(SCOREBOARD_BG);
        ImageManager.getImage(Images.pix255_255_255).draw(batch, x, y, boardW, boardH);

        batch.setColor(0.18f, 0.35f, 0.65f, 0.9f);
        ImageManager.getImage(Images.pix255_255_255).draw(batch, x, y, boardW, 2);
        ImageManager.getImage(Images.pix255_255_255).draw(batch, x, y + boardH - 2, boardW, 2);
        ImageManager.getImage(Images.pix255_255_255).draw(batch, x, y, 2, boardH);
        ImageManager.getImage(Images.pix255_255_255).draw(batch, x + boardW - 2, y, 2, boardH);

        int pad = CFG.s(20);
        int topY = y + pad;

        if (CFG.fontMain != null) {
            CFG.drawText(batch, "📊 ТАБЛИЦА СЕТЕВОГО МАТЧА (LIVE SCOREBOARD)", x + pad, topY + CFG.s(14), new Color(0.95f, 0.75f, 0.2f, 1.0f));
        }

        // Table headers
        int headerY = topY + CFG.s(38);
        int rowH = CFG.s(32);
        int colPlayer = x + pad;
        int colNation = colPlayer + CFG.s(150);
        int colLand = colNation + CFG.s(180);
        int colArmy = colLand + CFG.s(110);
        int colPing = colArmy + CFG.s(110);

        if (CFG.fontSmall != null) {
            CFG.drawTextSmall(batch, "ИГРОК", colPlayer, headerY, Color.LIGHT_GRAY);
            CFG.drawTextSmall(batch, "ГОСУДАРСТВО", colNation, headerY, Color.LIGHT_GRAY);
            CFG.drawTextSmall(batch, "ЗЕМЛИ (PX)", colLand, headerY, Color.LIGHT_GRAY);
            CFG.drawTextSmall(batch, "АРМИЯ", colArmy, headerY, Color.LIGHT_GRAY);
            CFG.drawTextSmall(batch, "ПИНГ", colPing, headerY, Color.LIGHT_GRAY);
        }

        batch.setColor(0.3f, 0.4f, 0.5f, 0.4f);
        ImageManager.getImage(Images.pix255_255_255).draw(batch, x + pad, headerY + CFG.s(6), boardW - pad * 2, 1);
        batch.setColor(Color.WHITE);

        GameManager gm = GameManager.getInstance();
        MultiplayerManager mpMgr = game != null ? game.getActiveMultiplayerManager() : null;

        int rowY = headerY + CFG.s(14);
        if (mpMgr != null && mpMgr.getLatestLobbyState() != null && mpMgr.getLatestLobbyState().players != null) {
            for (NetworkProtocol.PlayerInfo p : mpMgr.getLatestLobbyState().players) {
                String nationName = (p.selectedCountryName != null && !p.selectedCountryName.isEmpty()) ? p.selectedCountryName : "Выбирает...";
                long army = 0;
                long gold = 0;

                if (gm != null && gm.getWorldCountries() != null && p.selectedCountryName != null) {
                    CountryState cs = gm.getWorldCountries().get(p.selectedCountryName);
                    if (cs != null) {
                        army = cs.getMilitaryCount();
                        gold = cs.getGold();
                    }
                }

                batch.setColor(0.08f, 0.12f, 0.2f, 0.7f);
                ImageManager.getImage(Images.pix255_255_255).draw(batch, x + pad, rowY, boardW - pad * 2, rowH - CFG.s(4));

                Color stripeCol = p.isHost ? new Color(0.95f, 0.75f, 0.2f, 1.0f) : new Color(0.2f, 0.65f, 0.95f, 1.0f);
                batch.setColor(stripeCol);
                ImageManager.getImage(Images.pix255_255_255).draw(batch, x + pad, rowY, CFG.s(4), rowH - CFG.s(4));
                batch.setColor(Color.WHITE);

                if (CFG.fontSmall != null) {
                    int ty = rowY + CFG.s(20);
                    CFG.drawTextSmall(batch, p.playerName + (p.isHost ? " (Хост)" : ""), colPlayer + CFG.s(8), ty, Color.WHITE);
                    CFG.drawTextSmall(batch, nationName, colNation, ty, Color.CYAN);
                    CFG.drawTextSmall(batch, gold > 0 ? (gold + " 🪙") : "—", colLand, ty, Color.GOLD);
                    CFG.drawTextSmall(batch, army > 0 ? String.valueOf(army) : "—", colArmy, ty, Color.WHITE);

                    int ping = (int)mpMgr.getDisplayPingMs();
                    Color pingCol = (ping < 100) ? Color.GREEN : (ping < 200) ? Color.YELLOW : Color.RED;
                    CFG.drawTextSmall(batch, ping > 0 ? (ping + " ms") : "OK", colPing, ty, pingCol);
                }
                rowY += rowH;
            }
        } else {
            if (CFG.fontSmall != null) {
                CFG.drawTextSmall(batch, "Ожидание подключения игроков...", colPlayer + CFG.s(8), rowY + CFG.s(20), Color.GRAY);
            }
        }
    }
}
