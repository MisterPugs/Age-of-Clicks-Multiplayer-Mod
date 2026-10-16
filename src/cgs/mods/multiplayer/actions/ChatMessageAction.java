package cgs.mods.multiplayer.actions;

import ru.ius.david.cgs.game.GameManager;
import ru.ius.david.cgs.game.InputAction;
import cgs.mods.multiplayer.chat.MultiplayerChat;

public class ChatMessageAction extends InputAction {
    public String senderName = "";
    public String message = "";
    public boolean teamOnly = false;

    public ChatMessageAction() {}

    public ChatMessageAction(int playerId, String senderName, String message, boolean teamOnly) {
        this.playerId = playerId;
        this.senderName = senderName != null ? senderName : "Player";
        this.message = message != null ? message : "";
        this.teamOnly = teamOnly;
    }

    @Override
    public void execute(GameManager gm) {
        MultiplayerChat.getInstance().onReceiveMessage(senderName, message, teamOnly);
    }

    @Override
    public String describe() {
        return "Chat [" + senderName + "]: " + message;
    }
}
