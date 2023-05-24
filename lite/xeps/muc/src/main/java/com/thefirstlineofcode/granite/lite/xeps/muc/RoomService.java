package com.thefirstlineofcode.granite.lite.xeps.muc;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import javax.annotation.Resource;

import org.apache.ibatis.session.SqlSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.thefirstlineofcode.basalt.xeps.muc.Affiliation;
import com.thefirstlineofcode.basalt.xeps.muc.GetMemberList;
import com.thefirstlineofcode.basalt.xeps.muc.PresenceBroadcast;
import com.thefirstlineofcode.basalt.xeps.muc.RoomConfig;
import com.thefirstlineofcode.basalt.xmpp.core.JabberId;
import com.thefirstlineofcode.basalt.xmpp.core.ProtocolException;
import com.thefirstlineofcode.basalt.xmpp.core.stanza.error.ItemNotFound;
import com.thefirstlineofcode.granite.framework.core.adf.data.IDataObjectFactory;
import com.thefirstlineofcode.granite.framework.core.adf.data.IDataObjectFactoryAware;
import com.thefirstlineofcode.granite.framework.core.auth.IAuthenticator;
import com.thefirstlineofcode.granite.xeps.muc.AffiliatedUser;
import com.thefirstlineofcode.granite.xeps.muc.IRoomRuntimeInstance;
import com.thefirstlineofcode.granite.xeps.muc.IRoomService;
import com.thefirstlineofcode.granite.xeps.muc.IRoomSession;
import com.thefirstlineofcode.granite.xeps.muc.Room;
import com.thefirstlineofcode.granite.xeps.muc.RoomItem;
import com.thefirstlineofcode.granite.xeps.muc.RoomRuntimeInstance;
import com.thefirstlineofcode.granite.xeps.muc.RoomSession;

@Component
@Transactional
public class RoomService implements IRoomService, IDataObjectFactoryAware {
	@Autowired
	private SqlSession sqlSession;
	
	@Resource(name = "authenticator")
	private IAuthenticator authenticator;
	
	private IDataObjectFactory dataObjectFactory;
	
	private ConcurrentMap<JabberId, Room> rooms;
	
	private ConcurrentMap<JabberId, IRoomRuntimeInstance> runtimeInstances;
	
	public RoomService() {
		rooms = new ConcurrentHashMap<>();
		runtimeInstances = new ConcurrentHashMap<JabberId, IRoomRuntimeInstance>();
	}

	@Override
	public int getTotalNumberOfRooms() {
		return getMapper().selectCount();
	}

	@Override
	public boolean exists(JabberId roomJid) {
		return getMapper().selectCountByJid(roomJid.toString()) != 0;
	}

	@Override
	public void createRoom(JabberId roomJid, JabberId creator, RoomConfig roomConfig) {
		D_Room room = dataObjectFactory.create(Room.class);
		room.setRoomJid(roomJid);
		room.setCreator(creator);
		getMapper().insert(room);
		
		D_RoomConfig p_roomConfig = (D_RoomConfig)roomConfig;
		p_roomConfig.setRoomId(room.getId());
		getMapper().insertRoomConfig(p_roomConfig);
		
		D_PresenceBroadcast presenceBroadcast = dataObjectFactory.create(PresenceBroadcast.class);
		presenceBroadcast.setRoomConfigId(p_roomConfig.getId());
		getMapper().insertRoomConfigPresenceBroadcast(presenceBroadcast);
		
		D_GetMemberList getMemberList = dataObjectFactory.create(GetMemberList.class);
		getMemberList.setRoomConfigId(p_roomConfig.getId());
		getMapper().insertRoomConfigGetMemberList(getMemberList);
		
		D_AffiliatedUser owner = dataObjectFactory.create(AffiliatedUser.class);
		owner.setRoomId(room.getId());
		owner.setJid(creator);
		owner.setAffiliation(Affiliation.OWNER);
		getMapper().insertRoomAffiliatedUser(owner);
	}

	@Override
	public boolean isRoomLocked(JabberId roomJid) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public void unlockRoom(JabberId roomJid) {
		getMapper().updateLocked(roomJid.toString(), false);
		
		// update cache
		rooms.remove(roomJid);
	}
	
	private RoomMapper getMapper() {
		return sqlSession.getMapper(RoomMapper.class);
	}

