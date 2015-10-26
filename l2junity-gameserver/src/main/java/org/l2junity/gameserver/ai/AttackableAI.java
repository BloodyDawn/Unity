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
package org.l2junity.gameserver.ai;

import static org.l2junity.gameserver.ai.CtrlIntention.AI_INTENTION_ACTIVE;
import static org.l2junity.gameserver.ai.CtrlIntention.AI_INTENTION_ATTACK;
import static org.l2junity.gameserver.ai.CtrlIntention.AI_INTENTION_IDLE;

import java.lang.ref.WeakReference;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Future;

import org.l2junity.Config;
import org.l2junity.commons.util.Rnd;
import org.l2junity.gameserver.GameTimeController;
import org.l2junity.gameserver.GeoData;
import org.l2junity.gameserver.ThreadPoolManager;
import org.l2junity.gameserver.enums.AISkillScope;
import org.l2junity.gameserver.enums.AIType;
import org.l2junity.gameserver.model.Location;
import org.l2junity.gameserver.model.World;
import org.l2junity.gameserver.model.WorldObject;
import org.l2junity.gameserver.model.actor.Attackable;
import org.l2junity.gameserver.model.actor.Creature;
import org.l2junity.gameserver.model.actor.Npc;
import org.l2junity.gameserver.model.actor.Playable;
import org.l2junity.gameserver.model.actor.Summon;
import org.l2junity.gameserver.model.actor.instance.L2FriendlyMobInstance;
import org.l2junity.gameserver.model.actor.instance.L2GrandBossInstance;
import org.l2junity.gameserver.model.actor.instance.L2GuardInstance;
import org.l2junity.gameserver.model.actor.instance.L2MonsterInstance;
import org.l2junity.gameserver.model.actor.instance.L2RaidBossInstance;
import org.l2junity.gameserver.model.actor.instance.L2StaticObjectInstance;
import org.l2junity.gameserver.model.actor.instance.PlayerInstance;
import org.l2junity.gameserver.model.effects.L2EffectType;
import org.l2junity.gameserver.model.events.EventDispatcher;
import org.l2junity.gameserver.model.events.impl.character.npc.OnAttackableFactionCall;
import org.l2junity.gameserver.model.events.impl.character.npc.OnAttackableHate;
import org.l2junity.gameserver.model.events.returns.TerminateReturn;
import org.l2junity.gameserver.model.skills.Skill;
import org.l2junity.gameserver.model.skills.SkillCaster;
import org.l2junity.gameserver.model.skills.targets.L2TargetType;
import org.l2junity.gameserver.model.zone.ZoneId;
import org.l2junity.gameserver.util.Util;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class manages AI of L2Attackable.
 */
public class AttackableAI extends CharacterAI implements Runnable
{
	private static final Logger LOGGER = LoggerFactory.getLogger(AttackableAI.class);
	
	private static final int RANDOM_WALK_RATE = 30; // confirmed
	// private static final int MAX_DRIFT_RANGE = 300;
	private static final int MAX_ATTACK_TIMEOUT = 1200; // int ticks, i.e. 2min
	/**
	 * The L2Attackable AI task executed every 1s (call onEvtThink method).
	 */
	private Future<?> _aiTask;
	/**
	 * The delay after which the attacked is stopped.
	 */
	private int _attackTimeout;
	/**
	 * The L2Attackable aggro counter.
	 */
	private int _globalAggro;
	/**
	 * The flag used to indicate that a thinking action is in progress, to prevent recursive thinking.
	 */
	private boolean _thinking;
	
	private int timepass = 0;
	private int chaostime = 0;
	int lastBuffTick;
	
	public AttackableAI(Attackable attackable)
	{
		super(attackable);
		_attackTimeout = Integer.MAX_VALUE;
		_globalAggro = -10; // 10 seconds timeout of ATTACK after respawn
	}
	
	@Override
	public void run()
	{
		// Launch actions corresponding to the Event Think
		onEvtThink();
	}
	
	/**
	 * <B><U> Actor is a L2GuardInstance</U> :</B>
	 * <ul>
	 * <li>The target isn't a Folk or a Door</li>
	 * <li>The target isn't dead, isn't invulnerable, isn't in silent moving mode AND too far (>100)</li>
	 * <li>The target is in the actor Aggro range and is at the same height</li>
	 * <li>The L2PcInstance target has karma (=PK)</li>
	 * <li>The L2MonsterInstance target is aggressive</li>
	 * </ul>
	 * <B><U> Actor is a L2SiegeGuardInstance</U> :</B>
	 * <ul>
	 * <li>The target isn't a Folk or a Door</li>
	 * <li>The target isn't dead, isn't invulnerable, isn't in silent moving mode AND too far (>100)</li>
	 * <li>The target is in the actor Aggro range and is at the same height</li>
	 * <li>A siege is in progress</li>
	 * <li>The L2PcInstance target isn't a Defender</li>
	 * </ul>
	 * <B><U> Actor is a L2FriendlyMobInstance</U> :</B>
	 * <ul>
	 * <li>The target isn't a Folk, a Door or another L2Npc</li>
	 * <li>The target isn't dead, isn't invulnerable, isn't in silent moving mode AND too far (>100)</li>
	 * <li>The target is in the actor Aggro range and is at the same height</li>
	 * <li>The L2PcInstance target has karma (=PK)</li>
	 * </ul>
	 * <B><U> Actor is a L2MonsterInstance</U> :</B>
	 * <ul>
	 * <li>The target isn't a Folk, a Door or another L2Npc</li>
	 * <li>The target isn't dead, isn't invulnerable, isn't in silent moving mode AND too far (>100)</li>
	 * <li>The target is in the actor Aggro range and is at the same height</li>
	 * <li>The actor is Aggressive</li>
	 * </ul>
	 * @param target The targeted L2Object
	 * @return True if the target is autoattackable (depends on the actor type).
	 */
	private boolean autoAttackCondition(Creature target)
	{
		if ((target == null) || !target.isTargetable() || (getActiveChar() == null))
		{
			return false;
		}
		
		// Check if the target isn't invulnerable
		if (target.isInvul())
		{
			// However EffectInvincible requires to check GMs specially
			if (target.isPlayer() && target.isGM())
			{
				return false;
			}
			if (target.isSummon() && ((Summon) target).getOwner().isGM())
			{
				return false;
			}
		}
		
		// Check if the target isn't a Folk or a Door
		if (target.isDoor())
		{
			return false;
		}
		
		final Attackable me = getActiveChar();
		
		// Check if the target isn't dead, is in the Aggro range and is at the same height
		if (target.isAlikeDead())
		{
			return false;
		}
		
		// Check if the target is a L2Playable
		if (target.isPlayable())
		{
			// Check if the AI isn't a Raid Boss, can See Silent Moving players and the target isn't in silent move mode
			if (!(me.isRaid()) && !(me.canSeeThroughSilentMove()) && ((Playable) target).isSilentMovingAffected())
			{
				return false;
			}
		}
		
		// Gets the player if there is any.
		final PlayerInstance player = target.getActingPlayer();
		if (player != null)
		{
			// Don't take the aggro if the GM has the access level below or equal to GM_DONT_TAKE_AGGRO
			if (player.isGM() && !player.getAccessLevel().canTakeAggro())
			{
				return false;
			}
			
			// check if the target is within the grace period for JUST getting up from fake death
			if (player.isRecentFakeDeath())
			{
				return false;
			}
		}
		
		// Check if the actor is a L2GuardInstance
		if (me instanceof L2GuardInstance)
		{
			// Check if the PlayerInstance target has negative Reputation (=PK)
			if ((player != null) && (player.getReputation() < 0))
			{
				return GeoData.getInstance().canSeeTarget(me, player); // Los Check
			}
			// Check if the L2MonsterInstance target is aggressive
			if ((target.isMonster()) && Config.GUARD_ATTACK_AGGRO_MOB)
			{
				return (((L2MonsterInstance) target).isAggressive() && GeoData.getInstance().canSeeTarget(me, target));
			}
			
			return false;
		}
		else if (me instanceof L2FriendlyMobInstance)
		{
			// Check if the target isn't another Npc
			if (target.isNpc())
			{
				return false;
			}
			
			// Check if the PlayerInstance target has negative Reputation (=PK)
			if ((target.isPlayer()) && (target.getActingPlayer().getReputation() < 0))
			{
				return GeoData.getInstance().canSeeTarget(me, target); // Los Check
			}
			return false;
		}
		else
		{
			if (target.isAttackable())
			{
				if (!target.isAutoAttackable(me))
				{
					return false;
				}
				
				if (me.getTemplate().isChaos())
				{
					if (((Attackable) target).isInMyClan(me))
					{
						return false;
					}
					// Los Check
					return GeoData.getInstance().canSeeTarget(me, target);
				}
			}
			
			if ((target.isAttackable()) || (target.isNpc()))
			{
				return false;
			}
			
			// depending on config, do not allow mobs to attack _new_ players in peacezones,
			// unless they are already following those players from outside the peacezone.
			if (!Config.ALT_MOB_AGRO_IN_PEACEZONE && target.isInsideZone(ZoneId.PEACE))
			{
				return false;
			}
			
			if (me.isChampion() && Config.L2JMOD_CHAMPION_PASSIVE)
			{
				return false;
			}
			
			// Check if the actor is Aggressive
			return (me.isAggressive() && GeoData.getInstance().canSeeTarget(me, target));
		}
	}
	
