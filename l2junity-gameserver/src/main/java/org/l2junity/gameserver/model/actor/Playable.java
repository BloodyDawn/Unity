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
package org.l2junity.gameserver.model.actor;

import org.l2junity.gameserver.ai.CtrlEvent;
import org.l2junity.gameserver.enums.InstanceType;
import org.l2junity.gameserver.instancemanager.InstanceManager;
import org.l2junity.gameserver.model.actor.instance.L2PcInstance;
import org.l2junity.gameserver.model.actor.knownlist.PlayableKnownList;
import org.l2junity.gameserver.model.actor.stat.PlayableStat;
import org.l2junity.gameserver.model.actor.status.PlayableStatus;
import org.l2junity.gameserver.model.actor.templates.L2CharTemplate;
import org.l2junity.gameserver.model.effects.EffectFlag;
import org.l2junity.gameserver.model.effects.L2EffectType;
import org.l2junity.gameserver.model.entity.Instance;
import org.l2junity.gameserver.model.events.EventDispatcher;
import org.l2junity.gameserver.model.events.impl.character.OnCreatureKill;
import org.l2junity.gameserver.model.events.returns.TerminateReturn;
import org.l2junity.gameserver.model.quest.QuestState;
import org.l2junity.gameserver.model.skills.Skill;
import org.l2junity.gameserver.network.serverpackets.EtcStatusUpdate;

/**
 * This class represents all Playable characters in the world.<br>
 * L2Playable:
 * <ul>
 * <li>L2PcInstance</li>
 * <li>L2Summon</li>
 * </ul>
 */
public abstract class Playable extends Creature
{
	private Creature _lockedTarget = null;
	private L2PcInstance transferDmgTo = null;
	
	/**
	 * Constructor of L2Playable.<br>
	 * <B><U> Actions</U> :</B>
	 * <ul>
	 * <li>Call the L2Character constructor to create an empty _skills slot and link copy basic Calculator set to this L2Playable</li>
	 * </ul>
	 * @param objectId the object id
	 * @param template The L2CharTemplate to apply to the L2Playable
	 */
	public Playable(int objectId, L2CharTemplate template)
	{
		super(objectId, template);
		setInstanceType(InstanceType.L2Playable);
		setIsInvul(false);
	}
	
	public Playable(L2CharTemplate template)
	{
		super(template);
		setInstanceType(InstanceType.L2Playable);
		setIsInvul(false);
	}
	
	@Override
	public PlayableKnownList getKnownList()
	{
		return (PlayableKnownList) super.getKnownList();
	}
	
	@Override
	public void initKnownList()
	{
		setKnownList(new PlayableKnownList(this));
	}
	
	@Override
	public PlayableStat getStat()
	{
		return (PlayableStat) super.getStat();
	}
	
	@Override
	public void initCharStat()
	{
		setStat(new PlayableStat(this));
	}
	
	@Override
	public PlayableStatus getStatus()
	{
		return (PlayableStatus) super.getStatus();
	}
	
	@Override
	public void initCharStatus()
	{
		setStatus(new PlayableStatus(this));
	}
	
	@Override
	public boolean doDie(Creature killer)
	{
		final TerminateReturn returnBack = EventDispatcher.getInstance().notifyEvent(new OnCreatureKill(killer, this), this, TerminateReturn.class);
		if ((returnBack != null) && returnBack.terminate())
		{
			return false;
		}
		
		// killing is only possible one time
		synchronized (this)
		{
			if (isDead())
			{
				return false;
			}
			// now reset currentHp to zero
			setCurrentHp(0);
			setIsDead(true);
		}
		
		// Set target to null and cancel Attack or Cast
		setTarget(null);
		
		// Stop movement
		stopMove(null);
		
		// Stop HP/MP/CP Regeneration task
		getStatus().stopHpMpRegeneration();
		
		boolean deleteBuffs = true;
		
		if (isNoblesseBlessedAffected())
		{
			stopEffects(L2EffectType.NOBLESSE_BLESSING);
			deleteBuffs = false;
		}
		if (isResurrectSpecialAffected())
		{
			stopEffects(L2EffectType.RESURRECTION_SPECIAL);
			deleteBuffs = false;
		}
		if (isPlayer())
		{
			L2PcInstance activeChar = getActingPlayer();
			
			if (activeChar.hasCharmOfCourage())
			{
				if (activeChar.isInSiege())
				{
					getActingPlayer().reviveRequest(getActingPlayer(), null, false, 0);
				}
				activeChar.setCharmOfCourage(false);
				activeChar.sendPacket(new EtcStatusUpdate(activeChar));
			}
		}
		
		if (deleteBuffs)
		{
			stopAllEffectsExceptThoseThatLastThroughDeath();
		}
		
		// Send the Server->Client packet StatusUpdate with current HP and MP to all other L2PcInstance to inform
		broadcastStatusUpdate();
		
		if (getWorldRegion() != null)
		{
			getWorldRegion().onDeath(this);
		}
		
		// Notify Quest of L2Playable's death
		L2PcInstance actingPlayer = getActingPlayer();
		
		if (!actingPlayer.isNotifyQuestOfDeathEmpty())
		{
			for (QuestState qs : actingPlayer.getNotifyQuestOfDeath())
			{
				qs.getQuest().notifyDeath((killer == null ? this : killer), this, qs);
			}
		}
		// Notify instance
		if (getInstanceId() > 0)
		{
			final Instance instance = InstanceManager.getInstance().getInstance(getInstanceId());
			if (instance != null)
			{
				instance.notifyDeath(killer, this);
			}
		}
		
		if (killer != null)
		{
			L2PcInstance player = killer.getActingPlayer();
			
			if (player != null)
			{
				player.onKillUpdatePvPKarma(this);
			}
		}
		
		// Notify L2Character AI
		getAI().notifyEvent(CtrlEvent.EVT_DEAD);
		super.updateEffectIcons();
		return true;
	}
	
