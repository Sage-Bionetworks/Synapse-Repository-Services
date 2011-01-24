package org.sagebionetworks.repo.model;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
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
import org.sagebionetworks.repo.model.gaejdo.GAEJDODatasetLayer;
import org.sagebionetworks.repo.model.gaejdo.GAEJDODataset;
import org.sagebionetworks.repo.model.gaejdo.GAEJDOInputDataLayer;
import org.sagebionetworks.repo.model.gaejdo.GAEJDORevision;
import org.sagebionetworks.repo.model.gaejdo.GAEJDOStringAnnotation;
import org.sagebionetworks.repo.model.gaejdo.Version;

import com.google.appengine.api.datastore.Key;
import com.google.appengine.api.datastore.Text;
import com.google.appengine.tools.development.testing.LocalDatastoreServiceTestConfig;
import com.google.appengine.tools.development.testing.LocalServiceTestHelper;

public class DatasetDAOTest {
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
				fac.getDatasetDAO().delete(id);
			id = null;
		}
		helper.tearDown();
	}

	@Test
	public void testCreateandRetrieve() throws Exception {
		Dataset d = new Dataset();
		Date now = new Date();
		d.setName("dataset name");
		d.setCreationDate(now);
		d.setVersion("1.0");
		d.setStatus("in progress");
		DatasetDAO dao = fac.getDatasetDAO();
		id = dao.create(d);
		Assert.assertNotNull(id);
		dao.getStringAnnotationDAO().addAnnotation(id, "Tissue Type", "liver");

		// test retrieving by ID
		Dataset d2 = dao.get(id);
		Assert.assertEquals(d.getName(), d2.getName());
		// test that annotations are also retrieved
		Annotations annots = dao.getAnnotations(id);
		Collection<String> tissueType = annots.getStringAnnotations().get(
				"Tissue Type");
		Assert.assertEquals(1, tissueType.size());
		Assert.assertEquals("liver", tissueType.iterator().next());
		// test that version is retrieved
		Assert.assertEquals(d2.getVersion(), "1.0");
		Collection<Dataset> c;
		// test retrieving by range
		c = dao.getInRange(0, 100);
		Assert.assertEquals(1, c.size());
		Assert.assertEquals(d.getName(), c.iterator().next().getName());
		// test retrieving filtering by primary field
		c = dao.getInRangeHavingPrimaryField(0, 100, "status", "in progress");
		Assert.assertEquals(1, c.size());
		Assert.assertEquals(d.getName(), c.iterator().next().getName());
		// test retrieving, sorted by primary field
		c = dao.getInRangeSortedByPrimaryField(0, 100, "status", true);
		Assert.assertEquals(1, c.size());
		Assert.assertEquals(d.getName(), c.iterator().next().getName());
		// test retrieving sorted by annotation
		c = dao.getStringAnnotationDAO().getInRangeSortedBy(0, 100,
				"Tissue Type", true);
		Assert.assertEquals(1, c.size());
		Assert.assertEquals(d.getName(), c.iterator().next().getName());
		// test retrieving filtering by annotaton
		c = dao.getStringAnnotationDAO().getInRangeHaving(0, 100,
				"Tissue Type", "liver");
		Assert.assertEquals(1, c.size());
		Assert.assertEquals(d.getName(), c.iterator().next().getName());

		// // create a new project
		// GAEJDODataset dataset = new GAEJDODataset();
		// dataset.setName("dataset name");
		// dataset.setDescription("description");
		// String overview =
		// "This dataset is a megacross, and includes genotyoping data.";
		// dataset.setOverview(new Text(overview));
		// Date release = new Date();
		// dataset.setReleaseDate(release);
		// dataset.setStatus("IN_PROGRESS");
		// List<String> contributors = Arrays.asList(new String[]{"Larry",
		// "Curly", "Moe"});
		// dataset.setContributors(contributors);
		// dataset.setDownloadable(true);
		//
		// Collection<Key> layers = new HashSet<Key>();
		// dataset.setLayers(layers);
		// GAEJDOInputDataLayer idl = new GAEJDOInputDataLayer();
		// idl.setType(GAEJDOInputDataLayer.DataType.EXPRESSION);
		// idl.setRevision(new GAEJDORevision<GAEJDODatasetLayer>());
		//
		// InputDataLayerDAO dla = fac.getInputDataLayerAccessor();
		// dla.makePersistent(idl);
		// layers.add(idl.getId());
		//
		// GAEJDORevision<GAEJDODataset> r = new
		// GAEJDORevision<GAEJDODataset>();
		// Date revDate = new Date();
		// r.setRevisionDate(revDate);
		// r.setVersion(new Version("1.0.0"));
		// dataset.setRevision(r);
		//
		// GAEJDOAnnotations annots = new GAEJDOAnnotations();
		// dataset.setAnnotations(annots);
		// GAEJDOStringAnnotation stringAnnot = new
		// GAEJDOStringAnnotation("testKey", "testValue");
		// annots.getStringAnnotations().add(stringAnnot);
		//
		// // persist it
		// DatasetDAO da = fac.getDatasetAccessor();
		// da.makePersistent(dataset);
		// this.key=dataset.getId();
		//
		// // persisting creates a Key, which we can grab
		// Key id = dataset.getId();
		// Assert.assertNotNull(id);
		//
		// // now retrieve the object by its key
		// GAEJDODataset d2 = da.getDataset(id);
		// Assert.assertNotNull(d2);
		//
		// // check that all the fields were persisted
		// Assert.assertEquals("dataset name", d2.getName());
		// Assert.assertEquals("description", d2.getDescription());
		// Assert.assertEquals(overview, d2.getOverview().getValue());
		// Assert.assertEquals(release, d2.getReleaseDate());
		// Assert.assertEquals("IN_PROGRESS", d2.getStatus());
		// Assert.assertEquals(contributors, d2.getContributors());
		// Assert.assertEquals(true, d2.isDownloadable());
		// GAEJDORevision<GAEJDODataset> r2 = d2.getRevision();
		// Assert.assertNotNull(r2);
		// // Assert.assertEquals(d2, r2.getOwner());
		// Assert.assertEquals(revDate, r2.getRevisionDate());
		// Assert.assertEquals(new Version("1.0.0"), r2.getVersion());
		//
		// Collection<Key> l2 = d2.getLayers();
		// Assert.assertEquals(1, l2.size());
		// Key dlKey = l2.iterator().next();
		// GAEJDOInputDataLayer idl2 = dla.getDataLayer(dlKey);
		// // Assert.assertTrue((idl2 instanceof InputDataLayer));
		// GAEJDOInputDataLayer.DataType type = idl2.getType();
		// Assert.assertEquals(GAEJDOInputDataLayer.DataType.EXPRESSION, type);
		//
		// GAEJDOAnnotations annots2 = d2.getAnnotations();
		// Assert.assertNotNull(annots2);
		// Collection<GAEJDOStringAnnotation> stringAnnots =
		// annots2.getStringAnnotations();
		// Assert.assertNotNull(stringAnnots);
		// Assert.assertEquals(1, stringAnnots.size());
		// GAEJDOStringAnnotation stringAnnot2 = stringAnnots.iterator().next();
		// Assert.assertEquals("testKey", stringAnnot2.getAttribute());
		// Assert.assertEquals("testValue", stringAnnot2.getValue());
	}

}
