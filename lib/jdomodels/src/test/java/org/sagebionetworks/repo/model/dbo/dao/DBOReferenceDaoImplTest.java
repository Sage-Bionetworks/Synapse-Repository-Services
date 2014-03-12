package org.sagebionetworks.repo.model.dbo.dao;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.sagebionetworks.repo.model.ACCESS_TYPE;
import org.sagebionetworks.repo.model.AccessControlList;
import org.sagebionetworks.repo.model.AccessControlListDAO;
import org.sagebionetworks.repo.model.AuthorizationConstants.BOOTSTRAP_PRINCIPAL;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.EntityHeader;
import org.sagebionetworks.repo.model.EntityType;
import org.sagebionetworks.repo.model.Node;
import org.sagebionetworks.repo.model.NodeDAO;
import org.sagebionetworks.repo.model.NodeInheritanceDAO;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.QueryResults;
import org.sagebionetworks.repo.model.Reference;
import org.sagebionetworks.repo.model.ResourceAccess;
import org.sagebionetworks.repo.model.UserGroup;
import org.sagebionetworks.repo.model.UserGroupDAO;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.dbo.persistence.DBONode;
import org.sagebionetworks.repo.model.jdo.KeyFactory;
import org.sagebionetworks.repo.model.jdo.NodeTestUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath:jdomodels-test-context.xml" })
public class DBOReferenceDaoImplTest {

	private static final int NODE_COUNT = 2;
	
	@Autowired
	private DBOReferenceDao dboReferenceDao;
	
	@Autowired
	private NodeDAO nodeDAO;

	@Autowired
	private UserGroupDAO userGroupDAO;

	@Autowired
	private AccessControlListDAO aclDAO;
	
	@Autowired
	private NodeInheritanceDAO nodeInheritanceDao;
	
	private List<String> groupsToDelete;
	private String aclIdToDelete;
	private List<String> nodesToDelete;
	
	// This is the node we will add refrence too.
	private DBONode node;

	@After
	public void after() throws Exception {
		for (String id : nodesToDelete) {
			nodeDAO.delete(id);
		}
		
		for (String todelete : groupsToDelete) {
			userGroupDAO.delete(todelete);
		}

		if (aclIdToDelete != null) {
			aclDAO.delete(aclIdToDelete, ObjectType.ENTITY);
		}
	}
	
	
	@Before
	public void before() throws Exception {
		nodesToDelete = new ArrayList<String>();
		groupsToDelete = new ArrayList<String>();
		
		// Create a node to create revisions of.
		node = new DBONode();
		long createdById = BOOTSTRAP_PRINCIPAL.THE_ADMIN_USER.getPrincipalId();
		for (int i = 0; i < NODE_COUNT; i++) {
			Node dto = NodeTestUtils.createNew("DBOReferenceDAOImplTest node " + i, createdById);
			String id = nodeDAO.createNew(dto);
			nodesToDelete.add(id);
			node.setId(KeyFactory.stringToKey(id));
		}
	}
	
