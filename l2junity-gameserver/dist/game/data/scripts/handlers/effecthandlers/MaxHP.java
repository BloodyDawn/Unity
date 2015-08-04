/*
 * Copyright (C) 2004-2015 L2J Unity
 * 
 * This file is part of L2J Unity.
 * 
 * L2J Unity is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * L2J Unity is distributed in the hope that it will be useful,
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
import org.l2junity.gameserver.model.skills.BuffInfo;
import org.l2junity.gameserver.model.skills.Skill;
import org.l2junity.gameserver.model.stats.Stats;
import org.l2junity.gameserver.model.stats.functions.FuncAdd;
import org.l2junity.gameserver.model.stats.functions.FuncMul;

/**
 * @author NosBit
 */
public class MaxHP extends AbstractEffect
{
	private final double _amount;
	private final int _mode;
	private final boolean _heal;

	public MaxHP(Condition attachCond, Condition applyCond, StatsSet set, StatsSet params) throws IllegalArgumentException
	{
		super(attachCond, applyCond, set, params);

		_amount = params.getDouble("amount", 0);
		switch (params.getString("mode", "diff"))
		{
			case "DIFF":
			{
				_mode = 0;
				break;
			}
			case "PER":
			{
				_mode = 1;
				break;
			}
			default:
			{
				throw new IllegalArgumentException("Mode should be DIFF or PER skill id:" + params.getInt("id"));
			}
		}
		_heal = params.getBoolean("heal", false);
	}

	@Override
	public void continuousInstant(Creature effector, Creature effected, Skill skill)
	{
		if(_heal)
		{
			switch(_mode)
			{
				case 0: // DIFF
				{
					effected.setCurrentHp(effected.getCurrentHp() + _amount);
					break;
				}
				case 1: // PER
				{
					effected.setCurrentHp(effected.getCurrentHp() + (effected.getMaxHp() * (_amount / 100)));
					break;
				}
			}
		}
	}

	@Override
	public void onStart(BuffInfo info)
	{
		final Creature effected = info.getEffected();
		switch(_mode)
		{
			case 0: // DIFF
			{
				effected.addStatFunc(new FuncAdd(Stats.MAX_HP, 1, this, _amount, null));
				break;
			}
			case 1: // PER
			{
				effected.addStatFunc(new FuncMul(Stats.MAX_HP, 1, this, _amount, null));
				break;
			}
		}
	}

	@Override
	public void onExit(BuffInfo info)
	{
		final Creature effected = info.getEffected();
		effected.removeStatsOwner(this);
	}
}
