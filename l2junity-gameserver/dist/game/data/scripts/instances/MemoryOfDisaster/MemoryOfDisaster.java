/*
 * Copyright (C) 2004-2016 L2J Unity
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
package instances.MemoryOfDisaster;

import org.l2junity.commons.util.Rnd;
import org.l2junity.gameserver.ai.CtrlIntention;
import org.l2junity.gameserver.enums.CategoryType;
import org.l2junity.gameserver.enums.ChatType;
import org.l2junity.gameserver.enums.Movie;
import org.l2junity.gameserver.model.Location;
import org.l2junity.gameserver.model.StatsSet;
import org.l2junity.gameserver.model.actor.Creature;
import org.l2junity.gameserver.model.actor.Npc;
import org.l2junity.gameserver.model.actor.instance.PlayerInstance;
import org.l2junity.gameserver.model.base.ClassId;
import org.l2junity.gameserver.model.events.EventType;
import org.l2junity.gameserver.model.events.ListenerRegisterType;
import org.l2junity.gameserver.model.events.annotations.RegisterEvent;
import org.l2junity.gameserver.model.events.annotations.RegisterType;
import org.l2junity.gameserver.model.events.impl.character.OnCreatureAttacked;
import org.l2junity.gameserver.model.events.impl.character.OnCreatureKill;
import org.l2junity.gameserver.model.events.impl.character.OnCreatureSee;
import org.l2junity.gameserver.model.events.impl.character.player.OnPlayerCallToChangeClass;
import org.l2junity.gameserver.model.events.impl.character.player.OnPlayerLevelChanged;
import org.l2junity.gameserver.model.events.impl.character.player.OnPlayerLogin;
import org.l2junity.gameserver.model.holders.SkillHolder;
import org.l2junity.gameserver.model.instancezone.Instance;
import org.l2junity.gameserver.network.client.send.Earthquake;
import org.l2junity.gameserver.network.client.send.ExShowScreenMessage;
import org.l2junity.gameserver.network.client.send.OnEventTrigger;
import org.l2junity.gameserver.network.client.send.awakening.ExCallToChangeClass;
import org.l2junity.gameserver.network.client.send.string.NpcStringId;

import instances.AbstractInstance;

/**
 * Memory Of Disaster instance zone.
 * @author Sdw
 */
public class MemoryOfDisaster extends AbstractInstance
{
	// NPCs
	private static final int INVISIBLE_NPC = 18919;
	private static final int BRONK = 19192;
	private static final int ROGIN = 19193;
	private static final int TOROCCO = 19198;
	private static final int TENTACLE = 19171;
	private static final int TEREDOR = 19172;
	private static final int SOLDIER = 19196;
	private static final int SOLDIER2 = 19197;
	private static final int SIEGE_GOLEM = 19189;
	private static final int[] DWARVES =
	{
		19192,
		19193,
		19198,
		19199,
		19200,
		19201,
		19202,
		19203,
		19204,
		19205,
		19206,
		19207,
		19208,
		19209,
		19210,
		19211,
		19212,
		19213,
		19214,
		19215
	};
	// Locations
	private static final Location BATTLE_PORT = new Location(116063, -183167, -1460, 64960);
	private static final Location ROGIN_MOVE = new Location(116400, -183069, -1600);
	// Skills
	private static final SkillHolder SIEGE_GOLEM_SKILL_2 = new SkillHolder(16024, 1);
	// Misc
	private static final int FIRE_IN_DWARVEN_VILLAGE = 23120700;
	private static final int TEMPLATE_ID = 200;
	private static final NpcStringId[] SHOUT1 =
	{
		NpcStringId.BRONK,
		NpcStringId.CHIEF,
		NpcStringId.BRONK2,
		NpcStringId.NO_WAY3
	};
	
	public MemoryOfDisaster()
	{
		addInstanceCreatedId(TEMPLATE_ID);
		addSpawnId(INVISIBLE_NPC, TENTACLE, SOLDIER, SOLDIER2, TEREDOR, SIEGE_GOLEM);
		addMoveFinishedId(ROGIN);
		setCreatureKillId(this::onCreatureKill, BRONK);
		setCreatureAttackedId(this::onCreatureAttacked, BRONK, TENTACLE, SOLDIER, SOLDIER2, TEREDOR, SIEGE_GOLEM);
		setCreatureSeeId(this::onCreatureSee, TENTACLE, SOLDIER, SOLDIER2, TEREDOR);
	}
	
