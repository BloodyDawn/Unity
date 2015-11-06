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

import org.l2junity.gameserver.model.StatsSet;
import org.l2junity.gameserver.model.actor.Creature;
import org.l2junity.gameserver.model.conditions.Condition;
import org.l2junity.gameserver.model.effects.AbstractEffect;
import org.l2junity.gameserver.model.effects.L2EffectType;
import org.l2junity.gameserver.model.skills.BuffInfo;
import org.l2junity.gameserver.model.stats.Formulas;
import org.l2junity.gameserver.network.client.send.string.SystemMessageId;

/**
 * MagicalAttack-damage over time effect implementation.
 * @author Nik
 */
public final class MagicalDamOverTime extends AbstractEffect
{
	private final double _power;
	private final boolean _canKill;
	
	public MagicalDamOverTime(Condition attachCond, Condition applyCond, StatsSet set, StatsSet params)
	{
		super(attachCond, applyCond, set, params);
		
		_power = params.getDouble("power", 0);
		_canKill = params.getBoolean("canKill", false);
	}
	
	@Override
	public L2EffectType getEffectType()
	{
		return L2EffectType.MAGICAL_DMG_OVER_TIME;
	}
	
	@Override
	public boolean onActionTime(BuffInfo info)
	{
		final Creature activeChar = info.getEffector();
		final Creature target = info.getEffected();
		
		if (target.isDead())
		{
			return false;
		}
		
		final byte shld = 0; // No shield block is applied since this effect acts as a debuff.
		double damage = Formulas.calcMagicDam(activeChar, target, info.getSkill(), _power, shld, false, false, false); // In retail spiritshots change nothing.
		damage *= getTicksMultiplier();
		
		if (damage >= (target.getCurrentHp() - 1))
		{
			if (info.getSkill().isToggle())
			{
				target.sendPacket(SystemMessageId.YOUR_SKILL_HAS_BEEN_CANCELED_DUE_TO_LACK_OF_HP);
				return false;
			}
			
			// For DOT skills that will not kill effected player.
			if (!_canKill)
			{
				// Fix for players dying by DOTs if HP < 1 since reduceCurrentHP method will kill them
				if (target.getCurrentHp() <= 1)
				{
					return info.getSkill().isToggle();
				}
				damage = target.getCurrentHp() - 1;
			}
		}
		
		damage = info.getEffected().notifyDamageReceived(damage, info.getEffector(), info.getSkill(), false, true, false);
		info.getEffected().reduceCurrentHpByDOT(damage, info.getEffector(), info.getSkill());
		return info.getSkill().isToggle();
	}
}
