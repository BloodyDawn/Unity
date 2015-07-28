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
import org.l2junity.gameserver.model.actor.instance.PlayerInstance;
import org.l2junity.gameserver.model.conditions.Condition;
import org.l2junity.gameserver.model.effects.AbstractEffect;
import org.l2junity.gameserver.model.effects.L2EffectType;
import org.l2junity.gameserver.model.skills.BuffInfo;
import org.l2junity.gameserver.model.stats.Formulas;
import org.l2junity.gameserver.model.stats.Stats;

/**
 * Backstab effect implementation.
 * @author Adry_85
 */
public final class Backstab extends AbstractEffect
{
	private final double _power;
	private final double _chance;
	private final double _criticalChance;
	private final boolean _overHit;
	
	public Backstab(Condition attachCond, Condition applyCond, StatsSet set, StatsSet params)
	{
		super(attachCond, applyCond, set, params);
		
		_power = params.getDouble("power", 0);
		_chance = params.getDouble("chance", 0);
		_criticalChance = params.getDouble("criticalChance", 0);
		_overHit = params.getBoolean("overHit", false);
	}
	
	@Override
	public boolean calcSuccess(BuffInfo info)
	{
		return info.getEffector().isBehindTarget() && !Formulas.calcPhysicalSkillEvasion(info.getEffector(), info.getEffected(), info.getSkill()) && Formulas.calcBlowSuccess(info.getEffector(), info.getEffected(), info.getSkill(), _chance);
	}
	
	@Override
	public L2EffectType getEffectType()
	{
		return L2EffectType.PHYSICAL_ATTACK;
	}
	
	@Override
	public boolean isInstant()
	{
		return true;
	}
	
	@Override
	public void onStart(BuffInfo info)
	{
		if (info.getEffector().isAlikeDead())
		{
			return;
		}
		
		Creature target = info.getEffected();
		Creature activeChar = info.getEffector();
		
		if (_overHit && target.isAttackable())
		{
			((Attackable) target).overhitEnabled(true);
		}
		
		boolean ss = info.getSkill().useSoulShot() && activeChar.isChargedShot(ShotType.SOULSHOTS);
		byte shld = Formulas.calcShldUse(activeChar, target, info.getSkill());
		double damage = Formulas.calcBackstabDamage(activeChar, target, info.getSkill(), _power, shld, ss);
		
		if (Formulas.calcCrit(_criticalChance, true, activeChar, target))
		{
			damage *= 2;
		}
		
		// Check if damage should be reflected
		Formulas.calcDamageReflected(activeChar, target, info.getSkill(), true);
		
		damage = target.calcStat(Stats.DAMAGE_CAP, damage, null, null);
		target.reduceCurrentHp(damage, activeChar, !info.getSkill().isToggle(), false, true, info.getSkill());
		target.notifyDamageReceived(damage, activeChar, info.getSkill(), true, false, false);
		
		// Manage attack or cast break of the target (calculating rate, sending message...)
		if (!target.isRaid() && Formulas.calcAtkBreak(target, damage))
		{
			target.breakAttack();
			target.breakCast();
		}
		
		if (activeChar.isPlayer())
		{
			PlayerInstance activePlayer = activeChar.getActingPlayer();
			activePlayer.sendDamageMessage(target, (int) damage, false, true, false);
		}
	}
}