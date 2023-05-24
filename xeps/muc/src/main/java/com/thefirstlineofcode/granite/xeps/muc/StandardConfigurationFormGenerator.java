package com.thefirstlineofcode.granite.xeps.muc;

import com.thefirstlineofcode.basalt.xeps.muc.GetMemberList;
import com.thefirstlineofcode.basalt.xeps.muc.PresenceBroadcast;
import com.thefirstlineofcode.basalt.xeps.muc.RoomConfig;
import com.thefirstlineofcode.basalt.xeps.xdata.Field;
import com.thefirstlineofcode.basalt.xeps.xdata.Option;
import com.thefirstlineofcode.basalt.xeps.xdata.XData;

//TODO i18n support
public class StandardConfigurationFormGenerator implements IConfigurationFormGenerator {
	private Room room;
	
	public StandardConfigurationFormGenerator(Room room) {
		this.room = room;
	}

	@Override
	public XData generate() {
		XData xData = createConfigurationForm(room);
		xData.setTitle(String.format("Configuration for \"%s\" Room", room.getRoomJid()));
		xData.getInstructions().add(createInstructions(room));
		
		return xData;
	}
	
	private XData createConfigurationForm(Room room) {
		XData xData = new XData(XData.Type.FORM);
		
		RoomConfig roomConfig = room.getRoomConfig();
		xData.getFields().add(createFormTypeField());
		xData.getFields().add(createRoomNameField(roomConfig));
		xData.getFields().add(createRoomDescField(roomConfig));
		xData.getFields().add(createLangField(roomConfig));
		xData.getFields().add(createEnalbleLoggingField(roomConfig));
		xData.getFields().add(createChangeSubjectField(roomConfig));
		xData.getFields().add(createAllowInvitesField(roomConfig));
		xData.getFields().add(createAllowPmField(roomConfig));
		xData.getFields().add(createMaxUsersField(roomConfig));
		xData.getFields().add(createPresenceBroadcastField(roomConfig));
		xData.getFields().add(createGetMemberListField(roomConfig));
		xData.getFields().add(createPublicRoomField(roomConfig));
		xData.getFields().add(createPersistentRoomField(roomConfig));
		xData.getFields().add(createModeratedRoomField(roomConfig));
		xData.getFields().add(createMemeberOnlyField(roomConfig));
		xData.getFields().add(createPasswordProtectedRoomField(roomConfig));
		xData.getFields().add(createRoomSecretHintField());
		xData.getFields().add(createRoomSecretField(roomConfig));
		xData.getFields().add(createWhoIsField(roomConfig));
		xData.getFields().add(createMaxHistoryFetchField(roomConfig));
		xData.getFields().add(createRoomAdminsHintField());
		xData.getFields().add(createRoomAdminsField(room));
		xData.getFields().add(createRoomOwnersHintField());
		xData.getFields().add(createRoomOwnersField(room));
		
		return xData;
	}

	private Field createRoomOwnersField(Room room) {
		Field field = new Field();
		field.setLabel("Room Owners");
		field.setType(Field.Type.JID_MULTI);
		field.setVar("muc#roomconfig_roomowners");
		
		/*for (AffiliatedUser au : room.getAffiliatedUsers()) {
			if (au.getAffiliation() == Affiliation.OWNER) {
				field.getValues().add(au.getJid().toString());
			}
		}*/
		
		return field;
	}

	private Field createRoomOwnersHintField() {
		Field field = new Field();
		field.setType(Field.Type.FIXED);
		field.getValues().add("You may specify additional owners for this room. Please provide one Jabber ID per line.");
		
		return field;
	}

	private Field createRoomAdminsField(Room room) {
		Field field = new Field();
		field.setLabel("Room Admins");
		field.setType(Field.Type.JID_MULTI);
		field.setVar("muc#roomconfig_roomadmins");
		
		/*for (AffiliatedUser au : room.getAffiliatedUsers()) {
			if (au.getAffiliation() == Affiliation.ADMIN) {
				field.getValues().add(au.getJid().toString());
			}
		}*/
		
		return field;
	}

