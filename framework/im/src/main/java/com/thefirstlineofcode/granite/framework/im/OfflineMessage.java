package com.thefirstlineofcode.granite.framework.im;

import java.util.Date;

import com.thefirstlineofcode.basalt.xmpp.core.JabberId;

public class OfflineMessage {
	private String messageId;
	private String message;
	private JabberId jid;
	private Date createTime;
	
	public String getMessageId() {
		return messageId;
	}
	
	public void setMessageId(String messageId) {
		this.messageId = messageId;
	}
	
	public String getMessage() {
		return message;
	}
	
	public void setMessage(String message) {
		this.message = message;
	}
	
	public JabberId getJid() {
		return jid;
	}
	
	public void setJid(JabberId jid) {
		this.jid = jid;
	}
	
	public Date getCreateTime() {
		return createTime;
	}
	
	public void setCreateTime(Date createTime) {
		this.createTime = createTime;
	}
	
}
