/*
 * Decompiled with CFR 0.152.
 */
package ru.ius.david.cgs.multiplayer;

import com.badlogic.gdx.Gdx;
import com.codedisaster.steamworks.SteamID;
import java.io.IOException;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import ru.ius.david.cgs.CountryData;
import ru.ius.david.cgs.MapScreen;
import ru.ius.david.cgs.game.GameManager;
import ru.ius.david.cgs.game.InputAction;
import ru.ius.david.cgs.game.LanguageManager;
import ru.ius.david.cgs.game.actions.BuyAutoclickerAction;
import ru.ius.david.cgs.game.actions.ChangeSpeedAction;
import ru.ius.david.cgs.game.actions.ClickDiplomacyAction;
import ru.ius.david.cgs.game.actions.ManualClickAction;
import ru.ius.david.cgs.game.actions.ResearchTechAction;
import ru.ius.david.cgs.game.actions.SelectCountryAction;
import ru.ius.david.cgs.game.actions.UpgradeAutoclickerAction;
import ru.ius.david.cgs.game.actions.UpgradeBuildingAction;
import ru.ius.david.cgs.multiplayer.GameClient;
import ru.ius.david.cgs.multiplayer.GameServer;
import ru.ius.david.cgs.multiplayer.NetworkProtocol;
import ru.ius.david.cgs.multiplayer.WorldStateSnapshot;
import ru.ius.david.cgs.multiplayer.steam.SteamGameClient;
import ru.ius.david.cgs.multiplayer.steam.SteamGameServer;
import ru.ius.david.cgs.multiplayer.steam.SteamLobbyManager;

public class MultiplayerManager {
    private GameServer server;
    private GameClient client;
    private SteamGameServer steamServer;
    private SteamGameClient steamClient;
    private SteamLobbyManager steamLobby;
    private TransportMode transportMode = TransportMode.LAN;
    private GameManager gameManager;
    private boolean isHost;
    private boolean isMultiplayer = false;
    private boolean gameStarted = false;
    private int localPlayerId = -1;
    private long randomSeed;
    private String scenarioPath;
    private NetworkProtocol.LobbyState latestLobbyState;
    private NetworkProtocol.GameSettings latestGameSettings;
    private MultiplayerListener uiListener;
    private String playerName = "Player";
    private final List<PendingTick> pendingTicks = new ArrayList<PendingTick>();
    private final List<WorldStateSnapshot> pendingSnapshots = new ArrayList<WorldStateSnapshot>();
    private final List<WorldStateSnapshot> pendingEvents = new ArrayList<WorldStateSnapshot>();
    private final List<NetworkProtocol.FullTerritorySyncMessage> pendingFullSyncs = new ArrayList<NetworkProtocol.FullTerritorySyncMessage>();
    private volatile long lastPingRttMs = -1L;
    private volatile long lastPingResponseAtMs = 0L;
    private long lastPingSentMs = 0L;
    private static final long PING_INTERVAL_MS = 2000L;
    private static final long PING_STALE_MS = 8000L;
    private long lastSelectResendMs = 0L;
    private static final long SELECT_RESEND_INTERVAL_MS = 3000L;

    public void setUiListener(MultiplayerListener listener) {
        this.uiListener = listener;
    }

    public void hostGame(String scenarioPath, String playerName) {
        this.isHost = true;
        this.isMultiplayer = true;
        this.scenarioPath = scenarioPath;
        this.randomSeed = System.currentTimeMillis();
        this.localPlayerId = 0;
        if (this.gameManager != null) {
            this.gameManager.setLocalPlayerId(this.localPlayerId);
        }
        if (playerName != null && !playerName.isEmpty()) {
            this.playerName = playerName;
        }
        this.server = new GameServer(scenarioPath, this.randomSeed, this.playerName, new GameServer.ServerListener(){

            @Override
            public void onPlayerJoined(NetworkProtocol.PlayerInfo player) {
                Gdx.app.log("MP", "Player joined: " + player.playerName);
                if (MultiplayerManager.this.gameStarted) {
                    Integer savedCk = cgs.modloader.save.MultiplayerSaveManager.getInstance().getPlayerSlot(player.playerName);
                    if (savedCk != null && savedCk != 0) {
                        Gdx.app.log("MP", "⚡ Reconnecting player " + player.playerName + " restored to country colorKey " + savedCk);
                        player.selectedColorKey = savedCk;
                        player.ready = true;
                        cgs.mods.multiplayer.chat.MultiplayerChat.getInstance().sendMessage(
                            "⚡ [Система] Игрок " + player.playerName + " переподключился к матчу! Управление восстановлено.", null
                        );
                    }
                }
            }

            @Override
            public void onPlayerLeft(NetworkProtocol.PlayerInfo player) {
                Gdx.app.log("MP", "Player left: " + player.playerName);
                if (player.selectedColorKey != 0 && player.selectedColorKey != -1) {
                    cgs.modloader.save.MultiplayerSaveManager.getInstance().registerPlayerSlot(player.playerName, player.selectedColorKey);
                }
                MultiplayerManager.this.showPlayerLeftToast(player.playerName);
                if (MultiplayerManager.this.uiListener != null) {
                    MultiplayerManager.this.uiListener.onPlayerDisconnected(player.playerName);
                }
            }

            @Override
            public void onTickReady(int tickNumber, List<InputAction> allActions) {
            }

            @Override
            public void onCountrySelected(int playerId, int colorKey) {
                Gdx.app.log("MP", "Player " + playerId + " selected country " + colorKey);
            }

            @Override
            public void onPlayerReady(int playerId, boolean ready) {
                Gdx.app.log("MP", "Player " + playerId + " ready: " + ready);
            }
        });
        try {
            this.server.start();
        }
        catch (IOException e) {
            Gdx.app.log("MP", "Failed to start server: " + e.getMessage());
            this.isMultiplayer = false;
            return;
        }
        this.connectAsClient("127.0.0.1", playerName);
    }

