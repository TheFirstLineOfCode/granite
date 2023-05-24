package com.thefirstlineofcode.granite.stream.standard.codec;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CoderResult;
import java.nio.charset.CodingErrorAction;

import org.apache.mina.core.buffer.IoBuffer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.thefirstlineofcode.basalt.oxm.preprocessing.IMessagePreprocessor;
import com.thefirstlineofcode.basalt.oxm.preprocessing.XmlMessagePreprocessorAdapter;
import com.thefirstlineofcode.basalt.xmpp.Constants;

public class MessageParser implements IMessageParser {
	private Logger logger = LoggerFactory.getLogger(MessageParser.class);
	
	private static int DEFAULT_MAX_BUFFER_SIZE = 1024 * 1024;
	
	private boolean useXmlMessageFormat;
	private IMessagePreprocessor preprocessor;
	private Charset charset;
	private CharsetDecoder decoder;
	
	public MessageParser() {
		this(DEFAULT_MAX_BUFFER_SIZE);
	}
	
	public MessageParser(int maxBufferSize) {
		this(maxBufferSize, null);
	}
	
	public MessageParser(IMessagePreprocessor binaryMessagePreprocessor) {
		this(DEFAULT_MAX_BUFFER_SIZE, binaryMessagePreprocessor);
	}
	
	public MessageParser(int maxBufferSize, IMessagePreprocessor binaryMessagePreprocessor) {
		if (binaryMessagePreprocessor != null) {
			preprocessor = binaryMessagePreprocessor;
			useXmlMessageFormat = false;
		} else {			
			preprocessor = new XmlMessagePreprocessorAdapter();
			charset = Charset.forName(Constants.DEFAULT_CHARSET);
			decoder = charset.newDecoder()
				.onMalformedInput(CodingErrorAction.REPLACE)
					.onUnmappableCharacter(CodingErrorAction.REPLACE);
			useXmlMessageFormat = true;
		}
		
		preprocessor.setMaxBufferSize(maxBufferSize);
	}
	
	@Override
	public synchronized String[] parse(IoBuffer in) throws Exception {
		if (useXmlMessageFormat) {
			CharBuffer charBuffer = CharBuffer.allocate(in.capacity());
			decoder.reset();
			CoderResult coderResult = decoder.decode(in.buf(), charBuffer, false);
			
			if (coderResult.isError()) {
				coderResult.throwException();
			}
			
			charBuffer.flip();
			ByteBuffer byteBuffer = charset.encode(charBuffer);
			byte[] bytes = new byte[byteBuffer.limit()];
			byteBuffer.get(bytes);
			if (logger.isTraceEnabled())
				logger.trace("Some bytes received: {}. Ready to preprocess.", getBytesString(bytes));
			
			return preprocessor.process(bytes);
		} else {
			int limit = in.buf().limit();
			byte[] buffer = new byte[limit];
			
			in.buf().get(buffer, 0, limit);
			
			return preprocessor.process(buffer);
		}
	}
	
	public String getBytesString(byte[] bytes) {
		StringBuilder sb = new StringBuilder();
		
		for (byte b : bytes) {
			sb.append(String.format("0x%02x ", b & 0xff));
		}
		
		if (sb.length() > 1) {
			sb.deleteCharAt(sb.length() - 1);
		}
		
		return sb.toString();
	}

	@Override
	public void setMaxBufferSize(int maxBufferSize) {
		preprocessor.setMaxBufferSize(maxBufferSize);
	}

	@Override
	public int getMaxBufferSize() {
		return preprocessor.getMaxBufferSize();
	}

}
