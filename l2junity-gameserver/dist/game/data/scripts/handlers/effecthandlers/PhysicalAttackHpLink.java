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
import org.l2junity.gameserver.model.skills.BuffInfo;
import org.l2junity.gameserver.model.stats.BaseStats;
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
	public boolean calcSuccess(BuffInfo info)
	{
		return !Formulas.calcPhysicalSkillEvasion(info.getEffector(), info.getEffected(), info.getSkill());
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
	public void onStart(BuffInfo info)
	{
		Creature target = info.getEffected();
		Creature activeChar = info.getEffector();
		
		if (activeChar.isAlikeDead())
		{
			return;
		}
		
		if (activeChar.isMovementDisabled())
		{
			SystemMessage sm = SystemMessage.getSystemMessage(SystemMessageId.S1_CANNOT_BE_USED_DUE_TO_UNSUITABLE_TERMS);
			sm.addSkillName(info.getSkill());
			activeChar.sendPacket(sm);
			return;
		}
		
		final byte shld = Formulas.calcShldUse(activeChar, target, info.getSkill());
		// Physical damage critical rate is only affected by STR.
		boolean crit = false;
		if (_criticalChance > 0)
		{
			crit = Formulas.calcCrit(_criticalChance * 10 * BaseStats.STR.calcBonus(activeChar), true, target);
		}
		
		if (_overHit && target.isAttackable())
		{
			((Attackable) target).overhitEnabled(true);
		}
		
		int damage = 0;
		boolean ss = info.getSkill().isPhysical() && activeChar.isChargedShot(ShotType.SOULSHOTS);
		damage = (int) Formulas.calcPhysDam(activeChar, target, info.getSkill(), _power * (-((target.getCurrentHp() * 2) / target.getMaxHp()) + 2), shld, false, ss);
		
		if (damage > 0)
		{
			// Check if damage should be reflected.
			Formulas.calcDamageReflected(activeChar, target, info.getSkill(), crit);
			
			damage = (int) target.calcStat(Stats.DAMAGE_CAP, damage, null, null);
			activeChar.sendDamageMessage(target, damage, false, crit, false);
			target.reduceCurrentHp(damage, activeChar, info.getSkill());
			target.notifyDamageReceived(damage, activeChar, info.getSkill(), crit, false, false);
		}
		else
		{
			activeChar.sendPacket(SystemMessageId.YOUR_ATTACK_HAS_FAILED);
		}
	}
}
