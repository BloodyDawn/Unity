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
package org.l2junity.gameserver.model.actor.transform;

import java.util.ArrayList;
import java.util.List;

import org.l2junity.gameserver.data.xml.impl.SkillTreesData;
import org.l2junity.gameserver.model.SkillLearn;
import org.l2junity.gameserver.model.StatsSet;
import org.l2junity.gameserver.model.actor.instance.PlayerInstance;
import org.l2junity.gameserver.model.holders.AdditionalItemHolder;
import org.l2junity.gameserver.model.holders.AdditionalSkillHolder;
import org.l2junity.gameserver.model.holders.SkillHolder;
import org.l2junity.gameserver.model.interfaces.IIdentifiable;
import org.l2junity.gameserver.model.skills.Skill;
import org.l2junity.gameserver.model.stats.Stats;
import org.l2junity.gameserver.network.client.send.ExBasicActionList;

/**
 * @author UnAfraid
 */
public final class Transform implements IIdentifiable
{
	private final int _id;
	private final int _displayId;
	private final TransformType _type;
	private final boolean _canSwim;
	private final int _spawnHeight;
	private final boolean _canAttack;
	private final String _name;
	private final String _title;
	private final boolean _allowAllSkills;
	
	private TransformTemplate _maleTemplate;
	private TransformTemplate _femaleTemplate;
	
	public Transform(StatsSet set)
	{
		_id = set.getInt("id");
		_displayId = set.getInt("displayId", _id);
		_type = set.getEnum("type", TransformType.class, TransformType.COMBAT);
		_canSwim = set.getInt("can_swim", 0) == 1;
		_canAttack = set.getInt("normal_attackable", 1) == 1;
		_spawnHeight = set.getInt("spawn_height", 0);
		_name = set.getString("setName", null);
		_title = set.getString("setTitle", null);
		_allowAllSkills = set.getInt("allow_all_skills", 0) == 1;
	}
	
	/**
	 * Gets the transformation ID.
	 * @return the transformation ID
	 */
	@Override
	public int getId()
	{
		return _id;
	}
	
	public int getDisplayId()
	{
		return _displayId;
	}
	
	public TransformType getType()
	{
		return _type;
	}
	
	public boolean canSwim()
	{
		return _canSwim;
	}
	
	public boolean canAttack()
	{
		return _canAttack;
	}
	
	public int getSpawnHeight()
	{
		return _spawnHeight;
	}
	
	/**
	 * @return name that's going to be set to the player while is transformed with current transformation
	 */
	public String getName()
	{
		return _name;
	}
	
	/**
	 * @return title that's going to be set to the player while is transformed with current transformation
	 */
	public String getTitle()
	{
		return _title;
	}
	
	public TransformTemplate getTemplate(PlayerInstance player)
	{
		return player != null ? (player.getAppearance().getSex() ? _femaleTemplate : _maleTemplate) : null;
	}
	
	public void setTemplate(boolean male, TransformTemplate template)
	{
		if (male)
		{
			_maleTemplate = template;
		}
		else
		{
			_femaleTemplate = template;
		}
	}
	
	/**
	 * @return Allow all skills for this transformation.
	 */
	public boolean allowAllSkills()
	{
		return _allowAllSkills;
	}
	
	/**
	 * @return {@code true} if transform type is mode change, {@code false} otherwise
	 */
	public boolean isStance()
	{
		return _type == TransformType.MODE_CHANGE;
	}
	
	/**
	 * @return {@code true} if transform type is combat, {@code false} otherwise
	 */
	public boolean isCombat()
	{
		return _type == TransformType.COMBAT;
	}
	
	/**
	 * @return {@code true} if transform type is non combat, {@code false} otherwise
	 */
	public boolean isNonCombat()
	{
		return _type == TransformType.NON_COMBAT;
	}
	
	/**
	 * @return {@code true} if transform type is flying, {@code false} otherwise
	 */
	public boolean isFlying()
	{
		return _type == TransformType.FLYING;
	}
	
	/**
	 * @return {@code true} if transform type is cursed, {@code false} otherwise
	 */
	public boolean isCursed()
	{
		return _type == TransformType.CURSED;
	}
	
	/**
	 * @return {@code true} if transform type is raiding, {@code false} otherwise
	 */
	public boolean isRiding()
	{
		return _type == TransformType.RIDING_MODE;
	}
	
	/**
	 * @return {@code true} if transform type is pure stat, {@code false} otherwise
	 */
	public boolean isPureStats()
	{
		return _type == TransformType.PURE_STAT;
	}
	
	public double getCollisionHeight(PlayerInstance player)
	{
		final TransformTemplate template = getTemplate(player);
		return template != null ? template.getCollisionHeight() : player.getCollisionHeight();
	}
	
	public double getCollisionRadius(PlayerInstance player)
	{
		final TransformTemplate template = getTemplate(player);
		return template != null ? template.getCollisionRadius() : player.getCollisionRadius();
	}
	
	public int getBaseAttackRange(PlayerInstance player)
	{
		final TransformTemplate template = getTemplate(player);
		return template != null ? template.getBaseAttackRange() : player.getTemplate().getBaseAttackRange();
	}
	
