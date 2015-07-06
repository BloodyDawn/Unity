/*
 * Copyright (C) 2004-2015 L2J DataPack
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
package handlers.effecthandlers;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.l2junity.gameserver.datatables.ItemTable;
import org.l2junity.gameserver.model.StatsSet;
import org.l2junity.gameserver.model.actor.instance.PlayerInstance;
import org.l2junity.gameserver.model.conditions.Condition;
import org.l2junity.gameserver.model.effects.AbstractEffect;
import org.l2junity.gameserver.model.items.L2Item;
import org.l2junity.gameserver.model.items.instance.ItemInstance;
import org.l2junity.gameserver.model.skills.BuffInfo;
import org.l2junity.gameserver.network.client.send.InventoryUpdate;
import org.l2junity.gameserver.network.client.send.SystemMessage;
import org.l2junity.gameserver.network.client.send.string.SystemMessageId;

/**
 * Disarm by inventory slot effect implementation. At end of effect, it re-equips that item.
 * @author Nik
 */
public final class Disarmor extends AbstractEffect
{
	private final Map<Integer, Integer> _unequippedItems; // PlayerObjId, ItemObjId
	private final int _slot;
	
	public Disarmor(Condition attachCond, Condition applyCond, StatsSet set, StatsSet params)
	{
		super(attachCond, applyCond, set, params);
		_unequippedItems = new ConcurrentHashMap<>();
		
		String slot = set.getString("slot", "chest");
		if (ItemTable._slots.containsKey(slot))
		{
			_slot = ItemTable._slots.get(slot);
		}
		else
		{
			_slot = L2Item.SLOT_NONE;
			_log.error("Unknown bodypart slot for effect: {}", slot);
		}
		
	}
	
	@Override
	public boolean canStart(BuffInfo info)
	{
		return info.getEffected().isPlayer();
	}
	
	@Override
	public void onStart(BuffInfo info)
	{
		if (!info.getEffected().isPlayer())
		{
			return;
		}
		
		ItemInstance disarmed = disarmItem(info);
		if (disarmed != null)
		{
			info.getEffected().getInventory().blockItemSlot(_slot);
			_unequippedItems.put(info.getEffected().getObjectId(), disarmed.getObjectId());
		}
	}
	
	@Override
	public void onExit(BuffInfo info)
	{
		if (!info.getEffected().isPlayer())
		{
			return;
		}
		
		Integer disarmedObjId = _unequippedItems.get(info.getEffected().getObjectId());
		if ((disarmedObjId != null) && (disarmedObjId > 0))
		{
			PlayerInstance player = info.getEffected().getActingPlayer();
			info.getEffected().getInventory().blockItemSlot(_slot);
			
			ItemInstance item = player.getInventory().getItemByObjectId(disarmedObjId);
			if (item != null)
			{
				player.getInventory().equipItem(item);
				InventoryUpdate iu = new InventoryUpdate();
				iu.addModifiedItem(item);
				player.sendPacket(iu);
			}
		}
		super.onExit(info);
	}
	
	public ItemInstance disarmItem(BuffInfo info)
	{
		PlayerInstance player = info.getEffected().getActingPlayer();
		ItemInstance[] unequiped = player.getInventory().unEquipItemInBodySlotAndRecord(_slot);
		
		// this can be 0 if the user pressed the right mousebutton twice very fast
		if (unequiped.length > 0)
		{
			InventoryUpdate iu = new InventoryUpdate();
			for (ItemInstance itm : unequiped)
			{
				iu.addModifiedItem(itm);
			}
			player.sendPacket(iu);
			player.broadcastUserInfo();
			
			SystemMessage sm = null;
			if (unequiped[0].getEnchantLevel() > 0)
			{
				sm = SystemMessage.getSystemMessage(SystemMessageId.THE_EQUIPMENT_S1_S2_HAS_BEEN_REMOVED);
				sm.addInt(unequiped[0].getEnchantLevel());
				sm.addItemName(unequiped[0]);
			}
			else
			{
				sm = SystemMessage.getSystemMessage(SystemMessageId.S1_HAS_BEEN_UNEQUIPPED);
				sm.addItemName(unequiped[0]);
			}
			player.sendPacket(sm);
			return unequiped[0];
		}
		
		return null;
	}
}
