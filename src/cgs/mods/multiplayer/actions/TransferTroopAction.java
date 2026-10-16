package cgs.mods.multiplayer.actions;

import ru.ius.david.cgs.CountryData;
import ru.ius.david.cgs.MapScreen;
import ru.ius.david.cgs.WarManager;
import ru.ius.david.cgs.game.CountryState;
import ru.ius.david.cgs.game.GameManager;
import ru.ius.david.cgs.game.InputAction;
import ru.ius.david.cgs.units.MilitaryUnit;
import cgs.mods.multiplayer.chat.MultiplayerChat;

public class TransferTroopAction extends InputAction {
    public int unitId = -1;
    public int targetColorKey = 0;
    public String targetCountryName = "";

    public TransferTroopAction() {}

    public TransferTroopAction(int playerId, int unitId, int targetColorKey, String targetCountryName) {
        this.playerId = playerId;
        this.unitId = unitId;
        this.targetColorKey = targetColorKey;
        this.targetCountryName = targetCountryName != null ? targetCountryName : "";
    }

    @Override
    public void execute(GameManager gm) {
        if (gm == null || gm.getWarManager() == null || unitId < 0) return;

        WarManager wm = gm.getWarManager();
        MilitaryUnit unit = wm.getUnitManager().getById(unitId);
        if (unit == null) return;

        CountryState source = resolveSourceCountry(gm);
        int sourceColorKey = source != null ? wm.getColorKeyByName(source.getName()) : -1;

        if (unit.ownerColorKey == sourceColorKey || sourceColorKey == -1) {
            unit.ownerColorKey = targetColorKey;
            unit.clearOrders();

            CountryData targetCd = wm.getCountries().get(targetColorKey);
            String targetTitle = targetCd != null ? targetCd.getLocalizedName() : targetCountryName;
            String unitTitle = (unit.name != null && !unit.name.isEmpty()) ? unit.name : ("Корпус #" + unit.id);

            String msg = "⚔️ Передача дивизии: '" + unitTitle + "' передана под командование [" + targetTitle + "]!";
            MultiplayerChat.getInstance().sendMessage(msg, null);

            MapScreen ms = MapScreen.getActiveInstance();
            if (ms != null) {
                ms.showToast(msg);
            }
        }
    }

    @Override
    public String describe() {
        return "TransferTroop(unit=" + unitId + " ➔ " + targetCountryName + ")";
    }
}
