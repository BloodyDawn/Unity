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

import static org.l2junity.gameserver.ai.CtrlIntention.AI_INTENTION_ACTIVE;
import static org.l2junity.gameserver.ai.CtrlIntention.AI_INTENTION_ATTACK;
import static org.l2junity.gameserver.ai.CtrlIntention.AI_INTENTION_FOLLOW;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.l2junity.Config;
import org.l2junity.commons.util.CommonUtil;
import org.l2junity.commons.util.EmptyQueue;
import org.l2junity.commons.util.Rnd;
import org.l2junity.gameserver.GameTimeController;
import org.l2junity.gameserver.GeoData;
import org.l2junity.gameserver.ThreadPoolManager;
import org.l2junity.gameserver.ai.AttackableAI;
import org.l2junity.gameserver.ai.CharacterAI;
import org.l2junity.gameserver.ai.CtrlEvent;
import org.l2junity.gameserver.ai.CtrlIntention;
import org.l2junity.gameserver.data.xml.impl.CategoryData;
import org.l2junity.gameserver.data.xml.impl.DoorData;
import org.l2junity.gameserver.datatables.ItemTable;
import org.l2junity.gameserver.enums.CategoryType;
import org.l2junity.gameserver.enums.InstanceType;
import org.l2junity.gameserver.enums.Race;
import org.l2junity.gameserver.enums.ShotType;
import org.l2junity.gameserver.enums.Team;
import org.l2junity.gameserver.enums.UserInfoType;
import org.l2junity.gameserver.idfactory.IdFactory;
import org.l2junity.gameserver.instancemanager.InstanceManager;
import org.l2junity.gameserver.instancemanager.MapRegionManager;
import org.l2junity.gameserver.instancemanager.ZoneManager;
import org.l2junity.gameserver.model.AccessLevel;
import org.l2junity.gameserver.model.CharEffectList;
import org.l2junity.gameserver.model.L2Clan;
import org.l2junity.gameserver.model.Location;
import org.l2junity.gameserver.model.Party;
import org.l2junity.gameserver.model.PcCondOverride;
import org.l2junity.gameserver.model.TeleportWhereType;
import org.l2junity.gameserver.model.TimeStamp;
import org.l2junity.gameserver.model.World;
import org.l2junity.gameserver.model.WorldObject;
import org.l2junity.gameserver.model.WorldRegion;
import org.l2junity.gameserver.model.actor.instance.L2PetInstance;
import org.l2junity.gameserver.model.actor.instance.PlayerInstance;
import org.l2junity.gameserver.model.actor.stat.CharStat;
import org.l2junity.gameserver.model.actor.status.CharStatus;
import org.l2junity.gameserver.model.actor.tasks.character.FlyToLocationTask;
import org.l2junity.gameserver.model.actor.tasks.character.HitTask;
import org.l2junity.gameserver.model.actor.tasks.character.MagicUseTask;
import org.l2junity.gameserver.model.actor.tasks.character.NotifyAITask;
import org.l2junity.gameserver.model.actor.tasks.character.QueuedMagicUseTask;
import org.l2junity.gameserver.model.actor.tasks.character.UsePotionTask;
import org.l2junity.gameserver.model.actor.templates.L2CharTemplate;
import org.l2junity.gameserver.model.actor.transform.Transform;
import org.l2junity.gameserver.model.actor.transform.TransformTemplate;
import org.l2junity.gameserver.model.effects.EffectFlag;
import org.l2junity.gameserver.model.effects.L2EffectType;
import org.l2junity.gameserver.model.entity.Instance;
import org.l2junity.gameserver.model.events.Containers;
import org.l2junity.gameserver.model.events.EventDispatcher;
import org.l2junity.gameserver.model.events.EventType;
import org.l2junity.gameserver.model.events.impl.character.OnCreatureAttack;
import org.l2junity.gameserver.model.events.impl.character.OnCreatureAttackAvoid;
import org.l2junity.gameserver.model.events.impl.character.OnCreatureAttacked;
import org.l2junity.gameserver.model.events.impl.character.OnCreatureDamageDealt;
import org.l2junity.gameserver.model.events.impl.character.OnCreatureDamageReceived;
import org.l2junity.gameserver.model.events.impl.character.OnCreatureKill;
import org.l2junity.gameserver.model.events.impl.character.OnCreatureSkillFinishCast;
import org.l2junity.gameserver.model.events.impl.character.OnCreatureSkillUse;
import org.l2junity.gameserver.model.events.impl.character.OnCreatureTeleported;
import org.l2junity.gameserver.model.events.impl.character.npc.OnNpcSkillSee;
import org.l2junity.gameserver.model.events.listeners.AbstractEventListener;
import org.l2junity.gameserver.model.events.returns.TerminateReturn;
import org.l2junity.gameserver.model.holders.InvulSkillHolder;
import org.l2junity.gameserver.model.holders.SkillHolder;
import org.l2junity.gameserver.model.holders.SkillUseHolder;
import org.l2junity.gameserver.model.interfaces.IDeletable;
import org.l2junity.gameserver.model.interfaces.ILocational;
import org.l2junity.gameserver.model.interfaces.ISkillsHolder;
import org.l2junity.gameserver.model.itemcontainer.Inventory;
import org.l2junity.gameserver.model.items.L2Item;
import org.l2junity.gameserver.model.items.Weapon;
import org.l2junity.gameserver.model.items.instance.ItemInstance;
import org.l2junity.gameserver.model.items.type.WeaponType;
import org.l2junity.gameserver.model.options.OptionsSkillHolder;
import org.l2junity.gameserver.model.options.OptionsSkillType;
import org.l2junity.gameserver.model.skills.AbnormalType;
import org.l2junity.gameserver.model.skills.AbnormalVisualEffect;
import org.l2junity.gameserver.model.skills.BuffInfo;
import org.l2junity.gameserver.model.skills.CommonSkill;
import org.l2junity.gameserver.model.skills.EffectScope;
import org.l2junity.gameserver.model.skills.Skill;
import org.l2junity.gameserver.model.skills.SkillChannelized;
import org.l2junity.gameserver.model.skills.SkillChannelizer;
import org.l2junity.gameserver.model.skills.targets.L2TargetType;
import org.l2junity.gameserver.model.stats.BaseStats;
import org.l2junity.gameserver.model.stats.Calculator;
import org.l2junity.gameserver.model.stats.Formulas;
import org.l2junity.gameserver.model.stats.Stats;
import org.l2junity.gameserver.model.stats.functions.AbstractFunction;
import org.l2junity.gameserver.model.zone.ZoneId;
import org.l2junity.gameserver.model.zone.ZoneRegion;
import org.l2junity.gameserver.network.client.recv.RequestActionUse;
import org.l2junity.gameserver.network.client.send.ActionFailed;
import org.l2junity.gameserver.network.client.send.Attack;
import org.l2junity.gameserver.network.client.send.ChangeMoveType;
import org.l2junity.gameserver.network.client.send.ChangeWaitType;
import org.l2junity.gameserver.network.client.send.ExRotation;
import org.l2junity.gameserver.network.client.send.ExTeleportToLocationActivate;
import org.l2junity.gameserver.network.client.send.IClientOutgoingPacket;
import org.l2junity.gameserver.network.client.send.MagicSkillCanceld;
import org.l2junity.gameserver.network.client.send.MagicSkillLaunched;
import org.l2junity.gameserver.network.client.send.MagicSkillUse;
import org.l2junity.gameserver.network.client.send.MoveToLocation;
import org.l2junity.gameserver.network.client.send.NpcInfo;
import org.l2junity.gameserver.network.client.send.Revive;
import org.l2junity.gameserver.network.client.send.ServerObjectInfo;
import org.l2junity.gameserver.network.client.send.SetupGauge;
import org.l2junity.gameserver.network.client.send.SocialAction;
import org.l2junity.gameserver.network.client.send.StatusUpdate;
import org.l2junity.gameserver.network.client.send.StopMove;
import org.l2junity.gameserver.network.client.send.SystemMessage;
import org.l2junity.gameserver.network.client.send.TeleportToLocation;
import org.l2junity.gameserver.network.client.send.UserInfo;
import org.l2junity.gameserver.network.client.send.string.SystemMessageId;
import org.l2junity.gameserver.pathfinding.AbstractNodeLoc;
import org.l2junity.gameserver.pathfinding.PathFinding;
import org.l2junity.gameserver.taskmanager.AttackStanceTaskManager;
import org.l2junity.gameserver.util.Util;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Mother class of all character objects of the world (PC, NPC...)<br>
 * L2Character:<br>
 * <ul>
 * <li>L2DoorInstance</li>
 * <li>L2Playable</li>
 * <li>L2Npc</li>
 * <li>L2StaticObjectInstance</li>
 * <li>L2Trap</li>
 * <li>L2Vehicle</li>
 * </ul>
 * <br>
 * <b>Concept of L2CharTemplate:</b><br>
 * Each L2Character owns generic and static properties (ex : all Keltir have the same number of HP...).<br>
 * All of those properties are stored in a different template for each type of L2Character.<br>
 * Each template is loaded once in the server cache memory (reduce memory use).<br>
 * When a new instance of L2Character is spawned, server just create a link between the instance and the template.<br>
 * This link is stored in {@link #_template}
 * @version $Revision: 1.53.2.45.2.34 $ $Date: 2005/04/11 10:06:08 $
 */
public abstract class Creature extends WorldObject implements ISkillsHolder, IDeletable
{
	public static final Logger _log = LoggerFactory.getLogger(Creature.class);
	
	private volatile Set<WeakReference<Creature>> _attackByList;
	private volatile boolean _isCastingNow = false;
	private volatile boolean _isCastingSimultaneouslyNow = false;
	private Skill _lastSkillCast;
	private Skill _lastSimultaneousSkillCast;
	
	private boolean _isDead = false;
	private boolean _isImmobilized = false;
	private boolean _isOverloaded = false; // the char is carrying too much
	private boolean _isParalyzed = false;
	private boolean _isPendingRevive = false;
	private boolean _isRunning = false;
	private boolean _isNoRndWalk = false; // Is no random walk
	protected boolean _showSummonAnimation = false;
	protected boolean _isTeleporting = false;
	private boolean _isInvul = false;
	private boolean _isMortal = true; // Char will die when HP decreased to 0
	private boolean _isFlying = false;
	
	private CharStat _stat;
	private CharStatus _status;
	private L2CharTemplate _template; // The link on the L2CharTemplate object containing generic and static properties of this L2Character type (ex : Max HP, Speed...)
	private String _title;
	
	public static final double MAX_HP_BAR_PX = 352.0;
	
	private double _hpUpdateIncCheck = .0;
	private double _hpUpdateDecCheck = .0;
	private double _hpUpdateInterval = .0;
	
	/** Table of Calculators containing all used calculator */
	private Calculator[] _calculators;
	/** Map containing all skills of this character. */
	private final Map<Integer, Skill> _skills = new ConcurrentSkipListMap<>();
	/** Map containing the skill reuse time stamps. */
	private volatile Map<Integer, TimeStamp> _reuseTimeStampsSkills = null;
	/** Map containing the item reuse time stamps. */
	private volatile Map<Integer, TimeStamp> _reuseTimeStampsItems = null;
	/** Map containing all the disabled skills. */
	private volatile Map<Integer, Long> _disabledSkills = null;
	private boolean _allSkillsDisabled;
	
	private final byte[] _zones = new byte[ZoneId.getZoneCount()];
	protected byte _zoneValidateCounter = 4;
	
	private Creature _debugger = null;
	
	private final ReentrantLock _teleportLock = new ReentrantLock();
	private final Object _attackLock = new Object();
	
	private Team _team = Team.NONE;
	
	protected long _exceptions = 0L;
	
	private boolean _lethalable = true;
	
	private volatile Map<Integer, OptionsSkillHolder> _triggerSkills;
	
	private volatile Map<Integer, InvulSkillHolder> _invulAgainst;
	/** Creatures effect list. */
	private final CharEffectList _effectList = new CharEffectList(this);
	/** The character that summons this character. */
	private Creature _summoner = null;
	
	/** Map of summoned NPCs by this creature. */
	private volatile Map<Integer, Npc> _summonedNpcs = null;
	
	private SkillChannelizer _channelizer = null;
	
	private SkillChannelized _channelized = null;
	
	private volatile Set<AbnormalVisualEffect> _abnormalVisualEffects;
	private volatile Set<AbnormalVisualEffect> _currentAbnormalVisualEffects;
	
	/** Movement data of this L2Character */
	protected MoveData _move;
	
	/** This creature's target. */
	private WorldObject _target;
	
	// set by the start of attack, in game ticks
	private volatile long _attackEndTime;
	private int _disableBowAttackEndTime;
	private int _disableCrossBowAttackEndTime;
	
	private int _castInterruptTime;
	
	/** Table of calculators containing all standard NPC calculator (ex : ACCURACY_COMBAT, EVASION_RATE) */
	private static final Calculator[] NPC_STD_CALCULATOR = Formulas.getStdNPCCalculators();
	
	private volatile CharacterAI _ai = null;
	
	/** Future Skill Cast */
	protected Future<?> _skillCast;
	protected Future<?> _skillCast2;
	
	private final AtomicInteger _blockedDebuffTimes = new AtomicInteger();
	
	private final Map<Integer, Integer> _knownRelations = new ConcurrentHashMap<>();
	
	/**
	 * Creates a creature.
	 * @param template the creature template
	 */
	public Creature(L2CharTemplate template)
	{
		this(IdFactory.getInstance().getNextId(), template);
	}
	
	/**
	 * Constructor of L2Character.<br>
	 * <B><U>Concept</U>:</B><br>
	 * Each L2Character owns generic and static properties (ex : all Keltir have the same number of HP...).<br>
	 * All of those properties are stored in a different template for each type of L2Character. Each template is loaded once in the server cache memory (reduce memory use).<br>
	 * When a new instance of L2Character is spawned, server just create a link between the instance and the template This link is stored in <B>_template</B><br>
	 * <B><U> Actions</U>:</B>
	 * <ul>
	 * <li>Set the _template of the L2Character</li>
	 * <li>Set _overloaded to false (the character can take more items)</li>
	 * <li>If L2Character is a L2NPCInstance, copy skills from template to object</li>
	 * <li>If L2Character is a L2NPCInstance, link _calculators to NPC_STD_CALCULATOR</li>
	 * <li>If L2Character is NOT a L2NPCInstance, create an empty _skills slot</li>
	 * <li>If L2Character is a L2PcInstance or L2Summon, copy basic Calculator set to object</li>
	 * </ul>
	 * @param objectId Identifier of the object to initialized
	 * @param template The L2CharTemplate to apply to the object
	 */
	public Creature(int objectId, L2CharTemplate template)
	{
		super(objectId);
		if (template == null)
		{
			throw new NullPointerException("Template is null!");
		}
		
		setInstanceType(InstanceType.L2Character);
		initCharStat();
		initCharStatus();
		
		// Set its template to the new L2Character
		_template = template;
		
		if (isNpc())
		{
			// Copy the Standard Calculators of the L2NPCInstance in _calculators
			_calculators = NPC_STD_CALCULATOR;
			
			// Copy the skills of the L2NPCInstance from its template to the L2Character Instance
			// The skills list can be affected by spell effects so it's necessary to make a copy
			// to avoid that a spell affecting a L2NpcInstance, affects others L2NPCInstance of the same type too.
			for (Skill skill : template.getSkills().values())
			{
				addSkill(skill);
			}
		}
		else
		{
			// If L2Character is a L2PcInstance or a L2Summon, create the basic calculator set
			_calculators = new Calculator[Stats.NUM_STATS];
			
			if (isSummon())
			{
				// Copy the skills of the L2Summon from its template to the L2Character Instance
				// The skills list can be affected by spell effects so it's necessary to make a copy
				// to avoid that a spell affecting a L2Summon, affects others L2Summon of the same type too.
				for (Skill skill : template.getSkills().values())
				{
					addSkill(skill);
				}
			}
			
			Formulas.addFuncsToNewCharacter(this);
		}
		
		setIsInvul(true);
	}
	
	public final CharEffectList getEffectList()
	{
		return _effectList;
	}
	
	/**
	 * Verify if this character is under debug.
	 * @return {@code true} if this character is under debug, {@code false} otherwise
	 */
	public boolean isDebug()
	{
		return _debugger != null;
	}
	
	/**
	 * Sets character instance, to which debug packets will be send.
	 * @param debugger the character debugging this character
	 */
	public void setDebug(Creature debugger)
	{
		_debugger = debugger;
	}
	
	/**
	 * Send debug packet.
	 * @param pkt
	 */
	public void sendDebugPacket(IClientOutgoingPacket pkt)
	{
		if (_debugger != null)
		{
			_debugger.sendPacket(pkt);
		}
	}
	
	/**
	 * Send debug text string
	 * @param msg
	 */
	public void sendDebugMessage(String msg)
	{
		if (_debugger != null)
		{
			_debugger.sendMessage(msg);
		}
	}
	
	/**
	 * @return character inventory, default null, overridden in L2Playable types and in L2NPcInstance
	 */
	public Inventory getInventory()
	{
		return null;
	}
	
	public boolean destroyItemByItemId(String process, int itemId, long count, WorldObject reference, boolean sendMessage)
	{
		// Default: NPCs consume virtual items for their skills
		// TODO: should be logged if even happens.. should be false
		return true;
	}
	
	public boolean destroyItem(String process, int objectId, long count, WorldObject reference, boolean sendMessage)
	{
		// Default: NPCs consume virtual items for their skills
		// TODO: should be logged if even happens.. should be false
		return true;
	}
	
	/**
	 * Check if the character is in the given zone Id.
	 * @param zone the zone Id to check
	 * @return {code true} if the character is in that zone
	 */
	@Override
	public final boolean isInsideZone(ZoneId zone)
	{
		Instance instance = InstanceManager.getInstance().getInstance(getInstanceId());
		switch (zone)
		{
			case PVP:
				if ((instance != null) && instance.isPvPInstance())
				{
					return true;
				}
				return (_zones[ZoneId.PVP.ordinal()] > 0) && (_zones[ZoneId.PEACE.ordinal()] == 0);
			case PEACE:
				if ((instance != null) && instance.isPvPInstance())
				{
					return false;
				}
		}
		return _zones[zone.ordinal()] > 0;
	}
	
	/**
	 * @param zone
	 * @param state
	 */
	public final void setInsideZone(ZoneId zone, final boolean state)
	{
		synchronized (_zones)
		{
			if (state)
			{
				_zones[zone.ordinal()]++;
			}
			else if (_zones[zone.ordinal()] > 0)
			{
				_zones[zone.ordinal()]--;
			}
		}
	}
	
	/**
	 * This will return true if the player is transformed,<br>
	 * but if the player is not transformed it will return false.
	 * @return transformation status
	 */
	public boolean isTransformed()
	{
		return false;
	}
	
	public Transform getTransformation()
	{
		return null;
	}
	
	/**
	 * This will untransform a player if they are an instance of L2Pcinstance and if they are transformed.
	 */
	public void untransform()
	{
		// Just a place holder
	}
	
	/**
	 * This will return true if the player is GM,<br>
	 * but if the player is not GM it will return false.
	 * @return GM status
	 */
	public boolean isGM()
	{
		return false;
	}
	
	/**
	 * Overridden in L2PcInstance.
	 * @return the access level.
	 */
	public AccessLevel getAccessLevel()
	{
		return null;
	}
	
	protected void initCharStatusUpdateValues()
	{
		_hpUpdateIncCheck = getMaxHp();
		_hpUpdateInterval = _hpUpdateIncCheck / MAX_HP_BAR_PX;
		_hpUpdateDecCheck = _hpUpdateIncCheck - _hpUpdateInterval;
	}
	
	/**
	 * Remove the L2Character from the world when the decay task is launched.<br>
	 * <FONT COLOR=#FF0000><B> <U>Caution</U> : This method DOESN'T REMOVE the object from _allObjects of L2World </B></FONT><BR>
	 * <FONT COLOR=#FF0000><B> <U>Caution</U> : This method DOESN'T SEND Server->Client packets to players</B></FONT>
	 */
	public void onDecay()
	{
		decayMe();
		ZoneManager.getInstance().getRegion(this).removeFromZones(this);
		
		// Removes itself from the summoned list.
		if ((getSummoner() != null))
		{
			getSummoner().removeSummonedNpc(getObjectId());
		}
	}
	
	@Override
	public void onSpawn()
	{
		super.onSpawn();
		revalidateZone(true);
	}
	
	public void onTeleported()
	{
		if (!_teleportLock.tryLock())
		{
			return;
		}
		try
		{
			if (!isTeleporting())
			{
				return;
			}
			spawnMe(getX(), getY(), getZ());
			setIsTeleporting(false);
			EventDispatcher.getInstance().notifyEventAsync(new OnCreatureTeleported(this), this);
		}
		finally
		{
			_teleportLock.unlock();
		}
	}
	
	/**
	 * Add L2Character instance that is attacking to the attacker list.
	 * @param player The L2Character that attacks this one
	 */
	public void addAttackerToAttackByList(Creature player)
	{
		// DS: moved to L2Attackable
	}
	
	/**
	 * Send a packet to the L2Character AND to all L2PcInstance in the _KnownPlayers of the L2Character.<br>
	 * <B><U>Concept</U>:</B><br>
	 * L2PcInstance in the detection area of the L2Character are identified in <B>_knownPlayers</B>.<br>
	 * In order to inform other players of state modification on the L2Character, server just need to go through _knownPlayers to send Server->Client Packet
	 * @param mov
	 */
	public void broadcastPacket(IClientOutgoingPacket mov)
	{
		World.getInstance().forEachVisibleObject(this, PlayerInstance.class, player ->
		{
			if (isVisibleFor(player))
			{
				player.sendPacket(mov);
			}
		});
	}
	
	/**
	 * Send a packet to the L2Character AND to all L2PcInstance in the radius (max knownlist radius) from the L2Character.<br>
	 * <B><U>Concept</U>:</B><br>
	 * L2PcInstance in the detection area of the L2Character are identified in <B>_knownPlayers</B>.<br>
	 * In order to inform other players of state modification on the L2Character, server just need to go through _knownPlayers to send Server->Client Packet
	 * @param mov
	 * @param radiusInKnownlist
	 */
	public void broadcastPacket(IClientOutgoingPacket mov, int radiusInKnownlist)
	{
		World.getInstance().forEachVisibleObjectInRange(this, PlayerInstance.class, radiusInKnownlist, player ->
		{
			if (isVisibleFor(player))
			{
				player.sendPacket(mov);
			}
		});
	}
	
	/**
	 * @return true if hp update should be done, false if not
	 */
	protected boolean needHpUpdate()
	{
		double currentHp = getCurrentHp();
		double maxHp = getMaxHp();
		
		if ((currentHp <= 1.0) || (maxHp < MAX_HP_BAR_PX))
		{
			return true;
		}
		
		if ((currentHp < _hpUpdateDecCheck) || (Math.abs(currentHp - _hpUpdateDecCheck) <= 1e-6) || (currentHp > _hpUpdateIncCheck) || (Math.abs(currentHp - _hpUpdateIncCheck) <= 1e-6))
		{
			if (Math.abs(currentHp - maxHp) <= 1e-6)
			{
				_hpUpdateIncCheck = currentHp + 1;
				_hpUpdateDecCheck = currentHp - _hpUpdateInterval;
			}
			else
			{
				double doubleMulti = currentHp / _hpUpdateInterval;
				int intMulti = (int) doubleMulti;
				
				_hpUpdateDecCheck = _hpUpdateInterval * (doubleMulti < intMulti ? intMulti-- : intMulti);
				_hpUpdateIncCheck = _hpUpdateDecCheck + _hpUpdateInterval;
			}
			
			return true;
		}
		
		return false;
	}
	
	/**
	 * Send the Server->Client packet StatusUpdate with current HP and MP to all other L2PcInstance to inform.<br>
	 * <B><U>Actions</U>:</B>
	 * <ul>
	 * <li>Create the Server->Client packet StatusUpdate with current HP and MP</li>
	 * <li>Send the Server->Client packet StatusUpdate with current HP and MP to all L2Character called _statusListener that must be informed of HP/MP updates of this L2Character</li>
	 * </ul>
	 * <FONT COLOR=#FF0000><B><U>Caution</U>: This method DOESN'T SEND CP information</B></FONT>
	 */
	public void broadcastStatusUpdate()
	{
		if (getStatus().getStatusListener().isEmpty() || !needHpUpdate())
		{
			return;
		}
		
		// Create the Server->Client packet StatusUpdate with current HP
		StatusUpdate su = new StatusUpdate(this);
		su.addAttribute(StatusUpdate.MAX_HP, getMaxHp());
		su.addAttribute(StatusUpdate.CUR_HP, (int) getCurrentHp());
		
		// Go through the StatusListener
		// Send the Server->Client packet StatusUpdate with current HP and MP
		for (Creature temp : getStatus().getStatusListener())
		{
			if (temp != null)
			{
				temp.sendPacket(su);
			}
		}
	}
	
	/**
	 * @param text
	 */
	public void sendMessage(String text)
	{
		// default implementation
	}
	
	/**
	 * Teleport a L2Character and its pet if necessary.<br>
	 * <B><U>Actions</U>:</B>
	 * <ul>
	 * <li>Stop the movement of the L2Character</li>
	 * <li>Set the x,y,z position of the L2Object and if necessary modify its _worldRegion</li>
	 * <li>Send a Server->Client packet TeleportToLocationt to the L2Character AND to all L2PcInstance in its _KnownPlayers</li>
	 * <li>Modify the position of the pet if necessary</li>
	 * </ul>
	 * @param x
	 * @param y
	 * @param z
	 * @param heading
	 * @param instanceId
	 * @param randomOffset
	 */
	public void teleToLocation(int x, int y, int z, int heading, int instanceId, int randomOffset)
	{
		setInstanceId(instanceId);
		
		if (_isPendingRevive)
		{
			doRevive();
		}
		
		stopMove(null);
		abortAttack();
		abortCast();
		
		setIsTeleporting(true);
		setTarget(null);
		
		getAI().setIntention(CtrlIntention.AI_INTENTION_ACTIVE);
		
		if (Config.OFFSET_ON_TELEPORT_ENABLED && (randomOffset > 0))
		{
			x += Rnd.get(-randomOffset, randomOffset);
			y += Rnd.get(-randomOffset, randomOffset);
		}
		
		z += 5;
		
		// Send a Server->Client packet TeleportToLocationt to the L2Character AND to all L2PcInstance in the _KnownPlayers of the L2Character
		broadcastPacket(new TeleportToLocation(this, x, y, z, heading));
		sendPacket(new ExTeleportToLocationActivate(this));
		
		// remove the object from its old location
		decayMe();
		
		// Set the x,y,z position of the L2Object and if necessary modify its _worldRegion
		setXYZ(x, y, z);
		
		// temporary fix for heading on teleports
		if (heading != 0)
		{
			setHeading(heading);
		}
		
		// allow recall of the detached characters
		if (!isPlayer() || ((getActingPlayer().getClient() != null) && getActingPlayer().getClient().isDetached()))
		{
			onTeleported();
		}
		
		revalidateZone(true);
	}
	
	public void teleToLocation(int x, int y, int z, int heading, int instanceId, boolean randomOffset)
	{
		teleToLocation(x, y, z, heading, instanceId, (randomOffset) ? Config.MAX_OFFSET_ON_TELEPORT : 0);
	}
	
	public void teleToLocation(int x, int y, int z, int heading, int instanceId)
	{
		teleToLocation(x, y, z, heading, instanceId, 0);
	}
	
	public void teleToLocation(int x, int y, int z, int heading, boolean randomOffset)
	{
		teleToLocation(x, y, z, heading, -1, (randomOffset) ? Config.MAX_OFFSET_ON_TELEPORT : 0);
	}
	
	public void teleToLocation(int x, int y, int z, int heading)
	{
		teleToLocation(x, y, z, heading, -1, 0);
	}
	
	public void teleToLocation(int x, int y, int z, boolean randomOffset)
	{
		teleToLocation(x, y, z, 0, -1, (randomOffset) ? Config.MAX_OFFSET_ON_TELEPORT : 0);
	}
	
	public void teleToLocation(int x, int y, int z)
	{
		teleToLocation(x, y, z, 0, -1, 0);
	}
	
	public void teleToLocation(ILocational loc, int randomOffset)
	{
		teleToLocation(loc.getX(), loc.getY(), loc.getZ(), loc.getHeading(), loc.getInstanceId(), randomOffset);
	}
	
	public void teleToLocation(ILocational loc, int instanceId, int randomOffset)
	{
		teleToLocation(loc.getX(), loc.getY(), loc.getZ(), loc.getHeading(), instanceId, randomOffset);
	}
	
	public void teleToLocation(ILocational loc, boolean randomOffset)
	{
		teleToLocation(loc.getX(), loc.getY(), loc.getZ(), loc.getHeading(), loc.getInstanceId(), (randomOffset) ? Config.MAX_OFFSET_ON_TELEPORT : 0);
	}
	
	public void teleToLocation(ILocational loc)
	{
		teleToLocation(loc.getX(), loc.getY(), loc.getZ(), loc.getHeading(), loc.getInstanceId(), 0);
	}
	
	public void teleToLocation(TeleportWhereType teleportWhere)
	{
		teleToLocation(MapRegionManager.getInstance().getTeleToLocation(this, teleportWhere), true);
	}
	
	/**
	 * Launch a physical attack against a target (Simple, Bow, Pole or Dual).<br>
	 * <B><U>Actions</U>:</B>
	 * <ul>
	 * <li>Get the active weapon (always equipped in the right hand)</li>
	 * <li>If weapon is a bow, check for arrows, MP and bow re-use delay (if necessary, equip the L2PcInstance with arrows in left hand)</li>
	 * <li>If weapon is a bow, consume MP and set the new period of bow non re-use</li>
	 * <li>Get the Attack Speed of the L2Character (delay (in milliseconds) before next attack)</li>
	 * <li>Select the type of attack to start (Simple, Bow, Pole or Dual) and verify if SoulShot are charged then start calculation</li>
	 * <li>If the Server->Client packet Attack contains at least 1 hit, send the Server->Client packet Attack to the L2Character AND to all L2PcInstance in the _KnownPlayers of the L2Character</li>
	 * <li>Notify AI with EVT_READY_TO_ACT</li>
	 * </ul>
	 * @param target The L2Character targeted
	 */
	public void doAttack(Creature target)
	{
		synchronized (_attackLock)
		{
			if ((target == null) || isAttackingDisabled())
			{
				return;
			}
			
			// Notify to scripts
			final TerminateReturn attackReturn = EventDispatcher.getInstance().notifyEvent(new OnCreatureAttack(this, target), this, TerminateReturn.class);
			if ((attackReturn != null) && attackReturn.terminate())
			{
				getAI().setIntention(CtrlIntention.AI_INTENTION_ACTIVE);
				sendPacket(ActionFailed.STATIC_PACKET);
				return;
			}
			
			// Notify to scripts
			final TerminateReturn attackedReturn = EventDispatcher.getInstance().notifyEvent(new OnCreatureAttacked(this, target), target, TerminateReturn.class);
			if ((attackedReturn != null) && attackedReturn.terminate())
			{
				getAI().setIntention(CtrlIntention.AI_INTENTION_ACTIVE);
				sendPacket(ActionFailed.STATIC_PACKET);
				return;
			}
			
			if (!isAlikeDead())
			{
				if ((isNpc() && target.isAlikeDead()) || !isInSurroundingRegion(target))
				{
					getAI().setIntention(CtrlIntention.AI_INTENTION_ACTIVE);
					sendPacket(ActionFailed.STATIC_PACKET);
					return;
				}
				else if (isPlayer())
				{
					if (target.isDead())
					{
						getAI().setIntention(CtrlIntention.AI_INTENTION_ACTIVE);
						sendPacket(ActionFailed.STATIC_PACKET);
						return;
					}
					
					final PlayerInstance actor = getActingPlayer();
					if (actor.isTransformed() && !actor.getTransformation().canAttack())
					{
						sendPacket(ActionFailed.STATIC_PACKET);
						return;
					}
				}
			}
			
			// Check if attacker's weapon can attack
			if (getActiveWeaponItem() != null)
			{
				Weapon wpn = getActiveWeaponItem();
				if (!wpn.isAttackWeapon() && !isGM())
				{
					if (wpn.getItemType() == WeaponType.FISHINGROD)
					{
						sendPacket(SystemMessageId.YOU_LOOK_ODDLY_AT_THE_FISHING_POLE_IN_DISBELIEF_AND_REALIZE_THAT_YOU_CAN_T_ATTACK_ANYTHING_WITH_THIS);
					}
					else
					{
						sendPacket(SystemMessageId.THAT_WEAPON_CANNOT_PERFORM_ANY_ATTACKS);
					}
					sendPacket(ActionFailed.STATIC_PACKET);
					return;
				}
			}
			
			if (getActingPlayer() != null)
			{
				if (getActingPlayer().inObserverMode())
				{
					sendPacket(SystemMessageId.OBSERVERS_CANNOT_PARTICIPATE);
					sendPacket(ActionFailed.STATIC_PACKET);
					return;
				}
				
				else if ((target.getActingPlayer() != null) && (getActingPlayer().getSiegeState() > 0) && isInsideZone(ZoneId.SIEGE) && (target.getActingPlayer().getSiegeState() == getActingPlayer().getSiegeState()) && (target.getActingPlayer() != this) && (target.getActingPlayer().getSiegeSide() == getActingPlayer().getSiegeSide()))
				{
					sendPacket(SystemMessageId.FORCE_ATTACK_IS_IMPOSSIBLE_AGAINST_A_TEMPORARY_ALLIED_MEMBER_DURING_A_SIEGE);
					sendPacket(ActionFailed.STATIC_PACKET);
					return;
				}
				
				// Checking if target has moved to peace zone
				else if (target.isInsidePeaceZone(getActingPlayer()))
				{
					getAI().setIntention(CtrlIntention.AI_INTENTION_ACTIVE);
					sendPacket(ActionFailed.STATIC_PACKET);
					return;
				}
			}
			else if (isInsidePeaceZone(this, target))
			{
				getAI().setIntention(CtrlIntention.AI_INTENTION_ACTIVE);
				sendPacket(ActionFailed.STATIC_PACKET);
				return;
			}
			
			stopEffectsOnAction();
			
			// Get the active weapon item corresponding to the active weapon instance (always equipped in the right hand)
			Weapon weaponItem = getActiveWeaponItem();
			
			// GeoData Los Check here (or dz > 1000)
			if (!GeoData.getInstance().canSeeTarget(this, target))
			{
				sendPacket(SystemMessageId.CANNOT_SEE_TARGET);
				getAI().setIntention(CtrlIntention.AI_INTENTION_ACTIVE);
				sendPacket(ActionFailed.STATIC_PACKET);
				return;
			}
			
			// BOW and CROSSBOW checks
			if ((weaponItem != null) && !isTransformed())
			{
				if (weaponItem.getItemType() == WeaponType.BOW)
				{
					// Check for arrows and MP
					if (isPlayer())
					{
						// Checking if target has moved to peace zone - only for player-bow attacks at the moment
						// Other melee is checked in movement code and for offensive spells a check is done every time
						if (target.isInsidePeaceZone(getActingPlayer()))
						{
							getAI().setIntention(CtrlIntention.AI_INTENTION_ACTIVE);
							sendPacket(ActionFailed.STATIC_PACKET);
							return;
						}
						
						// Equip arrows needed in left hand and send a Server->Client packet ItemList to the L2PcInstance then return True
						if (!checkAndEquipArrows())
						{
							// Cancel the action because the L2PcInstance have no arrow
							getAI().setIntention(CtrlIntention.AI_INTENTION_IDLE);
							sendPacket(ActionFailed.STATIC_PACKET);
							sendPacket(SystemMessageId.YOU_HAVE_RUN_OUT_OF_ARROWS);
							return;
						}
						
						// Verify if the bow can be use
						if (_disableBowAttackEndTime <= GameTimeController.getInstance().getGameTicks())
						{
							// Verify if L2PcInstance owns enough MP
							int mpConsume = weaponItem.getMpConsume();
							if ((weaponItem.getReducedMpConsume() > 0) && (Rnd.get(100) < weaponItem.getReducedMpConsumeChance()))
							{
								mpConsume = weaponItem.getReducedMpConsume();
							}
							mpConsume = (int) calcStat(Stats.BOW_MP_CONSUME_RATE, mpConsume, null, null);
							
							if (getCurrentMp() < mpConsume)
							{
								// If L2PcInstance doesn't have enough MP, stop the attack
								ThreadPoolManager.getInstance().scheduleAi(new NotifyAITask(this, CtrlEvent.EVT_READY_TO_ACT), 1000);
								sendPacket(SystemMessageId.NOT_ENOUGH_MP);
								sendPacket(ActionFailed.STATIC_PACKET);
								return;
							}
							
							// If L2PcInstance have enough MP, the bow consumes it
							if (mpConsume > 0)
							{
								getStatus().reduceMp(mpConsume);
							}
							
							// Set the period of bow no re-use
							_disableBowAttackEndTime = (5 * GameTimeController.TICKS_PER_SECOND) + GameTimeController.getInstance().getGameTicks();
						}
						else
						{
							// Cancel the action because the bow can't be re-use at this moment
							ThreadPoolManager.getInstance().scheduleAi(new NotifyAITask(this, CtrlEvent.EVT_READY_TO_ACT), 1000);
							sendPacket(ActionFailed.STATIC_PACKET);
							return;
						}
					}
				}
				if ((weaponItem.getItemType() == WeaponType.CROSSBOW) || (weaponItem.getItemType() == WeaponType.TWOHANDCROSSBOW))
				{
					// Check for bolts
					if (isPlayer())
					{
						// Checking if target has moved to peace zone - only for player-crossbow attacks at the moment
						// Other melee is checked in movement code and for offensive spells a check is done every time
						if (target.isInsidePeaceZone(getActingPlayer()))
						{
							getAI().setIntention(CtrlIntention.AI_INTENTION_ACTIVE);
							sendPacket(ActionFailed.STATIC_PACKET);
							return;
						}
						
						// Equip bolts needed in left hand and send a Server->Client packet ItemList to the L2PcINstance then return True
						if (!checkAndEquipBolts())
						{
							// Cancel the action because the L2PcInstance have no arrow
							getAI().setIntention(CtrlIntention.AI_INTENTION_IDLE);
							sendPacket(ActionFailed.STATIC_PACKET);
							sendPacket(SystemMessageId.NOT_ENOUGH_BOLTS);
							return;
						}
						
						// Verify if the crossbow can be use
						if (_disableCrossBowAttackEndTime <= GameTimeController.getInstance().getGameTicks())
						{
							// Verify if L2PcInstance owns enough MP
							int mpConsume = weaponItem.getMpConsume();
							if ((weaponItem.getReducedMpConsume() > 0) && (Rnd.get(100) < weaponItem.getReducedMpConsumeChance()))
							{
								mpConsume = weaponItem.getReducedMpConsume();
							}
							mpConsume = (int) calcStat(Stats.BOW_MP_CONSUME_RATE, mpConsume, null, null);
							
							if (getCurrentMp() < mpConsume)
							{
								// If L2PcInstance doesn't have enough MP, stop the attack
								ThreadPoolManager.getInstance().scheduleAi(new NotifyAITask(this, CtrlEvent.EVT_READY_TO_ACT), 1000);
								sendPacket(SystemMessageId.NOT_ENOUGH_MP);
								sendPacket(ActionFailed.STATIC_PACKET);
								return;
							}
							
							// If L2PcInstance have enough MP, the bow consumes it
							if (mpConsume > 0)
							{
								getStatus().reduceMp(mpConsume);
							}
							
							// Set the period of crossbow no re-use
							_disableCrossBowAttackEndTime = (5 * GameTimeController.TICKS_PER_SECOND) + GameTimeController.getInstance().getGameTicks();
						}
						else
						{
							// Cancel the action because the crossbow can't be re-use at this moment
							ThreadPoolManager.getInstance().scheduleAi(new NotifyAITask(this, CtrlEvent.EVT_READY_TO_ACT), 1000);
							sendPacket(ActionFailed.STATIC_PACKET);
							return;
						}
					}
					else if (isNpc())
					{
						if (_disableCrossBowAttackEndTime > GameTimeController.getInstance().getGameTicks())
						{
							return;
						}
					}
				}
			}
			
			// Reduce the current CP if TIREDNESS configuration is activated
			if (Config.ALT_GAME_TIREDNESS)
			{
				setCurrentCp(getCurrentCp() - 10);
			}
			
			// Verify if soulshots are charged.
			final boolean wasSSCharged = isChargedShot(ShotType.SOULSHOTS);
			// Get the Attack Speed of the L2Character (delay (in milliseconds) before next attack)
			final int timeAtk = calculateTimeBetweenAttacks(target, weaponItem);
			// the hit is calculated to happen halfway to the animation - might need further tuning e.g. in bow case
			final int timeToHit = timeAtk / 2;
			_attackEndTime = System.currentTimeMillis() + timeAtk;
			final int ssGrade = (weaponItem != null) ? weaponItem.getItemGrade().ordinal() : 0;
			// Create a Server->Client packet Attack
			Attack attack = new Attack(this, target, wasSSCharged, ssGrade);
			
			// Make sure that char is facing selected target
			// also works: setHeading(Util.convertDegreeToClientHeading(Util.calculateAngleFrom(this, target)));
			setHeading(Util.calculateHeadingFrom(this, target));
			
			// Get the Attack Reuse Delay of the L2Weapon
			int reuse = calculateReuseTime(target, weaponItem);
			boolean hitted = false;
			switch (getAttackType())
			{
				case BOW:
				{
					hitted = doAttackHitByBow(attack, target, timeAtk, reuse, false);
					break;
				}
				case CROSSBOW:
				case TWOHANDCROSSBOW:
				{
					hitted = doAttackHitByBow(attack, target, timeAtk, reuse, true);
					break;
				}
				case POLE:
				{
					hitted = doAttackHitSimple(attack, target, timeToHit);
					break;
				}
				case FIST:
				{
					if (!isPlayer())
					{
						hitted = doAttackHitSimple(attack, target, timeToHit);
						break;
					}
				}
				case DUAL:
				case DUALFIST:
				case DUALDAGGER:
				{
					hitted = doAttackHitByDual(attack, target, timeToHit);
					break;
				}
				default:
				{
					hitted = doAttackHitSimple(attack, target, timeToHit);
					break;
				}
			}
			
			// Flag the attacker if it's a L2PcInstance outside a PvP area
			final PlayerInstance player = getActingPlayer();
			if (player != null)
			{
				AttackStanceTaskManager.getInstance().addAttackStanceTask(player);
				if ((player.getPet() != target) && !player.hasServitor(target.getObjectId()))
				{
					player.updatePvPStatus(target);
				}
			}
			// Check if hit isn't missed
			if (!hitted)
			{
				abortAttack(); // Abort the attack of the L2Character and send Server->Client ActionFailed packet
			}
			else
			{
				// If we didn't miss the hit, discharge the shoulshots, if any
				setChargedShot(ShotType.SOULSHOTS, false);
				
				if (player != null)
				{
					if (player.isCursedWeaponEquipped())
					{
						// If hit by a cursed weapon, CP is reduced to 0
						if (!target.isInvul())
						{
							target.setCurrentCp(0);
						}
					}
					else if (player.isHero())
					{
						// If a cursed weapon is hit by a Hero, CP is reduced to 0
						if (target.isPlayer() && target.getActingPlayer().isCursedWeaponEquipped())
						{
							target.setCurrentCp(0);
						}
					}
				}
			}
			
			// If the Server->Client packet Attack contains at least 1 hit, send the Server->Client packet Attack
			// to the L2Character AND to all L2PcInstance in the _KnownPlayers of the L2Character
			if (attack.hasHits())
			{
				broadcastPacket(attack);
			}
			
			// Notify AI with EVT_READY_TO_ACT
			ThreadPoolManager.getInstance().scheduleAi(new NotifyAITask(this, CtrlEvent.EVT_READY_TO_ACT), timeAtk + reuse);
		}
	}
	
	/**
	 * Launch a Bow attack.<br>
	 * <B><U>Actions</U>:</B>
	 * <ul>
	 * <li>Calculate if hit is missed or not</li>
	 * <li>Consume arrows</li>
	 * <li>If hit isn't missed, calculate if shield defense is efficient</li>
	 * <li>If hit isn't missed, calculate if hit is critical</li>
	 * <li>If hit isn't missed, calculate physical damages</li>
	 * <li>If the L2Character is a L2PcInstance, Send a Server->Client packet SetupGauge</li>
	 * <li>Create a new hit task with Medium priority</li>
	 * <li>Calculate and set the disable delay of the bow in function of the Attack Speed</li>
	 * <li>Add this hit to the Server-Client packet Attack</li>
	 * </ul>
	 * @param attack Server->Client packet Attack in which the hit will be added
	 * @param target The L2Character targeted
	 * @param sAtk The Attack Speed of the attacker
	 * @param reuse
	 * @param crossbow : if used weapon to fire is crossbow instead of a bow
	 * @return True if the hit isn't missed
	 */
	private boolean doAttackHitByBow(Attack attack, Creature target, int sAtk, int reuse, boolean crossbow)
	{
		int damage1 = 0;
		byte shld1 = 0;
		boolean crit1 = false;
		
		// Calculate if hit is missed or not
		boolean miss1 = Formulas.calcHitMiss(this, target);
		
		// Consume arrows
		reduceArrowCount(crossbow);
		
		_move = null;
		
		// Check if hit isn't missed
		if (!miss1)
		{
			// Calculate if shield defense is efficient
			shld1 = Formulas.calcShldUse(this, target);
			
			// Calculate if hit is critical
			crit1 = Formulas.calcCrit(getStat().getCriticalHit(target, null), false, target);
			
			// Calculate physical damages
			damage1 = (int) Formulas.calcPhysDam(this, target, null, shld1, crit1, attack.hasSoulshot());
			
			// Normal attacks have normal damage x 5
			damage1 = (int) calcStat(Stats.REGULAR_ATTACKS_DMG, damage1);
			
			// Bows Ranged Damage Formula (Damage gradually decreases when 60% or lower than full hit range, and increases when 60% or higher).
			// full hit range is 500 which is the base bow range, and the 60% of this is 800.
			if (!crossbow)
			{
				damage1 *= (calculateDistance(target, true, false) / 4000) + 0.8;
			}
		}
		
		// Check if the L2Character is a L2PcInstance
		if (isPlayer())
		{
			if (crossbow)
			{
				sendPacket(SystemMessageId.YOUR_CROSSBOW_IS_PREPARING_TO_FIRE);
			}
			
			sendPacket(new SetupGauge(getObjectId(), SetupGauge.RED, sAtk + reuse));
		}
		
		// Create a new hit task with Medium priority
		ThreadPoolManager.getInstance().scheduleAi(new HitTask(this, target, damage1, crit1, miss1, attack.hasSoulshot(), shld1), sAtk);
		
		// Calculate and set the disable delay of the bow in function of the Attack Speed
		if (crossbow)
		{
			_disableCrossBowAttackEndTime = ((sAtk + reuse) / GameTimeController.MILLIS_IN_TICK) + GameTimeController.getInstance().getGameTicks();
		}
		else
		{
			_disableBowAttackEndTime = ((sAtk + reuse) / GameTimeController.MILLIS_IN_TICK) + GameTimeController.getInstance().getGameTicks();
		}
		
		// Add this hit to the Server-Client packet Attack
		attack.addHit(target, damage1, miss1, crit1, shld1);
		
		// Return true if hit isn't missed
		return !miss1;
	}
	
	/**
	 * Launch a Dual attack.<br>
	 * <B><U>Actions</U>:</B>
	 * <ul>
	 * <li>Calculate if hits are missed or not</li>
	 * <li>If hits aren't missed, calculate if shield defense is efficient</li>
	 * <li>If hits aren't missed, calculate if hit is critical</li>
	 * <li>If hits aren't missed, calculate physical damages</li>
	 * <li>Create 2 new hit tasks with Medium priority</li>
	 * <li>Add those hits to the Server-Client packet Attack</li>
	 * </ul>
	 * @param attack Server->Client packet Attack in which the hit will be added
	 * @param target The L2Character targeted
	 * @param sAtk
	 * @return True if hit 1 or hit 2 isn't missed
	 */
	private boolean doAttackHitByDual(Attack attack, Creature target, int sAtk)
	{
		int damage1 = 0;
		int damage2 = 0;
		byte shld1 = 0;
		byte shld2 = 0;
		boolean crit1 = false;
		boolean crit2 = false;
		
		// Calculate if hits are missed or not
		boolean miss1 = Formulas.calcHitMiss(this, target);
		boolean miss2 = Formulas.calcHitMiss(this, target);
		
		// Check if hit 1 isn't missed
		if (!miss1)
		{
			// Calculate if shield defense is efficient against hit 1
			shld1 = Formulas.calcShldUse(this, target);
			
			// Calculate if hit 1 is critical
			crit1 = Formulas.calcCrit(getStat().getCriticalHit(target, null), false, target);
			
			// Calculate physical damages of hit 1
			damage1 = (int) Formulas.calcPhysDam(this, target, null, shld1, crit1, attack.hasSoulshot());
			
			// Normal attacks have normal damage x 5
			damage1 = (int) calcStat(Stats.REGULAR_ATTACKS_DMG, damage1);
			
			damage1 /= 2;
		}
		
		// Check if hit 2 isn't missed
		if (!miss2)
		{
			// Calculate if shield defense is efficient against hit 2
			shld2 = Formulas.calcShldUse(this, target);
			
			// Calculate if hit 2 is critical
			crit2 = Formulas.calcCrit(getStat().getCriticalHit(target, null), false, target);
			
			// Calculate physical damages of hit 2
			damage2 = (int) Formulas.calcPhysDam(this, target, null, shld2, crit2, attack.hasSoulshot());
			
			// Normal attacks have normal damage x 5
			damage2 = (int) calcStat(Stats.REGULAR_ATTACKS_DMG, damage2);
			
			damage2 /= 2;
		}
		
		// Create a new hit task with Medium priority for hit 1
		ThreadPoolManager.getInstance().scheduleAi(new HitTask(this, target, damage1, crit1, miss1, attack.hasSoulshot(), shld1), sAtk / 2);
		
		// Create a new hit task with Medium priority for hit 2 with a higher delay
		ThreadPoolManager.getInstance().scheduleAi(new HitTask(this, target, damage2, crit2, miss2, attack.hasSoulshot(), shld2), sAtk);
		
		// Add those hits to the Server-Client packet Attack
		attack.addHit(target, damage1, miss1, crit1, shld1);
		attack.addHit(target, damage2, miss2, crit2, shld2);
		
		// Launch multiple attack (if possible)
		int attackCountMax = (int) getStat().calcStat(Stats.ATTACK_COUNT_MAX, 1, null, null);
		if (attackCountMax > 1)
		{
			attackCountMax--; // Main target has already been attacked.
			List<Creature> attackSurround = getAttackSurround(target, sAtk, attackCountMax);
			for (Creature surroundTarget : attackSurround)
			{
				int damage = 0;
				byte shld = 0;
				boolean crit = false;
				boolean miss = Formulas.calcHitMiss(this, target);
				
				if (!miss)
				{
					shld = Formulas.calcShldUse(this, surroundTarget);
					crit = Formulas.calcCrit(getStat().getCriticalHit(surroundTarget, null), false, surroundTarget);
					damage = (int) Formulas.calcPhysDam(this, surroundTarget, null, shld, crit, attack.hasSoulshot());
					damage = (int) calcStat(Stats.REGULAR_ATTACKS_DMG, damage);
					damage /= 2;
				}
				
				ThreadPoolManager.getInstance().scheduleAi(new HitTask(this, surroundTarget, damage, crit, miss, attack.hasSoulshot(), shld), sAtk / 2);
				attack.addHit(surroundTarget, damage, miss, crit, shld);
				miss1 |= miss;
			}
			
			for (Creature surroundTarget : attackSurround)
			{
				int damage = 0;
				byte shld = 0;
				boolean crit = false;
				boolean miss = Formulas.calcHitMiss(this, target);
				
				if (!miss)
				{
					shld = Formulas.calcShldUse(this, surroundTarget);
					crit = Formulas.calcCrit(getStat().getCriticalHit(surroundTarget, null), false, surroundTarget);
					damage = (int) Formulas.calcPhysDam(this, surroundTarget, null, shld, crit, attack.hasSoulshot());
					damage = (int) calcStat(Stats.REGULAR_ATTACKS_DMG, damage);
					damage /= 2;
				}
				
				ThreadPoolManager.getInstance().scheduleAi(new HitTask(this, surroundTarget, damage, crit, miss, attack.hasSoulshot(), shld), sAtk);
				attack.addHit(surroundTarget, damage, miss, crit, shld);
				miss2 |= miss;
			}
			
		}
		
		// Return true if hit 1 or hit 2 isn't missed
		return (!miss1 || !miss2);
	}
	
	/**
	 * Launch a simple attack.<br>
	 * <B><U>Actions</U>:</B>
	 * <ul>
	 * <li>Calculate if hit is missed or not</li>
	 * <li>If hit isn't missed, calculate if shield defense is efficient</li>
	 * <li>If hit isn't missed, calculate if hit is critical</li>
	 * <li>If hit isn't missed, calculate physical damages</li>
	 * <li>Create a new hit task with Medium priority</li>
	 * <li>Add this hit to the Server-Client packet Attack</li>
	 * </ul>
	 * @param attack Server->Client packet Attack in which the hit will be added
	 * @param target The L2Character targeted
	 * @param sAtk
	 * @return True if the hit isn't missed
	 */
	private boolean doAttackHitSimple(Attack attack, Creature target, int sAtk)
	{
		int damage1 = 0;
		byte shld1 = 0;
		boolean crit1 = false;
		
		// Calculate if hit is missed or not
		boolean miss1 = Formulas.calcHitMiss(this, target);
		
		// Check if hit isn't missed
		if (!miss1)
		{
			// Calculate if shield defense is efficient
			shld1 = Formulas.calcShldUse(this, target);
			
			// Calculate if hit is critical
			crit1 = Formulas.calcCrit(getStat().getCriticalHit(target, null), false, target);
			
			// Calculate physical damages
			damage1 = (int) Formulas.calcPhysDam(this, target, null, shld1, crit1, attack.hasSoulshot());
			
			// Normal attacks have normal damage x 5
			damage1 = (int) calcStat(Stats.REGULAR_ATTACKS_DMG, damage1);
		}
		
		// Create a new hit task with Medium priority
		ThreadPoolManager.getInstance().scheduleAi(new HitTask(this, target, damage1, crit1, miss1, attack.hasSoulshot(), shld1), sAtk);
		
		// Add this hit to the Server-Client packet Attack
		attack.addHit(target, damage1, miss1, crit1, shld1);
		
		// H5 Changes: without Polearm Mastery (skill 216) max simultaneous attacks is 3 (1 by default + 2 in skill 3599).
		int attackCountMax = (int) getStat().calcStat(Stats.ATTACK_COUNT_MAX, 1, null, null);
		if (attackCountMax > 1)
		{
			attackCountMax--; // Main target has already been attacked.
			for (Creature surroundTarget : getAttackSurround(target, sAtk, attackCountMax))
			{
				int damage = 0;
				byte shld = 0;
				boolean crit = false;
				boolean miss = Formulas.calcHitMiss(this, target);
				
				if (!miss)
				{
					shld = Formulas.calcShldUse(this, surroundTarget);
					crit = Formulas.calcCrit(getStat().getCriticalHit(surroundTarget, null), false, surroundTarget);
					damage = (int) Formulas.calcPhysDam(this, surroundTarget, null, shld, crit, attack.hasSoulshot());
					damage = (int) calcStat(Stats.REGULAR_ATTACKS_DMG, damage);
				}
				
				ThreadPoolManager.getInstance().scheduleAi(new HitTask(this, surroundTarget, damage, crit, miss, attack.hasSoulshot(), shld), sAtk);
				attack.addHit(surroundTarget, damage, miss, crit, shld);
				miss1 |= miss;
			}
			
		}
		
		// Return true if hit isn't missed
		return !miss1;
	}
	
	/**
	 * @param target
	 * @param sAtk
	 * @param attackCountMax
	 * @return a list of surrounding enemies based on your weapon.
	 */
	private List<Creature> getAttackSurround(Creature target, int sAtk, int attackCountMax)
	{
		final List<Creature> list = new LinkedList<>();
		final int maxRadius = getStat().getPhysicalAttackRadius();
		final int maxAngleDiff = getStat().getPhysicalAttackAngle();
		for (Creature obj : World.getInstance().getVisibleObjects(this, Creature.class, maxRadius))
		{
			if (obj == target)
			{
				continue;
			}
			
			if (obj.isPet() && isPlayer() && (((L2PetInstance) obj).getOwner() == getActingPlayer()))
			{
				continue;
			}
			
			if (!isFacing(obj, maxAngleDiff))
			{
				continue;
			}
			
			if (isAttackable() && obj.isPlayer() && getTarget().isAttackable())
			{
				continue;
			}
			
			if (isAttackable() && obj.isAttackable() && !((Attackable) this).isChaos())
			{
				continue;
			}
			
			// Launch a simple attack against the L2Character targeted
			if (!obj.isAlikeDead())
			{
				if ((obj == getAI().getAttackTarget()) || obj.isAutoAttackable(this))
				{
					list.add(obj);
					if (list.size() >= attackCountMax)
					{
						break;
					}
				}
			}
		}
		
		return list;
	}
	
	/**
	 * Manage the casting task (casting and interrupt time, re-use delay...) and display the casting bar and animation on client.<br>
	 * <B><U>Actions</U>:</B>
	 * <ul>
	 * <li>Verify the possibility of the the cast : skill is a spell, caster isn't muted...</li>
	 * <li>Get the list of all targets (ex : area effects) and define the L2Charcater targeted (its stats will be used in calculation)</li>
	 * <li>Calculate the casting time (base + modifier of MAtkSpd), interrupt time and re-use delay</li>
	 * <li>Send a Server->Client packet MagicSkillUser (to display casting animation), a packet SetupGauge (to display casting bar) and a system message</li>
	 * <li>Disable all skills during the casting time (create a task EnableAllSkills)</li>
	 * <li>Disable the skill during the re-use delay (create a task EnableSkill)</li>
	 * <li>Create a task MagicUseTask (that will call method onMagicUseTimer) to launch the Magic Skill at the end of the casting time</li>
	 * </ul>
	 * @param skill The L2Skill to use
	 */
	public void doCast(Skill skill)
	{
		beginCast(skill, false);
	}
	
	public void doSimultaneousCast(Skill skill)
	{
		beginCast(skill, true);
	}
	
	public void doCast(Skill skill, Creature target, WorldObject[] targets)
	{
		if (!checkDoCastConditions(skill))
		{
			setIsCastingNow(false);
			return;
		}
		
		// Override casting type
		if (skill.isSimultaneousCast())
		{
			doSimultaneousCast(skill, target, targets);
			return;
		}
		
		stopEffectsOnAction();
		
		// Recharge AutoSoulShot
		// this method should not used with L2Playable
		
		beginCast(skill, false, target, targets);
	}
	
	public void doSimultaneousCast(Skill skill, Creature target, WorldObject[] targets)
	{
		if (!checkDoCastConditions(skill))
		{
			setIsCastingSimultaneouslyNow(false);
			return;
		}
		stopEffectsOnAction();
		
		beginCast(skill, true, target, targets);
	}
	
	private void beginCast(Skill skill, boolean simultaneously)
	{
		if (!checkDoCastConditions(skill))
		{
			if (simultaneously)
			{
				setIsCastingSimultaneouslyNow(false);
			}
			else
			{
				setIsCastingNow(false);
			}
			if (isPlayer())
			{
				getAI().setIntention(AI_INTENTION_ACTIVE);
			}
			return;
		}
		
		// Override casting type
		if (skill.isSimultaneousCast() && !simultaneously)
		{
			simultaneously = true;
		}
		
		stopEffectsOnAction();
		
		// Set the target of the skill in function of Skill Type and Target Type
		Creature target = null;
		// Get all possible targets of the skill in a table in function of the skill target type
		WorldObject[] targets = skill.getTargetList(this);
		
		boolean doit = false;
		
		// AURA skills should always be using caster as target
		switch (skill.getTargetType())
		{
			case AREA_SUMMON: // We need it to correct facing
				target = getServitors().values().stream().findFirst().orElse(getPet());
				break;
			case AURA:
			case AURA_CORPSE_MOB:
			case FRONT_AURA:
			case BEHIND_AURA:
			case GROUND:
				target = this;
				break;
			case SELF:
			case PET:
			case SERVITOR:
			case SUMMON:
			case OWNER_PET:
			case PARTY:
			case CLAN:
			case PARTY_CLAN:
			case COMMAND_CHANNEL:
				doit = true;
			default:
				if (targets.length == 0)
				{
					if (simultaneously)
					{
						setIsCastingSimultaneouslyNow(false);
					}
					else
					{
						setIsCastingNow(false);
					}
					// Send a Server->Client packet ActionFailed to the L2PcInstance
					if (isPlayer())
					{
						sendPacket(ActionFailed.STATIC_PACKET);
						getAI().setIntention(AI_INTENTION_ACTIVE);
					}
					return;
				}
				
				if ((skill.isContinuous() && !skill.isDebuff()) || skill.hasEffectType(L2EffectType.CPHEAL, L2EffectType.HEAL))
				{
					doit = true;
				}
				
				if (doit)
				{
					target = (Creature) targets[0];
				}
				else
				{
					target = (Creature) getTarget();
				}
		}
		beginCast(skill, simultaneously, target, targets);
	}
	
	private void beginCast(Skill skill, boolean simultaneously, Creature target, WorldObject[] targets)
	{
		if (target == null)
		{
			if (simultaneously)
			{
				setIsCastingSimultaneouslyNow(false);
			}
			else
			{
				setIsCastingNow(false);
			}
			if (isPlayer())
			{
				sendPacket(ActionFailed.STATIC_PACKET);
				getAI().setIntention(AI_INTENTION_ACTIVE);
			}
			return;
		}
		
		final TerminateReturn term = EventDispatcher.getInstance().notifyEvent(new OnCreatureSkillUse(this, skill, simultaneously, target, targets), this, TerminateReturn.class);
		if ((term != null) && term.terminate())
		{
			if (simultaneously)
			{
				setIsCastingSimultaneouslyNow(false);
			}
			else
			{
				setIsCastingNow(false);
			}
			if (isPlayer())
			{
				sendPacket(ActionFailed.STATIC_PACKET);
				getAI().setIntention(AI_INTENTION_ACTIVE);
			}
			return;
		}
		
		// TODO: Unhardcode using event listeners!
		if (skill.hasEffectType(L2EffectType.RESURRECTION))
		{
			if (isResurrectionBlocked() || target.isResurrectionBlocked())
			{
				sendPacket(SystemMessageId.REJECT_RESURRECTION); // Reject resurrection
				target.sendPacket(SystemMessageId.REJECT_RESURRECTION); // Reject resurrection
				
				if (simultaneously)
				{
					setIsCastingSimultaneouslyNow(false);
				}
				else
				{
					setIsCastingNow(false);
				}
				
				if (isPlayer())
				{
					getAI().setIntention(AI_INTENTION_ACTIVE);
					sendPacket(ActionFailed.STATIC_PACKET);
				}
				return;
			}
		}
		
		// Get the Identifier of the skill
		int magicId = skill.getId();
		
		// Get the Base Casting Time of the Skills.
		int skillTime = (skill.getHitTime() + skill.getCoolTime());
		
		if (!skill.isChanneling() || (skill.getChannelingSkillId() == 0))
		{
			// Calculate the Casting Time of the "Non-Static" Skills (with caster PAtk/MAtkSpd).
			if (!skill.isStatic())
			{
				skillTime = Formulas.calcAtkSpd(this, skill, skillTime);
			}
			// Calculate the Casting Time of Magic Skills (reduced in 40% if using SPS/BSPS)
			if (skill.isMagic() && (isChargedShot(ShotType.SPIRITSHOTS) || isChargedShot(ShotType.BLESSED_SPIRITSHOTS)))
			{
				skillTime = (int) (0.6 * skillTime);
			}
		}
		
		// Avoid broken Casting Animation.
		// Client can't handle less than 550ms Casting Animation in Magic Skills with more than 550ms base.
		if (skill.isMagic() && ((skill.getHitTime() + skill.getCoolTime()) > 550) && (skillTime < 550))
		{
			skillTime = 550;
		}
		// Client can't handle less than 500ms Casting Animation in Physical Skills with 500ms base or more.
		else if (!skill.isStatic() && ((skill.getHitTime() + skill.getCoolTime()) >= 500) && (skillTime < 500))
		{
			skillTime = 500;
		}
		
		// queue herbs and potions
		if (isCastingSimultaneouslyNow() && simultaneously)
		{
			ThreadPoolManager.getInstance().scheduleAi(new UsePotionTask(this, skill), 100);
			return;
		}
		
		// Set the _castInterruptTime and casting status (L2PcInstance already has this true)
		if (simultaneously)
		{
			setIsCastingSimultaneouslyNow(true);
		}
		else
		{
			setIsCastingNow(true);
		}
		
		if (!simultaneously)
		{
			_castInterruptTime = -2 + GameTimeController.getInstance().getGameTicks() + (skillTime / GameTimeController.MILLIS_IN_TICK);
			setLastSkillCast(skill);
		}
		else
		{
			setLastSimultaneousSkillCast(skill);
		}
		
		// Calculate the Reuse Time of the Skill
		int reuseDelay;
		if (skill.isStaticReuse() || skill.isStatic())
		{
			reuseDelay = skill.getReuseDelay();
		}
		else if (skill.isMagic())
		{
			reuseDelay = (int) (skill.getReuseDelay() * calcStat(Stats.MAGIC_REUSE_RATE, 1, null, null));
		}
		else if (skill.isPhysical())
		{
			reuseDelay = (int) (skill.getReuseDelay() * calcStat(Stats.P_REUSE, 1, null, null));
		}
		else
		{
			reuseDelay = (int) (skill.getReuseDelay() * calcStat(Stats.DANCE_REUSE, 1, null, null));
		}
		
		boolean skillMastery = Formulas.calcSkillMastery(this, skill);
		
		// Skill reuse check
		if ((reuseDelay > 30000) && !skillMastery)
		{
			addTimeStamp(skill, reuseDelay);
		}
		
		// Check if this skill consume mp on start casting
		int initmpcons = getStat().getMpInitialConsume(skill);
		if (initmpcons > 0)
		{
			getStatus().reduceMp(initmpcons);
			StatusUpdate su = new StatusUpdate(this);
			su.addAttribute(StatusUpdate.CUR_MP, (int) getCurrentMp());
			sendPacket(su);
		}
		
		// Disable the skill during the re-use delay and create a task EnableSkill with Medium priority to enable it at the end of the re-use delay
		if (reuseDelay > 10)
		{
			if (skillMastery)
			{
				reuseDelay = 100;
				
				if (getActingPlayer() != null)
				{
					getActingPlayer().sendPacket(SystemMessageId.A_SKILL_IS_READY_TO_BE_USED_AGAIN);
				}
			}
			
			disableSkill(skill, reuseDelay);
		}
		
		// Make sure that char is facing selected target
		if (target != this)
		{
			setHeading(Util.calculateHeadingFrom(this, target));
			broadcastPacket(new ExRotation(getObjectId(), getHeading()));
		}
		
		if (isPlayable())
		{
			if (skill.getItemConsumeId() > 0)
			{
				if (!destroyItemByItemId("Consume", skill.getItemConsumeId(), skill.getItemConsumeCount(), null, true))
				{
					getActingPlayer().sendPacket(SystemMessageId.INCORRECT_ITEM_COUNT2);
					abortCast();
					return;
				}
			}
			
			// reduce talisman mana on skill use
			if ((skill.getReferenceItemId() > 0) && (ItemTable.getInstance().getTemplate(skill.getReferenceItemId()).getBodyPart() == L2Item.SLOT_DECO))
			{
				for (ItemInstance item : getInventory().getItemsByItemId(skill.getReferenceItemId()))
				{
					if (item.isEquipped())
					{
						item.decreaseMana(false, item.useSkillDisTime());
						break;
					}
				}
			}
		}
		
		final int actionId = isSummon() ? RequestActionUse.getActionId(skill.getId()) : -1;
		
		// Send a Server->Client packet MagicSkillUser with target, displayId, level, skillTime, reuseDelay
		// to the L2Character AND to all L2PcInstance in the _KnownPlayers of the L2Character
		broadcastPacket(new MagicSkillUse(this, target, skill.getDisplayId(), skill.getDisplayLevel(), skillTime, reuseDelay, actionId));
		
		// Send a system message to the player.
		if (isPlayer() && !skill.isAbnormalInstant())
		{
			SystemMessage sm = null;
			switch (magicId)
			{
				case 1312: // Fishing
				{
					// Done in L2PcInstance.startFishing()
					break;
				}
				case 2046: // Wolf Collar
				{
					sm = SystemMessage.getSystemMessage(SystemMessageId.SUMMONING_YOUR_PET);
					break;
				}
				default:
				{
					sm = SystemMessage.getSystemMessage(SystemMessageId.YOU_USE_S1);
					sm.addSkillName(skill);
					break;
				}
			}
			
			sendPacket(sm);
		}
		
		if (skill.hasEffects(EffectScope.START))
		{
			skill.applyEffectScope(EffectScope.START, new BuffInfo(this, target, skill), true, false);
		}
		
		// Before start AI Cast Broadcast Fly Effect is Need
		if (skill.getFlyType() != null)
		{
			ThreadPoolManager.getInstance().scheduleEffect(new FlyToLocationTask(this, target, skill), 50);
		}
		
		MagicUseTask mut = new MagicUseTask(this, targets, skill, skillTime, simultaneously);
		
		// launch the magic in skillTime milliseconds
		if (skillTime > 0)
		{
			// Send a Server->Client packet SetupGauge with the color of the gauge and the casting time
			if (isPlayer() && !simultaneously)
			{
				sendPacket(new SetupGauge(getObjectId(), SetupGauge.BLUE, skillTime));
			}
			
			if (skill.isChanneling() && (skill.getChannelingSkillId() > 0))
			{
				getSkillChannelizer().startChanneling(skill);
			}
			
			if (simultaneously)
			{
				Future<?> future = _skillCast2;
				if (future != null)
				{
					future.cancel(true);
					_skillCast2 = null;
				}
				
				// Create a task MagicUseTask to launch the MagicSkill at the end of the casting time (skillTime)
				// For client animation reasons (party buffs especially) 400 ms before!
				_skillCast2 = ThreadPoolManager.getInstance().scheduleEffect(mut, skillTime - 400);
			}
			else
			{
				Future<?> future = _skillCast;
				if (future != null)
				{
					future.cancel(true);
					_skillCast = null;
				}
				
				// Create a task MagicUseTask to launch the MagicSkill at the end of the casting time (skillTime)
				// For client animation reasons (party buffs especially) 400 ms before!
				_skillCast = ThreadPoolManager.getInstance().scheduleEffect(mut, skillTime - 400);
			}
		}
		else
		{
			mut.setSkillTime(0);
			onMagicLaunchedTimer(mut);
		}
	}
	
	/**
	 * Check if casting of skill is possible
	 * @param skill
	 * @return True if casting is possible
	 */
	public boolean checkDoCastConditions(Skill skill)
	{
		if ((skill == null) || isSkillDisabled(skill) || (((skill.getFlyRadius() > 0) || (skill.getFlyType() != null)) && isMovementDisabled()))
		{
			// Send a Server->Client packet ActionFailed to the L2PcInstance
			sendPacket(ActionFailed.STATIC_PACKET);
			return false;
		}
		
		// Check if the caster has enough MP
		if (getCurrentMp() < (getStat().getMpConsume(skill) + getStat().getMpInitialConsume(skill)))
		{
			// Send a System Message to the caster
			sendPacket(SystemMessageId.NOT_ENOUGH_MP);
			
			// Send a Server->Client packet ActionFailed to the L2PcInstance
			sendPacket(ActionFailed.STATIC_PACKET);
			return false;
		}
		
		// Check if the caster has enough HP
		if (getCurrentHp() <= skill.getHpConsume())
		{
			// Send a System Message to the caster
			sendPacket(SystemMessageId.NOT_ENOUGH_HP);
			
			// Send a Server->Client packet ActionFailed to the L2PcInstance
			sendPacket(ActionFailed.STATIC_PACKET);
			return false;
		}
		
		// Skill mute checks.
		if (!skill.isStatic())
		{
			// Check if the skill is a magic spell and if the L2Character is not muted
			if (skill.isMagic())
			{
				if (isMuted())
				{
					// Send a Server->Client packet ActionFailed to the L2PcInstance
					sendPacket(ActionFailed.STATIC_PACKET);
					return false;
				}
			}
			else
			{
				// Check if the skill is physical and if the L2Character is not physical_muted
				if (isPhysicalMuted())
				{
					// Send a Server->Client packet ActionFailed to the L2PcInstance
					sendPacket(ActionFailed.STATIC_PACKET);
					return false;
				}
			}
		}
		
		// prevent casting signets to peace zone
		if (skill.isChanneling() && (skill.getChannelingSkillId() > 0))
		{
			final ZoneRegion zoneRegion = ZoneManager.getInstance().getRegion(this);
			boolean canCast = true;
			if ((skill.getTargetType() == L2TargetType.GROUND) && isPlayer())
			{
				Location wp = getActingPlayer().getCurrentSkillWorldPosition();
				if (!zoneRegion.checkEffectRangeInsidePeaceZone(skill, wp.getX(), wp.getY(), wp.getZ()))
				{
					canCast = false;
				}
			}
			else if (!zoneRegion.checkEffectRangeInsidePeaceZone(skill, getX(), getY(), getZ()))
			{
				canCast = false;
			}
			if (!canCast)
			{
				SystemMessage sm = SystemMessage.getSystemMessage(SystemMessageId.S1_CANNOT_BE_USED_DUE_TO_UNSUITABLE_TERMS);
				sm.addSkillName(skill);
				sendPacket(sm);
				return false;
			}
		}
		
		// Check if the caster's weapon is limited to use only its own skills
		if (getActiveWeaponItem() != null)
		{
			Weapon wep = getActiveWeaponItem();
			if (wep.useWeaponSkillsOnly() && !isGM() && wep.hasSkills())
			{
				boolean found = false;
				for (SkillHolder sh : wep.getSkills())
				{
					if (sh.getSkillId() == skill.getId())
					{
						found = true;
					}
				}
				
				if (!found)
				{
					if (getActingPlayer() != null)
					{
						sendPacket(SystemMessageId.THAT_WEAPON_CANNOT_USE_ANY_OTHER_SKILL_EXCEPT_THE_WEAPON_S_SKILL);
					}
					return false;
				}
			}
		}
		
		// Check if the spell consumes an Item
		// TODO: combine check and consume
		if ((skill.getItemConsumeId() > 0) && (getInventory() != null))
		{
			// Get the L2ItemInstance consumed by the spell
			ItemInstance requiredItems = getInventory().getItemByItemId(skill.getItemConsumeId());
			
			// Check if the caster owns enough consumed Item to cast
			if ((requiredItems == null) || (requiredItems.getCount() < skill.getItemConsumeCount()))
			{
				// Checked: when a summon skill failed, server show required consume item count
				if (skill.hasEffectType(L2EffectType.SUMMON))
				{
					SystemMessage sm = SystemMessage.getSystemMessage(SystemMessageId.SUMMONING_A_SERVITOR_COSTS_S2_S1);
					sm.addItemName(skill.getItemConsumeId());
					sm.addInt(skill.getItemConsumeCount());
					sendPacket(sm);
				}
				else
				{
					// Send a System Message to the caster
					sendPacket(SystemMessageId.THERE_ARE_NOT_ENOUGH_NECESSARY_ITEMS_TO_USE_THE_SKILL);
				}
				return false;
			}
		}
		return true;
	}
	
	/**
	 * Gets the item reuse time stamps map.
	 * @return the item reuse time stamps map
	 */
	public final Map<Integer, TimeStamp> getItemReuseTimeStamps()
	{
		return _reuseTimeStampsItems;
	}
	
	/**
	 * Adds a item reuse time stamp.
	 * @param item the item
	 * @param reuse the reuse
	 */
	public final void addTimeStampItem(ItemInstance item, long reuse)
	{
		addTimeStampItem(item, reuse, -1);
	}
	
	/**
	 * Adds a item reuse time stamp.<br>
	 * Used for restoring purposes.
	 * @param item the item
	 * @param reuse the reuse
	 * @param systime the system time
	 */
	public final void addTimeStampItem(ItemInstance item, long reuse, long systime)
	{
		if (_reuseTimeStampsItems == null)
		{
			synchronized (this)
			{
				if (_reuseTimeStampsItems == null)
				{
					_reuseTimeStampsItems = new ConcurrentHashMap<>();
				}
			}
		}
		_reuseTimeStampsItems.put(item.getObjectId(), new TimeStamp(item, reuse, systime));
	}
	
	/**
	 * Gets the item remaining reuse time for a given item object ID.
	 * @param itemObjId the item object ID
	 * @return if the item has a reuse time stamp, the remaining time, otherwise -1
	 */
	public synchronized final long getItemRemainingReuseTime(int itemObjId)
	{
		final TimeStamp reuseStamp = (_reuseTimeStampsItems != null) ? _reuseTimeStampsItems.get(itemObjId) : null;
		return reuseStamp != null ? reuseStamp.getRemaining() : -1;
	}
	
	/**
	 * Gets the item remaining reuse time for a given shared reuse item group.
	 * @param group the shared reuse item group
	 * @return if the shared reuse item group has a reuse time stamp, the remaining time, otherwise -1
	 */
	public final long getReuseDelayOnGroup(int group)
	{
		if ((group > 0) && (_reuseTimeStampsItems != null))
		{
			for (TimeStamp ts : _reuseTimeStampsItems.values())
			{
				if ((ts.getSharedReuseGroup() == group) && ts.hasNotPassed())
				{
					return ts.getRemaining();
				}
			}
		}
		return -1;
	}
	
	/**
	 * Gets the skill reuse time stamps map.
	 * @return the skill reuse time stamps map
	 */
	public final Map<Integer, TimeStamp> getSkillReuseTimeStamps()
	{
		return _reuseTimeStampsSkills;
	}
	
	/**
	 * Adds the skill reuse time stamp.
	 * @param skill the skill
	 * @param reuse the delay
	 */
	public final void addTimeStamp(Skill skill, long reuse)
	{
		addTimeStamp(skill, reuse, -1);
	}
	
	/**
	 * Adds the skill reuse time stamp.<br>
	 * Used for restoring purposes.
	 * @param skill the skill
	 * @param reuse the reuse
	 * @param systime the system time
	 */
	public final void addTimeStamp(Skill skill, long reuse, long systime)
	{
		if (_reuseTimeStampsSkills == null)
		{
			synchronized (this)
			{
				if (_reuseTimeStampsSkills == null)
				{
					_reuseTimeStampsSkills = new ConcurrentHashMap<>();
				}
			}
		}
		_reuseTimeStampsSkills.put(skill.getReuseHashCode(), new TimeStamp(skill, reuse, systime));
	}
	
	/**
	 * Removes a skill reuse time stamp.
	 * @param skill the skill to remove
	 */
	public synchronized final void removeTimeStamp(Skill skill)
	{
		if (_reuseTimeStampsSkills != null)
		{
			_reuseTimeStampsSkills.remove(skill.getReuseHashCode());
		}
	}
	
	/**
	 * Removes all skill reuse time stamps.
	 */
	public synchronized final void resetTimeStamps()
	{
		if (_reuseTimeStampsSkills != null)
		{
			_reuseTimeStampsSkills.clear();
		}
	}
	
	/**
	 * Gets the skill remaining reuse time for a given skill hash code.
	 * @param hashCode the skill hash code
	 * @return if the skill has a reuse time stamp, the remaining time, otherwise -1
	 */
	public synchronized final long getSkillRemainingReuseTime(int hashCode)
	{
		final TimeStamp reuseStamp = (_reuseTimeStampsSkills != null) ? _reuseTimeStampsSkills.get(hashCode) : null;
		return reuseStamp != null ? reuseStamp.getRemaining() : -1;
	}
	
	/**
	 * Verifies if the skill is under reuse time.
	 * @param hashCode the skill hash code
	 * @return {@code true} if the skill is under reuse time, {@code false} otherwise
	 */
	public synchronized final boolean hasSkillReuse(int hashCode)
	{
		final TimeStamp reuseStamp = (_reuseTimeStampsSkills != null) ? _reuseTimeStampsSkills.get(hashCode) : null;
		return (reuseStamp != null) && reuseStamp.hasNotPassed();
	}
	
	/**
	 * Gets the skill reuse time stamp.
	 * @param hashCode the skill hash code
	 * @return if the skill has a reuse time stamp, the skill reuse time stamp, otherwise {@code null}
	 */
	public synchronized final TimeStamp getSkillReuseTimeStamp(int hashCode)
	{
		return _reuseTimeStampsSkills != null ? _reuseTimeStampsSkills.get(hashCode) : null;
	}
	
	/**
	 * Gets the disabled skills map.
	 * @return the disabled skills map
	 */
	public Map<Integer, Long> getDisabledSkills()
	{
		return _disabledSkills;
	}
	
	/**
	 * Enables a skill.
	 * @param skill the skill to enable
	 */
	public void enableSkill(Skill skill)
	{
		if ((skill == null) || (_disabledSkills == null))
		{
			return;
		}
		_disabledSkills.remove(skill.getReuseHashCode());
	}
	
	/**
	 * Disables a skill for a given time.<br>
	 * If delay is lesser or equal than zero, skill will be disabled "forever".
	 * @param skill the skill to disable
	 * @param delay delay in milliseconds
	 */
	public void disableSkill(Skill skill, long delay)
	{
		if (skill == null)
		{
			return;
		}
		
		if (_disabledSkills == null)
		{
			synchronized (this)
			{
				if (_disabledSkills == null)
				{
					_disabledSkills = new ConcurrentHashMap<>();
				}
			}
		}
		
		_disabledSkills.put(skill.getReuseHashCode(), delay > 0 ? System.currentTimeMillis() + delay : Long.MAX_VALUE);
	}
	
	/**
	 * Removes all the disabled skills.
	 */
	public synchronized final void resetDisabledSkills()
	{
		if (_disabledSkills != null)
		{
			_disabledSkills.clear();
		}
	}
	
	/**
	 * Verifies if the skill is disabled.
	 * @param skill the skill
	 * @return {@code true} if the skill is disabled, {@code false} otherwise
	 */
	public boolean isSkillDisabled(Skill skill)
	{
		if (skill == null)
		{
			return false;
		}
		
		if (_allSkillsDisabled || (!skill.canCastWhileDisabled() && isAllSkillsDisabled()))
		{
			return true;
		}
		
		return isSkillDisabledByReuse(skill.getReuseHashCode());
	}
	
	/**
	 * Verifies if the skill is under reuse.
	 * @param hashCode the skill hash code
	 * @return {@code true} if the skill is disabled, {@code false} otherwise
	 */
	public boolean isSkillDisabledByReuse(int hashCode)
	{
		if (_disabledSkills == null)
		{
			return false;
		}
		
		final Long stamp = _disabledSkills.get(hashCode);
		if (stamp == null)
		{
			return false;
		}
		
		if (stamp < System.currentTimeMillis())
		{
			_disabledSkills.remove(hashCode);
			return false;
		}
		return true;
	}
	
	/**
	 * Disables all skills.
	 */
	public void disableAllSkills()
	{
		_allSkillsDisabled = true;
	}
	
	/**
	 * Enables all skills, except those under reuse time or previously disabled.
	 */
	public void enableAllSkills()
	{
		_allSkillsDisabled = false;
	}
	
	/**
	 * Kill the L2Character.<br>
	 * <B><U>Actions</U>:</B>
	 * <ul>
	 * <li>Set target to null and cancel Attack or Cast</li>
	 * <li>Stop movement</li>
	 * <li>Stop HP/MP/CP Regeneration task</li>
	 * <li>Stop all active skills effects in progress on the L2Character</li>
	 * <li>Send the Server->Client packet StatusUpdate with current HP and MP to all other L2PcInstance to inform</li>
	 * <li>Notify L2Character AI</li>
	 * </ul>
	 * @param killer The L2Character who killed it
	 * @return false if the player is already dead.
	 */
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
		
		stopAllEffectsExceptThoseThatLastThroughDeath();
		
		calculateRewards(killer);
		
		// Send the Server->Client packet StatusUpdate with current HP and MP to all other L2PcInstance to inform
		broadcastStatusUpdate();
		
		// Notify L2Character AI
		if (hasAI())
		{
			getAI().notifyEvent(CtrlEvent.EVT_DEAD);
		}
		
		ZoneManager.getInstance().getRegion(this).onDeath(this);
		
		getAttackByList().clear();
		
		if (isChannelized())
		{
			getSkillChannelized().abortChannelization();
		}
		return true;
	}
	
	@Override
	public boolean deleteMe()
	{
		setDebug(null);
		
		if (hasAI())
		{
			getAI().stopAITask();
		}
		
		// Removes itself from the summoned list.
		if ((getSummoner() != null))
		{
			getSummoner().removeSummonedNpc(getObjectId());
		}
		
		return true;
	}
	
	protected void calculateRewards(Creature killer)
	{
	}
	
	/** Sets HP, MP and CP and revives the L2Character. */
	public void doRevive()
	{
		if (!isDead())
		{
			return;
		}
		if (!isTeleporting())
		{
			setIsPendingRevive(false);
			setIsDead(false);
			
			if ((Config.RESPAWN_RESTORE_CP > 0) && (getCurrentCp() < (getMaxCp() * Config.RESPAWN_RESTORE_CP)))
			{
				_status.setCurrentCp(getMaxCp() * Config.RESPAWN_RESTORE_CP);
			}
			if ((Config.RESPAWN_RESTORE_HP > 0) && (getCurrentHp() < (getMaxHp() * Config.RESPAWN_RESTORE_HP)))
			{
				_status.setCurrentHp(getMaxHp() * Config.RESPAWN_RESTORE_HP);
			}
			if ((Config.RESPAWN_RESTORE_MP > 0) && (getCurrentMp() < (getMaxMp() * Config.RESPAWN_RESTORE_MP)))
			{
				_status.setCurrentMp(getMaxMp() * Config.RESPAWN_RESTORE_MP);
			}
			
			// Start broadcast status
			broadcastPacket(new Revive(this));
			
			ZoneManager.getInstance().getRegion(this).onRevive(this);
		}
		else
		{
			setIsPendingRevive(true);
		}
	}
	
	/**
	 * Revives the L2Character using skill.
	 * @param revivePower
	 */
	public void doRevive(double revivePower)
	{
		doRevive();
	}
	
	/**
	 * Gets this creature's AI.
	 * @return the AI
	 */
	public final CharacterAI getAI()
	{
		if (_ai == null)
		{
			synchronized (this)
			{
				if (_ai == null)
				{
					return _ai = initAI();
				}
			}
		}
		return _ai;
	}
	
	/**
	 * Initialize this creature's AI.<br>
	 * OOP approach to be overridden in child classes.
	 * @return the new AI
	 */
	protected CharacterAI initAI()
	{
		return new CharacterAI(this);
	}
	
	public void detachAI()
	{
		if (isWalker())
		{
			return;
		}
		setAI(null);
	}
	
	public void setAI(CharacterAI newAI)
	{
		CharacterAI oldAI = _ai;
		if ((oldAI != null) && (oldAI != newAI) && (oldAI instanceof AttackableAI))
		{
			oldAI.stopAITask();
		}
		_ai = newAI;
	}
	
	/**
	 * Verifies if this creature has an AI,
	 * @return {@code true} if this creature has an AI, {@code false} otherwise
	 */
	public boolean hasAI()
	{
		return _ai != null;
	}
	
	/**
	 * @return True if the L2Character is RaidBoss or his minion.
	 */
	public boolean isRaid()
	{
		return false;
	}
	
	/**
	 * @return True if the L2Character is minion.
	 */
	public boolean isMinion()
	{
		return false;
	}
	
	/**
	 * @return True if the L2Character is minion of RaidBoss.
	 */
	public boolean isRaidMinion()
	{
		return false;
	}
	
	/**
	 * @return a list of L2Character that attacked.
	 */
	public final Set<WeakReference<Creature>> getAttackByList()
	{
		if (_attackByList == null)
		{
			synchronized (this)
			{
				if (_attackByList == null)
				{
					_attackByList = ConcurrentHashMap.newKeySet();
				}
			}
		}
		return _attackByList;
	}
	
	public final Skill getLastSimultaneousSkillCast()
	{
		return _lastSimultaneousSkillCast;
	}
	
	public void setLastSimultaneousSkillCast(Skill skill)
	{
		_lastSimultaneousSkillCast = skill;
	}
	
	public final Skill getLastSkillCast()
	{
		return _lastSkillCast;
	}
	
	public void setLastSkillCast(Skill skill)
	{
		_lastSkillCast = skill;
	}
	
	public final boolean isNoRndWalk()
	{
		return _isNoRndWalk;
	}
	
	public final void setIsNoRndWalk(boolean value)
	{
		_isNoRndWalk = value;
	}
	
	public final boolean isAfraid()
	{
		return isAffected(EffectFlag.FEAR);
	}
	
	/**
	 * @return True if the L2Character can't use its skills (ex : stun, sleep...).
	 */
	public final boolean isAllSkillsDisabled()
	{
		return _allSkillsDisabled || isStunned() || isSleeping() || isParalyzed();
	}
	
	/**
	 * @return True if the L2Character can't attack (stun, sleep, attackEndTime, fakeDeath, paralyze, attackMute).
	 */
	public boolean isAttackingDisabled()
	{
		return isFlying() || isStunned() || isSleeping() || isAttackingNow() || isAlikeDead() || isParalyzed() || isPhysicalAttackMuted() || isCoreAIDisabled();
	}
	
	public final Calculator[] getCalculators()
	{
		return _calculators;
	}
	
	public final boolean isConfused()
	{
		return isAffected(EffectFlag.CONFUSED);
	}
	
	/**
	 * @return True if the L2Character is dead or use fake death.
	 */
	public boolean isAlikeDead()
	{
		return _isDead;
	}
	
	/**
	 * @return True if the L2Character is dead.
	 */
	public final boolean isDead()
	{
		return _isDead;
	}
	
	public final void setIsDead(boolean value)
	{
		_isDead = value;
	}
	
	public boolean isImmobilized()
	{
		return _isImmobilized;
	}
	
	public void setIsImmobilized(boolean value)
	{
		_isImmobilized = value;
	}
	
	public final boolean isMuted()
	{
		return isAffected(EffectFlag.MUTED);
	}
	
	public final boolean isPhysicalMuted()
	{
		return isAffected(EffectFlag.PSYCHICAL_MUTED);
	}
	
	public final boolean isPhysicalAttackMuted()
	{
		return isAffected(EffectFlag.PSYCHICAL_ATTACK_MUTED);
	}
	
	/**
	 * @return True if the L2Character can't move (stun, root, sleep, overload, paralyzed).
	 */
	public boolean isMovementDisabled()
	{
		// check for isTeleporting to prevent teleport cheating (if appear packet not received)
		return isStunned() || isRooted() || isSleeping() || isOverloaded() || isParalyzed() || isImmobilized() || isAlikeDead() || isTeleporting();
	}
	
	/**
	 * @return True if the L2Character can not be controlled by the player (confused, afraid).
	 */
	public final boolean isOutOfControl()
	{
		return isConfused() || isAfraid();
	}
	
	public final boolean isOverloaded()
	{
		return _isOverloaded;
	}
	
	/**
	 * Set the overloaded status of the L2Character is overloaded (if True, the L2PcInstance can't take more item).
	 * @param value
	 */
	public final void setIsOverloaded(boolean value)
	{
		_isOverloaded = value;
	}
	
	public final boolean isParalyzed()
	{
		return _isParalyzed || isAffected(EffectFlag.PARALYZED);
	}
	
	public final void setIsParalyzed(boolean value)
	{
		_isParalyzed = value;
	}
	
	public final boolean isPendingRevive()
	{
		return isDead() && _isPendingRevive;
	}
	
	public final void setIsPendingRevive(boolean value)
	{
		_isPendingRevive = value;
	}
	
	public final boolean isDisarmed()
	{
		return isAffected(EffectFlag.DISARMED);
	}
	
	/**
	 * @return the summon
	 */
	public Summon getPet()
	{
		return null;
	}
	
	/**
	 * @return the summon
	 */
	public Map<Integer, Summon> getServitors()
	{
		return Collections.emptyMap();
	}
	
	public Summon getServitor(int objectId)
	{
		return null;
	}
	
	/**
	 * @return {@code true} if the character has a summon, {@code false} otherwise
	 */
	public final boolean hasSummon()
	{
		return (getPet() != null) || !getServitors().isEmpty();
	}
	
	/**
	 * @return {@code true} if the character has a pet, {@code false} otherwise
	 */
	public final boolean hasPet()
	{
		return getPet() != null;
	}
	
	public final boolean hasServitor(int objectId)
	{
		return getServitors().containsKey(objectId);
	}
	
	/**
	 * @return {@code true} if the character has a servitor, {@code false} otherwise
	 */
	public final boolean hasServitors()
	{
		return !getServitors().isEmpty();
	}
	
	public void removeServitor(int objectId)
	{
		getServitors().remove(objectId);
	}
	
	public final boolean isRooted()
	{
		return isAffected(EffectFlag.ROOTED);
	}
	
	/**
	 * @return True if the L2Character is running.
	 */
	public boolean isRunning()
	{
		return _isRunning;
	}
	
	public final void setIsRunning(boolean value)
	{
		if (_isRunning == value)
		{
			return;
		}
		
		_isRunning = value;
		if (getRunSpeed() != 0)
		{
			broadcastPacket(new ChangeMoveType(this));
		}
		if (isPlayer())
		{
			getActingPlayer().broadcastUserInfo();
		}
		else if (isSummon())
		{
			broadcastStatusUpdate();
		}
		else if (isNpc())
		{
			World.getInstance().forEachVisibleObject(this, PlayerInstance.class, player ->
			{
				if (!isVisibleFor(player))
				{
					return;
				}
				
				if (getRunSpeed() == 0)
				{
					player.sendPacket(new ServerObjectInfo((Npc) this, player));
				}
				else
				{
					player.sendPacket(new NpcInfo((Npc) this));
				}
			});
		}
	}
	
	/** Set the L2Character movement type to run and send Server->Client packet ChangeMoveType to all others L2PcInstance. */
	public final void setRunning()
	{
		if (!isRunning())
		{
			setIsRunning(true);
		}
	}
	
	public final boolean isSleeping()
	{
		return isAffected(EffectFlag.SLEEP);
	}
	
	public final boolean isStunned()
	{
		return isAffected(EffectFlag.STUNNED);
	}
	
	public final boolean isBetrayed()
	{
		return isAffected(EffectFlag.BETRAYED);
	}
	
	public final boolean isTeleporting()
	{
		return _isTeleporting;
	}
	
	public void setIsTeleporting(boolean value)
	{
		_isTeleporting = value;
	}
	
	public void setIsInvul(boolean b)
	{
		_isInvul = b;
	}
	
	@Override
	public boolean isInvul()
	{
		return _isInvul || _isTeleporting || isAffected(EffectFlag.INVUL);
	}
	
	public void setIsMortal(boolean b)
	{
		_isMortal = b;
	}
	
	public boolean isMortal()
	{
		return _isMortal;
	}
	
	public boolean isUndead()
	{
		return false;
	}
	
	public boolean isResurrectionBlocked()
	{
		return isAffected(EffectFlag.BLOCK_RESURRECTION);
	}
	
	public final boolean isFlying()
	{
		return _isFlying;
	}
	
	public final void setIsFlying(boolean mode)
	{
		_isFlying = mode;
	}
	
	public CharStat getStat()
	{
		return _stat;
	}
	
	/**
	 * Initializes the CharStat class of the L2Object, is overwritten in classes that require a different CharStat Type.<br>
	 * Removes the need for instanceof checks.
	 */
	public void initCharStat()
	{
		_stat = new CharStat(this);
	}
	
	public final void setStat(CharStat value)
	{
		_stat = value;
	}
	
	public CharStatus getStatus()
	{
		return _status;
	}
	
	/**
	 * Initializes the CharStatus class of the L2Object, is overwritten in classes that require a different CharStatus Type.<br>
	 * Removes the need for instanceof checks.
	 */
	public void initCharStatus()
	{
		_status = new CharStatus(this);
	}
	
	public final void setStatus(CharStatus value)
	{
		_status = value;
	}
	
	public L2CharTemplate getTemplate()
	{
		return _template;
	}
	
	/**
	 * Set the template of the L2Character.<br>
	 * <B><U>Concept</U>:</B><br>
	 * Each L2Character owns generic and static properties (ex : all Keltir have the same number of HP...).<br>
	 * All of those properties are stored in a different template for each type of L2Character.<br>
	 * Each template is loaded once in the server cache memory (reduce memory use).<br>
	 * When a new instance of L2Character is spawned, server just create a link between the instance and the template This link is stored in <B>_template</B>.
	 * @param template
	 */
	protected final void setTemplate(L2CharTemplate template)
	{
		_template = template;
	}
	
	/**
	 * @return the Title of the L2Character.
	 */
	public final String getTitle()
	{
		return _title != null ? _title : "";
	}
	
	/**
	 * Set the Title of the L2Character.
	 * @param value
	 */
	public final void setTitle(String value)
	{
		if (value == null)
		{
			_title = "";
		}
		else
		{
			_title = value.length() > 21 ? value.substring(0, 20) : value;
		}
	}
	
	/**
	 * Set the L2Character movement type to walk and send Server->Client packet ChangeMoveType to all others L2PcInstance.
	 */
	public final void setWalking()
	{
		if (isRunning())
		{
			setIsRunning(false);
		}
	}
	
	/**
	 * Resets the abnormal visual effects recalculating all of them that are applied from skills and sending packet for updating them on client if a change is found.
	 */
	public void resetCurrentAbnormalVisualEffects()
	{
		final Collection<BuffInfo> passives = getEffectList().hasPassives() ? new ArrayList<>(getEffectList().getPassives().values()) : null;
		//@formatter:off
		final Set<AbnormalVisualEffect> abnormalVisualEffects =  Stream.concat(getEffectList().getEffects().stream(), passives != null ? passives.stream() : Stream.empty())
			.filter(Objects::nonNull)
			.map(BuffInfo::getSkill)
			.filter(Skill::hasAbnormalVisualEffects)
			.flatMap(s -> s.getAbnormalVisualEffects().stream())
			.collect(Collectors.toCollection(HashSet::new));
		//@formatter:on
		
		if (_abnormalVisualEffects != null)
		{
			abnormalVisualEffects.addAll(_abnormalVisualEffects);
		}
		
		if ((_currentAbnormalVisualEffects == null) || !_currentAbnormalVisualEffects.equals(abnormalVisualEffects))
		{
			_currentAbnormalVisualEffects = Collections.unmodifiableSet(abnormalVisualEffects);
			updateAbnormalVisualEffects();
		}
	}
	
	/**
	 * Gets the currently applied abnormal visual effects.
	 * @return the abnormal visual effects
	 */
	public Set<AbnormalVisualEffect> getCurrentAbnormalVisualEffects()
	{
		return _currentAbnormalVisualEffects != null ? _currentAbnormalVisualEffects : Collections.emptySet();
	}
	
	/**
	 * Checks if the creature has the abnormal visual effect.
	 * @param ave the abnormal visual effect
	 * @return {@code true} if the creature has the abnormal visual effect, {@code false} otherwise
	 */
	public boolean hasAbnormalVisualEffect(AbnormalVisualEffect ave)
	{
		return (_abnormalVisualEffects != null) && _abnormalVisualEffects.contains(ave);
	}
	
	/**
	 * Adds the abnormal visual and sends packet for updating them in client.
	 * @param aves the abnormal visual effects
	 */
	public final void startAbnormalVisualEffect(AbnormalVisualEffect... aves)
	{
		for (AbnormalVisualEffect ave : aves)
		{
			if (_abnormalVisualEffects == null)
			{
				synchronized (this)
				{
					if (_abnormalVisualEffects == null)
					{
						_abnormalVisualEffects = Collections.newSetFromMap(new ConcurrentHashMap<>());
					}
				}
			}
			_abnormalVisualEffects.add(ave);
		}
		resetCurrentAbnormalVisualEffects();
	}
	
	/**
	 * Removes the abnormal visual and sends packet for updating them in client.
	 * @param aves the abnormal visual effects
	 */
	public final void stopAbnormalVisualEffect(AbnormalVisualEffect... aves)
	{
		if (_abnormalVisualEffects != null)
		{
			for (AbnormalVisualEffect ave : aves)
			{
				_abnormalVisualEffects.remove(ave);
			}
			resetCurrentAbnormalVisualEffects();
		}
	}
	
	/**
	 * Active the abnormal effect Fake Death flag, notify the L2Character AI and send Server->Client UserInfo/CharInfo packet.
	 */
	public final void startFakeDeath()
	{
		if (!isPlayer())
		{
			return;
		}
		
		getActingPlayer().setIsFakeDeath(true);
		// Aborts any attacks/casts if fake dead
		abortAttack();
		abortCast();
		stopMove(null);
		getAI().notifyEvent(CtrlEvent.EVT_FAKE_DEATH);
		broadcastPacket(new ChangeWaitType(this, ChangeWaitType.WT_START_FAKEDEATH));
	}
	
	/**
	 * Launch a Stun Abnormal Effect on the L2Character.<br>
	 * <B><U>Actions</U>:</B>
	 * <ul>
	 * <li>Calculate the success rate of the Stun Abnormal Effect on this L2Character</li>
	 * <li>If Stun succeed, active the abnormal effect Stun flag, notify the L2Character AI and send Server->Client UserInfo/CharInfo packet</li>
	 * <li>If Stun NOT succeed, send a system message Failed to the L2PcInstance attacker</li>
	 * </ul>
	 */
	public final void startStunning()
	{
		// Aborts any attacks/casts if stunned
		abortAttack();
		abortCast();
		stopMove(null);
		getAI().notifyEvent(CtrlEvent.EVT_STUNNED);
		if (!isSummon())
		{
			getAI().setIntention(CtrlIntention.AI_INTENTION_IDLE);
		}
	}
	
	public final void startParalyze()
	{
		// Aborts any attacks/casts if paralyzed
		abortAttack();
		abortCast();
		stopMove(null);
		getAI().notifyEvent(CtrlEvent.EVT_PARALYZED);
	}
	
	/**
	 * Stop all active skills effects in progress on the L2Character.
	 */
	public void stopAllEffects()
	{
		_effectList.stopAllEffects();
	}
	
	/**
	 * Stops all effects, except those that last through death.
	 */
	public void stopAllEffectsExceptThoseThatLastThroughDeath()
	{
		_effectList.stopAllEffectsExceptThoseThatLastThroughDeath();
	}
	
	/**
	 * Stop and remove the effects corresponding to the skill ID.
	 * @param removed if {@code true} the effect will be set as removed, and a system message will be sent
	 * @param skillId the skill Id
	 */
	public void stopSkillEffects(boolean removed, int skillId)
	{
		_effectList.stopSkillEffects(removed, skillId);
	}
	
	public void stopSkillEffects(Skill skill)
	{
		_effectList.stopSkillEffects(true, skill.getId());
	}
	
	public final void stopEffects(L2EffectType type)
	{
		_effectList.stopEffects(type);
	}
	
	/**
	 * Exits all buffs effects of the skills with "removedOnAnyAction" set.<br>
	 * Called on any action except movement (attack, cast).
	 */
	public final void stopEffectsOnAction()
	{
		_effectList.stopEffectsOnAction();
	}
	
	/**
	 * Exits all buffs effects of the skills with "removedOnDamage" set.<br>
	 * Called on decreasing HP and mana burn.
	 * @param awake
	 */
	public final void stopEffectsOnDamage(boolean awake)
	{
		_effectList.stopEffectsOnDamage(awake);
	}
	
	/**
	 * Stop a specified/all Fake Death abnormal L2Effect.<br>
	 * <B><U>Actions</U>:</B>
	 * <ul>
	 * <li>Delete a specified/all (if effect=null) Fake Death abnormal L2Effect from L2Character and update client magic icon</li>
	 * <li>Set the abnormal effect flag _fake_death to False</li>
	 * <li>Notify the L2Character AI</li>
	 * </ul>
	 * @param removeEffects
	 */
	public final void stopFakeDeath(boolean removeEffects)
	{
		if (removeEffects)
		{
			stopEffects(L2EffectType.FAKE_DEATH);
		}
		
		// if this is a player instance, start the grace period for this character (grace from mobs only)!
		if (isPlayer())
		{
			getActingPlayer().setIsFakeDeath(false);
			getActingPlayer().setRecentFakeDeath(true);
		}
		
		broadcastPacket(new ChangeWaitType(this, ChangeWaitType.WT_STOP_FAKEDEATH));
		// TODO: Temp hack: players see FD on ppl that are moving: Teleport to someone who uses FD - if he gets up he will fall down again for that client -
		// even tho he is actually standing... Probably bad info in CharInfo packet?
		broadcastPacket(new Revive(this));
	}
	
	/**
	 * Stop a specified/all Stun abnormal L2Effect.<br>
	 * <B><U>Actions</U>:</B>
	 * <ul>
	 * <li>Delete a specified/all (if effect=null) Stun abnormal L2Effect from L2Character and update client magic icon</li>
	 * <li>Set the abnormal effect flag _stuned to False</li>
	 * <li>Notify the L2Character AI</li>
	 * <li>Send Server->Client UserInfo/CharInfo packet</li>
	 * </ul>
	 * @param removeEffects
	 */
	public final void stopStunning(boolean removeEffects)
	{
		if (removeEffects)
		{
			stopEffects(L2EffectType.STUN);
		}
		
		if (!isPlayer())
		{
			getAI().notifyEvent(CtrlEvent.EVT_THINK);
		}
	}
	
	/**
	 * Stop L2Effect: Transformation.<br>
	 * <B><U>Actions</U>:</B>
	 * <ul>
	 * <li>Remove Transformation Effect</li>
	 * <li>Notify the L2Character AI</li>
	 * <li>Send Server->Client UserInfo/CharInfo packet</li>
	 * </ul>
	 * @param removeEffects
	 */
	public final void stopTransformation(boolean removeEffects)
	{
		if (removeEffects)
		{
			getEffectList().stopSkillEffects(false, AbnormalType.TRANSFORM);
		}
		
		// if this is a player instance, then untransform, also set the transform_id column equal to 0 if not cursed.
		if (isPlayer())
		{
			if (getActingPlayer().getTransformation() != null)
			{
				getActingPlayer().untransform();
			}
		}
		
		if (!isPlayer())
		{
			getAI().notifyEvent(CtrlEvent.EVT_THINK);
		}
		updateAbnormalVisualEffects();
	}
	
	public abstract void updateAbnormalVisualEffects();
	
	/**
	 * Update active skills in progress (In Use and Not In Use because stacked) icons on client.<br>
	 * <B><U>Concept</U>:</B><br>
	 * All active skills effects in progress (In Use and Not In Use because stacked) are represented by an icon on the client.<br>
	 * <FONT COLOR=#FF0000><B> <U>Caution</U> : This method ONLY UPDATE the client of the player and not clients of all players in the party.</B></FONT>
	 */
	public final void updateEffectIcons()
	{
		updateEffectIcons(false);
	}
	
	/**
	 * Updates Effect Icons for this character(player/summon) and his party if any.
	 * @param partyOnly
	 */
	public void updateEffectIcons(boolean partyOnly)
	{
		// overridden
	}
	
	public boolean isAffectedBySkill(int skillId)
	{
		return _effectList.isAffectedBySkill(skillId);
	}
	
	/**
	 * This class group all movement data.<br>
	 * <B><U> Data</U> :</B>
	 * <ul>
	 * <li>_moveTimestamp : Last time position update</li>
	 * <li>_xDestination, _yDestination, _zDestination : Position of the destination</li>
	 * <li>_xMoveFrom, _yMoveFrom, _zMoveFrom : Position of the origin</li>
	 * <li>_moveStartTime : Start time of the movement</li>
	 * <li>_ticksToMove : Nb of ticks between the start and the destination</li>
	 * <li>_xSpeedTicks, _ySpeedTicks : Speed in unit/ticks</li>
	 * </ul>
	 */
	public static class MoveData
	{
		// when we retrieve x/y/z we use GameTimeControl.getGameTicks()
		// if we are moving, but move timestamp==gameticks, we don't need
		// to recalculate position
		public int _moveStartTime;
		public int _moveTimestamp; // last update
		public int _xDestination;
		public int _yDestination;
		public int _zDestination;
		public double _xAccurate; // otherwise there would be rounding errors
		public double _yAccurate;
		public double _zAccurate;
		public int _heading;
		
		public boolean disregardingGeodata;
		public int onGeodataPathIndex;
		public List<AbstractNodeLoc> geoPath;
		public int geoPathAccurateTx;
		public int geoPathAccurateTy;
		public int geoPathGtx;
		public int geoPathGty;
	}
	
	/**
	 * Add a Func to the Calculator set of the L2Character.<br>
	 * <b><u>Concept</u>:</b> A L2Character owns a table of Calculators called <b>_calculators</b>.<br>
	 * Each Calculator (a calculator per state) own a table of Func object.<br>
	 * A Func object is a mathematical function that permit to calculate the modifier of a state (ex : REGENERATE_HP_RATE...).<br>
	 * To reduce cache memory use, L2NPCInstances who don't have skills share the same Calculator set called <b>NPC_STD_CALCULATOR</b>.<br>
	 * That's why, if a L2NPCInstance is under a skill/spell effect that modify one of its state, a copy of the NPC_STD_CALCULATOR must be create in its _calculators before adding new Func object.<br>
	 * <b><u>Actions</u>:</b>
	 * <ul>
	 * <li>If _calculators is linked to NPC_STD_CALCULATOR, create a copy of NPC_STD_CALCULATOR in _calculators</li>
	 * <li>Add the Func object to _calculators</li>
	 * </ul>
	 * @param function The Func object to add to the Calculator corresponding to the state affected
	 */
	public final void addStatFunc(AbstractFunction function)
	{
		if (function == null)
		{
			return;
		}
		
		synchronized (this)
		{
			// Check if Calculator set is linked to the standard Calculator set of NPC
			if (_calculators == NPC_STD_CALCULATOR)
			{
				// Create a copy of the standard NPC Calculator set
				_calculators = new Calculator[Stats.NUM_STATS];
				
				for (int i = 0; i < Stats.NUM_STATS; i++)
				{
					if (NPC_STD_CALCULATOR[i] != null)
					{
						_calculators[i] = new Calculator(NPC_STD_CALCULATOR[i]);
					}
				}
			}
			
			// Select the Calculator of the affected state in the Calculator set
			int stat = function.getStat().ordinal();
			
			if (_calculators[stat] == null)
			{
				_calculators[stat] = new Calculator();
			}
			
			// Add the Func to the calculator corresponding to the state
			_calculators[stat].addFunc(function);
		}
	}
	
	/**
	 * Add a list of Funcs to the Calculator set of the L2Character.<br>
	 * <B><U>Concept</U>:</B><br>
	 * A L2Character owns a table of Calculators called <B>_calculators</B>.<br>
	 * Each Calculator (a calculator per state) own a table of Func object.<br>
	 * A Func object is a mathematic function that permit to calculate the modifier of a state (ex : REGENERATE_HP_RATE...).<br>
	 * <FONT COLOR=#FF0000><B> <U>Caution</U> : This method is ONLY for L2PcInstance</B></FONT><br>
	 * <B><U>Example of use</U>:</B>
	 * <ul>
	 * <li>Equip an item from inventory</li>
	 * <li>Learn a new passive skill</li>
	 * <li>Use an active skill</li>
	 * </ul>
	 * @param functions The list of Func objects to add to the Calculator corresponding to the state affected
	 */
	public final void addStatFuncs(List<AbstractFunction> functions)
	{
		final List<Stats> modifiedStats = new ArrayList<>();
		for (AbstractFunction f : functions)
		{
			modifiedStats.add(f.getStat());
			addStatFunc(f);
		}
		broadcastModifiedStats(modifiedStats);
	}
	
	/**
	 * Remove a Func from the Calculator set of the L2Character.<br>
	 * <B><U>Concept</U>:</B><br>
	 * A L2Character owns a table of Calculators called <B>_calculators</B>.<br>
	 * Each Calculator (a calculator per state) own a table of Func object.<br>
	 * A Func object is a mathematic function that permit to calculate the modifier of a state (ex : REGENERATE_HP_RATE...).<br>
	 * To reduce cache memory use, L2NPCInstances who don't have skills share the same Calculator set called <B>NPC_STD_CALCULATOR</B>.<br>
	 * That's why, if a L2NPCInstance is under a skill/spell effect that modify one of its state, a copy of the NPC_STD_CALCULATOR must be create in its _calculators before addind new Func object.<br>
	 * <B><U>Actions</U>:</B>
	 * <ul>
	 * <li>Remove the Func object from _calculators</li>
	 * <li>If L2Character is a L2NPCInstance and _calculators is equal to NPC_STD_CALCULATOR, free cache memory and just create a link on NPC_STD_CALCULATOR in _calculators</li>
	 * </ul>
	 * @param function The Func object to remove from the Calculator corresponding to the state affected
	 */
	public final void removeStatFunc(AbstractFunction function)
	{
		if (function == null)
		{
			return;
		}
		
		// Select the Calculator of the affected state in the Calculator set
		int stat = function.getStat().ordinal();
		
		synchronized (this)
		{
			if (_calculators[stat] == null)
			{
				return;
			}
			
			// Remove the Func object from the Calculator
			_calculators[stat].removeFunc(function);
			
			if (_calculators[stat].size() == 0)
			{
				_calculators[stat] = null;
			}
			
			// If possible, free the memory and just create a link on NPC_STD_CALCULATOR
			if (isNpc())
			{
				int i = 0;
				for (; i < Stats.NUM_STATS; i++)
				{
					if (!Calculator.equalsCals(_calculators[i], NPC_STD_CALCULATOR[i]))
					{
						break;
					}
				}
				
				if (i >= Stats.NUM_STATS)
				{
					_calculators = NPC_STD_CALCULATOR;
				}
			}
		}
	}
	
	/**
	 * Remove a list of Funcs from the Calculator set of the L2PcInstance.<br>
	 * <B><U>Concept</U>:</B><br>
	 * A L2Character owns a table of Calculators called <B>_calculators</B>.<br>
	 * Each Calculator (a calculator per state) own a table of Func object.<br>
	 * A Func object is a mathematic function that permit to calculate the modifier of a state (ex : REGENERATE_HP_RATE...).<br>
	 * <FONT COLOR=#FF0000><B> <U>Caution</U> : This method is ONLY for L2PcInstance</B></FONT><br>
	 * <B><U>Example of use</U>:</B>
	 * <ul>
	 * <li>Unequip an item from inventory</li>
	 * <li>Stop an active skill</li>
	 * </ul>
	 * @param functions The list of Func objects to add to the Calculator corresponding to the state affected
	 */
	public final void removeStatFuncs(AbstractFunction[] functions)
	{
		final List<Stats> modifiedStats = new ArrayList<>();
		for (AbstractFunction f : functions)
		{
			modifiedStats.add(f.getStat());
			removeStatFunc(f);
		}
		
		broadcastModifiedStats(modifiedStats);
	}
	
	/**
	 * Remove all Func objects with the selected owner from the Calculator set of the L2Character.<br>
	 * <B><U>Concept</U>:</B><br>
	 * A L2Character owns a table of Calculators called <B>_calculators</B>.<br>
	 * Each Calculator (a calculator per state) own a table of Func object.<br>
	 * A Func object is a mathematic function that permit to calculate the modifier of a state (ex : REGENERATE_HP_RATE...).<br>
	 * To reduce cache memory use, L2NPCInstances who don't have skills share the same Calculator set called <B>NPC_STD_CALCULATOR</B>.<br>
	 * That's why, if a L2NPCInstance is under a skill/spell effect that modify one of its state, a copy of the NPC_STD_CALCULATOR must be create in its _calculators before addind new Func object.<br>
	 * <B><U>Actions</U>:</B>
	 * <ul>
	 * <li>Remove all Func objects of the selected owner from _calculators</li>
	 * <li>If L2Character is a L2NPCInstance and _calculators is equal to NPC_STD_CALCULATOR, free cache memory and just create a link on NPC_STD_CALCULATOR in _calculators</li>
	 * </ul>
	 * <B><U>Example of use</U>:</B>
	 * <ul>
	 * <li>Unequip an item from inventory</li>
	 * <li>Stop an active skill</li>
	 * </ul>
	 * @param owner The Object(Skill, Item...) that has created the effect
	 */
	public final void removeStatsOwner(Object owner)
	{
		List<Stats> modifiedStats = null;
		int i = 0;
		// Go through the Calculator set
		synchronized (this)
		{
			for (Calculator calc : _calculators)
			{
				if (calc != null)
				{
					// Delete all Func objects of the selected owner
					if (modifiedStats != null)
					{
						modifiedStats.addAll(calc.removeOwner(owner));
					}
					else
					{
						modifiedStats = calc.removeOwner(owner);
					}
					
					if (calc.size() == 0)
					{
						_calculators[i] = null;
					}
				}
				i++;
			}
			
			// If possible, free the memory and just create a link on NPC_STD_CALCULATOR
			if (isNpc())
			{
				i = 0;
				for (; i < Stats.NUM_STATS; i++)
				{
					if (!Calculator.equalsCals(_calculators[i], NPC_STD_CALCULATOR[i]))
					{
						break;
					}
				}
				
				if (i >= Stats.NUM_STATS)
				{
					_calculators = NPC_STD_CALCULATOR;
				}
			}
			
			broadcastModifiedStats(modifiedStats);
		}
	}
	
	protected void broadcastModifiedStats(List<Stats> stats)
	{
		if ((stats == null) || stats.isEmpty())
		{
			return;
		}
		
		// Don't broadcast modified stats on login.
		if (isPlayer() && !getActingPlayer().isOnline())
		{
			return;
		}
		
		if (isSummon())
		{
			Summon summon = (Summon) this;
			if (summon.getOwner() != null)
			{
				summon.updateAndBroadcastStatus(1);
			}
		}
		else
		{
			boolean broadcastFull = true;
			StatusUpdate su = new StatusUpdate(this);
			UserInfo info = null;
			if (isPlayer())
			{
				info = new UserInfo(getActingPlayer(), false);
				info.addComponentType(UserInfoType.SLOTS, UserInfoType.ENCHANTLEVEL);
			}
			for (Stats stat : stats)
			{
				if (info != null)
				{
					switch (stat)
					{
						case MOVE_SPEED:
						{
							info.addComponentType(UserInfoType.MULTIPLIER);
							break;
						}
						case POWER_ATTACK_SPEED:
						{
							info.addComponentType(UserInfoType.MULTIPLIER, UserInfoType.STATS);
							break;
						}
						case POWER_ATTACK:
						case POWER_DEFENCE:
						case EVASION_RATE:
						case ACCURACY_COMBAT:
						case CRITICAL_RATE:
						case MCRITICAL_RATE:
						case MAGIC_EVASION_RATE:
						case ACCURACY_MAGIC:
						case MAGIC_ATTACK:
						case MAGIC_ATTACK_SPEED:
						case MAGIC_DEFENCE:
						{
							info.addComponentType(UserInfoType.STATS);
							break;
						}
						case MAX_CP:
						{
							su.addAttribute(StatusUpdate.MAX_CP, getMaxCp());
							break;
						}
						case MAX_HP:
						{
							su.addAttribute(StatusUpdate.MAX_HP, getMaxHp());
							break;
						}
						case MAX_MP:
						{
							su.addAttribute(StatusUpdate.MAX_CP, getMaxMp());
							break;
						}
						case STAT_STR:
						case STAT_CON:
						case STAT_DEX:
						case STAT_INT:
						case STAT_WIT:
						case STAT_MEN:
						{
							info.addComponentType(UserInfoType.BASE_STATS);
							break;
						}
						case FIRE_RES:
						case WATER_RES:
						case WIND_RES:
						case EARTH_RES:
						case HOLY_RES:
						case DARK_RES:
						{
							info.addComponentType(UserInfoType.ELEMENTALS);
							break;
						}
						case FIRE_POWER:
						case WATER_POWER:
						case WIND_POWER:
						case EARTH_POWER:
						case HOLY_POWER:
						case DARK_POWER:
						{
							info.addComponentType(UserInfoType.ATK_ELEMENTAL);
							break;
						}
					}
				}
			}
			
			if (isPlayer())
			{
				final PlayerInstance player = getActingPlayer();
				player.refreshOverloaded();
				player.refreshExpertisePenalty();
				sendPacket(info);
				
				if (broadcastFull)
				{
					player.broadcastCharInfo();
				}
				else
				{
					if (su.hasAttributes())
					{
						broadcastPacket(su);
					}
				}
				if (hasServitors() && isAffected(EffectFlag.SERVITOR_SHARE))
				{
					getServitors().values().forEach(Summon::broadcastStatusUpdate);
				}
			}
			else if (isNpc())
			{
				if (broadcastFull)
				{
					World.getInstance().forEachVisibleObject(this, PlayerInstance.class, player ->
					{
						if (!isVisibleFor(player))
						{
							return;
						}
						
						if (getRunSpeed() == 0)
						{
							player.sendPacket(new ServerObjectInfo((Npc) this, player));
						}
						else
						{
							player.sendPacket(new NpcInfo((Npc) this));
						}
					});
				}
				else if (su.hasAttributes())
				{
					broadcastPacket(su);
				}
			}
			else if (su.hasAttributes())
			{
				broadcastPacket(su);
			}
		}
	}
	
	public final int getXdestination()
	{
		MoveData m = _move;
		
		if (m != null)
		{
			return m._xDestination;
		}
		
		return getX();
	}
	
	/**
	 * @return the Y destination of the L2Character or the Y position if not in movement.
	 */
	public final int getYdestination()
	{
		MoveData m = _move;
		
		if (m != null)
		{
			return m._yDestination;
		}
		
		return getY();
	}
	
	/**
	 * @return the Z destination of the L2Character or the Z position if not in movement.
	 */
	public final int getZdestination()
	{
		MoveData m = _move;
		
		if (m != null)
		{
			return m._zDestination;
		}
		
		return getZ();
	}
	
	/**
	 * @return True if the L2Character is in combat.
	 */
	public boolean isInCombat()
	{
		return hasAI() && ((getAI().getAttackTarget() != null) || getAI().isAutoAttacking());
	}
	
	/**
	 * @return True if the L2Character is moving.
	 */
	public final boolean isMoving()
	{
		return _move != null;
	}
	
	/**
	 * @return True if the L2Character is travelling a calculated path.
	 */
	public final boolean isOnGeodataPath()
	{
		MoveData m = _move;
		if (m == null)
		{
			return false;
		}
		if (m.onGeodataPathIndex == -1)
		{
			return false;
		}
		if (m.onGeodataPathIndex == (m.geoPath.size() - 1))
		{
			return false;
		}
		return true;
	}
	
	/**
	 * @return True if the L2Character is casting.
	 */
	public final boolean isCastingNow()
	{
		return _isCastingNow;
	}
	
	public void setIsCastingNow(boolean value)
	{
		_isCastingNow = value;
	}
	
	public final boolean isCastingSimultaneouslyNow()
	{
		return _isCastingSimultaneouslyNow;
	}
	
	public void setIsCastingSimultaneouslyNow(boolean value)
	{
		_isCastingSimultaneouslyNow = value;
	}
	
	/**
	 * @return True if the cast of the L2Character can be aborted.
	 */
	public final boolean canAbortCast()
	{
		return _castInterruptTime > GameTimeController.getInstance().getGameTicks();
	}
	
	public int getCastInterruptTime()
	{
		return _castInterruptTime;
	}
	
	/**
	 * @return True if the L2Character is attacking.
	 */
	public final boolean isAttackingNow()
	{
		return _attackEndTime > System.currentTimeMillis();
	}
	
	/**
	 * Abort the attack of the L2Character and send Server->Client ActionFailed packet.
	 */
	public final void abortAttack()
	{
		if (isAttackingNow())
		{
			sendPacket(ActionFailed.STATIC_PACKET);
		}
	}
	
	/**
	 * Abort the cast of the L2Character and send Server->Client MagicSkillCanceld/ActionFailed packet.
	 */
	public final void abortCast()
	{
		if (isCastingNow() || isCastingSimultaneouslyNow())
		{
			Future<?> future = _skillCast;
			// cancels the skill hit scheduled task
			if (future != null)
			{
				future.cancel(true);
				_skillCast = null;
			}
			future = _skillCast2;
			if (future != null)
			{
				future.cancel(true);
				_skillCast2 = null;
			}
			
			// TODO: Handle removing spawned npc.
			
			if (isChanneling())
			{
				getSkillChannelizer().stopChanneling();
			}
			
			if (_allSkillsDisabled)
			{
				enableAllSkills(); // this remains for forced skill use, e.g. scroll of escape
			}
			setIsCastingNow(false);
			setIsCastingSimultaneouslyNow(false);
			// safeguard for cannot be interrupt any more
			_castInterruptTime = 0;
			if (isPlayer())
			{
				getAI().notifyEvent(CtrlEvent.EVT_FINISH_CASTING); // setting back previous intention
			}
			broadcastPacket(new MagicSkillCanceld(getObjectId())); // broadcast packet to stop animations client-side
			sendPacket(ActionFailed.STATIC_PACKET); // send an "action failed" packet to the caster
		}
	}
	
	/**
	 * Update the position of the L2Character during a movement and return True if the movement is finished.<br>
	 * <B><U>Concept</U>:</B><br>
	 * At the beginning of the move action, all properties of the movement are stored in the MoveData object called <B>_move</B> of the L2Character.<br>
	 * The position of the start point and of the destination permit to estimated in function of the movement speed the time to achieve the destination.<br>
	 * When the movement is started (ex : by MovetoLocation), this method will be called each 0.1 sec to estimate and update the L2Character position on the server.<br>
	 * Note, that the current server position can differe from the current client position even if each movement is straight foward.<br>
	 * That's why, client send regularly a Client->Server ValidatePosition packet to eventually correct the gap on the server.<br>
	 * But, it's always the server position that is used in range calculation. At the end of the estimated movement time,<br>
	 * the L2Character position is automatically set to the destination position even if the movement is not finished.<br>
	 * <FONT COLOR=#FF0000><B><U>Caution</U>: The current Z position is obtained FROM THE CLIENT by the Client->Server ValidatePosition Packet.<br>
	 * But x and y positions must be calculated to avoid that players try to modify their movement speed.</B></FONT>
	 * @return True if the movement is finished
	 */
	public boolean updatePosition()
	{
		// Get movement data
		MoveData m = _move;
		
		if (m == null)
		{
			return true;
		}
		
		if (!isVisible())
		{
			_move = null;
			return true;
		}
		
		// Check if this is the first update
		if (m._moveTimestamp == 0)
		{
			m._moveTimestamp = m._moveStartTime;
			m._xAccurate = getX();
			m._yAccurate = getY();
		}
		
		int gameTicks = GameTimeController.getInstance().getGameTicks();
		
		// Check if the position has already been calculated
		if (m._moveTimestamp == gameTicks)
		{
			return false;
		}
		
		int xPrev = getX();
		int yPrev = getY();
		int zPrev = getZ(); // the z coordinate may be modified by coordinate synchronizations
		
		double dx, dy, dz;
		if (Config.COORD_SYNCHRONIZE == 1)
		// the only method that can modify x,y while moving (otherwise _move would/should be set null)
		{
			dx = m._xDestination - xPrev;
			dy = m._yDestination - yPrev;
		}
		else
		// otherwise we need saved temporary values to avoid rounding errors
		{
			dx = m._xDestination - m._xAccurate;
			dy = m._yDestination - m._yAccurate;
		}
		
		final boolean isFloating = isFlying() || isInsideZone(ZoneId.WATER);
		
		// Z coordinate will follow geodata or client values
		if ((Config.COORD_SYNCHRONIZE == 2) && !isFloating && !m.disregardingGeodata && ((GameTimeController.getInstance().getGameTicks() % 10) == 0 // once a second to reduce possible cpu load
		) && GeoData.getInstance().hasGeo(xPrev, yPrev))
		{
			int geoHeight = GeoData.getInstance().getSpawnHeight(xPrev, yPrev, zPrev);
			dz = m._zDestination - geoHeight;
			// quite a big difference, compare to validatePosition packet
			if (isPlayer() && (Math.abs(getActingPlayer().getClientZ() - geoHeight) > 200) && (Math.abs(getActingPlayer().getClientZ() - geoHeight) < 1500))
			{
				dz = m._zDestination - zPrev; // allow diff
			}
			else if (isInCombat() && (Math.abs(dz) > 200) && (((dx * dx) + (dy * dy)) < 40000)) // allow mob to climb up to pcinstance
			{
				dz = m._zDestination - zPrev; // climbing
			}
			else
			{
				zPrev = geoHeight;
			}
		}
		else
		{
			dz = m._zDestination - zPrev;
		}
		
		double delta = (dx * dx) + (dy * dy);
		if ((delta < 10000) && ((dz * dz) > 2500) // close enough, allows error between client and server geodata if it cannot be avoided
			&& !isFloating)
		{
			delta = Math.sqrt(delta);
		}
		else
		{
			delta = Math.sqrt(delta + (dz * dz));
		}
		
		double distFraction = Double.MAX_VALUE;
		if (delta > 1)
		{
			final double distPassed = (getMoveSpeed() * (gameTicks - m._moveTimestamp)) / GameTimeController.TICKS_PER_SECOND;
			distFraction = distPassed / delta;
		}
		
		// if (Config.DEVELOPER) _log.warn("Move Ticks:" + (gameTicks - m._moveTimestamp) + ", distPassed:" + distPassed + ", distFraction:" + distFraction);
		
		if (distFraction > 1)
		{
			// Set the position of the L2Character to the destination
			super.setXYZ(m._xDestination, m._yDestination, m._zDestination);
		}
		else
		{
			m._xAccurate += dx * distFraction;
			m._yAccurate += dy * distFraction;
			
			// Set the position of the L2Character to estimated after parcial move
			super.setXYZ((int) (m._xAccurate), (int) (m._yAccurate), zPrev + (int) ((dz * distFraction) + 0.5));
		}
		revalidateZone(false);
		
		// Set the timer of last position update to now
		m._moveTimestamp = gameTicks;
		
		if (distFraction > 1)
		{
			ThreadPoolManager.getInstance().executeAi(() -> getAI().notifyEvent(CtrlEvent.EVT_ARRIVED));
			return true;
		}
		
		return false;
	}
	
	public void revalidateZone(boolean force)
	{
		// This function is called too often from movement code
		if (force)
		{
			_zoneValidateCounter = 4;
		}
		else
		{
			_zoneValidateCounter--;
			if (_zoneValidateCounter < 0)
			{
				_zoneValidateCounter = 4;
			}
			else
			{
				return;
			}
		}
		
		ZoneManager.getInstance().getRegion(this).revalidateZones(this);
	}
	
	/**
	 * Stop movement of the L2Character (Called by AI Accessor only).<br>
	 * <B><U>Actions</U>:</B>
	 * <ul>
	 * <li>Delete movement data of the L2Character</li>
	 * <li>Set the current position (x,y,z), its current L2WorldRegion if necessary and its heading</li>
	 * <li>Remove the L2Object object from _gmList of GmListTable</li>
	 * <li>Remove object from _knownObjects and _knownPlayer of all surrounding L2WorldRegion L2Characters</li>
	 * </ul>
	 * <FONT COLOR=#FF0000><B><U>Caution</U>: This method DOESN'T send Server->Client packet StopMove/StopRotation</B></FONT>
	 * @param loc
	 */
	public void stopMove(Location loc)
	{
		// Delete movement data of the L2Character
		_move = null;
		
		// All data are contained in a Location object
		if (loc != null)
		{
			setXYZ(loc.getX(), loc.getY(), loc.getZ());
			setHeading(loc.getHeading());
			revalidateZone(true);
		}
		broadcastPacket(new StopMove(this));
	}
	
	/**
	 * @return Returns the showSummonAnimation.
	 */
	public boolean isShowSummonAnimation()
	{
		return _showSummonAnimation;
	}
	
	/**
	 * @param showSummonAnimation The showSummonAnimation to set.
	 */
	public void setShowSummonAnimation(boolean showSummonAnimation)
	{
		_showSummonAnimation = showSummonAnimation;
	}
	
	/**
	 * Target a L2Object (add the target to the L2Character _target, _knownObject and L2Character to _KnownObject of the L2Object).<br>
	 * <B><U>Concept</U>:</B><br>
	 * The L2Object (including L2Character) targeted is identified in <B>_target</B> of the L2Character.<br>
	 * <B><U>Actions</U>:</B>
	 * <ul>
	 * <li>Set the _target of L2Character to L2Object</li>
	 * <li>If necessary, add L2Object to _knownObject of the L2Character</li>
	 * <li>If necessary, add L2Character to _KnownObject of the L2Object</li>
	 * <li>If object==null, cancel Attak or Cast</li>
	 * </ul>
	 * @param object L2object to target
	 */
	public void setTarget(WorldObject object)
	{
		if ((object != null) && !object.isVisible())
		{
			object = null;
		}
		
		_target = object;
	}
	
	/**
	 * @return the identifier of the L2Object targeted or -1.
	 */
	public final int getTargetId()
	{
		if (_target != null)
		{
			return _target.getObjectId();
		}
		return 0;
	}
	
	/**
	 * @return the L2Object targeted or null.
	 */
	public final WorldObject getTarget()
	{
		return _target;
	}
	
	// called from AIAccessor only
	/**
	 * Calculate movement data for a move to location action and add the L2Character to movingObjects of GameTimeController (only called by AI Accessor).<br>
	 * <B><U>Concept</U>:</B><br>
	 * At the beginning of the move action, all properties of the movement are stored in the MoveData object called <B>_move</B> of the L2Character.<br>
	 * The position of the start point and of the destination permit to estimated in function of the movement speed the time to achieve the destination.<br>
	 * All L2Character in movement are identified in <B>movingObjects</B> of GameTimeController that will call the updatePosition method of those L2Character each 0.1s.<br>
	 * <B><U>Actions</U>:</B>
	 * <ul>
	 * <li>Get current position of the L2Character</li>
	 * <li>Calculate distance (dx,dy) between current position and destination including offset</li>
	 * <li>Create and Init a MoveData object</li>
	 * <li>Set the L2Character _move object to MoveData object</li>
	 * <li>Add the L2Character to movingObjects of the GameTimeController</li>
	 * <li>Create a task to notify the AI that L2Character arrives at a check point of the movement</li>
	 * </ul>
	 * <FONT COLOR=#FF0000><B><U>Caution</U>: This method DOESN'T send Server->Client packet MoveToPawn/CharMoveToLocation.</B></FONT><br>
	 * <B><U>Example of use</U>:</B>
	 * <ul>
	 * <li>AI : onIntentionMoveTo(Location), onIntentionPickUp(L2Object), onIntentionInteract(L2Object)</li>
	 * <li>FollowTask</li>
	 * </ul>
	 * @param x The X position of the destination
	 * @param y The Y position of the destination
	 * @param z The Y position of the destination
	 * @param offset The size of the interaction area of the L2Character targeted
	 */
	public void moveToLocation(int x, int y, int z, int offset)
	{
		// Get the Move Speed of the L2Charcater
		double speed = getMoveSpeed();
		if ((speed <= 0) || isMovementDisabled())
		{
			return;
		}
		
		// Get current position of the L2Character
		final int curX = super.getX();
		final int curY = super.getY();
		final int curZ = super.getZ();
		
		// Calculate distance (dx,dy) between current position and destination
		// TODO: improve Z axis move/follow support when dx,dy are small compared to dz
		double dx = (x - curX);
		double dy = (y - curY);
		double dz = (z - curZ);
		double distance = Math.sqrt((dx * dx) + (dy * dy));
		
		final boolean verticalMovementOnly = isFlying() && (distance == 0) && (dz != 0);
		if (verticalMovementOnly)
		{
			distance = Math.abs(dz);
		}
		
		// make water move short and use no geodata checks for swimming chars
		// distance in a click can easily be over 3000
		if (isInsideZone(ZoneId.WATER) && (distance > 700))
		{
			double divider = 700 / distance;
			x = curX + (int) (divider * dx);
			y = curY + (int) (divider * dy);
			z = curZ + (int) (divider * dz);
			dx = (x - curX);
			dy = (y - curY);
			dz = (z - curZ);
			distance = Math.sqrt((dx * dx) + (dy * dy));
		}
		
		// Define movement angles needed
		// ^
		// | X (x,y)
		// | /
		// | /distance
		// | /
		// |/ angle
		// X ---------->
		// (curx,cury)
		
		double cos;
		double sin;
		
		// Check if a movement offset is defined or no distance to go through
		if ((offset > 0) || (distance < 1))
		{
			// approximation for moving closer when z coordinates are different
			// TODO: handle Z axis movement better
			offset -= Math.abs(dz);
			if (offset < 5)
			{
				offset = 5;
			}
			
			// If no distance to go through, the movement is canceled
			if ((distance < 1) || ((distance - offset) <= 0))
			{
				// Notify the AI that the L2Character is arrived at destination
				getAI().notifyEvent(CtrlEvent.EVT_ARRIVED);
				
				return;
			}
			// Calculate movement angles needed
			sin = dy / distance;
			cos = dx / distance;
			
			distance -= (offset - 5); // due to rounding error, we have to move a bit closer to be in range
			
			// Calculate the new destination with offset included
			x = curX + (int) (distance * cos);
			y = curY + (int) (distance * sin);
		}
		else
		{
			// Calculate movement angles needed
			sin = dy / distance;
			cos = dx / distance;
		}
		
		// Create and Init a MoveData object
		MoveData m = new MoveData();
		
		// GEODATA MOVEMENT CHECKS AND PATHFINDING
		m.onGeodataPathIndex = -1; // Initialize not on geodata path
		m.disregardingGeodata = false;
		
		if (!isFlying() // flying chars not checked - even canSeeTarget doesn't work yet
			&& (!isInsideZone(ZoneId.WATER) || isInsideZone(ZoneId.SIEGE))) // swimming also not checked unless in siege zone - but distance is limited
		{
			final boolean isInVehicle = isPlayer() && (getActingPlayer().getVehicle() != null);
			if (isInVehicle)
			{
				m.disregardingGeodata = true;
			}
			
			double originalDistance = distance;
			int originalX = x;
			int originalY = y;
			int originalZ = z;
			int gtx = (originalX - World.MAP_MIN_X) >> 4;
			int gty = (originalY - World.MAP_MIN_Y) >> 4;
			
			// Movement checks:
			// when PATHFINDING > 0, for all characters except mobs returning home (could be changed later to teleport if pathfinding fails)
			if (((Config.PATHFINDING > 0) && (!(isAttackable() && ((Attackable) this).isReturningToSpawnPoint()))) || (isPlayer() && !(isInVehicle && (distance > 1500))) || (isSummon() && !(getAI().getIntention() == AI_INTENTION_FOLLOW)) // assuming intention_follow only when following owner
				|| isAfraid())
			{
				if (isOnGeodataPath())
				{
					try
					{
						if ((gtx == _move.geoPathGtx) && (gty == _move.geoPathGty))
						{
							return;
						}
						_move.onGeodataPathIndex = -1; // Set not on geodata path
					}
					catch (NullPointerException e)
					{
						// nothing
					}
				}
				
				if ((curX < World.MAP_MIN_X) || (curX > World.MAP_MAX_X) || (curY < World.MAP_MIN_Y) || (curY > World.MAP_MAX_Y))
				{
					// Temporary fix for character outside world region errors
					_log.warn("Character " + getName() + " outside world area, in coordinates x:" + curX + " y:" + curY);
					getAI().setIntention(CtrlIntention.AI_INTENTION_IDLE);
					if (isPlayer())
					{
						getActingPlayer().logout();
					}
					else if (isSummon())
					{
						return; // preventation when summon get out of world coords, player will not loose him, unsummon handled from pcinstance
					}
					else
					{
						onDecay();
					}
					return;
				}
				Location destiny = GeoData.getInstance().moveCheck(curX, curY, curZ, x, y, z, getInstanceId());
				// location different if destination wasn't reached (or just z coord is different)
				x = destiny.getX();
				y = destiny.getY();
				z = destiny.getZ();
				dx = x - curX;
				dy = y - curY;
				dz = z - curZ;
				distance = verticalMovementOnly ? Math.abs(dz * dz) : Math.sqrt((dx * dx) + (dy * dy));
			}
			// Pathfinding checks. Only when geodata setting is 2, the LoS check gives shorter result
			// than the original movement was and the LoS gives a shorter distance than 2000
			// This way of detecting need for pathfinding could be changed.
			if ((Config.PATHFINDING > 0) && ((originalDistance - distance) > 30) && (distance < 2000) && !isAfraid())
			{
				// Path calculation
				// Overrides previous movement check
				if ((isPlayable() && !isInVehicle) || isMinion() || isInCombat())
				{
					m.geoPath = PathFinding.getInstance().findPath(curX, curY, curZ, originalX, originalY, originalZ, getInstanceId(), isPlayable());
					if ((m.geoPath == null) || (m.geoPath.size() < 2)) // No path found
					{
						// * Even though there's no path found (remember geonodes aren't perfect),
						// the mob is attacking and right now we set it so that the mob will go
						// after target anyway, is dz is small enough.
						// * With cellpathfinding this approach could be changed but would require taking
						// off the geonodes and some more checks.
						// * Summons will follow their masters no matter what.
						// * Currently minions also must move freely since L2AttackableAI commands
						// them to move along with their leader
						if (isPlayer() || (!isPlayable() && !isMinion() && (Math.abs(z - curZ) > 140)) || (isSummon() && !((Summon) this).getFollowStatus()))
						{
							getAI().setIntention(CtrlIntention.AI_INTENTION_IDLE);
							return;
						}
						
						m.disregardingGeodata = true;
						x = originalX;
						y = originalY;
						z = originalZ;
						distance = originalDistance;
					}
					else
					{
						m.onGeodataPathIndex = 0; // on first segment
						m.geoPathGtx = gtx;
						m.geoPathGty = gty;
						m.geoPathAccurateTx = originalX;
						m.geoPathAccurateTy = originalY;
						
						x = m.geoPath.get(m.onGeodataPathIndex).getX();
						y = m.geoPath.get(m.onGeodataPathIndex).getY();
						z = m.geoPath.get(m.onGeodataPathIndex).getZ();
						
						// check for doors in the route
						if (DoorData.getInstance().checkIfDoorsBetween(curX, curY, curZ, x, y, z, getInstanceId()))
						{
							m.geoPath = null;
							getAI().setIntention(CtrlIntention.AI_INTENTION_IDLE);
							return;
						}
						for (int i = 0; i < (m.geoPath.size() - 1); i++)
						{
							if (DoorData.getInstance().checkIfDoorsBetween(m.geoPath.get(i), m.geoPath.get(i + 1), getInstanceId()))
							{
								m.geoPath = null;
								getAI().setIntention(CtrlIntention.AI_INTENTION_IDLE);
								return;
							}
						}
						
						dx = x - curX;
						dy = y - curY;
						dz = z - curZ;
						distance = verticalMovementOnly ? Math.abs(dz * dz) : Math.sqrt((dx * dx) + (dy * dy));
						sin = dy / distance;
						cos = dx / distance;
					}
				}
			}
			// If no distance to go through, the movement is canceled
			if ((distance < 1) && ((Config.PATHFINDING > 0) || isPlayable()))
			{
				if (isSummon())
				{
					((Summon) this).setFollowStatus(false);
				}
				getAI().setIntention(CtrlIntention.AI_INTENTION_IDLE);
				return;
			}
		}
		
		// Apply Z distance for flying or swimming for correct timing calculations
		if ((isFlying() || isInsideZone(ZoneId.WATER)) && !verticalMovementOnly)
		{
			distance = Math.sqrt((distance * distance) + (dz * dz));
		}
		
		// Caclulate the Nb of ticks between the current position and the destination
		// One tick added for rounding reasons
		int ticksToMove = 1 + (int) ((GameTimeController.TICKS_PER_SECOND * distance) / speed);
		m._xDestination = x;
		m._yDestination = y;
		m._zDestination = z; // this is what was requested from client
		
		// Calculate and set the heading of the L2Character
		m._heading = 0; // initial value for coordinate sync
		// Does not broke heading on vertical movements
		if (!verticalMovementOnly)
		{
			setHeading(Util.calculateHeadingFrom(cos, sin));
		}
		
		m._moveStartTime = GameTimeController.getInstance().getGameTicks();
		
		// Set the L2Character _move object to MoveData object
		_move = m;
		
		// Add the L2Character to movingObjects of the GameTimeController
		// The GameTimeController manage objects movement
		GameTimeController.getInstance().registerMovingObject(this);
		
		// Create a task to notify the AI that L2Character arrives at a check point of the movement
		if ((ticksToMove * GameTimeController.MILLIS_IN_TICK) > 3000)
		{
			ThreadPoolManager.getInstance().scheduleAi(new NotifyAITask(this, CtrlEvent.EVT_ARRIVED_REVALIDATE), 2000);
		}
		
		// the CtrlEvent.EVT_ARRIVED will be sent when the character will actually arrive
		// to destination by GameTimeController
	}
	
	public boolean moveToNextRoutePoint()
	{
		if (!isOnGeodataPath())
		{
			// Cancel the move action
			_move = null;
			return false;
		}
		
		// Get the Move Speed of the L2Charcater
		double speed = getMoveSpeed();
		if ((speed <= 0) || isMovementDisabled())
		{
			// Cancel the move action
			_move = null;
			return false;
		}
		
		MoveData md = _move;
		if (md == null)
		{
			return false;
		}
		
		// Create and Init a MoveData object
		MoveData m = new MoveData();
		
		// Update MoveData object
		m.onGeodataPathIndex = md.onGeodataPathIndex + 1; // next segment
		m.geoPath = md.geoPath;
		m.geoPathGtx = md.geoPathGtx;
		m.geoPathGty = md.geoPathGty;
		m.geoPathAccurateTx = md.geoPathAccurateTx;
		m.geoPathAccurateTy = md.geoPathAccurateTy;
		
		if (md.onGeodataPathIndex == (md.geoPath.size() - 2))
		{
			m._xDestination = md.geoPathAccurateTx;
			m._yDestination = md.geoPathAccurateTy;
			m._zDestination = md.geoPath.get(m.onGeodataPathIndex).getZ();
		}
		else
		{
			m._xDestination = md.geoPath.get(m.onGeodataPathIndex).getX();
			m._yDestination = md.geoPath.get(m.onGeodataPathIndex).getY();
			m._zDestination = md.geoPath.get(m.onGeodataPathIndex).getZ();
		}
		double dx = (m._xDestination - super.getX());
		double dy = (m._yDestination - super.getY());
		double distance = Math.sqrt((dx * dx) + (dy * dy));
		// Calculate and set the heading of the L2Character
		if (distance != 0)
		{
			setHeading(Util.calculateHeadingFrom(getX(), getY(), m._xDestination, m._yDestination));
		}
		
		// Caclulate the Nb of ticks between the current position and the destination
		// One tick added for rounding reasons
		int ticksToMove = 1 + (int) ((GameTimeController.TICKS_PER_SECOND * distance) / speed);
		
		m._heading = 0; // initial value for coordinate sync
		
		m._moveStartTime = GameTimeController.getInstance().getGameTicks();
		
		// Set the L2Character _move object to MoveData object
		_move = m;
		
		// Add the L2Character to movingObjects of the GameTimeController
		// The GameTimeController manage objects movement
		GameTimeController.getInstance().registerMovingObject(this);
		
		// Create a task to notify the AI that L2Character arrives at a check point of the movement
		if ((ticksToMove * GameTimeController.MILLIS_IN_TICK) > 3000)
		{
			ThreadPoolManager.getInstance().scheduleAi(new NotifyAITask(this, CtrlEvent.EVT_ARRIVED_REVALIDATE), 2000);
		}
		
		// the CtrlEvent.EVT_ARRIVED will be sent when the character will actually arrive
		// to destination by GameTimeController
		
		// Send a Server->Client packet CharMoveToLocation to the actor and all L2PcInstance in its _knownPlayers
		MoveToLocation msg = new MoveToLocation(this);
		broadcastPacket(msg);
		
		return true;
	}
	
	public boolean validateMovementHeading(int heading)
	{
		MoveData m = _move;
		
		if (m == null)
		{
			return true;
		}
		
		boolean result = true;
		if (m._heading != heading)
		{
			result = (m._heading == 0); // initial value or false
			m._heading = heading;
		}
		
		return result;
	}
	
	/**
	 * Check if this object is inside the given radius around the given point.
	 * @param loc Location of the target
	 * @param radius the radius around the target
	 * @param checkZAxis should we check Z axis also
	 * @param strictCheck true if (distance < radius), false if (distance <= radius)
	 * @return true if the L2Character is inside the radius.
	 */
	public final boolean isInsideRadius(ILocational loc, int radius, boolean checkZAxis, boolean strictCheck)
	{
		return isInsideRadius(loc.getX(), loc.getY(), loc.getZ(), radius, checkZAxis, strictCheck);
	}
	
	/**
	 * Check if this object is inside the given radius around the given point.
	 * @param x X position of the target
	 * @param y Y position of the target
	 * @param z Z position of the target
	 * @param radius the radius around the target
	 * @param checkZAxis should we check Z axis also
	 * @param strictCheck true if (distance < radius), false if (distance <= radius)
	 * @return true if the L2Character is inside the radius.
	 */
	public final boolean isInsideRadius(int x, int y, int z, int radius, boolean checkZAxis, boolean strictCheck)
	{
		final double distance = calculateDistance(x, y, z, checkZAxis, true);
		return (strictCheck) ? (distance < (radius * radius)) : (distance <= (radius * radius));
	}
	
	/**
	 * <B><U> Overridden in </U> :</B> <li>L2PcInstance</li>
	 * @return True if arrows are available.
	 */
	protected boolean checkAndEquipArrows()
	{
		return true;
	}
	
	/**
	 * <B><U> Overridden in </U> :</B> <li>L2PcInstance</li>
	 * @return True if bolts are available.
	 */
	protected boolean checkAndEquipBolts()
	{
		return true;
	}
	
	/**
	 * Add Exp and Sp to the L2Character.<br>
	 * <B><U> Overridden in </U> :</B> <li>L2PcInstance</li> <li>L2PetInstance</li>
	 * @param addToExp
	 * @param addToSp
	 */
	public void addExpAndSp(long addToExp, long addToSp)
	{
		// Dummy method (overridden by players and pets)
	}
	
	/**
	 * <B><U> Overridden in </U> :</B> <li>L2PcInstance</li>
	 * @return the active weapon instance (always equiped in the right hand).
	 */
	public abstract ItemInstance getActiveWeaponInstance();
	
	/**
	 * <B><U> Overridden in </U> :</B> <li>L2PcInstance</li>
	 * @return the active weapon item (always equiped in the right hand).
	 */
	public abstract Weapon getActiveWeaponItem();
	
	/**
	 * <B><U> Overridden in </U> :</B> <li>L2PcInstance</li>
	 * @return the secondary weapon instance (always equiped in the left hand).
	 */
	public abstract ItemInstance getSecondaryWeaponInstance();
	
	/**
	 * <B><U> Overridden in </U> :</B> <li>L2PcInstance</li>
	 * @return the secondary {@link L2Item} item (always equiped in the left hand).
	 */
	public abstract L2Item getSecondaryWeaponItem();
	
	/**
	 * Manage hit process (called by Hit Task).<br>
	 * <B><U>Actions</U>:</B>
	 * <ul>
	 * <li>If the attacker/target is dead or use fake death, notify the AI with EVT_CANCEL and send a Server->Client packet ActionFailed (if attacker is a L2PcInstance)</li>
	 * <li>If attack isn't aborted, send a message system (critical hit, missed...) to attacker/target if they are L2PcInstance</li>
	 * <li>If attack isn't aborted and hit isn't missed, reduce HP of the target and calculate reflection damage to reduce HP of attacker if necessary</li>
	 * <li>if attack isn't aborted and hit isn't missed, manage attack or cast break of the target (calculating rate, sending message...)</li>
	 * </ul>
	 * @param target The L2Character targeted
	 * @param damage Nb of HP to reduce
	 * @param crit True if hit is critical
	 * @param miss True if hit is missed
	 * @param soulshot True if SoulShot are charged
	 * @param shld True if shield is efficient
	 */
	public void onHitTimer(Creature target, int damage, boolean crit, boolean miss, boolean soulshot, byte shld)
	{
		// If the attacker/target is dead or use fake death, notify the AI with EVT_CANCEL
		// and send a Server->Client packet ActionFailed (if attacker is a L2PcInstance)
		if ((target == null) || isAlikeDead() || (isNpc() && ((Npc) this).isEventMob()))
		{
			getAI().notifyEvent(CtrlEvent.EVT_CANCEL);
			return;
		}
		
		if ((isNpc() && target.isAlikeDead()) || target.isDead() || (!isInSurroundingRegion(target) && !isDoor()))
		{
			// getAI().setIntention(CtrlIntention.AI_INTENTION_ACTIVE, null);
			getAI().notifyEvent(CtrlEvent.EVT_CANCEL);
			
			sendPacket(ActionFailed.STATIC_PACKET);
			return;
		}
		
		if (miss)
		{
			// Notify target AI
			if (target.hasAI())
			{
				target.getAI().notifyEvent(CtrlEvent.EVT_EVADED, this);
			}
			notifyAttackAvoid(target, false);
		}
		
		// Send message about damage/crit or miss
		sendDamageMessage(target, damage, false, crit, miss);
		
		// Check Raidboss attack
		// Character will be petrified if attacking a raid that's more
		// than 8 levels lower
		if (target.isRaid() && target.giveRaidCurse() && !Config.RAID_DISABLE_CURSE)
		{
			if (getLevel() > (target.getLevel() + 8))
			{
				Skill skill = CommonSkill.RAID_CURSE2.getSkill();
				
				if (skill != null)
				{
					abortAttack();
					abortCast();
					getAI().setIntention(CtrlIntention.AI_INTENTION_IDLE);
					skill.applyEffects(target, this);
				}
				else
				{
					_log.warn("Skill 4515 at level 1 is missing in DP.");
				}
				
				damage = 0; // prevents messing up drop calculation
			}
		}
		
		// If L2Character target is a L2PcInstance, send a system message
		if (target.isPlayer())
		{
			PlayerInstance enemy = target.getActingPlayer();
			enemy.getAI().clientStartAutoAttack();
		}
		
		if (!miss && (damage > 0))
		{
			Weapon weapon = getActiveWeaponItem();
			boolean isBow = ((weapon != null) && weapon.isBowOrCrossBow());
			int reflectedDamage = 0;
			
			if (!isBow && !target.isInvul()) // Do not reflect if weapon is of type bow or target is invunlerable
			{
				// quick fix for no drop from raid if boss attack high-level char with damage reflection
				if (!target.isRaid() || (getActingPlayer() == null) || (getActingPlayer().getLevel() <= (target.getLevel() + 8)))
				{
					// Reduce HP of the target and calculate reflection damage to reduce HP of attacker if necessary
					double reflectPercent = target.getStat().calcStat(Stats.REFLECT_DAMAGE_PERCENT, 0, null, null);
					
					if (reflectPercent > 0)
					{
						reflectedDamage = (int) ((reflectPercent / 100.) * damage);
						
						if (reflectedDamage > target.getMaxHp())
						{
							reflectedDamage = target.getMaxHp();
						}
						
						// Cannot reflect more damage than your own P.Def.
						if (reflectedDamage > getStat().getPDef(target))
						{
							reflectedDamage = getStat().getPDef(target);
						}
					}
				}
			}
			
			// reduce targets HP
			target.reduceCurrentHp(damage, this, null);
			target.notifyDamageReceived(damage, this, null, crit, false, false);
			
			// When killing blow is made, the target doesn't reflect.
			if ((reflectedDamage > 0) && !target.isDead())
			{
				reduceCurrentHp(reflectedDamage, target, true, false, null);
				notifyDamageReceived(reflectedDamage, target, null, crit, false, true);
			}
			
			if (!isBow) // Do not absorb if weapon is of type bow
			{
				// Absorb HP from the damage inflicted
				double absorbPercent = getStat().calcStat(Stats.ABSORB_DAMAGE_PERCENT, 0, null, null);
				
				if (absorbPercent > 0)
				{
					int maxCanAbsorb = (int) (getMaxRecoverableHp() - getCurrentHp());
					int absorbDamage = (int) ((absorbPercent / 100.) * damage);
					
					if (absorbDamage > maxCanAbsorb)
					{
						absorbDamage = maxCanAbsorb; // Can't absord more than max hp
					}
					
					if (absorbDamage > 0)
					{
						setCurrentHp(getCurrentHp() + absorbDamage);
					}
				}
				
				// Absorb MP from the damage inflicted
				absorbPercent = getStat().calcStat(Stats.ABSORB_MANA_DAMAGE_PERCENT, 0, null, null);
				
				if (absorbPercent > 0)
				{
					int maxCanAbsorb = (int) (getMaxRecoverableMp() - getCurrentMp());
					int absorbDamage = (int) ((absorbPercent / 100.) * damage);
					
					if (absorbDamage > maxCanAbsorb)
					{
						absorbDamage = maxCanAbsorb; // Can't absord more than max hp
					}
					
					if (absorbDamage > 0)
					{
						setCurrentMp(getCurrentMp() + absorbDamage);
					}
				}
				
			}
			
			// Notify AI with EVT_ATTACKED
			if (target.hasAI())
			{
				target.getAI().notifyEvent(CtrlEvent.EVT_ATTACKED, this);
			}
			getAI().clientStartAutoAttack();
			if (isSummon())
			{
				PlayerInstance owner = ((Summon) this).getOwner();
				if (owner != null)
				{
					owner.getAI().clientStartAutoAttack();
				}
			}
			
			// Manage attack or cast break of the target (calculating rate, sending message...)
			if (!target.isRaid() && Formulas.calcAtkBreak(target, damage))
			{
				target.breakAttack();
				target.breakCast();
			}
			
			if (_triggerSkills != null)
			{
				for (OptionsSkillHolder holder : _triggerSkills.values())
				{
					if ((!crit && (holder.getSkillType() == OptionsSkillType.ATTACK)) || ((holder.getSkillType() == OptionsSkillType.CRITICAL) && crit))
					{
						if (Rnd.get(100) < holder.getChance())
						{
							makeTriggerCast(holder.getSkill(), target);
						}
					}
				}
			}
			// Launch weapon Special ability effect if available
			if (crit && (weapon != null))
			{
				weapon.castOnCriticalSkill(this, target);
			}
		}
		
		// Recharge any active auto-soulshot tasks for current creature.
		rechargeShots(true, false);
	}
	
	/**
	 * Break an attack and send Server->Client ActionFailed packet and a System Message to the L2Character.
	 */
	public void breakAttack()
	{
		if (isAttackingNow())
		{
			// Abort the attack of the L2Character and send Server->Client ActionFailed packet
			abortAttack();
			if (isPlayer())
			{
				// Send a system message
				sendPacket(SystemMessageId.YOUR_ATTACK_HAS_FAILED);
			}
		}
	}
	
	/**
	 * Break a cast and send Server->Client ActionFailed packet and a System Message to the L2Character.
	 */
	public void breakCast()
	{
		// damage can only cancel magical & static skills
		if (isCastingNow() && canAbortCast() && (getLastSkillCast() != null) && (getLastSkillCast().isMagic() || getLastSkillCast().isStatic()))
		{
			// Abort the cast of the L2Character and send Server->Client MagicSkillCanceld/ActionFailed packet.
			abortCast();
			
			if (isPlayer())
			{
				// Send a system message
				sendPacket(SystemMessageId.YOUR_CASTING_HAS_BEEN_INTERRUPTED);
			}
		}
	}
	
	/**
	 * Reduce the arrow number of the L2Character.<br>
	 * <B><U> Overridden in </U> :</B> <li>L2PcInstance</li>
	 * @param bolts
	 */
	protected void reduceArrowCount(boolean bolts)
	{
		// default is to do nothing
	}
	
	/**
	 * Manage Forced attack (shift + select target).<br>
	 * <B><U>Actions</U>:</B>
	 * <ul>
	 * <li>If L2Character or target is in a town area, send a system message TARGET_IN_PEACEZONE a Server->Client packet ActionFailed</li>
	 * <li>If target is confused, send a Server->Client packet ActionFailed</li>
	 * <li>If L2Character is a L2ArtefactInstance, send a Server->Client packet ActionFailed</li>
	 * <li>Send a Server->Client packet MyTargetSelected to start attack and Notify AI with AI_INTENTION_ATTACK</li>
	 * </ul>
	 * @param player The L2PcInstance to attack
	 */
	@Override
	public void onForcedAttack(PlayerInstance player)
	{
		if (isInsidePeaceZone(player))
		{
			// If L2Character or target is in a peace zone, send a system message TARGET_IN_PEACEZONE a Server->Client packet ActionFailed
			player.sendPacket(SystemMessageId.YOU_MAY_NOT_ATTACK_THIS_TARGET_IN_A_PEACEFUL_ZONE);
			player.sendPacket(ActionFailed.STATIC_PACKET);
			return;
		}
		if (player.isInOlympiadMode() && (player.getTarget() != null) && player.getTarget().isPlayable())
		{
			PlayerInstance target = null;
			WorldObject object = player.getTarget();
			if ((object != null) && object.isPlayable())
			{
				target = object.getActingPlayer();
			}
			
			if ((target == null) || (target.isInOlympiadMode() && (!player.isOlympiadStart() || (player.getOlympiadGameId() != target.getOlympiadGameId()))))
			{
				// if L2PcInstance is in Olympia and the match isn't already start, send a Server->Client packet ActionFailed
				player.sendPacket(ActionFailed.STATIC_PACKET);
				return;
			}
		}
		if ((player.getTarget() != null) && !player.getTarget().canBeAttacked() && !player.getAccessLevel().allowPeaceAttack())
		{
			// If target is not attackable, send a Server->Client packet ActionFailed
			player.sendPacket(ActionFailed.STATIC_PACKET);
			return;
		}
		if (player.isConfused())
		{
			// If target is confused, send a Server->Client packet ActionFailed
			player.sendPacket(ActionFailed.STATIC_PACKET);
			return;
		}
		// GeoData Los Check or dz > 1000
		if (!GeoData.getInstance().canSeeTarget(player, this))
		{
			player.sendPacket(SystemMessageId.CANNOT_SEE_TARGET);
			player.sendPacket(ActionFailed.STATIC_PACKET);
			return;
		}
		if (player.getBlockCheckerArena() != -1)
		{
			player.sendPacket(ActionFailed.STATIC_PACKET);
			return;
		}
		// Notify AI with AI_INTENTION_ATTACK
		player.getAI().setIntention(CtrlIntention.AI_INTENTION_ATTACK, this);
	}
	
	/**
	 * @param attacker
	 * @return True if inside peace zone.
	 */
	public boolean isInsidePeaceZone(PlayerInstance attacker)
	{
		return isInsidePeaceZone(attacker, this);
	}
	
	public boolean isInsidePeaceZone(PlayerInstance attacker, WorldObject target)
	{
		return (!attacker.getAccessLevel().allowPeaceAttack() && isInsidePeaceZone((WorldObject) attacker, target));
	}
	
	public boolean isInsidePeaceZone(WorldObject attacker, WorldObject target)
	{
		if (target == null)
		{
			return false;
		}
		if (!(target.isPlayable() && attacker.isPlayable()))
		{
			return false;
		}
		if (InstanceManager.getInstance().getInstance(getInstanceId()).isPvPInstance())
		{
			return false;
		}
		
		if (Config.ALT_GAME_KARMA_PLAYER_CAN_BE_KILLED_IN_PEACEZONE)
		{
			// allows red to be attacked and red to attack flagged players
			if ((target.getActingPlayer() != null) && (target.getActingPlayer().getReputation() < 0))
			{
				return false;
			}
			if ((attacker.getActingPlayer() != null) && (attacker.getActingPlayer().getReputation() < 0) && (target.getActingPlayer() != null) && (target.getActingPlayer().getPvpFlag() > 0))
			{
				return false;
			}
		}
		return (target.isInsideZone(ZoneId.PEACE) || attacker.isInsideZone(ZoneId.PEACE));
	}
	
	/**
	 * @return true if this character is inside an active grid.
	 */
	public boolean isInActiveRegion()
	{
		WorldRegion region = getWorldRegion();
		return ((region != null) && (region.isActive()));
	}
	
	/**
	 * @return True if the L2Character has a Party in progress.
	 */
	public boolean isInParty()
	{
		return false;
	}
	
	/**
	 * @return the L2Party object of the L2Character.
	 */
	public Party getParty()
	{
		return null;
	}
	
	/**
	 * @param target
	 * @param weapon
	 * @return the Attack Speed of the L2Character (delay (in milliseconds) before next attack).
	 */
	public int calculateTimeBetweenAttacks(Creature target, Weapon weapon)
	{
		if ((weapon != null) && !isTransformed())
		{
			switch (weapon.getItemType())
			{
				case BOW:
					return (1500 * 345) / getPAtkSpd();
				case CROSSBOW:
				case TWOHANDCROSSBOW:
					return (1200 * 345) / getPAtkSpd();
				case DAGGER:
					// atkSpd /= 1.15;
					break;
			}
		}
		return Formulas.calcPAtkSpd(this, target, getPAtkSpd());
	}
	
	public int calculateReuseTime(Creature target, Weapon weapon)
	{
		if ((weapon == null) || isTransformed())
		{
			return 0;
		}
		
		int reuse = weapon.getReuseDelay();
		// only bows should continue for now
		if (reuse == 0)
		{
			return 0;
		}
		
		reuse *= getStat().getWeaponReuseModifier(target);
		double atkSpd = getStat().getPAtkSpd();
		switch (weapon.getItemType())
		{
			case BOW:
			case CROSSBOW:
			case TWOHANDCROSSBOW:
				return (int) ((reuse * 345) / atkSpd);
			default:
				return (int) ((reuse * 312) / atkSpd);
		}
	}
	
	/**
	 * Add a skill to the L2Character _skills and its Func objects to the calculator set of the L2Character.<br>
	 * <B><U>Concept</U>:</B><br>
	 * All skills own by a L2Character are identified in <B>_skills</B><br>
	 * <B><U>Actions</U>:</B>
	 * <ul>
	 * <li>Replace oldSkill by newSkill or Add the newSkill</li>
	 * <li>If an old skill has been replaced, remove all its Func objects of L2Character calculator set</li>
	 * <li>Add Func objects of newSkill to the calculator set of the L2Character</li>
	 * </ul>
	 * <B><U>Overridden in</U>:</B>
	 * <ul>
	 * <li>L2PcInstance : Save update in the character_skills table of the database</li>
	 * </ul>
	 * @param newSkill The L2Skill to add to the L2Character
	 * @return The L2Skill replaced or null if just added a new L2Skill
	 */
	@Override
	public Skill addSkill(Skill newSkill)
	{
		Skill oldSkill = null;
		if (newSkill != null)
		{
			// Replace oldSkill by newSkill or Add the newSkill
			oldSkill = _skills.put(newSkill.getId(), newSkill);
			// If an old skill has been replaced, remove all its Func objects
			if (oldSkill != null)
			{
				removeStatsOwner(oldSkill);
			}
			// Add Func objects of newSkill to the calculator set of the L2Character
			addStatFuncs(newSkill.getStatFuncs(null, this));
			
			if (newSkill.isPassive())
			{
				newSkill.applyEffects(this, null, this, false, true, false, 0);
			}
		}
		return oldSkill;
	}
	
	public Skill removeSkill(Skill skill, boolean cancelEffect)
	{
		return (skill != null) ? removeSkill(skill.getId(), cancelEffect) : null;
	}
	
	public Skill removeSkill(int skillId)
	{
		return removeSkill(skillId, true);
	}
	
	public Skill removeSkill(int skillId, boolean cancelEffect)
	{
		// Remove the skill from the L2Character _skills
		Skill oldSkill = _skills.remove(skillId);
		// Remove all its Func objects from the L2Character calculator set
		if (oldSkill != null)
		{
			
			// Stop casting if this skill is used right now
			if ((getLastSkillCast() != null) && isCastingNow())
			{
				if (oldSkill.getId() == getLastSkillCast().getId())
				{
					abortCast();
				}
			}
			if ((getLastSimultaneousSkillCast() != null) && isCastingSimultaneouslyNow())
			{
				if (oldSkill.getId() == getLastSimultaneousSkillCast().getId())
				{
					abortCast();
				}
			}
			
			// Stop effects.
			if (cancelEffect || oldSkill.isToggle() || oldSkill.isPassive())
			{
				removeStatsOwner(oldSkill);
				stopSkillEffects(true, oldSkill.getId());
			}
		}
		
		return oldSkill;
	}
	
	/**
	 * <B><U>Concept</U>:</B><br>
	 * All skills own by a L2Character are identified in <B>_skills</B> the L2Character
	 * @return all skills own by the L2Character in a table of L2Skill.
	 */
	public final Collection<Skill> getAllSkills()
	{
		return new ArrayList<>(_skills.values());
	}
	
	/**
	 * @return the map containing this character skills.
	 */
	@Override
	public Map<Integer, Skill> getSkills()
	{
		return _skills;
	}
	
	/**
	 * Return the level of a skill owned by the L2Character.
	 * @param skillId The identifier of the L2Skill whose level must be returned
	 * @return The level of the L2Skill identified by skillId
	 */
	@Override
	public int getSkillLevel(int skillId)
	{
		final Skill skill = getKnownSkill(skillId);
		return (skill == null) ? -1 : skill.getLevel();
	}
	
	/**
	 * @param skillId The identifier of the L2Skill to check the knowledge
	 * @return the skill from the known skill.
	 */
	@Override
	public final Skill getKnownSkill(int skillId)
	{
		return _skills.get(skillId);
	}
	
	/**
	 * Return the number of buffs affecting this L2Character.
	 * @return The number of Buffs affecting this L2Character
	 */
	public int getBuffCount()
	{
		return _effectList.getBuffCount();
	}
	
	public int getDanceCount()
	{
		return _effectList.getDanceCount();
	}
	
	/**
	 * Manage the magic skill launching task (MP, HP, Item consumation...) and display the magic skill animation on client.<br>
	 * <B><U>Actions</U>:</B>
	 * <ul>
	 * <li>Send a Server->Client packet MagicSkillLaunched (to display magic skill animation) to all L2PcInstance of L2Charcater _knownPlayers</li>
	 * <li>Consumme MP, HP and Item if necessary</li>
	 * <li>Send a Server->Client packet StatusUpdate with MP modification to the L2PcInstance</li>
	 * <li>Launch the magic skill in order to calculate its effects</li>
	 * <li>If the skill type is PDAM, notify the AI of the target with AI_INTENTION_ATTACK</li>
	 * <li>Notify the AI of the L2Character with EVT_FINISH_CASTING</li>
	 * </ul>
	 * <FONT COLOR=#FF0000><B><U>Caution</U>: A magic skill casting MUST BE in progress</B></FONT>
	 * @param mut
	 */
	public void onMagicLaunchedTimer(MagicUseTask mut)
	{
		final Skill skill = mut.getSkill();
		WorldObject[] targets = mut.getTargets();
		
		if ((skill == null) || (targets == null))
		{
			abortCast();
			return;
		}
		
		if (targets.length == 0)
		{
			switch (skill.getTargetType())
			{
			// only AURA-type skills can be cast without target
				case AURA:
				case FRONT_AURA:
				case BEHIND_AURA:
				case AURA_CORPSE_MOB:
					break;
				default:
					abortCast();
					return;
			}
		}
		
		// Escaping from under skill's radius and peace zone check. First version, not perfect in AoE skills.
		int escapeRange = 0;
		if (skill.getEffectRange() > escapeRange)
		{
			escapeRange = skill.getEffectRange();
		}
		else if ((skill.getCastRange() < 0) && (skill.getAffectRange() > 80))
		{
			escapeRange = skill.getAffectRange();
		}
		
		if ((targets.length > 0) && (escapeRange > 0))
		{
			int _skiprange = 0;
			int _skippeace = 0;
			List<Creature> targetList = new ArrayList<>(targets.length);
			for (WorldObject target : targets)
			{
				if (target instanceof Creature)
				{
					if (!isInsideRadius(target.getX(), target.getY(), target.getZ(), escapeRange + getTemplate().getCollisionRadius(), true, false))
					{
						_skiprange++;
						continue;
					}
					
					if (skill.isBad())
					{
						if (isPlayer())
						{
							if (((Creature) target).isInsidePeaceZone(getActingPlayer()))
							{
								_skippeace++;
								continue;
							}
						}
						else
						{
							if (((Creature) target).isInsidePeaceZone(this, target))
							{
								_skippeace++;
								continue;
							}
						}
					}
					targetList.add((Creature) target);
				}
			}
			if (targetList.isEmpty())
			{
				if (isPlayer())
				{
					if (_skiprange > 0)
					{
						sendPacket(SystemMessageId.THE_DISTANCE_IS_TOO_FAR_AND_SO_THE_CASTING_HAS_BEEN_STOPPED);
					}
					else if (_skippeace > 0)
					{
						sendPacket(SystemMessageId.A_MALICIOUS_SKILL_CANNOT_BE_USED_IN_A_PEACE_ZONE);
					}
				}
				abortCast();
				return;
			}
			mut.setTargets(targetList.toArray(new Creature[targetList.size()]));
		}
		
		// Ensure that a cast is in progress
		// Check if player is using fake death.
		// Static skills can be used while faking death.
		if ((mut.isSimultaneous() && !isCastingSimultaneouslyNow()) || (!mut.isSimultaneous() && !isCastingNow()) || (isAlikeDead() && !skill.isStatic()))
		{
			// now cancels both, simultaneous and normal
			getAI().notifyEvent(CtrlEvent.EVT_CANCEL);
			return;
		}
		
		if (!skill.isToggle())
		{
			broadcastPacket(new MagicSkillLaunched(this, skill.getDisplayId(), skill.getDisplayLevel(), targets));
		}
		
		mut.setPhase(2);
		if (mut.getSkillTime() == 0)
		{
			onMagicHitTimer(mut);
		}
		else
		{
			_skillCast = ThreadPoolManager.getInstance().scheduleEffect(mut, 400);
		}
	}
	
	// Runs in the end of skill casting
	public void onMagicHitTimer(MagicUseTask mut)
	{
		final Skill skill = mut.getSkill();
		final WorldObject[] targets = mut.getTargets();
		
		if ((skill == null) || (targets == null))
		{
			abortCast();
			return;
		}
		
		try
		{
			// Go through targets table
			for (WorldObject tgt : targets)
			{
				if (tgt.isPlayable())
				{
					if (isPlayer() && tgt.isSummon())
					{
						((Summon) tgt).updateAndBroadcastStatus(1);
					}
				}
				else if (isPlayable() && tgt.isAttackable())
				{
					Creature target = (Creature) tgt;
					if (skill.getEffectPoint() > 0)
					{
						((Attackable) target).reduceHate(this, skill.getEffectPoint());
					}
					else if (skill.getEffectPoint() < 0)
					{
						((Attackable) target).addDamageHate(this, 0, -skill.getEffectPoint());
					}
				}
			}
			
			rechargeShots(skill.useSoulShot(), skill.useSpiritShot());
			
			final StatusUpdate su = new StatusUpdate(this);
			boolean isSendStatus = false;
			
			// Consume MP of the L2Character and Send the Server->Client packet StatusUpdate with current HP and MP to all other L2PcInstance to inform
			double mpConsume = getStat().getMpConsume(skill);
			
			if (mpConsume > 0)
			{
				if (mpConsume > getCurrentMp())
				{
					sendPacket(SystemMessageId.NOT_ENOUGH_MP);
					abortCast();
					return;
				}
				
				getStatus().reduceMp(mpConsume);
				su.addAttribute(StatusUpdate.CUR_MP, (int) getCurrentMp());
				isSendStatus = true;
			}
			
			// Consume HP if necessary and Send the Server->Client packet StatusUpdate with current HP and MP to all other L2PcInstance to inform
			if (skill.getHpConsume() > 0)
			{
				double consumeHp = skill.getHpConsume();
				
				if (consumeHp >= getCurrentHp())
				{
					sendPacket(SystemMessageId.NOT_ENOUGH_HP);
					abortCast();
					return;
				}
				
				getStatus().reduceHp(consumeHp, this, true);
				
				su.addAttribute(StatusUpdate.CUR_HP, (int) getCurrentHp());
				isSendStatus = true;
			}
			
			// Send a Server->Client packet StatusUpdate with MP modification to the L2PcInstance
			if (isSendStatus)
			{
				sendPacket(su);
			}
			
			if (isPlayer())
			{
				// Consume Charges
				if (skill.getChargeConsume() > 0)
				{
					getActingPlayer().decreaseCharges(skill.getChargeConsume());
				}
				
				// Consume Souls if necessary
				if (skill.getMaxSoulConsumeCount() > 0)
				{
					if (!getActingPlayer().decreaseSouls(skill.getMaxSoulConsumeCount(), skill))
					{
						abortCast();
						return;
					}
				}
			}
			
			// Launch the magic skill in order to calculate its effects
			callSkill(mut.getSkill(), mut.getTargets());
		}
		catch (NullPointerException e)
		{
			_log.warn("", e);
		}
		
		if (mut.getSkillTime() > 0)
		{
			mut.setCount(mut.getCount() + 1);
		}
		
		mut.setPhase(3);
		if (mut.getSkillTime() == 0)
		{
			onMagicFinalizer(mut);
		}
		else
		{
			if (mut.isSimultaneous())
			{
				_skillCast2 = ThreadPoolManager.getInstance().scheduleEffect(mut, 0);
			}
			else
			{
				_skillCast = ThreadPoolManager.getInstance().scheduleEffect(mut, 0);
			}
		}
	}
	
	// Runs after skillTime
	public void onMagicFinalizer(MagicUseTask mut)
	{
		if (mut.isSimultaneous())
		{
			_skillCast2 = null;
			setIsCastingSimultaneouslyNow(false);
			return;
		}
		
		// Cleanup
		_skillCast = null;
		_castInterruptTime = 0;
		
		// Stop casting
		setIsCastingNow(false);
		
		final Skill skill = mut.getSkill();
		final WorldObject target = mut.getTargets().length > 0 ? mut.getTargets()[0] : null;
		
		// On each repeat recharge shots before cast.
		if (mut.getCount() > 0)
		{
			rechargeShots(mut.getSkill().useSoulShot(), mut.getSkill().useSpiritShot());
		}
		
		// Attack target after skill use
		if ((skill.nextActionIsAttack()) && (getTarget() instanceof Creature) && (getTarget() != this) && (target != null) && (getTarget() == target) && target.canBeAttacked())
		{
			if ((getAI().getNextIntention() == null) || (getAI().getNextIntention().getCtrlIntention() != CtrlIntention.AI_INTENTION_MOVE_TO))
			{
				getAI().setIntention(CtrlIntention.AI_INTENTION_ATTACK, target);
			}
		}
		
		if (skill.isBad() && (skill.getTargetType() != L2TargetType.UNLOCKABLE))
		{
			getAI().clientStartAutoAttack();
		}
		
		// Notify the AI of the L2Character with EVT_FINISH_CASTING
		getAI().notifyEvent(CtrlEvent.EVT_FINISH_CASTING);
		
		final Creature creatureTarget = ((target != null) && target.isCreature()) ? (Creature) target : null;
		EventDispatcher.getInstance().notifyEvent(new OnCreatureSkillFinishCast(this, skill, mut.isSimultaneous(), creatureTarget, mut.getTargets()), this);
		
		// Notify DP Scripts
		notifyQuestEventSkillFinished(skill, target);
		
		// If character is a player, then wipe their current cast state and check if a skill is queued.
		// If there is a queued skill, launch it and wipe the queue.
		if (isPlayer())
		{
			PlayerInstance currPlayer = getActingPlayer();
			SkillUseHolder queuedSkill = currPlayer.getQueuedSkill();
			
			currPlayer.setCurrentSkill(null, false, false);
			
			if (queuedSkill != null)
			{
				currPlayer.setQueuedSkill(null, false, false);
				
				// DON'T USE : Recursive call to useMagic() method
				// currPlayer.useMagic(queuedSkill.getSkill(), queuedSkill.isCtrlPressed(), queuedSkill.isShiftPressed());
				ThreadPoolManager.getInstance().executeGeneral(new QueuedMagicUseTask(currPlayer, queuedSkill.getSkill(), queuedSkill.isCtrlPressed(), queuedSkill.isShiftPressed()));
			}
		}
		
		if (isChanneling())
		{
			getSkillChannelizer().stopChanneling();
		}
	}
	
	// Quest event ON_SPELL_FNISHED
	protected void notifyQuestEventSkillFinished(Skill skill, WorldObject target)
	{
		
	}
	
	/**
	 * Launch the magic skill and calculate its effects on each target contained in the targets table.
	 * @param skill The L2Skill to use
	 * @param targets The table of L2Object targets
	 */
	public void callSkill(Skill skill, WorldObject[] targets)
	{
		try
		{
			Weapon activeWeapon = getActiveWeaponItem();
			
			// Check if the toggle skill effects are already in progress on the L2Character
			if (skill.isToggle() && isAffectedBySkill(skill.getId()))
			{
				return;
			}
			
			// Initial checks
			for (WorldObject obj : targets)
			{
				if ((obj == null) || !obj.isCreature())
				{
					continue;
				}
				
				final Creature target = (Creature) obj;
				// Check raid monster attack and check buffing characters who attack raid monsters.
				Creature targetsAttackTarget = null;
				Creature targetsCastTarget = null;
				if (target.hasAI())
				{
					targetsAttackTarget = target.getAI().getAttackTarget();
					targetsCastTarget = target.getAI().getCastTarget();
				}
				
				if (!Config.RAID_DISABLE_CURSE && ((target.isRaid() && target.giveRaidCurse() && (getLevel() > (target.getLevel() + 8))) || (!skill.isBad() && (targetsAttackTarget != null) && targetsAttackTarget.isRaid() && targetsAttackTarget.giveRaidCurse() && targetsAttackTarget.getAttackByList().contains(target) && (getLevel() > (targetsAttackTarget.getLevel() + 8))) || (!skill.isBad() && (targetsCastTarget != null) && targetsCastTarget.isRaid() && targetsCastTarget.giveRaidCurse() && targetsCastTarget.getAttackByList().contains(target) && (getLevel() > (targetsCastTarget.getLevel() + 8)))))
				{
					final CommonSkill curse = skill.isMagic() ? CommonSkill.RAID_CURSE : CommonSkill.RAID_CURSE2;
					Skill curseSkill = curse.getSkill();
					if (curseSkill != null)
					{
						abortAttack();
						abortCast();
						getAI().setIntention(CtrlIntention.AI_INTENTION_IDLE);
						curseSkill.applyEffects(target, this);
					}
					else
					{
						_log.warn("Skill ID " + curse.getId() + " level " + curse.getLevel() + " is missing in DP!");
					}
					return;
				}
				
				// Check if over-hit is possible
				if (skill.isOverhit())
				{
					if (target.isAttackable())
					{
						((Attackable) target).overhitEnabled(true);
					}
				}
				
				// Static skills not trigger any chance skills
				if (!skill.isStatic())
				{
					// Launch weapon Special ability skill effect if available
					if ((activeWeapon != null) && !target.isDead())
					{
						activeWeapon.castOnMagicSkill(this, target, skill);
					}
					
					if (_triggerSkills != null)
					{
						for (OptionsSkillHolder holder : _triggerSkills.values())
						{
							if ((skill.isMagic() && (holder.getSkillType() == OptionsSkillType.MAGIC)) || (skill.isPhysical() && (holder.getSkillType() == OptionsSkillType.ATTACK)))
							{
								if (Rnd.get(100) < holder.getChance())
								{
									makeTriggerCast(holder.getSkill(), target);
								}
							}
						}
					}
				}
			}
			
			// Launch the magic skill and calculate its effects
			skill.activateSkill(this, targets);
			
			PlayerInstance player = getActingPlayer();
			if (player != null)
			{
				for (WorldObject target : targets)
				{
					// EVT_ATTACKED and PvPStatus
					if (target instanceof Creature)
					{
						if (skill.getEffectPoint() <= 0)
						{
							if ((target.isPlayable() || target.isTrap()) && skill.isBad())
							{
								// Casted on target_self but don't harm self
								if (!target.equals(this))
								{
									// Combat-mode check
									if (target.isPlayer())
									{
										target.getActingPlayer().getAI().clientStartAutoAttack();
									}
									else if (target.isSummon() && ((Creature) target).hasAI())
									{
										PlayerInstance owner = ((Summon) target).getOwner();
										if (owner != null)
										{
											owner.getAI().clientStartAutoAttack();
										}
									}
									
									// attack of the own pet does not flag player
									// triggering trap not flag trap owner
									if ((player.getPet() != target) && !player.hasServitor(target.getObjectId()) && !isTrap() && !((skill.getEffectPoint() == 0) && (skill.getAffectRange() > 0)))
									{
										player.updatePvPStatus((Creature) target);
									}
								}
							}
							else if (target.isAttackable())
							{
								switch (skill.getId())
								{
									case 51: // Lure
									case 511: // Temptation
										break;
									default:
										// add attacker into list
										((Creature) target).addAttackerToAttackByList(this);
								}
							}
							// notify target AI about the attack
							if (((Creature) target).hasAI() && !skill.hasEffectType(L2EffectType.HATE))
							{
								((Creature) target).getAI().notifyEvent(CtrlEvent.EVT_ATTACKED, this);
							}
						}
						else
						{
							if (target.isPlayer())
							{
								// Casting non offensive skill on player with pvp flag set or with karma
								if (!(target.equals(this) || target.equals(player)) && ((target.getActingPlayer().getPvpFlag() > 0) || (target.getActingPlayer().getReputation() < 0)))
								{
									player.updatePvPStatus();
								}
							}
							else if (target.isAttackable())
							{
								player.updatePvPStatus();
							}
						}
					}
				}
				
				// Mobs in range 1000 see spell
				World.getInstance().forEachVisibleObjectInRange(player, Npc.class, 1000, npcMob ->
				{
					EventDispatcher.getInstance().notifyEventAsync(new OnNpcSkillSee(npcMob, player, skill, targets, isSummon()), npcMob);
					
					// On Skill See logic
					if (npcMob.isAttackable())
					{
						final Attackable attackable = (Attackable) npcMob;
						
						int skillEffectPoint = skill.getEffectPoint();
						
						if (player.hasSummon())
						{
							if (targets.length == 1)
							{
								if (CommonUtil.contains(targets, player.getPet()))
								{
									skillEffectPoint = 0;
								}
								for (Summon servitor : player.getServitors().values())
								{
									if (CommonUtil.contains(targets, servitor))
									{
										skillEffectPoint = 0;
									}
								}
							}
						}
						
						if (skillEffectPoint > 0)
						{
							if (attackable.hasAI() && (attackable.getAI().getIntention() == AI_INTENTION_ATTACK))
							{
								WorldObject npcTarget = attackable.getTarget();
								for (WorldObject skillTarget : targets)
								{
									if ((npcTarget == skillTarget) || (npcMob == skillTarget))
									{
										Creature originalCaster = isSummon() ? this : player;
										attackable.addDamageHate(originalCaster, 0, (skillEffectPoint * 150) / (attackable.getLevel() + 7));
									}
								}
							}
						}
					}
				});
			}
			// Notify AI
			if (skill.isBad() && !skill.hasEffectType(L2EffectType.HATE))
			{
				for (WorldObject target : targets)
				{
					if ((target instanceof Creature) && ((Creature) target).hasAI())
					{
						// notify target AI about the attack
						((Creature) target).getAI().notifyEvent(CtrlEvent.EVT_ATTACKED, this);
					}
				}
			}
		}
		catch (Exception e)
		{
			_log.warn(getClass().getSimpleName() + ": callSkill() failed.", e);
		}
	}
	
	/**
	 * @param target
	 * @return True if the L2Character is behind the target and can't be seen.
	 */
	public boolean isBehind(WorldObject target)
	{
		double angleChar, angleTarget, angleDiff, maxAngleDiff = 60;
		
		if (target == null)
		{
			return false;
		}
		
		if (target instanceof Creature)
		{
			Creature target1 = (Creature) target;
			angleChar = Util.calculateAngleFrom(this, target1);
			angleTarget = Util.convertHeadingToDegree(target1.getHeading());
			angleDiff = angleChar - angleTarget;
			if (angleDiff <= (-360 + maxAngleDiff))
			{
				angleDiff += 360;
			}
			if (angleDiff >= (360 - maxAngleDiff))
			{
				angleDiff -= 360;
			}
			if (Math.abs(angleDiff) <= maxAngleDiff)
			{
				return true;
			}
		}
		return false;
	}
	
	public boolean isBehindTarget()
	{
		return isBehind(getTarget());
	}
	
	/**
	 * @param isAttacking if its an attack to be check, or the character itself.
	 * @return
	 */
	public boolean isBehindTarget(boolean isAttacking)
	{
		if (isAttacking && isAffected(EffectFlag.ATTACK_BEHIND))
		{
			return true;
		}
		
		return isBehind(getTarget());
	}
	
	/**
	 * @param target
	 * @return True if the target is facing the L2Character.
	 */
	public boolean isInFrontOf(Creature target)
	{
		double angleChar, angleTarget, angleDiff, maxAngleDiff = 60;
		if (target == null)
		{
			return false;
		}
		
		angleTarget = Util.calculateAngleFrom(target, this);
		angleChar = Util.convertHeadingToDegree(target.getHeading());
		angleDiff = angleChar - angleTarget;
		if (angleDiff <= (-360 + maxAngleDiff))
		{
			angleDiff += 360;
		}
		if (angleDiff >= (360 - maxAngleDiff))
		{
			angleDiff -= 360;
		}
		return Math.abs(angleDiff) <= maxAngleDiff;
	}
	
	/**
	 * @param target
	 * @param maxAngle
	 * @return true if target is in front of L2Character (shield def etc)
	 */
	public boolean isFacing(WorldObject target, int maxAngle)
	{
		double angleChar, angleTarget, angleDiff, maxAngleDiff;
		if (target == null)
		{
			return false;
		}
		maxAngleDiff = maxAngle / 2.;
		angleTarget = Util.calculateAngleFrom(this, target);
		angleChar = Util.convertHeadingToDegree(getHeading());
		angleDiff = angleChar - angleTarget;
		if (angleDiff <= (-360 + maxAngleDiff))
		{
			angleDiff += 360;
		}
		if (angleDiff >= (360 - maxAngleDiff))
		{
			angleDiff -= 360;
		}
		return Math.abs(angleDiff) <= maxAngleDiff;
	}
	
	public boolean isInFrontOfTarget()
	{
		WorldObject target = getTarget();
		if (target instanceof Creature)
		{
			return isInFrontOf((Creature) target);
		}
		return false;
	}
	
	/**
	 * @return the Level Modifier ((level + 89) / 100).
	 */
	public double getLevelMod()
	{
		return ((getLevel() + 89) / 100d);
	}
	
	public final void setSkillCast(Future<?> newSkillCast)
	{
		_skillCast = newSkillCast;
	}
	
	/**
	 * Sets _isCastingNow to true and _castInterruptTime is calculated from end time (ticks)
	 * @param newSkillCastEndTick
	 */
	public final void forceIsCasting(int newSkillCastEndTick)
	{
		setIsCastingNow(true);
		// for interrupt -400 ms
		_castInterruptTime = newSkillCastEndTick - 4;
	}
	
	private boolean _AIdisabled = false;
	
	public void updatePvPFlag(int value)
	{
		// Overridden in L2PcInstance
	}
	
	/**
	 * @return a multiplier based on weapon random damage
	 */
	public final double getRandomDamageMultiplier()
	{
		Weapon activeWeapon = getActiveWeaponItem();
		int random;
		
		if (activeWeapon != null)
		{
			random = activeWeapon.getRandomDamage();
		}
		else
		{
			random = 5 + (int) Math.sqrt(getLevel());
		}
		
		random = (int) calcStat(Stats.RANDOM_DAMAGE, random);
		
		return (1 + ((double) Rnd.get(-random, random) / 100));
	}
	
	public final long getAttackEndTime()
	{
		return _attackEndTime;
	}
	
	public int getBowAttackEndTime()
	{
		return _disableBowAttackEndTime;
	}
	
	/**
	 * Not Implemented.
	 * @return
	 */
	public abstract int getLevel();
	
	public final double calcStat(Stats stat, double init)
	{
		return getStat().calcStat(stat, init, null, null);
	}
	
	// Stat - NEED TO REMOVE ONCE L2CHARSTAT IS COMPLETE
	public final double calcStat(Stats stat, double init, Creature target, Skill skill)
	{
		return getStat().calcStat(stat, init, target, skill);
	}
	
	public int getAccuracy()
	{
		return getStat().getAccuracy();
	}
	
	public int getMagicAccuracy()
	{
		return getStat().getMagicAccuracy();
	}
	
	public int getMagicEvasionRate(Creature target)
	{
		return getStat().getMagicEvasionRate(target);
	}
	
	public final float getAttackSpeedMultiplier()
	{
		return getStat().getAttackSpeedMultiplier();
	}
	
	public final double getCriticalDmg(Creature target, double init)
	{
		return getStat().getCriticalDmg(target, init);
	}
	
	public int getCriticalHit(Creature target, Skill skill)
	{
		return getStat().getCriticalHit(target, skill);
	}
	
	public int getEvasionRate(Creature target)
	{
		return getStat().getEvasionRate(target);
	}
	
	public final int getMagicalAttackRange(Skill skill)
	{
		return getStat().getMagicalAttackRange(skill);
	}
	
	public final int getMaxCp()
	{
		return getStat().getMaxCp();
	}
	
	public final int getMaxRecoverableCp()
	{
		return getStat().getMaxRecoverableCp();
	}
	
	public int getMAtk(Creature target, Skill skill)
	{
		return getStat().getMAtk(target, skill);
	}
	
	public int getMAtkSpd()
	{
		return getStat().getMAtkSpd();
	}
	
	public int getMaxMp()
	{
		return getStat().getMaxMp();
	}
	
	public int getMaxRecoverableMp()
	{
		return getStat().getMaxRecoverableMp();
	}
	
	public int getMaxHp()
	{
		return getStat().getMaxHp();
	}
	
	public int getMaxRecoverableHp()
	{
		return getStat().getMaxRecoverableHp();
	}
	
	public final int getMCriticalHit(Creature target, Skill skill)
	{
		return getStat().getMCriticalHit(target, skill);
	}
	
	public int getMDef(Creature target, Skill skill)
	{
		return getStat().getMDef(target, skill);
	}
	
	public double getMReuseRate(Skill skill)
	{
		return getStat().getMReuseRate(skill);
	}
	
	public int getPAtk(Creature target)
	{
		return getStat().getPAtk(target);
	}
	
	public int getPAtkSpd()
	{
		return getStat().getPAtkSpd();
	}
	
	public int getPDef(Creature target)
	{
		return getStat().getPDef(target);
	}
	
	public final int getPhysicalAttackRange()
	{
		return getStat().getPhysicalAttackRange();
	}
	
	public double getMovementSpeedMultiplier()
	{
		return getStat().getMovementSpeedMultiplier();
	}
	
	public double getRunSpeed()
	{
		return getStat().getRunSpeed();
	}
	
	public double getWalkSpeed()
	{
		return getStat().getWalkSpeed();
	}
	
	public final double getSwimRunSpeed()
	{
		return getStat().getSwimRunSpeed();
	}
	
	public final double getSwimWalkSpeed()
	{
		return getStat().getSwimWalkSpeed();
	}
	
	public double getMoveSpeed()
	{
		return getStat().getMoveSpeed();
	}
	
	public final int getShldDef()
	{
		return getStat().getShldDef();
	}
	
	public int getSTR()
	{
		return getStat().getSTR();
	}
	
	public int getDEX()
	{
		return getStat().getDEX();
	}
	
	public int getCON()
	{
		return getStat().getCON();
	}
	
	public int getINT()
	{
		return getStat().getINT();
	}
	
	public int getWIT()
	{
		return getStat().getWIT();
	}
	
	public int getMEN()
	{
		return getStat().getMEN();
	}
	
	public int getLUC()
	{
		return getStat().getLUC();
	}
	
	public int getCHA()
	{
		return getStat().getCHA();
	}
	
	// Status - NEED TO REMOVE ONCE L2CHARTATUS IS COMPLETE
	public void addStatusListener(Creature object)
	{
		getStatus().addStatusListener(object);
	}
	
	public void reduceCurrentHp(double i, Creature attacker, Skill skill)
	{
		reduceCurrentHp(i, attacker, true, false, skill);
	}
	
	public void reduceCurrentHpByDOT(double i, Creature attacker, Skill skill)
	{
		reduceCurrentHp(i, attacker, !skill.isToggle(), true, skill);
	}
	
	public void reduceCurrentHp(double i, Creature attacker, boolean awake, boolean isDOT, Skill skill)
	{
		if (Config.L2JMOD_CHAMPION_ENABLE && isChampion() && (Config.L2JMOD_CHAMPION_HP != 0))
		{
			getStatus().reduceHp(i / Config.L2JMOD_CHAMPION_HP, attacker, awake, isDOT, false);
		}
		else
		{
			getStatus().reduceHp(i, attacker, awake, isDOT, false);
		}
	}
	
	public void reduceCurrentMp(double i)
	{
		getStatus().reduceMp(i);
	}
	
	@Override
	public void removeStatusListener(Creature object)
	{
		getStatus().removeStatusListener(object);
	}
	
	protected void stopHpMpRegeneration()
	{
		getStatus().stopHpMpRegeneration();
	}
	
	public final double getCurrentCp()
	{
		return getStatus().getCurrentCp();
	}
	
	public final void setCurrentCp(double newCp)
	{
		getStatus().setCurrentCp(newCp);
	}
	
	public final double getCurrentHp()
	{
		return getStatus().getCurrentHp();
	}
	
	public final void setCurrentHp(double newHp)
	{
		getStatus().setCurrentHp(newHp);
	}
	
	public final void setCurrentHpMp(double newHp, double newMp)
	{
		getStatus().setCurrentHpMp(newHp, newMp);
	}
	
	public final double getCurrentMp()
	{
		return getStatus().getCurrentMp();
	}
	
	public final void setCurrentMp(double newMp)
	{
		getStatus().setCurrentMp(newMp);
	}
	
	/**
	 * @return the max weight that the L2Character can load.
	 */
	public int getMaxLoad()
	{
		if (isPlayer() || isPet())
		{
			// Weight Limit = (CON Modifier*69000) * Skills
			// Source http://l2p.bravehost.com/weightlimit.html (May 2007)
			double baseLoad = Math.floor(BaseStats.CON.calcBonus(this) * 69000 * Config.ALT_WEIGHT_LIMIT);
			return (int) calcStat(Stats.WEIGHT_LIMIT, baseLoad, this, null);
		}
		return 0;
	}
	
	public int getBonusWeightPenalty()
	{
		if (isPlayer() || isPet())
		{
			return (int) calcStat(Stats.WEIGHT_PENALTY, 1, this, null);
		}
		return 0;
	}
	
	/**
	 * @return the current weight of the L2Character.
	 */
	public int getCurrentLoad()
	{
		if (isPlayer() || isPet())
		{
			return getInventory().getTotalWeight();
		}
		return 0;
	}
	
	public boolean isChampion()
	{
		return false;
	}
	
	/**
	 * Send system message about damage.
	 * @param target
	 * @param damage
	 * @param mcrit
	 * @param pcrit
	 * @param miss
	 */
	public void sendDamageMessage(Creature target, int damage, boolean mcrit, boolean pcrit, boolean miss)
	{
		
	}
	
	public byte getAttackElement()
	{
		return getStat().getAttackElement();
	}
	
	public int getAttackElementValue(byte attackAttribute)
	{
		return getStat().getAttackElementValue(attackAttribute);
	}
	
	public int getDefenseElementValue(byte defenseAttribute)
	{
		return getStat().getDefenseElementValue(defenseAttribute);
	}
	
	public final void startPhysicalAttackMuted()
	{
		abortAttack();
	}
	
	public void disableCoreAI(boolean val)
	{
		_AIdisabled = val;
	}
	
	public boolean isCoreAIDisabled()
	{
		return _AIdisabled;
	}
	
	/**
	 * @return true
	 */
	public boolean giveRaidCurse()
	{
		return true;
	}
	
	/**
	 * Check if target is affected with special buff
	 * @see CharEffectList#isAffected(EffectFlag)
	 * @param flag int
	 * @return boolean
	 */
	public boolean isAffected(EffectFlag flag)
	{
		return _effectList.isAffected(flag);
	}
	
	public void broadcastSocialAction(int id)
	{
		broadcastPacket(new SocialAction(getObjectId(), id));
	}
	
	public Team getTeam()
	{
		return _team;
	}
	
	public void setTeam(Team team)
	{
		_team = team;
	}
	
	public void addOverrideCond(PcCondOverride... excs)
	{
		for (PcCondOverride exc : excs)
		{
			_exceptions |= exc.getMask();
		}
	}
	
	public void removeOverridedCond(PcCondOverride... excs)
	{
		for (PcCondOverride exc : excs)
		{
			_exceptions &= ~exc.getMask();
		}
	}
	
	public boolean canOverrideCond(PcCondOverride excs)
	{
		return (_exceptions & excs.getMask()) == excs.getMask();
	}
	
	public void setOverrideCond(long masks)
	{
		_exceptions = masks;
	}
	
	public void setLethalable(boolean val)
	{
		_lethalable = val;
	}
	
	public boolean isLethalable()
	{
		return _lethalable;
	}
	
	public Map<Integer, OptionsSkillHolder> getTriggerSkills()
	{
		if (_triggerSkills == null)
		{
			synchronized (this)
			{
				if (_triggerSkills == null)
				{
					_triggerSkills = new ConcurrentHashMap<>();
				}
			}
		}
		return _triggerSkills;
	}
	
	public void addTriggerSkill(OptionsSkillHolder holder)
	{
		getTriggerSkills().put(holder.getSkillId(), holder);
	}
	
	public void removeTriggerSkill(OptionsSkillHolder holder)
	{
		getTriggerSkills().remove(holder.getSkillId());
	}
	
	public void makeTriggerCast(Skill skill, Creature target, boolean ignoreTargetType)
	{
		try
		{
			if ((skill == null))
			{
				return;
			}
			if (skill.checkCondition(this, target, false))
			{
				if (isSkillDisabled(skill))
				{
					return;
				}
				
				if (skill.getReuseDelay() > 0)
				{
					disableSkill(skill, skill.getReuseDelay());
				}
				
				// @formatter:off
				final WorldObject[] targets = !ignoreTargetType ? skill.getTargetList(this, false, target) : new Creature[]{ target };
				// @formatter:on
				if (targets.length == 0)
				{
					return;
				}
				
				for (WorldObject obj : targets)
				{
					if ((obj != null) && obj.isCreature())
					{
						target = (Creature) obj;
						break;
					}
				}
				
				if (Config.ALT_VALIDATE_TRIGGER_SKILLS && isPlayable() && (target != null) && target.isPlayable())
				{
					final PlayerInstance player = getActingPlayer();
					if (!player.checkPvpSkill(target, skill))
					{
						return;
					}
				}
				
				broadcastPacket(new MagicSkillUse(this, target, skill.getDisplayId(), skill.getLevel(), 0, 0));
				broadcastPacket(new MagicSkillLaunched(this, skill.getDisplayId(), skill.getLevel(), targets));
				
				// Launch the magic skill and calculate its effects
				skill.activateSkill(this, targets);
			}
		}
		catch (Exception e)
		{
			_log.warn("", e);
		}
	}
	
	public void makeTriggerCast(Skill skill, Creature target)
	{
		makeTriggerCast(skill, target, false);
	}
	
	/**
	 * Dummy method overriden in {@link PlayerInstance}
	 * @return {@code true} if current player can revive and shows 'To Village' button upon death, {@code false} otherwise.
	 */
	public boolean canRevive()
	{
		return true;
	}
	
	/**
	 * Dummy method overriden in {@link PlayerInstance}
	 * @param val
	 */
	public void setCanRevive(boolean val)
	{
	}
	
	/**
	 * Dummy method overriden in {@link Attackable}
	 * @return {@code true} if there is a loot to sweep, {@code false} otherwise.
	 */
	public boolean isSweepActive()
	{
		return false;
	}
	
	/**
	 * Dummy method overriden in {@link PlayerInstance}
	 * @return {@code true} if player is on event, {@code false} otherwise.
	 */
	public boolean isOnEvent()
	{
		return false;
	}
	
	/**
	 * Dummy method overriden in {@link PlayerInstance}
	 * @return the clan id of current character.
	 */
	public int getClanId()
	{
		return 0;
	}
	
	/**
	 * Dummy method overriden in {@link PlayerInstance}
	 * @return the clan of current character.
	 */
	public L2Clan getClan()
	{
		return null;
	}
	
	/**
	 * Dummy method overriden in {@link PlayerInstance}
	 * @return {@code true} if player is in academy, {@code false} otherwise.
	 */
	public boolean isAcademyMember()
	{
		return false;
	}
	
	/**
	 * Dummy method overriden in {@link PlayerInstance}
	 * @return the pledge type of current character.
	 */
	public int getPledgeType()
	{
		return 0;
	}
	
	/**
	 * Dummy method overriden in {@link PlayerInstance}
	 * @return the alliance id of current character.
	 */
	public int getAllyId()
	{
		return 0;
	}
	
	/**
	 * Notifies to listeners that current character received damage.
	 * @param damage
	 * @param attacker
	 * @param skill
	 * @param critical
	 * @param damageOverTime
	 * @param reflect
	 */
	public void notifyDamageReceived(double damage, Creature attacker, Skill skill, boolean critical, boolean damageOverTime, boolean reflect)
	{
		EventDispatcher.getInstance().notifyEventAsync(new OnCreatureDamageReceived(attacker, this, damage, skill, critical, damageOverTime, reflect), this);
		EventDispatcher.getInstance().notifyEventAsync(new OnCreatureDamageDealt(attacker, this, damage, skill, critical, damageOverTime, reflect), attacker);
	}
	
	/**
	 * Notifies to listeners that current character avoid attack.
	 * @param target
	 * @param isDot
	 */
	public void notifyAttackAvoid(final Creature target, final boolean isDot)
	{
		EventDispatcher.getInstance().notifyEventAsync(new OnCreatureAttackAvoid(this, target, isDot), target);
	}
	
	/**
	 * @return {@link WeaponType} of current character's weapon or basic weapon type.
	 */
	public final WeaponType getAttackType()
	{
		final Weapon weapon = getActiveWeaponItem();
		if (weapon != null)
		{
			return weapon.getItemType();
		}
		else if (isTransformed())
		{
			final TransformTemplate template = getTransformation().getTemplate(getActingPlayer());
			if (template != null)
			{
				return template.getBaseAttackType();
			}
		}
		return getTemplate().getBaseAttackType();
	}
	
	public final boolean isInCategory(CategoryType type)
	{
		return CategoryData.getInstance().isInCategory(type, getId());
	}
	
	/**
	 * @return the character that summoned this NPC.
	 */
	public Creature getSummoner()
	{
		return _summoner;
	}
	
	/**
	 * @param summoner the summoner of this NPC.
	 */
	public void setSummoner(Creature summoner)
	{
		_summoner = summoner;
	}
	
	/**
	 * Adds a summoned NPC.
	 * @param npc the summoned NPC
	 */
	public final void addSummonedNpc(Npc npc)
	{
		if (_summonedNpcs == null)
		{
			synchronized (this)
			{
				if (_summonedNpcs == null)
				{
					_summonedNpcs = new ConcurrentHashMap<>();
				}
			}
		}
		
		_summonedNpcs.put(npc.getObjectId(), npc);
		
		npc.setSummoner(this);
	}
	
	/**
	 * Removes a summoned NPC by object ID.
	 * @param objectId the summoned NPC object ID
	 */
	public final void removeSummonedNpc(int objectId)
	{
		if (_summonedNpcs != null)
		{
			_summonedNpcs.remove(objectId);
		}
	}
	
	/**
	 * Gets the summoned NPCs.
	 * @return the summoned NPCs
	 */
	public final Collection<Npc> getSummonedNpcs()
	{
		return _summonedNpcs != null ? _summonedNpcs.values() : Collections.<Npc> emptyList();
	}
	
	/**
	 * Gets the summoned NPC by object ID.
	 * @param objectId the summoned NPC object ID
	 * @return the summoned NPC
	 */
	public final Npc getSummonedNpc(int objectId)
	{
		if (_summonedNpcs != null)
		{
			return _summonedNpcs.get(objectId);
		}
		return null;
	}
	
	/**
	 * Gets the summoned NPC count.
	 * @return the summoned NPC count
	 */
	public final int getSummonedNpcCount()
	{
		return _summonedNpcs != null ? _summonedNpcs.size() : 0;
	}
	
	/**
	 * Resets the summoned NPCs list.
	 */
	public final void resetSummonedNpcs()
	{
		if (_summonedNpcs != null)
		{
			_summonedNpcs.clear();
		}
	}
	
	@Override
	public boolean isCreature()
	{
		return true;
	}
	
	/**
	 * @return {@code true} if current character is casting channeling skill, {@code false} otherwise.
	 */
	public final boolean isChanneling()
	{
		return (_channelizer != null) && _channelizer.isChanneling();
	}
	
	public final SkillChannelizer getSkillChannelizer()
	{
		if (_channelizer == null)
		{
			_channelizer = new SkillChannelizer(this);
		}
		return _channelizer;
	}
	
	/**
	 * @return {@code true} if current character is affected by channeling skill, {@code false} otherwise.
	 */
	public final boolean isChannelized()
	{
		return (_channelized != null) && !_channelized.isChannelized();
	}
	
	public final SkillChannelized getSkillChannelized()
	{
		if (_channelized == null)
		{
			_channelized = new SkillChannelized();
		}
		return _channelized;
	}
	
	public void addInvulAgainst(SkillHolder holder)
	{
		final InvulSkillHolder invulHolder = getInvulAgainstSkills().get(holder.getSkillId());
		if (invulHolder != null)
		{
			invulHolder.increaseInstances();
			return;
		}
		getInvulAgainstSkills().put(holder.getSkillId(), new InvulSkillHolder(holder));
	}
	
	public void removeInvulAgainst(SkillHolder holder)
	{
		final InvulSkillHolder invulHolder = getInvulAgainstSkills().get(holder.getSkillId());
		if ((invulHolder != null) && (invulHolder.decreaseInstances() < 1))
		{
			getInvulAgainstSkills().remove(holder.getSkillId());
		}
	}
	
	public boolean isInvulAgainst(int skillId, int skillLvl)
	{
		if (_invulAgainst != null)
		{
			final SkillHolder holder = getInvulAgainstSkills().get(skillId);
			return ((holder != null) && ((holder.getSkillLvl() < 1) || (holder.getSkillLvl() == skillLvl)));
		}
		return false;
	}
	
	private Map<Integer, InvulSkillHolder> getInvulAgainstSkills()
	{
		if (_invulAgainst == null)
		{
			synchronized (this)
			{
				if (_invulAgainst == null)
				{
					_invulAgainst = new ConcurrentHashMap<>();
				}
			}
		}
		return _invulAgainst;
	}
	
	@Override
	public Queue<AbstractEventListener> getListeners(EventType type)
	{
		final Queue<AbstractEventListener> objectListenres = super.getListeners(type);
		final Queue<AbstractEventListener> templateListeners = getTemplate().getListeners(type);
		final Queue<AbstractEventListener> globalListeners = isNpc() && !isMonster() ? Containers.Npcs().getListeners(type) : isMonster() ? Containers.Monsters().getListeners(type) : isPlayer() ? Containers.Players().getListeners(type) : EmptyQueue.emptyQueue();
		
		// Attempt to do not create collection
		if (objectListenres.isEmpty() && templateListeners.isEmpty() && globalListeners.isEmpty())
		{
			return EmptyQueue.emptyQueue();
		}
		else if (!objectListenres.isEmpty() && templateListeners.isEmpty() && globalListeners.isEmpty())
		{
			return objectListenres;
		}
		else if (!templateListeners.isEmpty() && objectListenres.isEmpty() && globalListeners.isEmpty())
		{
			return templateListeners;
		}
		else if (!globalListeners.isEmpty() && objectListenres.isEmpty() && templateListeners.isEmpty())
		{
			return globalListeners;
		}
		
		final Queue<AbstractEventListener> both = new LinkedBlockingDeque<>(objectListenres.size() + templateListeners.size() + globalListeners.size());
		both.addAll(objectListenres);
		both.addAll(templateListeners);
		both.addAll(globalListeners);
		return both;
	}
	
	public Race getRace()
	{
		return getTemplate().getRace();
	}
	
	@Override
	public final void setXYZ(int newX, int newY, int newZ)
	{
		try
		{
			final ZoneRegion oldZoneRegion = ZoneManager.getInstance().getRegion(this);
			final ZoneRegion newZoneRegion = ZoneManager.getInstance().getRegion(newX, newY);
			if (oldZoneRegion != newZoneRegion)
			{
				oldZoneRegion.removeFromZones(this);
				newZoneRegion.revalidateZones(this);
			}
		}
		catch (Exception e)
		{
			badCoords();
		}
		
		super.setXYZ(newX, newY, newZ);
	}
	
	public final Map<Integer, Integer> getKnownRelations()
	{
		return _knownRelations;
	}
	
	@Override
	public boolean isTargetable()
	{
		return super.isTargetable() && !isAffected(EffectFlag.UNTARGETABLE);
	}
	
	public boolean cannotEscape()
	{
		return isAffected(EffectFlag.CANNOT_ESCAPE);
	}
	
	/**
	 * Sets amount of debuffs that player can avoid
	 * @param times
	 */
	public void setDebuffBlockTimes(int times)
	{
		_blockedDebuffTimes.set(times);
	}
	
	/**
	 * @return the amount of debuffs that player can avoid
	 */
	public int getDebuffBlockedTime()
	{
		return _blockedDebuffTimes.get();
	}
	
	/**
	 * @return the amount of debuffs that player can avoid
	 */
	public int decrementDebuffBlockTimes()
	{
		return _blockedDebuffTimes.decrementAndGet();
	}
}
