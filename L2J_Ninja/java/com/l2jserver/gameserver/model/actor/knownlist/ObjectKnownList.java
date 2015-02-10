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
package com.l2jserver.gameserver.model.actor.knownlist;

import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.l2jserver.gameserver.model.WorldObject;
import com.l2jserver.gameserver.model.WorldRegion;
import com.l2jserver.gameserver.model.actor.Creature;
import com.l2jserver.gameserver.model.actor.Playable;
import com.l2jserver.gameserver.util.Util;

public class ObjectKnownList
{
	private final WorldObject _activeObject;
	private Map<Integer, WorldObject> _knownObjects;
	
	public ObjectKnownList(WorldObject activeObject)
	{
		_activeObject = activeObject;
	}
	
	public boolean addKnownObject(WorldObject object)
	{
		if (object == null)
		{
			return false;
		}
		
		// Instance -1 is for GMs that can see everything on all instances
		if ((getActiveObject().getInstanceId() != -1) && (object.getInstanceId() != getActiveObject().getInstanceId()))
		{
			return false;
		}
		
		// Check if the object is an L2PcInstance in ghost mode
		if (object.isPlayer() && object.getActingPlayer().getAppearance().isGhost())
		{
			return false;
		}
		
		// Check if already know object
		if (knowsObject(object))
		{
			return false;
		}
		
		// Check if object is not inside distance to watch object
		if (!Util.checkIfInShortRadius(getDistanceToWatchObject(object), getActiveObject(), object, true))
		{
			return false;
		}
		
		return (getKnownObjects().put(object.getObjectId(), object) == null);
	}
	
	public final boolean knowsObject(WorldObject object)
	{
		if (object == null)
		{
			return false;
		}
		
		return (getActiveObject() == object) || getKnownObjects().containsKey(object.getObjectId());
	}
	
	/**
	 * Remove all L2Object from _knownObjects
	 */
	public void removeAllKnownObjects()
	{
		getKnownObjects().clear();
	}
	
	public final boolean removeKnownObject(WorldObject object)
	{
		return removeKnownObject(object, false);
	}
	
	protected boolean removeKnownObject(WorldObject object, boolean forget)
	{
		if (object == null)
		{
			return false;
		}
		
		if (forget)
		{
			return true;
		}
		
		return getKnownObjects().remove(object.getObjectId()) != null;
	}
	
	/**
	 * Used only in Config.MOVE_BASED_KNOWNLIST and does not support guards seeing moving monsters
	 */
	public final void findObjects()
	{
		final WorldRegion region = getActiveObject().getWorldRegion();
		if (region == null)
		{
			return;
		}
		
		if (getActiveObject().isPlayable())
		{
			for (WorldRegion regi : region.getSurroundingRegions()) // offer members of this and surrounding regions
			{
				Collection<WorldObject> vObj = regi.getVisibleObjects().values();
				for (WorldObject object : vObj)
				{
					if (object != getActiveObject())
					{
						addKnownObject(object);
						if (object instanceof Creature)
						{
							object.getKnownList().addKnownObject(getActiveObject());
						}
					}
				}
			}
		}
		else if (getActiveObject() instanceof Creature)
		{
			for (WorldRegion regi : region.getSurroundingRegions()) // offer members of this and surrounding regions
			{
				if (regi.isActive())
				{
					Collection<Playable> vPls = regi.getVisiblePlayable().values();
					for (WorldObject object : vPls)
					{
						if (object != getActiveObject())
						{
							addKnownObject(object);
						}
					}
				}
			}
		}
	}
	
	/**
	 * Remove invisible and too far L2Object from _knowObject and if necessary from _knownPlayers of the L2Character
	 * @param fullCheck
	 */
	public void forgetObjects(boolean fullCheck)
	{
		// Go through knownObjects
		final Collection<WorldObject> objs = getKnownObjects().values();
		final Iterator<WorldObject> oIter = objs.iterator();
		WorldObject object;
		while (oIter.hasNext())
		{
			object = oIter.next();
			if (object == null)
			{
				oIter.remove();
				continue;
			}
			
			if (!fullCheck && !object.isPlayable())
			{
				continue;
			}
			
			// Remove all objects invisible or too far
			if (!object.isVisible() || !Util.checkIfInShortRadius(getDistanceToForgetObject(object), getActiveObject(), object, true))
			{
				oIter.remove();
				removeKnownObject(object, true);
			}
		}
	}
	
	public WorldObject getActiveObject()
	{
		return _activeObject;
	}
	
	public int getDistanceToForgetObject(WorldObject object)
	{
		return 0;
	}
	
	public int getDistanceToWatchObject(WorldObject object)
	{
		return 0;
	}
	
	/**
	 * @return the _knownObjects containing all L2Object known by the L2Character.
	 */
	public final Map<Integer, WorldObject> getKnownObjects()
	{
		if (_knownObjects == null)
		{
			_knownObjects = new ConcurrentHashMap<>();
		}
		return _knownObjects;
	}
}