	public void startAITask()
	{
		// If not idle - create an AI task (schedule onEvtThink repeatedly)
		if (_aiTask == null)
		{
			_aiTask = ThreadPoolManager.getInstance().scheduleAiAtFixedRate(this, 1000, 1000);
		}
	}
	
	@Override
	public void stopAITask()
	{
		if (_aiTask != null)
		{
			_aiTask.cancel(false);
			_aiTask = null;
		}
		super.stopAITask();
	}
	
	/**
	 * Set the Intention of this L2CharacterAI and create an AI Task executed every 1s (call onEvtThink method) for this L2Attackable.<br>
	 * <FONT COLOR=#FF0000><B> <U>Caution</U> : If actor _knowPlayer isn't EMPTY, AI_INTENTION_IDLE will be change in AI_INTENTION_ACTIVE</B></FONT>
	 * @param intention The new Intention to set to the AI
	 * @param args The first parameter of the Intention
	 */
	@Override
	synchronized void changeIntention(CtrlIntention intention, Object... args)
	{
		if ((intention == AI_INTENTION_IDLE) || (intention == AI_INTENTION_ACTIVE))
		{
			// Check if actor is not dead
			Attackable npc = getActiveChar();
			if (!npc.isAlikeDead())
			{
				// If its _knownPlayer isn't empty set the Intention to AI_INTENTION_ACTIVE
				if (!World.getInstance().getVisibleObjects(npc, PlayerInstance.class).isEmpty())
				{
					intention = AI_INTENTION_ACTIVE;
				}
				else
				{
					if (npc.getSpawn() != null)
					{
						final Location loc = npc.getSpawn().getLocation();
						final int range = Config.MAX_DRIFT_RANGE;
						
						if (!npc.isInsideRadius(loc, range + range, true, false))
						{
							intention = AI_INTENTION_ACTIVE;
						}
					}
				}
			}
			
			if (intention == AI_INTENTION_IDLE)
			{
				// Set the Intention of this L2AttackableAI to AI_INTENTION_IDLE
				super.changeIntention(AI_INTENTION_IDLE);
				
				stopAITask();
				
				// Cancel the AI
				_actor.detachAI();
				
				return;
			}
		}
		
		// Set the Intention of this L2AttackableAI to intention
		super.changeIntention(intention, args);
		
		// If not idle - create an AI task (schedule onEvtThink repeatedly)
		startAITask();
	}
	
	/**
	 * Manage the Attack Intention : Stop current Attack (if necessary), Calculate attack timeout, Start a new Attack and Launch Think Event.
	 * @param target The L2Character to attack
	 */
	@Override
	protected void onIntentionAttack(Creature target)
	{
		// Calculate the attack timeout
		_attackTimeout = MAX_ATTACK_TIMEOUT + GameTimeController.getInstance().getGameTicks();
		
		// self and buffs
		if ((lastBuffTick + 30) < GameTimeController.getInstance().getGameTicks())
		{
			for (Skill buff : getActiveChar().getTemplate().getAISkills(AISkillScope.BUFF))
			{
				if (SkillCaster.checkDoCastConditions(getActiveChar(), buff))
				{
					if (!_actor.isAffectedBySkill(buff.getId()))
					{
						_actor.setTarget(_actor);
						_actor.doCast(buff);
						_actor.setTarget(target);
						LOGGER.debug("{} used buff skill {} on {}", this, buff, _actor);
						break;
					}
				}
			}
			lastBuffTick = GameTimeController.getInstance().getGameTicks();
		}
		
		// Manage the Attack Intention : Stop current Attack (if necessary), Start a new Attack and Launch Think Event
		super.onIntentionAttack(target);
	}
	
	protected void thinkCast()
	{
		if (checkTargetLost(getCastTarget()))
		{
			setCastTarget(null);
			return;
		}
		if (maybeMoveToPawn(getCastTarget(), _actor.getMagicalAttackRange(_skill)))
		{
			return;
		}
		setIntention(AI_INTENTION_ACTIVE);
		_actor.doCast(_skill, _item, _forceUse, _dontMove);
	}
	
	/**
	 * Manage AI standard thinks of a L2Attackable (called by onEvtThink). <B><U> Actions</U> :</B>
	 * <ul>
	 * <li>Update every 1s the _globalAggro counter to come close to 0</li>
	 * <li>If the actor is Aggressive and can attack, add all autoAttackable L2Character in its Aggro Range to its _aggroList, chose a target and order to attack it</li>
	 * <li>If the actor is a L2GuardInstance that can't attack, order to it to return to its home location</li>
	 * <li>If the actor is a L2MonsterInstance that can't attack, order to it to random walk (1/100)</li>
	 * </ul>
	 */
	protected void thinkActive()
	{
		Attackable npc = getActiveChar();
		
		// Update every 1s the _globalAggro counter to come close to 0
		if (_globalAggro != 0)
		{
			if (_globalAggro < 0)
			{
				_globalAggro++;
			}
			else
			{
				_globalAggro--;
			}
		}
		
		// Add all autoAttackable L2Character in L2Attackable Aggro Range to its _aggroList with 0 damage and 1 hate
		// A L2Attackable isn't aggressive during 10s after its spawn because _globalAggro is set to -10
		if (_globalAggro >= 0)
		{
			if (npc.isAggressive())
			{
				World.getInstance().forEachVisibleObjectInRange(npc, Creature.class, npc.getAggroRange(), target ->
				{
					if (target instanceof L2StaticObjectInstance)
					{
						return;
					}
					
					// For each L2Character check if the target is autoattackable
					if (autoAttackCondition(target)) // check aggression
					{
						if (target.isPlayable())
						{
							final TerminateReturn term = EventDispatcher.getInstance().notifyEvent(new OnAttackableHate(getActiveChar(), target.getActingPlayer(), target.isSummon()), getActiveChar(), TerminateReturn.class);
							if ((term != null) && term.terminate())
							{
								return;
							}
						}
						
						// Get the hate level of the L2Attackable against this L2Character target contained in _aggroList
						int hating = npc.getHating(target);
						
						// Add the attacker to the L2Attackable _aggroList with 0 damage and 1 hate
						if (hating == 0)
						{
							npc.addDamageHate(target, 0, 0);
						}
					}
				});
			}
			
			// Chose a target from its aggroList
			Creature hated;
			if (npc.isConfused())
			{
				hated = getAttackTarget(); // effect handles selection
			}
			else
			{
				hated = npc.getMostHated();
			}
			
			// Order to the L2Attackable to attack the target
			if ((hated != null) && !npc.isCoreAIDisabled())
			{
				// Get the hate level of the L2Attackable against this L2Character target contained in _aggroList
				int aggro = npc.getHating(hated);
				
				if ((aggro + _globalAggro) > 0)
				{
					// Set the L2Character movement type to run and send Server->Client packet ChangeMoveType to all others L2PcInstance
					if (!npc.isRunning())
					{
						npc.setRunning();
					}
					
					// Set the AI Intention to AI_INTENTION_ATTACK
					setIntention(CtrlIntention.AI_INTENTION_ATTACK, hated);
				}
				
				return;
			}
		}
		
		// Chance to forget attackers after some time
		if ((npc.getCurrentHp() == npc.getMaxHp()) && (npc.getCurrentMp() == npc.getMaxMp()) && !npc.getAttackByList().isEmpty() && (Rnd.nextInt(500) == 0) && npc.canStopAttackByTime())
		{
			npc.clearAggroList();
			npc.getAttackByList().clear();
			if (npc.isMonster())
			{
				if (((L2MonsterInstance) npc).hasMinions())
				{
					((L2MonsterInstance) npc).getMinionList().deleteReusedMinions();
				}
			}
		}
		
		// Check if the mob should not return to spawn point
		if (!npc.canReturnToSpawnPoint())
		{
			return;
		}
		
		// Check if the actor is a L2GuardInstance
		if ((npc instanceof L2GuardInstance) && !npc.isWalker())
		{
			// Order to the L2GuardInstance to return to its home location because there's no target to attack
			npc.returnHome();
		}
		
		// Minions following leader
		final Creature leader = npc.getLeader();
		if ((leader != null) && !leader.isAlikeDead())
		{
			final int offset;
			final int minRadius = 30;
			
			if (npc.isRaidMinion())
			{
				offset = 500; // for Raids - need correction
			}
			else
			{
				offset = 200; // for normal minions - need correction :)
			}
			
			if (leader.isRunning())
			{
				npc.setRunning();
			}
			else
			{
				npc.setWalking();
			}
			
			if (npc.calculateDistance(leader, false, true) > (offset * offset))
			{
				int x1, y1, z1;
				x1 = Rnd.get(minRadius * 2, offset * 2); // x
				y1 = Rnd.get(x1, offset * 2); // distance
				y1 = (int) Math.sqrt((y1 * y1) - (x1 * x1)); // y
				if (x1 > (offset + minRadius))
				{
					x1 = (leader.getX() + x1) - offset;
				}
				else
				{
					x1 = (leader.getX() - x1) + minRadius;
				}
				if (y1 > (offset + minRadius))
				{
					y1 = (leader.getY() + y1) - offset;
				}
				else
				{
					y1 = (leader.getY() - y1) + minRadius;
				}
				
				z1 = leader.getZ();
				// Move the actor to Location (x,y,z) server side AND client side by sending Server->Client packet CharMoveToLocation (broadcast)
				moveTo(x1, y1, z1);
				return;
			}
			else if (Rnd.nextInt(RANDOM_WALK_RATE) == 0)
			{
				for (Skill sk : npc.getTemplate().getAISkills(AISkillScope.BUFF))
				{
					if (cast(sk))
					{
						return;
					}
				}
			}
		}
		// Order to the L2MonsterInstance to random walk (1/100)
		else if ((npc.getSpawn() != null) && (Rnd.nextInt(RANDOM_WALK_RATE) == 0) && npc.isRandomWalkingEnabled())
		{
			int x1 = 0;
			int y1 = 0;
			int z1 = 0;
			final int range = Config.MAX_DRIFT_RANGE;
			
			for (Skill sk : npc.getTemplate().getAISkills(AISkillScope.BUFF))
			{
				if (cast(sk))
				{
					return;
				}
			}
			
			x1 = npc.getSpawn().getX();
			y1 = npc.getSpawn().getY();
			z1 = npc.getSpawn().getZ();
			
			if (!npc.isInsideRadius(x1, y1, 0, range, false, false))
			{
				npc.setisReturningToSpawnPoint(true);
			}
			else
			{
				int deltaX = Rnd.nextInt(range * 2); // x
				int deltaY = Rnd.get(deltaX, range * 2); // distance
				deltaY = (int) Math.sqrt((deltaY * deltaY) - (deltaX * deltaX)); // y
				x1 = (deltaX + x1) - range;
				y1 = (deltaY + y1) - range;
				z1 = npc.getZ();
			}
			
			// Move the actor to Location (x,y,z) server side AND client side by sending Server->Client packet CharMoveToLocation (broadcast)
			final Location moveLoc = GeoData.getInstance().moveCheck(npc.getX(), npc.getY(), npc.getZ(), x1, y1, z1, npc.getInstanceId());
			
			moveTo(moveLoc.getX(), moveLoc.getY(), moveLoc.getZ());
		}
	}
	
