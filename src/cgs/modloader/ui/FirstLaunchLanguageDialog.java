package cgs.modloader.ui;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.Preferences;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import ru.ius.david.cgs.CFG;
import ru.ius.david.cgs.MainMenuScreen;
import ru.ius.david.cgs.game.LanguageManager;
import ru.ius.david.cgs.game.SettingsManager;
import ru.ius.david.cgs.images.CountryFlagManager;
import ru.ius.david.cgs.images.Image;
import ru.ius.david.cgs.images.ImageManager;
import ru.ius.david.cgs.images.Images;
import ru.ius.david.cgs.managers.SoundManager;

public class FirstLaunchLanguageDialog {
    private static final FirstLaunchLanguageDialog INSTANCE = new FirstLaunchLanguageDialog();
    public static FirstLaunchLanguageDialog getInstance() { return INSTANCE; }

    private static final String PREF_NAME = "cgs_modloader_settings";
    private static final String KEY_LANG_SELECTED = "first_launch_lang_selected";

    private boolean visible = false;
    private float openAnim = 0.0f;
    private float pulseTimer = 0.0f;
    private long lastClickTime = 0;
    private final GlyphLayout glyphLayout = new GlyphLayout();

    private FirstLaunchLanguageDialog() {}

    public boolean isVisible() {
        return visible;
    }

    public void show() {
        this.visible = true;
        this.openAnim = 0.0f;
    }

    public void close() {
        this.visible = false;
    }

    public boolean hasSelectedLanguage() {
        try {
            Preferences prefs = Gdx.app.getPreferences(PREF_NAME);
            return prefs.getBoolean(KEY_LANG_SELECTED, false);
        } catch (Exception e) {
            return false;
        }
    }

    public void checkFirstLaunch() {
        if (!hasSelectedLanguage()) {
            show();
        }
    }

    public boolean handleClick(float mouseX, float mouseY, float screenWidth, float screenHeight, MainMenuScreen mainMenu) {
        if (!visible) return false;

        long now = System.currentTimeMillis();
        if (now - lastClickTime < 90) return true;
        lastClickTime = now;

        float w = 680;
        float h = 430;
        float x = (screenWidth - w) / 2.0f;
        float y = (screenHeight - h) / 2.0f;

        float cardW = 270;
        float cardH = 220;
        float card1X = x + 45;
        float card2X = x + w - 45 - cardW;
        float cardY = y + 115;

        // Click Russian Card
        if (mouseX >= card1X && mouseX <= card1X + cardW && mouseY >= cardY && mouseY <= cardY + cardH) {
            selectLanguage("RUS", mainMenu);
            return true;
        }

        // Click English Card
        if (mouseX >= card2X && mouseX <= card2X + cardW && mouseY >= cardY && mouseY <= cardY + cardH) {
            selectLanguage("ENG", mainMenu);
            return true;
        }

        // Modal consumes all clicks while open so game underneath is locked
        return true;
    }

    private void selectLanguage(String lang, MainMenuScreen mainMenu) {
        playClickSound();

        try {
            SettingsManager sm = SettingsManager.getInstance();
            if (sm != null) {
                sm.setLanguage(lang);
                sm.setLanguageSet(true);
                sm.save();
            }
            LanguageManager lm = LanguageManager.getInstance();
            if (lm != null) {
                lm.init();
            }
        } catch (Exception e) {
            Gdx.app.log("FirstLaunchLanguageDialog", "Error applying language: " + e.getMessage());
        }

        try {
            Preferences prefs = Gdx.app.getPreferences(PREF_NAME);
            prefs.putBoolean(KEY_LANG_SELECTED, true);
            prefs.putString("chosen_language", lang);
            prefs.flush();
        } catch (Exception ignored) {}

        this.visible = false;

        if (mainMenu != null) {
            mainMenu.reloadLanguage();
        }
    }