    public void hostGameSteam(final String scenarioPath, String playerName, final SteamID localSteamId) {
        this.isHost = true;
        this.isMultiplayer = true;
        this.transportMode = TransportMode.STEAM;
        this.scenarioPath = scenarioPath;
        this.randomSeed = System.currentTimeMillis();
        this.localPlayerId = 0;
        if (playerName != null && !playerName.isEmpty()) {
            this.playerName = playerName;
        }
        if (this.gameManager != null) {
            this.gameManager.setLocalPlayerId(0);
        }
        if (this.steamLobby != null) {
            this.steamLobby.dispose();
            this.steamLobby = null;
        }
        this.steamLobby = new SteamLobbyManager(new SteamLobbyManager.LobbyListener(){

            @Override
            public void onLobbyCreated(SteamID lobbyId, boolean success) {
                if (!success) {
                    Gdx.app.log("MP", "Steam lobby creation failed!");
                    MultiplayerManager.this.isMultiplayer = false;
                    if (MultiplayerManager.this.uiListener != null) {
                        MultiplayerManager.this.uiListener.onDisconnected();
                    }
                    return;
                }
                MultiplayerManager.this.steamLobby.setLobbyMetadata(MultiplayerManager.this.playerName, scenarioPath, 1, 8);
                MultiplayerManager.this.steamServer.start(localSteamId);
            }

            @Override
            public void onLobbyList(List<SteamLobbyManager.SteamLobbyInfo> lobbies) {
            }

            @Override
            public void onLobbyJoined(SteamID lobbyId, SteamID hostSteamId) {
            }

            @Override
            public void onLobbyJoinFailed(String reason) {
            }

            @Override
            public void onLobbyMemberJoined(SteamID memberId) {
                if (MultiplayerManager.this.steamLobby != null) {
                    MultiplayerManager.this.steamLobby.updatePlayerCount(MultiplayerManager.this.steamServer.getPlayerCount());
                }
            }

            @Override
            public void onLobbyMemberLeft(SteamID memberId) {
                if (MultiplayerManager.this.steamLobby != null) {
                    MultiplayerManager.this.steamLobby.updatePlayerCount(MultiplayerManager.this.steamServer.getPlayerCount());
                }
            }
        });
        this.steamServer = new SteamGameServer(scenarioPath, this.randomSeed, playerName, new SteamGameServer.SteamServerListener(){

            @Override
            public void onHostReady(int hostPlayerId) {
                MultiplayerManager.this.localPlayerId = hostPlayerId;
                if (MultiplayerManager.this.uiListener != null) {
                    MultiplayerManager.this.uiListener.onConnected(hostPlayerId);
                }
            }

            @Override
            public void onLobbyStateChanged(NetworkProtocol.LobbyState state) {
                MultiplayerManager.this.latestLobbyState = state;
                if (state.gameSettings != null) {
                    MultiplayerManager.this.latestGameSettings = state.gameSettings;
                }
                if (MultiplayerManager.this.uiListener != null) {
                    MultiplayerManager.this.uiListener.onLobbyUpdated(state);
                }
            }

            @Override
            public void onGameStarted(String path, long seed, List<NetworkProtocol.PlayerInfo> players) {
                MultiplayerManager.this.gameStarted = true;
                if (MultiplayerManager.this.uiListener != null) {
                    MultiplayerManager.this.uiListener.onGameStarting(path, seed, players);
                }
            }

            @Override
            public void onGuestPlayerLeft(String name) {
                MultiplayerManager.this.showPlayerLeftToast(name);
                if (MultiplayerManager.this.uiListener != null) {
                    MultiplayerManager.this.uiListener.onPlayerDisconnected(name);
                }
            }
        }, new SteamGameServer.ServerListener(){

            @Override
            public void onPlayerJoined(NetworkProtocol.PlayerInfo p) {
                Gdx.app.log("MP", "Steam player joined: " + p.playerName);
            }

            @Override
            public void onPlayerLeft(NetworkProtocol.PlayerInfo p) {
                Gdx.app.log("MP", "Steam player left: " + p.playerName);
            }

            @Override
            public void onCountrySelected(int pid, int ck) {
            }

            @Override
            public void onPlayerReady(int pid, boolean rdy) {
            }
        });
        this.steamLobby.createLobby(8);
    }