	@Override
	public String onSpawn(Npc npc)
	{
		final Instance instance = npc.getInstanceWorld();
		if (isMoDInstance(instance))
		{
			if (npc.getId() == INVISIBLE_NPC)
			{
				switch (npc.getParameters().getString("type", ""))
				{
					case "Fight":
					{
						addSpawn(npc, npc.getParameters().getInt("npcId"), npc.getLocation(), true, instance.getId());
						switch (Rnd.get(3))
						{
							case 0:
							{
								addSpawn(npc, SOLDIER, npc.getLocation(), true, instance.getId());
								break;
							}
							case 1:
							{
								addSpawn(npc, SOLDIER, npc.getLocation(), true, instance.getId());
								addSpawn(npc, SOLDIER2, npc.getLocation(), true, instance.getId());
								break;
							}
							case 2:
							{
								addSpawn(npc, SOLDIER, npc.getLocation(), true, instance.getId());
								addSpawn(npc, SOLDIER2, npc.getLocation(), true, instance.getId());
								addSpawn(npc, SOLDIER2, npc.getLocation(), true, instance.getId());
								break;
							}
						}
						break;
					}
					case "EventC":
					{
						addSpawn(npc, SIEGE_GOLEM, npc.getLocation(), true, instance.getId());
						break;
					}
				}
			}
			else if ((npc.getId() == TENTACLE) || (npc.getId() == TEREDOR) || (npc.getId() == SOLDIER) || (npc.getId() == SOLDIER2))
			{
				npc.initSeenCreatures();
			}
			else if (npc.getId() == SIEGE_GOLEM)
			{
				Npc teredor = addSpawn(TEREDOR, 117100, -181088, -1272, 19956, false, 0, false, npc.getInstanceId());
				addAttackDesire(teredor, npc);
				teredor.setScriptValue(1);
				teredor = addSpawn(TEREDOR, 116925, -180420, -1200, 46585, false, 0, false, npc.getInstanceId());
				addAttackDesire(teredor, npc);
				teredor.setScriptValue(1);
				teredor = addSpawn(TEREDOR, 116656, -180461, -1240, 56363, false, 0, false, npc.getInstanceId());
				addAttackDesire(teredor, npc);
				teredor.setScriptValue(1);
			}
		}
		return super.onSpawn(npc);
	}
	
	public void onCreatureSee(OnCreatureSee event)
	{
		final Creature creature = event.getSeen();
		final Npc npc = (Npc) event.getSeer();
		final Instance world = npc.getInstanceWorld();
		if (isMoDInstance(world) && creature.isNpc())
		{
			switch (npc.getId())
			{
				case SOLDIER:
				case SOLDIER2:
				{
					if ((creature.getId() == TENTACLE) || (creature.getId() == TEREDOR))
					{
						addAttackDesire(npc, creature);
					}
					break;
				}
				case TENTACLE:
				case TEREDOR:
				{
					if ((creature.getId() == SOLDIER) || (creature.getId() == SOLDIER2))
					{
						addAttackDesire(npc, creature);
					}
					break;
				}
			}
		}
	}
	
