package cgs.mods.multiplayer.actions;

import ru.ius.david.cgs.game.GameManager;
import ru.ius.david.cgs.game.InputAction;
import cgs.mods.multiplayer.pause.TacticalPauseManager;

public class TacticalPauseAction extends InputAction {
    public static final int ACT_REQUEST_PAUSE = 0;
    public static final int ACT_RESUME = 1;
    public static final int ACT_CHANGE_SPEED = 2;

    public int actionType = 0;
    public String requestPlayerName = "";
    public int targetSpeed = 1;

    public TacticalPauseAction() {}

    public TacticalPauseAction(int playerId, String requestPlayerName, int actionType, int targetSpeed) {
        this.playerId = playerId;
        this.requestPlayerName = requestPlayerName != null ? requestPlayerName : "Player";
        this.actionType = actionType;
        this.targetSpeed = targetSpeed;
    }

    @Override
    public void execute(GameManager gm) {
        TacticalPauseManager.getInstance().handleAction(this, gm);
    }

    @Override
    public String describe() {
        return "TacticalPauseAction(type=" + actionType + ", player=" + requestPlayerName + ", speed=" + targetSpeed + ")";
    }
}
