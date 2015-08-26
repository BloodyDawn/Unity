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
package org.l2junity.gameserver.instancemanager;

import java.io.File;
import java.lang.reflect.Constructor;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.time.DayOfWeek;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;

import org.l2junity.Config;
import org.l2junity.DatabaseFactory;
import org.l2junity.gameserver.data.xml.IGameXmlReader;
import org.l2junity.gameserver.enums.InstanceReenterType;
import org.l2junity.gameserver.enums.InstanceRemoveBuffType;
import org.l2junity.gameserver.enums.InstanceTeleportType;
import org.l2junity.gameserver.model.Location;
import org.l2junity.gameserver.model.StatsSet;
import org.l2junity.gameserver.model.actor.Creature;
import org.l2junity.gameserver.model.actor.instance.PlayerInstance;
import org.l2junity.gameserver.model.holders.InstanceReenterTimeHolder;
import org.l2junity.gameserver.model.holders.SpawnHolder;
import org.l2junity.gameserver.model.instancezone.Instance;
import org.l2junity.gameserver.model.instancezone.InstanceTemplate;
import org.l2junity.gameserver.model.instancezone.conditions.Condition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

/**
 * Instance manager.
 * @author evill33t, GodKratos, malyelfik
 */
public final class InstanceManager implements IGameXmlReader
{
	private static final Logger LOGGER = LoggerFactory.getLogger(InstanceManager.class);
	// Database query
	private static final String DELETE_INSTANCE_TIME = "DELETE FROM character_instance_time WHERE charId=? AND instanceId=?";
	
	// Client instance names
	private final Map<Integer, String> _instanceNames = new HashMap<>();
	// Instance templates holder
	private final Map<Integer, InstanceTemplate> _instanceTemplates = new HashMap<>();
	// Created instance worlds
	private int _currentInstanceId = 0;
	private final Map<Integer, Instance> _instanceWorlds = new ConcurrentHashMap<>();
	// Player reenter times
	private final Map<Integer, Map<Integer, Long>> _playerInstanceTimes = new ConcurrentHashMap<>();
	
	protected InstanceManager()
	{
		load();
	}
	
	// --------------------------------------------------------------------
	// Instance data loader
	// --------------------------------------------------------------------
	
	@Override
	public void load()
	{
		// Load instance names
		_instanceNames.clear();
		parseDatapackFile("data/instancenames.xml");
		LOGGER.info("Loaded {} instance names.", _instanceNames.size());
		// Load instance templates
		_instanceTemplates.clear();
		parseDatapackDirectory("data/instances", true);
		LOGGER.info("Loaded {} instance templates.", _instanceTemplates.size());
		// Load player's reenter data
		_playerInstanceTimes.clear();
		restoreInstanceTimes();
		LOGGER.info("Loaded instance reenter times for {} players.", _playerInstanceTimes.size());
	}
	
	@Override
	public void parseDocument(Document doc, File f)
	{
		for (Node n = doc.getFirstChild(); n != null; n = n.getNextSibling())
		{
			final String nodeName = n.getNodeName();
			if (nodeName.equals("list"))
			{
				parseInstanceName(n);
			}
			else if (nodeName.equals("instance"))
			{
				parseInstanceTemplate(n, f);
			}
		}
	}
	
	/**
	 * Read instance names from XML file.
	 * @param n starting XML tag
	 */
	private void parseInstanceName(Node n)
	{
		for (Node d = n.getFirstChild(); d != null; d = d.getNextSibling())
		{
			if (d.getNodeName().equals("instance"))
			{
				final NamedNodeMap attrs = d.getAttributes();
				_instanceNames.put(parseInteger(attrs, "id"), parseString(attrs, "name"));
			}
		}
	}
	
