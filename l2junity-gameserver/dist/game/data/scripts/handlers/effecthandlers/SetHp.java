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
import org.l2junity.gameserver.model.skills.Skill;

/**
 * An effect that sets the current hp to the given amount.
 * @author Nik
 */
public final class SetHp extends AbstractEffect
{
	private final double _amount;
	private final boolean _isPercent;
	
	public SetHp(Condition attachCond, Condition applyCond, StatsSet set, StatsSet params)
	{
		super(attachCond, applyCond, set, params);
		
		_amount = params.getDouble("amount", 0);
		_isPercent = params.getBoolean("isPercent", false);
	}
	
	@Override
	public boolean isInstant()
	{
		return true;
	}

	@Override
	public void instant(Creature effector, Creature effected, Skill skill)
	{
		if (effected.isDead() || effected.isDoor())
		{
			return;
		}
		
		boolean full = _isPercent && (_amount == 100.0);
		double amount = full ? effected.getMaxHp() : _isPercent ? ((effected.getMaxHp() * _amount) / 100.0) : _amount;
		effected.setCurrentHp(amount);
	}
}
