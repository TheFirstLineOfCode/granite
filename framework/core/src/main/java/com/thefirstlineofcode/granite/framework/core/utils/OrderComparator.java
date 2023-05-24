package com.thefirstlineofcode.granite.framework.core.utils;

import java.util.Comparator;

public class OrderComparator<T> implements Comparator<T> {

	@Override
	public int compare(T o1, T o2) {
		return getAcceptableOrder(o1) - getAcceptableOrder(o2);
	}
	
	public static <T> int getAcceptableOrder(T t) {
		if (t instanceof IOrder) {
			int order = ((IOrder)t).getOrder();
			
			if (order >= IOrder.ORDER_MIN && order <= IOrder.ORDER_MAX) {
				return order;
			} else if (order < IOrder.ORDER_MIN) {
				return IOrder.ORDER_MIN;
			} else {
				return IOrder.ORDER_MAX;
			}
		}
		
		return IOrder.ORDER_NORMAL;
	}

}