	/**
	 * Parse instance template from XML file.
	 * @param n start XML tag
	 * @param file currently parsed file
	 */
	private void parseInstanceTemplate(Node n, File file)
	{
		final InstanceTemplate template = new InstanceTemplate();
		
		// Parse "instance" node
		NamedNodeMap attrs = n.getAttributes();
		final int id = parseInteger(attrs, "id");
		if (_instanceTemplates.containsKey(id))
		{
			LOGGER.warn("Instance template with ID {} already exists", id);
			return;
		}
		
		template.setId(id);
		template.setName(parseString(attrs, "name", _instanceNames.get(id)));
		template.setMaxWorlds(parseInteger(attrs, "maxWorlds", -1));
		
		// Parse "instance" node children
		for (Node d = n.getFirstChild(); d != null; d = d.getNextSibling())
		{
			switch (d.getNodeName())
			{
				case "time":
				{
					attrs = d.getAttributes();
					template.setDuration(parseInteger(attrs, "duration", -1));
					template.setEmptyDestroyTime(parseInteger(attrs, "empty", -1));
					template.setEjectTime(parseInteger(attrs, "eject", -1));
					break;
				}
				case "misc":
				{
					attrs = d.getAttributes();
					template.allowPlayerSummon(parseBoolean(attrs, "allowPlayerSummon", false));
					template.setIsPvP(parseBoolean(attrs, "isPvP", false));
					break;
				}
				case "locations":
				{
					for (Node e = d.getFirstChild(); e != null; e = e.getNextSibling())
					{
						if (e.getNodeName().equals("enter"))
						{
							final InstanceTeleportType type = parseEnum(e.getAttributes(), InstanceTeleportType.class, "type");
							final List<Location> locations = new ArrayList<>();
							for (Node f = e.getFirstChild(); f != null; f = f.getNextSibling())
							{
								if (f.getNodeName().equals("location"))
								{
									locations.add(parseLocation(f));
								}
							}
							template.setEnterLocation(type, locations);
						}
						else if (e.getNodeName().equals("exit"))
						{
							final InstanceTeleportType type = parseEnum(e.getAttributes(), InstanceTeleportType.class, "type");
							if (type.equals(InstanceTeleportType.ORIGIN))
							{
								template.setExitLocation(type, null);
							}
							else
							{
								final List<Location> locations = new ArrayList<>();
								for (Node f = e.getFirstChild(); f != null; f = f.getNextSibling())
								{
									if (f.getNodeName().equals("location"))
									{
										locations.add(parseLocation(f));
									}
								}
								if (locations.isEmpty())
								{
									LOGGER.warn("Missing exit location data for instance {} ({})!", template.getName(), template.getId());
								}
								else
								{
									template.setExitLocation(type, locations);
								}
							}
						}
					}
					break;
				}
				case "spawnlist":
				{
					for (Node e = d.getFirstChild(); e != null; e = e.getNextSibling())
					{
						if (e.getNodeName().equals("group"))
						{
							final String groupName = parseString(e.getAttributes(), "name");
							final List<SpawnHolder> group = new ArrayList<>();
							for (Node f = e.getFirstChild(); f != null; f = f.getNextSibling())
							{
								if (f.getNodeName().equals("spawn"))
								{
									attrs = f.getAttributes();
									final int npcId = parseInteger(attrs, "npcId");
									final int respawn = parseInteger(attrs, "respawnDelay", 0);
									final Location spawnLoc = parseLocation(f);
									group.add(new SpawnHolder(npcId, spawnLoc, respawn));
								}
							}
							template.addSpawnGroup(groupName, group);
						}
					}
					break;
				}
				case "doorlist":
				{
					for (Node e = d.getFirstChild(); e != null; e = e.getNextSibling())
					{
						if (e.getNodeName().equals("door"))
						{
							attrs = e.getAttributes();
							final int doorId = parseInteger(attrs, "id");
							final Boolean open = parseBoolean(attrs, "open", null); // Let's use some magic - null means default (door template value), true open and false close
							template.addDoor(doorId, open);
						}
					}
					break;
				}
				case "removeBuffs":
				{
					final InstanceRemoveBuffType removeBuffType = parseEnum(d.getAttributes(), InstanceRemoveBuffType.class, "type");
					final List<Integer> exceptionBuffList = new ArrayList<>();
					for (Node e = d.getFirstChild(); e != null; e = e.getNextSibling())
					{
						if (e.getNodeName().equals("skill"))
						{
							exceptionBuffList.add(parseInteger(e.getAttributes(), "id"));
						}
					}
					template.setRemoveBuff(removeBuffType, exceptionBuffList);
					break;
				}
				case "reenter":
				{
					final InstanceReenterType type = parseEnum(d.getAttributes(), InstanceReenterType.class, "apply", InstanceReenterType.NONE);
					final List<InstanceReenterTimeHolder> data = new ArrayList<>();
					for (Node e = d.getFirstChild(); e != null; e = e.getNextSibling())
					{
						if (e.getNodeName().equals("reset"))
						{
							attrs = e.getAttributes();
							final int time = parseInteger(attrs, "time", -1);
							if (time > 0)
							{
								data.add(new InstanceReenterTimeHolder(time));
							}
							else
							{
								final DayOfWeek day = parseEnum(attrs, DayOfWeek.class, "day");
								final int hour = parseInteger(attrs, "hour", -1);
								final int minute = parseInteger(attrs, "minute", -1);
								data.add(new InstanceReenterTimeHolder(day, hour, minute));
							}
						}
					}
					template.setReenterData(type, data);
					break;
				}
				case "parameters":
					template.setParameters(parseParameters(d));
					break;
				case "conditions":
				{
					final List<Condition> conditions = new ArrayList<>();
					for (Node e = d.getFirstChild(); e != null; e = e.getNextSibling())
					{
						if (e.getNodeName().equals("condition"))
						{
							attrs = e.getAttributes();
							final String type = parseString(attrs, "type");
							final boolean onlyLeader = parseBoolean(attrs, "onlyLeader", false);
							// Load parameters
							StatsSet params = null;
							for (Node f = e.getFirstChild(); f != null; f = f.getNextSibling())
							{
								if (f.getNodeName().equals("param"))
								{
									if (params == null)
									{
										params = new StatsSet();
									}
									
									attrs = f.getAttributes();
									params.set(parseString(attrs, "name"), parseString(attrs, "value"));
								}
							}
							
							// If none parameters found then set empty StatSet
							if (params == null)
							{
								params = StatsSet.EMPTY_STATSET;
							}
							
							// Now when everything is loaded register condition to template
							try
							{
								final Class<?> clazz = Class.forName("org.l2junity.gameserver.model.instancezone.conditions.Condition" + type);
								final Constructor<?> constructor = clazz.getConstructor(InstanceTemplate.class, StatsSet.class, boolean.class);
								conditions.add((Condition) constructor.newInstance(template, params, onlyLeader));
							}
							catch (Exception ex)
							{
								LOGGER.warn("Unknown condition type {} for instance {} ({})!", type, template.getName(), id);
							}
						}
					}
					template.setConditions(conditions);
					break;
				}
			}
		}
		
		// Save template
		_instanceTemplates.put(id, template);
	}
	
