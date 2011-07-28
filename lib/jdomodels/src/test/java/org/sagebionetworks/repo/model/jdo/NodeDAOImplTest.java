package org.sagebionetworks.repo.model.jdo;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Set;

import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.sagebionetworks.ids.IdGenerator;
import org.sagebionetworks.repo.model.Annotations;
import org.sagebionetworks.repo.model.ConflictingUpdateException;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.EntityHeader;
import org.sagebionetworks.repo.model.Node;
import org.sagebionetworks.repo.model.NodeDAO;
import org.sagebionetworks.repo.model.NodeInheritanceDAO;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.web.NotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.orm.jdo.JdoObjectRetrievalFailureException;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.IllegalTransactionStateException;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath:jdomodels-test-context.xml" })
public class NodeDAOImplTest {

	@Autowired
	NodeDAO nodeDao;
	@Autowired
	NodeInheritanceDAO nodeInheritanceDAO;
	@Autowired
	private IdGenerator idGenerator;
	
	// the datasets that must be deleted at the end of each test.
	List<String> toDelete = new ArrayList<String>();
	
	@Before
	public void before(){
		assertNotNull(nodeDao);
		assertNotNull(nodeInheritanceDAO);
		toDelete = new ArrayList<String>();
	}
	
	@After
	public void after(){
		if(toDelete != null && nodeDao != null){
			for(String id:  toDelete){
				// Delete each
				try{
					nodeDao.delete(id);
				}catch (NotFoundException e) {
					// happens if the object no longer exists.
				}
			}
		}
	}
	
	
	@Test 
	public void testCreateNode() throws Exception{
		Node toCreate = NodeTestUtils.createNew("firstNodeEver");
		toCreate.setVersionComment("This is the first version of the first node ever!");
		toCreate.setVersionLabel("0.0.1");
		String id = nodeDao.createNew(toCreate);
		toDelete.add(id);
		assertNotNull(id);
		// This node should exist
		assertTrue(nodeDao.doesNodeExist(Long.parseLong(id)));
		// Make sure we can fetch it
		Node loaded = nodeDao.getNode(id);
		assertNotNull(id);
		assertEquals(id, loaded.getId());
		assertNotNull(loaded.getETag());
		// All new nodes should start off as the first version.
		assertEquals(new Long(1),loaded.getVersionNumber());
		assertEquals(toCreate.getVersionComment(), loaded.getVersionComment());
		assertEquals(toCreate.getVersionLabel(), loaded.getVersionLabel());
		
		// Since this node has no parent, it should be its own benefactor.
		String benefactorId = nodeInheritanceDAO.getBenefactor(id);
		assertEquals(id, benefactorId);
	}
	
	@Test (expected=IllegalArgumentException.class)
	public void testCreateWithExistingId() throws Exception{
		Node toCreate = NodeTestUtils.createNew("secondNodeEver");
		toCreate.setVersionComment("This is the first version of the first node ever!");
		toCreate.setVersionLabel("0.0.1");
		String id = nodeDao.createNew(toCreate);
		toDelete.add(id);
		assertNotNull(id);
		// Now create another node using this id.
		Node duplicate = NodeTestUtils.createNew("should never exist");
		duplicate.setId(id);
		// This should throw an exception.
		String id2 = nodeDao.createNew(duplicate);
		toDelete.add(id2);
		assertNotNull(id2);;
	}
	
	@Test
	public void testCreateWithId() throws Exception{
		// Create a node with a specific id
		String id = new Long(idGenerator.generateNewId()).toString();
		Node toCreate = NodeTestUtils.createNew("secondNodeEver");
		toCreate.setId(id);
		String fetchedId = nodeDao.createNew(toCreate);
		toDelete.add(fetchedId);
		// The id should be the same as what we provided
		assertEquals(id, fetchedId);
	}
	
