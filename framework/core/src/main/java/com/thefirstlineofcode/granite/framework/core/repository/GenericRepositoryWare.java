package com.thefirstlineofcode.granite.framework.core.repository;

public abstract class GenericRepositoryWare {
	protected String id;
	protected Class<?> type;
	
	public GenericRepositoryWare(String id, Class<?> type) {
		if (id == null) {
			throw new IllegalArgumentException("Null ID.");
		}
		
		this.id = id;
		this.type = type;
	}

	public String getId() {
		return id;
	}
	
	public Class<?> getType() {
		return type;
	}
	
	@Override
	public int hashCode() {
		return id.hashCode();
	}
	
	@Override
	public boolean equals(Object obj) {
		if (this.getClass() == obj.getClass()) {
			GenericRepositoryWare other = (GenericRepositoryWare)obj;
			
			return this.id.equals(other.id);
		}
		
		return false;
	}

}
