package org.sagebionetworks.repo.manager;

import static org.junit.Assert.*;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.sagebionetworks.authutil.AuthUtilConstants;
import org.sagebionetworks.repo.model.AuthorizationConstants;
import org.sagebionetworks.repo.model.Node;
import org.sagebionetworks.repo.model.NodeInheritanceDAO;
import org.sagebionetworks.repo.model.User;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.query.ObjectType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath:manager-test-context.xml" })
public class NodeInheritanceManagerImplAutowireTest {
	
	@Autowired
	public NodeManager nodeManager;
	@Autowired
	public NodeInheritanceManager nodeInheritanceManager;
	@Autowired
	public NodeInheritanceDAO nodeInheritanceDao;
	// We use a mock auth DAO for this test.
	AuthorizationManager mockAuth;
	List<String> nodesToDelete;
	
	private String rootId = null;
	private String aId = null;
	private String aInheritsId = null;
	private String aInheritsChildId = null;
	private String aOverrideId = null;
	private String aOverrideChildId = null;
	private String bId = null;
	
	private UserInfo userInfo;
	
	@Before
	public void before() throws Exception{
		userInfo = new UserInfo();
		User anonUser = new User();
		anonUser.setUserId(AuthUtilConstants.ANONYMOUS_USER_ID);
		userInfo.setUser(anonUser);
		
		assertNotNull(nodeManager);
		assertNotNull(nodeInheritanceManager);
		assertNotNull(nodeInheritanceDao);
		nodesToDelete = new ArrayList<String>();
		mockAuth = Mockito.mock(AuthorizationManager.class);
		when(mockAuth.canAccess((UserInfo)any(), anyString(), any(AuthorizationConstants.ACCESS_TYPE.class))).thenReturn(true);
		when(mockAuth.canCreate((UserInfo)any(), anyString())).thenReturn(true);
		nodeManager.setAuthorizationManager(mockAuth);
		
		// For this test we need a complex hierarchy of nodes
		Node rootProject = new Node();
		rootProject.setName("root");
		rootProject.setNodeType(ObjectType.project.name());
		rootId = nodeManager.createNewNode(rootProject, userInfo);
		nodesToDelete.add(rootId);
		// This is the tree we are building
		// Root project is the root
		// Node A is a child of root.  Node A has two children
		// One child inherits from A while the other inherits from itself.
		// Create A
		// Node B is also a child of root, and a benefactor of root.
		Node node = new Node();
		node.setName("A");
		node.setNodeType(ObjectType.project.name());
		node.setParentId(rootId);
		aId = nodeManager.createNewNode(node, userInfo);
		
		// Create A.inherits
		node = new Node();
		node.setName("A.inherits");
		node.setNodeType(ObjectType.project.name());
		node.setParentId(aId);
		aInheritsId = nodeManager.createNewNode(node, userInfo);
		
		// Create A.inherits.child
		node = new Node();
		node.setName("A.inherits.child");
		node.setNodeType(ObjectType.project.name());
		node.setParentId(aInheritsId);
		aInheritsChildId = nodeManager.createNewNode(node, userInfo);
		
		// Create A.override
		node = new Node();
		node.setName("A.overrides");
		node.setNodeType(ObjectType.project.name());
		node.setParentId(aId);
		aOverrideId = nodeManager.createNewNode(node, userInfo);
		// Make sure this node inherits from itself
		nodeInheritanceDao.addBeneficiary(aOverrideId, aOverrideId);
		
		// Create A.override.child
		node = new Node();
		node.setName("A.overrides.child");
		node.setNodeType(ObjectType.project.name());
		node.setParentId(aOverrideId);
		aOverrideChildId = nodeManager.createNewNode(node, userInfo);
		
		// Create B
		node = new Node();
		node.setName("B");
		node.setNodeType(ObjectType.project.name());
		node.setParentId(rootId);
		bId = nodeManager.createNewNode(node, userInfo);
	}
	
	@After
	public void after() throws Exception {
		if(nodeManager != null && nodesToDelete != null){
			for(String id: nodesToDelete){
				try {
					nodeManager.delete(userInfo, id);
				} catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} 				
			}
		}
	}
	
	@Test
	public void testSetNodeToInheritFromItself() throws Exception{
		// Validate the starting conditions
		String benefacrorId = nodeInheritanceDao.getBenefactor(rootId);
		assertEquals(rootId, rootId);
		// Since the root already inherits from itself calling this should be
		// a no-opp.
		nodeInheritanceManager.setNodeToInheritFromItself(rootId);
		benefacrorId = nodeInheritanceDao.getBenefactor(rootId);
		assertEquals(rootId, rootId);
		
		// Make sure the rest of the nodes are as we expect
		benefacrorId = nodeInheritanceDao.getBenefactor(aId);
		assertEquals(rootId, benefacrorId);
		// A.inherits
		benefacrorId = nodeInheritanceDao.getBenefactor(aInheritsId);
		assertEquals(rootId, benefacrorId);
		// A.inherits.child
		benefacrorId = nodeInheritanceDao.getBenefactor(aInheritsChildId);
		assertEquals(rootId, benefacrorId);
		// A.override
		benefacrorId = nodeInheritanceDao.getBenefactor(aOverrideId);
		assertEquals(aOverrideId, benefacrorId);
		// A.overrride.child
		benefacrorId = nodeInheritanceDao.getBenefactor(aOverrideChildId);
		assertEquals(aOverrideId, benefacrorId);
		// B
		benefacrorId = nodeInheritanceDao.getBenefactor(bId);
		assertEquals(rootId, benefacrorId);
	
		// Now set A to override
		nodeInheritanceManager.setNodeToInheritFromItself(aId);
		// Now check the results
		// A
		benefacrorId = nodeInheritanceDao.getBenefactor(aId);
		assertEquals("A should now be its own benefacor", aId, benefacrorId);
		// A.inherits
		benefacrorId = nodeInheritanceDao.getBenefactor(aInheritsId);
		assertEquals("A.inherits should now be a beneficary of A", aId, benefacrorId);
		// A.inherits.child
		benefacrorId = nodeInheritanceDao.getBenefactor(aInheritsChildId);
		assertEquals("A.inherits.child should now be a beneficary of A", aId, benefacrorId);
		// A.override
		benefacrorId = nodeInheritanceDao.getBenefactor(aOverrideId);
		assertEquals("A.override should not have changed and should still be its own benefactor",aOverrideId, benefacrorId);
		// A.override.child
		benefacrorId = nodeInheritanceDao.getBenefactor(aOverrideChildId);
		assertEquals("A.override.child should not have changed and should still be inhertiting from its own parent",aOverrideId, benefacrorId);
		// B
		benefacrorId = nodeInheritanceDao.getBenefactor(bId);
		assertEquals("B should not have been affected by the change to a sibling.", rootId , benefacrorId);
	}
	

}
