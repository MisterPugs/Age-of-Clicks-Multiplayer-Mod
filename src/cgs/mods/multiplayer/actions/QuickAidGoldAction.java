package cgs.mods.multiplayer.actions;

import ru.ius.david.cgs.MapScreen;
import ru.ius.david.cgs.game.CountryState;
import ru.ius.david.cgs.game.GameManager;
import ru.ius.david.cgs.game.InputAction;
import cgs.mods.multiplayer.chat.MultiplayerChat;

public class QuickAidGoldAction extends InputAction {
    public int targetColorKey = 0;
    public String targetCountryName = "";
    public long goldAmount = 0L;

    public QuickAidGoldAction() {}

    public QuickAidGoldAction(int playerId, int targetColorKey, String targetCountryName, long goldAmount) {
        this.playerId = playerId;
        this.targetColorKey = targetColorKey;
        this.targetCountryName = targetCountryName != null ? targetCountryName : "";
        this.goldAmount = goldAmount;
    }

    @Override
    public void execute(GameManager gm) {
        if (gm == null || goldAmount <= 0) return;

        CountryState source = resolveSourceCountry(gm);
        CountryState target = null;

        if (targetCountryName != null && !targetCountryName.isEmpty()) {
            target = gm.getWorldCountries().get(targetCountryName);
        }
        if (target == null && gm.getWarManager() != null) {
            for (CountryState cs : gm.getWorldCountries().values()) {
                if (gm.getWarManager().getColorKeyByName(cs.getName()) == targetColorKey) {
                    target = cs;
                    break;
                }
            }
        }

        if (source != null && target != null) {
            long actualTransfer = Math.min(source.getGold(), goldAmount);
            if (actualTransfer > 0) {
                source.removeGold(actualTransfer);
                target.addGold(actualTransfer);

                String msg = "🤝 Финансовая помощь: " + source.getLocalizedName() + " передал " + actualTransfer + " 🪙 державе " + target.getLocalizedName() + "!";
                MultiplayerChat.getInstance().sendMessage(msg, null);

                MapScreen ms = MapScreen.getActiveInstance();
                if (ms != null) {
                    ms.showToast(msg);
                }
            }
        }
    }

    @Override
    public String describe() {
        return "QuickAidGold(" + goldAmount + " ➔ " + targetCountryName + ")";
    }
}
