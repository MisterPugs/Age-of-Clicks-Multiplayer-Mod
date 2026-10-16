package cgs.mods.multiplayer.diplomacy;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.MathUtils;
import java.util.ArrayList;
import java.util.List;
import ru.ius.david.cgs.CFG;
import ru.ius.david.cgs.cgsGame;
import ru.ius.david.cgs.game.CountryState;
import ru.ius.david.cgs.game.GameManager;
import ru.ius.david.cgs.images.CountryFlagManager;
import ru.ius.david.cgs.images.Image;
import ru.ius.david.cgs.images.ImageManager;
import ru.ius.david.cgs.images.Images;
import ru.ius.david.cgs.managers.SoundManager;
import ru.ius.david.cgs.multiplayer.MultiplayerManager;
import ru.ius.david.cgs.multiplayer.NetworkProtocol;
import ru.ius.david.cgs.CountryData;
import cgs.mods.multiplayer.actions.DiplomacyProposalAction;
import cgs.mods.multiplayer.chat.MultiplayerChat;

public class MultiplayerDiplomacyDialog {
    private static final MultiplayerDiplomacyDialog INSTANCE = new MultiplayerDiplomacyDialog();
    public static MultiplayerDiplomacyDialog getInstance() { return INSTANCE; }

    private boolean visible = false;
    private float openAnim = 0.0f;
    private float pulseTimer = 0.0f;
    private int selectedCountryIndex = 0;
    private long lastClickTime = 0;
    private final GlyphLayout glyphLayout = new GlyphLayout();

    // Incoming offer from another player
    private DiplomacyProposalAction incomingOffer = null;

    private MultiplayerDiplomacyDialog() {}

    public boolean isVisible() {
        return visible || incomingOffer != null;
    }

    public void open(cgsGame game) {
        this.visible = true;
        this.openAnim = 0.0f;
        this.selectedCountryIndex = 0;
    }

    public void close() {
        this.visible = false;
    }

    public void showIncomingOffer(DiplomacyProposalAction offer) {
        this.incomingOffer = offer;
    }

    public void closeIncomingOffer() {
        this.incomingOffer = null;
    }

    public String getPlayerNameForCountry(CountryState cs, GameManager gm, cgsGame game) {
        if (cs == null || game == null) return null;
        MultiplayerManager mp = game.getActiveMultiplayerManager();
        if (mp == null) return null;
        NetworkProtocol.LobbyState ls = mp.getLatestLobbyState();
        if (ls == null || ls.players == null) return null;

        int csColorKey = -1;
        if (gm != null && gm.getWarManager() != null) {
            try {
                csColorKey = gm.getWarManager().getColorKeyByName(cs.getName());
            } catch (Throwable ignored) {}
        }

        for (NetworkProtocol.PlayerInfo pi : ls.players) {
            if (pi.selectedColorKey != -1 && csColorKey != -1 && pi.selectedColorKey == csColorKey) {
                return pi.playerName;
            }
            if (pi.selectedCountryName != null && (pi.selectedCountryName.equalsIgnoreCase(cs.getName()) ||
                pi.selectedCountryName.equalsIgnoreCase(cs.getLocalizedName()))) {
                return pi.playerName;
            }
        }
        return null;
    }

    private List<CountryState> getAvailableCountries(GameManager gm, cgsGame game) {
        List<CountryState> list = new ArrayList<CountryState>();
        if (gm == null || gm.getWorldCountries() == null) return list;

        CountryState player = gm.getPlayerCountry();
        MultiplayerManager mp = game != null ? game.getActiveMultiplayerManager() : null;

        // In multiplayer, ONLY list real human players!
        if (mp != null && mp.isMultiplayer()) {
            NetworkProtocol.LobbyState ls = mp.getLatestLobbyState();
            if (ls != null && ls.players != null) {
                for (NetworkProtocol.PlayerInfo pi : ls.players) {
                    if (pi.playerId == mp.getLocalPlayerId()) continue; // Skip local player

                    CountryState cs = null;
                    if (pi.selectedColorKey != -1 && gm.getWarManager() != null) {
                        try {
                            CountryData cd = gm.getWarManager().getCountryData(pi.selectedColorKey);
                            if (cd != null && cd.name != null) {
                                cs = gm.getWorldCountries().get(cd.name);
                            }
                        } catch (Throwable ignored) {}
                    }
                    if (cs == null && pi.selectedCountryName != null) {
                        cs = gm.getWorldCountries().get(pi.selectedCountryName);
                        if (cs == null) {
                            for (CountryState anyCs : gm.getWorldCountries().values()) {
                                if (anyCs != null && (pi.selectedCountryName.equalsIgnoreCase(anyCs.getName()) ||
                                    pi.selectedCountryName.equalsIgnoreCase(anyCs.getLocalizedName()))) {
                                    cs = anyCs;
                                    break;
                                }
                            }
                        }
                    }

                    if (cs != null && cs.isOnMap() && !list.contains(cs)) {
                        if (player == null || !cs.getName().equalsIgnoreCase(player.getName())) {
                            list.add(cs);
                        }
                    }
                }
            }
            return list;
        }

        // Singleplayer: return empty list so diplomacy is directed to the built-in map diplomacy
        return list;
    }