    public void joinGame(String hostIp, String playerName) {
        this.isHost = false;
        this.isMultiplayer = true;
        if (playerName != null && !playerName.isEmpty()) {
            this.playerName = playerName;
        }
        this.connectAsClient(hostIp, this.playerName);
    }

    public void joinGameSteam(SteamLobbyManager.SteamLobbyInfo lobbyInfo, String playerName) {
        this.isHost = false;
        this.isMultiplayer = true;
        this.transportMode = TransportMode.STEAM;
        if (playerName != null && !playerName.isEmpty()) {
            this.playerName = playerName;
        }
        if (this.steamLobby != null) {
            this.steamLobby.dispose();
            this.steamLobby = null;
        }
        this.steamLobby = new SteamLobbyManager(new SteamLobbyManager.LobbyListener(){

            @Override
            public void onLobbyCreated(SteamID lobbyId, boolean success) {
            }

            @Override
            public void onLobbyList(List<SteamLobbyManager.SteamLobbyInfo> lobbies) {
            }

            @Override
            public void onLobbyJoined(SteamID lobbyId, SteamID hostSteamId) {
                Gdx.app.log("MP", "Steam lobby joined, connecting to host P2P");
                MultiplayerManager.this.steamClient = MultiplayerManager.this.createSteamClient();
                MultiplayerManager.this.steamClient.connect(hostSteamId, MultiplayerManager.this.playerName);
            }

            @Override
            public void onLobbyJoinFailed(String reason) {
                Gdx.app.log("MP", "Steam lobby join failed: " + reason);
                MultiplayerManager.this.isMultiplayer = false;
                if (MultiplayerManager.this.uiListener != null) {
                    MultiplayerManager.this.uiListener.onDisconnected();
                }
            }

            @Override
            public void onLobbyMemberJoined(SteamID memberId) {
            }

            @Override
            public void onLobbyMemberLeft(SteamID memberId) {
            }
        });
        this.steamLobby.joinLobby(lobbyInfo.lobbyId);
    }

