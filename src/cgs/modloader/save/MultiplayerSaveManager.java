package cgs.modloader.save;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.utils.Json;
import com.badlogic.gdx.utils.JsonWriter;
import ru.ius.david.cgs.MapScreen;
import ru.ius.david.cgs.game.CountryState;
import ru.ius.david.cgs.game.GameManager;
import ru.ius.david.cgs.game.SaveManager;
import ru.ius.david.cgs.multiplayer.MultiplayerManager;
import ru.ius.david.cgs.multiplayer.NetworkProtocol;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.*;

public class MultiplayerSaveManager {
    private static final MultiplayerSaveManager INSTANCE = new MultiplayerSaveManager();
    public static MultiplayerSaveManager getInstance() { return INSTANCE; }

    public static class SavedPlayer {
        public String name = "";
        public int colorKey = 0;
        public String countryName = "";
        public boolean isSpectator = false;
        public boolean isHost = false;

        public SavedPlayer() {}
        public SavedPlayer(String name, int colorKey, String countryName, boolean isSpectator, boolean isHost) {
            this.name = name;
            this.colorKey = colorKey;
            this.countryName = countryName;
            this.isSpectator = isSpectator;
            this.isHost = isHost;
        }
    }

    public static class MpSaveHeader {
        public String saveId = "";
        public String saveTitle = "";
        public String scenarioPath = "";
        public long randomSeed = 0L;
        public int gameDays = 0;
        public int playSpeed = 1;
        public long timestamp = 0L;
        public String hostName = "";
        public List<SavedPlayer> players = new ArrayList<>();

        public MpSaveHeader() {}
    }

    private final Map<String, Integer> persistentPlayerSlots = new HashMap<>();

    private MultiplayerSaveManager() {}

    public void registerPlayerSlot(String playerName, int colorKey) {
        if (playerName != null && !playerName.isEmpty()) {
            persistentPlayerSlots.put(playerName.trim().toLowerCase(), colorKey);
        }
    }

    public Integer getPlayerSlot(String playerName) {
        if (playerName == null) return null;
        return persistentPlayerSlots.get(playerName.trim().toLowerCase());
    }

    public void saveMatch(ru.ius.david.cgs.cgsGame game, MultiplayerManager mpMgr) {
        GameManager gm = GameManager.getInstance();
        if (gm != null) {
            saveCurrentMatch(gm, mpMgr, null);
        }
    }

    public String saveCurrentMatch(GameManager gm, MultiplayerManager mpMgr, String customTitle) {
        if (gm == null || mpMgr == null) return null;

        try {
            File mpSaveDir = new File("saves/mp");
            if (!mpSaveDir.exists()) {
                mpSaveDir.mkdirs();
            }

            SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd_HHmmss");
            String dateStr = sdf.format(new Date());
            String saveId = "MPSAVE_" + dateStr;

            MpSaveHeader header = new MpSaveHeader();
            header.saveId = saveId;
            header.saveTitle = (customTitle != null && !customTitle.isEmpty()) ? customTitle : ("Сетевая игра " + dateStr);
            header.scenarioPath = mpMgr.getScenarioPath();
            header.randomSeed = mpMgr.getRandomSeed();
            header.gameDays = gm.getGameDays();
            header.playSpeed = gm.getPlaySpeed();
            header.timestamp = System.currentTimeMillis();
            header.hostName = mpMgr.getPlayerName();

            // Collect player assignments
            if (mpMgr.getLatestLobbyState() != null && mpMgr.getLatestLobbyState().players != null) {
                for (NetworkProtocol.PlayerInfo p : mpMgr.getLatestLobbyState().players) {
                    boolean isSpec = p.selectedColorKey >= cgs.modloader.spectator.SpectatorManager.SPECTATOR_KEY_BASE;
                    header.players.add(new SavedPlayer(p.playerName, p.selectedColorKey, p.selectedCountryName, isSpec, p.isHost));
                }
            } else if (gm.getPlayerCountry() != null) {
                int hostCk = 0;
                if (gm.getWarManager() != null) {
                    hostCk = gm.getWarManager().getColorKeyByName(gm.getPlayerCountry().getName());
                }
                header.players.add(new SavedPlayer(mpMgr.getPlayerName(), hostCk, gm.getPlayerCountry().getName(), false, true));
            }

            // Save binary state through vanilla save manager
            SaveManager.saveGame(saveId, header.scenarioPath != null ? header.scenarioPath : "ContinentValerika");

            // Save JSON metadata
            Json json = new Json(JsonWriter.OutputType.json);
            FileHandle fh = Gdx.files.local("saves/mp/" + saveId + ".mpsave.json");
            fh.writeString(json.prettyPrint(header), false, "UTF-8");

            Gdx.app.log("MpSaveManager", "⚡ Multiplayer match successfully saved to " + fh.path());

            MapScreen ms = MapScreen.getActiveInstance();
            if (ms != null) {
                ms.showToast("💾 Сетевой матч сохранён: " + header.saveTitle);
            }

            cgs.mods.multiplayer.chat.MultiplayerChat.getInstance().sendMessage(
                "💾 [Система] Сетевой матч успешно сохранён хостом! (" + header.saveTitle + ")", null
            );

            return saveId;
        } catch (Exception e) {
            Gdx.app.log("MpSaveManager", "Failed to save match: " + e.getMessage());
            MapScreen ms = MapScreen.getActiveInstance();
            if (ms != null) {
                ms.showToast("❌ Ошибка сохранения сетевого матча!");
            }
            return null;
        }
    }

    public List<MpSaveHeader> listMultiplayerSaves() {
        List<MpSaveHeader> result = new ArrayList<>();
        try {
            FileHandle dir = Gdx.files.local("saves/mp");
            if (!dir.exists() || !dir.isDirectory()) {
                return result;
            }

            Json json = new Json();
            for (FileHandle f : dir.list(".mpsave.json")) {
                try {
                    MpSaveHeader h = json.fromJson(MpSaveHeader.class, f.readString("UTF-8"));
                    if (h != null) {
                        result.add(h);
                    }
                } catch (Exception ignored) {}
            }

            result.sort((a, b) -> Long.compare(b.timestamp, a.timestamp));
        } catch (Exception e) {
            Gdx.app.log("MpSaveManager", "Error listing mp saves: " + e.getMessage());
        }
        return result;
    }
}
