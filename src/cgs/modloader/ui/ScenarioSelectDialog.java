package cgs.modloader.ui;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputMultiplexer;
import com.badlogic.gdx.InputProcessor;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import ru.ius.david.cgs.CFG;
import ru.ius.david.cgs.ScenarioData;
import ru.ius.david.cgs.ScenarioFileResolver;
import ru.ius.david.cgs.images.Image;
import ru.ius.david.cgs.images.ImageManager;
import ru.ius.david.cgs.images.Images;
import ru.ius.david.cgs.managers.SoundManager;

import java.util.ArrayList;
import java.util.List;

public class ScenarioSelectDialog implements InputProcessor {
    private static final ScenarioSelectDialog INSTANCE = new ScenarioSelectDialog();
    public static ScenarioSelectDialog getInstance() { return INSTANCE; }

    public interface ScenarioSelectionCallback {
        void onScenarioSelected(int globalIndex, ScenarioData scenario);
    }

    private boolean visible = false;
    private List<ScenarioData> allScenarios = new ArrayList<>();
    private int selectedScenarioIndex = 0;
    private ScenarioSelectionCallback callback;

    private int activeFilterTab = 0; // 0 = All, 1 = Vanilla/Official, 2 = Steam Workshop
    private int pageIndex = 0;
    private static final int ITEMS_PER_PAGE = 5;
    private final GlyphLayout glyphLayout = new GlyphLayout();

    private ScenarioSelectDialog() {}

    public boolean isVisible() {
        return visible;
    }

    public void open(List<ScenarioData> scenarios, int currentIndex, ScenarioSelectionCallback callback) {
        this.allScenarios = scenarios != null ? scenarios : new ArrayList<>();
        this.selectedScenarioIndex = Math.max(0, currentIndex);
        this.callback = callback;
        this.visible = true;
        this.activeFilterTab = 0;
        this.pageIndex = 0;
        ensureInputProcessorAttached();
    }

