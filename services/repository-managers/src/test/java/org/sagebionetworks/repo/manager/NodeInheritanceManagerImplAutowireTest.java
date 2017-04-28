package org.sagebionetworks.repo.manager;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.sagebionetworks.repo.manager.trash.TrashManager;
import org.sagebionetworks.repo.model.AuthorizationConstants.BOOTSTRAP_PRINCIPAL;
import org.sagebionetworks.repo.model.EntityType;
import org.sagebionetworks.repo.model.Node;
import org.sagebionetworks.repo.model.NodeInheritanceDAO;
import org.sagebionetworks.repo.model.UserInfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath:test-context.xml" })
public class NodeInheritanceManagerImplAutowireTest {
	
	@Autowired
	private NodeManager nodeManager;
	
	@Autowired
	private NodeInheritanceManager nodeInheritanceManager;
	
	@Autowired
	private NodeInheritanceDAO nodeInheritanceDao;
	
	@Autowired
	private UserManager userManager;
	
	@Autowired
	TrashManager trashManager;

	private List<String> nodesToDelete;
	private String rootId = null;
	private String aId = null;
	private String aInheritsId = null;
	private String aInheritsChildId = null;
	private String aOverrideId = null;
	private String aOverrideChildId = null;
	private String bId = null;
	
	private UserInfo adminUserInfo;
	
	//projectOne is the root for the parentId change tests, need to hold it's id
	private String projectOneId = null;
	
	@Before
	public void before() throws Exception {
		adminUserInfo = userManager.getUserInfo(BOOTSTRAP_PRINCIPAL.THE_ADMIN_USER.getPrincipalId());
		assertNotNull(nodeManager);
		assertNotNull(nodeInheritanceManager);
		assertNotNull(nodeInheritanceDao);
		nodesToDelete = new ArrayList<String>();
		
		// For this test we need a complex hierarchy of nodes
		Node rootProject = new Node();
		rootProject.setName("root");
		rootProject.setNodeType(EntityType.project);
		rootId = nodeManager.createNewNode(rootProject, adminUserInfo);
		nodesToDelete.add(rootId);
		// This is the tree we are building
		// Root project is the root
		// Node A is a child of root.  Node A has two children
		// One child inherits from A while the other inherits from itself.
		// Create A
		// Node B is also a child of root, and a benefactor of root.
		Node node = new Node();
		node.setName("A");
		node.setNodeType(EntityType.project);
		node.setParentId(rootId);
		aId = nodeManager.createNewNode(node, adminUserInfo);
		
		// Create A.inherits
		node = new Node();
		node.setName("A.inherits");
		node.setNodeType(EntityType.project);
		node.setParentId(aId);
		aInheritsId = nodeManager.createNewNode(node, adminUserInfo);
		
		// Create A.inherits.child
		node = new Node();
		node.setName("A.inherits.child");
		node.setNodeType(EntityType.project);
		node.setParentId(aInheritsId);
		aInheritsChildId = nodeManager.createNewNode(node, adminUserInfo);
		
		// Create A.override
		node = new Node();
		node.setName("A.overrides");
		node.setNodeType(EntityType.project);
		node.setParentId(aId);
		aOverrideId = nodeManager.createNewNode(node, adminUserInfo);
		// Make sure this node inherits from itself
		nodeInheritanceDao.addBeneficiary(aOverrideId, aOverrideId);
		
		// Create A.override.child
		node = new Node();
		node.setName("A.overrides.child");
		node.setNodeType(EntityType.project);
		node.setParentId(aOverrideId);
		aOverrideChildId = nodeManager.createNewNode(node, adminUserInfo);
		
		// Create B
		node = new Node();
		node.setName("B");
		node.setNodeType(EntityType.project);
		node.setParentId(rootId);
		bId = nodeManager.createNewNode(node, adminUserInfo);
		
		//creating a new hierarchy to test changing a node's parentID
		//need a root projectOne
		Node projectOne = new Node();
		projectOne.setName("projectOne");
		projectOne.setNodeType(EntityType.project);
		projectOneId = nodeManager.createNewNode(projectOne, adminUserInfo);
		nodesToDelete.add(projectOneId);
	}
	
	@After
	public void after() throws Exception {
		if(nodeManager != null && nodesToDelete != null){
			for(String id: nodesToDelete){
				try {
					nodeManager.delete(adminUserInfo, id);
				} catch (Exception e) {
					e.printStackTrace();
				} 				
			}
		}
	}

	@Test
	public void testNodeParentChanged() throws Exception {
		// Validate the starting conditions
		String benefactorId = nodeInheritanceDao.getBenefactorCached(rootId);
		assertEquals(rootId, benefactorId);
		benefactorId = nodeInheritanceDao.getBenefactorCached(aOverrideId);
		assertEquals(aOverrideId, benefactorId);
		benefactorId = nodeInheritanceDao.getBenefactorCached(aOverrideChildId);
		assertEquals(aOverrideId, benefactorId);
		// Move aOverride to under A
		nodeInheritanceManager.nodeParentChanged(aOverrideId, aId);
		// Since aOverride is a benefactor, this shouldn't change anything
		benefactorId = nodeInheritanceDao.getBenefactorCached(aId);
		assertEquals(rootId, benefactorId);
		benefactorId = nodeInheritanceDao.getBenefactorCached(aOverrideId);
		assertEquals(aOverrideId, benefactorId);
		benefactorId = nodeInheritanceDao.getBenefactorCached(aOverrideChildId);
		assertEquals(aOverrideId, benefactorId);
	}
	
