package cgs.modloader.ui;

import cgs.modloader.ModContainer;
import cgs.modloader.ModLoader;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputAdapter;
import com.badlogic.gdx.InputProcessor;
import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.utils.ScreenUtils;
import java.util.List;
import ru.ius.david.cgs.CFG;
import ru.ius.david.cgs.cgsGame;
import ru.ius.david.cgs.elements.ButtonElement;
import ru.ius.david.cgs.elements.MenuElement;
import ru.ius.david.cgs.game.LanguageManager;
import ru.ius.david.cgs.images.ImageManager;
import ru.ius.david.cgs.images.Images;
import ru.ius.david.cgs.menus.SliderMenu;

public class EnhancedModManagerScreen extends ScreenAdapter {
    private final cgsGame game;
    private final SpriteBatch batch;
    private Matrix4 uiProjection;
    private SliderMenu menuPanel;
    private float scrollOffset = 0.0f;
    private float maxScrollOffset = 0.0f;
    private int panelX, panelY, panelW, panelH;

    private static final Color BG_COLOR = new Color(0.04f, 0.04f, 0.1f, 1.0f);
    private static final Color CARD_BG = new Color(0.07f, 0.09f, 0.16f, 0.95f);
    private static final Color CARD_BORDER = new Color(0.18f, 0.28f, 0.45f, 0.8f);
    private static final Color ACTIVE_GREEN = new Color(0.2f, 0.85f, 0.35f, 1.0f);
    private static final Color DISABLED_GRAY = new Color(0.55f, 0.55f, 0.6f, 1.0f);
    private static final Color TITLE_COLOR = new Color(0.95f, 0.75f, 0.2f, 1.0f);
    private static final Color TEXT_CYBER = new Color(0.7f, 0.85f, 0.95f, 1.0f);

    public EnhancedModManagerScreen(cgsGame game) {
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

        this.panelW = Math.min(CFG.s(720), W - CFG.s(40));
        this.panelH = Math.min(CFG.s(600), H - CFG.s(60));
        this.panelX = (W - this.panelW) / 2;
        this.panelY = (H - this.panelH) / 2;

        boolean isRus = LanguageManager.getLangSuffix().equalsIgnoreCase("Rus");
        java.util.ArrayList<MenuElement> elements = new java.util.ArrayList<MenuElement>();

        int btnH = CFG.s(44);
        int pad = CFG.s(20);
        int gap = CFG.s(12);
        int btnY = this.panelY + this.panelH - pad - btnH;
        int btnW = (this.panelW - pad * 2 - gap * 2) / 3;

        // Button 1: Open Mods Folder
        String b1Text = isRus ? "ПАПКА МОДОВ" : "MODS FOLDER";
        ButtonElement b1 = new ButtonElement(this.panelX + pad, btnY, btnW, btnH, b1Text, CFG.COLOR_PARCHMENT_BTN_BG, CFG.COLOR_TEXT_DARK, new Runnable() {
            @Override
            public void run() {
                ModLoader.getInstance().openModsFolder();
            }
        });
        b1.setIcon(Images.ic_folder);
        elements.add(b1);

        // Button 2: Rescan / Refresh
        String b2Text = isRus ? "ОБНОВИТЬ" : "REFRESH";
        ButtonElement b2 = new ButtonElement(this.panelX + pad + btnW + gap, btnY, btnW, btnH, b2Text, CFG.COLOR_PARCHMENT_BTN_BG, CFG.COLOR_TEXT_DARK, new Runnable() {
            @Override
            public void run() {
                ModLoader.getInstance().init();
                buildUI();
                setupInput();
            }
        });
        b2.setIcon(Images.ic_menu_tutorial);
        elements.add(b2);

        // Button 3: Back to Main Menu
        String b3Text = isRus ? "НАЗАД" : "BACK";
        ButtonElement b3 = new ButtonElement(this.panelX + pad + (btnW + gap) * 2, btnY, btnW, btnH, b3Text, CFG.COLOR_TERTIARY_BTN, Color.WHITE, new Runnable() {
            @Override
            public void run() {
                game.showMainMenu();
            }
        });
        b3.setIcon(Images.ic_menu_exit);
        elements.add(b3);

        // Button 4: Timeline & Replay
        int timeW = CFG.s(150);
        int timeH = CFG.s(36);
        String bTimeText = isRus ? "ХРОНИКА (REPLAY)" : "TIMELINE (REPLAY)";
        ButtonElement bTimeline = new ButtonElement(this.panelX + this.panelW - pad - timeW - CFG.s(40), this.panelY + pad + CFG.s(4), timeW, timeH, bTimeText, CFG.COLOR_PARCHMENT_BTN_BG, CFG.COLOR_TEXT_DARK, new Runnable() {
            @Override
            public void run() {
                game.setScreen(new cgs.modloader.timeline.TimelineViewerScreen(game));
            }
        });
        bTimeline.setIcon(Images.ic_menu_tutorial);
        elements.add(bTimeline);

        // Mod cards toggle buttons
        List<ModContainer> mods = ModLoader.getInstance().getLoadedMods();
        int cardH = CFG.s(76);
        int startY = this.panelY + pad + CFG.s(50);

        for (int i = 0; i < mods.size(); ++i) {
            final ModContainer mc = mods.get(i);
            int cardY = startY + i * (cardH + CFG.s(8));
            int toggleW = CFG.s(110);
            int toggleH = CFG.s(36);
            int toggleX = this.panelX + this.panelW - pad - toggleW - CFG.s(10);
            int toggleY = cardY + (cardH - toggleH) / 2;

            final boolean isEnabled = mc.metadata.enabled;
            String toggleText = isEnabled ? (isRus ? "ОТКЛЮЧИТЬ" : "DISABLE") : (isRus ? "ВКЛЮЧИТЬ" : "ENABLE");
            Color toggleBg = isEnabled ? CFG.COLOR_TERTIARY_BTN : ACTIVE_GREEN;
            Color toggleTxt = Color.WHITE;

            elements.add(new ButtonElement(toggleX, toggleY, toggleW, toggleH, toggleText, toggleBg, toggleTxt, new Runnable() {
                @Override
                public void run() {
                    ModLoader.getInstance().setModEnabled(mc.metadata.id, !isEnabled);
                    buildUI();
                    setupInput();
                }
            }));
        }

        this.menuPanel = new SliderMenu();
        this.menuPanel.initMenu(null, 0, 0, W, H, elements, true, false);
        this.menuPanel.setVisible(true);
    }

