package cgs.modloader.trade;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import ru.ius.david.cgs.CountryData;
import ru.ius.david.cgs.MapScreen;
import ru.ius.david.cgs.WarManager;
import ru.ius.david.cgs.game.CountryState;
import ru.ius.david.cgs.game.GameManager;
import ru.ius.david.cgs.multiplayer.MultiplayerManager;
import ru.ius.david.cgs.units.MilitaryUnit;
import cgs.mods.multiplayer.actions.QuickAidGoldAction;
import cgs.mods.multiplayer.actions.TransferTroopAction;

import java.util.ArrayList;
import java.util.List;

public class DirectAidDialog {
    private static final DirectAidDialog INSTANCE = new DirectAidDialog();
    public static DirectAidDialog getInstance() { return INSTANCE; }

    private boolean visible = false;
    private final GlyphLayout glyphLayout = new GlyphLayout();
    private int selectedAllyIndex = 0;

    private DirectAidDialog() {}

    public boolean isVisible() {
        return visible;
    }

    public void setVisible(boolean visible) {
        this.visible = visible;
    }

    public void toggle() {
        this.visible = !this.visible;
    }

    public boolean handleClick(float mouseX, float mouseY, float screenWidth, float screenHeight, GameManager gm, MultiplayerManager mpMgr) {
        if (!visible || gm == null) return false;

        float w = 540;
        float h = 360;
        float x = (screenWidth - w) / 2.0f;
        float y = (screenHeight - h) / 2.0f;

        if (mouseX < x || mouseX > x + w || mouseY < y || mouseY > y + h) {
            visible = false;
            return true;
        }

        List<CountryState> allies = getPotentialAllies(gm);
        if (!allies.isEmpty()) {
            if (selectedAllyIndex >= allies.size()) selectedAllyIndex = 0;
            CountryState target = allies.get(selectedAllyIndex);
            WarManager wm = gm.getWarManager();
            int targetColorKey = (wm != null) ? wm.getColorKeyByName(target.getName()) : -1;

            // Cycle ally button
            float btnAllyY = y + h - 110;
            if (mouseX >= x + 30 && mouseX <= x + w - 30 && mouseY >= btnAllyY && mouseY <= btnAllyY + 36) {
                selectedAllyIndex = (selectedAllyIndex + 1) % allies.size();
                return true;
            }

            // Gold buttons
            float goldY = y + 140;
            if (mouseY >= goldY && mouseY <= goldY + 40) {
                if (mouseX >= x + 30 && mouseX <= x + 170) {
                    sendGold(gm, mpMgr, targetColorKey, target.getName(), 10000L);
                    return true;
                } else if (mouseX >= x + 190 && mouseX <= x + 340) {
                    sendGold(gm, mpMgr, targetColorKey, target.getName(), 50000L);
                    return true;
                } else if (mouseX >= x + 360 && mouseX <= x + 510) {
                    sendGold(gm, mpMgr, targetColorKey, target.getName(), 250000L);
                    return true;
                }
            }

            // Transfer troop button
            float troopY = y + 80;
            if (mouseX >= x + 30 && mouseX <= x + w - 30 && mouseY >= troopY && mouseY <= troopY + 40) {
                transferFirstTroop(gm, mpMgr, targetColorKey, target.getName());
                return true;
            }
        }

        // Close button
        float closeY = y + 20;
        if (mouseX >= x + w / 2.0f - 80 && mouseX <= x + w / 2.0f + 80 && mouseY >= closeY && mouseY <= closeY + 35) {
            visible = false;
            return true;
        }

        return true;
    }

    private void sendGold(GameManager gm, MultiplayerManager mpMgr, int targetColorKey, String targetName, long amount) {
        QuickAidGoldAction action = new QuickAidGoldAction(mpMgr != null ? mpMgr.getLocalPlayerId() : 0, targetColorKey, targetName, amount);
        if (mpMgr != null) {
            mpMgr.enqueueAction(action);
        } else {
            action.execute(gm);
        }
    }