	/**
	 * Manage AI attack thinks of a L2Attackable (called by onEvtThink). <B><U> Actions</U> :</B>
	 * <ul>
	 * <li>Update the attack timeout if actor is running</li>
	 * <li>If target is dead or timeout is expired, stop this attack and set the Intention to AI_INTENTION_ACTIVE</li>
	 * <li>Call all L2Object of its Faction inside the Faction Range</li>
	 * <li>Chose a target and order to attack it with magic skill or physical attack</li>
	 * </ul>
	 * TODO: Manage casting rules to healer mobs (like Ant Nurses)
	 */
	protected void thinkAttack()
	{
		final Attackable npc = getActiveChar();
		if (npc.isCastingNow(s -> !s.isSimultaneousType()))
		{
			return;
		}
		
		Creature originalAttackTarget = getAttackTarget();
		// Check if target is dead or if timeout is expired to stop this attack
		if ((originalAttackTarget == null) || originalAttackTarget.isAlikeDead() || ((_attackTimeout < GameTimeController.getInstance().getGameTicks()) && npc.canStopAttackByTime()))
		{
			// Stop hating this target after the attack timeout or if target is dead
			npc.stopHating(originalAttackTarget);
			
			// Set the AI Intention to AI_INTENTION_ACTIVE
			setIntention(AI_INTENTION_ACTIVE);
			
			npc.setWalking();
			return;
		}
		
		final int collision = npc.getTemplate().getCollisionRadius();
		
		// Handle all L2Object of its Faction inside the Faction Range
		
		Set<Integer> clans = getActiveChar().getTemplate().getClans();
		if ((clans != null) && !clans.isEmpty())
		{
			final int factionRange = npc.getTemplate().getClanHelpRange() + collision;
			// Go through all L2Object that belong to its faction
			try
			{
				World.getInstance().forEachVisibleObjectInRange(npc, Npc.class, factionRange, called ->
				{
					if (!getActiveChar().getTemplate().isClan(called.getTemplate().getClans()))
					{
						return;
					}
					
					// Check if the L2Object is inside the Faction Range of the actor
					if (called.hasAI())
					{
						if ((Math.abs(originalAttackTarget.getZ() - called.getZ()) < 600) && npc.getAttackByList().contains(originalAttackTarget) && ((called.getAI()._intention == CtrlIntention.AI_INTENTION_IDLE) || (called.getAI()._intention == CtrlIntention.AI_INTENTION_ACTIVE)) && (called.getInstanceId() == npc.getInstanceId()))
						{
							if (originalAttackTarget.isPlayable())
							{
								// By default, when a faction member calls for help, attack the caller's attacker.
								// Notify the AI with EVT_AGGRESSION
								called.getAI().notifyEvent(CtrlEvent.EVT_AGGRESSION, originalAttackTarget, 1);
								EventDispatcher.getInstance().notifyEventAsync(new OnAttackableFactionCall(called, getActiveChar(), originalAttackTarget.getActingPlayer(), originalAttackTarget.isSummon()), called);
							}
							else if (called.isAttackable() && (getAttackTarget() != null) && (called.getAI()._intention != CtrlIntention.AI_INTENTION_ATTACK))
							{
								((Attackable) called).addDamageHate(getAttackTarget(), 0, npc.getHating(getAttackTarget()));
								called.getAI().setIntention(CtrlIntention.AI_INTENTION_ATTACK, getAttackTarget());
							}
						}
					}
				});
			}
			catch (NullPointerException e)
			{
				LOGGER.warn(getClass().getSimpleName() + ": thinkAttack() faction call failed: " + e.getMessage());
			}
		}
		
		if (npc.isCoreAIDisabled())
		{
			return;
		}
		
		// Initialize data
		Creature mostHate = npc.getMostHated();
		if (mostHate == null)
		{
			setIntention(AI_INTENTION_ACTIVE);
			return;
		}
		
		setAttackTarget(mostHate);
		npc.setTarget(mostHate);
		
		final int combinedCollision = collision + mostHate.getTemplate().getCollisionRadius();
		
		final List<Skill> aiSuicideSkills = npc.getTemplate().getAISkills(AISkillScope.SUICIDE);
		if (!aiSuicideSkills.isEmpty() && ((int) ((npc.getCurrentHp() / npc.getMaxHp()) * 100) < 30))
		{
			final Skill skill = aiSuicideSkills.get(Rnd.get(aiSuicideSkills.size()));
			if (Util.checkIfInRange(skill.getAffectRange(), getActiveChar(), mostHate, false) && npc.hasSkillChance())
			{
				if (cast(skill))
				{
					LOGGER.debug("{} used suicide skill {}", this, skill);
					return;
				}
			}
		}
		
		// ------------------------------------------------------
		// In case many mobs are trying to hit from same place, move a bit,
		// circling around the target
		// Note from Gnacik:
		// On l2js because of that sometimes mobs don't attack player only running
		// around player without any sense, so decrease chance for now
		if (!npc.isMovementDisabled() && (Rnd.nextInt(100) <= 3))
		{
			for (Attackable nearby : World.getInstance().getVisibleObjects(npc, Attackable.class))
			{
				if (npc.isInsideRadius(nearby, collision, false, false) && (nearby != mostHate))
				{
					int newX = combinedCollision + Rnd.get(40);
					if (Rnd.nextBoolean())
					{
						newX = mostHate.getX() + newX;
					}
					else
					{
						newX = mostHate.getX() - newX;
					}
					int newY = combinedCollision + Rnd.get(40);
					if (Rnd.nextBoolean())
					{
						newY = mostHate.getY() + newY;
					}
					else
					{
						newY = mostHate.getY() - newY;
					}
					
					if (!npc.isInsideRadius(newX, newY, 0, collision, false, false))
					{
						int newZ = npc.getZ() + 30;
						if (GeoData.getInstance().canMove(npc.getX(), npc.getY(), npc.getZ(), newX, newY, newZ, npc.getInstanceId()))
						{
							moveTo(newX, newY, newZ);
						}
					}
					return;
				}
			}
		}
		// Dodge if its needed
		if (!npc.isMovementDisabled() && (npc.getTemplate().getDodge() > 0))
		{
			if (Rnd.get(100) <= npc.getTemplate().getDodge())
			{
				// Micht: kepping this one otherwise we should do 2 sqrt
				double distance2 = npc.calculateDistance(mostHate, false, true);
				if (Math.sqrt(distance2) <= (60 + combinedCollision))
				{
					int posX = npc.getX();
					int posY = npc.getY();
					int posZ = npc.getZ() + 30;
					
					if (originalAttackTarget.getX() < posX)
					{
						posX = posX + 300;
					}
					else
					{
						posX = posX - 300;
					}
					
					if (originalAttackTarget.getY() < posY)
					{
						posY = posY + 300;
					}
					else
					{
						posY = posY - 300;
					}
					
					if (GeoData.getInstance().canMove(npc.getX(), npc.getY(), npc.getZ(), posX, posY, posZ, npc.getInstanceId()))
					{
						setIntention(CtrlIntention.AI_INTENTION_MOVE_TO, new Location(posX, posY, posZ, 0));
					}
					return;
				}
			}
		}
		
		// ------------------------------------------------------------------------------
		// BOSS/Raid Minion Target Reconsider
		if (npc.isRaid() || npc.isRaidMinion())
		{
			chaostime++;
			if (npc instanceof L2RaidBossInstance)
			{
				if (!((L2MonsterInstance) npc).hasMinions())
				{
					if (chaostime > Config.RAID_CHAOS_TIME)
					{
						if (Rnd.get(100) <= (100 - ((npc.getCurrentHp() * 100) / npc.getMaxHp())))
						{
							aggroReconsider();
							chaostime = 0;
							return;
						}
					}
				}
				else
				{
					if (chaostime > Config.RAID_CHAOS_TIME)
					{
						if (Rnd.get(100) <= (100 - ((npc.getCurrentHp() * 200) / npc.getMaxHp())))
						{
							aggroReconsider();
							chaostime = 0;
							return;
						}
					}
				}
			}
			else if (npc instanceof L2GrandBossInstance)
			{
				if (chaostime > Config.GRAND_CHAOS_TIME)
				{
					double chaosRate = 100 - ((npc.getCurrentHp() * 300) / npc.getMaxHp());
					if (((chaosRate <= 10) && (Rnd.get(100) <= 10)) || ((chaosRate > 10) && (Rnd.get(100) <= chaosRate)))
					{
						aggroReconsider();
						chaostime = 0;
						return;
					}
				}
			}
			else
			{
				if (chaostime > Config.MINION_CHAOS_TIME)
				{
					if (Rnd.get(100) <= (100 - ((npc.getCurrentHp() * 200) / npc.getMaxHp())))
					{
						aggroReconsider();
						chaostime = 0;
						return;
					}
				}
			}
		}
		
		final List<Skill> generalSkills = npc.getTemplate().getAISkills(AISkillScope.GENERAL);
		if (!generalSkills.isEmpty())
		{
			// -------------------------------------------------------------------------------
			// Heal Condition
			final List<Skill> aiHealSkills = npc.getTemplate().getAISkills(AISkillScope.HEAL);
			if (!aiHealSkills.isEmpty())
			{
				double percentage = (npc.getCurrentHp() / npc.getMaxHp()) * 100;
				if (npc.isMinion())
				{
					Creature leader = npc.getLeader();
					if ((leader != null) && !leader.isDead() && (Rnd.get(100) > ((leader.getCurrentHp() / leader.getMaxHp()) * 100)))
					{
						for (Skill healSkill : aiHealSkills)
						{
							if (healSkill.getTargetType() == L2TargetType.SELF)
							{
								continue;
							}
							if (!SkillCaster.checkDoCastConditions(npc, healSkill))
							{
								continue;
							}
							if (!Util.checkIfInRange((healSkill.getCastRange() + collision + leader.getTemplate().getCollisionRadius()), npc, leader, false) && !isParty(healSkill) && !npc.isMovementDisabled())
							{
								moveToPawn(leader, healSkill.getCastRange() + collision + leader.getTemplate().getCollisionRadius());
								return;
							}
							if (GeoData.getInstance().canSeeTarget(npc, leader))
							{
								final WorldObject target = npc.getTarget();
								npc.setTarget(leader);
								npc.doCast(healSkill);
								npc.setTarget(target);
								LOGGER.debug("{} used heal skill {} on leader {}", this, healSkill, leader);
								return;
							}
						}
					}
				}
				if (Rnd.get(100) < ((100 - percentage) / 3))
				{
					for (Skill sk : aiHealSkills)
					{
						if (!SkillCaster.checkDoCastConditions(npc, sk))
						{
							continue;
						}
						final WorldObject target = npc.getTarget();
						npc.setTarget(npc);
						npc.doCast(sk);
						npc.setTarget(target);
						LOGGER.debug("{} used heal skill {} on itself", this, sk);
						return;
					}
				}
				for (Skill sk : aiHealSkills)
				{
					if (!SkillCaster.checkDoCastConditions(npc, sk))
					{
						continue;
					}
					if (sk.getTargetType() == L2TargetType.ONE)
					{
						for (Attackable targets : World.getInstance().getVisibleObjects(npc, Attackable.class, sk.getCastRange() + collision))
						{
							if (targets.isDead())
							{
								return;
							}
							
							if (!targets.isInMyClan(npc))
							{
								return;
							}
							percentage = (targets.getCurrentHp() / targets.getMaxHp()) * 100;
							if (Rnd.get(100) < ((100 - percentage) / 10))
							{
								if (GeoData.getInstance().canSeeTarget(npc, targets))
								{
									final WorldObject target = npc.getTarget();
									npc.setTarget(targets);
									npc.doCast(sk);
									npc.setTarget(target);
									LOGGER.debug("{} used heal skill {} on {}", this, sk, targets);
									return;
								}
							}
						}
					}
					if (isParty(sk))
					{
						npc.doCast(sk);
						return;
					}
				}
			}
			// -------------------------------------------------------------------------------
			// Res Skill Condition
			final List<Skill> aiResSkills = npc.getTemplate().getAISkills(AISkillScope.RES);
			if (!aiResSkills.isEmpty())
			{
				if (npc.isMinion())
				{
					Creature leader = npc.getLeader();
					if ((leader != null) && leader.isDead())
					{
						for (Skill sk : aiResSkills)
						{
							if (sk.getTargetType() == L2TargetType.SELF)
							{
								continue;
							}
							if (!SkillCaster.checkDoCastConditions(npc, sk))
							{
								continue;
							}
							if (!Util.checkIfInRange((sk.getCastRange() + collision + leader.getTemplate().getCollisionRadius()), npc, leader, false) && !isParty(sk) && !npc.isMovementDisabled())
							{
								moveToPawn(leader, sk.getCastRange() + collision + leader.getTemplate().getCollisionRadius());
								return;
							}
							if (GeoData.getInstance().canSeeTarget(npc, leader))
							{
								final WorldObject target = npc.getTarget();
								npc.setTarget(leader);
								npc.doCast(sk);
								npc.setTarget(target);
								LOGGER.debug("{} used resurrection skill {} on leader {}", this, sk, leader);
								return;
							}
						}
					}
				}
				for (Skill sk : aiResSkills)
				{
					if (!SkillCaster.checkDoCastConditions(npc, sk))
					{
						continue;
					}
					if (sk.getTargetType() == L2TargetType.ONE)
					{
						for (Attackable targets : World.getInstance().getVisibleObjects(npc, Attackable.class, sk.getCastRange() + collision))
						{
							if (!targets.isDead())
							{
								continue;
							}
							
							if (!npc.isInMyClan(targets))
							{
								continue;
							}
							if (Rnd.get(100) < 10)
							{
								if (GeoData.getInstance().canSeeTarget(npc, targets))
								{
									final WorldObject target = npc.getTarget();
									npc.setTarget(targets);
									npc.doCast(sk);
									npc.setTarget(target);
									LOGGER.debug("{} used heal skill {} on clan member {}", this, sk, targets);
									return;
								}
							}
						}
					}
					if (isParty(sk))
					{
						final WorldObject target = npc.getTarget();
						npc.setTarget(npc);
						npc.doCast(sk);
						npc.setTarget(target);
						return;
					}
				}
			}
		}
		
		double dist = npc.calculateDistance(mostHate, false, false);
		int dist2 = (int) dist - collision;
		int range = npc.getPhysicalAttackRange() + combinedCollision;
		if (mostHate.isMoving())
		{
			range = range + 50;
			if (npc.isMoving())
			{
				range = range + 50;
			}
		}
		
		// -------------------------------------------------------------------------------
		// Immobilize Condition
		if ((npc.isMovementDisabled() && ((dist > range) || mostHate.isMoving())) || ((dist > range) && mostHate.isMoving()))
		{
			movementDisable();
			return;
		}
		
		setTimepass(0);
		// --------------------------------------------------------------------------------
		// Long/Short Range skill usage.
		if (!npc.getShortRangeSkills().isEmpty() && npc.hasSkillChance())
		{
			final Skill shortRangeSkill = npc.getShortRangeSkills().get(Rnd.get(npc.getShortRangeSkills().size()));
			if (SkillCaster.checkDoCastConditions(npc, shortRangeSkill))
			{
				npc.doCast(shortRangeSkill);
				LOGGER.debug("{} used short range skill {} on {}", this, shortRangeSkill, npc.getTarget());
				return;
			}
		}
		
		if (!npc.getLongRangeSkills().isEmpty() && npc.hasSkillChance())
		{
			final Skill longRangeSkill = npc.getLongRangeSkills().get(Rnd.get(npc.getLongRangeSkills().size()));
			if (SkillCaster.checkDoCastConditions(npc, longRangeSkill))
			{
				npc.doCast(longRangeSkill);
				LOGGER.debug("{} used long range skill {} on {}", this, longRangeSkill, npc.getTarget());
				return;
			}
		}
		
		// --------------------------------------------------------------------------------
		// Starts Melee or Primary Skill
		if ((dist2 > range) || !GeoData.getInstance().canSeeTarget(npc, mostHate))
		{
			if (npc.isMovementDisabled())
			{
				targetReconsider();
			}
			else if (getAttackTarget() != null)
			{
				if (getAttackTarget().isMoving())
				{
					range -= 100;
				}
				if (range < 5)
				{
					range = 5;
				}
				moveToPawn(getAttackTarget(), range);
			}
			return;
		}
		
		// Attacks target
		_actor.doAttack(getAttackTarget());
	}
	
