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
package org.l2junity.gameserver.data.xml.impl;

import java.io.File;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.l2junity.Config;
import org.l2junity.gameserver.data.xml.IXmlReader;
import org.l2junity.gameserver.model.ChanceLocation;
import org.l2junity.gameserver.model.StatsSet;
import org.l2junity.gameserver.model.actor.templates.L2NpcTemplate;
import org.l2junity.gameserver.model.holders.MinionHolder;
import org.l2junity.gameserver.model.interfaces.IParameterized;
import org.l2junity.gameserver.model.spawns.NpcSpawnTemplate;
import org.l2junity.gameserver.model.spawns.SpawnGroup;
import org.l2junity.gameserver.model.spawns.SpawnTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

/**
 * @author UnAfraid
 */
public class SpawnsData implements IXmlReader
{
	protected static final Logger LOGGER = LoggerFactory.getLogger(SpawnsData.class);
	
	private final List<SpawnTemplate> _spawns = new LinkedList<>();
	
	protected SpawnsData()
	{
		load();
	}
	
	@Override
	public void load()
	{
		parseDatapackDirectory("data/spawns", true);
		LOGGER.info("Loaded: {} spawns", _spawns.stream().flatMap(c -> c.getGroups().stream()).flatMap(c -> c.getSpawns().stream()).count());
	}
	
	@Override
	public void parseDocument(Document doc, File f)
	{
		for (Node n = doc.getFirstChild(); n != null; n = n.getNextSibling())
		{
			if ("list".equalsIgnoreCase(n.getNodeName()))
			{
				for (Node d = n.getFirstChild(); d != null; d = d.getNextSibling())
				{
					if ("spawn".equalsIgnoreCase(d.getNodeName()))
					{
						try
						{
							parseSpawn(d, f);
						}
						catch (Exception e)
						{
							LOGGER.warn("Error while processing spawn in file: {}", f.getAbsolutePath(), e);
						}
					}
				}
			}
		}
	}
	
	/**
	 * Initializing all spawns
	 */
	public void init()
	{
		LOGGER.info("Initializing spawns...");
		_spawns.forEach(SpawnTemplate::spawnAll);
		LOGGER.info("All spawns has been initialized!");
	}
	
	/**
	 * Removing all spawns
	 */
	public void despawnAll()
	{
		LOGGER.info("Removing all spawns...");
		_spawns.forEach(SpawnTemplate::despawnAll);
		LOGGER.info("All spawns has been removed!");
	}
	
	public List<SpawnTemplate> getSpawns()
	{
		return _spawns;
	}
	
	private void parseSpawn(Node n, File f) throws Exception
	{
		final StatsSet set = new StatsSet();
		final NamedNodeMap attrs = n.getAttributes();
		for (int i = 0; i < attrs.getLength(); i++)
		{
			final Node node = attrs.item(i);
			set.set(node.getNodeName(), node.getNodeValue());
		}
		
		final SpawnTemplate spawnTemplate = new SpawnTemplate(set, f);
		SpawnGroup group = null;
		for (Node d = n.getFirstChild(); d != null; d = d.getNextSibling())
		{
			if ("group".equalsIgnoreCase(d.getNodeName()))
			{
				parseGroup(d, spawnTemplate);
			}
			else if ("npc".equalsIgnoreCase(d.getNodeName()))
			{
				if (group == null)
				{
					group = new SpawnGroup(StatsSet.EMPTY_STATSET);
				}
				parseNpc(d, spawnTemplate, group);
			}
			else if ("parameters".equalsIgnoreCase(d.getNodeName()))
			{
				parseParameters(n, spawnTemplate);
			}
		}
		
		// One static group for all npcs outside group scope
		if (group != null)
		{
			spawnTemplate.addGroup(group);
		}
		_spawns.add(spawnTemplate);
	}
	
	private void parseGroup(Node n, SpawnTemplate spawnTemplate) throws Exception
	{
		final StatsSet set = new StatsSet();
		final NamedNodeMap attrs = n.getAttributes();
		for (int i = 0; i < attrs.getLength(); i++)
		{
			final Node node = attrs.item(i);
			set.set(node.getNodeName(), node.getNodeValue());
		}
		
		final SpawnGroup group = new SpawnGroup(set);
		for (Node d = n.getFirstChild(); d != null; d = d.getNextSibling())
		{
			if ("npc".equalsIgnoreCase(d.getNodeName()))
			{
				parseNpc(d, spawnTemplate, group);
			}
		}
		spawnTemplate.addGroup(group);
	}
	