	@Override
	public void onTimerEvent(String event, StatsSet params, Npc npc, PlayerInstance player)
	{
		switch (event)
		{
			case "EARTHQUAKE":
			{
				player.sendPacket(new Earthquake(player.getLocation(), 50, 4));
				break;
			}
			case "END_OF_OPENING_SCENE":
			{
				player.teleToLocation(BATTLE_PORT);
				getTimers().addTimer("SPAWN_ROGIN", 10000, null, player);
				break;
			}
			case "SPAWN_ROGIN":
			{
				showOnScreenMsg(player, NpcStringId.WATCH_THE_DWARVEN_VILLAGE_LAST_STAND, ExShowScreenMessage.TOP_CENTER, 5000);
				player.getInstanceWorld().spawnGroup("ROGIN").forEach(n ->
				{
					n.getAI().setIntention(CtrlIntention.AI_INTENTION_MOVE_TO, ROGIN_MOVE);
					n.setIsRunning(true);
				});
				break;
			}
			case "ROGIN_TALK":
			{
				switch (npc.getVariables().getInt("talkId", 0))
				{
					case 0:
					{
						npc.broadcastSay(ChatType.NPC_GENERAL, NpcStringId.CHIEF_REPORTING_IN);
						npc.getVariables().set("talkId", 1);
						getTimers().addTimer("ROGIN_TALK", 2000, npc, null);
						break;
					}
					case 1:
					{
						npc.broadcastSay(ChatType.NPC_GENERAL, NpcStringId.ENEMIES_ARE_APPROACHING_FORM_THE_SOUTH);
						npc.getVariables().set("talkId", 2);
						getTimers().addTimer("ROGIN_TALK", 2000, npc, null);
						npc.getInstanceWorld().getNpc(TOROCCO).broadcastSay(ChatType.NPC_GENERAL, NpcStringId.ROGIN_I_M_HERE);
						npc.getInstanceWorld().getNpc(BRONK).broadcastSay(ChatType.NPC_GENERAL, NpcStringId.MM_I_SEE);
						break;
					}
					case 2:
					{
						npc.broadcastSay(ChatType.NPC_GENERAL, NpcStringId.THE_ELDERS_HAVEN_T_BEEN_MOVED_TO_SAFETY);
						npc.getVariables().set("talkId", 3);
						getTimers().addTimer("ROGIN_TALK", 2000, npc, null);
						break;
					}
					case 3:
					{
						npc.broadcastSay(ChatType.NPC_GENERAL, NpcStringId.MANY_RESIDENTS_STILL_HAVEN_T_LEFT_THEIR_HOMES);
						getTimers().addTimer("BRONK_TALK", 2000, npc.getInstanceWorld().getNpc(BRONK), null);
						break;
					}
				}
				break;
			}
			case "BRONK_TALK":
			{
				switch (npc.getVariables().getInt("talkId", 0))
				{
					case 0:
					{
						npc.broadcastSay(ChatType.NPC_GENERAL, NpcStringId.THANK_YOU_FOR_THE_REPORT_ROGIN);
						npc.getVariables().set("talkId", 1);
						getTimers().addTimer("BRONK_TALK", 2000, npc, null);
						// myself->ChangeDir(myself->sm, 0, 17036);
						break;
					}
					case 1:
					{
						npc.broadcastSay(ChatType.NPC_GENERAL, NpcStringId.SOLDIERS_WE_RE_FIGHTING_A_BATTLE_THAT_CAN_T_BE_WON);
						npc.getVariables().set("talkId", 2);
						getTimers().addTimer("BRONK_TALK", 2000, npc, null);
						// myself->ChangeDir(myself->sm, 0, 17036);
						break;
					}
					case 2:
					{
						npc.broadcastSay(ChatType.NPC_GENERAL, NpcStringId.BUT_WE_HAVE_TO_DEFEND_OUR_VILLAGE_SO_WE_RE_FIGHTING);
						npc.getVariables().set("talkId", 3);
						getTimers().addTimer("BRONK_TALK", 2000, npc, null);
						break;
					}
					case 3:
					{
						npc.broadcastSay(ChatType.NPC_GENERAL, NpcStringId.FOR_THE_FINE_WINES_AND_TREASURES_OF_ADEN);
						npc.getVariables().set("talkId", 4);
						getTimers().addTimer("BRONK_TALK", 2000, npc, null);
						break;
					}
					case 4:
					{
						npc.broadcastSay(ChatType.NPC_GENERAL, NpcStringId.I_M_PROUD_OF_EVERY_ONE_OF);
						npc.getVariables().set("talkId", 5);
						getTimers().addTimer("BRONK_TALK", 2000, npc, null);
						break;
					}
					case 5:
					{
						npc.getInstanceWorld().spawnGroup("TENTACLE").forEach(n -> addAttackDesire(n, npc));
						break;
					}
				}
				break;
			}
		}
	}
	
	@Override
	public void onInstanceCreated(Instance instance)
	{
		getTimers().addTimer("OPENING_SCENE", 500, e ->
		{
			instance.getPlayers().forEach(p ->
			{
				p.sendPacket(new OnEventTrigger(FIRE_IN_DWARVEN_VILLAGE, true));
				playMovie(p, Movie.SC_AWAKENING_OPENING);
				getTimers().addRepeatingTimer("EARTHQUAKE", 10000, null, p);
				getTimers().addTimer("END_OF_OPENING_SCENE", 32000, null, p);
			});
		});
	}
	
	@Override
	public void onMoveFinished(Npc npc)
	{
		if ((npc.getId() == ROGIN) && ((npc.getX() == ROGIN_MOVE.getX()) && (npc.getY() == ROGIN_MOVE.getY())))
		{
			getTimers().addTimer("ROGIN_TALK", 3000, npc, null);
		}
	}
	
