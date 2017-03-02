package org.sagebionetworks.repo.model.jdo;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.sagebionetworks.repo.model.AccessControlList;
import org.sagebionetworks.repo.model.AccessControlListDAO;
import org.sagebionetworks.repo.model.AuthorizationConstants.BOOTSTRAP_PRINCIPAL;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.Node;
import org.sagebionetworks.repo.model.NodeDAO;
import org.sagebionetworks.repo.model.NodeInheritanceDAO;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.util.AccessControlListUtil;
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
	
	@Autowired
	private AccessControlListDAO aclDao;
	
	
	// the datasets that must be deleted at the end of each test.
	private List<String> toDelete = new ArrayList<String>();
	Map<String, ObjectType> aclsToDelete;
	Long creatorUserGroupId;
	UserInfo user;
	
	@Before
	public void before(){
		assertNotNull(nodeDao);
		assertNotNull(nodenheritanceDao);
		toDelete = new ArrayList<String>();
		creatorUserGroupId = BOOTSTRAP_PRINCIPAL.THE_ADMIN_USER.getPrincipalId();
		user = new UserInfo(false, creatorUserGroupId);
		aclsToDelete = new HashMap<String, ObjectType>(0);
	}
	
	@After
	public void after() throws DatastoreException{
		if(toDelete != null && nodeDao != null){
			for(String id:  toDelete){
				// Delete each
				try{
					nodeDao.delete(id);
				}catch (Exception e) {}
			}
		}
		if(aclsToDelete != null){
			for(String id: aclsToDelete.keySet()){
				try{
					aclDao.delete(id, aclsToDelete.get(id));
				}catch(Exception e){}
			}
		}
	}
	
	@Test
	public void testCrud() throws Exception{
		// First create a node
		Node toCreate = NodeTestUtils.createNew("nodeInheritanceDaoTest", creatorUserGroupId);
		String parentId = nodeDao.createNew(toCreate);
		toDelete.add(parentId);
		assertNotNull(parentId);
		// New nodes should inherit from themselves
		String benefactor = nodenheritanceDao.getBenefactorCached(parentId);
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
		benefactor = nodenheritanceDao.getBenefactorCached(childId);
		assertEquals(parentId, benefactor);
		String etagBefore = nodeDao.getNode(childId).getETag();
		assertNotNull(etagBefore);
		// Now add this child to the parent
		nodenheritanceDao.addBeneficiary(childId, parentId);
		String etagAfter = nodeDao.getNode(childId).getETag();
		assertFalse("Calling addBeneficiary() should unconditionally change the etag of the node",etagBefore.equals(etagAfter));
		// Check the change.
		benefactor = nodenheritanceDao.getBenefactorCached(childId);
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
		benefactor = nodenheritanceDao.getBenefactorCached(childId);
		assertEquals(parentId, benefactor);
		
		// Make sure we can delete the parent
		nodeDao.delete(parentId);
	}
	
	@Test
	public void testDoesNodeExist(){
		Node node = NodeTestUtils.createNew("node", creatorUserGroupId);
		node = nodeDao.createNewNode(node);
		toDelete.add(node.getId());
		assertTrue(nodenheritanceDao.doesNodeExist(node.getId()));
		assertFalse(nodenheritanceDao.doesNodeExist("syn9999"));
	}
	
	@Test (expected=NotFoundException.class)
	public void testGetBenefactorEntityDoesNotExist(){
		// should not exist
		nodenheritanceDao.getBenefactor("syn9999");
	}
	
	@Test
	public void testGetBenefactorSelf(){
		// create a parent
		Node grandparent = NodeTestUtils.createNew("grandparent", creatorUserGroupId);
		grandparent = nodeDao.createNewNode(grandparent);
		toDelete.add(grandparent.getId());
		
		// There is no ACL for this node
		try{
			nodenheritanceDao.getBenefactor(grandparent.getId());
			fail("Does not have a benefactor");
		}catch(IllegalStateException expected){
			// expected
		}
		AccessControlList acl = AccessControlListUtil.createACLToGrantEntityAdminAccess(grandparent.getId(), user, new Date());
		// create an ACL with the same ID but wrong type.
		aclDao.create(acl, ObjectType.EVALUATION);
		aclsToDelete.put(grandparent.getId(), ObjectType.EVALUATION);
		try{
			nodenheritanceDao.getBenefactor(grandparent.getId());
			fail("Does not have a benefactor");
		}catch(IllegalStateException expected){
			// expected
		}
		// Create an ACL with the correct type
		aclDao.create(acl, ObjectType.ENTITY);
		aclsToDelete.put(grandparent.getId(), ObjectType.ENTITY);
		
		String benefactor = nodenheritanceDao.getBenefactor(grandparent.getId());
		assertEquals("Entity should be its own benefactor",grandparent.getId(), benefactor);
	}
	
	@Test
	public void testGetBenefactorNotSelf(){
		// Setup some hierarchy.
		// grandparent
		Node grandparent = NodeTestUtils.createNew("grandparent", creatorUserGroupId);
		grandparent = nodeDao.createNewNode(grandparent);
		toDelete.add(grandparent.getId());
		// parent
		Node parent = NodeTestUtils.createNew("parent", creatorUserGroupId);
		parent.setParentId(grandparent.getId());
		parent = nodeDao.createNewNode(parent);
		toDelete.add(parent.getId());
		// child
		Node child = NodeTestUtils.createNew("child", creatorUserGroupId);
		child.setParentId(parent.getId());
		child = nodeDao.createNewNode(child);
		toDelete.add(child.getId());
		// benefactor does not exist yet
		try{
			nodenheritanceDao.getBenefactor(child.getId());
			fail("Does not have a benefactor");
		}catch(IllegalStateException expected){
			// expected
		}
		// add an ACL on the grandparent.
		AccessControlList acl = AccessControlListUtil.createACLToGrantEntityAdminAccess(grandparent.getId(), user, new Date());
		aclDao.create(acl, ObjectType.ENTITY);
		aclsToDelete.put(grandparent.getId(), ObjectType.ENTITY);
		// The benefactor of each should the grandparent.
		assertEquals(grandparent.getId(), nodenheritanceDao.getBenefactor(child.getId()));
		assertEquals(grandparent.getId(), nodenheritanceDao.getBenefactor(parent.getId()));
		assertEquals(grandparent.getId(), nodenheritanceDao.getBenefactor(grandparent.getId()));
	}
	
}
