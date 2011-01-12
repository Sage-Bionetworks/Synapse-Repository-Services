package org.sagebionetworks.repo.model.gaejdo;

import java.util.Collection;

import javax.jdo.PersistenceManager;
import javax.jdo.Query;
import javax.jdo.Transaction;

import org.sagebionetworks.repo.model.ScriptAccessor;

import com.google.appengine.api.datastore.Key;

public class ScriptAccessorImpl implements ScriptAccessor {
//	PersistenceManager pm;
//	
//	public ScriptAccessorImpl(PersistenceManager pm) {
//		this.pm=pm;
//	}
	
	public GAEJDOScript getScript(Key id) {
		PersistenceManager pm = PMF.get();		
		GAEJDOScript ans = (GAEJDOScript)pm.getObjectById(GAEJDOScript.class, id);
		//pm.close();
		return ans;
	}
	
	public void makePersistent(GAEJDOScript script) {
		PersistenceManager pm = PMF.get();		
		
		// the explicit transaction is important for the persistence of both objects in the
		// owned relationship.  see
		// http://code.google.com/appengine/docs/java/datastore/relationships.html#Relationships_Entity_Groups_and_Transactions
		Transaction tx=null;
		try {
			 	tx=pm.currentTransaction();
				tx.begin();
				pm.makePersistent(script);
				tx.commit();
		} finally {
				if(tx.isActive()) {
					tx.rollback();
				}
				pm.close();
		}	
	}
	
//	public void delete(Script script)  {
//		PersistenceManager pm = PMF.get();		
//		Transaction tx=null;
//		try {
//			 	tx=pm.currentTransaction();
//				tx.begin();
//				pm.deletePersistent(script);
//				tx.commit();
//		} finally {
//				if(tx.isActive()) {
//					tx.rollback();
//				}
//				pm.close();
//		}	
//	}
	
	public void delete(Key id) {
		PersistenceManager pm = PMF.get();		
		Transaction tx=null;
		try {
			 	tx=pm.currentTransaction();
				tx.begin();
				Query q = pm.newQuery(GAEJDOScript.class);
				q.setFilter("id==pId");
				q.declareParameters(Key.class.getName()+" pId");
				long n = q.deletePersistentAll(id);
				if (n!=1) throw new IllegalStateException("Expected 1 but got "+n);
				tx.commit();
		} finally {
				if(tx.isActive()) {
					tx.rollback();
				}
				pm.close();
		}	
	}

	
	public void makeTransient(GAEJDOScript script) {
		PersistenceManager pm = PMF.get();		
		Transaction tx=null;
		try {
			 	tx=pm.currentTransaction();
				tx.begin();
				pm.makeTransient(script);
				tx.commit();
		} finally {
				if(tx.isActive()) {
					tx.rollback();
				}
				pm.close();
		}	
	}
	
	// Note:  This used to be a query in RevisionAccessorImpl, but I got the error
	// "Received a request to find an object of kind Dataset but the provided identifier is a Key for kind Script"
	// from data-nucleus which suggests it's too much to ask that an owned object point back to
	// an owner which may be of more than one class.  Eliminating the backwards pointer means
	// queries on Revisions need to be made from the perspective of the owner.
	public GAEJDOScript getLatest(GAEJDOScript original) {
		PersistenceManager pm = PMF.get();		
		// get all Revisions having the given original
		Query query = pm.newQuery(GAEJDOScript.class);
		query.setFilter("revision==r && r.original==pFirstRevision");
		query.declareVariables(GAEJDORevision.class.getName()+" r");
		query.declareParameters(Key.class.getName()+" pFirstRevision");
		Collection<GAEJDOScript> scripts = (Collection<GAEJDOScript>)query.execute(original.getRevision().getId());
		//pm.close();
		// now find the latest and return it
		GAEJDOScript ans = original;
		for (GAEJDOScript s : scripts) {
			if (s.getRevision().getVersion().compareTo(ans.getRevision().getVersion())>0) ans=s;
		}
		return ans;
	}


}
