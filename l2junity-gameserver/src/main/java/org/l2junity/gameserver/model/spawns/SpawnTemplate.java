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

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import org.l2junity.gameserver.instancemanager.QuestManager;
import org.l2junity.gameserver.model.StatsSet;
import org.l2junity.gameserver.model.interfaces.IParameterized;
import org.l2junity.gameserver.model.quest.Quest;

/**
 * @author UnAfraid
 */
public class SpawnTemplate implements IParameterized<StatsSet>
{
	private final String _name;
	private final String _ai;
	private final File _file;
	private final List<SpawnGroup> _groups = new ArrayList<>();
	private StatsSet _parameters;
	
	public SpawnTemplate(StatsSet set, File f)
	{
		_name = set.getString("name", null);
		_ai = set.getString("ai", null);
		_file = f;
	}
	
	public String getName()
	{
		return _name;
	}
	
	public String getAI()
	{
		return _ai;
	}
	
	public File getFile()
	{
		return _file;
	}
	
	public void addGroup(SpawnGroup group)
	{
		_groups.add(group);
	}
	
	public List<SpawnGroup> getGroups()
	{
		return _groups;
	}
	
	public List<SpawnGroup> getGroupsByName(String name)
	{
		return _groups.stream().filter(group -> String.valueOf(group.getName()).equalsIgnoreCase(name)).collect(Collectors.toList());
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
	
	public void notifyEvent(Consumer<Quest> event)
	{
		if (_ai != null)
		{
			final Quest script = QuestManager.getInstance().getScripts().get(_ai);
			if (script != null)
			{
				event.accept(script);
			}
		}
	}
	
	public void spawnAll()
	{
		_groups.stream().filter(SpawnGroup::isSpawningByDefault).forEach(SpawnGroup::spawnAll);
		notifyEvent(script -> script.onSpawnActivate(this));
	}
	
	public void despawnAll()
	{
		_groups.forEach(SpawnGroup::despawnAll);
		notifyEvent(script -> script.onSpawnDeactivate(this));
	}
}
