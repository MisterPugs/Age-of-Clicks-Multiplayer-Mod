package cgs.modloader.timeline;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputAdapter;
import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.utils.ScreenUtils;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import ru.ius.david.cgs.CFG;
import ru.ius.david.cgs.cgsGame;
import ru.ius.david.cgs.elements.ButtonElement;
import ru.ius.david.cgs.images.Image;
import ru.ius.david.cgs.images.ImageManager;
import ru.ius.david.cgs.images.Images;
import ru.ius.david.cgs.menus.SliderMenu;

public class TimelineViewerScreen extends ScreenAdapter {
    private final cgsGame game;
    private final SpriteBatch batch;
    private Matrix4 uiProjection;
    private SliderMenu menuPanel;
    private int currentFrame = 0;
    private boolean isPlaying = true;
    private float playTimer = 0.0f;

    private static final Color[] NATION_COLORS = new Color[]{
        new Color(0.2f, 0.7f, 1.0f, 1.0f),
        new Color(0.95f, 0.3f, 0.3f, 1.0f),
        new Color(0.95f, 0.8f, 0.2f, 1.0f),
        new Color(0.3f, 0.9f, 0.45f, 1.0f),
        new Color(0.75f, 0.4f, 0.95f, 1.0f),
        new Color(0.95f, 0.5f, 0.15f, 1.0f)
    };

    public TimelineViewerScreen(cgsGame game) {
        this.game = game;
        this.batch = game.getSharedBatch();
        updateUIProjection();
        buildUI();
    }

    private void updateUIProjection() {
        int w = Gdx.graphics.getWidth();
        int h = Gdx.graphics.getHeight();
        this.uiProjection = new Matrix4();
        this.uiProjection.setToOrtho2D(0.0f, 0.0f, w, h);
        this.uiProjection.val[5] = -2.0f / (float)h;
        this.uiProjection.val[13] = 1.0f;
    }

    private void buildUI() {
        int W = CFG.GAME_WIDTH;
        int H = CFG.GAME_HEIGHT;
        java.util.ArrayList<ru.ius.david.cgs.elements.MenuElement> elements = new java.util.ArrayList<>();

        int btnW = CFG.s(150);
        int btnH = CFG.s(42);
        int y = H - CFG.s(56);

        // 1. Play / Pause
        elements.add(new ButtonElement(CFG.s(30), y, btnW, btnH, isPlaying ? "⏸️ ПАУЗА" : "▶️ ВОСПРОИЗВЕДЕНИЕ", CFG.COLOR_PARCHMENT_BTN_BG, CFG.COLOR_TEXT_DARK, new Runnable() {
            @Override
            public void run() {
                isPlaying = !isPlaying;
                buildUI();
            }
        }));

        // 2. Step Prev
        elements.add(new ButtonElement(CFG.s(190), y, CFG.s(70), btnH, "◀", CFG.COLOR_PARCHMENT_BTN_BG, CFG.COLOR_TEXT_DARK, new Runnable() {
            @Override
            public void run() {
                List<TimelineRecorder.Snapshot> snaps = TimelineRecorder.getInstance().getSnapshots();
                if (!snaps.isEmpty()) {
                    currentFrame = (currentFrame - 1 + snaps.size()) % snaps.size();
                }
            }
        }));

        // 3. Step Next
        elements.add(new ButtonElement(CFG.s(266), y, CFG.s(70), btnH, "▶", CFG.COLOR_PARCHMENT_BTN_BG, CFG.COLOR_TEXT_DARK, new Runnable() {
            @Override
            public void run() {
                List<TimelineRecorder.Snapshot> snaps = TimelineRecorder.getInstance().getSnapshots();
                if (!snaps.isEmpty()) {
                    currentFrame = (currentFrame + 1) % snaps.size();
                }
            }
        }));

        // 4. Close
        elements.add(new ButtonElement(W - btnW - CFG.s(30), y, btnW, btnH, "✖ В ИГРУ", CFG.COLOR_TERTIARY_BTN, Color.WHITE, new Runnable() {
            @Override
            public void run() {
                if (game.getActiveMultiplayerManager() != null) {
                    game.showMultiplayerLobby();
                } else {
                    game.showMainMenu();
                }
            }
        }));

        this.menuPanel = new SliderMenu();
        this.menuPanel.initMenu(null, 0, 0, W, H, elements, true, false);
        this.menuPanel.setVisible(true);
    }

