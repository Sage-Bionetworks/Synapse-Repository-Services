package org.sagebionetworks.repo.model.query.jdo;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.ConsoleHandler;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.jws.Oneway;

import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.sagebionetworks.repo.model.DAOFactory;
import org.sagebionetworks.repo.model.Dataset;
import org.sagebionetworks.repo.model.DatasetDAO;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.InputDataLayer;
import org.sagebionetworks.repo.model.InvalidModelException;
import org.sagebionetworks.repo.model.InputDataLayer.LayerTypeNames;
import org.sagebionetworks.repo.model.QueryResults;
import org.sagebionetworks.repo.model.jdo.JDOBootstrapperImpl;
import org.sagebionetworks.repo.model.jdo.JDODAOFactoryImpl;
import org.sagebionetworks.repo.model.jdo.persistence.JDODataset;
import org.sagebionetworks.repo.model.jdo.persistence.JDODateAnnotation;
import org.sagebionetworks.repo.model.jdo.persistence.JDODoubleAnnotation;
import org.sagebionetworks.repo.model.jdo.persistence.JDOInputDataLayer;
import org.sagebionetworks.repo.model.jdo.persistence.JDOLongAnnotation;
import org.sagebionetworks.repo.model.jdo.persistence.JDOStringAnnotation;
import org.sagebionetworks.repo.model.query.BasicQuery;
import org.sagebionetworks.repo.model.query.Compartor;
import org.sagebionetworks.repo.model.query.CompoundId;
import org.sagebionetworks.repo.model.query.Expression;
import org.sagebionetworks.repo.model.query.FieldType;
import org.sagebionetworks.repo.model.query.ObjectType;

/**
 * Test for JDOQueryDAOImpl.
 * 
 * @author jmhill
 * 
 */
public class JDOQueryDAOImplTest {

	private static DAOFactory daoFactory;

	private List<String> datasetIds = new ArrayList<String>();
	private List<String> layerIds = new ArrayList<String>();

	private String attOnall = "onAll";
	private String attOnEven = "onEven";
	private String attOnOdd = "onOdd";
	private String attString = "aStringAtt";
	private String attDate = "aDateAtt";
	private String attLong = "aLongAtt";
	private String attDouble = "aDoubleAtt";

	private int totalNumberOfDatasets = 5;

	private static JDOQueryDAOImpl queryDao;

	Collection<String> primaryFields;;

	@BeforeClass
	public static void beforeClass() throws Exception {
		// from
		// http://groups.google.com/group/google-appengine-java/browse_thread/thread/96baed75e3c30a58/00d5afb2e0445882?lnk=gst&q=DataNucleus+plugin#00d5afb2e0445882
		// This one caused all the WARNING and SEVERE logs about eclipse UI
		// elements
		Logger.getLogger("DataNucleus.Plugin").setLevel(Level.OFF);
		// This one logged the last couple INFOs about Persistence configuration
		Logger.getLogger("DataNucleus.Persistence").setLevel(Level.WARNING);

		Logger.getLogger("DataNucleus.Query").setLevel(Level.INFO);
		Logger.getLogger("DataNucleus.JDO").setLevel(Level.ALL);
		// Turn on logging for the dao.
		Logger.getLogger(JDOQueryDAOImpl.class.getName()).setLevel(Level.ALL);
		ConsoleHandler conHandler = new ConsoleHandler();
		Logger.getLogger(JDOQueryDAOImpl.class.getName())
				.addHandler(conHandler);
		Handler[] handlers = Logger.getLogger(JDOQueryDAOImpl.class.getName())
				.getHandlers();
		// Create some datasets
		daoFactory = new JDODAOFactoryImpl();

		// The query dao
		queryDao = new JDOQueryDAOImpl();

	}

