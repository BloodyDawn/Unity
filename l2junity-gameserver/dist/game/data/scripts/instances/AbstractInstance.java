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
package instances;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.logging.Logger;

import org.l2junity.Config;
import org.l2junity.gameserver.enums.InstanceReenterType;
import org.l2junity.gameserver.instancemanager.InstanceManager;
import org.l2junity.gameserver.model.World;
import org.l2junity.gameserver.model.actor.Npc;
import org.l2junity.gameserver.model.actor.Summon;
import org.l2junity.gameserver.model.actor.instance.PlayerInstance;
import org.l2junity.gameserver.model.entity.Instance;
import org.l2junity.gameserver.model.holders.InstanceReenterTimeHolder;
import org.l2junity.gameserver.model.instancezone.InstanceWorld;
import org.l2junity.gameserver.model.skills.BuffInfo;
import org.l2junity.gameserver.network.client.SystemMessageId;
import org.l2junity.gameserver.network.client.send.SystemMessage;

import ai.npc.AbstractNpcAI;

/**
 * Abstract class for Instances.
 * @author FallenAngel
 */
public abstract class AbstractInstance extends AbstractNpcAI
{
	public final Logger _log = Logger.getLogger(getClass().getSimpleName());
	
	public AbstractInstance(String name, String desc)
	{
		super(name, desc);
	}
	
	public AbstractInstance(String name)
	{
		super(name, "instances");
	}
	
	protected void enterInstance(PlayerInstance player, String template, int templateId)
	{
		enterInstance(player, new InstanceWorld(), template, templateId);
	}
	
	protected void enterInstance(PlayerInstance player, InstanceWorld instance, String template, int templateId)
	{
		final InstanceWorld world = InstanceManager.getInstance().getPlayerWorld(player);
		if (world != null)
		{
			if (world.getTemplateId() == templateId)
			{
				onEnterInstance(player, world, false);
				
				final Instance inst = InstanceManager.getInstance().getInstance(world.getInstanceId());
				if (inst.isRemoveBuffEnabled())
				{
					handleRemoveBuffs(player, world);
				}
				return;
			}
			player.sendPacket(SystemMessageId.YOU_HAVE_ENTERED_ANOTHER_INSTANT_ZONE_THEREFORE_YOU_CANNOT_ENTER_CORRESPONDING_DUNGEON);
			return;
		}
		
		if (checkConditions(player))
		{
			instance.setInstanceId(InstanceManager.getInstance().createDynamicInstance(template));
			instance.setTemplateId(templateId);
			instance.setStatus(0);
			InstanceManager.getInstance().addWorld(instance);
			onEnterInstance(player, instance, true);
			
			final Instance inst = InstanceManager.getInstance().getInstance(instance.getInstanceId());
			if (inst.getReenterType() == InstanceReenterType.ON_INSTANCE_ENTER)
			{
				handleReenterTime(instance);
			}
			
			if (inst.isRemoveBuffEnabled())
			{
				handleRemoveBuffs(instance);
			}
			
			if (Config.DEBUG_INSTANCES)
			{
				_log.info("Instance " + inst.getName() + " (" + instance.getTemplateId() + ") has been created by player " + player.getName());
			}
		}
	}
	
	protected void finishInstance(InstanceWorld world)
	{
		finishInstance(world, Config.INSTANCE_FINISH_TIME);
	}
	
	protected void finishInstance(InstanceWorld world, int duration)
	{
		final Instance inst = InstanceManager.getInstance().getInstance(world.getInstanceId());
		
		if (inst.getReenterType() == InstanceReenterType.ON_INSTANCE_FINISH)
		{
			handleReenterTime(world);
		}
		
		if (duration == 0)
		{
			InstanceManager.getInstance().destroyInstance(inst.getId());
		}
		else if (duration > 0)
		{
			inst.setDuration(duration);
			inst.setEmptyDestroyTime(0);
		}
	}
	
	protected void handleReenterTime(InstanceWorld world)
	{
		final Instance inst = InstanceManager.getInstance().getInstance(world.getInstanceId());
		final List<InstanceReenterTimeHolder> reenterData = inst.getReenterData();
		
		long time = -1;
		
		for (InstanceReenterTimeHolder data : reenterData)
		{
			if (data.getTime() > 0)
			{
				time = System.currentTimeMillis() + data.getTime();
				break;
			}
			
			final Calendar calendar = Calendar.getInstance();
			calendar.set(Calendar.AM_PM, data.getHour() >= 12 ? 1 : 0);
			calendar.set(Calendar.HOUR, data.getHour());
			calendar.set(Calendar.MINUTE, data.getMinute());
			calendar.set(Calendar.SECOND, 0);
			
			if (calendar.getTimeInMillis() <= System.currentTimeMillis())
			{
				calendar.add(Calendar.DAY_OF_MONTH, 1);
			}
			
			if (data.getDay() != null)
			{
				while (calendar.get(Calendar.DAY_OF_WEEK) != (data.getDay().getValue() + 1))
				{
					calendar.add(Calendar.DAY_OF_MONTH, 1);
				}
			}
			
			if (time == -1)
			{
				time = calendar.getTimeInMillis();
			}
			else if (calendar.getTimeInMillis() < time)
			{
				time = calendar.getTimeInMillis();
			}
		}
		
		if (time > 0)
		{
			setReenterTime(world, time);
		}
	}
	