    @Override
    public void render(float delta) {
        ScreenUtils.clear(new Color(0.03f, 0.05f, 0.09f, 1.0f));

        List<TimelineRecorder.Snapshot> snaps = TimelineRecorder.getInstance().getSnapshots();
        if (isPlaying && !snaps.isEmpty()) {
            playTimer += delta;
            if (playTimer >= 0.75f) {
                playTimer = 0.0f;
                currentFrame = (currentFrame + 1) % snaps.size();
            }
        }

        this.batch.setProjectionMatrix(this.uiProjection);
        this.batch.begin();

        Image pix = ImageManager.getImage(Images.pix255_255_255);
        int W = CFG.GAME_WIDTH;
        int H = CFG.GAME_HEIGHT;

        // Title Header
        if (CFG.fontMain != null) {
            String title = "📈 ХРОНИКА МАТЧА & ИТОГОВЫЕ ГРАФИКИ (POST-GAME TIMELINE)";
            CFG.drawText(this.batch, title, CFG.s(30), CFG.s(36), new Color(0.35f, 0.88f, 1.0f, 1.0f));
        }

        if (snaps.isEmpty()) {
            if (CFG.fontSmall != null) {
                CFG.drawTextSmall(this.batch, "Данные партии формируются... Сыграйте хотя бы несколько игровых месяцев!", CFG.s(30), CFG.s(80), Color.LIGHT_GRAY);
            }
        } else {
            TimelineRecorder.Snapshot s = snaps.get(Math.min(currentFrame, snaps.size() - 1));

            // Snapshot counter & status
            if (CFG.fontSmall != null) {
                String sub = "Игровой год: " + s.year + " | Активных войн: " + s.warsCount +
                             " | Кадр: [" + (currentFrame + 1) + " / " + snaps.size() + "]";
                CFG.drawTextSmall(this.batch, sub, CFG.s(30), CFG.s(68), new Color(0.95f, 0.8f, 0.2f, 1.0f));
            }

            // 1. Draw Graph Area
            int graphX = CFG.s(30);
            int graphY = CFG.s(96);
            int graphW = W - CFG.s(380);
            int graphH = H - graphY - CFG.s(160);

            // Graph background
            batch.setColor(0.05f, 0.08f, 0.15f, 0.95f);
            pix.draw(batch, graphX, graphY, graphW, graphH);

            // Graph border
            batch.setColor(0.2f, 0.5f, 0.85f, 0.8f);
            drawOutline(pix, batch, graphX, graphY, graphW, graphH, 2);

            // Grid lines
            batch.setColor(0.12f, 0.2f, 0.35f, 0.45f);
            for (int gy = 1; gy <= 4; gy++) {
                int lineY = graphY + gy * (graphH / 5);
                pix.draw(batch, graphX, lineY, graphW, 1);
            }

            // Plot curves for top 5 nations
            List<Map.Entry<String, Integer>> sorted = new ArrayList<>(s.territories.entrySet());
            sorted.sort((a, b) -> Integer.compare(b.getValue(), a.getValue()));

            int topCount = Math.min(5, sorted.size());
            int maxVal = 100;
            for (int i = 0; i < topCount; i++) {
                maxVal = Math.max(maxVal, sorted.get(i).getValue());
            }

            // Draw line steps across all snapshots
            for (int n = 0; n < topCount; n++) {
                String cName = sorted.get(n).getKey();
                Color cColor = NATION_COLORS[n % NATION_COLORS.length];
                batch.setColor(cColor);

                int prevPx = -1, prevPy = -1;
                for (int f = 0; f < snaps.size(); f++) {
                    TimelineRecorder.Snapshot snap = snaps.get(f);
                    int val = snap.territories.getOrDefault(cName, 0);

                    int px = graphX + (int)((float)f / Math.max(1, snaps.size() - 1) * (graphW - 10));
                    int py = graphY + graphH - (int)((float)val / maxVal * (graphH - 20)) - 10;

                    if (prevPx >= 0) {
                        // Draw segment
                        int dx = px - prevPx;
                        int dy = py - prevPy;
                        int steps = Math.max(Math.abs(dx), Math.abs(dy));
                        for (int st = 0; st <= steps; st++) {
                            int sx = prevPx + (st * dx) / Math.max(1, steps);
                            int sy = prevPy + (st * dy) / Math.max(1, steps);
                            pix.draw(batch, sx, sy, 2, 2);
                        }
                    }
                    prevPx = px;
                    prevPy = py;
                }
            }

            // Current frame vertical scrubber line
            int scrubX = graphX + (int)((float)currentFrame / Math.max(1, snaps.size() - 1) * (graphW - 10));
            batch.setColor(0.95f, 0.95f, 0.2f, 0.75f);
            pix.draw(batch, scrubX, graphY, 2, graphH);

            // 2. Right Side: Top Powers Leaderboard & Legend
            int leadX = graphX + graphW + CFG.s(20);
            int leadY = graphY;
            int leadW = W - leadX - CFG.s(30);

            batch.setColor(0.05f, 0.08f, 0.15f, 0.95f);
            pix.draw(batch, leadX, leadY, leadW, graphH);

            batch.setColor(0.2f, 0.5f, 0.85f, 0.8f);
            drawOutline(pix, batch, leadX, leadY, leadW, graphH, 2);

            if (CFG.fontSmall != null) {
                CFG.drawTextSmall(batch, "🏆 ЛИДЕРЫ МАТЧА:", leadX + CFG.s(16), leadY + CFG.s(14), Color.WHITE);

                int rowY = leadY + CFG.s(38);
                for (int i = 0; i < topCount; i++) {
                    Map.Entry<String, Integer> e = sorted.get(i);
                    Color cColor = NATION_COLORS[i % NATION_COLORS.length];

                    // Color indicator square
                    batch.setColor(cColor);
                    pix.draw(batch, leadX + CFG.s(16), rowY, CFG.s(12), CFG.s(12));

                    String info = (i + 1) + ". " + e.getKey() + ": " + e.getValue() + " армия";
                    CFG.drawTextSmall(batch, info, leadX + CFG.s(36), rowY - CFG.s(2), Color.WHITE);
                    rowY += CFG.s(26);
                }
            }

            // 3. Bottom: Accolades & Match Awards
            int awardY = H - CFG.s(146);
            int awardH = CFG.s(76);
            int awardW = W - CFG.s(60);

            batch.setColor(0.04f, 0.07f, 0.14f, 0.92f);
            pix.draw(batch, CFG.s(30), awardY, awardW, awardH);

            batch.setColor(0.95f, 0.75f, 0.15f, 0.85f);
            drawOutline(pix, batch, CFG.s(30), awardY, awardW, awardH, 1);

            if (CFG.fontSmall != null && !sorted.isEmpty()) {
                String mvp = sorted.get(0).getKey();
                String runnerUp = sorted.size() > 1 ? sorted.get(1).getKey() : "—";

                CFG.drawTextSmall(batch, "🎖️ НАГРАДЫ МАТЧА:", CFG.s(45), awardY + CFG.s(12), new Color(0.95f, 0.8f, 0.2f, 1.0f));
                CFG.drawTextSmall(batch, "👑 Император матча: " + mvp + " (Абсолютный лидер)", CFG.s(45), awardY + CFG.s(34), Color.WHITE);
                CFG.drawTextSmall(batch, "⚔️ Стальной маршал: " + runnerUp + " (Крупнейшие силы)", CFG.s(45), awardY + CFG.s(54), new Color(0.7f, 0.85f, 1.0f, 1.0f));
            }
        }

        if (this.menuPanel != null) {
            this.menuPanel.draw(this.batch);
        }

        this.batch.end();
    }

    private void drawOutline(Image pix, SpriteBatch batch, int x, int y, int w, int h, int th) {
        pix.draw(batch, x, y, w, th);
        pix.draw(batch, x, y + h - th, w, th);
        pix.draw(batch, x, y, th, h);
        pix.draw(batch, x + w - th, y, th, h);
    }

    @Override
    public void dispose() {
        this.menuPanel = null;
    }
}
