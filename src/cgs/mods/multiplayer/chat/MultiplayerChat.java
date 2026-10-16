package cgs.mods.multiplayer.chat;

import cgs.mods.multiplayer.actions.ChatMessageAction;
import cgs.mods.multiplayer.actions.TacticalPingAction;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputMultiplexer;
import com.badlogic.gdx.InputProcessor;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import ru.ius.david.cgs.CFG;
import ru.ius.david.cgs.cgsGame;
import ru.ius.david.cgs.game.CountryState;
import ru.ius.david.cgs.game.GameManager;
import ru.ius.david.cgs.images.ImageManager;
import ru.ius.david.cgs.images.Images;
import ru.ius.david.cgs.managers.SoundManager;
import ru.ius.david.cgs.multiplayer.MultiplayerManager;
import ru.ius.david.cgs.multiplayer.NetworkProtocol;

public class MultiplayerChat implements InputProcessor {
    private static final MultiplayerChat INSTANCE = new MultiplayerChat();
    public static MultiplayerChat getInstance() { return INSTANCE; }

    public static class ChatEntry {
        public final String sender;
        public final String text;
        public final boolean teamOnly;
        public final long timestamp;
        public float life = 12.0f; // visible for 12 seconds

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
    private cgsGame currentGame = null;

    // Interactive button bounding boxes (calculated each render)
    private int sendBtnX, sendBtnY, sendBtnW, sendBtnH;
    private int closeBtnX, closeBtnY, closeBtnW, closeBtnH;
    private int channelBtnX, channelBtnY, channelBtnW, channelBtnH;
    private int clearBtnX, clearBtnY, clearBtnW, clearBtnH;
    private int chatBoxX = -1000, chatBoxY = -1000, chatBoxW = -1, chatBoxH = -1;
    private int miniChatBtnX = -1000, miniChatBtnY = -1000, miniChatBtnW = -1, miniChatBtnH = -1;

    private static final Color CHAT_BG = new Color(0.02f, 0.04f, 0.08f, 0.92f);
    private static final Color CHAT_INPUT_BG = new Color(0.05f, 0.09f, 0.16f, 0.98f);
    private static final Color SCOREBOARD_BG = new Color(0.03f, 0.06f, 0.12f, 0.96f);
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

    public void ensureInputProcessorAttached() {
        InputProcessor current = Gdx.input.getInputProcessor();
        if (current instanceof InputMultiplexer) {
            InputMultiplexer mux = (InputMultiplexer) current;
            if (!mux.getProcessors().contains(this, true)) {
                mux.addProcessor(0, this);
            }
        } else if (current != this && current != null) {
            InputMultiplexer mux = new InputMultiplexer(this, current);
            Gdx.input.setInputProcessor(mux);
        }
    }

