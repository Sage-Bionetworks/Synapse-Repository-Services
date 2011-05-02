package org.sagebionetworks.repo.manager;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.sagebionetworks.repo.model.Annotations;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.InvalidModelException;
import org.sagebionetworks.repo.model.Node;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.web.ConflictingUpdateException;
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
@ContextConfiguration(locations = { "classpath:manager-test-context.xml" })
public class NodeManagerImplAutoWiredTest {
	
	@Autowired
	public NodeManager nodeManager;
	
	List<String> nodesToDelete;
	
	@Before
	public void before(){
		assertNotNull(nodeManager);
		nodesToDelete = new ArrayList<String>();
	}
	
	@After
	public void after(){
		if(nodeManager != null && nodesToDelete != null){
			for(String id: nodesToDelete){
				try {
					nodeManager.delete(null, id);
				} catch (Exception e) {
					// TODO Auto-generated catch block
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
		newNode.setType("someType");
		String id = nodeManager.createNewNode(newNode, null);
		assertNotNull(id);
		nodesToDelete.add(id);
		//Make sure we can get the node
		Node fetched = nodeManager.get(null, id);
		assertNotNull(fetched);
		assertEquals(NodeManagerImpl.ANNONYMOUS, fetched.getCreatedBy());
		assertEquals(NodeManagerImpl.ANNONYMOUS, fetched.getModifiedBy());
		assertNotNull(fetched.getCreatedOn());
		assertNotNull(fetched.getModifiedOn());
		assertEquals(id, fetched.getId());
		assertEquals(newNode.getName(), fetched.getName());
		assertEquals(newNode.getType(), fetched.getType());
		assertNotNull(fetched.geteTag());
		
		// Now try to update the node
		String startingETag = fetched.geteTag();
		fetched.setName("mySecondName");
		Node updated = nodeManager.update(null, fetched);
		assertNotNull(updated);
		// Make sure the result has a new eTag
		assertFalse(startingETag.equals(updated.geteTag()));
		// Now get it again
		Node fetchedAgain = nodeManager.get(null, id);
		assertNotNull(fetchedAgain);
		assertEquals("mySecondName", fetchedAgain.getName());
		assertEquals(updated.geteTag(), fetchedAgain.geteTag());

	}
	
	@Test
	public void testUpdateAnnotations() throws DatastoreException, InvalidModelException, NotFoundException, UnauthorizedException, ConflictingUpdateException{
		// Create a node
		Node newNode = new Node();
		newNode.setName("NodeManagerImplAutoWiredTest.testUpdateAnnotations");
		newNode.setType("someType");
		String id = nodeManager.createNewNode(newNode, null);
		assertNotNull(id);
		nodesToDelete.add(id);
		// First get the annotations for this node
		Annotations annos = nodeManager.getAnnotations(null, id);
		assertNotNull(annos);
		assertNotNull(annos.getEtag());
		String eTagBeforeUpdate = annos.getEtag();
		long before = Long.parseLong(eTagBeforeUpdate);
		String expectedEtagAfterUpdate = new Long(++before).toString();
		// Add some values
		annos.addAnnotation("longKey", new Long(1));
		// Now update the node
		Annotations updated = nodeManager.updateAnnotations(null, id, annos);
		assertNotNull(updated);
		Annotations copy = nodeManager.getAnnotations(null, id);
		assertEquals(updated,copy);
		// Make sure the eTag has changed
		assertEquals(expectedEtagAfterUpdate, copy.getEtag());
		Node nodeCopy = nodeManager.get(null, id);
		assertNotNull(nodeCopy);
		assertNotNull(nodeCopy.geteTag());
		assertEquals(expectedEtagAfterUpdate, nodeCopy.geteTag().toString());
	}
	

}
