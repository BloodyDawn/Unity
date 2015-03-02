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

import org.l2junity.gameserver.data.xml.impl.FishingRodsData;
import org.l2junity.gameserver.enums.ShotType;
import org.l2junity.gameserver.model.StatsSet;
import org.l2junity.gameserver.model.actor.Creature;
import org.l2junity.gameserver.model.actor.instance.PlayerInstance;
import org.l2junity.gameserver.model.conditions.Condition;
import org.l2junity.gameserver.model.effects.AbstractEffect;
import org.l2junity.gameserver.model.effects.L2EffectType;
import org.l2junity.gameserver.model.fishing.L2Fishing;
import org.l2junity.gameserver.model.fishing.L2FishingRod;
import org.l2junity.gameserver.model.items.Weapon;
import org.l2junity.gameserver.model.items.instance.ItemInstance;
import org.l2junity.gameserver.model.skills.BuffInfo;
import org.l2junity.gameserver.model.stats.Stats;
import org.l2junity.gameserver.network.client.SystemMessageId;
import org.l2junity.gameserver.network.client.send.ActionFailed;

/**
 * Reeling effect implementation.
 * @author UnAfraid
 */
public final class Reeling extends AbstractEffect
{
	private final double _power;
	
	public Reeling(Condition attachCond, Condition applyCond, StatsSet set, StatsSet params)
	{
		super(attachCond, applyCond, set, params);
		
		if (params.getString("power", null) == null)
		{
			throw new IllegalArgumentException(getClass().getSimpleName() + ": effect without power!");
		}
		_power = params.getDouble("power");
	}
	
	@Override
	public boolean isInstant()
	{
		return true;
	}
	
	@Override
	public L2EffectType getEffectType()
	{
		return L2EffectType.FISHING;
	}
	
	@Override
	public void onStart(BuffInfo info)
	{
		final Creature activeChar = info.getEffector();
		if (!activeChar.isPlayer())
		{
			return;
		}
		
		final PlayerInstance player = activeChar.getActingPlayer();
		final L2Fishing fish = player.getFishCombat();
		if (fish == null)
		{
			// Reeling skill is available only while fishing
			player.sendPacket(SystemMessageId.YOU_MAY_ONLY_USE_THE_REELING_SKILL_WHILE_YOU_ARE_FISHING);
			player.sendPacket(ActionFailed.STATIC_PACKET);
			return;
		}
		final Weapon weaponItem = player.getActiveWeaponItem();
		final ItemInstance weaponInst = activeChar.getActiveWeaponInstance();
		if ((weaponInst == null) || (weaponItem == null))
		{
			return;
		}
		int SS = 1;
		int pen = 0;
		if (activeChar.isChargedShot(ShotType.FISH_SOULSHOTS))
		{
			SS = 2;
		}
		final L2FishingRod fishingRod = FishingRodsData.getInstance().getFishingRod(weaponItem.getId());
		final double gradeBonus = fishingRod.getFishingRodLevel() * 0.1; // TODO: Check this formula (is guessed)
		int dmg = (int) ((fishingRod.getFishingRodDamage() + player.calcStat(Stats.FISHING_EXPERTISE, 1, null, null) + _power) * gradeBonus * SS);
		// Penalty 5% less damage dealt
		if (player.getSkillLevel(1315) <= (info.getSkill().getLevel() - 2)) // 1315 - Fish Expertise
		{
			player.sendPacket(SystemMessageId.DUE_TO_YOUR_REELING_AND_OR_PUMPING_SKILL_BEING_THREE_OR_MORE_LEVELS_HIGHER_THAN_YOUR_FISHING_SKILL_A_S1_DAMAGE_PENALTY_WILL_BE_APPLIED);
			pen = (int) (dmg * 0.05);
			dmg = dmg - pen;
		}
		if (SS > 1)
		{
			weaponInst.setChargedShot(ShotType.FISH_SOULSHOTS, false);
		}
		
		fish.useReeling(dmg, pen);
	}
}
