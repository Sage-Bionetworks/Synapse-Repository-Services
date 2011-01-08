package org.sagebionetworks.repo.model.gaejdo;

import javax.jdo.PersistenceManager;
import javax.jdo.Transaction;

import org.sagebionetworks.repo.model.AnalysisResult;
import org.sagebionetworks.repo.model.AnalysisResultAccessor;
import org.sagebionetworks.repo.model.InputDataLayer;
import org.sagebionetworks.repo.model.InputDataLayerAccessor;

import com.google.appengine.api.datastore.Key;

public class AnalysisResultAccessorImpl implements AnalysisResultAccessor {
	PersistenceManager pm;
	
	public AnalysisResultAccessorImpl(PersistenceManager pm) {
		this.pm=pm;
	}
	
	public AnalysisResult getAnalysisResult(Key id) {
		return (AnalysisResult)pm.getObjectById(AnalysisResultAccessor.class, id);		
	}
	
	public void makePersistent(AnalysisResult analysisResult) {
		Transaction tx=null;
		try {
			 	tx=pm.currentTransaction();
				tx.begin();
				pm.makePersistent(analysisResult);
				tx.commit();
		} finally {
				if(tx.isActive()) {
					tx.rollback();
				}
		}	
	}
	
	public void delete(AnalysisResult analysisResult)  {
		Transaction tx=null;
		try {
			 	tx=pm.currentTransaction();
				tx.begin();
				pm.deletePersistent(analysisResult);
				tx.commit();
		} finally {
				if(tx.isActive()) {
					tx.rollback();
				}
		}	
	}


}
