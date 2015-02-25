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
package org.l2junity.gameserver.network.serverpackets;

import org.l2junity.Config;
import org.l2junity.gameserver.model.actor.instance.PlayerInstance;
import org.l2junity.gameserver.model.stats.Stats;

/**
 * @author Sdw
 */
public class ExVitalityEffectInfo extends L2GameServerPacket
{
	private final int _vitalityBonus;
	private final int _vitalityItemsRemaining;
	private final int _points;
	
	public ExVitalityEffectInfo(PlayerInstance cha)
	{
		_points = cha.getVitalityPoints();
		_vitalityBonus = (int) Math.round(cha.calcStat(Stats.VITALITY_EXP_BONUS, Config.RATE_VITALITY_EXP_MULTIPLIER) * 100);
		_vitalityItemsRemaining = cha.getVariables().getInt("VITALITY_ITEMS_USED", 0) - Config.VITALITY_MAX_ITEMS_ALLOWED;
	}
	
	@Override
	protected final void writeImpl()
	{
		writeC(0xFE);
		writeH(0x118);
		
		writeD(_points);
		writeD(_vitalityBonus); // Vitality Bonus
		writeH(_vitalityItemsRemaining); // How much vitality items remaining for use
		writeH(Config.VITALITY_MAX_ITEMS_ALLOWED); // Max number of items for use
	}
}