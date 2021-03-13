package org.sagebionetworks.repo.model.dbo.dao;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.sagebionetworks.repo.model.ACCESS_TYPE;
import org.sagebionetworks.repo.model.AccessRequirement;
import org.sagebionetworks.repo.model.AccessRequirementDAO;
import org.sagebionetworks.repo.model.AccessRequirementStats;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.ManagedACTAccessRequirement;
import org.sagebionetworks.repo.model.Node;
import org.sagebionetworks.repo.model.NodeDAO;
import org.sagebionetworks.repo.model.RestrictableObjectDescriptor;
import org.sagebionetworks.repo.model.RestrictableObjectType;
import org.sagebionetworks.repo.model.SelfSignAccessRequirement;
import org.sagebionetworks.repo.model.TermsOfUseAccessRequirement;
import org.sagebionetworks.repo.model.UserGroup;
import org.sagebionetworks.repo.model.UserGroupDAO;
import org.sagebionetworks.repo.model.dataaccess.Request;
import org.sagebionetworks.repo.model.dataaccess.ResearchProject;
import org.sagebionetworks.repo.model.dbo.dao.dataaccess.RequestDAO;
import org.sagebionetworks.repo.model.dbo.dao.dataaccess.RequestTestUtils;
import org.sagebionetworks.repo.model.dbo.dao.dataaccess.ResearchProjectDAO;
import org.sagebionetworks.repo.model.dbo.dao.dataaccess.ResearchProjectTestUtils;
import org.sagebionetworks.repo.model.jdo.KeyFactory;
import org.sagebionetworks.repo.model.jdo.NodeTestUtils;
import org.sagebionetworks.repo.web.NotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.transaction.IllegalTransactionStateException;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(locations = { "classpath:jdomodels-test-context.xml" })
public class DBOAccessRequirementDAOImplTest {

	@Autowired
	UserGroupDAO userGroupDAO;

	@Autowired
	AccessRequirementDAO accessRequirementDAO;

	@Autowired
	NodeDAO nodeDao;

	@Autowired
	private RequestDAO requestDao;

	@Autowired
	private ResearchProjectDAO researchProjectDao;
	
	private UserGroup individualGroup = null;
	private Node node = null;
	private Node node2 = null;
	private TermsOfUseAccessRequirement accessRequirement = null;
	private TermsOfUseAccessRequirement accessRequirement2 = null;
	private List<AccessRequirement> ars = null;

	
	@BeforeEach
	public void setUp() throws Exception {
		requestDao.truncateAll();
		researchProjectDao.truncateAll();
		accessRequirementDAO.clear();
		ars = new ArrayList<>();
		individualGroup = new UserGroup();
		individualGroup.setIsIndividual(true);
		individualGroup.setCreationDate(new Date());
		individualGroup.setId(userGroupDAO.create(individualGroup).toString());
		// note: we set up multiple nodes and multiple evaluations to ensure that filtering works
		if (node==null) {
			node = NodeTestUtils.createNew("foo", Long.parseLong(individualGroup.getId()));
			node.setId( nodeDao.createNew(node) );
		};
		if (node2==null) {
			node2 = NodeTestUtils.createNew("bar", Long.parseLong(individualGroup.getId()));
			node2.setId( nodeDao.createNew(node2) );
		};
	}
	
	@AfterEach
	public void tearDown() throws Exception{
		requestDao.truncateAll();
		researchProjectDao.truncateAll();
		accessRequirementDAO.clear();
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
		accessRequirement.setVersionNumber(1L);
		RestrictableObjectDescriptor rod = AccessRequirementUtilsTest.createRestrictableObjectDescriptor(node.getId());
		accessRequirement.setSubjectIds(Arrays.asList(new RestrictableObjectDescriptor[]{rod, rod})); // test that repeated IDs doesn't break anything
		accessRequirement.setTermsOfUse(text);
		return accessRequirement;
	}