	@Test
	public void testNodeParentChangedDoNotSkipBenefactor() throws Exception {
		// Validate the starting conditions
		String benefactorId = nodeInheritanceDao.getBenefactorCached(rootId);
		assertEquals(rootId, benefactorId);
		benefactorId = nodeInheritanceDao.getBenefactorCached(aId);
		assertEquals(rootId, benefactorId);
		benefactorId = nodeInheritanceDao.getBenefactorCached(aOverrideId);
		assertEquals(aOverrideId, benefactorId);
		benefactorId = nodeInheritanceDao.getBenefactorCached(aOverrideChildId);
		assertEquals(aOverrideId, benefactorId);
		// Move aOverride to under A
		nodeInheritanceManager.nodeParentChanged(aOverrideId, aId, false);
		// aOverride's benefactor should be changed
		benefactorId = nodeInheritanceDao.getBenefactorCached(aId);
		assertEquals(rootId, benefactorId);
		benefactorId = nodeInheritanceDao.getBenefactorCached(aOverrideId);
		assertEquals(rootId, benefactorId);
		benefactorId = nodeInheritanceDao.getBenefactorCached(aOverrideChildId);
		assertEquals(rootId, benefactorId);
	}

	@Test
	public void testSetNodeToInheritFromItself() throws Exception{
		// Validate the starting conditions
		String benefacrorId = nodeInheritanceDao.getBenefactorCached(rootId);
		assertEquals(rootId, benefacrorId);
		// Since the root already inherits from itself calling this should be
		// a no-opp.
		nodeInheritanceManager.setNodeToInheritFromItself(rootId);
		benefacrorId = nodeInheritanceDao.getBenefactorCached(rootId);
		assertEquals(rootId, benefacrorId);
		
		// Make sure the rest of the nodes are as we expect
		benefacrorId = nodeInheritanceDao.getBenefactorCached(aId);
		assertEquals(rootId, benefacrorId);
		// A.inherits
		benefacrorId = nodeInheritanceDao.getBenefactorCached(aInheritsId);
		assertEquals(rootId, benefacrorId);
		// A.inherits.child
		benefacrorId = nodeInheritanceDao.getBenefactorCached(aInheritsChildId);
		assertEquals(rootId, benefacrorId);
		// A.override
		benefacrorId = nodeInheritanceDao.getBenefactorCached(aOverrideId);
		assertEquals(aOverrideId, benefacrorId);
		// A.overrride.child
		benefacrorId = nodeInheritanceDao.getBenefactorCached(aOverrideChildId);
		assertEquals(aOverrideId, benefacrorId);
		// B
		benefacrorId = nodeInheritanceDao.getBenefactorCached(bId);
		assertEquals(rootId, benefacrorId);
	
		// Now set A to override
		nodeInheritanceManager.setNodeToInheritFromItself(aId);
		// Now check the results
		// A
		benefacrorId = nodeInheritanceDao.getBenefactorCached(aId);
		assertEquals("A should now be its own benefacor", aId, benefacrorId);
		// A.inherits
		benefacrorId = nodeInheritanceDao.getBenefactorCached(aInheritsId);
		assertEquals("A.inherits should now be a beneficary of A", aId, benefacrorId);
		// A.inherits.child
		benefacrorId = nodeInheritanceDao.getBenefactorCached(aInheritsChildId);
		assertEquals("A.inherits.child should now be a beneficary of A", aId, benefacrorId);
		// A.override
		benefacrorId = nodeInheritanceDao.getBenefactorCached(aOverrideId);
		assertEquals("A.override should not have changed and should still be its own benefactor",aOverrideId, benefacrorId);
		// A.override.child
		benefacrorId = nodeInheritanceDao.getBenefactorCached(aOverrideChildId);
		assertEquals("A.override.child should not have changed and should still be inhertiting from its own parent",aOverrideId, benefacrorId);
		// B
		benefacrorId = nodeInheritanceDao.getBenefactorCached(bId);
		assertEquals("B should not have been affected by the change to a sibling.", rootId , benefacrorId);
	}