    private void playClickSound() {
        SoundManager sm = SoundManager.getInstance();
        if (sm != null) {
            try {
                sm.play(SoundManager.SFX.BUTTON_CLICK, 0.85f);
            } catch (Exception ignored) {}
        }
    }

    public void render(SpriteBatch batch, BitmapFont fontMain, BitmapFont fontSmall, float delta, float screenWidth, float screenHeight, MainMenuScreen mainMenu) {
        if (!visible) return;

        pulseTimer += delta;
        openAnim = Math.min(1.0f, openAnim + delta * 5.0f);
        float ease = 1.0f - (float) Math.pow(1.0f - openAnim, 3);

        // Hardware-direct left click polling with debounce
        if (Gdx.input.isButtonJustPressed(Input.Buttons.LEFT)) {
            handleClick(Gdx.input.getX(), Gdx.input.getY(), screenWidth, screenHeight, mainMenu);
        }

        float w = 680;
        float h = 430;
        float x = (screenWidth - w) / 2.0f;
        float y = (screenHeight - h) / 2.0f;

        float mouseX = Gdx.input.getX();
        float mouseY = Gdx.input.getY();

        Image pix = ImageManager.getImage(Images.pix255_255_255);
        if (pix == null) return;

        // 1. Dark veil covering entire screen with fade
        batch.setColor(0.0f, 0.0f, 0.0f, 0.75f * ease);
        pix.draw(batch, 0, 0, (int) screenWidth, (int) screenHeight);

        // 2. Dialog Panel background
        batch.setColor(0.04f, 0.08f, 0.14f, 0.98f * ease);
        pix.draw(batch, (int) x, (int) y, (int) w, (int) h);

        // 3. Cyber Outline with pulse glow
        float glow = 0.7f + 0.3f * (float) Math.sin(pulseTimer * 3.5f);
        batch.setColor(0.15f * glow, 0.65f * glow, 1.0f * glow, 0.95f * ease);
        drawOutline(pix, batch, (int) x, (int) y, (int) w, (int) h, 2);

        // Cyber top accent stripe
        batch.setColor(0.2f, 0.75f, 1.0f, 0.85f * ease);
        pix.draw(batch, (int) x + 2, (int) y + 2, (int) w - 4, 3);

        // Scanline animation
        float scanY = y + ((pulseTimer * 65.0f) % h);
        batch.setColor(0.3f, 0.8f, 1.0f, 0.06f * ease);
        pix.draw(batch, (int) x + 2, (int) scanY, (int) w - 4, 2);

        // 4. Header Titles
        if (fontMain != null) {
            String title = "🌍 ВЫБОР ЯЗЫКА  /  LANGUAGE SELECTION";
            glyphLayout.setText(fontMain, title);
            fontMain.setColor(1.0f, 0.92f, 0.3f, ease);
            fontMain.draw(batch, title, x + (w - glyphLayout.width) / 2.0f, y + 26);
        }

        if (fontSmall != null) {
            String sub = "Выберите язык для игры и мода • Choose preferred language for game & mod";
            glyphLayout.setText(fontSmall, sub);
            fontSmall.setColor(0.75f, 0.88f, 1.0f, 0.9f * ease);
            fontSmall.draw(batch, sub, x + (w - glyphLayout.width) / 2.0f, y + 54);
        }

        // Horizontal separator line
        batch.setColor(0.2f, 0.4f, 0.65f, 0.5f * ease);
        pix.draw(batch, (int) x + 30, (int) y + 80, (int) w - 60, 1);

        // 5. Two Language Cards
        float cardW = 270;
        float cardH = 220;
        float card1X = x + 45;
        float card2X = x + w - 45 - cardW;
        float cardY = y + 105;

        boolean hoverRus = (mouseX >= card1X && mouseX <= card1X + cardW && mouseY >= cardY && mouseY <= cardY + cardH);
        boolean hoverEng = (mouseX >= card2X && mouseX <= card2X + cardW && mouseY >= cardY && mouseY <= cardY + cardH);

        // --- Russian Card ---
        renderLanguageCard(batch, pix, fontMain, fontSmall, card1X, cardY, cardW, cardH,
            "RUS", "РУССКИЙ ЯЗЫК", "Русская локализация", "[ ВЫБРАТЬ ]", hoverRus, ease);

        // --- English Card ---
        renderLanguageCard(batch, pix, fontMain, fontSmall, card2X, cardY, cardW, cardH,
            "ENG", "ENGLISH", "English Localization", "[ SELECT ]", hoverEng, ease);

        // 6. Bottom Notice / Tip
        if (fontSmall != null) {
            String tip = "💡 Выбор запоминается навсегда (можно изменить в настройках игры)";
            glyphLayout.setText(fontSmall, tip);
            fontSmall.setColor(0.6f, 0.78f, 0.95f, 0.8f * ease);
            fontSmall.draw(batch, tip, x + (w - glyphLayout.width) / 2.0f, y + h - 52);

            String tipEn = "Selection is saved automatically (can be changed anytime in Settings)";
            glyphLayout.setText(fontSmall, tipEn);
            fontSmall.setColor(0.5f, 0.68f, 0.85f, 0.7f * ease);
            fontSmall.draw(batch, tipEn, x + (w - glyphLayout.width) / 2.0f, y + h - 28);
        }

        batch.setColor(Color.WHITE);
    }

