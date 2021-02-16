package org.sagebionetworks.repo.manager;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.sagebionetworks.ids.IdGenerator;
import org.sagebionetworks.ids.IdType;
import org.sagebionetworks.repo.model.ACCESS_TYPE;
import org.sagebionetworks.repo.model.AccessControlList;
import org.sagebionetworks.repo.model.AccessControlListDAO;
import org.sagebionetworks.repo.model.AuthorizationConstants.ACL_SCHEME;
import org.sagebionetworks.repo.model.AuthorizationConstants.BOOTSTRAP_PRINCIPAL;
import org.sagebionetworks.repo.model.ConflictingUpdateException;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.EntityType;
import org.sagebionetworks.repo.model.EntityTypeUtils;
import org.sagebionetworks.repo.model.InvalidModelException;
import org.sagebionetworks.repo.model.Node;
import org.sagebionetworks.repo.model.NodeConstants;
import org.sagebionetworks.repo.model.NodeDAO;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.annotation.v2.Annotations;
import org.sagebionetworks.repo.model.annotation.v2.AnnotationsV2TestUtils;
import org.sagebionetworks.repo.model.annotation.v2.AnnotationsValueType;
import org.sagebionetworks.repo.model.auth.NewUser;
import org.sagebionetworks.repo.model.bootstrap.EntityBootstrapper;
import org.sagebionetworks.repo.model.dao.FileHandleDao;
import org.sagebionetworks.repo.model.dbo.persistence.DBOCredential;
import org.sagebionetworks.repo.model.dbo.persistence.DBOTermsOfUseAgreement;
import org.sagebionetworks.repo.model.entity.FileHandleUpdateRequest;
import org.sagebionetworks.repo.model.file.FileHandle;
import org.sagebionetworks.repo.model.file.S3FileHandle;
import org.sagebionetworks.repo.model.provenance.Activity;
import org.sagebionetworks.repo.model.util.ModelConstants;
import org.sagebionetworks.repo.web.NotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

/**
 * This is an integration test for the NodeManagerImpl.  Most of the testing should occur
 * in NodeManagerImpleUnitTest.
 * @author John
 *
 */
@ExtendWith(SpringExtension.class)
@ContextConfiguration(locations = { "classpath:test-context.xml" })
public class NodeManagerImplAutoWiredTest {
	
	@Autowired
	public NodeManager nodeManager;
	
	@Autowired
	public UserManager userManager;
	
	@Autowired
	private NodeDAO nodeDAO;
	
	@Autowired
	private EntityBootstrapper entityBootstrapper;
	
	@Autowired
	private AccessControlListDAO aclDAO;
	
	@Autowired
	private AuthorizationManager authorizationManager;
	
	@Autowired
	private ActivityManager activityManager;
	
	@Autowired
	private IdGenerator idGenerator;
	
	@Autowired
	private FileHandleDao fileHandleDao;

	private List<String> activitiesToDelete;
	
	private UserInfo adminUserInfo;
	private UserInfo userInfo;
	
	@BeforeEach
	public void before() throws Exception {
		assertNotNull(nodeManager);
		activitiesToDelete = new ArrayList<String>();
		fileHandleDao.truncateTable();
		
		DBOTermsOfUseAgreement tou = new DBOTermsOfUseAgreement();
		tou.setAgreesToTermsOfUse(Boolean.TRUE);
		
		// Make sure we have a valid user.
		adminUserInfo = userManager.getUserInfo(BOOTSTRAP_PRINCIPAL.THE_ADMIN_USER.getPrincipalId());
		UserInfo.validateUserInfo(adminUserInfo);
		
		DBOCredential cred = new DBOCredential();
		cred.setSecretKey("");
		NewUser nu = new NewUser();
		nu.setEmail(UUID.randomUUID().toString() + "@test.com");
		nu.setUserName(UUID.randomUUID().toString());
		userInfo = userManager.createOrGetTestUser(adminUserInfo, nu, cred, tou);
		userInfo.getGroups().add(BOOTSTRAP_PRINCIPAL.CERTIFIED_USERS.getPrincipalId());
	}
	