	public void onTransform(PlayerInstance player)
	{
		final TransformTemplate template = getTemplate(player);
		if (template != null)
		{
			// Start flying.
			if (isFlying())
			{
				player.setIsFlying(true);
			}
			
			// Get player a bit higher so he doesn't drops underground after transformation happens
			player.setZ(player.getZ() + (int) template.getCollisionHeight());
			
			if (getName() != null)
			{
				player.getAppearance().setVisibleName(getName());
			}
			if (getTitle() != null)
			{
				player.getAppearance().setVisibleTitle(getTitle());
			}
			
			// Add common skills.
			for (SkillHolder holder : template.getSkills())
			{
				if (player.getSkillLevel(holder.getSkillId()) < holder.getSkillLvl())
				{
					player.addSkill(holder.getSkill(), false);
				}
				player.addTransformSkill(holder.getSkillId());
			}
			
			// Add skills depending on level.
			for (AdditionalSkillHolder holder : template.getAdditionalSkills())
			{
				if (player.getLevel() >= holder.getMinLevel())
				{
					if (player.getSkillLevel(holder.getSkillId()) < holder.getSkillLvl())
					{
						player.addSkill(holder.getSkill(), false);
					}
					player.addTransformSkill(holder.getSkillId());
				}
			}
			
			// Add collection skills.
			for (SkillLearn skill : SkillTreesData.getInstance().getCollectSkillTree().values())
			{
				if (player.getKnownSkill(skill.getSkillId()) != null)
				{
					player.addTransformSkill(skill.getSkillId());
				}
			}
			
			// Set inventory blocks if needed.
			if (!template.getAdditionalItems().isEmpty())
			{
				final List<Integer> allowed = new ArrayList<>();
				final List<Integer> notAllowed = new ArrayList<>();
				for (AdditionalItemHolder holder : template.getAdditionalItems())
				{
					if (holder.isAllowedToUse())
					{
						allowed.add(holder.getId());
					}
					else
					{
						notAllowed.add(holder.getId());
					}
				}
				
				if (!allowed.isEmpty())
				{
					final int[] items = new int[allowed.size()];
					for (int i = 0; i < items.length; i++)
					{
						items[i] = allowed.get(i);
					}
					player.getInventory().setInventoryBlock(items, 1);
				}
				
				if (!notAllowed.isEmpty())
				{
					final int[] items = new int[notAllowed.size()];
					for (int i = 0; i < items.length; i++)
					{
						items[i] = notAllowed.get(i);
					}
					player.getInventory().setInventoryBlock(items, 2);
				}
			}
			
			// Send basic action list.
			if (template.hasBasicActionList())
			{
				player.sendPacket(template.getBasicActionList());
			}
		}
	}
	
	public void onUntransform(PlayerInstance player)
	{
		final TransformTemplate template = getTemplate(player);
		if (template != null)
		{
			// Stop flying.
			if (isFlying())
			{
				player.setIsFlying(false);
			}
			
			if (getName() != null)
			{
				player.getAppearance().setVisibleName(null);
			}
			if (getTitle() != null)
			{
				player.getAppearance().setVisibleTitle(null);
			}
			
			// Remove common skills.
			if (!template.getSkills().isEmpty())
			{
				for (SkillHolder holder : template.getSkills())
				{
					final Skill skill = holder.getSkill();
					if (!SkillTreesData.getInstance().isSkillAllowed(player, skill))
					{
						player.removeSkill(skill, false, true);
					}
				}
			}
			
			// Remove skills depending on level.
			if (!template.getAdditionalSkills().isEmpty())
			{
				for (AdditionalSkillHolder holder : template.getAdditionalSkills())
				{
					final Skill skill = holder.getSkill();
					if ((player.getLevel() >= holder.getMinLevel()) && !SkillTreesData.getInstance().isSkillAllowed(player, skill))
					{
						player.removeSkill(skill, false, true);
					}
				}
			}
			
			// Remove transformation skills.
			player.removeAllTransformSkills();
			
			// Remove inventory blocks if needed.
			if (!template.getAdditionalItems().isEmpty())
			{
				player.getInventory().unblock();
			}
			
			player.sendPacket(ExBasicActionList.STATIC_PACKET);
		}
	}
	
	public void onLevelUp(PlayerInstance player)
	{
		final TransformTemplate template = getTemplate(player);
		if (template != null)
		{
			// Add skills depending on level.
			if (!template.getAdditionalSkills().isEmpty())
			{
				for (AdditionalSkillHolder holder : template.getAdditionalSkills())
				{
					if (player.getLevel() >= holder.getMinLevel())
					{
						if (player.getSkillLevel(holder.getSkillId()) < holder.getSkillLvl())
						{
							player.addSkill(holder.getSkill(), false);
							player.addTransformSkill(holder.getSkillId());
						}
					}
				}
			}
		}
	}
	
	public double getStats(PlayerInstance player, Stats stats, double defaultValue)
	{
		double val = defaultValue;
		final TransformTemplate template = getTemplate(player);
		if (template != null)
		{
			val = template.getStats(stats);
			final TransformLevelData data = template.getData(player.getLevel());
			if (data != null)
			{
				val = data.getStats(stats, defaultValue);
			}
		}
		return val;
	}
	
	/**
	 * @param player
	 * @param slot
	 * @return
	 */
	public int getBaseDefBySlot(PlayerInstance player, int slot)
	{
		final TransformTemplate template = getTemplate(player);
		if (template != null)
		{
			return template.getDefense(slot);
		}
		return player.getTemplate().getBaseDefBySlot(slot);
	}
	
	/**
	 * @param player
	 * @return
	 */
	public double getLevelMod(PlayerInstance player)
	{
		double val = -1;
		final TransformTemplate template = getTemplate(player);
		if (template != null)
		{
			final TransformLevelData data = template.getData(player.getLevel());
			if (data != null)
			{
				val = data.getLevelMod();
			}
		}
		return val;
	}
}
