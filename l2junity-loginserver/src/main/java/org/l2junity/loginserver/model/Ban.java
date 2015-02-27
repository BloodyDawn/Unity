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

import org.l2junity.loginserver.datatables.BansTable;

/**
 * @author UnAfraid
 */
public class Ban
{
	private final InetAddress _address;
	private final long _expiration;
	
	public Ban(InetAddress address, long expiration)
	{
		_address = address;
		_expiration = expiration;
	}
	
	public InetAddress getAddress()
	{
		return _address;
	}
	
	public long getExpirationTime()
	{
		return _expiration;
	}
	
	public boolean hasExpired()
	{
		return (_expiration > 0) && (System.currentTimeMillis() > _expiration);
	}
	
	public boolean storeMe()
	{
		return BansTable.getInstance().storeBan(this);
	}
	
	public boolean deleteMe()
	{
		return BansTable.getInstance().deleteBan(this);
	}
}
