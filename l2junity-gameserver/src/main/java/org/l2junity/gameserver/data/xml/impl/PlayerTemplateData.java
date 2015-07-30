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
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;

import org.l2junity.gameserver.data.xml.IGameXmlReader;
import org.l2junity.gameserver.model.Location;
import org.l2junity.gameserver.model.StatsSet;
import org.l2junity.gameserver.model.actor.templates.L2PcTemplate;
import org.l2junity.gameserver.model.base.ClassId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

/**
 * Loads player's base stats.
 * @author Forsaiken, Zoey76, GKR
 */
public final class PlayerTemplateData implements IGameXmlReader
{
	private static final Logger LOGGER = LoggerFactory.getLogger(PlayerTemplateData.class);
	
	private final Map<ClassId, L2PcTemplate> _playerTemplates = new EnumMap<>(ClassId.class);
	private final Map<ClassId, StatsSet> _templates = new EnumMap<>(ClassId.class);
	
	private int _dataCount = 0;
	
	protected PlayerTemplateData()
	{
		load();
	}
	
	@Override
	public void load()
	{
		_playerTemplates.clear();
		parseDatapackDirectory("data/stats/chars/baseStats", false);
		initializeTemplates();
		LOGGER.info("Loaded {} character templates.", _playerTemplates.size());
		LOGGER.info("Loaded {} level up gain records.", _dataCount);
	}
	
	@Override
	public void parseDocument(Document doc, File f)
	{
		NamedNodeMap attrs;
		for (Node n = doc.getFirstChild(); n != null; n = n.getNextSibling())
		{
			if ("list".equalsIgnoreCase(n.getNodeName()))
			{
				int classId = 0;
				for (Node d = n.getFirstChild(); d != null; d = d.getNextSibling())
				{
					if ("classId".equalsIgnoreCase(d.getNodeName()))
					{
						classId = Integer.parseInt(d.getTextContent());
					}
					else if ("staticData".equalsIgnoreCase(d.getNodeName()))
					{
						final StatsSet set = new StatsSet();
						set.set("classId", classId);
						final List<Location> creationPoints = new ArrayList<>();
						
						for (Node nd = d.getFirstChild(); nd != null; nd = nd.getNextSibling())
						{
							if (nd.getNodeType() != Node.ELEMENT_NODE)
							{
								continue;
							}
							
							if (nd.getChildNodes().getLength() > 1)
							{
								for (Node cnd = nd.getFirstChild(); cnd != null; cnd = cnd.getNextSibling())
								{
									if (cnd.getNodeType() != Node.ELEMENT_NODE)
									{
										continue;
									}
									
									// use L2CharTemplate(superclass) fields for male collision height and collision radius
									if (nd.getNodeName().equalsIgnoreCase("collisionMale"))
									{
										if (cnd.getNodeName().equalsIgnoreCase("radius"))
										{
											set.set("collision_radius", cnd.getTextContent());
										}
										else if (cnd.getNodeName().equalsIgnoreCase("height"))
										{
											set.set("collision_height", cnd.getTextContent());
										}
									}
									if ("node".equalsIgnoreCase(cnd.getNodeName()))
									{
										attrs = cnd.getAttributes();
										creationPoints.add(new Location(parseInteger(attrs, "x"), parseInteger(attrs, "y"), parseInteger(attrs, "z")));
									}
									else if ("walk".equalsIgnoreCase(cnd.getNodeName()))
									{
										set.set("baseWalkSpd", cnd.getTextContent());
									}
									else if ("run".equalsIgnoreCase(cnd.getNodeName()))
									{
										set.set("baseRunSpd", cnd.getTextContent());
									}
									else if ("slowSwim".equals(cnd.getNodeName()))
									{
										set.set("baseSwimWalkSpd", cnd.getTextContent());
									}
									else if ("fastSwim".equals(cnd.getNodeName()))
									{
										set.set("baseSwimRunSpd", cnd.getTextContent());
									}
									else
									{
										set.set(nd.getNodeName() + cnd.getNodeName(), cnd.getTextContent());
									}
								}
							}
							else
							{
								set.set(nd.getNodeName(), nd.getTextContent());
							}
						}
						// calculate total pdef and mdef from parts
						set.set("basePDef", (set.getInt("basePDefchest", 0) + set.getInt("basePDeflegs", 0) + set.getInt("basePDefhead", 0) + set.getInt("basePDeffeet", 0) + set.getInt("basePDefgloves", 0) + set.getInt("basePDefunderwear", 0) + set.getInt("basePDefcloak", 0)));
						set.set("baseMDef", (set.getInt("baseMDefrear", 0) + set.getInt("baseMDeflear", 0) + set.getInt("baseMDefrfinger", 0) + set.getInt("baseMDefrfinger", 0) + set.getInt("baseMDefneck", 0)));
						
						set.set("creationPoints", creationPoints);
						_templates.put(ClassId.getClassId(classId), set);
					}
					else if ("lvlUpgainData".equalsIgnoreCase(d.getNodeName()))
					{
						final LevelUpHolder holder = new LevelUpHolder();
						final StatsSet set = _templates.get(ClassId.getClassId(classId));
						if (set == null)
						{
							LOGGER.warn("Couldn't find basic player template data but found levelUpagainData!");
							continue;
						}
						
						if (set.getSet().containsKey("levelUpData"))
						{
							LOGGER.warn("Player template data contains two times levelUpagainData!");
						}
						
						set.set("levelUpData", holder);
						for (Node lvlNode = d.getFirstChild(); lvlNode != null; lvlNode = lvlNode.getNextSibling())
						{
							if ("level".equalsIgnoreCase(lvlNode.getNodeName()))
							{
								attrs = lvlNode.getAttributes();
								final int level = parseInteger(attrs, "val");
								
								for (Node valNode = lvlNode.getFirstChild(); valNode != null; valNode = valNode.getNextSibling())
								{
									String nodeName = valNode.getNodeName();
									
									if ((nodeName.startsWith("hp") || nodeName.startsWith("mp") || nodeName.startsWith("cp")))
									{
										holder.addLevelData(level, new LevelUpData(nodeName, Double.parseDouble(valNode.getTextContent())));
										_dataCount++;
									}
								}
							}
						}
					}
				}
			}
		}
	}
	
