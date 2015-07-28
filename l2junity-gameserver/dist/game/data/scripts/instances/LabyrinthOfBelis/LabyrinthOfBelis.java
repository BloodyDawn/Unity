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
package instances.LabyrinthOfBelis;

import instances.AbstractInstance;

import java.util.concurrent.CopyOnWriteArrayList;

import org.l2junity.gameserver.ai.CtrlIntention;
import org.l2junity.gameserver.enums.ChatType;
import org.l2junity.gameserver.enums.Movie;
import org.l2junity.gameserver.instancemanager.InstanceManager;
import org.l2junity.gameserver.model.Location;
import org.l2junity.gameserver.model.actor.Attackable;
import org.l2junity.gameserver.model.actor.Creature;
import org.l2junity.gameserver.model.actor.Npc;
import org.l2junity.gameserver.model.actor.instance.L2QuestGuardInstance;
import org.l2junity.gameserver.model.actor.instance.PlayerInstance;
import org.l2junity.gameserver.model.events.EventType;
import org.l2junity.gameserver.model.events.ListenerRegisterType;
import org.l2junity.gameserver.model.events.annotations.Id;
import org.l2junity.gameserver.model.events.annotations.RegisterEvent;
import org.l2junity.gameserver.model.events.annotations.RegisterType;
import org.l2junity.gameserver.model.events.impl.character.OnCreatureKill;
import org.l2junity.gameserver.model.holders.SkillHolder;
import org.l2junity.gameserver.model.instancezone.InstanceWorld;
import org.l2junity.gameserver.model.quest.QuestState;
import org.l2junity.gameserver.model.zone.ZoneType;
import org.l2junity.gameserver.network.client.send.ExShowScreenMessage;
import org.l2junity.gameserver.network.client.send.string.NpcStringId;

import quests.Q10331_StartOfFate.Q10331_StartOfFate;

/**
 * Labyrinth of Belis Instance Zone.
 * @author Gladicek
 */
public final class LabyrinthOfBelis extends AbstractInstance
{
	// NPC's
	private static final int SEBION = 32972;
	private static final int INFILTRATION_OFFICER = 19155;
	private static final int BELIS_VERITIFICATION_SYSTEM = 33215;
	private static final int OPERATIVE = 22998;
	private static final int HANDYMAN = 22997;
	private static final int ELECTRICITY_GENERATOR = 33216;
	private static final int NEMERTESS = 22984;
	// Items
	private static final int SARIL_NECKLACE = 17580;
	private static final int BELIS_MARK = 17615;
	// Skills
	private static final SkillHolder CURRENT_SHOCK = new SkillHolder(14698, 1);
	// Locations
	private static final Location START_LOC = new Location(-119941, 211148, -8599);
	private static final Location EXIT_LOC = new Location(-111782, 231892, -3178);
	private static final Location INFILTRATION_OFFICER_ROOM_1 = new Location(-119045, 211171, -8592);
	private static final Location INFILTRATION_OFFICER_ROOM_2 = new Location(-117040, 212502, -8592);
	private static final Location INFILTRATION_OFFICER_ROOM_3 = new Location(-117843, 214230, -8592);
	private static final Location INFILTRATION_OFFICER_ROOM_4 = new Location(-119217, 213743, -8600);
	private static final Location SPAWN_ATTACKERS = new Location(-116809, 213275, -8606);
	private static final Location GENERATOR_SPAWN = new Location(-118333, 214791, -8557);
	private static final Location ATTACKER_SPOT = new Location(-117927, 214391, -8600);
	private static final Location NEMERTESS_SPAWN = new Location(-118336, 212973, -8680);
	// Misc
	private static final int TEMPLATE_ID = 178;
	private static final int DOOR_ID_ROOM_1_1 = 16240001;
	private static final int DOOR_ID_ROOM_1_2 = 16240002;
	private static final int DOOR_ID_ROOM_2_1 = 16240003;
	private static final int DOOR_ID_ROOM_2_2 = 16240004;
	private static final int DOOR_ID_ROOM_3_1 = 16240005;
	private static final int DOOR_ID_ROOM_3_2 = 16240006;
	private static final int DOOR_ID_ROOM_4_1 = 16240007;
	private static final int DOOR_ID_ROOM_4_2 = 16240008;
	private static final int DAMAGE_ZONE = 12014;
	