	// for PLFM-1730 test that if we create a number of access requirements, they come back in the creation order
	@Test
	public void testRetrievalOrder() throws Exception{
		for (int i=0; i<10; i++) {
			TermsOfUseAccessRequirement accessRequirement = newEntityAccessRequirement(individualGroup, node, "foo_"+i);
			ars.add(accessRequirementDAO.create(accessRequirement));
		}
		List<AccessRequirement> ars2 = accessRequirementDAO.getAccessRequirementsForSubject(Collections.singletonList(KeyFactory.stringToKey(node.getId())), RestrictableObjectType.ENTITY, 10L, 0L);
		assertEquals(ars, ars2);
	}
	
	/**
	 * PLFM-4415 When subjectIds is null return an empty list.
	 * 
	 */
	@Test
	public void testPLFM_4415(){
		// Create a new object
		accessRequirement = newEntityAccessRequirement(individualGroup, node, "foo");
		accessRequirement.setSubjectIds(null);
		// Create it
		accessRequirement = accessRequirementDAO.create(accessRequirement);
		assertNotNull(accessRequirement.getSubjectIds());
		assertTrue(accessRequirement.getSubjectIds().isEmpty());
	}

	@Test
	public void testEntityAccessRequirementCRUD() throws Exception{
		// Create a new object
		accessRequirement = newEntityAccessRequirement(individualGroup, node, "foo");
		RestrictableObjectDescriptor subjectId = 
			AccessRequirementUtilsTest.createRestrictableObjectDescriptor(node.getId(), RestrictableObjectType.ENTITY);

		// Create it
		accessRequirement = accessRequirementDAO.create(accessRequirement);
		assertNotNull(accessRequirement.getId());
		assertEquals(accessRequirement.getSubjectIds(), accessRequirementDAO.getSubjects(accessRequirement.getId()));
		assertEquals(accessRequirement.getSubjectIds(), accessRequirementDAO.getSubjects(accessRequirement.getId(), 10L, 0L));

		// Fetch it
		// PLFM-1477, we have to check that retrieval works when there is another access requirement
		accessRequirement2 = newEntityAccessRequirement(individualGroup, node2, "bar");
		accessRequirement2 = accessRequirementDAO.create(accessRequirement2);		
		AccessRequirement clone = accessRequirementDAO.get(accessRequirement.getId().toString());
		assertNotNull(clone);
		assertEquals(accessRequirement, clone);

		// Get by Node Id
		Collection<AccessRequirement> ars = accessRequirementDAO.getAccessRequirementsForSubject(Collections.singletonList(KeyFactory.stringToKey(subjectId.getId())), subjectId.getType(), 10L, 0L);
		assertEquals(1, ars.size());
		assertEquals(accessRequirement, ars.iterator().next());
		
		// update it
		clone = ars.iterator().next();
		clone.setAccessType(ACCESS_TYPE.DOWNLOAD);
		clone.setVersionNumber(accessRequirement.getVersionNumber()+1);
		AccessRequirement updatedAR = accessRequirementDAO.update(clone);
		assertEquals(clone.getAccessType(), updatedAR.getAccessType());

		assertTrue(!clone.getEtag().equals(updatedAR.getEtag()), "etags should be different after an update");

		assertEquals(accessRequirement.getClass().getName(),
				accessRequirementDAO.getConcreteType(accessRequirement.getId().toString()));

		// Delete the access requirements
		accessRequirementDAO.delete(accessRequirement.getId().toString());
		accessRequirementDAO.delete(accessRequirement2.getId().toString());
	}
	
	@Test
	public void testMultipleNodes() throws Exception {
		// Create a new object
		accessRequirement = newEntityAccessRequirement(individualGroup, node, "foo");
		
		// Create it
		accessRequirement = accessRequirementDAO.create(accessRequirement);
		assertNotNull(accessRequirement.getId());
		
		accessRequirement2 = newEntityAccessRequirement(individualGroup, node2, "bar");
		accessRequirement2 = accessRequirementDAO.create(accessRequirement2);		
		AccessRequirement clone = accessRequirementDAO.get(accessRequirement.getId().toString());
		assertNotNull(clone);
		assertEquals(accessRequirement, clone);
		
		// Delete the access requirements
		accessRequirementDAO.delete(accessRequirement.getId().toString());
		accessRequirementDAO.delete(accessRequirement2.getId().toString());
	}
	
