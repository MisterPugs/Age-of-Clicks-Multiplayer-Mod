package cgs.modloader.timeline;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.InputAdapter;
import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.utils.ScreenUtils;
import java.util.List;
import java.util.Map;
import ru.ius.david.cgs.CFG;
import ru.ius.david.cgs.cgsGame;
import ru.ius.david.cgs.elements.ButtonElement;
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

        int btnW = CFG.s(160);
        int btnH = CFG.s(44);
        int y = H - CFG.s(60);

        elements.add(new ButtonElement(CFG.s(30), y, btnW, btnH, isPlaying ? "ПАУЗА" : "ВОСПРОИЗВЕДЕНИЕ", CFG.COLOR_PARCHMENT_BTN_BG, CFG.COLOR_TEXT_DARK, new Runnable() {
            @Override
            public void run() {
                isPlaying = !isPlaying;
                buildUI();
            }
        }));

        elements.add(new ButtonElement(W - btnW - CFG.s(30), y, btnW, btnH, "ЗАКРЫТЬ", CFG.COLOR_TERTIARY_BTN, Color.WHITE, new Runnable() {
            @Override
            public void run() {
                game.showMainMenu();
            }
        }));

        this.menuPanel = new SliderMenu();
        this.menuPanel.initMenu(null, 0, 0, W, H, elements, true, false);
        this.menuPanel.setVisible(true);
    }

    @Override
    public void render(float delta) {
        ScreenUtils.clear(new Color(0.04f, 0.04f, 0.08f, 1.0f));

        List<TimelineRecorder.Snapshot> snaps = TimelineRecorder.getInstance().getSnapshots();
        if (isPlaying && !snaps.isEmpty()) {
            playTimer += delta;
            if (playTimer >= 0.5f) {
                playTimer = 0.0f;
                currentFrame = (currentFrame + 1) % snaps.size();
            }
        }

        this.batch.setProjectionMatrix(this.uiProjection);
        this.batch.begin();

        if (CFG.fontMain != null) {
            String title = "📈 ХРОНИКА ВОЙНЫ & ТАЙМЛАПС (TIMELINE REPLAY)";
            CFG.drawText(this.batch, title, CFG.s(30), CFG.s(40), new Color(0.95f, 0.75f, 0.2f, 1.0f));
        }

        if (snaps.isEmpty()) {
            if (CFG.fontSmall != null) {
                CFG.drawTextSmall(this.batch, "Снимков истории пока нет. Сыграйте несколько игровых месяцев!", CFG.s(30), CFG.s(80), Color.LIGHT_GRAY);
            }
        } else {
            TimelineRecorder.Snapshot s = snaps.get(Math.min(currentFrame, snaps.size() - 1));
            if (CFG.fontSmall != null) {
                String frameInfo = "Год: " + s.year + " | Активных войн: " + s.warsCount + " [Кадр " + (currentFrame + 1) + " / " + snaps.size() + "]";
                CFG.drawTextSmall(this.batch, frameInfo, CFG.s(30), CFG.s(70), Color.CYAN);

                int listY = CFG.s(110);
                CFG.drawTextSmall(this.batch, "ТОП ГОСУДАРСТВ ПО ЗЕМЛЯМ В ЭТОТ ПЕРИОД:", CFG.s(30), listY, Color.WHITE);
                listY += CFG.s(24);

                int count = 0;
                for (Map.Entry<String, Integer> e : s.territories.entrySet()) {
                    if (++count > 10) break;
                    String row = count + ". " + e.getKey() + ": " + e.getValue() + " px";
                    CFG.drawTextSmall(this.batch, row, CFG.s(30), listY, new Color(0.8f, 0.85f, 0.95f, 1.0f));
                    listY += CFG.s(22);
                }
            }
        }

        if (this.menuPanel != null) {
            this.menuPanel.draw(this.batch);
        }

        this.batch.end();
    }

    @Override
    public void dispose() {
        this.menuPanel = null;
    }
}
