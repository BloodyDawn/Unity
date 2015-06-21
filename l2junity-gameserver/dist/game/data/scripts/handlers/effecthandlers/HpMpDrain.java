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
import org.l2junity.gameserver.model.actor.Summon;
import org.l2junity.gameserver.model.conditions.Condition;
import org.l2junity.gameserver.model.effects.AbstractEffect;
import org.l2junity.gameserver.model.effects.L2EffectType;
import org.l2junity.gameserver.model.skills.BuffInfo;
import org.l2junity.gameserver.model.stats.Formulas;

/**
 * HP and MP Drain effect implementation. Support for affecting summons and different power for PvP.
 * @author Nik
 */
public final class HpMpDrain extends AbstractEffect
{
	private final double _power;
	private final double _pvpPower;
	private final boolean _affectSummon; // Allows the drain to affect summons too.
	
	public HpMpDrain(Condition attachCond, Condition applyCond, StatsSet set, StatsSet params)
	{
		super(attachCond, applyCond, set, params);
		
		_power = params.getDouble("power", 0);
		_pvpPower = params.getDouble("pvpPower", _power);
		_affectSummon = params.getBoolean("affectSummon", false);
	}
	
	@Override
	public L2EffectType getEffectType()
	{
		return L2EffectType.HP_DRAIN;
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
		
		boolean sps = info.getSkill().useSpiritShot() && activeChar.isChargedShot(ShotType.SPIRITSHOTS);
		boolean bss = info.getSkill().useSpiritShot() && activeChar.isChargedShot(ShotType.BLESSED_SPIRITSHOTS);
		boolean mcrit = Formulas.calcMCrit(activeChar.getMCriticalHit(target, info.getSkill()), info.getSkill(), target);
		byte shld = Formulas.calcShldUse(activeChar, target, info.getSkill());
		int damage = (int) Formulas.calcMagicDam(activeChar, target, info.getSkill(), shld, sps, bss, mcrit);
		
		int drain = 0;
		int cp = (int) target.getCurrentCp();
		int hp = (int) target.getCurrentHp();
		
		if (cp > 0)
		{
			drain = (damage < cp) ? 0 : (damage - cp);
		}
		else if (damage > hp)
		{
			drain = hp;
		}
		else
		{
			drain = damage;
		}
		
		final double hpMpAdd = (activeChar.isPlayable() && target.isPlayable()) ? (_pvpPower * drain) : (_power * drain);
		double hpFinal = ((activeChar.getCurrentHp() + hpMpAdd) > activeChar.getMaxHp() ? activeChar.getMaxHp() : (activeChar.getCurrentHp() + hpMpAdd));
		double mpFinal = ((activeChar.getCurrentMp() + hpMpAdd) > activeChar.getMaxMp() ? activeChar.getMaxMp() : (activeChar.getCurrentMp() + hpMpAdd));
		activeChar.setCurrentHp(hpFinal);
		activeChar.setCurrentMp(mpFinal);
		
		if (_affectSummon)
		{
			for (Summon summon : activeChar.getServitors().values())
			{
				hpFinal = ((summon.getCurrentHp() + hpMpAdd) > summon.getMaxHp() ? summon.getMaxHp() : (summon.getCurrentHp() + hpMpAdd));
				mpFinal = ((summon.getCurrentMp() + hpMpAdd) > summon.getMaxMp() ? summon.getMaxMp() : (summon.getCurrentMp() + hpMpAdd));
				summon.setCurrentHp(hpFinal);
				summon.setCurrentMp(mpFinal);
			}
		}
		
		if (damage > 0)
		{
			// Manage attack or cast break of the target (calculating rate, sending message...)
			if (!target.isRaid() && Formulas.calcAtkBreak(target, damage))
			{
				target.breakAttack();
				target.breakCast();
			}
			activeChar.sendDamageMessage(target, damage, mcrit, false, false);
			target.reduceCurrentHp(damage, activeChar, info.getSkill());
			target.notifyDamageReceived(damage, activeChar, info.getSkill(), mcrit, false, false);
		}
	}
}