    public void render(SpriteBatch batch, BitmapFont font, float screenWidth, float screenHeight, GameManager gm, cgsGame game) {
        if (!isVisible() || gm == null || font == null) return;

        float delta = Gdx.graphics.getDeltaTime();
        pulseTimer += delta;
        openAnim = Math.min(1.0f, openAnim + delta * 5.0f);
        float ease = 1.0f - (float)Math.pow(1.0f - openAnim, 3);

        Image pix = ImageManager.getImage(Images.pix255_255_255);
        if (pix == null) return;

        // Dim veil
        batch.setColor(0.0f, 0.0f, 0.0f, 0.55f * ease);
        pix.draw(batch, 0, 0, (int)screenWidth, (int)screenHeight);

        // 1. INCOMING OFFER POPUP
        if (incomingOffer != null) {
            renderIncomingOffer(batch, font, pix, screenWidth, screenHeight, gm, game, ease);
            return;
        }

        // 2. MAIN DIPLOMACY DIALOG
        float w = Math.min(CFG.s(680), screenWidth - CFG.s(40));
        float h = CFG.s(460);
        float x = (screenWidth - w) / 2.0f;
        float y = (screenHeight - h) / 2.0f;

        // Background
        batch.setColor(0.04f, 0.07f, 0.12f, 0.98f * ease);
        pix.draw(batch, (int)x, (int)y, (int)w, (int)h);

        // Header bar
        batch.setColor(0.07f, 0.18f, 0.32f, 1.0f * ease);
        pix.draw(batch, (int)x, (int)y, (int)w, CFG.s(44));

        // Animated cyan outer border
        float pulse = 0.85f + 0.15f * MathUtils.sin(pulseTimer * 3.5f);
        batch.setColor(0.2f * pulse, 0.7f * pulse, 1.0f * pulse, 0.95f * ease);
        drawOutline(pix, batch, (int)x, (int)y, (int)w, (int)h, 2);

        // Header title
        font.setColor(0.35f, 0.92f, 1.0f, ease);
        String title = "🌐 МУЛЬТИПЛЕЕРНАЯ ДИПЛОМАТИЯ";
        CFG.drawText(batch, title, (int)(x + CFG.s(20)), (int)(y + CFG.textTopCentered(font, (int)y, CFG.s(44))), new Color(0.35f, 0.92f, 1.0f, ease));

        // Close button (Top-Right)
        float closeW = CFG.s(32);
        float closeH = CFG.s(32);
        float closeX = x + w - closeW - CFG.s(8);
        float closeY = y + CFG.s(6);
        batch.setColor(0.7f, 0.15f, 0.15f, 0.9f * ease);
        pix.draw(batch, (int)closeX, (int)closeY, (int)closeW, (int)closeH);
        CFG.drawText(batch, "X", (int)(closeX + CFG.s(10)), (int)(closeY + CFG.s(6)), Color.WHITE);

        List<CountryState> countries = getAvailableCountries(gm, game);
        if (countries.isEmpty()) {
            float infoX = x + CFG.s(20);
            float infoY = y + CFG.s(64);
            float infoW = w - CFG.s(40);
            float infoH = h - CFG.s(84);

            batch.setColor(0.06f, 0.1f, 0.18f, 0.95f * ease);
            pix.draw(batch, (int)infoX, (int)infoY, (int)infoW, (int)infoH);

            batch.setColor(0.2f, 0.7f, 1.0f, 0.4f * ease);
            drawOutline(pix, batch, (int)infoX, (int)infoY, (int)infoW, (int)infoH, 1);

            CFG.drawText(batch, "👥 ДРУГИЕ ИГРОКИ НЕ ОБНАРУЖЕНЫ", (int)(infoX + CFG.s(24)), (int)(infoY + CFG.s(24)), new Color(0.95f, 0.75f, 0.25f, ease));

            CFG.drawTextSmall(batch, "⚡ Меню [F8] предназначено исключительно для дипломатии между", (int)(infoX + CFG.s(24)), (int)(infoY + CFG.s(70)), Color.WHITE);
            CFG.drawTextSmall(batch, "РЕАЛЬНЫМИ ЖИВЫМИ ИГРОКАМИ в совместной игре.", (int)(infoX + CFG.s(24)), (int)(infoY + CFG.s(94)), new Color(0.35f, 0.92f, 1.0f, ease));

            CFG.drawTextSmall(batch, "🏛️ Для дипломатии с державами под управлением ИИ используйте", (int)(infoX + CFG.s(24)), (int)(infoY + CFG.s(140)), Color.LIGHT_GRAY);
            CFG.drawTextSmall(batch, "СТАНДАРТНОЕ ВСТРОЕННОЕ МЕНЮ игры (клик по державе на карте).", (int)(infoX + CFG.s(24)), (int)(infoY + CFG.s(164)), Color.LIGHT_GRAY);

            drawCyberButton(batch, pix, font, (int)(infoX + (infoW - CFG.s(220)) / 2.0f), (int)(infoY + infoH - CFG.s(54)), (int)CFG.s(220), (int)CFG.s(40),
                "ЗАКРЫТЬ [ESC]", new Color(0.2f, 0.35f, 0.5f, 0.9f * ease), false);

            batch.setColor(Color.WHITE);
            return;
        }

        CountryState player = gm.getPlayerCountry();
        if (selectedCountryIndex >= countries.size()) {
            selectedCountryIndex = 0;
        }
        CountryState target = countries.get(selectedCountryIndex);

        // Target country selector
        float selY = y + CFG.s(64);
        float prevX = x + CFG.s(20);
        float prevW = CFG.s(60);
        float prevH = CFG.s(44);
        float nextW = CFG.s(60);
        float nextH = CFG.s(44);
        float nextX = x + w - nextW - CFG.s(20);
        float cardX = prevX + prevW + CFG.s(10);
        float cardW = nextX - cardX - CFG.s(10);

        // [ < ] Button
        batch.setColor(0.12f, 0.22f, 0.38f, 0.9f * ease);
        pix.draw(batch, (int)prevX, (int)selY, (int)prevW, (int)prevH);
        CFG.drawText(batch, "<", (int)(prevX + CFG.s(24)), (int)(selY + CFG.s(10)), Color.WHITE);

        // Target Card
        batch.setColor(0.08f, 0.14f, 0.24f, 0.95f * ease);
        pix.draw(batch, (int)cardX, (int)selY, (int)cardW, (int)prevH);

        String targName = target != null ? target.getLocalizedName() : "Нет держав";
        String pName = getPlayerNameForCountry(target, gm, game);
        if (pName != null && !pName.isEmpty()) {
            targName += "  👤 [" + pName + "]";
        }
        CFG.drawText(batch, targName, (int)(cardX + CFG.s(16)), (int)(selY + CFG.s(10)), Color.WHITE);

        // [ > ] Button
        batch.setColor(0.12f, 0.22f, 0.38f, 0.9f * ease);
        pix.draw(batch, (int)nextX, (int)selY, (int)nextW, (int)nextH);
        CFG.drawText(batch, ">", (int)(nextX + CFG.s(24)), (int)(selY + CFG.s(10)), Color.WHITE);

        // Status Banner
        float statY = selY + CFG.s(54);
        String statText = "СТАТУС: НЕЙТРАЛИТЕТ";
        Color statColor = Color.GRAY;

        boolean isAtWar = false;
        boolean isAllied = false;
        if (target != null && player != null && gm.getWarManager() != null) {
            try {
                int k1 = gm.getWarManager().getColorKeyByName(player.getName());
                int k2 = gm.getWarManager().getColorKeyByName(target.getName());
                isAtWar = gm.getWarManager().areAtWar(k1, k2);
                isAllied = gm.getWarManager().areAllies(k1, k2) || target.isAlly();
            } catch (Throwable ignored) {}
        }

        if (isAtWar) {
            statText = "СТАТУС: СОСТОЯНИЕ ВОЙНЫ ⚔️";
            statColor = new Color(0.95f, 0.25f, 0.25f, 1.0f);
        } else if (isAllied) {
            statText = "СТАТУС: ВОЕННЫЙ СОЮЗ 🛡️";
            statColor = new Color(0.25f, 0.95f, 0.45f, 1.0f);
        }

        batch.setColor(0.06f, 0.1f, 0.18f, 0.9f * ease);
        pix.draw(batch, (int)(x + CFG.s(20)), (int)statY, (int)(w - CFG.s(40)), CFG.s(32));
        CFG.drawTextSmall(batch, statText, (int)(x + CFG.s(32)), (int)(statY + CFG.s(8)), statColor);

        // Action Buttons
        float btnW = w - CFG.s(40);
        float btnH = CFG.s(46);
        float b1Y = statY + CFG.s(46);
        float b2Y = b1Y + btnH + CFG.s(10);
        float b3Y = b2Y + btnH + CFG.s(10);
        float b4Y = b3Y + btnH + CFG.s(10);

        // Button 1: Пакт о ненападении
        drawCyberButton(batch, pix, font, (int)(x + CFG.s(20)), (int)b1Y, (int)btnW, (int)btnH,
            "🕊️ ПРЕДЛОЖИТЬ ПАКТ О НЕ НАПАДЕНИИ", new Color(0.12f, 0.38f, 0.55f, 0.9f * ease), isAtWar);

        // Button 2: Военный союз
        drawCyberButton(batch, pix, font, (int)(x + CFG.s(20)), (int)b2Y, (int)btnW, (int)btnH,
            "⚔️ ЗАКЛЮЧИТЬ ВОЕННЫЙ СОЮЗ", new Color(0.15f, 0.48f, 0.32f, 0.9f * ease), isAtWar || isAllied);

        // Button 3: Разорвать союз / пакт
        drawCyberButton(batch, pix, font, (int)(x + CFG.s(20)), (int)b3Y, (int)btnW, (int)btnH,
            "💔 РАЗОРВАТЬ СОЮЗНЫЕ ДОГОВОРЁННОСТИ", new Color(0.48f, 0.28f, 0.12f, 0.9f * ease), !isAllied);

        // Button 4: Объявить войну
        drawCyberButton(batch, pix, font, (int)(x + CFG.s(20)), (int)b4Y, (int)btnW, (int)btnH,
            "⚠️ ОБЪЯВИТЬ ВОЙНУ", new Color(0.65f, 0.15f, 0.15f, 0.9f * ease), isAtWar);

        batch.setColor(Color.WHITE);
    }

