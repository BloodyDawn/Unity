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

import java.util.List;

import org.l2junity.gameserver.enums.InstanceReenterType;
import org.l2junity.gameserver.instancemanager.InstanceManager;
import org.l2junity.gameserver.model.Location;
import org.l2junity.gameserver.model.PcCondOverride;
import org.l2junity.gameserver.model.actor.Creature;
import org.l2junity.gameserver.model.actor.Npc;
import org.l2junity.gameserver.model.actor.instance.PlayerInstance;
import org.l2junity.gameserver.model.instancezone.Instance;
import org.l2junity.gameserver.model.instancezone.InstanceTemplate;
import org.l2junity.gameserver.network.client.send.ExShowScreenMessage;
import org.l2junity.gameserver.network.client.send.string.NpcStringId;
import org.l2junity.gameserver.network.client.send.string.SystemMessageId;

import ai.npc.AbstractNpcAI;

/**
 * Abstract class for Instances.
 * @author FallenAngel
 */
public abstract class AbstractInstance extends AbstractNpcAI
{
	public AbstractInstance(String name, String desc)
	{
		super(name, desc);
	}
	
	public AbstractInstance(String name)
	{
		super(name, "instances");
	}
	
	/**
	 * Get instance world by instance ID.<br>
	 * Equivalent of {@link InstanceManager#getInstance(int)}
	 * @param instanceId
	 * @return instance itself if found, otherwise null
	 */
	public Instance getInstance(int instanceId)
	{
		return InstanceManager.getInstance().getInstance(instanceId);
	}
	
	/**
	 * Get instance world for {@link Creature}.<br>
	 * Equivalent of {@link InstanceManager#getInstance(Creature)}
	 * @param creature creature inside instance
	 * @return instance world if found, otherwise null
	 */
	public Instance getInstance(Creature creature)
	{
		return InstanceManager.getInstance().getInstance(creature);
	}
	
	/**
	 * Get instance world for {@link PlayerInstance}.<br>
	 * Equivalent of {@link InstanceManager#getPlayerInstance(PlayerInstance, boolean)}
	 * @param player player who wants get instance world
	 * @param isInside when {@code true} find world where player is currently located, otherwise find world where player can enter
	 * @return instance world if found, otherwise null
	 */
	public Instance getPlayerInstance(PlayerInstance player, boolean isInside)
	{
		return InstanceManager.getInstance().getPlayerInstance(player, isInside);
	}
	
	/**
	 * Show an on screen message to each player inside instance.
	 * @param instance instance where message should be broadcasted
	 * @param npcStringId the NPC string to display
	 * @param position the position of the message on the screen
	 * @param time the duration of the message in milliseconds
	 * @param params values of parameters to replace in the NPC String (like S1, C1 etc.)
	 */
	public void showOnScreenMsg(Instance instance, NpcStringId npcStringId, int position, int time, String... params)
	{
		instance.broadcastPacket(new ExShowScreenMessage(npcStringId, position, time, params));
	}
	
	/**
	 * Put player into instance world.<br>
	 * If instance world doesn't found for player then try to create new one.
	 * @param player player who wants to enter into instance
	 * @param npc NPC which allows to enter into instance
	 * @param templateId template ID of instance where player wants to enter
	 */
	protected final void enterInstance(PlayerInstance player, Npc npc, int templateId)
	{
		Instance instance = getPlayerInstance(player, false);
		if (instance != null) // Player has already any instance active
		{
			if (instance.getTemplateId() != templateId)
			{
				player.sendPacket(SystemMessageId.YOU_HAVE_ENTERED_ANOTHER_INSTANT_ZONE_THEREFORE_YOU_CANNOT_ENTER_CORRESPONDING_DUNGEON);
				return;
			}
			onEnter(player, instance, false);
		}
		else
		{
			// Get instance template
			final InstanceTemplate template = InstanceManager.getInstance().getInstanceTemplate(templateId);
			if (template == null)
			{
				_log.warn("Player {} ({}) wants to create instance with unknown template id {}!", player.getName(), player.getObjectId(), templateId);
				return;
			}
			
			// Get instance enter scope
			final List<PlayerInstance> enterGroup = template.getEnterGroup(player);
			// When nobody can enter
			if (enterGroup == null)
			{
				_log.warn("Instance {} ({}) has invalid group size limits!", template.getName(), templateId);
				return;
			}
			
			// Validate conditions for group
			if (!player.canOverrideCond(PcCondOverride.INSTANCE_CONDITIONS) && !template.validateConditions(enterGroup, npc, this::showHtmlFile))
			{
				return;
			}
			
			// Check if any player from enter group has active instance
			for (PlayerInstance member : enterGroup)
			{
				if (getPlayerInstance(member, false) != null)
				{
					enterGroup.forEach(p -> p.sendPacket(SystemMessageId.YOU_HAVE_ENTERED_ANOTHER_INSTANT_ZONE_THEREFORE_YOU_CANNOT_ENTER_CORRESPONDING_DUNGEON));
					return;
				}
			}
			
			// Check if maximum world count limit is exceeded
			if (InstanceManager.getInstance().getWorldCount(templateId) >= template.getMaxWorlds())
			{
				player.sendPacket(SystemMessageId.THE_NUMBER_OF_INSTANT_ZONES_THAT_CAN_BE_CREATED_HAS_BEEN_EXCEEDED_PLEASE_TRY_AGAIN_LATER);
				return;
			}
			
			// Create new instance for enter player group
			instance = InstanceManager.getInstance().createInstance(template);
			
			// Move each player from enter group to instance
			for (PlayerInstance member : enterGroup)
			{
				instance.addAllowed(member);
				onEnter(member, instance, true);
			}
			
			// Apply condition success effects
			template.applyConditionEffects(enterGroup);
			
			// Set re-enter for instances with re-enter on start
			if (instance.getReenterType().equals(InstanceReenterType.ON_ENTER))
			{
				instance.setReenterTime();
			}
		}
	}
	
	/**
	 * This function is called when player enter into instance trough NPC.
	 * @param player player who enter
	 * @param instance instance world where player enter
	 * @param firstEnter when {@code true} player enter first time, otherwise player entered multiple times
	 */
	protected void onEnter(PlayerInstance player, Instance instance, boolean firstEnter)
	{
		teleportPlayerIn(player, instance);
	}
	
	/**
	 * This method is used to teleport player into instance by start NPC.<br>
	 * When you override whole method, XML teleport data won't be applied.
	 * @param player player which should be teleported
	 * @param instance instance where player should be teleported
	 */
	protected void teleportPlayerIn(PlayerInstance player, Instance instance)
	{
		final Location loc = instance.getEnterLocation();
		if (loc != null)
		{
			player.teleToLocation(loc, false);
		}
		else
		{
			_log.warn("Missing start location for instance {} ({})", instance.getName(), instance.getId());
		}
	}
	
	/**
	 * This method is used to teleport player from instance world by NPC.
	 * @param player player which should be ejected
	 * @param instance instance from player should be removed
	 */
	protected void teleportPlayerOut(PlayerInstance player, Instance instance)
	{
		instance.ejectPlayer(player);
	}
	
	protected void finishInstance(PlayerInstance player)
	{
		final Instance inst = getPlayerInstance(player, true);
		if (inst != null)
		{
			inst.finishInstance();
		}
	}
	
	protected void finishInstance(PlayerInstance player, int delay)
	{
		final Instance inst = getPlayerInstance(player, true);
		if (inst != null)
		{
			inst.finishInstance(delay);
		}
	}
}