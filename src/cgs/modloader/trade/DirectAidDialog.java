package cgs.modloader.trade;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputMultiplexer;
import com.badlogic.gdx.InputProcessor;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import ru.ius.david.cgs.CFG;
import ru.ius.david.cgs.CountryData;
import ru.ius.david.cgs.MapScreen;
import ru.ius.david.cgs.WarManager;
import ru.ius.david.cgs.game.CountryState;
import ru.ius.david.cgs.game.GameManager;
import ru.ius.david.cgs.images.Image;
import ru.ius.david.cgs.images.ImageManager;
import ru.ius.david.cgs.images.Images;
import ru.ius.david.cgs.managers.SoundManager;
import ru.ius.david.cgs.multiplayer.MultiplayerManager;
import ru.ius.david.cgs.units.MilitaryUnit;
import cgs.mods.multiplayer.actions.QuickAidGoldAction;
import cgs.mods.multiplayer.actions.TransferTroopAction;

import java.util.ArrayList;
import java.util.List;

public class DirectAidDialog implements InputProcessor {
    private static final DirectAidDialog INSTANCE = new DirectAidDialog();
    public static DirectAidDialog getInstance() { return INSTANCE; }

    private boolean visible = false;
    private final GlyphLayout glyphLayout = new GlyphLayout();

    private boolean filterAlliesOnly = false;
    private int selectedCountryIndex = 0;
    private boolean countryListPopup = false;
    private int listScrollOffset = 0;

    private DirectAidDialog() {}

    public boolean isVisible() {
        return visible;
    }

    public void setVisible(boolean visible) {
        this.visible = visible;
        if (visible) {
            ensureInputProcessorAttached();
        }
    }

