package org.sagebionetworks.repo.model.dbo.dao;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.sagebionetworks.evaluation.model.Evaluation;
import org.sagebionetworks.evaluation.model.EvaluationStatus;
import org.sagebionetworks.ids.IdGenerator;
import org.sagebionetworks.ids.IdType;
import org.sagebionetworks.repo.model.ACCESS_TYPE;
import org.sagebionetworks.repo.model.ACTAccessRequirement;
import org.sagebionetworks.repo.model.AccessRequirement;
import org.sagebionetworks.repo.model.AccessRequirementDAO;
import org.sagebionetworks.repo.model.AccessRequirementStats;
import org.sagebionetworks.repo.model.ConflictingUpdateException;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.Node;
import org.sagebionetworks.repo.model.NodeDAO;
import org.sagebionetworks.repo.model.RestrictableObjectDescriptor;
import org.sagebionetworks.repo.model.RestrictableObjectType;
import org.sagebionetworks.repo.model.TermsOfUseAccessRequirement;
import org.sagebionetworks.repo.model.UserGroup;
import org.sagebionetworks.repo.model.UserGroupDAO;
import org.sagebionetworks.repo.model.jdo.KeyFactory;
import org.sagebionetworks.repo.model.jdo.NodeTestUtils;
import org.sagebionetworks.repo.web.NotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath:jdomodels-test-context.xml" })
public class DBOAccessRequirementDAOImplTest {

	@Autowired
	UserGroupDAO userGroupDAO;

	@Autowired
	AccessRequirementDAO accessRequirementDAO;

	@Autowired
	NodeDAO nodeDao;
	
	private UserGroup individualGroup = null;
	private Node node = null;
	private Node node2 = null;
	private TermsOfUseAccessRequirement accessRequirement = null;
	private TermsOfUseAccessRequirement accessRequirement2 = null;
	private List<AccessRequirement> ars = null;
	
	private TermsOfUseAccessRequirement accessRequirement3 = null;
	private TermsOfUseAccessRequirement accessRequirement4 = null;

	
	@Before
	public void setUp() throws Exception {
		individualGroup = new UserGroup();
		individualGroup.setIsIndividual(true);
		individualGroup.setCreationDate(new Date());
		individualGroup.setId(userGroupDAO.create(individualGroup).toString());
		// note:  we set up multiple nodes and multiple evaluations to ensure that filtering works
		if (node==null) {
			node = NodeTestUtils.createNew("foo", Long.parseLong(individualGroup.getId()));
			node.setId( nodeDao.createNew(node) );
		};
		if (node2==null) {
			node2 = NodeTestUtils.createNew("bar", Long.parseLong(individualGroup.getId()));
			node2.setId( nodeDao.createNew(node2) );
		};
	}
	
	@After
	public void tearDown() throws Exception{
		if (accessRequirement!=null && accessRequirement.getId()!=null) {
			accessRequirementDAO.delete(accessRequirement.getId().toString());
		}
		if (accessRequirement2!=null && accessRequirement2.getId()!=null) {
			accessRequirementDAO.delete(accessRequirement2.getId().toString());
		}
		if (accessRequirement3!=null && accessRequirement3.getId()!=null) {
			accessRequirementDAO.delete(accessRequirement3.getId().toString());
		}
		if (accessRequirement4!=null && accessRequirement4.getId()!=null) {
			accessRequirementDAO.delete(accessRequirement4.getId().toString());
		}
		if (ars!=null) {
			for (AccessRequirement ar : ars) accessRequirementDAO.delete(ar.getId().toString());
			ars.clear();
		}
		if (node!=null && nodeDao!=null) {
			nodeDao.delete(node.getId());
			node = null;
		}
		if (node2!=null && nodeDao!=null) {
			nodeDao.delete(node2.getId());
			node2 = null;
		}
		if (individualGroup != null) {
			userGroupDAO.delete(individualGroup.getId());
		}
	}
	
	public static TermsOfUseAccessRequirement newEntityAccessRequirement(UserGroup principal, Node node, String text) throws DatastoreException {
		TermsOfUseAccessRequirement accessRequirement = new TermsOfUseAccessRequirement();
		accessRequirement.setCreatedBy(principal.getId());
		accessRequirement.setCreatedOn(new Date());
		accessRequirement.setModifiedBy(principal.getId());
		accessRequirement.setModifiedOn(new Date());
		accessRequirement.setEtag("10");
		accessRequirement.setAccessType(ACCESS_TYPE.DOWNLOAD);
		RestrictableObjectDescriptor rod = AccessRequirementUtilsTest.createRestrictableObjectDescriptor(node.getId());
		accessRequirement.setSubjectIds(Arrays.asList(new RestrictableObjectDescriptor[]{rod, rod})); // test that repeated IDs doesn't break anything
		accessRequirement.setTermsOfUse(text);
		return accessRequirement;
	}

