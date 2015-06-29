/*
 * Copyright (C) 2004-2015 L2J Server
 * 
 * This file is part of L2J Server.
 * 
 * L2J Server is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * L2J Server is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package ai.group_template;

import org.l2junity.gameserver.model.actor.Npc;
import org.l2junity.gameserver.model.actor.instance.L2MonsterInstance;

import ai.npc.AbstractNpcAI;

/**
 * Orc Barracks AI
 * @author malyelfik
 */
public final class OrcBarracks extends AbstractNpcAI
{
	// NPC
	private static final int TUREK_ORC_FOOTMAN = 20499;
	private static final int TUREK_WAR_HOUND = 20494;
	// Misc
	private static final int MINION_COUNT = 2;
	
	public OrcBarracks()
	{
		super(OrcBarracks.class.getSimpleName(), "ai/group_template");
		addSpawnId(TUREK_ORC_FOOTMAN);
	}
	
	@Override
	public String onSpawn(Npc npc)
	{
		final boolean hasMinions = npc.getParameters().getBoolean("hasMinions", false);
		if (hasMinions)
		{
			for (int i = 0; i < MINION_COUNT; i++)
			{
				addMinion((L2MonsterInstance) npc, TUREK_WAR_HOUND);
			}
		}
		return super.onSpawn(npc);
	}
	
	public static void main(String[] args)
	{
		new OrcBarracks();
	}
}
