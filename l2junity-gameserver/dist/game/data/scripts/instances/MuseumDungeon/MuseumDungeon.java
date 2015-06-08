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
package instances.MuseumDungeon;

import instances.AbstractInstance;

import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.l2junity.gameserver.enums.ChatType;
import org.l2junity.gameserver.instancemanager.InstanceManager;
import org.l2junity.gameserver.model.Location;
import org.l2junity.gameserver.model.actor.Npc;
import org.l2junity.gameserver.model.actor.instance.L2MonsterInstance;
import org.l2junity.gameserver.model.actor.instance.L2QuestGuardInstance;
import org.l2junity.gameserver.model.actor.instance.PlayerInstance;
import org.l2junity.gameserver.model.instancezone.InstanceWorld;
import org.l2junity.gameserver.model.quest.QuestState;
import org.l2junity.gameserver.model.skills.Skill;
import org.l2junity.gameserver.network.client.send.ExShowScreenMessage;
import org.l2junity.gameserver.network.client.send.string.NpcStringId;

import quests.Q10327_IntruderWhoWantsTheBookOfGiants.Q10327_IntruderWhoWantsTheBookOfGiants;

/**
 * Museum Dungeon Instance Zone.
 * @author Gladicek
 */
public final class MuseumDungeon extends AbstractInstance
{
	// NPC's
	private static final int PANTHEON = 32972;
	private static final int TOYRON = 33004;
	private static final int DESK = 33126;
	private static final int THIEF = 23121;
	// Items
	private static final int THE_WAR_OF_GODS_AND_GIANTS = 17575;
	// Locations
	private static final Location START_LOC = new Location(-114711, 243911, -7968);
	private static final Location TOYRON_SPAWN = new Location(-114707, 245428, -7968);
	// Misc
	private static final int TEMPLATE_ID = 182;
	private static final NpcStringId[] TOYRON_SHOUT =
	{
		NpcStringId.YOUR_NORMAL_ATTACKS_AREN_T_WORKING,
		NpcStringId.LOOKS_LIKE_ONLY_SKILL_BASED_ATTACKS_DAMAGE_THEM
	};
	private static final NpcStringId[] THIEF_SHOUT =
	{
		NpcStringId.YOU_LL_NEVER_LEAVE_WITH_THAT_BOOK,
		NpcStringId.FINALLY_I_THOUGHT_I_WAS_GOING_TO_DIE_WAITING
	};
	
	protected class MDWorld extends InstanceWorld
	{
		protected L2QuestGuardInstance toyron = null;
		protected L2MonsterInstance thief = null;
		protected Set<Npc> spawnedThiefs = ConcurrentHashMap.newKeySet();
		protected Npc bookDesk = null;
		protected int killedThiefs = 0;
	}
	
	public MuseumDungeon()
	{
		super(MuseumDungeon.class.getSimpleName());
		addStartNpc(PANTHEON);
		addTalkId(PANTHEON, TOYRON);
		addFirstTalkId(DESK);
		addAttackId(THIEF);
		addKillId(THIEF);
	}
	
	@Override
	public String onAdvEvent(String event, Npc npc, PlayerInstance player)
	{
		if (event.equals("enter_instance"))
		{
			enterInstance(player, new MDWorld(), "MuseumDungeon.xml", TEMPLATE_ID);
		}
		else
		{
			final InstanceWorld tmpworld = InstanceManager.getInstance().getPlayerWorld(player);
			
			if ((tmpworld != null) && (tmpworld instanceof MDWorld))
			{
				final MDWorld world = (MDWorld) tmpworld;
				switch (event)
				{
					case "TOYRON_FOLLOW":
					{
						world.toyron.getAI().startFollow(player);
						break;
					}
					case "TOYRON_SHOUT":
					{
						if (!world.toyron.canTarget(player))
						{
							cancelQuestTimer("TOYRON_SHOUT", world.toyron, player);
						}
						world.toyron.broadcastSay(ChatType.NPC_GENERAL, TOYRON_SHOUT[getRandom(2)]);
						break;
					}
					case "SPAWN_THIEFS_STAGE_1":
					{
						final List<Npc> thiefs = spawnGroup("thiefs", world.getInstanceId());
						world.spawnedThiefs.addAll(thiefs);
						for (Npc thief : world.spawnedThiefs)
						{
							thief.setIsRunning(true);
							addAttackPlayerDesire(thief, player);
							thief.broadcastSay(ChatType.NPC_GENERAL, THIEF_SHOUT[getRandom(2)]);
						}
						break;
					}
					case "SPAWN_THIEFS_STAGE_2":
					{
						final List<Npc> thiefs = spawnGroup("thiefs", world.getInstanceId());
						world.spawnedThiefs.addAll(thiefs);
						for (Npc thief : world.spawnedThiefs)
						{
							thief.setIsRunning(true);
						}
						break;
					}
					case "CHECK_FOLLOW":
					{
						if (world.toyron.canTarget(player))
						{
							startQuestTimer("TOYRON_FOLLOW", 500, world.toyron, player);
						}
						break;
					}
					case "KILL_THIEF":
					{
						npc.doDie(player);
						startQuestTimer("TOYRON_FOLLOW", 500, world.toyron, player);
						break;
					}
				}
			}
		}
		return super.onAdvEvent(event, npc, player);
	}
	
