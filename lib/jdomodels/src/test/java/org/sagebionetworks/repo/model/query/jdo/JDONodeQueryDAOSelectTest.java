package org.sagebionetworks.repo.model.query.jdo;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.sagebionetworks.repo.model.AuthorizationConstants.BOOTSTRAP_PRINCIPAL;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.InvalidModelException;
import org.sagebionetworks.repo.model.NamedAnnotations;
import org.sagebionetworks.repo.model.Node;
import org.sagebionetworks.repo.model.NodeDAO;
import org.sagebionetworks.repo.model.NodeQueryDao;
import org.sagebionetworks.repo.model.NodeQueryResults;
import org.sagebionetworks.repo.model.User;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.jdo.KeyFactory;
import org.sagebionetworks.repo.model.jdo.NodeTestUtils;
import org.sagebionetworks.repo.model.query.BasicQuery;
import org.sagebionetworks.repo.model.query.Comparator;
import org.sagebionetworks.repo.model.query.CompoundId;
import org.sagebionetworks.repo.model.query.Expression;
import org.sagebionetworks.repo.web.NotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 * The purpose of this test is to test the various selects from a query.
 * All other query features are tested elsewhere.
 * @author jmhill
 *
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath:jdomodels-test-context.xml" })
public class JDONodeQueryDAOSelectTest {
	
	private static final String STRING_KEY_SECONDARY = "stringKeySecondary";

	private static final String STRING_KEY_PRIMARY = "stringKeyPrimary";

	@Autowired
	private NodeQueryDao nodeQueryDao;
	
	@Autowired
	private NodeDAO nodeDao;

	private UserInfo mockUserInfo = null;
	
	private static List<String> nodeIds = new ArrayList<String>();
	

	@Before
	public void before() throws Exception {
		// Make sure the Autowire is working
		assertNotNull(nodeQueryDao);
		assertNotNull(nodeDao);
		
		mockUserInfo = Mockito.mock(UserInfo.class);
		// All tests in the suite assume the user is an admin.
		when(mockUserInfo.isAdmin()).thenReturn(true);
		User mockUser = Mockito.mock(User.class);
		when(mockUser.getUserId()).thenReturn("mock@sagebase.org");
		when(mockUserInfo.getUser()).thenReturn(mockUser);
		
	}
	
	/**
	 * Cleanup
	 * 
	 * @throws Exception
	 */
	@After
	public void after() throws Exception {
		// Delete all entities
		if (nodeIds != null && nodeDao != null) {
			for (String id : nodeIds) {
				try{
					nodeDao.delete(id);
				}catch(Exception e){
				}
			}
		}
	}
	
	/**
	 * Create a single node.
	 * @param name
	 * @return
	 * @throws NotFoundException
	 * @throws DatastoreException
	 */
	private String createSingleNode(String name) throws NotFoundException,	DatastoreException, InvalidModelException  {
		Long createdBy = BOOTSTRAP_PRINCIPAL.THE_ADMIN_USER.getPrincipalId();
		Node root = NodeTestUtils.createNew(name, createdBy);
		String id = nodeDao.createNew(root);
		nodeIds.add(id);
		root = nodeDao.getNode(id);
		root.setVersionComment("Version comment");
		root.setVersionLabel("0.1.0");
		nodeDao.createNewVersion(root);
		// Add annotations to the new version only.
		NamedAnnotations annos = nodeDao.getAnnotations(id);
		annos.getPrimaryAnnotations().addAnnotation(STRING_KEY_PRIMARY, "string value");
		annos.getAdditionalAnnotations().addAnnotation(STRING_KEY_SECONDARY, "string value 2");
		nodeDao.updateAnnotations(id, annos);
		return id;
	}
	
	/**
	 * 
	 */
	@Test
	public void testSelectStar() throws Exception{
		String id = createSingleNode("testSelectStar");
		// Now query for this node
		BasicQuery query = new BasicQuery();
		// We want to select across all tables
		query.setFrom(null);
		// A null select should be treated as a select *
		query.setSelect(null);
		query.addExpression(new Expression(new CompoundId(null, "id"), Comparator.EQUALS, KeyFactory.stringToKey(id)));
		NodeQueryResults results = nodeQueryDao.executeQuery(query, mockUserInfo);
		assertNotNull(results);
		assertEquals(1, results.getTotalNumberOfResults());
		assertNotNull(results.getAllSelectedData());
		assertEquals(1, results.getAllSelectedData().size());
		// Make sure the map contains all of the expected values
		Map<String, Object> row = results.getAllSelectedData().get(0);
		// The map should contain all primary fields.
		for(NodeField field: NodeField.values()){
			assertTrue(field.name(), row.containsKey(field.getFieldName()));
		}
		// The map should also have all of the annotations.
		assertTrue(row.containsKey(STRING_KEY_SECONDARY));
		assertTrue(row.containsKey(STRING_KEY_PRIMARY));
	}
	
