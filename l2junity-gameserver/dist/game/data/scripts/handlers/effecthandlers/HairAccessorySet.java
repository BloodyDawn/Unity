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

import org.l2junity.gameserver.enums.UserInfoType;
import org.l2junity.gameserver.model.StatsSet;
import org.l2junity.gameserver.model.actor.instance.PlayerInstance;
import org.l2junity.gameserver.model.conditions.Condition;
import org.l2junity.gameserver.model.effects.AbstractEffect;
import org.l2junity.gameserver.model.skills.BuffInfo;
import org.l2junity.gameserver.network.client.send.string.SystemMessageId;

/**
 * @author UnAfraid
 */
public class HairAccessorySet extends AbstractEffect
{
	public HairAccessorySet(Condition attachCond, Condition applyCond, StatsSet set, StatsSet params)
	{
		super(attachCond, applyCond, set, params);
	}
	
	@Override
	public boolean isInstant()
	{
		return true;
	}
	
	@Override
	public void onStart(BuffInfo info)
	{
		if ((info.getEffector() == null) || (info.getEffected() == null) || !info.getEffector().isPlayer() || !info.getEffected().isPlayer() || info.getEffected().isAlikeDead())
		{
			return;
		}
		
		final PlayerInstance player = info.getEffected().getActingPlayer();
		
		player.setHairAccessoryEnabled(!player.isHairAccessoryEnabled());
		player.broadcastUserInfo(UserInfoType.APPAREANCE);
		player.sendPacket(player.isHairAccessoryEnabled() ? SystemMessageId.HAIR_ACCESSORIES_WILL_BE_DISPLAYED_FROM_NOW_ON : SystemMessageId.HAIR_ACCESSORIES_WILL_NO_LONGER_BE_DISPLAYED);
	}
}