    private void renderLanguageCard(SpriteBatch batch, Image pix, BitmapFont fontMain, BitmapFont fontSmall,
                                    float cx, float cy, float cw, float ch,
                                    String flagTag, String title, String subtitle, String btnLabel,
                                    boolean hovered, float ease) {
        // Card Plate Background
        if (hovered) {
            batch.setColor(0.12f, 0.22f, 0.36f, 1.0f * ease);
        } else {
            batch.setColor(0.06f, 0.11f, 0.18f, 0.95f * ease);
        }
        pix.draw(batch, (int) cx, (int) cy, (int) cw, (int) ch);

        // Card Outline
        if (hovered) {
            batch.setColor(0.35f, 0.9f, 1.0f, 1.0f * ease);
            drawOutline(pix, batch, (int) cx, (int) cy, (int) cw, (int) ch, 2);
        } else {
            batch.setColor(0.18f, 0.32f, 0.5f, 0.8f * ease);
            drawOutline(pix, batch, (int) cx, (int) cy, (int) cw, (int) ch, 1);
        }

        // Flag Image
        float flagW = 120;
        float flagH = 75;
        float flagX = cx + (cw - flagW) / 2.0f;
        float flagY = cy + 18;

        int flagId = -1;
        try {
            CountryFlagManager cfm = CountryFlagManager.getInstance();
            if (cfm != null) {
                flagId = cfm.getBaseFlagImageId(flagTag);
            }
        } catch (Exception ignored) {}

        if (flagId > 0 && ImageManager.getImage(flagId) != null) {
            batch.setColor(Color.WHITE);
            ImageManager.getImage(flagId).draw(batch, (int) flagX, (int) flagY, (int) flagW, (int) flagH);
        } else {
            if ("RUS".equalsIgnoreCase(flagTag)) {
                drawRussianFlagFallback(batch, pix, (int) flagX, (int) flagY, (int) flagW, (int) flagH);
            } else {
                drawEnglishFlagFallback(batch, pix, (int) flagX, (int) flagY, (int) flagW, (int) flagH);
            }
        }

        // Flag border outline
        batch.setColor(0.3f, 0.5f, 0.7f, 0.9f * ease);
        drawOutline(pix, batch, (int) flagX, (int) flagY, (int) flagW, (int) flagH, 1);

        // Title
        if (fontMain != null) {
            fontMain.setColor(hovered ? Color.WHITE : new Color(0.9f, 0.96f, 1.0f, ease));
            glyphLayout.setText(fontMain, title);
            fontMain.draw(batch, title, cx + (cw - glyphLayout.width) / 2.0f, cy + 110);
        }

        // Subtitle
        if (fontSmall != null) {
            fontSmall.setColor(hovered ? new Color(0.7f, 0.9f, 1.0f, ease) : new Color(0.55f, 0.7f, 0.85f, 0.85f * ease));
            glyphLayout.setText(fontSmall, subtitle);
            fontSmall.draw(batch, subtitle, cx + (cw - glyphLayout.width) / 2.0f, cy + 138);
        }

        // Action Button pill
        float btnW = 150;
        float btnH = 34;
        float btnX = cx + (cw - btnW) / 2.0f;
        float btnY = cy + 168;

        if (hovered) {
            batch.setColor(0.18f, 0.45f, 0.75f, 1.0f * ease);
        } else {
            batch.setColor(0.1f, 0.2f, 0.32f, 0.9f * ease);
        }
        pix.draw(batch, (int) btnX, (int) btnY, (int) btnW, (int) btnH);

        batch.setColor(hovered ? new Color(0.4f, 0.95f, 1.0f, 1.0f * ease) : new Color(0.25f, 0.55f, 0.8f, 0.85f * ease));
        drawOutline(pix, batch, (int) btnX, (int) btnY, (int) btnW, (int) btnH, hovered ? 2 : 1);

        if (fontSmall != null) {
            fontSmall.setColor(hovered ? Color.WHITE : new Color(0.85f, 0.95f, 1.0f, ease));
            glyphLayout.setText(fontSmall, btnLabel);
            int ty = CFG.textTopCentered(fontSmall, (int) btnY, (int) btnH);
            CFG.drawTextSmall(batch, btnLabel, (int)(btnX + (btnW - glyphLayout.width) / 2.0f), ty, hovered ? Color.WHITE : new Color(0.85f, 0.95f, 1.0f, ease));
        }
    }

