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
package instances.TeredorWarzone;

import org.l2junity.gameserver.instancemanager.WalkingManager;
import org.l2junity.gameserver.model.StatsSet;
import org.l2junity.gameserver.model.WorldObject;
import org.l2junity.gameserver.model.actor.Creature;
import org.l2junity.gameserver.model.actor.Npc;
import org.l2junity.gameserver.model.actor.instance.PlayerInstance;
import org.l2junity.gameserver.model.events.impl.character.OnCreatureSee;
import org.l2junity.gameserver.model.holders.SkillHolder;
import org.l2junity.gameserver.model.instancezone.Instance;
import org.l2junity.gameserver.model.skills.Skill;

import instances.AbstractInstance;

/**
 * Teredor Warzone instance zone.
 * @author St3eT
 */
public final class TeredorWarzone extends AbstractInstance
{
	// NPCs
	private static final int FILAUR = 30535;
	private static final int FAKE_TEREDOR = 25801;
	private static final int TEREDOR_POISON = 18998;
	private static final int BEETLE = 19024;
	private static final int POS_CHECKER = 18999;
	private static final int EGG_2 = 18997;
	private static final int ELITE_MILLIPADE = 19015;
	private static final int AWAKENED_MILLIPADE = 18995; // Awakened Millipede
	private static final int HATCHET_MILLIPADE = 18993; // Hatched Millipede
	private static final int HATCHET_UNDERBUG = 18994; // Hatched Underbug
	private static final int TEREDOR_LARVA = 19016; // Teredor's Larva
	private static final int MUTANTED_MILLIPADE = 19000; // Mutated Millipede
	// Items
	private static final int FAKE_TEREDOR_WEAPON = 15280;
	// Skill
	private static final SkillHolder FAKE_TEREDOR_JUMP_SKILL = new SkillHolder(6268, 1);
	private static final SkillHolder POISON_SKILL = new SkillHolder(14113, 1);
	private static final SkillHolder BEETLE_SKILL = new SkillHolder(14412, 1);
	// Misc
	private static final int TEMPLATE_ID = 160;
	
	public TeredorWarzone()
	{
		addStartNpc(FILAUR);
		addTalkId(FILAUR);
		addSpawnId(BEETLE, POS_CHECKER, EGG_2, FAKE_TEREDOR);
		addSpellFinishedId(BEETLE);
		addEventReceivedId(EGG_2);
		addKillId(EGG_2);
		setCreatureSeeId(this::onCreatureSee, BEETLE, POS_CHECKER, EGG_2, FAKE_TEREDOR);
	}
	
	@Override
	public void onTimerEvent(String event, StatsSet params, Npc npc, PlayerInstance player)
	{
		final Instance instance = npc.getInstanceWorld();
		if (isTeredorInstance(instance))
		{
			final StatsSet npcParams = npc.getParameters();
			
			switch (event)
			{
				case "EGG_SPAWN_TIMER":
				{
					final int spot = npcParams.getInt("Spot", 0);
					final Npc minion = addSpawn(AWAKENED_MILLIPADE, npc.getX(), npc.getY(), npc.getZ(), 0, false, 0, false, instance.getId());
					npc.deleteMe();
					
					switch (spot)
					{
						case 1:
							WalkingManager.getInstance().startMoving(minion, getRandomEntry("trajan101", "trajan102", "trajan103", "trajan104", "trajan105"));
							break;
						case 2:
							WalkingManager.getInstance().startMoving(minion, getRandomEntry("trajan106", "trajan107", "trajan108", "trajan109", "trajan110"));
							break;
						case 3:
							WalkingManager.getInstance().startMoving(minion, getRandomEntry("trajan111", "trajan112", "trajan113"));
							break;
						case 4:
							WalkingManager.getInstance().startMoving(minion, getRandomEntry("trajan114", "trajan115"));
							break;
						case 5:
							WalkingManager.getInstance().startMoving(minion, getRandomEntry("trajan116", "trajan117"));
							break;
						case 6:
							WalkingManager.getInstance().startMoving(minion, getRandomEntry("trajan118", "trajan119", "trajan120"));
							break;
						case 7:
							WalkingManager.getInstance().startMoving(minion, getRandomEntry("trajan121", "trajan122"));
							break;
						case 8:
							WalkingManager.getInstance().startMoving(minion, getRandomEntry("trajan14", "trajan15", "trajan16"));
							break;
					}
					break;
				}
				case "FAKE_TEREDOR_POISON_TIMER":
				{
					final Npc minion = addSpawn(TEREDOR_POISON, npc.getX(), npc.getY(), npc.getZ(), 0, false, 0, false, instance.getId());
					
					getTimers().addTimer("POISON_TIMER", 5000, minion, null);
					getTimers().addTimer("POISON_TIMER", 10000, minion, null);
					getTimers().addTimer("POISON_TIMER", 15000, minion, null);
					getTimers().addTimer("POISON_TIMER", 20000, minion, null);
					getTimers().addTimer("DELETE_ME", 22000, minion, null);
					break;
				}
				case "POISON_TIMER":
				{
					addSkillCastDesire(npc, npc, POISON_SKILL, 23);
					break;
				}
				case "DELETE_ME":
				{
					npc.deleteMe();
					break;
				}
			}
		}
	}
	