	private boolean cast(Skill sk)
	{
		if (sk == null)
		{
			return false;
		}
		
		final Attackable caster = getActiveChar();
		
		if (!SkillCaster.checkDoCastConditions(caster, sk))
		{
			return false;
		}
		
		if (getAttackTarget() == null)
		{
			if (caster.getMostHated() != null)
			{
				setAttackTarget(caster.getMostHated());
			}
		}
		Creature attackTarget = getAttackTarget();
		if (attackTarget == null)
		{
			return false;
		}
		double dist = caster.calculateDistance(attackTarget, false, false);
		double dist2 = dist - attackTarget.getTemplate().getCollisionRadius();
		double range = caster.getPhysicalAttackRange() + caster.getTemplate().getCollisionRadius() + attackTarget.getTemplate().getCollisionRadius();
		double srange = sk.getCastRange() + caster.getTemplate().getCollisionRadius();
		if (attackTarget.isMoving())
		{
			dist2 = dist2 - 30;
		}
		
		if (sk.isContinuous())
		{
			if (!sk.isDebuff())
			{
				if (!caster.isAffectedBySkill(sk.getId()))
				{
					caster.setTarget(caster);
					caster.doCast(sk);
					_actor.setTarget(attackTarget);
					return true;
				}
				// ----------------------------------------
				// If actor already have buff, start looking at others same faction mob to cast
				if (sk.getTargetType() == L2TargetType.SELF)
				{
					return false;
				}
				if (sk.getTargetType() == L2TargetType.ONE)
				{
					Creature target = effectTargetReconsider(sk, true);
					if (target != null)
					{
						caster.setTarget(target);
						caster.doCast(sk);
						caster.setTarget(attackTarget);
						return true;
					}
				}
				if (canParty(sk))
				{
					caster.setTarget(caster);
					caster.doCast(sk);
					caster.setTarget(attackTarget);
					return true;
				}
			}
			else
			{
				if (GeoData.getInstance().canSeeTarget(caster, attackTarget) && !canAOE(sk) && !attackTarget.isDead() && (dist2 <= srange))
				{
					if (!attackTarget.isAffectedBySkill(sk.getId()))
					{
						caster.doCast(sk);
						return true;
					}
				}
				else if (canAOE(sk))
				{
					if ((sk.getTargetType() == L2TargetType.AURA) || (sk.getTargetType() == L2TargetType.BEHIND_AURA) || (sk.getTargetType() == L2TargetType.FRONT_AURA) || (sk.getTargetType() == L2TargetType.AURA_CORPSE_MOB))
					{
						caster.doCast(sk);
						return true;
					}
					if (((sk.getTargetType() == L2TargetType.AREA) || (sk.getTargetType() == L2TargetType.BEHIND_AREA) || (sk.getTargetType() == L2TargetType.FRONT_AREA)) && GeoData.getInstance().canSeeTarget(caster, attackTarget) && !attackTarget.isDead() && (dist2 <= srange))
					{
						caster.doCast(sk);
						return true;
					}
				}
				else if (sk.getTargetType() == L2TargetType.ONE)
				{
					Creature target = effectTargetReconsider(sk, false);
					if (target != null)
					{
						caster.doCast(sk);
						return true;
					}
				}
			}
		}
		
		if (sk.hasEffectType(L2EffectType.DISPEL, L2EffectType.DISPEL_BY_SLOT))
		{
			if (sk.getTargetType() == L2TargetType.ONE)
			{
				if ((attackTarget.getEffectList().getFirstEffect(L2EffectType.BUFF) != null) && GeoData.getInstance().canSeeTarget(caster, attackTarget) && !attackTarget.isDead() && (dist2 <= srange))
				{
					caster.doCast(sk);
					return true;
				}
				Creature target = effectTargetReconsider(sk, false);
				if (target != null)
				{
					caster.setTarget(target);
					caster.doCast(sk);
					caster.setTarget(attackTarget);
					return true;
				}
			}
			else if (canAOE(sk))
			{
				if (((sk.getTargetType() == L2TargetType.AURA) || (sk.getTargetType() == L2TargetType.BEHIND_AURA) || (sk.getTargetType() == L2TargetType.FRONT_AURA)) && GeoData.getInstance().canSeeTarget(caster, attackTarget))
				
				{
					caster.doCast(sk);
					return true;
				}
				else if (((sk.getTargetType() == L2TargetType.AREA) || (sk.getTargetType() == L2TargetType.BEHIND_AREA) || (sk.getTargetType() == L2TargetType.FRONT_AREA)) && GeoData.getInstance().canSeeTarget(caster, attackTarget) && !attackTarget.isDead() && (dist2 <= srange))
				{
					caster.doCast(sk);
					return true;
				}
			}
		}
		
		if (sk.hasEffectType(L2EffectType.HEAL))
		{
			double percentage = (caster.getCurrentHp() / caster.getMaxHp()) * 100;
			if (caster.isMinion() && (sk.getTargetType() != L2TargetType.SELF))
			{
				Creature leader = caster.getLeader();
				if ((leader != null) && !leader.isDead() && (Rnd.get(100) > ((leader.getCurrentHp() / leader.getMaxHp()) * 100)))
				{
					if (!Util.checkIfInRange((sk.getCastRange() + caster.getTemplate().getCollisionRadius() + leader.getTemplate().getCollisionRadius()), caster, leader, false) && !isParty(sk) && !caster.isMovementDisabled())
					{
						moveToPawn(leader, sk.getCastRange() + caster.getTemplate().getCollisionRadius() + leader.getTemplate().getCollisionRadius());
					}
					if (GeoData.getInstance().canSeeTarget(caster, leader))
					{
						caster.setTarget(leader);
						caster.doCast(sk);
						caster.setTarget(attackTarget);
						return true;
					}
				}
			}
			if (Rnd.get(100) < ((100 - percentage) / 3))
			{
				caster.setTarget(caster);
				caster.doCast(sk);
				caster.setTarget(attackTarget);
				return true;
			}
			
			if (sk.getTargetType() == L2TargetType.ONE)
			{
				for (Attackable obj : World.getInstance().getVisibleObjects(caster, Attackable.class, sk.getCastRange() + caster.getTemplate().getCollisionRadius()))
				{
					if (obj.isDead())
					{
						continue;
					}
					
					if (!caster.isInMyClan(obj))
					{
						continue;
					}
					
					percentage = (obj.getCurrentHp() / obj.getMaxHp()) * 100;
					if (Rnd.get(100) < ((100 - percentage) / 10))
					{
						if (GeoData.getInstance().canSeeTarget(caster, obj))
						{
							caster.setTarget(obj);
							caster.doCast(sk);
							caster.setTarget(attackTarget);
							return true;
						}
					}
				}
			}
			if (isParty(sk))
			{
				for (Attackable obj : World.getInstance().getVisibleObjects(caster, Attackable.class, sk.getAffectRange() + caster.getTemplate().getCollisionRadius()))
				{
					if (obj.isInMyClan(caster))
					{
						if ((obj.getCurrentHp() < obj.getMaxHp()) && (Rnd.get(100) <= 20))
						{
							caster.setTarget(caster);
							caster.doCast(sk);
							caster.setTarget(attackTarget);
							return true;
						}
					}
				}
			}
		}
		
		if (sk.hasEffectType(L2EffectType.PHYSICAL_ATTACK, L2EffectType.PHYSICAL_ATTACK_HP_LINK, L2EffectType.MAGICAL_ATTACK, L2EffectType.DEATH_LINK, L2EffectType.HP_DRAIN))
		{
			if (!canAura(sk))
			{
				if (GeoData.getInstance().canSeeTarget(caster, attackTarget) && !attackTarget.isDead() && (dist2 <= srange))
				{
					caster.doCast(sk);
					return true;
				}
				
				Creature target = skillTargetReconsider(sk);
				if (target != null)
				{
					caster.setTarget(target);
					caster.doCast(sk);
					caster.setTarget(attackTarget);
					return true;
				}
			}
			else
			{
				caster.doCast(sk);
				return true;
			}
		}
		
		if (sk.hasEffectType(L2EffectType.SLEEP))
		{
			if (sk.getTargetType() == L2TargetType.ONE)
			{
				if (!attackTarget.isDead() && (dist2 <= srange))
				{
					if ((dist2 > range) || attackTarget.isMoving())
					{
						if (!attackTarget.isAffectedBySkill(sk.getId()))
						{
							caster.doCast(sk);
							return true;
						}
					}
				}
				
				Creature target = effectTargetReconsider(sk, false);
				if (target != null)
				{
					caster.doCast(sk);
					return true;
				}
			}
			else if (canAOE(sk))
			{
				if ((sk.getTargetType() == L2TargetType.AURA) || (sk.getTargetType() == L2TargetType.BEHIND_AURA) || (sk.getTargetType() == L2TargetType.FRONT_AURA))
				{
					caster.doCast(sk);
					return true;
				}
				if (((sk.getTargetType() == L2TargetType.AREA) || (sk.getTargetType() == L2TargetType.BEHIND_AREA) || (sk.getTargetType() == L2TargetType.FRONT_AREA)) && GeoData.getInstance().canSeeTarget(caster, attackTarget) && !attackTarget.isDead() && (dist2 <= srange))
				{
					caster.doCast(sk);
					return true;
				}
			}
		}
		
		if (sk.hasEffectType(L2EffectType.BLOCK_ACTIONS, L2EffectType.ROOT, L2EffectType.MUTE, L2EffectType.BLOCK_CONTROL))
		{
			if (GeoData.getInstance().canSeeTarget(caster, attackTarget) && !canAOE(sk) && (dist2 <= srange))
			{
				if (!attackTarget.isAffectedBySkill(sk.getId()))
				{
					caster.doCast(sk);
					return true;
				}
			}
			else if (canAOE(sk))
			{
				if ((sk.getTargetType() == L2TargetType.AURA) || (sk.getTargetType() == L2TargetType.BEHIND_AURA) || (sk.getTargetType() == L2TargetType.FRONT_AURA))
				{
					caster.doCast(sk);
					return true;
				}
				if (((sk.getTargetType() == L2TargetType.AREA) || (sk.getTargetType() == L2TargetType.BEHIND_AREA) || (sk.getTargetType() == L2TargetType.FRONT_AREA)) && GeoData.getInstance().canSeeTarget(caster, attackTarget) && !attackTarget.isDead() && (dist2 <= srange))
				{
					caster.doCast(sk);
					return true;
				}
			}
			else if (sk.getTargetType() == L2TargetType.ONE)
			{
				Creature target = effectTargetReconsider(sk, false);
				if (target != null)
				{
					caster.doCast(sk);
					return true;
				}
			}
		}
		
		if (sk.hasEffectType(L2EffectType.DMG_OVER_TIME, L2EffectType.DMG_OVER_TIME_PERCENT))
		{
			if (GeoData.getInstance().canSeeTarget(caster, attackTarget) && !canAOE(sk) && !attackTarget.isDead() && (dist2 <= srange))
			{
				if (!attackTarget.isAffectedBySkill(sk.getId()))
				{
					caster.doCast(sk);
					return true;
				}
			}
			else if (canAOE(sk))
			{
				if ((sk.getTargetType() == L2TargetType.AURA) || (sk.getTargetType() == L2TargetType.BEHIND_AURA) || (sk.getTargetType() == L2TargetType.FRONT_AURA) || (sk.getTargetType() == L2TargetType.AURA_CORPSE_MOB))
				{
					caster.doCast(sk);
					return true;
				}
				if (((sk.getTargetType() == L2TargetType.AREA) || (sk.getTargetType() == L2TargetType.BEHIND_AREA) || (sk.getTargetType() == L2TargetType.FRONT_AREA)) && GeoData.getInstance().canSeeTarget(caster, attackTarget) && !attackTarget.isDead() && (dist2 <= srange))
				{
					caster.doCast(sk);
					return true;
				}
			}
			else if (sk.getTargetType() == L2TargetType.ONE)
			{
				Creature target = effectTargetReconsider(sk, false);
				if (target != null)
				{
					caster.doCast(sk);
					return true;
				}
			}
		}
		
		if (sk.hasEffectType(L2EffectType.RESURRECTION))
		{
			if (!isParty(sk))
			{
				if (caster.isMinion() && (sk.getTargetType() != L2TargetType.SELF))
				{
					Creature leader = caster.getLeader();
					if (leader != null)
					{
						if (leader.isDead())
						{
							if (!Util.checkIfInRange((sk.getCastRange() + caster.getTemplate().getCollisionRadius() + leader.getTemplate().getCollisionRadius()), caster, leader, false) && !isParty(sk) && !caster.isMovementDisabled())
							{
								moveToPawn(leader, sk.getCastRange() + caster.getTemplate().getCollisionRadius() + leader.getTemplate().getCollisionRadius());
							}
						}
						if (GeoData.getInstance().canSeeTarget(caster, leader))
						{
							caster.setTarget(leader);
							caster.doCast(sk);
							caster.setTarget(attackTarget);
							return true;
						}
					}
				}
				
				for (Attackable obj : World.getInstance().getVisibleObjects(caster, Attackable.class, sk.getCastRange() + caster.getTemplate().getCollisionRadius()))
				{
					if (!obj.isDead())
					{
						continue;
					}
					
					if (!caster.isInMyClan(obj))
					{
						continue;
					}
					
					if (Rnd.get(100) < 10)
					{
						if (GeoData.getInstance().canSeeTarget(caster, obj))
						{
							caster.setTarget(obj);
							caster.doCast(sk);
							caster.setTarget(attackTarget);
							return true;
						}
					}
				}
			}
			else if (isParty(sk))
			{
				for (Creature obj : World.getInstance().getVisibleObjects(caster, Creature.class, sk.getAffectRange() + caster.getTemplate().getCollisionRadius()))
				{
					if (!obj.isAttackable())
					{
						continue;
					}
					Npc targets = ((Npc) obj);
					if (caster.isInMyClan(targets))
					{
						if ((obj.getCurrentHp() < obj.getMaxHp()) && (Rnd.get(100) <= 20))
						{
							caster.setTarget(caster);
							caster.doCast(sk);
							caster.setTarget(attackTarget);
							return true;
						}
					}
				}
			}
		}
		
		if (!canAura(sk))
		{
			
			if (GeoData.getInstance().canSeeTarget(caster, attackTarget) && !attackTarget.isDead() && (dist2 <= srange))
			{
				caster.doCast(sk);
				return true;
			}
			
			Creature target = skillTargetReconsider(sk);
			if (target != null)
			{
				caster.setTarget(target);
				caster.doCast(sk);
				caster.setTarget(attackTarget);
				return true;
			}
		}
		else
		{
			caster.doCast(sk);
			return true;
		}
		return false;
	}
	