	@Test
	public void testSetNodeToInheritFromItselfDoNotSkipBenefactor() throws Exception{

		// Validate the starting conditions
		String benefactorId = nodeInheritanceDao.getBenefactorCached(rootId);
		assertEquals(rootId, benefactorId);
		benefactorId = nodeInheritanceDao.getBenefactorCached(aOverrideId);
		assertEquals(aOverrideId, benefactorId);
		benefactorId = nodeInheritanceDao.getBenefactorCached(aOverrideChildId);
		assertEquals(aOverrideId, benefactorId);

		// This should set the benefactor for all the descendants
		nodeInheritanceManager.setNodeToInheritFromItself(rootId, false);
		benefactorId = nodeInheritanceDao.getBenefactorCached(rootId);
		assertEquals(rootId, benefactorId);
		benefactorId = nodeInheritanceDao.getBenefactorCached(aId);
		assertEquals(rootId, benefactorId);
		benefactorId = nodeInheritanceDao.getBenefactorCached(aInheritsId);
		assertEquals(rootId, benefactorId);
		benefactorId = nodeInheritanceDao.getBenefactorCached(aInheritsChildId);
		assertEquals(rootId, benefactorId);
		benefactorId = nodeInheritanceDao.getBenefactorCached(aOverrideId);
		assertEquals(rootId, benefactorId);
		benefactorId = nodeInheritanceDao.getBenefactorCached(aOverrideChildId);
		assertEquals(rootId, benefactorId);
		benefactorId = nodeInheritanceDao.getBenefactorCached(bId);
		assertEquals(rootId, benefactorId);
	}

	@Test
	public void testSetNodeToInheritFromNearestParent() throws Exception{
		// First make sure we can change the root which has a null parent
		// nodeInheritanceManager.setNodeToInheritFromNearestParent(rootId);
		String benefacrorId = nodeInheritanceDao.getBenefactorCached(rootId);
		assertEquals("The root should still inherit from itself",benefacrorId, rootId );
		
		// Now set Node A to override
		// Now set A to override
		nodeInheritanceManager.setNodeToInheritFromItself(aId);
		// This should not effect these children
		benefacrorId = nodeInheritanceDao.getBenefactorCached(aOverrideId);
		assertEquals("A.override should not have changed and should still be its own benefactor",aOverrideId, benefacrorId);
		// A.override.child
		benefacrorId = nodeInheritanceDao.getBenefactorCached(aOverrideChildId);
		assertEquals("A.override.child should not have changed and should still be inhertiting from its own parent",aOverrideId, benefacrorId);
		
		// Now restore A.override to inherit from its nearest parent
		nodeInheritanceManager.setNodeToInheritFromNearestParent(aOverrideId);
		// Check A.override
		benefacrorId = nodeInheritanceDao.getBenefactorCached(aOverrideId);
		assertEquals("A.override should now inherit from A", aId, benefacrorId);
		// Check A.override.child
		benefacrorId = nodeInheritanceDao.getBenefactorCached(aOverrideChildId);
		assertEquals("A.override should now inherit from A", aId, benefacrorId);
		
		// Now set node a back to inherit from its parent
		nodeInheritanceManager.setNodeToInheritFromNearestParent(aId);
		// Check A
		benefacrorId = nodeInheritanceDao.getBenefactorCached(aOverrideId);
		assertEquals("A.override should now inherit from A", rootId, benefacrorId);
		// Check A.override
		benefacrorId = nodeInheritanceDao.getBenefactorCached(aOverrideId);
		assertEquals("A.override should now inherit from A", rootId, benefacrorId);
		// Check A.override.child
		benefacrorId = nodeInheritanceDao.getBenefactorCached(aOverrideChildId);
		assertEquals("A.override should now inherit from A", rootId, benefacrorId);
	}
	
