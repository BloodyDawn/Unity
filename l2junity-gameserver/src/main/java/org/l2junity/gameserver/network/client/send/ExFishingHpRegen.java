/*
 * Copyright (C) 2004-2015 L2J Unity
 * 
 * This file is part of L2J Unity.
 * 
 * L2J Unity is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * L2J Unity is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package org.l2junity.gameserver.network.client.send;

import org.l2junity.gameserver.model.actor.Creature;
import org.l2junity.gameserver.network.client.OutgoingPackets;
import org.l2junity.network.PacketWriter;

/**
 * @author -Wooden-
 */
public class ExFishingHpRegen implements IClientOutgoingPacket
{
	private final Creature _activeChar;
	private final int _time, _fishHP, _hpMode, _anim, _goodUse, _penalty, _hpBarColor;
	
	public ExFishingHpRegen(Creature character, int time, int fishHP, int HPmode, int GoodUse, int anim, int penalty, int hpBarColor)
	{
		_activeChar = character;
		_time = time;
		_fishHP = fishHP;
		_hpMode = HPmode;
		_goodUse = GoodUse;
		_anim = anim;
		_penalty = penalty;
		_hpBarColor = hpBarColor;
	}
	
	@Override
	public boolean write(PacketWriter packet)
	{
		OutgoingPackets.EX_FISHING_HP_REGEN.writeId(packet);
		
		packet.writeD(_activeChar.getObjectId());
		packet.writeD(_time);
		packet.writeD(_fishHP);
		packet.writeC(_hpMode); // 0 = HP stop, 1 = HP raise
		packet.writeC(_goodUse); // 0 = none, 1 = success, 2 = failed
		packet.writeC(_anim); // Anim: 0 = none, 1 = reeling, 2 = pumping
		packet.writeD(_penalty); // Penalty
		packet.writeC(_hpBarColor); // 0 = normal hp bar, 1 = purple hp bar
		return true;
	}
}