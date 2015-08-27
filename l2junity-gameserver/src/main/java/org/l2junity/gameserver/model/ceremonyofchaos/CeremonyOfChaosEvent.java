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
import java.util.concurrent.TimeUnit;

import org.l2junity.gameserver.instancemanager.CeremonyOfChaosManager;
import org.l2junity.gameserver.instancemanager.InstanceManager;
import org.l2junity.gameserver.model.Party;
import org.l2junity.gameserver.model.Party.MessageType;
import org.l2junity.gameserver.model.StatsSet;
import org.l2junity.gameserver.model.actor.Npc;
import org.l2junity.gameserver.model.actor.Summon;
import org.l2junity.gameserver.model.actor.appearance.PcAppearance;
import org.l2junity.gameserver.model.actor.instance.L2MonsterInstance;
import org.l2junity.gameserver.model.actor.instance.PlayerInstance;
import org.l2junity.gameserver.model.entity.Instance;
import org.l2junity.gameserver.model.eventengine.AbstractEvent;
import org.l2junity.gameserver.model.events.EventType;
import org.l2junity.gameserver.model.events.ListenerRegisterType;
import org.l2junity.gameserver.model.events.annotations.RegisterEvent;
import org.l2junity.gameserver.model.events.annotations.RegisterType;
import org.l2junity.gameserver.model.events.impl.character.OnCreatureKill;
import org.l2junity.gameserver.model.holders.ItemHolder;
import org.l2junity.gameserver.model.holders.SkillHolder;
import org.l2junity.gameserver.model.skills.Skill;
import org.l2junity.gameserver.network.client.send.NpcHtmlMessage;
import org.l2junity.gameserver.network.client.send.SkillCoolTime;
import org.l2junity.gameserver.network.client.send.SystemMessage;
import org.l2junity.gameserver.network.client.send.appearance.ExCuriousHouseMemberUpdate;
import org.l2junity.gameserver.network.client.send.ceremonyofchaos.ExCuriousHouseEnter;
import org.l2junity.gameserver.network.client.send.ceremonyofchaos.ExCuriousHouseLeave;
import org.l2junity.gameserver.network.client.send.ceremonyofchaos.ExCuriousHouseMemberList;
import org.l2junity.gameserver.network.client.send.ceremonyofchaos.ExCuriousHouseRemainTime;
import org.l2junity.gameserver.network.client.send.string.SystemMessageId;

/**
 * @author UnAfraid
 */
public class CeremonyOfChaosEvent extends AbstractEvent<CeremonyOfChaosMember>
{
	private final int _id;
	private final Instance _instance;
	private final Set<L2MonsterInstance> _monsters = ConcurrentHashMap.newKeySet();
	private long _battleStartTime = 0;
	
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
			player.stopAllEffectsExceptThoseThatLastThroughDeath();
			
			// Player shouldn't be able to move and is hidden
			player.setIsImmobilized(true);
			player.setInvisible(true);
			
			// Same goes for summon
			player.getServitors().values().forEach(s ->
			{
				s.stopAllEffectsExceptThoseThatLastThroughDeath();
				s.setInvisible(true);
				s.setIsImmobilized(true);
			});
			
			if (player.isFlyingMounted())
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
			final Party party = player.getParty();
			if (party != null)
			{
				party.removePartyMember(player, MessageType.EXPELLED);
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
			
			// The character’s HP, MP, and CP are fully recovered.
			player.setCurrentHp(player.getMaxHp());
			player.setCurrentMp(player.getMaxMp());
			player.setCurrentCp(player.getMaxCp());
			
			// Skill reuse timers for all skills that have less than 15 minutes of cooldown time are reset.
			for (Skill skill : player.getAllSkills())
			{
				if (skill.getReuseDelay() <= 900000)
				{
					player.enableSkill(skill);
				}
			}
			
			player.sendSkillList();
			player.sendPacket(new SkillCoolTime(player));
			
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
			
			// Set player's instance id
			player.setInstanceId(_instance.getObjectId());
			
			// Teleport player to the arena
			player.teleToLocation(_instance.getSpawnLoc(), _instance.getObjectId(), 200);
		}
		
