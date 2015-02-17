/*
 * Copyright (C) 2004-2015 L2J DataPack
 * 
 * This file is part of L2J DataPack.
 * 
 * L2J DataPack is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * L2J DataPack is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package hellbound.AI.NPC.Quarry;

import org.l2junity.Config;
import org.l2junity.gameserver.ai.CtrlIntention;
import org.l2junity.gameserver.enums.ChatType;
import org.l2junity.gameserver.instancemanager.ZoneManager;
import org.l2junity.gameserver.model.actor.Attackable;
import org.l2junity.gameserver.model.actor.Creature;
import org.l2junity.gameserver.model.actor.Npc;
import org.l2junity.gameserver.model.actor.instance.PlayerInstance;
import org.l2junity.gameserver.model.actor.instance.L2QuestGuardInstance;
import org.l2junity.gameserver.model.holders.ItemChanceHolder;
import org.l2junity.gameserver.model.zone.ZoneType;
import org.l2junity.gameserver.network.NpcStringId;

import hellbound.HellboundEngine;
import ai.npc.AbstractNpcAI;

/**
 * Quarry AI.
 * @author DS, GKR
 */
public final class Quarry extends AbstractNpcAI
{
	// NPCs
	private static final int SLAVE = 32299;
	// Items
	protected static final ItemChanceHolder[] DROP_LIST =
	{
		new ItemChanceHolder(9628, 261), // Leonard
		new ItemChanceHolder(9630, 175), // Orichalcum
		new ItemChanceHolder(9629, 145), // Adamantine
		new ItemChanceHolder(1876, 6667), // Mithril ore
		new ItemChanceHolder(1877, 1333), // Adamantine nugget
		new ItemChanceHolder(1874, 2222), // Oriharukon ore
	};
	// Zone
	private static final int ZONE = 40107;
	// Misc
	private static final int TRUST = 50;
	
	public Quarry()
	{
		super(Quarry.class.getSimpleName(), "hellbound/AI/NPC");
		addSpawnId(SLAVE);
		addFirstTalkId(SLAVE);
		addStartNpc(SLAVE);
		addTalkId(SLAVE);
		addKillId(SLAVE);
		addEnterZoneId(ZONE);
	}
	
	@Override
	public final String onAdvEvent(String event, Npc npc, PlayerInstance player)
	{
		String htmltext = null;
		switch (event)
		{
			case "FollowMe":
			{
				npc.getAI().setIntention(CtrlIntention.AI_INTENTION_FOLLOW, player);
				npc.setTarget(player);
				npc.setAutoAttackable(true);
				npc.setRHandId(9136);
				npc.setWalking();
				
				if (getQuestTimer("TIME_LIMIT", npc, null) == null)
				{
					startQuestTimer("TIME_LIMIT", 900000, npc, null); // 15 min limit for save
				}
				htmltext = "32299-02.htm";
				break;
			}
			case "TIME_LIMIT":
			{
				for (ZoneType zone : ZoneManager.getInstance().getZones(npc))
				{
					if (zone.getId() == 40108)
					{
						npc.setTarget(null);
						npc.getAI().setIntention(CtrlIntention.AI_INTENTION_ACTIVE);
						npc.setAutoAttackable(false);
						npc.setRHandId(0);
						npc.teleToLocation(npc.getSpawn().getLocation());
						return null;
					}
				}
				broadcastNpcSay(npc, ChatType.NPC_GENERAL, NpcStringId.HUN_HUNGRY);
				npc.doDie(npc);
				break;
			}
			case "DECAY":
			{
				if ((npc != null) && !npc.isDead())
				{
					if (npc.getTarget().isPlayer())
					{
						for (ItemChanceHolder item : DROP_LIST)
						{
							if (getRandom(10000) < item.getChance())
							{
								npc.dropItem((PlayerInstance) npc.getTarget(), item.getId(), (int) (item.getCount() * Config.RATE_QUEST_DROP));
								break;
							}
						}
					}
					npc.setAutoAttackable(false);
					npc.deleteMe();
					npc.getSpawn().decreaseCount(npc);
					HellboundEngine.getInstance().updateTrust(TRUST, true);
				}
			}
		}
		
		return htmltext;
	}
	
	@Override
	public final String onSpawn(Npc npc)
	{
		npc.setAutoAttackable(false);
		if (npc instanceof L2QuestGuardInstance)
		{
			((L2QuestGuardInstance) npc).setPassive(true);
		}
		return super.onSpawn(npc);
	}
	
	@Override
	public final String onFirstTalk(Npc npc, PlayerInstance player)
	{
		if (HellboundEngine.getInstance().getLevel() != 5)
		{
			return "32299.htm";
		}
		return "32299-01.htm";
	}
	
	@Override
	public final String onKill(Npc npc, PlayerInstance killer, boolean isSummon)
	{
		npc.setAutoAttackable(false);
		return super.onKill(npc, killer, isSummon);
	}
	
	@Override
	public final String onEnterZone(Creature character, ZoneType zone)
	{
		if (character.isAttackable())
		{
			final Attackable npc = (Attackable) character;
			if (npc.getId() == SLAVE)
			{
				if (!npc.isDead() && !npc.isDecayed() && (npc.getAI().getIntention() == CtrlIntention.AI_INTENTION_FOLLOW))
				{
					if (HellboundEngine.getInstance().getLevel() == 5)
					{
						startQuestTimer("DECAY", 1000, npc, null);
						try
						{
							broadcastNpcSay(npc, ChatType.NPC_GENERAL, NpcStringId.THANK_YOU_FOR_THE_RESCUE_IT_S_A_SMALL_GIFT);
						}
						catch (Exception e)
						{
							//
						}
					}
				}
			}
		}
		return super.onEnterZone(character, zone);
	}
}