    private void renderIncomingOffer(SpriteBatch batch, BitmapFont font, Image pix,
                                    float screenWidth, float screenHeight, GameManager gm, cgsGame game, float ease) {
        float popW = Math.min(CFG.s(560), screenWidth - CFG.s(30));
        float popH = CFG.s(220);
        float popX = (screenWidth - popW) / 2.0f;
        float popY = (screenHeight - popH) / 2.0f;

        batch.setColor(0.04f, 0.08f, 0.16f, 0.98f * ease);
        pix.draw(batch, (int)popX, (int)popY, (int)popW, (int)popH);

        batch.setColor(0.95f, 0.75f, 0.15f, 0.95f * ease);
        drawOutline(pix, batch, (int)popX, (int)popY, (int)popW, (int)popH, 2);

        String typeStr = (incomingOffer.proposalType == DiplomacyProposalAction.TYPE_ALLIANCE) ? "ВОЕННЫЙ СОЮЗ ⚔️" : "ПАКТ О НЕ НАПАДЕНИИ 🕊️";
        String msg = incomingOffer.proposerCountry + " предлагает заключить " + typeStr + "!";

        CFG.drawText(batch, "🕊️ ДИПЛОМАТИЧЕСКИЙ ЗАПРОС", (int)(popX + CFG.s(20)), (int)(popY + CFG.s(24)), new Color(0.95f, 0.85f, 0.2f, ease));
        CFG.drawTextSmall(batch, msg, (int)(popX + CFG.s(20)), (int)(popY + CFG.s(70)), Color.WHITE);

        float btnW = (popW - CFG.s(50)) / 2.0f;
        float btnH = CFG.s(44);
        float bY = popY + popH - btnH - CFG.s(20);

        // Accept
        batch.setColor(0.15f, 0.55f, 0.3f, 0.95f * ease);
        pix.draw(batch, (int)(popX + CFG.s(20)), (int)bY, (int)btnW, (int)btnH);
        CFG.drawText(batch, "✅ ПРИНЯТЬ", (int)(popX + CFG.s(40)), (int)(bY + CFG.s(10)), Color.WHITE);

        // Reject
        batch.setColor(0.65f, 0.18f, 0.18f, 0.95f * ease);
        pix.draw(batch, (int)(popX + CFG.s(30) + btnW), (int)bY, (int)btnW, (int)btnH);
        CFG.drawText(batch, "❌ ОТКЛОНИТЬ", (int)(popX + CFG.s(50) + btnW), (int)(bY + CFG.s(10)), Color.WHITE);

        batch.setColor(Color.WHITE);
    }

