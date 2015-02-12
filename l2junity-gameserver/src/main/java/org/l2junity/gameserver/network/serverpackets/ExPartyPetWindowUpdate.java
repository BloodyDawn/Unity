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

import org.l2junity.gameserver.model.actor.Summon;

/**
 * @author KenM
 */
public class ExPartyPetWindowUpdate extends L2GameServerPacket
{
	private final Summon _summon;
	
	public ExPartyPetWindowUpdate(Summon summon)
	{
		_summon = summon;
	}
	
	@Override
	protected void writeImpl()
	{
		writeC(0xFE);
		writeH(0x19);
		writeD(_summon.getObjectId());
		writeD(_summon.getTemplate().getDisplayId() + 1000000);
		writeC(_summon.getSummonType());
		writeD(_summon.getOwner().getObjectId());
		writeD((int) _summon.getCurrentHp());
		writeD(_summon.getMaxHp());
		writeD((int) _summon.getCurrentMp());
		writeD(_summon.getMaxMp());
	}
}
