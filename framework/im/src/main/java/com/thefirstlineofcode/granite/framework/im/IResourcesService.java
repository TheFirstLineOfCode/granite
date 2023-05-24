package com.thefirstlineofcode.granite.framework.im;

import com.thefirstlineofcode.basalt.xmpp.core.JabberId;

public interface IResourcesService {
	IResource[] getResources(JabberId jid);
	IResource getResource(JabberId jid);
}
