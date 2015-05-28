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

import org.l2junity.gameserver.GeoData;
import org.l2junity.gameserver.ai.CtrlEvent;
import org.l2junity.gameserver.ai.CtrlIntention;
import org.l2junity.gameserver.enums.Race;
import org.l2junity.gameserver.model.Location;
import org.l2junity.gameserver.model.StatsSet;
import org.l2junity.gameserver.model.actor.instance.L2DefenderInstance;
import org.l2junity.gameserver.model.actor.instance.L2FortCommanderInstance;
import org.l2junity.gameserver.model.actor.instance.L2SiegeFlagInstance;
import org.l2junity.gameserver.model.conditions.Condition;
import org.l2junity.gameserver.model.effects.AbstractEffect;
import org.l2junity.gameserver.model.effects.EffectFlag;
import org.l2junity.gameserver.model.effects.L2EffectType;
import org.l2junity.gameserver.model.skills.BuffInfo;
import org.l2junity.gameserver.util.Util;

/**
 * Fear effect implementation.
 * @author littlecrow
 */
public final class Fear extends AbstractEffect
{
	public static final int FEAR_RANGE = 500;
	
	public Fear(Condition attachCond, Condition applyCond, StatsSet set, StatsSet params)
	{
		super(attachCond, applyCond, set, params);
	}
	
	@Override
	public boolean canStart(BuffInfo info)
	{
		return info.getEffected().isPlayer() || info.getEffected().isSummon() || (info.getEffected().isAttackable() && //
		!((info.getEffected() instanceof L2DefenderInstance) || (info.getEffected() instanceof L2FortCommanderInstance) || //
			(info.getEffected() instanceof L2SiegeFlagInstance) || (info.getEffected().getTemplate().getRace() == Race.SIEGE_WEAPON)));
	}
	
	@Override
	public int getEffectFlags()
	{
		return EffectFlag.FEAR.getMask();
	}
	
	@Override
	public L2EffectType getEffectType()
	{
		return L2EffectType.FEAR;
	}
	
	@Override
	public int getTicks()
	{
		return 5;
	}
	
	@Override
	public boolean onActionTime(BuffInfo info)
	{
		fearAction(info, false);
		return false;
	}
	
	@Override
	public void onStart(BuffInfo info)
	{
		info.getEffected().getAI().notifyEvent(CtrlEvent.EVT_AFRAID);
		fearAction(info, true);
	}
	
	@Override
	public void onExit(BuffInfo info)
	{
		if (!info.getEffected().isPlayer())
		{
			info.getEffected().getAI().notifyEvent(CtrlEvent.EVT_THINK);
		}
		super.onExit(info);
	}
	
	private void fearAction(BuffInfo info, boolean start)
	{
		double radians = Math.toRadians(start ? Util.calculateAngleFrom(info.getEffector(), info.getEffected()) : Util.convertHeadingToDegree(info.getEffected().getHeading()));
		
		int posX = (int) (info.getEffected().getX() + (FEAR_RANGE * Math.cos(radians)));
		int posY = (int) (info.getEffected().getY() + (FEAR_RANGE * Math.sin(radians)));
		int posZ = info.getEffected().getZ();
		
		final Location destination = GeoData.getInstance().moveCheck(info.getEffected().getX(), info.getEffected().getY(), info.getEffected().getZ(), posX, posY, posZ, info.getEffected().getInstanceId());
		info.getEffected().getAI().setIntention(CtrlIntention.AI_INTENTION_MOVE_TO, destination);
	}
}