    private SteamGameClient createSteamClient() {
        return new SteamGameClient(new SteamGameClient.ClientListener(){

            @Override
            public void onConnected(int pid, String path, long seed) {
                MultiplayerManager.this.localPlayerId = pid;
                MultiplayerManager.this.scenarioPath = path;
                MultiplayerManager.this.randomSeed = seed;
                if (MultiplayerManager.this.gameManager != null) {
                    MultiplayerManager.this.gameManager.setLocalPlayerId(pid);
                }
                if (MultiplayerManager.this.uiListener != null) {
                    MultiplayerManager.this.uiListener.onConnected(pid);
                }
            }

            @Override
            public void onDisconnected() {
                if (MultiplayerManager.this.uiListener != null) {
                    if (MultiplayerManager.this.gameStarted) {
                        MultiplayerManager.this.uiListener.onHostDisconnectedDuringGame();
                    } else {
                        MultiplayerManager.this.uiListener.onDisconnected();
                    }
                }
            }

            @Override
            public void onLobbyState(NetworkProtocol.LobbyState state) {
                MultiplayerManager.this.latestLobbyState = state;
                if (state.gameSettings != null) {
                    MultiplayerManager.this.latestGameSettings = state.gameSettings;
                }
                if (MultiplayerManager.this.uiListener != null) {
                    MultiplayerManager.this.uiListener.onLobbyUpdated(state);
                }
            }

            @Override
            public void onGameStart(NetworkProtocol.GameStartMessage msg) {
                MultiplayerManager.this.gameStarted = true;
                MultiplayerManager.this.randomSeed = msg.randomSeed;
                MultiplayerManager.this.scenarioPath = msg.scenarioPath;
                NetworkProtocol.LobbyState startState = new NetworkProtocol.LobbyState();
                startState.scenarioPath = msg.scenarioPath;
                startState.gameStarted = true;
                startState.players = msg.players;
                startState.gameSettings = msg.gameSettings;
                MultiplayerManager.this.latestLobbyState = startState;
                if (MultiplayerManager.this.uiListener != null) {
                    MultiplayerManager.this.uiListener.onGameStarting(msg.scenarioPath, msg.randomSeed, msg.players);
                }
            }

            /*
             * WARNING - Removed try catching itself - possible behaviour change.
             */
            @Override
            public void onTickReceived(int tick, List<InputAction> actions) {
                List list = MultiplayerManager.this.pendingTicks;
                synchronized (list) {
                    MultiplayerManager.this.pendingTicks.add(new PendingTick(tick, actions));
                }
            }

            /*
             * WARNING - Removed try catching itself - possible behaviour change.
             */
            @Override
            public void onWorldStateReceived(WorldStateSnapshot ws) {
                List list = MultiplayerManager.this.pendingSnapshots;
                synchronized (list) {
                    MultiplayerManager.this.pendingSnapshots.add(ws);
                }
            }

            /*
             * WARNING - Removed try catching itself - possible behaviour change.
             */
            @Override
            public void onWorldEventReceived(WorldStateSnapshot events) {
                List list = MultiplayerManager.this.pendingEvents;
                synchronized (list) {
                    MultiplayerManager.this.pendingEvents.add(events);
                }
            }

            /*
             * WARNING - Removed try catching itself - possible behaviour change.
             */
            @Override
            public void onFullTerritorySync(NetworkProtocol.FullTerritorySyncMessage msg) {
                List list = MultiplayerManager.this.pendingFullSyncs;
                synchronized (list) {
                    MultiplayerManager.this.pendingFullSyncs.add(msg);
                }
            }

            @Override
            public void onPlayerDisconnected(String name) {
                MultiplayerManager.this.showPlayerLeftToast(name);
                if (MultiplayerManager.this.uiListener != null) {
                    MultiplayerManager.this.uiListener.onPlayerDisconnected(name);
                }
            }

            @Override
            public void onPingResponse(long rtt) {
                MultiplayerManager.this.lastPingRttMs = rtt;
                MultiplayerManager.this.lastPingResponseAtMs = System.currentTimeMillis();
            }

            @Override
            public void onCountrySelectRejected(int ck, String takenBy) {
                if (MultiplayerManager.this.uiListener != null) {
                    MultiplayerManager.this.uiListener.onCountrySelectRejected(ck, takenBy);
                }
            }
        });
    }

    private void showPlayerLeftToast(String playerName) {
        if (!this.gameStarted) {
            return;
        }
        try {
            if (MapScreen.activeInstance != null) {
                MapScreen.activeInstance.showToast(LanguageManager.format("mp_status_player_left", playerName));
            }
        }
        catch (Exception exception) {
            // empty catch block
        }
    }

    public void discoverSteamLobbies(SteamLobbyManager.LobbyListener lobbyListener) {
        if (this.steamLobby == null) {
            this.steamLobby = new SteamLobbyManager(lobbyListener);
        }
        this.steamLobby.requestLobbyList();
    }

    public void openSteamInvite() {
        if (this.steamLobby != null) {
            this.steamLobby.openInviteOverlay();
        }
    }

    public TransportMode getTransportMode() {
        return this.transportMode;
    }

