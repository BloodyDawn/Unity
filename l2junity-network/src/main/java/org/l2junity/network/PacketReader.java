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
package org.l2junity.network;

import io.netty.buffer.ByteBuf;

/**
 * @author Nos
 */
public final class PacketReader
{
	private final ByteBuf _buf;
	
	public PacketReader(ByteBuf buf)
	{
		_buf = buf;
	}
	
	/**
	 * Gets the readable bytes.
	 * @return the readable bytes
	 */
	public int getReadableBytes()
	{
		return _buf.readableBytes();
	}
	
	/**
	 * Reads an unsigned byte
	 * @return the unsigned byte
	 * @throws IndexOutOfBoundsException if {@code readableBytes} is less than {@code 1}
	 */
	public short readC()
	{
		return _buf.readUnsignedByte();
	}
	
	/**
	 * Reads an unsigned short
	 * @return the unsigned short
	 * @throws IndexOutOfBoundsException if {@code readableBytes} is less than {@code 2}
	 */
	public int readH()
	{
		return _buf.readUnsignedShort();
	}
	
	/**
	 * Reads an integer
	 * @return the integer
	 * @throws IndexOutOfBoundsException if {@code readableBytes} is less than {@code 4}
	 */
	public int readD()
	{
		return _buf.readInt();
	}
	
	/**
	 * Reads a long
	 * @return the long
	 * @throws IndexOutOfBoundsException if {@code readableBytes} is less than {@code 8}
	 */
	public long readQ()
	{
		return _buf.readLong();
	}
	
	/**
	 * Reads a double
	 * @return the double
	 * @throws IndexOutOfBoundsException if {@code readableBytes} is less than {@code 8}
	 */
	public double readF()
	{
		return _buf.readDouble();
	}
	
	/**
	 * Reads a string
	 * @return the string
	 * @throws IndexOutOfBoundsException if string {@code null} terminator is not found
	 */
	public String readS()
	{
		StringBuilder sb = new StringBuilder();
		
		char chr;
		while ((chr = _buf.readChar()) != 0)
		{
			sb.append(chr);
		}
		return sb.toString();
	}
	
	/**
	 * @param length
	 * @return
	 */
	public byte[] readB(int length)
	{
		byte[] result = new byte[length];
		_buf.readBytes(result);
		return result;
	}
}
