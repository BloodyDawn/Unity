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
package org.l2junity.gameserver.instancemanager;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import org.l2junity.commons.util.Rnd;
import org.l2junity.gameserver.enums.CategoryType;
import org.l2junity.gameserver.enums.CeremonyOfChaosState;
import org.l2junity.gameserver.model.L2Clan;
import org.l2junity.gameserver.model.World;
import org.l2junity.gameserver.model.actor.instance.PlayerInstance;
import org.l2junity.gameserver.model.ceremonyofchaos.CeremonyOfChaosEvent;
import org.l2junity.gameserver.model.ceremonyofchaos.CeremonyOfChaosMember;
import org.l2junity.gameserver.model.eventengine.AbstractEventManager;
import org.l2junity.gameserver.model.eventengine.ScheduleTarget;
import org.l2junity.gameserver.model.events.EventType;
import org.l2junity.gameserver.model.events.ListenerRegisterType;
import org.l2junity.gameserver.model.events.annotations.RegisterEvent;
import org.l2junity.gameserver.model.events.annotations.RegisterType;
import org.l2junity.gameserver.model.events.impl.character.player.OnPlayerBypass;
import org.l2junity.gameserver.network.client.send.ceremonyofchaos.ExCuriousHouseState;
import org.l2junity.gameserver.network.client.send.string.SystemMessageId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Sdw
 */
public class CeremonyOfChaosManager extends AbstractEventManager<CeremonyOfChaosEvent>
{
	protected static final Logger LOGGER = LoggerFactory.getLogger(CeremonyOfChaosManager.class);
	
	public static final String BUFF_KEY = "buff";
	public static final String ITEMS_KEY = "items";
	public static final String MAX_PLAYERS_KEY = "max_players";
	public static final String INSTANCE_TEMPLATES_KEY = "instance_templates";
	
	// Used for holding registered player
	protected final Map<Integer, PlayerInstance> _waitingList = new ConcurrentHashMap<>();
	// Used for holding player in Arena- THAT OR PLAYERINSTANCE BOOL ?
	protected final Map<Integer, PlayerInstance> _participingList = new ConcurrentHashMap<>();
	
	public CeremonyOfChaosManager()
	{
		
	}
	
	@Override
	public void onInitialized()
	{
		
	}
	
	@ScheduleTarget
	public void onPeriodEnd(String text)
	{
		LOGGER.info("Period ended");
		LOGGER.info(text);
	}
	
	@ScheduleTarget
	public void onEventStart()
	{
		LOGGER.info("on event start");
	}
	
	@ScheduleTarget
	public void onEventEnd()
	{
		LOGGER.info("on event event");
	}
	
	@ScheduleTarget
	public void onRegistrationStart()
	{
		setState(CeremonyOfChaosState.REGISTRATION);
		
		for (PlayerInstance player : World.getInstance().getPlayers())
		{
			if (player.isOnline())
			{
				player.sendPacket(SystemMessageId.REGISTRATION_FOR_THE_CEREMONY_OF_CHAOS_HAS_BEGUN);
				if (canRegister(player, false))
				{
					player.sendPacket(ExCuriousHouseState.REGISTRATION_PACKET);
				}
			}
		}
	}
	
	@ScheduleTarget
	public void onRegistrationEnd()
	{
		setState(CeremonyOfChaosState.PREPARING_FOR_TELEPORT);
		for (PlayerInstance player : World.getInstance().getPlayers())
		{
			if (player.isOnline())
			{
				player.sendPacket(SystemMessageId.REGISTRATION_FOR_THE_CEREMONY_OF_CHAOS_HAS_ENDED);
				if (!isInWaitingList(player))
				{
					player.sendPacket(ExCuriousHouseState.IDLE_PACKET);
				}
				else
				{
					// Notify TP in 2 minutes
				}
			}
		}
	}
	
