/*
 * Copyright (C) 2004-2014 L2J Server
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
package org.l2junity.loginserver.controllers;

import java.security.MessageDigest;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.l2junity.loginserver.network.client.ClientHandler;

/**
 * @author UnAfraid
 */
public class LoginClientController
{
	private final Logger _log = Logger.getLogger(LoginClientController.class.getName());
	
	@SuppressWarnings("unused")
	private MessageDigest _passwordHashCrypt;
	private final AtomicInteger _connectionId = new AtomicInteger();
	
	protected LoginClientController()
	{
		try
		{
			_passwordHashCrypt = MessageDigest.getInstance("SHA");
		}
		catch (Exception e)
		{
			_log.log(Level.WARNING, getClass().getSimpleName() + ": Failed initializing: " + e.getMessage(), e);
		}
	}
	
	/**
	 * @param serverId
	 * @param client
	 */
	public void tryGameLogin(byte serverId, ClientHandler client)
	{
	}
	
	/**
	 * @return
	 */
	public int getNextConnectionId()
	{
		return _connectionId.getAndIncrement();
	}
	
	public static LoginClientController getInstance()
	{
		return SingletonHolder._instance;
	}
	
	private static final class SingletonHolder
	{
		protected static final LoginClientController _instance = new LoginClientController();
	}
}
