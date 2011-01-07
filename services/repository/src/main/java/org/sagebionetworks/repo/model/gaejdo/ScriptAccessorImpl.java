packageorg.sagebionetworks.repo.model.gaejdo;

import javax.jdo.PersistenceManager;
import javax.jdo.Transaction;

import org.sage.datamodel.Script;
import org.sage.datamodel.ScriptAccessor;

import com.google.appengine.api.datastore.Key;

public class ScriptAccessorImpl implements ScriptAccessor {
	PersistenceManager pm;
	
	public ScriptAccessorImpl(PersistenceManager pm) {
		this.pm=pm;
	}
	
	public Script getScript(Key id) {
		return (Script)pm.getObjectById(Script.class, id);		
	}
	
	public void makePersistent(Script script) {
		
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
		}	
	}
	
	public void delete(Script script)  {
		Transaction tx=null;
		try {
			 	tx=pm.currentTransaction();
				tx.begin();
				pm.deletePersistent(script);
				tx.commit();
		} finally {
				if(tx.isActive()) {
					tx.rollback();
				}
		}	
	}


}
