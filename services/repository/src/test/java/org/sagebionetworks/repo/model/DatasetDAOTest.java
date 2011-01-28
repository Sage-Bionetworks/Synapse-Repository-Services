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

import javax.jdo.JDOObjectNotFoundException;
import javax.jdo.PersistenceManager;

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
import org.sagebionetworks.repo.model.gaejdo.PMF;
import org.sagebionetworks.repo.model.gaejdo.Version;
import org.sagebionetworks.repo.web.NotFoundException;

import com.google.appengine.api.datastore.Key;
import com.google.appengine.api.datastore.KeyFactory;
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

	@Before
	public void setUp() throws Exception {
		helper.setUp();
		fac = new GAEJDODAOFactoryImpl();
	}

	@After
	public void tearDown() throws Exception {
		helper.tearDown();
	}

	// create a dataset and populate the shallow properties
	private static Dataset createShallow() {
		return createShallow("dataset name");
	}

	private static Dataset createShallow(String name) {
		Dataset d = new Dataset();
		Date now = new Date();
		d.setName(name);
		d.setCreator("Eric Schadt");
		d.setDescription("This is a genome-wide cancer study.");
		d.setCreationDate(now);
		d.setReleaseDate(now);
		d.setVersion("1.0");
		d.setStatus("in progress");
		return d;
	}

	@Test
	public void testCreateandRetrieve() throws Exception {
		Dataset d = createShallow();

		DatasetDAO dao = fac.getDatasetDAO();
		String id = dao.create(d);
		Assert.assertNotNull(id);
		dao.getStringAnnotationDAO().addAnnotation(id, "Tissue Type", "liver");
		// repeated annotations are to be ignored...
		dao.getStringAnnotationDAO().addAnnotation(id, "Tissue Type", "liver");
		// ... but multiple values for the same attribute are OK
		dao.getStringAnnotationDAO().addAnnotation(id, "Tissue Type", "brain");
		dao.getFloatAnnotationDAO().addAnnotation(id, "weight", 100F);
		Date now = new Date();
		dao.getDateAnnotationDAO().addAnnotation(id, "now", now);

		// test retrieving by ID
		Dataset d2 = dao.get(id);
		Assert.assertEquals(d.getName(), d2.getName());
		Assert.assertEquals(d.getDescription(), d2.getDescription());
		Assert.assertEquals(d.getCreator(), d2.getCreator());
		Assert.assertEquals(d.getCreationDate(), d2.getCreationDate());
		Assert.assertEquals(d.getStatus(), d2.getStatus());
		Assert.assertEquals(d.getReleaseDate(), d2.getReleaseDate());
		// test that annotations are also retrieved
		Annotations annots = dao.getAnnotations(id);
		Collection<String> tissueType = annots.getStringAnnotations().get(
				"Tissue Type");
		Assert.assertEquals(2, tissueType.size());
		Assert.assertEquals("liver", tissueType.iterator().next());
		Collection<Float> weightType = annots.getFloatAnnotations().get(
				"weight");
		Assert.assertEquals(1, weightType.size());
		Assert.assertEquals(100F, weightType.iterator().next());
		// test that version is retrieved
		Assert.assertEquals(d2.getVersion(), "1.0");

		// test that name is required

		d2.setName(null);
		try {
			dao.update(d2);
			Assert.fail("should have thrown InvalidModelException");
		} catch (InvalidModelException ime) {
			// as expected
		}

		// retrieve the annotations sorted
		dao.getStringAnnotationDAO().getInRangeSortedBy(0, 2, "Tissue", true);

	}

	private static InputDataLayer createLayer(Date date) {
		InputDataLayer ans = new InputDataLayer();
		ans.setName("input layer");
		ans.setDescription("description");
		ans.setCreationDate(date);
		ans.setVersion("1.0");
		ans.setPublicationDate(date);
		ans.setReleaseNotes("this version contains important revisions");
		ans.setType("clinical");
		ans.setTissueType("cell line");
		ans.setPlatform("Affymetrix");
		ans.setProcessingFacility("Broad Institute");
		ans.setQcBy("Fred");
		ans.setQcDate(date);
		return ans;
	}

	@Test
	public void testCreateAndRetrieveLayer() throws Exception {
		Dataset d = createShallow();

		DatasetDAO dao = fac.getDatasetDAO();
		String id = dao.create(d);
		Assert.assertNotNull(id);

		Date now = new Date();
		InputDataLayer layer1 = createLayer(now);
		layer1.setName("clinical data");
		InputDataLayerDAO layerDAO = dao.getInputDataLayerDAO(id);
		layerDAO.create(layer1);

		InputDataLayer layer2 = createLayer(now);
		layer2.setName("genotyping data");
		layerDAO.create(layer2);

		Collection<InputDataLayer> layers = layerDAO.getInRange(0, 100);
		Assert.assertEquals(2, layers.size());

		layers = layerDAO.getInRangeSortedByPrimaryField(0, 100, "name", true);
		Assert.assertEquals(2, layers.size());
		Assert.assertEquals("clinical data", layers.iterator().next().getName());

		layers = layerDAO.getInRangeSortedByPrimaryField(0, 100, "name", false);
		Assert.assertEquals(2, layers.size());
		Assert.assertEquals("genotyping data", layers.iterator().next()
				.getName());
	}

	@Test
	public void testCreateAndUpdate() throws Exception {
	}

	@Test
	public void testCreateAndDelete() throws Exception {
		Dataset d = createShallow();

		DatasetDAO dao = fac.getDatasetDAO();
		String id = dao.create(d);
		Assert.assertNotNull(id);

		Date now = new Date();
		InputDataLayer layer = createLayer(now);
		InputDataLayerDAO layerDAO = dao.getInputDataLayerDAO(id);
		String layerId = layerDAO.create(layer);

		Assert.assertNotNull(layerId);
		Assert.assertNotNull(layer.getId());
		
		Assert.assertEquals(1, dao.getInputDataLayerDAO(id).getInRange(0, 100).size());
		dao.delete(id);

		try {
			dao.get(id);
			Assert.fail("exception expected");
		} catch (NotFoundException e) {
			// as expected
		}
 
		// now try to get the layer. should be gone
		PersistenceManager pm = PMF.get();
		try {
			Key key = KeyFactory.stringToKey(layerId);
			GAEJDOInputDataLayer jdo = (GAEJDOInputDataLayer) pm.getObjectById(
					GAEJDOInputDataLayer.class, key);
			Assert.fail("exception expected");
		} catch (Exception e) {
			// as expected
		} finally {
			pm.close();
		}

	}

	@Test
	public void testGetCount() throws Exception {
		DatasetDAO dao = fac.getDatasetDAO();
		String id1 = dao.create(createShallow("dataset 1"));
		Dataset ds2 = createShallow("dataset 2");
		String id2 = dao.create(ds2);
		String id3 = dao.create(createShallow("dataset 3"));
		Assert.assertEquals(3, dao.getCount());
		dao.delete(id2);
		Assert.assertEquals(2, dao.getCount());
		System.out.println(ds2.getVersion());
		// now let's create a revision of ds2!
		ds2.setVersion("2.0");
		Date revisionDate = new Date(); // may
		//dao.revise(ds2, revisionDate); < THIS BREAKS!!!!
	}

	@Test
	public void testGetInRange() throws Exception {
	}

	@Test
	public void testGetPrimaryFields() throws Exception {
	}

	@Test
	public void testGetInRangeSortedByPrimaryField() throws Exception {
		DatasetDAO dao = fac.getDatasetDAO();
		Dataset d = null;
		d = createShallow();
		d.setName("d1");
		dao.create(d);
		dao.getStringAnnotationDAO().addAnnotation(d.getId(), "stringAttr",
				d.getName());
		d = createShallow();
		d.setName("d4");
		dao.create(d);
		dao.getStringAnnotationDAO().addAnnotation(d.getId(), "stringAttr",
				d.getName());
		d = createShallow();
		d.setName("d3");
		dao.create(d);
		dao.getStringAnnotationDAO().addAnnotation(d.getId(), "stringAttr",
				d.getName());
		d = createShallow();
		d.setName("d3");
		dao.create(d);
		dao.getStringAnnotationDAO().addAnnotation(d.getId(), "stringAttr",
				d.getName());
		List<Dataset> ans;
		ans = dao.getInRangeSortedByPrimaryField(0, 2, "name", /* ascending */
				true);
		Assert.assertEquals(2, ans.size());
		Assert.assertEquals("d1", ans.get(0).getName());
		Assert.assertEquals("d3", ans.get(1).getName());
		ans = dao.getInRangeSortedByPrimaryField(2, 4, "name", /* ascending */
				true);
		Assert.assertEquals(2, ans.size());
		Assert.assertEquals("d3", ans.get(0).getName());
		Assert.assertEquals("d4", ans.get(1).getName());
		ans = dao.getInRangeSortedByPrimaryField(0, 3, "name", /* ascending */
				false);
		Assert.assertEquals(3, ans.size());
		Assert.assertEquals("d4", ans.get(0).getName());
		Assert.assertEquals("d3", ans.get(1).getName());
		Assert.assertEquals("d3", ans.get(2).getName());

		// out of range -> no error, just no result
		ans = dao.getInRangeSortedByPrimaryField(10, 20, "name", /* ascending */
				false);
		Assert.assertEquals(0, ans.size());

		ans = dao.getStringAnnotationDAO().getInRangeSortedBy(0, 10,
				"stringAttr", true);
		Assert.assertEquals(4, ans.size());
		String order = "Order:";
		for (Dataset ds : ans)
			order += " " + ds.getName();
		Assert.assertEquals(order, "d1", ans.get(0).getName());
		Assert.assertEquals(order, "d3", ans.get(1).getName());
		Assert.assertEquals(order, "d3", ans.get(2).getName());
		Assert.assertEquals(order, "d4", ans.get(3).getName());

	}

	@Test
	public void testgetInRangeHavingByPrimaryField() throws Exception {
	}

	@Test
	public void testRetrieveByAnnot() throws Exception {
		// test querying after revision -- should only get the latest

		Dataset d = new Dataset();
		Date now = new Date();
		d.setName("dataset name");
		d.setCreationDate(now);
		d.setVersion("1.0");
		d.setStatus("in progress");

		DatasetDAO dao = fac.getDatasetDAO();
		String id = dao.create(d);
		Assert.assertNotNull(id);
		dao.getStringAnnotationDAO().addAnnotation(id, "Tissue Type", "liver");
		dao.getFloatAnnotationDAO().addAnnotation(id, "weight", 100F);

		// test retrieving by ID
		Dataset d2 = dao.get(id);
		Assert.assertEquals(d.getName(), d2.getName());
		// test that annotations are also retrieved
		Annotations annots = dao.getAnnotations(id);
		Collection<String> tissueType = annots.getStringAnnotations().get(
				"Tissue Type");
		Assert.assertEquals(1, tissueType.size());
		Assert.assertEquals("liver", tissueType.iterator().next());
		Collection<Float> weightType = annots.getFloatAnnotations().get(
				"weight");
		Assert.assertEquals(1, weightType.size());
		Assert.assertEquals(100F, weightType.iterator().next());
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

		c = dao.getFloatAnnotationDAO().getInRangeSortedBy(0, 100, "weight",
				true);
		Assert.assertEquals(1, c.size());
		Assert.assertEquals(d.getName(), c.iterator().next().getName());

		// test retrieving filtering by annotation
		c = dao.getFloatAnnotationDAO()
				.getInRangeHaving(0, 100, "weight", 100F);
		Assert.assertEquals(1, c.size());
		Assert.assertEquals(d.getName(), c.iterator().next().getName());

		c = dao.getStringAnnotationDAO().getInRangeHaving(0, 100,
				"Tissue Type", "liver");
		Assert.assertEquals(1, c.size());
		Assert.assertEquals(d.getName(), c.iterator().next().getName());

	}

	@Test
	public void testGetAllAnnotations() throws Exception {

	}

	@Test
	public void testAnnotationDAO() throws Exception {

	}

	@Test
	public void testCreateRevision() throws Exception {

	}

	@Test
	public void testGetLatest() throws Exception {

	}

	@Test
	public void testRetrieveByRevision() throws Exception {
	}

}
