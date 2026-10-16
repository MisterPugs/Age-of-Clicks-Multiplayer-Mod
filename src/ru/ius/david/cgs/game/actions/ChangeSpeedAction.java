package ru.ius.david.cgs.game.actions;

import ru.ius.david.cgs.MapScreen;
import ru.ius.david.cgs.game.GameManager;
import ru.ius.david.cgs.game.InputAction;

public class ChangeSpeedAction extends InputAction {
    public int newSpeed;

    public ChangeSpeedAction() {
    }

    public ChangeSpeedAction(int playerId, int newSpeed) {
        this.playerId = playerId;
        this.newSpeed = newSpeed;
    }

    @Override
    public void execute(GameManager gm) {
        if (gm != null && gm.isNetworkMode()) {
            // Only the host (playerId == 0) is permitted to change game speed
            if (this.playerId != 0) {
                System.out.println("[ChangeSpeedAction] Blocked speed change attempt from non-host playerId=" + this.playerId);
                MapScreen ms = MapScreen.getActiveInstance();
                if (ms != null && gm.getLocalPlayerId() == this.playerId) {
                    ms.showToast("⚠️ Только хост может изменять скорость игры!");
                }
                return;
            }
        }
        gm.setPlaySpeed(this.newSpeed);
    }

    @Override
    public String describe() {
        return "Change play speed to " + this.newSpeed + (this.playerId != 0 ? " (BLOCKED non-host)" : "");
    }
}
