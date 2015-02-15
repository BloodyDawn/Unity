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
package org.l2junity.gameserver.util;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.l2junity.Config;
import org.l2junity.DatabaseFactory;
import org.l2junity.gameserver.ThreadPoolManager;
import org.l2junity.gameserver.data.xml.impl.NpcData;
import org.l2junity.gameserver.data.xml.impl.PetDataTable;
import org.l2junity.gameserver.model.PetData;
import org.l2junity.gameserver.model.World;
import org.l2junity.gameserver.model.actor.Npc;
import org.l2junity.gameserver.model.actor.Summon;
import org.l2junity.gameserver.model.actor.instance.L2PcInstance;
import org.l2junity.gameserver.model.actor.instance.L2PetInstance;
import org.l2junity.gameserver.model.actor.templates.L2NpcTemplate;
import org.l2junity.gameserver.model.items.instance.ItemInstance;
import org.l2junity.gameserver.network.SystemMessageId;
import org.l2junity.gameserver.network.serverpackets.ExAdenaInvenCount;
import org.l2junity.gameserver.network.serverpackets.ExUserInfoInvenWeight;
import org.l2junity.gameserver.network.serverpackets.InventoryUpdate;
import org.l2junity.gameserver.network.serverpackets.MagicSkillLaunched;
import org.l2junity.gameserver.network.serverpackets.MagicSkillUse;
import org.l2junity.gameserver.network.serverpackets.SystemMessage;

/**
 * UnAfraid: TODO: MOVE IT TO DP AI
 */
public final class Evolve
{
	protected static final Logger _log = Logger.getLogger(Evolve.class.getName());
	
	public static final boolean doEvolve(L2PcInstance player, Npc npc, int itemIdtake, int itemIdgive, int petminlvl)
	{
		if ((itemIdtake == 0) || (itemIdgive == 0) || (petminlvl == 0))
		{
			return false;
		}
		
		final Summon pet = player.getPet();
		if (pet == null)
		{
			return false;
		}
		
		final L2PetInstance currentPet = (L2PetInstance) pet;
		if (currentPet.isAlikeDead())
		{
			Util.handleIllegalPlayerAction(player, "Player " + player.getName() + " tried to use death pet exploit!", Config.DEFAULT_PUNISH);
			return false;
		}
		
		ItemInstance item = null;
		long petexp = currentPet.getStat().getExp();
		String oldname = currentPet.getName();
		int oldX = currentPet.getX();
		int oldY = currentPet.getY();
		int oldZ = currentPet.getZ();
		
		PetData oldData = PetDataTable.getInstance().getPetDataByItemId(itemIdtake);
		
		if (oldData == null)
		{
			return false;
		}
		
		int oldnpcID = oldData.getNpcId();
		
		if ((currentPet.getStat().getLevel() < petminlvl) || (currentPet.getId() != oldnpcID))
		{
			return false;
		}
		
		PetData petData = PetDataTable.getInstance().getPetDataByItemId(itemIdgive);
		
		if (petData == null)
		{
			return false;
		}
		
		int npcID = petData.getNpcId();
		
		if (npcID == 0)
		{
			return false;
		}
		
		L2NpcTemplate npcTemplate = NpcData.getInstance().getTemplate(npcID);
		
		currentPet.unSummon(player);
		
		// deleting old pet item
		currentPet.destroyControlItem(player, true);
		
		item = player.getInventory().addItem("Evolve", itemIdgive, 1, player, npc);
		
		// Summoning new pet
		L2PetInstance petSummon = L2PetInstance.spawnPet(npcTemplate, player, item);
		
		if (petSummon == null)
		{
			return false;
		}
		
		// Fix for non-linear baby pet exp
		long _minimumexp = petSummon.getStat().getExpForLevel(petminlvl);
		if (petexp < _minimumexp)
		{
			petexp = _minimumexp;
		}
		
		petSummon.getStat().addExp(petexp);
		petSummon.setCurrentHp(petSummon.getMaxHp());
		petSummon.setCurrentMp(petSummon.getMaxMp());
		petSummon.setCurrentFed(petSummon.getMaxFed());
		petSummon.setTitle(player.getName());
		petSummon.setName(oldname);
		petSummon.setRunning();
		petSummon.storeMe();
		
		player.setPet(petSummon);
		
		player.sendPacket(new MagicSkillUse(npc, 2046, 1, 1000, 600000));
		player.sendPacket(SystemMessageId.SUMMONING_YOUR_PET);
		// L2World.getInstance().storeObject(petSummon);
		petSummon.spawnMe(oldX, oldY, oldZ);
		petSummon.startFeed();
		item.setEnchantLevel(petSummon.getLevel());
		
		ThreadPoolManager.getInstance().scheduleGeneral(new EvolveFinalizer(player, petSummon), 900);
		
		if (petSummon.getCurrentFed() <= 0)
		{
			ThreadPoolManager.getInstance().scheduleGeneral(new EvolveFeedWait(player, petSummon), 60000);
		}
		else
		{
			petSummon.startFeed();
		}
		
		return true;
	}
	
