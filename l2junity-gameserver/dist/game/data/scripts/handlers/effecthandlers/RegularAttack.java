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
import org.l2junity.gameserver.model.effects.L2EffectType;
import org.l2junity.gameserver.model.items.Weapon;
import org.l2junity.gameserver.model.skills.Skill;
import org.l2junity.gameserver.model.stats.Formulas;
import org.l2junity.gameserver.model.stats.Stats;
import org.l2junity.gameserver.network.client.send.string.SystemMessageId;

/**
 * An effect implementing regular attack (like autoattack with sword)
 * @author Nik
 */
public final class RegularAttack extends AbstractEffect
{
	private final double _pAtkMod;
	
	public RegularAttack(Condition attachCond, Condition applyCond, StatsSet set, StatsSet params)
	{
		super(attachCond, applyCond, set, params);
		
		_pAtkMod = params.getDouble("pAtkMod", 1.0);
	}
	
	@Override
	public boolean calcSuccess(Creature effector, Creature effected, Skill skill)
	{
		return true; // Check if this should be here: !Formulas.calcPhysicalSkillEvasion(info.getEffector(), info.getEffected(), info.getSkill());
	}
	
	@Override
	public L2EffectType getEffectType()
	{
		return L2EffectType.REGULAR_ATTACK;
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
		
		final byte shld = Formulas.calcShldUse(effector, effected);
		final boolean crit = Formulas.calcCrit(effector.getStat().getCriticalHit(effected, null), false, effector, effected);
		int damage = (int) Formulas.calcPhysDam(effector, effected, null, 0, shld, crit, effector.isChargedShot(ShotType.SOULSHOTS));
		damage *= _pAtkMod;
		damage = (int) effector.calcStat(Stats.REGULAR_ATTACKS_DMG, damage); // Normal attacks have normal damage x 5

		if (damage > 0)
		{
			effector.sendDamageMessage(effected, damage, false, crit, false);
			effected.reduceCurrentHp(damage, effector, skill);
			effected.notifyDamageReceived(damage, effector, skill, crit, false, false);
			
			Weapon weapon = effector.getActiveWeaponItem();
			boolean isBow = ((weapon != null) && weapon.isBowOrCrossBow());
			if (!isBow && !effected.isInvul()) // Do not reflect if weapon is of type bow or target is invunlerable
			{
				// quick fix for no drop from raid if boss attack high-level char with damage reflection
				if (!effected.isRaid() || (effector.getActingPlayer() == null) || (effector.getActingPlayer().getLevel() <= (effected.getLevel() + 8)))
				{
					// Reduce HP of the target and calculate reflection damage to reduce HP of attacker if necessary
					double reflectPercent = effected.getStat().getValue(Stats.REFLECT_DAMAGE_PERCENT, 0);
					
					if (reflectPercent > 0)
					{
						int reflectedDamage = (int) ((reflectPercent / 100.) * damage);
						
						if (reflectedDamage > effected.getMaxHp())
						{
							reflectedDamage = effected.getMaxHp();
						}
						
						if (reflectedDamage > 0)
						{
							effector.reduceCurrentHp(reflectedDamage, effected, true, false, null);
							effector.notifyDamageReceived(reflectedDamage, effected, null, crit, false, true);
						}
					}
				}
			}
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
}
