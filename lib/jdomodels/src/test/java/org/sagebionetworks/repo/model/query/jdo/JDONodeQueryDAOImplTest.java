package org.sagebionetworks.repo.model.query.jdo;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.ConsoleHandler;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.sagebionetworks.repo.model.Annotations;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.FieldTypeDAO;
import org.sagebionetworks.repo.model.InputDataLayer.LayerTypeNames;
import org.sagebionetworks.repo.model.InvalidModelException;
import org.sagebionetworks.repo.model.Node;
import org.sagebionetworks.repo.model.NodeDAO;
import org.sagebionetworks.repo.model.NodeQueryDao;
import org.sagebionetworks.repo.model.NodeQueryResults;
import org.sagebionetworks.repo.model.jdo.JDOBootstrapperImpl;
import org.sagebionetworks.repo.model.query.BasicQuery;
import org.sagebionetworks.repo.model.query.Compartor;
import org.sagebionetworks.repo.model.query.CompoundId;
import org.sagebionetworks.repo.model.query.Expression;
import org.sagebionetworks.repo.model.query.FieldType;
import org.sagebionetworks.repo.model.query.ObjectType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 * Test for JDONodeQueryDAOImplTest.
 * 
 * @author jmhill
 * 
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath:jdomodles-test-context.xml" })
public class JDONodeQueryDAOImplTest {

	private static List<String> nodeIds = new ArrayList<String>();
	
	private static String attOnall = "onAll";
	private static String attOnEven = "onEven";
	private static String attOnOdd = "onOdd";
	private static String attString = "aStringAtt";
	private static String attDate = "aDateAtt";
	private static String attLong = "aLongAtt";
	private static String attDouble = "aDoubleAtt";
	private static String attLayerType = "layerTypeName";
	
	private static Map<String, FieldType> fieldTypeMap = new HashMap<String, FieldType>();
	static{
		fieldTypeMap.put(attOnall, FieldType.STRING_ATTRIBUTE);
		fieldTypeMap.put(attOnEven, FieldType.LONG_ATTRIBUTE);
		fieldTypeMap.put(attOnOdd, FieldType.DATE_ATTRIBUTE);
		fieldTypeMap.put(attString, FieldType.STRING_ATTRIBUTE);
		fieldTypeMap.put(attDate, FieldType.DATE_ATTRIBUTE);
		fieldTypeMap.put(attLong, FieldType.LONG_ATTRIBUTE);
		fieldTypeMap.put(attDouble, FieldType.DOUBLE_ATTRIBUTE);
		fieldTypeMap.put(attLayerType, FieldType.STRING_ATTRIBUTE);
	}
	
	private static Map<String, String> idToNameMap = new HashMap<String, String>();

	private static int totalNumberOfDatasets = 5;

	@Autowired
	private NodeQueryDao nodeQueryDao;
	
	@Autowired
	private NodeDAO nodeDao;
	
	@Autowired
	private FieldTypeDAO fieldTypeDao;


	@Before
	public void before() throws Exception {
		(new JDOBootstrapperImpl()).bootstrap(); // creat admin user, public
		// group, etc.
		// Make sure the Autowire is working
		assertNotNull(nodeQueryDao);
		assertNotNull(nodeDao);
		assertNotNull(fieldTypeDao);
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
		
		populateNodesForTest();
	}


