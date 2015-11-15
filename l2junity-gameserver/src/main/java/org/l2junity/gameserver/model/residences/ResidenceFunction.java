/*
 * Copyright (C) 2004-2015 L2J Unity
 * 
 * This file is part of L2J Unity.
 * 
 * L2J Unity is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * L2J Unity is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package org.l2junity.gameserver.model.residences;

import java.util.concurrent.ScheduledFuture;

import org.l2junity.gameserver.ThreadPoolManager;
import org.l2junity.gameserver.data.sql.impl.ClanTable;
import org.l2junity.gameserver.data.xml.impl.ResidenceFunctionsData;
import org.l2junity.gameserver.model.L2Clan;
import org.l2junity.gameserver.model.itemcontainer.ItemContainer;
import org.l2junity.gameserver.model.items.instance.ItemInstance;
import org.l2junity.gameserver.network.client.send.AgitDecoInfo;

/**
 * @author UnAfraid
 */
public class ResidenceFunction
{
	private final int _id;
	private long _expiration;
	private final int _ownerId;
	private final AbstractResidence _residense;
	private ScheduledFuture<?> _task;
	
	public ResidenceFunction(int id, long expiration, int ownerId, AbstractResidence residense)
	{
		_id = id;
		_expiration = expiration;
		_ownerId = ownerId;
		_residense = residense;
		init();
	}
	
	private void init()
	{
		final ResidenceFunctionTemplate template = getTemplate();
		if ((template != null) && (_expiration > System.currentTimeMillis()))
		{
			_task = ThreadPoolManager.getInstance().scheduleGeneral(this::onFunctionExpiration, _expiration - System.currentTimeMillis());
		}
	}
	
	public int getId()
	{
		return _id;
	}
	
	public long getExpiration()
	{
		return _expiration;
	}
	
	public int getOwnerId()
	{
		return _ownerId;
	}
	
	public ResidenceFunctionTemplate getTemplate()
	{
		return ResidenceFunctionsData.getInstance().getFunctions(_id);
	}
	
	private void onFunctionExpiration()
	{
		if (!reactivate())
		{
			_residense.removeFunction(this);
			
			final L2Clan clan = ClanTable.getInstance().getClan(_ownerId);
			if (clan != null)
			{
				clan.broadcastToOnlineMembers(new AgitDecoInfo(_residense));
			}
		}
	}
	
	public boolean reactivate()
	{
		final ResidenceFunctionTemplate template = getTemplate();
		if (template == null)
		{
			return false;
		}
		
		final L2Clan clan = ClanTable.getInstance().getClan(_ownerId);
		if (clan == null)
		{
			return false;
		}
		
		final ItemContainer wh = clan.getWarehouse();
		final ItemInstance item = wh.getItemByItemId(template.getCost().getId());
		if ((item == null) || (item.getCount() < template.getCost().getCount()))
		{
			return false;
		}
		
		if (wh.destroyItem("FunctionFee", item, template.getCost().getCount(), null, this) != null)
		{
			_expiration = System.currentTimeMillis() + (template.getDuration().getSeconds() * 1000);
			init();
		}
		return true;
	}
	
	public void cancelExpiration()
	{
		if ((_task != null) && !_task.isDone())
		{
			_task.cancel(true);
		}
		_task = null;
	}
}