	@Test 
	public void testAddChild() throws Exception {
		Node parent = NodeTestUtils.createNew("parent");
		String parentId = nodeDao.createNew(parent);
		assertNotNull(parentId);
		toDelete.add(parentId);

		
		//Now add an child
		Node child = NodeTestUtils.createNew("child");
		child.setParentId(parentId);
		String childId = nodeDao.createNew(child);
		assertNotNull(childId);
		toDelete.add(parentId);
		Set<Node> children = nodeDao.getChildren(parentId);
		assertNotNull(children);
		assertEquals(1, children.size());
		Node childLoaded = children.iterator().next();
		assertEquals(childId, childLoaded.getId());
		assertEquals(parentId, childLoaded.getParentId());
		// Make sure we can fetch it
		childLoaded = nodeDao.getNode(childId);
		assertNotNull(childLoaded);
		assertEquals(parentId, childLoaded.getParentId());
		// This child should be inheriting from its parent by default
		String childBenefactorId = nodeInheritanceDAO.getBenefactor(childId);
		assertEquals(parentId, childBenefactorId);
		
		// Now delete the parent and confirm the child is gone too
		nodeDao.delete(parentId);
		// the child should no longer exist
		try{
			childLoaded = nodeDao.getNode(childId);
			fail("The child should not exist after the parent was deleted");
		}catch (NotFoundException e){
			// expected.
		}catch (JdoObjectRetrievalFailureException e){
			System.out.println(e);
		}
	}
	
	@Test (expected=IllegalArgumentException.class)
	public void testCreateUniqueChildNames() throws Exception {
		Node parent = NodeTestUtils.createNew("parent");
		String parentId = nodeDao.createNew(parent);
		assertNotNull(parentId);
		toDelete.add(parentId);
		
		//Now add an child
		Node child = NodeTestUtils.createNew("child");
		child.setParentId(parentId);
		String childId = nodeDao.createNew(child);
		assertNotNull(childId);
		toDelete.add(childId);
		
		// We should not be able to create a child with the same name
		child = NodeTestUtils.createNew("child");
		child.setParentId(parentId);
		String child2Id = nodeDao.createNew(child);
		assertNotNull(child2Id);
		toDelete.add(child2Id);
	}

	@Ignore  // This is not working because the exception is thrown when the transaction is committed, not when we change it.
	@Test (expected=IllegalArgumentException.class)
	public void testUpdateUniqueChildNames() throws Exception {
		Node parent = NodeTestUtils.createNew("parent");
		String parentId = nodeDao.createNew(parent);
		assertNotNull(parentId);
		toDelete.add(parentId);
		
		//Now add an child
		Node child = NodeTestUtils.createNew("child");
		child.setParentId(parentId);
		String childId = nodeDao.createNew(child);
		assertNotNull(childId);
		toDelete.add(childId);
		
		// We should not be able to create a child with the same name
		child = NodeTestUtils.createNew("child2");
		child.setParentId(parentId);
		String child2Id = nodeDao.createNew(child);
		assertNotNull(child2Id);
		toDelete.add(child2Id);
		
		// Now try to change child2's name to 'child' which should fail
		child = nodeDao.getNode(child2Id);
		child.setName("child");
		nodeDao.updateNode(child);
	}
	
	@Test
	public void testGetPath() throws Exception {
		Node parent = NodeTestUtils.createNew("parent");
		String parentId = nodeDao.createNew(parent);
		assertNotNull(parentId);
		toDelete.add(parentId);
		
		//Now add an child
		Node child = NodeTestUtils.createNew("child");
		child.setParentId(parentId);
		String childId = nodeDao.createNew(child);
		assertNotNull(childId);
		toDelete.add(parentId);
		
		//Now add an child
		Node grandChild = NodeTestUtils.createNew("grandChild");
		grandChild.setParentId(childId);
		String grandChildId = nodeDao.createNew(grandChild);
		assertNotNull(grandChildId);
		toDelete.add(grandChildId);
		
		// Now make sure we can get all three nodes using their path
		String path = "/parent";
		String id = nodeDao.getNodeIdForPath(path);
		assertEquals(parentId, id);
		// Get the child
		path = "/parent/child";
		id = nodeDao.getNodeIdForPath(path);
		assertEquals(childId, id);
		// Get the grand child
		path = "/parent/child/grandChild";
		id = nodeDao.getNodeIdForPath(path);
		assertEquals(grandChildId, id);
	}
	
	/**
	 * Calling getETagForUpdate() outside of a transaction in not allowed, and will throw an exception.
	 * @throws NotFoundException 
	 * @throws DatastoreException 
	 * @throws ConflictingUpdateException 
	 */
	@Test(expected=IllegalTransactionStateException.class)
	public void testGetETagForUpdate() throws Exception {
		Node toCreate = NodeTestUtils.createNew("testGetETagForUpdate");
		String id = nodeDao.createNew(toCreate);
		toDelete.add(id);
		assertNotNull(id);
		String eTag = nodeDao.peekCurrentEtag(id);
		eTag = nodeDao.lockNodeAndIncrementEtag(id, eTag);
		fail("Should have thrown an IllegalTransactionStateException");
	}
	
