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
package org.l2junity.gameserver.data.xml.impl;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Stream;

import org.l2junity.gameserver.data.xml.IGameXmlReader;
import org.l2junity.gameserver.datatables.ItemTable;
import org.l2junity.gameserver.model.ArmorSet;
import org.l2junity.gameserver.model.holders.ArmorsetSkillHolder;
import org.l2junity.gameserver.model.items.L2Item;
import org.l2junity.gameserver.model.stats.Stats;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

/**
 * Loads armor set bonuses.
 * @author godson, Luno, UnAfraid
 */
public final class ArmorSetsData implements IGameXmlReader
{
	private static final Logger LOGGER = LoggerFactory.getLogger(ArmorSetsData.class);
	
	private final Map<Integer, ArmorSet> _armorSets = new HashMap<>();
	private final Map<Integer, List<ArmorSet>> _armorSetItems = new HashMap<>();
	
	protected ArmorSetsData()
	{
		load();
	}
	
	@Override
	public void load()
	{
		_armorSets.clear();
		parseDatapackDirectory("data/stats/armorsets", false);
		LOGGER.info("Loaded {} Armor sets.", _armorSets.size());
	}
	
	@Override
	public void parseDocument(Document doc, File f)
	{
		for (Node n = doc.getFirstChild(); n != null; n = n.getNextSibling())
		{
			if ("list".equalsIgnoreCase(n.getNodeName()))
			{
				for (Node setNode = n.getFirstChild(); setNode != null; setNode = setNode.getNextSibling())
				{
					if ("set".equalsIgnoreCase(setNode.getNodeName()))
					{
						final int id = parseInteger(setNode.getAttributes(), "id");
						final int minimumPieces = parseInteger(setNode.getAttributes(), "minimumPieces", 0);
						final boolean isVisual = parseBoolean(setNode.getAttributes(), "visual", false);
						final ArmorSet set = new ArmorSet(id, minimumPieces, isVisual);
						if (_armorSets.putIfAbsent(id, set) != null)
						{
							LOGGER.warn("Duplicate set entry with id: {} in file: {}", id, f.getName());
						}
						for (Node innerSetNode = setNode.getFirstChild(); innerSetNode != null; innerSetNode = innerSetNode.getNextSibling())
						{
							switch (innerSetNode.getNodeName())
							{
								case "requiredItems":
								{
									readAhead(innerSetNode, b -> "item".equals(b.getNodeName()), attrs ->
									{
										final int itemId = parseInteger(attrs, "id");
										final L2Item item = ItemTable.getInstance().getTemplate(itemId);
										if (item == null)
										{
											LOGGER.warn("Attempting to register non existing required item: {} to a set: {}", itemId, f.getName());
										}
										else if (!set.addRequiredItem(itemId))
										{
											LOGGER.warn("Attempting to register duplicate required item {} to a set: {}", item, f.getName());
										}
									});
									break;
								}
								case "optionalItems":
								{
									readAhead(innerSetNode, b -> "item".equals(b.getNodeName()), attrs ->
									{
										final int itemId = parseInteger(attrs, "id");
										final L2Item item = ItemTable.getInstance().getTemplate(itemId);
										if (item == null)
										{
											LOGGER.warn("Attempting to register non existing optional item: {} to a set: {}", itemId, f.getName());
										}
										else if (!set.addOptionalItem(itemId))
										{
											LOGGER.warn("Attempting to register duplicate optional item {} to a set: {}", item, f.getName());
										}
									});
									break;
								}
								case "skills":
								{
									readAhead(innerSetNode, b -> "skill".equals(b.getNodeName()), attrs ->
									{
										final int skillId = parseInteger(attrs, "id");
										final int skillLevel = parseInteger(attrs, "level");
										final int minPieces = parseInteger(attrs, "minimumPieces", set.getMinimumPieces());
										final int minEnchant = parseInteger(attrs, "minimumEnchant", 0);
										final boolean isOptional = parseBoolean(attrs, "optional", false);
										set.addSkill(new ArmorsetSkillHolder(skillId, skillLevel, minPieces, minEnchant, isOptional));
									});
									break;
								}
								case "stats":
								{
									readAhead(innerSetNode, b -> "stat".equals(b.getNodeName()), attrs ->
									{
										final String stat = parseString(attrs, "type");
										set.addStatsBonus(Stats.valueOfXml(stat), parseInteger(attrs, "val"));
									});
									break;
								}
							}
						}
						
						Stream.concat(set.getRequiredItems().stream(), set.getOptionalItems().stream()).forEach(itemHolder -> _armorSetItems.computeIfAbsent(itemHolder, key -> new ArrayList<>()).add(set));
					}
				}
			}
		}
	}
	
	/**
	 * Reads the next elements and applies consumer's code if matches the filter's conditions
	 * @param n
	 * @param filter
	 * @param consumer
	 */
	private void readAhead(Node n, Predicate<Node> filter, Consumer<NamedNodeMap> consumer)
	{
		for (Node b = n.getFirstChild(); b != null; b = b.getNextSibling())
		{
			if (filter.test(b))
			{
				consumer.accept(b.getAttributes());
			}
		}
	}
	
	/**
	 * @param setId the set id that is attached to a set
	 * @return the armor set associated to the given item id
	 */
	public ArmorSet getSet(int setId)
	{
		return _armorSets.get(setId);
	}
	
	/**
	 * @param itemId the item id that is attached to a set
	 * @return the armor set associated to the given item id
	 */
	public List<ArmorSet> getSets(int itemId)
	{
		return _armorSetItems.getOrDefault(itemId, Collections.emptyList());
	}
	
	/**
	 * Gets the single instance of ArmorSetsData
	 * @return single instance of ArmorSetsData
	 */
	public static ArmorSetsData getInstance()
	{
		return SingletonHolder._instance;
	}
	
	private static class SingletonHolder
	{
		protected static final ArmorSetsData _instance = new ArmorSetsData();
	}
}
