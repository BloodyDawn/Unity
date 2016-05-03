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

import org.l2junity.commons.util.Rnd;
import org.l2junity.gameserver.model.StatsSet;
import org.l2junity.gameserver.model.effects.AbstractEffect;
import org.l2junity.gameserver.model.events.EventType;
import org.l2junity.gameserver.model.events.impl.character.OnCreatureKill;
import org.l2junity.gameserver.model.events.listeners.FunctionEventListener;
import org.l2junity.gameserver.model.events.returns.TerminateReturn;
import org.l2junity.gameserver.model.holders.SkillHolder;
import org.l2junity.gameserver.model.skills.BuffInfo;
import org.l2junity.gameserver.model.skills.Skill;
import org.l2junity.gameserver.model.skills.SkillCaster;

/**
 * Trigger Skill By Death Blow effect implementation.
 * @author Sdw
 */
public final class TriggerSkillByDeathBlow extends AbstractEffect
{
	private final int _chance;
	private final SkillHolder _skill;
	
	public TriggerSkillByDeathBlow(StatsSet params)
	{
		_chance = params.getInt("chance", 100);
		_skill = new SkillHolder(params.getInt("skillId"), params.getInt("skillLevel"));
	}
	
	public TerminateReturn onCreatureKill(OnCreatureKill event)
	{
		if ((_chance == 0) || ((_skill.getSkillId() == 0) || (_skill.getSkillLvl() == 0)))
		{
			return new TerminateReturn(false, false, false);
		}
		
		if (Rnd.get(100) > _chance)
		{
			return new TerminateReturn(false, false, false);
		}
		
		final Skill triggerSkill = _skill.getSkill();
		
		SkillCaster.triggerCast(event.getTarget(), event.getTarget(), triggerSkill);
		
		return new TerminateReturn(true, true, true);
	}
	
	@Override
	public void onExit(BuffInfo info)
	{
		info.getEffected().removeListenerIf(EventType.ON_CREATURE_KILL, listener -> listener.getOwner() == this);
	}
	
	@Override
	public void onStart(BuffInfo info)
	{
		info.getEffected().addListener(new FunctionEventListener(info.getEffected(), EventType.ON_CREATURE_KILL, (OnCreatureKill event) -> onCreatureKill(event), this));
	}
}
