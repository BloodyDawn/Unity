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
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.l2junity.gameserver.data.xml.IGameXmlReader;
import org.l2junity.gameserver.instancemanager.MapRegionManager;
import org.l2junity.gameserver.model.StatsSet;
import org.l2junity.gameserver.model.actor.instance.DoorInstance;
import org.l2junity.gameserver.model.actor.templates.DoorTemplate;
import org.l2junity.gameserver.model.instancezone.Instance;
import org.l2junity.gameserver.pathfinding.AbstractNodeLoc;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

/**
 * This class loads and hold info about doors.
 * @author JIV, GodKratos, UnAfraid
 */
public final class DoorData implements IGameXmlReader
{
	// Info holders
	private final Map<String, Set<Integer>> _groups = new HashMap<>();
	private final Map<Integer, DoorInstance> _doors = new HashMap<>();
	private final Map<Integer, DoorTemplate> _templates = new HashMap<>();
	private final Map<Integer, List<DoorInstance>> _regions = new HashMap<>();
	// Logger
	private static final Logger LOGGER = LoggerFactory.getLogger(DoorData.class);
	
	protected DoorData()
	{
		load();
	}
	
	@Override
	public void load()
	{
		_doors.clear();
		_groups.clear();
		_regions.clear();
		parseDatapackFile("data/DoorData.xml");
	}
	
	@Override
	public void parseDocument(Document doc, File f)
	{
		for (Node listNode = doc.getFirstChild(); listNode != null; listNode = listNode.getNextSibling())
		{
			if ("list".equals(listNode.getNodeName()))
			{
				for (Node doorNode = listNode.getFirstChild(); doorNode != null; doorNode = doorNode.getNextSibling())
				{
					if ("door".equals(doorNode.getNodeName()))
					{
						final StatsSet params = new StatsSet();
						params.set("baseHpMax", 1); // Avoid doors without HP value created dead due to default value 0 in L2CharTemplate
						NamedNodeMap attrs = doorNode.getAttributes();
						for (int i = 0; i < attrs.getLength(); i++)
						{
							final Node att = attrs.item(i);
							params.set(att.getNodeName(), att.getNodeValue());
						}
						
						for (Node doorNode2 = doorNode.getFirstChild(); doorNode2 != null; doorNode2 = doorNode2.getNextSibling())
						{
							attrs = doorNode2.getAttributes();
							if (doorNode2.getNodeName().equals("nodes"))
							{
								params.set("nodeZ", parseInteger(attrs, "nodeZ"));
								int count = 0;
								
								for (Node nodes = doorNode2.getFirstChild(); nodes != null; nodes = nodes.getNextSibling())
								{
									final NamedNodeMap np = nodes.getAttributes();
									if ("node".equals(nodes.getNodeName()))
									{
										params.set("nodeX_" + count, parseInteger(np, "x"));
										params.set("nodeY_" + count, parseInteger(np, "y"));
										count++;
									}
								}
							}
							else
							{
								if (attrs != null)
								{
									for (int i = 0; i < attrs.getLength(); i++)
									{
										final Node att = attrs.item(i);
										params.set(att.getNodeName(), att.getNodeValue());
									}
								}
							}
						}
						makeDoor(params);
					}
				}
			}
		}
		
		LOGGER.info("Loaded {} Door Templates for {} regions.", _doors.size(), _regions.size());
	}
	
	/**
	 * @param set
	 */
	private void makeDoor(StatsSet set)
	{
		// Insert Collision data
		int posX, posY, nodeX, nodeY, height;
		height = set.getInt("height", 150);
		nodeX = set.getInt("nodeX_0");
		nodeY = set.getInt("nodeY_0");
		posX = set.getInt("nodeX_1");
		posY = set.getInt("nodeX_1");
		int collisionRadius; // (max) radius for movement checks
		collisionRadius = Math.min(Math.abs(nodeX - posX), Math.abs(nodeY - posY));
		if (collisionRadius < 20)
		{
			collisionRadius = 20;
		}
		set.set("collision_radius", collisionRadius);
		set.set("collision_height", height);
		// Create door template + door instance
		final DoorTemplate template = new DoorTemplate(set);
		final DoorInstance door = new DoorInstance(template);
		door.setCurrentHp(door.getMaxHp());
		door.spawnMe(template.getX(), template.getY(), template.getZ());
		_templates.put(door.getId(), template);
		if (template.getGroupName() != null)
		{
			_groups.computeIfAbsent(template.getGroupName(), key -> new HashSet<>()).add(template.getId());
		}
		
		putDoor(door, MapRegionManager.getInstance().getMapRegionLocId(door));
	}
	
