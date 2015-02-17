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
package org.l2junity.gameserver.model;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;
import java.util.logging.Logger;

import javolution.util.FastList;

import org.l2junity.Config;
import org.l2junity.gameserver.ThreadPoolManager;
import org.l2junity.gameserver.datatables.SpawnTable;
import org.l2junity.gameserver.model.actor.Attackable;
import org.l2junity.gameserver.model.actor.Creature;
import org.l2junity.gameserver.model.actor.Npc;
import org.l2junity.gameserver.model.actor.Playable;
import org.l2junity.gameserver.model.actor.Vehicle;
import org.l2junity.gameserver.model.skills.Skill;
import org.l2junity.gameserver.model.zone.ZoneType;
import org.l2junity.gameserver.model.zone.type.PeaceZone;

public final class WorldRegion
{
	private static final Logger _log = Logger.getLogger(WorldRegion.class.getName());
	
	/** Map containing all playable characters in game in this world region. */
	private final Map<Integer, Playable> _allPlayable = new ConcurrentHashMap<>();
	
	/** Map containing visible objects in this world region. */
	private final Map<Integer, WorldObject> _visibleObjects = new ConcurrentHashMap<>();
	
	private final List<WorldRegion> _surroundingRegions;
	private final int _tileX, _tileY;
	private boolean _active = false;
	private ScheduledFuture<?> _neighborsTask = null;
	private final List<ZoneType> _zones = new FastList<>();
	
	public WorldRegion(int pTileX, int pTileY)
	{
		_surroundingRegions = new ArrayList<>();
		
		_tileX = pTileX;
		_tileY = pTileY;
		
		// default a newly initialized region to inactive, unless always on is specified
		_active = Config.GRIDS_ALWAYS_ON;
	}
	
	public List<ZoneType> getZones()
	{
		return _zones;
	}
	
	public void addZone(ZoneType zone)
	{
		_zones.add(zone);
	}
	
	public void removeZone(ZoneType zone)
	{
		_zones.remove(zone);
	}
	
	public void revalidateZones(Creature character)
	{
		// do NOT update the world region while the character is still in the process of teleporting
		// Once the teleport is COMPLETED, revalidation occurs safely, at that time.
		
		if (character.isTeleporting())
		{
			return;
		}
		
		for (ZoneType z : getZones())
		{
			if (z != null)
			{
				z.revalidateInZone(character);
			}
		}
	}
	
	public void removeFromZones(Creature character)
	{
		for (ZoneType z : getZones())
		{
			if (z != null)
			{
				z.removeCharacter(character);
			}
		}
	}
	
	public boolean containsZone(int zoneId)
	{
		for (ZoneType z : getZones())
		{
			if (z.getId() == zoneId)
			{
				return true;
			}
		}
		return false;
	}
	
	public boolean checkEffectRangeInsidePeaceZone(Skill skill, final int x, final int y, final int z)
	{
		final int range = skill.getEffectRange();
		final int up = y + range;
		final int down = y - range;
		final int left = x + range;
		final int right = x - range;
		
		for (ZoneType e : getZones())
		{
			if (e instanceof PeaceZone)
			{
				if (e.isInsideZone(x, up, z))
				{
					return false;
				}
				
				if (e.isInsideZone(x, down, z))
				{
					return false;
				}
				
				if (e.isInsideZone(left, y, z))
				{
					return false;
				}
				
				if (e.isInsideZone(right, y, z))
				{
					return false;
				}
				
				if (e.isInsideZone(x, y, z))
				{
					return false;
				}
			}
		}
		return true;
	}
	
	public void onDeath(Creature character)
	{
		for (ZoneType z : getZones())
		{
			if (z != null)
			{
				z.onDieInside(character);
			}
		}
	}
	
	public void onRevive(Creature character)
	{
		for (ZoneType z : getZones())
		{
			if (z != null)
			{
				z.onReviveInside(character);
			}
		}
	}
	
	/** Task of AI notification */
	public class NeighborsTask implements Runnable
	{
		private final boolean _isActivating;
		
		public NeighborsTask(boolean isActivating)
		{
			_isActivating = isActivating;
		}
		
