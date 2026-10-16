/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.esotericsoftware.kryo.Kryo
 *  com.esotericsoftware.kryonet.EndPoint
 *  ru.ius.david.cgs.game.GovernmentDesign
 *  ru.ius.david.cgs.game.InputAction
 *  ru.ius.david.cgs.game.actions.AllianceInviteAction
 *  ru.ius.david.cgs.game.actions.AllianceMembershipAction
 *  ru.ius.david.cgs.game.actions.AllianceRulesAction
 *  ru.ius.david.cgs.game.actions.AllyAction
 *  ru.ius.david.cgs.game.actions.AssignBuildingToCityAction
 *  ru.ius.david.cgs.game.actions.BuyAutoclickerAction
 *  ru.ius.david.cgs.game.actions.CallAlliesAction
 *  ru.ius.david.cgs.game.actions.CancelJustificationAction
 *  ru.ius.david.cgs.game.actions.CancelTradeContractAction
 *  ru.ius.david.cgs.game.actions.CedeCapitulationTermsAction
 *  ru.ius.david.cgs.game.actions.CedeLandAction
 *  ru.ius.david.cgs.game.actions.ChangeSpeedAction
 *  ru.ius.david.cgs.game.actions.ClickAttackAction
 *  ru.ius.david.cgs.game.actions.ClickBoostProductionAction
 *  ru.ius.david.cgs.game.actions.ClickBuildAirDefenseAction
 *  ru.ius.david.cgs.game.actions.ClickBuildSiloAction
 *  ru.ius.david.cgs.game.actions.ClickDiplomacyAction
 *  ru.ius.david.cgs.game.actions.ClickFortifyAction
 *  ru.ius.david.cgs.game.actions.DeclareWarAction
 *  ru.ius.david.cgs.game.actions.DeclareWarOn3rdAction
 *  ru.ius.david.cgs.game.actions.DemandGoldAction
 *  ru.ius.david.cgs.game.actions.DiplomacyAction
 *  ru.ius.david.cgs.game.actions.DiplomacyAction$DiplomacyType
 *  ru.ius.david.cgs.game.actions.DiplomacyCampaignAction
 *  ru.ius.david.cgs.game.actions.DisbandUnitAction
 *  ru.ius.david.cgs.game.actions.EconomicUnionRulesAction
 *  ru.ius.david.cgs.game.actions.FinanceAction
 *  ru.ius.david.cgs.game.actions.FormNationAction
 *  ru.ius.david.cgs.game.actions.FormUnitAction
 *  ru.ius.david.cgs.game.actions.FoundCityAction
 *  ru.ius.david.cgs.game.actions.GovernmentAction
 *  ru.ius.david.cgs.game.actions.GovernmentAction$ActionType
 *  ru.ius.david.cgs.game.actions.IntegrateVassalAction
 *  ru.ius.david.cgs.game.actions.InternalPoliticsAction
 *  ru.ius.david.cgs.game.actions.JoinAllyWarAction
 *  ru.ius.david.cgs.game.actions.JustifyWarAction
 *  ru.ius.david.cgs.game.actions.KickAllyAction
 *  ru.ius.david.cgs.game.actions.LaunchLaserAction
 *  ru.ius.david.cgs.game.actions.LaunchNukeAction
 *  ru.ius.david.cgs.game.actions.LeaveAllianceAction
 *  ru.ius.david.cgs.game.actions.LendLeaseAction
 *  ru.ius.david.cgs.game.actions.ManualClickAction
 *  ru.ius.david.cgs.game.actions.NavalInvasionAction
 *  ru.ius.david.cgs.game.actions.NonAggressionPactAction
 *  ru.ius.david.cgs.game.actions.OffensivePlanAction
 *  ru.ius.david.cgs.game.actions.PeaceAction
 *  ru.ius.david.cgs.game.actions.PinProductionAllocationAction
 *  ru.ius.david.cgs.game.actions.ProposeLoanAction
 *  ru.ius.david.cgs.game.actions.ProposeTerritoryLeaseAction
 *  ru.ius.david.cgs.game.actions.ProposeTerritoryPurchaseAction
 *  ru.ius.david.cgs.game.actions.ProposeTradeContractAction
 *  ru.ius.david.cgs.game.actions.ProxyWarAction
 *  ru.ius.david.cgs.game.actions.PullOutOfAllianceAction
 *  ru.ius.david.cgs.game.actions.PuppetAction
 *  ru.ius.david.cgs.game.actions.RefuseLeaseReturnAction
 *  ru.ius.david.cgs.game.actions.RegimeChangeAction
 *  ru.ius.david.cgs.game.actions.RenameCityAction
 *  ru.ius.david.cgs.game.actions.RepayMarketLoanAction
 *  ru.ius.david.cgs.game.actions.RequestMarketAccessAction
 *  ru.ius.david.cgs.game.actions.RequestPeaceAction
 *  ru.ius.david.cgs.game.actions.RequestTerritoryLeaseAction
 *  ru.ius.david.cgs.game.actions.ResearchTechAction
 *  ru.ius.david.cgs.game.actions.RevokeMarketAccessAction
 *  ru.ius.david.cgs.game.actions.SelectCountryAction
 *  ru.ius.david.cgs.game.actions.SendGiftAction
 *  ru.ius.david.cgs.game.actions.SendGoldAction
 *  ru.ius.david.cgs.game.actions.SendResourcesAction
 *  ru.ius.david.cgs.game.actions.SetAllianceDoctrineAction
 *  ru.ius.david.cgs.game.actions.SetArmyDoctrineAction
 *  ru.ius.david.cgs.game.actions.SetArmyPlanAction
 *  ru.ius.david.cgs.game.actions.SetLandingOpAction
 *  ru.ius.david.cgs.game.actions.SetProductionAllocationAction
 *  ru.ius.david.cgs.game.actions.SetProductionDecreeAction
 *  ru.ius.david.cgs.game.actions.SetProductionFocusAction
 *  ru.ius.david.cgs.game.actions.SetResearchFocusAction
 *  ru.ius.david.cgs.game.actions.SetTradeAutomationAction
 *  ru.ius.david.cgs.game.actions.SetTradeContractRenewAction
 *  ru.ius.david.cgs.game.actions.SetWarEconomyAction
 *  ru.ius.david.cgs.game.actions.SignConferencePeaceAction
 *  ru.ius.david.cgs.game.actions.SponsorUprisingAction
 *  ru.ius.david.cgs.game.actions.StartAssimilationAction
 *  ru.ius.david.cgs.game.actions.StartColonyAction
 *  ru.ius.david.cgs.game.actions.StartIntegrationAction
 *  ru.ius.david.cgs.game.actions.TakeMarketLoanAction
 *  ru.ius.david.cgs.game.actions.ToggleProductionPauseAction
 *  ru.ius.david.cgs.game.actions.TransferGoldReserveAction
 *  ru.ius.david.cgs.game.actions.TributeAction
 *  ru.ius.david.cgs.game.actions.UltimatumAction
 *  ru.ius.david.cgs.game.actions.UnitOrderAction
 *  ru.ius.david.cgs.game.actions.UpgradeAutoclickerAction
 *  ru.ius.david.cgs.game.actions.UpgradeBuildingAction
 *  ru.ius.david.cgs.game.actions.UsurpLeadershipAction
 *  ru.ius.david.cgs.game.actions.VassalLandAction
 *  ru.ius.david.cgs.menus.CGSUpgradesMenu$UpgradeType
 */
