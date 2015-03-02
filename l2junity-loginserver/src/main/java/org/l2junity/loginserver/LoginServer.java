/*
 * Copyright (C) 2004-2013 L2J Server
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
package org.l2junity.loginserver;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;

import org.l2junity.loginserver.network.client.ClientNetworkManager;
import org.l2junity.loginserver.network.client.crypt.KeyManager;
import org.l2junity.loginserver.network.gameserver.GameServerNetworkManager;

/**
 * @author UnAfraid
 */
public class LoginServer
{
	private static final Logger _log = Logger.getLogger(LoginServer.class.getName());
	
	private LoginServer()
	{
		try
		{
			init();
			printSection("Config");
			Config.load();
			
			printSection("Database");
			DatabaseFactory.getInstance();
			
			printSection("Network");
			KeyManager.getInstance();
			GameServerNetworkManager.getInstance().start();
			ClientNetworkManager.getInstance().start();
		}
		catch (Exception e)
		{
			_log.log(Level.WARNING, "Error while initializing: ", e);
		}
	}
	
	private static final void init()
	{
		// Create log folder
		File logFolder = new File(Config.LOG_FOLDER);
		logFolder.mkdir();
		
		// Create input stream for log file -- or store file data into memory
		try (InputStream is = new FileInputStream(new File(Config.ROOT_DIRECTORY, Config.LOG_NAME)))
		{
			LogManager.getLogManager().readConfiguration(is);
		}
		catch (IOException e)
		{
			_log.warning(Config.class.getSimpleName() + ": " + e.getMessage());
		}
	}
	
	public static void printSection(String s)
	{
		s = "=[ " + s + " ]";
		while (s.length() < 78)
		{
			s = "-" + s;
		}
		_log.info(s);
	}
	
	public static void main(String[] args)
	{
		new LoginServer();
	}
}