	// --------------------------------------------------------------------
	// Instance data loader - END
	// --------------------------------------------------------------------
	
	/**
	 * Create new instance with default template.
	 * @return newly created default instance.
	 */
	public Instance createInstance()
	{
		return new Instance(getNewInstanceId(), new InstanceTemplate());
	}
	
	/**
	 * Create new instance from given template.
	 * @param template template used for instance creation
	 * @return newly created instance if success, otherwise {@code null}
	 */
	public Instance createInstance(InstanceTemplate template)
	{
		return (template != null) ? new Instance(getNewInstanceId(), template) : null;
	}
	
	/**
	 * Create new instance with template defined in datapack.
	 * @param id template id of instance
	 * @return newly created instance if template was found, otherwise {@code null}
	 */
	public Instance createInstance(int id)
	{
		if (!_instanceTemplates.containsKey(id))
		{
			LOGGER.warn("Missing template for instance with id {}!", id);
			return null;
		}
		return new Instance(getNewInstanceId(), _instanceTemplates.get(id));
	}
	
	/**
	 * Get instance world with given ID.
	 * @param instanceId ID of instance
	 * @return instance itself if found, otherwise {@code null}
	 */
	public Instance getInstance(int instanceId)
	{
		return _instanceWorlds.get(instanceId);
	}
	
	/**
	 * Get all active instances.
	 * @return Collection of all instances
	 */
	public Collection<Instance> getInstances()
	{
		return _instanceWorlds.values();
	}
	