package ru.ius.david.cgs.multiplayer;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryonet.EndPoint;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import ru.ius.david.cgs.game.GovernmentDesign;
import ru.ius.david.cgs.game.InputAction;
import ru.ius.david.cgs.game.actions.AllianceInviteAction;
import ru.ius.david.cgs.game.actions.AllianceMembershipAction;
import ru.ius.david.cgs.game.actions.AllianceRulesAction;
import ru.ius.david.cgs.game.actions.AllyAction;
import ru.ius.david.cgs.game.actions.AssignBuildingToCityAction;
import ru.ius.david.cgs.game.actions.BuyAutoclickerAction;
import ru.ius.david.cgs.game.actions.CallAlliesAction;
import ru.ius.david.cgs.game.actions.CancelJustificationAction;
import ru.ius.david.cgs.game.actions.CancelTradeContractAction;
import ru.ius.david.cgs.game.actions.CedeCapitulationTermsAction;
import ru.ius.david.cgs.game.actions.CedeLandAction;
import ru.ius.david.cgs.game.actions.ChangeSpeedAction;
import ru.ius.david.cgs.game.actions.ClickAttackAction;
import ru.ius.david.cgs.game.actions.ClickBoostProductionAction;
import ru.ius.david.cgs.game.actions.ClickBuildAirDefenseAction;
import ru.ius.david.cgs.game.actions.ClickBuildSiloAction;
import ru.ius.david.cgs.game.actions.ClickDiplomacyAction;
import ru.ius.david.cgs.game.actions.ClickFortifyAction;
import ru.ius.david.cgs.game.actions.DeclareWarAction;
import ru.ius.david.cgs.game.actions.DeclareWarOn3rdAction;
import ru.ius.david.cgs.game.actions.DemandGoldAction;
import ru.ius.david.cgs.game.actions.DiplomacyAction;
import ru.ius.david.cgs.game.actions.DiplomacyCampaignAction;
import ru.ius.david.cgs.game.actions.DisbandUnitAction;
import ru.ius.david.cgs.game.actions.EconomicUnionRulesAction;
import ru.ius.david.cgs.game.actions.FinanceAction;
import ru.ius.david.cgs.game.actions.FormNationAction;
import ru.ius.david.cgs.game.actions.FormUnitAction;
import ru.ius.david.cgs.game.actions.FoundCityAction;
import ru.ius.david.cgs.game.actions.GovernmentAction;
import ru.ius.david.cgs.game.actions.IntegrateVassalAction;
import ru.ius.david.cgs.game.actions.InternalPoliticsAction;
import ru.ius.david.cgs.game.actions.JoinAllyWarAction;
import ru.ius.david.cgs.game.actions.JustifyWarAction;
import ru.ius.david.cgs.game.actions.KickAllyAction;
import ru.ius.david.cgs.game.actions.LaunchLaserAction;
import ru.ius.david.cgs.game.actions.LaunchNukeAction;
import ru.ius.david.cgs.game.actions.LeaveAllianceAction;
import ru.ius.david.cgs.game.actions.LendLeaseAction;
import ru.ius.david.cgs.game.actions.ManualClickAction;
import ru.ius.david.cgs.game.actions.NavalInvasionAction;
import ru.ius.david.cgs.game.actions.NonAggressionPactAction;
import ru.ius.david.cgs.game.actions.OffensivePlanAction;
import ru.ius.david.cgs.game.actions.PeaceAction;
import ru.ius.david.cgs.game.actions.PinProductionAllocationAction;
import ru.ius.david.cgs.game.actions.ProposeLoanAction;
import ru.ius.david.cgs.game.actions.ProposeTerritoryLeaseAction;
import ru.ius.david.cgs.game.actions.ProposeTerritoryPurchaseAction;
import ru.ius.david.cgs.game.actions.ProposeTradeContractAction;
import ru.ius.david.cgs.game.actions.ProxyWarAction;
import ru.ius.david.cgs.game.actions.PullOutOfAllianceAction;
import ru.ius.david.cgs.game.actions.PuppetAction;
import ru.ius.david.cgs.game.actions.RefuseLeaseReturnAction;
import ru.ius.david.cgs.game.actions.RegimeChangeAction;
import ru.ius.david.cgs.game.actions.RenameCityAction;
import ru.ius.david.cgs.game.actions.RepayMarketLoanAction;
import ru.ius.david.cgs.game.actions.RequestMarketAccessAction;
import ru.ius.david.cgs.game.actions.RequestPeaceAction;
import ru.ius.david.cgs.game.actions.RequestTerritoryLeaseAction;
import ru.ius.david.cgs.game.actions.ResearchTechAction;
import ru.ius.david.cgs.game.actions.RevokeMarketAccessAction;
import ru.ius.david.cgs.game.actions.SelectCountryAction;
import ru.ius.david.cgs.game.actions.SendGiftAction;
import ru.ius.david.cgs.game.actions.SendGoldAction;
import ru.ius.david.cgs.game.actions.SendResourcesAction;
import ru.ius.david.cgs.game.actions.SetAllianceDoctrineAction;
import ru.ius.david.cgs.game.actions.SetArmyDoctrineAction;
import ru.ius.david.cgs.game.actions.SetArmyPlanAction;
import ru.ius.david.cgs.game.actions.SetLandingOpAction;
import ru.ius.david.cgs.game.actions.SetProductionAllocationAction;
import ru.ius.david.cgs.game.actions.SetProductionDecreeAction;
import ru.ius.david.cgs.game.actions.SetProductionFocusAction;
import ru.ius.david.cgs.game.actions.SetResearchFocusAction;
import ru.ius.david.cgs.game.actions.SetTradeAutomationAction;
import ru.ius.david.cgs.game.actions.SetTradeContractRenewAction;
import ru.ius.david.cgs.game.actions.SetWarEconomyAction;
import ru.ius.david.cgs.game.actions.SignConferencePeaceAction;
import ru.ius.david.cgs.game.actions.SponsorUprisingAction;
import ru.ius.david.cgs.game.actions.StartAssimilationAction;
import ru.ius.david.cgs.game.actions.StartColonyAction;
import ru.ius.david.cgs.game.actions.StartIntegrationAction;
import ru.ius.david.cgs.game.actions.TakeMarketLoanAction;
import ru.ius.david.cgs.game.actions.ToggleProductionPauseAction;
import ru.ius.david.cgs.game.actions.TransferGoldReserveAction;
import ru.ius.david.cgs.game.actions.TributeAction;
import ru.ius.david.cgs.game.actions.UltimatumAction;
import ru.ius.david.cgs.game.actions.UnitOrderAction;
import ru.ius.david.cgs.game.actions.UpgradeAutoclickerAction;
import ru.ius.david.cgs.game.actions.UpgradeBuildingAction;
import ru.ius.david.cgs.game.actions.UsurpLeadershipAction;
import ru.ius.david.cgs.game.actions.VassalLandAction;
import ru.ius.david.cgs.menus.CGSUpgradesMenu;
import ru.ius.david.cgs.multiplayer.WorldStateSnapshot;