	/**
	 * This AI task will start when ACTOR cannot move and attack range larger than distance
	 */
	private void movementDisable()
	{
		final Attackable npc = getActiveChar();
		double dist = 0;
		double dist2 = 0;
		int range = 0;
		try
		{
			final Creature attackTarget = getAttackTarget();
			if (attackTarget == null)
			{
				return;
			}
			
			if (npc.getTarget() == null)
			{
				npc.setTarget(attackTarget);
			}
			dist = npc.calculateDistance(attackTarget, false, false);
			dist2 = dist - npc.getTemplate().getCollisionRadius();
			range = npc.getPhysicalAttackRange() + npc.getTemplate().getCollisionRadius() + attackTarget.getTemplate().getCollisionRadius();
			if (attackTarget.isMoving())
			{
				dist = dist - 30;
				if (npc.isMoving())
				{
					dist = dist - 50;
				}
			}
			
			// Check if activeChar has any skill
			if (!npc.getTemplate().getAISkills(AISkillScope.GENERAL).isEmpty())
			{
				// -------------------------------------------------------------
				// Try to stop the target or disable the target as priority
				int random = Rnd.get(100);
				if (!attackTarget.isImmobilized() && (random < 2))
				{
					for (Skill sk : npc.getTemplate().getAISkills(AISkillScope.IMMOBILIZE))
					{
						if (!SkillCaster.checkDoCastConditions(npc, sk) || (((sk.getCastRange() + npc.getTemplate().getCollisionRadius() + attackTarget.getTemplate().getCollisionRadius()) <= dist2) && !canAura(sk)))
						{
							continue;
						}
						if (!GeoData.getInstance().canSeeTarget(npc, attackTarget))
						{
							continue;
						}
						if (!attackTarget.isAffectedBySkill(sk.getId()))
						{
							// L2Object target = attackTarget;
							// _actor.setTarget(_actor);
							npc.doCast(sk);
							// _actor.setTarget(target);
							return;
						}
					}
				}
				// -------------------------------------------------------------
				// Same as Above, but with Mute/FEAR etc....
				if (random < 5)
				{
					for (Skill sk : npc.getTemplate().getAISkills(AISkillScope.COT))
					{
						if (!SkillCaster.checkDoCastConditions(npc, sk) || (((sk.getCastRange() + npc.getTemplate().getCollisionRadius() + attackTarget.getTemplate().getCollisionRadius()) <= dist2) && !canAura(sk)))
						{
							continue;
						}
						if (!GeoData.getInstance().canSeeTarget(npc, attackTarget))
						{
							continue;
						}
						if (!attackTarget.isAffectedBySkill(sk.getId()))
						{
							// L2Object target = attackTarget;
							// _actor.setTarget(_actor);
							npc.doCast(sk);
							// _actor.setTarget(target);
							return;
						}
					}
				}
				// -------------------------------------------------------------
				if (random < 8)
				{
					for (Skill sk : npc.getTemplate().getAISkills(AISkillScope.DEBUFF))
					{
						if (!SkillCaster.checkDoCastConditions(npc, sk) || (((sk.getCastRange() + npc.getTemplate().getCollisionRadius() + attackTarget.getTemplate().getCollisionRadius()) <= dist2) && !canAura(sk)))
						{
							continue;
						}
						if (!GeoData.getInstance().canSeeTarget(npc, attackTarget))
						{
							continue;
						}
						if (!attackTarget.isAffectedBySkill(sk.getId()))
						{
							// L2Object target = attackTarget;
							// _actor.setTarget(_actor);
							npc.doCast(sk);
							// _actor.setTarget(target);
							return;
						}
					}
				}
				// -------------------------------------------------------------
				// Some side effect skill like CANCEL or NEGATE
				if (random < 9)
				{
					for (Skill sk : npc.getTemplate().getAISkills(AISkillScope.NEGATIVE))
					{
						if (!SkillCaster.checkDoCastConditions(npc, sk) || (((sk.getCastRange() + npc.getTemplate().getCollisionRadius() + attackTarget.getTemplate().getCollisionRadius()) <= dist2) && !canAura(sk)))
						{
							continue;
						}
						if (!GeoData.getInstance().canSeeTarget(npc, attackTarget))
						{
							continue;
						}
						if (attackTarget.getEffectList().getFirstEffect(L2EffectType.BUFF) != null)
						{
							// L2Object target = attackTarget;
							// _actor.setTarget(_actor);
							npc.doCast(sk);
							// _actor.setTarget(target);
							return;
						}
					}
				}
				// -------------------------------------------------------------
				// Start ATK SKILL when nothing can be done
				if ((npc.isMovementDisabled() || (npc.getAiType() == AIType.MAGE) || (npc.getAiType() == AIType.HEALER)))
				{
					for (Skill sk : npc.getTemplate().getAISkills(AISkillScope.ATTACK))
					{
						if (!SkillCaster.checkDoCastConditions(npc, sk) || (((sk.getCastRange() + npc.getTemplate().getCollisionRadius() + attackTarget.getTemplate().getCollisionRadius()) <= dist2) && !canAura(sk)))
						{
							continue;
						}
						if (!GeoData.getInstance().canSeeTarget(npc, attackTarget))
						{
							continue;
						}
						// L2Object target = attackTarget;
						// _actor.setTarget(_actor);
						npc.doCast(sk);
						// _actor.setTarget(target);
						return;
					}
				}
				// -------------------------------------------------------------
				// if there is no ATK skill to use, then try Universal skill
				// @formatter:off
				/*
				for(L2Skill sk:_skillrender.getUniversalSkills())
				{
					if(sk.getMpConsume()>=_actor.getCurrentMp()
							|| _actor.isSkillDisabled(sk.getId())
							||(sk.getCastRange()+ _actor.getTemplate().getCollisionRadius() + attackTarget.getTemplate().getCollisionRadius() <= dist2 && !canAura(sk))
							||(sk.isMagic()&&_actor.isMuted())
							||(!sk.isMagic()&&_actor.isPhysicalMuted()))
					{
						continue;
					}
					if(!GeoData.getInstance().canSeeTarget(_actor,attackTarget))
						continue;
					clientStopMoving(null);
					L2Object target = attackTarget;
					//_actor.setTarget(_actor);
					_actor.doCast(sk);
					//_actor.setTarget(target);
					return;
				}
				*/
				// @formatter:on
			}
			// timepass = timepass + 1;
			if (npc.isMovementDisabled())
			{
				// timepass = 0;
				targetReconsider();
				
				return;
			}
			// else if(timepass>=5)
			// {
			// timepass = 0;
			// AggroReconsider();
			// return;
			// }
			
			if ((dist > range) || !GeoData.getInstance().canSeeTarget(npc, attackTarget))
			{
				if (attackTarget.isMoving())
				{
					range -= 100;
				}
				if (range < 5)
				{
					range = 5;
				}
				moveToPawn(attackTarget, range);
				return;
				
			}
			
			// Attacks target
			_actor.doAttack(getAttackTarget());
		}
		catch (NullPointerException e)
		{
			setIntention(AI_INTENTION_ACTIVE);
			LOGGER.warn("{} - failed executing movementDisable()", this, e);
			return;
		}
	}
	
