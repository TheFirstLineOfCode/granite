package com.thefirstlineofcode.granite.xeps.muc;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.thefirstlineofcode.basalt.xeps.address.Addresses;
import com.thefirstlineofcode.basalt.xeps.muc.Affiliation;
import com.thefirstlineofcode.basalt.xeps.muc.GetMemberList;
import com.thefirstlineofcode.basalt.xeps.muc.Muc;
import com.thefirstlineofcode.basalt.xeps.muc.PresenceBroadcast;
import com.thefirstlineofcode.basalt.xeps.muc.Role;
import com.thefirstlineofcode.basalt.xeps.muc.RoomConfig;
import com.thefirstlineofcode.basalt.xeps.muc.RoomConfig.AllowPm;
import com.thefirstlineofcode.basalt.xeps.muc.RoomConfig.WhoIs;
import com.thefirstlineofcode.basalt.xeps.muc.admin.MucAdmin;
import com.thefirstlineofcode.basalt.xeps.muc.owner.MucOwner;
import com.thefirstlineofcode.basalt.xeps.muc.user.Actor;
import com.thefirstlineofcode.basalt.xeps.muc.user.Continue;
import com.thefirstlineofcode.basalt.xeps.muc.user.Destroy;
import com.thefirstlineofcode.basalt.xeps.muc.user.Invite;
import com.thefirstlineofcode.basalt.xeps.muc.user.Item;
import com.thefirstlineofcode.basalt.xeps.muc.user.MucUser;
import com.thefirstlineofcode.basalt.xeps.muc.user.Status;
import com.thefirstlineofcode.basalt.xeps.muc.xconference.XConference;
import com.thefirstlineofcode.basalt.xeps.xdata.Field;
import com.thefirstlineofcode.basalt.xeps.xdata.XData;
import com.thefirstlineofcode.basalt.xmpp.core.JabberId;
import com.thefirstlineofcode.basalt.xmpp.core.ProtocolException;
import com.thefirstlineofcode.basalt.xmpp.core.stanza.Iq;
import com.thefirstlineofcode.basalt.xmpp.core.stanza.error.BadRequest;
import com.thefirstlineofcode.basalt.xmpp.core.stanza.error.Conflict;
import com.thefirstlineofcode.basalt.xmpp.core.stanza.error.Forbidden;
import com.thefirstlineofcode.basalt.xmpp.core.stanza.error.ItemNotFound;
import com.thefirstlineofcode.basalt.xmpp.core.stanza.error.JidMalformed;
import com.thefirstlineofcode.basalt.xmpp.core.stanza.error.NotAcceptable;
import com.thefirstlineofcode.basalt.xmpp.core.stanza.error.NotAllowed;
import com.thefirstlineofcode.basalt.xmpp.core.stanza.error.RegistrationRequired;
import com.thefirstlineofcode.basalt.xmpp.core.stanza.error.RemoteServerNotFound;
import com.thefirstlineofcode.basalt.xmpp.core.stanza.error.UnexpectedRequest;
import com.thefirstlineofcode.basalt.xmpp.im.stanza.Message;
import com.thefirstlineofcode.basalt.xmpp.im.stanza.Presence;
import com.thefirstlineofcode.granite.framework.core.adf.data.IDataObjectFactory;
import com.thefirstlineofcode.granite.framework.core.adf.data.IDataObjectFactoryAware;
import com.thefirstlineofcode.granite.framework.core.annotations.AppComponent;
import com.thefirstlineofcode.granite.framework.core.annotations.BeanDependency;
import com.thefirstlineofcode.granite.framework.core.auth.IAuthenticator;
import com.thefirstlineofcode.granite.framework.core.config.IConfiguration;
import com.thefirstlineofcode.granite.framework.core.config.IConfigurationAware;
import com.thefirstlineofcode.granite.framework.core.config.IServerConfiguration;
import com.thefirstlineofcode.granite.framework.core.config.IServerConfigurationAware;
import com.thefirstlineofcode.granite.framework.core.pipeline.stages.processing.IProcessingContext;
import com.thefirstlineofcode.granite.framework.core.utils.StanzaCloner;
import com.thefirstlineofcode.granite.framework.im.IResource;
import com.thefirstlineofcode.granite.framework.im.IResourcesService;

