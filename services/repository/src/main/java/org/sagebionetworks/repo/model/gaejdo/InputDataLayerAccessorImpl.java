package org.sagebionetworks.repo.model.gaejdo;

import javax.jdo.PersistenceManager;
import javax.jdo.Transaction;

import org.sagebionetworks.repo.model.InputDataLayer;
import org.sagebionetworks.repo.model.InputDataLayerAccessor;

import com.google.appengine.api.datastore.Key;

public class InputDataLayerAccessorImpl implements InputDataLayerAccessor {
	PersistenceManager pm;
	
	public InputDataLayerAccessorImpl(PersistenceManager pm) {
		this.pm=pm;
	}
	
	public InputDataLayer getDataLayer(Key id) {
		return (InputDataLayer)pm.getObjectById(InputDataLayer.class, id);		
	}
	
	public void makePersistent(InputDataLayer inputDataLayer) {
		Transaction tx=null;
		try {
			 	tx=pm.currentTransaction();
				tx.begin();
				pm.makePersistent(inputDataLayer);
				tx.commit();
		} finally {
				if(tx.isActive()) {
					tx.rollback();
				}
		}	
	}
	
	public void delete(InputDataLayer inputDataLayer)  {
		Transaction tx=null;
		try {
			 	tx=pm.currentTransaction();
				tx.begin();
				pm.deletePersistent(inputDataLayer);
				tx.commit();
		} finally {
				if(tx.isActive()) {
					tx.rollback();
				}
		}	
	}


}
