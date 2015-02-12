package org.sagebionetworks.repo.model.query.jdo;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.sagebionetworks.evaluation.model.Evaluation;
import org.sagebionetworks.repo.model.Annotations;
import org.sagebionetworks.repo.model.AsynchronousDAO;
import org.sagebionetworks.repo.model.AuthorizationConstants.BOOTSTRAP_PRINCIPAL;
import org.sagebionetworks.repo.model.ACCESS_TYPE;
import org.sagebionetworks.repo.model.AccessControlList;
import org.sagebionetworks.repo.model.AccessControlListDAO;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.EntityType;
import org.sagebionetworks.repo.model.InvalidModelException;
import org.sagebionetworks.repo.model.NamedAnnotations;
import org.sagebionetworks.repo.model.Node;
import org.sagebionetworks.repo.model.NodeConstants;
import org.sagebionetworks.repo.model.NodeDAO;
import org.sagebionetworks.repo.model.NodeQueryDao;
import org.sagebionetworks.repo.model.NodeQueryResults;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.ResourceAccess;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.jdo.KeyFactory;
import org.sagebionetworks.repo.model.jdo.NodeTestUtils;
import org.sagebionetworks.repo.model.query.BasicQuery;
import org.sagebionetworks.repo.model.query.Comparator;
import org.sagebionetworks.repo.model.query.CompoundId;
import org.sagebionetworks.repo.model.query.Expression;
import org.sagebionetworks.repo.model.query.FieldType;
import org.sagebionetworks.repo.web.NotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.google.common.collect.Sets;