public class NetworkProtocol {
    public static final int TCP_PORT = 54555;
    public static final int UDP_PORT = 54777;
    public static final int BUFFER_SIZE = 0x400000;

    public static Kryo createAndRegisterKryo() {
        Kryo kryo = new Kryo();
        kryo.setRegistrationRequired(false);
        NetworkProtocol.registerToKryo(kryo);
        return kryo;
    }

    public static void register(EndPoint endPoint) {
        NetworkProtocol.registerToKryo(endPoint.getKryo());
    }

    private static void registerToKryo(Kryo kryo) {
        kryo.register(ArrayList.class);
        kryo.register(String.class);
        kryo.register(int[].class);
        kryo.register(HashMap.class);
        kryo.register(JoinRequest.class);
        kryo.register(JoinResponse.class);
        kryo.register(LobbyState.class);
        kryo.register(PlayerInfo.class);
        kryo.register(CountrySelectRequest.class);
        kryo.register(PlayerReady.class);
        kryo.register(GameStartMessage.class);
        kryo.register(InputMessage.class);
        kryo.register(TickMessage.class);
        kryo.register(WorldStateSyncMessage.class);
        kryo.register(WorldStateSnapshot.class);
        kryo.register(WorldStateSnapshot.CountrySnapshot.class);
        kryo.register(WorldStateSnapshot.WarSnapshot.class);
        kryo.register(WorldStateSnapshot.WaveSnapshot.class);
        kryo.register(WorldStateSnapshot.ShipSnapshot.class);
        kryo.register(WorldStateSnapshot.UnitSnapshot.class);
        kryo.register(WorldStateSnapshot.TerritoryDelta.class);
        kryo.register(WorldStateSnapshot.FortificationSnapshot.class);
        kryo.register(WorldStateSnapshot.SiloSnapshot.class);
        kryo.register(WorldStateSnapshot.AirDefenseSnapshot.class);
        kryo.register(WorldStateSnapshot.CitySnapshot.class);
        kryo.register(PlayerDisconnected.class);
        kryo.register(PingRequest.class);
        kryo.register(PingResponse.class);
        kryo.register(DiscoveryResponse.class);
        kryo.register(SyncRequestMessage.class);
        kryo.register(GameSettings.class);
        kryo.register(GameSettingsUpdate.class);
        kryo.register(CountrySelectRejected.class);
        kryo.register(WorldStateSnapshot.NewCountryInfo.class);
        kryo.register(InputAction.class);
        kryo.register(ClickAttackAction.class);
        kryo.register(ClickFortifyAction.class);
        kryo.register(ClickBuildSiloAction.class);
        kryo.register(ClickBuildAirDefenseAction.class);
        kryo.register(NavalInvasionAction.class);
        kryo.register(DeclareWarAction.class);
        kryo.register(DiplomacyAction.class);
        kryo.register(DiplomacyAction.DiplomacyType.class);
        kryo.register(PeaceAction.class);
        kryo.register(PuppetAction.class);
        kryo.register(TributeAction.class);
        kryo.register(UpgradeBuildingAction.class);
        kryo.register(CGSUpgradesMenu.UpgradeType.class);
        kryo.register(ManualClickAction.class);
        kryo.register(FoundCityAction.class);
        kryo.register(RenameCityAction.class);
        kryo.register(StartColonyAction.class);
        kryo.register(ClickDiplomacyAction.class);
        kryo.register(SendGoldAction.class);
        kryo.register(SendGiftAction.class);
        kryo.register(AllyAction.class);
        kryo.register(ProxyWarAction.class);
        kryo.register(UltimatumAction.class);
        kryo.register(KickAllyAction.class);
        kryo.register(AllianceRulesAction.class);
        kryo.register(TransferGoldReserveAction.class);
        kryo.register(FinanceAction.class);
        kryo.register(InternalPoliticsAction.class);
        kryo.register(SetWarEconomyAction.class);
        kryo.register(GovernmentAction.class);
        kryo.register(GovernmentAction.ActionType.class);
        kryo.register(SelectCountryAction.class);
        kryo.register(FormNationAction.class);
        kryo.register(ResearchTechAction.class);
        kryo.register(SetResearchFocusAction.class);
        kryo.register(LaunchNukeAction.class);
        kryo.register(LaunchLaserAction.class);
        kryo.register(RequestPeaceAction.class);
        kryo.register(ToggleProductionPauseAction.class);
        kryo.register(SetProductionAllocationAction.class);
        kryo.register(PinProductionAllocationAction.class);
        kryo.register(SetProductionFocusAction.class);
        kryo.register(SetProductionDecreeAction.class);
        kryo.register(ClickBoostProductionAction.class);
        kryo.register(AssignBuildingToCityAction.class);
        kryo.register(BuyAutoclickerAction.class);
        kryo.register(ChangeSpeedAction.class);
        kryo.register(FormUnitAction.class);
        kryo.register(UnitOrderAction.class);
        kryo.register(JustifyWarAction.class);
        kryo.register(CancelJustificationAction.class);
        kryo.register(byte[].class);
        kryo.register(long[].class);
        kryo.register(WorldStateSnapshot.BulkTransfer.class);
        kryo.register(FullTerritorySyncMessage.class);
        kryo.register(MessageFragment.class);
        kryo.register(WorldEventMessage.class);
        kryo.register(SignConferencePeaceAction.class);
        kryo.register(DisbandUnitAction.class);
        kryo.register(NonAggressionPactAction.class);
        kryo.register(IntegrateVassalAction.class);
        kryo.register(VassalLandAction.class);
        kryo.register(CedeLandAction.class);
        kryo.register(UpgradeAutoclickerAction.class);
        kryo.register(SetArmyPlanAction.class);
        kryo.register(SetLandingOpAction.class);
        kryo.register(HashSet.class);
        kryo.register(Long.class);
        kryo.register(TakeMarketLoanAction.class);
        kryo.register(RepayMarketLoanAction.class);
        kryo.register(RefuseLeaseReturnAction.class);
        kryo.register(RequestMarketAccessAction.class);
        kryo.register(ProposeTradeContractAction.class);
        kryo.register(ProposeTerritoryPurchaseAction.class);
        kryo.register(ProposeTerritoryLeaseAction.class);
        kryo.register(ProposeLoanAction.class);
        kryo.register(DemandGoldAction.class);
        kryo.register(DeclareWarOn3rdAction.class);
        kryo.register(StartIntegrationAction.class);
        kryo.register(StartAssimilationAction.class);
        kryo.register(CancelTradeContractAction.class);
        kryo.register(RevokeMarketAccessAction.class);
        kryo.register(SetArmyDoctrineAction.class);
        kryo.register(RequestTerritoryLeaseAction.class);
        kryo.register(GovernmentDesign.class);
        kryo.register(RegimeChangeAction.class);
        kryo.register(AllianceInviteAction.class);
        kryo.register(AllianceMembershipAction.class);
        kryo.register(SetAllianceDoctrineAction.class);
        kryo.register(EconomicUnionRulesAction.class);
        kryo.register(LendLeaseAction.class);
        kryo.register(JoinAllyWarAction.class);
        kryo.register(SendResourcesAction.class);
        kryo.register(CallAlliesAction.class);
        kryo.register(LeaveAllianceAction.class);
        kryo.register(UsurpLeadershipAction.class);
        kryo.register(PullOutOfAllianceAction.class);
        kryo.register(SetTradeContractRenewAction.class);
        kryo.register(SponsorUprisingAction.class);
        kryo.register(CedeCapitulationTermsAction.class);
        kryo.register(SetTradeAutomationAction.class);
        kryo.register(OffensivePlanAction.class);
        kryo.register(DiplomacyCampaignAction.class);
        try {
            kryo.register(cgs.mods.multiplayer.actions.ChatMessageAction.class);
            kryo.register(cgs.mods.multiplayer.actions.TacticalPingAction.class);
            kryo.register(cgs.mods.multiplayer.actions.QuickAidGoldAction.class);
            kryo.register(cgs.mods.multiplayer.actions.TransferTroopAction.class);
        } catch(Throwable ignored) {}
    }

