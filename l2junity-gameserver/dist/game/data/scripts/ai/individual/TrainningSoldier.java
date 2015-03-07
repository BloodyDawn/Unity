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
package ai.individual;

import org.l2junity.gameserver.ai.CtrlIntention;
import org.l2junity.gameserver.model.WorldObject;
import org.l2junity.gameserver.model.actor.Creature;
import org.l2junity.gameserver.model.actor.Npc;
import org.l2junity.gameserver.model.actor.instance.L2QuestGuardInstance;
import org.l2junity.gameserver.model.actor.instance.PlayerInstance;

import ai.npc.AbstractNpcAI;

/**
 * Trainning Soldier AI.
 * @author St3eT
 */
public final class TrainningSoldier extends AbstractNpcAI
{
	// NPCs
	private static final int SOLDIER = 33201; // Trainning Soldier
	private static final int DUMMY = 33023; // Trainning Dummy
	
	private TrainningSoldier()
	{
		super(TrainningSoldier.class.getSimpleName(), "ai/individual");
		addSeeCreatureId(SOLDIER);
	}
	
	@Override
	public String onAdvEvent(String event, Npc npc, PlayerInstance player)
	{
		if (event.equals("START_ATTACK") && (npc instanceof L2QuestGuardInstance))
		{
			final L2QuestGuardInstance soldier = (L2QuestGuardInstance) npc;
			
			//@formatter:off
			final Npc dummy = (Npc) soldier.getKnownList().getKnownCharactersInRadius(150)
				.stream()
				.filter(WorldObject::isNpc)
				.filter(obj -> (obj.getId() == DUMMY))
				.findFirst()
				.orElse(null);
			//@formatter:on
			
			if (dummy != null)
			{
				soldier.reduceCurrentHp(1, dummy, null); // TODO: Find better way for attack
				dummy.reduceCurrentHp(1, soldier, null);
				soldier.setCanStopAttackByTime(false);
				soldier.setCanReturnToSpawnPoint(false);
				soldier.setIsInvul(true);
			}
			else
			{
				startQuestTimer("START_ATTACK", 250, npc, null);
			}
		}
		return super.onAdvEvent(event, npc, player);
	}
	
	@Override
	public String onSeeCreature(Npc npc, Creature creature, boolean isSummon)
	{
		if (creature.isPlayer() && (npc.getAI().getIntention() != CtrlIntention.AI_INTENTION_ATTACK))
		{
			startQuestTimer("START_ATTACK", 250, npc, null);
		}
		return super.onSeeCreature(npc, creature, isSummon);
	}
	
	public static void main(String[] args)
	{
		new TrainningSoldier();
	}
}