    private void transferFirstTroop(GameManager gm, MultiplayerManager mpMgr, int targetColorKey, String targetName) {
        if (gm == null || gm.getWarManager() == null || gm.getPlayerCountry() == null) return;
        WarManager wm = gm.getWarManager();
        int playerCk = wm.getColorKeyByName(gm.getPlayerCountry().getName());

        for (MilitaryUnit u : wm.getUnitManager().getUnits()) {
            if (u.ownerColorKey == playerCk) {
                TransferTroopAction action = new TransferTroopAction(mpMgr != null ? mpMgr.getLocalPlayerId() : 0, u.id, targetColorKey, targetName);
                if (mpMgr != null) {
                    mpMgr.enqueueAction(action);
                } else {
                    action.execute(gm);
                }
                return;
            }
        }

        MapScreen ms = MapScreen.getActiveInstance();
        if (ms != null) {
            ms.showToast("⚠️ У вас нет свободных корпусов для передачи!");
        }
    }

    private List<CountryState> getPotentialAllies(GameManager gm) {
        List<CountryState> list = new ArrayList<>();
        if (gm == null || gm.getPlayerCountry() == null) return list;
        CountryState player = gm.getPlayerCountry();

        for (CountryState cs : gm.getWorldCountries().values()) {
            if (cs != player && (cs.isAlly() || (player.allianceName != null && player.allianceName.equals(cs.allianceName)) || cs.getAiRelation(player.getName()) > 50)) {
                list.add(cs);
            }
        }
        if (list.isEmpty()) {
            for (CountryState cs : gm.getWorldCountries().values()) {
                if (cs != player && list.size() < 5) {
                    list.add(cs);
                }
            }
        }
        return list;
    }

