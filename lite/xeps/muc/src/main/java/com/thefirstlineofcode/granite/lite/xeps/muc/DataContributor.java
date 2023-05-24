package com.thefirstlineofcode.granite.lite.xeps.muc;

import org.pf4j.Extension;

import com.thefirstlineofcode.granite.framework.adf.mybatis.DataContributorAdapter;

@Extension
public class DataContributor extends DataContributorAdapter {
	@Override
	protected Class<?>[] getDataObjects() {
		return new Class<?>[] {
			D_AffiliatedUser.class,
			D_GetMemberList.class,
			D_PresenceBroadcast.class,
			D_Room.class,
			D_RoomConfig.class,
			D_RoomItem.class,
			D_Subject.class
		};
	}
	
	@Override
	protected String[] getInitScriptFileNames() {
		return new String[] {
			"muc.sql"	
		};
	}
	
	@Override
	protected String[] getMapperFileNames() {
		return new String[] {
			"RoomMapper.xml"
		};
	}

}