	public DoorTemplate getDoorTemplate(int doorId)
	{
		return _templates.get(doorId);
	}
	
	public DoorInstance getDoor(int doorId)
	{
		return _doors.get(doorId);
	}
	
	public void putDoor(DoorInstance door, int region)
	{
		_doors.put(door.getId(), door);
		
		if (!_regions.containsKey(region))
		{
			_regions.put(region, new ArrayList<DoorInstance>());
		}
		_regions.get(region).add(door);
	}
	
	public Set<Integer> getDoorsByGroup(String groupName)
	{
		return _groups.get(groupName);
	}
	
	public Collection<DoorInstance> getDoors()
	{
		return _doors.values();
	}
	
	public boolean checkIfDoorsBetween(AbstractNodeLoc start, AbstractNodeLoc end, Instance instance)
	{
		return checkIfDoorsBetween(start.getX(), start.getY(), start.getZ(), end.getX(), end.getY(), end.getZ(), instance);
	}
	
	public boolean checkIfDoorsBetween(int x, int y, int z, int tx, int ty, int tz, Instance instance)
	{
		return checkIfDoorsBetween(x, y, z, tx, ty, tz, instance, false);
	}
	
	/**
	 * GodKratos: TODO: remove GeoData checks from door table and convert door nodes to Geo zones
	 * @param x
	 * @param y
	 * @param z
	 * @param tx
	 * @param ty
	 * @param tz
	 * @param instance
	 * @param doubleFaceCheck
	 * @return {@code boolean}
	 */
	public boolean checkIfDoorsBetween(int x, int y, int z, int tx, int ty, int tz, Instance instance, boolean doubleFaceCheck)
	{
		final Collection<DoorInstance> allDoors = (instance != null) ? instance.getDoors() : _regions.get(MapRegionManager.getInstance().getMapRegionLocId(x, y));
		if (allDoors == null)
		{
			return false;
		}
		
		for (DoorInstance doorInst : allDoors)
		{
			// check dead and open
			if (doorInst.isDead() || doorInst.isOpen() || !doorInst.checkCollision() || (doorInst.getX(0) == 0))
			{
				continue;
			}
			
			boolean intersectFace = false;
			for (int i = 0; i < 4; i++)
			{
				int j = (i + 1) < 4 ? i + 1 : 0;
				// lower part of the multiplier fraction, if it is 0 we avoid an error and also know that the lines are parallel
				int denominator = ((ty - y) * (doorInst.getX(i) - doorInst.getX(j))) - ((tx - x) * (doorInst.getY(i) - doorInst.getY(j)));
				if (denominator == 0)
				{
					continue;
				}
				
				// multipliers to the equations of the lines. If they are lower than 0 or bigger than 1, we know that segments don't intersect
				float multiplier1 = (float) (((doorInst.getX(j) - doorInst.getX(i)) * (y - doorInst.getY(i))) - ((doorInst.getY(j) - doorInst.getY(i)) * (x - doorInst.getX(i)))) / denominator;
				float multiplier2 = (float) (((tx - x) * (y - doorInst.getY(i))) - ((ty - y) * (x - doorInst.getX(i)))) / denominator;
				if ((multiplier1 >= 0) && (multiplier1 <= 1) && (multiplier2 >= 0) && (multiplier2 <= 1))
				{
					int intersectZ = Math.round(z + (multiplier1 * (tz - z)));
					// now checking if the resulting point is between door's min and max z
					if ((intersectZ > doorInst.getZMin()) && (intersectZ < doorInst.getZMax()))
					{
						if (!doubleFaceCheck || intersectFace)
						{
							return true;
						}
						intersectFace = true;
					}
				}
			}
		}
		return false;
	}
	
	public static DoorData getInstance()
	{
		return SingletonHolder._instance;
	}
	
	private static class SingletonHolder
	{
		protected static final DoorData _instance = new DoorData();
	}
}
