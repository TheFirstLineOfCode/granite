package com.thefirstlineofcode.granite.stream.standard.codec;

import java.io.UnsupportedEncodingException;

import org.apache.mina.core.buffer.IoBuffer;
import org.apache.mina.core.future.IoFuture;
import org.apache.mina.core.future.IoFutureListener;
import org.apache.mina.core.future.WriteFuture;
import org.apache.mina.core.session.AbstractIoSession;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.filter.codec.ProtocolEncoderAdapter;
import org.apache.mina.filter.codec.ProtocolEncoderOutput;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.thefirstlineofcode.basalt.oxm.binary.IBinaryXmppProtocolConverter;
import com.thefirstlineofcode.basalt.xmpp.Constants;

public class MessageEncoder extends ProtocolEncoderAdapter {
	private static final Logger logger = LoggerFactory.getLogger(MessageEncoder.class);
	
	private static final char CHAR_HEART_BEAT = ' ';
	private static final byte BYTE_HEART_BEAT = (byte)CHAR_HEART_BEAT;
	
	private IBinaryXmppProtocolConverter bxmppProtocolConverter;
	
	public MessageEncoder() {}
	
	public MessageEncoder(IBinaryXmppProtocolConverter bxmppProtocolConverter) {
		this.bxmppProtocolConverter = bxmppProtocolConverter;
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public void encode(IoSession session, Object message, ProtocolEncoderOutput out) throws Exception {
		if (message instanceof String) {
			if (bxmppProtocolConverter != null) {
				out.write(IoBuffer.wrap(bxmppProtocolConverter.toBinary((String)message)));
			} else {
				out.write(IoBuffer.wrap(((String)message).getBytes(Constants.DEFAULT_CHARSET)));
			}
			
			if (logger.isTraceEnabled())
				logger.trace("Server wrote a message '{}' to client.", message);
		} else if (isHeartBeatChar(message)) {
			out.write(IoBuffer.wrap((byte[])message));
			
			if (logger.isTraceEnabled())
				logger.trace("Server wrote a heartbeat char to client.");
		} else if (isHeartBeatByte(message)) {
			out.write(IoBuffer.wrap(new byte[] {(byte)message}));
			
			if (logger.isTraceEnabled())
				logger.trace("Server wrote a heartbeat byte to client.");
		} else {
			throw new RuntimeException(String.format("Unknown message type: %s.", message.getClass().getName()));
		}
		
		WriteFuture future = out.flush();
		future.addListener(new IoFutureListener<IoFuture>() {
			@Override
			public void operationComplete(IoFuture future) {
				((AbstractIoSession)future.getSession()).getProcessor().flush(future.getSession());
			}
		});
		
	}

	private boolean isHeartBeatByte(Object message) {
		if (bxmppProtocolConverter == null)
			return false;
		
		if (!(message instanceof Byte))
			return false;
		
		return  ((Byte)message).byteValue() == BYTE_HEART_BEAT;
	}

	private boolean isHeartBeatChar(Object message) {
		if (bxmppProtocolConverter != null)
			return false;
		
		if (!(message instanceof byte[]))
			return false;
		
		try {
			return isHeartBeatString(new String((byte[])message, Constants.DEFAULT_CHARSET));
		} catch (UnsupportedEncodingException e) {
			throw new RuntimeException(String.format("%s not supported.", Constants.DEFAULT_CHARSET), e);
		}
	}

	private boolean isHeartBeatString(String message) {
		for (char c : message.toCharArray()) {
			if (c != CHAR_HEART_BEAT)
				return false;
		}
		
		return true;
	}

}
