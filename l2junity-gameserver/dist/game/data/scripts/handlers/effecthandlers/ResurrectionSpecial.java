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

import org.l2junity.gameserver.model.StatsSet;
import org.l2junity.gameserver.model.actor.instance.L2PcInstance;
import org.l2junity.gameserver.model.actor.instance.L2PetInstance;
import org.l2junity.gameserver.model.conditions.Condition;
import org.l2junity.gameserver.model.effects.AbstractEffect;
import org.l2junity.gameserver.model.effects.EffectFlag;
import org.l2junity.gameserver.model.effects.L2EffectType;
import org.l2junity.gameserver.model.skills.BuffInfo;
import org.l2junity.gameserver.model.skills.Skill;

/**
 * Resurrection Special effect implementation.
 * @author Zealar
 */
public final class ResurrectionSpecial extends AbstractEffect
{
	private final int _power;
	
	public ResurrectionSpecial(Condition attachCond, Condition applyCond, StatsSet set, StatsSet params)
	{
		super(attachCond, applyCond, set, params);
		
		_power = params.getInt("power", 0);
	}
	
	@Override
	public L2EffectType getEffectType()
	{
		return L2EffectType.RESURRECTION_SPECIAL;
	}
	
	@Override
	public int getEffectFlags()
	{
		return EffectFlag.RESURRECTION_SPECIAL.getMask();
	}
	
	@Override
	public void onExit(BuffInfo info)
	{
		if (!info.getEffected().isPlayer() && !info.getEffected().isPet())
		{
			return;
		}
		L2PcInstance caster = info.getEffector().getActingPlayer();
		
		Skill skill = info.getSkill();
		
		if (info.getEffected().isPlayer())
		{
			info.getEffected().getActingPlayer().reviveRequest(caster, skill, false, _power);
			return;
		}
		if (info.getEffected().isPet())
		{
			L2PetInstance pet = (L2PetInstance) info.getEffected();
			info.getEffected().getActingPlayer().reviveRequest(pet.getActingPlayer(), skill, true, _power);
		}
	}
}