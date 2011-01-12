package org.sagebionetworks.repo.model.gaejdo;

public interface GAEJDOAnnotation<T> {
	void setAttribute(String a);
	String getAttribute();
	void setValue(T value);
	T getValue();

}