	protected class LoBWorld extends InstanceWorld
	{
		protected L2QuestGuardInstance infiltration_officer = null;
		protected Npc electricity_generator = null;
		protected Attackable attacker = null;
		protected CopyOnWriteArrayList<Npc> spawnedNpc = new CopyOnWriteArrayList<>();
		protected boolean fighting = false;
		protected boolean instance_started = false;
		protected boolean device_activated = false;
		protected boolean nextStageBlock = false;
		public int counter = 0;
	}
	
	public LabyrinthOfBelis()
	{
		super(LabyrinthOfBelis.class.getSimpleName());
		addStartNpc(SEBION, INFILTRATION_OFFICER, BELIS_VERITIFICATION_SYSTEM);
		addTalkId(SEBION, INFILTRATION_OFFICER, BELIS_VERITIFICATION_SYSTEM);
		addKillId(OPERATIVE, HANDYMAN, INFILTRATION_OFFICER, NEMERTESS);
		addAttackId(INFILTRATION_OFFICER);
		addMoveFinishedId(INFILTRATION_OFFICER);
		addFirstTalkId(INFILTRATION_OFFICER, ELECTRICITY_GENERATOR, BELIS_VERITIFICATION_SYSTEM);
		addEnterZoneId(DAMAGE_ZONE);
		addExitZoneId(DAMAGE_ZONE);
	}
	
