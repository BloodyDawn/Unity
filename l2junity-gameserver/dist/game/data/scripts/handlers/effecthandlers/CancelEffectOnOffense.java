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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Skill that canceles certain effect when offense is initiated.
 * @author Nik
 */
public final class CancelEffectOnOffense extends AbstractEffect
{
	private static final Logger LOGGER = LoggerFactory.getLogger(CancelEffectOnOffense.class);

	private final int _cancelSkillId;
	private final L2TargetType _targetType;
	
	/**
	 * @param attachCond
	 * @param applyCond
	 * @param set
	 * @param params
	 */
	public CancelEffectOnOffense(Condition attachCond, Condition applyCond, StatsSet set, StatsSet params)
	{
		super(attachCond, applyCond, set, params);
		
		_cancelSkillId = params.getInt("cancelSkillId", set.getInt("id"));
		_targetType = params.getEnum("targetType", L2TargetType.class, null);
	}

	@Override
	public void onStart(Creature effector, Creature effected, Skill skill)
	{
		final Skill cancelSkill = SkillData.getInstance().getSkill(_cancelSkillId, 1);
		if(cancelSkill == null)
		{
			LOGGER.warn("Cancel skill {} does not exist from skill:{}", _cancelSkillId, skill);
			return;
		}

		final L2TargetType targetType = _targetType != L2TargetType.NONE ? _targetType : cancelSkill.getTargetType();
		final ITargetTypeHandler targetHandler = TargetHandler.getInstance().getHandler(targetType);
		if (targetHandler == null)
		{
			LOGGER.warn("Handler for target type: {} does not exist.", targetType);
			return;
		}

		effected.addListener(new ConsumerEventListener(effected, EventType.ON_CREATURE_DAMAGE_DEALT, (OnCreatureDamageDealt event) -> onOffenseEvent(event, null, skill, targetHandler), this));
		effected.addListener(new ConsumerEventListener(effected, EventType.ON_CREATURE_SKILL_FINISH_CAST, (OnCreatureSkillFinishCast event) -> onOffenseEvent(null, event, skill, targetHandler), this));
	}
	
	@Override
	public void onExit(BuffInfo info)
	{
		info.getEffected().removeListenerIf(EventType.ON_CREATURE_DAMAGE_DEALT, listener -> listener.getOwner() == this);
		info.getEffected().removeListenerIf(EventType.ON_CREATURE_SKILL_FINISH_CAST, listener -> listener.getOwner() == this);
	}
	
	public void onOffenseEvent(OnCreatureDamageDealt event, OnCreatureSkillFinishCast castEvent, Skill skill, ITargetTypeHandler targetHandler)
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
		
		for (Creature affected : targetHandler.getTargetList(skill, attacker, false, target))
		{
			affected.getEffectList().stopSkillEffects(true, _cancelSkillId);
		}
	}
}
