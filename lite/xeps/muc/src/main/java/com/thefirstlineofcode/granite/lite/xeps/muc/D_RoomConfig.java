package com.thefirstlineofcode.granite.lite.xeps.muc;

import com.thefirstlineofcode.basalt.xeps.muc.RoomConfig;
import com.thefirstlineofcode.granite.framework.core.adf.data.IIdProvider;

public class D_RoomConfig extends RoomConfig implements IIdProvider<String> {
	private String id;
	private String roomId;
	
	@Override
	public String getId() {
		return id;
	}
	
	@Override
	public void setId(String id) {
		this.id = id;
	}

	public String getRoomId() {
		return roomId;
	}

	public void setRoomId(String roomId) {
		this.roomId = roomId;
	}
	
}
