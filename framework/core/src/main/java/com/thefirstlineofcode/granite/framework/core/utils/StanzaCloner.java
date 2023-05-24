package com.thefirstlineofcode.granite.framework.core.utils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.thefirstlineofcode.basalt.xmpp.core.stanza.Iq;
import com.thefirstlineofcode.basalt.xmpp.core.stanza.Stanza;
import com.thefirstlineofcode.basalt.xmpp.im.stanza.Message;
import com.thefirstlineofcode.basalt.xmpp.im.stanza.Presence;

public final class StanzaCloner {
	private StanzaCloner() {}
	
	@SuppressWarnings("unchecked")
	public static <T extends Stanza> T clone(T stanza) {
		if (stanza instanceof Presence) {
			return (T)clone((Presence)stanza);
		} else if (stanza instanceof Message) {
			return (T)clone((Message)stanza);
		} else {
			return (T)clone((Iq)stanza);
		}
	}
	
	public static Presence clone(Presence presence) {
		Presence cloned = new Presence();
		
		cloned = cloneCommons(presence, cloned);
		
		cloned.setType(presence.getType());
		cloned.setStatuses(presence.getStatuses());
		cloned.setShow(presence.getShow());
		cloned.setPriority(presence.getPriority());
		
		cloned = cloneXep(presence, cloned);
		
		return cloned;
	}
	
	public static Message clone(Message message) {
		Message cloned = new Message();
		
		cloned = cloneCommons(message, cloned);
		
		cloned.setType(message.getType());
		cloned.setSubjects(message.getSubjects());
		cloned.setBodies(message.getBodies());
		cloned.setThread(message.getThread());
		
		cloned = cloneXep(message, cloned);
		
		return cloned;
	}
	
	public static Iq clone(Iq iq) {
		Iq cloned = new Iq();
		
		cloned = cloneCommons(iq, cloned);
		
		cloned = cloneXep(iq, cloned);
		cloned.setType(iq.getType());
		
		return cloned;
	}
	
	private static <T extends Stanza> T cloneCommons(Stanza original, T cloned) {
		cloned.setId(original.getId());
		cloned.setFrom(original.getFrom());
		cloned.setTo(original.getTo());
		cloned.setLang(original.getLang());
		
		return cloned;
	}
	
	public static <T extends Stanza> T cloneXep(Stanza original, T cloned) {
		cloned.setOriginalMessage(original.getOriginalMessage());
		cloned.setObjects(copyList(original.getObjects()));
		cloned.setObjectProtocols(copyMap(original.getObjectProtocols()));
		
		return cloned;
	}
	
	private static <T> List<T> copyList(List<T> source) {
		return new ArrayList<>(source);
	}
	
	private static <K, V> Map<K, V> copyMap(Map<K, V> source) {
		return new HashMap<>(source);
	}
}
