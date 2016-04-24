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

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.l2junity.gameserver.enums.ShotType;
import org.l2junity.gameserver.model.StatsSet;
import org.l2junity.gameserver.model.actor.Attackable;
import org.l2junity.gameserver.model.actor.Creature;
import org.l2junity.gameserver.model.effects.AbstractEffect;
import org.l2junity.gameserver.model.effects.L2EffectType;
import org.l2junity.gameserver.model.items.instance.ItemInstance;
import org.l2junity.gameserver.model.skills.AbnormalType;
import org.l2junity.gameserver.model.skills.Skill;
import org.l2junity.gameserver.model.stats.Formulas;
import org.l2junity.gameserver.model.stats.Stats;
import org.l2junity.gameserver.model.stats.TraitType;

/**
 * Physical Attack effect implementation. <br>
 * <b>Note</b>: Damage formula moved here to allow more params due to Ertheia physical skills' complexity.
 * @author Adry_85, Nik
 */
public final class PhysicalAttack extends AbstractEffect
{
	private final double _power;
	private final double _pAtkMod;
	private final double _pDefMod;
	private final double _criticalChance;
	private final boolean _ignoreShieldDefence;
	private final boolean _overHit;
	
	private final Set<AbnormalType> _abnormals;
	private final double _abnormalPowerMod;
	
	public PhysicalAttack(StatsSet params)
	{
		_power = params.getDouble("power", 0);
		_pAtkMod = params.getDouble("pAtkMod", 1.0);
		_pDefMod = params.getDouble("pDefMod", 1.0);
		_criticalChance = params.getDouble("criticalChance", 0);
		_ignoreShieldDefence = params.getBoolean("ignoreShieldDefence", false);
		_overHit = params.getBoolean("overHit", false);
		
		String abnormals = params.getString("abnormalType", null);
		if ((abnormals != null) && !abnormals.isEmpty())
		{
			_abnormals = new HashSet<>();
			for (String slot : abnormals.split(";"))
			{
				_abnormals.add(AbnormalType.getAbnormalType(slot));
			}
		}
		else
		{
			_abnormals = Collections.<AbnormalType> emptySet();
		}
		_abnormalPowerMod = params.getDouble("damageModifier", 1);
	}
	
	@Override
	public boolean calcSuccess(Creature effector, Creature effected, Skill skill)
	{
		return !Formulas.calcPhysicalSkillEvasion(effector, effected, skill);
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
	public void instant(Creature effector, Creature effected, Skill skill, ItemInstance item)
	{
		if (effector.isAlikeDead())
		{
			return;
		}
		
		if (effected.isPlayer() && effected.getActingPlayer().isFakeDeath())
		{
			effected.stopFakeDeath(true);
		}
		
		if (_overHit && effected.isAttackable())
		{
			((Attackable) effected).overhitEnabled(true);
		}
		
		double damage = (int) calcPhysDam(effector, effected, skill);
		boolean crit = Formulas.calcCrit(_criticalChance, true, effector, effected);
		
		if (crit)
		{
			damage *= (2 * Formulas.calcCritDamage(effector, effected, skill));
		}
		
		// Check if damage should be reflected
		Formulas.calcDamageReflected(effector, effected, skill, crit);
		final double damageCap = effected.getStat().getValue(Stats.DAMAGE_LIMIT);
		if (damageCap > 0)
		{
			damage = Math.min(damage, damageCap);
		}
		damage = effected.notifyDamageReceived(damage, effector, skill, crit, false, false);
		effected.reduceCurrentHp(damage, effector, skill);
		effector.sendDamageMessage(effected, skill, (int) damage, crit, false);
		
		if (skill.isSuicideAttack())
		{
			effector.doDie(effector);
		}
	}
	
	public final double calcPhysDam(Creature effector, Creature effected, Skill skill)
	{
		// If target is trait invul (not trait resistant) to the specific skill trait, you deal 0 damage (message shown in retail of 0 damage).
		if ((skill.getTraitType() != TraitType.NONE) && effected.getStat().isTraitInvul(skill.getTraitType()))
		{
			return 0;
		}
		
		double damage = effector.getPAtk();
		double defence = effected.getPDef();
		boolean ss = skill.isPhysical() && effector.isChargedShot(ShotType.SOULSHOTS);
		final byte shld = !_ignoreShieldDefence ? Formulas.calcShldUse(effector, effected, skill) : 0;
		final double distance = effector.calculateDistance(effected, true, false);
		
		if (distance > effected.getStat().getValue(Stats.SPHERIC_BARRIER_RANGE, Integer.MAX_VALUE))
		{
			return 0;
		}
		
		switch (shld)
		{
			case Formulas.SHIELD_DEFENSE_SUCCEED:
			{
				defence += effected.getShldDef();
				break;
			}
			case Formulas.SHIELD_DEFENSE_PERFECT_BLOCK: // perfect block
			{
				return 1.;
			}
		}
		
		// Most accurate bow formula so far: =((77 * ((patk * 1,333) + patk)) + 140 * power) / E2
		
		// Add soulshot boost.
		final double shotsBonus = effector.getStat().getValue(Stats.SHOTS_BONUS);
		double ssBoost = ss ? 2 * shotsBonus : 1;
		damage = (damage * _pAtkMod * ssBoost) + _power;
		damage = (70 * damage) / (defence * _pDefMod); // Calculate defence modifier.
		damage *= Formulas.calcAttackTraitBonus(effector, effected); // Calculate Weapon resists
		
		// Weapon random damage
		damage *= effector.getRandomDamageMultiplier();
		
		if ((damage > 0) && (damage < 1))
		{
			damage = 1;
		}
		else if (damage < 0)
		{
			damage = 0;
		}
		
		// Physical skill dmg boost
		damage = effector.getStat().getValue(Stats.PHYSICAL_SKILL_POWER, damage);
		
		damage *= Formulas.calcAttributeBonus(effector, effected, skill);
		damage *= Formulas.calculatePvpPveBonus(effector, effected, skill, false);
		
		// Check if we apply an abnormal modifier
		if (_abnormals.stream().anyMatch(a -> effected.getEffectList().getBuffInfoByAbnormalType(a) != null))
		{
			damage *= _abnormalPowerMod;
		}
		
		return damage;
	}
}