	@Before
	public void before() throws Exception {
		(new JDOBootstrapperImpl()).bootstrap(); // creat admin user, public
													// group, etc.
		// Create a few datasets
		datasetIds = new ArrayList<String>();
		DatasetDAO dao = daoFactory.getDatasetDAO(null);
		primaryFields = dao.getPrimaryFields();
		for (int i = 0; i < totalNumberOfDatasets; i++) {
			Dataset ds = new Dataset();
			ds.setName("dsName" + i);
			Date now = new Date(System.currentTimeMillis());
			ds.setCreationDate(now);
			ds.setDescription("description" + i);
			ds.setCreator("magic");
			ds.setEtag("someETag" + i);
			if ((i % 2) == 0) {
				ds.setStatus("Started");
			} else {
				ds.setStatus("Completed");
			}
			ds.setReleaseDate(now);
			ds.setVersion("1.0." + i);

			// Create this dataset
			String id = dao.create(ds);
			datasetIds.add(id);
			// Add a layer to the dataset
			InputDataLayer layer = createLayer(now, i);
			// Add a layer attribute
			String layerId = dao.getInputDataLayerDAO(id).create(layer);
			dao.getInputDataLayerDAO(id).getStringAnnotationDAO(layerId).addAnnotation("layerAnnotation", "layerAnnotValue"+i);

			// add this attribute to all datasets
			dao.getStringAnnotationDAO(id).addAnnotation(attOnall,
					"someNumber" + i);
			// Add some attributes to others.
			if ((i % 2) == 0) {
				dao.getLongAnnotationDAO(id).addAnnotation(attOnEven,
						new Long(i));
			} else {
				dao.getDateAnnotationDAO(id).addAnnotation(attOnOdd, now);
			}

			// Make sure we add one of each type
			dao.getStringAnnotationDAO(id).addAnnotation(attString,
					"someString" + i);
			dao.getDateAnnotationDAO(id).addAnnotation(attDate,
					new Date(System.currentTimeMillis() + i));
			dao.getLongAnnotationDAO(id).addAnnotation(attLong,
					new Long(123456));
			dao.getDoubleAnnotationDAO(id).addAnnotation(attDouble,
					new Double(123456.3));
		}
	}

	private static InputDataLayer createLayer(Date date, int i)
			throws InvalidModelException {
		InputDataLayer ans = new InputDataLayer();
		ans.setName("layerName"+i);
		ans.setDescription("description"+i);
		ans.setCreationDate(date);
		ans.setVersion("1.0");
		ans.setPublicationDate(date);
		ans.setReleaseNotes("this version contains important revisions"+i);
		if ((i % 2) == 0) {
			ans.setType(LayerTypeNames.C.name());
		} else if ((i % 3) == 0) {
			ans.setType(LayerTypeNames.E.name());
		} else {
			ans.setType(LayerTypeNames.G.name());
		}
		ans.setTissueType("cell line"+i);
		ans.setPlatform("Affymetrix");
		ans.setProcessingFacility("Broad Institute");
		ans.setQcBy("Fred");
		ans.setQcDate(date);
		return ans;
	}

	/**
	 * Cleanup
	 * 
	 * @throws Exception
	 */
	@After
	public void after() throws Exception {
		// Delete all datasets
		if (datasetIds != null && daoFactory != null) {
			DatasetDAO dao = daoFactory.getDatasetDAO(null);
			for (String id : datasetIds) {
				dao.delete(id);
			}
		}
	}

