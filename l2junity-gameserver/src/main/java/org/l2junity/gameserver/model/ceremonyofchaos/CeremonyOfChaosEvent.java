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
package org.l2junity.gameserver.model.ceremonyofchaos;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.l2junity.gameserver.instancemanager.CeremonyOfChaosManager;
import org.l2junity.gameserver.instancemanager.InstanceManager;
import org.l2junity.gameserver.model.actor.instance.L2MonsterInstance;
import org.l2junity.gameserver.model.actor.instance.PlayerInstance;
import org.l2junity.gameserver.model.entity.Instance;
import org.l2junity.gameserver.model.eventengine.AbstractEvent;
import org.l2junity.gameserver.model.holders.ItemHolder;
import org.l2junity.gameserver.model.holders.SkillHolder;
import org.l2junity.gameserver.network.client.send.NpcHtmlMessage;
import org.l2junity.gameserver.network.client.send.ceremonyofchaos.ExCuriousHouseEnter;
import org.l2junity.gameserver.network.client.send.ceremonyofchaos.ExCuriousHouseMemberList;

/**
 * @author UnAfraid
 */
public class CeremonyOfChaosEvent extends AbstractEvent<CeremonyOfChaosMember>
{
	private final int _id;
	private final Instance _instance;
	private final Set<CeremonyOfChaosMember> _players = ConcurrentHashMap.newKeySet();
	private final Set<L2MonsterInstance> _monsters = ConcurrentHashMap.newKeySet();
	
	public CeremonyOfChaosEvent(int id, String instanceTemplate)
	{
		_id = id;
		_instance = InstanceManager.getInstance().getInstance(InstanceManager.getInstance().createDynamicInstance(instanceTemplate));
	}
	
	public int getId()
	{
		return _id;
	}
	
	public int getInstanceId()
	{
		return _instance.getObjectId();
	}
	
	public Instance getInstance()
	{
		return _instance;
	}
	
	public void addPlayer(CeremonyOfChaosMember player)
	{
		_players.add(player);
	}
	
	public Set<CeremonyOfChaosMember> getPlayers()
	{
		return _players;
	}
	
	public Set<L2MonsterInstance> getMonsters()
	{
		return _monsters;
	}
	
	public void preparePlayers()
	{
		final ExCuriousHouseMemberList membersList = new ExCuriousHouseMemberList(_id, CeremonyOfChaosManager.getInstance().getVariables().getInt(CeremonyOfChaosManager.MAX_PLAYERS_KEY, 18), getMembers());
		final NpcHtmlMessage msg = new NpcHtmlMessage(0);
		
		for (CeremonyOfChaosMember member : getMembers())
		{
			final PlayerInstance player = member.getPlayer();
			
			// Remember player's last location
			player.setLastLocation();
			
			player.registerOnEvent(this);
			
			// Load the html
			msg.setFile(player.getHtmlPrefix(), "data/html/CeremonyOfChaos/started.htm");
			
			// Teleport player to the arena
			player.teleToLocation(_instance.getSpawnLoc(), _instance.getId(), 200);
			
			// Apply the Energy of Chaos skill
			for (SkillHolder holder : CeremonyOfChaosManager.getInstance().getVariables().getList(CeremonyOfChaosManager.BUFF_KEY, SkillHolder.class))
			{
				holder.getSkill().activateSkill(player, player);
			}
			
			// Send Enter packet
			player.sendPacket(ExCuriousHouseEnter.STATIC_PACKET);
			
			// Send all members
			player.sendPacket(membersList);
			
			// Send the entrance html
			player.sendPacket(msg);
			
			// Send support items to player
			for (ItemHolder holder : CeremonyOfChaosManager.getInstance().getVariables().getList(CeremonyOfChaosManager.ITEMS_KEY, ItemHolder.class))
			{
				player.addItem("CoC", holder, null, true);
			}
		}
	}
	
	public void startFight()
	{
		
	}
	
	public void stopFight()
	{
		for (CeremonyOfChaosMember member : getMembers())
		{
			final PlayerInstance player = member.getPlayer();
			if (player != null)
			{
				// Teleport player back
				player.teleToLocation(player.getLastLocation(), 0, 0);
				
				// Remove player from event
				player.removeFromEvent(this);
			}
		}
	}
}
