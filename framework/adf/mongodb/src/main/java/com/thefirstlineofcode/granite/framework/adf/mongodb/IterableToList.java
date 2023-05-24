package com.thefirstlineofcode.granite.framework.adf.mongodb;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.bson.Document;

import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCursor;

public class IterableToList<T> {
	public List<T> toList(FindIterable<Document> iterable, IDocToObj<T> converter) {
		MongoCursor<Document> cursor = iterable.iterator();
		if (!cursor.hasNext()) {
			return Collections.emptyList();
		}
		
		List<T> result = new ArrayList<>();
		while (cursor.hasNext()) {
			Document doc = cursor.next();
			
			result.add(converter.toObj(doc));
		}
		
		return result;
	}
}
