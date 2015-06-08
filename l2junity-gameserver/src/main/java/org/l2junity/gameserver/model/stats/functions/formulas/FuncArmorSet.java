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
package org.l2junity.gameserver.model.stats.functions.formulas;

import java.util.HashMap;
import java.util.Map;

import org.l2junity.gameserver.data.xml.impl.ArmorSetsData;
import org.l2junity.gameserver.model.ArmorSet;
import org.l2junity.gameserver.model.actor.Creature;
import org.l2junity.gameserver.model.actor.instance.PlayerInstance;
import org.l2junity.gameserver.model.items.instance.ItemInstance;
import org.l2junity.gameserver.model.skills.Skill;
import org.l2junity.gameserver.model.stats.Stats;
import org.l2junity.gameserver.model.stats.functions.AbstractFunction;

/**
 * @author UnAfraid
 */
public class FuncArmorSet extends AbstractFunction
{
	private static final Map<Stats, FuncArmorSet> _fh_instance = new HashMap<>();
	
	public static AbstractFunction getInstance(Stats st)
	{
		if (!_fh_instance.containsKey(st))
		{
			_fh_instance.put(st, new FuncArmorSet(st));
		}
		return _fh_instance.get(st);
	}
	
	private FuncArmorSet(Stats stat)
	{
		super(stat, 1, null, 0, null);
	}
	
	@Override
	public double calc(Creature effector, Creature effected, Skill skill, double initVal)
	{
		double value = initVal;
		final PlayerInstance player = effector.getActingPlayer();
		if (player != null)
		{
			for (ItemInstance item : player.getInventory().getItems(ItemInstance::isEquipped))
			{
				for (ArmorSet set : ArmorSetsData.getInstance().getSets(item.getId()))
				{
					if (set.getPiecesCount(player, ItemInstance::getId) >= set.getMinimumPieces())
					{
						value += set.getStatsBonus(getStat());
					}
				}
			}
		}
		return value;
	}
}