	@Test
	public void testUpdateNode() throws Exception{
		Node node = NodeTestUtils.createNew("testUpdateNode");
		String id = nodeDao.createNew(node);
		toDelete.add(id);
		assertNotNull(id);
		// Now fetch the node
		Node copy = nodeDao.getNode(id);
		assertNotNull(copy);
		// Now change the copy and push it back
		copy.setName("myNewName");
		copy.setDescription("myNewDescription");
		nodeDao.updateNode(copy);
		Node updatedCopy = nodeDao.getNode(id);
		assertNotNull(updatedCopy);
		// The updated copy should match the copy now
		assertEquals(copy, updatedCopy);
	}
	
	@Test(expected=Exception.class)
	public void testNullName() throws Exception{
		Node node = NodeTestUtils.createNew("setNameNull");
		node.setName(null);
		String id = nodeDao.createNew(node);
		toDelete.add(id);
	}
	
	@Test
	public void testCreateAllAnnotationsTypes() throws Exception{
		Node node = NodeTestUtils.createNew("testCreateAnnotations");
		String id = nodeDao.createNew(node);
		toDelete.add(id);
		assertNotNull(id);
		// Now get the annotations for this node.
		Annotations annos = nodeDao.getAnnotations(id);
		assertNotNull(annos);
		assertNotNull(annos.getEtag());
		// Now add some annotations to this node.
		annos.addAnnotation("stringOne", "one");
		annos.addAnnotation("doubleKey", new Double(23.5));
		annos.addAnnotation("longKey", new Long(1234));
		annos.addAnnotation("blobKey", "StringToBlob".getBytes("UTF-8"));
		byte[] bigBlob = new byte[6000];
		Arrays.fill(bigBlob, (byte)0xa3);
		annos.addAnnotation("bigBlob", bigBlob);
		annos.addAnnotation("dateKey", new Date(System.currentTimeMillis()));
		// update the eTag
		long currentETag = Long.parseLong(annos.getEtag());
		currentETag++;
		String newETagString = new Long(currentETag).toString();
		annos.setEtag(newETagString);
		// Update them
		nodeDao.updateAnnotations(id, annos);
		// Now get a copy and ensure it equals what we sent
		Annotations copy = nodeDao.getAnnotations(id);
		assertNotNull(copy);
		assertEquals("one", copy.getSingleValue("stringOne"));
		assertEquals(new Double(23.5), copy.getSingleValue("doubleKey"));
		assertEquals(new Long(1234), copy.getSingleValue("longKey"));
		byte[] blob = (byte[]) copy.getSingleValue("blobKey");
		assertNotNull(blob);
		String blobString = new String(blob, "UTF-8");
		assertEquals("StringToBlob", blobString);
		byte[] bigCopy = (byte[]) copy.getSingleValue("bigBlob");
		assertNotNull(bigCopy);
		assertTrue(Arrays.equals(bigBlob, bigCopy));
	}
	
	@Test
	public void testCreateAnnotations() throws Exception{
		Node node = NodeTestUtils.createNew("testCreateAnnotations");
		String id = nodeDao.createNew(node);
		toDelete.add(id);
		assertNotNull(id);
		// Now get the annotations for this node.
		Annotations annos = nodeDao.getAnnotations(id);
		assertNotNull(annos);
		assertNotNull(annos.getEtag());
		assertNotNull(annos.getBlobAnnotations());
		assertNotNull(annos.getStringAnnotations());
		assertNotNull(annos.getDoubleAnnotations());
		assertNotNull(annos.getLongAnnotations());
		assertNotNull(annos.getDateAnnotations());
		// Now add some annotations to this node.
		annos.addAnnotation("stringOne", "one");
		annos.addAnnotation("doubleKey", new Double(23.5));
		annos.addAnnotation("longKey", new Long(1234));
		// Update them
		nodeDao.updateAnnotations(id, annos);
		// Now get a copy and ensure it equals what we sent
		Annotations copy = nodeDao.getAnnotations(id);
		assertNotNull(copy);
		assertEquals(annos, copy);
		// clear an and update
		assertNotNull(copy.getStringAnnotations().remove("stringOne"));
		nodeDao.updateAnnotations(id, copy);
		Annotations copy2 = nodeDao.getAnnotations(id);
		assertNotNull(copy2);
		assertEquals(copy, copy2);
		// Make sure the node has a new eTag
		Node nodeCopy = nodeDao.getNode(id);
		assertNotNull(nodeCopy);
	}
	
