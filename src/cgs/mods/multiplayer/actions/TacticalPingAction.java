package cgs.mods.multiplayer.actions;

import ru.ius.david.cgs.game.GameManager;
import ru.ius.david.cgs.game.InputAction;
import cgs.mods.multiplayer.chat.MultiplayerChat;

public class TacticalPingAction extends InputAction {
    public String senderName = "";
    public float worldX;
    public float worldY;
    public int pingType; // 0 = attack (red), 1 = defense (blue), 2 = alert (yellow)

    public TacticalPingAction() {}

    public TacticalPingAction(int playerId, String senderName, float x, float y, int type) {
        this.playerId = playerId;
        this.senderName = senderName != null ? senderName : "Player";
        this.worldX = x;
        this.worldY = y;
        this.pingType = type;
    }

    @Override
    public void execute(GameManager gm) {
        MultiplayerChat.getInstance().onReceivePing(senderName, worldX, worldY, pingType);
    }

    @Override
    public String describe() {
        return "Ping [" + senderName + "] at (" + worldX + ", " + worldY + ")";
    }
}
