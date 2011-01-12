package org.sagebionetworks.repo.model.gaejdo;

import java.util.Collection;

import javax.jdo.PersistenceManager;
import javax.jdo.Query;
import javax.jdo.Transaction;

import org.sagebionetworks.repo.model.DatasetAccessor;

import com.google.appengine.api.datastore.Key;

public class DatasetAccessorImpl implements DatasetAccessor {
//	PersistenceManager pm;
//	
//	public DatasetAccessorImpl(PersistenceManager pm) {
//		this.pm=pm;
//	}
	
	public GAEJDODataset getDataset(Key id) {
		PersistenceManager pm = PMF.get();		
		GAEJDODataset ans = (GAEJDODataset)pm.getObjectById(GAEJDODataset.class, id);	
		//pm.close();
		return ans;
	}
	
	public void makePersistent(GAEJDODataset dataset) {
		PersistenceManager pm = PMF.get();		
		
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
				pm.close();
		}	
	}
	
//	public void delete(GAEJDODataset dataset)  {
//		PersistenceManager pm = PMF.get();		
//		Transaction tx=null;
//		try {
//			 	tx=pm.currentTransaction();
//				tx.begin();
////				dataset.getInputLayers().clear();
////				dataset.getAnalysisResults().clear();
//				pm.deletePersistent(dataset);
//				tx.commit();
//		} finally {
//				if(tx.isActive()) {
//					tx.rollback();
//				}
//				pm.close();
//		}	
//	}

	/**
	 * This enforces the 'owned relationship' between a dataset and its layers
	 * by deleting its member persistent objects.
	 * 
	 * @param dataset
	 */
	public void deleteDatasetAndContents(GAEJDODataset dataset)  {
		PersistenceManager pm = PMF.get();		
		Transaction tx=null;
		try {
			 	tx=pm.currentTransaction();
				tx.begin();
				for (Key layerKey : dataset.getLayers()) {
					// may have to check whether it's a InputDataLayer or AnalysisResult
					GAEJDODatasetLayer layer = (GAEJDODatasetLayer)pm.getObjectById(GAEJDODatasetLayer.class, layerKey);
					pm.deletePersistent(layer);
				}
				pm.deletePersistent(dataset);
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
				Query q = pm.newQuery(GAEJDODataset.class);
				q.setFilter("id==pId");
				q.declareParameters(Key.class.getName()+" pId");
				@SuppressWarnings("unchecked")
				Collection<GAEJDODataset> datasets = (Collection<GAEJDODataset>)q.execute(id);
				if (datasets.size()!=1) throw new IllegalStateException("Expected 1 but got "+datasets.size());
				pm.deletePersistent(datasets.iterator().next());
				tx.commit();
		} finally {
				if(tx.isActive()) {
					tx.rollback();
				}
				pm.close();
		}	
	}



	public void makeTransient(GAEJDODataset dataset) {
		PersistenceManager pm = PMF.get();		
		Transaction tx=null;
		try {
			 	tx=pm.currentTransaction();
				tx.begin();
				pm.makeTransient(dataset);
				tx.commit();
		} finally {
				if(tx.isActive()) {
					tx.rollback();
				}
				pm.close();
		}	
	}
	



}
