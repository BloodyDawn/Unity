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

import java.util.Collection;

import org.l2junity.gameserver.ai.CtrlEvent;
import org.l2junity.gameserver.model.StatsSet;
import org.l2junity.gameserver.model.actor.instance.L2MonsterInstance;
import org.l2junity.gameserver.model.actor.instance.PlayerInstance;
import org.l2junity.gameserver.model.conditions.Condition;
import org.l2junity.gameserver.model.effects.AbstractEffect;
import org.l2junity.gameserver.model.holders.ItemHolder;
import org.l2junity.gameserver.model.skills.BuffInfo;
import org.l2junity.gameserver.model.stats.Formulas;
import org.l2junity.gameserver.network.client.send.string.SystemMessageId;

/**
 * @author Sdw
 */
public final class Plunder extends AbstractEffect
{
	public Plunder(Condition attachCond, Condition applyCond, StatsSet set, StatsSet params)
	{
		super(attachCond, applyCond, set, params);
	}
	
	@Override
	public boolean calcSuccess(BuffInfo info)
	{
		return Formulas.calcMagicSuccess(info.getEffector(), info.getEffected(), info.getSkill());
	}
	
	@Override
	public boolean isInstant()
	{
		return true;
	}
	
	@Override
	public void onStart(BuffInfo info)
	{
		if (!info.getEffector().isPlayer())
		{
			return;
		}
		else if (!info.getEffected().isMonster() || info.getEffected().isDead())
		{
			info.getEffector().sendPacket(SystemMessageId.INVALID_TARGET);
			return;
		}
		
		final L2MonsterInstance monster = (L2MonsterInstance) info.getEffected();
		final PlayerInstance player = info.getEffector().getActingPlayer();
		
		if (monster.isSpoiled())
		{
			info.getEffector().sendPacket(SystemMessageId.PLUNDER_SKILL_HAS_BEEN_ALREADY_USED_ON_THIS_TARGET);
			return;
		}
		
		monster.setSpoilerObjectId(info.getEffector().getObjectId());
		if (monster.isSweepActive())
		{
			final Collection<ItemHolder> items = monster.takeSweep();
			
			if (items != null)
			{
				for (ItemHolder item : items)
				{
					if (info.getEffector().isInParty())
					{
						info.getEffector().getParty().distributeItem(player, item, true, monster);
					}
					else
					{
						player.addItem("Sweeper", item, info.getEffected(), true);
					}
				}
			}
		}
		monster.getAI().notifyEvent(CtrlEvent.EVT_ATTACKED, info.getEffector());
	}
}