    private void connectAsClient(String host, String playerName) {
        this.client = new GameClient(new GameClient.ClientListener(){

            @Override
            public void onConnected(int playerId, String scenarioPath, long randomSeed) {
                MultiplayerManager.this.localPlayerId = playerId;
                if (MultiplayerManager.this.gameManager != null) {
                    MultiplayerManager.this.gameManager.setLocalPlayerId(playerId);
                }
                MultiplayerManager.this.scenarioPath = scenarioPath;
                MultiplayerManager.this.randomSeed = randomSeed;
                Gdx.app.log("MP", "Connected as player " + playerId);
                if (MultiplayerManager.this.uiListener != null) {
                    MultiplayerManager.this.uiListener.onConnected(playerId);
                }
            }

            @Override
            public void onDisconnected() {
                Gdx.app.log("MP", "Disconnected from server");
                if (MultiplayerManager.this.uiListener != null) {
                    if (MultiplayerManager.this.gameStarted) {
                        MultiplayerManager.this.uiListener.onHostDisconnectedDuringGame();
                    } else {
                        MultiplayerManager.this.uiListener.onDisconnected();
                    }
                }
            }

            @Override
            public void onLobbyState(NetworkProtocol.LobbyState state) {
                MultiplayerManager.this.latestLobbyState = state;
                if (state.gameSettings != null) {
                    MultiplayerManager.this.latestGameSettings = state.gameSettings;
                }
                if (MultiplayerManager.this.uiListener != null) {
                    MultiplayerManager.this.uiListener.onLobbyUpdated(state);
                }
            }

            @Override
            public void onGameStart(NetworkProtocol.GameStartMessage msg) {
                MultiplayerManager.this.gameStarted = true;
                MultiplayerManager.this.randomSeed = msg.randomSeed;
                MultiplayerManager.this.scenarioPath = msg.scenarioPath;
                NetworkProtocol.LobbyState startState = new NetworkProtocol.LobbyState();
                startState.scenarioPath = msg.scenarioPath;
                startState.gameStarted = true;
                startState.players = msg.players;
                startState.gameSettings = msg.gameSettings;
                MultiplayerManager.this.latestLobbyState = startState;
                Gdx.app.log("MP", "Game starting!");
                if (MultiplayerManager.this.uiListener != null) {
                    MultiplayerManager.this.uiListener.onGameStarting(msg.scenarioPath, msg.randomSeed, msg.players);
                }
            }

            /*
             * WARNING - Removed try catching itself - possible behaviour change.
             */
            @Override
            public void onTickReceived(int tickNumber, List<InputAction> actions) {
                if (!MultiplayerManager.this.isHost) {
                    List list = MultiplayerManager.this.pendingTicks;
                    synchronized (list) {
                        MultiplayerManager.this.pendingTicks.add(new PendingTick(tickNumber, actions));
                    }
                }
            }

            /*
             * WARNING - Removed try catching itself - possible behaviour change.
             */
            @Override
            public void onWorldStateReceived(WorldStateSnapshot worldState) {
                if (!MultiplayerManager.this.isHost) {
                    List list = MultiplayerManager.this.pendingSnapshots;
                    synchronized (list) {
                        MultiplayerManager.this.pendingSnapshots.add(worldState);
                    }
                }
            }

            /*
             * WARNING - Removed try catching itself - possible behaviour change.
             */
            @Override
            public void onFullTerritorySync(NetworkProtocol.FullTerritorySyncMessage msg) {
                if (!MultiplayerManager.this.isHost) {
                    List list = MultiplayerManager.this.pendingFullSyncs;
                    synchronized (list) {
                        MultiplayerManager.this.pendingFullSyncs.add(msg);
                    }
                }
            }

            @Override
            public void onPlayerDisconnected(String playerName) {
                MultiplayerManager.this.showPlayerLeftToast(playerName);
                if (MultiplayerManager.this.uiListener != null) {
                    MultiplayerManager.this.uiListener.onPlayerDisconnected(playerName);
                }
            }

            @Override
            public void onPingResponse(long roundTripMs) {
                MultiplayerManager.this.lastPingRttMs = roundTripMs;
                MultiplayerManager.this.lastPingResponseAtMs = System.currentTimeMillis();
            }

            @Override
            public void onCountrySelectRejected(int colorKey, String takenByPlayer) {
                Gdx.app.log("MP", "Country selection rejected: taken by " + takenByPlayer);
                if (MultiplayerManager.this.uiListener != null) {
                    MultiplayerManager.this.uiListener.onCountrySelectRejected(colorKey, takenByPlayer);
                }
            }
        });
        this.client.connect(host, playerName);
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public void update(float delta) {
        long now;
        Object newestFullSync;
        if (!this.isMultiplayer) {
            return;
        }
        if (this.transportMode == TransportMode.STEAM) {
            if (this.isHost && this.steamServer != null) {
                this.steamServer.update(delta);
            }
            if (!this.isHost && this.steamClient != null) {
                this.steamClient.pollMessages();
                this.steamClient.flushActions();
            }
        } else {
            if (this.isHost && this.server != null) {
                this.server.update(delta);
            }
            if (this.client != null) {
                this.client.flushActions();
            }
        }
        if (this.gameStarted && this.gameManager != null && !this.isHost) {
            if (this.gameManager.getWarManager() != null) {
                ArrayList<WorldStateSnapshot> eventsToProcess;
                ArrayList<WorldStateSnapshot> snapshotsToProcess;
                ArrayList<PendingTick> ticksToProcess;
                newestFullSync = null;
                List<NetworkProtocol.FullTerritorySyncMessage> list = this.pendingFullSyncs;
                synchronized (list) {
                    if (!this.pendingFullSyncs.isEmpty()) {
                        newestFullSync = this.pendingFullSyncs.get(this.pendingFullSyncs.size() - 1);
                        this.pendingFullSyncs.clear();
                    }
                }
                if (newestFullSync != null) {
                    try {
                        this.gameManager.getWarManager().applyFullTerritorySync((NetworkProtocol.FullTerritorySyncMessage)newestFullSync);
                    }
                    catch (Exception e) {
                        Gdx.app.log("MP", "Full territory sync apply error: " + e.getMessage());
                    }
                }
                synchronized (this.pendingTicks) {
                    ticksToProcess = new ArrayList<PendingTick>(this.pendingTicks);
                    this.pendingTicks.clear();
                }
                synchronized (this.pendingSnapshots) {
                    snapshotsToProcess = new ArrayList<WorldStateSnapshot>(this.pendingSnapshots);
                    this.pendingSnapshots.clear();
                }
                synchronized (this.pendingEvents) {
                    eventsToProcess = new ArrayList<WorldStateSnapshot>(this.pendingEvents);
                    this.pendingEvents.clear();
                }
                for (int i = 0; i < eventsToProcess.size(); ++i) {
                    this.gameManager.applyWorldSnapshot((WorldStateSnapshot)eventsToProcess.get(i), false);
                }
                int maxPaired = Math.max(ticksToProcess.size(), snapshotsToProcess.size());
                for (int i = 0; i < maxPaired; ++i) {
                    if (i < ticksToProcess.size()) {
                        this.gameManager.tickFromNetwork(((PendingTick)ticksToProcess.get((int)i)).actions);
                    }
                    if (i >= snapshotsToProcess.size()) continue;
                    boolean newest = i == snapshotsToProcess.size() - 1;
                    this.gameManager.applyWorldSnapshot((WorldStateSnapshot)snapshotsToProcess.get(i), newest);
                }
                this.resendCountrySelectionIfUnconfirmed();
            }
        } else if (this.gameStarted && this.isHost) {
            newestFullSync = this.pendingTicks;
            synchronized (newestFullSync) {
                this.pendingTicks.clear();
            }
        }
        if (this.gameStarted && !this.isHost && (now = System.currentTimeMillis()) - this.lastPingSentMs >= 2000L) {
            this.lastPingSentMs = now;
            if (this.transportMode == TransportMode.STEAM) {
                if (this.steamClient != null) {
                    this.steamClient.sendPing();
                }
            } else if (this.client != null) {
                this.client.sendPing();
            }
        }
    }

    private void resendCountrySelectionIfUnconfirmed() {
        if (this.gameManager == null || this.gameManager.getWarManager() == null) {
            return;
        }
        if (this.gameManager.getLocalPlayerCountryName() == null) {
            return;
        }
        if (this.gameManager.isLocalPlayerConfirmedByHost()) {
            return;
        }
        long now = System.currentTimeMillis();
        if (now - this.lastSelectResendMs < 3000L) {
            return;
        }
        this.lastSelectResendMs = now;
        int colorKey = this.gameManager.getWarManager().getColorKeyByName(this.gameManager.getLocalPlayerCountryName());
        if (colorKey == -1) {
            return;
        }
        SelectCountryAction sca = new SelectCountryAction(this.localPlayerId, colorKey);
        sca.sourceCountryName = this.gameManager.getLocalPlayerCountryName();
        sca.sourceColorKey = colorKey;
        if (this.transportMode == TransportMode.STEAM) {
            if (this.steamClient != null) {
                this.steamClient.enqueueAction(sca);
            }
        } else if (this.client != null) {
            this.client.enqueueAction(sca);
        }
        Gdx.app.log("MP", "No host confirmation yet \u2014 re-sending country selection: " + this.gameManager.getLocalPlayerCountryName());
    }

    public long getDisplayPingMs() {
        if (!this.isMultiplayer) {
            return -1L;
        }
        if (this.isHost) {
            return 0L;
        }
        if (this.lastPingResponseAtMs > 0L && System.currentTimeMillis() - this.lastPingResponseAtMs > 8000L) {
            return -1L;
        }
        return this.lastPingRttMs;
    }

    public long getMaxGuestPingMs() {
        if (!this.isHost) {
            return -1L;
        }
        if (this.transportMode == TransportMode.STEAM && this.steamServer != null) {
            return this.steamServer.getMaxGuestPingMs();
        }
        if (this.server != null) {
            return this.server.getMaxGuestPingMs();
        }
        return -1L;
    }

    public void enqueueAction(InputAction action) {
        if (action instanceof ChangeSpeedAction) {
            if (this.isMultiplayer && !this.isHost) {
                System.out.println("[MultiplayerManager] Blocked speed change: only host can change game speed!");
                MapScreen ms = MapScreen.getActiveInstance();
                if (ms != null) {
                    ms.showToast("⚠️ Только хост может изменять скорость игры!");
                }
                return;
            }
        }
        action.playerId = this.localPlayerId;
        if (this.gameManager != null && this.gameManager.getPlayerCountry() != null) {
            action.sourceCountryName = this.gameManager.getPlayerCountry().getName();
        }
        if (action.sourceCountryName != null && this.gameManager != null && this.gameManager.getWarManager() != null) {
            for (CountryData cd : this.gameManager.getWarManager().getCountries().values()) {
                if (!action.sourceCountryName.equals(cd.name)) continue;
                action.sourceColorKey = cd.colorKey;
                break;
            }
        }
        if (this.isMultiplayer && this.gameStarted) {
            if (this.isHost && this.gameManager != null) {
                try {
                    action.execute(this.gameManager);
                }
                catch (Exception e) {
                    Gdx.app.log("MP", "Host action failed: " + e.getMessage());
                }
                this.gameManager.addBroadcastAction(action);
            } else if (!this.isHost) {
                if (this.gameManager != null && MultiplayerManager.isOptimisticAction(action)) {
                    try {
                        action.execute(this.gameManager);
                    }
                    catch (Exception e) {
                        Gdx.app.log("MP", "Optimistic action failed: " + e.getMessage());
                    }
                }
                if (this.gameManager != null) {
                    this.gameManager.predictActionLocally(action);
                }
                if (this.transportMode == TransportMode.STEAM) {
                    if (this.steamClient != null) {
                        this.steamClient.enqueueAction(action);
                    }
                } else if (this.client != null) {
                    this.client.enqueueAction(action);
                }
            }
        } else if (this.gameManager != null) {
            this.gameManager.enqueueAction(action);
        }
    }

    private static boolean isOptimisticAction(InputAction action) {
        return action instanceof ManualClickAction || action instanceof ClickDiplomacyAction || action instanceof UpgradeBuildingAction || action instanceof ResearchTechAction || action instanceof BuyAutoclickerAction || action instanceof UpgradeAutoclickerAction;
    }

    public void startGame() {
        if (this.isHost) {
            this.applyHostChosenSeed();
            if (this.transportMode == TransportMode.STEAM && this.steamServer != null) {
                this.steamServer.startGame();
            } else if (this.server != null) {
                this.server.startGame();
            }
        }
    }

    private void applyHostChosenSeed() {
        long chosen = GameManager.pendingGameSeed;
        if (chosen == 0L) {
            return;
        }
        this.randomSeed = chosen;
        if (this.transportMode == TransportMode.STEAM && this.steamServer != null) {
            this.steamServer.setRandomSeed(chosen);
        } else if (this.server != null) {
            this.server.setRandomSeed(chosen);
        }
        GameManager.pendingGameSeed = 0L;
        Gdx.app.log("MP", "Host-chosen world seed applied: " + chosen);
    }

    public void selectCountry(int colorKey) {
        if (this.transportMode == TransportMode.STEAM) {
            if (this.isHost && this.steamServer != null) {
                this.steamServer.handleHostCountrySelect(colorKey);
            } else if (this.steamClient != null) {
                this.steamClient.selectCountry(colorKey);
            }
        } else if (this.client != null) {
            this.client.selectCountry(colorKey);
        }
    }

    public void setReady(boolean ready) {
        if (this.transportMode == TransportMode.STEAM) {
            if (!this.isHost && this.steamClient != null) {
                this.steamClient.setReady(ready);
            }
            if (this.isHost && this.steamServer != null) {
                this.steamServer.handleHostReady(ready);
            }
        } else if (this.client != null) {
            this.client.setReady(ready);
        }
    }

    public NetworkProtocol.GameSettings getGameSettings() {
        return this.latestGameSettings;
    }

    public void updateGameSettings(NetworkProtocol.GameSettings settings) {
        this.latestGameSettings = settings;
        if (this.isHost) {
            if (this.transportMode == TransportMode.STEAM && this.steamServer != null) {
                this.steamServer.handleHostSettingsUpdate(settings);
            } else if (this.client != null) {
                this.client.sendGameSettings(settings);
            }
        }
    }

    public void requestSync() {
        if (this.isHost) {
            return;
        }
        if (this.transportMode == TransportMode.STEAM) {
            if (this.steamClient != null) {
                this.steamClient.requestSync();
            }
        } else if (this.client != null) {
            this.client.requestSync();
        }
    }

    public List<InetAddress> discoverHosts() {
        if (this.client == null) {
            this.client = new GameClient(null);
        }
        return this.client.discoverHosts(3000);
    }

    public Map<InetAddress, NetworkProtocol.DiscoveryResponse> getDiscoveredRooms() {
        if (this.client != null) {
            return this.client.getDiscoveredRooms();
        }
        return new HashMap<InetAddress, NetworkProtocol.DiscoveryResponse>();
    }

    public void setGameManager(GameManager gm) {
        this.gameManager = gm;
        if (gm != null) {
            gm.setActiveMultiplayerManager(this);
            gm.setHostInMultiplayer(this.isHost);
            gm.setLocalPlayerId(this.localPlayerId);
            if (this.isHost) {
                if (this.transportMode == TransportMode.STEAM && this.steamServer != null) {
                    this.steamServer.setGameManager(gm);
                } else if (this.server != null) {
                    this.server.setGameManager(gm);
                }
            }
            if (!this.isHost && gm.getWarManager() != null) {
                gm.getWarManager().setNetworkClientMode(true);
                Gdx.app.log("MP", "Set WarManager to network client mode (simulation disabled)");
            }
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public void shutdown() {
        if (this.gameManager != null) {
            this.gameManager.setActiveMultiplayerManager(null);
        }
        this.isMultiplayer = false;
        this.gameStarted = false;
        this.uiListener = null;
        this.transportMode = TransportMode.LAN;
        synchronized (this.pendingTicks) {
            this.pendingTicks.clear();
        }
        synchronized (this.pendingSnapshots) {
            this.pendingSnapshots.clear();
        }
        synchronized (this.pendingEvents) {
            this.pendingEvents.clear();
        }
        synchronized (this.pendingFullSyncs) {
            this.pendingFullSyncs.clear();
        }
        SteamGameClient steamClientRef = this.steamClient;
        SteamGameServer steamServerRef = this.steamServer;
        SteamLobbyManager steamLobbyRef = this.steamLobby;
        this.steamClient = null;
        this.steamServer = null;
        this.steamLobby = null;
        if (steamClientRef != null) {
            steamClientRef.disconnect();
        }
        if (steamServerRef != null) {
            steamServerRef.stop();
        }
        if (steamLobbyRef != null) {
            steamLobbyRef.dispose();
        }
        final GameClient clientRef = this.client;
        final GameServer serverRef = this.server;
        this.client = null;
        this.server = null;
        Thread shutdownThread = new Thread(new Runnable(){

            @Override
            public void run() {
                try {
                    if (clientRef != null) {
                        clientRef.disconnect();
                    }
                    Thread.sleep(100L);
                    if (serverRef != null) {
                        serverRef.stop();
                    }
                }
                catch (Exception exception) {
                    // empty catch block
                }
            }
        }, "MP-Shutdown");
        shutdownThread.setDaemon(true);
        shutdownThread.start();
        Gdx.app.log("MP", "Shutdown initiated");
    }

    public boolean isMultiplayer() {
        return this.isMultiplayer;
    }

    public boolean isHost() {
        return this.isHost;
    }

    public boolean isGameStarted() {
        return this.gameStarted;
    }

    public int getLocalPlayerId() {
        return this.localPlayerId;
    }

    public long getRandomSeed() {
        return this.randomSeed;
    }

    public String getScenarioPath() {
        return this.scenarioPath;
    }

    public NetworkProtocol.LobbyState getLatestLobbyState() {
        return this.latestLobbyState;
    }

    public NetworkProtocol.GameSettings getLatestGameSettings() {
        return this.latestGameSettings;
    }

    public GameClient getClient() {
        return this.client;
    }

    public GameServer getServer() {
        return this.server;
    }

    public SteamGameServer getSteamServer() {
        return this.steamServer;
    }

    public String getPlayerName() {
        return this.playerName;
    }

    public void setPlayerName(String name) {
        if (name != null && !name.isEmpty()) {
            this.playerName = name;
        }
    }

    public static interface MultiplayerListener {
        public void onConnected(int var1);

        public void onDisconnected();

        public void onLobbyUpdated(NetworkProtocol.LobbyState var1);

        public void onGameStarting(String var1, long var2, List<NetworkProtocol.PlayerInfo> var4);

        public void onPlayerDisconnected(String var1);

        public void onCountrySelectRejected(int var1, String var2);

        public void onHostDisconnectedDuringGame();
    }

    public static enum TransportMode {
        LAN,
        STEAM;

    }

    private static class PendingTick {
        List<InputAction> actions;

        PendingTick(int tick, List<InputAction> actions) {
            this.actions = actions;
        }
    }
}
