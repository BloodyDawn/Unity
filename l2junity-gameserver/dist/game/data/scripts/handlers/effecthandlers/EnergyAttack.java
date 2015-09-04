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
import org.l2junity.gameserver.enums.ShotType;
import org.l2junity.gameserver.model.StatsSet;
import org.l2junity.gameserver.model.actor.Attackable;
import org.l2junity.gameserver.model.actor.Creature;
import org.l2junity.gameserver.model.actor.instance.PlayerInstance;
import org.l2junity.gameserver.model.conditions.Condition;
import org.l2junity.gameserver.model.effects.AbstractEffect;
import org.l2junity.gameserver.model.effects.L2EffectType;
import org.l2junity.gameserver.model.items.Weapon;
import org.l2junity.gameserver.model.skills.Skill;
import org.l2junity.gameserver.model.stats.BaseStats;
import org.l2junity.gameserver.model.stats.Formulas;
import org.l2junity.gameserver.model.stats.Stats;
import org.l2junity.gameserver.network.client.send.SystemMessage;
import org.l2junity.gameserver.network.client.send.string.SystemMessageId;

/**
 * Energy Attack effect implementation.
 * @author NosBit
 */
public final class EnergyAttack extends AbstractEffect
{
	private final double _power;
	private final int _chargeConsume;
	private final int _criticalChance;
	private final boolean _ignoreShieldDefence;
	private final boolean _overHit;
	
	public EnergyAttack(Condition attachCond, Condition applyCond, StatsSet set, StatsSet params)
	{
		super(attachCond, applyCond, set, params);
		
		_power = params.getDouble("power", 0);
		_criticalChance = params.getInt("criticalChance", 0);
		_ignoreShieldDefence = params.getBoolean("ignoreShieldDefence", false);
		_overHit = params.getBoolean("overHit", false);
		_chargeConsume = params.getInt("chargeConsume", 0);
	}
	
	@Override
	public boolean calcSuccess(Creature effector, Creature effected, Skill skill)
	{
		// TODO: Verify this on retail
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
	public void instant(Creature effector, Creature effected, Skill skill)
	{
		if (!effector.isPlayer())
		{
			return;
		}
		
		final PlayerInstance attacker = effector.getActingPlayer();
		
		final int charge = Math.max(_chargeConsume, attacker.getCharges());
		
		if (!attacker.decreaseCharges(charge))
		{
			final SystemMessage sm = SystemMessage.getSystemMessage(SystemMessageId.S1_CANNOT_BE_USED_DUE_TO_UNSUITABLE_TERMS);
			sm.addSkillName(skill);
			attacker.sendPacket(sm);
			return;
		}

		double attack = attacker.getPAtk(effected);
		int defence = effected.getPDef(attacker);
		
		if (!_ignoreShieldDefence)
		{
			byte shield = Formulas.calcShldUse(attacker, effected, skill, true);
			switch (shield)
			{
				case Formulas.SHIELD_DEFENSE_SUCCEED:
				{
					defence += effected.getShldDef();
					break;
				}
				case Formulas.SHIELD_DEFENSE_PERFECT_BLOCK:
				{
					defence = -1;
					break;
				}
			}
		}
		
		if (_overHit && effected.isAttackable())
		{
			((Attackable) effected).overhitEnabled(true);
		}
		
		double damage = 1;
		boolean critical = false;
		
		if (defence != -1)
		{
			double damageMultiplier = Formulas.calcWeaponTraitBonus(attacker, effected) * Formulas.calcAttributeBonus(attacker, effected, skill) * Formulas.calcGeneralTraitBonus(attacker, effected, skill.getTraitType(), true);
			
			boolean ss = skill.useSoulShot() && attacker.isChargedShot(ShotType.SOULSHOTS);
			double ssBoost = ss ? 2 : 1.0;
			
			double weaponTypeBoost;
			Weapon weapon = attacker.getActiveWeaponItem();
			if ((weapon != null) && weapon.isBowOrCrossBow())
			{
				weaponTypeBoost = 70;
			}
			else
			{
				weaponTypeBoost = 77;
			}
			
			// TODO Nik says 0.2 might 0.25 in H5, check it out for IO
			// Sdw says (charge * 0.1) + 1
			double energyChargesBoost = ((charge - 1) * 0.2) + 1;
			
			attack += _power;
			attack *= ssBoost;
			attack *= energyChargesBoost;
			attack *= weaponTypeBoost;
			
			damage = attack / defence;
			damage *= damageMultiplier;
			if (effected instanceof PlayerInstance)
			{
				damage *= attacker.getStat().getValue(Stats.PVP_PHYS_SKILL_DMG, 1.0);
				damage *= effected.getStat().getValue(Stats.PVP_PHYS_SKILL_DEF, 1.0);
				damage = attacker.getStat().getValue(Stats.PHYSICAL_SKILL_POWER, damage);
			}
			
			critical = (BaseStats.STR.calcBonus(attacker) * _criticalChance) > (Rnd.nextDouble() * 100);
			if (critical)
			{
				damage *= 2;
			}
		}
		
		if (damage > 0)
		{
			// Check if damage should be reflected
			Formulas.calcDamageReflected(attacker, effected, skill, critical);
			
			damage = effected.calcStat(Stats.DAMAGE_CAP, damage, null, null);
			attacker.sendDamageMessage(effected, (int) damage, false, critical, false);
			effected.reduceCurrentHp(damage, attacker, skill);
			effected.notifyDamageReceived(damage, attacker, skill, critical, false, false);
		}
	}
}