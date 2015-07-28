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
import org.l2junity.gameserver.model.skills.BuffInfo;
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
	public boolean calcSuccess(BuffInfo info)
	{
		return !Formulas.calcPhysicalSkillEvasion(info.getEffector(), info.getEffected(), info.getSkill());
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
		Creature target = info.getEffected();
		Creature activeChar = info.getEffector();
		
		if (activeChar.isAlikeDead())
		{
			return;
		}
		
		if (((info.getSkill().getFlyRadius() > 0) || (info.getSkill().getFlyType() != null)) && activeChar.isMovementDisabled())
		{
			final SystemMessage sm = SystemMessage.getSystemMessage(SystemMessageId.S1_CANNOT_BE_USED_DUE_TO_UNSUITABLE_TERMS);
			sm.addSkillName(info.getSkill());
			activeChar.sendPacket(sm);
			return;
		}
		
		if (target.isPlayer() && target.getActingPlayer().isFakeDeath())
		{
			target.stopFakeDeath(true);
		}
		
		if (_overHit && target.isAttackable())
		{
			((Attackable) target).overhitEnabled(true);
		}
		
		double damage = (int) calcPhysDam(info);
		boolean crit = Formulas.calcCrit(_criticalChance, true, activeChar, target);
		
		if (crit)
		{
			damage = activeChar.calcStat(Stats.CRITICAL_DAMAGE_SKILL, damage, target, info.getSkill());
			damage *= 2;
		}
		
		final Weapon weapon = activeChar.getActiveWeaponItem();
		if (weapon != null)
		{
			damage *= _weaponBonus.getOrDefault(weapon.getItemType(), 1.);
		}
		
		if (damage > 0)
		{
			// Check if damage should be reflected
			Formulas.calcDamageReflected(activeChar, target, info.getSkill(), crit);
			
			damage = target.calcStat(Stats.DAMAGE_CAP, damage, null, null);
			activeChar.sendDamageMessage(target, (int) damage, false, crit, false);
			target.reduceCurrentHp(damage, activeChar, info.getSkill());
			target.notifyDamageReceived(damage, activeChar, info.getSkill(), crit, false, false);
		}
		else
		{
			activeChar.sendPacket(SystemMessageId.YOUR_ATTACK_HAS_FAILED);
		}
		
		if (info.getSkill().isSuicideAttack())
		{
			activeChar.doDie(activeChar);
		}
	}
	
	public final double calcPhysDam(BuffInfo info)
	{
		final Creature attacker = info.getEffector();
		final Creature target = info.getEffected();
		final Skill skill = info.getSkill();
		final boolean isPvP = attacker.isPlayable() && target.isPlayable();
		double damage = attacker.getPAtk(target);
		double defence = target.getPDef(attacker);
		boolean ss = info.getSkill().isPhysical() && attacker.isChargedShot(ShotType.SOULSHOTS);
		final byte shld = !_ignoreShieldDefence ? Formulas.calcShldUse(attacker, target, info.getSkill()) : 0;
		final double distance = attacker.calculateDistance(target, true, false);
		
		if (distance > target.calcStat(Stats.DAMAGED_MAX_RANGE, Integer.MAX_VALUE, target, skill))
		{
			return 0;
		}
		
		// Defense bonuses in PvP fight
		if (isPvP)
		{
			defence *= target.calcStat(Stats.PVP_PHYS_SKILL_DEF, 1, null, null);
		}
		
		switch (shld)
		{
			case Formulas.SHIELD_DEFENSE_SUCCEED:
			{
				if (!Config.ALT_GAME_SHIELD_BLOCKS)
				{
					defence += target.getShldDef();
				}
				break;
			}
			case Formulas.SHIELD_DEFENSE_PERFECT_BLOCK: // perfect block
			{
				return 1.;
			}
		}
		
		// Add soulshot boost.
		int ssBoost = ss ? 2 : 1;
		damage = (damage * ssBoost) + _power;
		damage = (70 * damage) / (defence); // Calculate defence modifier.
		damage *= Formulas.calcAttackTraitBonus(attacker, target); // Calculate Weapon resists
		
		// Weapon random damage
		damage *= attacker.getRandomDamageMultiplier();
		if ((shld > 0) && Config.ALT_GAME_SHIELD_BLOCKS)
		{
			damage -= target.getShldDef();
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
			damage *= attacker.calcStat(Stats.PVP_PHYS_SKILL_DMG, 1, null, null);
		}
		
		// Physical skill dmg boost
		damage = attacker.calcStat(Stats.PHYSICAL_SKILL_POWER, damage, null, null);
		
		damage *= Formulas.calcAttributeBonus(attacker, target, skill);
		if (target.isAttackable())
		{
			final Weapon weapon = attacker.getActiveWeaponItem();
			if ((weapon != null) && weapon.isBowOrCrossBow())
			{
				damage *= attacker.calcStat(Stats.PVE_BOW_SKILL_DMG, 1, null, null);
			}
			else
			{
				damage *= attacker.calcStat(Stats.PVE_PHYSICAL_DMG, 1, null, null);
			}
			if (!target.isRaid() && !target.isRaidMinion() && (target.getLevel() >= Config.MIN_NPC_LVL_DMG_PENALTY) && (attacker.getActingPlayer() != null) && ((target.getLevel() - attacker.getActingPlayer().getLevel()) >= 2))
			{
				int lvlDiff = target.getLevel() - attacker.getActingPlayer().getLevel() - 1;
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