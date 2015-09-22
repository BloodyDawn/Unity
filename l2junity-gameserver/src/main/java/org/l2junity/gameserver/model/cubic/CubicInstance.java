/*
 * Copyright (C) 2004-2015 L2J Unity
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
package org.l2junity.gameserver.model.cubic;

import java.util.Comparator;
import java.util.concurrent.ScheduledFuture;

import org.l2junity.Config;
import org.l2junity.commons.util.Rnd;
import org.l2junity.gameserver.ThreadPoolManager;
import org.l2junity.gameserver.model.Party;
import org.l2junity.gameserver.model.WorldObject;
import org.l2junity.gameserver.model.actor.Creature;
import org.l2junity.gameserver.model.actor.templates.L2CubicTemplate;
import org.l2junity.gameserver.model.skills.Skill;

/**
 * @author UnAfraid
 */
public class CubicInstance
{
	private final Creature _owner;
	private final Creature _caster;
	private final L2CubicTemplate _template;
	private ScheduledFuture<?> _skillUseTask;
	private ScheduledFuture<?> _expireTask;
	
	public CubicInstance(Creature owner, Creature caster, L2CubicTemplate template)
	{
		_owner = owner;
		_caster = caster;
		_template = template;
		activate();
	}
	
	private void activate()
	{
		_skillUseTask = ThreadPoolManager.getInstance().scheduleAiAtFixedRate(this::tryToUseSkill, _template.getDelay() * 1000, _template.getDelay() * 1000);
		_expireTask = ThreadPoolManager.getInstance().scheduleAi(this::deactivate, _template.getDuration() * 1000);
	}
	
	public void deactivate()
	{
		if ((_skillUseTask != null) && !_skillUseTask.isDone())
		{
			_skillUseTask.cancel(true);
		}
		_skillUseTask = null;
		
		if ((_expireTask != null) && !_expireTask.isDone())
		{
			_expireTask.cancel(true);
		}
		_expireTask = null;
	}
	
	private void tryToUseSkill()
	{
		final Creature target = findTarget();
		if (target != null)
		{
			final CubicSkill cubicSkill = _template.getSkills().stream().filter(sk -> sk.validateConditions(this, _owner, target)).findFirst().orElse(null);
			final Skill skill = cubicSkill.getSkill();
			if ((skill != null) && (Rnd.get(100) < cubicSkill.getSuccessRate()))
			{
				skill.activateSkill(_owner, target);
			}
		}
	}
	
	private Creature findTarget()
	{
		switch (_template.getTargetType())
		{
			case BY_SKILL:
			{
				final CubicSkill cubicSkill = _template.getSkills().stream().filter(sk -> sk.validateConditions(this, _owner, _owner)).findFirst().orElse(null);
				if (cubicSkill != null)
				{
					final Skill skill = cubicSkill.getSkill();
					if (skill != null)
					{
						final Creature[] targetList = skill.getTargetList(_caster);
						for (Creature possibleTarget : targetList)
						{
							if (cubicSkill.validateConditions(this, _owner, possibleTarget))
							{
								return possibleTarget;
							}
						}
					}
				}
				break;
			}
			case TARGET:
			{
				for (CubicSkill skill : _template.getSkills())
				{
					switch (skill.getTargetType())
					{
						case HEAL:
						{
							final Party party = _owner.getParty();
							if (party != null)
							{
								return party.getMembers().stream().filter(member -> skill.validateConditions(this, _owner, member) && member.isInsideRadius(_owner, Config.ALT_PARTY_RANGE, true, true)).sorted(Comparator.comparingInt(Creature::getCurrentHpPercent).reversed()).findFirst().orElse(null);
							}
							return _owner;
						}
						case MASTER:
						{
							if (skill.validateConditions(this, _owner, _owner))
							{
								return _owner;
							}
							break;
						}
						case TARGET:
						{
							final WorldObject targetObject = _owner.getTarget();
							final Creature target = (targetObject != null) && targetObject.isCreature() ? (Creature) targetObject : _owner;
							
							if (skill.validateConditions(this, _owner, target))
							{
								return target;
							}
							break;
						}
					}
				}
				break;
			}
			case HEAL:
			{
				final Party party = _owner.getParty();
				if (party != null)
				{
					return party.getMembers().stream().filter(member -> member.isInsideRadius(_owner, Config.ALT_PARTY_RANGE, true, true)).sorted(Comparator.comparingInt(Creature::getCurrentHpPercent).reversed()).findFirst().orElse(null);
				}
				return _owner;
			}
		}
		return null;
	}
	
	/**
	 * @return the {@link Creature} that owns this cubic
	 */
	public Creature getOwner()
	{
		return _owner;
	}
	
	/**
	 * @return the {@link Creature} that casted this cubic
	 */
	public Creature getCaster()
	{
		return _caster;
	}
	
	/**
	 * @return {@code true} if cubic is casted from someone else but the owner, {@code false}
	 */
	public boolean isGivenByOther()
	{
		return _caster != _owner;
	}
	
	/**
	 * @return the {@link L2CubicTemplate} of this cubic
	 */
	public L2CubicTemplate getTemplate()
	{
		return _template;
	}
}