    public void update(float delta, cgsGame game) {
        this.currentGame = game;

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

        // Tactical map ping: Alt/Shift + Left Click when not typing
        if (!isChatActive && (Gdx.input.isKeyPressed(Input.Keys.ALT_LEFT) || Gdx.input.isKeyPressed(Input.Keys.SHIFT_LEFT))) {
            if (Gdx.input.isButtonJustPressed(Input.Buttons.LEFT)) {
                sendTacticalPing(game);
            }
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
        this.currentGame = game;
        ensureInputProcessorAttached();
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
        int chatW = Math.min(CFG.s(480), CFG.GAME_WIDTH - CFG.s(28));
        int lineH = CFG.s(20);
        int maxVisible = 8;

        float mouseX = Gdx.input.getX();
        float mouseY = Gdx.input.getY();

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

        int totalH = visibleList.size() * lineH + (isChatActive ? CFG.s(68) : 0) + CFG.s(8);
        int y = CFG.GAME_HEIGHT - CFG.s(180) - totalH;

        this.chatBoxX = x;
        this.chatBoxY = y;
        this.chatBoxW = chatW;
        this.chatBoxH = totalH;

        if (!visibleList.isEmpty() || isChatActive) {
            // Draw background plate with cyber border
            batch.setColor(CHAT_BG.r, CHAT_BG.g, CHAT_BG.b, isChatActive ? 0.92f : 0.65f);
            ImageManager.getImage(Images.pix255_255_255).draw(batch, x, y, chatW, totalH);

            if (isChatActive) {
                batch.setColor(0.2f, 0.65f, 0.95f, 0.85f);
                drawOutline(batch, x, y, chatW, totalH, 1);
            }
            batch.setColor(Color.WHITE);

            int textY = y + CFG.s(8);
            for (ChatEntry ce : visibleList) {
                if (CFG.fontSmall != null) {
                    float alpha = isChatActive ? 1.0f : Math.min(1.0f, ce.life);
                    Color senderCol = ce.sender.startsWith("📍") ? COLOR_PING_ALERT :
                                      ce.sender.equals("СИСТЕМА") ? new Color(0.9f, 0.75f, 0.2f, alpha) :
                                      new Color(0.35f, 0.85f, 1.0f, alpha);

                    String prefix = (ce.teamOnly ? "[СОЮЗ] " : "") + ce.sender + ": ";
                    CFG.drawTextSmall(batch, prefix, x + CFG.s(8), textY + CFG.s(14), senderCol);

                    CFG.glyphLayout.setText(CFG.fontSmall, prefix);
                    int pw = (int)CFG.glyphLayout.width;

                    Color msgCol = new Color(0.9f, 0.92f, 0.96f, alpha);
                    CFG.drawTextSmall(batch, ce.text, x + CFG.s(8) + pw, textY + CFG.s(14), msgCol);
                }
                textY += lineH;
            }

            // Draw interactive input box & action buttons if typing
            if (isChatActive) {
                int inputY = textY + CFG.s(4);
                int inputH = CFG.s(28);

                // Row 1 of Input Area: Text Input field
                batch.setColor(CHAT_INPUT_BG);
                ImageManager.getImage(Images.pix255_255_255).draw(batch, x + CFG.s(4), inputY, chatW - CFG.s(8), inputH);

                batch.setColor(teamChatMode ? COLOR_PING_DEFENSE : new Color(0.2f, 0.7f, 1.0f, 0.9f));
                drawOutline(batch, x + CFG.s(4), inputY, chatW - CFG.s(8), inputH, 1);

                String channelPrompt = teamChatMode ? "[СОЮЗ] " : "[ОБЩИЙ] ";
                CFG.drawTextSmall(batch, channelPrompt, x + CFG.s(10), inputY + CFG.s(19), teamChatMode ? Color.CYAN : Color.WHITE);

                CFG.glyphLayout.setText(CFG.fontSmall, channelPrompt);
                int cpw = (int)CFG.glyphLayout.width;

                String displayInput = inputBuffer.toString() + ((System.currentTimeMillis() / 450) % 2 == 0 ? "_" : "");
                CFG.drawTextSmall(batch, displayInput, x + CFG.s(10) + cpw, inputY + CFG.s(19), Color.WHITE);

                // Row 2 of Input Area: Interactive Clickable Buttons
                int btnRowY = inputY + inputH + CFG.s(4);
                int btnH = CFG.s(24);

                // 1. Channel Toggle Button [ 🌐 ОБЩИЙ / 🤝 СОЮЗ ]
                this.channelBtnX = x + CFG.s(4);
                this.channelBtnY = btnRowY;
                this.channelBtnW = CFG.s(120);
                this.channelBtnH = btnH;
                boolean chHover = (mouseX >= channelBtnX && mouseX <= channelBtnX + channelBtnW && mouseY >= channelBtnY && mouseY <= channelBtnY + channelBtnH);

                batch.setColor(chHover ? 0.16f : 0.09f, chHover ? 0.28f : 0.18f, chHover ? 0.44f : 0.28f, 1.0f);
                ImageManager.getImage(Images.pix255_255_255).draw(batch, channelBtnX, channelBtnY, channelBtnW, channelBtnH);
                batch.setColor(chHover ? 0.4f : 0.25f, chHover ? 0.8f : 0.5f, chHover ? 1.0f : 0.75f, 0.9f);
                drawOutline(batch, channelBtnX, channelBtnY, channelBtnW, channelBtnH, 1);

                String chText = teamChatMode ? "🤝 СОЮЗ (Tab)" : "🌐 ОБЩИЙ (Tab)";
                CFG.glyphLayout.setText(CFG.fontSmall, chText);
                int chTextW = (int)CFG.glyphLayout.width;
                CFG.drawTextSmall(batch, chText, channelBtnX + (channelBtnW - chTextW) / 2, CFG.textTopCentered(CFG.fontSmall, channelBtnY, channelBtnH), Color.CYAN);

                // 2. Send Button [ ✉️ ОТПРАВИТЬ ]
                this.sendBtnX = channelBtnX + channelBtnW + CFG.s(6);
                this.sendBtnY = btnRowY;
                this.sendBtnW = CFG.s(140);
                this.sendBtnH = btnH;
                boolean sendHover = (mouseX >= sendBtnX && mouseX <= sendBtnX + sendBtnW && mouseY >= sendBtnY && mouseY <= sendBtnY + sendBtnH);

                batch.setColor(sendHover ? 0.14f : 0.08f, sendHover ? 0.32f : 0.2f, sendHover ? 0.2f : 0.12f, 1.0f);
                ImageManager.getImage(Images.pix255_255_255).draw(batch, sendBtnX, sendBtnY, sendBtnW, sendBtnH);
                batch.setColor(sendHover ? 0.45f : 0.3f, sendHover ? 0.95f : 0.8f, sendHover ? 0.6f : 0.45f, 0.9f);
                drawOutline(batch, sendBtnX, sendBtnY, sendBtnW, sendBtnH, sendHover ? 2 : 1);

                String sendText = "✉️ ОТПРАВИТЬ (Enter)";
                CFG.glyphLayout.setText(CFG.fontSmall, sendText);
                int sendTextW = (int)CFG.glyphLayout.width;
                CFG.drawTextSmall(batch, sendText, sendBtnX + (sendBtnW - sendTextW) / 2, CFG.textTopCentered(CFG.fontSmall, sendBtnY, sendBtnH), Color.GREEN);

                // 3. Clear Chat Button [ 🧹 Очистить ]
                this.clearBtnX = sendBtnX + sendBtnW + CFG.s(6);
                this.clearBtnY = btnRowY;
                this.clearBtnW = CFG.s(90);
                this.clearBtnH = btnH;
                boolean clrHover = (mouseX >= clearBtnX && mouseX <= clearBtnX + clearBtnW && mouseY >= clearBtnY && mouseY <= clearBtnY + clearBtnH);

                batch.setColor(clrHover ? 0.2f : 0.1f, clrHover ? 0.18f : 0.1f, clrHover ? 0.15f : 0.1f, 1.0f);
                ImageManager.getImage(Images.pix255_255_255).draw(batch, clearBtnX, clearBtnY, clearBtnW, clearBtnH);
                batch.setColor(clrHover ? 0.6f : 0.4f, clrHover ? 0.5f : 0.35f, clrHover ? 0.4f : 0.3f, 0.8f);
                drawOutline(batch, clearBtnX, clearBtnY, clearBtnW, clearBtnH, 1);

                CFG.glyphLayout.setText(CFG.fontSmall, "🧹 Очистить");
                int clrTextW = (int)CFG.glyphLayout.width;
                CFG.drawTextSmall(batch, "🧹 Очистить", clearBtnX + (clearBtnW - clrTextW) / 2, CFG.textTopCentered(CFG.fontSmall, clearBtnY, clearBtnH), Color.LIGHT_GRAY);

                // 4. Close Button [ ✖ ЗАКРЫТЬ (ESC) ]
                this.closeBtnX = clearBtnX + clearBtnW + CFG.s(6);
                this.closeBtnY = btnRowY;
                this.closeBtnW = chatW - CFG.s(4) - (closeBtnX - x);
                this.closeBtnH = btnH;
                boolean closeHover = (mouseX >= closeBtnX && mouseX <= closeBtnX + closeBtnW && mouseY >= closeBtnY && mouseY <= closeBtnY + closeBtnH);

                batch.setColor(closeHover ? 0.45f : 0.3f, closeHover ? 0.14f : 0.1f, closeHover ? 0.14f : 0.1f, 1.0f);
                ImageManager.getImage(Images.pix255_255_255).draw(batch, closeBtnX, closeBtnY, closeBtnW, closeBtnH);
                batch.setColor(closeHover ? 1.0f : 0.85f, closeHover ? 0.35f : 0.25f, closeHover ? 0.35f : 0.25f, 0.95f);
                drawOutline(batch, closeBtnX, closeBtnY, closeBtnW, closeBtnH, closeHover ? 2 : 1);

                CFG.glyphLayout.setText(CFG.fontSmall, "✖ (ESC)");
                int closeTextW = (int)CFG.glyphLayout.width;
                CFG.drawTextSmall(batch, "✖ (ESC)", closeBtnX + (closeBtnW - closeTextW) / 2, CFG.textTopCentered(CFG.fontSmall, closeBtnY, closeBtnH), Color.SALMON);
            }
        } else {
            // When chat is hidden: draw mini clickable pill in bottom-left to open chat with mouse
            this.miniChatBtnX = x;
            this.miniChatBtnY = CFG.GAME_HEIGHT - CFG.s(32);
            this.miniChatBtnW = CFG.s(110);
            this.miniChatBtnH = CFG.s(22);
            boolean miniHover = (mouseX >= miniChatBtnX && mouseX <= miniChatBtnX + miniChatBtnW && mouseY >= miniChatBtnY && mouseY <= miniChatBtnY + miniChatBtnH);

            batch.setColor(miniHover ? 0.15f : 0.08f, miniHover ? 0.25f : 0.14f, miniHover ? 0.38f : 0.22f, 0.85f);
            ImageManager.getImage(Images.pix255_255_255).draw(batch, miniChatBtnX, miniChatBtnY, miniChatBtnW, miniChatBtnH);
            batch.setColor(0.3f, 0.6f, 0.9f, 0.7f);
            drawOutline(batch, miniChatBtnX, miniChatBtnY, miniChatBtnW, miniChatBtnH, 1);

            CFG.drawTextSmall(batch, "💬 Чат (Enter / T)", miniChatBtnX + CFG.s(6), CFG.textTopCentered(CFG.fontSmall, miniChatBtnY, miniChatBtnH), Color.LIGHT_GRAY);
        }
    }

    private void renderScoreboard(SpriteBatch batch, cgsGame game) {
        int W = CFG.GAME_WIDTH;
        int H = CFG.GAME_HEIGHT;
        int boardW = Math.min(CFG.s(760), W - CFG.s(40));
        int boardH = Math.min(CFG.s(480), H - CFG.s(80));
        int x = (W - boardW) / 2;
        int y = (H - boardH) / 2;

        batch.setColor(SCOREBOARD_BG);
        ImageManager.getImage(Images.pix255_255_255).draw(batch, x, y, boardW, boardH);

        batch.setColor(0.2f, 0.65f, 0.95f, 0.95f);
        drawOutline(batch, x, y, boardW, boardH, 2);

        int pad = CFG.s(20);
        int topY = y + pad;

        if (CFG.fontMain != null) {
            CFG.drawText(batch, "📊 ТАБЛИЦА СЕТЕВОГО МАТЧА (LIVE SCOREBOARD)", x + pad, topY + CFG.s(6), new Color(0.95f, 0.75f, 0.2f, 1.0f));
        }

        // Table headers
        int headerY = topY + CFG.s(34);
        int headerH = CFG.s(26);
        int colPlayer = x + pad + CFG.s(12);
        int colNation = colPlayer + CFG.s(170);
        int colGold = colNation + CFG.s(180);
        int colArmy = colGold + CFG.s(130);
        int colPing = colArmy + CFG.s(110);

        if (CFG.fontSmall != null) {
            int hTextY = CFG.textTopCentered(CFG.fontSmall, headerY, headerH);
            CFG.drawTextSmall(batch, "ИГРОК", colPlayer, hTextY, Color.LIGHT_GRAY);
            CFG.drawTextSmall(batch, "ГОСУДАРСТВО", colNation, hTextY, Color.LIGHT_GRAY);
            CFG.drawTextSmall(batch, "КАЗНА", colGold, hTextY, Color.LIGHT_GRAY);
            CFG.drawTextSmall(batch, "АРМИЯ", colArmy, hTextY, Color.LIGHT_GRAY);
            CFG.drawTextSmall(batch, "ПИНГ", colPing, hTextY, Color.LIGHT_GRAY);
        }

        batch.setColor(0.3f, 0.5f, 0.7f, 0.5f);
        ImageManager.getImage(Images.pix255_255_255).draw(batch, x + pad, headerY + headerH + CFG.s(3), boardW - pad * 2, 1);
        batch.setColor(Color.WHITE);

        GameManager gm = GameManager.getInstance();
        MultiplayerManager mpMgr = game != null ? game.getActiveMultiplayerManager() : null;

        int rowY = headerY + headerH + CFG.s(10);
        int curRowH = CFG.s(34);

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

                // Row background box
                batch.setColor(0.08f, 0.14f, 0.22f, 0.85f);
                ImageManager.getImage(Images.pix255_255_255).draw(batch, x + pad, rowY, boardW - pad * 2, curRowH);

                // Row outline
                batch.setColor(0.18f, 0.35f, 0.55f, 0.6f);
                drawOutline(batch, x + pad, rowY, boardW - pad * 2, curRowH, 1);

                // Left accent bar (Host = gold, Client = cyan)
                Color stripeCol = p.isHost ? new Color(0.95f, 0.75f, 0.2f, 1.0f) : new Color(0.2f, 0.65f, 0.95f, 1.0f);
                batch.setColor(stripeCol);
                ImageManager.getImage(Images.pix255_255_255).draw(batch, x + pad, rowY, CFG.s(4), curRowH);
                batch.setColor(Color.WHITE);

                if (CFG.fontSmall != null) {
                    // Exact vertical centering so text NEVER slides down
                    int ty = CFG.textTopCentered(CFG.fontSmall, rowY, curRowH);
                    CFG.drawTextSmall(batch, p.playerName + (p.isHost ? " (Хост)" : ""), colPlayer + CFG.s(6), ty, Color.WHITE);
                    CFG.drawTextSmall(batch, nationName, colNation, ty, Color.CYAN);
                    CFG.drawTextSmall(batch, gold > 0 ? (gold + " 🪙") : "—", colGold, ty, Color.GOLD);
                    CFG.drawTextSmall(batch, army > 0 ? String.valueOf(army) : "—", colArmy, ty, Color.WHITE);

                    int ping = (int)mpMgr.getDisplayPingMs();
                    Color pingCol = (ping < 100) ? Color.GREEN : (ping < 200) ? Color.YELLOW : Color.RED;
                    CFG.drawTextSmall(batch, ping > 0 ? (ping + " ms") : "OK", colPing, ty, pingCol);
                }
                rowY += curRowH + CFG.s(4);
            }
        } else {
            if (CFG.fontSmall != null) {
                int ty = CFG.textTopCentered(CFG.fontSmall, rowY, curRowH);
                CFG.drawTextSmall(batch, "Ожидание подключения игроков...", colPlayer + CFG.s(6), ty, Color.GRAY);
            }
        }
    }

