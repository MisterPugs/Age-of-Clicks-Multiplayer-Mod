package cgs.modloader.nuclear;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import ru.ius.david.cgs.CFG;
import ru.ius.david.cgs.CountryData;
import ru.ius.david.cgs.MapScreen;
import ru.ius.david.cgs.WarManager;
import ru.ius.david.cgs.managers.NuclearStrikeManager;
import ru.ius.david.cgs.managers.SoundManager;

import java.util.List;

public class NuclearAlertOverlay {
    private static final NuclearAlertOverlay INSTANCE = new NuclearAlertOverlay();
    public static NuclearAlertOverlay getInstance() { return INSTANCE; }

    private float pulseTimer = 0.0f;
    private float soundCooldown = 0.0f;
    private final GlyphLayout glyphLayout = new GlyphLayout();
    private boolean hadActiveStrikes = false;

    private NuclearAlertOverlay() {}

    public void updateAndRender(SpriteBatch batch, ShapeRenderer shapes, BitmapFont font, float delta, float screenWidth, float screenHeight, MapScreen mapScreen) {
        pulseTimer += delta;
        soundCooldown -= delta;

        NuclearStrikeManager nsm = NuclearStrikeManager.getInstance();
        if (nsm == null) return;

        List<NuclearStrikeManager.NuclearStrike> strikes = nsm.getActiveStrikes();
        boolean hasStrikes = strikes != null && !strikes.isEmpty();

        // Detect new strike launch
        if (hasStrikes && !hadActiveStrikes) {
            triggerAlarm();
            cgs.mods.multiplayer.chat.MultiplayerChat.getInstance().sendMessage(
                "🚨 [ПВО] ЗАФИКСИРОВАН ПУСК МЕЖКОНТИНЕНТАЛЬНОЙ БАЛЛИСТИЧЕСКОЙ РАКЕТЫ!", null
            );
        } else if (!hasStrikes && hadActiveStrikes) {
            MapScreen ms = MapScreen.getActiveInstance();
            if (ms != null) {
                ms.showToast("💥 Ядерный удар завершён. Оценка разрушений...");
            }
        }
        hadActiveStrikes = hasStrikes;

        if (!hasStrikes) return;

        // Periodic alarm sound
        if (soundCooldown <= 0.0f) {
            SoundManager sm = SoundManager.getInstance();
            if (sm != null) {
                try {
                    sm.play(SoundManager.SFX.DENIED, 0.9f);
                } catch (Exception ignored) {}
            }
            soundCooldown = 2.8f;
        }

        NuclearStrikeManager.NuclearStrike primaryStrike = strikes.get(0);
        float progress = Math.max(0.0f, Math.min(1.0f, primaryStrike.progress));
        int secondsLeft = Math.max(1, Math.round((1.0f - progress) * 14.0f));

        String attackerName = "Неизвестно";
        String targetName = "Квадрат (" + (int) primaryStrike.targetX + ", " + (int) primaryStrike.targetY + ")";

        ru.ius.david.cgs.game.GameManager gm = ru.ius.david.cgs.game.GameManager.getInstance();
        if (gm != null && gm.getWarManager() != null) {
            WarManager wm = gm.getWarManager();
            CountryData atkCd = wm.getCountries().get(primaryStrike.ownerColorKey);
            CountryData defCd = wm.getCountries().get(primaryStrike.targetColorKey);
            if (atkCd != null) attackerName = atkCd.getLocalizedName();
            if (defCd != null) targetName = defCd.getLocalizedName();
        }

        float glow = 0.6f + 0.4f * (float) Math.sin(pulseTimer * 10.0f);

        // 1. Full-screen Edge Warning Perimeter
        if (shapes != null) {
            batch.end();
            shapes.setProjectionMatrix(batch.getProjectionMatrix());
            shapes.begin(ShapeRenderer.ShapeType.Line);
            shapes.setColor(1.0f, 0.05f, 0.05f, glow);
            for (int i = 0; i < 4; i++) {
                shapes.rect(i, i, screenWidth - 2 * i, screenHeight - 2 * i);
            }
            shapes.end();
            batch.begin();
        } else {
            ru.ius.david.cgs.images.Image pix = ru.ius.david.cgs.images.ImageManager.getImage(ru.ius.david.cgs.images.Images.pix255_255_255);
            if (pix != null) {
                batch.setColor(1.0f, 0.05f, 0.05f, glow);
                int th = 4;
                pix.draw(batch, 0, 0, (int)screenWidth, th);
                pix.draw(batch, 0, (int)screenHeight - th, (int)screenWidth, th);
                pix.draw(batch, 0, 0, th, (int)screenHeight);
                pix.draw(batch, (int)screenWidth - th, 0, th, (int)screenHeight);
                batch.setColor(Color.WHITE);
            }
        }

        // 2. Cyber Warning Banner at Top Center
        float bannerW = 620.0f;
        float bannerH = 64.0f;
        float bx = (screenWidth - bannerW) / 2.0f;
        float by = screenHeight - bannerH - 45.0f;

        if (shapes != null) {
            batch.end();
            shapes.setProjectionMatrix(batch.getProjectionMatrix());
            shapes.begin(ShapeRenderer.ShapeType.Filled);
            shapes.setColor(0.18f, 0.02f, 0.02f, 0.92f);
            shapes.rect(bx, by, bannerW, bannerH);
            shapes.end();

            shapes.begin(ShapeRenderer.ShapeType.Line);
            shapes.setColor(1.0f, 0.15f * glow, 0.15f * glow, 1.0f);
            shapes.rect(bx, by, bannerW, bannerH);
            shapes.rect(bx + 1, by + 1, bannerW - 2, bannerH - 2);
            shapes.end();
            batch.begin();
        } else {
            ru.ius.david.cgs.images.Image pix = ru.ius.david.cgs.images.ImageManager.getImage(ru.ius.david.cgs.images.Images.pix255_255_255);
            if (pix != null) {
                batch.setColor(0.18f, 0.02f, 0.02f, 0.92f);
                pix.draw(batch, (int)bx, (int)by, (int)bannerW, (int)bannerH);
                batch.setColor(1.0f, 0.15f * glow, 0.15f * glow, 1.0f);
                pix.draw(batch, (int)bx, (int)by, (int)bannerW, 2);
                pix.draw(batch, (int)bx, (int)(by + bannerH - 2), (int)bannerW, 2);
                pix.draw(batch, (int)bx, (int)by, 2, (int)bannerH);
                pix.draw(batch, (int)(bx + bannerW - 2), (int)by, 2, (int)bannerH);
                batch.setColor(Color.WHITE);
            }
        }

        // 3. Banner Typography
        if (font != null) {
            font.setColor(1.0f, 0.2f, 0.2f, 1.0f);
            String title = "🚨 DEFCON 1: ВНИМАНИЕ! ЗАПУСК МБР (" + attackerName + " ➔ " + targetName + ")";
            glyphLayout.setText(font, title);
            font.draw(batch, title, bx + (bannerW - glyphLayout.width) / 2.0f, by + bannerH - 12);

            font.setColor(1.0f, 0.9f, 0.4f, 1.0f);
            String subtitle = "ВРЕМЯ ДО УДАРА: ~" + secondsLeft + " СЕК  |  ПВО В БОЕВОЙ ГОТОВНОСТИ";
            glyphLayout.setText(font, subtitle);
            font.draw(batch, subtitle, bx + (bannerW - glyphLayout.width) / 2.0f, by + 22);
            font.setColor(Color.WHITE);
        }
    }

    private void triggerAlarm() {
        SoundManager sm = SoundManager.getInstance();
        if (sm != null) {
            try {
                sm.play(SoundManager.SFX.DENIED, 1.0f);
            } catch (Exception ignored) {}
        }
        soundCooldown = 2.5f;

        MapScreen ms = MapScreen.getActiveInstance();
        if (ms != null) {
            ms.showToast("🚨 ТРЕВОГА! Зафиксирован пуск ядерной ракеты!");
        }
    }
}
