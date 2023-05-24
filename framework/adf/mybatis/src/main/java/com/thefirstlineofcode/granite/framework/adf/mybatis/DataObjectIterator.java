package com.thefirstlineofcode.granite.framework.adf.mybatis;

import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

public abstract class DataObjectIterator<T> implements Iterator<T>{
	
	private int position;
	private int page;
	
	private int maxFetchSize;
	
	private List<T> dataObjects;
	
	public DataObjectIterator() {
		this(20);
	}
	
	public DataObjectIterator(int maxFetchSize) {
		this.maxFetchSize = maxFetchSize;
		position = 0;
		page = 0;
	}

	@Override
	public boolean hasNext() {
		if (needFetchMore()) {
			fetch();
		}
		
		if (position == 0 && dataObjects.size() == 0)
			return false;
		
		return position < dataObjects.size();
	}

	private boolean needFetchMore() {
		if (dataObjects == null) {
			return true;
		}
		
		if (dataObjects.size() == maxFetchSize && position == maxFetchSize) {
			return true;
		}
		
		return false;
	}

	private void fetch() {
		dataObjects = doFetch(page * maxFetchSize, maxFetchSize);
		page++;
		position = 0;
	}
	
	protected abstract List<T> doFetch(int offset, int limit);

	@Override
	public T next() {
		if (!hasNext()) {
			throw new NoSuchElementException();
		}
		
		T dataObject = dataObjects.get(position);
		position++;
		
		return dataObject;
	}
	
	@Override
	public void remove() {
		throw new UnsupportedOperationException();
	}
}