	@Test
	public void testRoundTrip() throws DatastoreException{
		// Build up two groups
		Map<String, Set<Reference>> references = new HashMap<String, Set<Reference>>();
		Set<Reference> one = new HashSet<Reference>();
		Set<Reference> two = new HashSet<Reference>();
		references.put("groupOne", one);
		references.put("groupTwo", two);
		// Add one to one
		Reference ref = new Reference();
		ref.setTargetId(KeyFactory.keyToString(123L));
		ref.setTargetVersionNumber(new Long(1));
		one.add(ref);
		// Add one to two
		 ref = new Reference();
		ref.setTargetId(KeyFactory.keyToString(456L));
		ref.setTargetVersionNumber(new Long(0));
		two.add(ref);
		// Now save to the DB
		dboReferenceDao.replaceReferences(node.getId(), references);
		// Now fetch them back
		Map<String, Set<Reference>> clone = dboReferenceDao.getReferences(node.getId());
		assertEquals(references, clone);
		
		// Make sure our returned ids have the syn prefix
		assertEquals(KeyFactory.keyToString(123L), clone.get("groupOne").iterator().next().getTargetId());
		assertEquals(KeyFactory.keyToString(456L), clone.get("groupTwo").iterator().next().getTargetId());
		
		// Now change the values and make sure we can replace them.
		clone.remove("groupOne");
		Set<Reference> three = new HashSet<Reference>();
		clone.put("groupThree", three);
		// Add one to two
		ref = new Reference();
		ref.setTargetId(KeyFactory.keyToString(789L));
		ref.setTargetVersionNumber(new Long(10));
		three.add(ref);
		// Replace them 
		dboReferenceDao.replaceReferences(node.getId(), clone);
		Map<String, Set<Reference>> clone2 = dboReferenceDao.getReferences(node.getId());
		assertEquals(clone, clone2);
		
		// Make sure our returned ids have the syn prefix
		assertEquals(KeyFactory.keyToString(456L), clone2.get("groupTwo").iterator().next().getTargetId());
		assertEquals(KeyFactory.keyToString(789L), clone2.get("groupThree").iterator().next().getTargetId());

		// Clear
		dboReferenceDao.deleteReferencesByOwnderId(node.getId());
		Map<String, Set<Reference>> clone3 = dboReferenceDao.getReferences(node.getId());
		assertNull(clone3.get("groupOne"));
		assertNull(clone3.get("groupTwo"));
		assertNull(clone3.get("groupThree"));
	}
	
	@Test
	public void testNullTargetRevNumber() throws DatastoreException{
		// Build up two groups
		Map<String, Set<Reference>> references = new HashMap<String, Set<Reference>>();
		Set<Reference> one = new HashSet<Reference>();
		Set<Reference> two = new HashSet<Reference>();
		references.put("groupOne", one);
		// Add one to one
		Reference ref = new Reference();
		ref.setTargetId(KeyFactory.keyToString(123L));
		ref.setTargetVersionNumber(null);
		one.add(ref);
		// Add one to two
		 ref = new Reference();
		ref.setTargetId(KeyFactory.keyToString(456L));
		ref.setTargetVersionNumber(new Long(12));
		two.add(ref);
		// Now save to the DB
		dboReferenceDao.replaceReferences(node.getId(), references);
		// Now fetch them back
		Map<String, Set<Reference>> clone = dboReferenceDao.getReferences(node.getId());
		assertEquals(references, clone);
	}
	
	@Test
	public void testUnique() throws DatastoreException{
		// Build up two groups
		Map<String, Set<Reference>> references = new HashMap<String, Set<Reference>>();
		Set<Reference> one = new HashSet<Reference>();
		references.put("groupOne", one);
		// Add one to one
		Reference ref = new Reference();
		ref.setTargetId(KeyFactory.keyToString(123L));
		ref.setTargetVersionNumber(new Long(12));
		one.add(ref);
		// Add one to two
		ref = new Reference();
		ref.setTargetId(KeyFactory.keyToString(123L));
		ref.setTargetVersionNumber(new Long(12));
		one.add(ref);
		// The set is actually enforcing this for us.
		assertEquals(1, one.size());
		// Now save to the DB
		dboReferenceDao.replaceReferences(node.getId(), references);
		// Now fetch them back
		Map<String, Set<Reference>> clone = dboReferenceDao.getReferences(node.getId());
		assertEquals(references, clone);
	}
	
	@Test
	public void testUniqueNull() throws DatastoreException{
		// Build up two groups
		Map<String, Set<Reference>> references = new HashMap<String, Set<Reference>>();
		Set<Reference> one = new HashSet<Reference>();
		references.put("groupOne", one);
		// Add one to one
		Reference ref = new Reference();
		ref.setTargetId(KeyFactory.keyToString(123L));
		ref.setTargetVersionNumber(null);
		one.add(ref);
		// Add one to two
		ref = new Reference();
		ref.setTargetId(KeyFactory.keyToString(123L));
		ref.setTargetVersionNumber(null);
		one.add(ref);
		// The set is actually enforcing this for us.
		assertEquals(1, one.size());
		// Now save to the DB
		dboReferenceDao.replaceReferences(node.getId(), references);
		// Now fetch them back
		Map<String, Set<Reference>> clone = dboReferenceDao.getReferences(node.getId());
		assertEquals(references, clone);
	}
	