	@Test
	public void testGetDoesNotExist(){
		assertThrows(NotFoundException.class, () -> {			
			accessRequirementDAO.get("-1");
		});
	}

	@Test
	public void testGetConcreteTypeNotFound() {
		assertThrows(NotFoundException.class, () -> {
			accessRequirementDAO.getConcreteType("1");
		});
	}

	@Test
	public void testGetAccessRequirementStatsWithNullSubjectIds() {
		assertThrows(IllegalArgumentException.class, () -> {
			accessRequirementDAO.getAccessRequirementStats(null, RestrictableObjectType.ENTITY);
		});
	}

	@Test
	public void testGetAccessRequirementStatsWithEmptySubjectIds() {
		assertThrows(IllegalArgumentException.class, () -> {
			accessRequirementDAO.getAccessRequirementStats(new LinkedList<Long>(), RestrictableObjectType.ENTITY);
		});
	}

	@Test
	public void testGetAccessRequirementStatsWithNullRestrictableObjectType() {
		assertThrows(IllegalArgumentException.class, () -> {
			accessRequirementDAO.getAccessRequirementStats(Arrays.asList(KeyFactory.stringToKey(node.getId())), null);
		});
	}

	@Test
	public void testGetAccessRequirementStats() {
		AccessRequirementStats stats = accessRequirementDAO.getAccessRequirementStats(Arrays.asList(KeyFactory.stringToKey(node.getId())), RestrictableObjectType.ENTITY);
		assertNotNull(stats);
		assertFalse(stats.getHasToU());
		assertFalse(stats.getHasACT());
		assertTrue(stats.getRequirementIdSet().isEmpty());

		/*
		 * PLFM-4501
		 */
		RestrictableObjectDescriptor rod = AccessRequirementUtilsTest.createRestrictableObjectDescriptor(node.getId());
		SelfSignAccessRequirement selfsignAR = new SelfSignAccessRequirement();
		selfsignAR.setCreatedBy(individualGroup.getId());
		selfsignAR.setCreatedOn(new Date());
		selfsignAR.setModifiedBy(individualGroup.getId());
		selfsignAR.setModifiedOn(new Date());
		selfsignAR.setEtag("etag");
		selfsignAR.setAccessType(ACCESS_TYPE.DOWNLOAD);
		selfsignAR.setSubjectIds(Arrays.asList(rod));
		selfsignAR.setVersionNumber(1L);
		selfsignAR = accessRequirementDAO.create(selfsignAR);

		stats = accessRequirementDAO.getAccessRequirementStats(Arrays.asList(KeyFactory.stringToKey(node.getId())), RestrictableObjectType.ENTITY);
		assertNotNull(stats);
		assertTrue(stats.getHasToU());
		assertFalse(stats.getHasACT());
		assertFalse(stats.getHasLock());
		assertTrue(stats.getRequirementIdSet().contains(selfsignAR.getId().toString()));

		accessRequirementDAO.delete(selfsignAR.getId().toString());

		accessRequirement = new TermsOfUseAccessRequirement();
		accessRequirement.setCreatedBy(individualGroup.getId());
		accessRequirement.setCreatedOn(new Date());
		accessRequirement.setModifiedBy(individualGroup.getId());
		accessRequirement.setModifiedOn(new Date());
		accessRequirement.setEtag("etag");
		accessRequirement.setAccessType(ACCESS_TYPE.DOWNLOAD);
		accessRequirement.setSubjectIds(Arrays.asList(rod));
		accessRequirement.setVersionNumber(1L);
		accessRequirement = accessRequirementDAO.create(accessRequirement);

		stats = accessRequirementDAO.getAccessRequirementStats(Arrays.asList(KeyFactory.stringToKey(node.getId())), RestrictableObjectType.ENTITY);
		assertNotNull(stats);
		assertTrue(stats.getHasToU());
		assertFalse(stats.getHasACT());
		assertFalse(stats.getHasLock());
		assertTrue(stats.getRequirementIdSet().contains(accessRequirement.getId().toString()));

		ManagedACTAccessRequirement accessRequirement2 = new ManagedACTAccessRequirement();
		accessRequirement2.setCreatedBy(individualGroup.getId());
		accessRequirement2.setCreatedOn(new Date());
		accessRequirement2.setModifiedBy(individualGroup.getId());
		accessRequirement2.setModifiedOn(new Date());
		accessRequirement2.setEtag("etag");
		accessRequirement2.setAccessType(ACCESS_TYPE.DOWNLOAD);
		accessRequirement2.setSubjectIds(Arrays.asList(rod));
		accessRequirement2 = accessRequirementDAO.create(accessRequirement2);

		stats = accessRequirementDAO.getAccessRequirementStats(Arrays.asList(KeyFactory.stringToKey(node.getId())), RestrictableObjectType.ENTITY);
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
		accessRequirement.setVersionNumber(1L);
		RestrictableObjectDescriptor rod1 = AccessRequirementUtilsTest.createRestrictableObjectDescriptor(node.getId());
		RestrictableObjectDescriptor rod2 = AccessRequirementUtilsTest.createRestrictableObjectDescriptor(node2.getId());
		accessRequirement.setSubjectIds(Arrays.asList(new RestrictableObjectDescriptor[]{rod1, rod2}));
		accessRequirement.setTermsOfUse("text");

		accessRequirement = accessRequirementDAO.create(accessRequirement);
		List<AccessRequirement> list = accessRequirementDAO.getAccessRequirementsForSubject(Arrays.asList(KeyFactory.stringToKey(node.getId()), KeyFactory.stringToKey(node2.getId())), RestrictableObjectType.ENTITY, 10L, 0L);
		assertNotNull(list);
		assertEquals(1, list.size());
		assertEquals(accessRequirement, list.get(0));

		accessRequirementDAO.delete(accessRequirement.getId().toString());
	}