		@Override
		public void run()
		{
			if (_isActivating)
			{
				// for each neighbor, if it's not active, activate.
				for (WorldRegion neighbor : getSurroundingRegions())
				{
					neighbor.setActive(true);
				}
			}
			else
			{
				if (areNeighborsEmpty())
				{
					setActive(false);
				}
				
				// check and deactivate
				for (WorldRegion neighbor : getSurroundingRegions())
				{
					if (neighbor.areNeighborsEmpty())
					{
						neighbor.setActive(false);
					}
				}
			}
		}
	}
	
	private void switchAI(boolean isOn)
	{
		int c = 0;
		if (!isOn)
		{
			Collection<WorldObject> vObj = _visibleObjects.values();
			for (WorldObject o : vObj)
			{
				if (o instanceof Attackable)
				{
					c++;
					Attackable mob = (Attackable) o;
					
					// Set target to null and cancel Attack or Cast
					mob.setTarget(null);
					
					// Stop movement
					mob.stopMove(null);
					
					// Stop all active skills effects in progress on the L2Character
					mob.stopAllEffects();
					
					mob.clearAggroList();
					mob.getAttackByList().clear();
					mob.getKnownList().removeAllKnownObjects();
					
					// stop the ai tasks
					if (mob.hasAI())
					{
						mob.getAI().setIntention(org.l2junity.gameserver.ai.CtrlIntention.AI_INTENTION_IDLE);
						mob.getAI().stopAITask();
					}
				}
				else if (o instanceof Vehicle)
				{
					c++;
					((Vehicle) o).getKnownList().removeAllKnownObjects();
				}
			}
			
			_log.fine(c + " mobs were turned off");
		}
		else
		{
			Collection<WorldObject> vObj = _visibleObjects.values();
			
			for (WorldObject o : vObj)
			{
				if (o instanceof Attackable)
				{
					c++;
					// Start HP/MP/CP Regeneration task
					((Attackable) o).getStatus().startHpMpRegeneration();
				}
				else if (o instanceof Npc)
				{
					((Npc) o).startRandomAnimationTimer();
				}
			}
			
			_log.fine(c + " mobs were turned on");
			
		}
		
	}
	
	public boolean isActive()
	{
		return _active;
	}
	
	// check if all 9 neighbors (including self) are inactive or active but with no players.
	// returns true if the above condition is met.
	public boolean areNeighborsEmpty()
	{
		// if this region is occupied, return false.
		if (isActive() && !_allPlayable.isEmpty())
		{
			return false;
		}
		
		// if any one of the neighbors is occupied, return false
		for (WorldRegion neighbor : _surroundingRegions)
		{
			if (neighbor.isActive() && !neighbor._allPlayable.isEmpty())
			{
				return false;
			}
		}
		
		// in all other cases, return true.
		return true;
	}
	
	/**
	 * this function turns this region's AI and geodata on or off
	 * @param value
	 */
	public void setActive(boolean value)
	{
		if (_active == value)
		{
			return;
		}
		
		_active = value;
		
		// turn the AI on or off to match the region's activation.
		switchAI(value);
		
		// TODO
		// turn the geodata on or off to match the region's activation.
		if (value)
		{
			_log.fine("Starting Grid " + _tileX + "," + _tileY);
		}
		else
		{
			_log.fine("Stoping Grid " + _tileX + "," + _tileY);
		}
	}
	
	/**
	 * Immediately sets self as active and starts a timer to set neighbors as active this timer is to avoid turning on neighbors in the case when a person just teleported into a region and then teleported out immediately...there is no reason to activate all the neighbors in that case.
	 */
	private void startActivation()
	{
		// first set self to active and do self-tasks...
		setActive(true);
		
		// if the timer to deactivate neighbors is running, cancel it.
		synchronized (this)
		{
			if (_neighborsTask != null)
			{
				_neighborsTask.cancel(true);
				_neighborsTask = null;
			}
			
			// then, set a timer to activate the neighbors
			_neighborsTask = ThreadPoolManager.getInstance().scheduleGeneral(new NeighborsTask(true), 1000 * Config.GRID_NEIGHBOR_TURNON_TIME);
		}
	}
	
