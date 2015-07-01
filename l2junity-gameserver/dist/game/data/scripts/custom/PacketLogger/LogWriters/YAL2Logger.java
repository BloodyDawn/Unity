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
package custom.PacketLogger.LogWriters;

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.nio.ByteBuffer;
import java.util.Calendar;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.l2junity.Config;
import org.l2junity.gameserver.network.client.L2GameClient;

import custom.PacketLogger.PacketBuffer;

/**
 * @author UnAfraid
 */
public class YAL2Logger extends PacketBuffer implements IPacketHandler
{
	private static final Logger _log = Logger.getLogger(YAL2Logger.class.getName());
	private static final String YAL2_PROTOCOL_NAME = "Infinite Odyssey";
	private static final byte YAL2_VERSION = 0x07;
	private final L2GameClient _client;
	
	public YAL2Logger(L2GameClient client)
	{
		_client = client;
		writeYAL2Header();
	}
	
	private void writeYAL2Header()
	{
		writeC(YAL2_VERSION); // YAL2 log version
		writeD(0x00); // Packets count
		writeC(0x00); // Split log
		writeH(0x00); // Client port
		writeH(Config.PORT_GAME); // Server port
		writeB(getClientIp()); // Client ip
		writeB(getServerIp()); // Server ip
		writeS(YAL2_PROTOCOL_NAME);
		writeS("Log sniffed from game server."); // comments
		writeS("L2J"); // Server type
		writeQ(0x00); // Analyser bit set
		writeQ(0x00); // Session id
		writeC(0x00); // Is encrypted
	}
	
	@Override
	public synchronized void handlePacket(byte[] data, boolean clientSide)
	{
		// Write packet data.
		writeC(!clientSide ? (byte) 0x01 : 0x00);
		writeH(data.length + 2);
		writeQ(System.currentTimeMillis());
		writeB(data);
		
		// Update packet count
		increasePackets();
	}
	
	@Override
	public void notifyTerminate()
	{
		try
		{
			final File curDir = new File("log/packetlogs/" + _client.getAccountName());
			if (!curDir.exists())
			{
				curDir.mkdirs();
			}
			final Calendar cal = Calendar.getInstance();
			final String fileName = _client.getConnectionAddress().getHostAddress() + " " + cal.get(Calendar.DAY_OF_WEEK) + "-" + (cal.get(Calendar.MONTH) + 1) + "-" + cal.get(Calendar.YEAR) + " " + cal.get(Calendar.HOUR_OF_DAY) + "-" + cal.get(Calendar.MINUTE) + "-" + cal.get(Calendar.SECOND) + ".l2l";
			
			writeToDisk(new File(curDir.getAbsoluteFile(), fileName));
		}
		catch (IOException e)
		{
			_log.log(Level.WARNING, "Error while saving log file!", e);
		}
	}
	
	@Override
	public void writeToDisk(File file) throws IOException
	{
		// Update packet count.
		final ByteBuffer buffer = getBuffer();
		int pos = buffer.position();
		buffer.position(1);
		buffer.putInt(getPackets());
		buffer.position(pos);
		
		super.writeToDisk(file);
	}
	
	private byte[] getClientIp()
	{
		try
		{
			return _client.getConnectionAddress().getAddress();
		}
		catch (Exception e)
		{
			return new byte[4];
		}
	}
	
	private byte[] getServerIp()
	{
		try
		{
			return InetAddress.getLocalHost().getAddress();
		}
		catch (Exception e)
		{
			return new byte[4];
		}
	}
}
