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
package org.l2junity.loginserver.datatables;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.l2junity.loginserver.DatabaseFactory;
import org.l2junity.loginserver.model.Account;

/**
 * @author UnAfraid
 */
public class AccountsTable
{
	private static final Logger _log = Logger.getLogger(AccountsTable.class.getName());
	
	private static final String INSERT_QUERY = "INSERT INTO accounts (login, password, lastServer, lastacive, lastIP) VALUES (?, ?, ?, ?, ?)";
	private static final String SELECT_QUERY = "SELECT * FROM accounts WHERE login = ?";
	private static final String UPDATE_QUERY = "UPDATE accounts SET lastServer = ?, lastacive = ?, lastIP = ? WHERE login = ?";
	private static final String ACCOUNT_IPAUTH_SELECT = "SELECT * FROM accounts_ipauth WHERE login = ?";
	
	protected AccountsTable()
	{
	}
	
	public Account getAccount(String name)
	{
		Account acccount = null;
		try (Connection con = DatabaseFactory.getInstance().getConnection())
		{
			try (PreparedStatement st = con.prepareStatement(SELECT_QUERY))
			{
				st.setString(1, name);
				try (ResultSet rset = st.executeQuery())
				{
					if (rset.next())
					{
						acccount = new Account(rset);
					}
				}
			}
			
			if (acccount != null)
			{
				try (PreparedStatement ps = con.prepareStatement(ACCOUNT_IPAUTH_SELECT))
				{
					ps.setString(1, name);
					try (ResultSet rset = ps.executeQuery())
					{
						InetAddress address;
						String type;
						while (rset.next())
						{
							address = InetAddress.getByName(rset.getString("ip"));
							type = rset.getString("type");
							
							if ("allow".equalsIgnoreCase(type))
							{
								acccount.addWhitelistedIp(address);
							}
							else if ("deny".equalsIgnoreCase(type))
							{
								acccount.addBlacklistIp(address);
							}
						}
					}
				}
			}
		}
		catch (SQLException | UnknownHostException e)
		{
			_log.log(Level.WARNING, "Error while selecting account " + name, e);
		}
		return acccount;
	}
	
	public boolean insertAccount(Account acc)
	{
		if (acc.hasChanges())
		{
			try (Connection con = DatabaseFactory.getInstance().getConnection();
				PreparedStatement st = con.prepareStatement(INSERT_QUERY))
			{
				st.setString(1, acc.getAccount());
				st.setString(2, acc.getPassword());
				st.setInt(3, acc.getLastServer());
				st.setLong(4, acc.getLastAccess());
				st.setString(5, acc.getLastAddress().getHostAddress());
				st.execute();
				return true;
			}
			catch (SQLException e)
			{
				_log.log(Level.WARNING, "Error while insert account " + acc.getAccount(), e);
			}
		}
		return false;
	}
	
	public boolean updateAccount(Account acc)
	{
		if (acc.hasChanges())
		{
			try (Connection con = DatabaseFactory.getInstance().getConnection();
				PreparedStatement st = con.prepareStatement(UPDATE_QUERY))
			{
				st.setInt(1, acc.getLastServer());
				st.setLong(2, acc.getLastAccess());
				st.setString(3, acc.getLastAddress().getHostAddress());
				st.setString(4, acc.getAccount());
				st.execute();
				return true;
			}
			catch (SQLException e)
			{
				_log.log(Level.WARNING, "Error while updating account " + acc.getAccount(), e);
			}
		}
		return false;
	}
	
	public static AccountsTable getInstance()
	{
		return SingletonHolder._instance;
	}
	
	private static final class SingletonHolder
	{
		protected static final AccountsTable _instance = new AccountsTable();
	}
}