    private void drawOutline(SpriteBatch batch, int x, int y, int w, int h, int thickness) {
        ImageManager.getImage(Images.pix255_255_255).draw(batch, x, y, w, thickness);
        ImageManager.getImage(Images.pix255_255_255).draw(batch, x, y + h - thickness, w, thickness);
        ImageManager.getImage(Images.pix255_255_255).draw(batch, x, y, thickness, h);
        ImageManager.getImage(Images.pix255_255_255).draw(batch, x + w - thickness, y, thickness, h);
    }

    // InputProcessor implementation
    @Override
    public boolean keyDown(int keycode) {
        if (keycode == Input.Keys.ENTER) {
            if (isChatActive) {
                String text = inputBuffer.toString().trim();
                if (!text.isEmpty()) {
                    sendMessage(text, currentGame);
                }
                isChatActive = false;
                inputBuffer.setLength(0);
                return true;
            } else {
                isChatActive = true;
                inputBuffer.setLength(0);
                return true;
            }
        }

        if (keycode == Input.Keys.T && !isChatActive) {
            isChatActive = true;
            inputBuffer.setLength(0);
            return true;
        }

        if (isChatActive) {
            if (keycode == Input.Keys.ESCAPE) {
                isChatActive = false;
                inputBuffer.setLength(0);
                return true;
            }
            if (keycode == Input.Keys.TAB) {
                teamChatMode = !teamChatMode;
                return true;
            }
            if (keycode == Input.Keys.BACKSPACE) {
                if (inputBuffer.length() > 0) {
                    inputBuffer.setLength(inputBuffer.length() - 1);
                }
                return true;
            }
            // Consume key while typing so in-game shortcuts don't fire
            return true;
        }

        return false;
    }