@AppComponent("muc.protocols.delegator")
public class MucProtocolsDelegator implements IServerConfigurationAware,
			IConfigurationAware, IDataObjectFactoryAware {

	private static final Logger logger = LoggerFactory.getLogger(MucProtocolsDelegator.class);
	
	private static final String COINFIGURATION_KEY_ROOM_DEFAULT_MAX_HISTORY_FETCH = "room.default.max.history.fetch";
	private static final String COINFIGURATION_KEY_ROOM_DEFAULT_WHO_IS = "room.default.who.is";
	private static final String COINFIGURATION_KEY_ROOM_DEFAULT_MEMBER_ONLY = "room.default.member.only";
	private static final String COINFIGURATION_KEY_ROOM_DEFAULT_PERSISTENT_ROOM = "room.default.persistent.room";
	private static final String COINFIGURATION_KEY_ROOM_DEFAULT_PUBLIC_ROOM = "room.default.public.room";
	private static final String COINFIGURATION_KEY_ROOM_DEFAULT_GET_MEMBER_LIST = "room.default.get.member.list";
	private static final String COINFIGURATION_KEY_ROOM_DEFAULT_PRESENCE_BROADCAST = "room.default.presence.broadcast";
	private static final String COINFIGURATION_KEY_ROOM_DEFAULT_MAX_USERS = "room.default.max.users";
	private static final String COINFIGURATION_KEY_ROOM_DEFAULT_ALLOW_PM = "room.default.allow.pm";
	private static final String COINFIGURATION_KEY_ROOM_DEFAULT_ALLOW_INVITES = "room.default.allow.invites";
	private static final String COINFIGURATION_KEY_ROOM_DEFAULT_CHANGE_SUBJECT = "room.default.change.subject";
	private static final String COINFIGURATION_KEY_ROOM_DEFAULT_ENABLE_LOGGING = "room.default.enable.logging";
	private static final String CONFIGURATION_KEY_ROOM_DEFAULT_LANG = "room.default.lang";
	private static final String CONFIGURATION_KEY_MUC_DOMAIN_NAME = "muc.domain.name";
	private static final String CONFIGURATION_KEY_DISABLE_USER_TO_CREATE_ROOM = "disable.user.to.create.room";
	
	private boolean disableUserToCreateRoom;
	private String domainName;
	private String mucDomainName;
	private IDataObjectFactory dataObjectFactory;
	private IConfiguration configuration;
	
	@BeanDependency
	private IRoomService roomService;
	
	@BeanDependency
	private IAuthenticator authenticator;
	
	@BeanDependency
	private IResourcesService resourcesService;
	
	public void process(IProcessingContext context, Iq iq, MucOwner mucOwner) {
		checkRoomJid(iq.getTo());
		
		if (mucOwner.getXData() != null) {
			if (XData.Type.SUBMIT != mucOwner.getXData().getType()) {
				throw new ProtocolException(new BadRequest("XData type must be set to 'submit'."));
			}
			
			Room room = roomService.getRoomSession(iq.getTo().getBareId()).getRoom();
			if (room.isLocked()) {
				processRoomCreation(context, iq, mucOwner.getXData());
			} else {
				processSubsequentConfiguration(context, iq, mucOwner, room);
			}
		} else if (mucOwner.getDestroy() != null) {
			destroyRoom(context, iq, mucOwner.getDestroy());
		} else {
			sendConfigurationForm(context, iq, mucOwner);
		}
	}

	private void processSubsequentConfiguration(IProcessingContext context,
			Iq iq, MucOwner mucOwner, Room room) {
		configureRoom(context, iq, mucOwner.getXData(), room);		
		context.write(new Iq(Iq.Type.RESULT, iq.getId()));
	}

	private void configureRoom(IProcessingContext context, Iq iq, XData xData, Room room) {
		// TODO Auto-generated method stub
		
	}

	private void destroyRoom(IProcessingContext context, Iq iq, Destroy destroy) {
		// TODO Auto-generated method stub
		
	}

	private void sendConfigurationForm(IProcessingContext context, Iq iq, MucOwner mucOwner) {
		Room room = roomService.getRoomSession(iq.getTo().getBareId()).getRoom();
		checkPriviliegeForConfiguration(iq, room);
		
		context.write(createRoomConfigurationFormIqResult(iq, room));
	}
	
	
	private Iq createRoomConfigurationFormIqResult(Iq iq, Room room) {
		Iq result = Iq.createResult(iq);
		
		MucOwner mucOwner = new MucOwner();
		mucOwner.setXData(generateConfigurationForm(room));
		result.setObject(mucOwner);
		
		return result;
	}

	protected XData generateConfigurationForm(Room room) {
		return new StandardConfigurationFormGenerator(room).generate();
	}

	private void processRoomCreation(IProcessingContext context, Iq iq, XData xData) {
		if (isReservedRoomCreationForm(xData)) {
			createReservedRoom(context, iq, xData);
		} else {
			// instant room creation
			createInstantRoom(context, iq);
		}
	}

	private void createReservedRoom(IProcessingContext context, Iq iq, XData xData) {
		Room room = roomService.getRoomSession(iq.getTo().getBareId()).getRoom();
		configureReservedRoom(context, iq, xData, room);		
		roomService.unlockRoom(room.getRoomJid());
		
		Iq ack = new Iq(Iq.Type.RESULT, iq.getId());
		ack.setFrom(room.getRoomJid());
		context.write(ack);
	}

	private void configureReservedRoom(IProcessingContext context, Iq iq, XData xData, Room room) {
		checkPriviliegeForConfiguration(iq, room);
		RoomConfig newRoomConfig = convertConfigFormToRoomConfig(xData);
		
		roomService.updateRoomConfig(room, newRoomConfig);
	}

	private void checkPriviliegeForConfiguration(Iq iq, Room room) {
		AffiliatedUser user = room.getAffiliatedUser(iq.getFrom());
		
		if (user == null || user.getAffiliation() != Affiliation.OWNER) {
			throw new ProtocolException(new Forbidden());
		}
	}

	private RoomConfig convertConfigFormToRoomConfig(XData form) {
		RoomConfig roomConfig = getDefaultRoomConfig();
		for (Field field : form.getFields()) {
			if ("muc#roomconfig_roomname".equals(field.getVar())) {
				roomConfig.setRoomName(getFieldStringValue(field));
			} else if ("muc#roomconfig_roomdesc".equals(field.getVar())) {
				roomConfig.setRoomDesc(getFieldStringValue(field));
			} else if ("muc#roomconfig_lang".equals(field.getVar())) {
				roomConfig.setLang(getFieldStringValue(field));
			} else if ("muc#roomconfig_enablelogging".equals(field.getVar())) {
				roomConfig.setEnableLogging(getFieldBooleanValue(field));
			} else if ("muc#roomconfig_changesubject".equals(field.getVar())) {
				roomConfig.setChangeSubject(getFieldBooleanValue(field));
			} else if ("muc#roomconfig_allowinvites".equals(field.getVar())) {
				roomConfig.setAllowInvites(getFieldBooleanValue(field));
			} else if ("muc#roomconfig_allowpm".equals(field.getVar())) {
				roomConfig.setAllowPm(AllowPm.valueOf(getFieldStringValue(field).toUpperCase()));
			} else if ("muc#roomconfig_maxusers".equals(field.getVar())) {
				roomConfig.setMaxUsers(getFieldIntValue(field));
			} else if ("muc#roomconfig_presencebroadcast".equals(field.getVar())) {
				roomConfig.setPresenceBroadcast(getPresenceBroadcast(field));
			} else if ("muc#roomconfig_getmemberlist".equals(field.getVar())) {
				roomConfig.setGetMemberList(getGetMemberList(field));
			} else if ("muc#roomconfig_publicroom".equals(field.getVar())) {
				roomConfig.setPublicRoom(getFieldBooleanValue(field));
			} else if ("muc#roomconfig_persistentroom".equals(field.getVar())) {
				roomConfig.setPersistentRoom(getFieldBooleanValue(field));
			} else if ("muc#roomconfig_moderatedroom".equals(field.getVar())) {
				roomConfig.setModeratedRoom(getFieldBooleanValue(field));
			} else if ("muc#roomconfig_membersonly".equals(field.getVar())) {
				roomConfig.setMembersOnly(getFieldBooleanValue(field));
			} else if ("muc#roomconfig_passwordprotectedroom".equals(field.getVar())) {
				roomConfig.setPasswordProtectedRoom(getFieldBooleanValue(field));
			} else if ("muc#roomconfig_roomsecret".equals(field.getVar())) {
				roomConfig.setRoomSecret(getFieldStringValue(field));
			} else if ("muc#roomconfig_whois".equals(field.getVar())) {
				roomConfig.setWhoIs(WhoIs.valueOf(getFieldStringValue(field).toUpperCase()));
			} else if ("muc#maxhistoryfetch".equals(field.getVar())) {
				roomConfig.setMaxHistoryFetch(getFieldIntValue(field));
			} else if ("muc#roomconfig_roomadmins".equals(field.getVar())) {
				roomConfig.setAdmins(getFieldJidsValue(field));
			} else if ("muc#roomconfig_roomowners".equals(field.getVar())) {
				roomConfig.setOwners(getFieldJidsValue(field));
			} else {
				// ignore
				continue;
			}
		}
		
		return roomConfig;
	}
	
	private List<JabberId> getFieldJidsValue(Field field) {
		List<JabberId> jids = new ArrayList<>();
		
		for (String value : field.getValues()) {
			jids.add(JabberId.parse(value));
		}
		
		return jids;
	}

	private GetMemberList getGetMemberList(Field field) {
		GetMemberList getMemberList = new GetMemberList();
		getMemberList.setModerator(false);
		getMemberList.setParticipant(false);
		getMemberList.setVisitor(false);
		for (String value : field.getValues()) {
			if ("moderator".equals(value)) {
				getMemberList.setModerator(true);
			} else if ("participant".equals(value)) {
				getMemberList.setParticipant(true);
			} else if ("visitor".equals(value)) {
				getMemberList.setVisitor(true);
			} else {
				throw new IllegalArgumentException(String.format("Invalid get member list config: %s.", value));
			}
		}
		
		return getMemberList;
	}

	private PresenceBroadcast getPresenceBroadcast(Field field) {
		PresenceBroadcast presenceBroadcast = new PresenceBroadcast();
		presenceBroadcast.setModerator(false);
		presenceBroadcast.setParticipant(false);
		presenceBroadcast.setVisitor(false);
		for (String value : field.getValues()) {
			if ("moderator".equals(value)) {
				presenceBroadcast.setModerator(true);
			} else if ("participant".equals(value)) {
				presenceBroadcast.setParticipant(true);
			} else if ("visitor".equals(value)) {
				presenceBroadcast.setVisitor(true);
			} else {
				throw new IllegalArgumentException(String.format("Invalid presence broadcast config: %s.", value));
			}
		}
		
		return presenceBroadcast;
	}

	private int getFieldIntValue(Field field) {
		String value = field.getValues().get(0);
		try {
			return Integer.valueOf(value);
		} catch (NumberFormatException e) {
			throw new ProtocolException(new BadRequest("'%s' isn't an integer value.", value));
		}
		
	}

	private boolean getFieldBooleanValue(Field field) {
		String value = field.getValues().get(0);
		if ("1".equals(value) || "true".equals(value)) {
			return true;
		} else if ("0".equals(value) || "false".equals(value)) {
			return false;
		} else {
			throw new ProtocolException(new BadRequest(String.format("'%s' isn't a boolean value.", value)));
		}
	}

	private String getFieldStringValue(Field field) {
		if (field.getValues().isEmpty())
			return null;
		
		return field.getValues().get(0);
	}

	private void createInstantRoom(IProcessingContext context, Iq iq) {
		Room room = roomService.getRoomSession(iq.getTo().getBareId()).getRoom();
		checkPriviliegeForConfiguration(iq, room);
		
		if (room.isLocked()) {
			roomService.unlockRoom(room.getRoomJid());
		}
		
		Iq ack = new Iq(Iq.Type.RESULT, iq.getId());
		ack.setFrom(room.getRoomJid());
		context.write(ack);
	}

	private boolean isReservedRoomCreationForm(XData xData) {
		return xData.getFields().size() != 0 ||
				xData.getInstructions().size() != 0 ||
				xData.getItems().size() != 0 ||
				xData.getReported() != null ||
				xData.getTitle() != null;
	}

	public void process(IProcessingContext context, Presence presence, Muc muc) {
		checkRoomJid(presence.getTo());
		
		if (presence.getTo().getResource() == null) {
			// (xep-0045 7.2.1)
			// no nickname specified
			throw new ProtocolException(new JidMalformed("No nickname specified."));
		}
		
		JabberId roomJid = presence.getTo().getBareId();
		if (roomService.exists(roomJid)) {
			Map<JabberId, String> roomJidToNicks = MucSessionUtils.getOrCreateRoomJidToNicks(context);
			String oldNick = roomJidToNicks.get(roomJid);
			if (oldNick == null) {
				processEnterRoom(context, presence, muc);
			} else{
				throw new ProtocolException(new UnexpectedRequest(String.format("User '%s' has already entered the room '%s'.",
						presence.getTo().getResource(), roomJid)));
			}
		} else {
			processCreateRoom(context, presence);
		}
	}
	
	// (xep-0045 7.6)
	// changing nickname
	private void processChangeNick(IProcessingContext context, Presence presence) {
		JabberId sessionJid = context.getJid();
		String nick = presence.getTo().getResource();
		
		if (isNickLockedDown(sessionJid, nick)) {
			throw new ProtocolException(new NotAcceptable(String.format("Nick '%s' is locked down.", nick)));
		}
		
		JabberId roomJid = presence.getTo().getBareId();
		
		IRoomSession roomSession = roomService.getRoomSession(roomJid);
		
		Map<JabberId, String> roomJidToNicks = MucSessionUtils.getOrCreateRoomJidToNicks(context);
		String oldNick = roomJidToNicks.get(roomJid);
		
		roomSession.changeNick(sessionJid, nick);
		roomJidToNicks.put(roomJid, nick);
		
		
		updateNickPresenceBroadcast(context, roomSession, oldNick, nick);
		sendNewOccupantPresenceToAllOccupants(context, roomSession, null, nick);
	}

	private void processCreateRoom(IProcessingContext context, Presence presence) {
		if (presence.getType() != null) {
			throw new ProtocolException(new BadRequest("Type of presence must be null."));
		}
		
		if (disableUserToCreateRoom) {
			throw new ProtocolException(new NotAllowed());
		}
		
		JabberId roomJid = presence.getTo().getBareId();
		JabberId creator = context.getJid().getBareId();
		createRoom(roomJid, creator);
		String nick = presence.getTo().getResource();
		enterRoom(context, roomJid, nick);
		
		Presence ack = createRoomCreationAcknowledge(context, roomService, presence.getTo());
		// some third-party clients need a id to track the creation.
		if (presence.getId() != null) {
			ack.setId(presence.getId());
		}
		context.write(ack);
	}
	
	private void processEnterRoom(IProcessingContext context, Presence presence, Muc muc) {
		JabberId roomJid = presence.getTo().getBareId();
		String nick = presence.getTo().getResource();
		
		IRoomSession roomSession = roomService.getRoomSession(roomJid);
		if (roomSession.getRoom().isLocked()) {
			// (xep-0045 7.2.11)
			// locked room
			throw new ProtocolException(new ItemNotFound());
		}
		
		Map<JabberId, String> roomJidToNicks = MucSessionUtils.getOrCreateRoomJidToNicks(context);
		String oldNick = roomJidToNicks.get(roomJid);
		if (oldNick == null) {
			// new occupant enters a room
			if (muc == null) {
				enterRoom(context, roomJid, nick);
			} else {
				enterRoom(context, roomJid, nick, muc.getPassword());
			}
			
			presenceBroadcast(context, roomSession, nick);
			
			if (roomSession.getRoom().getRoomConfig().getMaxHistoryFetch() > 0) {
				sendDiscussionHistoryToNewOccupant(context, roomJid, nick, muc);
			}
			
			sendSubjectToNewOccupant(context, roomJid, roomSession.getSubject());
		}
	}

	private void sendSubjectToNewOccupant(IProcessingContext context, JabberId roomJid, Message subject) {
		Message toNewOccupant;
		
		if (subject == null) {
			toNewOccupant = new Message();
			toNewOccupant.setSubject("");
			toNewOccupant.setFrom(roomJid);
			toNewOccupant.setTo(context.getJid());
			toNewOccupant.setType(Message.Type.GROUPCHAT);
		} else {
			toNewOccupant = StanzaCloner.clone(subject);
			toNewOccupant.setTo(context.getJid());
		}
		
		context.write(toNewOccupant);
	}

	private void sendDiscussionHistoryToNewOccupant(IProcessingContext context, JabberId roomJid,
			String nick, Muc muc) {
		IRoomSession roomSession = roomService.getRoomSession(roomJid);
		Message[] history = roomSession.getDiscussionHistory();
		history = getManagingDiscussionHistory(history, muc);
		
		boolean shouldSendFullJid = shouldSendFullJid(roomSession.getRoom().getRoomConfig().getWhoIs(),
				roomSession.getOccupant(nick));
		for (Message message : history) {
			Message toOccupant = StanzaCloner.clone(message);
			toOccupant.setTo(context.getJid());
			if (!shouldSendFullJid) {
				Addresses addresses = toOccupant.getObject(Addresses.class);
				if (addresses != null) {
					toOccupant.getObjects().remove(addresses);
					toOccupant.getObjectProtocols().remove(Addresses.class);
				}
			}
			
			context.write(toOccupant);
		}
	}

	private Message[] getManagingDiscussionHistory(Message[] history, Muc muc) {
		// TODO (xep-0045 7.2.15)
		// managing discussion history
		return history;
	}

	private void presenceBroadcast(IProcessingContext context, IRoomSession roomSession, String nick) {
		PresenceBroadcast presenceBroadcast = roomSession.getRoom().getRoomConfig().getPresenceBroadcast();
		if (shouldSendPresenceTo(presenceBroadcast, roomSession.getOccupant(nick))) {
			sendPresenceOfExistingOccupantsToNewOccupant(context, roomSession, nick);
		}
		
		sendNewOccupantPresenceToAllOccupants(context, roomSession, null, nick);
	}
	
	private void updateNickPresenceBroadcast(IProcessingContext context, IRoomSession roomSession,
			String oldNick, String newNick) {
		sendOccupantPresenceToAllOccupantsExceptSelf(context, roomSession, null, oldNick, newNick);
		sendSelfPresence(context, roomSession, null, oldNick, newNick);
	}

	private boolean shouldSendPresenceTo(PresenceBroadcast presenceBroadcast, Occupant occupant) {
		return (occupant.getRole() == Role.MODERATOR && presenceBroadcast.isModerator()) ||
				(occupant.getRole() == Role.PARTICIPANT && presenceBroadcast.isParticipant()) ||
					(occupant.getRole() == Role.VISITOR && presenceBroadcast.isVisitor());
	}

	private void sendNewOccupantPresenceToAllOccupants(IProcessingContext context, IRoomSession roomSession,
			Presence updatePresence, String nick) {
		sendOccupantPresenceToAllOccupantsExceptSelf(context, roomSession, updatePresence, nick, null);
		sendSelfPresence(context, roomSession, updatePresence, nick, null);
	}
	
	private void sendOccupantPresenceToAllOccupantsExceptSelf(IProcessingContext context, IRoomSession roomSession,
			Presence updatePresence, String nick, String newNick) {
		Occupant occupant = roomSession.getOccupant(nick);
		Affiliation occupantAffiliation = getAffiliation(roomSession, context.getJid());
		Role occupantRole = occupant.getRole();
		JabberId roomJid = roomSession.getRoom().getRoomJid();
		JabberId sessionJid = context.getJid();
		JabberId occupantJid = new JabberId(roomJid.getNode(), roomJid.getDomain(), occupant.getNick());
		PresenceBroadcast presenceBroadcast = roomSession.getRoom().getRoomConfig().getPresenceBroadcast();
		WhoIs whoIs = roomSession.getRoom().getRoomConfig().getWhoIs();
		for (Occupant existingOccupant : roomSession.getOccupants()) {
			if (!shouldSendPresenceTo(presenceBroadcast, existingOccupant))
				continue;
			
			for (JabberId existingOccupantSessionJid : existingOccupant.getJids()) {
				if (existingOccupantSessionJid.equals(sessionJid))
					continue;
				
				Item item = new Item();
				if (occupantAffiliation != null)
					item.setAffiliation(occupantAffiliation);
				item.setRole(occupantRole);
				
				if (shouldSendFullJid(whoIs, existingOccupant))
					item.setJid(sessionJid);
				
				MucUser mucUser = new MucUser();
				mucUser.getItems().add(item);
				
				if (updatePresence != null) {
					// use status code 130 to inform that user changes it's availability status
					mucUser.getStatuses().add(new Status(130));
				}
				
				Presence presence = createOrClonePresence(updatePresence);
				presence.setFrom(occupantJid);
				presence.setTo(existingOccupantSessionJid);
				presence.setObject(mucUser);
				
				if (newNick != null) { // change nickname
					mucUser = presence.getObject();
					mucUser.getStatuses().add(new Status(303));
					mucUser.getItems().get(0).setNick(newNick);
					presence.setType(Presence.Type.UNAVAILABLE);
				}
				
				context.write(presence);
			}
		}
	}

	private Presence createOrClonePresence(Presence updatePresence) {
		if (updatePresence != null) {
			return StanzaCloner.clone(updatePresence);
		} else {
			return new Presence();
		}
	}

	private void sendSelfPresence(IProcessingContext context, IRoomSession roomSession, Presence updatePresence, String nick, String newNick) {
		Occupant occupant = roomSession.getOccupant(nick);
		WhoIs whoIs = roomSession.getRoom().getRoomConfig().getWhoIs();
		Affiliation occupantAffiliation = getAffiliation(roomSession, context.getJid());
		Role occupantRole = occupant.getRole();
		JabberId roomJid = roomSession.getRoom().getRoomJid();
		JabberId sessionJid = context.getJid();
		JabberId occupantJid = new JabberId(roomJid.getNode(), roomJid.getDomain(), occupant.getNick());
		
		Item item = new Item();
		item.setAffiliation(occupantAffiliation);
		item.setRole(occupantRole);
		
		if (shouldSendFullJid(whoIs, occupant))
			item.setJid(sessionJid);
		
		MucUser mucUser = new MucUser();
		mucUser.getItems().add(item);
		
		if (isNoAnonymousRoom(roomSession.getRoom().getRoomConfig())) {
			mucUser.getStatuses().add(new Status(100));
		}
		mucUser.getStatuses().add(new Status(110));
		
		if (updatePresence != null) {
			// use status 130 to inform that user changes it's availability status
			mucUser.getStatuses().add(new Status(130));
		}
		
		Presence presence = createOrClonePresence(updatePresence);
		presence.setFrom(occupantJid);
		presence.setTo(sessionJid);
		presence.setObject(mucUser);
		
		if (newNick != null) { // change nickname
			mucUser = presence.getObject();
			mucUser.getStatuses().add(new Status(303));
			mucUser.getItems().get(0).setNick(newNick);
			presence.setType(Presence.Type.UNAVAILABLE);
		}
		
		context.write(presence);
	}

	private void sendPresenceOfExistingOccupantsToNewOccupant(IProcessingContext context,
			IRoomSession roomSession, String nick) {
		Occupant newOccupant = roomSession.getOccupant(nick);
		WhoIs whoIs = roomSession.getRoom().getRoomConfig().getWhoIs();
		boolean sendFullJid = shouldSendFullJid(whoIs, newOccupant);
		JabberId roomJid = roomSession.getRoom().getRoomJid();
		JabberId newOccupantSessionJid = context.getJid();
		
		for (Occupant existingOccupant : roomSession.getOccupants()) {
			JabberId existingOccupantJid = new JabberId(roomJid.getNode(), roomJid.getDomain(), existingOccupant.getNick());
			for (JabberId existingOccupantSessionJid : existingOccupant.getJids()) {
				if (existingOccupantSessionJid.equals(newOccupantSessionJid))
					continue;
				
				Item item = new Item();
				item.setAffiliation(getAffiliation(roomSession, existingOccupantSessionJid));
				item.setRole(existingOccupant.getRole());
				if (sendFullJid) {
					item.setJid(existingOccupantSessionJid);
				}
				
				MucUser mucUser = new MucUser();
				mucUser.getItems().add(item);
				
				Presence presence = new Presence();
				presence.setFrom(existingOccupantJid);
				presence.setTo(newOccupantSessionJid);
				presence.setObject(mucUser);
				
				context.write(presence);
			}
		}
	}

	private Affiliation getAffiliation(IRoomSession roomSession, JabberId sessionJid) {
		AffiliatedUser affiliatedUser = roomSession.getRoom().getAffiliatedUser(sessionJid);
		Affiliation affiliation = null;
		if (affiliatedUser != null) {
			affiliation = affiliatedUser.getAffiliation();
		}
		
		return affiliation;
	}

	private boolean shouldSendFullJid(WhoIs whoIs, Occupant occupant) {
		boolean sendFullJid;
		if (whoIs == WhoIs.NONE) {
			sendFullJid = false;
		} else if (whoIs == WhoIs.ANYONE) {
			sendFullJid = true;
		} else {
			sendFullJid = (occupant.getRole() == Role.MODERATOR);
		}
		
		return sendFullJid;
	}
	
	private void enterRoom(IProcessingContext context, JabberId roomJid, String nick) {
		this.enterRoom(context, roomJid, nick, null);
	}

	private void enterRoom(IProcessingContext context, JabberId roomJid, String nick, String password) {
		IRoomSession roomSession = roomService.getRoomSession(roomJid);
		AffiliatedUser affiliatedUser = roomSession.getRoom().getAffiliatedUser(context.getJid());
		
		// (xep-0045 7.2.8)
		// banned users
		if (affiliatedUser != null && affiliatedUser.getAffiliation() == Affiliation.OUTCAST) {
			throw new ProtocolException(new Forbidden());
		}
		
		if ((affiliatedUser == null || affiliatedUser.getAffiliation() == Affiliation.NONE) &&
				roomSession.getRoom().getRoomConfig().isMembersOnly()) {
			throw new ProtocolException(new RegistrationRequired());
		}
		
		if (isNickLockedDown(context.getJid().getBareId(), nick)) {
			throw new ProtocolException(new NotAcceptable(String.format("Nick '%s' is locked down.", nick)));
		}
		
		if (affiliatedUser != null && (affiliatedUser.getNick() == null || !affiliatedUser.getNick().equals(nick))) {
			roomService.updateNick(roomJid, context.getJid().getBareId(), nick);
		}
		
		if (password != null) {
			roomSession.enter(context.getJid(), nick, password);
		} else {
			roomSession.enter(context.getJid(), nick);
		}
		
		Map<JabberId, String> roomJidToNicks = MucSessionUtils.getOrCreateRoomJidToNicks(context);
		roomJidToNicks.put(roomJid, nick);
	}

	protected boolean isNickLockedDown(JabberId newOccupantJid, String nick) {
		JabberId roomJid = newOccupantJid.getBareId();
		JabberId jid = roomService.getAffiliatedUserJidByNick(roomJid, nick);
		return jid != null && !jid.equals(newOccupantJid);
	}

	private void checkRoomJid(JabberId jid) {
		if (jid == null || jid.getNode() == null) {
			throw new ProtocolException(new JidMalformed("Invalid room JID."));
		}
		
		if (!mucDomainName.equals(jid.getDomain())) {
			throw new ProtocolException(new RemoteServerNotFound());
		}
	}

	private Presence createRoomCreationAcknowledge(IProcessingContext context,
			IRoomService roomService, JabberId creator) {
		JabberId roomJid = creator.getBareId();
		IRoomSession roomSession = roomService.getRoomSession(roomJid);
		WhoIs whoIs = roomSession.getRoom().getRoomConfig().getWhoIs();
		Occupant occupant = roomSession.getOccupant(creator.getResource());
		
		Item item = new Item();
		
		item.setAffiliation(Affiliation.OWNER);
		item.setRole(Role.MODERATOR);
		if (shouldSendFullJid(whoIs, occupant))
			item.setJid(context.getJid());
		
		MucUser mucUser = new MucUser();
		
		mucUser.getItems().add(item);
		
		if (isNoAnonymousRoom(roomSession.getRoom().getRoomConfig())) {
			mucUser.getStatuses().add(new Status(100));
		}
		mucUser.getStatuses().add(new Status(110));
		mucUser.getStatuses().add(new Status(201));
		
		Presence ack = new Presence();
		ack.setFrom(creator);
		ack.setObject(mucUser);
		
		return ack;
	}

	private boolean isNoAnonymousRoom(RoomConfig roomConfig) {
		return roomConfig.getWhoIs() == WhoIs.NONE;
	}

	private void createRoom(JabberId roomJid, JabberId creator) {
		try {
			RoomConfig roomConfig = getDefaultRoomConfig();
			roomService.createRoom(roomJid, creator, roomConfig);
		} catch (ReduplicateRoomException e) {
			throw new ProtocolException(new Conflict());
		}
	}

	private RoomConfig getDefaultRoomConfig() {
		RoomConfig roomConfig = dataObjectFactory.create(RoomConfig.class);
		roomConfig.setLang(configuration.getString(CONFIGURATION_KEY_ROOM_DEFAULT_LANG, "zh_CN"));
		roomConfig.setEnableLogging(configuration.getBoolean(COINFIGURATION_KEY_ROOM_DEFAULT_ENABLE_LOGGING, false));
		roomConfig.setChangeSubject(configuration.getBoolean(COINFIGURATION_KEY_ROOM_DEFAULT_CHANGE_SUBJECT, false));
		roomConfig.setAllowPm(getAllowPmConfig(configuration.getString(COINFIGURATION_KEY_ROOM_DEFAULT_ALLOW_PM, "anyone")));
		roomConfig.setAllowInvites(configuration.getBoolean(COINFIGURATION_KEY_ROOM_DEFAULT_ALLOW_INVITES, true));
		int maxUsers = configuration.getInteger(COINFIGURATION_KEY_ROOM_DEFAULT_MAX_USERS, 20);
		if (maxUsers != 10 && maxUsers != 20 && maxUsers != 30 && maxUsers != 50 &&
				maxUsers != 100 && maxUsers != 200 && maxUsers != 500) {
			maxUsers = 20;
			logger.warn("Unacceptable max users config: {}, Instead of using default config: {}.", maxUsers, 20);
		}
		roomConfig.setMaxUsers(maxUsers);
		roomConfig.setPresenceBroadcast(getPresenceBroadcastConfig(configuration.getString(
				COINFIGURATION_KEY_ROOM_DEFAULT_PRESENCE_BROADCAST, "moderator, participant, visitor")));
		roomConfig.setGetMemberList(getGetMemberListConfig(configuration.getString(
				COINFIGURATION_KEY_ROOM_DEFAULT_GET_MEMBER_LIST, "moderator, participant, visitor")));
		roomConfig.setPublicRoom(configuration.getBoolean(COINFIGURATION_KEY_ROOM_DEFAULT_PUBLIC_ROOM, true));
		roomConfig.setPersistentRoom(configuration.getBoolean(COINFIGURATION_KEY_ROOM_DEFAULT_PERSISTENT_ROOM, false));
		roomConfig.setMembersOnly(configuration.getBoolean(COINFIGURATION_KEY_ROOM_DEFAULT_MEMBER_ONLY, false));
		roomConfig.setWhoIs(getWhoIsConfig(configuration.getString(COINFIGURATION_KEY_ROOM_DEFAULT_WHO_IS, "none")));
		int maxHistoryFetch = configuration.getInteger(COINFIGURATION_KEY_ROOM_DEFAULT_MAX_HISTORY_FETCH, 50);
		if (maxHistoryFetch > 200) {
			maxHistoryFetch = 50;
			logger.warn("Unacceptable max history fetch config: {}, Instead of using default config: {}.", maxHistoryFetch, 50);
		}
		roomConfig.setMaxHistoryFetch(maxHistoryFetch);
		
		return roomConfig;
	}

	private AllowPm getAllowPmConfig(String allowPmConfig) {
		try {
			return AllowPm.valueOf(allowPmConfig.toUpperCase());
		} catch (Exception e) {
			logger.warn("Invalid allow pm config: {}. Instead of using default config: {}.", allowPmConfig, AllowPm.ANYONE);
			return null;
		}
		
	}

	private WhoIs getWhoIsConfig(String whoIsConfig) {
		try {
			return WhoIs.valueOf(whoIsConfig.toUpperCase());
		} catch (Exception e) {
			logger.warn("Invalid allow pm config: {}. The config will be ignored.", whoIsConfig);
			return null;
		}
	}

	private GetMemberList getGetMemberListConfig(String getMemberListConfig) {
		StringTokenizer st = new StringTokenizer(getMemberListConfig, ",");
		
		GetMemberList getMemberList = new GetMemberList();
		getMemberList.setModerator(false);
		getMemberList.setParticipant(false);
		getMemberList.setVisitor(false);
		while (st.hasMoreTokens()) {
			String option = st.nextToken().trim();
			if ("moderator".equals(option)) {
				getMemberList.setModerator(true);
			} else if ("participant".equals(option)) {
				getMemberList.setParticipant(true);
			} else if ("visitor".equals(option)) {
				getMemberList.setVisitor(true);
			} else {
				logger.warn("Invalid get member list config: {}, Instead of using default config: {}.", getMemberListConfig, "moderator, participant, visitor");
				getMemberList.setModerator(true);
				getMemberList.setParticipant(true);
				getMemberList.setVisitor(true);
				
				return getMemberList;
			}
		}
		
		return getMemberList;
	}

	private PresenceBroadcast getPresenceBroadcastConfig(String presenceBroadcastConfig) {
		StringTokenizer st = new StringTokenizer(presenceBroadcastConfig, ",");
		
		PresenceBroadcast presenceBroadcast = new PresenceBroadcast();
		presenceBroadcast.setModerator(false);
		presenceBroadcast.setParticipant(false);
		presenceBroadcast.setVisitor(false);
		while (st.hasMoreTokens()) {
			String option = st.nextToken().trim();
			if ("moderator".equals(option)) {
				presenceBroadcast.setModerator(true);
			} else if ("participant".equals(option)) {
				presenceBroadcast.setParticipant(true);
			} else if ("visitor".equals(option)) {
				presenceBroadcast.setVisitor(true);
			} else {
				logger.warn("Invalid presence broadcast config: {}, Instead of using default config: {}.", presenceBroadcast, "moderator, participant, visitor");
				presenceBroadcast.setModerator(true);
				presenceBroadcast.setParticipant(true);
				presenceBroadcast.setVisitor(true);
				
				return presenceBroadcast;
			}
		}
		
		return presenceBroadcast;
	}

	@Override
	public void setConfiguration(IConfiguration configuration) {
		mucDomainName = configuration.getString(CONFIGURATION_KEY_MUC_DOMAIN_NAME, "muc." + domainName);
		disableUserToCreateRoom = configuration.getBoolean(CONFIGURATION_KEY_DISABLE_USER_TO_CREATE_ROOM, false);
		this.configuration = configuration;
	}
	
	@Override
	public void setServerConfiguration(IServerConfiguration serverConfiguration) {
		domainName = serverConfiguration.getDomainName();
	}

	@Override
	public void setDataObjectFactory(IDataObjectFactory dataObjectFactory) {
		this.dataObjectFactory = dataObjectFactory;
	}
	
	public int getRoomOccupants(JabberId roomJid) {
		IRoomSession roomSession = roomService.getRoomSession(roomJid);
		int occupants = 0;
		for (Occupant occupant : roomSession.getOccupants()) {
			occupants += occupant.getJids().length;
		}
		
		return occupants;
	}
	
	public void process(IProcessingContext context, Presence presence, MucUser mucUser) {
		
	}
	
	public void process(IProcessingContext context, Message message, MucUser mucUser) {
		checkRoomJid(message.getTo());
		
		if (message.getType() != null) {
			throw new ProtocolException(new BadRequest("Message type must be set to null."));
		}
		
		IRoomSession roomSession = roomService.getRoomSession(message.getTo());
		if (!mucUser.getInvites().isEmpty()) {
			checkMediatedInvitationPriviliege(context, message, roomSession);
			deliverMediatedInvitation(context, message, mucUser, roomSession);
		}  else if (mucUser.getDecline() != null) {
			
		} else {
			throw new ProtocolException(new BadRequest("Unknown MUC user message."));
		}
	}

	private void addToMemberListIfNeed(JabberId roomJid, JabberId invitee) {
		roomService.addToMemberList(roomJid, invitee);
	}

	private void deliverMediatedInvitation(IProcessingContext context, Message message,
			MucUser mucUser, IRoomSession roomSession) {
		String password = null;
		if (roomSession.getRoom().getRoomConfig().isPasswordProtectedRoom()) {
			password = roomSession.getRoom().getRoomConfig().getRoomSecret();
		}
		
		for (Invite invite : mucUser.getInvites()) {
			JabberId invitee = invite.getTo();
			if (invitee == null) {
				throw new ProtocolException(new BadRequest("Null invitee."));
			}
			
			JabberId roomJid = message.getTo();
			addToMemberListIfNeed(roomJid, invitee.getBareId());
			
			if (invitee.isBareId()) {
				IResource[] resources = resourcesService.getResources(invitee);
				if (resources.length == 0) {
					processOfflineMediatedInvitation(message, mucUser);
					return;
				}
				
				for (IResource resource : resources) {
					if (resource.isAvailable()) {
						Message toInvitee = createInvitationToInvitee(context.getJid(), roomJid, invite,
								resource, password);
						context.write(toInvitee);
					}
				}				

			} else {
				IResource resource = resourcesService.getResource(invitee);
				if (resource != null) {
					Message toInvitee = createInvitationToInvitee(context.getJid(), roomJid, invite,
							resource, password);
					context.write(toInvitee);
				} else {
					processOfflineMediatedInvitation(message, mucUser);
				}
			}
		}
	}

	private void processOfflineMediatedInvitation(Message message, MucUser mucUser) {
		// TODO Auto-generated method stub
		
	}

	private Message createInvitationToInvitee(JabberId invitor, JabberId invitee, Invite fromInvitor,
			IResource resource, String password) {
		Invite toInvitee = new Invite();
		toInvitee.setFrom(invitor);
		toInvitee.setReason(fromInvitor.getReason());
		
		if (fromInvitor.getContinue() != null) {
			Continue continuee = new Continue();
			continuee.setThread(fromInvitor.getContinue().getThread());
			toInvitee.setContinue(continuee);
		}
		
		MucUser mucUser = new MucUser();
		mucUser.getInvites().add(toInvitee);
		if (password != null) {
			mucUser.setPassword(password);
		}
		
		Message message = new Message();
		message.setFrom(invitee);
		message.setTo(resource.getJid());
		message.setObject(mucUser);
		
		return message;
	}

	private void checkMediatedInvitationPriviliege(IProcessingContext context, Message message,
				IRoomSession roomSession) {
		if (roomSession.getRoom().getRoomConfig().isMembersOnly()) {
			AffiliatedUser affiliatedUser = roomSession.getRoom().getAffiliatedUser(context.getJid());
			if (affiliatedUser == null ||
					(affiliatedUser.getAffiliation() != Affiliation.ADMIN &&
						affiliatedUser.getAffiliation() != Affiliation.OWNER)) {
				throw new ProtocolException(new Forbidden());
			}
		}
	}

	public void process(IProcessingContext context, Message message, XConference xConference) {
		if (message.getTo() == null) {
			throw new ProtocolException(new JidMalformed("Null invitee."));
		}
		
		if (message.getType() != null) {
			throw new ProtocolException(new BadRequest("Message type must be set to null."));
		}
		
		JabberId roomJid = xConference.getJid();
		checkRoomJid(roomJid);
		
		IRoomSession roomSession = roomService.getRoomSession(roomJid);
		
		AffiliatedUser affiliatedUser = roomSession.getRoom().getAffiliatedUser(context.getJid());
		if (affiliatedUser == null || affiliatedUser.getAffiliation() == Affiliation.OUTCAST) {
			throw new ProtocolException(new Forbidden());
		}
		
		if (affiliatedUser.getAffiliation() == Affiliation.MEMBER &&
				!roomSession.getRoom().getRoomConfig().isAllowInvites()) {
			throw new ProtocolException(new Forbidden());
		}
		
		if (roomSession.getRoom().getRoomConfig().isPasswordProtectedRoom()) {
			xConference.setPassword(roomSession.getRoom().getRoomConfig().getRoomSecret());
		}
		
		if (!message.getTo().isBareId()) {
			IResource resource = resourcesService.getResource(message.getTo());
			if (resource != null) {
				context.write(message);
			} else {
				processOfflineDirectInvitation(message, xConference);
			}
			
		} else {
			IResource[] resources = resourcesService.getResources(message.getTo().getBareId());
			if (resources.length == 0) {
				processOfflineDirectInvitation(message, xConference);
				return;
			}
			
			for (IResource resource : resources) {
				Message toInvitee = new Message();
				toInvitee.setFrom(message.getFrom());
				toInvitee.setTo(resource.getJid());
				toInvitee.setObject(message.getObject());
				
				context.write(toInvitee);
			}
		}
	}

	private void processOfflineDirectInvitation(Message message, XConference xConference) {
		// TODO Auto-generated method stub
		
	}

	public void processRoomSubjectOrGroupChatMessage(IProcessingContext context, Message message) {
		if (!message.getSubjects().isEmpty() && message.getBodies().isEmpty()) {
			processChangeRoomSubject(context, message);
		} else {
			processGroupChatMessage(context, message);
		}

	}

	private void processChangeRoomSubject(IProcessingContext context, Message message) {
		JabberId roomJid = message.getTo();
		IRoomSession roomSession = roomService.getRoomSession(roomJid);
		
		String nick = getRoomNick(context, roomJid);
		
		if (!checkChangeSubjectPrivilege(context, roomSession, nick)) {
			throw new ProtocolException(new Forbidden());
		}
		
		message.setFrom(new JabberId(roomJid.getNode(), roomJid.getDomain(), nick));
		roomSession.setSubject(message);
		
		subjectBroadcast(context, message);
	}

	private boolean checkChangeSubjectPrivilege(IProcessingContext context, IRoomSession roomSession, String nick) {
		Occupant occupant = roomSession.getOccupant(nick);
		if (roomSession.getRoom().getRoomConfig().isChangeSubject()) {
			if (occupant.getRole() != Role.NONE && occupant.getRole() != Role.VISITOR)
				return true;
		}
		
		return occupant.getRole() == Role.MODERATOR;
	}

	private void subjectBroadcast(IProcessingContext context, Message subject) {
		IRoomSession roomSession = roomService.getRoomSession(subject.getTo());
		for (Occupant occupant : roomSession.getOccupants()) {
			for (JabberId occupantJid : occupant.getJids()) {
				Message toOccupant = StanzaCloner.clone(subject);
				toOccupant.setTo(occupantJid);
				
				context.write(toOccupant);
			}
		}
	}
	
	private String getRoomNick(IProcessingContext context, JabberId roomJid) {
		Map<JabberId, String> roomJidToNicks = MucSessionUtils.getOrCreateRoomJidToNicks(context);
		String nick = roomJidToNicks.get(roomJid);
		
		if (nick == null) {
			throw new ProtocolException(new Forbidden());
		}
		
		return nick;
	}

	private void processGroupChatMessage(IProcessingContext context, Message message) {
		if (message.getBodies().isEmpty())
			throw new ProtocolException(new BadRequest("Null message body."));
		
		JabberId roomJid = message.getTo();
		String nick = getRoomNick(context, roomJid);
		IRoomSession roomSession = roomService.getRoomSession(roomJid);
		Occupant[] occupants = roomSession.getOccupants();
		for (Occupant occupant : occupants) {
			for (JabberId contactOccupantJid : occupant.getJids()) {
				Message toContact = StanzaCloner.clone(message);
				
				toContact.setFrom(new JabberId(roomJid.getNode(), roomJid.getDomain(), nick));
				toContact.setTo(contactOccupantJid);
				
				context.write(toContact);
			}
		}
		
		roomSession.addToDiscussionHistory(nick, context.getJid(), message);
	}

	public void processGroupChatPrivateMessage(IProcessingContext context, Message message) {
		JabberId roomJid = message.getTo().getBareId();
		String nick = getRoomNick(context, roomJid);
		IRoomSession roomSession = roomService.getRoomSession(roomJid);
		String occupantNick = message.getTo().getResource();
		
		if (nick.equals(occupantNick)) {
			throw new ProtocolException(new NotAllowed("Sending message to yourself."));
		}
		
		Occupant occupant = roomSession.getOccupant(occupantNick);
		
		if (occupant == null) {
			throw new ProtocolException(new ItemNotFound());
		}
		
		for (JabberId contactOccupantJid : occupant.getJids()) {
			Message toContact = StanzaCloner.clone(message);
			
			toContact.setFrom(new JabberId(roomJid.getNode(), roomJid.getDomain(), nick));
			toContact.setTo(contactOccupantJid);
			
			context.write(toContact);
		}
		
	}

	public void process(IProcessingContext context, Presence presence) {
		// TODO Auto-generated method stub
		checkRoomJid(presence.getTo());
		
		if (presence.getTo().getResource() == null) {
			// (xep-0045 7.2.1)
			// no nickname specified
			throw new ProtocolException(new JidMalformed());
		}
		
		JabberId roomJid = presence.getTo().getBareId();
		
		if (!roomService.exists(roomJid)) {
			throw new ProtocolException(new ItemNotFound());
		}
		
		if (presence.getType() == Presence.Type.UNAVAILABLE) {
			String nick = getRoomNick(context, roomJid);
			if (!nick.equals(presence.getTo().getResource())) {
				throw new ProtocolException(new JidMalformed("Wrong nick."));
			}
			
			exitRoom(context, roomJid, nick);
		} else {
			Map<JabberId, String> roomJidToNicks = MucSessionUtils.getOrCreateRoomJidToNicks(context);
			String oldNick = roomJidToNicks.get(roomJid);
			if (oldNick == null) {
				processEnterRoom(context, presence, null);
			} else{
				String nick = presence.getTo().getResource();
				if (!oldNick.equals(nick)) {
					processChangeNick(context, presence);
				} else {
					processChangeAvailabilityStatus(context, presence);
				}
			}
		}
	}

	private void processChangeAvailabilityStatus(IProcessingContext context, Presence presence) {
		JabberId roomJid = presence.getTo().getBareId();
		IRoomSession roomSession = roomService.getRoomSession(roomJid);
		String nick = presence.getTo().getResource();
		
		sendNewOccupantPresenceToAllOccupants(context, roomSession, presence, nick);
	}

	public void exitRoom(IProcessingContext context, JabberId roomJid, String nick) {
		JabberId sessionJid = context.getJid();
		JabberId exitOccupantJid = new JabberId(roomJid.getNode(), roomJid.getDomain(), nick);
		IRoomSession roomSession = roomService.getRoomSession(roomJid);
		Occupant exitOccupant = roomSession.getOccupant(nick);
		
		roomSession.exit(sessionJid);
		Map<JabberId, String> roomJidToNicks = MucSessionUtils.getOrCreateRoomJidToNicks(context);
		roomJidToNicks.remove(roomJid);
		
		PresenceBroadcast presenceBroadcast = roomSession.getRoom().getRoomConfig().getPresenceBroadcast();
		WhoIs whoIs = roomSession.getRoom().getRoomConfig().getWhoIs();
		Affiliation affiliation = getAffiliation(roomSession, exitOccupantJid);
		
		Presence selfPresence = createExitRoomPresence(roomJid, nick, affiliation, sessionJid,
				sessionJid, shouldSendFullJid(whoIs, exitOccupant));
		MucUser mucUser = selfPresence.getObject();
		mucUser.getStatuses().add(new Status(110));
		context.write(selfPresence);
		
		for (Occupant occupant : roomSession.getOccupants()) {
			for (JabberId targetSessionJid : occupant.getJids()) {
				if (targetSessionJid.equals(sessionJid)) // ignore self
					continue;
				
				if (!shouldSendPresenceTo(presenceBroadcast, occupant))
					continue;
				
				boolean shouldSendFullJid = shouldSendFullJid(whoIs, occupant);
				Presence presence = createExitRoomPresence(roomJid, nick, affiliation, sessionJid,
						targetSessionJid, shouldSendFullJid);
				
				context.write(presence);
			}
		}
	}
	
	private Presence createExitRoomPresence(JabberId roomJid, String nick, Affiliation affiliation,
			JabberId senderFullJid, JabberId targetSessionJid, boolean shouldSendFullJid) {
		JabberId occupantJid = new JabberId(roomJid.getNode(), roomJid.getDomain(), nick);
		Item item = new Item();
		item.setAffiliation(affiliation);
		item.setRole(Role.NONE);
		
		if (shouldSendFullJid) {
			item.setJid(senderFullJid);
		}
		
		MucUser mucUser = new MucUser();
		mucUser.getItems().add(item);
		
		Presence presence = new Presence(Presence.Type.UNAVAILABLE);
		presence.setFrom(occupantJid);
		presence.setTo(targetSessionJid);
		presence.setObject(mucUser);
		
		return presence;
	}

	public void process(IProcessingContext context, Iq iq, MucAdmin mucAdmin) {
		JabberId roomJid = iq.getTo();
		checkRoomJid(roomJid);
		
		if (mucAdmin.getItems().size() == 0)
			throw new ProtocolException(new BadRequest());
		
		String kickedNick = mucAdmin.getItems().get(0).getNick();
		if (kickedNick == null) {
			throw new ProtocolException(new BadRequest());
		}
		
		IRoomSession roomSession = roomService.getRoomSession(roomJid);
		
		String kickerNick = getRoomNick(context, roomJid);
		Occupant kicker = roomSession.getOccupant(kickerNick);
		
		if (kicker.getRole() != Role.MODERATOR)
			throw new ProtocolException(new Forbidden());
		
		Occupant kicked = roomSession.getOccupant(kickedNick);
		if (kicked == null) {
			throw new ProtocolException(new ItemNotFound());
		}
		
		if (tryToKickHigherAffiliationUser(roomSession, kicker, kicked)) {
			throw new ProtocolException(new NotAllowed());
		}
		
		String reason = mucAdmin.getItems().get(0).getReason();
		AffiliatedUser kickedAffilationUser = roomSession.getRoom().getAffiliatedUser(kicked.getJids()[0]);
		Affiliation affiliation = kickedAffilationUser == null ? null : kickedAffilationUser.getAffiliation();
		
		removeKickedOccupant(context, roomSession, kicker, kicked, reason, affiliation);
		informsModeratorOfSuccess(context, iq);
		informsRemainingOccupants(context, roomSession, kicked.getNick(), affiliation);
	}

	private void removeKickedOccupant(IProcessingContext context, IRoomSession roomSession, Occupant kicker,
			Occupant kicked, String reason, Affiliation affiliation) {
		JabberId roomJid = roomSession.getRoom().getRoomJid();
		JabberId[] kickedSessionJids = kicked.getJids();
		for (JabberId kickedSessionJid : kickedSessionJids) {
			roomSession.exit(kickedSessionJid);		
			sendNotificationToKicked(context, roomJid, kicker, kickedSessionJid, kicked.getNick(), affiliation, reason);
		}
	}

	private void informsRemainingOccupants(IProcessingContext context, IRoomSession roomSession, String kickedNick, Affiliation affiliation) {
		for (Occupant occupant : roomSession.getOccupants()) {
			for (JabberId sessionJid : occupant.getJids()) {
				Item item = new Item();
				item.setAffiliation(affiliation);
				item.setRole(Role.NONE);
				
				MucUser mucUser = new MucUser();
				mucUser.getItems().add(item);
				mucUser.getStatuses().add(new Status(307));
				
				Presence notification = new Presence(Presence.Type.UNAVAILABLE);
				JabberId roomJid = roomSession.getRoom().getRoomJid();
				notification.setFrom(new JabberId(roomJid.getDomain(), roomJid.getNode(), kickedNick));
				notification.setTo(sessionJid);
				notification.setObject(mucUser);
				
				context.write(notification);
			}
		}
	}

	private void informsModeratorOfSuccess(IProcessingContext context, Iq iq) {
		Iq result = Iq.createResult(iq);
		context.write(result);
	}

	private void sendNotificationToKicked(IProcessingContext context, JabberId roomJid, Occupant kicker,
			JabberId kickedSessionJid, String kickedNick, Affiliation affiliation, String reason) {
		Actor actor = new Actor();
		actor.setNick(kicker.getNick());
		
		Item item = new Item();
		item.setAffiliation(affiliation);
		item.setRole(Role.NONE);
		item.setActor(actor);
		if (reason != null) {
			item.setReason(reason);
		}
		
		MucUser mucUser = new MucUser();
		mucUser.getItems().add(item);
		mucUser.getStatuses().add(new Status(307));
		
		Presence notification = new Presence(Presence.Type.UNAVAILABLE);
		notification.setFrom(new JabberId(roomJid.getDomain(), roomJid.getNode(), kickedNick));
		notification.setTo(kickedSessionJid);
		notification.setObject(mucUser);
		
		context.write(notification);
	}

	private boolean tryToKickHigherAffiliationUser(IRoomSession roomSession, Occupant kicker, Occupant kicked) {
		AffiliatedUser kickerUser = roomSession.getRoom().getAffiliatedUser(kicker.getJids()[0]);
		AffiliatedUser kickedUser = roomSession.getRoom().getAffiliatedUser(kicked.getJids()[0]);
		
		return getAffiliationRank(kickedUser) > getAffiliationRank(kickerUser);
	}

	private int getAffiliationRank(AffiliatedUser affiliationUser) {
		if (affiliationUser == null)
			return 0;
		
		if (affiliationUser.getAffiliation() == null)
			return 0;
		
		return affiliationUser.getAffiliation().getRank();
	}
}