    public static class JoinRequest {
        public String playerName;
        public int graphicsQuality = -1;
    }

    public static class JoinResponse {
        public int playerId;
        public String scenarioPath;
        public long randomSeed;
    }

    public static class LobbyState {
        public ArrayList<PlayerInfo> players = new ArrayList();
        public String scenarioPath;
        public boolean gameStarted;
        public GameSettings gameSettings;
    }

    public static class PlayerInfo {
        public int playerId;
        public String playerName;
        public int selectedColorKey = -1;
        public String selectedCountryName;
        public boolean ready;
        public boolean isHost;
        public int graphicsQuality = -1;
    }

    public static class CountrySelectRequest {
        public int colorKey;
    }

    public static class PlayerReady {
        public boolean ready;
    }

    public static class GameStartMessage {
        public long randomSeed;
        public String scenarioPath;
        public ArrayList<PlayerInfo> players = new ArrayList();
        public GameSettings gameSettings;
    }

    public static class InputMessage {
        public ArrayList<InputAction> actions = new ArrayList();
    }

    public static class TickMessage {
        public int tickNumber;
        public ArrayList<InputAction> actions = new ArrayList();
    }

    public static class WorldStateSyncMessage {
        public WorldStateSnapshot worldState;
    }

    public static class PlayerDisconnected {
        public int playerId;
        public String playerName;
    }

    public static class PingRequest {
        public long sendTime;
    }

    public static class PingResponse {
        public long sendTime;
    }

    public static class DiscoveryResponse {
        public String hostName;
        public int playerCount;
        public int maxPlayers;
        public String scenarioName;
    }

    public static class SyncRequestMessage {
    }

    public static class GameSettings {
        public String scenarioPath = "scenarios/1936/";
        public int difficulty = 2;
        public float gameSpeed = 1.0f;
    }

    public static class GameSettingsUpdate {
        public GameSettings settings;
    }

    public static class CountrySelectRejected {
        public int colorKey;
        public String takenByPlayer;
    }

    public static class FullTerritorySyncMessage {
        public int tickNumber;
        public int mapWidth;
        public int mapHeight;
        public int[] runColors;
        public int[] runLengths;
    }

    public static class MessageFragment {
        public int msgId;
        public int index;
        public int count;
        public byte[] payload;
    }

    public static class WorldEventMessage {
        public WorldStateSnapshot events;
    }
}