	public boolean checkIfPvP(Creature target)
	{
		if (target == null)
		{
			return false; // Target is null
		}
		if (target == this)
		{
			return false; // Target is self
		}
		if (!target.isPlayable())
		{
			return false; // Target is not a L2Playable
		}
		
		final L2PcInstance player = getActingPlayer();
		if (player == null)
		{
			return false; // Active player is null
		}
		
		if (player.getKarma() != 0)
		{
			return false; // Active player has karma
		}
		
		final L2PcInstance targetPlayer = target.getActingPlayer();
		if (targetPlayer == null)
		{
			return false; // Target player is null
		}
		
		if (targetPlayer == this)
		{
			return false; // Target player is self
		}
		if (targetPlayer.getKarma() != 0)
		{
			return false; // Target player has karma
		}
		if (targetPlayer.getPvpFlag() == 0)
		{
			return false;
		}
		
		return true;
		// Even at war, there should be PvP flag
		// if(
		// player.getClan() == null ||
		// targetPlayer.getClan() == null ||
		// (
		// !targetPlayer.getClan().isAtWarWith(player.getClanId()) &&
		// targetPlayer.getWantsPeace() == 0 &&
		// player.getWantsPeace() == 0
		// )
		// )
		// {
		// return true;
		// }
		// return false;
	}
	
	/**
	 * Return True.
	 */
	@Override
	public boolean canBeAttacked()
	{
		return true;
	}
	
	// Support for Noblesse Blessing skill, where buffs are retained after resurrect
	public final boolean isNoblesseBlessedAffected()
	{
		return isAffected(EffectFlag.NOBLESS_BLESSING);
	}
	
	/**
	 * @return {@code true} if char can resurrect by himself, {@code false} otherwise
	 */
	public final boolean isResurrectSpecialAffected()
	{
		return isAffected(EffectFlag.RESURRECTION_SPECIAL);
	}
	
	/**
	 * @return {@code true} if the Silent Moving mode is active, {@code false} otherwise
	 */
	public boolean isSilentMovingAffected()
	{
		return isAffected(EffectFlag.SILENT_MOVE);
	}
	
	/**
	 * For Newbie Protection Blessing skill, keeps you safe from an attack by a chaotic character >= 10 levels apart from you.
	 * @return
	 */
	public final boolean isProtectionBlessingAffected()
	{
		return isAffected(EffectFlag.PROTECTION_BLESSING);
	}
	
	@Override
	public void updateEffectIcons(boolean partyOnly)
	{
		getEffectList().updateEffectIcons(partyOnly);
	}
	
	public boolean isLockedTarget()
	{
		return _lockedTarget != null;
	}
	
	public Creature getLockedTarget()
	{
		return _lockedTarget;
	}
	
	public void setLockedTarget(Creature cha)
	{
		_lockedTarget = cha;
	}
	
	public void setTransferDamageTo(L2PcInstance val)
	{
		transferDmgTo = val;
	}
	
	public L2PcInstance getTransferingDamageTo()
	{
		return transferDmgTo;
	}
	
	public abstract int getKarma();
	
	public abstract byte getPvpFlag();
	
	public abstract boolean useMagic(Skill skill, boolean forceUse, boolean dontMove);
	
	public abstract void storeMe();
	
	public abstract void storeEffect(boolean storeEffects);
	
	public abstract void restoreEffects();
	
	@Override
	public boolean isPlayable()
	{
		return true;
	}
}