	private Field createRoomAdminsHintField() {
		Field field = new Field();
		field.setType(Field.Type.FIXED);
		field.getValues().add("You may specify additional people who have admin status in the room. Please provide one Jabber ID per line.");
		
		return field;
	}

	private Field createMaxHistoryFetchField(RoomConfig roomConfig) {
		Field field = new Field();
		field.setLabel("Maximum Number of History Messages Returned by Room");
		field.setType(Field.Type.TEXT_SINGLE);
		field.setVar("muc#maxhistoryfetch");
		field.getValues().add(Integer.toString(roomConfig.getMaxHistoryFetch()));
		
		return field;
	}

	private Field createWhoIsField(RoomConfig roomConfig) {
		Field field = new Field();
		field.setLabel("Who May Discover Real JIDs?");
		field.setType(Field.Type.LIST_SINGLE);
		field.setVar("muc#roomconfig_whois");
		field.getOptions().add(new Option("Moderators Only", "moderators"));
		field.getOptions().add(new Option("Anyone", "anyone"));
		field.getOptions().add(new Option("None", "none"));
		
		if (roomConfig.getWhoIs() != null) {
			field.getValues().add(roomConfig.getWhoIs().toString());
		}
		
		return field;
	}

	private Field createRoomSecretHintField() {
		Field field = new Field();
		field.setType(Field.Type.FIXED);
		field.getValues().add("If a password is required to enter this room, you must specify the password below.");
		
		return field;
	}

	private Field createRoomSecretField(RoomConfig roomConfig) {
		Field field = new Field();
		field.setLabel("Password");
		field.setType(Field.Type.TEXT_PRIVATE);
		field.setVar("muc#roomconfig_roomsecret");
		
		return field;
	}

	private Field createPasswordProtectedRoomField(RoomConfig roomConfig) {
		Field field = new Field();
		field.setLabel("Password Required to Enter?");
		field.setType(Field.Type.BOOLEAN);
		field.setVar("muc#roomconfig_passwordprotectedroom");
		field.getValues().add(roomConfig.isPasswordProtectedRoom() ? "1" : "0");
		
		return field;
	}

	private Field createMemeberOnlyField(RoomConfig roomConfig) {
		Field field = new Field();
		field.setLabel("Make Room Members-Only?");
		field.setType(Field.Type.BOOLEAN);
		field.setVar("muc#roomconfig_membersonly");
		field.getValues().add(roomConfig.isMembersOnly() ? "1" : "0");
		
		return field;
	}

	private Field createModeratedRoomField(RoomConfig roomConfig) {
		Field field = new Field();
		field.setLabel("Make Room Moderated?");
		field.setType(Field.Type.BOOLEAN);
		field.setVar("muc#roomconfig_moderatedroom");
		field.getValues().add(roomConfig.isModeratedRoom() ? "1" : "0");
		
		return field;
	}

	private Field createPersistentRoomField(RoomConfig roomConfig) {
		Field field = new Field();
		field.setLabel("Make Room Persistent?");
		field.setType(Field.Type.BOOLEAN);
		field.setVar("muc#roomconfig_persistentroom");
		field.getValues().add(roomConfig.isPersistentRoom() ? "1" : "0");
		
		return field;
	}

	private Field createPublicRoomField(RoomConfig roomConfig) {
		Field field = new Field();
		field.setLabel("Make Room Publicly Searchable?");
		field.setType(Field.Type.BOOLEAN);
		field.setVar("muc#roomconfig_publicroom");
		field.getValues().add(roomConfig.isPublicRoom() ? "1" : "0");
		
		return field;
	}