	// for PLFM-1730 test that if we create a number of access requirements, they come back in the creation order
	@Test
	public void testRetrievalOrder() throws Exception{
		this.ars = new ArrayList<AccessRequirement>();
		for (int i=0; i<10; i++) {
			TermsOfUseAccessRequirement accessRequirement = newEntityAccessRequirement(individualGroup, node, "foo_"+i);
			ars.add(accessRequirementDAO.create(accessRequirement));
		}
		List<AccessRequirement> ars2 = accessRequirementDAO.getAllAccessRequirementsForSubject(Collections.singletonList(node.getId()), RestrictableObjectType.ENTITY);
		assertEquals(ars, ars2);
		List<AccessRequirement> ars3 = accessRequirementDAO.getAccessRequirementsForSubject(Collections.singletonList(node.getId()), RestrictableObjectType.ENTITY, 10L, 0L);
		assertEquals(ars, ars3);

		List<Long> principalIds = new ArrayList<Long>();
		principalIds.add(Long.parseLong(individualGroup.getId()));
		List<ACCESS_TYPE> downloadAccessType = new ArrayList<ACCESS_TYPE>();
		downloadAccessType.add(ACCESS_TYPE.DOWNLOAD);
		List<Long> arIds = accessRequirementDAO.getAllUnmetAccessRequirements(Collections.singletonList(node.getId()), RestrictableObjectType.ENTITY, principalIds, downloadAccessType);
		for (int i=0; i<ars.size(); i++) {
			assertEquals(ars.get(i).getId(), arIds.get(i));
		}
	}	
	

	@Test
	public void testEntityAccessRequirementCRUD() throws Exception{
		// Create a new object
		accessRequirement = newEntityAccessRequirement(individualGroup, node, "foo");
		RestrictableObjectDescriptor subjectId = 
			AccessRequirementUtilsTest.createRestrictableObjectDescriptor(node.getId(), RestrictableObjectType.ENTITY);

		long initialCount = accessRequirementDAO.getCount();
		
		// Create it
		accessRequirement = accessRequirementDAO.create(accessRequirement);
		assertNotNull(accessRequirement.getId());
		assertEquals(accessRequirement.getSubjectIds(), accessRequirementDAO.getSubjects(accessRequirement.getId()));
		
		assertEquals(1+initialCount, accessRequirementDAO.getCount());
		
		// Fetch it
		// PLFM-1477, we have to check that retrieval works when there is another access requirement
		accessRequirement2 = newEntityAccessRequirement(individualGroup, node2, "bar");
		accessRequirement2 = accessRequirementDAO.create(accessRequirement2);		
		AccessRequirement clone = accessRequirementDAO.get(accessRequirement.getId().toString());
		assertNotNull(clone);
		assertEquals(accessRequirement, clone);

		// Get by Node Id
		Collection<AccessRequirement> ars = accessRequirementDAO.getAllAccessRequirementsForSubject(Collections.singletonList(subjectId.getId()), subjectId.getType());
		assertEquals(1, ars.size());
		assertEquals(accessRequirement, ars.iterator().next());
		ars = accessRequirementDAO.getAccessRequirementsForSubject(Collections.singletonList(subjectId.getId()), subjectId.getType(), 10L, 0L);
		assertEquals(1, ars.size());
		assertEquals(accessRequirement, ars.iterator().next());
		
		// including an irrelevant  ID in the ID list doesn't change the result
		List<String> ids = new ArrayList<String>();
		ids.add(subjectId.getId());
		ids.add(KeyFactory.keyToString(KeyFactory.stringToKey(subjectId.getId())-100L));
		ars = accessRequirementDAO.getAllAccessRequirementsForSubject(ids, subjectId.getType());
		assertEquals(1, ars.size());
		assertEquals(accessRequirement, ars.iterator().next());
		
		// check the 'unmet' access requirements
		List<Long> principalIds = new ArrayList<Long>();
		principalIds.add(Long.parseLong(individualGroup.getId()));
		List<Long> arIds = accessRequirementDAO.getAllUnmetAccessRequirements(Collections.singletonList(subjectId.getId()), 
				subjectId.getType(), principalIds, Collections.singletonList(accessRequirement.getAccessType()));
		assertEquals(1, arIds.size());
		assertEquals(accessRequirement.getId(), arIds.get(0));
		// including an irrelevant node ID in the ID list doesn't change the result
		arIds = accessRequirementDAO.getAllUnmetAccessRequirements(ids, 
				subjectId.getType(), principalIds, Collections.singletonList(accessRequirement.getAccessType()));
		assertEquals(1, arIds.size());
		assertEquals(accessRequirement.getId(), arIds.get(0));
		
		// update it
		clone = ars.iterator().next();
		clone.setAccessType(ACCESS_TYPE.READ);
		AccessRequirement updatedAR = accessRequirementDAO.update(clone);
		assertEquals(clone.getAccessType(), updatedAR.getAccessType());

		assertTrue("etags should be different after an update", !clone.getEtag().equals(updatedAR.getEtag()));

		try {
			((TermsOfUseAccessRequirement)clone).setTermsOfUse("bar");
			accessRequirementDAO.update(clone);
			fail("conflicting update exception not thrown");
		}
		catch(ConflictingUpdateException e){
			// We expected this exception
		}

		assertEquals(accessRequirement.getClass().getName(),
				accessRequirementDAO.getConcreteType(accessRequirement.getId().toString()));

		// Delete the access requirements
		accessRequirementDAO.delete(accessRequirement.getId().toString());
		accessRequirementDAO.delete(accessRequirement2.getId().toString());

		assertEquals(initialCount, accessRequirementDAO.getCount());
	}
	
