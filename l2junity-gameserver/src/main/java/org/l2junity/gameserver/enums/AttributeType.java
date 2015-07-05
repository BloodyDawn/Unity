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
package org.l2junity.gameserver.enums;

/**
 * An enum representing all attribute types.
 * @author NosBit
 */
public enum AttributeType
{
	NONE(-1, AttributeType.NONE),
	FIRE(0, AttributeType.WATER),
	WATER(1, AttributeType.FIRE),
	WIND(2, AttributeType.EARTH),
	EARTH(3, AttributeType.WIND),
	HOLY(4, AttributeType.DARK),
	DARK(5, AttributeType.HOLY),;

	private final int _clientId;
	private final AttributeType _opposite;

	AttributeType(int clientId, AttributeType opposite)
	{
		_clientId = clientId;
		_opposite = opposite;
	}

	/**
	 * Gets the client id.
	 * @return the client id
	 */
	public int getClientId()
	{
		return _clientId;
	}

	/**
	 * Gets the opposite.
	 * @return the opposite
	 */
	public AttributeType getOpposite()
	{
		return _opposite;
	}
}