	protected void handleRemoveBuffs(InstanceWorld world)
	{
		for (Integer objId : world.getAllowed())
		{
			final PlayerInstance player = World.getInstance().getPlayer(objId);
			
			if (player != null)
			{
				handleRemoveBuffs(player, world);
			}
		}
	}
	
	protected abstract void onEnterInstance(PlayerInstance player, InstanceWorld world, boolean firstEntrance);
	
	protected boolean checkConditions(PlayerInstance player)
	{
		return true;
	}
	
	/**
	 * Spawns group of instance NPC's
	 * @param groupName - name of group from XML definition to spawn
	 * @param instanceId - ID of instance
	 * @return list of spawned NPC's
	 */
	protected List<Npc> spawnGroup(String groupName, int instanceId)
	{
		return InstanceManager.getInstance().getInstance(instanceId).spawnGroup(groupName);
	}
	
	/**
	 * Save Reenter time for every player in InstanceWorld.
	 * @param world - the InstanceWorld
	 * @param time - Time in miliseconds
	 */
	protected void setReenterTime(InstanceWorld world, long time)
	{
		for (int objectId : world.getAllowed())
		{
			InstanceManager.getInstance().setInstanceTime(objectId, world.getTemplateId(), time);
			final PlayerInstance player = World.getInstance().getPlayer(objectId);
			if ((player != null) && player.isOnline())
			{
				player.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.INSTANT_ZONE_S1_S_ENTRY_HAS_BEEN_RESTRICTED_YOU_CAN_CHECK_THE_NEXT_POSSIBLE_ENTRY_TIME_BY_USING_THE_COMMAND_INSTANCEZONE).addString(InstanceManager.getInstance().getInstance(world.getInstanceId()).getName()));
			}
		}
		
		if (Config.DEBUG_INSTANCES)
		{
			_log.info("Time restrictions has been set for player in instance ID: " + world.getInstanceId() + " (" + new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(time) + ")");
		}
	}
	
	private void handleRemoveBuffs(PlayerInstance player, InstanceWorld world)
	{
		final Instance inst = InstanceManager.getInstance().getInstance(world.getInstanceId());
		final List<BuffInfo> buffToRemove = new ArrayList<>();
		
		switch (inst.getRemoveBuffType())
		{
			case ALL:
			{
				player.stopAllEffectsExceptThoseThatLastThroughDeath();
				
				final Summon pet = player.getPet();
				if (pet != null)
				{
					pet.stopAllEffectsExceptThoseThatLastThroughDeath();
				}
				
				player.getServitors().values().forEach(Summon::stopAllEffectsExceptThoseThatLastThroughDeath);
				break;
			}
			case WHITELIST:
			{
				for (BuffInfo info : player.getEffectList().getBuffs().values())
				{
					if (!inst.getBuffExceptionList().contains(info.getSkill().getId()))
					{
						buffToRemove.add(info);
					}
				}
				
				for (Summon summon : player.getServitors().values())
				{
					for (BuffInfo info : summon.getEffectList().getBuffs().values())
					{
						if (!inst.getBuffExceptionList().contains(info.getSkill().getId()))
						{
							buffToRemove.add(info);
						}
					}
				}
				
				final Summon pet = player.getPet();
				if (pet != null)
				{
					for (BuffInfo info : pet.getEffectList().getBuffs().values())
					{
						if (!inst.getBuffExceptionList().contains(info.getSkill().getId()))
						{
							buffToRemove.add(info);
						}
					}
				}
				break;
			}
			case BLACKLIST:
			{
				for (BuffInfo info : player.getEffectList().getBuffs().values())
				{
					if (inst.getBuffExceptionList().contains(info.getSkill().getId()))
					{
						buffToRemove.add(info);
					}
				}
				
				for (Summon summon : player.getServitors().values())
				{
					for (BuffInfo info : summon.getEffectList().getBuffs().values())
					{
						if (inst.getBuffExceptionList().contains(info.getSkill().getId()))
						{
							buffToRemove.add(info);
						}
					}
				}
				
				final Summon pet = player.getPet();
				if (pet != null)
				{
					for (BuffInfo info : pet.getEffectList().getBuffs().values())
					{
						if (inst.getBuffExceptionList().contains(info.getSkill().getId()))
						{
							buffToRemove.add(info);
						}
					}
				}
				break;
			}
		}
		
		for (BuffInfo info : buffToRemove)
		{
			info.getEffected().getEffectList().stopSkillEffects(true, info.getSkill());
		}
	}
}