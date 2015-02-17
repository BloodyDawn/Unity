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
package org.l2junity.log.formatter;

import java.util.logging.LogRecord;

import org.l2junity.gameserver.model.actor.instance.PlayerInstance;
import org.l2junity.gameserver.network.L2GameClient;

public class AccountingFormatter extends AbstractFormatter
{
	@Override
	public String format(LogRecord record)
	{
		final Object[] params = record.getParameters();
		final StringBuilder output = new StringBuilder(32 + record.getMessage().length() + (params != null ? 10 * params.length : 0));
		output.append(super.format(record));
		
		if (params != null)
		{
			for (Object p : params)
			{
				if (p == null)
				{
					continue;
				}
				
				output.append(", ");
				
				if (p instanceof L2GameClient)
				{
					final L2GameClient client = (L2GameClient) p;
					String address = null;
					try
					{
						if (!client.isDetached())
						{
							address = client.getConnection().getInetAddress().getHostAddress();
						}
					}
					catch (Exception e)
					{
						
					}
					
					switch (client.getState())
					{
						case IN_GAME:
							if (client.getActiveChar() != null)
							{
								output.append(client.getActiveChar().getName());
								output.append("(");
								output.append(client.getActiveChar().getObjectId());
								output.append(") ");
							}
						case AUTHED:
							if (client.getAccountName() != null)
							{
								output.append(client.getAccountName());
								output.append(" ");
							}
						case CONNECTED:
							if (address != null)
							{
								output.append(address);
							}
							break;
						default:
							throw new IllegalStateException("Missing state on switch");
					}
				}
				else if (p instanceof PlayerInstance)
				{
					PlayerInstance player = (PlayerInstance) p;
					output.append(player.getName());
					output.append("(");
					output.append(player.getObjectId());
					output.append(")");
				}
				else
				{
					output.append(p);
				}
			}
		}
		
		output.append(System.lineSeparator());
		return output.toString();
	}
}