	private void populateNodesForTest() throws DatastoreException,
			InvalidModelException {
		Iterator<String> it = fieldTypeMap.keySet().iterator();
		while(it.hasNext()){
			String key = it.next();
			FieldType type = fieldTypeMap.get(key);
			fieldTypeDao.addNewType(key, type);
		}
		
		// Create a few datasets
		nodeIds = new ArrayList<String>();
		for (int i = 0; i < totalNumberOfDatasets; i++) {
			Node parent = Node.createNew("dsName" + i);
			Date now = new Date(System.currentTimeMillis());
			parent.setDescription("description" + i);
			parent.setCreatedBy("magic");
			parent.setType(ObjectType.dataset.name());

			// Create this dataset
			String parentId = nodeDao.createNew(parent);
			idToNameMap.put(parentId, parent.getName());
			nodeIds.add(parentId);
			Annotations parentAnnos = nodeDao.getAnnotations(parentId);
			parentAnnos.addAnnotation(attOnall,
					"someNumber" + i);
			// Add some attributes to others.
			if ((i % 2) == 0) {
				parentAnnos.addAnnotation(attOnEven,
						new Long(i));
			} else {
				parentAnnos.addAnnotation(attOnOdd, now);
			}

			// Make sure we add one of each type
			parentAnnos.addAnnotation(attString,
					"someString" + i);
			parentAnnos.addAnnotation(attDate,
					new Date(System.currentTimeMillis() + i));
			parentAnnos.addAnnotation(attLong,
					new Long(123456));
			parentAnnos.addAnnotation(attDouble,
					new Double(123456.3));
			nodeDao.updateAnnotations(parentAnnos);
			
			// Add a child to the parent
			Node child = createChild(now, i);
			child.setParentId(parentId);
			// Add a layer attribute
			String childId = nodeDao.createNew(child);
			idToNameMap.put(childId, child.getName());
			Annotations childAnnos = nodeDao.getAnnotations(childId);
			childAnnos.addAnnotation("layerAnnotation", "layerAnnotValue"+i);
			
			if ((i % 2) == 0) {
				childAnnos.addAnnotation(attLayerType, LayerTypeNames.C.name());
			} else if ((i % 3) == 0) {
				childAnnos.addAnnotation(attLayerType, LayerTypeNames.E.name());
			} else {
				childAnnos.addAnnotation(attLayerType, LayerTypeNames.G.name());
			}
			
			// Update the child annoations.
			nodeDao.updateAnnotations(childAnnos);

		}
	}

	private static Node createChild(Date date, int i)
			throws InvalidModelException {
		Node ans = Node.createNew("layerName"+i);
		ans.setDescription("description"+i);
		ans.setCreatedOn(date);
		ans.setType(ObjectType.layer.name());
//		ans.setTissueType("cell line"+i);
//		ans.setPlatform("Affymetrix");
//		ans.setProcessingFacility("Broad Institute");
//		ans.setQcBy("Fred");
//		ans.setQcDate(date);
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
		if (nodeIds != null && nodeDao != null) {
			for (String id : nodeIds) {
				try{
					nodeDao.delete(id);
				}catch(Exception e){
				}
			}
		}
		if(fieldTypeMap != null && fieldTypeDao != null){
			Iterator<String> it = fieldTypeMap.keySet().iterator();
			while(it.hasNext()){
				String key = it.next();
				try{
					fieldTypeDao.delete(key);
				}catch(Exception e){
					
				}
			}
		}
	}



	// Test basic query
	@Test
	public void testBasicQuery() throws DatastoreException {
		// This query is basically "select * from datasets"
		BasicQuery query = new BasicQuery();
		query.setFrom(ObjectType.dataset);
		NodeQueryResults results = nodeQueryDao.executeQuery(query);
		assertNotNull(results);
		assertEquals(totalNumberOfDatasets, results.getTotalNumberOfResults());
		// Validate all of the data is there
		List<String> rows = results.getResultIds();
		assertNotNull(rows);
		// Each row should have each primary field
		for (String id : rows) {
			assertNotNull(id);
			// Get the node with this id
			Node node = nodeDao.getNode(id);
			assertNotNull(node);
			assertEquals(ObjectType.dataset.name(), node.getType());
			// Load the annotations for this node
			Annotations annos = nodeDao.getAnnotations(id);
			
			// Check for the annotations they all should have.
			// String
			Object annoValue = annos.getStringAnnotations().get(attString);
			assertNotNull(annoValue);
			// Date
			annoValue = annos.getDateAnnotations().get(attDate);
			assertNotNull(annoValue);
			// Long
			annoValue = annos.getLongAnnotations().get(attLong);
			assertNotNull(annoValue);
			// Double
			annoValue = annos.getDoubleAnnotations().get(attDouble);
			assertNotNull(annoValue);
		}
	}


	@Test
	public void testPaggingFromZero() throws DatastoreException {
		BasicQuery query = new BasicQuery();
		query.setFrom(ObjectType.dataset);
		query.setOffset(0);
		query.setLimit(2);
		NodeQueryResults results = nodeQueryDao.executeQuery(query);
		assertNotNull(results);
		// The total count should not change with paging
		assertEquals(totalNumberOfDatasets, results.getTotalNumberOfResults());
		List<String> rows = results.getResultIds();
		assertNotNull(rows);
		// Validate that we only have two datasets
		assertEquals(2, rows.size());
		// The two values from the middle
		String one = rows.get(0);
		assertNotNull(one);
		assertEquals("dsName0", idToNameMap.get(one));

		String two = rows.get(1);
		assertNotNull(two);
		assertEquals("dsName1", idToNameMap.get(two));
	}

