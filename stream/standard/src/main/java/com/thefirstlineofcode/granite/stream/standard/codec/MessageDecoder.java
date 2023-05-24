package com.thefirstlineofcode.granite.stream.standard.codec;

import java.io.UnsupportedEncodingException;

import org.apache.mina.core.buffer.IoBuffer;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.filter.codec.CumulativeProtocolDecoder;
import org.apache.mina.filter.codec.ProtocolDecoderOutput;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.thefirstlineofcode.basalt.oxm.preprocessing.IMessagePreprocessor;
import com.thefirstlineofcode.basalt.xmpp.Constants;

public class MessageDecoder extends CumulativeProtocolDecoder {
	private static final char CHAR_HEART_BEAT = ' ';
	private static final byte BYTE_HEART_BEAT = (byte)CHAR_HEART_BEAT;
	private static final byte[] BYTES_OF_HEART_BEAT_CHAR =  getBytesOfHeartBeatChar();

	private static final Logger logger = LoggerFactory.getLogger(MessageDecoder.class);
	
	private static int DEFAULT_MAX_BUFFER_SIZE = 1024 * 1024;
	private static final String GRANITE_DECODER_MESSAGE_PARSER = "granite.decoder.message.parser";
	
	private int maxBufferSize;
	private IMessagePreprocessor binaryMessagePrecessor;
	
	public MessageDecoder() {
		this(DEFAULT_MAX_BUFFER_SIZE);
	}
	
	public MessageDecoder(int maxBufferSize) {
		this(maxBufferSize, null);
	}
	
	public MessageDecoder(IMessagePreprocessor binaryMessagePrecessor) {
		this(DEFAULT_MAX_BUFFER_SIZE, binaryMessagePrecessor);
	}

	public MessageDecoder(int maxBufferSize, IMessagePreprocessor binaryMessagePrecessor) {
		this.maxBufferSize = maxBufferSize;
		this.binaryMessagePrecessor = binaryMessagePrecessor;
	}
	
	private static byte[] getBytesOfHeartBeatChar() {
		try {
			return String.valueOf(CHAR_HEART_BEAT).getBytes(Constants.DEFAULT_CHARSET);
		} catch (UnsupportedEncodingException e) {
			throw new RuntimeException(String.format("%s not supported!", Constants.DEFAULT_CHARSET), e);
		}
	}

	@Override
	protected boolean doDecode(IoSession session, IoBuffer in, ProtocolDecoderOutput out) throws Exception {
		IMessageParser messageParser = (IMessageParser)session.getAttribute(GRANITE_DECODER_MESSAGE_PARSER);
		if (messageParser == null) {
			messageParser = new MessageParser(maxBufferSize, binaryMessagePrecessor);
			session.setAttribute(GRANITE_DECODER_MESSAGE_PARSER, messageParser);
		}
		
		String[] messages = null;
		try {
			messages = messageParser.parse(in);
		} catch (Exception e) {
			if (logger.isDebugEnabled())
				logger.debug("Decode error.", e);
			
			session.removeAttribute(GRANITE_DECODER_MESSAGE_PARSER);
			
			// throw e;
		}
		
		if (messages != null) {
			for (String message : messages) {
				if (isHeartbeats(message)) {
					if (logger.isTraceEnabled())
						logger.trace("Heartbeats decoded: '{}'. Server will write a heartbeat response to client.", message);
					
					writeHeartbeatToClient(session);
					out.write(message);
				} else {
					if (logger.isTraceEnabled())
						logger.trace("Message decoded: '{}'.", message);
					
					out.write(message);					
				}
			}
		}
		
		return !in.hasRemaining();
	}

	private void writeHeartbeatToClient(IoSession session) {
		if (binaryMessagePrecessor != null) {
			session.write(BYTE_HEART_BEAT);
		} else {
			session.write(BYTES_OF_HEART_BEAT_CHAR);
		}
	}

	private boolean isHeartbeats(String message) {
		for (char c : message.toCharArray()) {
			if (!(CHAR_HEART_BEAT == c))
				return false;
		}
		
		return true;
	}
}
