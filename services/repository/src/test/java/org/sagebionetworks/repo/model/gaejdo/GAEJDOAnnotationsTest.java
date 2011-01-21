package org.sagebionetworks.repo.model.gaejdo;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.jdo.PersistenceManager;
import javax.jdo.Query;

import junit.framework.Assert;

import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.sagebionetworks.repo.model.gaejdo.GAEJDODAOFactoryImpl;
import org.sagebionetworks.repo.model.gaejdo.GAEJDOAnnotations;
import org.sagebionetworks.repo.model.gaejdo.GAEJDODataset;
import org.sagebionetworks.repo.model.gaejdo.GAEJDORevision;
import org.sagebionetworks.repo.model.gaejdo.GAEJDOStringAnnotation;
import org.sagebionetworks.repo.model.gaejdo.Version;

import com.google.appengine.api.datastore.Key;
import com.google.appengine.tools.development.testing.LocalDatastoreServiceTestConfig;
import com.google.appengine.tools.development.testing.LocalServiceTestHelper;

public class GAEJDOAnnotationsTest {
	   private final LocalServiceTestHelper helper = 
	        new LocalServiceTestHelper(new LocalDatastoreServiceTestConfig()); 
	    
	    @BeforeClass
	    public static void beforeClass() throws Exception {
	    	// from http://groups.google.com/group/google-appengine-java/browse_thread/thread/96baed75e3c30a58/00d5afb2e0445882?lnk=gst&q=DataNucleus+plugin#00d5afb2e0445882  
	    	//This one caused all the WARNING and SEVERE logs about eclipse UI  elements 
	        Logger.getLogger("DataNucleus.Plugin").setLevel(Level.OFF); 
	        //This one logged the last couple INFOs about Persistence configuration 
	        Logger.getLogger("DataNucleus.Persistence").setLevel(Level.WARNING); 
	    }

		@Before
		public void setUp() throws Exception {
	        helper.setUp(); 
		}

		@After
		public void tearDown() throws Exception {
			helper.tearDown();
		}
		
		
		
		@Test
		public void testAnnotQuery() throws Exception {
			PersistenceManager pm = PMF.get();	
			try {
				GAEJDOAnnotations a = new GAEJDOAnnotations();
				Set<GAEJDOFloatAnnotation> as = a.getFloatAnnotations();
				as.add(new GAEJDOFloatAnnotation("weight", 120.5F));
				pm.makePersistent(a);
			} finally {
				pm.close();
			}			
			pm = PMF.get();
			try {
				Query query = pm.newQuery(GAEJDOAnnotations.class);
				query.setFilter("this.floatAnnotations.contains(vAnnotation) && "+
					"vAnnotation.attribute==pAttrib && vAnnotation.value==pValue");
				query.declareVariables(GAEJDOFloatAnnotation.class.getName()+" vAnnotation");
				query.declareParameters(String.class.getName()+" pAttrib, "+Float.class.getName()+" pValue");
				@SuppressWarnings("unchecked")
				List<GAEJDOAnnotations> annots = (List<GAEJDOAnnotations>)query.execute("weight", 120.5F);	
				System.out.println(annots.iterator().next().getFloatAnnotations());
			} finally {
				pm.close();
			}			
		}
}
