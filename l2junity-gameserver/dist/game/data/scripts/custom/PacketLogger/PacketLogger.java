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

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.l2junity.gameserver.model.actor.instance.PlayerInstance;
import org.l2junity.gameserver.model.events.EventType;
import org.l2junity.gameserver.model.events.ListenerRegisterType;
import org.l2junity.gameserver.model.events.annotations.RegisterEvent;
import org.l2junity.gameserver.model.events.annotations.RegisterType;
import org.l2junity.gameserver.model.events.impl.character.player.OnPlayerLogout;
import org.l2junity.gameserver.model.events.impl.server.OnPacketReceived;
import org.l2junity.gameserver.model.events.impl.server.OnPacketSent;
import org.l2junity.gameserver.network.client.L2GameClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ai.npc.AbstractNpcAI;
import custom.PacketLogger.LogWriters.IPacketHandler;
import custom.PacketLogger.LogWriters.YAL2Logger;

/**
 * @author UnAfraid
 */
public final class PacketLogger extends AbstractNpcAI
{
	private final Map<Integer, IPacketHandler> _logs = new ConcurrentHashMap<>();
	private static final Logger LOGGER = LoggerFactory.getLogger(PacketLogger.class);
	
	private PacketLogger()
	{
		super(PacketLogger.class.getSimpleName(), "custom");
	}
	
	@RegisterEvent(EventType.ON_PACKET_RECEIVED)
	@RegisterType(ListenerRegisterType.GLOBAL)
	public void onPacketReceived(OnPacketReceived event)
	{
		handlePacket(event.getClient(), event.getData(), true);
	}
	
	@RegisterEvent(EventType.ON_PACKET_SENT)
	@RegisterType(ListenerRegisterType.GLOBAL)
	public void onPacketSent(OnPacketSent event)
	{
		handlePacket(event.getClient(), event.getData(), false);
	}
	
	@RegisterEvent(EventType.ON_PLAYER_LOGOUT)
	@RegisterType(ListenerRegisterType.GLOBAL)
	public void onPlayerLogout(OnPlayerLogout event)
	{
		final PlayerInstance player = event.getActiveChar();
		final IPacketHandler handler = _logs.remove(player.getClient().getObjectId());
		if (handler != null)
		{
			handler.notifyTerminate();
			LOGGER.info("Ending log session for: {}", player);
		}
	}
	
	private final void handlePacket(L2GameClient client, byte[] data, boolean clientSide)
	{
		if (!_logs.containsKey(client.getObjectId()))
		{
			if (data.length > 0)
			{
				final int opCode = data[0] & 0xFF;
				if (opCode == 0x0E)
				{
					_logs.put(client.getObjectId(), new YAL2Logger(client));
					LOGGER.info("Starting log session for: {}", client);
				}
			}
		}
		
		final IPacketHandler handler = _logs.get(client.getObjectId());
		if (handler != null)
		{
			handler.handlePacket(data, clientSide);
		}
	}
	
	public static void main(String[] args)
	{
		new PacketLogger();
	}
}