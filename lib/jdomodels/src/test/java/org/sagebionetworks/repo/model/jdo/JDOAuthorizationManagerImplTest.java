package org.sagebionetworks.repo.model.jdo;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.sagebionetworks.authutil.AuthUtilConstants;
import org.sagebionetworks.repo.model.AuthorizationConstants;
import org.sagebionetworks.repo.model.AuthorizationManager;
import org.sagebionetworks.repo.model.Node;
import org.sagebionetworks.repo.model.NodeDAO;
import org.sagebionetworks.repo.model.User;
import org.sagebionetworks.repo.model.UserGroup;
import org.sagebionetworks.repo.model.query.ObjectType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.orm.jdo.JdoTemplate;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath:jdomodles-test-context.xml" })
public class JDOAuthorizationManagerImplTest {

	@Autowired
	AuthorizationManager authorizationManager;
	
	@Autowired
	JdoTemplate jdoTemplate;
	
	// things to delete in the after
	List<String> toDelete = new ArrayList<String>();
	
	private static final String TEST_USER_NAME = "test-user";
	private User user = null;

	@Autowired
	private JDOUserGroupDAO userGroupDAO;
	
	@Autowired
	private NodeDAO nodeDao;
	


	
	@Before
	public void before() throws Exception {
		assertNotNull(authorizationManager);
		toDelete = new ArrayList<String>();
		populateNodesForTest();
		createTestUser();
	}
	
	private static List<String> nodeIds = new ArrayList<String>();

	
	private void populateNodesForTest() throws Exception {
		// Create a few datasets
		nodeIds = new ArrayList<String>();
		for (int i = 0; i < 5; i++) {
			Node parent = Node.createNew("dsName" + i);
			parent.setDescription("description" + i);
			parent.setCreatedBy("magic");
			parent.setNodeType("dataset");

			// Create this dataset
			String parentId = nodeDao.createNew(parent);
//			idToNameMap.put(parentId, parent.getName());
			nodeIds.add(parentId);
		}
	}
	
	@After
	public void after()throws Exception {
		// Delete all datasets
		if (nodeIds != null && nodeDao != null) {
			for (String id : nodeIds) {
				try{
					nodeDao.delete(id);
				}catch(Exception e){
				}
			}
		}


		deleteTestUser();
	}
	
	private void createTestUser() throws Exception {
		deleteTestUser();
		this.user = authorizationManager.createUser(TEST_USER_NAME);
	}
	
	private void deleteTestUser() throws Exception {
		if (user!=null) {
			authorizationManager.deleteUser(user.getUserId());
			user=null;
		}
	}
	
