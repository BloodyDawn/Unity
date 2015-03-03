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
package org.l2junity.network.codecs;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;

import java.nio.ByteOrder;
import java.util.List;
import java.util.logging.Logger;

import org.l2junity.network.IConnectionState;
import org.l2junity.network.IIncomingPacket;
import org.l2junity.network.IIncomingPackets;
import org.l2junity.network.PacketReader;

/**
 * @author Nos
 */
public class PacketDecoder extends ByteToMessageDecoder
{
	private static final Logger LOGGER = Logger.getLogger(PacketDecoder.class.getName());
	
	private final ByteOrder _byteOrder;
	private final IIncomingPackets<?>[] _incomingPackets;
	
	public <T extends IIncomingPackets<?>> PacketDecoder(ByteOrder byteOrder, IIncomingPackets<?>[] incomingPackets)
	{
		_byteOrder = byteOrder;
		_incomingPackets = incomingPackets;
	}
	
	@Override
	protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out)
	{
		if ((in == null) || !in.isReadable())
		{
			return;
		}
		
		if (in.order() != _byteOrder)
		{
			in = in.order(_byteOrder);
		}
		
		try
		{
			final short packetId = in.readUnsignedByte();
			if (packetId >= _incomingPackets.length)
			{
				return;
			}
			
			final IIncomingPackets<?> incomingPacket = _incomingPackets[packetId];
			if (incomingPacket == null)
			{
				LOGGER.fine(getClass().getSimpleName() + ": Unknown packet: " + Integer.toHexString(packetId));
				return;
			}
			
			final IConnectionState connectionState = ctx.channel().attr(IConnectionState.ATTRIBUTE_KEY).get();
			if ((connectionState == null) || !incomingPacket.getConnectionStates().contains(connectionState))
			{
				LOGGER.warning(" Connection at invalid state: " + connectionState + " Required State: " + incomingPacket.getConnectionStates());
				return;
			}
			
			final IIncomingPacket<?> packet = incomingPacket.newIncomingPacket();
			if ((packet != null) && packet.read(new PacketReader(in)))
			{
				out.add(packet);
			}
		}
		finally
		{
			// We always consider that we read whole packet.
			in.readerIndex(in.writerIndex());
		}
	}
}