	/**
	 * Tests that when a node's parentId changes the resulting hierarchy
	 * is in left in the correct state.  The tree we are building consists of a vertical
	 * line of four entities.  The bottom is a layer who's parent is a dataset whose parent
	 * is projectTwo whose parent is ProjectOne.  ProjectOne is the top of the tree and root
	 * project.  All four entities inherit from ProjectOne.  The change tests that tree's state
	 * is correct after changing the dataset to receive it's parentID from a new ProjectThree
	 * @throws Exception
	 */
	@Test
	public void testDatasetParentIDChangeWhenAllInheritFromRoot() throws Exception {
		//projectOne should inherit from itself
		String benefactorId = nodeInheritanceDao.getBenefactorCached(projectOneId);
		assertEquals(benefactorId, projectOneId);
		
		//now need to build up heirarchy for the test
		//top is projectOne
		//under is projectTwo
		//under is dataset
		//under is layer
		//all will inherit from projectOne
		Node nextNode = new Node();
		String projectTwoId = null;
		String datasetId = null;
		String layerId = null; 
		
		nextNode.setName("projectTwo");
		nextNode.setNodeType(EntityType.project);
		nextNode.setParentId(projectOneId);
		projectTwoId = nodeManager.createNewNode(nextNode, adminUserInfo);
		nodeInheritanceManager.setNodeToInheritFromNearestParent(projectTwoId);
		benefactorId = nodeInheritanceDao.getBenefactorCached(projectTwoId);
		assertEquals(benefactorId, projectOneId);
		assertEquals(projectOneId, nextNode.getParentId());
		nodesToDelete.add(projectTwoId);
		
		nextNode = new Node();
		nextNode.setName("dataset");
		nextNode.setNodeType(EntityType.folder);
		nextNode.setParentId(projectTwoId);
		datasetId = nodeManager.createNewNode(nextNode, adminUserInfo);
		nodeInheritanceManager.setNodeToInheritFromNearestParent(datasetId);
		benefactorId = nodeInheritanceDao.getBenefactorCached(datasetId);
		assertEquals(benefactorId, projectOneId);
		assertEquals(projectTwoId, nextNode.getParentId());
		nodesToDelete.add(datasetId);
		
		nextNode = new Node();
		nextNode.setName("layer");
		nextNode.setNodeType(EntityType.folder);
		nextNode.setParentId(datasetId);
		layerId = nodeManager.createNewNode(nextNode, adminUserInfo);
		nodeInheritanceManager.setNodeToInheritFromNearestParent(layerId);
		benefactorId = nodeInheritanceDao.getBenefactorCached(layerId);
		assertEquals(benefactorId, projectOneId);
		assertEquals(datasetId, nextNode.getParentId());
		nodesToDelete.add(layerId);
		
		//here our structure exists
		//now change the dataset's parentId to that of a third project (called
		//projectThree). 
		
		//make projectThree
		String projectThreeId = null;
		nextNode = new Node();
		nextNode.setName("projectThree");
		nextNode.setNodeType(EntityType.project);
		projectThreeId = nodeManager.createNewNode(nextNode, adminUserInfo);
		nodeInheritanceManager.setNodeToInheritFromItself(projectThreeId);
		nodesToDelete.add(projectThreeId);
		benefactorId = nodeInheritanceDao.getBenefactorCached(projectThreeId);
		assertEquals(projectThreeId, benefactorId);
		nextNode = null;
		
		//change dataset's parentId to that of projectThree
		Node nodeToCheck = nodeManager.get(adminUserInfo, datasetId);
		assertNotNull(nodeToCheck);
		nodeToCheck.setParentId(projectThreeId);
		nodeManager.update(adminUserInfo, nodeToCheck);
		nodeToCheck = null;
		
		//now verify all entities have the correct state
		
		//check projectOne
		benefactorId = nodeInheritanceDao.getBenefactorCached(projectOneId);
		assertEquals(projectOneId, benefactorId);
		
		//check projectTwo
		nodeToCheck = nodeManager.get(adminUserInfo, projectTwoId);
		assertNotNull(nodeToCheck);
		assertEquals(projectOneId, nodeToCheck.getParentId());
		benefactorId = nodeInheritanceDao.getBenefactorCached(projectTwoId);
		assertEquals(projectOneId, benefactorId);
		nodeToCheck = null;
		
		//check projectThree
		nodeToCheck = nodeManager.get(adminUserInfo, projectThreeId);
		assertNotNull(nodeToCheck);
		benefactorId = nodeInheritanceDao.getBenefactorCached(projectThreeId);
		assertEquals(projectThreeId, benefactorId);
		nodeToCheck = null;
		
		//check dataset
		nodeToCheck = nodeManager.get(adminUserInfo, datasetId);
		assertNotNull(nodeToCheck);
		assertEquals(projectThreeId, nodeToCheck.getParentId());
		benefactorId = nodeInheritanceDao.getBenefactorCached(datasetId);
		assertEquals(projectThreeId, benefactorId);
		nodeToCheck = null;
		
		//check layer
		nodeToCheck = nodeManager.get(adminUserInfo, layerId);
		assertNotNull(nodeToCheck);
		assertEquals(datasetId, nodeToCheck.getParentId());
		benefactorId = nodeInheritanceDao.getBenefactorCached(layerId);
		assertEquals(projectThreeId, benefactorId);
		nodeToCheck = null;	
	}
	