    private void drawCyberButton(SpriteBatch batch, Image pix, BitmapFont font,
                                 int x, int y, int w, int h, String text, Color bg, boolean disabled) {
        if (disabled) {
            batch.setColor(0.1f, 0.12f, 0.15f, 0.7f);
            pix.draw(batch, x, y, w, h);
            CFG.drawTextSmall(batch, text, x + CFG.s(20), y + CFG.s(14), Color.DARK_GRAY);
            return;
        }

        batch.setColor(bg);
        pix.draw(batch, x, y, w, h);

        batch.setColor(1.0f, 1.0f, 1.0f, 0.25f);
        drawOutline(pix, batch, x, y, w, h, 1);

        CFG.drawTextSmall(batch, text, x + CFG.s(20), y + CFG.s(14), Color.WHITE);
    }

    private void drawOutline(Image pix, SpriteBatch batch, int x, int y, int w, int h, int th) {
        pix.draw(batch, x, y, w, th);
        pix.draw(batch, x, y + h - th, w, th);
        pix.draw(batch, x, y, th, h);
        pix.draw(batch, x + w - th, y, th, h);
    }

    public boolean handleClick(float mouseX, float mouseY, float screenWidth, float screenHeight, GameManager gm, cgsGame game) {
        if (!isVisible()) return false;

        long now = System.currentTimeMillis();
        if (now - lastClickTime < 90) return true;
        lastClickTime = now;

        // 1. INCOMING OFFER CLICK
        if (incomingOffer != null) {
            float popW = Math.min(CFG.s(560), screenWidth - CFG.s(30));
            float popH = CFG.s(220);
            float popX = (screenWidth - popW) / 2.0f;
            float popY = (screenHeight - popH) / 2.0f;

            float btnW = (popW - CFG.s(50)) / 2.0f;
            float btnH = CFG.s(44);
            float bY = popY + popH - btnH - CFG.s(20);

            // Accept
            if (mouseX >= popX + CFG.s(20) && mouseX <= popX + CFG.s(20) + btnW &&
                mouseY >= bY && mouseY <= bY + btnH) {
                sendResponse(incomingOffer, DiplomacyProposalAction.RESP_ACCEPT, gm, game);
                incomingOffer = null;
                return true;
            }

            // Reject
            if (mouseX >= popX + CFG.s(30) + btnW && mouseX <= popX + CFG.s(30) + btnW * 2 &&
                mouseY >= bY && mouseY <= bY + btnH) {
                sendResponse(incomingOffer, DiplomacyProposalAction.RESP_REJECT, gm, game);
                incomingOffer = null;
                return true;
            }
            return true; // absorb click
        }

        // 2. MAIN DIALOG CLICK
        float w = Math.min(CFG.s(680), screenWidth - CFG.s(40));
        float h = CFG.s(460);
        float x = (screenWidth - w) / 2.0f;
        float y = (screenHeight - h) / 2.0f;

        // Close button
        float closeW = CFG.s(32);
        float closeH = CFG.s(32);
        float closeX = x + w - closeW - CFG.s(8);
        float closeY = y + CFG.s(6);
        if (mouseX >= closeX && mouseX <= closeX + closeW && mouseY >= closeY && mouseY <= closeY + closeH) {
            close();
            return true;
        }

        List<CountryState> countries = getAvailableCountries(gm, game);
        if (countries.isEmpty()) {
            float infoX = x + CFG.s(20);
            float infoY = y + CFG.s(64);
            float infoW = w - CFG.s(40);
            float infoH = h - CFG.s(84);
            float btnW = CFG.s(220);
            float btnH = CFG.s(40);
            float btnX = infoX + (infoW - btnW) / 2.0f;
            float btnY = infoY + infoH - CFG.s(54);
            if (mouseX >= btnX && mouseX <= btnX + btnW && mouseY >= btnY && mouseY <= btnY + btnH) {
                close();
                return true;
            }
            return true;
        }

        CountryState player = gm.getPlayerCountry();
        if (selectedCountryIndex >= countries.size()) {
            selectedCountryIndex = 0;
        }
        CountryState target = countries.get(selectedCountryIndex);

        // Prev country
        float selY = y + CFG.s(64);
        float prevX = x + CFG.s(20);
        float prevW = CFG.s(60);
        float prevH = CFG.s(44);
        if (mouseX >= prevX && mouseX <= prevX + prevW && mouseY >= selY && mouseY <= selY + prevH) {
            if (!countries.isEmpty()) {
                selectedCountryIndex = (selectedCountryIndex - 1 + countries.size()) % countries.size();
            }
            return true;
        }

        // Next country
        float nextW = CFG.s(60);
        float nextH = CFG.s(44);
        float nextX = x + w - nextW - CFG.s(20);
        if (mouseX >= nextX && mouseX <= nextX + nextW && mouseY >= selY && mouseY <= selY + nextH) {
            if (!countries.isEmpty()) {
                selectedCountryIndex = (selectedCountryIndex + 1) % countries.size();
            }
            return true;
        }

        // Action buttons
        float statY = selY + CFG.s(54);
        float btnW = w - CFG.s(40);
        float btnH = CFG.s(46);
        float b1Y = statY + CFG.s(46);
        float b2Y = b1Y + btnH + CFG.s(10);
        float b3Y = b2Y + btnH + CFG.s(10);
        float b4Y = b3Y + btnH + CFG.s(10);

        if (target != null && player != null) {
            // Button 1: Pact
            if (mouseX >= x + CFG.s(20) && mouseX <= x + CFG.s(20) + btnW && mouseY >= b1Y && mouseY <= b1Y + btnH) {
                sendProposal(player.getName(), target.getName(), DiplomacyProposalAction.TYPE_PACT, gm, game);
                close();
                return true;
            }

            // Button 2: Alliance
            if (mouseX >= x + CFG.s(20) && mouseX <= x + CFG.s(20) + btnW && mouseY >= b2Y && mouseY <= b2Y + btnH) {
                sendProposal(player.getName(), target.getName(), DiplomacyProposalAction.TYPE_ALLIANCE, gm, game);
                close();
                return true;
            }

            // Button 3: Break
            if (mouseX >= x + CFG.s(20) && mouseX <= x + CFG.s(20) + btnW && mouseY >= b3Y && mouseY <= b3Y + btnH) {
                sendProposal(player.getName(), target.getName(), DiplomacyProposalAction.TYPE_BREAK, gm, game);
                close();
                return true;
            }

            // Button 4: War
            if (mouseX >= x + CFG.s(20) && mouseX <= x + CFG.s(20) + btnW && mouseY >= b4Y && mouseY <= b4Y + btnH) {
                sendProposal(player.getName(), target.getName(), DiplomacyProposalAction.TYPE_WAR, gm, game);
                close();
                return true;
            }
        }

        return true; // absorb dialog click
    }

