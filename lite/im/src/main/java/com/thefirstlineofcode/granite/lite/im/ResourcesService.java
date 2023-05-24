package com.thefirstlineofcode.granite.lite.im;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.springframework.stereotype.Component;

import com.thefirstlineofcode.basalt.xmpp.core.JabberId;
import com.thefirstlineofcode.basalt.xmpp.im.stanza.Presence;
import com.thefirstlineofcode.granite.framework.im.IResource;
import com.thefirstlineofcode.granite.framework.im.IResourcesRegister;
import com.thefirstlineofcode.granite.framework.im.IResourcesService;
import com.thefirstlineofcode.granite.framework.im.ResourceRegistrationException;

@Component
public class ResourcesService implements IResourcesService, IResourcesRegister {
	private ConcurrentMap<String, Resources> bareIdToResources = new ConcurrentHashMap<>();
	
	@Override
	public IResource[] getResources(JabberId jid) {
		Resources resources = bareIdToResources.get(jid.getBareIdString());
		if (resources == null)
			return new IResource[0];
		
		return resources.getResources();
	}
	
	@Override
	public void register(JabberId jid) throws ResourceRegistrationException {
		checkFullJid(jid);
		
		getResourcesByJid(jid).register(jid);
	}
	
	private void checkFullJid(JabberId jid) {
		if (jid.getNode() == null || jid.getResource() == null) {
			throw new IllegalArgumentException("Need a full JID.");
		}
	}
	
	@Override
	public void unregister(JabberId jid) throws ResourceRegistrationException  {
		checkFullJid(jid);
		
		Resources resources = bareIdToResources.get(jid.getBareIdString());
		if (resources != null) {
			resources.unregister(jid);
		}
	}
	
	@Override
	public void setRosterRequested(JabberId jid) throws ResourceRegistrationException  {
		checkFullJid(jid);
		
		getResourcesByJid(jid).setRosterRequested(jid);
	}
	
	@Override
	public void setBroadcastPresence(JabberId jid, Presence presence) throws ResourceRegistrationException  {
		checkFullJid(jid);
		
		getResourcesByJid(jid).setPresence(jid, presence);
	}
	
	@Override
	public void setAvailable(JabberId jid) throws ResourceRegistrationException  {
		checkFullJid(jid);
		
		getResourcesByJid(jid).setAvailable(jid);
	}

	private Resources getResourcesByJid(JabberId jid) {
		String bareId = jid.getBareIdString();
		Resources resources = bareIdToResources.get(bareId);
		if (resources == null) {
			resources = new Resources(bareId);
			Resources oldResources = bareIdToResources.putIfAbsent(bareId, resources);
			if (oldResources != null)
				resources = oldResources;
		}
		
		return resources;
	}
	
	private Resource getResourceByJid(JabberId jid) {
		String bareId = jid.getBareIdString();
		Resources resources = bareIdToResources.get(bareId);
		if (resources == null) {
			return null;
		}
		
		for (IResource resource : resources.getResources()) {
			if (resource.getJid().equals(jid))
				return (Resource)resource;
		}
		
		return null;
	}
	
	private class Resources {
		private String bareId;
		private List<Resource> resources;
		private boolean removed = false;
		
		public Resources(String bareId) {
			this.bareId = bareId;
			resources = new ArrayList<>();
		}
		
		public synchronized IResource[] getResources() {
			if (resources.size() == 0) {
				return new IResource[0];
			}
			
			return resources.toArray(new IResource[resources.size()]);
		}
		
		public void register(JabberId jid) throws ResourceRegistrationException {
			synchronized (this) {
				if (!removed) {
					Resource resource = new Resource(jid);
					resources.add(resource);
					return;
				}
			}
			
			ResourcesService.this.register(jid);
		}
		
		public void setRosterRequested(JabberId jid) throws ResourceRegistrationException {
			synchronized (this) {
				if (!removed) {
					for (Resource resource : resources) {
						if (resource.getJid().equals(jid)) {
							resource.rosterRequested = true;
						}
					}
					
					return;
				}
			}
			
			ResourcesService.this.setRosterRequested(jid);
		}
		
		public void setPresence(JabberId jid, Presence presence) throws ResourceRegistrationException {
			synchronized (this) {
				if (!removed) {
					for (Resource resource : resources) {
						if (resource.getJid().equals(jid)) {
							resource.broadcastPresence = presence;
						}
					}
					
					return;
				}
			}
			
			ResourcesService.this.setBroadcastPresence(jid, presence);
		}
		
		public void setDirectedPresence(JabberId from, Presence presence) throws ResourceRegistrationException {
			synchronized (this) {
				if (!removed) {
					for (Resource resource : resources) {
						resource.directedPresences.put(from, presence);
					}
					
					return;
				}
			}
			
			ResourcesService.this.setDirectedPresence(from, JabberId.parse(bareId), presence);
		}
		
		public void setAvailable(JabberId jid) throws ResourceRegistrationException {
			synchronized (this) {
				if (!removed) {
					for (Resource resource : resources) {
						if (resource.getJid().equals(jid)) {
							resource.available = true;
						}
					}
					
					return;
				}
			}
			
			ResourcesService.this.setAvailable(jid);
		}
		
		public void unregister(JabberId jid) throws ResourceRegistrationException {
			synchronized(this) {
				if (!removed) {
					Resource toBeRemoved = null;
					for (Resource resource : resources) {
						if (jid.equals(resource.getJid())) {
							toBeRemoved  = resource;
							break;
						}
					}
					
					if (toBeRemoved != null) {
						resources.remove(toBeRemoved);
					}
					
					if (resources.size() == 0) {
						ResourcesService.this.bareIdToResources.remove(bareId);
						removed = true;
					}
					
					return;
				}
			}
			
			ResourcesService.this.unregister(jid);
		}
		
		
	}
	
	private class Resource implements IResource {
		private JabberId jid;
		private volatile boolean rosterRequested;
		private volatile Presence broadcastPresence;
		private volatile boolean available;
		private ConcurrentMap<JabberId, Presence> directedPresences;
		
		public Resource(JabberId jid) {
			this.jid = jid;
			rosterRequested = false;
			directedPresences = new ConcurrentHashMap<>();
		}

		@Override
		public JabberId getJid() {
			return jid;
		}

		@Override
		public boolean isRosterRequested() {
			return rosterRequested;
		}

		@Override
		public Presence getBroadcastPresence() {
			return broadcastPresence;
		}
		
		@Override
		public boolean isAvailable() {
			return available;
		}

		@Override
		public Presence getDirectedPresence(JabberId from) {
			return directedPresences.get(from);
		}
	}

	@Override
	public IResource getResource(JabberId jid) {
		if (jid.getResource() == null)
			return null;
		
		IResource[] resources = getResources(jid);
		for (IResource resource : resources) {
			if (resource.getJid().equals(jid))
				return resource;
		}
		
		return null;
	}

	@Override
	public void setDirectedPresence(JabberId from, JabberId to, Presence presence) throws ResourceRegistrationException  {
		checkFullJid(from);
		
		if (to.getResource() == null) {
			getResourcesByJid(to).setDirectedPresence(from, presence);
		} else {
			Resource resource = getResourceByJid(to);
			if (resource != null) {
				resource.directedPresences.put(from, presence);
			}
		}
	}

}
