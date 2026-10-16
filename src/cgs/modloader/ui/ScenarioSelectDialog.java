package cgs.modloader.ui;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputMultiplexer;
import com.badlogic.gdx.InputProcessor;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.MathUtils;
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

    private int activeFilterTab = 0; // 0 = All, 1 = Official, 2 = Steam Workshop
    private float animatedTabUnderlineX = 0f;
    private int pageIndex = 0;
    private static final int ITEMS_PER_PAGE = 5;
    private final GlyphLayout glyphLayout = new GlyphLayout();

    // Animation states
    private float openAnim = 0.0f;
    private float pulseTimer = 0.0f;

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
        this.openAnim = 0.0f;
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

    private long lastClickTime = 0;

    public boolean handleClick(float mouseX, float mouseY, float screenWidth, float screenHeight) {
        if (!visible) return false;

        long now = System.currentTimeMillis();
        if (now - lastClickTime < 90) return true;
        lastClickTime = now;

        float w = 780;
        float h = 540;
        float x = (screenWidth - w) / 2.0f;
        float y = (screenHeight - h) / 2.0f;

        // Click outside window closes dialog and consumes click
        if (mouseX < x || mouseX > x + w || mouseY < y || mouseY > y + h) {
            playClickSound();
            close();
            return true;
        }

        // Close button (at bottom)
        float closeBtnW = 190;
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
        float tabH = 34;
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
        float nextX = x + w - 138;
        if (mouseY >= pagingY && mouseY <= pagingY + 34) {
            if (mouseX >= prevX && mouseX <= prevX + 110) {
                playClickSound();
                pageIndex = Math.max(0, pageIndex - 1);
                return true;
            } else if (mouseX >= nextX && mouseX <= nextX + 110) {
                playClickSound();
                if (pageIndex + 1 < totalPages) {
                    pageIndex++;
                }
                return true;
            }
        }

        // Click on Scenario Item
        float itemStartY = y + 104;
        float itemH = 56;
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

    private String truncate(BitmapFont font, String text, float maxWidth) {
        if (text == null || font == null) return "";
        glyphLayout.setText(font, text);
        if (glyphLayout.width <= maxWidth) return text;
        String ellipsis = "...";
        glyphLayout.setText(font, ellipsis);
        float ellW = glyphLayout.width;
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < text.length(); i++) {
            sb.append(text.charAt(i));
            glyphLayout.setText(font, sb.toString());
            if (glyphLayout.width + ellW > maxWidth) {
                sb.setLength(Math.max(0, sb.length() - 1));
                return sb.toString() + ellipsis;
            }
        }
        return sb.toString() + ellipsis;
    }

    public void render(SpriteBatch batch, BitmapFont font, float screenWidth, float screenHeight) {
        if (!visible) return;
        ensureInputProcessorAttached();

        float delta = Gdx.graphics.getDeltaTime();
        pulseTimer += delta;
        openAnim = Math.min(1.0f, openAnim + delta * 5.0f);
        float ease = 1.0f - (float)Math.pow(1.0f - openAnim, 3);

        if (Gdx.input.isButtonJustPressed(Input.Buttons.LEFT)) {
            handleClick(Gdx.input.getX(), Gdx.input.getY(), screenWidth, screenHeight);
        }

        float w = 780;
        float h = 540;
        float x = (screenWidth - w) / 2.0f;
        float y = (screenHeight - h) / 2.0f;

        float mouseX = Gdx.input.getX();
        float mouseY = Gdx.input.getY();

        Image pix = ImageManager.getImage(Images.pix255_255_255);
        if (pix == null) return;

        // Dim veil with fade
        batch.setColor(0.0f, 0.0f, 0.0f, 0.55f * ease);
        pix.draw(batch, 0, 0, (int)screenWidth, (int)screenHeight);

        // Dialog background
        batch.setColor(0.04f, 0.07f, 0.12f, 0.98f * ease);
        pix.draw(batch, (int)x, (int)y, (int)w, (int)h);

        // Cyber scan line
        float scanY = y + ((pulseTimer * 80.0f) % h);
        batch.setColor(0.2f, 0.7f, 1.0f, 0.08f * ease);
        pix.draw(batch, (int)x, (int)scanY, (int)w, 3);

        // Header bar
        batch.setColor(0.07f, 0.18f, 0.32f, 1.0f * ease);
        pix.draw(batch, (int)x, (int)y, (int)w, 48);

        // Animated neon outer border
        float pulse = 0.85f + 0.15f * MathUtils.sin(pulseTimer * 3.0f);
        batch.setColor(0.2f * pulse, 0.7f * pulse, 1.0f * pulse, 0.95f * ease);
        drawOutline(pix, batch, (int)x, (int)y, (int)w, (int)h, 2);

        // Header bottom separator line
        batch.setColor(0.3f, 0.8f, 1.0f, 0.9f * ease);
        pix.draw(batch, (int)x, (int)(y + 48), (int)w, 2);

        // Cyber corner brackets
        float cLen = 14;
        batch.setColor(0.4f, 0.9f, 1.0f, 1.0f * ease);
        pix.draw(batch, (int)x, (int)y, (int)cLen, 3);
        pix.draw(batch, (int)x, (int)y, 3, (int)cLen);
        pix.draw(batch, (int)(x + w - cLen), (int)y, (int)cLen, 3);
        pix.draw(batch, (int)(x + w - 3), (int)y, 3, (int)cLen);
        pix.draw(batch, (int)x, (int)(y + h - 3), (int)cLen, 3);
        pix.draw(batch, (int)x, (int)(y + h - cLen), 3, (int)cLen);
        pix.draw(batch, (int)(x + w - cLen), (int)(y + h - 3), (int)cLen, 3);
        pix.draw(batch, (int)(x + w - 3), (int)(y + h - cLen), 3, (int)cLen);

        // Header Title
        if (font != null) {
            font.setColor(0.35f, 0.92f, 1.0f, ease);
            String title = "🗺️ ВЫБОР СЦЕНАРИЯ ДЛЯ СЕТЕВОГО МАТЧА";
            glyphLayout.setText(font, title);
            font.draw(batch, title, x + (w - glyphLayout.width) / 2.0f, y + 14);
        }

        // Filter Tabs: [ ВСЕ ], [ 🌟 ОФИЦИАЛЬНЫЕ ], [ 🛠️ STEAM WORKSHOP ]
        float tabY = y + 58;
        float tabW = (w - 56 - 20) / 3.0f;
        float tabH = 34;
        String[] tabLabels = new String[] { "🌐 ВСЕ СЦЕНАРИИ", "🌟 ОФИЦИАЛЬНЫЕ", "🛠️ STEAM WORKSHOP" };
        for (int t = 0; t < 3; t++) {
            float tx = x + 28 + t * (tabW + 10);
            boolean active = (activeFilterTab == t);
            boolean tabHover = (mouseX >= tx && mouseX <= tx + tabW && mouseY >= tabY && mouseY <= tabY + tabH);

            if (active) {
                batch.setColor(0.16f, 0.32f, 0.5f, 1.0f * ease);
            } else if (tabHover) {
                batch.setColor(0.12f, 0.22f, 0.36f, 1.0f * ease);
            } else {
                batch.setColor(0.08f, 0.14f, 0.22f, 1.0f * ease);
            }
            pix.draw(batch, (int)tx, (int)tabY, (int)tabW, (int)tabH);

            // Tab border
            if (active) {
                batch.setColor(0.4f, 0.85f, 1.0f, 1.0f * ease);
                drawOutline(pix, batch, (int)tx, (int)tabY, (int)tabW, (int)tabH, 2);
                // Glowing underline
                batch.setColor(0.4f, 0.95f, 1.0f, pulse * ease);
                pix.draw(batch, (int)tx, (int)(tabY + tabH - 3), (int)tabW, 3);
            } else if (tabHover) {
                batch.setColor(0.3f, 0.65f, 0.85f, 0.8f * ease);
                drawOutline(pix, batch, (int)tx, (int)tabY, (int)tabW, (int)tabH, 1);
            } else {
                batch.setColor(0.2f, 0.35f, 0.5f, 0.6f * ease);
                drawOutline(pix, batch, (int)tx, (int)tabY, (int)tabW, (int)tabH, 1);
            }

            if (font != null) {
                font.setColor(active ? Color.WHITE : (tabHover ? Color.WHITE : Color.LIGHT_GRAY));
                glyphLayout.setText(font, tabLabels[t]);
                font.draw(batch, tabLabels[t], tx + (tabW - glyphLayout.width) / 2.0f, tabY + 9);
            }
        }

        List<Integer> filtered = getFilteredIndices();
        int totalPages = Math.max(1, (filtered.size() + ITEMS_PER_PAGE - 1) / ITEMS_PER_PAGE);
        int startIndex = pageIndex * ITEMS_PER_PAGE;

        // Render Scenario Items
        float itemStartY = y + 104;
        float itemH = 56;
        for (int i = 0; i < ITEMS_PER_PAGE; i++) {
            int entryIdx = startIndex + i;
            if (entryIdx >= filtered.size()) break;
            int globalIdx = filtered.get(entryIdx);
            ScenarioData sd = allScenarios.get(globalIdx);

            float iy = itemStartY + i * (itemH + 6);
            boolean isCur = (globalIdx == selectedScenarioIndex);
            boolean isHover = (mouseX >= x + 28 && mouseX <= x + w - 28 && mouseY >= iy && mouseY <= iy + itemH);

            // Item card background
            if (isCur) {
                batch.setColor(0.14f, 0.28f, 0.44f, 1.0f * ease);
            } else if (isHover) {
                batch.setColor(0.11f, 0.2f, 0.32f, 1.0f * ease);
            } else {
                batch.setColor(0.06f, 0.11f, 0.18f, 1.0f * ease);
            }
            pix.draw(batch, (int)(x + 28), (int)iy, (int)(w - 56), (int)itemH);

            // Item outline
            if (isCur) {
                batch.setColor(1.0f, 0.85f, 0.3f, 1.0f * ease);
                drawOutline(pix, batch, (int)(x + 28), (int)iy, (int)(w - 56), (int)itemH, 2);
                // Left accent bar
                pix.draw(batch, (int)(x + 28), (int)iy, 5, (int)itemH);
            } else if (isHover) {
                batch.setColor(0.35f, 0.8f, 1.0f, 0.9f * ease);
                drawOutline(pix, batch, (int)(x + 28), (int)iy, (int)(w - 56), (int)itemH, 1);
            } else {
                batch.setColor(0.2f, 0.4f, 0.6f, 0.7f * ease);
                drawOutline(pix, batch, (int)(x + 28), (int)iy, (int)(w - 56), (int)itemH, 1);
            }

            boolean isWorkshop = (sd.steamPublishedId != null && !sd.steamPublishedId.isEmpty()) || ScenarioFileResolver.isWorkshopPath(sd.folderPath);
            String badge = isWorkshop ? "[🛠️ WORKSHOP]" : "[🌟 ОФИЦИАЛЬНЫЙ]";
            Color badgeColor = isWorkshop ? new Color(1.0f, 0.75f, 0.25f, ease) : new Color(0.4f, 0.9f, 1.0f, ease);

            if (font != null) {
                // 1. Measure badge on the FAR RIGHT
                glyphLayout.setText(font, badge);
                float badgeW = glyphLayout.width;
                float badgeX = x + w - 28 - badgeW - 18;

                // Badge background pill
                batch.setColor(isWorkshop ? 0.3f : 0.1f, isWorkshop ? 0.2f : 0.25f, isWorkshop ? 0.05f : 0.35f, 0.9f * ease);
                pix.draw(batch, (int)(badgeX - 8), (int)(iy + 13), (int)(badgeW + 16), 28);
                batch.setColor(badgeColor);
                drawOutline(pix, batch, (int)(badgeX - 8), (int)(iy + 13), (int)(badgeW + 16), 28, 1);

                // Badge text
                font.setColor(badgeColor);
                font.draw(batch, badge, badgeX, iy + 18);

                // 2. Scenario Title on the LEFT (Safe area from x+42 to badgeX - 25)
                float maxTitleW = badgeX - (x + 42) - 25;
                font.setColor(isCur ? new Color(1.0f, 0.9f, 0.3f, ease) : (isHover ? Color.WHITE : new Color(0.9f, 0.95f, 1.0f, ease)));
                String nameRaw = (entryIdx + 1) + ". " + sd.name + (sd.year > 0 ? " (" + sd.year + " г.)" : "");
                String nameText = truncate(font, nameRaw, maxTitleW);
                font.draw(batch, nameText, x + 42, iy + 12);

                // 3. Scenario Path / ID on line 2 (NO overlapping!)
                font.setColor(0.6f, 0.72f, 0.85f, ease);
                String subRaw = isWorkshop ? ("ID: " + (sd.steamPublishedId != null ? sd.steamPublishedId : ScenarioFileResolver.workshopItemId(sd.folderPath)) + "  •  " + sd.folderPath) : ("Папка: " + sd.folderPath);
                String subText = truncate(font, subRaw, maxTitleW);
                font.draw(batch, subText, x + 42, iy + 34);
            }
        }

        // Paging Controls
        float pagingY = y + h - 96;
        float prevX = x + 28;
        float nextX = x + w - 138;
        boolean prevHover = (mouseX >= prevX && mouseX <= prevX + 110 && mouseY >= pagingY && mouseY <= pagingY + 34);
        boolean nextHover = (mouseX >= nextX && mouseX <= nextX + 110 && mouseY >= pagingY && mouseY <= pagingY + 34);

        // Prev Button
        batch.setColor(prevHover ? 0.16f : 0.1f, prevHover ? 0.28f : 0.18f, prevHover ? 0.44f : 0.28f, 1.0f * ease);
        pix.draw(batch, (int)prevX, (int)pagingY, 110, 34);
        batch.setColor(prevHover ? 0.4f : 0.25f, prevHover ? 0.8f : 0.5f, prevHover ? 1.0f : 0.75f, 0.9f * ease);
        drawOutline(pix, batch, (int)prevX, (int)pagingY, 110, 34, 1);

        // Next Button
        batch.setColor(nextHover ? 0.16f : 0.1f, nextHover ? 0.28f : 0.18f, nextHover ? 0.44f : 0.28f, 1.0f * ease);
        pix.draw(batch, (int)nextX, (int)pagingY, 110, 34);
        batch.setColor(nextHover ? 0.4f : 0.25f, nextHover ? 0.8f : 0.5f, nextHover ? 1.0f : 0.75f, 0.9f * ease);
        drawOutline(pix, batch, (int)nextX, (int)pagingY, 110, 34, 1);

        if (font != null) {
            font.setColor(prevHover ? Color.WHITE : Color.LIGHT_GRAY);
            glyphLayout.setText(font, "◀ Назад");
            font.draw(batch, "◀ Назад", prevX + (110 - glyphLayout.width) / 2.0f, pagingY + 9);

            font.setColor(nextHover ? Color.WHITE : Color.LIGHT_GRAY);
            glyphLayout.setText(font, "Вперёд ▶");
            font.draw(batch, "Вперёд ▶", nextX + (110 - glyphLayout.width) / 2.0f, pagingY + 9);

            String pageInfo = "Страница " + (pageIndex + 1) + " из " + totalPages + " (Всего карт: " + filtered.size() + ")";
            font.setColor(0.7f, 0.85f, 1.0f, ease);
            glyphLayout.setText(font, pageInfo);
            font.draw(batch, pageInfo, x + (w - glyphLayout.width) / 2.0f, pagingY + 9);
        }

        // Close Button
        float closeBtnW = 190;
        float closeBtnH = 38;
        float closeBtnX = x + (w - closeBtnW) / 2.0f;
        float closeBtnY = y + h - 50;
        boolean closeHover = (mouseX >= closeBtnX && mouseX <= closeBtnX + closeBtnW && mouseY >= closeBtnY && mouseY <= closeBtnY + closeBtnH);

        batch.setColor(closeHover ? 0.45f : 0.3f, closeHover ? 0.14f : 0.1f, closeHover ? 0.14f : 0.1f, 1.0f * ease);
        pix.draw(batch, (int)closeBtnX, (int)closeBtnY, (int)closeBtnW, (int)closeBtnH);
        batch.setColor(closeHover ? 1.0f : 0.85f, closeHover ? 0.35f : 0.25f, closeHover ? 0.35f : 0.25f, 0.95f * ease);
        drawOutline(pix, batch, (int)closeBtnX, (int)closeBtnY, (int)closeBtnW, (int)closeBtnH, closeHover ? 2 : 1);

        if (font != null) {
            font.setColor(closeHover ? Color.WHITE : new Color(1.0f, 0.85f, 0.85f, ease));
            String closeText = "ЗАКРЫТЬ (ESC)";
            glyphLayout.setText(font, closeText);
            font.draw(batch, closeText, closeBtnX + (closeBtnW - glyphLayout.width) / 2.0f, closeBtnY + 11);
        }

        batch.setColor(Color.WHITE);
    }

    private void drawOutline(Image pix, SpriteBatch batch, int x, int y, int w, int h, int thickness) {
        pix.draw(batch, x, y, w, thickness);
        pix.draw(batch, x, y + h - thickness, w, thickness);
        pix.draw(batch, x, y, thickness, h);
        pix.draw(batch, x + w - thickness, y, thickness, h);
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
    public boolean mouseMoved(int screenX, int screenY) { return false; }
    @Override
    public boolean scrolled(float amountX, float amountY) { return visible; }
}