	/**
	 * Tests that when a dataset's parentId is changed the resulting hierarchy
	 * is in left in the correct state.  The tree we are building consists of a vertical
	 * line of four entities.  The bottom is a layer who's parent is a dataset whose parent
	 * is projectTwo whose parent is ProjectOne.  ProjectOne is the top of the tree and root
	 * project.  All entities except layer inherit from ProjectOne.  Layer inherits from itself
	 * The change tests that tree's state is correct after changing the dataset to receive
	 *  it's parentID from a new ProjectThree
	 * @throws Exception
	 */
	@Test
	public void testDatasetParentIDChangeWhenSomeInheritFromRoot() throws Exception {
		//projectOne should inherit from itself
		String benefactorId = nodeInheritanceDao.getBenefactorCached(projectOneId);
		assertEquals(benefactorId, projectOneId);
		
		//now need to build up heirarchy for the test
		//top is projectOne
		//under is projectTwo
		//under is dataset
		//under is layer
		//layer inherits from itself
		//all others inherit from projectOne
		Node nextNode = new Node();
		String projectTwoId = null;
		String datasetId = null;
		String layerId = null; 
		
		nextNode.setName("projectTwo");
		nextNode.setNodeType(EntityType.project);
		nextNode.setParentId(projectOneId);
		projectTwoId = nodeManager.createNewNode(nextNode, adminUserInfo);
		nodeInheritanceManager.setNodeToInheritFromNearestParent(projectTwoId);
		benefactorId = nodeInheritanceDao.getBenefactorCached(projectTwoId);
		assertEquals(benefactorId, projectOneId);
		assertEquals(projectOneId, nextNode.getParentId());
		nodesToDelete.add(projectTwoId);
		
		nextNode = new Node();
		nextNode.setName("dataset");
		nextNode.setNodeType(EntityType.folder);
		nextNode.setParentId(projectTwoId);
		datasetId = nodeManager.createNewNode(nextNode, adminUserInfo);
		nodeInheritanceManager.setNodeToInheritFromNearestParent(datasetId);
		benefactorId = nodeInheritanceDao.getBenefactorCached(datasetId);
		assertEquals(benefactorId, projectOneId);
		assertEquals(projectTwoId, nextNode.getParentId());
		nodesToDelete.add(datasetId);
		
		nextNode = new Node();
		nextNode.setName("layer");
		nextNode.setNodeType(EntityType.folder);
		nextNode.setParentId(datasetId);
		layerId = nodeManager.createNewNode(nextNode, adminUserInfo);
		nodeInheritanceManager.setNodeToInheritFromItself(layerId);
		benefactorId = nodeInheritanceDao.getBenefactorCached(layerId);
		assertEquals(layerId, benefactorId);
		assertEquals(datasetId, nextNode.getParentId());
		nodesToDelete.add(layerId);
		
		//here our structure exists
		//now change the dataset's parentId to that of a third project (called
		//projectThree). 
		
		//make projectThree
		String projectThreeId = null;
		nextNode = new Node();
		nextNode.setName("projectThree");
		nextNode.setNodeType(EntityType.project);
		projectThreeId = nodeManager.createNewNode(nextNode, adminUserInfo);
		nodeInheritanceManager.setNodeToInheritFromItself(projectThreeId);
		nodesToDelete.add(projectThreeId);
		benefactorId = nodeInheritanceDao.getBenefactorCached(projectThreeId);
		assertEquals(projectThreeId, benefactorId);
		nextNode = null;
		
		//change dataset's parentId to that of projectThree
		Node nodeToCheck = nodeManager.get(adminUserInfo, datasetId);
		assertNotNull(nodeToCheck);
		nodeToCheck.setParentId(projectThreeId);
		nodeManager.update(adminUserInfo, nodeToCheck);
		nodeToCheck = null;
		
		//now verify all entities have the correct state
		
		//check projectOne
		benefactorId = nodeInheritanceDao.getBenefactorCached(projectOneId);
		assertEquals(projectOneId, benefactorId);
		
		//check projectTwo
		nodeToCheck = nodeManager.get(adminUserInfo, projectTwoId);
		assertNotNull(nodeToCheck);
		assertEquals(projectOneId, nodeToCheck.getParentId());
		benefactorId = nodeInheritanceDao.getBenefactorCached(projectTwoId);
		assertEquals(projectOneId, benefactorId);
		nodeToCheck = null;
		
		//check projectThree
		nodeToCheck = nodeManager.get(adminUserInfo, projectThreeId);
		assertNotNull(nodeToCheck);
		benefactorId = nodeInheritanceDao.getBenefactorCached(projectThreeId);
		assertEquals(projectThreeId, benefactorId);
		nodeToCheck = null;
		
		//check dataset
		nodeToCheck = nodeManager.get(adminUserInfo, datasetId);
		assertNotNull(nodeToCheck);
		assertEquals(projectThreeId, nodeToCheck.getParentId());
		benefactorId = nodeInheritanceDao.getBenefactorCached(datasetId);
		assertEquals(projectThreeId, benefactorId);
		nodeToCheck = null;
		
		//check layer
		nodeToCheck = nodeManager.get(adminUserInfo, layerId);
		assertNotNull(nodeToCheck);
		assertEquals(datasetId, nodeToCheck.getParentId());
		benefactorId = nodeInheritanceDao.getBenefactorCached(layerId);
		assertEquals(layerId, benefactorId);
		nodeToCheck = null;	
	}
	