	@Test
	public void testMultipleNodes() throws Exception {
		// Create a new object
		accessRequirement = newEntityAccessRequirement(individualGroup, node, "foo");
		
		long initialCount = accessRequirementDAO.getCount();
		
		// Create it
		accessRequirement = accessRequirementDAO.create(accessRequirement);
		assertNotNull(accessRequirement.getId());
		
		assertEquals(1+initialCount, accessRequirementDAO.getCount());
		
		accessRequirement2 = newEntityAccessRequirement(individualGroup, node2, "bar");
		accessRequirement2 = accessRequirementDAO.create(accessRequirement2);		
		AccessRequirement clone = accessRequirementDAO.get(accessRequirement.getId().toString());
		assertNotNull(clone);
		assertEquals(accessRequirement, clone);
				
		// retrieve by multiple node ids
		List<String> nodeIds = new ArrayList<String>();
		nodeIds.add(node.getId());
		nodeIds.add(node2.getId());
		ars = accessRequirementDAO.getAllAccessRequirementsForSubject(nodeIds, RestrictableObjectType.ENTITY);
		assertEquals(2, ars.size());
		boolean found1 = false;
		boolean found2 = false;
		for (AccessRequirement ar : ars) {
			if (ar.equals(accessRequirement)) found1=true;
			if (ar.equals(accessRequirement2)) found2=true;
		}
		assertTrue(found1);
		assertTrue(found2);

		// check the 'unmet' access requirements
		List<Long> principalIds = new ArrayList<Long>();
		principalIds.add(Long.parseLong(individualGroup.getId()));
		List<String> ids = new ArrayList<String>();
		ids.add(node.getId());
		List<Long> arIds = accessRequirementDAO.getAllUnmetAccessRequirements(ids, 
				RestrictableObjectType.ENTITY, principalIds, 
				Collections.singletonList(accessRequirement.getAccessType()));
		assertEquals(1, arIds.size());
		assertEquals(accessRequirement.getId(), arIds.get(0));
		
		// check that it works to retrieve from multiple nodes
		ids.add(node2.getId());
		arIds = accessRequirementDAO.getAllUnmetAccessRequirements(ids, 
				RestrictableObjectType.ENTITY, principalIds, 
				Collections.singletonList(accessRequirement.getAccessType()));
		assertEquals(2, arIds.size());
		found1 = false;
		found2 = false;
		for (AccessRequirement ar : ars) {
			if (ar.equals(accessRequirement)) found1=true;
			if (ar.equals(accessRequirement2)) found2=true;
		}
		assertTrue(found1);
		assertTrue(found2);
		
		// Delete the access requirements
		accessRequirementDAO.delete(accessRequirement.getId().toString());
		accessRequirementDAO.delete(accessRequirement2.getId().toString());

		assertEquals(initialCount, accessRequirementDAO.getCount());
	}

	@Test (expected = NotFoundException.class)
	public void testGetConcreteTypeNotFound() {
		accessRequirementDAO.getConcreteType("1");
	}

	@Test (expected = IllegalArgumentException.class)
	public void testGetAccessRequirementStatsWithNullSubjectIds() {
		accessRequirementDAO.getAccessRequirementStats(null, RestrictableObjectType.ENTITY);
	}