	private Creature effectTargetReconsider(Skill sk, boolean positive)
	{
		if (sk == null)
		{
			return null;
		}
		Attackable actor = getActiveChar();
		if (!sk.hasEffectType(L2EffectType.DISPEL, L2EffectType.DISPEL_BY_SLOT))
		{
			if (!positive)
			{
				double dist = 0;
				double dist2 = 0;
				int range = 0;
				
				for (WeakReference<Creature> obj : actor.getAttackByList())
				{
					final Creature creature = obj.get();
					if (creature == null)
					{
						actor.getAttackByList().remove(obj);
						continue;
					}
					else if (creature.isDead() || !GeoData.getInstance().canSeeTarget(actor, creature) || (creature == getAttackTarget()))
					{
						continue;
					}
					try
					{
						actor.setTarget(getAttackTarget());
						dist = actor.calculateDistance(creature, false, false);
						dist2 = dist - actor.getTemplate().getCollisionRadius();
						range = sk.getCastRange() + actor.getTemplate().getCollisionRadius() + creature.getTemplate().getCollisionRadius();
						if (creature.isMoving())
						{
							dist2 = dist2 - 70;
						}
					}
					catch (NullPointerException e)
					{
						continue;
					}
					if (dist2 <= range)
					{
						if (!getAttackTarget().isAffectedBySkill(sk.getId()))
						{
							return creature;
						}
					}
				}
				
				// ----------------------------------------------------------------------
				// If there is nearby Target with aggro, start going on random target that is attackable
				for (Creature obj : World.getInstance().getVisibleObjects(actor, Creature.class, range))
				{
					if (obj.isDead() || !GeoData.getInstance().canSeeTarget(actor, obj))
					{
						continue;
					}
					try
					{
						actor.setTarget(getAttackTarget());
						dist = actor.calculateDistance(obj, false, false);
						dist2 = dist;
						range = sk.getCastRange() + actor.getTemplate().getCollisionRadius() + obj.getTemplate().getCollisionRadius();
						if (obj.isMoving())
						{
							dist2 = dist2 - 70;
						}
					}
					catch (NullPointerException e)
					{
						continue;
					}
					
					if ((obj.isPlayer()) || (obj.isSummon()))
					{
						if (dist2 <= range)
						{
							if (!getAttackTarget().isAffectedBySkill(sk.getId()))
							{
								return obj;
							}
						}
					}
				}
			}
			else if (positive)
			{
				double dist = 0;
				double dist2 = 0;
				int range = 0;
				for (Attackable targets : World.getInstance().getVisibleObjects(actor, Attackable.class, range))
				{
					if (targets.isDead() || !GeoData.getInstance().canSeeTarget(actor, targets))
					{
						continue;
					}
					
					if (targets.isInMyClan(actor))
					{
						continue;
					}
					
					try
					{
						actor.setTarget(getAttackTarget());
						dist = actor.calculateDistance(targets, false, false);
						dist2 = dist - actor.getTemplate().getCollisionRadius();
						range = sk.getCastRange() + actor.getTemplate().getCollisionRadius() + targets.getTemplate().getCollisionRadius();
						if (targets.isMoving())
						{
							dist2 = dist2 - 70;
						}
					}
					catch (NullPointerException e)
					{
						continue;
					}
					if (dist2 <= range)
					{
						if (!targets.isAffectedBySkill(sk.getId()))
						{
							return targets;
						}
					}
				}
			}
		}
		else
		{
			double dist = 0;
			double dist2 = 0;
			int range = 0;
			range = sk.getCastRange() + actor.getTemplate().getCollisionRadius() + getAttackTarget().getTemplate().getCollisionRadius();
			for (Creature obj : World.getInstance().getVisibleObjects(actor, Creature.class, range))
			{
				if (obj.isDead() || !GeoData.getInstance().canSeeTarget(actor, obj))
				{
					continue;
				}
				try
				{
					actor.setTarget(getAttackTarget());
					dist = actor.calculateDistance(obj, false, false);
					dist2 = dist - actor.getTemplate().getCollisionRadius();
					range = sk.getCastRange() + actor.getTemplate().getCollisionRadius() + obj.getTemplate().getCollisionRadius();
					if (obj.isMoving())
					{
						dist2 = dist2 - 70;
					}
				}
				catch (NullPointerException e)
				{
					continue;
				}
				
				if ((obj.isPlayer()) || (obj.isSummon()))
				{
					
					if (dist2 <= range)
					{
						if (getAttackTarget().getEffectList().getFirstEffect(L2EffectType.BUFF) != null)
						{
							return obj;
						}
					}
				}
			}
		}
		return null;
	}
	
