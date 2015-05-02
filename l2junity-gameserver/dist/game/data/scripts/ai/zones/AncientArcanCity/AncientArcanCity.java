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
package ai.zones.AncientArcanCity;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import org.l2junity.gameserver.data.xml.IXmlReader;
import org.l2junity.gameserver.enums.Movie;
import org.l2junity.gameserver.instancemanager.ZoneManager;
import org.l2junity.gameserver.model.Location;
import org.l2junity.gameserver.model.actor.Creature;
import org.l2junity.gameserver.model.actor.Npc;
import org.l2junity.gameserver.model.actor.instance.PlayerInstance;
import org.l2junity.gameserver.model.holders.SpawnHolder;
import org.l2junity.gameserver.model.zone.ZoneType;
import org.l2junity.gameserver.model.zone.type.PeaceZone;
import org.l2junity.gameserver.model.zone.type.ScriptZone;
import org.l2junity.gameserver.network.client.send.Earthquake;
import org.l2junity.gameserver.network.client.send.ExShowScreenMessage;
import org.l2junity.gameserver.network.client.send.OnEventTrigger;
import org.l2junity.gameserver.network.client.send.string.NpcStringId;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

import ai.npc.AbstractNpcAI;

/**
 * Ancient Arcan City AI.
 * @author St3eT
 */
public final class AncientArcanCity extends AbstractNpcAI implements IXmlReader
{
	// NPC
	private static final int CEREMONIAL_CAT = 33093;
	// Location
	private static final Location ANCIENT_ARCAN_CITY = new Location(207559, 86429, -1000);
	// Zones
	private static final PeaceZone TOWN_ZONE = ZoneManager.getInstance().getZoneById(23600, PeaceZone.class); // Ancient Arcan City zone
	private static final ScriptZone TELEPORT_ZONE = ZoneManager.getInstance().getZoneById(12015, ScriptZone.class); // Anghel Waterfall teleport zone
	// Misc
	private static final int CHANGE_STATE_TIME = 1800000; // 30min
	private static final List<SpawnHolder> SPAWNS = new ArrayList<>();
	private static final List<Npc> SPAWNED_NPCS = new CopyOnWriteArrayList<>();
	private boolean isCeremonyRunning = false;
	
	private AncientArcanCity()
	{
		super(AncientArcanCity.class.getSimpleName(), "ai/group_template");
		addEnterZoneId(TOWN_ZONE.getId(), TELEPORT_ZONE.getId());
		load();
		notifyEvent("CHANGE_STATE", null, null);
		startQuestTimer("CHANGE_STATE", CHANGE_STATE_TIME, null, null, true);
	}
	
	@Override
	public String onAdvEvent(String event, Npc npc, PlayerInstance player)
	{
		if (event.equals("CHANGE_STATE"))
		{
			isCeremonyRunning = !isCeremonyRunning;
			
			for (PlayerInstance temp : TOWN_ZONE.getPlayersInside())
			{
				temp.sendPacket(new OnEventTrigger(262001, !isCeremonyRunning));
				temp.sendPacket(new OnEventTrigger(262003, isCeremonyRunning));
				
				if (isCeremonyRunning)
				{
					showOnScreenMsg(temp, NpcStringId.THE_INCREASED_GRASP_OF_DARK_ENERGY_CAUSES_THE_GROUND_TO_SHAKE, ExShowScreenMessage.TOP_CENTER, 5000, true);
					temp.sendPacket(new Earthquake(207088, 88720, -1128, 10, 5));
				}
			}
			
			if (isCeremonyRunning)
			{
				for (SpawnHolder holder : SPAWNS)
				{
					final Npc temp = addSpawn(holder.getNpcId(), holder.getLocation());
					SPAWNED_NPCS.add(temp);
					if (temp.getId() == CEREMONIAL_CAT)
					{
						temp.setRandomAnimationEnabled(false);
						startQuestTimer("SOCIAL_ACTION", 4500, temp, null, true);
					}
				}
			}
			else
			{
				SPAWNED_NPCS.stream().forEach(Npc::deleteMe);
				SPAWNED_NPCS.clear();
				cancelQuestTimers("SOCIAL_ACTION");
			}
		}
		else if (event.contains("SOCIAL_ACTION") && (npc != null))
		{
			npc.broadcastSocialAction(2);
		}
		return super.onAdvEvent(event, npc, player);
	}
	
	@Override
	public String onEnterZone(Creature creature, ZoneType zone)
	{
		if (creature.isPlayer())
		{
			final PlayerInstance player = creature.getActingPlayer();
			
			if (zone.getId() == TELEPORT_ZONE.getId())
			{
				player.teleToLocation(ANCIENT_ARCAN_CITY);
			}
			else
			{
				player.sendPacket(new OnEventTrigger(262001, !isCeremonyRunning));
				player.sendPacket(new OnEventTrigger(262003, isCeremonyRunning));
				
				if (player.getVariables().getBoolean("ANCIEJNT_ARCAN_CITY_SCENE", true))
				{
					player.getVariables().set("ANCIEJNT_ARCAN_CITY_SCENE", false);
					playMovie(player, Movie.SI_ARKAN_ENTER);
				}
			}
		}
		return super.onEnterZone(creature, zone);
	}
	
	@Override
	public synchronized void load()
	{
		SPAWNS.clear();
		parseDatapackFile("data/scripts/ai/zones/AncientArcanCity/spawnlist.xml");
	}
	
	@Override
	public void parseDocument(Document doc, File f)
	{
		for (Node n = doc.getFirstChild(); n != null; n = n.getNextSibling())
		{
			if ("list".equalsIgnoreCase(n.getNodeName()))
			{
				for (Node d = n.getFirstChild(); d != null; d = d.getNextSibling())
				{
					if ("spawn".equalsIgnoreCase(d.getNodeName()))
					{
						final NamedNodeMap attrs = d.getAttributes();
						final int npcId = parseInteger(attrs, "npcId");
						final int x = parseInteger(attrs, "x");
						final int y = parseInteger(attrs, "y");
						final int z = parseInteger(attrs, "z");
						final int heading = parseInteger(attrs, "heading");
						SPAWNS.add(new SpawnHolder(npcId, x, y, z, heading));
					}
				}
			}
		}
	}
	
	public static void main(String[] args)
	{
		new AncientArcanCity();
	}
}
