package com.thefirstlineofcode.granite.framework.core.bxmpp;

import com.thefirstlineofcode.basalt.oxm.binary.IBinaryXmppProtocolConverter;
import com.thefirstlineofcode.basalt.oxm.preprocessing.IMessagePreprocessor;

public interface IBxmppService {
	IMessagePreprocessor getBinaryMessagePreprocessor();
	IBinaryXmppProtocolConverter getBxmppProtocolConverter();
}
