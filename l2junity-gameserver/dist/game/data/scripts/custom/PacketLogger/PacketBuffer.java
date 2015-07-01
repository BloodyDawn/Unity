/*
 * Copyright (C) 2004-2013 L2J DataPack
 * 
 * This file is part of L2J DataPack.
 * 
 * L2J DataPack is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * L2J DataPack is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package custom.PacketLogger;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.FileChannel;

/**
 * @author UnAfraid
 */
public class PacketBuffer
{
	private final ByteBuffer _buffer = ByteBuffer.wrap(new byte[64 * 1024]).order(ByteOrder.LITTLE_ENDIAN);
	private int _packets = 0;
	
	public final void readB(final byte[] dst)
	{
		_buffer.get(dst);
	}
	
	public final void readB(final byte[] dst, final int offset, final int len)
	{
		_buffer.get(dst, offset, len);
	}
	
	public final void writeB(byte[] data)
	{
		_buffer.put(data);
	}
	
	public final void writeC(int val)
	{
		_buffer.put((byte) val);
	}
	
	public final int readC()
	{
		return _buffer.get() & 0xFF;
	}
	
	public final void writeH(int val)
	{
		_buffer.putShort((short) val);
	}
	
	public final int readH()
	{
		return _buffer.getShort() & 0xFFFF;
	}
	
	public final void writeD(int val)
	{
		_buffer.putInt(val);
	}
	
	public final int readD()
	{
		return _buffer.getInt();
	}
	
	public final void writeQ(long val)
	{
		_buffer.putLong(val);
	}
	
	public final long readQ()
	{
		return _buffer.getLong();
	}
	
	public final void writeF(double val)
	{
		_buffer.putDouble(val);
	}
	
	public final double readF()
	{
		return _buffer.getDouble();
	}
	
	public final String readS()
	{
		final StringBuilder sb = new StringBuilder();
		
		char ch;
		while ((ch = _buffer.getChar()) != 0)
		{
			sb.append(ch);
		}
		
		return sb.toString();
	}
	
	public final void writeS(String text)
	{
		if (text != null)
		{
			for (int i = 0; i < text.length(); i++)
			{
				_buffer.putChar(text.charAt(i));
			}
		}
		_buffer.putChar('\000');
	}
	
	public final void increasePackets()
	{
		_packets++;
	}
	
	public final int getPackets()
	{
		return _packets;
	}
	
	public final ByteBuffer getBuffer()
	{
		return _buffer;
	}
	
	public void writeToDisk(File file) throws IOException
	{
		// Write the data to file.
		try (FileOutputStream fs = new FileOutputStream(file);
			FileChannel channel = fs.getChannel())
		{
			// Flip the buffer.
			_buffer.flip();
			
			// Write to disk.
			channel.write(_buffer);
		}
	}
}
