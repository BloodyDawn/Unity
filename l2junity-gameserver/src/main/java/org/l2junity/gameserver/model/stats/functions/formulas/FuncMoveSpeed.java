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
 * @author Nik
 */
public class FuncMoveSpeed extends AbstractFunction
{
	private static final FuncMoveSpeed _fas_instance = new FuncMoveSpeed();
	
	public static AbstractFunction getInstance()
	{
		return _fas_instance;
	}
	
	private FuncMoveSpeed()
	{
		super(Stats.MOVE_SPEED, 1, null, 0, null);
	}
	
	@Override
	public double calc(Creature effector, Creature effected, Skill skill, double initVal)
	{
		int speedBonus = 0;
		byte speedStat = (byte) effector.calcStat(Stats.STAT_SPEED, -1);
		if ((speedStat >= 0) && (speedStat < BaseStats.values().length))
		{
			// Bad way of implementation... rework it once a better way is found.
			switch (speedStat)
			{
				case 0: // STR
					return Math.max(0, effector.getSTR() - 55);
				case 1: // INT
					return Math.max(0, effector.getINT() - 55);
				case 2: // DEX
					return Math.max(0, effector.getDEX() - 55);
				case 3: // WIT
					return Math.max(0, effector.getWIT() - 55);
				case 4: // CON
					return Math.max(0, effector.getCON() - 55);
				case 5: // MEN
					return Math.max(0, effector.getMEN() - 55);
				case 6: // CHA
					return Math.max(0, effector.getCHA() - 55);
				case 7: // LUC
					return Math.max(0, effector.getLUC() - 55);
			}
		}
		
		return initVal + speedBonus;
	}
}