	/**
	 * Tests that when a dataset's parentId is changed the resulting hierarchy
	 * is in left in the correct state.  The tree we are building consists of a 
	 * root project called projectOne.  Below projectOne is projectTwo.  Below 
	 * projectTwo is a dataset.  Below the dataset it two layers called layerOne 
	 * and layerTwo.  ProjectOne inherits from itself.  ProjectTwo inherits from 
	 * itself.  LayerOne inherits from itself.  The rest of the entities inherit
	 * from projectTwo.  The change tests that tree's state is correct after 
	 * changing the dataset to receive it's parentID from a new ProjectThree
	 * @throws Exception
	 */
	@Test
	public void testDatasetParentIDChangeWhenDatasetHasTreeBelowIt() throws Exception {
		//projectOne should inherit from itself
		String benefactorId = nodeInheritanceDao.getBenefactorCached(projectOneId);
		assertEquals(benefactorId, projectOneId);
		
		//now need to build up heirarchy for the test
		//top is projectOne
		//under is projectTwo
		//under is dataset
		//under is layerOne and layerTwo
		//projectOne, projectTwo, and layerOne inherit from themselves
		//all others inherit from projectTwo
		Node nextNode = new Node();
		String projectTwoId = null;
		String datasetId = null;
		String layerOneId = null;
		String layerTwoId = null;
		
		nextNode.setName("projectTwo");
		nextNode.setNodeType(EntityType.project);
		nextNode.setParentId(projectOneId);
		projectTwoId = nodeManager.createNewNode(nextNode, adminUserInfo);
		nodeInheritanceManager.setNodeToInheritFromItself(projectTwoId);
		benefactorId = nodeInheritanceDao.getBenefactorCached(projectTwoId);
		assertEquals(projectTwoId, benefactorId);
		assertEquals(projectOneId, nextNode.getParentId());
		nodesToDelete.add(projectTwoId);
		
		nextNode = new Node();
		nextNode.setName("dataset");
		nextNode.setNodeType(EntityType.folder);
		nextNode.setParentId(projectTwoId);
		datasetId = nodeManager.createNewNode(nextNode, adminUserInfo);
		nodeInheritanceManager.setNodeToInheritFromNearestParent(datasetId);
		benefactorId = nodeInheritanceDao.getBenefactorCached(datasetId);
		assertEquals(projectTwoId, benefactorId);
		assertEquals(projectTwoId, nextNode.getParentId());
		nodesToDelete.add(datasetId);
		
		nextNode = new Node();
		nextNode.setName("layerOne");
		nextNode.setNodeType(EntityType.folder);
		nextNode.setParentId(datasetId);
		layerOneId = nodeManager.createNewNode(nextNode, adminUserInfo);
		nodeInheritanceManager.setNodeToInheritFromItself(layerOneId);
		benefactorId = nodeInheritanceDao.getBenefactorCached(layerOneId);
		assertEquals(layerOneId, benefactorId);
		assertEquals(datasetId, nextNode.getParentId());
		nodesToDelete.add(layerOneId);
		
		nextNode = new Node();
		nextNode.setName("layerTwo");
		nextNode.setNodeType(EntityType.folder);
		nextNode.setParentId(datasetId);
		layerTwoId = nodeManager.createNewNode(nextNode, adminUserInfo);
		nodeInheritanceManager.setNodeToInheritFromNearestParent(layerTwoId);
		benefactorId = nodeInheritanceDao.getBenefactorCached(layerTwoId);
		assertEquals(projectTwoId, benefactorId);
		assertEquals(datasetId, nextNode.getParentId());
		nodesToDelete.add(layerTwoId);		
		
		//here our structure exists
		//now change the dataset's parentId to that of a third project (called
		//projectThree). 
		
		//make projectThree
		String projectThreeId = null;
		nextNode = new Node();
		nextNode.setName("projectThree");
		nextNode.setNodeType(EntityType.project);
		projectThreeId = nodeManager.createNewNode(nextNode, adminUserInfo);
		nodeInheritanceManager.setNodeToInheritFromItself(projectThreeId);
		nodesToDelete.add(projectThreeId);
		benefactorId = nodeInheritanceDao.getBenefactorCached(projectThreeId);
		assertEquals(projectThreeId, benefactorId);
		nextNode = null;
		
		//change dataset's parentId to that of projectThree
		Node nodeToCheck = nodeManager.get(adminUserInfo, datasetId);
		assertNotNull(nodeToCheck);
		nodeToCheck.setParentId(projectThreeId);
		nodeManager.update(adminUserInfo, nodeToCheck);
		nodeToCheck = null;
		
		//now verify all entities have the correct state
		
		//check projectOne
		benefactorId = nodeInheritanceDao.getBenefactorCached(projectOneId);
		assertEquals(projectOneId, benefactorId);
		
		//check projectTwo
		nodeToCheck = nodeManager.get(adminUserInfo, projectTwoId);
		assertNotNull(nodeToCheck);
		assertEquals(projectOneId, nodeToCheck.getParentId());
		benefactorId = nodeInheritanceDao.getBenefactorCached(projectTwoId);
		assertEquals(projectTwoId, benefactorId);
		nodeToCheck = null;
		
		//check projectThree
		nodeToCheck = nodeManager.get(adminUserInfo, projectThreeId);
		assertNotNull(nodeToCheck);
		benefactorId = nodeInheritanceDao.getBenefactorCached(projectThreeId);
		assertEquals(projectThreeId, benefactorId);
		nodeToCheck = null;
		
		//check dataset
		nodeToCheck = nodeManager.get(adminUserInfo, datasetId);
		assertNotNull(nodeToCheck);
		assertEquals(projectThreeId, nodeToCheck.getParentId());
		benefactorId = nodeInheritanceDao.getBenefactorCached(datasetId);
		assertEquals(projectThreeId, benefactorId);
		nodeToCheck = null;
		
		//check layerOne
		nodeToCheck = nodeManager.get(adminUserInfo, layerOneId);
		assertNotNull(nodeToCheck);
		assertEquals(datasetId, nodeToCheck.getParentId());
		benefactorId = nodeInheritanceDao.getBenefactorCached(layerOneId);
		assertEquals(layerOneId, benefactorId);
		nodeToCheck = null;	
		
		//check  layerTwo
		nodeToCheck = nodeManager.get(adminUserInfo, layerTwoId);
		assertNotNull(nodeToCheck);
		assertEquals(datasetId, nodeToCheck.getParentId());
		benefactorId = nodeInheritanceDao.getBenefactorCached(layerTwoId);
		assertEquals(projectThreeId, benefactorId);
		nodeToCheck = null;
	}
	