	public static final boolean doRestore(L2PcInstance player, Npc npc, int itemIdtake, int itemIdgive, int petminlvl)
	{
		if ((itemIdtake == 0) || (itemIdgive == 0) || (petminlvl == 0))
		{
			return false;
		}
		
		ItemInstance item = player.getInventory().getItemByItemId(itemIdtake);
		if (item == null)
		{
			return false;
		}
		
		int oldpetlvl = item.getEnchantLevel();
		if (oldpetlvl < petminlvl)
		{
			oldpetlvl = petminlvl;
		}
		
		PetData oldData = PetDataTable.getInstance().getPetDataByItemId(itemIdtake);
		if (oldData == null)
		{
			return false;
		}
		
		PetData petData = PetDataTable.getInstance().getPetDataByItemId(itemIdgive);
		if (petData == null)
		{
			return false;
		}
		
		int npcId = petData.getNpcId();
		if (npcId == 0)
		{
			return false;
		}
		
		L2NpcTemplate npcTemplate = NpcData.getInstance().getTemplate(npcId);
		
		// deleting old pet item
		ItemInstance removedItem = player.getInventory().destroyItem("PetRestore", item, player, npc);
		SystemMessage sm = SystemMessage.getSystemMessage(SystemMessageId.S1_DISAPPEARED);
		sm.addItemName(removedItem);
		player.sendPacket(sm);
		
		// Give new pet item
		ItemInstance addedItem = player.getInventory().addItem("PetRestore", itemIdgive, 1, player, npc);
		
		// Summoning new pet
		L2PetInstance petSummon = L2PetInstance.spawnPet(npcTemplate, player, addedItem);
		if (petSummon == null)
		{
			return false;
		}
		
		long _maxexp = petSummon.getStat().getExpForLevel(oldpetlvl);
		
		petSummon.getStat().addExp(_maxexp);
		petSummon.setCurrentHp(petSummon.getMaxHp());
		petSummon.setCurrentMp(petSummon.getMaxMp());
		petSummon.setCurrentFed(petSummon.getMaxFed());
		petSummon.setTitle(player.getName());
		petSummon.setRunning();
		petSummon.storeMe();
		
		player.setPet(petSummon);
		
		player.sendPacket(new MagicSkillUse(npc, 2046, 1, 1000, 600000));
		player.sendPacket(SystemMessageId.SUMMONING_YOUR_PET);
		// L2World.getInstance().storeObject(petSummon);
		petSummon.spawnMe(player.getX(), player.getY(), player.getZ());
		petSummon.startFeed();
		addedItem.setEnchantLevel(petSummon.getLevel());
		
		// Inventory update
		InventoryUpdate iu = new InventoryUpdate();
		iu.addRemovedItem(removedItem);
		player.sendPacket(iu);
		
		player.sendPacket(new ExUserInfoInvenWeight(player));
		player.sendPacket(new ExAdenaInvenCount(player));
		
		player.broadcastUserInfo();
		
		World world = World.getInstance();
		world.removeObject(removedItem);
		
		ThreadPoolManager.getInstance().scheduleGeneral(new EvolveFinalizer(player, petSummon), 900);
		
		if (petSummon.getCurrentFed() <= 0)
		{
			ThreadPoolManager.getInstance().scheduleGeneral(new EvolveFeedWait(player, petSummon), 60000);
		}
		else
		{
			petSummon.startFeed();
		}
		
		// pet control item no longer exists, delete the pet from the db
		try (Connection con = DatabaseFactory.getInstance().getConnection();
			PreparedStatement ps = con.prepareStatement("DELETE FROM pets WHERE item_obj_id=?"))
		{
			ps.setInt(1, removedItem.getObjectId());
			ps.execute();
		}
		catch (Exception e)
		{
		}
		return true;
	}
	
	static final class EvolveFeedWait implements Runnable
	{
		private final L2PcInstance _activeChar;
		private final L2PetInstance _petSummon;
		
		EvolveFeedWait(L2PcInstance activeChar, L2PetInstance petSummon)
		{
			_activeChar = activeChar;
			_petSummon = petSummon;
		}
		
		@Override
		public void run()
		{
			try
			{
				if (_petSummon.getCurrentFed() <= 0)
				{
					_petSummon.unSummon(_activeChar);
				}
				else
				{
					_petSummon.startFeed();
				}
			}
			catch (Exception e)
			{
				_log.log(Level.WARNING, "", e);
			}
		}
	}
	
	static final class EvolveFinalizer implements Runnable
	{
		private final L2PcInstance _activeChar;
		private final L2PetInstance _petSummon;
		
		EvolveFinalizer(L2PcInstance activeChar, L2PetInstance petSummon)
		{
			_activeChar = activeChar;
			_petSummon = petSummon;
		}
		
		@Override
		public void run()
		{
			try
			{
				_activeChar.sendPacket(new MagicSkillLaunched(_activeChar, 2046, 1));
				_petSummon.setFollowStatus(true);
				_petSummon.setShowSummonAnimation(false);
			}
			catch (Throwable e)
			{
				_log.log(Level.WARNING, "", e);
			}
		}
	}
}
