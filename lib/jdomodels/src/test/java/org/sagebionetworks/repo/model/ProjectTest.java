package org.sagebionetworks.repo.model;

import java.net.URL;
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;

import junit.framework.Assert;

import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.sagebionetworks.repo.model.jdo.GAEJDODAOFactoryImpl;
import org.sagebionetworks.repo.model.jdo.GAEJDOProject;






public class ProjectTest {

	@BeforeClass
	public static void beforeClass() throws Exception {
		// from
		// http://groups.google.com/group/google-appengine-java/browse_thread/thread/96baed75e3c30a58/00d5afb2e0445882?lnk=gst&q=DataNucleus+plugin#00d5afb2e0445882
		// This one caused all the WARNING and SEVERE logs about eclipse UI
		// elements
		Logger.getLogger("DataNucleus.Plugin").setLevel(Level.OFF);
		// This one logged the last couple INFOs about Persistence configuration
		Logger.getLogger("DataNucleus.Persistence").setLevel(Level.WARNING);
	}

	private DAOFactory fac;
	private String id;

	@Before
	public void setUp() throws Exception {
		fac = new GAEJDODAOFactoryImpl();
	}

	@After
	public void tearDown() throws Exception {
		if (fac != null && id != null) {
			fac.getProjectDAO().delete(id);
			// fac.close();
			id = null;
		}
	}

	@Test
	@Ignore
	public void testCreateandRetrieve() throws Exception {
		// create a new project
		GAEJDOProject project = new GAEJDOProject();

		project.setName("project name");
		project.setStatus(GAEJDOProject.Status.IN_PROGRESS);
		String overview = "This project is a megacross, and includes genotyoping data.";
		project.setOverview(new String(overview));
		Date started = new Date();
		project.setStarted(started);
		URL url = new URL("https://docs.google.com");
		project.setSharedDocs(url);

		// persist it
		ProjectDAO pa = fac.getProjectDAO();
		// pa.makePersistent(project);

		// persisting creates a Long, which we can grab
		Long id = project.getId();
		// this.id=id;
		Assert.assertNotNull(id);

		// now retrieve the object by its key
		GAEJDOProject p2 = null; // pa.getProject(id);
		Assert.assertNotNull(p2);

		// check that all the fields were persisted
		Assert.assertEquals("project name", p2.getName());
		Assert.assertEquals(GAEJDOProject.Status.IN_PROGRESS, p2.getStatus());
		Assert.assertEquals(overview, p2.getOverview());
		Assert.assertEquals(started, p2.getStarted());
		Assert.assertEquals(url, p2.getSharedDocs());
	}

}
