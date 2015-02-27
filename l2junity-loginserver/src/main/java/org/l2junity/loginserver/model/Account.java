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
package org.l2junity.loginserver.model;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import org.l2junity.loginserver.datatables.AccountsTable;

/**
 * @author UnAfraid
 */
public class Account
{
	private final String _account;
	private final String _password;
	private int _accessLevel;
	private int _lastServer;
	private long _lastAccess;
	private InetAddress _lastAddress;
	private final AtomicBoolean _hasChanges = new AtomicBoolean(false);
	private volatile List<InetAddress> _ipWhiteList = null;
	private volatile List<InetAddress> _ipBlackList = null;
	
	public Account(String account, String password)
	{
		_account = account;
		_password = password;
		_lastAccess = System.currentTimeMillis();
		_hasChanges.compareAndSet(false, true);
	}
	
	public Account(ResultSet rset) throws SQLException, UnknownHostException
	{
		_account = rset.getString("login");
		_password = rset.getString("password");
		_accessLevel = rset.getInt("accessLevel");
		_lastServer = rset.getInt("lastServer");
		_lastAccess = rset.getLong("lastactive");
		_lastAddress = InetAddress.getByName(rset.getString("lastIP"));
	}
	
	public String getAccount()
	{
		return _account;
	}
	
	public String getPassword()
	{
		return _password;
	}
	
	public int getAccessLevel()
	{
		return _accessLevel;
	}
	
	public int getLastServer()
	{
		return _lastServer;
	}
	
	public void setLastServer(int lastServer)
	{
		if (_lastServer != lastServer)
		{
			_lastAccess = lastServer;
			_hasChanges.compareAndSet(false, true);
		}
	}
	
	public long getLastAccess()
	{
		return _lastAccess;
	}
	
	public void setLastAccess(long lastAccess)
	{
		if (_lastAccess != lastAccess)
		{
			_lastAccess = lastAccess;
			_hasChanges.compareAndSet(false, true);
		}
	}
	
	public InetAddress getLastAddress()
	{
		return _lastAddress;
	}
	
	public void setLastAddress(InetAddress address)
	{
		if (_lastAddress != address)
		{
			_lastAddress = address;
			_hasChanges.compareAndSet(false, true);
		}
	}
	
	public boolean updateMe()
	{
		return AccountsTable.getInstance().updateAccount(this);
	}
	
	public boolean hasChanges()
	{
		return _hasChanges.get();
	}
	
	public boolean getAndSetChanges(boolean val)
	{
		return _hasChanges.getAndSet(val);
	}
	
	public void addWhitelistedIp(InetAddress address)
	{
		if (_ipWhiteList == null)
		{
			synchronized (this)
			{
				if (_ipWhiteList == null)
				{
					_ipWhiteList = Collections.synchronizedList(new ArrayList<InetAddress>());
				}
			}
		}
		_ipWhiteList.add(address);
	}
	
	public boolean hasWhitelistedIps()
	{
		return (_ipWhiteList != null) && !_ipWhiteList.isEmpty();
	}
	
	public boolean isWhitelisted(InetAddress address)
	{
		return ((_ipWhiteList != null) && _ipWhiteList.contains(address));
	}
	
	public void addBlacklistIp(InetAddress address)
	{
		if (_ipBlackList == null)
		{
			synchronized (this)
			{
				if (_ipBlackList == null)
				{
					_ipBlackList = Collections.synchronizedList(new ArrayList<InetAddress>());
				}
			}
		}
		_ipBlackList.add(address);
	}
	
	public boolean hasBlacklistIps()
	{
		return (_ipBlackList != null) && !_ipBlackList.isEmpty();
	}
	
	public boolean isBlacklisted(InetAddress address)
	{
		return ((_ipBlackList != null) && _ipBlackList.contains(address));
	}
}
