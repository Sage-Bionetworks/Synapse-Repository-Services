package org.sagebionetworks.repo.model;

import com.google.appengine.api.datastore.Key;

public interface InputDataLayerAccessor {
	public InputDataLayer getDataLayer(Key id);
	
	public void makePersistent(InputDataLayer layer);
	
	public void delete(InputDataLayer layer);
}
