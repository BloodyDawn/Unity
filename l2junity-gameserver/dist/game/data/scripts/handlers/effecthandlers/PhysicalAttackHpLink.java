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
import org.l2junity.gameserver.model.effects.AbstractEffect;
import org.l2junity.gameserver.model.effects.L2EffectType;
import org.l2junity.gameserver.model.items.instance.ItemInstance;
import org.l2junity.gameserver.model.skills.Skill;
import org.l2junity.gameserver.model.stats.Formulas;
import org.l2junity.gameserver.model.stats.Stats;
import org.l2junity.gameserver.model.stats.TraitType;

/**
 * Physical Attack HP Link effect implementation.
 * @author Adry_85
 */
public final class PhysicalAttackHpLink extends AbstractEffect
{
	private final double _power;
	private final double _criticalChance;
	private final boolean _overHit;
	
	public PhysicalAttackHpLink(StatsSet params)
	{
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
		
		final byte shld = Formulas.calcShldUse(effector, effected, skill);
		boolean crit = Formulas.calcCrit(_criticalChance, true, effector, effected);
		
		if (_overHit && effected.isAttackable())
		{
			((Attackable) effected).overhitEnabled(true);
		}
		
		boolean ss = skill.isPhysical() && effector.isChargedShot(ShotType.SOULSHOTS);
		double damage = calcPhysSkillDam(effector, effected, skill, _power * (-((effected.getCurrentHp() * 2) / effected.getMaxHp()) + 2), shld, false, ss);
		
		// Check if damage should be reflected.
		Formulas.calcDamageReflected(effector, effected, skill, crit);
		
		final double damageCap = effected.getStat().getValue(Stats.DAMAGE_LIMIT);
		if (damageCap > 0)
		{
			damage = Math.min(damage, damageCap);
		}
		damage = effected.notifyDamageReceived(damage, effector, skill, crit, false, false);
		effected.reduceCurrentHp(damage, effector, skill);
		effector.sendDamageMessage(effected, skill, (int) damage, crit, false);
	}
	
	public static double calcPhysSkillDam(Creature attacker, Creature target, Skill skill, double power, byte shld, boolean crit, boolean ss)
	{
		// If target is trait invul (not trait resistant) to the specific skill trait, you deal 0 damage (message shown in retail of 0 damage).
		if ((skill == null) || ((skill.getTraitType() != TraitType.NONE) && target.getStat().isTraitInvul(skill.getTraitType())))
		{
			return 0;
		}
		
		final double distance = attacker.calculateDistance(target, true, false);
		if (distance > target.getStat().getValue(Stats.SPHERIC_BARRIER_RANGE, Integer.MAX_VALUE))
		{
			return 0;
		}
		
		double defence = target.getPDef();
		
		switch (shld)
		{
			case Formulas.SHIELD_DEFENSE_SUCCEED:
			{
				defence += target.getShldDef();
				break;
			}
			case Formulas.SHIELD_DEFENSE_PERFECT_BLOCK: // perfect block
			{
				return 1.;
			}
		}
		
		// Trait, elements
		final double weaponTraitMod = Formulas.calcWeaponTraitBonus(attacker, target);
		final double generalTraitMod = Formulas.calcGeneralTraitBonus(attacker, target, skill.getTraitType(), false);
		final double attributeMod = Formulas.calcAttributeBonus(attacker, target, skill);
		final double weaponMod = attacker.getRandomDamageMultiplier();
		final double pvpPveMod = Formulas.calculatePvpPveBonus(attacker, target, skill, true);
		
		// Add soulshot boost.
		final double ssmod = ss ? attacker.getStat().getValue(Stats.SHOTS_BONUS, 2) : 1; // 2.04 for dual weapon?
		final double baseMod = (77 * ((attacker.getPAtk() * attacker.getLevelMod()) + power)) / defence;
		final double damage = attacker.getStat().getValue(Stats.PHYSICAL_SKILL_POWER, baseMod) * ssmod * weaponTraitMod * generalTraitMod * attributeMod * weaponMod * pvpPveMod;
		return Math.max(damage, 0);
	}
}
