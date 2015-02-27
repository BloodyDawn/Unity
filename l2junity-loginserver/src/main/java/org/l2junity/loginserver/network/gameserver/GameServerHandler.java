/*
 * Copyright (C) 2004-2013 L2J Server
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
package org.l2junity.loginserver.network.gameserver;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author UnAfraid
 */
public class GameServerHandler extends SimpleChannelInboundHandler<Object>
{
	private static final Logger _log = Logger.getLogger(GameServerHandler.class.getName());
	
	protected GameServerHandler()
	{
		
	}
	
	@Override
	protected void messageReceived(ChannelHandlerContext ctx, Object msg)
	{
		// TODO Auto-generated method stub
		
	}
	
	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause)
	{
		// Close the connection when an exception is raised.
		_log.log(Level.WARNING, "Unexpected exception from downstream.", cause);
		ctx.close();
	}
}