	@Test
	public void testGetForUpdateWithoutTransaction() {
		accessRequirement = newEntityAccessRequirement(individualGroup, node, "foo");
		accessRequirement = accessRequirementDAO.create(accessRequirement);
		assertThrows(IllegalTransactionStateException.class, () -> {
			accessRequirementDAO.getForUpdate(accessRequirement.getId().toString());
		});
	}

	@Test
	public void testGetAccessRequirementForUpdateWithoutTransaction() {
		accessRequirement = newEntityAccessRequirement(individualGroup, node, "foo");
		accessRequirement = accessRequirementDAO.create(accessRequirement);
		assertThrows(IllegalTransactionStateException.class, () -> {
			accessRequirementDAO.getAccessRequirementForUpdate(accessRequirement.getId().toString());
		});
	}

	@Test
	public void testGetAccessRequirementDiffForNullSource() {
		List<Long> destSubjects = Arrays.asList(KeyFactory.stringToKey(node.getId()));
		assertThrows(IllegalArgumentException.class, () -> {
			accessRequirementDAO.getAccessRequirementDiff(null, destSubjects , RestrictableObjectType.ENTITY);
		});
	}

	@Test
	public void testGetAccessRequirementDiffForNullDest() {
		List<Long> sourceSubjects = Arrays.asList(KeyFactory.stringToKey(node.getId()));
		assertThrows(IllegalArgumentException.class, () -> {
			accessRequirementDAO.getAccessRequirementDiff(sourceSubjects, null , RestrictableObjectType.ENTITY);
		});
	}

	@Test
	public void testGetAccessRequirementDiffForEmptySource() {
		List<Long> sourceSubjects = new LinkedList<Long>();
		List<Long> destSubjects = Arrays.asList(KeyFactory.stringToKey(node.getId()));
		assertThrows(IllegalArgumentException.class, () -> {
			accessRequirementDAO.getAccessRequirementDiff(sourceSubjects, destSubjects , RestrictableObjectType.ENTITY);
		});
	}

