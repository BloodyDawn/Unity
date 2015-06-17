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
package org.l2junity.gameserver.model.spawns;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.l2junity.commons.util.Rnd;
import org.l2junity.gameserver.GeoData;
import org.l2junity.gameserver.data.xml.impl.NpcData;
import org.l2junity.gameserver.datatables.SpawnTable;
import org.l2junity.gameserver.instancemanager.ZoneManager;
import org.l2junity.gameserver.model.L2Spawn;
import org.l2junity.gameserver.model.Location;
import org.l2junity.gameserver.model.StatsSet;
import org.l2junity.gameserver.model.actor.Creature;
import org.l2junity.gameserver.model.actor.Npc;
import org.l2junity.gameserver.model.actor.instance.L2MonsterInstance;
import org.l2junity.gameserver.model.actor.templates.L2NpcTemplate;
import org.l2junity.gameserver.model.holders.MinionHolder;
import org.l2junity.gameserver.model.interfaces.IParameterized;
import org.l2junity.gameserver.model.zone.type.NpcSpawnTerritory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author UnAfraid
 */
public class NpcSpawnTemplate implements IParameterized<StatsSet>
{
	private static final Logger LOGGER = LoggerFactory.getLogger(SpawnTemplate.class);
	
	private final int _npcId;
	private final int _count;
	private final Duration _respawnTime;
	private final Duration _respawnTimeRandom;
	private Location _location;
	private NpcSpawnTerritory _zone;
	private StatsSet _parameters;
	private List<MinionHolder> _minions;
	private final SpawnTemplate _spawnTemplate;
	private final SpawnGroup _group;
	private final Set<Npc> _spawns = ConcurrentHashMap.newKeySet();
	
	public NpcSpawnTemplate(SpawnTemplate spawnTemplate, SpawnGroup group, StatsSet set) throws Exception
	{
		_spawnTemplate = spawnTemplate;
		_group = group;
		_npcId = set.getInt("id");
		_count = set.getInt("count", 1);
		_respawnTime = set.getDuration("respawnTime", null);
		_respawnTimeRandom = set.getDuration("respawnRandom", null);
		
		final int x = set.getInt("x", Integer.MAX_VALUE);
		final int y = set.getInt("y", Integer.MAX_VALUE);
		final int z = set.getInt("z", Integer.MAX_VALUE);
		final boolean xDefined = x != Integer.MAX_VALUE;
		final boolean yDefined = y != Integer.MAX_VALUE;
		final boolean zDefined = z != Integer.MAX_VALUE;
		if (xDefined && yDefined && zDefined)
		{
			_location = new Location(x, y, z, set.getInt("heading", 0));
		}
		else
		{
			if (xDefined || yDefined || zDefined)
			{
				throw new IllegalStateException(String.format("Spawn with partially declared and x: %s y: %s z: %s!", processParam(x), processParam(y), processParam(z)));
			}
			
			final String zoneName = set.getString("zone", null);
			if (zoneName == null)
			{
				throw new NullPointerException("Spawn without zone and x y z!");
			}
			
			final NpcSpawnTerritory zone = ZoneManager.getInstance().getSpawnTerritory(zoneName);
			if (zone == null)
			{
				throw new NullPointerException("Spawn with non existing zone requested " + zoneName);
			}
			_zone = zone;
		}
	}
	
	public SpawnTemplate getSpawnTemplate()
	{
		return _spawnTemplate;
	}
	
	public SpawnGroup getGroup()
	{
		return _group;
	}
	
	private String processParam(int value)
	{
		return value != Integer.MAX_VALUE ? Integer.toString(value) : "undefined";
	}
	
	public int getNpcId()
	{
		return _npcId;
	}
	
	public int getCount()
	{
		return _count;
	}
	
	public Duration getRespawnTime()
	{
		return _respawnTime;
	}
	
	public Duration getRespawnTimeRandom()
	{
		return _respawnTimeRandom;
	}
	
	public Location getLocation()
	{
		return _location;
	}
	
	public NpcSpawnTerritory getZone()
	{
		return _zone;
	}
	
	@Override
	public StatsSet getParameters()
	{
		return _parameters;
	}
	
	@Override
	public void setParameters(StatsSet parameters)
	{
		_parameters = parameters;
	}
	
	public List<MinionHolder> getMinions()
	{
		return _minions != null ? _minions : Collections.emptyList();
	}
	
	public void addMinion(MinionHolder minion)
	{
		if (_minions == null)
		{
			_minions = new ArrayList<>();
		}
		_minions.add(minion);
	}
	
	public void spawn()
	{
		try
		{
			final L2NpcTemplate npcTemplate = NpcData.getInstance().getTemplate(_npcId);
			if (npcTemplate != null)
			{
				final L2Spawn spawn = new L2Spawn(npcTemplate);
				final int x, y, z, heading;
				if (_location != null)
				{
					x = _location.getX();
					y = _location.getY();
					z = GeoData.getInstance().getSpawnHeight(x, y, _location.getZ());
					heading = _location.getHeading();
				}
				else
				{
					final int[] spawnLoc = _zone.getRandomPoint();
					x = spawnLoc[0];
					y = spawnLoc[1];
					z = GeoData.getInstance().getSpawnHeight(x, y, spawnLoc[2]);
					heading = Rnd.get(65535);
				}
				
				spawn.setX(x);
				spawn.setY(y);
				spawn.setZ(z);
				spawn.setAmount(_count);
				spawn.setHeading(heading);
				int respawn = 0, respawnRandom = 0;
				if (_respawnTime != null)
				{
					respawn = (int) _respawnTime.getSeconds();
				}
				if (_respawnTimeRandom != null)
				{
					respawnRandom = (int) _respawnTimeRandom.getSeconds();
				}
				
				if (respawn > 0)
				{
					spawn.setRespawnDelay(respawn, respawnRandom);
					spawn.startRespawn();
				}
				else
				{
					spawn.stopRespawn();
				}
				
				spawn.setSpawnTemplate(this);
				for (int i = 0; i < spawn.getAmount(); i++)
				{
					final Npc npc = spawn.doSpawn();
					if (npc.isMonster() && (_minions != null))
					{
						((L2MonsterInstance) npc).getMinionList().spawnMinions(_minions);
					}
					_spawns.add(npc);
				}
			}
		}
		catch (Exception e)
		{
			LOGGER.warn("Couldn't spawn npc {}", _npcId, e);
		}
	}
	
	public void despawn()
	{
		_spawns.forEach(npc ->
		{
			SpawnTable.getInstance().deleteSpawn(npc.getSpawn(), false);
			npc.deleteMe();
		});
		_spawns.clear();
	}
	
	public void notifySpawnNpc(Npc npc)
	{
		_spawnTemplate.notifyEvent(event -> event.onSpawnNpc(_spawnTemplate, _group, npc));
	}
	
	public void notifyDespawnNpc(Npc npc)
	{
		_spawnTemplate.notifyEvent(event -> event.onSpawnDespawnNpc(_spawnTemplate, _group, npc));
	}
	
	public void notifyNpcDeath(Npc npc, Creature killer)
	{
		_spawnTemplate.notifyEvent(event -> event.onSpawnNpcDeath(_spawnTemplate, _group, npc, killer));
	}
}