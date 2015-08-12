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
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.stream.Stream;

import org.l2junity.Config;
import org.l2junity.gameserver.enums.AttributeType;
import org.l2junity.gameserver.model.CharEffectList;
import org.l2junity.gameserver.model.actor.Creature;
import org.l2junity.gameserver.model.itemcontainer.Inventory;
import org.l2junity.gameserver.model.items.Weapon;
import org.l2junity.gameserver.model.items.instance.ItemInstance;
import org.l2junity.gameserver.model.skills.BuffInfo;
import org.l2junity.gameserver.model.skills.Skill;
import org.l2junity.gameserver.model.stats.BaseStats;
import org.l2junity.gameserver.model.stats.Stats;
import org.l2junity.gameserver.model.stats.TraitType;
import org.l2junity.gameserver.model.stats.functions.FuncAdd;
import org.l2junity.gameserver.model.stats.functions.FuncMul;
import org.l2junity.gameserver.model.stats.functions.FuncSet;
import org.l2junity.gameserver.model.stats.functions.FuncSub;
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
	
	private final Map<Stats, Double> _statsAdd = new EnumMap<>(Stats.class);
	private final Map<Stats, Double> _statsMul = new EnumMap<>(Stats.class);
	private final ReentrantReadWriteLock _lock = new ReentrantReadWriteLock();
	
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
	 * <B><U> Concept</U> :</B><BR A L2Character owns a table of Calculators called <B>_calculators</B>. Each Calculator (a calculator per state) own a table of Func object. A Func object is a mathematic function that permit to calculate the modifier of a state (ex : REGENERATE_HP_RATE...) : <BR>
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
		return getValue(stat, initVal);
	}
	
	/**
	 * @return the Accuracy (base+modifier) of the L2Character in function of the Weapon Expertise Penalty.
	 */
	public int getAccuracy()
	{
		return (int) getValue(Stats.ACCURACY_COMBAT);
	}
	
	/**
	 * @return the Magic Accuracy (base+modifier) of the L2Character
	 */
	public int getMagicAccuracy()
	{
		return (int) getValue(Stats.ACCURACY_MAGIC);
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
		return (int) getValue(Stats.STAT_CON);
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
		return (int) getValue(Stats.CRITICAL_RATE);
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
		return (int) getValue(Stats.STAT_DEX);
	}
	
	/**
	 * @param target
	 * @return the Attack Evasion rate (base+modifier) of the L2Character.
	 */
	public int getEvasionRate(Creature target)
	{
		return (int) getValue(Stats.EVASION_RATE);
	}
	
	/**
	 * @param target
	 * @return the Attack Evasion rate (base+modifier) of the L2Character.
	 */
	public int getMagicEvasionRate(Creature target)
	{
		return (int) getValue(Stats.MAGIC_EVASION_RATE);
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
		return (int) getValue(Stats.STAT_INT);
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
			return (int) getValue(Stats.MAGIC_ATTACK_RANGE, skill.getCastRange());
		}
		
		return _activeChar.getTemplate().getBaseAttackRange();
	}
	
	public int getMaxCp()
	{
		return (int) getValue(Stats.MAX_CP);
	}
	
	public int getMaxRecoverableCp()
	{
		return (int) calcStat(Stats.MAX_RECOVERABLE_CP, getMaxCp());
	}
	
	public int getMaxHp()
	{
		return (int) getValue(Stats.MAX_HP);
	}
	
	public int getMaxRecoverableHp()
	{
		return (int) calcStat(Stats.MAX_RECOVERABLE_HP, getMaxHp());
	}
	
	public int getMaxMp()
	{
		return (int) getValue(Stats.MAX_MP);
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
		return (int) getValue(Stats.MAGIC_ATTACK);
	}
	
	/**
	 * @return the MAtk Speed (base+modifier) of the L2Character in function of the Armour Expertise Penalty.
	 */
	public int getMAtkSpd()
	{
		return (int) getValue(Stats.MAGIC_ATTACK_SPEED);
	}
	
	/**
	 * @param target
	 * @param skill
	 * @return the Magic Critical Hit rate (base+modifier) of the L2Character.
	 */
	public final int getMCriticalHit(Creature target, Skill skill)
	{
		return (int) getValue(Stats.MCRITICAL_RATE);
	}
	
	/**
	 * <B><U>Example of use </U>: Calculate Magic damage.
	 * @param target The L2Character targeted by the skill
	 * @param skill The L2Skill used against the target
	 * @return the MDef (base+modifier) of the L2Character against a skill in function of abnormal effects in progress.
	 */
	public int getMDef(Creature target, Skill skill)
	{
		return (int) getValue(Stats.MAGIC_DEFENCE);
	}
	
	/**
	 * @return the MEN of the L2Character (base+modifier).
	 */
	public final int getMEN()
	{
		return (int) getValue(Stats.STAT_MEN);
	}
	
	public final int getLUC()
	{
		return (int) getValue(Stats.STAT_LUC);
	}
	
	public final int getCHA()
	{
		return (int) getValue(Stats.STAT_CHA);
	}
	
	public double getMovementSpeedMultiplier()
	{
		double baseSpeed;
		if (_activeChar.isInsideZone(ZoneId.WATER))
		{
			baseSpeed = _activeChar.getTemplate().getBaseValue(_activeChar.isRunning() ? Stats.SWIM_RUN_SPEED : Stats.SWIM_WALK_SPEED, 0);
		}
		else
		{
			baseSpeed = _activeChar.getTemplate().getBaseValue(_activeChar.isRunning() ? Stats.RUN_SPEED : Stats.WALK_SPEED, 0);
		}
		return getMoveSpeed() * (1. / baseSpeed);
	}
	
	/**
	 * @return the RunSpeed (base+modifier) of the L2Character in function of the Armour Expertise Penalty.
	 */
	public double getRunSpeed()
	{
		return getValue(_activeChar.isInsideZone(ZoneId.WATER) ? Stats.SWIM_RUN_SPEED : Stats.RUN_SPEED);
	}
	
	/**
	 * @return the WalkSpeed (base+modifier) of the L2Character.
	 */
	public double getWalkSpeed()
	{
		return getValue(_activeChar.isInsideZone(ZoneId.WATER) ? Stats.SWIM_WALK_SPEED : Stats.WALK_SPEED);
	}
	
	/**
	 * @return the SwimRunSpeed (base+modifier) of the L2Character.
	 */
	public double getSwimRunSpeed()
	{
		return getValue(Stats.SWIM_RUN_SPEED);
	}
	
	/**
	 * @return the SwimWalkSpeed (base+modifier) of the L2Character.
	 */
	public double getSwimWalkSpeed()
	{
		return getValue(Stats.SWIM_WALK_SPEED);
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
		return (int) getValue(Stats.POWER_ATTACK);
	}
	
	/**
	 * @return the PAtk Speed (base+modifier) of the L2Character in function of the Armour Expertise Penalty.
	 */
	public int getPAtkSpd()
	{
		return (int) getValue(Stats.POWER_ATTACK_SPEED);
	}
	
	/**
	 * @param target
	 * @return the PDef (base+modifier) of the L2Character.
	 */
	public int getPDef(Creature target)
	{
		return (int) getValue(Stats.POWER_DEFENCE);
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
		return (int) getValue(Stats.STAT_STR);
	}
	
	/**
	 * @return the WIT of the L2Character (base+modifier).
	 */
	public final int getWIT()
	{
		return (int) getValue(Stats.STAT_WIT);
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
		stats[0] = getAttackElementValue(AttributeType.FIRE);
		stats[1] = getAttackElementValue(AttributeType.WATER);
		stats[2] = getAttackElementValue(AttributeType.WIND);
		stats[3] = getAttackElementValue(AttributeType.EARTH);
		stats[4] = getAttackElementValue(AttributeType.HOLY);
		stats[5] = getAttackElementValue(AttributeType.DARK);
		
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
				return (int) getValue(Stats.FIRE_POWER);
			case WATER:
				return (int) getValue(Stats.WATER_POWER);
			case WIND:
				return (int) getValue(Stats.WIND_POWER);
			case EARTH:
				return (int) getValue(Stats.EARTH_POWER);
			case HOLY:
				return (int) getValue(Stats.HOLY_POWER);
			case DARK:
				return (int) getValue(Stats.DARK_POWER);
			default:
				return 0;
		}
	}
	
	public int getDefenseElementValue(AttributeType defenseAttribute)
	{
		switch (defenseAttribute)
		{
			case FIRE:
				return (int) getValue(Stats.FIRE_RES);
			case WATER:
				return (int) getValue(Stats.WATER_RES);
			case WIND:
				return (int) getValue(Stats.WIND_RES);
			case EARTH:
				return (int) getValue(Stats.EARTH_RES);
			case HOLY:
				return (int) getValue(Stats.HOLY_RES);
			case DARK:
				return (int) getValue(Stats.DARK_RES);
			default:
				return (int) getValue(Stats.BASE_ATTRIBUTE_RES);
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
	
	/**
	 * Merges the stat's value with the values within the map of adds
	 * @param stat
	 * @param val
	 */
	public void mergeAdd(Stats stat, double val)
	{
		_statsAdd.merge(stat, val, stat::add);
	}
	
	/**
	 * Merges the stat's value with the values within the map of muls
	 * @param stat
	 * @param val
	 */
	public void mergeMul(Stats stat, double val)
	{
		_statsMul.merge(stat, val, stat::mul);
	}
	
	/**
	 * @param stat
	 * @return the add value
	 */
	public double getAdd(Stats stat)
	{
		return getAdd(stat, 0d);
	}
	
	/**
	 * @param stat
	 * @param defaultValue
	 * @return the add value
	 */
	public double getAdd(Stats stat, double defaultValue)
	{
		_lock.readLock().lock();
		try
		{
			return _statsAdd.getOrDefault(stat, defaultValue);
		}
		finally
		{
			_lock.readLock().unlock();
		}
	}
	
	/**
	 * @param stat
	 * @return the mul value
	 */
	public double getMul(Stats stat)
	{
		return getMul(stat, 1d);
	}
	
	/**
	 * @param stat
	 * @param defaultValue
	 * @return the mul value
	 */
	public double getMul(Stats stat, double defaultValue)
	{
		_lock.readLock().lock();
		try
		{
			return _statsMul.getOrDefault(stat, defaultValue);
		}
		finally
		{
			_lock.readLock().unlock();
		}
	}
	
	/**
	 * @param stat
	 * @param baseValue
	 * @return the final value of the stat
	 */
	public double getValue(Stats stat, double baseValue)
	{
		return stat.finalize(_activeChar, Optional.of(baseValue));
	}
	
	/**
	 * @param stat
	 * @return the final value of the stat
	 */
	public double getValue(Stats stat)
	{
		return stat.finalize(_activeChar, Optional.empty());
	}
	
	/**
	 * Locks and resets all stats and recalculates all
	 * @param broadcast TODO
	 */
	public void recalculateStats(boolean broadcast)
	{
		_lock.writeLock().lock();
		try
		{
			// Copy old data before wiping it out
			final Map<Stats, Double> adds = !broadcast ? Collections.emptyMap() : new EnumMap<>(_statsAdd);
			final Map<Stats, Double> muls = !broadcast ? Collections.emptyMap() : new EnumMap<>(_statsMul);
			
			// Wipe all the data
			_statsAdd.clear();
			_statsMul.clear();
			
			// Collect all necessary effects
			final CharEffectList effectList = _activeChar.getEffectList();
			final Stream<BuffInfo> passives = effectList.hasPassives() ? effectList.getPassives().stream() : null;
			final Stream<BuffInfo> effectsStream = Stream.concat(effectList.getEffects().stream(), passives != null ? passives : Stream.empty());
			
			// Call pump to each effect
			//@formatter:off
			effectsStream.forEach(info -> info.getEffects().stream()
				.filter(effect -> effect.canStart(info))
				.forEach(effect -> effect.pump(info.getEffected(), info.getSkill())));
			//@formatter:on
			
			final Inventory inventory = _activeChar.getInventory();
			if (inventory != null)
			{
				for (ItemInstance item : inventory.getItems(ItemInstance::isEquipped, ItemInstance::isAugmented))
				{
					item.getAugmentation().applyStats(_activeChar.getActingPlayer());
				}
			}
			
			if (broadcast)
			{
				// Calculate the difference between old and new stats
				final Set<Stats> changed = new HashSet<>();
				for (Stats stat : Stats.values())
				{
					if (_statsAdd.getOrDefault(stat, 0d) != adds.getOrDefault(stat, 0d))
					{
						changed.add(stat);
					}
					else if (_statsMul.getOrDefault(stat, 1d) != muls.getOrDefault(stat, 1d))
					{
						changed.add(stat);
					}
				}
				
				_activeChar.broadcastModifiedStats(changed);
			}
		}
		finally
		{
			_lock.writeLock().unlock();
		}
	}
	
	public void processStats(Creature effected, Class<?> funcClass, Stats stat, double value)
	{
		if (funcClass == FuncSet.class)
		{
			effected.getStat().mergeAdd(stat, value);
		}
		else if (funcClass == FuncAdd.class)
		{
			effected.getStat().mergeAdd(stat, value);
		}
		else if (funcClass == FuncSub.class)
		{
			effected.getStat().mergeAdd(stat, -value);
		}
		else if (funcClass == FuncMul.class)
		{
			effected.getStat().mergeMul(stat, value);
		}
		
		if (stat == Stats.MOVE_SPEED)
		{
			processStats(effected, funcClass, Stats.RUN_SPEED, value);
			processStats(effected, funcClass, Stats.WALK_SPEED, value);
			processStats(effected, funcClass, Stats.SWIM_RUN_SPEED, value);
			processStats(effected, funcClass, Stats.SWIM_WALK_SPEED, value);
			processStats(effected, funcClass, Stats.FLY_RUN_SPEED, value);
			processStats(effected, funcClass, Stats.FLY_WALK_SPEED, value);
		}
	}
}