	/**
	 * Get instance world for given creature.<br>
	 * <i>For player instance use {@link InstanceManager#getPlayerInstance(PlayerInstance, boolean)}.</i>
	 * @param creature creature inside instance
	 * @return instance world if found, otherwise {@code null}
	 */
	public Instance getInstance(Creature creature)
	{
		return _instanceWorlds.get(creature.getInstanceId());
	}
	
	/**
	 * Get instance world for player.
	 * @param player player who wants to get instance world
	 * @param isInside when {@code true} find world where player is currently located, otherwise find world where player can enter
	 * @return instance if found, otherwise {@code null}
	 */
	public Instance getPlayerInstance(PlayerInstance player, boolean isInside)
	{
		return _instanceWorlds.values().stream().filter(i -> (isInside) ? i.containsPlayer(player) : i.isAllowed(player)).findFirst().orElse(null);
	}
	
	/**
	 * Get ID for newly created instance.
	 * @return instance id
	 */
	private synchronized int getNewInstanceId()
	{
		do
		{
			if (_currentInstanceId == Integer.MAX_VALUE)
			{
				if (Config.DEBUG_INSTANCES)
				{
					LOGGER.info("Instance id owerflow, starting from zero.");
				}
				_currentInstanceId = 0;
			}
			_currentInstanceId++;
		}
		while (_instanceWorlds.containsKey(_currentInstanceId));
		return _currentInstanceId;
	}
	
	/**
	 * Register instance world.<br>
	 * @param instance instance which should be registered
	 */
	public void register(Instance instance)
	{
		final int instanceId = instance.getId();
		if (!_instanceWorlds.containsKey(instanceId))
		{
			_instanceWorlds.put(instanceId, instance);
		}
	}
	
	/**
	 * Unregister instance world.<br>
	 * <b><font color=red>To remove instance world properly use {@link Instance#destroy()}.</font></b>
	 * @param instanceId ID of instance to unregister
	 */
	public void unregister(int instanceId)
	{
		if (_instanceWorlds.containsKey(instanceId))
		{
			_instanceWorlds.remove(instanceId);
		}
	}
	
	/**
	 * Get instance name from file "instancenames.xml"
	 * @param templateId template ID of instance
	 * @return name of instance if found, otherwise {@code null}
	 */
	public String getInstanceName(int templateId)
	{
		return _instanceNames.get(templateId);
	}
	
	/**
	 * Restore instance reenter data for all players.
	 */
	private void restoreInstanceTimes()
	{
		try (Connection con = DatabaseFactory.getInstance().getConnection();
			Statement ps = con.createStatement();
			ResultSet rs = ps.executeQuery("SELECT * FROM character_instance_time ORDER BY charId"))
		{
			// Prepare variables
			int charId = -1;
			Map<Integer, Long> playerData = new ConcurrentHashMap<>();
			// Start reading database
			while (rs.next())
			{
				// If previous charId is different store data for previous player and start reading new one
				final int currCharId = rs.getInt("charId");
				if ((charId != currCharId) && (charId != -1))
				{
					if (!playerData.isEmpty())
					{
						_playerInstanceTimes.put(charId, playerData);
						playerData = new ConcurrentHashMap<>();
					}
					charId = currCharId;
				}
				
				// If time for instance penalty is lower then current time skip it
				final long time = rs.getLong("time");
				if (time > System.currentTimeMillis())
				{
					playerData.put(rs.getInt("instanceId"), time);
				}
			}
			// Store data for last read player
			if ((charId != -1) && !playerData.isEmpty())
			{
				_playerInstanceTimes.put(charId, playerData);
			}
		}
		catch (Exception e)
		{
			LOGGER.warn("Cannot restore players instance reenter data: ", e);
		}
	}
	
