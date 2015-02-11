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
import org.l2junity.gameserver.model.actor.instance.L2PcInstance;
import org.l2junity.gameserver.model.actor.stat.PcStat;
import org.l2junity.gameserver.model.variables.AccountVariables;
import org.l2junity.gameserver.network.L2GameClient;

/**
 * @author Sdw
 */
public class ExVitalityEffectInfo extends L2GameServerPacket
{
	private final int _points;
	
	public ExVitalityEffectInfo(L2PcInstance cha)
	{
		_points = cha.getVitalityPoints();
	}
	
	public ExVitalityEffectInfo(L2GameClient client)
	{
		final AccountVariables vars = new AccountVariables(client.getAccountName());
		_points = vars.getInt(PcStat.VITALITY_VARIABLE, Config.STARTING_VITALITY_POINTS);
	}
	
	@Override
	protected final void writeImpl()
	{
		writeC(0xFE);
		writeH(0x118);
		
		writeD(_points);
		writeD((int) (Config.RATE_VITALITY_EXP_MULTIPLIER * 100)); // Vitality Bonus
		writeH(0x05); // How much vitality items remaining for use
		writeH(0x05); // Max number of items for use
	}
}