	private Creature skillTargetReconsider(Skill sk)
	{
		double dist = 0;
		double dist2 = 0;
		int range = 0;
		Attackable actor = getActiveChar();
		if (actor.getHateList() != null)
		{
			for (Creature obj : actor.getHateList())
			{
				if ((obj == null) || !GeoData.getInstance().canSeeTarget(actor, obj) || obj.isDead())
				{
					continue;
				}
				try
				{
					actor.setTarget(getAttackTarget());
					dist = actor.calculateDistance(obj, false, false);
					dist2 = dist - actor.getTemplate().getCollisionRadius();
					range = sk.getCastRange() + actor.getTemplate().getCollisionRadius() + getAttackTarget().getTemplate().getCollisionRadius();
					// if(obj.isMoving())
					// dist2 = dist2 - 40;
				}
				catch (NullPointerException e)
				{
					continue;
				}
				if (dist2 <= range)
				{
					return obj;
				}
			}
		}
		
		if (!(actor instanceof L2GuardInstance))
		{
			for (WorldObject target : World.getInstance().getVisibleObjects(actor, WorldObject.class))
			{
				try
				{
					actor.setTarget(getAttackTarget());
					dist = actor.calculateDistance(target, false, false);
					dist2 = dist;
					range = sk.getCastRange() + actor.getTemplate().getCollisionRadius() + getAttackTarget().getTemplate().getCollisionRadius();
					// if(obj.isMoving())
					// dist2 = dist2 - 40;
				}
				catch (NullPointerException e)
				{
					continue;
				}
				Creature obj = null;
				if (target.isCreature())
				{
					obj = (Creature) target;
				}
				if ((obj == null) || !GeoData.getInstance().canSeeTarget(actor, obj) || (dist2 > range))
				{
					continue;
				}
				if (obj.isPlayer())
				{
					return obj;
					
				}
				if (obj.isAttackable())
				{
					if (actor.getTemplate().isChaos())
					{
						if (((Attackable) obj).isInMyClan(actor))
						{
							continue;
						}
						
						return obj;
					}
				}
				if (obj.isSummon())
				{
					return obj;
				}
			}
		}
		return null;
	}
	
