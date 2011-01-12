package org.sagebionetworks.repo.model.gaejdo;

import java.util.Collection;

import javax.jdo.PersistenceManager;
import javax.jdo.Query;

import org.sagebionetworks.repo.model.RevisionAccessor;

import com.google.appengine.api.datastore.Key;

public class RevisionAccessorImpl<T extends Revisable<T>> implements RevisionAccessor<T> {
	
	@SuppressWarnings("unchecked")
	public GAEJDORevision<T> getRevision(Key id) {
		PersistenceManager pm = PMF.get();		
		GAEJDORevision<T> ans = (GAEJDORevision<T>)pm.getObjectById(GAEJDORevision.class, id);	
		//pm.close();
		return ans;
	}
	
//	public Revision<T> getLatest(Revision<T> original) {
//		PersistenceManager pm = PMF.get();		
//		// get all Revisions having the given original
//		Query query = pm.newQuery(Revision.class);
//		query.setFilter("this.original==originalKeyp");
//		query.declareParameters(Key.class.getName()+" originalKeyp");
//		Collection<Revision<T>> revisions = (Collection<Revision<T>>)query.execute(original.getId());
//		//pm.close();
//		// now find the latest and return it
//		Revision<T> ans = original;
//		for (Revision<T> rev : revisions) {
//			if (rev.getVersion().compareTo(ans.getVersion())>0) ans=rev;
//		}
//		return ans;
//	}
	
	

}
