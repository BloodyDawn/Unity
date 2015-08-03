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
import org.l2junity.gameserver.model.actor.Creature;
import org.l2junity.gameserver.model.conditions.Condition;
import org.l2junity.gameserver.model.effects.AbstractEffect;
import org.l2junity.gameserver.model.effects.EffectFlag;
import org.l2junity.gameserver.model.effects.L2EffectType;
import org.l2junity.gameserver.model.items.instance.ItemInstance;
import org.l2junity.gameserver.model.items.type.CrystalType;
import org.l2junity.gameserver.model.skills.BuffInfo;
import org.l2junity.gameserver.model.skills.Skill;
import org.l2junity.gameserver.model.stats.Formulas;
import org.l2junity.gameserver.model.stats.Stats;
import org.l2junity.gameserver.network.client.send.ExMagicAttackInfo;
import org.l2junity.gameserver.network.client.send.StatusUpdate;
import org.l2junity.gameserver.network.client.send.SystemMessage;
import org.l2junity.gameserver.network.client.send.string.SystemMessageId;

/**
 * Heal effect implementation.
 * @author UnAfraid
 */
public final class Heal extends AbstractEffect
{
	private final double _power;
	
	public Heal(Condition attachCond, Condition applyCond, StatsSet set, StatsSet params)
	{
		super(attachCond, applyCond, set, params);
		
		_power = params.getDouble("power", 0);
	}
	
	@Override
	public L2EffectType getEffectType()
	{
		return L2EffectType.HEAL;
	}
	
	@Override
	public boolean isInstant()
	{
		return true;
	}

	@Override
	public void instant(Creature effector, Creature effected, Skill skill)
	{
		if (effected.isDead() || effected.isDoor() || effected.isInvul())
		{
			return;
		}
		
		if ((effected != effector) && effected.isAffected(EffectFlag.FACEOFF))
		{
			return;
		}
		
		double amount = _power;
		double staticShotBonus = 0;
		int mAtkMul = 1;
		boolean sps = skill.isMagic() && effector.isChargedShot(ShotType.SPIRITSHOTS);
		boolean bss = skill.isMagic() && effector.isChargedShot(ShotType.BLESSED_SPIRITSHOTS);
		
		if (((sps || bss) && (effector.isPlayer() && effector.getActingPlayer().isMageClass())) || effector.isSummon())
		{
			staticShotBonus = skill.getMpConsume(); // static bonus for spiritshots
			mAtkMul = bss ? 4 : 2;
			staticShotBonus *= bss ? 2.4 : 1.0;
		}
		else if ((sps || bss) && effector.isNpc())
		{
			staticShotBonus = 2.4 * skill.getMpConsume(); // always blessed spiritshots
			mAtkMul = 4;
		}
		else
		{
			// no static bonus
			// grade dynamic bonus
			final ItemInstance weaponInst = effector.getActiveWeaponInstance();
			if (weaponInst != null)
			{
				mAtkMul = weaponInst.getItem().getCrystalType() == CrystalType.S84 ? 4 : weaponInst.getItem().getCrystalType() == CrystalType.S80 ? 2 : 1;
			}
			// shot dynamic bonus
			mAtkMul = bss ? mAtkMul * 4 : mAtkMul + 1;
		}
		
		if (!skill.isStatic())
		{
			amount += staticShotBonus + Math.sqrt(mAtkMul * effector.getMAtk(effector, null));
			amount = effected.calcStat(Stats.HEAL_EFFECT, amount, null, null);
			// Heal critic, since CT2.3 Gracia Final
			if (skill.isMagic() && Formulas.calcMCrit(effector.getMCriticalHit(effected, skill), skill, effected))
			{
				amount *= 3;
				effector.sendPacket(SystemMessageId.M_CRITICAL);
				effector.sendPacket(new ExMagicAttackInfo(effector.getObjectId(), effected.getObjectId(), ExMagicAttackInfo.CRITICAL_HEAL));
				if (effected.isPlayer() && (effected != effector))
				{
					effected.sendPacket(new ExMagicAttackInfo(effector.getObjectId(), effected.getObjectId(), ExMagicAttackInfo.CRITICAL_HEAL));
				}
			}
		}
		
		// Adding healer's heal power
		amount = effector.calcStat(Stats.HEAL_POWER, amount, null, null);
		
		// Prevents overheal and negative amount
		amount = Math.max(Math.min(amount, effected.getMaxRecoverableHp() - effected.getCurrentHp()), 0);
		if (amount != 0)
		{
			final double newHp = amount + effected.getCurrentHp();
			effected.setCurrentHp(newHp, false);
			final StatusUpdate su = new StatusUpdate(effected);
			su.addAttribute(StatusUpdate.CUR_HP, (int) newHp);
			su.addCaster(effector);
			effected.broadcastPacket(su);
		}
		
		if (effected.isPlayer())
		{
			if (skill.getId() == 4051)
			{
				effected.sendPacket(SystemMessageId.REJUVENATING_HP);
			}
			else
			{
				if (effector.isPlayer() && (effector != effected))
				{
					SystemMessage sm = SystemMessage.getSystemMessage(SystemMessageId.S2_HP_HAS_BEEN_RESTORED_BY_C1);
					sm.addString(effector.getName());
					sm.addInt((int) amount);
					effected.sendPacket(sm);
				}
				else
				{
					SystemMessage sm = SystemMessage.getSystemMessage(SystemMessageId.S1_HP_HAS_BEEN_RESTORED);
					sm.addInt((int) amount);
					effected.sendPacket(sm);
				}
			}
		}
	}
}