	@AfterEach
	public void after() throws Exception {
		aclDAO.truncateAll();
		nodeDAO.truncateAll();
		if(activityManager != null && activitiesToDelete != null){
			for(String id: activitiesToDelete){
				try {
					activityManager.deleteActivity(adminUserInfo, id);
				} catch (Exception e) {
					e.printStackTrace();
				} 				
			}
		}
		fileHandleDao.truncateTable();
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
			newNode = nodeManager.getNode(userInfo, id);
			// A parent node should have been assigned to this node.
			assertNotNull(newNode.getParentId());
			// What is the parent path?
			String parentPaht = EntityTypeUtils.getDefaultParentPath(type);
			assertNotNull(parentPaht);
			ACL_SCHEME expectedSchem = entityBootstrapper.getChildAclSchemeForPath(parentPaht);
			if(ACL_SCHEME.INHERIT_FROM_PARENT == expectedSchem){
				// This node should inherit from its parent
				String benefactorId = nodeDAO.getBenefactor(id);
				String parentBenefactor = nodeDAO.getBenefactor(newNode.getParentId());
				assertEquals("This node should inherit from its parent",parentBenefactor, benefactorId);
			}else if(ACL_SCHEME.GRANT_CREATOR_ALL == expectedSchem){
				// This node should inherit from itself
				String benefactorId = nodeDAO.getBenefactor(id);
				assertEquals(id, benefactorId, "This node should inherit from its parent");
				AccessControlList acl = aclDAO.get(id, ObjectType.ENTITY);
				assertNotNull(acl);
				assertEquals(id, acl.getId());
				// Make sure the user can do everything
				for(ACCESS_TYPE accessType : ModelConstants.ENTITY_ADMIN_ACCESS_PERMISSIONS) {
					assertTrue(authorizationManager.canAccess(userInfo, id, ObjectType.ENTITY, accessType).isAuthorized());
				}
			}else{
				throw new IllegalStateException("Unknown ACL_SCHEME type: "+expectedSchem);
			}
		}
	}
	
	@Test
	public void testCreateWithEntityPropertyAnnotations() throws Exception {
		// We do not want an admin for this test
		
		// Create a node
		Node newNode = new Node();
		newNode.setName("NodeManagerImplAutoWiredTest.testCreateWithEntityPropertyAnnotations");
		// We are using an agreement because the user should have permission to create it but not update it
		newNode.setNodeType(EntityType.project);
		org.sagebionetworks.repo.model.Annotations annos = new org.sagebionetworks.repo.model.Annotations();
		annos.addAnnotation("stringKey", "stringValue");
		annos.addAnnotation("longKey", new Long(120));
		// We are not using the admin to create this node.
		newNode = nodeManager.createNewNode(newNode, annos, userInfo);
		String id = newNode.getId();
		assertNotNull(id);
		// Validate the node's annotations
		annos = nodeManager.getEntityPropertyAnnotations(userInfo, id);
		assertNotNull(annos);
		assertEquals("stringValue", annos.getSingleValue("stringKey"));
		assertEquals(120L, annos.getSingleValue("longKey"));
	}
	
	/**
	 * Extended for PLFM-5178.
	 * 
	 */
	@Test
	public void testCreateAndUpdate() throws Exception {
		// Create a node
		Node start = new Node();
		start.setName("NodeManagerImplAutoWiredTest.testCreateNode");
		
		// need to enable Public to have 'create' access to 'someType'
		start.setNodeType(EntityType.project);
		start = nodeManager.createNode(start, userInfo);
		assertNotNull(start);
		//Make sure we can get the node
		Node fetched = nodeManager.getNode(adminUserInfo, start.getId());
		assertNotNull(fetched);
		assertEquals(start, fetched);
		assertEquals(userInfo.getId().toString(), fetched.getCreatedByPrincipalId().toString());
		assertEquals(userInfo.getId().toString(), fetched.getModifiedByPrincipalId().toString());
		assertNotNull(fetched.getCreatedOn());
		assertNotNull(fetched.getModifiedOn());
		assertNotNull(fetched.getETag());
		
		// Now try to update the node
		String startingETag = fetched.getETag();
		fetched.setName("mySecondName");
		// ensure modified on changes
		Thread.sleep(10);
		// update as a different user.
		Node updated = nodeManager.update(adminUserInfo, fetched, null, false);
		assertNotNull(updated);
		// Make sure the result has a new eTag
		assertFalse(startingETag.equals(updated.getETag()));
		// Now get it again
		Node fetchedAfterUpdate = nodeManager.getNode(adminUserInfo, start.getId());
		assertNotNull(fetchedAfterUpdate);
		assertEquals(updated, fetchedAfterUpdate);
		assertEquals("mySecondName", fetchedAfterUpdate.getName());
		assertEquals(userInfo.getId().toString(), fetchedAfterUpdate.getCreatedByPrincipalId().toString());
		assertEquals(start.getCreatedOn(), fetchedAfterUpdate.getCreatedOn());
		assertEquals(adminUserInfo.getId(), fetchedAfterUpdate.getModifiedByPrincipalId());
		assertTrue(start.getModifiedOn().getTime() < fetchedAfterUpdate.getModifiedOn().getTime());
	}
	
	@Test
	public void testNodeUpdateConflict() throws DatastoreException, InvalidModelException, NotFoundException, UnauthorizedException, ConflictingUpdateException{
		Node newNode = new Node();
		newNode.setName("NodeManagerImplAutoWiredTest.testCreateNode");
		// need to enable Public to have 'create' access to 'someType'
		newNode.setNodeType(EntityType.project);
		String id = nodeManager.createNewNode(newNode, adminUserInfo);
		assertNotNull(id);
		Node node = nodeManager.getNode(adminUserInfo, id);
		// Now update
		node.setName("newName");
		nodeManager.update(adminUserInfo, node, null, false);
		
		// Now update again without a new eTag
		node.setName("Not going to take");
		assertThrows(ConflictingUpdateException.class, () -> {
			// Call under test
			nodeManager.update(adminUserInfo, node, null, false);
		});
	}
	
	@Test
	public void testUpdateUserAnnotations() throws DatastoreException, InvalidModelException, NotFoundException, UnauthorizedException, ConflictingUpdateException, InterruptedException{
		// Create a node
		Node startNode = new Node();
		startNode.setName("NodeManagerImplAutoWiredTest.updateUserAnnotations");
		startNode.setNodeType(EntityType.project);
		UserInfo userInfo = adminUserInfo;
		String id = nodeManager.createNewNode(startNode, userInfo);
		startNode = nodeManager.getNode(userInfo, id);
		assertNotNull(id);
		// First get the annotations for this node
		Annotations annos = nodeManager.getUserAnnotations(userInfo, id);
		assertNotNull(annos);
		assertNotNull(annos.getEtag());
		String eTagBeforeUpdate = annos.getEtag();
		// Add some values
		AnnotationsV2TestUtils.putAnnotations(annos, "longKey", "1", AnnotationsValueType.LONG);
		// sleep to ensure modifiedOn changes.
		Thread.sleep(10);
		// Now update the node
		Annotations updated = nodeManager.updateUserAnnotations(adminUserInfo, id, annos);
		assertNotNull(updated);
		assertNotNull(updated.getEtag());
		assertFalse(updated.getEtag().equals(eTagBeforeUpdate));
		Annotations copy = nodeManager.getUserAnnotations(userInfo, id);
		assertEquals(updated,copy);
		// Make sure the eTag has changed
		assertEquals(updated.getEtag(), copy.getEtag());
		Node updatedNode = nodeManager.getNode(userInfo, id);
		assertNotNull(updatedNode);
		assertNotNull(updatedNode.getETag());
		assertEquals(updated.getEtag(), updatedNode.getETag());
		assertFalse(startNode.getETag().equals(updatedNode.getETag()));
		// modified on/by should be updated.
		assertTrue(updatedNode.getModifiedOn().getTime() > startNode.getModifiedOn().getTime());
		assertEquals(adminUserInfo.getId(), updatedNode.getModifiedByPrincipalId());
		// created on/by should not change.
		assertEquals(userInfo.getId(), updatedNode.getCreatedByPrincipalId());
		assertEquals(startNode.getCreatedOn(), updatedNode.getCreatedOn());
	}
	
	@Test
	public void testUpdateUserAnnotations_UpdateConflict() throws DatastoreException, InvalidModelException, NotFoundException, UnauthorizedException, ConflictingUpdateException{
		Node newNode = new Node();
		newNode.setName("NodeManagerImplAutoWiredTest.testUpdateUserAnnotations_UpdateConflict");
		newNode.setNodeType(EntityType.project);
		UserInfo userInfo = adminUserInfo;
		String id = nodeManager.createNewNode(newNode, userInfo);
		assertNotNull(id);
		// First get the annotations for this node
		Annotations annos = nodeManager.getUserAnnotations(userInfo, id);
		AnnotationsV2TestUtils.putAnnotations(annos, "stringKey", "should take", AnnotationsValueType.STRING);
		String startingEtag = annos.getEtag();
		nodeManager.updateUserAnnotations(userInfo, id, annos);
		
		// Try it again without changing the eTag
		annos.setEtag(startingEtag);
		AnnotationsV2TestUtils.putAnnotations(annos, "stringKey", "should not take", AnnotationsValueType.STRING);
		
		assertThrows(ConflictingUpdateException.class, () -> {
			// Call under test
			nodeManager.updateUserAnnotations(userInfo, id, annos);
		});
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
		newNode = nodeManager.createNode(newNode, userInfo);
		String id = newNode.getId();
		assertNotNull(id);

		// Add some annotations to this version
		org.sagebionetworks.repo.model.Annotations annos = nodeManager.getEntityPropertyAnnotations(userInfo, id);
		String firstVersionValue = "Value on the first version.";
		annos.addAnnotation("stringKey", firstVersionValue);
		nodeManager.update(userInfo, newNode, annos, false);

		// In a typical new version scenario we will update a the node and annotations at the same
		// times as creating a new version.
		Node updatedNode = nodeManager.getNode(userInfo, id);
		// The current version for this node should be one
		assertEquals(new Long(1), updatedNode.getVersionNumber());
		org.sagebionetworks.repo.model.Annotations annosToUpdate = nodeManager.getEntityPropertyAnnotations(userInfo, id);
		assertEquals(firstVersionValue, annosToUpdate.getSingleValue("stringKey"));
		// Now attempt to update both the node and the annotations without changing the
		// the version label.  This should cause the update to fail
		updatedNode.setVersionComment("This comment should never get applied because we did not change the version label");
		annosToUpdate.addAnnotation("longKey", new Long(12));
		String eTagBeforeUpdate = updatedNode.getETag();
		// Now try the update
		try{
			nodeManager.update(userInfo, updatedNode, annosToUpdate, true);
			fail("Creating a new version without creating a new versoin label should have caused an IllegalArgumentException");
		}catch(IllegalArgumentException e){
			// expected
		}
		// Validate that the changes were not applied to the node or the annotations
		updatedNode = nodeManager.getNode(userInfo, id);
		assertEquals(eTagBeforeUpdate, updatedNode.getETag(), "Since updating failed, the eTag should not have changed");
		annosToUpdate = nodeManager.getEntityPropertyAnnotations(userInfo, id);
		assertEquals(newNode.getVersionComment(), updatedNode.getVersionComment(), "The version comment should have rolled back to its origianl value on a failure.");
		assertEquals(null, annosToUpdate.getSingleValue("longKey"), "The annoations should have rolled back to its origianl value on a failure.");

		// Now try the update again but with a new version label so the update should take.
		updatedNode.setVersionComment("This this comment should get applied this time.");
		updatedNode.setVersionLabel("0.0.2");
		annosToUpdate.addAnnotation("longKey", new Long(12));
		annosToUpdate.getStringAnnotations().clear();
		String valueOnSecondVersion = "Value on the second version.";
		annosToUpdate.addAnnotation("stringKey", valueOnSecondVersion);
		// call under test
		Node afterUpdate = nodeManager.update(adminUserInfo, updatedNode,  annosToUpdate, true);
		assertNotNull(afterUpdate);
		assertNotNull(afterUpdate.getETag());
		assertFalse(afterUpdate.getETag().equals(eTagBeforeUpdate), "The etag should have been different after an update.");

		// Now check that the update went through
		Node currentNode = nodeManager.getNode(userInfo, id);
		assertNotNull(currentNode);
		org.sagebionetworks.repo.model.Annotations currentAnnos = nodeManager.getEntityPropertyAnnotations(userInfo, id);
		assertNotNull(currentAnnos);
		// The version number should have incremented
		assertEquals(new Long(2), currentNode.getVersionNumber());
		assertEquals(annosToUpdate, currentAnnos);

		// Now get the first version of the node and annotations
		Node nodeZero = nodeManager.getNodeForVersionNumber(userInfo, id, new Long(1));
		assertNotNull(nodeZero);
		assertEquals(new Long(1), nodeZero.getVersionNumber());
		assertNotNull(nodeZero.getModifiedByPrincipalId());
		assertNotNull(nodeZero.getModifiedOn());
		assertEquals("This is the comment on the first version.", nodeZero.getVersionComment());
		// Now get the annotations for the first version.
		org.sagebionetworks.repo.model.Annotations annosZero = nodeManager.getEntityPropertyForVersion(userInfo, id, new Long(1));
		assertNotNull(annosZero);
		assertFalse(currentAnnos.equals(annosZero));
		assertEquals(null, annosZero.getSingleValue("longKey"));
		assertNotNull(annosZero.getStringAnnotations());
		assertEquals(1, annosZero.getStringAnnotations().size());
		assertEquals(firstVersionValue, annosZero.getSingleValue("stringKey"));
	}

	/**
	 * Test added for PLFM-5178
	 */
	@Test
	public void testUpdateVersionModifiedByModifiedOn() throws Exception {
		Node node = new Node();
		node.setName("startName");
		node.setNodeType(EntityType.project);
		// call under test
		node = nodeManager.createNode(node, userInfo);
		assertNotNull(node);
		assertNotNull(node.getETag());
		assertEquals(userInfo.getId(), node.getCreatedByPrincipalId());
		assertEquals(userInfo.getId(), node.getModifiedByPrincipalId());
		assertNotNull(node.getCreatedOn());
		assertEquals(node.getCreatedOn(), node.getModifiedOn());
		
		Node updated = nodeManager.getNode(userInfo, node.getId());
		updated.setName("nameChanged");
		updated.setVersionLabel("v2");
		Annotations annos = nodeManager.getUserAnnotations(adminUserInfo, node.getId());
		Thread.sleep(10);
		boolean newVersion = true;
		// create new version
		updated = nodeManager.update(adminUserInfo, updated, null, newVersion);
		assertNotNull(updated);
		assertEquals(node.getCreatedByPrincipalId(), updated.getCreatedByPrincipalId());
		assertEquals(node.getCreatedOn(), updated.getCreatedOn());
		assertEquals(adminUserInfo.getId(), updated.getModifiedByPrincipalId());
		assertTrue(updated.getModifiedOn().getTime() > node.getModifiedOn().getTime());
		
		// the first version should remain unchanged.
		Node firstVersion = nodeManager.getNodeForVersionNumber(userInfo, node.getId(), node.getVersionNumber());
		assertNotNull(firstVersion);
		assertEquals(node.getCreatedByPrincipalId(), firstVersion.getCreatedByPrincipalId());
		assertEquals(node.getCreatedOn(), firstVersion.getCreatedOn());
		assertEquals(node.getModifiedByPrincipalId(), firstVersion.getModifiedByPrincipalId());
		assertEquals(node.getModifiedOn(), firstVersion.getModifiedOn());
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
		// Now create a few versions
		int numberVersions = 3;
		for(int i=0; i<numberVersions; i++){
			Node node = nodeManager.getNode(userInfo, id);
			assertNotNull(node);
			node.setVersionComment("Comment:"+i);
			node.setVersionLabel("0.0."+i+1);
			nodeManager.update(userInfo, node, null, true);
		}
		// Get the eTag before the delete
		Node beforeDelete = nodeManager.getNode(userInfo, id);
		assertNotNull(beforeDelete);
		assertNotNull(beforeDelete.getETag());
		String eTagBeforeDelete = beforeDelete.getETag();
		// Now delete the current version
		nodeManager.deleteVersion(userInfo, id, new Long(1));
		// Make sure 
		Node afterDelete = nodeManager.getNode(userInfo, id);
		assertNotNull(afterDelete);
		assertNotNull(afterDelete.getETag());
		assertFalse(afterDelete.getETag().equals(eTagBeforeDelete), "Deleting a version failed to increment the eTag");
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
		
		//make a child node
		node = new Node();
		node.setName("child");
		node.setNodeType(EntityType.table);
		node.setParentId(rootId);
		String childId = nodeManager.createNewNode(node, adminUserInfo);
		assertNotNull(childId);
		
		//make a newProject node
		node = new Node();
		node.setName("newProject");
		node.setNodeType(EntityType.project);
		String newProjectId = nodeManager.createNewNode(node, adminUserInfo);
		assertNotNull(newProjectId);
		
		//get the child node and verify the state of it's parentId
		Node fetchedChild = nodeManager.getNode(adminUserInfo, childId);
		assertNotNull(fetchedChild);
		assertEquals(childId, fetchedChild.getId());
		assertEquals(rootId, fetchedChild.getParentId());
		
		//set child's parentId to the newProject
		fetchedChild.setParentId(newProjectId);
		Node updatedChild = nodeManager.update(adminUserInfo, fetchedChild, null, false);
		assertNotNull(updatedChild);
		assertEquals(childId, updatedChild.getId());
		assertEquals(newProjectId, updatedChild.getParentId());
		
		//check and make sure update is in database
		Node childFromDB = nodeManager.getNode(adminUserInfo, childId);
		assertNotNull(childFromDB);
		assertEquals(childId, childFromDB.getId());
		assertEquals(newProjectId, childFromDB.getParentId());
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
		
		//make a folder
		node = new Node();
		node.setName("folder");
		node.setNodeType(EntityType.folder);
		node.setParentId(rootId);
		String folderId = nodeManager.createNewNode(node, adminUserInfo);
		assertNotNull(folderId);
		
		//make a child node
		node = new Node();
		node.setName("child");
		node.setNodeType(EntityType.table);
		node.setParentId(folderId);
		String childId = nodeManager.createNewNode(node, adminUserInfo);
		assertNotNull(childId);
		
		// Get the folder
		Node folder = nodeManager.getNode(adminUserInfo, folderId);
		assertNotNull(folder);
		assertNotNull(folder.getETag());
		// Get the child
		Node child = nodeManager.getNode(adminUserInfo, childId);
		assertNotNull(child);
		assertNotNull(child.getETag());
		String childStartEtag = child.getETag();
		// Now change the parent
		folder.setName("MyNewName");
		folder = nodeManager.update(adminUserInfo, folder, null, false);
		// Validate that the child etag did not change
		child = nodeManager.getNode(adminUserInfo, childId);
		assertNotNull(child);
		assertEquals(childStartEtag, child.getETag(), "Updating a parent object should not have changed the child's etag");
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
		newNode = nodeManager.createNode(newNode, adminUserInfo);
		String nodeId = newNode.getId();
		assertNotNull(nodeId);
		Node createdNode = nodeManager.getNode(adminUserInfo, nodeId);
		assertEquals(actId, createdNode.getActivityId());

		// update activity id
		nodeManager.setActivityForNode(adminUserInfo, nodeId, act2Id);		
		Node updatedNode = nodeManager.getNode(adminUserInfo, nodeId);
		assertEquals(act2Id, updatedNode.getActivityId());
		
		// delete
		nodeManager.deleteActivityLinkToNode(adminUserInfo, nodeId);
		updatedNode = nodeManager.getNode(adminUserInfo, nodeId);
		assertEquals(null, updatedNode.getActivityId());
	}
	
	@Test
	public void testUpdateNodeFileHandle() {
		FileHandle oldFileHandle = createTestFileHandle("Old", userInfo.getId().toString());
		FileHandle newFileHandle = createTestFileHandle("New", userInfo.getId().toString());
		
		Node node = new Node();
		
		node.setName("FileEntity");
		node.setNodeType(EntityType.file);
		node.setFileHandleId(oldFileHandle.getId());
		
		node = nodeManager.createNode(node, userInfo);
		
		String currentEtag = node.getETag();
		
		FileHandleUpdateRequest updateRequest = new FileHandleUpdateRequest();
		
		updateRequest.setOldFileHandleId(oldFileHandle.getId());
		updateRequest.setNewFileHandleId(newFileHandle.getId());
		
		// Call under test
		nodeManager.updateNodeFileHandle(userInfo, node.getId(), node.getVersionNumber(), updateRequest);
		
		node = nodeManager.getNode(userInfo, node.getId());
		
		assertNotEquals(currentEtag, node.getETag());
		assertEquals(newFileHandle.getId(), node.getFileHandleId());
		
	}
	
	@Test
	public void testUpdateNodeFileHandleWithDifferentVersion() {
		FileHandle oldFileHandle = createTestFileHandle("Old", userInfo.getId().toString());
		FileHandle newFileHandle = createTestFileHandle("New", userInfo.getId().toString());
		
		Node node = new Node();
		
		node.setName("FileEntity");
		node.setNodeType(EntityType.file);
		node.setFileHandleId(oldFileHandle.getId());
		
		node = nodeManager.createNode(node, userInfo);
		
		Long firstVersionNumber = node.getVersionNumber();
		
		node.setVersionComment("New version comment");
		node.setVersionLabel("New version label");
		// Creates a new version
		node = nodeManager.update(userInfo, node, null, true);
		
		String currentEtag = node.getETag();
		
		FileHandleUpdateRequest updateRequest = new FileHandleUpdateRequest();
		
		updateRequest.setOldFileHandleId(oldFileHandle.getId());
		updateRequest.setNewFileHandleId(newFileHandle.getId());
		
		// Call under test
		nodeManager.updateNodeFileHandle(userInfo, node.getId(), firstVersionNumber, updateRequest);
		
		// Fetch the latest version
		node = nodeManager.getNode(node.getId());
		
		assertNotEquals(currentEtag, node.getETag());
		
		// The latest version retains the old file handle
		assertEquals(oldFileHandle.getId(), node.getFileHandleId());
		
		// Fetch the previous version
		node = nodeManager.getNodeForVersionNumber(userInfo, node.getId(), firstVersionNumber);
		
		assertEquals(newFileHandle.getId(), node.getFileHandleId());
		
		
	}
	
	/**
	 * Test for PLFM-6589 to ensure going over the limit blocks.
	 */
	@Test
	public void testCreateOverDepthLimit() {
		Node project = new Node();
		project.setName("theproject");
		project.setNodeType(EntityType.project);
		project = nodeManager.createNode(project, userInfo);
		String perviousParent = project.getId();
		for(int i=2; i<NodeConstants.MAX_PATH_DEPTH; i++) {
			Node folder = new Node();
			folder.setName("folder"+i);
			folder.setParentId(perviousParent);
			folder.setNodeType(EntityType.folder);
			folder = nodeManager.createNode(folder, userInfo);
			perviousParent = folder.getId();
		}
		Node folder = new Node();
		folder.setName("OverLimit");
		folder.setParentId(perviousParent);
		folder.setNodeType(EntityType.folder);
		String message = assertThrows(IllegalArgumentException.class, ()->{
			nodeManager.createNode(folder, userInfo);
		}).getMessage();
		assertEquals("Exceeded the maximum hierarchical depth of: 50 for parent: "+perviousParent, message);
		Node lastParent = nodeManager.getNode(perviousParent);
		assertNotNull(lastParent);
		assertEquals("folder49", lastParent.getName());
		List<Long> lastPath = nodeDAO.getEntityPathIds(lastParent.getId());
		assertNotNull(lastPath);
		assertEquals(50, lastPath.size());
	}
	
	private S3FileHandle createTestFileHandle(String fileName, String createdById){
		S3FileHandle fileHandle = new S3FileHandle();
		fileHandle.setBucketName("bucket");
		fileHandle.setKey("key");
		fileHandle.setCreatedBy(createdById);
		fileHandle.setFileName(fileName);
		fileHandle.setContentMd5("MD5");
		fileHandle.setContentSize(1024L);
		fileHandle.setId(idGenerator.generateNewId(IdType.FILE_IDS).toString());
		fileHandle.setEtag(UUID.randomUUID().toString());
		fileHandle = (S3FileHandle) fileHandleDao.createFile(fileHandle);
		return fileHandle;
	}

}
