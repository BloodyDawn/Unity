/*
 * Copyright (C) 2004-2015 L2J Server
 * 
 * This file is part of L2J Server.
 * 
 * L2J Server is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * L2J Server is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package org.l2junity.gameserver.model.items;

import java.util.Objects;

import org.l2junity.gameserver.model.StatsSet;
import org.l2junity.gameserver.model.actor.Creature;
import org.l2junity.gameserver.model.actor.Npc;
import org.l2junity.gameserver.model.actor.instance.PlayerInstance;
import org.l2junity.gameserver.model.conditions.Condition;
import org.l2junity.gameserver.model.conditions.ConditionGameChance;
import org.l2junity.gameserver.model.events.EventDispatcher;
import org.l2junity.gameserver.model.events.impl.character.npc.OnNpcSkillSee;
import org.l2junity.gameserver.model.holders.SkillHolder;
import org.l2junity.gameserver.model.items.type.WeaponType;
import org.l2junity.gameserver.model.skills.Skill;
import org.l2junity.gameserver.model.stats.Formulas;
import org.l2junity.gameserver.network.client.send.SystemMessage;
import org.l2junity.gameserver.network.client.send.string.SystemMessageId;
import org.l2junity.gameserver.util.Util;

/**
 * This class is dedicated to the management of weapons.
 */
public final class Weapon extends L2Item
{
	private WeaponType _type;
	private boolean _isMagicWeapon;
	private int _rndDam;
	private int _soulShotCount;
	private int _spiritShotCount;
	private int _mpConsume;
	private int _baseAttackRange;
	private int _baseAttackAngle;
	/**
	 * Skill that activates when item is enchanted +4 (for duals).
	 */
	private SkillHolder _enchant4Skill = null;
	private int _changeWeaponId;
	
	// Attached skills for Special Abilities
	private SkillHolder _skillsOnMagic;
	private Condition _skillsOnMagicCondition = null;
	private SkillHolder _skillsOnCrit;
	private Condition _skillsOnCritCondition = null;
	
	private int _reducedSoulshot;
	private int _reducedSoulshotChance;
	
	private int _reducedMpConsume;
	private int _reducedMpConsumeChance;
	
	private boolean _isForceEquip;
	private boolean _isAttackWeapon;
	private boolean _useWeaponSkillsOnly;
	
	/**
	 * Constructor for Weapon.
	 * @param set the StatsSet designating the set of couples (key,value) characterizing the weapon.
	 */
	public Weapon(StatsSet set)
	{
		super(set);
	}
	