	/**
	 * Get all instance re-enter times for specified player.<br>
	 * This method also removes the penalties that have already expired.
	 * @param player instance of player who wants to get re-enter data
	 * @return map in form templateId, penaltyEndTime
	 */
	public Map<Integer, Long> getAllInstanceTimes(PlayerInstance player)
	{
		// When player don't have any instance penalty
		final Map<Integer, Long> instanceTimes = _playerInstanceTimes.get(player.getObjectId());
		if ((instanceTimes == null) || instanceTimes.isEmpty())
		{
			return Collections.emptyMap();
		}
		
		// Find passed penalty
		final List<Integer> invalidPenalty = new ArrayList<>(instanceTimes.size());
		for (Entry<Integer, Long> entry : instanceTimes.entrySet())
		{
			if (entry.getValue() <= System.currentTimeMillis())
			{
				invalidPenalty.add(entry.getKey());
			}
		}
		
		// Remove them
		if (!invalidPenalty.isEmpty())
		{
			try (Connection con = DatabaseFactory.getInstance().getConnection();
				PreparedStatement ps = con.prepareStatement(DELETE_INSTANCE_TIME))
			{
				for (Integer id : invalidPenalty)
				{
					ps.setInt(1, player.getObjectId());
					ps.setInt(2, id);
					ps.addBatch();
				}
				ps.executeBatch();
				invalidPenalty.forEach(instanceTimes::remove);
			}
			catch (Exception e)
			{
				LOGGER.warn("Cannot delete instance character reenter data: ", e);
			}
		}
		return instanceTimes;
	}
	
	/**
	 * Set re-enter penalty for specified player.<br>
	 * <font color=red><b>This method store penalty into memory only. Use {@link Instance#setReenterTime} to set instance penalty properly.</b></font>
	 * @param objectId object ID of player
	 * @param id instance template id
	 * @param time penalty time
	 */
	public void setReenterPenalty(int objectId, int id, long time)
	{
		Map<Integer, Long> playerData = _playerInstanceTimes.get(objectId);
		if (playerData == null)
		{
			playerData = new ConcurrentHashMap<>();
			_playerInstanceTimes.put(objectId, playerData);
		}
		playerData.put(id, time);
	}
	
	/**
	 * Get re-enter time to instance (by template ID) for player.<br>
	 * This method also removes penalty if expired.
	 * @param player player who wants to get re-enter time
	 * @param id template ID of instance
	 * @return penalty end time if penalty is found, otherwise -1
	 */
	public long getInstanceTime(PlayerInstance player, int id)
	{
		// Check if exists reenter data for player
		final Map<Integer, Long> playerData = _playerInstanceTimes.get(player.getObjectId());
		if ((playerData == null) || !playerData.containsKey(id))
		{
			return -1;
		}
		
		// If reenter time is higher then current, delete it
		final long time = playerData.get(id);
		if (time <= System.currentTimeMillis())
		{
			deleteInstanceTime(player, id);
			return -1;
		}
		return time;
	}
	
	/**
	 * Remove re-enter penalty for specified instance from player.
	 * @param player player who wants to delete penalty
	 * @param id template id of instance world
	 */
	public void deleteInstanceTime(PlayerInstance player, int id)
	{
		try (Connection con = DatabaseFactory.getInstance().getConnection();
			PreparedStatement ps = con.prepareStatement(DELETE_INSTANCE_TIME))
		{
			ps.setInt(1, player.getObjectId());
			ps.setInt(2, id);
			ps.execute();
			_playerInstanceTimes.get(player.getObjectId()).remove(id);
		}
		catch (Exception e)
		{
			LOGGER.warn("Could not delete character instance reenter data: ", e);
		}
	}
	
	/**
	 * Get instance template by template ID.
	 * @param id template id of instance
	 * @return instance template if found, otherwise {@code null}
	 */
	public InstanceTemplate getInstanceTemplate(int id)
	{
		return _instanceTemplates.get(id);
	}
	
	/**
	 * Get all instances template.
	 * @return Collection of all instance templates
	 */
	public Collection<InstanceTemplate> getInstanceTemplates()
	{
		return _instanceTemplates.values();
	}
	
	/**
	 * Get count of created instance worlds with same template ID.
	 * @param templateId template id of instance
	 * @return count of created instances
	 */
	public long getWorldCount(int templateId)
	{
		return _instanceWorlds.values().stream().filter(i -> i.getTemplateId() == templateId).count();
	}
	
	/**
	 * Gets the single instance of {@code InstanceManager}.
	 * @return single instance of {@code InstanceManager}
	 */
	public static InstanceManager getInstance()
	{
		return SingletonHolder._instance;
	}
	
	private static class SingletonHolder
	{
		protected static final InstanceManager _instance = new InstanceManager();
	}
}
