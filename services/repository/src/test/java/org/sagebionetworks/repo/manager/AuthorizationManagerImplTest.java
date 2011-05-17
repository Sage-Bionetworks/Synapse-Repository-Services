package org.sagebionetworks.repo.manager;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.sagebionetworks.repo.model.AuthorizationConstants;
import org.sagebionetworks.repo.model.GroupPermissionsDAO;
import org.sagebionetworks.repo.model.Node;
import org.sagebionetworks.repo.model.NodeDAO;
import org.sagebionetworks.repo.model.UserGroup;
import org.sagebionetworks.repo.model.jdo.JDOExecutor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.orm.jdo.JdoTemplate;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath:manager-test-context.xml" })
public class AuthorizationManagerImplTest {

	@Autowired
	AuthorizationManager authorizationManager;
	
	@Autowired
	JdoTemplate jdoTemplate;
	
	// things to delete in the after
	List<String> toDelete = new ArrayList<String>();
	
//	private static final String TEST_USER_NAME = "test-user";
	
	private Collection<String> groups = new ArrayList<String>();

	@Autowired
	private GroupPermissionsDAO groupPermissionsDAO;
	
	@Autowired
	private NodeDAO nodeDao;
	
	@Before
	public void before() throws Exception {
		assertNotNull(authorizationManager);
		toDelete = new ArrayList<String>();
		populateNodesForTest();
		createTestGroups();
	}
	
	private static List<String> nodeIds = new ArrayList<String>();

	
	private void populateNodesForTest() throws Exception {
		// Create a few datasets
		nodeIds = new ArrayList<String>();
		for (int i = 0; i < 5; i++) {
			Node parent = createNew("dsName" + i);
			parent.setDescription("description" + i);
			parent.setCreatedBy("magic");
			parent.setNodeType("dataset");

			// Create this dataset
			String parentId = nodeDao.createNew(parent);
//			idToNameMap.put(parentId, parent.getName());
			nodeIds.add(parentId);
		}
	}
	
	public Node createNew(String name){
		Node node = new Node();
		node.setName(name);
		node.setCreatedBy("anonymous");
		node.setModifiedBy("anonymous");
		node.setCreatedOn(new Date(System.currentTimeMillis()));
		node.setModifiedOn(node.getCreatedOn());
		node.setNodeType("unknown");
		return node;
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
		deleteTestGroups();
	}
	
	private void createTestGroups() throws Exception {
		deleteTestGroups();
		Map<String,UserGroup> gs = groupPermissionsDAO.getGroupsByNames(Arrays.asList(new String[]{"test-group"}));
		UserGroup g = null;
		if (gs.isEmpty()) {
			g = new UserGroup();
			g.setName("test-group");
			groupPermissionsDAO.create(g);
		} else {
			g = gs.get("test-group");
		}
		groups.add(g.getId());
	}
	
	private void deleteTestGroups() throws Exception {
		for (String gid : groups) {
			groupPermissionsDAO.delete(gid);
		}
	}
	