	@Override
	public void set(StatsSet set)
	{
		super.set(set);
		_type = WeaponType.valueOf(set.getString("weapon_type", "none").toUpperCase());
		_type1 = L2Item.TYPE1_WEAPON_RING_EARRING_NECKLACE;
		_type2 = L2Item.TYPE2_WEAPON;
		_isMagicWeapon = set.getBoolean("is_magic_weapon", false);
		_soulShotCount = set.getInt("soulshots", 0);
		_spiritShotCount = set.getInt("spiritshots", 0);
		_rndDam = set.getInt("random_damage", 0);
		_mpConsume = set.getInt("mp_consume", 0);
		_baseAttackRange = set.getInt("attack_range", 40);
		String[] damgeRange = set.getString("damage_range", "").split(";"); // 0?;0?;fan sector;base attack angle
		if ((damgeRange.length > 1) && Util.isDigit(damgeRange[3]))
		{
			_baseAttackAngle = Integer.parseInt(damgeRange[3]);
		}
		else
		{
			_baseAttackAngle = 120;
		}
		
		String[] reduced_soulshots = set.getString("reduced_soulshot", "").split(",");
		_reducedSoulshotChance = (reduced_soulshots.length == 2) ? Integer.parseInt(reduced_soulshots[0]) : 0;
		_reducedSoulshot = (reduced_soulshots.length == 2) ? Integer.parseInt(reduced_soulshots[1]) : 0;
		
		String[] reduced_mpconsume = set.getString("reduced_mp_consume", "").split(",");
		_reducedMpConsumeChance = (reduced_mpconsume.length == 2) ? Integer.parseInt(reduced_mpconsume[0]) : 0;
		_reducedMpConsume = (reduced_mpconsume.length == 2) ? Integer.parseInt(reduced_mpconsume[1]) : 0;
		
		String skill = set.getString("enchant4_skill", null);
		if (skill != null)
		{
			String[] info = skill.split("-");
			
			if ((info != null) && (info.length == 2))
			{
				int id = 0;
				int level = 0;
				try
				{
					id = Integer.parseInt(info[0]);
					level = Integer.parseInt(info[1]);
				}
				catch (Exception nfe)
				{
					// Incorrect syntax, dont add new skill
					_log.info("> Couldnt parse " + skill + " in weapon enchant skills! item " + this);
				}
				if ((id > 0) && (level > 0))
				{
					_enchant4Skill = new SkillHolder(id, level);
				}
			}
		}
		
		skill = set.getString("onmagic_skill", null);
		if (skill != null)
		{
			String[] info = skill.split("-");
			final int chance = set.getInt("onmagic_chance", 100);
			if ((info != null) && (info.length == 2))
			{
				int id = 0;
				int level = 0;
				try
				{
					id = Integer.parseInt(info[0]);
					level = Integer.parseInt(info[1]);
				}
				catch (Exception nfe)
				{
					// Incorrect syntax, don't add new skill
					_log.info("> Couldnt parse " + skill + " in weapon onmagic skills! item " + this);
				}
				if ((id > 0) && (level > 0) && (chance > 0))
				{
					_skillsOnMagic = new SkillHolder(id, level);
					_skillsOnMagicCondition = new ConditionGameChance(chance);
				}
			}
		}
		
		skill = set.getString("oncrit_skill", null);
		if (skill != null)
		{
			String[] info = skill.split("-");
			final int chance = set.getInt("oncrit_chance", 100);
			if ((info != null) && (info.length == 2))
			{
				int id = 0;
				int level = 0;
				try
				{
					id = Integer.parseInt(info[0]);
					level = Integer.parseInt(info[1]);
				}
				catch (Exception nfe)
				{
					// Incorrect syntax, don't add new skill
					_log.info("> Couldnt parse " + skill + " in weapon oncrit skills! item " + this);
				}
				if ((id > 0) && (level > 0) && (chance > 0))
				{
					_skillsOnCrit = new SkillHolder(id, level);
					_skillsOnCritCondition = new ConditionGameChance(chance);
				}
			}
		}
		
		_changeWeaponId = set.getInt("change_weaponId", 0);
		_isForceEquip = set.getBoolean("isForceEquip", false);
		_isAttackWeapon = set.getBoolean("isAttackWeapon", true);
		_useWeaponSkillsOnly = set.getBoolean("useWeaponSkillsOnly", false);
	}
	
	/**
	 * @return the type of Weapon
	 */
	@Override
	public WeaponType getItemType()
	{
		return _type;
	}
	
	/**
	 * @return the ID of the Etc item after applying the mask.
	 */
	@Override
	public int getItemMask()
	{
		return getItemType().mask();
	}
	
	/**
	 * @return {@code true} if the weapon is magic, {@code false} otherwise.
	 */
	@Override
	public boolean isMagicWeapon()
	{
		return _isMagicWeapon;
	}
	
	/**
	 * @return the quantity of SoulShot used.
	 */
	public int getSoulShotCount()
	{
		return _soulShotCount;
	}
	
	/**
	 * @return the quantity of SpiritShot used.
	 */
	public int getSpiritShotCount()
	{
		return _spiritShotCount;
	}
	
	/**
	 * @return the reduced quantity of SoultShot used.
	 */
	public int getReducedSoulShot()
	{
		return _reducedSoulshot;
	}
	
	/**
	 * @return the chance to use Reduced SoultShot.
	 */
	public int getReducedSoulShotChance()
	{
		return _reducedSoulshotChance;
	}
	
	/**
	 * @return the random damage inflicted by the weapon.
	 */
	public int getRandomDamage()
	{
		return _rndDam;
	}
	
	/**
	 * @return the MP consumption with the weapon.
	 */
	public int getMpConsume()
	{
		return _mpConsume;
	}
	
	public int getBaseAttackRange()
	{
		return _baseAttackRange;
	}
	
	public int getBaseAttackAngle()
	{
		return _baseAttackAngle;
	}
	
	/**
	 * @return the reduced MP consumption with the weapon.
	 */
	public int getReducedMpConsume()
	{
		return _reducedMpConsume;
	}
	