/**
 * Test for JDONodeQueryDAOImplTest.
 *  * @author jmhill
 * 
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath:jdomodels-test-context.xml" })
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
	private AsynchronousDAO asynchronousDAO;
	
	@Autowired
	private NodeDAO nodeDao;
	
	@Autowired
	private AccessControlListDAO aclDAO;

	private AccessControlList aclToDelete = null;

	private UserInfo mockAdminUserInfo = null;
	private UserInfo mockUserInfo = null;
	
	private Long createdBy = null;
	
	@Before
	public void before() throws Exception {
		// Make sure the Autowire is working
		assertNotNull(nodeQueryDao);
		assertNotNull(nodeDao);
		
		mockAdminUserInfo = Mockito.mock(UserInfo.class);
		// Most tests in the suite assume the user is an admin.
		when(mockAdminUserInfo.isAdmin()).thenReturn(true);

		mockUserInfo = Mockito.mock(UserInfo.class);
		// All tests in the suite assume the user is an admin.
		when(mockUserInfo.isAdmin()).thenReturn(false);
		when(mockUserInfo.getGroups()).thenReturn(Sets.newHashSet(BOOTSTRAP_PRINCIPAL.ANONYMOUS_USER.getPrincipalId()));

		createdBy = BOOTSTRAP_PRINCIPAL.THE_ADMIN_USER.getPrincipalId();
		
		populateNodesForTest();
	}

	private void populateNodesForTest() throws Exception {
		Iterator<String> it = fieldTypeMap.keySet().iterator();
		while(it.hasNext()){
			String key = it.next();
			FieldType type = fieldTypeMap.get(key);
		}
		
		// Create a few datasets
		List<String> idsToUpdate = new LinkedList<String>();
		nodeIds = new ArrayList<String>();
		for (int i = 0; i < totalNumberOfDatasets; i++) {
			Node parent = NodeTestUtils.createNew("dsName" + i, createdBy);
			Date now = new Date(System.currentTimeMillis());
			parent.setDescription("description" + i);
			parent.setCreatedByPrincipalId(createdBy);
			parent.setNodeType(EntityType.dataset.name());

			// Create this dataset
			String parentId = nodeDao.createNew(parent);
			idToNameMap.put(parentId, parent.getName());
			nodeIds.add(parentId);
			idsToUpdate.add(parentId);
			NamedAnnotations named = nodeDao.getAnnotations(parentId);
			Annotations parentAnnos = named.getAdditionalAnnotations();
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
			nodeDao.updateAnnotations(parentId, named);
			
			// Add a child to the parent
			Node child = createChild(now, i, createdBy);
			child.setParentId(parentId);
			// Add a layer attribute
			String childId = nodeDao.createNew(child);
			idToNameMap.put(childId, child.getName());
			idsToUpdate.add(childId);
			NamedAnnotations childNamed = nodeDao.getAnnotations(childId);
			Annotations childAnnos = childNamed.getPrimaryAnnotations();
			childAnnos.addAnnotation("layerAnnotation", "layerAnnotValue"+i);
			
			if ((i % 2) == 0) {
				childAnnos.addAnnotation(attLayerType, LayerTypeNames.C.name());
			} else if ((i % 3) == 0) {
				childAnnos.addAnnotation(attLayerType, LayerTypeNames.E.name());
			} else {
				childAnnos.addAnnotation(attLayerType, LayerTypeNames.G.name());
			}
			
			// Update the child annoations.
			nodeDao.updateAnnotations(childId, childNamed);

		}
		// since we have moved the annotation updates to an asynchronous process we need to manually
		// update the annotations of all nodes for this test. See PLFM-1548
		for(String id: idsToUpdate){
			asynchronousDAO.createEntity(id);
		}
	}
	
	// this was formerly defined in the (now defunct) Layer class
    public enum LayerTypeNames {                E, G, C;        }

	private static Node createChild(Date date, int i, Long createdByPrincipalId)
			throws InvalidModelException {
		Node ans = NodeTestUtils.createNew("layerName"+i, createdByPrincipalId);
		ans.setDescription("description"+i);
		ans.setCreatedOn(date);
		ans.setNodeType(EntityType.layer.name());
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
		if (aclToDelete != null) {
			aclDAO.delete(aclToDelete.getId(), ObjectType.ENTITY);
			aclToDelete = null;
		}
		if (nodeIds != null && nodeDao != null) {
			for (String id : nodeIds) {
				try{
					nodeDao.delete(id);
				}catch(Exception e){
				}
			}
		}
	}
	

	// Test basic query
	@Test
	public void testBasicQuery() throws Exception {
		// This query is basically "select * from datasets"
		BasicQuery query = new BasicQuery();
		query.setFrom(EntityType.dataset.name());
		NodeQueryResults results = nodeQueryDao.executeQuery(query, mockAdminUserInfo);
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
			assertEquals(EntityType.dataset.name(), node.getNodeType());
			// Load the annotations for this node
			NamedAnnotations named = nodeDao.getAnnotations(id);
			Annotations annos = named.getAdditionalAnnotations();
			
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

	// Test basic query
	@Test
	public void testBasicQueryWithAuthorization() throws Exception {
		AccessControlList acl = new AccessControlList();
		acl.setId(nodeIds.get(2));
		acl.setCreationDate(new Date());
		Set<ResourceAccess> ras = new HashSet<ResourceAccess>();
		ResourceAccess ra = new ResourceAccess();
		ra.setPrincipalId(BOOTSTRAP_PRINCIPAL.ANONYMOUS_USER.getPrincipalId());
		ra.setAccessType(new HashSet<ACCESS_TYPE>(Arrays.asList(new ACCESS_TYPE[] { ACCESS_TYPE.READ })));
		ras.add(ra);
		acl.setResourceAccess(ras);
		String aclId = aclDAO.create(acl, ObjectType.ENTITY);
		acl.setId(aclId);
		aclToDelete = acl;

		// This query is basically "select * from datasets"
		BasicQuery query = new BasicQuery();
		query.setFrom(EntityType.dataset.name());
		NodeQueryResults results = nodeQueryDao.executeQuery(query, mockUserInfo);
		assertNotNull(results);
		assertEquals(1, results.getTotalNumberOfResults());
		// Validate all of the data is there
		List<String> rows = results.getResultIds();
		assertNotNull(rows);
		// Each row should have each primary field
		String id = rows.get(0);
		assertNotNull(id);
		// Get the node with this id
		Node node = nodeDao.getNode(id);
		assertNotNull(node);
		assertEquals(EntityType.dataset.name(), node.getNodeType());
		// Load the annotations for this node
		NamedAnnotations named = nodeDao.getAnnotations(id);
		Annotations annos = named.getAdditionalAnnotations();

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

	@Test
	public void testBasicQueryOnChildren() throws Exception {
		// This query is basically "select * from datasets"
		BasicQuery query = new BasicQuery();
		query.setFrom(EntityType.layer.name());
		NodeQueryResults results = nodeQueryDao.executeQuery(query, mockAdminUserInfo);
		assertNotNull(results);
		assertEquals(totalNumberOfDatasets, results.getTotalNumberOfResults());
		// Validate all of the data is there
		int numRows = results.getResultIds().size();
		assertTrue(0 < numRows);
		// Each row should have each primary field
		for (int i = 0 ; i < numRows; i++) {
			String id = results.getResultIds().get(i);
			Map<String, Object> row = results.getAllSelectedData().get(i);
			
			assertNotNull(id);
			// Get the node with this id
			Node node = nodeDao.getNode(id);
			assertNotNull(node);
			assertEquals(EntityType.layer.name(), node.getNodeType());
			
			// Make sure ids in query results have the syn prefix too
			assertEquals(node.getId(), row.get(NodeField.ID.name()));
			assertEquals(node.getParentId(), row.get(NodeField.PARENT_ID.getFieldName()));
		}
	}

	@Test
	public void testPagingFromZero() throws DatastoreException {
		BasicQuery query = new BasicQuery();
		query.setFrom(EntityType.dataset.name());
		query.setOffset(0);
		query.setLimit(2);
		NodeQueryResults results = nodeQueryDao.executeQuery(query, mockAdminUserInfo);
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
	public void testPagingFromNonZero() throws DatastoreException {
		BasicQuery query = new BasicQuery();
		query.setFrom(EntityType.dataset.name());
		query.setOffset(2);
		query.setLimit(2);
		NodeQueryResults results = nodeQueryDao.executeQuery(query, mockAdminUserInfo);
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
		query.setFrom(EntityType.dataset.name());
		query.setSort("name");
		query.setAscending(true);
		NodeQueryResults results = nodeQueryDao.executeQuery(query, mockAdminUserInfo);
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
		query.setFrom(EntityType.dataset.name());
		query.setSort("name");
		query.setAscending(false);
		NodeQueryResults results = nodeQueryDao.executeQuery(query, mockAdminUserInfo);
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
	public void testSortOnPrimaryDate() throws Exception {
		BasicQuery query = new BasicQuery();
		query.setFrom(EntityType.dataset.name());
		query.setSort("createdOn");
		query.setAscending(false);
		NodeQueryResults results = nodeQueryDao.executeQuery(query, mockAdminUserInfo);
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
	
	@Test
	public void testSortOnModifedOn() throws Exception {
		BasicQuery query = new BasicQuery();
		query.setFrom(EntityType.dataset.name());
		query.setSort(NodeField.MODIFIED_ON.getFieldName());
		query.setAscending(false);
		NodeQueryResults results = nodeQueryDao.executeQuery(query, mockAdminUserInfo);
		assertNotNull(results);
		assertEquals(totalNumberOfDatasets, results.getTotalNumberOfResults());
		// Validate the sort
		List<String> rows = results.getResultIds();
		assertNotNull(rows);
		// Each row should have each primary field
		Long previousDate = null;
		Long modified = null;
		for (String id : rows) {
			previousDate = modified;
			Node row = nodeDao.getNode(id);
			modified = row.getModifiedOn().getTime();
			System.out.println(modified);
			if (previousDate != null) {
				assertTrue(previousDate.compareTo(modified) >= 0);
			}
		}
	}

	// Sorting on a string attribute
	@Test
	public void testSortOnStringAttribute() throws Exception {
		BasicQuery query = new BasicQuery();
		query.setFrom(EntityType.dataset.name());
		query.setSort(attString);
		query.setAscending(false);
		NodeQueryResults results = nodeQueryDao.executeQuery(query, mockAdminUserInfo);
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
			NamedAnnotations named = nodeDao.getAnnotations(id);
			Annotations annos = named.getAdditionalAnnotations();
			Collection<String> collection = annos.getStringAnnotations().get(attString);
			name = collection.iterator().next();
			System.out.println(name);
			if (previousName != null) {
				assertTrue(previousName.compareTo(name) > 0);
			}
		}
	}

	@Test
	public void testFilterOnSinglePrimary() throws Exception {
		BasicQuery query = new BasicQuery();
		query.setFrom(EntityType.dataset.name());
		// query.setSort(attString);
		// query.setAscending(false);
		Expression expression = new Expression(new CompoundId("dataset",
				"createdByPrincipalId"), Comparator.EQUALS, createdBy);
		List<Expression> filters = new ArrayList<Expression>();
		filters.add(expression);
		query.setFilters(filters);
		NodeQueryResults results = nodeQueryDao.executeQuery(query, mockAdminUserInfo);
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
		query.setFrom(EntityType.dataset.name());
		// query.setSort(attString);
		// query.setAscending(false);
		Expression expression = new Expression(new CompoundId("dataset",
				"createdOn"), Comparator.GREATER_THAN, "1");
		List<Expression> filters = new ArrayList<Expression>();
		filters.add(expression);
		query.setFilters(filters);
		NodeQueryResults results = nodeQueryDao.executeQuery(query, mockAdminUserInfo);
		assertNotNull(results);
		// Every dataset should have this creator so the count should match the
		// total
		assertEquals(totalNumberOfDatasets, results.getTotalNumberOfResults());
		List<String> list = results.getResultIds();
		assertNotNull(list);
		assertEquals(totalNumberOfDatasets, list.size());
	}

	@Test
	public void testFilterOnMultiplePrimary() throws Exception {
		BasicQuery query = new BasicQuery();
		query.setFrom(EntityType.dataset.name());
		List<Expression> filters = new ArrayList<Expression>();
		Long filterCreator = createdBy;
		String filterName = "dsName0";
		Expression expression = new Expression(new CompoundId("dataset",
				"createdByPrincipalId"), Comparator.EQUALS, filterCreator);
		Expression expression2 = new Expression(new CompoundId("dataset",
				"name"), Comparator.EQUALS, filterName);
		filters.add(expression);
		filters.add(expression2);
		query.setFilters(filters);
		NodeQueryResults results = nodeQueryDao.executeQuery(query, mockAdminUserInfo);
		assertNotNull(results);
		// Only one data has the name so the filter should limit to it.
		assertEquals(1, results.getTotalNumberOfResults());
		List<String> list = results.getResultIds();
		assertNotNull(list);
		assertEquals(1, list.size());
		String id = list.get(0);
		Node node = nodeDao.getNode(id);
		assertEquals(filterName, node.getName());
		assertEquals(filterCreator, node.getCreatedByPrincipalId());
	}

	@Test
	public void testFilterOnSingleAttribute() throws Exception {
		BasicQuery query = new BasicQuery();
		query.setFrom(EntityType.dataset.name());
		query.setSort(attOnOdd);
		query.setAscending(false);
		// Filter on an annotation using does not equal with a bogus value to
		// get all datasets.
		Expression expression = new Expression(new CompoundId("dataset",
				attOnall), Comparator.NOT_EQUALS, "I do not exist");
		List<Expression> filters = new ArrayList<Expression>();
		filters.add(expression);
		query.setFilters(filters);
		NodeQueryResults results = nodeQueryDao.executeQuery(query, mockAdminUserInfo);
		assertNotNull(results);
		// Every dataset should have this creator so the count should match the
		// total
		assertEquals(2, results.getTotalNumberOfResults());
		List<String> list = results.getResultIds();
		assertNotNull(list);
		assertEquals(2, list.size());
	}

	@Test
	public void testFilterOnSingleAttributeAndSinglePrimary()
			throws Exception {
		BasicQuery query = new BasicQuery();
		query.setFrom(EntityType.dataset.name());
		query.setSort(attOnall);
		query.setAscending(false);
		List<Expression> filters = new ArrayList<Expression>();
		// Filter on an annotation using does not equal with a bogus value to
		// get all datasets.
		String onAllValue = "someNumber2";
		Long creator = createdBy;
		Expression expression = new Expression(new CompoundId("dataset",
				attOnall), Comparator.EQUALS, onAllValue);
		Expression expression2 = new Expression(new CompoundId("dataset",
				"createdByPrincipalId"), Comparator.EQUALS, creator);
		filters.add(expression);
		filters.add(expression2);
		query.setFilters(filters);
		NodeQueryResults results = nodeQueryDao.executeQuery(query, mockAdminUserInfo);
		assertNotNull(results);
		// Every dataset should have this creator so the count should match the
		// total
		assertEquals(1, results.getTotalNumberOfResults());
		List<String> list = results.getResultIds();
		assertNotNull(list);
		assertEquals(1, list.size());
		String id = list.get(0);
		NamedAnnotations named = nodeDao.getAnnotations(id);
		Annotations annos = named.getAdditionalAnnotations();
		Collection<String> values = annos.getStringAnnotations().get(attOnall);
		assertNotNull(values);
		assertEquals(1, values.size());
		assertEquals(onAllValue, values.iterator().next());
		Node node = nodeDao.getNode(id);
		assertNotNull(node);
		assertEquals(creator, node.getCreatedByPrincipalId());
	}

	@Test
	public void testFilterMultiple() throws Exception {
		BasicQuery query = new BasicQuery();
		query.setFrom(EntityType.dataset.name());
		query.setSort(attOnall);
		query.setAscending(true);
		query.setLimit(3);
		query.setOffset(0);
		List<Expression> filters = new ArrayList<Expression>();
		// Filter on an annotation using does not equal with a bogus value to
		// get all datasets.
		String onAllValue = "someNumber2";
		Long creator = createdBy;
		Long longValue = new Long(2);
		Expression expression = new Expression(new CompoundId("dataset",
				attOnall), Comparator.EQUALS, onAllValue);
		Expression expression2 = new Expression(new CompoundId("dataset",
				"createdByPrincipalId"), Comparator.EQUALS, creator);
		Expression expression3 = new Expression(new CompoundId("dataset",
				attOnEven), Comparator.EQUALS, longValue);
		Expression expression4 = new Expression(new CompoundId("dataset",
				"name"), Comparator.EQUALS, "dsName2");
		filters.add(expression);
		filters.add(expression2);
		filters.add(expression3);
		filters.add(expression4);
		query.setFilters(filters);
		NodeQueryResults results = nodeQueryDao.executeQuery(query, mockAdminUserInfo);
		assertNotNull(results);
		// Every dataset should have this creator so the count should match the
		// total
		assertEquals(1, results.getTotalNumberOfResults());
		List<String> list = results.getResultIds();
		assertNotNull(list);
		assertEquals(1, list.size());
		String id = list.get(0);
		NamedAnnotations named = nodeDao.getAnnotations(id);
		Annotations annos = named.getAdditionalAnnotations();
		Collection<Long> values = annos.getLongAnnotations().get(attOnEven);
		assertNotNull(values);
		assertEquals(1, values.size());
		assertEquals(longValue, values.iterator().next());
		Node node = nodeDao.getNode(id);
		assertNotNull(node);
		assertEquals(creator, node.getCreatedByPrincipalId());
	}
	
	@Test
	public void testLayerQueryStringId() throws Exception{
		BasicQuery query = new BasicQuery();
		query.setFrom(EntityType.layer.name());
		query.setSort("name");
		query.setAscending(true);
		query.setLimit(3);
		query.setOffset(0);
		List<Expression> filters = new ArrayList<Expression>();
		Expression expression = new Expression(new CompoundId(null, NodeConstants.COL_PARENT_ID), Comparator.EQUALS, KeyFactory.stringToKey(nodeIds.get(1)));
		filters.add(expression);
		query.setFilters(filters);
		// Execute the query.
		NodeQueryResults results = nodeQueryDao.executeQuery(query, mockAdminUserInfo);
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
	public void testLayerQueryNumericId() throws Exception{
		BasicQuery query = new BasicQuery();
		query.setFrom(EntityType.layer.name());
		query.setSort("name");
		query.setAscending(true);
		query.setLimit(3);
		query.setOffset(0);
		List<Expression> filters = new ArrayList<Expression>();
		Long id = KeyFactory.stringToKey(nodeIds.get(1));
		Expression expression = new Expression(new CompoundId(null, NodeConstants.COL_PARENT_ID), Comparator.EQUALS, id);
		filters.add(expression);
		query.setFilters(filters);
		// Execute the query.
		NodeQueryResults results = nodeQueryDao.executeQuery(query, mockAdminUserInfo);
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
		assertEquals(KeyFactory.keyToString(id), node.getParentId());
	}
	
	@Test
	public void testInvalidAttributeName() throws DatastoreException{
		BasicQuery query = new BasicQuery();
		query.setFrom(EntityType.dataset.name());
		query.setSort("name");
		query.setAscending(true);
		query.setLimit(3);
		query.setOffset(0);
		List<Expression> filters = new ArrayList<Expression>();
		Expression expression = new Expression(new CompoundId("dataset", "invalid name"), Comparator.EQUALS, nodeIds.get(1));
		filters.add(expression);
		query.setFilters(filters);
		// Execute the query.
		NodeQueryResults results = nodeQueryDao.executeQuery(query, mockAdminUserInfo);
		assertNotNull(results);
		// No results should be found
		assertEquals(0, results.getTotalNumberOfResults());
		List<String> list = results.getResultIds();
		assertNotNull(list);
		assertEquals(0, list.size());
		
	}
	
	@Test
	public void testExecuteCountQuery() throws DatastoreException{
		BasicQuery query = new BasicQuery();
		query.setFrom(EntityType.layer.name());
		List<Expression> filters = new ArrayList<Expression>();
		Expression expression = new Expression(new CompoundId("layer", attLayerType), Comparator.EQUALS, LayerTypeNames.C.name());
		filters.add(expression);
		query.setFilters(filters);
		long count = nodeQueryDao.executeCountQuery(query, mockAdminUserInfo);
		assertEquals(3, count);
		// Try the next
		query = new BasicQuery();
		query.setFrom(EntityType.layer.name());
		filters = new ArrayList<Expression>();
		expression = new Expression(new CompoundId("layer", attLayerType), Comparator.EQUALS, LayerTypeNames.G.name());
		filters.add(expression);
		query.setFilters(filters);
		count = nodeQueryDao.executeCountQuery(query, mockAdminUserInfo);
		assertEquals(1, count);
		// Try the next
		query = new BasicQuery();
		query.setFrom(EntityType.layer.name());
		filters = new ArrayList<Expression>();
		expression = new Expression(new CompoundId("layer", attLayerType), Comparator.EQUALS, LayerTypeNames.E.name());
		filters.add(expression);
		query.setFilters(filters);
		count = nodeQueryDao.executeCountQuery(query, mockAdminUserInfo);
		assertEquals(1, count);
	}
	@Test
	public void testExecuteCountQueryNonExistant() throws DatastoreException{
		// Value does not exist
		BasicQuery query = new BasicQuery();
		query.setFrom(EntityType.layer.name());
		List<Expression> filters = new ArrayList<Expression>();
		Expression expression = new Expression(new CompoundId("layer", attLayerType), Comparator.EQUALS, "i do not exists");
		filters.add(expression);
		query.setFilters(filters);
		long count = nodeQueryDao.executeCountQuery(query, mockAdminUserInfo);
		assertEquals(0, count);
		
		// Key does not exist
		query = new BasicQuery();
		query.setFrom(EntityType.layer.name());
		filters = new ArrayList<Expression>();
		expression = new Expression(new CompoundId("layer", "someFakeKeyThatDoesNotExist"), Comparator.EQUALS, LayerTypeNames.E.name());
		filters.add(expression);
		query.setFilters(filters);
		count = nodeQueryDao.executeCountQuery(query, mockAdminUserInfo);
		assertEquals(0, count);
	}
	
	@Test
	public void testAliasQuery() throws DatastoreException{
		// Value does not exist
		BasicQuery query = new BasicQuery();
		// This should select all entites.
		query.setFrom("entity");
		long count = nodeQueryDao.executeCountQuery(query, mockAdminUserInfo);
		assertTrue(count >= totalNumberOfDatasets*2);
		
		// try the dataset alias
		query = new BasicQuery();
		query.setFrom("versionable");
		count = nodeQueryDao.executeCountQuery(query, mockAdminUserInfo);
		assertTrue(count >= totalNumberOfDatasets*2);
		
	}
	
	/**
	 * This is a test for PLFM-1542
	 * @throws NotFoundException 
	 * @throws InvalidModelException 
	 * @throws DatastoreException 
	 */
	@Test
	public void testFilterBenefactorId() throws DatastoreException, InvalidModelException, NotFoundException{
		// Add several layers of hierarchy with the same benefactor
		Node parent = NodeTestUtils.createNew("parentPLFM-1542", createdBy);
		Date now = new Date(System.currentTimeMillis());
		parent.setDescription("description");
		parent.setCreatedByPrincipalId(createdBy);
		parent.setNodeType(EntityType.project.name());

		// Create a parent
		String rootParentId = nodeDao.createNew(parent);
		nodeIds.add(rootParentId);
		List<Map<String, Object>> expected = new LinkedList<Map<String,Object>>();
		Map<String, Object> map = new HashMap<String, Object>();
		map.put("id", rootParentId);
		expected.add(map);
		// Add some hierarchy
		int numberLevels = 4;
		String thisParentId = rootParentId;
		for(int i=0; i<numberLevels; i++){
			Node child = NodeTestUtils.createNew("child-"+i, createdBy);
			child.setParentId(thisParentId);
			child.setNodeType(EntityType.dataset.name());
			thisParentId = nodeDao.createNew(child);
			// Add it to the expected list
			 map = new HashMap<String, Object>();
			 map.put("id", thisParentId);
			 expected.add(map);
		}
		// Query for all entites that 
		BasicQuery query = new BasicQuery();
		query.setFrom("entity");
		query.setSelect(new LinkedList<String>());
		query.getSelect().add("id");
		query.addExpression(new Expression(new CompoundId(null, NodeConstants.COL_BENEFACTOR_ID), Comparator.EQUALS, rootParentId));
		NodeQueryResults result = nodeQueryDao.executeQuery(query, mockAdminUserInfo);
		assertNotNull(result);
		assertEquals(expected.size(), result.getTotalNumberOfResults());
		assertEquals(expected, result.getAllSelectedData());
	}

	
}