	@Test
	public void testAuthQueryComponent() throws Exception {
		
		String sql = authorizationManager.authorizationSQL();
		Map<String, Object> parameters = new HashMap<String,Object>();
		
		// check accessible objects from a non-existent user
		parameters.put("userName"/*.toUpperCase()*/, "foo");
		parameters.put("accessType"/*.toUpperCase()*/, AuthorizationConstants.ACCESS_TYPE.READ.name());
		JDOExecutor exec = new JDOExecutor(jdoTemplate);

		List<Object> list = exec.executeSingleCol(sql, parameters);
		
		// anything in this list must be leftovers from another test in the Public group
		// so remove them
		UserGroup publicGroup = userGroupDAO.getPublicGroup();
		for (Object o : list) {
			userGroupDAO.removeResource(publicGroup, new AuthorizableImpl(o.toString(), 
				AuthorizationManager.NODE_RESOURCE_TYPE));
		}
		
		// test that anonymous can't access any nodes
		parameters.put("userName"/*.toUpperCase()*/, AuthUtilConstants.ANONYMOUS_USER_ID);
		parameters.put("accessType"/*.toUpperCase()*/, AuthorizationConstants.ACCESS_TYPE.READ.name());
		list = exec.executeSingleCol(sql, parameters);
		assertTrue(list.toString(), list.size()==0);
		
		// there is a non-admin user, created by this test suite
		// test that the user can't access any nodes
		parameters.put("userName"/*.toUpperCase()*/, user.getUserId());
		parameters.put("accessType"/*.toUpperCase()*/, AuthorizationConstants.ACCESS_TYPE.READ.name());
		list = exec.executeSingleCol(sql, parameters);
		assertTrue(list.toString(), list.size()==0);
		
		// add access for the user
		String nodeId = nodeIds.get(0);
		Node n = nodeDao.getNode(nodeId);
		authorizationManager.addUserAccess(n, user.getUserId());
		// test that the user CAN access the node in a query
		list = exec.executeSingleCol(sql, parameters);
		assertTrue(list.toString(), list.size()==1);
		
		// test that the user CANNOT access the node with a different access type
		parameters.put("userName"/*.toUpperCase()*/, user.getUserId());
		parameters.put("accessType"/*.toUpperCase()*/, "foo");
		list = exec.executeSingleCol(sql, parameters);
		assertTrue(list.toString(), list.size()==0);

		// test that anonymous CANNOT access the node
		parameters.put("userName"/*.toUpperCase()*/, AuthUtilConstants.ANONYMOUS_USER_ID);
		parameters.put("accessType"/*.toUpperCase()*/, AuthorizationConstants.ACCESS_TYPE.READ.name());
		list = exec.executeSingleCol(sql, parameters);
		assertTrue(list.toString(), list.size()==0);

		parameters.put("accessType"/*.toUpperCase()*/, AuthorizationConstants.ACCESS_TYPE.SHARE.name());
		list = exec.executeSingleCol(sql, parameters);
		assertTrue(list.toString(), list.size()==0);

		// give public access to another node
		Node n2 = nodeDao.getNode(nodeIds.get(1));
		userGroupDAO.addResource(publicGroup, 
				new AuthorizableImpl(n2.getId(), 
						AuthorizationManager.NODE_RESOURCE_TYPE), 
						Arrays.asList(new AuthorizationConstants.ACCESS_TYPE[] {
						AuthorizationConstants.ACCESS_TYPE.READ}));
		
		// check that anonymous can access the node for 'read'...
		parameters.put("userName"/*.toUpperCase()*/, AuthUtilConstants.ANONYMOUS_USER_ID);
		parameters.put("accessType"/*.toUpperCase()*/, AuthorizationConstants.ACCESS_TYPE.READ.name());
		list = exec.executeSingleCol(sql, parameters);
		assertTrue(list.toString(), list.size()==1);
		
		// ... but not for 'change'
		parameters.put("userName"/*.toUpperCase()*/, AuthUtilConstants.ANONYMOUS_USER_ID);
		parameters.put("accessType"/*.toUpperCase()*/, AuthorizationConstants.ACCESS_TYPE.CHANGE.name());
		list = exec.executeSingleCol(sql, parameters);
		assertTrue(list.toString(), list.size()==0);
		
		// check that the user can access the node
		parameters.put("userName"/*.toUpperCase()*/, user.getUserId());
		parameters.put("accessType"/*.toUpperCase()*/, AuthorizationConstants.ACCESS_TYPE.READ.name());
		list = exec.executeSingleCol(sql, parameters);
		assertTrue(list.toString(), list.size()==2);
	}
	
//	@Test
//	public void testAdminQueryComponent() throws Exception {
//		String sql = nodeQueryDao.adminSQL();
//		Map<String, Object> parameters = new HashMap<String,Object>();
//		parameters.put("userName"/*.toUpperCase()*/, AuthUtilConstants.ANONYMOUS_USER_ID);
//		List list = exec.execute(sql, parameters);
//		assertEquals(new Integer(0), new Integer(list.get(0).toString()));
//		parameters.put("userName"/*.toUpperCase()*/, "admin");
//		list = exec.execute(sql, parameters);
//		assertEquals(new Integer(1), new Integer(list.get(0).toString()));
//		parameters.put("userName"/*.toUpperCase()*/, "undefinedUserFoo");
//		list = exec.execute(sql, parameters);
//		assertEquals(new Integer(0), new Integer(list.get(0).toString()));
//		parameters.put("userName"/*.toUpperCase()*/, user.getUserId());
//		list = exec.execute(sql, parameters);
//		assertEquals(new Integer(0), new Integer(list.get(0).toString()));
//	}
	
	

}
