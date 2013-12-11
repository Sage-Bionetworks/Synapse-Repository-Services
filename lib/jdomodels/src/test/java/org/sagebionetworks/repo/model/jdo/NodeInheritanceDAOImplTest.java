package org.sagebionetworks.repo.model.jdo;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.sagebionetworks.repo.model.AuthorizationConstants.BOOTSTRAP_PRINCIPAL;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.Node;
import org.sagebionetworks.repo.model.NodeDAO;
import org.sagebionetworks.repo.model.NodeInheritanceDAO;
import org.sagebionetworks.repo.web.NotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;


@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath:jdomodels-test-context.xml" })
public class NodeInheritanceDAOImplTest {

	@Autowired
	private NodeDAO nodeDao;
	
	@Autowired
	private NodeInheritanceDAO nodenheritanceDao;
	
	// the datasets that must be deleted at the end of each test.
	private List<String> toDelete = new ArrayList<String>();
	
	@Before
	public void before(){
		assertNotNull(nodeDao);
		assertNotNull(nodenheritanceDao);
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
	
	@Test
	public void testCrud() throws Exception{
		Long creatorUserGroupId = BOOTSTRAP_PRINCIPAL.THE_ADMIN_USER.getPrincipalId();
		// First create a node
		Node toCreate = NodeTestUtils.createNew("nodeInheritanceDaoTest", creatorUserGroupId);
		String parentId = nodeDao.createNew(toCreate);
		toDelete.add(parentId);
		assertNotNull(parentId);
		// New nodes should inherit from themselves
		String benefactor = nodenheritanceDao.getBenefactor(parentId);
		assertEquals(parentId, benefactor);
		// Do the reverse
		Set<String> beneficiaries = nodenheritanceDao.getBeneficiaries(parentId);
		assertNotNull(beneficiaries);
		assertEquals(1, beneficiaries.size());
		// Create a new child node
		Node child = NodeTestUtils.createNew("nodeInheritanceDaoTestChild", creatorUserGroupId);
		child.setParentId(parentId);
		String childId = nodeDao.createNew(child);
		toDelete.add(childId);
		assertNotNull(childId);
		benefactor = nodenheritanceDao.getBenefactor(childId);
		assertEquals(parentId, benefactor);
		String etagBefore = nodeDao.getNode(childId).getETag();
		assertNotNull(etagBefore);
		// Now add this child to the parent
		nodenheritanceDao.addBeneficiary(childId, parentId);
		String etagAfter = nodeDao.getNode(childId).getETag();
		assertFalse("Calling addBeneficiary() should unconditionally change the etag of the node",etagBefore.equals(etagAfter));
		// Check the change.
		benefactor = nodenheritanceDao.getBenefactor(childId);
		assertEquals(parentId, benefactor);
		// Make sure we can get the beneficiaries from the parent
		beneficiaries = nodenheritanceDao.getBeneficiaries(parentId);
		assertNotNull(beneficiaries);
		assertEquals(2, beneficiaries.size());
		assertTrue(beneficiaries.contains(parentId));
		assertTrue(beneficiaries.contains(childId));
		
		// Now add this child to the parent
		// Do not change the etag this time
		etagBefore = nodeDao.getNode(childId).getETag();
		assertNotNull(etagBefore);
		boolean keepOldEtag = true;
		nodenheritanceDao.addBeneficiary(childId, parentId, keepOldEtag);
		etagAfter = nodeDao.getNode(childId).getETag();
		assertEquals("Calling addBeneficiary() with keepOldEtag=true should not have changed the etag",etagBefore ,etagAfter);
		// Check the change.
		benefactor = nodenheritanceDao.getBenefactor(childId);
		assertEquals(parentId, benefactor);
		
		// Make sure we can delete the parent
		nodeDao.delete(parentId);
	}
	
}