	@Override
	public void setDataObjectFactory(IDataObjectFactory dataObjectFactory) {
		this.dataObjectFactory = dataObjectFactory;
	}

	private Room getRoom(JabberId roomJid) {
		Room room = rooms.get(roomJid);
		
		if (room == null) {
			room = getMapper().selectByJid(roomJid.toString());
			
			if (room == null) {
				new ProtocolException(new ItemNotFound());
			}
			
			List<AffiliatedUser> affiliatedUsers = getMapper().selectAffiliatedUsersByRoomId(((D_Room)room).getId());
			for (AffiliatedUser affiliatedUser : affiliatedUsers) {
				room.addAffiliatedUser(affiliatedUser);
			}
			
			Room previous = rooms.putIfAbsent(roomJid, room);
			if (previous != null)
				room = previous;
		}
		
		return room;
	}
	
	private IRoomRuntimeInstance getRoomRuntimeInstance(JabberId roomJid) {
		IRoomRuntimeInstance runtimeInstance = runtimeInstances.get(roomJid);
		
		if (runtimeInstance == null) {
			Room room = getRoom(roomJid);
			int maxHistory = room.getRoomConfig().getMaxHistoryFetch();
			runtimeInstance = new RoomRuntimeInstance(maxHistory);
			
			IRoomRuntimeInstance previous = runtimeInstances.putIfAbsent(roomJid, runtimeInstance);
			if (previous != null) {
				runtimeInstance = previous;
			}
		}
		
		return runtimeInstance;
	}

	@Override
	public void updateRoomConfig(Room room, RoomConfig newRoomConfig) {
		D_RoomConfig pNewRoomConfig = (D_RoomConfig)newRoomConfig;
		pNewRoomConfig.setId(((D_RoomConfig)room.getRoomConfig()).getId());
		
		getMapper().updateRoomConfig(newRoomConfig);
		
		updateGetMemberList(room, newRoomConfig);
		updatePresenceBroadcast(room, newRoomConfig);
		
		addOrUpdateAdmins(room);
		addOrUpdateOwners(room);
		
		// update cache
		rooms.remove(room.getRoomJid());
	}

	private void updatePresenceBroadcast(Room room, RoomConfig newRoomConfig) {
		D_PresenceBroadcast newPresenceBroadcast = dataObjectFactory.create(PresenceBroadcast.class);
		D_PresenceBroadcast oldPresenceBroadcast = (D_PresenceBroadcast)room.getRoomConfig().getPresenceBroadcast();
		
		newPresenceBroadcast.setModerator(newRoomConfig.getPresenceBroadcast().isModerator());
		newPresenceBroadcast.setParticipant(newRoomConfig.getPresenceBroadcast().isParticipant());
		newPresenceBroadcast.setVisitor(newRoomConfig.getPresenceBroadcast().isVisitor());
		
		newPresenceBroadcast.setId(oldPresenceBroadcast.getId());
		
		getMapper().updateRoomConfigPresenceBroadcast(newPresenceBroadcast);
	}

	private void updateGetMemberList(Room room, RoomConfig newRoomConfig) {
		D_GetMemberList newGetMemberList = dataObjectFactory.create(GetMemberList.class);
		D_GetMemberList oldGetMemberList = (D_GetMemberList)room.getRoomConfig().getGetMemberList();
		
		newGetMemberList.setModerator(newRoomConfig.getGetMemberList().isModerator());
		newGetMemberList.setParticipant(newRoomConfig.getGetMemberList().isParticipant());
		newGetMemberList.setVisitor(newRoomConfig.getGetMemberList().isVisitor());
		
		newGetMemberList.setId(oldGetMemberList.getId());
		
		getMapper().updateRoomConfigGetMemberList(newGetMemberList);
	}