	private void initializeTemplates()
	{
		for (Entry<ClassId, StatsSet> entry : _templates.entrySet())
		{
			final ClassId classId = entry.getKey();
			final StatsSet set = entry.getValue();
			
			if (classId.getParent() != null)
			{
				final StatsSet newSet = new StatsSet();
				final Set<ClassId> parents = new HashSet<>();
				ClassId parent = classId;
				while ((parent = parent.getParent()) != null)
				{
					parents.add(parent);
				}
				
				parents.stream().sorted(Comparator.comparingInt(ClassId::ordinal)).forEach(id ->
				{
					final StatsSet parentSet = _templates.get(id);
					if (parentSet == null)
					{
						LOGGER.warn("Missing template {}", id);
						return;
					}
					newSet.merge(_templates.get(id));
				});
				initializeTemplate(classId, newSet);
			}
			else
			{
				initializeTemplate(classId, set);
			}
		}
		_templates.clear();
	}
	
	private void initializeTemplate(ClassId classId, StatsSet set)
	{
		final List<Location> creationPoints = set.getList("creationPoints", Location.class);
		final LevelUpHolder holder = set.getObject("levelUpData", LevelUpHolder.class);
		final L2PcTemplate template = new L2PcTemplate(set, creationPoints);
		
		// Apply level up again data
		if (holder != null)
		{
			for (Entry<Integer, List<LevelUpData>> entry : holder.getLevelUpData().entrySet())
			{
				for (LevelUpData data : entry.getValue())
				{
					template.setUpgainValue(data.getName(), entry.getKey(), data.getValue());
				}
			}
		}
		_playerTemplates.put(classId, template);
	}
	
	class LevelUpHolder
	{
		private final Map<Integer, List<LevelUpData>> _levelUpData = new TreeMap<>();
		
		public void addLevelData(int level, LevelUpData data)
		{
			_levelUpData.computeIfAbsent(level, key -> new ArrayList<>()).add(data);
		}
		
		public Map<Integer, List<LevelUpData>> getLevelUpData()
		{
			return _levelUpData;
		}
	}
	
	class LevelUpData
	{
		private final String _name;
		private final double _value;
		
		public LevelUpData(String name, double value)
		{
			_name = name;
			_value = value;
		}
		
		public String getName()
		{
			return _name;
		}
		
		public double getValue()
		{
			return _value;
		}
	}
	
	public L2PcTemplate getTemplate(ClassId classId)
	{
		return _playerTemplates.get(classId);
	}
	
	public L2PcTemplate getTemplate(int classId)
	{
		return _playerTemplates.get(ClassId.getClassId(classId));
	}
	
	public static final PlayerTemplateData getInstance()
	{
		return SingletonHolder._instance;
	}
	
	private static class SingletonHolder
	{
		protected static final PlayerTemplateData _instance = new PlayerTemplateData();
	}
}