	/**
	 * starts a timer to set neighbors (including self) as inactive this timer is to avoid turning off neighbors in the case when a person just moved out of a region that he may very soon return to. There is no reason to turn self & neighbors off in that case.
	 */
	private void startDeactivation()
	{
		// if the timer to activate neighbors is running, cancel it.
		synchronized (this)
		{
			if (_neighborsTask != null)
			{
				_neighborsTask.cancel(true);
				_neighborsTask = null;
			}
			
			// start a timer to "suggest" a deactivate to self and neighbors.
			// suggest means: first check if a neighbor has L2PcInstances in it. If not, deactivate.
			_neighborsTask = ThreadPoolManager.getInstance().scheduleGeneral(new NeighborsTask(false), 1000 * Config.GRID_NEIGHBOR_TURNOFF_TIME);
		}
	}
	
	/**
	 * Add the L2Object in the L2ObjectHashSet(L2Object) _visibleObjects containing L2Object visible in this L2WorldRegion <BR>
	 * If L2Object is a L2PcInstance, Add the L2PcInstance in the L2ObjectHashSet(L2PcInstance) _allPlayable containing L2PcInstance of all player in game in this L2WorldRegion <BR>
	 * Assert : object.getCurrentWorldRegion() == this
	 * @param object
	 */
	public void addVisibleObject(WorldObject object)
	{
		if (object == null)
		{
			return;
		}
		
		assert object.getWorldRegion() == this;
		
		_visibleObjects.put(object.getObjectId(), object);
		
		if (object instanceof Playable)
		{
			_allPlayable.put(object.getObjectId(), (Playable) object);
			
			// if this is the first player to enter the region, activate self & neighbors
			if ((_allPlayable.size() == 1) && (!Config.GRIDS_ALWAYS_ON))
			{
				startActivation();
			}
		}
	}
	
	/**
	 * Remove the L2Object from the L2ObjectHashSet(L2Object) _visibleObjects in this L2WorldRegion. If L2Object is a L2PcInstance, remove it from the L2ObjectHashSet(L2PcInstance) _allPlayable of this L2WorldRegion <BR>
	 * Assert : object.getCurrentWorldRegion() == this || object.getCurrentWorldRegion() == null
	 * @param object
	 */
	public void removeVisibleObject(WorldObject object)
	{
		if (object == null)
		{
			return;
		}
		
		assert (object.getWorldRegion() == this) || (object.getWorldRegion() == null);
		
		_visibleObjects.remove(object.getObjectId());
		
		if (object instanceof Playable)
		{
			_allPlayable.remove(object.getObjectId());
			
			if (_allPlayable.isEmpty() && !Config.GRIDS_ALWAYS_ON)
			{
				startDeactivation();
			}
		}
	}
	
	public void addSurroundingRegion(WorldRegion region)
	{
		_surroundingRegions.add(region);
	}
	
	/**
	 * @return the FastList _surroundingRegions containing all L2WorldRegion around the current L2WorldRegion
	 */
	public List<WorldRegion> getSurroundingRegions()
	{
		return _surroundingRegions;
	}
	
	public Map<Integer, Playable> getVisiblePlayable()
	{
		return _allPlayable;
	}
	
	public Map<Integer, WorldObject> getVisibleObjects()
	{
		return _visibleObjects;
	}
	
	public String getName()
	{
		return "(" + _tileX + ", " + _tileY + ")";
	}
	
	/**
	 * Deleted all spawns in the world.
	 */
	public void deleteVisibleNpcSpawns()
	{
		_log.fine("Deleting all visible NPC's in Region: " + getName());
		Collection<WorldObject> vNPC = _visibleObjects.values();
		for (WorldObject obj : vNPC)
		{
			if (obj instanceof Npc)
			{
				Npc target = (Npc) obj;
				target.deleteMe();
				L2Spawn spawn = target.getSpawn();
				if (spawn != null)
				{
					spawn.stopRespawn();
					SpawnTable.getInstance().deleteSpawn(spawn, false);
				}
				_log.finest("Removed NPC " + target.getObjectId());
			}
		}
		_log.info("All visible NPC's deleted in Region: " + getName());
	}
}
