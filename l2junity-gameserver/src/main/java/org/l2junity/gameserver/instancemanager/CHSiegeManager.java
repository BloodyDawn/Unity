/*
 * Copyright (C) 2004-2015 L2J Unity
 * 
 * This file is part of L2J Unity.
 * 
 * L2J Unity is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * L2J Unity is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package org.l2junity.gameserver.instancemanager;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.l2junity.Config;
import org.l2junity.DatabaseFactory;
import org.l2junity.gameserver.model.L2Clan;
import org.l2junity.gameserver.model.StatsSet;
import org.l2junity.gameserver.model.actor.Creature;
import org.l2junity.gameserver.model.actor.instance.PlayerInstance;
import org.l2junity.gameserver.model.entity.clanhall.ClanHallSiegeEngine;
import org.l2junity.gameserver.model.entity.clanhall.SiegableHall;
import org.l2junity.gameserver.model.zone.type.ClanHallZone;
import org.l2junity.gameserver.network.client.send.SystemMessage;
import org.l2junity.gameserver.network.client.send.string.SystemMessageId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author BiggBoss
 */
public final class CHSiegeManager
{
	private static final Logger _log = LoggerFactory.getLogger(CHSiegeManager.class);
	private static final String SQL_LOAD_HALLS = "SELECT * FROM siegable_clanhall";
	
	private final Map<Integer, SiegableHall> _siegableHalls = new ConcurrentHashMap<>();
	
	protected CHSiegeManager()
	{
		loadClanHalls();
	}
	
	private void loadClanHalls()
	{
		try (Connection con = DatabaseFactory.getInstance().getConnection();
			Statement s = con.createStatement();
			ResultSet rs = s.executeQuery(SQL_LOAD_HALLS))
		{
			_siegableHalls.clear();
			
			while (rs.next())
			{
				final int id = rs.getInt("clanHallId");
				
				StatsSet set = new StatsSet();
				
				set.set("id", id);
				set.set("name", rs.getString("name"));
				set.set("ownerId", rs.getInt("ownerId"));
				set.set("desc", rs.getString("desc"));
				set.set("location", rs.getString("location"));
				set.set("nextSiege", rs.getLong("nextSiege"));
				set.set("siegeLenght", rs.getLong("siegeLenght"));
				set.set("scheduleConfig", rs.getString("schedule_config"));
				SiegableHall hall = new SiegableHall(set);
				_siegableHalls.put(id, hall);
				ClanHallManager.addClanHall(hall);
			}
			_log.info("Loaded {} conquerable clan halls.", _siegableHalls.size());
		}
		catch (Exception e)
		{
			_log.warn("Could not load siegable clan halls!:", e);
		}
	}
	
	public Map<Integer, SiegableHall> getConquerableHalls()
	{
		return _siegableHalls;
	}
	
	public SiegableHall getSiegableHall(int clanHall)
	{
		return getConquerableHalls().get(clanHall);
	}
	
	public final SiegableHall getNearbyClanHall(Creature activeChar)
	{
		return getNearbyClanHall(activeChar.getX(), activeChar.getY(), 10000);
	}
	
	public final SiegableHall getNearbyClanHall(int x, int y, int maxDist)
	{
		ClanHallZone zone = null;
		
		for (SiegableHall ch : _siegableHalls.values())
		{
			zone = ch.getZone();
			if ((zone != null) && (zone.getDistanceToZone(x, y) < maxDist))
			{
				return ch;
			}
		}
		return null;
	}
	
	public final ClanHallSiegeEngine getSiege(Creature character)
	{
		SiegableHall hall = getNearbyClanHall(character);
		if (hall == null)
		{
			return null;
		}
		return hall.getSiege();
	}
	
	public final void registerClan(L2Clan clan, SiegableHall hall, PlayerInstance player)
	{
		if (clan.getLevel() < Config.CHS_CLAN_MINLEVEL)
		{
			player.sendMessage("Only clans of level " + Config.CHS_CLAN_MINLEVEL + " or higher may register for a castle siege");
		}
		else if (hall.isWaitingBattle())
		{
			SystemMessage sm = SystemMessage.getSystemMessage(SystemMessageId.THE_DEADLINE_TO_REGISTER_FOR_THE_SIEGE_OF_S1_HAS_PASSED);
			sm.addString(hall.getName());
			player.sendPacket(sm);
		}
		else if (hall.isInSiege())
		{
			player.sendPacket(SystemMessageId.THIS_IS_NOT_THE_TIME_FOR_SIEGE_REGISTRATION_AND_SO_REGISTRATION_AND_CANCELLATION_CANNOT_BE_DONE);
		}
		else if (hall.getOwnerId() == clan.getId())
		{
			player.sendPacket(SystemMessageId.CASTLE_OWNING_CLANS_ARE_AUTOMATICALLY_REGISTERED_ON_THE_DEFENDING_SIDE);
		}
		else if ((clan.getCastleId() != 0) || (clan.getHideoutId() != 0))
		{
			player.sendPacket(SystemMessageId.A_CLAN_THAT_OWNS_A_CASTLE_CANNOT_PARTICIPATE_IN_ANOTHER_SIEGE);
		}
		else if (hall.getSiege().checkIsAttacker(clan))
		{
			player.sendPacket(SystemMessageId.YOU_HAVE_ALREADY_REQUESTED_A_CASTLE_SIEGE);
		}
		else if (isClanParticipating(clan))
		{
			player.sendPacket(SystemMessageId.YOUR_APPLICATION_HAS_BEEN_DENIED_BECAUSE_YOU_HAVE_ALREADY_SUBMITTED_A_REQUEST_FOR_ANOTHER_CASTLE_SIEGE);
		}
		else if (hall.getSiege().getAttackers().size() >= Config.CHS_MAX_ATTACKERS)
		{
			player.sendPacket(SystemMessageId.NO_MORE_REGISTRATIONS_MAY_BE_ACCEPTED_FOR_THE_ATTACKER_SIDE);
		}
		else
		{
			hall.addAttacker(clan);
		}
	}
	
	public final void unRegisterClan(L2Clan clan, SiegableHall hall)
	{
		if (!hall.isRegistering())
		{
			return;
		}
		hall.removeAttacker(clan);
	}
	
	public final boolean isClanParticipating(L2Clan clan)
	{
		for (SiegableHall hall : getConquerableHalls().values())
		{
			if ((hall.getSiege() != null) && hall.getSiege().checkIsAttacker(clan))
			{
				return true;
			}
		}
		return false;
	}
	
	public final void onServerShutDown()
	{
		for (SiegableHall hall : getConquerableHalls().values())
		{
			// Rainbow springs has his own attackers table
			if ((hall.getId() == 62) || (hall.getSiege() == null))
			{
				continue;
			}
			
			hall.getSiege().saveAttackers();
		}
	}
	
	public static CHSiegeManager getInstance()
	{
		return SingletonHolder._instance;
	}
	
	private static final class SingletonHolder
	{
		protected static final CHSiegeManager _instance = new CHSiegeManager();
	}
}
