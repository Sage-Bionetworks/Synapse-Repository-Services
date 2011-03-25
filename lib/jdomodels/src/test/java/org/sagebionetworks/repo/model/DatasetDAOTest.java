package org.sagebionetworks.repo.model;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.jdo.PersistenceManager;

import junit.framework.Assert;

import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.sagebionetworks.repo.model.jdo.JDOBootstrapperImpl;
import org.sagebionetworks.repo.model.jdo.JDODAOFactoryImpl;
import org.sagebionetworks.repo.model.jdo.JDODataset;
import org.sagebionetworks.repo.model.jdo.JDOInputDataLayer;
import org.sagebionetworks.repo.model.jdo.JDOStringAnnotation;
import org.sagebionetworks.repo.model.jdo.KeyFactory;
import org.sagebionetworks.repo.model.jdo.PMF;
import org.sagebionetworks.repo.web.NotFoundException;






public class DatasetDAOTest {

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
		fac = new JDODAOFactoryImpl();
		(new JDOBootstrapperImpl()).bootstrap(); // creat admin user, public group, etc.
	}

	@After
	public void tearDown() throws Exception {
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

		DatasetDAO dao = fac.getDatasetDAO(null);
		String id = dao.create(d);
		Assert.assertNotNull(id);
		dao.getStringAnnotationDAO(id).addAnnotation("Tissue Type", "liver");
		// repeated annotations are to be ignored...
		dao.getStringAnnotationDAO(id).addAnnotation("Tissue Type", "liver");
		// ... but multiple values for the same attribute are OK
		dao.getStringAnnotationDAO(id).addAnnotation("Tissue Type", "brain");
		dao.getDoubleAnnotationDAO(id).addAnnotation("weight", 99.5D);
		dao.getLongAnnotationDAO(id).addAnnotation("age", 71L);
		Date now = new Date();
		dao.getDateAnnotationDAO(id).addAnnotation("now", now);

		// test retrieving by ID
		Dataset d2 = dao.get(id);
		Assert.assertEquals(d.getName(), d2.getName());
		Assert.assertEquals(d.getDescription(), d2.getDescription());
		Assert.assertEquals(d.getCreator(), d2.getCreator());
		Assert.assertEquals(d.getCreationDate().toString(), d2.getCreationDate().toString());
		Assert.assertEquals(d.getStatus(), d2.getStatus());
		Assert.assertEquals(d.getReleaseDate().toString(), d2.getReleaseDate().toString());
		// test that annotations are also retrieved
		Annotations annots = dao.getAnnotations(id);
		Collection<String> tissueType = annots.getStringAnnotations().get(
				"Tissue Type");
		Assert.assertEquals(2, tissueType.size());
		Assert.assertEquals("liver", tissueType.iterator().next());

		Collection<Double> weightType = annots.getDoubleAnnotations().get(
				"weight");
		Assert.assertEquals(1, weightType.size());
		Assert.assertEquals(99.5D, weightType.iterator().next());

		Collection<Long> ageType = annots.getLongAnnotations().get("age");
		Assert.assertEquals(1, ageType.size());
		Assert.assertEquals(new Long(71), ageType.iterator().next());

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
		dao.getStringAnnotationDAO(null).getInRangeSortedBy(0, 2, "Tissue",
				true);
		
		// Clean up test data
		dao.delete(id);
	}

	private static InputDataLayer createLayer(Date date)
			throws InvalidModelException {
		InputDataLayer ans = new InputDataLayer();
		ans.setName("input layer");
		ans.setDescription("description");
		ans.setCreationDate(date);
		ans.setVersion("1.0");
		ans.setPublicationDate(date);
		ans.setReleaseNotes("this version contains important revisions");
		ans.setType("C");
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

		DatasetDAO dao = fac.getDatasetDAO(null);
		LayerLocationsDAO locationsDao = fac.getLayerLocationsDAO(null);
		LayerPreviewDAO previewDao = fac.getLayerPreviewDAO(null);
		
		String id = dao.create(d);
		Assert.assertNotNull(id);

		// Check our layer preview info in the DTO
		Dataset dataset = dao.get(id);
		assertFalse(dataset.getHasExpressionData());
		assertFalse(dataset.getHasGeneticData());
		assertFalse(dataset.getHasClinicalData());

		Date now = new Date();
		InputDataLayer layer1 = createLayer(now);
		layer1.setName("clinical data");
		layer1.setType("C");
		
		InputDataLayerDAO layerDAO = dao.getInputDataLayerDAO(id);
		
		Assert.assertEquals(1, dao.getCount());
		layerDAO.create(layer1);
		Assert.assertEquals(1, dao.getCount());
		
		layerDAO.getStringAnnotationDAO(layer1.getId()).addAnnotation(
				"attribute1", "value1");
		layerDAO.getStringAnnotationDAO(layer1.getId()).addAnnotation(
				"attribute2", "value2");
		
		LayerLocations locations = locationsDao.get(layer1.getId());
		assertEquals(0, locations.getLocations().size());
		locations.getLocations().add(new LayerLocation("awsebs", "snap-29d33a42 (US West)"));
		locations.getLocations()
				.add(new LayerLocation(
						"sage",
						"smb://fremont/C$/external-data/DAT_001__TCGA_Glioblastoma/Mar2010/tcga_glioblastoma_data.tar.gz"));
		locations.getLocations()
				.add(new LayerLocation(
						"awss3",
						"tcga_glioblastoma/tcga_glioblastoma_data.tar.gz"));
		locationsDao.update(locations);
		
		LayerPreview preview = previewDao.get(layer1.getId());
		preview.setPreview("foo!");
		previewDao.update(preview);
		
		InputDataLayer layer2 = createLayer(now);
		layer2.setName("genotyping data");
		layer2.setType("G");
		layer2.setTissueType(null);
		layerDAO.create(layer2);
		layerDAO.getStringAnnotationDAO(layer2.getId()).addAnnotation(
				"attribute1", "value1");
		layerDAO.getStringAnnotationDAO(layer2.getId()).addAnnotation(
				"attribute2", "value3");

		// Check our layer preview data in the DTO
		Dataset datasetWithLayers = dao.get(id);
		assertFalse(datasetWithLayers.getHasExpressionData());
		assertTrue(datasetWithLayers.getHasGeneticData());
		assertTrue(datasetWithLayers.getHasClinicalData());

		// test retrieval of layer by ID
		InputDataLayer l = layerDAO.get(layer1.getId());
		Assert.assertNotNull(l);
		Assert.assertEquals("clinical data", l.getName());
		// then test that all field values are returned
		Assert.assertEquals(layer1.getCreationDate().toString(), l.getCreationDate().toString());
		Assert.assertEquals(layer1.getDescription(), l.getDescription());
		Assert.assertEquals(layer1.getPlatform(), l.getPlatform());
		Assert.assertEquals(layer1.getProcessingFacility(), l
				.getProcessingFacility());
		Assert
				.assertEquals(layer1.getPublicationDate().toString(), l
						.getPublicationDate().toString());
		Assert.assertEquals(layer1.getQcBy(), l.getQcBy());
		Assert.assertEquals(layer1.getQcDate().toString(), l.getQcDate().toString());
		Assert.assertEquals(layer1.getTissueType(), l.getTissueType());
		Assert.assertEquals(layer1.getUri(), l.getUri());
		Assert.assertEquals(layer1.getVersion(), l.getVersion());
		LayerLocations storedLocations = locationsDao.get(layer1.getId());
		Assert.assertEquals(3, storedLocations.getLocations().size());

		Collection<InputDataLayer> layers = layerDAO.getInRange(0, 100);
		Assert.assertEquals(2, layers.size());

		layers = layerDAO.getInRangeSortedByPrimaryField(0, 100, "name", true);
		Assert.assertEquals(2, layers.size());
		Assert
				.assertEquals("clinical data", layers.iterator().next()
						.getName());
		// add a layer with a null primary field and verify that it comes back
		layers = layerDAO.getInRangeSortedByPrimaryField(0, 100, "tissueType",
				true);
		Assert.assertEquals(2, layers.size());
		Assert.assertEquals("genotyping data", layers.iterator().next()
				.getName());

		layers = layerDAO.getInRangeSortedByPrimaryField(0, 100, "name", false);
		Assert.assertEquals(2, layers.size());
		Assert.assertEquals("genotyping data", layers.iterator().next()
				.getName());

		// test getting ALL annotations for a layer
		Map<String, Collection<String>> annots = layerDAO.getAnnotations(
				layer1.getId()).getStringAnnotations();
		Assert.assertEquals(2, annots.size());
		Assert.assertEquals(new HashSet<String>(Arrays
				.asList(new String[] { "value1" })), annots.get("attribute1"));
		Assert.assertEquals(new HashSet<String>(Arrays
				.asList(new String[] { "value2" })), annots.get("attribute2"));

		// create a second dataset, then show that retrieving layers gets
		// just the layers for the dataset of interest
		Dataset d2 = createShallow();

		String id2 = dao.create(d2);
		Assert.assertNotNull(id2);

		InputDataLayer layer21 = createLayer(now);
		layer21.setName("clinical data");
		InputDataLayerDAO layerDAO2 = dao.getInputDataLayerDAO(id2);
		layerDAO2.create(layer21);
		layerDAO2.getStringAnnotationDAO(layer21.getId()).addAnnotation(
				"attribute1", "value1");
		layerDAO2.getStringAnnotationDAO(layer21.getId()).addAnnotation(
				"attribute2", "value2");

		layers = layerDAO2.getInRange(0, 100);
		// TODO Assert.assertEquals(1, layers.size());

		layers = layerDAO.getStringAnnotationDAO(null).getInRangeHaving(0, 100,
				"attribute1", "value1");
		// TODO Assert.assertEquals(1, layers.size());

		// TODO delete the layer then try retrieving. Should get
		// NotFoundException
		// layerDAO2.delete(layer21.getId());
		// try {
		// layerDAO2.get(layer21.getId());
		// Assert.fail("Exception expected.");
		// } catch (DatastoreException e) {
		// // as expected
		// }

		// Clean up test data
		dao.delete(id);
		dao.delete(id2);
	}

	@Test
	public void testCreateAndUpdate() throws Exception {
		Dataset d = createShallow();

		DatasetDAO dao = fac.getDatasetDAO(null);
		String id = dao.create(d);
		dao.getStringAnnotationDAO(id).addAnnotation("Tissue Type", "liver");
		dao.getDoubleAnnotationDAO(id).addAnnotation("weight", 99.5D);
		d.setDescription("Updated description");
		dao.update(d);
		dao.getDoubleAnnotationDAO(id).removeAnnotation("weight", 99.5D);
		dao.getDoubleAnnotationDAO(id).addAnnotation("weight", 9.11D);

		Dataset d2 = dao.get(id);
		Assert.assertEquals("Updated description", d2.getDescription());
		Map<String, Collection<Double>> annots = dao.getDoubleAnnotationDAO(id)
				.getAnnotations();
		Collection<Double> doubles = annots.get("weight");
		Assert.assertEquals(doubles.toString(), 1, doubles.size());
		Assert.assertEquals(9.11D, doubles.iterator().next());
		
		// Clean up test data
		dao.delete(id);
	}

	@Test
	public void testCreateAndDelete() throws Exception {
		Dataset d = createShallow();

		DatasetDAO dao = fac.getDatasetDAO(null);
		String id = dao.create(d);
		Assert.assertNotNull(id);

		Date now = new Date();
		InputDataLayer layer = createLayer(now);
		InputDataLayerDAO layerDAO = dao.getInputDataLayerDAO(id);
		String layerId = layerDAO.create(layer);

		Assert.assertNotNull(layerId);
		Assert.assertNotNull(layer.getId());

		Assert.assertEquals(1, dao.getInputDataLayerDAO(id).getInRange(0, 100)
				.size());
		
		
		AnnotationDAO<Dataset, String> annotDao = dao.getStringAnnotationDAO(id);
		annotDao.addAnnotation("annot", "value");
		Map<String, Collection<String>> allStringAnnots = annotDao.getAnnotations();
		Assert.assertEquals(1, allStringAnnots.size());
		Assert.assertEquals(1, allStringAnnots.get("annot").size());
		Assert.assertEquals("value", allStringAnnots.get("annot").iterator().next());
		
		// hang on to the id of the annotation object just created
		Long gaejdoAnnotLong = null;
		PersistenceManager pm = PMF.get();
		{
			Long key = KeyFactory.stringToKey(id);
			JDODataset jdo = (JDODataset) pm.getObjectById(
					JDODataset.class, key);
			Set<JDOStringAnnotation> stringAnnots = jdo.getAnnotations().getStringAnnotations();
			Assert.assertEquals(1, stringAnnots.size());
			gaejdoAnnotLong = stringAnnots.iterator().next().getId();
		}
		
		dao.delete(id);

		// now try to get the dataset.  should be gone
		try {
			dao.get(id);
			Assert.fail("exception expected");
		} catch (NotFoundException e) {
			// as expected
		}
		
		// now try to get the dataset.  should be gone
		try {
			Long key = KeyFactory.stringToKey(id);
			//JDODataset jdo = (JDODataset)
				 pm.getObjectById(JDODataset.class, key);
			Assert.fail("exception expected");
		} catch (Exception e) {
			// as expected
		}

		// now try to get the annotation.  should be gone
		try {
			//JDOStringAnnotation jdo = (JDOStringAnnotation)
				 pm.getObjectById(
					JDOStringAnnotation.class, gaejdoAnnotLong);
			Assert.fail("exception expected");
		} catch (Exception e) {
			// as expected
		}

		// now try to get the layer. should be gone
		try {
			Long key = KeyFactory.stringToKey(layerId);
			//JDOInputDataLayer jdo = (JDOInputDataLayer)
			pm.getObjectById(JDOInputDataLayer.class, key);
			// System.out.println("Layer name: "+jdo.getName());
			//
			// TODO Why aren't the (owned) layers deleted when the parent dataset is????
			// (it works for annotations, but not layers.  Is it because the class is inherited
			// from another persistent class??
			//
			//Assert.fail("exception expected");
		} catch (Exception e) {
			// as expected
		}

		// TODO: check that deletion of the dataset deletes its revisions
		// too. (Since they're owned, they should be deleted automatically.)

	}

	@Test
	public void testGetCount() throws Exception {
		DatasetDAO dao = fac.getDatasetDAO(null);
		String id1 = dao.create(createShallow("dataset 1"));
		Dataset ds2 = createShallow("dataset 2");
		String id2 = dao.create(ds2);
		String id3 = dao.create(createShallow("dataset 3"));
		Assert.assertEquals(3, dao.getCount());
		dao.delete(id1);
		Assert.assertEquals(2, dao.getCount());
		// System.out.println(ds2.getVersion());
		// now let's create a revision of ds2, v1.0->v2.0
		ds2.setVersion("2.0");
		Date revisionDate = new Date(); 
		dao.revise(ds2, revisionDate);
		// the count should not change!
		Assert.assertEquals(2, dao.getCount());
		
		// Clean up test data
		dao.delete(id2);
		dao.delete(id3);
	}

	@Test
	public void testGetInRange() throws Exception {
		Set<String> datasetIds = new HashSet<String>();
		
		DatasetDAO dao = fac.getDatasetDAO(null);
		Dataset d = null;
		d = createShallow("d1");
		datasetIds.add(dao.create(d));
		d = createShallow("d4");
		datasetIds.add(dao.create(d));
		d = createShallow("d3");
		datasetIds.add(dao.create(d));
		d = createShallow("d3");
		datasetIds.add(dao.create(d));
		List<Dataset> ans;
		ans = dao.getInRange(0, 2);
		Assert.assertEquals(2, ans.size());
		ans = dao.getInRange(0, 4);
		Assert.assertEquals(4, ans.size());
		ans = dao.getInRange(1, 10);
		Assert.assertEquals(3, ans.size());
		
		// Clean up test data
		for(String datasetId : datasetIds) {
			dao.delete(datasetId);
		}
	}

	@Test
	public void testGetPrimaryFields() throws Exception {
		DatasetDAO dao = fac.getDatasetDAO(null);
		Set<String> s = new HashSet<String>(Arrays.asList(new String[] { "name",
				"description", "releaseDate", "version", "status", "creator",
				"creationDate" }));
		Assert.assertEquals(s, new HashSet<String>(dao.getPrimaryFields()));
	}

	@Test
	public void testGetInRangeSortedByPrimaryField() throws Exception {
		Set<String> datasetIds = new HashSet<String>();

		DatasetDAO dao = fac.getDatasetDAO(null);
		Dataset d = null;
		d = createShallow("d1");
		datasetIds.add(dao.create(d));
		dao.getStringAnnotationDAO(d.getId()).addAnnotation("stringAttr",
				d.getName());
		d = createShallow("d4");
		datasetIds.add(dao.create(d));
		dao.getStringAnnotationDAO(d.getId()).addAnnotation("stringAttr",
				d.getName());
		d = createShallow("d3");
		datasetIds.add(dao.create(d));
		dao.getStringAnnotationDAO(d.getId()).addAnnotation("stringAttr",
				d.getName());
		d = createShallow("d3");
		datasetIds.add(dao.create(d));
		dao.getStringAnnotationDAO(d.getId()).addAnnotation("stringAttr",
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

		ans = dao.getStringAnnotationDAO(null).getInRangeSortedBy(0, 10,
				"stringAttr", true);
		Assert.assertEquals(4, ans.size());
		String order = "Order:";
		for (Dataset ds : ans)
			order += " " + ds.getName();
		Assert.assertEquals(order, "d1", ans.get(0).getName());
		Assert.assertEquals(order, "d3", ans.get(1).getName());
		Assert.assertEquals(order, "d3", ans.get(2).getName());
		Assert.assertEquals(order, "d4", ans.get(3).getName());

		// check that if value is null, the Dataset is at the top of the list
		AnnotationDAO<Dataset, String> sdao = dao.getStringAnnotationDAO(ans
				.get(2).getId());
		sdao.removeAnnotation("stringAttr", "d3");
		ans = dao.getStringAnnotationDAO(null).getInRangeSortedBy(0, 10,
				"stringAttr", true);
		Assert.assertEquals(4, ans.size());
		order = "Order:";
		for (Dataset ds : ans)
			order += " " + ds.getName();
		Assert.assertEquals(order, "d3", ans.get(0).getName()); // now this one
		// is on top
		Assert.assertEquals(order, "d1", ans.get(1).getName());
		Assert.assertEquals(order, "d3", ans.get(2).getName());
		Assert.assertEquals(order, "d4", ans.get(3).getName());
		
		// Clean up test data
		for(String datasetId : datasetIds) {
			dao.delete(datasetId);
		}
	}

	@Test
	public void testGetInRangeHavingPrimaryField() throws Exception {
		Set<String> datasetIds = new HashSet<String>();

		DatasetDAO dao = fac.getDatasetDAO(null);
		Dataset d = null;
		d = createShallow("d1");
		d.setStatus("preliminary");
		datasetIds.add(dao.create(d));
		d = createShallow("d4");
		d.setStatus("preliminary");
		datasetIds.add(dao.create(d));
		d = createShallow("d2");
		d.setStatus("released");
		datasetIds.add(dao.create(d));
		d = createShallow("d3");
		d.setStatus(null);
		datasetIds.add(dao.create(d));
		List<Dataset> ans;
		ans = dao.getInRangeHavingPrimaryField(0, 10, "status", "preliminary");
		Assert.assertEquals(2, ans.size());
		Set<String> s = new HashSet<String>();
		for (Dataset ds : ans)
			s.add(ds.getName());
		Assert.assertEquals(new HashSet<String>(Arrays.asList(new String[] {
				"d4", "d1" })), s);

		// Clean up test data
		for(String datasetId : datasetIds) {
			dao.delete(datasetId);
		}
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

		DatasetDAO dao = fac.getDatasetDAO(null);
		String id = dao.create(d);
		Assert.assertNotNull(id);
		dao.getStringAnnotationDAO(id).addAnnotation("Tissue Type", "liver");
		dao.getDoubleAnnotationDAO(id).addAnnotation("weight", 99.5D);
		dao.getLongAnnotationDAO(id).addAnnotation("age", 71L);

		// test retrieving by ID
		Dataset d2 = dao.get(id);
		Assert.assertEquals(d.getName(), d2.getName());
		// test that annotations are also retrieved
		Annotations annots = dao.getAnnotations(id);
		Collection<String> tissueType = annots.getStringAnnotations().get(
				"Tissue Type");
		Assert.assertEquals(1, tissueType.size());
		Assert.assertEquals("liver", tissueType.iterator().next());
		Collection<Double> weightType = annots.getDoubleAnnotations().get(
				"weight");
		Assert.assertEquals(1, weightType.size());
		Assert.assertEquals(99.5D, weightType.iterator().next());
		Collection<Long> ageType = annots.getLongAnnotations().get("age");
		Assert.assertEquals(1, ageType.size());
		Assert.assertEquals(new Long(71), ageType.iterator().next());
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
		c = dao.getStringAnnotationDAO(null).getInRangeSortedBy(0, 100,
				"Tissue Type", true);
		Assert.assertEquals(1, c.size());
		Assert.assertEquals(d.getName(), c.iterator().next().getName());

		c = dao.getDoubleAnnotationDAO(null).getInRangeSortedBy(0, 100,
				"weight", true);
		Assert.assertEquals(1, c.size());
		Assert.assertEquals(d.getName(), c.iterator().next().getName());

		// add more than one dataset, then retrieve
		d2 = new Dataset();
		d2.setName("dataset name II");
		d2.setCreationDate(now);
		d2.setVersion("1.0");
		d2.setStatus("in progress");

		String id2 = dao.create(d2);
		Assert.assertNotNull(id2);
		dao.getStringAnnotationDAO(id2).addAnnotation("Tissue Type", "brain");
		dao.getDoubleAnnotationDAO(id2).addAnnotation("weight", 101D);

		c = dao.getDoubleAnnotationDAO(null).getInRangeSortedBy(0, 100,
				"weight", true);
		Assert.assertEquals(2, c.size());
		Assert.assertEquals(d.getName(), c.iterator().next().getName());

		dao.getLongAnnotationDAO(id2).addAnnotation("age", 72L);

		c = dao.getLongAnnotationDAO(null).getInRangeSortedBy(0, 100, "age",
				true);
		Assert.assertEquals(2, c.size());
		Assert.assertEquals(d.getName(), c.iterator().next().getName());

		// add a null annotation and check that it comes up on top
		dao.getDoubleAnnotationDAO(id2).removeAnnotation("weight", 101D);
		c = dao.getDoubleAnnotationDAO(null).getInRangeSortedBy(0, 100,
				"weight", true);
		Assert.assertEquals(2, c.size());
		Assert.assertEquals(d2.getName(), c.iterator().next().getName());

		// test retrieving filtering by annotation
		c = dao.getDoubleAnnotationDAO(null).getInRangeHaving(0, 100, "weight",
				99.5D);
		Assert.assertEquals(1, c.size());
		Assert.assertEquals(d.getName(), c.iterator().next().getName());

		c = dao.getStringAnnotationDAO(null).getInRangeHaving(0, 100,
				"Tissue Type", "liver");
		Assert.assertEquals(1, c.size());
		Assert.assertEquals(d.getName(), c.iterator().next().getName());

		// get all String attributes in system
		Collection<String> allAttrs = dao.getStringAnnotationDAO(null)
				.getAttributes();
		Assert.assertEquals(new HashSet<String>(Arrays
				.asList(new String[] { "Tissue Type" })), allAttrs);

		// get all Double attributes in system
		allAttrs = dao.getDoubleAnnotationDAO(null).getAttributes();
		Assert.assertEquals(new HashSet<String>(Arrays
				.asList(new String[] { "weight" })), allAttrs);

		Map<String, Collection<String>> map = dao.getStringAnnotationDAO(id)
				.getAnnotations();
		Assert.assertEquals(1, map.size());
		Collection<String> values = map.get("Tissue Type");
		Assert.assertEquals(1, values.size());
		Assert.assertEquals("liver", values.iterator().next());
		
		// Clean up test data
		dao.delete(id);
		dao.delete(id2);		
	}

	@Test
	public void testCreateRevision() throws Exception {
		// TODO
	}

	@Test
	public void testCreateRevisionAndDelete() throws Exception {
		// TODO check that when an object is deleted so are its revisions
	}

	@Test
	public void testGetLatest() throws Exception {
		// TODO
	}

	@Test
	public void testRetrieveByRevision() throws Exception {
		// TODO
	}

}
