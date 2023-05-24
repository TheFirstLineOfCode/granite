package com.thefirstlineofcode.granite.framework.adf.mongodb;

import org.bson.Document;

public interface IDocToObj<T> {
	T toObj(Document doc);
}
