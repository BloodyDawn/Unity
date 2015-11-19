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

import org.l2junity.gameserver.enums.ShotType;
import org.l2junity.gameserver.model.StatsSet;
import org.l2junity.gameserver.model.actor.Attackable;
import org.l2junity.gameserver.model.actor.Creature;
import org.l2junity.gameserver.model.conditions.Condition;
import org.l2junity.gameserver.model.effects.AbstractEffect;
import org.l2junity.gameserver.model.effects.L2EffectType;
import org.l2junity.gameserver.model.items.instance.ItemInstance;
import org.l2junity.gameserver.model.skills.Skill;
import org.l2junity.gameserver.model.stats.Formulas;
import org.l2junity.gameserver.model.stats.Stats;
import org.l2junity.gameserver.network.client.send.SystemMessage;
import org.l2junity.gameserver.network.client.send.string.SystemMessageId;

/**
 * Physical Attack HP Link effect implementation.
 * @author Adry_85
 */
public final class PhysicalAttackHpLink extends AbstractEffect
{
	private final double _power;
	private final double _criticalChance;
	private final boolean _overHit;
	
	public PhysicalAttackHpLink(Condition attachCond, Condition applyCond, StatsSet set, StatsSet params)
	{
		super(attachCond, applyCond, set, params);
		
		_power = params.getDouble("power", 0);
		_criticalChance = params.getDouble("criticalChance", 0);
		_overHit = params.getBoolean("overHit", false);
	}
	
	@Override
	public boolean calcSuccess(Creature effector, Creature effected, Skill skill)
	{
		return !Formulas.calcPhysicalSkillEvasion(effector, effected, skill);
	}
	
	@Override
	public L2EffectType getEffectType()
	{
		return L2EffectType.PHYSICAL_ATTACK_HP_LINK;
	}
	
	@Override
	public boolean isInstant()
	{
		return true;
	}
	
	@Override
	public void instant(Creature effector, Creature effected, Skill skill, ItemInstance item)
	{
		if (effector.isAlikeDead())
		{
			return;
		}
		
		if (effector.isMovementDisabled())
		{
			SystemMessage sm = SystemMessage.getSystemMessage(SystemMessageId.S1_CANNOT_BE_USED_DUE_TO_UNSUITABLE_TERMS);
			sm.addSkillName(skill);
			effector.sendPacket(sm);
			return;
		}
		
		final byte shld = Formulas.calcShldUse(effector, effected, skill);
		boolean crit = Formulas.calcCrit(_criticalChance, true, effector, effected);
		
		if (_overHit && effected.isAttackable())
		{
			((Attackable) effected).overhitEnabled(true);
		}
		
		boolean ss = skill.isPhysical() && effector.isChargedShot(ShotType.SOULSHOTS);
		double damage = Formulas.calcPhysDam(effector, effected, skill, _power * (-((effected.getCurrentHp() * 2) / effected.getMaxHp()) + 2), shld, false, ss);
		
		if (damage > 0)
		{
			// Check if damage should be reflected.
			Formulas.calcDamageReflected(effector, effected, skill, crit);
			
			final double damageCap = effected.getStat().getValue(Stats.DAMAGE_CAP);
			if (damageCap > 0)
			{
				damage = Math.min(damage, damageCap);
			}
			damage = effected.notifyDamageReceived(damage, effector, skill, crit, false, false);
			effected.reduceCurrentHp(damage, effector, skill);
			effector.sendDamageMessage(effected, skill, (int) damage, crit, false);
		}
		else
		{
			effector.sendPacket(SystemMessageId.YOUR_ATTACK_HAS_FAILED);
		}
	}
}
