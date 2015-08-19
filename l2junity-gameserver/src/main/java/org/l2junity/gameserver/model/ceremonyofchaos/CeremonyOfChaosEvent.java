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
import org.l2junity.gameserver.model.actor.Summon;
import org.l2junity.gameserver.model.actor.appearance.PcAppearance;
import org.l2junity.gameserver.model.actor.instance.L2MonsterInstance;
import org.l2junity.gameserver.model.actor.instance.PlayerInstance;
import org.l2junity.gameserver.model.eventengine.AbstractEvent;
import org.l2junity.gameserver.model.holders.ItemHolder;
import org.l2junity.gameserver.model.holders.SkillHolder;
import org.l2junity.gameserver.model.instancezone.Instance;
import org.l2junity.gameserver.network.client.send.NpcHtmlMessage;
import org.l2junity.gameserver.network.client.send.ceremonyofchaos.ExCuriousHouseEnter;
import org.l2junity.gameserver.network.client.send.ceremonyofchaos.ExCuriousHouseMemberList;
import org.l2junity.gameserver.network.client.send.string.SystemMessageId;

/**
 * @author UnAfraid
 */
public class CeremonyOfChaosEvent extends AbstractEvent<CeremonyOfChaosMember>
{
	private final int _id;
	private final Instance _instance;
	private final Set<CeremonyOfChaosMember> _players = ConcurrentHashMap.newKeySet();
	private final Set<L2MonsterInstance> _monsters = ConcurrentHashMap.newKeySet();
	
	public CeremonyOfChaosEvent(int id, int templateId)
	{
		_id = id;
		_instance = InstanceManager.getInstance().createInstance(templateId);
	}
	
	public int getId()
	{
		return _id;
	}
	
	public int getInstanceId()
	{
		return _instance.getId();
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
		final ExCuriousHouseMemberList membersList = new ExCuriousHouseMemberList(_id, CeremonyOfChaosManager.getInstance().getVariables().getInt(CeremonyOfChaosManager.MAX_PLAYERS_KEY, 18), getMembers().values());
		final NpcHtmlMessage msg = new NpcHtmlMessage(0);
		
		for (CeremonyOfChaosMember member : getMembers().values())
		{
			final PlayerInstance player = member.getPlayer();
			
			if (player.inObserverMode())
			{
				player.leaveObserverMode();
			}
			
			if (player.isInDuel())
			{
				player.setIsInDuel(0);
			}
			
			// Remember player's last location
			player.setLastLocation();
			
			// Hide player information
			final PcAppearance app = player.getAppearance();
			app.setVisibleName("Challenger" + member.getPosition());
			app.setVisibleTitle("");
			app.setVisibleClanData(0, 0, 0, 0, 0);
			
			// Register the event instance
			player.registerOnEvent(this);
			
			// Load the html
			msg.setFile(player.getHtmlPrefix(), "data/html/CeremonyOfChaos/started.htm");
			
			// Remove buffs
			player.stopAllEffects();
			
			// Player shouldn't be able to move and is hidden
			player.setIsImmobilized(true);
			player.setInvisible(true);
			
			// Same goes for summon
			player.getServitors().values().forEach(s ->
			{
				s.setInvisible(true);
				s.setIsImmobilized(true);
			});
			
			if (player.isTransformed())
			{
				player.untransform();
			}
			
			// If player is dead, revive it
			if (player.isDead())
			{
				player.doRevive();
			}
			
			// If player is sitting, stand up
			if (player.isSitting())
			{
				player.standUp();
			}
			
			// If player in party, leave it
			if (player.isInParty())
			{
				player.leaveParty();
			}
			
			// Cancel any started action
			player.abortAttack();
			player.abortCast();
			player.stopMove(null);
			player.setTarget(null);
			
			// Unsummon pet
			final Summon pet = player.getPet();
			if (pet != null)
			{
				pet.unSummon(player);
			}
			
			// Unsummon agathion
			if (player.getAgathionId() > 0)
			{
				player.setAgathionId(0);
			}
			
			// Fully regen player
			player.setCurrentHp(player.getMaxHp());
			player.setCurrentMp(player.getMaxMp());
			player.setCurrentCp(player.getMaxCp());
			
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
			
			// Teleport player to the arena
			player.teleToLocation(_instance.getEnterLocation(), _instance.getId(), 200);
		}
	}
	
	public void startFight()
	{
		for (CeremonyOfChaosMember member : getMembers().values())
		{
			final PlayerInstance player = member.getPlayer();
			if (player != null)
			{
				player.sendPacket(SystemMessageId.THE_MATCH_HAS_STARTED_FIGHT);
				player.setIsImmobilized(false);
				player.setInvisible(false);
				player.broadcastInfo();
				player.getServitors().values().forEach(s ->
				{
					s.setInvisible(false);
					s.setIsImmobilized(false);
					s.broadcastInfo();
				});
			}
		}
	}
	
	public void stopFight()
	{
		for (CeremonyOfChaosMember member : getMembers().values())
		{
			final PlayerInstance player = member.getPlayer();
			if (player != null)
			{
				// Teleport player back
				player.teleToLocation(player.getLastLocation(), 0, 0);
				
				// Restore player information
				final PcAppearance app = player.getAppearance();
				app.setVisibleName(null);
				app.setVisibleTitle(null);
				app.setVisibleClanData(-1, -1, -1, -1, -1);
				
				// Remove player from event
				player.removeFromEvent(this);
			}
		}
	}
}
