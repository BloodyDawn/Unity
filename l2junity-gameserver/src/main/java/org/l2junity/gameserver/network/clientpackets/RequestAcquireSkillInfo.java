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
package org.l2junity.gameserver.network.clientpackets;

import org.l2junity.gameserver.data.xml.impl.SkillTreesData;
import org.l2junity.gameserver.datatables.SkillData;
import org.l2junity.gameserver.enums.CategoryType;
import org.l2junity.gameserver.enums.Race;
import org.l2junity.gameserver.model.ClanPrivilege;
import org.l2junity.gameserver.model.SkillLearn;
import org.l2junity.gameserver.model.actor.Npc;
import org.l2junity.gameserver.model.actor.instance.L2NpcInstance;
import org.l2junity.gameserver.model.actor.instance.PlayerInstance;
import org.l2junity.gameserver.model.base.AcquireSkillType;
import org.l2junity.gameserver.model.skills.Skill;
import org.l2junity.gameserver.network.L2GameClient;
import org.l2junity.gameserver.network.serverpackets.AcquireSkillInfo;
import org.l2junity.gameserver.network.serverpackets.ExAcquireSkillInfo;
import org.l2junity.network.PacketReader;

/**
 * Request Acquire Skill Info client packet implementation.
 * @author Zoey76
 */
public final class RequestAcquireSkillInfo implements IGameClientPacket
{
	private int _id;
	private int _level;
	private AcquireSkillType _skillType;
	
	@Override
	public boolean read(PacketReader packet)
	{
		_id = packet.readD();
		_level = packet.readD();
		_skillType = AcquireSkillType.getAcquireSkillType(packet.readD());
		return true;
	}
	
	@Override
	public void run(L2GameClient client)
	{
		if ((_id <= 0) || (_level <= 0))
		{
			_log.warning(RequestAcquireSkillInfo.class.getSimpleName() + ": Invalid Id: " + _id + " or level: " + _level + "!");
			return;
		}
		
		final PlayerInstance activeChar = client.getActiveChar();
		if (activeChar == null)
		{
			return;
		}
		
		final Npc trainer = activeChar.getLastFolkNPC();
		if (!(trainer instanceof L2NpcInstance) && (_skillType != AcquireSkillType.CLASS))
		{
			return;
		}
		
		if ((_skillType != AcquireSkillType.CLASS) && !trainer.canInteract(activeChar) && !activeChar.isGM())
		{
			return;
		}
		
		final Skill skill = SkillData.getInstance().getSkill(_id, _level);
		if (skill == null)
		{
			_log.warning(RequestAcquireSkillInfo.class.getSimpleName() + ": Skill Id: " + _id + " level: " + _level + " is undefined. " + RequestAcquireSkillInfo.class.getName() + " failed.");
			return;
		}
		
		// Hack check. Doesn't apply to all Skill Types
		final int prevSkillLevel = activeChar.getSkillLevel(_id);
		if ((prevSkillLevel > 0) && !((_skillType == AcquireSkillType.TRANSFER) || (_skillType == AcquireSkillType.SUBPLEDGE)))
		{
			if (prevSkillLevel == _level)
			{
				_log.warning(RequestAcquireSkillInfo.class.getSimpleName() + ": Player " + activeChar.getName() + " is trequesting info for a skill that already knows, Id: " + _id + " level: " + _level + "!");
			}
			else if (prevSkillLevel != (_level - 1))
			{
				_log.warning(RequestAcquireSkillInfo.class.getSimpleName() + ": Player " + activeChar.getName() + " is requesting info for skill Id: " + _id + " level " + _level + " without knowing it's previous level!");
			}
		}
		
		final SkillLearn s = SkillTreesData.getInstance().getSkillLearn(_skillType, _id, _level, activeChar);
		if (s == null)
		{
			return;
		}
		
		switch (_skillType)
		{
			case TRANSFORM:
			case FISHING:
			case SUBCLASS:
			case COLLECT:
			case TRANSFER:
			case DUALCLASS:
			{
				client.sendPacket(new AcquireSkillInfo(_skillType, s));
				break;
			}
			case CLASS:
			{
				final int customSp = s.getCalculatedLevelUpSp(activeChar.getClassId(), activeChar.getLearningClass());
				client.sendPacket(new ExAcquireSkillInfo(activeChar, s, customSp));
				break;
			}
			case PLEDGE:
			{
				if (!activeChar.isClanLeader())
				{
					return;
				}
				client.sendPacket(new AcquireSkillInfo(_skillType, s));
				break;
			}
			case SUBPLEDGE:
			{
				if (!activeChar.isClanLeader() || !activeChar.hasClanPrivilege(ClanPrivilege.CL_TROOPS_FAME))
				{
					return;
				}
				client.sendPacket(new AcquireSkillInfo(_skillType, s));
				break;
			}
			case ALCHEMY:
			{
				if (activeChar.getRace() != Race.ERTHEIA)
				{
					return;
				}
				client.sendPacket(new AcquireSkillInfo(_skillType, s));
				break;
			}
			case REVELATION:
			{
				if ((activeChar.getLevel() < 85) || !activeChar.isInCategory(CategoryType.AWAKEN_GROUP))
				{
					return;
				}
				client.sendPacket(new AcquireSkillInfo(_skillType, s));
				break;
			}
			case REVELATION_DUALCLASS:
			{
				if (!activeChar.isSubClassActive() || !activeChar.isDualClassActive())
				{
					return;
				}
				client.sendPacket(new AcquireSkillInfo(_skillType, s));
				break;
			}
		}
	}
}