	@Test
	public void testGetAccessRequirementDiffForEmptyDest() {
		List<Long> sourceSubjects = Arrays.asList(KeyFactory.stringToKey(node.getId()));
		List<Long> destSubjects = new LinkedList<Long>();
		assertThrows(IllegalArgumentException.class, () -> {
			accessRequirementDAO.getAccessRequirementDiff(sourceSubjects, destSubjects , RestrictableObjectType.ENTITY);
		});
	}

	@Test
	public void testGetAccessRequirementDiffWithDestinationMoreRestricted() {
		accessRequirement = newEntityAccessRequirement(individualGroup, node, "foo");
		accessRequirement = accessRequirementDAO.create(accessRequirement);
		List<Long> sourceSubjects = Arrays.asList(KeyFactory.stringToKey(node2.getId()));
		List<Long> destSubjects = Arrays.asList(KeyFactory.stringToKey(node.getId()));
		assertEquals(new LinkedList<String>(),
				accessRequirementDAO.getAccessRequirementDiff(sourceSubjects, destSubjects , RestrictableObjectType.ENTITY));
	}

	@Test
	public void testGetAccessRequirementDiffWithMatchingAR() {
		accessRequirement = newEntityAccessRequirement(individualGroup, node, "foo");
		accessRequirement = accessRequirementDAO.create(accessRequirement);
		List<Long> sourceSubjects = Arrays.asList(KeyFactory.stringToKey(node.getId()));
		List<Long> destSubjects = Arrays.asList(KeyFactory.stringToKey(node.getId()));
		assertEquals(new LinkedList<String>(),
				accessRequirementDAO.getAccessRequirementDiff(sourceSubjects, destSubjects , RestrictableObjectType.ENTITY));
	}

	@Test
	public void testGetAccessRequirementDiffWithSourceMoreRestricted() {
		accessRequirement = newEntityAccessRequirement(individualGroup, node, "foo");
		accessRequirement = accessRequirementDAO.create(accessRequirement);
		List<Long> sourceSubjects = Arrays.asList(KeyFactory.stringToKey(node.getId()));
		List<Long> destSubjects = Arrays.asList(KeyFactory.stringToKey(node2.getId()));
		assertEquals(Arrays.asList(accessRequirement.getId().toString()),
				accessRequirementDAO.getAccessRequirementDiff(sourceSubjects, destSubjects , RestrictableObjectType.ENTITY));
	}

	@Test
	public void testDeleteAccessRequirement() {
		accessRequirement = newEntityAccessRequirement(individualGroup, node, "foo");
		accessRequirement = accessRequirementDAO.create(accessRequirement);
		String accessRequirementId = String.valueOf(accessRequirement.getId());
		String expectedMessage = "An access requirement with id "+ accessRequirementId + " cannot be found.";
		// Call under test
		accessRequirementDAO.delete(accessRequirementId);
		NotFoundException exception = assertThrows(NotFoundException.class, () -> {
			accessRequirementDAO.get(accessRequirementId);
		});
		assertEquals(expectedMessage, exception.getMessage());
	}

	@Test
	public void testDeleteAccessRequirementForeignKeyConstraint() throws Exception {
		accessRequirement = newEntityAccessRequirement(individualGroup, node, "foo");
		accessRequirement = accessRequirementDAO.create(accessRequirement);
		String accessRequirementId = String.valueOf(accessRequirement.getId());
		ResearchProject researchProject = ResearchProjectTestUtils.createNewDto();
		researchProject.setAccessRequirementId(accessRequirementId);
		researchProject = researchProjectDao.create(researchProject);
		Request dto = RequestTestUtils.createNewRequest();
		dto.setAccessRequirementId(accessRequirementId);
		dto.setResearchProjectId(researchProject.getId());
		dto = requestDao.create(dto);
		String expectedMessage = "The access requirement with id " + accessRequirementId +
				" cannot be deleted as it is referenced by another object.";
		IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
			// Call under test
			accessRequirementDAO.delete(accessRequirementId);
		});
		assertEquals(expectedMessage, exception.getMessage());
	}

}