	@ScheduleTarget
	public void onPrepareForFight()
	{
		setState(CeremonyOfChaosState.PREPARING_FOR_FIGHT);
		int eventId = 0;
		int position = 1;
		CeremonyOfChaosEvent event = null;
		final List<PlayerInstance> players = _waitingList.values().stream().sorted(Comparator.comparingInt(PlayerInstance::getLevel)).collect(Collectors.toList());
		final int maxPlayers = getVariables().getInt(MAX_PLAYERS_KEY, 18);
		final List<String> templates = getVariables().getList(INSTANCE_TEMPLATES_KEY, String.class);
		for (PlayerInstance player : players)
		{
			if (player.isOnline() && canRegister(player, true))
			{
				_participingList.put(player.getObjectId(), player);
				
				if ((event == null) || (event.getPlayers().size() >= maxPlayers))
				{
					
					event = new CeremonyOfChaosEvent(eventId++, templates.get(Rnd.get(templates.size())));
					position = 1;
					getEvents().add(event);
				}
				
				event.addPlayer(new CeremonyOfChaosMember(player, position++));
			}
			else
			{
				// TODO: Handle player penalties
			}
		}
		
		// Prepare all event's players for start
		getEvents().forEach(CeremonyOfChaosEvent::preparePlayers);
	}
	
	@ScheduleTarget
	public void onStartFight()
	{
		setState(CeremonyOfChaosState.RUNNING);
		getEvents().forEach(CeremonyOfChaosEvent::startFight);
	}
	
	@ScheduleTarget
	public void onEndFight()
	{
		setState(CeremonyOfChaosState.SCHEDULED);
		getEvents().forEach(CeremonyOfChaosEvent::stopFight);
	}
	
	public void addInWaitingList(PlayerInstance player)
	{
		_waitingList.put(player.getObjectId(), player);
	}
	
	public void removeFromWaitingList(PlayerInstance player)
	{
		_waitingList.remove(player.getObjectId());
	}
	
	public boolean isInWaitingList(PlayerInstance player)
	{
		return _waitingList.containsKey(player.getObjectId());
	}
	
	public boolean canRegister(PlayerInstance player, boolean sendMessage)
	{
		boolean canRegister = true;
		
		final L2Clan clan = player.getClan();
		
		SystemMessageId sm = null;
		
		if (CeremonyOfChaosManager.getInstance().getState() != CeremonyOfChaosState.REGISTRATION)
		{
			canRegister = false;
		}
		else if (player.getLevel() < 85)
		{
			sm = SystemMessageId.ONLY_CHARACTERS_LEVEL_85_OR_ABOVE_MAY_PARTICIPATE_IN_THE_TOURNAMENT;
			canRegister = false;
		}
		else if (player.isFlyingMounted())
		{
			sm = SystemMessageId.YOU_CANNOT_PARTICIPATE_IN_THE_CEREMONY_OF_CHAOS_AS_A_FLYING_TRANSFORMED_OBJECT;
			canRegister = false;
		}
		else if (!player.isInCategory(CategoryType.AWAKEN_GROUP))
		{
			sm = SystemMessageId.ONLY_CHARACTERS_WHO_HAVE_COMPLETED_THE_3RD_CLASS_TRANSFER_MAY_PARTICIPATE;
			canRegister = false;
		}
		else if ((player.getInventory().getSize(false) >= (player.getInventoryLimit() * 0.8)) || (player.getWeightPenalty() >= 3))
		{
			sm = SystemMessageId.UNABLE_TO_PROCESS_THIS_REQUEST_UNTIL_YOUR_INVENTORY_S_WEIGHT_AND_SLOT_COUNT_ARE_LESS_THAN_80_PERCENT_OF_CAPACITY;
			canRegister = false;
		}
		else if ((clan == null) || (clan.getLevel() < 6))
		{
			sm = SystemMessageId.ONLY_CHARACTERS_WHO_ARE_A_PART_OF_A_CLAN_OF_LEVEL_6_OR_ABOVE_MAY_PARTICIPATE;
			canRegister = false;
		}
		else if (getWaitingListCount() >= 72)
		{
			sm = SystemMessageId.THERE_ARE_TOO_MANY_CHALLENGERS_YOU_CANNOT_PARTICIPATE_NOW;
			canRegister = false;
		}
		else if (getState() != CeremonyOfChaosState.REGISTRATION)
		{
			sm = SystemMessageId.YOU_MAY_NOT_REGISTER_AS_A_PARTICIPANT;
			canRegister = false;
		}
		else if (player.isCursedWeaponEquipped() || (player.getReputation() < 0))
		{
			sm = SystemMessageId.WAITING_LIST_REGISTRATION_IS_NOT_ALLOWED_WHILE_THE_CURSED_SWORD_IS_BEING_USED_OR_THE_STATUS_IS_IN_A_CHAOTIC_STATE;
			canRegister = false;
		}
		else if (player.isInDuel())
		{
			sm = SystemMessageId.YOU_CANNOT_REGISTER_IN_THE_WAITING_LIST_DURING_A_DUEL;
			canRegister = false;
		}
		else if (player.isInOlympiadMode())
		{
			sm = SystemMessageId.YOU_CANNOT_REGISTER_IN_THE_WAITING_LIST_WHILE_PARTICIPATING_IN_OLYMPIAD;
			canRegister = false;
		}
		else if (player.getInstanceId() > 0)
		{
			// TODO : check if there is a message for that case
			canRegister = false;
		}
		else if (player.isInSiege())
		{
			sm = SystemMessageId.YOU_CANNOT_REGISTER_FOR_THE_WAITING_LIST_ON_THE_BATTLEFIELD_CASTLE_SIEGE_FORTRESS_SIEGE;
			canRegister = false;
		}
		else if (player.isInsideZone(org.l2junity.gameserver.model.zone.ZoneId.SIEGE))
		{
			sm = SystemMessageId.YOU_CANNOT_REGISTER_IN_THE_WAITING_LIST_WHILE_BEING_INSIDE_OF_A_BATTLEGROUND_CASTLE_SIEGE_FORTRESS_SIEGE;
			canRegister = false;
		}
		else if (isInWaitingList(player))
		{
			sm = SystemMessageId.YOU_ARE_ON_THE_WAITING_LIST_FOR_THE_CEREMONY_OF_CHAOS;
			canRegister = false;
		}
		
		if ((sm != null) && sendMessage)
		{
			player.sendPacket(sm);
		}
		
		return canRegister;
	}
	