	/**
	 * 
	 */
	@Test
	public void testSelectId() throws Exception{
		String id = createSingleNode("testSelectId");
		// Now query for this node
		BasicQuery query = new BasicQuery();
		// We want to select across all tables
		query.setFrom(null);
		// A null select should be treated as a select *
		query.setSelect(new ArrayList<String>());
		query.getSelect().add(NodeField.ID.getFieldName());
		query.addExpression(new Expression(new CompoundId(null, "id"), Comparator.EQUALS, KeyFactory.stringToKey(id)));
		NodeQueryResults results = nodeQueryDao.executeQuery(query, mockUserInfo);
		assertNotNull(results);
		assertEquals(1, results.getTotalNumberOfResults());
		assertNotNull(results.getAllSelectedData());
		assertEquals(1, results.getAllSelectedData().size());
		// Make sure the map contains all of the expected values
		Map<String, Object> row = results.getAllSelectedData().get(0);
		assertEquals(1, row.size());
		// The map should contain all primary fields.
		// It should only have a single value
		String fetchedId = (String) row.get(NodeField.ID.getFieldName());
		assertEquals(id, fetchedId);
	}
	
	/**
	 * 
	 */
	@Test
	public void testSelectAnnotaion() throws Exception{
		String id = createSingleNode("testSelectAnnotaion");
		// Now query for this node
		BasicQuery query = new BasicQuery();
		// We want to select across all tables
		query.setFrom(null);
		// A null select should be treated as a select *
		query.setSelect(new ArrayList<String>());
		query.getSelect().add(STRING_KEY_PRIMARY);
		query.addExpression(new Expression(new CompoundId(null, "id"), Comparator.EQUALS, KeyFactory.stringToKey(id)));
		NodeQueryResults results = nodeQueryDao.executeQuery(query, mockUserInfo);
		assertNotNull(results);
		assertEquals(1, results.getTotalNumberOfResults());
		assertNotNull(results.getAllSelectedData());
		assertEquals(1, results.getAllSelectedData().size());
		// Make sure the map contains all of the expected values
		Map<String, Object> row = results.getAllSelectedData().get(0);
		System.out.println(row);
		assertEquals(2, row.size());
		// Get the value
		Object annoValue = row.get(STRING_KEY_PRIMARY);
		assertNotNull(annoValue);
	}
	
	
	/**
	 * 
	 */
	@Test
	public void testSelectAllNodeValues() throws Exception{
		String id = createSingleNode("testSelectAllNodeValues");
		// Now query for this node
		BasicQuery query = new BasicQuery();
		// We want to select across all tables
		query.setFrom(null);
		// A null select should be treated as a select
		List<String> select  = new ArrayList<String>();
		for(NodeField value: NodeField.values()){
			select.add(value.getFieldName());
		}
		query.setSelect(select);
		query.addExpression(new Expression(new CompoundId(null, "id"), Comparator.EQUALS, KeyFactory.stringToKey(id)));
		NodeQueryResults results = nodeQueryDao.executeQuery(query, mockUserInfo);
		assertNotNull(results);
		assertEquals(1, results.getTotalNumberOfResults());
		assertNotNull(results.getAllSelectedData());
		assertEquals(1, results.getAllSelectedData().size());
		// Make sure the map contains all of the expected values
		Map<String, Object> row = results.getAllSelectedData().get(0);
		assertEquals(select.size(), row.size());
		for(String name: select){
			assertTrue(row.containsKey(name));
		}
	}
	
	@Test
	public void testFilterNull() throws Exception {
		BasicQuery query = new BasicQuery();
		query.setFrom(null);
		List<Expression> filters = new ArrayList<Expression>();
		Expression expression = new Expression(new CompoundId(null, NodeField.PARENT_ID.getFieldName()), Comparator.EQUALS, null);
		filters.add(expression);
		query.setFilters(filters);
		long count = nodeQueryDao.executeCountQuery(query, mockUserInfo);
		assertTrue(count > 0);
		NodeQueryResults results = nodeQueryDao.executeQuery(query, mockUserInfo);
		assertNotNull(results);
		assertTrue(results.getTotalNumberOfResults() > 0);
	}
	

}
