package org.sagebionetworks.repo.manager;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.sagebionetworks.repo.model.ACCESS_TYPE;
import org.sagebionetworks.repo.model.AccessControlList;
import org.sagebionetworks.repo.model.AccessControlListDAO;
import org.sagebionetworks.repo.model.Annotations;
import org.sagebionetworks.repo.model.AuthorizationConstants.ACL_SCHEME;
import org.sagebionetworks.repo.model.AuthorizationConstants.BOOTSTRAP_PRINCIPAL;
import org.sagebionetworks.repo.model.ConflictingUpdateException;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.DomainType;
import org.sagebionetworks.repo.model.EntityType;
import org.sagebionetworks.repo.model.EntityTypeUtils;
import org.sagebionetworks.repo.model.InvalidModelException;
import org.sagebionetworks.repo.model.NamedAnnotations;
import org.sagebionetworks.repo.model.Node;
import org.sagebionetworks.repo.model.NodeDAO;
import org.sagebionetworks.repo.model.NodeInheritanceDAO;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.auth.NewUser;
import org.sagebionetworks.repo.model.bootstrap.EntityBootstrapper;
import org.sagebionetworks.repo.model.dbo.DBOBasicDao;
import org.sagebionetworks.repo.model.dbo.persistence.DBOCredential;
import org.sagebionetworks.repo.model.dbo.persistence.DBOTermsOfUseAgreement;
import org.sagebionetworks.repo.model.provenance.Activity;
import org.sagebionetworks.repo.model.util.ModelConstants;
import org.sagebionetworks.repo.web.NotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;


