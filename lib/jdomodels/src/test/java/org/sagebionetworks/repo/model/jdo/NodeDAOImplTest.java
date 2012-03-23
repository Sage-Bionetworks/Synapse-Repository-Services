package org.sagebionetworks.repo.model.jdo;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.sagebionetworks.ids.IdGenerator;
import org.sagebionetworks.repo.model.Annotations;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.EntityHeader;
import org.sagebionetworks.repo.model.EntityType;
import org.sagebionetworks.repo.model.NamedAnnotations;
import org.sagebionetworks.repo.model.Node;
import org.sagebionetworks.repo.model.NodeBackupDAO;
import org.sagebionetworks.repo.model.NodeConstants;
import org.sagebionetworks.repo.model.NodeDAO;
import org.sagebionetworks.repo.model.NodeInheritanceDAO;
import org.sagebionetworks.repo.model.NodeRevisionBackup;
import org.sagebionetworks.repo.model.Reference;
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
	NodeBackupDAO nodeBackupDao;

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
	public void after() throws DatastoreException{
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
	public void testCreateNode() throws Exception{
		Node toCreate = NodeTestUtils.createNew("firstNodeEver");
		toCreate.setVersionComment("This is the first version of the first node ever!");
		toCreate.setVersionLabel("0.0.1");
		String id = nodeDao.createNew(toCreate);
		toDelete.add(id);
		assertNotNull(id);
		// This node should exist
		assertTrue(nodeDao.doesNodeExist(KeyFactory.stringToKey(id)));
		// Make sure we can fetch it
		Node loaded = nodeDao.getNode(id);
		assertNotNull(id);
		assertEquals(id, loaded.getId());
		assertNotNull(loaded.getETag());
		assertTrue(nodeDao.doesNodeRevisionExist(id, loaded.getVersionNumber()));
		assertFalse(nodeDao.doesNodeRevisionExist(id, loaded.getVersionNumber()+1));
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
	public void testCreateWithDuplicateName() throws Exception{
		String commonName = "name";
		Node parent = NodeTestUtils.createNew("parent");
		String parentId = nodeDao.createNew(parent);
		toDelete.add(parentId);
		assertNotNull(parentId);
		Node one = NodeTestUtils.createNew(commonName);
		one.setParentId(parentId);
		String id = nodeDao.createNew(one);
		toDelete.add(id);
		assertNotNull(id);
		// Now create another node using this id.
		Node oneDuplicate = NodeTestUtils.createNew(commonName);
		oneDuplicate.setParentId(parentId);
		// This should throw an exception.
		try{
			String id2 = nodeDao.createNew(oneDuplicate);
			fail("Setting a duplicate name should have failed");
		}catch(IllegalArgumentException e){
			System.out.println(e.getMessage());
			assertTrue(e.getMessage().indexOf("An entity with the name: name already exists") > -1);
		}
	}
	
	@Test
	public void testCreateWithId() throws Exception{
		// Create a new node with an ID that is beyond the current max of the 
		// ID generator.
		long idLong = idGenerator.generateNewId() + 10;
		String idString = KeyFactory.keyToString(new Long(idLong));
		Node toCreate = NodeTestUtils.createNew("secondNodeEver");
		toCreate.setId(idString);
		String fetchedId = nodeDao.createNew(toCreate);
		toDelete.add(fetchedId);
		// The id should be the same as what we provided
		assertEquals(idString, fetchedId);
		// Also make sure the ID generator was increment to reserve this ID.
		long nextId = idGenerator.generateNewId();
		assertEquals(idLong+1, nextId);
	}
	
	@Test
	public void testCreateWithIdGreaterThanIdGenerator() throws Exception{
		// Create a node with a specific id
		String id = KeyFactory.keyToString(new Long(idGenerator.generateNewId()+10));
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
	
	
	@Test
	public void testGetPathDoesNotExist() throws Exception {
		// Make sure we get null for a path that does not exist.
		String path = "/fake/should/not/eixst";
		String id = nodeDao.getNodeIdForPath(path);
		assertEquals(null, id);
	}
	
 	// Calling getETagForUpdate() outside of a transaction in not allowed, and will throw an exception.
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
	
	@Test
	public void testUpdateNodeDuplicateName() throws Exception{
		String commonName = "name";
		Node parent = NodeTestUtils.createNew("parent");
		String parentId = nodeDao.createNew(parent);
		toDelete.add(parentId);
		assertNotNull(parentId);
		Node one = NodeTestUtils.createNew(commonName);
		one.setParentId(parentId);
		String id = nodeDao.createNew(one);
		toDelete.add(id);
		assertNotNull(id);
		// Now create another node using this id.
		Node oneDuplicate = NodeTestUtils.createNew("unique");
		oneDuplicate.setParentId(parentId);
		String id2 = nodeDao.createNew(oneDuplicate);
		oneDuplicate = nodeDao.getNode(id2);
		// This should throw an exception.
		try{
			// Set this name to be a duplicate name.
			oneDuplicate.setName(commonName);
			// Now update this node
			nodeDao.updateNode(oneDuplicate);
			fail("Setting a duplicate name should have failed");
		}catch(IllegalArgumentException e){
			System.out.println(e.getMessage());
			assertTrue(e.getMessage().indexOf("An entity with the name: name already exists") > -1);
		}
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
		NamedAnnotations named = nodeDao.getAnnotations(id);
		Annotations annos = named.getAdditionalAnnotations();
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
		nodeDao.updateAnnotations(id, named);
		// Now get a copy and ensure it equals what we sent
		NamedAnnotations namedCopy = nodeDao.getAnnotations(id);
		Annotations copy = namedCopy.getAdditionalAnnotations();
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
		NamedAnnotations named = nodeDao.getAnnotations(id);
		Annotations annos = named.getAdditionalAnnotations();
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
		nodeDao.updateAnnotations(id, named);
		// Now get a copy and ensure it equals what we sent
		NamedAnnotations namedCopy = nodeDao.getAnnotations(id);
		Annotations copy = namedCopy.getAdditionalAnnotations();
		assertNotNull(copy);
		assertEquals(annos, copy);
		// clear an and update
		assertNotNull(copy.getStringAnnotations().remove("stringOne"));
		nodeDao.updateAnnotations(id, namedCopy);
		NamedAnnotations namedCopy2 = nodeDao.getAnnotations(id);
		Annotations copy2 = namedCopy2.getAdditionalAnnotations();
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
		node.setVersionNumber(new Long(0));
		String id = nodeDao.createNew(node);
		toDelete.add(id);
		assertNotNull(id);
		// Load the node
		Node loaded = nodeDao.getNode(id);
		assertNotNull(loaded);
		assertEquals(node.getVersionComment(), loaded.getVersionComment());
		assertEquals(node.getVersionLabel(), loaded.getVersionLabel());
		assertEquals(NodeConstants.DEFAULT_VERSION_NUMBER, loaded.getVersionNumber());
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
	public void testCreateNewVersionNullLabel() throws Exception {
		Node node = NodeTestUtils.createNew("testCreateNewVersion");
		// Start this node with version and comment information
		node.setVersionComment("This is the very first version of this node.");
		node.setVersionLabel("0.0.1");
		node.setVersionNumber(new Long(0));
		String id = nodeDao.createNew(node);
		toDelete.add(id);
		assertNotNull(id);
		// Load the node
		Node loaded = nodeDao.getNode(id);
		assertNotNull(loaded);
		assertEquals(node.getVersionComment(), loaded.getVersionComment());
		assertEquals(node.getVersionLabel(), loaded.getVersionLabel());
		assertEquals(NodeConstants.DEFAULT_VERSION_NUMBER, loaded.getVersionNumber());
		// Now try to create a new version with a null label
		loaded.setVersionLabel(null);
		Long newNumber = nodeDao.createNewVersion(loaded);
		// Since creation of a new version failed we should be back to one version
		loaded = nodeDao.getNode(id);
		assertNotNull(loaded);
		assertEquals(node.getVersionComment(), loaded.getVersionComment());
		assertEquals(KeyFactory.keyToString(newNumber), loaded.getVersionLabel());
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
		NamedAnnotations named = nodeDao.getAnnotations(id);
		Annotations annos = named.getAdditionalAnnotations();
		assertNotNull(annos);
		annos.addAnnotation("string", "value");
		annos.addAnnotation("date", new Date(1));
		annos.addAnnotation("double", 2.3);
		annos.addAnnotation("long", 56l);
		annos.addAnnotation("blob", "Some blob value".getBytes("UTF-8"));
		// Update the annotations
		nodeDao.updateAnnotations(id, named);
		// Now create a new version
		Node copy = nodeDao.getNode(id);
		copy.setVersionComment(null);
		copy.setVersionLabel("1.0.1");
		Long revNumber = nodeDao.createNewVersion(copy);
		assertEquals(new Long(2), revNumber);
		// At this point the new and old version should have the
		// same annotations.
		NamedAnnotations namedCopyV1 = nodeDao.getAnnotationsForVersion(id, 1L);
		Annotations v1Annos = namedCopyV1.getAdditionalAnnotations();
		assertNotNull(v1Annos);
		NamedAnnotations namedCopyV2 = nodeDao.getAnnotationsForVersion(id, 2L);
		Annotations v2Annos = namedCopyV2.getAdditionalAnnotations();
		assertNotNull(v2Annos);
		assertEquals(v1Annos, v2Annos);
		NamedAnnotations namedCopy = nodeDao.getAnnotations(id);
		Annotations currentAnnos = namedCopy.getAdditionalAnnotations();
		assertNotNull(currentAnnos);
		assertEquals(currentAnnos, v2Annos);
		
		// Now update the current annotations
		currentAnnos.getDoubleAnnotations().clear();
		currentAnnos.addAnnotation("double", 8989898.2);
		nodeDao.updateAnnotations(id, namedCopy);
		
		// Now the old and new should no longer match.
		namedCopyV1 = nodeDao.getAnnotationsForVersion(id, 1L);
		v1Annos = namedCopyV1.getAdditionalAnnotations();
		assertNotNull(v1Annos);
		assertEquals(2.3, v1Annos.getSingleValue("double"));
		namedCopyV2 = nodeDao.getAnnotationsForVersion(id, 2L);
		v2Annos = namedCopyV2.getAdditionalAnnotations();
		assertNotNull(v2Annos);
		assertEquals(8989898.2, v2Annos.getSingleValue("double"));
		// The two version should now be out of synch with each other.
		assertFalse(v1Annos.equals(v2Annos));
		// The current annos should still match the v2
		namedCopy = nodeDao.getAnnotations(id);
		currentAnnos = namedCopy.getAdditionalAnnotations();
		assertNotNull(currentAnnos);
		assertEquals(currentAnnos, v2Annos);
		assertEquals(8989898.2, currentAnnos.getSingleValue("double"));
		
		// Node delete the current revision and confirm that the annotations are rolled back
		node = nodeDao.getNode(id);
		nodeDao.deleteVersion(id, node.getVersionNumber());
		NamedAnnotations rolledBackAnnos = nodeDao.getAnnotations(id);
		assertEquals(2.3, rolledBackAnnos.getAdditionalAnnotations().getSingleValue("double"));
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
		parent.setNodeType(EntityType.project.name());
		String parentId = nodeDao.createNew(parent);
		toDelete.add(parentId);
		assertNotNull(parentId);
		// Get the header of this node
		EntityHeader parentHeader = nodeDao.getEntityHeader(parentId);
		assertNotNull(parentHeader);
		assertEquals(EntityType.project.getUrlPrefix(), parentHeader.getType());
		assertEquals("parent", parentHeader.getName());
		assertEquals(parentId, parentHeader.getId());
		
		Node child = NodeTestUtils.createNew("child");
		child.setNodeType(EntityType.dataset.name());
		child.setParentId(parentId);
		String childId = nodeDao.createNew(child);
		toDelete.add(childId);
		assertNotNull(childId);
		// Get the header of this node
		EntityHeader childHeader = nodeDao.getEntityHeader(childId);
		assertNotNull(childHeader);
		assertEquals(EntityType.dataset.getUrlPrefix(), childHeader.getType());
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
		node.setNodeType(EntityType.project.name());
		String parentId = nodeDao.createNew(node);
		toDelete.add(parentId);
		assertNotNull(parentId);
		// Add a child		
		node = NodeTestUtils.createNew("child");
		node.setNodeType(EntityType.dataset.name());
		node.setParentId(parentId);
		String childId = nodeDao.createNew(node);
		toDelete.add(childId);
		assertNotNull(childId);
		// Add a GrandChild		
		node = NodeTestUtils.createNew("grandChild");
		node.setNodeType(EntityType.layer.name());
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
	
	@Test
	public void testGetChildrenList() throws NotFoundException, DatastoreException{
		Node node = NodeTestUtils.createNew("parent");
		node.setNodeType(EntityType.project.name());
		String parentId = nodeDao.createNew(node);
		toDelete.add(parentId);
		assertNotNull(parentId);
		
		// Create a few children
		List<String> childIds = new ArrayList<String>();
		for(int i=0; i<4; i++){
			node = NodeTestUtils.createNew("child"+i);
			node.setNodeType(EntityType.dataset.name());
			node.setParentId(parentId);
			String id = nodeDao.createNew(node);
			childIds.add(id);
		}
		// Now get the list of children
		List<String> fromDao =  nodeDao.getChildrenIdsAsList(parentId);
		// Check that the ids returned have the syn prefix
		assertEquals(childIds, fromDao);
	}
	
	@Test
	public void testUpdateRevision() throws NotFoundException, DatastoreException{
		Node node = NodeTestUtils.createNew("parent");
		node.setNodeType(EntityType.project.name());
		String id = nodeDao.createNew(node);
		toDelete.add(id);
		assertNotNull(id);
		node = nodeDao.getNode(id);
		Long v1Number = node.getVersionNumber();
		// Get the current rev
		NodeRevisionBackup currentRev = nodeBackupDao.getNodeRevision(id, node.getVersionNumber());
		assertNotNull(currentRev);
		// Add some annotations
		String keyOnFirstVersion = "NodeDAOImplTest.testUpdateRevision.OnFirstVersion";
		currentRev.getNamedAnnotations().getAdditionalAnnotations().addAnnotation(keyOnFirstVersion, "newValue");
		currentRev.setLabel("2.0");
		nodeBackupDao.updateRevisionFromBackup(currentRev);
		// Since we added this annotation to the current version it should be query-able
		assertTrue(nodeDao.isStringAnnotationQueryable(id, keyOnFirstVersion));
		
		// Get it back
		NodeRevisionBackup clone = nodeBackupDao.getNodeRevision(id, node.getVersionNumber());
		assertNotNull(clone);
		assertEquals("newValue", clone.getNamedAnnotations().getAdditionalAnnotations().getSingleValue(keyOnFirstVersion));
		assertEquals("2.0", clone.getLabel());
		
		// now create a new version
		node = nodeDao.getNode(id);
		node.setVersionLabel("3.0");
		nodeDao.createNewVersion(node);
		node = nodeDao.getNode(id);
		// Get the latest
		clone = nodeBackupDao.getNodeRevision(id, node.getVersionNumber());
		// Clear the string annoations.
		clone.getNamedAnnotations().getAdditionalAnnotations().setStringAnnotations(new HashMap<String, List<String>>());
		nodeBackupDao.updateRevisionFromBackup(clone);
		// The string annotation should no longer be query-able
		assertFalse(nodeDao.isStringAnnotationQueryable(id, keyOnFirstVersion));
		
		// Finally, update the first version again, adding back the string property
		// but this time since this is not the current version it should not be query-able
		clone = nodeBackupDao.getNodeRevision(id, v1Number);
		clone.getNamedAnnotations().getAdditionalAnnotations().addAnnotation(keyOnFirstVersion, "updatedValue");
		nodeBackupDao.updateRevisionFromBackup(clone);
		// This annotation should not be query-able
		assertFalse(nodeDao.isStringAnnotationQueryable(id, keyOnFirstVersion));
	}
	
	@Test
	public void testCreateRevision() throws NotFoundException, DatastoreException{
		Long currentVersionNumver = new Long(8);
		Node node = NodeTestUtils.createNew("parent");
		// Start with a node already on an advanced version
		node.setVersionNumber(currentVersionNumver);
		node.setVersionComment("Current comment");
		node.setVersionLabel("8.0");
		node.setNodeType(EntityType.project.name());
		String id = nodeDao.createNew(node);
		toDelete.add(id);
		assertNotNull(id);
		node = nodeDao.getNode(id);
		assertEquals("8.0", node.getVersionLabel());
		assertEquals(currentVersionNumver, node.getVersionNumber());
		
		// now create a new version number
		NodeRevisionBackup newRev = new NodeRevisionBackup();
		Annotations annos = new Annotations();
		annos.addAnnotation("stringKey", "StringValue");
		annos.addAnnotation("dateKey", new Date(1000));
		annos.addAnnotation("longKey", new Long(123));
		annos.addAnnotation("doubleKey", new Double(4.5));
		NamedAnnotations nammed = new NamedAnnotations();
		nammed.put("newNamed", annos);
		String keyOnNewVersion = "NodeDAOImplTest.testCreateRevision.OnNew";
		annos.addAnnotation(keyOnNewVersion, "value on new");
		newRev.setNamedAnnotations(nammed);
		Long newVersionNumber = new Long(1);
		newRev.setRevisionNumber(newVersionNumber);
		newRev.setNodeId(id);
		newRev.setLabel("1.0");
		newRev.setModifiedBy("me");
		newRev.setModifiedOn(new Date());
		newRev.setReferences(new HashMap<String, Set<Reference>>());

		// This annotation should not be query-able
		assertFalse(nodeDao.isStringAnnotationQueryable(id, keyOnNewVersion));
		// Now create the version
		nodeBackupDao.createNewRevisionFromBackup(newRev);
		// This annotation should still not be query-able because it is not on the current version.
		assertFalse(nodeDao.isStringAnnotationQueryable(id, keyOnNewVersion));
		
		// Get the older version
		NodeRevisionBackup clone = nodeBackupDao.getNodeRevision(id, newVersionNumber);
		assertNotNull(clone);
		assertEquals("value on new", clone.getNamedAnnotations().getAnnotationsForName("newNamed").getSingleValue(keyOnNewVersion));
		assertEquals("1.0", clone.getLabel());
		assertEquals(newRev, clone);		
	}

	@Test
	public void testAddReferencesNoVersionSpecified() throws Exception {
		String deleteMeNode = null;

		// Create a few nodes we will refer to, use the current version held in the repo svc
		Set<Reference> referees = new HashSet<Reference>();
		Set<Reference> copyReferees = new HashSet<Reference>();
		for(int i=0; i<10; i++){
			Node node = NodeTestUtils.createNew("referee"+i);
			String id = nodeDao.createNew(node);
			toDelete.add(id);

			Reference ref = new Reference();
			ref.setTargetId(id);
			referees.add(ref);
			
			ref = new Reference();
			ref.setTargetId(id);
			copyReferees.add(ref);
			
			deleteMeNode = id;
		}

		// Create our reference map
		Map<String, Set<Reference>> refs = new HashMap<String, Set<Reference>>();
		refs.put("referees", referees);
		
		// Create the node that holds the references
		Node referer = NodeTestUtils.createNew("referer");
		referer.setReferences(refs);
		String refererId = nodeDao.createNew(referer);
		assertNotNull(refererId);
		toDelete.add(refererId);

		// Make sure it got stored okay
		Node storedNode = nodeDao.getNode(refererId);
		assertNotNull(storedNode);
		assertNotNull(storedNode.getReferences());
		assertEquals(1, storedNode.getReferences().size());
		assertEquals(10, storedNode.getReferences().get("referees").size());
		Object[] storedRefs = storedNode.getReferences().get("referees").toArray();
		assertEquals(null, ((Reference)storedRefs[0]).getTargetVersionNumber());
		
		// Make sure our reference Ids have the syn prefix
		for(Reference ref : storedNode.getReferences().get("referees")) {
			assertTrue(toDelete.contains(ref.getTargetId()));
		}
		
		// Now delete one of those nodes, such that one of our references has become 
		// invalid after we've created it.  This is okay and does not cause an error 
		// because we are not enforcing referential integrity.
		nodeDao.delete(deleteMeNode);
	}
	
	@Test 
	public void testUpdateReferences() throws Exception {
		Reference inEvenFirstBatch = null, inOddFirstBatch = null, inEvenSecondBatch = null, inOddSecondBatch = null;
		
		// Create a few nodes we will refer to
		Set<Reference> even = new HashSet<Reference>();
		Set<Reference> odd = new HashSet<Reference>();
		for(int i=1; i<=5; i++){
			Node node = NodeTestUtils.createNew("referee"+i);
			node.setVersionNumber(999L);
			String id = nodeDao.createNew(node);
			toDelete.add(id);
			Reference ref = new Reference();
			ref.setTargetId(id);
			ref.setTargetVersionNumber(node.getVersionNumber());
			if(0 == (i % 2)) {
				even.add(ref);
				inEvenFirstBatch = ref;
			}
			else {
				odd.add(ref);
				inOddFirstBatch = ref;
			}
		}

		// Create our reference map
		Map<String, Set<Reference>> refs = new HashMap<String, Set<Reference>>();
		refs.put("even", even);
		refs.put("odd", odd);
		
		// Create the node that holds the references
		Node referer = NodeTestUtils.createNew("referer");
		referer.setReferences(refs);
		String refererId = nodeDao.createNew(referer);
		assertNotNull(refererId);
		toDelete.add(refererId);

		// Make sure it got stored okay
		Node storedNode = nodeDao.getNode(refererId);
		assertNotNull(storedNode);
		assertNotNull(storedNode.getReferences());
		assertEquals(2, storedNode.getReferences().size());
		assertEquals(2, storedNode.getReferences().get("even").size());
		assertEquals(3, storedNode.getReferences().get("odd").size());
		assertTrue(storedNode.getReferences().get("even").contains(inEvenFirstBatch));
		assertTrue(storedNode.getReferences().get("odd").contains(inOddFirstBatch));
		assertFalse(storedNode.getReferences().get("even").contains(inEvenSecondBatch));
		assertFalse(storedNode.getReferences().get("odd").contains(inOddSecondBatch));

		// Now delete some references
		storedNode.getReferences().get("even").clear();
		// And add a few new ones
		for(int i=1; i<=4; i++){
			Node node = NodeTestUtils.createNew("referee"+i);
			node.setVersionNumber(999L);
			String id = nodeDao.createNew(node);
			toDelete.add(id);
			Reference ref = new Reference();
			ref.setTargetId(id);
			ref.setTargetVersionNumber(node.getVersionNumber());
			if(0 == (i % 2)) {
				storedNode.getReferences().get("even").add(ref);
				inEvenSecondBatch = ref;
			}
			else {
				storedNode.getReferences().get("odd").add(ref);
				inOddSecondBatch = ref;
			}
		}

		// Make sure it got updated okay
		nodeDao.updateNode(storedNode);
		storedNode = nodeDao.getNode(refererId);
		assertNotNull(storedNode);
		assertNotNull(storedNode.getReferences());
		assertEquals(2, storedNode.getReferences().size());
		assertEquals(2, storedNode.getReferences().get("even").size());
		assertEquals(5, storedNode.getReferences().get("odd").size());
		assertFalse(storedNode.getReferences().get("even").contains(inEvenFirstBatch));
		assertTrue(storedNode.getReferences().get("odd").contains(inOddFirstBatch));
		assertTrue(storedNode.getReferences().get("even").contains(inEvenSecondBatch));
		assertTrue(storedNode.getReferences().get("odd").contains(inOddSecondBatch));
		
		// Make sure our reference Ids have the syn prefix
		for(Reference ref : storedNode.getReferences().get("even")) {
			assertTrue(toDelete.contains(ref.getTargetId()));
		}
		for(Reference ref : storedNode.getReferences().get("odd")) {
			assertTrue(toDelete.contains(ref.getTargetId()));
		}

		
		// Now nuke all the references
		storedNode.getReferences().clear();
		nodeDao.updateNode(storedNode);
		storedNode = nodeDao.getNode(refererId);
		assertNotNull(storedNode);
		assertNotNull(storedNode.getReferences());
		assertEquals(0, storedNode.getReferences().size());
	}
	
	/**
	 * Tests that getParentId method returns the Id of a node's parent.
	 * @throws Exception
	 */
	@Test
	public void testGetParentId() throws Exception {
		//make parent project
		Node node = NodeTestUtils.createNew("parent");
		node.setNodeType(EntityType.project.name());
		String parentId = nodeDao.createNew(node);
		toDelete.add(parentId);
		assertNotNull(parentId);
		
		//add a child to the parent	
		node = NodeTestUtils.createNew("child1");
		node.setNodeType(EntityType.dataset.name());
		node.setParentId(parentId);
		String child1Id = nodeDao.createNew(node);
		toDelete.add(child1Id);
		
		// Now get child's parentId
		String answerParentId =  nodeDao.getParentId(child1Id);
		assertEquals(parentId, answerParentId);
	}
	
	/**
	 * Tests that changeNodeParent correctly sets a node's parent to reference
	 * the parentNode sent as a parameter.
	 * @throws Exception
	 */
	@Test
	public void testChangeNodeParent() throws Exception {
		//make a parent project
		Node node = NodeTestUtils.createNew("parentProject");
		node.setNodeType(EntityType.project.name());
		String parentProjectId = nodeDao.createNew(node);
		toDelete.add(parentProjectId);
		assertNotNull(parentProjectId);
		
		//add a child to the parent
		node = NodeTestUtils.createNew("child");
		node.setNodeType(EntityType.dataset.name());
		node.setParentId(parentProjectId);
		String childId = nodeDao.createNew(node);
		toDelete.add(childId);
		assertNotNull(childId);
		
		//make a second project
		node = NodeTestUtils.createNew("newParent");
		node.setNodeType(EntityType.project.name());
		String newParentId = nodeDao.createNew(node);
		toDelete.add(newParentId);
		assertNotNull(newParentId);
		
		//check state of child node before the change
		Node oldNode = nodeDao.getNode(childId);
		assertNotNull(oldNode);
		assertEquals(parentProjectId, oldNode.getParentId());
		
		//change child's parent to newProject
		boolean changeReturn = nodeDao.changeNodeParent(childId, newParentId);
		assertTrue(changeReturn);
		
		Node changedNode = nodeDao.getNode(childId);
		assertNotNull(changedNode);
		assertEquals(newParentId, changedNode.getParentId());		
	}
	
	/**
	 * Tests that changeNodeParent correctly throws a IllegalArgumentException
	 * when the JDONode's parentId is null
	 * @throws Exception
	 */
	@Test(expected = IllegalArgumentException.class)
	public void testChangeNodeParentWhenParentIsNull() throws Exception {
		//make a project
		Node node = NodeTestUtils.createNew("root");
		node.setNodeType(EntityType.project.name());
		String rootId = nodeDao.createNew(node);
		toDelete.add(rootId);
		assertNotNull(rootId);
		
		//make a second project
		node = NodeTestUtils.createNew("newParent");
		node.setNodeType(EntityType.project.name());
		String newParentId = nodeDao.createNew(node);
		toDelete.add(newParentId);
		assertNotNull(newParentId);
		
		Node parent = nodeDao.getNode(rootId);
		assertNull(parent.getParentId());
		nodeDao.changeNodeParent(rootId, newParentId);
	}
	
	/**
	 * Tests that changeNodeParent does nothing if the new parent parameter
	 * is the parent the current node already references
	 * @throws Exception
	 */
	@Test
	public void testChangeNodeParentWhenParamParentIsCurrentParent() throws Exception {
		//make a parent project
		Node node = NodeTestUtils.createNew("parentProject");
		node.setNodeType(EntityType.project.name());
		String parentProjectId = nodeDao.createNew(node);
		toDelete.add(parentProjectId);
		assertNotNull(parentProjectId);
		
		//add a child to the parent
		node = NodeTestUtils.createNew("child");
		node.setNodeType(EntityType.dataset.name());
		node.setParentId(parentProjectId);
		String childId = nodeDao.createNew(node);
		toDelete.add(childId);
		assertNotNull(childId);
		
		//check current state of node
		Node oldNode = nodeDao.getNode(childId);
		assertNotNull(oldNode);
		assertEquals(childId, oldNode.getId());
		assertEquals(parentProjectId, oldNode.getParentId());
		
		//make the parentChange update
		boolean updateReturn = nodeDao.changeNodeParent(childId, parentProjectId);
		assertFalse(updateReturn);
		
		//check new state of node
		Node newNode = nodeDao.getNode(childId);
		assertNotNull(newNode);
		assertEquals(childId, newNode.getId());
		assertEquals(parentProjectId, newNode.getParentId());
	}

	@Test
	public void testReferencesDeleteCurrentVersion() throws NotFoundException, DatastoreException{
		Reference inEven = null, inOdd = null;
		
		// Create a few nodes we will refer to
		Set<Reference> even = new HashSet<Reference>();
		Set<Reference> odd = new HashSet<Reference>();
		for(int i=1; i<=5; i++){
			Node node = NodeTestUtils.createNew("referee"+i);
			node.setVersionNumber(999L);
			String id = nodeDao.createNew(node);
			toDelete.add(id);
			Reference ref = new Reference();
			ref.setTargetId(id);
			ref.setTargetVersionNumber(node.getVersionNumber());
			if(0 == (i % 2)) {
				even.add(ref);
				inEven = ref;
			}
			else {
				odd.add(ref);
				inOdd = ref;
			}
		}

		// Create our reference map
		Map<String, Set<Reference>> refs = new HashMap<String, Set<Reference>>();
		refs.put("even", even);
		refs.put("odd", odd);
		
		// Create the node that holds the references
		Node node = NodeTestUtils.createNew("parent");
		node.setNodeType(EntityType.project.name());
		node.setReferences(refs);
		node.setVersionLabel("references 1.0");
		String id = nodeDao.createNew(node);
		toDelete.add(id);
		assertNotNull(id);
		
		// Get the newly created node
		node = nodeDao.getNode(id);
		Long v1Number = node.getVersionNumber();
		assertTrue(node.getReferences().get("even").contains(inEven));
		assertTrue(node.getReferences().get("odd").contains(inOdd));
		
		// now create a new version and change the references
		node = nodeDao.getNode(id);
		node.getReferences().put("even2", node.getReferences().get("even"));
		node.setVersionLabel("references 2.0");
		nodeDao.createNewVersion(node);

		// Get the updated node
		node = nodeDao.getNode(id);
		// Since we added more references, we should see them in the node and in the revision
		assertTrue(node.getReferences().get("even").contains(inEven));
		assertTrue(node.getReferences().get("odd").contains(inOdd));
		assertTrue(node.getReferences().get("even2").contains(inEven));
		
		// Delete the current version.
		nodeDao.deleteVersion(id, node.getVersionNumber());

		// Get the (rolled back) node and check that the references have been reverted
		node = nodeDao.getNode(id);
		assertTrue(node.getReferences().get("even").contains(inEven));
		assertTrue(node.getReferences().get("odd").contains(inOdd));
		assertNull(node.getReferences().get("even2"));
	}
	
	@Test
	public void testForPLFM_791() throws Exception {
		// In the past annotations, were persisted in the annotations tables.  So when we had large strings we had to store
		// the values as BLOB annotations.  This is no longer the case.  Now all annotations are not persisted as a single 
		// zipped blob on the revision table.  Therefore, we only use the annotations tables for query.  This means there 
		// is no need to have a blob annotations table anymore.  There is no need to store very large strings in the
		// string annotations table since it does not make sense to query for large strings.
		// This test ensures that we can have giant string annotations without any problems.
		//make a parent project
		Node node = NodeTestUtils.createNew("testForPLFM_791");
		node.setNodeType(EntityType.project.name());
		String projectId = nodeDao.createNew(node);
		toDelete.add(projectId);
		assertNotNull(projectId);
		// Now get the annotations of the entity
		NamedAnnotations annos = nodeDao.getAnnotations(projectId);
		assertNotNull(annos);
		assertNotNull(annos.getAdditionalAnnotations());
		// Create a very large string
		byte[] largeArray = new byte[10000];
		byte value = 101;
		Arrays.fill(largeArray, value);
		String largeString = new String(largeArray, "UTF-8");
		String key = "veryLargeString";
		annos.getAdditionalAnnotations().addAnnotation(key, largeString);
		// This update will fail before PLFM-791 is fixed.
		nodeDao.updateAnnotations(projectId, annos);
		// Get the values back
		annos = nodeDao.getAnnotations(projectId);
		assertNotNull(annos);
		assertNotNull(annos.getAdditionalAnnotations());
		// Make sure we can still get the string
		assertEquals(largeString, annos.getAdditionalAnnotations().getSingleValue(key));
	}
	
	@Test
	public void testCreateNodeFromBackup() throws NotFoundException, DatastoreException{
		// This will be our backup node.
		Node backup = NodeTestUtils.createNew("backMeUp");
		backup.setNodeType(EntityType.project.name());
		String id = nodeDao.createNew(backup);
		toDelete.add(id);
		assertNotNull(id);
		// We will use this to do the backup.
		backup = nodeDao.getNode(id);
		// Delete the original node
		nodeDao.delete(id);
		// Change the etag (see: PLFM-845)
		String newEtag = "45";
		backup.setETag(newEtag);
		// Now create the node from the backup
		nodeDao.createNewNodeFromBackup(backup);
		// Get a fresh copy
		Node restored = nodeDao.getNode(id);
		assertNotNull(restored);
		assertEquals(id, restored.getId());
		assertEquals("Failed to set the eTag. See: PLFM-845", newEtag, restored.getETag());
	}
	
	@Test
	public void testUpdateNodeFromBackup() throws NotFoundException, DatastoreException{
		// This will be our backup node.
		Node backup = NodeTestUtils.createNew("backMeUp2");
		backup.setNodeType(EntityType.project.name());
		String id = nodeDao.createNew(backup);
		toDelete.add(id);
		assertNotNull(id);
		// We will use this to do the backup.
		backup = nodeDao.getNode(id);
		// Change the etag (see: PLFM-845)
		String newEtag = "199";
		backup.setETag(newEtag);
		String newDescription = "New description";
		backup.setDescription(newDescription);
		// Now create the node from the backup
		nodeDao.updateNodeFromBackup(backup);
		// The revision should have been deleted.
		assertFalse(nodeDao.doesNodeRevisionExist(id, backup.getVersionNumber()));
		// Get a fresh copy
		try{
			Node restored = nodeDao.getNode(id);
			fail("All revision for this node should have been deleted so this should have failed.");
		}catch (NotFoundException e){
			// This is expected since updating from a backup replaces all revisions.
		}
		// Now create a new revision from backup
		NodeRevisionBackup revBackup  = new NodeRevisionBackup();
		revBackup.setNodeId(id);
		revBackup.setModifiedBy("somebody");
		revBackup.setModifiedOn(new Date(System.currentTimeMillis()));
		revBackup.setRevisionNumber(1l);
		revBackup.setLabel("v1");
		revBackup.setComment("No Comment");
		((NodeBackupDAO)nodeDao).createNewRevisionFromBackup(revBackup);

		Node restored = nodeDao.getNode(id);
		assertNotNull(restored);
		assertEquals(id, restored.getId());
		assertEquals("Failed to set the eTag. See: PLFM-845", newEtag, restored.getETag());
		assertEquals(newDescription, restored.getDescription());
	}
	
	@Test
	public void testGetCurrentRevNumber() throws NotFoundException, DatastoreException{
		Node backup = NodeTestUtils.createNew("withReveNumber");
		backup.setNodeType(EntityType.project.name());
		String id = nodeDao.createNew(backup);
		toDelete.add(id);
		assertNotNull(id);
		Long currentRev = nodeDao.getCurrentRevisionNumber(id);
		assertEquals(NodeConstants.DEFAULT_VERSION_NUMBER, currentRev);
	}
	
	@Test (expected=NotFoundException.class)
	public void testGetCurrentRevNumberDoesNotExist() throws NotFoundException, DatastoreException{
		// This should throw a NotFoundException exception
		Long currentRev = nodeDao.getCurrentRevisionNumber(KeyFactory.keyToString(new Long(-12)));
	}
	
	@Test
	public void testGetAllNodeTypesForAlias(){
		// This should return all entity types.
		List<Short> expected = new ArrayList<Short>();
		for(short i=0; i<EntityType.values().length; i++){
			expected.add(i);
		}
		List<Short> ids = nodeDao.getAllNodeTypesForAlias("entity");
		assertNotNull(ids);
		System.out.println(ids);
		assertEquals(expected, ids);
		
		// Test some of the known types
		ids = nodeDao.getAllNodeTypesForAlias("dataset");
		assertNotNull(ids);
		assertEquals(1, ids.size());
		assertEquals(new Short(EntityType.dataset.getId()), ids.get(0));
		
		ids = nodeDao.getAllNodeTypesForAlias("study");
		assertNotNull(ids);
		assertEquals(1, ids.size());
		assertEquals(new Short(EntityType.dataset.getId()), ids.get(0));
		
		ids = nodeDao.getAllNodeTypesForAlias("layer");
		assertNotNull(ids);
		assertEquals(1, ids.size());
		assertEquals(new Short(EntityType.layer.getId()), ids.get(0));
		
		ids = nodeDao.getAllNodeTypesForAlias("data");
		assertNotNull(ids);
		assertEquals(1, ids.size());
		assertEquals(new Short(EntityType.layer.getId()), ids.get(0));
		
	}
}