	private Field createGetMemberListField(RoomConfig roomConfig) {
		Field field = new Field();
		field.setLabel("Roles and Affiliations that May Retrieve Member List");
		field.setType(Field.Type.LIST_MULTI);
		field.setVar("muc#roomconfig_getmemberlist");
		
		field.getOptions().add(new Option("Moderator", "moderator"));
		field.getOptions().add(new Option("Participant", "participant"));
		field.getOptions().add(new Option("Visitor", "visitor"));
		
		GetMemberList getMemberList = roomConfig.getGetMemberList();
		if (getMemberList.isModerator()) {
			field.getValues().add("moderator");
		}
		if (getMemberList.isParticipant()) {
			field.getValues().add("participant");
		}
		if (getMemberList.isVisitor()) {
			field.getValues().add("visitor");
		}
		
		return field;
	}

	private Field createPresenceBroadcastField(RoomConfig roomConfig) {
		Field field = new Field();
		field.setLabel("Roles for which Presence is Broadcasted");
		field.setType(Field.Type.LIST_MULTI);
		field.setVar("muc#roomconfig_presencebroadcast");
		
		field.getOptions().add(new Option("Moderator", "moderator"));
		field.getOptions().add(new Option("Participant", "participant"));
		field.getOptions().add(new Option("Visitor", "visitor"));
		
		PresenceBroadcast presenceBroadcast = roomConfig.getPresenceBroadcast();
		if (presenceBroadcast.isModerator()) {
			field.getValues().add("moderator");
		}
		if (presenceBroadcast.isParticipant()) {
			field.getValues().add("participant");
		}
		if (presenceBroadcast.isVisitor()) {
			field.getValues().add("visitor");
		}
		
		return field;
	}

	private Field createMaxUsersField(RoomConfig roomConfig) {
		Field field = new Field();
		field.setLabel("Maximum Number of Occupants");
		field.setType(Field.Type.LIST_SINGLE);
		field.setVar("muc#roomconfig_maxusers");
		field.getValues().add(Integer.toString(roomConfig.getMaxUsers()));
		
		return field;
	}

	private Field createAllowPmField(RoomConfig roomConfig) {
		Field field = new Field();
		field.setLabel("Who Can Send Private Messages?");
		field.setType(Field.Type.LIST_SINGLE);
		field.setVar("muc#roomconfig_allowpm");
		if (roomConfig.getAllowPm() != null) {
			field.getValues().add(roomConfig.getAllowPm().toString());
		}
		
		return field;
	}

	private Field createAllowInvitesField(RoomConfig roomConfig) {
		Field field = new Field();
		field.setLabel("Allow Occupants to Invite Others?");
		field.setType(Field.Type.BOOLEAN);
		field.setVar("muc#roomconfig_allowinvites");
		field.getValues().add(roomConfig.isAllowInvites() ? "1" : "0");
		
		return field;
	}

	private Field createChangeSubjectField(RoomConfig roomConfig) {
		Field field = new Field();
		field.setLabel("Allow Occupants to Change Subject?");
		field.setType(Field.Type.BOOLEAN);
		field.setVar("muc#roomconfig_changesubject");
		field.getValues().add(roomConfig.isChangeSubject() ? "1" : "0");
		
		return field;
	}

	private Field createEnalbleLoggingField(RoomConfig roomConfig) {
		Field field = new Field();
		field.setLabel("Enable Public Logging?");
		field.setType(Field.Type.BOOLEAN);
		field.setVar("muc#roomconfig_enablelogging");
		field.getValues().add(roomConfig.isEnableLogging() ? "1" : "0");
		
		return field;
	}

	private Field createLangField(RoomConfig roomConfig) {
		Field field = new Field();
		field.setLabel("Natural Language for Room Discussions");
		field.setType(Field.Type.TEXT_SINGLE);
		field.setVar("muc#roomconfig_lang");
		if (roomConfig.getLang() != null) {
			field.getValues().add(roomConfig.getLang());
		}
		
		return field;
	}

