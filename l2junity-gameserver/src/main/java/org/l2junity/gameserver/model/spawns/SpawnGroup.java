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

import java.util.ArrayList;
import java.util.List;

import org.l2junity.gameserver.model.StatsSet;

/**
 * @author UnAfraid
 */
public class SpawnGroup
{
	private final String _name;
	private final boolean _spawnByDefault;
	private final List<NpcSpawnTemplate> _spawns = new ArrayList<>();
	
	public SpawnGroup(StatsSet set)
	{
		_name = set.getString("name", null);
		_spawnByDefault = set.getBoolean("spawnByDefault", true);
	}
	
	public String getName()
	{
		return _name;
	}
	
	public boolean isSpawningByDefault()
	{
		return _spawnByDefault;
	}
	
	public void addSpawn(NpcSpawnTemplate template)
	{
		_spawns.add(template);
	}
	
	public List<NpcSpawnTemplate> getSpawns()
	{
		return _spawns;
	}
	
	public void spawnAll()
	{
		_spawns.forEach(NpcSpawnTemplate::spawn);
	}
	
	public void despawnAll()
	{
		_spawns.forEach(NpcSpawnTemplate::despawn);
	}
}