	@Override
	public String onAdvEvent(String event, Npc npc, PlayerInstance player)
	{
		if (event.equals("enter_instance"))
		{
			enterInstance(player, new LoBWorld(), "LabyrinthOfBelis.xml", TEMPLATE_ID);
		}
		else
		{
			final InstanceWorld tmpworld = InstanceManager.getInstance().getWorld(npc.getInstanceId());
			if ((tmpworld != null) && (tmpworld instanceof LoBWorld))
			{
				final LoBWorld world = (LoBWorld) tmpworld;
				switch (event)
				{
					case "SPAM_MESSAGE_1":
					{
						if (world.infiltration_officer != null)
						{
							if (!world.nextStageBlock)
							{
								world.infiltration_officer.broadcastSay(ChatType.NPC_GENERAL, NpcStringId.LET_ME_KNOW_WHEN_YOU_RE_ALL_READY);
								break;
							}
							cancelQuestTimer("SPAM_MESSAGE_1", world.infiltration_officer, player);
							break;
						}
						cancelQuestTimer("SPAM_MESSAGE_1", world.infiltration_officer, player);
						break;
					}
					case "ROOM_1":
					{
						world.setStatus(2);
						world.fighting = true;
						world.spawnedNpc.addAll(spawnGroup("operatives", world.getInstanceId()));
						openDoor(DOOR_ID_ROOM_1_2, world.getInstanceId());
						world.infiltration_officer.getAI().startFollow(player);
						cancelQuestTimer("SPAM_MESSAGE_1", world.infiltration_officer, player);
						break;
					}
					case "ROOM_1_DONE":
					{
						world.setStatus(3);
						openDoor(DOOR_ID_ROOM_2_1, world.getInstanceId());
						world.infiltration_officer.getAI().setIntention(CtrlIntention.AI_INTENTION_MOVE_TO, INFILTRATION_OFFICER_ROOM_2);
						world.infiltration_officer.broadcastSay(ChatType.NPC_GENERAL, NpcStringId.ALL_RIGHT_LET_S_MOVE_OUT);
						break;
					}
					case "ROOM_2":
					{
						world.setStatus(4);
						world.fighting = true;
						openDoor(DOOR_ID_ROOM_2_2, world.getInstanceId());
						world.infiltration_officer.getAI().startFollow(player);
						showOnScreenMsg(player, NpcStringId.MARK_OF_BELIS_CAN_BE_ACQUIRED_FROM_ENEMIES_NUSE_THEM_IN_THE_BELIS_VERIFICATION_SYSTEM, ExShowScreenMessage.TOP_CENTER, 4500);
						startQuestTimer("SPAM_MESSAGE_2", 10000, world.infiltration_officer, player, true);
						break;
					}
					case "ROOM_2_DONE":
					{
						world.setStatus(5);
						openDoor(DOOR_ID_ROOM_3_1, world.getInstanceId());
						world.infiltration_officer.getAI().setIntention(CtrlIntention.AI_INTENTION_MOVE_TO, INFILTRATION_OFFICER_ROOM_3);
						world.infiltration_officer.broadcastSay(ChatType.NPC_GENERAL, NpcStringId.COME_ON_ONTO_THE_NEXT_PLACE);
						cancelQuestTimer("SPAM_MESSAGE_2", world.infiltration_officer, player);
						break;
					}
					case "ROOM_3":
					{
						world.setStatus(6);
						world.fighting = true;
						world.electricity_generator = addSpawn(ELECTRICITY_GENERATOR, GENERATOR_SPAWN, false, 0, true, world.getInstanceId());
						openDoor(DOOR_ID_ROOM_3_2, world.getInstanceId());
						world.infiltration_officer.broadcastSay(ChatType.NPC_GENERAL, NpcStringId.DON_T_COME_BACK_HERE);
						world.infiltration_officer.setTarget(world.electricity_generator);
						world.infiltration_officer.addDamageHate(world.electricity_generator, 0, 9999);
						world.infiltration_officer.reduceCurrentHp(1, world.electricity_generator, null); // TODO: Find better way for attack
						world.electricity_generator.reduceCurrentHp(1, world.infiltration_officer, null);
						startQuestTimer("SPAM_MESSAGE_3", 7000, world.infiltration_officer, player, true);
						startQuestTimer("SPAWN_ATTACKERS", 12500, npc, player, true);
						startQuestTimer("NPC_EFFECT", 500, world.electricity_generator, player);
						break;
					}
					case "NPC_EFFECT":
					{
						world.electricity_generator.setState(1);
						break;
					}
					case "ROOM_3_DONE":
					{
						if (!world.nextStageBlock)
						{
							world.setStatus(7);
							world.electricity_generator.deleteMe();
							showOnScreenMsg(player, NpcStringId.ELECTRONIC_DEVICE_HAS_BEEN_DESTROYED, ExShowScreenMessage.TOP_CENTER, 4500);
							openDoor(DOOR_ID_ROOM_4_1, world.getInstanceId());
							world.infiltration_officer.getAI().setIntention(CtrlIntention.AI_INTENTION_MOVE_TO, INFILTRATION_OFFICER_ROOM_4);
							world.infiltration_officer.broadcastSay(ChatType.NPC_GENERAL, NpcStringId.DEVICE_DESTROYED_LET_S_GO_ONTO_THE_NEXT);
							break;
						}
						break;
					}
					case "ROOM_4":
					{
						world.setStatus(8);
						world.fighting = true;
						openDoor(DOOR_ID_ROOM_4_2, world.getInstanceId());
						playMovie(player, Movie.SC_TALKING_ISLAND_BOSS_OPENING);
						startQuestTimer("SPAWN_NEMERTESS", 50000, npc, player);
						break;
					}
					case "SPAM_MESSAGE_2":
					{
						if (world.infiltration_officer != null)
						{
							if (!world.nextStageBlock)
							{
								showOnScreenMsg(player, NpcStringId.MARK_OF_BELIS_CAN_BE_ACQUIRED_FROM_ENEMIES_NUSE_THEM_IN_THE_BELIS_VERIFICATION_SYSTEM, ExShowScreenMessage.TOP_CENTER, 4500);
								break;
							}
							cancelQuestTimer("SPAM_MESSAGE_2", world.infiltration_officer, player);
							break;
						}
						cancelQuestTimer("SPAM_MESSAGE_2", world.infiltration_officer, player);
						break;
					}
					case "SPAM_MESSAGE_3":
					{
						if (world.infiltration_officer != null)
						{
							if (!world.nextStageBlock)
							{
								world.infiltration_officer.broadcastSay(ChatType.NPC_GENERAL, NpcStringId.DON_T_COME_BACK_HERE);
								break;
							}
							cancelQuestTimer("SPAM_MESSAGE_3", world.infiltration_officer, player);
							break;
						}
						cancelQuestTimer("SPAM_MESSAGE_3", world.infiltration_officer, player);
						break;
					}
					case "GIVE_BELIS_MARK":
					{
						if (!world.device_activated)
						{
							if (hasAtLeastOneQuestItem(player, BELIS_MARK))
							{
								takeItems(player, BELIS_MARK, 1);
								
								switch (npc.getScriptValue())
								{
									case 0:
									{
										npc.setScriptValue(1);
										return "33215-01.html";
									}
									case 1:
									{
										npc.setScriptValue(2);
										return "33215-02.html";
									}
									case 2:
									{
										world.device_activated = true;
										startQuestTimer("ROOM_2_DONE", 500, world.infiltration_officer, player);
										cancelQuestTimer("SPAM_MESSAGE_2", world.infiltration_officer, player);
										return "33215-03.html";
									}
								}
							}
							return "33215-04.html";
						}
						return "33215-05.html";
					}
					case "RESPAWN_HANDYMAN":
					{
						addSpawn(HANDYMAN, npc, false, 0, true, world.getInstanceId());
						break;
					}
					case "SPAWN_ATTACKERS":
					{
						if (!world.nextStageBlock)
						{
							if (world.counter == 6)
							{
								cancelQuestTimer("SPAWN_ATTACKERS", npc, player);
							}
							else
							{
								showOnScreenMsg(player, (getRandomBoolean() ? NpcStringId.IF_TERAIN_DIES_THE_MISSION_WILL_FAIL : NpcStringId.BEHIND_YOU_THE_ENEMY_IS_AMBUSHING_YOU), ExShowScreenMessage.TOP_CENTER, 4500);
								world.spawnedNpc.add(world.attacker = (Attackable) addSpawn((getRandomBoolean() ? OPERATIVE : HANDYMAN), SPAWN_ATTACKERS, false, 0, true, world.getInstanceId()));
								world.attacker.setIsRunning(true);
								world.attacker.getAI().setIntention(CtrlIntention.AI_INTENTION_MOVE_TO, ATTACKER_SPOT);
								world.attacker.broadcastSay(ChatType.NPC_GENERAL, (getRandomBoolean() ? NpcStringId.KILL_THE_GUY_MESSING_WITH_THE_ELECTRIC_DEVICE : NpcStringId.FOCUS_ON_ATTACKING_THE_GUY_IN_THE_ROOM));
								world.attacker.addDamageHate(world.infiltration_officer, 0, 9999);
								world.attacker.reduceCurrentHp(1, world.infiltration_officer, null); // TODO: Find better way for attack
								world.counter++;
								break;
							}
						}
						else
						{
							cancelQuestTimer("SPAWN_ATTACKERS", npc, player);
							break;
						}
						break;
					}
					case "SPAWN_NEMERTESS":
					{
						addSpawn(NEMERTESS, NEMERTESS_SPAWN, false, 0, false, world.getInstanceId());
						break;
					}
					case "ROOM_4_DONE":
					{
						world.setStatus(9);
						world.infiltration_officer.getAI().setIntention(CtrlIntention.AI_INTENTION_MOVE_TO, NEMERTESS_SPAWN);
						break;
					}
					case "FINISH_INSTANCE":
					{
						finishInstance(world);
						teleportPlayer(player, EXIT_LOC, 0);
						break;
					}
					case "DEBUFF":
					{
						CURRENT_SHOCK.getSkill().applyEffects(world.electricity_generator, player);
						break;
					}
				}
			}
		}
		return super.onAdvEvent(event, npc, player);
	}
	