    private void setupInput() {
        Gdx.input.setInputProcessor(new InputAdapter() {
            @Override
            public boolean keyDown(int keycode) {
                if (keycode == Input.Keys.ESCAPE || keycode == Input.Keys.BACK) {
                    game.showMainMenu();
                    return true;
                }
                return false;
            }

            @Override
            public boolean touchDown(int screenX, int screenY, int pointer, int button) {
                if (menuPanel != null) {
                    menuPanel.actionPressDown(screenX, screenY);
                }
                return true;
            }

            @Override
            public boolean touchUp(int screenX, int screenY, int pointer, int button) {
                int closeBtnSize = CFG.s(32);
                int closeBtnX = panelX + panelW - CFG.s(20) - closeBtnSize;
                int closeBtnY = panelY + CFG.s(6);

                // 1. Click on Close [X] button
                if (screenX >= closeBtnX && screenX <= closeBtnX + closeBtnSize && screenY >= closeBtnY && screenY <= closeBtnY + closeBtnSize) {
                    game.showMainMenu();
                    return true;
                }

                // 2. Click outside modal panel
                if (screenX < panelX || screenX > panelX + panelW || screenY < panelY || screenY > panelY + panelH) {
                    game.showMainMenu();
                    return true;
                }

                if (menuPanel != null) {
                    menuPanel.actionPressUp(screenX, screenY);
                }
                return true;
            }

            @Override
            public boolean mouseMoved(int screenX, int screenY) {
                if (menuPanel != null && menuPanel.getMenuElements() != null) {
                    for (MenuElement el : menuPanel.getMenuElements()) {
                        if (el != null) {
                            boolean hov = screenX >= el.getPosX() && screenX <= el.getPosX() + el.getWidth()
                                       && screenY >= el.getPosY() && screenY <= el.getPosY() + el.getHeight();
                            el.setIsHovered(hov);
                        }
                    }
                }
                return false;
            }
        });
    }

    @Override
    public void show() {
        setupInput();
    }