	@Override
	public String onAdvEvent(String event, Npc npc, PlayerInstance player)
	{
		if (event.equals("enterInstance"))
		{
			enterInstance(player, npc, TEMPLATE_ID);
		}
		return super.onAdvEvent(event, npc, player);
	}
	
	@Override
	public String onSpawn(Npc npc)
	{
		final Instance instance = npc.getInstanceWorld();
		if (isTeredorInstance(instance))
		{
			final StatsSet npcParams = npc.getParameters();
			
			switch (npc.getId())
			{
				case BEETLE:
				{
					if (npcParams.getInt("Sp", 0) == 1)
					{
						WalkingManager.getInstance().startMoving(npc, npcParams.getString("SuperPointName1", ""));
					}
					npc.initSeenCreatures();
					break;
				}
				case FAKE_TEREDOR:
				{
					WalkingManager.getInstance().startMoving(npc, npcParams.getString("SuperPointName", ""));
					npc.setRHandId(FAKE_TEREDOR_WEAPON);
					npc.initSeenCreatures();
					getTimers().addTimer("FAKE_TEREDOR_POISON_TIMER", 3000, npc, null);
					break;
				}
				default:
				{
					npc.initSeenCreatures();
					break;
				}
			}
		}
		return super.onSpawn(npc);
	}
	
	public void onCreatureSee(OnCreatureSee event)
	{
		final Creature creature = event.getSeen();
		final Npc npc = (Npc) event.getSeer();
		final Instance instance = npc.getInstanceWorld();
		
		if (isTeredorInstance(instance))
		{
			final StatsSet npcParams = npc.getParameters();
			
			switch (npc.getId())
			{
				case BEETLE:
				{
					if (creature.isPlayer())
					{
						addSkillCastDesire(npc, npc, BEETLE_SKILL, 23);
					}
					break;
				}
				case FAKE_TEREDOR:
				{
					if (creature.isPlayer() && npc.isScriptValue(0))
					{
						npc.setScriptValue(1);
						addSkillCastDesire(npc, npc, FAKE_TEREDOR_JUMP_SKILL, 23);
						getTimers().addTimer("FAKE_TEREDOR_ELITE_REUSE", 30000, n -> npc.setScriptValue(0));
						final Npc minion = addSpawn(ELITE_MILLIPADE, npc.getX(), npc.getY(), npc.getZ(), 0, false, 0, false, instance.getId());
						addAttackPlayerDesire(minion, creature.getActingPlayer());
					}
					break;
				}
				case POS_CHECKER:
				{
					if (creature.isPlayer() && npc.isScriptValue(0))
					{
						npc.setScriptValue(1);
						final int spot = npcParams.getInt("Spot", 0);
						
						switch (spot)
						{
							case 1:
							{
								instance.spawnGroup("schuttgart29_2512_sp1m1");
								npc.broadcastEvent("SCE_BREAK_AN_EGG1", 800, null);
								// getTimers().addTimer("EGG_SPAWN_TIMER", 30000, npc, null);
								break;
							}
							case 3:
							{
								instance.spawnGroup("schuttgart29_2512_sp2m1");
								npc.broadcastEvent("SCE_BREAK_AN_EGG1", 800, null);
								// getTimers().addTimer("EGG_SPAWN_TIMER", 30000, npc, null);
								break;
							}
							case 5:
							{
								instance.spawnGroup("schuttgart29_2512_sp4m1");
								break;
							}
							case 6:
							{
								instance.spawnGroup("schuttgart29_2512_306m1");
								npc.broadcastEvent("SCE_BREAK_AN_EGG1", 800, null);
								// getTimers().addTimer("EGG_SPAWN_TIMER", 30000, npc, null);
								break;
							}
							case 7:
							{
								instance.spawnGroup("schuttgart29_2512_305m1");
								npc.broadcastEvent("SCE_BREAK_AN_EGG1", 800, null);
								// getTimers().addTimer("EGG_SPAWN_TIMER", 30000, npc, null);
								break;
							}
						}
					}
					break;
				}
				case EGG_2:
				{
					if (creature.isPlayer() && npc.isScriptValue(0))
					{
						npc.setScriptValue(1);
						getTimers().addTimer("EGG_SPAWN_TIMER", (180 + getRandom(120)) * 1000, npc, null);
						npc.getVariables().set("SEE_CREATURE_ID", creature.getObjectId());
					}
					break;
				}
			}
		}
	}
	