	private void addOrUpdateOwners(Room room) {
		for (JabberId owner : room.getRoomConfig().getOwners()) {
			if (!authenticator.exists(owner.getNode())) {
				throw new ProtocolException(new ItemNotFound("%s isn't a valid node.", owner.getNode()));
			}
			
			D_AffiliatedUser pAffiliatedUser = dataObjectFactory.create(AffiliatedUser.class);
			pAffiliatedUser.setRoomId(((D_Room)room).getId());
			pAffiliatedUser.setJid(owner.getBareId());
			pAffiliatedUser.setAffiliation(Affiliation.OWNER);
			
			AffiliatedUser affiliatedUser = room.getAffiliatedUser(owner.getBareId());
			if (affiliatedUser == null) {
				getMapper().insertRoomAffiliatedUser(pAffiliatedUser);
			} else {
				if (affiliatedUser.getAffiliation() != Affiliation.OWNER) {
					pAffiliatedUser.setId(((D_AffiliatedUser)affiliatedUser).getId());
					pAffiliatedUser.setRole(affiliatedUser.getRole());
					pAffiliatedUser.setNick(affiliatedUser.getNick());
					pAffiliatedUser.setAffiliation(Affiliation.OWNER);
					getMapper().updateRoomAffiliatedUser(pAffiliatedUser);
				}
			}
		}
	}

	private void addOrUpdateAdmins(Room room) {
		for (JabberId admin : room.getRoomConfig().getAdmins()) {
			if (!authenticator.exists(admin.getNode())) {
				throw new ProtocolException(new ItemNotFound("%s isn't a valid node", admin.getNode()));
			}
			
			D_AffiliatedUser pAffiliatedUser = dataObjectFactory.create(AffiliatedUser.class);
			pAffiliatedUser.setRoomId(((D_Room)room).getId());
			pAffiliatedUser.setJid(admin.getBareId());
			pAffiliatedUser.setAffiliation(Affiliation.ADMIN);
			
			AffiliatedUser affiliatedUser = room.getAffiliatedUser(admin.getBareId());
			if (affiliatedUser == null) {
				getMapper().insertRoomAffiliatedUser(pAffiliatedUser);
			} else {
				if (affiliatedUser.getAffiliation() != Affiliation.OWNER &&
						affiliatedUser.getAffiliation() != Affiliation.ADMIN) {
					pAffiliatedUser.setId(((D_AffiliatedUser)affiliatedUser).getId());
					pAffiliatedUser.setRole(affiliatedUser.getRole());
					pAffiliatedUser.setNick(affiliatedUser.getNick());
					pAffiliatedUser.setAffiliation(Affiliation.ADMIN);
					getMapper().updateRoomAffiliatedUser(pAffiliatedUser);
				}
			}
		}
	}
	
	@Override
	public IRoomSession getRoomSession(JabberId roomJid) {
		Room room = getRoom(roomJid.getBareId());
		
		if (room == null) {
			throw new ProtocolException(new ItemNotFound(String.format("%s doesn't exist.", roomJid)));
		}
		
		IRoomRuntimeInstance runtimeInstance = getRoomRuntimeInstance(roomJid.getBareId());
		
		return new RoomSession(room, runtimeInstance);
	}

	@Override
	public void addToMemberList(JabberId roomJid, JabberId member) {
		IRoomSession roomSession = getRoomSession(roomJid);
		AffiliatedUser affiliatedUser = roomSession.getRoom().getAffiliatedUser(member);
		if (affiliatedUser == null) {
			D_AffiliatedUser pAffiliatedUser = dataObjectFactory.create(AffiliatedUser.class);
			pAffiliatedUser.setRoomId(((D_Room)roomSession.getRoom()).getId());
			pAffiliatedUser.setJid(member.getBareId());
			pAffiliatedUser.setAffiliation(Affiliation.MEMBER);
			getMapper().insertRoomAffiliatedUser(pAffiliatedUser);
		} else {
			if (affiliatedUser.getAffiliation() == Affiliation.OUTCAST ||
					affiliatedUser.getAffiliation() == Affiliation.NONE) {
				affiliatedUser.setAffiliation(Affiliation.MEMBER);
				getMapper().updateRoomAffiliatedUser(affiliatedUser);
			}
		}
		
		// update cache
		rooms.remove(roomJid);
	}
	
	@Override
	public void updateNick(JabberId roomJid, JabberId member, String nick) {
		IRoomSession roomSession = getRoomSession(roomJid);
		AffiliatedUser affiliatedUser = roomSession.getRoom().getAffiliatedUser(member);
		affiliatedUser.setNick(nick);
		getMapper().updateRoomAffiliatedUser(affiliatedUser);
	}

	@Override
	public JabberId getAffiliatedUserJidByNick(JabberId roomJid, String nick) {
		return getMapper().selectAffiliatedUserJidByNick(roomJid, nick);
	}

	@Override
	public List<RoomItem> getRoomItems() {
		return getMapper().selectRoomItems();
	}

}