	@Override
	public String onEnterZone(Creature character, ZoneType zone)
	{
		final InstanceWorld tmpworld = InstanceManager.getInstance().getPlayerWorld(character.getActingPlayer());
		if ((tmpworld != null) && (tmpworld instanceof LoBWorld))
		{
			final LoBWorld world = (LoBWorld) tmpworld;
			if (character.isPlayer() && world.isStatus(6))
			{
				startQuestTimer("DEBUFF", 1500, world.electricity_generator, character.getActingPlayer(), true);
			}
		}
		return super.onEnterZone(character, zone);
	}
	
	@Override
	public String onExitZone(Creature character, ZoneType zone)
	{
		final InstanceWorld tmpworld = InstanceManager.getInstance().getPlayerWorld(character.getActingPlayer());
		if ((tmpworld != null) && (tmpworld instanceof LoBWorld))
		{
			final LoBWorld world = (LoBWorld) tmpworld;
			if ((character.isPlayer() && world.isStatus(6)) || world.isStatus(7))
			{
				cancelQuestTimer("DEBUFF", world.electricity_generator, character.getActingPlayer());
			}
		}
		return super.onExitZone(character, zone);
	}
	
	@Override
	public void onMoveFinished(Npc npc)
	{
		final InstanceWorld tmpworld = InstanceManager.getInstance().getWorld(npc.getInstanceId());
		final LoBWorld world = (LoBWorld) tmpworld;
		
		if (world != null)
		{
			switch (world.getStatus())
			{
				case 3:
				{
					world.infiltration_officer.broadcastInfo();
					world.fighting = false;
					world.infiltration_officer.broadcastSay(ChatType.NPC_GENERAL, NpcStringId.HEY_YOU_RE_NOT_ALL_BAD_LET_ME_KNOW_WHEN_YOU_RE_READY);
					world.infiltration_officer.setHeading(world.infiltration_officer.getHeading() + 32500);
					break;
				}
				case 5:
				{
					world.infiltration_officer.broadcastInfo();
					world.fighting = false;
					world.infiltration_officer.broadcastSay(ChatType.NPC_GENERAL, NpcStringId.READY_LET_ME_KNOW);
					world.infiltration_officer.setHeading(world.infiltration_officer.getHeading() + 32500);
					break;
				}
				case 7:
				{
					world.infiltration_officer.broadcastInfo();
					world.fighting = false;
					world.infiltration_officer.broadcastSay(ChatType.NPC_GENERAL, NpcStringId.SOMETHING_OMINOUS_IN_THERE_I_HOPE_YOU_RE_REALLY_READY_FOR_THIS_LET_ME_KNOW);
					world.infiltration_officer.setHeading(world.infiltration_officer.getHeading() + 32500);
					break;
				}
				case 9:
				{
					world.fighting = false;
					world.infiltration_officer.setHeading(world.infiltration_officer.getHeading() + 32500);
					break;
				}
			}
		}
	}
	
