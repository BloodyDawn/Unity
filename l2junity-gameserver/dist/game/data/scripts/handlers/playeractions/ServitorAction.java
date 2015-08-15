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
package handlers.playeractions;

import org.l2junity.gameserver.ai.CtrlIntention;
import org.l2junity.gameserver.ai.SummonAI;
import org.l2junity.gameserver.handler.IPlayerActionHandler;
import org.l2junity.gameserver.handler.PlayerActionHandler;
import org.l2junity.gameserver.model.ActionDataHolder;
import org.l2junity.gameserver.model.actor.instance.PlayerInstance;
import org.l2junity.gameserver.network.client.send.string.SystemMessageId;

/**
 * Servitor actions player action handler.
 * @author St3eT, Nik
 */
public final class ServitorAction implements IPlayerActionHandler
{
	@Override
	public void useAction(PlayerInstance activeChar, ActionDataHolder data, boolean ctrlPressed, boolean shiftPressed)
	{
		if (!activeChar.hasServitors())
		{
			activeChar.sendPacket(SystemMessageId.YOU_DO_NOT_HAVE_A_SERVITOR);
			return;
		}
		
		switch (data.getOptionId())
		{
			case 1: // Attack
			{
				activeChar.getServitors().values().stream().filter(s -> s.canAttack(activeChar.getTarget(), ctrlPressed)).forEach(s ->
				{
					if (s.isBetrayed())
					{
						activeChar.sendPacket(SystemMessageId.YOUR_PET_SERVITOR_IS_UNRESPONSIVE_AND_WILL_NOT_OBEY_ANY_ORDERS);
						return;
					}
					
					s.doAttack(activeChar.getTarget());
				});
				break;
			}
			case 2: // Stop
			{
				activeChar.getServitors().values().forEach(s ->
				{
					if (s.isBetrayed())
					{
						activeChar.sendPacket(SystemMessageId.YOUR_PET_SERVITOR_IS_UNRESPONSIVE_AND_WILL_NOT_OBEY_ANY_ORDERS);
						return;
					}
					
					s.cancelAction();
				});
				break;
			}
			case 3: // Change movement mode
			{
				activeChar.getServitors().values().forEach(s ->
				{
					if (s.isBetrayed())
					{
						activeChar.sendPacket(SystemMessageId.YOUR_PET_SERVITOR_IS_UNRESPONSIVE_AND_WILL_NOT_OBEY_ANY_ORDERS);
						return;
					}
					
					((SummonAI) s.getAI()).notifyFollowStatusChange();
				});
				break;
			}
			case 4: // Move to target
			{
				if (activeChar.getTarget() != null)
				{
					activeChar.getServitors().values().stream().filter(s -> (s != activeChar.getTarget()) && !s.isMovementDisabled()).forEach(s ->
					{
						if (s.isBetrayed())
						{
							activeChar.sendPacket(SystemMessageId.YOUR_PET_SERVITOR_IS_UNRESPONSIVE_AND_WILL_NOT_OBEY_ANY_ORDERS);
							return;
						}
						
						s.setFollowStatus(false);
						s.getAI().setIntention(CtrlIntention.AI_INTENTION_MOVE_TO, activeChar.getTarget().getLocation());
					});
				}
				break;
			}
			case 5: // Passive mode
			{
				activeChar.getServitors().values().forEach(s ->
				{
					if (s.isBetrayed())
					{
						activeChar.sendPacket(SystemMessageId.YOUR_PET_SERVITOR_IS_UNRESPONSIVE_AND_WILL_NOT_OBEY_ANY_ORDERS);
						return;
					}
					
					((SummonAI) s.getAI()).setDefending(false);
				});
				break;
			}
			case 6: // Defending mode
			{
				activeChar.getServitors().values().forEach(s ->
				{
					if (s.isBetrayed())
					{
						activeChar.sendPacket(SystemMessageId.YOUR_PET_SERVITOR_IS_UNRESPONSIVE_AND_WILL_NOT_OBEY_ANY_ORDERS);
						return;
					}
					
					((SummonAI) s.getAI()).setDefending(true);
				});
			}
		}
	}
	
	public static void main(String[] args)
	{
		PlayerActionHandler.getInstance().registerHandler(new ServitorAction());
	}
}
