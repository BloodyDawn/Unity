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
package hellbound.AI;

import org.l2junity.gameserver.model.actor.Creature;
import org.l2junity.gameserver.model.actor.Npc;
import org.l2junity.gameserver.model.actor.instance.PlayerInstance;
import org.l2junity.gameserver.model.holders.SkillHolder;

import hellbound.HellboundEngine;
import ai.npc.AbstractNpcAI;

/**
 * Manages Naia's cast on the Hellbound Core
 * @author GKR
 */
public final class HellboundCore extends AbstractNpcAI
{
	// NPCs
	private static final int NAIA = 18484;
	private static final int HELLBOUND_CORE = 32331;
	// Skills
	private static SkillHolder BEAM = new SkillHolder(5493, 1);
	
	public HellboundCore()
	{
		super(HellboundCore.class.getSimpleName(), "hellbound/AI");
		addSpawnId(HELLBOUND_CORE);
	}
	
	@Override
	public String onAdvEvent(String event, Npc npc, PlayerInstance player)
	{
		if (event.equalsIgnoreCase("cast") && (HellboundEngine.getInstance().getLevel() <= 6))
		{
			for (Creature naia : npc.getKnownList().getKnownCharactersInRadius(900))
			{
				if ((naia != null) && naia.isMonster() && (naia.getId() == NAIA) && !naia.isDead())
				{
					naia.setTarget(npc);
					naia.doSimultaneousCast(BEAM.getSkill());
				}
			}
			startQuestTimer("cast", 10000, npc, null);
		}
		return super.onAdvEvent(event, npc, player);
	}
	
	@Override
	public final String onSpawn(Npc npc)
	{
		startQuestTimer("cast", 10000, npc, null);
		return super.onSpawn(npc);
	}
}