    @Override
    public boolean keyTyped(char character) {
        if (!isChatActive) return false;
        if (character >= 32 && character != 127 && inputBuffer.length() < 120) {
            inputBuffer.append(character);
            return true;
        }
        return false;
    }

    @Override
    public boolean touchDown(int screenX, int screenY, int pointer, int button) {
        if (!isChatActive) {
            // Click on mini chat trigger pill in bottom-left
            if (miniChatBtnW > 0 && screenX >= miniChatBtnX && screenX <= miniChatBtnX + miniChatBtnW &&
                screenY >= miniChatBtnY && screenY <= miniChatBtnY + miniChatBtnH) {
                isChatActive = true;
                inputBuffer.setLength(0);
                return true;
            }
            return false;
        }

        // 1. Click Send Button
        if (screenX >= sendBtnX && screenX <= sendBtnX + sendBtnW &&
            screenY >= sendBtnY && screenY <= sendBtnY + sendBtnH) {
            String text = inputBuffer.toString().trim();
            if (!text.isEmpty()) {
                sendMessage(text, currentGame);
            }
            isChatActive = false;
            inputBuffer.setLength(0);
            return true;
        }

        // 2. Click Close Button [ ✖ ]
        if (screenX >= closeBtnX && screenX <= closeBtnX + closeBtnW &&
            screenY >= closeBtnY && screenY <= closeBtnY + closeBtnH) {
            isChatActive = false;
            inputBuffer.setLength(0);
            return true;
        }

        // 3. Click Channel Toggle Button [ 🌐 ОБЩИЙ / 🤝 СОЮЗ ]
        if (screenX >= channelBtnX && screenX <= channelBtnX + channelBtnW &&
            screenY >= channelBtnY && screenY <= channelBtnY + channelBtnH) {
            teamChatMode = !teamChatMode;
            return true;
        }

        // 4. Click Clear Button [ 🧹 ]
        if (screenX >= clearBtnX && screenX <= clearBtnX + clearBtnW &&
            screenY >= clearBtnY && screenY <= clearBtnY + clearBtnH) {
            messages.clear();
            addSystemMessage("Чат очищен.");
            return true;
        }

        // 5. Click INSIDE chat box: consume click so player doesn't click on provinces underneath
        if (screenX >= chatBoxX && screenX <= chatBoxX + chatBoxW &&
            screenY >= chatBoxY && screenY <= chatBoxY + chatBoxH) {
            return true;
        }

        // 6. Click OUTSIDE chat box while typing:
        // Automatically close/defocus chat so the player is never trapped!
        isChatActive = false;
        return false; // let the game process click on map/units!
    }

    @Override
    public boolean keyUp(int keycode) { return isChatActive; }
    @Override
    public boolean touchCancelled(int screenX, int screenY, int pointer, int button) { return false; }
    @Override
    public boolean touchUp(int screenX, int screenY, int pointer, int button) { return false; }
    @Override
    public boolean touchDragged(int screenX, int screenY, int pointer) { return false; }
    @Override
    public boolean mouseMoved(int screenX, int screenY) { return false; }
    @Override
    public boolean scrolled(float amountX, float amountY) { return false; }
}
