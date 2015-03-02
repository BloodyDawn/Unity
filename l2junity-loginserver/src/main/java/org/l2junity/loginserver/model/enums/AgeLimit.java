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
package org.l2junity.loginserver.model.enums;

/**
 * {@code AgeLimit} is an {@code enum} representing the age limit such as None, Fifteen and Eighteen.
 * @author Nos
 */
public enum AgeLimit
{
	NONE(0),
	FIFTEEN(15),
	EIGHTEEN(18);
	
	private final int _age;
	
	/**
	 * Creates an age limit instance.
	 * @param age the age
	 */
	private AgeLimit(int age)
	{
		_age = age;
	}
	
	/**
	 * Gets the age.
	 * @return the age
	 */
	public int getAge()
	{
		return _age;
	}
}
