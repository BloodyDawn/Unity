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
package handlers.effecthandlers;

import org.l2junity.commons.util.Rnd;
import org.l2junity.gameserver.model.StatsSet;
import org.l2junity.gameserver.model.actor.Summon;
import org.l2junity.gameserver.model.actor.instance.L2PcInstance;
import org.l2junity.gameserver.model.conditions.Condition;
import org.l2junity.gameserver.model.effects.AbstractEffect;
import org.l2junity.gameserver.model.skills.BuffInfo;
import org.l2junity.gameserver.model.stats.Formulas;
import org.l2junity.gameserver.network.SystemMessageId;

/**
 * Unsummon effect implementation.
 * @author Adry_85
 */
public final class Unsummon extends AbstractEffect
{
	private final int _chance;
	
	public Unsummon(Condition attachCond, Condition applyCond, StatsSet set, StatsSet params)
	{
		super(attachCond, applyCond, set, params);
		
		_chance = params.getInt("chance", 100);
	}
	
	@Override
	public boolean calcSuccess(BuffInfo info)
	{
		int magicLevel = info.getSkill().getMagicLevel();
		if ((magicLevel <= 0) || ((info.getEffected().getLevel() - 9) <= magicLevel))
		{
			double chance = _chance * Formulas.calcAttributeBonus(info.getEffector(), info.getEffected(), info.getSkill()) * Formulas.calcGeneralTraitBonus(info.getEffector(), info.getEffected(), info.getSkill().getTraitType(), false);
			if (chance > (Rnd.nextDouble() * 100))
			{
				return true;
			}
		}
		
		return false;
	}
	
	@Override
	public boolean canStart(BuffInfo info)
	{
		return info.getEffected().isSummon();
	}
	
	@Override
	public boolean isInstant()
	{
		return true;
	}
	
	@Override
	public void onStart(BuffInfo info)
	{
		if (info.getEffected().isServitor())
		{
			final Summon servitor = (Summon) info.getEffected();
			final L2PcInstance summonOwner = servitor.getOwner();
			
			servitor.abortAttack();
			servitor.abortCast();
			servitor.stopAllEffects();
			
			servitor.unSummon(summonOwner);
			summonOwner.sendPacket(SystemMessageId.YOUR_SERVITOR_HAS_VANISHED_YOU_LL_NEED_TO_SUMMON_A_NEW_ONE);
		}
	}
}
