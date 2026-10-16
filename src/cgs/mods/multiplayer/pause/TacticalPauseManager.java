package cgs.mods.multiplayer.pause;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import ru.ius.david.cgs.CFG;
import ru.ius.david.cgs.cgsGame;
import ru.ius.david.cgs.game.GameManager;
import ru.ius.david.cgs.images.ImageManager;
import ru.ius.david.cgs.images.Images;
import ru.ius.david.cgs.managers.SoundManager;
import ru.ius.david.cgs.multiplayer.MultiplayerManager;
import cgs.mods.multiplayer.actions.TacticalPauseAction;
import cgs.mods.multiplayer.chat.MultiplayerChat;

public class TacticalPauseManager {
    private static final TacticalPauseManager INSTANCE = new TacticalPauseManager();
    public static TacticalPauseManager getInstance() { return INSTANCE; }

    private boolean isPaused = false;
    private float pauseRemainingSeconds = 0.0f;
    private String pausedByPlayer = "";
    private int localPausesUsed = 0;
    private int currentSpeed = 1;

    // Resume button bounding box for click detection
    private int resumeBtnX = -1000, resumeBtnY = -1000, resumeBtnW = 0, resumeBtnH = 0;

    private TacticalPauseManager() {}

    public boolean isPaused() {
        return isPaused;
    }

    public int getCurrentSpeed() {
        return currentSpeed;
    }

    public int getLocalPausesRemaining() {
        return Math.max(0, 2 - localPausesUsed);
    }

    public void requestTacticalPause(cgsGame game) {
        if (isPaused) return;
        if (localPausesUsed >= 2) {
            MultiplayerChat.getInstance().addSystemMessage("⚠️ Лимит тактических пауз исчерпан (максимум 2 за матч).");
            return;
        }

        GameManager gm = GameManager.getInstance();
        if (gm == null) return;

        MultiplayerManager mp = game != null ? game.getActiveMultiplayerManager() : null;
        int localId = mp != null ? mp.getLocalPlayerId() : 0;
        String myName = (gm.getPlayerCountry() != null) ? gm.getPlayerCountry().getLocalizedName() : "Игрок";

        localPausesUsed++;
        TacticalPauseAction act = new TacticalPauseAction(localId, myName, TacticalPauseAction.ACT_REQUEST_PAUSE, 0);

        if (mp != null) {
            mp.enqueueAction(act);
        } else {
            act.execute(gm);
        }
    }

    public void requestResume(cgsGame game) {
        if (!isPaused) return;

        GameManager gm = GameManager.getInstance();
        if (gm == null) return;

        MultiplayerManager mp = game != null ? game.getActiveMultiplayerManager() : null;
        int localId = mp != null ? mp.getLocalPlayerId() : 0;
        String myName = (gm.getPlayerCountry() != null) ? gm.getPlayerCountry().getLocalizedName() : "Игрок";

        TacticalPauseAction act = new TacticalPauseAction(localId, myName, TacticalPauseAction.ACT_RESUME, 0);

        if (mp != null) {
            mp.enqueueAction(act);
        } else {
            act.execute(gm);
        }
    }

    public void requestSetSpeed(cgsGame game, int speed) {
        GameManager gm = GameManager.getInstance();
        if (gm == null) return;

        MultiplayerManager mp = game != null ? game.getActiveMultiplayerManager() : null;
        int localId = mp != null ? mp.getLocalPlayerId() : 0;
        String myName = (gm.getPlayerCountry() != null) ? gm.getPlayerCountry().getLocalizedName() : "Игрок";

        TacticalPauseAction act = new TacticalPauseAction(localId, myName, TacticalPauseAction.ACT_CHANGE_SPEED, speed);

        if (mp != null) {
            mp.enqueueAction(act);
        } else {
            act.execute(gm);
        }
    }

    public void handleAction(TacticalPauseAction action, GameManager gm) {
        if (action == null || gm == null) return;

        if (action.actionType == TacticalPauseAction.ACT_REQUEST_PAUSE) {
            this.isPaused = true;
            this.pauseRemainingSeconds = 45.0f;
            this.pausedByPlayer = action.requestPlayerName;
            try {
                gm.setPlaySpeed(0);
            } catch (Throwable ignored) {}

            MultiplayerChat.getInstance().addSystemMessage("⏸️ [ПАУЗА] " + pausedByPlayer + " взял тактическую паузу (45с)!");
            try {
                SoundManager.getInstance().play(SoundManager.SFX.BUTTON_CLICK);
            } catch (Throwable ignored) {}
        } else if (action.actionType == TacticalPauseAction.ACT_RESUME) {
            this.isPaused = false;
            try {
                gm.setPlaySpeed(currentSpeed);
            } catch (Throwable ignored) {}

            MultiplayerChat.getInstance().addSystemMessage("▶️ [ПАУЗА] Игра возобновлена (" + action.requestPlayerName + ").");
            try {
                SoundManager.getInstance().play(SoundManager.SFX.BUTTON_CLICK);
            } catch (Throwable ignored) {}
        } else if (action.actionType == TacticalPauseAction.ACT_CHANGE_SPEED) {
            this.currentSpeed = Math.max(1, Math.min(5, action.targetSpeed));
            if (!this.isPaused) {
                try {
                    gm.setPlaySpeed(currentSpeed);
                } catch (Throwable ignored) {}
            }
            MultiplayerChat.getInstance().addSystemMessage("⏩ [СКОРОСТЬ] Установлена скорость " + currentSpeed + "x (" + action.requestPlayerName + ").");
        }
    }

