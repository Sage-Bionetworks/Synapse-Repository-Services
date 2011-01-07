package org.sagebionetworks.repo.model.gaejdo;

import java.util.Collection;

import javax.jdo.PersistenceManager;
import javax.jdo.Query;

import org.sagebionetworks.repo.model.Revisable;
import org.sagebionetworks.repo.model.Revision;
import org.sagebionetworks.repo.model.RevisionAccessor;

import com.google.appengine.api.datastore.Key;

public class RevisionAccessorImpl<T extends Revisable<T>> implements RevisionAccessor<T> {
	private PersistenceManager pm;
	
	public RevisionAccessorImpl(PersistenceManager pm) {
		this.pm=pm;
	}
	
	@SuppressWarnings("unchecked")
	public Revision<T> getRevision(Key id) {
		return (Revision<T>)pm.getObjectById(Revision.class, id);		
	}
	
	public Revision<T> getLatest(Revision<T> original) {
		// get all Revisions having the given original
		Query query = pm.newQuery(Revision.class);
		query.setFilter("this.original==originalKeyp");
		query.declareParameters(Key.class.getName()+" originalKeyp");
		Collection<Revision<T>> revisions = (Collection<Revision<T>>)query.execute(original.getId());
		// now find the latest and return it
		Revision<T> ans = original;
		for (Revision<T> rev : revisions) {
			if (rev.getVersion().compareTo(ans.getVersion())>0) ans=rev;
		}
		return ans;
	}
	
//	public void makePersistent(Revision revision) {
//		pm.makePersistent(revision);
//	}
//	
//	public void delete(Revision revision) {
//		pm.deletePersistent(revision);
//	}
	

}