	@Test
	public void testGetTableNameForClass() {
		// Test each class that we expect to use
		// Dataset
		Class clazz = JDODataset.class;
		String tableName = queryDao.getTableNameForClass(clazz);
		System.out.println("Table name for: " + clazz.getName() + " = "
				+ tableName);
		assertNotNull(tableName);

		// String annotation.
		clazz = JDOStringAnnotation.class;
		tableName = queryDao.getTableNameForClass(clazz);
		System.out.println("Table name for: " + clazz.getName() + " = "
				+ tableName);
		assertNotNull(tableName);

		// Long annotation.
		clazz = JDOLongAnnotation.class;
		tableName = queryDao.getTableNameForClass(clazz);
		System.out.println("Table name for: " + clazz.getName() + " = "
				+ tableName);
		assertNotNull(tableName);

		// Date annotation.
		clazz = JDODateAnnotation.class;
		tableName = queryDao.getTableNameForClass(clazz);
		System.out.println("Table name for: " + clazz.getName() + " = "
				+ tableName);
		assertNotNull(tableName);

		// Double annotation.
		clazz = JDODoubleAnnotation.class;
		tableName = queryDao.getTableNameForClass(clazz);
		System.out.println("Table name for: " + clazz.getName() + " = "
				+ tableName);
		assertNotNull(tableName);

		// Layer.
		clazz = JDOInputDataLayer.class;
		tableName = queryDao.getTableNameForClass(clazz);
		System.out.println("Table name for: " + clazz.getName() + " = "
				+ tableName);
		assertNotNull(tableName);
	}

	@Test
	public void testGetTableNameForObjectType() {
		// There should be a table name for each object type
		ObjectType[] all = ObjectType.values();
		for (ObjectType type : all) {
			String tableName = queryDao.getTableNameForObjectType(type);
			assertNotNull(tableName);
			System.out.println("Table Name for: " + type.name() + " = "
					+ tableName);
		}
	}

	@Test
	public void testGetTableNameForFieldType() {
		// Only test for the types that are valid
		// String
		String tableName = queryDao
				.getTableNameForFieldType(FieldType.STRING_ATTRIBUTE);
		assertNotNull(tableName);
		// Date
		tableName = queryDao.getTableNameForFieldType(FieldType.DATE_ATTRIBUTE);
		assertNotNull(tableName);
		// Long
		tableName = queryDao.getTableNameForFieldType(FieldType.LONG_ATTRIBUTE);
		assertNotNull(tableName);
		// Double
		tableName = queryDao
				.getTableNameForFieldType(FieldType.DOUBLE_ATTRIBUTE);
		assertNotNull(tableName);
	}

	/**
	 * Each primary field should show as primary.
	 * 
	 * @throws DatastoreException
	 */
	@Test
	public void testGetFieldTypePrimary() throws DatastoreException {
		DatasetDAO dao = daoFactory.getDatasetDAO(null);
		Collection<String> primary = dao.getPrimaryFields();
		Iterator<String> it = primary.iterator();
		while (it.hasNext()) {
			String key = it.next();
			FieldType type = queryDao.getFieldType(key);
			assertNotNull(type);
			assertEquals(FieldType.PRIMARY_FIELD, type);
		}
	}

	@Test
	public void testDoesNotExist() throws DatastoreException {
		FieldType type = queryDao.getFieldType("I do not exist");
		assertNotNull(type);
		assertEquals(FieldType.DOES_NOT_EXIST, type);

		type = queryDao.getFieldType("Nor Do I");
		assertNotNull(type);
		assertEquals(FieldType.DOES_NOT_EXIST, type);
	}

	// Test each type
	@Test
	public void testEachType() throws DatastoreException {
		// String
		FieldType type = queryDao.getFieldType(attString);
		assertEquals(FieldType.STRING_ATTRIBUTE, type);

		// Date
		type = queryDao.getFieldType(attDate);
		assertEquals(FieldType.DATE_ATTRIBUTE, type);

		// Long
		type = queryDao.getFieldType(attLong);
		assertEquals(FieldType.LONG_ATTRIBUTE, type);

		// Double
		type = queryDao.getFieldType(attDouble);
		assertEquals(FieldType.DOUBLE_ATTRIBUTE, type);
	}

