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
package org.l2junity.gameserver.model.stats;

import java.util.ArrayList;
import java.util.List;

import org.l2junity.Config;
import org.l2junity.commons.util.CommonUtil;
import org.l2junity.commons.util.Rnd;
import org.l2junity.gameserver.data.xml.impl.HitConditionBonusData;
import org.l2junity.gameserver.data.xml.impl.KarmaData;
import org.l2junity.gameserver.enums.AttributeType;
import org.l2junity.gameserver.enums.BasicProperty;
import org.l2junity.gameserver.instancemanager.CastleManager;
import org.l2junity.gameserver.instancemanager.ClanHallManager;
import org.l2junity.gameserver.instancemanager.FortManager;
import org.l2junity.gameserver.instancemanager.SiegeManager;
import org.l2junity.gameserver.instancemanager.ZoneManager;
import org.l2junity.gameserver.model.SiegeClan;
import org.l2junity.gameserver.model.StatsSet;
import org.l2junity.gameserver.model.actor.Creature;
import org.l2junity.gameserver.model.actor.instance.L2CubicInstance;
import org.l2junity.gameserver.model.actor.instance.L2PetInstance;
import org.l2junity.gameserver.model.actor.instance.L2SiegeFlagInstance;
import org.l2junity.gameserver.model.actor.instance.L2StaticObjectInstance;
import org.l2junity.gameserver.model.actor.instance.PlayerInstance;
import org.l2junity.gameserver.model.effects.EffectFlag;
import org.l2junity.gameserver.model.effects.L2EffectType;
import org.l2junity.gameserver.model.entity.Castle;
import org.l2junity.gameserver.model.entity.ClanHall;
import org.l2junity.gameserver.model.entity.Fort;
import org.l2junity.gameserver.model.entity.Siege;
import org.l2junity.gameserver.model.items.Armor;
import org.l2junity.gameserver.model.items.L2Item;
import org.l2junity.gameserver.model.items.Weapon;
import org.l2junity.gameserver.model.items.type.ArmorType;
import org.l2junity.gameserver.model.items.type.WeaponType;
import org.l2junity.gameserver.model.skills.BuffInfo;
import org.l2junity.gameserver.model.skills.Skill;
import org.l2junity.gameserver.model.zone.ZoneId;
import org.l2junity.gameserver.model.zone.type.CastleZone;
import org.l2junity.gameserver.model.zone.type.ClanHallZone;
import org.l2junity.gameserver.model.zone.type.FortZone;
import org.l2junity.gameserver.model.zone.type.MotherTreeZone;
import org.l2junity.gameserver.network.Debug;
import org.l2junity.gameserver.network.client.send.SystemMessage;
import org.l2junity.gameserver.network.client.send.string.SystemMessageId;
import org.l2junity.gameserver.util.Util;

/**
 * Global calculations.
 */
public final class Formulas
{
	/** Regeneration Task period. */
	private static final int HP_REGENERATE_PERIOD = 3000; // 3 secs
	
	public static final byte SHIELD_DEFENSE_FAILED = 0; // no shield defense
	public static final byte SHIELD_DEFENSE_SUCCEED = 1; // normal shield defense
	public static final byte SHIELD_DEFENSE_PERFECT_BLOCK = 2; // perfect block
	
	private static final byte MELEE_ATTACK_RANGE = 40;
	
	/**
	 * Return the period between 2 regeneration task (3s for L2Character, 5 min for L2DoorInstance).
	 * @param cha
	 * @return
	 */
	public static int getRegeneratePeriod(Creature cha)
	{
		return cha.isDoor() ? HP_REGENERATE_PERIOD * 100 : HP_REGENERATE_PERIOD;
	}
	
	/**
	 * Calculate the HP regen rate (base + modifiers).
	 * @param cha
	 * @return
	 */
	public static double calcHpRegen(Creature cha)
	{
		double init = cha.isPlayer() ? cha.getActingPlayer().getTemplate().getBaseHpRegen(cha.getLevel()) : cha.getTemplate().getBaseHpReg();
		double hpRegenMultiplier = cha.isRaid() ? Config.RAID_HP_REGEN_MULTIPLIER : Config.HP_REGEN_MULTIPLIER;
		double hpRegenBonus = 0;
		
		if (Config.L2JMOD_CHAMPION_ENABLE && cha.isChampion())
		{
			hpRegenMultiplier *= Config.L2JMOD_CHAMPION_HP_REGEN;
		}
		
		if (cha.isPlayer())
		{
			PlayerInstance player = cha.getActingPlayer();
			
			double siegeModifier = calcSiegeRegenModifier(player);
			if (siegeModifier > 0)
			{
				hpRegenMultiplier *= siegeModifier;
			}
			
			if (player.isInsideZone(ZoneId.CLAN_HALL) && (player.getClan() != null) && (player.getClan().getHideoutId() > 0))
			{
				ClanHallZone zone = ZoneManager.getInstance().getZone(player, ClanHallZone.class);
				int posChIndex = zone == null ? -1 : zone.getResidenceId();
				int clanHallIndex = player.getClan().getHideoutId();
				if ((clanHallIndex > 0) && (clanHallIndex == posChIndex))
				{
					ClanHall clansHall = ClanHallManager.getInstance().getClanHallById(clanHallIndex);
					if (clansHall != null)
					{
						if (clansHall.getFunction(ClanHall.FUNC_RESTORE_HP) != null)
						{
							hpRegenMultiplier *= 1 + ((double) clansHall.getFunction(ClanHall.FUNC_RESTORE_HP).getLvl() / 100);
						}
					}
				}
			}
			
			if (player.isInsideZone(ZoneId.CASTLE) && (player.getClan() != null) && (player.getClan().getCastleId() > 0))
			{
				CastleZone zone = ZoneManager.getInstance().getZone(player, CastleZone.class);
				int posCastleIndex = zone == null ? -1 : zone.getResidenceId();
				int castleIndex = player.getClan().getCastleId();
				if ((castleIndex > 0) && (castleIndex == posCastleIndex))
				{
					Castle castle = CastleManager.getInstance().getCastleById(castleIndex);
					if (castle != null)
					{
						if (castle.getFunction(Castle.FUNC_RESTORE_HP) != null)
						{
							hpRegenMultiplier *= 1 + ((double) castle.getFunction(Castle.FUNC_RESTORE_HP).getLvl() / 100);
						}
					}
				}
			}
			
			if (player.isInsideZone(ZoneId.FORT) && (player.getClan() != null) && (player.getClan().getFortId() > 0))
			{
				FortZone zone = ZoneManager.getInstance().getZone(player, FortZone.class);
				int posFortIndex = zone == null ? -1 : zone.getResidenceId();
				int fortIndex = player.getClan().getFortId();
				if ((fortIndex > 0) && (fortIndex == posFortIndex))
				{
					Fort fort = FortManager.getInstance().getFortById(fortIndex);
					if (fort != null)
					{
						if (fort.getFunction(Fort.FUNC_RESTORE_HP) != null)
						{
							hpRegenMultiplier *= 1 + ((double) fort.getFunction(Fort.FUNC_RESTORE_HP).getLvl() / 100);
						}
					}
				}
			}
			
			// Mother Tree effect is calculated at last
			if (player.isInsideZone(ZoneId.MOTHER_TREE))
			{
				MotherTreeZone zone = ZoneManager.getInstance().getZone(player, MotherTreeZone.class);
				int hpBonus = zone == null ? 0 : zone.getHpRegenBonus();
				hpRegenBonus += hpBonus;
			}
			
			// Calculate Movement bonus
			if (player.isSitting())
			{
				hpRegenMultiplier *= 1.5; // Sitting
			}
			else if (!player.isMoving())
			{
				hpRegenMultiplier *= 1.1; // Staying
			}
			else if (player.isRunning())
			{
				hpRegenMultiplier *= 0.7; // Running
			}
			
			// Add CON bonus
			init *= cha.getLevelMod() * BaseStats.CON.calcBonus(cha);
		}
		else if (cha.isPet())
		{
			init = ((L2PetInstance) cha).getPetLevelData().getPetRegenHP() * Config.PET_HP_REGEN_MULTIPLIER;
		}
		
		return (cha.getStat().getValue(Stats.REGENERATE_HP_RATE, Math.max(1, init)) * hpRegenMultiplier) + hpRegenBonus;
	}
	