	public int getWaitingListCount()
	{
		return _waitingList.size();
	}
	
	@RegisterEvent(EventType.ON_PLAYER_BYPASS)
	@RegisterType(ListenerRegisterType.GLOBAL_PLAYERS)
	public void OnPlayerBypass(OnPlayerBypass event)
	{
		final PlayerInstance player = event.getActiveChar();
		if (player == null)
		{
			return;
		}
		
		if (event.getCommand().equalsIgnoreCase("pledgegame?command=apply"))
		{
			if (canRegister(player, true))
			{
				CeremonyOfChaosManager.getInstance().addInWaitingList(player);
				player.sendPacket(SystemMessageId.YOU_ARE_NOW_ON_THE_WAITING_LIST_YOU_WILL_AUTOMATICALLY_BE_TELEPORTED_WHEN_THE_TOURNAMENT_STARTS_AND_WILL_BE_REMOVED_FROM_THE_WAITING_LIST_IF_YOU_LOG_OUT_IF_YOU_CANCEL_REGISTRATION_WITHIN_THE_LAST_MINUTE_OF_ENTERING_THE_ARENA_AFTER_SIGNING_UP_30_TIMES_OR_MORE_OR_FORFEIT_AFTER_ENTERING_THE_ARENA_30_TIMES_OR_MORE_DURING_A_CYCLE_YOU_BECOME_INELIGIBLE_FOR_PARTICIPATION_IN_THE_CEREMONY_OF_CHAOS_UNTIL_THE_NEXT_CYCLE_ALL_THE_BUFFS_EXCEPT_THE_VITALITY_BUFF_WILL_BE_REMOVED_ONCE_YOU_ENTER_THE_ARENAS);
				player.sendPacket(SystemMessageId.EXCEPT_THE_VITALITY_BUFF_ALL_BUFFS_INCLUDING_ART_OF_SEDUCTION_WILL_BE_DELETED);
				player.sendPacket(ExCuriousHouseState.PREPARE_PACKET);
			}
		}
	}
	
	// player leave world
	// player leave clan
	
	public static CeremonyOfChaosManager getInstance()
	{
		return SingletonHolder._instance;
	}
	
	private static class SingletonHolder
	{
		protected static final CeremonyOfChaosManager _instance = new CeremonyOfChaosManager();
	}
}