	@Test
	public void testUniqueMixed() throws DatastoreException{
		// Build up two groups
		Map<String, Set<Reference>> references = new HashMap<String, Set<Reference>>();
		Set<Reference> one = new HashSet<Reference>();
		references.put("groupOne", one);
		// Add one to one
		Reference ref = new Reference();
		ref.setTargetId(KeyFactory.keyToString(123L));
		ref.setTargetVersionNumber(new Long(1));
		one.add(ref);
		// Add one to two
		ref = new Reference();
		ref.setTargetId(KeyFactory.keyToString(123L));
		ref.setTargetVersionNumber(null);
		one.add(ref);
		// The set is actually enforcing this for us.
		assertEquals(2, one.size());
		// Now save to the DB
		dboReferenceDao.replaceReferences(node.getId(), references);
		// Now fetch them back
		Map<String, Set<Reference>> clone = dboReferenceDao.getReferences(node.getId());
		assertEquals(references, clone);
	}
	
	private static Set<String> justIds(Collection<EntityHeader> ehs) throws DatastoreException {
		Set<String> ans = new HashSet<String>();
		for (EntityHeader eh : ehs) ans.add(eh.getId());
		return ans;
	}
	
	@Test
	public void testReferrersQuery() throws Exception {
		// get two nodes
		assertTrue(nodesToDelete.size() >= 2);
		Iterator<String> it = nodesToDelete.iterator();
		
		// This node needs more fields than just the ID
		DBONode node0 = new DBONode();
		String node0Id = it.next();
		node0.setId(KeyFactory.stringToKey(node0Id));
		Node temp = nodeDAO.getNode(node0Id);
		node0.setName(temp.getName());
		node0.setNodeType(EntityType.valueOf(temp.getNodeType()).getId());
		
		DBONode node1 = new DBONode();
		node1.setId(KeyFactory.stringToKey(it.next()));
		
		// create a node having references
		{
			Map<String, Set<Reference>> references = new HashMap<String, Set<Reference>>();
			Set<Reference> one = new HashSet<Reference>();
			Set<Reference> two = new HashSet<Reference>();
			references.put("groupOne", one);
			references.put("groupTwo", two);
			// Add one to one
			Reference ref = new Reference();
			ref.setTargetId(KeyFactory.keyToString(123L));
			ref.setTargetVersionNumber(new Long(1));
			one.add(ref);
			// Add one to two
			 ref = new Reference();
			ref.setTargetId(KeyFactory.keyToString(456L));
			ref.setTargetVersionNumber(new Long(0));
			two.add(ref);
			// Now save to the DB
			dboReferenceDao.replaceReferences(node0.getId(), references);
		}
		
		// now create a node that refers to just one
		{
			Map<String, Set<Reference>> references = new HashMap<String, Set<Reference>>();
			Set<Reference> one = new HashSet<Reference>();
			references.put("groupOne", one);
			// Add one to one
			Reference ref = new Reference();
			ref.setTargetId(KeyFactory.keyToString(123L));
			ref.setTargetVersionNumber(new Long(1));
			one.add(ref);
			// Now save to the DB
			dboReferenceDao.replaceReferences(node1.getId(), references);
		}

		UserInfo userInfo = new UserInfo(true/*is admin*/);
		// both should refer to id=123
		QueryResults<EntityHeader> ehqr = dboReferenceDao.getReferrers(123L, null, userInfo, null, null);
		long count = ehqr.getTotalNumberOfResults();
		Collection<EntityHeader> referrers = ehqr.getResults();
		Set<String> expected = new HashSet<String>(); 
		// Make sure our referrers have the syn prefix
		expected.add(KeyFactory.keyToString(node0.getId())); expected.add(KeyFactory.keyToString(node1.getId()));
		assertEquals(2, count);
		assertEquals(expected, justIds(referrers));
		// but just one refers to id=456
		ehqr = dboReferenceDao.getReferrers(456L, null, userInfo, null, null);
		count = ehqr.getTotalNumberOfResults();
		referrers = ehqr.getResults();
		expected = new HashSet<String>(); 
		expected.add(KeyFactory.keyToString(node0.getId()));
		assertEquals(1, count);
		assertEquals(expected, justIds(referrers));
		EntityHeader eh = referrers.iterator().next();
		assertEquals(node0.getName(), eh.getName());
		assertEquals(EntityType.getTypeForId(node0.getNodeType()).name(), eh.getType());
		
		
		// now make the second node refer to a different revision of 123
		{
			Map<String, Set<Reference>> references = new HashMap<String, Set<Reference>>();
			Set<Reference> one = new HashSet<Reference>();
			references.put("groupOne", one);
			// Add one to one
			Reference ref = new Reference();
			ref.setTargetId(KeyFactory.keyToString(123L));
			ref.setTargetVersionNumber(new Long(2));
			one.add(ref);
			// Now save to the DB
			dboReferenceDao.replaceReferences(node1.getId(), references);
		}
		
		// only the first node refers to revision 1
		ehqr = dboReferenceDao.getReferrers(123L, 1, userInfo, null, null);
		count = ehqr.getTotalNumberOfResults();
		referrers = ehqr.getResults();
		expected = new HashSet<String>(); 
		expected.add(KeyFactory.keyToString(node0.getId()));
		assertEquals(1, count);
		assertEquals(expected, justIds(referrers));
		
		// only the second node refers to revision 2
		ehqr = dboReferenceDao.getReferrers(123L, 2, userInfo, null, null);
		count = ehqr.getTotalNumberOfResults();
		referrers = ehqr.getResults();
		expected = new HashSet<String>(); 
		expected.add(KeyFactory.keyToString(node1.getId()));
		assertEquals(1, count);
		assertEquals(expected, justIds(referrers));
		
		// if we are revision-agnostic, both should still refer to id=123
		ehqr = dboReferenceDao.getReferrers(123L, null, userInfo, null, null);
		count = ehqr.getTotalNumberOfResults();
		referrers = ehqr.getResults();
		expected = new HashSet<String>(); 
		expected.add(KeyFactory.keyToString(node0.getId())); expected.add(KeyFactory.keyToString(node1.getId()));
		assertEquals(2, count);
		assertEquals(expected, justIds(referrers));
		
		// ask for a specific version when the *reference* leaves the version blank:
		//    make the second node refer to a different revision of 123
		{
			Map<String, Set<Reference>> references = new HashMap<String, Set<Reference>>();
			Set<Reference> one = new HashSet<Reference>();
			references.put("groupOne", one);
			// Add one to one
			Reference ref = new Reference();
			ref.setTargetId(KeyFactory.keyToString(123L));
			//ref.setTargetVersionNumber(new Long(2)); <<<< LEAVE IT BLANK
			one.add(ref);
			// Now save to the DB
			dboReferenceDao.replaceReferences(node1.getId(), references);
		}
		
		// only the first node refers to revision 1.
		ehqr = dboReferenceDao.getReferrers(123L, 1, userInfo, null, null);
		count = ehqr.getTotalNumberOfResults();
		referrers = ehqr.getResults();
		expected = new HashSet<String>(); 
		expected.add(KeyFactory.keyToString(node0.getId()));
		assertEquals(1, count);
		assertEquals(expected, justIds(referrers));

		// check authorization
		UserGroup group = new UserGroup();
		group.setIsIndividual(false);
		String groupId = userGroupDAO.create(group).toString();
		group.setId(groupId);
		groupsToDelete.add(groupId);

		// create an ACL for node0
		AccessControlList acl = new AccessControlList();
		acl.setId(""+node0.getId());
		acl.setCreationDate(new Date());
		// add the group to the ACL
		Set<ResourceAccess> ras = new HashSet<ResourceAccess>();
		ResourceAccess ra = new ResourceAccess();
		Set<ACCESS_TYPE> accessTypes = new HashSet<ACCESS_TYPE>();
		accessTypes.add(ACCESS_TYPE.READ);
		ra.setAccessType(accessTypes);
		ra.setPrincipalId(Long.parseLong(groupId));
		ras.add(ra);
		acl.setResourceAccess(ras);
		String aclId = aclDAO.create(acl, ObjectType.ENTITY);
		acl = aclDAO.get(aclId, ObjectType.ENTITY);
		// add acl to a list of objects to delete
		aclIdToDelete = acl.getId();
		assertEquals(""+node0.getId()+"!="+acl.getId(), ""+node0.getId(), aclId);
		
		// add the group to the userInfo
		userInfo = new UserInfo(false); // not an administrator!
		Set<Long> userGroups = new HashSet<Long>();
		userGroups.add(Long.parseLong(group.getId()));
		userInfo.setGroups(userGroups);
		
		// check that permissions are set up.  'userInfo' should be able to get node0 but not node1
		String permissionsBenefactor0 = nodeInheritanceDao.getBenefactor(""+node0.getId());
		// node0 is its own permissions supplier
		assertEquals(""+node0.getId()+"!="+permissionsBenefactor0, ""+KeyFactory.keyToString(node0.getId()), permissionsBenefactor0);
		AccessControlList acl2 = aclDAO.get(""+node0.getId(), ObjectType.ENTITY);
		assertNotNull(acl2);
		Set<ResourceAccess> ras2 = acl2.getResourceAccess();
		assertEquals(1, ras2.size());
		ResourceAccess ra2 = ras2.iterator().next();
		assertEquals(groupId, ra2.getPrincipalId().toString());
		assertTrue(aclDAO.canAccess(userInfo.getGroups(), permissionsBenefactor0, ObjectType.ENTITY, ACCESS_TYPE.READ));
		String permissionsBenefactor1 = nodeInheritanceDao.getBenefactor(""+node1.getId());
		// node1 is its own permissions supplier
		assertEquals(""+node1.getId()+"!="+permissionsBenefactor1, KeyFactory.keyToString(node1.getId()), permissionsBenefactor1);
		assertFalse(aclDAO.canAccess(userInfo.getGroups(), permissionsBenefactor1, ObjectType.ENTITY, ACCESS_TYPE.READ));
		
		// now should only find referrers which allow the created group
		ehqr = dboReferenceDao.getReferrers(123L, null, userInfo, null, null);
		count = ehqr.getTotalNumberOfResults();
		referrers = ehqr.getResults();
		expected = new HashSet<String>(); 
		expected.add(KeyFactory.keyToString(node0.getId()));
		assertEquals(expected, justIds(referrers));
		
		
		// repeat using a specific *version* of the referenced object
		{
			Map<String, Set<Reference>> references = new HashMap<String, Set<Reference>>();
			Set<Reference> one = new HashSet<Reference>();
			references.put("groupOne", one);
			// Add one to one
			Reference ref = new Reference();
			ref.setTargetId(KeyFactory.keyToString(123L));
			ref.setTargetVersionNumber(new Long(1));
			one.add(ref);
			// Now save to the DB
			dboReferenceDao.replaceReferences(node1.getId(), references);
		}
		ehqr = dboReferenceDao.getReferrers(123L, 1, userInfo, null, null);
		count = ehqr.getTotalNumberOfResults();
		referrers = ehqr.getResults();
		assertEquals(expected, justIds(referrers));
		
		// if you refer to the wrong version, you get nothing back
		ehqr = dboReferenceDao.getReferrers(123L, 99, userInfo, null, null);
		count = ehqr.getTotalNumberOfResults();
		referrers = ehqr.getResults();
		expected = new HashSet<String>(); 
		assertEquals(expected, justIds(referrers));
	}
	
}
