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

import org.l2junity.Config;
import org.l2junity.util.Util;

public class ConsoleLogFormatter extends AbstractFormatter
{
	@Override
	public String format(LogRecord record)
	{
		final StringBuilder output = new StringBuilder(128);
		output.append(super.format(record));
		output.append(Config.EOL);
		
		if (record.getThrown() != null)
		{
			try
			{
				output.append(Util.getStackTrace(record.getThrown()));
				output.append(Config.EOL);
			}
			catch (Exception ex)
			{
			}
		}
		return output.toString();
	}
}