	/**
	 * @param n
	 * @param spawnTemplate
	 * @param group
	 * @throws Exception
	 */
	private void parseNpc(Node n, SpawnTemplate spawnTemplate, SpawnGroup group) throws Exception
	{
		final StatsSet set = new StatsSet();
		final NamedNodeMap attrs = n.getAttributes();
		for (int i = 0; i < attrs.getLength(); i++)
		{
			final Node node = attrs.item(i);
			set.set(node.getNodeName(), node.getNodeValue());
		}
		
		final NpcSpawnTemplate npcTemplate = new NpcSpawnTemplate(spawnTemplate, group, set);
		final L2NpcTemplate template = NpcData.getInstance().getTemplate(npcTemplate.getId());
		if (template == null)
		{
			LOGGER.warn("Requested spawn for non existing npc: ", npcTemplate.getId());
			return;
		}
		
		if (template.isType("L2Servitor") || template.isType("L2Pet"))
		{
			LOGGER.warn("Requested spawn for {} {}({}) file: {}", template.getType(), template.getName(), template.getId(), spawnTemplate.getFile().getName());
			return;
		}
		
		if (!Config.ALLOW_CLASS_MASTERS && template.isType("L2ClassMaster"))
		{
			// Don't spawn Class Masters unless config say so
			return;
		}
		
		for (Node d = n.getFirstChild(); d != null; d = d.getNextSibling())
		{
			if ("parameters".equalsIgnoreCase(d.getNodeName()))
			{
				parseParameters(d, npcTemplate);
			}
			else if ("minions".equalsIgnoreCase(d.getNodeName()))
			{
				parseMinions(d, npcTemplate);
			}
			else if ("locations".equalsIgnoreCase(d.getNodeName()))
			{
				parseLocations(d, npcTemplate);
			}
		}
		group.addSpawn(npcTemplate);
	}
	
	/**
	 * @param n
	 * @param npcTemplate
	 */
	private void parseLocations(Node n, NpcSpawnTemplate npcTemplate)
	{
		for (Node d = n.getFirstChild(); d != null; d = d.getNextSibling())
		{
			if ("location".equalsIgnoreCase(d.getNodeName()))
			{
				final int x = parseInteger(d.getAttributes(), "x");
				final int y = parseInteger(d.getAttributes(), "y");
				final int z = parseInteger(d.getAttributes(), "z");
				final int heading = parseInteger(d.getAttributes(), "heading", 0);
				final double chance = parseDouble(d.getAttributes(), "chance");
				npcTemplate.addSpawnLocation(new ChanceLocation(x, y, z, heading, chance));
			}
		}
	}
	
	/**
	 * @param n
	 * @param npcTemplate
	 */
	private void parseParameters(Node n, IParameterized<StatsSet> npcTemplate)
	{
		final Map<String, Object> params = parseParameters(n);
		npcTemplate.setParameters(!params.isEmpty() ? new StatsSet(Collections.unmodifiableMap(params)) : StatsSet.EMPTY_STATSET);
	}
	
	/**
	 * @param n
	 * @param npcTemplate
	 */
	private void parseMinions(Node n, NpcSpawnTemplate npcTemplate)
	{
		for (Node d = n.getFirstChild(); d != null; d = d.getNextSibling())
		{
			if ("minion".equalsIgnoreCase(d.getNodeName()))
			{
				final StatsSet set = new StatsSet();
				final NamedNodeMap attrs = n.getAttributes();
				for (int i = 0; i < attrs.getLength(); i++)
				{
					final Node node = attrs.item(i);
					set.set(node.getNodeName(), node.getNodeValue());
				}
				npcTemplate.addMinion(new MinionHolder(set));
			}
		}
	}
	
	/**
	 * Gets the single instance of SpawnsData.
	 * @return single instance of SpawnsData
	 */
	public static SpawnsData getInstance()
	{
		return SingletonHolder._instance;
	}
	
	private static class SingletonHolder
	{
		protected static final SpawnsData _instance = new SpawnsData();
	}
}
