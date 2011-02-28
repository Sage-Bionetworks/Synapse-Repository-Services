package org.sagebionetworks.repo.model;

import java.util.Collection;
import java.util.HashSet;
import java.util.logging.Level;
import java.util.logging.Logger;

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

public class AnnotationsDAOTest {
	private final LocalServiceTestHelper helper = new LocalServiceTestHelper(
			new LocalDatastoreServiceTestConfig());

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
		helper.setUp();
		fac = new GAEJDODAOFactoryImpl();
	}

	@After
	public void tearDown() throws Exception {
		if (fac != null && id != null) {
			if (false)
				fac.getDatasetDAO(null).delete(id);
			// fac.close();
			id = null;
		}
		helper.tearDown();
	}

	@Test
	@Ignore
	public void testCreateandRetrieve() throws Exception {
		// create a new dataset
		GAEJDODataset dataset = new GAEJDODataset();

		GAEJDORevision<GAEJDODataset> r = new GAEJDORevision<GAEJDODataset>();
		r.setVersion(new Version("1.0.0"));
		dataset.setRevision(r);

		GAEJDOAnnotations annots = GAEJDOAnnotations.newGAEJDOAnnotations();
		dataset.setAnnotations(annots);
		GAEJDOStringAnnotation stringAnnot = new GAEJDOStringAnnotation(
				"testKey", "testValue");
		// annots.getStringAnnotations().add(stringAnnot);

		// persist it
		DatasetDAO da = fac.getDatasetDAO(null);
		// da.makePersistent(dataset);

		// persisting creates a Key, which we can grab
		Key id = dataset.getId();
		Assert.assertNotNull(id);
		// this.id=id;

		// now retrieve the object by its key
		// Dataset d2 = da.getDataset(id);
		// Assert.assertNotNull(d2);

		// AnnotatableDAO<GAEJDODataset> aa =
		// fac.getDatasetAnnotationsAccessor();
		//
		// // now, test that we can retrieve the object having the given
		// annotation
		// Collection<GAEJDODataset> ac =
		// aa.getHavingStringAnnotation("testKey", "testValue");
		// Assert.assertEquals(1, ac.size());
		// GAEJDODataset d2 = ac.iterator().next();
		// Assert.assertEquals(dataset.getId(), d2.getId());
		// aa.close();
	}
}
