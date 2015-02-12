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
package org.l2junity.gameserver.model.actor.knownlist;

import org.l2junity.gameserver.model.WorldObject;

public class NullKnownList extends ObjectKnownList
{
	public NullKnownList(WorldObject activeObject)
	{
		super(activeObject);
	}
	
	@Override
	public boolean addKnownObject(WorldObject object)
	{
		return false;
	}
	
	@Override
	public WorldObject getActiveObject()
	{
		return super.getActiveObject();
	}
	
	@Override
	public int getDistanceToForgetObject(WorldObject object)
	{
		return 0;
	}
	
	@Override
	public int getDistanceToWatchObject(WorldObject object)
	{
		return 0;
	}
	
	@Override
	public void removeAllKnownObjects()
	{
	}
	
	@Override
	protected boolean removeKnownObject(WorldObject object, boolean forget)
	{
		return false;
	}
}