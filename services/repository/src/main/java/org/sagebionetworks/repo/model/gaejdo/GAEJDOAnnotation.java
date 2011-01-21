package org.sagebionetworks.repo.model.gaejdo;

import com.google.appengine.api.datastore.Key;

public interface GAEJDOAnnotation<T> {
	Key getId();
	void setAttribute(String a);
	String getAttribute();
	void setValue(T value);
	T getValue();

}
