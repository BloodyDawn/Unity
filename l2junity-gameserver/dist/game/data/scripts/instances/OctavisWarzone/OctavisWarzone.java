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
package instances.OctavisWarzone;

import org.l2junity.commons.util.CommonUtil;
import org.l2junity.gameserver.enums.Movie;
import org.l2junity.gameserver.instancemanager.WalkingManager;
import org.l2junity.gameserver.instancemanager.ZoneManager;
import org.l2junity.gameserver.model.Location;
import org.l2junity.gameserver.model.StatsSet;
import org.l2junity.gameserver.model.actor.Attackable;
import org.l2junity.gameserver.model.actor.Creature;
import org.l2junity.gameserver.model.actor.Npc;
import org.l2junity.gameserver.model.actor.instance.PlayerInstance;
import org.l2junity.gameserver.model.events.impl.character.OnCreatureSee;
import org.l2junity.gameserver.model.instancezone.Instance;
import org.l2junity.gameserver.model.zone.ZoneType;
import org.l2junity.gameserver.model.zone.type.ScriptZone;

import instances.AbstractInstance;

/**
 * Octavis Warzone instance zone.
 * @author St3eT
 */
public final class OctavisWarzone extends AbstractInstance
{
	// NPCs
	private static final int[] OCTAVIS_STAGE_1 =
	{
		29191, // Common
		29209, // Extreme
	};
	private static final int[] OCTAVIS_STAGE_2 =
	{
		29193, // Common
		29211, // Extreme
	};
	private static final int[] BEASTS =
	{
		29192, // Common
		29210, // Extreme
	};
	private static final int LYDIA = 32892;
	private static final int DOOR_MANAGER = 18984;
	// Locations
	private static final Location BATTLE_LOC = new Location(208720, 120576, -10000);
	private static final Location OCTAVIS_SPAWN_LOC = new Location(207069, 120580, -9987);
	// Zones
	private static final ScriptZone TELEPORT_ZONE = ZoneManager.getInstance().getZoneById(12042, ScriptZone.class);
	// Misc
	private static final int TEMPLATE_ID = 180;
	private static final int EXTREME_TEMPLATE_ID = 181;
	private static final int MAIN_DOOR_1 = 26210002;
	private static final int MAIN_DOOR_2 = 26210001;
	
	public OctavisWarzone()
	{
		addStartNpc(LYDIA);
		addTalkId(LYDIA);
		addSpawnId(DOOR_MANAGER);
		addAttackId(OCTAVIS_STAGE_1);
		addAttackId(BEASTS);
		addKillId(OCTAVIS_STAGE_1);
		addKillId(OCTAVIS_STAGE_2);
		addEnterZoneId(TELEPORT_ZONE.getId());
		setCreatureSeeId(this::onCreatureSee, DOOR_MANAGER);
		addInstanceDestroyId(TEMPLATE_ID, EXTREME_TEMPLATE_ID);
	}
	
	@Override
	public String onAdvEvent(String event, Npc npc, PlayerInstance player)
	{
		switch (event)
		{
			case "enterEasyInstance":
			{
				enterInstance(player, npc, TEMPLATE_ID);
				return "PartyEnterCommon.html";
			}
			case "enterExtremeInstance":
			{
				enterInstance(player, npc, EXTREME_TEMPLATE_ID);
				break;
			}
			case "reenterInstance":
			{
				final Instance activeInstance = getPlayerInstance(player);
				if (isOctavisInstance(activeInstance))
				{
					enterInstance(player, npc, activeInstance.getTemplateId());
					return "PartyMemberReenter.html";
				}
			}
		}
		return super.onAdvEvent(event, npc, player);
	}
	
	@Override
	public void onTimerEvent(String event, StatsSet params, Npc npc, PlayerInstance player)
	{
		final Instance world = npc.getInstanceWorld();
		if (isOctavisInstance(world))
		{
			switch (event)
			{
				case "SECOND_DOOR_OPEN":
				{
					world.openCloseDoor(MAIN_DOOR_2, true);
					break;
				}
				case "CLOSE_DOORS":
				{
					world.openCloseDoor(MAIN_DOOR_2, false);
					world.openCloseDoor(MAIN_DOOR_1, false);
					world.getParameters().set("TELEPORT_ACTIVE", true);
					npc.teleToLocation(BATTLE_LOC);
					playMovie(world, Movie.SC_OCTABIS_OPENING);
					getTimers().addTimer("START_STAGE_1", 26500, npc, null);
					break;
				}
				case "START_STAGE_1":
				{
					world.spawnGroup("STAGE_1");
					world.getAliveNpcs(BEASTS).forEach(beasts ->
					{
						beasts.disableCoreAI(true);
						beasts.setUndying(true);
						((Attackable) beasts).setCanReturnToSpawnPoint(false);
						final Npc octavis = addSpawn((!isExtremeMode(world) ? OCTAVIS_STAGE_1[0] : OCTAVIS_STAGE_1[1]), OCTAVIS_SPAWN_LOC, false, 0, false, world.getId());
						octavis.disableCoreAI(true);
						octavis.setIsRunning(true);
						octavis.sendChannelingEffect(beasts, 1);
						octavis.setTargetable(false);
						((Attackable) octavis).setCanReturnToSpawnPoint(false);
						getTimers().addRepeatingTimer("FOLLOW_BEASTS", 500, octavis, null);
						WalkingManager.getInstance().startMoving(beasts, "octabis_superpoint");
					});
					break;
				}
				case "FOLLOW_BEASTS":
				{
					world.getAliveNpcs(BEASTS).forEach(beasts ->
					{
						addMoveToDesire(npc, beasts.getLocation(), 23);
						npc.sendChannelingEffect(beasts, 1);
					});
					break;
				}
				case "END_STAGE_1":
				{
					playMovie(world, Movie.SC_OCTABIS_PHASECH_A);
					getTimers().addTimer("START_STAGE_2", 12000, npc, null);
					break;
				}
				case "START_STAGE_2":
				{
					world.spawnGroup("STAGE_2");
					break;
				}
			}
		}
	}
	
