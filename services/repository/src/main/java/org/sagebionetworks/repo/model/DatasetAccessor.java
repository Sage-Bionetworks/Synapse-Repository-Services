package org.sagebionetworks.repo.model;

import com.google.appengine.api.datastore.Key;

public interface DatasetAccessor {
	public Dataset getDataset(Key id);
	
	public void makePersistent(Dataset dataset);
	
	public void delete(Dataset dataset);

	public void deleteDatasetAndContents(Dataset dataset);
}