    public void close() {
        this.visible = false;
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

    private List<Integer> getFilteredIndices() {
        List<Integer> list = new ArrayList<>();
        for (int i = 0; i < allScenarios.size(); i++) {
            ScenarioData sd = allScenarios.get(i);
            boolean isWorkshop = (sd.steamPublishedId != null && !sd.steamPublishedId.isEmpty()) || ScenarioFileResolver.isWorkshopPath(sd.folderPath);
            if (activeFilterTab == 1 && isWorkshop) continue; // Official only
            if (activeFilterTab == 2 && !isWorkshop) continue; // Workshop only
            list.add(i);
        }
        return list;
    }

    public boolean handleClick(float mouseX, float mouseY, float screenWidth, float screenHeight) {
        if (!visible) return false;

        float w = 680;
        float h = 500;
        float x = (screenWidth - w) / 2.0f;
        float y = (screenHeight - h) / 2.0f;

        // Click outside window closes dialog and consumes click
        if (mouseX < x || mouseX > x + w || mouseY < y || mouseY > y + h) {
            playClickSound();
            close();
            return true;
        }

        // Close button (at bottom)
        float closeBtnW = 180;
        float closeBtnH = 38;
        float closeBtnX = x + (w - closeBtnW) / 2.0f;
        float closeBtnY = y + h - 50;
        if (mouseX >= closeBtnX && mouseX <= closeBtnX + closeBtnW && mouseY >= closeBtnY && mouseY <= closeBtnY + closeBtnH) {
            playClickSound();
            close();
            return true;
        }

        // Filter Tabs
        float tabY = y + 58;
        float tabW = (w - 56 - 20) / 3.0f;
        float tabH = 32;
        for (int t = 0; t < 3; t++) {
            float tx = x + 28 + t * (tabW + 10);
            if (mouseX >= tx && mouseX <= tx + tabW && mouseY >= tabY && mouseY <= tabY + tabH) {
                playClickSound();
                activeFilterTab = t;
                pageIndex = 0;
                return true;
            }
        }

        List<Integer> filtered = getFilteredIndices();
        int totalPages = Math.max(1, (filtered.size() + ITEMS_PER_PAGE - 1) / ITEMS_PER_PAGE);

        // Paging buttons
        float pagingY = y + h - 96;
        float prevX = x + 28;
        float nextX = x + w - 128;
        if (mouseY >= pagingY && mouseY <= pagingY + 34) {
            if (mouseX >= prevX && mouseX <= prevX + 100) {
                playClickSound();
                pageIndex = Math.max(0, pageIndex - 1);
                return true;
            } else if (mouseX >= nextX && mouseX <= nextX + 100) {
                playClickSound();
                if (pageIndex + 1 < totalPages) {
                    pageIndex++;
                }
                return true;
            }
        }

        // Click on Scenario Item
        float itemStartY = y + 102;
        float itemH = 54;
        int startIndex = pageIndex * ITEMS_PER_PAGE;
        for (int i = 0; i < ITEMS_PER_PAGE; i++) {
            int entryIdx = startIndex + i;
            if (entryIdx >= filtered.size()) break;
            int globalIdx = filtered.get(entryIdx);
            ScenarioData sd = allScenarios.get(globalIdx);

            float iy = itemStartY + i * (itemH + 6);
            if (mouseX >= x + 28 && mouseX <= x + w - 28 && mouseY >= iy && mouseY <= iy + itemH) {
                playClickSound();
                selectedScenarioIndex = globalIdx;
                if (callback != null) {
                    callback.onScenarioSelected(globalIdx, sd);
                }
                close();
                return true;
            }
        }

        return true;
    }

    private void playClickSound() {
        SoundManager sm = SoundManager.getInstance();
        if (sm != null) {
            try {
                sm.play(SoundManager.SFX.BUTTON_CLICK, 0.8f);
            } catch (Exception ignored) {}
        }
    }

    public void render(SpriteBatch batch, BitmapFont font, float screenWidth, float screenHeight) {
        if (!visible) return;
        ensureInputProcessorAttached();

        float w = 680;
        float h = 500;
        float x = (screenWidth - w) / 2.0f;
        float y = (screenHeight - h) / 2.0f;

        Image pix = ImageManager.getImage(Images.pix255_255_255);
        if (pix == null) return;

        // Dim veil
        batch.setColor(0.0f, 0.0f, 0.0f, 0.5f);
        pix.draw(batch, 0, 0, (int)screenWidth, (int)screenHeight);

        // Dialog background
        batch.setColor(0.05f, 0.08f, 0.14f, 0.98f);
        pix.draw(batch, (int)x, (int)y, (int)w, (int)h);

        // Header bar
        batch.setColor(0.08f, 0.22f, 0.38f, 1.0f);
        pix.draw(batch, (int)x, (int)y, (int)w, 46);

        // Outer Cyber border
        batch.setColor(0.2f, 0.65f, 0.95f, 1.0f);
        drawOutline(pix, batch, (int)x, (int)y, (int)w, (int)h);

        // Header Title
        if (font != null) {
            font.setColor(0.35f, 0.9f, 1.0f, 1.0f);
            String title = "🗺️ ВЫБОР СЦЕНАРИЯ ДЛЯ СЕТЕВОГО МАТЧА";
            glyphLayout.setText(font, title);
            font.draw(batch, title, x + (w - glyphLayout.width) / 2.0f, y + 14);
        }

        // Filter Tabs: [ ВСЕ ], [ ОФИЦИАЛЬНЫЕ ], [ 🛠️ WORKSHOP ]
        float tabY = y + 58;
        float tabW = (w - 56 - 20) / 3.0f;
        float tabH = 32;
        String[] tabLabels = new String[] { "ВСЕ СЦЕНАРИИ", "🌟 ОФИЦИАЛЬНЫЕ", "🛠️ STEAM WORKSHOP" };
        for (int t = 0; t < 3; t++) {
            float tx = x + 28 + t * (tabW + 10);
            boolean active = (activeFilterTab == t);
            batch.setColor(active ? 0.18f : 0.09f, active ? 0.35f : 0.16f, active ? 0.52f : 0.26f, 1.0f);
            pix.draw(batch, (int)tx, (int)tabY, (int)tabW, (int)tabH);
            batch.setColor(active ? 0.3f : 0.18f, active ? 0.8f : 0.45f, active ? 1.0f : 0.65f, 0.8f);
            drawOutline(pix, batch, (int)tx, (int)tabY, (int)tabW, (int)tabH);

            if (font != null) {
                font.setColor(active ? Color.WHITE : Color.LIGHT_GRAY);
                glyphLayout.setText(font, tabLabels[t]);
                font.draw(batch, tabLabels[t], tx + (tabW - glyphLayout.width) / 2.0f, tabY + 9);
            }
        }

        List<Integer> filtered = getFilteredIndices();
        int totalPages = Math.max(1, (filtered.size() + ITEMS_PER_PAGE - 1) / ITEMS_PER_PAGE);
        int startIndex = pageIndex * ITEMS_PER_PAGE;

        // Render Scenario Items
        float itemStartY = y + 102;
        float itemH = 54;
        for (int i = 0; i < ITEMS_PER_PAGE; i++) {
            int entryIdx = startIndex + i;
            if (entryIdx >= filtered.size()) break;
            int globalIdx = filtered.get(entryIdx);
            ScenarioData sd = allScenarios.get(globalIdx);

            float iy = itemStartY + i * (itemH + 6);
            boolean isCur = (globalIdx == selectedScenarioIndex);

            // Item card background
            batch.setColor(isCur ? 0.15f : 0.08f, isCur ? 0.28f : 0.14f, isCur ? 0.42f : 0.22f, 1.0f);
            pix.draw(batch, (int)(x + 28), (int)iy, (int)(w - 56), (int)itemH);
            batch.setColor(isCur ? 0.95f : 0.22f, isCur ? 0.85f : 0.5f, isCur ? 0.3f : 0.75f, 0.9f);
            drawOutline(pix, batch, (int)(x + 28), (int)iy, (int)(w - 56), (int)itemH);

            boolean isWorkshop = (sd.steamPublishedId != null && !sd.steamPublishedId.isEmpty()) || ScenarioFileResolver.isWorkshopPath(sd.folderPath);
            String badge = isWorkshop ? "[🛠️ WORKSHOP]" : "[🌟 ОФИЦИАЛЬНЫЙ]";
            Color badgeColor = isWorkshop ? new Color(0.95f, 0.65f, 0.2f, 1.0f) : new Color(0.35f, 0.85f, 1.0f, 1.0f);

            if (font != null) {
                // Scenario Title
                font.setColor(isCur ? Color.YELLOW : Color.WHITE);
                String nameText = (entryIdx + 1) + ". " + sd.name + (sd.year > 0 ? " (" + sd.year + " г.)" : "");
                font.draw(batch, nameText, x + 40, iy + 10);

                // Badge & Path
                font.setColor(badgeColor);
                font.draw(batch, badge, x + 40, iy + 30);

                font.setColor(0.65f, 0.75f, 0.85f, 1.0f);
                String sub = isWorkshop ? ("ID: " + (sd.steamPublishedId != null ? sd.steamPublishedId : ScenarioFileResolver.workshopItemId(sd.folderPath))) : sd.folderPath;
                font.draw(batch, sub, x + 180, iy + 30);
            }
        }

        // Paging Controls
        float pagingY = y + h - 96;
        batch.setColor(0.12f, 0.2f, 0.32f, 1.0f);
        pix.draw(batch, (int)(x + 28), (int)pagingY, 100, 34);
        pix.draw(batch, (int)(x + w - 128), (int)pagingY, 100, 34);
        batch.setColor(0.3f, 0.6f, 0.9f, 0.8f);
        drawOutline(pix, batch, (int)(x + 28), (int)pagingY, 100, 34);
        drawOutline(pix, batch, (int)(x + w - 128), (int)pagingY, 100, 34);

        if (font != null) {
            font.setColor(Color.WHITE);
            font.draw(batch, "◀ Назад", x + 44, pagingY + 9);
            font.draw(batch, "Вперёд ▶", x + w - 114, pagingY + 9);

            String pageInfo = "Страница " + (pageIndex + 1) + " из " + totalPages + " (Всего: " + filtered.size() + ")";
            glyphLayout.setText(font, pageInfo);
            font.draw(batch, pageInfo, x + (w - glyphLayout.width) / 2.0f, pagingY + 9);
        }

        // Close Button
        float closeBtnW = 180;
        float closeBtnH = 38;
        float closeBtnX = x + (w - closeBtnW) / 2.0f;
        float closeBtnY = y + h - 50;
        batch.setColor(0.35f, 0.12f, 0.12f, 1.0f);
        pix.draw(batch, (int)closeBtnX, (int)closeBtnY, (int)closeBtnW, (int)closeBtnH);
        batch.setColor(0.9f, 0.3f, 0.3f, 0.9f);
        drawOutline(pix, batch, (int)closeBtnX, (int)closeBtnY, (int)closeBtnW, (int)closeBtnH);

        if (font != null) {
            font.setColor(1.0f, 0.85f, 0.85f, 1.0f);
            String closeText = "ЗАКРЫТЬ (ESC)";
            glyphLayout.setText(font, closeText);
            font.draw(batch, closeText, closeBtnX + (closeBtnW - glyphLayout.width) / 2.0f, closeBtnY + 11);
        }

        batch.setColor(Color.WHITE);
    }

    private void drawOutline(Image pix, SpriteBatch batch, int x, int y, int w, int h) {
        pix.draw(batch, x, y, w, 2);
        pix.draw(batch, x, y + h - 2, w, 2);
        pix.draw(batch, x, y, 2, h);
        pix.draw(batch, x + w - 2, y, 2, h);
    }

    @Override
    public boolean keyDown(int keycode) {
        if (!visible) return false;
        if (keycode == Input.Keys.ESCAPE) {
            playClickSound();
            close();
            return true;
        }
        return true;
    }

    @Override
    public boolean keyUp(int keycode) { return visible; }
    @Override
    public boolean keyTyped(char character) { return visible; }

    @Override
    public boolean touchDown(int screenX, int screenY, int pointer, int button) {
        if (!visible) return false;
        handleClick(screenX, screenY, CFG.GAME_WIDTH, CFG.GAME_HEIGHT);
        return true;
    }

    @Override
    public boolean touchCancelled(int screenX, int screenY, int pointer, int button) { return visible; }
    @Override
    public boolean touchUp(int screenX, int screenY, int pointer, int button) { return visible; }
    @Override
    public boolean touchDragged(int screenX, int screenY, int pointer) { return visible; }
    @Override
    public boolean mouseMoved(int screenX, int screenY) { return visible; }
    @Override
    public boolean scrolled(float amountX, float amountY) { return visible; }
}
