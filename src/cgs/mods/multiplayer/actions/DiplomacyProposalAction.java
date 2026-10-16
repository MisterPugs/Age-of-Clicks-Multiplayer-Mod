package cgs.mods.multiplayer.actions;

import ru.ius.david.cgs.game.CountryState;
import ru.ius.david.cgs.game.GameManager;
import ru.ius.david.cgs.game.InputAction;
import ru.ius.david.cgs.managers.SoundManager;
import cgs.mods.multiplayer.chat.MultiplayerChat;
import cgs.mods.multiplayer.diplomacy.MultiplayerDiplomacyDialog;

public class DiplomacyProposalAction extends InputAction {
    public static final int TYPE_PACT = 1;       // Пакт о ненападении
    public static final int TYPE_ALLIANCE = 2;   // Военный союз
    public static final int TYPE_WAR = 3;        // Объявление войны
    public static final int TYPE_BREAK = 4;      // Разрыв соглашения

    public static final int RESP_OFFER = 0;      // Запрос
    public static final int RESP_ACCEPT = 1;     // Принято
    public static final int RESP_REJECT = 2;     // Отклонено

    public String proposerCountry = "";
    public String proposerPlayerName = "";
    public String targetCountry = "";
    public int proposalType = 0;
    public int responseStatus = 0;

    public DiplomacyProposalAction() {}

    public DiplomacyProposalAction(int playerId, String proposerCountry, String proposerPlayerName,
                                   String targetCountry, int proposalType, int responseStatus) {
        this.playerId = playerId;
        this.proposerCountry = proposerCountry != null ? proposerCountry : "";
        this.proposerPlayerName = proposerPlayerName != null ? proposerPlayerName : "Player";
        this.targetCountry = targetCountry != null ? targetCountry : "";
        this.proposalType = proposalType;
        this.responseStatus = responseStatus;
    }

    @Override
    public void execute(GameManager gm) {
        if (gm == null) return;

        CountryState propState = gm.getWorldCountries().get(proposerCountry);
        CountryState targState = gm.getWorldCountries().get(targetCountry);

        String propName = propState != null ? propState.getLocalizedName() : proposerCountry;
        String targName = targState != null ? targState.getLocalizedName() : targetCountry;

        // 1. Direct Actions (War, Break)
        if (proposalType == TYPE_WAR) {
            if (gm.getWarManager() != null) {
                try {
                    int k1 = gm.getWarManager().getColorKeyByName(proposerCountry);
                    int k2 = gm.getWarManager().getColorKeyByName(targetCountry);
                    gm.getWarManager().declareWar(k1, k2);
                } catch (Throwable ignored) {}
            }
            String warMsg = "⚔️ [ВОЙНА] " + propName + " объявляет войну державе " + targName + "!";
            MultiplayerChat.getInstance().addSystemMessage(warMsg);
            try {
                SoundManager.getInstance().play(SoundManager.SFX.WAR_DECLARE);
            } catch (Throwable ignored) {}
            return;
        }

        if (proposalType == TYPE_BREAK) {
            if (propState != null && targState != null) {
                propState.setAllyOfCountryName(null);
                targState.setAllyOfCountryName(null);
                propState.isAlly = false;
                targState.isAlly = false;
            }
            String breakMsg = "💔 [ДИПЛОМАТИЯ] " + propName + " разрывает союзные соглашения с " + targName + ".";
            MultiplayerChat.getInstance().addSystemMessage(breakMsg);
            try {
                SoundManager.getInstance().play(SoundManager.SFX.BUTTON_CLICK);
            } catch (Throwable ignored) {}
            return;
        }

        // 2. Incoming Offer
        if (responseStatus == RESP_OFFER) {
            CountryState localCountry = gm.getPlayerCountry();
            boolean isTargetLocal = localCountry != null && targetCountry.equalsIgnoreCase(localCountry.getName());

            if (isTargetLocal) {
                MultiplayerDiplomacyDialog.getInstance().showIncomingOffer(this);
                try {
                    SoundManager.getInstance().play(SoundManager.SFX.ACHIEVEMENT);
                } catch (Throwable ignored) {}
            } else if (!gm.isHumanCountry(targetCountry) && gm.isHostInMultiplayer()) {
                // AI target decision on Host: AI accepts if not already at war
                boolean aiAccept = true;
                if (gm.getWarManager() != null) {
                    try {
                        int k1 = gm.getWarManager().getColorKeyByName(proposerCountry);
                        int k2 = gm.getWarManager().getColorKeyByName(targetCountry);
                        aiAccept = !gm.getWarManager().areAtWar(k1, k2);
                    } catch (Throwable ignored) {}
                }
                int newResp = aiAccept ? RESP_ACCEPT : RESP_REJECT;
                DiplomacyProposalAction aiResp = new DiplomacyProposalAction(
                    this.playerId, proposerCountry, proposerPlayerName, targetCountry, proposalType, newResp
                );
                aiResp.execute(gm);
            }
            return;
        }

        // 3. Responses (Accept / Reject)
        if (responseStatus == RESP_ACCEPT) {
            String typeName = (proposalType == TYPE_ALLIANCE) ? "Военный Союз" : "Пакт о ненападении";
            if (propState != null && targState != null) {
                if (proposalType == TYPE_ALLIANCE) {
                    propState.setAllyOfCountryName(targetCountry);
                    targState.setAllyOfCountryName(proposerCountry);
                    propState.isAlly = true;
                    targState.isAlly = true;
                }
            }
            String pactMsg = "🕊️ [ДИПЛОМАТИЯ] " + propName + " и " + targName + " заключили " + typeName + "!";
            MultiplayerChat.getInstance().addSystemMessage(pactMsg);
            try {
                SoundManager.getInstance().play(SoundManager.SFX.ACHIEVEMENT);
            } catch (Throwable ignored) {}
        } else if (responseStatus == RESP_REJECT) {
            String rejMsg = "⚠️ [ДИПЛОМАТИЯ] " + targName + " отклонил предложение от " + propName + ".";
            MultiplayerChat.getInstance().addSystemMessage(rejMsg);
        }
    }

    @Override
    public String describe() {
        return "DiplomacyProposal(" + proposerCountry + " ➔ " + targetCountry + ", type=" + proposalType + ", resp=" + responseStatus + ")";
    }
}
