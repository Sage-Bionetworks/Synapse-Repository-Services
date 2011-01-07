package org.sagebionetworks.repo.model.gaejdo;

import javax.jdo.PersistenceManager;
import javax.jdo.Transaction;

import org.sagebionetworks.repo.model.Dataset;
import org.sagebionetworks.repo.model.DatasetAccessor;
import org.sagebionetworks.repo.model.Script;
import org.sagebionetworks.repo.model.ScriptAccessor;

import com.google.appengine.api.datastore.Key;

public class DatasetAccessorImpl implements DatasetAccessor {
	PersistenceManager pm;
	
	public DatasetAccessorImpl(PersistenceManager pm) {
		this.pm=pm;
	}
	
	public Dataset getDataset(Key id) {
		return (Dataset)pm.getObjectById(Dataset.class, id);		
	}
	
	public void makePersistent(Dataset dataset) {
		
		// the explicit transaction is important for the persistence of both objects in the
		// owned relationship.  see
		// http://code.google.com/appengine/docs/java/datastore/relationships.html#Relationships_Entity_Groups_and_Transactions
		Transaction tx=null;
		try {
			 	tx=pm.currentTransaction();
				tx.begin();
				pm.makePersistent(dataset);
				tx.commit();
		} finally {
				if(tx.isActive()) {
					tx.rollback();
				}
		}	
	}
	
	public void delete(Dataset dataset)  {
		Transaction tx=null;
		try {
			 	tx=pm.currentTransaction();
				tx.begin();
				dataset.getInputLayers().clear();
				dataset.getAnalysisResults().clear();
				pm.deletePersistent(dataset);
				tx.commit();
		} finally {
				if(tx.isActive()) {
					tx.rollback();
				}
		}	
	}


}
