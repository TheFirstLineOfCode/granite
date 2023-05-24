package com.thefirstlineofcode.granite.stream.standard.codec;

import org.apache.mina.core.buffer.IoBuffer;

public interface IMessageParser {
	String[] parse(IoBuffer in) throws Exception;
	void setMaxBufferSize(int maxBufferSize);
	int getMaxBufferSize();
}
