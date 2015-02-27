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

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.Base64;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.l2junity.commons.util.Rnd;
import org.l2junity.loginserver.Config;
import org.l2junity.loginserver.datatables.AccountsTable;
import org.l2junity.loginserver.model.Account;
import org.l2junity.loginserver.network.client.ClientHandler;
import org.l2junity.loginserver.network.client.ConnectionState;
import org.l2junity.loginserver.network.client.send.LoginFail2;
import org.l2junity.loginserver.network.client.send.LoginOk;
import org.l2junity.loginserver.network.client.send.ServerList;

/**
 * @author UnAfraid
 */
public class LoginClientController
{
	private final Logger _log = Logger.getLogger(LoginClientController.class.getName());
	
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
	 * @param account
	 * @param password
	 * @param client
	 */
	public void tryAuthLogin(String account, String password, ClientHandler client)
	{
		Account acc = AccountsTable.getInstance().getAccount(account);
		boolean autoCreated = acc == null;
		if (acc == null)
		{
			if (!Config.AUTO_CREATE_ACCOUNTS)
			{
				client.sendPacket(LoginFail2.THE_USERNAME_AND_PASSWORD_DO_NOT_MATCH_PLEASE_CHECK_YOUR_ACCOUNT_INFORMATION_AND_TRY_LOGGING_IN_AGAIN2);
				return;
			}
			acc = new Account(account, password);
			acc.setLastServer(-1);
			acc.setLastAddress(client.getInetAddress());
			AccountsTable.getInstance().insertAccount(acc);
			_log.info("Creating account for: " + account);
		}
		else
		{
			// TODO: Add more checks
			
			final byte[] decodePW = Base64.getDecoder().decode(acc.getPassword());
			final byte[] cryptPW = _passwordHashCrypt.digest(password.getBytes(StandardCharsets.UTF_8));
			if (!Arrays.equals(decodePW, cryptPW))
			{
				client.sendPacket(LoginFail2.THE_USERNAME_AND_PASSWORD_DO_NOT_MATCH_PLEASE_CHECK_YOUR_ACCOUNT_INFORMATION_AND_TRY_LOGGING_IN_AGAIN2);
				return;
			}
		}
		
		if (Config.SHOW_LICENCE)
		{
			final long loginSessionId = Rnd.nextLong();
			client.setLoginSessionId(loginSessionId);
			client.setConnectionState(ConnectionState.AUTHED_LICENCE);
			client.sendPacket(new LoginOk(loginSessionId));
		}
		else
		{
			client.setConnectionState(ConnectionState.AUTHED_SERVER_LIST);
			client.sendPacket(new ServerList(client));
		}
		
		// Update last access and IP
		if (!autoCreated)
		{
			acc.setLastAccess(System.currentTimeMillis());
			acc.setLastAddress(client.getInetAddress());
			AccountsTable.getInstance().updateAccount(acc);
		}
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
