package com.thefirstlineofcode.granite.lite.xeps.muc;

import com.thefirstlineofcode.granite.framework.core.adf.data.IIdProvider;
import com.thefirstlineofcode.granite.xeps.muc.Subject;

public class D_Subject extends Subject implements IIdProvider<String> {
	private String id;
	private String roomId;

	@Override
	public void setId(String id) {
		this.id = id;
	}

	@Override
	public String getId() {
		return id;
	}

	public String getRoomId() {
		return roomId;
	}

	public void setRoomId(String roomId) {
		this.roomId = roomId;
	}
	
}