	@Test
	public void testAuthQueryComponent() throws Exception {
		
		String sql = null;
		Map<String, Object> parameters = new HashMap<String,Object>();
		
		// check accessible objects from a non-existent user
		parameters.put("groupIdList", new ArrayList<String>());
		parameters.put("accessType", AuthorizationConstants.ACCESS_TYPE.READ);
		sql = groupPermissionsDAO.authorizationSQL((AuthorizationConstants.ACCESS_TYPE)parameters.get("accessType"), (List<String>)parameters.get("groupIdList"));
		JDOExecutor exec = new JDOExecutor(jdoTemplate);

		List<Object> list = exec.executeSingleCol(sql);
		
		// anything in this list must be leftovers from another test in the Public group
		// so remove them
		UserGroup publicGroup = groupPermissionsDAO.getPublicGroup();
		for (Object o : list) {
			groupPermissionsDAO.removeResource(publicGroup, o.toString());
		}
		
		// test that anonymous can't access any nodes
		Collection<String> justPublic = Arrays.asList(new String[]{groupPermissionsDAO.getPublicGroup().getId()});
		parameters.put("groupIdList", justPublic);
		parameters.put("accessType", AuthorizationConstants.ACCESS_TYPE.READ);
		sql = groupPermissionsDAO.authorizationSQL((AuthorizationConstants.ACCESS_TYPE)parameters.get("accessType"), (List<String>)parameters.get("groupIdList"));
		list = exec.executeSingleCol(sql);
		assertTrue(list.toString(), list.size()==0);
		
		// there is a non-admin user, created by this test suite
		// test that the user can't access any nodes
		Collection<String> userGroups = new ArrayList<String>(groups);
		userGroups.add(groupPermissionsDAO.getPublicGroup().getId());
		parameters.put("groupIdList", userGroups);
		parameters.put("accessType", AuthorizationConstants.ACCESS_TYPE.READ);
		sql = groupPermissionsDAO.authorizationSQL((AuthorizationConstants.ACCESS_TYPE)parameters.get("accessType"), (List<String>)parameters.get("groupIdList"));
		list = exec.executeSingleCol(sql);
		assertTrue(list.toString(), list.size()==0);
		
		// add access for a group
		String nodeId = nodeIds.get(0);
//		Node n = nodeDao.getNode(nodeId);
		UserGroup g = groupPermissionsDAO.get(groups.iterator().next());
		groupPermissionsDAO.addResource(g, nodeId, 
				Arrays.asList(new AuthorizationConstants.ACCESS_TYPE[]{
						AuthorizationConstants.ACCESS_TYPE.READ
				}));
//		// test that the user CAN access the node in a query
		sql = groupPermissionsDAO.authorizationSQL((AuthorizationConstants.ACCESS_TYPE)parameters.get("accessType"), (List<String>)parameters.get("groupIdList"));
		list = exec.executeSingleCol(sql);
		assertTrue(list.toString(), list.size()==1);
		
		// test that the user CANNOT access the node with a different access type
		parameters.put("groupIdList", userGroups);
		parameters.put("accessType", AuthorizationConstants.ACCESS_TYPE.SHARE);
		sql = groupPermissionsDAO.authorizationSQL((AuthorizationConstants.ACCESS_TYPE)parameters.get("accessType"), (List<String>)parameters.get("groupIdList"));
		list = exec.executeSingleCol(sql);
		assertTrue(list.toString(), list.size()==0);

		// test that anonymous CANNOT access the node
		parameters.put("groupIdList", justPublic);
		parameters.put("accessType", AuthorizationConstants.ACCESS_TYPE.READ);
		sql = groupPermissionsDAO.authorizationSQL((AuthorizationConstants.ACCESS_TYPE)parameters.get("accessType"), (List<String>)parameters.get("groupIdList"));
		list = exec.executeSingleCol(sql);
		assertTrue(list.toString(), list.size()==0);

		parameters.put("accessType", AuthorizationConstants.ACCESS_TYPE.SHARE);
		sql = groupPermissionsDAO.authorizationSQL((AuthorizationConstants.ACCESS_TYPE)parameters.get("accessType"), (List<String>)parameters.get("groupIdList"));
		list = exec.executeSingleCol(sql);
		assertTrue(list.toString(), list.size()==0);

		// give public access to another node
		Node n2 = nodeDao.getNode(nodeIds.get(1));
		groupPermissionsDAO.addResource(publicGroup, 
				n2.getId(), 
						Arrays.asList(new AuthorizationConstants.ACCESS_TYPE[] {
						AuthorizationConstants.ACCESS_TYPE.READ}));
		
		// check that anonymous can access the node for 'read'...
		parameters.put("groupIdList", justPublic);
		parameters.put("accessType", AuthorizationConstants.ACCESS_TYPE.READ);
		sql = groupPermissionsDAO.authorizationSQL((AuthorizationConstants.ACCESS_TYPE)parameters.get("accessType"), (List<String>)parameters.get("groupIdList"));
		list = exec.executeSingleCol(sql);
		assertTrue(list.toString(), list.size()==1);
		
		// ... but not for 'change'
		parameters.put("groupIdList", justPublic);
		parameters.put("accessType", AuthorizationConstants.ACCESS_TYPE.CHANGE);
		sql = groupPermissionsDAO.authorizationSQL((AuthorizationConstants.ACCESS_TYPE)parameters.get("accessType"), (List<String>)parameters.get("groupIdList"));
		list = exec.executeSingleCol(sql);
		assertTrue(list.toString(), list.size()==0);
		
		// check that the user can access the node
		parameters.put("groupIdList", userGroups);
		parameters.put("accessType", AuthorizationConstants.ACCESS_TYPE.READ);
		sql = groupPermissionsDAO.authorizationSQL((AuthorizationConstants.ACCESS_TYPE)parameters.get("accessType"), (List<String>)parameters.get("groupIdList"));
		list = exec.executeSingleCol(sql);
		assertTrue(list.toString(), list.size()==2);
	}
	
	
	

}