    private void drawRussianFlagFallback(SpriteBatch batch, Image pix, int fx, int fy, int fw, int fh) {
        int stripeH = fh / 3;
        // White
        batch.setColor(0.95f, 0.95f, 0.95f, 1.0f);
        pix.draw(batch, fx, fy, fw, stripeH);
        // Blue
        batch.setColor(0.0f, 0.25f, 0.75f, 1.0f);
        pix.draw(batch, fx, fy + stripeH, fw, stripeH);
        // Red
        batch.setColor(0.85f, 0.1f, 0.1f, 1.0f);
        pix.draw(batch, fx, fy + stripeH * 2, fw, fh - stripeH * 2);
    }

    private void drawEnglishFlagFallback(SpriteBatch batch, Image pix, int fx, int fy, int fw, int fh) {
        // Dark blue base (Union Jack style)
        batch.setColor(0.02f, 0.12f, 0.45f, 1.0f);
        pix.draw(batch, fx, fy, fw, fh);
        // White cross bars
        int crossW = 20;
        int crossH = 20;
        batch.setColor(0.95f, 0.95f, 0.95f, 1.0f);
        pix.draw(batch, fx, fy + (fh - crossH) / 2, fw, crossH);
        pix.draw(batch, fx + (fw - crossW) / 2, fy, crossW, fh);
        // Red cross
        int redW = 12;
        int redH = 12;
        batch.setColor(0.85f, 0.1f, 0.15f, 1.0f);
        pix.draw(batch, fx, fy + (fh - redH) / 2, fw, redH);
        pix.draw(batch, fx + (fw - redW) / 2, fy, redW, fh);
    }

    private void drawOutline(Image pix, SpriteBatch batch, int x, int y, int w, int h, int thickness) {
        pix.draw(batch, x, y, w, thickness);
        pix.draw(batch, x, y + h - thickness, w, thickness);
        pix.draw(batch, x, y, thickness, h);
        pix.draw(batch, x + w - thickness, y, thickness, h);
    }
}
