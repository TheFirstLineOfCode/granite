package com.thefirstlineofcode.granite.lite.pipeline;

public class Constants {
	private Constants() {}
	
	public static final String CONNECTOR_ID_LITE_STREAM_2_PARSING = "lite.stream.2.parsing.message.receiver";
	public static final String CONNECTOR_ID_LITE_PARSING_2_PROCESSING = "lite.parsing.2.processing.message.receiver";
	public static final String CONNECTOR_ID_LITE_ANY_2_ROUTING = "lite.any.2.routing.message.receiver";
	public static final String CONNECTOR_ID_LITE_ANY_2_EVENT = "lite.any.2.event.message.receiver";
	public static final String CONNECTOR_ID_LITE_ROUTING_2_STREAM = "lite.routing.2.stream.message.receiver";
}
