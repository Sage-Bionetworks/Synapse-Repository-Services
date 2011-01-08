package org.sagebionetworks.repo.model;

import com.google.appengine.api.datastore.Key;

public interface AnalysisResultAccessor {
	public AnalysisResult getAnalysisResult(Key id);
	
	public void makePersistent(AnalysisResult layer);
	
	public void delete(AnalysisResult layer);
}
