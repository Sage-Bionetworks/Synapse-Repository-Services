package org.sagebionetworks.repo.model.gaejdo;

import java.util.Collection;

import javax.jdo.PersistenceManager;
import javax.jdo.Query;
import javax.jdo.Transaction;

import org.sagebionetworks.repo.model.InputDataLayerDAO;

import com.google.appengine.api.datastore.Key;

public class GAEJDOInputDataLayerDAOImpl implements InputDataLayerDAO {
//	PersistenceManager pm;
//	
//	public GAEJDOInputDataLayerDAOImpl(PersistenceManager pm) {
//		this.pm=pm;
//	}
	
	public GAEJDOInputDataLayer getDataLayer(Key id) {
		PersistenceManager pm = PMF.get();		
		GAEJDOInputDataLayer ans = (GAEJDOInputDataLayer)pm.getObjectById(GAEJDOInputDataLayer.class, id);	
		//pm.close();
		return ans;
	}
	
	public void makePersistent(GAEJDOInputDataLayer inputDataLayer) {
		PersistenceManager pm = PMF.get();		
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
				pm.close();
		}	
	}
	
//	public void delete(InputDataLayer inputDataLayer)  {
//		PersistenceManager pm = PMF.get();		
//		Transaction tx=null;
//		try {
//			 	tx=pm.currentTransaction();
//				tx.begin();
//				pm.deletePersistent(inputDataLayer);
//				tx.commit();
//		} finally {
//				if(tx.isActive()) {
//					tx.rollback();
//				}
//				pm.close();
//		}	
//	}

	public void makeTransient(GAEJDOInputDataLayer inputDataLayer) {
		PersistenceManager pm = PMF.get();		
		Transaction tx=null;
		try {
			 	tx=pm.currentTransaction();
				tx.begin();
				pm.makeTransient(inputDataLayer);
				tx.commit();
		} finally {
				if(tx.isActive()) {
					tx.rollback();
				}
				pm.close();
		}	
	}
	
	public void delete(Key id) {
		PersistenceManager pm = PMF.get();		
		Transaction tx=null;
		try {
			 	tx=pm.currentTransaction();
				tx.begin();
				Query q = pm.newQuery(GAEJDOInputDataLayer.class);
				q.setFilter("id==pId");
				q.declareParameters(Key.class.getName()+" pId");
				@SuppressWarnings("unchecked")
				Collection<GAEJDOInputDataLayer> layers = (Collection<GAEJDOInputDataLayer>)q.execute(id);
				if (layers.size()!=1) throw new IllegalStateException("Expected 1 but got "+layers.size());
				pm.deletePersistent(layers.iterator().next());
				tx.commit();
		} finally {
				if(tx.isActive()) {
					tx.rollback();
				}
				pm.close();
		}	
	}



}