	// Test basic query
	@Test
	public void testBasicQuery() throws DatastoreException {
		// This query is basically "select * from datasets"
		BasicQuery query = new BasicQuery();
		query.setFrom(ObjectType.dataset);
		QueryResults results = queryDao.executeQuery(query);
		assertNotNull(results);
		assertEquals(totalNumberOfDatasets, results.getTotalNumberOfResults());
		// Validate all of the data is there
		List<Map<String, Object>> rows = results.getResults();
		assertNotNull(rows);
		// Each row should have each primary field
		for (Map<String, Object> row : rows) {
			assertNotNull(row);
			// Check each primary
			Iterator<String> it = primaryFields.iterator();
			while (it.hasNext()) {
				String primaryFieldName = it.next();
				Object value = row.get(primaryFieldName);
				assertNotNull("Null value for: " + primaryFieldName, value);
			}

			// Check for the annotations they all should have.
			// String
			Object annoValue = row.get(attString);
			assertNotNull(annoValue);
			// Date
			annoValue = row.get(attDate);
			assertNotNull(annoValue);
			// Long
			annoValue = row.get(attLong);
			assertNotNull(annoValue);
			// Double
			annoValue = row.get(attDouble);
			assertNotNull(annoValue);
		}
	}


	@Test
	public void testPaggingFromZero() throws DatastoreException {
		BasicQuery query = new BasicQuery();
		query.setFrom(ObjectType.dataset);
		query.setOffset(0);
		query.setLimit(2);
		QueryResults results = queryDao.executeQuery(query);
		assertNotNull(results);
		// The total count should not change with paging
		assertEquals(totalNumberOfDatasets, results.getTotalNumberOfResults());
		List<Map<String, Object>> rows = results.getResults();
		assertNotNull(rows);
		// Validate that we only have two datasets
		assertEquals(2, rows.size());
		// The two values from the middle
		Map<String, Object> one = rows.get(0);
		assertNotNull(one);
		assertEquals("dsName0", one.get("name"));

		Map<String, Object> two = rows.get(1);
		assertNotNull(two);
		assertEquals("dsName1", two.get("name"));
	}

	@Test
	public void testPaggingFromNonZero() throws DatastoreException {
		BasicQuery query = new BasicQuery();
		query.setFrom(ObjectType.dataset);
		query.setOffset(2);
		query.setLimit(2);
		QueryResults results = queryDao.executeQuery(query);
		assertNotNull(results);
		// The total count should not change with paging
		assertEquals(totalNumberOfDatasets, results.getTotalNumberOfResults());
		List<Map<String, Object>> rows = results.getResults();
		assertNotNull(rows);
		// Validate that we only have two datasets
		assertEquals(2, rows.size());
		// The two values from the middle
		Map<String, Object> one = rows.get(0);
		assertNotNull(one);
		assertEquals("dsName2", one.get("name"));

		Map<String, Object> two = rows.get(1);
		assertNotNull(two);
		assertEquals("dsName3", two.get("name"));
	}

	@Test
	public void testSortOnPrimaryAscending() throws DatastoreException {
		BasicQuery query = new BasicQuery();
		query.setFrom(ObjectType.dataset);
		query.setSort("name");
		query.setAscending(true);
		QueryResults results = queryDao.executeQuery(query);
		assertNotNull(results);
		assertEquals(totalNumberOfDatasets, results.getTotalNumberOfResults());
		// Validate the sort
		List<Map<String, Object>> rows = results.getResults();
		assertNotNull(rows);
		// Each row should have each primary field
		String previousName = null;
		String name = null;
		for (Map<String, Object> row : rows) {
			previousName = name;
			name = (String) row.get("name");
			System.out.println(name);
			if (previousName != null) {
				assertTrue(previousName.compareTo(name) < 0);
			}
		}
	}

	@Test
	public void testSortOnPrimaryDecending() throws DatastoreException {
		BasicQuery query = new BasicQuery();
		query.setFrom(ObjectType.dataset);
		query.setSort("name");
		query.setAscending(false);
		QueryResults results = queryDao.executeQuery(query);
		assertNotNull(results);
		assertEquals(totalNumberOfDatasets, results.getTotalNumberOfResults());
		// Validate the sort
		List<Map<String, Object>> rows = results.getResults();
		assertNotNull(rows);
		// Each row should have each primary field
		String previousName = null;
		String name = null;
		for (Map<String, Object> row : rows) {
			previousName = name;
			name = (String) row.get("name");
			System.out.println(name);
			if (previousName != null) {
				assertTrue(previousName.compareTo(name) > 0);
			}
		}
	}
	
