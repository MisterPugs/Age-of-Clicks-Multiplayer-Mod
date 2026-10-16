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
import com.badlogic.gdx.math.MathUtils;
import ru.ius.david.cgs.CFG;
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

    // Animation & Feedback states
    private float openAnim = 0.0f;
    private float pulseTimer = 0.0f;
    private String feedbackText = null;
    private float feedbackTimer = 0.0f;
    private Color feedbackColor = Color.YELLOW;

    private DirectAidDialog() {}

    public boolean isVisible() {
        return visible;
    }

    public void setVisible(boolean visible) {
        this.visible = visible;
        if (visible) {
            this.openAnim = 0.0f;
            this.feedbackTimer = 0.0f;
            this.countryListPopup = false;
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

        float w = 720;
        float h = 530;
        float x = (screenWidth - w) / 2.0f;
        float y = (screenHeight - h) / 2.0f;

        // If click is outside dialog window -> close dialog and CONSUME CLICK
        if (mouseX < x || mouseX > x + w || mouseY < y || mouseY > y + h) {
            playClickSound();
            visible = false;
            countryListPopup = false;
            return true;
        }

        List<CountryState> countries = getAvailableCountries(gm);
        CountryState player = gm.getPlayerCountry();

        // 1. Close button (at bottom)
        float closeBtnW = 190;
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
            float popH = 340;
            float popX = x + 30;
            float popY = y + 70;

            // Prev / Next page buttons inside popup
            float popPagingY = popY + popH - 38;
            if (mouseY >= popPagingY && mouseY <= popPagingY + 32) {
                if (mouseX >= popX + 10 && mouseX <= popX + 110) {
                    playClickSound();
                    listScrollOffset = Math.max(0, listScrollOffset - 7);
                    return true;
                } else if (mouseX >= popX + popW - 110 && mouseX <= popX + popW - 10) {
                    playClickSound();
                    if (listScrollOffset + 7 < countries.size()) {
                        listScrollOffset += 7;
                    }
                    return true;
                }
            }

            // Click on a country item
            float itemH = 36;
            for (int i = 0; i < 7; i++) {
                int cIdx = listScrollOffset + i;
                if (cIdx >= countries.size()) break;
                float itemY = popY + 42 + i * itemH;
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
            float prevBtnY = y + 88;
            float prevBtnW = 85;
            float prevBtnH = 46;
            if (mouseX >= prevBtnX && mouseX <= prevBtnX + prevBtnW && mouseY >= prevBtnY && mouseY <= prevBtnY + prevBtnH) {
                playClickSound();
                selectedCountryIndex = (selectedCountryIndex - 1 + countries.size()) % countries.size();
                return true;
            }

            // 3. Country Display Card (Clicking opens full list popup)
            float cardX = x + 121;
            float cardY = y + 88;
            float cardW = w - 242;
            float cardH = 46;
            if (mouseX >= cardX && mouseX <= cardX + cardW && mouseY >= cardY && mouseY <= cardY + cardH) {
                playClickSound();
                countryListPopup = true;
                listScrollOffset = (selectedCountryIndex / 7) * 7;
                return true;
            }

            // 4. Next Country Button [ СЛЕД ▶ ]
            float nextBtnX = x + w - 113;
            float nextBtnY = y + 88;
            float nextBtnW = 85;
            float nextBtnH = 46;
            if (mouseX >= nextBtnX && mouseX <= nextBtnX + nextBtnW && mouseY >= nextBtnY && mouseY <= nextBtnY + nextBtnH) {
                playClickSound();
                selectedCountryIndex = (selectedCountryIndex + 1) % countries.size();
                return true;
            }

            // 5. Filter Toggle
            float filterBtnX = x + 28;
            float filterBtnY = y + 142;
            float filterBtnW = w - 56;
            float filterBtnH = 32;
            if (mouseX >= filterBtnX && mouseX <= filterBtnX + filterBtnW && mouseY >= filterBtnY && mouseY <= filterBtnY + filterBtnH) {
                playClickSound();
                filterAlliesOnly = !filterAlliesOnly;
                selectedCountryIndex = 0;
                return true;
            }

            // 6. Gold Aid Buttons (+10k, +50k, +250k)
            float goldBtnsY = y + 234;
            float goldBtnH = 46;
            float goldBtnW = (w - 76) / 3.0f;
            if (mouseY >= goldBtnsY && mouseY <= goldBtnsY + goldBtnH && !isEnemy) {
                // Button 1: +10 000
                if (mouseX >= x + 28 && mouseX <= x + 28 + goldBtnW) {
                    playClickSound();
                    sendGold(gm, mpMgr, targetColorKey, target.getName(), 10000L);
                    return true;
                }
                // Button 2: +50 000
                else if (mouseX >= x + 38 + goldBtnW && mouseX <= x + 38 + goldBtnW * 2) {
                    playClickSound();
                    sendGold(gm, mpMgr, targetColorKey, target.getName(), 50000L);
                    return true;
                }
                // Button 3: +250 000
                else if (mouseX >= x + 48 + goldBtnW * 2 && mouseX <= x + 48 + goldBtnW * 3) {
                    playClickSound();
                    sendGold(gm, mpMgr, targetColorKey, target.getName(), 250000L);
                    return true;
                }
            }

            // 7. Troop Transfer Button
            float troopBtnX = x + 28;
            float troopBtnY = y + 292;
            float troopBtnW = w - 56;
            float troopBtnH = 58;
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

    private void triggerFeedback(String text, Color color) {
        this.feedbackText = text;
        this.feedbackTimer = 1.8f;
        this.feedbackColor = color;
    }

    private void sendGold(GameManager gm, MultiplayerManager mpMgr, int targetColorKey, String targetName, long amount) {
        CountryState player = gm.getPlayerCountry();
        if (player != null && player.getGold() < amount) {
            triggerFeedback("⚠️ Недостаточно золота в казне!", new Color(1.0f, 0.4f, 0.4f, 1.0f));
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
        triggerFeedback("🪙 +" + amount + " передано для " + targetName + "!", new Color(1.0f, 0.9f, 0.2f, 1.0f));
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
                triggerFeedback("⚔️ Корпус передан державе " + targetName + "!", new Color(0.4f, 1.0f, 0.5f, 1.0f));
                MapScreen ms = MapScreen.getActiveInstance();
                if (ms != null) ms.showToast("⚔️ Армейский корпус успешно передан державе " + targetName + "!");
                return;
            }
        }

        triggerFeedback("⚠️ Нет свободных корпусов для передачи!", new Color(1.0f, 0.4f, 0.4f, 1.0f));
        MapScreen ms = MapScreen.getActiveInstance();
        if (ms != null) {
            ms.showToast("⚠️ У вас нет свободных корпусов для передачи!");
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

    public void render(SpriteBatch batch, ShapeRenderer shapes, BitmapFont font, float screenWidth, float screenHeight, GameManager gm) {
        if (!visible || gm == null) return;
        ensureInputProcessorAttached();

        float delta = Gdx.graphics.getDeltaTime();
        pulseTimer += delta;
        openAnim = Math.min(1.0f, openAnim + delta * 5.0f);
        float ease = 1.0f - (float)Math.pow(1.0f - openAnim, 3);

        float baseW = 720;
        float baseH = 530;
        float scale = 0.92f + 0.08f * ease;
        float w = baseW * scale;
        float h = baseH * scale;
        float x = (screenWidth - w) / 2.0f;
        float y = (screenHeight - h) / 2.0f;

        float mouseX = Gdx.input.getX();
        float mouseY = Gdx.input.getY();

        Image pix = ImageManager.getImage(Images.pix255_255_255);
        if (pix == null) return;

        // Dim background veil with fade
        batch.setColor(0.0f, 0.0f, 0.0f, 0.5f * ease);
        pix.draw(batch, 0, 0, (int)screenWidth, (int)screenHeight);

        // Main Dialog Panel Background
        batch.setColor(0.04f, 0.07f, 0.12f, 0.98f * ease);
        pix.draw(batch, (int)x, (int)y, (int)w, (int)h);

        // Cyber scan line
        float scanY = y + ((pulseTimer * 75.0f) % h);
        batch.setColor(0.2f, 0.7f, 1.0f, 0.07f * ease);
        pix.draw(batch, (int)x, (int)scanY, (int)w, 3);

        // Header Background Bar
        batch.setColor(0.07f, 0.18f, 0.32f, 1.0f * ease);
        pix.draw(batch, (int)x, (int)y, (int)w, 48);

        // Animated neon outer border
        float pulse = 0.85f + 0.15f * MathUtils.sin(pulseTimer * 3.5f);
        batch.setColor(0.2f * pulse, 0.7f * pulse, 1.0f * pulse, 0.95f * ease);
        drawRectOutline(pix, batch, (int)x, (int)y, (int)w, (int)h, 2);

        // Header bottom separator
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

        List<CountryState> countries = getAvailableCountries(gm);
        CountryState player = gm.getPlayerCountry();
        CountryState target = (!countries.isEmpty() && selectedCountryIndex < countries.size()) ? countries.get(selectedCountryIndex) : null;

        // 1. Header Title Text
        if (font != null) {
            font.setColor(0.35f, 0.92f, 1.0f, ease);
            String title = "🤝 СЕТЕВОЙ ЛЕНД-ЛИЗ И ВОЕННАЯ ПОМОЩЬ (F7)";
            glyphLayout.setText(font, title);
            font.draw(batch, title, x + (w - glyphLayout.width) / 2.0f, y + 14);

            // Row 1 Label: Section Header
            font.setColor(0.6f, 0.85f, 1.0f, ease);
            String secLabel = "🎯 ВЫБЕРИТЕ ГОСУДАРСТВО-ПОЛУЧАТЕЛЬ ПОМОЩИ:";
            glyphLayout.setText(font, secLabel);
            font.draw(batch, secLabel, x + (w - glyphLayout.width) / 2.0f, y + 62);
        }

        // 2. Country Selector Section (Row 2, y + 88, h = 46)
        float prevBtnX = x + 28;
        float prevBtnY = y + 88;
        float prevBtnW = 85;
        float prevBtnH = 46;
        boolean prevHover = (mouseX >= prevBtnX && mouseX <= prevBtnX + prevBtnW && mouseY >= prevBtnY && mouseY <= prevBtnY + prevBtnH);

        float nextBtnX = x + w - 113;
        float nextBtnY = y + 88;
        float nextBtnW = 85;
        float nextBtnH = 46;
        boolean nextHover = (mouseX >= nextBtnX && mouseX <= nextBtnX + nextBtnW && mouseY >= nextBtnY && mouseY <= nextBtnY + nextBtnH);

        float cardX = x + 121;
        float cardY = y + 88;
        float cardW = w - 242;
        float cardH = 46;
        boolean cardHover = (mouseX >= cardX && mouseX <= cardX + cardW && mouseY >= cardY && mouseY <= cardY + cardH);

        // Prev Button
        batch.setColor(prevHover ? 0.18f : 0.11f, prevHover ? 0.28f : 0.18f, prevHover ? 0.44f : 0.28f, 1.0f * ease);
        pix.draw(batch, (int)prevBtnX, (int)prevBtnY, (int)prevBtnW, (int)prevBtnH);
        batch.setColor(prevHover ? 0.4f : 0.25f, prevHover ? 0.8f : 0.5f, prevHover ? 1.0f : 0.75f, 0.9f * ease);
        drawRectOutline(pix, batch, (int)prevBtnX, (int)prevBtnY, (int)prevBtnW, (int)prevBtnH, prevHover ? 2 : 1);

        // Center Country Display Card
        batch.setColor(cardHover ? 0.13f : 0.08f, cardHover ? 0.22f : 0.14f, cardHover ? 0.35f : 0.22f, 1.0f * ease);
        pix.draw(batch, (int)cardX, (int)cardY, (int)cardW, (int)cardH);
        batch.setColor(cardHover ? 0.45f : 0.3f, cardHover ? 0.85f : 0.65f, cardHover ? 1.0f : 0.9f, 0.9f * ease);
        drawRectOutline(pix, batch, (int)cardX, (int)cardY, (int)cardW, (int)cardH, cardHover ? 2 : 1);

        // Next Button
        batch.setColor(nextHover ? 0.18f : 0.11f, nextHover ? 0.28f : 0.18f, nextHover ? 0.44f : 0.28f, 1.0f * ease);
        pix.draw(batch, (int)nextBtnX, (int)nextBtnY, (int)nextBtnW, (int)nextBtnH);
        batch.setColor(nextHover ? 0.4f : 0.25f, nextHover ? 0.8f : 0.5f, nextHover ? 1.0f : 0.75f, 0.9f * ease);
        drawRectOutline(pix, batch, (int)nextBtnX, (int)nextBtnY, (int)nextBtnW, (int)nextBtnH, nextHover ? 2 : 1);

        // Filter Toggle Button (Row 3, y + 142, h = 32)
        float filterBtnX = x + 28;
        float filterBtnY = y + 142;
        float filterBtnW = w - 56;
        float filterBtnH = 32;
        boolean filterHover = (mouseX >= filterBtnX && mouseX <= filterBtnX + filterBtnW && mouseY >= filterBtnY && mouseY <= filterBtnY + filterBtnH);

        batch.setColor(filterAlliesOnly ? (filterHover ? 0.18f : 0.12f) : (filterHover ? 0.12f : 0.07f),
                       filterAlliesOnly ? (filterHover ? 0.38f : 0.28f) : (filterHover ? 0.22f : 0.15f),
                       filterAlliesOnly ? (filterHover ? 0.25f : 0.18f) : (filterHover ? 0.35f : 0.25f), 1.0f * ease);
        pix.draw(batch, (int)filterBtnX, (int)filterBtnY, (int)filterBtnW, (int)filterBtnH);
        batch.setColor(filterAlliesOnly ? 0.3f : 0.25f, filterAlliesOnly ? 0.8f : 0.55f, filterAlliesOnly ? 0.5f : 0.8f, 0.85f * ease);
        drawRectOutline(pix, batch, (int)filterBtnX, (int)filterBtnY, (int)filterBtnW, (int)filterBtnH, filterHover ? 2 : 1);

        // Labels for Country Selector & Filter
        if (font != null) {
            font.setColor(prevHover ? Color.WHITE : Color.LIGHT_GRAY);
            glyphLayout.setText(font, "◀ ПРЕД");
            font.draw(batch, "◀ ПРЕД", prevBtnX + (prevBtnW - glyphLayout.width) / 2.0f, prevBtnY + 14);

            font.setColor(nextHover ? Color.WHITE : Color.LIGHT_GRAY);
            glyphLayout.setText(font, "СЛЕД ▶");
            font.draw(batch, "СЛЕД ▶", nextBtnX + (nextBtnW - glyphLayout.width) / 2.0f, nextBtnY + 14);

            String targetName = target != null ? target.getLocalizedName() : "Нет доступных стран";
            String relBadge = "";
            Color relColor = Color.YELLOW;
            if (target != null && player != null) {
                if (target.isAlly() || (player.allianceName != null && player.allianceName.equals(target.allianceName))) {
                    relBadge = " [СОЮЗНИК]";
                    relColor = new Color(0.4f, 1.0f, 0.5f, ease);
                } else if (target.hasWarGoalAgainst(gm.getWarManager() != null ? gm.getWarManager().getColorKeyByName(player.getName()) : -1)) {
                    relBadge = " [ВОЙНА]";
                    relColor = new Color(1.0f, 0.35f, 0.35f, ease);
                } else {
                    relBadge = " [НЕЙТРАЛ, +" + target.getAiRelation(player.getName()) + "]";
                    relColor = new Color(0.9f, 0.85f, 0.4f, ease);
                }
            }

            // Line 1 inside Card: Country Name + Badge (measured & truncated to never overflow card)
            String fullCardLine = "👑 " + targetName + relBadge;
            String cleanCardLine = truncate(font, fullCardLine, cardW - 20);
            font.setColor(cardHover ? Color.WHITE : relColor);
            glyphLayout.setText(font, cleanCardLine);
            font.draw(batch, cleanCardLine, cardX + (cardW - glyphLayout.width) / 2.0f, cardY + 8);

            // Line 2 inside Card: Hint
            font.setColor(0.5f, 0.8f, 0.95f, ease);
            String cardHint = "(нажмите для выбора из списка держав ▾)";
            glyphLayout.setText(font, cardHint);
            font.draw(batch, cardHint, cardX + (cardW - glyphLayout.width) / 2.0f, cardY + 27);

            // Filter toggle label
            String filterText = filterAlliesOnly ? "🔍 Фильтр: ТОЛЬКО СОЮЗНИКИ (клик для показа всех держав)" : "🔍 Фильтр: ВСЕ ГОСУДАРСТВА МИРА (клик для фильтра союзников)";
            String cleanFilterText = truncate(font, filterText, filterBtnW - 20);
            font.setColor(filterHover ? Color.WHITE : (filterAlliesOnly ? new Color(0.5f, 1.0f, 0.6f, ease) : new Color(0.65f, 0.85f, 1.0f, ease)));
            glyphLayout.setText(font, cleanFilterText);
            font.draw(batch, cleanFilterText, filterBtnX + (filterBtnW - glyphLayout.width) / 2.0f, filterBtnY + 7);
        }

        // 3. Treasury Status Cards (Row 4, y + 184, h = 40)
        long playerGold = player != null ? player.getGold() : 0L;
        long targetGold = target != null ? target.getGold() : 0L;
        float cardHalfW = (w - 66) / 2.0f;

        // Player Treasury Card
        batch.setColor(0.08f, 0.12f, 0.18f, 1.0f * ease);
        pix.draw(batch, (int)(x + 28), (int)(y + 184), (int)cardHalfW, 40);
        batch.setColor(0.85f, 0.7f, 0.2f, 0.7f * ease);
        drawRectOutline(pix, batch, (int)(x + 28), (int)(y + 184), (int)cardHalfW, 40, 1);

        // Target Treasury Card
        batch.setColor(0.08f, 0.12f, 0.18f, 1.0f * ease);
        pix.draw(batch, (int)(x + 38 + cardHalfW), (int)(y + 184), (int)cardHalfW, 40);
        batch.setColor(0.4f, 0.65f, 0.85f, 0.7f * ease);
        drawRectOutline(pix, batch, (int)(x + 38 + cardHalfW), (int)(y + 184), (int)cardHalfW, 40, 1);

        if (font != null) {
            font.setColor(1.0f, 0.88f, 0.35f, ease);
            String pGoldStr = "💰 Ваша казна: " + playerGold + " 🪙";
            glyphLayout.setText(font, pGoldStr);
            font.draw(batch, pGoldStr, x + 28 + (cardHalfW - glyphLayout.width) / 2.0f, y + 194);

            font.setColor(0.75f, 0.9f, 1.0f, ease);
            String tName = target != null ? target.getLocalizedName() : "цели";
            String tGoldStr = truncate(font, "🏛️ Казна " + tName + ": " + targetGold + " 🪙", cardHalfW - 16);
            glyphLayout.setText(font, tGoldStr);
            font.draw(batch, tGoldStr, x + 38 + cardHalfW + (cardHalfW - glyphLayout.width) / 2.0f, y + 194);
        }

        // 4. Gold Aid Buttons (+10 000, +50 000, +250 000) (Row 5, y + 234, h = 46)
        float goldBtnsY = y + 234;
        float goldBtnH = 46;
        float goldBtnW = (w - 76) / 3.0f;

        float g1X = x + 28;
        float g2X = x + 38 + goldBtnW;
        float g3X = x + 48 + goldBtnW * 2;

        boolean g1Hover = (mouseX >= g1X && mouseX <= g1X + goldBtnW && mouseY >= goldBtnsY && mouseY <= goldBtnsY + goldBtnH);
        boolean g2Hover = (mouseX >= g2X && mouseX <= g2X + goldBtnW && mouseY >= goldBtnsY && mouseY <= goldBtnsY + goldBtnH);
        boolean g3Hover = (mouseX >= g3X && mouseX <= g3X + goldBtnW && mouseY >= goldBtnsY && mouseY <= goldBtnsY + goldBtnH);

        // Button 1: +10 000
        batch.setColor(g1Hover ? 0.32f : 0.2f, g1Hover ? 0.25f : 0.16f, 0.07f, 1.0f * ease);
        pix.draw(batch, (int)g1X, (int)goldBtnsY, (int)goldBtnW, (int)goldBtnH);
        batch.setColor(g1Hover ? 1.0f : 0.85f, g1Hover ? 0.9f : 0.7f, 0.25f, 0.9f * ease);
        drawRectOutline(pix, batch, (int)g1X, (int)goldBtnsY, (int)goldBtnW, (int)goldBtnH, g1Hover ? 2 : 1);

        // Button 2: +50 000
        batch.setColor(g2Hover ? 0.34f : 0.22f, g2Hover ? 0.26f : 0.17f, 0.07f, 1.0f * ease);
        pix.draw(batch, (int)g2X, (int)goldBtnsY, (int)goldBtnW, (int)goldBtnH);
        batch.setColor(g2Hover ? 1.0f : 0.9f, g2Hover ? 0.92f : 0.75f, 0.25f, 0.9f * ease);
        drawRectOutline(pix, batch, (int)g2X, (int)goldBtnsY, (int)goldBtnW, (int)goldBtnH, g2Hover ? 2 : 1);

        // Button 3: +250 000
        batch.setColor(g3Hover ? 0.36f : 0.24f, g3Hover ? 0.27f : 0.18f, 0.07f, 1.0f * ease);
        pix.draw(batch, (int)g3X, (int)goldBtnsY, (int)goldBtnW, (int)goldBtnH);
        batch.setColor(g3Hover ? 1.0f : 0.95f, g3Hover ? 0.95f : 0.8f, 0.3f, 0.9f * ease);
        drawRectOutline(pix, batch, (int)g3X, (int)goldBtnsY, (int)goldBtnW, (int)goldBtnH, g3Hover ? 2 : 1);

        if (font != null) {
            font.setColor(g1Hover ? Color.WHITE : new Color(1.0f, 0.9f, 0.35f, ease));
            glyphLayout.setText(font, "+10 000 🪙");
            font.draw(batch, "+10 000 🪙", g1X + (goldBtnW - glyphLayout.width) / 2.0f, goldBtnsY + 14);

            font.setColor(g2Hover ? Color.WHITE : new Color(1.0f, 0.92f, 0.4f, ease));
            glyphLayout.setText(font, "+50 000 🪙");
            font.draw(batch, "+50 000 🪙", g2X + (goldBtnW - glyphLayout.width) / 2.0f, goldBtnsY + 14);

            font.setColor(g3Hover ? Color.WHITE : new Color(1.0f, 0.95f, 0.45f, ease));
            glyphLayout.setText(font, "+250 000 🪙");
            font.draw(batch, "+250 000 🪙", g3X + (goldBtnW - glyphLayout.width) / 2.0f, goldBtnsY + 14);
        }

        // 5. Troop Transfer Button (Row 6, y + 292, h = 58)
        float troopY = y + 292;
        float troopBtnW = w - 56;
        float troopBtnH = 58;
        boolean troopHover = (mouseX >= x + 28 && mouseX <= x + 28 + troopBtnW && mouseY >= troopY && mouseY <= troopY + troopBtnH);

        batch.setColor(troopHover ? 0.15f : 0.1f, troopHover ? 0.3f : 0.2f, troopHover ? 0.2f : 0.14f, 1.0f * ease);
        pix.draw(batch, (int)(x + 28), (int)troopY, (int)troopBtnW, (int)troopBtnH);
        batch.setColor(troopHover ? 0.45f : 0.3f, troopHover ? 0.95f : 0.8f, troopHover ? 0.6f : 0.45f, 0.9f * ease);
        drawRectOutline(pix, batch, (int)(x + 28), (int)troopY, (int)troopBtnW, (int)troopBtnH, troopHover ? 2 : 1);

        if (font != null) {
            font.setColor(troopHover ? Color.WHITE : new Color(0.45f, 1.0f, 0.65f, ease));
            String troopTitle = "⚔️ ПЕРЕДАТЬ АРМЕЙСКИЙ КОРПУС СОЮЗНИКУ";
            glyphLayout.setText(font, troopTitle);
            font.draw(batch, troopTitle, x + 28 + (troopBtnW - glyphLayout.width) / 2.0f, troopY + 10);

            font.setColor(0.7f, 0.88f, 0.78f, ease);
            String troopSub = truncate(font, "(Мгновенно передаёт 1 дивизию под управление союзного командования)", troopBtnW - 20);
            glyphLayout.setText(font, troopSub);
            font.draw(batch, troopSub, x + 28 + (troopBtnW - glyphLayout.width) / 2.0f, troopY + 31);
        }

        // 6. Close Button (at bottom)
        float closeBtnW = 190;
        float closeBtnH = 38;
        float closeBtnX = x + (w - closeBtnW) / 2.0f;
        float closeBtnY = y + h - 50;
        boolean closeHover = (mouseX >= closeBtnX && mouseX <= closeBtnX + closeBtnW && mouseY >= closeBtnY && mouseY <= closeBtnY + closeBtnH);

        batch.setColor(closeHover ? 0.45f : 0.3f, closeHover ? 0.14f : 0.1f, closeHover ? 0.14f : 0.1f, 1.0f * ease);
        pix.draw(batch, (int)closeBtnX, (int)closeBtnY, (int)closeBtnW, (int)closeBtnH);
        batch.setColor(closeHover ? 1.0f : 0.85f, closeHover ? 0.35f : 0.25f, closeHover ? 0.35f : 0.25f, 0.95f * ease);
        drawRectOutline(pix, batch, (int)closeBtnX, (int)closeBtnY, (int)closeBtnW, (int)closeBtnH, closeHover ? 2 : 1);

        if (font != null) {
            font.setColor(closeHover ? Color.WHITE : new Color(1.0f, 0.85f, 0.85f, ease));
            String closeText = "ЗАКРЫТЬ (ESC)";
            glyphLayout.setText(font, closeText);
            font.draw(batch, closeText, closeBtnX + (closeBtnW - glyphLayout.width) / 2.0f, closeBtnY + 11);
        }

        // 7. Floating Action Feedback Toast Animation
        if (feedbackTimer > 0.0f) {
            feedbackTimer -= delta;
            float fAlpha = Math.min(1.0f, feedbackTimer * 1.5f);
            float fFloatY = y + 360 - (1.8f - feedbackTimer) * 25.0f;

            if (font != null && feedbackText != null) {
                glyphLayout.setText(font, feedbackText);
                float fW = glyphLayout.width + 40;
                float fH = 34;
                float fX = x + (w - fW) / 2.0f;

                batch.setColor(0.05f, 0.1f, 0.15f, 0.92f * fAlpha);
                pix.draw(batch, (int)fX, (int)fFloatY, (int)fW, (int)fH);
                batch.setColor(feedbackColor.r, feedbackColor.g, feedbackColor.b, 0.95f * fAlpha);
                drawRectOutline(pix, batch, (int)fX, (int)fFloatY, (int)fW, (int)fH, 1);

                font.setColor(feedbackColor.r, feedbackColor.g, feedbackColor.b, fAlpha);
                font.draw(batch, feedbackText, fX + (fW - glyphLayout.width) / 2.0f, fFloatY + 8);
            }
        }

        // 8. Render Country Selection Popup if open
        if (countryListPopup) {
            renderCountryListPopup(batch, font, pix, x + 30, y + 70, w - 60, 340, countries, mouseX, mouseY);
        }

        batch.setColor(Color.WHITE);
    }

    private void renderCountryListPopup(SpriteBatch batch, BitmapFont font, Image pix, float px, float py, float pw, float ph, List<CountryState> countries, float mouseX, float mouseY) {
        batch.setColor(0.04f, 0.07f, 0.12f, 0.99f);
        pix.draw(batch, (int)px, (int)py, (int)pw, (int)ph);
        batch.setColor(0.3f, 0.8f, 1.0f, 1.0f);
        drawRectOutline(pix, batch, (int)px, (int)py, (int)pw, (int)ph, 2);

        // Popup Title
        if (font != null) {
            font.setColor(0.35f, 0.95f, 1.0f, 1.0f);
            int totalPages = Math.max(1, (countries.size() + 6) / 7);
            int curPage = listScrollOffset / 7 + 1;
            String t = "Выберите державу из списка (Стр. " + curPage + " / " + totalPages + ", Всего: " + countries.size() + "):";
            font.draw(batch, t, px + 14, py + 12);
        }

        float itemH = 36;
        for (int i = 0; i < 7; i++) {
            int cIdx = listScrollOffset + i;
            if (cIdx >= countries.size()) break;
            CountryState cs = countries.get(cIdx);
            float iy = py + 42 + i * itemH;

            boolean isCur = (cIdx == selectedCountryIndex);
            boolean isHover = (mouseX >= px + 8 && mouseX <= px + pw - 8 && mouseY >= iy && mouseY <= iy + itemH - 4);

            if (isCur) {
                batch.setColor(0.18f, 0.35f, 0.55f, 1.0f);
            } else if (isHover) {
                batch.setColor(0.12f, 0.24f, 0.38f, 1.0f);
            } else {
                batch.setColor(0.07f, 0.12f, 0.18f, 1.0f);
            }
            pix.draw(batch, (int)(px + 8), (int)iy, (int)(pw - 16), (int)(itemH - 4));

            if (isCur) {
                batch.setColor(1.0f, 0.85f, 0.3f, 0.95f);
                drawRectOutline(pix, batch, (int)(px + 8), (int)iy, (int)(pw - 16), (int)(itemH - 4), 2);
            } else if (isHover) {
                batch.setColor(0.35f, 0.75f, 1.0f, 0.8f);
                drawRectOutline(pix, batch, (int)(px + 8), (int)iy, (int)(pw - 16), (int)(itemH - 4), 1);
            }

            if (font != null) {
                font.setColor(isCur ? Color.YELLOW : (isHover ? Color.WHITE : Color.LIGHT_GRAY));
                String line = truncate(font, (cIdx + 1) + ". " + cs.getLocalizedName() + " (Казна: " + cs.getGold() + " 🪙)", pw - 36);
                font.draw(batch, line, px + 18, iy + 9);
            }
        }

        // Paging buttons
        float popPagingY = py + ph - 38;
        boolean pPrevHover = (mouseX >= px + 10 && mouseX <= px + 110 && mouseY >= popPagingY && mouseY <= popPagingY + 32);
        boolean pNextHover = (mouseX >= px + pw - 110 && mouseX <= px + pw - 10 && mouseY >= popPagingY && mouseY <= popPagingY + 32);

        batch.setColor(pPrevHover ? 0.18f : 0.1f, pPrevHover ? 0.28f : 0.16f, 0.32f, 1.0f);
        pix.draw(batch, (int)(px + 10), (int)popPagingY, 100, 32);
        batch.setColor(pPrevHover ? 0.45f : 0.25f, 0.75f, 1.0f, 0.85f);
        drawRectOutline(pix, batch, (int)(px + 10), (int)popPagingY, 100, 32, 1);

        batch.setColor(pNextHover ? 0.18f : 0.1f, pNextHover ? 0.28f : 0.16f, 0.32f, 1.0f);
        pix.draw(batch, (int)(px + pw - 110), (int)popPagingY, 100, 32);
        batch.setColor(pNextHover ? 0.45f : 0.25f, 0.75f, 1.0f, 0.85f);
        drawRectOutline(pix, batch, (int)(px + pw - 110), (int)popPagingY, 100, 32, 1);

        if (font != null) {
            font.setColor(pPrevHover ? Color.WHITE : Color.LIGHT_GRAY);
            glyphLayout.setText(font, "◀ Назад");
            font.draw(batch, "◀ Назад", px + 10 + (100 - glyphLayout.width) / 2.0f, popPagingY + 8);

            font.setColor(pNextHover ? Color.WHITE : Color.LIGHT_GRAY);
            glyphLayout.setText(font, "Вперёд ▶");
            font.draw(batch, "Вперёд ▶", px + pw - 110 + (100 - glyphLayout.width) / 2.0f, popPagingY + 8);
        }
    }

    private void drawRectOutline(Image pix, SpriteBatch batch, int x, int y, int w, int h, int thickness) {
        pix.draw(batch, x, y, w, thickness);
        pix.draw(batch, x, y + h - thickness, w, thickness);
        pix.draw(batch, x, y, thickness, h);
        pix.draw(batch, x + w - thickness, y, thickness, h);
    }

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
        return visible;
    }

    @Override
    public boolean mouseMoved(int screenX, int screenY) {
        return visible;
    }

    @Override
    public boolean scrolled(float amountX, float amountY) {
        return visible;
    }
}
