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
package instances.BaylorWarzone;

import org.l2junity.gameserver.model.Location;
import org.l2junity.gameserver.model.StatsSet;
import org.l2junity.gameserver.model.actor.Creature;
import org.l2junity.gameserver.model.actor.Npc;
import org.l2junity.gameserver.model.actor.instance.DoorInstance;
import org.l2junity.gameserver.model.actor.instance.PlayerInstance;
import org.l2junity.gameserver.model.events.impl.character.OnCreatureSee;
import org.l2junity.gameserver.model.instancezone.Instance;

import instances.AbstractInstance;

/**
 * Baylor Warzone instance zone.
 * @author St3eT
 */
public final class BaylorWarzone extends AbstractInstance
{
	// NPCs
	private static final int ENTRANCE_PORTAL = 33523;
	private static final int INVISIBLE_NPC_1 = 29106;
	// private static final int INVISIBLE_NPC_2 = 29107;
	// private static final int INVISIBLE_NPC_3 = 29108;
	// private static final int INVISIBLE_NPC_4 = 29109;
	// Locations
	private static final Location BATTLE_PORT = new Location(153569, 143236, -12737);
	// Misc
	private static final int TEMPLATE_ID = 166;
	
	public BaylorWarzone()
	{
		addStartNpc(ENTRANCE_PORTAL);
		addTalkId(ENTRANCE_PORTAL);
		addInstanceCreatedId(TEMPLATE_ID);
		addSpawnId(INVISIBLE_NPC_1);
		setCreatureSeeId(this::onCreatureSee, INVISIBLE_NPC_1);
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
	public void onTimerEvent(String event, StatsSet params, Npc npc, PlayerInstance player)
	{
		final Instance world = npc.getInstanceWorld();
		if (isBylorInstance(world))
		{
			switch (event)
			{
			
			}
		}
	}
	
	@Override
	public void onInstanceCreated(Instance instance)
	{
		getTimers().addTimer("BATTLE_PORT", 3000, e ->
		{
			instance.getPlayers().forEach(p -> p.teleToLocation(BATTLE_PORT));
			instance.getDoors().forEach(DoorInstance::closeMe);
		});
	}
	
	@Override
	public String onAttack(Npc npc, PlayerInstance attacker, int damage, boolean isSummon)
	{
		final Instance world = npc.getInstanceWorld();
		if (isBylorInstance(world))
		{
		
		}
		return super.onAttack(npc, attacker, damage, isSummon);
	}
	
	@Override
	public String onKill(Npc npc, PlayerInstance killer, boolean isSummon)
	{
		final Instance world = npc.getInstanceWorld();
		if (isBylorInstance(world))
		{
		
		}
		return super.onKill(npc, killer, isSummon);
	}
	
	public void onCreatureSee(OnCreatureSee event)
	{
		final Creature creature = event.getSeen();
		final Npc npc = (Npc) event.getSeer();
		
		if (creature.isPlayer() && npc.isScriptValue(0))
		{
			npc.setScriptValue(1);
			_log.info("See creature - OK");
		}
	}
	
	@Override
	public String onSpawn(Npc npc)
	{
		if (npc.getId() == INVISIBLE_NPC_1)
		{
			npc.initSeenCreatures();
		}
		return super.onSpawn(npc);
	}
	
	private boolean isBylorInstance(Instance instance)
	{
		return (instance != null) && (instance.getTemplateId() == TEMPLATE_ID);
	}
	
	public static void main(String[] args)
	{
		new BaylorWarzone();
	}
}