	@Override
	public String onAttack(Npc npc, PlayerInstance attacker, int damage, boolean isSummon)
	{
		final Instance world = npc.getInstanceWorld();
		if (isOctavisInstance(world))
		{
			final int hpPer = npc.getCurrentHpPercent();
			
			if (CommonUtil.contains(OCTAVIS_STAGE_1, npc.getId()))
			{
				if (hpPer >= 90)
				{
					npc.setState(0);
				}
				else if (hpPer >= 80)
				{
					npc.setState(1);
				}
				else if (hpPer >= 70)
				{
					npc.setState(2);
				}
				else if (hpPer >= 60)
				{
					npc.setState(3);
				}
				else if (hpPer >= 50)
				{
					npc.setState(4);
				}
				else
				{
					npc.setState(5);
				}
			}
			else if (CommonUtil.contains(BEASTS, npc.getId()))
			{
				if ((hpPer < 50) && npc.isScriptValue(0))
				{
					// gg->SetNpcParam(myself->sm, @HP_Regen, boost_regen_value); (boost_regen_value is 95000) //TODO: Finish me!
					npc.setScriptValue(1);
				}
				else if ((hpPer > 90) && npc.isScriptValue(1))
				{
					// gg->SetNpcParam(myself->sm, @HP_Regen, 0); //TODO: Finish me!
					npc.setScriptValue(0);
				}
				
				final Npc octavis = world.getAliveNpcs(OCTAVIS_STAGE_1).stream().findAny().orElse(null);
				if (octavis != null)
				{
					octavis.setTargetable(hpPer < 50);
				}
			}
		}
		return super.onAttack(npc, attacker, damage, isSummon);
	}
	
	@Override
	public String onKill(Npc npc, PlayerInstance killer, boolean isSummon)
	{
		final Instance world = npc.getInstanceWorld();
		if (isOctavisInstance(world))
		{
			if (CommonUtil.contains(OCTAVIS_STAGE_1, npc.getId()))
			{
				getTimers().cancelTimer("FOLLOW_BEASTS", npc, null);
				world.getAliveNpcs(BEASTS).forEach(beasts -> beasts.deleteMe());
				getTimers().addTimer("END_STAGE_1", 1000, npc, null);
			}
		}
		return super.onKill(npc, killer, isSummon);
	}
	
	@Override
	public void onInstanceDestroy(Instance instance)
	{
		instance.getAliveNpcs(OCTAVIS_STAGE_1).forEach(octavis ->
		{
			getTimers().cancelTimer("FOLLOW_BEASTS", octavis, null);
		});
		
	}
	
	@Override
	public String onSpawn(Npc npc)
	{
		final Instance world = npc.getInstanceWorld();
		if (isOctavisInstance(world))
		{
			npc.initSeenCreatures();
		}
		return super.onSpawn(npc);
	}
	
	public void onCreatureSee(OnCreatureSee event)
	{
		final Creature creature = event.getSeen();
		final Npc npc = (Npc) event.getSeer();
		final Instance world = npc.getInstanceWorld();
		
		if (isOctavisInstance(world) && creature.isPlayer() && npc.isScriptValue(0))
		{
			world.openCloseDoor(MAIN_DOOR_1, true);
			getTimers().addTimer("SECOND_DOOR_OPEN", 3000, npc, null);
			getTimers().addTimer("CLOSE_DOORS", 60000, npc, null);
			npc.setScriptValue(1);
		}
	}
	
	@Override
	public String onEnterZone(Creature character, ZoneType zone)
	{
		final Instance world = character.getInstanceWorld();
		if (character.isPlayer() && isOctavisInstance(world))
		{
			if (world.getParameters().getBoolean("TELEPORT_ACTIVE", false))
			{
				character.teleToLocation(BATTLE_LOC);
			}
		}
		return super.onEnterZone(character, zone);
	}
	
	private boolean isOctavisInstance(Instance instance)
	{
		return ((instance != null) && ((instance.getTemplateId() == TEMPLATE_ID) || (instance.getTemplateId() == EXTREME_TEMPLATE_ID)));
	}
	
	private boolean isExtremeMode(Instance instance)
	{
		return instance.getTemplateId() == EXTREME_TEMPLATE_ID;
	}
	
	public static void main(String[] args)
	{
		new OctavisWarzone();
	}
}