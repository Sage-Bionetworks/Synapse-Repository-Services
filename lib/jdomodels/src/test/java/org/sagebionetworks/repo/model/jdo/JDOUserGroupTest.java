package org.sagebionetworks.repo.model.jdo;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.jdo.PersistenceManager;
import javax.jdo.Query;

import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.sagebionetworks.repo.model.AuthorizationConstants;

public class JDOUserGroupTest {
	
	private static final boolean VERBOSE = false;
	
	@BeforeClass
	public static void beforeClass() throws Exception {
		Logger.getLogger("DataNucleus.Plugin").setLevel(Level.FINEST);
		Logger.getLogger("DataNucleus.Persistence").setLevel(Level.FINEST);
		Logger.getLogger("DataNucleus.JDO").setLevel(Level.FINEST);
		Logger.getLogger("DataNucleus.Query").setLevel(Level.FINEST);
		Logger.getLogger("DataNucleus.Datastore.Retrieve").setLevel(Level.FINEST);
		Logger.getLogger("DataNucleus.SchemaTool").setLevel(Level.FINEST);
		(new JDOBootstrapperImpl()).bootstrap(); // creat admin user, public group, etc.
	}

	
	private long  makeGroup(String name, Long resourceId) {
		JDOUserGroup g = new JDOUserGroup();
		g.setUsers(new HashSet<Long>());
		g.getUsers().add(0L);
		g.setName(name);
		Set<JDOResourceAccess> ras = new HashSet<JDOResourceAccess>();
		if (resourceId!=null) {
			JDOResourceAccess ra = new JDOResourceAccess();
			ra.setResourceType("foo");
			ra.setResourceId(resourceId); // fake pointer.  we won't actually 'follow' it
			ra.setAccessType(AuthorizationConstants.READ_ACCESS);
			ras.add(ra);
		}
		g.setResourceAccess(ras);
		PersistenceManager pm = PMF.get();
		try {
			pm.makePersistent(g);
		} finally {
			pm.close();
		}
		pm = null;
		return g.getId();
	}

	@Test
	public void testGroup() throws Exception {
		makeGroup("group1", 111L);
		makeGroup("group2", 222L);
		makeGroup("group3", null);
		// now retrieve
		//org.datanucleus.jdo.JDOPersistenceManagerFactory PMF;

		PersistenceManager pm = PMF.get();
//		g = pm.getObjectById(JDOUserGroup.class, id);
//		assertNotNull(g);
//		ra = g.getResourceAccess().iterator().next();
//		assertEquals(g, ra.getOwner());
		Query query = pm.newQuery(JDOUserGroup.class);
//		Map<String,String> props = new HashMap<String,String>();
//		JDOPersistenceManagerFactory PMF = new JDOPersistenceManagerFactory(props);



//		ObjectManagerImpl om = new ObjectManagerImpl(
//				omf, "owner", "userName", "password");
//		Query query = om.newQuery();
		query.setFilter("this.id==vra.owner.id && this.users.contains(0L)");
//		query.setFilter("this.id==vra.owner.id && vra.resource==pResource");
//		query.declareParameters(Long.class.getName()+" pResource");
		query.declareVariables(JDOResourceAccess.class.getName()+" vra");
		query.setResult("this, vra.resourceId");
//		List<Object[]> list = (List<Object[]>)query.execute(111L);
		query.addExtension("datanucleus.query.jdoql.vra.join", "LEFTOUTERJOIN");
		query.addExtension("datanucleus.rdbms.jdoql.joinType", "LEFT OUTER");
		if (VERBOSE) o("JDOUserGroupTest.testGroup: before query execution.");
		@SuppressWarnings("unchecked")
		List<Object[]> list = (List<Object[]>)query.execute();
		
		// http://www.datanucleus.org/products/accessplatform/jdo/sql.html
		// Query query = pm.newQuery("javax.jdo.query.SQL", "SELECT MY_ID, MY_NAME FROM MYTABLE");
		// query.setClass(MyClass.class);
		// List<MyClass> results = (List<MyClass>) query.execute();
		
		if (VERBOSE) displayResultSet(list);
		
//		Query query2 = pm.newQuery("javax.jdo.query.JDOQL", "SELECT this, vra.resource FROM "+JDOUserGroup.class.getName()+" WHERE this.id==vra.owner.id VARIABLES "+JDOResourceAccess.class.getName()+" vra");
//		displayResultSet((List<Object[]>)query2.execute());

		Query query3 = pm.newQuery("javax.jdo.query.JPQL", "SELECT g.name,ra.resourceId FROM "+JDOUserGroup.class.getName()+" g LEFT OUTER JOIN xx, "+JDOResourceAccess.class.getName()+" ra where g=ra.owner");
		@SuppressWarnings("unchecked")
		List<Object[]> list3 = (List<Object[]>)query3.execute();
		if (VERBOSE) displayResultSet(list3);
	}
	
	private void displayResultSet(List<Object[]> list) {
		o("Result set size: "+list.size());
		for (Object[] row : list) {
			o("#fields="+row.length+": "+row[0]+" "+row[1]);
		}		
	}
	
	private static void o(Object s) {System.out.println(s);}

}