		final SystemMessage countdown = SystemMessage.getSystemMessage(SystemMessageId.THE_MATCH_WILL_START_IN_S1_SECOND_S);
		countdown.addByte(60);
		broadcastPacket(countdown);
		
		getTimers().addTimer("teleport_message1", 10000, null, null);
		getTimers().addTimer("teleport_message2", 14000, null, null);
		getTimers().addTimer("teleport_message3", 18000, null, null);
		
		getTimers().addTimer("countdown", StatsSet.valueOf("time", 30), 30000, null, null);
		getTimers().addTimer("countdown", StatsSet.valueOf("time", 20), 40000, null, null);
		getTimers().addTimer("countdown", StatsSet.valueOf("time", 10), 50000, null, null);
		
		for (int i = 5; i > 0; i--)
		{
			getTimers().addTimer("countdown", StatsSet.valueOf("time", i), (55 + (5 - i)) * 1000, null, null);
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
		_battleStartTime = System.currentTimeMillis();
		getTimers().addRepeatingTimer("update", 1000, null, null);
	}
	
	public void stopFight()
	{
		getMembers().values().stream().filter(p -> p.getLifeTime() == 0).forEach(p -> p.setLifeTime(System.currentTimeMillis() - _battleStartTime));
		
		for (CeremonyOfChaosMember member : getMembers().values())
		{
			final PlayerInstance player = member.getPlayer();
			if (player != null)
			{
				// Remove quit button
				player.sendPacket(ExCuriousHouseLeave.STATIC_PACKET);
				
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
		getTimers().cancelTimer("update", null, null);
		getMembers().clear();
		InstanceManager.getInstance().destroyInstance(_instance.getObjectId());
	}
	
	@Override
	public void onTimerEvent(String event, StatsSet params, Npc npc, PlayerInstance player)
	{
		switch (event)
		{
			case "update":
			{
				final int time = (int) CeremonyOfChaosManager.getInstance().getScheduler("stopFight").getRemainingTime(TimeUnit.SECONDS);
				broadcastPacket(new ExCuriousHouseRemainTime(time));
				getMembers().values().forEach(p -> broadcastPacket(new ExCuriousHouseMemberUpdate(p)));
				break;
			}
			case "teleport_message1":
			{
				broadcastPacket(SystemMessage.getSystemMessage(SystemMessageId.PROVE_YOUR_ABILITIES));
				break;
			}
			case "teleport_message2":
			{
				broadcastPacket(SystemMessage.getSystemMessage(SystemMessageId.THERE_ARE_NO_ALLIES_HERE_EVERYONE_IS_AN_ENEMY));
				break;
			}
			case "teleport_message3":
			{
				broadcastPacket(SystemMessage.getSystemMessage(SystemMessageId.IT_WILL_BE_A_LONELY_BATTLE_BUT_I_WISH_YOU_VICTORY));
				break;
			}
			case "countdown":
			{
				final SystemMessage countdown = SystemMessage.getSystemMessage(SystemMessageId.THE_MATCH_WILL_START_IN_S1_SECOND_S);
				countdown.addByte(params.getInt("time", 0));
				broadcastPacket(countdown);
				break;
			}
		}
	}
	
	@RegisterEvent(EventType.ON_CREATURE_KILL)
	@RegisterType(ListenerRegisterType.GLOBAL_PLAYERS)
	public void onPlayerDeath(OnCreatureKill event)
	{
		if (event.getAttacker().isPlayer() && event.getTarget().isPlayer())
		{
			final CeremonyOfChaosMember attackerMember = getMembers().get(event.getAttacker().getActingPlayer().getObjectId());
			final CeremonyOfChaosMember targetMember = getMembers().get(event.getTarget().getActingPlayer().getObjectId());
			
			if ((attackerMember != null) && (targetMember != null))
			{
				attackerMember.incrementScore();
				targetMember.setLifeTime(System.currentTimeMillis() - _battleStartTime);
			}
		}
	}
}