	/**
	 * Tests that when a dataset's parentId is changed the resulting hierarchy
	 * is in left in the correct state.  The tree we are building consists of a 
	 * root project called projectOne, and below it is a dataset.  Both
	 * entities inherit from themselves.  Tests that when you change dataset to 
	 * point to a a  new project called newProject the resulting structure is 
	 * in the correct state.
	 * @throws Exception
	 */
	@Test
	public void testDatasetParentIDChangeWhenDatasetInheritsFromItself() throws Exception {
		//projectOne should inherit from itself
		String benefactorId = nodeInheritanceDao.getBenefactorCached(projectOneId);
		assertEquals(benefactorId, projectOneId);
		
		//now need to build up heirarchy for the test
		//top is projectOne
		//under is dataset
		//both inherit from themselves
		Node nextNode = new Node();
		String datasetId = null;
		
		nextNode.setName("dataset");
		nextNode.setNodeType(EntityType.folder);
		nextNode.setParentId(projectOneId);
		datasetId = nodeManager.createNewNode(nextNode, adminUserInfo);
		nodeInheritanceManager.setNodeToInheritFromItself(datasetId);
		benefactorId = nodeInheritanceDao.getBenefactorCached(datasetId);
		assertEquals(datasetId, benefactorId);
		assertEquals(projectOneId, nextNode.getParentId());
		nodesToDelete.add(datasetId);
		
		//here our structure exists
		//now change the dataset's parentId to that of a new project
		
		//make newProject
		String newProjectId = null;
		nextNode = new Node();
		nextNode.setName("newProject");
		nextNode.setNodeType(EntityType.project);
		newProjectId = nodeManager.createNewNode(nextNode, adminUserInfo);
		nodeInheritanceManager.setNodeToInheritFromItself(newProjectId);
		nodesToDelete.add(newProjectId);
		benefactorId = nodeInheritanceDao.getBenefactorCached(newProjectId);
		assertEquals(newProjectId, benefactorId);
		nextNode = null;
		
		//change dataset's parentId to that of newProject
		Node nodeToCheck = nodeManager.get(adminUserInfo, datasetId);
		assertNotNull(nodeToCheck);
		nodeToCheck.setParentId(newProjectId);
		nodeManager.update(adminUserInfo, nodeToCheck);
		nodeToCheck = null;
		
		//now verify all entities have correct state
		
		//check projectOne
		benefactorId = nodeInheritanceDao.getBenefactorCached(projectOneId);
		assertEquals(projectOneId, benefactorId);
		
		//check newProject
		benefactorId = nodeInheritanceDao.getBenefactorCached(newProjectId);
		assertEquals(newProjectId, benefactorId);
		
		//check dataset
		nodeToCheck = nodeManager.get(adminUserInfo, datasetId);
		assertNotNull(nodeToCheck);
		assertEquals(newProjectId, nodeToCheck.getParentId());
		benefactorId = nodeInheritanceDao.getBenefactorCached(datasetId);
		assertEquals(datasetId, benefactorId);
		nodeToCheck = null;
	}
	
