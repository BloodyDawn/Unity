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

import org.l2junity.gameserver.enums.Movie;
import org.l2junity.gameserver.instancemanager.ZoneManager;
import org.l2junity.gameserver.model.Location;
import org.l2junity.gameserver.model.StatsSet;
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
	private static final int LYDIA = 32892;
	private static final int DOOR_MANAGER = 18984;
	// Locations
	private static final Location BATTLE_LOC = new Location(208720, 120576, -10000);
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
		addEnterZoneId(TELEPORT_ZONE.getId());
		setCreatureSeeId(this::onCreatureSee, DOOR_MANAGER);
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
				if (activeInstance != null)
				{
					if (activeInstance.getTemplateId() == TEMPLATE_ID)
					{
						enterInstance(player, npc, TEMPLATE_ID);
					}
					else if (activeInstance.getTemplateId() == EXTREME_TEMPLATE_ID)
					{
						enterInstance(player, npc, EXTREME_TEMPLATE_ID);
					}
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
			}
		}
	}
	
	@Override
	public String onSpawn(Npc npc)
	{
		npc.initSeenCreatures();
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
	
	public static void main(String[] args)
	{
		new OctavisWarzone();
	}
}