package com.thefirstlineofcode.granite.im;

import com.thefirstlineofcode.granite.framework.im.SubscriptionType;
import com.thefirstlineofcode.granite.framework.im.Subscription.State;

public final class SubscriptionStateChangeRules {
	private SubscriptionStateChangeRules() {}
	
	// (rfc3921 9.2)
	public static State getOutboundSubscriptionNewState(State oldState, SubscriptionType subscriptionType) {
		State newState;
		
		if (subscriptionType == SubscriptionType.SUBSCRIBE) {
			if (oldState == State.NONE) {
				newState = State.NONE_PENDING_OUT;
			} else if (oldState == State.NONE_PENDING_IN) {
				newState = State.NONE_PENDING_IN_OUT;
			} else if (oldState == State.FROM) {
				newState = State.FROM_PENDING_OUT;
			} else {
				newState = oldState;
			}
		} else if (subscriptionType == SubscriptionType.UNSUBSCRIBE) {
			if (oldState == State.NONE_PENDING_OUT) {
				newState = State.NONE;
			} else if (oldState == State.NONE_PENDING_IN_OUT) {
				newState = State.NONE_PENDING_IN;
			} else if (oldState == State.TO) {
				newState = State.NONE;
			} else if (oldState == State.TO_PENDING_IN) {
				newState = State.NONE_PENDING_IN;
			} else if (oldState == State.FROM_PENDING_OUT) {
				newState = State.FROM;
			} else if (oldState == State.BOTH) {
				newState = State.FROM;
			} else {
				newState = oldState;
			}
		} else if (subscriptionType == SubscriptionType.SUBSCRIBED) {
			if (oldState == State.NONE_PENDING_IN) {
				newState = State.FROM;
			} else if (oldState == State.NONE_PENDING_IN_OUT) {
				newState = State.FROM_PENDING_OUT;
			} else if (oldState == State.TO_PENDING_IN) {
				newState = State.BOTH;
			} else {
				newState = oldState;
			}
		} else { // subscriptionType == SubscriptionType.UNSUBSCRIBED
			if (oldState == State.NONE_PENDING_IN) {
				newState = State.NONE;
			} else if (oldState == State.NONE_PENDING_IN_OUT) {
				newState = State.NONE_PENDING_OUT;
			} else if (oldState == State.TO_PENDING_IN) {
				newState = State.TO;
			} else if (oldState == State.FROM) {
				newState = State.NONE;
			} else if (oldState == State.FROM_PENDING_OUT) {
				newState = State.NONE_PENDING_OUT;
			} else if (oldState == State.BOTH) {
				newState = State.TO;
			} else {
				newState = oldState;
			}
		}
		
		return newState;
	}
	
	// (rfc3921 9.3)
	public static State getInboundSubscriptionNewState(State oldState, SubscriptionType subscriptionType) {
		State newState;
		
		if (subscriptionType == SubscriptionType.SUBSCRIBE) {
			if (oldState == State.NONE) {
				newState = State.NONE_PENDING_IN;
			} else if (oldState == State.NONE_PENDING_OUT) {
				newState = State.NONE_PENDING_IN_OUT;
			} else if (oldState == State.TO) {
				newState = State.TO_PENDING_IN;
			} else {
				newState = oldState;
			}
		} else if (subscriptionType == SubscriptionType.UNSUBSCRIBE) {
			if (oldState == State.NONE_PENDING_IN) {
				newState = State.NONE;
			} else if (oldState == State.NONE_PENDING_IN_OUT) {
				newState = State.NONE_PENDING_OUT;
			} else if (oldState == State.TO_PENDING_IN) {
				newState = State.TO;
			} else if (oldState == State.FROM) {
				newState = State.NONE;
			} else if (oldState == State.FROM_PENDING_OUT) {
				newState = State.NONE_PENDING_OUT;
			} else if (oldState == State.BOTH) {
				newState = State.TO;
			} else {
				newState = oldState;
			}
		} else if (subscriptionType == SubscriptionType.SUBSCRIBED) {
			if (oldState == State.NONE_PENDING_OUT) {
				newState = State.TO;
			} else if (oldState == State.NONE_PENDING_IN_OUT) {
				newState = State.TO_PENDING_IN;
			} else if (oldState == State.FROM_PENDING_OUT) {
				newState = State.BOTH;
			} else {
				newState = oldState;
			}
		} else { // subscriptionType == SubscriptionType.UNSUBSCRIBED
			if (oldState == State.NONE_PENDING_OUT) {
				newState = State.NONE;
			} else if (oldState == State.NONE_PENDING_IN_OUT) {
				newState = State.NONE_PENDING_IN;
			} else if (oldState == State.TO) {
				newState = State.NONE;
			} else if (oldState == State.TO_PENDING_IN) {
				newState = State.NONE_PENDING_IN;
			} else if (oldState == State.FROM_PENDING_OUT) {
				newState = State.FROM;
			} else if (oldState == State.BOTH) {
				newState = State.FROM;
			} else {
				newState = oldState;
			}
		}
		
		return newState;
	}
}
