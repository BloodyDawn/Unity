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
package org.l2junity.gameserver.model.stats;

import java.io.File;
import java.util.NoSuchElementException;
import java.util.function.Function;

import org.l2junity.commons.util.IXmlReader;
import org.l2junity.gameserver.data.xml.IGameXmlReader;
import org.l2junity.gameserver.model.actor.Creature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;

/**
 * @author DS
 */
public enum BaseStats
{
	STR(Creature::getSTR, Stats.STAT_STR),
	INT(Creature::getINT, Stats.STAT_INT),
	DEX(Creature::getDEX, Stats.STAT_DEX),
	WIT(Creature::getWIT, Stats.STAT_WIT),
	CON(Creature::getCON, Stats.STAT_CON),
	MEN(Creature::getMEN, Stats.STAT_MEN),
	CHA(Creature::getCHA, Stats.STAT_CHA),
	LUC(Creature::getLUC, Stats.STAT_LUC),
	NONE(creature -> 0, null);
	
	public static final int MAX_STAT_VALUE = 201;
	
	private final double[] _bonus = new double[MAX_STAT_VALUE];
	private final Function<Creature, Integer> _valueCalculator;
	private final Stats _stat;
	
	BaseStats(Function<Creature, Integer> valueCalculator, Stats stat)
	{
		_valueCalculator = valueCalculator;
		_stat = stat;
	}
	
	public Stats getStat()
	{
		return _stat;
	}
	
	public int calcValue(Creature creature)
	{
		if (creature != null)
		{
			return Math.min(_valueCalculator.apply(creature), MAX_STAT_VALUE - 1);
		}
		return 0;
	}
	
	public double calcBonus(Creature creature)
	{
		if (creature != null)
		{
			return _bonus[calcValue(creature)];
		}
		
		return 1;
	}
	
	void setValue(int index, double value)
	{
		_bonus[index] = value;
	}
	
	public static BaseStats valueOf(Stats stat)
	{
		for (BaseStats baseStat : values())
		{
			if (baseStat.getStat() == stat)
			{
				return baseStat;
			}
		}
		throw new NoSuchElementException("Unknown base stat '" + stat + "' for enum BaseStats");
	}
	
	static
	{
		new IGameXmlReader()
		{
			final Logger LOGGER = LoggerFactory.getLogger(BaseStats.class);
			
			@Override
			public void load()
			{
				parseDatapackFile("data/stats/statBonus.xml");
			}
			
			@Override
			public void parseDocument(Document doc, File f)
			{
				forEach(doc, "list", listNode -> forEach(listNode, IXmlReader::isNode, statNode ->
				{
					final BaseStats baseStat;
					try
					{
						baseStat = valueOf(statNode.getNodeName());
					}
					catch (Exception e)
					{
						LOGGER.error("Invalid base stats type: {}, skipping", statNode.getNodeValue());
						return;
					}
					
					forEach(statNode, "stat", statValue ->
					{
						final NamedNodeMap attrs = statValue.getAttributes();
						final int val = parseInteger(attrs, "value");
						final double bonus = parseDouble(attrs, "bonus");
						baseStat.setValue(val, bonus);
					});
				}));
			}
		}.load();
	}
}