	@Test
	public void testCreateNewVersion() throws Exception {
		Node node = NodeTestUtils.createNew("testCreateNewVersion");
		// Start this node with version and comment information
		node.setVersionComment("This is the very first version of this node.");
		node.setVersionLabel("0.0.1");
		String id = nodeDao.createNew(node);
		toDelete.add(id);
		assertNotNull(id);
		// Load the node
		Node loaded = nodeDao.getNode(id);
		assertNotNull(loaded);
		assertEquals(node.getVersionComment(), loaded.getVersionComment());
		assertEquals(node.getVersionLabel(), loaded.getVersionLabel());
		// Now try to create a new version with a duplicate label
		try{
			Long newNumber = nodeDao.createNewVersion(loaded);
			fail("This should have failed due to a duplicate version label");
		}catch(IllegalArgumentException e){
			// Expected
			System.out.println(e.getMessage());
		}
		// Since creation of a new version failed we should be back to one version
		loaded = nodeDao.getNode(id);
		assertNotNull(loaded);
		assertEquals(node.getVersionComment(), loaded.getVersionComment());
		assertEquals(node.getVersionLabel(), loaded.getVersionLabel());
		
		// Now try to create a new revision with new data
		Node newRev = nodeDao.getNode(id);
		newRev.setVersionLabel("0.0.2");
		newRev.setModifiedBy("someChap");
		newRev.setModifiedOn(new Date(System.currentTimeMillis()));
		Long newNumber = nodeDao.createNewVersion(newRev);
		assertNotNull(newNumber);
		assertEquals(new Long(2), newNumber);
		// Now load the node and check the fields
		loaded = nodeDao.getNode(id);
		assertNotNull(loaded);
		assertEquals(newRev.getVersionComment(), loaded.getVersionComment());
		assertEquals(newRev.getVersionLabel(), loaded.getVersionLabel());
		assertEquals(newRev.getModifiedBy(), newRev.getModifiedBy());
	}
	
	@Test
	public void testNewVersionAnnotations() throws Exception {
		Node node = NodeTestUtils.createNew("testCreateAnnotations");
		// Start this node with version and comment information
		node.setVersionComment("This is the very first version of this node.");
		node.setVersionLabel("0.0.1");
		String id = nodeDao.createNew(node);
		toDelete.add(id);
		assertNotNull(id);
		Annotations annos = nodeDao.getAnnotations(id);
		assertNotNull(annos);
		annos.addAnnotation("string", "value");
		annos.addAnnotation("date", new Date(1));
		annos.addAnnotation("double", 2.3);
		annos.addAnnotation("long", 56l);
		annos.addAnnotation("blob", "Some blob value".getBytes("UTF-8"));
		// Update the annotations
		nodeDao.updateAnnotations(id, annos);
		// Now create a new version
		Node copy = nodeDao.getNode(id);
		copy.setVersionComment(null);
		copy.setVersionLabel("1.0.1");
		Long revNumber = nodeDao.createNewVersion(copy);
		assertEquals(new Long(2), revNumber);
		// At this point the new and old version should have the
		// same annotations.
		Annotations v1Annos = nodeDao.getAnnotationsForVersion(id, 1L);
		assertNotNull(v1Annos);
		Annotations v2Annos = nodeDao.getAnnotationsForVersion(id, 2L);
		assertNotNull(v2Annos);
		assertEquals(v1Annos, v2Annos);
		Annotations currentAnnos = nodeDao.getAnnotations(id);
		assertNotNull(currentAnnos);
		assertEquals(currentAnnos, v2Annos);
		
		// Now update the current annotations
		currentAnnos.getDoubleAnnotations().clear();
		currentAnnos.addAnnotation("double", 8989898.2);
		nodeDao.updateAnnotations(id, currentAnnos);
		
		// Now the old and new should no longer match.
		v1Annos = nodeDao.getAnnotationsForVersion(id, 1L);
		assertNotNull(v1Annos);
		assertEquals(2.3, v1Annos.getSingleValue("double"));
		v2Annos = nodeDao.getAnnotationsForVersion(id, 2L);
		assertNotNull(v2Annos);
		assertEquals(8989898.2, v2Annos.getSingleValue("double"));
		// The two version should now be out of synch with each other.
		assertFalse(v1Annos.equals(v2Annos));
		// The current annos should still match the v2
		currentAnnos = nodeDao.getAnnotations(id);
		assertNotNull(currentAnnos);
		assertEquals(currentAnnos, v2Annos);
	}
	