	/**
	 * Calculate the MP regen rate (base + modifiers).
	 * @param cha
	 * @return
	 */
	public static double calcMpRegen(Creature cha)
	{
		double init = cha.isPlayer() ? cha.getActingPlayer().getTemplate().getBaseMpRegen(cha.getLevel()) : cha.getTemplate().getBaseMpReg();
		double mpRegenMultiplier = cha.isRaid() ? Config.RAID_MP_REGEN_MULTIPLIER : Config.MP_REGEN_MULTIPLIER;
		double mpRegenBonus = 0;
		
		if (cha.isPlayer())
		{
			PlayerInstance player = cha.getActingPlayer();
			
			// Mother Tree effect is calculated at last'
			if (player.isInsideZone(ZoneId.MOTHER_TREE))
			{
				MotherTreeZone zone = ZoneManager.getInstance().getZone(player, MotherTreeZone.class);
				int mpBonus = zone == null ? 0 : zone.getMpRegenBonus();
				mpRegenBonus += mpBonus;
			}
			
			if (player.isInsideZone(ZoneId.CLAN_HALL) && (player.getClan() != null) && (player.getClan().getHideoutId() > 0))
			{
				ClanHallZone zone = ZoneManager.getInstance().getZone(player, ClanHallZone.class);
				int posChIndex = zone == null ? -1 : zone.getResidenceId();
				int clanHallIndex = player.getClan().getHideoutId();
				if ((clanHallIndex > 0) && (clanHallIndex == posChIndex))
				{
					ClanHall clansHall = ClanHallManager.getInstance().getClanHallById(clanHallIndex);
					if (clansHall != null)
					{
						if (clansHall.getFunction(ClanHall.FUNC_RESTORE_MP) != null)
						{
							mpRegenMultiplier *= 1 + ((double) clansHall.getFunction(ClanHall.FUNC_RESTORE_MP).getLvl() / 100);
						}
					}
				}
			}
			
			if (player.isInsideZone(ZoneId.CASTLE) && (player.getClan() != null) && (player.getClan().getCastleId() > 0))
			{
				CastleZone zone = ZoneManager.getInstance().getZone(player, CastleZone.class);
				int posCastleIndex = zone == null ? -1 : zone.getResidenceId();
				int castleIndex = player.getClan().getCastleId();
				if ((castleIndex > 0) && (castleIndex == posCastleIndex))
				{
					Castle castle = CastleManager.getInstance().getCastleById(castleIndex);
					if (castle != null)
					{
						if (castle.getFunction(Castle.FUNC_RESTORE_MP) != null)
						{
							mpRegenMultiplier *= 1 + ((double) castle.getFunction(Castle.FUNC_RESTORE_MP).getLvl() / 100);
						}
					}
				}
			}
			
			if (player.isInsideZone(ZoneId.FORT) && (player.getClan() != null) && (player.getClan().getFortId() > 0))
			{
				FortZone zone = ZoneManager.getInstance().getZone(player, FortZone.class);
				int posFortIndex = zone == null ? -1 : zone.getResidenceId();
				int fortIndex = player.getClan().getFortId();
				if ((fortIndex > 0) && (fortIndex == posFortIndex))
				{
					Fort fort = FortManager.getInstance().getFortById(fortIndex);
					if (fort != null)
					{
						if (fort.getFunction(Fort.FUNC_RESTORE_MP) != null)
						{
							mpRegenMultiplier *= 1 + ((double) fort.getFunction(Fort.FUNC_RESTORE_MP).getLvl() / 100);
						}
					}
				}
			}
			
			// Calculate Movement bonus
			if (player.isSitting())
			{
				mpRegenMultiplier *= 1.5; // Sitting
			}
			else if (!player.isMoving())
			{
				mpRegenMultiplier *= 1.1; // Staying
			}
			else if (player.isRunning())
			{
				mpRegenMultiplier *= 0.7; // Running
			}
			
			// Add MEN bonus
			init *= cha.getLevelMod() * BaseStats.MEN.calcBonus(cha);
		}
		else if (cha.isPet())
		{
			init = ((L2PetInstance) cha).getPetLevelData().getPetRegenMP() * Config.PET_MP_REGEN_MULTIPLIER;
		}
		
		return (cha.getStat().getValue(Stats.REGENERATE_MP_RATE, Math.max(1, init)) * mpRegenMultiplier) + mpRegenBonus;
	}
	
	/**
	 * Calculate the CP regen rate (base + modifiers).
	 * @param player the player
	 * @return
	 */
	public static double calcCpRegen(PlayerInstance player)
	{
		final double init = player.getActingPlayer().getTemplate().getBaseCpRegen(player.getLevel()) * player.getLevelMod() * BaseStats.CON.calcBonus(player);
		double cpRegenMultiplier = Config.CP_REGEN_MULTIPLIER;
		if (player.isSitting())
		{
			cpRegenMultiplier *= 1.5; // Sitting
		}
		else if (!player.isMoving())
		{
			cpRegenMultiplier *= 1.1; // Staying
		}
		else if (player.isRunning())
		{
			cpRegenMultiplier *= 0.7; // Running
		}
		return player.getStat().getValue(Stats.REGENERATE_CP_RATE, Math.max(1, init)) * cpRegenMultiplier;
	}
	
	public static double calcSiegeRegenModifier(PlayerInstance activeChar)
	{
		if ((activeChar == null) || (activeChar.getClan() == null))
		{
			return 0;
		}
		
		Siege siege = SiegeManager.getInstance().getSiege(activeChar.getX(), activeChar.getY(), activeChar.getZ());
		if ((siege == null) || !siege.isInProgress())
		{
			return 0;
		}
		
		SiegeClan siegeClan = siege.getAttackerClan(activeChar.getClan().getId());
		if ((siegeClan == null) || siegeClan.getFlag().isEmpty() || !Util.checkIfInRange(200, activeChar, siegeClan.getFlag().stream().findAny().get(), true))
		{
			return 0;
		}
		
		return 1.5; // If all is true, then modifier will be 50% more
	}
	
	public static double calcBlowDamage(Creature attacker, Creature target, Skill skill, double power, byte shld, boolean ss)
	{
		final double distance = attacker.calculateDistance(target, true, false);
		if (distance > target.getStat().getValue(Stats.DAMAGED_MAX_RANGE, Integer.MAX_VALUE))
		{
			return 0;
		}
		
		double defence = target.getPDef(attacker);
		
		switch (shld)
		{
			case Formulas.SHIELD_DEFENSE_SUCCEED:
			{
				defence += target.getShldDef();
				break;
			}
			case Formulas.SHIELD_DEFENSE_PERFECT_BLOCK: // perfect block
			{
				return 1;
			}
		}
		
		final boolean isPvP = attacker.isPlayable() && target.isPlayable();
		final double ssboost = ss ? 2 : 1;
		final double proximityBonus = attacker.isBehindTarget(true) ? 1.2 : attacker.isInFrontOfTarget() ? 1 : 1.1; // Behind: +20% - Side: +10% (TODO: values are unconfirmed, possibly custom, remove or update when confirmed);
		double damage = 0;
		double pvpBonus = 1;
		
		if (isPvP)
		{
			// Damage bonuses in PvP fight
			pvpBonus = attacker.getStat().getValue(Stats.PVP_PHYS_SKILL_DMG, 1);
			// Defense bonuses in PvP fight
			defence *= target.getStat().getValue(Stats.PVP_PHYS_SKILL_DEF, 1);
		}
		
		// Initial damage
		final double baseMod = ((77 * (power + (attacker.getPAtk(target) * ssboost))) / defence);
		// Critical
		final double criticalMod = (attacker.getStat().getValue(Stats.CRITICAL_DAMAGE, 1));
		final double criticalVulnMod = (target.getStat().getValue(Stats.DEFENCE_CRITICAL_DAMAGE, 1));
		final double criticalAddMod = ((attacker.getStat().getValue(Stats.CRITICAL_DAMAGE_ADD, 0) * 6.1 * 77) / defence);
		final double criticalAddVuln = target.getStat().getValue(Stats.DEFENCE_CRITICAL_DAMAGE_ADD, 0);
		// Trait, elements
		final double weaponTraitMod = calcWeaponTraitBonus(attacker, target);
		final double generalTraitMod = calcGeneralTraitBonus(attacker, target, skill.getTraitType(), false);
		final double attributeMod = calcAttributeBonus(attacker, target, skill);
		final double weaponMod = attacker.getRandomDamageMultiplier();
		
		double penaltyMod = 1;
		if (target.isAttackable() && !target.isRaid() && !target.isRaidMinion() && (target.getLevel() >= Config.MIN_NPC_LVL_DMG_PENALTY) && (attacker.getActingPlayer() != null) && ((target.getLevel() - attacker.getActingPlayer().getLevel()) >= 2))
		{
			int lvlDiff = target.getLevel() - attacker.getActingPlayer().getLevel() - 1;
			if (lvlDiff >= Config.NPC_SKILL_DMG_PENALTY.size())
			{
				penaltyMod *= Config.NPC_SKILL_DMG_PENALTY.get(Config.NPC_SKILL_DMG_PENALTY.size() - 1);
			}
			else
			{
				penaltyMod *= Config.NPC_SKILL_DMG_PENALTY.get(lvlDiff);
			}
		}
		
		damage = (baseMod * criticalMod * criticalVulnMod * proximityBonus * pvpBonus) + criticalAddMod + criticalAddVuln;
		damage *= weaponTraitMod;
		damage *= generalTraitMod;
		damage *= attributeMod;
		damage *= weaponMod;
		damage *= penaltyMod;
		
		if (attacker.isDebug())
		{
			final StatsSet set = new StatsSet();
			set.set("skillPower", power);
			set.set("ssboost", ssboost);
			set.set("proximityBonus", proximityBonus);
			set.set("pvpBonus", pvpBonus);
			set.set("baseMod", baseMod);
			set.set("criticalMod", criticalMod);
			set.set("criticalVulnMod", criticalVulnMod);
			set.set("criticalAddMod", criticalAddMod);
			set.set("criticalAddVuln", criticalAddVuln);
			set.set("weaponTraitMod", weaponTraitMod);
			set.set("generalTraitMod", generalTraitMod);
			set.set("attributeMod", attributeMod);
			set.set("weaponMod", weaponMod);
			set.set("penaltyMod", penaltyMod);
			set.set("damage", (int) damage);
			Debug.sendSkillDebug(attacker, target, skill, set);
		}
		
		return Math.max(damage, 1);
	}
	
