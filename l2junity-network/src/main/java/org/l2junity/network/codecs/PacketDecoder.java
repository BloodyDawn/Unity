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
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;

import java.nio.ByteOrder;

import org.l2junity.network.IIncomingPacket;
import org.l2junity.network.IIncomingPackets;
import org.l2junity.network.PacketReader;

/**
 * @author Nos
 */
public class PacketDecoder extends LengthFieldBasedFrameDecoder
{
	private final ByteOrder _byteOrder;
	private final IIncomingPackets<?>[] _incomingPackets;
	
	public <T extends IIncomingPackets<?>> PacketDecoder(ByteOrder byteOrder, int maxPacketSize, IIncomingPackets<?>[] incomingPackets)
	{
		super(byteOrder, maxPacketSize, 0, 2, -2, 2, false);
		_byteOrder = byteOrder;
		_incomingPackets = incomingPackets;
	}
	
	@Override
	protected Object decode(ChannelHandlerContext ctx, ByteBuf in) throws Exception
	{
		ByteBuf frame = (ByteBuf) super.decode(ctx, in);
		if ((frame == null) || !frame.isReadable())
		{
			return null;
		}
		
		if (frame.order() != _byteOrder)
		{
			frame = frame.order(_byteOrder);
		}
		
		final short packetId = frame.readUnsignedByte();
		if (packetId >= _incomingPackets.length)
		{
			System.out.format("Invalid Packet: 0x%02X", packetId);
			return null;
		}
		
		System.out.format("Incoming Packet: 0x%02X ", packetId);
		
		final IIncomingPackets<?> incomingPacket = _incomingPackets[packetId];
		if (incomingPacket == null)
		{
			System.out.println();
			return null;
		}
		
		// Attribute<IConnectionState> attribute = ctx.channel().attr(CONNECTION_STATE);
		// if ((attribute.get() == null) || (attribute.get().getState() != incomingPacket.getState()))
		// {
		// System.out.println(" Connection at invalid state: " + attribute.get() + " Required State: " + incomingPacket.getState());
		// return null;
		// }
		
		System.out.println(" Handler: " + incomingPacket);
		IIncomingPacket<?> packet = incomingPacket.newIncomingPacket();
		return (packet != null) && packet.read(new PacketReader(frame)) ? packet : null;
	}
	
	@Override
	protected ByteBuf extractFrame(ChannelHandlerContext ctx, ByteBuf buffer, int index, int length)
	{
		return buffer.slice(index, length);
	}
}