    public void update(float delta, GameManager gm) {
        if (!isPaused) return;

        pauseRemainingSeconds -= delta;
        if (pauseRemainingSeconds <= 0.0f) {
            isPaused = false;
            pauseRemainingSeconds = 0.0f;
            if (gm != null) {
                try {
                    gm.setPlaySpeed(currentSpeed);
                } catch (Throwable ignored) {}
            }
            MultiplayerChat.getInstance().addSystemMessage("▶️ [ПАУЗА] Время тактической паузы истекло. Возобновление игры!");
            try {
                SoundManager.getInstance().play(SoundManager.SFX.BUTTON_CLICK);
            } catch (Throwable ignored) {}
        }
    }

    public void renderBanner(SpriteBatch batch, float delta, int screenW, int screenH, cgsGame game) {
        if (!isPaused || CFG.fontMain == null) return;

        int barW = Math.min(CFG.s(580), screenW - CFG.s(40));
        int barH = CFG.s(44);
        int barX = (screenW - barW) / 2;
        int barY = CFG.s(10);

        // Dark neon bar background
        batch.setColor(0.04f, 0.07f, 0.14f, 0.95f);
        ImageManager.getImage(Images.pix255_255_255).draw(batch, barX, barY, barW, barH);

        // Cyber yellow/cyan glow border
        batch.setColor(0.95f, 0.75f, 0.15f, 0.9f);
        ImageManager.getImage(Images.pix255_255_255).draw(batch, barX, barY, barW, 2);
        ImageManager.getImage(Images.pix255_255_255).draw(batch, barX, barY + barH - 2, barW, 2);
        ImageManager.getImage(Images.pix255_255_255).draw(batch, barX, barY, 2, barH);
        ImageManager.getImage(Images.pix255_255_255).draw(batch, barX + barW - 2, barY, 2, barH);

        // Text info
        int sec = (int) Math.ceil(pauseRemainingSeconds);
        String label = "⏸️ ПАУЗА (" + pausedByPlayer + "): " + sec + "с";
        CFG.drawText(batch, label, barX + CFG.s(16), barY + CFG.textTopCentered(CFG.fontMain, barY, barH), new Color(0.95f, 0.85f, 0.3f, 1.0f));

        // Resume button
        resumeBtnW = CFG.s(140);
        resumeBtnH = CFG.s(32);
        resumeBtnX = barX + barW - resumeBtnW - CFG.s(8);
        resumeBtnY = barY + (barH - resumeBtnH) / 2;

        batch.setColor(0.15f, 0.55f, 0.35f, 0.95f);
        ImageManager.getImage(Images.pix255_255_255).draw(batch, resumeBtnX, resumeBtnY, resumeBtnW, resumeBtnH);

        batch.setColor(0.3f, 0.95f, 0.5f, 0.9f);
        ImageManager.getImage(Images.pix255_255_255).draw(batch, resumeBtnX, resumeBtnY, resumeBtnW, 1);
        ImageManager.getImage(Images.pix255_255_255).draw(batch, resumeBtnX, resumeBtnY + resumeBtnH - 1, resumeBtnW, 1);
        ImageManager.getImage(Images.pix255_255_255).draw(batch, resumeBtnX, resumeBtnY, 1, resumeBtnH);
        ImageManager.getImage(Images.pix255_255_255).draw(batch, resumeBtnX + resumeBtnW - 1, resumeBtnY, 1, resumeBtnH);

        String resText = "▶️ ПУСК";
        CFG.glyphLayout.setText(CFG.fontSmall != null ? CFG.fontSmall : CFG.fontMain, resText);
        int tw = (int) CFG.glyphLayout.width;
        CFG.drawTextSmall(batch, resText, resumeBtnX + (resumeBtnW - tw) / 2, resumeBtnY + CFG.textTopCentered(CFG.fontSmall, resumeBtnY, resumeBtnH), Color.WHITE);

        batch.setColor(Color.WHITE);
    }

    public boolean handleClick(float screenX, float screenY, cgsGame game) {
        if (!isPaused) return false;

        if (screenX >= resumeBtnX && screenX <= resumeBtnX + resumeBtnW &&
            screenY >= resumeBtnY && screenY <= resumeBtnY + resumeBtnH) {
            requestResume(game);
            return true;
        }
        return false;
    }
}