	private Field createRoomDescField(RoomConfig roomConfig) {
		Field field = new Field();
		field.setLabel("Short Description of Room");
		field.setType(Field.Type.TEXT_SINGLE);
		field.setVar("muc#roomconfig_roomdesc");
		if (roomConfig.getRoomDesc() != null) {
			field.getValues().add(roomConfig.getRoomDesc());
		}
		
		return field;
	}

	private Field createRoomNameField(RoomConfig roomConfig) {
		Field field = new Field();
		field.setLabel("Natural-Language Room Name");
		field.setType(Field.Type.TEXT_SINGLE);
		field.setVar("muc#roomconfig_roomname");
		if (roomConfig.getRoomName() != null) {
			field.getValues().add(roomConfig.getRoomName());
		}
		
		return field;
	}

	private Field createFormTypeField() {
		Field field = new Field();
		field.setType(Field.Type.HIDDEN);
		field.setVar("FORM_TYPE");
		field.getValues().add("http://jabber.org/protocol/muc#roomconfig");
		
		return field;
	}

	private String createInstructions(Room room) {
		StringBuilder sb = new StringBuilder();
		sb.append(String.format("Your room %s has been created!\n", room.getRoomJid()));
		sb.append("The default configuration is as follows:\n");
		
		RoomConfig roomConfig = room.getRoomConfig();
		if (roomConfig.isEnableLogging()) {
			sb.append("  - No logging\n");
		} else {
			sb.append("  - Logging");
		}
		
		if (roomConfig.isModeratedRoom()) {
			sb.append("  - Moderation\n");
		} else {
			sb.append("  - No moderation\n");
		}
		
		sb.append(String.format("  - Up to %d occupants\n", room.getRoomConfig().getMaxUsers()));
		
		if (roomConfig.isPasswordProtectedRoom()) {
			sb.append("  - Password required\n");
		} else {
			sb.append("  - No password required\n");
		}
		
		if (roomConfig.isMembersOnly()) {
			sb.append("  - Room is member only\n");
		} else {
			sb.append("  - Room is open\n");
		}
		
		if (roomConfig.isPersistentRoom()) {
			sb.append("  - Room is persistent\n");
		} else {
			sb.append("  - Room is not persistent\n");
		}
		
		if (roomConfig.isChangeSubject()) {
			sb.append("  - Only admins may change the subject\n");
		} else {
			sb.append("  - Anyone may change the subject\n");
		}
		
		PresenceBroadcast presenceBroadcast = roomConfig.getPresenceBroadcast();
		if ((roomConfig.isMembersOnly() && presenceBroadcast.isModerator() && presenceBroadcast.isParticipant()) ||
				(!roomConfig.isMembersOnly() && presenceBroadcast.isVisitor() && presenceBroadcast.isModerator() &&
						presenceBroadcast.isParticipant())) {
			sb.append("  - Presence broadcasted for all users\n");
		} else {
			String roles = getPresenceBroadcastRoles(presenceBroadcast, roomConfig.isMembersOnly());
			if (roles == null) {
				sb.append("  - Presence not broadcasted for anyone\n");
			} else {
				sb.append(String.format("  - Presence broadcasted for %\n", roles));
			}
		}
		
		sb.append("To accept the default configuration, click OK.").
			append(" To select a different configuration, please complete this form.");
		
		return sb.toString();
	}

	private String getPresenceBroadcastRoles(PresenceBroadcast presenceBroadcast, boolean memberOnly) {
		StringBuilder sb = new StringBuilder();
		if (presenceBroadcast.isModerator()) {
			sb.append("moderator, ");
		}
		
		if (presenceBroadcast.isParticipant()) {
			sb.append("participant, ");
		}
		
		if (!memberOnly && presenceBroadcast.isVisitor()) {
			sb.append("visitor");
		}
		
		if (sb.length() == 0) {
			return null;
		}
		
		if (sb.charAt(sb.length() - 1) == ' ') {
			sb.delete(sb.length() - 2, sb.length());
		}
		
		return sb.toString();
	}

}
