package org.sagebionetworks.repo.manager.oauth;

abstract public class CachingGetter<T> {
	private T value;
	abstract protected T getIntern();
	public T get() {
		if (value==null) {
			value=getIntern();
		}
		return value;
	}

}