	@Override
	public String onEventReceived(String eventName, Npc sender, Npc npc, WorldObject reference)
	{
		final Instance instance = npc.getInstanceWorld();
		if (isTeredorInstance(instance))
		{
			switch (npc.getId())
			{
				case EGG_2:
				{
					switch (eventName)
					{
						case "SCE_EGG_DIE":
						{
							npc.setState(2);
							final PlayerInstance player = instance.getPlayerById(npc.getVariables().getInt("SEE_CREATURE_ID", 0));
							if (player != null)
							{
								final Npc minion = addSpawn(getRandomBoolean() ? HATCHET_MILLIPADE : HATCHET_UNDERBUG, npc.getX(), npc.getY(), npc.getZ(), 0, false, 0, false, instance.getId());
								addAttackPlayerDesire(minion, player, 23);
								npc.deleteMe();
							}
							break;
						}
						case "SCE_BREAK_AN_EGG1":
						{
							npc.setState(2);
							break;
						}
						case "SCE_BREAK_AN_EGG2":
						{
							final PlayerInstance player = instance.getPlayerById(npc.getVariables().getInt("SEE_CREATURE_ID", 0));
							
							switch (npc.getParameters().getInt("Spot", 0))
							{
								case 1:
								{
									final Npc minion = addSpawn(getRandomBoolean() ? TEREDOR_LARVA : MUTANTED_MILLIPADE, npc.getX(), npc.getY(), npc.getZ(), 0, false, 0, false, instance.getId());
									if (player != null)
									{
										addAttackPlayerDesire(minion, player, 23);
									}
									npc.deleteMe();
									break;
								}
								case 2:
								case 3:
								{
									final Npc minion = addSpawn(getRandomBoolean() ? MUTANTED_MILLIPADE : HATCHET_UNDERBUG, npc.getX(), npc.getY(), npc.getZ(), 0, false, 0, false, instance.getId());
									if (player != null)
									{
										addAttackPlayerDesire(minion, player, 23);
									}
									npc.deleteMe();
									break;
								}
								case 4:
								case 5:
								case 6:
								{
									final Npc minion = addSpawn(getRandomBoolean() ? MUTANTED_MILLIPADE : HATCHET_UNDERBUG, npc.getX(), npc.getY(), npc.getZ(), 0, false, 0, false, instance.getId());
									if (player != null)
									{
										addAttackPlayerDesire(minion, player, 23);
									}
									npc.deleteMe();
									break;
								}
								case 7:
								{
									final Npc minion = addSpawn(getRandomEntry(MUTANTED_MILLIPADE, HATCHET_UNDERBUG, HATCHET_MILLIPADE), npc.getX(), npc.getY(), npc.getZ(), 0, false, 0, false, instance.getId());
									if (player != null)
									{
										addAttackPlayerDesire(minion, player, 23);
									}
									npc.deleteMe();
									break;
								}
							}
							break;
						}
						case "SCE_BREAK_AN_EGG3":
						{
							npc.setState(4);
							break;
						}
					}
					break;
				}
			}
		}
		return super.onEventReceived(eventName, sender, npc, reference);
	}
	
	@Override
	public String onKill(Npc npc, PlayerInstance killer, boolean isSummon)
	{
		final Instance instance = npc.getInstanceWorld();
		if (isTeredorInstance(instance))
		{
			switch (npc.getId())
			{
				case EGG_2:
				{
					if (getRandom(4) < 3)
					{
						final Npc minion = addSpawn(getRandomBoolean() ? MUTANTED_MILLIPADE : TEREDOR_LARVA, npc.getX(), npc.getY(), npc.getZ(), 0, false, 0, false, instance.getId());
						addAttackPlayerDesire(minion, killer, 23);
						npc.deleteMe();
					}
					break;
				}
			}
		}
		return super.onKill(npc, killer, isSummon);
	}
	
	@Override
	public String onSpellFinished(Npc npc, PlayerInstance player, Skill skill)
	{
		final Instance instance = npc.getInstanceWorld();
		if (isTeredorInstance(instance))
		{
			switch (npc.getId())
			{
				case BEETLE:
				{
					npc.broadcastEvent("SCE_EGG_DIE", 500, null);
					break;
				}
			}
		}
		return super.onSpellFinished(npc, player, skill);
	}
	
	private boolean isTeredorInstance(Instance instance)
	{
		return ((instance != null) && (instance.getTemplateId() == TEMPLATE_ID));
	}
	
	public static void main(String[] args)
	{
		new TeredorWarzone();
	}
}