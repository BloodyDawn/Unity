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

import java.util.Collection;
import java.util.concurrent.ScheduledFuture;

import org.l2junity.gameserver.ThreadPoolManager;
import org.l2junity.gameserver.ai.CtrlIntention;
import org.l2junity.gameserver.enums.InstanceType;
import org.l2junity.gameserver.instancemanager.WalkingManager;
import org.l2junity.gameserver.model.WorldObject;
import org.l2junity.gameserver.model.actor.Attackable;
import org.l2junity.gameserver.model.actor.Creature;
import org.l2junity.gameserver.model.actor.Npc;
import org.l2junity.gameserver.model.actor.instance.L2PcInstance;
import org.l2junity.gameserver.model.events.EventDispatcher;
import org.l2junity.gameserver.model.events.impl.character.npc.OnNpcCreatureSee;

public class NpcKnownList extends CharKnownList
{
	private ScheduledFuture<?> _trackingTask = null;
	
	public NpcKnownList(Npc activeChar)
	{
		super(activeChar);
	}
	
	@Override
	public boolean addKnownObject(WorldObject object)
	{
		if (!super.addKnownObject(object))
		{
			return false;
		}
		
		if (getActiveObject().isNpc() && (object instanceof Creature))
		{
			final Npc npc = (Npc) getActiveObject();
			
			// Notify to scripts
			EventDispatcher.getInstance().notifyEventAsync(new OnNpcCreatureSee(npc, (Creature) object, object.isSummon()), npc);
		}
		return true;
	}
	
	@Override
	public Npc getActiveChar()
	{
		return (Npc) super.getActiveChar();
	}
	
	@Override
	public int getDistanceToForgetObject(WorldObject object)
	{
		return 2 * getDistanceToWatchObject(object);
	}
	
	@Override
	public int getDistanceToWatchObject(WorldObject object)
	{
		if (!(object instanceof Creature))
		{
			return 0;
		}
		
		if (object.isPlayable())
		{
			return 1500;
		}
		
		return 500;
	}
	
	// Support for Walking monsters aggro
	public void startTrackingTask()
	{
		if ((_trackingTask == null) && (getActiveChar().getAggroRange() > 0))
		{
			_trackingTask = ThreadPoolManager.getInstance().scheduleAiAtFixedRate(new TrackingTask(), 2000, 2000);
		}
	}
	
	// Support for Walking monsters aggro
	public void stopTrackingTask()
	{
		if (_trackingTask != null)
		{
			_trackingTask.cancel(true);
			_trackingTask = null;
		}
	}
	
	// Support for Walking monsters aggro
	protected class TrackingTask implements Runnable
	{
		@Override
		public void run()
		{
			if (getActiveChar() instanceof Attackable)
			{
				final Attackable monster = (Attackable) getActiveChar();
				if (monster.getAI().getIntention() == CtrlIntention.AI_INTENTION_MOVE_TO)
				{
					final Collection<L2PcInstance> players = getKnownPlayers().values();
					if (players != null)
					{
						for (L2PcInstance pl : players)
						{
							if (!pl.isDead() && !pl.isInvul() && pl.isInsideRadius(monster, monster.getAggroRange(), true, false) && (monster.isMonster() || (monster.isInstanceTypes(InstanceType.L2GuardInstance) && (pl.getKarma() > 0))))
							{
								// Send aggroRangeEnter
								if (monster.getHating(pl) == 0)
								{
									monster.addDamageHate(pl, 0, 0);
								}
								
								// Skip attack for other targets, if one is already chosen for attack
								if ((monster.getAI().getIntention() != CtrlIntention.AI_INTENTION_ATTACK) && !monster.isCoreAIDisabled())
								{
									WalkingManager.getInstance().stopMoving(getActiveChar(), false, true);
									monster.addDamageHate(pl, 0, 100);
									monster.getAI().setIntention(CtrlIntention.AI_INTENTION_ATTACK, pl, null);
								}
							}
						}
					}
				}
			}
		}
	}
}