    public void toggle() {
        setVisible(!this.visible);
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

    private List<CountryState> getAvailableCountries(GameManager gm) {
        List<CountryState> result = new ArrayList<>();
        if (gm == null || gm.getPlayerCountry() == null) return result;
        CountryState player = gm.getPlayerCountry();

        for (CountryState cs : gm.getWorldCountries().values()) {
            if (cs == player) continue;
            boolean isAlly = cs.isAlly() || (player.allianceName != null && player.allianceName.equals(cs.allianceName)) || cs.getAiRelation(player.getName()) > 50;
            if (!filterAlliesOnly || isAlly) {
                result.add(cs);
            }
        }
        if (result.isEmpty() && filterAlliesOnly) {
            for (CountryState cs : gm.getWorldCountries().values()) {
                if (cs != player) result.add(cs);
            }
        }
        return result;
    }

    public boolean handleClick(float mouseX, float mouseY, float screenWidth, float screenHeight, GameManager gm, MultiplayerManager mpMgr) {
        if (!visible || gm == null) return false;

        float w = 620;
        float h = 480;
        float x = (screenWidth - w) / 2.0f;
        float y = (screenHeight - h) / 2.0f;

        // If click is outside dialog window -> close dialog and CONSUME CLICK (prevent click-through to map)
        if (mouseX < x || mouseX > x + w || mouseY < y || mouseY > y + h) {
            playClickSound();
            visible = false;
            countryListPopup = false;
            return true;
        }

        List<CountryState> countries = getAvailableCountries(gm);
        CountryState player = gm.getPlayerCountry();

        // 1. Close button (at bottom)
        float closeBtnW = 180;
        float closeBtnH = 38;
        float closeBtnX = x + (w - closeBtnW) / 2.0f;
        float closeBtnY = y + h - 50;
        if (mouseX >= closeBtnX && mouseX <= closeBtnX + closeBtnW && mouseY >= closeBtnY && mouseY <= closeBtnY + closeBtnH) {
            playClickSound();
            visible = false;
            countryListPopup = false;
            return true;
        }

        // If Country Dropdown Popup is open
        if (countryListPopup) {
            float popW = w - 60;
            float popH = 260;
            float popX = x + 30;
            float popY = y + 105;

            // Prev / Next page buttons inside popup
            if (mouseY >= popY + popH - 36 && mouseY <= popY + popH) {
                if (mouseX >= popX && mouseX <= popX + 100) {
                    playClickSound();
                    listScrollOffset = Math.max(0, listScrollOffset - 6);
                    return true;
                } else if (mouseX >= popX + popW - 100 && mouseX <= popX + popW) {
                    playClickSound();
                    if (listScrollOffset + 6 < countries.size()) {
                        listScrollOffset += 6;
                    }
                    return true;
                }
            }

            // Click on a country item
            float itemH = 34;
            for (int i = 0; i < 6; i++) {
                int cIdx = listScrollOffset + i;
                if (cIdx >= countries.size()) break;
                float itemY = popY + 40 + i * itemH;
                if (mouseX >= popX + 10 && mouseX <= popX + popW - 10 && mouseY >= itemY && mouseY <= itemY + itemH - 4) {
                    playClickSound();
                    selectedCountryIndex = cIdx;
                    countryListPopup = false;
                    return true;
                }
            }

            // Click elsewhere in popup closes popup
            countryListPopup = false;
            return true;
        }

        if (!countries.isEmpty()) {
            if (selectedCountryIndex >= countries.size()) selectedCountryIndex = 0;
            CountryState target = countries.get(selectedCountryIndex);
            WarManager wm = gm.getWarManager();
            int targetColorKey = (wm != null) ? wm.getColorKeyByName(target.getName()) : -1;
            boolean isEnemy = target.hasWarGoalAgainst(wm != null && player != null ? wm.getColorKeyByName(player.getName()) : -1);

            // 2. Previous Country Button [ ◀ ПРЕД ]
            float prevBtnX = x + 28;
            float prevBtnY = y + 68;
            float prevBtnW = 75;
            float prevBtnH = 36;
            if (mouseX >= prevBtnX && mouseX <= prevBtnX + prevBtnW && mouseY >= prevBtnY && mouseY <= prevBtnY + prevBtnH) {
                playClickSound();
                selectedCountryIndex = (selectedCountryIndex - 1 + countries.size()) % countries.size();
                return true;
            }

            // 3. Country Display Card (Clicking opens full list popup)
            float cardX = x + 108;
            float cardY = y + 68;
            float cardW = w - 216;
            float cardH = 36;
            if (mouseX >= cardX && mouseX <= cardX + cardW && mouseY >= cardY && mouseY <= cardY + cardH) {
                playClickSound();
                countryListPopup = true;
                listScrollOffset = (selectedCountryIndex / 6) * 6;
                return true;
            }

            // 4. Next Country Button [ СЛЕД ▶ ]
            float nextBtnX = x + w - 103;
            float nextBtnY = y + 68;
            float nextBtnW = 75;
            float nextBtnH = 36;
            if (mouseX >= nextBtnX && mouseX <= nextBtnX + nextBtnW && mouseY >= nextBtnY && mouseY <= nextBtnY + nextBtnH) {
                playClickSound();
                selectedCountryIndex = (selectedCountryIndex + 1) % countries.size();
                return true;
            }

            // 5. Filter Toggle [ 🔄 Фильтр: Только союзники / Все страны ]
            float filterBtnX = x + 28;
            float filterBtnY = y + 112;
            float filterBtnW = w - 56;
            float filterBtnH = 28;
            if (mouseX >= filterBtnX && mouseX <= filterBtnX + filterBtnW && mouseY >= filterBtnY && mouseY <= filterBtnY + filterBtnH) {
                playClickSound();
                filterAlliesOnly = !filterAlliesOnly;
                selectedCountryIndex = 0;
                return true;
            }

            // 6. Gold Aid Buttons (+10k, +50k, +250k)
            float goldBtnsY = y + 218;
            float goldBtnH = 44;
            if (mouseY >= goldBtnsY && mouseY <= goldBtnsY + goldBtnH && !isEnemy) {
                // Button 1: +10 000
                if (mouseX >= x + 28 && mouseX <= x + 198) {
                    playClickSound();
                    sendGold(gm, mpMgr, targetColorKey, target.getName(), 10000L);
                    return true;
                }
                // Button 2: +50 000
                else if (mouseX >= x + 212 && mouseX <= x + 382) {
                    playClickSound();
                    sendGold(gm, mpMgr, targetColorKey, target.getName(), 50000L);
                    return true;
                }
                // Button 3: +250 000
                else if (mouseX >= x + 396 && mouseX <= x + w - 28) {
                    playClickSound();
                    sendGold(gm, mpMgr, targetColorKey, target.getName(), 250000L);
                    return true;
                }
            }

            // 7. Troop Transfer Button
            float troopBtnX = x + 28;
            float troopBtnY = y + 288;
            float troopBtnW = w - 56;
            float troopBtnH = 46;
            if (mouseX >= troopBtnX && mouseX <= troopBtnX + troopBtnW && mouseY >= troopBtnY && mouseY <= troopBtnY + troopBtnH && !isEnemy) {
                playClickSound();
                transferFirstTroop(gm, mpMgr, targetColorKey, target.getName());
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

    private void sendGold(GameManager gm, MultiplayerManager mpMgr, int targetColorKey, String targetName, long amount) {
        CountryState player = gm.getPlayerCountry();
        if (player != null && player.getGold() < amount) {
            MapScreen ms = MapScreen.getActiveInstance();
            if (ms != null) ms.showToast("⚠️ Недостаточно золота в казне!");
            return;
        }

        QuickAidGoldAction action = new QuickAidGoldAction(mpMgr != null ? mpMgr.getLocalPlayerId() : 0, targetColorKey, targetName, amount);
        if (mpMgr != null && mpMgr.isMultiplayer()) {
            mpMgr.enqueueAction(action);
        } else {
            action.execute(gm);
        }
        MapScreen ms = MapScreen.getActiveInstance();
        if (ms != null) ms.showToast("🪙 Отправлена помощь: +" + amount + " для " + targetName);
    }

    private void transferFirstTroop(GameManager gm, MultiplayerManager mpMgr, int targetColorKey, String targetName) {
        if (gm == null || gm.getWarManager() == null || gm.getPlayerCountry() == null) return;
        WarManager wm = gm.getWarManager();
        int playerCk = wm.getColorKeyByName(gm.getPlayerCountry().getName());

        for (MilitaryUnit u : wm.getUnitManager().getUnits()) {
            if (u.ownerColorKey == playerCk) {
                TransferTroopAction action = new TransferTroopAction(mpMgr != null ? mpMgr.getLocalPlayerId() : 0, u.id, targetColorKey, targetName);
                if (mpMgr != null && mpMgr.isMultiplayer()) {
                    mpMgr.enqueueAction(action);
                } else {
                    action.execute(gm);
                }
                MapScreen ms = MapScreen.getActiveInstance();
                if (ms != null) ms.showToast("⚔️ Армейский корпус успешно передан державе " + targetName + "!");
                return;
            }
        }

        MapScreen ms = MapScreen.getActiveInstance();
        if (ms != null) {
            ms.showToast("⚠️ У вас нет свободных корпусов для передачи!");
        }
    }

    public void render(SpriteBatch batch, ShapeRenderer shapes, BitmapFont font, float screenWidth, float screenHeight, GameManager gm) {
        if (!visible || gm == null) return;
        ensureInputProcessorAttached();

        float w = 620;
        float h = 480;
        float x = (screenWidth - w) / 2.0f;
        float y = (screenHeight - h) / 2.0f; // y is TOP of the window in Age of Clicks Y-down coordinate system

        Image pix = ImageManager.getImage(Images.pix255_255_255);
        if (pix == null) return;

        // Dim background veil
        batch.setColor(0.0f, 0.0f, 0.0f, 0.45f);
        pix.draw(batch, 0, 0, (int)screenWidth, (int)screenHeight);

        // Main Dialog Panel Background
        batch.setColor(0.05f, 0.08f, 0.14f, 0.98f);
        pix.draw(batch, (int)x, (int)y, (int)w, (int)h);

        // Header Background Bar (at top of dialog)
        batch.setColor(0.08f, 0.22f, 0.38f, 1.0f);
        pix.draw(batch, (int)x, (int)y, (int)w, 48);

        // Cyber Border around dialog
        batch.setColor(0.2f, 0.65f, 0.95f, 1.0f);
        pix.draw(batch, (int)x, (int)y, (int)w, 2);
        pix.draw(batch, (int)x, (int)(y + h - 2), (int)w, 2);
        pix.draw(batch, (int)x, (int)y, 2, (int)h);
        pix.draw(batch, (int)(x + w - 2), (int)y, 2, (int)h);

        // Header bottom separator
        batch.setColor(0.25f, 0.75f, 1.0f, 0.9f);
        pix.draw(batch, (int)x, (int)(y + 48), (int)w, 2);

        List<CountryState> countries = getAvailableCountries(gm);
        CountryState player = gm.getPlayerCountry();
        CountryState target = (!countries.isEmpty() && selectedCountryIndex < countries.size()) ? countries.get(selectedCountryIndex) : null;

        // 1. Header Title Text
        if (font != null) {
            font.setColor(0.35f, 0.9f, 1.0f, 1.0f);
            String title = "🤝 СЕТЕВОЙ ЛЕНД-ЛИЗ И ВОЕННАЯ ПОМОЩЬ (F7)";
            glyphLayout.setText(font, title);
            font.draw(batch, title, x + (w - glyphLayout.width) / 2.0f, y + 14);
        }

        // 2. Country Selector Section
        // Left Button [ ◀ ПРЕД ]
        batch.setColor(0.14f, 0.24f, 0.38f, 1.0f);
        pix.draw(batch, (int)(x + 28), (int)(y + 68), 75, 36);
        batch.setColor(0.25f, 0.6f, 0.9f, 0.8f);
        drawRectOutline(pix, batch, (int)(x + 28), (int)(y + 68), 75, 36);

        // Center Country Display Card
        batch.setColor(0.1f, 0.16f, 0.26f, 1.0f);
        pix.draw(batch, (int)(x + 108), (int)(y + 68), (int)(w - 216), 36);
        batch.setColor(0.3f, 0.7f, 1.0f, 0.9f);
        drawRectOutline(pix, batch, (int)(x + 108), (int)(y + 68), (int)(w - 216), 36);

        // Right Button [ СЛЕД ▶ ]
        batch.setColor(0.14f, 0.24f, 0.38f, 1.0f);
        pix.draw(batch, (int)(x + w - 103), (int)(y + 68), 75, 36);
        batch.setColor(0.25f, 0.6f, 0.9f, 0.8f);
        drawRectOutline(pix, batch, (int)(x + w - 103), (int)(y + 68), 75, 36);

        // Filter Toggle Button
        batch.setColor(filterAlliesOnly ? 0.16f : 0.08f, filterAlliesOnly ? 0.32f : 0.18f, filterAlliesOnly ? 0.2f : 0.28f, 1.0f);
        pix.draw(batch, (int)(x + 28), (int)(y + 112), (int)(w - 56), 28);
        batch.setColor(0.2f, 0.5f, 0.7f, 0.7f);
        drawRectOutline(pix, batch, (int)(x + 28), (int)(y + 112), (int)(w - 56), 28);

        // Labels for Country Selector
        if (font != null) {
            font.setColor(1.0f, 1.0f, 1.0f, 1.0f);
            glyphLayout.setText(font, "◀ ПРЕД");
            font.draw(batch, "◀ ПРЕД", x + 34, y + 78);
            glyphLayout.setText(font, "СЛЕД ▶");
            font.draw(batch, "СЛЕД ▶", x + w - 97, y + 78);

            String targetName = target != null ? target.getLocalizedName() : "Нет доступных стран";
            String relBadge = "";
            if (target != null && player != null) {
                if (target.isAlly() || (player.allianceName != null && player.allianceName.equals(target.allianceName))) {
                    relBadge = " [СОЮЗНИК]";
                } else if (target.hasWarGoalAgainst(gm.getWarManager() != null ? gm.getWarManager().getColorKeyByName(player.getName()) : -1)) {
                    relBadge = " [ВОЙНА]";
                } else {
                    relBadge = " [НЕЙТРАЛ, +" + target.getAiRelation(player.getName()) + "]";
                }
            }
            String countryCardText = "🎯 Получатель: " + targetName + relBadge + " (список ▾)";
            glyphLayout.setText(font, countryCardText);
            font.draw(batch, countryCardText, x + 108 + (w - 216 - glyphLayout.width) / 2.0f, y + 78);

            String filterText = filterAlliesOnly ? "🔍 Фильтр: ТОЛЬКО СОЮЗНИКИ (клик для показа всех)" : "🔍 Фильтр: ВСЕ ГОСУДАРСТВА МИРА (клик для фильтра союзников)";
            font.setColor(0.6f, 0.85f, 1.0f, 1.0f);
            glyphLayout.setText(font, filterText);
            font.draw(batch, filterText, x + 28 + (w - 56 - glyphLayout.width) / 2.0f, y + 118);
        }

        // 3. Treasury Status Information
        long playerGold = player != null ? player.getGold() : 0L;
        long targetGold = target != null ? target.getGold() : 0L;
        if (font != null) {
            font.setColor(1.0f, 0.85f, 0.3f, 1.0f);
            font.draw(batch, "💰 Ваша казна: " + playerGold + " 🪙", x + 32, y + 158);
            font.setColor(0.85f, 0.85f, 0.85f, 1.0f);
            font.draw(batch, "🏛️ Казна " + (target != null ? target.getLocalizedName() : "цели") + ": " + targetGold + " 🪙", x + 32, y + 184);
        }

        // 4. Gold Aid Buttons (+10 000, +50 000, +250 000)
        float goldY = y + 218;
        // Button 1: +10 000
        batch.setColor(0.22f, 0.18f, 0.08f, 1.0f);
        pix.draw(batch, (int)(x + 28), (int)goldY, 170, 44);
        batch.setColor(0.9f, 0.75f, 0.2f, 0.9f);
        drawRectOutline(pix, batch, (int)(x + 28), (int)goldY, 170, 44);

        // Button 2: +50 000
        batch.setColor(0.24f, 0.19f, 0.08f, 1.0f);
        pix.draw(batch, (int)(x + 212), (int)goldY, 170, 44);
        batch.setColor(0.95f, 0.8f, 0.25f, 0.9f);
        drawRectOutline(pix, batch, (int)(x + 212), (int)goldY, 170, 44);

        // Button 3: +250 000
        batch.setColor(0.28f, 0.2f, 0.08f, 1.0f);
        pix.draw(batch, (int)(x + 396), (int)goldY, (int)(w - 424), 44);
        batch.setColor(1.0f, 0.85f, 0.3f, 0.9f);
        drawRectOutline(pix, batch, (int)(x + 396), (int)goldY, (int)(w - 424), 44);

        if (font != null) {
            font.setColor(1.0f, 0.9f, 0.3f, 1.0f);
            glyphLayout.setText(font, "+10 000 🪙");
            font.draw(batch, "+10 000 🪙", x + 28 + (170 - glyphLayout.width) / 2.0f, goldY + 14);

            glyphLayout.setText(font, "+50 000 🪙");
            font.draw(batch, "+50 000 🪙", x + 212 + (170 - glyphLayout.width) / 2.0f, goldY + 14);

            glyphLayout.setText(font, "+250 000 🪙");
            font.draw(batch, "+250 000 🪙", x + 396 + (w - 424 - glyphLayout.width) / 2.0f, goldY + 14);
        }

        // 5. Troop Transfer Button
        float troopY = y + 288;
        batch.setColor(0.12f, 0.24f, 0.16f, 1.0f);
        pix.draw(batch, (int)(x + 28), (int)troopY, (int)(w - 56), 46);
        batch.setColor(0.3f, 0.85f, 0.45f, 0.9f);
        drawRectOutline(pix, batch, (int)(x + 28), (int)troopY, (int)(w - 56), 46);

        if (font != null) {
            font.setColor(0.45f, 1.0f, 0.6f, 1.0f);
            String troopTitle = "⚔️ ПЕРЕДАТЬ АРМЕЙСКИЙ КОРПУС СОЮЗНИКУ";
            glyphLayout.setText(font, troopTitle);
            font.draw(batch, troopTitle, x + 28 + (w - 56 - glyphLayout.width) / 2.0f, troopY + 10);

            font.setColor(0.7f, 0.85f, 0.75f, 1.0f);
            String troopSub = "(Мгновенно передаёт 1 дивизию под управление союзного командования)";
            glyphLayout.setText(font, troopSub);
            font.draw(batch, troopSub, x + 28 + (w - 56 - glyphLayout.width) / 2.0f, troopY + 28);
        }

        // 6. Close Button (at bottom)
        float closeBtnW = 180;
        float closeBtnH = 38;
        float closeBtnX = x + (w - closeBtnW) / 2.0f;
        float closeBtnY = y + h - 50;

        batch.setColor(0.35f, 0.12f, 0.12f, 1.0f);
        pix.draw(batch, (int)closeBtnX, (int)closeBtnY, (int)closeBtnW, (int)closeBtnH);
        batch.setColor(0.9f, 0.3f, 0.3f, 0.9f);
        drawRectOutline(pix, batch, (int)closeBtnX, (int)closeBtnY, (int)closeBtnW, (int)closeBtnH);

        if (font != null) {
            font.setColor(1.0f, 0.85f, 0.85f, 1.0f);
            String closeText = "ЗАКРЫТЬ (ESC)";
            glyphLayout.setText(font, closeText);
            font.draw(batch, closeText, closeBtnX + (closeBtnW - glyphLayout.width) / 2.0f, closeBtnY + 11);
        }

        // 7. Render Country Selection Popup if open
        if (countryListPopup) {
            renderCountryListPopup(batch, font, pix, x + 30, y + 105, w - 60, 260, countries);
        }

        batch.setColor(Color.WHITE);
    }

    private void renderCountryListPopup(SpriteBatch batch, BitmapFont font, Image pix, float px, float py, float pw, float ph, List<CountryState> countries) {
        batch.setColor(0.04f, 0.07f, 0.12f, 0.99f);
        pix.draw(batch, (int)px, (int)py, (int)pw, (int)ph);
        batch.setColor(0.2f, 0.7f, 1.0f, 1.0f);
        drawRectOutline(pix, batch, (int)px, (int)py, (int)pw, (int)ph);

        // Popup Title
        if (font != null) {
            font.setColor(0.3f, 0.9f, 1.0f, 1.0f);
            String t = "Выберите державу из списка (Стр. " + (listScrollOffset / 6 + 1) + "/" + Math.max(1, (countries.size() + 5) / 6) + "):";
            font.draw(batch, t, px + 12, py + 12);
        }

        float itemH = 34;
        for (int i = 0; i < 6; i++) {
            int cIdx = listScrollOffset + i;
            if (cIdx >= countries.size()) break;
            CountryState cs = countries.get(cIdx);
            float iy = py + 38 + i * itemH;

            boolean isCur = (cIdx == selectedCountryIndex);
            batch.setColor(isCur ? 0.16f : 0.09f, isCur ? 0.28f : 0.14f, isCur ? 0.44f : 0.22f, 1.0f);
            pix.draw(batch, (int)(px + 8), (int)iy, (int)(pw - 16), (int)(itemH - 4));
            if (isCur) {
                batch.setColor(0.3f, 0.8f, 1.0f, 0.9f);
                drawRectOutline(pix, batch, (int)(px + 8), (int)iy, (int)(pw - 16), (int)(itemH - 4));
            }

            if (font != null) {
                font.setColor(isCur ? Color.YELLOW : Color.WHITE);
                String line = (cIdx + 1) + ". " + cs.getLocalizedName() + " (Казна: " + cs.getGold() + " 🪙)";
                font.draw(batch, line, px + 16, iy + 8);
            }
        }

        // Paging buttons
        batch.setColor(0.12f, 0.2f, 0.32f, 1.0f);
        pix.draw(batch, (int)px, (int)(py + ph - 34), 100, 34);
        pix.draw(batch, (int)(px + pw - 100), (int)(py + ph - 34), 100, 34);
        batch.setColor(0.3f, 0.6f, 0.9f, 0.8f);
        drawRectOutline(pix, batch, (int)px, (int)(py + ph - 34), 100, 34);
        drawRectOutline(pix, batch, (int)(px + pw - 100), (int)(py + ph - 34), 100, 34);

        if (font != null) {
            font.setColor(Color.WHITE);
            font.draw(batch, "◀ Назад", px + 18, py + ph - 22);
            font.draw(batch, "Вперёд ▶", px + pw - 86, py + ph - 22);
        }
    }

    private void drawRectOutline(Image pix, SpriteBatch batch, int x, int y, int w, int h) {
        pix.draw(batch, x, y, w, 2);
        pix.draw(batch, x, y + h - 2, w, 2);
        pix.draw(batch, x, y, 2, h);
        pix.draw(batch, x + w - 2, y, 2, h);
    }

    // InputProcessor implementation to fully block click-through to map!
    @Override
    public boolean keyDown(int keycode) {
        if (!visible) return false;
        if (keycode == Input.Keys.ESCAPE || keycode == Input.Keys.F7) {
            playClickSound();
            visible = false;
            countryListPopup = false;
            return true;
        }
        return true;
    }

    @Override
    public boolean keyUp(int keycode) {
        return visible;
    }

    @Override
    public boolean keyTyped(char character) {
        return visible;
    }

    @Override
    public boolean touchCancelled(int screenX, int screenY, int pointer, int button) {
        return visible;
    }

    @Override
    public boolean touchDown(int screenX, int screenY, int pointer, int button) {
        if (!visible) return false;
        cgs.modloader.ModLoader ml = cgs.modloader.ModLoader.getInstance();
        ru.ius.david.cgs.cgsGame g = (ml != null) ? ml.getGame() : null;
        MultiplayerManager mpMgr = (g != null) ? g.getActiveMultiplayerManager() : null;
        handleClick(screenX, screenY, CFG.GAME_WIDTH, CFG.GAME_HEIGHT, GameManager.getInstance(), mpMgr);
        return true; // ALWAYS CONSUME: map will never receive click!
    }

    @Override
    public boolean touchUp(int screenX, int screenY, int pointer, int button) {
        return visible;
    }

    @Override
    public boolean touchDragged(int screenX, int screenY, int pointer) {
        return visible; // Consume dragging so map does not pan
    }

    @Override
    public boolean mouseMoved(int screenX, int screenY) {
        return visible;
    }

    @Override
    public boolean scrolled(float amountX, float amountY) {
        return visible; // Consume scroll so map does not zoom
    }
}
