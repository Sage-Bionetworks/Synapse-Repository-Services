package org.sagebionetworks.repo.model;


import java.net.URL;
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;

import junit.framework.Assert;

import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.sage.datamodel.gaejdo.AccessorFactoryImpl;

import com.google.appengine.api.datastore.Key;
import com.google.appengine.api.datastore.Text;
import com.google.appengine.tools.development.testing.LocalDatastoreServiceTestConfig;
import com.google.appengine.tools.development.testing.LocalServiceTestHelper;

public class ProjectTest {
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


    private AccessorFactory fac;
	private Project project;

	@Before
	public void setUp() throws Exception {
        helper.setUp(); 
		fac = new AccessorFactoryImpl();
	}

	@After
	public void tearDown() throws Exception {
		if (fac!=null && project!=null) {
			fac.getProjectAccessor().delete(project);
			fac.close();
			project = null;
		}
		helper.tearDown();
	}
	
	
	
	@Test
	public void testCreateandRetrieve() throws Exception {
		// create a new project
		project = new Project();
		
		project.setName("project name");
		project.setStatus(Project.Status.IN_PROGRESS);
		String overview = "This project is a megacross, and includes genotyoping data.";
		project.setOverview(new Text(overview));
		Date started = new Date();
		project.setStarted(started);
		URL url = new URL("https://docs.google.com");
		project.setSharedDocs(url);
		
		// persist it
		ProjectAccessor pa = fac.getProjectAccessor();
		pa.makePersistent(project);
		
		// persisting creates a Key, which we can grab
		Key id = project.getId();
		Assert.assertNotNull(id);
		
		// now retrieve the object by its key
		Project p2 = pa.getProject(id);
		Assert.assertNotNull(p2);
		
		// check that all the fields were persisted
		Assert.assertEquals("project name", p2.getName());
		Assert.assertEquals(Project.Status.IN_PROGRESS, p2.getStatus());
		Assert.assertEquals(overview, p2.getOverview().getValue());
		Assert.assertEquals(started, p2.getStarted());
		Assert.assertEquals(url, p2.getSharedDocs());
	}

}