	private void targetReconsider()
	{
		double dist = 0;
		double dist2 = 0;
		int range = 0;
		Attackable actor = getActiveChar();
		Creature MostHate = actor.getMostHated();
		if (actor.getHateList() != null)
		{
			for (Creature obj : actor.getHateList())
			{
				if ((obj == null) || !GeoData.getInstance().canSeeTarget(actor, obj) || obj.isDead() || (obj != MostHate) || (obj == actor))
				{
					continue;
				}
				try
				{
					dist = actor.calculateDistance(obj, false, false);
					dist2 = dist - actor.getTemplate().getCollisionRadius();
					range = actor.getPhysicalAttackRange() + actor.getTemplate().getCollisionRadius() + obj.getTemplate().getCollisionRadius();
					if (obj.isMoving())
					{
						dist2 = dist2 - 70;
					}
				}
				catch (NullPointerException e)
				{
					continue;
				}
				
				if (dist2 <= range)
				{
					if (MostHate != null)
					{
						actor.addDamageHate(obj, 0, actor.getHating(MostHate));
					}
					else
					{
						actor.addDamageHate(obj, 0, 2000);
					}
					actor.setTarget(obj);
					setAttackTarget(obj);
					return;
				}
			}
		}
		if (!(actor instanceof L2GuardInstance))
		{
			World.getInstance().forEachVisibleObject(actor, Creature.class, obj ->
			{
				if ((obj == null) || !GeoData.getInstance().canSeeTarget(actor, obj) || obj.isDead() || (obj != MostHate) || (obj == actor) || (obj == getAttackTarget()))
				{
					return;
				}
				if (obj.isPlayer())
				{
					if (MostHate != null)
					{
						actor.addDamageHate(obj, 0, actor.getHating(MostHate));
					}
					else
					{
						actor.addDamageHate(obj, 0, 2000);
					}
					actor.setTarget(obj);
					setAttackTarget(obj);
					
				}
				else if (obj.isAttackable())
				{
					if (actor.getTemplate().isChaos())
					{
						if (((Attackable) obj).isInMyClan(actor))
						{
							return;
						}
						
						if (MostHate != null)
						{
							actor.addDamageHate(obj, 0, actor.getHating(MostHate));
						}
						else
						{
							actor.addDamageHate(obj, 0, 2000);
						}
						actor.setTarget(obj);
						setAttackTarget(obj);
					}
				}
				else if (obj.isSummon())
				{
					if (MostHate != null)
					{
						actor.addDamageHate(obj, 0, actor.getHating(MostHate));
					}
					else
					{
						actor.addDamageHate(obj, 0, 2000);
					}
					actor.setTarget(obj);
					setAttackTarget(obj);
				}
			});
		}
	}
	
	private void aggroReconsider()
	{
		Attackable actor = getActiveChar();
		Creature MostHate = actor.getMostHated();
		if (actor.getHateList() != null)
		{
			
			int rand = Rnd.get(actor.getHateList().size());
			int count = 0;
			for (Creature obj : actor.getHateList())
			{
				if (count < rand)
				{
					count++;
					continue;
				}
				
				if ((obj == null) || !GeoData.getInstance().canSeeTarget(actor, obj) || obj.isDead() || (obj == getAttackTarget()) || (obj == actor))
				{
					continue;
				}
				
				try
				{
					actor.setTarget(getAttackTarget());
				}
				catch (NullPointerException e)
				{
					continue;
				}
				if (MostHate != null)
				{
					actor.addDamageHate(obj, 0, actor.getHating(MostHate));
				}
				else
				{
					actor.addDamageHate(obj, 0, 2000);
				}
				actor.setTarget(obj);
				setAttackTarget(obj);
				return;
			}
		}
		
		if (!(actor instanceof L2GuardInstance))
		{
			World.getInstance().forEachVisibleObject(actor, Creature.class, obj ->
			{
				if (!GeoData.getInstance().canSeeTarget(actor, obj) || obj.isDead() || (obj != MostHate) || (obj == actor))
				{
					return;
				}
				if (obj.isPlayer())
				{
					if ((MostHate != null) && !MostHate.isDead())
					{
						actor.addDamageHate(obj, 0, actor.getHating(MostHate));
					}
					else
					{
						actor.addDamageHate(obj, 0, 2000);
					}
					actor.setTarget(obj);
					setAttackTarget(obj);
				}
				else if (obj.isAttackable())
				{
					if (actor.getTemplate().isChaos())
					{
						if (((Attackable) obj).isInMyClan(actor))
						{
							return;
						}
						
						if (MostHate != null)
						{
							actor.addDamageHate(obj, 0, actor.getHating(MostHate));
						}
						else
						{
							actor.addDamageHate(obj, 0, 2000);
						}
						actor.setTarget(obj);
						setAttackTarget(obj);
					}
				}
				else if (obj.isSummon())
				{
					if (MostHate != null)
					{
						actor.addDamageHate(obj, 0, actor.getHating(MostHate));
					}
					else
					{
						actor.addDamageHate(obj, 0, 2000);
					}
					actor.setTarget(obj);
					setAttackTarget(obj);
				}
			});
		}
	}
	
	/**
	 * Manage AI thinking actions of a L2Attackable.
	 */
	@Override
	protected void onEvtThink()
	{
		// Check if the actor can't use skills and if a thinking action isn't already in progress
		if (_thinking || getActiveChar().isAllSkillsDisabled())
		{
			return;
		}
		
		// Start thinking action
		_thinking = true;
		
		try
		{
			// Manage AI thinks of a L2Attackable
			switch (getIntention())
			{
				case AI_INTENTION_ACTIVE:
					thinkActive();
					break;
				case AI_INTENTION_ATTACK:
					thinkAttack();
					break;
				case AI_INTENTION_CAST:
					thinkCast();
					break;
			}
		}
		catch (Exception e)
		{
			LOGGER.warn("{} -  onEvtThink() failed", this, e);
		}
		finally
		{
			// Stop thinking action
			_thinking = false;
		}
	}
	
	/**
	 * Launch actions corresponding to the Event Attacked.<br>
	 * <B><U> Actions</U> :</B>
	 * <ul>
	 * <li>Init the attack : Calculate the attack timeout, Set the _globalAggro to 0, Add the attacker to the actor _aggroList</li>
	 * <li>Set the L2Character movement type to run and send Server->Client packet ChangeMoveType to all others L2PcInstance</li>
	 * <li>Set the Intention to AI_INTENTION_ATTACK</li>
	 * </ul>
	 * @param attacker The L2Character that attacks the actor
	 */
	@Override
	protected void onEvtAttacked(Creature attacker)
	{
		Attackable me = getActiveChar();
		
		// Calculate the attack timeout
		_attackTimeout = MAX_ATTACK_TIMEOUT + GameTimeController.getInstance().getGameTicks();
		
		// Set the _globalAggro to 0 to permit attack even just after spawn
		if (_globalAggro < 0)
		{
			_globalAggro = 0;
		}
		
		// Add the attacker to the _aggroList of the actor
		me.addDamageHate(attacker, 0, 1);
		
		// Set the L2Character movement type to run and send Server->Client packet ChangeMoveType to all others L2PcInstance
		if (!me.isRunning())
		{
			me.setRunning();
		}
		
		// Set the Intention to AI_INTENTION_ATTACK
		if (getIntention() != AI_INTENTION_ATTACK)
		{
			setIntention(CtrlIntention.AI_INTENTION_ATTACK, attacker);
		}
		else if (me.getMostHated() != getAttackTarget())
		{
			setIntention(CtrlIntention.AI_INTENTION_ATTACK, attacker);
		}
		
		if (me.isMonster())
		{
			L2MonsterInstance master = (L2MonsterInstance) me;
			
			if (master.hasMinions())
			{
				master.getMinionList().onAssist(me, attacker);
			}
			
			master = master.getLeader();
			if ((master != null) && master.hasMinions())
			{
				master.getMinionList().onAssist(me, attacker);
			}
		}
		
		super.onEvtAttacked(attacker);
	}
	
	/**
	 * Launch actions corresponding to the Event Aggression.<br>
	 * <B><U> Actions</U> :</B>
	 * <ul>
	 * <li>Add the target to the actor _aggroList or update hate if already present</li>
	 * <li>Set the actor Intention to AI_INTENTION_ATTACK (if actor is L2GuardInstance check if it isn't too far from its home location)</li>
	 * </ul>
	 * @param aggro The value of hate to add to the actor against the target
	 */
	@Override
	protected void onEvtAggression(Creature target, int aggro)
	{
		final Attackable me = getActiveChar();
		if (me.isDead())
		{
			return;
		}
		
		if (target != null)
		{
			// Add the target to the actor _aggroList or update hate if already present
			me.addDamageHate(target, 0, aggro);
			
			// Set the actor AI Intention to AI_INTENTION_ATTACK
			if (getIntention() != CtrlIntention.AI_INTENTION_ATTACK)
			{
				// Set the L2Character movement type to run and send Server->Client packet ChangeMoveType to all others L2PcInstance
				if (!me.isRunning())
				{
					me.setRunning();
				}
				
				setIntention(CtrlIntention.AI_INTENTION_ATTACK, target);
			}
			
			if (me.isMonster())
			{
				L2MonsterInstance master = (L2MonsterInstance) me;
				
				if (master.hasMinions())
				{
					master.getMinionList().onAssist(me, target);
				}
				
				master = master.getLeader();
				if ((master != null) && master.hasMinions())
				{
					master.getMinionList().onAssist(me, target);
				}
			}
		}
	}
	
	@Override
	protected void onIntentionActive()
	{
		// Cancel attack timeout
		_attackTimeout = Integer.MAX_VALUE;
		super.onIntentionActive();
	}
	
	public void setGlobalAggro(int value)
	{
		_globalAggro = value;
	}
	
	/**
	 * @param TP The timepass to set.
	 */
	public void setTimepass(int TP)
	{
		timepass = TP;
	}
	
	/**
	 * @return Returns the timepass.
	 */
	public int getTimepass()
	{
		return timepass;
	}
	
	public Attackable getActiveChar()
	{
		return (Attackable) _actor;
	}
}
