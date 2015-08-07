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

import javax.xml.parsers.DocumentBuilderFactory;

import org.l2junity.Config;
import org.l2junity.gameserver.model.actor.Creature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

/**
 * @author DS
 */
public enum BaseStats
{
	STR(BaseStats::calcSTRBonus),
	INT(BaseStats::calcINTBonus),
	DEX(BaseStats::calcDEXBonus),
	WIT(BaseStats::calcWITBonus),
	CON(BaseStats::calcCONBonus),
	MEN(BaseStats::calcMENBonus),
	CHA(BaseStats::calcCHABonus),
	NONE(creature -> 1d);
	
	private static final Logger LOGGER = LoggerFactory.getLogger(BaseStats.class);
	
	public static final int MAX_STAT_VALUE = 201;
	
	private static final double[] STR_BONUS = new double[MAX_STAT_VALUE];
	private static final double[] INT_BONUS = new double[MAX_STAT_VALUE];
	private static final double[] DEX_BONUS = new double[MAX_STAT_VALUE];
	private static final double[] WIT_BONUS = new double[MAX_STAT_VALUE];
	private static final double[] CON_BONUS = new double[MAX_STAT_VALUE];
	private static final double[] MEN_BONUS = new double[MAX_STAT_VALUE];
	private static final double[] CHA_BONUS = new double[MAX_STAT_VALUE];
	
	private final Function<Creature, Double> _bonusCalculator;
	
	BaseStats(Function<Creature, Double> bonusCalculator)
	{
		_bonusCalculator = bonusCalculator;
	}
	
	public final double calcBonus(Creature actor)
	{
		if (actor != null)
		{
			return _bonusCalculator.apply(actor);
		}
		
		return 1;
	}
	
	public static BaseStats valueOfXml(String name)
	{
		name = name.intern();
		for (BaseStats s : values())
		{
			if (s.name().equalsIgnoreCase(name))
			{
				return s;
			}
		}
		throw new NoSuchElementException("Unknown name '" + name + "' for enum BaseStats");
	}
	
	public static double calcSTRBonus(Creature actor)
	{
		return calcBonus(STR_BONUS, actor.getSTR());
	}
	
	public static double calcINTBonus(Creature actor)
	{
		return calcBonus(INT_BONUS, actor.getINT());
	}
	
	public static double calcDEXBonus(Creature actor)
	{
		return calcBonus(DEX_BONUS, actor.getDEX());
	}
	
	public static double calcWITBonus(Creature actor)
	{
		return calcBonus(WIT_BONUS, actor.getWIT());
	}
	
	public static double calcCONBonus(Creature actor)
	{
		return calcBonus(CON_BONUS, actor.getCON());
	}
	
	public static double calcMENBonus(Creature actor)
	{
		return calcBonus(MEN_BONUS, actor.getMEN());
	}
	
	public static double calcCHABonus(Creature actor)
	{
		return calcBonus(CHA_BONUS, actor.getCHA());
	}
	
	public static double calcBonus(double[] values, int value)
	{
		return values[Math.min(value, MAX_STAT_VALUE - 1)];
	}
	
	static
	{
		final DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		factory.setValidating(false);
		factory.setIgnoringComments(true);
		final File file = new File(Config.DATAPACK_ROOT, "data/stats/statBonus.xml");
		Document doc = null;
		
		if (file.exists())
		{
			try
			{
				doc = factory.newDocumentBuilder().parse(file);
			}
			catch (Exception e)
			{
				LOGGER.warn("Could not parse file: " + e.getMessage(), e);
			}
			
			if (doc != null)
			{
				String statName;
				int val;
				double bonus;
				NamedNodeMap attrs;
				for (Node list = doc.getFirstChild(); list != null; list = list.getNextSibling())
				{
					if ("list".equalsIgnoreCase(list.getNodeName()))
					{
						for (Node stat = list.getFirstChild(); stat != null; stat = stat.getNextSibling())
						{
							statName = stat.getNodeName();
							for (Node value = stat.getFirstChild(); value != null; value = value.getNextSibling())
							{
								if ("stat".equalsIgnoreCase(value.getNodeName()))
								{
									attrs = value.getAttributes();
									try
									{
										val = Integer.parseInt(attrs.getNamedItem("value").getNodeValue());
										bonus = Double.parseDouble(attrs.getNamedItem("bonus").getNodeValue());
									}
									catch (Exception e)
									{
										LOGGER.error("Invalid stats value: " + value.getNodeValue() + ", skipping");
										continue;
									}
									
									if ("STR".equalsIgnoreCase(statName))
									{
										STR_BONUS[val] = bonus;
									}
									else if ("INT".equalsIgnoreCase(statName))
									{
										INT_BONUS[val] = bonus;
									}
									else if ("DEX".equalsIgnoreCase(statName))
									{
										DEX_BONUS[val] = bonus;
									}
									else if ("WIT".equalsIgnoreCase(statName))
									{
										WIT_BONUS[val] = bonus;
									}
									else if ("CON".equalsIgnoreCase(statName))
									{
										CON_BONUS[val] = bonus;
									}
									else if ("MEN".equalsIgnoreCase(statName))
									{
										MEN_BONUS[val] = bonus;
									}
									else if ("CHA".equalsIgnoreCase(statName))
									{
										CHA_BONUS[val] = bonus;
									}
									else
									{
										LOGGER.error("Invalid stats name: {}, skipping", statName);
									}
								}
							}
						}
					}
				}
			}
		}
		else
		{
			throw new Error("[BaseStats] File not found: " + file.getName());
		}
	}
}