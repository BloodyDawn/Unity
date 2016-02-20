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
package ai.individual.IsleOfPrayer;

import org.l2junity.gameserver.model.actor.Npc;
import org.l2junity.gameserver.model.actor.instance.PlayerInstance;
import org.l2junity.gameserver.model.holders.SkillHolder;
import org.l2junity.gameserver.model.skills.BuffInfo;

import ai.AbstractNpcAI;

/**
 * Eva's Gift Box AI.
 * @author St3eT
 */
public final class EvasGiftBox extends AbstractNpcAI
{
	// NPC
	private static final int BOX = 32342; // Eva's Gift Box
	// Skill
	private static final SkillHolder KISS_OF_EVA = new SkillHolder(1073, 1); // Kiss of Eva
	// Items
	private static final int CORAL = 9692; // Red Coral
	private static final int CRYSTAL = 9693; // Crystal Fragment
	
	private EvasGiftBox()
	{
		addKillId(BOX);
	}
	
	@Override
	public String onKill(Npc npc, PlayerInstance killer, boolean isSummon)
	{
		final BuffInfo buffInfo = killer.getEffectList().getBuffInfoByAbnormalType(KISS_OF_EVA.getSkill().getAbnormalType());
		final int abnoLv = buffInfo == null ? 0 : buffInfo.getSkill().getAbnormalLvl();
		
		if (abnoLv > 0)
		{
			if (getRandomBoolean())
			{
				npc.dropItem(killer, CRYSTAL, 1);
			}
			
			if (getRandom(100) < 33)
			{
				npc.dropItem(killer, CORAL, 1);
			}
		}
		return super.onKill(npc, killer, isSummon);
	}
	
	public static void main(String[] args)
	{
		new EvasGiftBox();
	}
}