	/**
	 * Tests that when a folder's parentId is changed the resulting hierarchy
	 * is in left in the correct state.  The tree we are building consists of a 
	 * rootFolder.  Below rootFolder is folderTwo. Below folderTwo is
	 * folderThree.  FolderThree has two folders below it named folderFour
	 * and folderFive.  FolderFour inherits from itself and all other
	 * folders inherit from rootFolder.  folderTwo changes it's id to a 
	 * newFolder and test checks to make sure state is correct for all folders
	 * after the parentId change update.
	 * @throws Exception
	 */
	@Test
	public void testFolderParentIDChangeWhenFolderHasTwoLevelsBelowIt() throws Exception {
		//now need to build up heirarchy for the test
		//top is rootFolder
		//under is folderTwo
		//under is folderThree
		//under is folderFour and folderFive
		//folderFour inherits from itself
		//all other folders inherit from rootFolder
		
		Node nextNode = new Node();
		String rootFolderId = null;
		String folderTwoId = null;
		String folderThreeId = null;
		String folderFourId = null;
		String folderFiveId = null;
		
		nextNode.setName("rootFolder");
		nextNode.setNodeType(EntityType.folder);
		rootFolderId = nodeManager.createNewNode(nextNode, adminUserInfo);
		nodeInheritanceManager.setNodeToInheritFromItself(rootFolderId);
		String benefactorId = nodeInheritanceDao.getBenefactorCached(rootFolderId);
		assertEquals(rootFolderId, benefactorId);
		nodesToDelete.add(rootFolderId);
		
		nextNode = new Node();
		nextNode.setName("folderTwo");
		nextNode.setNodeType(EntityType.folder);
		nextNode.setParentId(rootFolderId);
		folderTwoId = nodeManager.createNewNode(nextNode, adminUserInfo);
		nodeInheritanceManager.setNodeToInheritFromNearestParent(folderTwoId);
		benefactorId = nodeInheritanceDao.getBenefactorCached(folderTwoId);
		assertEquals(rootFolderId, benefactorId);
		assertEquals(rootFolderId, nextNode.getParentId());
		nodesToDelete.add(folderTwoId);
		
		nextNode = new Node();
		nextNode.setName("folderThree");
		nextNode.setNodeType(EntityType.folder);
		nextNode.setParentId(folderTwoId);
		folderThreeId = nodeManager.createNewNode(nextNode, adminUserInfo);
		nodeInheritanceManager.setNodeToInheritFromNearestParent(folderThreeId);
		benefactorId = nodeInheritanceDao.getBenefactorCached(folderThreeId);
		assertEquals(rootFolderId, benefactorId);
		assertEquals(folderTwoId, nextNode.getParentId());
		nodesToDelete.add(folderThreeId);
		
		nextNode = new Node();
		nextNode.setName("folderFour");
		nextNode.setNodeType(EntityType.folder);
		nextNode.setParentId(folderThreeId);
		folderFourId = nodeManager.createNewNode(nextNode, adminUserInfo);
		nodeInheritanceManager.setNodeToInheritFromItself(folderFourId);
		benefactorId = nodeInheritanceDao.getBenefactorCached(folderFourId);
		assertEquals(folderFourId, benefactorId);
		assertEquals(folderThreeId, nextNode.getParentId());
		nodesToDelete.add(folderFourId);
		
		nextNode = new Node();
		nextNode.setName("folderFive");
		nextNode.setNodeType(EntityType.folder);
		nextNode.setParentId(folderThreeId);
		folderFiveId = nodeManager.createNewNode(nextNode, adminUserInfo);
		nodeInheritanceManager.setNodeToInheritFromNearestParent(folderFiveId);
		benefactorId = nodeInheritanceDao.getBenefactorCached(folderFiveId);
		assertEquals(rootFolderId, benefactorId);
		assertEquals(folderThreeId, nextNode.getParentId());
		nodesToDelete.add(folderFiveId);
		
		//here our structure exists
		//now change the dataset's parentId to that of a newFolder 
		
		//make newFolder
		String newFolderId = null;
		nextNode = new Node();
		nextNode.setName("newFolder");
		nextNode.setNodeType(EntityType.folder);
		newFolderId = nodeManager.createNewNode(nextNode, adminUserInfo);
		nodeInheritanceManager.setNodeToInheritFromItself(newFolderId);
		nodesToDelete.add(newFolderId);
		benefactorId = nodeInheritanceDao.getBenefactorCached(newFolderId);
		assertEquals(newFolderId, benefactorId);
		nextNode = null;
		
		//change folderTwo's parentId to that of newFolder
		Node nodeToCheck = nodeManager.get(adminUserInfo, folderTwoId);
		assertNotNull(nodeToCheck);
		nodeToCheck.setParentId(newFolderId);
		nodeManager.update(adminUserInfo, nodeToCheck);
		nodeToCheck = null;
		
		//now verify all entities are in correct state
		
		//check rootFolder
		benefactorId = nodeInheritanceDao.getBenefactorCached(rootFolderId);
		assertEquals(rootFolderId, benefactorId);
		
		//check newFolder
		benefactorId = nodeInheritanceDao.getBenefactorCached(newFolderId);
		assertEquals(newFolderId, benefactorId);
		
		//check folderTwo
		nodeToCheck = nodeManager.get(adminUserInfo, folderTwoId);
		assertNotNull(nodeToCheck);
		assertEquals(newFolderId, nodeToCheck.getParentId());
		benefactorId = nodeInheritanceDao.getBenefactorCached(folderTwoId);
		assertEquals(newFolderId, benefactorId);
		nodeToCheck = null;
		
		//check folderThree
		nodeToCheck = nodeManager.get(adminUserInfo, folderThreeId);
		assertNotNull(nodeToCheck);
		assertEquals(folderTwoId, nodeToCheck.getParentId());
		benefactorId = nodeInheritanceDao.getBenefactorCached(folderThreeId);
		assertEquals(newFolderId, benefactorId);
		nodeToCheck = null;
		
		//check folderFour
		nodeToCheck = nodeManager.get(adminUserInfo, folderFourId);
		assertNotNull(nodeToCheck);
		assertEquals(folderThreeId, nodeToCheck.getParentId());
		benefactorId = nodeInheritanceDao.getBenefactorCached(folderFourId);
		assertEquals(folderFourId, benefactorId);
		nodeToCheck = null;
		
		//check folderFive
		nodeToCheck = nodeManager.get(adminUserInfo, folderFiveId);
		assertNotNull(nodeToCheck);
		assertEquals(folderThreeId, nodeToCheck.getParentId());
		benefactorId = nodeInheritanceDao.getBenefactorCached(folderFiveId);
		assertEquals(newFolderId, benefactorId);
		nodeToCheck = null;
	}

}
