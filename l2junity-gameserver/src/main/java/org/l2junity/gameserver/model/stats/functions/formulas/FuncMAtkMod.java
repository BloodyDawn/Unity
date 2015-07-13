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
package org.l2junity.gameserver.model.stats.functions.formulas;

import org.l2junity.gameserver.model.actor.Creature;
import org.l2junity.gameserver.model.skills.Skill;
import org.l2junity.gameserver.model.stats.BaseStats;
import org.l2junity.gameserver.model.stats.Stats;
import org.l2junity.gameserver.model.stats.functions.AbstractFunction;

/**
 * @author UnAfraid
 */
public class FuncMAtkMod extends AbstractFunction
{
	private static final FuncMAtkMod _fma_instance = new FuncMAtkMod();
	
	public static AbstractFunction getInstance()
	{
		return _fma_instance;
	}
	
	private FuncMAtkMod()
	{
		super(Stats.MAGIC_ATTACK, 1, null, 0, null);
	}
	
	@Override
	public double calc(Creature effector, Creature effected, Skill skill, double initVal)
	{
		// Level Modifier^2 * INT Modifier^2
		final double chaMod = effector.isPlayer() ? BaseStats.CHA.calcBonus(effector) : 1.;
		return initVal * Math.pow(BaseStats.INT.calcBonus(effector), 2) * Math.pow(effector.getLevelMod(), 2) * chaMod;
	}
}