	/**
	 * Helper method to create a node with multiple versions.
	 * @param numberOfVersions
	 * @return
	 * @throws NotFoundException 
	 * @throws DatastoreException 
	 */
	public String createNodeWithMultipleVersions(int numberOfVersions) throws Exception {
		Node node = NodeTestUtils.createNew("createNodeWithMultipleVersions");
		// Start this node with version and comment information
		node.setVersionComment("This is the very first version of this node.");
		node.setVersionLabel("0.0.0");
		String id = nodeDao.createNew(node);
		toDelete.add(id);
		assertNotNull(id);
		
		// this is the number of versions to create
		for(int i=1; i<numberOfVersions; i++){
			Node current = nodeDao.getNode(id);
			current.setVersionComment("Comment "+i);
			current.setVersionLabel("0.0."+i);
			nodeDao.createNewVersion(current);
		}
		return id;
	}
	
	@Test
	public void testGetVersionNumbers() throws Exception {
		// Create a number of versions
		int numberVersions = 10;
		String id = createNodeWithMultipleVersions(numberVersions);
		// Now list the versions
		List<Long> versionNumbers = nodeDao.getVersionNumbers(id);
		assertNotNull(versionNumbers);
		assertEquals(numberVersions,versionNumbers.size());
		// The highest version should be first
		assertEquals(new Long(numberVersions), versionNumbers.get(0));
		// The very fist version should be last
		assertEquals(new Long(1), versionNumbers.get(versionNumbers.size()-1));
		
		// Make sure we can fetch each version
		for(Long versionNumber: versionNumbers){
			Node nodeVersion = nodeDao.getNodeForVersion(id, versionNumber);
			assertNotNull(nodeVersion);
			assertEquals(versionNumber, nodeVersion.getVersionNumber());
		}
	}
	
	@Test
	public void testDeleteCurrentVersion() throws Exception {
		// Create a number of versions
		int numberVersions = 2;
		String id = createNodeWithMultipleVersions(numberVersions);
		Node node = nodeDao.getNode(id);
		long currentVersion = node.getVersionNumber();
		List<Long> startingVersions = nodeDao.getVersionNumbers(id);
		assertNotNull(startingVersions);
		assertEquals(numberVersions, startingVersions.size());
		// Delete the current version.
		nodeDao.deleteVersion(id, new Long(currentVersion));
		List<Long> endingVersions = nodeDao.getVersionNumbers(id);
		assertNotNull(endingVersions);
		assertEquals(numberVersions-1, endingVersions.size());
		assertFalse(endingVersions.contains(currentVersion));
		// Now make sure the current version of the node still exists
		node = nodeDao.getNode(id);
		assertNotNull(node);
		assertEquals("Deleting the current version of a node failed to change the current version to be current - 1",new Long(currentVersion-1), node.getVersionNumber());
	}
	
	@Test
	public void testDeleteFirstVersion() throws Exception {
		// Create a number of versions
		int numberVersions = 2;
		String id = createNodeWithMultipleVersions(numberVersions);
		Node node = nodeDao.getNode(id);
		long currentVersion = node.getVersionNumber();
		List<Long> startingVersions = nodeDao.getVersionNumbers(id);
		assertNotNull(startingVersions);
		assertEquals(numberVersions, startingVersions.size());
		// Delete the first version
		nodeDao.deleteVersion(id, new Long(1));
		List<Long> endingVersions = nodeDao.getVersionNumbers(id);
		assertNotNull(endingVersions);
		assertEquals(numberVersions-1, endingVersions.size());
		assertFalse(endingVersions.contains(new Long(1)));
		// The current version should not have changed.
		node = nodeDao.getNode(id);
		assertNotNull(node);
		assertEquals("Deleting the first version should not have changed the current version of the node",new Long(currentVersion), node.getVersionNumber());
	}
	
