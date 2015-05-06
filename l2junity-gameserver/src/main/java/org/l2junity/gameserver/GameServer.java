/*
 * Copyright (C) 2004-2015 L2J Server
 * 
 * This file is part of L2J Server.
 * 
 * L2J Server is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * L2J Server is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package org.l2junity.gameserver;

import java.awt.Toolkit;
import java.lang.management.ManagementFactory;
import java.time.Duration;
import java.util.Calendar;

import org.l2junity.Config;
import org.l2junity.DatabaseFactory;
import org.l2junity.Server;
import org.l2junity.UPnPService;
import org.l2junity.commons.util.DeadLockDetector;
import org.l2junity.gameserver.cache.HtmCache;
import org.l2junity.gameserver.data.sql.impl.AnnouncementsTable;
import org.l2junity.gameserver.data.sql.impl.CharNameTable;
import org.l2junity.gameserver.data.sql.impl.CharSummonTable;
import org.l2junity.gameserver.data.sql.impl.ClanTable;
import org.l2junity.gameserver.data.sql.impl.CrestTable;
import org.l2junity.gameserver.data.sql.impl.NpcBufferTable;
import org.l2junity.gameserver.data.sql.impl.OfflineTradersTable;
import org.l2junity.gameserver.data.sql.impl.SummonSkillsTable;
import org.l2junity.gameserver.data.sql.impl.TeleportLocationTable;
import org.l2junity.gameserver.data.xml.impl.AbilityPointsData;
import org.l2junity.gameserver.data.xml.impl.AdminData;
import org.l2junity.gameserver.data.xml.impl.AlchemyData;
import org.l2junity.gameserver.data.xml.impl.AppearanceItemData;
import org.l2junity.gameserver.data.xml.impl.ArmorSetsData;
import org.l2junity.gameserver.data.xml.impl.BeautyShopData;
import org.l2junity.gameserver.data.xml.impl.BuyListData;
import org.l2junity.gameserver.data.xml.impl.CategoryData;
import org.l2junity.gameserver.data.xml.impl.ClassListData;
import org.l2junity.gameserver.data.xml.impl.DoorData;
import org.l2junity.gameserver.data.xml.impl.EnchantItemData;
import org.l2junity.gameserver.data.xml.impl.EnchantItemGroupsData;
import org.l2junity.gameserver.data.xml.impl.EnchantItemHPBonusData;
import org.l2junity.gameserver.data.xml.impl.EnchantItemOptionsData;
import org.l2junity.gameserver.data.xml.impl.EnchantSkillGroupsData;
import org.l2junity.gameserver.data.xml.impl.ExperienceData;
import org.l2junity.gameserver.data.xml.impl.FishData;
import org.l2junity.gameserver.data.xml.impl.FishingMonstersData;
import org.l2junity.gameserver.data.xml.impl.FishingRodsData;
import org.l2junity.gameserver.data.xml.impl.HennaData;
import org.l2junity.gameserver.data.xml.impl.HitConditionBonusData;
import org.l2junity.gameserver.data.xml.impl.InitialEquipmentData;
import org.l2junity.gameserver.data.xml.impl.InitialShortcutData;
import org.l2junity.gameserver.data.xml.impl.ItemCrystalizationData;
import org.l2junity.gameserver.data.xml.impl.KarmaData;
import org.l2junity.gameserver.data.xml.impl.MultisellData;
import org.l2junity.gameserver.data.xml.impl.NpcData;
import org.l2junity.gameserver.data.xml.impl.OptionData;
import org.l2junity.gameserver.data.xml.impl.PetDataTable;
import org.l2junity.gameserver.data.xml.impl.PlayerTemplateData;
import org.l2junity.gameserver.data.xml.impl.PlayerXpPercentLostData;
import org.l2junity.gameserver.data.xml.impl.PrimeShopData;
import org.l2junity.gameserver.data.xml.impl.RecipeData;
import org.l2junity.gameserver.data.xml.impl.SayuneData;
import org.l2junity.gameserver.data.xml.impl.SecondaryAuthData;
import org.l2junity.gameserver.data.xml.impl.ShuttleData;
import org.l2junity.gameserver.data.xml.impl.SiegeScheduleData;
import org.l2junity.gameserver.data.xml.impl.SkillLearnData;
import org.l2junity.gameserver.data.xml.impl.SkillTreesData;
import org.l2junity.gameserver.data.xml.impl.StaticObjectData;
import org.l2junity.gameserver.data.xml.impl.TeleportersData;
import org.l2junity.gameserver.data.xml.impl.TransformData;
import org.l2junity.gameserver.data.xml.impl.UIData;
import org.l2junity.gameserver.datatables.AugmentationData;
import org.l2junity.gameserver.datatables.BotReportTable;
import org.l2junity.gameserver.datatables.EventDroplist;
import org.l2junity.gameserver.datatables.ItemTable;
import org.l2junity.gameserver.datatables.MerchantPriceConfigTable;
import org.l2junity.gameserver.datatables.SkillData;
import org.l2junity.gameserver.datatables.SpawnTable;
import org.l2junity.gameserver.handler.EffectHandler;
import org.l2junity.gameserver.idfactory.IdFactory;
import org.l2junity.gameserver.instancemanager.AirShipManager;
import org.l2junity.gameserver.instancemanager.AntiFeedManager;
import org.l2junity.gameserver.instancemanager.AuctionManager;
import org.l2junity.gameserver.instancemanager.BoatManager;
import org.l2junity.gameserver.instancemanager.CHSiegeManager;
import org.l2junity.gameserver.instancemanager.CastleManager;
import org.l2junity.gameserver.instancemanager.CastleManorManager;
import org.l2junity.gameserver.instancemanager.ClanEntryManager;
import org.l2junity.gameserver.instancemanager.ClanHallManager;
import org.l2junity.gameserver.instancemanager.CursedWeaponsManager;
import org.l2junity.gameserver.instancemanager.DayNightSpawnManager;
import org.l2junity.gameserver.instancemanager.FortManager;
import org.l2junity.gameserver.instancemanager.FortSiegeManager;
import org.l2junity.gameserver.instancemanager.FourSepulchersManager;
import org.l2junity.gameserver.instancemanager.GlobalVariablesManager;
import org.l2junity.gameserver.instancemanager.GraciaSeedsManager;
import org.l2junity.gameserver.instancemanager.GrandBossManager;
import org.l2junity.gameserver.instancemanager.InstanceManager;
import org.l2junity.gameserver.instancemanager.ItemAuctionManager;
import org.l2junity.gameserver.instancemanager.ItemsOnGroundManager;
import org.l2junity.gameserver.instancemanager.MailManager;
import org.l2junity.gameserver.instancemanager.MapRegionManager;
import org.l2junity.gameserver.instancemanager.MatchingRoomManager;
import org.l2junity.gameserver.instancemanager.MentorManager;
import org.l2junity.gameserver.instancemanager.PetitionManager;
import org.l2junity.gameserver.instancemanager.PunishmentManager;
import org.l2junity.gameserver.instancemanager.QuestManager;
import org.l2junity.gameserver.instancemanager.RaidBossSpawnManager;
import org.l2junity.gameserver.instancemanager.SiegeGuardManager;
import org.l2junity.gameserver.instancemanager.SiegeManager;
import org.l2junity.gameserver.instancemanager.WalkingManager;
import org.l2junity.gameserver.instancemanager.ZoneManager;
import org.l2junity.gameserver.model.AutoSpawnHandler;
import org.l2junity.gameserver.model.World;
import org.l2junity.gameserver.model.entity.Hero;
import org.l2junity.gameserver.model.entity.TvTManager;
import org.l2junity.gameserver.model.events.EventDispatcher;
import org.l2junity.gameserver.model.olympiad.Olympiad;
import org.l2junity.gameserver.network.client.ClientNetworkManager;
import org.l2junity.gameserver.pathfinding.PathFinding;
import org.l2junity.gameserver.script.faenor.FaenorScriptEngine;
import org.l2junity.gameserver.scripting.ScriptEngineManager;
import org.l2junity.gameserver.taskmanager.TaskManager;
import org.l2junity.gameserver.util.Broadcast;
import org.l2junity.status.Status;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GameServer
{
	private static final Logger LOGGER = LoggerFactory.getLogger(GameServer.class);
	
	private final DeadLockDetector _deadDetectThread;
	public static GameServer gameServer;
	public static final Calendar dateTimeServerStarted = Calendar.getInstance();
	
	public long getUsedMemoryMB()
	{
		return (Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()) / 1048576;
	}
	
	public DeadLockDetector getDeadLockDetectorThread()
	{
		return _deadDetectThread;
	}
	
	public GameServer() throws Exception
	{
		LOGGER.info("Used memory: {} MB", getUsedMemoryMB());
		
		if (!IdFactory.getInstance().isInitialized())
		{
			LOGGER.error("Could not read object IDs from DB. Please check your data.");
			throw new Exception("Could not initialize the ID factory");
		}
		
		ThreadPoolManager.getInstance();
		EventDispatcher.getInstance();
		
		// load script engines
		printSection("Scripting Engines");
		ScriptEngineManager.getInstance();
		
		printSection("World");
		// start game time control early
		GameTimeController.init();
		InstanceManager.getInstance();
		World.getInstance();
		MapRegionManager.getInstance();
		AnnouncementsTable.getInstance();
		GlobalVariablesManager.getInstance();
		
		printSection("Data");
		CategoryData.getInstance();
		SecondaryAuthData.getInstance();
		AbilityPointsData.getInstance();
		SayuneData.getInstance();
		
		printSection("Skills");
		EffectHandler.getInstance().executeScript();
		EnchantSkillGroupsData.getInstance();
		SkillTreesData.getInstance();
		SkillData.getInstance();
		SummonSkillsTable.getInstance();
		
		printSection("Items");
		ItemTable.getInstance();
		EnchantItemGroupsData.getInstance();
		EnchantItemData.getInstance();
		EnchantItemOptionsData.getInstance();
		ItemCrystalizationData.getInstance();
		OptionData.getInstance();
		EnchantItemHPBonusData.getInstance();
		MerchantPriceConfigTable.getInstance().loadInstances();
		BuyListData.getInstance();
		MultisellData.getInstance();
		RecipeData.getInstance();
		ArmorSetsData.getInstance();
		FishData.getInstance();
		FishingMonstersData.getInstance();
		FishingRodsData.getInstance();
		HennaData.getInstance();
		PrimeShopData.getInstance();
		AppearanceItemData.getInstance();
		AlchemyData.getInstance();
		
		printSection("Characters");
		ClassListData.getInstance();
		InitialEquipmentData.getInstance();
		InitialShortcutData.getInstance();
		ExperienceData.getInstance();
		PlayerXpPercentLostData.getInstance();
		KarmaData.getInstance();
		HitConditionBonusData.getInstance();
		PlayerTemplateData.getInstance();
		CharNameTable.getInstance();
		AdminData.getInstance();
		PetDataTable.getInstance();
		CharSummonTable.getInstance().init();
		BeautyShopData.getInstance();
		MentorManager.getInstance();
		
		printSection("Clans");
		ClanTable.getInstance();
		CHSiegeManager.getInstance();
		ClanHallManager.getInstance();
		AuctionManager.getInstance();
		ClanEntryManager.getInstance();
		
		printSection("Geodata");
		GeoData.getInstance();
		
		if (Config.PATHFINDING > 0)
		{
			PathFinding.getInstance();
		}
		
		printSection("NPCs");
		SkillLearnData.getInstance();
		NpcData.getInstance();
		WalkingManager.getInstance();
		StaticObjectData.getInstance();
		ZoneManager.getInstance();
		DoorData.getInstance();
		ItemAuctionManager.getInstance();
		CastleManager.getInstance().loadInstances();
		NpcBufferTable.getInstance();
		GrandBossManager.getInstance();
		EventDroplist.getInstance();
		
		printSection("Olympiad");
		Olympiad.getInstance();
		Hero.getInstance();
		
		// Call to load caches
		printSection("Cache");
		HtmCache.getInstance();
		CrestTable.getInstance();
		TeleportLocationTable.getInstance();
		TeleportersData.getInstance();
		UIData.getInstance();
		MatchingRoomManager.getInstance();
		PetitionManager.getInstance();
		AugmentationData.getInstance();
		CursedWeaponsManager.getInstance();
		TransformData.getInstance();
		BotReportTable.getInstance();
		
		printSection("Scripts");
		QuestManager.getInstance();
		BoatManager.getInstance();
		AirShipManager.getInstance();
		ShuttleData.getInstance();
		GraciaSeedsManager.getInstance();
		
		try
		{
			LOGGER.info("Loading server scripts...");
			ScriptEngineManager.getInstance().executeScriptList();
		}
		catch (Exception e)
		{
			LOGGER.warn("Failed to execute script list!", e);
		}
		
		SpawnTable.getInstance().load();
		DayNightSpawnManager.getInstance().trim().notifyChangeMode();
		FourSepulchersManager.getInstance().init();
		RaidBossSpawnManager.getInstance();
		
		printSection("Siege");
		SiegeManager.getInstance().getSieges();
		CastleManager.getInstance().activateInstances();
		FortManager.getInstance().loadInstances();
		FortManager.getInstance().activateInstances();
		FortSiegeManager.getInstance();
		SiegeScheduleData.getInstance();
		
		MerchantPriceConfigTable.getInstance().updateReferences();
		CastleManorManager.getInstance();
		SiegeGuardManager.getInstance();
		QuestManager.getInstance().report();
		
		if (Config.SAVE_DROPPED_ITEM)
		{
			ItemsOnGroundManager.getInstance();
		}
		
		if ((Config.AUTODESTROY_ITEM_AFTER > 0) || (Config.HERB_AUTO_DESTROY_TIME > 0))
		{
			ItemsAutoDestroy.getInstance();
		}
		
		MonsterRace.getInstance();
		AutoSpawnHandler.getInstance();
		
		FaenorScriptEngine.getInstance();
		// Init of a cursed weapon manager
		
		LOGGER.info("AutoSpawnHandler: Loaded {} handlers in total.", AutoSpawnHandler.getInstance().size());
		
		TaskManager.getInstance();
		
		AntiFeedManager.getInstance().registerEvent(AntiFeedManager.GAME_ID);
		
		if (Config.ALLOW_MAIL)
		{
			MailManager.getInstance();
		}
		
		PunishmentManager.getInstance();
		
		Runtime.getRuntime().addShutdownHook(Shutdown.getInstance());
		
		LOGGER.info("IdFactory: Free ObjectID's remaining: {}", IdFactory.getInstance().size());
		
		TvTManager.getInstance();
		
		if ((Config.OFFLINE_TRADE_ENABLE || Config.OFFLINE_CRAFT_ENABLE) && Config.RESTORE_OFFLINERS)
		{
			OfflineTradersTable.getInstance().restoreOfflineTraders();
		}
		
		if (Config.DEADLOCK_DETECTOR)
		{
			_deadDetectThread = new DeadLockDetector(Duration.ofSeconds(Config.DEADLOCK_CHECK_INTERVAL), () ->
			{
				if (Config.RESTART_ON_DEADLOCK)
				{
					Broadcast.toAllOnlinePlayers("Server has stability issues - restarting now.");
					Shutdown.getInstance().startTelnetShutdown("DeadLockDetector - Auto Restart", 60, true);
				}
			});
			_deadDetectThread.setDaemon(true);
			_deadDetectThread.start();
		}
		else
		{
			_deadDetectThread = null;
		}
		System.gc();
		// maxMemory is the upper limit the jvm can use, totalMemory the size of
		// the current allocation pool, freeMemory the unused memory in the allocation pool
		long freeMem = ((Runtime.getRuntime().maxMemory() - Runtime.getRuntime().totalMemory()) + Runtime.getRuntime().freeMemory()) / 1048576;
		long totalMem = Runtime.getRuntime().maxMemory() / 1048576;
		LOGGER.info("Started, free memory {} MB of {} MB", freeMem, totalMem);
		Toolkit.getDefaultToolkit().beep();
		LoginServerThread.getInstance().start();
		
		ClientNetworkManager.getInstance().start();
		
		LOGGER.info("Maximum numbers of connected players: {}", Config.MAXIMUM_ONLINE_USERS);
		LOGGER.info("Server loaded in {} seconds.", ManagementFactory.getRuntimeMXBean().getUptime() / 1000);
		
		printSection("UPnP");
		UPnPService.getInstance();
	}
	
	public static void main(String[] args) throws Exception
	{
		Server.serverMode = Server.MODE_GAMESERVER;
		// Initialize config
		Config.load();
		printSection("Database");
		DatabaseFactory.getInstance();
		gameServer = new GameServer();
		
		if (Config.IS_TELNET_ENABLED)
		{
			new Status(Server.serverMode).start();
		}
		else
		{
			LOGGER.info("Telnet server is currently disabled.");
		}
	}
	
	public static void printSection(String s)
	{
		s = "=[ " + s + " ]";
		while (s.length() < 61)
		{
			s = "-" + s;
		}
		LOGGER.info(s);
	}
}