	@Test (expected = IllegalArgumentException.class)
	public void testGetAccessRequirementStatsWithEmptySubjectIds() {
		accessRequirementDAO.getAccessRequirementStats(new LinkedList<String>(), RestrictableObjectType.ENTITY);
	}

	@Test (expected = IllegalArgumentException.class)
	public void testGetAccessRequirementStatsWithNullRestrictableObjectType() {
		accessRequirementDAO.getAccessRequirementStats(Arrays.asList(node.getId()), null);
	}

	@Test
	public void testGetAccessRequirementStats() {
		AccessRequirementStats stats = accessRequirementDAO.getAccessRequirementStats(Arrays.asList(node.getId()), RestrictableObjectType.ENTITY);
		assertNotNull(stats);
		assertFalse(stats.getHasToU());
		assertFalse(stats.getHasACT());
		assertTrue(stats.getRequirementIdSet().isEmpty());

		RestrictableObjectDescriptor rod = AccessRequirementUtilsTest.createRestrictableObjectDescriptor(node.getId());
		accessRequirement = new TermsOfUseAccessRequirement();
		accessRequirement.setCreatedBy(individualGroup.getId());
		accessRequirement.setCreatedOn(new Date());
		accessRequirement.setModifiedBy(individualGroup.getId());
		accessRequirement.setModifiedOn(new Date());
		accessRequirement.setEtag("etag");
		accessRequirement.setAccessType(ACCESS_TYPE.DOWNLOAD);
		accessRequirement.setSubjectIds(Arrays.asList(rod));
		accessRequirement = accessRequirementDAO.create(accessRequirement);

		stats = accessRequirementDAO.getAccessRequirementStats(Arrays.asList(node.getId()), RestrictableObjectType.ENTITY);
		assertNotNull(stats);
		assertTrue(stats.getHasToU());
		assertFalse(stats.getHasACT());
		assertFalse(stats.getHasLock());
		assertTrue(stats.getRequirementIdSet().contains(accessRequirement.getId().toString()));

		ACTAccessRequirement accessRequirement2 = new ACTAccessRequirement();
		accessRequirement2.setCreatedBy(individualGroup.getId());
		accessRequirement2.setCreatedOn(new Date());
		accessRequirement2.setModifiedBy(individualGroup.getId());
		accessRequirement2.setModifiedOn(new Date());
		accessRequirement2.setEtag("etag");
		accessRequirement2.setAccessType(ACCESS_TYPE.DOWNLOAD);
		accessRequirement2.setSubjectIds(Arrays.asList(rod));
		accessRequirement2 = accessRequirementDAO.create(accessRequirement2);

		stats = accessRequirementDAO.getAccessRequirementStats(Arrays.asList(node.getId()), RestrictableObjectType.ENTITY);
		assertNotNull(stats);
		assertTrue(stats.getHasToU());
		assertTrue(stats.getHasACT());
		assertTrue(stats.getRequirementIdSet().contains(accessRequirement.getId().toString()));
		assertTrue(stats.getRequirementIdSet().contains(accessRequirement2.getId().toString()));

		accessRequirementDAO.delete(accessRequirement2.getId().toString());
	}

	/*
	 * PLFM-3300
	 */
	@Test
	public void testAROnEntityAndItsParent() {
		node2.setParentId(node.getId());
		nodeDao.updateNode(node2);

		TermsOfUseAccessRequirement accessRequirement = new TermsOfUseAccessRequirement();
		accessRequirement.setCreatedBy(individualGroup.getId());
		accessRequirement.setCreatedOn(new Date());
		accessRequirement.setModifiedBy(individualGroup.getId());
		accessRequirement.setModifiedOn(new Date());
		accessRequirement.setAccessType(ACCESS_TYPE.DOWNLOAD);
		RestrictableObjectDescriptor rod1 = AccessRequirementUtilsTest.createRestrictableObjectDescriptor(node.getId());
		RestrictableObjectDescriptor rod2 = AccessRequirementUtilsTest.createRestrictableObjectDescriptor(node2.getId());
		accessRequirement.setSubjectIds(Arrays.asList(new RestrictableObjectDescriptor[]{rod1, rod2}));
		accessRequirement.setTermsOfUse("text");

		accessRequirement = accessRequirementDAO.create(accessRequirement);
		List<AccessRequirement> list = accessRequirementDAO.getAccessRequirementsForSubject(Arrays.asList(node.getId(), node2.getId()), RestrictableObjectType.ENTITY, 10L, 0L);
		assertNotNull(list);
		assertEquals(1, list.size());
		assertEquals(accessRequirement, list.get(0));

		accessRequirementDAO.delete(accessRequirement.getId().toString());
	}
}