	/**
	 * This is a test for bug http://sagebionetworks.jira.com/browse/PLFM-111
	 * @throws DatastoreException
	 */
	@Test
	public void testSortOnPrimaryDate() throws DatastoreException {
		BasicQuery query = new BasicQuery();
		query.setFrom(ObjectType.dataset);
		query.setSort("creationDate");
		query.setAscending(false);
		QueryResults results = queryDao.executeQuery(query);
		assertNotNull(results);
		assertEquals(totalNumberOfDatasets, results.getTotalNumberOfResults());
		// Validate the sort
		List<Map<String, Object>> rows = results.getResults();
		assertNotNull(rows);
		// Each row should have each primary field
		Long previousDate = null;
		Long creation = null;
		for (Map<String, Object> row : rows) {
			previousDate = creation;
			creation = (Long) row.get("creationDate");
			System.out.println(creation);
			if (previousDate != null) {
				assertTrue(previousDate.compareTo(creation) >= 0);
			}
		}
	}

	// Sorting on a string attribute
	@Test
	public void testSortOnStringAttribute() throws DatastoreException {
		BasicQuery query = new BasicQuery();
		query.setFrom(ObjectType.dataset);
		query.setSort(attString);
		query.setAscending(false);
		QueryResults results = queryDao.executeQuery(query);
		assertNotNull(results);
		// Sorting should not reduce the number of columns
		assertEquals(totalNumberOfDatasets, results.getTotalNumberOfResults());
		// Validate the sort
		List<Map<String, Object>> rows = results.getResults();
		assertNotNull(rows);
		// Each row should have each primary field
		String previousName = null;
		String name = null;
		for (Map<String, Object> row : rows) {
			previousName = name;
			Set<String> valueSet = (Set<String>) row.get(attString);
			name = valueSet.iterator().next();
			System.out.println(name);
			if (previousName != null) {
				assertTrue(previousName.compareTo(name) > 0);
			}
		}
	}

	@Test
	public void testFilterOnSinglePrimary() throws DatastoreException {
		BasicQuery query = new BasicQuery();
		query.setFrom(ObjectType.dataset);
		// query.setSort(attString);
		// query.setAscending(false);
		Expression expression = new Expression(new CompoundId("dataset",
				"creator"), Compartor.EQUALS, "magic");
		List<Expression> filters = new ArrayList<Expression>();
		filters.add(expression);
		query.setFilters(filters);
		QueryResults results = queryDao.executeQuery(query);
		assertNotNull(results);
		// Every dataset should have this creator so the count should match the
		// total
		assertEquals(totalNumberOfDatasets, results.getTotalNumberOfResults());
		List<Map<String, Object>> list = results.getResults();
		assertNotNull(list);
		assertEquals(totalNumberOfDatasets, list.size());
	}
	
	@Test
	public void testFilterOnSinglePrimaryDate() throws DatastoreException {
		BasicQuery query = new BasicQuery();
		query.setFrom(ObjectType.dataset);
		// query.setSort(attString);
		// query.setAscending(false);
		Expression expression = new Expression(new CompoundId("dataset",
				"creationDate"), Compartor.GREATER_THAN, "1");
		List<Expression> filters = new ArrayList<Expression>();
		filters.add(expression);
		query.setFilters(filters);
		QueryResults results = queryDao.executeQuery(query);
		assertNotNull(results);
		// Every dataset should have this creator so the count should match the
		// total
		assertEquals(totalNumberOfDatasets, results.getTotalNumberOfResults());
		List<Map<String, Object>> list = results.getResults();
		assertNotNull(list);
		assertEquals(totalNumberOfDatasets, list.size());
	}

