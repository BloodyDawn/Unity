/*
 * Copyright (C) 2004-2015 L2J Unity
 * 
 * This file is part of L2J Unity.
 * 
 * L2J Unity is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * L2J Unity is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package org.l2junity.gameserver.model.actor.stat;

import java.util.Arrays;

import org.l2junity.Config;
import org.l2junity.gameserver.enums.AttributeType;
import org.l2junity.gameserver.model.PcCondOverride;
import org.l2junity.gameserver.model.actor.Creature;
import org.l2junity.gameserver.model.items.Weapon;
import org.l2junity.gameserver.model.items.instance.ItemInstance;
import org.l2junity.gameserver.model.skills.Skill;
import org.l2junity.gameserver.model.stats.BaseStats;
import org.l2junity.gameserver.model.stats.Calculator;
import org.l2junity.gameserver.model.stats.MoveType;
import org.l2junity.gameserver.model.stats.Stats;
import org.l2junity.gameserver.model.stats.TraitType;
import org.l2junity.gameserver.model.zone.ZoneId;

public class CharStat
{
	private final Creature _activeChar;
	private long _exp = 0;
	private long _sp = 0;
	private byte _level = 1;
	private final float[] _attackTraits = new float[TraitType.values().length];
	private final int[] _attackTraitsCount = new int[TraitType.values().length];
	private final float[] _defenceTraits = new float[TraitType.values().length];
	private final int[] _defenceTraitsCount = new int[TraitType.values().length];
	private final int[] _traitsInvul = new int[TraitType.values().length];
	/** Creature's maximum buff count. */
	private int _maxBuffCount = Config.BUFFS_MAX_AMOUNT;
	
	public CharStat(Creature activeChar)
	{
		_activeChar = activeChar;
		Arrays.fill(_attackTraits, 1.0f);
		Arrays.fill(_defenceTraits, 1.0f);
	}
	
	public final double calcStat(Stats stat, double init)
	{
		return calcStat(stat, init, null, null);
	}
	
	/**
	 * Calculate the new value of the state with modifiers that will be applied on the targeted L2Character.<BR>
	 * <B><U> Concept</U> :</B><BR
	 * A L2Character owns a table of Calculators called <B>_calculators</B>. Each Calculator (a calculator per state) own a table of Func object. A Func object is a mathematic function that permit to calculate the modifier of a state (ex : REGENERATE_HP_RATE...) : <BR>
	 * FuncAtkAccuracy -> Math.sqrt(_player.getDEX())*6+_player.getLevel()<BR>
	 * When the calc method of a calculator is launched, each mathematical function is called according to its priority <B>_order</B>.<br>
	 * Indeed, Func with lowest priority order is executed firsta and Funcs with the same order are executed in unspecified order.<br>
	 * The result of the calculation is stored in the value property of an Env class instance.<br>
	 * @param stat The stat to calculate the new value with modifiers
	 * @param initVal The initial value of the stat before applying modifiers
	 * @param target The L2Charcater whose properties will be used in the calculation (ex : CON, INT...)
	 * @param skill The L2Skill whose properties will be used in the calculation (ex : Level...)
	 * @return
	 */
	public final double calcStat(Stats stat, double initVal, Creature target, Skill skill)
	{
		double value = initVal;
		if (stat == null)
		{
			return value;
		}
		
		final int id = stat.ordinal();
		final Calculator c = _activeChar.getCalculators()[id];
		
		// If no Func object found, no modifier is applied
		if ((c == null) || (c.size() == 0))
		{
			return value;
		}
		
		// Apply transformation stats.
		if (getActiveChar().isPlayer() && getActiveChar().isTransformed())
		{
			double val = getActiveChar().getTransformation().getStat(getActiveChar().getActingPlayer(), stat);
			if (val > 0)
			{
				value = val;
			}
		}
		
		// Launch the calculation
		value = c.calc(_activeChar, target, skill, value);
		
		// avoid some troubles with negative stats (some stats should never be negative)
		if (value <= 0)
		{
			switch (stat)
			{
				case MAX_HP:
				case MAX_MP:
				case MAX_CP:
				case MAGIC_DEFENCE:
				case POWER_DEFENCE:
				case POWER_ATTACK:
				case MAGIC_ATTACK:
				case POWER_ATTACK_SPEED:
				case MAGIC_ATTACK_SPEED:
				case SHIELD_DEFENCE:
				case STAT_CON:
				case STAT_DEX:
				case STAT_INT:
				case STAT_MEN:
				case STAT_STR:
				case STAT_WIT:
				case STAT_LUC:
				case STAT_CHA:
				{
					value = 1.0;
					break;
				}
			}
		}
		return value;
	}
	
	/**
	 * @return the Accuracy (base+modifier) of the L2Character in function of the Weapon Expertise Penalty.
	 */
	public int getAccuracy()
	{
		return (int) Math.round(calcStat(Stats.ACCURACY_COMBAT, 0, null, null));
	}
	
	/**
	 * @return the Magic Accuracy (base+modifier) of the L2Character
	 */
	public int getMagicAccuracy()
	{
		return (int) Math.round(calcStat(Stats.ACCURACY_MAGIC, 0, null, null));
	}
	
	public Creature getActiveChar()
	{
		return _activeChar;
	}
	
	/**
	 * @return the Attack Speed multiplier (base+modifier) of the L2Character to get proper animations.
	 */
	public final float getAttackSpeedMultiplier()
	{
		return (float) (((1.1) * getPAtkSpd()) / _activeChar.getTemplate().getBasePAtkSpd());
	}
	
	/**
	 * @return the CON of the L2Character (base+modifier).
	 */
	public final int getCON()
	{
		return (int) calcStat(Stats.STAT_CON, _activeChar.getTemplate().getBaseCON());
	}
	
	/**
	 * @param target
	 * @param init
	 * @return the Critical Damage rate (base+modifier) of the L2Character.
	 */
	public final double getCriticalDmg(Creature target, double init)
	{
		return calcStat(Stats.CRITICAL_DAMAGE, init, target, null);
	}
	
	/**
	 * @param target
	 * @param skill
	 * @return the Critical Hit rate (base+modifier) of the L2Character.
	 */
	public int getCriticalHit(Creature target, Skill skill)
	{
		int val = (int) calcStat(Stats.CRITICAL_RATE, _activeChar.getTemplate().getBaseCritRate(), target, skill);
		
		if (!_activeChar.canOverrideCond(PcCondOverride.MAX_STATS_VALUE))
		{
			val = Math.min(val, Config.MAX_PCRIT_RATE);
		}
		
		return val;
	}
	
	public double getSkillCriticalRateBonus()
	{
		// There is a chance that activeChar has altered base stat for skill critical.
		byte skillCritRateStat = (byte) _activeChar.calcStat(Stats.STAT_SKILLCRITICAL, -1);
		if ((skillCritRateStat >= 0) && (skillCritRateStat < BaseStats.values().length))
		{
			return BaseStats.values()[skillCritRateStat].calcBonus(_activeChar);
		}
		
		// Default base stat used for skill critical formula is STR.
		return BaseStats.STR.calcBonus(_activeChar);
	}
	
	/**
	 * @return the DEX of the L2Character (base+modifier).
	 */
	public final int getDEX()
	{
		return (int) calcStat(Stats.STAT_DEX, _activeChar.getTemplate().getBaseDEX());
	}
	
	/**
	 * @param target
	 * @return the Attack Evasion rate (base+modifier) of the L2Character.
	 */
	public int getEvasionRate(Creature target)
	{
		int val = (int) Math.round(calcStat(Stats.EVASION_RATE, 0, target, null));
		
		if (!_activeChar.canOverrideCond(PcCondOverride.MAX_STATS_VALUE))
		{
			val = Math.min(val, Config.MAX_EVASION);
		}
		
		return val;
	}
	
	/**
	 * @param target
	 * @return the Attack Evasion rate (base+modifier) of the L2Character.
	 */
	public int getMagicEvasionRate(Creature target)
	{
		int val = (int) Math.round(calcStat(Stats.MAGIC_EVASION_RATE, 0, target, null));
		
		if (!_activeChar.canOverrideCond(PcCondOverride.MAX_STATS_VALUE))
		{
			val = Math.min(val, Config.MAX_EVASION);
		}
		
		return val;
	}
	
	public long getExp()
	{
		return _exp;
	}
	
	public void setExp(long value)
	{
		_exp = value;
	}
	
	/**
	 * @return the INT of the L2Character (base+modifier).
	 */
	public int getINT()
	{
		return (int) calcStat(Stats.STAT_INT, _activeChar.getTemplate().getBaseINT());
	}
	
	public byte getLevel()
	{
		return _level;
	}
	
	public void setLevel(byte value)
	{
		_level = value;
	}
	
	/**
	 * @param skill
	 * @return the Magical Attack range (base+modifier) of the L2Character.
	 */
	public final int getMagicalAttackRange(Skill skill)
	{
		if (skill != null)
		{
			return (int) calcStat(Stats.MAGIC_ATTACK_RANGE, skill.getCastRange(), null, skill);
		}
		
		return _activeChar.getTemplate().getBaseAttackRange();
	}
	
	public int getMaxCp()
	{
		return (int) calcStat(Stats.MAX_CP, _activeChar.getTemplate().getBaseCpMax());
	}
	
	public int getMaxRecoverableCp()
	{
		return (int) calcStat(Stats.MAX_RECOVERABLE_CP, getMaxCp());
	}
	
	public int getMaxHp()
	{
		return (int) calcStat(Stats.MAX_HP, _activeChar.getTemplate().getBaseHpMax());
	}
	
	public int getMaxRecoverableHp()
	{
		return (int) calcStat(Stats.MAX_RECOVERABLE_HP, getMaxHp());
	}
	
	public int getMaxMp()
	{
		return (int) calcStat(Stats.MAX_MP, _activeChar.getTemplate().getBaseMpMax());
	}
	
	public int getMaxRecoverableMp()
	{
		return (int) calcStat(Stats.MAX_RECOVERABLE_MP, getMaxMp());
	}
	
	/**
	 * Return the MAtk (base+modifier) of the L2Character.<br>
	 * <B><U>Example of use</U>: Calculate Magic damage
	 * @param target The L2Character targeted by the skill
	 * @param skill The L2Skill used against the target
	 * @return
	 */
	public int getMAtk(Creature target, Skill skill)
	{
		float bonusAtk = 1;
		if (Config.L2JMOD_CHAMPION_ENABLE && _activeChar.isChampion())
		{
			bonusAtk = Config.L2JMOD_CHAMPION_ATK;
		}
		if (_activeChar.isRaid())
		{
			bonusAtk *= Config.RAID_MATTACK_MULTIPLIER;
		}
		
		// Calculate modifiers Magic Attack
		return (int) calcStat(Stats.MAGIC_ATTACK, _activeChar.getTemplate().getBaseMAtk() * bonusAtk, target, skill);
	}
	
	/**
	 * @return the MAtk Speed (base+modifier) of the L2Character in function of the Armour Expertise Penalty.
	 */
	public int getMAtkSpd()
	{
		float bonusSpdAtk = 1;
		if (Config.L2JMOD_CHAMPION_ENABLE && _activeChar.isChampion())
		{
			bonusSpdAtk = Config.L2JMOD_CHAMPION_SPD_ATK;
		}
		
		double val = calcStat(Stats.MAGIC_ATTACK_SPEED, _activeChar.getTemplate().getBaseMAtkSpd() * bonusSpdAtk);
		
		if (!_activeChar.canOverrideCond(PcCondOverride.MAX_STATS_VALUE))
		{
			val = Math.min(val, Config.MAX_MATK_SPEED);
		}
		
		return (int) val;
	}
	
	/**
	 * @param target
	 * @param skill
	 * @return the Magic Critical Hit rate (base+modifier) of the L2Character.
	 */
	public final int getMCriticalHit(Creature target, Skill skill)
	{
		int val = (int) calcStat(Stats.MCRITICAL_RATE, getActiveChar().getTemplate().getBaseMCritRate(), target, skill);
		
		if (!_activeChar.canOverrideCond(PcCondOverride.MAX_STATS_VALUE))
		{
			val = Math.min(val, Config.MAX_MCRIT_RATE);
		}
		
		return val;
	}
	
	/**
	 * <B><U>Example of use </U>: Calculate Magic damage.
	 * @param target The L2Character targeted by the skill
	 * @param skill The L2Skill used against the target
	 * @return the MDef (base+modifier) of the L2Character against a skill in function of abnormal effects in progress.
	 */
	public int getMDef(Creature target, Skill skill)
	{
		// Get the base MDef of the L2Character
		double defence = _activeChar.getTemplate().getBaseMDef();
		
		// Calculate modifier for Raid Bosses
		if (_activeChar.isRaid())
		{
			defence *= Config.RAID_MDEFENCE_MULTIPLIER;
		}
		
		// Calculate modifiers Magic Attack
		return (int) calcStat(Stats.MAGIC_DEFENCE, defence, target, skill);
	}
	
	/**
	 * @return the MEN of the L2Character (base+modifier).
	 */
	public final int getMEN()
	{
		return (int) calcStat(Stats.STAT_MEN, _activeChar.getTemplate().getBaseMEN());
	}
	
	public final int getLUC()
	{
		return (int) calcStat(Stats.STAT_LUC, _activeChar.getTemplate().getBaseLUC());
	}
	
	public final int getCHA()
	{
		return (int) calcStat(Stats.STAT_CHA, _activeChar.getTemplate().getBaseCHA());
	}
	
	public int getMovementSpeedStatBonus()
	{
		byte speedStat = (byte) calcStat(Stats.STAT_SPEED, -1);
		if ((speedStat >= 0) && (speedStat < BaseStats.values().length))
		{
			// Very bad way of implementation... rework it once a better way is found.
			switch (speedStat)
			{
				case 0: // STR
					return Math.max(0, _activeChar.getSTR() - 55);
				case 1: // INT
					return Math.max(0, _activeChar.getINT() - 55);
				case 2: // DEX
					return Math.max(0, _activeChar.getDEX() - 55);
				case 3: // WIT
					return Math.max(0, _activeChar.getWIT() - 55);
				case 4: // CON
					return Math.max(0, _activeChar.getCON() - 55);
				case 5: // MEN
					return Math.max(0, _activeChar.getMEN() - 55);
				case 6: // CHA
					return Math.max(0, _activeChar.getCHA() - 55);
				case 7: // LUC
					return Math.max(0, _activeChar.getLUC() - 55);
			}
		}
		
		return 0;
	}
	
	public double getMovementSpeedMultiplier()
	{
		double baseSpeed;
		if (_activeChar.isInsideZone(ZoneId.WATER))
		{
			baseSpeed = getBaseMoveSpeed(_activeChar.isRunning() ? MoveType.FAST_SWIM : MoveType.SLOW_SWIM);
		}
		else
		{
			baseSpeed = getBaseMoveSpeed(_activeChar.isRunning() ? MoveType.RUN : MoveType.WALK);
		}
		return getMoveSpeed() * (1. / baseSpeed);
	}
	
	/**
	 * @return the RunSpeed (base+modifier) of the L2Character in function of the Armour Expertise Penalty.
	 */
	public double getRunSpeed()
	{
		final double baseRunSpd = _activeChar.isInsideZone(ZoneId.WATER) ? getSwimRunSpeed() : getBaseMoveSpeed(MoveType.RUN) + getMovementSpeedStatBonus();
		if (baseRunSpd <= 0)
		{
			return 0;
		}
		
		return calcStat(Stats.MOVE_SPEED, baseRunSpd, null, null);
	}
	
	/**
	 * @return the WalkSpeed (base+modifier) of the L2Character.
	 */
	public double getWalkSpeed()
	{
		final double baseWalkSpd = _activeChar.isInsideZone(ZoneId.WATER) ? getSwimWalkSpeed() : getBaseMoveSpeed(MoveType.WALK) + getMovementSpeedStatBonus();
		if (baseWalkSpd <= 0)
		{
			return 0;
		}
		
		return calcStat(Stats.MOVE_SPEED, baseWalkSpd);
	}
	
	/**
	 * @return the SwimRunSpeed (base+modifier) of the L2Character.
	 */
	public double getSwimRunSpeed()
	{
		final double baseRunSpd = getBaseMoveSpeed(MoveType.FAST_SWIM);
		if (baseRunSpd <= 0)
		{
			return 0;
		}
		
		return calcStat(Stats.MOVE_SPEED, baseRunSpd, null, null);
	}
	
	/**
	 * @return the SwimWalkSpeed (base+modifier) of the L2Character.
	 */
	public double getSwimWalkSpeed()
	{
		final double baseWalkSpd = getBaseMoveSpeed(MoveType.SLOW_SWIM);
		if (baseWalkSpd <= 0)
		{
			return 0;
		}
		
		return calcStat(Stats.MOVE_SPEED, baseWalkSpd);
	}
	
	/**
	 * @param type movement type
	 * @return the base move speed of given movement type.
	 */
	public double getBaseMoveSpeed(MoveType type)
	{
		return _activeChar.getTemplate().getBaseMoveSpeed(type);
	}
	
	/**
	 * @return the RunSpeed (base+modifier) or WalkSpeed (base+modifier) of the L2Character in function of the movement type.
	 */
	public double getMoveSpeed()
	{
		if (_activeChar.isInsideZone(ZoneId.WATER))
		{
			return _activeChar.isRunning() ? getSwimRunSpeed() : getSwimWalkSpeed();
		}
		return _activeChar.isRunning() ? getRunSpeed() : getWalkSpeed();
	}
	
	/**
	 * @param skill
	 * @return the MReuse rate (base+modifier) of the L2Character.
	 */
	public final double getMReuseRate(Skill skill)
	{
		return calcStat(Stats.MAGIC_REUSE_RATE, 1, null, skill);
	}
	
	/**
	 * @param target
	 * @return the PAtk (base+modifier) of the L2Character.
	 */
	public int getPAtk(Creature target)
	{
		float bonusAtk = 1;
		if (Config.L2JMOD_CHAMPION_ENABLE && _activeChar.isChampion())
		{
			bonusAtk = Config.L2JMOD_CHAMPION_ATK;
		}
		if (_activeChar.isRaid())
		{
			bonusAtk *= Config.RAID_PATTACK_MULTIPLIER;
		}
		return (int) calcStat(Stats.POWER_ATTACK, _activeChar.getTemplate().getBasePAtk() * bonusAtk, target, null);
	}
	
	/**
	 * @return the PAtk Speed (base+modifier) of the L2Character in function of the Armour Expertise Penalty.
	 */
	public int getPAtkSpd()
	{
		float bonusAtk = 1;
		if (Config.L2JMOD_CHAMPION_ENABLE && _activeChar.isChampion())
		{
			bonusAtk = Config.L2JMOD_CHAMPION_SPD_ATK;
		}
		return (int) Math.round(calcStat(Stats.POWER_ATTACK_SPEED, _activeChar.getTemplate().getBasePAtkSpd() * bonusAtk, null, null));
	}
	
	/**
	 * @param target
	 * @return the PDef (base+modifier) of the L2Character.
	 */
	public int getPDef(Creature target)
	{
		return (int) calcStat(Stats.POWER_DEFENCE, (_activeChar.isRaid()) ? _activeChar.getTemplate().getBasePDef() * Config.RAID_PDEFENCE_MULTIPLIER : _activeChar.getTemplate().getBasePDef(), target, null);
	}
	
	/**
	 * @return the Physical Attack range (base+modifier) of the L2Character.
	 */
	public final int getPhysicalAttackRange()
	{
		final Weapon weapon = _activeChar.getActiveWeaponItem();
		int baseAttackRange;
		if (_activeChar.isTransformed() && _activeChar.isPlayer())
		{
			baseAttackRange = _activeChar.getTransformation().getBaseAttackRange(_activeChar.getActingPlayer());
		}
		else if (weapon != null)
		{
			baseAttackRange = weapon.getBaseAttackRange();
		}
		else
		{
			baseAttackRange = _activeChar.getTemplate().getBaseAttackRange();
		}
		
		return (int) calcStat(Stats.POWER_ATTACK_RANGE, baseAttackRange, null, null);
	}
	
	public int getPhysicalAttackRadius()
	{
		final Weapon weapon = _activeChar.getActiveWeaponItem();
		final int baseAttackRadius;
		if (weapon != null)
		{
			baseAttackRadius = weapon.getBaseAttackRadius();
		}
		else
		{
			baseAttackRadius = 40;
		}
		return baseAttackRadius;
	}
	
	public int getPhysicalAttackAngle()
	{
		final Weapon weapon = _activeChar.getActiveWeaponItem();
		final int baseAttackAngle;
		if (weapon != null)
		{
			baseAttackAngle = weapon.getBaseAttackAngle();
		}
		else
		{
			baseAttackAngle = 120;
		}
		return baseAttackAngle;
	}
	
	/**
	 * @param target
	 * @return the weapon reuse modifier.
	 */
	public final double getWeaponReuseModifier(Creature target)
	{
		return calcStat(Stats.ATK_REUSE, 1, target, null);
	}
	
	/**
	 * @return the ShieldDef rate (base+modifier) of the L2Character.
	 */
	public final int getShldDef()
	{
		return (int) calcStat(Stats.SHIELD_DEFENCE, 0);
	}
	
	public long getSp()
	{
		return _sp;
	}
	
	public void setSp(long value)
	{
		_sp = value;
	}
	
	/**
	 * @return the STR of the L2Character (base+modifier).
	 */
	public final int getSTR()
	{
		return (int) calcStat(Stats.STAT_STR, _activeChar.getTemplate().getBaseSTR());
	}
	
	/**
	 * @return the WIT of the L2Character (base+modifier).
	 */
	public final int getWIT()
	{
		return (int) calcStat(Stats.STAT_WIT, _activeChar.getTemplate().getBaseWIT());
	}
	
	/**
	 * @param skill
	 * @return the mpConsume.
	 */
	public final int getMpConsume(Skill skill)
	{
		if (skill == null)
		{
			return 1;
		}
		double mpConsume = skill.getMpConsume();
		double nextDanceMpCost = Math.ceil(skill.getMpConsume() / 2.);
		if (skill.isDance())
		{
			if (Config.DANCE_CONSUME_ADDITIONAL_MP && (_activeChar != null) && (_activeChar.getDanceCount() > 0))
			{
				mpConsume += _activeChar.getDanceCount() * nextDanceMpCost;
			}
		}
		
		mpConsume = calcStat(Stats.MP_CONSUME, mpConsume, null, skill);
		
		if (skill.isDance())
		{
			return (int) calcStat(Stats.DANCE_MP_CONSUME_RATE, mpConsume);
		}
		else if (skill.isMagic())
		{
			return (int) calcStat(Stats.MAGICAL_MP_CONSUME_RATE, mpConsume);
		}
		else
		{
			return (int) calcStat(Stats.PHYSICAL_MP_CONSUME_RATE, mpConsume);
		}
	}
	
	/**
	 * @param skill
	 * @return the mpInitialConsume.
	 */
	public final int getMpInitialConsume(Skill skill)
	{
		if (skill == null)
		{
			return 1;
		}
		
		return (int) calcStat(Stats.MP_CONSUME, skill.getMpInitialConsume(), null, skill);
	}
	
	public AttributeType getAttackElement()
	{
		ItemInstance weaponInstance = _activeChar.getActiveWeaponInstance();
		// 1st order - weapon element
		if ((weaponInstance != null) && (weaponInstance.getAttackAttributeType() != AttributeType.NONE))
		{
			return weaponInstance.getAttackAttributeType();
		}
		
		// temp fix starts
		int tempVal = 0, stats[] =
		{
			0,
			0,
			0,
			0,
			0,
			0
		};
		
		AttributeType returnVal = AttributeType.NONE;
		stats[0] = (int) calcStat(Stats.FIRE_POWER, _activeChar.getTemplate().getBaseFire());
		stats[1] = (int) calcStat(Stats.WATER_POWER, _activeChar.getTemplate().getBaseWater());
		stats[2] = (int) calcStat(Stats.WIND_POWER, _activeChar.getTemplate().getBaseWind());
		stats[3] = (int) calcStat(Stats.EARTH_POWER, _activeChar.getTemplate().getBaseEarth());
		stats[4] = (int) calcStat(Stats.HOLY_POWER, _activeChar.getTemplate().getBaseHoly());
		stats[5] = (int) calcStat(Stats.DARK_POWER, _activeChar.getTemplate().getBaseDark());
		
		for (byte x = 0; x < 6; x++)
		{
			if (stats[x] > tempVal)
			{
				returnVal = AttributeType.findByClientId(x);
				tempVal = stats[x];
			}
		}
		
		return returnVal;
	}
	
	public int getAttackElementValue(AttributeType attackAttribute)
	{
		switch (attackAttribute)
		{
			case FIRE:
				return (int) calcStat(Stats.FIRE_POWER, _activeChar.getTemplate().getBaseFire());
			case WATER:
				return (int) calcStat(Stats.WATER_POWER, _activeChar.getTemplate().getBaseWater());
			case WIND:
				return (int) calcStat(Stats.WIND_POWER, _activeChar.getTemplate().getBaseWind());
			case EARTH:
				return (int) calcStat(Stats.EARTH_POWER, _activeChar.getTemplate().getBaseEarth());
			case HOLY:
				return (int) calcStat(Stats.HOLY_POWER, _activeChar.getTemplate().getBaseHoly());
			case DARK:
				return (int) calcStat(Stats.DARK_POWER, _activeChar.getTemplate().getBaseDark());
			default:
				return 0;
		}
	}
	
	public int getDefenseElementValue(AttributeType defenseAttribute)
	{
		switch (defenseAttribute)
		{
			case FIRE:
				return (int) calcStat(Stats.FIRE_RES, _activeChar.getTemplate().getBaseFireRes());
			case WATER:
				return (int) calcStat(Stats.WATER_RES, _activeChar.getTemplate().getBaseWaterRes());
			case WIND:
				return (int) calcStat(Stats.WIND_RES, _activeChar.getTemplate().getBaseWindRes());
			case EARTH:
				return (int) calcStat(Stats.EARTH_RES, _activeChar.getTemplate().getBaseEarthRes());
			case HOLY:
				return (int) calcStat(Stats.HOLY_RES, _activeChar.getTemplate().getBaseHolyRes());
			case DARK:
				return (int) calcStat(Stats.DARK_RES, _activeChar.getTemplate().getBaseDarkRes());
			default:
				return (int) _activeChar.getTemplate().getBaseElementRes();
		}
	}
	
	public float getAttackTrait(TraitType traitType)
	{
		return _attackTraits[traitType.getId()];
	}
	
	public float[] getAttackTraits()
	{
		return _attackTraits;
	}
	
	public boolean hasAttackTrait(TraitType traitType)
	{
		return _attackTraitsCount[traitType.getId()] > 0;
	}
	
	public int[] getAttackTraitsCount()
	{
		return _attackTraitsCount;
	}
	
	public float getDefenceTrait(TraitType traitType)
	{
		return _defenceTraits[traitType.getId()];
	}
	
	public float[] getDefenceTraits()
	{
		return _defenceTraits;
	}
	
	public boolean hasDefenceTrait(TraitType traitType)
	{
		return _defenceTraitsCount[traitType.getId()] > 0;
	}
	
	public int[] getDefenceTraitsCount()
	{
		return _defenceTraitsCount;
	}
	
	public boolean isTraitInvul(TraitType traitType)
	{
		return _traitsInvul[traitType.getId()] > 0;
	}
	
	public int[] getTraitsInvul()
	{
		return _traitsInvul;
	}
	
	/**
	 * Gets the maximum buff count.
	 * @return the maximum buff count
	 */
	public int getMaxBuffCount()
	{
		return _maxBuffCount;
	}
	
	/**
	 * Sets the maximum buff count.
	 * @param buffCount the buff count
	 */
	public void setMaxBuffCount(int buffCount)
	{
		_maxBuffCount = buffCount;
	}
}
