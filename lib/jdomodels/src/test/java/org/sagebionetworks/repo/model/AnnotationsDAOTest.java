package org.sagebionetworks.repo.model;

import java.util.logging.Level;
import java.util.logging.Logger;

import junit.framework.Assert;

import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.sagebionetworks.repo.model.jdo.JDOAnnotations;
import org.sagebionetworks.repo.model.jdo.JDODAOFactoryImpl;
import org.sagebionetworks.repo.model.jdo.JDODataset;
import org.sagebionetworks.repo.model.jdo.JDORevision;
import org.sagebionetworks.repo.model.jdo.Version;





public class AnnotationsDAOTest {

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
		fac = new JDODAOFactoryImpl();
	}

	@After
	public void tearDown() throws Exception {
		if (fac != null && id != null) {
//			if (false)
//				fac.getDatasetDAO(null).delete(id);
			id = null;
		}
	}

	@Test
	@Ignore
	public void testCreateandRetrieve() throws Exception {
		// create a new dataset
		JDODataset dataset = new JDODataset();

		JDORevision<JDODataset> r = new JDORevision<JDODataset>();
		r.setVersion(new Version("1.0.0"));
		dataset.setRevision(r);

		JDOAnnotations annots = JDOAnnotations.newJDOAnnotations();
		dataset.setAnnotations(annots);
//		JDOStringAnnotation stringAnnot = new JDOStringAnnotation(
//				"testLong", "testValue");
		// annots.getStringAnnotations().add(stringAnnot);

		// persist it
//		DatasetDAO da = fac.getDatasetDAO(null);
		// da.makePersistent(dataset);

		// persisting creates a Long, which we can grab
		Long id = dataset.getId();
		Assert.assertNotNull(id);
		// this.id=id;

		// now retrieve the object by its key
		// Dataset d2 = da.getDataset(id);
		// Assert.assertNotNull(d2);

		// AnnotatableDAO<JDODataset> aa =
		// fac.getDatasetAnnotationsAccessor();
		//
		// // now, test that we can retrieve the object having the given
		// annotation
		// Collection<JDODataset> ac =
		// aa.getHavingStringAnnotation("testLong", "testValue");
		// Assert.assertEquals(1, ac.size());
		// JDODataset d2 = ac.iterator().next();
		// Assert.assertEquals(dataset.getId(), d2.getId());
		// aa.close();
	}
}
