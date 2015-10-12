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

import java.util.HashMap;
import java.util.Map;

import org.l2junity.Config;
import org.l2junity.gameserver.enums.ShotType;
import org.l2junity.gameserver.model.StatsSet;
import org.l2junity.gameserver.model.actor.Attackable;
import org.l2junity.gameserver.model.actor.Creature;
import org.l2junity.gameserver.model.conditions.Condition;
import org.l2junity.gameserver.model.effects.AbstractEffect;
import org.l2junity.gameserver.model.effects.L2EffectType;
import org.l2junity.gameserver.model.items.Weapon;
import org.l2junity.gameserver.model.items.type.WeaponType;
import org.l2junity.gameserver.model.skills.Skill;
import org.l2junity.gameserver.model.stats.Formulas;
import org.l2junity.gameserver.model.stats.Stats;
import org.l2junity.gameserver.network.client.send.SystemMessage;
import org.l2junity.gameserver.network.client.send.string.SystemMessageId;

/**
 * Physical Attack effect implementation. <br>
 * <b>Note</b>: Damage formula moved here to allow more params due to Ertheia physical skills' complexity.
 * @author Adry_85, Nik
 */
public final class PhysicalAttackWeaponBonus extends AbstractEffect
{
	private final double _power;
	private final double _criticalChance;
	private final boolean _ignoreShieldDefence;
	private final boolean _overHit;
	
	private final Map<WeaponType, Double> _weaponBonus = new HashMap<>();
	
	public PhysicalAttackWeaponBonus(Condition attachCond, Condition applyCond, StatsSet set, StatsSet params)
	{
		super(attachCond, applyCond, set, params);
		
		_power = params.getDouble("power", 0);
		_criticalChance = params.getDouble("criticalChance", 0);
		_ignoreShieldDefence = params.getBoolean("ignoreShieldDefence", false);
		_overHit = params.getBoolean("overHit", false);
		
		for (WeaponType weapon : WeaponType.values())
		{
			final double bonus = params.getDouble(weapon.name(), 1);
			if (bonus != 1)
			{
				_weaponBonus.put(weapon, bonus);
			}
		}
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
	public void instant(Creature effector, Creature effected, Skill skill)
	{
		if (effector.isAlikeDead())
		{
			return;
		}
		
		if (((skill.getFlyRadius() > 0) || (skill.getFlyType() != null)) && effector.isMovementDisabled())
		{
			final SystemMessage sm = SystemMessage.getSystemMessage(SystemMessageId.S1_CANNOT_BE_USED_DUE_TO_UNSUITABLE_TERMS);
			sm.addSkillName(skill);
			effector.sendPacket(sm);
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
			damage = effector.getStat().getValue(Stats.CRITICAL_DAMAGE_SKILL, damage);
			damage *= 2;
		}
		
		final Weapon weapon = effector.getActiveWeaponItem();
		if (weapon != null)
		{
			damage *= _weaponBonus.getOrDefault(weapon.getItemType(), 1.);
		}
		
		if (damage > 0)
		{
			// Check if damage should be reflected
			Formulas.calcDamageReflected(effector, effected, skill, crit);
			
			final double damageCap = effected.getStat().getValue(Stats.DAMAGE_CAP);
			if (damageCap > 0)
			{
				damage = Math.min(damage, damageCap);
			}
			effector.sendDamageMessage(effected, (int) damage, false, crit, false);
			effected.reduceCurrentHp(damage, effector, skill);
			effected.notifyDamageReceived(damage, effector, skill, crit, false, false);
		}
		else
		{
			effector.sendPacket(SystemMessageId.YOUR_ATTACK_HAS_FAILED);
		}
		
		if (skill.isSuicideAttack())
		{
			effector.doDie(effector);
		}
	}
	
	public final double calcPhysDam(Creature effector, Creature effected, Skill skill)
	{
		final boolean isPvP = effector.isPlayable() && effected.isPlayable();
		double damage = effector.getPAtk(effected);
		double defence = effected.getPDef(effector);
		boolean ss = skill.isPhysical() && effector.isChargedShot(ShotType.SOULSHOTS);
		final byte shld = !_ignoreShieldDefence ? Formulas.calcShldUse(effector, effected, skill) : 0;
		final double distance = effector.calculateDistance(effected, true, false);
		
		if (distance > effected.getStat().getValue(Stats.DAMAGED_MAX_RANGE, Integer.MAX_VALUE))
		{
			return 0;
		}
		
		// Defense bonuses in PvP fight
		if (isPvP)
		{
			defence *= effected.getStat().getValue(Stats.PVP_PHYS_SKILL_DEF, 1);
		}
		
		switch (shld)
		{
			case Formulas.SHIELD_DEFENSE_SUCCEED:
			{
				if (!Config.ALT_GAME_SHIELD_BLOCKS)
				{
					defence += effected.getShldDef();
				}
				break;
			}
			case Formulas.SHIELD_DEFENSE_PERFECT_BLOCK: // perfect block
			{
				return 1.;
			}
		}
		
		// Add soulshot boost.
		final double shotsBonus = effector.getStat().getValue(Stats.SHOTS_BONUS);
		double ssBoost = ss ? 2 * shotsBonus : 1;
		damage = (damage * ssBoost) + _power;
		damage = (70 * damage) / (defence); // Calculate defence modifier.
		damage *= Formulas.calcAttackTraitBonus(effector, effected); // Calculate Weapon resists
		
		// Weapon random damage
		damage *= effector.getRandomDamageMultiplier();
		if ((shld > 0) && Config.ALT_GAME_SHIELD_BLOCKS)
		{
			damage -= effected.getShldDef();
			if (damage < 0)
			{
				damage = 0;
			}
		}
		
		if ((damage > 0) && (damage < 1))
		{
			damage = 1;
		}
		else if (damage < 0)
		{
			damage = 0;
		}
		
		// Dmg bonuses in PvP fight
		if (isPvP)
		{
			damage *= effector.getStat().getValue(Stats.PVP_PHYS_SKILL_DMG, 1);
		}
		
		// Physical skill dmg boost
		damage = effector.getStat().getValue(Stats.PHYSICAL_SKILL_POWER, damage);
		
		damage *= Formulas.calcAttributeBonus(effector, effected, skill);
		damage *= (1 - (effected.getStat().getValue(Stats.FIXED_DAMAGE_RES, 0) / 100)); // Include fixed damage resistance.
		
		if (effected.isAttackable())
		{
			final Weapon weapon = effector.getActiveWeaponItem();
			if ((weapon != null) && weapon.isBowOrCrossBow())
			{
				damage *= effector.getStat().getValue(Stats.PVE_BOW_SKILL_DMG, 1);
			}
			else
			{
				damage *= effector.getStat().getValue(Stats.PVE_PHYSICAL_DMG, 1);
			}
			if (!effected.isRaid() && !effected.isRaidMinion() && (effected.getLevel() >= Config.MIN_NPC_LVL_DMG_PENALTY) && (effector.getActingPlayer() != null) && ((effected.getLevel() - effector.getActingPlayer().getLevel()) >= 2))
			{
				int lvlDiff = effected.getLevel() - effector.getActingPlayer().getLevel() - 1;
				if (lvlDiff >= Config.NPC_SKILL_DMG_PENALTY.size())
				{
					damage *= Config.NPC_SKILL_DMG_PENALTY.get(Config.NPC_SKILL_DMG_PENALTY.size() - 1);
				}
				else
				{
					damage *= Config.NPC_SKILL_DMG_PENALTY.get(lvlDiff);
				}
			}
		}
		
		return damage;
	}
}
