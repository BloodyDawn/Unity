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
package org.l2junity.loginserver.network.client;

import io.netty.channel.ChannelInitializer;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;

import java.nio.ByteOrder;

import javax.crypto.SecretKey;

import org.l2junity.loginserver.network.client.crypt.Crypt;
import org.l2junity.loginserver.network.client.crypt.KeyManager;
import org.l2junity.network.codecs.CryptCodec;
import org.l2junity.network.codecs.PacketDecoder;
import org.l2junity.network.codecs.PacketEncoder;

/**
 * @author Nos
 */
public class ClientInitializer extends ChannelInitializer<SocketChannel>
{
	private static final PacketEncoder packetEncoder = new PacketEncoder(ByteOrder.LITTLE_ENDIAN, 0x8000);
	
	@Override
	protected void initChannel(SocketChannel ch)
	{
		final SecretKey blowfishKey = KeyManager.getInstance().generateBlowfishKey();
		ch.pipeline().addLast("crypt-codec", new CryptCodec(new Crypt(blowfishKey)));
		ch.pipeline().addLast(new LoggingHandler(LogLevel.INFO));
		ch.pipeline().addLast("packet-decoder", new PacketDecoder(ByteOrder.LITTLE_ENDIAN, 0x8000, IncomingPackets.PACKET_ARRAY));
		ch.pipeline().addLast("packet-encoder", packetEncoder);
		ch.pipeline().addLast(new ClientHandler(blowfishKey));
	}
}