	@Test
	public void testPaggingFromNonZero() throws DatastoreException {
		BasicQuery query = new BasicQuery();
		query.setFrom(ObjectType.dataset);
		query.setOffset(2);
		query.setLimit(2);
		NodeQueryResults results = nodeQueryDao.executeQuery(query);
		assertNotNull(results);
		// The total count should not change with paging
		assertEquals(totalNumberOfDatasets, results.getTotalNumberOfResults());
		List<String> rows = results.getResultIds();
		assertNotNull(rows);
		// Validate that we only have two datasets
		assertEquals(2, rows.size());
		// The two values from the middle
		String one = rows.get(0);
		assertNotNull(one);
		assertEquals("dsName2", idToNameMap.get(one));

		String two = rows.get(1);
		assertNotNull(two);
		assertEquals("dsName3", idToNameMap.get(two));
	}

	@Test
	public void testSortOnPrimaryAscending() throws DatastoreException {
		BasicQuery query = new BasicQuery();
		query.setFrom(ObjectType.dataset);
		query.setSort("name");
		query.setAscending(true);
		NodeQueryResults results = nodeQueryDao.executeQuery(query);
		assertNotNull(results);
		assertEquals(totalNumberOfDatasets, results.getTotalNumberOfResults());
		// Validate the sort
		List<String> rows = results.getResultIds();
		assertNotNull(rows);
		// Each row should have each primary field
		String previousName = null;
		String name = null;
		for (String id : rows) {
			previousName = name;
			name = idToNameMap.get(id);
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
		NodeQueryResults results = nodeQueryDao.executeQuery(query);
		assertNotNull(results);
		assertEquals(totalNumberOfDatasets, results.getTotalNumberOfResults());
		// Validate the sort
		List<String> rows = results.getResultIds();
		assertNotNull(rows);
		// Each row should have each primary field
		String previousName = null;
		String name = null;
		for (String id : rows) {
			previousName = name;
			name = (String) idToNameMap.get(id);
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
		query.setSort("createdOn");
		query.setAscending(false);
		NodeQueryResults results = nodeQueryDao.executeQuery(query);
		assertNotNull(results);
		assertEquals(totalNumberOfDatasets, results.getTotalNumberOfResults());
		// Validate the sort
		List<String> rows = results.getResultIds();
		assertNotNull(rows);
		// Each row should have each primary field
		Long previousDate = null;
		Long creation = null;
		for (String id : rows) {
			previousDate = creation;
			Node row = nodeDao.getNode(id);
			creation = row.getCreatedOn().getTime();
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
		NodeQueryResults results = nodeQueryDao.executeQuery(query);
		assertNotNull(results);
		// Sorting should not reduce the number of columns
		assertEquals(totalNumberOfDatasets, results.getTotalNumberOfResults());
		// Validate the sort
		List<String> rows = results.getResultIds();
		assertNotNull(rows);
		// Each row should have each primary field
		String previousName = null;
		String name = null;
		for (String id : rows) {
			previousName = name;
			Annotations annos = nodeDao.getAnnotations(id);
			Collection<String> collection = annos.getStringAnnotations().get(attString);
			name = collection.iterator().next();
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
				"createdBy"), Compartor.EQUALS, "magic");
		List<Expression> filters = new ArrayList<Expression>();
		filters.add(expression);
		query.setFilters(filters);
		NodeQueryResults results = nodeQueryDao.executeQuery(query);
		assertNotNull(results);
		// Every dataset should have this creator so the count should match the
		// total
		assertEquals(totalNumberOfDatasets, results.getTotalNumberOfResults());
		List<String> list = results.getResultIds();
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
				"createdOn"), Compartor.GREATER_THAN, "1");
		List<Expression> filters = new ArrayList<Expression>();
		filters.add(expression);
		query.setFilters(filters);
		NodeQueryResults results = nodeQueryDao.executeQuery(query);
		assertNotNull(results);
		// Every dataset should have this creator so the count should match the
		// total
		assertEquals(totalNumberOfDatasets, results.getTotalNumberOfResults());
		List<String> list = results.getResultIds();
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
				"createdBy"), Compartor.EQUALS, filterCreator);
		Expression expression2 = new Expression(new CompoundId("dataset",
				"name"), Compartor.EQUALS, filterName);
		filters.add(expression);
		filters.add(expression2);
		query.setFilters(filters);
		NodeQueryResults results = nodeQueryDao.executeQuery(query);
		assertNotNull(results);
		// Only one data has the name so the filter should limit to it.
		assertEquals(1, results.getTotalNumberOfResults());
		List<String> list = results.getResultIds();
		assertNotNull(list);
		assertEquals(1, list.size());
		String id = list.get(0);
		Node node = nodeDao.getNode(id);
		assertEquals(filterName, node.getName());
		assertEquals(filterCreator, node.getCreatedBy());
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
		NodeQueryResults results = nodeQueryDao.executeQuery(query);
		assertNotNull(results);
		// Every dataset should have this creator so the count should match the
		// total
		assertEquals(totalNumberOfDatasets, results.getTotalNumberOfResults());
		List<String> list = results.getResultIds();
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
				"createdBy"), Compartor.EQUALS, creator);
		filters.add(expression);
		filters.add(expression2);
		query.setFilters(filters);
		NodeQueryResults results = nodeQueryDao.executeQuery(query);
		assertNotNull(results);
		// Every dataset should have this creator so the count should match the
		// total
		assertEquals(1, results.getTotalNumberOfResults());
		List<String> list = results.getResultIds();
		assertNotNull(list);
		assertEquals(1, list.size());
		String id = list.get(0);
		Annotations annos = nodeDao.getAnnotations(id);
		Collection<String> values = annos.getStringAnnotations().get(attOnall);
		assertNotNull(values);
		assertEquals(1, values.size());
		assertEquals(onAllValue, values.iterator().next());
		Node node = nodeDao.getNode(id);
		assertNotNull(node);
		assertEquals(creator, node.getCreatedBy());
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
				"createdBy"), Compartor.EQUALS, creator);
		Expression expression3 = new Expression(new CompoundId("dataset",
				attOnEven), Compartor.EQUALS, longValue);
		Expression expression4 = new Expression(new CompoundId("dataset",
				"name"), Compartor.EQUALS, "dsName2");
		filters.add(expression);
		filters.add(expression2);
		filters.add(expression3);
		filters.add(expression4);
		query.setFilters(filters);
		NodeQueryResults results = nodeQueryDao.executeQuery(query);
		assertNotNull(results);
		// Every dataset should have this creator so the count should match the
		// total
		assertEquals(1, results.getTotalNumberOfResults());
		List<String> list = results.getResultIds();
		assertNotNull(list);
		assertEquals(1, list.size());
		String id = list.get(0);
		Annotations annos = nodeDao.getAnnotations(id);
		Collection<Long> values = annos.getLongAnnotations().get(attOnEven);
		assertNotNull(values);
		assertEquals(1, values.size());
		assertEquals(longValue, values.iterator().next());
		Node node = nodeDao.getNode(id);
		assertNotNull(node);
		assertEquals(creator, node.getCreatedBy());
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
		Expression expression = new Expression(new CompoundId(null, "parentId"), Compartor.EQUALS, nodeIds.get(1));
		filters.add(expression);
		query.setFilters(filters);
		// Execute the query.
		NodeQueryResults results = nodeQueryDao.executeQuery(query);
		assertNotNull(results);
		// There should only be one layer
		assertEquals(1, results.getTotalNumberOfResults());
		List<String> list = results.getResultIds();
		assertNotNull(list);
		assertEquals(1, list.size());
		String id = list.get(0);
		Node node = nodeDao.getNode(id);
		assertNotNull(node);
		assertEquals("layerName1", node.getName());	
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
		Long id = new Long(nodeIds.get(1));
		Expression expression = new Expression(new CompoundId(null, "parentId"), Compartor.EQUALS, id);
		filters.add(expression);
		query.setFilters(filters);
		// Execute the query.
		NodeQueryResults results = nodeQueryDao.executeQuery(query);
		assertNotNull(results);
		// There should only be one layer
		assertEquals(1, results.getTotalNumberOfResults());
		List<String> list = results.getResultIds();
		assertNotNull(list);
		assertEquals(1, list.size());
		String nodeId = list.get(0);
		Node node = nodeDao.getNode(nodeId);
		assertNotNull(node);
		assertEquals("layerName1", node.getName());	
		assertEquals(id.toString(), node.getParentId());
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
		Expression expression = new Expression(new CompoundId("dataset", "invalid name"), Compartor.EQUALS, nodeIds.get(1));
		filters.add(expression);
		query.setFilters(filters);
		// Execute the query.
		NodeQueryResults results = nodeQueryDao.executeQuery(query);
		assertNotNull(results);
		// No results should be found
		assertEquals(0, results.getTotalNumberOfResults());
		List<String> list = results.getResultIds();
		assertNotNull(list);
		assertEquals(0, list.size());
		
	}
	
}
