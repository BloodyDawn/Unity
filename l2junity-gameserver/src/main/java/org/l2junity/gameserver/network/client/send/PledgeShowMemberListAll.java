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
package org.l2junity.gameserver.network.client.send;

import java.util.Collection;

import org.l2junity.gameserver.model.ClanMember;
import org.l2junity.gameserver.model.L2Clan;
import org.l2junity.gameserver.model.L2Clan.SubPledge;
import org.l2junity.gameserver.model.World;
import org.l2junity.gameserver.model.actor.instance.PlayerInstance;
import org.l2junity.gameserver.network.client.OutgoingPackets;
import org.l2junity.network.PacketWriter;

public class PledgeShowMemberListAll implements IClientOutgoingPacket
{
	private final L2Clan _clan;
	private final Collection<ClanMember> _members;
	private int _pledgeType;
	
	public PledgeShowMemberListAll(L2Clan clan)
	{
		_clan = clan;
		_members = _clan.getMembers();
	}
	
	@Override
	public boolean write(PacketWriter packet)
	{
		// write main Clan
		writePledge(packet, null, _clan.getLeaderName());
		
		for (SubPledge subPledge : _clan.getAllSubPledges())
		{
			PlayerInstance pLeader = World.getInstance().getPlayer(subPledge.getLeaderId());
			writePledge(packet, subPledge, (pLeader == null ? "" : pLeader.getName()));
		}
		return true;
	}
	
	private void writePledge(PacketWriter packet, SubPledge pledge, String name)
	{
		final int pledgeId = (pledge == null ? 0x00 : pledge.getId());
		OutgoingPackets.PLEDGE_SHOW_MEMBER_LIST_ALL.writeId(packet);
		
		packet.writeD(pledge == null ? 0 : 1);
		packet.writeD(_clan.getId());
		packet.writeD(0x00); // pledge db id
		packet.writeD(pledgeId);
		packet.writeS(_clan.getName());
		packet.writeS(_clan.getLeaderName());
		
		packet.writeD(_clan.getCrestId()); // crest id .. is used again
		packet.writeD(_clan.getLevel());
		packet.writeD(_clan.getCastleId());
		packet.writeD(_clan.getHideoutId());
		packet.writeD(_clan.getFortId());
		packet.writeD(0x00);
		packet.writeD(_clan.getRank());
		packet.writeD(_clan.getReputationScore());
		packet.writeD(0x00); // 0
		packet.writeD(0x00); // 0
		packet.writeD(_clan.getAllyId());
		packet.writeS(_clan.getAllyName());
		packet.writeD(_clan.getAllyCrestId());
		packet.writeD(_clan.isAtWar() ? 1 : 0);// new c3
		packet.writeD(0x00); // Territory castle ID
		packet.writeD(_clan.getSubPledgeMembersCount(_pledgeType));
		
		for (ClanMember m : _members)
		{
			if (m.getPledgeType() != _pledgeType)
			{
				continue;
			}
			packet.writeS(m.getName());
			packet.writeD(m.getLevel());
			packet.writeD(m.getClassId());
			PlayerInstance player = m.getPlayerInstance();
			if (player != null)
			{
				packet.writeD(player.getAppearance().getSex() ? 1 : 0); // no visible effect
				packet.writeD(player.getRace().ordinal());// packet.writeD(1);
			}
			else
			{
				packet.writeD(0x01); // no visible effect
				packet.writeD(0x01); // packet.writeD(1);
			}
			packet.writeD(m.isOnline() ? m.getObjectId() : 0); // objectId = online 0 = offline
			packet.writeD(m.getSponsor() != 0 ? 1 : 0);
		}
	}
}
