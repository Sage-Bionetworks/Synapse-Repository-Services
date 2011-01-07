package org.sagebionetworks.repo.model;

import com.google.appengine.api.datastore.Key;

public interface ScriptAccessor {
	public Script getScript(Key id);
	
	public void makePersistent(Script script);
	
	public void delete(Script script);

}