	@Override
	public String onFirstTalk(Npc npc, PlayerInstance player)
	{
		final InstanceWorld tmpworld = InstanceManager.getInstance().getWorld(npc.getInstanceId());
		final LoBWorld world = (LoBWorld) tmpworld;
		String htmltext = null;
		
		switch (npc.getId())
		{
			case INFILTRATION_OFFICER:
			{
				if (!world.fighting)
				{
					switch (world.getStatus())
					{
						case 1:
						{
							htmltext = "19155-01.html";
							break;
						}
						case 3:
						{
							htmltext = "19155-03.html";
							break;
						}
						case 5:
						{
							htmltext = "19155-04.html";
							break;
						}
						case 7:
						{
							htmltext = "19155-05.html";
							break;
						}
						case 9:
						{
							htmltext = "19155-06.html";
							break;
						}
					}
				}
				else
				{
					htmltext = "19155-02.html";
					break;
				}
				break;
			}
			case BELIS_VERITIFICATION_SYSTEM:
			{
				htmltext = "33215.html";
				break;
			}
			case ELECTRICITY_GENERATOR:
			{
				htmltext = "33216.html";
				break;
			}
		}
		return htmltext;
	}
	
	@Override
	public String onKill(Npc npc, PlayerInstance player, boolean isSummon)
	{
		final InstanceWorld tmpworld = InstanceManager.getInstance().getWorld(npc.getInstanceId());
		final LoBWorld world = (LoBWorld) tmpworld;
		
		switch (npc.getId())
		{
			case OPERATIVE:
			{
				if (world.isStatus(2))
				{
					world.spawnedNpc.remove(npc);
					if (world.spawnedNpc.isEmpty())
					{
						startQuestTimer("ROOM_1_DONE", 500, npc, player);
						world.setStatus(2);
						break;
					}
					break;
				}
				else if (world.isStatus(6))
				{
					world.spawnedNpc.remove(npc);
					if (world.spawnedNpc.isEmpty() && (world.counter == 6))
					{
						cancelQuestTimer("SPAM_MESSAGE_3", world.infiltration_officer, player);
						startQuestTimer("ROOM_3_DONE", 2000, world.infiltration_officer, player);
						break;
					}
					break;
				}
				break;
			}
			case HANDYMAN:
			{
				if (world.isStatus(4))
				{
					if (getRandom(100) > 60)
					{
						npc.dropItem(player, BELIS_MARK, 1);
					}
					startQuestTimer("RESPAWN_HANDYMAN", 15000, npc, player);
					break;
				}
				else if (world.isStatus(6))
				{
					world.spawnedNpc.remove(npc);
					if (world.spawnedNpc.isEmpty() && (world.counter == 6))
					{
						cancelQuestTimer("SPAM_MESSAGE_3", world.infiltration_officer, player);
						startQuestTimer("ROOM_3_DONE", 2000, world.infiltration_officer, player);
					}
				}
				break;
			}
			case INFILTRATION_OFFICER:
			{
				world.nextStageBlock = true;
				finishInstance(world, 60);
				break;
			}
			case NEMERTESS:
			{
				final QuestState qs = player.getQuestState(Q10331_StartOfFate.class.getSimpleName());
				
				if (qs.isCond(3))
				{
					qs.setCond(4, true);
					giveItems(player, SARIL_NECKLACE, 1);
				}
				npc.deleteMe();
				playMovie(player, Movie.SC_TALKING_ISLAND_BOSS_ENDING);
				startQuestTimer("ROOM_4_DONE", 30000, npc, player);
				break;
			}
		}
		return super.onKill(npc, player, isSummon);
	}
	