	@Test
	public void testFilterOnMultiplePrimary() throws DatastoreException {
		BasicQuery query = new BasicQuery();
		query.setFrom(ObjectType.dataset);
		List<Expression> filters = new ArrayList<Expression>();
		String filterCreator = "magic";
		String filterName = "dsName0";
		Expression expression = new Expression(new CompoundId("dataset",
				"creator"), Compartor.EQUALS, filterCreator);
		Expression expression2 = new Expression(new CompoundId("dataset",
				"name"), Compartor.EQUALS, filterName);
		filters.add(expression);
		filters.add(expression2);
		query.setFilters(filters);
		QueryResults results = queryDao.executeQuery(query);
		assertNotNull(results);
		// Only one data has the name so the filter should limit to it.
		assertEquals(1, results.getTotalNumberOfResults());
		List<Map<String, Object>> list = results.getResults();
		assertNotNull(list);
		assertEquals(1, list.size());
		Map<String, Object> row = list.get(0);
		assertEquals(filterName, row.get("name"));
		assertEquals(filterCreator, row.get("creator"));
	}

	@Test
	public void testFilterOnSingleAttribute() throws DatastoreException {
		BasicQuery query = new BasicQuery();
		query.setFrom(ObjectType.dataset);
		query.setSort(attOnOdd);
		query.setAscending(false);
		// Filter on an annotation using does not equal with a bogus value to
		// get all datasets.
		Expression expression = new Expression(new CompoundId("dataset",
				attOnall), Compartor.NOT_EQUALS, "I do not exist");
		List<Expression> filters = new ArrayList<Expression>();
		filters.add(expression);
		query.setFilters(filters);
		QueryResults results = queryDao.executeQuery(query);
		assertNotNull(results);
		// Every dataset should have this creator so the count should match the
		// total
		assertEquals(totalNumberOfDatasets, results.getTotalNumberOfResults());
		List<Map<String, Object>> list = results.getResults();
		assertNotNull(list);
		assertEquals(totalNumberOfDatasets, list.size());
	}

	@Test
	public void testFilterOnSingleAttributeAndSinglePrimary()
			throws DatastoreException {
		BasicQuery query = new BasicQuery();
		query.setFrom(ObjectType.dataset);
		query.setSort(attOnOdd);
		query.setAscending(false);
		List<Expression> filters = new ArrayList<Expression>();
		// Filter on an annotation using does not equal with a bogus value to
		// get all datasets.
		String onAllValue = "someNumber2";
		String creator = "magic";
		Expression expression = new Expression(new CompoundId("dataset",
				attOnall), Compartor.EQUALS, onAllValue);
		Expression expression2 = new Expression(new CompoundId("dataset",
				"creator"), Compartor.EQUALS, creator);
		filters.add(expression);
		filters.add(expression2);
		query.setFilters(filters);
		QueryResults results = queryDao.executeQuery(query);
		assertNotNull(results);
		// Every dataset should have this creator so the count should match the
		// total
		assertEquals(1, results.getTotalNumberOfResults());
		List<Map<String, Object>> list = results.getResults();
		assertNotNull(list);
		assertEquals(1, list.size());
		Map<String, Object> row = list.get(0);
		Set<String> values = (Set<String>) row.get(attOnall);
		assertNotNull(values);
		assertEquals(1, values.size());
		assertEquals(onAllValue, values.iterator().next());
		assertEquals(creator, row.get("creator"));
	}

