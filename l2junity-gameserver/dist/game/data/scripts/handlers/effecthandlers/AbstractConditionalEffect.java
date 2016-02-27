/*
 * Copyright (C) 2004-2016 L2J Unity
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
package handlers.effecthandlers;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

import org.l2junity.gameserver.model.StatsSet;
import org.l2junity.gameserver.model.actor.Creature;
import org.l2junity.gameserver.model.skills.BuffInfo;
import org.l2junity.gameserver.model.skills.Skill;
import org.l2junity.gameserver.model.stats.Stats;

/**
 * @author UnAfraid
 */
public abstract class AbstractConditionalEffect extends AbstractStatEffect
{
	private final Map<Integer, EffectedConditionHolder> _holders = new ConcurrentHashMap<>();
	
	protected AbstractConditionalEffect(StatsSet params, Stats stat)
	{
		super(params, stat);
	}
	
	@Override
	public final void onStart(Creature effector, Creature effected, Skill skill)
	{
		// Augmentation option
		if (skill == null)
		{
			return;
		}
		
		final EffectedConditionHolder oldHolder = _holders.putIfAbsent(effected.getObjectId(), new EffectedConditionHolder(effector, effected, skill, canPump(effector, effected, skill)));
		if (oldHolder != null)
		{
			_log.warn("Duplicate effect condition holder old effected: {} old skill: {},  new effected: {} new skill: {}", oldHolder.getEffected(), oldHolder.getSkill(), effected, skill, new IllegalStateException());
		}
		else
		{
			// Register listeners
			registerCondition(effector, effected, skill);
		}
	}
	
	@Override
	public final void onExit(BuffInfo info)
	{
		// Augmentation option
		if (info.getSkill() == null)
		{
			return;
		}
		
		unregisterCondition(info.getEffector(), info.getEffected(), info.getSkill());
		final EffectedConditionHolder oldHolder = _holders.remove(info.getEffected().getObjectId());
		if (oldHolder == null)
		{
			_log.warn("Failed onExit condition holder new effected: {} new skill: {}", info.getEffected(), info.getSkill(), new IllegalStateException());
		}
	}
	
	protected final EffectedConditionHolder getHolder(int objectId)
	{
		return _holders.get(objectId);
	}
	
	protected abstract void registerCondition(Creature effector, Creature effected, Skill skill);
	
	protected abstract void unregisterCondition(Creature effector, Creature effected, Skill skill);
	
	protected final void update(int objectId)
	{
		final EffectedConditionHolder holder = _holders.get(objectId);
		if (holder == null)
		{
			_log.warn("Effected condition holder is null!", new NullPointerException());
			return;
		}
		
		final boolean condStatus = canPump(holder.getEffector(), holder.getEffected(), holder.getSkill());
		if (holder.getLastConditionStatus().compareAndSet(!condStatus, condStatus))
		{
			holder.getEffected().getStat().recalculateStats(true);
		}
	}
	
	protected static class EffectedConditionHolder
	{
		private final Creature _effector;
		private final Creature _effected;
		private final Skill _skill;
		private final AtomicBoolean _lastConditionStatus = new AtomicBoolean();
		
		public EffectedConditionHolder(Creature effector, Creature effected, Skill skill, boolean condStatus)
		{
			_effector = effector;
			_effected = effected;
			_skill = skill;
			_lastConditionStatus.set(condStatus);
		}
		
		/**
		 * Gets the character that launched the buff.
		 * @return the effector
		 */
		public Creature getEffector()
		{
			return _effector;
		}
		
		/**
		 * Gets the target of the skill.
		 * @return the effected
		 */
		public Creature getEffected()
		{
			return _effected;
		}
		
		/**
		 * Gets the skill that created this buff info.
		 * @return the skill
		 */
		public Skill getSkill()
		{
			return _skill;
		}
		
		public AtomicBoolean getLastConditionStatus()
		{
			return _lastConditionStatus;
		}
	}
}