	@Override
	public String onKill(Npc npc, PlayerInstance player, boolean isSummon)
	{
		final QuestState qs = player.getQuestState(Q10327_IntruderWhoWantsTheBookOfGiants.class.getSimpleName());
		final InstanceWorld tmpworld = InstanceManager.getInstance().getWorld(npc.getInstanceId());
		final MDWorld world = (MDWorld) tmpworld;
		
		if ((qs != null) && qs.isCond(2))
		{
			if (world.killedThiefs >= 1)
			{
				qs.setCond(3, true);
				showOnScreenMsg(player, NpcStringId.TALK_TO_TOYRON_TO_RETURN_TO_THE_MUSEUM_LOBBY, ExShowScreenMessage.TOP_CENTER, 4500);
			}
			else
			{
				world.killedThiefs++;
			}
		}
		return super.onKill(npc, player, isSummon);
	}
	
	@Override
	public String onAttack(Npc npc, PlayerInstance attacker, int damage, boolean isSummon, Skill skill)
	{
		final InstanceWorld tmpworld = InstanceManager.getInstance().getWorld(npc.getInstanceId());
		final MDWorld world = (MDWorld) tmpworld;
		
		if (skill != null)
		{
			world.toyron.broadcastSay(ChatType.NPC_GENERAL, NpcStringId.ENOUGH_OF_THIS_COME_AT_ME);
			world.toyron.reduceCurrentHp(1, npc, null); // TODO: Find better way for attack
			npc.reduceCurrentHp(1, world.toyron, null);
			startQuestTimer("KILL_THIEF", 2500, npc, attacker);
		}
		else
		{
			showOnScreenMsg(attacker, NpcStringId.USE_YOUR_SKILL_ATTACKS_AGAINST_THEM, ExShowScreenMessage.TOP_CENTER, 4500);
		}
		return super.onAttack(npc, attacker, damage, isSummon, skill);
	}
	
	@Override
	public String onFirstTalk(Npc npc, PlayerInstance player)
	{
		final InstanceWorld tmpworld = InstanceManager.getInstance().getWorld(npc.getInstanceId());
		final QuestState qs = player.getQuestState(Q10327_IntruderWhoWantsTheBookOfGiants.class.getSimpleName());
		final MDWorld world = (MDWorld) tmpworld;
		String htmltext = null;
		
		if (qs == null)
		{
			htmltext = "33126.html";
		}
		else if (qs.isCond(1))
		{
			if (((npc == world.bookDesk) && !hasQuestItems(player, THE_WAR_OF_GODS_AND_GIANTS)))
			{
				qs.setCond(2);
				giveItems(player, THE_WAR_OF_GODS_AND_GIANTS, 1);
				showOnScreenMsg(player, NpcStringId.WATCH_OUT_YOU_ARE_BEING_ATTACKED, ExShowScreenMessage.TOP_CENTER, 4500);
				startQuestTimer("SPAWN_THIEFS_STAGE_1", 500, world.thief, player);
				startQuestTimer("TOYRON_FOLLOW", 500, world.toyron, player);
				htmltext = "33126-01.html";
			}
			else
			{
				htmltext = "33126-02.html";
			}
		}
		else if (qs.isCond(2))
		{
			htmltext = "33126.html";
		}
		return htmltext;
	}
	
	protected void spawnToyron(PlayerInstance player, MDWorld world)
	{
		if (world.toyron != null)
		{
			world.toyron.deleteMe();
		}
		world.toyron = (L2QuestGuardInstance) addSpawn(TOYRON, TOYRON_SPAWN, false, 0, true, world.getInstanceId());
		world.toyron.setIsRunning(true);
		world.toyron.setCanReturnToSpawnPoint(false);
	}
	
	protected void checkStage(PlayerInstance player, MDWorld world)
	{
		final QuestState qs = player.getQuestState(Q10327_IntruderWhoWantsTheBookOfGiants.class.getSimpleName());
		
		if (qs != null)
		{
			if (qs.isCond(1))
			{
				showOnScreenMsg(player, NpcStringId.AMONG_THE_4_BOOKSHELVES_FIND_THE_ONE_CONTAINING_A_VOLUME_CALLED_THE_WAR_OF_GODS_AND_GIANTS, ExShowScreenMessage.TOP_CENTER, 4500);
			}
			else if (qs.isCond(2))
			{
				if (world.spawnedThiefs.isEmpty())
				{
					startQuestTimer("SPAWN_THIEFS_STAGE_2", 500, world.thief, player);
					startQuestTimer("TOYRON_FOLLOW", 500, world.toyron, player);
				}
				else
				{
					startQuestTimer("CHECK_FOLLOW", 1000, world.toyron, player);
				}
			}
		}
	}
	
	protected void spawnDesks(PlayerInstance player, MDWorld world)
	{
		final List<Npc> desks = spawnGroup("desks", world.getInstanceId());
		world.bookDesk = desks.get(getRandom(desks.size()));
	}
	
	@Override
	public void onEnterInstance(PlayerInstance player, InstanceWorld world, boolean firstEntrance)
	{
		if (firstEntrance)
		{
			world.addAllowed(player.getObjectId());
			spawnToyron(player, (MDWorld) world);
			spawnDesks(player, (MDWorld) world);
		}
		teleportPlayer(player, START_LOC, world.getInstanceId());
		checkStage(player, (MDWorld) world);
	}
}