	private void onCreatureAttacked(OnCreatureAttacked event)
	{
		final Instance world = event.getTarget().getInstanceWorld();
		if (isMoDInstance(world))
		{
			if (!event.getAttacker().isPlayable())
			{
				final Npc npc = (Npc) event.getTarget();
				final Npc attacker = (Npc) event.getAttacker();
				switch (npc.getId())
				{
					case BRONK:
					{
						npc.doDie(attacker);
						// hEre SCE_AWAKENING_BU_START
						break;
					}
					case SOLDIER:
					case SOLDIER2:
					{
						final int attackCount = npc.getVariables().getInt("attackCount", 0) + 1;
						if (attackCount == 10)
						{
							npc.doDie(attacker);
							addSpawn((Npc) npc.getSummoner(), SOLDIER, npc.getLocation(), true, world.getId());
						}
						else
						{
							npc.getVariables().set("attackCount", attackCount);
						}
						break;
					}
					case TENTACLE:
					case TEREDOR:
					{
						if (!npc.isScriptValue(1))
						{
							final int attackCount = npc.getVariables().getInt("attackCount", 0) + 1;
							if (attackCount == 20)
							{
								npc.doDie(attacker);
								addSpawn((Npc) npc.getSummoner(), npc.getId(), npc.getLocation(), true, world.getId());
							}
							else
							{
								npc.getVariables().set("attackCount", attackCount);
								addAttackDesire(npc, attacker);
							}
						}
						break;
					}
					case SIEGE_GOLEM:
					{
						if (npc.isScriptValue(0))
						{
							addSkillCastDesire(npc, attacker, SIEGE_GOLEM_SKILL_2, 1000000);
						}
						break;
					}
				}
			}
		}
	}
	
	private void onCreatureKill(OnCreatureKill event)
	{
		final Npc bronk = ((Npc) event.getTarget());
		for (Npc dwarf : bronk.getInstanceWorld().getNpcs(DWARVES))
		{
			if (dwarf.getId() == ROGIN)
			{
				dwarf.broadcastSay(ChatType.NPC_GENERAL, NpcStringId.CHIEF2);
			}
			else
			{
				dwarf.broadcastSay(ChatType.NPC_GENERAL, SHOUT1[Rnd.get(SHOUT1.length)]);
			}
		}
		bronk.broadcastSay(ChatType.NPC_GENERAL, NpcStringId.UGH_IF_I_SEE_YOU_IN_THE_SPIRIT_WORLD_FIRST_ROUND_IS_ON_ME);
	}
	
	@RegisterEvent(EventType.ON_PLAYER_CALL_TO_CHANGE_CLASS)
	@RegisterType(ListenerRegisterType.GLOBAL_PLAYERS)
	public void onPlayerCallToChangeClass(OnPlayerCallToChangeClass event)
	{
		enterInstance(event.getActiveChar(), null, TEMPLATE_ID);
	}
	
	@RegisterEvent(EventType.ON_PLAYER_LOGIN)
	@RegisterType(ListenerRegisterType.GLOBAL_PLAYERS)
	public void OnPlayerLogin(OnPlayerLogin event)
	{
		final PlayerInstance player = event.getActiveChar();
		if ((player.getLevel() > 84) && player.isInCategory(CategoryType.FOURTH_CLASS_GROUP) && !player.isSubClassActive() && (player.getClassId() != ClassId.JUDICATOR))
		{
			for (ClassId newClass : player.getClassId().getNextClassIds())
			{
				player.sendPacket(new ExCallToChangeClass(newClass.getId(), false));
				showOnScreenMsg(player, NpcStringId.FREE_THE_GIANT_FROM_HIS_IMPRISONMENT_AND_AWAKEN_YOUR_TRUE_POWER, ExShowScreenMessage.TOP_CENTER, 5000);
			}
		}
	}
	
	@RegisterEvent(EventType.ON_PLAYER_LEVEL_CHANGED)
	@RegisterType(ListenerRegisterType.GLOBAL_PLAYERS)
	public void OnPlayerLevelChanged(OnPlayerLevelChanged event)
	{
		final PlayerInstance player = event.getActiveChar();
		if ((player.getLevel() > 84) && player.isInCategory(CategoryType.FOURTH_CLASS_GROUP) && !player.isSubClassActive() && (player.getClassId() != ClassId.JUDICATOR))
		{
			for (ClassId newClass : player.getClassId().getNextClassIds())
			{
				player.sendPacket(new ExCallToChangeClass(newClass.getId(), false));
				showOnScreenMsg(player, NpcStringId.FREE_THE_GIANT_FROM_HIS_IMPRISONMENT_AND_AWAKEN_YOUR_TRUE_POWER, ExShowScreenMessage.TOP_CENTER, 5000);
			}
		}
	}
	
	private boolean isMoDInstance(Instance instance)
	{
		return (instance != null) && (instance.getTemplateId() == TEMPLATE_ID);
	}
	
	public static void main(String[] args)
	{
		new MemoryOfDisaster();
	}
}
