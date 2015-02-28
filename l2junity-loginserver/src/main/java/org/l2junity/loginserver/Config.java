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
import java.util.logging.Level;
import java.util.logging.Logger;

import org.l2junity.commons.util.PropertiesParser;

/**
 * @author UnAfraid
 */
public final class Config
{
	private static final Logger _log = Logger.getLogger(Config.class.getName());
	
	// --------------------------------------------------
	// Constants
	// --------------------------------------------------
	public static final String EOL = System.getProperty("line.separator");
	
	// --------------------------------------------------
	// L2J Property File Definitions
	// --------------------------------------------------
	public static final String LOGIN_CONFIGURATION_FILE = "config/LoginServer.properties";
	public static final String DATABASE_CONFIGURATION_FILE = "config/Database.properties";
	
	// --------------------------------------------------
	// L2J Variable Definitions
	// --------------------------------------------------
	
	public static final String LOG_FOLDER = "log"; // Name of folder for log file
	public static final String LOG_NAME = "config/log.cfg"; // Name of log file
	public static final String ROOT_DIRECTORY = System.getProperty("rootDirectory", ".");
	
	// --------------------------------------------------
	// Server Settings
	// --------------------------------------------------
	public static int GAME_SERVER_LOGIN_PORT;
	public static String GAME_SERVER_LOGIN_HOST;
	public static int GAME_CLIENT_LOGIN_PORT;
	public static String GAME_CLIENT_LOGIN_HOST;
	public static boolean AUTO_CREATE_ACCOUNTS;
	public static boolean SHOW_LICENCE;
	
	// --------------------------------------------------
	// Database Settings
	// --------------------------------------------------
	public static String DATABASE_URL;
	public static String DATABASE_LOGIN;
	public static String DATABASE_PASSWORD;
	public static int DATABASE_MAX_CONNECTIONS;
	public static int DATABASE_MAX_IDLE_TIME;
	
	public static void load()
	{
		try
		{
			final PropertiesParser ServerSettings = new PropertiesParser(new File(ROOT_DIRECTORY, LOGIN_CONFIGURATION_FILE));
			
			GAME_SERVER_LOGIN_HOST = ServerSettings.getString("ServerLoginHostname", "127.0.0.1");
			GAME_SERVER_LOGIN_PORT = ServerSettings.getInt("ServerLoginPort", 2104);
			
			GAME_CLIENT_LOGIN_HOST = ServerSettings.getString("ClientLoginHostname", "*");
			GAME_CLIENT_LOGIN_PORT = ServerSettings.getInt("ClientLoginPort", 2106);
			
			AUTO_CREATE_ACCOUNTS = ServerSettings.getBoolean("AutoCreateAccounts", false);
			
			SHOW_LICENCE = ServerSettings.getBoolean("ShowLicense", true);
		}
		catch (Exception e)
		{
			_log.log(Level.WARNING, Config.class.getSimpleName() + ": Error while loading configuration: ", e);
		}
		
		try
		{
			final PropertiesParser databaseSettings = new PropertiesParser(new File(ROOT_DIRECTORY, DATABASE_CONFIGURATION_FILE));
			DATABASE_URL = databaseSettings.getString("URL", "jdbc:mysql://localhost/l2jls2");
			DATABASE_LOGIN = databaseSettings.getString("Login", "root");
			DATABASE_PASSWORD = databaseSettings.getString("Password", "");
			DATABASE_MAX_CONNECTIONS = databaseSettings.getInt("MaximumDbConnections", 10);
			DATABASE_MAX_IDLE_TIME = databaseSettings.getInt("MaximumDbIdleTime", 0);
		}
		catch (Exception e)
		{
			_log.log(Level.WARNING, Config.class.getSimpleName() + ": Error while loading configuration: ", e);
		}
		
		_log.log(Level.INFO, Config.class.getSimpleName() + ": Initialized!");
	}
}
