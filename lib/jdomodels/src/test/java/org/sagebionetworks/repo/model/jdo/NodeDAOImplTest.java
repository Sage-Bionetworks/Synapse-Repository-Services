package org.sagebionetworks.repo.model.jdo;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.sagebionetworks.repo.model.Annotations;
import org.sagebionetworks.repo.model.Node;
import org.sagebionetworks.repo.model.NodeDAO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.orm.jdo.JdoObjectRetrievalFailureException;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.IllegalTransactionStateException;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath:jdomodles-test-context.xml" })
public class NodeDAOImplTest {

	@Autowired
	NodeDAO nodeDao;
	
	// the datasets that must be deleted at the end of each test.
	List<String> toDelete = new ArrayList<String>();
	
	@Before
	public void before(){
		assertNotNull(nodeDao);
		toDelete = new ArrayList<String>();
	}
	
	@After
	public void after(){
		if(toDelete != null && nodeDao != null){
			for(String id:  toDelete){
				// Delete each
				try{
					nodeDao.delete(id);
				}catch (JdoObjectRetrievalFailureException e){
					// happens if the object no longer exists.
				}
			}
		}
	}
	
	@Test 
	public void testCreateNode(){
		Node toCreate = Node.createNew("firstNodeEver");
		String id = nodeDao.createNew(toCreate);
		toDelete.add(id);
		assertNotNull(id);
		// Make sure we can fetch it
		Node loaded = nodeDao.getNode(id);
		assertNotNull(id);
		assertEquals(id, loaded.getId());
		assertNotNull(loaded.getETag());
	}
	
	@Test 
	public void testAddChild(){
		Node parent = Node.createNew("parent");
		String parentId = nodeDao.createNew(parent);
		assertNotNull(parentId);
		toDelete.add(parentId);
		//Now add an child
		Node child = Node.createNew("child");
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
		
		// Now delete the parent and confirm the child is gone too
		nodeDao.delete(parentId);
		// the child should no longer exist
		try{
			childLoaded = nodeDao.getNode(childId);
			fail("The child should not exist after the parent was deleted");
		}catch (JdoObjectRetrievalFailureException e){
			// expected.
		}
	}
	
	/**
	 * Calling getETagForUpdate() outside of a transaction in not allowed, and will throw an exception.
	 */
	@Test(expected=IllegalTransactionStateException.class)
	public void testGetETagForUpdate(){
		Node toCreate = Node.createNew("testGetETagForUpdate");
		String id = nodeDao.createNew(toCreate);
		toDelete.add(id);
		assertNotNull(id);
		Long eTag = nodeDao.getETagForUpdate(id);
		fail("Should have thrown an IllegalTransactionStateException");
	}
	
	@Test
	public void testUpdateNode(){
		Node node = Node.createNew("testUpdateNode");
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
	public void testCreateAnnotations(){
		Node node = Node.createNew("testCreateAnnotations");
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
		assertEquals(annos, copy);
		// Make sure the node has a new eTag
		Node nodeCopy = nodeDao.getNode(id);
		assertNotNull(nodeCopy);
		assertEquals(newETagString, nodeCopy.getETag());
	}
	
}