	public static double calcBackstabDamage(Creature attacker, Creature target, Skill skill, double power, byte shld, boolean ss)
	{
		final double distance = attacker.calculateDistance(target, true, false);
		if (distance > target.getStat().getValue(Stats.DAMAGED_MAX_RANGE, Integer.MAX_VALUE))
		{
			return 0;
		}
		
		double defence = target.getPDef(attacker);
		
		switch (shld)
		{
			case Formulas.SHIELD_DEFENSE_SUCCEED:
			{
				defence += target.getShldDef();
				break;
			}
			case Formulas.SHIELD_DEFENSE_PERFECT_BLOCK: // perfect block
			{
				return 1;
			}
		}
		
		boolean isPvP = attacker.isPlayable() && target.isPlayer();
		double damage = 0;
		double proximityBonus = attacker.isBehindTarget(true) ? 1.2 : attacker.isInFrontOfTarget() ? 1 : 1.1; // Behind: +20% - Side: +10% (TODO: values are unconfirmed, possibly custom, remove or update when confirmed)
		double ssboost = ss ? 2 : 1;
		double pvpBonus = 1;
		
		if (isPvP)
		{
			// Damage bonuses in PvP fight
			pvpBonus = attacker.getStat().getValue(Stats.PVP_PHYS_SKILL_DMG, 1);
			// Defense bonuses in PvP fight
			defence *= target.getStat().getValue(Stats.PVP_PHYS_SKILL_DEF, 1);
		}
		
		// Initial damage
		double baseMod = ((77 * (power + attacker.getPAtk(target))) / defence) * ssboost;
		// Critical
		double criticalMod = (attacker.getStat().getValue(Stats.CRITICAL_DAMAGE, 1));
		double criticalVulnMod = (target.getStat().getValue(Stats.DEFENCE_CRITICAL_DAMAGE, 1));
		double criticalAddMod = ((attacker.getStat().getValue(Stats.CRITICAL_DAMAGE_ADD, 0) * 6.1 * 77) / defence);
		double criticalAddVuln = target.getStat().getValue(Stats.DEFENCE_CRITICAL_DAMAGE_ADD, 0);
		// Trait, elements
		double generalTraitMod = calcGeneralTraitBonus(attacker, target, skill.getTraitType(), false);
		double attributeMod = calcAttributeBonus(attacker, target, skill);
		double weaponMod = attacker.getRandomDamageMultiplier();
		
		double penaltyMod = 1;
		if (target.isAttackable() && !target.isRaid() && !target.isRaidMinion() && (target.getLevel() >= Config.MIN_NPC_LVL_DMG_PENALTY) && (attacker.getActingPlayer() != null) && ((target.getLevel() - attacker.getActingPlayer().getLevel()) >= 2))
		{
			int lvlDiff = target.getLevel() - attacker.getActingPlayer().getLevel() - 1;
			if (lvlDiff >= Config.NPC_SKILL_DMG_PENALTY.size())
			{
				penaltyMod *= Config.NPC_SKILL_DMG_PENALTY.get(Config.NPC_SKILL_DMG_PENALTY.size() - 1);
			}
			else
			{
				penaltyMod *= Config.NPC_SKILL_DMG_PENALTY.get(lvlDiff);
			}
			
		}
		
		damage = (baseMod * criticalMod * criticalVulnMod * proximityBonus * pvpBonus) + criticalAddMod + criticalAddVuln;
		damage *= generalTraitMod;
		damage *= attributeMod;
		damage *= weaponMod;
		damage *= penaltyMod;
		
		if (attacker.isDebug())
		{
			final StatsSet set = new StatsSet();
			set.set("skillPower", power);
			set.set("ssboost", ssboost);
			set.set("proximityBonus", proximityBonus);
			set.set("pvpBonus", pvpBonus);
			set.set("baseMod", baseMod);
			set.set("criticalMod", criticalMod);
			set.set("criticalVulnMod", criticalVulnMod);
			set.set("criticalAddMod", criticalAddMod);
			set.set("criticalAddVuln", criticalAddVuln);
			set.set("generalTraitMod", generalTraitMod);
			set.set("attributeMod", attributeMod);
			set.set("weaponMod", weaponMod);
			set.set("penaltyMod", penaltyMod);
			set.set("damage", (int) damage);
			Debug.sendSkillDebug(attacker, target, skill, set);
		}
		
		return Math.max(damage, 1);
	}
	
	/**
	 * Calculated damage caused by ATTACK of attacker on target.
	 * @param attacker player or NPC that makes ATTACK
	 * @param target player or NPC, target of ATTACK
	 * @param skill
	 * @param power
	 * @param shld
	 * @param crit if the ATTACK have critical success
	 * @param ss if weapon item was charged by soulshot
	 * @return
	 */
	public static double calcPhysDam(Creature attacker, Creature target, Skill skill, double power, byte shld, boolean crit, boolean ss)
	{
		final boolean isPvP = attacker.isPlayable() && target.isPlayable();
		double damage = attacker.getPAtk(target);
		double defence = target.getPDef(attacker);
		final double distance = attacker.calculateDistance(target, true, false);
		
		if (distance > target.getStat().getValue(Stats.DAMAGED_MAX_RANGE, Integer.MAX_VALUE))
		{
			return 0;
		}
		
		// Defense bonuses in PvP fight
		if (isPvP)
		{
			defence *= (skill == null) ? target.getStat().getValue(Stats.PVP_PHYSICAL_DEF, 1) : target.getStat().getValue(Stats.PVP_PHYS_SKILL_DEF, 1);
		}
		
		switch (shld)
		{
			case SHIELD_DEFENSE_SUCCEED:
			{
				if (!Config.ALT_GAME_SHIELD_BLOCKS)
				{
					defence += target.getShldDef();
				}
				break;
			}
			case SHIELD_DEFENSE_PERFECT_BLOCK: // perfect block
			{
				return 1.;
			}
		}
		
		// Add soulshot boost.
		int ssBoost = ss ? 2 : 1;
		damage = (skill != null) ? ((damage * ssBoost) + power) : (damage * ssBoost);
		
		if (crit)
		{
			// Finally retail like formula
			damage = 2 * attacker.getStat().getValue(Stats.CRITICAL_DAMAGE, 1) * target.getStat().getValue(Stats.DEFENCE_CRITICAL_DAMAGE, 1) * ((70 * damage) / defence);
			// Crit dmg add is almost useless in normal hits...
			damage += ((attacker.getStat().getValue(Stats.CRITICAL_DAMAGE_ADD, 0) * 70) / defence);
			damage += target.getStat().getValue(Stats.DEFENCE_CRITICAL_DAMAGE_ADD, 0);
		}
		else
		{
			damage = (70 * damage) / defence;
		}
		
		damage *= calcAttackTraitBonus(attacker, target);
		
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
			if (skill == null)
			{
				damage *= attacker.getStat().getValue(Stats.PVP_PHYSICAL_DMG, 1);
			}
			else
			{
				damage *= attacker.getStat().getValue(Stats.PVP_PHYS_SKILL_DMG, 1);
			}
		}
		
		// Physical skill dmg boost
		if (skill != null)
		{
			damage = attacker.getStat().getValue(Stats.PHYSICAL_SKILL_POWER, damage);
			if (crit)
			{
				damage = attacker.getStat().getValue(Stats.CRITICAL_DAMAGE_SKILL, damage);
			}
		}
		
		damage *= calcAttributeBonus(attacker, target, skill);
		