	@Test
	public void testFilterMultiple() throws DatastoreException {
		BasicQuery query = new BasicQuery();
		query.setFrom(ObjectType.dataset);
		query.setSort(attOnOdd);
		query.setAscending(true);
		query.setLimit(3);
		query.setOffset(0);
		List<Expression> filters = new ArrayList<Expression>();
		// Filter on an annotation using does not equal with a bogus value to
		// get all datasets.
		String onAllValue = "someNumber2";
		String creator = "magic";
		Long longValue = new Long(2);
		Expression expression = new Expression(new CompoundId("dataset",
				attOnall), Compartor.EQUALS, onAllValue);
		Expression expression2 = new Expression(new CompoundId("dataset",
				"creator"), Compartor.EQUALS, creator);
		Expression expression3 = new Expression(new CompoundId("dataset",
				attOnEven), Compartor.EQUALS, longValue);
		Expression expression4 = new Expression(new CompoundId("dataset",
				"name"), Compartor.EQUALS, "dsName2");
		filters.add(expression);
		filters.add(expression2);
		filters.add(expression3);
		filters.add(expression4);
		query.setFilters(filters);
		QueryResults results = queryDao.executeQuery(query);
		assertNotNull(results);
		// Every dataset should have this creator so the count should match the
		// total
		assertEquals(1, results.getTotalNumberOfResults());
		List<Map<String, Object>> list = results.getResults();
		assertNotNull(list);
		assertEquals(1, list.size());
		Map<String, Object> row = list.get(0);
		Set<Long> values = (Set<Long>) row.get(attOnEven);
		assertNotNull(values);
		assertEquals(1, values.size());
		assertEquals(longValue, values.iterator().next());
		assertEquals(creator, row.get("creator"));
	}
	
	@Test
	public void testLayerQueryStringId() throws DatastoreException{
		BasicQuery query = new BasicQuery();
		query.setFrom(ObjectType.layer);
		query.setSort("name");
		query.setAscending(true);
		query.setLimit(3);
		query.setOffset(0);
		List<Expression> filters = new ArrayList<Expression>();
		Expression expression = new Expression(new CompoundId("dataset", "id"), Compartor.EQUALS, datasetIds.get(1));
		filters.add(expression);
		query.setFilters(filters);
		// Execute the query.
		QueryResults results = queryDao.executeQuery(query);
		assertNotNull(results);
		// There should only be one layer
		assertEquals(1, results.getTotalNumberOfResults());
		List<Map<String, Object>> list = results.getResults();
		assertNotNull(list);
		assertEquals(1, list.size());
		Map<String, Object> row = list.get(0);
		assertEquals("layerName1", row.get("name"));	
	}

	@Test
	public void testLayerQueryNumericId() throws DatastoreException{
		BasicQuery query = new BasicQuery();
		query.setFrom(ObjectType.layer);
		query.setSort("name");
		query.setAscending(true);
		query.setLimit(3);
		query.setOffset(0);
		List<Expression> filters = new ArrayList<Expression>();
		Long id = new Long(datasetIds.get(1));
		Expression expression = new Expression(new CompoundId("dataset", "id"), Compartor.EQUALS, id);
		filters.add(expression);
		query.setFilters(filters);
		// Execute the query.
		QueryResults results = queryDao.executeQuery(query);
		assertNotNull(results);
		// There should only be one layer
		assertEquals(1, results.getTotalNumberOfResults());
		List<Map<String, Object>> list = results.getResults();
		assertNotNull(list);
		assertEquals(1, list.size());
		Map<String, Object> row = list.get(0);
		assertEquals("layerName1", row.get("name"));	
	}
	
	@Test
	public void testInvalidAttributeName() throws DatastoreException{
		BasicQuery query = new BasicQuery();
		query.setFrom(ObjectType.dataset);
		query.setSort("name");
		query.setAscending(true);
		query.setLimit(3);
		query.setOffset(0);
		List<Expression> filters = new ArrayList<Expression>();
		Expression expression = new Expression(new CompoundId("dataset", "invalid name"), Compartor.EQUALS, datasetIds.get(1));
		filters.add(expression);
		query.setFilters(filters);
		// Execute the query.
		QueryResults results = queryDao.executeQuery(query);
		assertNotNull(results);
		// No results should be found
		assertEquals(0, results.getTotalNumberOfResults());
		List<Map<String, Object>> list = results.getResults();
		assertNotNull(list);
		assertEquals(0, list.size());
		
	}

	
}