	/**
	 * @return the chance to use getReducedMpConsume()
	 */
	public int getReducedMpConsumeChance()
	{
		return _reducedMpConsumeChance;
	}
	
	/**
	 * @return the skill that player get when has equipped weapon +4 or more (for duals SA).
	 */
	@Override
	public Skill getEnchant4Skill()
	{
		if (_enchant4Skill == null)
		{
			return null;
		}
		return _enchant4Skill.getSkill();
	}
	
	/**
	 * @return the Id in which weapon this weapon can be changed.
	 */
	public int getChangeWeaponId()
	{
		return _changeWeaponId;
	}
	
	/**
	 * @return {@code true} if the weapon is force equip, {@code false} otherwise.
	 */
	public boolean isForceEquip()
	{
		return _isForceEquip;
	}
	
	/**
	 * @return {@code true} if the weapon is attack weapon, {@code false} otherwise.
	 */
	public boolean isAttackWeapon()
	{
		return _isAttackWeapon;
	}
	
	/**
	 * @return {@code true} if the weapon is skills only, {@code false} otherwise.
	 */
	public boolean useWeaponSkillsOnly()
	{
		return _useWeaponSkillsOnly;
	}
	
	/**
	 * @param caster the L2Character pointing out the caster
	 * @param target the L2Character pointing out the target
	 */
	public void castOnCriticalSkill(Creature caster, Creature target)
	{
		if ((_skillsOnCrit == null))
		{
			return;
		}
		
		final Skill onCritSkill = _skillsOnCrit.getSkill();
		if (_skillsOnCritCondition != null)
		{
			if (!_skillsOnCritCondition.test(caster, target, onCritSkill))
			{
				// Chance not met
				return;
			}
		}
		
		if (!onCritSkill.checkCondition(caster, target, false))
		{
			// Skill condition not met
			return;
		}
		
		Creature[] targets =
		{
			target
		};
		
		onCritSkill.activateSkill(caster, targets);
	}
	
	/**
	 * @param caster the L2Character pointing out the caster
	 * @param target the L2Character pointing out the target
	 * @param trigger the L2Skill pointing out the skill triggering this action
	 */
	public void castOnMagicSkill(Creature caster, Creature target, Skill trigger)
	{
		if (_skillsOnMagic == null)
		{
			return;
		}
		
		final Skill onMagicSkill = _skillsOnMagic.getSkill();
		
		// Trigger only if both are good or bad magic.
		if (trigger.isBad() != onMagicSkill.isBad())
		{
			return;
		}
		
		// No Trigger if not Magic Skill
		if (!trigger.isMagic() && !onMagicSkill.isMagic())
		{
			return;
		}
		
		if (_skillsOnMagicCondition != null)
		{
			if (!_skillsOnMagicCondition.test(caster, target, onMagicSkill))
			{
				// Chance not met
				return;
			}
		}
		
		if (!onMagicSkill.checkCondition(caster, target, false))
		{
			// Skill condition not met
			return;
		}
		
		if (onMagicSkill.isBad() && (Formulas.calcShldUse(caster, target, onMagicSkill) == Formulas.SHIELD_DEFENSE_PERFECT_BLOCK))
		{
			return;
		}
		
		Creature[] targets =
		{
			target
		};
		
		// Launch the magic skill and calculate its effects
		// Get the skill handler corresponding to the skill type
		onMagicSkill.activateSkill(caster, targets);
		
		// notify quests of a skill use
		if (caster instanceof PlayerInstance)
		{
			//@formatter:off
			caster.getKnownList().getKnownObjects().values().stream()
				.filter(Objects::nonNull)
				.filter(npc -> npc.isNpc())
				.filter(npc -> Util.checkIfInRange(1000, npc, caster, false))
				.forEach(npc -> 
				{
					EventDispatcher.getInstance().notifyEventAsync(new OnNpcSkillSee((Npc) npc, caster.getActingPlayer(), onMagicSkill, targets, false), npc);
				});
			//@formatter:on
		}
		if (caster.isPlayer())
		{
			SystemMessage sm = SystemMessage.getSystemMessage(SystemMessageId.S1_HAS_BEEN_ACTIVATED);
			sm.addSkillName(onMagicSkill);
			caster.sendPacket(sm);
		}
	}
}
