/*
 * Copyright (C) 2004-2015 L2J DataPack
 * 
 * This file is part of L2J DataPack.
 * 
 * L2J DataPack is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * L2J DataPack is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package handlers.effecthandlers;

import org.l2junity.gameserver.datatables.SkillData;
import org.l2junity.gameserver.handler.ITargetTypeHandler;
import org.l2junity.gameserver.handler.TargetHandler;
import org.l2junity.gameserver.model.StatsSet;
import org.l2junity.gameserver.model.WorldObject;
import org.l2junity.gameserver.model.actor.Creature;
import org.l2junity.gameserver.model.conditions.Condition;
import org.l2junity.gameserver.model.effects.AbstractEffect;
import org.l2junity.gameserver.model.events.EventType;
import org.l2junity.gameserver.model.events.impl.character.OnCreatureDamageDealt;
import org.l2junity.gameserver.model.events.impl.character.OnCreatureSkillFinishCast;
import org.l2junity.gameserver.model.events.listeners.ConsumerEventListener;
import org.l2junity.gameserver.model.skills.BuffInfo;
import org.l2junity.gameserver.model.skills.Skill;
import org.l2junity.gameserver.model.skills.targets.L2TargetType;

/**
 * Skill that canceles certain effect when offense is initiated.
 * @author Nik
 */
public final class CancelEffectOnOffense extends AbstractEffect
{
	private int _cancelSkillId;
	private L2TargetType _targetType;
	private Skill _skill;
	
	/**
	 * @param attachCond
	 * @param applyCond
	 * @param set
	 * @param params
	 */
	
	public CancelEffectOnOffense(Condition attachCond, Condition applyCond, StatsSet set, StatsSet params)
	{
		super(attachCond, applyCond, set, params);
		
		_cancelSkillId = params.getInt("cancelSkillId", 0);
		_targetType = params.getEnum("targetType", L2TargetType.class, L2TargetType.NONE);
	}
	
	@Override
	public void onStart(BuffInfo info)
	{
		// Default buff cancel is this.
		if (_cancelSkillId <= 0)
		{
			_cancelSkillId = info.getSkill().getId();
		}
		
		if (SkillData.getInstance().getSkill(_cancelSkillId, 1) == null)
		{
			return;
		}
		
		_skill = info.getSkill();
		
		// Default target type is this skill's target type.
		if (_targetType == L2TargetType.NONE)
		{
			_targetType = info.getSkill().getTargetType();
		}
		
		info.getEffected().addListener(new ConsumerEventListener(info.getEffected(), EventType.ON_CREATURE_DAMAGE_DEALT, (OnCreatureDamageDealt event) -> onOffenseEvent(event, null), this));
		info.getEffected().addListener(new ConsumerEventListener(info.getEffected(), EventType.ON_CREATURE_SKILL_FINISH_CAST, (OnCreatureSkillFinishCast event) -> onOffenseEvent(null, event), this));
	}
	
	@Override
	public void onExit(BuffInfo info)
	{
		info.getEffected().removeListenerIf(EventType.ON_CREATURE_DAMAGE_DEALT, listener -> listener.getOwner() == this);
		info.getEffected().removeListenerIf(EventType.ON_CREATURE_SKILL_FINISH_CAST, listener -> listener.getOwner() == this);
	}
	
	public void onOffenseEvent(OnCreatureDamageDealt event, OnCreatureSkillFinishCast castEvent)
	{
		final Creature attacker = event != null ? event.getAttacker() : castEvent != null ? castEvent.getCaster() : null;
		final Creature target = event != null ? event.getTarget() : castEvent != null ? castEvent.getTarget() : null;
		
		if ((attacker == null) || (target == null) || (attacker == target))
		{
			return;
		}
		
		// On Attack or offensive skill
		if ((castEvent != null) && (castEvent.isSimultaneously() || !castEvent.getSkill().isBad()))
		{
			return;
		}
		
		final ITargetTypeHandler targetHandler = TargetHandler.getInstance().getHandler(_targetType);
		if (targetHandler == null)
		{
			_log.warn("Handler for target type: " + _targetType + " does not exist.");
			return;
		}
		
		for (WorldObject affected : targetHandler.getTargetList(_skill, attacker, false, target))
		{
			if (!affected.isCreature())
			{
				return;
			}
			
			((Creature) affected).getEffectList().stopSkillEffects(true, _cancelSkillId);
		}
	}
}
