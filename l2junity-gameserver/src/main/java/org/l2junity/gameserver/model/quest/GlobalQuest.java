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
package org.l2junity.gameserver.model.quest;

/**
 * @author Pere
 */
public enum GlobalQuest
{
	UNK1(0),
	UNK2(0),
	UNK3(0),
	UNK4(0),
	UNK5(0),
	UNK6(0),
	UNK7(0),
	UNK8(0),
	UNK9(0),
	UNK10(0),
	STARTING(0),
	YE_SAGIRA(0),
	UNK11(0), // Seems like the quest in Forsaken Plains: Q10402 Nowhere to run
	UNK12(0),
	UNK13(0),
	UNK14(0),
	UNK15(0),
	UNK16(0),
	UNK17(0),
	UNK18(0),
	UNK19(0),
	UNK20(0),
	UNK21(0),
	UNK22(0),
	UNK23(0),
	UNK24(0),
	UNK25(0),
	UNK26(0),
	UNK27(0),
	UNK28(0),
	UNK29(0),
	UNK30(0);
	private final int _value;
	
	GlobalQuest(int value)
	{
		_value = value;
	}
	
	public int getValue()
	{
		return _value;
	}
}
