package org.sagebionetworks.repo.manager;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.sagebionetworks.repo.model.Annotations;
import org.sagebionetworks.repo.model.ConflictingUpdateException;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.InvalidModelException;
import org.sagebionetworks.repo.model.Node;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.web.NotFoundException;
import org.sagebionetworks.repo.web.util.UserProvider;
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
	public UserProvider testUserProvider;
	
	List<String> nodesToDelete;
	
	private UserInfo testUser;
	
	@Before
	public void before() throws Exception{
		assertNotNull(nodeManager);
		nodesToDelete = new ArrayList<String>();
		// Make sure we have a valid user.
		testUser = testUserProvider.getTestAdiminUserInfo();
		UserInfo.validateUserInfo(testUser);

	}
	
	@After
	public void after() throws Exception {
		if(nodeManager != null && nodesToDelete != null){
			for(String id: nodesToDelete){
				try {
					nodeManager.delete(testUser, id);
				} catch (Exception e) {
					e.printStackTrace();
				} 				
			}
		}
	}
	
	@Test
	public void testCreateAndUpdate() throws ConflictingUpdateException, NotFoundException, DatastoreException, UnauthorizedException, InvalidModelException{
		// Create a node
		Node newNode = new Node();
		newNode.setName("NodeManagerImplAutoWiredTest.testCreateNode");
		
		// need to enable Public to have 'create' access to 'someType'
		newNode.setNodeType(ObjectType.project.name());
		String id = nodeManager.createNewNode(newNode, testUser);
		assertNotNull(id);
		nodesToDelete.add(id);
		//Make sure we can get the node
		Node fetched = nodeManager.get(testUser, id);
		assertNotNull(fetched);
		assertEquals(testUser.getUser().getUserId(), fetched.getCreatedBy());
		assertEquals(testUser.getUser().getUserId(), fetched.getModifiedBy());
		assertNotNull(fetched.getCreatedOn());
		assertNotNull(fetched.getModifiedOn());
		assertEquals(id, fetched.getId());
		assertEquals(newNode.getName(), fetched.getName());
		assertEquals(newNode.getNodeType(), fetched.getNodeType());
		assertNotNull(fetched.getETag());
		
		// Now try to update the node
		String startingETag = fetched.getETag();
		fetched.setName("mySecondName");
		Node updated = nodeManager.update(testUser, fetched);
		assertNotNull(updated);
		// Make sure the result has a new eTag
		assertFalse(startingETag.equals(updated.getETag()));
		// Now get it again
		Node fetchedAgain = nodeManager.get(testUser, id);
		assertNotNull(fetchedAgain);
		assertEquals("mySecondName", fetchedAgain.getName());
		assertEquals(updated.getETag(), fetchedAgain.getETag());

	}
	
	@Test (expected=ConflictingUpdateException.class)
	public void testNodeUpdateConflict() throws DatastoreException, InvalidModelException, NotFoundException, UnauthorizedException, ConflictingUpdateException{
		Node newNode = new Node();
		newNode.setName("NodeManagerImplAutoWiredTest.testCreateNode");
		// need to enable Public to have 'create' access to 'someType'
		newNode.setNodeType(ObjectType.project.name());
		String id = nodeManager.createNewNode(newNode, testUser);
		assertNotNull(id);
		nodesToDelete.add(id);
		Node node = nodeManager.get(testUser, id);
		// Now update
		node.setName("newName");
		nodeManager.update(testUser, node);
		// Now update again without a new eTag
		node.setName("Not going to take");
		nodeManager.update(testUser, node);
	}
	
	@Test
	public void testUpdateAnnotations() throws DatastoreException, InvalidModelException, NotFoundException, UnauthorizedException, ConflictingUpdateException{
		// Create a node
		Node newNode = new Node();
		newNode.setName("NodeManagerImplAutoWiredTest.testUpdateAnnotations");
		newNode.setNodeType(ObjectType.project.name());
		UserInfo userInfo = testUser;
		String id = nodeManager.createNewNode(newNode, userInfo);
		assertNotNull(id);
		nodesToDelete.add(id);
		// First get the annotations for this node
		Annotations annos = nodeManager.getAnnotations(userInfo, id);
		assertNotNull(annos);
		assertNotNull(annos.getEtag());
		String eTagBeforeUpdate = annos.getEtag();
		long before = Long.parseLong(eTagBeforeUpdate);
		String expectedEtagAfterUpdate = new Long(++before).toString();
		// Add some values
		annos.addAnnotation("longKey", new Long(1));
		// Now update the node
		Annotations updated = nodeManager.updateAnnotations(userInfo,id, annos);
		assertNotNull(updated);
		assertEquals(expectedEtagAfterUpdate, updated.getEtag());
		Annotations copy = nodeManager.getAnnotations(userInfo, id);
		assertEquals(updated,copy);
		// Make sure the eTag has changed
		assertEquals(expectedEtagAfterUpdate, copy.getEtag());
		Node nodeCopy = nodeManager.get(userInfo, id);
		assertNotNull(nodeCopy);
		assertNotNull(nodeCopy.getETag());
		assertEquals(expectedEtagAfterUpdate, nodeCopy.getETag().toString());
	}
	
	@Test (expected=ConflictingUpdateException.class)
	public void testAnnotationsUpdateConflict() throws DatastoreException, InvalidModelException, NotFoundException, UnauthorizedException, ConflictingUpdateException{
		Node newNode = new Node();
		newNode.setName("NodeManagerImplAutoWiredTest.testUpdateAnnotations");
		newNode.setNodeType(ObjectType.project.name());
		UserInfo userInfo = testUser;
		String id = nodeManager.createNewNode(newNode, userInfo);
		assertNotNull(id);
		nodesToDelete.add(id);
		// First get the annotations for this node
		Annotations annos = nodeManager.getAnnotations(userInfo, id);
		annos.addAnnotation("stringKey", "should take");
		nodeManager.updateAnnotations(userInfo, id, annos);
		// Try it again without changing the eTag
		annos.addAnnotation("stringKey", "should not take");
		nodeManager.updateAnnotations(userInfo, id, annos);
	}
	
	@Test
	public void testUpdateWithVersion() throws DatastoreException, InvalidModelException, NotFoundException, UnauthorizedException, ConflictingUpdateException{
		// First create a node with 
		Node newNode = new Node();
		newNode.setName("NodeManagerImplAutoWiredTest.testUpdateAnnotations");
		newNode.setNodeType(ObjectType.layerlocation.name());
		newNode.setVersionLabel("0.0.1");
		newNode.setVersionComment("This is the comment on the first version.");
		UserInfo userInfo = testUser;
		String id = nodeManager.createNewNode(newNode, userInfo);
		assertNotNull(id);
		nodesToDelete.add(id);
		
		// There should be one version for this node
		List<Long> versions = nodeManager.getAllVersionNumbersForNode(userInfo, id);
		assertNotNull(versions);
		assertEquals(1, versions.size());
		assertEquals(new Long(1), versions.get(0));
		
		
		// Add some annotations to this version
		Annotations annos = nodeManager.getAnnotations(userInfo, id);
		String firstVersionValue = "Value on the first version.";
		annos.addAnnotation("stringKey", firstVersionValue);
		nodeManager.updateAnnotations(userInfo, id, annos);
		
		// In a typical new version scenario we will update a the node and annotations at the same
		// times as creating a new version.
		Node updatedNode = nodeManager.get(userInfo, id);
		// The current version for this node should be one
		assertEquals(new Long(1), updatedNode.getVersionNumber());
		Annotations annosToUpdate = nodeManager.getAnnotations(userInfo, id);
		assertEquals(firstVersionValue, annos.getSingleValue("stringKey"));
		// Now attempt to update both the node and the annotations without changing the 
		// the version label.  This should cause the update to fail
		updatedNode.setVersionComment("This comment should never get applied because we did not change the version label");
		annosToUpdate.addAnnotation("longKey", new Long(12));
		String eTagBeforeUpdate = updatedNode.getETag();
		String eTagAfterUpdate = new Long(Long.parseLong(eTagBeforeUpdate) + 1).toString();
		// Now try the update
		try{
			nodeManager.update(userInfo, updatedNode, annosToUpdate, true);
			fail("Creating a new version without creating a new versoin label should have caused an IllegalArgumentException");
		}catch(IllegalArgumentException e){
			// expected
		}
		// Validate that the changes were not applied to the node or the annotations
		updatedNode = nodeManager.get(userInfo, id);
		assertEquals("Since updating failed, the eTag should not have changed",eTagBeforeUpdate, updatedNode.getETag());
		annosToUpdate = nodeManager.getAnnotations(userInfo, id);
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
		Node afterUpdate = nodeManager.update(userInfo, updatedNode, annosToUpdate, true);
		assertNotNull(afterUpdate);
		assertEquals("The etag should have been incremented after an update.",eTagAfterUpdate, afterUpdate.getETag());
		
		// Now check that the update went through
		Node currentNode = nodeManager.get(userInfo, id);
		assertNotNull(currentNode);
		Annotations currentAnnos = nodeManager.getAnnotations(userInfo, id);
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
		assertNotNull(nodeZero.getModifiedBy());
		assertNotNull(nodeZero.getModifiedOn());
		assertEquals("This is the comment on the first version.", nodeZero.getVersionComment());
		// Now get the annotations for the first version.
		Annotations annosZero = nodeManager.getAnnotationsForVersion(userInfo, id, new Long(1));
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
			Annotations thisAnnosVersoin = nodeManager.getAnnotationsForVersion(userInfo, id, versionNumber);
			assertNotNull(thisAnnosVersoin);
		}
	}
	
	@Test
	public void testDeleteVersion() throws DatastoreException, InvalidModelException, NotFoundException, UnauthorizedException, ConflictingUpdateException{
		// First create a node with 
		Node newNode = new Node();
		newNode.setName("NodeManagerImplAutoWiredTest.testUpdateAnnotations");
		newNode.setNodeType(ObjectType.layerlocation.name());
		newNode.setVersionLabel("0.0.0");
		newNode.setVersionComment("This is the comment on the first version.");
		UserInfo userInfo = testUser;
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
		String eTagAfterDelete = new Long(Long.parseLong(eTagBeforeDelete)+1).toString();
		// Now delete the current version
		nodeManager.deleteVersion(userInfo, id, new Long(1));
		// Make sure 
		Node afterDelete = nodeManager.get(userInfo, id);
		assertNotNull(afterDelete);
		assertEquals("Deleting a version failed to increment the eTag",eTagAfterDelete, afterDelete.getETag());
	}

}
