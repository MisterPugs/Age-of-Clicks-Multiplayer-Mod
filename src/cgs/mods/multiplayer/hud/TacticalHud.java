package cgs.mods.multiplayer.hud;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import ru.ius.david.cgs.CFG;
import ru.ius.david.cgs.cgsGame;
import ru.ius.david.cgs.game.GameManager;
import ru.ius.david.cgs.images.ImageManager;
import ru.ius.david.cgs.images.Images;
import ru.ius.david.cgs.managers.SoundManager;
import cgs.modloader.timeline.TimelineViewerScreen;
import cgs.modloader.trade.DirectAidDialog;
import cgs.mods.multiplayer.chat.MultiplayerChat;
import cgs.mods.multiplayer.diplomacy.MultiplayerDiplomacyDialog;
import cgs.mods.multiplayer.pause.TacticalPauseManager;

public class TacticalHud {
    private static final TacticalHud INSTANCE = new TacticalHud();
    public static TacticalHud getInstance() { return INSTANCE; }

    public static class HudButton {
        public final String icon;
        public final String label;
        public final Color accent;
        public final Runnable action;
        public int x, y, w, h;

        public HudButton(String icon, String label, Color accent, Runnable action) {
            this.icon = icon;
            this.label = label;
            this.accent = accent;
            this.action = action;
        }
    }

    private final java.util.List<HudButton> buttons = new java.util.ArrayList<HudButton>();
    private boolean initialized = false;
    private long lastClickTime = 0;

    private TacticalHud() {}

    private void initButtons(final cgsGame game) {
        if (initialized) return;
        initialized = true;

        // 1. Chat
        buttons.add(new HudButton("💬", "ЧАТ", new Color(0.2f, 0.65f, 0.95f, 1.0f), new Runnable() {
            @Override
            public void run() {
                MultiplayerChat chat = MultiplayerChat.getInstance();
                chat.setTyping(!chat.isTyping());
                try {
                    Gdx.input.setOnscreenKeyboardVisible(chat.isTyping());
                } catch (Throwable ignored) {}
            }
        }));

        // 2. Lend-Lease Aid
        buttons.add(new HudButton("🤝", "ПОМОЩЬ", new Color(0.95f, 0.75f, 0.2f, 1.0f), new Runnable() {
            @Override
            public void run() {
                DirectAidDialog.getInstance().setVisible(true);
            }
        }));

        // 3. Diplomacy
        buttons.add(new HudButton("🕊️", "ДИПЛО", new Color(0.3f, 0.85f, 0.5f, 1.0f), new Runnable() {
            @Override
            public void run() {
                MultiplayerDiplomacyDialog.getInstance().open(game);
            }
        }));

        // 4. Scoreboard
        buttons.add(new HudButton("📊", "СЧЁТ", new Color(0.7f, 0.45f, 0.95f, 1.0f), new Runnable() {
            @Override
            public void run() {
                MultiplayerChat.getInstance().toggleScoreboard();
            }
        }));

        // 5. Tactical Pause
        buttons.add(new HudButton("⏸️", "ПАУЗА", new Color(0.95f, 0.4f, 0.3f, 1.0f), new Runnable() {
            @Override
            public void run() {
                TacticalPauseManager pm = TacticalPauseManager.getInstance();
                if (pm.isPaused()) {
                    pm.requestResume(game);
                } else {
                    pm.requestTacticalPause(game);
                }
            }
        }));

        // 6. Timeline & Match Graphs
        buttons.add(new HudButton("📈", "ИТОГИ", new Color(0.2f, 0.85f, 0.95f, 1.0f), new Runnable() {
            @Override
            public void run() {
                if (game != null) {
                    game.setScreen(new TimelineViewerScreen(game));
                }
            }
        }));
    }

    public boolean isMobileDevice() {
        return Gdx.app.getType() == com.badlogic.gdx.Application.ApplicationType.Android;
    }