	@Test 
	public void testDeleteAllVersions() throws  Exception {
		// Create a number of versions
		int numberVersions = 3;
		String id = createNodeWithMultipleVersions(numberVersions);
		Node node = nodeDao.getNode(id);
		List<Long> startingVersions = nodeDao.getVersionNumbers(id);
		assertNotNull(startingVersions);
		assertEquals(numberVersions, startingVersions.size());
		// Now delete all versions. This should fail.
		try{
			for(Long versionNumber: startingVersions){
				nodeDao.deleteVersion(id, versionNumber);
			}
			fail("Should not have been able to delte all versions of a node");
		}catch(IllegalArgumentException e){
			// expected.
		}
		// There should be one version left and it should be the first version.
		node = nodeDao.getNode(id);
		assertNotNull(node);
		assertEquals("Deleting all versions except the first should have left the node in place with a current version of 1.",new Long(1), node.getVersionNumber());
	}
	
	@Test
	public void testPeekCurrentEtag() throws  Exception {
		Node node = NodeTestUtils.createNew("testPeekCurrentEtag");
		// Start this node with version and comment information
		node.setVersionComment("This is the very first version of this node.");
		node.setVersionLabel("0.0.0");
		String id = nodeDao.createNew(node);
		toDelete.add(id);
		assertNotNull(id);
		node = nodeDao.getNode(id);
		assertNotNull(node);
		assertNotNull(node.getETag());
		String peekEtag = nodeDao.peekCurrentEtag(id);
		assertEquals(node.getETag(), peekEtag);
	}	
	
	@Test
	public void testGetEntityHeader() throws Exception {
		Node parent = NodeTestUtils.createNew("parent");
		parent.setNodeType(ObjectType.project.name());
		String parentId = nodeDao.createNew(parent);
		toDelete.add(parentId);
		assertNotNull(parentId);
		// Get the header of this node
		EntityHeader parentHeader = nodeDao.getEntityHeader(parentId);
		assertNotNull(parentHeader);
		assertEquals(ObjectType.project.getUrlPrefix(), parentHeader.getType());
		assertEquals("parent", parentHeader.getName());
		assertEquals(parentId, parentHeader.getId());
		
		Node child = NodeTestUtils.createNew("child");
		child.setNodeType(ObjectType.dataset.name());
		child.setParentId(parentId);
		String childId = nodeDao.createNew(child);
		toDelete.add(childId);
		assertNotNull(childId);
		// Get the header of this node
		EntityHeader childHeader = nodeDao.getEntityHeader(childId);
		assertNotNull(childHeader);
		assertEquals(ObjectType.dataset.getUrlPrefix(), childHeader.getType());
		assertEquals("child", childHeader.getName());
		assertEquals(childId, childHeader.getId());
	}
	
	@Test (expected=NotFoundException.class)
	public void testGetEntityHeaderDoesNotExist() throws NotFoundException, DatastoreException{
		// There should be no node with this id.
		long id = idGenerator.generateNewId();
		nodeDao.getEntityHeader(KeyFactory.keyToString(id));
	}
	
	@Test
	public void testGetEntityPath() throws Exception {
		Node node = NodeTestUtils.createNew("parent");
		node.setNodeType(ObjectType.project.name());
		String parentId = nodeDao.createNew(node);
		toDelete.add(parentId);
		assertNotNull(parentId);
		// Add a child		
		node = NodeTestUtils.createNew("child");
		node.setNodeType(ObjectType.dataset.name());
		node.setParentId(parentId);
		String childId = nodeDao.createNew(node);
		toDelete.add(childId);
		assertNotNull(childId);
		// Add a GrandChild		
		node = NodeTestUtils.createNew("grandChild");
		node.setNodeType(ObjectType.layer.name());
		node.setParentId(childId);
		String grandId = nodeDao.createNew(node);
		toDelete.add(grandId);
		assertNotNull(grandId);
		
		// Get the individual headers
		EntityHeader[] array = new EntityHeader[3];;
		array[0] = nodeDao.getEntityHeader(parentId);
		array[1] = nodeDao.getEntityHeader(childId);
		array[2] = nodeDao.getEntityHeader(grandId);
		
		// Now get the path for each node
		List<EntityHeader> path = nodeDao.getEntityPath(grandId);
		assertNotNull(path);
		assertEquals(3, path.size());
		assertEquals(array[0], path.get(0));
		assertEquals(array[1], path.get(1));
		assertEquals(array[2], path.get(2));
		
		// child
		path = nodeDao.getEntityPath(childId);
		assertNotNull(path);
		assertEquals(2, path.size());
		assertEquals(array[0], path.get(0));
		assertEquals(array[1], path.get(1));
		
		// parent
		// child
		path = nodeDao.getEntityPath(parentId);
		assertNotNull(path);
		assertEquals(1, path.size());
		assertEquals(array[0], path.get(0));
	}

}