	protected void spawnInfiltrationOfficer(PlayerInstance player, LoBWorld world)
	{
		if (world.infiltration_officer != null)
		{
			world.infiltration_officer.deleteMe();
		}
		world.infiltration_officer = (L2QuestGuardInstance) addSpawn(INFILTRATION_OFFICER, INFILTRATION_OFFICER_ROOM_1, false, 0, true, world.getInstanceId());
		world.infiltration_officer.setIsRunning(true);
		world.infiltration_officer.setCanReturnToSpawnPoint(false);
		world.infiltration_officer.setHeading(world.infiltration_officer.getHeading() + 32500);
		if (!world.instance_started)
		{
			startQuestTimer("SPAM_MESSAGE_1", 6000, world.infiltration_officer, player, true);
			world.instance_started = true;
		}
	}
	
	@RegisterEvent(EventType.ON_CREATURE_KILL)
	@RegisterType(ListenerRegisterType.NPC)
	@Id(INFILTRATION_OFFICER)
	public void onCreatureKill(OnCreatureKill event)
	{
		final Npc npc = (Npc) event.getTarget();
		
		final InstanceWorld tmpworld = InstanceManager.getInstance().getWorld(npc.getInstanceId());
		if ((tmpworld != null) && (tmpworld instanceof LoBWorld))
		{
			final LoBWorld world = (LoBWorld) tmpworld;
			
			world.nextStageBlock = true;
			finishInstance(world, 60);
		}
	}
	
	@Override
	public void onEnterInstance(PlayerInstance player, InstanceWorld world, boolean firstEntrance)
	{
		if (firstEntrance)
		{
			world.addAllowed(player.getObjectId());
			spawnInfiltrationOfficer(player, (LoBWorld) world);
			openDoor(DOOR_ID_ROOM_1_1, world.getInstanceId());
			world.setStatus(1);
		}
		teleportPlayer(player, START_LOC, world.getInstanceId());
	}
}