    public void render(SpriteBatch batch, ShapeRenderer shapes, BitmapFont font, float screenWidth, float screenHeight, GameManager gm) {
        if (!visible || gm == null) return;

        float w = 540;
        float h = 360;
        float x = (screenWidth - w) / 2.0f;
        float y = (screenHeight - h) / 2.0f;

        if (shapes != null) {
            batch.end();
            shapes.setProjectionMatrix(batch.getProjectionMatrix());
            shapes.begin(ShapeRenderer.ShapeType.Filled);
            shapes.setColor(0.06f, 0.09f, 0.16f, 0.96f);
            shapes.rect(x, y, w, h);

            // Header background
            shapes.setColor(0.1f, 0.2f, 0.35f, 0.9f);
            shapes.rect(x, y + h - 50, w, 50);

            // Ally selector button
            shapes.setColor(0.14f, 0.19f, 0.28f, 1.0f);
            shapes.rect(x + 30, y + h - 110, w - 60, 36);

            // Gold buttons
            shapes.setColor(0.2f, 0.18f, 0.08f, 1.0f);
            shapes.rect(x + 30, y + 140, 140, 40);
            shapes.rect(x + 190, y + 140, 150, 40);
            shapes.rect(x + 360, y + 140, 150, 40);

            // Troop button
            shapes.setColor(0.18f, 0.24f, 0.15f, 1.0f);
            shapes.rect(x + 30, y + 80, w - 60, 40);

            // Close button
            shapes.setColor(0.35f, 0.12f, 0.12f, 1.0f);
            shapes.rect(x + w / 2.0f - 80, y + 20, 160, 35);
            shapes.end();

            shapes.begin(ShapeRenderer.ShapeType.Line);
            shapes.setColor(0.2f, 0.7f, 1.0f, 0.9f);
            shapes.rect(x, y, w, h);
            shapes.setColor(0.9f, 0.7f, 0.2f, 0.8f);
            shapes.rect(x + 30, y + 140, 140, 40);
            shapes.rect(x + 190, y + 140, 150, 40);
            shapes.rect(x + 360, y + 140, 150, 40);
            shapes.setColor(0.4f, 0.9f, 0.3f, 0.8f);
            shapes.rect(x + 30, y + 80, w - 60, 40);
            shapes.end();
            batch.begin();
        } else {
            ru.ius.david.cgs.images.Image pix = ru.ius.david.cgs.images.ImageManager.getImage(ru.ius.david.cgs.images.Images.pix255_255_255);
            if (pix != null) {
                // Background
                batch.setColor(0.06f, 0.09f, 0.16f, 0.96f);
                pix.draw(batch, (int)x, (int)y, (int)w, (int)h);

                // Header
                batch.setColor(0.1f, 0.2f, 0.35f, 0.9f);
                pix.draw(batch, (int)x, (int)(y + h - 50), (int)w, 50);

                // Ally selector button
                batch.setColor(0.14f, 0.19f, 0.28f, 1.0f);
                pix.draw(batch, (int)(x + 30), (int)(y + h - 110), (int)(w - 60), 36);

                // Gold buttons
                batch.setColor(0.2f, 0.18f, 0.08f, 1.0f);
                pix.draw(batch, (int)(x + 30), (int)(y + 140), 140, 40);
                pix.draw(batch, (int)(x + 190), (int)(y + 140), 150, 40);
                pix.draw(batch, (int)(x + 360), (int)(y + 140), 150, 40);

                // Troop button
                batch.setColor(0.18f, 0.24f, 0.15f, 1.0f);
                pix.draw(batch, (int)(x + 30), (int)(y + 80), (int)(w - 60), 40);

                // Close button
                batch.setColor(0.35f, 0.12f, 0.12f, 1.0f);
                pix.draw(batch, (int)(x + w / 2.0f - 80), (int)(y + 20), 160, 35);

                // Outer border
                batch.setColor(0.2f, 0.7f, 1.0f, 0.9f);
                pix.draw(batch, (int)x, (int)y, (int)w, 2);
                pix.draw(batch, (int)x, (int)(y + h - 2), (int)w, 2);
                pix.draw(batch, (int)x, (int)y, 2, (int)h);
                pix.draw(batch, (int)(x + w - 2), (int)y, 2, (int)h);
                batch.setColor(Color.WHITE);
            }
        }

        if (font != null) {
            font.setColor(0.3f, 0.9f, 1.0f, 1.0f);
            String title = "🤝 СЕТЕВОЙ ЛЕНД-ЛИЗ И ПОМОЩЬ СОЮЗНИКАМ (F7)";
            glyphLayout.setText(font, title);
            font.draw(batch, title, x + (w - glyphLayout.width) / 2.0f, y + h - 18);

            List<CountryState> allies = getPotentialAllies(gm);
            String targetTitle = "Нет доступных союзников";
            if (!allies.isEmpty()) {
                if (selectedAllyIndex >= allies.size()) selectedAllyIndex = 0;
                targetTitle = "Получатель: " + allies.get(selectedAllyIndex).getLocalizedName() + " (клик для смены ➔)";
            }
            font.setColor(1.0f, 1.0f, 1.0f, 1.0f);
            glyphLayout.setText(font, targetTitle);
            font.draw(batch, targetTitle, x + (w - glyphLayout.width) / 2.0f, y + h - 86);

            CountryState player = gm.getPlayerCountry();
            String goldInfo = "Ваша казна: " + (player != null ? player.getGold() : 0) + " 🪙";
            font.setColor(1.0f, 0.85f, 0.3f, 1.0f);
            glyphLayout.setText(font, goldInfo);
            font.draw(batch, goldInfo, x + 30, y + 205);

            // Gold button labels
            font.setColor(1.0f, 0.9f, 0.3f, 1.0f);
            font.draw(batch, "+10 000 🪙", x + 55, y + 165);
            font.draw(batch, "+50 000 🪙", x + 220, y + 165);
            font.draw(batch, "+250 000 🪙", x + 380, y + 165);

            // Troop transfer label
            font.setColor(0.5f, 1.0f, 0.5f, 1.0f);
            String troopText = "⚔️ Передать армейский корпус союзнику";
            glyphLayout.setText(font, troopText);
            font.draw(batch, troopText, x + (w - glyphLayout.width) / 2.0f, y + 105);

            // Close button label
            font.setColor(1.0f, 0.8f, 0.8f, 1.0f);
            String closeText = "ЗАКРЫТЬ";
            glyphLayout.setText(font, closeText);
            font.draw(batch, closeText, x + (w - glyphLayout.width) / 2.0f, y + 43);

            font.setColor(Color.WHITE);
        }
    }
}