/**
 * This is an integration test for the NodeManagerImpl.  Most of the testing should occur
 * in NodeManagerImpleUnitTest.
 * @author John
 *
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath:test-context.xml" })
public class NodeManagerImplAutoWiredTest {
	
	@Autowired
	public NodeManager nodeManager;
	
	@Autowired
	public UserManager userManager;
	
	@Autowired
	private NodeInheritanceDAO inheritanceDAO;
	
	@Autowired
	private EntityBootstrapper entityBootstrapper;
	
	@Autowired
	private AccessControlListDAO aclDAO;
	
	@Autowired
	private AuthorizationManager authorizationManager;
	
	@Autowired
	private ActivityManager activityManager;
	
	@Autowired
	private DBOBasicDao basicDao;
	
	@Autowired
	private ProjectSettingsManager projectSettingsManager;

	private List<String> nodesToDelete;
	private List<String> activitiesToDelete;
	
	private UserInfo adminUserInfo;
	private UserInfo userInfo;
	
	@Before
	public void before() throws Exception {
		assertNotNull(nodeManager);
		nodesToDelete = new ArrayList<String>();
		activitiesToDelete = new ArrayList<String>();
		
		DBOTermsOfUseAgreement tou = new DBOTermsOfUseAgreement();
		tou.setDomain(DomainType.SYNAPSE);
		tou.setAgreesToTermsOfUse(Boolean.TRUE);
		
		// Make sure we have a valid user.
		adminUserInfo = userManager.getUserInfo(BOOTSTRAP_PRINCIPAL.THE_ADMIN_USER.getPrincipalId());
		UserInfo.validateUserInfo(adminUserInfo);
		
		DBOCredential cred = new DBOCredential();
		cred.setSecretKey("");
		NewUser nu = new NewUser();
		nu.setEmail(UUID.randomUUID().toString() + "@test.com");
		nu.setUserName(UUID.randomUUID().toString());
		userInfo = userManager.createUser(adminUserInfo, nu, cred, tou);
		userInfo.getGroups().add(BOOTSTRAP_PRINCIPAL.CERTIFIED_USERS.getPrincipalId());
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
		if(activityManager != null && activitiesToDelete != null){
			for(String id: activitiesToDelete){
				try {
					activityManager.deleteActivity(adminUserInfo, id);
				} catch (Exception e) {
					e.printStackTrace();
				} 				
			}
		}
		
		userManager.deletePrincipal(adminUserInfo, userInfo.getId());
	}
	
	@Test
	public void testCreateEachType() throws Exception {
		// We do not want an admin for this test

		// Create a node of each type.
		EntityType[] array = EntityType.values();
		for(EntityType type: array){
			Node newNode = new Node();
			newNode.setName("NodeManagerImplAutoWiredTest."+type.name());
			newNode.setNodeType(type);
			String id = nodeManager.createNewNode(newNode, userInfo);
			assertNotNull(id);
			nodesToDelete.add(id);
			newNode = nodeManager.get(userInfo, id);
			// A parent node should have been assigned to this node.
			assertNotNull(newNode.getParentId());
			// What is the parent path?
			String parentPaht = EntityTypeUtils.getDefaultParentPath(type);
			assertNotNull(parentPaht);
			ACL_SCHEME expectedSchem = entityBootstrapper.getChildAclSchemeForPath(parentPaht);
			if(ACL_SCHEME.INHERIT_FROM_PARENT == expectedSchem){
				// This node should inherit from its parent
				String benefactorId = inheritanceDAO.getBenefactor(id);
				String parentBenefactor = inheritanceDAO.getBenefactor(newNode.getParentId());
				assertEquals("This node should inherit from its parent",parentBenefactor, benefactorId);
			}else if(ACL_SCHEME.GRANT_CREATOR_ALL == expectedSchem){
				// This node should inherit from itself
				String benefactorId = inheritanceDAO.getBenefactor(id);
				assertEquals("This node should inherit from its parent",id, benefactorId);
				AccessControlList acl = aclDAO.get(id, ObjectType.ENTITY);
				assertNotNull(acl);
				assertEquals(id, acl.getId());
				// Make sure the user can do everything
				for(ACCESS_TYPE accessType : ModelConstants.ENITY_ADMIN_ACCESS_PERMISSIONS) {
					assertTrue(authorizationManager.canAccess(userInfo, id, ObjectType.ENTITY, accessType).getAuthorized());
				}
			}else{
				throw new IllegalStateException("Unknown ACL_SCHEME type: "+expectedSchem);
			}
		}
	}
	
	@Test
	public void testCreateWithAnnotations() throws Exception {
		// We do not want an admin for this test
		
		// Create a node
		Node newNode = new Node();
		newNode.setName("NodeManagerImplAutoWiredTest.testCreateWithAnnotations");
		// We are using an agreement because the user should have permission to create it but not update it
		newNode.setNodeType(EntityType.project);
		NamedAnnotations named = new NamedAnnotations();
		Annotations annos = named.getAdditionalAnnotations();
		annos.addAnnotation("stringKey", "stringValue");
		annos.addAnnotation("longKey", new Long(120));
		// We are not using the admin to create this node.
		newNode = nodeManager.createNewNode(newNode, named, userInfo);
		String id = newNode.getId();
		assertNotNull(id);
		nodesToDelete.add(id);
		// Validate the node's annotations
		named = nodeManager.getAnnotations(userInfo, id);
		assertNotNull(named);
		annos = named.getAdditionalAnnotations();
		assertNotNull(annos);
		assertEquals("stringValue", annos.getSingleValue("stringKey"));
		assertEquals(new Long(120), annos.getSingleValue("longKey"));
	}
	
	@Test
	public void testCreateAndUpdate() throws ConflictingUpdateException, NotFoundException, DatastoreException, UnauthorizedException, InvalidModelException{
		// Create a node
		Node newNode = new Node();
		newNode.setName("NodeManagerImplAutoWiredTest.testCreateNode");
		
		// need to enable Public to have 'create' access to 'someType'
		newNode.setNodeType(EntityType.project);
		String id = nodeManager.createNewNode(newNode, adminUserInfo);
		assertNotNull(id);
		nodesToDelete.add(id);
		//Make sure we can get the node
		Node fetched = nodeManager.get(adminUserInfo, id);
		assertNotNull(fetched);
		assertEquals(adminUserInfo.getId().toString(), fetched.getCreatedByPrincipalId().toString());
		assertEquals(adminUserInfo.getId().toString(), fetched.getModifiedByPrincipalId().toString());
		assertNotNull(fetched.getCreatedOn());
		assertNotNull(fetched.getModifiedOn());
		assertEquals(id, fetched.getId());
		assertEquals(newNode.getName(), fetched.getName());
		assertEquals(newNode.getNodeType(), fetched.getNodeType());
		assertNotNull(fetched.getETag());
		
		// Now try to update the node
		String startingETag = fetched.getETag();
		fetched.setName("mySecondName");
		Node updated = nodeManager.update(adminUserInfo, fetched);
		assertNotNull(updated);
		// Make sure the result has a new eTag
		assertFalse(startingETag.equals(updated.getETag()));
		// Now get it again
		Node fetchedAgain = nodeManager.get(adminUserInfo, id);
		assertNotNull(fetchedAgain);
		assertEquals("mySecondName", fetchedAgain.getName());
		assertEquals(updated.getETag(), fetchedAgain.getETag());

	}
	
	@Test (expected=ConflictingUpdateException.class)
	public void testNodeUpdateConflict() throws DatastoreException, InvalidModelException, NotFoundException, UnauthorizedException, ConflictingUpdateException{
		Node newNode = new Node();
		newNode.setName("NodeManagerImplAutoWiredTest.testCreateNode");
		// need to enable Public to have 'create' access to 'someType'
		newNode.setNodeType(EntityType.project);
		String id = nodeManager.createNewNode(newNode, adminUserInfo);
		assertNotNull(id);
		nodesToDelete.add(id);
		Node node = nodeManager.get(adminUserInfo, id);
		// Now update
		node.setName("newName");
		nodeManager.update(adminUserInfo, node);
		// Now update again without a new eTag
		node.setName("Not going to take");
		nodeManager.update(adminUserInfo, node);
	}
	
	@Test
	public void testUpdateAnnotations() throws DatastoreException, InvalidModelException, NotFoundException, UnauthorizedException, ConflictingUpdateException{
		// Create a node
		Node newNode = new Node();
		newNode.setName("NodeManagerImplAutoWiredTest.testUpdateAnnotations");
		newNode.setNodeType(EntityType.project);
		UserInfo userInfo = adminUserInfo;
		String id = nodeManager.createNewNode(newNode, userInfo);
		assertNotNull(id);
		nodesToDelete.add(id);
		// First get the annotations for this node
		NamedAnnotations named = nodeManager.getAnnotations(userInfo, id);
		Annotations annos = named.getAdditionalAnnotations();
		assertNotNull(annos);
		assertNotNull(annos.getEtag());
		String eTagBeforeUpdate = annos.getEtag();
		// Add some values
		annos.addAnnotation("longKey", new Long(1));
		// Now update the node
		Annotations updated = nodeManager.updateAnnotations(userInfo,id, annos, NamedAnnotations.NAME_SPACE_ADDITIONAL);
		assertNotNull(updated);
		assertNotNull(updated.getEtag());
		assertFalse(updated.getEtag().equals(eTagBeforeUpdate));
		NamedAnnotations namedCopy = nodeManager.getAnnotations(userInfo, id);
		Annotations copy = namedCopy.getAdditionalAnnotations();
		assertEquals(updated,copy);
		// Make sure the eTag has changed
		assertEquals(updated.getEtag(), copy.getEtag());
		Node nodeCopy = nodeManager.get(userInfo, id);
		assertNotNull(nodeCopy);
		assertNotNull(nodeCopy.getETag());
		assertEquals(updated.getEtag(), nodeCopy.getETag());
	}
	
	@Test (expected=ConflictingUpdateException.class)
	public void testAnnotationsUpdateConflict() throws DatastoreException, InvalidModelException, NotFoundException, UnauthorizedException, ConflictingUpdateException{
		Node newNode = new Node();
		newNode.setName("NodeManagerImplAutoWiredTest.testUpdateAnnotations");
		newNode.setNodeType(EntityType.project);
		UserInfo userInfo = adminUserInfo;
		String id = nodeManager.createNewNode(newNode, userInfo);
		assertNotNull(id);
		nodesToDelete.add(id);
		// First get the annotations for this node
		NamedAnnotations named = nodeManager.getAnnotations(userInfo, id);
		Annotations annos = named.getAdditionalAnnotations();
		annos.addAnnotation("stringKey", "should take");
		String startingEtag = annos.getEtag();
		nodeManager.updateAnnotations(userInfo, id, annos, NamedAnnotations.NAME_SPACE_ADDITIONAL);
		// Try it again without changing the eTag
		annos.setEtag(startingEtag);
		annos.addAnnotation("stringKey", "should not take");
		nodeManager.updateAnnotations(userInfo, id, annos, NamedAnnotations.NAME_SPACE_ADDITIONAL);
	}
	
	@Test
	public void testUpdateWithVersion() throws DatastoreException, InvalidModelException, NotFoundException, UnauthorizedException, ConflictingUpdateException{
		// First create a node with 
		Node newNode = new Node();
		newNode.setName("NodeManagerImplAutoWiredTest.testUpdateAnnotations");
		newNode.setNodeType(EntityType.table);
		newNode.setVersionLabel("0.0.1");
		newNode.setVersionComment("This is the comment on the first version.");
		UserInfo userInfo = adminUserInfo;
		String id = nodeManager.createNewNode(newNode, userInfo);
		assertNotNull(id);
		nodesToDelete.add(id);
		
		// There should be one version for this node
		List<Long> versions = nodeManager.getAllVersionNumbersForNode(userInfo, id);
		assertNotNull(versions);
		assertEquals(1, versions.size());
		assertEquals(new Long(1), versions.get(0));
		
		
		// Add some annotations to this version
		NamedAnnotations named = nodeManager.getAnnotations(userInfo, id);
		Annotations annos = named.getAdditionalAnnotations();
		String firstVersionValue = "Value on the first version.";
		annos.addAnnotation("stringKey", firstVersionValue);
		nodeManager.updateAnnotations(userInfo, id, annos, NamedAnnotations.NAME_SPACE_ADDITIONAL);
		
		// In a typical new version scenario we will update a the node and annotations at the same
		// times as creating a new version.
		Node updatedNode = nodeManager.get(userInfo, id);
		// The current version for this node should be one
		assertEquals(new Long(1), updatedNode.getVersionNumber());
		NamedAnnotations namedToUpdate = nodeManager.getAnnotations(userInfo, id);
		Annotations annosToUpdate = namedToUpdate.getAdditionalAnnotations();
		assertEquals(firstVersionValue, annos.getSingleValue("stringKey"));
		// Now attempt to update both the node and the annotations without changing the 
		// the version label.  This should cause the update to fail
		updatedNode.setVersionComment("This comment should never get applied because we did not change the version label");
		annosToUpdate.addAnnotation("longKey", new Long(12));
		String eTagBeforeUpdate = updatedNode.getETag();
		// Now try the update
		try{
			nodeManager.update(userInfo, updatedNode, namedToUpdate, true);
			fail("Creating a new version without creating a new versoin label should have caused an IllegalArgumentException");
		}catch(IllegalArgumentException e){
			// expected
		}
		// Validate that the changes were not applied to the node or the annotations
		updatedNode = nodeManager.get(userInfo, id);
		assertEquals("Since updating failed, the eTag should not have changed",eTagBeforeUpdate, updatedNode.getETag());
		namedToUpdate = nodeManager.getAnnotations(userInfo, id);
		annosToUpdate = namedToUpdate.getAdditionalAnnotations();
		assertEquals("The version comment should have rolled back to its origianl value on a failure.",newNode.getVersionComment(), updatedNode.getVersionComment());
		assertEquals("The annoations should have rolled back to its origianl value on a failure.",null, annosToUpdate.getSingleValue("longKey"));
		versions = nodeManager.getAllVersionNumbersForNode(userInfo, id);
		assertNotNull(versions);
		assertEquals(1, versions.size());
		
		// Now try the update again but with a new version label so the update should take.
		updatedNode.setVersionComment("This this comment should get applied this time.");
		updatedNode.setVersionLabel("0.0.2");
		annosToUpdate.addAnnotation("longKey", new Long(12));
		annosToUpdate.getStringAnnotations().clear();
		String valueOnSecondVersion = "Value on the second version.";
		annosToUpdate.addAnnotation("stringKey", valueOnSecondVersion);
		Node afterUpdate = nodeManager.update(userInfo, updatedNode, namedToUpdate, true);
		assertNotNull(afterUpdate);
		assertNotNull(afterUpdate.getETag());
		assertFalse("The etag should have been different after an update.", afterUpdate.getETag().equals(eTagBeforeUpdate));
		
		// Now check that the update went through
		Node currentNode = nodeManager.get(userInfo, id);
		assertNotNull(currentNode);
		NamedAnnotations currentNamed = nodeManager.getAnnotations(userInfo, id);
		Annotations currentAnnos = currentNamed.getAdditionalAnnotations();
		assertNotNull(currentAnnos);
		// The version number should have incremented
		assertEquals(new Long(2), currentNode.getVersionNumber());
		assertEquals(annosToUpdate, currentAnnos);
		// There should be two versions
		versions = nodeManager.getAllVersionNumbersForNode(userInfo, id);
		assertNotNull(versions);
		assertEquals(2, versions.size());
		// The first on the list should be the current
		assertEquals(new Long(2), versions.get(0));
		
		// Now get the first version of the node and annotations
		Node nodeZero = nodeManager.getNodeForVersionNumber(userInfo, id, new Long(1));
		assertNotNull(nodeZero);
		assertEquals(new Long(1), nodeZero.getVersionNumber());
		assertNotNull(nodeZero.getModifiedByPrincipalId());
		assertNotNull(nodeZero.getModifiedOn());
		assertEquals("This is the comment on the first version.", nodeZero.getVersionComment());
		// Now get the annotations for the first version.
		NamedAnnotations namedZero = nodeManager.getAnnotationsForVersion(userInfo, id, new Long(1));
		Annotations annosZero = namedZero.getAdditionalAnnotations();
		assertNotNull(annosZero);
		assertFalse(currentAnnos.equals(annosZero));
		assertEquals(null, annosZero.getSingleValue("longKey"));
		assertNotNull(annosZero.getStringAnnotations());
		assertEquals(1, annosZero.getStringAnnotations().size());
		assertEquals(firstVersionValue, annosZero.getSingleValue("stringKey"));
		
		// Finally, make sure we can fetch each version of the node and annotations
		for(Long versionNumber: versions){
			Node thisNodeVersion = nodeManager.getNodeForVersionNumber(userInfo, id, versionNumber);
			assertNotNull(thisNodeVersion);
			assertEquals(versionNumber,thisNodeVersion.getVersionNumber());
			NamedAnnotations thisNamedVersion = nodeManager.getAnnotationsForVersion(userInfo, id, versionNumber);
			Annotations thisAnnosVersoin = thisNamedVersion.getAdditionalAnnotations();
			assertNotNull(thisAnnosVersoin);
		}
	}
	
	@Test
	public void testDeleteVersion() throws DatastoreException, InvalidModelException, NotFoundException, UnauthorizedException, ConflictingUpdateException{
		// First create a node with 
		Node newNode = new Node();
		newNode.setName("NodeManagerImplAutoWiredTest.testUpdateAnnotations");
		newNode.setNodeType(EntityType.table);
		newNode.setVersionLabel("0.0.0");
		newNode.setVersionComment("This is the comment on the first version.");
		UserInfo userInfo = adminUserInfo;
		String id = nodeManager.createNewNode(newNode, userInfo);
		assertNotNull(id);
		nodesToDelete.add(id);
		// Now create a few versions
		int numberVersions = 3;
		for(int i=0; i<numberVersions; i++){
			Node node = nodeManager.get(userInfo, id);
			assertNotNull(node);
			node.setVersionComment("Comment:"+i);
			node.setVersionLabel("0.0."+i+1);
			nodeManager.update(userInfo, node, null, true);
		}
		// List the versions
		List<Long> versionNumbers = nodeManager.getAllVersionNumbersForNode(userInfo, id);
		assertNotNull(versionNumbers);
		assertEquals(numberVersions+1, versionNumbers.size());
		// Get the eTag before the delete
		Node beforeDelete = nodeManager.get(userInfo, id);
		assertNotNull(beforeDelete);
		assertNotNull(beforeDelete.getETag());
		String eTagBeforeDelete = beforeDelete.getETag();
		// Now delete the current version
		nodeManager.deleteVersion(userInfo, id, new Long(1));
		// Make sure 
		Node afterDelete = nodeManager.get(userInfo, id);
		assertNotNull(afterDelete);
		assertNotNull(afterDelete.getETag());
		assertFalse("Deleting a version failed to increment the eTag", afterDelete.getETag().equals(eTagBeforeDelete));
	}

	/**
	 * Checks that parentId updates are correctly handled in NodeManagerImpl's update method.
	 * @throws Exception
	 */
	@Test
	public void testParentIdUpdate() throws Exception {
		//make a root node
		Node node = new Node();
		node.setName("root");
		node.setNodeType(EntityType.project);
		String rootId = nodeManager.createNewNode(node, adminUserInfo);
		assertNotNull(rootId);
		nodesToDelete.add(rootId);
		
		//make a child node
		node = new Node();
		node.setName("child");
		node.setNodeType(EntityType.table);
		node.setParentId(rootId);
		String childId = nodeManager.createNewNode(node, adminUserInfo);
		assertNotNull(childId);
		nodesToDelete.add(childId);
		
		//make a newProject node
		node = new Node();
		node.setName("newProject");
		node.setNodeType(EntityType.project);
		String newProjectId = nodeManager.createNewNode(node, adminUserInfo);
		assertNotNull(newProjectId);
		nodesToDelete.add(newProjectId);
		
		//get the child node and verify the state of it's parentId
		Node fetchedChild = nodeManager.get(adminUserInfo, childId);
		assertNotNull(fetchedChild);
		assertEquals(childId, fetchedChild.getId());
		assertEquals(rootId, fetchedChild.getParentId());
		
		//set child's parentId to the newProject
		fetchedChild.setParentId(newProjectId);
		Node updatedChild = nodeManager.update(adminUserInfo, fetchedChild);
		assertNotNull(updatedChild);
		assertEquals(childId, updatedChild.getId());
		assertEquals(newProjectId, updatedChild.getParentId());
		
		//check and make sure update is in database
		Node childFromDB = nodeManager.get(adminUserInfo, childId);
		assertNotNull(childFromDB);
		assertEquals(childId, childFromDB.getId());
		assertEquals(newProjectId, childFromDB.getParentId());
	}
	
	/**
	 * Verify that correct flow of control happens when update happens for 
	 * parentId change
	 * @throws Exception
	 */
	@Test
	public void testParentIdUpdateFlowOfControl() throws Exception {
		//make a root node
		Node node = new Node();
		node.setName("root");
		node.setNodeType(EntityType.project);
		String rootId = nodeManager.createNewNode(node, adminUserInfo);
		assertNotNull(rootId);
		nodesToDelete.add(rootId);
		
		//make a child node
		node = new Node();
		node.setName("child");
		node.setNodeType(EntityType.table);
		node.setParentId(rootId);
		String childId = nodeManager.createNewNode(node, adminUserInfo);
		assertNotNull(childId);
		nodesToDelete.add(childId);
		
		//make a newProject node
		node = new Node();
		node.setName("newProject");
		node.setNodeType(EntityType.project);
		String newProjectId = nodeManager.createNewNode(node, adminUserInfo);
		assertNotNull(newProjectId);
		nodesToDelete.add(newProjectId);
		
		//get the child node and verify the state of it's parentId
		Node fetchedChild = nodeManager.get(adminUserInfo, childId);
		assertNotNull(fetchedChild);
		assertEquals(childId, fetchedChild.getId());
		assertEquals(rootId, fetchedChild.getParentId());
		
		//root and child nodes are in correct state
		
		//I need a NodeManagerImpl with the mocked dependencies so behavior
		//can be verified
		NodeDAO mockNodeDao = Mockito.mock(NodeDAO.class);
		NodeInheritanceManager mockNodeInheritanceManager = Mockito.mock(NodeInheritanceManager.class);
		when(mockNodeDao.getNode(childId)).thenReturn(nodeManager.get(adminUserInfo, childId));
		
		NodeManager nodeManagerWMocks = new NodeManagerImpl(mockNodeDao, authorizationManager, aclDAO, entityBootstrapper,
				mockNodeInheritanceManager, null, activityManager, projectSettingsManager);
		
		//set child's parentId to the newProject
		fetchedChild.setParentId(newProjectId);
		nodeManagerWMocks.update(adminUserInfo, fetchedChild);
		verify(mockNodeDao, times(1)).changeNodeParent(childId, newProjectId, false);
		verify(mockNodeInheritanceManager, times(1)).nodeParentChanged(childId, newProjectId);
	}
	
	/**
	 * Verify that correct flow of control happens when update happens  
	 * that is not a parentId change
	 * @throws Exception
	 */
	@Test
	public void testNonParentIdUpdateFlowOfControl() throws Exception {
		//make a root node
		Node node = new Node();
		node.setName("root");
		node.setNodeType(EntityType.project);
		String rootId = nodeManager.createNewNode(node, adminUserInfo);
		assertNotNull(rootId);
		nodesToDelete.add(rootId);
		
		//make a child node
		node = new Node();
		node.setName("child");
		node.setNodeType(EntityType.table);
		node.setParentId(rootId);
		String childId = nodeManager.createNewNode(node, adminUserInfo);
		assertNotNull(childId);
		nodesToDelete.add(childId);
		
		//I need a NodeManagerImpl with the mocked dependencies so behavior
		//can be verified
		NodeDAO mockNodeDao = Mockito.mock(NodeDAO.class);
		NodeInheritanceManager mockNodeInheritanceManager = Mockito.mock(NodeInheritanceManager.class);
		when(mockNodeDao.getNode(childId)).thenReturn(nodeManager.get(adminUserInfo, childId));

		NodeManager nodeManagerWMocks = new NodeManagerImpl(mockNodeDao, authorizationManager, aclDAO, entityBootstrapper,
				mockNodeInheritanceManager, null, activityManager, projectSettingsManager);
		
		//make a non parentId change to the child
		Node fetchedNode = nodeManager.get(adminUserInfo, childId);
		fetchedNode.setName("notTheChildName");
		when(mockNodeDao.getParentId(anyString())).thenReturn(new String(fetchedNode.getParentId()));
		nodeManagerWMocks.update(adminUserInfo, fetchedNode);
		verify(mockNodeDao, never()).changeNodeParent(anyString(), anyString(), anyBoolean());
		verify(mockNodeInheritanceManager, never()).nodeParentChanged(anyString(), anyString(), anyBoolean());
	}
	
	@Test
	public void testPLFM_1533() throws DatastoreException, InvalidModelException, UnauthorizedException, NotFoundException{
		// Create a project
		//make a root node
		Node node = new Node();
		node.setName("root");
		node.setNodeType(EntityType.project);
		String rootId = nodeManager.createNewNode(node, adminUserInfo);
		assertNotNull(rootId);
		nodesToDelete.add(rootId);
		
		//make a folder
		node = new Node();
		node.setName("folder");
		node.setNodeType(EntityType.folder);
		node.setParentId(rootId);
		String folderId = nodeManager.createNewNode(node, adminUserInfo);
		assertNotNull(folderId);
		nodesToDelete.add(folderId);
		
		//make a child node
		node = new Node();
		node.setName("child");
		node.setNodeType(EntityType.table);
		node.setParentId(folderId);
		String childId = nodeManager.createNewNode(node, adminUserInfo);
		assertNotNull(childId);
		nodesToDelete.add(childId);
		
		// Get the folder
		Node folder = nodeManager.get(adminUserInfo, folderId);
		assertNotNull(folder);
		assertNotNull(folder.getETag());
		// Get the child
		Node child = nodeManager.get(adminUserInfo, childId);
		assertNotNull(child);
		assertNotNull(child.getETag());
		String childStartEtag = child.getETag();
		// Now change the parent
		folder.setName("MyNewName");
		folder = nodeManager.update(adminUserInfo, folder);
		// Validate that the child etag did not change
		child = nodeManager.get(adminUserInfo, childId);
		assertNotNull(child);
		assertEquals("Updating a parent object should not have changed the child's etag",childStartEtag, child.getETag());
	}

	@Test
	public void testActivityForNodeCrud() throws Exception {
		Activity activity = new Activity();
		activity.setName("NodeManagerImplAutoWiredTest.testSetActivityForNode activity");
		String actId = activityManager.createActivity(adminUserInfo, activity);
		activitiesToDelete.add(actId);

		activity = new Activity();
		activity.setName("NodeManagerImplAutoWiredTest.testSetActivityForNode activity 2");
		String act2Id = activityManager.createActivity(adminUserInfo, activity);
		activitiesToDelete.add(act2Id);
		
		Node newNode = new Node();
		newNode.setName("NodeManagerImplAutoWiredTest.testSetActivityForNode");
		newNode.setNodeType(EntityType.table);
		newNode.setActivityId(actId);
		
		
		// create with activity id
		String nodeId = nodeManager.createNewNode(newNode, adminUserInfo);
		assertNotNull(nodeId);
		nodesToDelete.add(nodeId);
		Node createdNode = nodeManager.get(adminUserInfo, nodeId);
		assertEquals(actId, createdNode.getActivityId());

		// update activity id
		nodeManager.setActivityForNode(adminUserInfo, nodeId, act2Id);		
		Node updatedNode = nodeManager.get(adminUserInfo, nodeId);
		assertEquals(act2Id, updatedNode.getActivityId());
		
		// delete
		nodeManager.deleteActivityLinkToNode(adminUserInfo, nodeId);
		updatedNode = nodeManager.get(adminUserInfo, nodeId);
		assertEquals(null, updatedNode.getActivityId());
	}

}
