package com.thefirstlineofcode.granite.framework.core.pipeline.stages.stream;

import com.thefirstlineofcode.granite.framework.core.connection.IConnectionManagerAware;
import com.thefirstlineofcode.granite.framework.core.pipeline.IMessageReceiver;

public interface IDeliveryMessageReceiver extends IMessageReceiver, IConnectionManagerAware {}