		if (target.isAttackable())
		{
			final Weapon weapon = attacker.getActiveWeaponItem();
			if ((weapon != null) && weapon.isBowOrCrossBow())
			{
				if (skill != null)
				{
					damage *= attacker.getStat().getValue(Stats.PVE_BOW_SKILL_DMG, 1);
				}
				else
				{
					damage *= attacker.getStat().getValue(Stats.PVE_BOW_DMG, 1);
				}
			}
			else
			{
				damage *= attacker.getStat().getValue(Stats.PVE_PHYSICAL_DMG, 1);
			}
			if (!target.isRaid() && !target.isRaidMinion() && (target.getLevel() >= Config.MIN_NPC_LVL_DMG_PENALTY) && (attacker.getActingPlayer() != null) && ((target.getLevel() - attacker.getActingPlayer().getLevel()) >= 2))
			{
				int lvlDiff = target.getLevel() - attacker.getActingPlayer().getLevel() - 1;
				if (skill != null)
				{
					if (lvlDiff >= Config.NPC_SKILL_DMG_PENALTY.size())
					{
						damage *= Config.NPC_SKILL_DMG_PENALTY.get(Config.NPC_SKILL_DMG_PENALTY.size() - 1);
					}
					else
					{
						damage *= Config.NPC_SKILL_DMG_PENALTY.get(lvlDiff);
					}
				}
				else if (crit)
				{
					if (lvlDiff >= Config.NPC_CRIT_DMG_PENALTY.size())
					{
						damage *= Config.NPC_CRIT_DMG_PENALTY.get(Config.NPC_CRIT_DMG_PENALTY.size() - 1);
					}
					else
					{
						damage *= Config.NPC_CRIT_DMG_PENALTY.get(lvlDiff);
					}
				}
				else
				{
					if (lvlDiff >= Config.NPC_DMG_PENALTY.size())
					{
						damage *= Config.NPC_DMG_PENALTY.get(Config.NPC_DMG_PENALTY.size() - 1);
					}
					else
					{
						damage *= Config.NPC_DMG_PENALTY.get(lvlDiff);
					}
				}
			}
		}
		return damage;
	}
	
	public static double calcMagicDam(Creature attacker, Creature target, Skill skill, double power, byte shld, boolean sps, boolean bss, boolean mcrit)
	{
		final double distance = attacker.calculateDistance(target, true, false);
		
		if (distance > target.getStat().getValue(Stats.DAMAGED_MAX_RANGE, Integer.MAX_VALUE))
		{
			return 0;
		}
		
		int mDef = target.getMDef(attacker, skill);
		switch (shld)
		{
			case SHIELD_DEFENSE_SUCCEED:
			{
				mDef += target.getShldDef();
				break;
			}
			case SHIELD_DEFENSE_PERFECT_BLOCK:
			{
				return 1;
			}
		}
		
		int mAtk = attacker.getMAtk(target, skill);
		final boolean isPvP = attacker.isPlayable() && target.isPlayable();
		
		// PvP bonuses for defense
		if (isPvP)
		{
			if (skill.isMagic())
			{
				mDef *= target.getStat().getValue(Stats.PVP_MAGICAL_DEF, 1);
			}
			else
			{
				mDef *= target.getStat().getValue(Stats.PVP_PHYS_SKILL_DEF, 1);
			}
		}
		
		// Bonus Spirit shot
		mAtk *= bss ? 4 : sps ? 2 : 1;
		// MDAM Formula.
		double damage = ((91 * Math.sqrt(mAtk)) / mDef) * power;
		
		// Failure calculation
		if (Config.ALT_GAME_MAGICFAILURES && !calcMagicSuccess(attacker, target, skill))
		{
			if (attacker.isPlayer())
			{
				if (calcMagicSuccess(attacker, target, skill) && ((target.getLevel() - attacker.getLevel()) <= 9))
				{
					if (skill.hasEffectType(L2EffectType.HP_DRAIN))
					{
						attacker.sendPacket(SystemMessageId.DRAIN_WAS_ONLY_50_PERCENT_SUCCESSFUL);
					}
					else
					{
						attacker.sendPacket(SystemMessageId.YOUR_ATTACK_HAS_FAILED);
					}
					damage /= 2;
				}
				else
				{
					SystemMessage sm = SystemMessage.getSystemMessage(SystemMessageId.C1_HAS_RESISTED_YOUR_S2);
					sm.addCharName(target);
					sm.addSkillName(skill);
					attacker.sendPacket(sm);
					damage = 1;
				}
			}
			
			if (target.isPlayer())
			{
				final SystemMessage sm = (skill.hasEffectType(L2EffectType.HP_DRAIN)) ? SystemMessage.getSystemMessage(SystemMessageId.YOU_RESISTED_C1_S_DRAIN) : SystemMessage.getSystemMessage(SystemMessageId.YOU_RESISTED_C1_S_MAGIC);
				sm.addCharName(attacker);
				target.sendPacket(sm);
			}
		}
		else if (mcrit)
		{
			damage *= attacker.isPlayer() && target.isPlayer() ? 2 : 3;
			damage *= attacker.getStat().getValue(Stats.MAGIC_CRIT_DMG, 1);
		}
		
		// Weapon random damage
		damage *= attacker.getRandomDamageMultiplier();
		
		// PvP bonuses for damage
		if (isPvP)
		{
			Stats stat = skill.isMagic() ? Stats.PVP_MAGICAL_DMG : Stats.PVP_PHYS_SKILL_DMG;
			damage *= attacker.getStat().getValue(stat, 1);
		}
		
		damage *= calcAttributeBonus(attacker, target, skill);
		
		// Bonus damage if target is affected by Storm Sign
		damage *= target.getStat().getValue(Stats.STORM_SIGN_BONUS, 1);
		
		// Critical damage resistance
		damage *= target.getStat().getValue(Stats.DEFENCE_MAGIC_CRITICAL_DAMAGE, 1);
		
		if (target.isAttackable())
		{
			damage *= attacker.getStat().getValue(Stats.PVE_MAGICAL_DMG, 1);
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
	
	public static double calcMagicDam(L2CubicInstance attacker, Creature target, Skill skill, double power, boolean mcrit, byte shld)
	{
		int mDef = target.getMDef(attacker.getOwner(), skill);
		switch (shld)
		{
			case SHIELD_DEFENSE_SUCCEED:
				mDef += target.getShldDef(); // kamael
				break;
			case SHIELD_DEFENSE_PERFECT_BLOCK: // perfect block
				return 1;
		}
		
		int mAtk = attacker.getCubicPower();
		
		// Cubics MDAM Formula (similar to PDAM formula, but using 91 instead of 70, also resisted by mDef).
		double damage = 91 * ((91 * Math.sqrt(mAtk)) / mDef) * power;
		
		// Failure calculation
		PlayerInstance owner = attacker.getOwner();
		if (Config.ALT_GAME_MAGICFAILURES && !calcMagicSuccess(owner, target, skill))
		{
			if (calcMagicSuccess(owner, target, skill) && ((target.getLevel() - skill.getMagicLevel()) <= 9))
			{
				if (skill.hasEffectType(L2EffectType.HP_DRAIN))
				{
					owner.sendPacket(SystemMessageId.DRAIN_WAS_ONLY_50_PERCENT_SUCCESSFUL);
				}
				else
				{
					owner.sendPacket(SystemMessageId.YOUR_ATTACK_HAS_FAILED);
				}
				damage /= 2;
			}
			else
			{
				SystemMessage sm = SystemMessage.getSystemMessage(SystemMessageId.C1_HAS_RESISTED_YOUR_S2);
				sm.addCharName(target);
				sm.addSkillName(skill);
				owner.sendPacket(sm);
				damage = 1;
			}
			
			if (target.isPlayer())
			{
				if (skill.hasEffectType(L2EffectType.HP_DRAIN))
				{
					SystemMessage sm = SystemMessage.getSystemMessage(SystemMessageId.YOU_RESISTED_C1_S_DRAIN);
					sm.addCharName(owner);
					target.sendPacket(sm);
				}
				else
				{
					SystemMessage sm = SystemMessage.getSystemMessage(SystemMessageId.YOU_RESISTED_C1_S_MAGIC);
					sm.addCharName(owner);
					target.sendPacket(sm);
				}
			}
		}
		else if (mcrit)
		{
			damage *= 3;
		}
		
		damage *= calcAttributeBonus(owner, target, skill);
		
		if (target.isAttackable())
		{
			damage *= attacker.getOwner().getStat().getValue(Stats.PVE_MAGICAL_DMG, 1);
			if (!target.isRaid() && !target.isRaidMinion() && (target.getLevel() >= Config.MIN_NPC_LVL_DMG_PENALTY) && (attacker.getOwner() != null) && ((target.getLevel() - attacker.getOwner().getLevel()) >= 2))
			{
				int lvlDiff = target.getLevel() - attacker.getOwner().getLevel() - 1;
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
	
	/**
	 * Returns true in case of critical hit
	 * @param rate
	 * @param skill
	 * @param activeChar
	 * @param target
	 * @return
	 */
	public static boolean calcCrit(double rate, boolean skill, Creature activeChar, Creature target)
	{
		// Skills are not affected by critical rate defence buffs.
		if (skill)
		{
			// Skill critical rate is calculated up to the first decimal, thats why multiply by 10 and compare to 1000.
			double finalRate = rate * 10 * activeChar.getStat().getSkillCriticalRateBonus();
			return finalRate > Rnd.get(1000);
		}
		
		// In retail, it appears that when you are higher level attacking lower level mobs, your critical rate is much higher.
		// Level 91 attacking level 1 appear that nearly all hits are critical. Unconfirmed for skills and pvp.
		if (activeChar.isNpc() || target.isNpc())
		{
			final double levelMod = 1 + (activeChar.getLevelMod() - target.getLevelMod());
			rate *= levelMod;
		}
		
		double finalRate = target.getStat().getValue(Stats.DEFENCE_CRITICAL_RATE, rate) + target.getStat().getValue(Stats.DEFENCE_CRITICAL_RATE_ADD, 0);
		return finalRate > Rnd.get(1000);
	}
	
	public static boolean calcMCrit(double mRate, Skill skill, Creature target)
	{
		if ((target == null) || (skill == null) || !skill.isBad())
		{
			// Juji Producer Update: The devs said that 32% is the maximum critical chance for magic spells and that it is intended for balance reasons. It may change in a future update though.
			if (mRate > 320)
			{
				mRate = 320;
			}
			
			return mRate > Rnd.get(1000);
		}
		
		double finalRate = target.getStat().getValue(Stats.DEFENCE_MAGIC_CRITICAL_RATE, mRate) + target.getStat().getValue(Stats.DEFENCE_MAGIC_CRITICAL_RATE_ADD, 0);
		// Juji Producer Update: The devs said that 32% is the maximum critical chance for magic spells and that it is intended for balance reasons. It may change in a future update though.
		if (finalRate > 320)
		{
			finalRate = 320;
		}
		return finalRate > Rnd.get(1000);
	}
	
	/**
	 * @param target
	 * @param dmg
	 * @return true in case when ATTACK is canceled due to hit
	 */
	public static boolean calcAtkBreak(Creature target, double dmg)
	{
		if (target.isChanneling())
		{
			return false;
		}
		
		double init = 0;
		
		if (Config.ALT_GAME_CANCEL_CAST && target.isCastingNow())
		{
			init = 15;
		}
		if (Config.ALT_GAME_CANCEL_BOW && target.isAttackingNow())
		{
			Weapon wpn = target.getActiveWeaponItem();
			if ((wpn != null) && (wpn.getItemType() == WeaponType.BOW))
			{
				init = 15;
			}
		}
		
		if (target.isRaid() || target.isInvul() || (init <= 0))
		{
			return false; // No attack break
		}
		
		// Chance of break is higher with higher dmg
		init += Math.sqrt(13 * dmg);
		
		// Chance is affected by target MEN
		init -= ((BaseStats.MEN.calcBonus(target) * 100) - 100);
		
		// Calculate all modifiers for ATTACK_CANCEL
		double rate = target.getStat().getValue(Stats.ATTACK_CANCEL, init);
		
		// Adjust the rate to be between 1 and 99
		rate = Math.max(Math.min(rate, 99), 1);
		
		return Rnd.get(100) < rate;
	}
	
	/**
	 * Calculate delay (in milliseconds) before next ATTACK
	 * @param attacker
	 * @param target
	 * @param rate
	 * @return
	 */
	public static int calcPAtkSpd(Creature attacker, Creature target, double rate)
	{
		// measured Oct 2006 by Tank6585, formula by Sami
		// attack speed 312 equals 1500 ms delay... (or 300 + 40 ms delay?)
		if (rate < 2)
		{
			return 2700;
		}
		return (int) (470000 / rate);
	}
	
	/**
	 * Calculate delay (in milliseconds) for skills cast
	 * @param attacker
	 * @param skill
	 * @param skillTime
	 * @return
	 */
	public static int calcAtkSpd(Creature attacker, Skill skill, double skillTime)
	{
		if (skill.isMagic())
		{
			return (int) ((skillTime / attacker.getMAtkSpd()) * 333);
		}
		return (int) ((skillTime / attacker.getPAtkSpd()) * 300);
	}
	
	/**
	 * Formula based on http://l2p.l2wh.com/nonskillattacks.html
	 * @param attacker
	 * @param target
	 * @return {@code true} if hit missed (target evaded), {@code false} otherwise.
	 */
	public static boolean calcHitMiss(Creature attacker, Creature target)
	{
		int chance = (80 + (2 * (attacker.getAccuracy() - target.getEvasionRate(attacker)))) * 10;
		
		// Get additional bonus from the conditions when you are attacking
		chance *= HitConditionBonusData.getInstance().getConditionBonus(attacker, target);
		
		chance = Math.max(chance, 200);
		chance = Math.min(chance, 980);
		
		return chance < Rnd.get(1000);
	}
	
	/**
	 * Returns:<br>
	 * 0 = shield defense doesn't succeed<br>
	 * 1 = shield defense succeed<br>
	 * 2 = perfect block<br>
	 * @param attacker
	 * @param target
	 * @param skill
	 * @param sendSysMsg
	 * @return
	 */
	public static byte calcShldUse(Creature attacker, Creature target, Skill skill, boolean sendSysMsg)
	{
		L2Item item = target.getSecondaryWeaponItem();
		if ((item == null) || !(item instanceof Armor) || (((Armor) item).getItemType() == ArmorType.SIGIL))
		{
			return 0;
		}
		
		double shldRate = target.getStat().getValue(Stats.SHIELD_RATE, 0) * BaseStats.DEX.calcBonus(target);
		if (shldRate <= 1e-6)
		{
			return 0;
		}
		
		int degreeside = (int) target.getStat().getValue(Stats.SHIELD_DEFENCE_ANGLE, 0) + 120;
		if ((degreeside < 360) && (!target.isFacing(attacker, degreeside)))
		{
			return 0;
		}
		
		byte shldSuccess = SHIELD_DEFENSE_FAILED;
		// if attacker
		// if attacker use bow and target wear shield, shield block rate is multiplied by 1.3 (30%)
		Weapon at_weapon = attacker.getActiveWeaponItem();
		if ((at_weapon != null) && (at_weapon.getItemType() == WeaponType.BOW))
		{
			shldRate *= 1.3;
		}
		
		if ((shldRate > 0) && ((100 - Config.ALT_PERFECT_SHLD_BLOCK) < Rnd.get(100)))
		{
			shldSuccess = SHIELD_DEFENSE_PERFECT_BLOCK;
		}
		else if (shldRate > Rnd.get(100))
		{
			shldSuccess = SHIELD_DEFENSE_SUCCEED;
		}
		
		if (sendSysMsg && target.isPlayer())
		{
			PlayerInstance enemy = target.getActingPlayer();
			
			switch (shldSuccess)
			{
				case SHIELD_DEFENSE_SUCCEED:
					enemy.sendPacket(SystemMessageId.YOUR_SHIELD_DEFENSE_HAS_SUCCEEDED);
					break;
				case SHIELD_DEFENSE_PERFECT_BLOCK:
					enemy.sendPacket(SystemMessageId.YOUR_EXCELLENT_SHIELD_DEFENSE_WAS_A_SUCCESS);
					break;
			}
		}
		
		return shldSuccess;
	}
	
	public static byte calcShldUse(Creature attacker, Creature target, Skill skill)
	{
		return calcShldUse(attacker, target, skill, true);
	}
	
	public static byte calcShldUse(Creature attacker, Creature target)
	{
		return calcShldUse(attacker, target, null, true);
	}
	
	public static boolean calcMagicAffected(Creature actor, Creature target, Skill skill)
	{
		// TODO: CHECK/FIX THIS FORMULA UP!!
		double defence = 0;
		if (skill.isActive() && skill.isBad())
		{
			defence = target.getMDef(actor, skill);
		}
		
		double attack = 2 * actor.getMAtk(target, skill) * calcGeneralTraitBonus(actor, target, skill.getTraitType(), false);
		double d = (attack - defence) / (attack + defence);
		
		if (skill.isDebuff())
		{
			if (target.getStat().getValue(Stats.DEBUFF_IMMUNITY, 0) > 0)
			{
				return false;
			}
		}
		
		d += 0.5 * Rnd.nextGaussian();
		return d > 0;
	}
	
	public static double calcLvlBonusMod(Creature attacker, Creature target, Skill skill)
	{
		int attackerLvl = skill.getMagicLevel() > 0 ? skill.getMagicLevel() : attacker.getLevel();
		double skillLvlBonusRateMod = 1 + (skill.getLvlBonusRate() / 100.);
		double lvlMod = 1 + ((attackerLvl - target.getLevel()) / 100.);
		return skillLvlBonusRateMod * lvlMod;
	}
	
	/**
	 * Calculates the effect landing success.<br>
	 * @param attacker the attacker
	 * @param target the target
	 * @param skill the skill
	 * @return {@code true} if the effect lands
	 */
	public static boolean calcEffectSuccess(Creature attacker, Creature target, Skill skill)
	{
		// StaticObjects can not receive continuous effects.
		if (target.isDoor() || (target instanceof L2SiegeFlagInstance) || (target instanceof L2StaticObjectInstance))
		{
			return false;
		}
		
		if (skill.isDebuff())
		{
			boolean resisted = target.getStat().getValue(Stats.DEBUFF_IMMUNITY, 0) > 0;
			if (!resisted)
			{
				final double distance = attacker.calculateDistance(target, true, false);
				if (distance > target.getStat().getValue(Stats.DEBUFFED_MAX_RANGE, Integer.MAX_VALUE))
				{
					resisted = true;
				}
			}
			if (!resisted && target.isAffected(EffectFlag.DEBUFF_BLOCK))
			{
				int times = target.getDebuffBlockedTime();
				if (times > 0)
				{
					times = target.decrementDebuffBlockTimes();
					if (times == 0)
					{
						target.stopEffects(L2EffectType.DEBUFF_BLOCK);
					}
				}
				else
				{
					target.stopEffects(L2EffectType.DEBUFF_BLOCK);
				}
				resisted = true;
			}
			
			resisted |= target.isCastingNow(s -> s.getSkill().getAbnormalResists().contains(skill.getAbnormalType()));
			
			if (resisted)
			{
				final SystemMessage sm = SystemMessage.getSystemMessage(SystemMessageId.C1_HAS_RESISTED_YOUR_S2);
				sm.addCharName(target);
				sm.addSkillName(skill);
				attacker.sendPacket(sm);
				return false;
			}
		}
		
		final int activateRate = skill.getActivateRate();
		if ((activateRate == -1))
		{
			return true;
		}
		
		int magicLevel = skill.getMagicLevel();
		if (magicLevel <= -1)
		{
			magicLevel = target.getLevel() + 3;
		}
		
		double targetBasicProperty = getAbnormalResist(skill.getBasicProperty(), target);
		final double baseMod = ((((((magicLevel - target.getLevel()) + 3) * skill.getLvlBonusRate()) + activateRate) + 30.0) - targetBasicProperty);
		final double elementMod = calcAttributeBonus(attacker, target, skill);
		final double traitMod = calcGeneralTraitBonus(attacker, target, skill.getTraitType(), false);
		final double basicPropertyResist = getBasicPropertyResistBonus(skill.getBasicProperty(), target);
		Stats stat = skill.isDebuff() ? Stats.DEBUFF_VULN : Stats.BUFF_VULN;
		final double buffDebuffMod = 1 + (target.getStat().getValue(stat, 1) / 100);
		final double rate = baseMod * elementMod * traitMod * buffDebuffMod;
		final double finalRate = traitMod > 0 ? CommonUtil.constrain(rate, skill.getMinChance(), skill.getMaxChance()) * basicPropertyResist : 0;
		
		if (attacker.isDebug())
		{
			final StatsSet set = new StatsSet();
			set.set("baseMod", baseMod);
			set.set("elementMod", elementMod);
			set.set("traitMod", traitMod);
			set.set("buffDebuffMod", buffDebuffMod);
			set.set("rate", rate);
			set.set("finalRate", finalRate);
			Debug.sendSkillDebug(attacker, target, skill, set);
		}
		
		if (finalRate <= Rnd.get(100))
		{
			final SystemMessage sm = SystemMessage.getSystemMessage(SystemMessageId.C1_HAS_RESISTED_YOUR_S2);
			sm.addCharName(target);
			sm.addSkillName(skill);
			attacker.sendPacket(sm);
			return false;
		}
		return true;
	}
	
	public static boolean calcCubicSkillSuccess(L2CubicInstance attacker, Creature target, Skill skill, byte shld)
	{
		if (skill.isDebuff())
		{
			if (skill.getActivateRate() == -1)
			{
				return true;
			}
			else if (target.getStat().getValue(Stats.DEBUFF_IMMUNITY, 0) > 0)
			{
				return false;
			}
		}
		
		// Perfect Shield Block.
		if (shld == SHIELD_DEFENSE_PERFECT_BLOCK)
		{
			return false;
		}
		
		// if target reflect this skill then the effect will fail
		if (calcBuffDebuffReflection(target, skill))
		{
			return false;
		}
		
		double targetBasicProperty = getAbnormalResist(skill.getBasicProperty(), target);
		
		// Calculate BaseRate.
		double baseRate = skill.getActivateRate();
		double statMod = 1 + (targetBasicProperty / 100);
		double rate = (baseRate / statMod);
		
		// Resist Modifier.
		double resMod = calcGeneralTraitBonus(attacker.getOwner(), target, skill.getTraitType(), false);
		rate *= resMod;
		
		// Lvl Bonus Modifier.
		double lvlBonusMod = calcLvlBonusMod(attacker.getOwner(), target, skill);
		rate *= lvlBonusMod;
		
		// Element Modifier.
		double elementMod = calcAttributeBonus(attacker.getOwner(), target, skill);
		rate *= elementMod;
		
		final double basicPropertyResist = getBasicPropertyResistBonus(skill.getBasicProperty(), target);
		
		// Add Matk/Mdef Bonus (TODO: Pending)
		
		// Check the Rate Limits.
		final double finalRate = CommonUtil.constrain(rate, skill.getMinChance(), skill.getMaxChance()) * basicPropertyResist;
		
		if (attacker.getOwner().isDebug())
		{
			final StatsSet set = new StatsSet();
			set.set("baseMod", baseRate);
			set.set("resMod", resMod);
			set.set("statMod", statMod);
			set.set("elementMod", elementMod);
			set.set("lvlBonusMod", lvlBonusMod);
			set.set("rate", rate);
			set.set("finalRate", finalRate);
			Debug.sendSkillDebug(attacker.getOwner(), target, skill, set);
		}
		
		return (Rnd.get(100) < finalRate);
	}
	
	public static boolean calcMagicSuccess(Creature attacker, Creature target, Skill skill)
	{
		// FIXME: Fix this LevelMod Formula.
		int lvlDifference = (target.getLevel() - (skill.getMagicLevel() > 0 ? skill.getMagicLevel() : attacker.getLevel()));
		double lvlModifier = Math.pow(1.3, lvlDifference);
		float targetModifier = 1;
		if (target.isAttackable() && !target.isRaid() && !target.isRaidMinion() && (target.getLevel() >= Config.MIN_NPC_LVL_MAGIC_PENALTY) && (attacker.getActingPlayer() != null) && ((target.getLevel() - attacker.getActingPlayer().getLevel()) >= 3))
		{
			int lvlDiff = target.getLevel() - attacker.getActingPlayer().getLevel() - 2;
			if (lvlDiff >= Config.NPC_SKILL_CHANCE_PENALTY.size())
			{
				targetModifier = Config.NPC_SKILL_CHANCE_PENALTY.get(Config.NPC_SKILL_CHANCE_PENALTY.size() - 1);
			}
			else
			{
				targetModifier = Config.NPC_SKILL_CHANCE_PENALTY.get(lvlDiff);
			}
		}
		// general magic resist
		final double resModifier = target.getStat().getValue(Stats.MAGIC_SUCCESS_RES, 1);
		int rate = 100 - Math.round((float) (lvlModifier * targetModifier * resModifier));
		
		if (attacker.isDebug())
		{
			final StatsSet set = new StatsSet();
			set.set("lvlDifference", lvlDifference);
			set.set("lvlModifier", lvlModifier);
			set.set("resModifier", resModifier);
			set.set("targetModifier", targetModifier);
			set.set("rate", rate);
			Debug.sendSkillDebug(attacker, target, skill, set);
		}
		
		return (Rnd.get(100) < rate);
	}
	
	public static double calcManaDam(Creature attacker, Creature target, Skill skill, double power, byte shld, boolean sps, boolean bss, boolean mcrit)
	{
		// Formula: (SQR(M.Atk)*Power*(Target Max MP/97))/M.Def
		double mAtk = attacker.getMAtk(target, skill);
		double mDef = target.getMDef(attacker, skill);
		double mp = target.getMaxMp();
		
		switch (shld)
		{
			case SHIELD_DEFENSE_SUCCEED:
				mDef += target.getShldDef();
				break;
			case SHIELD_DEFENSE_PERFECT_BLOCK: // perfect block
				return 1;
		}
		
		// Bonus Spiritshot
		mAtk *= bss ? 4 : sps ? 2 : 1;
		
		double damage = (Math.sqrt(mAtk) * power * (mp / 97)) / mDef;
		damage *= calcGeneralTraitBonus(attacker, target, skill.getTraitType(), false);
		
		if (target.isAttackable())
		{
			damage *= attacker.getStat().getValue(Stats.PVE_MAGICAL_DMG, 1);
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
		
		// Failure calculation
		if (Config.ALT_GAME_MAGICFAILURES && !calcMagicSuccess(attacker, target, skill))
		{
			if (attacker.isPlayer())
			{
				SystemMessage sm = SystemMessage.getSystemMessage(SystemMessageId.DAMAGE_IS_DECREASED_BECAUSE_C1_RESISTED_C2_S_MAGIC);
				sm.addCharName(target);
				sm.addCharName(attacker);
				attacker.sendPacket(sm);
				damage /= 2;
			}
			
			if (target.isPlayer())
			{
				SystemMessage sm2 = SystemMessage.getSystemMessage(SystemMessageId.C1_WEAKLY_RESISTED_C2_S_MAGIC);
				sm2.addCharName(target);
				sm2.addCharName(attacker);
				target.sendPacket(sm2);
			}
		}
		
		if (mcrit)
		{
			damage *= 3;
			attacker.sendPacket(SystemMessageId.M_CRITICAL);
		}
		return damage;
	}
	
	public static double calculateSkillResurrectRestorePercent(double baseRestorePercent, Creature caster)
	{
		if ((baseRestorePercent == 0) || (baseRestorePercent == 100))
		{
			return baseRestorePercent;
		}
		
		double restorePercent = baseRestorePercent * BaseStats.WIT.calcBonus(caster);
		if ((restorePercent - baseRestorePercent) > 20.0)
		{
			restorePercent += 20.0;
		}
		
		restorePercent = Math.max(restorePercent, baseRestorePercent);
		restorePercent = Math.min(restorePercent, 90.0);
		
		return restorePercent;
	}
	
	public static boolean calcPhysicalSkillEvasion(Creature activeChar, Creature target, Skill skill)
	{
		if (skill.isMagic() || skill.isDebuff())
		{
			return false;
		}
		if (Rnd.get(100) < target.getStat().getValue(Stats.P_SKILL_EVASION, 0))
		{
			if (activeChar.isPlayer())
			{
				SystemMessage sm = SystemMessage.getSystemMessage(SystemMessageId.C1_DODGED_THE_ATTACK);
				sm.addString(target.getName());
				activeChar.getActingPlayer().sendPacket(sm);
			}
			if (target.isPlayer())
			{
				SystemMessage sm = SystemMessage.getSystemMessage(SystemMessageId.YOU_HAVE_DODGED_C1_S_ATTACK);
				sm.addString(activeChar.getName());
				target.getActingPlayer().sendPacket(sm);
			}
			return true;
		}
		return false;
	}
	
	public static boolean calcSkillMastery(Creature actor, Skill sk)
	{
		// Static Skills are not affected by Skill Mastery.
		if (sk.isStatic())
		{
			return false;
		}
		
		final int val = (int) actor.getStat().getValue(Stats.SKILL_CRITICAL, 0);
		
		if (val == 0)
		{
			return false;
		}
		
		if (actor.isPlayer())
		{
			double initVal = 0;
			switch (val)
			{
				case 1:
				{
					initVal = (BaseStats.STR).calcBonus(actor);
					break;
				}
				case 4:
				{
					initVal = (BaseStats.INT).calcBonus(actor);
					break;
				}
			}
			initVal *= actor.getStat().getValue(Stats.SKILL_CRITICAL_PROBABILITY, 1);
			return (Rnd.get(100) < initVal);
		}
		
		return false;
	}
	
	/**
	 * Calculates the Attribute Bonus
	 * @param attacker
	 * @param target
	 * @param skill Can be {@code null} if there is no skill used for the attack.
	 * @return The attribute bonus
	 */
	public static double calcAttributeBonus(Creature attacker, Creature target, Skill skill)
	{
		int attack_attribute;
		int defence_attribute;
		
		if (skill != null)
		{
			if ((skill.getAttributeType() == AttributeType.NONE) || (skill.getAttributeType() == AttributeType.NONE_ARMOR))
			{
				attack_attribute = 0;
				defence_attribute = target.getDefenseElementValue(AttributeType.NONE_ARMOR);
			}
			else
			{
				if (attacker.getAttackElement() == skill.getAttributeType())
				{
					attack_attribute = attacker.getAttackElementValue(attacker.getAttackElement()) + skill.getAttributeValue();
					defence_attribute = target.getDefenseElementValue(attacker.getAttackElement());
				}
				else
				{
					attack_attribute = skill.getAttributeValue();
					defence_attribute = target.getDefenseElementValue(skill.getAttributeType());
				}
			}
		}
		else
		{
			attack_attribute = attacker.getAttackElementValue(attacker.getAttackElement());
			defence_attribute = target.getDefenseElementValue(attacker.getAttackElement());
		}
		
		double attack_attribute_mod = 0;
		double defence_attribute_mod = 0;
		
		if (attack_attribute >= 450)
		{
			if (defence_attribute >= 450)
			{
				attack_attribute_mod = 0.06909;
				defence_attribute_mod = 0.078;
			}
			else if (attack_attribute >= 350)
			{
				attack_attribute_mod = 0.0887;
				defence_attribute_mod = 0.1007;
			}
			else
			{
				attack_attribute_mod = 0.129;
				defence_attribute_mod = 0.1473;
			}
		}
		else if (attack_attribute >= 300)
		{
			if (defence_attribute >= 300)
			{
				attack_attribute_mod = 0.0887;
				defence_attribute_mod = 0.1007;
			}
			else if (defence_attribute >= 150)
			{
				attack_attribute_mod = 0.129;
				defence_attribute_mod = 0.1473;
			}
			else
			{
				attack_attribute_mod = 0.25;
				defence_attribute_mod = 0.2894;
			}
		}
		else if (attack_attribute >= 150)
		{
			if (defence_attribute >= 150)
			{
				attack_attribute_mod = 0.129;
				defence_attribute_mod = 0.1473;
			}
			else if (defence_attribute >= 0)
			{
				attack_attribute_mod = 0.25;
				defence_attribute_mod = 0.2894;
			}
			else
			{
				attack_attribute_mod = 0.4;
				defence_attribute_mod = 0.55;
			}
		}
		else if (attack_attribute >= -99)
		{
			if (defence_attribute >= 0)
			{
				attack_attribute_mod = 0.25;
				defence_attribute_mod = 0.2894;
			}
			else
			{
				attack_attribute_mod = 0.4;
				defence_attribute_mod = 0.55;
			}
		}
		else
		{
			if (defence_attribute >= 450)
			{
				attack_attribute_mod = 0.06909;
				defence_attribute_mod = 0.078;
			}
			else if (defence_attribute >= 350)
			{
				attack_attribute_mod = 0.0887;
				defence_attribute_mod = 0.1007;
			}
			else
			{
				attack_attribute_mod = 0.129;
				defence_attribute_mod = 0.1473;
			}
		}
		
		int attribute_diff = attack_attribute - defence_attribute;
		double min;
		double max;
		if (attribute_diff >= 300)
		{
			max = 100.0;
			min = -50;
		}
		else if (attribute_diff >= 150)
		{
			max = 70.0;
			min = -50;
		}
		else if (attribute_diff >= -150)
		{
			max = 40.0;
			min = -50;
		}
		else if (attribute_diff >= -300)
		{
			max = 40.0;
			min = -60;
		}
		else
		{
			max = 40.0;
			min = -80;
		}
		
		attack_attribute += 100;
		attack_attribute *= attack_attribute;
		
		attack_attribute_mod = (attack_attribute / 144.0) * attack_attribute_mod;
		
		defence_attribute += 100;
		defence_attribute *= defence_attribute;
		
		defence_attribute_mod = (defence_attribute / 169.0) * defence_attribute_mod;
		
		double attribute_mod_diff = attack_attribute_mod - defence_attribute_mod;
		
		attribute_mod_diff = CommonUtil.constrain(attribute_mod_diff, min, max);
		
		double result = (attribute_mod_diff / 100.0) + 1;
		
		if (attacker.isPlayer() && target.isPlayer() && (result < 1.0))
		{
			result = 1.0;
		}
		
		return result;
	}
	
	public static void calcDamageReflected(Creature attacker, Creature target, Skill skill, boolean crit)
	{
		// Only melee skills can be reflected
		if (skill.isMagic() || (skill.getCastRange() > MELEE_ATTACK_RANGE))
		{
			return;
		}
		
		final double chance = target.getStat().getValue(Stats.VENGEANCE_SKILL_PHYSICAL_DAMAGE, 0);
		if (Rnd.get(100) < chance)
		{
			if (target.isPlayer())
			{
				final SystemMessage sm = SystemMessage.getSystemMessage(SystemMessageId.YOU_COUNTERED_C1_S_ATTACK);
				sm.addCharName(attacker);
				target.sendPacket(sm);
			}
			if (attacker.isPlayer())
			{
				final SystemMessage sm = SystemMessage.getSystemMessage(SystemMessageId.C1_IS_PERFORMING_A_COUNTERATTACK);
				sm.addCharName(target);
				attacker.sendPacket(sm);
			}
			
			double counterdmg = ((target.getPAtk(attacker) * 873) / attacker.getPDef(target)); // Old: (((target.getPAtk(attacker) * 10.0) * 70.0) / attacker.getPDef(target));
			counterdmg *= calcWeaponTraitBonus(attacker, target);
			counterdmg *= calcGeneralTraitBonus(attacker, target, skill.getTraitType(), false);
			counterdmg *= calcAttributeBonus(attacker, target, skill);
			
			// TODO: It counters multiple times depending on how much attack effects skill has, but gotta be verified which effects are attack effects and do multiple effects of the same type stack!
			if (skill.hasEffectType(L2EffectType.PHYSICAL_ATTACK, L2EffectType.PHYSICAL_ATTACK_HP_LINK))
			{
				attacker.reduceCurrentHp(counterdmg, target, skill);
			}
			if (skill.hasEffectType(L2EffectType.LETHAL_ATTACK))
			{
				attacker.reduceCurrentHp(counterdmg, target, skill);
			}
		}
	}
	
	/**
	 * Calculate buff/debuff reflection.
	 * @param target
	 * @param skill
	 * @return {@code true} if reflect, {@code false} otherwise.
	 */
	public static boolean calcBuffDebuffReflection(Creature target, Skill skill)
	{
		if (!skill.isDebuff() || (skill.getActivateRate() == -1))
		{
			return false;
		}
		Stats stat = skill.isMagic() ? Stats.REFLECT_SKILL_MAGIC : Stats.REFLECT_SKILL_PHYSIC;
		final double reflectChance = target.getStat().getValue(stat, 0);
		return reflectChance > Rnd.get(100);
	}
	
	/**
	 * Calculate damage caused by falling
	 * @param cha
	 * @param fallHeight
	 * @return damage
	 */
	public static double calcFallDam(Creature cha, int fallHeight)
	{
		if (!Config.ENABLE_FALLING_DAMAGE || (fallHeight < 0))
		{
			return 0;
		}
		return cha.getStat().getValue(Stats.FALL, (fallHeight * cha.getMaxHp()) / 1000.0);
	}
	
	public static boolean calcBlowSuccess(Creature activeChar, Creature target, Skill skill, double blowChance)
	{
		// Apply DEX Mod.
		double dexMod = BaseStats.DEX.calcBonus(activeChar);
		// Apply Position Bonus (TODO: values are unconfirmed, possibly custom, remove or update when confirmed).
		double sideMod = (activeChar.isInFrontOfTarget()) ? 1 : (activeChar.isBehindTarget()) ? 2 : 1.5;
		// Apply all mods.
		double baseRate = blowChance * dexMod * sideMod;
		// Apply blow rates
		double rate = activeChar.getStat().getValue(Stats.BLOW_RATE, baseRate);
		// Debug
		if (activeChar.isDebug())
		{
			final StatsSet set = new StatsSet();
			set.set("dexMod", dexMod);
			set.set("blowChance", blowChance);
			set.set("sideMod", sideMod);
			set.set("baseRate", baseRate);
			set.set("rate", rate);
			Debug.sendSkillDebug(activeChar, target, skill, set);
		}
		return Rnd.get(100) < rate;
	}
	
	public static List<BuffInfo> calcCancelStealEffects(Creature activeChar, Creature target, Skill skill, String slot, int rate, int max)
	{
		final List<BuffInfo> canceled = new ArrayList<>(max);
		switch (slot)
		{
			case "buff":
			{
				// Resist Modifier.
				int cancelMagicLvl = skill.getMagicLevel();
				final double vuln = target.getStat().getValue(Stats.CANCEL_VULN, 0);
				final double prof = activeChar.getStat().getValue(Stats.CANCEL_PROF, 0);
				double resMod = 1 + (((vuln + prof) * -1) / 100);
				double finalRate = rate / resMod;
				
				if (activeChar.isDebug())
				{
					final StatsSet set = new StatsSet();
					set.set("baseMod", rate);
					set.set("magicLevel", cancelMagicLvl);
					set.set("resMod", resMod);
					set.set("rate", finalRate);
					Debug.sendSkillDebug(activeChar, target, skill, set);
				}
				
				// Prevent initialization.
				final List<BuffInfo> buffs = target.getEffectList().hasBuffs() ? new ArrayList<>(target.getEffectList().getBuffs()) : new ArrayList<>(1);
				if (target.getEffectList().hasTriggered())
				{
					buffs.addAll(target.getEffectList().getTriggered());
				}
				if (target.getEffectList().hasDances())
				{
					buffs.addAll(target.getEffectList().getDances());
				}
				for (int i = buffs.size() - 1; i >= 0; i--) // reverse order
				{
					BuffInfo info = buffs.get(i);
					if (!info.getSkill().canBeStolen() || (!calcCancelSuccess(info, cancelMagicLvl, (int) finalRate, skill)))
					{
						continue;
					}
					canceled.add(info);
					if (canceled.size() >= max)
					{
						break;
					}
				}
				break;
			}
			case "debuff":
			{
				final List<BuffInfo> debuffs = new ArrayList<>(target.getEffectList().getDebuffs());
				for (int i = debuffs.size() - 1; i >= 0; i--)
				{
					BuffInfo info = debuffs.get(i);
					if (info.getSkill().isDebuff() && info.getSkill().canBeDispelled() && (Rnd.get(100) <= rate))
					{
						canceled.add(info);
						if (canceled.size() >= max)
						{
							break;
						}
					}
				}
				break;
			}
		}
		return canceled;
	}
	
	public static boolean calcCancelSuccess(BuffInfo info, int cancelMagicLvl, int rate, Skill skill)
	{
		// Lvl Bonus Modifier.
		rate *= info.getSkill().getMagicLevel() > 0 ? 1 + ((cancelMagicLvl - info.getSkill().getMagicLevel()) / 100.) : 1;
		return Rnd.get(100) < CommonUtil.constrain(rate, skill.getMinChance(), skill.getMaxChance());
	}
	
	/**
	 * Calculates the abnormal time for an effect.<br>
	 * The abnormal time is taken from the skill definition, and it's global for all effects present in the skills.
	 * @param caster the caster
	 * @param target the target
	 * @param skill the skill
	 * @return the time that the effect will last
	 */
	public static int calcEffectAbnormalTime(Creature caster, Creature target, Skill skill)
	{
		int time = skill.isPassive() || skill.isToggle() ? -1 : skill.getAbnormalTime();
		
		// An herb buff will affect both master and servitor, but the buff duration will be half of the normal duration.
		// If a servitor is not summoned, the master will receive the full buff duration.
		if ((target != null) && target.isServitor() && skill.isAbnormalInstant())
		{
			time /= 2;
		}
		
		// If the skill is a mastery skill, the effect will last twice the default time.
		if (Formulas.calcSkillMastery(caster, skill))
		{
			time *= 2;
		}
		
		return time;
	}
	
	/**
	 * Calculate Probability in following effects:<br>
	 * TargetCancel,<br>
	 * TargetMeProbability,<br>
	 * SkillTurning,<br>
	 * Betray,<br>
	 * Bluff,<br>
	 * DeleteHate,<br>
	 * RandomizeHate,<br>
	 * DeleteHateOfMe,<br>
	 * TransferHate,<br>
	 * Confuse<br>
	 * @param baseChance chance from effect parameter
	 * @param attacker
	 * @param target
	 * @param skill
	 * @return chance for effect to succeed
	 */
	public static boolean calcProbability(double baseChance, Creature attacker, Creature target, Skill skill)
	{
		return Rnd.get(100) < ((((((skill.getMagicLevel() + baseChance) - target.getLevel()) + 30) - target.getINT()) * calcAttributeBonus(attacker, target, skill)) * calcGeneralTraitBonus(attacker, target, skill.getTraitType(), false));
	}
	
	/**
	 * Calculates karma lost upon death.
	 * @param player
	 * @param exp
	 * @return the amount of karma player has loosed.
	 */
	public static int calculateKarmaLost(PlayerInstance player, long exp)
	{
		double karmaLooseMul = KarmaData.getInstance().getMultiplier(player.getLevel());
		if (exp > 0) // Received exp
		{
			exp /= Config.RATE_KARMA_LOST;
		}
		return (int) ((Math.abs(exp) / karmaLooseMul) / 30);
	}
	
	/**
	 * Calculates karma gain upon playable kill.</br>
	 * Updated to High Five on 10.09.2014 by Zealar tested in retail.
	 * @param pkCount
	 * @param isSummon
	 * @return karma points that will be added to the player.
	 */
	public static int calculateKarmaGain(int pkCount, boolean isSummon)
	{
		int result = 43200;
		
		if (isSummon)
		{
			result = (int) ((((pkCount * 0.375) + 1) * 60) * 4) - 150;
			
			if (result > 10800)
			{
				return 10800;
			}
		}
		
		if (pkCount < 99)
		{
			result = (int) ((((pkCount * 0.5) + 1) * 60) * 12);
		}
		else if (pkCount < 180)
		{
			result = (int) ((((pkCount * 0.125) + 37.75) * 60) * 12);
		}
		
		return result;
	}
	
	public static double calcGeneralTraitBonus(Creature attacker, Creature target, TraitType traitType, boolean ignoreResistance)
	{
		if (traitType == TraitType.NONE)
		{
			return 1.0;
		}
		
		if (target.getStat().isTraitInvul(traitType))
		{
			return 0;
		}
		
		switch (traitType.getType())
		{
			case 2:
			{
				if (!attacker.getStat().hasAttackTrait(traitType) || !target.getStat().hasDefenceTrait(traitType))
				{
					return 1.0;
				}
				break;
			}
			case 3:
			{
				if (ignoreResistance)
				{
					return 1.0;
				}
				break;
			}
			default:
			{
				return 1.0;
			}
		}
		
		final double result = (attacker.getStat().getAttackTrait(traitType) - target.getStat().getDefenceTrait(traitType)) + 1.0;
		return CommonUtil.constrain(result, 0.05, 2.0);
	}
	
	public static double calcWeaponTraitBonus(Creature attacker, Creature target)
	{
		final TraitType type = attacker.getAttackType().getTraitType();
		double result = target.getStat().getDefenceTraits()[type.getId()] - 1.0;
		return 1.0 - result;
	}
	
	public static double calcAttackTraitBonus(Creature attacker, Creature target)
	{
		final double weaponTraitBonus = calcWeaponTraitBonus(attacker, target);
		if (weaponTraitBonus == 0)
		{
			return 0;
		}
		
		double weaknessBonus = 1.0;
		for (TraitType traitType : TraitType.values())
		{
			if (traitType.getType() == 2)
			{
				weaknessBonus *= calcGeneralTraitBonus(attacker, target, traitType, true);
				if (weaknessBonus == 0)
				{
					return 0;
				}
			}
		}
		
		return CommonUtil.constrain((weaponTraitBonus * weaknessBonus), 0.05, 2.0);
	}
	
	public static double getBasicPropertyResistBonus(BasicProperty basicProperty, Creature target)
	{
		if ((basicProperty == BasicProperty.NONE) || !target.isPlayer() || !target.getActingPlayer().hasBasicPropertyResist())
		{
			return 1.0;
		}
		
		final BasicPropertyResist resist = target.getActingPlayer().getBasicPropertyResist(basicProperty);
		switch (resist.getResistLevel())
		{
			case 0:
				return 1.0;
			case 1:
				return 0.6;
			case 2:
				return 0.3;
			default:
				return 0;
		}
	}
	
	/**
	 * Calculated damage caused by ATTACK of attacker on target.
	 * @param attacker player or NPC that makes ATTACK
	 * @param target player or NPC, target of ATTACK
	 * @param power
	 * @param shld
	 * @param crit if the ATTACK have critical success
	 * @param ss if weapon item was charged by soulshot
	 * @return
	 */
	public static double calcAutoAttackDamage(Creature attacker, Creature target, double power, byte shld, boolean crit, boolean ss)
	{
		final double distance = attacker.calculateDistance(target, true, false);
		
		if (distance > target.getStat().getValue(Stats.DAMAGED_MAX_RANGE, Integer.MAX_VALUE))
		{
			return 0;
		}
		
		// DEFENCE CALCULATION (pDef + sDef)
		double defence = target.getPDef(attacker);
		
		switch (shld)
		{
			case SHIELD_DEFENSE_SUCCEED:
			{
				if (!Config.ALT_GAME_SHIELD_BLOCKS)
				{
					defence += target.getShldDef();
				}
				break;
			}
			case SHIELD_DEFENSE_PERFECT_BLOCK:
			{
				return 1.;
			}
		}
		
		final Weapon weapon = attacker.getActiveWeaponItem();
		final boolean isBow = (weapon != null) && weapon.isBowOrCrossBow();
		
		double cAtk = 1;
		double cAtkAdd = 0;
		double critMod = 0;
		double ssBonus = ss ? 2 : 1; // TODO: There are items that increase ss damage, including enchant.
		double attack = attacker.getPAtk(target);
		double random_damage = attacker.getRandomDamageMultiplier();
		attack *= random_damage;
		
		if (crit)
		{
			critMod = isBow ? 0.5 : 1;
			cAtk = 2 * attacker.getStat().getValue(Stats.CRITICAL_DAMAGE, 1) * target.getStat().getValue(Stats.DEFENCE_CRITICAL_DAMAGE, 1);
			cAtkAdd += attacker.getStat().getValue(Stats.CRITICAL_DAMAGE_ADD, 0);
			cAtkAdd += target.getStat().getValue(Stats.DEFENCE_CRITICAL_DAMAGE_ADD, 0);
		}
		
		double proxBonus = (attacker.isInFrontOf(target) ? 0 : (attacker.isBehind(target) ? 0.2 : 0.05)) * attacker.getPAtk(target);
		attack += proxBonus;
		
		// ....................______________Critical Section___________________...._______Non-Critical Section______
		// ATTACK CALCULATION (((pAtk * cAtk * ss + cAtkAdd) * crit) * weaponMod) + (pAtk (1 - crit) * ss * weaponMod)
		// ````````````````````^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^````^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
		attack = ((((attack * cAtk * ssBonus) + cAtkAdd) * critMod) * (isBow ? 154 : 77)) + (attack * (1 - critMod) * ssBonus * (isBow ? 154 : 77));
		
		// DAMAGE CALCULATION (ATTACK / DEFENCE) * trait bonus * attr bonus * pvp bonus * pve bonus
		double damage = attack / defence;
		damage *= calcAttackTraitBonus(attacker, target);
		damage *= calcAttributeBonus(attacker, target, null);
		
		// PvP bonus
		if (attacker.isPlayable() && target.isPlayable())
		{
			damage *= attacker.getStat().getValue(Stats.PVP_PHYSICAL_DMG, 1) * target.getStat().getValue(Stats.PVP_PHYSICAL_DEF, 1);
		}
		
		// PvE Bonus
		if (target.isAttackable())
		{
			if (isBow)
			{
				damage *= attacker.getStat().getValue(Stats.PVE_BOW_DMG, 1);
			}
			else
			{
				damage *= attacker.getStat().getValue(Stats.PVE_PHYSICAL_DMG, 1);
			}
			if (!target.isRaid() && !target.isRaidMinion() && (target.getLevel() >= Config.MIN_NPC_LVL_DMG_PENALTY) && (attacker.getActingPlayer() != null) && ((target.getLevel() - attacker.getActingPlayer().getLevel()) >= 2))
			{
				int lvlDiff = target.getLevel() - attacker.getActingPlayer().getLevel() - 1;
				if (crit)
				{
					if (lvlDiff >= Config.NPC_CRIT_DMG_PENALTY.size())
					{
						damage *= Config.NPC_CRIT_DMG_PENALTY.get(Config.NPC_CRIT_DMG_PENALTY.size() - 1);
					}
					else
					{
						damage *= Config.NPC_CRIT_DMG_PENALTY.get(lvlDiff);
					}
				}
				else
				{
					if (lvlDiff >= Config.NPC_DMG_PENALTY.size())
					{
						damage *= Config.NPC_DMG_PENALTY.get(Config.NPC_DMG_PENALTY.size() - 1);
					}
					else
					{
						damage *= Config.NPC_DMG_PENALTY.get(lvlDiff);
					}
				}
			}
		}
		
		if ((shld > 0) && Config.ALT_GAME_SHIELD_BLOCKS)
		{
			damage -= target.getShldDef();
		}
		
		damage = Math.max(0, damage);
		
		return damage;
	}
	
	public static double getAbnormalResist(BasicProperty basicProperty, Creature target)
	{
		switch (basicProperty)
		{
			case PHYSICAL:
			{
				return target.getStat().getValue(Stats.ABNORMAL_RESIST_PHYSICAL);
			}
			case MAGIC:
			{
				return target.getStat().getValue(Stats.ABNORMAL_RESIST_MAGICAL);
			}
			default:
			{
				return 0;
			}
		}
	}
}