    public void render(SpriteBatch batch, float delta, int screenW, int screenH, final cgsGame game) {
        if (!isMobileDevice()) return; // ТОЛЬКО ДЛЯ АНДРОЙДА!
        initButtons(game);
        if (CFG.fontSmall == null) return;

        int count = buttons.size();
        int btnW = Math.max(CFG.s(58), Math.min(CFG.s(82), (screenW - CFG.s(60)) / count));
        int btnH = CFG.s(32);
        int gap = CFG.s(6);
        int totalW = count * btnW + (count - 1) * gap;

        // Position: Top-Center
        int startX = (screenW - totalW) / 2;
        int startY = CFG.s(8);

        // Background dock bar
        batch.setColor(0.03f, 0.06f, 0.12f, 0.88f);
        ImageManager.getImage(Images.pix255_255_255).draw(batch, startX - CFG.s(8), startY - CFG.s(3), totalW + CFG.s(16), btnH + CFG.s(6));

        // Dock border
        batch.setColor(0.2f, 0.55f, 0.85f, 0.75f);
        ImageManager.getImage(Images.pix255_255_255).draw(batch, startX - CFG.s(8), startY - CFG.s(3), totalW + CFG.s(16), 1);
        ImageManager.getImage(Images.pix255_255_255).draw(batch, startX - CFG.s(8), startY + btnH + CFG.s(3), totalW + CFG.s(16), 1);

        float mouseX = Gdx.input.getX();
        float mouseY = Gdx.input.getY();

        for (int i = 0; i < count; i++) {
            HudButton btn = buttons.get(i);
            btn.x = startX + i * (btnW + gap);
            btn.y = startY;
            btn.w = btnW;
            btn.h = btnH;

            boolean hovered = mouseX >= btn.x && mouseX <= btn.x + btn.w && mouseY >= btn.y && mouseY <= btn.y + btn.h;

            // Button body
            if (hovered) {
                batch.setColor(btn.accent.r * 0.45f, btn.accent.g * 0.45f, btn.accent.b * 0.45f, 0.95f);
            } else {
                batch.setColor(0.08f, 0.14f, 0.22f, 0.9f);
            }
            ImageManager.getImage(Images.pix255_255_255).draw(batch, btn.x, btn.y, btn.w, btn.h);

            // Button glow outline
            batch.setColor(btn.accent.r, btn.accent.g, btn.accent.b, hovered ? 1.0f : 0.65f);
            ImageManager.getImage(Images.pix255_255_255).draw(batch, btn.x, btn.y, btn.w, 1);
            ImageManager.getImage(Images.pix255_255_255).draw(batch, btn.x, btn.y + btn.h - 1, btn.w, 1);
            ImageManager.getImage(Images.pix255_255_255).draw(batch, btn.x, btn.y, 1, btn.h);
            ImageManager.getImage(Images.pix255_255_255).draw(batch, btn.x + btn.w - 1, btn.y, 1, btn.h);

            // Icon + Label text
            String text = btn.icon + " " + btn.label;
            CFG.glyphLayout.setText(CFG.fontSmall, text);
            int tw = (int) CFG.glyphLayout.width;
            int tx = btn.x + (btn.w - tw) / 2;
            int ty = btn.y + CFG.textTopCentered(CFG.fontSmall, btn.y, btn.h);

            CFG.drawTextSmall(batch, text, tx, ty, hovered ? Color.WHITE : Color.LIGHT_GRAY);
        }

        batch.setColor(Color.WHITE);
    }

    public boolean handleClick(float mouseX, float mouseY, cgsGame game) {
        if (!isMobileDevice()) return false;
        initButtons(game);

        long now = System.currentTimeMillis();
        if (now - lastClickTime < 120) return false;

        for (HudButton btn : buttons) {
            if (mouseX >= btn.x && mouseX <= btn.x + btn.w && mouseY >= btn.y && mouseY <= btn.y + btn.h) {
                lastClickTime = now;
                try {
                    SoundManager.getInstance().play(SoundManager.SFX.BUTTON_CLICK);
                } catch (Throwable ignored) {}
                if (btn.action != null) {
                    btn.action.run();
                }
                return true; // absorb click!
            }
        }
        return false;
    }
}