    @Override
    public void render(float delta) {
        ScreenUtils.clear(BG_COLOR);

        this.batch.setProjectionMatrix(this.uiProjection);
        this.batch.begin();

        // 1. Draw main container panel
        this.batch.setColor(CARD_BG);
        ImageManager.getImage(Images.pix255_255_255).draw(this.batch, this.panelX, this.panelY, this.panelW, this.panelH);

        this.batch.setColor(CARD_BORDER);
        ImageManager.getImage(Images.pix255_255_255).draw(this.batch, this.panelX, this.panelY, this.panelW, 2);
        ImageManager.getImage(Images.pix255_255_255).draw(this.batch, this.panelX, this.panelY + this.panelH - 2, this.panelW, 2);
        ImageManager.getImage(Images.pix255_255_255).draw(this.batch, this.panelX, this.panelY, 2, this.panelH);
        ImageManager.getImage(Images.pix255_255_255).draw(this.batch, this.panelX + this.panelW - 2, this.panelY, 2, this.panelH);

        boolean isRus = LanguageManager.getLangSuffix().equalsIgnoreCase("Rus");

        // 2. Draw Header & Close [X] Button
        int pad = CFG.s(20);
        if (CFG.fontMain != null) {
            String title = isRus ? "⚡ МЕНЕДЖЕР МОДОВ (CGS MODLOADER v1.0)" : "⚡ MOD MANAGER (CGS MODLOADER v1.0)";
            CFG.drawText(this.batch, title, this.panelX + pad, this.panelY + pad + CFG.s(10), TITLE_COLOR);
        }

        // Close [X] button
        int closeBtnSize = CFG.s(32);
        int closeBtnX = this.panelX + this.panelW - CFG.s(20) - closeBtnSize;
        int closeBtnY = this.panelY + CFG.s(6);
        int mx = Gdx.input.getX();
        int my = Gdx.input.getY();
        boolean isCloseHover = (mx >= closeBtnX && mx <= closeBtnX + closeBtnSize && my >= closeBtnY && my <= closeBtnY + closeBtnSize);

        this.batch.setColor(isCloseHover ? new Color(0.85f, 0.22f, 0.25f, 1.0f) : new Color(0.25f, 0.12f, 0.15f, 0.9f));
        ImageManager.getImage(Images.pix255_255_255).draw(this.batch, closeBtnX, closeBtnY, closeBtnSize, closeBtnSize);
        this.batch.setColor(isCloseHover ? Color.WHITE : new Color(0.7f, 0.3f, 0.35f, 1.0f));
        ImageManager.getImage(Images.pix255_255_255).draw(this.batch, closeBtnX, closeBtnY, closeBtnSize, 1);
        ImageManager.getImage(Images.pix255_255_255).draw(this.batch, closeBtnX, closeBtnY + closeBtnSize - 1, closeBtnSize, 1);
        ImageManager.getImage(Images.pix255_255_255).draw(this.batch, closeBtnX, closeBtnY, 1, closeBtnSize);
        ImageManager.getImage(Images.pix255_255_255).draw(this.batch, closeBtnX + closeBtnSize - 1, closeBtnY, 1, closeBtnSize);

        if (CFG.fontMain != null) {
            CFG.drawTextCentered(this.batch, "✕", closeBtnX + closeBtnSize / 2, closeBtnY + closeBtnSize / 2, Color.WHITE);
        }

        // 3. Draw Mod Cards
        List<ModContainer> mods = ModLoader.getInstance().getLoadedMods();
        int cardH = CFG.s(76);
        int startY = this.panelY + pad + CFG.s(50);
        int cardW = this.panelW - pad * 2;

        if (mods.isEmpty()) {
            if (CFG.fontMain != null) {
                String emptyMsg = isRus ? "Моды не найдены в папке mods/" : "No mods found in mods/ folder";
                CFG.drawText(this.batch, emptyMsg, this.panelX + pad, startY + CFG.s(40), Color.GRAY);
            }
        }

        for (int i = 0; i < mods.size(); ++i) {
            ModContainer mc = mods.get(i);
            int cardY = startY + i * (cardH + CFG.s(8));

            // Card background
            this.batch.setColor(0.1f, 0.14f, 0.22f, 0.9f);
            ImageManager.getImage(Images.pix255_255_255).draw(this.batch, this.panelX + pad, cardY, cardW, cardH);

            // Left status stripe
            Color stripeColor = mc.metadata.enabled ? ACTIVE_GREEN : DISABLED_GRAY;
            this.batch.setColor(stripeColor);
            ImageManager.getImage(Images.pix255_255_255).draw(this.batch, this.panelX + pad, cardY, CFG.s(6), cardH);

            // Mod Name and Version
            this.batch.setColor(Color.WHITE);
            String title = mc.metadata.name + " v" + mc.metadata.version;
            CFG.drawText(this.batch, title, this.panelX + pad + CFG.s(16), cardY + CFG.s(18), Color.WHITE);

            // Author and Status Badge
            if (CFG.fontSmall != null) {
                String authorLabel = isRus ? "Автор: " : "Author: ";
                String statusLabel = mc.metadata.enabled ? (isRus ? "[АКТИВЕН]" : "[ENABLED]") : (isRus ? "[ОТКЛЮЧЕН]" : "[DISABLED]");
                String sub = authorLabel + mc.metadata.author + "  |  " + statusLabel;
                CFG.drawTextSmall(this.batch, sub, this.panelX + pad + CFG.s(16), cardY + CFG.s(38), stripeColor);

                // Description
                String desc = mc.metadata.description;
                if (desc.length() > 65) desc = desc.substring(0, 62) + "...";
                CFG.drawTextSmall(this.batch, desc, this.panelX + pad + CFG.s(16), cardY + CFG.s(56), TEXT_CYBER);
            }
        }

        // 4. Draw buttons properly
        if (this.menuPanel != null) {
            this.menuPanel.updateElements(delta);
            if (this.menuPanel.getMenuElements() != null) {
                for (MenuElement el : this.menuPanel.getMenuElements()) {
                    if (el != null && el.getVisible()) {
                        el.draw(this.batch, 0, 0);
                    }
                }
            }
        }

        this.batch.end();
    }

    @Override
    public void resize(int width, int height) {
        updateUIProjection();
        buildUI();
        setupInput();
    }

    @Override
    public void dispose() {
        this.menuPanel = null;
    }
}
