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
package org.l2junity.gameserver.network.client.recv.alchemy;

import org.l2junity.commons.util.Rnd;
import org.l2junity.gameserver.data.xml.impl.AlchemyData;
import org.l2junity.gameserver.enums.Race;
import org.l2junity.gameserver.model.actor.instance.PlayerInstance;
import org.l2junity.gameserver.model.alchemy.AlchemyCraftData;
import org.l2junity.gameserver.model.holders.ItemHolder;
import org.l2junity.gameserver.model.items.instance.ItemInstance;
import org.l2junity.gameserver.network.client.L2GameClient;
import org.l2junity.gameserver.network.client.recv.IClientIncomingPacket;
import org.l2junity.gameserver.network.client.send.InventoryUpdate;
import org.l2junity.gameserver.network.client.send.alchemy.ExAlchemyConversion;
import org.l2junity.network.PacketReader;

/**
 * @author Sdw
 */
public class RequestAlchemyConversion implements IClientIncomingPacket
{
	private int _craftTimes;
	private int _skillId;
	private int _skillLevel;
	
	// private final Set<ItemHolder> _ingredients = new HashSet<>();
	
	@Override
	public boolean read(PacketReader packet)
	{
		_craftTimes = packet.readD();
		packet.readH(); // TODO: Find me
		_skillId = packet.readD();
		_skillLevel = packet.readD();
		
		// final int ingredientsSize = packet.readD();
		// for (int i = 0; i < ingredientsSize; i++)
		// {
		// _ingredients.add(new ItemHolder(packet.readD(), packet.readQ()));
		// }
		return true;
	}
	
	@Override
	public void run(L2GameClient client)
	{
		final PlayerInstance player = client.getActiveChar();
		if ((player == null) || (player.getRace() != Race.ERTHEIA))
		{
			return;
		}
		
		final AlchemyCraftData data = AlchemyData.getInstance().getCraftData(_skillId, _skillLevel);
		if (data == null)
		{
			_log.warn("Missing data");
			return;
		}
		
		// if (!_ingredients.equals(data.getIngredients()))
		// {
		// _log.warn("Client ingredients are not same as server ingredients for alchemy conversion player: {}", player);
		// return;
		// }
		
		// TODO: Figure out the chance
		final int baseChance = 50;
		
		int successCount = 0;
		int failureCount = 0;
		
		// Run _craftItems iteration of item craft
		final InventoryUpdate ui = new InventoryUpdate();
		CRAFTLOOP: for (int i = 0; i < _craftTimes; i++)
		{
			// for each tries, check if player have enough items and destroy
			for (ItemHolder ingredient : data.getIngredients())
			{
				final ItemInstance item = player.getInventory().getItemByItemId(ingredient.getId());
				if (item == null)
				{
					break CRAFTLOOP;
				}
				if (item.getCount() < ingredient.getCount())
				{
					break CRAFTLOOP;
				}
				
				player.getInventory().destroyItem("Alchemy", item, ingredient.getCount(), player, null);
				ui.addItem(item);
			}
			
			if (Rnd.get(100) < baseChance)
			{
				successCount++;
			}
			else
			{
				failureCount++;
			}
		}
		
		if (successCount > 0)
		{
			final ItemInstance item = player.getInventory().addItem("Alchemy", data.getProductionSuccess().getId(), data.getProductionSuccess().getCount() * successCount, player, null);
			ui.addItem(item);
		}
		
		if (failureCount > 0)
		{
			final ItemInstance item = player.getInventory().addItem("Alchemy", data.getProductionFailure().getId(), data.getProductionFailure().getCount() * failureCount, player, null);
			ui.addItem(item);
		}
		
		player.sendPacket(new ExAlchemyConversion(successCount, failureCount));
		player.sendPacket(ui);
		
	}
}