    private void sendProposal(String playerCountry, String targetCountry, int type, GameManager gm, cgsGame game) {
        MultiplayerManager mp = game != null ? game.getActiveMultiplayerManager() : null;
        int localId = mp != null ? mp.getLocalPlayerId() : 0;

        DiplomacyProposalAction act = new DiplomacyProposalAction(
            localId, playerCountry, playerCountry, targetCountry, type, DiplomacyProposalAction.RESP_OFFER
        );

        if (mp != null) {
            mp.enqueueAction(act);
        } else {
            act.execute(gm);
        }
        MultiplayerChat.getInstance().addSystemMessage("🕊️ [ДИПЛОМАТИЯ] Запрос направлен державе " + targetCountry + "...");
    }

    private void sendResponse(DiplomacyProposalAction offer, int responseStatus, GameManager gm, cgsGame game) {
        MultiplayerManager mp = game != null ? game.getActiveMultiplayerManager() : null;
        int localId = mp != null ? mp.getLocalPlayerId() : 0;

        DiplomacyProposalAction resp = new DiplomacyProposalAction(
            localId, offer.proposerCountry, offer.proposerPlayerName, offer.targetCountry, offer.proposalType, responseStatus
        );

        if (mp != null) {
            mp.enqueueAction(resp);
        } else {
            resp.execute(gm);
        }
    }
}
